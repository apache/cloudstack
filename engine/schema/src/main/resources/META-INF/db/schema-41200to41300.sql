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
-- Schema upgrade from 4.12.0.0 to 4.13.0.0
--;

-- Add support for VMware 6.7
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '6.7', 128, 0, 13, 32, 1, 1);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '6.7', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='6.5';

-- XenServer 7.1.2
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, storage_motion_supported) values (UUID(), 'XenServer', '7.1.2', 500, 13, 1);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '7.1.2', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='7.1.0';

-- XenServer 7.6
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, storage_motion_supported) values (UUID(), 'XenServer', '7.6.0', 500, 13, 1);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '7.6.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='7.5.0';

-- DPDK client and server mode support
ALTER TABLE `cloud`.`service_offering_details` CHANGE COLUMN `value` `value` TEXT NOT NULL;

ALTER TABLE `cloud`.`vpc_offerings` ADD COLUMN `sort_key` int(32) NOT NULL default 0 COMMENT 'sort key used for customising sort method';
-- PR#3186 Add possibility to set MTU size for NIC
ALTER TABLE `cloud`.`nics` ADD COLUMN `mtu` smallint (6) COMMENT 'MTU size for the interface';

DROP VIEW IF EXISTS `cloud`.`user_vm_view`;
CREATE VIEW `cloud`.`user_vm_view` AS
  select
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
   `disk_offering`.`id` AS `disk_offering_id`,case when `service_offering`.`cpu` is null then `custom_cpu`.`value` else `service_offering`.`cpu` end AS `cpu`,case when `service_offering`.`speed` is null then `custom_speed`.`value` else `service_offering`.`speed` end AS `speed`,case when `service_offering`.`ram_size` is null then `custom_ram_size`.`value` else `service_offering`.`ram_size` end AS `ram_size`,
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
   `nics`.`mtu` AS `mtu`,
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
FROM ((((((((((((((((((((((((((((((((`user_vm` join `vm_instance` on(`vm_instance`.`id` = `user_vm`.`id` and `vm_instance`.`removed` is null)) join `account` on(`vm_instance`.`account_id` = `account`.`id`)) join `domain` on(`vm_instance`.`domain_id` = `domain`.`id`)) left join `guest_os` on(`vm_instance`.`guest_os_id` = `guest_os`.`id`)) left join `host_pod_ref` on(`vm_instance`.`pod_id` = `host_pod_ref`.`id`)) left join `projects` on(`projects`.`project_account_id` = `account`.`id`)) left join `instance_group_vm_map` on(`vm_instance`.`id` = `instance_group_vm_map`.`instance_id`)) left join `instance_group` on(`instance_group_vm_map`.`group_id` = `instance_group`.`id`)) left join `data_center` on(`vm_instance`.`data_center_id` = `data_center`.`id`)) left join `host` on(`vm_instance`.`host_id` = `host`.`id`)) left join `vm_template` on(`vm_instance`.`vm_template_id` = `vm_template`.`id`)) left join `vm_template` `iso` on(`iso`.`id` = `user_vm`.`iso_id`)) left join `service_offering` on(`vm_instance`.`service_offering_id` = `service_offering`.`id`)) left join `disk_offering` `svc_disk_offering` on(`vm_instance`.`service_offering_id` = `svc_disk_offering`.`id`)) left join `disk_offering` on(`vm_instance`.`disk_offering_id` = `disk_offering`.`id`)) left join `volumes` on(`vm_instance`.`id` = `volumes`.`instance_id`)) left join `storage_pool` on(`volumes`.`pool_id` = `storage_pool`.`id`)) left join `security_group_vm_map` on(`vm_instance`.`id` = `security_group_vm_map`.`instance_id`)) left join `security_group` on(`security_group_vm_map`.`security_group_id` = `security_group`.`id`)) left join `nics` on(`vm_instance`.`id` = `nics`.`instance_id` and `nics`.`removed` is null)) left join `networks` on(`nics`.`network_id` = `networks`.`id`)) left join `vpc` on(`networks`.`vpc_id` = `vpc`.`id` and `vpc`.`removed` is null)) left join `user_ip_address` on(`user_ip_address`.`vm_id` = `vm_instance`.`id`)) left join `user_vm_details` `ssh_details` on(`ssh_details`.`vm_id` = `vm_instance`.`id` and `ssh_details`.`name` = 'SSH.PublicKey')) left join `ssh_keypairs` on(`ssh_keypairs`.`public_key` = `ssh_details`.`value` and `ssh_keypairs`.`account_id` = `account`.`id`)) left join `resource_tags` on(`resource_tags`.`resource_id` = `vm_instance`.`id` and `resource_tags`.`resource_type` = 'UserVm')) left join `async_job` on(`async_job`.`instance_id` = `vm_instance`.`id` and `async_job`.`instance_type` = 'VirtualMachine' and `async_job`.`job_status` = 0)) left join `affinity_group_vm_map` on(`vm_instance`.`id` = `affinity_group_vm_map`.`instance_id`)) left join `affinity_group` on(`affinity_group_vm_map`.`affinity_group_id` = `affinity_group`.`id`)) left join `user_vm_details` `custom_cpu` on(`custom_cpu`.`vm_id` = `vm_instance`.`id` and `custom_cpu`.`name` = 'CpuNumber')) left join `user_vm_details` `custom_speed` on(`custom_speed`.`vm_id` = `vm_instance`.`id` and `custom_speed`.`name` = 'CpuSpeed')) left join `user_vm_details` `custom_ram_size` on(`custom_ram_size`.`vm_id` = `vm_instance`.`id` and `custom_ram_size`.`name` = 'memory'));

-- Add `sort_key` column to data_center
ALTER TABLE `cloud`.`data_center` ADD COLUMN `sort_key` INT(32) NOT NULL DEFAULT 0;

-- Move domain_id to disk offering details and drop the domain_id column
INSERT INTO `cloud`.`disk_offering_details` (offering_id, name, value, display) SELECT id, 'domainid', domain_id, 0 FROM `cloud`.`disk_offering` WHERE domain_id IS NOT NULL AND type='Disk';
INSERT INTO `cloud`.`service_offering_details` (service_offering_id, name, value, display) SELECT id, 'domainid', domain_id, 0 FROM `cloud`.`disk_offering` WHERE domain_id IS NOT NULL AND type='Service';

ALTER TABLE `cloud`.`disk_offering` DROP COLUMN `domain_id`;

ALTER TABLE `cloud`.`service_offering_details` DROP FOREIGN KEY `fk_service_offering_details__service_offering_id`, DROP KEY `uk_service_offering_id_name`;
ALTER TABLE `cloud`.`service_offering_details` ADD CONSTRAINT `fk_service_offering_details__service_offering_id` FOREIGN KEY (`service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE;

-- Disk offering with multi-domains and multi-zones
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
        `disk_offering`.`state`  AS `state`,
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
    WHERE
        `disk_offering`.`state`='Active'
    GROUP BY
        `disk_offering`.`id`;

-- Service offering with multi-domains and multi-zones
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
        GROUP_CONCAT(DISTINCT(domain.id)) AS domain_id,
        GROUP_CONCAT(DISTINCT(domain.uuid)) AS domain_uuid,
        GROUP_CONCAT(DISTINCT(domain.name)) AS domain_name,
        GROUP_CONCAT(DISTINCT(domain.path)) AS domain_path,
        GROUP_CONCAT(DISTINCT(zone.id)) AS zone_id,
        GROUP_CONCAT(DISTINCT(zone.uuid)) AS zone_uuid,
        GROUP_CONCAT(DISTINCT(zone.name)) AS zone_name
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
    WHERE
        `disk_offering`.`state`='Active'
    GROUP BY
        `service_offering`.`id`;

-- Add display column for network offering details table
ALTER TABLE `cloud`.`network_offering_details` ADD COLUMN `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user';

-- Network offering with multi-domains and multi-zones
DROP VIEW IF EXISTS `cloud`.`network_offering_view`;
CREATE VIEW `cloud`.`network_offering_view` AS
    SELECT
        `network_offerings`.`id` AS `id`,
        `network_offerings`.`uuid` AS `uuid`,
        `network_offerings`.`name` AS `name`,
        `network_offerings`.`unique_name` AS `unique_name`,
        `network_offerings`.`display_text` AS `display_text`,
        `network_offerings`.`nw_rate` AS `nw_rate`,
        `network_offerings`.`mc_rate` AS `mc_rate`,
        `network_offerings`.`traffic_type` AS `traffic_type`,
        `network_offerings`.`tags` AS `tags`,
        `network_offerings`.`system_only` AS `system_only`,
        `network_offerings`.`specify_vlan` AS `specify_vlan`,
        `network_offerings`.`service_offering_id` AS `service_offering_id`,
        `network_offerings`.`conserve_mode` AS `conserve_mode`,
        `network_offerings`.`created` AS `created`,
        `network_offerings`.`removed` AS `removed`,
        `network_offerings`.`default` AS `default`,
        `network_offerings`.`availability` AS `availability`,
        `network_offerings`.`dedicated_lb_service` AS `dedicated_lb_service`,
        `network_offerings`.`shared_source_nat_service` AS `shared_source_nat_service`,
        `network_offerings`.`sort_key` AS `sort_key`,
        `network_offerings`.`redundant_router_service` AS `redundant_router_service`,
        `network_offerings`.`state` AS `state`,
        `network_offerings`.`guest_type` AS `guest_type`,
        `network_offerings`.`elastic_ip_service` AS `elastic_ip_service`,
        `network_offerings`.`eip_associate_public_ip` AS `eip_associate_public_ip`,
        `network_offerings`.`elastic_lb_service` AS `elastic_lb_service`,
        `network_offerings`.`specify_ip_ranges` AS `specify_ip_ranges`,
        `network_offerings`.`inline` AS `inline`,
        `network_offerings`.`is_persistent` AS `is_persistent`,
        `network_offerings`.`internal_lb` AS `internal_lb`,
        `network_offerings`.`public_lb` AS `public_lb`,
        `network_offerings`.`egress_default_policy` AS `egress_default_policy`,
        `network_offerings`.`concurrent_connections` AS `concurrent_connections`,
        `network_offerings`.`keep_alive_enabled` AS `keep_alive_enabled`,
        `network_offerings`.`supports_streched_l2` AS `supports_streched_l2`,
        `network_offerings`.`supports_public_access` AS `supports_public_access`,
        `network_offerings`.`for_vpc` AS `for_vpc`,
        `network_offerings`.`service_package_id` AS `service_package_id`,
        GROUP_CONCAT(DISTINCT(domain.id)) AS domain_id,
        GROUP_CONCAT(DISTINCT(domain.uuid)) AS domain_uuid,
        GROUP_CONCAT(DISTINCT(domain.name)) AS domain_name,
        GROUP_CONCAT(DISTINCT(domain.path)) AS domain_path,
        GROUP_CONCAT(DISTINCT(zone.id)) AS zone_id,
        GROUP_CONCAT(DISTINCT(zone.uuid)) AS zone_uuid,
        GROUP_CONCAT(DISTINCT(zone.name)) AS zone_name
    FROM
        `cloud`.`network_offerings`
            LEFT JOIN
        `cloud`.`network_offering_details` AS `domain_details` ON `domain_details`.`network_offering_id` = `network_offerings`.`id` AND `domain_details`.`name`='domainid'
            LEFT JOIN
        `cloud`.`domain` AS `domain` ON FIND_IN_SET(`domain`.`id`, `domain_details`.`value`)
            LEFT JOIN
        `cloud`.`network_offering_details` AS `zone_details` ON `zone_details`.`network_offering_id` = `network_offerings`.`id` AND `zone_details`.`name`='zoneid'
            LEFT JOIN
        `cloud`.`data_center` AS `zone` ON FIND_IN_SET(`zone`.`id`, `zone_details`.`value`)
    GROUP BY
        `network_offerings`.`id`;

-- Create VPC offering details table
CREATE TABLE `vpc_offering_details` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `offering_id` bigint(20) unsigned NOT NULL COMMENT 'vpc offering id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  KEY `fk_vpc_offering_details__vpc_offering_id` (`offering_id`),
  CONSTRAINT `fk_vpc_offering_details__vpc_offering_id` FOREIGN KEY (`offering_id`) REFERENCES `vpc_offerings` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- VPC offering with multi-domains and multi-zones
DROP VIEW IF EXISTS `cloud`.`vpc_offering_view`;
CREATE VIEW `cloud`.`vpc_offering_view` AS
    SELECT
        `vpc_offerings`.`id` AS `id`,
        `vpc_offerings`.`uuid` AS `uuid`,
        `vpc_offerings`.`name` AS `name`,
        `vpc_offerings`.`unique_name` AS `unique_name`,
        `vpc_offerings`.`display_text` AS `display_text`,
        `vpc_offerings`.`state` AS `state`,
        `vpc_offerings`.`default` AS `default`,
        `vpc_offerings`.`created` AS `created`,
        `vpc_offerings`.`removed` AS `removed`,
        `vpc_offerings`.`service_offering_id` AS `service_offering_id`,
        `vpc_offerings`.`supports_distributed_router` AS `supports_distributed_router`,
        `vpc_offerings`.`supports_region_level_vpc` AS `supports_region_level_vpc`,
        `vpc_offerings`.`redundant_router_service` AS `redundant_router_service`,
        `vpc_offerings`.`sort_key` AS `sort_key`,
        GROUP_CONCAT(DISTINCT(domain.id)) AS domain_id,
        GROUP_CONCAT(DISTINCT(domain.uuid)) AS domain_uuid,
        GROUP_CONCAT(DISTINCT(domain.name)) AS domain_name,
        GROUP_CONCAT(DISTINCT(domain.path)) AS domain_path,
        GROUP_CONCAT(DISTINCT(zone.id)) AS zone_id,
        GROUP_CONCAT(DISTINCT(zone.uuid)) AS zone_uuid,
        GROUP_CONCAT(DISTINCT(zone.name)) AS zone_name
    FROM
        `cloud`.`vpc_offerings`
            LEFT JOIN
        `cloud`.`vpc_offering_details` AS `domain_details` ON `domain_details`.`offering_id` = `vpc_offerings`.`id` AND `domain_details`.`name`='domainid'
            LEFT JOIN
        `cloud`.`domain` AS `domain` ON FIND_IN_SET(`domain`.`id`, `domain_details`.`value`)
            LEFT JOIN
        `cloud`.`vpc_offering_details` AS `zone_details` ON `zone_details`.`offering_id` = `vpc_offerings`.`id` AND `zone_details`.`name`='zoneid'
            LEFT JOIN
        `cloud`.`data_center` AS `zone` ON FIND_IN_SET(`zone`.`id`, `zone_details`.`value`)
    GROUP BY
        `vpc_offerings`.`id`;

-- Recreate data_center_view
DROP VIEW IF EXISTS `cloud`.`data_center_view`;
CREATE VIEW `cloud`.`data_center_view` AS
    select
        data_center.id,
        data_center.uuid,
        data_center.name,
        data_center.is_security_group_enabled,
        data_center.is_local_storage_enabled,
        data_center.description,
        data_center.dns1,
        data_center.dns2,
        data_center.ip6_dns1,
        data_center.ip6_dns2,
        data_center.internal_dns1,
        data_center.internal_dns2,
        data_center.guest_network_cidr,
        data_center.domain,
        data_center.networktype,
        data_center.allocation_state,
        data_center.zone_token,
        data_center.dhcp_provider,
        data_center.removed,
        data_center.sort_key,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        dedicated_resources.affinity_group_id,
        dedicated_resources.account_id,
        affinity_group.uuid affinity_group_uuid
    from
        `cloud`.`data_center`
            left join
        `cloud`.`domain` ON data_center.domain_id = domain.id
            left join
        `cloud`.`dedicated_resources` ON data_center.id = dedicated_resources.data_center_id
            left join
        `cloud`.`affinity_group` ON dedicated_resources.affinity_group_id = affinity_group.id;

-- Remove key/value tags from project_view
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

-- KVM: Add background task to upload certificates for direct download
CREATE TABLE `cloud`.`direct_download_certificate` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL,
  `alias` varchar(255) NOT NULL,
  `certificate` text NOT NULL,
  `hypervisor_type` varchar(45) NOT NULL,
  `zone_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `i_direct_download_certificate_alias` (`alias`),
  KEY `fk_direct_download_certificate__zone_id` (`zone_id`),
  CONSTRAINT `fk_direct_download_certificate__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`direct_download_certificate_host_map` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `certificate_id` bigint(20) unsigned NOT NULL,
  `host_id` bigint(20) unsigned NOT NULL,
  `revoked` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `fk_direct_download_certificate_host_map__host_id` (`host_id`),
  KEY `fk_direct_download_certificate_host_map__certificate_id` (`certificate_id`),
  CONSTRAINT `fk_direct_download_certificate_host_map__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_direct_download_certificate_host_map__certificate_id` FOREIGN KEY (`certificate_id`) REFERENCES `direct_download_certificate` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add configuration to set the MTU for OVS and KVM
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'kvm.ovs.mtu.size', null,
 'Set the MTU for OVS and KVM (if not set default MTU will be considered, Attention: the main bridge will be automatically adjusted to this value)', null, null, null, 0);
