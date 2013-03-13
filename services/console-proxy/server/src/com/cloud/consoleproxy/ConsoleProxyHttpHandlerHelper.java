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
            if(paramTokens != null && paramTokens.length == 2) {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name, value);
            } else if (paramTokens.length == 3) {
                // very ugly, added for Xen tunneling url
                String name = paramTokens[0];
                String value = paramTokens[1] + "=" + paramTokens[2];
                map.put(name, value);
            } else {
                if(s_logger.isDebugEnabled())
                    s_logger.debug("Invalid paramemter in URL found. param: " + param);
            }
        }
        
        // This is a ugly solution for now. We will do encryption/decryption translation
        // here to make it transparent to rest of the code.
        if(map.get("token") != null) {
            ConsoleProxyPasswordBasedEncryptor encryptor = new ConsoleProxyPasswordBasedEncryptor(
                ConsoleProxy.getEncryptorPassword());
    
            ConsoleProxyClientParam param = encryptor.decryptObject(ConsoleProxyClientParam.class, map.get("token"));

            // make sure we get information from token only
            map.clear();
            if(param != null) {
                if(param.getClientHostAddress() != null)
                    map.put("host", param.getClientHostAddress());
                if(param.getClientHostPort() != 0)
                    map.put("port", String.valueOf(param.getClientHostPort()));
                if(param.getClientTag() != null)
                    map.put("tag", param.getClientTag());
                if(param.getClientHostPassword() != null)
                    map.put("sid", param.getClientHostPassword());
                if(param.getClientTunnelUrl() != null)
                    map.put("consoleurl", param.getClientTunnelUrl());
                if(param.getClientTunnelSession() != null)
                    map.put("sessionref", param.getClientTunnelSession());
                if(param.getTicket() != null)
                    map.put("ticket", param.getTicket());
            }
        } else {
        	// we no longer accept information from parameter other than token 
        	map.clear();
        }
        
        return map;
    }
}
