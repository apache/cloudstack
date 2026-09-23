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
import java.util.HashMap;
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

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleDao;
import org.apache.cloudstack.resourcealert.dao.ResourceAlertRuleWebhookDao;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleVO;
import org.apache.cloudstack.resourcealert.vo.ResourceAlertVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.cloudstack.utils.mailing.MailAddress;
import org.apache.cloudstack.utils.mailing.SMTPMailProperties;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;
import org.apache.cloudstack.webhook.WebhookHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.AlertGenerator;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.server.ResourceTag;
import com.cloud.server.StatsCollector;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageStats;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.GlobalLock;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.UserVmDao;
import com.google.gson.JsonObject;

public class ResourceAlertManagerImpl extends ManagerBase implements ResourceAlertManager, Configurable {

    static final String ALERT_EVENT_TYPE = "RESOURCE.ALERT";

    static final ConfigKey<Integer> EVAL_INTERVAL = new ConfigKey<>("Advanced", Integer.class,
            "resourcealert.evaluation.interval", "60",
            "Interval in seconds between resource alert rule evaluations", false);

    public static final ConfigKey<Integer> RULES_PER_ACCOUNT_LIMIT = new ConfigKey<>("Advanced", Integer.class,
            "resourcealert.per.user.limit", "20",
            "Maximum number of resource alert rules an account can own; 0 = unlimited", true, ConfigKey.Scope.Account);

    public static final ConfigKey<Integer> DEFAULT_RESET_INTERVAL = new ConfigKey<>("Advanced", Integer.class,
            "resourcealert.repeat.interval.default", "600",
            "Default minimum seconds between repeat firings of a resource alert rule, used when a rule does not set one", true);

    @Inject ResourceAlertRuleDao ruleDao;
    @Inject ResourceAlertDao alertDao;
    @Inject ResourceAlertRuleWebhookDao ruleWebhookDao;
    @Inject UserVmDao userVmDao;
    @Inject HostDao hostDao;
    @Inject PrimaryDataStoreDao storagePoolDao;
    @Inject VolumeDao volumeDao;
    @Inject StatsCollector statsCollector;
    @Inject ConfigurationDao configDao;
    @Inject ResourceTagDao resourceTagDao;
    @Inject AccountDao accountDao;
    @Inject DomainDao domainDao;
    @Inject ManagementServerHostDao managementServerHostDao;

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
        String emailList = configDao.getValue("alert.email.addresses");
        if (StringUtils.isNotBlank(emailList)) {
            emailRecipients = emailList.split(",");
        }
        senderAddress = configDao.getValue("alert.email.sender");

        Map<String, String> smtpConfigs = new HashMap<>();
        for (String key : new String[]{
                "alert.smtp.host", "alert.smtp.port", "alert.smtp.useAuth",
                "alert.smtp.username", "alert.smtp.password", "alert.smtp.useStartTLS",
                "alert.smtp.enabledSecurityProtocols", "alert.smtp.timeout", "alert.smtp.connectiontimeout"}) {
            String val = configDao.getValue(key);
            if (val != null) smtpConfigs.put(key, val);
        }
        mailSender = new SMTPMailSender(smtpConfigs, "alert.smtp");

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
        executor.scheduleAtFixedRate(new EvaluationTask(), interval, interval, TimeUnit.SECONDS);
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

    // Every management server collects stats for all hosts, so only one may evaluate or alerts fire once per server.
    boolean isEvaluatingServer() {
        ManagementServerHostVO msHost = managementServerHostDao.findOneByLongestRuntime();
        return msHost != null && msHost.getMsid() == ManagementServerNode.getManagementServerId();
    }

    class EvaluationTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock lock = GlobalLock.getInternLock("ResourceAlertEvaluation");
            try {
                if (!lock.lock(5)) {
                    return;
                }
                try {
                    if (isEvaluatingServer()) {
                        evaluateRules();
                    }
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                logger.warn("Failed to evaluate resource alert rules", e);
            } finally {
                lock.releaseRef();
            }
        }
    }

    private void evaluateRule(ResourceAlertRuleVO rule) {
        ResourceAlertMetric metric = ResourceAlertMetric.valueOf(rule.getMetric());
        boolean isGeneric = rule.getResourceId() == null;
        for (Long resourceId : getResourceIds(rule)) {
            try {
                if (isGeneric) {
                    if (isOptedOut(rule.getResourceType(), resourceId)) continue;
                    if (ruleDao.existsSpecificRule(rule.getResourceType(), rule.getMetric(), resourceId)) continue;
                }
                Double value = getMetricValue(rule.getResourceType(), metric, resourceId);
                if (value == null || value < 0) {
                    continue;
                }
                if (rule.getCondition().evaluate(value, rule.getThreshold())
                        && canFire(rule.getId(), resourceId, rule.getResetInterval())) {
                    fireAlert(rule, resourceId, value);
                }
            } catch (Exception e) {
                logger.warn("Failed to evaluate resource alert rule {} for resource {}", rule.getUuid(), resourceId, e);
            }
        }
    }

    private boolean isOptedOut(ResourceAlertRule.ResourceType type, long resourceId) {
        ResourceTag.ResourceObjectType objType = null;
        if (type == ResourceAlertRule.ResourceType.VirtualMachine) {
            objType = ResourceTag.ResourceObjectType.UserVm;
        } else if (type == ResourceAlertRule.ResourceType.Volume) {
            objType = ResourceTag.ResourceObjectType.Volume;
        }
        if (objType == null) return false;
        ResourceTag tag = resourceTagDao.findByKey(resourceId, objType, "resource.alert.opt.out");
        return tag != null && "true".equalsIgnoreCase(tag.getValue());
    }

    private List<Long> getResourceIds(ResourceAlertRuleVO rule) {
        if (rule.getResourceId() != null) {
            return Collections.singletonList(rule.getResourceId());
        }
        switch (rule.getResourceType()) {
            case VirtualMachine: {
                Pair<Long, List<Long>> scope = getGenericRuleScope(rule);
                return scope == null ? Collections.emptyList() : userVmDao.listIdsByAccountOrDomainsAndState(
                        scope.first(), scope.second(), VirtualMachine.State.Running);
            }
            case Volume: {
                Pair<Long, List<Long>> scope = getGenericRuleScope(rule);
                return scope == null ? Collections.emptyList() : volumeDao.listIdsByAccountOrDomainsAndState(
                        scope.first(), scope.second(), Volume.State.Ready);
            }
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

    // Root admin rules cover the whole cloud, domain admin rules their domain tree, other rules their own account.
    Pair<Long, List<Long>> getGenericRuleScope(ResourceAlertRule rule) {
        Account owner = accountDao.findById(rule.getAccountId());
        if (owner == null) {
            return null;
        }
        if (Account.Type.ADMIN.equals(owner.getType())) {
            return new Pair<>(null, null);
        }
        if (Account.Type.DOMAIN_ADMIN.equals(owner.getType()) || Account.Type.RESOURCE_DOMAIN_ADMIN.equals(owner.getType())) {
            return new Pair<>(null, domainDao.getDomainAndChildrenIds(owner.getDomainId()));
        }
        return new Pair<>(owner.getId(), null);
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
                if (type == ResourceAlertRule.ResourceType.Host) {
                    HostStats s = statsCollector.getHostStats(resourceId);
                    return s != null ? s.getNetworkReadKBs() : null;
                }
                VmStats s = statsCollector.getVmStats(resourceId, false);
                return s != null ? s.getNetworkReadKBs() : null;
            }
            case NETWORK_WRITE_KBPS: {
                if (type == ResourceAlertRule.ResourceType.Host) {
                    HostStats s = statsCollector.getHostStats(resourceId);
                    return s != null ? s.getNetworkWriteKBs() : null;
                }
                VmStats s = statsCollector.getVmStats(resourceId, false);
                return s != null ? s.getNetworkWriteKBs() : null;
            }
            case STORAGE_USED_IOPS: {
                // only reported by storage drivers that track IOPS
                StorageStats pool = statsCollector.getStoragePoolStats(resourceId);
                return pool != null && pool.getUsedIops() != null ? pool.getUsedIops().doubleValue() : null;
            }
            case VOLUME_SIZE_GB: {
                VolumeStats s = getVolumeStats(resourceId);
                return s != null ? s.getPhysicalSize() / (1024.0 * 1024.0 * 1024.0) : null;
            }
            case LOAD_AVERAGE: {
                HostStats s = statsCollector.getHostStats(resourceId);
                return s != null ? s.getLoadAverage() : null;
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

    // Stats are keyed by path, except OVA volumes which are keyed by chain info.
    private VolumeStats getVolumeStats(long volumeId) {
        VolumeVO vol = volumeDao.findById(volumeId);
        if (vol == null) return null;
        String locator = Storage.ImageFormat.OVA.equals(vol.getFormat()) ? vol.getChainInfo() : vol.getPath();
        return locator != null ? statsCollector.getVolumeStats(locator) : null;
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

        String subject = buildSubject(rule, resourceId, value);
        String body = buildBody(rule, resourceId, value);
        long dcId = getDataCenterId(rule.getResourceType(), resourceId);
        publishAlertEvent(dcId, subject, body);
        deliverToWebhooks(rule, alert, resourceId, value);

        if (rule.isEmail()) {
            sendEmail(subject, body);
        }

        logger.warn("Alert fired: rule={} metric={} resource={} value={} threshold={}",
                rule.getUuid(), rule.getMetric(), resourceId, value, rule.getThreshold());
    }

    protected WebhookHelper getWebhookHelper() {
        try {
            return ComponentContext.getDelegateComponentOfType(WebhookHelper.class);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    private void deliverToWebhooks(ResourceAlertRuleVO rule, ResourceAlertVO alert, Long resourceId, double value) {
        List<Long> webhookIds = ruleWebhookDao.listWebhookIdsByRule(rule.getId());
        if (webhookIds.isEmpty()) {
            return;
        }
        WebhookHelper webhookHelper = getWebhookHelper();
        if (webhookHelper == null) {
            logger.warn("Unable to deliver alert for rule {} to webhooks as the webhook plugin is not enabled", rule.getUuid());
            return;
        }
        webhookHelper.deliverToWebhooks(webhookIds, rule.getAccountId(), ALERT_EVENT_TYPE,
                buildWebhookPayload(rule, alert, resourceId, value));
    }

    String buildWebhookPayload(ResourceAlertRuleVO rule, ResourceAlertVO alert, Long resourceId, double value) {
        JsonObject payload = new JsonObject();
        payload.addProperty("event", ALERT_EVENT_TYPE);
        payload.addProperty("id", alert.getUuid());
        payload.addProperty("ruleid", rule.getUuid());
        payload.addProperty("rulename", rule.getName());
        payload.addProperty("resourcetype", rule.getResourceType().name());
        payload.addProperty("resourceid", getResourceUuid(rule.getResourceType(), resourceId));
        payload.addProperty("metric", rule.getMetric());
        payload.addProperty("condition", rule.getCondition().name());
        payload.addProperty("threshold", rule.getThreshold());
        payload.addProperty("value", value);
        payload.addProperty("severity", rule.getSeverity().name());
        payload.addProperty("message", rule.getMessage());
        payload.addProperty("timestamp", alert.getAlertTimestamp().toInstant().toString());
        return payload.toString();
    }

    private String getResourceUuid(ResourceAlertRule.ResourceType type, Long resourceId) {
        if (resourceId == null) {
            return null;
        }
        Identity resource;
        switch (type) {
            case VirtualMachine:
                resource = userVmDao.findByIdIncludingRemoved(resourceId);
                break;
            case Volume:
                resource = volumeDao.findByIdIncludingRemoved(resourceId);
                break;
            case Host:
                resource = hostDao.findByIdIncludingRemoved(resourceId);
                break;
            case StoragePool:
                resource = storagePoolDao.findByIdIncludingRemoved(resourceId);
                break;
            default:
                resource = null;
        }
        return resource != null ? resource.getUuid() : null;
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
            AlertGenerator.publishAlertOnEventBus(ALERT_EVENT_TYPE, dcId, null, subject, body);
        } catch (Exception e) {
            logger.warn("Failed to publish resource alert on the event bus", e);
        }
    }

    @Override
    public String getConfigComponentName() {
        return ResourceAlertManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{EVAL_INTERVAL, RULES_PER_ACCOUNT_LIMIT, DEFAULT_RESET_INTERVAL};
    }
}
