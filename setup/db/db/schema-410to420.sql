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

ALTER TABLE `cloud`.`hypervisor_capabilities` ADD COLUMN `max_hosts_per_cluster` int unsigned DEFAULT NULL COMMENT 'Max. hosts in cluster supported by hypervisor';
UPDATE `cloud`.`hypervisor_capabilities` SET `max_hosts_per_cluster`=32 WHERE `hypervisor_type`='VMware';
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_hosts_per_cluster) VALUES ('VMware', '5.1', 128, 0, 32);
DELETE FROM `cloud`.`configuration` where name='vmware.percluster.host.max';
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'xen.nics.max', '7', 'Maximum allowed nics for Vms created on Xen');
ALTER TABLE `cloud`.`load_balancer_vm_map` ADD state VARCHAR(40) NULL COMMENT 'service status updated by LB healthcheck manager';

alter table storage_pool change storage_provider_id storage_provider_name varchar(255);
alter table template_host_ref add state varchar(255);
alter table template_host_ref add update_count bigint unsigned;
alter table template_host_ref add updated datetime;
alter table volume_host_ref add state varchar(255);
alter table volume_host_ref add update_count bigint unsigned;
alter table volume_host_ref add updated datetime;
alter table template_spool_ref add updated datetime;
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

CREATE TABLE `cloud`.`image_store` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of data store',
  `image_provider_name` varchar(255) NOT NULL COMMENT 'id of image_store_provider',
  `protocol` varchar(255) NOT NULL COMMENT 'protocol of data store',
  `url` varchar(255) COMMENT 'url for image data store',
  `data_center_id` bigint unsigned  COMMENT 'datacenter id of data store',
  `scope` varchar(255) COMMENT 'scope of data store',
  `uuid` varchar(255) COMMENT 'uuid of data store',
  `parent` varchar(255) COMMENT 'parent path for the storage server',
  `created` datetime COMMENT 'date the image store first signed on',
  `removed` datetime COMMENT 'date removed if not null',  
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
  `physical_size` bigint unsigned DEFAULT 0,
  `download_state` varchar(255),
  `error_str` varchar(255),
  `local_path` varchar(255),
  `install_path` varchar(255),
  `url` varchar(255),
  `state` varchar(255) NOT NULL,
  `destroyed` tinyint(1) COMMENT 'indicates whether the template_store entry was destroyed by the user or not',
  `is_copy` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'indicates whether this was copied ',
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
  `size` bigint unsigned,
  `physical_size` bigint unsigned DEFAULT 0,
  `install_path` varchar(255),
  `state` varchar(255) NOT NULL,  
  `destroyed` tinyint(1) COMMENT 'indicates whether the snapshot_store entry was destroyed by the user or not',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_snapshot_store_ref__store_id` FOREIGN KEY `fk_snapshot_store_ref__store_id` (`store_id`) REFERENCES `image_store` (`id`) ON DELETE CASCADE,
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
  `state` varchar(255) NOT NULL,  
  `format` varchar(32) NOT NULL COMMENT 'format for the volume', 
  `destroyed` tinyint(1) COMMENT 'indicates whether the volume_host entry was destroyed by the user or not',
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
  UNIQUE (`vpc_id`, `service`)
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


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.instancename.flag', 'false', 'Append guest VM display Name (if set) to the internal name of the VM');

INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (208, UUID(), 6, 'Windows 8');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (209, UUID(), 6, 'Windows 8 (64 bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (210, UUID(), 6, 'Windows 8 Server (64 bit)');
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Windows 8', 208);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Windows 8 (64 bit)', 209);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Windows 8 Server (64 bit)', 210);

CREATE TABLE `cloud`.`user_vm_clone_setting` (
  `vm_id` bigint unsigned NOT NULL COMMENT 'guest VM id',
  `clone_type` varchar(10) NOT NULL COMMENT 'Full or Linked Clone (applicable to VMs on ESX)',
  PRIMARY KEY (`vm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'UserVmManager', 'vmware.create.full.clone' , 'false', 'If set to true, creates VMs as full clones on ESX hypervisor');

-- Re-enable foreign key checking, at the end of the upgrade path
SET foreign_key_checks = 1;


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

ALTER TABLE `cloud`.`region` ADD COLUMN `gslb_service_enabled` tinyint(1) unsigned NOT NULL DEFAULT 1 COMMENT 'Is GSLB service enalbed in the Region';

ALTER TABLE `cloud`.`external_load_balancer_devices` ADD COLUMN `is_gslb_provider` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if load balancer appliance is acting as gslb service provider in the zone';

ALTER TABLE `cloud`.`external_load_balancer_devices` ADD COLUMN `gslb_site_publicip` varchar(255)  DEFAULT NULL COMMENT 'GSLB service Provider site public ip';

ALTER TABLE `cloud`.`external_load_balancer_devices` ADD COLUMN `gslb_site_privateip` varchar(255) DEFAULT NULL COMMENT 'GSLB service Provider site private ip';

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
  `revoke` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 is when rule is set for Revoke',
  PRIMARY KEY  (`id`),
  UNIQUE KEY (`gslb_rule_id`, `lb_rule_id`),
  CONSTRAINT `fk_gslb_rule_id` FOREIGN KEY(`gslb_rule_id`) REFERENCES `global_load_balancing_rules`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_lb_rule_id` FOREIGN KEY(`lb_rule_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.cpus', '40', 'The default maximum number of cpu cores that can be used for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.memory', '40960', 'The default maximum memory (in MiB) that can be used for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.primary.storage', '200', 'The default maximum primary storage space (in GiB) that can be used for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.secondary.storage', '400', 'The default maximum secondary storage space (in GiB) that can be used for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.cpus', '40', 'The default maximum number of cpu cores that can be used for a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.memory', '40960', 'The default maximum memory (in MiB) that can be used for a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.primary.storage', '200', 'The default maximum primary storage space (in GiB) that can be used for a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.secondary.storage', '400', 'The default maximum secondary storage space (in GiB) that can be used for a project');

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

ALTER TABLE `cloud`.`remote_access_vpn` ADD COLUMN `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id';
ALTER TABLE `cloud`.`remote_access_vpn` ADD COLUMN `uuid` varchar(40) UNIQUE;

-- START: support for LXC
 
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('LXC', 'default', 50, 1);
ALTER TABLE `cloud`.`physical_network_traffic_types` ADD COLUMN `lxc_network_label` varchar(255) DEFAULT 'cloudbr0' COMMENT 'The network name label of the physical device dedicated to this traffic on a LXC host';
 
UPDATE configuration SET value='KVM,XenServer,VMware,BareMetal,Ovm,LXC' WHERE name='hypervisor.list';
 
INSERT INTO `cloud`.`vm_template` (id, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id, featured, cross_zones, hypervisor_type)
     VALUES (10, 'routing-10', 'SystemVM Template (LXC)', 0, now(), 'SYSTEM', 0, 64, 1, 'http://download.cloud.com/templates/acton/acton-systemvm-02062012.qcow2.bz2', '2755de1f9ef2ce4d6f2bee2efbb4da92', 0, 'SystemVM Template (LXC)', 'QCOW2', 15, 0, 1, 'LXC');

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
        resource_tags.customer tag_customer
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
        `cloud`.`template_zone_ref` ON template_zone_ref.template_id = vm_template.id
            AND template_zone_ref.removed is null
            left join
        `cloud`.`data_center` ON template_zone_ref.zone_id = data_center.id
            left join
        `cloud`.`image_store` ON image_store.data_center_id = data_center.id OR image_store.scope = 'REGION'
            left join
        `cloud`.`template_store_ref` ON template_store_ref.template_id = vm_template.id AND template_store_ref.store_id = image_store.id
            left join
        `cloud`.`launch_permission` ON launch_permission.template_id = vm_template.id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = vm_template.id
            and (resource_tags.resource_type = 'Template' or resource_tags.resource_type='ISO');


DROP VIEW IF EXISTS `cloud`.`volume_view`;
CREATE VIEW `cloud`.`volume_view` AS
    select 
        volumes.id,
        volumes.uuid,
        volumes.name,
        volumes.device_id,
        volumes.volume_type,
        volumes.size,
        volumes.created,
        volumes.state,
        volumes.attached,
        volumes.removed,
        volumes.pod_id,
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
        vm_instance.id vm_id,
        vm_instance.uuid vm_uuid,
        vm_instance.name vm_name,
        vm_instance.state vm_state,
        vm_instance.vm_type,
        user_vm.display_name vm_display_name,
        volume_host_ref.size volume_host_size,
        volume_host_ref.created volume_host_created,
        volume_host_ref.format,
        volume_host_ref.download_pct,
        volume_host_ref.download_state,
        volume_host_ref.error_str,
        disk_offering.id disk_offering_id,
        disk_offering.uuid disk_offering_uuid,
        disk_offering.name disk_offering_name,
        disk_offering.display_text disk_offering_display_text,
        disk_offering.use_local_storage,
        disk_offering.system_use,
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
            and volumes.data_center_id = volume_store_ref.zone_id
            left join
        `cloud`.`disk_offering` ON volumes.disk_offering_id = disk_offering.id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`cluster` ON storage_pool.cluster_id = cluster.id
            left join
        `cloud`.`vm_template` ON volumes.template_id = vm_template.id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = volumes.id
            and resource_tags.resource_type = 'Volume'
            left join
        `cloud`.`async_job` ON async_job.instance_id = volumes.id
            and async_job.instance_type = 'Volume'
            and async_job.job_status = 0;     
