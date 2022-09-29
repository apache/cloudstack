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
-- Schema upgrade from 4.17.1.0 to 4.18.0.0
--;
-- Enable CPU cap for default system offerings;
UPDATE `cloud`.`service_offering` so
SET so.limit_cpu_use = 1
WHERE so.default_use = 1 AND so.vm_type IN ('domainrouter', 'secondarystoragevm', 'consoleproxy', 'internalloadbalancervm', 'elasticloadbalancervm');

-- Idempotent ADD COLUMN
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_COLUMN`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_COLUMN` (
    IN in_table_name VARCHAR(200)
, IN in_column_name VARCHAR(200)
, IN in_column_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;


-- Add foreign key procedure to link volumes to passphrase table
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY` (
    IN in_table_name VARCHAR(200),
    IN in_foreign_table_name VARCHAR(200),
    IN in_foreign_column_name VARCHAR(200)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1005 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', ' ADD CONSTRAINT '); SET @ddl = CONCAT(@ddl, 'fk_', in_foreign_table_name, '_', in_foreign_column_name); SET @ddl = CONCAT(@ddl, ' FOREIGN KEY (', in_foreign_table_name, '_', in_foreign_column_name, ')'); SET @ddl = CONCAT(@ddl, ' REFERENCES ', in_foreign_table_name, '(', in_foreign_column_name, ')'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Add passphrase table
CREATE TABLE IF NOT EXISTS `cloud`.`passphrase` (
    `id` bigint unsigned NOT NULL auto_increment,
    `passphrase` varchar(64) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add passphrase column to volumes table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'passphrase_id', 'bigint unsigned DEFAULT NULL COMMENT ''encryption passphrase id'' ');
CALL `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`('cloud.volumes', 'passphrase', 'id');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'encrypt_format', 'varchar(64) DEFAULT NULL COMMENT ''encryption format'' ');

-- Add encrypt column to disk_offering
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.disk_offering', 'encrypt', 'tinyint(1) DEFAULT 0 COMMENT ''volume encrypt requested'' ');

-- add encryption support to disk offering view
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

-- add encryption support to service offering view
DROP VIEW IF EXISTS `cloud`.`service_offering_view`;
CREATE VIEW `cloud`.`service_offering_view` AS
SELECT
    `service_offering`.`id` AS `id`,
    `service_offering`.`uuid` AS `uuid`,
    `service_offering`.`name` AS `name`,
    `service_offering`.`display_text` AS `display_text`,
    `disk_offering`.`provisioning_type` AS `provisioning_type`,
    `service_offering`.`created` AS `created`,
    `disk_offering`.`tags` AS `tags`,
    `service_offering`.`removed` AS `removed`,
    `disk_offering`.`use_local_storage` AS `use_local_storage`,
    `service_offering`.`system_use` AS `system_use`,
    `disk_offering`.`id` AS `disk_offering_id`,
    `disk_offering`.`name` AS `disk_offering_name`,
    `disk_offering`.`uuid` AS `disk_offering_uuid`,
    `disk_offering`.`display_text` AS `disk_offering_display_text`,
    `disk_offering`.`customized_iops` AS `customized_iops`,
    `disk_offering`.`min_iops` AS `min_iops`,
    `disk_offering`.`max_iops` AS `max_iops`,
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
    `disk_offering`.`disk_size` AS `root_disk_size`,
    `disk_offering`.`encrypt` AS `encrypt_root`,
    `service_offering`.`cpu` AS `cpu`,
    `service_offering`.`speed` AS `speed`,
    `service_offering`.`ram_size` AS `ram_size`,
    `service_offering`.`nw_rate` AS `nw_rate`,
    `service_offering`.`mc_rate` AS `mc_rate`,
    `service_offering`.`ha_enabled` AS `ha_enabled`,
    `service_offering`.`limit_cpu_use` AS `limit_cpu_use`,
    `service_offering`.`host_tag` AS `host_tag`,
    `service_offering`.`default_use` AS `default_use`,
    `service_offering`.`vm_type` AS `vm_type`,
    `service_offering`.`sort_key` AS `sort_key`,
    `service_offering`.`is_volatile` AS `is_volatile`,
    `service_offering`.`deployment_planner` AS `deployment_planner`,
    `service_offering`.`dynamic_scaling_enabled` AS `dynamic_scaling_enabled`,
    `service_offering`.`disk_offering_strictness` AS `disk_offering_strictness`,
    `vsphere_storage_policy`.`value` AS `vsphere_storage_policy`,
    GROUP_CONCAT(DISTINCT(domain.id)) AS domain_id,
    GROUP_CONCAT(DISTINCT(domain.uuid)) AS domain_uuid,
    GROUP_CONCAT(DISTINCT(domain.name)) AS domain_name,
    GROUP_CONCAT(DISTINCT(domain.path)) AS domain_path,
    GROUP_CONCAT(DISTINCT(zone.id)) AS zone_id,
    GROUP_CONCAT(DISTINCT(zone.uuid)) AS zone_uuid,
    GROUP_CONCAT(DISTINCT(zone.name)) AS zone_name,
    IFNULL(`min_compute_details`.`value`, `cpu`) AS min_cpu,
    IFNULL(`max_compute_details`.`value`, `cpu`) AS max_cpu,
    IFNULL(`min_memory_details`.`value`, `ram_size`) AS min_memory,
    IFNULL(`max_memory_details`.`value`, `ram_size`) AS max_memory
FROM
    `cloud`.`service_offering`
        INNER JOIN
    `cloud`.`disk_offering_view` AS `disk_offering` ON service_offering.disk_offering_id = disk_offering.id
        LEFT JOIN
    `cloud`.`service_offering_details` AS `domain_details` ON `domain_details`.`service_offering_id` = `service_offering`.`id` AND `domain_details`.`name`='domainid'
        LEFT JOIN
    `cloud`.`domain` AS `domain` ON FIND_IN_SET(`domain`.`id`, `domain_details`.`value`)
        LEFT JOIN
    `cloud`.`service_offering_details` AS `zone_details` ON `zone_details`.`service_offering_id` = `service_offering`.`id` AND `zone_details`.`name`='zoneid'
        LEFT JOIN
    `cloud`.`data_center` AS `zone` ON FIND_IN_SET(`zone`.`id`, `zone_details`.`value`)
        LEFT JOIN
    `cloud`.`service_offering_details` AS `min_compute_details` ON `min_compute_details`.`service_offering_id` = `service_offering`.`id`
        AND `min_compute_details`.`name` = 'mincpunumber'
        LEFT JOIN
    `cloud`.`service_offering_details` AS `max_compute_details` ON `max_compute_details`.`service_offering_id` = `service_offering`.`id`
        AND `max_compute_details`.`name` = 'maxcpunumber'
        LEFT JOIN
    `cloud`.`service_offering_details` AS `min_memory_details` ON `min_memory_details`.`service_offering_id` = `service_offering`.`id`
        AND `min_memory_details`.`name` = 'minmemory'
        LEFT JOIN
    `cloud`.`service_offering_details` AS `max_memory_details` ON `max_memory_details`.`service_offering_id` = `service_offering`.`id`
        AND `max_memory_details`.`name` = 'maxmemory'
        LEFT JOIN
    `cloud`.`service_offering_details` AS `vsphere_storage_policy` ON `vsphere_storage_policy`.`service_offering_id` = `service_offering`.`id`
        AND `vsphere_storage_policy`.`name` = 'storagepolicy'
WHERE
        `service_offering`.`state`='Active'
GROUP BY
    `service_offering`.`id`;

-- Add cidr_list column to load_balancing_rules
ALTER TABLE `cloud`.`load_balancing_rules`
ADD cidr_list VARCHAR(4096);

-- savely add resources in parallel
-- PR#5984 Create table to persist VM stats.
DROP TABLE IF EXISTS `cloud`.`resource_reservation`;
CREATE TABLE `cloud`.`resource_reservation` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `resource_type` varchar(255) NOT NULL,
  `amount` bigint unsigned NOT NULL,
  PRIMARY KEY (`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Alter networks table to add ip6dns1 and ip6dns2
ALTER TABLE `cloud`.`networks`
    ADD COLUMN `ip6dns1` varchar(255) DEFAULT NULL COMMENT 'first IPv6 DNS for the network' AFTER `dns2`,
    ADD COLUMN `ip6dns2` varchar(255) DEFAULT NULL COMMENT 'second IPv6 DNS for the network' AFTER `ip6dns1`;
-- Alter vpc table to add dns1, dns2, ip6dns1 and ip6dns2
ALTER TABLE `cloud`.`vpc`
    ADD COLUMN `dns1` varchar(255) DEFAULT NULL COMMENT 'first IPv4 DNS for the vpc' AFTER `network_domain`,
    ADD COLUMN `dns2` varchar(255) DEFAULT NULL COMMENT 'second IPv4 DNS for the vpc' AFTER `dns1`,
    ADD COLUMN `ip6dns1` varchar(255) DEFAULT NULL COMMENT 'first IPv6 DNS for the vpc' AFTER `dns2`,
    ADD COLUMN `ip6dns2` varchar(255) DEFAULT NULL COMMENT 'second IPv6 DNS for the vpc' AFTER `ip6dns1`;

-- Fix migrateVolume permissions #6224.
DELETE role_perm
FROM role_permissions role_perm
INNER JOIN roles ON role_perm.role_id = roles.id
WHERE roles.role_type != 'Admin' AND roles.is_default = 1 AND role_perm.rule = 'migrateVolume';
