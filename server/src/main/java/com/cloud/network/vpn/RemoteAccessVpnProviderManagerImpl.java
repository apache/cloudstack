package com.cloud.network.vpn;

import com.cloud.exception.VPNProviderNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class RemoteAccessVpnProviderManagerImpl implements RemoteAccessVpnProviderManagerService {

    private static HashMap<String, RemoteAccessVpnProvider> providers;

    static {
        // Discover and register VPN services at boot
        ServiceLoader<RemoteAccessVpnProvider> loader = ServiceLoader.load(RemoteAccessVpnProvider.class);
        for (RemoteAccessVpnProvider service : loader) {
            providers.put(service.GetName().toLowerCase(), service);
            System.out.println("Registered VPN Service: " + service.GetName().toLowerCase());
        }
    }


    @Override
    public Map<String, RemoteAccessVpnProvider> GetAvailableProviders() {
        return providers;
    }

    public RemoteAccessVpnProvider GetProvider(String key) throws VPNProviderNotFoundException {

        if (!providers.containsKey(key)) {
            throw new VPNProviderNotFoundException(key + " is not a valid VPN provider");
        }

        return providers.get(key);
    }
}
