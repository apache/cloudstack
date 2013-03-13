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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.user.discovery.ListApisCmd;
import org.apache.cloudstack.api.response.ApiDiscoveryResponse;
import org.apache.cloudstack.api.response.ApiParameterResponse;
import org.apache.cloudstack.api.response.ApiResponseResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.serializer.Param;
import com.cloud.user.User;
import com.cloud.utils.ReflectUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.PluggableService;
import com.google.gson.annotations.SerializedName;

@Component
@Local(value = ApiDiscoveryService.class)
public class ApiDiscoveryServiceImpl implements ApiDiscoveryService {
    private static final Logger s_logger = Logger.getLogger(ApiDiscoveryServiceImpl.class);

    @Inject protected List<APIChecker> _apiAccessCheckers = null;
    @Inject protected List<PluggableService> _services = null;
    private static Map<String, ApiDiscoveryResponse> s_apiNameDiscoveryResponseMap = null;

    protected ApiDiscoveryServiceImpl() {
        super();
    }

    @PostConstruct
    void init() {
        if (s_apiNameDiscoveryResponseMap == null) {
            long startTime = System.nanoTime();
            s_apiNameDiscoveryResponseMap = new HashMap<String, ApiDiscoveryResponse>();
            Set<Class<?>> cmdClasses = new HashSet<Class<?>>();
            for(PluggableService service: _services) {
                s_logger.debug(String.format("getting api commands of service: %s", service.getClass().getName()));
                cmdClasses.addAll(service.getCommands());
            }
            cmdClasses.addAll(this.getCommands());
            cacheResponseMap(cmdClasses);
            long endTime = System.nanoTime();
            s_logger.info("Api Discovery Service: Annotation, docstrings, api relation graph processed in " + (endTime - startTime) / 1000000.0 + " ms");
        }
    }

    protected void cacheResponseMap(Set<Class<?>> cmdClasses) {
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

            Set<Field> fields = ReflectUtil.getAllFieldsForClass(cmdClass,
                    new Class<?>[]{BaseCmd.class, BaseAsyncCmd.class, BaseAsyncCreateCmd.class});

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
            s_apiNameDiscoveryResponseMap.put(apiName, response);
        }

        for (String apiName : s_apiNameDiscoveryResponseMap.keySet()) {
            ApiDiscoveryResponse response = s_apiNameDiscoveryResponseMap.get(apiName);
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
            s_apiNameDiscoveryResponseMap.put(apiName, response);
        }
    }

    @Override
    public ListResponse<? extends BaseResponse> listApis(User user, String name) {
        ListResponse<ApiDiscoveryResponse> response = new ListResponse<ApiDiscoveryResponse>();
        List<ApiDiscoveryResponse> responseList = new ArrayList<ApiDiscoveryResponse>();

        if (user == null)
            return null;

        if (name != null) {
            if (!s_apiNameDiscoveryResponseMap.containsKey(name))
                return null;

            for (APIChecker apiChecker : _apiAccessCheckers) {
                try {
                    apiChecker.checkAccess(user, name);
                } catch (Exception ex) {
                    return null;
                }
            }
            responseList.add(s_apiNameDiscoveryResponseMap.get(name));

        } else {
            for (String apiName : s_apiNameDiscoveryResponseMap.keySet()) {
                boolean isAllowed = true;
                for (APIChecker apiChecker : _apiAccessCheckers) {
                    try {
                        apiChecker.checkAccess(user, apiName);
                    } catch (Exception ex) {
                        isAllowed = false;
                    }
                }
                if (isAllowed)
                    responseList.add(s_apiNameDiscoveryResponseMap.get(apiName));
            }
        }
        response.setResponses(responseList);
        return response;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ListApisCmd.class);
        return cmdList;
    }
}
