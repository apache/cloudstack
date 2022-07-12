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
package com.cloud.network.as;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd.HTTPMethod;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.autoscale.CreateCounterCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScalePolicyCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScaleVmGroupCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScaleVmProfileCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateConditionCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListAutoScalePoliciesCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListAutoScaleVmGroupsCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListAutoScaleVmProfilesCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListConditionsCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListCountersCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScalePolicyCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScaleVmGroupCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScaleVmProfileCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateConditionCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.PerformanceMonitorAnswer;
import com.cloud.agent.api.PerformanceMonitorCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.routing.GetAutoScaleMetricsAnswer;
import com.cloud.agent.api.routing.GetAutoScaleMetricsCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.LoadBalancerTO.AutoScaleVmProfileTO;
import com.cloud.agent.api.to.LoadBalancerTO.AutoScaleVmGroupTO;
import com.cloud.agent.api.to.LoadBalancerTO.AutoScalePolicyTO;
import com.cloud.agent.api.to.LoadBalancerTO.ConditionTO;
import com.cloud.agent.api.to.LoadBalancerTO.CounterTO;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.dispatch.DispatchChainFactory;
import com.cloud.api.dispatch.DispatchTask;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.IpAddresses;
import com.cloud.network.as.AutoScaleCounter.AutoScaleCounterParam;
import com.cloud.network.as.dao.AutoScalePolicyConditionMapDao;
import com.cloud.network.as.dao.AutoScalePolicyDao;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import com.cloud.network.as.dao.AutoScaleVmGroupPolicyMapDao;
import com.cloud.network.as.dao.AutoScaleVmGroupStatisticsDao;
import com.cloud.network.as.dao.AutoScaleVmGroupVmMapDao;
import com.cloud.network.as.dao.AutoScaleVmProfileDao;
import com.cloud.network.as.dao.ConditionDao;
import com.cloud.network.as.dao.CounterDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.lb.LoadBalancingRulesService;
import com.cloud.network.router.RouterControlHelper;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetrics;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetricsValue;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleValueType;
import com.cloud.network.router.VirtualRouterAutoScale.VirtualRouterAutoScaleCounter;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class AutoScaleManagerImpl<Type> extends ManagerBase implements AutoScaleManager, AutoScaleService, Configurable {
    private static final Logger s_logger = Logger.getLogger(AutoScaleManagerImpl.class);
    private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool(1);

    @Inject
    protected DispatchChainFactory dispatchChainFactory = null;
    @Inject
    EntityManager _entityMgr;
    @Inject
    AccountDao _accountDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    TemplateManager _templateMgr;
    @Inject
    LoadBalancingRulesManager _lbRulesMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    CounterDao _counterDao;
    @Inject
    ConditionDao _conditionDao;
    @Inject
    LoadBalancerVMMapDao _lb2VmMapDao;
    @Inject
    LoadBalancerDao _lbDao;
    @Inject
    AutoScaleVmProfileDao _autoScaleVmProfileDao;
    @Inject
    AutoScalePolicyDao _autoScalePolicyDao;
    @Inject
    AutoScalePolicyConditionMapDao _autoScalePolicyConditionMapDao;
    @Inject
    AutoScaleVmGroupDao _autoScaleVmGroupDao;
    @Inject
    AutoScaleVmGroupPolicyMapDao _autoScaleVmGroupPolicyMapDao;
    @Inject
    AutoScaleVmGroupVmMapDao _autoScaleVmGroupVmMapDao;
    @Inject
    DataCenterDao _dcDao = null;
    @Inject
    UserDao _userDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    AccountService _accountService;
    @Inject
    UserVmService _userVmService;
    @Inject
    UserVmManager _userVmManager;
    @Inject
    LoadBalancerVMMapDao _lbVmMapDao;
    @Inject
    LoadBalancingRulesService _loadBalancingRulesService;
    @Inject
    private VMInstanceDao _vmInstanceDao;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    private UserVmManager _userVmMgr;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private AutoScaleVmGroupStatisticsDao _asGroupStatisticsDao;
    @Inject
    private DomainRouterDao _routerDao;
    @Inject
    private AnnotationDao _annotationDao;
    @Inject
    protected RouterControlHelper _routerControlHelper;
    @Inject
    private DiskOfferingDao _diskOfferingDao;

    private static final Long ONE_MINUTE_IN_MILLISCONDS = 60000L;

    private static final List<String> supportedDeployParams = Arrays.asList("rootdisksize", "diskofferingid", "size", "securitygroupids");

    ExecutorService _groupExecutor;

    CompletionService<Pair<Long, Boolean>> _completionService;

    Map<Long, ScheduledExecutorService> vmGroupMonitorMaps = new HashMap<>();

    @Override
    public boolean start() {
        // create thread pool and blocking queue
        final int workersCount = AutoScaleStatsWorker.value();
        _groupExecutor = Executors.newFixedThreadPool(workersCount);
        s_logger.info("AutoScale Manager created a thread pool to check autoscale vm groups. The pool size is : " + workersCount);

        final BlockingQueue<Future<Pair<Long, Boolean>>> queue = new LinkedBlockingQueue<>(workersCount);
        s_logger.info("AutoScale Manager created a blocking queue to check autoscale vm groups. The queue size is : " + workersCount);

        _completionService = new ExecutorCompletionService<>(_groupExecutor, queue);

        scheduleMonitorTasks();

        return true;
    }

    @Override
    public boolean stop() {
        if (_groupExecutor != null) {
            _groupExecutor.shutdown();
        }
        return true;
    }

    public List<AutoScaleCounter> getSupportedAutoScaleCounters(long networkid) {
        String capability = _lbRulesMgr.getLBCapability(networkid, Capability.AutoScaleCounters.getName());
        if (capability == null) {
            return null;
        }
        Gson gson = new Gson();
        java.lang.reflect.Type listType = new TypeToken<List<AutoScaleCounter>>() {
        }.getType();
        List<AutoScaleCounter> result = gson.fromJson(capability, listType);
        return result;
    }

    public void validateAutoScaleCounters(long networkid, List<Counter> counters, List<Pair<String, String>> counterParamPassed) {
        List<AutoScaleCounter> supportedCounters = getSupportedAutoScaleCounters(networkid);
        if (supportedCounters == null) {
            throw new InvalidParameterException("AutoScale is not supported in the network");
        }
        for (Counter counter : counters) {
            String counterName = counter.getSource().name();
            boolean isCounterSupported = false;
            for (AutoScaleCounter autoScaleCounter : supportedCounters) {
                if (autoScaleCounter.getName().equals(counterName)) {
                    isCounterSupported = true;
                    List<AutoScaleCounterParam> counterParams = autoScaleCounter.getParamList();
                    for (AutoScaleCounterParam autoScaleCounterParam : counterParams) {
                        boolean isRequiredParameter = autoScaleCounterParam.getRequired();
                        if (isRequiredParameter) {
                            boolean isRequiredParamPresent = false;
                            for (Pair<String, String> pair : counterParamPassed) {
                                if (pair.first().equals(autoScaleCounterParam.getParamName()))
                                    isRequiredParamPresent = true;

                            }
                            if (!isRequiredParamPresent) {
                                throw new InvalidParameterException("Parameter " + autoScaleCounterParam.getParamName() + " has to be set in AutoScaleVmProfile's " +
                                    ApiConstants.COUNTERPARAM_LIST);
                            }
                        }
                    }
                    break;
                }
            }
            if (!isCounterSupported) {
                throw new InvalidParameterException("AutoScale counter with source='" + counter.getSource().name() + "' is not supported " + "in the network");
            }
        }
    }

    private <VO extends ControlledEntity> VO getEntityInDatabase(Account caller, String paramName, Long id, GenericDao<VO, Long> dao) {

        VO vo = dao.findById(id);

        if (vo == null) {
            throw new InvalidParameterValueException("Unable to find " + paramName);
        }

        _accountMgr.checkAccess(caller, null, false, (ControlledEntity)vo);

        return vo;
    }

    private boolean isAutoScaleScaleUpPolicy(AutoScalePolicy policyVO) {
        return policyVO.getAction().equals(AutoScalePolicy.Action.ScaleUp);
    }

    private List<AutoScalePolicyVO> getAutoScalePolicies(String paramName, List<Long> policyIds, List<Counter> counters, int interval, boolean scaleUpPolicies) {
        SearchBuilder<AutoScalePolicyVO> policySearch = _autoScalePolicyDao.createSearchBuilder();
        policySearch.and("ids", policySearch.entity().getId(), Op.IN);
        policySearch.done();
        SearchCriteria<AutoScalePolicyVO> sc = policySearch.create();

        sc.setParameters("ids", policyIds.toArray(new Object[0]));
        List<AutoScalePolicyVO> policies = _autoScalePolicyDao.search(sc, null);

        int prevQuietTime = 0;

        for (AutoScalePolicyVO policy : policies) {
            int quietTime = policy.getQuietTime();
            if (prevQuietTime == 0) {
                prevQuietTime = quietTime;
            }
            int duration = policy.getDuration();
            if (duration < interval) {
                throw new InvalidParameterValueException("duration : " + duration + " specified in a policy cannot be less than vm group's interval : " + interval);
            }

            if (quietTime != prevQuietTime) {
                throw new InvalidParameterValueException("quietTime should be same for all the policies specified in " + paramName);
            }

            if (scaleUpPolicies) {
                if (!isAutoScaleScaleUpPolicy(policy)) {
                    throw new InvalidParameterValueException("Only scaleup policies can be specified in scaleuppolicyids");
                }
            } else {
                if (isAutoScaleScaleUpPolicy(policy)) {
                    throw new InvalidParameterValueException("Only scaledown policies can be specified in scaledownpolicyids");
                }
            }
            List<AutoScalePolicyConditionMapVO> policyConditionMapVOs = _autoScalePolicyConditionMapDao.listByAll(policy.getId(), null);
            for (AutoScalePolicyConditionMapVO policyConditionMapVO : policyConditionMapVOs) {
                long conditionid = policyConditionMapVO.getConditionId();
                Condition condition = _conditionDao.findById(conditionid);
                Counter counter = _counterDao.findById(condition.getCounterId());
                counters.add(counter);
            }
        }
        return policies;
    }

    @DB
    protected AutoScaleVmProfileVO checkValidityAndPersist(AutoScaleVmProfileVO vmProfile) {
        long templateId = vmProfile.getTemplateId();
        long autoscaleUserId = vmProfile.getAutoScaleUserId();
        int destroyVmGraceperiod = vmProfile.getDestroyVmGraceperiod();

        Long serviceOfferingId = vmProfile.getServiceOfferingId();
        if (serviceOfferingId != null) {
            ServiceOffering serviceOffering = _entityMgr.findByIdIncludingRemoved(ServiceOffering.class, serviceOfferingId);
            if (serviceOffering == null) {
                throw new InvalidParameterValueException("Unable to find service offering by id");
            }
            if (serviceOffering.isDynamic()) {
                throw new InvalidParameterValueException("Unable to use dynamic service offering in AutoScale vm profile");
            }
        }

        VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, templateId);
        // Make sure a valid template ID was specified
        if (template == null) {
            throw new InvalidParameterValueException("Unable to use the given template.");
        }

        if (destroyVmGraceperiod < 0) {
            throw new InvalidParameterValueException("Destroy Vm Grace Period cannot be less than 0.");
        }

        User user = _userDao.findById(autoscaleUserId);
        if (user.getAccountId() != vmProfile.getAccountId()) {
            throw new InvalidParameterValueException("AutoScale User id does not belong to the same account");
        }

        String apiKey = user.getApiKey();
        String secretKey = user.getSecretKey();
        String csUrl = ApiServiceConfiguration.ApiServletPath.value();

        if (apiKey == null) {
            throw new InvalidParameterValueException("apiKey for user: " + user.getUsername() + " is empty. Please generate it");
        }

        if (secretKey == null) {
            throw new InvalidParameterValueException("secretKey for user: " + user.getUsername() + " is empty. Please generate it");
        }

        if (csUrl == null || csUrl.contains("localhost")) {
            throw new InvalidParameterValueException(String.format("Global setting %s has to be set to the Management Server's API end point", ApiServiceConfiguration.ApiServletPath.key()));
        }

        Integer autoScaleStatsInterval = AutoScaleStatsInterval.value();
        if (autoScaleStatsInterval <= 0) {
            throw new InvalidParameterValueException(String.format("Global setting %s has to be set to larger than 0", AutoScaleStatsInterval.key()));
        }

        List<Pair<String, String>> otherDeployParams = vmProfile.getOtherDeployParamsList();
        if (CollectionUtils.isNotEmpty(otherDeployParams)) {
            for (Pair<String, String> kvPair : otherDeployParams) {
                String key = kvPair.first();
                if (!supportedDeployParams.contains(key)) {
                    throw new InvalidParameterValueException(String.format("Unsupported otherdeployparams key: %s. Supported values are %s", key, supportedDeployParams));
                }
            }
        }
        vmProfile = _autoScaleVmProfileDao.persist(vmProfile);

        return vmProfile;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMPROFILE_CREATE, eventDescription = "creating autoscale vm profile", create = true)
    public AutoScaleVmProfile createAutoScaleVmProfile(CreateAutoScaleVmProfileCmd cmd) {

        Account owner = _accountDao.findById(cmd.getAccountId());
        Account caller = CallContext.current().getCallingAccount();
        _accountMgr.checkAccess(caller, null, true, owner);

        long zoneId = cmd.getZoneId();
        long serviceOfferingId = cmd.getServiceOfferingId();
        long autoscaleUserId = cmd.getAutoscaleUserId();

        DataCenter zone = _entityMgr.findById(DataCenter.class, zoneId);

        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id");
        }

        ServiceOffering serviceOffering = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException("Unable to find service offering by id");
        }

        // validations
        HashMap<String, String> deployParams = cmd.getDeployParamMap();
        if (deployParams.containsKey("networks") && deployParams.get("networks").length() > 0) {
            throw new InvalidParameterValueException(
                "'networks' is not a valid parameter, network for an AutoScaled VM is chosen automatically. An autoscaled VM is deployed in the loadbalancer's network");
        }
        /*
         * Just for making sure the values are right in other deploy params.
         * For ex. if projectId is given as a string instead of an long value, this
         * will be throwing an error.
         */
        dispatchChainFactory.getStandardDispatchChain().dispatch(new DispatchTask(ComponentContext.inject(DeployVMCmd.class), deployParams));

        AutoScaleVmProfileVO profileVO =
            new AutoScaleVmProfileVO(cmd.getZoneId(), cmd.getDomainId(), cmd.getAccountId(), cmd.getServiceOfferingId(), cmd.getTemplateId(), cmd.getOtherDeployParams(),
                cmd.getCounterParamList(), cmd.getDestroyVmGraceperiod(), autoscaleUserId);

        if (cmd.getDisplay() != null) {
            profileVO.setDisplay(cmd.getDisplay());
        }

        profileVO = checkValidityAndPersist(profileVO);
        s_logger.info("Successfully create AutoScale Vm Profile with Id: " + profileVO.getId());

        return profileVO;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMPROFILE_UPDATE, eventDescription = "updating autoscale vm profile")
    public AutoScaleVmProfile updateAutoScaleVmProfile(UpdateAutoScaleVmProfileCmd cmd) {
        Long profileId = cmd.getId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        Long templateId = cmd.getTemplateId();
        Long autoscaleUserId = cmd.getAutoscaleUserId();
        Map otherDeployParams = cmd.getOtherDeployParams();
        Map counterParamList = cmd.getCounterParamList();

        Integer destroyVmGraceperiod = cmd.getDestroyVmGraceperiod();

        AutoScaleVmProfileVO vmProfile = getEntityInDatabase(CallContext.current().getCallingAccount(), "Auto Scale Vm Profile", profileId, _autoScaleVmProfileDao);

        boolean physicalParameterUpdate = (templateId != null || autoscaleUserId != null || counterParamList != null || otherDeployParams != null && destroyVmGraceperiod != null);

        if (serviceOfferingId != null) {
            vmProfile.setServiceOfferingId(serviceOfferingId);
        }

        if (templateId != null) {
            vmProfile.setTemplateId(templateId);
        }

        if (autoscaleUserId != null) {
            vmProfile.setAutoscaleUserId(autoscaleUserId);
        }

        if (otherDeployParams != null) {
            vmProfile.setOtherDeployParamsForUpdate(otherDeployParams);
        }

        if (counterParamList != null) {
            vmProfile.setCounterParamsForUpdate(counterParamList);
        }

        if (destroyVmGraceperiod != null) {
            vmProfile.setDestroyVmGraceperiod(destroyVmGraceperiod);
        }

        if (cmd.getCustomId() != null) {
            vmProfile.setUuid(cmd.getCustomId());
        }

        if (cmd.getDisplay() != null) {
            vmProfile.setDisplay(cmd.getDisplay());
        }

        List<AutoScaleVmGroupVO> vmGroupList = _autoScaleVmGroupDao.listByAll(null, profileId);
        for (AutoScaleVmGroupVO vmGroupVO : vmGroupList) {
            if (physicalParameterUpdate && !vmGroupVO.getState().equals(AutoScaleVmGroup.State.Disabled)) {
                throw new InvalidParameterValueException("The AutoScale Vm Profile can be updated only if the Vm Group it is associated with is disabled in state");
            }
        }

        vmProfile = checkValidityAndPersist(vmProfile);
        s_logger.info("Updated Auto Scale Vm Profile id:" + vmProfile.getId());

        return vmProfile;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMPROFILE_DELETE, eventDescription = "deleting autoscale vm profile")
    public boolean deleteAutoScaleVmProfile(long id) {
        /* Check if entity is in database */
        getEntityInDatabase(CallContext.current().getCallingAccount(), "AutoScale Vm Profile", id, _autoScaleVmProfileDao);
        if (_autoScaleVmGroupDao.isProfileInUse(id)) {
            throw new InvalidParameterValueException("Cannot delete AutoScale Vm Profile when it is in use by one more vm groups");
        }

        // Remove comments (if any)
        AutoScaleVmGroup group = _autoScaleVmGroupDao.findById(id);
        if (group != null) {
            _annotationDao.removeByEntityType(AnnotationService.EntityType.AUTOSCALE_VM_GROUP.name(), group.getUuid());
        }

        boolean success = _autoScaleVmProfileDao.remove(id);
        if (success) {
            s_logger.info("Successfully deleted AutoScale Vm Profile with Id: " + id);
        }
        return success;
    }

    @Override
    public List<? extends AutoScaleVmProfile> listAutoScaleVmProfiles(ListAutoScaleVmProfilesCmd cmd) {
        Long id = cmd.getId();
        Long templateId = cmd.getTemplateId();
        String otherDeployParams = cmd.getOtherDeployParams();
        Long serviceOffId = cmd.getServiceOfferingId();
        Long zoneId = cmd.getZoneId();
        Boolean display = cmd.getDisplay();

        SearchWrapper<AutoScaleVmProfileVO> searchWrapper = new SearchWrapper<AutoScaleVmProfileVO>(_autoScaleVmProfileDao, AutoScaleVmProfileVO.class, cmd, cmd.getId());
        SearchBuilder<AutoScaleVmProfileVO> sb = searchWrapper.getSearchBuilder();

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("templateId", sb.entity().getTemplateId(), SearchCriteria.Op.EQ);
        sb.and("serviceOfferingId", sb.entity().getServiceOfferingId(), SearchCriteria.Op.EQ);
        sb.and("otherDeployParams", sb.entity().getOtherDeployParams(), SearchCriteria.Op.LIKE);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("display", sb.entity().isDisplay(), SearchCriteria.Op.EQ);
        SearchCriteria<AutoScaleVmProfileVO> sc = searchWrapper.buildSearchCriteria();

        if (id != null) {
            sc.setParameters("id", id);
        }
        if (templateId != null) {
            sc.setParameters("templateId", templateId);
        }
        if (otherDeployParams != null) {
            sc.addAnd("otherDeployParams", SearchCriteria.Op.LIKE, "%" + otherDeployParams + "%");
        }

        if (serviceOffId != null) {
            sc.setParameters("serviceOfferingId", serviceOffId);
        }

        if (zoneId != null) {
            sc.setParameters("zoneId", zoneId);
        }

        if (display != null) {
            sc.setParameters("display", display);
        }

        return searchWrapper.search();
    }

    @DB
    protected AutoScalePolicyVO checkValidityAndPersist(final AutoScalePolicyVO autoScalePolicyVOFinal, final List<Long> conditionIds) {
        final int duration = autoScalePolicyVOFinal.getDuration();
        final int quietTime = autoScalePolicyVOFinal.getQuietTime();

        if (duration < 0) {
            throw new InvalidParameterValueException("duration is an invalid value: " + duration);
        }

        if (quietTime < 0) {
            throw new InvalidParameterValueException("quiettime is an invalid value: " + quietTime);
        }

        return Transaction.execute(new TransactionCallback<AutoScalePolicyVO>() {
            @Override
            public AutoScalePolicyVO doInTransaction(TransactionStatus status) {
                AutoScalePolicyVO autoScalePolicyVO = _autoScalePolicyDao.persist(autoScalePolicyVOFinal);

                if (conditionIds != null) {
                    SearchBuilder<ConditionVO> conditionsSearch = _conditionDao.createSearchBuilder();
                    conditionsSearch.and("ids", conditionsSearch.entity().getId(), Op.IN);
                    conditionsSearch.done();
                    SearchCriteria<ConditionVO> sc = conditionsSearch.create();

                    sc.setParameters("ids", conditionIds.toArray(new Object[0]));
                    List<ConditionVO> conditions = _conditionDao.search(sc, null);

                    ControlledEntity[] sameOwnerEntities = conditions.toArray(new ControlledEntity[conditions.size() + 1]);
                    sameOwnerEntities[sameOwnerEntities.length - 1] = autoScalePolicyVO;
                    _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, sameOwnerEntities);

                    if (conditionIds.size() != conditions.size()) {
                        // TODO report the condition id which could not be found
                        throw new InvalidParameterValueException("Unable to find the condition specified");
                    }

                    ArrayList<Long> counterIds = new ArrayList<Long>();
                    for (ConditionVO condition : conditions) {
                        if (counterIds.contains(condition.getCounterId())) {
                            throw new InvalidParameterValueException(
                                "at least two conditions in the conditionids have the same counter. It is not right to apply two different conditions for the same counter");
                        }
                        counterIds.add(condition.getCounterId());
                    }

                    /* For update case remove the existing mappings and create fresh ones */
                    _autoScalePolicyConditionMapDao.removeByAutoScalePolicyId(autoScalePolicyVO.getId());

                    for (Long conditionId : conditionIds) {
                        AutoScalePolicyConditionMapVO policyConditionMapVO = new AutoScalePolicyConditionMapVO(autoScalePolicyVO.getId(), conditionId);
                        _autoScalePolicyConditionMapDao.persist(policyConditionMapVO);
                    }
                }

                return autoScalePolicyVO;
            }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEPOLICY_CREATE, eventDescription = "creating autoscale policy", create = true)
    public AutoScalePolicy createAutoScalePolicy(CreateAutoScalePolicyCmd cmd) {

        int duration = cmd.getDuration();
        Integer quietTime = cmd.getQuietTime();
        String action = cmd.getAction();

        if (quietTime == null) {
            quietTime = NetUtils.DEFAULT_AUTOSCALE_POLICY_QUIET_TIME;
        }

        AutoScalePolicy.Action scaleAction = AutoScalePolicy.Action.fromValue(action);
        if (scaleAction == null) {
            throw new InvalidParameterValueException("action is invalid. Supported actions are: " + Arrays.toString(AutoScalePolicy.Action.values()));
        }

        AutoScalePolicyVO policyVO = new AutoScalePolicyVO(cmd.getDomainId(), cmd.getAccountId(), duration, quietTime, null, scaleAction);

        policyVO = checkValidityAndPersist(policyVO, cmd.getConditionIds());
        s_logger.info("Successfully created AutoScale Policy with Id: " + policyVO.getId());
        return policyVO;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEPOLICY_DELETE, eventDescription = "deleting autoscale policy")
    public boolean deleteAutoScalePolicy(final long id) {
        /* Check if entity is in database */
        getEntityInDatabase(CallContext.current().getCallingAccount(), "AutoScale Policy", id, _autoScalePolicyDao);

        if (_autoScaleVmGroupPolicyMapDao.isAutoScalePolicyInUse(id)) {
            throw new InvalidParameterValueException("Cannot delete AutoScale Policy when it is in use by one or more AutoScale Vm Groups");
        }

        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                boolean success = true;
                success = _autoScalePolicyDao.remove(id);
                if (!success) {
                    s_logger.warn("Failed to remove AutoScale Policy db object");
                    return false;
                }
                success = _autoScalePolicyConditionMapDao.removeByAutoScalePolicyId(id);
                if (!success) {
                    s_logger.warn("Failed to remove AutoScale Policy Condition mappings");
                    return false;
                }
                s_logger.info("Successfully deleted autoscale policy id : " + id);

                return success;
            }
        });
    }

    public void checkCallerAccess(String accountName, Long domainId) {
        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountDao.findActiveAccount(accountName, domainId);
        if (owner == null) {
            List<String> idList = new ArrayList<String>();
            idList.add(ApiDBUtils.findDomainById(domainId).getUuid());
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain with specifed domainId");
        }
        _accountMgr.checkAccess(caller, null, false, owner);
    }

    private class SearchWrapper<VO extends ControlledEntity> {
        GenericDao<VO, Long> dao;
        SearchBuilder<VO> searchBuilder;
        SearchCriteria<VO> searchCriteria;
        Long domainId;
        boolean isRecursive;
        List<Long> permittedAccounts = new ArrayList<Long>();
        ListProjectResourcesCriteria listProjectResourcesCriteria;
        Filter searchFilter;

        public SearchWrapper(GenericDao<VO, Long> dao, Class<VO> entityClass, BaseListAccountResourcesCmd cmd, Long id)
        {
            this.dao = dao;
            this.searchBuilder = dao.createSearchBuilder();
            domainId = cmd.getDomainId();
            String accountName = cmd.getAccountName();
            isRecursive = cmd.isRecursive();
            boolean listAll = cmd.listAll();
            long startIndex = cmd.getStartIndex();
            long pageSizeVal = cmd.getPageSizeVal();
            Account caller = CallContext.current().getCallingAccount();

            Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean,
                    ListProjectResourcesCriteria>(domainId, isRecursive, null);
            _accountMgr.buildACLSearchParameters(caller, id, accountName, null, permittedAccounts, domainIdRecursiveListProject,
                    listAll, false);
            domainId = domainIdRecursiveListProject.first();
            isRecursive = domainIdRecursiveListProject.second();
            ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
            _accountMgr.buildACLSearchBuilder(searchBuilder, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
            searchFilter = new Filter(entityClass, "id", false, startIndex, pageSizeVal);
        }

        public SearchBuilder<VO> getSearchBuilder() {
            return searchBuilder;
        }

        public SearchCriteria<VO> buildSearchCriteria() {
            searchCriteria = searchBuilder.create();
            _accountMgr.buildACLSearchCriteria(searchCriteria, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
            return searchCriteria;
        }

        public List<VO> search() {
            return dao.search(searchCriteria, searchFilter);
        }
    }

    @Override
    public List<? extends AutoScalePolicy> listAutoScalePolicies(ListAutoScalePoliciesCmd cmd) {
        SearchWrapper<AutoScalePolicyVO> searchWrapper = new SearchWrapper<AutoScalePolicyVO>(_autoScalePolicyDao, AutoScalePolicyVO.class, cmd, cmd.getId());
        SearchBuilder<AutoScalePolicyVO> sb = searchWrapper.getSearchBuilder();
        Long id = cmd.getId();
        Long conditionId = cmd.getConditionId();
        String action = cmd.getAction();
        Long vmGroupId = cmd.getVmGroupId();

        if (action != null) {
            AutoScalePolicy.Action scaleAction = AutoScalePolicy.Action.fromValue(action);
            if (scaleAction == null) {
                throw new InvalidParameterValueException("action is invalid. Supported actions are: " + Arrays.toString(AutoScalePolicy.Action.values()));
            }
        }

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("action", sb.entity().getAction(), SearchCriteria.Op.EQ);

        if (conditionId != null) {
            SearchBuilder<AutoScalePolicyConditionMapVO> asPolicyConditionSearch = _autoScalePolicyConditionMapDao.createSearchBuilder();
            asPolicyConditionSearch.and("conditionId", asPolicyConditionSearch.entity().getConditionId(), SearchCriteria.Op.EQ);
            sb.join("asPolicyConditionSearch", asPolicyConditionSearch, sb.entity().getId(), asPolicyConditionSearch.entity().getPolicyId(), JoinBuilder.JoinType.INNER);
        }

        if (vmGroupId != null) {
            SearchBuilder<AutoScaleVmGroupPolicyMapVO> asVmGroupPolicySearch = _autoScaleVmGroupPolicyMapDao.createSearchBuilder();
            asVmGroupPolicySearch.and("vmGroupId", asVmGroupPolicySearch.entity().getVmGroupId(), SearchCriteria.Op.EQ);
            sb.join("asVmGroupPolicySearch", asVmGroupPolicySearch, sb.entity().getId(), asVmGroupPolicySearch.entity().getPolicyId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<AutoScalePolicyVO> sc = searchWrapper.buildSearchCriteria();

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (action != null) {
            sc.setParameters("action", action);
        }

        if (conditionId != null) {
            sc.setJoinParameters("asPolicyConditionSearch", "conditionId", conditionId);
        }

        if (vmGroupId != null) {
            sc.setJoinParameters("asVmGroupPolicySearch", "vmGroupId", vmGroupId);
        }

        return searchWrapper.search();
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEPOLICY_UPDATE, eventDescription = "updating autoscale policy")
    public AutoScalePolicy updateAutoScalePolicy(UpdateAutoScalePolicyCmd cmd) {
        Long policyId = cmd.getId();
        Integer duration = cmd.getDuration();
        Integer quietTime = cmd.getQuietTime();
        List<Long> conditionIds = cmd.getConditionIds();
        AutoScalePolicyVO policy = getEntityInDatabase(CallContext.current().getCallingAccount(), "Auto Scale Policy", policyId, _autoScalePolicyDao);

        if (duration != null) {
            policy.setDuration(duration);
        }

        if (quietTime != null) {
            policy.setQuietTime(quietTime);
        }

        List<AutoScaleVmGroupPolicyMapVO> vmGroupPolicyList = _autoScaleVmGroupPolicyMapDao.listByPolicyId(policyId);
        for (AutoScaleVmGroupPolicyMapVO vmGroupPolicy : vmGroupPolicyList) {
            AutoScaleVmGroupVO vmGroupVO = _autoScaleVmGroupDao.findById(vmGroupPolicy.getVmGroupId());
            if (vmGroupVO == null) {
                s_logger.warn("Stale database entry! There is an entry in VmGroupPolicyMap but the vmGroup is missing:" + vmGroupPolicy.getVmGroupId());

                continue;

            }
            if (!vmGroupVO.getState().equals(AutoScaleVmGroup.State.Disabled)) {
                throw new InvalidParameterValueException("The AutoScale Policy can be updated only if the Vm Group it is associated with is disabled in state");
            }
            if (policy.getDuration() < vmGroupVO.getInterval()) {
                throw new InvalidParameterValueException("duration is less than the associated AutoScaleVmGroup's interval");
            }
        }

        policy = checkValidityAndPersist(policy, conditionIds);
        s_logger.info("Successfully updated Auto Scale Policy id:" + policyId);
        return policy;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMGROUP_CREATE, eventDescription = "creating autoscale vm group", create = true)
    public AutoScaleVmGroup createAutoScaleVmGroup(CreateAutoScaleVmGroupCmd cmd) {
        String name = cmd.getName();
        int minMembers = cmd.getMinMembers();
        int maxMembers = cmd.getMaxMembers();
        Integer interval = cmd.getInterval();
        Boolean forDisplay = cmd.getDisplay();

        if (interval == null) {
            interval = NetUtils.DEFAULT_AUTOSCALE_POLICY_INTERVAL_TIME;
        }

        LoadBalancerVO loadBalancer = getEntityInDatabase(CallContext.current().getCallingAccount(), ApiConstants.LBID, cmd.getLbRuleId(), _lbDao);

        Long zoneId = _ipAddressDao.findById(loadBalancer.getSourceIpAddressId()).getDataCenterId();

        if (_autoScaleVmGroupDao.isAutoScaleLoadBalancer(loadBalancer.getId())) {
            throw new InvalidParameterValueException("an AutoScaleVmGroup is already attached to the lb rule, the existing vm group has to be first deleted");
        }

        if (_lb2VmMapDao.isVmAttachedToLoadBalancer(loadBalancer.getId())) {
            throw new InvalidParameterValueException(
                "there are Vms already bound to the specified LoadBalancing Rule. User bound Vms and AutoScaled Vm Group cannot co-exist on a Load Balancing Rule");
        }

        AutoScaleVmGroupVO vmGroupVO = new AutoScaleVmGroupVO(cmd.getLbRuleId(), zoneId, loadBalancer.getDomainId(), loadBalancer.getAccountId(), name, minMembers, maxMembers,
            loadBalancer.getDefaultPortStart(), interval, null, cmd.getProfileId(), AutoScaleVmGroup.State.New);

        if (forDisplay != null) {
            vmGroupVO.setDisplay(forDisplay);
        }

        vmGroupVO = checkValidityAndPersist(vmGroupVO, cmd.getScaleUpPolicyIds(), cmd.getScaleDownPolicyIds());
        s_logger.info("Successfully created Autoscale Vm Group with Id: " + vmGroupVO.getId());

        scheduleMonitorTask(vmGroupVO.getId());

        return vmGroupVO;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMGROUP_CREATE, eventDescription = "creating autoscale vm group", async = true)
    public boolean configureAutoScaleVmGroup(CreateAutoScaleVmGroupCmd cmd) throws ResourceUnavailableException {
        return configureAutoScaleVmGroup(cmd.getEntityId(), AutoScaleVmGroup.State.New);
    }

    public boolean isLoadBalancerBasedAutoScaleVmGroup(AutoScaleVmGroup vmGroup) {
        return vmGroup.getLoadBalancerId() != null;
    }

    private boolean configureAutoScaleVmGroup(long vmGroupid, AutoScaleVmGroup.State currentState) throws ResourceUnavailableException {
        AutoScaleVmGroup vmGroup = _autoScaleVmGroupDao.findById(vmGroupid);

        if (isLoadBalancerBasedAutoScaleVmGroup(vmGroup)) {
            try {
                return _lbRulesMgr.configureLbAutoScaleVmGroup(vmGroupid, currentState);
            } catch (ResourceUnavailableException re) {
                throw re;
            } catch (Exception e) {
                s_logger.warn("Exception during configureLbAutoScaleVmGroup in lb rules manager", e);
                return false;
            }
        }

        // This should never happen, because today loadbalancerruleid is manadatory for AutoScaleVmGroup.
        throw new InvalidParameterValueException("Only LoadBalancer based AutoScale is supported");
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMGROUP_DELETE, eventDescription = "deleting autoscale vm group", async = true)
    public boolean deleteAutoScaleVmGroup(final long id) {
        AutoScaleVmGroupVO autoScaleVmGroupVO = getEntityInDatabase(CallContext.current().getCallingAccount(), "AutoScale Vm Group", id, _autoScaleVmGroupDao);

        if (autoScaleVmGroupVO.getState().equals(AutoScaleVmGroup.State.New)) {
            /* This condition is for handling failures during creation command */
            return _autoScaleVmGroupDao.remove(id);
        }
        AutoScaleVmGroup.State bakupState = autoScaleVmGroupVO.getState();
        autoScaleVmGroupVO.setState(AutoScaleVmGroup.State.Revoke);
        _autoScaleVmGroupDao.persist(autoScaleVmGroupVO);
        boolean success = false;

        try {
            success = configureAutoScaleVmGroup(id, bakupState);
        } catch (ResourceUnavailableException e) {
            autoScaleVmGroupVO.setState(bakupState);
            _autoScaleVmGroupDao.persist(autoScaleVmGroupVO);
        } finally {
            if (!success) {
                s_logger.warn("Could not delete AutoScale Vm Group id : " + id);
                return false;
            }
        }

        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                boolean success = _autoScaleVmGroupDao.remove(id);

                if (!success) {
                    s_logger.warn("Failed to remove AutoScale Group db object");
                    return false;
                }

                success = _autoScaleVmGroupPolicyMapDao.removeByGroupId(id);
                if (!success) {
                    s_logger.warn("Failed to remove AutoScale Group Policy mappings");
                    return false;
                }

                s_logger.info("Successfully deleted autoscale vm group id : " + id);
                return success; // Successfull
            }
        });

    }

    @Override
    public List<? extends AutoScaleVmGroup> listAutoScaleVmGroups(ListAutoScaleVmGroupsCmd cmd) {
        Long id = cmd.getId();
        Long policyId = cmd.getPolicyId();
        Long loadBalancerId = cmd.getLoadBalancerId();
        Long profileId = cmd.getProfileId();
        Long zoneId = cmd.getZoneId();
        Boolean forDisplay = cmd.getDisplay();

        SearchWrapper<AutoScaleVmGroupVO> searchWrapper = new SearchWrapper<AutoScaleVmGroupVO>(_autoScaleVmGroupDao, AutoScaleVmGroupVO.class, cmd, cmd.getId());
        SearchBuilder<AutoScaleVmGroupVO> sb = searchWrapper.getSearchBuilder();

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("loadBalancerId", sb.entity().getLoadBalancerId(), SearchCriteria.Op.EQ);
        sb.and("profileId", sb.entity().getProfileId(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("display", sb.entity().isDisplay(), SearchCriteria.Op.EQ);

        if (policyId != null) {
            SearchBuilder<AutoScaleVmGroupPolicyMapVO> asVmGroupPolicySearch = _autoScaleVmGroupPolicyMapDao.createSearchBuilder();
            asVmGroupPolicySearch.and("policyId", asVmGroupPolicySearch.entity().getPolicyId(), SearchCriteria.Op.EQ);
            sb.join("asVmGroupPolicySearch", asVmGroupPolicySearch, sb.entity().getId(), asVmGroupPolicySearch.entity().getVmGroupId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<AutoScaleVmGroupVO> sc = searchWrapper.buildSearchCriteria();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (loadBalancerId != null) {
            sc.setParameters("loadBalancerId", loadBalancerId);
        }
        if (profileId != null) {
            sc.setParameters("profileId", profileId);
        }
        if (zoneId != null) {
            sc.setParameters("zoneId", zoneId);
        }
        if (policyId != null) {
            sc.setJoinParameters("asVmGroupPolicySearch", "policyId", policyId);
        }
        if (forDisplay != null) {
            sc.setParameters("display", forDisplay);
        }
        return searchWrapper.search();
    }

    @DB
    protected AutoScaleVmGroupVO checkValidityAndPersist(final AutoScaleVmGroupVO vmGroup, final List<Long> passedScaleUpPolicyIds,
        final List<Long> passedScaleDownPolicyIds) {
        int minMembers = vmGroup.getMinMembers();
        int maxMembers = vmGroup.getMaxMembers();
        int interval = vmGroup.getInterval();
        List<Counter> counters = new ArrayList<Counter>();
        List<AutoScalePolicyVO> policies = new ArrayList<AutoScalePolicyVO>();
        final List<Long> policyIds = new ArrayList<Long>();
        List<Long> currentScaleUpPolicyIds = new ArrayList<Long>();
        List<Long> currentScaleDownPolicyIds = new ArrayList<Long>();
        if (vmGroup.getCreated() != null) {
            ApiDBUtils.getAutoScaleVmGroupPolicyIds(vmGroup.getId(), currentScaleUpPolicyIds, currentScaleDownPolicyIds);
        }

        if (minMembers < 0) {
            throw new InvalidParameterValueException(ApiConstants.MIN_MEMBERS + " is an invalid value: " + minMembers);
        }

        if (maxMembers < 0) {
            throw new InvalidParameterValueException(ApiConstants.MAX_MEMBERS + " is an invalid value: " + maxMembers);
        }

        if (minMembers > maxMembers) {
            throw new InvalidParameterValueException(ApiConstants.MIN_MEMBERS + " (" + minMembers + ")cannot be greater than " + ApiConstants.MAX_MEMBERS + " (" +
                maxMembers + ")");
        }

        if (interval < 0) {
            throw new InvalidParameterValueException("interval is an invalid value: " + interval);
        }

        if (passedScaleUpPolicyIds != null) {
            policies.addAll(getAutoScalePolicies("scaleuppolicyid", passedScaleUpPolicyIds, counters, interval, true));
            policyIds.addAll(passedScaleUpPolicyIds);
        } else {
            // Run the interval check for existing policies
            getAutoScalePolicies("scaleuppolicyid", currentScaleUpPolicyIds, counters, interval, true);
            policyIds.addAll(currentScaleUpPolicyIds);
        }

        if (passedScaleDownPolicyIds != null) {
            policies.addAll(getAutoScalePolicies("scaledownpolicyid", passedScaleDownPolicyIds, counters, interval, false));
            policyIds.addAll(passedScaleDownPolicyIds);
        } else {
            // Run the interval check for existing policies
            getAutoScalePolicies("scaledownpolicyid", currentScaleDownPolicyIds, counters, interval, false);
            policyIds.addAll(currentScaleDownPolicyIds);
        }
        AutoScaleVmProfileVO profileVO =
            getEntityInDatabase(CallContext.current().getCallingAccount(), ApiConstants.VMPROFILE_ID, vmGroup.getProfileId(), _autoScaleVmProfileDao);

        LoadBalancerVO loadBalancer = getEntityInDatabase(CallContext.current().getCallingAccount(), ApiConstants.LBID, vmGroup.getLoadBalancerId(), _lbDao);
        validateAutoScaleCounters(loadBalancer.getNetworkId(), counters, profileVO.getCounterParams());

        ControlledEntity[] sameOwnerEntities = policies.toArray(new ControlledEntity[policies.size() + 2]);
        sameOwnerEntities[sameOwnerEntities.length - 2] = loadBalancer;
        sameOwnerEntities[sameOwnerEntities.length - 1] = profileVO;
        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, sameOwnerEntities);

        return Transaction.execute(new TransactionCallback<AutoScaleVmGroupVO>() {
            @Override
            public AutoScaleVmGroupVO doInTransaction(TransactionStatus status) {
                AutoScaleVmGroupVO vmGroupNew = _autoScaleVmGroupDao.persist(vmGroup);

                if (passedScaleUpPolicyIds != null || passedScaleDownPolicyIds != null) {
                    _autoScaleVmGroupPolicyMapDao.removeByGroupId(vmGroupNew.getId());

                    for (Long policyId : policyIds) {
                        _autoScaleVmGroupPolicyMapDao.persist(new AutoScaleVmGroupPolicyMapVO(vmGroupNew.getId(), policyId));
                    }
                }

                return vmGroupNew;
            }
        });

    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMGROUP_UPDATE, eventDescription = "updating autoscale vm group", async = true)
    public AutoScaleVmGroup updateAutoScaleVmGroup(UpdateAutoScaleVmGroupCmd cmd) {
        Long vmGroupId = cmd.getId();
        String name = cmd.getName();
        Integer minMembers = cmd.getMinMembers();
        Integer maxMembers = cmd.getMaxMembers();
        Integer interval = cmd.getInterval();
        Boolean forDisplay = cmd.getDisplay();

        List<Long> scaleUpPolicyIds = cmd.getScaleUpPolicyIds();
        List<Long> scaleDownPolicyIds = cmd.getScaleDownPolicyIds();

        AutoScaleVmGroupVO vmGroupVO = getEntityInDatabase(CallContext.current().getCallingAccount(), "AutoScale Vm Group", vmGroupId, _autoScaleVmGroupDao);

        boolean physicalParametersUpdate = (minMembers != null || maxMembers != null || interval != null);

        if (physicalParametersUpdate && !vmGroupVO.getState().equals(AutoScaleVmGroup.State.Disabled)) {
            throw new InvalidParameterValueException("An AutoScale Vm Group can be updated with minMembers/maxMembers/Interval only when it is in disabled state");
        }

        if (StringUtils.isNotBlank(name)) {
            vmGroupVO.setName(name);
        }

        if (minMembers != null) {
            vmGroupVO.setMinMembers(minMembers);
        }

        if (maxMembers != null) {
            vmGroupVO.setMaxMembers(maxMembers);
        }

        if (interval != null) {
            vmGroupVO.setInterval(interval);
        }

        if (cmd.getCustomId() != null) {
            vmGroupVO.setUuid(cmd.getCustomId());
        }

        if (forDisplay != null) {
            vmGroupVO.setDisplay(forDisplay);
        }

        vmGroupVO = checkValidityAndPersist(vmGroupVO, scaleUpPolicyIds, scaleDownPolicyIds);
        if (vmGroupVO != null) {
            s_logger.debug("Updated Auto Scale VmGroup id:" + vmGroupId);
            return vmGroupVO;
        } else
            return null;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMGROUP_ENABLE, eventDescription = "enabling autoscale vm group", async = true)
    public AutoScaleVmGroup enableAutoScaleVmGroup(Long id) {
        AutoScaleVmGroupVO vmGroup = getEntityInDatabase(CallContext.current().getCallingAccount(), "AutoScale Vm Group", id, _autoScaleVmGroupDao);
        boolean success = false;
        if (!vmGroup.getState().equals(AutoScaleVmGroup.State.Disabled)) {
            throw new InvalidParameterValueException("Only a AutoScale Vm Group which is in Disabled state can be enabled.");
        }

        try {
            vmGroup.setState(AutoScaleVmGroup.State.Enabled);
            vmGroup = _autoScaleVmGroupDao.persist(vmGroup);
            success = configureAutoScaleVmGroup(id, AutoScaleVmGroup.State.Disabled);
            scheduleMonitorTask(vmGroup.getId());
        } catch (ResourceUnavailableException e) {
            vmGroup.setState(AutoScaleVmGroup.State.Disabled);
            _autoScaleVmGroupDao.persist(vmGroup);
        } finally {
            if (!success) {
                s_logger.warn("Failed to enable AutoScale Vm Group id : " + id);
                return null;
            }
            s_logger.info("Successfully enabled AutoScale Vm Group with Id:" + id);
        }
        return vmGroup;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMGROUP_DISABLE, eventDescription = "disabling autoscale vm group", async = true)
    @DB
    public AutoScaleVmGroup disableAutoScaleVmGroup(Long id) {
        AutoScaleVmGroupVO vmGroup = getEntityInDatabase(CallContext.current().getCallingAccount(), "AutoScale Vm Group", id, _autoScaleVmGroupDao);
        boolean success = false;
        if (!vmGroup.getState().equals(AutoScaleVmGroup.State.Enabled)) {
            throw new InvalidParameterValueException("Only a AutoScale Vm Group which is in Enabled state can be disabled.");
        }

        try {
            vmGroup.setState(AutoScaleVmGroup.State.Disabled);
            vmGroup = _autoScaleVmGroupDao.persist(vmGroup);
            success = configureAutoScaleVmGroup(id, AutoScaleVmGroup.State.Enabled);
            _asGroupStatisticsDao.removeByGroupId(id);
            cancelCheckTask(vmGroup.getId());
        } catch (ResourceUnavailableException e) {
            vmGroup.setState(AutoScaleVmGroup.State.Enabled);
            _autoScaleVmGroupDao.persist(vmGroup);
        } finally {
            if (!success) {
                s_logger.warn("Failed to disable AutoScale Vm Group id : " + id);
                return null;
            }
            s_logger.info("Successfully disabled AutoScale Vm Group with Id:" + id);
        }
        return vmGroup;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_COUNTER_CREATE, eventDescription = "Counter", create = true)
    @DB
    public Counter createCounter(CreateCounterCmd cmd) {
        String source = cmd.getSource().toLowerCase();
        String name = cmd.getName();
        Counter.Source src;
        // Validate Source
        try {
            src = Counter.Source.valueOf(source);
        } catch (Exception ex) {
            throw new InvalidParameterValueException("The Source " + source + " does not exist; Unable to create Counter");
        }

        // Validate Provider
        Network.Provider provider = Network.Provider.getProvider(cmd.getProvider());;
        if (provider == null) {
            throw new InvalidParameterValueException("The Provider " + cmd.getProvider() + " does not exist; Unable to create Counter");
        }

        CounterVO counter = null;

        s_logger.debug("Adding Counter " + name);
        counter = _counterDao.persist(new CounterVO(src, name, cmd.getValue(), provider));

        CallContext.current().setEventDetails(" Id: " + counter.getId() + " Name: " + name);
        return counter;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CONDITION_CREATE, eventDescription = "Condition", create = true)
    public Condition createCondition(CreateConditionCmd cmd) {
        checkCallerAccess(cmd.getAccountName(), cmd.getDomainId());
        String opr = cmd.getRelationalOperator().toUpperCase();
        long cid = cmd.getCounterId();
        long threshold = cmd.getThreshold();
        Condition.Operator op;
        // Validate Relational Operator
        try {
            op = Condition.Operator.valueOf(opr);
        } catch (IllegalArgumentException ex) {
            throw new InvalidParameterValueException("The Operator " + opr + " does not exist; Unable to create Condition.");
        }
        // TODO - Validate threshold

        CounterVO counter = _counterDao.findById(cid);

        if (counter == null) {
            throw new InvalidParameterValueException("Unable to find counter");
        }
        ConditionVO condition = null;

        condition = _conditionDao.persist(new ConditionVO(cid, threshold, cmd.getEntityOwnerId(), cmd.getDomainId(), op));
        s_logger.info("Successfully created condition with Id: " + condition.getId());

        CallContext.current().setEventDetails(" Id: " + condition.getId());
        return condition;
    }

    @Override
    public List<? extends Counter> listCounters(ListCountersCmd cmd) {
        String name = cmd.getName();
        Long id = cmd.getId();
        String source = cmd.getSource();
        if (source != null) {
            source = source.toLowerCase();
        }
        String providerStr = cmd.getProvider();
        if (providerStr != null) {
            Network.Provider provider = Network.Provider.getProvider(providerStr);
            if (provider == null) {
                throw new InvalidParameterValueException("The Provider " + providerStr + " does not exist; Unable to list Counter");
            }
            providerStr = provider.getName();
        }

        Filter searchFilter = new Filter(CounterVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        List<CounterVO> counters = _counterDao.listCounters(id, name, source, providerStr, cmd.getKeyword(), searchFilter);

        return counters;
    }

    @Override
    public List<? extends Condition> listConditions(ListConditionsCmd cmd) {
        Long id = cmd.getId();
        Long counterId = cmd.getCounterId();
        Long policyId = cmd.getPolicyId();
        SearchWrapper<ConditionVO> searchWrapper = new SearchWrapper<ConditionVO>(_conditionDao, ConditionVO.class, cmd, cmd.getId());
        SearchBuilder<ConditionVO> sb = searchWrapper.getSearchBuilder();
        if (policyId != null) {
            SearchBuilder<AutoScalePolicyConditionMapVO> asPolicyConditionSearch = _autoScalePolicyConditionMapDao.createSearchBuilder();
            asPolicyConditionSearch.and("policyId", asPolicyConditionSearch.entity().getPolicyId(), SearchCriteria.Op.EQ);
            sb.join("asPolicyConditionSearch", asPolicyConditionSearch, sb.entity().getId(), asPolicyConditionSearch.entity().getConditionId(),
                JoinBuilder.JoinType.INNER);
        }

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("counterId", sb.entity().getCounterId(), SearchCriteria.Op.EQ);

        // now set the SC criteria...
        SearchCriteria<ConditionVO> sc = searchWrapper.buildSearchCriteria();

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (counterId != null) {
            sc.setParameters("counterId", counterId);
        }

        if (policyId != null) {
            sc.setJoinParameters("asPolicyConditionSearch", "policyId", policyId);
        }

        return searchWrapper.search();
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_COUNTER_DELETE, eventDescription = "counter")
    public boolean deleteCounter(long counterId) throws ResourceInUseException {
        // Verify Counter id
        CounterVO counter = _counterDao.findById(counterId);
        if (counter == null) {
            throw new InvalidParameterValueException("Unable to find Counter");
        }

        // Verify if it is used in any Condition

        ConditionVO condition = _conditionDao.findByCounterId(counterId);
        if (condition != null) {
            s_logger.info("Cannot delete counter " + counter.getName() + " as it is being used in a condition.");
            throw new ResourceInUseException("Counter is in use.");
        }

        boolean success = _counterDao.remove(counterId);
        if (success) {
            s_logger.info("Successfully deleted counter with Id: " + counterId);
        }

        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CONDITION_DELETE, eventDescription = "delete a condition")
    public boolean deleteCondition(long conditionId) throws ResourceInUseException {
        /* Check if entity is in database */
        ConditionVO condition = getEntityInDatabase(CallContext.current().getCallingAccount(), "Condition", conditionId, _conditionDao);
        if (condition == null) {
            throw new InvalidParameterValueException("Unable to find Condition");
        }

        // Verify if condition is used in any autoscale policy
        if (_autoScalePolicyConditionMapDao.isConditionInUse(conditionId)) {
            s_logger.info("Cannot delete condition " + conditionId + " as it is being used in a condition.");
            throw new ResourceInUseException("Cannot delete Condition when it is in use by one or more AutoScale Policies.");
        }
        boolean success = _conditionDao.remove(conditionId);
        if (success) {
            s_logger.info("Successfully deleted condition " + condition.getId());
        }
        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CONDITION_UPDATE, eventDescription = "update a condition")
    public Condition updateCondition(UpdateConditionCmd cmd) throws ResourceInUseException {
        Long conditionId = cmd.getId();
        /* Check if entity is in database */
        ConditionVO condition = getEntityInDatabase(CallContext.current().getCallingAccount(), "Condition", conditionId, _conditionDao);
        if (condition == null) {
            throw new InvalidParameterValueException("Unable to find Condition");
        }

        String operator = cmd.getRelationalOperator();
        Long threshold = cmd.getThreshold();

        Condition.Operator op;
        // Validate Relational Operator
        try {
            op = Condition.Operator.valueOf(operator);
        } catch (IllegalArgumentException ex) {
            throw new InvalidParameterValueException("The Operator " + operator + " does not exist; Unable to update Condition.");
        }

        // Verify if condition is used in any autoscale vmgroup
        GenericSearchBuilder<AutoScalePolicyConditionMapVO, Long> conditionSearch = _autoScalePolicyConditionMapDao.createSearchBuilder(Long.class);
        conditionSearch.selectFields(conditionSearch.entity().getPolicyId());
        conditionSearch.and("conditionId", conditionSearch.entity().getConditionId(), Op.EQ);
        SearchCriteria<Long> sc = conditionSearch.create();
        sc.setParameters("conditionId", conditionId);
        List<Long> policyIds = _autoScalePolicyConditionMapDao.customSearch(sc, null);

        if (CollectionUtils.isNotEmpty(policyIds)) {
            SearchBuilder<AutoScaleVmGroupPolicyMapVO> policySearch = _autoScaleVmGroupPolicyMapDao.createSearchBuilder();
            policySearch.and("policyId", policySearch.entity().getPolicyId(), Op.IN);
            SearchBuilder<AutoScaleVmGroupVO> vmGroupSearch = _autoScaleVmGroupDao.createSearchBuilder();
            vmGroupSearch.and("stateNEQ", vmGroupSearch.entity().getState(), Op.NEQ);
            vmGroupSearch.join("policySearch", policySearch, vmGroupSearch.entity().getId(), policySearch.entity().getVmGroupId(), JoinBuilder.JoinType.INNER);
            vmGroupSearch.done();

            SearchCriteria<AutoScaleVmGroupVO> sc2 = vmGroupSearch.create();
            sc2.setParameters("stateNEQ", AutoScaleVmGroup.State.Disabled);
            sc2.setJoinParameters("policySearch", "policyId", policyIds.toArray((new Object[policyIds.size()])));
            List<AutoScaleVmGroupVO> groups = _autoScaleVmGroupDao.search(sc2, null);
            if (CollectionUtils.isNotEmpty(groups)) {
                String msg = String.format("Cannot update condition %d as it is being used in %d vm groups.", conditionId, groups.size());
                s_logger.info(msg);
                throw new ResourceInUseException(msg);
            }
        }

        condition.setRelationalOperator(op);
        condition.setThreshold(threshold);
        boolean success = _conditionDao.update(conditionId, condition);
        if (success) {
            s_logger.info("Successfully updated condition " + condition.getId());
        }
        return condition;
    }

    @Override
    public void cleanUpAutoScaleResources(Long accountId) {
        // cleans Autoscale VmProfiles, AutoScale Policies and Conditions belonging to an account
        int count = 0;
        count = _autoScaleVmProfileDao.removeByAccountId(accountId);
        if (count > 0) {
            s_logger.debug("Deleted " + count + " AutoScale Vm Profile for account Id: " + accountId);
        }
        count = _autoScalePolicyDao.removeByAccountId(accountId);
        if (count > 0) {
            s_logger.debug("Deleted " + count + " AutoScale Policies for account Id: " + accountId);
        }
        count = _conditionDao.removeByAccountId(accountId);
        if (count > 0) {
            s_logger.debug("Deleted " + count + " Conditions for account Id: " + accountId);
        }
    }

    private boolean checkConditionUp(AutoScaleVmGroupVO asGroup, Integer numVm) {
        // check maximum
        Integer currentVM = _autoScaleVmGroupVmMapDao.countByGroup(asGroup.getId());
        Integer maxVm = asGroup.getMaxMembers();
        if (currentVM + numVm > maxVm) {
            s_logger.warn("number of VM will greater than the maximum in this group if scaling up, so do nothing more");
            return false;
        }
        return true;
    }

    private boolean checkConditionDown(AutoScaleVmGroupVO asGroup) {
        Integer currentVM = _autoScaleVmGroupVmMapDao.countByGroup(asGroup.getId());
        Integer minVm = asGroup.getMinMembers();
        if (currentVM - 1 < minVm) {
            s_logger.warn("number of VM will less than the minimum in this group if scaling down, so do nothing more");
            return false;
        }
        return true;
    }

    private Map<String, String> getDeployParams (String otherDeployParams) {
        Map<String, String> deployParams = new HashMap<>();
        if (StringUtils.isNotBlank(otherDeployParams)) {
            for (String param : otherDeployParams.split("&")) {
                if (param.split("=").length >= 2) {
                    deployParams.put(param.split("=")[0], param.split("=")[1]);
                }
            }
        }
        return deployParams;
    }

    private long createNewVM(AutoScaleVmGroupVO asGroup) {
        AutoScaleVmProfileVO profileVo = _autoScaleVmProfileDao.findById(asGroup.getProfileId());
        long templateId = profileVo.getTemplateId();
        long serviceOfferingId = profileVo.getServiceOfferingId();
        if (templateId == -1) {
            return -1;
        }
        // create new VM into DB
        try {
            //Verify that all objects exist before passing them to the service
            Account owner = _accountService.getActiveAccountById(profileVo.getAccountId());

            DataCenter zone = _entityMgr.findById(DataCenter.class, profileVo.getZoneId());
            if (zone == null) {
                throw new InvalidParameterValueException("Unable to find zone by id=" + profileVo.getZoneId());
            }

            ServiceOffering serviceOffering = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
            if (serviceOffering == null) {
                throw new InvalidParameterValueException("Unable to find service offering: " + serviceOfferingId);
            }

            DiskOffering diskOffering = _entityMgr.findById(DiskOffering.class, serviceOffering.getDiskOfferingId());
            if (diskOffering == null) {
                throw new InvalidParameterValueException("Unable to find disk offering: " + serviceOffering.getDiskOfferingId());
            }

            VirtualMachineTemplate template = _entityMgr.findById(VirtualMachineTemplate.class, templateId);
            // Make sure a valid template ID was specified
            if (template == null) {
                throw new InvalidParameterValueException("Unable to use template " + templateId);
            }

            if (!zone.isLocalStorageEnabled()) {
                if (diskOffering.isUseLocalStorage()) {
                    throw new InvalidParameterValueException("Zone is not configured to use local storage but disk offering " + diskOffering.getName() + " associated to the service offering " + serviceOffering.getName() + " uses it");
                }
            }

            UserVm vm = null;
            IpAddresses addrs = new IpAddresses(null, null);
            HypervisorType hypervisorType = template.getHypervisorType();
            final Network network = getNetwork(asGroup.getLoadBalancerId());
            final List<Long> networkIds = new ArrayList<>();
            networkIds.add(network.getId());
            Map<String, String> customParameters = new HashMap<String, String>();
            List<String> sshKeyPairs = new ArrayList<>();

            Map<String, String> deployParams = getDeployParams(profileVo.getOtherDeployParams());
            if (deployParams.get("cpunumber") != null) {    // CPU number
                String value = deployParams.get("cpunumber");
                try {
                    Long cpuNumber = Long.parseLong(value);
                    customParameters.put(VmDetailConstants.CPU_NUMBER, String.valueOf(cpuNumber));
                } catch (NumberFormatException ex) {
                    s_logger.warn("Cannot parse cpunumber from otherdeployparams in AutoScale Vm profile");
                }
            }
            if (deployParams.get("cpuspeed") != null) {     // CPU speed
                String value = deployParams.get("cpuspeed");
                try {
                    Long cpuSpeed = Long.parseLong(value);
                    customParameters.put(VmDetailConstants.CPU_SPEED, String.valueOf(cpuSpeed));
                } catch (NumberFormatException ex) {
                    s_logger.warn("Cannot parse cpuspeed from otherdeployparams in AutoScale Vm profile");
                }
            }
            if (deployParams.get("memory") != null) {       // memory
                String value = deployParams.get("memory");
                try {
                    Long memory = Long.parseLong(value);
                    customParameters.put(VmDetailConstants.MEMORY, String.valueOf(memory));
                } catch (NumberFormatException ex) {
                    s_logger.warn("Cannot parse memory from otherdeployparams in AutoScale Vm profile");
                }
            }
            if (deployParams.get("rootdisksize") != null) {     // ROOT disk size
                String value = deployParams.get("rootdisksize");
                try {
                    Long rootDiskSize = Long.parseLong(value);
                    customParameters.put(VmDetailConstants.ROOT_DISK_SIZE, String.valueOf(rootDiskSize));
                } catch (NumberFormatException ex) {
                    s_logger.warn("Cannot parse rootdisksize from otherdeployparams in AutoScale Vm profile");
                }
            }
            Long overrideDiskOfferingId = null;     // override ROOT disk offering
            if (deployParams.get("overridediskofferingid") != null) {
                String overrideDiskOfferingUuid = deployParams.get("overridediskofferingid");
                DiskOffering overrideDiskOfferingInParam = _diskOfferingDao.findByUuid(overrideDiskOfferingUuid);
                if (overrideDiskOfferingInParam != null) {
                    overrideDiskOfferingId = overrideDiskOfferingInParam.getId();
                } else {
                    s_logger.warn("Cannot find disk offering by overridediskofferingid from otherdeployparams in AutoScale Vm profile");
                }
            }
            Long diskOfferingId = null;     // DATA disk offering ID
            if (deployParams.get("diskofferingid") != null) {
                String diskOfferingUuid = deployParams.get("diskofferingid");
                DiskOffering diskOfferingInParam = _diskOfferingDao.findByUuid(diskOfferingUuid);
                if (diskOfferingInParam != null) {
                    diskOfferingId = diskOfferingInParam.getId();
                } else {
                    s_logger.warn("Cannot find disk offering by diskofferingid from otherdeployparams in AutoScale Vm profile");
                }
            }
            Long dataDiskSize = null;       // DATA disk size
            if (deployParams.get("size") != null) {
                String dataDiskSizeInParam = deployParams.get("size");
                try {
                    dataDiskSize = Long.parseLong(dataDiskSizeInParam);
                } catch (NumberFormatException ex) {
                    s_logger.warn("Cannot parse size from otherdeployparams in AutoScale Vm profile");
                }
            }

            if (zone.getNetworkType() == NetworkType.Basic) {
                vm = _userVmService.createBasicSecurityGroupVirtualMachine(zone, serviceOffering, template, null, owner, "autoScaleVm-" + asGroup.getId() + "-" +
                    getCurrentTimeStampString(),
                    "autoScaleVm-" + asGroup.getId() + "-" + getCurrentTimeStampString(), diskOfferingId, dataDiskSize, null,
                    hypervisorType, HTTPMethod.GET, null, sshKeyPairs, null,
                    null, true, null, null, customParameters, null, null, null,
                    null, true, overrideDiskOfferingId);
            } else {
                if (zone.isSecurityGroupEnabled()) {
                    vm = _userVmService.createAdvancedSecurityGroupVirtualMachine(zone, serviceOffering, template, networkIds, null,
                        owner, "autoScaleVm-" + asGroup.getId() + "-" + getCurrentTimeStampString(),
                        "autoScaleVm-" + asGroup.getId() + "-" + getCurrentTimeStampString(), diskOfferingId, dataDiskSize, null,
                        hypervisorType, HTTPMethod.GET, null, sshKeyPairs,null,
                        null, true, null, null, customParameters, null, null, null,
                        null, true, overrideDiskOfferingId, null);
                } else {
                    vm = _userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner, "autoScaleVm-" + asGroup.getId() + "-" +
                        getCurrentTimeStampString(), "autoScaleVm-" + asGroup.getId() + "-" + getCurrentTimeStampString(),
                            diskOfferingId, dataDiskSize, null,
                        hypervisorType, HTTPMethod.GET, null, sshKeyPairs,null,
                        addrs, true, null, null, customParameters, null, null, null,
                        null, true, null, overrideDiskOfferingId);
                }
            }

            if (vm != null) {
                return vm.getId();
            } else {
                return -1;
            }
        } catch (InsufficientCapacityException ex) {
            s_logger.info(ex);
            s_logger.trace(ex.getMessage(), ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (ResourceAllocationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_ALLOCATION_ERROR, ex.getMessage());
        }
    }

    private String getCurrentTimeStampString() {
        Date current = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

        return sdf.format(current);
    }

    private boolean startNewVM(long vmId) {
        try {
            CallContext.current().setEventDetails("Vm Id: " + vmId);
            _userVmManager.startVirtualMachine(vmId, null, null, null);
        } catch (final ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (ResourceAllocationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_ALLOCATION_ERROR, ex.getMessage());
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (InsufficientCapacityException ex) {
            StringBuilder message = new StringBuilder(ex.getMessage());
            if (ex instanceof InsufficientServerCapacityException) {
                if (((InsufficientServerCapacityException)ex).isAffinityApplied()) {
                    message.append(", Please check the affinity groups provided, there may not be sufficient capacity to follow them");
                }
            }
            s_logger.info(ex);
            s_logger.info(message.toString(), ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, message.toString());
        }
        return true;
    }

    private boolean assignLBruleToNewVm(long vmId, AutoScaleVmGroupVO asGroup) {
        List<Long> lstVmId = new ArrayList<Long>();
        long lbId = asGroup.getLoadBalancerId();

        List<LoadBalancerVMMapVO> LbVmMapVos = _lbVmMapDao.listByLoadBalancerId(lbId);
        if ((LbVmMapVos != null) && (LbVmMapVos.size() > 0)) {
            for (LoadBalancerVMMapVO LbVmMapVo : LbVmMapVos) {
                long instanceId = LbVmMapVo.getInstanceId();
                if (instanceId == vmId) {
                    s_logger.warn("the new VM is already mapped to LB rule. What's wrong?");
                    return true;
                }
            }
        }
        lstVmId.add(new Long(vmId));
        return _loadBalancingRulesService.assignToLoadBalancer(lbId, lstVmId, new HashMap<Long, List<String>>());

    }

    private long removeLBrule(AutoScaleVmGroupVO asGroup) {
        long lbId = asGroup.getLoadBalancerId();
        long instanceId = -1;
        List<LoadBalancerVMMapVO> LbVmMapVos = _lbVmMapDao.listByLoadBalancerId(lbId);
        if ((LbVmMapVos != null) && (LbVmMapVos.size() > 0)) {
            for (LoadBalancerVMMapVO LbVmMapVo : LbVmMapVos) {
                instanceId = LbVmMapVo.getInstanceId();
            }
        }
        // take last VM out of the list
        List<Long> lstVmId = new ArrayList<Long>();
        if (instanceId != -1)
            lstVmId.add(instanceId);
        if (_loadBalancingRulesService.removeFromLoadBalancer(lbId, lstVmId, new HashMap<Long, List<String>>()))
            return instanceId;
        else
            return -1;
    }

    @Override
    public void doScaleUp(long groupId, Integer numVm) {
        AutoScaleVmGroupVO asGroup = _autoScaleVmGroupDao.findById(groupId);
        if (asGroup == null) {
            s_logger.error("Can not find the groupid " + groupId + " for scaling up");
            return;
        }
        if (!checkConditionUp(asGroup, numVm)) {
            return;
        }
        AutoScaleVmGroup.State oldState = asGroup.getState();
        AutoScaleVmGroup.State newState = AutoScaleVmGroup.State.Scaling;
        if (!_autoScaleVmGroupDao.updateState(groupId, oldState, newState)) {
            s_logger.error(String.format("Can not update vmgroup state from %s to %s, groupId: %s", oldState, newState, groupId));
            return;
        }
        for (int i = 0; i < numVm; i++) {
            long vmId = createNewVM(asGroup);
            if (vmId == -1) {
                s_logger.error("Can not deploy new VM for scaling up in the group "
                    + asGroup.getId() + ". Waiting for next round");
                break;
            }
            if (startNewVM(vmId)) {
                if (assignLBruleToNewVm(vmId, asGroup)) {
                    // persist to DB
                    AutoScaleVmGroupVmMapVO GroupVmVO = new AutoScaleVmGroupVmMapVO(
                        asGroup.getId(), vmId);
                    _autoScaleVmGroupVmMapDao.persist(GroupVmVO);
                    // update last_quiettime
                    List<AutoScaleVmGroupPolicyMapVO> GroupPolicyVOs = _autoScaleVmGroupPolicyMapDao
                        .listByVmGroupId(groupId);
                    for (AutoScaleVmGroupPolicyMapVO GroupPolicyVO : GroupPolicyVOs) {
                        AutoScalePolicyVO vo = _autoScalePolicyDao
                            .findById(GroupPolicyVO.getPolicyId());
                        if (vo.getAction().equals(AutoScalePolicy.Action.ScaleUp)) {
                            vo.setLastQuietTime(new Date());
                            _autoScalePolicyDao.persist(vo);
                            break;
                        }
                    }
                } else {
                    s_logger.error("Can not assign LB rule for this new VM");
                    break;
                }
            } else {
                s_logger.error("Can not deploy new VM for scaling up in the group "
                    + asGroup.getId() + ". Waiting for next round");
                break;
            }
        }
        if (!_autoScaleVmGroupDao.updateState(groupId, newState, oldState)) {
            s_logger.error(String.format("Can not update vmgroup state from %s back to %s, groupId: %s", newState, oldState, groupId));
        }
    }

    @Override
    public void doScaleDown(final long groupId) {
        AutoScaleVmGroupVO asGroup = _autoScaleVmGroupDao.findById(groupId);
        if (asGroup == null) {
            s_logger.error("Can not find the groupid " + groupId + " for scaling down");
            return;
        }
        if (!checkConditionDown(asGroup)) {
            return;
        }
        AutoScaleVmGroup.State oldState = asGroup.getState();
        AutoScaleVmGroup.State newState = AutoScaleVmGroup.State.Scaling;
        if (!_autoScaleVmGroupDao.updateState(groupId, oldState, newState)) {
            s_logger.error(String.format("Can not update vmgroup state from %s to %s, groupId: %s", oldState, newState, groupId));
            return;
        }
        final long vmId = removeLBrule(asGroup);
        if (vmId != -1) {
            long profileId = asGroup.getProfileId();

            // update group-vm mapping
            _autoScaleVmGroupVmMapDao.remove(groupId, vmId);
            // update last_quiettime
            List<AutoScaleVmGroupPolicyMapVO> GroupPolicyVOs = _autoScaleVmGroupPolicyMapDao.listByVmGroupId(groupId);
            for (AutoScaleVmGroupPolicyMapVO GroupPolicyVO : GroupPolicyVOs) {
                AutoScalePolicyVO vo = _autoScalePolicyDao.findById(GroupPolicyVO.getPolicyId());
                if (vo.getAction().equals(AutoScalePolicy.Action.ScaleUp)) {
                    vo.setLastQuietTime(new Date());
                    _autoScalePolicyDao.persist(vo);
                    break;
                }
            }

            // get destroyvmgrace param
            AutoScaleVmProfileVO asProfile = _autoScaleVmProfileDao.findById(profileId);
            Integer destroyVmGracePeriod = asProfile.getDestroyVmGraceperiod();
            if (destroyVmGracePeriod >= 0) {
                _executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            _userVmManager.destroyVm(vmId, false);

                        } catch (ResourceUnavailableException e) {
                            e.printStackTrace();
                        } catch (ConcurrentOperationException e) {
                            e.printStackTrace();
                        }
                    }
                }, destroyVmGracePeriod, TimeUnit.SECONDS);
            }
        } else {
            s_logger.error("Can not remove LB rule for the VM being destroyed. Do nothing more.");
        }
        if (!_autoScaleVmGroupDao.updateState(groupId, newState, oldState)) {
            s_logger.error(String.format("Can not update vmgroup state from %s back to %s, groupId: %s", newState, oldState, groupId));
        }
    }

    @Override
    public String getConfigComponentName() {
        return AutoScaleManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                AutoScaleStatsInterval,
                AutoScaleStatsCleanupDelay,
                AutoScaleStatsWorker
        };
    }

    @Override
    public void checkAllAutoScaleVmGroups() {
        // list all AS VMGroups
        List<AutoScaleVmGroupVO> asGroups = _autoScaleVmGroupDao.listAll();
        for (AutoScaleVmGroupVO asGroup : asGroups) {
            _completionService.submit(new CheckAutoScaleVmGroupAsync(asGroup));
        }
        try {
            for (AutoScaleVmGroupVO asGroup : asGroups) {
                Future<Pair<Long, Boolean>> future = _completionService.take();
                Pair<Long, Boolean> result = future.get();
                s_logger.debug("Checked AutoScale vm group " + result.first() + " with result: " + result.second());
            }
        } catch (ExecutionException ex) {
            s_logger.warn("Failed to get result of checking AutoScale vm group due to Exception: " , ex);
        } catch (InterruptedException ex) {
            s_logger.warn("Failed to get result of checking AutoScale vm group due to Exception: " , ex);
        }
    }

    protected class CheckAutoScaleVmGroupAsync implements Callable<Pair<Long, Boolean>> {
        AutoScaleVmGroupVO asGroup;

        public CheckAutoScaleVmGroupAsync(AutoScaleVmGroupVO asGroup) {
            this.asGroup = asGroup;
        }

        @Override
        public Pair<Long, Boolean> call() {
            try {
                s_logger.debug("Checking AutoScale vm group " + asGroup);
                checkAutoScaleVmGroup(asGroup);
            } catch (Exception ex) {
                s_logger.warn("Failed to check AutoScale vm group " + asGroup + " due to Exception: " , ex);
                return new Pair<>(asGroup.getId(), false);
            }
            return new Pair<>(asGroup.getId(), true);
        }
    }

    @Override
    public void checkAutoScaleVmGroup(AutoScaleVmGroupVO asGroup) {
        // check group state
        if (asGroup.getState().equals(AutoScaleVmGroup.State.Enabled)) {
            Network.Provider provider = getLoadBalancerServiceProvider(asGroup.getLoadBalancerId());
            if (Network.Provider.Netscaler.equals(provider)) {
                checkNetScalerAsGroup(asGroup);
            } else if (Network.Provider.VirtualRouter.equals(provider) || Network.Provider.VPCVirtualRouter.equals(provider)) {
                checkVirtualRouterAsGroup(asGroup);
            }
        }
    }

    private void monitorAutoScaleVmGroup(Long groupId) {
        AutoScaleVmGroupVO asGroup = _autoScaleVmGroupDao.findById(groupId);
        s_logger.debug("Start monitoring on AutoScale VmGroup " + asGroup);
        // check group state
        if (asGroup.getState().equals(AutoScaleVmGroup.State.Enabled)) {
            Network.Provider provider = getLoadBalancerServiceProvider(asGroup.getLoadBalancerId());
            if (Network.Provider.Netscaler.equals(provider)) {
                s_logger.debug("Skipping the monitoring on AutoScale VmGroup with Netscaler provider: " + asGroup);
            } else if (Network.Provider.VirtualRouter.equals(provider) || Network.Provider.VPCVirtualRouter.equals(provider)) {
                monitorVirtualRouterAsGroup(asGroup);
            }
        }
    }
    private boolean is_native(AutoScaleVmGroupTO groupTO) {
        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            for (ConditionTO conditionTO : policyTO.getConditions()) {
                CounterTO counterTO = conditionTO.getCounter();
                if (Counter.NativeSources.contains(counterTO.getSource())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean has_source_virtual_router(AutoScaleVmGroupTO groupTO) {
        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            for (ConditionTO conditionTO : policyTO.getConditions()) {
                CounterTO counterTO = conditionTO.getCounter();
                if (Counter.Source.virtualrouter.equals(counterTO.getSource())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<Long, List<CounterTO>> getCounters(AutoScaleVmGroupTO groupTO) {
        Map<Long, List<CounterTO>> counters = new HashMap<>();
        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            List<CounterTO> counterTOs = new ArrayList<>();
            for (ConditionTO conditionTO : policyTO.getConditions()) {
                CounterTO counterTO = conditionTO.getCounter();
                counterTOs.add(counterTO);
            }
            counters.put(policyTO.getId(), counterTOs);
        }
        return counters;
    }

    private AutoScalePolicy.Action getAutoscaleAction(Map<String, Double> countersMap, Map<String, Integer> countersNumberMap, AutoScaleVmGroupTO groupTO) {
        s_logger.debug("[AutoScale] Getting autoscale action for group : " + groupTO.getId());

        Network.Provider provider = getLoadBalancerServiceProvider(groupTO.getLoadBalancerId());

        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            int quiettime = policyTO.getQuietTime();
            Date quiettimeDate = policyTO.getLastQuietTime();
            long last_quiettime = 0L;
            if (quiettimeDate != null) {
                last_quiettime = policyTO.getLastQuietTime().getTime();
            }
            long current_time = (new Date()).getTime();

            // check quiet time for this policy
            if ((current_time - last_quiettime) >= (long)quiettime) {
                // check whole conditions of this policy
                boolean bValid = true;
                for (ConditionTO conditionTO : policyTO.getConditions()) {
                    CounterTO counter = conditionTO.getCounter();
                    long thresholdValue = conditionTO.getThreshold();
                    Double thresholdPercent = (double)thresholdValue;

                    String key = generateKeyFromPolicyAndConditionAndCounter(policyTO.getId(), conditionTO.getId(), counter.getId());
                    if (Network.Provider.Netscaler.equals(provider)) {
                        key = generateKeyFromPolicyAndConditionAndCounter(0L, conditionTO.getId(), counter.getId());
                    }
                    Double sum = countersMap.get(key);
                    Integer number = countersNumberMap.get(key);
                    s_logger.debug(String.format("policyId = %d, conditionId = %d, counter = %s, sum = %f, number = %s", policyTO.getId(), conditionTO.getId(), counter.getSource(), sum, number));
                    if (number == null || number == 0) {
                        bValid = false;
                        break;
                    }
                    Double avg = sum / number;
                    Condition.Operator op = conditionTO.getRelationalOperator();
                    boolean bConditionCheck = ((op == com.cloud.network.as.Condition.Operator.EQ) && (thresholdPercent.equals(avg)))
                            || ((op == com.cloud.network.as.Condition.Operator.GE) && (avg.doubleValue() >= thresholdPercent.doubleValue()))
                            || ((op == com.cloud.network.as.Condition.Operator.GT) && (avg.doubleValue() > thresholdPercent.doubleValue()))
                            || ((op == com.cloud.network.as.Condition.Operator.LE) && (avg.doubleValue() <= thresholdPercent.doubleValue()))
                            || ((op == com.cloud.network.as.Condition.Operator.LT) && (avg.doubleValue() < thresholdPercent.doubleValue()));

                    if (!bConditionCheck) {
                        bValid = false;
                        break;
                    }
                }
                if (bValid) {
                    return policyTO.getAction();
                }
            }
        }
        return null;
    }

    public List<Pair<String, Integer>> getPairofCounternameAndDuration(AutoScaleVmGroupTO groupTO) {
        List<Pair<String, Integer>> result = new ArrayList<Pair<String, Integer>>();

        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            //get duration
            Integer duration = policyTO.getDuration();
            for (ConditionTO conditionTO : policyTO.getConditions()) {
                CounterTO counter = conditionTO.getCounter();
                StringBuffer buff = new StringBuffer();
                buff.append(counter.getName());
                buff.append(",");
                buff.append(conditionTO.getId());
                // add to result
                Pair<String, Integer> pair = new Pair<String, Integer>(buff.toString(), duration);
                result.add(pair);
            }
        }
        return result;
    }

    private Network getNetwork(Long loadBalancerId) {
        final LoadBalancerVO loadBalancer = _lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            throw new CloudRuntimeException(String.format("Unable to find load balancer with id: % ", loadBalancerId));
        }
        Network network = _networkDao.findById(loadBalancer.getNetworkId());
        if (network == null) {
            throw new CloudRuntimeException(String.format("Unable to find network with id: % ", loadBalancer.getNetworkId()));
        }
        return network;
    }

    private Pair<String, Integer> getPublicIpAndPort(Long loadBalancerId) {
        final LoadBalancerVO loadBalancer = _lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            throw new CloudRuntimeException(String.format("Unable to find load balancer with id: % ", loadBalancerId));
        }
        IPAddressVO ipAddress = _ipAddressDao.findById(loadBalancer.getSourceIpAddressId());
        if (ipAddress == null) {
            throw new CloudRuntimeException(String.format("Unable to find IP Address with id: % ", loadBalancer.getSourceIpAddressId()));
        }
        return new Pair<>(ipAddress.getAddress().addr(), loadBalancer.getSourcePortStart());
    }

    private Network.Provider getLoadBalancerServiceProvider(Long loadBalancerId) {
        Network network = getNetwork(loadBalancerId);
        List<Network.Provider> providers = _networkMgr.getProvidersForServiceInNetwork(network, Network.Service.Lb);
        if (providers == null || providers.size() == 0) {
            throw new CloudRuntimeException(String.format("Unable to find LB provider for network with id: % ", network.getId()));
        }
        return providers.get(0);
    }

    private void checkNetScalerAsGroup(AutoScaleVmGroupVO asGroup) {
        AutoScaleVmGroupTO groupTO = _lbRulesMgr.toAutoScaleVmGroupTO(asGroup);

        if (!is_native(groupTO)) {
            return;
        }
        // check minimum vm of group
        Integer currentVM = _autoScaleVmGroupVmMapDao.countByGroup(asGroup.getId());
        if (currentVM < asGroup.getMinMembers()) {
            doScaleUp(asGroup.getId(), asGroup.getMinMembers() - currentVM);
            return;
        }

        //check interval
        long now = (new Date()).getTime();
        if (asGroup.getLastInterval() != null && (now - asGroup.getLastInterval().getTime()) < asGroup.getInterval()) {
            return;
        }

        // update last_interval
        asGroup.setLastInterval(new Date());
        _autoScaleVmGroupDao.persist(asGroup);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("[Netscaler AutoScale] Collecting RRDs data...");
        }
        Map<String, String> params = new HashMap<String, String>();
        List<AutoScaleVmGroupVmMapVO> asGroupVmVOs = _autoScaleVmGroupVmMapDao.listByGroup(asGroup.getId());
        params.put("total_vm", String.valueOf(asGroupVmVOs.size()));
        for (int i = 0; i < asGroupVmVOs.size(); i++) {
            long vmId = asGroupVmVOs.get(i).getInstanceId();
            VMInstanceVO vmVO = _vmInstanceDao.findById(vmId);
            //xe vm-list | grep vmname -B 1 | head -n 1 | awk -F':' '{print $2}'
            params.put("vmname" + String.valueOf(i + 1), vmVO.getInstanceName());
            params.put("vmid" + String.valueOf(i + 1), String.valueOf(vmVO.getId()));

        }
        // get random hostid because all vms are in a cluster
        long vmId = asGroupVmVOs.get(0).getInstanceId();
        VMInstanceVO vmVO = _vmInstanceDao.findById(vmId);
        Long receiveHost = vmVO.getHostId();

        setPerformanceMonitorCommandParams(groupTO, params);

        PerformanceMonitorCommand perfMon = new PerformanceMonitorCommand(params, 20);

        try {
            PerformanceMonitorAnswer answer = (PerformanceMonitorAnswer) _agentMgr.send(receiveHost, perfMon);
            if (answer == null || !answer.getResult()) {
                s_logger.debug("Failed to send data to node !");
            } else {
                String result = answer.getDetails();
                s_logger.debug("[AutoScale] RRDs collection answer: " + result);
                HashMap<String, Double> countersMap = new HashMap<>();
                HashMap<String, Integer> countersNumberMap = new HashMap<>();

                // extract data
                String[] counterElements = result.split(",");
                if ((counterElements != null) && (counterElements.length > 0)) {
                    for (String string : counterElements) {
                        try {
                            String[] counterVals = string.split(":");
                            String[] counter_vm = counterVals[0].split("\\.");

                            Long counterId = Long.parseLong(counter_vm[1]);
                            Long conditionId = Long.parseLong(params.get("con" + counter_vm[1]));
                            Integer duration = Integer.parseInt(params.get("duration" + counter_vm[1]));
                            Long policyId = 0L; // For NetScaler, the policyId is not returned in PerformanceMonitorAnswer

                            Double coVal = Double.parseDouble(counterVals[1]);

                            updateCountersMapWithInstantData(countersMap, countersNumberMap, groupTO, counterId, conditionId, policyId, coVal);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    AutoScalePolicy.Action scaleAction = getAutoscaleAction(countersMap, countersNumberMap, groupTO);
                    if (scaleAction != null) {
                        s_logger.debug("[AutoScale] Doing scale action: " + scaleAction + " for group " + asGroup.getId());
                        if (AutoScalePolicy.Action.ScaleUp.equals(scaleAction)) {
                            doScaleUp(asGroup.getId(), 1);
                        } else {
                            doScaleDown(asGroup.getId());
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setPerformanceMonitorCommandParams(AutoScaleVmGroupTO groupTO, Map<String, String> params) {
        // setup parameters phase: duration and counter
        // list pair [counter, duration]
        List<Pair<String, Integer>> lstPair = getPairofCounternameAndDuration(groupTO);
        int total_counter = 0;
        String[] lstCounter = new String[lstPair.size()];
        for (int i = 0; i < lstPair.size(); i++) {
            Pair<String, Integer> pair = lstPair.get(i);
            String strCounterNames = pair.first();
            Integer duration = pair.second();

            lstCounter[i] = strCounterNames.split(",")[0];
            total_counter++;
            params.put("duration" + String.valueOf(total_counter), duration.toString());
            params.put("counter" + String.valueOf(total_counter), lstCounter[i]);
            params.put("con" + String.valueOf(total_counter), strCounterNames.split(",")[1]);
        }
        params.put("total_counter", String.valueOf(total_counter));
    }

    private void updateCountersMapWithInstantData(Map<String, Double> countersMap, Map<String, Integer> countersNumberMap, AutoScaleVmGroupTO groupTO, Long counterId, Long conditionId, Long policyId, Double coVal) {
        // Summary of all counter by counterId key
        String key = generateKeyFromPolicyAndConditionAndCounter(policyId, conditionId, counterId);
        if (countersMap.get(key) == null) {
            /* initialize if data is not set */
            countersMap.put(key, Double.valueOf(0));
        }
        if (countersNumberMap.get(key) == null) {
            /* initialize if data is not set */
            countersNumberMap.put(key, 0);
        }

        CounterVO counter = _counterDao.findById(counterId);
        if (counter == null) {
            return;
        }
        if (Counter.Source.memory.equals(counter.getSource())) {
            // calculate memory in percent
            AutoScaleVmProfileTO profile = groupTO.getProfile();
            ServiceOfferingVO serviceOff = _serviceOfferingDao.findByUuidIncludingRemoved(profile.getServiceOfferingId());
            int maxRAM = serviceOff.getRamSize();

            // get current RAM percent
            coVal = coVal / maxRAM * 100;
        } else if (Counter.Source.cpu.equals(counter.getSource())) {
            // cpu
            coVal = coVal * 100;
        }

        // update data entry
        s_logger.debug(String.format("Updating countersMap for conditionId = %s, counterId = %d from %f to %f", conditionId, counterId, countersMap.get(key), countersMap.get(key) + coVal));
        countersMap.put(key, countersMap.get(key) + coVal);
        countersNumberMap.put(key, countersNumberMap.get(key) + 1);
    }

    private void updateCountersMapWithAggregatedData(Map<String, Double> countersMap, Map<String, Integer> countersNumberMap, Long counterId, Long conditionId, Long policyId, Double coVal) {
        // Summary of all counter by counterId key
        String key = generateKeyFromPolicyAndConditionAndCounter(policyId, conditionId, counterId);
        CounterVO counter = _counterDao.findById(counterId);
        if (counter == null) {
            return;
        }
        s_logger.debug(String.format("Updating countersMap for conditionId = %s, counterId = %d to %f", conditionId, counterId, coVal));
        countersMap.put(key, coVal);
        countersNumberMap.put(key, 1);
    }

    private void monitorVirtualRouterAsGroup(AutoScaleVmGroupVO asGroup) {
        // check minimum vm of group
        Integer currentVM = _autoScaleVmGroupVmMapDao.countByGroup(asGroup.getId());
        if (currentVM < asGroup.getMinMembers()) {
            doScaleUp(asGroup.getId(), asGroup.getMinMembers() - currentVM);
            return;
        }

        //check interval
        long now = (new Date()).getTime();
        if (asGroup.getLastInterval() != null && (now - asGroup.getLastInterval().getTime()) < asGroup.getInterval()) {
            return;
        }

        // update last_interval
        asGroup.setLastInterval(new Date());
        _autoScaleVmGroupDao.persist(asGroup);

        s_logger.debug("[AutoScale] Collecting performance data ...");

        AutoScaleVmGroupTO groupTO = _lbRulesMgr.toAutoScaleVmGroupTO(asGroup);

        if (is_native(groupTO)) {
            s_logger.debug("[AutoScale] Collecting performance data from hosts ...");
            getVmStatsFromHosts(groupTO);
        }

        if (has_source_virtual_router(groupTO)) {
            s_logger.debug("[AutoScale] Collecting performance data from virtual router ...");
            getNetworkStatsFromVirtualRouter(groupTO);
        }
    }

    private void checkVirtualRouterAsGroup(AutoScaleVmGroupVO asGroup) {
        AutoScaleVmGroupTO groupTO = _lbRulesMgr.toAutoScaleVmGroupTO(asGroup);

        Map<String, Double> countersMap = new HashMap<>();
        Map<String, Integer> countersNumberMap = new HashMap<>();


        // update counter maps in memory
        updateCountersMap(groupTO, countersMap, countersNumberMap);

        // get scale action
        AutoScalePolicy.Action scaleAction = getAutoscaleAction(countersMap, countersNumberMap, groupTO);
        if (scaleAction != null) {
            s_logger.debug("[AutoScale] Doing scale action: " + scaleAction + " for group " + asGroup.getId());
            if (AutoScalePolicy.Action.ScaleUp.equals(scaleAction)) {
                doScaleUp(asGroup.getId(), 1);
            } else {
                doScaleDown(asGroup.getId());
            }
        }

        // Remove old statistics from database
        cleanupAsVmGroupStatistics(groupTO);
    }

    private void getVmStatsFromHosts(AutoScaleVmGroupTO groupTO) {
        // group vms by host id
        Map<Long, List<Long>> hostAndVmIdsMap = new HashMap<>();

        List<AutoScaleVmGroupVmMapVO> asGroupVmVOs = _autoScaleVmGroupVmMapDao.listByGroup(groupTO.getId());
        for (AutoScaleVmGroupVmMapVO asGroupVmVO : asGroupVmVOs) {
            Long vmId = asGroupVmVO.getInstanceId();
            UserVmVO vm = _userVmDao.findById(vmId);
            if (vm.getHostId() != null) {
                List<Long> vmIds = hostAndVmIdsMap.get(vm.getHostId());
                if (vmIds == null) {
                    vmIds = new ArrayList<>();
                }
                vmIds.add(vmId);
                hostAndVmIdsMap.put(vm.getHostId(), vmIds);
            }
        }

        Map<Long, List<CounterTO>> countersMap = getCounters(groupTO);

        // get vm stats from each host and update database
        for (Long hostId : hostAndVmIdsMap.keySet()) {
            Date timestamp = new Date();
            HostVO host = _hostDao.findById(hostId);
            List<Long> vmIds = hostAndVmIdsMap.get(hostId);

            try {
                Map<Long, VmStatsEntry> vmStatsById = _userVmMgr.getVirtualMachineStatistics(host.getId(), host.getName(), vmIds);

                if (vmStatsById == null) {
                    s_logger.warn("Got empty result for virtual machine statistics from host: " + host);
                    continue;
                }
                Set<Long> vmIdSet = vmStatsById.keySet();

                for (Long vmId : vmIdSet) {
                    VmStatsEntry vmStats = vmStatsById.get(vmId);
                    for (Long policyId : countersMap.keySet()) {
                        List<CounterTO> counters = countersMap.get(policyId);
                        for (CounterTO counter : counters) {
                            if (Counter.Source.cpu.equals(counter.getSource())) {
                                Double counterValue = vmStats.getCPUUtilization() / 100;
                                _asGroupStatisticsDao.persist(new AutoScaleVmGroupStatisticsVO(groupTO.getId(), policyId, counter.getId(), vmId, ResourceTag.ResourceObjectType.UserVm,
                                        counterValue, AutoScaleValueType.INSTANT, timestamp));
                            } else if (Counter.Source.memory.equals(counter.getSource())) {
                                Double counterValue = vmStats.getMemoryKBs() / 1024;
                                _asGroupStatisticsDao.persist(new AutoScaleVmGroupStatisticsVO(groupTO.getId(), policyId, counter.getId(), vmId, ResourceTag.ResourceObjectType.UserVm,
                                        counterValue, AutoScaleValueType.INSTANT, timestamp));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                s_logger.debug("Failed to get VM stats from host : " + host.getName());
                continue;
            }
        }
    }

    private void getNetworkStatsFromVirtualRouter(AutoScaleVmGroupTO groupTO) {
        Network network = getNetwork(groupTO.getLoadBalancerId());
        Pair<String, Integer> publicIpAddr = getPublicIpAndPort(groupTO.getLoadBalancerId());
        List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), VirtualRouter.Role.VIRTUAL_ROUTER);
        if (CollectionUtils.isEmpty(routers)) {
            return;
        }
        List<AutoScaleMetrics> metrics = setGetAutoScaleMetricsCommandMetrics(groupTO);
        for (DomainRouterVO router : routers) {
            if (VirtualMachine.State.Running.equals(router.getState())) {
                final GetAutoScaleMetricsCommand command = new GetAutoScaleMetricsCommand(router.getPrivateIpAddress(), network.getVpcId() != null, publicIpAddr.first(), publicIpAddr.second(), metrics);
                command.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
                command.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
                command.setWait(30);
                GetAutoScaleMetricsAnswer answer = (GetAutoScaleMetricsAnswer) _agentMgr.easySend(router.getHostId(), command);
                if (answer == null || !answer.getResult()) {
                    s_logger.error("Failed to get autoscale metrics from virtual router " + router.getName());
                    continue;
                }
                processGetAutoScaleMetricsAnswer(groupTO, answer.getValues(), router.getId());
            }
        }
    }

    private List<AutoScaleMetrics> setGetAutoScaleMetricsCommandMetrics(AutoScaleVmGroupTO groupTO) {
        List<AutoScaleMetrics> metrics = new ArrayList<>();
        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            for (ConditionTO conditionTO : policyTO.getConditions()) {
                CounterTO counter = conditionTO.getCounter();
                String provider = counter.getProvider();
                if (! Network.Provider.VirtualRouter.getName().equals(provider) && ! Network.Provider.VPCVirtualRouter.getName().equals(provider)) {
                    continue;
                }
                VirtualRouterAutoScaleCounter vrCounter = VirtualRouterAutoScaleCounter.fromValue(counter.getValue());
                if (vrCounter == null) {
                    continue;
                }
                metrics.add(new AutoScaleMetrics(vrCounter, policyTO.getId(), conditionTO.getId(), counter.getId(), policyTO.getDuration()));
            }
        }
        return metrics;
    }

    private void processGetAutoScaleMetricsAnswer(AutoScaleVmGroupTO groupTO, List<AutoScaleMetricsValue> values, Long routerId) {
        Date timestamp = new Date();
        for (AutoScaleMetricsValue value : values) {
            AutoScaleMetrics metrics = value.getMetrics();
            _asGroupStatisticsDao.persist(new AutoScaleVmGroupStatisticsVO(groupTO.getId(), metrics.getPolicyId(), metrics.getCounterId(), routerId,
                    ResourceTag.ResourceObjectType.DomainRouter, value.getValue(), value.getType(), timestamp));
        }
    }

    private void  updateCountersMap(AutoScaleVmGroupTO groupTO, Map<String, Double> countersMap, Map<String, Integer> countersNumberMap) {
        s_logger.debug("Updating countersMap for as group: " + groupTO.getId());
        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            s_logger.debug(String.format("Updating countersMap for policy %d in as group %d: ", policyTO.getId(), groupTO.getId()));
            for (ConditionTO conditionTO : policyTO.getConditions()) {
                Long conditionId = conditionTO.getId();
                CounterTO counter = conditionTO.getCounter();
                Date afterDate = new Date(System.currentTimeMillis() - ((long)policyTO.getDuration() << 10));
                s_logger.debug(String.format("Updating countersMap for condition %d in policy %d in as group %d: ", conditionId, policyTO.getId(), groupTO.getId()));
                s_logger.debug(String.format("Updating countersMap with stats in %d seconds : between %s and %s", policyTO.getDuration(), afterDate, new Date()));
                List<AutoScaleVmGroupStatisticsVO> stats = _asGroupStatisticsDao.listByVmGroupAndPolicyAndCounter(groupTO.getId(), policyTO.getId(), counter.getId(), afterDate);
                if (CollectionUtils.isEmpty(stats)) {
                    continue;
                }
                s_logger.debug(String.format("Updating countersMap with %d stats", stats.size()));
                Map<String, List<AutoScaleVmGroupStatisticsVO>> aggregatedRecords = new HashMap<>();
                List<String> incorrectRecords = new ArrayList<>();
                for (AutoScaleVmGroupStatisticsVO stat : stats) {
                    if (AutoScaleValueType.INSTANT.equals(stat.getValueType())) {
                        s_logger.debug(String.format("Updating countersMap with %s (%s): %f, created on %s", counter.getSource(), counter.getValue(), stat.getRawValue(), stat.getCreated()));
                        updateCountersMapWithInstantData(countersMap, countersNumberMap, groupTO, counter.getId(), conditionId, policyTO.getId(), stat.getRawValue());
                    } else if (AutoScaleValueType.AGGREGATED.equals(stat.getValueType())) {
                        String key = stat.getCounterId() + "-" + stat.getResourceId();
                        if (incorrectRecords.contains(key)) {
                            continue;
                        }
                        if (!aggregatedRecords.containsKey(key)) {
                            List<AutoScaleVmGroupStatisticsVO> aggregatedRecordList = new ArrayList<>();
                            aggregatedRecordList.add(stat);
                            aggregatedRecords.put(key, aggregatedRecordList);
                            continue;
                        }
                        List<AutoScaleVmGroupStatisticsVO> aggregatedRecordList = aggregatedRecords.get(key);
                        AutoScaleVmGroupStatisticsVO lastRecord = aggregatedRecordList.get(aggregatedRecordList.size() - 1);
                        if (stat.getCreated().after(lastRecord.getCreated())) {
                            if (stat.getRawValue() >= lastRecord.getRawValue()) {
                                aggregatedRecordList.add(stat);
                            } else {
                                s_logger.info("The new raw value is less than the previous raw value, which means the data is incorrect. The key is " + key);
                                aggregatedRecords.remove(key);
                                incorrectRecords.add(key);
                            }
                        }
                    }
                }
                if (MapUtils.isNotEmpty(aggregatedRecords)) {
                    s_logger.debug("Processing aggregated data");
                    for (String recordKey : aggregatedRecords.keySet()) {
                        s_logger.debug("Processing aggregated data with recordKey = " + recordKey);
                        Long counterId = Long.valueOf(recordKey.split("-")[0]);
                        Long resourceId = Long.valueOf(recordKey.split("-")[1]);
                        List<AutoScaleVmGroupStatisticsVO> records = aggregatedRecords.get(recordKey);
                        if (records.size() <= 1) {
                            s_logger.info(String.format("Ignoring aggregated records, conditionId = %s, counterId = %s", conditionId, counterId));
                            continue;
                        }
                        AutoScaleVmGroupStatisticsVO firstRecord = records.get(0);
                        AutoScaleVmGroupStatisticsVO lastRecord = records.get(records.size() - 1);
                        Double coVal = (lastRecord.getRawValue() - firstRecord.getRawValue()) * 1000 / (lastRecord.getCreated().getTime() - firstRecord.getCreated().getTime());
                        updateCountersMapWithAggregatedData(countersMap, countersNumberMap, counterId, conditionId, policyTO.getId(), coVal);
                    }
                }
            }
            s_logger.debug(String.format("DONE Updating countersMap for policy %d in as group %d: ", policyTO.getId(), groupTO.getId()));
        }
        s_logger.debug("DONE Updating countersMap for as group: " + groupTO.getId());
    }

    private String generateKeyFromPolicyAndConditionAndCounter(Long policyId, Long conditionId, Long counterId) {
        return policyId + "-" + conditionId + "-" + counterId;
    }

    private void cleanupAsVmGroupStatistics(AutoScaleVmGroupTO groupTO) {
        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            Integer cleanupDelay = AutoScaleStatsCleanupDelay.value();
            Integer duration = policyTO.getDuration();
            Integer delaySecs = cleanupDelay >= duration ?  cleanupDelay : duration;
            Date beforeDate = new Date(System.currentTimeMillis() - ((long)delaySecs * 1000));
            s_logger.debug(String.format("Removing stats for policy %d in as group %d, before %s", policyTO.getId(), groupTO.getId(), beforeDate));
            _asGroupStatisticsDao.removeByGroupAndPolicy(groupTO.getId(), policyTO.getId(), beforeDate);
        }
    }

    private void scheduleMonitorTasks() {
        List<AutoScaleVmGroupVO> vmGroups = _autoScaleVmGroupDao.listAll();
        for (AutoScaleVmGroupVO vmGroup : vmGroups) {
            if (vmGroup.getState().equals(AutoScaleVmGroup.State.Enabled)) {
                scheduleMonitorTask(vmGroup.getId());
            }
        }
    }

    private void scheduleMonitorTask(Long groupId) {
        ScheduledExecutorService executor = vmGroupMonitorMaps.get(groupId);
        if (executor == null) {
            AutoScaleVmGroupVO vmGroup = _autoScaleVmGroupDao.findById(groupId);
            s_logger.debug("Scheduling monitor task for autoscale vm group " + vmGroup);
            executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("VmGroup-Monitor-" + groupId));
            executor.scheduleAtFixedRate(new MonitorTask(groupId), vmGroup.getInterval(), vmGroup.getInterval(), TimeUnit.SECONDS);
            vmGroupMonitorMaps.put(groupId, executor);
        }
    }

    private void cancelCheckTask(Long groupId) {
        ScheduledExecutorService executor = vmGroupMonitorMaps.get(groupId);
        if (executor != null) {
            s_logger.debug("Cancelling monitor task for autoscale vm group " + groupId);
            executor.shutdown();
            vmGroupMonitorMaps.remove(groupId);
        }
    }

    protected class MonitorTask extends ManagedContextRunnable {

        Long groupId;

        public MonitorTask(Long groupId) {
            this.groupId = groupId;
        }

        @Override
        protected synchronized void runInContext() {
            try {
                monitorAutoScaleVmGroup(groupId);
            } catch (final Exception e) {
                s_logger.warn("Caught the following exception on monitoring AutoScale Vm Group", e);
            }
        }
    }
}
