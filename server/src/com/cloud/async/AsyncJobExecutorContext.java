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

package com.cloud.async;

import com.cloud.agent.AgentManager;
import com.cloud.async.dao.AsyncJobDao;
import com.cloud.event.dao.EventDao;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.server.ManagementServer;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.Manager;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;

public interface AsyncJobExecutorContext extends Manager {
	public ManagementServer getManagementServer();
	public AgentManager getAgentMgr();
	public NetworkManager getNetworkMgr();
	public UserVmManager getVmMgr();
	public SnapshotManager getSnapshotMgr();
	public AccountManager getAccountMgr();
	public StorageManager getStorageMgr();
	public EventDao getEventDao();
	public UserVmDao getVmDao();
	public AccountDao getAccountDao();
	public VolumeDao getVolumeDao();
    public DomainRouterDao getRouterDao();
    public IPAddressDao getIpAddressDao();
    public AsyncJobDao getJobDao();
    public UserDao getUserDao();
}
