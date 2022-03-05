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
