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
package com.cloud.consoleproxy;

import java.util.HashMap;
import java.util.Map;

import com.cloud.consoleproxy.util.Logger;

public class ConsoleProxyHttpHandlerHelper {
    private static final Logger s_logger = Logger.getLogger(ConsoleProxyHttpHandlerHelper.class);
    private static final String AND = "&";
    private static final String EQUALS = "=";
    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String SID = "sid";
    private static final String TAG = "tag";
    private static final String CONSOLE_URL = "consoleurl";
    private static final String SESSION_REF = "sessionref";
    private static final String TICKET = "ticket";
    private static final String LOCALE = "locale";
    private static final String HYPERV_HOST = "hypervHost";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String TOKEN = "token";

    public static Map<String, String> getQueryMap(String query) {
        String[] params = query.split(AND);
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String[] paramTokens = param.split(EQUALS);
            if (paramTokens != null && paramTokens.length == 2) {
                String name = paramTokens[0];
                String value = paramTokens[1];
                map.put(name, value);
            } else if (paramTokens.length == 3) {
                // very ugly, added for Xen tunneling url
                String name = paramTokens[0];
                String value = paramTokens[1] + EQUALS + paramTokens[2];
                map.put(name, value);
            } else {
                if (s_logger.isDebugEnabled())
                    s_logger.debug("Invalid paramemter in URL found. param: " + param);
            }
        }

        // This is a ugly solution for now. We will do encryption/decryption translation
        // here to make it transparent to rest of the code.
        if (map.get(TOKEN) != null) {
            ConsoleProxyPasswordBasedEncryptor encryptor = new ConsoleProxyPasswordBasedEncryptor(ConsoleProxy.getEncryptorPassword());

            ConsoleProxyClientParam param = encryptor.decryptObject(ConsoleProxyClientParam.class, map.get(TOKEN));

            // make sure we get information from token only
            guardUserInput(map);
            if (param != null) {
                if (param.getClientHostAddress() != null) {
                    s_logger.debug("decode token. host: " + param.getClientHostAddress());
                    map.put(HOST, param.getClientHostAddress());
                } else {
                    logMessageIfCannotFindClientParam(HOST);
                }
                if (param.getClientHostPort() != 0) {
                    s_logger.debug("decode token. port: " + param.getClientHostPort());
                    map.put(PORT, String.valueOf(param.getClientHostPort()));
                } else {
                    logMessageIfCannotFindClientParam(PORT);
                }
                if (param.getClientTag() != null) {
                    s_logger.debug("decode token. tag: " + param.getClientTag());
                    map.put(TAG, param.getClientTag());
                } else {
                    logMessageIfCannotFindClientParam(TAG);
                }
                if (param.getClientHostPassword() != null) {
                    map.put(SID, param.getClientHostPassword());
                } else {
                    logMessageIfCannotFindClientParam(SID);
                }
                if (param.getClientTunnelUrl() != null)
                    map.put(CONSOLE_URL, param.getClientTunnelUrl());
                if (param.getClientTunnelSession() != null)
                    map.put(SESSION_REF, param.getClientTunnelSession());
                if (param.getTicket() != null)
                    map.put(TICKET, param.getTicket());
                if (param.getLocale() != null)
                    map.put(LOCALE, param.getLocale());
                if (param.getHypervHost() != null)
                    map.put(HYPERV_HOST, param.getHypervHost());
                if (param.getUsername() != null)
                    map.put(USERNAME, param.getUsername());
                if (param.getPassword() != null)
                    map.put(PASSWORD, param.getPassword());
            } else {
                s_logger.error("Unable to decode token due to null console proxy client param");
            }
        } else {
            // we no longer accept information from parameter other than token
            guardUserInput(map);
        }

        return map;
    }

    private static void logMessageIfCannotFindClientParam(String param) {
        s_logger.error("decode token. " + param + " info is not found!");
    }

    private static void guardUserInput(Map<String, String> map) {
        map.remove(HOST);
        map.remove(PORT);
        map.remove(TAG);
        map.remove(SID);
        map.remove(CONSOLE_URL);
        map.remove(SESSION_REF);
        map.remove(TICKET);
        map.remove(LOCALE);
        map.remove(HYPERV_HOST);
        map.remove(USERNAME);
        map.remove(PASSWORD);
    }
}
