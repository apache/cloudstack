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
-- Schema upgrade from 4.20.1.0 to 4.21.0.0
--;

-- Add columns max_backup and backup_interval_type to backup table
ALTER TABLE `cloud`.`backup_schedule` ADD COLUMN `max_backups` int(8) default NULL COMMENT 'maximum number of backups to maintain';
ALTER TABLE `cloud`.`backups` ADD COLUMN `backup_interval_type` int(5) COMMENT 'type of backup, e.g. manual, recurring - hourly, daily, weekly or monthly';

-- Add console_endpoint_creator_address column to cloud.console_session table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'console_endpoint_creator_address', 'VARCHAR(45)');

-- Add client_address column to cloud.console_session table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'client_address', 'VARCHAR(45)');

-- Allow default roles to use quotaCreditsList
INSERT INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'quotaCreditsList', permission, sort_order
FROM `cloud`.`role_permissions` rp
WHERE rp.rule = 'quotaStatement'
AND NOT EXISTS(SELECT 1 FROM cloud.role_permissions rp_ WHERE rp.role_id = rp_.role_id AND rp_.rule = 'quotaCreditsList');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host', 'last_mgmt_server_id', 'bigint unsigned DEFAULT NULL COMMENT "last management server this host is connected to" AFTER `mgmt_server_id`');

-- Add column lease_duration and lease_expiry_action to service_offering_view
DROP VIEW IF EXISTS `cloud`.`service_offering_view`;

CREATE VIEW `cloud`.`service_offering_view` AS
SELECT
    `service_offering`.`id` AS `id`,
    `service_offering`.`uuid` AS `uuid`,
    `service_offering`.`name` AS `name`,
    `service_offering`.`state` AS `state`,
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
    `lease_duration_details`.`value` AS `lease_duration`,
    `lease_expiry_action_details`.`value` AS `lease_expiry_action`,
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
    `cloud`.`disk_offering` ON service_offering.disk_offering_id = disk_offering.id
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
        LEFT JOIN
    `cloud`.`service_offering_details` AS `lease_duration_details` ON `lease_duration_details`.`service_offering_id` = `service_offering`.`id`
        AND `lease_duration_details`.`name` = 'leaseduration'
        LEFT JOIN
    `cloud`.`service_offering_details` AS `lease_expiry_action_details` ON `lease_expiry_action_details`.`service_offering_id` = `service_offering`.`id`
        AND `lease_expiry_action_details`.`name` = 'leaseexpiryaction'
GROUP BY
    `service_offering`.`id`;

-- Add lease_expiry_date and lease_expiry_action to user_vm_view

DROP VIEW IF EXISTS `cloud`.`user_vm_view`;

CREATE VIEW `user_vm_view` AS
SELECT
    `vm_instance`.`id` AS `id`,
    `vm_instance`.`name` AS `name`,
    `user_vm`.`display_name` AS `display_name`,
    `user_vm`.`user_data` AS `user_data`,
    `user_vm`.`user_vm_type` AS `user_vm_type`,
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
    `vm_instance`.`delete_protection` AS `delete_protection`,
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
    `data_center`.`networktype` AS `data_center_network_type`,
    `host`.`id` AS `host_id`,
    `host`.`uuid` AS `host_uuid`,
    `host`.`name` AS `host_name`,
    `host`.`cluster_id` AS `cluster_id`,
    `host`.`status` AS `host_status`,
    `host`.`resource_state` AS `host_resource_state`,
    `vm_template`.`id` AS `template_id`,
    `vm_template`.`uuid` AS `template_uuid`,
    `vm_template`.`name` AS `template_name`,
    `vm_template`.`type` AS `template_type`,
    `vm_template`.`format` AS `template_format`,
    `vm_template`.`display_text` AS `template_display_text`,
    `vm_template`.`enable_password` AS `password_enabled`,
    `iso`.`id` AS `iso_id`,
    `iso`.`uuid` AS `iso_uuid`,
    `iso`.`name` AS `iso_name`,
    `iso`.`display_text` AS `iso_display_text`,
    `service_offering`.`id` AS `service_offering_id`,
    `service_offering`.`uuid` AS `service_offering_uuid`,
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
    `service_offering`.`name` AS `service_offering_name`,
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
    `ssh_details`.`value` AS `keypair_names`,
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
    `autoscale_vmgroups`.`id` AS `autoscale_vmgroup_id`,
    `autoscale_vmgroups`.`uuid` AS `autoscale_vmgroup_uuid`,
    `autoscale_vmgroups`.`name` AS `autoscale_vmgroup_name`,
    `vm_instance`.`dynamically_scalable` AS `dynamically_scalable`,
    `user_data`.`id` AS `user_data_id`,
    `user_data`.`uuid` AS `user_data_uuid`,
    `user_data`.`name` AS `user_data_name`,
    `user_vm`.`user_data_details` AS `user_data_details`,
    `vm_template`.`user_data_link_policy` AS `user_data_policy`,
    `lease_expiry_date`.`value` AS `lease_expiry_date`,
    `lease_expiry_action`.`value` AS `lease_expiry_action`
FROM
    (((((((((((((((((((((((((((((((((((`user_vm`
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
        LEFT JOIN `disk_offering` `svc_disk_offering` ON ((`volumes`.`disk_offering_id` = `svc_disk_offering`.`id`)))
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
        LEFT JOIN `user_ip_address` FORCE INDEX(`fk_user_ip_address__vm_id`) ON ((`user_ip_address`.`vm_id` = `vm_instance`.`id`)))
        LEFT JOIN `user_vm_details` `ssh_details` ON (((`ssh_details`.`vm_id` = `vm_instance`.`id`)
        AND (`ssh_details`.`name` = 'SSH.KeyPairNames'))))
        LEFT JOIN `resource_tags` ON (((`resource_tags`.`resource_id` = `vm_instance`.`id`)
        AND (`resource_tags`.`resource_type` = 'UserVm'))))
        LEFT JOIN `async_job` ON (((`async_job`.`instance_id` = `vm_instance`.`id`)
        AND (`async_job`.`instance_type` = 'VirtualMachine')
        AND (`async_job`.`job_status` = 0))))
        LEFT JOIN `affinity_group_vm_map` ON ((`vm_instance`.`id` = `affinity_group_vm_map`.`instance_id`)))
        LEFT JOIN `affinity_group` ON ((`affinity_group_vm_map`.`affinity_group_id` = `affinity_group`.`id`)))
        LEFT JOIN `autoscale_vmgroup_vm_map` ON ((`autoscale_vmgroup_vm_map`.`instance_id` = `vm_instance`.`id`)))
        LEFT JOIN `autoscale_vmgroups` ON ((`autoscale_vmgroup_vm_map`.`vmgroup_id` = `autoscale_vmgroups`.`id`)))
        LEFT JOIN `user_vm_details` `custom_cpu` ON (((`custom_cpu`.`vm_id` = `vm_instance`.`id`)
        AND (`custom_cpu`.`name` = 'CpuNumber'))))
        LEFT JOIN `user_vm_details` `custom_speed` ON (((`custom_speed`.`vm_id` = `vm_instance`.`id`)
        AND (`custom_speed`.`name` = 'CpuSpeed'))))
        LEFT JOIN `user_vm_details` `custom_ram_size` ON (((`custom_ram_size`.`vm_id` = `vm_instance`.`id`)
        AND (`custom_ram_size`.`name` = 'memory')))
        LEFT JOIN `user_vm_details` `lease_expiry_date` ON ((`lease_expiry_date`.`vm_id` = `vm_instance`.`id`)
        AND (`lease_expiry_date`.`name` = 'leaseexpirydate'))
        LEFT JOIN `user_vm_details` `lease_expiry_action` ON (((`lease_expiry_action`.`vm_id` = `vm_instance`.`id`)
        AND (`lease_expiry_action`.`name` = 'leaseexpiryaction'))));
