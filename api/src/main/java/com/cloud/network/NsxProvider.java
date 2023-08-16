package com.cloud.network;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface NsxProvider extends InternalIdentity, Identity {
    String getHostname();
    String getProviderName();
    String getUsername();
    long getZoneId();
}
