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

import java.util.List;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface VolumeDao extends GenericDao<VolumeVO, Long>, StateDao<Volume.State, Volume.Event, Volume> {
	List<VolumeVO> findDetachedByAccount(long accountId);
    List<VolumeVO> findByAccount(long accountId);
    Pair<Long, Long> getCountAndTotalByPool(long poolId);
    List<VolumeVO> findByInstance(long id);
    List<VolumeVO> findByInstanceAndType(long id, Volume.Type vType);
    List<VolumeVO> findByInstanceIdDestroyed(long vmId);
    List<VolumeVO> findByAccountAndPod(long accountId, long podId);
    List<VolumeVO> findByTemplateAndZone(long templateId, long zoneId);
    void deleteVolumesByInstance(long instanceId);
    void attachVolume(long volumeId, long vmId, long deviceId);
    void detachVolume(long volumeId);
    boolean isAnyVolumeActivelyUsingTemplateOnPool(long templateId, long poolId);
    List<VolumeVO> findCreatedByInstance(long id);
    List<VolumeVO> findByPoolId(long poolId);
	List<VolumeVO> findByInstanceAndDeviceId(long instanceId, long deviceId);
    List<VolumeVO> findUsableVolumesForInstance(long instanceId);
    Long countAllocatedVolumesForAccount(long accountId); 
   
    HypervisorType getHypervisorType(long volumeId);
    
    List<VolumeVO> listVolumesToBeDestroyed();
    ImageFormat getImageFormat(Long volumeId);
    
    List<VolumeVO> findReadyRootVolumesByInstance(long instanceId);
    List<Long> listPoolIdsByVolumeCount(long dcId, Long podId, Long clusterId, long accountId);
}
