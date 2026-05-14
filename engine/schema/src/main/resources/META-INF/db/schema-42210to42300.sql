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

--- Disable/enable NICs
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.nics','enabled', 'TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''Indicates whether the NIC is enabled or not'' ');

--- Quota tariff/usage mapping
CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_tariff_usage` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `tariff_id` bigint(20) unsigned NOT NULL COMMENT 'ID of the tariff of the Quota usage detail calculated, foreign key to quota_tariff table',
    `quota_usage_id` bigint(20) unsigned NOT NULL COMMENT 'ID of the aggregation of Quota usage details, foreign key to quota_usage table',
    `quota_used` decimal(20,8) NOT NULL COMMENT 'Amount of quota used',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_quota_tariff_usage__tariff_id` FOREIGN KEY (`tariff_id`) REFERENCES `cloud_usage`.`quota_tariff` (`id`),
    CONSTRAINT `fk_quota_tariff_usage__quota_usage_id` FOREIGN KEY (`quota_usage_id`) REFERENCES `cloud_usage`.`quota_usage` (`id`));

-- Add the 'keep_mac_address_on_public_nic' column to the 'cloud.networks' and 'cloud.vpc' tables
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.networks', 'keep_mac_address_on_public_nic', 'TINYINT(1) NOT NULL DEFAULT 1');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc', 'keep_mac_address_on_public_nic', 'TINYINT(1) NOT NULL DEFAULT 1');

-- VMware CBT warm migration session state
CREATE TABLE IF NOT EXISTS `cloud`.`vmware_cbt_migration` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40) NOT NULL COMMENT 'UUID',
    `zone_id` bigint unsigned NOT NULL COMMENT 'Zone ID',
    `account_id` bigint unsigned NOT NULL COMMENT 'Account ID',
    `user_id` bigint unsigned NOT NULL COMMENT 'User ID',
    `vm_id` bigint unsigned COMMENT 'Imported VM ID after cutover',
    `existing_vcenter_id` bigint unsigned COMMENT 'Linked VMware datacenter ID when the source vCenter is registered',
    `destination_cluster_id` bigint unsigned NOT NULL COMMENT 'Destination KVM cluster ID',
    `convert_host_id` bigint unsigned COMMENT 'KVM host used for conversion and synchronization',
    `storage_pool_id` bigint unsigned COMMENT 'Destination primary storage pool ID',
    `display_name` varchar(255) COMMENT 'Target VM display name',
    `vcenter` varchar(255) COMMENT 'Source vCenter',
    `datacenter` varchar(255) COMMENT 'Source vCenter datacenter name',
    `source_host` varchar(255) COMMENT 'Source VMware host name or IP',
    `source_cluster` varchar(255) COMMENT 'Source VMware cluster name',
    `source_vm_name` varchar(255) NOT NULL COMMENT 'Source VM name on vCenter',
    `vddk_lib_dir` varchar(1024) COMMENT 'Optional VDDK library directory override',
    `vddk_transports` varchar(255) COMMENT 'Optional VDDK transport list override',
    `vddk_thumbprint` varchar(255) COMMENT 'Optional vCenter TLS thumbprint for VDDK connections',
    `state` varchar(32) NOT NULL COMMENT 'Migration state',
    `current_step` varchar(255) COMMENT 'Current migration step',
    `last_error` varchar(1024) COMMENT 'Last error message',
    `completed_cycles` int unsigned NOT NULL DEFAULT 0 COMMENT 'Completed CBT delta cycles',
    `quiet_cycles` int unsigned NOT NULL DEFAULT 0 COMMENT 'Consecutive quiet CBT delta cycles',
    `total_changed_bytes` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'Total changed bytes copied across delta cycles',
    `last_changed_bytes` bigint unsigned COMMENT 'Changed bytes copied in the latest delta cycle',
    `last_dirty_rate` bigint unsigned COMMENT 'Changed bytes per second in the latest delta cycle',
    `created` datetime NOT NULL COMMENT 'date created',
    `updated` datetime COMMENT 'date updated if not null',
    `removed` datetime COMMENT 'date removed if not null',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_vmware_cbt_migration__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_vmware_cbt_migration__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_vmware_cbt_migration__user_id` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_vmware_cbt_migration__vm_id` FOREIGN KEY (`vm_id`) REFERENCES `vm_instance`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_vmware_cbt_migration__existing_vcenter_id` FOREIGN KEY (`existing_vcenter_id`) REFERENCES `vmware_data_center`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_vmware_cbt_migration__destination_cluster_id` FOREIGN KEY (`destination_cluster_id`) REFERENCES `cluster`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_vmware_cbt_migration__convert_host_id` FOREIGN KEY (`convert_host_id`) REFERENCES `host`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_vmware_cbt_migration__storage_pool_id` FOREIGN KEY (`storage_pool_id`) REFERENCES `storage_pool`(`id`) ON DELETE SET NULL,
    INDEX `i_vmware_cbt_migration__zone_id` (`zone_id`),
    INDEX `i_vmware_cbt_migration__state` (`state`),
    INDEX `i_vmware_cbt_migration__source_vm_name` (`source_vm_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`vmware_cbt_migration_disk` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40) NOT NULL COMMENT 'UUID',
    `migration_id` bigint unsigned NOT NULL COMMENT 'VMware CBT migration ID',
    `source_disk_id` varchar(255) COMMENT 'Source VMware disk key or label',
    `source_disk_device_key` int COMMENT 'Source VMware virtual disk device key for QueryChangedDiskAreas',
    `source_disk_path` varchar(1024) COMMENT 'Source VMware disk path',
    `datastore_name` varchar(255) COMMENT 'Source VMware datastore name',
    `capacity_bytes` bigint unsigned COMMENT 'Source disk capacity in bytes',
    `target_path` varchar(1024) COMMENT 'Target KVM disk path',
    `target_format` varchar(32) COMMENT 'Target KVM disk format',
    `change_id` varchar(255) COMMENT 'Latest VMware CBT change ID',
    `snapshot_moref` varchar(255) COMMENT 'Latest VMware snapshot managed object reference',
    `state` varchar(32) NOT NULL COMMENT 'Disk synchronization state',
    `created` datetime NOT NULL COMMENT 'date created',
    `updated` datetime COMMENT 'date updated if not null',
    `removed` datetime COMMENT 'date removed if not null',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_vmware_cbt_migration_disk__migration_id` FOREIGN KEY (`migration_id`) REFERENCES `vmware_cbt_migration`(`id`) ON DELETE CASCADE,
    INDEX `i_vmware_cbt_migration_disk__migration_id` (`migration_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`vmware_cbt_migration_cycle` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40) NOT NULL COMMENT 'UUID',
    `migration_id` bigint unsigned NOT NULL COMMENT 'VMware CBT migration ID',
    `cycle_number` int unsigned NOT NULL COMMENT 'CBT delta cycle number',
    `snapshot_moref` varchar(255) COMMENT 'VMware snapshot managed object reference used for the cycle',
    `changed_bytes` bigint unsigned COMMENT 'Changed bytes copied in this cycle',
    `dirty_rate` bigint unsigned COMMENT 'Changed bytes per second in this cycle',
    `duration` bigint unsigned COMMENT 'Cycle duration in milliseconds',
    `state` varchar(32) NOT NULL COMMENT 'CBT delta cycle state',
    `description` varchar(1024) COMMENT 'Cycle description or error message',
    `created` datetime NOT NULL COMMENT 'date created',
    `updated` datetime COMMENT 'date updated if not null',
    `removed` datetime COMMENT 'date removed if not null',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_vmware_cbt_migration_cycle__migration_id` FOREIGN KEY (`migration_id`) REFERENCES `vmware_cbt_migration`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uc_vmware_cbt_migration_cycle__migration_id__cycle_number` (`migration_id`, `cycle_number`),
    INDEX `i_vmware_cbt_migration_cycle__migration_id` (`migration_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
