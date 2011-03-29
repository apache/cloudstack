ALTER TABLE `cloud`.`account_vlan_map` DROP FOREIGN KEY `fk_account_vlan_map__domain_id`;
ALTER TABLE `cloud`.`account_vlan_map` DROP COLUMN `domain_id`;
DELETE FROM `cloud`.`account_vlan_map` WHERE account_id IS NULL;
ALTER TABLE `cloud`.`data_center` DROP COLUMN `enable`;
ALTER TABLE `cloud`.`host_pod_ref` DROP COLUMN `enabled`;