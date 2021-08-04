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
-- Schema upgrade from 4.1.0 to 4.2.0;
--;

-- Disable foreign key checking
SET foreign_key_checks = 0;

-- All new inserts to the hypervisor_capabilities table should be after this
ALTER TABLE `cloud`.`hypervisor_capabilities` ADD CONSTRAINT `uc_hypervisor` UNIQUE (`hypervisor_type`, `hypervisor_version`);

ALTER TABLE `cloud`.`hypervisor_capabilities` ADD COLUMN `max_hosts_per_cluster` int unsigned DEFAULT NULL COMMENT 'Max. hosts in cluster supported by hypervisor';
ALTER TABLE `cloud`.`hypervisor_capabilities` ADD COLUMN `storage_motion_supported` int(1) unsigned DEFAULT 0 COMMENT 'Is storage motion supported';
ALTER TABLE volumes ADD COLUMN vm_snapshot_chain_size bigint(20) unsigned;
ALTER TABLE volumes ADD COLUMN iso_id bigint(20)  unsigned;

UPDATE `cloud`.`hypervisor_capabilities` SET `max_hosts_per_cluster`=32 WHERE `hypervisor_type`='VMware';
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, storage_motion_supported) VALUES (UUID(), 'XenServer', '6.1.0', 50, 1, 13, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, storage_motion_supported) VALUES (UUID(), 'XenServer', '6.2.0', 50, 1, 13, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_hosts_per_cluster) VALUES (UUID(), 'VMware', '5.1', 128, 0, 32);
UPDATE `cloud`.`hypervisor_capabilities` SET `storage_motion_supported`=true WHERE `hypervisor_type`='VMware' AND `hypervisor_version`='5.1';
UPDATE `cloud`.`hypervisor_capabilities` SET `storage_motion_supported`=true WHERE `hypervisor_type`='VMware' AND `hypervisor_version`='5.0';
UPDATE `cloud`.`hypervisor_capabilities` SET `storage_motion_supported`=true WHERE `hypervisor_type`='XenServer' AND `hypervisor_version`='6.1.0';
UPDATE `cloud`.`hypervisor_capabilities` SET `storage_motion_supported`=true WHERE `hypervisor_type`='XenServer' AND `hypervisor_version`='6.2.0';
DELETE FROM `cloud`.`configuration` where name='vmware.percluster.host.max';
DELETE FROM `cloud`.`configuration` where name='router.template.id';
DELETE FROM `cloud`.`configuration` where name='swift.enable';
DELETE FROM `cloud`.`configuration` where name='s3.enable';
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'xen.nics.max', '7', 'Maximum allowed nics for Vms created on Xen');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'vmware.use.dvswitch', 'false', 'Enable/Disable Nexus/Vmware dvSwitch in VMware environment');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'vmware.ports.per.dvportgroup', '256', 'Default number of ports per Vmware dvPortGroup in VMware environment');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'midonet.apiserver.address', 'http://localhost:8081', 'Specify the address at which the Midonet API server can be contacted (if using Midonet)');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'midonet.providerrouter.id', 'd7c5e6a3-e2f4-426b-b728-b7ce6a0448e5', 'Specifies the UUID of the Midonet provider router (if using Midonet)');
ALTER TABLE `cloud`.`load_balancer_vm_map` ADD state VARCHAR(40) NULL COMMENT 'service status updated by LB healthcheck manager';

ALTER TABLE `cloud`.`vm_template` ADD COLUMN `dynamically_scalable` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT 'true if template contains XS/VMWare tools inorder to support dynamic scaling of VM cpu/memory';
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `dynamically_scalable` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT 'true if VM contains XS/VMWare tools inorder to support dynamic scaling of VM cpu/memory';
UPDATE `cloud`.`vm_template` SET dynamically_scalable = 1 WHERE name = "CentOS 5.6(64-bit) no GUI (XenServer)" AND type = "BUILTIN";
UPDATE `cloud`.`vm_template` SET dynamically_scalable = 1 WHERE name = "SystemVM Template (vSphere)" AND type = "SYSTEM";

alter table storage_pool add hypervisor varchar(32);
alter table storage_pool change storage_provider_id storage_provider_name varchar(255);
alter table storage_pool change available_bytes used_bytes bigint unsigned;
-- alter table template_host_ref add state varchar(255);
-- alter table template_host_ref add update_count bigint unsigned;
-- alter table template_host_ref add updated datetime;
-- alter table volume_host_ref add state varchar(255);
-- alter table volume_host_ref add update_count bigint unsigned;
-- alter table volume_host_ref add updated datetime;
alter table template_spool_ref add updated datetime;
UPDATE `cloud`.`template_spool_ref` set state='Ready' WHERE download_state = 'DOWNLOADED';
UPDATE `cloud`.`template_spool_ref` set update_count=0;


CREATE TABLE  `cloud`.`object_datastore_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `datastore_uuid`  varchar(255) NOT NULL,
  `datastore_role` varchar(255) NOT NULL,
  `object_uuid` varchar(255) NOT NULL,
  `object_type` varchar(255) NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `job_id` varchar(255),
  `download_pct` int(10) unsigned,
  `download_state` varchar(255),
  `url` varchar(255),
  `format` varchar(255),
  `checksum` varchar(255),
  `error_str` varchar(255),
  `local_path` varchar(255),
  `install_path` varchar(255),
  `size` bigint unsigned COMMENT 'the size of the template on the pool',
  `state` varchar(255) NOT NULL,
  `update_count` bigint unsigned NOT NULL,
  `updated` DATETIME,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- CREATE TABLE `cloud`.`data_store_provider` (
--  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
--  `name` varchar(255) NOT NULL COMMENT 'name of primary data store provider',
--  `uuid` varchar(255) NOT NULL COMMENT 'uuid of primary data store provider',
--  PRIMARY KEY(`id`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 's3.rrs.enabled', 'false', 'enable s3 reduced redundancy storage');

CREATE TABLE `cloud`.`image_store` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of data store',
  `image_provider_name` varchar(255) NOT NULL COMMENT 'id of image_store_provider',
  `protocol` varchar(255) NOT NULL COMMENT 'protocol of data store',
  `url` varchar(255) COMMENT 'url for image data store',
  `data_center_id` bigint unsigned  COMMENT 'datacenter id of data store',
  `scope` varchar(255) COMMENT 'scope of data store',
  `role` varchar(255) COMMENT 'role of data store',
  `uuid` varchar(255) COMMENT 'uuid of data store',
  `parent` varchar(255) COMMENT 'parent path for the storage server',
  `created` datetime COMMENT 'date the image store first signed on',
  `removed` datetime COMMENT 'date removed if not null',  
  `total_size` bigint unsigned COMMENT 'storage total size statistics',
  `used_bytes` bigint unsigned COMMENT 'storage available bytes statistics',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`image_store_details` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `store_id` bigint unsigned NOT NULL COMMENT 'store the detail is related to',
  `name` varchar(255) NOT NULL COMMENT 'name of the detail',
  `value` varchar(255) NOT NULL COMMENT 'value of the detail',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_image_store_details__store_id` FOREIGN KEY `fk_image_store__store_id`(`store_id`) REFERENCES `image_store`(`id`) ON DELETE CASCADE,
  INDEX `i_image_store__name__value`(`name`(128), `value`(128))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP VIEW IF EXISTS `cloud`.`image_store_view`;
CREATE VIEW `cloud`.`image_store_view` AS
    select 
        image_store.id,
        image_store.uuid,
        image_store.name,
        image_store.image_provider_name,
        image_store.protocol,
        image_store.url,
        image_store.scope,
        image_store.role,
        image_store.removed,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        image_store_details.name detail_name,
        image_store_details.value detail_value
    from
        `cloud`.`image_store`
            left join
        `cloud`.`data_center` ON image_store.data_center_id = data_center.id
            left join
        `cloud`.`image_store_details` ON image_store_details.store_id = image_store.id;

            
-- here we have to allow null for store_id to accomodate baremetal case to search for ready templates since template state is only stored in this table
-- FK also commented out due to this            
CREATE TABLE  `cloud`.`template_store_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `store_id` bigint unsigned,
  `template_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `job_id` varchar(255),
  `download_pct` int(10) unsigned,
  `size` bigint unsigned,
  `store_role` varchar(255),  
  `physical_size` bigint unsigned DEFAULT 0,
  `download_state` varchar(255),
  `error_str` varchar(255),
  `local_path` varchar(255),
  `install_path` varchar(255),
  `url` varchar(255),
  `state` varchar(255) NOT NULL,
  `destroyed` tinyint(1) COMMENT 'indicates whether the template_store entry was destroyed by the user or not',
  `is_copy` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'indicates whether this was copied ',
  `update_count` bigint unsigned,
  `ref_cnt` bigint unsigned DEFAULT 0,
  `updated` datetime, 
  PRIMARY KEY  (`id`),
--  CONSTRAINT `fk_template_store_ref__store_id` FOREIGN KEY `fk_template_store_ref__store_id` (`store_id`) REFERENCES `image_store` (`id`) ON DELETE CASCADE,
  INDEX `i_template_store_ref__store_id`(`store_id`),
  CONSTRAINT `fk_template_store_ref__template_id` FOREIGN KEY `fk_template_store_ref__template_id` (`template_id`) REFERENCES `vm_template` (`id`),
  INDEX `i_template_store_ref__template_id`(`template_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- ALTER TABLE `cloud`.`vm_template` ADD COLUMN `image_data_store_id` bigint unsigned;

-- Do we still need these columns? TODO, to delete them, remove FK constraints from snapshots table
-- ALTER TABLE `cloud`.`snapshots` DROP COLUMN `swift_id`;
-- ALTER TABLE `cloud`.`snapshots` DROP COLUMN `s3_id`;
-- ALTER TABLE `cloud`.`snapshots` DROP COLUMN `sechost_id`;

-- change upload host_id FK to point to image_store table
ALTER TABLE `cloud`.`upload` DROP FOREIGN KEY `fk_upload__host_id`; 
ALTER TABLE `cloud`.`upload` ADD CONSTRAINT `fk_upload__store_id` FOREIGN KEY(`host_id`) REFERENCES `image_store` (`id`) ON DELETE CASCADE;

CREATE TABLE  `cloud`.`snapshot_store_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `store_id` bigint unsigned NOT NULL,
  `snapshot_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `job_id` varchar(255),
  `store_role` varchar(255),
  `size` bigint unsigned,
  `physical_size` bigint unsigned DEFAULT 0,
  `parent_snapshot_id` bigint unsigned DEFAULT 0,
  `install_path` varchar(255),
  `state` varchar(255) NOT NULL,  
  -- `removed` datetime COMMENT 'date removed if not null',  
  `update_count` bigint unsigned,
  `ref_cnt` bigint unsigned,
  `updated` datetime,   
  `volume_id` bigint unsigned,
  PRIMARY KEY  (`id`),
  INDEX `i_snapshot_store_ref__store_id`(`store_id`),
  CONSTRAINT `fk_snapshot_store_ref__snapshot_id` FOREIGN KEY `fk_snapshot_store_ref__snapshot_id` (`snapshot_id`) REFERENCES `snapshots` (`id`),
  INDEX `i_snapshot_store_ref__snapshot_id`(`snapshot_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`volume_store_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `store_id` bigint unsigned NOT NULL,
  `volume_id` bigint unsigned NOT NULL,
  `zone_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `job_id` varchar(255),
  `download_pct` int(10) unsigned,
  `size` bigint unsigned,
  `physical_size` bigint unsigned DEFAULT 0,
  `download_state` varchar(255),
  `checksum` varchar(255) COMMENT 'checksum for the data disk',
  `error_str` varchar(255),
  `local_path` varchar(255),
  `install_path` varchar(255),
  `url` varchar(255),
  `download_url` varchar(255),
  `state` varchar(255) NOT NULL,  
  `destroyed` tinyint(1) COMMENT 'indicates whether the volume_host entry was destroyed by the user or not',
  `update_count` bigint unsigned,
  `ref_cnt` bigint unsigned,
  `updated` datetime,   
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_volume_store_ref__store_id` FOREIGN KEY `fk_volume_store_ref__store_id` (`store_id`) REFERENCES `image_store` (`id`) ON DELETE CASCADE,
  INDEX `i_volume_store_ref__store_id`(`store_id`),
  CONSTRAINT `fk_volume_store_ref__volume_id` FOREIGN KEY `fk_volume_store_ref__volume_id` (`volume_id`) REFERENCES `volumes` (`id`),
  INDEX `i_volume_store_ref__volume_id`(`volume_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`service_offering` ADD COLUMN `is_volatile` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT 'true if the vm needs to be volatile, i.e., on every reboot of vm from API root disk is discarded and creates a new root disk';

ALTER TABLE `cloud`.`networks` ADD COLUMN `network_cidr` VARCHAR(18) COMMENT 'The network cidr for the isolated guest network which uses IP Reservation facility.For networks not using IP reservation, network_cidr is always null.';
ALTER TABLE `cloud`.`networks` CHANGE `cidr` `cidr` varchar(18) COMMENT 'CloudStack managed vms get IP address from cidr.In general this cidr also serves as the network CIDR. But in case IP reservation feature is being used by a Guest network, networkcidr is the Effective network CIDR for that network';


CREATE TABLE  `vpc_service_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vpc_id` bigint unsigned NOT NULL COMMENT 'vpc_id',
  `service` varchar(255) NOT NULL COMMENT 'service',
  `provider` varchar(255) COMMENT 'service provider',
  `created` datetime COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vpc_service_map__vpc_id` FOREIGN KEY(`vpc_id`) REFERENCES `vpc`(`id`) ON DELETE CASCADE,
  UNIQUE (`vpc_id`, `service`, `provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`load_balancer_healthcheck_policies` (
 `id` bigint(20) NOT NULL auto_increment,
  `uuid` varchar(40),
  `load_balancer_id` bigint unsigned NOT NULL,
  `pingpath` varchar(225) NULL DEFAULT '/',
  `description` varchar(4096)  NULL,
  `response_time` int(11) DEFAULT 5,
  `healthcheck_interval` int(11) DEFAULT 5,
  `healthcheck_thresshold` int(11) DEFAULT 2,
  `unhealth_thresshold` int(11) DEFAULT 10,
  `revoke` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 is when rule is set for Revoke',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  CONSTRAINT `fk_load_balancer_healthcheck_policies_loadbalancer_id` FOREIGN KEY(`load_balancer_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.instancename.flag', 'false', 'If set to true, will set guest VM\'s name as it appears on the hypervisor, to its hostname');

UPDATE `cloud`.`guest_os` SET category_id=10 where id=59;
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (165, UUID(), 6, 'Windows 8 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (166, UUID(), 6, 'Windows 8 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (167, UUID(), 6, 'Windows Server 2012 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (168, UUID(), 6, 'Windows Server 8 (64-bit)');

# clean up row added in 3.0.6.
UPDATE `cloud`.`guest_os_hypervisor` set guest_os_id = 166 where guest_os_id = 206;
UPDATE `cloud`.`vm_template` set guest_os_id = 166 where guest_os_id = 206;
UPDATE `cloud`.`vm_instance` set guest_os_id = 166 where guest_os_id = 206;
DELETE IGNORE FROM `cloud`.`guest_os` where id=206;

INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (141, UUID(), 1, 'CentOS 5.6 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (142, UUID(), 1, 'CentOS 5.6 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (169, UUID(), 10, 'Ubuntu 11.04 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (170, UUID(), 10, 'Ubuntu 11.04 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (171, UUID(), 1, 'CentOS 6.3 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (172, UUID(), 1, 'CentOS 6.3 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (173, UUID(), 1, 'CentOS 5.8 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (174, UUID(), 1, 'CentOS 5.8 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (175, UUID(), 1, 'CentOS 5.9 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (176, UUID(), 1, 'CentOS 5.9 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (177, UUID(), 1, 'CentOS 6.1 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (178, UUID(), 1, 'CentOS 6.1 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (179, UUID(), 1, 'CentOS 6.2 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (180, UUID(), 1, 'CentOS 6.2 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (181, UUID(), 1, 'CentOS 6.4 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (182, UUID(), 1, 'CentOS 6.4 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (183, UUID(), 2, 'Debian GNU/Linux 7(32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (184, UUID(), 2, 'Debian GNU/Linux 7(64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (185, UUID(), 5, 'SUSE Linux Enterprise Server 11 SP2 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (186, UUID(), 5, 'SUSE Linux Enterprise Server 11 SP2 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (187, UUID(), 5, 'SUSE Linux Enterprise Server 11 SP3 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (188, UUID(), 5, 'SUSE Linux Enterprise Server 11 SP3 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (189, UUID(), 4, 'Red Hat Enterprise Linux 5.7 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (190, UUID(), 4, 'Red Hat Enterprise Linux 5.7 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (191, UUID(), 4, 'Red Hat Enterprise Linux 5.8 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (192, UUID(), 4, 'Red Hat Enterprise Linux 5.8 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (193, UUID(), 4, 'Red Hat Enterprise Linux 5.9 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (194, UUID(), 4, 'Red Hat Enterprise Linux 5.9 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (195, UUID(), 4, 'Red Hat Enterprise Linux 6.1 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (196, UUID(), 4, 'Red Hat Enterprise Linux 6.1 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (197, UUID(), 4, 'Red Hat Enterprise Linux 6.2 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (198, UUID(), 4, 'Red Hat Enterprise Linux 6.2 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (199, UUID(), 4, 'Red Hat Enterprise Linux 6.3 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (204, UUID(), 4, 'Red Hat Enterprise Linux 6.3 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (205, UUID(), 4, 'Red Hat Enterprise Linux 6.4 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (206, UUID(), 4, 'Red Hat Enterprise Linux 6.4 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (207, UUID(), 3, 'Oracle Enterprise Linux 5.7 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (208, UUID(), 3, 'Oracle Enterprise Linux 5.7 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (209, UUID(), 3, 'Oracle Enterprise Linux 5.8 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (210, UUID(), 3, 'Oracle Enterprise Linux 5.8 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (211, UUID(), 3, 'Oracle Enterprise Linux 5.9 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (212, UUID(), 3, 'Oracle Enterprise Linux 5.9 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (213, UUID(), 3, 'Oracle Enterprise Linux 6.1 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (214, UUID(), 3, 'Oracle Enterprise Linux 6.1 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (215, UUID(), 3, 'Oracle Enterprise Linux 6.2 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (216, UUID(), 3, 'Oracle Enterprise Linux 6.2 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (217, UUID(), 3, 'Oracle Enterprise Linux 6.3 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (218, UUID(), 3, 'Oracle Enterprise Linux 6.3 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (219, UUID(), 3, 'Oracle Enterprise Linux 6.4 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (220, UUID(), 3, 'Oracle Enterprise Linux 6.4 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (221, UUID(), 7, 'Apple Mac OS X 10.6 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (222, UUID(), 7, 'Apple Mac OS X 10.6 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (223, UUID(), 7, 'Apple Mac OS X 10.7 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (224, UUID(), 7, 'Apple Mac OS X 10.7 (64-bit)');


INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Windows 8 (32-bit)', 165);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Windows 8 (64-bit)', 166);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Windows Server 2012 (64-bit)', 167);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Windows Server 8 (64-bit)', 168);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'Windows 8 (32-bit)', 165);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'Windows 8 (64-bit)', 166);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'Windows Server 2012 (64-bit)', 167);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'Windows Server 8 (64-bit)', 168);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 5.5 (32-bit)', 111);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 5.5 (64-bit)', 112);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 5.6 (32-bit)', 141);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 5.6 (64-bit)', 142);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 5.7 (32-bit)', 161);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 5.7 (64-bit)', 162);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 5.8 (32-bit)', 173);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 5.8 (64-bit)', 174);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 5.9 (32-bit)', 175);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 5.9 (64-bit)', 176);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 6.0 (32-bit)', 143);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 6.0 (64-bit)', 144);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 6.1 (32-bit)', 177);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 6.1 (64-bit)', 178);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 6.2 (32-bit)', 179);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 6.2 (64-bit)', 180);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 6.3 (32-bit)', 171);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 6.3 (64-bit)', 172);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 6.4 (32-bit)', 181);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'CentOS 6.4 (64-bit)', 182);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'Debian GNU/Linux 7(32-bit)', 183);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'Debian GNU/Linux 7(64-bit)', 184);

INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Apple Mac OS X 10.6 (32-bit)', 221);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Apple Mac OS X 10.6 (64-bit)', 222);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Apple Mac OS X 10.7 (32-bit)', 223);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Apple Mac OS X 10.7 (64-bit)', 224);

CREATE TABLE `cloud`.`user_vm_clone_setting` (
  `vm_id` bigint unsigned NOT NULL COMMENT 'guest VM id',
  `clone_type` varchar(10) NOT NULL COMMENT 'Full or Linked Clone (applicable to VMs on ESX)',
  PRIMARY KEY (`vm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`affinity_group` (
  `id` bigint unsigned NOT NULL auto_increment,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `uuid` varchar(40),
  `description` varchar(4096) NULL,
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `acl_type` varchar(15) NOT NULL COMMENT 'ACL access type. can be Account/Domain',
  UNIQUE (`name`, `account_id`),
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_affinity_group__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`),
  CONSTRAINT `fk_affinity_group__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`),
  CONSTRAINT `uc_affinity_group__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`affinity_group_vm_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `affinity_group_id` bigint unsigned NOT NULL,
  `instance_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_agvm__group_id` FOREIGN KEY(`affinity_group_id`) REFERENCES `affinity_group`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_affinity_group_vm_map___instance_id` FOREIGN KEY(`instance_id`) REFERENCES `user_vm` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`affinity_group_domain_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain id',
  `affinity_group_id` bigint unsigned NOT NULL COMMENT 'affinity group id',
  `subdomain_access` int(1) unsigned DEFAULT 1 COMMENT '1 if affinity group can be accessible from the subdomain',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_affinity_group_domain_map__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_affinity_group_domain_map__affinity_group_id` FOREIGN KEY (`affinity_group_id`) REFERENCES `affinity_group`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`dedicated_resources` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `data_center_id` bigint unsigned COMMENT 'data center id',
  `pod_id` bigint unsigned COMMENT 'pod id',
  `cluster_id` bigint unsigned COMMENT 'cluster id',
  `host_id` bigint unsigned COMMENT 'host id',
  `domain_id` bigint unsigned COMMENT 'domain id of the domain to which resource is dedicated',
  `account_id` bigint unsigned COMMENT 'account id of the account to which resource is dedicated',
  `affinity_group_id` bigint unsigned NOT NULL COMMENT 'affinity group id associated',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_dedicated_resources__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `cloud`.`data_center`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_dedicated_resources__pod_id` FOREIGN KEY (`pod_id`) REFERENCES `cloud`.`host_pod_ref`(`id`),
  CONSTRAINT `fk_dedicated_resources__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cloud`.`cluster`(`id`),
  CONSTRAINT `fk_dedicated_resources__host_id` FOREIGN KEY (`host_id`) REFERENCES `cloud`.`host`(`id`),
  CONSTRAINT `fk_dedicated_resources__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`),
  CONSTRAINT `fk_dedicated_resources__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`),
  CONSTRAINT `fk_dedicated_resources__affinity_group_id` FOREIGN KEY (`affinity_group_id`) REFERENCES `affinity_group`(`id`) ON DELETE CASCADE,
  INDEX `i_dedicated_resources_domain_id`(`domain_id`),
  INDEX `i_dedicated_resources_account_id`(`account_id`),
  INDEX `i_dedicated_resources_affinity_group_id`(`affinity_group_id`),
  CONSTRAINT `uc_dedicated_resources__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE nic_secondary_ips (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid` varchar(40),
  `vmId` bigint unsigned COMMENT 'vm instance id',
  `nicId` bigint unsigned NOT NULL,
  `ip4_address` char(40) COMMENT 'ip4 address',
  `ip6_address` char(40) COMMENT 'ip6 address',
  `network_id` bigint unsigned NOT NULL COMMENT 'network configuration id',
  `created` datetime NOT NULL COMMENT 'date created',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner.  foreign key to   account table',
  `domain_id` bigint unsigned NOT NULL COMMENT 'the domain that the owner belongs to',
   PRIMARY KEY (`id`),
   CONSTRAINT `fk_nic_secondary_ip__vmId` FOREIGN KEY `fk_nic_secondary_ip__vmId`(`vmId`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE,
   CONSTRAINT `fk_nic_secondary_ip__networks_id` FOREIGN KEY `fk_nic_secondary_ip__networks_id`(`network_id`) REFERENCES `networks`(`id`),
   CONSTRAINT `uc_nic_secondary_ip__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`nics` ADD COLUMN secondary_ip SMALLINT DEFAULT '0' COMMENT 'secondary ips configured for the nic';
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN dnat_vmip VARCHAR(40);
UPDATE `cloud`.`user_ip_address`,`cloud`.`nics` SET `user_ip_address`.`dnat_vmip` = `nics`.`ip4_address`
      WHERE `user_ip_address`.`vm_id` = `nics`.`instance_id` AND `user_ip_address`.`network_id` = `nics`.`network_id` AND `user_ip_address`.`one_to_one_nat` = 1;

ALTER TABLE `cloud`.`alert` ADD COLUMN `archived` tinyint(1) unsigned NOT NULL DEFAULT 0;
ALTER TABLE `cloud`.`event` ADD COLUMN `archived` tinyint(1) unsigned NOT NULL DEFAULT 0;
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'alert.purge.interval', '86400', 'The interval (in seconds) to wait before running the alert purge thread');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'alert.purge.delay', '0', 'Alerts older than specified number days will be purged. Set this value to 0 to never delete alerts');

DROP VIEW IF EXISTS `cloud`.`event_view`;
CREATE VIEW `cloud`.`event_view` AS
    select
        event.id,
        event.uuid,
        event.type,
        event.state,
        event.description,
        event.created,
        event.level,
        event.parameters,
        event.start_id,
        eve.uuid start_uuid,
        event.user_id,
        event.archived,
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
    from
        `cloud`.`event`
            inner join
        `cloud`.`account` ON event.account_id = account.id
            inner join
        `cloud`.`domain` ON event.domain_id = domain.id
            inner join
        `cloud`.`user` ON event.user_id = user.id
            left join
        `cloud`.`projects` ON projects.project_account_id = event.account_id
            left join
        `cloud`.`event` eve ON event.start_id = eve.id;

ALTER TABLE `cloud`.`region` ADD COLUMN `portableip_service_enabled` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is Portable IP service enalbed in the Region';

ALTER TABLE `cloud`.`region` ADD COLUMN `gslb_service_enabled` tinyint(1) unsigned NOT NULL DEFAULT 1 COMMENT 'Is GSLB service enalbed in the Region';

ALTER TABLE `cloud`.`external_load_balancer_devices` ADD COLUMN `is_gslb_provider` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if load balancer appliance is acting as gslb service provider in the zone';

ALTER TABLE `cloud`.`external_load_balancer_devices` ADD COLUMN `gslb_site_publicip` varchar(255)  DEFAULT NULL COMMENT 'GSLB service Provider site public ip';

ALTER TABLE `cloud`.`external_load_balancer_devices` ADD COLUMN `gslb_site_privateip` varchar(255) DEFAULT NULL COMMENT 'GSLB service Provider site private ip';

ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `display_vm` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should vm instance be displayed to the end user';

ALTER TABLE `cloud`.`user_vm_details` ADD COLUMN `display_detail` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should vm detail instance be displayed to the end user';

ALTER TABLE `cloud`.`volumes` ADD COLUMN `display_volume` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should volume be displayed to the end user';

ALTER TABLE `cloud`.`volumes` ADD COLUMN `format` varchar(255) COMMENT 'volume format';
update  `cloud`.`volumes` v,  `cloud`.`storage_pool` s,  `cloud`.`cluster` c  set v.format='VHD' where v.pool_id=s.id and s.cluster_id=c.id and c.hypervisor_type='XenServer';
update  `cloud`.`volumes` v,  `cloud`.`storage_pool` s,  `cloud`.`cluster` c  set v.format='OVA' where v.pool_id=s.id and s.cluster_id=c.id and c.hypervisor_type='VMware';
update  `cloud`.`volumes` v,  `cloud`.`storage_pool` s,  `cloud`.`cluster` c  set v.format='QCOW2' where v.pool_id=s.id and s.cluster_id=c.id and c.hypervisor_type='KVM';
update  `cloud`.`volumes` v,  `cloud`.`storage_pool` s,  `cloud`.`cluster` c  set v.format='RAW' where v.pool_id=s.id and s.cluster_id=c.id and c.hypervisor_type='Ovm';

ALTER TABLE `cloud`.`networks` ADD COLUMN `display_network` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should network be displayed to the end user';

ALTER TABLE `cloud`.`nics` ADD COLUMN `display_nic` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should nic be displayed to the end user';

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `display_offering` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should disk offering be displayed to the end user';

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `customized_iops` tinyint(1) unsigned COMMENT 'Should customized IOPS be displayed to the end user';

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `min_iops` bigint(20) unsigned COMMENT 'Minimum IOPS';

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `max_iops` bigint(20) unsigned COMMENT 'Maximum IOPS';

ALTER TABLE `cloud`.`volumes` ADD COLUMN `min_iops` bigint(20) unsigned COMMENT 'Minimum IOPS';

ALTER TABLE `cloud`.`volumes` ADD COLUMN `max_iops` bigint(20) unsigned COMMENT 'Maximum IOPS';

ALTER TABLE `cloud`.`storage_pool` ADD COLUMN `managed` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Should CloudStack manage this storage';

ALTER TABLE `cloud`.`storage_pool` ADD COLUMN `capacity_iops` bigint(20) unsigned DEFAULT NULL COMMENT 'IOPS CloudStack can provision from this storage pool';

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `bytes_read_rate` bigint(20);

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `bytes_write_rate` bigint(20);

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `iops_read_rate` bigint(20);

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `iops_write_rate` bigint(20);

CREATE TABLE `cloud`.`volume_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `volume_id` bigint unsigned NOT NULL COMMENT 'volume id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display_detail` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should detail be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_volume_details__volume_id` FOREIGN KEY `fk_volume_details__volume_id`(`volume_id`) REFERENCES `volumes`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_id` bigint unsigned NOT NULL COMMENT 'network id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display_detail` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should detail be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_network_details__network_id` FOREIGN KEY `fk_network_details__network_id`(`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`nic_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `nic_id` bigint unsigned NOT NULL COMMENT 'nic id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display_detail` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should detail be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_nic_details__nic_id` FOREIGN KEY `fk_nic_details__nic_id`(`nic_id`) REFERENCES `nics`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`disk_offering_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `offering_id` bigint unsigned NOT NULL COMMENT 'offering id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display_detail` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should detail be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_offering_details__offering_id` FOREIGN KEY `fk_offering_details__offering_id`(`offering_id`) REFERENCES `disk_offering`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`global_load_balancing_rules` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `account_id` bigint unsigned NOT NULL COMMENT 'account id',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain id',
  `region_id`  int unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096) NULL COMMENT 'description',
  `state` char(32) NOT NULL COMMENT 'current state of this rule',
  `algorithm` varchar(255) NOT NULL COMMENT 'load balancing algorithm used to distribbute traffic across zones',
  `persistence` varchar(255) NOT NULL COMMENT 'session persistence used across the zone',
  `service_type` varchar(255) NOT NULL COMMENT 'GSLB service type (tcp/udp)',
  `gslb_domain_name` varchar(255) NOT NULL COMMENT 'DNS name for the GSLB service that is used to provide a FQDN for the GSLB service',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_global_load_balancing_rules_account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_global_load_balancing_rules_region_id` FOREIGN KEY(`region_id`) REFERENCES `region`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`global_load_balancer_lb_rule_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `gslb_rule_id` bigint unsigned NOT NULL,
  `lb_rule_id` bigint unsigned NOT NULL,
  `weight` bigint unsigned NOT NULL DEFAULT 1 COMMENT 'weight of the site in gslb',
  `revoke` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 is when rule is set for Revoke',
  PRIMARY KEY  (`id`),
  UNIQUE KEY (`gslb_rule_id`, `lb_rule_id`),
  CONSTRAINT `fk_gslb_rule_id` FOREIGN KEY(`gslb_rule_id`) REFERENCES `global_load_balancing_rules`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_lb_rule_id` FOREIGN KEY(`lb_rule_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'cloud.dns.name', null, 'DNS name of the cloud for the GSLB service');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.cpus', '40', 'The default maximum number of cpu cores that can be used for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.memory', '40960', 'The default maximum memory (in MiB) that can be used for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.primary.storage', '200', 'The default maximum primary storage space (in GiB) that can be used for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.secondary.storage', '400', 'The default maximum secondary storage space (in GiB) that can be used for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.cpus', '40', 'The default maximum number of cpu cores that can be used for a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.memory', '40960', 'The default maximum memory (in MiB) that can be used for a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.primary.storage', '200', 'The default maximum primary storage space (in GiB) that can be used for a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.secondary.storage', '400', 'The default maximum secondary storage space (in GiB) that can be used for a project');



ALTER TABLE `cloud`.`remote_access_vpn` ADD COLUMN `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id';
ALTER TABLE `cloud`.`remote_access_vpn` ADD COLUMN `uuid` varchar(40) UNIQUE;

-- START: support for LXC
 
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES (UUID(), 'LXC', 'default', 50, 1);
ALTER TABLE `cloud`.`physical_network_traffic_types` ADD COLUMN `lxc_network_label` varchar(255) DEFAULT 'cloudbr0' COMMENT 'The network name label of the physical device dedicated to this traffic on a LXC host';
 
UPDATE configuration SET value='KVM,XenServer,VMware,BareMetal,Ovm,LXC' WHERE name='hypervisor.list';
 
INSERT INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id, featured, cross_zones, hypervisor_type)
     VALUES (10, UUID(), 'routing-10', 'SystemVM Template (LXC)', 0, now(), 'SYSTEM', 0, 64, 1, 'http://download.cloudstack.org/templates/acton/acton-systemvm-02062012.qcow2.bz2', '2755de1f9ef2ce4d6f2bee2efbb4da92', 0, 'SystemVM Template (LXC)', 'QCOW2', 15, 0, 1, 'LXC');

ALTER TABLE `cloud`.`user_vm` MODIFY user_data TEXT(32768);

-- END: support for LXC

CREATE TABLE `cloud`.`vm_snapshots` (
  `id` bigint(20) unsigned NOT NULL auto_increment COMMENT 'Primary Key',
  `uuid` varchar(40) NOT NULL,
  `name` varchar(255) NOT NULL,
  `display_name` varchar(255) default NULL,
  `description` varchar(255) default NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `vm_snapshot_type` varchar(32) default NULL,
  `state` varchar(32) NOT NULL,
  `parent` bigint unsigned default NULL,
  `current` int(1) unsigned default NULL,
  `update_count` bigint unsigned NOT NULL DEFAULT 0,
  `updated` datetime default NULL,
  `created` datetime default NULL,
  `removed` datetime default NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT UNIQUE KEY `uc_vm_snapshots_uuid` (`uuid`),
  INDEX `vm_snapshots_name` (`name`),
  INDEX `vm_snapshots_vm_id` (`vm_id`),
  INDEX `vm_snapshots_account_id` (`account_id`),
  INDEX `vm_snapshots_display_name` (`display_name`),
  INDEX `vm_snapshots_removed` (`removed`),
  INDEX `vm_snapshots_parent` (`parent`),
  CONSTRAINT `fk_vm_snapshots_vm_id__vm_instance_id` FOREIGN KEY `fk_vm_snapshots_vm_id__vm_instance_id` (`vm_id`) REFERENCES `vm_instance` (`id`),
  CONSTRAINT `fk_vm_snapshots_account_id__account_id` FOREIGN KEY `fk_vm_snapshots_account_id__account_id` (`account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_vm_snapshots_domain_id__domain_id` FOREIGN KEY `fk_vm_snapshots_domain_id__domain_id` (`domain_id`) REFERENCES `domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`hypervisor_capabilities` ADD COLUMN `vm_snapshot_enabled` tinyint(1) DEFAULT 0 NOT NULL COMMENT 'Whether VM snapshot is supported by hypervisor';
UPDATE `cloud`.`hypervisor_capabilities` SET `vm_snapshot_enabled`=1 WHERE `hypervisor_type` in ('VMware', 'XenServer');

CREATE TABLE `cloud`.`service_offering_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `service_offering_id` bigint unsigned NOT NULL COMMENT 'service offering id',
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_service_offering_details__service_offering_id` FOREIGN KEY (`service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE,
  CONSTRAINT UNIQUE KEY `uk_service_offering_id_name` (`service_offering_id`, `name`)
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
        guest_os.uuid guest_os_uuid,
        vm_instance.pod_id pod_id,
        host_pod_ref.uuid pod_uuid,
        vm_instance.private_ip_address private_ip_address,
        vm_instance.private_mac_address private_mac_address,
        vm_instance.vm_type vm_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_type,
        data_center.is_security_group_enabled security_group_enabled,
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
        disk_offering.uuid service_offering_uuid,
        service_offering.cpu cpu,
        service_offering.speed speed,
        service_offering.ram_size ram_size,
        disk_offering.name service_offering_name,
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
        affinity_group.description affinity_group_description
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
        `cloud`.`disk_offering` ON vm_instance.service_offering_id = disk_offering.id
            left join
        `cloud`.`volumes` ON vm_instance.id = volumes.instance_id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`security_group_vm_map` ON vm_instance.id = security_group_vm_map.instance_id
            left join
        `cloud`.`security_group` ON security_group_vm_map.security_group_id = security_group.id
            left join
        `cloud`.`nics` ON vm_instance.id = nics.instance_id
            left join
        `cloud`.`networks` ON nics.network_id = networks.id
            left join
        `cloud`.`vpc` ON networks.vpc_id = vpc.id
            left join
        `cloud`.`user_ip_address` ON user_ip_address.vm_id = vm_instance.id
            left join
        `cloud`.`user_vm_details` ON user_vm_details.vm_id = vm_instance.id
            and user_vm_details.name = 'SSH.PublicKey'
            left join
        `cloud`.`ssh_keypairs` ON ssh_keypairs.public_key = user_vm_details.value
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
        `cloud`.`affinity_group` ON affinity_group_vm_map.affinity_group_id = affinity_group.id;

DROP VIEW IF EXISTS `cloud`.`affinity_group_view`;
CREATE VIEW `cloud`.`affinity_group_view` AS
    select 
        affinity_group.id id,
        affinity_group.name name,
        affinity_group.type type,
        affinity_group.description description,
        affinity_group.uuid uuid,
		affinity_group.acl_type acl_type,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        vm_instance.id vm_id,
        vm_instance.uuid vm_uuid,
        vm_instance.name vm_name,
        vm_instance.state vm_state,
        user_vm.display_name vm_display_name
    from
        `cloud`.`affinity_group`
            inner join
        `cloud`.`account` ON affinity_group.account_id = account.id
            inner join
        `cloud`.`domain` ON affinity_group.domain_id = domain.id
            left join
        `cloud`.`affinity_group_vm_map` ON affinity_group.id = affinity_group_vm_map.affinity_group_id
            left join
        `cloud`.`vm_instance` ON vm_instance.id = affinity_group_vm_map.instance_id
            left join
        `cloud`.`user_vm` ON user_vm.id = vm_instance.id;

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
            
CREATE TABLE `cloud`.`external_cisco_vnmc_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which cisco vnmc device is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name corresponding to this cisco vnmc device',
  `device_name` varchar(255) NOT NULL COMMENT 'name of the cisco vnmc device',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id coresponding to the external cisco vnmc device',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_external_cisco_vnmc_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_cisco_vnmc_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`external_cisco_asa1000v_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which cisco asa1kv device is added',
  `management_ip` varchar(255) UNIQUE NOT NULL COMMENT 'mgmt. ip of cisco asa1kv device',
  `in_port_profile` varchar(255) NOT NULL COMMENT 'inside port profile name of cisco asa1kv device',
  `cluster_id` bigint unsigned NOT NULL COMMENT 'id of the Vmware cluster to which cisco asa1kv device is attached (cisco n1kv switch)',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_external_cisco_asa1000v_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_cisco_asa1000v_devices__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_asa1000v_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `network_id` bigint unsigned NOT NULL UNIQUE COMMENT 'id of guest network',
  `asa1000v_id` bigint unsigned NOT NULL UNIQUE COMMENT 'id of asa1000v device',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_network_asa1000v_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_network_asa1000v_map__asa1000v_id` FOREIGN KEY (`asa1000v_id`) REFERENCES `external_cisco_asa1000v_devices`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vmware_data_center` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `name` varchar(255) NOT NULL COMMENT 'Name of VMware datacenter',
  `guid` varchar(255) NOT NULL UNIQUE COMMENT 'id of VMware datacenter',
  `vcenter_host` varchar(255) NOT NULL COMMENT 'vCenter host containing this VMware datacenter',
  `username` varchar(255) NOT NULL COMMENT 'Name of vCenter host user',
  `password` varchar(255) NOT NULL COMMENT 'Password of vCenter host user',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vmware_data_center_zone_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `zone_id` bigint unsigned NOT NULL UNIQUE COMMENT 'id of CloudStack zone',
  `vmware_data_center_id` bigint unsigned NOT NULL UNIQUE COMMENT 'id of VMware datacenter',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vmware_data_center_zone_map__vmware_data_center_id` FOREIGN KEY (`vmware_data_center_id`) REFERENCES `vmware_data_center`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`legacy_zones` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `zone_id` bigint unsigned NOT NULL UNIQUE COMMENT 'id of CloudStack zone',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_legacy_zones__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `eip_associate_public_ip` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if public IP is associated with user VM creation by default when EIP service is enabled.' AFTER `elastic_ip_service`;


CREATE TABLE `cloud`.`op_host_planner_reservation` (
  `id` bigint unsigned NOT NULL auto_increment,
  `data_center_id` bigint unsigned NOT NULL,
  `pod_id` bigint unsigned,
  `cluster_id` bigint unsigned,
  `host_id` bigint unsigned,
  `resource_usage` varchar(255) COMMENT 'Shared(between planners) Vs Dedicated (exclusive usage to a planner)',
  PRIMARY KEY  (`id`),
  INDEX `i_op_host_planner_reservation__host_resource_usage`(`host_id`, `resource_usage`),
  CONSTRAINT `fk_planner_reservation__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_planner_reservation__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `cloud`.`data_center`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_planner_reservation__pod_id` FOREIGN KEY (`pod_id`) REFERENCES `cloud`.`host_pod_ref`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_planner_reservation__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cloud`.`cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`service_offering` ADD COLUMN `deployment_planner` varchar(255) COMMENT 'Planner heuristics used to deploy a VM of this offering; if null global config vm.deployment.planner is used';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.deployment.planner', 'FirstFitPlanner', '[''FirstFitPlanner'', ''UserDispersingPlanner'', ''UserConcentratedPodPlanner'']: DeploymentPlanner heuristic that will be used for VM deployment.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'host.reservation.release.period', '300000', 'The interval in milliseconds between host reservation release checks');

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
        `cloud`.`domain` ON disk_offering.domain_id = domain.id;

-- Add "default" field to account/user tables
ALTER TABLE `cloud`.`account` ADD COLUMN `default` int(1) unsigned NOT NULL DEFAULT '0' COMMENT '1 if account is default';
ALTER TABLE `cloud_usage`.`account` ADD COLUMN `default` int(1) unsigned NOT NULL DEFAULT '0' COMMENT '1 if account is default';
ALTER TABLE `cloud`.`user` ADD COLUMN `default` int(1) unsigned NOT NULL DEFAULT '0' COMMENT '1 if user is default';
UPDATE `cloud`.`account` SET `cloud`.`account`.`default`=1 WHERE id IN (1,2);
UPDATE `cloud_usage`.`account` SET `default`=1 WHERE id IN (1,2);
UPDATE `cloud`.`user` SET `cloud`.`user`.`default`=1 WHERE id IN (1,2);

CREATE OR REPLACE VIEW `cloud`.`user_view` AS
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
        user.default,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
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
       

DROP VIEW IF EXISTS `cloud`.`account_view`;
CREATE VIEW `cloud`.`account_view` AS
    select
        account.id,
        account.uuid,
        account.account_name,
        account.type,
        account.state,
        account.removed,
        account.cleanup_needed,
        account.network_domain,
        account.default,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        account_netstats_view.bytesReceived,
        account_netstats_view.bytesSent,
        vmlimit.max vmLimit,
        vmcount.count vmTotal,
        runningvm.vmcount runningVms,
        stoppedvm.vmcount stoppedVms,
        iplimit.max ipLimit,
        ipcount.count ipTotal,
        free_ip_view.free_ip ipFree,
        volumelimit.max volumeLimit,
        volumecount.count volumeTotal,
        snapshotlimit.max snapshotLimit,
        snapshotcount.count snapshotTotal,
        templatelimit.max templateLimit,
        templatecount.count templateTotal,
        vpclimit.max vpcLimit,
        vpccount.count vpcTotal,
        projectlimit.max projectLimit,
        projectcount.count projectTotal,
        networklimit.max networkLimit,
        networkcount.count networkTotal,
        cpulimit.max cpuLimit,
        cpucount.count cpuTotal,
        memorylimit.max memoryLimit,
        memorycount.count memoryTotal,
        primary_storage_limit.max primaryStorageLimit,
        primary_storage_count.count primaryStorageTotal,
        secondary_storage_limit.max secondaryStorageLimit,
        secondary_storage_count.count secondaryStorageTotal,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
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



ALTER TABLE `cloud`.`load_balancing_rules` ADD COLUMN `source_ip_address` varchar(40) COMMENT 'source ip address for the load balancer rule';
ALTER TABLE `cloud`.`load_balancing_rules` ADD COLUMN `source_ip_address_network_id` bigint unsigned COMMENT 'the id of the network where source ip belongs to';
ALTER TABLE `cloud`.`load_balancing_rules` ADD COLUMN `scheme` varchar(40) NOT NULL COMMENT 'load balancer scheme; can be Internal or Public';
UPDATE `cloud`.`load_balancing_rules` SET `scheme`='Public';



-- Add details talbe for the network offering
CREATE TABLE `cloud`.`network_offering_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_offering_id` bigint unsigned NOT NULL COMMENT 'network offering id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_network_offering_details__network_offering_id` FOREIGN KEY `fk_network_offering_details__network_offering_id`(`network_offering_id`) REFERENCES `network_offerings`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Change the constraint for the network service map table. Now we support multiple provider for the same service
ALTER TABLE `cloud`.`ntwk_service_map` DROP FOREIGN KEY `fk_ntwk_service_map__network_id`;
ALTER TABLE `cloud`.`ntwk_service_map` DROP INDEX `network_id`;

ALTER TABLE `cloud`.`ntwk_service_map` ADD UNIQUE `network_id` (`network_id`,`service`,`provider`);
ALTER TABLE `cloud`.`ntwk_service_map` ADD  CONSTRAINT `fk_ntwk_service_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE;


ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `internal_lb` int(1) unsigned NOT NULL DEFAULT '0' COMMENT 'true if the network offering supports Internal lb service';
ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `public_lb` int(1) unsigned NOT NULL DEFAULT '0' COMMENT 'true if the network offering supports Public lb service';
UPDATE `cloud`.`network_offerings` SET public_lb=1 where id IN (SELECT DISTINCT network_offering_id FROM `cloud`.`ntwk_offering_service_map` WHERE service='Lb');


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'internallbvm.service.offering', null, 'Uuid of the service offering used by internal lb vm; if NULL - default system internal lb offering will be used');


alter table `cloud_usage`.`usage_network_offering` add column nic_id bigint(20) unsigned NOT NULL;

CREATE TABLE `cloud`.`portable_ip_range` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid` varchar(40),
  `region_id` int unsigned NOT NULL,
  `vlan_id` varchar(255),
  `gateway` varchar(255),
  `netmask` varchar(255),
  `start_ip` varchar(255),
  `end_ip` varchar(255),
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_portableip__region_id` FOREIGN KEY (`region_id`) REFERENCES `region`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`portable_ip_address` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `account_id` bigint unsigned NULL,
  `domain_id` bigint unsigned NULL,
  `allocated` datetime NULL COMMENT 'Date portable ip was allocated',
  `state` char(32) NOT NULL default 'Free' COMMENT 'state of the portable ip address',
  `region_id` int unsigned NOT NULL,
  `vlan` varchar(255),
  `gateway` varchar(255),
  `netmask` varchar(255),
  `portable_ip_address` varchar(255),
  `portable_ip_range_id` bigint unsigned NOT NULL,
  `data_center_id` bigint unsigned NULL COMMENT 'zone to which portable IP is associated',
  `physical_network_id` bigint unsigned NULL COMMENT 'physical network id in the zone to which portable IP is associated',
  `network_id` bigint unsigned NULL COMMENT 'guest network to which portable ip address is associated with',
  `vpc_id` bigint unsigned COMMENT 'vpc to which portable ip address is associated with',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_portable_ip_address__portable_ip_range_id` FOREIGN KEY (`portable_ip_range_id`) REFERENCES `portable_ip_range`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_portable_ip_address__region_id` FOREIGN KEY (`region_id`) REFERENCES `region`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN is_portable int(1) unsigned NOT NULL default '0';

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
        `cloud`.`domain` ON disk_offering.domain_id = domain.id;

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
        disk_offering.uuid service_offering_uuid,
        service_offering.cpu cpu,
        service_offering.speed speed,
        service_offering.ram_size ram_size,
        disk_offering.name service_offering_name,
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
        vm_instance.dynamically_scalable dynamically_scalable

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
        `cloud`.`disk_offering` ON vm_instance.service_offering_id = disk_offering.id
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
        `cloud`.`user_vm_details` ON user_vm_details.vm_id = vm_instance.id
            and user_vm_details.name = 'SSH.PublicKey'
            left join
        `cloud`.`ssh_keypairs` ON ssh_keypairs.public_key = user_vm_details.value
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
        `cloud`.`affinity_group` ON affinity_group_vm_map.affinity_group_id = affinity_group.id;

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

ALTER TABLE `cloud`.`data_center_details` MODIFY value varchar(1024);
ALTER TABLE `cloud`.`cluster_details` MODIFY value varchar(255);
ALTER TABLE `cloud`.`storage_pool_details` MODIFY value varchar(255);
ALTER TABLE `cloud`.`account_details` MODIFY value varchar(255);

-- END: support for LXC

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
            and (resource_tags.resource_type = 'Template' or resource_tags.resource_type='ISO');
            
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'midonet.apiserver.address', 'http://localhost:8081', 'Specify the address at which the Midonet API server can be contacted (if using Midonet)');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'midonet.providerrouter.id', 'd7c5e6a3-e2f4-426b-b728-b7ce6a0448e5', 'Specifies the UUID of the Midonet provider router (if using Midonet)');

alter table `cloud`.`vpc_gateways` add column `source_nat` boolean default false;
alter table `cloud`.`private_ip_address` add column `source_nat` boolean default false;

CREATE TABLE `cloud`.`account_vnet_map` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `vnet_range` varchar(255) NOT NULL COMMENT 'dedicated guest vlan range',
  `account_id` bigint unsigned NOT NULL COMMENT 'account id. foreign key to account table',
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'physical network id. foreign key to the the physical network table',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_account_vnet_map__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network` (`id`) ON DELETE CASCADE,
  INDEX `i_account_vnet_map__physical_network_id`(`physical_network_id`),
  CONSTRAINT `fk_account_vnet_map__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
  INDEX `i_account_vnet_map__account_id`(`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`op_dc_vnet_alloc` ADD COLUMN account_vnet_map_id bigint unsigned;
ALTER TABLE `cloud`.`op_dc_vnet_alloc` ADD CONSTRAINT `fk_op_dc_vnet_alloc__account_vnet_map_id` FOREIGN KEY `fk_op_dc_vnet_alloc__account_vnet_map_id` (`account_vnet_map_id`) REFERENCES `account_vnet_map` (`id`);
            
 update  `cloud`.`vm_template` set state='Allocated' where state is NULL;
 update  `cloud`.`vm_template` set update_count=0 where update_count is NULL;

CREATE TABLE `cloud`.`network_acl` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of the network acl',
  `uuid` varchar(40),
  `vpc_id` bigint unsigned COMMENT 'vpc this network acl belongs to',
  `description` varchar(1024),
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_acl_item` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `acl_id` bigint unsigned NOT NULL COMMENT 'network acl id',
  `start_port` int(10) COMMENT 'starting port of a port range',
  `end_port` int(10) COMMENT 'end port of a port range',
  `state` char(32) NOT NULL COMMENT 'current state of this rule',
  `protocol` char(16) NOT NULL default 'TCP' COMMENT 'protocol to open these ports for',
  `created` datetime COMMENT 'Date created',
  `icmp_code` int(10) COMMENT 'The ICMP code (if protocol=ICMP). A value of -1 means all codes for the given ICMP type.',
  `icmp_type` int(10) COMMENT 'The ICMP type (if protocol=ICMP). A value of -1 means all types.',
  `traffic_type` char(32) COMMENT 'the traffic type of the rule, can be Ingress or Egress',
  `cidr` varchar(255) COMMENT 'comma seperated cidr list',
  `number` int(10) NOT NULL COMMENT 'priority number of the acl item',
  `action` varchar(10) NOT NULL COMMENT 'rule action, allow or deny',
  PRIMARY KEY  (`id`),
  UNIQUE KEY (`acl_id`, `number`),
  CONSTRAINT `fk_network_acl_item__acl_id` FOREIGN KEY(`acl_id`) REFERENCES `network_acl`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_network_acl_item__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`networks` add column `network_acl_id` bigint unsigned COMMENT 'network acl id';

-- Add Default ACL deny_all
INSERT INTO `cloud`.`network_acl` (id, uuid, vpc_id, description, name) values (1, UUID(), 0, "Default Network ACL Deny All", "default_deny");
INSERT INTO `cloud`.`network_acl_item` (id, uuid, acl_id, state, protocol, created, traffic_type, cidr, number, action) values (1, UUID(), 1, "Active", "all", now(), "Ingress", "0.0.0.0/0", 1, "Deny");
INSERT INTO `cloud`.`network_acl_item` (id, uuid, acl_id, state, protocol, created, traffic_type, cidr, number, action) values (2, UUID(), 1, "Active", "all", now(), "Egress", "0.0.0.0/0", 2, "Deny");

-- Add Default ACL allow_all
INSERT INTO `cloud`.`network_acl` (id, uuid, vpc_id, description, name) values (2, UUID(), 0, "Default Network ACL Allow All", "default_allow");
INSERT INTO `cloud`.`network_acl_item` (id, uuid, acl_id, state, protocol, created, traffic_type, cidr, number, action) values (3, UUID(), 2, "Active", "all", now(), "Ingress", "0.0.0.0/0", 1, "Allow");
INSERT INTO `cloud`.`network_acl_item` (id, uuid, acl_id, state, protocol, created, traffic_type, cidr, number, action) values (4, UUID(), 2, "Active", "all", now(), "Egress", "0.0.0.0/0", 2, "Allow");

CREATE  TABLE `cloud`.`nic_ip_alias` (
  `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT ,
  `uuid`  VARCHAR(40) NOT NULL ,
  `nic_id` BIGINT(20) UNSIGNED NULL ,
  `ip4_address` CHAR(40) NULL ,
  `ip6_address` CHAR(40) NULL ,
  `netmask` CHAR(40) NULL ,
  `gateway` CHAR(40) NULL ,
  `start_ip_of_subnet` CHAR(40),
  `network_id` BIGINT(20) UNSIGNED NULL ,
  `vmId` BIGINT(20) UNSIGNED NULL   ,
  `alias_count` BIGINT(20) UNSIGNED NULL ,
  `created` DATETIME NOT NULL ,
  `account_id` BIGINT(20) UNSIGNED NOT NULL ,
  `domain_id` BIGINT(20) UNSIGNED NOT NULL ,
  `state`  char(32)  NOT NULL,
  PRIMARY KEY (`id`) ,
  UNIQUE INDEX `id_UNIQUE` (`id` ASC) );

alter table `cloud`.`vpc_gateways` add column network_acl_id bigint unsigned default 1 NOT NULL;
update `cloud`.`vpc_gateways` set network_acl_id = 2;


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'VpcManager', 'blacklisted.routes', NULL, 'Routes that are blacklisted, can not be used for Static Routes creation for the VPC Private Gateway');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'enable.dynamic.scale.vm', 'false', 'Enables/Diables dynamically scaling a vm');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'scale.retry', '2', 'Number of times to retry scaling up the vm');

UPDATE `cloud`.`snapshots` set swift_id=null where swift_id=0;

DROP TABLE IF EXISTS `cloud`.`vm_disk_statistics`;
CREATE TABLE `cloud`.`vm_disk_statistics` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `data_center_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `volume_id` bigint(20) unsigned NOT NULL DEFAULT '0',
  `net_io_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `net_io_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `current_io_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `current_io_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `agg_io_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `agg_io_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `net_bytes_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `net_bytes_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `current_bytes_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `current_bytes_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `agg_bytes_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `agg_bytes_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `account_id` (`account_id`,`data_center_id`,`vm_id`,`volume_id`),
  KEY `i_vm_disk_statistics__account_id` (`account_id`),
  KEY `i_vm_disk_statistics__account_id_data_center_id` (`account_id`,`data_center_id`),
  CONSTRAINT `fk_vm_disk_statistics__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8;

insert into `cloud`.`vm_disk_statistics`(data_center_id,account_id,vm_id,volume_id) 
select volumes.data_center_id, volumes.account_id, vm_instance.id, volumes.id from volumes,vm_instance where vm_instance.vm_type="User" and vm_instance.state<>"Expunging" and volumes.instance_id=vm_instance.id order by vm_instance.id;

DROP TABLE IF EXISTS `cloud`.`ovs_providers`;
CREATE TABLE `cloud`.`ovs_providers` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `nsp_id` bigint unsigned NOT NULL COMMENT 'Network Service Provider ID',
  `uuid` varchar(40),
  `enabled` int(1) NOT NULL COMMENT 'Enabled or disabled',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_ovs_providers__nsp_id` FOREIGN KEY (`nsp_id`) REFERENCES `physical_network_service_providers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_ovs_providers__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `cloud_usage`.`vm_disk_statistics`;
CREATE TABLE `cloud_usage`.`vm_disk_statistics` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `data_center_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `volume_id` bigint(20) unsigned NOT NULL DEFAULT '0',
  `net_io_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `net_io_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `current_io_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `current_io_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `agg_io_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `agg_io_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `net_bytes_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `net_bytes_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `current_bytes_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `current_bytes_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `agg_bytes_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `agg_bytes_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `account_id` (`account_id`,`data_center_id`,`vm_id`,`volume_id`)
) ENGINE=InnoDB CHARSET=utf8;

insert into `cloud_usage`.`vm_disk_statistics` select * from `cloud`.`vm_disk_statistics`;

DROP TABLE IF EXISTS `cloud_usage`.`usage_vm_disk`;
CREATE TABLE `cloud_usage`.`usage_vm_disk` (
  `account_id` bigint(20) unsigned NOT NULL,
  `zone_id` bigint(20) unsigned NOT NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `volume_id` bigint(20) unsigned NOT NULL DEFAULT '0',
  `io_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `io_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `agg_io_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `agg_io_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `bytes_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `bytes_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `agg_bytes_read` bigint(20) unsigned NOT NULL DEFAULT '0',
  `agg_bytes_write` bigint(20) unsigned NOT NULL DEFAULT '0',
  `event_time_millis` bigint(20) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`account_id`,`zone_id`,`vm_id`,`volume_id`,`event_time_millis`)
) ENGINE=InnoDB CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.disk.stats.interval', 0, 'Interval (in seconds) to report vm disk statistics.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.disk.throttling.iops_read_rate', 0, 'Default disk I/O read rate in requests per second allowed in User vm\'s disk. ');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.disk.throttling.iops_write_rate', 0, 'Default disk I/O write rate in requests per second allowed in User vm\'s disk. ');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.disk.throttling.bytes_read_rate', 0, 'Default disk I/O read rate in bytes per second allowed in User vm\'s disk. ');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.disk.throttling.bytes_write_rate', 0, 'Default disk I/O write rate in bytes per second allowed in User vm\'s disk. ');

-- Re-enable foreign key checking, at the end of the upgrade path
SET foreign_key_checks = 1;			

UPDATE `cloud`.`snapshot_policy` set uuid=id WHERE uuid is NULL;
#update shared sg enabled network with not null name in Advance Security Group enabled network
UPDATE `cloud`.`networks` set name='Shared SG enabled network', display_text='Shared SG enabled network' WHERE name IS null AND traffic_type='Guest' AND data_center_id IN (select id from data_center where networktype='Advanced' and is_security_group_enabled=1) AND acl_type='Domain';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'use.system.public.ips', 'true', 'If true, when account has dedicated public ip range(s), once the ips dedicated to the account have been consumed ips will be acquired from the system pool');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'use.system.guest.vlans', 'true', 'If true, when account has dedicated guest vlan range(s), once the vlans dedicated to the account have been consumed vlans will be allocated from the system pool');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'execute.in.sequence.hypervisor.commands', 'false', 'If set to true, StartCommand, StopCommand, CopyCommand will be synchronized on the agent side. If set to false, these commands become asynchronous. Default value is false.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'execute.in.sequence.network.element.commands', 'false', 'If set to true, DhcpEntryCommand, SavePasswordCommand, UserDataCommand, VmDataCommand will be synchronized on the agent side. If set to false, these commands become asynchronous. Default value is false.');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'direct.agent.scan.interval', 90, 'Time interval (in seconds) to run the direct agent scan task.');


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'external.baremetal.system.url', null, 'url of external baremetal system that CloudStack will talk to');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'external.baremetal.resource.classname', null, 'class name for handling external baremetal resource');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'enable.baremetal.securitygroup.agent.echo', 'false', 'After starting provision process, periodcially echo security agent installed in the template. Treat provisioning as success only if echo successfully');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'interval.baremetal.securitygroup.agent.echo', 10, 'Interval to echo baremetal security group agent, in seconds');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'timeout.baremetal.securitygroup.agent.echo', 3600, 'Timeout to echo baremetal security group agent, in seconds, the provisioning process will be treated as a failure');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'router.template.hyperv', 'SystemVM Template (HyperV)', 'Name of the default router template on Hyperv.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'router.template.kvm', 'SystemVM Template (KVM)', 'Name of the default router template on KVM.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'router.template.lxc', 'SystemVM Template (LXC)', 'Name of the default router template on LXC.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'router.template.vmware', 'SystemVM Template (vSphere)', 'Name of the default router template on Vmware.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'router.template.xen', 'SystemVM Template (XenServer)', 'Name of the default router template on Xenserver.');

alter table `cloud`.`network_offerings` add column egress_default_policy boolean default false;

-- Add stratospher ssp tables
CREATE TABLE `cloud`.`external_stratosphere_ssp_uuids` (
  `id` bigint(20) UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `uuid` varchar(255) NOT NULL COMMENT "uuid provided by SSP",
  `obj_class` varchar(255) NOT NULL,
  `obj_id` bigint(20) NOT NULL,
  `reservation_id` varchar(255)
) Engine=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`external_stratosphere_ssp_tenants` (
  `id` bigint(20) UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `uuid` varchar(255) NOT NULL COMMENT "SSP tenant uuid",
  `zone_id` bigint(20) NOT NULL COMMENT "cloudstack zone_id"
) Engine=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`external_stratosphere_ssp_credentials` (
  `id` bigint(20) UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  `data_center_id` bigint(20) unsigned NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL
) Engine=InnoDB DEFAULT CHARSET=utf8;


DROP VIEW IF EXISTS `cloud`.`project_view`;
CREATE VIEW `cloud`.`project_view` AS
    select 
        projects.id,
        projects.uuid,
        projects.name,
        projects.display_text,
        projects.state,
        projects.removed,
        projects.created,
        projects.project_account_id,
        account.account_name owner,
        pacct.account_id,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        resource_tags.id tag_id,
        resource_tags.uuid tag_uuid,
        resource_tags.key tag_key,
        resource_tags.value tag_value,
        resource_tags.domain_id tag_domain_id,
        resource_tags.account_id tag_account_id,
        resource_tags.resource_id tag_resource_id,
        resource_tags.resource_uuid tag_resource_uuid,
        resource_tags.resource_type tag_resource_type,
        resource_tags.customer tag_customer
    from
        `cloud`.`projects`
            inner join
        `cloud`.`domain` ON projects.domain_id = domain.id
            inner join
        `cloud`.`project_account` ON projects.id = project_account.project_id
            and project_account.account_role = 'Admin'
            inner join
        `cloud`.`account` ON account.id = project_account.account_id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = projects.id
            and resource_tags.resource_type = 'Project'
            left join
        `cloud`.`project_account` pacct ON projects.id = pacct.project_id;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.max.conn', '4096', 'Load Balancer(haproxy) maximum number of concurrent connections(global max)');

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `concurrent_connections` int(10) unsigned COMMENT 'Load Balancer(haproxy) maximum number of concurrent connections(global max)';

        
ALTER TABLE `cloud`.`sync_queue` MODIFY `queue_size` smallint(6) NOT NULL DEFAULT '0' COMMENT 'number of items being processed by the queue';
ALTER TABLE `cloud`.`sync_queue` MODIFY `queue_size_limit` smallint(6) NOT NULL DEFAULT '1' COMMENT 'max number of items the queue can process concurrently';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'ucs.sync.blade.interval', '3600', 'the interval cloudstack sync with UCS manager for available blades in case user remove blades from chassis without notifying CloudStack');

ALTER TABLE `cloud`.`usage_event` ADD COLUMN `virtual_size` bigint unsigned;
ALTER TABLE `cloud_usage`.`usage_event` ADD COLUMN `virtual_size` bigint unsigned;
ALTER TABLE `cloud_usage`.`usage_storage` ADD COLUMN `virtual_size` bigint unsigned;
ALTER TABLE `cloud_usage`.`cloud_usage` ADD COLUMN `virtual_size` bigint unsigned;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'kvm.ssh.to.agent', 'true', 'Specify whether or not the management server is allowed to SSH into KVM Agents');

#update the account_vmstats_view - count only user vms
DROP VIEW IF EXISTS `cloud`.`account_vmstats_view`;
CREATE VIEW `cloud`.`account_vmstats_view` AS
    SELECT 
        account_id, state, count(*) as vmcount
    from
        `cloud`.`vm_instance`
    where
        vm_type = 'User'
    group by account_id , state;
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'network.loadbalancer.haproxy.max.conn', '4096', 'Load Balancer(haproxy) maximum number of concurrent connections(global max)');


DROP TABLE IF EXISTS `cloud_usage`.`usage_vmsnapshot`;
CREATE TABLE `cloud_usage`.`usage_vmsnapshot` (
  `id` bigint(20) unsigned NOT NULL,
  `zone_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `disk_offering_id` bigint(20) unsigned,
  `size` bigint(20),
  `created` datetime NOT NULL,
  `processed` datetime,
  INDEX `i_usage_vmsnapshot` (`account_id`,`id`,`vm_id`,`created`)
) ENGINE=InnoDB CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'healthcheck.update.interval', '600', 'Time Interval to fetch the LB health check states (in sec)');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Snapshots', 'DEFAULT', 'SnapshotManager', 'kvm.snapshot.enabled', 'false', 'whether snapshot is enabled for KVM hosts');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'eip.use.multiple.netscalers', 'false', 'Should be set to true, if there will be multiple NetScaler devices providing EIP service in a zone');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Snapshots', 'DEFAULT', 'SnapshotManager', 'snapshot.backup.rightafter', 'true', 'backup snapshot right after snapshot is taken');

DELETE FROM `cloud`.`configuration` where name='vmware.guest.vswitch';
DELETE FROM `cloud`.`configuration` where name='vmware.private.vswitch';
DELETE FROM `cloud`.`configuration` where name='vmware.public.vswitch';


UPDATE `cloud`.`autoscale_vmgroups` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`autoscale_vmprofiles` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`autoscale_policies` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`counter` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`conditions` set uuid=id WHERE uuid is NULL;
update `cloud`.`configuration` set component = 'SnapshotManager' where category = 'Snapshots' and component = 'none';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'management-server', 'storage.cache.replacement.lru.interval', '30', 'time interval for unused data on cache storage (in days).');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'management-server', 'storage.cache.replacement.enabled', 'true', 'enable or disable cache storage replacement algorithm.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'management-server', 'storage.cache.replacement.interval', '86400', 'time interval between cache replacement threads (in seconds).');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Advanced", 'DEFAULT', 'management-server', 'vmware.nested.virtualization', 'false', 'When set to true this will enable nested virtualization when this is supported by the hypervisor');


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
        data_center.removed,
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



UPDATE `cloud`.`ntwk_offering_service_map` SET Provider='VpcVirtualRouter' WHERE network_offering_id IN (SELECT id from `cloud`.`network_offerings` WHERE name IN ('DefaultIsolatedNetworkOfferingForVpcNetworks', 'DefaultIsolatedNetworkOfferingForVpcNetworksNoLB'));
