-- First the easy stuff.  Drop useless tables
DROP TABLE IF EXISTS `cloud`.`ext_lun_details`;
DROP TABLE IF EXISTS `cloud`.`ext_lun_alloc`;
DROP TABLE IF EXISTS `cloud`.`disk_template_ref`;

-- Then remove columns

ALTER TABLE `cloud`.`volumes` DROP COLUMN `mirror_state`;
ALTER TABLE `cloud`.`volumes` DROP COLUMN `mirror_vol`;
ALTER TABLE `cloud`.`volumes` DROP COLUMN `destroyed`;

DROP TABLE `cloud`.`ip_forwarding`;

ALTER TABLE `cloud`.`host` DROP COLUMN `sequence`;

DROP TABLE `cloud`.`op_vm_host`;

ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `iso_id`;
ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `display_name`;
ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `group`;
ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `storage_ip`;
ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `mirrored_vols`;

DROP TABLE `cloud`.`pricing`;

ALTER TABLE `cloud`.`user_vm` DROP FOREIGN KEY `fk_user_vm__domain_router_id`;
ALTER TABLE `cloud`.`user_vm` DROP INDEX `i_user_vm__domain_router_id`;
ALTER TABLE `cloud`.`user_vm` DROP FOREIGN KEY `fk_user_vm__service_offering_id`;
ALTER TABLE `cloud`.`user_vm` DROP INDEX `i_user_vm__service_offering_id`;
ALTER TABLE `cloud`.`user_vm` DROP FOREIGN KEY `fk_user_vm__account_id`;
ALTER TABLE `cloud`.`user_vm` DROP INDEX `i_user_vm__account_id`;
ALTER TABLE `cloud`.`user_vm` DROP FOREIGN KEY `fk_user_vm__external_ip_address`;
ALTER TABLE `cloud`.`user_vm` DROP INDEX `i_user_vm__external_ip_address`;
ALTER TABLE `cloud`.`user_vm` DROP FOREIGN KEY `fk_user_vm__external_vlan_db_id`;
ALTER TABLE `cloud`.`user_vm` DROP INDEX `i_user_vm__external_vlan_db_id`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `domain_router_id`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `service_offering_id`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `vnet`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `dc_vlan`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `account_id`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `domain_id`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `external_ip_address`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `external_mac_address`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `external_vlan_db_id`;

ALTER TABLE `cloud`.`domain_router` DROP FOREIGN KEY `fk_domain_router__account_id`;
ALTER TABLE `cloud`.`domain_router` DROP FOREIGN KEY `fk_domain_router__public_ip_address`;
ALTER TABLE `cloud`.`domain_router` DROP FOREIGN KEY `fk_domain_router__vlan_id`;
ALTER TABLE `cloud`.`domain_router` DROP INDEX `i_domain_router__account_id`;
ALTER TABLE `cloud`.`domain_router` DROP INDEX `i_domain_router__public_ip_address`;
ALTER TABLE `cloud`.`domain_router` DROP INDEX `i_domain_router__vlan_id`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `gateway`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `ram_size`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `dns1`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `dns2`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `guest_mac_address`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `guest_dc_mac_address`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `vnet`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `dc_vlan`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `vlan_db_id`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `vlan_id`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `domain_id`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `account_id`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `dhcp_ip_address`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `domain`;

ALTER TABLE `cloud`.`console_proxy` DROP FOREIGN KEY `fk_console_proxy__vlan_id`;
ALTER TABLE `cloud`.`console_proxy` DROP INDEX `i_console_proxy__vlan_id`;
ALTER TABLE `cloud`.`console_proxy` DROP COLUMN `gateway`;
ALTER TABLE `cloud`.`console_proxy` DROP COLUMN `dns1`;
ALTER TABLE `cloud`.`console_proxy` DROP COLUMN `dns2`;
ALTER TABLE `cloud`.`console_proxy` DROP COLUMN `domain`;
ALTER TABLE `cloud`.`console_proxy` DROP COLUMN `guest_mac_address`;
ALTER TABLE `cloud`.`console_proxy` DROP COLUMN `guest_ip_address`;
ALTER TABLE `cloud`.`console_proxy` DROP COLUMN `guest_netmask`;
ALTER TABLE `cloud`.`console_proxy` DROP COLUMN `vlan_db_id`;
ALTER TABLE `cloud`.`console_proxy` DROP COLUMN `vlan_id`;
ALTER TABLE `cloud`.`console_proxy` DROP COLUMN `ram_size`;

ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `gateway`;
ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `dns1`;
ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `dns2`;
ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `domain`;
ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `guest_mac_address`;
ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `guest_ip_address`;
ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `guest_netmask`;
ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `vlan_db_id`;
ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `vlan_id`;
ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `ram_size`;
#ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `guid`;

#ALTER TABLE `cloud`.`vlan` ADD CONSTRAINT `fk_vlan__network_id` FOREIGN KEY `fk_vlan__network_id`(`network_id`) REFERENCES `networks`(`id`);

DROP TABLE `cloud`.`vm_disk`;

ALTER TABLE `cloud`.`disk_offering` DROP COLUMN `mirrored`;

ALTER TABLE `cloud`.`service_offering` DROP COLUMN `guest_ip_type`;

ALTER TABLE `cloud`.`load_balancer_vm_map` DROP COLUMN `pending`;

ALTER TABLE `cloud`.`account_vlan_map` MODIFY COLUMN `account_id` bigint unsigned NOT NULL;

ALTER TABLE `cloud`.`load_balancer_vm_map` ADD UNIQUE KEY (`load_balancer_id`, `instance_id`);
ALTER TABLE `cloud`.`load_balancer_vm_map` ADD CONSTRAINT `fk_load_balancer_vm_map__load_balancer_id` FOREIGN KEY(`load_balancer_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`load_balancer_vm_map` ADD CONSTRAINT `fk_load_balancer_vm_map__instance_id` FOREIGN KEY(`instance_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE;


ALTER TABLE `cloud`.`user_ip_address` DROP PRIMARY KEY;
ALTER TABLE `cloud`.`user_ip_address` ADD PRIMARY KEY (`id`);
ALTER TABLE `cloud`.`user_ip_address` DROP INDEX public_ip_address;
ALTER TABLE `cloud`.`user_ip_address` DROP KEY `i_user_ip_address__public_ip_address`;
ALTER TABLE `cloud`.`user_ip_address` ADD UNIQUE (`public_ip_address`, `source_network_id`);
ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__source_network_id` FOREIGN KEY (`source_network_id`) REFERENCES `networks`(`id`);
ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`);

ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__account_id` FOREIGN KEY `fk_vm_instance__account_id` (`account_id`) REFERENCES `account` (`id`);
ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__service_offering_id` FOREIGN KEY `fk_vm_instance__service_offering_id` (`service_offering_id`) REFERENCES `service_offering` (`id`);

ALTER TABLE `cloud`.`template_spool_ref` ADD CONSTRAINT `fk_template_spool_ref__pool_id` FOREIGN KEY (`pool_id`) REFERENCES `storage_pool`(`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`volumes` MODIFY COLUMN `state` VARCHAR(32) NOT NULL;

