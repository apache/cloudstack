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
-- Schema upgrade from 4.10.0.0 to 4.11.0.0
--;

-- Add For VPC flag
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','for_vpc', 'INT(1) NOT NULL DEFAULT 0');

UPDATE cloud.network_offerings o
SET for_vpc = 1
where
  o.conserve_mode = 0
  and o.guest_type = 'Isolated'
  and exists(
    SELECT id
    from cloud.ntwk_offering_service_map
    where network_offering_id = o.id and (
      provider in ('VpcVirtualRouter', 'InternalLbVm', 'JuniperContrailVpcRouter')
      or service in ('NetworkACL')
    )
  );

UPDATE `cloud`.`configuration` SET value = '600', default_value = '600' WHERE category = 'Advanced' AND name = 'router.aggregation.command.each.timeout';

-- CA framework changes
DELETE from `cloud`.`configuration` where name='ssl.keystore';

-- Certificate Revocation List
CREATE TABLE IF NOT EXISTS `cloud`.`crl` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `serial` varchar(255) UNIQUE NOT NULL COMMENT 'certificate\'s serial number as hex string',
  `cn` varchar(255) COMMENT 'certificate\'s common name',
  `revoker_uuid` varchar(40) COMMENT 'revoker user account uuid',
  `revoked` datetime COMMENT 'date of revocation',
  PRIMARY KEY (`id`),
  KEY (`serial`),
  UNIQUE KEY (`serial`, `cn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Host HA feature
CREATE TABLE IF NOT EXISTS `cloud`.`ha_config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resource_id` bigint(20) unsigned DEFAULT NULL COMMENT 'id of the resource',
  `resource_type` varchar(255) NOT NULL COMMENT 'the type of the resource',
  `enabled` int(1) unsigned DEFAULT '0' COMMENT 'is HA enabled for the resource',
  `ha_state` varchar(255) DEFAULT 'Disabled' COMMENT 'HA state',
  `provider` varchar(255) DEFAULT NULL COMMENT 'HA provider',
  `update_count` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT 'state based incr-only counter for atomic ha_state updates',
  `update_time` datetime COMMENT 'last ha_state update datetime',
  `mgmt_server_id` bigint(20) unsigned DEFAULT NULL COMMENT 'management server id that is responsible for the HA for the resource',
  PRIMARY KEY (`id`),
  KEY `i_ha_config__enabled` (`enabled`),
  KEY `i_ha_config__ha_state` (`ha_state`),
  KEY `i_ha_config__mgmt_server_id` (`mgmt_server_id`),
  UNIQUE KEY (`resource_id`, `resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE from `cloud`.`configuration` where name='outofbandmanagement.sync.interval';

-- Annotations specific changes following
CREATE TABLE IF NOT EXISTS `cloud`.`annotations` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) UNIQUE,
  `annotation` text,
  `entity_uuid` varchar(40),
  `entity_type` varchar(32),
  `user_uuid` varchar(40),
  `created` datetime COMMENT 'date of creation',
  `removed` datetime COMMENT 'date of removal',
  PRIMARY KEY (`id`),
  KEY (`uuid`),
  KEY `i_entity` (`entity_uuid`, `entity_type`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP VIEW IF EXISTS `cloud`.`last_annotation_view`;
CREATE VIEW `cloud`.`last_annotation_view` AS
    SELECT
        `annotations`.`uuid` AS `uuid`,
        `annotations`.`annotation` AS `annotation`,
        `annotations`.`entity_uuid` AS `entity_uuid`,
        `annotations`.`entity_type` AS `entity_type`,
        `annotations`.`user_uuid` AS `user_uuid`,
        `annotations`.`created` AS `created`,
        `annotations`.`removed` AS `removed`
    FROM
        `annotations`
    WHERE
        `annotations`.`created` IN (SELECT
                                        MAX(`annotations`.`created`)
                                    FROM
                                        `annotations`
                                    WHERE
                                        `annotations`.`removed` IS NULL
                                    GROUP BY `annotations`.`entity_uuid`);

-- Host HA changes:
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
        host_tags.tag,
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
        `cloud`.`user` ON `user`.`uuid` = `last_annotation_view`.`user_uuid`;
-- End Of Annotations specific changes

-- Out-of-band management driver for nested-cloudstack
ALTER TABLE `cloud`.`oobm` MODIFY COLUMN port VARCHAR(255);

-- CLOUDSTACK-9902: Console proxy SSL toggle
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `description`, `default_value`, `is_dynamic`) VALUES ('Console Proxy', 'DEFAULT', 'AgentManager', 'consoleproxy.sslEnabled', 'false', 'Enable SSL for console proxy', 'false', 0);

-- CLOUDSTACK-9859: Retirement of midonet plugin (final removal)
delete from `cloud`.`configuration` where name in ('midonet.apiserver.address', 'midonet.providerrouter.id');

-- CLOUDSTACK-9972: Enhance listVolumes API
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Premium', 'DEFAULT', 'management-server', 'volume.stats.interval', '600000', 'Interval (in seconds) to report volume statistics', '600000', now(), NULL, NULL);

DROP VIEW IF EXISTS `cloud`.`volume_view`;
CREATE VIEW `cloud`.`volume_view` AS
    SELECT
        volumes.id,
        volumes.uuid,
        volumes.name,
        volumes.device_id,
        volumes.volume_type,
        volumes.provisioning_type,
        volumes.size,
        volumes.min_iops,
        volumes.max_iops,
        volumes.created,
        volumes.state,
        volumes.attached,
        volumes.removed,
        volumes.display_volume,
        volumes.format,
        volumes.path,
        volumes.chain_info,
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
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_type,
        vm_instance.id vm_id,
        vm_instance.uuid vm_uuid,
        vm_instance.name vm_name,
        vm_instance.state vm_state,
        vm_instance.vm_type,
        user_vm.display_name vm_display_name,
        volume_store_ref.size volume_store_size,
        volume_store_ref.download_pct,
        volume_store_ref.download_state,
        volume_store_ref.error_str,
        volume_store_ref.created created_on_store,
        disk_offering.id disk_offering_id,
        disk_offering.uuid disk_offering_uuid,
        disk_offering.name disk_offering_name,
        disk_offering.display_text disk_offering_display_text,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        disk_offering.bytes_read_rate,
        disk_offering.bytes_write_rate,
        disk_offering.iops_read_rate,
        disk_offering.iops_write_rate,
        disk_offering.cache_mode,
        storage_pool.id pool_id,
        storage_pool.uuid pool_uuid,
        storage_pool.name pool_name,
        cluster.id cluster_id,
        cluster.name cluster_name,
        cluster.uuid cluster_uuid,
        cluster.hypervisor_type,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        vm_template.extractable,
        vm_template.type template_type,
        vm_template.name template_name,
        vm_template.display_text template_display_text,
        iso.id iso_id,
        iso.uuid iso_uuid,
        iso.name iso_name,
        iso.display_text iso_display_text,
        resource_tags.id tag_id,
        resource_tags.uuid tag_uuid,
        resource_tags.key tag_key,
        resource_tags.value tag_value,
        resource_tags.domain_id tag_domain_id,
        resource_tags.account_id tag_account_id,
        resource_tags.resource_id tag_resource_id,
        resource_tags.resource_uuid tag_resource_uuid,
        resource_tags.resource_type tag_resource_type,
        resource_tags.customer tag_customer,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id,
        host_pod_ref.id pod_id,
        host_pod_ref.uuid pod_uuid,
        host_pod_ref.name pod_name,
        resource_tag_account.account_name tag_account_name,
        resource_tag_domain.uuid tag_domain_uuid,
        resource_tag_domain.name tag_domain_name
    from
        `cloud`.`volumes`
            inner join
        `cloud`.`account` ON volumes.account_id = account.id
            inner join
        `cloud`.`domain` ON volumes.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`data_center` ON volumes.data_center_id = data_center.id
            left join
        `cloud`.`vm_instance` ON volumes.instance_id = vm_instance.id
            left join
        `cloud`.`user_vm` ON user_vm.id = vm_instance.id
            left join
        `cloud`.`volume_store_ref` ON volumes.id = volume_store_ref.volume_id
            left join
        `cloud`.`disk_offering` ON volumes.disk_offering_id = disk_offering.id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`host_pod_ref` ON storage_pool.pod_id = host_pod_ref.id
            left join
        `cloud`.`cluster` ON storage_pool.cluster_id = cluster.id
            left join
        `cloud`.`vm_template` ON volumes.template_id = vm_template.id
            left join
        `cloud`.`vm_template` iso ON iso.id = volumes.iso_id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = volumes.id
            and resource_tags.resource_type = 'Volume'
            left join
        `cloud`.`async_job` ON async_job.instance_id = volumes.id
            and async_job.instance_type = 'Volume'
            and async_job.job_status = 0
            left join
        `cloud`.`account` resource_tag_account ON resource_tag_account.id = resource_tags.account_id
            left join
        `cloud`.`domain` resource_tag_domain ON resource_tag_domain.id = resource_tags.domain_id;

-- Extra Dhcp Options
CREATE TABLE IF NOT EXISTS `cloud`.`nic_extra_dhcp_options` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `nic_id` bigint unsigned NOT NULL COMMENT ' nic id where dhcp options are applied',
  `code` int(32),
  `value` text,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_nic_extra_dhcp_options_nic_id` FOREIGN KEY (`nic_id`) REFERENCES `nics`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add new OS versions

-- Add XenServer 7.1 and 7.2 hypervisor capabilities
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, storage_motion_supported) values (UUID(), 'XenServer', '7.1.0', 500, 13, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, storage_motion_supported) values (UUID(), 'XenServer', '7.2.0', 500, 13, 1);

-- Add XenServer 7.0 support for windows 10
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.0.0', 'Windows 10 (64-bit)', 258, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.0.0', 'Windows 10 (32-bit)', 257, now(), 0);

-- Add XenServer 7.1 hypervisor guest OS mappings (copy 7.0.0)
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '7.1.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='7.0.0';

-- Add XenServer 7.1 hypervisor guest OS (see https://docs.citrix.com/content/dam/docs/en-us/xenserver/7-1/downloads/xenserver-7-1-release-notes.pdf)
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.0', 'Windows Server 2016 (64-bit)', 259, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.0', 'SUSE Linux Enterprise Server 11 SP4', 187, now(),  0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.0', 'Red Hat Enterprise Linux 6 (64-bit)', 240, now(),  0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.0', 'Red Hat Enterprise Linux 7', 245, now(),  0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.0', 'Oracle Enterprise Linux 6 (64-bit)', 251, now(),  0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.0', 'Oracle Linux 7', 247, now(),  0);

-- Add XenServer 7.2 hypervisor guest OS mappings (copy 7.1.0 & remove Windows Vista, Windows XP, Windows 2003, CentOS 4.x, RHEL 4.xS, LES 10 (all versions) as per XenServer 7.2 Release Notes)
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '7.2.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='7.1.0' AND guest_os_id not in (1,2,3,4,56,101,56,58,93,94,50,51,87,88,89,90,91,92,26,27,28,29,40,41,42,43,44,45,96,97,107,108,109,110,151,152,153);

-- Add table to track primary storage in use for snapshots
CREATE TABLE IF NOT EXISTS `cloud_usage`.`usage_snapshot_on_primary` (
  `id` bigint(20) unsigned NOT NULL,
  `zone_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `name` varchar(128),
  `type` int(1) unsigned NOT NULL,
  `physicalsize` bigint(20),
  `virtualsize` bigint(20),
  `created` datetime NOT NULL,
  `deleted` datetime,
  INDEX `i_usage_snapshot_on_primary` (`account_id`,`id`,`vm_id`,`created`)
) ENGINE=InnoDB CHARSET=utf8;

-- Change monitor patch for apache2 in systemvm
UPDATE `cloud`.`monitoring_services` SET pidfile="/var/run/apache2/apache2.pid" WHERE process_name="apache2" AND service_name="apache2";

-- Use 'Other Linux 64-bit' as guest os for the default systemvmtemplate for VMware
-- This fixes a memory allocation issue to systemvms on VMware/ESXi
UPDATE `cloud`.`vm_template` SET guest_os_id=99 WHERE id=8;

-- Network External Ids
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.networks','external_id', 'varchar(255)');

-- Separate Subnet for CPVM and SSVM (system vms)
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.op_dc_ip_address_alloc','forsystemvms', 'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Indicates if IP is dedicated for CPVM or SSVM'' ');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.op_dc_ip_address_alloc','vlan', 'INT(10) UNSIGNED NULL COMMENT ''Vlan the management network range is on'' ');

-- CLOUDSTACK-4757: Support multidisk OVA
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template','parent_template_id', 'bigint(20) unsigned DEFAULT NULL COMMENT ''If datadisk template, then id of the root template this template belongs to'' ');

-- CLOUDSTACK-10146: Bypass Secondary Storage for KVM templates
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_template','direct_download', 'TINYINT(1) DEFAULT 0 COMMENT ''Indicates if Secondary Storage is bypassed and template is downloaded to Primary Storage'' ');

-- Changes to template_view for both multidisk OVA and bypass secondary storage for KVM templates
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
          `vm_template`.`direct_download` AS `direct_download`
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
         LEFT JOIN `resource_tags` ON (((`resource_tags`.`resource_id` = `vm_template`.`id`)
             AND ((`resource_tags`.`resource_type` = 'Template')
             OR (`resource_tags`.`resource_type` = 'ISO')))));

-- CLOUDSTACK-10109: Enable dedication of public IPs to SSVM and CPVM
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.user_ip_address','forsystemvms', 'TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''true if IP is set to system vms, false if not'' ');

-- ldap binding on domain level
CREATE TABLE IF NOT EXISTS `cloud`.`domain_details` (
    `id` bigint unsigned NOT NULL auto_increment,
    `domain_id` bigint unsigned NOT NULL COMMENT 'account id',
    `name` varchar(255) NOT NULL,
    `value` varchar(255) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_domain_details__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.ldap_configuration','domain_id', 'BIGINT(20) DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.ldap_trust_map','account_id', 'BIGINT(20) DEFAULT 0');
CALL `cloud`.`IDEMPOTENT_DROP_FOREIGN_KEY`('cloud.ldap_trust_map','fk_ldap_trust_map__domain_id');
CALL `cloud`.`IDEMPOTENT_DROP_INDEX`('uk_ldap_trust_map__domain_id','cloud.ldap_trust_map');
CALL `cloud`.`IDEMPOTENT_CREATE_UNIQUE_INDEX`('uk_ldap_trust_map__bind_location','cloud.ldap_trust_map', '(domain_id, account_id)');

CREATE TABLE IF NOT EXISTS `cloud`.`netscaler_servicepackages` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `name` varchar(255) UNIQUE COMMENT 'name of the service package',
  `description` varchar(255) COMMENT 'description of the service package',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`external_netscaler_controlcenter` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `username` varchar(255) COMMENT 'username of the NCC',
  `password` varchar(255) COMMENT 'password of NCC',
  `ncc_ip` varchar(255) COMMENT 'IP of NCC Manager',
  `num_retries` bigint unsigned NOT NULL default 2 COMMENT 'Number of retries in ncc for command failure',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.sslcerts','name', 'varchar(255) NULL default NULL COMMENT ''Name of the Certificate'' ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','service_package_id', 'varchar(255) NULL default NULL COMMENT ''Netscaler ControlCenter Service Package'' ');
