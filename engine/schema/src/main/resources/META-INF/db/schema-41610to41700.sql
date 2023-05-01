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
-- Schema upgrade from 4.16.1.0 to 4.17.0.0
--;

-- PR#5668 Change the type of the 'ipsec_psk' field to allow large PSK.
ALTER TABLE cloud.remote_access_vpn MODIFY ipsec_psk text NOT NULL;


-- PR#5832 Fix 'endpointe.url' global settings configuration typo.
UPDATE `cloud`.`configuration` SET name='endpoint.url' WHERE name='endpointe.url';

ALTER TABLE `cloud`.`service_offering` ADD COLUMN `uuid` varchar(40) UNIQUE DEFAULT NULL;
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `name` varchar(255) NOT NULL;
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `display_text` varchar(4096) DEFAULT NULL ;
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `unique_name` varchar(32) DEFAULT NULL COMMENT 'unique name for system offerings';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `customized` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT '0 implies not customized by default';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `created` datetime DEFAULT NULL COMMENT 'date when service offering was created';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `removed` datetime DEFAULT NULL COMMENT 'date when service offering was removed';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `state` CHAR(40) NOT NULL DEFAULT 'Active' COMMENT 'state of service offering either Active or Inactive';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `disk_offering_id` bigint unsigned;
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `system_use` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'is this offering for system used only';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `disk_offering_strictness` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT 'strict binding with disk offering or not';
ALTER TABLE `cloud`.`service_offering` ADD CONSTRAINT `fk_service_offering__disk_offering_id` FOREIGN KEY `fk_service_offering__disk_offering_id`(`disk_offering_id`) REFERENCES `disk_offering`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`service_offering` DROP FOREIGN KEY `fk_service_offering__id`;

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `disk_size_strictness` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT 'To allow or disallow the resize operation on the disks created from this offering';
ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `compute_only` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT 'when set to 1, disk offering has one to one binding with service offering';

ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `disk_offering_id`;

UPDATE `cloud`.`service_offering` so, `cloud`.`disk_offering` do SET    so.`uuid` = do.`uuid`,
                                                                        so.`name` = do.`name`,
                                                                        so.`display_text` = do.`display_text`,
                                                                        so.`unique_name` = do.`unique_name`,
                                                                        so.`customized` = do.`customized`,
                                                                        so.`created` = do.`created`,
                                                                        so.`removed` = do.`removed`,
                                                                        so.`state` = do.`state`,
                                                                        so.`disk_offering_id` = do.`id`,
                                                                        so.`system_use` = do.`system_use` WHERE so.`id` = do.`id`;

UPDATE `cloud`.`disk_offering` SET `compute_only` = 1 where `type` = 'Service';
UPDATE `cloud`.`disk_offering` SET `disk_size_strictness` = 1 WHERE `compute_only` = 1 AND `disk_size` != 0;

ALTER TABLE `cloud`.`disk_offering` DROP COLUMN `type`;
ALTER TABLE `cloud`.`disk_offering` DROP COLUMN `system_use`;

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

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes','external_uuid', 'VARCHAR(40) DEFAULT null ');

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
        volumes.external_uuid,
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
        service_offering.system_use,
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
        `cloud`.`service_offering` ON vm_instance.service_offering_id = service_offering.id
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
    `vm_instance`.`dynamically_scalable` AS `dynamically_scalable`
FROM
    ((((((((((((((((((((((((((((((((`user_vm`
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
        LEFT JOIN `user_vm_details` `custom_cpu` ON (((`custom_cpu`.`vm_id` = `vm_instance`.`id`)
            AND (`custom_cpu`.`name` = 'CpuNumber'))))
        LEFT JOIN `user_vm_details` `custom_speed` ON (((`custom_speed`.`vm_id` = `vm_instance`.`id`)
            AND (`custom_speed`.`name` = 'CpuSpeed'))))
        LEFT JOIN `user_vm_details` `custom_ram_size` ON (((`custom_ram_size`.`vm_id` = `vm_instance`.`id`)
        AND (`custom_ram_size`.`name` = 'memory'))));

INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) SELECT UUID(), 3, 'listConfigurations', 'ALLOW', (SELECT MAX(`sort_order`)+1 FROM `cloud`.`role_permissions`) ON DUPLICATE KEY UPDATE rule=rule;
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) SELECT UUID(), 3, 'updateConfiguration', 'ALLOW', (SELECT MAX(`sort_order`)+1 FROM `cloud`.`role_permissions`) ON DUPLICATE KEY UPDATE rule=rule;

-- table for network permissions
CREATE TABLE  `cloud`.`network_permissions` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`),
  INDEX `i_network_permission_network_id`(`network_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `cloud`.`user_vm_details`(`vm_id`, `name`, `value`)
    SELECT `user_vm_details`.`vm_id`, 'SSH.KeyPairNames', `ssh_keypairs`.`keypair_name`
        FROM `cloud`.`user_vm_details`
        INNER JOIN `cloud`.`ssh_keypairs` ON ssh_keypairs.public_key = user_vm_details.value
        INNER JOIN `cloud`.`vm_instance` ON vm_instance.id = user_vm_details.vm_id
        WHERE ssh_keypairs.account_id = vm_instance.account_id;

ALTER TABLE `cloud`.`kubernetes_cluster` ADD COLUMN `security_group_id` bigint unsigned DEFAULT NULL,
ADD CONSTRAINT `fk_kubernetes_cluster__security_group_id` FOREIGN KEY `fk_kubernetes_cluster__security_group_id`(`security_group_id`) REFERENCES `security_group`(`id`) ON DELETE CASCADE;

-- PR#5984 Create table to persist VM stats.
DROP TABLE IF EXISTS `cloud`.`vm_stats`;
CREATE TABLE `cloud`.`vm_stats` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL,
  `mgmt_server_id` bigint unsigned NOT NULL,
  `timestamp` datetime NOT NULL,
  `vm_stats_data` text NOT NULL,
  PRIMARY KEY (`id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- PR#5984 Update name for global configuration vm.stats.increment.metrics
UPDATE `cloud`.`configuration` SET name = 'vm.stats.increment.metrics' WHERE name = 'vm.stats.increment.metrics.in.memory';

ALTER TABLE `cloud`.`domain_router` ADD COLUMN `software_version` varchar(100) COMMENT 'Software version';

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
        data_center.networktype data_center_type,
        data_center.dns1 dns1,
        data_center.dns2 dns2,
        data_center.ip6_dns1 ip6_dns1,
        data_center.ip6_dns2 ip6_dns2,
        host.id host_id,
        host.uuid host_uuid,
        host.name host_name,
        host.hypervisor_type,
        host.cluster_id cluster_id,
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

-- For IPv6 guest prefixes.
CREATE TABLE `cloud`.`dc_ip6_guest_prefix` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) DEFAULT NULL,
  `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'zone it belongs to',
  `prefix` varchar(255) NOT NULL COMMENT 'prefix of the ipv6 network',
  `created` datetime default NULL,
  `removed` datetime default NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_dc_ip6_guest_prefix__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`),
  CONSTRAINT `uc_dc_ip6_guest_prefix__uuid` UNIQUE (`uuid`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ip6_guest_prefix_subnet_network_map` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) DEFAULT NULL,
  `prefix_id` bigint(20) unsigned NOT NULL COMMENT 'ip6 guest prefix to which subnet belongs to',
  `subnet` varchar(255) NOT NULL COMMENT 'subnet of the ipv6 network',
  `network_id` bigint(20) unsigned DEFAULT NULL COMMENT 'network to which subnet is associated to',
  `state` varchar(255) NOT NULL COMMENT 'state of the subnet network',
  `updated` datetime default NULL,
  `created` datetime default NULL,
  `removed` datetime default NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_ip6_guest_prefix_subnet_network_map__prefix_id` FOREIGN KEY (`prefix_id`) REFERENCES `dc_ip6_guest_prefix`(`id`),
  CONSTRAINT `fk_ip6_guest_prefix_subnet_network_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`),
  CONSTRAINT `uc_ip6_guest_prefix_subnet_network_map__uuid` UNIQUE (`uuid`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Network offering with internet protocol
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

-- VPC offering with multi-domains and multi-zones
DROP VIEW IF EXISTS `cloud`.`vpc_offering_view`;
CREATE VIEW `cloud`.`vpc_offering_view` AS
    SELECT
        `vpc_offerings`.`id` AS `id`,
        `vpc_offerings`.`uuid` AS `uuid`,
        `vpc_offerings`.`name` AS `name`,
        `vpc_offerings`.`unique_name` AS `unique_name`,
        `vpc_offerings`.`display_text` AS `display_text`,
        `vpc_offerings`.`state` AS `state`,
        `vpc_offerings`.`default` AS `default`,
        `vpc_offerings`.`created` AS `created`,
        `vpc_offerings`.`removed` AS `removed`,
        `vpc_offerings`.`service_offering_id` AS `service_offering_id`,
        `vpc_offerings`.`supports_distributed_router` AS `supports_distributed_router`,
        `vpc_offerings`.`supports_region_level_vpc` AS `supports_region_level_vpc`,
        `vpc_offerings`.`redundant_router_service` AS `redundant_router_service`,
        `vpc_offerings`.`sort_key` AS `sort_key`,
        GROUP_CONCAT(DISTINCT(domain.id)) AS domain_id,
        GROUP_CONCAT(DISTINCT(domain.uuid)) AS domain_uuid,
        GROUP_CONCAT(DISTINCT(domain.name)) AS domain_name,
        GROUP_CONCAT(DISTINCT(domain.path)) AS domain_path,
        GROUP_CONCAT(DISTINCT(zone.id)) AS zone_id,
        GROUP_CONCAT(DISTINCT(zone.uuid)) AS zone_uuid,
        GROUP_CONCAT(DISTINCT(zone.name)) AS zone_name,
        `offering_details`.value AS internet_protocol
    FROM
        `cloud`.`vpc_offerings`
            LEFT JOIN
        `cloud`.`vpc_offering_details` AS `domain_details` ON `domain_details`.`offering_id` = `vpc_offerings`.`id` AND `domain_details`.`name`='domainid'
            LEFT JOIN
        `cloud`.`domain` AS `domain` ON FIND_IN_SET(`domain`.`id`, `domain_details`.`value`)
            LEFT JOIN
        `cloud`.`vpc_offering_details` AS `zone_details` ON `zone_details`.`offering_id` = `vpc_offerings`.`id` AND `zone_details`.`name`='zoneid'
            LEFT JOIN
        `cloud`.`data_center` AS `zone` ON FIND_IN_SET(`zone`.`id`, `zone_details`.`value`)
            LEFT JOIN
        `cloud`.`vpc_offering_details` AS `offering_details` ON `offering_details`.`offering_id` = `vpc_offerings`.`id` AND `offering_details`.`name`='internetprotocol'
    GROUP BY
        `vpc_offerings`.`id`;

-- Allow storing IPv6 CIDRs
ALTER TABLE `cloud`.`firewall_rules_cidrs` MODIFY COLUMN `source_cidr` varchar(43) DEFAULT NULL;
ALTER TABLE `cloud`.`firewall_rules_dcidrs` MODIFY COLUMN `destination_cidr` varchar(43) DEFAULT NULL;

--
-- Management Server Status
--
ALTER TABLE `cloud`.`mshost` ADD CONSTRAINT `mshost_UUID` UNIQUE KEY (`uuid`);
CREATE TABLE `cloud`.`mshost_status` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `ms_id` varchar(40) DEFAULT NULL COMMENT 'the uuid of the management server record',
  `last_jvm_start` datetime DEFAULT NULL COMMENT 'the last start time for this MS',
  `last_jvm_stop` datetime DEFAULT NULL COMMENT 'the last stop time for this MS',
  `last_system_boot` datetime DEFAULT NULL COMMENT 'the last system boot time for the host of this MS',
  `os_distribution` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'the name of the os type running on the host of this MS',
  `java_name` varchar(64) DEFAULT NULL COMMENT 'the name of the java distribution running this MS',
  `java_version` varchar(64) DEFAULT NULL COMMENT 'the version of the java distribution running this MS',
  `updated` datetime DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_ms_id` UNIQUE (`ms_id`),
  CONSTRAINT `mshost_status_FK` FOREIGN KEY (`ms_id`) REFERENCES `mshost` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3;

DROP VIEW IF EXISTS `cloud`.`mshost_view`;
CREATE VIEW `cloud`.`mshost_view` AS
select
    `cloud`.`mshost`.`id` AS `id`,
    `cloud`.`mshost`.`msid` AS `msid`,
    `cloud`.`mshost`.`runid` AS `runid`,
    `cloud`.`mshost`.`name` AS `name`,
    `cloud`.`mshost`.`uuid` AS `uuid`,
    `cloud`.`mshost`.`state` AS `state`,
    `cloud`.`mshost`.`version` AS `version`,
    `cloud`.`mshost`.`service_ip` AS `service_ip`,
    `cloud`.`mshost`.`service_port` AS `service_port`,
    `cloud`.`mshost`.`last_update` AS `last_update`,
    `cloud`.`mshost`.`removed` AS `removed`,
    `cloud`.`mshost`.`alert_count` AS `alert_count`,
    `cloud`.`mshost_status`.`last_jvm_start` AS `last_jvm_start`,
    `cloud`.`mshost_status`.`last_jvm_stop` AS `last_jvm_stop`,
    `cloud`.`mshost_status`.`last_system_boot` AS `last_system_boot`,
    `cloud`.`mshost_status`.`os_distribution` AS `os_distribution`,
    `cloud`.`mshost_status`.`java_name` AS `java_name`,
    `cloud`.`mshost_status`.`java_version` AS `java_version`
from
    (`cloud`.`mshost`
left join `cloud`.`mshost_status` on
    ((`cloud`.`mshost`.`uuid` = `cloud`.`mshost_status`.`ms_id`)));

-- Alter event table to add resource_id and resource_type
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.event','resource_id', 'bigint unsigned COMMENT "ID of the resource associated with the event" AFTER `domain_id`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.event','resource_type', 'VARCHAR(32) COMMENT "Type of the resource associated with the event" AFTER `resource_id`');

DROP VIEW IF EXISTS `cloud`.`event_view`;
CREATE VIEW `cloud`.`event_view` AS
    SELECT
        event.id,
        event.uuid,
        event.type,
        event.state,
        event.description,
        event.resource_id,
        event.resource_type,
        event.created,
        event.level,
        event.parameters,
        event.start_id,
        eve.uuid start_uuid,
        event.user_id,
        event.archived,
        event.display,
        user.username user_name,
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
        projects.name project_name
    FROM
        `cloud`.`event`
            INNER JOIN
        `cloud`.`account` ON event.account_id = account.id
            INNER JOIN
        `cloud`.`domain` ON event.domain_id = domain.id
            INNER JOIN
        `cloud`.`user` ON event.user_id = user.id
            LEFT JOIN
        `cloud`.`projects` ON projects.project_account_id = event.account_id
            LEFT JOIN
        `cloud`.`event` eve ON event.start_id = eve.id;

-- Add XenServer 8.2.1 hypervisor capabilities
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported) VALUES (UUID(), 'XenServer', '8.2.1', 1000, 253, 64, 1);

-- Copy XenServer 8.2.0 hypervisor guest OS mappings to XenServer 8.2.1
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '8.2.1', guest_os_name, guest_os_id, utc_timestamp(), 0 FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='8.2.0';

DROP PROCEDURE IF EXISTS `cloud`.`ADD_GUEST_OS_AND_HYPERVISOR_MAPPING`;
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

CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 11 (64-bit)', 'XenServer', '8.2.1', 'Debian Bullseye 11');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (2, 'Debian GNU/Linux 11 (32-bit)', 'XenServer', '8.2.1', 'Debian Bullseye 11');
-- Table for customized load balancer configurations
CREATE TABLE IF NOT EXISTS `cloud`.`load_balancer_config`  (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `uuid` varchar(40) DEFAULT NULL,
    `scope` varchar(20) DEFAULT NULL COMMENT 'The scope of this config, Vpc/Network/LoadBalancer',
    `network_id` bigint(20) unsigned DEFAULT NULL,
    `vpc_id` bigint(20) unsigned DEFAULT NULL,
    `load_balancer_id` bigint(20) unsigned DEFAULT NULL,
    `name` varchar(255) NOT NULL,
    `value` varchar(255) DEFAULT NULL,
    `created` datetime NOT NULL COMMENT 'date created',
    `removed` datetime DEFAULT NULL COMMENT 'date removed',
    PRIMARY KEY (`id`),
    UNIQUE KEY `id_UNIQUE` (`id`),
    KEY `fk_load_balancer_config_network_id` (`network_id`),
    KEY `fk_load_balancer_config_vpc_id` (`vpc_id`),
    KEY `fk_load_balancer_config_loadbalancer_id` (`load_balancer_id`),
    CONSTRAINT `fk_load_balancer_config_network_id` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_load_balancer_config_vpc_id` FOREIGN KEY (`vpc_id`) REFERENCES `vpc` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_load_balancer_config_loadbalancer_id` FOREIGN KEY (`load_balancer_id`) REFERENCES `load_balancing_rules` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
