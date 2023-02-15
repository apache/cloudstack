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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd.HTTPMethod;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.PerformanceMonitorAnswer;
import com.cloud.agent.api.PerformanceMonitorCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.routing.GetAutoScaleMetricsAnswer;
import com.cloud.agent.api.routing.GetAutoScaleMetricsCommand;
import com.cloud.agent.api.to.LoadBalancerTO.AutoScalePolicyTO;
import com.cloud.agent.api.to.LoadBalancerTO.AutoScaleVmGroupTO;
import com.cloud.agent.api.to.LoadBalancerTO.AutoScaleVmProfileTO;
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
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
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
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
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
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
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
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.VmStats;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class AutoScaleManagerImpl extends ManagerBase implements AutoScaleManager, AutoScaleService, Configurable {
    private static final Logger s_logger = Logger.getLogger(AutoScaleManagerImpl.class);

    @Inject
    protected DispatchChainFactory dispatchChainFactory = null;
    @Inject
    EntityManager entityMgr;
    @Inject
    AccountDao accountDao;
    @Inject
    AccountManager accountMgr;
    @Inject
    ConfigurationManager configMgr;
    @Inject
    TemplateManager templateMgr;
    @Inject
    LoadBalancingRulesManager lbRulesMgr;
    @Inject
    NetworkDao networkDao;
    @Inject
    CounterDao counterDao;
    @Inject
    ConditionDao conditionDao;
    @Inject
    LoadBalancerDao lbDao;
    @Inject
    AutoScaleVmProfileDao autoScaleVmProfileDao;
    @Inject
    AutoScalePolicyDao autoScalePolicyDao;
    @Inject
    AutoScalePolicyConditionMapDao autoScalePolicyConditionMapDao;
    @Inject
    AutoScaleVmGroupDao autoScaleVmGroupDao;
    @Inject
    AutoScaleVmGroupPolicyMapDao autoScaleVmGroupPolicyMapDao;
    @Inject
    AutoScaleVmGroupVmMapDao autoScaleVmGroupVmMapDao;
    @Inject
    DataCenterDao dcDao = null;
    @Inject
    UserDao userDao;
    @Inject
    ConfigurationDao configDao;
    @Inject
    IPAddressDao ipAddressDao;
    @Inject
    AccountService accountService;
    @Inject
    UserVmService userVmService;
    @Inject
    LoadBalancerVMMapDao lbVmMapDao;
    @Inject
    LoadBalancingRulesService loadBalancingRulesService;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private AgentManager agentMgr;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    NetworkOrchestrationService networkMgr;
    @Inject
    private UserVmManager userVmMgr;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private AutoScaleVmGroupStatisticsDao asGroupStatisticsDao;
    @Inject
    private DomainRouterDao routerDao;
    @Inject
    private AnnotationDao annotationDao;
    @Inject
    protected RouterControlHelper routerControlHelper;
    @Inject
    private DiskOfferingDao diskOfferingDao;
    @Inject
    private SSHKeyPairDao sshKeyPairDao;
    @Inject
    private AffinityGroupDao affinityGroupDao;
    @Inject
    private NetworkOfferingDao networkOfferingDao;
    @Inject
    private VirtualMachineManager virtualMachineManager;

    private static final String PARAM_ROOT_DISK_SIZE = "rootdisksize";
    private static final String PARAM_DISK_OFFERING_ID = "diskofferingid";
    private static final String PARAM_DISK_SIZE = "disksize";
    private static final String PARAM_OVERRIDE_DISK_OFFERING_ID = "overridediskofferingid";
    private static final String PARAM_SECURITY_GROUP_IDS = "securitygroupids";
    private static final String PARAM_SSH_KEYPAIRS = "keypairs";
    private static final String PARAM_AFFINITY_GROUP_IDS = "affinitygroupids";
    private static final String PARAM_NETWORK_IDS = "networkids";

    private static final List<String> supportedDeployParams = Arrays.asList(PARAM_ROOT_DISK_SIZE, PARAM_DISK_OFFERING_ID, PARAM_DISK_SIZE, PARAM_SECURITY_GROUP_IDS,
            PARAM_OVERRIDE_DISK_OFFERING_ID, PARAM_SSH_KEYPAIRS, PARAM_AFFINITY_GROUP_IDS, PARAM_NETWORK_IDS);

    protected static final String VM_HOSTNAME_PREFIX = "autoScaleVm-";
    protected static final int VM_HOSTNAME_RANDOM_SUFFIX_LENGTH = 6;

    private static final Long DEFAULT_HOST_ID = -1L;

    ExecutorService groupExecutor;

    CompletionService<Pair<Long, Boolean>> completionService;

    Map<Long, ScheduledExecutorService> vmGroupMonitorMaps = new HashMap<>();

    @Override
    public boolean start() {
        // create thread pool and blocking queue
        final int workersCount = AutoScaleStatsWorker.value();
        groupExecutor = Executors.newFixedThreadPool(workersCount);
        s_logger.info("AutoScale Manager created a thread pool to check autoscale vm groups. The pool size is : " + workersCount);

        final BlockingQueue<Future<Pair<Long, Boolean>>> queue = new LinkedBlockingQueue<>(workersCount);
        s_logger.info("AutoScale Manager created a blocking queue to check autoscale vm groups. The queue size is : " + workersCount);

        completionService = new ExecutorCompletionService<>(groupExecutor, queue);

        scheduleMonitorTasks();

        return true;
    }

    @Override
    public boolean stop() {
        if (groupExecutor != null) {
            groupExecutor.shutdown();
        }
        return true;
    }

    public List<AutoScaleCounter> getSupportedAutoScaleCounters(long networkid) {
        String capability = lbRulesMgr.getLBCapability(networkid, Capability.AutoScaleCounters.getName());
        if (capability == null) {
            return null;
        }
        Gson gson = new Gson();
        java.lang.reflect.Type listType = new TypeToken<List<AutoScaleCounter>>() {
        }.getType();
        List<AutoScaleCounter> result = gson.fromJson(capability, listType);
        return result;
    }

    public void validateNetworkCapability(long networkId) {
        Network network = networkDao.findById(networkId);
        if (network == null) {
            throw new CloudRuntimeException(String.format("Unable to find network with id: %s ", networkId));
        }
        NetworkOffering offering = networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
        if (!offering.isSupportsVmAutoScaling()) {
            throw new InvalidParameterValueException("Vm AutoScaling is not supported by this network.");
        }
    }

    public void validateAutoScaleCounters(long networkid, List<Counter> counters, List<Pair<String, String>> counterParamPassed) {
        List<AutoScaleCounter> supportedCounters = getSupportedAutoScaleCounters(networkid);
        if (supportedCounters == null) {
            throw new InvalidParameterValueException("AutoScale is not supported in the network");
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
                                throw new InvalidParameterValueException("Parameter " + autoScaleCounterParam.getParamName() + " has to be set in AutoScaleVmProfile's " +
                                    ApiConstants.COUNTERPARAM_LIST);
                            }
                        }
                    }
                    break;
                }
            }
            if (!isCounterSupported) {
                throw new InvalidParameterValueException("AutoScale counter with source='" + counter.getSource().name() + "' is not supported " + "in the network");
            }
        }
    }

    private <VO extends ControlledEntity> VO getEntityInDatabase(Account caller, String paramName, Long id, GenericDao<VO, Long> dao) {

        VO vo = dao.findById(id);

        if (vo == null) {
            throw new InvalidParameterValueException("Unable to find " + paramName);
        }

        accountMgr.checkAccess(caller, null, false, (ControlledEntity)vo);

        return vo;
    }

    private boolean isAutoScaleScaleUpPolicy(AutoScalePolicy policyVO) {
        return policyVO.getAction().equals(AutoScalePolicy.Action.SCALEUP);
    }

    private List<AutoScalePolicyVO> getAutoScalePolicies(List<Long> policyIds, List<Counter> counters, int interval, boolean scaleUpPolicies) {
        SearchBuilder<AutoScalePolicyVO> policySearch = autoScalePolicyDao.createSearchBuilder();
        policySearch.and("ids", policySearch.entity().getId(), Op.IN);
        policySearch.done();
        SearchCriteria<AutoScalePolicyVO> sc = policySearch.create();

        sc.setParameters("ids", policyIds.toArray(new Object[0]));
        List<AutoScalePolicyVO> policies = autoScalePolicyDao.search(sc, null);

        for (AutoScalePolicyVO policy : policies) {
            int duration = policy.getDuration();
            if (duration < interval) {
                throw new InvalidParameterValueException("duration : " + duration + " specified in a policy cannot be less than vm group's interval : " + interval);
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
            List<AutoScalePolicyConditionMapVO> policyConditionMapVOs = autoScalePolicyConditionMapDao.listByAll(policy.getId(), null);
            for (AutoScalePolicyConditionMapVO policyConditionMapVO : policyConditionMapVOs) {
                long conditionid = policyConditionMapVO.getConditionId();
                Condition condition = conditionDao.findById(conditionid);
                Counter counter = counterDao.findById(condition.getCounterId());
                counters.add(counter);
            }
        }
        return policies;
    }

    @DB
    protected AutoScaleVmProfileVO checkValidityAndPersist(AutoScaleVmProfileVO vmProfile, boolean isNew) {
        long templateId = vmProfile.getTemplateId();
        Long autoscaleUserId = vmProfile.getAutoScaleUserId();
        int expungeVmGracePeriod = vmProfile.getExpungeVmGracePeriod();

        Long serviceOfferingId = vmProfile.getServiceOfferingId();
        if (serviceOfferingId != null) {
            ServiceOffering serviceOffering = entityMgr.findByIdIncludingRemoved(ServiceOffering.class, serviceOfferingId);
            if (serviceOffering == null) {
                throw new InvalidParameterValueException("Unable to find service offering by id");
            }
            if (serviceOffering.isDynamic()) {
                throw new InvalidParameterValueException("Unable to use dynamic service offering in AutoScale vm profile");
            }
        }

        VirtualMachineTemplate template = entityMgr.findById(VirtualMachineTemplate.class, templateId);
        // Make sure a valid template ID was specified
        if (template == null) {
            throw new InvalidParameterValueException("Unable to use the given template.");
        }

        if (expungeVmGracePeriod < 0) {
            throw new InvalidParameterValueException("Expunge Vm Grace Period cannot be less than 0.");
        }

        if (!isNew) {
            List<AutoScaleVmGroupVO> vmGroups = autoScaleVmGroupDao.listByProfile(vmProfile.getId());
            for (AutoScaleVmGroupVO vmGroup : vmGroups) {
                Network.Provider provider = getLoadBalancerServiceProvider(vmGroup.getLoadBalancerId());
                if (Network.Provider.Netscaler.equals(provider)) {
                    checkAutoScaleUser(autoscaleUserId, vmProfile.getAccountId());
                }
            }
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
        vmProfile = autoScaleVmProfileDao.persist(vmProfile);

        return vmProfile;
    }

    @Override
    public void checkAutoScaleUser(Long autoscaleUserId, long accountId) {
        if (autoscaleUserId == null) {
            throw new InvalidParameterValueException("autoscaleuserid is required but not passed");
        }
        User user = userDao.findById(autoscaleUserId);
        if (user == null) {
            throw new InvalidParameterValueException("Unable to find autoscale user id: " + autoscaleUserId);
        }

        if (user.getAccountId() != accountId) {
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
    }
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMPROFILE_CREATE, eventDescription = "creating autoscale vm profile", create = true)
    public AutoScaleVmProfile createAutoScaleVmProfile(CreateAutoScaleVmProfileCmd cmd) {

        Account caller = CallContext.current().getCallingAccount();
        Account owner = accountMgr.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        accountMgr.checkAccess(caller, null, true, owner);

        long zoneId = cmd.getZoneId();
        long serviceOfferingId = cmd.getServiceOfferingId();
        Long autoscaleUserId = cmd.getAutoscaleUserId();
        String userData = cmd.getUserData();

        DataCenter zone = entityMgr.findById(DataCenter.class, zoneId);

        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id");
        }

        ServiceOffering serviceOffering = entityMgr.findById(ServiceOffering.class, serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException("Unable to find service offering by id");
        }

        // validations
        HashMap<String, String> deployParams = cmd.getDeployParamMap();
        /*
         * Just for making sure the values are right in other deploy params.
         * For ex. if projectId is given as a string instead of an long value, this
         * will be throwing an error.
         */
        dispatchChainFactory.getStandardDispatchChain().dispatch(new DispatchTask(ComponentContext.inject(DeployVMCmd.class), deployParams));

        AutoScaleVmProfileVO profileVO =
            new AutoScaleVmProfileVO(cmd.getZoneId(), owner.getDomainId(), owner.getAccountId(), cmd.getServiceOfferingId(), cmd.getTemplateId(), cmd.getOtherDeployParams(),
                cmd.getCounterParamList(), cmd.getUserData(), cmd.getExpungeVmGracePeriod(), autoscaleUserId);

        if (cmd.getDisplay() != null) {
            profileVO.setDisplay(cmd.getDisplay());
        }

        if (userData != null) {
            profileVO.setUserData(userData);
        }

        profileVO = checkValidityAndPersist(profileVO, true);
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
        Map<String, HashMap<String, String>> otherDeployParams = cmd.getOtherDeployParams();
        Map counterParamList = cmd.getCounterParamList();
        String userData = cmd.getUserData();

        Integer expungeVmGracePeriod = cmd.getExpungeVmGracePeriod();

        AutoScaleVmProfileVO vmProfile = getEntityInDatabase(CallContext.current().getCallingAccount(), "Auto Scale Vm Profile", profileId, autoScaleVmProfileDao);

        boolean physicalParameterUpdate = (templateId != null || autoscaleUserId != null || counterParamList != null || otherDeployParams != null || expungeVmGracePeriod != null || userData != null);

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

        if (userData != null) {
            vmProfile.setUserData(userData);
        }

        if (expungeVmGracePeriod != null) {
            vmProfile.setExpungeVmGracePeriod(expungeVmGracePeriod);
        }

        if (cmd.getCustomId() != null) {
            vmProfile.setUuid(cmd.getCustomId());
        }

        if (cmd.getDisplay() != null) {
            vmProfile.setDisplay(cmd.getDisplay());
        }

        List<AutoScaleVmGroupVO> vmGroupList = autoScaleVmGroupDao.listByAll(null, profileId);
        for (AutoScaleVmGroupVO vmGroupVO : vmGroupList) {
            if (physicalParameterUpdate && !vmGroupVO.getState().equals(AutoScaleVmGroup.State.DISABLED)) {
                throw new InvalidParameterValueException("The AutoScale Vm Profile can be updated only if the Vm Group it is associated with is disabled in state");
            }
        }

        vmProfile = checkValidityAndPersist(vmProfile, false);
        s_logger.info("Updated Auto Scale Vm Profile id:" + vmProfile.getId());

        return vmProfile;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMPROFILE_DELETE, eventDescription = "deleting autoscale vm profile")
    public boolean deleteAutoScaleVmProfile(long id) {
        /* Check if entity is in database */
        getEntityInDatabase(CallContext.current().getCallingAccount(), "AutoScale Vm Profile", id, autoScaleVmProfileDao);
        if (autoScaleVmGroupDao.isProfileInUse(id)) {
            throw new InvalidParameterValueException("Cannot delete AutoScale Vm Profile when it is in use by one more vm groups");
        }

        boolean success = autoScaleVmProfileDao.remove(id);
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

        SearchWrapper<AutoScaleVmProfileVO> searchWrapper = new SearchWrapper<>(autoScaleVmProfileDao, AutoScaleVmProfileVO.class, cmd, cmd.getId());
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
            throw new InvalidParameterValueException("quietTime is an invalid value: " + quietTime);
        }

        return Transaction.execute(new TransactionCallback<AutoScalePolicyVO>() {
            @Override
            public AutoScalePolicyVO doInTransaction(TransactionStatus status) {
                AutoScalePolicyVO autoScalePolicyVO = autoScalePolicyDao.persist(autoScalePolicyVOFinal);

                if (conditionIds != null) {
                    if (CollectionUtils.isEmpty(conditionIds)) {
                        throw new InvalidParameterValueException("AutoScale policy must have at least one condition");
                    }
                    SearchBuilder<ConditionVO> conditionsSearch = conditionDao.createSearchBuilder();
                    conditionsSearch.and("ids", conditionsSearch.entity().getId(), Op.IN);
                    conditionsSearch.done();
                    SearchCriteria<ConditionVO> sc = conditionsSearch.create();

                    sc.setParameters("ids", conditionIds.toArray(new Object[0]));
                    List<ConditionVO> conditions = conditionDao.search(sc, null);

                    ControlledEntity[] sameOwnerEntities = conditions.toArray(new ControlledEntity[conditions.size() + 1]);
                    sameOwnerEntities[sameOwnerEntities.length - 1] = autoScalePolicyVO;
                    accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, sameOwnerEntities);

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
                    autoScalePolicyConditionMapDao.removeByAutoScalePolicyId(autoScalePolicyVO.getId());

                    for (Long conditionId : conditionIds) {
                        AutoScalePolicyConditionMapVO policyConditionMapVO = new AutoScalePolicyConditionMapVO(autoScalePolicyVO.getId(), conditionId);
                        autoScalePolicyConditionMapDao.persist(policyConditionMapVO);
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

        AutoScalePolicy.Action scaleAction = null;
        try {
            scaleAction = AutoScalePolicy.Action.fromValue(action);
        } catch (IllegalArgumentException ex) {
            throw new InvalidParameterValueException("action is invalid. Supported actions are: " + Arrays.toString(AutoScalePolicy.Action.values()));
        }

        AutoScalePolicyVO policyVO = new AutoScalePolicyVO(cmd.getName(), cmd.getDomainId(), cmd.getAccountId(), duration, quietTime, null, scaleAction);

        policyVO = checkValidityAndPersist(policyVO, cmd.getConditionIds());
        s_logger.info("Successfully created AutoScale Policy with Id: " + policyVO.getId());
        return policyVO;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEPOLICY_DELETE, eventDescription = "deleting autoscale policy")
    public boolean deleteAutoScalePolicy(final long id) {
        /* Check if entity is in database */
        getEntityInDatabase(CallContext.current().getCallingAccount(), "AutoScale Policy", id, autoScalePolicyDao);

        if (autoScaleVmGroupPolicyMapDao.isAutoScalePolicyInUse(id)) {
            throw new InvalidParameterValueException("Cannot delete AutoScale Policy when it is in use by one or more AutoScale Vm Groups");
        }

        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                boolean success = true;
                success = autoScalePolicyDao.remove(id);
                if (!success) {
                    s_logger.warn("Failed to remove AutoScale Policy db object");
                    return false;
                }
                success = autoScalePolicyConditionMapDao.removeByAutoScalePolicyId(id);
                if (!success) {
                    s_logger.warn("Failed to remove AutoScale Policy Condition mappings");
                    return false;
                }
                s_logger.info("Successfully deleted autoscale policy id : " + id);

                return success;
            }
        });
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

        public SearchWrapper(GenericDao<VO, Long> dao, Class<VO> entityClass, BaseListProjectAndAccountResourcesCmd cmd, Long id)
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
            accountMgr.buildACLSearchParameters(caller, id, accountName, cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject,
                    listAll, false);
            domainId = domainIdRecursiveListProject.first();
            isRecursive = domainIdRecursiveListProject.second();
            listProjectResourcesCriteria = domainIdRecursiveListProject.third();

            accountMgr.buildACLSearchBuilder(searchBuilder, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
            searchFilter = new Filter(entityClass, "id", false, startIndex, pageSizeVal);
        }

        public SearchBuilder<VO> getSearchBuilder() {
            return searchBuilder;
        }

        public SearchCriteria<VO> buildSearchCriteria() {
            searchCriteria = searchBuilder.create();
            accountMgr.buildACLSearchCriteria(searchCriteria, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
            return searchCriteria;
        }

        public List<VO> search() {
            return dao.search(searchCriteria, searchFilter);
        }
    }

    @Override
    public List<? extends AutoScalePolicy> listAutoScalePolicies(ListAutoScalePoliciesCmd cmd) {
        SearchWrapper<AutoScalePolicyVO> searchWrapper = new SearchWrapper<>(autoScalePolicyDao, AutoScalePolicyVO.class, cmd, cmd.getId());
        SearchBuilder<AutoScalePolicyVO> sb = searchWrapper.getSearchBuilder();
        Long id = cmd.getId();
        String name = cmd.getName();
        Long conditionId = cmd.getConditionId();
        String action = cmd.getAction();
        Long vmGroupId = cmd.getVmGroupId();

        AutoScalePolicy.Action scaleAction = null;
        if (action != null) {
            try {
                scaleAction = AutoScalePolicy.Action.fromValue(action);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("action is invalid. Supported actions are: " + Arrays.toString(AutoScalePolicy.Action.values()));
            }
        }

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("action", sb.entity().getAction(), SearchCriteria.Op.EQ);

        if (conditionId != null) {
            SearchBuilder<AutoScalePolicyConditionMapVO> asPolicyConditionSearch = autoScalePolicyConditionMapDao.createSearchBuilder();
            asPolicyConditionSearch.and("conditionId", asPolicyConditionSearch.entity().getConditionId(), SearchCriteria.Op.EQ);
            sb.join("asPolicyConditionSearch", asPolicyConditionSearch, sb.entity().getId(), asPolicyConditionSearch.entity().getPolicyId(), JoinBuilder.JoinType.INNER);
        }

        if (vmGroupId != null) {
            SearchBuilder<AutoScaleVmGroupPolicyMapVO> asVmGroupPolicySearch = autoScaleVmGroupPolicyMapDao.createSearchBuilder();
            asVmGroupPolicySearch.and("vmGroupId", asVmGroupPolicySearch.entity().getVmGroupId(), SearchCriteria.Op.EQ);
            sb.join("asVmGroupPolicySearch", asVmGroupPolicySearch, sb.entity().getId(), asVmGroupPolicySearch.entity().getPolicyId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<AutoScalePolicyVO> sc = searchWrapper.buildSearchCriteria();

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (scaleAction != null) {
            sc.setParameters("action", scaleAction);
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
        String name = cmd.getName();
        Integer duration = cmd.getDuration();
        Integer quietTime = cmd.getQuietTime();
        List<Long> conditionIds = cmd.getConditionIds();
        AutoScalePolicyVO policy = getEntityInDatabase(CallContext.current().getCallingAccount(), "Auto Scale Policy", policyId, autoScalePolicyDao);

        if (name != null) {
            policy.setName(name);
        }

        if (duration != null) {
            policy.setDuration(duration);
        }

        if (quietTime != null) {
            policy.setQuietTime(quietTime);
        }

        List<AutoScaleVmGroupPolicyMapVO> vmGroupPolicyList = autoScaleVmGroupPolicyMapDao.listByPolicyId(policyId);
        for (AutoScaleVmGroupPolicyMapVO vmGroupPolicy : vmGroupPolicyList) {
            AutoScaleVmGroupVO vmGroupVO = autoScaleVmGroupDao.findById(vmGroupPolicy.getVmGroupId());
            if (vmGroupVO == null) {
                s_logger.warn("Stale database entry! There is an entry in VmGroupPolicyMap but the vmGroup is missing:" + vmGroupPolicy.getVmGroupId());

                continue;

            }
            if (!vmGroupVO.getState().equals(AutoScaleVmGroup.State.DISABLED)) {
                throw new InvalidParameterValueException("The AutoScale Policy can be updated only if the Vm Group it is associated with is disabled in state");
            }
            if (policy.getDuration() < vmGroupVO.getInterval()) {
                throw new InvalidParameterValueException("duration is less than the associated AutoScaleVmGroup's interval");
            }
        }

        policy = checkValidityAndPersist(policy, conditionIds);
        s_logger.info("Successfully updated Auto Scale Policy id:" + policyId);

        if (CollectionUtils.isNotEmpty(conditionIds)) {
            markStatisticsAsInactive(null, policyId);
        }

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

        LoadBalancerVO loadBalancer = getEntityInDatabase(CallContext.current().getCallingAccount(), ApiConstants.LBID, cmd.getLbRuleId(), lbDao);

        Long zoneId = ipAddressDao.findById(loadBalancer.getSourceIpAddressId()).getDataCenterId();

        if (autoScaleVmGroupDao.isAutoScaleLoadBalancer(loadBalancer.getId())) {
            throw new InvalidParameterValueException("an AutoScaleVmGroup is already attached to the lb rule, the existing vm group has to be first deleted");
        }

        if (lbVmMapDao.isVmAttachedToLoadBalancer(loadBalancer.getId())) {
            throw new InvalidParameterValueException(
                "there are Vms already bound to the specified LoadBalancing Rule. User bound Vms and AutoScaled Vm Group cannot co-exist on a Load Balancing Rule");
        }

        AutoScaleVmGroupVO vmGroupVO = new AutoScaleVmGroupVO(cmd.getLbRuleId(), zoneId, loadBalancer.getDomainId(), loadBalancer.getAccountId(), name, minMembers, maxMembers,
            loadBalancer.getDefaultPortStart(), interval, null, cmd.getProfileId(), AutoScaleVmGroup.State.NEW);

        if (forDisplay != null) {
            vmGroupVO.setDisplay(forDisplay);
        }

        vmGroupVO = checkValidityAndPersist(vmGroupVO, cmd.getScaleUpPolicyIds(), cmd.getScaleDownPolicyIds());
        s_logger.info("Successfully created Autoscale Vm Group with Id: " + vmGroupVO.getId());

        createInactiveDummyRecord(vmGroupVO.getId());
        scheduleMonitorTask(vmGroupVO.getId());

        return vmGroupVO;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMGROUP_CREATE, eventDescription = "creating autoscale vm group", async = true)
    public boolean configureAutoScaleVmGroup(CreateAutoScaleVmGroupCmd cmd) throws ResourceUnavailableException {
        return configureAutoScaleVmGroup(cmd.getEntityId(), AutoScaleVmGroup.State.NEW);
    }

    public boolean isLoadBalancerBasedAutoScaleVmGroup(AutoScaleVmGroup vmGroup) {
        return vmGroup.getLoadBalancerId() != null;
    }

    protected boolean configureAutoScaleVmGroup(long vmGroupid, AutoScaleVmGroup.State currentState) throws ResourceUnavailableException {
        AutoScaleVmGroup vmGroup = autoScaleVmGroupDao.findById(vmGroupid);

        if (isLoadBalancerBasedAutoScaleVmGroup(vmGroup)) {
            try {
                return lbRulesMgr.configureLbAutoScaleVmGroup(vmGroupid, currentState);
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
    public boolean deleteAutoScaleVmGroup(final long id, final Boolean cleanup) {
        AutoScaleVmGroupVO autoScaleVmGroupVO = getEntityInDatabase(CallContext.current().getCallingAccount(), "AutoScale Vm Group", id, autoScaleVmGroupDao);

        if (autoScaleVmGroupVO.getState().equals(AutoScaleVmGroup.State.NEW)) {
            /* This condition is for handling failures during creation command */
            return autoScaleVmGroupDao.remove(id);
        }

        if (!autoScaleVmGroupVO.getState().equals(AutoScaleVmGroup.State.DISABLED) && !Boolean.TRUE.equals(cleanup)) {
            throw new InvalidParameterValueException(String.format("Cannot delete autoscale vm group id : %d because it is in %s state. Please disable it or pass cleanup=true flag which will destroy all VMs.", id, autoScaleVmGroupVO.getState()));
        }

        Integer currentVM = autoScaleVmGroupVmMapDao.countByGroup(id);
        if (currentVM > 0 && !Boolean.TRUE.equals(cleanup)) {
            throw new InvalidParameterValueException(String.format("Cannot delete autoscale vm group id : %d because there are %d VMs. Please remove the VMs or pass cleanup=true flag which will destroy all VMs.", id, currentVM));
        }

        AutoScaleVmGroup.State bakupState = autoScaleVmGroupVO.getState();
        autoScaleVmGroupVO.setState(AutoScaleVmGroup.State.REVOKE);
        autoScaleVmGroupDao.persist(autoScaleVmGroupVO);
        boolean success = false;

        if (Boolean.TRUE.equals(cleanup)) {
            List<AutoScaleVmGroupVmMapVO> asGroupVmVOs = autoScaleVmGroupVmMapDao.listByGroup(id);
            for (AutoScaleVmGroupVmMapVO asGroupVmVO : asGroupVmVOs) {
                Long vmId = asGroupVmVO.getInstanceId();
                destroyVm(vmId);
            }
        }

        try {
            success = configureAutoScaleVmGroup(id, bakupState);
        } catch (ResourceUnavailableException e) {
            autoScaleVmGroupVO.setState(bakupState);
            autoScaleVmGroupDao.persist(autoScaleVmGroupVO);
        } finally {
            if (!success) {
                s_logger.warn("Could not delete AutoScale Vm Group id : " + id);
                return false;
            }
        }

        // Remove comments (if any)
        annotationDao.removeByEntityType(AnnotationService.EntityType.AUTOSCALE_VM_GROUP.name(), autoScaleVmGroupVO.getUuid());

        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                boolean success = autoScaleVmGroupDao.remove(id);

                if (!success) {
                    s_logger.warn("Failed to remove AutoScale Group db object");
                    return false;
                }

                cancelMonitorTask(id);

                success = autoScaleVmGroupPolicyMapDao.removeByGroupId(id);
                if (!success) {
                    s_logger.warn("Failed to remove AutoScale Group Policy mappings");
                    return false;
                }

                success = autoScaleVmGroupVmMapDao.removeByGroup(id);
                if (!success) {
                    s_logger.warn("Failed to remove AutoScale Group VM mappings");
                    return false;
                }

                success = asGroupStatisticsDao.removeByGroupId(id);
                if (!success) {
                    s_logger.warn("Failed to remove AutoScale Group statistics");
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
        String name = cmd.getName();
        Long policyId = cmd.getPolicyId();
        Long loadBalancerId = cmd.getLoadBalancerId();
        Long profileId = cmd.getProfileId();
        Long zoneId = cmd.getZoneId();
        Boolean forDisplay = cmd.getDisplay();

        SearchWrapper<AutoScaleVmGroupVO> searchWrapper = new SearchWrapper<>(autoScaleVmGroupDao, AutoScaleVmGroupVO.class, cmd, cmd.getId());
        SearchBuilder<AutoScaleVmGroupVO> sb = searchWrapper.getSearchBuilder();

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("loadBalancerId", sb.entity().getLoadBalancerId(), SearchCriteria.Op.EQ);
        sb.and("profileId", sb.entity().getProfileId(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("display", sb.entity().isDisplay(), SearchCriteria.Op.EQ);

        if (policyId != null) {
            SearchBuilder<AutoScaleVmGroupPolicyMapVO> asVmGroupPolicySearch = autoScaleVmGroupPolicyMapDao.createSearchBuilder();
            asVmGroupPolicySearch.and("policyId", asVmGroupPolicySearch.entity().getPolicyId(), SearchCriteria.Op.EQ);
            sb.join("asVmGroupPolicySearch", asVmGroupPolicySearch, sb.entity().getId(), asVmGroupPolicySearch.entity().getVmGroupId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<AutoScaleVmGroupVO> sc = searchWrapper.buildSearchCriteria();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (name != null) {
            sc.setParameters("name", name);
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

        checkAutoScaleVmGroupName(vmGroup.getName());

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

        if (minMembers <= 0) {
            throw new InvalidParameterValueException(ApiConstants.MIN_MEMBERS + " is an invalid value: " + minMembers);
        }

        if (maxMembers <= 0) {
            throw new InvalidParameterValueException(ApiConstants.MAX_MEMBERS + " is an invalid value: " + maxMembers);
        }

        if (minMembers > maxMembers) {
            throw new InvalidParameterValueException(ApiConstants.MIN_MEMBERS + " (" + minMembers + ")cannot be greater than " + ApiConstants.MAX_MEMBERS + " (" +
                maxMembers + ")");
        }

        if (interval <= 0) {
            throw new InvalidParameterValueException("interval is an invalid value: " + interval);
        }

        if (passedScaleUpPolicyIds != null) {
            if (CollectionUtils.isEmpty(passedScaleUpPolicyIds)) {
                throw new InvalidParameterValueException("AutoScale VM group must have at least one ScaleUp policy");
            }
            policies.addAll(getAutoScalePolicies(passedScaleUpPolicyIds, counters, interval, true));
            policyIds.addAll(passedScaleUpPolicyIds);
        } else {
            // Run the interval check for existing policies
            getAutoScalePolicies(currentScaleUpPolicyIds, counters, interval, true);
            policyIds.addAll(currentScaleUpPolicyIds);
        }

        if (passedScaleDownPolicyIds != null) {
            if (CollectionUtils.isEmpty(passedScaleDownPolicyIds)) {
                throw new InvalidParameterValueException("AutoScale VM group must have at least one ScaleDown policy");
            }
            policies.addAll(getAutoScalePolicies(passedScaleDownPolicyIds, counters, interval, false));
            policyIds.addAll(passedScaleDownPolicyIds);
        } else {
            // Run the interval check for existing policies
            getAutoScalePolicies(currentScaleDownPolicyIds, counters, interval, false);
            policyIds.addAll(currentScaleDownPolicyIds);
        }
        AutoScaleVmProfileVO profileVO =
            getEntityInDatabase(CallContext.current().getCallingAccount(), ApiConstants.VMPROFILE_ID, vmGroup.getProfileId(), autoScaleVmProfileDao);

        LoadBalancerVO loadBalancer = getEntityInDatabase(CallContext.current().getCallingAccount(), ApiConstants.LBID, vmGroup.getLoadBalancerId(), lbDao);
        validateNetworkCapability(loadBalancer.getNetworkId());
        validateAutoScaleCounters(loadBalancer.getNetworkId(), counters, profileVO.getCounterParams());

        Network.Provider provider = getLoadBalancerServiceProvider(vmGroup.getLoadBalancerId());
        if (Network.Provider.Netscaler.equals(provider)) {
            checkAutoScaleUser(profileVO.getAutoScaleUserId(), vmGroup.getAccountId());
        }

        ControlledEntity[] sameOwnerEntities = policies.toArray(new ControlledEntity[policies.size() + 2]);
        sameOwnerEntities[sameOwnerEntities.length - 2] = loadBalancer;
        sameOwnerEntities[sameOwnerEntities.length - 1] = profileVO;
        accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, sameOwnerEntities);

        return Transaction.execute(new TransactionCallback<AutoScaleVmGroupVO>() {
            @Override
            public AutoScaleVmGroupVO doInTransaction(TransactionStatus status) {
                AutoScaleVmGroupVO vmGroupNew = autoScaleVmGroupDao.persist(vmGroup);

                if (passedScaleUpPolicyIds != null || passedScaleDownPolicyIds != null) {
                    autoScaleVmGroupPolicyMapDao.removeByGroupId(vmGroupNew.getId());

                    for (Long policyId : policyIds) {
                        autoScaleVmGroupPolicyMapDao.persist(new AutoScaleVmGroupPolicyMapVO(vmGroupNew.getId(), policyId));
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

        AutoScaleVmGroupVO vmGroupVO = getEntityInDatabase(CallContext.current().getCallingAccount(), "AutoScale Vm Group", vmGroupId, autoScaleVmGroupDao);
        int currentInterval = vmGroupVO.getInterval();

        boolean physicalParametersUpdate = (minMembers != null || maxMembers != null || (interval != null && interval != currentInterval) || CollectionUtils.isNotEmpty(scaleUpPolicyIds) || CollectionUtils.isNotEmpty(scaleDownPolicyIds));

        if (physicalParametersUpdate && !vmGroupVO.getState().equals(AutoScaleVmGroup.State.DISABLED)) {
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

            if ((interval != null && interval != currentInterval) || CollectionUtils.isNotEmpty(scaleUpPolicyIds) || CollectionUtils.isNotEmpty(scaleDownPolicyIds)) {
                markStatisticsAsInactive(vmGroupId, null);
            }

            return vmGroupVO;
        } else
            return null;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMGROUP_ENABLE, eventDescription = "enabling autoscale vm group", async = true)
    public AutoScaleVmGroup enableAutoScaleVmGroup(Long id) {
        AutoScaleVmGroupVO vmGroup = getEntityInDatabase(CallContext.current().getCallingAccount(), "AutoScale Vm Group", id, autoScaleVmGroupDao);
        boolean success = false;

        if (vmGroup.getState().equals(AutoScaleVmGroup.State.ENABLED)) {
            return vmGroup;
        }

        if (!vmGroup.getState().equals(AutoScaleVmGroup.State.DISABLED)) {
            throw new InvalidParameterValueException("Only a AutoScale Vm Group which is in Disabled state can be enabled.");
        }

        try {
            vmGroup.setState(AutoScaleVmGroup.State.ENABLED);
            vmGroup = autoScaleVmGroupDao.persist(vmGroup);
            success = configureAutoScaleVmGroup(id, AutoScaleVmGroup.State.DISABLED);
            scheduleMonitorTask(vmGroup.getId());
        } catch (ResourceUnavailableException e) {
            vmGroup.setState(AutoScaleVmGroup.State.DISABLED);
            autoScaleVmGroupDao.persist(vmGroup);
        } finally {
            if (!success) {
                s_logger.warn("Failed to enable AutoScale Vm Group id : " + id);
                return null;
            }
            s_logger.info("Successfully enabled AutoScale Vm Group with Id:" + id);
            createInactiveDummyRecord(vmGroup.getId());
        }
        return vmGroup;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_AUTOSCALEVMGROUP_DISABLE, eventDescription = "disabling autoscale vm group", async = true)
    @DB
    public AutoScaleVmGroup disableAutoScaleVmGroup(Long id) {
        AutoScaleVmGroupVO vmGroup = getEntityInDatabase(CallContext.current().getCallingAccount(), "AutoScale Vm Group", id, autoScaleVmGroupDao);
        boolean success = false;

        if (vmGroup.getState().equals(AutoScaleVmGroup.State.DISABLED)) {
            return vmGroup;
        }

        if (!vmGroup.getState().equals(AutoScaleVmGroup.State.ENABLED) && !vmGroup.getState().equals(AutoScaleVmGroup.State.SCALING)) {
            throw new InvalidParameterValueException("Only a AutoScale Vm Group which is in Enabled or Scaling state can be disabled.");
        }

        try {
            vmGroup.setState(AutoScaleVmGroup.State.DISABLED);
            vmGroup = autoScaleVmGroupDao.persist(vmGroup);
            success = configureAutoScaleVmGroup(id, AutoScaleVmGroup.State.ENABLED);
            createInactiveDummyRecord(vmGroup.getId());
            cancelMonitorTask(vmGroup.getId());
        } catch (ResourceUnavailableException e) {
            vmGroup.setState(AutoScaleVmGroup.State.ENABLED);
            autoScaleVmGroupDao.persist(vmGroup);
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
        String source = cmd.getSource().toUpperCase();
        String name = cmd.getName();
        Counter.Source src;
        // Validate Source
        try {
            src = Counter.Source.valueOf(source);
        } catch (Exception ex) {
            throw new InvalidParameterValueException("The Source " + source + " does not exist; Unable to create Counter");
        }

        // Validate Provider
        Network.Provider provider = Network.Provider.getProvider(cmd.getProvider());
        if (provider == null) {
            throw new InvalidParameterValueException("The Provider " + cmd.getProvider() + " does not exist; Unable to create Counter");
        }

        CounterVO counter = null;

        s_logger.debug("Adding Counter " + name);
        counter = counterDao.persist(new CounterVO(src, name, cmd.getValue(), provider));

        CallContext.current().setEventDetails(" Id: " + counter.getId() + " Name: " + name);
        return counter;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CONDITION_CREATE, eventDescription = "Condition", create = true)
    public Condition createCondition(CreateConditionCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Account owner = accountMgr.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        accountMgr.checkAccess(caller, null, true, owner);

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
        if (threshold < 0) {
            throw new InvalidParameterValueException("The threshold " + threshold + " must be equal to or greater than 0.");
        }

        CounterVO counter = counterDao.findById(cid);

        if (counter == null) {
            throw new InvalidParameterValueException("Unable to find counter");
        }
        ConditionVO condition = null;

        condition = conditionDao.persist(new ConditionVO(cid, threshold, owner.getAccountId(), owner.getDomainId(), op));
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
            source = source.toUpperCase();
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

        return counterDao.listCounters(id, name, source, providerStr, cmd.getKeyword(), searchFilter);
    }

    @Override
    public List<? extends Condition> listConditions(ListConditionsCmd cmd) {
        Long id = cmd.getId();
        Long counterId = cmd.getCounterId();
        Long policyId = cmd.getPolicyId();
        SearchWrapper<ConditionVO> searchWrapper = new SearchWrapper<>(conditionDao, ConditionVO.class, cmd, cmd.getId());
        SearchBuilder<ConditionVO> sb = searchWrapper.getSearchBuilder();
        if (policyId != null) {
            SearchBuilder<AutoScalePolicyConditionMapVO> asPolicyConditionSearch = autoScalePolicyConditionMapDao.createSearchBuilder();
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
        CounterVO counter = counterDao.findById(counterId);
        if (counter == null) {
            throw new InvalidParameterValueException("Unable to find Counter");
        }

        // Verify if it is used in any Condition

        ConditionVO condition = conditionDao.findByCounterId(counterId);
        if (condition != null) {
            s_logger.info("Cannot delete counter " + counter.getName() + " as it is being used in a condition.");
            throw new ResourceInUseException("Counter is in use.");
        }

        boolean success = counterDao.remove(counterId);
        if (success) {
            s_logger.info("Successfully deleted counter with Id: " + counterId);
        }

        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CONDITION_DELETE, eventDescription = "delete a condition")
    public boolean deleteCondition(long conditionId) throws ResourceInUseException {
        /* Check if entity is in database */
        ConditionVO condition = getEntityInDatabase(CallContext.current().getCallingAccount(), "Condition", conditionId, conditionDao);
        if (condition == null) {
            throw new InvalidParameterValueException("Unable to find Condition");
        }

        // Verify if condition is used in any autoscale policy
        if (autoScalePolicyConditionMapDao.isConditionInUse(conditionId)) {
            s_logger.info("Cannot delete condition " + conditionId + " as it is being used in a condition.");
            throw new ResourceInUseException("Cannot delete Condition when it is in use by one or more AutoScale Policies.");
        }
        boolean success = conditionDao.remove(conditionId);
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
        ConditionVO condition = getEntityInDatabase(CallContext.current().getCallingAccount(), "Condition", conditionId, conditionDao);

        String operator = cmd.getRelationalOperator().toUpperCase();
        Long threshold = cmd.getThreshold();

        Condition.Operator op;
        // Validate Relational Operator
        try {
            op = Condition.Operator.valueOf(operator);
        } catch (IllegalArgumentException ex) {
            throw new InvalidParameterValueException("The Operator " + operator + " does not exist; Unable to update Condition.");
        }
        if (threshold < 0) {
            throw new InvalidParameterValueException("The threshold " + threshold + " must be equal to or greater than 0.");
        }

        // Verify if condition is used in any autoscale vmgroup
        GenericSearchBuilder<AutoScalePolicyConditionMapVO, Long> conditionSearch = autoScalePolicyConditionMapDao.createSearchBuilder(Long.class);
        conditionSearch.selectFields(conditionSearch.entity().getPolicyId());
        conditionSearch.and("conditionId", conditionSearch.entity().getConditionId(), Op.EQ);
        SearchCriteria<Long> sc = conditionSearch.create();
        sc.setParameters("conditionId", conditionId);
        List<Long> policyIds = autoScalePolicyConditionMapDao.customSearch(sc, null);

        if (CollectionUtils.isNotEmpty(policyIds)) {
            SearchBuilder<AutoScaleVmGroupPolicyMapVO> policySearch = autoScaleVmGroupPolicyMapDao.createSearchBuilder();
            policySearch.and("policyId", policySearch.entity().getPolicyId(), Op.IN);
            SearchBuilder<AutoScaleVmGroupVO> vmGroupSearch = autoScaleVmGroupDao.createSearchBuilder();
            vmGroupSearch.and("stateNEQ", vmGroupSearch.entity().getState(), Op.NEQ);
            vmGroupSearch.join("policySearch", policySearch, vmGroupSearch.entity().getId(), policySearch.entity().getVmGroupId(), JoinBuilder.JoinType.INNER);
            vmGroupSearch.done();

            SearchCriteria<AutoScaleVmGroupVO> sc2 = vmGroupSearch.create();
            sc2.setParameters("stateNEQ", AutoScaleVmGroup.State.DISABLED);
            sc2.setJoinParameters("policySearch", "policyId", policyIds.toArray((new Object[policyIds.size()])));
            List<AutoScaleVmGroupVO> groups = autoScaleVmGroupDao.search(sc2, null);
            if (CollectionUtils.isNotEmpty(groups)) {
                String msg = String.format("Cannot update condition %d as it is being used in %d vm groups NOT in Disabled state.", conditionId, groups.size());
                s_logger.info(msg);
                throw new ResourceInUseException(msg);
            }
        }

        condition.setRelationalOperator(op);
        condition.setThreshold(threshold);
        boolean success = conditionDao.update(conditionId, condition);
        if (success) {
            s_logger.info("Successfully updated condition " + condition.getId());

            for (Long policyId : policyIds) {
                markStatisticsAsInactive(null, policyId);
            }
        }
        return condition;
    }

    @Override
    public boolean deleteAutoScaleVmGroupsByAccount(Long accountId) {
        boolean success = true;
        List<AutoScaleVmGroupVO> groups = autoScaleVmGroupDao.listByAccount(accountId);
        for (AutoScaleVmGroupVO group : groups) {
            s_logger.debug("Deleting AutoScale Vm Group " + group + " for account Id: " + accountId);
            try {
                deleteAutoScaleVmGroup(group.getId(), true);
                s_logger.debug("AutoScale Vm Group " + group + " has been successfully deleted for account Id: " + accountId);
            } catch (Exception e) {
                s_logger.warn("Failed to delete AutoScale Vm Group " + group + " for account Id: " + accountId + " due to: ", e);
                success = false;
            }
        }
        return success;
    }

    @Override
    public void cleanUpAutoScaleResources(Long accountId) {
        // cleans Autoscale VmProfiles, AutoScale Policies and Conditions belonging to an account
        int count = 0;
        count = autoScaleVmProfileDao.removeByAccountId(accountId);
        if (count > 0) {
            s_logger.debug("Deleted " + count + " AutoScale Vm Profile for account Id: " + accountId);
        }
        count = autoScalePolicyDao.removeByAccountId(accountId);
        if (count > 0) {
            s_logger.debug("Deleted " + count + " AutoScale Policies for account Id: " + accountId);
        }
        count = conditionDao.removeByAccountId(accountId);
        if (count > 0) {
            s_logger.debug("Deleted " + count + " Conditions for account Id: " + accountId);
        }
    }

    private boolean checkConditionUp(AutoScaleVmGroupVO asGroup, Integer numVm) {
        // check maximum
        Integer currentVM = autoScaleVmGroupVmMapDao.countAvailableVmsByGroup(asGroup.getId());
        Integer maxVm = asGroup.getMaxMembers();
        if (currentVM + numVm > maxVm) {
            s_logger.warn("number of VM will greater than the maximum in this group if scaling up, so do nothing more");
            return false;
        }
        return true;
    }

    private boolean checkConditionDown(AutoScaleVmGroupVO asGroup) {
        Integer currentVM = autoScaleVmGroupVmMapDao.countAvailableVmsByGroup(asGroup.getId());
        Integer minVm = asGroup.getMinMembers();
        if (currentVM - 1 < minVm) {
            s_logger.warn("number of VM will less than the minimum in this group if scaling down, so do nothing more");
            return false;
        }
        return true;
    }

    protected Map<String, String> getDeployParams (String otherDeployParams) {
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

    protected long createNewVM(AutoScaleVmGroupVO asGroup) {
        AutoScaleVmProfileVO profileVo = autoScaleVmProfileDao.findById(asGroup.getProfileId());
        long templateId = profileVo.getTemplateId();
        long serviceOfferingId = profileVo.getServiceOfferingId();
        if (templateId == -1) {
            return -1;
        }
        // create new VM into DB
        try {
            //Verify that all objects exist before passing them to the service
            Account owner = accountService.getActiveAccountById(profileVo.getAccountId());

            DataCenter zone = entityMgr.findById(DataCenter.class, profileVo.getZoneId());
            if (zone == null) {
                throw new InvalidParameterValueException("Unable to find zone by id=" + profileVo.getZoneId());
            }

            ServiceOffering serviceOffering = entityMgr.findById(ServiceOffering.class, serviceOfferingId);
            if (serviceOffering == null) {
                throw new InvalidParameterValueException("Unable to find service offering: " + serviceOfferingId);
            }

            DiskOffering diskOffering = entityMgr.findById(DiskOffering.class, serviceOffering.getDiskOfferingId());
            if (diskOffering == null) {
                throw new InvalidParameterValueException("Unable to find disk offering: " + serviceOffering.getDiskOfferingId());
            }

            VirtualMachineTemplate template = entityMgr.findById(VirtualMachineTemplate.class, templateId);
            // Make sure a valid template ID was specified
            if (template == null) {
                throw new InvalidParameterValueException("Unable to use template " + templateId);
            }

            if (!zone.isLocalStorageEnabled()) {
                if (diskOffering.isUseLocalStorage()) {
                    throw new InvalidParameterValueException("Zone is not configured to use local storage but disk offering " + diskOffering.getName() + " associated to the service offering " + serviceOffering.getName() + " uses it");
                }
            }

            String userData = profileVo.getUserData();

            UserVm vm = null;
            IpAddresses addrs = new IpAddresses(null, null);
            HypervisorType hypervisorType = template.getHypervisorType();
            final Network network = getNetwork(asGroup.getLoadBalancerId());
            final Map<String, String> customParameters = new HashMap<>();
            final Map<String, String> deployParams = getDeployParams(profileVo.getOtherDeployParams());

            final List<Long> networkIds = getVmNetworkIds(deployParams, network.getId());
            Long overrideDiskOfferingId = getVmOverrideDiskOfferingId(deployParams);
            Long diskOfferingId = getVmDiskOfferingId(deployParams);
            Long dataDiskSize = getVmDataDiskSize(deployParams);
            List<String> sshKeyPairs = getVmSshKeyPairs(deployParams, owner);
            List<Long> affinityGroupIdList = getVmAffinityGroupId(deployParams);
            updateVmDetails(deployParams, customParameters);

            String vmHostName = getNextVmHostName(asGroup);
            asGroup.setNextVmSeq(asGroup.getNextVmSeq() + 1);
            autoScaleVmGroupDao.persist(asGroup);

            if (zone.getNetworkType() == NetworkType.Basic) {
                vm = userVmService.createBasicSecurityGroupVirtualMachine(zone, serviceOffering, template, null, owner, vmHostName,
                        vmHostName, diskOfferingId, dataDiskSize, null,
                        hypervisorType, HTTPMethod.GET, userData, null, null, sshKeyPairs,
                        null, null, true, null, affinityGroupIdList, customParameters, null, null, null,
                        null, true, overrideDiskOfferingId);
            } else {
                if (zone.isSecurityGroupEnabled()) {
                    vm = userVmService.createAdvancedSecurityGroupVirtualMachine(zone, serviceOffering, template, networkIds, null,
                            owner, vmHostName,vmHostName, diskOfferingId, dataDiskSize, null,
                            hypervisorType, HTTPMethod.GET, userData, null, null, sshKeyPairs,
                            null, null, true, null, affinityGroupIdList, customParameters, null, null, null,
                            null, true, overrideDiskOfferingId, null);
                } else {
                    vm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner, vmHostName, vmHostName,
                            diskOfferingId, dataDiskSize, null,
                            hypervisorType, HTTPMethod.GET, userData, null, null, sshKeyPairs,
                            null, addrs, true, null, affinityGroupIdList, customParameters, null, null, null,
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

    protected List<Long> getVmNetworkIds(Map<String, String> deployParams, long defaultNetworkId) {
        final List<Long> networkIds = new ArrayList<>();
        networkIds.add(defaultNetworkId);
        if (deployParams.get(PARAM_NETWORK_IDS) != null) { // networkids, append to LB networkid
            String[] networkids = deployParams.get(PARAM_NETWORK_IDS).split(",");
            for (String networkid : networkids) {
                Network otherNetwork = networkDao.findByUuid(networkid);
                if (otherNetwork != null && otherNetwork.getId() != defaultNetworkId) {
                    networkIds.add(otherNetwork.getId());
                }
            }
        }
        return networkIds;
    }

    protected Long getVmOverrideDiskOfferingId(Map<String, String> deployParams) {
        Long overrideDiskOfferingId = null;     // override ROOT disk offering
        if (deployParams.get(PARAM_OVERRIDE_DISK_OFFERING_ID) != null) {
            String overrideDiskOfferingUuid = deployParams.get(PARAM_OVERRIDE_DISK_OFFERING_ID);
            DiskOffering overrideDiskOfferingInParam = diskOfferingDao.findByUuid(overrideDiskOfferingUuid);
            if (overrideDiskOfferingInParam != null) {
                overrideDiskOfferingId = overrideDiskOfferingInParam.getId();
            } else {
                s_logger.warn("Cannot find disk offering by overridediskofferingid from otherdeployparams in AutoScale Vm profile");
            }
        }
        return overrideDiskOfferingId;
    }

    protected final Long getVmDiskOfferingId(Map<String, String> deployParams) {
        Long diskOfferingId = null;     // DATA disk offering ID
        if (deployParams.get(PARAM_DISK_OFFERING_ID) != null) {
            String diskOfferingUuid = deployParams.get(PARAM_DISK_OFFERING_ID);
            DiskOffering diskOfferingInParam = diskOfferingDao.findByUuid(diskOfferingUuid);
            if (diskOfferingInParam != null) {
                diskOfferingId = diskOfferingInParam.getId();
            } else {
                s_logger.warn("Cannot find disk offering by diskofferingid from otherdeployparams in AutoScale Vm profile");
            }
        }
        return diskOfferingId;
    }

    protected Long getVmDataDiskSize(Map<String, String> deployParams) {
        Long dataDiskSize = null;       // DATA disk size
        if (deployParams.get(PARAM_DISK_SIZE) != null) {
            String dataDiskSizeInParam = deployParams.get(PARAM_DISK_SIZE);
            try {
                dataDiskSize = Long.parseLong(dataDiskSizeInParam);
            } catch (NumberFormatException ex) {
                s_logger.warn("Cannot parse size from otherdeployparams in AutoScale Vm profile");
            }
        }
        return dataDiskSize;
    }

    protected List<String> getVmSshKeyPairs(Map<String, String> deployParams, Account owner) {
        List<String> sshKeyPairs = new ArrayList<>();
        if (deployParams.get(PARAM_SSH_KEYPAIRS) != null) {   // SSH keypairs
            String[] keypairs = deployParams.get(PARAM_SSH_KEYPAIRS).split(",");
            for (String keypair : keypairs) {
                SSHKeyPairVO s = sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), keypair);
                if (s != null) {
                    sshKeyPairs.add(s.getName());
                } else {
                    s_logger.warn("Cannot find ssh keypair by name in sshkeypairs from otherdeployparams in AutoScale Vm profile");
                }
            }
        }
        return sshKeyPairs;
    }

    protected List<Long> getVmAffinityGroupId(Map<String, String> deployParams) {
        List<Long> affinityGroupIdList = new ArrayList<>();
        if (deployParams.get(PARAM_AFFINITY_GROUP_IDS) != null) {   // Affinity groups
            String[] affinityGroupIds = deployParams.get(PARAM_AFFINITY_GROUP_IDS).split(",");
            for (String affinityGroupId : affinityGroupIds) {
                AffinityGroupVO affintyGroup = affinityGroupDao.findByUuid(affinityGroupId);
                if (affintyGroup != null) {
                    affinityGroupIdList.add(affintyGroup.getId());
                } else {
                    s_logger.warn("Cannot find affinity group by affinitygroupids from otherdeployparams in AutoScale Vm profile");
                }
            }
        }
        return affinityGroupIdList;
    }

    public void updateVmDetails(Map<String, String> deployParams, Map<String, String> customParameters) {
        if (deployParams.get(PARAM_ROOT_DISK_SIZE) != null) {     // ROOT disk size
            String value = deployParams.get(PARAM_ROOT_DISK_SIZE);
            try {
                Long rootDiskSize = Long.parseLong(value);
                customParameters.put(VmDetailConstants.ROOT_DISK_SIZE, String.valueOf(rootDiskSize));
            } catch (NumberFormatException ex) {
                s_logger.warn("Cannot parse rootdisksize from otherdeployparams in AutoScale Vm profile");
            }
        }
    }

    private String getNextVmHostName(AutoScaleVmGroupVO asGroup) {
        String vmHostNameSuffix = "-" + asGroup.getNextVmSeq() + "-" +
                RandomStringUtils.random(VM_HOSTNAME_RANDOM_SUFFIX_LENGTH, 0, 0, true, false, (char[])null, new SecureRandom()).toLowerCase();
        // Truncate vm group name because max length of vm name is 63
        int subStringLength = Math.min(asGroup.getName().length(), 63 - VM_HOSTNAME_PREFIX.length() - vmHostNameSuffix.length());
        return VM_HOSTNAME_PREFIX + asGroup.getName().substring(0, subStringLength) + vmHostNameSuffix;
    }

    private void checkAutoScaleVmGroupName(String groupName) {
        String errorMessage = "";
        if (groupName == null || groupName.length() > 255 || groupName.length() < 1) {
            errorMessage = "AutoScale Vm Group name must be between 1 and 255 characters long";
        } else if (!groupName.toLowerCase().matches("[a-z0-9-]*")) {
            errorMessage = "AutoScale Vm Group name may contain only the ASCII letters 'a' through 'z' (in a case-insensitive manner), " +
                    "the digits '0' through '9' and the hyphen ('-')";
        }
        if (StringUtils.isNotBlank(errorMessage)) {
            s_logger.warn(errorMessage);
            throw new InvalidParameterValueException("Invalid AutoScale VM group name. It can contain the ASCII letters 'a' through 'z', " +
                    "'A' through 'Z', the digits '0' through '9' and the hyphen ('-'), must be between 1 and 255 characters long.");
        }
    }

    private boolean startNewVM(long vmId) {
        try {
            CallContext.current().setEventDetails("Vm Id: " + vmId);
            userVmMgr.startVirtualMachine(vmId, null, null, null);
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

        List<LoadBalancerVMMapVO> lbVmMapVos = lbVmMapDao.listByLoadBalancerId(lbId);
        if (CollectionUtils.isNotEmpty(lbVmMapVos)) {
            for (LoadBalancerVMMapVO LbVmMapVo : lbVmMapVos) {
                long instanceId = LbVmMapVo.getInstanceId();
                if (instanceId == vmId) {
                    s_logger.warn("the new VM is already mapped to LB rule. What's wrong?");
                    return true;
                }
            }
        }
        lstVmId.add(new Long(vmId));
        try {
            return loadBalancingRulesService.assignToLoadBalancer(lbId, lstVmId, new HashMap<>(), true);
        } catch (CloudRuntimeException ex) {
            s_logger.warn("Caught exception: ", ex);
            return false;
        }
    }

    private long removeLBrule(AutoScaleVmGroupVO asGroup) {
        long lbId = asGroup.getLoadBalancerId();
        long instanceId = -1;
        List<LoadBalancerVMMapVO> lbVmMapVos = lbVmMapDao.listByLoadBalancerId(lbId);
        if (CollectionUtils.isNotEmpty(lbVmMapVos)) {
            for (LoadBalancerVMMapVO LbVmMapVo : lbVmMapVos) {
                instanceId = LbVmMapVo.getInstanceId();
            }
        }
        // take last VM out of the list
        List<Long> lstVmId = new ArrayList<Long>();
        if (instanceId != -1)
            lstVmId.add(instanceId);
        if (loadBalancingRulesService.removeFromLoadBalancer(lbId, lstVmId, new HashMap<>(), true))
            return instanceId;
        else
            return -1;
    }

    @Override
    public void doScaleUp(long groupId, Integer numVm) {
        AutoScaleVmGroupVO asGroup = autoScaleVmGroupDao.findById(groupId);
        if (asGroup == null) {
            s_logger.error("Can not find the groupid " + groupId + " for scaling up");
            return;
        }
        if (!checkConditionUp(asGroup, numVm)) {
            return;
        }
        AutoScaleVmGroup.State oldState = asGroup.getState();
        AutoScaleVmGroup.State newState = AutoScaleVmGroup.State.SCALING;
        if (!autoScaleVmGroupDao.updateState(groupId, oldState, newState)) {
            s_logger.error(String.format("Can not update vmgroup state from %s to %s, groupId: %s", oldState, newState, groupId));
            return;
        }
        try {
            for (int i = 0; i < numVm; i++) {
                ActionEventUtils.onStartedActionEvent(User.UID_SYSTEM, asGroup.getAccountId(), EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEUP,
                        "Scaling Up AutoScale VM group " + groupId, groupId, ApiCommandResourceType.AutoScaleVmGroup.toString(),
                        true, 0);
                long vmId = createNewVM(asGroup);
                if (vmId == -1) {
                    s_logger.error("Can not deploy new VM for scaling up in the group "
                            + asGroup.getId() + ". Waiting for next round");
                    break;
                }
                // persist to DB
                AutoScaleVmGroupVmMapVO groupVmMapVO = new AutoScaleVmGroupVmMapVO(asGroup.getId(), vmId);
                autoScaleVmGroupVmMapDao.persist(groupVmMapVO);

                // Add an Inactive-dummy record to statistics table
                createInactiveDummyRecord(asGroup.getId());

                try {
                    startNewVM(vmId);
                    createInactiveDummyRecord(asGroup.getId());
                    if (assignLBruleToNewVm(vmId, asGroup)) {
                        // update last_quietTime
                        List<AutoScaleVmGroupPolicyMapVO> groupPolicyVOs = autoScaleVmGroupPolicyMapDao
                                .listByVmGroupId(groupId);
                        for (AutoScaleVmGroupPolicyMapVO groupPolicyVO : groupPolicyVOs) {
                            AutoScalePolicyVO vo = autoScalePolicyDao
                                    .findById(groupPolicyVO.getPolicyId());
                            if (vo.getAction().equals(AutoScalePolicy.Action.SCALEUP)) {
                                vo.setLastQuietTime(new Date());
                                autoScalePolicyDao.persist(vo);
                                break;
                            }
                        }
                        ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, asGroup.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEUP,
                                String.format("Started and assigned LB rule for VM %d in AutoScale VM group %d", vmId, groupId), groupId, ApiCommandResourceType.AutoScaleVmGroup.toString(), 0);
                    } else {
                        s_logger.error("Can not assign LB rule for this new VM");
                        ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, asGroup.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEUP,
                                String.format("Failed to assign LB rule for VM %d in AutoScale VM group %d", vmId, groupId), groupId, ApiCommandResourceType.AutoScaleVmGroup.toString(), 0);
                        break;
                    }
                } catch (ServerApiException e) {
                    s_logger.error("Can not deploy new VM for scaling up in the group "
                            + asGroup.getId() + ". Waiting for next round");
                    ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, asGroup.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEUP,
                            String.format("Failed to start VM %d in AutoScale VM group %d", vmId, groupId), groupId, ApiCommandResourceType.AutoScaleVmGroup.toString(), 0);
                    destroyVm(vmId);
                    break;
                }
            }
        } finally {
            if (!autoScaleVmGroupDao.updateState(groupId, newState, oldState)) {
                s_logger.error(String.format("Can not update vmgroup state from %s back to %s, groupId: %s", newState, oldState, groupId));
            }
        }
    }

    @Override
    public void doScaleDown(final long groupId) {
        AutoScaleVmGroupVO asGroup = autoScaleVmGroupDao.findById(groupId);
        if (asGroup == null) {
            s_logger.error("Can not find the groupid " + groupId + " for scaling down");
            return;
        }
        if (!checkConditionDown(asGroup)) {
            return;
        }
        AutoScaleVmGroup.State oldState = asGroup.getState();
        AutoScaleVmGroup.State newState = AutoScaleVmGroup.State.SCALING;
        if (!autoScaleVmGroupDao.updateState(groupId, oldState, newState)) {
            s_logger.error(String.format("Can not update vmgroup state from %s to %s, groupId: %s", oldState, newState, groupId));
            return;
        }
        ActionEventUtils.onStartedActionEvent(User.UID_SYSTEM, asGroup.getAccountId(), EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEDOWN,
                "Scaling down AutoScale VM group " + groupId, groupId, ApiCommandResourceType.AutoScaleVmGroup.toString(),
                true, 0);
        try {
            long vmId = -1;
            try {
                vmId = removeLBrule(asGroup);
            } catch (Exception ex) {
                s_logger.info("Got exception when remove LB rule for a VM in AutoScale VM group %d: " + groupId, ex);
                ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, asGroup.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEDOWN,
                        String.format("Failed to remove LB rule for a VM in AutoScale VM group %d", groupId), groupId, ApiCommandResourceType.AutoScaleVmGroup.toString(), 0);
                throw ex;
            }
            if (vmId != -1) {
                long profileId = asGroup.getProfileId();

                // update group-vm mapping
                autoScaleVmGroupVmMapDao.remove(groupId, vmId);
                // update last_quietTime
                List<AutoScaleVmGroupPolicyMapVO> groupPolicyVOs = autoScaleVmGroupPolicyMapDao.listByVmGroupId(groupId);
                for (AutoScaleVmGroupPolicyMapVO groupPolicyVO : groupPolicyVOs) {
                    AutoScalePolicyVO vo = autoScalePolicyDao.findById(groupPolicyVO.getPolicyId());
                    if (vo.getAction().equals(AutoScalePolicy.Action.SCALEUP)) {
                        vo.setLastQuietTime(new Date());
                        autoScalePolicyDao.persist(vo);
                        break;
                    }
                }

                // Add an Inactive-dummy record to statistics table
                createInactiveDummyRecord(asGroup.getId());

                // get expungeVmGracePeriod param
                AutoScaleVmProfileVO asProfile = autoScaleVmProfileDao.findById(profileId);
                Integer expungeVmGracePeriod = asProfile.getExpungeVmGracePeriod();
                if (expungeVmGracePeriod > 0) {
                    try {
                        Thread.sleep(expungeVmGracePeriod * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new CloudRuntimeException(String.format("Error while waiting %s seconds to destroy the VM %s", expungeVmGracePeriod, vmId));
                    }
                }
                if (destroyVm(vmId)) {
                    ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, asGroup.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEDOWN,
                            String.format("Destroyed VM %d in AutoScale VM group %d", vmId, groupId), groupId, ApiCommandResourceType.AutoScaleVmGroup.toString(), 0);
                } else {
                    ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, asGroup.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEDOWN,
                            String.format("Failed to destroy VM %d in AutoScale VM group %d", vmId, groupId), groupId, ApiCommandResourceType.AutoScaleVmGroup.toString(), 0);
                }
            } else {
                s_logger.error("Can not remove LB rule for the VM being destroyed. Do nothing more.");
                ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, asGroup.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_AUTOSCALEVMGROUP_SCALEDOWN,
                        String.format("Failed to remove LB rule for a VM in AutoScale VM group %d", groupId), groupId, ApiCommandResourceType.AutoScaleVmGroup.toString(), 0);
            }
        } finally {
            if (!autoScaleVmGroupDao.updateState(groupId, newState, oldState)) {
                s_logger.error(String.format("Can not update vmgroup state from %s back to %s, groupId: %s", newState, oldState, groupId));
            }
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
        List<AutoScaleVmGroupVO> asGroups = autoScaleVmGroupDao.listAll();
        for (AutoScaleVmGroupVO asGroup : asGroups) {
            completionService.submit(new CheckAutoScaleVmGroupAsync(asGroup));
        }
        for (int i = 0; i < asGroups.size(); i++) {
            try {
                Future<Pair<Long, Boolean>> future = completionService.take();
                Pair<Long, Boolean> result = future.get();
                s_logger.debug("Checked AutoScale vm group " + result.first() + " with result: " + result.second());
            } catch (ExecutionException ex) {
                s_logger.warn("Failed to get result of checking AutoScale vm group due to Exception: " , ex);
            } catch (InterruptedException ex) {
                s_logger.warn("Failed to get result of checking AutoScale vm group due to Exception: " , ex);
                Thread.currentThread().interrupt();
            }
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
        if (asGroup.getState().equals(AutoScaleVmGroup.State.ENABLED)) {
            Network.Provider provider = getLoadBalancerServiceProvider(asGroup.getLoadBalancerId());
            if (Network.Provider.Netscaler.equals(provider)) {
                checkNetScalerAsGroup(asGroup);
            } else if (Network.Provider.VirtualRouter.equals(provider) || Network.Provider.VPCVirtualRouter.equals(provider)) {
                checkVirtualRouterAsGroup(asGroup);
            }
        }
    }

    protected boolean isNative(AutoScaleVmGroupTO groupTO) {
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

    protected boolean hasSourceVirtualRouter(AutoScaleVmGroupTO groupTO) {
        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            for (ConditionTO conditionTO : policyTO.getConditions()) {
                CounterTO counterTO = conditionTO.getCounter();
                if (Counter.Source.VIRTUALROUTER.equals(counterTO.getSource())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected Map<Long, List<Long>> getHostAndVmIdsMap(AutoScaleVmGroupTO groupTO) {
        Map<Long, List<Long>> hostAndVmIdsMap = new HashMap<>();

        List<AutoScaleVmGroupVmMapVO> asGroupVmVOs = autoScaleVmGroupVmMapDao.listByGroup(groupTO.getId());
        for (AutoScaleVmGroupVmMapVO asGroupVmVO : asGroupVmVOs) {
            updateHostAndVmIdsMap(hostAndVmIdsMap, asGroupVmVO);
        }
        return hostAndVmIdsMap;
    }

    protected void updateHostAndVmIdsMap(Map<Long, List<Long>> hostAndVmIdsMap, AutoScaleVmGroupVmMapVO asGroupVmVO) {
        Long vmId = asGroupVmVO.getInstanceId();
        UserVmVO vm = userVmDao.findById(vmId);
        Long vmHostId = DEFAULT_HOST_ID;
        if (VirtualMachine.State.Running.equals(vm.getState())) {
            vmHostId = vm.getHostId();
        } else if (VirtualMachine.State.Migrating.equals(vm.getState())) {
            vmHostId = vm.getLastHostId();
        }
        List<Long> vmIds = hostAndVmIdsMap.get(vmHostId);
        if (vmIds == null) {
            vmIds = new ArrayList<>();
        }
        vmIds.add(vmId);
        hostAndVmIdsMap.put(vmHostId, vmIds);
    }

    protected Map<Long, List<CounterTO>> getPolicyCounters(AutoScaleVmGroupTO groupTO) {
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

    protected AutoScalePolicy.Action getAutoscaleAction(Map<String, Double> countersMap, Map<String, Integer> countersNumberMap, AutoScaleVmGroupTO groupTO) {
        s_logger.debug("[AutoScale] Getting autoscale action for group : " + groupTO.getId());

        Network.Provider provider = getLoadBalancerServiceProvider(groupTO.getLoadBalancerId());

        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            if (isQuitTimePassForPolicy(policyTO)) {
                AutoScalePolicy.Action determinedAction = checkConditionsForPolicy(countersMap, countersNumberMap, policyTO, provider);
                if (determinedAction != null) {
                    return determinedAction;
                }
            }
        }
        return null;
    }

    protected boolean isQuitTimePassForPolicy(AutoScalePolicyTO policyTO) {
        int quietTime = policyTO.getQuietTime();
        Date quietTimeDate = policyTO.getLastQuietTime();
        long lastQuiettime = 0L;
        if (quietTimeDate != null) {
            lastQuiettime = policyTO.getLastQuietTime().getTime();
        }
        long currentTime = (new Date()).getTime();
        return (currentTime - lastQuiettime) >= quietTime * 1000L;
    }

    protected AutoScalePolicy.Action checkConditionsForPolicy(Map<String, Double> countersMap, Map<String, Integer> countersNumberMap, AutoScalePolicyTO policyTO, Network.Provider provider) {
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
            s_logger.debug(String.format("Checking policyId = %d, conditionId = %d, counter = \"%s\", sum = %f, number = %s", policyTO.getId(), conditionTO.getId(), counter.getName(), sum, number));
            if (number == null || number == 0) {
                bValid = false;
                s_logger.debug(String.format("Skipping policyId = %d, conditionId = %d, counter = \"%s\" because the number is %s", policyTO.getId(), conditionTO.getId(), counter.getName(), number));
                break;
            }
            Double avg = sum / number;
            Condition.Operator op = conditionTO.getRelationalOperator();
            boolean bConditionCheck = ((op == com.cloud.network.as.Condition.Operator.EQ) && (thresholdPercent.equals(avg)))
                    || ((op == com.cloud.network.as.Condition.Operator.GE) && (avg.doubleValue() >= thresholdPercent.doubleValue()))
                    || ((op == com.cloud.network.as.Condition.Operator.GT) && (avg.doubleValue() > thresholdPercent.doubleValue()))
                    || ((op == com.cloud.network.as.Condition.Operator.LE) && (avg.doubleValue() <= thresholdPercent.doubleValue()))
                    || ((op == com.cloud.network.as.Condition.Operator.LT) && (avg.doubleValue() < thresholdPercent.doubleValue()));

            s_logger.debug(String.format("Check result on policyId = %d, conditionId = %d, counter = %s is : %s" +
                            " (actual result = %f, operator = %s, threshold = %f)",
                    policyTO.getId(), conditionTO.getId(), counter.getSource(), bConditionCheck, avg, op, thresholdPercent));

            if (!bConditionCheck) {
                bValid = false;
                break;
            }
        }
        AutoScalePolicy.Action action = bValid ? policyTO.getAction() : null;
        s_logger.debug(String.format("Check result on policyId = %d is %s", policyTO.getId(), action));

        return action;
    }

    private List<Pair<String, Integer>> getPairofCounternameAndDuration(AutoScaleVmGroupTO groupTO) {
        List<Pair<String, Integer>> result = new ArrayList<>();

        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            //get duration
            Integer duration = policyTO.getDuration();
            for (ConditionTO conditionTO : policyTO.getConditions()) {
                CounterTO counter = conditionTO.getCounter();
                StringBuilder buff = new StringBuilder();
                buff.append(counter.getName());
                buff.append(",");
                buff.append(conditionTO.getId());
                // add to result
                Pair<String, Integer> pair = new Pair<>(buff.toString(), duration);
                result.add(pair);
            }
        }
        return result;
    }

    protected Network getNetwork(Long loadBalancerId) {
        final LoadBalancerVO loadBalancer = lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            throw new CloudRuntimeException(String.format("Unable to find load balancer with id: %s ", loadBalancerId));
        }
        Network network = networkDao.findById(loadBalancer.getNetworkId());
        if (network == null) {
            throw new CloudRuntimeException(String.format("Unable to find network with id: %s ", loadBalancer.getNetworkId()));
        }
        return network;
    }

    protected Pair<String, Integer> getPublicIpAndPort(Long loadBalancerId) {
        final LoadBalancerVO loadBalancer = lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            throw new CloudRuntimeException(String.format("Unable to find load balancer with id: %s ", loadBalancerId));
        }
        IPAddressVO ipAddress = ipAddressDao.findById(loadBalancer.getSourceIpAddressId());
        if (ipAddress == null) {
            throw new CloudRuntimeException(String.format("Unable to find IP Address with id: %s ", loadBalancer.getSourceIpAddressId()));
        }
        return new Pair<>(ipAddress.getAddress().addr(), loadBalancer.getSourcePortStart());
    }

    protected Network.Provider getLoadBalancerServiceProvider(Long loadBalancerId) {
        final LoadBalancerVO loadBalancer = lbDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            throw new CloudRuntimeException(String.format("Unable to find load balancer with id: %s ", loadBalancerId));
        }
        return lbRulesMgr.getLoadBalancerServiceProvider(loadBalancer);
    }

    protected boolean checkAsGroupMaxAndMinMembers(AutoScaleVmGroupVO asGroup) {
        // check minimum vm of group
        Integer currentVM = autoScaleVmGroupVmMapDao.countAvailableVmsByGroup(asGroup.getId());
        if (currentVM < asGroup.getMinMembers()) {
            s_logger.debug(String.format("There are currently %s available VMs which is less than the minimum member of " +
                    "the AS group (%s), scaling up %d VMs", currentVM, asGroup.getMinMembers(), asGroup.getMinMembers() - currentVM));
            doScaleUp(asGroup.getId(), asGroup.getMinMembers() - currentVM);
            return false;
        }

        // check maximum vm of group
        if (currentVM > asGroup.getMaxMembers()) {
            s_logger.debug(String.format("There are currently %s available VMs which is more than the maximum member of " +
                    "the AS group (%s), scaling down %d VMs", currentVM, asGroup.getMaxMembers(), currentVM - asGroup.getMaxMembers()));
            for (int i = 0; i <  currentVM - asGroup.getMaxMembers(); i++) {
                doScaleDown(asGroup.getId());
            }
            return false;
        }
        return true;
    }

    protected void checkNetScalerAsGroup(AutoScaleVmGroupVO asGroup) {
        AutoScaleVmGroupTO groupTO = lbRulesMgr.toAutoScaleVmGroupTO(asGroup);

        if (!isNative(groupTO)) {
            return;
        }

        if (!checkAsGroupMaxAndMinMembers(asGroup)) {
            return;
        }

        //check interval
        long now = (new Date()).getTime();
        if (asGroup.getLastInterval() != null && (now - asGroup.getLastInterval().getTime()) < asGroup.getInterval()) {
            return;
        }

        // update last_interval
        asGroup.setLastInterval(new Date());
        autoScaleVmGroupDao.persist(asGroup);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("[Netscaler AutoScale] Collecting RRDs data...");
        }
        Map<String, String> params = new HashMap<>();
        List<AutoScaleVmGroupVmMapVO> asGroupVmVOs = autoScaleVmGroupVmMapDao.listByGroup(asGroup.getId());
        params.put("total_vm", String.valueOf(asGroupVmVOs.size()));
        for (int i = 0; i < asGroupVmVOs.size(); i++) {
            long vmId = asGroupVmVOs.get(i).getInstanceId();
            VMInstanceVO vmVO = vmInstanceDao.findById(vmId);
            //xe vm-list | grep vmname -B 1 | head -n 1 | awk -F':' '{print $2}'
            params.put("vmname" + (i + 1), vmVO.getInstanceName());
            params.put("vmid" + (i + 1), String.valueOf(vmVO.getId()));

        }
        // get random hostid because all vms are in a cluster
        long vmId = asGroupVmVOs.get(0).getInstanceId();
        VMInstanceVO vmVO = vmInstanceDao.findById(vmId);
        Long receiveHost = vmVO.getHostId();

        setPerformanceMonitorCommandParams(groupTO, params);

        PerformanceMonitorCommand perfMon = new PerformanceMonitorCommand(params, 20);

        try {
            PerformanceMonitorAnswer answer = (PerformanceMonitorAnswer) agentMgr.send(receiveHost, perfMon);
            if (answer == null || !answer.getResult()) {
                s_logger.debug("Failed to send data to node !");
            } else {
                String result = answer.getDetails();
                s_logger.debug("[AutoScale] RRDs collection answer: " + result);
                HashMap<String, Double> countersMap = new HashMap<>();
                HashMap<String, Integer> countersNumberMap = new HashMap<>();

                processPerformanceMonitorAnswer(countersMap, countersNumberMap, groupTO, params, result);

                AutoScalePolicy.Action scaleAction = getAutoscaleAction(countersMap, countersNumberMap, groupTO);
                if (scaleAction != null) {
                    s_logger.debug("[AutoScale] Doing scale action: " + scaleAction + " for group " + asGroup.getId());
                    if (AutoScalePolicy.Action.SCALEUP.equals(scaleAction)) {
                        doScaleUp(asGroup.getId(), 1);
                    } else {
                        doScaleDown(asGroup.getId());
                    }
                }
            }

        } catch (Exception e) {
            s_logger.error("Cannot sent PerformanceMonitorCommand to host " + receiveHost + " or process the answer due to Exception: ", e);
        }
    }

    protected void setPerformanceMonitorCommandParams(AutoScaleVmGroupTO groupTO, Map<String, String> params) {
        // setup parameters phase: duration and counter
        // list pair [counter, duration]
        List<Pair<String, Integer>> lstPair = getPairofCounternameAndDuration(groupTO);
        int totalCounter = 0;
        String[] lstCounter = new String[lstPair.size()];
        for (int i = 0; i < lstPair.size(); i++) {
            Pair<String, Integer> pair = lstPair.get(i);
            String strCounterNames = pair.first();
            Integer duration = pair.second();

            lstCounter[i] = strCounterNames.split(",")[0];  // counter name
            totalCounter++;
            params.put("duration" + totalCounter, duration.toString());
            params.put("counter" + totalCounter, lstCounter[i]);
            params.put("con" + totalCounter, strCounterNames.split(",")[1]);    // condition id
        }
        params.put("totalCounter", String.valueOf(totalCounter));
    }

    protected void processPerformanceMonitorAnswer(Map<String, Double> countersMap, Map<String, Integer> countersNumberMap,
                                                 AutoScaleVmGroupTO groupTO, Map<String, String> params, String details) {
        // extract data
        String[] counterElements = details.split(",");
        if ((counterElements != null) && (counterElements.length > 0)) {
            for (String string : counterElements) {
                try {
                    String[] counterVals = string.split(":");
                    String[] counterVm = counterVals[0].split("\\.");

                    Long counterId = Long.parseLong(counterVm[1]);

                    Long conditionId = Long.parseLong(params.get("con" + counterVm[0]));

                    Long policyId = 0L; // For NetScaler, the policyId is not returned in PerformanceMonitorAnswer

                    Double coVal = Double.parseDouble(counterVals[1]);

                    updateCountersMapWithInstantData(countersMap, countersNumberMap, groupTO, counterId, conditionId, policyId, coVal, AutoScaleValueType.INSTANT_VM);

                } catch (Exception e) {
                    s_logger.error("Cannot process PerformanceMonitorAnswer due to Exception: ", e);
                }
            }
        }
    }

    protected void updateCountersMapWithInstantData(Map<String, Double> countersMap, Map<String, Integer> countersNumberMap, AutoScaleVmGroupTO groupTO,
                                                    Long counterId, Long conditionId, Long policyId, Double coVal, AutoScaleValueType valueType) {
        // Summary of all counter by counterId key
        String key = generateKeyFromPolicyAndConditionAndCounter(policyId, conditionId, counterId);

        CounterVO counter = counterDao.findById(counterId);
        if (counter == null) {
            return;
        }
        if (Counter.Source.MEMORY.equals(counter.getSource())) {
            // calculate memory in percent
            AutoScaleVmProfileTO profile = groupTO.getProfile();
            ServiceOfferingVO serviceOff = serviceOfferingDao.findByUuidIncludingRemoved(profile.getServiceOfferingId());
            int maxRAM = serviceOff.getRamSize();

            // get current RAM percent
            coVal = coVal / maxRAM * 100;
        } else if (Counter.Source.CPU.equals(counter.getSource())) {
            // cpu
            coVal = coVal * 100;
        }

        if (AutoScaleValueType.INSTANT_VM_GROUP.equals(valueType)) {
            Integer currentVM = autoScaleVmGroupVmMapDao.countAvailableVmsByGroup(groupTO.getId());
            if (currentVM == 0) {
                s_logger.debug(String.format("Skipping updating countersMap for group %s and policy %s and counter %s due to no VMs", groupTO.getId(), policyId, counterId));
                return;
            }
            coVal = coVal / currentVM;
        }

        updateCountersMapWithProcessedData(countersMap, countersNumberMap, key, coVal);
    }

    protected void updateCountersMapWithProcessedData(Map<String, Double> countersMap, Map<String, Integer> countersNumberMap, String key, Double coVal) {
        /* initialize if data is not set */
        countersMap.computeIfAbsent(key, k -> Double.valueOf(0));
        countersNumberMap.computeIfAbsent(key, k -> 0);

        // update data entry
        countersMap.put(key, countersMap.get(key) + coVal);
        countersNumberMap.put(key, countersNumberMap.get(key) + 1);
    }

    protected void monitorVirtualRouterAsGroup(AutoScaleVmGroupVO asGroup) {
        if (!checkAsGroupMaxAndMinMembers(asGroup)) {
            return;
        }

        //check interval
        long now = (new Date()).getTime();
        if (asGroup.getLastInterval() != null && (now - asGroup.getLastInterval().getTime()) < asGroup.getInterval()) {
            return;
        }

        // update last_interval
        asGroup.setLastInterval(new Date());
        autoScaleVmGroupDao.persist(asGroup);

        s_logger.debug("[AutoScale] Collecting performance data ...");

        AutoScaleVmGroupTO groupTO = lbRulesMgr.toAutoScaleVmGroupTO(asGroup);

        if (isNative(groupTO)) {
            s_logger.debug("[AutoScale] Collecting performance data from hosts ...");
            getVmStatsFromHosts(groupTO);
        }

        if (hasSourceVirtualRouter(groupTO)) {
            s_logger.debug("[AutoScale] Collecting performance data from virtual router ...");
            getNetworkStatsFromVirtualRouter(groupTO);
        }
    }

    protected void checkVirtualRouterAsGroup(AutoScaleVmGroupVO asGroup) {
        AutoScaleVmGroupTO groupTO = lbRulesMgr.toAutoScaleVmGroupTO(asGroup);

        Map<String, Double> countersMap = new HashMap<>();
        Map<String, Integer> countersNumberMap = new HashMap<>();

        try {
            // update counter maps in memory
            if (!updateCountersMap(groupTO, countersMap, countersNumberMap)) {
                return;
            }
        } finally {
            // Remove old statistics from database
            cleanupAsVmGroupStatistics(groupTO);
        }

        // get scale action
        AutoScalePolicy.Action scaleAction = getAutoscaleAction(countersMap, countersNumberMap, groupTO);
        if (scaleAction != null) {
            s_logger.debug("[AutoScale] Doing scale action: " + scaleAction + " for group " + asGroup.getId());
            if (AutoScalePolicy.Action.SCALEUP.equals(scaleAction)) {
                doScaleUp(asGroup.getId(), 1);
            } else {
                doScaleDown(asGroup.getId());
            }
        }
    }

    protected void getVmStatsFromHosts(AutoScaleVmGroupTO groupTO) {
        // group vms by host id
        Map<Long, List<Long>> hostAndVmIdsMap = getHostAndVmIdsMap(groupTO);

        Map<Long, List<CounterTO>> policyCountersMap = getPolicyCounters(groupTO);

        // get vm stats from each host and update database
        for (Map.Entry<Long, List<Long>> hostAndVmIds : hostAndVmIdsMap.entrySet()) {
            Long hostId = hostAndVmIds.getKey();
            List<Long> vmIds = hostAndVmIds.getValue();

            if (!DEFAULT_HOST_ID.equals(hostId)) {
                Map<Long, ? extends VmStats> vmStatsById = getVmStatsByIdFromHost(hostId, vmIds);
                processVmStatsByIdFromHost(groupTO, vmIds, vmStatsById, policyCountersMap);
            }
        }
    }

    protected Map<Long, ? extends VmStats> getVmStatsByIdFromHost(Long hostId, List<Long> vmIds) {
        Map<Long, ? extends VmStats> vmStatsById = new HashMap<>();
        HostVO host = hostDao.findById(hostId);
        if (host == null) {
            s_logger.debug("Failed to get VM stats from non-existing host : " + hostId);
            return vmStatsById;
        }
        try {
            vmStatsById = virtualMachineManager.getVirtualMachineStatistics(host.getId(), host.getName(), vmIds);
            if (MapUtils.isEmpty(vmStatsById)) {
                s_logger.warn("Got empty result for virtual machine statistics from host: " + host);
            }
        } catch (Exception e) {
            s_logger.debug("Failed to get VM stats from host : " + host.getName());
        }
        return vmStatsById;
    }

    protected void processVmStatsByIdFromHost(AutoScaleVmGroupTO groupTO, List<Long> vmIds, Map<Long, ? extends VmStats> vmStatsById, Map<Long, List<CounterTO>> policyCountersMap) {
        Date timestamp = new Date();
        for (Long vmId : vmIds) {
            VmStatsEntry vmStats = vmStatsById == null ? null : (VmStatsEntry)vmStatsById.get(vmId);
            for (Map.Entry<Long, List<CounterTO>> policyCounters : policyCountersMap.entrySet()) {
                Long policyId = policyCounters.getKey();
                List<CounterTO> counters = policyCounters.getValue();
                for (CounterTO counter : counters) {
                    if (!Counter.NativeSources.contains(counter.getSource())) {
                        continue;
                    }
                    Double counterValue = null;
                    if (vmStats != null) {
                        if (Counter.Source.CPU.equals(counter.getSource())) {
                            counterValue = vmStats.getCPUUtilization() / 100;
                        } else if (Counter.Source.MEMORY.equals(counter.getSource())) {
                            if (vmStats.getIntFreeMemoryKBs() >= 0 && vmStats.getIntFreeMemoryKBs() <= vmStats.getMemoryKBs()) {
                                counterValue = (vmStats.getMemoryKBs() - vmStats.getIntFreeMemoryKBs()) / 1024;
                            } else {
                                // In some scenarios, the free memory is greater than VM memory
                                // see https://github.com/apache/cloudstack/issues/4566
                                s_logger.warn(String.format("Getting virtual machine statistics return invalid free memory KBs for VM %d: %f", vmId, vmStats.getIntFreeMemoryKBs()));
                            }
                        }
                    }
                    if (counterValue == null) {
                        asGroupStatisticsDao.persist(new AutoScaleVmGroupStatisticsVO(groupTO.getId(), policyId, counter.getId(), vmId, ResourceTag.ResourceObjectType.UserVm,
                                AutoScaleValueType.INSTANT_VM, timestamp));
                    } else {
                        asGroupStatisticsDao.persist(new AutoScaleVmGroupStatisticsVO(groupTO.getId(), policyId, counter.getId(), vmId, ResourceTag.ResourceObjectType.UserVm,
                                counterValue, AutoScaleValueType.INSTANT_VM, timestamp));
                    }
                }
            }
        }
    }

    protected void getNetworkStatsFromVirtualRouter(AutoScaleVmGroupTO groupTO) {
        Network network = getNetwork(groupTO.getLoadBalancerId());
        Pair<String, Integer> publicIpAddr = getPublicIpAndPort(groupTO.getLoadBalancerId());
        List<DomainRouterVO> routers = routerDao.listByNetworkAndRole(network.getId(), VirtualRouter.Role.VIRTUAL_ROUTER);
        if (CollectionUtils.isEmpty(routers)) {
            processGetAutoScaleMetricsAnswer(groupTO, new ArrayList<>(), null);
            return;
        }
        List<AutoScaleMetrics> metrics = setGetAutoScaleMetricsCommandMetrics(groupTO);
        for (DomainRouterVO router : routers) {
            if (VirtualMachine.State.Running.equals(router.getState())) {
                final GetAutoScaleMetricsCommand command = new GetAutoScaleMetricsCommand(router.getPrivateIpAddress(), network.getVpcId() != null, publicIpAddr.first(), publicIpAddr.second(), metrics);
                command.setWait(30);
                GetAutoScaleMetricsAnswer answer = (GetAutoScaleMetricsAnswer) agentMgr.easySend(router.getHostId(), command);
                if (answer == null || !answer.getResult()) {
                    s_logger.error("Failed to get autoscale metrics from virtual router " + router.getName());
                    processGetAutoScaleMetricsAnswer(groupTO, new ArrayList<>(), router.getId());
                } else {
                    processGetAutoScaleMetricsAnswer(groupTO, answer.getValues(), router.getId());
                }
            } else {
                processGetAutoScaleMetricsAnswer(groupTO, new ArrayList<>(), router.getId());
            }
        }
    }
    protected List<AutoScaleMetrics> setGetAutoScaleMetricsCommandMetrics(AutoScaleVmGroupTO groupTO) {
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

    protected void processGetAutoScaleMetricsAnswer(AutoScaleVmGroupTO groupTO, List<AutoScaleMetricsValue> values, Long routerId) {
        Date timestamp = new Date();
        Map<Long, List<CounterTO>> policyCountersMap = getPolicyCounters(groupTO);
        for (Map.Entry<Long, List<CounterTO>> policyCounters : policyCountersMap.entrySet()) {
            Long policyId = policyCounters.getKey();
            List<CounterTO> counters = policyCounters.getValue();
            for (CounterTO counter : counters) {
                if (!Counter.Source.VIRTUALROUTER.equals(counter.getSource())) {
                    continue;
                }
                boolean found = false;
                for (AutoScaleMetricsValue value : values) {
                    AutoScaleMetrics metrics = value.getMetrics();
                    if (policyId.equals(metrics.getPolicyId()) && counter.getId().equals(metrics.getCounterId())) {
                        asGroupStatisticsDao.persist(new AutoScaleVmGroupStatisticsVO(groupTO.getId(), policyId, counter.getId(), routerId,
                                ResourceTag.ResourceObjectType.DomainRouter, value.getValue(), value.getType(), timestamp));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    asGroupStatisticsDao.persist(new AutoScaleVmGroupStatisticsVO(groupTO.getId(), policyId, counter.getId(), routerId,
                            ResourceTag.ResourceObjectType.DomainRouter, AutoScaleValueType.INSTANT_VM, timestamp));
                }
            }
        }
    }

    protected boolean updateCountersMap(AutoScaleVmGroupTO groupTO, Map<String, Double> countersMap, Map<String, Integer> countersNumberMap) {
        s_logger.debug("Updating countersMap for as group: " + groupTO.getId());
        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            Date afterDate = new Date(System.currentTimeMillis() - ((long)policyTO.getDuration() << 10));
            List<AutoScaleVmGroupStatisticsVO> dummyStats = asGroupStatisticsDao.listDummyRecordsByVmGroup(groupTO.getId(), afterDate);
            if (CollectionUtils.isNotEmpty(dummyStats)) {
                s_logger.error(String.format("Failed to update counters map as there are %d dummy statistics in as group %d", dummyStats.size(), groupTO.getId()));
                return false;
            }
            List<AutoScaleVmGroupStatisticsVO> inactiveStats = asGroupStatisticsDao.listInactiveByVmGroupAndPolicy(groupTO.getId(), policyTO.getId(), afterDate);
            if (CollectionUtils.isNotEmpty(inactiveStats)) {
                s_logger.error(String.format("Failed to update counters map as there are %d Inactive statistics in as group %d and policy %s", inactiveStats.size(), groupTO.getId(), policyTO.getId()));
                continue;
            }
            for (ConditionTO conditionTO : policyTO.getConditions()) {
                updateCountersMapPerCondition(groupTO, policyTO, conditionTO, afterDate, countersMap, countersNumberMap);
            }
        }
        s_logger.debug("DONE Updating countersMap for as group: " + groupTO.getId());
        return true;
    }

    private void updateCountersMapPerCondition(AutoScaleVmGroupTO groupTO, AutoScalePolicyTO policyTO, ConditionTO conditionTO, Date afterDate,
                                                  Map<String, Double> countersMap, Map<String, Integer> countersNumberMap) {
        Long conditionId = conditionTO.getId();
        CounterTO counter = conditionTO.getCounter();
        List<AutoScaleVmGroupStatisticsVO> stats = asGroupStatisticsDao.listByVmGroupAndPolicyAndCounter(groupTO.getId(), policyTO.getId(), counter.getId(), afterDate);
        if (CollectionUtils.isEmpty(stats)) {
            s_logger.debug(String.format("Skipping updating countersMap for group %s and policy %s and counter %s due to no stats", groupTO.getId(), policyTO.getId(), counter.getId()));
            return;
        }
        s_logger.debug(String.format("Updating countersMap with %d stats for group %s and policy %s and counter %s", stats.size(), groupTO.getId(), policyTO.getId(), counter.getId()));
        Map<String, List<AutoScaleVmGroupStatisticsVO>> aggregatedRecords = new HashMap<>();
        List<String> incorrectRecords = new ArrayList<>();
        for (AutoScaleVmGroupStatisticsVO stat : stats) {
            if (Arrays.asList(AutoScaleValueType.INSTANT_VM, AutoScaleValueType.INSTANT_VM_GROUP).contains(stat.getValueType())) {
                updateCountersMapWithInstantData(countersMap, countersNumberMap, groupTO, counter.getId(), conditionId, policyTO.getId(), stat.getRawValue(), stat.getValueType());
            } else if (Arrays.asList(AutoScaleValueType.AGGREGATED_VM, AutoScaleValueType.AGGREGATED_VM_GROUP).contains(stat.getValueType())) {
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

        updateCountersMapByAggregatedRecords(countersMap, countersNumberMap, aggregatedRecords, conditionId, policyTO.getId(), groupTO.getId());
    }

    public void updateCountersMapByAggregatedRecords(Map<String, Double> countersMap, Map<String, Integer> countersNumberMap,
                                                     Map<String, List<AutoScaleVmGroupStatisticsVO>> aggregatedRecords,
                                                     Long conditionId, Long policyId, Long groupId) {
        if (MapUtils.isNotEmpty(aggregatedRecords)) {
            s_logger.debug("Processing aggregated data");
            for (Map.Entry<String, List<AutoScaleVmGroupStatisticsVO>> aggregatedRecord : aggregatedRecords.entrySet()) {
                String recordKey = aggregatedRecord.getKey();
                Long counterId = Long.valueOf(recordKey.split("-")[0]);
                List<AutoScaleVmGroupStatisticsVO> records = aggregatedRecord.getValue();
                if (records.size() <= 1) {
                    s_logger.info(String.format("Ignoring aggregated records, conditionId = %s, counterId = %s", conditionId, counterId));
                    continue;
                }
                AutoScaleVmGroupStatisticsVO firstRecord = records.get(0);
                AutoScaleVmGroupStatisticsVO lastRecord = records.get(records.size() - 1);
                Double coVal = (lastRecord.getRawValue() - firstRecord.getRawValue()) * 1000 / (lastRecord.getCreated().getTime() - firstRecord.getCreated().getTime());
                if (AutoScaleValueType.AGGREGATED_VM_GROUP.equals(firstRecord.getValueType())) {
                    Integer currentVM = autoScaleVmGroupVmMapDao.countAvailableVmsByGroup(groupId);
                    if (currentVM == 0) {
                        s_logger.debug(String.format("Skipping updating countersMap for group %s and policy %s and counter %s due to no VMs", groupId, policyId, counterId));
                        return;
                    }
                    coVal = coVal / currentVM;
                }
                String key = generateKeyFromPolicyAndConditionAndCounter(policyId, conditionId, counterId);
                updateCountersMapWithProcessedData(countersMap, countersNumberMap, key, coVal);
            }
        }
    }

    private String generateKeyFromPolicyAndConditionAndCounter(Long policyId, Long conditionId, Long counterId) {
        return policyId + "-" + conditionId + "-" + counterId;
    }

    protected void cleanupAsVmGroupStatistics(AutoScaleVmGroupTO groupTO) {
        Integer cleanupDelay = AutoScaleStatsCleanupDelay.value();
        Integer maxDelaySecs = cleanupDelay;
        for (AutoScalePolicyTO policyTO : groupTO.getPolicies()) {
            Integer duration = policyTO.getDuration();
            Integer delaySecs = cleanupDelay >= duration ?  cleanupDelay : duration;
            Date beforeDate = new Date(System.currentTimeMillis() - ((long)delaySecs * 1000));
            s_logger.debug(String.format("Removing stats for policy %d in as group %d, before %s", policyTO.getId(), groupTO.getId(), beforeDate));
            asGroupStatisticsDao.removeByGroupAndPolicy(groupTO.getId(), policyTO.getId(), beforeDate);
            if (delaySecs > maxDelaySecs) {
                maxDelaySecs = delaySecs;
            }
        }
        Date beforeDate = new Date(System.currentTimeMillis() - ((long)maxDelaySecs * 1000));
        s_logger.debug(String.format("Removing stats for other policies in as group %d, before %s", groupTO.getId(), beforeDate));
        asGroupStatisticsDao.removeByGroupId(groupTO.getId(), beforeDate);
    }

    protected void scheduleMonitorTasks() {
        List<AutoScaleVmGroupVO> vmGroups = autoScaleVmGroupDao.listAll();
        for (AutoScaleVmGroupVO vmGroup : vmGroups) {
            if (vmGroup.getState().equals(AutoScaleVmGroup.State.ENABLED)) {
                scheduleMonitorTask(vmGroup.getId());
            }
        }
    }

    protected void scheduleMonitorTask(Long groupId) {
        ScheduledExecutorService vmGroupExecutor = vmGroupMonitorMaps.get(groupId);
        if (vmGroupExecutor == null) {
            AutoScaleVmGroupVO vmGroup = autoScaleVmGroupDao.findById(groupId);
            s_logger.debug("Scheduling monitor task for autoscale vm group " + vmGroup);
            vmGroupExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("VmGroup-Monitor-" + groupId));
            vmGroupExecutor.scheduleWithFixedDelay(new MonitorTask(groupId), vmGroup.getInterval(), vmGroup.getInterval(), TimeUnit.SECONDS);
            vmGroupMonitorMaps.put(groupId, vmGroupExecutor);
        }
    }

    protected void cancelMonitorTask(Long groupId) {
        ScheduledExecutorService vmGroupExecutor = vmGroupMonitorMaps.get(groupId);
        if (vmGroupExecutor != null) {
            s_logger.debug("Cancelling monitor task for autoscale vm group " + groupId);
            vmGroupExecutor.shutdown();
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
                AutoScaleVmGroupVO asGroup = autoScaleVmGroupDao.findById(groupId);
                if (asGroup == null) {
                    s_logger.error("Can not find the groupid " + groupId + " for monitoring");
                    return;
                }
                s_logger.debug("Start monitoring on AutoScale VmGroup " + asGroup);
                // check group state
                if (asGroup.getState().equals(AutoScaleVmGroup.State.ENABLED)) {
                    Network.Provider provider = getLoadBalancerServiceProvider(asGroup.getLoadBalancerId());
                    if (Network.Provider.Netscaler.equals(provider)) {
                        s_logger.debug("Skipping the monitoring on AutoScale VmGroup with Netscaler provider: " + asGroup);
                    } else if (Network.Provider.VirtualRouter.equals(provider) || Network.Provider.VPCVirtualRouter.equals(provider)) {
                        monitorVirtualRouterAsGroup(asGroup);
                    }
                }
            } catch (final Exception e) {
                s_logger.warn("Caught the following exception on monitoring AutoScale Vm Group", e);
            }
        }
    }

    @Override
    public void checkIfVmActionAllowed(Long vmId) {
        List<AutoScaleVmGroupVmMapVO> asGroupVmVOs = autoScaleVmGroupVmMapDao.listByVm(vmId);
        if (CollectionUtils.isNotEmpty(asGroupVmVOs)) {
            for (AutoScaleVmGroupVmMapVO asGroupVmVO : asGroupVmVOs) {
                AutoScaleVmGroupVO group = autoScaleVmGroupDao.findById(asGroupVmVO.getVmGroupId());
                if (group != null && !AutoScaleVmGroup.State.DISABLED.equals(group.getState())) {
                    throw new InvalidParameterValueException("Cannot perform actions on VM in an AutoScale VM group, Please disable the VM group and retry.");
                }
            }
        }
    }

    @Override
    public void removeVmFromVmGroup(Long vmId) {
        autoScaleVmGroupVmMapDao.removeByVm(vmId);
    }

    protected boolean destroyVm(Long vmId) {
        try {
            UserVmVO vm = userVmDao.findById(vmId);
            if (vm != null) {
                userVmMgr.destroyVm(vmId, true);
                userVmMgr.expunge(vm);
            }
            return true;
        } catch (Exception ex) {
            s_logger.error("Cannot destroy vm with id: " + vmId + "due to Exception: ", ex);
            return false;
        }
    }

    private void markStatisticsAsInactive(Long groupId, Long policyId) {
        asGroupStatisticsDao.updateStateByGroup(groupId, policyId, AutoScaleVmGroupStatisticsVO.State.INACTIVE);
    }

    private void createInactiveDummyRecord(Long groupId) {
        asGroupStatisticsDao.createInactiveDummyRecord(groupId);
    }
}
