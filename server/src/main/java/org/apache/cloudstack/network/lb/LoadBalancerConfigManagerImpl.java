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
package org.apache.cloudstack.network.lb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.dao.LoadBalancerConfigDao;
import com.cloud.network.dao.LoadBalancerConfigVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.cloudstack.api.command.user.loadbalancer.CreateLoadBalancerConfigCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.DeleteLoadBalancerConfigCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ListLoadBalancerConfigsCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.ReplaceLoadBalancerConfigsCmd;
import org.apache.cloudstack.api.command.user.loadbalancer.UpdateLoadBalancerConfigCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.network.lb.LoadBalancerConfig.Scope;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;

public class LoadBalancerConfigManagerImpl extends ManagerBase implements LoadBalancerConfigService, LoadBalancerConfigManager {
    private static final Logger LOGGER = Logger.getLogger(LoadBalancerConfigManagerImpl.class);

    @Inject
    LoadBalancerConfigDao _lbConfigDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    LoadBalancerDao _lbDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    LoadBalancingRulesManager _lbMgr;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;

    @Override
    public List<? extends LoadBalancerConfig> searchForLoadBalancerConfigs(ListLoadBalancerConfigsCmd cmd) {
        Long id = cmd.getId();
        String scopeStr = cmd.getScope();
        Long networkId = cmd.getNetworkId();
        Long vpcId = cmd.getVpcId();
        Long loadBalancerId = cmd.getLoadBalancerId();
        String name = cmd.getName();

        if (id == null && scopeStr == null) {
            throw new InvalidParameterValueException("At least one of id/scope is required");
        }

        //validate parameters
        Scope scope = null;
        if (scopeStr != null) {
            scope = LoadBalancerConfigKey.getScopeFromString(scopeStr);
            if (scope == null) {
                throw new InvalidParameterValueException("Invalid scope " + scopeStr);
            }
            checkPermission(scope, networkId, vpcId, loadBalancerId, cmd.listAll());
        }

        if (id != null) {
            LoadBalancerConfigVO config = _lbConfigDao.findById(id);
            if (config == null) {
                throw new InvalidParameterValueException("Cannot find load balancer config by id " + id);
            }
            checkPermission(config);
        }

        if (cmd.listAll()) {
            if (id != null || name != null) {
                throw new InvalidParameterValueException("id and name must be null if listall is true");
            }
        }

        SearchCriteria<LoadBalancerConfigVO> sc = _lbConfigDao.createSearchCriteria();
        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }
        if (scope != null) {
            sc.addAnd("scope", SearchCriteria.Op.EQ, scope);
        }
        if (networkId != null) {
            sc.addAnd("networkId", SearchCriteria.Op.EQ, networkId);
        }
        if (vpcId != null) {
            sc.addAnd("vpcId", SearchCriteria.Op.EQ, vpcId);
        }
        if (loadBalancerId != null) {
            sc.addAnd("loadBalancerId", SearchCriteria.Op.EQ, loadBalancerId);
        }
        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.EQ, name);
        }
        List<LoadBalancerConfigVO> configs = new ArrayList<>();
        if ( ObjectUtils.anyNotNull( id, networkId, vpcId, loadBalancerId)) {
            configs = _lbConfigDao.search(sc, null);
        }
        if (cmd.listAll()) {
            LOGGER.debug("Adding config keys for scope " + scope);
            Map<String, LoadBalancerConfigVO> configsMap = new LinkedHashMap<String, LoadBalancerConfigVO>();
            for (LoadBalancerConfigVO config : configs) {
                configsMap.put(config.getName(), config);
            }
            List<LoadBalancerConfigVO> result = new ArrayList<LoadBalancerConfigVO>();
            Map<String, LoadBalancerConfigKey> configKeys = LoadBalancerConfigKey.getConfigsByScope(scope);
            for (LoadBalancerConfigKey configKey : configKeys.values()) {
                if (configsMap.get(configKey.key()) != null) {
                    result.add(configsMap.get(configKey.key()));
                } else {
                    result.add(new LoadBalancerConfigVO(scope, null, null, null, configKey, null));
                }
            }
            return result;
        } else {
            return configs;
        }
    }

    @Override
    public LoadBalancerConfig createLoadBalancerConfig(CreateLoadBalancerConfigCmd cmd) {
        String scopeStr = cmd.getScope();
        Long networkId = cmd.getNetworkId();
        Long vpcId = cmd.getVpcId();
        Long loadBalancerId = cmd.getLoadBalancerId();
        String name = cmd.getName();
        String value = cmd.getValue();

        //validate parameters
        Scope scope = LoadBalancerConfigKey.getScopeFromString(scopeStr);
        if (scope == null) {
            throw new InvalidParameterValueException("Invalid scope " + scopeStr);
        }
        LoadBalancerConfigKey configKey = validateParameters(scope, name, value);

        checkPermission(scope, networkId, vpcId, loadBalancerId);

        LoadBalancerConfigVO existingConfig = _lbConfigDao.findConfig(scope, networkId, vpcId, loadBalancerId, name);
        if (existingConfig != null) {
            if (cmd.isForced()) {
                _lbConfigDao.remove(existingConfig.getId());
            } else {
                throw new InvalidParameterValueException("config " + name + " already exists, please add forced=true or update it instead");           }
        }
        LoadBalancerConfigVO config = _lbConfigDao.persist(new LoadBalancerConfigVO(scope, networkId, vpcId, loadBalancerId, configKey, value));

        applyLbConfigsForNetwork(config.getNetworkId(), config.getVpcId(), config.getLoadBalancerId());

        return config;
    }

    @Override
    public boolean deleteLoadBalancerConfig(DeleteLoadBalancerConfigCmd cmd) {
        Long id = cmd.getId();
        LoadBalancerConfigVO config = _lbConfigDao.findById(id);
        if (config == null) {
            throw new InvalidParameterValueException("Cannot find load balancer config by id " + id);
        }
        checkPermission(config);

        boolean result = _lbConfigDao.remove(id);

        applyLbConfigsForNetwork(config.getNetworkId(), config.getVpcId(), config.getLoadBalancerId());

        return result;
    }

    @Override
    public LoadBalancerConfig updateLoadBalancerConfig(UpdateLoadBalancerConfigCmd cmd) {
        Long id = cmd.getId();
        String value = cmd.getValue();

        LoadBalancerConfigVO config = _lbConfigDao.findById(id);
        if (config == null) {
            throw new InvalidParameterValueException("Cannot find load balancer config by id " + id);
        }
        //validate parameters
        LoadBalancerConfigKey configKey = validateParameters(config.getScope(), config.getName(), value);

        checkPermission(config);
        config.setValue(value);

        _lbConfigDao.update(config.getId(), config);

        applyLbConfigsForNetwork(config.getNetworkId(), config.getVpcId(), config.getLoadBalancerId());

        return config;
    }

    @Override
    public List<? extends LoadBalancerConfig> replaceLoadBalancerConfigs(ReplaceLoadBalancerConfigsCmd cmd) {
        String scopeStr = cmd.getScope();
        Long networkId = cmd.getNetworkId();
        Long vpcId = cmd.getVpcId();
        Long loadBalancerId = cmd.getLoadBalancerId();
        Map<String, String> configList = cmd.getConfigList();
        if (configList == null) {
            throw new InvalidParameterValueException("Invalid config list");
        }

        //validate parameters
        Scope scope = LoadBalancerConfigKey.getScopeFromString(scopeStr);
        if (scope == null) {
            throw new InvalidParameterValueException("Invalid scope " + scopeStr);
        }
        List<LoadBalancerConfigVO> configs = new ArrayList<LoadBalancerConfigVO>();
        for (String name : configList.keySet()) {
            String value = configList.get(name);
            LoadBalancerConfigKey configKey = validateParameters(scope, name, value);
            configs.add(new LoadBalancerConfigVO(scope, networkId, vpcId, loadBalancerId, configKey, value));
        }

        checkPermission(scope, networkId, vpcId, loadBalancerId);

        configs = _lbConfigDao.saveConfigs(configs);

        applyLbConfigsForNetwork(networkId, vpcId, loadBalancerId);

        return configs;
    }

    private LoadBalancerConfigKey validateParameters(Scope scope, String name, String value) {
        Pair<LoadBalancerConfigKey, String> res = LoadBalancerConfigKey.validate(scope, name, value);
        if (res.second() != null) {
            throw new InvalidParameterValueException(res.second());
        }
        return res.first();
    }

    private void checkPermission(LoadBalancerConfigVO config) {
        checkPermission(config.getScope(), config.getNetworkId(), config.getVpcId(), config.getLoadBalancerId());
    }

    private void checkPermission(Scope scope, Long networkId, Long vpcId, Long loadBalancerId) {
        checkPermission(scope, networkId, vpcId, loadBalancerId, false);
    }

    private void checkPermission(Scope scope, Long networkId, Long vpcId, Long loadBalancerId, Boolean listAll) {
        Account caller = CallContext.current().getCallingAccount();
        if (scope == Scope.Network) {
            if (networkId == null) {
                if (listAll) {
                    return;
                }
                throw new InvalidParameterValueException("networkId is required");
            }
            if (ObjectUtils.anyNotNull( vpcId, loadBalancerId)) {
                throw new InvalidParameterValueException("vpcId and loadBalancerId should be null if scope is Network");
            }
            NetworkVO network = _networkDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Cannot find network by id " + networkId);
            }
            // Perform permission check
            _accountMgr.checkAccess(caller, null, true, network);
            if (network.getVpcId() != null) {
                throw new InvalidParameterValueException("network " + network.getName() + " is a VPC tier, please add LB configs to VPC instead");
            }
        } else if (scope == Scope.Vpc) {
            if (vpcId == null) {
                if (listAll) {
                    return;
                }
                throw new InvalidParameterValueException("vpcId is required");
            }
            if (networkId != null || loadBalancerId != null) {
                throw new InvalidParameterValueException("networkId and loadBalancerId should be null if scope is Vpc");
            }
            VpcVO vpc = _vpcDao.findById(vpcId);
            if (vpc == null) {
                throw new InvalidParameterValueException("Cannot find vpc by id " + vpcId);
            }
            // Perform permission check
            _accountMgr.checkAccess(caller, null, true, vpc);
        } else if (scope == Scope.LoadBalancerRule) {
            if (loadBalancerId == null) {
                if (listAll) {
                    return;
                }
                throw new InvalidParameterValueException("loadBalancerId is required");
            }
            if (networkId != null || vpcId != null) {
                throw new InvalidParameterValueException("networkId and vpcId should be null if scope is LoadBalancerRule");
            }
            LoadBalancerVO rule = _lbDao.findById(loadBalancerId);
            if (rule == null) {
                throw new InvalidParameterValueException("Cannot find load balancer rule by id " + loadBalancerId);
            }
            if (networkId != null) {
                // Perform permission check
                checkPermission(Scope.Network, rule.getNetworkId(), null, null);
            }
        }
    }

    @Override
    public List<? extends LoadBalancerConfig> getNetworkLbConfigs(Long networkId) {
        return _lbConfigDao.listByNetworkId(networkId);
    }

    @Override
    public List<? extends LoadBalancerConfig> getVpcLbConfigs(Long vpcId) {
        return _lbConfigDao.listByVpcId(vpcId);
    }

    @Override
    public List<? extends LoadBalancerConfig> getRuleLbConfigs(Long loadBalancerId) {
        return _lbConfigDao.listByLoadBalancerId(loadBalancerId);
    }

    private void applyLbConfigsForNetwork(Long networkId, Long vpcId, Long loadBalancerId) {
        if (loadBalancerId != null) {
            LoadBalancerVO rule = _lbDao.findById(loadBalancerId);
            networkId = rule.getNetworkId();
        }
        if (networkId != null) {
            applyLbConfigsForNetwork(networkId);
        } else if (vpcId != null) {
            List<NetworkVO> networks = _networkDao.listByVpc(vpcId);
            for (NetworkVO network : networks) {
                if (applyLbConfigsForNetwork(network.getId())) {
                    break;
                }
            }
        }
    }

    private boolean applyLbConfigsForNetwork(Long networkId) {
        if (!_ntwkSrvcDao.canProviderSupportServiceInNetwork(networkId, Service.Lb, Provider.VirtualRouter) &&
                !_ntwkSrvcDao.canProviderSupportServiceInNetwork(networkId, Service.Lb, Provider.VPCVirtualRouter)) {
            LOGGER.info("Lb is not supported or not provided by VirtualRouter/VpcVirtualRouter in network " + networkId);
            return false;
        }
        try {
            if (!_lbMgr.applyLoadBalancersForNetwork(networkId, Scheme.Public)) {
                LOGGER.warn("Failed to apply LB configs of network id=" + networkId);
                return false;
            }
            return true;
        } catch (ResourceUnavailableException ex) {
            LOGGER.error("Failed to apply LB configs in virtual router on network: " + networkId, ex);
            throw new CloudRuntimeException("Failed to apply LB configs in virtual router on network: " + networkId);
        }
    }

    @Override
    public String getConfigComponentName() {
        return LoadBalancerConfigManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{ DefaultLbSSLConfiguration };
    }

}
