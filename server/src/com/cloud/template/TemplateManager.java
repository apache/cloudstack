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
package com.cloud.template;

import java.util.List;

import com.cloud.dc.DataCenterVO;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;

/**
 * TemplateManager manages the templates stored on secondary storage. It is responsible for creating private/public templates.
 * It is also responsible for downloading.
 */
public interface TemplateManager extends TemplateService{

    /**
     * Prepares a template for vm creation for a certain storage pool.
     * 
     * @param template
     *            template to prepare
     * @param pool
     *            pool to make sure the template is ready in.
     * @return VMTemplateStoragePoolVO if preparation is complete; null if not.
     */
    VMTemplateStoragePoolVO prepareTemplateForCreate(VMTemplateVO template, StoragePool pool);

    boolean resetTemplateDownloadStateOnPool(long templateStoragePoolRefId);

    /**
     * Copies a template from its current secondary storage server to the secondary storage server in the specified zone.
     * 
     * @param template
     * @param srcSecHost
     * @param srcZone
     * @param destZone
     * @return true if success
     * @throws InternalErrorException
     * @throws StorageUnavailableException
     * @throws ResourceAllocationException
     */
    boolean copy(long userId, VMTemplateVO template, HostVO srcSecHost, DataCenterVO srcZone, DataCenterVO dstZone) throws StorageUnavailableException, ResourceAllocationException;

    /**
     * Deletes a template from secondary storage servers
     * 
     * @param userId
     * @param templateId
     * @param zoneId
     *            - optional. If specified, will only delete the template from the specified zone's secondary storage server.
     * @return true if success
     */
    boolean delete(long userId, long templateId, Long zoneId);

    /**
     * Lists templates in the specified storage pool that are not being used by any VM.
     * 
     * @param pool
     * @return list of VMTemplateStoragePoolVO
     */
    List<VMTemplateStoragePoolVO> getUnusedTemplatesInPool(StoragePoolVO pool);

    /**
     * Deletes a template in the specified storage pool.
     * 
     * @param templatePoolVO
     */
    void evictTemplateFromStoragePool(VMTemplateStoragePoolVO templatePoolVO);

    boolean templateIsDeleteable(VMTemplateHostVO templateHostRef);

    VMTemplateHostVO prepareISOForCreate(VMTemplateVO template, StoragePool pool);

}
