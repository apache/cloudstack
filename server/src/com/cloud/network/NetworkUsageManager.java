/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
