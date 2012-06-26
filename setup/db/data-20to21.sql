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


use cloud;

--
-- We don't want to mess up with customer's data, all data updates are wrapped in a big transactions
-- hopefully we don't have a huge data set to deal with
--
START TRANSACTION;

UPDATE service_offering SET guest_ip_type='VirtualNetwork';
UPDATE vlan SET vlan_type='VirtualNetwork';

INSERT INTO configuration (`category`, `instance`, `component`, `name`, `value`, `description`) VALUES ('Advanced', 'DEFAULT', 'management-server', 'linkLocalIp.nums', '10', 'The number of link local ip that needed by domR(in power of 2)'); 
UPDATE host SET resource='com.cloud.hypervisor.xen.resource.XenServer56Resource' WHERE resource='com.cloud.resource.xen.XenServer56Resource';

--
-- Delete orphan records to deal with DELETE ON CASCADE missing in following two tables 
--
DELETE FROM console_proxy where id NOT IN (SELECT id FROM vm_instance WHERE type='ConsoleProxy');
DELETE FROM secondary_storage_vm where id NOT IN (SELECT id FROM vm_instance WHERE type='SecondaryStorageVm');

COMMIT;
