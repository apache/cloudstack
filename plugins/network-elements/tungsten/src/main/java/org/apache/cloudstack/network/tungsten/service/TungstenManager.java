package org.apache.cloudstack.network.tungsten.service;

import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.network.tungsten.api.command.AddVRouterPortCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteVRouterPortCmd;

import java.io.IOException;

public interface TungstenManager extends PluggableService {

    SuccessResponse addVRouterPort(AddVRouterPortCmd cmd) throws IOException;

    SuccessResponse deleteVRouterPort(DeleteVRouterPortCmd cmd) throws IOException;
}
