package org.apache.cloudstack.storage.datastore.driver;

public final class EcsConstants {
    private EcsConstants() {}

    // Object store details keys
    public static final String MGMT_URL  = "mgmt_url";
    public static final String SA_USER   = "sa_user";
    public static final String SA_PASS   = "sa_password";
    public static final String NAMESPACE = "namespace";
    public static final String INSECURE  = "insecure";
    public static final String S3_HOST   = "s3_host";
    public static final String USER_PREFIX = "user_prefix";
    public static final String DEFAULT_USER_PREFIX = "cs-";

    // Per-account keys
    public static final String AD_KEY_ACCESS = "ecs.accesskey";
    public static final String AD_KEY_SECRET = "ecs.secretkey";
}
