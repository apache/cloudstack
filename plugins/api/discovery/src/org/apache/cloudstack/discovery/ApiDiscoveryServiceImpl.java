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

import com.cloud.utils.ReflectUtil;
import com.cloud.utils.component.AdapterBase;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ApiDiscoveryResponse;
import org.apache.cloudstack.api.response.ApiParameterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.log4j.Logger;

import javax.ejb.Local;
import javax.naming.ConfigurationException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Local(value = ApiDiscoveryService.class)
public class ApiDiscoveryServiceImpl extends AdapterBase implements ApiDiscoveryService {

    private static final Logger s_logger = Logger.getLogger(ApiDiscoveryServiceImpl.class);
    private Map<String, Class<?>> _apiNameCmdClassMap;
    private ListResponse<ApiDiscoveryResponse> _discoveryResponse;

    protected ApiDiscoveryServiceImpl() {
        super();
    }

    private void generateApiNameCmdClassMapping() {
        _apiNameCmdClassMap = new HashMap<String, Class<?>>();
        Set<Class<?>> cmdClasses = ReflectUtil.getClassesWithAnnotation(APICommand.class, new String[]{"org.apache.cloudstack.api", "com.cloud.api"});

        for(Class<?> cmdClass: cmdClasses) {
            String apiName = cmdClass.getAnnotation(APICommand.class).name();
            if (_apiNameCmdClassMap.containsKey(apiName)) {
                s_logger.error("API Cmd class " + cmdClass.getName() + " has non-unique apiname" + apiName);
                continue;
            }
            _apiNameCmdClassMap.put(apiName, cmdClass);
        }
    }

    private void precacheListApiResponse() {

        if(_apiNameCmdClassMap == null)
            return;

        _discoveryResponse = new ListResponse<ApiDiscoveryResponse>();

        List<ApiDiscoveryResponse> apiDiscoveryResponses = new ArrayList<ApiDiscoveryResponse>();

        for(String key: _apiNameCmdClassMap.keySet()) {
            Class<?> cmdClass = _apiNameCmdClassMap.get(key);
            APICommand apiCmdAnnotation = cmdClass.getAnnotation(APICommand.class);
            if (apiCmdAnnotation == null)
                apiCmdAnnotation = cmdClass.getSuperclass().getAnnotation(APICommand.class);
            if (apiCmdAnnotation == null
                    || !apiCmdAnnotation.includeInApiDoc()
                    || apiCmdAnnotation.name().isEmpty())
                continue;

            ApiDiscoveryResponse response = new ApiDiscoveryResponse();
            response.setName(apiCmdAnnotation.name());
            response.setDescription(apiCmdAnnotation.description());
            response.setSince(apiCmdAnnotation.since());

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
                    paramResponse.setType(parameterAnnotation.type().toString());
                    paramResponse.setLength(parameterAnnotation.length());
                    paramResponse.setRequired(parameterAnnotation.required());
                    paramResponse.setSince(parameterAnnotation.since());
                    response.addParam(paramResponse);
                }
            }
            response.setObjectName("apis");
            apiDiscoveryResponses.add(response);
        }
        _discoveryResponse.setResponses(apiDiscoveryResponses);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        super.configure(name, params);

        generateApiNameCmdClassMapping();
        precacheListApiResponse();

        return true;
    }

    public Map<String, Class<?>> getApiNameCmdClassMapping() {
        return _apiNameCmdClassMap;
    }

    @Override
    public ListResponse<? extends BaseResponse> listApis() {
        return _discoveryResponse;
    }

    @Override
    public String[] getPropertiesFiles() {
        return new String[] { "api-discovery_commands.properties" };
    }
}
