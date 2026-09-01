//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.backup;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.UserVmDao;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.cloudstack.api.response.BackupReportAccountResponse;
import org.apache.cloudstack.api.response.BackupReportDomainResponse;
import org.apache.cloudstack.api.response.BackupReportResponse;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.BackupScheduleResponse;
import org.apache.cloudstack.backup.dao.BackupReportDao;
import org.apache.cloudstack.backup.dao.BackupReportJoinDao;
import org.apache.cloudstack.backup.dao.BackupScheduleDao;
import org.apache.cloudstack.email.template.EmailTemplateVO;
import org.apache.cloudstack.email.template.dao.EmailTemplateDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.utils.mailing.MailAddress;
import org.apache.cloudstack.utils.mailing.SMTPMailProperties;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;
import org.apache.logging.log4j.ThreadContext;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BackupReportServiceImpl extends ManagerBase implements Configurable, BackupReportService {

    protected ConfigKey<Boolean> backupReportTaskEnabled = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class,
            "backup.report.task.enabled", "false", "Whether the backup report task should run or not.", true, ConfigKey.Scope.Zone);

    protected ConfigKey<Integer> backupReportPeriod = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Integer.class,
            "backup.report.period", "1440", "The period of time, in minutes, between two executions of the backup report task. The report will always contain all information" +
            " regarding backups between the last execution and the current execution. If the task was disabled, the report will contain information up to the period configured.",
            true, ConfigKey.Scope.Global);

    protected ConfigKey<Integer> backupReportTimeout = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Integer.class, "backup.report.timeout", "10", "Timeout, in minutes, of the " +
            "backup report task.", true, ConfigKey.Scope.Global);

    private static final String LOCK = "backup_report_lock";
    private static final String TEMPLATE_NAME = "backup_report_template";
    protected static final String LOGCONTEXTID = "logcontextid";
    private static final double GIB = (1024 * 1024 * 1024);

    private final List<Backup.Status> aliveBackupStates = List.of(Backup.Status.BackedUp, Backup.Status.Restoring);
    private final List<Backup.Status> errorBackupStates = List.of(Backup.Status.Error, Backup.Status.Failed);

    private ScheduledExecutorService scheduledExecutor;

    @Inject
    private BackupReportDao backupReportDao;

    @Inject
    private DomainDao domainDao;

    @Inject
    private AccountDao accountDao;

    @Inject
    private ProjectDao projectDao;

    @Inject
    private UserVmDao userVmDao;

    @Inject
    private DataCenterDao dataCenterDao;

    @Inject
    private BackupReportJoinDao backupReportJoinDao;

    @Inject
    private BackupScheduleDao backupScheduleDao;

    @Inject
    private ConfigurationDao configurationDao;

    @Inject
    private EmailTemplateDao emailTemplateDao;

    @Inject
    private BackupManager backupManager;

    private SMTPMailSender mailSender;
    private String senderAddress;
    private String[] recipients;
    private Configuration freemarkerConfig;

    public BackupReportServiceImpl () {
    };

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_34);

        Map<String, String> configs = configurationDao.getConfiguration("management-server", params);

        senderAddress = configs.get("alert.email.sender");
        String emailAddressList = configs.get("alert.email.addresses");
        recipients = null;
        if (emailAddressList != null) {
            recipients = emailAddressList.split(",");
        } else {
            logger.warn("No recipients set in global setting 'alert.email.addresses', skipping running backup report task.");
            return true;
        }

        String namespace = "alert.smtp";

        mailSender = new SMTPMailSender(configs, namespace);

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("BackupCompressionScheduler"));
        scheduledExecutor.schedule(this::run, 60, TimeUnit.SECONDS);
        return true;
    }

    protected void run() {
        ThreadContext.put(LOGCONTEXTID, UuidUtils.first(UUID.randomUUID().toString()));
        logger.info("Starting backup report task.");

        try {
            Transaction.execute(TransactionLegacy.CLOUD_DB, new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    buildAndSendReport();
                }
            });
        } catch (Exception e) {
            logger.error("Caught exception [{}] while executing backup report task.", e.getMessage(), e);
        }
        int nextExecutionIn = getPeriod();

        if (logger.isDebugEnabled()) {
            Calendar now = Calendar.getInstance();
            now.add(Calendar.MINUTE, nextExecutionIn);
            logger.debug("Scheduling next backup report task to [{}]", now.toInstant().toString());
        }

        scheduledExecutor.schedule(this::run, nextExecutionIn, TimeUnit.MINUTES);
        ThreadContext.pop();
    }

    @Override
    public BackupReportResponse getBackupReport(Date startDate, Date endDate, Long zoneId, Long domainId, Long accountId, Long projectId) {
        logger.info("Generating backup report from [{}] to [{}] for zone [{}], domain [{}], account [{}] or project [{}].", startDate, endDate, zoneId, domainId, accountId, projectId);
        BackupReportResponse backupReportResponse = new BackupReportResponse(startDate, endDate);
        if (projectId != null) {
            accountId = projectDao.findById(projectId).getProjectAccountId();
        }

        addReportsOnExistingBackups(startDate, endDate, zoneId, domainId, accountId, backupReportResponse);
        addReportsOnFutureBackups(startDate, endDate, zoneId, domainId, accountId, backupReportResponse);

        List<Long> zoneIds = zoneId == null ? dataCenterDao.listAllZones().stream().map(DataCenterVO::getId).collect(Collectors.toList()) : List.of(zoneId);
        for (Long zone : zoneIds) {
            BackupProvider provider = backupManager.getBackupProvider(zone);
            logger.info("Asking provider [{}] for the backup report on zone [{}].", provider.getName(), zone);
            List<Object> providerReports = provider.getBackupReport(startDate, endDate, zone, domainId, accountId);
            backupReportResponse.addProviderInfo(providerReports);
        }

        return backupReportResponse;
    }

    private void addReportsOnFutureBackups(Date startDate, Date endDate, Long zoneId, Long domainId, Long accountId, BackupReportResponse backupReportResponse) {
        Date now = new Date();
        if (endDate.before(now)) {
            logger.debug("End date [{}] is before now [{}], not adding report on future backups.", endDate, now);
            return;
        }
        logger.info("Adding reports on future backups.");
        List<BackupScheduleVO> backupSchedules = backupScheduleDao.getSchedulesToExecuteForDomainAndAccount(endDate, zoneId, domainId, accountId);
        for (BackupScheduleVO schedule : backupSchedules) {
            Date nextExecution = schedule.getScheduledTimestamp();
            if (nextExecution.before(startDate)) {
                nextExecution = DateUtil.getNextRunTime(schedule.getScheduleType(), schedule.getSchedule(), TimeZone.getTimeZone(schedule.getTimezone()).getID(), startDate);
                if (nextExecution.after(endDate)) {
                    continue;
                }
            }
            addBackupScheduleResponse(schedule, nextExecution, backupReportResponse);
        }
    }

    private void addReportsOnExistingBackups(Date startDate, Date endDate, Long zoneId, Long domainId, Long accountId, BackupReportResponse backupReportResponse) {
        List<BackupReportJoinVO> backupInfos = backupReportJoinDao.listByZoneAndDomainAndAccountAndBetweenDates(zoneId, domainId, accountId, startDate,
                endDate);
        long currentDomainId = -1;
        BackupReportDomainResponse currentDomainResponse = null;
        long currentAccountId = -1;
        BackupReportAccountResponse currentAccountResponse = null;
        logger.info("Adding reports on existing backups. A total of [{}] backups will be reported.", backupInfos.size());

        for (BackupReportJoinVO backupInfo : backupInfos) {
            if (backupInfo.getDomainId() != currentDomainId) {
                currentDomainId = backupInfo.getDomainId();
                currentDomainResponse = new BackupReportDomainResponse(backupInfo.getDomainUuid(), backupInfo.getDomainName());
                backupReportResponse.addBackupReportDomainResponse(currentDomainResponse);
                logger.debug("Adding reports for domain [{}].", backupInfo.getDomainUuid());
            }

            if (backupInfo.getAccountId() != currentAccountId) {
                currentAccountId = backupInfo.getAccountId();
                currentAccountResponse = new BackupReportAccountResponse();
                if (backupInfo.getProjectUuid() != null) {
                    currentAccountResponse.setProjectName(backupInfo.getProjectName());
                    currentAccountResponse.setProjectId(backupInfo.getProjectUuid());
                } else {
                    currentAccountResponse.setAccountName(backupInfo.getAccountName());
                    currentAccountResponse.setAccountId(backupInfo.getAccountUuid());
                }
                currentDomainResponse.addBackupReportAccountResponse(currentAccountResponse);
                logger.debug("Adding reports for account [{}].", currentAccountResponse.getAccountId());
            }

            addBackupReport(backupReportResponse, backupInfo, currentDomainResponse, currentAccountResponse);
        }
    }

    private void addBackupReport(BackupReportResponse backupReportResponse, BackupReportJoinVO backupInfo, BackupReportDomainResponse currentDomainResponse,
            BackupReportAccountResponse currentAccountResponse) {
        if (backupInfo.getStatus() == Backup.Status.BackingUp) {
            return;
        }
        logger.trace("Adding report for backup [{}].", backupInfo.getBackupUuid());
        BackupResponse backupResponse = new BackupResponse();
        backupResponse.setBackupOffering(backupInfo.getOfferingName());
        backupResponse.setId(backupInfo.getBackupUuid());
        backupResponse.setName(backupInfo.getBackupName());
        backupResponse.setVmId(backupInfo.getVmUuid());
        backupResponse.setVmName(backupInfo.getVmName());
        backupResponse.setDate(backupInfo.getDate());
        backupResponse.setZone(backupInfo.getZoneName());
        backupResponse.setZoneId(backupInfo.getZoneUuid());

        if (aliveBackupStates.contains(backupInfo.getStatus()) && backupInfo.getRemoved() == null) {
            double backupSizeInGib = backupInfo.getSize() / GIB;
            backupReportResponse.addStorageUsage(backupSizeInGib);
            currentDomainResponse.addStorageUsage(backupSizeInGib);
            currentAccountResponse.addStorageUsage(backupSizeInGib);
            currentAccountResponse.addSuccessfulBackup(backupResponse);
        } else if (errorBackupStates.contains(backupInfo.getStatus()) && backupInfo.getRemoved() == null) {
            backupResponse.setFailureReason(backupInfo.getFailureReason());
            backupResponse.setLogid(backupInfo.getLogid());
            currentAccountResponse.addFailedBackup(backupResponse);
        } else {
            backupResponse.setRemoved(backupInfo.getRemoved());
            currentAccountResponse.addDeletedBackup(backupResponse);
        }
    }

    private void addBackupScheduleResponse(BackupScheduleVO schedule, Date nextExecution, BackupReportResponse backupReportResponse) {
        BackupScheduleResponse scheduleResponse = new BackupScheduleResponse();
        logger.trace("Adding report for future execution of backup schedule [{}].", schedule.getUuid());
        scheduleResponse.setId(schedule.getUuid());
        scheduleResponse.setSchedule(nextExecution.toInstant().toString());
        scheduleResponse.setQuiesceVM(schedule.getQuiesceVM());
        scheduleResponse.setIsolated(schedule.isIsolated());

        VMInstanceVO vm = userVmDao.findById(schedule.getVmId());
        if (vm != null) {
            scheduleResponse.setVmId(vm.getUuid());
            scheduleResponse.setVmName(vm.getHostName());
        }

        AccountVO backupAccount = accountDao.findById(schedule.getAccountId());
        scheduleResponse.setAccountId(backupAccount.getUuid());
        if (backupAccount.getType() == Account.Type.PROJECT) {
            ProjectVO project = projectDao.findByProjectAccountId(backupAccount.getAccountId());
            scheduleResponse.setProjectId(project.getUuid());
            scheduleResponse.setProjectName(project.getName());
        } else {
            scheduleResponse.setAccount(backupAccount.getAccountName());
        }

        DomainVO domain = domainDao.findById(backupAccount.getDomainId());
        scheduleResponse.setDomain(domain.getName());
        scheduleResponse.setDomainid(domain.getUuid());
        backupReportResponse.addBackupScheduleResponse(scheduleResponse);
    }

    private int getPeriod() {
        return backupReportPeriod.value() < 1 ? Integer.parseInt(backupReportPeriod.defaultValue()) : backupReportPeriod.value();
    }

    protected void buildAndSendReport() {
        boolean lock = false;
        try {
            lock = backupReportDao.lockInLockTable(LOCK, 300);
            if (!lock) {
                logger.warn("Unable to get lock for backup report. Giving up.");
                return;
            }

            BackupReportVO latestReport = backupReportDao.findLatest();

            if (isTaskDisabled(latestReport)) {
                return;
            }

            if (isLastTaskRunning(latestReport)) {
                return;
            }

            int period = getPeriod();
            Calendar start = getStart(latestReport, period);
            if (start == null) {
                return;
            }

            Calendar end = Calendar.getInstance();

            BackupReportVO thisReport = new BackupReportVO(end.getTime(), true);
            thisReport = backupReportDao.persist(thisReport);

            end.add(Calendar.MINUTE, period);
            BackupReportResponse response = getBackupReport(start.getTime(), end.getTime(), null, null, null, null);

            String subject = String.format("Backup report from %s to %s", start.toInstant().toString(), end.toInstant().toString());

            EmailTemplateVO templateVO = emailTemplateDao.findByName(TEMPLATE_NAME);
            Template template = new Template("template", new StringReader(templateVO.getTemplate()), freemarkerConfig);
            StringWriter writer = new StringWriter();
            template.process(response, writer);
            String result = writer.toString();

            sendMail(subject, result);
            backupReportDao.remove(thisReport.getId());
        } catch (TemplateException | IOException e) {
            logger.error(e);
            throw new CloudRuntimeException(e);
        } finally {
            if (lock) {
                backupReportDao.unlockFromLockTable(LOCK);
            }
        }
    }

    private boolean isLastTaskRunning(BackupReportVO latestReport) {
        Calendar timeout = Calendar.getInstance();
        timeout.add(Calendar.MINUTE, -backupReportTimeout.value());
        if (latestReport != null && latestReport.getRemoved() == null) {
            if (latestReport.getCreated().before(timeout.getTime())) {
                logger.warn("Last backup report task has timed out. Will set it as removed and proceed with execution of new task.");
                backupReportDao.remove(latestReport.getId());
            } else {
                logger.debug("Last backup report task is still running. Skipping this task.");
                return true;
            }
        }
        return false;
    }

    private Calendar getStart(BackupReportVO latestReport, int period) {
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        if (latestReport == null || !latestReport.isTaskEnabled()) {
            logger.info("Last report not found or task was disabled, will get the report from the last [{}] minutes.", period);
            start.add(Calendar.MINUTE, -period);
        } else {
            Calendar lastReportStart = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            lastReportStart.setTime(latestReport.getCreated());
            if (ChronoUnit.MINUTES.between(lastReportStart.toInstant(), start.toInstant()) < period) {
                logger.debug("Last report was less then [{}] minutes ago. Skipping execution.", period);
                return null;
            }
            start.setTime(lastReportStart.getTime());
        }
        return start;
    }

    private boolean isTaskDisabled(BackupReportVO latestReport) {
        if (backupReportTaskEnabled.value()) {
            return false;
        }

        if (latestReport == null || latestReport.isTaskEnabled()) {
            Date now = new Date();
            BackupReportVO disabledTask = new BackupReportVO(now, false);
            disabledTask.setRemoved(now);
            backupReportDao.persist(disabledTask);
        }
        logger.debug("Backup report task is disabled, skipping running.");
        return true;
    }

    private void sendMail(String subject, String body) {
        SMTPMailProperties mailProps = new SMTPMailProperties();
        mailProps.setSender(new MailAddress(senderAddress));
        mailProps.setSubject(subject);
        mailProps.setContent(body);
        mailProps.setContentType("text/html; charset=utf-8");

        Set<MailAddress> addresses = new HashSet<>();
        for (String recipient : recipients) {
            addresses.add(new MailAddress(recipient));
        }

        mailProps.setRecipients(addresses);
        mailSender.sendMail(mailProps);
    }

    @Override
    public String getConfigComponentName() {
        return BackupReportServiceImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {backupReportTaskEnabled, backupReportPeriod, backupReportTimeout};
    }
}
