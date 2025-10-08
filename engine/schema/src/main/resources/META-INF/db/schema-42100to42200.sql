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

-- Add uuid column to ldap_configuration table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.ldap_configuration', 'uuid', 'VARCHAR(40) NOT NULL');

-- Populate uuid for existing rows where uuid is NULL or empty
UPDATE `cloud`.`ldap_configuration` SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';

-- Add the column cross_zone_instance_creation to cloud.backup_repository. if enabled it means that new Instance can be created on all Zones from Backups on this Repository.
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_repository', 'cross_zone_instance_creation', 'TINYINT(1) DEFAULT NULL COMMENT ''Backup Repository can be used for disaster recovery on another zone''');

-- Updated display to false for password/token detail of the storage pool details
UPDATE `cloud`.`storage_pool_details` SET display = 0 WHERE name LIKE '%password%';
UPDATE `cloud`.`storage_pool_details` SET display = 0 WHERE name LIKE '%token%';

-- VMware to KVM migration improvements
CREATE TABLE IF NOT EXISTS `cloud`.`import_vm_task`(
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40),
    `zone_id` bigint unsigned NOT NULL COMMENT 'Zone ID',
    `account_id` bigint unsigned NOT NULL COMMENT 'Account ID',
    `user_id` bigint unsigned NOT NULL COMMENT 'User ID',
    `vm_id` bigint unsigned COMMENT 'VM ID',
    `display_name` varchar(255) COMMENT 'Display VM Name',
    `vcenter` varchar(255) COMMENT 'VCenter',
    `datacenter` varchar(255) COMMENT 'VCenter Datacenter name',
    `source_vm_name` varchar(255) COMMENT 'Source VM name on vCenter',
    `convert_host_id` bigint unsigned COMMENT 'Convert Host ID',
    `import_host_id` bigint unsigned COMMENT 'Import Host ID',
    `step` varchar(20) NOT NULL COMMENT 'Importing VM Task Step',
    `description` varchar(255) COMMENT 'Importing VM Task Description',
    `duration` bigint unsigned COMMENT 'Duration in milliseconds for the completed tasks',
    `created` datetime NOT NULL COMMENT 'date created',
    `updated` datetime COMMENT 'date updated if not null',
    `removed` datetime COMMENT 'date removed if not null',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_import_vm_task__zone_id` FOREIGN KEY `fk_import_vm_task__zone_id` (`zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_import_vm_task__account_id` FOREIGN KEY `fk_import_vm_task__account_id` (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_import_vm_task__user_id` FOREIGN KEY `fk_import_vm_task__user_id` (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_import_vm_task__vm_id` FOREIGN KEY `fk_import_vm_task__vm_id` (`vm_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_import_vm_task__convert_host_id` FOREIGN KEY `fk_import_vm_task__convert_host_id` (`convert_host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_import_vm_task__import_host_id` FOREIGN KEY `fk_import_vm_task__import_host_id` (`import_host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
    INDEX `i_import_vm_task__zone_id`(`zone_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
