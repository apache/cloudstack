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
package org.apache.cloudstack.storage.volume;

import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.storage.volume.db.TemplatePrimaryDataStoreDao;
import org.apache.cloudstack.storage.volume.db.TemplatePrimaryDataStoreVO;

import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

public class TemplateOnPrimaryDataStoreObject implements TemplateOnPrimaryDataStoreInfo {
    protected PrimaryDataStore dataStore;
    protected TemplateInfo template;
    protected TemplatePrimaryDataStoreVO vo;
    protected TemplatePrimaryDataStoreDao templateStoreDao;
    protected TemplatePrimaryDataStoreManager mgr;
    protected StateMachine2<State, Event, TemplatePrimaryDataStoreVO> stateMachine;

    public TemplateOnPrimaryDataStoreObject(PrimaryDataStore primaryDataStore, TemplateInfo template, TemplatePrimaryDataStoreVO vo,
    		TemplatePrimaryDataStoreDao templateStoreDao, TemplatePrimaryDataStoreManager mgr) {
        this.dataStore = primaryDataStore;
        this.template = template;
        this.vo = vo;
        this.templateStoreDao = templateStoreDao;
        this.mgr = mgr;
        this.stateMachine = mgr.getStateMachine();
    }

    @Override
    public String getPath() {
        return vo.getInstallPath();
    }

    @Override
    public void setPath(String path) {
        this.vo.setInstallPath(path);
    }

    @Override
    public PrimaryDataStore getPrimaryDataStore() {
        return this.dataStore;
    }

    @Override
    public TemplateInfo getTemplate() {
        return this.template;
    }

    public void updateStatus(Status status) {
        vo.setDownloadState(status);
        templateStoreDao.update(vo.getId(), vo);
        vo = templateStoreDao.findById(vo.getId());
    }
    
    public void stateTransit(ObjectInDataStoreStateMachine.Event event) {
        try {
            this.stateMachine.transitTo(vo, event, null, templateStoreDao);
            vo = templateStoreDao.findById(vo.getId());
        } catch (NoTransitionException e) {
           throw new CloudRuntimeException("Failed change state", e);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed change state", e);
        }
    }
}
