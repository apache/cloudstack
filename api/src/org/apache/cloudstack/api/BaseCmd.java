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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.cloudstack.affinity.AffinityGroupService;

import com.cloud.server.ResourceMetaDataService;

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
import com.cloud.network.vpc.NetworkACLService;
import com.cloud.network.lb.LoadBalancingRulesService;
import com.cloud.network.rules.RulesService;
import com.cloud.network.security.SecurityGroupService;
import com.cloud.network.vpc.VpcProvisioningService;
import com.cloud.network.vpc.VpcService;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.network.vpn.Site2SiteVpnService;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectService;
import com.cloud.resource.ResourceService;
import com.cloud.server.ManagementService;
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
import com.cloud.utils.db.EntityManager;
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
    private static final DateFormat _outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private Object _responseObject = null;
    private Map<String, String> fullUrlParams;

    public enum HTTPMethod {
        GET, POST, PUT, DELETE
    }
    private HTTPMethod httpMethod;

    @Parameter(name = "response", type = CommandType.STRING)
    private String responseType;

    @Inject public ConfigurationService _configService;
    @Inject public AccountService _accountService;
    @Inject public UserVmService _userVmService;
    @Inject public ManagementService _mgr;
    @Inject public StorageService _storageService;
    @Inject public VolumeApiService _volumeService;
    @Inject public ResourceService _resourceService;
    @Inject public NetworkService _networkService;
    @Inject public TemplateApiService _templateService;
    @Inject public SecurityGroupService _securityGroupService;
    @Inject public SnapshotApiService _snapshotService;
    @Inject public VpcVirtualNetworkApplianceService _routerService;
    @Inject public ResponseGenerator _responseGenerator;
    @Inject public EntityManager _entityMgr;
    @Inject public RulesService _rulesService;
    @Inject public AutoScaleService _autoScaleService;
    @Inject public LoadBalancingRulesService _lbService;
    @Inject public RemoteAccessVpnService _ravService;
    @Inject public ProjectService _projectService;
    @Inject public FirewallService _firewallService;
    @Inject public DomainService _domainService;
    @Inject public ResourceLimitService _resourceLimitService;
    @Inject public IdentityService _identityService;
    @Inject public StorageNetworkService _storageNetworkService;
    @Inject public TaggedResourceService _taggedResourceService;
    @Inject public ResourceMetaDataService _resourceMetaDataService;
    @Inject public VpcService _vpcService;
    @Inject public NetworkACLService _networkACLService;
    @Inject public Site2SiteVpnService _s2sVpnService;

    @Inject public QueryService _queryService;
    @Inject public UsageService _usageService;
    @Inject public NetworkUsageService _networkUsageService;
    @Inject public VMSnapshotService _vmSnapshotService;
    @Inject public DataStoreProviderApiService dataStoreProviderApiService;
    @Inject public VpcProvisioningService _vpcProvSvc;
    @Inject public ApplicationLoadBalancerService _newLbSvc;
    @Inject public ApplicationLoadBalancerService _appLbService;
    @Inject public AffinityGroupService _affinityGroupService;
    @Inject public InternalLoadBalancerElementService _internalLbElementSvc;
    @Inject public InternalLoadBalancerVMService _internalLbSvc;
    @Inject public NetworkModel _ntwkModel;

    public abstract void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException;

    public void configure() {
    }

    public HTTPMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String method) {
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

    public void setResponseType(String responseType) {
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

    public void setResponseObject(Object responseObject) {
        _responseObject = responseObject;
    }

    public ManagementService getMgmtServiceRef() {
        return _mgr;
    }

    public static String getDateString(Date date) {
        if (date == null) {
            return "";
        }
        String formattedString = null;
        synchronized (_outputFormat) {
            formattedString = _outputFormat.format(date);
        }
        return formattedString;
    }

    // FIXME: move this to a utils method so that maps can be unpacked and integer/long values can be appropriately cast
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map<String, Object> unpackParams(Map<String, String> params) {
        Map<String, Object> lowercaseParams = new HashMap<String, Object>();
        for (String key : params.keySet()) {
            int arrayStartIndex = key.indexOf('[');
            int arrayStartLastIndex = key.lastIndexOf('[');
            if (arrayStartIndex != arrayStartLastIndex) {
                throw new ServerApiException(ApiErrorCode.MALFORMED_PARAMETER_ERROR, "Unable to decode parameter " + key
                        + "; if specifying an object array, please use parameter[index].field=XXX, e.g. userGroupList[0].group=httpGroup");
            }

            if (arrayStartIndex > 0) {
                int arrayEndIndex = key.indexOf(']');
                int arrayEndLastIndex = key.lastIndexOf(']');
                if ((arrayEndIndex < arrayStartIndex) || (arrayEndIndex != arrayEndLastIndex)) {
                    // malformed parameter
                    throw new ServerApiException(ApiErrorCode.MALFORMED_PARAMETER_ERROR, "Unable to decode parameter " + key
                            + "; if specifying an object array, please use parameter[index].field=XXX, e.g. userGroupList[0].group=httpGroup");
                }

                // Now that we have an array object, check for a field name in the case of a complex object
                int fieldIndex = key.indexOf('.');
                String fieldName = null;
                if (fieldIndex < arrayEndIndex) {
                    throw new ServerApiException(ApiErrorCode.MALFORMED_PARAMETER_ERROR, "Unable to decode parameter " + key
                            + "; if specifying an object array, please use parameter[index].field=XXX, e.g. userGroupList[0].group=httpGroup");
                } else {
                    fieldName = key.substring(fieldIndex + 1);
                }

                // parse the parameter name as the text before the first '[' character
                String paramName = key.substring(0, arrayStartIndex);
                paramName = paramName.toLowerCase();

                Map<Integer, Map> mapArray = null;
                Map<String, Object> mapValue = null;
                String indexStr = key.substring(arrayStartIndex + 1, arrayEndIndex);
                int index = 0;
                boolean parsedIndex = false;
                try {
                    if (indexStr != null) {
                        index = Integer.parseInt(indexStr);
                        parsedIndex = true;
                    }
                } catch (NumberFormatException nfe) {
                    s_logger.warn("Invalid parameter " + key + " received, unable to parse object array, returning an error.");
                }

                if (!parsedIndex) {
                    throw new ServerApiException(ApiErrorCode.MALFORMED_PARAMETER_ERROR, "Unable to decode parameter " + key
                            + "; if specifying an object array, please use parameter[index].field=XXX, e.g. userGroupList[0].group=httpGroup");
                }

                Object value = lowercaseParams.get(paramName);
                if (value == null) {
                    // for now, assume object array with sub fields
                    mapArray = new HashMap<Integer, Map>();
                    mapValue = new HashMap<String, Object>();
                    mapArray.put(Integer.valueOf(index), mapValue);
                } else if (value instanceof Map) {
                    mapArray = (HashMap) value;
                    mapValue = mapArray.get(Integer.valueOf(index));
                    if (mapValue == null) {
                        mapValue = new HashMap<String, Object>();
                        mapArray.put(Integer.valueOf(index), mapValue);
                    }
                }

                // we are ready to store the value for a particular field into the map for this object
                mapValue.put(fieldName, params.get(key));

                lowercaseParams.put(paramName, mapArray);
            } else {
                lowercaseParams.put(key.toLowerCase(), params.get(key));
            }
        }
        return lowercaseParams;
    }

    protected long getInstanceIdFromJobSuccessResult(String result) {
        s_logger.debug("getInstanceIdFromJobSuccessResult not overridden in subclass " + this.getClass().getName());
        return 0;
    }

    public static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
                (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) ||
                (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    public static boolean isRootAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN));
    }

    public void setFullUrlParams(Map<String, String> map) {
        this.fullUrlParams = map;
    }

    public Map<String, String> getFullUrlParams() {
        return this.fullUrlParams;
    }

    public Long finalyzeAccountId(String accountName, Long domainId, Long projectId, boolean enabledOnly) {
        if (accountName != null) {
            if (domainId == null) {
                throw new InvalidParameterValueException("Account must be specified with domainId parameter");
            }

            Domain domain = _domainService.getDomain(domainId);
            if (domain == null) {
                throw new InvalidParameterValueException("Unable to find domain by id");
            }

            Account account = _accountService.getActiveAccountByName(accountName, domainId);
            if (account != null && account.getType() != Account.ACCOUNT_TYPE_PROJECT) {
                if (!enabledOnly || account.getState() == Account.State.enabled) {
                    return account.getId();
                } else {
                    throw new PermissionDeniedException("Can't add resources to the account id=" + account.getId() + " in state=" + account.getState() + " as it's no longer active");
                }
            } else {
                // idList is not used anywhere, so removed it now
                //List<IdentityProxy> idList = new ArrayList<IdentityProxy>();
                //idList.add(new IdentityProxy("domain", domainId, "domainId"));
                throw new InvalidParameterValueException("Unable to find account by name " + accountName + " in domain with specified id");
            }
        }

        if (projectId != null) {
            Project project = _projectService.getProject(projectId);
            if (project != null) {
                if (!enabledOnly || project.getState() == Project.State.Active) {
                    return project.getProjectAccountId();
                } else {
                    PermissionDeniedException ex = new PermissionDeniedException("Can't add resources to the project with specified projectId in state=" + project.getState() + " as it's no longer active");
                    ex.addProxyObject(project.getUuid(), "projectId");
                    throw ex;
                }
            } else {
                throw new InvalidParameterValueException("Unable to find project by id");
            }
        }
        return null;
    }
}
