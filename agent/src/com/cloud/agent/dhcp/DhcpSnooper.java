/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
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
package com.cloud.agent.dhcp;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapter;

public interface DhcpSnooper   extends Adapter{

    public  InetAddress getIPAddr(String macAddr, String vmName);

    public InetAddress getDhcpServerIP();

    public  void cleanup(String macAddr, String vmName);

    public  Map<String, InetAddress> syncIpAddr();

    public  boolean stop();

    public  void initializeMacTable(List<Pair<String, String>> macVmNameList);

}
