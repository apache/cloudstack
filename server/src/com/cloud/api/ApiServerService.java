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
package com.cloud.api;

import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.cloudstack.api.ServerApiException;
import com.cloud.exception.CloudAuthenticationException;

public interface ApiServerService {
    public boolean verifyRequest(Map<String, Object[]> requestParameters, Long userId) throws ServerApiException;
    public Long fetchDomainId(String domainUUID);
    public void loginUser(HttpSession session, String username, String password, Long domainId, String domainPath, String loginIpAddress ,Map<String, Object[]> requestParameters) throws CloudAuthenticationException;
    public void logoutUser(long userId);
    public boolean verifyUser(Long userId);

    public String getSerializedApiError(int errorCode, String errorText, Map<String, Object[]> apiCommandParams, String responseType);
    public String getSerializedApiError(ServerApiException ex, Map<String, Object[]> apiCommandParams, String responseType);

    public String handleRequest(Map params, String responseType, StringBuffer auditTrailSb) throws ServerApiException;
}
