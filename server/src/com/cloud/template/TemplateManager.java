// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.template;

import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.dc.DataCenterVO;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.Pair;

/**
 * TemplateManager manages the templates stored on secondary storage. It is responsible for creating private/public templates.
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


    VMTemplateHostVO findVmTemplateHost(long templateId,
            StoragePool pool);

    Pair<String, String> getAbsoluteIsoPath(long templateId, long dataCenterId);

    String getSecondaryStorageURL(long zoneId);

    HostVO getSecondaryStorageHost(long zoneId, long tmpltId);

    VMTemplateHostVO getTemplateHostRef(long zoneId, long tmpltId,
            boolean readyOnly);

    HostVO getSecondaryStorageHost(long zoneId);

    List<HostVO> getSecondaryStorageHosts(long zoneId);

    Long getTemplateSize(long templateId, long zoneId);

    DataStore getImageStore(String storeUuid, long zoneId);

    String getChecksum(Long hostId, String templatePath);

}
