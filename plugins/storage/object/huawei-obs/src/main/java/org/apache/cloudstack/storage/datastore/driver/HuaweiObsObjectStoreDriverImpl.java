package org.apache.cloudstack.storage.datastore.driver;

import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.Grantee;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.Permission;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.iam.v3.IamClient;
import com.huaweicloud.sdk.iam.v3.model.CreateUserOption;
import com.huaweicloud.sdk.iam.v3.model.CreateUserRequest;
import com.huaweicloud.sdk.iam.v3.model.CreateUserRequestBody;
import com.huaweicloud.sdk.iam.v3.model.ShowUserRequest;
import com.huaweicloud.sdk.iam.v3.model.ShowUserResult;
import com.huaweicloud.sdk.iam.v3.model.UpdateUserOption;
import com.huaweicloud.sdk.iam.v3.model.UpdateUserRequest;
import com.huaweicloud.sdk.iam.v3.model.UpdateUserRequestBody;
import com.obs.services.ObsClient;
import com.obs.services.model.BucketEncryption;
import com.obs.services.model.BucketPolicyResponse;
import com.obs.services.model.BucketQuota;
import com.obs.services.model.BucketStorageInfo;
import com.obs.services.model.BucketVersioningConfiguration;
import com.obs.services.model.CreateBucketRequest;
import com.obs.services.model.GrantAndPermission;
import com.obs.services.model.GranteeInterface;
import com.obs.services.model.ListBucketsRequest;
import com.obs.services.model.ListBucketsResult;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.SSEAlgorithmEnum;
import com.obs.services.model.VersioningStatusEnum;
import java.net.URI;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.object.BaseObjectStoreDriverImpl;
import org.apache.cloudstack.storage.object.Bucket;
import org.apache.cloudstack.storage.object.BucketObject;
import org.apache.commons.codec.binary.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;

public class HuaweiObsObjectStoreDriverImpl extends BaseObjectStoreDriverImpl {

    @Inject
    AccountDao _accountDao;
    @Inject
    AccountDetailsDao _accountDetailsDao;
    @Inject
    ObjectStoreDao _storeDao;
    @Inject
    BucketDao _bucketDao;
    @Inject
    ObjectStoreDetailsDao _storeDetailsDao;

    private static final String ACCESS_KEY = "accesskey";
    private static final String SECRET_KEY = "secretkey";
    private static final String OBS_ACCESS_KEY = "huawei-obs-accesskey";
    private static final String OBS_SECRET_KEY = "huawei-obs-secretkey";

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    @Override
    public Bucket createBucket(Bucket bucket, boolean objectLock) {
        long accountId = bucket.getAccountId();
        long storeId = bucket.getObjectStoreId();
        Account account = _accountDao.findById(accountId);

        if ((_accountDetailsDao.findDetail(accountId, OBS_ACCESS_KEY) == null) || (_accountDetailsDao.findDetail(accountId, OBS_SECRET_KEY) == null)) {
            throw new CloudRuntimeException("Bucket access credentials unavailable for account: " + account.getAccountName());
        }

        try (ObsClient obsClient = getObsClient(storeId)) {
            String bucketName = bucket.getName();

            if (obsClient.headBucket(bucketName)) {
                throw new CloudRuntimeException("A bucket with the name " + bucketName + " already exists");
            }

            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            createBucketRequest.setAcl(com.obs.services.model.AccessControlList.REST_CANNED_PUBLIC_READ_WRITE);
            obsClient.createBucket(createBucketRequest);

            BucketVO bucketVO = _bucketDao.findById(bucket.getId());
            String accountAccessKey = _accountDetailsDao.findDetail(accountId, OBS_ACCESS_KEY).getValue();
            String accountSecretKey = _accountDetailsDao.findDetail(accountId, OBS_SECRET_KEY).getValue();
            String endpoint = _storeDao.findById(storeId).getUrl();
            String scheme = new URI(endpoint).getScheme() + "://";
            String everythingelse = endpoint.substring(scheme.length());
            bucketVO.setAccessKey(accountAccessKey);
            bucketVO.setSecretKey(accountSecretKey);
            bucketVO.setBucketURL(scheme + bucketName + "." + everythingelse);
            _bucketDao.update(bucket.getId(), bucketVO);
            return bucket;
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
    }

    @Override
    public List<Bucket> listBuckets(long storeId) {
        List<Bucket> bucketsList = new ArrayList<>();
        try (ObsClient obsClient = getObsClient(storeId)) {
            ListBucketsRequest request = new ListBucketsRequest();
            request.setQueryLocation(true);
            ListBucketsResult buckets = obsClient.listBucketsV2(request);
            for (ObsBucket obsBucket : buckets.getBuckets()) {
                Bucket bucket = new BucketObject();
                bucket.setName(obsBucket.getBucketName());
                bucketsList.add(bucket);
            }
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
        return bucketsList;
    }

    @Override
    public boolean deleteBucket(String bucketName, long storeId) {
        try (ObsClient obsClient = getObsClient(storeId)) {

            if (!obsClient.headBucket(bucketName)) {
                throw new CloudRuntimeException("Bucket does not exist: " + bucketName);
            }

            ObjectListing objectListing = obsClient.listObjects(bucketName);
            if (objectListing == null || objectListing.getObjects().isEmpty()) {
                obsClient.deleteBucket(bucketName);
            } else {
                throw new CloudRuntimeException("Bucket " + bucketName + " cannot be deleted because it is not empty");
            }
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
        return true;
    }

    @Override
    public AccessControlList getBucketAcl(String bucketName, long storeId) {
        AccessControlList accessControlList = new AccessControlList();
        try (ObsClient obsClient = getObsClient(storeId)) {
            com.obs.services.model.AccessControlList obsAccessControlList = obsClient.getBucketAcl(bucketName);
            com.obs.services.model.Owner obsOwner = obsAccessControlList.getOwner();
            Owner owner = new Owner(obsOwner.getId(), obsOwner.getDisplayName());
            accessControlList.setOwner(owner);
            for (GrantAndPermission grantAndPermission : obsAccessControlList.getGrantAndPermissions()) {
                com.obs.services.model.Permission obsPermission = grantAndPermission.getPermission();
                Permission permission = castPermission(obsPermission);
                GranteeInterface granteeInterface = grantAndPermission.getGrantee();
                if (granteeInterface instanceof com.obs.services.model.CanonicalGrantee) {
                    Grantee grantee = new CanonicalGrantee(granteeInterface.getIdentifier());
                    accessControlList.grantPermission(grantee, permission);
                } else if (granteeInterface instanceof com.obs.services.model.GroupGrantee) {
                    com.obs.services.model.GroupGrantee obsGroupGrantee = (com.obs.services.model.GroupGrantee) granteeInterface;
                    if (obsGroupGrantee.getGroupGranteeType() == com.obs.services.model.GroupGranteeEnum.ALL_USERS) {
                        accessControlList.grantPermission(GroupGrantee.AllUsers, permission);
                    } else if (obsGroupGrantee.getGroupGranteeType() == com.obs.services.model.GroupGranteeEnum.LOG_DELIVERY) {
                        accessControlList.grantPermission(GroupGrantee.LogDelivery, permission);
                    } else if (obsGroupGrantee.getGroupGranteeType() == com.obs.services.model.GroupGranteeEnum.AUTHENTICATED_USERS) {
                        accessControlList.grantPermission(GroupGrantee.AuthenticatedUsers, permission);
                    }
                }
            }
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
        return accessControlList;
    }

    private Permission castPermission(com.obs.services.model.Permission obsPermission) {
        if (com.obs.services.model.Permission.PERMISSION_FULL_CONTROL == obsPermission) {
            return Permission.FullControl;
        } else if (com.obs.services.model.Permission.PERMISSION_READ == obsPermission) {
            return Permission.Read;
        } else if (com.obs.services.model.Permission.PERMISSION_READ_ACP == obsPermission) {
            return Permission.ReadAcp;
        } else if (com.obs.services.model.Permission.PERMISSION_WRITE == obsPermission) {
            return Permission.Write;
        } else if (com.obs.services.model.Permission.PERMISSION_WRITE_ACP == obsPermission) {
            return Permission.WriteAcp;
        }
        return Permission.FullControl;
    }

    private com.obs.services.model.Permission castPermission(Permission permission) {
        if (Permission.FullControl == permission) {
            return com.obs.services.model.Permission.PERMISSION_FULL_CONTROL;
        } else if (Permission.Read == permission) {
            return com.obs.services.model.Permission.PERMISSION_READ;
        } else if (Permission.ReadAcp == permission) {
            return com.obs.services.model.Permission.PERMISSION_READ_ACP;
        } else if (Permission.Write == permission) {
            return com.obs.services.model.Permission.PERMISSION_WRITE;
        } else if (Permission.WriteAcp == permission) {
            return com.obs.services.model.Permission.PERMISSION_WRITE_ACP;
        }
        return com.obs.services.model.Permission.PERMISSION_FULL_CONTROL;
    }

    @Override
    public void setBucketAcl(String bucketName, AccessControlList accessControlList, long storeId) {
        com.obs.services.model.AccessControlList obsAccessControlList = new com.obs.services.model.AccessControlList();
        Owner owner = accessControlList.getOwner();
        com.obs.services.model.Owner obsOwner = new com.obs.services.model.Owner();
        obsOwner.setId(owner.getId());
        obsOwner.setDisplayName(owner.getDisplayName());
        obsAccessControlList.setOwner(obsOwner);
        for (Grant grant : accessControlList.getGrantsAsList()) {
            if (grant.getGrantee() instanceof CanonicalGrantee) {
                com.obs.services.model.CanonicalGrantee canonicalGrantee = new com.obs.services.model.CanonicalGrantee(grant.getGrantee().getIdentifier());
                obsAccessControlList.grantPermission(canonicalGrantee, castPermission(grant.getPermission()));
            } else if (grant.getGrantee() instanceof GroupGrantee) {
                GroupGrantee groupGrantee = (GroupGrantee) grant.getGrantee();
                if (GroupGrantee.AllUsers == groupGrantee) {
                    obsAccessControlList.grantPermission(com.obs.services.model.GroupGrantee.ALL_USERS, castPermission(grant.getPermission()));
                } else if (GroupGrantee.LogDelivery == groupGrantee) {
                    obsAccessControlList.grantPermission(com.obs.services.model.GroupGrantee.LOG_DELIVERY, castPermission(grant.getPermission()));
                } else if (GroupGrantee.AuthenticatedUsers == groupGrantee) {
                    obsAccessControlList.grantPermission(com.obs.services.model.GroupGrantee.AUTHENTICATED_USERS, castPermission(grant.getPermission()));
                }

            }
        }
        try (ObsClient obsClient = getObsClient(storeId)) {
            obsClient.setBucketAcl(bucketName, obsAccessControlList);
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
    }

    @Override
    public void setBucketPolicy(String bucketName, String policy, long storeId) {
        String privatePolicy = "{\"Version\":\"2012-10-17\",\"Statement\":[]}";

        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("    \"Statement\": [\n");
        builder.append("        {\n");
        builder.append("            \"Action\": [\n");
        builder.append("                \"s3:GetBucketLocation\",\n");
        builder.append("                \"s3:ListBucket\"\n");
        builder.append("            ],\n");
        builder.append("            \"Effect\": \"Allow\",\n");
        builder.append("            \"Principal\": \"*\",\n");
        builder.append("            \"Resource\": \"arn:aws:s3:::").append(bucketName).append("\"\n");
        builder.append("        },\n");
        builder.append("        {\n");
        builder.append("            \"Action\": \"s3:GetObject\",\n");
        builder.append("            \"Effect\": \"Allow\",\n");
        builder.append("            \"Principal\": \"*\",\n");
        builder.append("            \"Resource\": \"arn:aws:s3:::").append(bucketName).append("/*\"\n");
        builder.append("        }\n");
        builder.append("    ],\n");
        builder.append("    \"Version\": \"2012-10-17\"\n");
        builder.append("}\n");
        String publicPolicy = builder.toString();

        //ToDo Support custom policy
        String policyConfig = (policy.equalsIgnoreCase("public")) ? publicPolicy : privatePolicy;

        try (ObsClient obsClient = getObsClient(storeId)) {
            obsClient.setBucketPolicy(bucketName, policyConfig);
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
    }

    @Override
    public BucketPolicy getBucketPolicy(String bucketName, long storeId) {
        try (ObsClient obsClient = getObsClient(storeId)) {
            BucketPolicyResponse bucketPolicyResponse = obsClient.getBucketPolicyV2(bucketName);
            BucketPolicy bucketPolicy = new BucketPolicy();
            bucketPolicy.setPolicyText(bucketPolicyResponse.getPolicy());
            return bucketPolicy;
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
    }

    @Override
    public void deleteBucketPolicy(String bucketName, long storeId) {
        try (ObsClient obsClient = getObsClient(storeId)) {
            obsClient.deleteBucketPolicy(bucketName);
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
    }

    @Override
    public boolean createUser(long accountId, long storeId) {
        Account account = _accountDao.findById(accountId);
        String accessKey = account.getAccountName();
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String endpoint = _storeDao.findById(storeId).getUrl();
        String clientAccessKey = storeDetails.get(ACCESS_KEY);
        String clientSecretKey = storeDetails.get(SECRET_KEY);

        try {
            ICredential credentials = new BasicCredentials().withAk(clientAccessKey).withSk(clientSecretKey).withIamEndpoint(endpoint);
            IamClient iamClient = IamClient.newBuilder().withCredential(credentials).build();
            ShowUserRequest showUserRequest = new ShowUserRequest().withUserId(accessKey);
            ShowUserResult showUserResult = iamClient.showUser(showUserRequest).getUser();
            if (showUserResult == null || showUserResult.getPwdStatus()) {
                KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
                SecretKey key = generator.generateKey();
                String secretKey = Base64.encodeBase64URLSafeString(key.getEncoded());
                CreateUserOption createUserOption = new CreateUserOption().withName(accessKey).withPassword(secretKey).withEnabled(Boolean.TRUE);
                CreateUserRequestBody createUserRequestBody = new CreateUserRequestBody().withUser(createUserOption);
                CreateUserRequest createUserRequest = new CreateUserRequest().withBody(createUserRequestBody);
                iamClient.createUser(createUserRequest);

                // Store user credentials
                Map<String, String> details = new HashMap<>();
                details.put(OBS_ACCESS_KEY, accessKey);
                details.put(OBS_SECRET_KEY, secretKey);
                _accountDetailsDao.persist(accountId, details);
            } else if (!showUserResult.getEnabled()) {
                UpdateUserOption updateUserOption = new UpdateUserOption().withName(accessKey).withEnabled(Boolean.TRUE);
                UpdateUserRequestBody updateUserRequestBody = new UpdateUserRequestBody().withUser(updateUserOption);
                UpdateUserRequest updateUserRequest = new UpdateUserRequest().withBody(updateUserRequestBody);
                iamClient.updateUser(updateUserRequest);
            }
            return true;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public boolean setBucketEncryption(String bucketName, long storeId) {
        try (ObsClient obsClient = getObsClient(storeId)) {
            BucketEncryption bucketEncryption = new BucketEncryption(SSEAlgorithmEnum.KMS);
            obsClient.setBucketEncryption(bucketName, bucketEncryption);
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
        return true;
    }

    @Override
    public boolean deleteBucketEncryption(String bucketName, long storeId) {
        try (ObsClient obsClient = getObsClient(storeId)) {
            obsClient.deleteBucketEncryption(bucketName);
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
        return true;
    }

    @Override
    public boolean setBucketVersioning(String bucketName, long storeId) {
        try (ObsClient obsClient = getObsClient(storeId)) {
            BucketVersioningConfiguration bucketVersioningConfiguration = new BucketVersioningConfiguration(VersioningStatusEnum.ENABLED);
            obsClient.setBucketVersioning(bucketName, bucketVersioningConfiguration);
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
        return true;
    }

    @Override
    public boolean deleteBucketVersioning(String bucketName, long storeId) {
        try (ObsClient obsClient = getObsClient(storeId)) {
            BucketVersioningConfiguration bucketVersioningConfiguration = new BucketVersioningConfiguration(VersioningStatusEnum.SUSPENDED);
            obsClient.setBucketVersioning(bucketName, bucketVersioningConfiguration);
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
        return true;
    }

    @Override
    public void setBucketQuota(String bucketName, long storeId, long size) {
        try (ObsClient obsClient = getObsClient(storeId)) {
            BucketQuota quota = new BucketQuota();
            quota.setBucketQuota(size);
            obsClient.setBucketQuota(bucketName, quota);
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
    }

    @Override
    public Map<String, Long> getAllBucketsUsage(long storeId) {
        Map<String, Long> allBucketsUsage = new HashMap<>();
        try (ObsClient obsClient = getObsClient(storeId)) {
            for (Bucket bucket : listBuckets(storeId)) {
                String bucketName = bucket.getName();
                BucketStorageInfo storageInfo = obsClient.getBucketStorageInfo(bucketName);
                allBucketsUsage.put(bucketName, storageInfo.getSize());
            }
        } catch (Exception ex) {
            throw new CloudRuntimeException(ex);
        }
        return allBucketsUsage;
    }

    protected ObsClient getObsClient(long storeId) {
        ObjectStoreVO store = _storeDao.findById(storeId);
        String endpoint = store.getUrl();
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String clientAccessKey = storeDetails.get(ACCESS_KEY);
        String clientSecretKey = storeDetails.get(SECRET_KEY);
        return new ObsClient(clientAccessKey, clientSecretKey, endpoint);
    }
}
