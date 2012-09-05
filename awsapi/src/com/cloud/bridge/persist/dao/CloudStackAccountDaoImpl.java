package com.cloud.bridge.persist.dao;

import javax.ejb.Local;

import com.cloud.bridge.model.CloudStackAccountVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={CloudStackAccountDao.class})
public class CloudStackAccountDaoImpl extends GenericDaoBase<CloudStackAccountVO, String> implements CloudStackAccountDao {
    
    @Override
    public String getDefaultZoneId(String accountId) {
        
        SearchBuilder<CloudStackAccountVO> SearchByUUID = createSearchBuilder();
        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
        try {
            txn.start();
            SearchByUUID.and("uuid", SearchByUUID.entity().getUuid(),
                    SearchCriteria.Op.EQ);
            SearchByUUID.done();
            SearchCriteria<CloudStackAccountVO> sc = SearchByUUID.create();
            sc.setParameters("uuid", accountId);
            CloudStackAccountVO account = findOneBy(sc);
            if (null != account) 
                if(null != account.getDefaultZoneId())
                    return Long.toString(account.getDefaultZoneId());
            return null;
        } finally {
            txn.commit();
            txn.close();
        }

    }
    

}
