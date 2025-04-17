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

package com.cloud.network.vpn;

import com.cloud.exception.VPNProviderNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class RemoteAccessVpnServiceManagerImpl implements RemoteAccessVpnServiceManager {

    private static HashMap<String, RemoteAccessVpnService> providers;

    static {
        // Discover and register VPN services at boot
        ServiceLoader<RemoteAccessVpnProtocolBase> loader = ServiceLoader.load(RemoteAccessVpnProtocolBase.class);
        for (RemoteAccessVpnProtocolBase service : loader) {
            providers.put(service.GetName().toLowerCase(), service);
            System.out.println("Registered VPN Service: " + service.GetName().toLowerCase());
        }
    }


    @Override
    public Map<String, RemoteAccessVpnService> GetAvailableProviders() {
        return providers;
    }

    public RemoteAccessVpnService GetProvider(String key) throws VPNProviderNotFoundException {

        if (!providers.containsKey(key)) {
            throw new VPNProviderNotFoundException(key + " is not a valid VPN provider");
        }

        return providers.get(key);
    }
}
