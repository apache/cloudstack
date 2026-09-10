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
-- Schema upgrade from 4.23.0.0 to 4.24.0.0
--;

-- VMware CBT warm migration session state
CREATE TABLE IF NOT EXISTS `cloud`.`vmware_cbt_migration` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40) NOT NULL COMMENT 'UUID',
    `zone_id` bigint unsigned NOT NULL COMMENT 'Zone ID',
    `account_id` bigint unsigned NOT NULL COMMENT 'Account ID',
    `user_id` bigint unsigned NOT NULL COMMENT 'User ID',
    `vm_id` bigint unsigned COMMENT 'Imported VM ID after cutover',
    `existing_vcenter_id` bigint unsigned COMMENT 'Linked VMware datacenter ID when the source vCenter is registered',
    `source_username` varchar(255) COMMENT 'Stored source vCenter username for external VMware CBT migrations',
    `source_password` varchar(1024) COMMENT 'Encrypted stored source vCenter password for external VMware CBT migrations',
    `destination_cluster_id` bigint unsigned NOT NULL COMMENT 'Destination KVM cluster ID',
    `convert_host_id` bigint unsigned COMMENT 'KVM host used for conversion and synchronization',
    `storage_pool_id` bigint unsigned COMMENT 'Destination primary storage pool ID',
    `display_name` varchar(255) COMMENT 'Target VM display name',
    `host_name` varchar(255) COMMENT 'Target VM host name',
    `template_id` bigint unsigned COMMENT 'Template ID for CloudStack VM import',
    `service_offering_id` bigint unsigned COMMENT 'Service offering ID for CloudStack VM import',
    `guest_os_id` bigint unsigned COMMENT 'Guest OS ID for CloudStack VM import',
    `data_disk_offering_map` text COMMENT 'JSON data disk to disk offering ID map for CloudStack VM import',
    `nic_network_map` text COMMENT 'JSON NIC to network ID map for CloudStack VM import',
    `nic_ip_address_map` text COMMENT 'JSON NIC to IPv4 address map for CloudStack VM import',
    `import_details` text COMMENT 'JSON details map for CloudStack VM import',
    `forced` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Whether duplicate NIC MAC addresses are allowed during CloudStack VM import',
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

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vmware_cbt_migration', 'host_name', 'varchar(255) COMMENT "Target VM host name"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vmware_cbt_migration', 'source_username', 'varchar(255) COMMENT "Stored source vCenter username for external VMware CBT migrations"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vmware_cbt_migration', 'source_password', 'varchar(1024) COMMENT "Encrypted stored source vCenter password for external VMware CBT migrations"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vmware_cbt_migration', 'template_id', 'bigint unsigned COMMENT "Template ID for CloudStack VM import"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vmware_cbt_migration', 'service_offering_id', 'bigint unsigned COMMENT "Service offering ID for CloudStack VM import"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vmware_cbt_migration', 'guest_os_id', 'bigint unsigned COMMENT "Guest OS ID for CloudStack VM import"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vmware_cbt_migration', 'data_disk_offering_map', 'text COMMENT "JSON data disk to disk offering ID map for CloudStack VM import"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vmware_cbt_migration', 'nic_network_map', 'text COMMENT "JSON NIC to network ID map for CloudStack VM import"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vmware_cbt_migration', 'nic_ip_address_map', 'text COMMENT "JSON NIC to IPv4 address map for CloudStack VM import"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vmware_cbt_migration', 'import_details', 'text COMMENT "JSON details map for CloudStack VM import"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vmware_cbt_migration', 'forced', 'tinyint(1) NOT NULL DEFAULT 0 COMMENT "Whether duplicate NIC MAC addresses are allowed during CloudStack VM import"');

INSERT INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `description`, `default_value`, `updated`, `scope`, `is_dynamic`)
VALUES ('Advanced', 'DEFAULT', 'VmwareCbtMigrationManagerImpl', 'vmware.cbt.allow.non.inplace.finalization', 'false',
        'If true, VMware CBT cutover may fall back to regular virt-v2v finalization for qcow2 file targets when true in-place finalization is unavailable. The fallback stages temporary data on the selected primary storage and requires additional free space.',
        'false', NOW(), 1, 1)
ON DUPLICATE KEY UPDATE `category` = VALUES(`category`), `component` = VALUES(`component`),
    `description` = VALUES(`description`), `default_value` = VALUES(`default_value`),
    `updated` = NOW(), `scope` = VALUES(`scope`), `is_dynamic` = VALUES(`is_dynamic`);

INSERT INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `description`, `default_value`, `updated`, `scope`, `is_dynamic`)
VALUES ('Advanced', 'DEFAULT', 'VmwareCbtMigrationManagerImpl', 'vmware.cbt.migration.agent.command.timeout', '86400',
        'Timeout in seconds for long-running VMware CBT data-plane commands dispatched to the KVM agent, including initial full sync, delta sync, final delta sync, and cutover finalization.',
        '86400', NOW(), 1, 1)
ON DUPLICATE KEY UPDATE `category` = VALUES(`category`), `component` = VALUES(`component`),
    `description` = VALUES(`description`), `default_value` = VALUES(`default_value`),
    `updated` = NOW(), `scope` = VALUES(`scope`), `is_dynamic` = VALUES(`is_dynamic`);

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
