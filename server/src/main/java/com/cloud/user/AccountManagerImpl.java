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
package com.cloud.user;

import java.net.InetAddress;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.InfrastructureEntity;
import org.apache.cloudstack.acl.QuerySelector;
import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.account.CreateAccountCmd;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.GetUserKeysCmd;
import org.apache.cloudstack.api.command.admin.user.MoveUserCmd;
import org.apache.cloudstack.api.command.admin.user.RegisterCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.api.response.UserTwoFactorAuthenticationSetupResponse;
import org.apache.cloudstack.auth.UserAuthenticator;
import org.apache.cloudstack.auth.UserAuthenticator.ActionOnFailedAuthentication;
import org.apache.cloudstack.auth.UserTwoFactorAuthenticator;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.network.RoutedIpv4Manager;
import org.apache.cloudstack.network.dao.NetworkPermissionDao;
import org.apache.cloudstack.region.gslb.GlobalLoadBalancerRuleDao;
import org.apache.cloudstack.resourcedetail.UserDetailVO;
import org.apache.cloudstack.resourcedetail.dao.UserDetailsDao;
import org.apache.cloudstack.utils.baremetal.BaremetalUtils;
import org.apache.cloudstack.webhook.WebhookHelper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.auth.SetupUserTwoFactorAuthenticationCmd;
import com.cloud.api.query.vo.ControlledViewEntity;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.ResourceLimit;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterVnetDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.ActionEvents;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.exception.CloudTwoFactorAuthenticationException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.dao.HostDao;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.VpnUserVO;
import com.cloud.network.as.AutoScaleManager;
import com.cloud.network.dao.AccountGuestVlanMapDao;
import com.cloud.network.dao.AccountGuestVlanMapVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.RemoteAccessVpnVO;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.SecurityGroupService;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.projects.Project;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.projects.ProjectInvitationVO;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.region.ha.GlobalLoadBalancingRulesService;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account.State;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserDataDao;
import com.cloud.utils.ConstantTimeComparator;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotManager;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class AccountManagerImpl extends ManagerBase implements AccountManager, Manager {

    @Inject
    private AccountDao _accountDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ResourceCountDao _resourceCountDao;
    @Inject
    private UserDao _userDao;
    @Inject
    private UserDetailsDao _userDetailsDao;
    @Inject
    private InstanceGroupDao _vmGroupDao;
    @Inject
    private UserAccountDao _userAccountDao;
    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private SecurityGroupDao _securityGroupDao;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private SecurityGroupManager _networkGroupMgr;
    @Inject
    private NetworkOrchestrationService _networkMgr;
    @Inject
    private SnapshotManager _snapMgr;
    @Inject
    private VMSnapshotManager _vmSnapshotMgr;
    @Inject
    private VMSnapshotDao _vmSnapshotDao;
    @Inject
    private UserVmManager _vmMgr;
    @Inject
    private TemplateManager _tmpltMgr;
    @Inject
    private ConfigurationManager _configMgr;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    private RemoteAccessVpnDao _remoteAccessVpnDao;
    @Inject
    private RemoteAccessVpnService _remoteAccessVpnMgr;
    @Inject
    private VpnUserDao _vpnUser;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private DomainManager _domainMgr;
    @Inject
    private ProjectManager _projectMgr;
    @Inject
    private ProjectDao _projectDao;
    @Inject
    private AccountDetailsDao _accountDetailsDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private ProjectAccountDao _projectAccountDao;
    @Inject
    private IPAddressDao _ipAddressDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private VpcManager _vpcMgr;
    @Inject
    private NetworkModel _networkModel;
    @Inject
    private Site2SiteVpnManager _vpnMgr;
    @Inject
    private AutoScaleManager _autoscaleMgr;
    @Inject
    private VolumeApiService volumeService;
    @Inject
    private AffinityGroupDao _affinityGroupDao;
    @Inject
    private AccountGuestVlanMapDao _accountGuestVlanMapDao;
    @Inject
    private DataCenterVnetDao _dataCenterVnetDao;
    @Inject
    private ResourceLimitService _resourceLimitMgr;
    @Inject
    private ResourceLimitDao _resourceLimitDao;
    @Inject
    private DedicatedResourceDao _dedicatedDao;
    @Inject
    private GlobalLoadBalancerRuleDao _gslbRuleDao;
    @Inject
    private SSHKeyPairDao _sshKeyPairDao;
    @Inject
    private UserDataDao userDataDao;
    @Inject
    private NetworkPermissionDao networkPermissionDao;

    private List<QuerySelector> _querySelectors;

    @Inject
    private MessageBus _messageBus;

    @Inject
    private GlobalLoadBalancingRulesService _gslbService;

    @Inject
    public AccountService _accountService;

    private List<UserAuthenticator> _userAuthenticators;
    private List<UserTwoFactorAuthenticator> _userTwoFactorAuthenticators;
    protected List<UserAuthenticator> _userPasswordEncoders;
    protected List<PluggableService> services;
    private List<APIChecker> apiAccessCheckers;

    @Inject
    private IpAddressManager _ipAddrMgr;
    @Inject
    private RoleService roleService;
    @Inject
    private RoutedIpv4Manager routedIpv4Manager;

    @Inject
    private PasswordPolicy passwordPolicy;

    private final ScheduledExecutorService _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("AccountChecker"));

    private int _allowedLoginAttempts;

    private UserVO _systemUser;
    private AccountVO _systemAccount;

    private List<SecurityChecker> _securityCheckers;
    private int _cleanupInterval;
    private static final String OAUTH2_PROVIDER_NAME = "oauth2";
    private List<String> apiNameList;

    protected static Map<String, UserTwoFactorAuthenticator> userTwoFactorAuthenticationProvidersMap = new HashMap<>();

    private List<UserTwoFactorAuthenticator> userTwoFactorAuthenticationProviders;

    private long validUserLastAuthTimeDurationInMs = 0L;
    private static final long DEFAULT_USER_AUTH_TIME_DURATION_MS = 350L;

    public static ConfigKey<Boolean> enableUserTwoFactorAuthentication = new ConfigKey<>("Advanced",
            Boolean.class,
            "enable.user.2fa",
            "false",
            "Determines whether two factor authentication is enabled or not. This can also be configured at domain level.",
            true,
            ConfigKey.Scope.Domain);

    public static ConfigKey<Boolean> mandateUserTwoFactorAuthentication = new ConfigKey<>("Advanced",
            Boolean.class,
            "mandate.user.2fa",
            "false",
            "Determines whether to make the two factor authentication mandatory or not. This setting is applicable only when enable.user.2fa is true. This can also be configured at domain level.",
            true,
            ConfigKey.Scope.Domain);

    public static final ConfigKey<String> userTwoFactorAuthenticationIssuer = new ConfigKey<>("Advanced",
            String.class,
            "user.2fa.issuer",
            "CloudStack",
            "Name of the issuer of two factor authentication",
            true,
            ConfigKey.Scope.Domain);

    static final ConfigKey<String> userTwoFactorAuthenticationDefaultProvider = new ConfigKey<>("Advanced", String.class,
            "user.2fa.default.provider",
            "totp",
            "The default user two factor authentication provider. Eg. totp, staticpin", true, ConfigKey.Scope.Domain);

    public static final ConfigKey<Boolean> apiKeyAccess = new ConfigKey<>(ConfigKey.CATEGORY_SYSTEM, Boolean.class,
            "api.key.access",
            "true",
            "Determines whether API (api-key/secret-key) access is allowed or not. Editable only by Root Admin.",
            true,
            ConfigKey.Scope.Domain);

    static ConfigKey<Boolean> userAllowMultipleAccounts = new ConfigKey<>("Advanced",
            Boolean.class,
            "user.allow.multiple.accounts",
            "false",
            "Determines if the same username can be added to more than one account in the same domain (SAML-only).",
            true,
            ConfigKey.Scope.Domain);

    public static ConfigKey<String> listOfRoleTypesAllowedForOperationsOfSameRoleType = new ConfigKey<>("Hidden",
            String.class,
            "role.types.allowed.for.operations.on.accounts.of.same.role.type",
            "Admin, ResourceAdmin, DomainAdmin",
            "Comma separated list of role types that are allowed to do operations on accounts or users of the same role type within a domain.",
            true,
            ConfigKey.Scope.Domain);

    public static ConfigKey<Boolean> allowOperationsOnUsersInSameAccount = new ConfigKey<>("Hidden",
            Boolean.class,
            "allow.operations.on.users.in.same.account",
            "true",
            "Allow operations on users among them in the same account",
            true,
            ConfigKey.Scope.Domain);

    protected AccountManagerImpl() {
        super();
    }

    public List<UserAuthenticator> getUserAuthenticators() {
        return _userAuthenticators;
    }

    public void setUserAuthenticators(List<UserAuthenticator> authenticators) {
        _userAuthenticators = authenticators;
    }

    public List<UserTwoFactorAuthenticator> getUserTwoFactorAuthenticators() {
        return _userTwoFactorAuthenticators;
    }

    public void setUserTwoFactorAuthenticators(List<UserTwoFactorAuthenticator> twoFactorAuthenticators) {
        _userTwoFactorAuthenticators = twoFactorAuthenticators;
    }

    public List<UserAuthenticator> getUserPasswordEncoders() {
        return _userPasswordEncoders;
    }

    public void setUserPasswordEncoders(List<UserAuthenticator> encoders) {
        _userPasswordEncoders = encoders;
    }

    public List<SecurityChecker> getSecurityCheckers() {
        return _securityCheckers;
    }

    public void setSecurityCheckers(List<SecurityChecker> securityCheckers) {
        _securityCheckers = securityCheckers;
    }

    public List<PluggableService> getServices() {
        return services;
    }

    public void setServices(List<PluggableService> services) {
        this.services = services;
    }

    public List<APIChecker> getApiAccessCheckers() {
        return apiAccessCheckers;
    }

    public void setApiAccessCheckers(List<APIChecker> apiAccessCheckers) {
        this.apiAccessCheckers = apiAccessCheckers;
    }

    public List<QuerySelector> getQuerySelectors() {
        return _querySelectors;
    }

    public void setQuerySelectors(List<QuerySelector> querySelectors) {
        _querySelectors = querySelectors;
    }

    protected void deleteWebhooksForAccount(long accountId) {
        try {
            WebhookHelper webhookService = ComponentContext.getDelegateComponentOfType(WebhookHelper.class);
            webhookService.deleteWebhooksForAccount(accountId);
        } catch (NoSuchBeanDefinitionException ignored) {
            logger.debug("No WebhookHelper bean found");
        }
    }

    @Override
    public List<String> getApiNameList() {
        return apiNameList;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _systemAccount = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);
        if (_systemAccount == null) {
            throw new ConfigurationException("Unable to find the system account using " + Account.ACCOUNT_ID_SYSTEM);
        }

        _systemUser = _userDao.findById(User.UID_SYSTEM);
        if (_systemUser == null) {
            throw new ConfigurationException("Unable to find the system user using " + User.UID_SYSTEM);
        }

        Map<String, String> configs = _configDao.getConfiguration(params);

        String loginAttempts = configs.get(Config.IncorrectLoginAttemptsAllowed.key());
        _allowedLoginAttempts = NumbersUtil.parseInt(loginAttempts, 5);

        String value = configs.get(Config.AccountCleanupInterval.key());
        _cleanupInterval = NumbersUtil.parseInt(value, 60 * 60 * 24); // 1 day.

        return true;
    }

    @Override
    public UserVO getSystemUser() {
        if (_systemUser == null) {
            _systemUser = _userDao.findById(User.UID_SYSTEM);
        }
        return _systemUser;
    }

    @Override
    public boolean start() {

        initializeUserTwoFactorAuthenticationProvidersMap();

        if (apiNameList == null) {
            long startTime = System.nanoTime();
            apiNameList = new ArrayList<>();
            Set<Class<?>> cmdClasses = new LinkedHashSet<>();
            for (PluggableService service : services) {
                logger.debug(String.format("getting api commands of service: %s", service.getClass().getName()));
                cmdClasses.addAll(service.getCommands());
            }
            apiNameList = createApiNameList(cmdClasses);
            long endTime = System.nanoTime();
            logger.info("Api Discovery Service: Annotation, docstrings, api relation graph processed in " + (endTime - startTime) / 1000000.0 + " ms");
        }
        _executor.scheduleAtFixedRate(new AccountCleanupTask(), _cleanupInterval, _cleanupInterval, TimeUnit.SECONDS);
        return true;
    }

    protected List<String> createApiNameList(Set<Class<?>> cmdClasses) {
        List<String> apiNameList = new ArrayList<>();

        for (Class<?> cmdClass : cmdClasses) {
            APICommand apiCmdAnnotation = cmdClass.getAnnotation(APICommand.class);
            if (apiCmdAnnotation == null) {
                apiCmdAnnotation = cmdClass.getSuperclass().getAnnotation(APICommand.class);
            }
            if (apiCmdAnnotation == null || !apiCmdAnnotation.includeInApiDoc() || apiCmdAnnotation.name().isEmpty()) {
                continue;
            }

            String apiName = apiCmdAnnotation.name();
            if (logger.isTraceEnabled()) {
                logger.trace("Found api: " + apiName);
            }

            apiNameList.add(apiName);
        }

        return apiNameList;
    }


    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public AccountVO getSystemAccount() {
        if (_systemAccount == null) {
            _systemAccount = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);
        }
        return _systemAccount;
    }

    @Override
    public boolean isAdmin(Long accountId) {
        if (accountId != null) {
            AccountVO acct = _accountDao.findById(accountId);
            if (acct == null) {
                return false;  //account is deleted or does not exist
            }
            if ((isRootAdmin(accountId)) || (isDomainAdmin(accountId)) || (isResourceDomainAdmin(accountId))) {
                return true;
            } else if (acct.getType() == Account.Type.READ_ONLY_ADMIN) {
                return true;
            }

        }
        return false;
    }

    @Override
    public boolean isRootAdmin(Long accountId) {
        if (accountId != null) {
            AccountVO acct = _accountDao.findById(accountId);
            if (acct == null) {
                return false;  //account is deleted or does not exist
            }
            for (SecurityChecker checker : _securityCheckers) {
                try {
                    if (checker.checkAccess(acct, null, null, "SystemCapability")) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Root Access granted to " + acct + " by " + checker.getName());
                        }
                        return true;
                    }
                } catch (PermissionDeniedException ex) {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isDomainAdmin(Long accountId) {
        if (accountId != null) {
            AccountVO acct = _accountDao.findById(accountId);
            if (acct == null) {
                return false;  //account is deleted or does not exist
            }
            for (SecurityChecker checker : _securityCheckers) {
                try {
                    if (checker.checkAccess(acct, null, null, "DomainCapability")) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("DomainAdmin Access granted to " + acct + " by " + checker.getName());
                        }
                        return true;
                    }
                } catch (PermissionDeniedException ex) {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isNormalUser(long accountId) {
        AccountVO acct = _accountDao.findById(accountId);
        if (acct != null && acct.getType() == Account.Type.NORMAL) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isResourceDomainAdmin(Long accountId) {
        if (accountId != null) {
            AccountVO acct = _accountDao.findById(accountId);
            if (acct == null) {
                return false;  //account is deleted or does not exist
            }
            for (SecurityChecker checker : _securityCheckers) {
                try {
                    if (checker.checkAccess(acct, null, null, "DomainResourceCapability")) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("ResourceDomainAdmin Access granted to " + acct + " by " + checker.getName());
                        }
                        return true;
                    }
                } catch (PermissionDeniedException ex) {
                    return false;
                }
            }
        }
        return false;
    }

    public boolean isInternalAccount(long accountId) {
        Account account = _accountDao.findById(accountId);
        if (account == null) {
            return false;  //account is deleted or does not exist
        }
        if (isRootAdmin(accountId) || (account.getType() == Account.Type.ADMIN)) {
            return true;
        }
        return false;
    }

    @Override
    public void checkAccess(Account caller, Domain domain) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(caller, domain)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Access granted to " + caller + " to " + domain + " by " + checker.getName());
                }
                return;
            }
        }
        throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to " + domain);
    }

    @Override
    public void checkAccess(Account caller, AccessType accessType, boolean sameOwner, ControlledEntity... entities) {
        checkAccess(caller, accessType, sameOwner, null, entities);
    }

    @Override
    public void checkAccess(Account caller, AccessType accessType, boolean sameOwner, String apiName, ControlledEntity... entities) {

        //check for the same owner
        Long ownerId = null;
        ControlledEntity prevEntity = null;
        if (sameOwner) {
            for (ControlledEntity entity : entities) {
                if (ownerId == null) {
                    ownerId = entity.getAccountId();
                } else if (ownerId.longValue() != entity.getAccountId()) {
                    throw new PermissionDeniedException("Entity " + entity + " and entity " + prevEntity + " belong to different accounts");
                }
                prevEntity = entity;
            }
        }

        if (caller.getId() == Account.ACCOUNT_ID_SYSTEM || isRootAdmin(caller.getId())) {
            // no need to make permission checks if the system/root admin makes the call
            if (logger.isTraceEnabled()) {
                logger.trace("No need to make permission check for System/RootAdmin account, returning true");
            }

            return;
        }

        HashMap<Long, List<ControlledEntity>> domains = new HashMap<>();

        for (ControlledEntity entity : entities) {
            long domainId = entity.getDomainId();
            if (entity.getAccountId() != -1 && domainId == -1) { // If account exists domainId should too so calculate
                // it. This condition might be hit for templates or entities which miss domainId in their tables
                Account account = ApiDBUtils.findAccountById(entity.getAccountId());
                domainId = account != null ? account.getDomainId() : -1;
            }
            if (entity.getAccountId() != -1 && domainId != -1 && !(entity instanceof VirtualMachineTemplate)
                    && !(entity instanceof Network && accessType != null && (accessType == AccessType.UseEntry || accessType == AccessType.OperateEntry))
                    && !(entity instanceof AffinityGroup) && !(entity instanceof VirtualRouter)) {
                List<ControlledEntity> toBeChecked = domains.get(entity.getDomainId());
                // for templates, we don't have to do cross domains check
                if (toBeChecked == null) {
                    toBeChecked = new ArrayList<>();
                    domains.put(domainId, toBeChecked);
                }
                toBeChecked.add(entity);
            }
            boolean granted = false;
            for (SecurityChecker checker : _securityCheckers) {
                if (checker.checkAccess(caller, entity, accessType, apiName)) {
                    if (logger.isDebugEnabled()) {
                        User user = CallContext.current().getCallingUser();
                        String userName = "";
                        if (user != null)
                            userName = user.getUsername();
                        logger.debug("Access to {} granted to {} by {} on behalf of user {}", entity, caller, checker.getName(), userName);
                    }
                    granted = true;
                    break;
                }
            }

            if (!granted) {
                assert false : "How can all of the security checkers pass on checking this check: " + entity;
            throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to " + entity);
            }
        }

        for (Map.Entry<Long, List<ControlledEntity>> domain : domains.entrySet()) {
            for (SecurityChecker checker : _securityCheckers) {
                Domain d = _domainMgr.getDomain(domain.getKey());
                if (d == null || d.getRemoved() != null) {
                    throw new PermissionDeniedException("Domain is not found.", caller, domain.getValue());
                }
                try {
                    checker.checkAccess(caller, d);
                } catch (PermissionDeniedException e) {
                    e.addDetails(caller, domain.getValue());
                    throw e;
                }
            }
        }

        // check that resources belong to the same account

    }

    @Override
    public void validateAccountHasAccessToResource(Account account, AccessType accessType, Object resource) {
        Class<?> resourceClass = resource.getClass();
        if (ControlledEntity.class.isAssignableFrom(resourceClass)) {
            checkAccess(account, accessType, true, (ControlledEntity) resource);
        } else if (Domain.class.isAssignableFrom(resourceClass)) {
            checkAccess(account, (Domain) resource);
        } else if (InfrastructureEntity.class.isAssignableFrom(resourceClass)) {
            logger.trace("Validation of access to infrastructure entity has been disabled in CloudStack version 4.4.");
        }
        logger.debug("Account [{}] has access to resource.", account);
    }

    @Override
    public Long checkAccessAndSpecifyAuthority(Account caller, Long zoneId) {
        // We just care for resource domain admins for now, and they should be permitted to see only their zone.
        if (isResourceDomainAdmin(caller.getAccountId())) {
            if (zoneId == null) {
                return getZoneIdForAccount(caller);
            } else if (zoneId.compareTo(getZoneIdForAccount(caller)) != 0) {
                throw new PermissionDeniedException("Caller " + caller + "is not allowed to access the zone " + zoneId);
            } else {
                return zoneId;
            }
        } else {
            return zoneId;
        }
    }

    private Long getZoneIdForAccount(Account account) {

        // Currently just for resource domain admin
        List<DataCenterVO> dcList = _dcDao.findZonesByDomainId(account.getDomainId());
        if (dcList != null && dcList.size() != 0) {
            return dcList.get(0).getId();
        } else {
            throw new CloudRuntimeException("Failed to find any private zone for Resource domain admin.");
        }

    }

    @DB
    public void updateLoginAttempts(final Long id, final int attempts, final boolean toDisable) {
        try {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    UserAccountVO user = null;
                    user = _userAccountDao.lockRow(id, true);
                    user.setLoginAttempts(attempts);
                    if (toDisable) {
                        user.setState(State.DISABLED.toString());
                    }
                    _userAccountDao.update(id, user);
                }
            });
        } catch (Exception e) {
            logger.error("Failed to update login attempts for user {}", () -> _userAccountDao.findById(id));
        }
    }

    private boolean doSetUserStatus(long userId, State state) {
        UserVO userForUpdate = _userDao.createForUpdate();
        userForUpdate.setState(state);
        return _userDao.update(Long.valueOf(userId), userForUpdate);
    }

    @Override
    public boolean enableAccount(long accountId) {
        boolean success = false;
        AccountVO acctForUpdate = _accountDao.createForUpdate();
        acctForUpdate.setState(State.ENABLED);
        acctForUpdate.setNeedsCleanup(false);
        success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);
        return success;
    }

    protected boolean lockAccount(long accountId) {
        boolean success = false;
        Account account = _accountDao.findById(accountId);
        if (account != null) {
            if (account.getState().equals(State.LOCKED)) {
                return true; // already locked, no-op
            } else if (account.getState().equals(State.ENABLED)) {
                AccountVO acctForUpdate = _accountDao.createForUpdate();
                acctForUpdate.setState(State.LOCKED);
                success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("Attempting to lock a non-enabled account {}, current state is {}, locking failed.", account, account.getState());
                }
            }
        } else {
            logger.warn("Failed to lock account " + accountId + ", account not found.");
        }
        return success;
    }

    @Override
    public boolean deleteAccount(AccountVO account, long callerUserId, Account caller) {
        long accountId = account.getId();

        // delete the account record
        if (!_accountDao.remove(accountId)) {
            logger.error("Unable to delete account {}", account);
            return false;
        }

        account.setState(State.REMOVED);
        _accountDao.update(accountId, account);

        if (logger.isDebugEnabled()) {
            logger.debug("Removed account {}", account);
        }

        return cleanupAccount(account, callerUserId, caller);
    }

    protected void cleanupPluginsResourcesIfNeeded(Account account) {
        try {
            KubernetesServiceHelper kubernetesServiceHelper =
                    ComponentContext.getDelegateComponentOfType(KubernetesServiceHelper.class);
            kubernetesServiceHelper.cleanupForAccount(account);
        } catch (NoSuchBeanDefinitionException ignored) {
            logger.debug("No KubernetesServiceHelper bean found");
        }
    }

    protected boolean cleanupAccount(AccountVO account, long callerUserId, Account caller) {
        long accountId = account.getId();
        boolean accountCleanupNeeded = false;

        try {
            // cleanup the users from the account
            List<UserVO> users = _userDao.listByAccount(accountId);
            for (UserVO user : users) {
                if (!_userDao.remove(user.getId())) {
                    logger.error("Unable to delete user: " + user + " as a part of account " + account + " cleanup");
                    accountCleanupNeeded = true;
                }
            }

            // delete autoscaling VM groups
            if (!_autoscaleMgr.deleteAutoScaleVmGroupsByAccount(account)) {
                accountCleanupNeeded = true;
            }


            // delete global load balancer rules for the account.
            List<org.apache.cloudstack.region.gslb.GlobalLoadBalancerRuleVO> gslbRules = _gslbRuleDao.listByAccount(accountId);
            if (gslbRules != null && !gslbRules.isEmpty()) {
                _gslbService.revokeAllGslbRulesForAccount(caller, account);
            }

            // delete the account from project accounts
            _projectAccountDao.removeAccountFromProjects(accountId);

            // Delete account's network permissions
            networkPermissionDao.removeAccountPermissions(accountId);

            if (account.getType() != Account.Type.PROJECT) {
                // delete the account from group
                _messageBus.publish(_name, MESSAGE_REMOVE_ACCOUNT_EVENT, PublishScope.LOCAL, accountId);
            }

            // delete all vm groups belonging to account
            List<InstanceGroupVO> groups = _vmGroupDao.listByAccountId(accountId);
            for (InstanceGroupVO group : groups) {
                if (!_vmMgr.deleteVmGroup(group.getId())) {
                    logger.error("Unable to delete group: {}", group);
                    accountCleanupNeeded = true;
                }
            }

            // Delete the snapshots dir for the account. Have to do this before destroying the VMs.
            boolean success = _snapMgr.deleteSnapshotDirsForAccount(account);
            if (success) {
                logger.debug("Successfully deleted snapshots directories for all volumes under account {} across all zones", account);
            }

            // clean up templates
            List<VMTemplateVO> userTemplates = _templateDao.listByAccountId(accountId);
            boolean allTemplatesDeleted = true;
            for (VMTemplateVO template : userTemplates) {
                if (template.getRemoved() == null) {
                    try {
                        allTemplatesDeleted = _tmpltMgr.delete(callerUserId, template.getId(), null);
                    } catch (Exception e) {
                        logger.warn("Failed to delete template {} while removing account {} due to: ", template, account, e);
                        allTemplatesDeleted = false;
                    }
                }
            }

            if (!allTemplatesDeleted) {
                logger.warn("Failed to delete templates while removing account {}", account);
                accountCleanupNeeded = true;
            }

            // Destroy VM Snapshots
            List<VMSnapshotVO> vmSnapshots = _vmSnapshotDao.listByAccountId(Long.valueOf(accountId));
            for (VMSnapshot vmSnapshot : vmSnapshots) {
                try {
                    _vmSnapshotMgr.deleteVMSnapshot(vmSnapshot.getId());
                } catch (Exception e) {
                    logger.debug("Failed to cleanup vm snapshot {} due to {}", vmSnapshot, e.toString());
                }
            }

            cleanupPluginsResourcesIfNeeded(account);

            // Destroy the account's VMs
            List<UserVmVO> vms = _userVmDao.listByAccountId(accountId);
            if (logger.isDebugEnabled()) {
                logger.debug("Expunging # of vms (account={}): {}", account, vms.size());
            }

            for (UserVmVO vm : vms) {
                if (vm.getState() != VirtualMachine.State.Destroyed && vm.getState() != VirtualMachine.State.Expunging) {
                    try {
                        _vmMgr.destroyVm(vm.getId(), false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.warn("Failed destroying instance {} as part of account deletion.", vm);
                    }
                }
                // no need to catch exception at this place as expunging vm
                // should pass in order to perform further cleanup
                if (!_vmMgr.expunge(vm)) {
                    logger.error("Unable to expunge vm: {}", vm);
                    accountCleanupNeeded = true;
                }
            }

            // Mark the account's volumes as destroyed
            List<VolumeVO> volumes = _volumeDao.findDetachedByAccount(accountId);
            for (VolumeVO volume : volumes) {
                try {
                    volumeService.deleteVolume(volume.getId(), caller);
                } catch (Exception ex) {
                    logger.warn("Failed to cleanup volumes as a part of account {} cleanup due to Exception: ", account, ex);
                    accountCleanupNeeded = true;
                }
            }

            // delete remote access vpns and associated users
            List<RemoteAccessVpnVO> remoteAccessVpns = _remoteAccessVpnDao.findByAccount(accountId);
            List<VpnUserVO> vpnUsers = _vpnUser.listByAccount(accountId);

            for (VpnUserVO vpnUser : vpnUsers) {
                _remoteAccessVpnMgr.removeVpnUser(account, vpnUser.getUsername(), caller);
            }

            try {
                for (RemoteAccessVpnVO vpn : remoteAccessVpns) {
                    _remoteAccessVpnMgr.destroyRemoteAccessVpnForIp(vpn.getServerAddressId(), caller, false);
                }
            } catch (ResourceUnavailableException ex) {
                logger.warn("Failed to cleanup remote access vpn resources as a part of account {} cleanup due to Exception: ", account, ex);
                accountCleanupNeeded = true;
            }

            // Cleanup tungsten security groups
            List<SecurityGroupVO> securityGroupList = _securityGroupDao.listByAccountId(accountId);
            for(SecurityGroupVO securityGroupVO : securityGroupList) {
                _messageBus.publish(_name, SecurityGroupService.MESSAGE_DELETE_TUNGSTEN_SECURITY_GROUP_EVENT, PublishScope.LOCAL, securityGroupVO);
            }

            // Cleanup security groups
            int numRemoved = _securityGroupDao.removeByAccountId(accountId);
            logger.info("deleteAccount: Deleted {} network groups for account {}", numRemoved, account);

            // Cleanup affinity groups
            int numAGRemoved = _affinityGroupDao.removeByAccountId(accountId);
            logger.info("deleteAccount: Deleted {} affinity groups for account {}", numAGRemoved, account);

            // Delete all the networks
            boolean networksDeleted = true;
            logger.debug("Deleting networks for account {}", account);
            List<NetworkVO> networks = _networkDao.listByOwner(accountId);
            if (networks != null) {
                Collections.sort(networks, new Comparator<>() {
                    @Override
                    public int compare(NetworkVO network1, NetworkVO network2) {
                        if (network1.getGuestType() != network2.getGuestType() && Network.GuestType.Isolated.equals(network2.getGuestType())) {
                            return -1;
                        }
                        return 1;
                    }
                });
                for (NetworkVO network : networks) {
                    if (_networkModel.isPrivateGateway(network.getId())) {
                        continue;
                    }

                    ReservationContext context = new ReservationContextImpl(null, null, getActiveUser(callerUserId), caller);

                    if (!_networkMgr.destroyNetwork(network.getId(), context, false)) {
                        logger.warn("Unable to destroy network {} as a part of account {} cleanup.", network, account);
                        accountCleanupNeeded = true;
                        networksDeleted = false;
                    } else {
                        logger.debug("Network {} successfully deleted as a part of account {} cleanup.", network, account);
                    }
                }
            }

            // Delete all VPCs
            boolean vpcsDeleted = true;
            logger.debug("Deleting vpcs for account {}", account);
            List<? extends Vpc> vpcs = _vpcMgr.getVpcsForAccount(account.getId());
            for (Vpc vpc : vpcs) {

                if (!_vpcMgr.destroyVpc(vpc, caller, callerUserId)) {
                    logger.warn("Unable to destroy VPC {} as a part of account {} cleanup.", vpc, account);
                    accountCleanupNeeded = true;
                    vpcsDeleted = false;
                } else {
                    logger.debug("VPC {} successfully deleted as a part of account {} cleanup.", vpc, account);
                }
            }

            if (networksDeleted && vpcsDeleted) {
                // release ip addresses belonging to the account
                List<? extends IpAddress> ipsToRelease = _ipAddressDao.listByAccount(accountId);
                for (IpAddress ip : ipsToRelease) {
                    logger.debug("Releasing ip {} as a part of account {} cleanup", ip, account);
                    if (!_ipAddrMgr.disassociatePublicIpAddress(ip, callerUserId, caller)) {
                        logger.warn("Failed to release ip address {} as a part of account {} cleanup", ip, account);
                        accountCleanupNeeded = true;
                    }
                }
            }

            // Delete Site 2 Site VPN customer gateway
            logger.debug("Deleting site-to-site VPN customer gateways for account {}", account);
            if (!_vpnMgr.deleteCustomerGatewayByAccount(accountId)) {
                logger.warn("Fail to delete site-to-site VPN customer gateways for account {}", account);
            }

            // Delete autoscale resources if any
            try {
                _autoscaleMgr.cleanUpAutoScaleResources(account);
            } catch (CloudRuntimeException ex) {
                logger.warn("Failed to cleanup AutoScale resources as a part of account {} cleanup due to exception:", account, ex);
                accountCleanupNeeded = true;
            }

            // release account specific Virtual vlans (belong to system Public Network) - only when networks are cleaned
            // up successfully
            if (networksDeleted) {
                if (!_configMgr.releaseAccountSpecificVirtualRanges(account)) {
                    accountCleanupNeeded = true;
                } else {
                    logger.debug("Account specific Virtual IP ranges are successfully released as a part of account {} cleanup.", account);
                }
            }

            // remove dedicated IPv4 subnets
            routedIpv4Manager.removeIpv4SubnetsForZoneByAccountId(accountId);

            // remove dedicated BGP peers
            routedIpv4Manager.removeBgpPeersByAccountId(accountId);

            // release account specific guest vlans
            List<AccountGuestVlanMapVO> maps = _accountGuestVlanMapDao.listAccountGuestVlanMapsByAccount(accountId);
            for (AccountGuestVlanMapVO map : maps) {
                _dataCenterVnetDao.releaseDedicatedGuestVlans(map.getId());
            }
            int vlansReleased = _accountGuestVlanMapDao.removeByAccountId(accountId);
            logger.info("deleteAccount: Released {} dedicated guest vlan ranges from account {}", vlansReleased, account);

            // release account specific acquired portable IP's. Since all the portable IP's must have been already
            // disassociated with VPC/guest network (due to deletion), so just mark portable IP as free.
            List<? extends IpAddress> ipsToRelease = _ipAddressDao.listByAccount(accountId);
            for (IpAddress ip : ipsToRelease) {
                if (ip.isPortable()) {
                    logger.debug("Releasing portable ip {} as a part of account {} cleanup", ip, account);
                    _ipAddrMgr.releasePortableIpAddress(ip.getId());
                }
            }

            // release dedication if any
            List<DedicatedResourceVO> dedicatedResources = _dedicatedDao.listByAccountId(accountId);
            if (dedicatedResources != null && !dedicatedResources.isEmpty()) {
                logger.debug("Releasing dedicated resources for account {}", account);
                for (DedicatedResourceVO dr : dedicatedResources) {
                    if (!_dedicatedDao.remove(dr.getId())) {
                        logger.warn("Fail to release dedicated resources for account {}", account);
                    }
                }
            }

            // Updating and deleting the resourceLimit and resourceCount should be the last step in cleanupAccount
// process.
            // Update resource count for this account and for parent domains.
            List<ResourceCountVO> resourceCounts = _resourceCountDao.listByOwnerId(accountId, ResourceOwnerType.Account);
            for (ResourceCountVO resourceCount : resourceCounts) {
                _resourceLimitMgr.decrementResourceCount(accountId, resourceCount.getType(), resourceCount.getCount());
            }

            // Delete resource count and resource limits entries set for this account (if there are any).
            _resourceCountDao.removeEntriesByOwner(accountId, ResourceOwnerType.Account);
            _resourceLimitDao.removeEntriesByOwner(accountId, ResourceOwnerType.Account);

            // Delete ssh keypairs
            List<SSHKeyPairVO> sshkeypairs = _sshKeyPairDao.listKeyPairs(accountId, account.getDomainId());
            for (SSHKeyPairVO keypair : sshkeypairs) {
                _sshKeyPairDao.remove(keypair.getId());
            }

            // Delete registered UserData
            userDataDao.removeByAccountId(accountId);

            // Delete Webhooks
            deleteWebhooksForAccount(accountId);

            return true;
        } catch (Exception ex) {
            logger.warn("Failed to cleanup account " + account + " due to ", ex);
            accountCleanupNeeded = true;
            return true;
        } finally {
            logger.info("Cleanup for account {} {}", account, accountCleanupNeeded ? "is needed." : "is not needed.");
            if (accountCleanupNeeded) {
                _accountDao.markForCleanup(accountId);
            } else {
                account.setNeedsCleanup(false);
                _accountDao.update(accountId, account);
            }
        }
    }

    @Override
    public boolean disableAccount(long accountId) throws ConcurrentOperationException, ResourceUnavailableException {
        boolean success = false;
        if (accountId <= 2) {
            if (logger.isInfoEnabled()) {
                logger.info("disableAccount -- invalid account id: " + accountId);
            }
            return false;
        }

        AccountVO account = _accountDao.findById(accountId);
        if ((account == null) || (account.getState().equals(State.DISABLED) && !account.getNeedsCleanup())) {
            success = true;
        } else {
            AccountVO acctForUpdate = _accountDao.createForUpdate();
            acctForUpdate.setState(State.DISABLED);
            success = _accountDao.update(Long.valueOf(accountId), acctForUpdate);

            if (success) {
                boolean disableAccountResult = false;
                try {
                    disableAccountResult = doDisableAccount(accountId);
                } finally {
                    if (!disableAccountResult) {
                        logger.warn("Failed to disable account " + account + " resources as a part of disableAccount call, marking the account for cleanup");
                        _accountDao.markForCleanup(accountId);
                    } else {
                        acctForUpdate = _accountDao.createForUpdate();
                        account.setNeedsCleanup(false);
                        _accountDao.update(accountId, account);
                    }
                }
            }
        }
        return success;
    }

    private boolean doDisableAccount(long accountId) throws ConcurrentOperationException, ResourceUnavailableException {
        List<VMInstanceVO> vms = _vmDao.listByAccountId(accountId);
        boolean success = true;
        for (VMInstanceVO vm : vms) {
            try {
                try {
                    _itMgr.advanceStop(vm.getUuid(), false);
                } catch (OperationTimedoutException ote) {
                    logger.warn("Operation for stopping vm timed out, unable to stop vm {}", vm, ote);
                    success = false;
                }
            } catch (AgentUnavailableException aue) {
                logger.warn("Agent running on host {} is unavailable, unable to stop vm {}", () -> hostDao.findById(vm.getHostId()), vm::toString, () -> aue);
                success = false;
            }
        }

        return success;
    }

    @Override
    @ActionEvents({@ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_CREATE, eventDescription = "creating Account"),
            @ActionEvent(eventType = EventTypes.EVENT_USER_CREATE, eventDescription = "creating User")})
    public UserAccount createUserAccount(CreateAccountCmd accountCmd) {
        return createUserAccount(accountCmd.getUsername(), accountCmd.getPassword(), accountCmd.getFirstName(),
                accountCmd.getLastName(), accountCmd.getEmail(), accountCmd.getTimeZone(), accountCmd.getAccountName(),
                accountCmd.getAccountType(), accountCmd.getRoleId(), accountCmd.getDomainId(),
                accountCmd.getNetworkDomain(), accountCmd.getDetails(), accountCmd.getAccountUUID(),
                accountCmd.getUserUUID(), User.Source.UNKNOWN);
    }

    // ///////////////////////////////////////////////////
    // ////////////// API commands /////////////////////
    // ///////////////////////////////////////////////////

    @Override
    @DB
    @ActionEvents({@ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_CREATE, eventDescription = "creating Account"),
        @ActionEvent(eventType = EventTypes.EVENT_USER_CREATE, eventDescription = "creating User")})
    public UserAccount createUserAccount(final String userName, final String password, final String firstName,
                                         final String lastName, final String email, final String timezone,
                                         String accountName, final Account.Type accountType, final Long roleId, Long domainId,
                                         final String networkDomain, final Map<String, String> details,
                                         String accountUUID, final String userUUID, final User.Source source) {

        if (accountName == null) {
            accountName = userName;
        }
        if (domainId == null) {
            domainId = Domain.ROOT_DOMAIN;
        }

        if (StringUtils.isEmpty(userName)) {
            throw new InvalidParameterValueException("Username is empty");
        }

        if (StringUtils.isEmpty(firstName)) {
            throw new InvalidParameterValueException("Firstname is empty");
        }

        if (StringUtils.isEmpty(lastName)) {
            throw new InvalidParameterValueException("Lastname is empty");
        }

        // Validate domain
        Domain domain = _domainMgr.getDomain(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("The domain " + domainId + " does not exist; unable to create account");
        }

        // Check permissions
        checkAccess(getCurrentCallingAccount(), domain);

        if (!userAllowMultipleAccounts.valueInScope(ConfigKey.Scope.Domain, domainId) && !_userAccountDao.validateUsernameInDomain(userName, domainId)) {
            throw new InvalidParameterValueException(String.format("The user %s already exists in domain %s", userName, domain));
        }

        if (networkDomain != null && networkDomain.length() > 0) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        final String accountNameFinal = accountName;
        final Long domainIdFinal = domainId;
        final String accountUUIDFinal = accountUUID;
        Pair<Long, Account> pair = Transaction.execute(new TransactionCallback<>() {
            @Override
            public Pair<Long, Account> doInTransaction(TransactionStatus status) {
                // create account
                String accountUUID = accountUUIDFinal;
                if (accountUUID == null) {
                    accountUUID = UUID.randomUUID().toString();
                }
                AccountVO account = createAccount(accountNameFinal, accountType, roleId, domainIdFinal, networkDomain, details, accountUUID);
                long accountId = account.getId();

                checkRoleEscalation(getCurrentCallingAccount(), account);

                // create the first user for the account
                UserVO user = createUser(accountId, userName, password, firstName, lastName, email, timezone, userUUID, source);

                if (accountType == Account.Type.RESOURCE_DOMAIN_ADMIN) {
                    // set registration token
                    byte[] bytes = (domainIdFinal + accountNameFinal + userName + System.currentTimeMillis()).getBytes();
                    String registrationToken = UUID.nameUUIDFromBytes(bytes).toString();
                    user.setRegistrationToken(registrationToken);
                }

                return new Pair<>(user.getId(), account);
            }
        });

        long userId = pair.first();
        Account account = pair.second();

        // create correct account and group association based on accountType
        if (accountType != Account.Type.PROJECT) {
            Map<Long, Long> accountGroupMap = new HashMap<>();
            accountGroupMap.put(account.getId(), (long) (accountType.ordinal() + 1));
            _messageBus.publish(_name, MESSAGE_ADD_ACCOUNT_EVENT, PublishScope.LOCAL, accountGroupMap);
        }

        CallContext.current().putContextParameter(Account.class, account.getUuid());
        CallContext.current().putContextParameter(User.class, userId);

        // check success
        return _userAccountDao.findById(userId);
    }

    /*
     Role change should follow the below conditions:
     - Caller should not be of Unknown role type
     - New role's type should not be Unknown
     - Caller should not be able to escalate or de-escalate an account's role which is of higher role type
     - New role should not be of type Admin with domain other than ROOT domain
     */
    protected void validateRoleChange(Account account, Role role, Account caller) {
        Role currentRole = roleService.findRole(account.getRoleId());
        Role callerRole = roleService.findRole(caller.getRoleId());
        String errorMsg = String.format("Unable to update account role to %s, ", role.getName());
        if (RoleType.Unknown.equals(callerRole.getRoleType())) {
            throw new PermissionDeniedException(String.format("%s as the caller privileges are unknown", errorMsg));
        }
        if (RoleType.Unknown.equals(role.getRoleType())) {
            throw new PermissionDeniedException(String.format("%s as the new role privileges are unknown", errorMsg));
        }
        if (!callerRole.getRoleType().equals(RoleType.Admin) &&
                (role.getRoleType().ordinal() < callerRole.getRoleType().ordinal() ||
                        currentRole.getRoleType().ordinal() < callerRole.getRoleType().ordinal())) {
            throw new PermissionDeniedException(String.format("%s as either current or new role has higher " +
                    "privileges than the caller", errorMsg));
        }
        if (role.getRoleType().equals(RoleType.Admin) && account.getDomainId() != Domain.ROOT_DOMAIN) {
            throw new PermissionDeniedException(String.format("%s as the user does not belong to the ROOT domain",
                    errorMsg));
        }
    }

    /**
     * if there is any permission under the requested role that is not permitted for the caller, refuse
     */
    protected void checkRoleEscalation(Account caller, Account requested) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Checking if user of account %s [%s] with role-id [%d] can create an account of type %s [%s] with role-id [%d]",
                    caller.getAccountName(),
                    caller.getUuid(),
                    caller.getRoleId(),
                    requested.getAccountName(),
                    requested.getUuid(),
                    requested.getRoleId()));
        }
        List<APIChecker> apiCheckers = getEnabledApiCheckers();
        for (String command : apiNameList) {
            try {
                checkApiAccess(apiCheckers, requested, command);
            } catch (PermissionDeniedException pde) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format(
                            "Checking for permission to \"%s\" is irrelevant as it is not requested for %s [%s]",
                            command,
                            requested.getAccountName(),
                            requested.getUuid()
                        )
                    );
                }
                continue;
            }
            // so requested can, now make sure caller can as well
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("permission to \"%s\" is requested",
                            command));
                }
                checkApiAccess(apiCheckers, caller, command);
            } catch (PermissionDeniedException pde) {
                String msg = String.format("User of Account %s and domain %s can not create an account with access to more privileges they have themself.",
                        caller, _domainMgr.getDomain(caller.getDomainId()));
                logger.warn(msg);
                throw new PermissionDeniedException(msg,pde);
            }
        }
    }

    private void checkApiAccess(List<APIChecker> apiCheckers, Account caller, String command) {
        for (final APIChecker apiChecker : apiCheckers) {
            apiChecker.checkAccess(caller, command);
        }
    }

    @Override
    public void checkApiAccess(Account caller, String command) {
        List<APIChecker> apiCheckers = getEnabledApiCheckers();
        checkApiAccess(apiCheckers, caller, command);
    }

    @NotNull
    private List<APIChecker> getEnabledApiCheckers() {
        // we are really only interested in the dynamic access checker
        List<APIChecker> usableApiCheckers = new ArrayList<>();
        for (APIChecker apiChecker : apiAccessCheckers) {
            if (apiChecker.isEnabled()) {
                usableApiCheckers.add(apiChecker);
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("using api checker \"%s\"",
                            apiChecker.getName()));
                }
            }
        }
        return usableApiCheckers;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_CREATE, eventDescription = "creating User")
    public UserVO createUser(String userName, String password, String firstName, String lastName, String email, String timeZone, String accountName, Long domainId, String userUUID,
            User.Source source) {
        // default domain to ROOT if not specified
        if (domainId == null) {
            domainId = Domain.ROOT_DOMAIN;
        }

        Domain domain = _domainMgr.getDomain(domainId);
        if (domain == null) {
            throw new CloudRuntimeException("The domain " + domainId + " does not exist; unable to create user");
        } else if (domain.getState().equals(Domain.State.Inactive)) {
            throw new CloudRuntimeException("The user cannot be created as domain " + domain.getName() + " is being deleted");
        }

        checkAccess(getCurrentCallingAccount(), domain);

        Account account = _accountDao.findEnabledAccount(accountName, domainId);
        if (account == null || account.getType() == Account.Type.PROJECT) {
            throw new InvalidParameterValueException(String.format("Unable to find account %s in domain %s to create user", accountName, domain));
        }

        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException(String.format("Account: %s is a system account, can't add a user to it", account));
        }

        if (!userAllowMultipleAccounts.valueInScope(ConfigKey.Scope.Domain, domainId) && !_userAccountDao.validateUsernameInDomain(userName, domainId)) {
            throw new CloudRuntimeException("The user " + userName + " already exists in domain " + domainId);
        }
        List<UserVO> duplicatedUsers = _userDao.findUsersByName(userName);
        for (UserVO duplicatedUser : duplicatedUsers) {
            // users can't exist in same account
            assertUserNotAlreadyInAccount(duplicatedUser, account);
        }

        verifyCallerPrivilegeForUserOrAccountOperations(account);
        UserVO user;
        user = createUser(account.getId(), userName, password, firstName, lastName, email, timeZone, userUUID, source);
        return user;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_CREATE, eventDescription = "creating User")
    public UserVO createUser(String userName, String password, String firstName, String lastName, String email, String timeZone, String accountName, Long domainId, String userUUID) {

        return createUser(userName, password, firstName, lastName, email, timeZone, accountName, domainId, userUUID, User.Source.UNKNOWN);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_UPDATE, eventDescription = "Updating User")
    public UserAccount updateUser(UpdateUserCmd updateUserCmd) {
        UserVO user = retrieveAndValidateUser(updateUserCmd);
        Account account = retrieveAndValidateAccount(user);
        User caller = CallContext.current().getCallingUser();
        checkAccess(caller, account);
        verifyCallerPrivilegeForUserOrAccountOperations(user);

        logger.debug("Updating user {}", user);

        validateAndUpdateApiAndSecretKeyIfNeeded(updateUserCmd, user);
        validateAndUpdateUserApiKeyAccess(updateUserCmd, user);

        validateAndUpdateFirstNameIfNeeded(updateUserCmd, user);
        validateAndUpdateLastNameIfNeeded(updateUserCmd, user);
        validateAndUpdateUsernameIfNeeded(updateUserCmd, user, account);

        validateUserPasswordAndUpdateIfNeeded(updateUserCmd.getPassword(), user, updateUserCmd.getCurrentPassword(), false);
        String email = updateUserCmd.getEmail();
        if (StringUtils.isNotBlank(email)) {
            user.setEmail(email);
        }
        String timezone = updateUserCmd.getTimezone();
        if (StringUtils.isNotBlank(timezone)) {
            user.setTimezone(timezone);
        }
        Boolean mandate2FA = updateUserCmd.getMandate2FA();
        if (mandate2FA != null && mandate2FA) {
            user.setUser2faEnabled(true);
        }
        _userDao.update(user.getId(), user);
        return _userAccountDao.findById(user.getId());
    }

    @Override
    public void verifyCallerPrivilegeForUserOrAccountOperations(Account userAccount) {
        logger.debug(String.format("Verifying whether the caller has the correct privileges based on the user's role type and API permissions: %s", userAccount));

        if (!Account.Type.PROJECT.equals(userAccount.getType())) {
            checkCallerRoleTypeAllowedForUserOrAccountOperations(userAccount, null);
            checkCallerApiPermissionsForUserOrAccountOperations(userAccount);
        }
    }

    protected void verifyCallerPrivilegeForUserOrAccountOperations(User user) {
        logger.debug(String.format("Verifying whether the caller has the correct privileges based on the user's role type and API permissions: %s", user));

        Account userAccount = getAccount(user.getAccountId());
        if (!Account.Type.PROJECT.equals(userAccount.getType())) {
            checkCallerRoleTypeAllowedForUserOrAccountOperations(userAccount, user);
            checkCallerApiPermissionsForUserOrAccountOperations(userAccount);
        }
    }

    protected void checkCallerRoleTypeAllowedForUserOrAccountOperations(Account userAccount, User user) {
        Account callingAccount = getCurrentCallingAccount();
        RoleType callerRoleType = getRoleType(callingAccount);
        RoleType userAccountRoleType = getRoleType(userAccount);

        if (RoleType.Unknown == callerRoleType || RoleType.Unknown == userAccountRoleType) {
            String errMsg = String.format("The role type of caller account [%s, %s] or target account [%s, %s] is unknown",
                    callingAccount.getName(), callingAccount.getUuid(), userAccount.getName(), userAccount.getUuid());
            throw new PermissionDeniedException(errMsg);
        }

        boolean isCallerSystemOrDefaultAdmin = callingAccount.getId() == Account.ACCOUNT_ID_SYSTEM || callingAccount.getId() == Account.ACCOUNT_ID_ADMIN;
        if (isCallerSystemOrDefaultAdmin) {
            logger.trace(String.format("Admin account [%s, %s] performing this operation for user account [%s, %s] ", callingAccount.getName(), callingAccount.getUuid(), userAccount.getName(), userAccount.getUuid()));
        } else if (callerRoleType.getId() < userAccountRoleType.getId()) {
            logger.trace(String.format("The calling account [%s, %s] has a higher role type than the user account [%s, %s]",
                    callingAccount.getName(), callingAccount.getUuid(), userAccount.getName(), userAccount.getUuid()));
        } else if (callerRoleType.getId() == userAccountRoleType.getId()) {
            if (callingAccount.getId() != userAccount.getId()) {
                String allowedRoleTypes = listOfRoleTypesAllowedForOperationsOfSameRoleType.valueInScope(ConfigKey.Scope.Domain, callingAccount.getDomainId());
                boolean updateAllowed = allowedRoleTypes != null &&
                        Arrays.stream(allowedRoleTypes.split(","))
                                .map(String::trim)
                                .anyMatch(role -> role.equals(callerRoleType.toString()));
                if (BooleanUtils.isFalse(updateAllowed)) {
                    String errMsg = String.format("The calling account [%s, %s] is not allowed to perform this operation on users from other accounts " +
                            "of the same role type within the domain", callingAccount.getName(), callingAccount.getUuid());
                    logger.error(errMsg);
                    throw new PermissionDeniedException(errMsg);
                }
            } else if ((callingAccount.getId() == userAccount.getId()) && user != null) {
                Boolean allowOperationOnUsersinSameAccount = allowOperationsOnUsersInSameAccount.valueInScope(ConfigKey.Scope.Domain, callingAccount.getDomainId());
                User callingUser = CallContext.current().getCallingUser();
                if (callingUser.getId() != user.getId() && BooleanUtils.isFalse(allowOperationOnUsersinSameAccount)) {
                    String errMsg = "The user operations are not allowed by the users in the same account";
                    logger.error(errMsg);
                    throw new PermissionDeniedException(errMsg);
                }
            }
        } else {
            String errMsg = String.format("The calling account [%s, %s] has a lower role type than the user account [%s, %s]",
                    callingAccount.getName(), callingAccount.getUuid(), userAccount.getName(), userAccount.getUuid());
            throw new PermissionDeniedException(errMsg);
        }
    }

    protected void checkCallerApiPermissionsForUserOrAccountOperations(Account userAccount) {
        Account callingAccount = getCurrentCallingAccount();
        boolean isCallerRootAdmin = callingAccount.getId() == Account.ACCOUNT_ID_SYSTEM || isRootAdmin(callingAccount.getId());

        if (isCallerRootAdmin) {
            logger.trace(String.format("Admin account [%s, %s] performing this operation for user account [%s, %s] ", callingAccount.getName(), callingAccount.getUuid(), userAccount.getName(), userAccount.getUuid()));
        } else if (isRootAdmin(userAccount.getAccountId())) {
            String errMsg = String.format("Account [%s, %s] cannot perform this operation for user account [%s, %s] ", callingAccount.getName(), callingAccount.getUuid(), userAccount.getName(), userAccount.getUuid());
            logger.error(errMsg);
            throw new PermissionDeniedException(errMsg);
        } else {
            logger.debug(String.format("Checking calling account [%s, %s] permission to perform this operation for user account [%s, %s] ", callingAccount.getName(), callingAccount.getUuid(), userAccount.getName(), userAccount.getUuid()));
            checkRoleEscalation(callingAccount, userAccount);
            logger.debug(String.format("Calling account [%s, %s] is allowed to perform this operation for user account [%s, %s] ", callingAccount.getName(), callingAccount.getUuid(), userAccount.getName(), userAccount.getUuid()));
        }
    }

    /**
     * Updates the password in the user POJO if needed. If no password is provided, then the password is not updated.
     * The following validations are executed if 'password' is not null. Admins (root admins or domain admins) can execute password updates without entering the current password.
     * <ul>
     *  <li> If 'password' is blank, we throw an {@link InvalidParameterValueException};
     *  <li> If 'current password' is not provided and user is not an Admin, we throw an {@link InvalidParameterValueException};
     *  <li> If the user whose password is being changed has a source equal to {@link User.Source#SAML2}, {@link User.Source#SAML2DISABLED} or {@link User.Source#LDAP},
     *      we throw an {@link InvalidParameterValueException};
     *  <li> If a normal user is calling this method, we use {@link #validateCurrentPassword(UserVO, String)} to check if the provided old password matches the database one;
     * </ul>
     *
     * If all checks pass, we encode the given password with the most preferable password mechanism given in {@link #_userPasswordEncoders}.
     */
    public void validateUserPasswordAndUpdateIfNeeded(String newPassword, UserVO user, String currentPassword, boolean skipCurrentPassValidation) {
        if (newPassword == null) {
            logger.trace("No new password to update for user: {}", user);
            return;
        }
        if (StringUtils.isBlank(newPassword)) {
            throw new InvalidParameterValueException("Password cannot be empty or blank.");
        }

        User.Source userSource = user.getSource();
        if (userSource == User.Source.SAML2 || userSource == User.Source.SAML2DISABLED || userSource == User.Source.LDAP) {
            logger.warn("Unable to update the password for user [{}], as its source is [{}].", user, user.getSource().toString());
            throw new InvalidParameterValueException("CloudStack does not support updating passwords for SAML or LDAP users. Please contact your cloud administrator for assistance.");
        }

        passwordPolicy.verifyIfPasswordCompliesWithPasswordPolicies(newPassword, user.getUsername(), getAccount(user.getAccountId()).getDomainId());

        Account callingAccount = getCurrentCallingAccount();
        boolean isRootAdminExecutingPasswordUpdate = callingAccount.getId() == Account.ACCOUNT_ID_SYSTEM || isRootAdmin(callingAccount.getId());
        boolean isDomainAdmin = isDomainAdmin(callingAccount.getId());
        boolean isAdmin = isDomainAdmin || isRootAdminExecutingPasswordUpdate;
        boolean skipValidation = isAdmin || skipCurrentPassValidation;
        if (isAdmin) {
            logger.trace("Admin account [{}] executing password update for user [{}] ", callingAccount, user);
        }
        if (!skipValidation && StringUtils.isBlank(currentPassword)) {
            throw new InvalidParameterValueException("To set a new password the current password must be provided.");
        }
        if (CollectionUtils.isEmpty(_userPasswordEncoders)) {
            throw new CloudRuntimeException("No user authenticators configured!");
        }
        if (!skipValidation) {
            validateCurrentPassword(user, currentPassword);
        }
        UserAuthenticator userAuthenticator = _userPasswordEncoders.get(0);
        String newPasswordEncoded = userAuthenticator.encode(newPassword);
        user.setPassword(newPasswordEncoded);
    }

    /**
     * Iterates over all configured user authenticators and tries to authenticate the user using the current password.
     * If the user is authenticated with success, we have nothing else to do here; otherwise, an {@link InvalidParameterValueException} is thrown.
     */
    protected void validateCurrentPassword(UserVO user, String currentPassword) {
        AccountVO userAccount = _accountDao.findById(user.getAccountId());
        boolean currentPasswordMatchesDataBasePassword = false;
        for (UserAuthenticator userAuthenticator : _userPasswordEncoders) {
            Pair<Boolean, ActionOnFailedAuthentication> authenticationResult = userAuthenticator.authenticate(user.getUsername(), currentPassword, userAccount.getDomainId(), null);
            if (authenticationResult == null) {
                logger.trace(String.format("Authenticator [%s] is returning null for the authenticate method.", userAuthenticator.getClass()));
                continue;
            }
            if (BooleanUtils.toBoolean(authenticationResult.first())) {
                logger.debug("User [{}] re-authenticated [authenticator={}] during password update.", user, userAuthenticator.getName());
                currentPasswordMatchesDataBasePassword = true;
                break;
            }
        }
        if (!currentPasswordMatchesDataBasePassword) {
            throw new InvalidParameterValueException("Current password is incorrect.");
        }
    }

    /**
     * Validates the user 'username' if provided. The 'username' cannot be blank (when provided).
     * <ul>
     *  <li> If the 'username' is not provided, we do not update it (setting to null) in the User POJO.
     *  <li> If the 'username' is blank, we throw an {@link InvalidParameterValueException}.
     *  <li> The username must be unique in each domain. Therefore, if there is already another user with the same username, an {@link InvalidParameterValueException} is thrown.
     * </ul>
     */
    protected void validateAndUpdateUsernameIfNeeded(UpdateUserCmd updateUserCmd, UserVO newUser, Account newAccount) {
        String userName = updateUserCmd.getUsername();
        if (userName == null) {
            return;
        }
        if (StringUtils.isBlank(userName)) {
            throw new InvalidParameterValueException("Username cannot be empty.");
        }
        List<UserVO> existingUsers = _userDao.findUsersByName(userName);
        for (UserVO existingUser : existingUsers) {
            if (existingUser.getId() == newUser.getId()) {
                continue;
            }

            // duplicate usernames cannot exist in same domain unless explicitly configured
            if (!userAllowMultipleAccounts.valueInScope(ConfigKey.Scope.Domain, newAccount.getDomainId())) {
                assertUserNotAlreadyInDomain(existingUser, newAccount);
            }

            // can't rename a username to an existing one in the same account
            assertUserNotAlreadyInAccount(existingUser, newAccount);
        }
        newUser.setUsername(userName);
    }

    /**
     * Validates the user 'lastName' if provided. The 'lastName' cannot be blank (when provided).
     * <ul>
     *  <li> If the 'lastName' is not provided, we do not update it (setting to null) in the User POJO.
     *  <li> If the 'lastName' is blank, we throw an {@link InvalidParameterValueException}.
     * </ul>
     */
    protected void validateAndUpdateLastNameIfNeeded(UpdateUserCmd updateUserCmd, UserVO user) {
        String lastName = updateUserCmd.getLastname();
        if (lastName != null) {
            if (StringUtils.isBlank(lastName)) {
                throw new InvalidParameterValueException("Lastname cannot be empty.");
            }

            user.setLastname(lastName);
        }
    }

    /**
     * Validates the user 'firstName' if provided. The 'firstName' cannot be blank (when provided).
     * <ul>
     *  <li> If the 'firstName' is not provided, we do not update it (setting to null) in the User POJO.
     *  <li> If the 'firstName' is blank, we throw an {@link InvalidParameterValueException}.
     * </ul>
     */
    protected void validateAndUpdateFirstNameIfNeeded(UpdateUserCmd updateUserCmd, UserVO user) {
        String firstName = updateUserCmd.getFirstname();
        if (firstName != null) {
            if (StringUtils.isBlank(firstName)) {
                throw new InvalidParameterValueException("Firstname cannot be empty.");
            }
            user.setFirstname(firstName);
        }
    }

    /**
     * Searches an account for the given users. Then, we validate it as follows:
     * <ul>
     *  <li>If no account is found for the given user, we throw a {@link CloudRuntimeException}. There must be something wrong in the database for this case.
     *  <li>If the account is of {@link Account.Type#PROJECT}, we throw an {@link InvalidParameterValueException}.
     *  <li>If the account is of {@link Account#ACCOUNT_ID_SYSTEM}, we throw an {@link PermissionDeniedException}.
     * </ul>
     *
     * Afterwards, we check if the logged user has access to the user being updated via {@link #checkAccess(Account, AccessType, boolean, ControlledEntity...)}
     */
    protected Account retrieveAndValidateAccount(UserVO user) {
        Account account = _accountDao.findById(user.getAccountId());
        if (account == null) {
            throw new CloudRuntimeException("Unable to find user account with ID: " + user.getAccountId());
        }
        if (account.getType() == Account.Type.PROJECT) {
            throw new InvalidParameterValueException(String.format("Unable to find user with: %s", user));
        }
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException(String.format("user: %s is a system account; update is not allowed.", user));
        }
        checkAccess(getCurrentCallingAccount(), AccessType.OperateEntry, true, account);
        return account;
    }

    /**
     * Returns the calling account using the method {@link CallContext#getCallingAccount()}.
     * We are introducing this method to avoid using 'PowerMockRunner' in unit tests. Then, we can mock the calls to this method, which facilitates the development of test cases.
     */
    protected Account getCurrentCallingAccount() {
        return CallContext.current().getCallingAccount();
    }

    /**
     * Validates user API and Secret keys. If a new pair of keys is provided, we update them in the user POJO.
     * <ul>
     * <li>When updating the keys, it must be provided a pair (API and Secret keys); otherwise, an {@link InvalidParameterValueException} is thrown.
     * <li>If a pair of keys is provided, we validate to see if there is an user already using the provided API key. If there is someone else using, we throw an {@link InvalidParameterValueException} because two users cannot have the same API key.
     * </ul>
     */
    protected void validateAndUpdateApiAndSecretKeyIfNeeded(UpdateUserCmd updateUserCmd, UserVO user) {
        String apiKey = updateUserCmd.getApiKey();
        String secretKey = updateUserCmd.getSecretKey();

        boolean isApiKeyBlank = StringUtils.isBlank(apiKey);
        boolean isSecretKeyBlank = StringUtils.isBlank(secretKey);
        if (isApiKeyBlank ^ isSecretKeyBlank) {
            throw new InvalidParameterValueException("Please provide a userApiKey/userSecretKey pair");
        }
        if (isApiKeyBlank && isSecretKeyBlank) {
            return;
        }
        Pair<User, Account> apiKeyOwner = _accountDao.findUserAccountByApiKey(apiKey);
        if (apiKeyOwner != null) {
            User userThatHasTheProvidedApiKey = apiKeyOwner.first();
            if (userThatHasTheProvidedApiKey.getId() != user.getId()) {
                throw new InvalidParameterValueException(String.format("The API key [%s] already exists in the system. Please provide a unique key.", apiKey));
            }
        }
        user.setApiKey(apiKey);
        user.setSecretKey(secretKey);
    }

    protected void validateAndUpdateUserApiKeyAccess(UpdateUserCmd updateUserCmd, UserVO user) {
        if (updateUserCmd.getApiKeyAccess() != null) {
            try {
                ApiConstants.ApiKeyAccess access = ApiConstants.ApiKeyAccess.valueOf(updateUserCmd.getApiKeyAccess().toUpperCase());
                user.setApiKeyAccess(access.toBoolean());
                Long callingUserId = CallContext.current().getCallingUserId();
                Account callingAccount = CallContext.current().getCallingAccount();
                ActionEventUtils.onActionEvent(callingUserId, callingAccount.getAccountId(), callingAccount.getDomainId(),
                        EventTypes.API_KEY_ACCESS_UPDATE, "Api key access was changed for the User to " + access,
                        user.getId(), ApiCommandResourceType.User.toString());
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("ApiKeyAccess value can only be Enabled/Disabled/Inherit");
            }
        }
    }

    protected void validateAndUpdateAccountApiKeyAccess(UpdateAccountCmd updateAccountCmd, AccountVO account) {
        if (updateAccountCmd.getApiKeyAccess() != null) {
            try {
                ApiConstants.ApiKeyAccess access = ApiConstants.ApiKeyAccess.valueOf(updateAccountCmd.getApiKeyAccess().toUpperCase());
                account.setApiKeyAccess(access.toBoolean());
                Long callingUserId = CallContext.current().getCallingUserId();
                Account callingAccount = CallContext.current().getCallingAccount();
                ActionEventUtils.onActionEvent(callingUserId, callingAccount.getAccountId(), callingAccount.getDomainId(),
                        EventTypes.API_KEY_ACCESS_UPDATE, "Api key access was changed for the Account to " + access,
                        account.getId(), ApiCommandResourceType.Account.toString());
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("ApiKeyAccess value can only be Enabled/Disabled/Inherit");
            }
        }
    }

    /**
     * Searches for a user with the given userId. If no user is found we throw an {@link InvalidParameterValueException}.
     */
    protected UserVO retrieveAndValidateUser(UpdateUserCmd updateUserCmd) {
        Long userId = updateUserCmd.getId();

        UserVO user = _userDao.getUser(userId);
        if (user == null) {
            throw new InvalidParameterValueException("Unable to find user with id: " + userId);
        }
        return user;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_DISABLE, eventDescription = "disabling User", async = true)
    public UserAccount disableUser(long userId) {
        Account caller = getCurrentCallingAccount();

        // Check if user exists in the system
        User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        Account account = _accountDao.findById(user.getAccountId());
        if (account == null) {
            throw new InvalidParameterValueException("unable to find user account " + user.getAccountId());
        }

        // don't allow disabling user belonging to project's account
        if (account.getType() == Account.Type.PROJECT) {
            throw new InvalidParameterValueException(String.format("Unable to find active user %s", user));
        }

        // If the user is a System user, return an error
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException(String.format("User: %s is a system user, disabling is not allowed", user));
        }

        checkAccess(caller, AccessType.OperateEntry, true, account);
        verifyCallerPrivilegeForUserOrAccountOperations(user);

        boolean success = doSetUserStatus(userId, State.DISABLED);
        if (success) {

            CallContext.current().putContextParameter(User.class, user.getUuid());

            // user successfully disabled
            return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException(String.format("Unable to disable user %s", user));
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_USER_ENABLE, eventDescription = "enabling User")
    public UserAccount enableUser(final long userId) {

        Account caller = getCurrentCallingAccount();

        // Check if user exists in the system
        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }

        Account account = _accountDao.findById(user.getAccountId());
        if (account == null) {
            throw new InvalidParameterValueException("unable to find user account " + user.getAccountId());
        }

        if (account.getType() == Account.Type.PROJECT) {
            throw new InvalidParameterValueException(String.format("Unable to find active user %s", user));
        }

        // If the user is a System user, return an error
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException(String.format("User: %s is a system user, enabling is not allowed", user));
        }

        checkAccess(caller, AccessType.OperateEntry, true, account);
        verifyCallerPrivilegeForUserOrAccountOperations(user);

        boolean success = Transaction.execute(new TransactionCallback<>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                boolean success = doSetUserStatus(userId, State.ENABLED);

                // make sure the account is enabled too
                success = success && enableAccount(user.getAccountId());

                return success;
            }
        });

        if (success) {
            // whenever the user is successfully enabled, reset the login attempts to zero
            updateLoginAttempts(userId, 0, false);

            CallContext.current().putContextParameter(User.class, user.getUuid());

            return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException(String.format("Unable to enable user %s", user));
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_LOCK, eventDescription = "locking User")
    public UserAccount lockUser(long userId) {
        Account caller = getCurrentCallingAccount();

        // Check if user with id exists in the system
        User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find user by id");
        }

        Account account = _accountDao.findById(user.getAccountId());
        if (account == null) {
            throw new InvalidParameterValueException("unable to find user account " + user.getAccountId());
        }

        // don't allow to lock user of the account of type Project
        if (account.getType() == Account.Type.PROJECT) {
            throw new InvalidParameterValueException("Unable to find user by id");
        }

        // If the user is a System user, return an error. We do not allow this
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException(String.format("user: %s is a system user, locking is not allowed", user));
        }

        checkAccess(caller, AccessType.OperateEntry, true, account);
        verifyCallerPrivilegeForUserOrAccountOperations(user);

        // make sure the account is enabled too
        // if the user is either locked already or disabled already, don't change state...only lock currently enabled
        // users
        boolean success;
        if (user.getState().equals(State.LOCKED)) {
            // already locked...no-op
            return _userAccountDao.findById(userId);
        } else if (user.getState().equals(State.ENABLED)) {
            success = doSetUserStatus(user.getId(), State.LOCKED);

            boolean lockAccount = true;
            List<UserVO> allUsersByAccount = _userDao.listByAccount(user.getAccountId());
            for (UserVO oneUser : allUsersByAccount) {
                if (oneUser.getState().equals(State.ENABLED)) {
                    lockAccount = false;
                    break;
                }
            }

            if (lockAccount) {
                success = (success && lockAccount(user.getAccountId()));
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Attempting to lock a non-enabled user {}, current state is {}, locking failed.", user, user.getState());
            }
            success = false;
        }

        if (success) {

            CallContext.current().putContextParameter(User.class, user.getUuid());

            return _userAccountDao.findById(userId);
        } else {
            throw new CloudRuntimeException(String.format("Unable to lock user %s", user));
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DELETE, eventDescription = "deleting account", async = true)
    public boolean deleteUserAccount(long accountId) {

        CallContext ctx = CallContext.current();
        long callerUserId = ctx.getCallingUserId();
        Account caller = ctx.getCallingAccount();

        // If the user is a System user, return an error. We do not allow this
        AccountVO account = _accountDao.findById(accountId);

        if (caller.getId() == accountId) {
            Domain domain = _domainDao.findById(account.getDomainId());
            throw new InvalidParameterValueException(String.format("Deletion of your own account is not allowed. To delete account %s (ID: %s, Domain: %s), " +
                            "request to another user with permissions to perform the operation.",
                    account.getAccountName(), account.getUuid(), domain.getUuid()));
        }

        if (!isDeleteNeeded(account, accountId, caller)) {
            return true;
        }

        checkIfAccountManagesProjects(accountId);
        verifyCallerPrivilegeForUserOrAccountOperations(account);

        CallContext.current().putContextParameter(Account.class, account.getUuid());

        return deleteAccount(account, callerUserId, caller);
    }

    protected void checkIfAccountManagesProjects(long accountId) {
        List<Long> managedProjectIds = _projectAccountDao.listAdministratedProjectIds(accountId);
        if (!CollectionUtils.isEmpty(managedProjectIds)) {
            throw new InvalidParameterValueException(String.format(
                    "Unable to delete account [%s], because it manages the following project(s): %s. Please, remove the account from these projects or demote it to a regular project role first.",
                    accountId, managedProjectIds
            ));
        }
    }

    protected boolean isDeleteNeeded(AccountVO account, long accountId, Account caller) {
        if (account == null) {
            logger.info(String.format("The account, identified by id %d, doesn't exist", accountId ));
            return false;
        }
        if (account.getRemoved() != null) {
            logger.info("The account:{} is already removed", account);
            return false;
        }
        // don't allow removing Project account
        if (account.getType() == Account.Type.PROJECT) {
            throw new InvalidParameterValueException("The specified account does not exist in the system");
        }

        checkAccess(caller, null, true, account);

        // don't allow to delete default account (system and admin)
        if (account.isDefault()) {
            throw new InvalidParameterValueException("The account is default and can't be removed");
        }
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_ENABLE, eventDescription = "enabling account", async = true)
    public AccountVO enableAccount(String accountName, Long domainId, Long accountId) {

        // Check if account exists
        Account account;
        if (accountId != null) {
            account = _accountDao.findById(accountId);
        } else {
            account = _accountDao.findActiveAccount(accountName, domainId);
        }

        if (account == null || account.getType() == Account.Type.PROJECT) {
            throw new InvalidParameterValueException(String.format("Unable to find account by accountId: %d OR by name: %s in domain %s", accountId, accountName, _domainMgr.getDomain(domainId)));
        }

        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException(String.format("Account: %s is a system account, enable is not allowed", account));
        }

        // Check if user performing the action is allowed to modify this account
        Account caller = getCurrentCallingAccount();
        checkAccess(caller, AccessType.OperateEntry, true, account);
        verifyCallerPrivilegeForUserOrAccountOperations(account);

        boolean success = enableAccount(account.getId());
        if (success) {

            CallContext.current().putContextParameter(Account.class, account.getUuid());

            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException(String.format("Unable to enable account %s[%s] in domain %s", accountName, account.getUuid(), _domainMgr.getDomain(domainId)));
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DISABLE, eventDescription = "locking account", async = true)
    public AccountVO lockAccount(String accountName, Long domainId, Long accountId) {
        Account caller = getCurrentCallingAccount();

        Account account;
        if (accountId != null) {
            account = _accountDao.findById(accountId);
        } else {
            account = _accountDao.findActiveAccount(accountName, domainId);
        }

        if (account == null || account.getType() == Account.Type.PROJECT) {
            throw new InvalidParameterValueException("Unable to find active account by accountId: " + accountId + " OR by name: " + accountName + " in domain " + domainId);
        }

        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException(String.format("Account: %s is a system account, lock is not allowed", account));
        }

        checkAccess(caller, AccessType.OperateEntry, true, account);
        verifyCallerPrivilegeForUserOrAccountOperations(account);

        if (lockAccount(account.getId())) {
            CallContext.current().putContextParameter(Account.class, account.getUuid());
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException(String.format("Unable to lock account %s by accountId: %d OR by name: %s in domain %s", account, accountId, accountName, _domainMgr.getDomain(domainId)));
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_DISABLE, eventDescription = "disabling account", async = true)
    public AccountVO disableAccount(String accountName, Long domainId, Long accountId) throws ConcurrentOperationException, ResourceUnavailableException {
        Account caller = getCurrentCallingAccount();

        Account account;
        if (accountId != null) {
            account = _accountDao.findById(accountId);
        } else {
            account = _accountDao.findActiveAccount(accountName, domainId);
        }

        if (account == null || account.getType() == Account.Type.PROJECT) {
            throw new InvalidParameterValueException(String.format("Unable to find account by accountId: %d OR by name: %s in domain %s", accountId, accountName, _domainMgr.getDomain(domainId)));
        }

        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new PermissionDeniedException(String.format("Account: %s is a system account, disable is not allowed", account));
        }

        checkAccess(caller, AccessType.OperateEntry, true, account);
        verifyCallerPrivilegeForUserOrAccountOperations(account);

        if (disableAccount(account.getId())) {
            CallContext.current().putContextParameter(Account.class, account.getUuid());
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException(String.format("Unable to update account %s by accountId: %d OR by name: %s in domain %s", account, accountId, accountName, _domainMgr.getDomain(domainId)));
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_UPDATE, eventDescription = "updating account", async = true)
    public AccountVO updateAccount(UpdateAccountCmd cmd) {
        Long accountId = cmd.getId();
        Long domainId = cmd.getDomainId();
        Long roleId = cmd.getRoleId();
        String accountName = cmd.getAccountName();
        String newAccountName = cmd.getNewName();
        String networkDomain = cmd.getNetworkDomain();
        final Map<String, String> details = cmd.getDetails();

        boolean success;
        Account account;
        if (accountId != null) {
            account = _accountDao.findById(accountId);
        } else {
            account = _accountDao.findEnabledAccount(accountName, domainId);
        }

        final AccountVO acctForUpdate = _accountDao.findById(account.getId());

        // Check if account exists
        if (account == null || account.getType() == Account.Type.PROJECT) {
            logger.error("Unable to find account by accountId: {} OR by name: {} in domain {}", accountId, accountName, _domainMgr.getDomain(domainId));
            throw new InvalidParameterValueException(String.format("Unable to find account by accountId: %d OR by name: %s in domain %s", accountId, accountName, _domainMgr.getDomain(domainId)));
        }

        // Don't allow to modify system account
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("Can not modify system account");
        }

        // Check if user performing the action is allowed to modify this account
        Account caller = getCurrentCallingAccount();
        checkAccess(caller, _domainMgr.getDomain(account.getDomainId()));
        verifyCallerPrivilegeForUserOrAccountOperations(account);

        validateAndUpdateAccountApiKeyAccess(cmd, acctForUpdate);

        if(newAccountName != null) {

            if (newAccountName.isEmpty()) {
                throw new InvalidParameterValueException(String.format("The new account name for " +
                        "account '%s' within domain '%s' is empty string. Account will be not renamed.",
                        account, _domainMgr.getDomain(domainId)));
            }

            // check if the new proposed account name is absent in the domain
            Account existingAccount = _accountDao.findActiveAccount(newAccountName, domainId);
            if (existingAccount != null && existingAccount.getId() != account.getId()) {
                throw new InvalidParameterValueException(String.format("The account with the " +
                                "proposed name '%s' exists in the domain '%s' with existing account %s",
                        newAccountName, _domainMgr.getDomain(domainId), existingAccount));
            }

            acctForUpdate.setAccountName(newAccountName);
        }

        if (networkDomain != null && !networkDomain.isEmpty()) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException("Invalid network domain or format. " +
                        "Total length shouldn't exceed 190 chars. Every domain part must be between 1 and 63 " +
                        "characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }


        if (roleId != null) {
            final List<Role> roles = cmd.roleService.listRoles();
            final boolean roleNotFound = roles.stream().filter(r -> r.getId() == roleId).count() == 0;
            if (roleNotFound) {
                throw new InvalidParameterValueException(String.format("Role with ID '%s' is not " +
                        "found or not available for the account '%s' in the domain '%s'.",
                        roleId, account, _domainMgr.getDomain(domainId)));
            }

            Role role = roleService.findRole(roleId);
            validateRoleChange(account, role, caller);
            acctForUpdate.setRoleId(roleId);
            acctForUpdate.setType(role.getRoleType().getAccountType());
            checkRoleEscalation(getCurrentCallingAccount(), acctForUpdate);
        }

        if (networkDomain != null) {
            if (networkDomain.isEmpty()) {
                acctForUpdate.setNetworkDomain(null);
            } else {
                acctForUpdate.setNetworkDomain(networkDomain);
            }
        }

        final Account accountFinal = account;
        success = Transaction.execute((TransactionCallback<Boolean>) status -> {
            boolean success1 = _accountDao.update(accountFinal.getId(), acctForUpdate);

            if (details != null && success1) {
                _accountDetailsDao.update(accountFinal.getId(), details);
            }

            return success1;
        });

        if (success) {
            CallContext.current().putContextParameter(Account.class, account.getUuid());
            return _accountDao.findById(account.getId());
        } else {
            throw new CloudRuntimeException(String.format("Unable to update account %s by accountId: %d OR by name: %s in domain %d", account, accountId, accountName, domainId));
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_DELETE, eventDescription = "deleting User")
    public boolean deleteUser(DeleteUserCmd deleteUserCmd) {
        final Long id = deleteUserCmd.getId();
        User caller = CallContext.current().getCallingUser();
        UserVO user = getValidUserVO(id);
        Account account = _accountDao.findById(user.getAccountId());

        if (caller.getId() == id) {
            Domain domain = _domainDao.findById(account.getDomainId());
            throw new InvalidParameterValueException(String.format("The caller is requesting to delete itself. As a security measure, ACS will not allow this operation." +
                    " To delete user %s (ID: %s, Domain: %s), request to another user with permission to execute the operation.", user.getUsername(), user.getUuid(), domain.getUuid()));
        }

        // don't allow to delete the user from the account of type Project
        checkAccountAndAccess(user, account);
        verifyCallerPrivilegeForUserOrAccountOperations(user);
        return _userDao.remove(id);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_USER_MOVE, eventDescription = "moving User to a new account")
    public boolean moveUser(MoveUserCmd cmd) {
        final Long id = cmd.getId();
        UserVO user = getValidUserVO(id);
        Account oldAccount = _accountDao.findById(user.getAccountId());
        checkAccountAndAccess(user, oldAccount);
        verifyCallerPrivilegeForUserOrAccountOperations(user);
        long domainId = oldAccount.getDomainId();

        long newAccountId = getNewAccountId(domainId, cmd.getAccountName(), cmd.getAccountId());

        return moveUser(user, newAccountId);
    }

    @Override
    public boolean moveUser(long id, Long domainId, Account newAccount) {
        UserVO user = getValidUserVO(id);
        Account oldAccount = _accountDao.findById(user.getAccountId());
        checkAccountAndAccess(user, oldAccount);
        checkIfNotMovingAcrossDomains(domainId, newAccount);
        return moveUser(user, newAccount.getId());
    }

    private boolean moveUser(UserVO user, long newAccountId) {
        if (newAccountId == user.getAccountId()) {
            // could do a not silent fail but the objective of the user is reached
            return true; // no need to create a new user object for this user
        }

        return Transaction.execute(new TransactionCallback<>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                UserVO newUser = new UserVO(user);
                user.setExternalEntity(user.getUuid());
                user.setUuid(UUID.randomUUID().toString());
                user.setApiKey(null);
                user.setSecretKey(null);
                _userDao.update(user.getId(), user);
                newUser.setAccountId(newAccountId);
                boolean success = _userDao.remove(user.getId());
                UserVO persisted = _userDao.persist(newUser);
                return success && persisted.getUuid().equals(user.getExternalEntity());
            }
        });
    }

    private long getNewAccountId(long domainId, String accountName, Long accountId) {
        Account newAccount = null;
        if (StringUtils.isNotBlank(accountName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Getting id for account by name '" + accountName + "' in domain " + domainId);
            }
            newAccount = _accountDao.findEnabledAccount(accountName, domainId);
        }
        if (newAccount == null && accountId != null) {
            newAccount = _accountDao.findById(accountId);
        }
        if (newAccount == null) {
            throw new CloudRuntimeException("no account name or account id. this should have been caught before this point");
        }

        checkIfNotMovingAcrossDomains(domainId, newAccount);
        return newAccount.getAccountId();
    }

    private void checkIfNotMovingAcrossDomains(long domainId, Account newAccount) {
        if (newAccount.getDomainId() != domainId) {
            // not in scope
            throw new InvalidParameterValueException("moving a user from an account in one domain to an account in another domain is not supported!");
        }
    }

    protected void checkAccountAndAccess(UserVO user, Account account) {
        // don't allow to delete the user from the account of type Project
        if (account.getType() == Account.Type.PROJECT) {
            throw new InvalidParameterValueException("Project users cannot be deleted or moved.");
        }

        checkAccess(getCurrentCallingAccount(), AccessType.OperateEntry, true, account);
        CallContext.current().putContextParameter(User.class, user.getUuid());
    }

    protected UserVO getValidUserVO(long id) {
        UserVO user = _userDao.findById(id);

        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("The specified user doesn't exist in the system");
        }

        // don't allow to delete default user (system and admin users)
        if (user.isDefault()) {
            throw new InvalidParameterValueException("The user is default and can't be (re)moved");
        }

        return user;
    }

    protected class AccountCleanupTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                GlobalLock lock = GlobalLock.getInternLock("AccountCleanup");
                if (lock == null) {
                    logger.debug("Couldn't get the global lock");
                    return;
                }

                if (!lock.lock(30)) {
                    logger.debug("Couldn't lock the db");
                    return;
                }

                try {
                    // Cleanup removed accounts
                    List<AccountVO> removedAccounts = _accountDao.findCleanupsForRemovedAccounts(null);
                    logger.info("Found " + removedAccounts.size() + " removed accounts to cleanup");
                    for (AccountVO account : removedAccounts) {
                        logger.debug("Cleaning up {}", account);
                        cleanupAccount(account, getSystemUser().getId(), getSystemAccount());
                    }

                    // cleanup disabled accounts
                    List<AccountVO> disabledAccounts = _accountDao.findCleanupsForDisabledAccounts();
                    logger.info("Found " + disabledAccounts.size() + " disabled accounts to cleanup");
                    for (AccountVO account : disabledAccounts) {
                        logger.debug("Disabling account {}", account);
                        try {
                            disableAccount(account.getId());
                        } catch (Exception e) {
                            logger.error("Skipping due to error on account {}", account, e);
                        }
                    }

                    // cleanup inactive domains
                    List<? extends Domain> inactiveDomains = _domainMgr.findInactiveDomains();
                    logger.info("Found " + inactiveDomains.size() + " inactive domains to cleanup");
                    for (Domain inactiveDomain : inactiveDomains) {
                        long domainId = inactiveDomain.getId();
                        try {
                            List<AccountVO> accountsForCleanupInDomain = _accountDao.findCleanupsForRemovedAccounts(domainId);
                            if (accountsForCleanupInDomain.isEmpty()) {
                                // release dedication if any, before deleting the domain
                                List<DedicatedResourceVO> dedicatedResources = _dedicatedDao.listByDomainId(domainId);
                                if (dedicatedResources != null && !dedicatedResources.isEmpty()) {
                                    logger.debug("Releasing dedicated resources for domain {}", inactiveDomain);
                                    for (DedicatedResourceVO dr : dedicatedResources) {
                                        if (!_dedicatedDao.remove(dr.getId())) {
                                            logger.warn("Fail to release dedicated resources for domain {}", inactiveDomain);
                                        }
                                    }
                                }
                                logger.debug("Removing inactive domain {}", inactiveDomain);
                                _domainMgr.removeDomain(domainId);
                            } else {
                                logger.debug("Can't remove inactive domain {} as it has accounts that need cleanup", inactiveDomain);
                            }
                        } catch (Exception e) {
                            logger.error("Skipping due to error on domain {}", inactiveDomain, e);
                        }
                    }

                    // cleanup inactive projects
                    List<ProjectVO> inactiveProjects = _projectDao.listByState(Project.State.Disabled);
                    logger.info("Found " + inactiveProjects.size() + " disabled projects to cleanup");
                    for (ProjectVO project : inactiveProjects) {
                        try {
                            Account projectAccount = getAccount(project.getProjectAccountId());
                            if (projectAccount == null) {
                                logger.debug("Removing inactive project {}", project);
                                _projectMgr.deleteProject(CallContext.current().getCallingAccount(), CallContext.current().getCallingUserId(), project);
                            } else {
                                logger.debug("Can't remove disabled project {} as it has non removed account {}", project, projectAccount);
                            }
                        } catch (Exception e) {
                            logger.error("Skipping due to error on project " + project, e);
                        }
                    }

                } catch (Exception e) {
                    logger.error("Exception ", e);
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                logger.error("Exception ", e);
            }
        }
    }

    @Override
    public Account finalizeOwner(Account caller, String accountName, Long domainId, Long projectId) {
        // don't default the owner to the system account
        if (caller.getId() == Account.ACCOUNT_ID_SYSTEM && ((accountName == null || domainId == null) && projectId == null)) {
            throw new InvalidParameterValueException("Account and domainId are needed for resource creation");
        }

        // projectId and account/domainId can't be specified together
        if ((accountName != null && domainId != null) && projectId != null) {
            throw new InvalidParameterValueException("ProjectId and account/domainId can't be specified together");
        }

        if (projectId != null) {
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id=" + projectId);
            }

            if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                throw new PermissionDeniedException("Account " + caller + " is unauthorised to use project id=" + projectId);
            }

            return getAccount(project.getProjectAccountId());
        }

        if (isAdmin(caller.getId()) && accountName != null && domainId != null) {
            Domain domain = _domainMgr.getDomain(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find the domain by id=" + domainId);
            }

            Account owner = _accountDao.findActiveAccount(accountName, domainId);
            if (owner == null) {
                throw new InvalidParameterValueException(String.format("Unable to find account %s in domain %s", accountName, domain));
            }
            checkAccess(caller, domain);

            return owner;
        } else if (!isAdmin(caller.getId()) && accountName != null && domainId != null) {
            if (!accountName.equals(caller.getAccountName()) || domainId.longValue() != caller.getDomainId()) {
                throw new PermissionDeniedException("Can't create/list resources for account " + accountName + " in domain " + domainId + ", permission denied");
            } else {
                return caller;
            }
        } else {
            if (accountName != null && domainId == null) {
                throw new InvalidParameterValueException("AccountName and domainId must be specified together");
            }
            // regular user can't create/list resources for other people
            return caller;
        }
    }

    @Override
    public Account getActiveAccountByName(String accountName, Long domainId) {
        if (accountName == null || domainId == null) {
            throw new InvalidParameterValueException("Both accountName and domainId are required for finding active account in the system");
        } else {
            return _accountDao.findActiveAccount(accountName, domainId);
        }
    }

    @Override
    public UserAccount getActiveUserAccount(String username, Long domainId) {
        return _userAccountDao.getUserAccount(username, domainId);
    }

    @Override
    public List<UserAccount> getActiveUserAccountByEmail(String email, Long domainId) {
        List<UserAccountVO> userAccountByEmail = _userAccountDao.getUserAccountByEmail(email, domainId);
        List<UserAccount> userAccounts = userAccountByEmail.stream()
                .map(userAccountVO -> (UserAccount) userAccountVO)
                .collect(Collectors.toList());
        return userAccounts;
    }

    @Override
    public Account getActiveAccountById(long accountId) {
        return _accountDao.findById(accountId);
    }

    @Override
    public Account getAccount(long accountId) {
        return _accountDao.findByIdIncludingRemoved(accountId);
    }

    @Override
    public RoleType getRoleType(Account account) {
        if (account == null) {
            return RoleType.Unknown;
        }
        return RoleType.getByAccountType(account.getType());
    }

    @Override
    public User getActiveUser(long userId) {
        return _userDao.findById(userId);
    }

    @Override
    public User getUserIncludingRemoved(long userId) {
        return _userDao.findByIdIncludingRemoved(userId);
    }

    @Override
    public User getActiveUserByRegistrationToken(String registrationToken) {
        return _userDao.findUserByRegistrationToken(registrationToken);
    }

    @Override
    public void markUserRegistered(long userId) {
        UserVO userForUpdate = _userDao.createForUpdate();
        userForUpdate.setRegistered(true);
        _userDao.update(Long.valueOf(userId), userForUpdate);
    }

    @Override
    @DB
    public AccountVO createAccount(final String accountName, final Account.Type accountType, final Long roleId, final Long domainId, final String networkDomain, final Map<String, String> details, final String uuid) {
        // Validate domain
        Domain domain = _domainMgr.getDomain(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("The domain " + domainId + " does not exist; unable to create account");
        }

        if (domain.getState().equals(Domain.State.Inactive)) {
            throw new CloudRuntimeException("The account cannot be created as domain " + domain.getName() + " is being deleted");
        }

        if ((domainId != Domain.ROOT_DOMAIN) && (accountType == Account.Type.ADMIN)) {
            throw new InvalidParameterValueException(String.format("Invalid account type %s given for " +
                    "an account in domain %s; unable to create user of admin role type in non-ROOT domain.", accountType, domain));
        }

        // Validate account/user/domain settings
        if (_accountDao.findActiveAccount(accountName, domainId) != null) {
            throw new InvalidParameterValueException("The specified account: " + accountName + " already exists");
        }

        if (networkDomain != null) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }


        if (accountType == Account.Type.RESOURCE_DOMAIN_ADMIN) {
            List<DataCenterVO> dc = _dcDao.findZonesByDomainId(domainId);
            if (dc.isEmpty()) {
                throw new InvalidParameterValueException(String.format("The account cannot be created as domain %s is not associated with any private Zone", domain));
            }
        }

        AccountVO newAccount = new AccountVO(accountName, domainId, networkDomain, accountType, roleId, uuid);
        verifyCallerPrivilegeForUserOrAccountOperations(newAccount);

        // Create the account
        return Transaction.execute(new TransactionCallback<>() {
            @Override
            public AccountVO doInTransaction(TransactionStatus status) {
                AccountVO account = _accountDao.persist(new AccountVO(accountName, domainId, networkDomain, accountType, roleId, uuid));

                if (account == null) {
                    throw new CloudRuntimeException(String.format("Failed to create account name %s in domain id=%s", accountName, _domainMgr.getDomain(domainId)));
                }

                Long accountId = account.getId();

                if (details != null) {
                    _accountDetailsDao.persist(accountId, details);
                }

                // Create resource count records for the account
                _resourceCountDao.createResourceCounts(accountId, ResourceLimit.ResourceOwnerType.Account);

                // Create default security group
                _networkGroupMgr.createDefaultSecurityGroup(accountId);

                return account;
            }
        });
    }

    protected UserVO createUser(long accountId, String userName, String password, String firstName, String lastName, String email, String timezone, String userUUID, User.Source source) {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating user: " + userName + ", accountId: " + accountId + " timezone:" + timezone);
        }

        passwordPolicy.verifyIfPasswordCompliesWithPasswordPolicies(password, userName, getAccount(accountId).getDomainId());

        String encodedPassword = null;
        for (UserAuthenticator authenticator : _userPasswordEncoders) {
            encodedPassword = authenticator.encode(password);
            if (encodedPassword != null) {
                break;
            }
        }
        if (encodedPassword == null) {
            throw new CloudRuntimeException("Failed to encode password");
        }

        if (userUUID == null) {
            userUUID = UUID.randomUUID().toString();
        }

        UserVO user = _userDao.persist(new UserVO(accountId, userName, encodedPassword, firstName, lastName, email, timezone, userUUID, source));
        CallContext.current().putContextParameter(User.class, user.getUuid());
        return user;
    }

    @Override
    public void logoutUser(long userId) {
        UserAccount userAcct = _userAccountDao.findById(userId);
        if (userAcct != null) {
            ActionEventUtils.onActionEvent(userId, userAcct.getAccountId(), userAcct.getDomainId(), EventTypes.EVENT_USER_LOGOUT, "user has logged out", userId, ApiCommandResourceType.User.toString());
        } // else log some kind of error event? This likely means the user doesn't exist, or has been deleted...
    }

    @Override
    public UserAccount authenticateUser(final String username, final String password, final Long domainId, final InetAddress loginIpAddress, final Map<String, Object[]> requestParameters) {
        long authStartTimeInMs = System.currentTimeMillis();
        UserAccount user = null;
        final String[] oAuthProviderArray = (String[])requestParameters.get(ApiConstants.PROVIDER);
        final String[] secretCodeArray = (String[])requestParameters.get(ApiConstants.SECRET_CODE);
        String oauthProvider = ((oAuthProviderArray == null) ? null : oAuthProviderArray[0]);
        String secretCode = ((secretCodeArray == null) ? null : secretCodeArray[0]);

        if ((password != null && !password.isEmpty()) || (oauthProvider != null && secretCode != null)) {
            user = getUserAccount(username, password, domainId, requestParameters);
        } else {
            user = getUserAccountForSSO(username, domainId, requestParameters);
        }

        if (user != null) {
            // don't allow to authenticate system user
            if (user.getId() == User.UID_SYSTEM) {
                logger.error("Failed to authenticate user: " + username + " in domain " + domainId);
                return null;
            }
            // don't allow baremetal system user
            if (BaremetalUtils.BAREMETAL_SYSTEM_ACCOUNT_NAME.equals(user.getUsername())) {
                logger.error("Won't authenticate user: " + username + " in domain " + domainId);
                return null;
            }

            // We authenticated successfully by now, let's check if we are allowed to login from the ip address the reqest comes from
            final Account account = getAccount(user.getAccountId());
            final DomainVO domain = (DomainVO) _domainMgr.getDomain(account.getDomainId());

            // Get the CIDRs from where this account is allowed to make calls
            final String accessAllowedCidrs = ApiServiceConfiguration.ApiAllowedSourceCidrList.valueIn(account.getId()).replaceAll("\\s", "");
            final Boolean ApiSourceCidrChecksEnabled = ApiServiceConfiguration.ApiSourceCidrChecksEnabled.value();

            if (ApiSourceCidrChecksEnabled) {
                logger.debug("CIDRs from which account '" + account.toString() + "' is allowed to perform API calls: " + accessAllowedCidrs);

                // Block when is not in the list of allowed IPs
                if (!NetUtils.isIpInCidrList(loginIpAddress, accessAllowedCidrs.split(","))) {
                    logger.warn("Request by account '" + account.toString() + "' was denied since " + loginIpAddress.toString().replace("/", "") + " does not match " + accessAllowedCidrs);
                    throw new CloudAuthenticationException("Failed to authenticate user '" + username + "' in domain '" + domain.getPath() + "' from ip "
                            + loginIpAddress.toString().replace("/", "") + "; please provide valid credentials");
                }
            }

            ActionEventUtils.onActionEvent(user.getId(), user.getAccountId(), user.getDomainId(), EventTypes.EVENT_USER_LOGIN, "user has logged in from IP Address " + loginIpAddress, user.getId(), ApiCommandResourceType.User.toString());

            validUserLastAuthTimeDurationInMs = System.currentTimeMillis() - authStartTimeInMs;
            // Here all is fine!
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("User: %s in domain %d has successfully logged in, auth time duration - %d ms", username, domainId, validUserLastAuthTimeDurationInMs));
            }

            return user;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("User: " + username + " in domain " + domainId + " has failed to log in");
            }

            long waitTimeDurationInMs;
            long invalidUserAuthTimeDurationInMs = System.currentTimeMillis() - authStartTimeInMs;
            if (validUserLastAuthTimeDurationInMs > 0) {
                waitTimeDurationInMs = validUserLastAuthTimeDurationInMs - invalidUserAuthTimeDurationInMs;
            } else {
                waitTimeDurationInMs = DEFAULT_USER_AUTH_TIME_DURATION_MS - invalidUserAuthTimeDurationInMs;
            }

            if (waitTimeDurationInMs > 0) {
                try {
                    Thread.sleep(waitTimeDurationInMs);
                } catch (final InterruptedException e) {
                }
            }

            return null;
        }
    }

    private UserAccount getUserAccount(String username, String password, Long domainId, Map<String, Object[]> requestParameters) {
        if (logger.isDebugEnabled()) {
            logger.debug("Attempting to log in user: " + username + " in domain " + domainId);
        }
        UserAccount userAccount = _userAccountDao.getUserAccount(username, domainId);

        boolean authenticated = false;
        HashSet<ActionOnFailedAuthentication> actionsOnFailedAuthenticaion = new HashSet<>();
        User.Source userSource = userAccount != null ? userAccount.getSource() : User.Source.UNKNOWN;
        for (UserAuthenticator authenticator : _userAuthenticators) {
            final String[] secretCodeArray = (String[])requestParameters.get(ApiConstants.SECRET_CODE);
            String secretCode = ((secretCodeArray == null) ? null : secretCodeArray[0]);
            if (userSource != User.Source.UNKNOWN && secretCode == null) {
                if (!authenticator.getName().equalsIgnoreCase(userSource.name())) {
                    continue;
                }
            }
            if ((secretCode != null && !authenticator.getName().equals(OAUTH2_PROVIDER_NAME))
                    || (secretCode == null && authenticator.getName().equals(OAUTH2_PROVIDER_NAME))) {
                continue;
            }
            Pair<Boolean, ActionOnFailedAuthentication> result = authenticator.authenticate(username, password, domainId, requestParameters);
            if (result.first()) {
                authenticated = true;
                break;
            } else if (result.second() != null) {
                actionsOnFailedAuthenticaion.add(result.second());
            }
        }

        boolean updateIncorrectLoginCount = actionsOnFailedAuthenticaion.contains(ActionOnFailedAuthentication.INCREMENT_INCORRECT_LOGIN_ATTEMPT_COUNT);

        if (authenticated) {
            Domain domain = _domainMgr.getDomain(domainId);
            String domainName = null;
            if (domain != null) {
                domainName = domain.getName();
            }
            userAccount = _userAccountDao.getUserAccount(username, domainId);

            if (!userAccount.getState().equalsIgnoreCase(Account.State.ENABLED.toString()) || !userAccount.getAccountState().equalsIgnoreCase(Account.State.ENABLED.toString())) {
                if (logger.isInfoEnabled()) {
                    logger.info("User {} in domain {} is disabled/locked (or account is disabled/locked)", userAccount, domain);
                }
                throw new CloudAuthenticationException(String.format("User %s (or their account) in domain %s is disabled/locked. Please contact the administrator.", userAccount, domain));
            }
            // Whenever the user is able to log in successfully, reset the login attempts to zero
            if (!isInternalAccount(userAccount.getId())) {
                updateLoginAttempts(userAccount.getId(), 0, false);
            }

            return userAccount;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to authenticate user with username " + username + " in domain " + domainId);
            }

            if (userAccount == null) {
                logger.warn("Unable to find an user with username " + username + " in domain " + domainId);
                return null;
            }

            if (userAccount.getState().equalsIgnoreCase(Account.State.ENABLED.toString())) {
                if (!isInternalAccount(userAccount.getId())) {
                    // Internal accounts are not disabled
                    updateLoginAttemptsWhenIncorrectLoginAttemptsEnabled(userAccount, updateIncorrectLoginCount, _allowedLoginAttempts);
                }
            } else {
                logger.info("User " + userAccount.getUsername() + " is disabled/locked");
            }
            return null;
        }
    }

    private UserAccount getUserAccountForSSO(String username, Long domainId, Map<String, Object[]> requestParameters) {
        String key = _configDao.getValue("security.singlesignon.key");
        if (key == null) {
            // the SSO key is gone, don't authenticate
            return null;
        }

        String singleSignOnTolerance = _configDao.getValue("security.singlesignon.tolerance.millis");
        if (singleSignOnTolerance == null) {
            // the SSO tolerance is gone (how much time before/after system time we'll allow the login request to be
            // valid),
            // don't authenticate
            return null;
        }

        UserAccount user = null;
        long tolerance = Long.parseLong(singleSignOnTolerance);
        String signature = null;
        long timestamp = 0L;
        String unsignedRequest;
        StringBuffer unsignedRequestBuffer = new StringBuffer();

        // - build a request string with sorted params, make sure it's all lowercase
        // - sign the request, verify the signature is the same
        List<String> parameterNames = new ArrayList<>();

        for (Object paramNameObj : requestParameters.keySet()) {
            parameterNames.add((String)paramNameObj); // put the name in a list that we'll sort later
        }

        Collections.sort(parameterNames);

        try {
            for (String paramName : parameterNames) {
                // parameters come as name/value pairs in the form String/String[]
                String paramValue = ((String[])requestParameters.get(paramName))[0];

                if ("signature".equalsIgnoreCase(paramName)) {
                    signature = paramValue;
                } else {
                    if ("timestamp".equalsIgnoreCase(paramName)) {
                        String timestampStr = paramValue;
                        try {
                            // If the timestamp is in a valid range according to our tolerance, verify the request
                            // signature, otherwise return null to indicate authentication failure
                            timestamp = Long.parseLong(timestampStr);
                            long currentTime = System.currentTimeMillis();
                            if (Math.abs(currentTime - timestamp) > tolerance) {
                                logger.debug("Expired timestamp passed in to login, current time = {}, timestamp = {}", currentTime, timestamp);
                                return null;
                            }
                        } catch (NumberFormatException nfe) {
                            logger.debug("Invalid timestamp passed in to login: {}", timestampStr);
                            return null;
                        }
                    }

                    if (unsignedRequestBuffer.length() != 0) {
                        unsignedRequestBuffer.append("&");
                    }
                    unsignedRequestBuffer.append(paramName).append("=").append(URLEncoder.encode(paramValue, "UTF-8"));
                }
            }

            if ((signature == null) || (timestamp == 0L)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Missing parameters in login request, signature = " + signature + ", timestamp = " + timestamp);
                }
                return null;
            }

            unsignedRequest = unsignedRequestBuffer.toString().toLowerCase().replaceAll("\\+", "%20");

            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA1");
            mac.init(keySpec);
            mac.update(unsignedRequest.getBytes());
            byte[] encryptedBytes = mac.doFinal();
            String computedSignature = new String(Base64.encodeBase64(encryptedBytes));
            boolean equalSig = ConstantTimeComparator.compareStrings(signature, computedSignature);
            if (!equalSig) {
                logger.info("User signature: " + signature + " is not equaled to computed signature: " + computedSignature);
            } else {
                user = _userAccountDao.getUserAccount(username, domainId);
            }
        } catch (Exception ex) {
            logger.error("Exception authenticating user", ex);
            return null;
        }

        return user;
    }

    protected void updateLoginAttemptsWhenIncorrectLoginAttemptsEnabled(UserAccount account, boolean updateIncorrectLoginCount,
                                                                      int allowedLoginAttempts) {
        int attemptsMade = account.getLoginAttempts() + 1;
        if (allowedLoginAttempts <= 0 || !updateIncorrectLoginCount) {
            return;
        }
        if (attemptsMade < allowedLoginAttempts) {
            updateLoginAttempts(account.getId(), attemptsMade, false);
            logger.warn("Login attempt failed. You have " +
                    (allowedLoginAttempts - attemptsMade) + " attempt(s) remaining");
        } else {
            updateLoginAttempts(account.getId(), allowedLoginAttempts, true);
            logger.warn("User {} has been disabled due to multiple failed login attempts. Please contact admin.", account);
        }
    }

    @Override
    public Pair<User, Account> findUserByApiKey(String apiKey) {
        return _accountDao.findUserAccountByApiKey(apiKey);
    }

    @Override
    public Pair<Boolean, Map<String, String>> getKeys(GetUserKeysCmd cmd) {
        final long userId = cmd.getID();
        return getKeys(userId);
    }

    @Override
    public Pair<Boolean, Map<String, String>> getKeys(Long userId) {
        User user = getActiveUser(userId);
        if (user == null) {
            throw new InvalidParameterValueException("Unable to find user by id");
        }
        final Account account = getAccount(getUserAccountById(userId).getAccountId()); //Extracting the Account from the userID of the requested user.
        User caller = CallContext.current().getCallingUser();
        checkAccess(caller, account);
        verifyCallerPrivilegeForUserOrAccountOperations(user);

        Map<String, String> keys = new HashMap<>();
        keys.put("apikey", user.getApiKey());
        keys.put("secretkey", user.getSecretKey());

        Boolean apiKeyAccess = user.getApiKeyAccess();
        if (apiKeyAccess == null) {
            apiKeyAccess = account.getApiKeyAccess();
            if (apiKeyAccess == null) {
                apiKeyAccess = AccountManagerImpl.apiKeyAccess.valueIn(account.getDomainId());
            }
        }

        return new Pair<>(apiKeyAccess, keys);
    }

    protected void preventRootDomainAdminAccessToRootAdminKeys(User caller, ControlledEntity account) {
        if (isDomainAdminForRootDomain(caller) && isRootAdmin(account.getAccountId())) {
            String msg = String.format("Caller Username %s does not have access to root admin keys", caller.getUsername());
            logger.error(msg);
            throw new PermissionDeniedException(msg);
        }
    }

    protected boolean isDomainAdminForRootDomain(User callingUser) {
        AccountVO caller = _accountDao.findById(callingUser.getAccountId());
        return caller.getType() == Account.Type.DOMAIN_ADMIN && caller.getDomainId() == Domain.ROOT_DOMAIN;
    }

    @Override
    public List<UserTwoFactorAuthenticator> listUserTwoFactorAuthenticationProviders() {
        return userTwoFactorAuthenticationProviders;
    }

    @Override
    public UserTwoFactorAuthenticator getUserTwoFactorAuthenticationProvider(Long domainId) {
        final String name = userTwoFactorAuthenticationDefaultProvider.valueIn(domainId);
        return getUserTwoFactorAuthenticationProvider(name);
    }

    public UserTwoFactorAuthenticator getUserTwoFactorAuthenticationProvider(final String name) {
        if (StringUtils.isEmpty(name)) {
            throw new CloudRuntimeException("Two factor authentication provider name is empty");
        }
        if (!userTwoFactorAuthenticationProvidersMap.containsKey(name.toLowerCase())) {
            throw new CloudRuntimeException(String.format("Failed to find two factor authentication provider by the name: %s.", name));
        }
        return userTwoFactorAuthenticationProvidersMap.get(name.toLowerCase());
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_REGISTER_FOR_SECRET_API_KEY, eventDescription = "register for the developer API keys")
    public String[] createApiKeyAndSecretKey(RegisterCmd cmd) {
        Account caller = getCurrentCallingAccount();
        final Long userId = cmd.getId();

        User user = getUserIncludingRemoved(userId);
        if (user == null) {
            throw new InvalidParameterValueException("unable to find user by id");
        }

        Account account = _accountDao.findById(user.getAccountId());
        checkAccess(caller, null, true, account);
        verifyCallerPrivilegeForUserOrAccountOperations(user);

        // don't allow updating system user
        if (user.getId() == User.UID_SYSTEM) {
            throw new PermissionDeniedException(String.format("user: %s is system account, update is not allowed", user));
        }
        // don't allow baremetal system user
        if (BaremetalUtils.BAREMETAL_SYSTEM_ACCOUNT_NAME.equals(user.getUsername())) {
            throw new PermissionDeniedException(String.format("user: %s is system account, update is not allowed", user));
        }

        // generate both an api key and a secret key, update the user table with the keys, return the keys to the user
        final String[] keys = new String[2];
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                keys[0] = createUserApiKey(userId);
                keys[1] = createUserSecretKey(userId);
            }
        });

        return keys;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_REGISTER_FOR_SECRET_API_KEY, eventDescription = "register for the developer API keys")
    public String[] createApiKeyAndSecretKey(final long userId) {
        Account caller = getCurrentCallingAccount();
        User user = getUserIncludingRemoved(userId);
        if (user == null) {
            throw new InvalidParameterValueException("Unable to find user by id");
        }
        Account account = _accountDao.findById(user.getAccountId());
        checkAccess(caller, null, true, account);
        final String[] keys = new String[2];
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                keys[0] = AccountManagerImpl.this.createUserApiKey(userId);
                keys[1] = AccountManagerImpl.this.createUserSecretKey(userId);
            }
        });
        return keys;
    }

    private String createUserApiKey(long userId) {
        try {
            UserVO updatedUser = _userDao.createForUpdate();

            String encodedKey;
            Pair<User, Account> userAcct;
            int retryLimit = 10;
            do {
                // FIXME: what algorithm should we use for API keys?
                KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
                SecretKey key = generator.generateKey();
                encodedKey = Base64.encodeBase64URLSafeString(key.getEncoded());
                userAcct = _accountDao.findUserAccountByApiKey(encodedKey);
                retryLimit--;
            } while ((userAcct != null) && (retryLimit >= 0));

            if (userAcct != null) {
                return null;
            }
            updatedUser.setApiKey(encodedKey);
            _userDao.update(userId, updatedUser);
            return encodedKey;
        } catch (NoSuchAlgorithmException ex) {
            logger.error("error generating secret key for user {}", _userAccountDao.findById(userId), ex);
        }
        return null;
    }

    private String createUserSecretKey(long userId) {
        try {
            UserVO updatedUser = _userDao.createForUpdate();
            String encodedKey;
            int retryLimit = 10;
            UserVO userBySecretKey;
            do {
                KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
                SecretKey key = generator.generateKey();
                encodedKey = Base64.encodeBase64URLSafeString(key.getEncoded());
                userBySecretKey = _userDao.findUserBySecretKey(encodedKey);
                retryLimit--;
            } while ((userBySecretKey != null) && (retryLimit >= 0));

            if (userBySecretKey != null) {
                return null;
            }

            updatedUser.setSecretKey(encodedKey);
            _userDao.update(userId, updatedUser);
            return encodedKey;
        } catch (NoSuchAlgorithmException ex) {
            logger.error("error generating secret key for user {}", _userAccountDao.findById(userId), ex);
        }
        return null;
    }

    @Override
    public void buildACLSearchBuilder(SearchBuilder<? extends ControlledEntity> sb, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {

        if (sb.entity() instanceof IPAddressVO) {
            sb.and("accountIdIN", ((IPAddressVO)sb.entity()).getAllocatedToAccountId(), SearchCriteria.Op.IN);
            sb.and("domainId", ((IPAddressVO)sb.entity()).getAllocatedInDomainId(), SearchCriteria.Op.EQ);
        } else if (sb.entity() instanceof ProjectInvitationVO) {
            sb.and("accountIdIN", ((ProjectInvitationVO)sb.entity()).getForAccountId(), SearchCriteria.Op.IN);
            sb.and("domainId", ((ProjectInvitationVO)sb.entity()).getInDomainId(), SearchCriteria.Op.EQ);
        } else {
            sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
            sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        }

        if (((permittedAccounts.isEmpty()) && (domainId != null) && isRecursive)) {
            // if accountId isn't specified, we can do a domain match for the admin case if isRecursive is true
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);

            if (sb.entity() instanceof IPAddressVO) {
                sb.join("domainSearch", domainSearch, ((IPAddressVO)sb.entity()).getAllocatedInDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            } else if (sb.entity() instanceof ProjectInvitationVO) {
                sb.join("domainSearch", domainSearch, ((ProjectInvitationVO)sb.entity()).getInDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            } else {
                sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            }

        }
        if (listProjectResourcesCriteria != null) {
            SearchBuilder<AccountVO> accountSearch = _accountDao.createSearchBuilder();
            if (listProjectResourcesCriteria == Project.ListProjectResourcesCriteria.ListProjectResourcesOnly) {
                accountSearch.and("type", accountSearch.entity().getType(), SearchCriteria.Op.EQ);
            } else if (listProjectResourcesCriteria == Project.ListProjectResourcesCriteria.SkipProjectResources) {
                accountSearch.and("type", accountSearch.entity().getType(), SearchCriteria.Op.NEQ);
            }

            if (sb.entity() instanceof IPAddressVO) {
                sb.join("accountSearch", accountSearch, ((IPAddressVO)sb.entity()).getAllocatedToAccountId(), accountSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            } else if (sb.entity() instanceof ProjectInvitationVO) {
                sb.join("accountSearch", accountSearch, ((ProjectInvitationVO)sb.entity()).getForAccountId(), accountSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            } else {
                sb.join("accountSearch", accountSearch, sb.entity().getAccountId(), accountSearch.entity().getId(), JoinBuilder.JoinType.INNER);
            }
        }
    }

    @Override
    public void buildACLSearchCriteria(SearchCriteria<? extends ControlledEntity> sc, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {

        if (listProjectResourcesCriteria != null) {
            sc.setJoinParameters("accountSearch", "type", Account.Type.PROJECT);
        }

        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountIdIN", permittedAccounts.toArray());
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            if (isRecursive) {
                sc.setJoinParameters("domainSearch", "path", domain.getPath() + "%");
            } else {
                sc.setParameters("domainId", domainId);
            }
        }
    }

    //TODO: deprecate this to use the new buildACLSearchParameters with permittedDomains, permittedAccounts, and permittedResources as return
    @Override
    public void buildACLSearchParameters(Account caller, Long id, String accountName, Long projectId, List<Long> permittedAccounts,
            Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject, boolean listAll, boolean forProjectInvitation) {
        Long domainId = domainIdRecursiveListProject.first();
        if (domainId != null) {
            Domain domain = _domainDao.findById(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find domain by id " + domainId);
            }
            // check permissions
            checkAccess(caller, domain);
        }

        if (accountName != null) {
            if (projectId != null) {
                throw new InvalidParameterValueException("Account and projectId can't be specified together");
            }

            Account userAccount;
            Domain domain;
            if (domainId != null) {
                userAccount = _accountDao.findActiveAccount(accountName, domainId);
                domain = _domainDao.findById(domainId);
            } else {
                userAccount = _accountDao.findActiveAccount(accountName, caller.getDomainId());
                domain = _domainDao.findById(caller.getDomainId());
            }

            if (userAccount != null) {
                checkAccess(caller, null, false, userAccount);
                // check permissions
                permittedAccounts.add(userAccount.getId());
            } else {
                throw new InvalidParameterValueException("could not find account " + accountName + " in domain " + domain);
            }
        }

        // set project information
        if (projectId != null) {
            if (!forProjectInvitation) {
                if (projectId == -1L) {
                    domainIdRecursiveListProject.third(Project.ListProjectResourcesCriteria.ListProjectResourcesOnly);
                    if (caller.getType() != Account.Type.ADMIN) {
                        permittedAccounts.addAll(_projectMgr.listPermittedProjectAccounts(caller.getId()));
                        // permittedAccounts can be empty when the caller is not a part of any project (a domain account)
                        if (permittedAccounts.isEmpty() || listAll) {
                            permittedAccounts.add(caller.getId());
                        }
                    }
                    if (listAll) {
                        domainIdRecursiveListProject.third(ListProjectResourcesCriteria.ListAllIncludingProjectResources);
                    }
                } else {
                    Project project = _projectMgr.getProject(projectId);
                    if (project == null) {
                        throw new InvalidParameterValueException("Unable to find project by id " + projectId);
                    }
                    if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                        throw new PermissionDeniedException("Account " + caller + " can't access project id=" + projectId);
                    }
                    permittedAccounts.add(project.getProjectAccountId());
                }
            }
        } else {
            if (id == null) {
                domainIdRecursiveListProject.third(Project.ListProjectResourcesCriteria.SkipProjectResources);
            }
            if (permittedAccounts.isEmpty() && domainId == null) {
                if (caller.getType() == Account.Type.NORMAL) {
                    permittedAccounts.add(caller.getId());
                } else if (!listAll) {
                    if (id == null) {
                        permittedAccounts.add(caller.getId());
                    } else if (caller.getType() != Account.Type.ADMIN) {
                        domainIdRecursiveListProject.first(caller.getDomainId());
                        domainIdRecursiveListProject.second(true);
                    }
                } else if (domainId == null) {
                    if (caller.getType() == Account.Type.DOMAIN_ADMIN) {
                        domainIdRecursiveListProject.first(caller.getDomainId());
                        domainIdRecursiveListProject.second(true);
                    }
                }
            } else if (domainId != null) {
                if (caller.getType() == Account.Type.NORMAL) {
                    permittedAccounts.add(caller.getId());
                }
            }

        }

    }

    @Override
    public void buildACLViewSearchBuilder(SearchBuilder<? extends ControlledViewEntity> sb, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {

        sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);

        if (((permittedAccounts.isEmpty()) && (domainId != null) && isRecursive)) {
            // if accountId isn't specified, we can do a domain match for the
            // admin case if isRecursive is true
            sb.and("domainPath", sb.entity().getDomainPath(), SearchCriteria.Op.LIKE);
        }

        if (listProjectResourcesCriteria != null) {
            if (listProjectResourcesCriteria == Project.ListProjectResourcesCriteria.ListProjectResourcesOnly) {
                sb.and("accountType", sb.entity().getAccountType(), SearchCriteria.Op.EQ);
            } else if (listProjectResourcesCriteria == Project.ListProjectResourcesCriteria.SkipProjectResources) {
                sb.and("accountType", sb.entity().getAccountType(), SearchCriteria.Op.NEQ);
            }
        }

    }

    @Override
    public void buildACLViewSearchCriteria(SearchCriteria<? extends ControlledViewEntity> sc, Long domainId, boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {
        if (listProjectResourcesCriteria != null) {
            sc.setParameters("accountType", Account.Type.PROJECT);
        }

        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountIdIN", permittedAccounts.toArray());
        } else if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            if (isRecursive) {
                sc.setParameters("domainPath", domain.getPath() + "%");
            } else {
                sc.setParameters("domainId", domainId);
            }
        }

    }

    @Override
    public UserAccount getUserByApiKey(String apiKey) {
        return _userAccountDao.getUserByApiKey(apiKey);
    }

    @Override
    public List<String> listAclGroupsByAccount(Long accountId) {
        if (_querySelectors == null || _querySelectors.size() == 0) {
            return new ArrayList<>();
        }

        QuerySelector qs = _querySelectors.get(0);
        return qs.listAclGroupsByAccount(accountId);
    }

    @Override
    public Long finalyzeAccountId(final String accountName, final Long domainId, final Long projectId, final boolean enabledOnly) {
        if (accountName != null) {
            if (domainId == null) {
                throw new InvalidParameterValueException("Account must be specified with domainId parameter");
            }

            final Domain domain = _domainMgr.getDomain(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find domain by id");
            }

            final Account account = getActiveAccountByName(accountName, domainId);
            if (account != null && account.getType() != Account.Type.PROJECT) {
                if (!enabledOnly || account.getState() == Account.State.ENABLED) {
                    return account.getId();
                } else {
                    throw new PermissionDeniedException(String.format("Can't add resources to the account %s in state=%s as it's no longer active", account, account.getState()));
                }
            } else {
                // idList is not used anywhere, so removed it now
                // List<IdentityProxy> idList = new ArrayList<IdentityProxy>();
                // idList.add(new IdentityProxy("domain", domainId, "domainId"));
                throw new InvalidParameterValueException("Unable to find account by name " + accountName + " in domain with specified id");
            }
        }

        if (projectId != null) {
            final Project project = _projectMgr.getProject(projectId);
            if (project != null) {
                if (!enabledOnly || project.getState() == Project.State.Active) {
                    return project.getProjectAccountId();
                } else {
                    final PermissionDeniedException ex = new PermissionDeniedException(
                            "Can't add resources to the project with specified projectId in state=" + project.getState() + " as it's no longer active");
                    ex.addProxyObject(project.getUuid(), "projectId");
                    throw ex;
                }
            } else {
                throw new InvalidParameterValueException("Unable to find project by id");
            }
        }
        return null;
    }

    @Override
    public UserAccount getUserAccountById(Long userId) {
        UserAccount userAccount = _userAccountDao.findById(userId);
        Map<String, String> details = _userDetailsDao.listDetailsKeyPairs(userId);
        userAccount.setDetails(details);

        return userAccount;
    }

    @Override
    public void checkAccess(Account account, ServiceOffering so, DataCenter zone) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(account, so, zone)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Access granted to " + account + " to " + so + " by " + checker.getName());
                }
                return;
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + account + " has access to " + so);
    }

    @Override
    public void checkAccess(Account account, DiskOffering dof, DataCenter zone) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(account, dof, zone)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Access granted to " + account + " to " + dof + " by " + checker.getName());
                }
                return;
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + account + " has access to " + dof);
    }

    @Override
    public void checkAccess(Account account, NetworkOffering nof, DataCenter zone) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(account, nof, zone)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Access granted to " + account + " to " + nof + " by " + checker.getName());
                }
                return;
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + account + " has access to " + nof);
    }

    @Override
    public void checkAccess(Account account, VpcOffering vof, DataCenter zone) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(account, vof, zone)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Access granted to " + account + " to " + vof + " by " + checker.getName());
                }
                return;
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + account + " has access to " + vof);
    }

    @Override
    public void checkAccess(User user, ControlledEntity entity) throws PermissionDeniedException {
        for (SecurityChecker checker : _securityCheckers) {
            if (checker.checkAccess(user, entity)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Access granted to " + user + "to " + entity + "by " + checker.getName());
                }
                return;
            }
        }
        throw new PermissionDeniedException("There's no way to confirm " + user + " has access to " + entity);
    }

    @Override
    public String getConfigComponentName() {
        return AccountManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {UseSecretKeyInResponse, enableUserTwoFactorAuthentication,
                userTwoFactorAuthenticationDefaultProvider, mandateUserTwoFactorAuthentication, userTwoFactorAuthenticationIssuer, apiKeyAccess,
                userAllowMultipleAccounts, listOfRoleTypesAllowedForOperationsOfSameRoleType, allowOperationsOnUsersInSameAccount};
    }

    public List<UserTwoFactorAuthenticator> getUserTwoFactorAuthenticationProviders() {
        return userTwoFactorAuthenticationProviders;
    }

    public void setUserTwoFactorAuthenticationProviders(final List<UserTwoFactorAuthenticator> userTwoFactorAuthenticationProviders) {
        this.userTwoFactorAuthenticationProviders = userTwoFactorAuthenticationProviders;
    }

    protected void initializeUserTwoFactorAuthenticationProvidersMap() {
        if (userTwoFactorAuthenticationProviders != null) {
            for (final UserTwoFactorAuthenticator userTwoFactorAuthenticator : userTwoFactorAuthenticationProviders) {
                userTwoFactorAuthenticationProvidersMap.put(userTwoFactorAuthenticator.getName().toLowerCase(), userTwoFactorAuthenticator);
            }
        }
    }

    @Override
    public void verifyUsingTwoFactorAuthenticationCode(final String code, final Long domainId, final Long userAccountId) {

        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountService.getActiveAccountById(caller.getId());

        checkAccess(caller, null, true, owner);

        UserAccount userAccount = _accountService.getUserAccountById(userAccountId);
        if (!userAccount.isUser2faEnabled()) {
            throw new CloudRuntimeException(String.format("Two factor authentication is not enabled on the user: %s", userAccount.getUsername()));
        }
        if (StringUtils.isBlank(userAccount.getUser2faProvider()) || StringUtils.isBlank(userAccount.getKeyFor2fa())) {
            throw new CloudRuntimeException(String.format("Two factor authentication is not setup for the user: %s, please setup 2FA before verifying", userAccount.getUsername()));
        }

        UserTwoFactorAuthenticator userTwoFactorAuthenticator = getUserTwoFactorAuthenticator(domainId, userAccountId);
        try {
            userTwoFactorAuthenticator.check2FA(code, userAccount);
            UserDetailVO userDetailVO = _userDetailsDao.findDetail(userAccountId, UserDetailVO.Setup2FADetail);
            if (userDetailVO != null) {
                userDetailVO.setValue(UserAccountVO.Setup2FAstatus.VERIFIED.name());
                _userDetailsDao.update(userDetailVO.getId(), userDetailVO);
            }
        } catch (CloudTwoFactorAuthenticationException e) {
            UserDetailVO userDetailVO = _userDetailsDao.findDetail(userAccountId, UserDetailVO.Setup2FADetail);
            if (userDetailVO != null && userDetailVO.getValue().equals(UserAccountVO.Setup2FAstatus.ENABLED.name())) {
                disableTwoFactorAuthentication(userAccountId, caller, owner);
            }
            throw e;
        }
    }

    @Override
    public UserTwoFactorAuthenticator getUserTwoFactorAuthenticator(Long domainId, Long userAccountId) {
        if (userAccountId != null) {
            UserAccount userAccount = _accountService.getUserAccountById(userAccountId);
            String user2FAProvider = userAccount.getUser2faProvider();
            if (user2FAProvider != null) {
                return getUserTwoFactorAuthenticator(user2FAProvider);
            }
        }
        final String name = userTwoFactorAuthenticationDefaultProvider.valueIn(domainId);
        return getUserTwoFactorAuthenticator(name);
    }

    @Override
    public UserTwoFactorAuthenticationSetupResponse setupUserTwoFactorAuthentication(SetupUserTwoFactorAuthenticationCmd cmd) {
        String providerName = cmd.getProvider();

        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountService.getActiveAccountById(caller.getId());

        if (Boolean.TRUE.equals(cmd.getEnable())) {
            checkAccess(caller, null, true, owner);
            Long userId = CallContext.current().getCallingUserId();

            return enableTwoFactorAuthentication(userId, providerName);
        }

        // Admin can disable 2FA of the users
        Long userId = cmd.getUserId();
        return disableTwoFactorAuthentication(userId, caller, owner);
    }

    protected UserTwoFactorAuthenticationSetupResponse enableTwoFactorAuthentication(Long userId, String providerName) {
        UserAccountVO userAccount = _userAccountDao.findById(userId);
        UserVO userVO = _userDao.findById(userId);
        Long domainId = userAccount.getDomainId();
        if (Boolean.FALSE.equals(enableUserTwoFactorAuthentication.valueIn(domainId)) && Boolean.FALSE.equals(mandateUserTwoFactorAuthentication.valueIn(domainId))) {
            throw new CloudRuntimeException("2FA is not enabled for this domain or at global level");
        }

        if (StringUtils.isEmpty(providerName)) {
            providerName = userTwoFactorAuthenticationDefaultProvider.valueIn(domainId);
            logger.debug(String.format("Provider name is not given to setup 2FA, so using the default 2FA provider %s", providerName));
        }

        UserTwoFactorAuthenticator provider = getUserTwoFactorAuthenticationProvider(providerName);
        String code = provider.setup2FAKey(userAccount);
        UserVO user = _userDao.createForUpdate();
        user.setKeyFor2fa(code);
        user.setUser2faProvider(provider.getName());
        user.setUser2faEnabled(true);
        _userDao.update(userId, user);

        // 2FA setup will be complete only upon successful verification with 2FA code
        UserDetailVO setup2FAstatus = new UserDetailVO(userId, UserDetailVO.Setup2FADetail, UserAccountVO.Setup2FAstatus.ENABLED.name());
        _userDetailsDao.persist(setup2FAstatus);

        UserTwoFactorAuthenticationSetupResponse response = new UserTwoFactorAuthenticationSetupResponse();
        response.setId(userVO.getUuid());
        response.setUsername(userAccount.getUsername());
        response.setSecretCode(code);

        return response;
    }

    protected UserTwoFactorAuthenticationSetupResponse disableTwoFactorAuthentication(Long userId, Account caller, Account owner) {
        UserVO userVO;
        if (userId != null) {
            userVO = validateUser(userId);
            owner = _accountService.getActiveAccountById(userVO.getAccountId());
        } else {
            userId = CallContext.current().getCallingUserId();
            userVO = _userDao.findById(userId);
        }
        checkAccess(caller, null, true, owner);

        UserVO user = _userDao.createForUpdate();
        user.setKeyFor2fa(null);
        user.setUser2faProvider(null);
        user.setUser2faEnabled(false);
        _userDao.update(userVO.getId(), user);
        _userDetailsDao.removeDetail(userId, UserDetailVO.Setup2FADetail);

        UserTwoFactorAuthenticationSetupResponse response = new UserTwoFactorAuthenticationSetupResponse();
        response.setId(userVO.getUuid());
        response.setUsername(userVO.getUsername());

        return response;
    }

    private UserVO validateUser(Long userId) {
        UserVO user = null;
        if (userId != null) {
            user = _userDao.findById(userId);
            if (user == null) {
                throw new InvalidParameterValueException("Invalid user ID provided");
            }
        }
        return user;
    }

    public UserTwoFactorAuthenticator getUserTwoFactorAuthenticator(final String name) {
        if (StringUtils.isEmpty(name)) {
            throw new CloudRuntimeException("UserTwoFactorAuthenticator name provided is empty");
        }
        if (!userTwoFactorAuthenticationProvidersMap.containsKey(name.toLowerCase())) {
            throw new CloudRuntimeException(String.format("Failed to find UserTwoFactorAuthenticator by the name: %s.", name));
        }
        return userTwoFactorAuthenticationProvidersMap.get(name.toLowerCase());
    }

    @Override
    public UserAccount clearUserTwoFactorAuthenticationInSetupStateOnLogin(UserAccount user) {
        return Transaction.execute((TransactionCallback<UserAccount>) status -> {
            if (!user.isUser2faEnabled() && StringUtils.isBlank(user.getUser2faProvider())) {
                return user;
            }
            UserDetailVO userDetailVO = _userDetailsDao.findDetail(user.getId(), UserDetailVO.Setup2FADetail);
            if (userDetailVO != null && UserAccountVO.Setup2FAstatus.VERIFIED.name().equals(userDetailVO.getValue())) {
                return user;
            }
            logger.info("Clearing 2FA configurations for {} as it is still in setup on a new login request", user);
            if (userDetailVO != null) {
                _userDetailsDao.remove(userDetailVO.getId());
            }
            UserAccountVO userAccountVO = _userAccountDao.findById(user.getId());
            userAccountVO.setUser2faEnabled(false);
            userAccountVO.setUser2faProvider(null);
            userAccountVO.setKeyFor2fa(null);
            _userAccountDao.update(user.getId(), userAccountVO);
            return userAccountVO;
        });
    }

    void assertUserNotAlreadyInAccount(User existingUser, Account newAccount) {
        System.out.println(existingUser.getAccountId());
        System.out.println(newAccount.getId());
        if (existingUser.getAccountId() == newAccount.getId()) {
            AccountVO existingAccount = _accountDao.findById(newAccount.getId());
            throw new InvalidParameterValueException(String.format("Username [%s] already exists in account [id=%s,name=%s]", existingUser.getUsername(), existingAccount.getUuid(), existingAccount.getAccountName()));
        }
    }

    void assertUserNotAlreadyInDomain(User existingUser, Account originalAccount) {
        Account existingAccount = _accountDao.findById(existingUser.getAccountId());
        if (existingAccount.getDomainId() == originalAccount.getDomainId()) {
            DomainVO existingDomain = _domainDao.findById(existingAccount.getDomainId());
            throw new InvalidParameterValueException(String.format("Username [%s] already exists in domain [id=%s,name=%s] user account [id=%s,name=%s]", existingUser.getUsername(), existingDomain.getUuid(), existingDomain.getName(), existingAccount.getUuid(), existingAccount.getAccountName()));
        }
    }
}
