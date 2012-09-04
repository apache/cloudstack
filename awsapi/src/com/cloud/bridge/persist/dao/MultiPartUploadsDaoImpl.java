package com.cloud.bridge.persist.dao;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import com.cloud.bridge.model.MultiPartPartsVO;
import com.cloud.bridge.model.MultiPartUploadsVO;
import com.cloud.bridge.model.SBucketVO;
import com.cloud.bridge.util.OrderedPair;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={MultiPartUploadsDao.class})
public class MultiPartUploadsDaoImpl extends GenericDaoBase<MultiPartUploadsVO, Long> implements MultiPartUploadsDao {
    
    @Override
    public OrderedPair<String,String> multipartExits( int uploadId ) {
        MultiPartUploadsVO uploadvo = null;
        
        Transaction txn = null; 
        try {
            txn = Transaction.open(Transaction.AWSAPI_DB);
            uploadvo = findById(new Long(uploadId));
            if (null != uploadvo)
                return new OrderedPair<String,String>(uploadvo.getAccessKey(), uploadvo.getNameKey());

            return null;
        } finally {
            txn.close();
        }
    }
    
    @Override
    public void deleteUpload(int uploadId) {
        
        Transaction txn = null; 
        try {
            txn = Transaction.open(Transaction.AWSAPI_DB);
            remove(new Long(uploadId));
            txn.commit();
        }finally {
            txn.close();
        }
    }
    
    @Override
    public String getAtrributeValue(String attribute, int uploadid) {
        Transaction txn = null;
        MultiPartUploadsVO uploadvo = null;
        try {
            txn = Transaction.open(Transaction.AWSAPI_DB);
            uploadvo = findById(new Long(uploadid));
            if (null != uploadvo) {
                if ( attribute.equalsIgnoreCase("AccessKey") )
                    return uploadvo.getAccessKey();
                else if ( attribute.equalsIgnoreCase("x_amz_acl") ) 
                    return uploadvo.getAmzAcl();
            }
            return null;
        } finally {
            txn.close();
        }
    }
    
    @Override
    public List<MultiPartUploadsVO> getInitiatedUploads(String bucketName, int maxParts, String prefix, String keyMarker, String uploadIdMarker) {

        List<MultiPartUploadsVO> uploadList = new ArrayList<MultiPartUploadsVO>();
        
        SearchBuilder<MultiPartUploadsVO> byBucket = createSearchBuilder();
        byBucket.and("BucketName", byBucket.entity().getBucketName() , SearchCriteria.Op.EQ);
        
        if (null != prefix)
            byBucket.and("NameKey", byBucket.entity().getNameKey(), SearchCriteria.Op.LIKE);
        if (null != uploadIdMarker)
            byBucket.and("NameKey", byBucket.entity().getNameKey(), SearchCriteria.Op.GT);
        if (null != uploadIdMarker)
            byBucket.and("ID", byBucket.entity().getId(), SearchCriteria.Op.GT);
        
       Filter filter = new Filter(MultiPartUploadsVO.class, "nameKey", Boolean.TRUE, null, null);
       filter.addOrderBy(MultiPartUploadsVO.class, "createTime", Boolean.TRUE);
       
       Transaction txn = Transaction.open("cloudbridge", Transaction.AWSAPI_DB, true);
       try {
           txn.start();
           SearchCriteria<MultiPartUploadsVO> sc = byBucket.create();
           sc.setParameters("BucketName", bucketName);
           if (null != prefix)
               sc.setParameters("NameKey", prefix);
           if (null != uploadIdMarker)
               sc.setParameters("NameKey", keyMarker);
           if (null != uploadIdMarker)
               sc.setParameters("ID", uploadIdMarker);
           listBy(sc, filter);
       
       }finally {
           txn.close();
       }
        return null;
    }
    
}
