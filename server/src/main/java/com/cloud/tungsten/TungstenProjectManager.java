package com.cloud.tungsten;

import com.cloud.domain.Domain;
import com.cloud.network.TungstenProvider;
import com.cloud.network.element.TungstenProviderVO;

import java.util.List;

public interface TungstenProjectManager {
    List<TungstenProviderVO> getTungstenProviders();

    void createProjectInTungsten(TungstenProvider tungstenProvider, String projectUuid, String projectName, Domain domain);

    void deleteProjectFromTungsten(TungstenProvider tungstenProvider, String projectUuid);
}
