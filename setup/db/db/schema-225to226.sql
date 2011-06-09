--;
-- Schema upgrade from 2.2.5 to 2.2.6;
--;

ALTER TABLE `cloud`.`storage_pool` MODIFY `host_address` varchar(255) NOT NULL;

ALTER TABLE `cloud`.`network_offerings` MODIFY `name` varchar(64) NOT NULL COMMENT 'name of the network offering';
ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `unique_name` varchar(64) NOT NULL COMMENT 'unique name of the network offering';
UPDATE `cloud`.`network_offerings` SET unique_name=name;
ALTER TABLE `cloud`.`network_offerings` MODIFY `unique_name` varchar(64) NOT NULL UNIQUE COMMENT 'unique name of the network offering';



