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
-- Schema upgrade from 4.9.0 to 4.9.1;
--;

-- Fix default user role description
UPDATE `cloud`.`roles` SET `description`='Default user role' WHERE `id`=4 AND `role_type`='User' AND `description`='Default Root Admin role';

-- Fix mysql engine to innodb
ALTER TABLE cloud.load_balancer_cert_map ENGINE=INNODB;
ALTER TABLE cloud.monitoring_services ENGINE=INNODB;
ALTER TABLE cloud.nic_ip_alias ENGINE=INNODB;
ALTER TABLE cloud.sslcerts ENGINE=INNODB;
ALTER TABLE cloud.op_lock ENGINE=INNODB;
ALTER TABLE cloud.op_nwgrp_work ENGINE=INNODB;
ALTER TABLE cloud_usage.quota_account ENGINE=INNODB;

-- Add Ubuntu 16.04 LTS as support guest os
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (255, UUID(), 10, 'Ubuntu 16.04 (32-bit)', utc_timestamp());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (256, UUID(), 10, 'Ubuntu 16.04 (64-bit)', utc_timestamp());
-- Ubuntu 16.04 VMware guest os mapping
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '5.0', 'ubuntuGuest', 255, utc_timestamp(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '5.1', 'ubuntuGuest', 255, utc_timestamp(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '5.5', 'ubuntuGuest', 255, utc_timestamp(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '5.0', 'ubuntu64Guest', 256, utc_timestamp(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '5.1', 'ubuntu64Guest', 256, utc_timestamp(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '5.5', 'ubuntu64Guest', 256, utc_timestamp(), 0);
-- Ubuntu 16.04 KVM guest os mapping
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'KVM', 'default', 'Ubuntu 16.04', 255, utc_timestamp(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'KVM', 'default', 'Ubuntu 16.04', 256, utc_timestamp(), 0);
-- Ubuntu 16.04 LXC guest os mapping
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'LXC', 'default', 'Ubuntu 16.04', 255, utc_timestamp(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'LXC', 'default', 'Ubuntu 16.04', 256, utc_timestamp(), 0);
-- Ubuntu 16.04 XenServer guest os mapping
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '6.5.0', 'Ubuntu Trusty Tahr 14.04', 255, utc_timestamp(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '6.5.0', 'Ubuntu Trusty Tahr 14.04', 256, utc_timestamp(), 0);
