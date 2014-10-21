-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
-- 
--   http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.


USE cloudbridge;

-- This file (and cloudbridge_policy_alter.sql) can be applied to an existing cloudbridge 
-- database.   It is used to manage defined bucket access policies.
--
SET foreign_key_checks = 0;

DROP TABLE IF EXISTS bucket_policies;

-- 1) Amazon S3 only allows one policy to be defined for a bucket.
-- 2) The maximum size of a policy is 20 KB.
-- 3) A bucket policy has to be able to exist even before the bucket itself (e.g., to
--    support "CreateBucket" actions).
--
CREATE TABLE bucket_policies (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	
	BucketName VARCHAR(64) NOT NULL,
	OwnerCanonicalID VARCHAR(150) NOT NULL,
	
	Policy VARCHAR(20000) NOT NULL,  -- policies are written in JSON 

	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET foreign_key_checks = 1;

