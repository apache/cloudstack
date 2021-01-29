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
DROP VIEW IF EXISTS `cloud`.`project_invitation_view`;
CREATE VIEW `cloud`.`project_invitation_view` AS
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
DROP VIEW IF EXISTS `cloud`.`project_account_view`;
CREATE VIEW `cloud`.`project_account_view` AS
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

-- Alter project_view to incorporate user id
DROP VIEW IF EXISTS `cloud`.`project_view`;
CREATE VIEW `cloud`.`project_view` AS
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

-- Add passthrough instruction for appliance deployments
ALTER TABLE `cloud`.`vm_template` ADD COLUMN `deploy_as_is` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the template should be deployed with disks and networks as defined by OVF';

-- Changes to template_view for both deploying multidisk OVA/vApp as is
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
          `vm_template`.`deploy_as_is` AS `deploy_as_is`
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
         LEFT JOIN `resource_tags` ON (((`resource_tags`.`resource_id` = `vm_template`.`id`)
             AND ((`resource_tags`.`resource_type` = 'Template')
             OR (`resource_tags`.`resource_type` = 'ISO')))));

-- Add mincpu, maxcpu, minmemory and maxmemory to the view supporting constrained offerings
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

DROP VIEW IF EXISTS `cloud`.`image_store_view`;
CREATE VIEW `cloud`.`image_store_view` AS
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

-- OVF configured OS while registering deploy-as-is templates Linux 3.x Kernel OS
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (305, UUID(), 11, 'OVF Configured OS', now());
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (306, UUID(), 2, 'Linux 3.x Kernel (64 bit)', now());
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (307, UUID(), 2, 'Linux 3.x Kernel (32 bit)', now());

INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.0', 'other3xLinux64Guest', 306, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'other3xLinux64Guest', 306, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.7', 'other3xLinux64Guest', 306, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.7.1', 'other3xLinux64Guest', 306, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.7.2', 'other3xLinux64Guest', 306, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.7.3', 'other3xLinux64Guest', 306, now(), 0);

INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.0', 'other3xLinuxGuest', 307, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'other3xLinuxGuest', 307, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.7', 'other3xLinuxGuest', 307, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.7.1', 'other3xLinuxGuest', 307, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.7.2', 'other3xLinuxGuest', 307, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.7.3', 'other3xLinuxGuest', 307, now(), 0);


-- Add amazonlinux as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (308, UUID(), 7, 'Amazon Linux 2 (64 bit)', now());
-- Amazonlinux VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'amazonlinux2_64Guest', 308, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'amazonlinux2_64Guest', 308, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'amazonlinux2_64Guest', 308, now(), 0);


-- Add asianux4 32 as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (309, UUID(), 7, 'Asianux Server 4 (32 bit)', now());
-- asianux4 VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'asianux4Guest', 309, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'asianux4Guest', 309, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'asianux4Guest', 309, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'asianux4Guest', 309, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'asianux4Guest', 309, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'asianux4Guest', 309, now(), 0);


-- Add asianux4 64 as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (310, UUID(), 7, 'Asianux Server 4 (64 bit)', now());
-- asianux4 VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'asianux4_64Guest', 310, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'asianux4_64Guest', 310, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'asianux4_64Guest', 310, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'asianux4_64Guest', 310, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'asianux4_64Guest', 310, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'asianux4_64Guest', 310, now(), 0);


-- Add asianux5 32 as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (311, UUID(), 7, 'Asianux Server 5 (32 bit)', now());
-- asianux5 VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'asianux5Guest', 311, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'asianux5Guest', 311, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'asianux5Guest', 311, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'asianux5Guest', 311, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'asianux5Guest', 311, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'asianux5Guest', 311, now(), 0);


-- Add asianux5 64 as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (312, UUID(), 7, 'Asianux Server 5 (64 bit)', now());
-- asianux5 VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'asianux5_64Guest', 312, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'asianux5_64Guest', 312, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'asianux5_64Guest', 312, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'asianux5_64Guest', 312, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'asianux5_64Guest', 312, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'asianux5_64Guest', 312, now(), 0);


-- Add asianux7 32 as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (313, UUID(), 7, 'Asianux Server 7 (32 bit)', now());
-- asianux7 VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'asianux7Guest', 313, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'asianux7Guest', 313, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'asianux7Guest', 313, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'asianux7Guest', 313, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'asianux7Guest', 313, now(), 0);


-- Add asianux7 64 as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (314, UUID(), 7, 'Asianux Server 7 (64 bit)', now());
-- asianux7 VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'asianux7_64Guest', 314, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'asianux7_64Guest', 314, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'asianux7_64Guest', 314, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'asianux7_64Guest', 314, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'asianux7_64Guest', 314, now(), 0);


-- Add asianux8 as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (315, UUID(), 7, 'Asianux Server 8 (64 bit)', now());
-- asianux8 VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'asianux8_64Guest', 315, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'asianux8_64Guest', 315, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'asianux8_64Guest', 315, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'asianux8_64Guest', 315, now(), 0);


-- Add eComStation 2.0   as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (316, UUID(), 7, 'eComStation 2.0', now());
-- eComStation 2.0 VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'eComStation2Guest', 316, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'eComStation2Guest', 316, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'eComStation2Guest', 316, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'eComStation2Guest', 316, now(), 0);

-- Add macOS 10.13 (64 bit)  as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (317, UUID(), 7, 'macOS 10.13 (64 bit)', now());
-- macOS 10.13 (64 bit)  VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'darwin17_64Guest', 317, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'darwin17_64Guest', 317, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'darwin17_64Guest', 317, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'darwin17_64Guest', 317, now(), 0);

-- Add macOS 10.14 (64 bit)  as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (318, UUID(), 7, 'macOS 10.14 (64 bit)', now());
-- macOS 10.14 (64 bit) VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'darwin18_64Guest', 318, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'darwin18_64Guest', 318, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'darwin18_64Guest', 318, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'darwin18_64Guest', 318, now(), 0);


-- Add Fedora Linux (64 bit)   as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (319, UUID(), 7, 'Fedora Linux (64 bit)', now());
-- Fedora Linux (64 bit)  VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'fedora64Guest', 319, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'fedora64Guest', 319, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'fedora64Guest', 319, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'fedora64Guest', 319, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'fedora64Guest', 319, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'fedora64Guest', 319, now(), 0);


-- Add Fedora Linux   as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (320, UUID(), 7, 'Fedora Linux', now());
-- Fedora Linux  VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'fedoraGuest', 320, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'fedoraGuest', 320, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'fedoraGuest', 320, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'fedoraGuest', 320, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'fedoraGuest', 320, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'fedoraGuest', 320, now(), 0);

-- Add Mandrake Linux   as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (321, UUID(), 7, 'Mandrake Linux', now());
-- Mandrake Linux  VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'mandrakeGuest', 321, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'mandrakeGuest', 321, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'mandrakeGuest', 321, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'mandrakeGuest', 321, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'mandrakeGuest', 321, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'mandrakeGuest', 321, now(), 0);

-- Add Mandriva Linux (64 bit)  as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (322, UUID(), 7, 'Mandriva Linux (64 bit)', now());
-- Mandriva Linux (64 bit)  VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'mandriva64Guest', 322, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'mandriva64Guest', 322, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'mandriva64Guest', 322, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'mandriva64Guest', 322, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'mandriva64Guest', 322, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'mandriva64Guest', 322, now(), 0);


-- Add Mandriva Linux  as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (323, UUID(), 7, 'Mandriva Linux', now());
-- Mandriva Linux  VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'mandrivaGuest', 323, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'mandrivaGuest', 323, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'mandrivaGuest', 323, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'mandrivaGuest', 323, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'mandrivaGuest', 323, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'mandrivaGuest', 323, now(), 0);


-- Add SCO OpenServer 5   as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (324, UUID(), 7, 'SCO OpenServer 5', now());
-- SCO OpenServer 5   VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'openServer5Guest', 324, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'openServer5Guest', 324, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'openServer5Guest', 324, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'openServer5Guest', 324, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'openServer5Guest', 324, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'openServer5Guest', 324, now(), 0);


-- Add SCO OpenServer 6   as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (325, UUID(), 7, 'SCO OpenServer 6', now());
-- SCO OpenServer 6   VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'openServer6Guest', 325, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'openServer6Guest', 325, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'openServer6Guest', 325, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'openServer6Guest', 325, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'openServer6Guest', 325, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'openServer6Guest', 325, now(), 0);



-- Add OpenSUSE Linux (64 bit)    as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (326, UUID(), 7, 'OpenSUSE Linux (64 bit)', now());
-- OpenSUSE Linux (64 bit)   VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'opensuse64Guest', 326, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'opensuse64Guest', 326, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'opensuse64Guest', 326, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'opensuse64Guest', 326, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'opensuse64Guest', 326, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'opensuse64Guest', 326, now(), 0);


-- Add SCO OpenServer 6   as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (327, UUID(), 7, 'SCO OpenServer 6', now());
-- SCO OpenServer 6   VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'opensuseGuest', 327, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'opensuseGuest', 327, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'opensuseGuest', 327, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'opensuseGuest', 327, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'opensuseGuest', 327, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'opensuseGuest', 327, now(), 0);


-- Add Solaris 11 (64 bit)    as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (328, UUID(), 7, 'Solaris 11 (64 bit)', now());
-- Solaris 11 (64 bit)    VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'solaris11_64Guest', 328, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'solaris11_64Guest', 328, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'solaris11_64Guest', 328, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'solaris11_64Guest', 328, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'solaris11_64Guest', 328, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'solaris11_64Guest', 328, now(), 0);

-- Add  VMware Photon (64 bit)     as support guest os
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (329, UUID(), 7, 'VMware Photon (64 bit)', now());
-- VMware Photon (64 bit)    VMWare guest os mapping
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.5', 'vmwarePhoton64Guest', 329, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7', 'vmwarePhoton64Guest', 329, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.1', 'vmwarePhoton64Guest', 329, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.2', 'vmwarePhoton64Guest', 329, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.7.3', 'vmwarePhoton64Guest', 329, now(), 0);

-- Fix OS category for Guest OS 'Other PV Virtio-SCSI (64-bit)'
UPDATE `cloud`.`guest_os` SET category_id = 7 WHERE id = 275 AND display_name = 'Other PV Virtio-SCSI (64-bit)';

-- Add flag 'hidden' in tables usage_ip_address and cloud_usage
ALTER TABLE `cloud_usage`.`usage_ip_address` ADD COLUMN `is_hidden` smallint(1) NOT NULL DEFAULT '0' COMMENT 'is usage hidden';
ALTER TABLE `cloud_usage`.`cloud_usage` ADD COLUMN `is_hidden` smallint(1) NOT NULL DEFAULT '0' COMMENT 'is usage hidden';

-- Fix Zones are returned in a random order (#3934)
UPDATE `cloud`.`data_center` JOIN (SELECT COUNT(1) AS count FROM `cloud`.`data_center` WHERE `sort_key` != 0) AS tbl_tmp SET `sort_key` = `id` WHERE count = 0;

-- Fix description of volume.stats.interval which is in milliseconds not seconds
UPDATE `cloud`.`configuration` SET `description` = 'Interval (in milliseconds) to report volume statistics' WHERE `name` = 'volume.stats.interval';