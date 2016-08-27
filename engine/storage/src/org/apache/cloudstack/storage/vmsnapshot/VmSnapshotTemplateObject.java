/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.vmsnapshot;

import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VmSnapshotTemplateInfo;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.to.VmSnapshotTemplateObjectTO;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMSnapshotTemplateStoragePoolVO;
import com.cloud.storage.VMSnapshotTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.VMSnapshotTemplatePoolDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class VmSnapshotTemplateObject implements VmSnapshotTemplateInfo {

    private static final Logger s_logger = Logger.getLogger(VmSnapshotTemplateObject.class);
    private VMSnapshotVO vmSnapshotVo;
    private VMSnapshotTemplateStoragePoolVO vmSnapTmplPoolVo;
    private DataStore dataStore;
    private String installPath;
    private String guestOsType;
    @Inject
    VMSnapshotDao vmSnapDao;
    @Inject
    ObjectInDataStoreManager objectInStoreMgr;
    @Inject
    VMSnapshotTemplatePoolDao vmSnapTmplPoolDao;

    public VmSnapshotTemplateObject() {
    }

    protected void configure(VMSnapshotVO vmSnapVo, DataStore dtore, VMSnapshotTemplateStoragePoolVO vSnapTmplPoolVo) {
        vmSnapshotVo = vmSnapVo;
        dataStore = dtore;
        vmSnapTmplPoolVo = vSnapTmplPoolVo;
    }

    public static VmSnapshotTemplateObject getTemplate(VMSnapshotVO vo, DataStore store, VMSnapshotTemplateStoragePoolVO vmSnapTmplPoolVo) {
        VmSnapshotTemplateObject to = ComponentContext.inject(VmSnapshotTemplateObject.class);
        to.configure(vo, store, vmSnapTmplPoolVo);
        return to;
    }

    public VMSnapshotTemplateStoragePoolVO getVmSnapshotTemplateVo() {
        return vmSnapTmplPoolVo;
    }

    public void setSize(Long size) {
        vmSnapTmplPoolVo.setTemplateSize(size);
    }

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }

    public VMSnapshotVO getVmSnapshot() {
        return vmSnapshotVo;
    }

    @Override
    public String getUniqueName() {
        return vmSnapTmplPoolVo.getInstallPath();
    }

    @Override
    public long getId() {
        return vmSnapTmplPoolVo.getId();
    }

    public ObjectInDataStoreStateMachine.State getState() {
        return vmSnapTmplPoolVo.getState();
    }

    public Status getStatus() {
        return vmSnapTmplPoolVo.getStatus();
    }

    @Override
    public String getUuid() {
        return vmSnapTmplPoolVo.getUuid();
    }

    @Override
    public DataObjectType getType() {
        return DataObjectType.VMSNAPSHOT_TEMPLATE;
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event) {
        try {
            objectInStoreMgr.update(this, event);
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException("Failed to update state", e);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Failed to update state", e);
        } finally {
            // in case of OperationFailed, expunge the entry
            if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                objectInStoreMgr.deleteIfNotReady(this);
            }
        }
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event, Answer answer) {
        try {
            if (getDataStore().getRole() == DataStoreRole.Primary) {
                Status statusCreated = Status.CREATED;
                if (answer instanceof CopyCmdAnswer) {
                    CopyCmdAnswer cpyAnswer = (CopyCmdAnswer)answer;
                    VMSnapshotTemplateStoragePoolVO vmSnaptmplPoolRef = vmSnapTmplPoolDao.findById(getId());
                    if (cpyAnswer != null && cpyAnswer.getResult()) {
                        DataTO newDataTo = cpyAnswer.getNewData();
                        if (newDataTo != null && newDataTo instanceof VmSnapshotTemplateObjectTO) {
                            vmSnaptmplPoolRef.setInstallPath(newDataTo.getPath());
                            vmSnaptmplPoolRef.setTemplateSize(((VmSnapshotTemplateObjectTO)newDataTo).getPhysicalSize());
                        }
                        vmSnaptmplPoolRef.setStatus(statusCreated);
                        vmSnapTmplPoolDao.update(vmSnaptmplPoolRef.getId(), vmSnaptmplPoolRef);
                    } else {
                        s_logger.debug("Failed to seed template " + getInstallPath() + " on primary storage from vm snapshot.");
                    }
                    s_logger.debug("Successfully updated the status of vm snapshot template " + getUuid() + " to " + statusCreated);
                } else {
                    s_logger.debug("Templates based on vm snapshots are supported only on primary storage. Skipping event processing for object : " + getUuid());
                }
                objectInStoreMgr.update(this, event);
            }
        } catch (NoTransitionException e) {
            s_logger.debug("failed to update state", e);
            throw new CloudRuntimeException("Failed to update state" + e.toString());
        } catch (Exception ex) {
            s_logger.debug("failed to process event and answer", ex);
            objectInStoreMgr.delete(this);
            throw new CloudRuntimeException("Failed to process event", ex);
        } finally {
            // in case of OperationFailed, expunge the entry
            if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                objectInStoreMgr.deleteIfNotReady(this);
            }
        }
    }

    @Override
    public String getInstallPath() {
        if (installPath != null)
            return installPath;

        if (dataStore == null) {
            return null;
        }

        // managed primary data stores should not have an install path
        if (dataStore instanceof PrimaryDataStore) {
            PrimaryDataStore primaryDataStore = (PrimaryDataStore)dataStore;

            Map<String, String> details = primaryDataStore.getDetails();

            boolean managed = details != null && Boolean.parseBoolean(details.get(PrimaryDataStore.MANAGED));

            if (managed) {
                return null;
            }
        }

        DataObjectInStore obj = objectInStoreMgr.findObject(this, dataStore);
        return obj.getInstallPath();
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    @Override
    public long getAccountId() {
        return vmSnapshotVo.getAccountId();
    }

    @Override
    public String getUri() {
        return null;
    }

    @Override
    public DataTO getTO() {
        DataTO to = null;
        if (dataStore == null) {
            to = new VmSnapshotTemplateObjectTO(this);
        } else {
            to = dataStore.getDriver().getTO(this);
            if (to == null) {
                to = new VmSnapshotTemplateObjectTO(this);
            }
        }

        return to;
    }

    @Override
    public Long getSize() {
        return null;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public void incRefCount() {
    }

    @Override
    public void decRefCount() {
    }

    @Override
    public Long getRefCount() {
        return null;
    }
}
