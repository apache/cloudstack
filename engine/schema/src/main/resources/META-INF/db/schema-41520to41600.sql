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
-- Schema upgrade from 4.15.2.0 to 4.16.0.0
--;

ALTER TABLE `cloud`.`user_vm` ADD COLUMN `user_vm_type` varchar(255) DEFAULT "UserVM" COMMENT 'Defines the type of UserVM';

-- This is set, so as to ensure that the controller details from the ovf template are adhered to
UPDATE `cloud`.`vm_template` set deploy_as_is = 1 where id = 8;

DELETE FROM `cloud`.`configuration` WHERE name IN ("cloud.kubernetes.cluster.template.name.kvm", "cloud.kubernetes.cluster.template.name.vmware", "cloud.kubernetes.cluster.template.name.xenserver", "cloud.kubernetes.cluster.template.name.hyperv");

ALTER TABLE `cloud`.`kubernetes_cluster` ADD COLUMN `autoscaling_enabled` tinyint(1) unsigned NOT NULL DEFAULT 0;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD COLUMN `minsize` bigint;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD COLUMN `maxsize` bigint;

ALTER TABLE `cloud`.`kubernetes_cluster_vm_map` ADD COLUMN `control_node` tinyint(1) unsigned NOT NULL DEFAULT 0;

-- Adding dynamic scalable flag for service offering table
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `dynamic_scaling_enabled` tinyint(1) unsigned NOT NULL DEFAULT 1  COMMENT 'true(1) if VM needs to be dynamically scalable of cpu or memory';


CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.account','created', 'datetime DEFAULT NULL COMMENT ''date created'' AFTER `state` ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.domain','created', 'datetime DEFAULT NULL COMMENT ''date created'' AFTER `next_child_seq` ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.account','created', 'datetime DEFAULT NULL COMMENT ''date created'' AFTER `state` ');

-- Update name for global configuration user.vm.readonly.ui.details
Update configuration set name='user.vm.readonly.details' where name='user.vm.readonly.ui.details';

-- Update name for global configuration 'user.vm.readonly.ui.details' to 'user.vm.denied.details'
UPDATE `cloud`.`configuration` SET name='user.vm.denied.details' WHERE name='user.vm.blacklisted.details';

-- Update name for global configuration 'blacklisted.routes' to 'denied.routes'
UPDATE `cloud`.`configuration` SET name='denied.routes', description='Routes that are denied, can not be used for Static Routes creation for the VPC Private Gateway' WHERE name='blacklisted.routes';

-- Rename 'master_node_count' to 'control_node_count' in kubernetes_cluster table
ALTER TABLE `cloud`.`kubernetes_cluster` CHANGE master_node_count control_node_count bigint NOT NULL default '0' COMMENT 'the number of the control nodes deployed for this Kubernetes cluster';

UPDATE `cloud`.`domain_router` SET redundant_state = 'PRIMARY' WHERE redundant_state = 'MASTER';

DROP TABLE IF EXISTS `cloud`.`external_bigswitch_vns_devices`;
DROP TABLE IF EXISTS `cloud`.`template_s3_ref`;
DROP TABLE IF EXISTS `cloud`.`template_swift_ref`;
DROP TABLE IF EXISTS `cloud`.`template_ovf_properties`;
DROP TABLE IF EXISTS `cloud`.`op_host_upgrade`;
DROP TABLE IF EXISTS `cloud`.`stack_maid`;
DROP TABLE IF EXISTS `cloud`.`volume_host_ref`;
DROP TABLE IF EXISTS `cloud`.`template_host_ref`;
DROP TABLE IF EXISTS `cloud`.`swift`;

ALTER TABLE `cloud`.`snapshots` DROP FOREIGN KEY `fk_snapshots__s3_id` ;
ALTER TABLE `cloud`.`snapshots` DROP COLUMN `s3_id` ;
DROP TABLE IF EXISTS `cloud`.`s3`;

CREATE TABLE `cloud`.`resource_icon` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `icon` blob COMMENT 'Base64 version of the resource icon',
  `resource_id` bigint unsigned NOT NULL,
  `resource_uuid` varchar(40),
  `resource_type` varchar(255),
  `updated` datetime default NULL,
  `created` datetime default NULL,
  `removed` datetime default NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_resource_icon__uuid` UNIQUE (`uuid`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`annotations` ADD COLUMN `admins_only` tinyint(1) unsigned NOT NULL DEFAULT 1;

-- Add uuid for ssh keypairs
ALTER TABLE `cloud`.`ssh_keypairs` ADD COLUMN `uuid` varchar(40) AFTER `id`;

-- PR#4699 Call procedure `ADD_GUEST_OS_AND_HYPERVISOR_MAPPING` to add new data to guest_os and guest_os_hypervisor.
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 20.04 LTS', 'KVM', 'default', 'Ubuntu 20.04 LTS');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 21.04', 'KVM', 'default', 'Ubuntu 21.04');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'pfSense 2.4', 'KVM', 'default', 'pfSense 2.4');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'OpenBSD 6.7', 'KVM', 'default', 'OpenBSD 6.7');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'OpenBSD 6.8', 'KVM', 'default', 'OpenBSD 6.8');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 8.3', 'KVM', 'default', 'AlmaLinux 8.3');

-- Alter value column of *_details table to prevent NULL values
UPDATE cloud.account_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.account_details MODIFY value varchar(255) NOT NULL;
UPDATE cloud.cluster_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.cluster_details MODIFY value varchar(255) NOT NULL;
UPDATE cloud.data_center_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.data_center_details MODIFY value varchar(1024) NOT NULL;
UPDATE cloud.domain_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.domain_details MODIFY value varchar(255) NOT NULL;
UPDATE cloud.image_store_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.image_store_details MODIFY value varchar(255) NOT NULL;
UPDATE cloud.storage_pool_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.storage_pool_details MODIFY value varchar(255) NOT NULL;
UPDATE cloud.template_deploy_as_is_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.template_deploy_as_is_details MODIFY value text NOT NULL;
UPDATE cloud.user_vm_deploy_as_is_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.user_vm_deploy_as_is_details MODIFY value text NOT NULL;
UPDATE cloud.user_vm_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.user_vm_details MODIFY value varchar(5120) NOT NULL;

ALTER TABLE cloud_usage.usage_network DROP PRIMARY KEY, ADD PRIMARY KEY (`account_id`,`zone_id`,`host_id`,`network_id`,`event_time_millis`);
ALTER TABLE `cloud`.`user_statistics` DROP INDEX `account_id`, ADD UNIQUE KEY `account_id`  (`account_id`,`data_center_id`,`public_ip_address`,`device_id`,`device_type`, `network_id`);
ALTER TABLE `cloud_usage`.`user_statistics` DROP INDEX `account_id`, ADD UNIQUE KEY `account_id`  (`account_id`,`data_center_id`,`public_ip_address`,`device_id`,`device_type`, `network_id`);
