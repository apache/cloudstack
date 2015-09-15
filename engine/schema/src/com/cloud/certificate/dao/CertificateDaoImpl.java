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

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.certificate.CertificateVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;

@Component
@Local(value = {CertificateDao.class})
@DB
public class CertificateDaoImpl extends GenericDaoBase<CertificateVO, Long> implements CertificateDao {


    public CertificateDaoImpl() {

    }

    @Override
    public Long persistCustomCertToDb(String certStr, CertificateVO cert, Long managementServerId) {
        try {
            cert.setCertificate(certStr);
            cert.setUpdated("Y");
            update(cert.getId(), cert);
            return cert.getId();
        } catch (Exception e) {
            logger.warn("Unable to read the certificate: " + e);
            return new Long(0);
        }
    }
}
