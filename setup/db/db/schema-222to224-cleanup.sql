ALTER TABLE `cloud`.`account_vlan_map` DROP FOREIGN KEY `fk_account_vlan_map__domain_id`;
ALTER TABLE `cloud`.`account_vlan_map` DROP COLUMN `domain_id`;
DELETE FROM `cloud`.`account_vlan_map` WHERE account_id IS NULL;
ALTER TABLE `cloud`.`data_center` DROP COLUMN `enable`;
ALTER TABLE `cloud`.`host_pod_ref` DROP COLUMN `enabled`;
DELETE FROM `cloud`.`configuration` WHERE name in ('direct.attach.security.groups.enabled', 'direct.attach.untagged.vlan.enabled', 'hypervisor.type', 'management-server', 'max.volume.size.gb', 'multicast.throttling.rate', 'network.type', 'xen.preallocated.lun.size.range');
