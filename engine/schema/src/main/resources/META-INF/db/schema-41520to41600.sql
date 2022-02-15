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
-- Schema upgrade from 4.15.2.0 to 4.16.0.0
--;

ALTER TABLE `cloud`.`user_vm` ADD COLUMN `user_vm_type` varchar(255) DEFAULT "UserVM" COMMENT 'Defines the type of UserVM';

-- This is set, so as to ensure that the controller details from the ovf template are adhered to
UPDATE `cloud`.`vm_template` set deploy_as_is = 1 where id = 8;

DELETE FROM `cloud`.`configuration` WHERE name IN ("cloud.kubernetes.cluster.template.name.kvm", "cloud.kubernetes.cluster.template.name.vmware", "cloud.kubernetes.cluster.template.name.xenserver", "cloud.kubernetes.cluster.template.name.hyperv");

ALTER TABLE `cloud`.`kubernetes_cluster` ADD COLUMN `autoscaling_enabled` tinyint(1) unsigned NOT NULL DEFAULT 0;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD COLUMN `minsize` bigint;
ALTER TABLE `cloud`.`kubernetes_cluster` ADD COLUMN `maxsize` bigint;

ALTER TABLE `cloud`.`kubernetes_cluster_vm_map` ADD COLUMN `control_node` tinyint(1) unsigned NOT NULL DEFAULT 0;

-- Adding dynamic scalable flag for service offering table
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `dynamic_scaling_enabled` tinyint(1) unsigned NOT NULL DEFAULT 1  COMMENT 'true(1) if VM needs to be dynamically scalable of cpu or memory';
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
        `service_offering`.`dynamic_scaling_enabled` AS `dynamic_scaling_enabled`,
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

--;
-- Stored procedure to do idempotent column add;
-- This is copied from schema-41000to41100.sql
--;
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_COLUMN`;

CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_COLUMN` (
    IN in_table_name VARCHAR(200),
    IN in_column_name VARCHAR(200),
    IN in_column_definition VARCHAR(1000)
)
BEGIN

    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.account','created', 'datetime DEFAULT NULL COMMENT ''date created'' AFTER `state` ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.domain','created', 'datetime DEFAULT NULL COMMENT ''date created'' AFTER `next_child_seq` ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.account','created', 'datetime DEFAULT NULL COMMENT ''date created'' AFTER `state` ');

DROP VIEW IF EXISTS `cloud`.`account_view`;
CREATE VIEW `cloud`.`account_view` AS
select
    `account`.`id` AS `id`,
    `account`.`uuid` AS `uuid`,
    `account`.`account_name` AS `account_name`,
    `account`.`type` AS `type`,
    `account`.`role_id` AS `role_id`,
    `account`.`state` AS `state`,
    `account`.`created` AS `created`,
    `account`.`removed` AS `removed`,
    `account`.`cleanup_needed` AS `cleanup_needed`,
    `account`.`network_domain` AS `network_domain` ,
    `account`.`default` AS `default`,
    `domain`.`id` AS `domain_id`,
    `domain`.`uuid` AS `domain_uuid`,
    `domain`.`name` AS `domain_name`,
    `domain`.`path` AS `domain_path`,
    `data_center`.`id` AS `data_center_id`,
    `data_center`.`uuid` AS `data_center_uuid`,
    `data_center`.`name` AS `data_center_name`,
    `account_netstats_view`.`bytesReceived` AS `bytesReceived`,
    `account_netstats_view`.`bytesSent` AS `bytesSent`,
    `vmlimit`.`max` AS `vmLimit`,
    `vmcount`.`count` AS `vmTotal`,
    `runningvm`.`vmcount` AS `runningVms`,
    `stoppedvm`.`vmcount` AS `stoppedVms`,
    `iplimit`.`max` AS `ipLimit`,
    `ipcount`.`count` AS `ipTotal`,
    `free_ip_view`.`free_ip` AS `ipFree`,
    `volumelimit`.`max` AS `volumeLimit`,
    `volumecount`.`count` AS `volumeTotal`,
    `snapshotlimit`.`max` AS `snapshotLimit`,
    `snapshotcount`.`count` AS `snapshotTotal`,
    `templatelimit`.`max` AS `templateLimit`,
    `templatecount`.`count` AS `templateTotal`,
    `vpclimit`.`max` AS `vpcLimit`,
    `vpccount`.`count` AS `vpcTotal`,
    `projectlimit`.`max` AS `projectLimit`,
    `projectcount`.`count` AS `projectTotal`,
    `networklimit`.`max` AS `networkLimit`,
    `networkcount`.`count` AS `networkTotal`,
    `cpulimit`.`max` AS `cpuLimit`,
    `cpucount`.`count` AS `cpuTotal`,
    `memorylimit`.`max` AS `memoryLimit`,
    `memorycount`.`count` AS `memoryTotal`,
    `primary_storage_limit`.`max` AS `primaryStorageLimit`,
    `primary_storage_count`.`count` AS `primaryStorageTotal`,
    `secondary_storage_limit`.`max` AS `secondaryStorageLimit`,
    `secondary_storage_count`.`count` AS `secondaryStorageTotal`,
    `async_job`.`id` AS `job_id`,
    `async_job`.`uuid` AS `job_uuid`,
    `async_job`.`job_status` AS `job_status`,
    `async_job`.`account_id` AS `job_account_id`
from
    `cloud`.`free_ip_view`,
    `cloud`.`account`
        inner join
    `cloud`.`domain` ON account.domain_id = domain.id
        left join
    `cloud`.`data_center` ON account.default_zone_id = data_center.id
        left join
    `cloud`.`account_netstats_view` ON account.id = account_netstats_view.account_id
        left join
    `cloud`.`resource_limit` vmlimit ON account.id = vmlimit.account_id
        and vmlimit.type = 'user_vm'
        left join
    `cloud`.`resource_count` vmcount ON account.id = vmcount.account_id
        and vmcount.type = 'user_vm'
        left join
    `cloud`.`account_vmstats_view` runningvm ON account.id = runningvm.account_id
        and runningvm.state = 'Running'
        left join
    `cloud`.`account_vmstats_view` stoppedvm ON account.id = stoppedvm.account_id
        and stoppedvm.state = 'Stopped'
        left join
    `cloud`.`resource_limit` iplimit ON account.id = iplimit.account_id
        and iplimit.type = 'public_ip'
        left join
    `cloud`.`resource_count` ipcount ON account.id = ipcount.account_id
        and ipcount.type = 'public_ip'
        left join
    `cloud`.`resource_limit` volumelimit ON account.id = volumelimit.account_id
        and volumelimit.type = 'volume'
        left join
    `cloud`.`resource_count` volumecount ON account.id = volumecount.account_id
        and volumecount.type = 'volume'
        left join
    `cloud`.`resource_limit` snapshotlimit ON account.id = snapshotlimit.account_id
        and snapshotlimit.type = 'snapshot'
        left join
    `cloud`.`resource_count` snapshotcount ON account.id = snapshotcount.account_id
        and snapshotcount.type = 'snapshot'
        left join
    `cloud`.`resource_limit` templatelimit ON account.id = templatelimit.account_id
        and templatelimit.type = 'template'
        left join
    `cloud`.`resource_count` templatecount ON account.id = templatecount.account_id
        and templatecount.type = 'template'
        left join
    `cloud`.`resource_limit` vpclimit ON account.id = vpclimit.account_id
        and vpclimit.type = 'vpc'
        left join
    `cloud`.`resource_count` vpccount ON account.id = vpccount.account_id
        and vpccount.type = 'vpc'
        left join
    `cloud`.`resource_limit` projectlimit ON account.id = projectlimit.account_id
        and projectlimit.type = 'project'
        left join
    `cloud`.`resource_count` projectcount ON account.id = projectcount.account_id
        and projectcount.type = 'project'
        left join
    `cloud`.`resource_limit` networklimit ON account.id = networklimit.account_id
        and networklimit.type = 'network'
        left join
    `cloud`.`resource_count` networkcount ON account.id = networkcount.account_id
        and networkcount.type = 'network'
        left join
    `cloud`.`resource_limit` cpulimit ON account.id = cpulimit.account_id
        and cpulimit.type = 'cpu'
        left join
    `cloud`.`resource_count` cpucount ON account.id = cpucount.account_id
        and cpucount.type = 'cpu'
        left join
    `cloud`.`resource_limit` memorylimit ON account.id = memorylimit.account_id
        and memorylimit.type = 'memory'
        left join
    `cloud`.`resource_count` memorycount ON account.id = memorycount.account_id
        and memorycount.type = 'memory'
        left join
    `cloud`.`resource_limit` primary_storage_limit ON account.id = primary_storage_limit.account_id
        and primary_storage_limit.type = 'primary_storage'
        left join
    `cloud`.`resource_count` primary_storage_count ON account.id = primary_storage_count.account_id
        and primary_storage_count.type = 'primary_storage'
        left join
    `cloud`.`resource_limit` secondary_storage_limit ON account.id = secondary_storage_limit.account_id
        and secondary_storage_limit.type = 'secondary_storage'
        left join
    `cloud`.`resource_count` secondary_storage_count ON account.id = secondary_storage_count.account_id
        and secondary_storage_count.type = 'secondary_storage'
        left join
    `cloud`.`async_job` ON async_job.instance_id = account.id
        and async_job.instance_type = 'Account'
        and async_job.job_status = 0;


DROP VIEW IF EXISTS `cloud`.`domain_view`;
CREATE VIEW `cloud`.`domain_view` AS
select
    `domain`.`id` AS `id`,
    `domain`.`parent` AS `parent`,
    `domain`.`name` AS `name`,
    `domain`.`uuid` AS `uuid`,
    `domain`.`owner` AS `owner`,
    `domain`.`path` AS `path`,
    `domain`.`level` AS `level`,
    `domain`.`child_count` AS `child_count`,
    `domain`.`next_child_seq` AS `next_child_seq`,
    `domain`.`created` AS `created`,
    `domain`.`removed` AS `removed`,
    `domain`.`state` AS `state`,
    `domain`.`network_domain` AS `network_domain`,
    `domain`.`type` AS `type`,
    `vmlimit`.`max` AS `vmLimit`,
    `vmcount`.`count` AS `vmTotal`,
    `iplimit`.`max` AS `ipLimit`,
    `ipcount`.`count` AS `ipTotal`,
    `volumelimit`.`max` AS `volumeLimit`,
    `volumecount`.`count` AS `volumeTotal`,
    `snapshotlimit`.`max` AS `snapshotLimit`,
    `snapshotcount`.`count` AS `snapshotTotal`,
    `templatelimit`.`max` AS `templateLimit`,
    `templatecount`.`count` AS `templateTotal`,
    `vpclimit`.`max` AS `vpcLimit`,
    `vpccount`.`count` AS `vpcTotal`,
    `projectlimit`.`max` AS `projectLimit`,
    `projectcount`.`count` AS `projectTotal`,
    `networklimit`.`max` AS `networkLimit`,
    `networkcount`.`count` AS `networkTotal`,
    `cpulimit`.`max` AS `cpuLimit`,
    `cpucount`.`count` AS `cpuTotal`,
    `memorylimit`.`max` AS `memoryLimit`,
    `memorycount`.`count` AS `memoryTotal`,
    `primary_storage_limit`.`max` AS `primaryStorageLimit`,
    `primary_storage_count`.`count` AS `primaryStorageTotal`,
    `secondary_storage_limit`.`max` AS `secondaryStorageLimit`,
    `secondary_storage_count`.`count` AS `secondaryStorageTotal`
from
    `cloud`.`domain`
        left join
    `cloud`.`resource_limit` vmlimit ON domain.id = vmlimit.domain_id
        and vmlimit.type = 'user_vm'
        left join
    `cloud`.`resource_count` vmcount ON domain.id = vmcount.domain_id
        and vmcount.type = 'user_vm'
        left join
    `cloud`.`resource_limit` iplimit ON domain.id = iplimit.domain_id
        and iplimit.type = 'public_ip'
        left join
    `cloud`.`resource_count` ipcount ON domain.id = ipcount.domain_id
        and ipcount.type = 'public_ip'
        left join
    `cloud`.`resource_limit` volumelimit ON domain.id = volumelimit.domain_id
        and volumelimit.type = 'volume'
        left join
    `cloud`.`resource_count` volumecount ON domain.id = volumecount.domain_id
        and volumecount.type = 'volume'
        left join
    `cloud`.`resource_limit` snapshotlimit ON domain.id = snapshotlimit.domain_id
        and snapshotlimit.type = 'snapshot'
        left join
    `cloud`.`resource_count` snapshotcount ON domain.id = snapshotcount.domain_id
        and snapshotcount.type = 'snapshot'
        left join
    `cloud`.`resource_limit` templatelimit ON domain.id = templatelimit.domain_id
        and templatelimit.type = 'template'
        left join
    `cloud`.`resource_count` templatecount ON domain.id = templatecount.domain_id
        and templatecount.type = 'template'
        left join
    `cloud`.`resource_limit` vpclimit ON domain.id = vpclimit.domain_id
        and vpclimit.type = 'vpc'
        left join
    `cloud`.`resource_count` vpccount ON domain.id = vpccount.domain_id
        and vpccount.type = 'vpc'
        left join
    `cloud`.`resource_limit` projectlimit ON domain.id = projectlimit.domain_id
        and projectlimit.type = 'project'
        left join
    `cloud`.`resource_count` projectcount ON domain.id = projectcount.domain_id
        and projectcount.type = 'project'
        left join
    `cloud`.`resource_limit` networklimit ON domain.id = networklimit.domain_id
        and networklimit.type = 'network'
        left join
    `cloud`.`resource_count` networkcount ON domain.id = networkcount.domain_id
        and networkcount.type = 'network'
        left join
    `cloud`.`resource_limit` cpulimit ON domain.id = cpulimit.domain_id
        and cpulimit.type = 'cpu'
        left join
    `cloud`.`resource_count` cpucount ON domain.id = cpucount.domain_id
        and cpucount.type = 'cpu'
        left join
    `cloud`.`resource_limit` memorylimit ON domain.id = memorylimit.domain_id
        and memorylimit.type = 'memory'
        left join
    `cloud`.`resource_count` memorycount ON domain.id = memorycount.domain_id
        and memorycount.type = 'memory'
        left join
    `cloud`.`resource_limit` primary_storage_limit ON domain.id = primary_storage_limit.domain_id
        and primary_storage_limit.type = 'primary_storage'
        left join
    `cloud`.`resource_count` primary_storage_count ON domain.id = primary_storage_count.domain_id
        and primary_storage_count.type = 'primary_storage'
        left join
    `cloud`.`resource_limit` secondary_storage_limit ON domain.id = secondary_storage_limit.domain_id
        and secondary_storage_limit.type = 'secondary_storage'
        left join
    `cloud`.`resource_count` secondary_storage_count ON domain.id = secondary_storage_count.domain_id
        and secondary_storage_count.type = 'secondary_storage';


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
    `data_center`.`networktype` AS `data_center_type`,
    `host`.`id` AS `host_id`,
    `host`.`uuid` AS `host_uuid`,
    `host`.`name` AS `host_name`,
    `host`.`cluster_id` AS `cluster_id`,
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
    `backup_offering`.`uuid` AS `backup_offering_uuid`,
    `backup_offering`.`id` AS `backup_offering_id`,
    `svc_disk_offering`.`name` AS `service_offering_name`,
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
    (((((((((((((((((((((((((((((((((`user_vm`
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
        LEFT JOIN `backup_offering` ON ((`vm_instance`.`backup_offering_id` = `backup_offering`.`id`)))
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

-- Update name for global configuration user.vm.readonly.ui.details
Update configuration set name='user.vm.readonly.details' where name='user.vm.readonly.ui.details';

-- Update name for global configuration 'user.vm.readonly.ui.details' to 'user.vm.denied.details'
UPDATE `cloud`.`configuration` SET name='user.vm.denied.details' WHERE name='user.vm.blacklisted.details';

-- Update name for global configuration 'blacklisted.routes' to 'denied.routes'
UPDATE `cloud`.`configuration` SET name='denied.routes', description='Routes that are denied, can not be used for Static Routes creation for the VPC Private Gateway' WHERE name='blacklisted.routes';

-- Rename 'master_node_count' to 'control_node_count' in kubernetes_cluster table
ALTER TABLE `cloud`.`kubernetes_cluster` CHANGE master_node_count control_node_count bigint NOT NULL default '0' COMMENT 'the number of the control nodes deployed for this Kubernetes cluster';

UPDATE `cloud`.`domain_router` SET redundant_state = 'PRIMARY' WHERE redundant_state = 'MASTER';

DROP TABLE IF EXISTS `cloud`.`external_bigswitch_vns_devices`;
DROP TABLE IF EXISTS `cloud`.`template_s3_ref`;
DROP TABLE IF EXISTS `cloud`.`template_swift_ref`;
DROP TABLE IF EXISTS `cloud`.`template_ovf_properties`;
DROP TABLE IF EXISTS `cloud`.`op_host_upgrade`;
DROP TABLE IF EXISTS `cloud`.`stack_maid`;
DROP TABLE IF EXISTS `cloud`.`volume_host_ref`;
DROP TABLE IF EXISTS `cloud`.`template_host_ref`;
DROP TABLE IF EXISTS `cloud`.`swift`;

ALTER TABLE `cloud`.`snapshots` DROP FOREIGN KEY `fk_snapshots__s3_id` ;
ALTER TABLE `cloud`.`snapshots` DROP COLUMN `s3_id` ;
DROP TABLE IF EXISTS `cloud`.`s3`;

-- Re-create host view to prevent multiple entries for hosts with multiple tags
DROP VIEW IF EXISTS `cloud`.`host_view`;
CREATE VIEW `cloud`.`host_view` AS
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

CREATE TABLE `cloud`.`resource_icon` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `icon` blob COMMENT 'Base64 version of the resource icon',
  `resource_id` bigint unsigned NOT NULL,
  `resource_uuid` varchar(40),
  `resource_type` varchar(255),
  `updated` datetime default NULL,
  `created` datetime default NULL,
  `removed` datetime default NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_resource_icon__uuid` UNIQUE (`uuid`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`annotations` ADD COLUMN `admins_only` tinyint(1) unsigned NOT NULL DEFAULT 1;

-- Add uuid for ssh keypairs
ALTER TABLE `cloud`.`ssh_keypairs` ADD COLUMN `uuid` varchar(40) AFTER `id`;

-- PR#4699 Drop the procedure `ADD_GUEST_OS_AND_HYPERVISOR_MAPPING` if it already exist.
DROP PROCEDURE IF EXISTS `cloud`.`ADD_GUEST_OS_AND_HYPERVISOR_MAPPING`;

-- PR#4699 Create the procedure `ADD_GUEST_OS_AND_HYPERVISOR_MAPPING` to add guest_os and guest_os_hypervisor mapping.
CREATE PROCEDURE `cloud`.`ADD_GUEST_OS_AND_HYPERVISOR_MAPPING` (
    IN guest_os_category_id bigint(20) unsigned,
    IN guest_os_display_name VARCHAR(255),
    IN guest_os_hypervisor_hypervisor_type VARCHAR(32),
    IN guest_os_hypervisor_hypervisor_version VARCHAR(32),
    IN guest_os_hypervisor_guest_os_name VARCHAR(255)
)
BEGIN
	INSERT  INTO cloud.guest_os (uuid, category_id, display_name, created)
	SELECT 	UUID(), guest_os_category_id, guest_os_display_name, now()
	FROM    DUAL
	WHERE 	not exists( SELECT  1
	                    FROM    cloud.guest_os
	                    WHERE   cloud.guest_os.category_id = guest_os_category_id
	                    AND     cloud.guest_os.display_name = guest_os_display_name)

;	INSERT  INTO cloud.guest_os_hypervisor (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created)
	SELECT 	UUID(), guest_os_hypervisor_hypervisor_type, guest_os_hypervisor_hypervisor_version, guest_os_hypervisor_guest_os_name, guest_os.id, now()
	FROM 	cloud.guest_os
	WHERE 	guest_os.category_id = guest_os_category_id
	AND 	guest_os.display_name = guest_os_display_name
	AND	NOT EXISTS (SELECT  1
	                    FROM    cloud.guest_os_hypervisor as hypervisor
	                    WHERE   hypervisor_type = guest_os_hypervisor_hypervisor_type
	                    AND     hypervisor_version = guest_os_hypervisor_hypervisor_version
	                    AND     hypervisor.guest_os_id = guest_os.id
	                    AND     hypervisor.guest_os_name = guest_os_hypervisor_guest_os_name)
;END;

-- PR#4699 Call procedure `ADD_GUEST_OS_AND_HYPERVISOR_MAPPING` to add new data to guest_os and guest_os_hypervisor.
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 20.04 LTS', 'KVM', 'default', 'Ubuntu 20.04 LTS');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (10, 'Ubuntu 21.04', 'KVM', 'default', 'Ubuntu 21.04');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'pfSense 2.4', 'KVM', 'default', 'pfSense 2.4');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'OpenBSD 6.7', 'KVM', 'default', 'OpenBSD 6.7');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (9, 'OpenBSD 6.8', 'KVM', 'default', 'OpenBSD 6.8');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 8.3', 'KVM', 'default', 'AlmaLinux 8.3');

-- Alter value column of *_details table to prevent NULL values
UPDATE cloud.account_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.account_details MODIFY value varchar(255) NOT NULL;
UPDATE cloud.cluster_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.cluster_details MODIFY value varchar(255) NOT NULL;
UPDATE cloud.data_center_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.data_center_details MODIFY value varchar(1024) NOT NULL;
UPDATE cloud.domain_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.domain_details MODIFY value varchar(255) NOT NULL;
UPDATE cloud.image_store_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.image_store_details MODIFY value varchar(255) NOT NULL;
UPDATE cloud.storage_pool_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.storage_pool_details MODIFY value varchar(255) NOT NULL;
UPDATE cloud.template_deploy_as_is_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.template_deploy_as_is_details MODIFY value text NOT NULL;
UPDATE cloud.user_vm_deploy_as_is_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.user_vm_deploy_as_is_details MODIFY value text NOT NULL;
UPDATE cloud.user_vm_details SET value='' WHERE value IS NULL;
ALTER TABLE cloud.user_vm_details MODIFY value varchar(5120) NOT NULL;

ALTER TABLE cloud_usage.usage_network DROP PRIMARY KEY, ADD PRIMARY KEY (`account_id`,`zone_id`,`host_id`,`network_id`,`event_time_millis`);
ALTER TABLE `cloud`.`user_statistics` DROP INDEX `account_id`, ADD UNIQUE KEY `account_id`  (`account_id`,`data_center_id`,`public_ip_address`,`device_id`,`device_type`, `network_id`);
ALTER TABLE `cloud_usage`.`user_statistics` DROP INDEX `account_id`, ADD UNIQUE KEY `account_id`  (`account_id`,`data_center_id`,`public_ip_address`,`device_id`,`device_type`, `network_id`);
