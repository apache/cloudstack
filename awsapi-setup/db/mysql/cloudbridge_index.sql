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

ALTER TABLE shost ADD UNIQUE shost_uq_host(Host, HostType, ExportRoot);
ALTER TABLE shost ADD CONSTRAINT FOREIGN KEY shost_fk_mhost_id(MHostID) REFERENCES mhost(ID);
ALTER TABLE shost ADD INDEX shost_idx_mhost_id(MHostID);

ALTER TABLE sbucket ADD UNIQUE sbucket_uq_name(Name);
ALTER TABLE sbucket ADD CONSTRAINT FOREIGN KEY sbucket_fk_shost_id(SHostID) REFERENCES shost(ID);
ALTER TABLE sbucket ADD INDEX sbucket_idx_shost_id(SHostID);
ALTER TABLE sbucket ADD INDEX sbucket_idx_owner_cid(OwnerCanonicalID);
ALTER TABLE sbucket ADD INDEX sbucket_idx_create_time(CreateTime);

ALTER TABLE sobject ADD CONSTRAINT FOREIGN KEY sobject_fk_sbuckt_id(SBucketID) REFERENCES sbucket(ID) ON DELETE CASCADE;
ALTER TABLE sobject ADD INDEX sobject_idx_bucket_id(SBucketID);
ALTER TABLE sobject ADD INDEX sobject_idx_owner_cid(OwnerCanonicalID);
ALTER TABLE sobject ADD UNIQUE sobject_uq_sbucket_id_name_key(SBucketID, NameKey);
ALTER TABLE sobject ADD INDEX sobject_idx_create_time(CreateTime);

ALTER TABLE sobject_item ADD CONSTRAINT FOREIGN KEY sobject_item_fk_object_id(SObjectID) REFERENCES sobject(ID) ON DELETE CASCADE;
ALTER TABLE sobject_item ADD INDEX sobject_item_idx_object_id(SObjectID);
ALTER TABLE sobject_item ADD UNIQUE sobject_item_uq_sobject_id_version(SObjectID, Version);
ALTER TABLE sobject_item ADD INDEX sobject_item_idx_create_time(CreateTime);
ALTER TABLE sobject_item ADD INDEX sobject_item_idx_modify_time(LastModifiedTime);
ALTER TABLE sobject_item ADD INDEX sobject_item_idx_access_time(LastAccessTime);
ALTER TABLE sobject_item ADD INDEX sobject_item_idx_stored_size(StoredSize);

ALTER TABLE meta ADD UNIQUE meta_uq_target_name(Target, TargetID, Name);
ALTER TABLE meta ADD INDEX meta_idx_target(Target, TargetID);

ALTER TABLE usercredentials ADD UNIQUE usercredentials_mappings1(AccessKey);
ALTER TABLE usercredentials ADD UNIQUE usercredentials_mappings2(CertUniqueId);
ALTER TABLE usercredentials ADD INDEX usercredentials_idx_access(AccessKey);
ALTER TABLE usercredentials ADD INDEX usercredentials_idx_cert(CertUniqueId);

ALTER TABLE acl ADD INDEX acl_idx_target(Target, TargetID);
ALTER TABLE acl ADD INDEX acl_idx_modify_time(LastModifiedTime);

ALTER TABLE mhost ADD UNIQUE mhost_uq_host(Host);
ALTER TABLE mhost ADD INDEX mhost_idx_mhost_key(MHostKey);
ALTER TABLE mhost ADD INDEX mhost_idx_heartbeat_time(LastHeartbeatTime);

ALTER TABLE mhost_mount ADD UNIQUE mhost_mnt_uq_mhost_shost(MHostID, SHostID);
ALTER TABLE mhost_mount ADD CONSTRAINT FOREIGN KEY mhost_mnt_fk_mhost_id(MHostID) REFERENCES mhost(ID) ON DELETE CASCADE;
ALTER TABLE mhost_mount ADD CONSTRAINT FOREIGN KEY mhost_mnt_fk_shost_id(SHostID) REFERENCES shost(ID) ON DELETE CASCADE;
ALTER TABLE mhost_mount ADD INDEX mhost_mnt_idx_mhost_id(MHostID);
ALTER TABLE mhost_mount ADD INDEX mhost_mnt_idx_shost_id(SHostID);
ALTER TABLE mhost_mount ADD INDEX mhost_mnt_idx_mount_time(LastMountTime);
