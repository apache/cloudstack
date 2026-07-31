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
package org.apache.cloudstack.email.template.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.email.template.EmailTemplateVO;

public class EmailTemplateDaoImpl extends GenericDaoBase<EmailTemplateVO, Long> implements EmailTemplateDao {

    private static final String NAME = "name";

    private SearchBuilder<EmailTemplateVO> findByName;

    public EmailTemplateDaoImpl() {
        findByName = createSearchBuilder();
        findByName.and(NAME, findByName.entity().getName(), SearchCriteria.Op.EQ);
        findByName.done();
    }

    @Override
    public EmailTemplateVO findByName(String name) {
        SearchCriteria<EmailTemplateVO> sc = findByName.create();
        sc.setParameters(NAME, name);
        return findOneBy(sc);
    }
}
