# Cloudian HyperStore Object Storage Plugin

## Plugin Purpose

This plugin implements the Object Storage DataStore for Cloudian HyperStore.

## About Cloudian HyperStore

Cloudian HyperStore is a fully AWS-S3 compatible Object Storage solution. The following services are used by this plugin.

| Service | HTTP Port | HTTPS Port | Description            |
|:-------:|----------:|-----------:|:-----------------------|
|  Admin  |           |      19443 | User Management etc.   |
|   S3    |        80 |        443 | AWS-S3 compatible API  |
|   IAM   |     16080 |      16443 | AWS-IAM compatible API |

## Configuration

### HyperStore Configuration

1. Enable Bucket Usage Statistics

   Bucket Level QoS settings must be set to true. On HyperStore 8+, this can be done as follows. Earlier versions require puppet configuration which is not documented here.

   ```shell
   hsh$ hsctl config set s3.qos.bucketLevel=true
   hsh$ hsctl config apply s3 cmc
   hsh$ hsctl service restart s3 cmc --nodes=ALL
   ```

2. The Admin API Username and Password

   The connector requires an ADMIN API username and password to connect to the Admin service and create and manage HyperStore resources such as HyperStore Users and Groups. Please review your HyperStore Admin Guide and the settings under the `admin.auth` namespace.

3. Enable Object Lock via License

   HyperStore fully supports S3 Object Lock. However, Object Lock is currently only available with a special Object Lock License from Cloudian. If the connected HyperStore system does not have an Object Lock license, it will only allow creating regular buckets. Contact Cloudian Support to request an Object Lock license if required.

### CloudStack Configuration

A new `Cloudian HyperStore` Object Store can be added by the CloudStack `admin` user via the UI -> Infrastructure -> Object Storage -> Add Object Storage button.

Once added, this passes various configuration parameters to the LifeCycle class as a map with the following keys and values.

```text
DataStoreInfo MAP
++++++++++++++++++++++++++++++++++++++
| Key         | Value                |
|-------------|----------------------|
|name         | <user`s choice>      |
|providerName | Cloudian HyperStore  |
|url          | <ADMIN endpoint URL> |
|details      |     <MAP> ===========|=====+
++++++++++++++++++++++++++++++++++++++     v
                                           v
  　+======================================+
    V
Details MAP
++++++++++++++++++++++++++++++++++
| Key         | Value            |
|-------------|------------------|
| validateSSL | true/false       |
| accesskey   | Admin Username   |
| secretkey   | Admin Password   |
| s3Url       | S3 endpoint URL  |
| iamUrl      | IAM endpoint URL |
++++++++++++++++++++++++++++++++++
```

The following "details" map entries are all required.

- validateSSL : The ADMIN API is internal and may not have a proper SSL Certificate.
- accesskey : Reuse of a shared configuration parameter to pass the Admin Username.
- secretkey : Reuse of a shared configuration parameter to pass the Admin password.
- s3Url : The HyperStore S3 endpoint URL. HTTPS is preferred when the service has a proper SSL Certificate which should be true in production.
- iamUrl : The HyperStore IAM endpoint URL. Again HTTPS is preferred.

The LifeCycle initialize() method should validate connectivity to the different services.

## CloudStack Account Mappings

| CloudStack | HyperStore       | Name Assigned        |
|:-----------|:-----------------|:---------------------|
| Domain     | HyperStore Group | Domain UUID          |
| Account    | HyperStore User  | Account UUID         |
| Project    | HyperStore User  | Project Account UUID |

When a CloudStack Account user creates a bucket under their account for the first time a new HyperStore User is allocated under the HyperStore Group that is mapped to the CloudStack Domain. A new HyperStore Group is also allocated if one does not already exist.

## HyperStore User Resources

The following additional resources are also created for each HyperStore User.

| Resource                 | Description                                                                                                                                                                                                                                                         |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Root Credential Pair     | These credentials have full access to the HyperStore User account. They are used to manage the IAM user resources listed below as well as to perform any top level bucket actions such as creating buckets, updating policies, enabling versioning etc.             |
| IAM User "CloudStack"    | The "CloudStack" IAM user is created with an inline policy as-per below. The IAM user is used by the CloudStack Bucket Browser UI to manage bucket contents.                                                                                                        |
| IAM User Policy          | This inline IAM user policy grants the "CloudStack" IAM user permission to any S3 action except `s3:createBucket` and `s3:deleteBucket`. This is mostly to ensure that all Buckets remain under CloudStack control as well as to restrict control over IAM actions. |
| IAM User Credential Pair | The "CloudStack" IAM user credentials are also managed by the plugin and are made available to the user under the "Bucket Details" page. They are additionally used by the CloudStack Bucket Browser UI. They are restricted by the aforementioned user policy.     |

## Bucket Management

The following are noteworthy.

### Bucket Quota is Unsupported

Cloudian HyperStore does not currently support restricting the size of a bucket to a particular quota limit. The plugin accepts a quota value of 0 to indicate no quota setting. When creating a bucket in the CloudStack UI, the user is required to set a quota of 0. Any other value will fail.

### Bucket Usage

HyperStore does not collect bucket usage statistics by default. They must be enabled by the HyperStore Administrator. On systems where this has not been enabled, bucket usage is reported as 0 bytes.

See the configuration section above for more details.

### Supported Bucket Policies

Two "policies" are configurable using the CloudStack interface.

- Private : Objects are only accessible to the bucket owner. This is the equivalent of no bucket policy (and is implemented that way).
- Public : Objects are readable to everyone. Listing of all bucket objects is not granted so the object name must be known in order to access it.

  ```json
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "PublicReadForObjects",
        "Effect": "Allow",
        "Principal": "*",
        "Action": "s3:GetObject",
        "Resource": "arn:aws:s3:::BUCKET/*"
      }
    ]
  }
  ```

### Additional Bucket CORS Settings

Buckets created by the CloudStack plugin are additionally created with a Cross-Origin Resource Sharing (CORS) configuration. A permissive CORS setting on buckets is required by the CloudStack Bucket Browser UI functionality as it is written in JavaScript and runs in the end user's browser.

```xml
<CORSConfiguration xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
  <CORSRule>
    <ID>AllowAny</ID>
    <AllowedOrigin>*</AllowedOrigin>
    <AllowedMethod>GET</AllowedMethod>
    <AllowedMethod>HEAD</AllowedMethod>
    <AllowedMethod>PUT</AllowedMethod>
    <AllowedMethod>POST</AllowedMethod>
    <AllowedMethod>DELETE</AllowedMethod>
    <AllowedHeader>*</AllowedHeader>
  </CORSRule>
</CORSConfiguration>
```

### Visibility of other Buckets under the same HyperStore User

While the "CloudStack" IAM user cannot create other buckets under the HyperStore User account, there are other reasons that buckets can exist under the HyperStore user but not be known by the CloudStack. These include network connectivity issues between creating a bucket and updating the database. Note that this can usually be rectified by retrying the create bucket operation.

While a bucket is not visible to CloudStack, a 3rd party application using the same IAM credentials will be able to see and operate on the bucket.

## Interoperability with Existing HyperStore Plugin

This plugin is mostly interoperable with the existing HyperStore Infrastructure plugin. However, it is recommended to use one or the other but __not both__ plugins.

The purpose of the older HyperStore infrastructure plugin is to grant full access to the HyperStore User that is mapped to the CloudStack Account. As such it grants the logged in CloudStack Account Single-Sign-On (SSO) into the Cloudian Management Console (CMC) as the Root User of the HyperStore User. This would allow the CloudStack Account to create and delete HyperStore User resources (credentials/IAM users/federated logins/buckets/etc) outside CloudStack control.

In comparison, this plugin attempts to restrict HyperStore User level, IAM and Bucket level actions by providing CloudStack Account access via IAM credentials.

## Known Issues

1. Currently, there is no way to edit the Object Storage Configuration for any of the parameters configured in the "details" map. It seems that other Object Storage providers have the same issue.
2. The Bucket Browser UI feature may not work correctly on HyperStore versions older than 8.2 due to some bugs in the CORS implementation. However, everything else will still function correctly.
3. Object metadata is not correctly displayed in the CloudStack Bucket Browser. This is due to the javascript client using a MinIO only (non-s3 compatible) extension call that collects the metadata as part of the bucket listing. To fix this for non-MinIO S3 Object Stores, Object Metadata should be collected using the S3 standard headObject operation.
4. CloudStack does not yet have a deleteUser API for Object Stores so when a CloudStack Account is deleted, the mapped HyperStore User is not currently cleaned up.
