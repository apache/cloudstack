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
package org.apache.cloudstack.discovery;

import com.cloud.serializer.Param;
import com.cloud.server.ManagementServer;
import com.cloud.utils.ReflectUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.PluggableService;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ApiDiscoveryResponse;
import org.apache.cloudstack.api.response.ApiParameterResponse;
import org.apache.cloudstack.api.response.ApiResponseResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.log4j.Logger;

import javax.ejb.Local;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Local(value = ApiDiscoveryService.class)
public class ApiDiscoveryServiceImpl implements ApiDiscoveryService {
    private static final Logger s_logger = Logger.getLogger(ApiDiscoveryServiceImpl.class);

    private static Map<RoleType, List<ApiDiscoveryResponse>> _roleTypeDiscoveryResponseListMap;

    private static Map<String, ApiDiscoveryResponse> _apiNameDiscoveryResponseMap =
            new HashMap<String, ApiDiscoveryResponse>();

    private static Map<String, List<RoleType>> _apiNameRoleTypeListMap = null;

    protected ApiDiscoveryServiceImpl() {
        super();
        if (_roleTypeDiscoveryResponseListMap == null) {
            long startTime = System.nanoTime();
            _roleTypeDiscoveryResponseListMap = new HashMap<RoleType, List<ApiDiscoveryResponse>>();
            for (RoleType roleType: RoleType.values())
                _roleTypeDiscoveryResponseListMap.put(roleType, new ArrayList<ApiDiscoveryResponse>());
            cacheResponseMap();
            long endTime = System.nanoTime();
            s_logger.info("Api Discovery Service: Annotation, docstrings, api relation graph processed in " + (endTime - startTime) / 1000000.0 + " ms");
        }
    }

    private Map<String, List<RoleType>> getApiNameRoleTypeListMap() {
        Map<String, List<RoleType>> apiNameRoleTypeMap = new HashMap<String, List<RoleType>>();
        ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
        List<PluggableService> services = locator.getAllPluggableServices();
        services.add((PluggableService) ComponentLocator.getComponent(ManagementServer.Name));
        for (PluggableService service : services) {
            for (Map.Entry<String, String> entry: service.getProperties().entrySet()) {
                String apiName = entry.getKey();
                String roleMask = entry.getValue();
                try {
                    short cmdPermissions = Short.parseShort(roleMask);
                    if (!apiNameRoleTypeMap.containsKey(apiName))
                        apiNameRoleTypeMap.put(apiName, new ArrayList<RoleType>());
                    for (RoleType roleType: RoleType.values()) {
                        if ((cmdPermissions & roleType.getValue()) != 0)
                            apiNameRoleTypeMap.get(apiName).add(roleType);
                    }
                } catch (NumberFormatException nfe) {
                }
            }
        }
        return apiNameRoleTypeMap;
    }

    private void cacheResponseMap() {
        Set<Class<?>> cmdClasses = ReflectUtil.getClassesWithAnnotation(APICommand.class,
                new String[]{"org.apache.cloudstack.api", "com.cloud.api"});

        Map<String, List<String>> responseApiNameListMap = new HashMap<String, List<String>>();

        for(Class<?> cmdClass: cmdClasses) {
            APICommand apiCmdAnnotation = cmdClass.getAnnotation(APICommand.class);
            if (apiCmdAnnotation == null)
                apiCmdAnnotation = cmdClass.getSuperclass().getAnnotation(APICommand.class);
            if (apiCmdAnnotation == null
                    || !apiCmdAnnotation.includeInApiDoc()
                    || apiCmdAnnotation.name().isEmpty())
                continue;

            String apiName = apiCmdAnnotation.name();
            String responseName = apiCmdAnnotation.responseObject().getName();
            if (!responseName.contains("SuccessResponse")) {
                if (!responseApiNameListMap.containsKey(responseName))
                    responseApiNameListMap.put(responseName, new ArrayList<String>());
                responseApiNameListMap.get(responseName).add(apiName);
            }
            ApiDiscoveryResponse response = new ApiDiscoveryResponse();
            response.setName(apiName);
            response.setDescription(apiCmdAnnotation.description());
            if (!apiCmdAnnotation.since().isEmpty())
                response.setSince(apiCmdAnnotation.since());
            response.setRelated(responseName);

            Field[] responseFields = apiCmdAnnotation.responseObject().getDeclaredFields();
            for(Field responseField: responseFields) {
                SerializedName serializedName = responseField.getAnnotation(SerializedName.class);
                if(serializedName != null) {
                    ApiResponseResponse responseResponse = new ApiResponseResponse();
                    responseResponse.setName(serializedName.value());
                    Param param = responseField.getAnnotation(Param.class);
                    if (param != null)
                        responseResponse.setDescription(param.description());
                    responseResponse.setType(responseField.getType().getSimpleName().toLowerCase());
                    response.addApiResponse(responseResponse);
                }
            }

            Field[] fields = ReflectUtil.getAllFieldsForClass(cmdClass,
                    new Class<?>[] {BaseCmd.class, BaseAsyncCmd.class, BaseAsyncCreateCmd.class});

            boolean isAsync = ReflectUtil.isCmdClassAsync(cmdClass,
                    new Class<?>[] {BaseAsyncCmd.class, BaseAsyncCreateCmd.class});

            response.setAsync(isAsync);

            for(Field field: fields) {
                Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
                if (parameterAnnotation != null
                        && parameterAnnotation.expose()
                        && parameterAnnotation.includeInApiDoc()) {

                    ApiParameterResponse paramResponse = new ApiParameterResponse();
                    paramResponse.setName(parameterAnnotation.name());
                    paramResponse.setDescription(parameterAnnotation.description());
                    paramResponse.setType(parameterAnnotation.type().toString().toLowerCase());
                    paramResponse.setLength(parameterAnnotation.length());
                    paramResponse.setRequired(parameterAnnotation.required());
                    if (!parameterAnnotation.since().isEmpty())
                        paramResponse.setSince(parameterAnnotation.since());
                    paramResponse.setRelated(parameterAnnotation.entityType()[0].getName());
                    response.addParam(paramResponse);
                }
            }
            response.setObjectName("api");
            _apiNameDiscoveryResponseMap.put(apiName, response);
        }

        for (String apiName: _apiNameDiscoveryResponseMap.keySet()) {
            ApiDiscoveryResponse response = _apiNameDiscoveryResponseMap.get(apiName);
            Set<ApiParameterResponse> processedParams = new HashSet<ApiParameterResponse>();
            for (ApiParameterResponse param: response.getParams()) {
                if (responseApiNameListMap.containsKey(param.getRelated())) {
                    List<String> relatedApis = responseApiNameListMap.get(param.getRelated());
                    param.setRelated(StringUtils.join(relatedApis, ","));
                } else {
                    param.setRelated(null);
                }
                processedParams.add(param);
            }
            response.setParams(processedParams);

            if (responseApiNameListMap.containsKey(response.getRelated())) {
                List<String> relatedApis = responseApiNameListMap.get(response.getRelated());
                relatedApis.remove(apiName);
                response.setRelated(StringUtils.join(relatedApis, ","));
            } else {
                response.setRelated(null);
            }
            _apiNameDiscoveryResponseMap.put(apiName, response);
        }
    }

    @Override
    public ListResponse<? extends BaseResponse> listApis(RoleType roleType, String name) {
        // Creates roles based response list cache the first time listApis is called
        // Due to how adapters work, this cannot be done when mgmt loads
        if (_apiNameRoleTypeListMap == null) {
            long startTime = System.nanoTime();
            _apiNameRoleTypeListMap = getApiNameRoleTypeListMap();
            for (Map.Entry<String, List<RoleType>> entry: _apiNameRoleTypeListMap.entrySet()) {
                String apiName = entry.getKey();
                for (RoleType roleTypeInList: entry.getValue()) {
                    _roleTypeDiscoveryResponseListMap.get(roleTypeInList).add(
                            _apiNameDiscoveryResponseMap.get(apiName));
                }
            }
            long endTime = System.nanoTime();
            s_logger.info("Api Discovery Service: List apis cached in " + (endTime - startTime) / 1000000.0 + " ms");
        }
        ListResponse<ApiDiscoveryResponse> response = new ListResponse<ApiDiscoveryResponse>();
        if (name != null) {
            if (!_apiNameDiscoveryResponseMap.containsKey(name))
                return null;

             List<ApiDiscoveryResponse> singleResponse = new ArrayList<ApiDiscoveryResponse>();
            singleResponse.add(_apiNameDiscoveryResponseMap.get(name));
            response.setResponses(singleResponse);

        } else {
            response.setResponses(_roleTypeDiscoveryResponseListMap.get(roleType));
        }
        return response;
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> apiDiscoveryPropertyMap = new HashMap<String, String>();
        apiDiscoveryPropertyMap.put("listApis", "15");
        return apiDiscoveryPropertyMap;
    }
}
