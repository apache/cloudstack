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

SET foreign_key_checks = 0;

DROP TABLE IF EXISTS shost;
DROP TABLE IF EXISTS mhost;
DROP TABLE IF EXISTS mhost_mount;
DROP TABLE IF EXISTS sbucket;
DROP TABLE IF EXISTS sobject;
DROP TABLE IF EXISTS sobject_item;
DROP TABLE IF EXISTS meta;
DROP TABLE IF EXISTS acl;
DROP TABLE IF EXISTS usercredentials;

-- storage host
CREATE TABLE shost (							
	ID BIGINT NOT NULL AUTO_INCREMENT,
	
	Host VARCHAR(128) NOT NULL,
	HostType INT NOT NULL DEFAULT 0, 	-- 0 : local, 1 : nfs
	ExportRoot VARCHAR(128) NOT NULL,
	
	MHostID BIGINT,						-- when host type is local, MHostID points to its owner management host												
	
	UserOnHost VARCHAR(64),
	UserPassword VARCHAR(128),
	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- management host
CREATE TABLE mhost (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	
	MHostKey VARCHAR(128) NOT NULL, 	-- host key could be derived from MAC address or named configuration value
	Host VARCHAR(128),					-- public host address for redirecting request from/to 
	
	Version VARCHAR(64),
	LastHeartbeatTime DATETIME,
	
	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE mhost_mount (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	MHostID BIGINT NOT NULL,
	SHostID BIGINT NOT NULL,

	MountPath VARCHAR(256),				-- local mount path
	LastMountTime DATETIME,				-- null : unmounted, otherwise the mount location
	
	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE sbucket (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	
	Name VARCHAR(64) NOT NULL,
	OwnerCanonicalID VARCHAR(150) NOT NULL,
	
	SHostID BIGINT,
	
	CreateTime DATETIME,
	
	VersioningStatus INT NOT NULL DEFAULT 0,  -- 0 : initial not set, 1 : enabled, 2 : suspended 
	
	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE sobject (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	
	SBucketID BIGINT NOT NULL,
	NameKey VARCHAR(255) NOT NULL,
	
	OwnerCanonicalID VARCHAR(150) NOT NULL,
	NextSequence INT NOT NULL DEFAULT 1,
	DeletionMark VARCHAR (150), 
 	
 	CreateTime DATETIME,
 	
	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE sobject_item (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	
	SObjectID BIGINT NOT NULL,
 	Version VARCHAR(64),
 	
 	MD5 VARCHAR(128),
 	StoredPath VARCHAR(256),					-- relative to mount point of the root
 	StoredSize BIGINT NOT NULL DEFAULT 0,
 	
 	CreateTime DATETIME,
 	LastModifiedTime DATETIME,
 	LastAccessTime DATETIME,
 	
	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE meta (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	
	Target VARCHAR(64) NOT NULL,
	TargetID BIGINT NOT NULL,

	Name VARCHAR(64) NOT NULL,
	Value VARCHAR(256),

	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE acl (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	
	Target VARCHAR(64) NOT NULL,
	TargetID BIGINT NOT NULL,
	
	GranteeType INT NOT NULL DEFAULT 0,			-- 0 : Cloud service user, 1 : Cloud user community, 2: Public user community
	GranteeCanonicalID VARCHAR(150),            -- make it big enought to hold a Cloud API access key
	
	Permission INT NOT NULL DEFAULT 0,			-- 0 : no permission, 1 : read, 2 : write, 4 : read_acl, 8 : write_acl 
	GrantOrder INT NOT NULL DEFAULT 0, 
	
	CreateTime DATETIME,
	LastModifiedTime DATETIME,

	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- each account has to have a separate <AccessKey,SecretKey>
-- each account has to have a separate <CertUniqueID,AccessKey> mappings
CREATE TABLE usercredentials (
	ID BIGINT NOT NULL AUTO_INCREMENT,
	
	AccessKey VARCHAR(150) NOT NULL,
	SecretKey VARCHAR(150) NOT NULL,
	CertUniqueId VARCHAR(200),

	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET foreign_key_checks = 1;

