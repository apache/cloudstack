USE cloudbridge;

ALTER TABLE bucket_policies ADD UNIQUE one_policy_per_bucket(BucketName);
