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

-- Flexible tags
ALTER TABLE `cloud`.`storage_pool_tags` ADD COLUMN is_tag_a_rule int(1) UNSIGNED not null DEFAULT 0;

ALTER TABLE `cloud`.`storage_pool_tags` MODIFY tag text NOT NULL;

CREATE OR REPLACE
    VIEW `cloud`.`storage_pool_view` AS
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
    `storage_pool_tags`.`is_tag_a_rule` AS `is_tag_a_rule`,
    `op_host_capacity`.`used_capacity` AS `disk_used_capacity`,
    `op_host_capacity`.`reserved_capacity` AS `disk_reserved_capacity`,
    `async_job`.`id` AS `job_id`,
    `async_job`.`uuid` AS `job_uuid`,
    `async_job`.`job_status` AS `job_status`,
    `async_job`.`account_id` AS `job_account_id`
FROM
    ((((((`cloud`.`storage_pool`
        LEFT JOIN `cloud`.`cluster` ON ((`storage_pool`.`cluster_id` = `cluster`.`id`)))
        LEFT JOIN `cloud`.`data_center` ON ((`storage_pool`.`data_center_id` = `data_center`.`id`)))
        LEFT JOIN `cloud`.`host_pod_ref` ON ((`storage_pool`.`pod_id` = `host_pod_ref`.`id`)))
        LEFT JOIN `cloud`.`storage_pool_tags` ON (((`storage_pool_tags`.`pool_id` = `storage_pool`.`id`))))
        LEFT JOIN `cloud`.`op_host_capacity` ON (((`storage_pool`.`id` = `op_host_capacity`.`host_id`)
        AND (`op_host_capacity`.`capacity_type` IN (3 , 9)))))
        LEFT JOIN `cloud`.`async_job` ON (((`async_job`.`instance_id` = `storage_pool`.`id`)
        AND (`async_job`.`instance_type` = 'StoragePool')
        AND (`async_job`.`job_status` = 0))));


ALTER TABLE `cloud`.`host_tags` ADD COLUMN is_tag_a_rule int(1) UNSIGNED not null DEFAULT 0;

ALTER TABLE `cloud`.`host_tags` MODIFY tag text NOT NULL;

CREATE OR REPLACE VIEW `cloud`.`host_view` AS
SELECT
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
    GROUP_CONCAT(DISTINCT(host_tags.tag)) AS tag,
    `host_tags`.`is_tag_a_rule` AS `is_tag_a_rule`,
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
    oobm.power_state AS `oobm_power_state`,
    ha_config.enabled AS `ha_enabled`,
    ha_config.ha_state AS `ha_state`,
    ha_config.provider AS `ha_provider`,
    `last_annotation_view`.`annotation` AS `annotation`,
    `last_annotation_view`.`created` AS `last_annotated`,
    `user`.`username` AS `username`
FROM
    `cloud`.`host`
        LEFT JOIN
    `cloud`.`cluster` ON host.cluster_id = cluster.id
        LEFT JOIN
    `cloud`.`data_center` ON host.data_center_id = data_center.id
        LEFT JOIN
    `cloud`.`host_pod_ref` ON host.pod_id = host_pod_ref.id
        LEFT JOIN
    `cloud`.`host_details` ON host.id = host_details.host_id
        AND host_details.name = 'guest.os.category.id'
        LEFT JOIN
    `cloud`.`guest_os_category` ON guest_os_category.id = CONVERT ( host_details.value, UNSIGNED )
        LEFT JOIN
    `cloud`.`host_tags` ON host_tags.host_id = host.id
        LEFT JOIN
    `cloud`.`op_host_capacity` mem_caps ON host.id = mem_caps.host_id
        AND mem_caps.capacity_type = 0
        LEFT JOIN
    `cloud`.`op_host_capacity` cpu_caps ON host.id = cpu_caps.host_id
        AND cpu_caps.capacity_type = 1
        LEFT JOIN
    `cloud`.`async_job` ON async_job.instance_id = host.id
        AND async_job.instance_type = 'Host'
        AND async_job.job_status = 0
        LEFT JOIN
    `cloud`.`oobm` ON oobm.host_id = host.id
        left join
    `cloud`.`ha_config` ON ha_config.resource_id=host.id
        and ha_config.resource_type='Host'
        LEFT JOIN
    `cloud`.`last_annotation_view` ON `last_annotation_view`.`entity_uuid` = `host`.`uuid`
        LEFT JOIN
    `cloud`.`user` ON `user`.`uuid` = `last_annotation_view`.`user_uuid`
GROUP BY
    `host`.`id`;
