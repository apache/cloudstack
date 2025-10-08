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
-- Schema upgrade from 4.21.0.0 to 4.22.0.0
--;


-- health check status as enum
CALL `cloud`.`IDEMPOTENT_CHANGE_COLUMN`('router_health_check', 'check_result', 'check_result', 'varchar(16) NOT NULL COMMENT "check executions result: SUCCESS, FAILURE, WARNING, UNKNOWN"');

-- Increase length of scripts_version column to 128 due to md5sum to sha512sum change
CALL `cloud`.`IDEMPOTENT_CHANGE_COLUMN`('cloud.domain_router', 'scripts_version', 'scripts_version', 'VARCHAR(128)');

-- Add uuid column to ldap_configuration table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.ldap_configuration', 'uuid', 'VARCHAR(40) NOT NULL');

-- Populate uuid for existing rows where uuid is NULL or empty
UPDATE `cloud`.`ldap_configuration` SET uuid = UUID() WHERE uuid IS NULL OR uuid = '';

-- Add the column cross_zone_instance_creation to cloud.backup_repository. if enabled it means that new Instance can be created on all Zones from Backups on this Repository.
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_repository', 'cross_zone_instance_creation', 'TINYINT(1) DEFAULT NULL COMMENT ''Backup Repository can be used for disaster recovery on another zone''');

-- Updated display to false for password/token detail of the storage pool details
UPDATE `cloud`.`storage_pool_details` SET display = 0 WHERE name LIKE '%password%';
UPDATE `cloud`.`storage_pool_details` SET display = 0 WHERE name LIKE '%token%';

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
