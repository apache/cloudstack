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
-- Schema upgrade from 2.2.2 to 2.2.4;
--;

CREATE TABLE IF NOT EXISTS `cloud`.`version` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `version` char(40) NOT NULL UNIQUE COMMENT 'version',
  `updated` datetime NOT NULL COMMENT 'Date this version table was updated',
  `step` char(32) NOT NULL COMMENT 'Step in the upgrade to this version',
  PRIMARY KEY (`id`),
  INDEX `i_version__version`(`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`op_host_capacity` ADD COLUMN `cluster_id` bigint unsigned AFTER `pod_id`;
ALTER TABLE `cloud`.`op_host_capacity` ADD CONSTRAINT `fk_op_host_capacity__cluster_id` FOREIGN KEY `fk_op_host_capacity__cluster_id` (`cluster_id`) REFERENCES `cloud`.`cluster`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`op_host_capacity` ADD INDEX `i_op_host_capacity__cluster_id`(`cluster_id`);
ALTER TABLE `cloud`.`usage_event` ADD COLUMN `resource_type` varchar(32);

CREATE TABLE `cloud`.`domain_network_ref` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain id',
  `network_id` bigint unsigned NOT NULL COMMENT 'network id',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_domain_network_ref__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_domain_network_ref__networks_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`nics` MODIFY `ip4_address` char(40);
ALTER TABLE `cloud`.`op_lock` MODIFY `ip` char(40) NOT NULL;
ALTER TABLE `cloud`.`volumes` MODIFY `host_ip` char(40);
ALTER TABLE `cloud`.`op_dc_ip_address_alloc` MODIFY `ip_address` char(40) NOT NULL;
ALTER TABLE `cloud`.`op_dc_link_local_ip_address_alloc` MODIFY `ip_address` char(40) NOT NULL;
ALTER TABLE `cloud`.`host` MODIFY `private_ip_address` char(40) NOT NULL;
ALTER TABLE `cloud`.`host` MODIFY `storage_ip_address` char(40) NOT NULL;
ALTER TABLE `cloud`.`host` MODIFY `storage_ip_address_2` char(40);
ALTER TABLE `cloud`.`host` MODIFY `public_ip_address` char(40);
ALTER TABLE `cloud`.`mshost` MODIFY `service_ip` char(40) NOT NULL;
ALTER TABLE `cloud`.`user_statistics` MODIFY `public_ip_address` char(40);
ALTER TABLE `cloud`.`vm_instance` MODIFY `private_ip_address` char(40);
ALTER TABLE `cloud`.`user_vm` MODIFY `guest_ip_address` char(40);
ALTER TABLE `cloud`.`domain_router` MODIFY `public_ip_address` char(40);
ALTER TABLE `cloud`.`domain_router` MODIFY `guest_ip_address` char(40);
ALTER TABLE `cloud`.`console_proxy` MODIFY `public_ip_address` char(40) UNIQUE;
ALTER TABLE `cloud`.`secondary_storage_vm` MODIFY `public_ip_address` char(40) UNIQUE;
ALTER TABLE `cloud`.`remote_access_vpn` MODIFY `local_ip` char(40) NOT NULL;
ALTER TABLE `cloud`.`user_ip_address` MODIFY `public_ip_address` char(40) NOT NULL;

#Commented out these lines because they have to be done inside java.;
#ALTER TABLE `cloud`.`networks` DROP FOREIGN KEY `fk_networks__related`;
#ALTER TABLE `cloud`.`networks` ADD CONSTRAINT `fk_networks__related` FOREIGN KEY(`related`) REFERENCES `networks`(`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`cluster` ADD COLUMN  `removed` datetime COMMENT 'date removed if not null';
ALTER TABLE `cloud`.`cluster` MODIFY `name` varchar(255) COMMENT 'name for the cluster';

ALTER TABLE `cloud`.`network_offerings` MODIFY `guest_type` char(32);


UPDATE `cloud`.`network_offerings` SET `nw_rate`=0, `mc_rate`=0 WHERE system_only=1 and guest_type IS NULL;
UPDATE `cloud`.`network_offerings` SET `default`=1 WHERE system_only=1;

ALTER TABLE `cloud`.`data_center` ADD COLUMN `allocation_state` varchar(32) NOT NULL DEFAULT 'Enabled';
ALTER TABLE `cloud`.`data_center` ADD INDEX `i_data_center__allocation_state`(`allocation_state`);
ALTER TABLE `cloud`.`cluster` ADD COLUMN `allocation_state` varchar(32) NOT NULL DEFAULT 'Enabled';
ALTER TABLE `cloud`.`cluster` ADD INDEX `i_cluster__allocation_state`(`allocation_state`);
ALTER TABLE `cloud`.`host_pod_ref` ADD COLUMN `allocation_state` varchar(32) NOT NULL DEFAULT 'Enabled';
ALTER TABLE `cloud`.`host_pod_ref` ADD INDEX `i_host_pod_ref__allocation_state`(`allocation_state`);
ALTER TABLE `cloud`.`host` ADD COLUMN `allocation_state` varchar(32) NOT NULL DEFAULT 'Enabled';
ALTER TABLE `cloud`.`host` ADD INDEX `i_host__allocation_state`(`allocation_state`);

ALTER TABLE `cloud`.`domain` ADD INDEX `i_domain__path`(`path`);

CREATE TABLE `cloud`.`data_center_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `dc_id` bigint unsigned NOT NULL COMMENT 'dc id',
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_dc_details__dc_id` FOREIGN KEY (`dc_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`op_host_capacity` MODIFY `used_capacity` bigint signed NOT NULL;
ALTER TABLE `cloud`.`op_host_capacity` MODIFY `reserved_capacity` bigint signed NOT NULL;
ALTER TABLE `cloud`.`op_host_capacity` MODIFY `total_capacity` bigint signed NOT NULL;

INSERT IGNORE INTO `cloud`.`configuration` VALUES 
('Advanced','DEFAULT','management-server','control.cidr','169.254.0.0/16','Changes the cidr for the control network traffic.  Defaults to using link local.  Must be unique within pods'),
('Advanced','DEFAULT','management-server','control.gateway','169.254.0.1','gateway for the control network traffic'),
('Advanced','DEFAULT','AgentManager','cmd.wait','7200','Time (in seconds) to wait for some heavy time-consuming commands'),
('Console Proxy','DEFAULT','AgentManager','consoleproxy.cpu.mhz','500','CPU speed (in MHz) used to create new console proxy VMs'),
('Console Proxy','DEFAULT','AgentManager','consoleproxy.disable.rpfilter','true','disable rp_filter on console proxy VM public interface'),
('Console Proxy','DEFAULT','AgentManager','consoleproxy.launch.max','10','maximum number of console proxy instances per zone can be launched'),
('Console Proxy','DEFAULT','AgentManager','consoleproxy.restart','true','Console proxy restart flag, defaulted to true'),
('Console Proxy','DEFAULT','AgentManager','consoleproxy.url.domain','realhostip.com','Console proxy url domain'),
('Advanced','DEFAULT','management-server','extract.url.cleanup.interval','120','The interval (in seconds) to wait before cleaning up the extract URL\'s '),
('Network','DEFAULT','AgentManager','guest.ip.network','10.1.1.1','The network address of the guest virtual network. Virtual machines will be assigned an IP in this subnet.'),
('Network','DEFAULT','AgentManager','guest.netmask','255.255.255.0','The netmask of the guest virtual network.'), 
('Network','DEFAULT','management-server','guest.vlan.bits','12','The number of bits to reserve for the VLAN identifier in the guest subnet.'),
('Advanced','DEFAULT','management-server','host.capacity.checker.interval','3600','Time (in seconds) to wait before recalculating host\'s capacity'),
('Advanced','DEFAULT','management-server','host.capacity.checker.wait','3600','Time (in seconds) to wait before starting host capacity background checker'),
('Advanced','DEFAULT','management-server','host.capacityType.to.order.clusters','CPU','The host capacity type (CPU or RAM) is used by deployment planner to order clusters during VM resource allocation'),
('Advanced','DEFAULT','management-server','job.cancel.threshold.minutes','60','Time (in minutes) for async-jobs to be forcely cancelled if it has been in process for long'),
('Advanced','DEFAULT','management-server','kvm.private.network.device',NULL,'Specify the private bridge on host for private network'),
('Advanced','DEFAULT','management-server','kvm.public.network.device',NULL,'Specify the public bridge on host for public network'),
('Advanced','DEFAULT','management-server','network.gc.interval','600','Seconds to wait before checking for networks to shutdown'),
('Advanced','DEFAULT','management-server','network.gc.wait','600','Time (in seconds) to wait before shutting down a network that\'s not in used'),
('Network','DEFAULT','management-server','open.vswitch.tunnel.network','false','enable/disable open vswitch tunnel network(no vlan)'),
('Network','DEFAULT','management-server','open.vswitch.vlan.network','false','enable/disable vlan remapping of  open vswitch network'),
('Advanced','DEFAULT','none','router.cpu.mhz','500','Default CPU speed (MHz) for router VM.'),
('Advanced','DEFAULT','none','router.template.id','1','Default ID for template.'),
('Advanced','DEFAULT','AgentManager','secstorage.vm.cpu.mhz','500','CPU speed (in MHz) used to create new secondary storage vms'),
('Snapshots','DEFAULT','none','snapshot.delta.max','16','max delta snapshots between two full snapshots.'),
('Advanced','DEFAULT','management-server','system.vm.auto.reserve.capacity','true','Indicates whether or not to automatically reserver system VM standby capacity.'),
('Advanced','DEFAULT','management-server','use.user.concentrated.pod.allocation','true','If true, deployment planner applies the user concentration heuristic during VM resource allocation'),
('Advanced','DEFAULT','management-server','vm.op.cancel.interval','3600','Time (in seconds) to wait before cancelling a operation'),
('Advanced','DEFAULT','management-server','vm.op.cleanup.interval','86400','Interval to run the thread that cleans up the vm operations (in seconds)'),
('Advanced','DEFAULT','management-server','vm.op.cleanup.wait','3600','Time (in seconds) to wait before cleanuping up any vm work items'),
('Advanced','DEFAULT','management-server','vm.op.lock.state.retry','5','Times to retry locking the state of a VM for operations'),
('Advanced','DEFAULT','management-server','vm.op.wait.interval','120','Time (in seconds) to wait before checking if a previous operation has succeeded'),
('Advanced','DEFAULT','management-server','vm.stats.interval','60000','The interval (in milliseconds) when vm stats are retrieved from agents.'),
('Advanced','DEFAULT','management-server','vm.tranisition.wait.interval','3600','Time (in seconds) to wait before taking over a VM in transition state'),
('Advanced','DEFAULT','management-server','vmware.guest.vswitch',NULL,'Specify the vSwitch on host for guest network'),
('Advanced','DEFAULT','management-server','vmware.private.vswitch',NULL,'Specify the vSwitch on host for private network'),
('Advanced','DEFAULT','management-server','vmware.public.vswitch',NULL,'Specify the vSwitch on host for public network'),
('Advanced','DEFAULT','management-server','vmware.service.console','Service Console','Specify the service console network name (ESX host only)'),
('Advanced','DEFAULT','AgentManager','xapiwait','600','Time (in seconds) to wait for XAPI to return'),
('Account Defaults','DEFAULT','management-server','max.account.user.vms','20','The default maximum number of user VMs that can be deployed for an account'),
('Account Defaults','DEFAULT','management-server','max.account.public.ips','20','The default maximum number of public IPs that can be consumed by an account'),
('Account Defaults','DEFAULT','management-server','max.account.templates','20','The default maximum number of templates that can be deployed for an account'),
('Account Defaults','DEFAULT','management-server','max.account.snapshots','20','The default maximum number of snapshots that can be created for an account'),
('Account Defaults','DEFAULT','management-server','max.account.volumes','20','The default maximum number of volumes that can be created for an account');

ALTER TABLE `cloud`.`op_dc_ip_address_alloc` CHANGE COLUMN `instance_id` `nic_id` bigint unsigned DEFAULT NULL;
ALTER TABLE `cloud`.`op_dc_link_local_ip_address_alloc` CHANGE COLUMN `instance_id` `nic_id` bigint unsigned DEFAULT NULL;


DELETE FROM `cloud`.`sequence` WHERE name='snapshots_seq';
UPDATE `cloud`.`service_offering` s, `cloud`.`disk_offering` d SET s.ha_enabled=1 where s.id=d.id and d.system_use=1;

UPDATE `cloud`.`data_center` SET is_security_group_enabled=1 WHERE networktype='Basic';
UPDATE `cloud`.`networks` SET is_security_group_enabled=1 WHERE guru_name='DirectPodBasedNetworkGuru';

UPDATE `cloud`.`nics` SET ip_type='Ip4';
UPDATE `cloud`.`nics` SET broadcast_uri='vlan://untagged', isolation_uri='ec2://untagged', strategy='Create' where reserver_name='DirectPodBasedNetworkGuru';


DELETE FROM `cloud`.`configuration` where name='schema.level';
DELETE FROM `cloud`.`configuration` where name='direct.attach.security.groups.enabled';

ALTER TABLE `cloud`.`domain` MODIFY COLUMN `path` varchar(255) NOT NULL;

ALTER TABLE `cloud`.`network_offerings` ADD INDEX `i_network_offerings__removed`(`removed`);

ALTER TABLE `cloud`.`nics` ADD INDEX `i_nics__removed`(`removed`);

ALTER TABLE `cloud`.`snapshot_schedule` DROP KEY `volume_id`;
ALTER TABLE `cloud`.`snapshot_schedule` ADD UNIQUE KEY  `volume_id` (`volume_id`,`policy_id`);

ALTER TABLE `cloud`.`storage_pool` MODIFY COLUMN `uuid` varchar(255) UNIQUE;

ALTER TABLE `cloud`.`user_statistics` DROP KEY `account_id`;
ALTER TABLE `cloud`.`user_statistics` ADD UNIQUE KEY `account_id` (`account_id`,`data_center_id`, `public_ip_address`, `device_id`,`device_type`); 

ALTER TABLE `cloud`.`usage_event` ADD INDEX `i_usage_event__created`(`created`);

UPDATE `cloud`.`vm_instance` SET ha_enabled=0 WHERE type='ConsoleProxy';

UPDATE service_offering SET ha_enabled=0 WHERE id=(SELECT id FROM disk_offering WHERE name LIKE '%console proxy%' AND system_use=1);

ALTER TABLE `cloud`.`storage_pool_details` DROP KEY `i_storage_pool_details__name__value`;
ALTER TABLE `cloud`.`storage_pool_details` ADD INDEX `i_storage_pool_details__name__value`(`name`(128), `value`(128));

DROP TABLE `cloud`.`load_balancer`;


UPDATE `cloud`.`nics` SET strategy='Create' where reserver_name='DirectNetworkGuru';

UPDATE storage_pool SET cluster_id=(SELECT cluster_id FROM host INNER JOIN storage_pool_host_ref WHERE host.id=storage_pool_host_ref.host_id AND storage_pool_host_ref.pool_id=storage_pool.id) WHERE pool_type='LVM';
UPDATE `cloud`.`host` SET resource='com.cloud.hypervisor.xen.resource.XenServer56FP1Resource' WHERE resource='com.cloud.hypervisor.xen.resource.XenServer56FP1PremiumResource';

