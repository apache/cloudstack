ALTER TABLE `cloud`.`account_vlan_map` modify `account_id` bigint unsigned default null;


ALTER TABLE `cloud`.`account_vlan_map` ADD COLUMN `domain_id` bigint unsigned COMMENT 'domain id. foreign key to domain table';
ALTER TABLE `cloud`.`account_vlan_map` ADD CONSTRAINT `fk_account_vlan_map__domain_id` FOREIGN KEY `fk_account_vlan_map__domain_id` (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`account_vlan_map` ADD INDEX `i_account_vlan_map__domain_id`(`domain_id`);

CREATE TABLE `cloud`.`version` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `version` char(40) NOT NULL UNIQUE COMMENT 'version',
  `updated` datetime NOT NULL COMMENT 'Date this version table was updated',
  `step` char(32) NOT NULL COMMENT 'Step in the upgrade to this version',
  PRIMARY KEY (`id`),
  INDEX `i_version__version`(`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;