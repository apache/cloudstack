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

-- Add columns max_backup and backup_interval_type to backup table
ALTER TABLE `cloud`.`backup_schedule` ADD COLUMN `max_backups` int(8) default NULL COMMENT 'maximum number of backups to maintain';
ALTER TABLE `cloud`.`backups` ADD COLUMN `backup_interval_type` int(5) COMMENT 'type of backup, e.g. manual, recurring - hourly, daily, weekly or monthly';

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

-- Rename user_vm_details to vm_instance_details
ALTER TABLE `cloud`.`user_vm_details` RENAME TO `cloud`.`vm_instance_details`;
ALTER TABLE `cloud`.`vm_instance_details` DROP FOREIGN KEY `fk_user_vm_details__vm_id`;
ALTER TABLE `cloud`.`vm_instance_details` ADD CONSTRAINT `fk_vm_instance_details__vm_id` FOREIGN KEY (vm_id) REFERENCES vm_instance(id) ON DELETE CASCADE;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_schedule', 'uuid', 'VARCHAR(40) NOT NULL');
UPDATE `cloud`.`backup_schedule` SET uuid = UUID();
