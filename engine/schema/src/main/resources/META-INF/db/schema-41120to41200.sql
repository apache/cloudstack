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
-- Schema upgrade from 4.11.2.0 to 4.12.0.0
--;

-- [CLOUDSTACK-10314] Add reason column to ACL rule table
ALTER TABLE `cloud`.`network_acl_item` ADD COLUMN `reason` VARCHAR(2500) AFTER `display`;

-- [CLOUDSTACK-9846] Make provision to store content and subject for Alerts in separate columns.
ALTER TABLE `cloud`.`alert` ADD COLUMN `content` VARCHAR(5000);

-- Fix the name of the column used to hold IPv4 range in 'vlan' table.
ALTER TABLE `vlan` CHANGE `description` `ip4_range` varchar(255);

-- [CLOUDSTACK-10344] bug when moving ACL rules (change order with drag and drop)
-- We are only adding the permission to the default rules. Any custom rule must be configured by the root admin.
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'moveNetworkAclItem', 'ALLOW', 100) ON DUPLICATE KEY UPDATE rule=rule;
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'moveNetworkAclItem', 'ALLOW', 302) ON DUPLICATE KEY UPDATE rule=rule;
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'moveNetworkAclItem', 'ALLOW', 260) ON DUPLICATE KEY UPDATE rule=rule;

UPDATE `cloud`.`async_job` SET `removed` = now() WHERE `removed` IS NULL;

-- PR#1448 update description of 'execute.in.sequence.network.element.commands' parameter to reflect an unused command that has been removed. The removed class command is 'UserDataCommand'.
update `cloud`.`configuration` set description = 'If set to true, DhcpEntryCommand, SavePasswordCommand, VmDataCommand will be synchronized on the agent side. If set to false, these commands become asynchronous. Default value is false.' where name = 'execute.in.sequence.network.element.commands';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'StorageManager', 'kvm.storage.offline.migration.wait', '10800', 'Timeout in seconds for offline (non-live) storage migration to complete on KVM', '10800', null, 'Global', 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'StorageManager', 'kvm.storage.online.migration.wait', '10800', 'Timeout in seconds for online (live) storage migration to complete on KVM (migrateVirtualMachineWithVolume)', '10800', null, 'Global', 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'StorageManager', 'max.number.managed.clustered.file.systems', '200', 'XenServer and VMware only: Maximum number of managed SRs or datastores per compute cluster', '200', null, 'Cluster', 0);

-- add KVM Guest OS mapping for Windows Server 2019
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (276, UUID(), 6, 'Windows Server 2019 (64-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Windows Server 2019', 276, now(), 0);

-- changed fingerprint type to TEXT, it avoids db exception when creating the certificate issue #3123
ALTER TABLE `cloud`.`sslcerts` MODIFY `fingerprint` TEXT;

-- add KVM / qemu io bursting options PR 3133
alter table `cloud`.`disk_offering` add `bytes_read_rate_max` bigint(20) default null after `bytes_read_rate`;
alter table `cloud`.`disk_offering` add `bytes_read_rate_max_length` bigint(20) default null after `bytes_read_rate_max`;
alter table `cloud`.`disk_offering` add `bytes_write_rate_max` bigint(20) default null after `bytes_write_rate`;
alter table `cloud`.`disk_offering` add `bytes_write_rate_max_length` bigint(20) default null after `bytes_write_rate_max`;
alter table `cloud`.`disk_offering` add `iops_read_rate_max` bigint(20) default null after `iops_read_rate`;
alter table `cloud`.`disk_offering` add `iops_read_rate_max_length` bigint(20) default null after `iops_read_rate_max`;
alter table `cloud`.`disk_offering` add `iops_write_rate_max` bigint(20) default null after `iops_write_rate`;
alter table `cloud`.`disk_offering` add `iops_write_rate_max_length` bigint(20) default null after `iops_write_rate_max`;

ALTER VIEW `cloud`.`disk_offering_view` AS
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
        `domain`.`id` AS `domain_id`,
        `domain`.`uuid` AS `domain_uuid`,
        `domain`.`name` AS `domain_name`,
        `domain`.`path` AS `domain_path`
    FROM
        (`disk_offering`
        LEFT JOIN `domain` ON ((`disk_offering`.`domain_id` = `domain`.`id`)))
    WHERE
        (`disk_offering`.`state` = 'ACTIVE');


ALTER VIEW `cloud`.`service_offering_view` AS
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
        `domain`.`id` AS `domain_id`,
        `domain`.`uuid` AS `domain_uuid`,
        `domain`.`name` AS `domain_name`,
        `domain`.`path` AS `domain_path`
    FROM
        ((`service_offering`
        JOIN `disk_offering` ON ((`service_offering`.`id` = `disk_offering`.`id`)))
        LEFT JOIN `domain` ON ((`disk_offering`.`domain_id` = `domain`.`id`)))
    WHERE
        (`disk_offering`.`state` = 'Active');

-- PR#2578 New column for listManagementServers API call
ALTER TABLE `mshost` ADD COLUMN `uuid` varchar(40) AFTER `name`;

-- PR#3186 Add possibility to set MTU size for NIC
ALTER TABLE `cloud`.`nics` ADD COLUMN `mtu` smallint (6) NOT NULL DEFAULT 0 COMMENT 'MTU size for the interface';

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
