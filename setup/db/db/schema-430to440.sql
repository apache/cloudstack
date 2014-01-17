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
-- Schema upgrade from 4.3.0 to 4.4.0;
--;

-- Disable foreign key checking
SET foreign_key_checks = 0;

ALTER TABLE `cloud`.`disk_offering` ADD `cache_mode` VARCHAR( 16 ) DEFAULT 'none' COMMENT 'The disk cache mode to use for disks created with this offering';

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
        disk_offering.cache_mode,
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
        disk_offering.cache_mode,
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
        disk_offering.cache_mode,
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

UPDATE `cloud`.`configuration` SET `description` = 'If set to true, StartCommand, StopCommand, CopyCommand, MigrateCommand will be synchronized on the agent side. If set to false, these commands become asynchronous. Default value is true.' WHERE `name` = 'execute.in.sequence.hypervisor.commands';
ALTER TABLE `cloud`.`disk_offering_details` CHANGE `display_detail` `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';

CREATE TABLE `cloud`.`user_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `user_id` bigint unsigned NOT NULL COMMENT 'VPC gateway id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_user_details__user_id` FOREIGN KEY `fk_user_details__user_id`(`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`external_opendaylight_controllers` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which the device is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name corresponding to this device',
  `device_name` varchar(255) NOT NULL COMMENT 'name of the device',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id corresponding to the external device',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_external_opendaylight_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_opendaylight_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP VIEW IF EXISTS `cloud`.`user_vm_view`;
CREATE VIEW `cloud`.`user_vm_view` AS
    select
        vm_instance.id id,
        vm_instance.name name,
        user_vm.display_name display_name,
        user_vm.user_data user_data,
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
        instance_group.id instance_group_id,
        instance_group.uuid instance_group_uuid,
        instance_group.name instance_group_name,
        vm_instance.uuid uuid,
        vm_instance.last_host_id last_host_id,
        vm_instance.vm_type type,
        vm_instance.vnc_password vnc_password,
        vm_instance.limit_cpu_use limit_cpu_use,
        vm_instance.created created,
        vm_instance.state state,
        vm_instance.removed removed,
        vm_instance.ha_enabled ha_enabled,
        vm_instance.hypervisor_type hypervisor_type,
        vm_instance.instance_name instance_name,
        vm_instance.guest_os_id guest_os_id,
        vm_instance.display_vm display_vm,
        guest_os.uuid guest_os_uuid,
        vm_instance.pod_id pod_id,
        host_pod_ref.uuid pod_uuid,
        vm_instance.private_ip_address private_ip_address,
        vm_instance.private_mac_address private_mac_address,
        vm_instance.vm_type vm_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.is_security_group_enabled security_group_enabled,
        data_center.networktype data_center_type,
        host.id host_id,
        host.uuid host_uuid,
        host.name host_name,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        vm_template.name template_name,
        vm_template.display_text template_display_text,
        vm_template.enable_password password_enabled,
        iso.id iso_id,
        iso.uuid iso_uuid,
        iso.name iso_name,
        iso.display_text iso_display_text,
        service_offering.id service_offering_id,
        svc_disk_offering.uuid service_offering_uuid,
        disk_offering.uuid disk_offering_uuid,
        disk_offering.id disk_offering_id,
        Case
             When (`cloud`.`service_offering`.`cpu` is null) then (`custom_cpu`.`value`)
             Else ( `cloud`.`service_offering`.`cpu`)
        End as `cpu`,
        Case
            When (`cloud`.`service_offering`.`speed` is null) then (`custom_speed`.`value`)
            Else ( `cloud`.`service_offering`.`speed`)
        End as `speed`,
        Case
            When (`cloud`.`service_offering`.`ram_size` is null) then (`custom_ram_size`.`value`)
            Else ( `cloud`.`service_offering`.`ram_size`)
        END as `ram_size`,
        svc_disk_offering.name service_offering_name,
        disk_offering.name disk_offering_name,
        storage_pool.id pool_id,
        storage_pool.uuid pool_uuid,
        storage_pool.pool_type pool_type,
        volumes.id volume_id,
        volumes.uuid volume_uuid,
        volumes.device_id volume_device_id,
        volumes.volume_type volume_type,
        security_group.id security_group_id,
        security_group.uuid security_group_uuid,
        security_group.name security_group_name,
        security_group.description security_group_description,
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
        networks.traffic_type traffic_type,
        networks.guest_type guest_type,
        user_ip_address.id public_ip_id,
        user_ip_address.uuid public_ip_uuid,
        user_ip_address.public_ip_address public_ip_address,
        ssh_keypairs.keypair_name keypair_name,
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
        affinity_group.id affinity_group_id,
        affinity_group.uuid affinity_group_uuid,
        affinity_group.name affinity_group_name,
        affinity_group.description affinity_group_description,
        vm_instance.dynamically_scalable dynamically_scalable,
        all_details.name detail_name,
        all_details.value detail_value

    from
        `cloud`.`user_vm`
            inner join
        `cloud`.`vm_instance` ON vm_instance.id = user_vm.id
            and vm_instance.removed is NULL
            inner join
        `cloud`.`account` ON vm_instance.account_id = account.id
            inner join
        `cloud`.`domain` ON vm_instance.domain_id = domain.id
            left join
        `cloud`.`guest_os` ON vm_instance.guest_os_id = guest_os.id
            left join
        `cloud`.`host_pod_ref` ON vm_instance.pod_id = host_pod_ref.id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`instance_group_vm_map` ON vm_instance.id = instance_group_vm_map.instance_id
            left join
        `cloud`.`instance_group` ON instance_group_vm_map.group_id = instance_group.id
            left join
        `cloud`.`data_center` ON vm_instance.data_center_id = data_center.id
            left join
        `cloud`.`host` ON vm_instance.host_id = host.id
            left join
	`cloud`.`vm_template` ON vm_instance.vm_template_id = vm_template.id
            left join
        `cloud`.`vm_template` iso ON iso.id = user_vm.iso_id
            left join
        `cloud`.`service_offering` ON vm_instance.service_offering_id = service_offering.id
            left join
        `cloud`.`disk_offering` svc_disk_offering ON vm_instance.service_offering_id = svc_disk_offering.id
            left join
        `cloud`.`disk_offering` ON vm_instance.disk_offering_id = disk_offering.id
            left join
        `cloud`.`volumes` ON vm_instance.id = volumes.instance_id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`security_group_vm_map` ON vm_instance.id = security_group_vm_map.instance_id
            left join
        `cloud`.`security_group` ON security_group_vm_map.security_group_id = security_group.id
            left join
        `cloud`.`nics` ON vm_instance.id = nics.instance_id and nics.removed is null
            left join
        `cloud`.`networks` ON nics.network_id = networks.id
            left join
        `cloud`.`vpc` ON networks.vpc_id = vpc.id and vpc.removed is null
            left join
        `cloud`.`user_ip_address` ON user_ip_address.vm_id = vm_instance.id
            left join
        `cloud`.`user_vm_details` as ssh_details ON ssh_details.vm_id = vm_instance.id
            and ssh_details.name = 'SSH.PublicKey'
            left join
        `cloud`.`ssh_keypairs` ON ssh_keypairs.public_key = ssh_details.value
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = vm_instance.id
            and resource_tags.resource_type = 'UserVm'
            left join
        `cloud`.`async_job` ON async_job.instance_id = vm_instance.id
            and async_job.instance_type = 'VirtualMachine'
            and async_job.job_status = 0
            left join
        `cloud`.`affinity_group_vm_map` ON vm_instance.id = affinity_group_vm_map.instance_id
            left join
        `cloud`.`affinity_group` ON affinity_group_vm_map.affinity_group_id = affinity_group.id
            left join
        `cloud`.`user_vm_details` as all_details ON all_details.vm_id = vm_instance.id
            left join
        `cloud`.`user_vm_details` `custom_cpu`  ON (((`custom_cpu`.`vm_id` = `cloud`.`vm_instance`.`id`) and (`custom_cpu`.`name` = 'CpuNumber')))
            left join
        `cloud`.`user_vm_details` `custom_speed`  ON (((`custom_speed`.`vm_id` = `cloud`.`vm_instance`.`id`) and (`custom_speed`.`name` = 'CpuSpeed')))
           left join
        `cloud`.`user_vm_details` `custom_ram_size`  ON (((`custom_ram_size`.`vm_id` = `cloud`.`vm_instance`.`id`) and (`custom_ram_size`.`name` = 'memory')));

-- ACL DB schema        
CREATE TABLE `cloud`.`acl_group` (
  `id` bigint unsigned NOT NULL UNIQUE auto_increment,
  `name` varchar(255) NOT NULL,
  `description` varchar(255) default NULL,
  `uuid` varchar(40),
  `path` varchar(255) NOT NULL,  
  `account_id` bigint unsigned NOT NULL,
  `view` varchar(40) default 'User' COMMENT 'response review this group account should see for result',
  `removed` datetime COMMENT 'date the group was removed',
  `created` datetime COMMENT 'date the group was created',
  PRIMARY KEY  (`id`),
  INDEX `i_acl_group__removed`(`removed`),
  CONSTRAINT `uc_acl_group__uuid` UNIQUE (`uuid`)  
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`acl_group_account_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `group_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `removed` datetime COMMENT 'date the account was removed from the group',
  `created` datetime COMMENT 'date the account was assigned to the group',  
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_acl_group_vm_map__group_id` FOREIGN KEY(`group_id`) REFERENCES `acl_group` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_acl_group_vm_map__account_id` FOREIGN KEY(`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;        


CREATE TABLE `acl_policy` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `uuid` varchar(40) DEFAULT NULL,
  `path` varchar(255) NOT NULL,
  `account_id` bigint unsigned NOT NULL,  
  `removed` datetime DEFAULT NULL COMMENT 'date the role was removed',
  `created` datetime DEFAULT NULL COMMENT 'date the role was created',
  `policy_type` varchar(64) DEFAULT 'Static' COMMENT 'Static or Dynamic',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uc_acl_policy__uuid` (`uuid`),
  KEY `i_acl_policy__removed` (`removed`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `acl_group_policy_map` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `group_id` bigint(20) unsigned NOT NULL,
  `policy_id` bigint(20) unsigned NOT NULL,
  `removed` datetime DEFAULT NULL COMMENT 'date the policy was revoked from the group',
  `created` datetime DEFAULT NULL COMMENT 'date the policy was attached to the group',
  PRIMARY KEY (`id`),
  KEY `fk_acl_group_policy_map__group_id` (`group_id`),
  KEY `fk_acl_group_policy_map__policy_id` (`policy_id`),
  CONSTRAINT `fk_acl_group_policy_map__group_id` FOREIGN KEY (`group_id`) REFERENCES `acl_group` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_acl_group_policy_map__policy_id` FOREIGN KEY (`policy_id`) REFERENCES `acl_policy` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `acl_policy_permission` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `policy_id` bigint(20) unsigned NOT NULL,
  `action` varchar(100) NOT NULL,
  `resource_type` varchar(100) DEFAULT NULL,
  `scope_id` bigint(20) DEFAULT NULL,
  `scope` varchar(40) DEFAULT NULL,
  `access_type` varchar(40) DEFAULT NULL,
  `permission`  varchar(40) NOT NULL COMMENT 'Allow or Deny',
  `removed` datetime DEFAULT NULL COMMENT 'date the permission was revoked',
  `created` datetime DEFAULT NULL COMMENT 'date the permission was granted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fk_acl_policy_permission__policy_id` (`policy_id`),
  CONSTRAINT `fk_acl_policy_permission__policy_id` FOREIGN KEY (`policy_id`) REFERENCES `acl_policy` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


INSERT IGNORE INTO `cloud`.`acl_policy` (id, name, description, uuid, path, account_id, created, policy_type) VALUES (1, 'NORMAL', 'Domain user role', UUID(), '/', 1, Now(), 'Static');
INSERT IGNORE INTO `cloud`.`acl_policy` (id, name, description, uuid, path, account_id, created, policy_type) VALUES (2, 'ADMIN', 'Root admin role', UUID(), '/', 1, Now(), 'Static');
INSERT IGNORE INTO `cloud`.`acl_policy` (id, name, description, uuid, path, account_id, created, policy_type) VALUES (3, 'DOMAIN_ADMIN', 'Domain admin role', UUID(), '/', 1, Now(), 'Static');
INSERT IGNORE INTO `cloud`.`acl_policy` (id, name, description, uuid, path, account_id, created, policy_type) VALUES (4, 'RESOURCE_DOMAIN_ADMIN', 'Resource domain admin role', UUID(), '/', 1, Now(), 'Static');
INSERT IGNORE INTO `cloud`.`acl_policy` (id, name, description, uuid, path, account_id, created, policy_type) VALUES (5, 'READ_ONLY_ADMIN', 'Read only admin role', UUID(), '/', 1, Now(), 'Static');
INSERT IGNORE INTO `cloud`.`acl_policy` (id, name, description, uuid, path, account_id, created, policy_type) VALUES (6, 'RESOURCE_OWNER', 'Resource owner role', UUID(), '/', 1, Now(), 'Dynamic');


INSERT IGNORE INTO `cloud`.`acl_group` (id, name, description, uuid, path, account_id, created) VALUES (1, 'NORMAL', 'Domain user group', UUID(), '/', 1, Now());
INSERT IGNORE INTO `cloud`.`acl_group` (id, name, description, uuid, path, account_id, created) VALUES (2, 'ADMIN', 'Root admin group', UUID(), '/', 1, Now());
INSERT IGNORE INTO `cloud`.`acl_group` (id, name, description, uuid, path, account_id, created) VALUES (3, 'DOMAIN_ADMIN', 'Domain admin group', UUID(), '/', 1, Now());
INSERT IGNORE INTO `cloud`.`acl_group` (id, name, description, uuid, path, account_id, created) VALUES (4, 'RESOURCE_DOMAIN_ADMIN', 'Resource domain admin group', UUID(), '/', 1, Now());
INSERT IGNORE INTO `cloud`.`acl_group` (id, name, description, uuid, path, account_id, created) VALUES (5, 'READ_ONLY_ADMIN', 'Read only admin group', UUID(), '/', 1, Now());

INSERT INTO `cloud`.`acl_group_policy_map` (group_id, policy_id, created) values(1, 1, Now());
INSERT INTO `cloud`.`acl_group_policy_map` (group_id, policy_id, created) values(2, 2, Now());
INSERT INTO `cloud`.`acl_group_policy_map` (group_id, policy_id, created) values(3, 3, Now());
INSERT INTO `cloud`.`acl_group_policy_map` (group_id, policy_id, created) values(4, 4, Now());
INSERT INTO `cloud`.`acl_group_policy_map` (group_id, policy_id, created) values(5, 5, Now());

INSERT IGNORE INTO `cloud`.`acl_policy_permission` (id, policy_id, action, permission, created) VALUES (1, 2, 'SystemCapability', 'Allow', Now());
INSERT IGNORE INTO `cloud`.`acl_policy_permission` (id, policy_id, action, permission, created) VALUES (2, 3, 'DomainCapability', 'Allow', Now());
INSERT IGNORE INTO `cloud`.`acl_policy_permission` (id, policy_id, action, permission, created) VALUES (3, 4, 'DomainResourceCapability', 'Allow', Now());

