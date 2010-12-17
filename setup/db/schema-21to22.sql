SET foreign_key_checks = 0;

--
-- Schema upgrade from 2.1 to 2.2
--
ALTER TABLE `cloud`.`template_host_ref` ADD COLUMN `physical_size` bigint unsigned NOT NULL DEFAULT 0
ALTER TABLE `cloud`.`snapshots` MODIFY COLUMN `id` bigint unsigned UNIQUE NOT NULL
ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `group`
ALTER TABLE `cloud`.`cluster` ADD COLUMN `guid` varchar(255) UNIQUE DEFAULT NULL
ALTER TABLE `cloud`.`cluster` ADD COLUMN `cluster_type` varchar(64) DEFAULT 'CloudManaged'



-- NOTE for tables below
-- these 2 tables were used in 2.1, but are not in 2.2
-- we will need a migration script for these tables when the migration is written
-- furthermore we have renamed the following in 2.2
-- network_group table --> security_group table
-- network_group_vm_map table --> security_group_vm_map table
DROP TABLE `cloud`.`security_group`;
DROP TABLE `cloud`.`security_group_vm_map`;
