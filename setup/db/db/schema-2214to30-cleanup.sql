# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 

ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `net_bytes_received`;
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `net_bytes_sent`;
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `current_bytes_received`;
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `current_bytes_sent`;

ALTER TABLE `cloud`.`template_host_ref` DROP COLUMN `pool_id`;
DELETE from `cloud`.`op_host_capacity` where capacity_type in (2,4,6);
ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `private_netmask`; 

ALTER TABLE `cloud`.`security_group_rule` drop foreign key `fk_security_ingress_rule___security_group_id`;
ALTER TABLE `cloud`.`security_group_rule` drop foreign key `fk_security_ingress_rule___allowed_network_id`;
ALTER TABLE `cloud`.`security_group_rule` drop index `i_security_ingress_rule_network_id`;
ALTER TABLE `cloud`.`security_group_rule` drop index `i_security_ingress_rule_allowed_network`;
ALTER TABLE `cloud`.`host` DROP COLUMN `allocation_state`;

ALTER TABLE `cloud`.`data_center` DROP COLUMN `vnet`;

ALTER TABLE `cloud`.`network_offerings` DROP COLUMN `dns_service`;
ALTER TABLE `cloud`.`network_offerings` DROP COLUMN `gateway_service`;
ALTER TABLE `cloud`.`network_offerings` DROP COLUMN `firewall_service`;
ALTER TABLE `cloud`.`network_offerings` DROP COLUMN `lb_service`;
ALTER TABLE `cloud`.`network_offerings` DROP COLUMN `userdata_service`;
ALTER TABLE `cloud`.`network_offerings` DROP COLUMN `vpn_service`;
ALTER TABLE `cloud`.`network_offerings` DROP COLUMN `dhcp_service`;


ALTER TABLE `cloud`.`networks` DROP COLUMN `shared`;
ALTER TABLE `cloud`.`networks` DROP COLUMN `is_domain_specific`;
ALTER TABLE `cloud`.`networks` DROP COLUMN `is_security_group_enabled`;
ALTER TABLE `cloud`.`networks` DROP COLUMN `is_default`;

UPDATE `cloud`.`networks` SET guest_type=(SELECT guest_type FROM network_offerings no where no.id=network_offering_id);
UPDATE `cloud`.`networks` SET guru_name='ExternalGuestNetworkGuru' where guest_type='Isolated';

DELETE FROM `cloud`.`configuration` WHERE name='use.user.concentrated.pod.allocation';

UPDATE `cloud`.`domain_router` SET role='VIRTUAL_ROUTER' WHERE role = 'DHCP_FIREWALL_LB_PASSWD_USERDATA' or role = 'DHCP_USERDATA';
ALTER TABLE `cloud`.`domain_router` ADD CONSTRAINT `fk_domain_router__element_id` FOREIGN KEY `fk_domain_router__element_id`(`element_id`) REFERENCES `virtual_router_providers`(`id`);
ALTER TABLE `cloud`.`vlan` ADD CONSTRAINT `fk_vlan__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`);
ALTER TABLE `cloud`.`op_dc_vnet_alloc` ADD CONSTRAINT `fk_op_dc_vnet_alloc__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`volumes` ADD INDEX `i_volumes__last_pool_id`(`last_pool_id`);
ALTER TABLE `cloud`.`swift` MODIFY `account` varchar(255) NOT NULL;
ALTER TABLE `cloud`.`swift` MODIFY `username` varchar(255) NOT NULL;

DROP TABLE IF EXISTS `cloud`.`network_tags`;
DROP TABLE IF EXISTS `cloud_usage`.`event`;

DELETE from `cloud`.`guest_os` where id=204 or id=205;

