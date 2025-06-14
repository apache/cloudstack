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

-- Add column max_backup to backup_schedule table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_schedule', 'max_backups', 'int(8) default NULL COMMENT "maximum number of backups to maintain"');

-- Add columns name, description and backup_interval_type to backup table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'name', 'VARCHAR(255) NULL COMMENT "name of the backup"');
UPDATE `cloud`.`backups` backup INNER JOIN `cloud`.`vm_instance` vm ON backup.vm_id = vm.id SET backup.name = vm.name;
ALTER TABLE `cloud`.`backups` MODIFY COLUMN `name` VARCHAR(255) NOT NULL;
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'description', 'VARCHAR(1024) COMMENT "description for the backup"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'backup_interval_type', 'int(5) COMMENT "type of backup, e.g. manual, recurring - hourly, daily, weekly or monthly"');

-- Make the column vm_id in backups table nullable to handle orphan backups
ALTER TABLE `cloud`.`backups` MODIFY COLUMN `vm_id` BIGINT UNSIGNED NULL;

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
