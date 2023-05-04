/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.driver;

import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.Bucket;
import com.cloud.utils.exception.CloudRuntimeException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.BaseObjectStoreDriverImpl;
import org.apache.cloudstack.storage.object.BucketObject;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MinIOObjectStoreDriverImpl extends BaseObjectStoreDriverImpl {
    private static final Logger s_logger = Logger.getLogger(MinIOObjectStoreDriverImpl.class);

    @Inject
    ObjectStoreDao _storeDao;

    @Inject
    ObjectStoreDetailsDao _storeDetailsDao;

    private static final String ACCESS_KEY = "accesskey";
    private static final String SECRET_KEY = "secretkey";

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    @Override
    public Bucket createBucket(String bucketName, long storeId) {
        //ToDo Client pool mgmt
        MinioClient minioClient = getMinIOClient(storeId);
        try {
            if(minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                throw new CloudRuntimeException("Bucket already exists with name "+ bucketName);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        Bucket bucket = new BucketObject();
        bucket.setName(bucketName);
        return bucket;
    }

    @Override
    public List<Bucket> listBuckets(long storeId) {
        MinioClient minioClient = getMinIOClient(storeId);
        List<Bucket> bucketsList = new ArrayList<>();
        try {
            List<io.minio.messages.Bucket> minIOBuckets =  minioClient.listBuckets();
            for(io.minio.messages.Bucket minIObucket : minIOBuckets) {
                Bucket bucket = new BucketObject();
                bucket.setName(minIObucket.name());
                bucketsList.add(bucket);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return bucketsList;
    }

    @Override
    public void deleteBucket(String bucketName, long storeId) {
        MinioClient minioClient = getMinIOClient(storeId);
        try {
            if(minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                throw new CloudRuntimeException("Bucket with name %s doesn't exist "+ bucketName);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        //ToDo: check bucket empty
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public AccessControlList getBucketAcl(String bucketName, long storeId) {
        return null;
    }

    @Override
    public void setBucketAcl(String bucketName, AccessControlList acl, long storeId) {

    }

    @Override
    public void setBucketPolicy(String bucketName, String policyText, long storeId) {

    }

    @Override
    public BucketPolicy getBucketPolicy(String bucketName, long storeId) {
        return null;
    }

    @Override
    public void deleteBucketPolicy(String bucketName, long storeId) {

    }

    private MinioClient getMinIOClient(long storeId) {
        ObjectStoreVO store = _storeDao.findById(storeId);
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String url = store.getUrl();
        String accessKey = storeDetails.get(ACCESS_KEY);
        String secretKey = storeDetails.get(SECRET_KEY);
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint(url)
                        .credentials(accessKey,secretKey)
                        .build();
        return minioClient;
    }
}
