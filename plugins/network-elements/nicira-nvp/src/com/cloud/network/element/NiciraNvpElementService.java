package com.cloud.network.element;

import java.util.List;

import com.cloud.api.commands.AddNiciraNvpDeviceCmd;
import com.cloud.api.commands.DeleteNiciraNvpDeviceCmd;
import com.cloud.api.commands.ListNiciraNvpDeviceNetworksCmd;
import com.cloud.api.commands.ListNiciraNvpDevicesCmd;
import com.cloud.api.response.NiciraNvpDeviceResponse;
import com.cloud.network.Network;
import com.cloud.network.NiciraNvpDeviceVO;
import com.cloud.utils.component.PluggableService;

public interface NiciraNvpElementService extends PluggableService {

    public NiciraNvpDeviceVO addNiciraNvpDevice(AddNiciraNvpDeviceCmd cmd);

    public NiciraNvpDeviceResponse createNiciraNvpDeviceResponse(
            NiciraNvpDeviceVO niciraDeviceVO);

    boolean deleteNiciraNvpDevice(DeleteNiciraNvpDeviceCmd cmd);

    List<? extends Network> listNiciraNvpDeviceNetworks(
            ListNiciraNvpDeviceNetworksCmd cmd);

    List<NiciraNvpDeviceVO> listNiciraNvpDevices(ListNiciraNvpDevicesCmd cmd);

}
