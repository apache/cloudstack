package org.apache.cloudstack.service;

import com.cloud.network.NsxProvider;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.command.AddNsxControllerCmd;
import org.apache.cloudstack.api.response.NsxControllerResponse;

import java.util.List;

public interface NsxProviderService extends PluggableService {
    NsxProvider addProvider(AddNsxControllerCmd cmd);

    NsxControllerResponse createNsxControllerResponse(NsxProvider tungstenProvider);

    List<BaseResponse> listTungstenProvider(Long zoneId);
}
