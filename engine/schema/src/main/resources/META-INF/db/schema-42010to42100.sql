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

-- Extension framework
UPDATE `cloud`.`configuration` SET value = CONCAT(value, ',External') WHERE name = 'hypervisor.list';

CREATE TABLE IF NOT EXISTS `cloud`.`extension` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096),
  `type` varchar(255) NOT NULL COMMENT 'Type of the extension: Orchestrator, etc',
  `relative_entry_point` varchar(2048) NOT NULL COMMENT 'Path of entry point for the extension relative to the root extensions directory',
  `entry_point_ready` tinyint(1) DEFAULT '0' COMMENT 'True if the extension entry point is in ready state across management servers',
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
  `timeout` int unsigned NOT NULL DEFAULT '3' COMMENT 'The timeout in seconds to wait for the action to complete before failing',
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
  `value` varchar(255) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_custom_action_details__action_id` FOREIGN KEY (`extension_custom_action_id`)
    REFERENCES `cloud`.`extension_custom_action`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template', 'extension_id', 'bigint unsigned DEFAULT NULL COMMENT "id of the extension"');

-- Add built-in extensions

DROP PROCEDURE IF EXISTS `cloud`.`INSERT_EXTENSION_IF_NOT_EXISTS`;
CREATE PROCEDURE `cloud`.`INSERT_EXTENSION_IF_NOT_EXISTS`(
    IN ext_name VARCHAR(255),
    IN ext_desc VARCHAR(255),
    IN entry_point VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM extension WHERE name = ext_name
    ) THEN
        INSERT INTO extension (
            uuid, name, description, type,
            relative_entry_point, entry_point_sync,
            is_user_defined, state, created, removed
        )
        VALUES (
            UUID(), ext_name, ext_desc, 'Orchestrator',
            entry_point, 1, 0, 'Enabled', NOW(), NULL
        )
    ;END IF
;END;

DROP PROCEDURE IF EXISTS `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`;
CREATE PROCEDURE `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`(
    IN ext_name VARCHAR(255),
    IN detail_key VARCHAR(255),
    IN detail_value TEXT,
    IN display_detail TINYINT(1)
)
BEGIN
    DECLARE ext_id BIGINT
;   SELECT id INTO ext_id FROM extension WHERE name = ext_name LIMIT 1
;   IF NOT EXISTS (
        SELECT 1 FROM extension_details
        WHERE extension_id = ext_id AND name = detail_key
    ) THEN
        INSERT INTO extension_details (
            extension_id, name, value, display
        )
        VALUES (
            ext_id, detail_key, detail_value, display_detail
        )
    ;END IF
;END;

CALL `cloud`.`INSERT_EXTENSION_IF_NOT_EXISTS`('org.apache.cloudstack.extension.Proxmox', 'Sample extension for Proxmox written in bash', 'Proxmox/proxmox.sh');
CALL `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`('org.apache.cloudstack.extension.Proxmox', 'url', '', 1);
CALL `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`('org.apache.cloudstack.extension.Proxmox', 'user', '', 1);
CALL `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`('org.apache.cloudstack.extension.Proxmox', 'token', '', 1);
CALL `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`('org.apache.cloudstack.extension.Proxmox', 'secret', '', 0);

CALL `cloud`.`INSERT_EXTENSION_IF_NOT_EXISTS`('org.apache.cloudstack.extension.HyperV', 'Sample extension for HyperV written in python', 'HyperV/hyperv.py');
CALL `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`('org.apache.cloudstack.extension.HyperV', 'url', '', 1);
CALL `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`('org.apache.cloudstack.extension.HyperV', 'username', '', 1);
CALL `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`('org.apache.cloudstack.extension.HyperV', 'password', '', 0);
