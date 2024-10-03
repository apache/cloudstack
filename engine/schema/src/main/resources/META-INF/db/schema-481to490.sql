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
-- Schema upgrade from 4.8.1 to 4.9.0;
--;

ALTER TABLE `event` ADD INDEX `archived` (`archived`);
ALTER TABLE `event` ADD INDEX `state` (`state`);

DROP VIEW IF EXISTS `cloud`.`template_view`;
CREATE
VIEW `template_view` AS
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
                IFNULL(`data_center`.`id`, 0)) AS `temp_zone_pair`
    FROM
        ((((((((((((`vm_template`
        JOIN `guest_os` ON ((`guest_os`.`id` = `vm_template`.`guest_os_id`)))
        JOIN `account` ON ((`account`.`id` = `vm_template`.`account_id`)))
        JOIN `domain` ON ((`domain`.`id` = `account`.`domain_id`)))
        LEFT JOIN `projects` ON ((`projects`.`project_account_id` = `account`.`id`)))
        LEFT JOIN `vm_template_details` ON ((`vm_template_details`.`template_id` = `vm_template`.`id`)))
        LEFT JOIN `vm_template` `source_template` ON ((`source_template`.`id` = `vm_template`.`source_template_id`)))
        LEFT JOIN `template_store_ref` ON (((`template_store_ref`.`template_id` = `vm_template`.`id`)
            AND (`template_store_ref`.`store_role` = 'Image')
            AND (`template_store_ref`.`destroyed` = 0))))
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

DROP VIEW IF EXISTS `cloud`.`volume_view`;
CREATE
VIEW `volume_view` AS
    SELECT
        `volumes`.`id` AS `id`,
        `volumes`.`uuid` AS `uuid`,
        `volumes`.`name` AS `name`,
        `volumes`.`device_id` AS `device_id`,
        `volumes`.`volume_type` AS `volume_type`,
        `volumes`.`provisioning_type` AS `provisioning_type`,
        `volumes`.`size` AS `size`,
        `volumes`.`min_iops` AS `min_iops`,
        `volumes`.`max_iops` AS `max_iops`,
        `volumes`.`created` AS `created`,
        `volumes`.`state` AS `state`,
        `volumes`.`attached` AS `attached`,
        `volumes`.`removed` AS `removed`,
        `volumes`.`pod_id` AS `pod_id`,
        `volumes`.`display_volume` AS `display_volume`,
        `volumes`.`format` AS `format`,
        `volumes`.`path` AS `path`,
        `volumes`.`chain_info` AS `chain_info`,
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
        `data_center`.`networktype` AS `data_center_type`,
        `vm_instance`.`id` AS `vm_id`,
        `vm_instance`.`uuid` AS `vm_uuid`,
        `vm_instance`.`name` AS `vm_name`,
        `vm_instance`.`state` AS `vm_state`,
        `vm_instance`.`vm_type` AS `vm_type`,
        `user_vm`.`display_name` AS `vm_display_name`,
        `volume_store_ref`.`size` AS `volume_store_size`,
        `volume_store_ref`.`download_pct` AS `download_pct`,
        `volume_store_ref`.`download_state` AS `download_state`,
        `volume_store_ref`.`error_str` AS `error_str`,
        `volume_store_ref`.`created` AS `created_on_store`,
        `disk_offering`.`id` AS `disk_offering_id`,
        `disk_offering`.`uuid` AS `disk_offering_uuid`,
        `disk_offering`.`name` AS `disk_offering_name`,
        `disk_offering`.`display_text` AS `disk_offering_display_text`,
        `disk_offering`.`use_local_storage` AS `use_local_storage`,
        `disk_offering`.`system_use` AS `system_use`,
        `disk_offering`.`bytes_read_rate` AS `bytes_read_rate`,
        `disk_offering`.`bytes_write_rate` AS `bytes_write_rate`,
        `disk_offering`.`iops_read_rate` AS `iops_read_rate`,
        `disk_offering`.`iops_write_rate` AS `iops_write_rate`,
        `disk_offering`.`cache_mode` AS `cache_mode`,
        `storage_pool`.`id` AS `pool_id`,
        `storage_pool`.`uuid` AS `pool_uuid`,
        `storage_pool`.`name` AS `pool_name`,
        `cluster`.`hypervisor_type` AS `hypervisor_type`,
        `vm_template`.`id` AS `template_id`,
        `vm_template`.`uuid` AS `template_uuid`,
        `vm_template`.`extractable` AS `extractable`,
        `vm_template`.`type` AS `template_type`,
        `vm_template`.`name` AS `template_name`,
        `vm_template`.`display_text` AS `template_display_text`,
        `iso`.`id` AS `iso_id`,
        `iso`.`uuid` AS `iso_uuid`,
        `iso`.`name` AS `iso_name`,
        `iso`.`display_text` AS `iso_display_text`,
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
        `async_job`.`account_id` AS `job_account_id`
    FROM
        ((((((((((((((`volumes`
        JOIN `account` ON ((`volumes`.`account_id` = `account`.`id`)))
        JOIN `domain` ON ((`volumes`.`domain_id` = `domain`.`id`)))
        LEFT JOIN `projects` ON ((`projects`.`project_account_id` = `account`.`id`)))
        LEFT JOIN `data_center` ON ((`volumes`.`data_center_id` = `data_center`.`id`)))
        LEFT JOIN `vm_instance` ON ((`volumes`.`instance_id` = `vm_instance`.`id`)))
        LEFT JOIN `user_vm` ON ((`user_vm`.`id` = `vm_instance`.`id`)))
        LEFT JOIN `volume_store_ref` ON ((`volumes`.`id` = `volume_store_ref`.`volume_id`)))
        LEFT JOIN `disk_offering` ON ((`volumes`.`disk_offering_id` = `disk_offering`.`id`)))
        LEFT JOIN `storage_pool` ON ((`volumes`.`pool_id` = `storage_pool`.`id`)))
        LEFT JOIN `cluster` ON ((`storage_pool`.`cluster_id` = `cluster`.`id`)))
        LEFT JOIN `vm_template` ON ((`volumes`.`template_id` = `vm_template`.`id`)))
        LEFT JOIN `vm_template` `iso` ON ((`iso`.`id` = `volumes`.`iso_id`)))
        LEFT JOIN `resource_tags` ON (((`resource_tags`.`resource_id` = `volumes`.`id`)
            AND (`resource_tags`.`resource_type` = 'Volume'))))
        LEFT JOIN `async_job` ON (((`async_job`.`instance_id` = `volumes`.`id`)
            AND (`async_job`.`instance_type` = 'Volume')
            AND (`async_job`.`job_status` = 0))));

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
        `svc_disk_offering`.`name` AS `service_offering_name`,
        `disk_offering`.`name` AS `disk_offering_name`,
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
        `vm_instance`.`dynamically_scalable` AS `dynamically_scalable`
    FROM
        ((((((((((((((((((((((((((((((((`user_vm`
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
        LEFT JOIN `service_offering` ON ((`vm_instance`.`service_offering_id` = `service_offering`.`id`)))
        LEFT JOIN `disk_offering` `svc_disk_offering` ON ((`vm_instance`.`service_offering_id` = `svc_disk_offering`.`id`)))
        LEFT JOIN `disk_offering` ON ((`vm_instance`.`disk_offering_id` = `disk_offering`.`id`)))
        LEFT JOIN `volumes` ON ((`vm_instance`.`id` = `volumes`.`instance_id`)))
        LEFT JOIN `storage_pool` ON ((`volumes`.`pool_id` = `storage_pool`.`id`)))
        LEFT JOIN `security_group_vm_map` ON ((`vm_instance`.`id` = `security_group_vm_map`.`instance_id`)))
        LEFT JOIN `security_group` ON ((`security_group_vm_map`.`security_group_id` = `security_group`.`id`)))
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

-- Add cluster.storage.operations.exclude property
INSERT INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `description`, `default_value`, `updated`, `scope`, `is_dynamic`) VALUES ('Advanced', 'DEFAULT', 'CapacityManager', 'cluster.storage.operations.exclude', 'Exclude cluster from storage operations', 'false', now(), 'Cluster', '1');

----- 3) Missing indexes (Add indexes to avoid full table scans)
ALTER TABLE `cloud`.`op_it_work` ADD INDEX `i_type_and_updated` (`type` ASC, `updated_at` ASC);
ALTER TABLE `cloud`.`vm_root_disk_tags` ADD INDEX `i_vm_id` (`vm_id` ASC);
ALTER TABLE `cloud`.`vm_compute_tags` ADD INDEX `i_vm_id` (`vm_id` ASC);
ALTER TABLE `cloud`.`vm_network_map` ADD INDEX `i_vm_id` (`vm_id` ASC);
ALTER TABLE `cloud`.`ssh_keypairs` ADD INDEX `i_public_key` (`public_key` (64) ASC);
ALTER TABLE `cloud`.`user_vm_details` ADD INDEX `i_name_vm_id` (`vm_id` ASC, `name` ASC);
ALTER TABLE `cloud`.`instance_group` ADD INDEX `i_name` (`name` ASC);

----- 4) Some views query (Change view to improve account retrieval speed)
CREATE OR REPLACE
VIEW `account_vmstats_view` AS
    SELECT
        `vm_instance`.`account_id` AS `account_id`,
        `vm_instance`.`state` AS `state`,
        COUNT(0) AS `vmcount`
    FROM
        `vm_instance`
    WHERE
        (`vm_instance`.`vm_type` = 'User' and `vm_instance`.`removed` is NULL)
    GROUP BY `vm_instance`.`account_id` , `vm_instance`.`state`;
-- End CLOUDSTACK-9340

-- Dynamic roles
CREATE TABLE IF NOT EXISTS `cloud`.`roles` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `name` varchar(255) COMMENT 'unique name of the dynamic role',
  `role_type` varchar(255) NOT NULL COMMENT 'the type of the role',
  `removed` datetime COMMENT 'date removed',
  `description` text COMMENT 'description of the role',
  PRIMARY KEY (`id`),
  KEY `i_roles__name` (`name`),
  KEY `i_roles__role_type` (`role_type`),
  UNIQUE KEY (`name`, `role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`role_permissions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `role_id` bigint(20) unsigned NOT NULL COMMENT 'id of the role',
  `rule` varchar(255) NOT NULL COMMENT 'rule for the role, api name or wildcard',
  `permission` varchar(255) NOT NULL COMMENT 'access authority, allow or deny',
  `description` text COMMENT 'description of the rule',
  `sort_order` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'permission sort order',
  PRIMARY KEY (`id`),
  KEY `fk_role_permissions__role_id` (`role_id`),
  KEY `i_role_permissions__sort_order` (`sort_order`),
  UNIQUE KEY (`role_id`, `rule`),
  CONSTRAINT `fk_role_permissions__role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Default CloudStack roles
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (1, UUID(), 'Root Admin', 'Admin', 'Default root admin role') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (2, UUID(), 'Resource Admin', 'ResourceAdmin', 'Default resource admin role') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (3, UUID(), 'Domain Admin', 'DomainAdmin', 'Default domain admin role') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (4, UUID(), 'User', 'User', 'Default Root Admin role') ON DUPLICATE KEY UPDATE name=name;

-- Out-of-band management
CREATE TABLE IF NOT EXISTS `cloud`.`oobm` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `host_id` bigint(20) unsigned DEFAULT NULL COMMENT 'foreign key to host',
  `enabled` int(1) unsigned DEFAULT '0' COMMENT 'is out-of-band management enabled for host',
  `power_state` varchar(32) DEFAULT 'Disabled' COMMENT 'out-of-band management power status',
  `driver` varchar(32) DEFAULT NULL COMMENT 'out-of-band management driver',
  `address` varchar(255) DEFAULT NULL COMMENT 'out-of-band management interface address',
  `port` int(10) unsigned DEFAULT NULL COMMENT 'out-of-band management interface port',
  `username` varchar(255) DEFAULT NULL COMMENT 'out-of-band management interface username',
  `password` varchar(255) DEFAULT NULL COMMENT 'out-of-band management interface password',
  `update_count` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT 'atomic increase count making status update operation atomical',
  `update_time` datetime COMMENT 'last power state update datetime',
  `mgmt_server_id` bigint(20) unsigned DEFAULT NULL COMMENT 'management server id which owns out-of-band management for the host',
  PRIMARY KEY (`id`),
  KEY `fk_oobm__host_id` (`host_id`),
  KEY `i_oobm__enabled` (`enabled`),
  KEY `i_oobm__power_state` (`power_state`),
  KEY `i_oobm__update_time` (`update_time`),
  KEY `i_oobm__mgmt_server_id` (`mgmt_server_id`),
  CONSTRAINT `fk_oobm__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Out-of-band management
DROP VIEW IF EXISTS `cloud`.`host_view`;
CREATE VIEW `cloud`.`host_view` AS
    select
        host.id,
        host.uuid,
        host.name,
        host.status,
        host.disconnected,
        host.type,
        host.private_ip_address,
        host.version,
        host.hypervisor_type,
        host.hypervisor_version,
        host.capabilities,
        host.last_ping,
        host.created,
        host.removed,
        host.resource_state,
        host.mgmt_server_id,
        host.cpu_sockets,
        host.cpus,
        host.speed,
        host.ram,
        cluster.id cluster_id,
        cluster.uuid cluster_uuid,
        cluster.name cluster_name,
        cluster.cluster_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_type,
        host_pod_ref.id pod_id,
        host_pod_ref.uuid pod_uuid,
        host_pod_ref.name pod_name,
        host_tags.tag,
        guest_os_category.id guest_os_category_id,
        guest_os_category.uuid guest_os_category_uuid,
        guest_os_category.name guest_os_category_name,
        mem_caps.used_capacity memory_used_capacity,
        mem_caps.reserved_capacity memory_reserved_capacity,
        cpu_caps.used_capacity cpu_used_capacity,
        cpu_caps.reserved_capacity cpu_reserved_capacity,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id,
        oobm.enabled AS `oobm_enabled`,
        oobm.power_state AS `oobm_power_state`
    from
        `cloud`.`host`
            left join
        `cloud`.`cluster` ON host.cluster_id = cluster.id
            left join
        `cloud`.`data_center` ON host.data_center_id = data_center.id
            left join
        `cloud`.`host_pod_ref` ON host.pod_id = host_pod_ref.id
            left join
        `cloud`.`host_details` ON host.id = host_details.host_id
            and host_details.name = 'guest.os.category.id'
            left join
        `cloud`.`guest_os_category` ON guest_os_category.id = CONVERT( host_details.value , UNSIGNED)
            left join
        `cloud`.`host_tags` ON host_tags.host_id = host.id
            left join
        `cloud`.`op_host_capacity` mem_caps ON host.id = mem_caps.host_id
            and mem_caps.capacity_type = 0
            left join
        `cloud`.`op_host_capacity` cpu_caps ON host.id = cpu_caps.host_id
            and cpu_caps.capacity_type = 1
            left join
        `cloud`.`async_job` ON async_job.instance_id = host.id
            and async_job.instance_type = 'Host'
            and async_job.job_status = 0
            left join
        `cloud`.`oobm` ON oobm.host_id = host.id;

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centosGuest', 171, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centosGuest', 171, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centosGuest', 171, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centosGuest', 171, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centosGuest', 171, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centos64Guest', 172, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centos64Guest', 172, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centos64Guest', 172, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centos64Guest', 172, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centos64Guest', 172, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centosGuest', 177, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centosGuest', 177, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centosGuest', 177, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centosGuest', 177, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centosGuest', 177, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centos64Guest', 178, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centos64Guest', 178, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centos64Guest', 178, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centos64Guest', 178, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centos64Guest', 178, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centosGuest', 179, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centosGuest', 179, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centosGuest', 179, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centosGuest', 179, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centosGuest', 179, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centos64Guest', 180, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centos64Guest', 180, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centos64Guest', 180, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centos64Guest', 180, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centos64Guest', 180, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centosGuest', 181, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centosGuest', 181, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centosGuest', 181, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centosGuest', 181, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centosGuest', 181, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centos64Guest', 182, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centos64Guest', 182, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centos64Guest', 182, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centos64Guest', 182, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centos64Guest', 182, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centosGuest', 227, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centosGuest', 227, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centosGuest', 227, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centosGuest', 227, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centosGuest', 227, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centos64Guest', 228, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centos64Guest', 228, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centos64Guest', 228, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centos64Guest', 228, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centos64Guest', 228, now(), 0);
