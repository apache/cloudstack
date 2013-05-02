/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.storage.dao;

import static com.cloud.utils.db.SearchCriteria.Op.*;
import static com.cloud.storage.VMTemplateS3VO.*;

import com.cloud.storage.VMTemplateS3VO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Local(VMTemplateS3Dao.class)
public class VMTemplateS3DaoImpl extends GenericDaoBase<VMTemplateS3VO, Long>
        implements VMTemplateS3Dao {

    private final SearchBuilder<VMTemplateS3VO> searchBuilder;

    public VMTemplateS3DaoImpl() {

        super();

        this.searchBuilder = createSearchBuilder();
        this.searchBuilder
                .and(S3_ID_COLUMN_NAME, this.searchBuilder.entity().getS3Id(),
                        EQ)
                .and(TEMPLATE_ID_COLUMN_NAME,
                        this.searchBuilder.entity().getTemplateId(), EQ).done();

    }

    @Override
    public List<VMTemplateS3VO> listByS3Id(final long s3id) {

        final SearchCriteria<VMTemplateS3VO> criteria = this.searchBuilder
                .create();

        criteria.setParameters(S3_ID_COLUMN_NAME, s3id);

        return this.listBy(criteria);

    }

    @Override
    public VMTemplateS3VO findOneByTemplateId(final long templateId) {

        final SearchCriteria<VMTemplateS3VO> criteria = this.searchBuilder
                .create();

        criteria.setParameters(TEMPLATE_ID_COLUMN_NAME, templateId);

        return this.findOneBy(criteria);

    }

    @Override
    public VMTemplateS3VO findOneByS3Template(final long s3Id,
            final long templateId) {

        final SearchCriteria<VMTemplateS3VO> criteria = this.searchBuilder
                .create();

        criteria.setParameters(S3_ID_COLUMN_NAME, s3Id);
        criteria.setParameters(TEMPLATE_ID_COLUMN_NAME, templateId);

        return this.findOneBy(criteria);

    }

    @Override
    public void expungeAllByTemplateId(long templateId) {

        final SearchCriteria<VMTemplateS3VO> criteria = this.searchBuilder
                .create();

        criteria.setParameters(TEMPLATE_ID_COLUMN_NAME, templateId);

        this.expunge(criteria);

    }

}
