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
-- Schema upgrade from 4.16.0.0 to 4.16.1.0
--;

ALTER TABLE `cloud`.`vm_work_job` ADD COLUMN `secondary_object` char(100) COMMENT 'any additional item that must be checked during queueing' AFTER `vm_instance_id`;

-- Stored procedures to handle cloud and cloud_schema changes

-- Idempotent ADD COLUMN
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_COLUMN`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_COLUMN` (
    IN in_table_name VARCHAR(200)
, IN in_column_name VARCHAR(200)
, IN in_column_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Idempotent ADD COLUMN
DROP PROCEDURE IF EXISTS `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`;
CREATE PROCEDURE `cloud_usage`.`IDEMPOTENT_ADD_COLUMN` (
    IN in_table_name VARCHAR(200)
, IN in_column_name VARCHAR(200)
, IN in_column_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Idempotent DROP INDEX
DROP PROCEDURE IF EXISTS `cloud_usage`.`IDEMPOTENT_DROP_INDEX`;
CREATE PROCEDURE `cloud_usage`.`IDEMPOTENT_DROP_INDEX` (
    IN in_index_name VARCHAR(200)
, IN in_table_name VARCHAR(200)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1091 BEGIN END; SET @ddl = CONCAT('DROP INDEX ', in_index_name); SET @ddl = CONCAT(@ddl, ' ', ' ON ') ; SET @ddl = CONCAT(@ddl, ' ', in_table_name); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Idempotent ADD UNIQUE INDEX
DROP PROCEDURE IF EXISTS `cloud_usage`.`IDEMPOTENT_ADD_UNIQUE_INDEX`;
CREATE PROCEDURE `cloud_usage`.`IDEMPOTENT_ADD_UNIQUE_INDEX` (
    IN in_table_name VARCHAR(200)
, IN in_index_name VARCHAR(200)
, IN in_index_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1061 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD UNIQUE INDEX ', in_index_name); SET @ddl = CONCAT(@ddl, ' ', in_index_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Idempotent CHANGE COLUMN
DROP PROCEDURE IF EXISTS `cloud_usage`.`IDEMPOTENT_CHANGE_COLUMN`;
CREATE PROCEDURE `cloud_usage`.`IDEMPOTENT_CHANGE_COLUMN` (
    IN in_table_name VARCHAR(200)
, IN in_old_column_name VARCHAR(200)
, IN in_new_column_name VARCHAR(200)
, IN in_column_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', ' CHANGE COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_old_column_name); SET @ddl = CONCAT(@ddl, ' ', in_new_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Invoke stored procedures to add primary keys on missing tables

-- Add PK to cloud.op_user_stats_log
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.op_user_stats_log', 'id', 'BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`)');

-- Add PK to cloud_usage.usage_ip_address
CALL `cloud_usage`.`IDEMPOTENT_DROP_INDEX`('id','cloud_usage.usage_ip_address');
CALL `cloud_usage`.`IDEMPOTENT_CHANGE_COLUMN`('cloud_usage.usage_ip_address', 'id', 'ip_id', 'BIGINT(20) UNSIGNED NOT NULL');
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_ip_address', 'id', 'BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`)');
CALL `cloud_usage`.`IDEMPOTENT_ADD_UNIQUE_INDEX`('cloud_usage.usage_ip_address', 'id', '(ip_id ASC, assigned ASC)');

-- Add PK to usage_load_balancer_policy
CALL `cloud_usage`.`IDEMPOTENT_CHANGE_COLUMN`('cloud_usage.usage_load_balancer_policy', 'id', 'lb_id', 'BIGINT(20) UNSIGNED NOT NULL');
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_load_balancer_policy', 'id', 'BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`)');

-- Add PK to cloud_usage.usage_network_offering
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_network_offering', 'id', 'BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`)');

-- Add PK to cloud_usage.usage_port_forwarding
CALL `cloud_usage`.`IDEMPOTENT_CHANGE_COLUMN`('cloud_usage.usage_port_forwarding', 'id', 'pf_id', 'BIGINT(20) UNSIGNED NOT NULL');
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_port_forwarding', 'id', 'BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`)');

-- Add PK to cloud_usage.usage_security_group
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_security_group', 'id', 'BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`)');

-- cloud_usage.usage_snapshot_on_primary
CALL `cloud_usage`.`IDEMPOTENT_DROP_INDEX`('i_usage_snapshot_on_primary','cloud_usage.usage_snapshot_on_primary');
CALL `cloud_usage`.`IDEMPOTENT_CHANGE_COLUMN`('cloud_usage.usage_snapshot_on_primary', 'id', 'volume_id', 'BIGINT(20) UNSIGNED NOT NULL');
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_snapshot_on_primary', 'id', 'BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`)');
CALL `cloud_usage`.`IDEMPOTENT_ADD_UNIQUE_INDEX`('cloud_usage.usage_snapshot_on_primary', 'i_usage_snapshot_on_primary', '(account_id ASC, volume_id ASC, vm_id ASC, created ASC)');

-- Add PK to cloud_usage.usage_storage
CALL `cloud_usage`.`IDEMPOTENT_DROP_INDEX`('id','cloud_usage.usage_storage');
CALL `cloud_usage`.`IDEMPOTENT_CHANGE_COLUMN`('cloud_usage.usage_storage', 'id', 'entity_id', 'BIGINT(20) UNSIGNED NOT NULL');
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_storage', 'id', 'BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`)');
CALL `cloud_usage`.`IDEMPOTENT_ADD_UNIQUE_INDEX`('cloud_usage.usage_storage', 'id', '(entity_id ASC, storage_type ASC, zone_id ASC, created ASC)');

-- Add PK to cloud_usage.usage_vm_instance
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_vm_instance', 'id', 'BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`)');

-- Add PK to cloud_usage.usage_vmsnapshot
CALL `cloud_usage`.`IDEMPOTENT_DROP_INDEX`('i_usage_vmsnapshot','cloud_usage.usage_vmsnapshot');
CALL `cloud_usage`.`IDEMPOTENT_CHANGE_COLUMN`('cloud_usage.usage_vmsnapshot', 'id', 'volume_id', 'BIGINT(20) UNSIGNED NOT NULL');
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_vmsnapshot', 'id', 'BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`)');
CALL `cloud_usage`.`IDEMPOTENT_ADD_UNIQUE_INDEX`('cloud_usage.usage_vmsnapshot', 'i_usage_vmsnapshot', '(account_id ASC, volume_id ASC, vm_id ASC, created ASC)');

-- Add PK to cloud_usage.usage_volume
CALL `cloud_usage`.`IDEMPOTENT_DROP_INDEX`('id','cloud_usage.usage_volume');
CALL `cloud_usage`.`IDEMPOTENT_CHANGE_COLUMN`('cloud_usage.usage_volume', 'id', 'volume_id', 'BIGINT(20) UNSIGNED NOT NULL');
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_volume', 'id', 'BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`)');
CALL `cloud_usage`.`IDEMPOTENT_ADD_UNIQUE_INDEX`('cloud_usage.usage_volume', 'id', '(volume_id ASC, created ASC)');

-- Add PK to cloud_usage.usage_vpn_user
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.usage_vpn_user', 'id', 'BIGINT(20) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`)');

UPDATE `cloud`.`vm_template` SET deploy_as_is = 0 WHERE id = 8;

CREATE PROCEDURE `cloud`.`UPDATE_KUBERNETES_NODE_DETAILS`()
BEGIN
  DECLARE vmid BIGINT
; DECLARE done TINYINT DEFAULT FALSE
; DECLARE vmidcursor CURSOR FOR SELECT DISTINCT(vm_id) FROM `cloud`.`kubernetes_cluster_vm_map`
; DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE
; OPEN vmidcursor
; vmid_loop:LOOP
    FETCH NEXT FROM vmidcursor INTO vmid
;   IF done THEN
      LEAVE vmid_loop
;   ELSE
      INSERT `cloud`.`user_vm_details` (vm_id, name, value, display) VALUES (vmid, 'controlNodeLoginUser', 'core', 1)
;   END IF
; END LOOP
; CLOSE vmidcursor
; END;

CALL `cloud`.`UPDATE_KUBERNETES_NODE_DETAILS`();
DROP PROCEDURE IF EXISTS `cloud`.`UPDATE_KUBERNETES_NODE_DETAILS`;

-- Add support for VMware 7.0.2.0
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '7.0.2.0', 1024, 0, 59, 64, 1, 1);
-- Copy VMware 7.0.1.0 hypervisor guest OS mappings to VMware 7.0.2.0
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '7.0.2.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='7.0.1.0';

-- Add support for VMware 7.0.3.0
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '7.0.3.0', 1024, 0, 59, 64, 1, 1);
-- Copy VMware 7.0.1.0 hypervisor guest OS mappings to VMware 7.0.3.0
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '7.0.3.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='7.0.1.0';

 -- Alter event table to add resource_id and resource_type
ALTER TABLE `cloud`.`event`
    ADD COLUMN `resource_id` bigint unsigned COMMENT 'ID of the resource associated with the even' AFTER `domain_id`,
    ADD COLUMN `resource_type` varchar(32) COMMENT 'Account role in the project (Owner or Regular)' AFTER `resource_id`;

DROP VIEW IF EXISTS `cloud`.`event_view`;
CREATE VIEW `cloud`.`event_view` AS
    SELECT
        event.id,
        event.uuid,
        event.type,
        event.state,
        event.description,
        event.resource_id,
        event.resource_type,
        event.created,
        event.level,
        event.parameters,
        event.start_id,
        eve.uuid start_uuid,
        event.user_id,
        event.archived,
        event.display,
        user.username user_name,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name
    FROM
        `cloud`.`event`
            INNER JOIN
        `cloud`.`account` ON event.account_id = account.id
            INNER JOIN
        `cloud`.`domain` ON event.domain_id = domain.id
            INNER JOIN
        `cloud`.`user` ON event.user_id = user.id
            LEFT JOIN
        `cloud`.`projects` ON projects.project_account_id = event.account_id
            LEFT JOIN
        `cloud`.`event` eve ON event.start_id = eve.id;

-- Add passphrase table
CREATE TABLE IF NOT EXISTS `cloud`.`passphrase` (
    `id` bigint unsigned NOT NULL auto_increment,
    `passphrase` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add foreign key procedure to link volumes to passphrase table
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY` (
    IN in_table_name VARCHAR(200),
    IN in_foreign_table_name VARCHAR(200),
    IN in_foreign_column_name VARCHAR(200)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1005 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', ' ADD CONSTRAINT '); SET @ddl = CONCAT(@ddl, 'fk_', in_foreign_table_name, '_', in_foreign_column_name); SET @ddl = CONCAT(@ddl, ' FOREIGN KEY (', in_foreign_table_name, '_', in_foreign_column_name, ')'); SET @ddl = CONCAT(@ddl, ' REFERENCES ', in_foreign_table_name, '(', in_foreign_column_name, ')'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Add passphrase column to volumes table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'passphrase_id', 'bigint unsigned DEFAULT NULL COMMENT ''encryption passphrase id'' ');
CALL `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`('cloud.volumes', 'passphrase', 'id');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'encrypt_format', 'varchar(64) DEFAULT NULL COMMENT ''encryption format'' ');

-- Add encrypt column to disk_offering
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.disk_offering', 'encrypt', 'tinyint(1) DEFAULT 0 COMMENT ''volume encrypt requested'' ');

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
    `disk_offering`.`system_use` AS `system_use`,
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
    `disk_offering`.`type` AS `type`,
    `disk_offering`.`display_offering` AS `display_offering`,
    `disk_offering`.`state` AS `state`,
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

-- Add encrypt field to service_offering_view
DROP VIEW IF EXISTS `cloud`.`service_offering_view`;
CREATE VIEW `cloud`.`service_offering_view` AS
    SELECT
        `service_offering`.`id` AS `id`,
        `disk_offering`.`uuid` AS `uuid`,
        `disk_offering`.`name` AS `name`,
        `disk_offering`.`display_text` AS `display_text`,
        `disk_offering`.`provisioning_type` AS `provisioning_type`,
        `disk_offering`.`created` AS `created`,
        `disk_offering`.`tags` AS `tags`,
        `disk_offering`.`removed` AS `removed`,
        `disk_offering`.`use_local_storage` AS `use_local_storage`,
        `disk_offering`.`system_use` AS `system_use`,
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
        `cloud`.`disk_offering_view` AS `disk_offering` ON service_offering.id = disk_offering.id
            LEFT JOIN
        `cloud`.`service_offering_details` AS `domain_details` ON `domain_details`.`service_offering_id` = `disk_offering`.`id` AND `domain_details`.`name`='domainid'
            LEFT JOIN
        `cloud`.`domain` AS `domain` ON FIND_IN_SET(`domain`.`id`, `domain_details`.`value`)
            LEFT JOIN
        `cloud`.`service_offering_details` AS `zone_details` ON `zone_details`.`service_offering_id` = `disk_offering`.`id` AND `zone_details`.`name`='zoneid'
            LEFT JOIN
        `cloud`.`data_center` AS `zone` ON FIND_IN_SET(`zone`.`id`, `zone_details`.`value`)
			LEFT JOIN
		`cloud`.`service_offering_details` AS `min_compute_details` ON `min_compute_details`.`service_offering_id` = `disk_offering`.`id`
				AND `min_compute_details`.`name` = 'mincpunumber'
			LEFT JOIN
		`cloud`.`service_offering_details` AS `max_compute_details` ON `max_compute_details`.`service_offering_id` = `disk_offering`.`id`
				AND `max_compute_details`.`name` = 'maxcpunumber'
			LEFT JOIN
		`cloud`.`service_offering_details` AS `min_memory_details` ON `min_memory_details`.`service_offering_id` = `disk_offering`.`id`
				AND `min_memory_details`.`name` = 'minmemory'
			LEFT JOIN
		`cloud`.`service_offering_details` AS `max_memory_details` ON `max_memory_details`.`service_offering_id` = `disk_offering`.`id`
				AND `max_memory_details`.`name` = 'maxmemory'
			LEFT JOIN
		`cloud`.`service_offering_details` AS `vsphere_storage_policy` ON `vsphere_storage_policy`.`service_offering_id` = `disk_offering`.`id`
				AND `vsphere_storage_policy`.`name` = 'storagepolicy'
    WHERE
        `disk_offering`.`state`='Active'
    GROUP BY
        `service_offering`.`id`;

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

CREATE TABLE `cloud`.`user_data` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) NOT NULL COMMENT 'UUID of the user data',
  `name` varchar(256) NOT NULL COMMENT 'name of the user data',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner, foreign key to account table',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain, foreign key to domain table',
  `user_data` mediumtext COMMENT 'value of the userdata',
  `params` mediumtext COMMENT 'value of the comma-separated list of parameters',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_userdata__account_id` FOREIGN KEY(`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_userdata__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_userdata__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`user_vm` ADD COLUMN `user_data_id` bigint unsigned DEFAULT NULL COMMENT 'id of the user data' AFTER `user_data`;
ALTER TABLE `cloud`.`user_vm` ADD COLUMN `user_data_details` mediumtext DEFAULT NULL COMMENT 'value of the comma-separated list of parameters' AFTER `user_data_id`;
ALTER TABLE `cloud`.`user_vm` ADD CONSTRAINT `fk_user_vm__user_data_id` FOREIGN KEY `fk_user_vm__user_data_id`(`user_data_id`) REFERENCES `user_data`(`id`);

ALTER TABLE `cloud`.`vm_template` ADD COLUMN `user_data_id` bigint unsigned DEFAULT NULL COMMENT 'id of the user data';
ALTER TABLE `cloud`.`vm_template` ADD COLUMN `user_data_link_policy` varchar(255) DEFAULT NULL COMMENT 'user data link policy with template';
ALTER TABLE `cloud`.`vm_template` ADD CONSTRAINT `fk_vm_template__user_data_id` FOREIGN KEY `fk_vm_template__user_data_id`(`user_data_id`) REFERENCES `user_data`(`id`);

-- Added userdata details to template
DROP VIEW IF EXISTS `cloud`.`template_view`;
CREATE VIEW `cloud`.`template_view` AS
     SELECT
         `vm_template`.`id` AS `id`,
         `vm_template`.`uuid` AS `uuid`,
         `vm_template`.`unique_name` AS `unique_name`,
         `vm_template`.`name` AS `name`,
         `vm_template`.`public` AS `public`,
         `vm_template`.`featured` AS `featured`,
         `vm_template`.`type` AS `type`,
         `vm_template`.`hvm` AS `hvm`,
         `vm_template`.`bits` AS `bits`,
         `vm_template`.`url` AS `url`,
         `vm_template`.`format` AS `format`,
         `vm_template`.`created` AS `created`,
         `vm_template`.`checksum` AS `checksum`,
         `vm_template`.`display_text` AS `display_text`,
         `vm_template`.`enable_password` AS `enable_password`,
         `vm_template`.`dynamically_scalable` AS `dynamically_scalable`,
         `vm_template`.`state` AS `template_state`,
         `vm_template`.`guest_os_id` AS `guest_os_id`,
         `guest_os`.`uuid` AS `guest_os_uuid`,
         `guest_os`.`display_name` AS `guest_os_name`,
         `vm_template`.`bootable` AS `bootable`,
         `vm_template`.`prepopulate` AS `prepopulate`,
         `vm_template`.`cross_zones` AS `cross_zones`,
         `vm_template`.`hypervisor_type` AS `hypervisor_type`,
         `vm_template`.`extractable` AS `extractable`,
         `vm_template`.`template_tag` AS `template_tag`,
         `vm_template`.`sort_key` AS `sort_key`,
         `vm_template`.`removed` AS `removed`,
         `vm_template`.`enable_sshkey` AS `enable_sshkey`,
         `parent_template`.`id` AS `parent_template_id`,
         `parent_template`.`uuid` AS `parent_template_uuid`,
         `source_template`.`id` AS `source_template_id`,
         `source_template`.`uuid` AS `source_template_uuid`,
         `account`.`id` AS `account_id`,
         `account`.`uuid` AS `account_uuid`,
         `account`.`account_name` AS `account_name`,
         `account`.`type` AS `account_type`,
         `domain`.`id` AS `domain_id`,
         `domain`.`uuid` AS `domain_uuid`,
         `domain`.`name` AS `domain_name`,
         `domain`.`path` AS `domain_path`,
         `projects`.`id` AS `project_id`,
         `projects`.`uuid` AS `project_uuid`,
         `projects`.`name` AS `project_name`,
         `data_center`.`id` AS `data_center_id`,
         `data_center`.`uuid` AS `data_center_uuid`,
         `data_center`.`name` AS `data_center_name`,
         `launch_permission`.`account_id` AS `lp_account_id`,
         `template_store_ref`.`store_id` AS `store_id`,
         `image_store`.`scope` AS `store_scope`,
         `template_store_ref`.`state` AS `state`,
         `template_store_ref`.`download_state` AS `download_state`,
         `template_store_ref`.`download_pct` AS `download_pct`,
         `template_store_ref`.`error_str` AS `error_str`,
         `template_store_ref`.`size` AS `size`,
         `template_store_ref`.physical_size AS `physical_size`,
         `template_store_ref`.`destroyed` AS `destroyed`,
         `template_store_ref`.`created` AS `created_on_store`,
         `vm_template_details`.`name` AS `detail_name`,
         `vm_template_details`.`value` AS `detail_value`,
         `resource_tags`.`id` AS `tag_id`,
         `resource_tags`.`uuid` AS `tag_uuid`,
         `resource_tags`.`key` AS `tag_key`,
         `resource_tags`.`value` AS `tag_value`,
         `resource_tags`.`domain_id` AS `tag_domain_id`,
         `domain`.`uuid` AS `tag_domain_uuid`,
         `domain`.`name` AS `tag_domain_name`,
         `resource_tags`.`account_id` AS `tag_account_id`,
         `account`.`account_name` AS `tag_account_name`,
         `resource_tags`.`resource_id` AS `tag_resource_id`,
         `resource_tags`.`resource_uuid` AS `tag_resource_uuid`,
         `resource_tags`.`resource_type` AS `tag_resource_type`,
         `resource_tags`.`customer` AS `tag_customer`,
          CONCAT(`vm_template`.`id`,
                 '_',
                 IFNULL(`data_center`.`id`, 0)) AS `temp_zone_pair`,
          `vm_template`.`direct_download` AS `direct_download`,
          `vm_template`.`deploy_as_is` AS `deploy_as_is`,
         `user_data`.`id` AS `user_data_id`,
         `user_data`.`uuid` AS `user_data_uuid`,
         `user_data`.`name` AS `user_data_name`,
         `user_data`.`params` AS `user_data_params`,
         `vm_template`.`user_data_link_policy` AS `user_data_policy`
     FROM
         (((((((((((((`vm_template`
         JOIN `guest_os` ON ((`guest_os`.`id` = `vm_template`.`guest_os_id`)))
         JOIN `account` ON ((`account`.`id` = `vm_template`.`account_id`)))
         JOIN `domain` ON ((`domain`.`id` = `account`.`domain_id`)))
         LEFT JOIN `projects` ON ((`projects`.`project_account_id` = `account`.`id`)))
         LEFT JOIN `vm_template_details` ON ((`vm_template_details`.`template_id` = `vm_template`.`id`)))
         LEFT JOIN `vm_template` `source_template` ON ((`source_template`.`id` = `vm_template`.`source_template_id`)))
         LEFT JOIN `template_store_ref` ON (((`template_store_ref`.`template_id` = `vm_template`.`id`)
             AND (`template_store_ref`.`store_role` = 'Image')
             AND (`template_store_ref`.`destroyed` = 0))))
         LEFT JOIN `vm_template` `parent_template` ON ((`parent_template`.`id` = `vm_template`.`parent_template_id`)))
         LEFT JOIN `image_store` ON ((ISNULL(`image_store`.`removed`)
             AND (`template_store_ref`.`store_id` IS NOT NULL)
             AND (`image_store`.`id` = `template_store_ref`.`store_id`))))
         LEFT JOIN `template_zone_ref` ON (((`template_zone_ref`.`template_id` = `vm_template`.`id`)
             AND ISNULL(`template_store_ref`.`store_id`)
             AND ISNULL(`template_zone_ref`.`removed`))))
         LEFT JOIN `data_center` ON (((`image_store`.`data_center_id` = `data_center`.`id`)
             OR (`template_zone_ref`.`zone_id` = `data_center`.`id`))))
         LEFT JOIN `launch_permission` ON ((`launch_permission`.`template_id` = `vm_template`.`id`)))
         LEFT JOIN `user_data` ON ((`user_data`.`id` = `vm_template`.`user_data_id`))
         LEFT JOIN `resource_tags` ON (((`resource_tags`.`resource_id` = `vm_template`.`id`)
             AND ((`resource_tags`.`resource_type` = 'Template')
             OR (`resource_tags`.`resource_type` = 'ISO')))));

DROP VIEW IF EXISTS `cloud`.`user_vm_view`;
CREATE
    VIEW `user_vm_view` AS
SELECT
    `vm_instance`.`id` AS `id`,
    `vm_instance`.`name` AS `name`,
    `user_vm`.`display_name` AS `display_name`,
    `user_vm`.`user_data` AS `user_data`,
    `account`.`id` AS `account_id`,
    `account`.`uuid` AS `account_uuid`,
    `account`.`account_name` AS `account_name`,
    `account`.`type` AS `account_type`,
    `domain`.`id` AS `domain_id`,
    `domain`.`uuid` AS `domain_uuid`,
    `domain`.`name` AS `domain_name`,
    `domain`.`path` AS `domain_path`,
    `projects`.`id` AS `project_id`,
    `projects`.`uuid` AS `project_uuid`,
    `projects`.`name` AS `project_name`,
    `instance_group`.`id` AS `instance_group_id`,
    `instance_group`.`uuid` AS `instance_group_uuid`,
    `instance_group`.`name` AS `instance_group_name`,
    `vm_instance`.`uuid` AS `uuid`,
    `vm_instance`.`user_id` AS `user_id`,
    `vm_instance`.`last_host_id` AS `last_host_id`,
    `vm_instance`.`vm_type` AS `type`,
    `vm_instance`.`limit_cpu_use` AS `limit_cpu_use`,
    `vm_instance`.`created` AS `created`,
    `vm_instance`.`state` AS `state`,
    `vm_instance`.`update_time` AS `update_time`,
    `vm_instance`.`removed` AS `removed`,
    `vm_instance`.`ha_enabled` AS `ha_enabled`,
    `vm_instance`.`hypervisor_type` AS `hypervisor_type`,
    `vm_instance`.`instance_name` AS `instance_name`,
    `vm_instance`.`guest_os_id` AS `guest_os_id`,
    `vm_instance`.`display_vm` AS `display_vm`,
    `guest_os`.`uuid` AS `guest_os_uuid`,
    `vm_instance`.`pod_id` AS `pod_id`,
    `host_pod_ref`.`uuid` AS `pod_uuid`,
    `vm_instance`.`private_ip_address` AS `private_ip_address`,
    `vm_instance`.`private_mac_address` AS `private_mac_address`,
    `vm_instance`.`vm_type` AS `vm_type`,
    `data_center`.`id` AS `data_center_id`,
    `data_center`.`uuid` AS `data_center_uuid`,
    `data_center`.`name` AS `data_center_name`,
    `data_center`.`is_security_group_enabled` AS `security_group_enabled`,
    `data_center`.`networktype` AS `data_center_type`,
    `host`.`id` AS `host_id`,
    `host`.`uuid` AS `host_uuid`,
    `host`.`name` AS `host_name`,
    `host`.`cluster_id` AS `cluster_id`,
    `host`.`status` AS `host_status`,
    `host`.`resource_state` AS `host_resource_state`,
    `vm_template`.`id` AS `template_id`,
    `vm_template`.`uuid` AS `template_uuid`,
    `vm_template`.`name` AS `template_name`,
    `vm_template`.`display_text` AS `template_display_text`,
    `vm_template`.`enable_password` AS `password_enabled`,
    `iso`.`id` AS `iso_id`,
    `iso`.`uuid` AS `iso_uuid`,
    `iso`.`name` AS `iso_name`,
    `iso`.`display_text` AS `iso_display_text`,
    `service_offering`.`id` AS `service_offering_id`,
    `svc_disk_offering`.`uuid` AS `service_offering_uuid`,
    `disk_offering`.`uuid` AS `disk_offering_uuid`,
    `disk_offering`.`id` AS `disk_offering_id`,
    (CASE
         WHEN ISNULL(`service_offering`.`cpu`) THEN `custom_cpu`.`value`
         ELSE `service_offering`.`cpu`
        END) AS `cpu`,
    (CASE
         WHEN ISNULL(`service_offering`.`speed`) THEN `custom_speed`.`value`
         ELSE `service_offering`.`speed`
        END) AS `speed`,
    (CASE
         WHEN ISNULL(`service_offering`.`ram_size`) THEN `custom_ram_size`.`value`
         ELSE `service_offering`.`ram_size`
        END) AS `ram_size`,
    `backup_offering`.`uuid` AS `backup_offering_uuid`,
    `backup_offering`.`id` AS `backup_offering_id`,
    `svc_disk_offering`.`name` AS `service_offering_name`,
    `disk_offering`.`name` AS `disk_offering_name`,
    `backup_offering`.`name` AS `backup_offering_name`,
    `storage_pool`.`id` AS `pool_id`,
    `storage_pool`.`uuid` AS `pool_uuid`,
    `storage_pool`.`pool_type` AS `pool_type`,
    `volumes`.`id` AS `volume_id`,
    `volumes`.`uuid` AS `volume_uuid`,
    `volumes`.`device_id` AS `volume_device_id`,
    `volumes`.`volume_type` AS `volume_type`,
    `security_group`.`id` AS `security_group_id`,
    `security_group`.`uuid` AS `security_group_uuid`,
    `security_group`.`name` AS `security_group_name`,
    `security_group`.`description` AS `security_group_description`,
    `nics`.`id` AS `nic_id`,
    `nics`.`uuid` AS `nic_uuid`,
    `nics`.`device_id` AS `nic_device_id`,
    `nics`.`network_id` AS `network_id`,
    `nics`.`ip4_address` AS `ip_address`,
    `nics`.`ip6_address` AS `ip6_address`,
    `nics`.`ip6_gateway` AS `ip6_gateway`,
    `nics`.`ip6_cidr` AS `ip6_cidr`,
    `nics`.`default_nic` AS `is_default_nic`,
    `nics`.`gateway` AS `gateway`,
    `nics`.`netmask` AS `netmask`,
    `nics`.`mac_address` AS `mac_address`,
    `nics`.`broadcast_uri` AS `broadcast_uri`,
    `nics`.`isolation_uri` AS `isolation_uri`,
    `vpc`.`id` AS `vpc_id`,
    `vpc`.`uuid` AS `vpc_uuid`,
    `networks`.`uuid` AS `network_uuid`,
    `networks`.`name` AS `network_name`,
    `networks`.`traffic_type` AS `traffic_type`,
    `networks`.`guest_type` AS `guest_type`,
    `user_ip_address`.`id` AS `public_ip_id`,
    `user_ip_address`.`uuid` AS `public_ip_uuid`,
    `user_ip_address`.`public_ip_address` AS `public_ip_address`,
    `ssh_keypairs`.`keypair_name` AS `keypair_name`,
    `resource_tags`.`id` AS `tag_id`,
    `resource_tags`.`uuid` AS `tag_uuid`,
    `resource_tags`.`key` AS `tag_key`,
    `resource_tags`.`value` AS `tag_value`,
    `resource_tags`.`domain_id` AS `tag_domain_id`,
    `domain`.`uuid` AS `tag_domain_uuid`,
    `domain`.`name` AS `tag_domain_name`,
    `resource_tags`.`account_id` AS `tag_account_id`,
    `account`.`account_name` AS `tag_account_name`,
    `resource_tags`.`resource_id` AS `tag_resource_id`,
    `resource_tags`.`resource_uuid` AS `tag_resource_uuid`,
    `resource_tags`.`resource_type` AS `tag_resource_type`,
    `resource_tags`.`customer` AS `tag_customer`,
    `async_job`.`id` AS `job_id`,
    `async_job`.`uuid` AS `job_uuid`,
    `async_job`.`job_status` AS `job_status`,
    `async_job`.`account_id` AS `job_account_id`,
    `affinity_group`.`id` AS `affinity_group_id`,
    `affinity_group`.`uuid` AS `affinity_group_uuid`,
    `affinity_group`.`name` AS `affinity_group_name`,
    `affinity_group`.`description` AS `affinity_group_description`,
    `vm_instance`.`dynamically_scalable` AS `dynamically_scalable`,
    `user_data`.`id` AS `user_data_id`,
    `user_data`.`uuid` AS `user_data_uuid`,
    `user_data`.`name` AS `user_data_name`,
    `user_vm`.`user_data_details` AS `user_data_details`,
    `vm_template`.`user_data_link_policy` AS `user_data_policy`
FROM
    ((((((((((((((((((((((((((((((((((`user_vm`
        JOIN `vm_instance` ON (((`vm_instance`.`id` = `user_vm`.`id`)
            AND ISNULL(`vm_instance`.`removed`))))
        JOIN `account` ON ((`vm_instance`.`account_id` = `account`.`id`)))
        JOIN `domain` ON ((`vm_instance`.`domain_id` = `domain`.`id`)))
        LEFT JOIN `guest_os` ON ((`vm_instance`.`guest_os_id` = `guest_os`.`id`)))
        LEFT JOIN `host_pod_ref` ON ((`vm_instance`.`pod_id` = `host_pod_ref`.`id`)))
        LEFT JOIN `projects` ON ((`projects`.`project_account_id` = `account`.`id`)))
        LEFT JOIN `instance_group_vm_map` ON ((`vm_instance`.`id` = `instance_group_vm_map`.`instance_id`)))
        LEFT JOIN `instance_group` ON ((`instance_group_vm_map`.`group_id` = `instance_group`.`id`)))
        LEFT JOIN `data_center` ON ((`vm_instance`.`data_center_id` = `data_center`.`id`)))
        LEFT JOIN `host` ON ((`vm_instance`.`host_id` = `host`.`id`)))
        LEFT JOIN `vm_template` ON ((`vm_instance`.`vm_template_id` = `vm_template`.`id`)))
        LEFT JOIN `vm_template` `iso` ON ((`iso`.`id` = `user_vm`.`iso_id`)))
        LEFT JOIN `volumes` ON ((`vm_instance`.`id` = `volumes`.`instance_id`)))
        LEFT JOIN `service_offering` ON ((`vm_instance`.`service_offering_id` = `service_offering`.`id`)))
        LEFT JOIN `disk_offering` `svc_disk_offering` ON ((`vm_instance`.`service_offering_id` = `svc_disk_offering`.`id`)))
        LEFT JOIN `disk_offering` ON ((`volumes`.`disk_offering_id` = `disk_offering`.`id`)))
        LEFT JOIN `backup_offering` ON ((`vm_instance`.`backup_offering_id` = `backup_offering`.`id`)))
        LEFT JOIN `storage_pool` ON ((`volumes`.`pool_id` = `storage_pool`.`id`)))
        LEFT JOIN `security_group_vm_map` ON ((`vm_instance`.`id` = `security_group_vm_map`.`instance_id`)))
        LEFT JOIN `security_group` ON ((`security_group_vm_map`.`security_group_id` = `security_group`.`id`)))
        LEFT JOIN `user_data` ON ((`user_data`.`id` = `user_vm`.`user_data_id`)))
        LEFT JOIN `nics` ON (((`vm_instance`.`id` = `nics`.`instance_id`)
            AND ISNULL(`nics`.`removed`))))
        LEFT JOIN `networks` ON ((`nics`.`network_id` = `networks`.`id`)))
        LEFT JOIN `vpc` ON (((`networks`.`vpc_id` = `vpc`.`id`)
            AND ISNULL(`vpc`.`removed`))))
        LEFT JOIN `user_ip_address` ON ((`user_ip_address`.`vm_id` = `vm_instance`.`id`)))
        LEFT JOIN `user_vm_details` `ssh_details` ON (((`ssh_details`.`vm_id` = `vm_instance`.`id`)
            AND (`ssh_details`.`name` = 'SSH.PublicKey'))))
        LEFT JOIN `ssh_keypairs` ON (((`ssh_keypairs`.`public_key` = `ssh_details`.`value`)
            AND (`ssh_keypairs`.`account_id` = `account`.`id`))))
        LEFT JOIN `resource_tags` ON (((`resource_tags`.`resource_id` = `vm_instance`.`id`)
            AND (`resource_tags`.`resource_type` = 'UserVm'))))
        LEFT JOIN `async_job` ON (((`async_job`.`instance_id` = `vm_instance`.`id`)
            AND (`async_job`.`instance_type` = 'VirtualMachine')
            AND (`async_job`.`job_status` = 0))))
        LEFT JOIN `affinity_group_vm_map` ON ((`vm_instance`.`id` = `affinity_group_vm_map`.`instance_id`)))
        LEFT JOIN `affinity_group` ON ((`affinity_group_vm_map`.`affinity_group_id` = `affinity_group`.`id`)))
        LEFT JOIN `user_vm_details` `custom_cpu` ON (((`custom_cpu`.`vm_id` = `vm_instance`.`id`)
            AND (`custom_cpu`.`name` = 'CpuNumber'))))
        LEFT JOIN `user_vm_details` `custom_speed` ON (((`custom_speed`.`vm_id` = `vm_instance`.`id`)
            AND (`custom_speed`.`name` = 'CpuSpeed'))))
        LEFT JOIN `user_vm_details` `custom_ram_size` ON (((`custom_ram_size`.`vm_id` = `vm_instance`.`id`)
        AND (`custom_ram_size`.`name` = 'memory'))));

DROP VIEW IF EXISTS `cloud`.`domain_router_view`;
CREATE VIEW `cloud`.`domain_router_view` AS
select
    vm_instance.id id,
    vm_instance.name name,
    account.id account_id,
    account.uuid account_uuid,
    account.account_name account_name,
    account.type account_type,
    domain.id domain_id,
    domain.uuid domain_uuid,
    domain.name domain_name,
    domain.path domain_path,
    projects.id project_id,
    projects.uuid project_uuid,
    projects.name project_name,
    vm_instance.uuid uuid,
    vm_instance.created created,
    vm_instance.state state,
    vm_instance.removed removed,
    vm_instance.pod_id pod_id,
    vm_instance.instance_name instance_name,
    host_pod_ref.uuid pod_uuid,
    data_center.id data_center_id,
    data_center.uuid data_center_uuid,
    data_center.name data_center_name,
    data_center.networktype data_center_type,
    data_center.dns1 dns1,
    data_center.dns2 dns2,
    data_center.ip6_dns1 ip6_dns1,
    data_center.ip6_dns2 ip6_dns2,
    host.id host_id,
    host.uuid host_uuid,
    host.name host_name,
    host.hypervisor_type,
    host.cluster_id cluster_id,
    host.status host_status,
    host.resource_state host_resource_state,
    vm_template.id template_id,
    vm_template.uuid template_uuid,
    service_offering.id service_offering_id,
    disk_offering.uuid service_offering_uuid,
    disk_offering.name service_offering_name,
    nics.id nic_id,
    nics.uuid nic_uuid,
    nics.network_id network_id,
    nics.ip4_address ip_address,
    nics.ip6_address ip6_address,
    nics.ip6_gateway ip6_gateway,
    nics.ip6_cidr ip6_cidr,
    nics.default_nic is_default_nic,
    nics.gateway gateway,
    nics.netmask netmask,
    nics.mac_address mac_address,
    nics.broadcast_uri broadcast_uri,
    nics.isolation_uri isolation_uri,
    vpc.id vpc_id,
    vpc.uuid vpc_uuid,
    vpc.name vpc_name,
    networks.uuid network_uuid,
    networks.name network_name,
    networks.network_domain network_domain,
    networks.traffic_type traffic_type,
    networks.guest_type guest_type,
    async_job.id job_id,
    async_job.uuid job_uuid,
    async_job.job_status job_status,
    async_job.account_id job_account_id,
    domain_router.template_version template_version,
    domain_router.scripts_version scripts_version,
    domain_router.is_redundant_router is_redundant_router,
    domain_router.redundant_state redundant_state,
    domain_router.stop_pending stop_pending,
    domain_router.role role
from
    `cloud`.`domain_router`
        inner join
    `cloud`.`vm_instance` ON vm_instance.id = domain_router.id
        inner join
    `cloud`.`account` ON vm_instance.account_id = account.id
        inner join
    `cloud`.`domain` ON vm_instance.domain_id = domain.id
        left join
    `cloud`.`host_pod_ref` ON vm_instance.pod_id = host_pod_ref.id
        left join
    `cloud`.`projects` ON projects.project_account_id = account.id
        left join
    `cloud`.`data_center` ON vm_instance.data_center_id = data_center.id
        left join
    `cloud`.`host` ON vm_instance.host_id = host.id
        left join
    `cloud`.`vm_template` ON vm_instance.vm_template_id = vm_template.id
        left join
    `cloud`.`service_offering` ON vm_instance.service_offering_id = service_offering.id
        left join
    `cloud`.`disk_offering` ON vm_instance.service_offering_id = disk_offering.id
        left join
    `cloud`.`nics` ON vm_instance.id = nics.instance_id and nics.removed is null
        left join
    `cloud`.`networks` ON nics.network_id = networks.id
        left join
    `cloud`.`vpc` ON domain_router.vpc_id = vpc.id and vpc.removed is null
        left join
    `cloud`.`async_job` ON async_job.instance_id = vm_instance.id
        and async_job.instance_type = 'DomainRouter'
        and async_job.job_status = 0;


--- Create table for handling console sessions #7094

CREATE TABLE IF NOT EXISTS `cloud`.`console_session` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `uuid` varchar(40) NOT NULL COMMENT 'UUID generated for the session',
    `created` datetime NOT NULL COMMENT 'When the session was created',
    `account_id` bigint(20) unsigned NOT NULL COMMENT 'Account who generated the session',
    `user_id` bigint(20) unsigned NOT NULL COMMENT 'User who generated the session',
    `instance_id` bigint(20) unsigned NOT NULL COMMENT 'VM for which the session was generated',
    `host_id` bigint(20) unsigned NOT NULL COMMENT 'Host where the VM was when the session was generated',
    `acquired` int(1) NOT NULL DEFAULT 0 COMMENT 'True if the session was already used',
    `removed` datetime COMMENT 'When the session was removed/used',
    CONSTRAINT `fk_consolesession__account_id` FOREIGN KEY(`account_id`) REFERENCES `cloud`.`account` (`id`),
    CONSTRAINT `fk_consolesession__user_id` FOREIGN KEY(`user_id`) REFERENCES `cloud`.`user`(`id`),
    CONSTRAINT `fk_consolesession__instance_id` FOREIGN KEY(`instance_id`) REFERENCES `cloud`.`vm_instance`(`id`),
    CONSTRAINT `fk_consolesession__host_id` FOREIGN KEY(`host_id`) REFERENCES `cloud`.`host`(`id`),
    CONSTRAINT `uc_consolesession__uuid` UNIQUE (`uuid`)
);
