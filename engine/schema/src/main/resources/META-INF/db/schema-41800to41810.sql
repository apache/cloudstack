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

-- create_public_parameter_on_roles. #6960
ALTER TABLE `cloud`.`roles` ADD COLUMN `public_role` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Indicates whether the role will be visible to all users (public) or only to root admins (private). If this parameter is not specified during the creation of the role its value will be defaulted to true (public).';

-- Add Windows Server 2022 guest OS and mappings
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'KVM', 'default', 'Windows Server 2022 (64-bit)');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '7.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '7.0.1.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '7.0.2.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '7.0.3.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '8.0', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'VMware', '8.0.0.1', 'windows2019srvNext_64Guest');
CALL ADD_GUEST_OS_AND_HYPERVISOR_MAPPING (6, 'Windows Server 2022 (64-bit)', 'Xenserver', '8.2.0', 'Windows Server 2022 (64-bit)');
