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

ALTER TABLE `cloud`.`disk_offering`
    ADD COLUMN `fileshare` boolean DEFAULT 0 COMMENT 'offering is for fileshare';

-- add fileshare support to disk offering view
DROP VIEW IF EXISTS `cloud`.`disk_offering_view`;
CREATE VIEW `cloud`.`disk_offering_view` AS
SELECT
    `disk_offering`.`id` AS `id`,
    `disk_offering`.`uuid` AS `uuid`,
    `disk_offering`.`name` AS `name`,
    `disk_offering`.`display_text` AS `display_text`,
    `disk_offering`.`provisioning_type` AS `provisioning_type`,
    `disk_offering`.`disk_size` AS `disk_size`,
    `disk_offering`.`min_iops` AS `min_iops`,
    `disk_offering`.`max_iops` AS `max_iops`,
    `disk_offering`.`created` AS `created`,
    `disk_offering`.`tags` AS `tags`,
    `disk_offering`.`customized` AS `customized`,
    `disk_offering`.`customized_iops` AS `customized_iops`,
    `disk_offering`.`removed` AS `removed`,
    `disk_offering`.`use_local_storage` AS `use_local_storage`,
    `disk_offering`.`hv_ss_reserve` AS `hv_ss_reserve`,
    `disk_offering`.`bytes_read_rate` AS `bytes_read_rate`,
    `disk_offering`.`bytes_read_rate_max` AS `bytes_read_rate_max`,
    `disk_offering`.`bytes_read_rate_max_length` AS `bytes_read_rate_max_length`,
    `disk_offering`.`bytes_write_rate` AS `bytes_write_rate`,
    `disk_offering`.`bytes_write_rate_max` AS `bytes_write_rate_max`,
    `disk_offering`.`bytes_write_rate_max_length` AS `bytes_write_rate_max_length`,
    `disk_offering`.`iops_read_rate` AS `iops_read_rate`,
    `disk_offering`.`iops_read_rate_max` AS `iops_read_rate_max`,
    `disk_offering`.`iops_read_rate_max_length` AS `iops_read_rate_max_length`,
    `disk_offering`.`iops_write_rate` AS `iops_write_rate`,
    `disk_offering`.`iops_write_rate_max` AS `iops_write_rate_max`,
    `disk_offering`.`iops_write_rate_max_length` AS `iops_write_rate_max_length`,
    `disk_offering`.`cache_mode` AS `cache_mode`,
    `disk_offering`.`sort_key` AS `sort_key`,
    `disk_offering`.`compute_only` AS `compute_only`,
    `disk_offering`.`display_offering` AS `display_offering`,
    `disk_offering`.`state` AS `state`,
    `disk_offering`.`disk_size_strictness` AS `disk_size_strictness`,
    `vsphere_storage_policy`.`value` AS `vsphere_storage_policy`,
    `disk_offering`.`encrypt` AS `encrypt`,
    `disk_offering`.`fileshare` AS `fileshare`,
    GROUP_CONCAT(DISTINCT(domain.id)) AS domain_id,
    GROUP_CONCAT(DISTINCT(domain.uuid)) AS domain_uuid,
    GROUP_CONCAT(DISTINCT(domain.name)) AS domain_name,
    GROUP_CONCAT(DISTINCT(domain.path)) AS domain_path,
    GROUP_CONCAT(DISTINCT(zone.id)) AS zone_id,
    GROUP_CONCAT(DISTINCT(zone.uuid)) AS zone_uuid,
    GROUP_CONCAT(DISTINCT(zone.name)) AS zone_name
FROM
    `cloud`.`disk_offering`
        LEFT JOIN
    `cloud`.`disk_offering_details` AS `domain_details` ON `domain_details`.`offering_id` = `disk_offering`.`id` AND `domain_details`.`name`='domainid'
        LEFT JOIN
    `cloud`.`domain` AS `domain` ON FIND_IN_SET(`domain`.`id`, `domain_details`.`value`)
        LEFT JOIN
    `cloud`.`disk_offering_details` AS `zone_details` ON `zone_details`.`offering_id` = `disk_offering`.`id` AND `zone_details`.`name`='zoneid'
        LEFT JOIN
    `cloud`.`data_center` AS `zone` ON FIND_IN_SET(`zone`.`id`, `zone_details`.`value`)
        LEFT JOIN
    `cloud`.`disk_offering_details` AS `vsphere_storage_policy` ON `vsphere_storage_policy`.`offering_id` = `disk_offering`.`id`
        AND `vsphere_storage_policy`.`name` = 'storagepolicy'
WHERE
       `disk_offering`.`state`='Active'
GROUP BY
    `disk_offering`.`id`;

CREATE TABLE `cloud`.`storage_fileshare`(
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'ID',
    `uuid` varchar(40) COMMENT 'UUID',
    `name` varchar(255) NOT NULL COMMENT 'Name of the file share',
    `description` varchar(1024) COMMENT 'Description',
    `domain_id` bigint unsigned NOT NULL COMMENT 'Domain ID',
    `account_id` bigint unsigned NOT NULL COMMENT 'Account ID',
    `project_id` bigint unsigned NOT NULL COMMENT 'Project ID',
    `data_center_id` bigint unsigned NOT NULL COMMENT 'Data center ID',
    `state` varchar(12) NOT NULL COMMENT 'State of the file share in the FSM',
    `endpoint_ip` varchar(40) COMMENT 'IP address of the file share server',
    `endpoint_path` varchar(255) COMMENT 'Path of the file share',
    `fs_provider_name` varchar(255) COMMENT 'Name of the file share provider',
    `protocol` varchar(10) COMMENT 'Protocol supported by the file share',
    `volume_id` bigint unsigned COMMENT 'Volume which the file share is using as storage',
    `vm_id` bigint unsigned COMMENT 'vm on which the file share is hosted',
    `mount_options` varchar(255) COMMENT 'default mount options to be used while mounting the file share',
    `fs_type` varchar(10) NOT NULL COMMENT 'The filesystem format to be used for the file share',
    `service_offering_id` bigint unsigned COMMENT 'Service offering for the vm',
    `update_count` bigint unsigned COMMENT 'Update count for state change',
    `updated` datetime COMMENT 'date updated',
    `created` datetime NOT NULL COMMENT 'date created',
    `removed` datetime COMMENT 'date removed if not null',
    PRIMARY KEY (`id`),
    CONSTRAINT `uc_storage_fileshare__uuid` UNIQUE (`uuid`),
    INDEX `i_storage_fileshare__account_id`(`account_id`),
    INDEX `i_storage_fileshare__domain_id`(`domain_id`),
    INDEX `i_storage_fileshare__project_id`(`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`storagefsvm`(
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `fileshare_id` bigint unsigned NOT NULL COMMENT 'corresponding file share ID',
    PRIMARY KEY (`id`),
    KEY `fk_storage_fileshare__id` (`fileshare_id`),
    CONSTRAINT `fk_storagefsvm__id` FOREIGN KEY (`id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP VIEW IF EXISTS `cloud`.`storage_fileshare_view`;
CREATE VIEW `cloud`.`storage_fileshare_view` AS
SELECT
    `storage_fileshare`.`id` AS `id`,
    `storage_fileshare`.`uuid` AS `uuid`,
    `storage_fileshare`.`name` AS `name`,
    `storage_fileshare`.`description` AS `description`,
    `storage_fileshare`.`state` AS `state`,
    `storage_fileshare`.`fs_provider_name` AS `provider`,
    `zone`.`uuid` AS `zone_uuid`,
    `zone`.`name` AS `zone_name`,
    `instance`.`id` AS `instance_id`,
    `instance`.`uuid` AS `instance_uuid`,
    `instance`.`name` AS `instance_name`,
    `volumes`.`size` AS `size`,
    `volumes`.`uuid` AS `volume_uuid`,
    `volumes`.`name` AS `volume_name`,
    `storage_pool`.`uuid` AS `pool_uuid`,
    `storage_pool`.`name` AS `pool_name`,
    `account`.`account_name` AS `account_name`,
    `project`.`uuid` AS `project_uuid`,
    `project`.`name` AS `project_name`,
    `domain`.`uuid` AS `domain_uuid`,
    `domain`.`name` AS `domain_name`,
    `service_offering`.`uuid` AS `service_offering_uuid`,
    `service_offering`.`name` AS `service_offering_name`,
    `disk_offering`.`uuid` AS `disk_offering_uuid`,
    `disk_offering`.`name` AS `disk_offering_name`,
    `disk_offering`.`display_text` AS `disk_offering_display_text`,
    `disk_offering`.`disk_size` AS `disk_offering_size`,
    `disk_offering`.`customized` AS `disk_offering_custom`,
    GROUP_CONCAT(DISTINCT(nics.uuid) ORDER BY nics.id) AS nic_uuid,
    GROUP_CONCAT(DISTINCT(nics.ip4_address) ORDER BY nics.id) AS ip_address
FROM
    `cloud`.`storage_fileshare`
        LEFT JOIN
    `cloud`.`data_center` AS `zone` ON `storage_fileshare`.`data_center_id` = `zone`.`id`
        LEFT JOIN
    `cloud`.`vm_instance` AS `instance` ON `storage_fileshare`.`vm_id` = `instance`.`id`
        LEFT JOIN
    `cloud`.`volumes` AS `volumes` ON `instance`.`id` = `volumes`.`instance_id` AND `volumes`.`volume_type` = 'datadisk'
        LEFT JOIN
    `cloud`.`storage_pool` AS `storage_pool` ON `volumes`.`pool_id` = `storage_pool`.`id`
        LEFT JOIN
    `cloud`.`nics` AS `nics` ON `storage_fileshare`.`vm_id` = `nics`.`instance_id`
        LEFT JOIN
    `cloud`.`account` AS `account` ON `storage_fileshare`.`account_id` = `account`.`id`
        LEFT JOIN
    `cloud`.`projects` AS `project` ON `storage_fileshare`.`project_id` = `project`.`id`
        LEFT JOIN
    `cloud`.`domain` AS `domain` ON `storage_fileshare`.`domain_id` = `domain`.`id`
        LEFT JOIN
    `cloud`.`service_offering` AS `service_offering` ON `storage_fileshare`.`service_offering_id` = `service_offering`.`id`
        LEFT JOIN
    `cloud`.`disk_offering` AS `disk_offering` ON `volumes`.`disk_offering_id` = `disk_offering`.`id`
GROUP BY
    `storage_fileshare`.`id`;

-- Quota inject tariff result into subsequent ones
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.quota_tariff', 'position', 'bigint(20) NOT NULL DEFAULT 1 COMMENT "Position in the execution sequence for tariffs of the same type"');
