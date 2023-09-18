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
-- Schema upgrade from 4.18.0.0 to 4.18.1.0
--;

-- Add support for VMware 8.0u1 (8.0.1.x)
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '8.0.1', 1024, 0, 59, 64, 1, 1);

-- Update conserve_mode of the default network offering for Tungsten Fabric (this fixes issue #7241)
UPDATE `cloud`.`network_offerings` SET conserve_mode = 0 WHERE unique_name ='DefaultTungstenFarbicNetworkOffering';

-- Add Windows Server 2022 guest OS and mappings
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'KVM', 'default', 'Windows Server 2022 (64-bit)');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '7.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '7.0.1.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '7.0.2.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '7.0.3.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '8.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '8.0.0.1', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'Xenserver', '8.2.0', 'Windows Server 2022 (64-bit)');

-- Support userdata ids and details in VM AutoScaling
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.autoscale_vmprofiles', 'user_data_id', 'bigint unsigned DEFAULT NULL COMMENT "id of the user data" AFTER `user_data`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.autoscale_vmprofiles', 'user_data_details', 'mediumtext DEFAULT NULL COMMENT "value of the comma-separated list of parameters" AFTER `user_data_id`');

-- Don't enable CPU cap for default system offerings, fixes regression from https://github.com/apache/cloudstack/pull/6420
UPDATE `cloud`.`service_offering` so
SET so.limit_cpu_use = 0
WHERE so.default_use = 1 AND so.vm_type IN ('domainrouter', 'secondarystoragevm', 'consoleproxy', 'internalloadbalancervm', 'elasticloadbalancervm');

-- fix erronous commas in guest_os names
UPDATE `cloud`.`guest_os_hypervisor` SET guest_os_name = 'rhel9_64Guest' WHERE guest_os_name = 'rhel9_64Guest,';
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.guest_os', 'display', 'tinyint(1) DEFAULT ''1'' COMMENT ''should this guest_os be shown to the end user'' ');
