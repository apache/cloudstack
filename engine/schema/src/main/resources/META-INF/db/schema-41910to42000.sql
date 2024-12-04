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
-- Schema upgrade from 4.19.0.0 to 4.20.0.0
--;

-- Add tag column to tables
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_limit', 'tag', 'varchar(64) DEFAULT NULL COMMENT "tag for the limit" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_count', 'tag', 'varchar(64) DEFAULT NULL COMMENT "tag for the resource count" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_reservation', 'tag', 'varchar(64) DEFAULT NULL COMMENT "tag for the resource reservation" ');
CALL `cloud`.`IDEMPOTENT_DROP_INDEX`('i_resource_count__type_accountId', 'cloud.resource_count');
CALL `cloud`.`IDEMPOTENT_DROP_INDEX`('i_resource_count__type_domaintId', 'cloud.resource_count');

DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_UNIQUE_INDEX`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_UNIQUE_INDEX` (
    IN in_table_name VARCHAR(200),
    IN in_index_name VARCHAR(200),
    IN in_index_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1061 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name, ' ', 'ADD UNIQUE INDEX ', in_index_name, ' ', in_index_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

CALL `cloud`.`IDEMPOTENT_ADD_UNIQUE_INDEX`('cloud.resource_count', 'i_resource_count__type_tag_accountId', '(type, tag, account_id)');
CALL `cloud`.`IDEMPOTENT_ADD_UNIQUE_INDEX`('cloud.resource_count', 'i_resource_count__type_tag_domainId', '(type, tag, domain_id)');

ALTER TABLE `cloud`.`resource_reservation`
    MODIFY COLUMN `amount` bigint NOT NULL;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_reservation', 'resource_id', 'bigint unsigned NULL COMMENT "id of the resource" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_reservation', 'mgmt_server_id', 'bigint unsigned NULL COMMENT "management server id" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_reservation', 'created', 'datetime DEFAULT NULL COMMENT "date when the reservation was created" ');

UPDATE `cloud`.`resource_reservation` SET `created` = now() WHERE created IS NULL;


-- Update Default System offering for Router to 512MiB
UPDATE `cloud`.`service_offering` SET ram_size = 512 WHERE unique_name IN ("Cloud.Com-SoftwareRouter", "Cloud.Com-SoftwareRouter-Local",
                                                                           "Cloud.Com-InternalLBVm", "Cloud.Com-InternalLBVm-Local",
                                                                           "Cloud.Com-ElasticLBVm", "Cloud.Com-ElasticLBVm-Local")
                                                       AND system_use = 1 AND ram_size < 512;

-- NSX Plugin --
CREATE TABLE `cloud`.`nsx_providers` (
                                         `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
                                         `uuid` varchar(40),
                                         `zone_id` bigint unsigned NOT NULL COMMENT 'Zone ID',
                                         `host_id` bigint unsigned NOT NULL COMMENT 'Host ID',
                                         `provider_name` varchar(40),
                                         `hostname` varchar(255) NOT NULL,
                                         `port` varchar(255),
                                         `username` varchar(255) NOT NULL,
                                         `password` varchar(255) NOT NULL,
                                         `tier0_gateway` varchar(255),
                                         `edge_cluster` varchar(255),
                                         `transport_zone` varchar(255),
                                         `created` datetime NOT NULL COMMENT 'date created',
                                         `removed` datetime COMMENT 'date removed if not null',
                                         PRIMARY KEY (`id`),
                                         CONSTRAINT `fk_nsx_providers__zone_id` FOREIGN KEY `fk_nsx_providers__zone_id` (`zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE,
                                         INDEX `i_nsx_providers__zone_id`(`zone_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- NSX Plugin --
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','for_nsx', 'int(1) unsigned DEFAULT "0" COMMENT "is nsx enabled for the resource"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','nsx_mode', 'varchar(32) COMMENT "mode in which the network would route traffic"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc_offerings','for_nsx', 'int(1) unsigned DEFAULT "0" COMMENT "is nsx enabled for the resource"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc_offerings','nsx_mode', 'varchar(32) COMMENT "mode in which the network would route traffic"');

-- Create table to persist quota email template configurations
CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_email_configuration`(
    `account_id` int(11) NOT NULL,
    `email_template_id` bigint(20) NOT NULL,
    `enabled` int(1) UNSIGNED NOT NULL,
    PRIMARY KEY (`account_id`, `email_template_id`),
    CONSTRAINT `FK_quota_email_configuration_account_id` FOREIGN KEY (`account_id`) REFERENCES `cloud_usage`.`quota_account`(`account_id`),
    CONSTRAINT `FK_quota_email_configuration_email_template_id` FOREIGN KEY (`email_template_id`) REFERENCES `cloud_usage`.`quota_email_templates`(`id`));

-- Remove on delete cascade from snapshot schedule
ALTER TABLE `cloud`.`snapshot_schedule` DROP CONSTRAINT `fk__snapshot_schedule_async_job_id`;

-- Add `is_implicit` column to `host_tags` table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host_tags', 'is_implicit', 'int(1) UNSIGNED NOT NULL DEFAULT 0 COMMENT "If host tag is implicit or explicit" ');

-- Fields related to Snapshot Extraction
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.snapshot_store_ref', 'download_url', 'varchar(2048) DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.snapshot_store_ref', 'download_url_created', 'datetime DEFAULT NULL');

-- Webhooks feature
DROP TABLE IF EXISTS `cloud`.`webhook`;
CREATE TABLE `cloud`.`webhook` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id of the webhook',
  `uuid` varchar(255) COMMENT 'uuid of the webhook',
  `name` varchar(255) NOT NULL COMMENT 'name of the webhook',
  `description` varchar(4096) COMMENT 'description for the webhook',
  `state` char(32) NOT NULL COMMENT 'state of the webhook - Enabled or Disabled',
  `domain_id` bigint unsigned NOT NULL COMMENT 'id of the owner domain of the webhook',
  `account_id` bigint unsigned NOT NULL COMMENT 'id of the owner account of the webhook',
  `payload_url` varchar(255) COMMENT 'payload URL for the webhook',
  `secret_key` varchar(255) COMMENT 'secret key for the webhook',
  `ssl_verification` boolean COMMENT 'for https payload url, if true then strict ssl verification',
  `scope` char(32) NOT NULL COMMENT 'scope for the webhook - Local, Domain, Global',
  `created` datetime COMMENT 'date the webhook was created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY(`id`),
  INDEX `i_webhook__account_id`(`account_id`),
  CONSTRAINT `fk_webhook__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `cloud`.`webhook_delivery`;
CREATE TABLE `cloud`.`webhook_delivery` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id of the webhook delivery',
  `uuid` varchar(255) COMMENT 'uuid of the webhook',
  `event_id` bigint unsigned NOT NULL COMMENT 'id of the event',
  `webhook_id` bigint unsigned NOT NULL COMMENT 'id of the webhook',
  `mshost_msid` bigint unsigned NOT NULL COMMENT 'msid of the management server',
  `headers` TEXT COMMENT 'headers for the webhook delivery',
  `payload` TEXT COMMENT 'payload for the webhook delivery',
  `success` boolean COMMENT 'webhook delivery succeeded or not',
  `response` TEXT COMMENT 'response of the webhook delivery',
  `start_time` datetime COMMENT 'start timestamp of the webhook delivery',
  `end_time` datetime COMMENT 'end timestamp of the webhook delivery',
  PRIMARY KEY(`id`),
  INDEX `i_webhook__event_id`(`event_id`),
  INDEX `i_webhook__webhook_id`(`webhook_id`),
  CONSTRAINT `fk_webhook__event_id` FOREIGN KEY (`event_id`) REFERENCES `event`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_webhook__webhook_id` FOREIGN KEY (`webhook_id`) REFERENCES `webhook`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Normalize quota.usage.smtp.useStartTLS, quota.usage.smtp.useAuth, alert.smtp.useAuth and project.smtp.useAuth values
UPDATE
    `cloud`.`configuration`
SET
    value = "true"
WHERE
    name IN ("quota.usage.smtp.useStartTLS", "quota.usage.smtp.useAuth", "alert.smtp.useAuth", "project.smtp.useAuth")
    AND value IN ("true", "y", "t", "1", "on", "yes");

UPDATE
    `cloud`.`configuration`
SET
    value = "false"
WHERE
    name IN ("quota.usage.smtp.useStartTLS", "quota.usage.smtp.useAuth", "alert.smtp.useAuth", "project.smtp.useAuth")
    AND value NOT IN ("true", "y", "t", "1", "on", "yes");

-- Create tables for static and dynamic routing
CREATE TABLE `cloud`.`dc_ip4_guest_subnets` (
   `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
   `uuid` varchar(40) DEFAULT NULL,
   `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'zone it belongs to',
   `subnet` varchar(255) NOT NULL COMMENT 'subnet of the ip4 network',
   `domain_id` bigint unsigned DEFAULT NULL COMMENT 'domain the subnet belongs to',
   `account_id` bigint unsigned DEFAULT NULL COMMENT 'owner of this subnet',
   `created` datetime DEFAULT NULL,
   `removed` datetime DEFAULT NULL,
   PRIMARY KEY (`id`),
   CONSTRAINT `fk_dc_ip4_guest_subnets__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`),
   CONSTRAINT `fk_dc_ip4_guest_subnets__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`),
   CONSTRAINT `fk_dc_ip4_guest_subnets__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`),
   CONSTRAINT `uc_dc_ip4_guest_subnets__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ip4_guest_subnet_network_map` (
   `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
   `uuid` varchar(40) DEFAULT NULL,
   `parent_id` bigint(20) unsigned COMMENT 'ip4 guest subnet which subnet belongs to',
   `subnet` varchar(255) NOT NULL COMMENT 'subnet of the ip4 network',
   `network_id` bigint(20) unsigned DEFAULT NULL COMMENT 'network which subnet is associated to',
   `vpc_id` bigint(20) unsigned DEFAULT NULL COMMENT 'VPC which subnet is associated to',
   `state` varchar(255) NOT NULL COMMENT 'state of the subnet',
   `allocated` datetime DEFAULT NULL,
   `created` datetime DEFAULT NULL,
   `removed` datetime DEFAULT NULL,
   PRIMARY KEY (`id`),
   CONSTRAINT `fk_ip4_guest_subnet_network_map__parent_id` FOREIGN KEY (`parent_id`) REFERENCES `dc_ip4_guest_subnets`(`id`),
   CONSTRAINT `fk_ip4_guest_subnet_network_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`),
   CONSTRAINT `fk_ip4_guest_subnet_network_map__vpc_id` FOREIGN KEY (`vpc_id`) REFERENCES `vpc`(`id`),
   CONSTRAINT `uc_ip4_guest_subnet_network_map__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL `cloud`.`IDEMPOTENT_CHANGE_COLUMN`('network_offerings',  'nsx_mode', 'network_mode', 'varchar(32)  COMMENT "mode in which the network would route traffic"');
CALL `cloud`.`IDEMPOTENT_CHANGE_COLUMN`('vpc_offerings', 'nsx_mode', 'network_mode', 'varchar(32)  COMMENT "mode in which the network would route traffic"');
ALTER TABLE `cloud`.`event` MODIFY COLUMN `type` varchar(50) NOT NULL;

-- Add tables for AS Numbers and range
CREATE TABLE IF NOT EXISTS `cloud`.`as_number_range` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `uuid` varchar(40) DEFAULT NULL,
    `data_center_id` bigint unsigned NOT NULL COMMENT 'zone that it belongs to',
    `start_as_number` bigint unsigned NOT NULL COMMENT 'start AS number of the range',
    `end_as_number` bigint unsigned NOT NULL COMMENT 'end AS number of the range',
    `created` datetime DEFAULT NULL COMMENT 'date created',
    `removed` datetime DEFAULT NULL COMMENT 'date removed',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_as_number_range__uuid` (`uuid`),
    UNIQUE KEY `uk_as_number_range__range` (`data_center_id`,`start_as_number`,`end_as_number`, `removed`),
    CONSTRAINT `fk_as_number_range__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`as_number` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `uuid` varchar(40) DEFAULT NULL,
    `account_id` bigint unsigned DEFAULT NULL,
    `domain_id` bigint unsigned DEFAULT NULL,
    `as_number` bigint unsigned NOT NULL COMMENT 'the AS Number',
    `as_number_range_id` bigint unsigned NOT NULL,
    `data_center_id` bigint unsigned NOT NULL COMMENT 'zone that it belongs to',
    `allocated` datetime DEFAULT NULL COMMENT 'Date this AS Number was allocated to some network',
    `is_allocated` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'indicates if the AS Number is allocated to some network',
    `network_id` bigint unsigned DEFAULT NULL COMMENT 'Network this AS Number is associated with',
    `vpc_id` bigint unsigned DEFAULT NULL COMMENT 'VPC this AS Number is associated with',
    `created` datetime DEFAULT NULL COMMENT 'date created',
    `removed` datetime DEFAULT NULL COMMENT 'date removed',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_as_number__uuid` (`uuid`),
    UNIQUE KEY `uk_as_number__number` (`data_center_id`,`as_number`,`as_number_range_id`),
    CONSTRAINT `fk_as_number__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`),
    CONSTRAINT `fk_as_number__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_as_number__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`),
    CONSTRAINT `fk_as_number__as_number_range_id` FOREIGN KEY (`as_number_range_id`) REFERENCES `as_number_range` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','routing_mode', 'varchar(10) COMMENT "routing mode for the offering"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','specify_as_number', 'tinyint(1) NOT NULL DEFAULT 0 COMMENT "specify AS number when using dynamic routing"');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc_offerings','routing_mode', 'varchar(10) COMMENT "routing mode for the offering"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc_offerings','specify_as_number', 'tinyint(1) NOT NULL DEFAULT 0 COMMENT "specify AS number when using dynamic routing"');

-- Tables for Dynamic Routing
CREATE TABLE IF NOT EXISTS `cloud`.`bgp_peers` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `uuid` varchar(40) DEFAULT NULL,
    `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'zone it belongs to',
    `ip4_address` varchar(40) DEFAULT NULL COMMENT 'IPv4 address of the BGP peer',
    `ip6_address` varchar(40) DEFAULT NULL COMMENT 'IPv6 address of the BGP peer',
    `as_number` bigint unsigned NOT NULL COMMENT 'AS number of the BGP peer',
    `password` varchar(255) DEFAULT NULL COMMENT 'Password of the BGP peer',
    `domain_id` bigint unsigned DEFAULT NULL COMMENT 'domain the subnet belongs to',
    `account_id` bigint unsigned DEFAULT NULL COMMENT 'owner of this subnet',
    `created` datetime DEFAULT NULL COMMENT 'date created',
    `removed` datetime DEFAULT NULL COMMENT 'date removed',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bgp_peers__uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`bgp_peer_details` (
    `id` bigint unsigned NOT NULL auto_increment,
    `bgp_peer_id` bigint unsigned NOT NULL COMMENT 'bgp peer id',
    `name` varchar(255) NOT NULL,
    `value` varchar(1024) NOT NULL,
    `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_bgp_peer_details__bgp_peer_id` FOREIGN KEY `fk_bgp_peer_details__bgp_peer_id`(`bgp_peer_id`) REFERENCES `bgp_peers`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`bgp_peer_network_map` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `bgp_peer_id` bigint(20) unsigned COMMENT 'id of the BGP peer',
    `network_id` bigint(20) unsigned DEFAULT NULL COMMENT 'network which BGP peer is associated to',
    `vpc_id` bigint(20) unsigned DEFAULT NULL COMMENT 'vpc which BGP peer is associated to',
    `state` varchar(40) DEFAULT NULL,
    `created` datetime DEFAULT NULL COMMENT 'date created',
    `removed` datetime DEFAULT NULL COMMENT 'date removed',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_bgp_peer_network_map__bgp_peer_id` FOREIGN KEY (`bgp_peer_id`) REFERENCES `bgp_peers`(`id`),
    CONSTRAINT `fk_bgp_peer_network_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`),
    CONSTRAINT `fk_bgp_peer_network_map__vpc_id` FOREIGN KEY (`vpc_id`) REFERENCES `vpc`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`shared_filesystem`(
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'ID',
    `uuid` varchar(40) COMMENT 'UUID',
    `name` varchar(255) NOT NULL COMMENT 'Name of the shared filesystem',
    `description` varchar(1024) COMMENT 'Description',
    `domain_id` bigint unsigned NOT NULL COMMENT 'Domain ID',
    `account_id` bigint unsigned NOT NULL COMMENT 'Account ID',
    `data_center_id` bigint unsigned NOT NULL COMMENT 'Data center ID',
    `state` varchar(12) NOT NULL COMMENT 'State of the shared filesystem in the FSM',
    `fs_provider_name` varchar(255) COMMENT 'Name of the shared filesystem provider',
    `protocol` varchar(10) COMMENT 'Protocol supported by the shared filesystem',
    `volume_id` bigint unsigned COMMENT 'Volume which the shared filesystem is using as storage',
    `vm_id` bigint unsigned COMMENT 'vm on which the shared filesystem is hosted',
    `fs_type` varchar(10) NOT NULL COMMENT 'The filesystem format to be used for the shared filesystem',
    `service_offering_id` bigint unsigned COMMENT 'Service offering for the vm',
    `update_count` bigint unsigned COMMENT 'Update count for state change',
    `updated` datetime COMMENT 'date updated',
    `created` datetime NOT NULL COMMENT 'date created',
    `removed` datetime COMMENT 'date removed if not null',
    PRIMARY KEY (`id`),
    CONSTRAINT `uc_shared_filesystem__uuid` UNIQUE (`uuid`),
    INDEX `i_shared_filesystem__account_id`(`account_id`),
    INDEX `i_shared_filesystem__domain_id`(`domain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Quota inject tariff result into subsequent ones
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.quota_tariff', 'position', 'bigint(20) NOT NULL DEFAULT 1 COMMENT "Position in the execution sequence for tariffs of the same type"');

-- Idempotent IDEMPOTENT_MODIFY_COLUMN_CHAR_SET
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET` (
  IN in_table_name VARCHAR(200)
, IN in_column_name VARCHAR(200)
, IN in_column_type VARCHAR(200)
, IN in_column_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', ' MODIFY COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_type); SET @ddl = CONCAT(@ddl, ' ', ' CHARACTER SET utf8mb4'); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

DROP PROCEDURE IF EXISTS `cloud_usage`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`;
CREATE PROCEDURE `cloud_usage`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET` (
  IN in_table_name VARCHAR(200)
, IN in_column_name VARCHAR(200)
, IN in_column_type VARCHAR(200)
, IN in_column_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', ' MODIFY COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_type); SET @ddl = CONCAT(@ddl, ' ', ' CHARACTER SET utf8mb4'); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('async_job', 'job_result', 'TEXT', 'COMMENT \'job result info\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('async_job', 'job_cmd_info', 'TEXT', 'COMMENT \'command parameter info\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('event', 'description', 'VARCHAR(1024)', 'NOT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('usage_event', 'resource_name', 'VARCHAR(255)', 'DEFAULT NULL');
CALL `cloud_usage`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('usage_event', 'resource_name', 'VARCHAR(255)', 'DEFAULT NULL');

CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('account', 'account_name', 'VARCHAR(100)', 'DEFAULT NULL COMMENT \'an account name set by the creator of the account, defaults to username for single accounts\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('affinity_group', 'description', 'VARCHAR(4096)', 'DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('annotations', 'annotation', 'TEXT', '');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('autoscale_vmgroups', 'name', 'VARCHAR(255)', 'DEFAULT NULL COMMENT \'name of the autoscale vm group\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('backup_offering', 'name', 'VARCHAR(255)', 'NOT NULL COMMENT \'backup offering name\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('backup_offering', 'description', 'VARCHAR(255)', 'NOT NULL COMMENT \'backup offering description\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('disk_offering', 'name', 'VARCHAR(255)', 'NOT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('disk_offering', 'unique_name', 'VARCHAR(32)', 'DEFAULT NULL COMMENT \'unique name\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('disk_offering', 'display_text', 'VARCHAR(4096)', 'DEFAULT NULL COMMENT \'Optional text set by the admin for display purpose only\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('instance_group', 'name', 'VARCHAR(255)', 'NOT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('kubernetes_cluster', 'name', 'VARCHAR(255)', 'NOT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('kubernetes_cluster', 'description', 'VARCHAR(4096)', 'DEFAULT NULL COMMENT \'display text for this Kubernetes cluster\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('kubernetes_supported_version', 'name', 'VARCHAR(255)', 'NOT NULL COMMENT \'the name of this Kubernetes version\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('network_offerings', 'name', 'VARCHAR(64)', 'DEFAULT NULL COMMENT \'name of the network offering\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('network_offerings', 'unique_name', 'VARCHAR(64)', 'DEFAULT NULL COMMENT \'unique name of the network offering\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('network_offerings', 'display_text', 'VARCHAR(255)', 'NOT NULL COMMENT \'text to display to users\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('networks', 'name', 'VARCHAR(255)', 'DEFAULT NULL COMMENT \'name for this network\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('networks', 'display_text', 'VARCHAR(255)', 'DEFAULT NULL COMMENT \'display text for this network\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('project_role', 'description', 'TEXT', 'COMMENT \'description of the project role\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('projects', 'name', 'VARCHAR(255)', 'DEFAULT NULL COMMENT \'project name\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('projects', 'display_text', 'VARCHAR(255)', 'DEFAULT NULL COMMENT \'project display text\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('roles', 'description', 'TEXT', 'COMMENT \'description of the role\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('service_offering', 'name', 'VARCHAR(255)', 'NOT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('service_offering', 'unique_name', 'VARCHAR(32)', 'DEFAULT NULL COMMENT \'unique name for offerings\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('service_offering', 'display_text', 'VARCHAR(4096)', 'DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('snapshots', 'name', 'VARCHAR(255)', 'NOT NULL COMMENT \'snapshot name\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('ssh_keypairs', 'keypair_name', 'VARCHAR(256)', 'NOT NULL COMMENT \'name of the key pair\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('user_vm', 'display_name', 'VARCHAR(255)', 'DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('user_vm_details', 'value', 'VARCHAR(5120)', 'NOT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('user', 'firstname', 'VARCHAR(255)', 'DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('user', 'lastname', 'VARCHAR(255)', 'DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('user_data', 'name', 'VARCHAR(256)', 'NOT NULL COMMENT \'name of the user data\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('vm_instance', 'display_name', 'VARCHAR(255)', 'DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('vm_snapshots', 'display_name', 'VARCHAR(255)', 'DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('vm_snapshots', 'description', 'VARCHAR(255)', 'DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('vm_template', 'name', 'VARCHAR(255)', 'NOT NULL');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('vm_template', 'display_text', 'VARCHAR(4096)', 'DEFAULT NULL COMMENT \'Description text set by the admin for display purpose only\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('volumes', 'name', 'VARCHAR(255)', 'DEFAULT NULL COMMENT \'A user specified name for the volume\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('vpc', 'name', 'VARCHAR(255)', 'DEFAULT NULL COMMENT \'vpc name\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('vpc', 'display_text', 'VARCHAR(255)', 'DEFAULT NULL COMMENT \'vpc display text\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('vpc_offerings', 'name', 'VARCHAR(255)', 'DEFAULT NULL COMMENT \'vpc offering name\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('vpc_offerings', 'unique_name', 'VARCHAR(64)', 'DEFAULT NULL COMMENT \'unique name of the vpc offering\'');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('vpc_offerings', 'display_text', 'VARCHAR(255)', 'DEFAULT NULL COMMENT \'display text\'');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.roles','state', 'varchar(10) NOT NULL default "enabled" COMMENT "role state"');

-- Multi-Arch Zones
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.cluster', 'arch', 'varchar(8) DEFAULT "x86_64" COMMENT "the CPU architecture of the hosts in the cluster"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host', 'arch', 'varchar(8) DEFAULT "x86_64" COMMENT "the CPU architecture of the host"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template', 'arch', 'varchar(8) DEFAULT "x86_64" COMMENT "the CPU architecture of the template/ISO"');

-- NAS B&R Plugin Backup Repository
DROP TABLE IF EXISTS `cloud`.`backup_repository`;
CREATE TABLE `cloud`.`backup_repository` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id of the backup repository',
  `uuid` varchar(255) NOT NULL COMMENT 'uuid of the backup repository',
  `name` varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT 'name of the backup repository',
  `zone_id` bigint unsigned NOT NULL COMMENT 'id of zone',
  `provider` varchar(255) NOT NULL COMMENT 'backup provider name',
  `type` varchar(255) NOT NULL COMMENT 'backup repo type',
  `address` varchar(1024) NOT NULL COMMENT 'url of the backup repository',
  `mount_opts` varchar(1024) NOT NULL COMMENT 'mount options for the backup repository',
  `used_bytes` bigint unsigned,
  `capacity_bytes` bigint unsigned,
  `created` datetime,
  `removed` datetime,
  PRIMARY KEY(`id`),
  INDEX `i_backup_repository__uuid`(`uuid`),
  INDEX `i_backup_repository__zone_id_provider`(`zone_id`, `provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Drop foreign key on backup_schedule, drop unique key on vm_id and re-add foreign key to allow multiple backup schedules to be created
ALTER TABLE `cloud`.`backup_schedule` DROP FOREIGN KEY fk_backup_schedule__vm_id;
ALTER TABLE `cloud`.`backup_schedule` DROP INDEX vm_id;
ALTER TABLE `cloud`.`backup_schedule` ADD CONSTRAINT fk_backup_schedule__vm_id FOREIGN KEY (vm_id) REFERENCES vm_instance(id) ON DELETE CASCADE;

-- Add volume details to the backups table to keep track of the volumes being backed up
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'backed_volumes', 'text DEFAULT NULL COMMENT "details of backed-up volumes" ');
CALL `cloud`.`IDEMPOTENT_MODIFY_COLUMN_CHAR_SET`('backups', 'backed_volumes', 'TEXT', 'DEFAULT NULL COMMENT \'details of backed-up volumes\'');

-- Add support for VMware 8.0u2 (8.0.2.x) and 8.0u3 (8.0.3.x)
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '8.0.2', 1024, 0, 59, 64, 1, 1);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '8.0.2', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='8.0';
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '8.0.3', 1024, 0, 59, 64, 1, 1);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '8.0.3', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='8.0';

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_instance', 'delete_protection', 'boolean DEFAULT FALSE COMMENT "delete protection for vm" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'delete_protection', 'boolean DEFAULT FALSE COMMENT "delete protection for volumes" ');
