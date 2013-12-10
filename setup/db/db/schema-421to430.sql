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
-- Schema upgrade from 4.2.1 to 4.3.0;
--;

-- Disable foreign key checking
SET foreign_key_checks = 0;

ALTER TABLE `cloud`.`async_job` ADD COLUMN `related` CHAR(40) NOT NULL;
ALTER TABLE `cloud`.`async_job` DROP COLUMN `session_key`;
ALTER TABLE `cloud`.`async_job` DROP COLUMN `job_cmd_originator`;
ALTER TABLE `cloud`.`async_job` DROP COLUMN `callback_type`;
ALTER TABLE `cloud`.`async_job` DROP COLUMN `callback_address`;

ALTER TABLE `cloud`.`async_job` ADD COLUMN `job_type` VARCHAR(32);
ALTER TABLE `cloud`.`async_job` ADD COLUMN `job_dispatcher` VARCHAR(64);
ALTER TABLE `cloud`.`async_job` ADD COLUMN `job_executing_msid` bigint;
ALTER TABLE `cloud`.`async_job` ADD COLUMN `job_pending_signals` int(10) NOT NULL DEFAULT 0;

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `keep_alive_enabled` int(1) unsigned NOT NULL DEFAULT 1 COMMENT 'true if connection should be reset after requests.';

ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `power_state` VARCHAR(74) DEFAULT 'PowerUnknown';
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `power_state_update_time` DATETIME;
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `power_state_update_count` INT DEFAULT 0;
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `power_host` bigint unsigned;
ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__power_host` FOREIGN KEY (`power_host`) REFERENCES `cloud`.`host`(`id`);

ALTER TABLE `cloud`.`load_balancing_rules` ADD COLUMN `lb_protocol` VARCHAR(40);

DROP TABLE IF EXISTS `cloud`.`vm_snapshot_details`;
CREATE TABLE `cloud`.`vm_snapshot_details` (
  `id` bigint unsigned UNIQUE NOT NULL,
  `vm_snapshot_id` bigint unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `cloud`.`snapshot_details`;
CREATE TABLE `cloud`.`snapshot_details` (
  `id` bigint unsigned UNIQUE NOT NULL,
  `snapshot_id` bigint unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vm_work_job` (
  `id` bigint unsigned UNIQUE NOT NULL,
  `step` char(32) NOT NULL COMMENT 'state',
  `vm_type` char(32) NOT NULL COMMENT 'type of vm',
  `vm_instance_id` bigint unsigned NOT NULL COMMENT 'vm instance',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vm_work_job__instance_id` FOREIGN KEY (`vm_instance_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE,
  INDEX `i_vm_work_job__vm`(`vm_type`, `vm_instance_id`),
  INDEX `i_vm_work_job__step`(`step`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`async_job_journal` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `job_id` bigint unsigned NOT NULL,
  `journal_type` varchar(32),
  `journal_text` varchar(1024) COMMENT 'journal descriptive informaton',
  `journal_obj` varchar(1024) COMMENT 'journal strutural information, JSON encoded object',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_async_job_journal__job_id` FOREIGN KEY (`job_id`) REFERENCES `async_job`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`async_job_join_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `job_id` bigint unsigned NOT NULL,
  `join_job_id` bigint unsigned NOT NULL,
  `join_status` int NOT NULL,
  `join_result` varchar(1024),
  `join_msid` bigint,
  `complete_msid` bigint,
  `sync_source_id` bigint COMMENT 'upper-level job sync source info before join',
  `wakeup_handler` varchar(64),
  `wakeup_dispatcher` varchar(64),
  `wakeup_interval` bigint NOT NULL DEFAULT 3000 COMMENT 'wakeup interval in seconds',
  `created` datetime NOT NULL,
  `last_updated` datetime,
  `next_wakeup` datetime,
  `expiration` datetime,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_async_job_join_map__job_id` FOREIGN KEY (`job_id`) REFERENCES `async_job`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_async_job_join_map__join_job_id` FOREIGN KEY (`join_job_id`) REFERENCES `async_job`(`id`),
  CONSTRAINT `fk_async_job_join_map__join` UNIQUE (`job_id`, `join_job_id`),
  INDEX `i_async_job_join_map__join_job_id`(`join_job_id`),
  INDEX `i_async_job_join_map__created`(`created`),
  INDEX `i_async_job_join_map__last_updated`(`last_updated`),
  INDEX `i_async_job_join_map__next_wakeup`(`next_wakeup`),
  INDEX `i_async_job_join_map__expiration`(`expiration`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`configuration` ADD COLUMN `default_value` VARCHAR(4095) COMMENT 'Default value for a configuration parameter';
ALTER TABLE `cloud`.`configuration` ADD COLUMN `updated` datetime COMMENT 'Time this was updated by the server. null means this row is obsolete.';
ALTER TABLE `cloud`.`configuration` ADD COLUMN `scope` VARCHAR(255) DEFAULT NULL COMMENT 'Can this parameter be scoped';
ALTER TABLE `cloud`.`configuration` ADD COLUMN `is_dynamic` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Can the parameter be change dynamically without restarting the server';

UPDATE `cloud`.`configuration` SET `default_value` = `value`;

#Upgrade the offerings and template table to have actual remove and states
ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `state` CHAR(40) NOT NULL DEFAULT 'Active' COMMENT 'state for disk offering';
ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `hv_ss_reserve` int(32) unsigned DEFAULT NULL COMMENT 'Hypervisor snapshot reserve space as a percent of a volume (for managed storage using Xen or VMware)';

ALTER TABLE `cloud`.`volumes` ADD COLUMN `hv_ss_reserve` int(32) unsigned DEFAULT NULL COMMENT 'Hypervisor snapshot reserve space as a percent of a volume (for managed storage using Xen or VMware)';

UPDATE `cloud`.`disk_offering` SET `state`='Inactive' WHERE `removed` IS NOT NULL;
UPDATE `cloud`.`disk_offering` SET `removed`=NULL;

UPDATE `cloud`.`vm_template` SET `state`='Inactive' WHERE `removed` IS NOT NULL;
UPDATE `cloud`.`vm_template` SET `state`='Active' WHERE `removed` IS NULL;
UPDATE `cloud`.`vm_template` SET `removed`=NULL;

ALTER TABLE `cloud`.`remote_access_vpn` MODIFY COLUMN `network_id` bigint unsigned;
ALTER TABLE `cloud`.`remote_access_vpn` ADD COLUMN `vpc_id` bigint unsigned default NULL;

ALTER TABLE `cloud`.`s2s_vpn_connection` ADD COLUMN `passive` int(1) unsigned NOT NULL DEFAULT 0;

DROP VIEW IF EXISTS `cloud`.`disk_offering_view`;
CREATE VIEW `cloud`.`disk_offering_view` AS
    select
        disk_offering.id,
        disk_offering.uuid,
        disk_offering.name,
        disk_offering.display_text,
        disk_offering.disk_size,
        disk_offering.min_iops,
        disk_offering.max_iops,
        disk_offering.created,
        disk_offering.tags,
        disk_offering.customized,
        disk_offering.customized_iops,
        disk_offering.removed,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        disk_offering.bytes_read_rate,
        disk_offering.bytes_write_rate,
        disk_offering.iops_read_rate,
        disk_offering.iops_write_rate,
        disk_offering.sort_key,
        disk_offering.type,
		disk_offering.display_offering,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`disk_offering`
            left join
        `cloud`.`domain` ON disk_offering.domain_id = domain.id
	where
		disk_offering.state='ACTIVE';

DROP VIEW IF EXISTS `cloud`.`service_offering_view`;
CREATE VIEW `cloud`.`service_offering_view` AS
    select 
        service_offering.id,
        disk_offering.uuid,
        disk_offering.name,
        disk_offering.display_text,
        disk_offering.created,
        disk_offering.tags,
        disk_offering.removed,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        disk_offering.bytes_read_rate,
        disk_offering.bytes_write_rate,
        disk_offering.iops_read_rate,
        disk_offering.iops_write_rate,
        service_offering.cpu,
        service_offering.speed,
        service_offering.ram_size,
        service_offering.nw_rate,
        service_offering.mc_rate,
        service_offering.ha_enabled,
        service_offering.limit_cpu_use,
        service_offering.host_tag,
        service_offering.default_use,
        service_offering.vm_type,
        service_offering.sort_key,
        service_offering.is_volatile,
        service_offering.deployment_planner,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`service_offering`
            inner join
        `cloud`.`disk_offering` ON service_offering.id = disk_offering.id
            left join
        `cloud`.`domain` ON disk_offering.domain_id = domain.id
	where
		disk_offering.state='Active';
		
DROP VIEW IF EXISTS `cloud`.`template_view`;
CREATE VIEW `cloud`.`template_view` AS
    select 
        vm_template.id,
        vm_template.uuid,
        vm_template.unique_name,
        vm_template.name,
        vm_template.public,
        vm_template.featured,
        vm_template.type,
        vm_template.hvm,
        vm_template.bits,
        vm_template.url,
        vm_template.format,
        vm_template.created,
        vm_template.checksum,
        vm_template.display_text,
        vm_template.enable_password,
        vm_template.dynamically_scalable,
        vm_template.guest_os_id,
        guest_os.uuid guest_os_uuid,
        guest_os.display_name guest_os_name,
        vm_template.bootable,
        vm_template.prepopulate,
        vm_template.cross_zones,
        vm_template.hypervisor_type,
        vm_template.extractable,
        vm_template.template_tag,
        vm_template.sort_key,
        vm_template.removed,
        vm_template.enable_sshkey,
        source_template.id source_template_id,
        source_template.uuid source_template_uuid,
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
        launch_permission.account_id lp_account_id,
        template_store_ref.store_id,
		image_store.scope as store_scope,
        template_store_ref.state,
        template_store_ref.download_state,
        template_store_ref.download_pct,
        template_store_ref.error_str,
        template_store_ref.size,
        template_store_ref.destroyed,
        template_store_ref.created created_on_store,
        vm_template_details.name detail_name,
        vm_template_details.value detail_value,
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
		CONCAT(vm_template.id, '_', IFNULL(data_center.id, 0)) as temp_zone_pair
    from
        `cloud`.`vm_template`
            inner join
        `cloud`.`guest_os` ON guest_os.id = vm_template.guest_os_id        
            inner join
        `cloud`.`account` ON account.id = vm_template.account_id
            inner join
        `cloud`.`domain` ON domain.id = account.domain_id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id    
            left join
        `cloud`.`vm_template_details` ON vm_template_details.template_id = vm_template.id         
            left join
        `cloud`.`vm_template` source_template ON source_template.id = vm_template.source_template_id    
            left join
        `cloud`.`template_store_ref` ON template_store_ref.template_id = vm_template.id and template_store_ref.store_role = 'Image'
            left join
        `cloud`.`image_store` ON image_store.removed is NULL AND template_store_ref.store_id is not NULL AND image_store.id = template_store_ref.store_id 
        	left join
        `cloud`.`template_zone_ref` ON template_zone_ref.template_id = vm_template.id AND template_store_ref.store_id is NULL AND template_zone_ref.removed is null    
            left join
        `cloud`.`data_center` ON (image_store.data_center_id = data_center.id OR template_zone_ref.zone_id = data_center.id)
            left join
        `cloud`.`launch_permission` ON launch_permission.template_id = vm_template.id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = vm_template.id
            and (resource_tags.resource_type = 'Template' or resource_tags.resource_type='ISO')
    where
        vm_template.state='Active';

DROP VIEW IF EXISTS `cloud`.`volume_view`;
CREATE VIEW `cloud`.`volume_view` AS
    select
        volumes.id,
        volumes.uuid,
        volumes.name,
        volumes.device_id,
        volumes.volume_type,
        volumes.size,
        volumes.min_iops,
        volumes.max_iops,
        volumes.created,
        volumes.state,
        volumes.attached,
        volumes.removed,
        volumes.pod_id,
	volumes.display_volume,
        volumes.format,
	volumes.path,
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
        storage_pool.id pool_id,
        storage_pool.uuid pool_uuid,
        storage_pool.name pool_name,
        cluster.hypervisor_type,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        vm_template.extractable,
        vm_template.type template_type,
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
        async_job.account_id job_account_id
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
        `cloud`.`cluster` ON storage_pool.cluster_id = cluster.id
            left join
        `cloud`.`vm_template` ON volumes.template_id = vm_template.id OR volumes.iso_id = vm_template.id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = volumes.id
            and resource_tags.resource_type = 'Volume'
            left join
        `cloud`.`async_job` ON async_job.instance_id = volumes.id
            and async_job.instance_type = 'Volume'
            and async_job.job_status = 0;
            
DROP VIEW IF EXISTS `cloud`.`storage_pool_view`;
CREATE VIEW `cloud`.`storage_pool_view` AS
    select
        storage_pool.id,
        storage_pool.uuid,
        storage_pool.name,
        storage_pool.status,
        storage_pool.path,
        storage_pool.pool_type,
        storage_pool.host_address,
        storage_pool.created,
        storage_pool.removed,
        storage_pool.capacity_bytes,
        storage_pool.capacity_iops,
        storage_pool.scope,
        storage_pool.hypervisor,
        storage_pool.storage_provider_name,
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
        storage_pool_details.name tag,
        op_host_capacity.used_capacity disk_used_capacity,
        op_host_capacity.reserved_capacity disk_reserved_capacity,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`storage_pool`
            left join
        `cloud`.`cluster` ON storage_pool.cluster_id = cluster.id
            left join
        `cloud`.`data_center` ON storage_pool.data_center_id = data_center.id
            left join
        `cloud`.`host_pod_ref` ON storage_pool.pod_id = host_pod_ref.id
            left join
        `cloud`.`storage_pool_details` ON storage_pool_details.pool_id = storage_pool.id
            and storage_pool_details.value = 'true'
            left join
        `cloud`.`op_host_capacity` ON storage_pool.id = op_host_capacity.host_id
            and op_host_capacity.capacity_type = 3
            left join
        `cloud`.`async_job` ON async_job.instance_id = storage_pool.id
            and async_job.instance_type = 'StoragePool'
            and async_job.job_status = 0;


CREATE TABLE `sslcerts` (
      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
      `uuid` varchar(40) DEFAULT NULL,
      `account_id` bigint(20) unsigned NOT NULL,
      `domain_id` bigint(20) unsigned NOT NULL,
      `certificate` text NOT NULL,
      `fingerprint` varchar(62) NOT NULL,
      `key` text NOT NULL,
      `chain` text,
      `password` varchar(255) DEFAULT NULL,
      PRIMARY KEY (`id`),
      CONSTRAINT `fk_sslcert__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
      CONSTRAINT `fk_sslcert__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE
);

CREATE TABLE `load_balancer_cert_map` (
      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
      `uuid` varchar(40) DEFAULT NULL,
      `load_balancer_id` bigint(20) unsigned NOT NULL,
      `certificate_id` bigint(20) unsigned NOT NULL,
      `revoke` tinyint(1) NOT NULL DEFAULT '0',
      PRIMARY KEY (`id`),
      CONSTRAINT `fk_load_balancer_cert_map__certificate_id` FOREIGN KEY (`certificate_id`) REFERENCES `sslcerts` (`id`) ON DELETE CASCADE,
      CONSTRAINT `fk_load_balancer_cert_map__load_balancer_id` FOREIGN KEY (`load_balancer_id`) REFERENCES `load_balancing_rules` (`id`) ON DELETE CASCADE);

ALTER TABLE `cloud`.`host` ADD COLUMN `cpu_sockets` int(10) unsigned DEFAULT NULL COMMENT "the number of CPU sockets on the host" AFTER pod_id;
ALTER TABLE `cloud`.`physical_network_traffic_types` ADD COLUMN `hyperv_network_label` varchar(255) DEFAULT NULL COMMENT 'The network name label of the physical device dedicated to this traffic on a HyperV host';

DROP VIEW IF EXISTS `cloud`.`host_view`;
CREATE VIEW `cloud`.`host_view` AS
    select
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
        async_job.account_id job_account_id
    from
        `cloud`.`host`
            left join
        `cloud`.`cluster` ON host.cluster_id = cluster.id
            left join
        `cloud`.`data_center` ON host.data_center_id = data_center.id
            left join
        `cloud`.`host_pod_ref` ON host.pod_id = host_pod_ref.id
            left join
        `cloud`.`host_details` ON host.id = host_details.host_id
            and host_details.name = 'guest.os.category.id'
            left join
        `cloud`.`guest_os_category` ON guest_os_category.id = CONVERT( host_details.value , UNSIGNED)
            left join
        `cloud`.`host_tags` ON host_tags.host_id = host.id
            left join
        `cloud`.`op_host_capacity` mem_caps ON host.id = mem_caps.host_id
            and mem_caps.capacity_type = 0
            left join
        `cloud`.`op_host_capacity` cpu_caps ON host.id = cpu_caps.host_id
            and cpu_caps.capacity_type = 1
            left join
        `cloud`.`async_job` ON async_job.instance_id = host.id
            and async_job.instance_type = 'Host'
            and async_job.job_status = 0;

CREATE TABLE `cloud`.`firewall_rule_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `firewall_rule_id` bigint unsigned NOT NULL COMMENT 'Firewall rule id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_firewall_rule_details__firewall_rule_id` FOREIGN KEY `fk_firewall_rule_details__firewall_rule_id`(`firewall_rule_id`) REFERENCES `firewall_rules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`data_center_details` ADD COLUMN `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`network_details` CHANGE `display_detail` `display` tinyint(0) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`vm_template_details` ADD COLUMN `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`volume_details` CHANGE `display_detail` `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`nic_details` CHANGE `display_detail` `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`user_vm_details` CHANGE `display_detail` `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`service_offering_details` ADD COLUMN `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`storage_pool_details` ADD COLUMN `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'ldap.group.object', 'groupOfUniqueNames',
'Sets the object type of groups within LDAP','groupOfUniqueNames',NULL,NULL,0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'ldap.group.user.uniquemember', 'uniquemember',
'Sets the attribute for uniquemembers within a group','uniquemember',NULL,NULL,0);

UPDATE `cloud`.`volumes` SET display_volume=1 where id>0;

create table `cloud`.`monitoring_services` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
`uuid` varchar(40), `service` varchar(255) COMMENT 'Service name',
`process_name` varchar(255) COMMENT 'running process name',
`service_name` varchar(255) COMMENT 'exact name of the running service',
`service_path` varchar(255) COMMENT 'path of the service in system',
`pidfile` varchar(255) COMMENT 'path of the pid file of the service',
`isDefault` boolean COMMENT 'Default service', PRIMARY KEY (`id`)
);

insert into cloud.monitoring_services(id, uuid, service, process_name, service_name, service_path, pidfile, isDefault) values(1, UUID(), 'ssh','sshd', 'ssh','/etc/init.d/ssh','/var/run/sshd.pid',true);
insert into cloud.monitoring_services(id, uuid, service, process_name, service_name, service_path, pidfile, isDefault) values(2, UUID(), 'dhcp','dnsmasq','dnsmasq','/etc/init.d/dnsmasq','/var/run/dnsmasq/dnsmasq.pid',false);
insert into cloud.monitoring_services(id, uuid, service, process_name, service_name, service_path, pidfile, isDefault) values(3, UUID(), 'loadbalancing','haproxy','haproxy','/etc/init.d/haproxy','/var/run/haproxy.pid',false);
insert into cloud.monitoring_services(id, uuid, service, process_name,  service_name, service_path, pidfile, isDefault) values(4, UUID(), 'webserver','apache2','apache2','/etc/init.d/apache2','/var/run/apache2.pid', true);

ALTER TABLE `cloud`.`service_offering` CHANGE COLUMN `cpu` `cpu` INT(10) UNSIGNED NULL COMMENT '# of cores'  , CHANGE COLUMN `speed` `speed` INT(10) UNSIGNED NULL COMMENT 'speed per core in mhz'  , CHANGE COLUMN `ram_size` `ram_size` BIGINT(20) UNSIGNED NULL  ;

CREATE TABLE `cloud`.`usage_event_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `usage_event_id` bigint unsigned NOT NULL COMMENT 'usage event id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_usage_event_details__usage_event_id` FOREIGN KEY `fk_usage_event_details__usage_event_id`(`usage_event_id`) REFERENCES `usage_event`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`user_ip_address_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `user_ip_address_id` bigint unsigned NOT NULL COMMENT 'User ip address id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_user_ip_address_details__user_ip_address_id` FOREIGN KEY `fk_user_ip_address_details__user_ip_address_id`(`user_ip_address_id`) REFERENCES `user_ip_address`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`remote_access_vpn_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `remote_access_vpn_id` bigint unsigned NOT NULL COMMENT 'Remote access vpn id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_remote_access_vpn_details__remote_access_vpn_id` FOREIGN KEY `fk_remote_access_vpn_details__remote_access_vpn_id`(`remote_access_vpn_id`) REFERENCES `remote_access_vpn`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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
        host.cluster_id cluster_id,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        service_offering.id service_offering_id,
        disk_offering.uuid service_offering_uuid,
        disk_offering.name service_offering_name,
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
        domain_router.role role
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
        `cloud`.`disk_offering` ON vm_instance.service_offering_id = disk_offering.id
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

INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Advanced", 'DEFAULT', 'management-server', "vmware.vcenter.session.timeout", "1200", "VMware client timeout in seconds", "1200", NULL,NULL,0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Advanced", 'DEFAULT', 'management-server', "mgt.server.vendor", "ACS", "the vendor of management server", "ACS", NULL,NULL,0);

ALTER TABLE `cloud_usage`.`usage_vm_instance` ADD COLUMN `cpu_speed` INT(10) UNSIGNED NULL  COMMENT 'speed per core in Mhz',
    ADD COLUMN `cpu_cores` INT(10) UNSIGNED NULL  COMMENT 'number of cpu cores',
    ADD COLUMN  `memory` INT(10) UNSIGNED NULL  COMMENT 'memory in MB';

CREATE TABLE `cloud`.`vpc_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vpc_id` bigint unsigned NOT NULL COMMENT 'VPC id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vpc_details__vpc_id` FOREIGN KEY `fk_vpc_details__vpc_id`(`vpc_id`) REFERENCES `vpc`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`vpc_gateway_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vpc_gateway_id` bigint unsigned NOT NULL COMMENT 'VPC gateway id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vpc_gateway_details__vpc_gateway_id` FOREIGN KEY `fk_vpc_gateway_details__vpc_gateway_id`(`vpc_gateway_id`) REFERENCES `vpc_gateways`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_acl_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_acl_id` bigint unsigned NOT NULL COMMENT 'VPC gateway id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_network_acl_details__network_acl_id` FOREIGN KEY `fk_network_acl_details__network_acl_id`(`network_acl_id`) REFERENCES `network_acl`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_acl_item_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_acl_item_id` bigint unsigned NOT NULL COMMENT 'VPC gateway id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_network_acl_item_details__network_acl_item_id` FOREIGN KEY `fk_network_acl_item_details__network_acl_item_id`(`network_acl_item_id`) REFERENCES `network_acl_item`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`alert` ADD COLUMN `name` varchar(255) DEFAULT NULL COMMENT 'name of the alert';

UPDATE `cloud`.`hypervisor_capabilities` SET `max_data_volumes_limit`=13 WHERE `hypervisor_type`='Vmware';

ALTER TABLE `cloud`.`external_load_balancer_devices` ADD COLUMN `is_exclusive_gslb_provider` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if load balancer appliance is acting exclusively as gslb service provider in the zone and can not be used for LB';
