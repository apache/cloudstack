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
    `passphrase` varchar(64) DEFAULT NULL,
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
