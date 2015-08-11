//
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
//

package com.cloud.utils.exception;

import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * CSExceptionErrorCode lists the CloudStack error codes that correspond
 * to a each exception thrown by the CloudStack API.
 */

public class CSExceptionErrorCode {

    public static final Logger s_logger = Logger.getLogger(CSExceptionErrorCode.class.getName());

    // Declare a hashmap of CloudStack Error Codes for Exceptions.
    protected static final HashMap<String, Integer> ExceptionErrorCodeMap;

    static {
        try {
            ExceptionErrorCodeMap = new HashMap<String, Integer>();
            ExceptionErrorCodeMap.put("com.cloud.utils.exception.CloudRuntimeException", 4250);
            ExceptionErrorCodeMap.put("com.cloud.utils.exception.ExecutionException", 4260);
            ExceptionErrorCodeMap.put("com.cloud.utils.exception.HypervisorVersionChangedException", 4265);
            ExceptionErrorCodeMap.put("com.cloud.exception.CloudException", 4275);
            ExceptionErrorCodeMap.put("com.cloud.exception.AccountLimitException", 4280);
            ExceptionErrorCodeMap.put("com.cloud.exception.AgentUnavailableException", 4285);
            ExceptionErrorCodeMap.put("com.cloud.exception.CloudAuthenticationException", 4290);
            ExceptionErrorCodeMap.put("com.cloud.exception.ConcurrentOperationException", 4300);
            ExceptionErrorCodeMap.put("com.cloud.exception.ConflictingNetworkSettingsException", 4305);
            ExceptionErrorCodeMap.put("com.cloud.exception.DiscoveredWithErrorException", 4310);
            ExceptionErrorCodeMap.put("com.cloud.exception.HAStateException", 4315);
            ExceptionErrorCodeMap.put("com.cloud.exception.InsufficientAddressCapacityException", 4320);
            ExceptionErrorCodeMap.put("com.cloud.exception.InsufficientCapacityException", 4325);
            ExceptionErrorCodeMap.put("com.cloud.exception.InsufficientNetworkCapacityException", 4330);
            ExceptionErrorCodeMap.put("com.cloud.exception.InsufficientVirtualNetworkCapacityException", 4331);
            ExceptionErrorCodeMap.put("com.cloud.exception.InsufficientServerCapacityException", 4335);
            ExceptionErrorCodeMap.put("com.cloud.exception.InsufficientStorageCapacityException", 4340);
            ExceptionErrorCodeMap.put("com.cloud.exception.InternalErrorException", 4345);
            ExceptionErrorCodeMap.put("com.cloud.exception.InvalidParameterValueException", 4350);
            ExceptionErrorCodeMap.put("com.cloud.exception.ManagementServerException", 4355);
            ExceptionErrorCodeMap.put("com.cloud.exception.NetworkRuleConflictException", 4360);
            ExceptionErrorCodeMap.put("com.cloud.exception.PermissionDeniedException", 4365);
            ExceptionErrorCodeMap.put("com.cloud.exception.ResourceAllocationException", 4370);
            ExceptionErrorCodeMap.put("com.cloud.exception.ResourceInUseException", 4375);
            ExceptionErrorCodeMap.put("com.cloud.exception.ResourceUnavailableException", 4380);
            ExceptionErrorCodeMap.put("com.cloud.exception.StorageUnavailableException", 4385);
            ExceptionErrorCodeMap.put("com.cloud.exception.UnsupportedServiceException", 4390);
            ExceptionErrorCodeMap.put("com.cloud.exception.VirtualMachineMigrationException", 4395);
            ExceptionErrorCodeMap.put("com.cloud.async.AsyncCommandQueued", 4540);
            ExceptionErrorCodeMap.put("com.cloud.exception.RequestLimitException", 4545);
            ExceptionErrorCodeMap.put("com.cloud.exception.StorageConflictException", 4550);

            // Have a special error code for ServerApiException when it is
            // thrown in a standalone manner when failing to detect any of the above
            // standard exceptions.
            ExceptionErrorCodeMap.put("org.apache.cloudstack.api.ServerApiException", 9999);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        }
    }

    public static HashMap<String, Integer> getErrCodeList() {
        return ExceptionErrorCodeMap;
    }

    public static int getCSErrCode(String exceptionName) {
        if (ExceptionErrorCodeMap.containsKey(exceptionName)) {
            return ExceptionErrorCodeMap.get(exceptionName);
        } else {
            s_logger.info("Could not find exception: " + exceptionName + " in error code list for exceptions");
            return -1;
        }
    }

    public static String getCurMethodName() {
        StackTraceElement stackTraceCalls[] = (new Throwable()).getStackTrace();
        return stackTraceCalls[1].toString();
    }
}
