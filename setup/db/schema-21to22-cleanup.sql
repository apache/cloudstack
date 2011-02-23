-- First the easy stuff.  Drop useless tables
DROP TABLE IF EXISTS `cloud`.`ext_lun_details`;
DROP TABLE IF EXISTS `cloud`.`ext_lun_alloc`;
DROP TABLE IF EXISTS `cloud`.`disk_template_ref`;

-- Then remove columns

ALTER TABLE `cloud`.`volume` DROP COLUMN `mirror_state`;
ALTER TABLE `cloud`.`volume` DROP COLUMN `mirror_vol`;
ALTER TABLE `cloud`.`volume` DROP COLUMN `destroyed`;

DROP TABLE `cloud`.`ip_forwarding`;

ALTER TABLE `cloud`.`host` DROP COLUMN `sequence`;

DROP TABLE `cloud`,`op_vm_host`;

ALTER TABLE `cloud`.vm_instance` DROP COLUMN `iso_id`;
ALTER TABLE `cloud`.vm_instance` DROP COLUMN `display_name`;
ALTER TABLE `cloud`.vm_instance` DROP COLUMN `group`;
ALTER TABLE `cloud`.vm_instance` DROP COLUMN `storage_ip`;
ALTER TABLE `cloud`.vm_instance` DROP COLUMN `mirrored_vols`;

DROP TABLE `cloud`.`pricing`;

ALTER TABLE `cloud`.`user_vm` DROP COLUMN `domain_router_id`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `service_offering_id`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `vnet`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `dc_vlan`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `account_id`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `domain_id`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `external_ip_address`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `external_mac_address`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `external_vlan_db_id`;

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
ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `guid`;

DROP TABLE `cloud`.`vm_disk`;

ALTER TABLE `cloud`.`disk_offering` DROP COLUMN `mirrored`;

ALTER TABLE `cloud`.`service_offering` DROP COLUMN `guest_ip_type`;

DROP TABLE `cloud`.`security_group_vm_map`;
DROP TABLE `cloud`.`load_balancer_vm_map`;
DROP TABLE `cloud`.`security_group`;

ALTER TABLE `cloud`.`load_balancer_vm_map` DROP COLUMN `pending`;

