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

import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.db.GenericDao;

public interface VMTemplateHostDao extends GenericDao<VMTemplateHostVO, Long> {
    List<VMTemplateHostVO> listByHostId(long id);

    List<VMTemplateHostVO> listByTemplateId(long templateId);
    
    List<VMTemplateHostVO> listByOnlyTemplateId(long templateId);

    VMTemplateHostVO findByHostTemplate(long hostId, long templateId);

    VMTemplateHostVO findByHostTemplate(long hostId, long templateId, boolean lock);

    List<VMTemplateHostVO> listByHostTemplate(long hostId, long templateId);

    void update(VMTemplateHostVO instance);    

    List<VMTemplateHostVO> listByTemplateStatus(long templateId, VMTemplateHostVO.Status downloadState);

    List<VMTemplateHostVO> listByTemplateStatus(long templateId, long datacenterId, VMTemplateHostVO.Status downloadState);

    List<VMTemplateHostVO> listByTemplateStatus(long templateId, long datacenterId, long podId, VMTemplateHostVO.Status downloadState);

    List<VMTemplateHostVO> listByTemplateStates(long templateId, VMTemplateHostVO.Status... states);

    List<VMTemplateHostVO> listDestroyed(long hostId);

    boolean templateAvailable(long templateId, long hostId);

    List<VMTemplateHostVO> listByZoneTemplate(long dcId, long templateId, boolean readyOnly);

    void deleteByHost(Long hostId);

    VMTemplateHostVO findLocalSecondaryStorageByHostTemplate(long hostId, long templateId);

    List<VMTemplateHostVO> listByTemplateHostStatus(long templateId, long hostId, Status... states);

    List<VMTemplateHostVO> listByState(Status state);
}
