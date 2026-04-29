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
package org.apache.cloudstack.api.auth;

import org.apache.cloudstack.api.ServerApiException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;

/*
* APIAuthenticator is an interface that defines method that
* a class should implement that help with Authentication and accepts
* a command string and an array of parameters. This should be used only
* in the Servlet that is doing authentication.
*
* @param command The API command name such as login, logout etc
* @param params An array of HTTP parameters
* @param session HttpSession object
* */
public interface APIAuthenticator {
    public String authenticate(String command, Map<String, Object[]> params,
                               HttpSession session, InetAddress remoteAddress, String responseType,
                               StringBuilder auditTrailSb, final HttpServletRequest req, final HttpServletResponse resp) throws ServerApiException;

    public APIAuthenticationType getAPIType();

    public void setAuthenticators(List<PluggableAPIAuthenticator> authenticators);
}
