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

    public static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String[] paramTokens = param.split("=");
            if (paramTokens != null && paramTokens.length == 2) {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name, value);
            } else if (paramTokens.length == 3) {
                // very ugly, added for Xen tunneling url
                String name = paramTokens[0];
                String value = paramTokens[1] + "=" + paramTokens[2];
                map.put(name, value);
            } else {
                if (s_logger.isDebugEnabled())
                    s_logger.debug("Invalid paramemter in URL found. param: " + param);
            }
        }

        // This is a ugly solution for now. We will do encryption/decryption translation
        // here to make it transparent to rest of the code.
        if (map.get("token") != null) {
            ConsoleProxyPasswordBasedEncryptor encryptor = new ConsoleProxyPasswordBasedEncryptor(ConsoleProxy.getEncryptorPassword());

            ConsoleProxyClientParam param = encryptor.decryptObject(ConsoleProxyClientParam.class, map.get("token"));

            // make sure we get information from token only
            guardUserInput(map);
            if (param != null) {
                if (param.getClientHostAddress() != null) {
                    s_logger.debug("decode token. host: " + param.getClientHostAddress());
                    map.put("host", param.getClientHostAddress());
                } else {
                    s_logger.error("decode token. host info is not found!");
                }
                if (param.getClientHostPort() != 0) {
                    s_logger.debug("decode token. port: " + param.getClientHostPort());
                    map.put("port", String.valueOf(param.getClientHostPort()));
                } else {
                    s_logger.error("decode token. port info is not found!");
                }
                if (param.getClientTag() != null) {
                    s_logger.debug("decode token. tag: " + param.getClientTag());
                    map.put("tag", param.getClientTag());
                } else {
                    s_logger.error("decode token. tag info is not found!");
                }
                if (param.getClientHostPassword() != null) {
                    map.put("sid", param.getClientHostPassword());
                } else {
                    s_logger.error("decode token. sid info is not found!");
                }
                if (param.getClientTunnelUrl() != null)
                    map.put("consoleurl", param.getClientTunnelUrl());
                if (param.getClientTunnelSession() != null)
                    map.put("sessionref", param.getClientTunnelSession());
                if (param.getTicket() != null)
                    map.put("ticket", param.getTicket());
                if (param.getLocale() != null)
                    map.put("locale", param.getLocale());
                if (param.getHypervHost() != null)
                    map.put("hypervHost", param.getHypervHost());
                if (param.getUsername() != null)
                    map.put("username", param.getUsername());
                if (param.getPassword() != null)
                    map.put("password", param.getPassword());
                if (param.getSourceIP() != null)
                    map.put("sourceIP", param.getSourceIP());
                if (param.getWebsocketUrl() != null) {
                    map.put("websocketUrl", param.getWebsocketUrl());
                }
            } else {
                s_logger.error("Unable to decode token");
            }
        } else {
            // we no longer accept information from parameter other than token
            guardUserInput(map);
        }

        return map;
    }

    private static void guardUserInput(Map<String, String> map) {
        map.remove("host");
        map.remove("port");
        map.remove("tag");
        map.remove("sid");
        map.remove("consoleurl");
        map.remove("sessionref");
        map.remove("ticket");
        map.remove("locale");
        map.remove("hypervHost");
        map.remove("username");
        map.remove("password");
        map.remove("websocketUrl");
    }
}
