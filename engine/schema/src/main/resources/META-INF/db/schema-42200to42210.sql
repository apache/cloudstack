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
-- Schema upgrade from 4.22.0.0 to 4.22.1.0
--;

-- Add vm_id column to usage_event table for volume usage events
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.usage_event','vm_id', 'bigint UNSIGNED NULL COMMENT "VM ID associated with volume usage events"');
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_event','vm_id', 'bigint UNSIGNED NULL COMMENT "VM ID associated with volume usage events"');

-- Add vm_id column to cloud_usage.usage_volume table
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_volume','vm_id', 'bigint UNSIGNED NULL COMMENT "VM ID associated with the volume usage"');

ALTER TABLE `cloud`.`template_store_ref` MODIFY COLUMN `download_url` varchar(2048);

UPDATE `cloud`.`alert` SET type = 33 WHERE name = 'ALERT.VR.PUBLIC.IFACE.MTU';
UPDATE `cloud`.`alert` SET type = 34 WHERE name = 'ALERT.VR.PRIVATE.IFACE.MTU';

-- Update configuration 'kvm.ssh.to.agent' description and is_dynamic fields
UPDATE `cloud`.`configuration` SET description = 'True if the management server will restart the agent service via SSH into the KVM hosts after or during maintenance operations', is_dynamic = 1 WHERE name = 'kvm.ssh.to.agent';

-- Sanitize legacy network-level addressing fields for Public networks
UPDATE `cloud`.`networks`
SET `broadcast_uri` = NULL,
	`gateway` = NULL,
	`cidr` = NULL,
	`ip6_gateway` = NULL,
	`ip6_cidr` = NULL
WHERE `traffic_type` = 'Public';

UPDATE `cloud`.`vm_template` SET guest_os_id = 99 WHERE name = 'kvm-default-vm-import-dummy-template';

-- Update existing vm_template records with NULL type to "USER"
UPDATE `cloud`.`vm_template` SET `type` = 'USER' WHERE `type` IS NULL;

-- VMware CBT warm migration session state
CREATE TABLE IF NOT EXISTS `cloud`.`vmware_cbt_migration` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40) NOT NULL COMMENT 'UUID',
    `zone_id` bigint unsigned NOT NULL COMMENT 'Zone ID',
    `account_id` bigint unsigned NOT NULL COMMENT 'Account ID',
    `user_id` bigint unsigned NOT NULL COMMENT 'User ID',
    `vm_id` bigint unsigned COMMENT 'Imported VM ID after cutover',
    `destination_cluster_id` bigint unsigned NOT NULL COMMENT 'Destination KVM cluster ID',
    `convert_host_id` bigint unsigned COMMENT 'KVM host used for conversion and synchronization',
    `storage_pool_id` bigint unsigned COMMENT 'Destination primary storage pool ID',
    `display_name` varchar(255) COMMENT 'Target VM display name',
    `vcenter` varchar(255) COMMENT 'Source vCenter',
    `datacenter` varchar(255) COMMENT 'Source vCenter datacenter name',
    `source_host` varchar(255) COMMENT 'Source VMware host name or IP',
    `source_cluster` varchar(255) COMMENT 'Source VMware cluster name',
    `source_vm_name` varchar(255) NOT NULL COMMENT 'Source VM name on vCenter',
    `state` varchar(32) NOT NULL COMMENT 'Migration state',
    `current_step` varchar(64) COMMENT 'Current migration step',
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

-- remove unused config item
DELETE FROM `cloud`.`configuration` WHERE name = 'consoleproxy.cmd.port';

-- Drops the unused "backup_interval_type" column of the "cloud.backups" table
ALTER TABLE `cloud`.`backups` DROP COLUMN `backup_interval_type`;

-- Update `user.password.reset.mail.template` configuration value to match new logic
UPDATE `cloud`.`configuration`
SET value = CONCAT_WS('\n', 'Hello {{username}}!', 'You have requested to reset your password. Please click the following link to reset your password:', '{{{resetLink}}}', 'If you did not request a password reset, please ignore this email.', '', 'Regards,', 'The CloudStack Team')
WHERE name = 'user.password.reset.mail.template'
  AND value IN (CONCAT_WS('\n', 'Hello {{username}}!', 'You have requested to reset your password. Please click the following link to reset your password:', 'http://{{{resetLink}}}', 'If you did not request a password reset, please ignore this email.', '', 'Regards,', 'The CloudStack Team'), CONCAT_WS('\n', 'Hello {{username}}!', 'You have requested to reset your password. Please click the following link to reset your password:', '{{{domainUrl}}}{{{resetLink}}}', 'If you did not request a password reset, please ignore this email.', '', 'Regards,', 'The CloudStack Team'));
