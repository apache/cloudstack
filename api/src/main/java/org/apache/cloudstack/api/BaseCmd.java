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

import org.apache.cloudstack.acl.ProjectRoleService;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.affinity.AffinityGroupService;
import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.annotation.AnnotationService;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.element.InternalLoadBalancerElementService;
import org.apache.cloudstack.network.lb.ApplicationLoadBalancerService;
import org.apache.cloudstack.network.lb.InternalLoadBalancerVMService;
import org.apache.cloudstack.network.lb.LoadBalancerConfigService;
import org.apache.cloudstack.query.QueryService;
import org.apache.cloudstack.storage.ImageStoreService;
import org.apache.cloudstack.usage.UsageService;
import org.apache.log4j.Logger;

import com.cloud.configuration.ConfigurationService;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Ipv6Service;
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
import com.cloud.projects.ProjectService;
import com.cloud.resource.ResourceService;
import com.cloud.server.ManagementService;
import com.cloud.server.ResourceIconManager;
import com.cloud.server.ResourceManagerUtil;
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
import com.cloud.utils.HttpUtils;
import com.cloud.utils.ReflectUtil;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.UUIDManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.snapshot.VMSnapshotService;

public abstract class BaseCmd {
    private static final Logger s_logger = Logger.getLogger(BaseCmd.class.getName());
    public static final String RESPONSE_SUFFIX = "response";
    public static final String RESPONSE_TYPE_XML = HttpUtils.RESPONSE_TYPE_XML;
    public static final String RESPONSE_TYPE_JSON = HttpUtils.RESPONSE_TYPE_JSON;
    public static final String USER_ERROR_MESSAGE = "Internal error executing command, please contact your system administrator";
    public static Pattern newInputDateFormat = Pattern.compile("[\\d]+-[\\d]+-[\\d]+ [\\d]+:[\\d]+:[\\d]+");
    private static final DateFormat s_outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    protected static final Map<Class<?>, List<Field>> fieldsForCmdClass = new HashMap<Class<?>, List<Field>>();

    public static enum HTTPMethod {
        GET, POST, PUT, DELETE
    }
    public static enum CommandType {
        BOOLEAN, DATE, FLOAT, DOUBLE, INTEGER, SHORT, LIST, LONG, OBJECT, MAP, STRING, UUID
    }

    private Object _responseObject;
    private Map<String, String> fullUrlParams;
    private HTTPMethod httpMethod;
    @Parameter(name = "response", type = CommandType.STRING)
    private String responseType;

    @Inject
    public ConfigurationService _configService;
    @Inject
    public AccountService _accountService;
    @Inject
    public RoleService roleService;
    @Inject
    public ProjectRoleService projRoleService;
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
    public ImageStoreService _imageStoreService;
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
    public LoadBalancerConfigService _lbConfigService;
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
    public ResourceManagerUtil resourceManagerUtil;
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
    @Inject
    public AnnotationService annotationService;
    @Inject
    public ResourceIconManager resourceIconManager;
    @Inject
    public Ipv6Service ipv6Service;

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

    /**
     * Gets the CommandName based on the class annotations: the value from {@link APICommand#name()}
     *
     * @return the value from {@link APICommand#name()}
     */
    public static String getCommandNameByClass(Class<?> clazz) {
        String cmdName = null;
        APICommand apiClassAnnotation = clazz.getAnnotation(APICommand.class);

        if (apiClassAnnotation != null && apiClassAnnotation.name() != null) {
            cmdName = apiClassAnnotation.name();
        } else {
            cmdName = clazz.getName();
        }
        return cmdName;
    }

    public String getActualCommandName() {
        return getCommandNameByClass(this.getClass());
    }

    public String getCommandName() {
        return getResponseNameByClass(this.getClass());
    }

    /**
     * Retrieves the name defined in {@link APICommand#name()}, in lower case, with the prefix {@link BaseCmd#RESPONSE_SUFFIX}
     */
    public static String getResponseNameByClass(Class<?> clazz) {
        return getCommandNameByClass(clazz).toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    /**
     * For commands the API framework needs to know the owner of the object being acted upon. This method is
     * used to determine that information.
     *
     * @return the id of the account that owns the object being acted upon
     */
    public abstract long getEntityOwnerId();

    public List<Long> getEntityOwnerIds() {
        return null;
    }

    public Object getResponseObject() {
        return _responseObject;
    }

    public void setResponseObject(final Object responseObject) {
        _responseObject = responseObject;
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

    /**
     * This method doesn't return all the @{link Parameter}, but only the ones exposed
     * and allowed for current @{link RoleType}. This method will get the fields for a given
     * Cmd class only once and never again, so in case of a dynamic update the result would
     * be obsolete (this might be a plugin update. It is agreed upon that we will not do
     * upgrades dynamically but in case we come back on that decision we need to revisit this)
     *
     * @return
     */
    public List<Field> getParamFields() {
        final List<Field> allFields = getAllFieldsForClass(this.getClass());
        final List<Field> validFields = new ArrayList<Field>();
        final Account caller = CallContext.current().getCallingAccount();

        for (final Field field : allFields) {
            final Parameter parameterAnnotation = field.getAnnotation(Parameter.class);

            //TODO: Annotate @Validate on API Cmd classes, FIXME how to process Validate
            final RoleType[] allowedRoles = parameterAnnotation.authorized();
            boolean roleIsAllowed = true;
            if (allowedRoles.length > 0) {
                roleIsAllowed = false;
                for (final RoleType allowedRole : allowedRoles) {
                    if (allowedRole.getAccountType() == caller.getType()) {
                        roleIsAllowed = true;
                        break;
                    }
                }
            }

            if (roleIsAllowed) {
                validFields.add(field);
            } else {
                s_logger.debug("Ignoring parameter " + parameterAnnotation.name() + " as the caller is not authorized to pass it in");
            }
        }

        return validFields;
    }

    public void setFullUrlParams(final Map<String, String> map) {
        fullUrlParams = map;
    }

    public Map<String, String> getFullUrlParams() {
        return fullUrlParams;
    }

    /**
     * To be overwritten by any class who needs specific validation
     */
    public void validateSpecificParameters(final Map<String, String> params){
        // To be overwritten by any class who needs specific validation
    }

    /**
     * display flag is used to control the display of the resource only to the end user. It doesn't affect Root Admin.
     * @return display flag
     */
    public boolean isDisplay(){
        CallContext context = CallContext.current();
        Map<Object, Object> contextMap = context.getContextParameters();
        boolean isDisplay = true;

        // Iterate over all the first class entities in context and check their display property.
        for(Map.Entry<Object, Object> entry : contextMap.entrySet()){
            try{
                Object key = entry.getKey();
                Class clz = Class.forName((String)key);
                if(Displayable.class.isAssignableFrom(clz)){
                    final Object objVO = getEntityVO(clz, entry.getValue());
                    isDisplay = ((Displayable) objVO).isDisplay();
                }

                // If the flag is false break immediately
                if(!isDisplay)
                    break;
            } catch (Exception e){
                s_logger.trace("Caught exception while checking first class entities for display property, continuing on", e);
            }
        }

        context.setEventDisplayEnabled(isDisplay);
        return isDisplay;

    }

    private Object getEntityVO(Class entityType, Object entityId){

        // entityId can be internal db id or UUID so accordingly call findbyId or findByUUID

        if (entityId instanceof Long){
            // Its internal db id - use findById
            return _entityMgr.findById(entityType, (Long)entityId);
        } else if(entityId instanceof String){
            try{
                // In case its an async job the internal db id would be a string because of json deserialization
                Long internalId = Long.valueOf((String) entityId);
                return _entityMgr.findById(entityType, internalId);
            } catch (NumberFormatException e){
               // It is uuid - use findByUuid`
               return _entityMgr.findByUuid(entityType, (String)entityId);
            }
        }

        return null;
    }

    /**
     * Commands that generate action events associated to a resource and
     * async commands that want to be tracked as part of the listXXX commands
     * need to provide implementations of the two following methods,
     * getApiResourceId() and getApiResourceType()
     *
     * getApiResourceId() should return the id of the object the async command is executing on
     * getApiResourceType() should return a type from the ApiCommandResourceType enumeration
     */
    public Long getApiResourceId() {
        return null;
    }

    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.None;
    }

}
