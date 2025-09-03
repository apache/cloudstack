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
-- Schema upgrade from 4.14.0.0 to 4.15.0.0
--;

-- Project roles
CREATE TABLE IF NOT EXISTS `cloud`.`project_role` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `name` varchar(255) COMMENT 'unique name of the dynamic project role',
  `removed` datetime COMMENT 'date removed',
  `description` text COMMENT 'description of the project role',
  `project_id` bigint(20) unsigned COMMENT 'Id of the project to which the role belongs',
  PRIMARY KEY (`id`),
  KEY `i_project_role__name` (`name`),
  UNIQUE KEY (`name`, `project_id`),
  CONSTRAINT `fk_project_role__project_id` FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Project role permissions table
CREATE TABLE IF NOT EXISTS `cloud`.`project_role_permissions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `project_id` bigint(20) unsigned NOT NULL COMMENT 'id of the role',
  `project_role_id` bigint(20) unsigned NOT NULL COMMENT 'id of the role',
  `rule` varchar(255) NOT NULL COMMENT 'rule for the role, api name or wildcard',
  `permission` varchar(255) NOT NULL COMMENT 'access authority, allow or deny',
  `description` text COMMENT 'description of the rule',
  `sort_order` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'permission sort order',
  PRIMARY KEY (`id`),
  KEY `fk_project_role_permissions__project_role_id` (`project_role_id`),
  KEY `i_project_role_permissions__sort_order` (`sort_order`),
  UNIQUE KEY (`project_role_id`, `rule`),
  CONSTRAINT `fk_project_role_permissions__project_id` FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_role_permissions__project_role_id` FOREIGN KEY (`project_role_id`) REFERENCES `project_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Alter project accounts table to include user_id and project_role_id for role based users in projects
ALTER TABLE `cloud`.`project_account`
 ADD COLUMN `user_id` bigint unsigned COMMENT 'ID of user to be added to the project' AFTER `account_id`,
 ADD CONSTRAINT `fk_project_account__user_id` FOREIGN KEY `fk_project_account__user_id`(`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
 ADD COLUMN `project_role_id` bigint unsigned COMMENT 'Project role id' AFTER `project_account_id`,
 ADD CONSTRAINT `fk_project_account__project_role_id` FOREIGN KEY (`project_role_id`) REFERENCES `project_role` (`id`) ON DELETE SET NULL,
 DROP FOREIGN KEY `fk_project_account__account_id`,
 DROP INDEX `account_id`;

ALTER TABLE `cloud`.`project_account`
 ADD CONSTRAINT `fk_project_account__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE ,
 ADD CONSTRAINT `uc_project_account__project_id_account_id_user_id` UNIQUE (`project_id`, `account_id`, `user_id`) ;

-- Alter project invitations table to include user_id for invites sent to specific users of an account
ALTER TABLE `cloud`.`project_invitations`
    ADD COLUMN `user_id` bigint unsigned COMMENT 'ID of user to be added to the project' AFTER `account_id`,
    ADD COLUMN `account_role` varchar(255) NOT NULL DEFAULT 'Regular' COMMENT 'Account role in the project (Owner or Regular)' AFTER `domain_id`,
    ADD COLUMN `project_role_id` bigint unsigned COMMENT 'Project role id' AFTER `account_role`,
    ADD CONSTRAINT `fk_project_invitations__user_id` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    ADD CONSTRAINT `fk_project_invitations__project_role_id` FOREIGN KEY (`project_role_id`) REFERENCES `project_role` (`id`) ON DELETE SET NULL,
    DROP INDEX `project_id`,
    ADD CONSTRAINT `uc_project_invitations__project_id_account_id_user_id` UNIQUE (`project_id`, `account_id`,`user_id`);

-- Fix Debian 10 32-bit hypervisor mappings on VMware, debian10-32bit OS ID in guest_os table is 292, not 282
UPDATE `cloud`.`guest_os_hypervisor` SET guest_os_id=292 WHERE guest_os_id=282 AND hypervisor_type="VMware" AND guest_os_name="debian10Guest";
-- Fix CentOS 32-bit mapping for VMware 5.5 which does not have a centos6Guest but only centosGuest and centos64Guest
UPDATE `cloud`.`guest_os_hypervisor` SET guest_os_name='centosGuest' where hypervisor_type="VMware" and hypervisor_version="5.5" and guest_os_name="centos6Guest";

ALTER TABLE `cloud`.`roles` ADD COLUMN `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'is this a default role';
UPDATE `cloud`.`roles` SET `is_default` = 1 WHERE id IN (1, 2, 3, 4);

-- Updated Default CloudStack roles with read-only and support admin and user roles
INSERT INTO `cloud`.`roles` (`uuid`, `name`, `role_type`, `description`, `is_default`) VALUES (UUID(), 'Read-Only Admin - Default', 'Admin', 'Default read-only admin role', 1);
INSERT INTO `cloud`.`roles` (`uuid`, `name`, `role_type`, `description`, `is_default`) VALUES (UUID(), 'Read-Only User - Default', 'User', 'Default read-only user role', 1);
INSERT INTO `cloud`.`roles` (`uuid`, `name`, `role_type`, `description`, `is_default`) VALUES (UUID(), 'Support Admin - Default', 'Admin', 'Default support admin role', 1);
INSERT INTO `cloud`.`roles` (`uuid`, `name`, `role_type`, `description`, `is_default`) VALUES (UUID(), 'Support User - Default', 'User', 'Default support user role', 1);

-- mysql8 nics table fix for newer distributions
ALTER TABLE `cloud`.`nics` MODIFY COLUMN update_time timestamp DEFAULT CURRENT_TIMESTAMP;

-- Change guest OS name to support default CentOS 5 template in XenServer8.0
UPDATE `cloud`.`guest_os_hypervisor` SET guest_os_name='CentOS 7' where guest_os_id=(SELECT guest_os_id from `cloud`.`vm_template` WHERE unique_name='centos56-x86_64-xen') AND hypervisor_type='Xenserver' AND hypervisor_version='8.0.0';

-- Add XenServer 8.1 hypervisor capabilities
INSERT INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported) values (UUID(), 'XenServer', '8.1.0', 1000, 253, 64, 1);

-- Copy XenServer 8.0 hypervisor guest OS mappings to XenServer8.1
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '8.1.0', guest_os_name, guest_os_id, now(), 0 FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='8.0.0';

CREATE TABLE IF NOT EXISTS `cloud`.`vsphere_storage_policy` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `zone_id` bigint(20) unsigned NOT NULL COMMENT 'id of the zone',
  `policy_id` varchar(255) NOT NULL COMMENT 'the identifier of the Storage Policy in vSphere DataCenter',
  `name` varchar(255) NOT NULL COMMENT 'name of the storage policy',
  `description` text COMMENT 'description of the storage policy',
  `update_time` datetime COMMENT 'last updated when policy imported',
  `removed` datetime COMMENT 'date removed',
  PRIMARY KEY (`id`),
  KEY `fk_vsphere_storage_policy__zone_id` (`zone_id`),
  UNIQUE KEY (`zone_id`, `policy_id`),
  CONSTRAINT `fk_vsphere_storage_policy__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`storage_pool` ADD COLUMN `parent` BIGINT(20) UNSIGNED NOT NULL DEFAULT 0 COMMENT 'ID of the Datastore cluster (storage pool) if this is a child in that Datastore cluster';

-- Add passthrough instruction for appliance deployments
ALTER TABLE `cloud`.`vm_template` ADD COLUMN `deploy_as_is` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the template should be deployed with disks and networks as defined by OVF';

ALTER TABLE `cloud`.`template_spool_ref`
DROP FOREIGN KEY `fk_template_spool_ref__template_id`;

ALTER TABLE `cloud`.`template_spool_ref`
ADD COLUMN `deployment_option` VARCHAR(255) NULL DEFAULT NULL AFTER `updated`,
ADD INDEX `fk_template_spool_ref__template_id_idx` (`template_id` ASC),
ADD UNIQUE INDEX `index_template_spool_configuration` (`pool_id` ASC, `template_id` ASC, `deployment_option` ASC),
DROP INDEX `i_template_spool_ref__template_id__pool_id` ;

ALTER TABLE `cloud`.`template_spool_ref`
ADD CONSTRAINT `fk_template_spool_ref__template_id`
  FOREIGN KEY (`template_id`)
  REFERENCES `cloud`.`vm_template` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

CREATE TABLE `cloud`.`template_deploy_as_is_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `template_id` bigint unsigned NOT NULL COMMENT 'template id',
  `name` varchar(255) NOT NULL,
  `value` TEXT,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_template_deploy_as_is_details__template_id` FOREIGN KEY `fk_template_deploy_as_is_details__template_id`(`template_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`user_vm_deploy_as_is_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vm_id` bigint unsigned NOT NULL COMMENT 'virtual machine id',
  `name` varchar(255) NOT NULL,
  `value` TEXT,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_user_vm_deploy_as_is_details__vm_id` FOREIGN KEY `fk_user_vm_deploy_as_is_details__vm_id`(`vm_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`image_store` ADD COLUMN `readonly` boolean DEFAULT false COMMENT 'defines status of image store';

-- Fix OS category for Guest OS 'Other PV Virtio-SCSI (64-bit)'
UPDATE `cloud`.`guest_os` SET category_id = 7 WHERE id = 275 AND display_name = 'Other PV Virtio-SCSI (64-bit)';

-- Add flag 'hidden' in tables usage_ip_address and cloud_usage
ALTER TABLE `cloud_usage`.`usage_ip_address` ADD COLUMN `is_hidden` smallint(1) NOT NULL DEFAULT '0' COMMENT 'is usage hidden';
ALTER TABLE `cloud_usage`.`cloud_usage` ADD COLUMN `is_hidden` smallint(1) NOT NULL DEFAULT '0' COMMENT 'is usage hidden';

-- Fix Zones are returned in a random order (#3934)
UPDATE `cloud`.`data_center` JOIN (SELECT COUNT(1) AS count FROM `cloud`.`data_center` WHERE `sort_key` != 0) AS tbl_tmp SET `sort_key` = `id` WHERE count = 0;

-- Fix description of volume.stats.interval which is in milliseconds not seconds
UPDATE `cloud`.`configuration` SET `description` = 'Interval (in milliseconds) to report volume statistics' WHERE `name` = 'volume.stats.interval';
