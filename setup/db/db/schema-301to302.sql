# Copyright 2012 Citrix Systems, Inc. Licensed under the
# Apache License, Version 2.0 (the "License"); you may not use this
# file except in compliance with the License.  Citrix Systems, Inc.
# reserves all rights not expressly granted by the License.
# You may obtain a copy of the License at http:#www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 


#Schema upgrade from 3.0.1 to 3.0.2;

DELETE FROM `cloud`.`configuration` WHERE name='consoleproxy.cpu.mhz';
DELETE FROM `cloud`.`configuration` WHERE name='secstorage.vm.cpu.mhz';
DELETE FROM `cloud`.`configuration` WHERE name='consoleproxy.ram.size';
DELETE FROM `cloud`.`configuration` WHERE name='secstorage.vm.ram.size';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'consoleproxy.service.offering', NULL, 'Service offering used by console proxy; if NULL - system offering will be used');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'secstorage.service.offering', NULL, 'Service offering used by secondary storage; if NULL - system offering will be used');

DROP INDEX `i_host__allocation_state` ON `cloud`.`host`;

UPDATE `cloud`.`network_offerings` SET display_text='Offering for Isolated networks with Source Nat service enabled' WHERE name='DefaultIsolatedNetworkOfferingWithSourceNatService' and `cloud`.`network_offerings`.default=1;
UPDATE `cloud`.`network_offerings` SET display_text='Offering for Isolated networks with no Source Nat service' WHERE name='DefaultIsolatedNetworkOffering' and `cloud`.`network_offerings`.default=1;
UPDATE `cloud`.`network_offerings` SET display_text='Offering for Shared networks' WHERE name='DefaultSharedNetworkOffering' and `cloud`.`network_offerings`.default=1;



ALTER TABLE `cloud`.`op_lock` ENGINE=MEMORY, ROW_FORMAT = FIXED;
ALTER TABLE `cloud`.`op_nwgrp_work` ENGINE=MEMORY, ROW_FORMAT = FIXED;

INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('XenServer', '6.0.2', 50, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('VMware', '5.0', 128, 0);