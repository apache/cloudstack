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

-- Add vm_id column to usage_event table for volume usage events
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.usage_event','vm_id', 'bigint UNSIGNED NULL COMMENT "VM ID associated with volume usage events"');
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_event','vm_id', 'bigint UNSIGNED NULL COMMENT "VM ID associated with volume usage events"');

-- Add vm_id column to cloud_usage.usage_volume table
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_volume','vm_id', 'bigint UNSIGNED NULL COMMENT "VM ID associated with the volume usage"');

-- Add the column cross_zone_instance_creation to cloud.backup_repository. if enabled it means that new Instance can be created on all Zones from Backups on this Repository.
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_repository', 'cross_zone_instance_creation', 'TINYINT(1) DEFAULT NULL COMMENT ''Backup Repository can be used for disaster recovery on another zone''');

