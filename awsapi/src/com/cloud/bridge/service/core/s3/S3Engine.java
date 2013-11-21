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
package com.cloud.bridge.service.core.s3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;

import com.cloud.bridge.io.S3CAStorBucketAdapter;
import com.cloud.bridge.io.S3FileSystemBucketAdapter;
import com.cloud.bridge.model.BucketPolicyVO;
import com.cloud.bridge.model.MHostMountVO;
import com.cloud.bridge.model.MHostVO;
import com.cloud.bridge.model.SAcl;
import com.cloud.bridge.model.SAclVO;
import com.cloud.bridge.model.SBucket;
import com.cloud.bridge.model.SBucketVO;
import com.cloud.bridge.model.SHost;
import com.cloud.bridge.model.SHostVO;
import com.cloud.bridge.model.SMetaVO;
import com.cloud.bridge.model.SObjectItemVO;
import com.cloud.bridge.model.SObjectVO;
import com.cloud.bridge.persist.dao.BucketPolicyDao;
import com.cloud.bridge.persist.dao.MHostDao;
import com.cloud.bridge.persist.dao.MHostMountDao;
import com.cloud.bridge.persist.dao.MultipartLoadDao;
import com.cloud.bridge.persist.dao.SAclDao;
import com.cloud.bridge.persist.dao.SBucketDao;
import com.cloud.bridge.persist.dao.SHostDao;
import com.cloud.bridge.persist.dao.SMetaDao;
import com.cloud.bridge.persist.dao.SObjectDao;
import com.cloud.bridge.persist.dao.SObjectItemDao;
import com.cloud.bridge.service.UserContext;
import com.cloud.bridge.service.controller.s3.ServiceProvider;
import com.cloud.bridge.service.core.s3.S3BucketPolicy.PolicyAccess;
import com.cloud.bridge.service.core.s3.S3CopyObjectRequest.MetadataDirective;
import com.cloud.bridge.service.core.s3.S3PolicyAction.PolicyActions;
import com.cloud.bridge.service.core.s3.S3PolicyCondition.ConditionKeys;
import com.cloud.bridge.service.exception.HostNotMountedException;
import com.cloud.bridge.service.exception.InternalErrorException;
import com.cloud.bridge.service.exception.InvalidBucketName;
import com.cloud.bridge.service.exception.NoSuchObjectException;
import com.cloud.bridge.service.exception.ObjectAlreadyExistsException;
import com.cloud.bridge.service.exception.OutOfServiceException;
import com.cloud.bridge.service.exception.OutOfStorageException;
import com.cloud.bridge.service.exception.PermissionDeniedException;
import com.cloud.bridge.util.DateHelper;
import com.cloud.bridge.util.OrderedPair;
import com.cloud.bridge.util.PolicyParser;
import com.cloud.bridge.util.StringHelper;
import com.cloud.bridge.util.Triple;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;

/**
 * The CRUD control actions to be invoked from S3BucketAction or S3ObjectAction.
 */
@Component
public class S3Engine {
    protected final static Logger logger = Logger.getLogger(S3Engine.class);
    @Inject
    SHostDao shostDao;
    @Inject
    MHostDao mhostDao;
    @Inject
    BucketPolicyDao bPolicy;
    @Inject
    BucketPolicyDao bPolicyDao;
    @Inject
    SBucketDao bucketDao;
    @Inject
    SAclDao aclDao;
    @Inject
    SAclDao saclDao;
    @Inject
    SObjectDao objectDao;
    @Inject
    SObjectItemDao itemDao;
    @Inject
    SMetaDao metaDao;
    @Inject
    MHostMountDao mountDao;

    static SAclDao s_saclDao;
    static BucketPolicyDao s_bPolicy;

    private final int LOCK_ACQUIRING_TIMEOUT_SECONDS = 10;        // ten seconds

    private final Map<Integer, S3BucketAdapter> bucketAdapters = new HashMap<Integer, S3BucketAdapter>();

    public S3Engine() {
        bucketAdapters.put(SHost.STORAGE_HOST_TYPE_LOCAL, new S3FileSystemBucketAdapter());
        bucketAdapters.put(SHost.STORAGE_HOST_TYPE_CASTOR, new S3CAStorBucketAdapter());
    }

    @PostConstruct
    void init() {
        s_saclDao = saclDao;
        s_bPolicy = bPolicy;
    }

    /**
     * Return a S3CopyObjectResponse which represents an object being copied from source
     * to destination bucket.
     * Called from S3ObjectAction when copying an object.
     * This can be treated as first a GET followed by a PUT of the object the user wants to copy.
     */

    public S3CopyObjectResponse handleRequest(S3CopyObjectRequest request) {
        S3CopyObjectResponse response = new S3CopyObjectResponse();

        // [A] Get the object we want to copy
        S3GetObjectRequest getRequest = new S3GetObjectRequest();
        getRequest.setBucketName(request.getSourceBucketName());
        getRequest.setKey(request.getSourceKey());
        getRequest.setVersion(request.getVersion());
        getRequest.setConditions(request.getConditions());

        getRequest.setInlineData(true);
        getRequest.setReturnData(true);
        if (MetadataDirective.COPY == request.getDirective())
            getRequest.setReturnMetadata(true);
        else
            getRequest.setReturnMetadata(false);

        //-> before we do anything verify the permissions on a copy basis
        String destinationBucketName = request.getDestinationBucketName();
        String destinationKeyName = request.getDestinationKey();
        S3PolicyContext context = new S3PolicyContext(PolicyActions.PutObject, destinationBucketName);
        context.setKeyName(destinationKeyName);
        context.setEvalParam(ConditionKeys.MetaData, request.getDirective().toString());
        context.setEvalParam(ConditionKeys.CopySource, "/" + request.getSourceBucketName() + "/" + request.getSourceKey());
        if (PolicyAccess.DENY == verifyPolicy(context))
            throw new PermissionDeniedException("Access Denied - bucket policy DENY result");

        S3GetObjectResponse originalObject = handleRequest(getRequest);
        int resultCode = originalObject.getResultCode();
        if (200 != resultCode) {
            response.setResultCode(resultCode);
            response.setResultDescription(originalObject.getResultDescription());
            return response;
        }

        response.setCopyVersion(originalObject.getVersion());

        // [B] Put the object into the destination bucket
        S3PutObjectInlineRequest putRequest = new S3PutObjectInlineRequest();
        putRequest.setBucketName(request.getDestinationBucketName());
        putRequest.setKey(destinationKeyName);
        if (MetadataDirective.COPY == request.getDirective())
            putRequest.setMetaEntries(originalObject.getMetaEntries());
        else
            putRequest.setMetaEntries(request.getMetaEntries());
        putRequest.setAcl(request.getAcl());                    // -> if via a SOAP call
        putRequest.setCannedAccess(request.getCannedAccess());  // -> if via a REST call
        putRequest.setContentLength(originalObject.getContentLength());
        putRequest.setData(originalObject.getData());

        S3PutObjectInlineResponse putResp = handleRequest(putRequest);
        response.setResultCode(putResp.resultCode);
        response.setResultDescription(putResp.getResultDescription());
        response.setETag(putResp.getETag());
        response.setLastModified(putResp.getLastModified());
        response.setPutVersion(putResp.getVersion());
        return response;
    }

    public S3CreateBucketResponse handleRequest(S3CreateBucketRequest request) {
        S3CreateBucketResponse response = new S3CreateBucketResponse();
        String cannedAccessPolicy = request.getCannedAccess();
        String bucketName = request.getBucketName();
        response.setBucketName(bucketName);
        TransactionLegacy txn = null;
        verifyBucketName(bucketName, false);

        S3PolicyContext context = new S3PolicyContext(PolicyActions.CreateBucket, bucketName);
        context.setEvalParam(ConditionKeys.Acl, cannedAccessPolicy);
        if (PolicyAccess.DENY == verifyPolicy(context))
            throw new PermissionDeniedException("Access Denied - bucket policy DENY result");
        OrderedPair<SHostVO, String> shost_storagelocation_pair = null;
        boolean success = false;
        try {
            txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);

            if (bucketDao.getByName(request.getBucketName()) != null)
                throw new ObjectAlreadyExistsException("Bucket already exists");

            shost_storagelocation_pair = allocBucketStorageHost(request.getBucketName(), null);
            SBucketVO sbucket =
                new SBucketVO(request.getBucketName(), DateHelper.currentGMTTime(), UserContext.current().getCanonicalUserId(), shost_storagelocation_pair.getFirst());

            shost_storagelocation_pair.getFirst().getBuckets().add(sbucket);
            // bucketDao.save(sbucket);
            sbucket = bucketDao.persist(sbucket);
            S3AccessControlList acl = request.getAcl();

            if (null != cannedAccessPolicy)
                setCannedAccessControls(cannedAccessPolicy, "SBucket", sbucket.getId(), sbucket);
            else if (null != acl)
                aclDao.save("SBucket", sbucket.getId(), acl);
            else
                setSingleAcl("SBucket", sbucket.getId(), SAcl.PERMISSION_FULL);

            success = true;
        } finally {
            if (!success && shost_storagelocation_pair != null) {
                S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(shost_storagelocation_pair.getFirst());
                bucketAdapter.deleteContainer(shost_storagelocation_pair.getSecond(), request.getBucketName());
            }
            txn.rollback();
            txn.close();
        }
        return response;
    }

    /**
     * Return a S3Response which represents the effect of an object being deleted from its bucket.
     * Called from S3BucketAction when deleting an object.
     */

    public S3Response handleRequest(S3DeleteBucketRequest request) {
        S3Response response = new S3Response();
        //
        String bucketName = request.getBucketName();
        SBucketVO sbucket = bucketDao.getByName(bucketName);

        TransactionLegacy txn = null;
        if (sbucket != null) {
            txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
            txn.start();
            S3PolicyContext context = new S3PolicyContext(PolicyActions.DeleteBucket, bucketName);
            switch (verifyPolicy(context)) {
                case ALLOW:
                    // The bucket policy can give users permission to delete a
                    // bucket whereas ACLs cannot
                    break;

                case DENY:
                    throw new PermissionDeniedException("Access Denied - bucket policy DENY result");

                case DEFAULT_DENY:
                default:
                    // Irrespective of what the ACLs say, only the owner can delete
                    // a bucket
                    String client = UserContext.current().getCanonicalUserId();
                    if (!client.equals(sbucket.getOwnerCanonicalId())) {
                        throw new PermissionDeniedException("Access Denied - only the owner can delete a bucket");
                    }
                    break;
            }

            // Delete the file from its storage location
            OrderedPair<SHostVO, String> host_storagelocation_pair = getBucketStorageHost(sbucket);
            S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());
            bucketAdapter.deleteContainer(host_storagelocation_pair.getSecond(), request.getBucketName());

            // Cascade-deleting can delete related SObject/SObjectItem objects, but not SAcl, SMeta and policy objects.
            // To delete SMeta & SAcl objects:
            // (1)Get all the objects in the bucket,
            // (2)then all the items in each object,
            // (3) then all meta & acl data for each item
            Set<SObjectVO> objectsInBucket = sbucket.getObjectsInBucket();
            Iterator<SObjectVO> it = objectsInBucket.iterator();
            while (it.hasNext()) {
                SObjectVO oneObject = it.next();
                Set<SObjectItemVO> itemsInObject = oneObject.getItems();
                Iterator<SObjectItemVO> is = itemsInObject.iterator();
                while (is.hasNext()) {
                    SObjectItemVO oneItem = is.next();
                    deleteMetaData(oneItem.getId());
                    deleteObjectAcls("SObjectItem", oneItem.getId());
                }
            }

            // Delete all the policy state associated with the bucket
            try {
                ServiceProvider.getInstance().deleteBucketPolicy(bucketName);
                bPolicyDao.deletePolicy(bucketName);
            } catch (Exception e) {
                logger.error("When deleting a bucket we must try to delete its policy: ", e);
            }

            deleteBucketAcls(sbucket.getId());
            bucketDao.remove(sbucket.getId());

            response.setResultCode(204);
            response.setResultDescription("OK");

            txn.close();
        } else {
            response.setResultCode(404);
            response.setResultDescription("Bucket does not exist");
        }
        return response;
    }

    /**
     * Return a S3ListBucketResponse which represents a list of up to 1000 objects contained ins  the bucket.
     * Called from S3BucketAction for GETting objects and for GETting object versions.
     */

    public S3ListBucketResponse listBucketContents(S3ListBucketRequest request, boolean includeVersions) {
        S3ListBucketResponse response = new S3ListBucketResponse();
        String bucketName = request.getBucketName();
        String prefix = request.getPrefix();
        if (prefix == null)
            prefix = StringHelper.EMPTY_STRING;
        String marker = request.getMarker();
        if (marker == null)
            marker = StringHelper.EMPTY_STRING;

        String delimiter = request.getDelimiter();
        int maxKeys = request.getMaxKeys();
        if (maxKeys <= 0)
            maxKeys = 1000;

        //
        SBucketVO sbucket = bucketDao.getByName(bucketName);
        if (sbucket == null)
            throw new NoSuchObjectException("Bucket " + bucketName + " does not exist");

        PolicyActions action = (includeVersions ? PolicyActions.ListBucketVersions : PolicyActions.ListBucket);
        S3PolicyContext context = new S3PolicyContext(action, bucketName);
        context.setEvalParam(ConditionKeys.MaxKeys, new String("" + maxKeys));
        context.setEvalParam(ConditionKeys.Prefix, prefix);
        context.setEvalParam(ConditionKeys.Delimiter, delimiter);
        verifyAccess(context, "SBucket", sbucket.getId(), SAcl.PERMISSION_READ);

        // Wen execting the query, request one more item so that we know how to set isTruncated flag
        List<SObjectVO> l = null;

        if (includeVersions)
            l = objectDao.listAllBucketObjects(sbucket, prefix, marker, maxKeys + 1);
        else
            l = objectDao.listBucketObjects(sbucket, prefix, marker, maxKeys + 1);

        response.setBucketName(bucketName);
        response.setMarker(marker);
        response.setMaxKeys(maxKeys);
        response.setPrefix(prefix);
        response.setDelimiter(delimiter);
        if (null != l) {
            response.setTruncated(l.size() > maxKeys);
            if (l.size() > maxKeys) {
                response.setNextMarker(l.get(l.size() - 1).getNameKey());
            }
        }
        // If needed - SOAP response does not support versioning
        response.setContents(composeListBucketContentEntries(l, prefix, delimiter, maxKeys, includeVersions, request.getVersionIdMarker()));
        response.setCommonPrefixes(composeListBucketPrefixEntries(l, prefix, delimiter, maxKeys));
        return response;
    }

    /**
     * Return a S3ListAllMyBucketResponse which represents a list of all buckets owned by the requester.
     * Called from S3BucketAction for GETting all buckets.
     * To check on bucket policies defined we have to (look for and) evaluate the policy on each
     * bucket the user owns.
     */
    public S3ListAllMyBucketsResponse handleRequest(S3ListAllMyBucketsRequest request) {
        S3ListAllMyBucketsResponse response = new S3ListAllMyBucketsResponse();

        // "...you can only list buckets for which you are the owner."
        List<SBucketVO> buckets = bucketDao.listBuckets(UserContext.current().getCanonicalUserId());
        S3CanonicalUser owner = new S3CanonicalUser();
        owner.setID(UserContext.current().getCanonicalUserId());
        owner.setDisplayName("");
        response.setOwner(owner);

        if (buckets != null) {
            S3ListAllMyBucketsEntry[] entries = new S3ListAllMyBucketsEntry[buckets.size()];
            int i = 0;
            for (SBucketVO bucket : buckets) {
                String bucketName = bucket.getName();
                S3PolicyContext context = new S3PolicyContext(PolicyActions.ListAllMyBuckets, bucketName);
                verifyAccess(context, "SBucket", bucket.getId(), SAcl.PERMISSION_PASS);

                entries[i] = new S3ListAllMyBucketsEntry();
                entries[i].setName(bucketName);
                entries[i].setCreationDate(DateHelper.toCalendar(bucket.getCreateTime()));
                i++;
            }
            response.setBuckets(entries);
        }
        return response;
    }

    /**
     * Return an S3Response representing the result of PUTTING the ACL of a given bucket.
     * Called from S3BucketAction to PUT its ACL.
     */

    public S3Response handleRequest(S3SetBucketAccessControlPolicyRequest request) {
        S3Response response = new S3Response();
        String bucketName = request.getBucketName();
        SBucketVO sbucket = bucketDao.getByName(bucketName);
        if (sbucket == null) {
            response.setResultCode(404);
            response.setResultDescription("Bucket does not exist");
            return response;
        }

        S3PolicyContext context = new S3PolicyContext(PolicyActions.PutBucketAcl, bucketName);
        verifyAccess(context, "SBucket", sbucket.getId(), SAcl.PERMISSION_WRITE_ACL);

        aclDao.save("SBucket", sbucket.getId(), request.getAcl());

        response.setResultCode(200);
        response.setResultDescription("OK");
        return response;
    }

    /**
     * Return a S3AccessControlPolicy representing the ACL of a given bucket.
     * Called from S3BucketAction to GET its ACL.
     */

    public S3AccessControlPolicy handleRequest(S3GetBucketAccessControlPolicyRequest request) {
        S3AccessControlPolicy policy = new S3AccessControlPolicy();
        String bucketName = request.getBucketName();
        SBucketVO sbucket = bucketDao.getByName(bucketName);
        if (sbucket == null)
            throw new NoSuchObjectException("Bucket " + bucketName + " does not exist");

        S3CanonicalUser owner = new S3CanonicalUser();
        owner.setID(sbucket.getOwnerCanonicalId());
        owner.setDisplayName("");
        policy.setOwner(owner);

        S3PolicyContext context = new S3PolicyContext(PolicyActions.GetBucketAcl, bucketName);
        verifyAccess(context, "SBucket", sbucket.getId(), SAcl.PERMISSION_READ_ACL);

        List<SAclVO> grants = aclDao.listGrants("SBucket", sbucket.getId());
        policy.setGrants(S3Grant.toGrants(grants));
        return policy;
    }

    /**
     * This method should be called if a multipart upload is aborted OR has completed successfully and
     * the individual parts have to be cleaned up.
     * Called from S3ObjectAction when executing at completion or when aborting multipart upload.
     * @param bucketName
     * @param uploadId
     * @param verifyPermissiod - If false then do not check the user's permission to clean up the state
     */
    public int freeUploadParts(String bucketName, int uploadId, boolean verifyPermission) {

        // -> we need to look up the final bucket to figure out which mount
        // point to use to save the part in
        // SBucketDao bucketDao = new SBucketDao();
        SBucketVO bucket = bucketDao.getByName(bucketName);
        if (bucket == null) {
            logger.error("initiateMultipartUpload failed since " + bucketName + " does not exist");
            return 404;
        }

        OrderedPair<SHostVO, String> host_storagelocation_pair = getBucketStorageHost(bucket);
        S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());

        try {
            MultipartLoadDao uploadDao = new MultipartLoadDao();
            OrderedPair<String, String> exists = uploadDao.multipartExits(uploadId);

            if (null == exists) {
                logger.error("initiateMultipartUpload failed since multipart upload" + uploadId + " does not exist");
                return 404;
            }

            // -> the multipart initiator or bucket owner can do this action by
            // default
            if (verifyPermission) {
                String initiator = uploadDao.getInitiator(uploadId);
                if (null == initiator || !initiator.equals(UserContext.current().getAccessKey())) {
                    // -> write permission on a bucket allows a PutObject /
                    // DeleteObject action on any object in the bucket
                    S3PolicyContext context = new S3PolicyContext(PolicyActions.AbortMultipartUpload, bucketName);
                    context.setKeyName(exists.getSecond());
                    verifyAccess(context, "SBucket", bucket.getId(), SAcl.PERMISSION_WRITE);
                }
            }

            // -> first get a list of all the uploaded files and delete one by
            // one
            S3MultipartPart[] parts = uploadDao.getParts(uploadId, 10000, 0);
            for (int i = 0; i < parts.length; i++) {
                bucketAdapter.deleteObject(host_storagelocation_pair.getSecond(), ServiceProvider.getInstance().getMultipartDir(), parts[i].getPath());
            }
            uploadDao.deleteUpload(uploadId);
            return 204;

        } catch (PermissionDeniedException e) {
            logger.error("freeUploadParts failed due to [" + e.getMessage() + "]", e);
            throw e;
        } catch (Exception e) {
            logger.error("freeUploadParts failed due to [" + e.getMessage() + "]", e);
            return 500;
        }
    }

    /**
     * The initiator must have permission to write to the bucket in question in order to initiate
     * a multipart upload.  Also check to make sure the special folder used to store parts of
     * a multipart exists for this bucket.
     * Called from S3ObjectAction during many stages of multipart upload.
     */
    public S3PutObjectInlineResponse initiateMultipartUpload(S3PutObjectInlineRequest request) {
        S3PutObjectInlineResponse response = new S3PutObjectInlineResponse();
        String bucketName = request.getBucketName();
        String nameKey = request.getKey();

        // -> does the bucket exist and can we write to it?
        SBucketVO bucket = bucketDao.getByName(bucketName);
        if (bucket == null) {
            logger.error("initiateMultipartUpload failed since " + bucketName + " does not exist");
            response.setResultCode(404);
        }

        S3PolicyContext context = new S3PolicyContext(PolicyActions.PutObject, bucketName);
        context.setKeyName(nameKey);
        context.setEvalParam(ConditionKeys.Acl, request.getCannedAccess());
        verifyAccess(context, "SBucket", bucket.getId(), SAcl.PERMISSION_WRITE);

        createUploadFolder(bucketName);

        try {
            MultipartLoadDao uploadDao = new MultipartLoadDao();
            int uploadId = uploadDao.initiateUpload(UserContext.current().getAccessKey(), bucketName, nameKey, request.getCannedAccess(), request.getMetaEntries());
            response.setUploadId(uploadId);
            response.setResultCode(200);

        } catch (Exception e) {
            logger.error("initiateMultipartUpload exception: ", e);
            response.setResultCode(500);
        }

        return response;
    }

    /**
     * Save the object fragment in a special (i.e., hidden) directory inside the same mount point as
     * the bucket location that the final object will be stored in.
     * Called from S3ObjectAction during many stages of multipart upload.
     * @param request
     * @param uploadId
     * @param partNumber
     * @return S3PutObjectInlineResponse
     */
    public S3PutObjectInlineResponse saveUploadPart(S3PutObjectInlineRequest request, int uploadId, int partNumber) {
        S3PutObjectInlineResponse response = new S3PutObjectInlineResponse();
        String bucketName = request.getBucketName();

        // -> we need to look up the final bucket to figure out which mount point to use to save the part in
        //SBucketDao bucketDao = new SBucketDao();
        SBucketVO bucket = bucketDao.getByName(bucketName);
        if (bucket == null) {
            logger.error("saveUploadedPart failed since " + bucketName + " does not exist");
            response.setResultCode(404);
        }
        S3PolicyContext context = new S3PolicyContext(PolicyActions.PutObject, bucketName);
        context.setKeyName(request.getKey());
        verifyAccess(context, "SBucket", bucket.getId(), SAcl.PERMISSION_WRITE);

        OrderedPair<SHostVO, String> host_storagelocation_pair = getBucketStorageHost(bucket);
        S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());
        String itemFileName = new String(uploadId + "-" + partNumber);
        InputStream is = null;

        try {
            is = request.getDataInputStream();
            String md5Checksum = bucketAdapter.saveObject(is, host_storagelocation_pair.getSecond(), ServiceProvider.getInstance().getMultipartDir(), itemFileName);
            response.setETag(md5Checksum);
            MultipartLoadDao uploadDao = new MultipartLoadDao();
            uploadDao.savePart(uploadId, partNumber, md5Checksum, itemFileName, (int)request.getContentLength());
            response.setResultCode(200);

        } catch (IOException e) {
            logger.error("UploadPart failed due to " + e.getMessage(), e);
            response.setResultCode(500);
        } catch (OutOfStorageException e) {
            logger.error("UploadPart failed due to " + e.getMessage(), e);
            response.setResultCode(500);
        } catch (Exception e) {
            logger.error("UploadPart failed due to " + e.getMessage(), e);
            response.setResultCode(500);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("UploadPart unable to close stream from data handler.", e);
                }
            }
        }

        return response;
    }

    /**
     * Create the real object represented by all the parts of the multipart upload.
     * Called from S3ObjectAction at completion of multipart upload.
     * @param httpResp - Servlet response handle to return the headers of the response (including version header)
     * @param request - Normal parameters needed to create a new object (including metadata)
     * @param parts - List of files that make up the multipart
     * @param outputStream - Response output stream
     * N.B. - This method can be long-lasting
     * We are required to keep the connection alive by returning whitespace characters back periodically.
     */

    public S3PutObjectInlineResponse concatentateMultipartUploads(HttpServletResponse httpResp, S3PutObjectInlineRequest request, S3MultipartPart[] parts,
        OutputStream outputStream) throws IOException {
        // [A] Set up and initial error checking
        S3PutObjectInlineResponse response = new S3PutObjectInlineResponse();
        String bucketName = request.getBucketName();
        String key = request.getKey();
        S3MetaDataEntry[] meta = request.getMetaEntries();

        SBucketVO bucket = bucketDao.getByName(bucketName);
        if (bucket == null) {
            logger.error("completeMultipartUpload( failed since " + bucketName + " does not exist");
            response.setResultCode(404);
        }

        // [B] Now we need to create the final re-assembled object
        // -> the allocObjectItem checks for the bucket policy PutObject
        // permissions
        OrderedPair<SObjectVO, SObjectItemVO> object_objectitem_pair = allocObjectItem(bucket, key, meta, null, request.getCannedAccess());
        OrderedPair<SHostVO, String> host_storagelocation_pair = getBucketStorageHost(bucket);

        S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());
        String itemFileName = object_objectitem_pair.getSecond().getStoredPath();

        // -> Amazon defines that we must return a 200 response immediately to
        // the client, but
        // -> we don't know the version header until we hit here
        httpResp.setStatus(200);
        httpResp.setContentType("text/xml; charset=UTF-8");
        String version = object_objectitem_pair.getSecond().getVersion();
        if (null != version)
            httpResp.addHeader("x-amz-version-id", version);
        httpResp.flushBuffer();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
        // [C] Re-assemble the object from its uploaded file parts
        try {
            // explicit transaction control to avoid holding transaction during
            // long file concatenation process
            txn.start();
            OrderedPair<String, Long> result =
                bucketAdapter.concatentateObjects(host_storagelocation_pair.getSecond(), bucket.getName(), itemFileName, ServiceProvider.getInstance().getMultipartDir(),
                    parts, outputStream);

            response.setETag(result.getFirst());
            response.setLastModified(DateHelper.toCalendar(object_objectitem_pair.getSecond().getLastModifiedTime()));
            SObjectItemVO item = itemDao.findById(object_objectitem_pair.getSecond().getId());
            item.setMd5(result.getFirst());
            item.setStoredSize(result.getSecond().longValue());
            itemDao.update(item.getId(), item);
            response.setResultCode(200);
        } catch (Exception e) {
            logger.error("completeMultipartUpload failed due to " + e.getMessage(), e);
            txn.close();
        }
        return response;
    }

    /**
     * Return a S3PutObjectInlineResponse which represents an object being created into a bucket
     * Called from S3ObjectAction when PUTting or POTing an object.
     */
    @DB
    public S3PutObjectInlineResponse handleRequest(S3PutObjectInlineRequest request) {
        S3PutObjectInlineResponse response = new S3PutObjectInlineResponse();
        String bucketName = request.getBucketName();
        String key = request.getKey();
        long contentLength = request.getContentLength();
        S3MetaDataEntry[] meta = request.getMetaEntries();
        S3AccessControlList acl = request.getAcl();

        SBucketVO bucket = bucketDao.getByName(bucketName);
        if (bucket == null)
            throw new NoSuchObjectException("Bucket " + bucketName + " does not exist");

        // Is the caller allowed to write the object?
        // The allocObjectItem checks for the bucket policy PutObject permissions
        OrderedPair<SObjectVO, SObjectItemVO> object_objectitem_pair = allocObjectItem(bucket, key, meta, acl, request.getCannedAccess());
        OrderedPair<SHostVO, String> host_storagelocation_pair = getBucketStorageHost(bucket);

        S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());
        String itemFileName = object_objectitem_pair.getSecond().getStoredPath();
        InputStream is = null;
        TransactionLegacy txn = null;
        try {
            // explicit transaction control to avoid holding transaction during file-copy process

            txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
            txn.start();
            is = request.getDataInputStream();
            String md5Checksum = bucketAdapter.saveObject(is, host_storagelocation_pair.getSecond(), bucket.getName(), itemFileName);
            response.setETag(md5Checksum);
            response.setLastModified(DateHelper.toCalendar(object_objectitem_pair.getSecond().getLastModifiedTime()));
            response.setVersion(object_objectitem_pair.getSecond().getVersion());

            //SObjectItemDaoImpl itemDao = new SObjectItemDaoImpl();
            SObjectItemVO item = itemDao.findById(object_objectitem_pair.getSecond().getId());
            item.setMd5(md5Checksum);
            item.setStoredSize(contentLength);
            itemDao.update(item.getId(), item);
            txn.commit();
        } catch (IOException e) {
            logger.error("PutObjectInline failed due to " + e.getMessage(), e);
        } catch (OutOfStorageException e) {
            logger.error("PutObjectInline failed due to " + e.getMessage(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("PutObjectInline unable to close stream from data handler.", e);
                }
            }
            txn.close();
        }

        return response;
    }

    /**
     * Return a S3PutObjectResponse which represents an object being created into a bucket
     * Called from S3RestServlet when processing a DIME request.
     */

    public S3PutObjectResponse handleRequest(S3PutObjectRequest request) {
        S3PutObjectResponse response = new S3PutObjectResponse();
        String bucketName = request.getBucketName();
        String key = request.getKey();
        long contentLength = request.getContentLength();
        S3MetaDataEntry[] meta = request.getMetaEntries();
        S3AccessControlList acl = request.getAcl();

        SBucketVO bucket = bucketDao.getByName(bucketName);
        if (bucket == null)
            throw new NoSuchObjectException("Bucket " + bucketName + " does not exist");

        // Is the caller allowed to write the object?
        // The allocObjectItem checks for the bucket policy PutObject permissions
        OrderedPair<SObjectVO, SObjectItemVO> object_objectitem_pair = allocObjectItem(bucket, key, meta, acl, null);
        OrderedPair<SHostVO, String> host_storagelocation_pair = getBucketStorageHost(bucket);

        S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());
        String itemFileName = object_objectitem_pair.getSecond().getStoredPath();
        InputStream is = null;
        TransactionLegacy txn = null;
        try {
            // explicit transaction control to avoid holding transaction during file-copy process

            txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
            txn.start();

            is = request.getInputStream();
            String md5Checksum = bucketAdapter.saveObject(is, host_storagelocation_pair.getSecond(), bucket.getName(), itemFileName);
            response.setETag(md5Checksum);
            response.setLastModified(DateHelper.toCalendar(object_objectitem_pair.getSecond().getLastModifiedTime()));

            SObjectItemVO item = itemDao.findById(object_objectitem_pair.getSecond().getId());
            item.setMd5(md5Checksum);
            item.setStoredSize(contentLength);
            itemDao.update(item.getId(), item);
            txn.commit();

        } catch (OutOfStorageException e) {
            logger.error("PutObject failed due to " + e.getMessage(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("Unable to close stream from data handler.", e);
                }
            }
            txn.close();
        }

        return response;
    }

    /**
     * The ACL of an object is set at the object version level. By default, PUT sets the ACL of the latest
     * version of an object. To set the ACL of a different version, using the versionId subresource.
     * Called from S3ObjectAction to PUT an object's ACL.
     */

    public S3Response handleRequest(S3SetObjectAccessControlPolicyRequest request) {
        S3PolicyContext context = null;

        // [A] First find the object in the bucket
        S3Response response = new S3Response();
        String bucketName = request.getBucketName();
        SBucketVO sbucket = bucketDao.getByName(bucketName);
        if (sbucket == null) {
            response.setResultCode(404);
            response.setResultDescription("Bucket " + bucketName + "does not exist");
            return response;
        }

        String nameKey = request.getKey();
        SObjectVO sobject = objectDao.getByNameKey(sbucket, nameKey);
        if (sobject == null) {
            response.setResultCode(404);
            response.setResultDescription("Object " + request.getKey() + " in bucket " + bucketName + " does not exist");
            return response;
        }

        String deletionMark = sobject.getDeletionMark();
        if (null != deletionMark) {
            response.setResultCode(404);
            response.setResultDescription("Object " + request.getKey() + " has been deleted (1)");
            return response;
        }

        // [B] Versioning allow the client to ask for a specific version not just the latest
        SObjectItemVO item = null;
        int versioningStatus = sbucket.getVersioningStatus();
        String wantVersion = request.getVersion();
        if (SBucket.VERSIONING_ENABLED == versioningStatus && null != wantVersion)
            item = sobject.getVersion(wantVersion);
        else
            item = sobject.getLatestVersion((SBucket.VERSIONING_ENABLED != versioningStatus));

        if (item == null) {
            response.setResultCode(404);
            response.setResultDescription("Object " + request.getKey() + " has been deleted (2)");
            return response;
        }

        if (SBucket.VERSIONING_ENABLED == versioningStatus) {
            context = new S3PolicyContext(PolicyActions.PutObjectAclVersion, bucketName);
            context.setEvalParam(ConditionKeys.VersionId, wantVersion);
            response.setVersion(item.getVersion());
        } else
            context = new S3PolicyContext(PolicyActions.PutObjectAcl, bucketName);
        context.setKeyName(nameKey);
        verifyAccess(context, "SObjectItem", item.getId(), SAcl.PERMISSION_WRITE_ACL);

        // -> the acl always goes on the instance of the object
        aclDao.save("SObjectItem", item.getId(), request.getAcl());

        response.setResultCode(200);
        response.setResultDescription("OK");
        return response;
    }

    /**
     * By default, GET returns ACL information about the latest version of an object. To return ACL
     * information about a different version, use the versionId subresource
     * Called from S3ObjectAction to get an object's ACL.
     */

    public S3AccessControlPolicy handleRequest(S3GetObjectAccessControlPolicyRequest request) {
        S3PolicyContext context = null;

        // [A] Does the object exist that holds the ACL we are looking for?
        S3AccessControlPolicy policy = new S3AccessControlPolicy();

        String bucketName = request.getBucketName();
        SBucketVO sbucket = bucketDao.getByName(bucketName);
        if (sbucket == null)
            throw new NoSuchObjectException("Bucket " + bucketName + " does not exist");

        //SObjectDaoImpl sobjectDao = new SObjectDaoImpl();
        String nameKey = request.getKey();
        SObjectVO sobject = objectDao.getByNameKey(sbucket, nameKey);
        if (sobject == null)
            throw new NoSuchObjectException("Object " + request.getKey() + " does not exist");

        String deletionMark = sobject.getDeletionMark();
        if (null != deletionMark) {
            policy.setResultCode(404);
            policy.setResultDescription("Object " + request.getKey() + " has been deleted (1)");
            return policy;
        }

        // [B] Versioning allow the client to ask for a specific version not just the latest
        SObjectItemVO item = null;
        int versioningStatus = sbucket.getVersioningStatus();
        String wantVersion = request.getVersion();
        if (SBucket.VERSIONING_ENABLED == versioningStatus && null != wantVersion)
            item = sobject.getVersion(wantVersion);
        else
            item = sobject.getLatestVersion((SBucket.VERSIONING_ENABLED != versioningStatus));

        if (item == null) {
            policy.setResultCode(404);
            policy.setResultDescription("Object " + request.getKey() + " has been deleted (2)");
            return policy;
        }

        if (SBucket.VERSIONING_ENABLED == versioningStatus) {
            context = new S3PolicyContext(PolicyActions.GetObjectVersionAcl, bucketName);
            context.setEvalParam(ConditionKeys.VersionId, wantVersion);
            policy.setVersion(item.getVersion());
        } else
            context = new S3PolicyContext(PolicyActions.GetObjectAcl, bucketName);
        context.setKeyName(nameKey);
        verifyAccess(context, "SObjectItem", item.getId(), SAcl.PERMISSION_READ_ACL);

        // [C] ACLs are ALWAYS on an instance of the object
        S3CanonicalUser owner = new S3CanonicalUser();
        owner.setID(sobject.getOwnerCanonicalId());
        owner.setDisplayName("");
        policy.setOwner(owner);
        policy.setResultCode(200);

        List<SAclVO> grants = aclDao.listGrants("SObjectItem", item.getId());
        policy.setGrants(S3Grant.toGrants(grants));
        return policy;
    }

    /**
     * Handle requests for GET object and HEAD "get object extended"
     * Called from S3ObjectAction for GET and HEAD of an object.
     */

    public S3GetObjectResponse handleRequest(S3GetObjectRequest request) {
        S3GetObjectResponse response = new S3GetObjectResponse();
        S3PolicyContext context = null;
        boolean ifRange = false;
        long bytesStart = request.getByteRangeStart();
        long bytesEnd = request.getByteRangeEnd();
        int resultCode = 200;

        // [A] Verify that the bucket and the object exist

        String bucketName = request.getBucketName();
        SBucketVO sbucket = bucketDao.getByName(bucketName);
        if (sbucket == null) {
            response.setResultCode(404);
            response.setResultDescription("Bucket " + request.getBucketName() + " does not exist");
            return response;
        }

        String nameKey = request.getKey();
        SObjectVO sobject = objectDao.getByNameKey(sbucket, nameKey);
        if (sobject == null) {
            response.setResultCode(404);
            response.setResultDescription("Object " + request.getKey() + " does not exist in bucket " + request.getBucketName());
            return response;
        }

        String deletionMark = sobject.getDeletionMark();
        if (null != deletionMark) {
            response.setDeleteMarker(deletionMark);
            response.setResultCode(404);
            response.setResultDescription("Object " + request.getKey() + " has been deleted (1)");
            return response;
        }

        // [B] Versioning allow the client to ask for a specific version not just the latest
        SObjectItemVO item = null;
        int versioningStatus = sbucket.getVersioningStatus();
        String wantVersion = request.getVersion();
        if (SBucket.VERSIONING_ENABLED == versioningStatus && null != wantVersion)
            item = sobject.getVersion(wantVersion);
        else
            item = sobject.getLatestVersion((SBucket.VERSIONING_ENABLED != versioningStatus));

        if (item == null) {
            response.setResultCode(404);
            response.setResultDescription("Object " + request.getKey() + " has been deleted (2)");
            return response;
        }

        if (SBucket.VERSIONING_ENABLED == versioningStatus) {
            context = new S3PolicyContext(PolicyActions.GetObjectVersion, bucketName);
            context.setEvalParam(ConditionKeys.VersionId, wantVersion);
        } else
            context = new S3PolicyContext(PolicyActions.GetObject, bucketName);
        context.setKeyName(nameKey);
        verifyAccess(context, "SObjectItem", item.getId(), SAcl.PERMISSION_READ);

        // [C] Handle all the IFModifiedSince ... conditions, and access privileges
        // -> http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.27 (HTTP If-Range header)
        if (request.isReturnCompleteObjectOnConditionFailure() && (0 <= bytesStart && 0 <= bytesEnd))
            ifRange = true;

        resultCode = conditionPassed(request.getConditions(), item.getLastModifiedTime(), item.getMd5(), ifRange);
        if (-1 == resultCode) {
            // -> If-Range implementation, we have to return the entire object
            resultCode = 200;
            bytesStart = -1;
            bytesEnd = -1;
        } else if (200 != resultCode) {
            response.setResultCode(resultCode);
            response.setResultDescription("Precondition Failed");
            return response;
        }

        // [D] Return the contents of the object inline
        // -> extract the meta data that corresponds the specific versioned item

        List<SMetaVO> itemMetaData = metaDao.getByTarget("SObjectItem", item.getId());
        if (null != itemMetaData) {
            int i = 0;
            S3MetaDataEntry[] metaEntries = new S3MetaDataEntry[itemMetaData.size()];
            ListIterator<SMetaVO> it = itemMetaData.listIterator();
            while (it.hasNext()) {
                SMetaVO oneTag = it.next();
                S3MetaDataEntry oneEntry = new S3MetaDataEntry();
                oneEntry.setName(oneTag.getName());
                oneEntry.setValue(oneTag.getValue());
                metaEntries[i++] = oneEntry;
            }
            response.setMetaEntries(metaEntries);
        }

        //  -> support a single byte range
        if (0 <= bytesStart && 0 <= bytesEnd) {
            response.setContentLength(bytesEnd - bytesStart);
            resultCode = 206;
        } else
            response.setContentLength(item.getStoredSize());

        if (request.isReturnData()) {
            response.setETag(item.getMd5());
            response.setLastModified(DateHelper.toCalendar(item.getLastModifiedTime()));
            response.setVersion(item.getVersion());
            if (request.isInlineData()) {
                OrderedPair<SHostVO, String> tupleSHostInfo = getBucketStorageHost(sbucket);
                S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(tupleSHostInfo.getFirst());

                if (0 <= bytesStart && 0 <= bytesEnd)
                    response.setData(bucketAdapter.loadObjectRange(tupleSHostInfo.getSecond(), request.getBucketName(), item.getStoredPath(), bytesStart, bytesEnd));
                else
                    response.setData(bucketAdapter.loadObject(tupleSHostInfo.getSecond(), request.getBucketName(), item.getStoredPath()));
            }
        }

        response.setResultCode(resultCode);
        response.setResultDescription("OK");
        return response;
    }

    /**
     * Handle object deletion requests, both versioning and non-versioning requirements.
     * Called from S3ObjectAction for deletion.
     */
    public S3Response handleRequest(S3DeleteObjectRequest request) {
        // Verify that the bucket and object exist
        S3Response response = new S3Response();

        String bucketName = request.getBucketName();
        SBucketVO sbucket = bucketDao.getByName(bucketName);
        if (sbucket == null) {
            response.setResultCode(404);
            response.setResultDescription("<Code>Bucket doesn't exists</Code><Message>Bucket " + bucketName + " does not exist</Message>");
            return response;
        }

        String nameKey = request.getKey();
        SObjectVO sobject = objectDao.getByNameKey(sbucket, nameKey);
        if (sobject == null) {
            response.setResultCode(404);
            response.setResultDescription("<Code>Not Found</Code><Message>No object with key " + nameKey + " exists in bucket " + bucketName + "</Message>");
            return response;
        }

        // Discover whether versioning is enabled.  If so versioning requires the setting of a deletion marker.
        String storedPath = null;
        SObjectItemVO item = null;
        int versioningStatus = sbucket.getVersioningStatus();
        if (SBucket.VERSIONING_ENABLED == versioningStatus) {
            String wantVersion = request.getVersion();
            S3PolicyContext context = new S3PolicyContext(PolicyActions.DeleteObjectVersion, bucketName);
            context.setKeyName(nameKey);
            context.setEvalParam(ConditionKeys.VersionId, wantVersion);
            verifyAccess(context, "SBucket", sbucket.getId(), SAcl.PERMISSION_WRITE);

            if (null == wantVersion) {
                // If versioning is on and no versionId is given then we just write a deletion marker
                sobject.setDeletionMark(UUID.randomUUID().toString());
                objectDao.update(sobject.getId(), sobject);
                response.setResultDescription("<DeleteMarker>true</DeleteMarker><DeleteMarkerVersionId>" + sobject.getDeletionMark() + "</DeleteMarkerVersionId>");
            } else {
                // Otherwise remove the deletion marker if this has been set
                String deletionMarker = sobject.getDeletionMark();
                if (null != deletionMarker && wantVersion.equalsIgnoreCase(deletionMarker)) {
                    sobject.setDeletionMark(null);
                    objectDao.update(sobject.getId(), sobject);
                    response.setResultDescription("<VersionId>" + wantVersion + "</VersionId>");
                    response.setResultDescription("<DeleteMarker>true</DeleteMarker><DeleteMarkerVersionId>" + sobject.getDeletionMark() + "</DeleteMarkerVersionId>");
                    response.setResultCode(204);
                    return response;
                }

                // If versioning is on and the versionId is given (non-null) then delete the object matching that version
                if (null == (item = sobject.getVersion(wantVersion))) {
                    response.setResultCode(404);
                    return response;
                } else {
                    // Providing versionId is non-null, then just delete the one item that matches the versionId from the database
                    storedPath = item.getStoredPath();
                    sobject.deleteItem(item.getId());
                    objectDao.update(sobject.getId(), sobject);
                    response.setResultDescription("<VersionId>" + wantVersion + "</VersionId>");
                }
            }
        } else {     // If versioning is off then we do delete the null object
            S3PolicyContext context = new S3PolicyContext(PolicyActions.DeleteObject, bucketName);
            context.setKeyName(nameKey);
            verifyAccess(context, "SBucket", sbucket.getId(), SAcl.PERMISSION_WRITE);

            if (null == (item = sobject.getLatestVersion(true))) {
                response.setResultCode(404);
                response.setResultDescription("<Code>AccessDenied</Code><Message>Access Denied</Message>");
                return response;
            } else {
                // If there is no item with a null version then we are done
                if (null == item.getVersion()) {
                    // Otherwiswe remove the entire object
                    // Cascade-deleting can delete related SObject/SObjectItem objects, but not SAcl and SMeta objects.
                    storedPath = item.getStoredPath();
                    deleteMetaData(item.getId());
                    deleteObjectAcls("SObjectItem", item.getId());
                    objectDao.remove(sobject.getId());
                }
            }
        }

        // Delete the file holding the object
        if (null != storedPath) {
            OrderedPair<SHostVO, String> host_storagelocation_pair = getBucketStorageHost(sbucket);
            S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(host_storagelocation_pair.getFirst());
            bucketAdapter.deleteObject(host_storagelocation_pair.getSecond(), bucketName, storedPath);
        }

        response.setResultCode(204);
        return response;
    }

    private void deleteMetaData(long itemId) {
        List<SMetaVO> itemMetaData = metaDao.getByTarget("SObjectItem", itemId);
        if (null != itemMetaData) {
            ListIterator<SMetaVO> it = itemMetaData.listIterator();
            while (it.hasNext()) {
                SMetaVO oneTag = it.next();
                metaDao.remove(oneTag.getId());
            }
        }
    }

    private void deleteObjectAcls(String target, long itemId) {
        List<SAclVO> itemAclData = aclDao.listGrants(target, itemId);
        if (null != itemAclData) {
            ListIterator<SAclVO> it = itemAclData.listIterator();
            while (it.hasNext()) {
                SAclVO oneTag = it.next();
                aclDao.remove(oneTag.getId());
            }
        }
    }

    private void deleteBucketAcls(long bucketId) {

        List<SAclVO> bucketAclData = aclDao.listGrants("SBucket", bucketId);
        if (null != bucketAclData) {
            ListIterator<SAclVO> it = bucketAclData.listIterator();
            while (it.hasNext()) {
                SAclVO oneTag = it.next();
                aclDao.remove(oneTag.getId());
            }
        }
    }

    private S3ListBucketPrefixEntry[] composeListBucketPrefixEntries(List<SObjectVO> l, String prefix, String delimiter, int maxKeys) {
        List<S3ListBucketPrefixEntry> entries = new ArrayList<S3ListBucketPrefixEntry>();
        int count = 0;

        for (SObjectVO sobject : l) {
            if (delimiter != null && !delimiter.isEmpty()) {
                String subName = StringHelper.substringInBetween(sobject.getNameKey(), prefix, delimiter);
                if (subName != null) {
                    S3ListBucketPrefixEntry entry = new S3ListBucketPrefixEntry();
                    if (prefix != null && prefix.length() > 0)
                        entry.setPrefix(prefix + delimiter + subName);
                    else
                        entry.setPrefix(subName);
                }
            }
            count++;
            if (count >= maxKeys)
                break;
        }

        if (entries.size() > 0)
            return entries.toArray(new S3ListBucketPrefixEntry[0]);
        return null;
    }

    /**
     * The 'versionIdMarker' parameter only makes sense if enableVersion is true.
     * versionIdMarker is the starting point to return information back.  So for example if an
     * object has versions 1,2,3,4,5 and the versionIdMarker is '3', then 3,4,5 will be returned
     * by this function.   If the versionIdMarker is null then all versions are returned.
     *
     * TODO - how does the versionIdMarker work when there is a deletion marker in the object?
     */
    private S3ListBucketObjectEntry[] composeListBucketContentEntries(List<SObjectVO> l, String prefix, String delimiter, int maxKeys, boolean enableVersion,
        String versionIdMarker) {
        List<S3ListBucketObjectEntry> entries = new ArrayList<S3ListBucketObjectEntry>();
        SObjectItemVO latest = null;
        boolean hitIdMarker = false;
        int count = 0;

        for (SObjectVO sobject : l) {
            if (delimiter != null && !delimiter.isEmpty()) {
                if (StringHelper.substringInBetween(sobject.getNameKey(), prefix, delimiter) != null)
                    continue;
            }

            if (enableVersion) {
                hitIdMarker = (null == versionIdMarker ? true : false);

                // This supports GET REST calls with /?versions
                String deletionMarker = sobject.getDeletionMark();
                if (null != deletionMarker) {
                    // TODO we should also save the timestamp when something is deleted
                    S3ListBucketObjectEntry entry = new S3ListBucketObjectEntry();
                    entry.setKey(sobject.getNameKey());
                    entry.setVersion(deletionMarker);
                    entry.setIsLatest(true);
                    entry.setIsDeletionMarker(true);
                    entry.setLastModified(Calendar.getInstance(TimeZone.getTimeZone("GMT")));
                    entry.setOwnerCanonicalId(sobject.getOwnerCanonicalId());
                    entry.setOwnerDisplayName("");
                    entries.add(entry);
                    latest = null;
                } else
                    latest = sobject.getLatestVersion(false);

                Iterator<SObjectItemVO> it = sobject.getItems().iterator();
                while (it.hasNext()) {
                    SObjectItemVO item = it.next();

                    if (!hitIdMarker) {
                        if (item.getVersion().equalsIgnoreCase(versionIdMarker)) {
                            hitIdMarker = true;
                            entries.add(toListEntry(sobject, item, latest));
                        }
                    } else
                        entries.add(toListEntry(sobject, item, latest));
                }
            } else {   // -> if there are multiple versions of an object then just return its last version
                Iterator<SObjectItemVO> it = sobject.getItems().iterator();
                SObjectItemVO lastestItem = null;
                int maxVersion = 0;
                int version = 0;
                while (it.hasNext()) {
                    SObjectItemVO item = it.next();
                    String versionStr = item.getVersion();

                    if (null != versionStr)
                        version = Integer.parseInt(item.getVersion());
                    else
                        lastestItem = item;

                    // -> if the bucket has versions turned on
                    if (version > maxVersion) {
                        maxVersion = version;
                        lastestItem = item;
                    }
                }
                if (lastestItem != null) {
                    entries.add(toListEntry(sobject, lastestItem, null));
                }
            }

            count++;
            if (count >= maxKeys)
                break;
        }

        if (entries.size() > 0)
            return entries.toArray(new S3ListBucketObjectEntry[0]);
        else
            return null;
    }

    private static S3ListBucketObjectEntry toListEntry(SObjectVO sobject, SObjectItemVO item, SObjectItemVO latest) {
        S3ListBucketObjectEntry entry = new S3ListBucketObjectEntry();
        entry.setKey(sobject.getNameKey());
        entry.setVersion(item.getVersion());
        entry.setETag("\"" + item.getMd5() + "\"");
        entry.setSize(item.getStoredSize());
        entry.setStorageClass("STANDARD");
        entry.setLastModified(DateHelper.toCalendar(item.getLastModifiedTime()));
        entry.setOwnerCanonicalId(sobject.getOwnerCanonicalId());
        entry.setOwnerDisplayName("");

        if (null != latest && item == latest)
            entry.setIsLatest(true);
        return entry;
    }

    private OrderedPair<SHostVO, String> getBucketStorageHost(SBucketVO bucket) {

        SHostVO shost = shostDao.findById(bucket.getShostID());
        if (shost.getHostType() == SHost.STORAGE_HOST_TYPE_LOCAL) {
            return new OrderedPair<SHostVO, String>(shost, shost.getExportRoot());
        }

        if (shost.getHostType() == SHost.STORAGE_HOST_TYPE_CASTOR) {
            return new OrderedPair<SHostVO, String>(shost, shost.getExportRoot());
        }

        MHostMountVO mount = mountDao.getHostMount(ServiceProvider.getInstance().getManagementHostId(), shost.getId());
        if (mount != null) {
            return new OrderedPair<SHostVO, String>(shost, mount.getMountPath());
        }
        //return null;
        // need to redirect request to other node
        throw new HostNotMountedException("Storage host "); // + shost.getHost() + " is not locally mounted");
    }

    /**
     * Locate the folder to hold upload parts at the same mount point as the upload's final bucket
     * location.   Create the upload folder dynamically.
     *
     * @param bucketName
     */
    private void createUploadFolder(String bucketName) {
        try {
            allocBucketStorageHost(bucketName, ServiceProvider.getInstance().getMultipartDir());
        } finally {

        }
    }

    /**
     * The overrideName is used to create a hidden storage bucket (folder) in the same location
     * as the given bucketName.   This can be used to create a folder for parts of a multipart
     * upload for the associated bucket.
     *
     * @param bucketName
     * @param overrideName
     * @return
     */
    private OrderedPair<SHostVO, String> allocBucketStorageHost(String bucketName, String overrideName) {
        //SHostDao shostDao = new SHostDao();

        MHostVO mhost = mhostDao.findById(ServiceProvider.getInstance().getManagementHostId());
        if (mhost == null)
            throw new OutOfServiceException("Temporarily out of service");

        if (mhost.getMounts().size() > 0) {
            Random random = new Random();
            MHostMountVO[] mounts = (MHostMountVO[])mhost.getMounts().toArray();
            MHostMountVO mount = mounts[random.nextInt(mounts.length)];
            S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(mount.getShost());
            bucketAdapter.createContainer(mount.getMountPath(), (null != overrideName ? overrideName : bucketName));
            return new OrderedPair<SHostVO, String>(mount.getShost(), mount.getMountPath());
        }

        // To make things simple, only allow one local mounted storage root TODO - Change in the future
        String localStorageRoot = ServiceProvider.getInstance().getStartupProperties().getProperty("storage.root");
        if (localStorageRoot != null) {
            SHostVO localSHost = shostDao.getLocalStorageHost(mhost.getId(), localStorageRoot);
            if (localSHost == null)
                throw new InternalErrorException("storage.root is configured but not initialized");

            S3BucketAdapter bucketAdapter = getStorageHostBucketAdapter(localSHost);
            bucketAdapter.createContainer(localSHost.getExportRoot(), (null != overrideName ? overrideName : bucketName));
            return new OrderedPair<SHostVO, String>(localSHost, localStorageRoot);
        }

        throw new OutOfStorageException("No storage host is available");
    }

    public S3BucketAdapter getStorageHostBucketAdapter(SHostVO shost) {
        S3BucketAdapter adapter = bucketAdapters.get(shost.getHostType());
        if (adapter == null)
            throw new InternalErrorException("Bucket adapter is not installed for host type: " + shost.getHostType());

        return adapter;
    }

    /**
     * If acl is set then the cannedAccessPolicy parameter should be null and is ignored.
     * The cannedAccessPolicy parameter is for REST Put requests only where a simple set of ACLs can be
     * created with a single header value.  Note that we do not currently support "anonymous" un-authenticated
     * access in our implementation.
     *
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    public OrderedPair<SObjectVO, SObjectItemVO> allocObjectItem(SBucketVO bucket, String nameKey, S3MetaDataEntry[] meta, S3AccessControlList acl,
        String cannedAccessPolicy) {
        SObjectItemVO item = null;
        int versionSeq = 1;
        int versioningStatus = bucket.getVersioningStatus();

        //Session session = PersistContext.getSession();

        // [A] To write into a bucket the user must have write permission to that bucket
        S3PolicyContext context = new S3PolicyContext(PolicyActions.PutObject, bucket.getName());
        context.setKeyName(nameKey);
        context.setEvalParam(ConditionKeys.Acl, cannedAccessPolicy);

        verifyAccess(context, "SBucket", bucket.getId(), SAcl.PERMISSION_WRITE);  // TODO - check this validates plain POSTs
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
        txn.start();

        // [B] If versioning is off them we over write a null object item
        SObjectVO object = objectDao.getByNameKey(bucket, nameKey);
        if (object != null) {
            // -> if versioning is on create new object items
            if (SBucket.VERSIONING_ENABLED == versioningStatus) {

                versionSeq = object.getNextSequence();
                object.setNextSequence(versionSeq + 1);
                objectDao.update(object.getId(), object);

                item = new SObjectItemVO();
                item.setTheObject(object);
                object.getItems().add(item);
                item.setsObjectID(object.getId());
                item.setVersion(String.valueOf(versionSeq));
                Date ts = DateHelper.currentGMTTime();
                item.setCreateTime(ts);
                item.setLastAccessTime(ts);
                item.setLastModifiedTime(ts);
                item = itemDao.persist(item);
                txn.commit();
                //session.save(item);
            } else {    // -> find an object item with a null version, can be null
                     //    if bucket started out with versioning enabled and was then suspended
                item = itemDao.getByObjectIdNullVersion(object.getId());
                if (item == null) {
                    item = new SObjectItemVO();
                    item.setTheObject(object);
                    item.setsObjectID(object.getId());
                    object.getItems().add(item);
                    Date ts = DateHelper.currentGMTTime();
                    item.setCreateTime(ts);
                    item.setLastAccessTime(ts);
                    item.setLastModifiedTime(ts);
                    item = itemDao.persist(item);
                    txn.commit();
                }
            }
        } else {
            TransactionLegacy txn1 = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
            txn1.start();
            // -> there is no object nor an object item
            object = new SObjectVO();
            object.setBucket(bucket);
            object.setNameKey(nameKey);
            object.setNextSequence(2);
            object.setBucketID(bucket.getId());
            object.setCreateTime(DateHelper.currentGMTTime());
            object.setOwnerCanonicalId(UserContext.current().getCanonicalUserId());
            object = objectDao.persist(object);
            item = new SObjectItemVO();
            item.setTheObject(object);
            item.setsObjectID(object.getId());
            object.getItems().add(item);
            if (SBucket.VERSIONING_ENABLED == versioningStatus)
                item.setVersion(String.valueOf(versionSeq));
            Date ts = DateHelper.currentGMTTime();
            item.setCreateTime(ts);
            item.setLastAccessTime(ts);
            item.setLastModifiedTime(ts);
            item = itemDao.persist(item);
            txn.commit();
            txn.close();

        }

        // [C] We will use the item DB id as the file name, MD5/contentLength will be stored later
        String suffix = null;
        int dotPos = nameKey.lastIndexOf('.');
        if (dotPos >= 0)
            suffix = nameKey.substring(dotPos);
        if (suffix != null)
            item.setStoredPath(String.valueOf(item.getId()) + suffix);
        else
            item.setStoredPath(String.valueOf(item.getId()));

        metaDao.save("SObjectItem", item.getId(), meta);

        // [D] Are we setting an ACL along with the object
        //  -> the ACL is ALWAYS set on a particular instance of the object (i.e., a version)
        if (null != cannedAccessPolicy) {
            setCannedAccessControls(cannedAccessPolicy, "SObjectItem", item.getId(), bucket);
        } else if (null == acl || 0 == acl.size()) {
            // -> this is termed the "private" or default ACL, "Owner gets FULL_CONTROL"
            setSingleAcl("SObjectItem", item.getId(), SAcl.PERMISSION_FULL);
        } else if (null != acl) {
            aclDao.save("SObjectItem", item.getId(), acl);
        }

        itemDao.update(item.getId(), item);
        txn.close();
        return new OrderedPair<SObjectVO, SObjectItemVO>(object, item);
    }

    /**
     * Access controls that are specified via the "x-amz-acl:" headers in REST requests.
     * Note that canned policies can be set when the object's contents are set
     */
    public void setCannedAccessControls(String cannedAccessPolicy, String target, long objectId, SBucketVO bucket) {
        // Find the permission and symbol for the principal corresponding to the requested cannedAccessPolicy
        Triple<Integer, Integer, String> permission_permission_symbol_triple = SAclVO.getCannedAccessControls(cannedAccessPolicy, target, bucket.getOwnerCanonicalId());
        if (null == permission_permission_symbol_triple.getThird())
            setSingleAcl(target, objectId, permission_permission_symbol_triple.getFirst());
        else {
            setDefaultAcls(target, objectId, permission_permission_symbol_triple.getFirst(),    // permission according to ownership of object
                permission_permission_symbol_triple.getSecond(),   // permission according to ownership of bucket
                permission_permission_symbol_triple.getThird());  // "symbol" to indicate principal or otherwise name of owner

        }
    }

    private void setSingleAcl(String target, long targetId, int permission) {
        S3AccessControlList defaultAcl = new S3AccessControlList();

        // -> if an annoymous request, then do not rewrite the ACL
        String userId = UserContext.current().getCanonicalUserId();
        if (0 < userId.length()) {
            S3Grant defaultGrant = new S3Grant();
            defaultGrant.setGrantee(SAcl.GRANTEE_USER);
            defaultGrant.setCanonicalUserID(userId);
            defaultGrant.setPermission(permission);
            defaultAcl.addGrant(defaultGrant);
            aclDao.save(target, targetId, defaultAcl);
        }
    }

    /**
     * The Cloud Stack API Access key is used for for the Canonical User Id everywhere (buckets and objects).
     *
     * @param owner - this can be the Cloud Access Key for a bucket owner or one of the
     *                following special symbols:
     *                (a) '*' - any principal authenticated user (i.e., any user with a registered Cloud Access Key)
     *                (b) 'A' - any anonymous principal (i.e., S3 request without an Authorization header)
     */
    private void setDefaultAcls(String target, long objectId, int permission1, int permission2, String owner) {
        S3AccessControlList defaultAcl = new S3AccessControlList();

        // -> object owner
        S3Grant defaultGrant = new S3Grant();
        defaultGrant.setGrantee(SAcl.GRANTEE_USER);
        defaultGrant.setCanonicalUserID(UserContext.current().getCanonicalUserId());
        defaultGrant.setPermission(permission1);
        defaultAcl.addGrant(defaultGrant);

        // -> bucket owner
        defaultGrant = new S3Grant();
        defaultGrant.setGrantee(SAcl.GRANTEE_USER);
        defaultGrant.setCanonicalUserID(owner);
        defaultGrant.setPermission(permission2);
        defaultAcl.addGrant(defaultGrant);
        aclDao.save(target, objectId, defaultAcl);
    }

    public static PolicyAccess verifyPolicy(S3PolicyContext context) {
        S3BucketPolicy policy = null;

        // Ordinarily a REST request will pass in an S3PolicyContext for a given bucket by this stage.  The HttpServletRequest object
        // should be held in the UserContext ready for extraction of the S3BucketPolicy.
        // If there is an error in obtaining the request object or in loading the policy then log the failure and return a S3PolicyContext
        // which indicates DEFAULT_DENY.  Where there is no failure, the policy returned should be specific to the Canonical User ID of the requester.

        try {
            // -> in SOAP the HttpServletRequest object is hidden and not passed around
            if (null != context) {
                context.setHttp(UserContext.current().getHttp());
                policy = loadPolicy(context);
            }

            if (null != policy)
                return policy.eval(context, UserContext.current().getCanonicalUserId());
            else
                return PolicyAccess.DEFAULT_DENY;
        } catch (Exception e) {
            logger.error("verifyAccess - loadPolicy failed, bucket: " + context.getBucketName() + " policy ignored", e);
            return PolicyAccess.DEFAULT_DENY;
        }
    }

    /**
     * To determine access to a bucket or an object in a bucket evaluate first a define
     * bucket policy and then any defined ACLs.
     *
     * @param context - all data needed for bucket policies
     * @param target - used for ACL evaluation, object identifier
     * @param targetId - used for ACL evaluation
     * @param requestedPermission - ACL type access requested
     *
     * @throws ParseException, SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException
     */
    public static void verifyAccess(S3PolicyContext context, String target, long targetId, int requestedPermission) {
        switch (verifyPolicy(context)) {
            case ALLOW:   // overrides ACLs (?)
                return;

            case DENY:
                throw new PermissionDeniedException("Access Denied - bucket policy DENY result");

            case DEFAULT_DENY:
            default:
                accessAllowed(target, targetId, requestedPermission);
                break;
        }
    }

    /**
     * This method verifies that the accessing client has the requested
     * permission on the object/bucket/Acl represented by the tuple: <target, targetId>
     *
     * For cases where an ACL is meant for any authenticated user we place a "*" for the
     * Canonical User Id.  N.B. - "*" is not a legal Cloud (Bridge) Access key.
     *
     * For cases where an ACL is meant for any anonymous user (or 'AllUsers') we place a "A" for the
     * Canonical User Id.  N.B. - "A" is not a legal Cloud (Bridge) Access key.
     */
    public static void accessAllowed(String target, long targetId, int requestedPermission) {
        if (SAcl.PERMISSION_PASS == requestedPermission)
            return;

        // If an annoymous request, then canonicalUserId is an empty string
        String userId = UserContext.current().getCanonicalUserId();
        if (0 == userId.length()) {
            // Is an anonymous principal ACL set for this <target, targetId>?
            if (hasPermission(s_saclDao.listGrants(target, targetId, "A"), requestedPermission))
                return;
        } else {
            if (hasPermission(s_saclDao.listGrants(target, targetId, userId), requestedPermission))
                return;
            // Or alternatively is there is any principal authenticated ACL set for this <target, targetId>?
            if (hasPermission(s_saclDao.listGrants(target, targetId, "*"), requestedPermission))
                return;
        }
        // No privileges implies that no access is allowed    in the case of an anonymous user
        throw new PermissionDeniedException("Access Denied - ACLs do not give user the required permission");
    }

    /**
     * This method assumes that the bucket has been tested to make sure it exists before
     * it is called.
     *
     * @param context
     * @return S3BucketPolicy
     * @throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException, ParseException
     */
    public static S3BucketPolicy loadPolicy(S3PolicyContext context) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException,
        ParseException {
        OrderedPair<S3BucketPolicy, Integer> result = ServiceProvider.getInstance().getBucketPolicy(context.getBucketName());
        S3BucketPolicy policy = result.getFirst();
        if (null == policy) {
            // -> do we have to load it from the database (any other value means there is no policy)?
            if (-1 == result.getSecond().intValue()) {
                BucketPolicyVO policyvo = s_bPolicy.getByName(context.getBucketName());
                String policyInJson = null;
                if (null != policyvo)
                    policyInJson = policyvo.getPolicy();

                // -> place in cache that no policy exists in the database
                if (null == policyInJson) {
                    ServiceProvider.getInstance().setBucketPolicy(context.getBucketName(), null);
                    return null;
                }

                PolicyParser parser = new PolicyParser();
                policy = parser.parse(policyInJson, context.getBucketName());
                if (null != policy)
                    ServiceProvider.getInstance().setBucketPolicy(context.getBucketName(), policy);
            }
        }
        return policy;
    }

    public static void verifyBucketName(String bucketName, boolean useDNSGuidelines) throws InvalidBucketName {
        // [A] To comply with Amazon S3 basic requirements, bucket names must meet the following conditions
        // -> must be between 3 and 255 characters long
        int size = bucketName.length();
        if (3 > size || size > 255)
            throw new InvalidBucketName(bucketName + " is not between 3 and 255 characters long");

        // -> must start with a number or letter
        if (!Character.isLetterOrDigit(bucketName.charAt(0)))
            throw new InvalidBucketName(bucketName + " does not start with a number or letter");

        // -> can contain lowercase letters, numbers, periods (.), underscores (_), and dashes (-)
        // -> the bucket name can also contain uppercase letters but it is not recommended
        for (int i = 0; i < bucketName.length(); i++) {
            char next = bucketName.charAt(i);
            if (Character.isLetter(next))
                continue;
            else if (Character.isDigit(next))
                continue;
            else if ('.' == next)
                continue;
            else if ('_' == next)
                continue;
            else if ('-' == next)
                continue;
            else
                throw new InvalidBucketName(bucketName + " contains the invalid character: " + next);
        }

        // -> must not be formatted as an IP address (e.g., 192.168.5.4)
        String[] parts = bucketName.split("\\.");
        if (4 == parts.length) {
            try {
                int first = Integer.parseInt(parts[0]);
                int second = Integer.parseInt(parts[1]);
                int third = Integer.parseInt(parts[2]);
                int fourth = Integer.parseInt(parts[3]);
                throw new InvalidBucketName(bucketName + " is formatted as an IP address");
            } catch (NumberFormatException e) {
                throw new InvalidBucketName(bucketName);
            }
        }

        // [B] To conform with DNS requirements, Amazon recommends following these additional guidelines when creating buckets
        // -> bucket names should be between 3 and 63 characters long
        if (useDNSGuidelines) {
            // -> bucket names should be between 3 and 63 characters long
            if (3 > size || size > 63)
                throw new InvalidBucketName("DNS requiremens, bucket name: " + bucketName + " is not between 3 and 63 characters long");

            // -> bucket names should not contain underscores (_)
            int pos = bucketName.indexOf('_');
            if (-1 != pos)
                throw new InvalidBucketName("DNS requiremens, bucket name: " + bucketName + " should not contain underscores");

            // -> bucket names should not end with a dash
            if (bucketName.endsWith("-"))
                throw new InvalidBucketName("DNS requiremens, bucket name: " + bucketName + " should not end with a dash");

            // -> bucket names cannot contain two, adjacent periods
            pos = bucketName.indexOf("..");
            if (-1 != pos)
                throw new InvalidBucketName("DNS requiremens, bucket name: " + bucketName + " should not contain \"..\"");

            // -> bucket names cannot contain dashes next to periods (e.g., "my-.bucket.com" and "my.-bucket" are invalid)
            if (-1 != bucketName.indexOf("-.") || -1 != bucketName.indexOf(".-"))
                throw new InvalidBucketName("DNS requiremens, bucket name: " + bucketName + " should not contain \".-\" or \"-.\"");
        }
    }

    private static boolean hasPermission(List<SAclVO> privileges, int requestedPermission) {
        ListIterator<SAclVO> it = privileges.listIterator();
        while (it.hasNext()) {
            // True providing the requested permission is contained in one or the granted rights for this user.  False otherwise.
            SAclVO rights = it.next();
            int permission = rights.getPermission();
            if (requestedPermission == (permission & requestedPermission))
                return true;
        }
        return false;
    }

    /**
     * ifRange is true and ifUnmodifiedSince or IfMatch fails then we return the entire object (indicated by
     * returning a -1 as the function result.
     *
     * @param ifCond - conditional get defined by these tests
     * @param lastModified - value used on ifModifiedSince or ifUnmodifiedSince
     * @param ETag - value used on ifMatch and ifNoneMatch
     * @param ifRange - using an if-Range HTTP functionality
     * @return -1 means return the entire object with an HTTP 200 (not a subrange)
     */
    private int conditionPassed(S3ConditionalHeaders ifCond, Date lastModified, String ETag, boolean ifRange) {
        if (null == ifCond)
            return 200;

        if (0 > ifCond.ifModifiedSince(lastModified))
            return 304;

        if (0 > ifCond.ifUnmodifiedSince(lastModified))
            return (ifRange ? -1 : 412);

        if (0 > ifCond.ifMatchEtag(ETag))
            return (ifRange ? -1 : 412);

        if (0 > ifCond.ifNoneMatchEtag(ETag))
            return 412;

        return 200;
    }
}
