ALTER TABLE `cloud`.`account_vlan_map` modify `account_id` bigint unsigned default null;


ALTER TABLE `cloud`.`account_vlan_map` ADD COLUMN `domain_id` bigint unsigned COMMENT 'domain id. foreign key to domain table';
ALTER TABLE `cloud`.`account_vlan_map` ADD CONSTRAINT `fk_account_vlan_map__domain_id` FOREIGN KEY `fk_account_vlan_map__domain_id` (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`account_vlan_map` ADD INDEX `i_account_vlan_map__domain_id`(`domain_id`);
