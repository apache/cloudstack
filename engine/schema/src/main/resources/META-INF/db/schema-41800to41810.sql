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
-- Schema upgrade from 4.18.0.0 to 4.18.1.0
--;

-- Add support for VMware 8.0u1 (8.0.1.x)
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '8.0.1', 1024, 0, 59, 64, 1, 1);

-- Update conserve_mode of the default network offering for Tungsten Fabric (this fixes issue #7241)
UPDATE `cloud`.`network_offerings` SET conserve_mode = 0 WHERE unique_name ='DefaultTungstenFarbicNetworkOffering';

-- Add Windows Server 2022 guest OS and mappings
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'KVM', 'default', 'Windows Server 2022 (64-bit)');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '7.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '7.0.1.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '7.0.2.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '7.0.3.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '8.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '8.0.0.1', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'Xenserver', '8.2.0', 'Windows Server 2022 (64-bit)');

-- Support userdata ids and details in VM AutoScaling
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.autoscale_vmprofiles', 'user_data_id', 'bigint unsigned DEFAULT NULL COMMENT "id of the user data" AFTER `user_data`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.autoscale_vmprofiles', 'user_data_details', 'mediumtext DEFAULT NULL COMMENT "value of the comma-separated list of parameters" AFTER `user_data_id`');

-- Don't enable CPU cap for default system offerings, fixes regression from https://github.com/apache/cloudstack/pull/6420
UPDATE `cloud`.`service_offering` so
SET so.limit_cpu_use = 0
WHERE so.default_use = 1 AND so.vm_type IN ('domainrouter', 'secondarystoragevm', 'consoleproxy', 'internalloadbalancervm', 'elasticloadbalancervm');

-- fix erronous commas in guest_os names
UPDATE `cloud`.`guest_os_hypervisor` SET guest_os_name = 'rhel9_64Guest' WHERE guest_os_name = 'rhel9_64Guest,';
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os', 'display', 'tinyint(1) DEFAULT ''1'' COMMENT ''should this guest_os be shown to the end user'' ');

--
DROP VIEW IF EXISTS `cloud`.`service_offering_view`;
CREATE VIEW `cloud`.`service_offering_view` AS
SELECT
    `service_offering`.`id` AS `id`,
    `service_offering`.`uuid` AS `uuid`,
    `service_offering`.`name` AS `name`,
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
    `cloud`.`disk_offering` ON service_offering.disk_offering_id = disk_offering.id AND `disk_offering`.`state`='Active'
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
WHERE
        `service_offering`.`state`='Active'
GROUP BY
    `service_offering`.`id`;

-- Idempotent ADD COLUMN
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_COLUMN`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_COLUMN` (
    IN in_table_name VARCHAR(200)
, IN in_column_name VARCHAR(200)
, IN in_column_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Add tag column to resource_limit, resource_count and resource_reservation table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_limit', 'tag', 'varchar(64) DEFAULT NULL COMMENT "tag for the limit" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_count', 'tag', 'varchar(64) DEFAULT NULL COMMENT "tag for the resource count" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_reservation', 'tag', 'varchar(64) DEFAULT NULL COMMENT "tag for the resource reservation" ');
ALTER TABLE `cloud`.`resource_count`
DROP INDEX `i_resource_count__type_accountId`,
DROP INDEX `i_resource_count__type_domaintId`,
ADD UNIQUE INDEX `i_resource_count__type_tag_accountId` (`type`,`tag`,`account_id`),
ADD UNIQUE INDEX `i_resource_count__type_tag_domainId` (`type`,`tag`,`domain_id`);

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
        and vmlimit.type = 'user_vm' and vmlimit.tag IS NULL
        left join
    `cloud`.`resource_count` vmcount ON domain.id = vmcount.domain_id
        and vmcount.type = 'user_vm' and vmcount.tag IS NULL
        left join
    `cloud`.`resource_limit` iplimit ON domain.id = iplimit.domain_id
        and iplimit.type = 'public_ip'
        left join
    `cloud`.`resource_count` ipcount ON domain.id = ipcount.domain_id
        and ipcount.type = 'public_ip'
        left join
    `cloud`.`resource_limit` volumelimit ON domain.id = volumelimit.domain_id
        and volumelimit.type = 'volume' and volumelimit.tag IS NULL
        left join
    `cloud`.`resource_count` volumecount ON domain.id = volumecount.domain_id
        and volumecount.type = 'volume' and volumecount.tag IS NULL
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
        and cpulimit.type = 'cpu' and cpulimit.tag IS NULL
        left join
    `cloud`.`resource_count` cpucount ON domain.id = cpucount.domain_id
        and cpucount.type = 'cpu' and cpucount.tag IS NULL
        left join
    `cloud`.`resource_limit` memorylimit ON domain.id = memorylimit.domain_id
        and memorylimit.type = 'memory' and memorylimit.tag IS NULL
        left join
    `cloud`.`resource_count` memorycount ON domain.id = memorycount.domain_id
        and memorycount.type = 'memory' and memorycount.tag IS NULL
        left join
    `cloud`.`resource_limit` primary_storage_limit ON domain.id = primary_storage_limit.domain_id
        and primary_storage_limit.type = 'primary_storage' and primary_storage_limit.tag IS NULL
        left join
    `cloud`.`resource_count` primary_storage_count ON domain.id = primary_storage_count.domain_id
        and primary_storage_count.type = 'primary_storage' and primary_storage_count.tag IS NULL
        left join
    `cloud`.`resource_limit` secondary_storage_limit ON domain.id = secondary_storage_limit.domain_id
        and secondary_storage_limit.type = 'secondary_storage'
        left join
    `cloud`.`resource_count` secondary_storage_count ON domain.id = secondary_storage_count.domain_id
        and secondary_storage_count.type = 'secondary_storage';

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
        and vmlimit.type = 'user_vm' and vmlimit.tag IS NULL
        left join
    `cloud`.`resource_count` vmcount ON account.id = vmcount.account_id
        and vmcount.type = 'user_vm' and vmcount.tag IS NULL
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
        and volumelimit.type = 'volume' and volumelimit.tag IS NULL
        left join
    `cloud`.`resource_count` volumecount ON account.id = volumecount.account_id
        and volumecount.type = 'volume' and volumecount.tag IS NULL
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
        and cpulimit.type = 'cpu' and cpulimit.tag IS NULL
        left join
    `cloud`.`resource_count` cpucount ON account.id = cpucount.account_id
        and cpucount.type = 'cpu' and cpucount.tag IS NULL
        left join
    `cloud`.`resource_limit` memorylimit ON account.id = memorylimit.account_id
        and memorylimit.type = 'memory' and memorylimit.tag IS NULL
        left join
    `cloud`.`resource_count` memorycount ON account.id = memorycount.account_id
        and memorycount.type = 'memory' and memorycount.tag IS NULL
        left join
    `cloud`.`resource_limit` primary_storage_limit ON account.id = primary_storage_limit.account_id
        and primary_storage_limit.type = 'primary_storage' and primary_storage_limit.tag IS NULL
        left join
    `cloud`.`resource_count` primary_storage_count ON account.id = primary_storage_count.account_id
        and primary_storage_count.type = 'primary_storage' and primary_storage_count.tag IS NULL
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

ALTER TABLE `cloud`.`resource_reservation`
    ADD COLUMN `resource_id` bigint unsigned NULL;

ALTER TABLE `cloud`.`resource_reservation`
    MODIFY COLUMN `amount` bigint NOT NULL;

-- Add `is_implicit` column to `host_tags` table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host_tags', 'is_implicit', 'int(1) UNSIGNED NOT NULL DEFAULT 0 COMMENT "If host tag is implicit or explicit" ');

-- Update host_view for implicit host tags
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
        GROUP_CONCAT(DISTINCT(explicit_host_tags.tag)) AS explicit_tag,
        GROUP_CONCAT(DISTINCT(implicit_host_tags.tag)) AS implicit_tag,
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
        `cloud`.`host_tags` AS explicit_host_tags ON explicit_host_tags.host_id = host.id AND explicit_host_tags.is_implicit = 0
            LEFT JOIN
        `cloud`.`host_tags` AS implicit_host_tags ON implicit_host_tags.host_id = host.id AND implicit_host_tags.is_implicit = 1
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

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_reservation', 'mgmt_server_id', 'bigint unsigned NULL COMMENT "management server id" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_reservation', 'created', 'datetime DEFAULT NULL COMMENT "date when the reservation was created" ');

UPDATE `cloud`.`resource_reservation` SET `created` = now() WHERE created IS NULL;

ALTER TABLE `cloud`.`async_job` MODIFY `job_result` TEXT CHARACTER SET utf8mb4 COMMENT 'job result info';
ALTER TABLE `cloud`.`async_job` MODIFY `job_cmd_info` TEXT CHARACTER SET utf8mb4 COMMENT 'command parameter info';
ALTER TABLE `cloud`.`event` MODIFY `description` VARCHAR(1024) CHARACTER SET utf8mb4 NOT NULL;
ALTER TABLE `cloud`.`usage_event` MODIFY `resource_name` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL;
ALTER TABLE `cloud_usage`.`usage_event` MODIFY `resource_name` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL;

ALTER TABLE `cloud`.`account` MODIFY `account_name` VARCHAR(100) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'an account name set by the creator of the account, defaults to username for single accounts';
ALTER TABLE `cloud`.`affinity_group` MODIFY `description` VARCHAR(4096) CHARACTER SET utf8mb4 DEFAULT NULL;
ALTER TABLE `cloud`.`annotations` MODIFY `annotation` TEXT CHARACTER SET utf8mb4;
ALTER TABLE `cloud`.`autoscale_vmgroups` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'name of the autoscale vm group';
ALTER TABLE `cloud`.`backup_offering` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL COMMENT 'backup offering name';
ALTER TABLE `cloud`.`backup_offering` MODIFY `description` VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL COMMENT 'backup offering description';
ALTER TABLE `cloud`.`disk_offering` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL;
ALTER TABLE `cloud`.`disk_offering` MODIFY `unique_name` VARCHAR(32) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'unique name';
ALTER TABLE `cloud`.`disk_offering` MODIFY `display_text` VARCHAR(4096) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'Optional text set by the admin for display purpose only';
ALTER TABLE `cloud`.`instance_group` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL;
ALTER TABLE `cloud`.`kubernetes_cluster` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL;
ALTER TABLE `cloud`.`kubernetes_cluster` MODIFY `description` VARCHAR(4096) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'display text for this Kubernetes cluster';
ALTER TABLE `cloud`.`kubernetes_supported_version` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL COMMENT 'the name of this Kubernetes version';
ALTER TABLE `cloud`.`network_offerings` MODIFY `name` VARCHAR(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'name of the network offering';
ALTER TABLE `cloud`.`network_offerings` MODIFY `unique_name` VARCHAR(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'unique name of the network offering';
ALTER TABLE `cloud`.`network_offerings` MODIFY `display_text` VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL COMMENT 'text to display to users';
ALTER TABLE `cloud`.`networks` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'name for this network';
ALTER TABLE `cloud`.`networks` MODIFY `display_text` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'display text for this network';
ALTER TABLE `cloud`.`project_role` MODIFY `description` TEXT CHARACTER SET utf8mb4 COMMENT 'description of the project role';
ALTER TABLE `cloud`.`projects` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'project name';
ALTER TABLE `cloud`.`projects` MODIFY `display_text` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'project name';
ALTER TABLE `cloud`.`roles` MODIFY `description` TEXT CHARACTER SET utf8mb4 COMMENT 'description of the role';
ALTER TABLE `cloud`.`service_offering` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL;
ALTER TABLE `cloud`.`service_offering` MODIFY `unique_name` VARCHAR(32) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'unique name for offerings';
ALTER TABLE `cloud`.`service_offering` MODIFY `display_text` VARCHAR(4096) CHARACTER SET utf8mb4 DEFAULT NULL;
ALTER TABLE `cloud`.`snapshots` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL COMMENT 'snapshot name';
ALTER TABLE `cloud`.`ssh_keypairs` MODIFY `keypair_name` VARCHAR(256) CHARACTER SET utf8mb4 NOT NULL COMMENT 'name of the key pair';
ALTER TABLE `cloud`.`user_vm` MODIFY `display_name` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL;
ALTER TABLE `cloud`.`user_vm_details` MODIFY `value` VARCHAR(5120) CHARACTER SET utf8mb4 NOT NULL;
ALTER TABLE `cloud`.`user` MODIFY `firstname` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL;
ALTER TABLE `cloud`.`user` MODIFY `lastname` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL;
ALTER TABLE `cloud`.`user_data` MODIFY `name` VARCHAR(256) CHARACTER SET utf8mb4 NOT NULL COMMENT 'name of the user data';
ALTER TABLE `cloud`.`vm_instance` MODIFY `display_name` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL;
ALTER TABLE `cloud`.`vm_snapshots` MODIFY `display_name` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL;
ALTER TABLE `cloud`.`vm_snapshots` MODIFY `description` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL;
ALTER TABLE `cloud`.`vm_template` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 NOT NULL;
ALTER TABLE `cloud`.`vm_template` MODIFY `display_text` VARCHAR(4096) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'Description text set by the admin for display purpose only';
ALTER TABLE `cloud`.`volumes` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'A user specified name for the volume';
ALTER TABLE `cloud`.`vpc` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'vpc name';
ALTER TABLE `cloud`.`vpc` MODIFY `display_text` VARCHAR(255) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT 'vpc display text';
ALTER TABLE `cloud`.`vpc_offerings` MODIFY `name` VARCHAR(255) CHARACTER SET utf8mb4  DEFAULT NULL COMMENT 'vpc offering name';
ALTER TABLE `cloud`.`vpc_offerings` MODIFY `unique_name` VARCHAR(64) CHARACTER SET utf8mb4  DEFAULT NULL COMMENT 'unique name of the vpc offering';
ALTER TABLE `cloud`.`vpc_offerings` MODIFY `display_text` VARCHAR(255) CHARACTER SET utf8mb4  DEFAULT NULL COMMENT 'display text';
