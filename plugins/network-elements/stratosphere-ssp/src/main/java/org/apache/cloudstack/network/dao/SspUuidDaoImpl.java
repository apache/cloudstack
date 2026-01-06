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
package org.apache.cloudstack.network.dao;

import java.util.List;



import com.cloud.network.Network;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.NicProfile;

public class SspUuidDaoImpl extends GenericDaoBase<SspUuidVO, Long> implements SspUuidDao {


    protected final SearchBuilder<SspUuidVO> native2uuid;
    protected final SearchBuilder<SspUuidVO> uuid2native;
    protected final SearchBuilder<SspUuidVO> uuidfetch;

    public SspUuidDaoImpl() {
        native2uuid = createSearchBuilder();
        native2uuid.and("obj_class", native2uuid.entity().getObjClass(), Op.EQ);
        native2uuid.and("obj_id", native2uuid.entity().getObjId(), Op.EQ);
        native2uuid.done();

        uuid2native = createSearchBuilder();
        uuid2native.and("obj_class", uuid2native.entity().getObjClass(), Op.EQ);
        uuid2native.and("uuid", uuid2native.entity().getUuid(), Op.EQ);
        uuid2native.done();

        uuidfetch = createSearchBuilder();
        uuidfetch.and("uuid", uuidfetch.entity().getUuid(), Op.EQ);
        uuidfetch.done();
    }

    @Override
    public String findUuidByNetwork(Network network) {
        SearchCriteria<SspUuidVO> cs = native2uuid.create();
        cs.setParameters("obj_class", SspUuidVO.objClassNetwork);
        cs.setParameters("obj_id", network.getId());
        SspUuidVO vo = findOneBy(cs);
        if (vo != null) {
            return vo.getUuid();
        }
        return null;
    }

    @Override
    public String findUuidByNicProfile(NicProfile nicProfile) {
        SearchCriteria<SspUuidVO> cs = native2uuid.create();
        cs.setParameters("obj_class", SspUuidVO.objClassNicProfile);
        cs.setParameters("obj_id", nicProfile.getId());
        SspUuidVO vo = findOneBy(cs);
        if (vo != null) {
            return vo.getUuid();
        }
        return null;
    }

    @Override
    public List<SspUuidVO> listUUidVoByNicProfile(NicProfile nicProfile) {
        SearchCriteria<SspUuidVO> cs = native2uuid.create();
        cs.setParameters("obj_class", SspUuidVO.objClassNicProfile);
        cs.setParameters("obj_id", nicProfile.getId());
        return listBy(cs);
    }

    @Override
    public Long findNetworkIdByUuid(String uuid) {
        return findByUuid(SspUuidVO.objClassNetwork, uuid);
    }

    @Override
    public Long findNicProfileIdByUuid(String uuid) {
        return findByUuid(SspUuidVO.objClassNicProfile, uuid);
    }

    private Long findByUuid(String clazz, String uuid) {
        SearchCriteria<SspUuidVO> cs = uuid2native.create();
        cs.setParameters("obj_class", clazz);
        cs.setParameters("uuid", uuid);
        SspUuidVO vo = findOneBy(cs);
        if (vo != null) {
            return vo.getObjId();
        }
        return null;
    }

    @Override
    public int removeUuid(String uuid) {
        SearchCriteria<SspUuidVO> cs = uuidfetch.create();
        cs.setParameters("uuid", uuid);
        return this.remove(cs);
    }
}
