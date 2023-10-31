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
-- Schema upgrade from 4.18.1.0 to 4.19.0.0
--;

ALTER TABLE `cloud`.`mshost` MODIFY COLUMN `state` varchar(25);

DROP VIEW IF EXISTS `cloud`.`async_job_view`;
CREATE VIEW `cloud`.`async_job_view` AS
    select
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        user.id user_id,
        user.uuid user_uuid,
        async_job.id,
        async_job.uuid,
        async_job.job_cmd,
        async_job.job_status,
        async_job.job_process_status,
        async_job.job_result_code,
        async_job.job_result,
        async_job.created,
        async_job.removed,
        async_job.instance_type,
        async_job.instance_id,
        async_job.job_executing_msid,
        CASE
            WHEN async_job.instance_type = 'Volume' THEN volumes.uuid
            WHEN
                async_job.instance_type = 'Template'
                    or async_job.instance_type = 'Iso'
            THEN
                vm_template.uuid
            WHEN
                async_job.instance_type = 'VirtualMachine'
                    or async_job.instance_type = 'ConsoleProxy'
                    or async_job.instance_type = 'SystemVm'
                    or async_job.instance_type = 'DomainRouter'
            THEN
                vm_instance.uuid
            WHEN async_job.instance_type = 'Snapshot' THEN snapshots.uuid
            WHEN async_job.instance_type = 'Host' THEN host.uuid
            WHEN async_job.instance_type = 'StoragePool' THEN storage_pool.uuid
            WHEN async_job.instance_type = 'IpAddress' THEN user_ip_address.uuid
            WHEN async_job.instance_type = 'SecurityGroup' THEN security_group.uuid
            WHEN async_job.instance_type = 'PhysicalNetwork' THEN physical_network.uuid
            WHEN async_job.instance_type = 'TrafficType' THEN physical_network_traffic_types.uuid
            WHEN async_job.instance_type = 'PhysicalNetworkServiceProvider' THEN physical_network_service_providers.uuid
            WHEN async_job.instance_type = 'FirewallRule' THEN firewall_rules.uuid
            WHEN async_job.instance_type = 'Account' THEN acct.uuid
            WHEN async_job.instance_type = 'User' THEN us.uuid
            WHEN async_job.instance_type = 'StaticRoute' THEN static_routes.uuid
            WHEN async_job.instance_type = 'PrivateGateway' THEN vpc_gateways.uuid
            WHEN async_job.instance_type = 'Counter' THEN counter.uuid
            WHEN async_job.instance_type = 'Condition' THEN conditions.uuid
            WHEN async_job.instance_type = 'AutoScalePolicy' THEN autoscale_policies.uuid
            WHEN async_job.instance_type = 'AutoScaleVmProfile' THEN autoscale_vmprofiles.uuid
            WHEN async_job.instance_type = 'AutoScaleVmGroup' THEN autoscale_vmgroups.uuid
            ELSE null
        END instance_uuid
    from
        `cloud`.`async_job`
            left join
        `cloud`.`account` ON async_job.account_id = account.id
            left join
        `cloud`.`domain` ON domain.id = account.domain_id
            left join
        `cloud`.`user` ON async_job.user_id = user.id
            left join
        `cloud`.`volumes` ON async_job.instance_id = volumes.id
            left join
        `cloud`.`vm_template` ON async_job.instance_id = vm_template.id
            left join
        `cloud`.`vm_instance` ON async_job.instance_id = vm_instance.id
            left join
        `cloud`.`snapshots` ON async_job.instance_id = snapshots.id
            left join
        `cloud`.`host` ON async_job.instance_id = host.id
            left join
        `cloud`.`storage_pool` ON async_job.instance_id = storage_pool.id
            left join
        `cloud`.`user_ip_address` ON async_job.instance_id = user_ip_address.id
            left join
        `cloud`.`security_group` ON async_job.instance_id = security_group.id
            left join
        `cloud`.`physical_network` ON async_job.instance_id = physical_network.id
            left join
        `cloud`.`physical_network_traffic_types` ON async_job.instance_id = physical_network_traffic_types.id
            left join
        `cloud`.`physical_network_service_providers` ON async_job.instance_id = physical_network_service_providers.id
            left join
        `cloud`.`firewall_rules` ON async_job.instance_id = firewall_rules.id
            left join
        `cloud`.`account` acct ON async_job.instance_id = acct.id
            left join
        `cloud`.`user` us ON async_job.instance_id = us.id
            left join
        `cloud`.`static_routes` ON async_job.instance_id = static_routes.id
            left join
        `cloud`.`vpc_gateways` ON async_job.instance_id = vpc_gateways.id
            left join
        `cloud`.`counter` ON async_job.instance_id = counter.id
            left join
        `cloud`.`conditions` ON async_job.instance_id = conditions.id
            left join
        `cloud`.`autoscale_policies` ON async_job.instance_id = autoscale_policies.id
            left join
        `cloud`.`autoscale_vmprofiles` ON async_job.instance_id = autoscale_vmprofiles.id
            left join
        `cloud`.`autoscale_vmgroups` ON async_job.instance_id = autoscale_vmgroups.id;

-- Invalidate existing console_session records
UPDATE `cloud`.`console_session` SET removed=now();
-- Modify acquired column in console_session to datetime type
ALTER TABLE `cloud`.`console_session` DROP `acquired`, ADD `acquired` datetime COMMENT 'When the session was acquired' AFTER `host_id`;

-- create_public_parameter_on_roles. #6960
ALTER TABLE `cloud`.`roles` ADD COLUMN `public_role` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Indicates whether the role will be visible to all users (public) or only to root admins (private). If this parameter is not specified during the creation of the role its value will be defaulted to true (public).';

-- Add tables for VM Scheduler
DROP TABLE IF EXISTS `cloud`.`vm_schedule`;
CREATE TABLE `cloud`.`vm_schedule` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL,
  `uuid` varchar(40) NOT NULL COMMENT 'schedule uuid',
  `description` varchar(1024) COMMENT 'description of the vm schedule',
  `schedule` varchar(255) NOT NULL COMMENT 'schedule frequency in cron format',
  `timezone` varchar(100) NOT NULL COMMENT 'the timezone in which the schedule time is specified',
  `action` varchar(20) NOT NULL COMMENT 'action to perform',
  `enabled` int(1) NOT NULL COMMENT 'Enabled or disabled',
  `start_date` datetime NOT NULL COMMENT 'start time for this schedule',
  `end_date` datetime COMMENT 'end time for this schedule',
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY (`id`),
  INDEX `i_vm_schedule__vm_id`(`vm_id`),
  INDEX `i_vm_schedule__enabled_end_date`(`enabled`, `end_date`),
  CONSTRAINT `fk_vm_schedule__vm_id` FOREIGN KEY (`vm_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `cloud`.`vm_scheduled_job`;
CREATE TABLE `cloud`.`vm_scheduled_job` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL,
  `vm_schedule_id` bigint unsigned NOT NULL,
  `uuid` varchar(40) NOT NULL COMMENT 'scheduled job uuid',
  `action` varchar(20) NOT NULL COMMENT 'action to perform',
  `scheduled_timestamp` datetime NOT NULL COMMENT 'Time at which the action is taken',
  `async_job_id` bigint unsigned DEFAULT NULL COMMENT 'If this schedule is being executed, it is the id of the create aysnc_job. Before that it is null',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`vm_schedule_id`, `scheduled_timestamp`),
  INDEX `i_vm_scheduled_job__scheduled_timestamp`(`scheduled_timestamp`),
  INDEX `i_vm_scheduled_job__vm_id`(`vm_id`),
  CONSTRAINT `fk_vm_scheduled_job__vm_id` FOREIGN KEY (`vm_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vm_scheduled_job__vm_schedule_id` FOREIGN KEY (`vm_schedule_id`) REFERENCES `vm_schedule`(`id`) ON DELETE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add support for different cluster types for kubernetes
ALTER TABLE `cloud`.`kubernetes_cluster` ADD COLUMN `cluster_type` varchar(64) DEFAULT 'CloudManaged' COMMENT 'type of cluster';
ALTER TABLE `cloud`.`kubernetes_cluster` MODIFY COLUMN `kubernetes_version_id` bigint unsigned NULL COMMENT 'the ID of the Kubernetes version of this Kubernetes cluster';

-- Set removed state for all removed accounts
UPDATE `cloud`.`account` SET state='removed' WHERE `removed` IS NOT NULL;


-- New tables for VNF
CREATE TABLE IF NOT EXISTS `cloud`.`vnf_template_nics` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `template_id` bigint unsigned NOT NULL COMMENT 'id of the VNF template',
    `device_id` bigint unsigned NOT NULL COMMENT 'Device id of the NIC when plugged into the VNF appliances',
    `device_name` varchar(1024) NOT NULL COMMENT 'Name of the NIC',
    `required` tinyint NOT NULL DEFAULT '1' COMMENT 'True if the NIC is required. False if optional',
    `management` tinyint NOT NULL DEFAULT '1' COMMENT 'True if the NIC is a management interface',
    `description` varchar(1024) COMMENT 'Description of the NIC',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_id_device_id` (`template_id`, `device_id`),
    KEY `fk_vnf_template_nics__template_id` (`template_id`),
    CONSTRAINT `fk_vnf_template_nics__template_id` FOREIGN KEY (`template_id`) REFERENCES `vm_template` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cloud`.`vnf_template_details` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `template_id` bigint unsigned NOT NULL COMMENT 'id of the VNF template',
    `name` varchar(255) NOT NULL,
    `value` varchar(1024) NOT NULL,
    `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user',
    PRIMARY KEY (`id`),
    KEY `fk_vnf_template_details__template_id` (`template_id`),
    CONSTRAINT `fk_vnf_template_details__template_id` FOREIGN KEY (`template_id`) REFERENCES `vm_template` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    `vm_instance`.`update_time` AS `update_time`,
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
    `vm_template`.`user_data_link_policy` AS `user_data_policy`
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
        LEFT JOIN `user_ip_address` ON ((`user_ip_address`.`vm_id` = `vm_instance`.`id`)))
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
        AND (`custom_ram_size`.`name` = 'memory'))));

-- Add tables for Cluster DRS
DROP TABLE IF EXISTS `cloud`.`cluster_drs_plan`;
CREATE TABLE `cloud`.`cluster_drs_plan` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `cluster_id` bigint unsigned NOT NULL,
  `event_id` bigint unsigned NOT NULL,
  `uuid` varchar(40) NOT NULL COMMENT 'schedule uuid',
  `type` varchar(20) NOT NULL COMMENT 'type of plan',
  `status` varchar(20) NOT NULL COMMENT 'status of plan',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  INDEX `i_cluster_drs_plan__cluster_id_status`(`cluster_id`, `status`),
  INDEX `i_cluster_drs_plan__status`(`status`),
  INDEX `i_cluster_drs_plan__created`(`created`),
  CONSTRAINT `fk_cluster_drs_plan__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster`(`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `cloud`.`cluster_drs_plan_migration`;
CREATE TABLE `cloud`.`cluster_drs_plan_migration` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `plan_id` bigint unsigned NOT NULL,
  `vm_id` bigint unsigned NOT NULL,
  `src_host_id` bigint unsigned NOT NULL,
  `dest_host_id` bigint unsigned NOT NULL,
  `job_id` bigint unsigned NULL,
  `status` varchar(20) NULL COMMENT 'status of async job',
  PRIMARY KEY (`id`),
  INDEX `i_cluster_drs_plan_migration__plan_id_status`(`plan_id`, `status`),
  CONSTRAINT `fk_cluster_drs_plan_migration__plan_id` FOREIGN KEY (`plan_id`) REFERENCES `cluster_drs_plan`(`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('DRS', 'drs', 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Miscellaneous'));

UPDATE `cloud`.`configuration`
    SET subgroup_id = (SELECT id FROM `cloud`.`configuration_subgroup` WHERE name = 'DRS')
    WHERE name IN ('drs.automatic.enable', 'drs.algorithm', 'drs.automatic.interval', 'drs.max.migrations', 'drs.imbalance', 'drs.metric', 'drs.plan.expire.interval');

-- Add table for snapshot zone reference
CREATE TABLE  `cloud`.`snapshot_zone_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `zone_id` bigint unsigned NOT NULL,
  `snapshot_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_snapshot_zone_ref__zone_id` FOREIGN KEY `fk_snapshot_zone_ref__zone_id` (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE,
  INDEX `i_snapshot_zone_ref__zone_id`(`zone_id`),
  CONSTRAINT `fk_snapshot_zone_ref__snapshot_id` FOREIGN KEY `fk_snapshot_zone_ref__snapshot_id` (`snapshot_id`) REFERENCES `snapshots` (`id`) ON DELETE CASCADE,
  INDEX `i_snapshot_zone_ref__snapshot_id`(`snapshot_id`),
  INDEX `i_snapshot_zone_ref__removed`(`removed`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- Alter snapshot_store_ref table to add download related fields
ALTER TABLE `cloud`.`snapshot_store_ref`
    ADD COLUMN `download_state` varchar(255) DEFAULT NULL COMMENT 'the state of the snapshot download' AFTER `volume_id`,
    ADD COLUMN `download_pct` int unsigned DEFAULT NULL COMMENT 'the percentage of the snapshot download completed' AFTER `download_state`,
    ADD COLUMN `error_str` varchar(255) DEFAULT NULL COMMENT 'the error message when the snapshot download occurs' AFTER `download_pct`,
    ADD COLUMN `local_path` varchar(255) DEFAULT NULL COMMENT 'the path of the snapshot download' AFTER `error_str`,
    ADD COLUMN `display` tinyint(1) unsigned NOT NULL DEFAULT 1  COMMENT '1 implies store reference is available for listing' AFTER `error_str`;

-- Create snapshot_view
DROP VIEW IF EXISTS `cloud`.`snapshot_view`;
CREATE VIEW `cloud`.`snapshot_view` AS
     SELECT
         `snapshots`.`id` AS `id`,
         `snapshots`.`uuid` AS `uuid`,
         `snapshots`.`name` AS `name`,
         `snapshots`.`status` AS `status`,
         `snapshots`.`disk_offering_id` AS `disk_offering_id`,
         `snapshots`.`snapshot_type` AS `snapshot_type`,
         `snapshots`.`type_description` AS `type_description`,
         `snapshots`.`size` AS `size`,
         `snapshots`.`created` AS `created`,
         `snapshots`.`removed` AS `removed`,
         `snapshots`.`location_type` AS `location_type`,
         `snapshots`.`hypervisor_type` AS `hypervisor_type`,
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
         `volumes`.`id` AS `volume_id`,
         `volumes`.`uuid` AS `volume_uuid`,
         `volumes`.`name` AS `volume_name`,
         `volumes`.`volume_type` AS `volume_type`,
         `volumes`.`size` AS `volume_size`,
         `data_center`.`id` AS `data_center_id`,
         `data_center`.`uuid` AS `data_center_uuid`,
         `data_center`.`name` AS `data_center_name`,
         `snapshot_store_ref`.`store_id` AS `store_id`,
         IFNULL(`image_store`.`uuid`, `storage_pool`.`uuid`) AS `store_uuid`,
         IFNULL(`image_store`.`name`, `storage_pool`.`name`) AS `store_name`,
         `snapshot_store_ref`.`store_role` AS `store_role`,
         `snapshot_store_ref`.`state` AS `store_state`,
         `snapshot_store_ref`.`download_state` AS `download_state`,
         `snapshot_store_ref`.`download_pct` AS `download_pct`,
         `snapshot_store_ref`.`error_str` AS `error_str`,
         `snapshot_store_ref`.`size` AS `store_size`,
         `snapshot_store_ref`.`created` AS `created_on_store`,
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
          CONCAT(`snapshots`.`id`,
                 '_',
                 IFNULL(`snapshot_store_ref`.`store_role`, 'UNKNOWN'),
                 '_',
                 IFNULL(`snapshot_store_ref`.`store_id`, 0)) AS `snapshot_store_pair`
     FROM
         ((((((((((`snapshots`
         JOIN `account` ON ((`account`.`id` = `snapshots`.`account_id`)))
         JOIN `domain` ON ((`domain`.`id` = `account`.`domain_id`)))
         LEFT JOIN `projects` ON ((`projects`.`project_account_id` = `account`.`id`)))
         LEFT JOIN `volumes` ON ((`volumes`.`id` = `snapshots`.`volume_id`)))
         LEFT JOIN `snapshot_store_ref` ON (((`snapshot_store_ref`.`snapshot_id` = `snapshots`.`id`)
             AND (`snapshot_store_ref`.`state` != 'Destroyed')
             AND (`snapshot_store_ref`.`display` = 1))))
         LEFT JOIN `image_store` ON ((ISNULL(`image_store`.`removed`)
             AND (`snapshot_store_ref`.`store_role` = 'Image')
             AND (`snapshot_store_ref`.`store_id` IS NOT NULL)
             AND (`image_store`.`id` = `snapshot_store_ref`.`store_id`))))
         LEFT JOIN `storage_pool` ON ((ISNULL(`storage_pool`.`removed`)
             AND (`snapshot_store_ref`.`store_role` = 'Primary')
             AND (`snapshot_store_ref`.`store_id` IS NOT NULL)
             AND (`storage_pool`.`id` = `snapshot_store_ref`.`store_id`))))
         LEFT JOIN `snapshot_zone_ref` ON (((`snapshot_zone_ref`.`snapshot_id` = `snapshots`.`id`)
             AND ISNULL(`snapshot_store_ref`.`store_id`)
             AND ISNULL(`snapshot_zone_ref`.`removed`))))
         LEFT JOIN `data_center` ON (((`image_store`.`data_center_id` = `data_center`.`id`)
             OR (`storage_pool`.`data_center_id` = `data_center`.`id`)
             OR (`snapshot_zone_ref`.`zone_id` = `data_center`.`id`))))
         LEFT JOIN `resource_tags` ON ((`resource_tags`.`resource_id` = `snapshots`.`id`)
             AND (`resource_tags`.`resource_type` = 'Snapshot')));

UPDATE `cloud`.`configuration` SET
    `options` = concat(`options`, ',OAUTH2'),
    `default_value` = concat(`default_value`, ',OAUTH2'),
    `value` = concat(`value`, ',OAUTH2')
WHERE `name` = 'user.authenticators.order' ;

UPDATE `cloud`.`configuration` SET
    `options` = concat(`options`, ',OAUTH2Auth'),
    `default_value` = concat(`default_value`, ',OAUTH2Auth'),
    `value` = concat(`value`, ',OAUTH2Auth')
where `name` = 'pluggableApi.authenticators.order' ;

-- Create table for OAuth provider details
DROP TABLE IF EXISTS `cloud`.`oauth_provider`;
CREATE TABLE `cloud`.`oauth_provider` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) NOT NULL COMMENT 'unique identifier',
  `description` varchar(1024) COMMENT 'description of the provider',
  `provider` varchar(40) NOT NULL COMMENT 'name of the provider',
  `client_id` varchar(255) NOT NULL COMMENT 'client id which is configured in the provider',
  `secret_key` varchar(255) NOT NULL COMMENT 'secret key which is configured in the provider',
  `redirect_uri` varchar(255) NOT NULL COMMENT 'redirect uri which is configured in the provider',
  `enabled` int(1) NOT NULL DEFAULT 1 COMMENT 'Enabled or disabled',
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY (`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
