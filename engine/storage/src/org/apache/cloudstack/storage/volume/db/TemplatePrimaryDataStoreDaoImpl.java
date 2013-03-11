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
package org.apache.cloudstack.storage.volume.db;

import java.util.Date;

import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.db.UpdateBuilder;

@Component
public class TemplatePrimaryDataStoreDaoImpl extends GenericDaoBase<TemplatePrimaryDataStoreVO, Long> implements TemplatePrimaryDataStoreDao {
    private static final Logger s_logger = Logger.getLogger(TemplatePrimaryDataStoreDaoImpl.class);
    protected final SearchBuilder<TemplatePrimaryDataStoreVO> updateSearchBuilder;
    public TemplatePrimaryDataStoreDaoImpl() {
        updateSearchBuilder = createSearchBuilder();
        updateSearchBuilder.and("id", updateSearchBuilder.entity().getId(), Op.EQ);
        updateSearchBuilder.and("state", updateSearchBuilder.entity().getState(), Op.EQ);
        updateSearchBuilder.and("updatedCount", updateSearchBuilder.entity().getUpdatedCount(), Op.EQ);
        updateSearchBuilder.done();
    }
    @Override
    public TemplatePrimaryDataStoreVO findByTemplateIdAndPoolId(long templateId, long poolId) {
        SearchCriteriaService<TemplatePrimaryDataStoreVO, TemplatePrimaryDataStoreVO> sc = SearchCriteria2.create(TemplatePrimaryDataStoreVO.class);
        sc.addAnd(sc.getEntity().getTemplateId(), Op.EQ, templateId);
        sc.addAnd(sc.getEntity().getPoolId(), Op.EQ, poolId);
        return sc.find();
    }

    @Override
    public TemplatePrimaryDataStoreVO findByTemplateIdAndPoolIdAndReady(long templateId, long poolId) {
        SearchCriteriaService<TemplatePrimaryDataStoreVO, TemplatePrimaryDataStoreVO> sc = SearchCriteria2.create(TemplatePrimaryDataStoreVO.class);
        sc.addAnd(sc.getEntity().getTemplateId(), Op.EQ, templateId);
        sc.addAnd(sc.getEntity().getPoolId(), Op.EQ, poolId);
        sc.addAnd(sc.getEntity().getState(), Op.EQ, ObjectInDataStoreStateMachine.State.Ready);
        return sc.find();
    }

    @Override
    public boolean updateState(State currentState, Event event, State nextState, TemplatePrimaryDataStoreVO vo, Object data) {
        Long oldUpdated = vo.getUpdatedCount();
        Date oldUpdatedTime = vo.getLastUpdated();
        
        SearchCriteria<TemplatePrimaryDataStoreVO> sc = updateSearchBuilder.create();
        sc.setParameters("id", vo.getId());
        sc.setParameters("state", currentState);
        sc.setParameters("updatedCount", vo.getUpdatedCount());
        
        vo.incrUpdatedCount();
        
        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        builder.set(vo, "lastUpdated", new Date());
        
        int rows = update((TemplatePrimaryDataStoreVO)vo, sc);
        if (rows == 0 && s_logger.isDebugEnabled()) {
            TemplatePrimaryDataStoreVO template = findByIdIncludingRemoved(vo.getId()); 
            if (template != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
                str.append(": DB Data={id=").append(template.getId()).append("; state=").append(template.getState()).append("; updatecount=").append(template.getUpdatedCount()).append(";updatedTime=").append(template.getLastUpdated());
                str.append(": New Data={id=").append(vo.getId()).append("; state=").append(nextState).append("; event=").append(event).append("; updatecount=").append(vo.getUpdatedCount()).append("; updatedTime=").append(vo.getLastUpdated());
                str.append(": stale Data={id=").append(vo.getId()).append("; state=").append(currentState).append("; event=").append(event).append("; updatecount=").append(oldUpdated).append("; updatedTime=").append(oldUpdatedTime);
            } else {
                s_logger.debug("Unable to update template: id=" + vo.getId() + ", as there is no such template exists in the database anymore");
            }
        }
        return rows > 0;
    }
   
}
