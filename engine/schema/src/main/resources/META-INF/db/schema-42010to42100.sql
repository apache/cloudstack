-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

--;
-- Schema upgrade from 4.20.1.0 to 4.21.0.0
--;

-- Add console_endpoint_creator_address column to cloud.console_session table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'console_endpoint_creator_address', 'VARCHAR(45)');

-- Add client_address column to cloud.console_session table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'client_address', 'VARCHAR(45)');

-- Allow default roles to use quotaCreditsList
INSERT INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'quotaCreditsList', permission, sort_order
FROM `cloud`.`role_permissions` rp
WHERE rp.rule = 'quotaStatement'
AND NOT EXISTS(SELECT 1 FROM cloud.role_permissions rp_ WHERE rp.role_id = rp_.role_id AND rp_.rule = 'quotaCreditsList');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host', 'last_mgmt_server_id', 'bigint unsigned DEFAULT NULL COMMENT "last management server this host is connected to" AFTER `mgmt_server_id`');

-- Disk controller mappings
CREATE TABLE IF NOT EXISTS `cloud`.`disk_controller_mapping` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `uuid` varchar(255) UNIQUE NOT NULL,
    `name` varchar(255) NOT NULL,
    `controller_reference` varchar(255) NOT NULL,
    `bus_name` varchar(255) NOT NULL,
    `hypervisor` varchar(40) NOT NULL,
    `max_device_count` bigint unsigned DEFAULT NULL,
    `max_controller_count` bigint unsigned DEFAULT NULL,
    `vmdk_adapter_type` varchar(255) DEFAULT NULL,
    `min_hardware_version` varchar(20) DEFAULT NULL,
    `created` datetime NOT NULL,
    `removed` datetime DEFAULT NULL,
    PRIMARY KEY (`id`)
    );

-- Add VMware's default disk controller mappings
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('osdefault', 'unused', 'unused', 'VMware', NULL, NULL, NULL, NULL);
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('ide', 'com.vmware.vim25.VirtualIDEController', 'ide', 'VMware', 2, 2, 'ide', NULL);
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('scsi', 'com.vmware.vim25.VirtualLsiLogicController', 'scsi', 'VMware', 16, 4, 'lsilogic', NULL);
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('buslogic', 'com.vmware.vim25.VirtualBusLogicController', 'scsi', 'VMware', 16, 4, 'buslogic', NULL);
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('lsilogic', 'com.vmware.vim25.VirtualLsiLogicController', 'scsi', 'VMware', 16, 4, 'lsilogic', NULL);
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('lsisas1068', 'com.vmware.vim25.VirtualLsiLogicSASController', 'scsi', 'VMware', 16, 4, 'lsilogic', NULL);
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('pvscsi', 'com.vmware.vim25.ParaVirtualSCSIController', 'scsi', 'VMware', 16, 4, 'lsilogic', '7');
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('sata', 'com.vmware.vim25.VirtualAHCIController', 'sata', 'VMware', 30, 4, 'ide', '10');
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('ahci', 'com.vmware.vim25.VirtualAHCIController', 'sata', 'VMware', 30, 4, 'ide', '10');
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('nvme', 'com.vmware.vim25.VirtualNVMEController', 'nvme', 'VMware', 15, 4, 'ide', '13');
