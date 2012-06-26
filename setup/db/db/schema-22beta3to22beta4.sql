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

CREATE TABLE `cloud`.`host_tags` (
  `id` bigint unsigned NOT NULL auto_increment,
  `host_id` bigint unsigned NOT NULL COMMENT 'host id',
  `tag` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ovs_host_vlan_alloc`(
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `host_id` bigint unsigned COMMENT 'host id',
  `account_id` bigint unsigned COMMENT 'account id',
  `vlan` bigint unsigned COMMENT 'vlan id under account #account_id on host #host_id',
  `ref` int unsigned NOT NULL DEFAULT 0 COMMENT 'reference count',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ovs_tunnel_alloc`(
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `from` bigint unsigned COMMENT 'from host id',
  `to` bigint unsigned COMMENT 'to host id',
  `in_port` int unsigned COMMENT 'in port on open vswitch',
  PRIMARY KEY(`from`, `to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ovs_vlan_mapping_dirty`(
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `account_id` bigint unsigned COMMENT 'account id',
  `dirty` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 means vlan mapping of this account was changed',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ovs_vm_flow_log` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint unsigned NOT NULL COMMENT 'vm instance that needs flows to be synced.',
  `created` datetime NOT NULL COMMENT 'time the entry was requested',
  `logsequence` bigint unsigned  COMMENT 'seq number to be sent to agent, uniquely identifies flow update',
  `vm_name` varchar(255) NOT NULL COMMENT 'vm name',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ovs_work` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint unsigned NOT NULL COMMENT 'vm instance that needs rules to be synced.',
  `mgmt_server_id` bigint unsigned COMMENT 'management server that has taken up the work of doing rule sync',
  `created` datetime NOT NULL COMMENT 'time the entry was requested',
  `taken` datetime COMMENT 'time it was taken by the management server',
  `step` varchar(32) NOT NULL COMMENT 'Step in the work',
  `seq_no` bigint unsigned  COMMENT 'seq number to be sent to agent, uniquely identifies ruleset update',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ssh_keypairs` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner, foreign key to account table',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain, foreign key to domain table',
  `keypair_name` varchar(256) NOT NULL COMMENT 'name of the key pair',
  `fingerprint` varchar(128) NOT NULL COMMENT 'fingerprint for the ssh public key',
  `public_key` varchar(5120) NOT NULL COMMENT 'public key of the ssh key pair',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`user_vm_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
    
CREATE TABLE `cloud`.`cluster_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `cluster_id` bigint unsigned NOT NULL COMMENT 'cluster id',
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`host_tags` ADD CONSTRAINT `fk_host_tags__host_id` FOREIGN KEY `fk_host_tags__host_id`(`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `host_tag` varchar(255);


ALTER TABLE `cloud`.`op_it_work` change created created_at bigint unsigned NOT NULL COMMENT 'when was this work detail created';
ALTER TABLE `cloud`.`op_it_work` change state step char(32) NOT NULL COMMENT 'state';
ALTER TABLE `cloud`.`op_it_work` change cancel_taken updated_at bigint unsigned NOT NULL COMMENT 'time it was taken over';
ALTER TABLE `cloud`.`op_it_work` ADD COLUMN `instance_id` bigint unsigned NOT NULL COMMENT 'vm instance';                                                    
ALTER TABLE `cloud`.`op_it_work` ADD COLUMN `resource_id` bigint unsigned COMMENT 'resource id being worked on';                                             
ALTER TABLE `cloud`.`op_it_work` ADD COLUMN `resource_type` char(32) COMMENT 'type of resource being worked on';      
ALTER TABLE `cloud`.`hypervsior_properties` ADD COLUMN `is_default` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if network is default';
ALTER TABLE `cloud`.`network_offerings` drop column TYPE;
ALTER TABLE `cloud`.`domain_router` ADD COLUMN `host_tag` varchar(255) COMMENT 'host tag specified by the service_offering';
ALTER TABLE `cloud`.`usage_event` ADD COLUMN `processed` tinyint NOT NULL default '0';


ALTER TABLE `cloud`.`vm_instance` ADD  COLUMN `hypervisor_type` char(32) COMMENT 'hypervisor type';
ALTER TABLE `cloud`.`vm_instance` ADD  CONSTRAINT `fk_vm_instance__last_host_id` FOREIGN KEY `fk_vm_instance__last_host_id` (`last_host_id`) REFERENCES `host`(`id`);

ALTER TABLE `cloud`.`ssh_keypairs` ADD CONSTRAINT `fk_ssh_keypairs__account_id` FOREIGN KEY `fk_ssh_keypair__account_id` (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`ssh_keypairs` ADD CONSTRAINT `fk_ssh_keypairs__domain_id` FOREIGN KEY `fk_ssh_keypair__domain_id` (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`user_vm_details` ADD CONSTRAINT `fk_user_vm_details__vm_id` FOREIGN KEY `fk_user_vm_details__vm_id`(`vm_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`cluster_details` ADD CONSTRAINT `fk_cluster_details__cluster_id` FOREIGN KEY `fk_cluster_details__cluster_id`(`cluster_id`) REFERENCES `cluster`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`security_ingress_rule` DROP FOREIGN KEY  `fk_security_ingress_rule__security_group_id`;
ALTER TABLE `cloud`.`security_ingress_rule` DROP FOREIGN KEY `fk_security_ingress_rule__allowed_network_id`;
ALTER TABLE `cloud`.`security_group` DROP FOREIGN KEY `fk_security_group__account_id`;
ALTER TABLE `cloud`.`security_ingress_rule` ADD CONSTRAINT `fk_security_ingress_rule___security_group_id` FOREIGN KEY `fk_security_ingress_rule__security_group_id` (`security_group_id`) REFERENCES `security_group` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`security_ingress_rule` ADD CONSTRAINT `fk_security_ingress_rule___allowed_network_id` FOREIGN KEY `fk_security_ingress_rule__allowed_network_id` (`allowed_network_id`) REFERENCES `security_group` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`security_group` ADD CONSTRAINT `fk_security_group___account_id` FOREIGN KEY `fk_security_group__account_id` (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE;

DROP VIEW `cloud`.`user_ip_address_view`;
CREATE VIEW `cloud`.`user_ip_address_view` AS SELECT INET_NTOA(user_ip_address.public_ip_address) as ip_address, user_ip_address.data_center_id, user_ip_address.account_id, user_ip_address.domain_id, user_ip_address.source_nat, user_ip_address.allocated, user_ip_address.vlan_db_id, user_ip_address.one_to_one_nat, user_ip_address.state, user_ip_address.mac_address, user_ip_address.network_id as associated_network_id from user_ip_address;


