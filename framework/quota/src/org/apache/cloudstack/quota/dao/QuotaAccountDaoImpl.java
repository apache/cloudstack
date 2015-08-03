//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.quota.dao;

import javax.ejb.Local;

import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = { QuotaAccountDao.class })
public class QuotaAccountDaoImpl extends GenericDaoBase<QuotaAccountVO, Long> implements QuotaAccountDao {

    @Override
    public QuotaAccountVO findById(Long id){
        QuotaAccountVO result=null;
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
        result = super.findById(id);
        TransactionLegacy.open(opendb).close();
        return result;
    }

    @Override
    public QuotaAccountVO persist(QuotaAccountVO entity){
        QuotaAccountVO result=null;
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
        result = super.persist(entity);
        TransactionLegacy.open(opendb).close();
        return result;
    }

    @Override
    public boolean update(Long id, QuotaAccountVO entity){
        boolean result=false;
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
        result=super.update(id, entity);
        TransactionLegacy.open(opendb).close();
        return result;
    }

}
