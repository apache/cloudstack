--step 1
-- drop all constraints for user_ip_address
ALTER TABLE firewall_rules DROP foreign key fk_firewall_rules__ip_address ;
ALTER TABLE remote_access_vpn DROP foreign key fk_remote_access_vpn__server_addr ; 
ALTER TABLE user_ip_address DROP primary key;



--step 2
--schema+data changes
----------------------------------------user ip address table-------------------------------------------------------------------------
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `id` bigint unsigned NOT NULL auto_increment primary key;
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `source_network_id` bigint unsigned NOT NULL COMMENT 'network id ip belongs to';
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `vm_id` bigint unsigned NOT NULL COMMENT 'foreign key to virtual machine id';
ALTER TABLE `cloud`.`user_ip_address` ADD UNIQUE (source_network_id, public_ip_address);
UPDATE user_ip_address SET source_network_id=(select network_id from vlan where vlan.id=user_ip_address.vlan_db_id);
ALTER VIEW `cloud`.`user_ip_address_view` AS SELECT user_ip_address.id, user_ip_address.source_network_id,user_ip_address.vm_id,INET_NTOA(user_ip_address.public_ip_address) as ip_address, user_ip_address.data_center_id, user_ip_address.account_id, user_ip_address.domain_id, user_ip_address.source_nat, user_ip_address.allocated, user_ip_address.vlan_db_id, user_ip_address.one_to_one_nat, user_ip_address.state, user_ip_address.mac_address, user_ip_address.network_id as associated_network_id from user_ip_address; 

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
--done in the java layer
-- the updates the user ip address table with the vm id; using a 3 way join on firewall rules, user ip address, port forwarding tables
-- to do this, run Db22beta4to22GAMigrationUtil.java

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


--step 4 (independent of above)
----------------------usage changes (for cloud_usage database)--------------------------------------------------------------------------------------------------------------
ALTER TABLE `cloud_usage`.`user_statistics` DROP COLUMN host_id;
ALTER TABLE `cloud_usage`.`user_statistics` DROP COLUMN host_type;
ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `device_id` bigint unsigned NOT NULL;
ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `device_type` varchar(32) NOT NULL;
ALTER TABLE `cloud_usage`.`user_statistics` ADD UNIQUE (`account_id`, `data_center_id`, `device_id`, `device_type`);
