/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.cloud.configuration.ConfigurationService;
import com.cloud.consoleproxy.ConsoleProxyService;
import com.cloud.dao.EntityManager;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkService;
import com.cloud.network.StorageNetworkService;
import com.cloud.network.VirtualNetworkApplianceService;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.lb.LoadBalancingRulesService;
import com.cloud.network.rules.RulesService;
import com.cloud.network.security.SecurityGroupService;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectService;
import com.cloud.resource.ResourceService;
import com.cloud.server.ManagementService;
import com.cloud.storage.StorageService;
import com.cloud.storage.snapshot.SnapshotService;
import com.cloud.template.TemplateService;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.DomainService;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.AnnotationHelper;
import com.cloud.vm.BareMetalVmService;
import com.cloud.vm.UserVmService;

public abstract class BaseCmd {
    private static final Logger s_logger = Logger.getLogger(BaseCmd.class.getName());

    public static final String USER_ERROR_MESSAGE = "Internal error executing command, please contact your system administrator";
    public static final int PROGRESS_INSTANCE_CREATED = 1;

    public static final String RESPONSE_TYPE_XML = "xml";
    public static final String RESPONSE_TYPE_JSON = "json";

    public enum CommandType {
        BOOLEAN, DATE, FLOAT, INTEGER, SHORT, LIST, LONG, OBJECT, MAP, STRING, TZDATE
    }

    // FIXME: Extract these out into a separate file
    // Client error codes
    public static final int MALFORMED_PARAMETER_ERROR = 430;
    public static final int PARAM_ERROR = 431;
    public static final int UNSUPPORTED_ACTION_ERROR = 432;
    public static final int PAGE_LIMIT_EXCEED = 433;

    // Server error codes
    public static final int INTERNAL_ERROR = 530;
    public static final int ACCOUNT_ERROR = 531;
    public static final int ACCOUNT_RESOURCE_LIMIT_ERROR = 532;
    public static final int INSUFFICIENT_CAPACITY_ERROR = 533;
    public static final int RESOURCE_UNAVAILABLE_ERROR = 534;
    public static final int RESOURCE_ALLOCATION_ERROR = 534;
    public static final int RESOURCE_IN_USE_ERROR = 536;
    public static final int NETWORK_RULE_CONFLICT_ERROR = 537;

    public static final DateFormat INPUT_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat NEW_INPUT_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static Pattern newInputDateFormat = Pattern.compile("[\\d]+-[\\d]+-[\\d]+ [\\d]+:[\\d]+:[\\d]+");
    private static final DateFormat _outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private Object _responseObject = null;
    private Map<String, String> fullUrlParams;

    @Parameter(name = "response", type = CommandType.STRING)
    private String responseType;

    public static ComponentLocator s_locator;
    public static ConfigurationService _configService;
    public static AccountService _accountService;
    public static UserVmService _userVmService;
    public static ManagementService _mgr;
    public static StorageService _storageService;
    public static ResourceService _resourceService;
    public static NetworkService _networkService;
    public static TemplateService _templateService;
    public static SecurityGroupService _securityGroupService;
    public static SnapshotService _snapshotService;
    public static ConsoleProxyService _consoleProxyService;
    public static VirtualNetworkApplianceService _routerService;
    public static ResponseGenerator _responseGenerator;
    public static EntityManager _entityMgr;
    public static RulesService _rulesService;
    public static LoadBalancingRulesService _lbService;
    public static RemoteAccessVpnService _ravService;
    public static BareMetalVmService _bareMetalVmService;
    public static ProjectService _projectService;
    public static FirewallService _firewallService;
    public static DomainService _domainService;
    public static ResourceLimitService _resourceLimitService;
    public static IdentityService _identityService;
    public static StorageNetworkService _storageNetworkService;

    static void setComponents(ResponseGenerator generator) {
        ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
        _mgr = (ManagementService) ComponentLocator.getComponent(ManagementService.Name);
        _accountService = locator.getManager(AccountService.class);
        _configService = locator.getManager(ConfigurationService.class);
        _userVmService = locator.getManager(UserVmService.class);
        _storageService = locator.getManager(StorageService.class);
        _resourceService = locator.getManager(ResourceService.class);
        _networkService = locator.getManager(NetworkService.class);
        _templateService = locator.getManager(TemplateService.class);
        _securityGroupService = locator.getManager(SecurityGroupService.class);
        _snapshotService = locator.getManager(SnapshotService.class);
        _consoleProxyService = locator.getManager(ConsoleProxyService.class);
        _routerService = locator.getManager(VirtualNetworkApplianceService.class);
        _entityMgr = locator.getManager(EntityManager.class);
        _rulesService = locator.getManager(RulesService.class);
        _lbService = locator.getManager(LoadBalancingRulesService.class);
        _ravService = locator.getManager(RemoteAccessVpnService.class);
        _responseGenerator = generator;
        _bareMetalVmService = locator.getManager(BareMetalVmService.class);
        _projectService = locator.getManager(ProjectService.class);
        _firewallService = locator.getManager(FirewallService.class);
        _domainService = locator.getManager(DomainService.class);
        _resourceLimitService = locator.getManager(ResourceLimitService.class);
        _identityService = locator.getManager(IdentityService.class);
        _storageNetworkService = locator.getManager(StorageNetworkService.class);
    }

    public abstract void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException;

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
                throw new ServerApiException(MALFORMED_PARAMETER_ERROR, "Unable to decode parameter " + key
                        + "; if specifying an object array, please use parameter[index].field=XXX, e.g. userGroupList[0].group=httpGroup");
            }

            if (arrayStartIndex > 0) {
                int arrayEndIndex = key.indexOf(']');
                int arrayEndLastIndex = key.lastIndexOf(']');
                if ((arrayEndIndex < arrayStartIndex) || (arrayEndIndex != arrayEndLastIndex)) {
                    // malformed parameter
                    throw new ServerApiException(MALFORMED_PARAMETER_ERROR, "Unable to decode parameter " + key
                            + "; if specifying an object array, please use parameter[index].field=XXX, e.g. userGroupList[0].group=httpGroup");
                }

                // Now that we have an array object, check for a field name in the case of a complex object
                int fieldIndex = key.indexOf('.');
                String fieldName = null;
                if (fieldIndex < arrayEndIndex) {
                    throw new ServerApiException(MALFORMED_PARAMETER_ERROR, "Unable to decode parameter " + key
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
                    throw new ServerApiException(MALFORMED_PARAMETER_ERROR, "Unable to decode parameter " + key
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

    public String buildResponse(ServerApiException apiException, String responseType) {
        StringBuffer sb = new StringBuffer();
        if (RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            // JSON response
            sb.append("{ \"" + getCommandName() + "\" : { " + "\"@attributes\":{\"cloud-stack-version\":\"" + _mgr.getVersion() + "\"},");
            sb.append("\"errorcode\" : \"" + apiException.getErrorCode() + "\", \"description\" : \"" + apiException.getDescription() + "\" } }");
        } else {
            sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            sb.append("<" + getCommandName() + ">");
            sb.append("<errorcode>" + apiException.getErrorCode() + "</errorcode>");
            sb.append("<description>" + escapeXml(apiException.getDescription()) + "</description>");
            sb.append("</" + getCommandName() + " cloud-stack-version=\"" + _mgr.getVersion() + "\">");
        }
        return sb.toString();
    }

    public String buildResponse(List<Pair<String, Object>> tagList, String responseType) {
        StringBuffer prefixSb = new StringBuffer();
        StringBuffer suffixSb = new StringBuffer();

        // set up the return value with the name of the response
        if (RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            prefixSb.append("{ \"" + getCommandName() + "\" : { \"@attributes\":{\"cloud-stack-version\":\"" + _mgr.getVersion() + "\"},");
        } else {
            prefixSb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            prefixSb.append("<" + getCommandName() + " cloud-stack-version=\"" + _mgr.getVersion() + "\">");
        }

        int i = 0;
        for (Pair<String, Object> tagData : tagList) {
            String tagName = tagData.first();
            Object tagValue = tagData.second();
            if (tagValue instanceof Object[]) {
                Object[] subObjects = (Object[]) tagValue;
                if (subObjects.length < 1) {
                    continue;
                }
                writeObjectArray(responseType, suffixSb, i++, tagName, subObjects);
            } else {
                writeNameValuePair(suffixSb, tagName, tagValue, responseType, i++);
            }
        }

        if (suffixSb.length() > 0) {
            if (RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) { // append comma only if we have some suffix else
// not as per strict Json syntax.
                prefixSb.append(",");
            }
            prefixSb.append(suffixSb);
        }
        // close the response
        if (RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            prefixSb.append("} }");
        } else {
            prefixSb.append("</" + getCommandName() + ">");
        }
        return prefixSb.toString();
    }

    private void writeNameValuePair(StringBuffer sb, String tagName, Object tagValue, String responseType, int propertyCount) {
        if (tagValue == null) {
            return;
        }

        if (tagValue instanceof Object[]) {
            Object[] subObjects = (Object[]) tagValue;
            if (subObjects.length < 1) {
                return;
            }
            writeObjectArray(responseType, sb, propertyCount, tagName, subObjects);
        } else {
            if (RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
                String seperator = ((propertyCount > 0) ? ", " : "");
                sb.append(seperator + "\"" + tagName + "\" : \"" + escapeJSON(tagValue.toString()) + "\"");
            } else {
                sb.append("<" + tagName + ">" + escapeXml(tagValue.toString()) + "</" + tagName + ">");
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void writeObjectArray(String responseType, StringBuffer sb, int propertyCount, String tagName, Object[] subObjects) {
        if (RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            String separator = ((propertyCount > 0) ? ", " : "");
            sb.append(separator);
        }
        int j = 0;
        for (Object subObject : subObjects) {
            if (subObject instanceof List) {
                List subObjList = (List) subObject;
                writeSubObject(sb, tagName, subObjList, responseType, j++);
            }
        }

        if (RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            sb.append("]");
        }
    }

    @SuppressWarnings("rawtypes")
    private void writeSubObject(StringBuffer sb, String tagName, List tagList, String responseType, int objectCount) {
        if (RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            sb.append(((objectCount == 0) ? "\"" + tagName + "\" : [  { " : ", { "));
        } else {
            sb.append("<" + tagName + ">");
        }

        int i = 0;
        for (Object tag : tagList) {
            if (tag instanceof Pair) {
                Pair nameValuePair = (Pair) tag;
                writeNameValuePair(sb, (String) nameValuePair.first(), nameValuePair.second(), responseType, i++);
            }
        }

        if (RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            sb.append("}");
        } else {
            sb.append("</" + tagName + ">");
        }
    }

    /**
     * Escape xml response set to false by default. API commands to override this method to allow escaping
     */
    public boolean requireXmlEscape() {
        return true;
    }

    private String escapeXml(String xml) {
        if (!requireXmlEscape()) {
            return xml;
        }
        int iLen = xml.length();
        if (iLen == 0) {
            return xml;
        }
        StringBuffer sOUT = new StringBuffer(iLen + 256);
        int i = 0;
        for (; i < iLen; i++) {
            char c = xml.charAt(i);
            if (c == '<') {
                sOUT.append("&lt;");
            } else if (c == '>') {
                sOUT.append("&gt;");
            } else if (c == '&') {
                sOUT.append("&amp;");
            } else if (c == '"') {
                sOUT.append("&quot;");
            } else if (c == '\'') {
                sOUT.append("&apos;");
            } else {
                sOUT.append(c);
            }
        }
        return sOUT.toString();
    }

    private static String escapeJSON(String str) {
        if (str == null) {
            return str;
        }

        return str.replace("\"", "\\\"");
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
                throw new InvalidParameterValueException("Unable to find domain by id=" + domainId);
            }

            Account account = _accountService.getActiveAccountByName(accountName, domainId);
            if (account != null && account.getType() != Account.ACCOUNT_TYPE_PROJECT) {
                if (!enabledOnly || account.getState() == Account.State.enabled) {
                    return account.getId();
                } else {
                    throw new PermissionDeniedException("Can't add resources to the account id=" + account.getId() + " in state=" + account.getState() + " as it's no longer active");                    
                }
            } else {
                throw new InvalidParameterValueException("Unable to find account by name " + accountName + " in domain id=" + domainId);
            }
        }

        if (projectId != null) {
            Project project = _projectService.getProject(projectId);
            if (project != null) {
                if (!enabledOnly || project.getState() == Project.State.Active) {
                    return project.getProjectAccountId();
                } else {
                	PermissionDeniedException ex = new PermissionDeniedException("Can't add resources to the project with specified projectId in state=" + project.getState() + " as it's no longer active");
                	ex.addProxyObject(project, projectId, "projectId");                    
                    throw ex;
                }
            } else {
            	InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find project with specified projectId");
            	ex.addProxyObject(project, projectId, "projectId");                
                throw ex;
            }
        }
        return null;
    }
}
