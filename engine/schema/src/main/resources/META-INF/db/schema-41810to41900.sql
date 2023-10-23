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

CREATE TABLE `cloud`.`nsx_providers` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40),
    `zone_id` bigint unsigned NOT NULL COMMENT 'Zone ID',
    `host_id` bigint unsigned NOT NULL COMMENT 'Host ID',
    `provider_name` varchar(40),
    `hostname` varchar(255) NOT NULL,
    `port` varchar(255),
    `username` varchar(255) NOT NULL,
    `password` varchar(255) NOT NULL,
    `tier0_gateway` varchar(255),
    `edge_cluster` varchar(255),
    `transport_zone` varchar(255),
    `created` datetime NOT NULL COMMENT 'date created',
    `removed` datetime COMMENT 'date removed if not null',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_nsx_providers__zone_id` FOREIGN KEY `fk_nsx_providers__zone_id` (`zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE,
    INDEX `i_nsx_providers__zone_id`(`zone_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- Idempotent ADD COLUMN
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_COLUMN`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_COLUMN` (
    IN in_table_name VARCHAR(200)
, IN in_column_name VARCHAR(200)
, IN in_column_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- NSX Plugin --
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','for_nsx', 'int(1) unsigned DEFAULT "0" COMMENT "is nsx enabled for the resource"');

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
    `network_offerings`.`supports_vm_autoscaling` AS `supports_vm_autoscaling`,
    `network_offerings`.`for_vpc` AS `for_vpc`,
    `network_offerings`.`for_tungsten` AS `for_tungsten`,
    `network_offerings`.`for_nsx` AS `for_nsx`,
    `network_offerings`.`service_package_id` AS `service_package_id`,
    GROUP_CONCAT(DISTINCT(domain.id)) AS domain_id,
    GROUP_CONCAT(DISTINCT(domain.uuid)) AS domain_uuid,
    GROUP_CONCAT(DISTINCT(domain.name)) AS domain_name,
    GROUP_CONCAT(DISTINCT(domain.path)) AS domain_path,
    GROUP_CONCAT(DISTINCT(zone.id)) AS zone_id,
    GROUP_CONCAT(DISTINCT(zone.uuid)) AS zone_uuid,
    GROUP_CONCAT(DISTINCT(zone.name)) AS zone_name,
    `offering_details`.value AS internet_protocol
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
        LEFT JOIN
    `cloud`.`network_offering_details` AS `offering_details` ON `offering_details`.`network_offering_id` = `network_offerings`.`id` AND `offering_details`.`name`='internetProtocol'
GROUP BY
    `network_offerings`.`id`;

-- Set removed state for all removed accounts
UPDATE `cloud`.`account` SET state='removed' WHERE `removed` IS NOT NULL;

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
