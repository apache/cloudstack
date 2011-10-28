/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
package com.cloud.storage.secondary;

import java.util.List;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.HostVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.vm.SecondaryStorageVmVO;

public interface SecondaryStorageVmManager extends Manager {

	public static final int DEFAULT_SS_VM_RAMSIZE = 256;			// 256M
	public static final int DEFAULT_SS_VM_CPUMHZ = 500;				// 500 MHz
	public static final int DEFAULT_SS_VM_MTUSIZE = 1500;			
    public static final int DEFAULT_SS_VM_CAPACITY = 50;			// max command execution session per SSVM
    public static final int DEFAULT_STANDBY_CAPACITY = 10;			// standy capacity to reserve per zone
	
	public static final String ALERT_SUBJECT = "secondarystoragevm-alert";
		
	public SecondaryStorageVmVO startSecStorageVm(long ssVmVmId);
	public boolean stopSecStorageVm(long ssVmVmId);
	public boolean rebootSecStorageVm(long ssVmVmId);
	public boolean destroySecStorageVm(long ssVmVmId);
	public void onAgentConnect(Long dcId, StartupCommand cmd);
	public boolean  generateFirewallConfiguration(Long agentId);
	public boolean generateVMSetupCommand(Long hostId);
	
	public Pair<HostVO, SecondaryStorageVmVO> assignSecStorageVm(long zoneId, Command cmd);
    boolean generateSetupCommand(Long hostId);
    boolean deleteHost(Long hostId);
    public HostVO findSecondaryStorageHost(long dcId);
    public List<HostVO> listSecondaryStorageHostsInAllZones();
    public List<HostVO> listSecondaryStorageHostsInOneZone(long dataCenterId);
    public List<HostVO> listLocalSecondaryStorageHostsInOneZone(long dataCenterId);
    public List<HostVO> listAllTypesSecondaryStorageHostsInOneZone(long dataCenterId);
    public List<HostVO> listUpAndConnectingSecondaryStorageVmHost(long dcId);
    public HostVO pickSsvmHost(HostVO ssHost);
}
