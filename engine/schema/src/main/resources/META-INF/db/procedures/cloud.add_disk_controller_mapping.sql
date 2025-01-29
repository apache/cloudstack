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

DROP PROCEDURE IF EXISTS `cloud`.`ADD_DISK_CONTROLLER_MAPPING`;

CREATE PROCEDURE `cloud`.`ADD_DISK_CONTROLLER_MAPPING` (
    IN disk_controller_name varchar(255),
    IN disk_controller_reference varchar(255),
    IN disk_controller_bus_name varchar(255),
    IN disk_controller_hypervisor varchar(40),
    IN disk_controller_max_device_count bigint unsigned,
    IN disk_controller_max_controller_count bigint unsigned,
    IN disk_controller_vmdk_adapter_type varchar(255),
    IN disk_controller_min_hardware_version varchar(20)
)
BEGIN
INSERT INTO cloud.disk_controller_mapping (uuid, name, controller_reference, bus_name, hypervisor, max_device_count,
                                           max_controller_count, vmdk_adapter_type, min_hardware_version, created)
SELECT UUID(), disk_controller_name, disk_controller_reference, disk_controller_bus_name, disk_controller_hypervisor,
       disk_controller_max_device_count, disk_controller_max_controller_count, disk_controller_vmdk_adapter_type,
       disk_controller_min_hardware_version, now()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cloud.disk_controller_mapping WHERE cloud.disk_controller_mapping.name = disk_controller_name
                                                                AND cloud.disk_controller_mapping.hypervisor = disk_controller_hypervisor)
;END;
