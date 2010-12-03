SET foreign_key_checks = 0;

--
-- Schema upgrade from 2.1 to 2.2
--
ALTER TABLE `cloud`.`template_host_ref` ADD COLUMN `physical_size` bigint unsigned NOT NULL DEFAULT 0
ALTER TABLE `cloud`.`snapshots` MODIFY COLUMN `id` bigint unsigned UNIQUE NOT NULL
ALTER TABLE `vm_instance` DROP COLUMN `group`
