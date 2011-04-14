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

package com.cloud.user.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.user.SSHKeyPairVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;

@Local(value={SSHKeyPairDao.class})
public class SSHKeyPairDaoImpl extends GenericDaoBase<SSHKeyPairVO, Long> implements SSHKeyPairDao {

	@Override
	public List<SSHKeyPairVO> listKeyPairs(long accountId, long domainId) {
		SearchCriteria<SSHKeyPairVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
		return listBy(sc);
	}
	
	@Override 
	public List<SSHKeyPairVO> listKeyPairsByName(long accountId, long domainId, String name) {
		SearchCriteria<SSHKeyPairVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addAnd("name", SearchCriteria.Op.EQ, name);
		return listBy(sc);
	}
	
	@Override 
	public List<SSHKeyPairVO> listKeyPairsByFingerprint(long accountId, long domainId, String fingerprint) {
		SearchCriteria<SSHKeyPairVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        sc.addAnd("fingerprint", SearchCriteria.Op.EQ, fingerprint);
		return listBy(sc);
	}
	
	@Override
	public SSHKeyPairVO findByName(long accountId, long domainId, String name) {
		SearchCriteria<SSHKeyPairVO> sc = createSearchCriteria();
        sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
		sc.addAnd("name", SearchCriteria.Op.EQ, name);
		return findOneBy(sc);
	}
	
	@Override
	public boolean deleteByName(long accountId, long domainId, String name) {
		SSHKeyPairVO pair = findByName(accountId, domainId, name);
		if (pair == null) 
			return false;
		
		expunge(pair.getId());
		return true;
	}
		
}
