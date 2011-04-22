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

import javax.ejb.Local;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value={KeystoreDao.class})
public class KeystoreDaoImpl extends GenericDaoBase<KeystoreVO, Long> implements KeystoreDao {
    protected final SearchBuilder<KeystoreVO> FindByNameSearch;

	public KeystoreDaoImpl() {
		FindByNameSearch = createSearchBuilder();
		FindByNameSearch.and("name", FindByNameSearch.entity().getName(), Op.EQ);
		FindByNameSearch.done();
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
}
