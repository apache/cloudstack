alter table firewall_rules drop column is_static_nat;
delete from configuration where name='router.cleanup';

ALTER TABLE `cloud`.`console_proxy` DROP COLUMN `ram_size`;

ALTER TABLE `cloud`.`domain_router` DROP COLUMN `ram_size`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `domain`;
ALTER TABLE `cloud`.`domain_router` DROP COLUMN `guest_mac_address`;

ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `ram_size`;

ALTER TABLE `cloud`.`service_offering` DROP COLUMN `guest_ip_type`;

ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `mirrored_vols`;
