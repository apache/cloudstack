package com.cloud.user.dao;

import java.util.List;

import com.cloud.user.SSHKeyPairVO;
import com.cloud.utils.db.GenericDao;

public interface SSHKeyPairDao extends GenericDao<SSHKeyPairVO, Long> {

	public List<SSHKeyPairVO> listKeyPairs(long accountId, long domainId);
	
	public List<SSHKeyPairVO> listKeyPairsByName(long accountId, long domainId, String name);

	public List<SSHKeyPairVO> listKeyPairsByFingerprint(long accountId, long domainId, String fingerprint);
	
	public SSHKeyPairVO findByName(long accountId, long domainId, String name);

	public boolean deleteByName(long accountId, long domainId, String name);

}
