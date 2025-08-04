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
-- Schema upgrade from 4.20.1.0 to 4.21.0.0
--;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_schedule', 'max_backups', 'INT(8) UNSIGNED NOT NULL DEFAULT 0 COMMENT ''Maximum number of backups to be retained''');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'backup_schedule_id', 'BIGINT(20) UNSIGNED');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_schedule', 'quiescevm', 'tinyint(1) default NULL COMMENT "Quiesce VM before taking backup"');

-- Update default value for the config 'vm.network.nic.max.secondary.ipaddresses' (and value to default value if value is null)
UPDATE `cloud`.`configuration` SET default_value = '10' WHERE name = 'vm.network.nic.max.secondary.ipaddresses';
UPDATE `cloud`.`configuration` SET value = '10' WHERE name = 'vm.network.nic.max.secondary.ipaddresses' AND value IS NULL;

-- Add console_endpoint_creator_address column to cloud.console_session table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'console_endpoint_creator_address', 'VARCHAR(45)');

-- Add client_address column to cloud.console_session table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'client_address', 'VARCHAR(45)');

-- Allow default roles to use quotaCreditsList
INSERT INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'quotaCreditsList', permission, sort_order
FROM `cloud`.`role_permissions` rp
WHERE rp.rule = 'quotaStatement'
  AND NOT EXISTS(SELECT 1 FROM cloud.role_permissions rp_ WHERE rp.role_id = rp_.role_id AND rp_.rule = 'quotaCreditsList');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host', 'last_mgmt_server_id', 'bigint unsigned DEFAULT NULL COMMENT "last management server this host is connected to" AFTER `mgmt_server_id`');

-----------------------------------------------------------
-- CKS Enhancements:
-----------------------------------------------------------
-- Add for_cks column to the vm_template table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template','for_cks', 'int(1) unsigned DEFAULT "0" COMMENT "if true, the template can be used for CKS cluster deployment"');

-- Add support for different node types service offerings on CKS clusters
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','control_node_service_offering_id', 'bigint unsigned COMMENT "service offering ID for Control Node(s)"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','worker_node_service_offering_id', 'bigint unsigned COMMENT "service offering ID for Worker Node(s)"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','etcd_node_service_offering_id', 'bigint unsigned COMMENT "service offering ID for etcd Nodes"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','etcd_node_count', 'bigint unsigned COMMENT "number of etcd nodes to be deployed for the Kubernetes cluster"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','control_node_template_id', 'bigint unsigned COMMENT "template id to be used for Control Node(s)"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','worker_node_template_id', 'bigint unsigned COMMENT "template id to be used for Worker Node(s)"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','etcd_node_template_id', 'bigint unsigned COMMENT "template id to be used for etcd Nodes"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','cni_config_id', 'bigint unsigned COMMENT "user data id representing the associated cni configuration"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster','cni_config_details', 'varchar(4096) DEFAULT NULL COMMENT "user data details representing the values required for the cni configuration associated"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster_vm_map','etcd_node', 'tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT "indicates if the VM is an etcd node"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster_vm_map','external_node', 'tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT "indicates if the node was imported into the Kubernetes cluster"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster_vm_map','manual_upgrade', 'tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT "indicates if the node is marked for manual upgrade and excluded from the Kubernetes cluster upgrade operation"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster_vm_map','kubernetes_node_version', 'varchar(40) COMMENT "version of k8s the cluster node is on"');

ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__control_node_service_offering_id` FOREIGN KEY `fk_cluster__control_node_service_offering_id`(`control_node_service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__worker_node_service_offering_id` FOREIGN KEY `fk_cluster__worker_node_service_offering_id`(`worker_node_service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__etcd_node_service_offering_id` FOREIGN KEY `fk_cluster__etcd_node_service_offering_id`(`etcd_node_service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__control_node_template_id` FOREIGN KEY `fk_cluster__control_node_template_id`(`control_node_template_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__worker_node_template_id` FOREIGN KEY `fk_cluster__worker_node_template_id`(`worker_node_template_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__etcd_node_template_id` FOREIGN KEY `fk_cluster__etcd_node_template_id`(`etcd_node_template_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE;

-- Add for_cks column to the user_data table to represent CNI Configuration stored as userdata
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.user_data','for_cks', 'int(1) unsigned DEFAULT "0" COMMENT "if true, the user data represent CNI configuration meant for CKS use only"');

-- Add use VR IP as resolver option on VPC
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc','use_router_ip_resolver', 'tinyint(1) DEFAULT 0 COMMENT "use router ip as resolver instead of dns options"');
-----------------------------------------------------------
-- END - CKS Enhancements
-----------------------------------------------------------

-- Add table for reconcile commands
CREATE TABLE IF NOT EXISTS `cloud`.`reconcile_commands` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `management_server_id` bigint unsigned NOT NULL COMMENT 'node id of the management server',
    `host_id` bigint unsigned NOT NULL COMMENT 'id of the host',
    `request_sequence` bigint unsigned NOT NULL COMMENT 'sequence of the request',
    `resource_id` bigint unsigned DEFAULT NULL COMMENT 'id of the resource',
    `resource_type` varchar(255) COMMENT 'type if the resource',
    `state_by_management` varchar(255) COMMENT 'state of the command updated by management server',
    `state_by_agent` varchar(255) COMMENT 'state of the command updated by cloudstack agent',
    `command_name` varchar(255) COMMENT 'name of the command',
    `command_info` MEDIUMTEXT COMMENT 'info of the command',
    `answer_name` varchar(255) COMMENT 'name of the answer',
    `answer_info` MEDIUMTEXT COMMENT 'info of the answer',
    `created` datetime COMMENT 'date the reconcile command was created',
    `removed` datetime COMMENT 'date the reconcile command was removed',
    `updated` datetime COMMENT 'date the reconcile command was updated',
    `retry_count` bigint unsigned DEFAULT 0 COMMENT 'The retry count of reconciliation',
    PRIMARY KEY(`id`),
    INDEX `i_reconcile_command__host_id`(`host_id`),
    CONSTRAINT `fk_reconcile_command__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--- KVM Incremental Snapshots

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.snapshot_store_ref', 'kvm_checkpoint_path', 'varchar(255)');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.snapshot_store_ref', 'end_of_chain', 'int(1) unsigned');

-- Create table storage_pool_and_access_group_map
CREATE TABLE IF NOT EXISTS `cloud`.`storage_pool_and_access_group_map` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pool_id` bigint(20) unsigned NOT NULL COMMENT "pool id",
  `storage_access_group` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_storage_pool_and_access_group_map__pool_id` (`pool_id`),
  CONSTRAINT `fk_storage_pool_and_access_group_map__pool_id` FOREIGN KEY (`pool_id`) REFERENCES `storage_pool` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host', 'storage_access_groups', 'varchar(255) DEFAULT NULL COMMENT "storage access groups for the host"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.cluster', 'storage_access_groups', 'varchar(255) DEFAULT NULL COMMENT "storage access groups for the hosts in the cluster"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host_pod_ref', 'storage_access_groups', 'varchar(255) DEFAULT NULL COMMENT "storage access groups for the hosts in the pod"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.data_center', 'storage_access_groups', 'varchar(255) DEFAULT NULL COMMENT "storage access groups for the hosts in the zone"');

-- Add featured, sort_key, created, removed columns for guest_os_category
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os_category', 'featured', 'tinyint(1) NOT NULL DEFAULT 0 COMMENT "whether the category is featured or not" AFTER `uuid`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os_category', 'sort_key', 'int NOT NULL DEFAULT 0 COMMENT "sort key used for customising sort method" AFTER `featured`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os_category', 'created', 'datetime COMMENT "date on which the category was created" AFTER `sort_key`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os_category', 'removed', 'datetime COMMENT "date removed if not null" AFTER `created`');

-- Begin: Changes for Guest OS category cleanup
-- Add new OS categories if not present
DROP PROCEDURE IF EXISTS `cloud`.`INSERT_CATEGORY_IF_NOT_EXIST`;
CREATE PROCEDURE `cloud`.`INSERT_CATEGORY_IF_NOT_EXIST`(IN os_name VARCHAR(255))
BEGIN
    IF NOT EXISTS ((SELECT 1 FROM `cloud`.`guest_os_category` WHERE name = os_name))
    THEN
        INSERT INTO `cloud`.`guest_os_category` (name, uuid)
            VALUES (os_name, UUID())
;   END IF
; END;

CALL `cloud`.`INSERT_CATEGORY_IF_NOT_EXIST`('Fedora');
CALL `cloud`.`INSERT_CATEGORY_IF_NOT_EXIST`('Rocky Linux');
CALL `cloud`.`INSERT_CATEGORY_IF_NOT_EXIST`('AlmaLinux');

-- Move existing guest OS to new categories
DROP PROCEDURE IF EXISTS `cloud`.`UPDATE_CATEGORY_FOR_GUEST_OSES`;
CREATE PROCEDURE `cloud`.`UPDATE_CATEGORY_FOR_GUEST_OSES`(IN category_name VARCHAR(255), IN os_name VARCHAR(255))
BEGIN
    DECLARE category_id BIGINT
;   SELECT `id` INTO category_id
    FROM `cloud`.`guest_os_category`
    WHERE `name` = category_name
    LIMIT 1
;   IF category_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Category not found'
;   END IF
;   UPDATE `cloud`.`guest_os`
    SET `category_id` = category_id
    WHERE `display_name` LIKE CONCAT('%', os_name, '%')
; END;
CALL `cloud`.`UPDATE_CATEGORY_FOR_GUEST_OSES`('Rocky Linux', 'Rocky Linux');
CALL `cloud`.`UPDATE_CATEGORY_FOR_GUEST_OSES`('AlmaLinux', 'AlmaLinux');
CALL `cloud`.`UPDATE_CATEGORY_FOR_GUEST_OSES`('Fedora', 'Fedora');

-- Move existing guest OS whose category will be deleted to Other category
DROP PROCEDURE IF EXISTS `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`;
CREATE PROCEDURE `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`(IN to_category_name VARCHAR(255), IN from_category_name VARCHAR(255))
BEGIN
    DECLARE done INT DEFAULT 0
;   DECLARE to_category_id BIGINT
;   SELECT id INTO to_category_id
    FROM `cloud`.`guest_os_category`
    WHERE `name` = to_category_name
    LIMIT 1
;   IF to_category_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ToCategory not found'
;   END IF
;   UPDATE `cloud`.`guest_os`
    SET `category_id` = to_category_id
    WHERE `category_id` = (SELECT `id` FROM `cloud`.`guest_os_category` WHERE `name` = from_category_name)
;   UPDATE `cloud`.`guest_os_category` SET `removed`=now() WHERE `name` = from_category_name
; END;
CALL `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`('Other', 'Novel');
CALL `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`('Other', 'None');
CALL `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`('Other', 'Unix');
CALL `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`('Other', 'Mac');

-- Update featured for existing guest OS categories
UPDATE `cloud`.`guest_os_category` SET featured = 1;

-- Update sort order for all guest OS categories
UPDATE `cloud`.`guest_os_category`
SET `sort_key` = CASE
    WHEN `name` = 'Ubuntu' THEN 1
    WHEN `name` = 'Debian' THEN 2
    WHEN `name` = 'Fedora' THEN 3
    WHEN `name` = 'CentOS' THEN 4
    WHEN `name` = 'Rocky Linux' THEN 5
    WHEN `name` = 'AlmaLinux' THEN 6
    WHEN `name` = 'Oracle' THEN 7
    WHEN `name` = 'RedHat' THEN 8
    WHEN `name` = 'SUSE' THEN 9
    WHEN `name` = 'Windows' THEN 10
    WHEN `name` = 'Other' THEN 11
    ELSE `sort_key`
END;
-- End: Changes for Guest OS category cleanup

-- Update description for configuration: host.capacityType.to.order.clusters
UPDATE `cloud`.`configuration` SET
    `description` = 'The host capacity type (CPU, RAM or COMBINED) is used by deployment planner to order clusters during VM resource allocation'
WHERE `name` = 'host.capacityType.to.order.clusters'
  AND `description` = 'The host capacity type (CPU or RAM) is used by deployment planner to order clusters during VM resource allocation';

-- Whitelabel GUI
CREATE TABLE IF NOT EXISTS `cloud`.`gui_themes` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `uuid` varchar(255) UNIQUE,
    `name` varchar(2048) NOT NULL COMMENT 'A name to identify the theme.',
    `description` varchar(4096) DEFAULT NULL COMMENT 'A description for the theme.',
    `css` text DEFAULT NULL COMMENT 'The CSS to be retrieved and imported into the GUI when matching the theme access configurations.',
    `json_configuration` text DEFAULT NULL COMMENT 'The JSON with the configurations to be retrieved and imported into the GUI when matching the theme access configurations.',
    `recursive_domains` tinyint(1) DEFAULT 0 COMMENT 'Defines whether the subdomains of the informed domains are considered. Default value is false.',
    `is_public` tinyint(1) default 1 COMMENT 'Defines whether a theme can be retrieved by anyone when only the `internet_domains_names` is informed. If the `domain_uuids` or `account_uuids` is informed, it is considered as `false`.',
    `created` datetime NOT NULL,
    `removed` datetime DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `cloud`.`gui_themes_details` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `gui_theme_id` bigint(20) unsigned NOT NULL COMMENT 'Foreign key referencing the GUI theme on `gui_themes` table.',
    `type` varchar(100) NOT NULL COMMENT 'The type of GUI theme details. Valid options are: `account`, `domain` and `commonName`',
    `value` text NOT NULL COMMENT 'The value of the `type` details. Can be an UUID (account or domain) or internet common name.',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_gui_themes_details__gui_theme_id` FOREIGN KEY (`gui_theme_id`) REFERENCES `gui_themes`(`id`)
);

-- Create the GPU card table to hold the GPU card information
CREATE TABLE IF NOT EXISTS `cloud`.`gpu_card` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40) NOT NULL UNIQUE,
  `device_id` varchar(4) NOT NULL COMMENT 'device id of the GPU card',
  `device_name` varchar(255) NOT NULL COMMENT 'device name of the GPU card',
  `name` varchar(255) NOT NULL COMMENT 'name of the GPU card',
  `vendor_name` varchar(255) NOT NULL COMMENT 'vendor name of the GPU card',
  `vendor_id` varchar(4) NOT NULL COMMENT 'vendor id of the GPU card',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`vendor_id`, `device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GPU cards supported by CloudStack';

-- Create the vGPU profile table to hold the vGPU profile information.
CREATE TABLE IF NOT EXISTS `cloud`.`vgpu_profile` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL COMMENT 'name of the vGPU profile',
  `description` varchar(255) DEFAULT NULL COMMENT 'description of the vGPU profile',
  `card_id` bigint unsigned NOT NULL COMMENT 'id of the GPU card',
  `video_ram` bigint unsigned DEFAULT NULL COMMENT 'video RAM of the vGPU profile',
  `max_heads` bigint unsigned DEFAULT NULL COMMENT 'maximum number of heads of the vGPU profile',
  `max_resolution_x` bigint unsigned DEFAULT NULL COMMENT 'maximum resolution x of the vGPU profile',
  `max_resolution_y` bigint unsigned DEFAULT NULL COMMENT 'maximum resolution y of the vGPU profile',
  `max_vgpu_per_pgpu` bigint unsigned DEFAULT NULL COMMENT 'Maximum number of vGPUs per physical GPU',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`name`, `card_id`),
  CONSTRAINT `fk_vgpu_profile_card_id` FOREIGN KEY (`card_id`) REFERENCES `gpu_card`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='vGPU profiles supported by CloudStack';

-- Create the GPU device table to hold the GPU device information on different hosts
CREATE TABLE IF NOT EXISTS `cloud`.`gpu_device` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40) NOT NULL UNIQUE,
  `card_id` bigint unsigned NOT NULL COMMENT 'id of the GPU card',
  `vgpu_profile_id` bigint unsigned DEFAULT NULL COMMENT 'id of the vGPU profile.',
  `bus_address` varchar(255) NOT NULL COMMENT 'PCI bus address of the GPU device',
  `type` varchar(32) NOT NULL COMMENT 'type of the GPU device. PCI or MDEV',
  `host_id` bigint unsigned NOT NULL COMMENT 'id of the host where GPU is installed',
  `vm_id` bigint unsigned DEFAULT NULL COMMENT 'id of the VM using this GPU device',
  `numa_node` varchar(255) DEFAULT NULL COMMENT 'NUMA node of the GPU device',
  `pci_root` varchar(255) DEFAULT NULL COMMENT 'PCI root of the GPU device',
  `parent_gpu_device_id` bigint unsigned DEFAULT NULL COMMENT 'id of the parent GPU device. null if it is a physical GPU device and for vGPUs points to the actual GPU',
  `state` varchar(32) NOT NULL COMMENT 'state of the GPU device',
  `managed_state` varchar(32) NOT NULL COMMENT 'resource state of the GPU device',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`bus_address`, `host_id`),
  CONSTRAINT `fk_gpu_devices__card_id` FOREIGN KEY (`card_id`) REFERENCES `gpu_card` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_gpu_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_gpu_devices__vm_id` FOREIGN KEY (`vm_id`) REFERENCES `vm_instance` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_gpu_devices__parent_gpu_device_id` FOREIGN KEY (`parent_gpu_device_id`) REFERENCES `gpu_device` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GPU devices installed on hosts';

-- Add references to GPU tables
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.service_offering', 'vgpu_profile_id', 'bigint unsigned DEFAULT NULL COMMENT "vgpu profile ID"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.service_offering', 'gpu_count', 'int unsigned DEFAULT NULL COMMENT "number of GPUs"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.service_offering', 'gpu_display', 'boolean DEFAULT false COMMENT "enable GPU display"');
CALL `cloud`.`IDEMPOTENT_DROP_FOREIGN_KEY`('cloud.service_offering','fk_service_offering__vgpu_profile_id');
CALL `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`('cloud.service_offering', 'fk_service_offering__vgpu_profile_id', '(vgpu_profile_id)', '`vgpu_profile`(`id`)');

-- Netris Plugin
CREATE TABLE `cloud`.`netris_providers` (
                                            `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
                                            `uuid` varchar(40),
                                            `zone_id` bigint unsigned NOT NULL COMMENT 'Zone ID',
                                            `host_id` bigint unsigned NOT NULL COMMENT 'Host ID',
                                            `name` varchar(40),
                                            `url` varchar(255) NOT NULL,
                                            `username` varchar(255) NOT NULL,
                                            `password` varchar(255) NOT NULL,
                                            `site_name` varchar(255) NOT NULL,
                                            `tenant_name` varchar(255) NOT NULL,
                                            `netris_tag` varchar(255) NOT NULL,
                                            `created` datetime NOT NULL COMMENT 'created date',
                                            `removed` datetime COMMENT 'removed date if not null',
                                            PRIMARY KEY (`id`),
                                            CONSTRAINT `fk_netris_providers__zone_id` FOREIGN KEY `fk_netris_providers__zone_id` (`zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE,
                                            INDEX `i_netris_providers__zone_id`(`zone_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Drop the Tungsten and NSX columns from the network offerings (replaced by checking the provider on the ntwk_offering_service_map table)
ALTER TABLE `cloud`.`network_offerings` DROP COLUMN `for_tungsten`;
ALTER TABLE `cloud`.`network_offerings` DROP COLUMN `for_nsx`;

-- Drop the Tungsten and NSX columns from the VPC offerings (replaced by checking the provider on the vpc_offering_service_map table)
ALTER TABLE `cloud`.`vpc_offerings` DROP COLUMN `for_nsx`;

-- Add next_hop to the static_routes table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.static_routes', 'next_hop', 'varchar(50) COMMENT "next hop of the static route" AFTER `vpc_gateway_id`');

-- Add `for_router` to `user_ip_address` table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.user_ip_address', 'for_router', 'tinyint(1) DEFAULT 0 COMMENT "True if the ip address is used by Domain Router to expose services"');

-- Add Netris Autoscaling rules
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'Netris', 'cpu', 'VM CPU - average percentage', 'vm.cpu.average.percentage', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'Netris', 'memory', 'VM Memory - average percentage', 'vm.memory.average.percentage', NOW());

-- Rename user_vm_details to vm_instance_details
ALTER TABLE `cloud`.`user_vm_details` RENAME TO `cloud`.`vm_instance_details`;
ALTER TABLE `cloud`.`vm_instance_details` DROP FOREIGN KEY `fk_user_vm_details__vm_id`;
ALTER TABLE `cloud`.`vm_instance_details` ADD CONSTRAINT `fk_vm_instance_details__vm_id` FOREIGN KEY (vm_id) REFERENCES vm_instance(id) ON DELETE CASCADE;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_schedule', 'uuid', 'VARCHAR(40) NOT NULL');
UPDATE `cloud`.`backup_schedule` SET uuid = UUID();

-- Extension framework
UPDATE `cloud`.`configuration` SET value = CONCAT(value, ',External') WHERE name = 'hypervisor.list';

CREATE TABLE IF NOT EXISTS `cloud`.`extension` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096),
  `type` varchar(255) NOT NULL COMMENT 'Type of the extension: Orchestrator, etc',
  `relative_path` varchar(2048) NOT NULL COMMENT 'Path for the extension relative to the root extensions directory',
  `path_ready` tinyint(1) DEFAULT '0' COMMENT 'True if the extension path is in ready state across management servers',
  `is_user_defined` tinyint(1) DEFAULT '0' COMMENT 'True if the extension is added by admin',
  `state` char(32) NOT NULL COMMENT 'State of the extension - Enabled or Disabled',
  `created` datetime NOT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`extension_details` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `extension_id` bigint unsigned NOT NULL COMMENT 'extension to which the detail is related to',
  `name` varchar(255) NOT NULL COMMENT 'name of the detail',
  `value` varchar(255) NOT NULL COMMENT 'value of the detail',
  `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_extension_details__extension_id` FOREIGN KEY (`extension_id`)
    REFERENCES `extension` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`extension_resource_map` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `extension_id` bigint(20) unsigned NOT NULL,
  `resource_id` bigint(20) unsigned NOT NULL,
  `resource_type` char(255) NOT NULL,
  `created` datetime NOT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_extension_resource_map__extension_id` FOREIGN KEY (`extension_id`)
      REFERENCES `cloud`.`extension`(`id`) ON DELETE CASCADE,
  INDEX `idx_extension_resource` (`resource_id`, `resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`extension_resource_map_details` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `extension_resource_map_id` bigint unsigned NOT NULL COMMENT 'mapping to which the detail is related',
  `name` varchar(255) NOT NULL COMMENT 'name of the detail',
  `value` varchar(255) NOT NULL COMMENT 'value of the detail',
  `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_extension_resource_map_details__map_id` FOREIGN KEY (`extension_resource_map_id`)
    REFERENCES `extension_resource_map` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`extension_custom_action` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096),
  `extension_id` bigint(20) unsigned NOT NULL,
  `resource_type` varchar(255),
  `allowed_role_types` int unsigned NOT NULL DEFAULT '1',
  `success_message` varchar(4096),
  `error_message` varchar(4096),
  `enabled` boolean DEFAULT true,
  `timeout` int unsigned NOT NULL DEFAULT '5' COMMENT 'The timeout in seconds to wait for the action to complete before failing',
  `created` datetime NOT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_extension_custom_action__extension_id` FOREIGN KEY (`extension_id`)
    REFERENCES `cloud`.`extension`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`extension_custom_action_details` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `extension_custom_action_id` bigint(20) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `value` TEXT NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_custom_action_details__action_id` FOREIGN KEY (`extension_custom_action_id`)
    REFERENCES `cloud`.`extension_custom_action`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template', 'extension_id', 'bigint unsigned DEFAULT NULL COMMENT "id of the extension"');

-- Add built-in Extensions and Custom Actions

DROP PROCEDURE IF EXISTS `cloud`.`INSERT_EXTENSION_IF_NOT_EXISTS`;
CREATE PROCEDURE `cloud`.`INSERT_EXTENSION_IF_NOT_EXISTS`(
    IN ext_name VARCHAR(255),
    IN ext_desc VARCHAR(255),
    IN ext_path VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM `cloud`.`extension` WHERE `name` = ext_name
    ) THEN
        INSERT INTO `cloud`.`extension` (
            `uuid`, `name`, `description`, `type`,
            `relative_path`, `path_ready`,
            `is_user_defined`, `state`, `created`, `removed`
        )
        VALUES (
            UUID(), ext_name, ext_desc, 'Orchestrator',
            ext_path, 1, 0, 'Enabled', NOW(), NULL
        )
;   END IF
;END;

DROP PROCEDURE IF EXISTS `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`;
CREATE PROCEDURE `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`(
    IN ext_name VARCHAR(255),
    IN detail_key VARCHAR(255),
    IN detail_value TEXT,
    IN display TINYINT(1)
)
BEGIN
    DECLARE ext_id BIGINT
;   SELECT `id` INTO ext_id FROM `cloud`.`extension` WHERE `name` = ext_name LIMIT 1
;   IF NOT EXISTS (
        SELECT 1 FROM `cloud`.`extension_details`
        WHERE `extension_id` = ext_id AND `name` = detail_key
    ) THEN
        INSERT INTO `cloud`.`extension_details` (
            `extension_id`, `name`, `value`, `display`
        )
        VALUES (
            ext_id, detail_key, detail_value, display
        )
;   END IF
;END;

CALL `cloud`.`INSERT_EXTENSION_IF_NOT_EXISTS`('Proxmox', 'Sample extension for Proxmox written in bash', 'Proxmox/proxmox.sh');
CALL `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`('Proxmox', 'orchestratorrequirespreparevm', 'true', 0);

CALL `cloud`.`INSERT_EXTENSION_IF_NOT_EXISTS`('HyperV', 'Sample extension for HyperV written in python', 'HyperV/hyperv.py');

DROP PROCEDURE IF EXISTS `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`;
CREATE PROCEDURE `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`(
    IN ext_name VARCHAR(255),
    IN action_name VARCHAR(255),
    IN action_desc VARCHAR(4096),
    IN resource_type VARCHAR(255),
    IN allowed_roles INT UNSIGNED,
    IN success_msg VARCHAR(4096),
    IN error_msg VARCHAR(4096),
    IN timeout_seconds INT UNSIGNED
)
BEGIN
    DECLARE ext_id BIGINT
;   SELECT `id` INTO ext_id FROM `cloud`.`extension` WHERE `name` = ext_name LIMIT 1
;   IF NOT EXISTS (
        SELECT 1 FROM `cloud`.`extension_custom_action` WHERE `name` = action_name AND `extension_id` = ext_id
    ) THEN
        INSERT INTO `cloud`.`extension_custom_action` (
            `uuid`, `name`, `description`, `extension_id`, `resource_type`,
            `allowed_role_types`, `success_message`, `error_message`,
            `enabled`, `timeout`, `created`, `removed`
        )
        VALUES (
            UUID(), action_name, action_desc, ext_id, resource_type,
            allowed_roles, success_msg, error_msg,
            1, timeout_seconds, NOW(), NULL
        )
;   END IF
;END;

DROP PROCEDURE IF EXISTS `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS`;
CREATE PROCEDURE `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS` (
    IN ext_name VARCHAR(255),
    IN action_name VARCHAR(255),
    IN param_json TEXT
)
BEGIN
    DECLARE action_id BIGINT UNSIGNED
;   SELECT `eca`.`id` INTO action_id FROM `cloud`.`extension_custom_action` `eca`
    JOIN `cloud`.`extension` `e` ON `e`.`id` = `eca`.`extension_id`
    WHERE `eca`.`name` = action_name AND `e`.`name` = ext_name LIMIT 1
;   IF NOT EXISTS (
        SELECT 1 FROM `cloud`.`extension_custom_action_details`
        WHERE `extension_custom_action_id` = action_id
          AND `name` = 'parameters'
    ) THEN
        INSERT INTO `cloud`.`extension_custom_action_details` (
            `extension_custom_action_id`,
            `name`,
            `value`,
            `display`
        ) VALUES (
            action_id,
            'parameters',
            param_json,
            0
        )
;   END IF
;END;

CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`('Proxmox', 'ListSnapshots', 'List Instance snapshots', 'VirtualMachine', 15, 'Snapshots fetched for {{resourceName}} in {{extensionName}}', 'List Snapshots failed for {{resourceName}}', 60);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`('Proxmox', 'CreateSnapshot', 'Create an Instance snapshot', 'VirtualMachine', 15, 'Snapshot created for {{resourceName}} in {{extensionName}}', 'Snapshot creation failed for {{resourceName}}', 60);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`('Proxmox', 'RestoreSnapshot', 'Restore Instance to the specific snapshot', 'VirtualMachine', 15, 'Successfully restored snapshot for {{resourceName}} in {{extensionName}}', 'Restore snapshot failed for {{resourceName}}', 60);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`('Proxmox', 'DeleteSnapshot', 'Delete the specified snapshot', 'VirtualMachine', 15, 'Successfully deleted snapshot for {{resourceName}} in {{extensionName}}', 'Delete snapshot failed for {{resourceName}}', 60);

CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS`(
    'Proxmox',
    'ListSnapshots',
    '[]'
);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS`(
    'Proxmox',
    'CreateSnapshot',
    '[
      {
        "name": "snap_name",
        "type": "STRING",
        "validationformat": "NONE",
        "required": true
      },
      {
        "name": "snap_description",
        "type": "STRING",
        "validationformat": "NONE",
        "required": false
      },
      {
        "name": "snap_save_memory",
        "type": "BOOLEAN",
        "validationformat": "NONE",
        "required": false
      }
    ]'
);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS`(
    'Proxmox',
    'RestoreSnapshot',
    '[
      {
        "name": "snap_name",
        "type": "STRING",
        "validationformat": "NONE",
        "required": true
      }
    ]'
);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS`(
    'Proxmox',
    'DeleteSnapshot',
    '[
      {
        "name": "snap_name",
        "type": "STRING",
        "validationformat": "NONE",
        "required": true
      }
    ]'
);

CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`('HyperV', 'ListSnapshots', 'List checkpoints/snapshots for the Instance', 'VirtualMachine', 15, 'Snapshots fetched for {{resourceName}} in {{extensionName}}', 'List Snapshots failed for {{resourceName}}', 60);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`('HyperV', 'CreateSnapshot', 'Create a checkpoint/snapshot for the Instance', 'VirtualMachine', 15, 'Snapshot created for {{resourceName}} in {{extensionName}}', 'Snapshot creation failed for {{resourceName}}', 60);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`('HyperV', 'RestoreSnapshot', 'Restore Instance to the specified snapshot', 'VirtualMachine', 15, 'Successfully restored snapshot for {{resourceName}} in {{extensionName}}', 'Restore snapshot failed for {{resourceName}}', 60);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`('HyperV', 'DeleteSnapshot', 'Delete the specified snapshot', 'VirtualMachine', 15, 'Successfully deleted snapshot for {{resourceName}} in {{extensionName}}', 'Delete snapshot failed for {{resourceName}}', 60);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`('HyperV', 'Suspend', 'Suspend the Instance by freezing its current state in RAM', 'VirtualMachine', 15, 'Successfully suspended {{resourceName}} in {{extensionName}}', 'Suspend failed for {{resourceName}}', 60);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`('HyperV', 'Resume', 'Resumes a suspended Instance, restoring CPU execution from memory.', 'VirtualMachine', 15, 'Successfully resumed {{resourceName}} in {{extensionName}}', 'Resume failed for {{resourceName}}', 60);

CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS`(
    'HyperV',
    'ListSnapshots',
    '[]'
);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS`(
    'HyperV',
    'CreateSnapshot',
    '[
      {
        "name": "snapshot_name",
        "type": "STRING",
        "validationformat": "NONE",
        "required": true
      }
    ]'
);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS`(
    'HyperV',
    'RestoreSnapshot',
    '[
      {
        "name": "snapshot_name",
        "type": "STRING",
        "validationformat": "NONE",
        "required": true
      }
    ]'
);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS`(
    'HyperV',
    'DeleteSnapshot',
    '[
      {
        "name": "snapshot_name",
        "type": "STRING",
        "validationformat": "NONE",
        "required": true
      }
    ]'
);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS`(
    'HyperV',
    'Suspend',
    '[]'
);
CALL `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS`(
    'HyperV',
    'Resume',
    '[]'
);

ALTER TABLE `cloud`.`networks` MODIFY COLUMN `cidr` varchar(255) DEFAULT NULL COMMENT 'CloudStack managed vms get IP address from cidr.In general this cidr also serves as the network CIDR. But in case IP reservation feature is being used by a Guest network, networkcidr is the Effective network CIDR for that network';
ALTER TABLE `cloud`.`networks` MODIFY COLUMN `gateway` varchar(255) DEFAULT NULL COMMENT 'gateway(s) for this network configuration';
ALTER TABLE `cloud`.`networks` MODIFY COLUMN `ip6_cidr` varchar(1024) DEFAULT NULL COMMENT 'IPv6 cidr(s) for this network';
ALTER TABLE `cloud`.`networks` MODIFY COLUMN `ip6_gateway` varchar(1024) DEFAULT NULL COMMENT 'IPv6 gateway(s) for this network';

-- Add columns name, description and backup_interval_type to backup table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'name', 'VARCHAR(255) NULL COMMENT "name of the backup"');
UPDATE `cloud`.`backups` backup INNER JOIN `cloud`.`vm_instance` vm ON backup.vm_id = vm.id SET backup.name = vm.name;
ALTER TABLE `cloud`.`backups` MODIFY COLUMN `name` VARCHAR(255) NOT NULL;
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'description', 'VARCHAR(1024) COMMENT "description for the backup"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'backup_interval_type', 'int(5) COMMENT "type of backup, e.g. manual, recurring - hourly, daily, weekly or monthly"');

-- Create backup details table
CREATE TABLE IF NOT EXISTS `cloud`.`backup_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `backup_id` bigint unsigned NOT NULL COMMENT 'backup id',
  `name` varchar(255) NOT NULL,
  `value` TEXT NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should detail be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_backup_details__backup_id` FOREIGN KEY `fk_backup_details__backup_id`(`backup_id`) REFERENCES `backups`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add diskOfferingId, deviceId, minIops and maxIops to backed_volumes in backups table
UPDATE `cloud`.`backups` b
INNER JOIN `cloud`.`vm_instance` vm ON b.vm_id = vm.id
SET b.backed_volumes = (
    SELECT CONCAT("[",
        GROUP_CONCAT(
            CONCAT(
                "{\"uuid\":\"", v.uuid, "\",",
                "\"type\":\"", v.volume_type, "\",",
                "\"size\":", v.`size`, ",",
                "\"path\":\"", IFNULL(v.path, 'null'), "\",",
                "\"deviceId\":", IFNULL(v.device_id, 'null'), ",",
                "\"diskOfferingId\":\"", doff.uuid, "\",",
                "\"minIops\":", IFNULL(v.min_iops, 'null'), ",",
                "\"maxIops\":", IFNULL(v.max_iops, 'null'),
                "}"
            )
            SEPARATOR ","
        ),
    "]")
    FROM `cloud`.`volumes` v
    LEFT JOIN `cloud`.`disk_offering` doff ON v.disk_offering_id = doff.id
    WHERE v.instance_id = vm.id
);

-- Add diskOfferingId, deviceId, minIops and maxIops to backup_volumes in vm_instance table
UPDATE `cloud`.`vm_instance` vm
SET vm.backup_volumes = (
    SELECT CONCAT("[",
        GROUP_CONCAT(
            CONCAT(
                "{\"uuid\":\"", v.uuid, "\",",
                "\"type\":\"", v.volume_type, "\",",
                "\"size\":", v.`size`, ",",
                "\"path\":\"", IFNULL(v.path, 'null'), "\",",
                "\"deviceId\":", IFNULL(v.device_id, 'null'), ",",
                "\"diskOfferingId\":\"", doff.uuid, "\",",
                "\"minIops\":", IFNULL(v.min_iops, 'null'), ",",
                "\"maxIops\":", IFNULL(v.max_iops, 'null'),
                "}"
            )
            SEPARATOR ","
        ),
    "]")
    FROM `cloud`.`volumes` v
    LEFT JOIN `cloud`.`disk_offering` doff ON v.disk_offering_id = doff.id
    WHERE v.instance_id = vm.id
)
WHERE vm.backup_offering_id IS NOT NULL;

-- Add column allocated_size to object_store table. Rename column 'used_bytes' to 'used_size'
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.object_store', 'allocated_size', 'bigint unsigned COMMENT "allocated size in bytes"');
ALTER TABLE `cloud`.`object_store` CHANGE COLUMN `used_bytes` `used_size` BIGINT UNSIGNED COMMENT 'used size in bytes';
ALTER TABLE `cloud`.`object_store` MODIFY COLUMN `total_size` bigint unsigned COMMENT 'total size in bytes';
UPDATE `cloud`.`object_store`
JOIN (
    SELECT object_store_id, SUM(quota) AS total_quota
    FROM `cloud`.`bucket`
    WHERE removed IS NULL
    GROUP BY object_store_id
) buckets_quota_sum_view ON `object_store`.id = buckets_quota_sum_view.object_store_id
SET `object_store`.allocated_size = buckets_quota_sum_view.total_quota;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'domain_id', 'bigint(20) unsigned NOT NULL');

UPDATE `cloud`.`console_session` `cs`
SET `cs`.`domain_id` = (
    SELECT `acc`.`domain_id`
    FROM `cloud`.`account` `acc`
    WHERE `acc`.`id` = `cs`.`account_id`
);
