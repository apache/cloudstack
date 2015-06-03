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
package org.apache.cloudstack.framework.security.keystore;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
@Local(value = {KeystoreDao.class})
public class KeystoreDaoImpl extends GenericDaoBase<KeystoreVO, Long> implements KeystoreDao {
    protected final SearchBuilder<KeystoreVO> FindByNameSearch;
    protected final SearchBuilder<KeystoreVO> CertChainSearch;
    protected final SearchBuilder<KeystoreVO> CertChainSearchForDomainSuffix;

    public KeystoreDaoImpl() {
        FindByNameSearch = createSearchBuilder();
        FindByNameSearch.and("name", FindByNameSearch.entity().getName(), Op.EQ);
        FindByNameSearch.done();

        CertChainSearch = createSearchBuilder();
        CertChainSearch.and("key", CertChainSearch.entity().getKey(), Op.NULL);
        CertChainSearch.done();

        CertChainSearchForDomainSuffix = createSearchBuilder();
        CertChainSearchForDomainSuffix.and("key", CertChainSearchForDomainSuffix.entity().getKey(), Op.NULL);
        CertChainSearchForDomainSuffix.and("domainSuffix", CertChainSearchForDomainSuffix.entity().getDomainSuffix(), Op.EQ);
        CertChainSearchForDomainSuffix.done();
    }

    @Override
    public List<KeystoreVO> findCertChain() {
        SearchCriteria<KeystoreVO> sc = CertChainSearch.create();
        List<KeystoreVO> ks = listBy(sc);
        Collections.sort(ks, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Integer seq1 = ((KeystoreVO)o1).getIndex();
                Integer seq2 = ((KeystoreVO)o2).getIndex();
                return seq1.compareTo(seq2);
            }
        });
        return ks;
    }

    @Override
    public List<KeystoreVO> findCertChain(String domainSuffix) {
        SearchCriteria<KeystoreVO> sc =  CertChainSearchForDomainSuffix.create();
        sc.setParameters("domainSuffix", domainSuffix);
        List<KeystoreVO> ks = listBy(sc);
        Collections.sort(ks, new Comparator() { public int compare(Object o1, Object o2) {
            Integer seq1 = ((KeystoreVO)o1).getIndex();
            Integer seq2 = ((KeystoreVO)o2).getIndex();
            return seq1.compareTo(seq2);
        }});
        return ks;
    }

    @Override
    public KeystoreVO findByName(String name) {
        assert (name != null);

        SearchCriteria<KeystoreVO> sc = FindByNameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    @DB
    public void save(String name, String certificate, String key, String domainSuffix) {
        KeystoreVO keystore = findByName(name);
        if (keystore != null) {
            keystore.setCertificate(certificate);
            keystore.setKey(key);
            keystore.setDomainSuffix(domainSuffix);
            this.update(keystore.getId(), keystore);
        } else {
            keystore = new KeystoreVO();
            keystore.setName(name);
            keystore.setCertificate(certificate);
            keystore.setKey(key);
            keystore.setDomainSuffix(domainSuffix);
            this.persist(keystore);
        }
    }

    @Override
    @DB
    public void save(String alias, String certificate, Integer index, String domainSuffix) {
        KeystoreVO ks = findByName(alias);
        if (ks != null) {
            ks.setCertificate(certificate);
            ks.setName(alias);
            ks.setIndex(index);
            ks.setDomainSuffix(domainSuffix);
            this.update(ks.getId(), ks);
        } else {
            ks = new KeystoreVO();
            ks.setCertificate(certificate);
            ks.setName(alias);
            ks.setIndex(index);
            ks.setDomainSuffix(domainSuffix);
            this.persist(ks);
        }
    }
}
