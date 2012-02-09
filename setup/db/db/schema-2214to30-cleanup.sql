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