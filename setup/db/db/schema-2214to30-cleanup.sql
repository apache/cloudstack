ALTER TABLE `cloud`.`template_host_ref` DROP COLUMN `pool_id`;
DELETE from `cloud`.`op_host_capacity` where capacity_type in (2,4,6);
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `net_bytes_received`;
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `net_bytes_sent`;
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `current_bytes_received`;
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `current_bytes_sent`;
ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `private_netmask`; 

ALTER TABLE `cloud`.`security_group_rule` drop foreign key `fk_security_ingress_rule___security_group_id`;
ALTER TABLE `cloud`.`security_group_rule` drop foreign key `fk_security_ingress_rule___allowed_network_id`;
ALTER TABLE `cloud`.`security_group_rule` drop index `i_security_ingress_rule_network_id`;
ALTER TABLE `cloud`.`security_group_rule` drop index `i_security_ingress_rule_allowed_network`;
ALTER TABLE `cloud`.`host` DROP COLUMN `allocation_state`;

ALTER TABLE `cloud`.`data_center` DROP COLUMN `vnet`;


