// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.db;

import java.sql.PreparedStatement;

import com.cloud.utils.exception.CloudRuntimeException;

public class DbTestDao extends GenericDaoBase<DbTestVO, Long> implements GenericDao<DbTestVO, Long> {
    protected DbTestDao() {
    }

    @DB
    public void create(int fldInt, long fldLong, String fldString) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        try {
            txn.start();
            pstmt = txn.prepareAutoCloseStatement("insert into cloud.test(fld_int, fld_long, fld_string) values(?, ?, ?)");
            pstmt.setInt(1, fldInt);
            pstmt.setLong(2, fldLong);
            pstmt.setString(3, fldString);

            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception e) {
            throw new CloudRuntimeException("Problem with creating a record in test table", e);
        }
    }

    @DB
    public void update(int fldInt, long fldLong, String fldString) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        try {
            txn.start();
            pstmt = txn.prepareAutoCloseStatement("update cloud.test set fld_int=?, fld_long=? where fld_string=?");
            pstmt.setInt(1, fldInt);
            pstmt.setLong(2, fldLong);
            pstmt.setString(3, fldString);

            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception e) {
            throw new CloudRuntimeException("Problem with creating a record in test table", e);
        }
    }
}
