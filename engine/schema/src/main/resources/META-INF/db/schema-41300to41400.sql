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
-- Schema upgrade from 4.13.0.0 to 4.14.0.0
--;

-- KVM: enable storage data motion on KVM hypervisor_capabilities
UPDATE `cloud`.`hypervisor_capabilities` SET `storage_motion_supported` = 1 WHERE `hypervisor_capabilities`.`hypervisor_type` = 'KVM';

-- New builtin templates based on CentOS8, for VMware/KVM/XenServer
DELETE from `cloud`.`vm_template` where id in ('4', '5', '7');
INSERT INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, display_text, enable_password, format, guest_os_id, featured, cross_zones, hypervisor_type, extractable) VALUES (4, UUID(), 'default-tmpl-centos8.0-kvm', 'CentOS 8.0(64-bit) no GUI (KVM)', 1, now(), 'BUILTIN', 0, 64, 1, 'http://download.cloudstack.org/releases/4.14/default-tmpl-centos8.0.qcow2.bz2', 'f217c2b8d1e37b7dadea467d0fe14e61', 'CentOS 8.0(64-bit) no GUI (KVM)', 0, 'QCOW2', 274, 1, 1, 'KVM', 1);
INSERT INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text,  format, guest_os_id, featured, cross_zones, hypervisor_type, extractable) VALUES (5, UUID(), 'default-tmpl-centos8.0-xen', 'CentOS 8.0(64-bit) no GUI (XenServer)', 1, now(), 'BUILTIN', 0, 64, 1, 'http://download.cloudstack.org/releases/4.14/default-tmpl-centos8.0.vhd.bz2', '0816519171b836b2d2f3062971eaa921', 0, 'CentOS 8.0(64-bit) no GUI (XenServer)', 'VHD', 274, 1, 1, 'XenServer', 1);
INSERT INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text,  format, guest_os_id, featured, cross_zones, hypervisor_type, extractable) VALUES (7, UUID(), 'default-tmpl-centos8.0-vmware', 'CentOS 8.0(64-bit) no GUI (vSphere)', 1, now(), 'BUILTIN', 0, 64, 1, 'http://download.cloudstack.org/releases/4.14/default-tmpl-centos8.0.ova', '85168c007c6ab85c516274cdf23eda48', 0, 'CentOS 8.0(64-bit) no GUI (vSphere)', 'OVA', 274, 1, 1, 'VMware', 1);
UPDATE `cloud`.`vm_template` SET `state`="Active" WHERE id IN ('4', '5', '7');
INSERT INTO `cloud`.`vm_template_details` (`id`, `template_id`, `name`, `value`) VALUES (1, 7, 'nicAdapter', 'Vmxnet3');
INSERT INTO `cloud`.`vm_template_details` (`id`, `template_id`, `name`, `value`) VALUES (2, 7, 'rootDiskController', 'pvscsi');
