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

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.List;

@Component
@Local(value = { QuotaAccountDao.class })
public class QuotaAccountDaoImpl extends GenericDaoBase<QuotaAccountVO, Long> implements QuotaAccountDao {
    public static final Logger s_logger = Logger.getLogger(QuotaAccountDaoImpl.class.getName());

    @Override
    public List<QuotaAccountVO> listAll() {
        List<QuotaAccountVO> result = null;
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        try {
            TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
            result = super.listAll();
        } catch (Exception e) {
            s_logger.error("QuotaAccountDaoImpl::listAll() failed due to: " + e.getMessage());
            throw new CloudRuntimeException("Unable to list Quota Accounts");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return result;
    }

    @Override
    public QuotaAccountVO findById(Long id) {
        QuotaAccountVO result = null;
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        try {
            TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
            result = super.findById(id);
        } catch (Exception e) {
            s_logger.error("QuotaAccountDaoImpl::findById() failed due to: " + e.getMessage());
            throw new CloudRuntimeException("Unable to find Quota Account by ID");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return result;
    }

    @Override
    public QuotaAccountVO persist(QuotaAccountVO entity) {
        QuotaAccountVO result = null;
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        try {
            TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
            result = super.persist(entity);
        } catch (Exception e) {
            s_logger.error("QuotaAccountDaoImpl::persist() failed due to: " + e.getMessage());
            throw new CloudRuntimeException("Unable to save Quota Account");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return result;
    }

    @Override
    public boolean update(Long id, QuotaAccountVO entity) {
        boolean result = false;
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        try {
            TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
            result = super.update(id, entity);
        } catch (Exception e) {
            s_logger.error("QuotaAccountDaoImpl::update() failed due to: " + e.getMessage());
            throw new CloudRuntimeException("Unable to update Quota Account");
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return result;
    }

}
