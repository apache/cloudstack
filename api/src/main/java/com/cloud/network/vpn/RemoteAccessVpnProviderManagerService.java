package com.cloud.network.vpn;

import com.cloud.exception.VPNProviderNotFoundException;

import java.util.Map;

public interface RemoteAccessVpnProviderManagerService {
    Map<String, RemoteAccessVpnProvider> GetAvailableProviders();
    RemoteAccessVpnProvider GetProvider(String key) throws VPNProviderNotFoundException;
}
