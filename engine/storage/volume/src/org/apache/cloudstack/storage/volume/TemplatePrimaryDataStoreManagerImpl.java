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

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreStateMachine.Event;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreStateMachine.State;
import org.apache.cloudstack.storage.volume.db.TemplatePrimaryDataStoreDao;
import org.apache.cloudstack.storage.volume.db.TemplatePrimaryDataStoreVO;
import org.springframework.stereotype.Component;

import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateMachine2;

@Component
public class TemplatePrimaryDataStoreManagerImpl implements TemplatePrimaryDataStoreManager {
    @Inject
    TemplatePrimaryDataStoreDao templateStoreDao;
    protected long waitingTime = 1800; //half an hour
    protected long waitingReties = 10;
    protected StateMachine2<State, Event, TemplatePrimaryDataStoreVO> stateMachines;
    public TemplatePrimaryDataStoreManagerImpl() {
        stateMachines = new StateMachine2<State, Event, TemplatePrimaryDataStoreVO>();
        stateMachines.addTransition(TemplateOnPrimaryDataStoreStateMachine.State.Allocated, Event.CreateRequested, TemplateOnPrimaryDataStoreStateMachine.State.Creating);
        stateMachines.addTransition(TemplateOnPrimaryDataStoreStateMachine.State.Creating, Event.OperationSuccessed, TemplateOnPrimaryDataStoreStateMachine.State.Ready);
        stateMachines.addTransition(TemplateOnPrimaryDataStoreStateMachine.State.Creating, Event.OperationFailed, TemplateOnPrimaryDataStoreStateMachine.State.Failed);
        stateMachines.addTransition(TemplateOnPrimaryDataStoreStateMachine.State.Failed, Event.CreateRequested, TemplateOnPrimaryDataStoreStateMachine.State.Creating);
        stateMachines.addTransition(TemplateOnPrimaryDataStoreStateMachine.State.Ready, Event.DestroyRequested, TemplateOnPrimaryDataStoreStateMachine.State.Destroying);
        stateMachines.addTransition(TemplateOnPrimaryDataStoreStateMachine.State.Destroying, Event.OperationSuccessed, TemplateOnPrimaryDataStoreStateMachine.State.Destroyed);
        stateMachines.addTransition(TemplateOnPrimaryDataStoreStateMachine.State.Destroying, Event.OperationFailed, TemplateOnPrimaryDataStoreStateMachine.State.Destroying);
        stateMachines.addTransition(TemplateOnPrimaryDataStoreStateMachine.State.Destroying, Event.DestroyRequested, TemplateOnPrimaryDataStoreStateMachine.State.Destroying);
    }
    
    private TemplatePrimaryDataStoreVO waitingForTemplateDownload(TemplateInfo template, PrimaryDataStoreInfo dataStore) {
        //the naive version, polling.
        long retries = waitingReties;
        TemplatePrimaryDataStoreVO templateStoreVO = null;
        do {
            try {
                Thread.sleep(waitingTime);
            } catch (InterruptedException e) {
                
            }
            
            templateStoreVO = templateStoreDao.findByTemplateIdAndPoolIdAndReady(template.getId(), dataStore.getId());
            if (templateStoreVO != null) {
                break;
            }
            retries--;
        } while (retries > 0);
        
        if (templateStoreVO == null) {
            throw new CloudRuntimeException("waiting too long for template downloading, marked it as failed");
        }
        
        return templateStoreVO;
    }
    @Override
    public TemplateOnPrimaryDataStoreObject createTemplateOnPrimaryDataStore(TemplateInfo template, PrimaryDataStoreInfo dataStore) {
        TemplatePrimaryDataStoreVO templateStoreVO = null;
        boolean freshNewTemplate = false;
        templateStoreVO = templateStoreDao.findByTemplateIdAndPoolId(template.getId(), dataStore.getId());
        if (templateStoreVO == null) {
            try {
                templateStoreVO = new TemplatePrimaryDataStoreVO(dataStore.getId(), template.getId());
                templateStoreVO = templateStoreDao.persist(templateStoreVO);
                freshNewTemplate = true;
            } catch (Throwable th) {
                templateStoreVO = templateStoreDao.findByTemplateIdAndPoolId(template.getId(), dataStore.getId());
                if (templateStoreVO == null) {
                    throw new CloudRuntimeException("Failed create db entry: " + th.toString());
                }
            }
        }
        
        //If it's not a fresh template downloading, waiting for other people downloading finished.
        if (!freshNewTemplate && templateStoreVO.getState() != TemplateOnPrimaryDataStoreStateMachine.State.Ready) {
            templateStoreVO = waitingForTemplateDownload(template, dataStore);
        }

        TemplateOnPrimaryDataStoreObject templateStoreObject = new TemplateOnPrimaryDataStoreObject(dataStore, template, templateStoreVO, templateStoreDao, this);
        return templateStoreObject;
    }

    @Override
    public TemplateOnPrimaryDataStoreObject findTemplateOnPrimaryDataStore(TemplateInfo template, PrimaryDataStoreInfo dataStore) {
        SearchCriteriaService<TemplatePrimaryDataStoreVO, TemplatePrimaryDataStoreVO> sc = SearchCriteria2.create(TemplatePrimaryDataStoreVO.class);
        sc.addAnd(sc.getEntity().getTemplateId(), Op.EQ, template.getId());
        sc.addAnd(sc.getEntity().getPoolId(), Op.EQ, dataStore.getId());
        sc.addAnd(sc.getEntity().getDownloadState(), Op.EQ, VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        TemplatePrimaryDataStoreVO templateStoreVO = sc.find();
        if (templateStoreVO == null) {
        	return null;
        }
        
        TemplateOnPrimaryDataStoreObject templateStoreObject = new TemplateOnPrimaryDataStoreObject(dataStore, template, templateStoreVO, templateStoreDao, this);
        return templateStoreObject;
    }
    
    @Override
    public StateMachine2<State, Event, TemplatePrimaryDataStoreVO> getStateMachine() {
        return stateMachines;
    }
     
}
