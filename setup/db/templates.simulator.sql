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


INSERT INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id, featured, cross_zones, hypervisor_type, image_data_store_id)
    VALUES (100, UUID(), 'simulator-domR', 'SystemVM Template (simulator)', 0, now(), 'SYSTEM', 0, 64, 1, 'http://download.cloud.com/templates/acton/acton-systemvm-02062012.vhd.bz2', '', 0, 'SystemVM Template (simulator)', 'VHD', 15, 0, 1, 'Simulator', 1);
INSERT INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text,  format, guest_os_id, featured, cross_zones, hypervisor_type, image_data_store_id)
    VALUES (111, UUID(), 'simulator-Centos', 'CentOS 5.3(64-bit) no GUI (Simulator)', 1, now(), 'BUILTIN', 0, 64, 1, 'http://download.cloud.com/templates/builtin/f59f18fb-ae94-4f97-afd2-f84755767aca.vhd.bz2', '', 0, 'CentOS 5.3(64-bit) no GUI (Simulator)', 'VHD', 12, 1, 1, 'Simulator', 1);
