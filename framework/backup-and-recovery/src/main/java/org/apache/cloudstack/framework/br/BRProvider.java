package org.apache.cloudstack.framework.br;

public interface BRProvider {
    long getId();
    String getUuid();
    String getName();
    String getUrl();
    long getZoneId();
    String getProviderName();
}
