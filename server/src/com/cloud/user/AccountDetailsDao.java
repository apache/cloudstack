package com.cloud.user;

import java.util.Map;
import com.cloud.utils.db.GenericDao;


public interface AccountDetailsDao extends GenericDao<AccountDetailVO, Long> {
    Map<String, String> findDetails(long accountId);
    
    void persist(long accountId, Map<String, String> details);
    
    AccountDetailVO findDetail(long accountId, String name);

	void deleteDetails(long accountId);
	
	/*
	 * details may or may not include entries which has been in database.
	 * For these existing entries, they will get updated. For these new entries,
	 * they will get created 
	 */
	void update(long accountId, Map<String, String> details);
}
