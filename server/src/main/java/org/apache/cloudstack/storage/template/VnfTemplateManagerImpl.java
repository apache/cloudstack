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
package org.apache.cloudstack.storage.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.VNF;
import com.cloud.storage.VnfTemplateNicVO;
import com.cloud.storage.dao.VnfTemplateDetailsDao;
import com.cloud.storage.dao.VnfTemplateNicDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.command.admin.template.ListVnfTemplatesCmdByAdmin;
import org.apache.cloudstack.api.command.admin.template.RegisterVnfTemplateCmdByAdmin;
import org.apache.cloudstack.api.command.admin.template.UpdateVnfTemplateCmdByAdmin;
import org.apache.cloudstack.api.command.admin.vm.DeployVnfApplianceCmdByAdmin;
import org.apache.cloudstack.api.command.user.template.DeleteVnfTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ListVnfTemplatesCmd;
import org.apache.cloudstack.api.command.user.template.RegisterVnfTemplateCmd;
import org.apache.cloudstack.api.command.user.template.UpdateVnfTemplateCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVnfApplianceCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;


public class VnfTemplateManagerImpl extends ManagerBase implements VnfTemplateManager, PluggableService, Configurable {

    static final Logger LOGGER = Logger.getLogger(VnfTemplateManagerImpl.class);

    @Inject
    VnfTemplateDetailsDao vnfTemplateDetailsDao;
    @Inject
    VnfTemplateNicDao vnfTemplateNicDao;

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        if (!VnfTemplateAndApplianceEnabled.value()) {
            return cmdList;
        }
        cmdList.add(RegisterVnfTemplateCmd.class);
        cmdList.add(RegisterVnfTemplateCmdByAdmin.class);
        cmdList.add(ListVnfTemplatesCmd.class);
        cmdList.add(ListVnfTemplatesCmdByAdmin.class);
        cmdList.add(UpdateVnfTemplateCmd.class);
        cmdList.add(UpdateVnfTemplateCmdByAdmin.class);
        cmdList.add(DeleteVnfTemplateCmd.class);
        cmdList.add(DeployVnfApplianceCmd.class);
        cmdList.add(DeployVnfApplianceCmdByAdmin.class);
        return cmdList;
    }

    @Override
    public void persistVnfTemplate(long templateId, RegisterVnfTemplateCmd cmd) {
        persistVnfTemplateNics(templateId, cmd.getVnfNics());
        persistVnfTemplateDetails(templateId, cmd);
    }

    private void persistVnfTemplateNics(long templateId, List<VNF.VnfNic> nics) {
        for (VNF.VnfNic nic : nics) {
            VnfTemplateNicVO vnfTemplateNicVO = new VnfTemplateNicVO(templateId, nic.getDeviceId(), nic.getName(), nic.isRequired(), nic.getDescription());
            vnfTemplateNicDao.persist(vnfTemplateNicVO);
        }
    }

    private void persistVnfTemplateDetails(long templateId, RegisterVnfTemplateCmd cmd) {
        persistVnfTemplateDetails(templateId, cmd.getVnfDetails());
    }

    private void persistVnfTemplateDetails(long templateId, Map<String, String> vnfDetails) {
        for (Map.Entry<String, String> entry:  vnfDetails.entrySet()) {
            String value = entry.getValue();
            if (VNF.AccessDetail.ACCESS_METHODS.name().equalsIgnoreCase(entry.getKey())) {
                value = Arrays.stream(value.split(",")).sorted().collect(Collectors.joining(","));
            }
            vnfTemplateDetailsDao.addDetail(templateId, entry.getKey().toLowerCase(), value, true);
        }
    }

    @Override
    public void updateVnfTemplate(long templateId, UpdateVnfTemplateCmd cmd) {
        updateVnfTemplateDetails(templateId, cmd);
        updateVnfTemplateNics(templateId, cmd);
    }

    private void updateVnfTemplateDetails(long templateId, UpdateVnfTemplateCmd cmd) {
        boolean cleanupVnfDetails = cmd.isCleanupVnfDetails();
        if (cleanupVnfDetails) {
            vnfTemplateDetailsDao.removeDetails(templateId);
        } else if (MapUtils.isNotEmpty(cmd.getVnfDetails())) {
            vnfTemplateDetailsDao.removeDetails(templateId);
            persistVnfTemplateDetails(templateId, cmd.getVnfDetails());
        }
    }

    private void updateVnfTemplateNics(long templateId, UpdateVnfTemplateCmd cmd) {
        List<VNF.VnfNic> nics = cmd.getVnfNics();
        if (CollectionUtils.isEmpty(nics)) {
            return;
        }
        vnfTemplateNicDao.deleteByTemplateId(templateId);
        persistVnfTemplateNics(templateId, nics);
    }

    @Override
    public String getConfigComponentName() {
        return VnfTemplateManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] { VnfTemplateAndApplianceEnabled };
    }

    @Override
    public void validateVnfApplianceNics(VirtualMachineTemplate template, List<Long> networkIds) {
        if (CollectionUtils.isEmpty(networkIds)) {
            throw new InvalidParameterValueException("VNF nics list is empty");
        }
        List<VnfTemplateNicVO> vnfNics = vnfTemplateNicDao.listByTemplateId(template.getId());
        for (VnfTemplateNicVO vnfNic : vnfNics) {
            if (vnfNic.getRequired() && networkIds.size() <= vnfNic.getDeviceId()) {
                throw new InvalidParameterValueException("VNF nic is required but not found: " + vnfNic);
            }
        }
    }
}