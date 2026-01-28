//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.backup.dao;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.cloudstack.backup.ImageTransfer;
import org.apache.cloudstack.backup.ImageTransferVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class ImageTransferDaoImpl extends GenericDaoBase<ImageTransferVO, Long> implements ImageTransferDao {

    private SearchBuilder<ImageTransferVO> backupIdSearch;
    private SearchBuilder<ImageTransferVO> uuidSearch;
    private SearchBuilder<ImageTransferVO> nbdPortSearch;
    private SearchBuilder<ImageTransferVO> volumeSearch;
    private SearchBuilder<ImageTransferVO> phaseDirectionSearch;

    public ImageTransferDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        backupIdSearch = createSearchBuilder();
        backupIdSearch.and("backupId", backupIdSearch.entity().getBackupId(), SearchCriteria.Op.EQ);
        backupIdSearch.done();

        uuidSearch = createSearchBuilder();
        uuidSearch.and("uuid", uuidSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        uuidSearch.done();

        nbdPortSearch = createSearchBuilder();
        nbdPortSearch.and("nbdPort", nbdPortSearch.entity().getNbdPort(), SearchCriteria.Op.EQ);
        nbdPortSearch.done();

        volumeSearch = createSearchBuilder();
        volumeSearch.and("volumeId", volumeSearch.entity().getDiskId(), SearchCriteria.Op.EQ);
        volumeSearch.done();

        phaseDirectionSearch = createSearchBuilder();
        phaseDirectionSearch.and("phase", phaseDirectionSearch.entity().getPhase(), SearchCriteria.Op.EQ);
        phaseDirectionSearch.and("direction", phaseDirectionSearch.entity().getDirection(), SearchCriteria.Op.EQ);
        phaseDirectionSearch.done();
    }

    @Override
    public List<ImageTransferVO> listByBackupId(Long backupId) {
        SearchCriteria<ImageTransferVO> sc = backupIdSearch.create();
        sc.setParameters("backupId", backupId);
        return listBy(sc);
    }

    @Override
    public ImageTransferVO findByUuid(String uuid) {
        SearchCriteria<ImageTransferVO> sc = uuidSearch.create();
        sc.setParameters("uuid", uuid);
        return findOneBy(sc);
    }

    @Override
    public ImageTransferVO findByNbdPort(int port) {
        SearchCriteria<ImageTransferVO> sc = nbdPortSearch.create();
        sc.setParameters("nbdPort", port);
        return findOneBy(sc);
    }

    @Override
    public ImageTransferVO findByVolume(Long volumeId) {
        SearchCriteria<ImageTransferVO> sc = volumeSearch.create();
        sc.setParameters("volumeId", volumeId);
        return findOneBy(sc);
    }

    @Override
    public List<ImageTransferVO> listByPhaseAndDirection(ImageTransfer.Phase phase, ImageTransfer.Direction direction) {
        SearchCriteria<ImageTransferVO> sc = phaseDirectionSearch.create();
        sc.setParameters("phase", phase);
        sc.setParameters("direction", direction);
        return listBy(sc);
    }
}
