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
-- Schema upgrade from 4.1.0 to 4.2.0;
--;

ALTER TABLE `cloud`.`hypervisor_capabilities` ADD COLUMN `max_hosts_per_cluster` int unsigned DEFAULT NULL COMMENT 'Max. hosts in cluster supported by hypervisor';
UPDATE `cloud`.`hypervisor_capabilities` SET `max_hosts_per_cluster`=32 WHERE `hypervisor_type`='VMware';
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_hosts_per_cluster) VALUES ('VMware', '5.1', 128, 0, 32);
DELETE FROM `cloud`.`configuration` where name='vmware.percluster.host.max';
