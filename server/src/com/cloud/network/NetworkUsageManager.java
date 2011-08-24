/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.network;

import java.util.List;

import com.cloud.api.commands.AddTrafficMonitorCmd;
import com.cloud.api.commands.DeleteTrafficMonitorCmd;
import com.cloud.api.commands.ListTrafficMonitorsCmd;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.server.api.response.TrafficMonitorResponse;
import com.cloud.utils.component.Manager;

public interface NetworkUsageManager extends Manager {

    Host addTrafficMonitor(AddTrafficMonitorCmd cmd);

    TrafficMonitorResponse getApiResponse(Host trafficMonitor);

    boolean deleteTrafficMonitor(DeleteTrafficMonitorCmd cmd);

    List<HostVO> listTrafficMonitors(ListTrafficMonitorsCmd cmd);

    List<IPAddressVO> listAllocatedDirectIps(long zoneId);
		
}
