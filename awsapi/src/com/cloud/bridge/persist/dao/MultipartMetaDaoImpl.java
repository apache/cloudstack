package com.cloud.bridge.persist.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.bridge.model.MultipartMetaVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={MultipartMetaDao.class})
public class MultipartMetaDaoImpl extends GenericDaoBase<MultipartMetaVO, Long> implements MultipartMetaDao {
    
    @Override
    public List<MultipartMetaVO> getByUploadID (long uploadID) {
        SearchBuilder <MultipartMetaVO> searchByUID = createSearchBuilder();
        searchByUID.and("UploadID", searchByUID.entity().getUploadID(), SearchCriteria.Op.EQ);
        searchByUID.done();
        Transaction txn = Transaction.open(Transaction.AWSAPI_DB);
        try {
            txn.start();
            SearchCriteria<MultipartMetaVO> sc = searchByUID.create();
            sc.setParameters("UploadID", uploadID);
            return  listBy(sc);
        
        }finally {
            txn.close();
        }
        
    }
}

