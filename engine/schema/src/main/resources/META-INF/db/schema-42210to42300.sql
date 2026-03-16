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
-- Schema upgrade from 4.22.1.0 to 4.23.0.0
--;

CREATE TABLE `cloud`.`backup_offering_details` (
    `id` bigint unsigned NOT NULL auto_increment,
    `backup_offering_id` bigint unsigned NOT NULL COMMENT 'Backup offering id',
    `name` varchar(255) NOT NULL,
    `value` varchar(1024) NOT NULL,
    `display` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should detail be displayed to the end user',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_offering_details__backup_offering_id` FOREIGN KEY `fk_offering_details__backup_offering_id`(`backup_offering_id`) REFERENCES `backup_offering`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Update value to random for the config 'vm.allocation.algorithm' or 'volume.allocation.algorithm' if configured as userconcentratedpod_random
-- Update value to firstfit for the config 'vm.allocation.algorithm' or 'volume.allocation.algorithm' if configured as userconcentratedpod_firstfit
UPDATE `cloud`.`configuration` SET value='random' WHERE name IN ('vm.allocation.algorithm', 'volume.allocation.algorithm') AND value='userconcentratedpod_random';
UPDATE `cloud`.`configuration` SET value='firstfit' WHERE name IN ('vm.allocation.algorithm', 'volume.allocation.algorithm') AND value='userconcentratedpod_firstfit';

-- Create kubernetes_cluster_affinity_group_map table for CKS per-node-type affinity groups
CREATE TABLE IF NOT EXISTS `cloud`.`kubernetes_cluster_affinity_group_map` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `cluster_id` bigint unsigned NOT NULL COMMENT 'kubernetes cluster id',
    `node_type` varchar(32) NOT NULL COMMENT 'CONTROL, WORKER, or ETCD',
    `affinity_group_id` bigint unsigned NOT NULL COMMENT 'affinity group id',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_kubernetes_cluster_ag_map__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `kubernetes_cluster`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kubernetes_cluster_ag_map__ag_id` FOREIGN KEY (`affinity_group_id`) REFERENCES `affinity_group`(`id`) ON DELETE CASCADE,
    INDEX `i_kubernetes_cluster_ag_map__cluster_id`(`cluster_id`),
    INDEX `i_kubernetes_cluster_ag_map__ag_id`(`affinity_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create webhook_filter table
DROP TABLE IF EXISTS `cloud`.`webhook_filter`;
CREATE TABLE IF NOT EXISTS `cloud`.`webhook_filter` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id of the webhook filter',
    `uuid` varchar(255) COMMENT 'uuid of the webhook filter',
    `webhook_id` bigint unsigned  NOT NULL COMMENT 'id of the webhook',
    `type` varchar(20) COMMENT 'type of the filter',
    `mode` varchar(20) COMMENT 'mode of the filter',
    `match_type` varchar(20) COMMENT 'match type of the filter',
    `value` varchar(256) NOT NULL COMMENT 'value of the filter used for matching',
    `created` datetime NOT NULL COMMENT 'date created',
    PRIMARY KEY (`id`),
    INDEX `i_webhook_filter__webhook_id`(`webhook_id`),
    CONSTRAINT `fk_webhook_filter__webhook_id` FOREIGN KEY(`webhook_id`) REFERENCES `webhook`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- "api_keypair" table for API and secret keys
CREATE TABLE IF NOT EXISTS `cloud`.`api_keypair` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `uuid` varchar(40) UNIQUE NOT NULL,
    `name` varchar(255) NOT NULL,
    `domain_id` bigint(20) unsigned NOT NULL,
    `account_id` bigint(20) unsigned NOT NULL,
    `user_id` bigint(20) unsigned NOT NULL,
    `start_date` datetime,
    `end_date` datetime,
    `description` varchar(100),
    `api_key` varchar(255) NOT NULL,
    `secret_key` varchar(255) NOT NULL,
    `created` datetime NOT NULL,
    `removed` datetime,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_api_keypair__user_id` FOREIGN KEY(`user_id`) REFERENCES `cloud`.`user`(`id`),
    CONSTRAINT `fk_api_keypair__account_id` FOREIGN KEY(`account_id`) REFERENCES `cloud`.`account`(`id`),
    CONSTRAINT `fk_api_keypair__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `cloud`.`domain`(`id`)
);

-- "api_keypair_permissions" table for API key pairs permissions
CREATE TABLE IF NOT EXISTS `cloud`.`api_keypair_permissions` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `uuid` varchar(40) UNIQUE,
    `sort_order` bigint(20) unsigned NOT NULL DEFAULT 0,
    `rule` varchar(255) NOT NULL,
    `api_keypair_id` bigint(20) unsigned NOT NULL,
    `permission` varchar(255) NOT NULL,
    `description` varchar(255),
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_keypair_permissions__api_keypair_id` FOREIGN KEY(`api_keypair_id`) REFERENCES `cloud`.`api_keypair`(`id`)
);

-- Populate "api_keypair" table with existing user API keys
INSERT INTO `cloud`.`api_keypair` (uuid, user_id, domain_id, account_id, api_key, secret_key, created, name)
SELECT UUID(), user.id, account.domain_id, account.id, user.api_key, user.secret_key, NOW(), 'Active key pair'
FROM `cloud`.`user` AS user
JOIN `cloud`.`account` AS account ON user.account_id = account.id
WHERE user.api_key IS NOT NULL AND user.secret_key IS NOT NULL;

-- Drop API keys from user table
ALTER TABLE `cloud`.`user` DROP COLUMN api_key, DROP COLUMN secret_key;

-- Grant access to the "deleteUserKeys" API to the "User", "Domain Admin" and "Resource Admin" roles, similarly to the "registerUserKeys" API
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('User', 'deleteUserKeys', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Domain Admin', 'deleteUserKeys', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Resource Admin', 'deleteUserKeys', 'ALLOW');

-- Add conserve mode for VPC offerings
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc_offerings','conserve_mode', 'tinyint(1) unsigned NULL DEFAULT 0 COMMENT ''True if the VPC offering is IP conserve mode enabled, allowing public IP services to be used across multiple VPC tiers'' ');

-- KNIB

CREATE TABLE IF NOT EXISTS `cloud`.`native_backup_pool_ref` (
    `id` bigint NOT NULL UNIQUE AUTO_INCREMENT,
    `backup_id` bigint unsigned NOT NULL COMMENT 'The backup ID. Foreign key that points to the backups table.',
    `storage_pool_id` bigint unsigned NOT NULL COMMENT 'The storage ID. Foreign key that points to the storage_pool table.',
    `volume_id` bigint unsigned NOT NULL COMMENT 'The volumes ID. Foreign key that points to the volumes table.',
    `backup_delta_path` varchar(255) COMMENT 'Path of the created delta.',
    `backup_parent_path` varchar(255) COMMENT 'Path of the created delta parent.',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_native_backup_pool_ref__backup_id` FOREIGN KEY (`backup_id`) REFERENCES `backups`(`id`),
    CONSTRAINT `fk_native_backup_pool_ref__storage_pool_id` FOREIGN KEY (`storage_pool_id`) REFERENCES `storage_pool`(`id`),
    CONSTRAINT `fk_native_backup_pool_ref__volume_id` FOREIGN KEY (`volume_id`) REFERENCES `volumes`(`id`)
    );

CREATE TABLE IF NOT EXISTS `cloud`.`native_backup_store_ref` (
     `id` bigint NOT NULL UNIQUE AUTO_INCREMENT,
     `backup_id` bigint unsigned NOT NULL COMMENT 'The backup ID. Foreign key that points to the backups table.',
     `volume_id` bigint unsigned NOT NULL COMMENT 'The volume ID. Foreign key that points to the volumes table.',
     `device_id` bigint unsigned COMMENT 'device ID of the volume',
     `path` varchar(255) COMMENT 'Path of the backup.',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_native_backup_store_ref__backup_id` FOREIGN KEY (`backup_id`) REFERENCES `backups`(`id`),
    CONSTRAINT `fk_native_backup_store_ref__volume_id` FOREIGN KEY (`volume_id`) REFERENCES `volumes`(`id`)
    );


CREATE TABLE IF NOT EXISTS `cloud`.`native_backup_offering` (
    `id` bigint NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid` varchar(40) NOT NULL,
    `name` varchar(255) NOT NULL,
    `compress` tinyint(1) UNSIGNED NOT NULL,
    `validate` tinyint(1) UNSIGNED NOT NULL,
    `validation_steps` varchar(255),
    `allow_quick_restore` tinyint(1) UNSIGNED NOT NULL,
    `allow_extract_file` tinyint(1) UNSIGNED NOT NULL,
    `backup_chain_size` INT,
    `compression_library` varchar(55) NOT NULL DEFAULT 'zstd',
    `created` datetime NOT NULL,
    `removed` datetime,
    PRIMARY KEY (`id`)
    );

CREATE TABLE IF NOT EXISTS `cloud`.`native_backup_service_job` (
    `id` bigint NOT NULL UNIQUE AUTO_INCREMENT,
    `backup_id` bigint unsigned NOT NULL COMMENT 'The backup ID. Foreign key that points to the backups table.',
    `instance_id` bigint unsigned NOT NULL COMMENT 'The instance ID. Foreign key that points to the vm_instance table.',
    `account_id` bigint unsigned COMMENT 'Account ID of the owner of the VM.',
    `host_id` bigint unsigned COMMENT 'The host ID that is executing the compression. Foreign key that points to the host table.',
    `zone_id` bigint unsigned NOT NULL COMMENT 'The zone ID of the where the VM is. Foreign key that points to the data_center table',
    `attempts` int(32) unsigned NOT NULL DEFAULT 0,
    `type` varchar(55) NOT NULL,
    `created` datetime NOT NULL,
    `scheduled_start_time` datetime NOT NULL,
    `start_time` datetime,
    `removed` datetime,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_backup_compression_job__backup_id` FOREIGN KEY (`backup_id`) REFERENCES `backups`(`id`),
    CONSTRAINT `fk_backup_compression_job__instance_id` FOREIGN KEY (`instance_id`) REFERENCES `vm_instance`(`id`),
    CONSTRAINT `fk_backup_compression_job__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`),
    CONSTRAINT `fk_backup_compression_job__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center`(`id`)
    );

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'uncompressed_size', 'bigint unsigned');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'compression_status', 'varchar(55)');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'validation_status', 'varchar(55)');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_schedule', 'isolated', 'TINYINT(1) NOT NULL DEFAULT 0 COMMENT "Whether the scheduled backups will be isolated or not."');

UPDATE `cloud`.`configuration` SET `value`=CONCAT(`value`, ', backupValidationCommandTimeout, backupValidationScreenshotWait, backupValidationBootTimeout')
WHERE `name`='user.vm.readonly.details' AND `value` IS NOT NULL;
