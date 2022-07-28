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
-- Schema upgrade from 4.17.1.0 to 4.18.0.0
--;

-- Enable CPU cap for default system offerings;
UPDATE `cloud`.`service_offering` so
SET so.limit_cpu_use = 1
WHERE so.default_use = 1 AND so.vm_type IN ('domainrouter', 'secondarystoragevm', 'consoleproxy', 'internalloadbalancervm', 'elasticloadbalancervm');

-- VM autoscaling

-- Add column 'name' to 'autoscale_vmgroups' table

ALTER TABLE `cloud`.`autoscale_vmgroups` ADD COLUMN `name` varchar(255) DEFAULT NULL COMMENT 'name of the autoscale vm group' AFTER `load_balancer_id`;

UPDATE `cloud`.`autoscale_vmgroups` SET `name` = CONCAT('AutoScale-VmGroup-',id) WHERE `name` IS NULL;

-- Add column 'user_data' to 'autoscale_vmprofiles' table

ALTER TABLE `cloud`.`autoscale_vmprofiles` ADD COLUMN `user_data` TEXT(32768) AFTER `counter_params`;

-- Add column 'provider' and update values

ALTER TABLE `cloud`.`counter` ADD COLUMN `provider` varchar(255) NOT NULL COMMENT 'Network provider name' AFTER `uuid`;

UPDATE `cloud`.`counter` SET provider = 'Netscaler';

-- Add new counters for VM autoscaling

INSERT INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'cpu', 'VM CPU - average percentage', 'vm.cpu.average.percentage', NOW());
INSERT INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'memory', 'VM Memory - average percentage', 'vm.memory.average.percentage', NOW());
INSERT INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'virtualrouter', 'Virtual Network - Receive (in Bytes)', 'virtual.network.receive', NOW());
INSERT INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'virtualrouter', 'Virtual Network - Transmit (in Bytes)', 'virtual.network.transmit', NOW());
INSERT INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VirtualRouter', 'virtualrouter', 'Load Balancer - average connections per vm', 'virtual.network.lb.average.connections', NOW());

INSERT INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'cpu', 'VM CPU - average percentage', 'vm.cpu.average.percentage', NOW());
INSERT INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'memory', 'VM Memory - average percentage', 'vm.memory.average.percentage', NOW());
INSERT INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'virtualrouter', 'Virtual Network - Receive (in Bytes)', 'virtual.network.receive', NOW());
INSERT INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'virtualrouter', 'Virtual Network - Transmit (in Bytes)', 'virtual.network.transmit', NOW());
INSERT INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'VpcVirtualRouter', 'virtualrouter', 'Load Balancer - average connections per vm', 'virtual.network.lb.average.connections', NOW());

INSERT INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'None', 'cpu', 'VM CPU - average percentage', 'vm.cpu.average.percentage', NOW());
INSERT INTO `cloud`.`counter` (uuid, provider, source, name, value, created) VALUES (UUID(), 'None', 'memory', 'VM Memory - average percentage', 'vm.memory.average.percentage', NOW());

-- Update autoscale_vmgroups to new state

UPDATE `cloud`.`autoscale_vmgroups` SET state='New' WHERE state='new';
UPDATE `cloud`.`autoscale_vmgroups` SET state='Enabled' WHERE state='enabled';
UPDATE `cloud`.`autoscale_vmgroups` SET state='Disabled' WHERE state='disabled';
UPDATE `cloud`.`autoscale_vmgroups` SET state='Revoke' WHERE state='revoke';

-- Create table for VM autoscaling historic data

CREATE TABLE `cloud`.`autoscale_vmgroup_statistics` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vmgroup_id` bigint unsigned NOT NULL,
  `policy_id` bigint unsigned NOT NULL,
  `counter_id` bigint unsigned NOT NULL,
  `resource_id` bigint unsigned DEFAULT NULL,
  `resource_type` varchar(255) NOT NULL,
  `raw_value` double NOT NULL,
  `value_type` varchar(255) NOT NULL,
  `created` datetime NOT NULL COMMENT 'Date this data is created',
  `state` varchar(255) NOT NULL COMMENT 'State of the data',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_vmgroup_statistics__vmgroup_id` FOREIGN KEY `fk_autoscale_vmgroup_statistics__vmgroup_id` (`vmgroup_id`) REFERENCES `autoscale_vmgroups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmgroup_statistics__policy_id` FOREIGN KEY `fk_autoscale_vmgroup_statistics__policy_id` (`policy_id`) REFERENCES `autoscale_policies` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmgroup_statistics__counter_id` FOREIGN KEY `fk_autoscale_vmgroup_statistics__counter_id` (`counter_id`) REFERENCES `counter` (`id`),
  INDEX `i_autoscale_vmgroup_statistics__vmgroup_id`(`vmgroup_id`),
  INDEX `i_autoscale_vmgroup_statistics__counter_id`(`counter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
