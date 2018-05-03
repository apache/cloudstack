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
package org.apache.cloudstack.framework.backuprecovery.impl;

import com.cloud.agent.AgentManager;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.framework.backuprecovery.BackupRecoveryManager;
import org.apache.cloudstack.framework.backuprecovery.agent.api.AssignVMToBackupPolicyAnswer;
import org.apache.cloudstack.framework.backuprecovery.agent.api.AssignVMToBackupPolicyCommand;
import org.apache.cloudstack.framework.backuprecovery.agent.api.CheckBackupPolicyAnswer;
import org.apache.cloudstack.framework.backuprecovery.agent.api.CheckBackupPolicyCommand;
import org.apache.cloudstack.framework.backuprecovery.agent.api.ListBackupPoliciesAnswer;
import org.apache.cloudstack.framework.backuprecovery.agent.api.ListBackupPoliciesCommand;
import org.apache.cloudstack.framework.backuprecovery.agent.api.to.BackupPolicyTO;
import org.apache.cloudstack.framework.backuprecovery.api.AddBackupRecoveryPolicyCmd;
import org.apache.cloudstack.framework.backuprecovery.api.AddBackupRecoveryProviderCmd;
import org.apache.cloudstack.framework.backuprecovery.api.AssignBackupPolicyCmd;
import org.apache.cloudstack.framework.backuprecovery.api.DeleteBackupRecoveryProviderCmd;
import org.apache.cloudstack.framework.backuprecovery.api.ListBackupRecoveryPoliciesCmd;
import org.apache.cloudstack.framework.backuprecovery.api.ListBackupRecoveryProviderPoliciesCmd;
import org.apache.cloudstack.framework.backuprecovery.api.ListBackupRecoveryProvidersCmd;
import org.apache.cloudstack.framework.backuprecovery.api.response.BackupPolicyResponse;
import org.apache.cloudstack.framework.backuprecovery.api.response.BackupRecoveryProviderPolicyResponse;
import org.apache.cloudstack.framework.backuprecovery.api.response.BackupRecoveryProviderResponse;
import org.apache.cloudstack.framework.backuprecovery.dao.BackupPoliciesDao;
import org.apache.cloudstack.framework.backuprecovery.dao.BackupRecoveryProviderDao;
import org.apache.cloudstack.framework.backuprecovery.helper.BackupRecoveryHelper;
import org.apache.cloudstack.framework.backuprecovery.resource.BackupRecoveryResource;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

public class BackupRecoveryManagerImpl extends ManagerBase implements BackupRecoveryManager {

    @Inject
    BackupRecoveryProviderDao backupRecoveryProviderDao;
    @Inject
    HostDetailsDao hostDetailsDao;
    @Inject
    ResourceManager resourceManager;
    @Inject
    HostDao hostDao;
    @Inject
    DataCenterDao dataCenterDao;
    @Inject
    BackupPoliciesDao backupPoliciesDao;
    @Inject
    AgentManager agentManager;
    @Inject
    VMInstanceDao vmInstanceDao;

    private static final Logger s_logger = Logger.getLogger(BackupRecoveryManagerImpl.class);

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        return true;
    }

    @Override
    public BackupRecoveryProviderVO addBackupRecoveryProvider(AddBackupRecoveryProviderCmd cmd) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        String name = cmd.getName();
        String url = cmd.getUrl();
        String username = cmd.getUsername();
        String password = cmd.getPassword();
        Long zoneId = cmd.getZoneId();
        String providerName = cmd.getProvider();

        if (!BackupRecoveryHelper.isProviderSupported(providerName)) {
            throw new InvalidParameterException("Unsopported provider: " + providerName);
        }

        s_logger.debug("Registering a new Backup and Recovery provider on zone: " + zoneId + " - url: " + url + " - name: " + name
                        + " - provider: " + providerName);
        BackupRecoveryResource resource = BackupRecoveryHelper.getResource(providerName);

        Map<String, String> params = new HashMap<String, String>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", String.valueOf(zoneId));
        params.put("name", name + " - " + url);
        //params.put("ip", cmd.getHost());
        params.put("adminuser", username);
        params.put("adminpass", password);
        params.put("url", url);

        Map<String, Object> hostdetails = new HashMap<String, Object>();
        hostdetails.putAll(params);
        try {
            resource.configure(name, hostdetails);
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        final Host host = resourceManager.addHost(zoneId, resource, Host.Type.BackupRecovery, params);
        if (host != null) {
            return Transaction.execute(new TransactionCallback<BackupRecoveryProviderVO>() {
                @Override
                public BackupRecoveryProviderVO doInTransaction(TransactionStatus status) {
                    BackupRecoveryProviderVO providerVO = new BackupRecoveryProviderVO(host.getId(), name, zoneId, url, providerName);
                    providerVO = backupRecoveryProviderDao.persist(providerVO);

                    DetailVO detail = new DetailVO(host.getId(), "providerid", String.valueOf(providerVO.getId()));
                    hostDetailsDao.persist(detail);

                    return providerVO;
                }
            });
        } else {
            throw new CloudRuntimeException("Failed to add the Backup and Recovery provider " + providerName + " - " + name);
        }
    }

    @Override
    public boolean deleteBackupRecoveryProvider(DeleteBackupRecoveryProviderCmd cmd) {
        Long providerId = cmd.getProviderId();
        BackupRecoveryProviderVO providerVO = backupRecoveryProviderDao.findById(providerId);
        if (providerVO == null) {
            throw new InvalidParameterValueException("Could not find a Backup and Recovery provider with id: " + providerId);
        }

        HostVO host = hostDao.findById(providerVO.getHostId());

        host.setResourceState(ResourceState.Maintenance);
        hostDao.update(host.getId(), host);
        resourceManager.deleteHost(host.getId(), false, false);

        backupRecoveryProviderDao.remove(providerId);
        return true;
    }

    @Override
    public List<BackupRecoveryProviderVO> listBackupRecoveryProviders(ListBackupRecoveryProvidersCmd cmd) {
        Long zoneId = cmd.getZoneId();
        String providerName = cmd.getProvider();

        if (zoneId == null) {
            throw new InvalidParameterException("Must specify a zone id");
        }

        if (StringUtils.isNotBlank(providerName)) {
            if (!BackupRecoveryHelper.isProviderSupported(providerName)) {
                throw new InvalidParameterException("Unsopported provider: " + providerName);
            }
            backupRecoveryProviderDao.listByZoneAndProvider(zoneId, providerName);
        }
        return backupRecoveryProviderDao.listByZone(zoneId);
    }

    @Override
    public BackupRecoveryProviderResponse createBackupRecoveryProviderResponse(BackupRecoveryProviderVO vo) {
        HostVO host = hostDao.findById(vo.getHostId());
        DataCenterVO dc = dataCenterDao.findById(vo.getZoneId());

        BackupRecoveryProviderResponse response = new BackupRecoveryProviderResponse();
        response.setHostId(host.getUuid());
        response.setId(vo.getUuid());
        response.setName(vo.getName());
        response.setProviderName(vo.getProviderName());
        response.setZoneId(dc.getUuid());
        return response;
    }

    @Override
    public BackupPolicyResponse createBackupPolicyResponse(BackupPolicyVO policyVO) {
        BackupRecoveryProviderVO provider = backupRecoveryProviderDao.findById(policyVO.getProviderId());

        BackupPolicyResponse response = new BackupPolicyResponse();
        response.setId(policyVO.getUuid());
        response.setPolicyId(policyVO.getPolicyUuid());
        response.setName(policyVO.getName());
        response.setProviderId(provider.getUuid());
        return response;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddBackupRecoveryProviderCmd.class);
        cmdList.add(DeleteBackupRecoveryProviderCmd.class);
        cmdList.add(ListBackupRecoveryProvidersCmd.class);
        cmdList.add(ListBackupRecoveryPoliciesCmd.class);
        cmdList.add(ListBackupRecoveryProviderPoliciesCmd.class);
        cmdList.add(AddBackupRecoveryPolicyCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return null;
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[0];
    }

    @Override
    public List<BackupRecoveryProviderPolicyResponse> listBackupPolicies(ListBackupRecoveryProviderPoliciesCmd cmd) throws AgentUnavailableException, OperationTimedoutException {
        Long providerId = cmd.getProviderId();
        BackupRecoveryProviderVO providerVO = backupRecoveryProviderDao.findById(providerId);
        if (providerVO == null) {
            throw new InvalidParameterValueException("Could not find a Backup and Recovery provider with id: " + cmd.getProviderId());
        }

        if (!BackupRecoveryHelper.isProviderSupported(providerVO.getProviderName())) {
            throw new InvalidParameterValueException("Provider: " + providerVO.getProviderName() + " is not supported");
        }

        DataCenterVO zone = dataCenterDao.findById(providerVO.getZoneId());
        ListBackupPoliciesCommand command = new ListBackupPoliciesCommand(zone.getUuid());
        ListBackupPoliciesAnswer answer = (ListBackupPoliciesAnswer) agentManager.send(providerVO.getHostId(), command);

        if (answer != null && answer.getResult()) {
            return createBackupRecoveryProviderPoliciesResponse(answer.getPolicies());
        }
        return null;
    }

    private List<BackupRecoveryProviderPolicyResponse> createBackupRecoveryProviderPoliciesResponse(List<BackupPolicyTO> policies) {
        List<BackupRecoveryProviderPolicyResponse> list = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(policies)) {
            for (BackupPolicyTO policy : policies) {
                BackupRecoveryProviderPolicyResponse p = new BackupRecoveryProviderPolicyResponse();
                p.setName(policy.getName());
                p.setPolicyId(policy.getId());
                p.setDescription(policy.getDescription());
                list.add(p);
            }
        }
        return list;
    }

    @Override
    public List<BackupPolicyVO> listBackupPolicies(ListBackupRecoveryPoliciesCmd cmd) {
        return backupPoliciesDao.listByProvider(cmd.getProviderId());
    }

    @Override
    public BackupPolicyVO addBackupPolicy(AddBackupRecoveryPolicyCmd cmd) throws AgentUnavailableException, OperationTimedoutException {
        String policyId = cmd.getPolicyId();
        Long providerId = cmd.getProviderId();
        String name = cmd.getPolicyName();

        BackupRecoveryProviderVO provider = backupRecoveryProviderDao.findById(providerId);
        if (provider == null) {
            throw new InvalidParameterValueException("Could not find a Backup and Recovery provider with id: " + providerId);
        }

        CheckBackupPolicyCommand command = new CheckBackupPolicyCommand(policyId);
        CheckBackupPolicyAnswer answer = (CheckBackupPolicyAnswer) agentManager.send(provider.getHostId(), command);
        if (answer == null || !answer.getResult()) {
            throw new InvalidParameterValueException("Could not find a backup policy with id: " + policyId + " on the provider");
        }
        BackupPolicyVO policy = new BackupPolicyVO(providerId, name, policyId);
        return backupPoliciesDao.persist(policy);
    }

    @Override
    public boolean assignVMToBackupPolicy(AssignBackupPolicyCmd cmd) throws AgentUnavailableException, OperationTimedoutException {
        String policyId = cmd.getPolicyId();
        Long virtualMachineId = cmd.getVirtualMachineId();
        VMInstanceVO vm = vmInstanceDao.findById(virtualMachineId);
        BackupPolicyVO policy = backupPoliciesDao.findByUuid(policyId);
        BackupRecoveryProviderVO provider = backupRecoveryProviderDao.findById(policy.getProviderId());

        AssignVMToBackupPolicyCommand command = new AssignVMToBackupPolicyCommand(virtualMachineId, vm.getUuid(), policyId);
        AssignVMToBackupPolicyAnswer answer = (AssignVMToBackupPolicyAnswer) agentManager.send(provider.getHostId(), command);
        return answer != null && answer.getResult();
    }

    @Override
    public void restoreVMFromBackup() {

    }

    @Override
    public void restoreAndAttachVolumeToVM() {

    }
}
