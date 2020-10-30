package com.cloud.tungsten;

import com.cloud.network.TungstenProvider;
import net.juniper.tungsten.api.types.Domain;

import java.io.IOException;
import java.util.List;

public interface TungstenDomainManager {
    List<TungstenProvider> getTungstenProviders();

    void createDomainInTungsten(TungstenProvider tungstenProvider, String domainName, String domainUuid);

    Domain getDefaultTungstenDomain(TungstenProvider tungstenProvider) throws IOException;

    Domain getTungstenDomainByUuid(TungstenProvider tungstenProvider, String domainUuid) throws IOException;

    void deleteDomainFromTungsten(TungstenProvider tungstenProvider, String domainUuid);
}
