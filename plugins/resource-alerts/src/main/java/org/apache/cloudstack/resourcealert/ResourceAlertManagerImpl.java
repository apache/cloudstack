// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.resourcealert;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleDao;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleVO;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.mailing.MailAddress;
import org.apache.cloudstack.utils.mailing.SMTPMailProperties;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.event.AlertGenerator;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.server.StatsCollector;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.UserVmDao;

public class ResourceAlertManagerImpl extends ManagerBase implements ResourceAlertManager, Configurable {

    static final ConfigKey<Integer> EVAL_INTERVAL = new ConfigKey<>("Advanced", Integer.class,
            "resource.alert.evaluation.interval", "60",
            "Interval in seconds between resource alert rule evaluations", true);

    @Inject ResourceAlertRuleDao ruleDao;
    @Inject ResourceAlertDao alertDao;
    @Inject UserVmDao userVmDao;
    @Inject HostDao hostDao;
    @Inject PrimaryDataStoreDao storagePoolDao;
    @Inject VolumeDao volumeDao;
    @Inject StatsCollector statsCollector;
    @Inject ConfigurationDao configDao;

    private ScheduledExecutorService executor;
    ExecutorService emailExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ResourceAlertEmailSender");
        t.setDaemon(true);
        return t;
    });

    private SMTPMailSender mailSender;
    private String[] emailRecipients;
    private String senderAddress;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        Map<String, String> configs = configDao.getConfiguration("management-server", params);

        String emailList = configs.get("alert.email.addresses");
        if (StringUtils.isNotBlank(emailList)) {
            emailRecipients = emailList.split(",");
        }
        senderAddress = configs.get("alert.email.sender");
        mailSender = new SMTPMailSender(configs, "alert.smtp");

        return super.configure(name, params);
    }

    @Override
    public boolean start() {
        int interval = EVAL_INTERVAL.value();
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ResourceAlertEvaluator");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::safeEvaluateRules, interval, interval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        if (executor != null) {
            executor.shutdown();
        }
        emailExecutor.shutdown();
        return true;
    }

    @Override
    public void evaluateRules() {
        List<ResourceAlertRuleVO> rules = ruleDao.listActive();
        for (ResourceAlertRuleVO rule : rules) {
            evaluateRule(rule);
        }
    }

    private void safeEvaluateRules() {
        try {
            evaluateRules();
        } catch (Exception e) {
            logger.error("Uncaught exception in resource alert evaluation", e);
        }
    }

    private void evaluateRule(ResourceAlertRuleVO rule) {
        ResourceAlertMetric metric = ResourceAlertMetric.valueOf(rule.getMetric());
        for (Long resourceId : getResourceIds(rule)) {
            try {
                Double value = getMetricValue(rule.getResourceType(), metric, resourceId);
                if (value == null || value < 0) {
                    continue;
                }
                if (rule.getCondition().evaluate(value, rule.getThreshold())
                        && canFire(rule.getId(), resourceId, rule.getResetInterval())) {
                    fireAlert(rule, resourceId, value);
                }
            } catch (Exception e) {
                logger.warn("Error evaluating rule {} for resource {}: {}", rule.getUuid(), resourceId, e.getMessage());
            }
        }
    }

    private List<Long> getResourceIds(ResourceAlertRuleVO rule) {
        if (rule.getResourceId() != null) {
            return Collections.singletonList(rule.getResourceId());
        }
        switch (rule.getResourceType()) {
            case VirtualMachine:
                return userVmDao.listByAccountId(rule.getAccountId()).stream()
                        .filter(vm -> VirtualMachine.State.Running.equals(vm.getState()))
                        .map(vm -> vm.getId())
                        .collect(Collectors.toList());
            case Volume:
                return volumeDao.findByAccount(rule.getAccountId()).stream()
                        .map(v -> v.getId())
                        .collect(Collectors.toList());
            case Host:
                return hostDao.listAll().stream()
                        .filter(h -> Host.Type.Routing.equals(h.getType()))
                        .map(h -> h.getId())
                        .collect(Collectors.toList());
            case StoragePool:
                return storagePoolDao.listAll().stream()
                        .map(p -> p.getId())
                        .collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }

    private Double getMetricValue(ResourceAlertRule.ResourceType type, ResourceAlertMetric metric, long resourceId) {
        switch (metric) {
            case CPU_UTILIZATION:
                if (type == ResourceAlertRule.ResourceType.VirtualMachine) {
                    VmStats s = statsCollector.getVmStats(resourceId, false);
                    return s != null ? s.getCPUUtilization() : null;
                }
                if (type == ResourceAlertRule.ResourceType.Host) {
                    HostStats s = statsCollector.getHostStats(resourceId);
                    return s != null ? s.getCpuUtilization() : null;
                }
                break;
            case MEMORY_UTILIZATION:
                if (type == ResourceAlertRule.ResourceType.VirtualMachine) {
                    VmStats s = statsCollector.getVmStats(resourceId, false);
                    if (s == null) return null;
                    double total = s.getMemoryKBs();
                    double free = s.getIntFreeMemoryKBs();
                    // free is -1 when VM has no balloon driver
                    if (total <= 0 || free < 0) return null;
                    return (1.0 - free / total) * 100.0;
                }
                if (type == ResourceAlertRule.ResourceType.Host) {
                    HostStats s = statsCollector.getHostStats(resourceId);
                    if (s == null) return null;
                    double total = s.getTotalMemoryKBs();
                    double free = s.getFreeMemoryKBs();
                    if (total <= 0) return null;
                    return ((total - free) / total) * 100.0;
                }
                break;
            case DISK_READ_IOPS:
                return getVmDiskStat(type, resourceId, s -> s.getDiskReadIOs());
            case DISK_WRITE_IOPS:
                return getVmDiskStat(type, resourceId, s -> s.getDiskWriteIOs());
            case DISK_READ_KBPS:
                return getVmDiskStat(type, resourceId, s -> s.getDiskReadKBs());
            case DISK_WRITE_KBPS:
                return getVmDiskStat(type, resourceId, s -> s.getDiskWriteKBs());
            case NETWORK_READ_KBPS: {
                VmStats s = statsCollector.getVmStats(resourceId, false);
                return s != null ? s.getNetworkReadKBs() : null;
            }
            case NETWORK_WRITE_KBPS: {
                VmStats s = statsCollector.getVmStats(resourceId, false);
                return s != null ? s.getNetworkWriteKBs() : null;
            }
            case STORAGE_UTILIZATION: {
                StorageStats pool = statsCollector.getStoragePoolStats(resourceId);
                if (pool == null || pool.getCapacityBytes() <= 0) return null;
                return ((double) pool.getByteUsed() / pool.getCapacityBytes()) * 100.0;
            }
            default:
                break;
        }
        return null;
    }

    // For volume rules, resolve the attached VM and use its aggregate disk stats.
    private Double getVmDiskStat(ResourceAlertRule.ResourceType type, long resourceId, ToDoubleFunction<VmStats> extractor) {
        long vmId = resourceId;
        if (type == ResourceAlertRule.ResourceType.Volume) {
            VolumeVO vol = volumeDao.findById(resourceId);
            if (vol == null || vol.getInstanceId() == null) return null;
            vmId = vol.getInstanceId();
        }
        VmStats s = statsCollector.getVmStats(vmId, false);
        return s != null ? extractor.applyAsDouble(s) : null;
    }

    private boolean canFire(long ruleId, Long resourceId, int resetInterval) {
        ResourceAlertVO last = alertDao.findLastFiredForRule(ruleId, resourceId);
        if (last == null) return true;
        long secondsSinceLast = (System.currentTimeMillis() - last.getAlertTimestamp().getTime()) / 1000;
        return secondsSinceLast >= resetInterval;
    }

    private void fireAlert(ResourceAlertRuleVO rule, Long resourceId, double value) {
        ResourceAlertVO alert = new ResourceAlertVO(
                rule.getId(), resourceId, rule.getMetric(), value, rule.getSeverity(),
                rule.getMessage(), new Date());
        alertDao.persist(alert);
        logger.warn("Alert fired: rule={} metric={} resource={} value={} threshold={}",
                rule.getUuid(), rule.getMetric(), resourceId, value, rule.getThreshold());

        String subject = buildSubject(rule, resourceId, value);
        String body = buildBody(rule, resourceId, value);
        long dcId = getDataCenterId(rule.getResourceType(), resourceId);
        publishAlertEvent(dcId, subject, body);

        if (rule.isEmail()) {
            sendEmail(subject, body);
        }
    }

    private String buildSubject(ResourceAlertRuleVO rule, Long resourceId, double value) {
        return String.format("[%s] Resource Alert: %s %s %.2f on %s %s",
                rule.getSeverity().name(),
                rule.getMetric(),
                rule.getCondition().name(),
                rule.getThreshold(),
                rule.getResourceType().name(),
                resourceId);
    }

    private String buildBody(ResourceAlertRuleVO rule, Long resourceId, double value) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rule: ").append(rule.getName()).append('\n');
        sb.append("Resource Type: ").append(rule.getResourceType().name()).append('\n');
        sb.append("Resource ID: ").append(resourceId).append('\n');
        sb.append("Metric: ").append(rule.getMetric()).append('\n');
        sb.append(String.format("Condition: %s %.2f%n", rule.getCondition().name(), rule.getThreshold()));
        sb.append(String.format("Current Value: %.2f%n", value));
        sb.append("Severity: ").append(rule.getSeverity().name()).append('\n');
        if (StringUtils.isNotBlank(rule.getMessage())) {
            sb.append("Message: ").append(rule.getMessage()).append('\n');
        }
        return sb.toString();
    }

    private long getDataCenterId(ResourceAlertRule.ResourceType type, long resourceId) {
        try {
            switch (type) {
                case VirtualMachine: {
                    UserVmVO vm = userVmDao.findById(resourceId);
                    return vm != null ? vm.getDataCenterId() : 0L;
                }
                case Volume: {
                    VolumeVO vol = volumeDao.findById(resourceId);
                    return vol != null ? vol.getDataCenterId() : 0L;
                }
                case Host: {
                    HostVO host = hostDao.findById(resourceId);
                    return host != null ? host.getDataCenterId() : 0L;
                }
                case StoragePool: {
                    StoragePoolVO pool = storagePoolDao.findById(resourceId);
                    return pool != null ? pool.getDataCenterId() : 0L;
                }
                default:
                    return 0L;
            }
        } catch (Exception e) {
            return 0L;
        }
    }

    private void sendEmail(String subject, String body) {
        if (mailSender == null || ArrayUtils.isEmpty(emailRecipients)) {
            return;
        }
        SMTPMailProperties mailProps = new SMTPMailProperties();
        if (StringUtils.isNotBlank(senderAddress)) {
            mailProps.setSender(new MailAddress(senderAddress));
        }
        mailProps.setSubject(subject);
        mailProps.setContent(body);
        mailProps.setContentType("text/plain");

        Set<MailAddress> addresses = new HashSet<>();
        for (String recipient : emailRecipients) {
            if (StringUtils.isNotBlank(recipient)) {
                addresses.add(new MailAddress(recipient.trim()));
            }
        }
        mailProps.setRecipients(addresses);
        emailExecutor.execute(() -> mailSender.sendMail(mailProps));
    }

    // package-private so tests can stub it without needing a Spring context
    void publishAlertEvent(long dcId, String subject, String body) {
        try {
            AlertGenerator.publishAlertOnEventBus("RESOURCE.ALERT", dcId, null, subject, body);
        } catch (Exception e) {
            logger.warn("Failed to publish resource alert event: {}", e.getMessage());
        }
    }

    @Override
    public String getConfigComponentName() {
        return ResourceAlertManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{EVAL_INTERVAL};
    }
}
