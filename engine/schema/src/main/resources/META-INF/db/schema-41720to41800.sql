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
-- Schema upgrade from 4.17.2.0 to 4.18.0.0
--;

-- Add el9 guest OS mappings
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 9', 'KVM', 'default', 'AlmaLinux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'CentOS 9', 'KVM', 'default', 'CentOS 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Oracle Linux 9', 'KVM', 'default', 'Oracle Linux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Red Hat Enterprise Linux 9', 'KVM', 'default', 'Red Hat Enterprise Linux 9');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 9', 'KVM', 'default', 'Rocky Linux 9');

CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 9', 'VMware', '7.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Oracle Linux 9', 'VMware', '7.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Red Hat Enterprise Linux 9', 'VMware', '7.0', 'rhel9_64Guest,');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 9', 'VMware', '7.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 9', 'VMware', '7.0.1.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Oracle Linux 9', 'VMware', '7.0.1.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Red Hat Enterprise Linux 9', 'VMware', '7.0.1.0', 'rhel9_64Guest,');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 9', 'VMware', '7.0.1.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 9', 'VMware', '7.0.2.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Oracle Linux 9', 'VMware', '7.0.2.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Red Hat Enterprise Linux 9', 'VMware', '7.0.2.0', 'rhel9_64Guest,');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 9', 'VMware', '7.0.2.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'AlmaLinux 9', 'VMware', '7.0.3.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Oracle Linux 9', 'VMware', '7.0.3.0', 'otherLinux64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Red Hat Enterprise Linux 9', 'VMware', '7.0.3.0', 'rhel9_64Guest,');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (1, 'Rocky Linux 9', 'VMware', '7.0.3.0', 'otherLinux64Guest');

-- Add support for VMware 8.0 and 8.0a (8.0.0.1)
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '8.0', 1024, 0, 59, 64, 1, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '8.0.0.1', 1024, 0, 59, 64, 1, 1);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '8.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='7.0.3.0';
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '8.0.0.1', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='7.0.3.0';

-- Enable CPU cap for default system offerings;
UPDATE `cloud`.`service_offering` so
SET so.limit_cpu_use = 1
WHERE so.default_use = 1 AND so.vm_type IN ('domainrouter', 'secondarystoragevm', 'consoleproxy', 'internalloadbalancervm', 'elasticloadbalancervm');

ALTER TABLE `cloud`.`networks` ADD COLUMN `public_mtu` bigint unsigned comment "MTU for VR public interface" ;
ALTER TABLE `cloud`.`networks` ADD COLUMN `private_mtu` bigint unsigned comment "MTU for VR private interfaces" ;
ALTER TABLE `cloud`.`vpc` ADD COLUMN `public_mtu` bigint unsigned comment "MTU for VPC VR public interface" ;
ALTER TABLE `cloud`.`nics` ADD COLUMN `mtu` bigint unsigned comment "MTU for the VR interface" ;

UPDATE `cloud`.`networks` SET public_mtu = 1500, private_mtu = 1500 WHERE removed IS NULL AND guest_type='Isolated';
UPDATE `cloud`.`vpc` SET public_mtu = 1500 WHERE removed IS NULL;
UPDATE `cloud`.`nics` SET mtu = 1500 WHERE vm_type='DomainRouter' AND removed IS NULL AND reserver_name IN ('PublicNetworkGuru', 'ExternalGuestNetworkGuru');

-- Add type column to data_center table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.data_center', 'type', 'varchar(32) DEFAULT ''Core'' COMMENT ''the type of the zone'' ');

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
        data_center.type,
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

DROP VIEW IF EXISTS `cloud`.`domain_router_view`;
CREATE VIEW `cloud`.`domain_router_view` AS
    select
        vm_instance.id id,
        vm_instance.name name,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        vm_instance.uuid uuid,
        vm_instance.created created,
        vm_instance.state state,
        vm_instance.removed removed,
        vm_instance.pod_id pod_id,
        vm_instance.instance_name instance_name,
        host_pod_ref.uuid pod_uuid,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_network_type,
        data_center.dns1 dns1,
        data_center.dns2 dns2,
        data_center.ip6_dns1 ip6_dns1,
        data_center.ip6_dns2 ip6_dns2,
        host.id host_id,
        host.uuid host_uuid,
        host.name host_name,
        host.hypervisor_type,
        host.cluster_id cluster_id,
        host.status host_status,
        host.resource_state host_resource_state,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        service_offering.id service_offering_id,
        service_offering.uuid service_offering_uuid,
        service_offering.name service_offering_name,
        nics.id nic_id,
        nics.uuid nic_uuid,
        nics.network_id network_id,
        nics.ip4_address ip_address,
        nics.ip6_address ip6_address,
        nics.ip6_gateway ip6_gateway,
        nics.ip6_cidr ip6_cidr,
        nics.default_nic is_default_nic,
        nics.gateway gateway,
        nics.netmask netmask,
        nics.mac_address mac_address,
        nics.broadcast_uri broadcast_uri,
        nics.isolation_uri isolation_uri,
        nics.mtu mtu,
        vpc.id vpc_id,
        vpc.uuid vpc_uuid,
        vpc.name vpc_name,
        networks.uuid network_uuid,
        networks.name network_name,
        networks.network_domain network_domain,
        networks.traffic_type traffic_type,
        networks.guest_type guest_type,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id,
        domain_router.template_version template_version,
        domain_router.scripts_version scripts_version,
        domain_router.is_redundant_router is_redundant_router,
        domain_router.redundant_state redundant_state,
        domain_router.stop_pending stop_pending,
        domain_router.role role,
        domain_router.software_version software_version
    from
        `cloud`.`domain_router`
            inner join
        `cloud`.`vm_instance` ON vm_instance.id = domain_router.id
            inner join
        `cloud`.`account` ON vm_instance.account_id = account.id
            inner join
        `cloud`.`domain` ON vm_instance.domain_id = domain.id
            left join
        `cloud`.`host_pod_ref` ON vm_instance.pod_id = host_pod_ref.id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`data_center` ON vm_instance.data_center_id = data_center.id
            left join
        `cloud`.`host` ON vm_instance.host_id = host.id
            left join
        `cloud`.`vm_template` ON vm_instance.vm_template_id = vm_template.id
            left join
        `cloud`.`service_offering` ON vm_instance.service_offering_id = service_offering.id
            left join
        `cloud`.`nics` ON vm_instance.id = nics.instance_id and nics.removed is null
            left join
        `cloud`.`networks` ON nics.network_id = networks.id
            left join
        `cloud`.`vpc` ON domain_router.vpc_id = vpc.id and vpc.removed is null
            left join
        `cloud`.`async_job` ON async_job.instance_id = vm_instance.id
            and async_job.instance_type = 'DomainRouter'
            and async_job.job_status = 0;

-- Idempotent ADD COLUMN
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_COLUMN`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_COLUMN` (
    IN in_table_name VARCHAR(200)
, IN in_column_name VARCHAR(200)
, IN in_column_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;


-- Add foreign key procedure to link volumes to passphrase table
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY` (
    IN in_table_name VARCHAR(200),
    IN in_foreign_table_name VARCHAR(200),
    IN in_foreign_column_name VARCHAR(200)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1005,1826 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', ' ADD CONSTRAINT '); SET @ddl = CONCAT(@ddl, 'fk_', in_foreign_table_name, '_', in_foreign_column_name); SET @ddl = CONCAT(@ddl, ' FOREIGN KEY (', in_foreign_table_name, '_', in_foreign_column_name, ')'); SET @ddl = CONCAT(@ddl, ' REFERENCES ', in_foreign_table_name, '(', in_foreign_column_name, ')'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Add passphrase table
CREATE TABLE IF NOT EXISTS `cloud`.`passphrase` (
    `id` bigint unsigned NOT NULL auto_increment,
    `passphrase` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add passphrase column to volumes table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'passphrase_id', 'bigint unsigned DEFAULT NULL COMMENT "encryption passphrase id" ');
CALL `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`('cloud.volumes', 'passphrase', 'id');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'encrypt_format', 'varchar(64) DEFAULT NULL COMMENT "encryption format" ');

-- Add encrypt column to disk_offering
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.disk_offering', 'encrypt', 'tinyint(1) DEFAULT 0 COMMENT "volume encrypt requested" ');

-- add encryption support to disk offering view
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
    `disk_offering`.`compute_only` AS `compute_only`,
    `disk_offering`.`display_offering` AS `display_offering`,
    `disk_offering`.`state` AS `state`,
    `disk_offering`.`disk_size_strictness` AS `disk_size_strictness`,
    `vsphere_storage_policy`.`value` AS `vsphere_storage_policy`,
    `disk_offering`.`encrypt` AS `encrypt`,
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
        LEFT JOIN
    `cloud`.`disk_offering_details` AS `vsphere_storage_policy` ON `vsphere_storage_policy`.`offering_id` = `disk_offering`.`id`
        AND `vsphere_storage_policy`.`name` = 'storagepolicy'
WHERE
        `disk_offering`.`state`='Active'
GROUP BY
    `disk_offering`.`id`;

-- add encryption support to service offering view
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
    `cloud`.`disk_offering_view` AS `disk_offering` ON service_offering.disk_offering_id = disk_offering.id
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

-- Add cidr_list column to load_balancing_rules
ALTER TABLE `cloud`.`load_balancing_rules`
ADD cidr_list VARCHAR(4096);

-- savely add resources in parallel
-- PR#5984 Create table to persist VM stats.
DROP TABLE IF EXISTS `cloud`.`resource_reservation`;
CREATE TABLE `cloud`.`resource_reservation` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `resource_type` varchar(255) NOT NULL,
  `amount` bigint unsigned NOT NULL,
  PRIMARY KEY (`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Alter networks table to add ip6dns1 and ip6dns2
ALTER TABLE `cloud`.`networks`
    ADD COLUMN `ip6dns1` varchar(255) DEFAULT NULL COMMENT 'first IPv6 DNS for the network' AFTER `dns2`,
    ADD COLUMN `ip6dns2` varchar(255) DEFAULT NULL COMMENT 'second IPv6 DNS for the network' AFTER `ip6dns1`;
-- Alter vpc table to add dns1, dns2, ip6dns1 and ip6dns2
ALTER TABLE `cloud`.`vpc`
    ADD COLUMN `dns1` varchar(255) DEFAULT NULL COMMENT 'first IPv4 DNS for the vpc' AFTER `network_domain`,
    ADD COLUMN `dns2` varchar(255) DEFAULT NULL COMMENT 'second IPv4 DNS for the vpc' AFTER `dns1`,
    ADD COLUMN `ip6dns1` varchar(255) DEFAULT NULL COMMENT 'first IPv6 DNS for the vpc' AFTER `dns2`,
    ADD COLUMN `ip6dns2` varchar(255) DEFAULT NULL COMMENT 'second IPv6 DNS for the vpc' AFTER `ip6dns1`;

-- Fix migrateVolume permissions #6224.
DELETE role_perm
FROM role_permissions role_perm
INNER JOIN roles ON role_perm.role_id = roles.id
WHERE roles.role_type != 'Admin' AND roles.is_default = 1 AND role_perm.rule = 'migrateVolume';

-- VM autoscaling

-- Idempotent ADD COLUMN
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_COLUMN`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_COLUMN` (
    IN in_table_name VARCHAR(200)
, IN in_column_name VARCHAR(200)
, IN in_column_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Idempotent RENAME COLUMN
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_CHANGE_COLUMN`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_CHANGE_COLUMN` (
    IN in_table_name VARCHAR(200)
, IN in_column_name VARCHAR(200)
, IN in_column_new_name VARCHAR(200)
, IN in_column_new_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1054 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'CHANGE COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_new_name); SET @ddl = CONCAT(@ddl, ' ', in_column_new_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Idempotent ADD UNIQUE KEY
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_UNIQUE_KEY`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_UNIQUE_KEY` (
    IN in_table_name VARCHAR(200)
, IN in_key_name VARCHAR(200)
, IN in_key_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1061 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD UNIQUE KEY ', in_key_name); SET @ddl = CONCAT(@ddl, ' ', in_key_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Idempotent DROP FOREIGN KEY
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_DROP_FOREIGN_KEY`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_DROP_FOREIGN_KEY` (
    IN in_table_name VARCHAR(200)
, IN in_foreign_key_name VARCHAR(200)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1091, 1025 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', ' DROP FOREIGN KEY '); SET @ddl = CONCAT(@ddl, ' ', in_foreign_key_name); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

-- Add column 'supports_vm_autoscaling' to 'network_offerings' table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings', 'supports_vm_autoscaling', 'boolean default false');

-- Add column 'name' to 'autoscale_vmgroups' table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.autoscale_vmgroups', 'name', 'VARCHAR(255) DEFAULT NULL COMMENT "name of the autoscale vm group" AFTER `load_balancer_id`');
UPDATE `cloud`.`autoscale_vmgroups` SET `name` = CONCAT('AutoScale-VmGroup-',id) WHERE `name` IS NULL;

-- Add column 'next_vm_seq' to 'autoscale_vmgroups' table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.autoscale_vmgroups', 'next_vm_seq', 'BIGINT UNSIGNED NOT NULL DEFAULT 1');

-- Add column 'user_data' to 'autoscale_vmprofiles' table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.autoscale_vmprofiles', 'user_data', 'TEXT(32768) AFTER `counter_params`');

-- Add column 'name' to 'autoscale_policies' table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.autoscale_policies', 'name', 'VARCHAR(255) DEFAULT NULL COMMENT "name of the autoscale policy" AFTER `uuid`');

-- Add column 'provider' and update values
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.counter', 'provider', 'VARCHAR(255) NOT NULL COMMENT "Network provider name" AFTER `uuid`');
UPDATE `cloud`.`counter` SET provider = 'Netscaler' WHERE `provider` IS NULL OR `provider` = '';

CALL `cloud`.`IDEMPOTENT_ADD_UNIQUE_KEY`('cloud.counter', 'uc_counter__provider__source__value', '(provider, source, value)');

-- Add new counters for VM autoscaling

INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'cpu', 'VM CPU - average percentage', 'vm.cpu.average.percentage', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'memory', 'VM Memory - average percentage', 'vm.memory.average.percentage', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'virtualrouter', 'Public Network - mbps received per vm', 'public.network.received.average.mbps', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'virtualrouter', 'Public Network - mbps transmit per vm', 'public.network.transmit.average.mbps', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'virtualrouter', 'Load Balancer - average connections per vm', 'virtual.network.lb.average.connections', NOW());

INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'cpu', 'VM CPU - average percentage', 'vm.cpu.average.percentage', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'memory', 'VM Memory - average percentage', 'vm.memory.average.percentage', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'virtualrouter', 'Public Network - mbps received per vm', 'public.network.received.average.mbps', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'virtualrouter', 'Public Network - mbps transmit per vm', 'public.network.transmit.average.mbps', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'virtualrouter', 'Load Balancer - average connections per vm', 'virtual.network.lb.average.connections', NOW());

INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'None', 'cpu', 'VM CPU - average percentage', 'vm.cpu.average.percentage', NOW());
INSERT IGNORE INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'None', 'memory', 'VM Memory - average percentage', 'vm.memory.average.percentage', NOW());

-- Update autoscale_vmgroups to new state

UPDATE `cloud`.`autoscale_vmgroups` SET state= UPPER(state);

-- Update autoscale_vmgroups so records will not be removed when LB rule is removed

CALL `cloud`.`IDEMPOTENT_DROP_FOREIGN_KEY`('cloud.autoscale_vmgroups', 'fk_autoscale_vmgroup__load_balancer_id');

-- Update autoscale_vmprofiles to make autoscale_user_id optional

ALTER TABLE `cloud`.`autoscale_vmprofiles` MODIFY COLUMN `autoscale_user_id` bigint unsigned;

-- Update autoscale_vmprofiles to rename destroy_vm_grace_period

CALL `cloud`.`IDEMPOTENT_CHANGE_COLUMN`('cloud.autoscale_vmprofiles', 'destroy_vm_grace_period', 'expunge_vm_grace_period', 'int unsigned COMMENT "the time allowed for existing connections to get closed before a vm is expunged"');

-- Create table for VM autoscaling historic data

CREATE TABLE IF NOT EXISTS `cloud`.`autoscale_vmgroup_statistics` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vmgroup_id` bigint unsigned NOT NULL,
  `policy_id` bigint unsigned NOT NULL,
  `counter_id` bigint unsigned NOT NULL,
  `resource_id` bigint unsigned DEFAULT NULL,
  `resource_type` varchar(255) NOT NULL,
  `raw_value` double NOT NULL,
  `value_type` varchar(255) NOT NULL,
  `created` datetime NOT NULL COMMENT 'Date this data is created',
  `state` varchar(255) NOT NULL COMMENT 'State of the data',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_vmgroup_statistics__vmgroup_id` FOREIGN KEY `fk_autoscale_vmgroup_statistics__vmgroup_id` (`vmgroup_id`) REFERENCES `autoscale_vmgroups` (`id`) ON DELETE CASCADE,
  INDEX `i_autoscale_vmgroup_statistics__vmgroup_id`(`vmgroup_id`),
  INDEX `i_autoscale_vmgroup_statistics__policy_id`(`policy_id`),
  INDEX `i_autoscale_vmgroup_statistics__counter_id`(`counter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Update Network offering view with supports_vm_autoscaling
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

-- Update column 'supports_vm_autoscaling' to 1 if network offerings support Lb
UPDATE `cloud`.`network_offerings`
JOIN `cloud`.`ntwk_offering_service_map`
ON network_offerings.id = ntwk_offering_service_map.network_offering_id
SET network_offerings.supports_vm_autoscaling = 1
WHERE ntwk_offering_service_map.service = 'Lb'
    AND ntwk_offering_service_map.provider IN ('VirtualRouter', 'VpcVirtualRouter', 'Netscaler')
    AND network_offerings.removed IS NULL
    AND (SELECT COUNT(id) AS count FROM `network_offering_view` WHERE supports_vm_autoscaling = 1) = 0;

-- UserData as first class resource (PR #6202)
CREATE TABLE IF NOT EXISTS `cloud`.`user_data` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) NOT NULL COMMENT 'UUID of the user data',
  `name` varchar(256) NOT NULL COMMENT 'name of the user data',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner, foreign key to account table',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain, foreign key to domain table',
  `user_data` mediumtext COMMENT 'value of the userdata',
  `params` mediumtext COMMENT 'value of the comma-separated list of parameters',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_userdata__account_id` FOREIGN KEY(`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_userdata__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_userdata__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.user_vm', 'user_data_id', 'bigint unsigned DEFAULT NULL COMMENT "id of the user data" AFTER `user_data`');
CALL `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`('cloud.user_vm', 'user_data', 'id');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.user_vm', 'user_data_details', 'mediumtext DEFAULT NULL COMMENT "value of the comma-separated list of parameters" AFTER `user_data_id`');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template', 'user_data_id', 'bigint unsigned DEFAULT NULL COMMENT "id of the user data"');
CALL `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`('cloud.vm_template', 'user_data', 'id');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template', 'user_data_link_policy', 'varchar(255) DEFAULT NULL COMMENT "user data link policy with template"');

-- Added userdata details to template
DROP VIEW IF EXISTS `cloud`.`template_view`;
CREATE VIEW `cloud`.`template_view` AS
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
         `parent_template`.`id` AS `parent_template_id`,
         `parent_template`.`uuid` AS `parent_template_uuid`,
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
         `template_store_ref`.physical_size AS `physical_size`,
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
                 IFNULL(`data_center`.`id`, 0)) AS `temp_zone_pair`,
          `vm_template`.`direct_download` AS `direct_download`,
          `vm_template`.`deploy_as_is` AS `deploy_as_is`,
         `user_data`.`id` AS `user_data_id`,
         `user_data`.`uuid` AS `user_data_uuid`,
         `user_data`.`name` AS `user_data_name`,
         `user_data`.`params` AS `user_data_params`,
         `vm_template`.`user_data_link_policy` AS `user_data_policy`
     FROM
         (((((((((((((`vm_template`
         JOIN `guest_os` ON ((`guest_os`.`id` = `vm_template`.`guest_os_id`)))
         JOIN `account` ON ((`account`.`id` = `vm_template`.`account_id`)))
         JOIN `domain` ON ((`domain`.`id` = `account`.`domain_id`)))
         LEFT JOIN `projects` ON ((`projects`.`project_account_id` = `account`.`id`)))
         LEFT JOIN `vm_template_details` ON ((`vm_template_details`.`template_id` = `vm_template`.`id`)))
         LEFT JOIN `vm_template` `source_template` ON ((`source_template`.`id` = `vm_template`.`source_template_id`)))
         LEFT JOIN `template_store_ref` ON (((`template_store_ref`.`template_id` = `vm_template`.`id`)
             AND (`template_store_ref`.`store_role` = 'Image')
             AND (`template_store_ref`.`destroyed` = 0))))
         LEFT JOIN `vm_template` `parent_template` ON ((`parent_template`.`id` = `vm_template`.`parent_template_id`)))
         LEFT JOIN `image_store` ON ((ISNULL(`image_store`.`removed`)
             AND (`template_store_ref`.`store_id` IS NOT NULL)
             AND (`image_store`.`id` = `template_store_ref`.`store_id`))))
         LEFT JOIN `template_zone_ref` ON (((`template_zone_ref`.`template_id` = `vm_template`.`id`)
             AND ISNULL(`template_store_ref`.`store_id`)
             AND ISNULL(`template_zone_ref`.`removed`))))
         LEFT JOIN `data_center` ON (((`image_store`.`data_center_id` = `data_center`.`id`)
             OR (`template_zone_ref`.`zone_id` = `data_center`.`id`))))
         LEFT JOIN `launch_permission` ON ((`launch_permission`.`template_id` = `vm_template`.`id`)))
         LEFT JOIN `user_data` ON ((`user_data`.`id` = `vm_template`.`user_data_id`))
         LEFT JOIN `resource_tags` ON (((`resource_tags`.`resource_id` = `vm_template`.`id`)
             AND ((`resource_tags`.`resource_type` = 'Template')
             OR (`resource_tags`.`resource_type` = 'ISO')))));

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

-- Improve alert.email.addresses description #6806.
UPDATE  cloud.configuration
SET     description = 'Comma separated list of email addresses which are going to receive alert emails.'
WHERE   name = 'alert.email.addresses';

-- Improve description of configuration `secstorage.encrypt.copy` #6811.
UPDATE  cloud.configuration
SET     description = "Use SSL method used to encrypt copy traffic between zones. Also ensures that the certificate assigned to the zone is used when
generating links for external access."
WHERE   name = 'secstorage.encrypt.copy';

-- Create table to persist volume stats.
DROP TABLE IF EXISTS `cloud`.`volume_stats`;
CREATE TABLE `cloud`.`volume_stats` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `volume_id` bigint unsigned NOT NULL,
    `mgmt_server_id` bigint unsigned NOT NULL,
    `timestamp` datetime NOT NULL,
    `volume_stats_data` text NOT NULL,
    PRIMARY KEY(`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- allow isolated networks without services to be used as is.
UPDATE `cloud`.`networks` ntwk
  SET ntwk.state = 'Implemented'
  WHERE ntwk.network_offering_id in
    (SELECT id FROM `cloud`.`network_offerings` ntwkoff
      WHERE (SELECT count(*) FROM `cloud`.`ntwk_offering_service_map` ntwksrvcmp WHERE ntwksrvcmp.network_offering_id = ntwkoff.id) = 0
        AND ntwkoff.is_persistent = 1) AND
    ntwk.state = 'Setup' AND
    ntwk.removed is NULL AND
    ntwk.guest_type = 'Isolated';

----- PR Quota custom tariffs #5909---
-- Create column 'uuid'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `uuid` varchar(40);

UPDATE  cloud_usage.quota_tariff
SET     uuid = UUID()
WHERE   uuid is null;

ALTER TABLE cloud_usage.quota_tariff
    MODIFY      `uuid` varchar(40) NOT NULL;


-- Create column 'name'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `name` text
    COMMENT     'A name, deﬁned by the user, to the tariff. This column will be used as identiﬁer along the tariff updates.';

UPDATE  cloud_usage.quota_tariff
SET     name = case when effective_on <= now() then usage_name else concat(usage_name, '-', id) end
WHERE   name is null;

ALTER TABLE cloud_usage.quota_tariff
    MODIFY      `name` text NOT NULL;


-- Create column 'description'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `description` text DEFAULT NULL
    COMMENT     'The description of the tariff.';


-- Create column 'activation_rule'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `activation_rule` text DEFAULT NULL
    COMMENT     'JS expression that defines when the tariff should be activated.';


-- Create column 'removed'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `removed` datetime DEFAULT NULL;


-- Create column 'end_date'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `end_date` datetime DEFAULT NULL
    COMMENT     'Defines the end date of the tariff.';


-- Change usage unit to right unit
UPDATE  cloud_usage.quota_tariff
SET     usage_unit = 'Compute*Month'
WHERE   usage_unit = 'Compute-Month';

UPDATE  cloud_usage.quota_tariff
SET     usage_unit = 'IP*Month'
WHERE   usage_unit = 'IP-Month';

UPDATE  cloud_usage.quota_tariff
SET     usage_unit = 'GB*Month'
WHERE   usage_unit = 'GB-Month';

UPDATE  cloud_usage.quota_tariff
SET     usage_unit = 'Policy*Month'
WHERE   usage_unit = 'Policy-Month';

----- PR Quota custom tariffs #5909 -----

-- delete configuration task.cleanup.retry.interval #6910
DELETE FROM `cloud`.`configuration` WHERE name='task.cleanup.retry.interval';

-- Tungsten Fabric Plugin --
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','for_tungsten', 'int(1) unsigned DEFAULT "0" COMMENT "is tungsten enabled for the resource"');

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


CREATE TABLE IF NOT EXISTS `cloud`.`tungsten_providers` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `zone_id` bigint unsigned NOT NULL COMMENT 'Zone ID',
    `uuid` varchar(40),
    `host_id` bigint unsigned NOT NULL,
    `provider_name` varchar(40),
    `port` varchar(40),
    `hostname` varchar(40),
    `gateway` varchar(40),
    `vrouter_port` varchar(40),
    `introspect_port` varchar(40),
    PRIMARY KEY  (`id`),
    CONSTRAINT `fk_tungsten_providers__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tungsten_providers__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE,
    CONSTRAINT `uc_tungsten_providers__uuid` UNIQUE (`uuid`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`tungsten_guest_network_ip_address` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `network_id` bigint unsigned NOT NULL COMMENT 'network id',
    `public_ip_address` varchar(15) COMMENT 'ip public_ip_address',
    `guest_ip_address` varchar(15) NOT NULL COMMENT 'ip guest_ip_address',
    `logical_router_uuid` varchar(40) COMMENT 'logical router uuid',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fk_tungsten_guest_network_ip_address__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`tungsten_security_group_rule` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40) NOT NULL COMMENT 'rule uuid',
    `zone_id` bigint unsigned NOT NULL COMMENT 'Zone ID',
    `security_group_id` bigint unsigned NOT NULL COMMENT 'security group id',
    `rule_type` varchar(40) NOT NULL COMMENT 'rule type',
    `rule_target` varchar(40) NOT NULL COMMENT 'rule target',
    `ether_type` varchar(40) NOT NULL COMMENT 'ether type',
    `default_rule` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if security group is default',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fk_tungsten_security_group_rule__security_group_id` FOREIGN KEY (`security_group_id`) REFERENCES `security_group`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`tungsten_lb_health_monitor` (
    `id` bigint unsigned NOT NULL auto_increment,
    `uuid` varchar(40),
    `load_balancer_id` bigint unsigned NOT NULL,
    `type` varchar(40) NOT NULL,
    `retry` bigint unsigned NOT NULL,
    `timeout` bigint unsigned NOT NULL,
    `interval` bigint unsigned NOT NULL,
    `http_method` varchar(40),
    `expected_code` varchar(40),
    `url_path` varchar(255),
    PRIMARY KEY  (`id`),
    CONSTRAINT `fk_tungsten_lb_health_monitor__load_balancer_id` FOREIGN KEY(`load_balancer_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--- #6888 add index to speed up querying IPs in the network-tab
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_KEY`;

CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_KEY` (
		IN in_index_name VARCHAR(200)
    , IN in_table_name VARCHAR(200)
    , IN in_key_definition VARCHAR(1000)
)
BEGIN

    DECLARE CONTINUE HANDLER FOR 1061 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', ' ADD KEY ') ; SET @ddl = CONCAT(@ddl, ' ', in_index_name); SET @ddl = CONCAT(@ddl, ' ', in_key_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

CALL `cloud`.`IDEMPOTENT_ADD_KEY`('i_user_ip_address_state','user_ip_address', '(state)');

UPDATE  `cloud`.`role_permissions`
SET     sort_order = sort_order + 2
WHERE   rule = '*'
AND     permission = 'DENY'
AND     role_id in (SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only Admin - Default');

INSERT  INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT  UUID(), role_id, 'quotaStatement', 'ALLOW', MAX(sort_order)-1
FROM    `cloud`.`role_permissions` RP
WHERE   role_id = (SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only Admin - Default');

INSERT  INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT  UUID(), role_id, 'quotaBalance', 'ALLOW', MAX(sort_order)-2
FROM    `cloud`.`role_permissions` RP
WHERE   role_id = (SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only Admin - Default');

UPDATE  `cloud`.`role_permissions`
SET     sort_order = sort_order + 2
WHERE   rule = '*'
AND     permission = 'DENY'
AND     role_id in (SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only User - Default');

INSERT  INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT  UUID(), role_id, 'quotaStatement', 'ALLOW', MAX(sort_order)-1
FROM    `cloud`.`role_permissions` RP
WHERE   role_id = (SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only User - Default');

INSERT  INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT  UUID(), role_id, 'quotaBalance', 'ALLOW', MAX(sort_order)-2
FROM    `cloud`.`role_permissions` RP
WHERE   role_id = (SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only User - Default');

-- Add permission for domain admins to call isAccountAllowedToCreateOfferingsWithTags API

INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`)
SELECT UUID(), `roles`.`id`, 'isAccountAllowedToCreateOfferingsWithTags', 'ALLOW'
FROM `cloud`.`roles` WHERE `role_type` = 'DomainAdmin';

--
-- Update Configuration Groups and Subgroups
--
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.configuration', 'group_id', 'bigint unsigned DEFAULT 1 COMMENT "group id this configuration belongs to" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.configuration', 'subgroup_id', 'bigint unsigned DEFAULT 1 COMMENT "subgroup id this configuration belongs to" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.configuration', 'parent', 'VARCHAR(255) DEFAULT NULL COMMENT "name of the parent configuration if this depends on it" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.configuration', 'display_text', 'VARCHAR(255) DEFAULT NULL COMMENT "Short text about configuration to display to the users" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.configuration', 'kind', 'VARCHAR(255) DEFAULT NULL COMMENT "kind of the value such as order, csv, etc" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.configuration', 'options', 'VARCHAR(255) DEFAULT NULL COMMENT "possible options for the value" ');

CREATE TABLE `cloud`.`configuration_group` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of the configuration group',
  `description` varchar(1024) DEFAULT NULL COMMENT 'description of the configuration group',
  `precedence` bigint(20) unsigned DEFAULT '999' COMMENT 'precedence for the configuration group',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`configuration_subgroup` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of the configuration subgroup',
  `keywords` varchar(4096) DEFAULT NULL COMMENT 'comma-separated keywords for the configuration subgroup',
  `precedence` bigint(20) unsigned DEFAULT '999' COMMENT 'precedence for the configuration subgroup',
  `group_id` bigint(20) unsigned NOT NULL COMMENT 'configuration group id',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`name`, `group_id`),
  CONSTRAINT `fk_configuration_subgroup__group_id` FOREIGN KEY (`group_id`) REFERENCES `configuration_group` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`configuration_group` AUTO_INCREMENT=1;

INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Miscellaneous', 'Miscellaneous configuration', 999);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Access', 'Identity and Access management configuration', 1);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Compute', 'Compute configuration', 2);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Storage', 'Storage configuration', 3);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Network', 'Network configuration', 4);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Hypervisor', 'Hypervisor specific configuration', 5);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Management Server', 'Management Server configuration', 6);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('System VMs', 'System VMs related configuration', 7);
INSERT INTO `cloud`.`configuration_group` (`name`, `description`, `precedence`) VALUES ('Infrastructure', 'Infrastructure configuration', 8);

ALTER TABLE `cloud`.`configuration_subgroup` AUTO_INCREMENT=1;

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Others', NULL, 999, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Miscellaneous'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Account', NULL, 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Access'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Domain', NULL, 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Access'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Project', NULL, 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Access'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('LDAP', NULL, 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Access'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('SAML', 'saml2', 5, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Access'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Virtual Machine', 'vm,instance,cpu,ssh,affinity', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Compute'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Kubernetes', 'kubernetes', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Compute'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('High Availability', 'ha', 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Compute'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Images', 'template,iso', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Storage'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Volume', 'disk,diskoffering', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Storage'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Snapshot', NULL, 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Storage'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('VM Snapshot', 'vmsnapshot', 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Storage'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Network', 'firewall,vlan,dns,globodns,ipaddress,cidr', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Network'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('DHCP', 'externaldhcp', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Network'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('VPC', NULL, 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Network'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('LoadBalancer', 'lb,gslb', 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Network'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('API', NULL, 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Alerts', 'alert', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Events', 'event', 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Security', 'secure,password,authenticators', 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Usage', NULL, 5, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Limits', 'capacity,delay,interval,workers', 6, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Jobs', 'job', 7, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Agent', NULL, 8, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Management Server'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Hypervisor', 'host', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Hypervisor'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('KVM', 'libvirt', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Hypervisor'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('VMware', 'vcenter', 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Hypervisor'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('XenServer', 'xen,xapi,XCP', 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Hypervisor'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('OVM', 'ovm3', 5, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Hypervisor'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Baremetal', NULL, 6, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Hypervisor'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('ConsoleProxyVM', 'cpvm,consoleproxy,novnc', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'System VMs'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('SecStorageVM', 'ssvm,secondary', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'System VMs'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('VirtualRouter', 'vr,router,vrouter', 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'System VMs'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Diagnostics', NULL, 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'System VMs'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Primary Storage', 'storage,pool,primary', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Infrastructure'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Secondary Storage', 'image,secstorage', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Infrastructure'));

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Backup & Recovery', 'backup,recovery,veeam', 1, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Miscellaneous'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Certificate Authority', 'CA', 2, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Miscellaneous'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Quota', NULL, 3, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Miscellaneous'));
INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('Cloudian', NULL, 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Miscellaneous'));

UPDATE `cloud`.`configuration` SET parent = 'agent.lb.enabled' WHERE name IN ('agent.load.threshold');
UPDATE `cloud`.`configuration` SET parent = 'indirect.agent.lb.check.interval' WHERE name IN ('indirect.agent.lb.algorithm');
UPDATE `cloud`.`configuration` SET parent = 'alert.purge.delay' WHERE name IN ('alert.purge.interval');
UPDATE `cloud`.`configuration` SET parent = 'api.throttling.enabled' WHERE name IN ('api.throttling.cachesize', 'api.throttling.interval', 'api.throttling.max');
UPDATE `cloud`.`configuration` SET parent = 'backup.framework.enabled' WHERE name IN ('backup.framework.provider.plugin', 'backup.framework.sync.interval');
UPDATE `cloud`.`configuration` SET parent = 'cloud.kubernetes.service.enabled' WHERE name IN ('cloud.kubernetes.cluster.max.size', 'cloud.kubernetes.cluster.network.offering', 'cloud.kubernetes.cluster.scale.timeout', 'cloud.kubernetes.cluster.start.timeout', 'cloud.kubernetes.cluster.upgrade.timeout', 'cloud.kubernetes.cluster.experimental.features.enabled');
UPDATE `cloud`.`configuration` SET parent = 'diagnostics.data.gc.enable' WHERE name IN ('diagnostics.data.gc.interval', 'diagnostics.data.max.file.age');
UPDATE `cloud`.`configuration` SET parent = 'enable.additional.vm.configuration' WHERE name IN ('allow.additional.vm.configuration.list.kvm', 'allow.additional.vm.configuration.list.vmware', 'allow.additional.vm.configuration.list.xenserver');
UPDATE `cloud`.`configuration` SET parent = 'event.purge.delay' WHERE name IN ('event.purge.interval');
UPDATE `cloud`.`configuration` SET parent = 'network.loadbalancer.basiczone.elb.enabled' WHERE name IN ('network.loadbalancer.basiczone.elb.network', 'network.loadbalancer.basiczone.elb.vm.cpu.mhz', 'network.loadbalancer.basiczone.elb.vm.ram.size', 'network.loadbalancer.basiczone.elb.vm.vcpu.num', 'network.loadbalancer.basiczone.elb.gc.interval.minutes');
UPDATE `cloud`.`configuration` SET parent = 'prometheus.exporter.enable' WHERE name IN ('prometheus.exporter.port', 'prometheus.exporter.allowed.ips');
UPDATE `cloud`.`configuration` SET parent = 'router.health.checks.enable' WHERE name IN ('router.health.checks.basic.interval', 'router.health.checks.advanced.interval', 'router.health.checks.config.refresh.interval', 'router.health.checks.results.fetch.interval', 'router.health.checks.to.exclude', 'router.health.checks.failures.to.recreate.vr', 'router.health.checks.free.disk.space.threshold', 'router.health.checks.max.cpu.usage.threshold', 'router.health.checks.max.memory.usage.threshold');
UPDATE `cloud`.`configuration` SET parent = 'storage.cache.replacement.enabled' WHERE name IN ('storage.cache.replacement.interval', 'storage.cache.replacement.lru.interval');
UPDATE `cloud`.`configuration` SET parent = 'storage.cleanup.enabled' WHERE name IN ('storage.cleanup.interval', 'storage.cleanup.delay', 'storage.template.cleanup.enabled');
UPDATE `cloud`.`configuration` SET parent = 'vm.configdrive.primarypool.enabled' WHERE name IN ('vm.configdrive.use.host.cache.on.unsupported.pool');

UPDATE `cloud`.`configuration` SET display_text = CONCAT(UCASE(LEFT(REPLACE(name, ".", " "), 1)), LCASE(SUBSTRING(REPLACE(name, ".", " "), 2)));

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'HostAntiAffinityProcessor,ExplicitDedicationProcessor,HostAffinityProcessor'
where `name` = 'affinity.processors.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'FirstFitPlanner,UserDispersingPlanner,UserConcentratedPodPlanner,ImplicitDedicationPlanner,BareMetalPlanner'
    where `name` = 'deployment.planners.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'SimpleInvestigator,XenServerInvestigator,KVMInvestigator,HypervInvestigator,VMwareInvestigator,PingInvestigator,ManagementIPSysVMInvestigator,Ovm3Investigator'
where `name` = 'ha.investigators.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'FirstFitRouting'
where `name` = 'host.allocators.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'SAML2Auth'
where `name` = 'pluggableApi.authenticators.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'AffinityGroupAccessChecker,DomainChecker'
where `name` = 'security.checkers.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'LocalStorage,ClusterScopeStoragePoolAllocator,ZoneWideStoragePoolAllocator'
where `name` = 'storage.pool.allocators.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'PBKDF2,SHA256SALT,MD5,LDAP,SAML2,PLAINTEXT'
where `name` = 'user.authenticators.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Order',
    `options` = 'PBKDF2,SHA256SALT,MD5,LDAP,SAML2,PLAINTEXT'
where `name` = 'user.password.encoders.order' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'CSV'
where `name` like "%.list" ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'CSV'
where `name` like "%.defaults" ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'CSV'
where `name` like "%.details" ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'CSV'
where `name` like "%.exclude" ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'CSV'
where `name` IN ("alert.email.addresses", "allow.additional.vm.configuration.list.kvm", "allow.additional.vm.configuration.list.xenserver", "host",
    "network.dhcp.nondefaultnetwork.setgateway.guestos", "router.health.checks.failures.to.recreate.vr", "router.health.checks.to.exclude") ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'Error,Migration,ForceStop'
where `name` = 'host.maintenance.local.storage.strategy' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'SHA256withRSA'
where `name` = 'ca.framework.cert.signature.algorithm' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'firstfitleastconsumed,random'
where `name` = 'image.store.allocation.algorithm' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'static,roundrobin,shuffle'
where `name` = 'indirect.agent.lb.algorithm' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'random,firstfit,userdispersing,userconcentratedpod_random,userconcentratedpod_firstfit,firstfitleastconsumed'
where `name` = 'vm.allocation.algorithm' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'all,pod'
where `name` = 'network.dns.basiczone.updates' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'global,guest-network,link-local,disabled,all,default'
where `name` = 'network.loadbalancer.haproxy.stats.visibility' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'SHA1,SHA256,SHA384,SHA512'
where `name` = 'saml2.sigalg' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'FirstFitPlanner,UserDispersingPlanner,UserConcentratedPodPlanner'
where `name` = 'vm.deployment.planner' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'scsi,ide,osdefault'
where `name` = 'vmware.root.disk.controller' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'E1000,PCNet32,Vmxnet2,Vmxnet3'
where `name` = 'vmware.systemvm.nic.device.type' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'first,last,random'
where `name` = 'vrouter.redundant.tiers.placement' ;

UPDATE `cloud`.`configuration` SET
    `kind` = 'Select',
    `options` = 'xenserver56,xenserver61'
where `name` = 'xenserver.pvdriver.version' ;

--- Create table for handling console sessions #7094

CREATE TABLE IF NOT EXISTS `cloud`.`console_session` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `uuid` varchar(40) NOT NULL COMMENT 'UUID generated for the session',
    `created` datetime NOT NULL COMMENT 'When the session was created',
    `account_id` bigint(20) unsigned NOT NULL COMMENT 'Account who generated the session',
    `user_id` bigint(20) unsigned NOT NULL COMMENT 'User who generated the session',
    `instance_id` bigint(20) unsigned NOT NULL COMMENT 'VM for which the session was generated',
    `host_id` bigint(20) unsigned NOT NULL COMMENT 'Host where the VM was when the session was generated',
    `acquired` int(1) NOT NULL DEFAULT 0 COMMENT 'True if the session was already used',
    `removed` datetime COMMENT 'When the session was removed/used',
    CONSTRAINT `fk_consolesession__account_id` FOREIGN KEY(`account_id`) REFERENCES `cloud`.`account` (`id`),
    CONSTRAINT `fk_consolesession__user_id` FOREIGN KEY(`user_id`) REFERENCES `cloud`.`user`(`id`),
    CONSTRAINT `fk_consolesession__instance_id` FOREIGN KEY(`instance_id`) REFERENCES `cloud`.`vm_instance`(`id`),
    CONSTRAINT `fk_consolesession__host_id` FOREIGN KEY(`host_id`) REFERENCES `cloud`.`host`(`id`),
    CONSTRAINT `uc_consolesession__uuid` UNIQUE (`uuid`)
);

-- Add assignVolume API permission to default resource admin and domain admin
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`) VALUES (UUID(), 2, 'assignVolume', 'ALLOW');
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`) VALUES (UUID(), 3, 'assignVolume', 'ALLOW');

-- Increases the precision of the column `quota_used` from 15 to 20, keeping the scale of 8.

ALTER TABLE `cloud_usage`.`quota_usage` MODIFY COLUMN quota_used decimal(20,8) unsigned NOT NULL;

ALTER TABLE `cloud`.`user` ADD COLUMN `is_user_2fa_enabled` tinyint NOT NULL DEFAULT 0;
ALTER TABLE `cloud`.`user` ADD COLUMN `key_for_2fa` varchar(255) default NULL;
ALTER TABLE `cloud`.`user` ADD COLUMN `user_2fa_provider` varchar(255) default NULL;

DROP VIEW IF EXISTS `cloud`.`user_view`;
CREATE VIEW `cloud`.`user_view` AS
    select
        user.id,
        user.uuid,
        user.username,
        user.password,
        user.firstname,
        user.lastname,
        user.email,
        user.state,
        user.api_key,
        user.secret_key,
        user.created,
        user.removed,
        user.timezone,
        user.registration_token,
        user.is_registered,
        user.incorrect_login_attempts,
        user.source,
        user.default,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        account.role_id account_role_id,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id,
        user.is_user_2fa_enabled is_user_2fa_enabled
    from
        `cloud`.`user`
            inner join
        `cloud`.`account` ON user.account_id = account.id
            inner join
        `cloud`.`domain` ON account.domain_id = domain.id
            left join
        `cloud`.`async_job` ON async_job.instance_id = user.id
            and async_job.instance_type = 'User'
            and async_job.job_status = 0;

-- Remove snapshot references if primary storage pool has been removed, see github issue #7093
DELETE FROM `cloud`.`snapshot_store_ref`
WHERE store_role = "Primary" AND store_id IN (SELECT id FROM storage_pool WHERE removed IS NOT NULL);


-- Change usage of VM_DISK_IO_WRITE to use right usage_type
UPDATE
  `cloud_usage`.`cloud_usage`
SET
  usage_type = 22
WHERE
  usage_type = 24 AND usage_display like '% io write';
