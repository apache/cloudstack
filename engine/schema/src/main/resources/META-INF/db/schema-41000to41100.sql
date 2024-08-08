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
-- Schema upgrade from 4.10.0.0 to 4.11.0.0
--;

--;
-- Stored procedure to do idempotent column add;
--;
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_COLUMN`;

CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_COLUMN` (
		IN in_table_name VARCHAR(200)
    , IN in_column_name VARCHAR(200)
    , IN in_column_definition VARCHAR(1000)
)
BEGIN

    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_DROP_FOREIGN_KEY`;

CREATE PROCEDURE `cloud`.`IDEMPOTENT_DROP_FOREIGN_KEY` (
		IN in_table_name VARCHAR(200)
    , IN in_foreign_key_name VARCHAR(200)
)
BEGIN

    DECLARE CONTINUE HANDLER FOR 1091 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', ' DROP FOREIGN KEY '); SET @ddl = CONCAT(@ddl, ' ', in_foreign_key_name); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_DROP_INDEX`;

CREATE PROCEDURE `cloud`.`IDEMPOTENT_DROP_INDEX` (
		IN in_index_name VARCHAR(200)
    , IN in_table_name VARCHAR(200)
)
BEGIN

    DECLARE CONTINUE HANDLER FOR 1091 BEGIN END; SET @ddl = CONCAT('DROP INDEX ', in_index_name); SET @ddl = CONCAT(@ddl, ' ', ' ON ') ; SET @ddl = CONCAT(@ddl, ' ', in_table_name); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_CREATE_UNIQUE_INDEX`;

CREATE PROCEDURE `cloud`.`IDEMPOTENT_CREATE_UNIQUE_INDEX` (
		IN in_index_name VARCHAR(200)
    , IN in_table_name VARCHAR(200)
    , IN in_index_definition VARCHAR(1000)
)
BEGIN

    DECLARE CONTINUE HANDLER FOR 1061 BEGIN END; SET @ddl = CONCAT('CREATE UNIQUE INDEX ', in_index_name); SET @ddl = CONCAT(@ddl, ' ', ' ON ') ; SET @ddl = CONCAT(@ddl, ' ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', in_index_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Add For VPC flag
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','for_vpc', 'INT(1) NOT NULL DEFAULT 0');

UPDATE cloud.network_offerings o
SET for_vpc = 1
where
  o.conserve_mode = 0
  and o.guest_type = 'Isolated'
  and exists(
    SELECT id
    from cloud.ntwk_offering_service_map
    where network_offering_id = o.id and (
      provider in ('VpcVirtualRouter', 'InternalLbVm', 'JuniperContrailVpcRouter')
      or service in ('NetworkACL')
    )
  );

UPDATE `cloud`.`configuration` SET value = '600', default_value = '600' WHERE category = 'Advanced' AND name = 'router.aggregation.command.each.timeout';

-- CA framework changes
DELETE from `cloud`.`configuration` where name='ssl.keystore';

-- Certificate Revocation List
CREATE TABLE IF NOT EXISTS `cloud`.`crl` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `serial` varchar(255) UNIQUE NOT NULL COMMENT 'certificate\'s serial number as hex string',
  `cn` varchar(255) COMMENT 'certificate\'s common name',
  `revoker_uuid` varchar(40) COMMENT 'revoker user account uuid',
  `revoked` datetime COMMENT 'date of revocation',
  PRIMARY KEY (`id`),
  KEY (`serial`),
  UNIQUE KEY (`serial`, `cn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Host HA feature
CREATE TABLE IF NOT EXISTS `cloud`.`ha_config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resource_id` bigint(20) unsigned DEFAULT NULL COMMENT 'id of the resource',
  `resource_type` varchar(255) NOT NULL COMMENT 'the type of the resource',
  `enabled` int(1) unsigned DEFAULT '0' COMMENT 'is HA enabled for the resource',
  `ha_state` varchar(255) DEFAULT 'Disabled' COMMENT 'HA state',
  `provider` varchar(255) DEFAULT NULL COMMENT 'HA provider',
  `update_count` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT 'state based incr-only counter for atomic ha_state updates',
  `update_time` datetime COMMENT 'last ha_state update datetime',
  `mgmt_server_id` bigint(20) unsigned DEFAULT NULL COMMENT 'management server id that is responsible for the HA for the resource',
  PRIMARY KEY (`id`),
  KEY `i_ha_config__enabled` (`enabled`),
  KEY `i_ha_config__ha_state` (`ha_state`),
  KEY `i_ha_config__mgmt_server_id` (`mgmt_server_id`),
  UNIQUE KEY (`resource_id`, `resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE from `cloud`.`configuration` where name='outofbandmanagement.sync.interval';

-- Annotations specific changes following
CREATE TABLE IF NOT EXISTS `cloud`.`annotations` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) UNIQUE,
  `annotation` text,
  `entity_uuid` varchar(40),
  `entity_type` varchar(32),
  `user_uuid` varchar(40),
  `created` datetime COMMENT 'date of creation',
  `removed` datetime COMMENT 'date of removal',
  PRIMARY KEY (`id`),
  KEY (`uuid`),
  KEY `i_entity` (`entity_uuid`, `entity_type`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Out-of-band management driver for nested-cloudstack
ALTER TABLE `cloud`.`oobm` MODIFY COLUMN port VARCHAR(255);

-- CLOUDSTACK-9902: Console proxy SSL toggle
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `description`, `default_value`, `is_dynamic`) VALUES ('Console Proxy', 'DEFAULT', 'AgentManager', 'consoleproxy.sslEnabled', 'false', 'Enable SSL for console proxy', 'false', 0);

-- CLOUDSTACK-9859: Retirement of midonet plugin (final removal)
delete from `cloud`.`configuration` where name in ('midonet.apiserver.address', 'midonet.providerrouter.id');

-- CLOUDSTACK-9972: Enhance listVolumes API
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Premium', 'DEFAULT', 'management-server', 'volume.stats.interval', '600000', 'Interval (in seconds) to report volume statistics', '600000', now(), NULL, NULL);

-- Extra Dhcp Options
CREATE TABLE IF NOT EXISTS `cloud`.`nic_extra_dhcp_options` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `nic_id` bigint unsigned NOT NULL COMMENT ' nic id where dhcp options are applied',
  `code` int(32),
  `value` text,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_nic_extra_dhcp_options_nic_id` FOREIGN KEY (`nic_id`) REFERENCES `nics`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add new OS versions

-- Add XenServer 7.1 and 7.2 hypervisor capabilities
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, storage_motion_supported) values (UUID(), 'XenServer', '7.1.0', 500, 13, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, storage_motion_supported) values (UUID(), 'XenServer', '7.2.0', 500, 13, 1);

-- Add XenServer 7.0 support for windows 10
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.0.0', 'Windows 10 (64-bit)', 258, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.0.0', 'Windows 10 (32-bit)', 257, now(), 0);

-- Add XenServer 7.1 hypervisor guest OS mappings (copy 7.0.0)
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '7.1.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='7.0.0';

-- Add XenServer 7.1 hypervisor guest OS (see https://docs.citrix.com/content/dam/docs/en-us/xenserver/7-1/downloads/xenserver-7-1-release-notes.pdf)
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.0', 'Windows Server 2016 (64-bit)', 259, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.0', 'SUSE Linux Enterprise Server 11 SP4', 187, now(),  0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.0', 'Red Hat Enterprise Linux 6 (64-bit)', 240, now(),  0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.0', 'Red Hat Enterprise Linux 7', 245, now(),  0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.0', 'Oracle Enterprise Linux 6 (64-bit)', 251, now(),  0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.0', 'Oracle Linux 7', 247, now(),  0);

-- Add XenServer 7.2 hypervisor guest OS mappings (copy 7.1.0 & remove Windows Vista, Windows XP, Windows 2003, CentOS 4.x, RHEL 4.xS, LES 10 (all versions) as per XenServer 7.2 Release Notes)
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '7.2.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='7.1.0' AND guest_os_id not in (1,2,3,4,56,101,56,58,93,94,50,51,87,88,89,90,91,92,26,27,28,29,40,41,42,43,44,45,96,97,107,108,109,110,151,152,153);

-- Add table to track primary storage in use for snapshots
CREATE TABLE IF NOT EXISTS `cloud_usage`.`usage_snapshot_on_primary` (
  `id` bigint(20) unsigned NOT NULL,
  `zone_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `name` varchar(128),
  `type` int(1) unsigned NOT NULL,
  `physicalsize` bigint(20),
  `virtualsize` bigint(20),
  `created` datetime NOT NULL,
  `deleted` datetime,
  INDEX `i_usage_snapshot_on_primary` (`account_id`,`id`,`vm_id`,`created`)
) ENGINE=InnoDB CHARSET=utf8;

-- Change monitor patch for apache2 in systemvm
UPDATE `cloud`.`monitoring_services` SET pidfile="/var/run/apache2/apache2.pid" WHERE process_name="apache2" AND service_name="apache2";

-- Use 'Other Linux 64-bit' as guest os for the default systemvmtemplate for VMware
-- This fixes a memory allocation issue to systemvms on VMware/ESXi
UPDATE `cloud`.`vm_template` SET guest_os_id=99 WHERE id=8;

-- Network External Ids
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.networks','external_id', 'varchar(255)');

-- Separate Subnet for CPVM and SSVM (system vms)
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.op_dc_ip_address_alloc','forsystemvms', 'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Indicates if IP is dedicated for CPVM or SSVM'' ');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.op_dc_ip_address_alloc','vlan', 'INT(10) UNSIGNED NULL COMMENT ''Vlan the management network range is on'' ');

-- CLOUDSTACK-4757: Support multidisk OVA
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template','parent_template_id', 'bigint(20) unsigned DEFAULT NULL COMMENT ''If datadisk template, then id of the root template this template belongs to'' ');

-- CLOUDSTACK-10146: Bypass Secondary Storage for KVM templates
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template','direct_download', 'TINYINT(1) DEFAULT 0 COMMENT ''Indicates if Secondary Storage is bypassed and template is downloaded to Primary Storage'' ');

-- CLOUDSTACK-10109: Enable dedication of public IPs to SSVM and CPVM
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.user_ip_address','forsystemvms', 'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''true if IP is set to system vms, false if not'' ');

-- ldap binding on domain level
CREATE TABLE IF NOT EXISTS `cloud`.`domain_details` (
    `id` bigint unsigned NOT NULL auto_increment,
    `domain_id` bigint unsigned NOT NULL COMMENT 'account id',
    `name` varchar(255) NOT NULL,
    `value` varchar(255) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_domain_details__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.ldap_configuration','domain_id', 'BIGINT(20) DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.ldap_trust_map','account_id', 'BIGINT(20) DEFAULT 0');
CALL `cloud`.`IDEMPOTENT_DROP_FOREIGN_KEY`('cloud.ldap_trust_map','fk_ldap_trust_map__domain_id');
CALL `cloud`.`IDEMPOTENT_DROP_INDEX`('uk_ldap_trust_map__domain_id','cloud.ldap_trust_map');
CALL `cloud`.`IDEMPOTENT_CREATE_UNIQUE_INDEX`('uk_ldap_trust_map__bind_location','cloud.ldap_trust_map', '(domain_id, account_id)');

CREATE TABLE IF NOT EXISTS `cloud`.`netscaler_servicepackages` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `name` varchar(255) UNIQUE COMMENT 'name of the service package',
  `description` varchar(255) COMMENT 'description of the service package',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`external_netscaler_controlcenter` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `username` varchar(255) COMMENT 'username of the NCC',
  `password` varchar(255) COMMENT 'password of NCC',
  `ncc_ip` varchar(255) COMMENT 'IP of NCC Manager',
  `num_retries` bigint unsigned NOT NULL default 2 COMMENT 'Number of retries in ncc for command failure',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.sslcerts','name', 'varchar(255) NULL default NULL COMMENT ''Name of the Certificate'' ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','service_package_id', 'varchar(255) NULL default NULL COMMENT ''Netscaler ControlCenter Service Package'' ');
