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

import com.cloud.utils.DateUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

@DB
@Component
public class SAMLTokenDaoImpl extends GenericDaoBase<SAMLTokenVO, Long> implements SAMLTokenDao {

    protected static final String EXPIRE_TOKENS_SQL = "DELETE FROM `saml_token` WHERE `created` < ?";
    protected static final long TOKEN_LIFETIME_MILLIS = TimeUnit.HOURS.toMillis(1);

    public SAMLTokenDaoImpl() {
        super();
    }

    @Override
    public void expireTokens() {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();
            Timestamp cutOff = new Timestamp(DateUtil.currentGMTTime().getTime() - TOKEN_LIFETIME_MILLIS);
            PreparedStatement pstmt = txn.prepareAutoCloseStatement(EXPIRE_TOKENS_SQL);
            pstmt.setTimestamp(1, cutOff, gmtCalendar());
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to flush old SAML tokens due to exception", e);
        }
    }
}
