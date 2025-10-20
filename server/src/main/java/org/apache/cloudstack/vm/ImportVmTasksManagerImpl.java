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
package org.apache.cloudstack.vm;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.vm.ImportVMTaskVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.ImportVMTaskDao;
import com.cloud.vm.dao.UserVmDao;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.vm.ListImportVMTasksCmd;
import org.apache.cloudstack.api.response.ImportVMTaskResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.apache.cloudstack.vm.ImportVmTask.Step.CloningInstance;
import static org.apache.cloudstack.vm.ImportVmTask.Step.Completed;
import static org.apache.cloudstack.vm.ImportVmTask.Step.ConvertingInstance;
import static org.apache.cloudstack.vm.ImportVmTask.Step.Importing;
import static org.apache.cloudstack.vm.ImportVmTask.Step.Prepare;

public class ImportVmTasksManagerImpl implements ImportVmTasksManager {

    protected Logger logger = LogManager.getLogger(ImportVmTasksManagerImpl.class);

    @Inject
    private ImportVMTaskDao importVMTaskDao;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private AccountService accountService;
    @Inject
    private HostDao hostDao;
    @Inject
    private UserVmDao userVmDao;

    public ImportVmTasksManagerImpl() {
    }

    @Override
    public ListResponse<ImportVMTaskResponse> listImportVMTasks(ListImportVMTasksCmd cmd) {
        Long zoneId = cmd.getZoneId();
        Long accountId = cmd.getAccountId();
        String vcenter = cmd.getVcenter();
        Long convertHostId = cmd.getConvertHostId();
        Long startIndex = cmd.getStartIndex();
        Long pageSizeVal = cmd.getPageSizeVal();

        ImportVmTask.TaskState state = getStateFromFilter(cmd.getTasksFilter());
        Pair<List<ImportVMTaskVO>, Integer> result = importVMTaskDao.listImportVMTasks(zoneId, accountId, vcenter, convertHostId, state, startIndex, pageSizeVal);
        List<ImportVMTaskVO> tasks = result.first();

        List<ImportVMTaskResponse> responses = new ArrayList<>();
        for (ImportVMTaskVO task : tasks) {
            responses.add(createImportVMTaskResponse(task));
        }
        ListResponse<ImportVMTaskResponse> listResponses = new ListResponse<>();
        listResponses.setResponses(responses, result.second());
        return listResponses;
    }

    private ImportVmTask.TaskState getStateFromFilter(String tasksFilter) {
        if (StringUtils.isBlank(tasksFilter) || tasksFilter.equalsIgnoreCase("all")) {
            return null;
        }
        try {
            return ImportVmTask.TaskState.getValue(tasksFilter);
        } catch (IllegalArgumentException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Invalid value for task state: %s", tasksFilter));
        }
    }

    @Override
    public ImportVmTask createImportVMTaskRecord(DataCenter zone, Account owner, long userId, String displayName, String vcenter, String datacenterName, String sourceVMName, Host convertHost, Host importHost) {
        logger.debug("Creating import VM task entry for VM: {} for account {} on zone {} " +
                        "from the vCenter: {} / datacenter: {} / source VM: {}",
                sourceVMName, owner.getAccountName(), zone.getName(), displayName, vcenter, datacenterName);
        ImportVMTaskVO importVMTaskVO = new ImportVMTaskVO(zone.getId(), owner.getAccountId(), userId, displayName,
                vcenter, datacenterName, sourceVMName, convertHost.getId(), importHost.getId());
        importVMTaskVO.setState(ImportVmTask.TaskState.Running);
        return importVMTaskDao.persist(importVMTaskVO);
    }

    private String getStepDescription(ImportVMTaskVO importVMTaskVO, Host convertHost, Host importHost,
                                      ImportVMTaskVO.Step step, Date updatedDate) {
        String sourceVMName = importVMTaskVO.getSourceVMName();
        String vcenter = importVMTaskVO.getVcenter();
        String datacenter = importVMTaskVO.getDatacenter();

        StringBuilder stringBuilder = new StringBuilder();
        if (Completed == step) {
            stringBuilder.append("Completed at ").append(DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), updatedDate));
        } else {
            if (CloningInstance == step) {
                stringBuilder.append(String.format("Cloning source instance: %s on vCenter: %s / datacenter: %s", sourceVMName, vcenter, datacenter));
            } else if (ConvertingInstance == step) {
                stringBuilder.append(String.format("Converting the cloned VMware instance to a KVM instance on the host: %s", convertHost.getName()));
            } else if (Importing == step) {
                stringBuilder.append(String.format("Importing the converted KVM instance on the host: %s", importHost.getName()));
            } else if (Prepare == step) {
                stringBuilder.append("Preparing to convert Vmware instance");
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public void updateImportVMTaskStep(ImportVmTask importVMTask, DataCenter zone, Account owner, Host convertHost,
                                       Host importHost, Long vmId, ImportVmTask.Step step) {
        ImportVMTaskVO importVMTaskVO = (ImportVMTaskVO) importVMTask;
        logger.debug("Updating import VM task entry for VM: {} for account {} on zone {} " +
                        "from the vCenter: {} / datacenter: {} / source VM: {} to step: {}",
                importVMTaskVO.getSourceVMName(), owner.getAccountName(), zone.getName(), importVMTaskVO.getDisplayName(),
                importVMTaskVO.getVcenter(), importVMTaskVO.getDatacenter(), step);
        Date updatedDate = DateUtil.now();
        String description = getStepDescription(importVMTaskVO, convertHost, importHost, step, updatedDate);
        importVMTaskVO.setStep(step);
        importVMTaskVO.setDescription(description);
        importVMTaskVO.setUpdated(updatedDate);
        if (Completed == step) {
            Duration duration = Duration.between(importVMTaskVO.getCreated().toInstant(), updatedDate.toInstant());
            importVMTaskVO.setDuration(duration.toMillis());
            importVMTaskVO.setVmId(vmId);
            importVMTaskVO.setState(ImportVmTask.TaskState.Completed);
        }
        importVMTaskDao.update(importVMTaskVO.getId(), importVMTaskVO);
    }

    @Override
    public void updateImportVMTaskErrorState(ImportVmTask importVMTask, ImportVmTask.TaskState state, String errorMsg) {
        ImportVMTaskVO importVMTaskVO = (ImportVMTaskVO) importVMTask;
        Date updatedDate = DateUtil.now();
        importVMTaskVO.setUpdated(updatedDate);
        importVMTaskVO.setState(state);
        importVMTaskVO.setDescription(errorMsg);
        importVMTaskDao.update(importVMTaskVO.getId(), importVMTaskVO);
    }

    private ImportVMTaskResponse createImportVMTaskResponse(ImportVMTaskVO task) {
        ImportVMTaskResponse response = new ImportVMTaskResponse();
        DataCenterVO zone = dataCenterDao.findById(task.getZoneId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }
        Account account = accountService.getAccount(task.getAccountId());
        if (account != null) {
            response.setAccountId(account.getUuid());
            response.setAccountName(account.getAccountName());
        }
        response.setVcenter(task.getVcenter());
        response.setDatacenterName(task.getDatacenter());
        response.setSourceVMName(task.getSourceVMName());
        response.setDisplayName(task.getDisplayName());
        response.setStep(getStepDisplayField(task.getStep()));
        response.setDescription(task.getDescription());
        response.setState(task.getState().name());

        Date updated = task.getUpdated();
        Date currentDate = new Date();

        if (updated != null) {
            if (ImportVmTask.TaskState.Running == task.getState()) {
                Duration stepDuration = Duration.between(updated.toInstant(), currentDate.toInstant());
                response.setStepDuration(getDurationDisplay(stepDuration.toMillis()));
            } else {
                Duration totalDuration = Duration.between(task.getCreated().toInstant(), updated.toInstant());
                response.setDuration(getDurationDisplay(totalDuration.toMillis()));
            }
        }

        HostVO host = hostDao.findById(task.getConvertHostId());
        if (host != null) {
            response.setConvertInstanceHostId(host.getUuid());
            response.setConvertInstanceHostName(host.getName());
        }
        if (task.getVmId() != null) {
            UserVmVO userVm = userVmDao.findById(task.getVmId());
            response.setVirtualMachineId(userVm.getUuid());
        }
        response.setCreated(task.getCreated());
        response.setLastUpdated(task.getUpdated());
        response.setObjectName("importvmtask");
        return response;
    }

    protected String getStepDisplayField(ImportVMTaskVO.Step step) {
        int totalSteps = ImportVMTaskVO.Step.values().length;
        return String.format("[%s/%s] %s", step.ordinal() + 1, totalSteps, step.name());
    }

    protected static String getDurationDisplay(Long durationMs) {
        if (durationMs == null) {
            return null;
        }
        long hours = durationMs / (1000 * 60 * 60);
        long minutes = (durationMs / (1000 * 60)) % 60;
        long seconds = (durationMs / 1000) % 60;

        StringBuilder result = new StringBuilder();
        if (hours > 0) {
            result.append(String.format("%s hs ", hours));
        }
        if (minutes > 0) {
            result.append(String.format("%s min ", minutes));
        }
        if (seconds > 0) {
            result.append(String.format("%s secs", seconds));
        }
        return result.toString();
    }
}
