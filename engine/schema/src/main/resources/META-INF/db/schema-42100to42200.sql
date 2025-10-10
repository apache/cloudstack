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

--;
-- Schema upgrade from 4.21.0.0 to 4.22.0.0
--;


-- health check status as enum
CALL `cloud`.`IDEMPOTENT_CHANGE_COLUMN`('router_health_check', 'check_result', 'check_result', 'varchar(16) NOT NULL COMMENT "check executions result: SUCCESS, FAILURE, WARNING, UNKNOWN"');

-- Increase length of scripts_version column to 128 due to md5sum to sha512sum change
CALL `cloud`.`IDEMPOTENT_CHANGE_COLUMN`('cloud.domain_router', 'scripts_version', 'scripts_version', 'VARCHAR(128)');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.snapshot_policy','domain_id', 'BIGINT(20) DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.snapshot_policy','account_id', 'BIGINT(20) DEFAULT NULL');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_schedule','domain_id', 'BIGINT(20) DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_schedule','account_id', 'BIGINT(20) DEFAULT NULL');

-- Increase the cache_mode column size from cloud.disk_offering table
CALL `cloud`.`IDEMPOTENT_CHANGE_COLUMN`('cloud.disk_offering', 'cache_mode', 'cache_mode', 'varchar(18) DEFAULT "none" COMMENT "The disk cache mode to use for disks created with this offering"');

-- Add uuid column to ldap_configuration table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.ldap_configuration', 'uuid', 'VARCHAR(40) NOT NULL');

-- Populate uuid for existing rows where uuid is NULL or empty
UPDATE `cloud`.`ldap_configuration` SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';

-- Add the column cross_zone_instance_creation to cloud.backup_repository. if enabled it means that new Instance can be created on all Zones from Backups on this Repository.
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_repository', 'cross_zone_instance_creation', 'TINYINT(1) DEFAULT NULL COMMENT ''Backup Repository can be used for disaster recovery on another zone''');

-- Updated display to false for password/token detail of the storage pool details
UPDATE `cloud`.`storage_pool_details` SET display = 0 WHERE name LIKE '%password%';
UPDATE `cloud`.`storage_pool_details` SET display = 0 WHERE name LIKE '%token%';
