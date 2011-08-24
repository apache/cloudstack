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
 * @author-aj
 */

package com.cloud.netapp;

import java.net.UnknownHostException;
import java.rmi.ServerException;
import java.util.List;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.utils.component.Manager;

public interface NetappManager  extends Manager {
	enum AlgorithmType {
		RoundRobin,
		LeastFull
	}
	void destroyVolumeOnFiler(String ipAddress, String aggrName, String volName) throws ServerException, InvalidParameterValueException, ResourceInUseException;

	void createVolumeOnFiler(String ipAddress, String aggName, String poolName,
			String volName, String volSize, String snapshotPolicy,
			Integer snapshotReservation, String username, String password)
			throws UnknownHostException, ServerException, InvalidParameterValueException;

	public String[] associateLun(String guestIqn, String path) throws ServerException, InvalidParameterValueException;


	void disassociateLun(String iGroup, String path) throws ServerException, InvalidParameterValueException;

	List<LunVO> listLunsOnFiler(String poolName);

	void destroyLunOnFiler(String path) throws ServerException, InvalidParameterValueException;

	List<NetappVolumeVO> listVolumesOnFiler(String poolName);

	List<NetappVolumeVO> listVolumesAscending(String poolName);

	long returnAvailableVolumeSize(String volName, String userName,
			String password, String serverIp) throws ServerException;

	void createPool(String poolName, String algorithm) throws InvalidParameterValueException;

	void modifyPool(String poolName, String algorithm) throws InvalidParameterValueException;

	void deletePool(String poolName) throws InvalidParameterValueException, ResourceInUseException;

	List<PoolVO> listPools();

	public String[] createLunOnFiler(String poolName, Long lunSize) throws InvalidParameterValueException, ServerException, ResourceAllocationException;

}
