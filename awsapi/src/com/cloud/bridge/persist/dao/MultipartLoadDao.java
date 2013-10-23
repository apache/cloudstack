// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.bridge.persist.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.bridge.model.MultiPartPartsVO;
import com.cloud.bridge.model.MultiPartUploadsVO;
import com.cloud.bridge.model.MultipartMetaVO;
import com.cloud.bridge.service.core.s3.S3MetaDataEntry;
import com.cloud.bridge.service.core.s3.S3MultipartPart;
import com.cloud.bridge.service.core.s3.S3MultipartUpload;
import com.cloud.bridge.util.OrderedPair;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionLegacy;

public class MultipartLoadDao {
    public static final Logger logger = Logger.getLogger(MultipartLoadDao.class);

    @Inject MultipartMetaDao mpartMetaDao;
    @Inject MultiPartPartsDao mpartPartsDao;
    @Inject MultiPartUploadsDao mpartUploadDao;

    public MultipartLoadDao() {}

    /**
     * If a multipart upload exists with the uploadId value then return the non-null creators
     * accessKey.
     * 
     * @param uploadId
     * @return creator of the multipart upload, and NameKey of upload
     */


    public OrderedPair<String,String> multipartExits( int uploadId ) 
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
            {
        return mpartUploadDao.multipartExits(uploadId);
            }

    /**
     * The multipart upload was either successfully completed or was aborted.   In either case, we need
     * to remove all of its state from the tables.   Note that we have cascade deletes so all tables with
     * uploadId as a foreign key are automatically cleaned.
     * 
     * @param uploadId
     * 
     */
    public void deleteUpload( int uploadId ) {
        mpartUploadDao.deleteUpload(uploadId);
    }

    /**
     * The caller needs to know who initiated the multipart upload.
     * 
     * @param uploadId
     * @return the access key value defining the initiator
     */
    public String getInitiator( int uploadId ) {
        return mpartUploadDao.getAtrributeValue("AccessKey", uploadId);
    }

    /**
     * Create a new "in-process" multipart upload entry to keep track of its state.
     * 
     * @param accessKey
     * @param bucketName
     * @param key
     * @param cannedAccess
     * 
     * @return if positive its the uploadId to be returned to the client
     *
     */
    public int initiateUpload( String accessKey, String bucketName, String key, String cannedAccess, S3MetaDataEntry[] meta ) {
        int uploadId = -1;
        TransactionLegacy txn = null;
        try {
            txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
            Date tod = new Date();
            MultiPartUploadsVO uploadVO = new MultiPartUploadsVO(accessKey,
                    bucketName, key, cannedAccess, tod);
            uploadVO = mpartUploadDao.persist(uploadVO);

            if (null != uploadVO) {
                uploadId = uploadVO.getId().intValue();
                if (null != meta) {
                    for (int i = 0; i < meta.length; i++) {
                        MultipartMetaVO mpartMeta = new MultipartMetaVO();
                        mpartMeta.setUploadID(uploadId);
                        S3MetaDataEntry entry = meta[i];
                        mpartMeta.setName(entry.getName());
                        mpartMeta.setValue(entry.getValue());
                        mpartMetaDao.persist(mpartMeta);
                    }
                    txn.commit();
                }
            }

            return uploadId;
        } finally {
            txn.close();
        }
    }

    /**
     * Remember all the individual parts that make up the entire multipart upload so that once
     * the upload is complete all the parts can be glued together into a single object.  Note, 
     * the caller can over write an existing part.
     * 
     * @param uploadId
     * @param partNumber
     * @param md5
     * @param storedPath
     * @param size
     * @throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
     */
    public void savePart( int uploadId, int partNumber, String md5, String storedPath, int size ) {

        try {
            MultiPartPartsVO partVO = null;

            partVO = mpartPartsDao.findByUploadID(uploadId, partNumber);
            // -> are we doing an update or an insert? (are we over writting an
            // existing entry?)

            if (null == partVO) {
                MultiPartPartsVO part = new MultiPartPartsVO(uploadId,
                        partNumber, md5, storedPath, size, new Date());
                mpartPartsDao.persist(part);
            } else {
                partVO.setMd5(md5);
                partVO.setStoredSize(new Long(size));
                partVO.setCreateTime(new Date());
                partVO.setUploadid(new Long(uploadId));
                partVO.setPartNumber(partNumber);
                mpartPartsDao.updateParts(partVO, uploadId, partNumber);
            }
        } finally {
        }
    }

    /**
     * It is possible for there to be a null canned access policy defined.
     * @param uploadId
     * @return the value defined in the x-amz-acl header or null
     */
    public String getCannedAccess( int uploadId ) {
        return mpartUploadDao.getAtrributeValue("x_amz_acl", uploadId);
    }

    /**
     * When the multipart are being composed into one object we need any meta data to be saved with
     * the new re-constituted object.
     * 
     * @param uploadId
     * @return an array of S3MetaDataEntry (will be null if no meta values exist)
     * @throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
     */
    public S3MetaDataEntry[] getMeta( int uploadId )
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
            {
        List<S3MetaDataEntry> metaList = new ArrayList<S3MetaDataEntry>();
        int count = 0;
        List<MultipartMetaVO> metaVO; 
        try {

            metaVO = mpartMetaDao.getByUploadID(uploadId);
            for (MultipartMetaVO multipartMetaVO : metaVO) {
                S3MetaDataEntry oneMeta = new S3MetaDataEntry();
                oneMeta.setName(  multipartMetaVO.getName());
                oneMeta.setValue( multipartMetaVO.getValue());
                metaList.add( oneMeta );
                count++;
            }

            if ( 0 == count )
                return null;
            else return metaList.toArray(new S3MetaDataEntry[0]);

        } finally {

        }
            }

    /** 
     * The result has to be ordered by key and if there is more than one identical key then all the 
     * identical keys are ordered by create time.
     * 
     * @param bucketName
     * @param maxParts
     * @param prefix - can be null
     * @param keyMarker - can be null
     * @param uploadIdMarker - can be null, should only be defined if keyMarker is not-null
     * @return OrderedPair<S3MultipartUpload[], isTruncated>
     * @throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
     */
    public OrderedPair<S3MultipartUpload[],Boolean> getInitiatedUploads( String bucketName, int maxParts, String prefix, String keyMarker, String uploadIdMarker )
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
            {
        S3MultipartUpload[] inProgress = new S3MultipartUpload[maxParts];
        boolean isTruncated = false;
        int i = 0;
        int pos = 1;
        List<MultiPartUploadsVO> uploadList;
        // -> SQL like condition requires the '%' as a wildcard marker
        if (null != prefix) prefix = prefix + "%";


        try {
            uploadList = mpartUploadDao.getInitiatedUploads(bucketName, maxParts, prefix, keyMarker, uploadIdMarker);
            for (MultiPartUploadsVO uploadsVO : uploadList) {
                Calendar tod = Calendar.getInstance();
                tod.setTime(uploadsVO.getCreateTime());
                inProgress[i] = new S3MultipartUpload();
                inProgress[i].setId( uploadsVO.getId().intValue()); 
                inProgress[i].setAccessKey(uploadsVO.getAccessKey());
                inProgress[i].setLastModified( tod );
                inProgress[i].setBucketName( bucketName );
                inProgress[i].setKey(uploadsVO.getNameKey());
                i++;
            }

            if (i < maxParts)
                inProgress = (S3MultipartUpload[]) resizeArray(inProgress, i);
            return new OrderedPair<S3MultipartUpload[], Boolean>(inProgress,
                    isTruncated);
        }finally {
        }

            }

    /**
     * Return info on a range of upload parts that have already been stored in disk.
     * Note that parts can be uploaded in any order yet we must returned an ordered list
     * of parts thus we use the "ORDERED BY" clause to sort the list.
     * 
     * @param uploadId
     * @param maxParts
     * @param startAt
     * @return an array of S3MultipartPart objects
     * @throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
     */
    public S3MultipartPart[] getParts( int uploadId, int maxParts, int startAt ) 
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
            {
        S3MultipartPart[] parts = new S3MultipartPart[maxParts];
        int i = 0;
        List<MultiPartPartsVO> partsVO;
        try {

            partsVO = mpartPartsDao.getParts(uploadId, startAt + maxParts + 1, startAt);

            for (MultiPartPartsVO partVO : partsVO) {
                Calendar tod = Calendar.getInstance();
                tod.setTime(partVO.getCreateTime());

                parts[i] = new S3MultipartPart();
                parts[i].setPartNumber(partVO.getPartNumber());
                parts[i].setEtag(partVO.getMd5());
                parts[i].setLastModified(tod);
                parts[i].setSize(partVO.getStoredSize().intValue());
                parts[i].setPath(partVO.getStoredPath());
                i++;
            }

            if (i < maxParts) parts = (S3MultipartPart[])resizeArray(parts,i);
            return parts;

        } finally {

        }
            }

    /**
     * How many parts exist after the endMarker part number?
     * 
     * @param uploadId
     * @param endMarker - can be used to see if getUploadedParts was truncated
     * @return number of parts with partNumber greater than endMarker
     * @throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
     */
    public int numParts( int uploadId, int endMarker ) {
        return mpartPartsDao.getnumParts(uploadId, endMarker);
    }

    /**
     * A multipart upload request can have zero to many meta data entries to be applied to the
     * final object.   We need to remember all of the objects meta data until the multipart is complete.
     * 
     * @param uploadId - defines an in-process multipart upload
     * @param meta - an array of meta data to be assocated with the uploadId value
     * 
     */
    private void saveMultipartMeta( int uploadId, S3MetaDataEntry[] meta ) {
        if (null == meta) return;

        TransactionLegacy txn = null;
        try {
            txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
            for( int i=0; i < meta.length; i++ ) 
            {
                S3MetaDataEntry entry = meta[i];
                MultipartMetaVO metaVO = new MultipartMetaVO();
                metaVO.setUploadID(uploadId);
                metaVO.setName(entry.getName());
                metaVO.setValue(entry.getValue());
                metaVO=mpartMetaDao.persist(metaVO);
            }
            txn.commit();
        } finally {
            txn.close();
        }
    }


    /**
     * Reallocates an array with a new size, and copies the contents
     * of the old array to the new array.
     * 
     * @param oldArray  the old array, to be reallocated.
     * @param newSize   the new array size.
     * @return          A new array with the same contents.
     */
    private static Object resizeArray(Object oldArray, int newSize) 
    {
        int oldSize = java.lang.reflect.Array.getLength(oldArray);
        Class elementType = oldArray.getClass().getComponentType();
        Object newArray = java.lang.reflect.Array.newInstance(
                elementType,newSize);
        int preserveLength = Math.min(oldSize,newSize);
        if (preserveLength > 0)
            System.arraycopy (oldArray,0,newArray,0,preserveLength);
        return newArray; 
    }
}
