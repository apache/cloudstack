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

--- KVM Incremental Snapshots
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.snapshot_store_ref', 'kvm_checkpoint_path', 'varchar(255)');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.snapshot_store_ref', 'end_of_chain', 'int(1) unsigned');
