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

package com.cloud.certificate.dao;

import java.math.BigInteger;

import org.apache.cloudstack.context.CallContext;

import com.cloud.certificate.CrlVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class CrlDaoImpl extends GenericDaoBase<CrlVO, Long> implements CrlDao {

    private final SearchBuilder<CrlVO> CrlBySerialSearch;

    public CrlDaoImpl() {
        super();

        CrlBySerialSearch = createSearchBuilder();
        CrlBySerialSearch.and("certSerial", CrlBySerialSearch.entity().getCertSerial(), SearchCriteria.Op.EQ);
        CrlBySerialSearch.done();
    }

    @Override
    public CrlVO findBySerial(final BigInteger certSerial) {
        if (certSerial == null) {
            return null;
        }
        final SearchCriteria<CrlVO> sc = CrlBySerialSearch.create("certSerial", certSerial.toString(16));
        return findOneBy(sc);
    }

    @Override
    public CrlVO revokeCertificate(final BigInteger certSerial, final String certCn) {
        final CrlVO revokedCertificate = new CrlVO(certSerial, certCn == null ? "" : certCn, CallContext.current().getCallingUserUuid());
        return persist(revokedCertificate);
    }
}
