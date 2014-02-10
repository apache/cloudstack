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

-- This file can be applied to an existing cloudbridge database.   It is used
-- to keep track of in progress multipart file uploads.
--
SET foreign_key_checks = 0;

DROP TABLE IF EXISTS multipart_uploads;
DROP TABLE IF EXISTS multipart_meta;
DROP TABLE IF EXISTS multipart_parts;

-- We need to keep track of the multipart uploads and all the parts of each upload until they
-- are completed or aborted.
-- The AccessKey is where we store the AWS account id
--
CREATE TABLE multipart_uploads (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	
	AccessKey  VARCHAR(150) NOT NULL,  -- this is the initiator of the request
	BucketName VARCHAR(64)  NOT NULL,
	NameKey    VARCHAR(255) NOT NULL,
	x_amz_acl  VARCHAR(64)  NULL,
	
	CreateTime DATETIME,

	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- We need to store all the meta data for an object being mutlipart uploaded 
-- UploadID is a foreign key to an entry in the mutipart_uploads table
--
CREATE TABLE multipart_meta (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	
	UploadID BIGINT NOT NULL,  
	Name  VARCHAR(64) NOT NULL,
	Value VARCHAR(256),
	
	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Each part of a multipart upload gets a row in this table
-- UploadId is a foreign key to an entry in the mutipart_uploads table
--
CREATE TABLE multipart_parts (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	
	UploadID BIGINT NOT NULL,  
	partNumber INT NOT NULL,
	MD5 VARCHAR(128),
 	StoredPath VARCHAR(256),					-- relative to mount point of the root
 	StoredSize BIGINT NOT NULL DEFAULT 0,
	
	CreateTime DATETIME,
	
	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET foreign_key_checks = 1;

