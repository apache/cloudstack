-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliances
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

#This file doesn't exist on 3.0.x branch. The fake template record is being inserted just because this template will never be used in 4.2 version of the code
INSERT IGNORE INTO `cloud`.`vm_template` (unique_name, name, public, featured, type, hvm, bits, url, format, created, account_id, checksum, display_text, enable_password, 
  enable_sshkey, guest_os_id, bootable, prepopulate, cross_zones, extractable, hypervisor_type, source_template_id, template_tag, sort_key)
VALUES ("systemvm-vmware-3.0.5", "systemvm-vmware-3.0.5", 1, 1, "USER", 0, 64, "fake url", "fake format", now(), 1, null, "fake text", 0,
 0, 99, 1, 0, 1, 0, "VMware", NULL, NULL, 0);
