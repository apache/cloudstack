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

package com.cloud.storage.dao;

import java.util.ArrayList;
import java.util.List;

import com.cloud.host.Status;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.Transaction;

public interface StoragePoolHostDao extends GenericDao<StoragePoolHostVO, Long> {
	public List<StoragePoolHostVO> listByPoolId(long id);
	
	public List<StoragePoolHostVO> listByHostId(long hostId);
	
	public StoragePoolHostVO findByPoolHost(long poolId, long hostId);

	List<StoragePoolHostVO> listByHostStatus(long poolId, Status hostStatus);

	List<Pair<Long, Integer>> getDatacenterStoragePoolHostInfo(long dcId, boolean sharedOnly);

	public ArrayList<Long> getPoolIds(Long hostId);
	
	public void deletePrimaryRecordsForHost(long hostId);
	
	public void deleteStoragePoolHostDetails(long hostId, long poolId);
}
