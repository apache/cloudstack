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
package org.apache.cloudstack.saml;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.sql.PreparedStatement;

@DB
@Component
@Local(value = {SAMLTokenDao.class})
public class SAMLTokenDaoImpl extends GenericDaoBase<SAMLTokenVO, Long> implements SAMLTokenDao {

    public SAMLTokenDaoImpl() {
        super();
    }

    @Override
    public void expireTokens() {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();
            String sql = "DELETE FROM `saml_token` WHERE `created` < (NOW() - INTERVAL 1 HOUR)";
            PreparedStatement pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to flush old SAML tokens due to exception", e);
        }
    }
}
