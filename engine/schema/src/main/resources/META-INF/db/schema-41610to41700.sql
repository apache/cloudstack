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
-- Schema upgrade from 4.16.1.0 to 4.17.0.0
--;

-- PR#5668 Change the type of the 'ipsec_psk' field to allow large PSK.
ALTER TABLE cloud.remote_access_vpn MODIFY ipsec_psk text NOT NULL;


-- PR#5832 Fix 'endpointe.url' global settings configuration typo.
UPDATE `cloud`.`configuration` SET name='endpoint.url' WHERE name='endpointe.url';

ALTER TABLE `cloud`.`service_offering` ADD COLUMN `uuid` varchar(40) UNIQUE DEFAULT NULL;
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `name` varchar(255) NOT NULL;
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `display_text` varchar(4096) DEFAULT NULL ;
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `unique_name` varchar(32) DEFAULT NULL COMMENT 'unique name for system offerings';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `customized` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT '0 implies not customized by default';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `created` datetime DEFAULT NULL COMMENT 'date when service offering was created';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `removed` datetime DEFAULT NULL COMMENT 'date when service offering was removed';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `state` CHAR(40) NOT NULL DEFAULT 'Active' COMMENT 'state of service offering either Active or Inactive';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `disk_offering_id` bigint unsigned;
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `system_use` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'is this offering for system used only';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `disk_offering_strictness` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT 'strict binding with disk offering or not';
ALTER TABLE `cloud`.`service_offering` ADD CONSTRAINT `fk_service_offering__disk_offering_id` FOREIGN KEY `fk_service_offering__disk_offering_id`(`disk_offering_id`) REFERENCES `disk_offering`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`service_offering` DROP FOREIGN KEY `fk_service_offering__id`;

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `disk_size_strictness` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT 'To allow or disallow the resize operation on the disks created from this offering';
ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `compute_only` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT 'when set to 1, disk offering has one to one binding with service offering';

ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `disk_offering_id`;

UPDATE `cloud`.`service_offering` so, `cloud`.`disk_offering` do SET    so.`uuid` = do.`uuid`,
                                                                        so.`name` = do.`name`,
                                                                        so.`display_text` = do.`display_text`,
                                                                        so.`unique_name` = do.`unique_name`,
                                                                        so.`customized` = do.`customized`,
                                                                        so.`created` = do.`created`,
                                                                        so.`removed` = do.`removed`,
                                                                        so.`state` = do.`state`,
                                                                        so.`disk_offering_id` = do.`id`,
                                                                        so.`system_use` = do.`system_use` WHERE so.`id` = do.`id`;

UPDATE `cloud`.`disk_offering` SET `compute_only` = 1 where `type` = 'Service';
UPDATE `cloud`.`disk_offering` SET `disk_size_strictness` = 1 WHERE `compute_only` = 1 AND `disk_size` != 0;

ALTER TABLE `cloud`.`disk_offering` DROP COLUMN `type`;
ALTER TABLE `cloud`.`disk_offering` DROP COLUMN `system_use`;

--;
-- Stored procedure to do idempotent column add;
-- This is copied from schema-41000to41100.sql
--;
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_COLUMN`;

CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_COLUMN` (
    IN in_table_name VARCHAR(200),
    IN in_column_name VARCHAR(200),
    IN in_column_definition VARCHAR(1000)
)
BEGIN

    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes','external_uuid', 'VARCHAR(40) DEFAULT null ');

INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) SELECT UUID(), 3, 'listConfigurations', 'ALLOW', (SELECT MAX(`sort_order`)+1 FROM `cloud`.`role_permissions`) ON DUPLICATE KEY UPDATE rule=rule;
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) SELECT UUID(), 3, 'updateConfiguration', 'ALLOW', (SELECT MAX(`sort_order`)+1 FROM `cloud`.`role_permissions`) ON DUPLICATE KEY UPDATE rule=rule;

-- table for network permissions
CREATE TABLE  `cloud`.`network_permissions` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`),
  INDEX `i_network_permission_network_id`(`network_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `cloud`.`user_vm_details`(`vm_id`, `name`, `value`)
    SELECT `user_vm_details`.`vm_id`, 'SSH.KeyPairNames', `ssh_keypairs`.`keypair_name`
        FROM `cloud`.`user_vm_details`
        INNER JOIN `cloud`.`ssh_keypairs` ON ssh_keypairs.public_key = user_vm_details.value
        INNER JOIN `cloud`.`vm_instance` ON vm_instance.id = user_vm_details.vm_id
        WHERE ssh_keypairs.account_id = vm_instance.account_id;

ALTER TABLE `cloud`.`kubernetes_cluster` ADD COLUMN `security_group_id` bigint unsigned DEFAULT NULL,
ADD CONSTRAINT `fk_kubernetes_cluster__security_group_id` FOREIGN KEY `fk_kubernetes_cluster__security_group_id`(`security_group_id`) REFERENCES `security_group`(`id`) ON DELETE CASCADE;

-- PR#5984 Create table to persist VM stats.
DROP TABLE IF EXISTS `cloud`.`vm_stats`;
CREATE TABLE `cloud`.`vm_stats` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL,
  `mgmt_server_id` bigint unsigned NOT NULL,
  `timestamp` datetime NOT NULL,
  `vm_stats_data` text NOT NULL,
  PRIMARY KEY (`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- PR#5984 Update name for global configuration vm.stats.increment.metrics
UPDATE `cloud`.`configuration` SET name = 'vm.stats.increment.metrics' WHERE name = 'vm.stats.increment.metrics.in.memory';

ALTER TABLE `cloud`.`domain_router` ADD COLUMN `software_version` varchar(100) COMMENT 'Software version';

-- For IPv6 guest prefixes.
CREATE TABLE `cloud`.`dc_ip6_guest_prefix` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) DEFAULT NULL,
  `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'zone it belongs to',
  `prefix` varchar(255) NOT NULL COMMENT 'prefix of the ipv6 network',
  `created` datetime default NULL,
  `removed` datetime default NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_dc_ip6_guest_prefix__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`),
  CONSTRAINT `uc_dc_ip6_guest_prefix__uuid` UNIQUE (`uuid`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ip6_guest_prefix_subnet_network_map` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) DEFAULT NULL,
  `prefix_id` bigint(20) unsigned NOT NULL COMMENT 'ip6 guest prefix to which subnet belongs to',
  `subnet` varchar(255) NOT NULL COMMENT 'subnet of the ipv6 network',
  `network_id` bigint(20) unsigned DEFAULT NULL COMMENT 'network to which subnet is associated to',
  `state` varchar(255) NOT NULL COMMENT 'state of the subnet network',
  `updated` datetime default NULL,
  `created` datetime default NULL,
  `removed` datetime default NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_ip6_guest_prefix_subnet_network_map__prefix_id` FOREIGN KEY (`prefix_id`) REFERENCES `dc_ip6_guest_prefix`(`id`),
  CONSTRAINT `fk_ip6_guest_prefix_subnet_network_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`),
  CONSTRAINT `uc_ip6_guest_prefix_subnet_network_map__uuid` UNIQUE (`uuid`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Allow storing IPv6 CIDRs
ALTER TABLE `cloud`.`firewall_rules_cidrs` MODIFY COLUMN `source_cidr` varchar(43) DEFAULT NULL;
ALTER TABLE `cloud`.`firewall_rules_dcidrs` MODIFY COLUMN `destination_cidr` varchar(43) DEFAULT NULL;

--
-- Management Server Status
--
ALTER TABLE `cloud`.`mshost` ADD CONSTRAINT `mshost_UUID` UNIQUE KEY (`uuid`);
CREATE TABLE `cloud`.`mshost_status` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `ms_id` varchar(40) DEFAULT NULL COMMENT 'the uuid of the management server record',
  `last_jvm_start` datetime DEFAULT NULL COMMENT 'the last start time for this MS',
  `last_jvm_stop` datetime DEFAULT NULL COMMENT 'the last stop time for this MS',
  `last_system_boot` datetime DEFAULT NULL COMMENT 'the last system boot time for the host of this MS',
  `os_distribution` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'the name of the os type running on the host of this MS',
  `java_name` varchar(64) DEFAULT NULL COMMENT 'the name of the java distribution running this MS',
  `java_version` varchar(64) DEFAULT NULL COMMENT 'the version of the java distribution running this MS',
  `updated` datetime DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_ms_id` UNIQUE (`ms_id`),
  CONSTRAINT `mshost_status_FK` FOREIGN KEY (`ms_id`) REFERENCES `mshost` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3;

-- Alter event table to add resource_id and resource_type
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.event','resource_id', 'bigint unsigned COMMENT "ID of the resource associated with the event" AFTER `domain_id`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.event','resource_type', 'VARCHAR(32) COMMENT "Type of the resource associated with the event" AFTER `resource_id`');

-- Add XenServer 8.2.1 hypervisor capabilities
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported) VALUES (UUID(), 'XenServer', '8.2.1', 1000, 253, 64, 1);

-- Copy XenServer 8.2.0 hypervisor guest OS mappings to XenServer 8.2.1
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '8.2.1', guest_os_name, guest_os_id, utc_timestamp(), 0 FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='8.2.0';

DROP PROCEDURE IF EXISTS `cloud`.`ADD_GUEST_OS_AND_HYPERVISOR_MAPPING`;
CREATE PROCEDURE `cloud`.`ADD_GUEST_OS_AND_HYPERVISOR_MAPPING` (
    IN guest_os_category_id bigint(20) unsigned,
    IN guest_os_display_name VARCHAR(255),
    IN guest_os_hypervisor_hypervisor_type VARCHAR(32),
    IN guest_os_hypervisor_hypervisor_version VARCHAR(32),
    IN guest_os_hypervisor_guest_os_name VARCHAR(255)
        )
BEGIN
INSERT  INTO cloud.guest_os (uuid, category_id, display_name, created)
SELECT 	UUID(), guest_os_category_id, guest_os_display_name, now()
FROM    DUAL
WHERE 	not exists( SELECT  1
                     FROM    cloud.guest_os
                     WHERE   cloud.guest_os.category_id = guest_os_category_id
                       AND     cloud.guest_os.display_name = guest_os_display_name)

;	INSERT  INTO cloud.guest_os_hypervisor (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created)
     SELECT 	UUID(), guest_os_hypervisor_hypervisor_type, guest_os_hypervisor_hypervisor_version, guest_os_hypervisor_guest_os_name, guest_os.id, now()
     FROM 	cloud.guest_os
     WHERE 	guest_os.category_id = guest_os_category_id
       AND 	guest_os.display_name = guest_os_display_name
       AND	NOT EXISTS (SELECT  1
                          FROM    cloud.guest_os_hypervisor as hypervisor
                          WHERE   hypervisor_type = guest_os_hypervisor_hypervisor_type
                            AND     hypervisor_version = guest_os_hypervisor_hypervisor_version
                            AND     hypervisor.guest_os_id = guest_os.id
                            AND     hypervisor.guest_os_name = guest_os_hypervisor_guest_os_name)
;END;

CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 11 (64-bit)', 'XenServer', '8.2.1', 'Debian Bullseye 11');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 11 (32-bit)', 'XenServer', '8.2.1', 'Debian Bullseye 11');
