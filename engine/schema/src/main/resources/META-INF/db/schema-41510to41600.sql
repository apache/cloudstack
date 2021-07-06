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
-- Schema upgrade from 4.15.1.0 to 4.16.0.0
--;

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
