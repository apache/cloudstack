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
package org.apache.cloudstack.backup.backroll;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.Backup.Metric;
import org.apache.cloudstack.backup.backroll.BackrollService.NotOkBodyException;
import org.apache.cloudstack.backup.backroll.model.BackrollBackup;
import org.apache.cloudstack.backup.backroll.model.BackrollBackupMetrics;
import org.apache.cloudstack.backup.backroll.model.BackrollOffering;
import org.apache.cloudstack.backup.backroll.model.BackrollTaskStatus;
import org.apache.cloudstack.backup.backroll.model.BackrollVmBackup;
import org.apache.cloudstack.backup.backroll.model.response.BackrollTaskRequestResponse;
import org.apache.cloudstack.backup.backroll.model.response.TaskState;
import org.apache.cloudstack.backup.backroll.model.response.archive.BackrollBackupsFromVMResponse;
import org.apache.cloudstack.backup.backroll.model.response.backup.BackrollBackupStatusResponse;
import org.apache.cloudstack.backup.backroll.model.response.backup.BackrollBackupStatusSuccessResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.backup.BackrollBackupMetricsResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine.BackrollVmMetricsResponse;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine.CacheStats;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups.BackupInfos;
import org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups.VirtualMachineBackupsResponse;
import org.apache.cloudstack.backup.backroll.model.response.policy.BackrollBackupPolicyResponse;
import org.apache.cloudstack.backup.backroll.model.response.policy.BackupPoliciesResponse;
import org.apache.cloudstack.backup.backroll.utils.BackrollApiException;
import org.apache.cloudstack.backup.backroll.utils.BackrollHttpClientProvider;

import org.apache.commons.lang3.StringUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joda.time.DateTime;

import org.json.JSONException;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;



public class BackrollClient {

    protected Logger logger = LogManager.getLogger(BackrollClient.class);

    private BackrollHttpClientProvider httpProvider;

    public BackrollClient(BackrollHttpClientProvider httpProvider)
            throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {

        this.httpProvider = httpProvider;
    }

    public String startBackupJob(final String jobId) throws IOException, BackrollApiException {
        logger.info("Trying to start backup for Backroll job: {}", jobId);
        String backupJob = "";
        BackrollTaskRequestResponse requestResponse = httpProvider.post(String.format("/tasks/singlebackup/%s", jobId), null, BackrollTaskRequestResponse.class);
        backupJob = requestResponse.location.replace("/api/v1/status/", "");
        return StringUtils.isEmpty(backupJob) ? null : backupJob;
    }

    public String getBackupOfferingUrl() throws IOException, BackrollApiException  {
        logger.info("Trying to get backroll backup policies url");
        String url = "";
        BackrollTaskRequestResponse requestResponse = httpProvider.get("/backup_policies", BackrollTaskRequestResponse.class);
        logger.info("BackrollClient:getBackupOfferingUrl:Apres Parse:  " + requestResponse.location);
        url = requestResponse.location.replace("/api/v1", "");
        return StringUtils.isEmpty(url) ? null : url;
    }

    public List<BackupOffering> getBackupOfferings(String idTask) throws BackrollApiException, IOException {
        logger.info("Trying to list backroll backup policies");
        final List<BackupOffering> policies = new ArrayList<>();
        BackupPoliciesResponse backupPoliciesResponse = httpProvider.waitGet(idTask, BackupPoliciesResponse.class);
        logger.info("BackrollClient:getBackupOfferings:Apres Parse:  " + backupPoliciesResponse.backupPolicies.get(0).name);
        for (final BackrollBackupPolicyResponse policy : backupPoliciesResponse.backupPolicies) {
            policies.add(new BackrollOffering(policy.name, policy.id));
        }

        return policies;
    }

    public boolean restoreVMFromBackup(final String vmId, final String backupName) throws IOException, BackrollApiException  {
        logger.info("Start restore backup with backroll with backup {} for vm {}", backupName, vmId);

        boolean isRestoreOk = false;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("virtual_machine_id", vmId);
            jsonBody.put("backup_name", backupName);
            jsonBody.put("storage", "");
            jsonBody.put("mode", "single");

        } catch (JSONException e) {
            logger.error("Backroll Error: {}", e.getMessage());
        }

        BackrollTaskRequestResponse requestResponse = httpProvider.post(String.format("/tasks/restore/%s", vmId), jsonBody, BackrollTaskRequestResponse.class);
        String urlToRequest = requestResponse.location.replace("/api/v1", "");

        String result = httpProvider.waitGetWithoutParseResponse(urlToRequest);
        if(result.contains("SUCCESS")) {
            logger.debug("RESTORE SUCCESS content : " + result);
            logger.debug("RESTORE SUCCESS");
            isRestoreOk = true;
        }

        return isRestoreOk;
    }

    public BackrollTaskStatus checkBackupTaskStatus(String taskId) throws IOException, BackrollApiException {
        logger.info("Trying to get backup status for Backroll task: {}", taskId);

        BackrollTaskStatus status = new BackrollTaskStatus();

        String backupResponse = httpProvider.getWithoutParseResponse("/status/" + taskId);

        if (backupResponse.contains(TaskState.FAILURE) || backupResponse.contains(TaskState.PENDING)) {
            BackrollBackupStatusResponse backupStatusRequestResponse = new ObjectMapper().readValue(backupResponse, BackrollBackupStatusResponse.class);
            status.setState(backupStatusRequestResponse.state);
        } else {
            BackrollBackupStatusSuccessResponse backupStatusSuccessRequestResponse = new ObjectMapper().readValue(backupResponse, BackrollBackupStatusSuccessResponse.class);
            status.setState(backupStatusSuccessRequestResponse.state);
            status.setInfo(backupStatusSuccessRequestResponse.info);
        }

        return StringUtils.isEmpty(status.getState()) ? null : status;
    }

    public boolean deleteBackup(final String vmId, final String backupName) throws IOException, BackrollApiException{
        logger.info("Trying to delete backup {} for vm {} using Backroll", vmId, backupName);
        boolean isBackupDeleted = false;

        BackrollTaskRequestResponse requestResponse =  httpProvider.delete(String.format("/virtualmachines/%s/backups/%s", vmId, backupName), BackrollTaskRequestResponse.class);
        String urlToRequest = requestResponse.location.replace("/api/v1", "");

        BackrollBackupsFromVMResponse backrollBackupsFromVMResponse = httpProvider.waitGet(urlToRequest, BackrollBackupsFromVMResponse.class);
        logger.debug(backrollBackupsFromVMResponse.state);
        isBackupDeleted = backrollBackupsFromVMResponse.state.equals(TaskState.SUCCESS);

        return isBackupDeleted;
    }

    public Metric getVirtualMachineMetrics(final String vmId) throws IOException, BackrollApiException {
        logger.info("Trying to retrieve virtual machine metric from Backroll for vm {}", vmId);

        Metric metric = new Metric(0L, 0L);

        BackrollTaskRequestResponse requestResponse = httpProvider.get(String.format("/virtualmachines/%s/repository", vmId), BackrollTaskRequestResponse.class);

        String urlToRequest = requestResponse.location.replace("/api/v1", "");

        BackrollVmMetricsResponse vmMetricsResponse = httpProvider.waitGet(urlToRequest, BackrollVmMetricsResponse.class);

        if (vmMetricsResponse != null && vmMetricsResponse.state.equals(TaskState.SUCCESS)) {
            logger.debug("SUCCESS ok");
            CacheStats stats = null;
            try {
                stats = vmMetricsResponse.infos.cache.stats;
            } catch (NullPointerException e) {
            }
            if (stats != null) {
                long size = Long.parseLong(stats.totalSize);
                metric = new Metric(size, size);
            }
        }

        return metric;
    }

    public BackrollBackupMetrics getBackupMetrics(String vmId, String backupId) throws IOException, BackrollApiException {
        logger.info("Trying to get backup metrics for VM: {}, and backup: {}", vmId, backupId);

        BackrollBackupMetrics metrics = null;

        BackrollTaskRequestResponse requestResponse = httpProvider.get(String.format("/virtualmachines/%s/backups/%s", vmId, backupId), BackrollTaskRequestResponse.class);

        String urlToRequest = requestResponse.location.replace("/api/v1", "");

        logger.debug(urlToRequest);

        BackrollBackupMetricsResponse metricsResponse = httpProvider.waitGet(urlToRequest, BackrollBackupMetricsResponse.class);
        if (metricsResponse.info != null) {
            metrics = new BackrollBackupMetrics(Long.parseLong(metricsResponse.info.originalSize),
                    Long.parseLong(metricsResponse.info.deduplicatedSize));
        }
        return metrics;
    }

    public List<BackrollVmBackup> getAllBackupsfromVirtualMachine(String vmId) throws BackrollApiException, IOException {
        logger.info("Trying to retrieve all backups for vm {}", vmId);

        List<BackrollVmBackup> backups = new ArrayList<BackrollVmBackup>();

        BackrollTaskRequestResponse requestResponse = httpProvider.get(String.format("/virtualmachines/%s/backups", vmId), BackrollTaskRequestResponse.class);

        String urlToRequest = requestResponse.location.replace("/api/v1", "");
        logger.debug(urlToRequest);
        VirtualMachineBackupsResponse virtualMachineBackupsResponse = httpProvider.waitGet(urlToRequest, VirtualMachineBackupsResponse.class);

        if (virtualMachineBackupsResponse.state.equals(TaskState.SUCCESS)) {
            if (virtualMachineBackupsResponse.info.archives.size() > 0) {
                for (BackupInfos infos : virtualMachineBackupsResponse.info.archives) {
                    var dateStart = new DateTime(infos.start);
                    backups.add(new BackrollVmBackup(infos.id, infos.name, dateStart.toDate()));
                }
            }
        }

        return backups;
    }
}