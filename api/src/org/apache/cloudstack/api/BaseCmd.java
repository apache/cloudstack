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

package org.apache.cloudstack.api;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.affinity.AffinityGroupService;
import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.element.InternalLoadBalancerElementService;
import org.apache.cloudstack.network.lb.ApplicationLoadBalancerService;
import org.apache.cloudstack.network.lb.InternalLoadBalancerVMService;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.usage.UsageService;
import org.apache.log4j.Logger;

import com.cloud.configuration.ConfigurationService;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.NetworkUsageService;
import com.cloud.network.StorageNetworkService;
import com.cloud.network.VpcVirtualNetworkApplianceService;
import com.cloud.network.as.AutoScaleService;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.lb.LoadBalancingRulesService;
import com.cloud.network.rules.RulesService;
import com.cloud.network.security.SecurityGroupService;
import com.cloud.network.vpc.NetworkACLService;
import com.cloud.network.vpc.VpcProvisioningService;
import com.cloud.network.vpc.VpcService;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.network.vpn.Site2SiteVpnService;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectService;
import com.cloud.resource.ResourceService;
import com.cloud.server.ManagementService;
import com.cloud.server.ResourceMetaDataService;
import com.cloud.server.TaggedResourceService;
import com.cloud.storage.DataStoreProviderApiService;
import com.cloud.storage.StorageService;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.snapshot.SnapshotApiService;
import com.cloud.template.TemplateApiService;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.DomainService;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.ReflectUtil;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.UUIDManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.snapshot.VMSnapshotService;

public abstract class BaseCmd {
    private static final Logger s_logger = Logger.getLogger(BaseCmd.class.getName());

    public static final String USER_ERROR_MESSAGE = "Internal error executing command, please contact your system administrator";
    public static final int PROGRESS_INSTANCE_CREATED = 1;

    public static final String RESPONSE_TYPE_XML = "xml";
    public static final String RESPONSE_TYPE_JSON = "json";

    public enum CommandType {
        BOOLEAN, DATE, FLOAT, INTEGER, SHORT, LIST, LONG, OBJECT, MAP, STRING, TZDATE, UUID
    }

    public static final DateFormat INPUT_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat NEW_INPUT_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static Pattern newInputDateFormat = Pattern.compile("[\\d]+-[\\d]+-[\\d]+ [\\d]+:[\\d]+:[\\d]+");
    private static final DateFormat s_outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    protected static final Map<Class<?>, List<Field>> fieldsForCmdClass = new HashMap<Class<?>, List<Field>>();

    private Object _responseObject = null;
    private Map<String, String> fullUrlParams;

    public enum HTTPMethod {
        GET, POST, PUT, DELETE
    }

    private HTTPMethod httpMethod;

    @Parameter(name = "response", type = CommandType.STRING)
    private String responseType;

    @Inject
    public ConfigurationService _configService;
    @Inject
    public AccountService _accountService;
    @Inject
    public UserVmService _userVmService;
    @Inject
    public ManagementService _mgr;
    @Inject
    public StorageService _storageService;
    @Inject
    public VolumeApiService _volumeService;
    @Inject
    public ResourceService _resourceService;
    @Inject
    public NetworkService _networkService;
    @Inject
    public TemplateApiService _templateService;
    @Inject
    public SecurityGroupService _securityGroupService;
    @Inject
    public SnapshotApiService _snapshotService;
    @Inject
    public VpcVirtualNetworkApplianceService _routerService;
    @Inject
    public ResponseGenerator _responseGenerator;
    @Inject
    public EntityManager _entityMgr;
    @Inject
    public RulesService _rulesService;
    @Inject
    public AutoScaleService _autoScaleService;
    @Inject
    public LoadBalancingRulesService _lbService;
    @Inject
    public RemoteAccessVpnService _ravService;
    @Inject
    public ProjectService _projectService;
    @Inject
    public FirewallService _firewallService;
    @Inject
    public DomainService _domainService;
    @Inject
    public ResourceLimitService _resourceLimitService;
    @Inject
    public StorageNetworkService _storageNetworkService;
    @Inject
    public TaggedResourceService _taggedResourceService;
    @Inject
    public ResourceMetaDataService _resourceMetaDataService;
    @Inject
    public VpcService _vpcService;
    @Inject
    public NetworkACLService _networkACLService;
    @Inject
    public Site2SiteVpnService _s2sVpnService;

    @Inject
    public QueryService _queryService;
    @Inject
    public UsageService _usageService;
    @Inject
    public NetworkUsageService _networkUsageService;
    @Inject
    public VMSnapshotService _vmSnapshotService;
    @Inject
    public DataStoreProviderApiService dataStoreProviderApiService;
    @Inject
    public VpcProvisioningService _vpcProvSvc;
    @Inject
    public ApplicationLoadBalancerService _newLbSvc;
    @Inject
    public ApplicationLoadBalancerService _appLbService;
    @Inject
    public AffinityGroupService _affinityGroupService;
    @Inject
    public InternalLoadBalancerElementService _internalLbElementSvc;
    @Inject
    public InternalLoadBalancerVMService _internalLbSvc;
    @Inject
    public NetworkModel _ntwkModel;
    @Inject
    public AlertService _alertSvc;
    @Inject
    public UUIDManager _uuidMgr;

    public abstract void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException, NetworkRuleConflictException;

    public void configure() {
    }

    public HTTPMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(final String method) {
        if (method != null) {
            if (method.equalsIgnoreCase("GET"))
                httpMethod = HTTPMethod.GET;
            else if (method.equalsIgnoreCase("PUT"))
                httpMethod = HTTPMethod.PUT;
            else if (method.equalsIgnoreCase("POST"))
                httpMethod = HTTPMethod.POST;
            else if (method.equalsIgnoreCase("DELETE"))
                httpMethod = HTTPMethod.DELETE;
        } else {
            httpMethod = HTTPMethod.GET;
        }
    }

    public String getResponseType() {
        if (responseType == null) {
            return RESPONSE_TYPE_XML;
        }
        return responseType;
    }

    public void setResponseType(final String responseType) {
        this.responseType = responseType;
    }

    public abstract String getCommandName();

    /**
     * For commands the API framework needs to know the owner of the object being acted upon. This method is
     * used to determine that information.
     *
     * @return the id of the account that owns the object being acted upon
     */
    public abstract long getEntityOwnerId();

    public Object getResponseObject() {
        return _responseObject;
    }

    public void setResponseObject(final Object responseObject) {
        _responseObject = responseObject;
    }

    public ManagementService getMgmtServiceRef() {
        return _mgr;
    }

    public static String getDateString(final Date date) {
        if (date == null) {
            return "";
        }
        String formattedString = null;
        synchronized (s_outputFormat) {
            formattedString = s_outputFormat.format(date);
        }
        return formattedString;
    }

    protected List<Field> getAllFieldsForClass(final Class<?> clazz) {
        List<Field> filteredFields = fieldsForCmdClass.get(clazz);

        // If list of fields was not cached yet
        if (filteredFields == null) {
            final List<Field> allFields = ReflectUtil.getAllFieldsForClass(this.getClass(), BaseCmd.class);
            filteredFields = new ArrayList<Field>();

            for (final Field field : allFields) {
                final Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
                if ((parameterAnnotation != null) && parameterAnnotation.expose()) {
                    filteredFields.add(field);
                }
            }

            // Cache the prepared list for future use
            fieldsForCmdClass.put(clazz, filteredFields);
        }
        return filteredFields;
    }

    protected Account getCurrentContextAccount() {
        return CallContext.current().getCallingAccount();
    }

    /**
     * this method doesn't return all the @{link Parameter}, but only the ones exposed
     * and allowed for current @{link RoleType}
     *
     * @return
     */
    public List<Field> getParamFields() {
        final List<Field> allFields = getAllFieldsForClass(this.getClass());
        final List<Field> validFields = new ArrayList<Field>();
        final Account caller = getCurrentContextAccount();

        for (final Field field : allFields) {
            final Parameter parameterAnnotation = field.getAnnotation(Parameter.class);

            //TODO: Annotate @Validate on API Cmd classes, FIXME how to process Validate
            final RoleType[] allowedRoles = parameterAnnotation.authorized();
            boolean roleIsAllowed = true;
            if (allowedRoles.length > 0) {
                roleIsAllowed = false;
                for (final RoleType allowedRole : allowedRoles) {
                    if (allowedRole.getValue() == caller.getType()) {
                        roleIsAllowed = true;
                        break;
                    }
                }
            }

            if (roleIsAllowed) {
                validFields.add(field);
            } else {
                s_logger.debug("Ignoring paremeter " + parameterAnnotation.name() + " as the caller is not authorized to pass it in");
            }
        }

        return validFields;
    }

    protected long getInstanceIdFromJobSuccessResult(final String result) {
        s_logger.debug("getInstanceIdFromJobSuccessResult not overridden in subclass " + this.getClass().getName());
        return 0;
    }

    public static boolean isAdmin(final short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) || (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) ||
            (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    public static boolean isRootAdmin(final short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN));
    }

    public void setFullUrlParams(final Map<String, String> map) {
        fullUrlParams = map;
    }

    public Map<String, String> getFullUrlParams() {
        return fullUrlParams;
    }

    public Long finalyzeAccountId(final String accountName, final Long domainId, final Long projectId, final boolean enabledOnly) {
        if (accountName != null) {
            if (domainId == null) {
                throw new InvalidParameterValueException("Account must be specified with domainId parameter");
            }

            final Domain domain = _domainService.getDomain(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find domain by id");
            }

            final Account account = _accountService.getActiveAccountByName(accountName, domainId);
            if (account != null && account.getType() != Account.ACCOUNT_TYPE_PROJECT) {
                if (!enabledOnly || account.getState() == Account.State.enabled) {
                    return account.getId();
                } else {
                    throw new PermissionDeniedException("Can't add resources to the account id=" + account.getId() + " in state=" + account.getState() +
                        " as it's no longer active");
                }
            } else {
                // idList is not used anywhere, so removed it now
                //List<IdentityProxy> idList = new ArrayList<IdentityProxy>();
                //idList.add(new IdentityProxy("domain", domainId, "domainId"));
                throw new InvalidParameterValueException("Unable to find account by name " + accountName + " in domain with specified id");
            }
        }

        if (projectId != null) {
            final Project project = _projectService.getProject(projectId);
            if (project != null) {
                if (!enabledOnly || project.getState() == Project.State.Active) {
                    return project.getProjectAccountId();
                } else {
                    final PermissionDeniedException ex =
                        new PermissionDeniedException("Can't add resources to the project with specified projectId in state=" + project.getState() +
                            " as it's no longer active");
                    ex.addProxyObject(project.getUuid(), "projectId");
                    throw ex;
                }
            } else {
                throw new InvalidParameterValueException("Unable to find project by id");
            }
        }
        return null;
    }

    /**
     * To be overwritten by any class who needs specific validation
     */
    public void validateSpecificParameters(final Map<String, Object> params){
        // To be overwritten by any class who needs specific validation
    }
}
