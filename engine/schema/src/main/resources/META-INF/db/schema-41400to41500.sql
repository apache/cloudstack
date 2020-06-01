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

-- Alter project_invitation_view to incorporate user_id as a field
ALTER VIEW `cloud`.`project_invitation_view` AS
    select
        project_invitations.id,
        project_invitations.uuid,
        project_invitations.email,
        project_invitations.created,
        project_invitations.state,
        project_invitations.project_role_id,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name,
        account.type account_type,
        user.id user_id,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`project_invitations`
            left join
        `cloud`.`account` ON project_invitations.account_id = account.id
            left join
        `cloud`.`domain` ON project_invitations.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.id = project_invitations.project_id
            left join
        `cloud`.`user` ON project_invitations.user_id = user.id;

-- Alter project_account_view to incorporate user id
ALTER VIEW `cloud`.`project_account_view` AS
    select
        project_account.id,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name,
        account.type account_type,
        user.id user_id,
        user.uuid user_uuid,
        user.username user_name,
        project_account.account_role,
        project_role.id project_role_id,
        project_role.uuid project_role_uuid,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`project_account`
            inner join
        `cloud`.`account` ON project_account.account_id = account.id
            inner join
        `cloud`.`domain` ON account.domain_id = domain.id
            inner join
        `cloud`.`projects` ON projects.id = project_account.project_id
            left join
        `cloud`.`project_role` ON project_account.project_role_id = project_role.id
            left join
        `cloud`.`user` ON (project_account.user_id = user.id);

ALTER VIEW `cloud`.`project_view` AS
    select
        projects.id,
        projects.uuid,
        projects.name,
        projects.display_text,
        projects.state,
        projects.removed,
        projects.created,
        projects.project_account_id,
        account.account_name owner,
        pacct.account_id,
        pacct.user_id,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`projects`
            inner join
        `cloud`.`domain` ON projects.domain_id = domain.id
            inner join
        `cloud`.`project_account` ON projects.id = project_account.project_id
            and project_account.account_role = 'Admin'
            inner join
        `cloud`.`account` ON account.id = project_account.account_id
            left join
        `cloud`.`project_account` pacct ON projects.id = pacct.project_id;

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
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported) values (UUID(), 'XenServer', '8.1.0', 1000, 253, 64, 1);

-- Copy XenServer 8.0 hypervisor guest OS mappings to XenServer8.1
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '8.1.0', guest_os_name, guest_os_id, utc_timestamp(), 0 FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='8.0.0';

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

-- Added parent column to support datastore clusters in vmware vsphere
DROP VIEW IF EXISTS `cloud`.`storage_pool_view`;
CREATE VIEW `cloud`.`storage_pool_view` AS
    SELECT
        `storage_pool`.`id` AS `id`,
        `storage_pool`.`uuid` AS `uuid`,
        `storage_pool`.`name` AS `name`,
        `storage_pool`.`status` AS `status`,
        `storage_pool`.`path` AS `path`,
        `storage_pool`.`pool_type` AS `pool_type`,
        `storage_pool`.`host_address` AS `host_address`,
        `storage_pool`.`created` AS `created`,
        `storage_pool`.`removed` AS `removed`,
        `storage_pool`.`capacity_bytes` AS `capacity_bytes`,
        `storage_pool`.`capacity_iops` AS `capacity_iops`,
        `storage_pool`.`scope` AS `scope`,
        `storage_pool`.`hypervisor` AS `hypervisor`,
        `storage_pool`.`storage_provider_name` AS `storage_provider_name`,
        `storage_pool`.`parent` AS `parent`,
        `cluster`.`id` AS `cluster_id`,
        `cluster`.`uuid` AS `cluster_uuid`,
        `cluster`.`name` AS `cluster_name`,
        `cluster`.`cluster_type` AS `cluster_type`,
        `data_center`.`id` AS `data_center_id`,
        `data_center`.`uuid` AS `data_center_uuid`,
        `data_center`.`name` AS `data_center_name`,
        `data_center`.`networktype` AS `data_center_type`,
        `host_pod_ref`.`id` AS `pod_id`,
        `host_pod_ref`.`uuid` AS `pod_uuid`,
        `host_pod_ref`.`name` AS `pod_name`,
        `storage_pool_tags`.`tag` AS `tag`,
        `op_host_capacity`.`used_capacity` AS `disk_used_capacity`,
        `op_host_capacity`.`reserved_capacity` AS `disk_reserved_capacity`,
        `async_job`.`id` AS `job_id`,
        `async_job`.`uuid` AS `job_uuid`,
        `async_job`.`job_status` AS `job_status`,
        `async_job`.`account_id` AS `job_account_id`
    FROM
        ((((((`storage_pool`
        LEFT JOIN `cluster` ON ((`storage_pool`.`cluster_id` = `cluster`.`id`)))
        LEFT JOIN `data_center` ON ((`storage_pool`.`data_center_id` = `data_center`.`id`)))
        LEFT JOIN `host_pod_ref` ON ((`storage_pool`.`pod_id` = `host_pod_ref`.`id`)))
        LEFT JOIN `storage_pool_tags` ON (((`storage_pool_tags`.`pool_id` = `storage_pool`.`id`))))
        LEFT JOIN `op_host_capacity` ON (((`storage_pool`.`id` = `op_host_capacity`.`host_id`)
            AND (`op_host_capacity`.`capacity_type` IN (3 , 9)))))
        LEFT JOIN `async_job` ON (((`async_job`.`instance_id` = `storage_pool`.`id`)
            AND (`async_job`.`instance_type` = 'StoragePool')
            AND (`async_job`.`job_status` = 0))));

ALTER TABLE `cloud`.`image_store` ADD COLUMN `readonly` boolean DEFAULT false COMMENT 'defines status of image store';

ALTER VIEW `cloud`.`image_store_view` AS
    select
        image_store.id,
        image_store.uuid,
        image_store.name,
        image_store.image_provider_name,
        image_store.protocol,
        image_store.url,
        image_store.scope,
        image_store.role,
        image_store.readonly,
        image_store.removed,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        image_store_details.name detail_name,
        image_store_details.value detail_value
    from
        `cloud`.`image_store`
            left join
        `cloud`.`data_center` ON image_store.data_center_id = data_center.id
            left join
        `cloud`.`image_store_details` ON image_store_details.store_id = image_store.id;
