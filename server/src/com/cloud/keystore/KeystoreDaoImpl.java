/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.keystore;

import java.sql.PreparedStatement;
import java.util.Comparator;
import java.util.List;

import javax.ejb.Local;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

import edu.emory.mathcs.backport.java.util.Collections;

@Local(value={KeystoreDao.class})
public class KeystoreDaoImpl extends GenericDaoBase<KeystoreVO, Long> implements KeystoreDao {
    protected final SearchBuilder<KeystoreVO> FindByNameSearch;
    protected final SearchBuilder<KeystoreVO> CertChainSearch;

	public KeystoreDaoImpl() {
		FindByNameSearch = createSearchBuilder();
		FindByNameSearch.and("name", FindByNameSearch.entity().getName(), Op.EQ);
		FindByNameSearch.done();
		
		CertChainSearch = createSearchBuilder();
		CertChainSearch.and("key", CertChainSearch.entity().getKey(), Op.NULL);
		CertChainSearch.done();
	}
	
	@Override
	public List<KeystoreVO> findCertChain() {
		SearchCriteria<KeystoreVO> sc =  CertChainSearch.create();
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
		assert(name != null);
		
		SearchCriteria<KeystoreVO> sc =  FindByNameSearch.create();
		sc.setParameters("name", name);
		return findOneBy(sc);
	}
	
	@Override
	@DB
	public void save(String name, String certificate, String key, String domainSuffix) {
		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
			
			String sql = "INSERT INTO keystore (`name`, `certificate`, `key`, `domain_suffix`) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE `certificate`=?, `key`=?, `domain_suffix`=?";
			PreparedStatement pstmt = txn.prepareAutoCloseStatement(sql);
			pstmt.setString(1, name);
			pstmt.setString(2, certificate);
			pstmt.setString(3, key);
			pstmt.setString(4, domainSuffix);
			pstmt.setString(5, certificate);
			pstmt.setString(6, key);
			pstmt.setString(7, domainSuffix);

			pstmt.executeUpdate();
			txn.commit();
		} catch(Exception e) {
			txn.rollback();
			throw new CloudRuntimeException("Unable to save certificate under name " + name + " due to exception", e);
		}
	}
	
	@Override
	@DB
	public void save(String alias, String certificate, Integer index, String domainSuffix) {
		KeystoreVO ks = this.findByName(alias);
		if (ks != null) {
			ks.setCertificate(certificate);
			ks.setName(alias);
			ks.setIndex(index);
			ks.setDomainSuffix(domainSuffix);
			this.update(ks.getId(), ks);
		} else {
			KeystoreVO newks = new KeystoreVO();
			newks.setCertificate(certificate);
			newks.setName(alias);
			newks.setIndex(index);
			newks.setDomainSuffix(domainSuffix);
			this.persist(newks);
		}
	}
}
