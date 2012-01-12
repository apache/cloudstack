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
