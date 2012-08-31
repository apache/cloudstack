package com.cloud.bridge.persist.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.bridge.model.MultiPartPartsVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={MultiPartPartsDao.class})
public class MultiPartPartsDaoImpl extends GenericDaoBase<MultiPartPartsVO, Long> implements MultiPartPartsDao {

    @Override
    public  List<MultiPartPartsVO> getParts(int uploadId, int maxParts, int startAt ) {
        
        SearchBuilder<MultiPartPartsVO> ByUploadID = createSearchBuilder();
        ByUploadID.and("UploadID", ByUploadID.entity().getUploadid(), SearchCriteria.Op.EQ);
        ByUploadID.and("partNumber", ByUploadID.entity().getPartNumber(), SearchCriteria.Op.GT);
        ByUploadID.and("partNumber", ByUploadID.entity().getPartNumber(), SearchCriteria.Op.LT);
        Filter filter = new Filter(MultiPartPartsVO.class, "partNumber", Boolean.TRUE, null, null);
        
        Transaction txn = Transaction.currentTxn();  // Transaction.open("cloudbridge", Transaction.AWSAPI_DB, true);
        try {
            txn.start();
            SearchCriteria<MultiPartPartsVO> sc = ByUploadID.create();
            sc.setParameters("UploadID", new Long(uploadId));
            sc.setParameters("partNumber", startAt);
            sc.setParameters("partNumber", maxParts);
        return listBy(sc, filter);
        
        } finally {
            txn.close();
        }
    }
    
    @Override
    public int getnumParts( int uploadId, int endMarker ) {
        SearchBuilder<MultiPartPartsVO> byUploadID = createSearchBuilder();
        byUploadID.and("UploadID", byUploadID.entity().getUploadid(), SearchCriteria.Op.EQ);
        byUploadID.and("partNumber", byUploadID.entity().getPartNumber(), SearchCriteria.Op.GT);
        Transaction txn = Transaction.currentTxn();  // Transaction.open("cloudbridge", Transaction.AWSAPI_DB, true);
        try {
            txn.start();
            SearchCriteria<MultiPartPartsVO> sc = byUploadID.create();
            sc.setParameters("UploadID", new Long(uploadId));
            sc.setParameters("partNumber", endMarker);
            return listBy(sc).size();
        
        } finally {
            txn.close();
        }

        
    }
    
    @Override
    public MultiPartPartsVO findByUploadID(int uploadId, int partNumber) {
        
        SearchBuilder<MultiPartPartsVO> byUploadID = createSearchBuilder();
        byUploadID.and("UploadID", byUploadID.entity().getUploadid(), SearchCriteria.Op.EQ);
        byUploadID.and("partNumber", byUploadID.entity().getPartNumber(), SearchCriteria.Op.EQ);
        Transaction txn = Transaction.currentTxn();  // Transaction.open("cloudbridge", Transaction.AWSAPI_DB, true);
        try {
            txn.start();
            SearchCriteria<MultiPartPartsVO> sc = byUploadID.create();
            sc.setParameters("UploadID", new Long(uploadId));
            sc.setParameters("partNumber", partNumber);
            return findOneBy(sc);
            
        } finally {
            txn.close();
        }
        
    }
    
    @Override
    public void updateParts(MultiPartPartsVO partVO, int uploadId, int partNumber) {
        
        SearchBuilder<MultiPartPartsVO> byUploadID = createSearchBuilder();
        byUploadID.and("UploadID", byUploadID.entity().getUploadid(), SearchCriteria.Op.EQ);
        byUploadID.and("partNumber", byUploadID.entity().getPartNumber(), SearchCriteria.Op.EQ);
        Transaction txn = Transaction.currentTxn();  // Transaction.open("cloudbridge", Transaction.AWSAPI_DB, true);
        try {
            txn.start();
            SearchCriteria<MultiPartPartsVO> sc = byUploadID.create();
            sc.setParameters("UploadID", new Long(uploadId));
            sc.setParameters("partNumber", partNumber);
            update(partVO, sc);
            txn.commit();
        
        } finally {
            txn.close();
        }
    }
    
}

