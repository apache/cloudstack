package com.cloud.network.element;

import java.util.List;

import com.cloud.api.response.OvsDeviceResponse;
import com.cloud.network.commands.AddOvsDeviceCmd;
import com.cloud.network.commands.DeleteOvsDeviceCmd;
import com.cloud.network.commands.ListOvsDevicesCmd;
import com.cloud.network.ovs.dao.OvsDeviceVO;
import com.cloud.utils.component.PluggableService;

public interface OvsElementService extends PluggableService {

	public OvsDeviceVO addOvsDevice(AddOvsDeviceCmd cmd);

	public OvsDeviceResponse createOvsDeviceResponse(OvsDeviceVO ovsDeviceVO);

	public boolean deleteOvsDevice(DeleteOvsDeviceCmd cmd);

	public List<OvsDeviceVO> listOvsDevices(ListOvsDevicesCmd cmd);

}
