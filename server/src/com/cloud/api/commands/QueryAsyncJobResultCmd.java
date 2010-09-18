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

package com.cloud.api.commands;

import java.util.Date;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.Parameter;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.AsyncJobResponse;
import com.cloud.async.AsyncJobResult;

public class QueryAsyncJobResultCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(QueryAsyncJobResultCmd.class.getName());

    private static final String s_name = "queryasyncjobresultresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    @Override
    public String getResponse() {
        AsyncJobResult result = (AsyncJobResult)getResponseObject();

        AsyncJobResponse response = new AsyncJobResponse();
        response.setId(result.getJobId());
        response.setJobStatus(result.getJobStatus());
        response.setJobProcStatus(result.getProcessStatus());
        response.setJobResultCode(result.getResultCode());
        response.setJobResult(result.getResult());

        Object resultObject = result.getResultObject();
        if (resultObject != null) {
            Class<?> clz = resultObject.getClass();
            if(clz.isPrimitive() || clz.getSuperclass() == Number.class || clz == String.class || clz == Date.class) {
                response.setJobResultType("text");
            } else {
                response.setJobResultType("object");
            }
        }

        /*
        Object resultObject = result.getResultObject();
        if (resultObject != null) {

            Class<?> clz = resultObject.getClass();
            if(clz.isPrimitive() || clz.getSuperclass() == Number.class || clz == String.class || clz == Date.class) {
                returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_RESULT_TYPE.getName(), "text"));
                SerializerHelper.appendPairList(returnValues, resultObject, BaseCmd.Properties.JOB_RESULT.getName());
            } else {
                returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_RESULT_TYPE.getName(), "object"));

                if(result.getCmdOriginator() != null && !result.getCmdOriginator().isEmpty()) {
                    List<Pair<String, Object>> resultValues = new ArrayList<Pair<String, Object>>();
                    if (resultObject instanceof NetworkGroupResultObject) {
                        serializeNetworkGroupResults(resultValues, (NetworkGroupResultObject)resultObject);
                    } else {
                        SerializerHelper.appendPairList(resultValues, resultObject, BaseCmd.Properties.JOB_RESULT.getName());
                    }
                    returnValues.add(new Pair<String, Object>(result.getCmdOriginator(), new Object[] { resultValues } ));
                }
            }
        } 
        */

        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response);
    }

    // For now network groups are the only objects with nested objects inside, so we special case serialization to handle this one case.
    // In the future, if a generic serialization that handles nested objects is implemented then this special case can be removed.
    /*
    private void serializeNetworkGroupResults(List<Pair<String, Object>> resultValues, NetworkGroupResultObject resultObject) {
        if (resultObject != null) {
            resultValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), resultObject.getId().toString()));
            resultValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), resultObject.getName()));
            resultValues.add(new Pair<String, Object>(BaseCmd.Properties.DESCRIPTION.getName(), resultObject.getDescription()));
            resultValues.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), resultObject.getAccountName()));
            resultValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), resultObject.getDomainId().toString()));

            List<IngressRuleResultObject> ingressRules = resultObject.getIngressRules();
            if ((ingressRules != null) && !ingressRules.isEmpty()) {
                Object[] ingressDataArray = new Object[ingressRules.size()];
                int j = 0;
                for (IngressRuleResultObject ingressRule : ingressRules) {
                    List<Pair<String, Object>> ingressData = new ArrayList<Pair<String, Object>>();

                    ingressData.add(new Pair<String, Object>(BaseCmd.Properties.RULE_ID.getName(), ingressRule.getId().toString()));
                    ingressData.add(new Pair<String, Object>(BaseCmd.Properties.PROTOCOL.getName(), ingressRule.getProtocol()));
                    if ("icmp".equalsIgnoreCase(ingressRule.getProtocol())) {
                        ingressData.add(new Pair<String, Object>(BaseCmd.Properties.ICMP_TYPE.getName(), Integer.valueOf(ingressRule.getStartPort()).toString()));
                        ingressData.add(new Pair<String, Object>(BaseCmd.Properties.ICMP_CODE.getName(), Integer.valueOf(ingressRule.getEndPort()).toString()));
                    } else {
                        ingressData.add(new Pair<String, Object>(BaseCmd.Properties.START_PORT.getName(), Integer.valueOf(ingressRule.getStartPort()).toString()));
                        ingressData.add(new Pair<String, Object>(BaseCmd.Properties.END_PORT.getName(), Integer.valueOf(ingressRule.getEndPort()).toString()));
                    }

                    if (ingressRule.getAllowedNetworkGroup() != null) {
                        ingressData.add(new Pair<String, Object>(BaseCmd.Properties.NETWORK_GROUP_NAME.getName(), ingressRule.getAllowedNetworkGroup()));
                        ingressData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), ingressRule.getAllowedNetGroupAcct()));
                    } else if (ingressRule.getAllowedSourceIpCidr() != null) {
                        ingressData.add(new Pair<String, Object>(BaseCmd.Properties.CIDR.getName(), ingressRule.getAllowedSourceIpCidr()));
                    }
                    ingressDataArray[j++] = ingressData;
                }

                resultValues.add(new Pair<String, Object>("ingressrule", ingressDataArray));
            }
        }
    }
    */
}
