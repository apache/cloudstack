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
package org.apache.cloudstack.br;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.br.dao.BRProviderDetailsDao;
import org.apache.cloudstack.framework.br.BRPolicy;
import org.apache.cloudstack.framework.br.BRProvider;
import org.apache.cloudstack.api.command.admin.br.policy.AddBRPolicyCmd;
import org.apache.cloudstack.api.command.admin.br.provider.AddBRProviderCmd;
import org.apache.cloudstack.api.command.admin.br.provider.DeleteBRProviderCmd;
import org.apache.cloudstack.api.command.admin.br.policy.ListBRPoliciesCmd;
import org.apache.cloudstack.api.command.admin.br.provider.ListBRProviderPoliciesCmd;
import org.apache.cloudstack.api.command.admin.br.provider.ListBRProvidersCmd;
import org.apache.cloudstack.api.response.BRPolicyResponse;
import org.apache.cloudstack.api.response.BRProviderResponse;
import org.apache.cloudstack.br.dao.BRPoliciesDao;
import org.apache.cloudstack.br.dao.BRProviderDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Component
public class BRManagerImpl extends ManagerBase implements BRManager {

    @Inject
    BRProviderDao brProviderDao;
    @Inject
    BRProviderDetailsDao brProviderDetailsDao;
    @Inject
    BRPoliciesDao brPoliciesDao;
    @Inject
    DataCenterDao dataCenterDao;

    private List<BRProviderDriver> brProviders = new ArrayList<>();
    private Map<String, BRProviderDriver> brProvidersMap = new HashMap<>();

    private static final Logger s_logger = Logger.getLogger(BRManagerImpl.class);

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        return true;
    }

    public BRProviderDriver getBRProvider(String provider) {
        return brProvidersMap.getOrDefault(provider, null);
    }

    public BRProviderDriver getBRProviderFromPolicy(String policyId) {
        BRPolicyVO policy = brPoliciesDao.findByUuid(policyId);
        BRProviderVO provider = brProviderDao.findById(policy.getProviderId());
        return getBRProvider(provider.getProviderName());
    }

    public BRProviderDriver getBRProviderFromProvider(String providerId) {
        BRProviderVO provider = brProviderDao.findByUuid(providerId);
        return brProvidersMap.getOrDefault(provider.getProviderName(), null);
    }

    public List<BRProviderDriver> getBackupRecoveryProviders() {
        return brProviders;
    }

    public void setBackupRecoveryProviders(List<BRProviderDriver> backupRecoveryProviders) {
        this.brProviders = backupRecoveryProviders;
    }

    private void initializeDriversMap() {
        if (brProvidersMap.isEmpty() && brProviders != null && brProviders.size() > 0) {
            for (final BRProviderDriver driver : brProviders) {
                brProvidersMap.put(driver.getName().toLowerCase(), driver);
            }
            s_logger.debug("Discovered Backup and Recovery providers configured in the BRManager");
        }
    }
    @Override
    public boolean start() {
        initializeDriversMap();
        return true;
    }

    /*
        Backup Provider provider services
     */

    List<BRProviderDetailVO> createProviderDetails(long providerId, String url, String username, String password) {
        BRProviderDetailVO detail1 = new BRProviderDetailVO(providerId, "url", url);
        BRProviderDetailVO detail2 = new BRProviderDetailVO(providerId, "username", username);
        BRProviderDetailVO detail3 = new BRProviderDetailVO(providerId, "password", password);
        return Arrays.asList(detail1, detail2, detail3);
    }

    @Override
    public BRProviderVO addBRProvider(String name, String url, String username, String password, Long zoneId, String providerName) {
        if (!brProvidersMap.containsKey(providerName)) {
            throw new InvalidParameterException("Unsopported provider: " + providerName);
        }

        s_logger.debug("Registering a new Backup and Recovery provider on zone: " + zoneId + " - url: " + url + " - name: " + name
                        + " - provider: " + providerName);
        BRProviderDriver provider = brProvidersMap.get(providerName);
        boolean result = provider.registerProvider(zoneId, name, url, username, password);

        if (!result) {
            throw new CloudRuntimeException("Could not register backup and recovery provider " + name);
        }

        try {
            BRProviderVO providerVO = new BRProviderVO(name, zoneId, url, providerName);
            providerVO = brProviderDao.persist(providerVO);

            brProviderDetailsDao.addDetails(createProviderDetails(providerVO.getId(), url, username, password));
            return providerVO;
        } catch (Exception e) {
            throw new CloudRuntimeException("Error persisting Backup and recovery provider after succesfull registration: " + e.getMessage());
        }
    }

    @Override
    public List<BRProvider> listBRProviders() {
        return new ArrayList<BRProvider>(brProviderDao.listAll());
    }

    @Override
    public boolean deleteBRProvider(String providerUuid) {
        BRProviderVO providerVO = brProviderDao.findByUuid(providerUuid);
        long providerId = providerVO.getId();
        if (providerVO == null) {
            throw new InvalidParameterValueException("Could not find a Backup and Recovery provider with id: " + providerId);
        }
        BRProviderDriver provider = brProvidersMap.get(providerVO.getProviderName());
        boolean result = provider.unregisterProvider();

        if (!result) {
            throw new CloudRuntimeException("Unable to unregister provider: " + providerVO.getProviderName() + " id: " +providerId);
        }

        clearBRPolicies(providerId);
        clearBRProviderDetails(providerId);

        brProviderDao.remove(providerId);
        return true;
    }

    /*
        Backup Provider policies services
     */
    @Override
    public BRPolicy addBRPolicy(String policyId, String policyName, String providerId) {
        BRProviderVO providerVO = brProviderDao.findByUuid(providerId);
        BRProviderDriver provider = getBRProvider(providerVO.getProviderName());
        boolean exists = provider.policyExists(policyId, policyName);
        if (!exists) {
            throw new CloudRuntimeException("Policy " + policyId + " does not exist on provider " + providerVO.getName());
        }

        BRPolicyVO policy = new BRPolicyVO(providerVO.getId(), policyName, policyId);
        return brPoliciesDao.persist(policy);
    }

    @Override
    public List<BRPolicy> listBRPolicies(String providerId) {
        BRProviderVO provider = brProviderDao.findByUuid(providerId);
        return new ArrayList<BRPolicy>(brPoliciesDao.listByProvider(provider.getId()));
    }

    /**
     * Remove a Backup and Recovery Provider mapped policies
     */
    private void clearBRPolicies(long providerId) {
        brPoliciesDao.removeByProvider(providerId);
    }

    /**
     * Remove a Backup and Recovery Provider details
     */
    private void clearBRProviderDetails(Long providerId) {
        brProviderDetailsDao.removeDetails(providerId);
    }

    @Override
    public BRProviderResponse createBRProviderResponse(BRProvider vo) {
        DataCenterVO dc = dataCenterDao.findById(vo.getZoneId());

        BRProviderResponse response = new BRProviderResponse();
        response.setId(vo.getUuid());
        response.setName(vo.getName());
        response.setProviderName(vo.getProviderName());
        response.setZoneId(dc.getUuid());
        return response;
    }

    @Override
    public BRPolicyResponse createBackupPolicyResponse(BRPolicy policyVO) {
        BRProviderVO provider = brProviderDao.findById(policyVO.getProviderId());

        BRPolicyResponse response = new BRPolicyResponse();
        response.setId(policyVO.getUuid());
        response.setPolicyId(policyVO.getPolicyUuid());
        response.setName(policyVO.getName());
        response.setProviderId(provider.getUuid());
        return response;
    }

    @Override
    public long getProviderId(String providerId) {
        BRProviderVO provider = brProviderDao.findByUuid(providerId);
        return provider.getId();
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddBRProviderCmd.class);
        cmdList.add(DeleteBRProviderCmd.class);
        cmdList.add(ListBRProvidersCmd.class);
        cmdList.add(ListBRPoliciesCmd.class);
        cmdList.add(ListBRProviderPoliciesCmd.class);
        cmdList.add(AddBRPolicyCmd.class);
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
}
