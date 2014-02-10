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

--step 1
-- drop all constraints for user_ip_address
ALTER TABLE firewall_rules DROP foreign key fk_firewall_rules__ip_address ;
ALTER TABLE remote_access_vpn DROP foreign key fk_remote_access_vpn__server_addr ; 
ALTER TABLE user_ip_address DROP primary key;



--step 2A
--schema+data changes
----------------------------------------user ip address table-------------------------------------------------------------------------
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `id` bigint unsigned NOT NULL auto_increment primary key;
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `source_network_id` bigint unsigned NOT NULL COMMENT 'network id ip belongs to';
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `vm_id` bigint unsigned NOT NULL COMMENT 'foreign key to virtual machine id';
UPDATE user_ip_address SET source_network_id=(select network_id from vlan where vlan.id=user_ip_address.vlan_db_id);

-------------------------------firewall_rules table -------------------------------------------------------------------------------------
ALTER TABLE `cloud`.`firewall_rules` ADD COLUMN `ip_address_id` bigint unsigned NOT NULL COMMENT 'foreign key to ip address table';
UPDATE firewall_rules set ip_address_id = (SELECT id from user_ip_address where public_ip_address = firewall_rules.ip_address);
ALTER TABLE `cloud`.`firewall_rules` ADD COLUMN `is_static_nat` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if firewall rule is one to one nat rule';
UPDATE firewall_rules set protocol='tcp',is_static_nat=1 where protocol='NAT';
UPDATE firewall_rules set start_port = 1, end_port = 65535 where start_port = -1 AND end_port = -1;
ALTER TABLE `cloud`.`firewall_rules` DROP COLUMN ip_address;

-------------------------------port forwarding table ---------------------------------------------------------------------------------------
UPDATE port_forwarding_rules set dest_port_start = 1, dest_port_end = 65535 where dest_port_start = -1 AND dest_port_end = -1;

----------------------------------remote_access_vpn table ----------------------------------------------------------------------------------
ALTER TABLE `cloud`.`remote_access_vpn` ADD COLUMN `vpn_server_addr_id` bigint unsigned NOT NULL COMMENT 'foreign key to ip address table';
UPDATE remote_access_vpn SET vpn_server_addr_id = (SELECT id from user_ip_address where public_ip_address = remote_access_vpn.vpn_server_addr);
ALTER TABLE `cloud`.`remote_access_vpn` DROP COLUMN vpn_server_addr;

--------------------------user_ip_address table re-visited------------------------------------------------------------------------------------
--step 2B
--done in the java layer
-- the updates the user ip address table with the vm id; using a 3 way join on firewall rules, user ip address, port forwarding tables
-- to do this, run Db22beta4to22GAMigrationUtil.java

--step 2C
DROP VIEW if exists user_ip_address_view;
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `public_ip_address1` char(40) NOT NULL COMMENT 'the public ip address';
UPDATE user_ip_address SET public_ip_address1 = INET_NTOA(public_ip_address); 
ALTER TABLE `cloud`.`user_ip_address` DROP COLUMN public_ip_address;
ALTER TABLE `cloud`.`user_ip_address` CHANGE public_ip_address1 public_ip_address char(40) NOT NULL COMMENT 'the public ip address';

DROP VIEW if exists port_forwarding_rules_view;
ALTER TABLE `cloud`.`port_forwarding_rules` ADD COLUMN `dest_ip_address1` char(40) NOT NULL COMMENT 'the destination ip address';
UPDATE port_forwarding_rules SET dest_ip_address1 = INET_NTOA(dest_ip_address);
ALTER TABLE `cloud`.`port_forwarding_rules` DROP COLUMN dest_ip_address;
ALTER TABLE `cloud`.`port_forwarding_rules` CHANGE dest_ip_address1 dest_ip_address char(40) NOT NULL COMMENT 'the destination ip address';



--step3 (Run this ONLY after the java program is run: Db22beta4to22GAMigrationUtil.java)
---------------------------------------------------------------------------------------------------------------------------------------------------
--recreate indices
ALTER TABLE `cloud`.`firewall_rules` ADD CONSTRAINT `fk_firewall_rules__ip_address_id` FOREIGN KEY(`ip_address_id`) REFERENCES `user_ip_address`(`id`);
ALTER TABLE `cloud`.`remote_access_vpn` ADD CONSTRAINT `fk_remote_access_vpn__server_addr` FOREIGN KEY `fk_remote_access_vpn__server_addr_id` (`vpn_server_addr_id`) REFERENCES `user_ip_address` (`id`);
ALTER TABLE `cloud`.`op_it_work` ADD CONSTRAINT `fk_op_it_work__mgmt_server_id` FOREIGN KEY (`mgmt_server_id`) REFERENCES `mshost`(`msid`);
ALTER TABLE `cloud`.`op_it_work` ADD CONSTRAINT `fk_op_it_work__instance_id` FOREIGN KEY (`instance_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`op_it_work` ADD INDEX `i_op_it_work__step`(`step`);
ALTER TABLE `cloud`.`user_ip_address` ADD UNIQUE (source_network_id, public_ip_address);


--step 4 (independent of above)

ALTER TABLE `cloud`.`user_statistics` CHANGE `host_id` `device_id` bigint unsigned NOT NULL default 0;
ALTER TABLE `cloud`.`user_statistics` ADD COLUMN `device_type` varchar(32) NOT NULL default 'DomainRouter';
UPDATE `cloud`.`user_statistics` us,`cloud`.`host` h SET us.device_type = h.type where us.device_id = h.id AND us.device_id > 0;
ALTER TABLE `cloud`.`user_statistics` ADD UNIQUE (`account_id`, `data_center_id`, `public_ip_address`, `device_id`, `device_type`);

ALTER TABLE `cloud`.`snapshots` modify `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'Primary Key';

----------------------usage changes (for cloud_usage database)--------------------------------------------------------------------------------------------------------------

ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `device_id` bigint unsigned NOT NULL default 0;
ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `device_type` varchar(32) NOT NULL default 'DomainRouter';
ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `public_ip_address` varchar(15);
UPDATE `cloud_usage`.`user_statistics` cus, `cloud`.`user_statistics` us SET cus.device_id=us.device_id, cus.device_type=us.device_type, cus.public_ip_address=us.public_ip_address WHERE cus.id = us.id;
ALTER TABLE `cloud_usage`.`user_statistics` ADD UNIQUE (`account_id`, `data_center_id`, `public_ip_address`, `device_id`, `device_type`);

INSERT INTO user_statistics ( account_id, data_center_id, device_id, device_type ) SELECT VM.account_id, VM.data_center_id, DR.id,'DomainRouter' FROM vm_instance VM, domain_router DR WHERE VM.id = DR.id;

ALTER TABLE `cloud_usage`.`usage_network` ADD COLUMN `host_id` bigint unsigned NOT NULL default 0;
ALTER TABLE `cloud_usage`.`usage_network` ADD COLUMN `host_type` varchar(32);
ALTER TABLE `cloud_usage`.`usage_network` drop PRIMARY KEY;
ALTER TABLE `cloud_usage`.`usage_network` add PRIMARY KEY (`account_id`, `zone_id`, `host_id`, `event_time_millis`);

ALTER TABLE `cloud_usage`.`usage_ip_address` ADD COLUMN `id` bigint unsigned NOT NULL;
ALTER TABLE `cloud_usage`.`usage_ip_address` ADD COLUMN `is_source_nat` smallint(1) NOT NULL default 0;

update `cloud`.`usage_event` SET size = 0 where type = 'NET.IPASSIGN' and size is null;
update `cloud_usage`.`usage_event` SET size = 0 where type = 'NET.IPASSIGN' and size is null;

----------------------volume units changed from MB to bytes. Update the same in existing usage_volume records and volume usage events which are not processed-------------

update `cloud_usage`.`usage_volume` set size = (size * 1048576);
update `cloud_usage`.`usage_event` set size = (size * 1048576) where type = 'VOLUME.CREATE' and processed = 0;

ALTER TABLE `cloud_usage`.`cloud_usage` ADD COLUMN `type` varchar(32);

CREATE TABLE  `cloud_usage`.`usage_port_forwarding` (
  `id` bigint unsigned NOT NULL,
  `zone_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `deleted` DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud_usage`.`usage_network_offering` (
  `zone_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `vm_instance_id` bigint unsigned NOT NULL,
  `network_offering_id` bigint unsigned NOT NULL,
  `is_default` smallint(1) NOT NULL,
  `created` DATETIME NOT NULL,
  `deleted` DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
