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

-- Creates the 'kvm.memory.dynamic.scaling.capacity' and, for already active ACS environments,
-- initializes it with the value of the setting 'vm.serviceoffering.ram.size.max'
INSERT INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `updated`, `scope`, `is_dynamic`, `group_id`, `subgroup_id`, `display_text`, `description`)
SELECT 'Advanced', 'DEFAULT', 'CapacityManager', 'kvm.memory.dynamic.scaling.capacity', `cfg`.`value`, 0, NULL, 4, 1, 6, 27,
       'KVM memory dynamic scaling capacity', 'Defines the maximum memory capacity in MiB for which VMs can be dynamically scaled to with KVM. The ''kvm.memory.dynamic.scaling.capacity'' setting''s value will be used to define the value of the ''<maxMemory />'' element of domain XMLs. If it is set to a value less than or equal to ''0'', then the host''s memory capacity will be considered.'
FROM `cloud`.`configuration` `cfg`
WHERE NOT EXISTS (SELECT 1 FROM `cloud`.`configuration` WHERE `name` = 'kvm.memory.dynamic.scaling.capacity')
  AND `cfg`.`name` = 'vm.serviceoffering.ram.size.max';

-- Creates the 'kvm.cpu.dynamic.scaling.capacity' and, for already active ACS environments,
-- initializes it with the value of the setting 'vm.serviceoffering.cpu.cores.max'
INSERT INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `updated`, `scope`, `is_dynamic`, `group_id`, `subgroup_id`, `display_text`, `description`)
SELECT 'Advanced', 'DEFAULT', 'CapacityManager', 'kvm.cpu.dynamic.scaling.capacity', `cfg`.`value`, 0, NULL, 4, 1, 6, 27,
       'KVM CPU dynamic scaling capacity', 'Defines the maximum vCPU capacity for which VMs can be dynamically scaled to with KVM. The ''kvm.cpu.dynamic.scaling.capacity'' setting''s value will be used to define the value of the ''<vcpu />'' element of domain XMLs. If it is set to a value less than or equal to ''0'', then the host''s CPU cores capacity will be considered.'
FROM `cloud`.`configuration` `cfg`
WHERE NOT EXISTS (SELECT 1 FROM `cloud`.`configuration` WHERE `name` = 'kvm.cpu.dynamic.scaling.capacity')
  AND `cfg`.`name` = 'vm.serviceoffering.cpu.cores.max';

-- Generalise VM schedule tables into resource-agnostic resource_schedule / resource_scheduled_job.
-- Step 1: rename vm_schedule → resource_schedule, rename vm_id → resource_id, add resource_type column.
ALTER TABLE `cloud`.`vm_schedule`
    DROP FOREIGN KEY `fk_vm_schedule__vm_id`,
    DROP INDEX `i_vm_schedule__vm_id`,
    DROP INDEX `i_vm_schedule__enabled_end_date`,
    CHANGE COLUMN `vm_id` `resource_id` bigint unsigned NOT NULL COMMENT 'id of the scheduled resource',
    ADD COLUMN `resource_type` varchar(64) NOT NULL DEFAULT 'VirtualMachine' COMMENT 'type of the scheduled resource' AFTER `uuid`;

RENAME TABLE `cloud`.`vm_schedule` TO `cloud`.`resource_schedule`;

ALTER TABLE `cloud`.`resource_schedule`
    ADD INDEX `i_resource_schedule__resource` (`resource_type`, `resource_id`),
    ADD INDEX `i_resource_schedule__enabled_end_date` (`enabled`, `end_date`);

-- Step 2: rename vm_scheduled_job → resource_scheduled_job, rename columns.
ALTER TABLE `cloud`.`vm_scheduled_job`
    DROP FOREIGN KEY `fk_vm_scheduled_job__vm_id`,
    DROP FOREIGN KEY `fk_vm_scheduled_job__vm_schedule_id`,
    DROP INDEX `i_vm_scheduled_job__vm_id`,
    DROP INDEX `i_vm_scheduled_job__scheduled_timestamp`,
    DROP INDEX `vm_schedule_id`,
    CHANGE COLUMN `vm_id` `resource_id` bigint unsigned NOT NULL COMMENT 'id of the scheduled resource',
    CHANGE COLUMN `vm_schedule_id` `schedule_id` bigint unsigned NOT NULL COMMENT 'id of the resource_schedule row',
    ADD COLUMN `resource_type` varchar(64) NOT NULL DEFAULT 'VirtualMachine' COMMENT 'type of the scheduled resource' AFTER `uuid`;

RENAME TABLE `cloud`.`vm_scheduled_job` TO `cloud`.`resource_scheduled_job`;

ALTER TABLE `cloud`.`resource_scheduled_job`
    ADD UNIQUE KEY `uc_resource_scheduled_job__schedule_timestamp` (`schedule_id`, `scheduled_timestamp`),
    ADD INDEX `i_resource_scheduled_job__resource` (`resource_type`, `resource_id`),
    ADD INDEX `i_resource_scheduled_job__scheduled_timestamp` (`scheduled_timestamp`),
    ADD CONSTRAINT `fk_resource_scheduled_job__schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `resource_schedule`(`id`) ON DELETE CASCADE;

-- Step 3: details table for action-specific parameters (used by the generic resource schedule API in Commit 2).
CREATE TABLE IF NOT EXISTS `cloud`.`resource_schedule_details` (
    `id` bigint unsigned NOT NULL auto_increment,
    `schedule_id` bigint unsigned NOT NULL COMMENT 'id of the resource_schedule row',
    `name` varchar(255) NOT NULL,
    `value` varchar(1024) NOT NULL,
    `display` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'should this detail be visible to the end user',
    PRIMARY KEY (`id`),
    INDEX `i_resource_schedule_details__schedule_id` (`schedule_id`),
    CONSTRAINT `fk_resource_schedule_details__schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `resource_schedule`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Step 4: rename CRUD event types from VM.SCHEDULE.{CREATE,UPDATE,DELETE} to the new generic SCHEDULE.{CREATE,UPDATE,DELETE}.
-- Action-execution events (VM.SCHEDULE.START, .STOP, .REBOOT, .FORCE_STOP, .FORCE_REBOOT) keep their existing names.
UPDATE `cloud`.`event` SET `type` = 'SCHEDULE.CREATE' WHERE `type` = 'VM.SCHEDULE.CREATE';
UPDATE `cloud`.`event` SET `type` = 'SCHEDULE.UPDATE' WHERE `type` = 'VM.SCHEDULE.UPDATE';
UPDATE `cloud`.`event` SET `type` = 'SCHEDULE.DELETE' WHERE `type` = 'VM.SCHEDULE.DELETE';

-- Step 5: Rename the global configuration key for the scheduler
UPDATE `cloud`.`configuration` SET name='scheduler.jobs.expire.interval' WHERE name='vmscheduler.jobs.expire.interval';

-- Remove stale realhostip.com default values; domain has been dead since ~2015.
UPDATE `cloud`.`configuration`
    SET value = NULL
    WHERE name IN ('consoleproxy.url.domain', 'secstorage.ssl.cert.domain')
      AND value IN ('realhostip.com', '*.realhostip.com');

-- Add management_server_details table to allow ManagementServer scope configs
CREATE TABLE IF NOT EXISTS `management_server_details` (
                                                           `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
                                                           `management_server_id` bigint unsigned NOT NULL COMMENT 'management server the detail is related to',
                                                           `name` varchar(255) NOT NULL COMMENT 'name of the detail',
    `value` varchar(255) NOT NULL,
    `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_management_server_details__management_server_id` FOREIGN KEY `fk_management_server_details__management_server_id`(`management_server_id`) REFERENCES `mshost`(`id`) ON DELETE CASCADE,
    KEY `i_management_server_details__name__value` (`name`(128),`value`(128))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add checkpoint tracking fields to backups table for incremental backup support
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'from_checkpoint_id', 'VARCHAR(255) DEFAULT NULL COMMENT "Previous active checkpoint id for incremental backups"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'to_checkpoint_id', 'VARCHAR(255) DEFAULT NULL COMMENT "New checkpoint id created for the next incremental backup"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'checkpoint_create_time', 'BIGINT DEFAULT NULL COMMENT "Checkpoint creation timestamp from libvirt"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'host_id', 'BIGINT UNSIGNED DEFAULT NULL COMMENT "Host where backup is running"');

-- Create image_transfer table for per-disk image transfers
CREATE TABLE IF NOT EXISTS `cloud`.`image_transfer`(
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40) NOT NULL COMMENT 'uuid',
    `account_id` bigint unsigned NOT NULL COMMENT 'Account ID',
    `domain_id` bigint unsigned NOT NULL COMMENT 'Domain ID',
    `data_center_id` bigint unsigned NOT NULL COMMENT 'Data Center ID',
    `backup_id` bigint unsigned COMMENT 'Backup ID',
    `volume_id` bigint unsigned NOT NULL COMMENT 'Volume ID',
    `host_id` bigint unsigned NOT NULL COMMENT 'Host ID',
    `transfer_url` varchar(255) COMMENT 'ImageIO transfer URL',
    `file` varchar(255) COMMENT 'File for the file backend',
    `phase` varchar(20) NOT NULL COMMENT 'Transfer phase: initializing, transferring, finished, failed',
    `socket` varchar(255) COMMENT 'Unix socket for nbd backend',
    `direction` varchar(20) NOT NULL COMMENT 'Direction: upload, download',
    `backend` varchar(20) NOT NULL COMMENT 'Backend: nbd, file',
    `progress` int COMMENT 'Transfer progress percentage (0-100)',
    `signed_ticket_id` varchar(255) COMMENT 'Signed ticket ID from ImageIO',
    `created` datetime NOT NULL COMMENT 'date created',
    `updated` datetime COMMENT 'date updated if not null',
    `removed` datetime COMMENT 'date removed if not null',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uuid` (`uuid`),
    CONSTRAINT `fk_image_transfer__backup_id` FOREIGN KEY (`backup_id`) REFERENCES `backups`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_image_transfer__volume_id` FOREIGN KEY (`volume_id`) REFERENCES `volumes`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_image_transfer__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
    INDEX `i_image_transfer__backup_id`(`backup_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--- Quota resource statement
INSERT INTO cloud.role_permissions (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'quotaResourceStatement', permission, sort_order
FROM cloud.role_permissions rp
WHERE rule = 'quotaStatement' AND NOT EXISTS(SELECT 1 FROM cloud.role_permissions rp_ WHERE rp.role_id = rp_.role_id AND rp_.rule = 'quotaResourceStatement');
