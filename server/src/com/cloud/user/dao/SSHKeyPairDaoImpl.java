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
