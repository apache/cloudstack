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

UPDATE `cloud`.`configuration` SET value = CONCAT(value, ',External') WHERE name = 'hypervisor.list';

CREATE TABLE IF NOT EXISTS `cloud`.`extension` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `script` varchar(2048) NOT NULL,
  `created` datetime NOT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`extension_details` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `extension_id` bigint unsigned NOT NULL COMMENT 'extension to which the detail is related to',
  `name` varchar(255) NOT NULL COMMENT 'name of the detail',
  `value` varchar(255) NOT NULL COMMENT 'value of the detail',
  `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_extension_details__extension_id` FOREIGN KEY (`extension_id`)
    REFERENCES `extension` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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

-- Extension framework
CREATE TABLE IF NOT EXISTS `cloud`.`extension_resource_map` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `extension_id` bigint(20) unsigned NOT NULL,
  `resource_id` bigint(20) unsigned NOT NULL,
  `resource_type` varchar(255) NOT NULL,
  `created` datetime NOT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_extension_resource_map__extension_id` FOREIGN KEY (`extension_id`)
      REFERENCES `cloud`.`extension`(`id`) ON DELETE CASCADE,
  INDEX `idx_extension_resource` (`resource_id`, `resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`extension_resource_map_details` (
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
  `description` varchar(255) NOT NULL,
  `extension_id` bigint(20) unsigned NOT NULL,
  `roles_list` varchar(255) DEFAULT NULL,
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
