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

package com.cloud.api.dispatch;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.log4j.Logger;

/**
 * This worker validates parameters in a generic way, by using annotated
 * restrictions without involving the {@Link BaseCmd}. This worker doesn't
 * know or care about the meaning of the parameters and that's why we can
 * have it out of the {@Link BaseCmd}
 *
 * @author afornie
 */
public class ParamGenericValidationWorker implements DispatchWorker {

    static Logger s_logger = Logger.getLogger(ParamGenericValidationWorker.class.getName());

    protected static final List<String> defaultParamNames = new ArrayList<String>();

    static {
        defaultParamNames.add(ApiConstants.ACCOUNT_ID);
        defaultParamNames.add(ApiConstants.CTX_START_EVENT_ID);
        defaultParamNames.add(ApiConstants.COMMAND);
        defaultParamNames.add(ApiConstants.CMD_EVENT_TYPE);
        defaultParamNames.add(ApiConstants.USERNAME);
        defaultParamNames.add(ApiConstants.USER_ID);
        defaultParamNames.add(ApiConstants.PASSWORD);
        defaultParamNames.add(ApiConstants.DOMAIN);
        defaultParamNames.add(ApiConstants.DOMAIN_ID);
        defaultParamNames.add(ApiConstants.DOMAIN__ID);
        defaultParamNames.add(ApiConstants.SESSIONKEY);
        defaultParamNames.add(ApiConstants.RESPONSE);
        defaultParamNames.add(ApiConstants.PAGE);
        defaultParamNames.add(ApiConstants.USER_API_KEY);
        defaultParamNames.add(ApiConstants.API_KEY);
        defaultParamNames.add(ApiConstants.PAGE_SIZE);
        defaultParamNames.add(ApiConstants.HTTPMETHOD);
        defaultParamNames.add(ApiConstants.SIGNATURE);
        defaultParamNames.add(ApiConstants.CTX_ACCOUNT_ID);
        defaultParamNames.add(ApiConstants.CTX_START_EVENT_ID);
        defaultParamNames.add(ApiConstants.CTX_USER_ID);
        defaultParamNames.add(ApiConstants.CTX_DETAILS);
        defaultParamNames.add(ApiConstants.UUID);
        defaultParamNames.add(ApiConstants.ID);
        defaultParamNames.add("_");
    }

    protected static final String ERROR_MSG_PREFIX = "Unknown parameters :";

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(final DispatchTask task) {
        final BaseCmd cmd = task.getCmd();
        final Map params = task.getParams();

        final List<String> expectedParamNames = getParamNamesForCommand(cmd);

        final StringBuilder errorMsg = new StringBuilder(ERROR_MSG_PREFIX);
        boolean foundUnknownParam = false;
        for (final Object actualParamName : params.keySet()) {
            // If none of the expected params matches, we have an unknown param
            boolean matchedCurrentParam = false;
            for (final String expectedName : expectedParamNames) {
                if (expectedName.equalsIgnoreCase((String) actualParamName)) {
                    matchedCurrentParam = true;
                    break;
                }
            }
            if (!matchedCurrentParam && !((String)actualParamName).equalsIgnoreCase("expires") && !((String)actualParamName).equalsIgnoreCase("signatureversion")) {
                errorMsg.append(" ").append(actualParamName);
                foundUnknownParam= true;
            }
        }

        if (foundUnknownParam) {
            s_logger.warn(String.format("Received unknown parameters for command %s. %s", cmd.getActualCommandName(), errorMsg));
        }
    }

    protected List<String> getParamNamesForCommand(final BaseCmd cmd) {
        final List<String> paramNames = new ArrayList<String>();
        // The expected param names are all the specific for the current command class ...
        for (final Field field : cmd.getParamFields()) {
            final Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
            paramNames.add(parameterAnnotation.name());
        }
        // ... plus the default ones
        paramNames.addAll(defaultParamNames);
        return paramNames;
    }
}
