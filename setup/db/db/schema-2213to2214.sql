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
-- Schema upgrade from 2.2.13 to 2.2.14;
--;

ALTER TABLE `cloud`.`vm_template` MODIFY `extractable` int(1) unsigned NOT NULL default 0 COMMENT 'Is this template extractable';
INSERT IGNORE INTO `cloud`.`configuration` (category, instance, component, name, value, description) VALUES ('Advanced', 'DEFAULT', 'management-server', 'external.network.stats.interval', '300', 'Interval (in seconds) to report external network statistics.');

CREATE TABLE  `cloud`.`mshost_peer` (
  `id` bigint unsigned NOT NULL auto_increment,
  `owner_mshost` bigint unsigned NOT NULL,
  `peer_mshost` bigint unsigned NOT NULL,
  `peer_runid` bigint NOT NULL,
  `peer_state` varchar(10) NOT NULL DEFAULT 'Down',
  `last_update` DATETIME NULL COMMENT 'Last record update time',
  
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_mshost_peer__owner_mshost` FOREIGN KEY (`owner_mshost`) REFERENCES `mshost`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_mshost_peer__peer_mshost` FOREIGN KEY (`peer_mshost`) REFERENCES `mshost`(`id`),
  UNIQUE `i_mshost_peer__owner_peer_runid`(`owner_mshost`, `peer_mshost`, `peer_runid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO configuration (category, instance, component, name, value, description) VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.systemvm.nic.device.type', 'E1000', 'Specify the default network device type for system VMs, valid values are E1000, PCNet32, Vmxnet2, Vmxnet3');
INSERT IGNORE INTO configuration (category, instance, component, name, value, description) VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.recycle.hung.wokervm', 'false', 'Specify whether or not to recycle hung worker VMs');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'migratewait', '3600', 'Time (in seconds) to wait for VM migrate finish');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'sync.interval', '60', 'Cluster Delta sync interval in seconds');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'skip.steps', '60', 'Cluster full sync skip steps count');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'StorageManager', 'backup.snapshot.wait', '10800', 'In second, timeout for BackupSnapshotCommand');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'StorageManager', 'copy.volume.wait', '10800', 'In second, timeout for copy volume command');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'UserVmManager', 'create.private.template.from.snapshot.wait', '10800', 'In second, timeout for CreatePrivateTemplateFromSnapshotCommand');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'UserVmManager', 'create.private.template.from.volume.wait', '10800', 'In second, timeout for CreatePrivateTemplateFromVolumeCommand');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'StorageManager', 'create.volume.from.snapshot.wait', '10800', 'In second, timeout for create template from snapshot');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'TemplateManager', 'primary.storage.download.wait', '10800', 'In second, timeout for download template to primary storage');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Premium', 'DEFAULT', 'management-server', 'usage.execution.timezone', null, 'The timezone to use for usage job execution time');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Premium', 'DEFAULT', 'management-server', 'usage.stats.job.aggregation.range', '1440', 'The range of time for aggregating the user statistics specified in minutes (e.g. 1440 for daily, 60 for hourly.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Premium', 'DEFAULT', 'management-server', 'usage.stats.job.exec.time', '00:15', 'The time at which the usage statistics aggregation job will run as an HH24:MM time, e.g. 00:30 to run at 12:30am.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Premium', 'DEFAULT', 'management-server', 'enable.usage.server', 'true', 'Flag for enabling usage');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Premium', 'DEFAULT', 'management-server', 'direct.network.stats.interval', '86400', 'Interval (in seconds) to collect stats from Traffic Monitor');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Premium', 'DEFAULT', 'management-server', 'usage.aggregation.timezone', 'GMT', 'The timezone to use for usage stats aggregation');


INSERT IGNORE INTO `cloud`.`guest_os` (category_id, name, display_name) VALUES (6, NULL, "Windows PV");

CREATE TABLE `cloud`.`vm_template_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `template_id` bigint unsigned NOT NULL COMMENT 'template id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vm_template_details__template_id` FOREIGN KEY `fk_vm_template_details__template_id`(`template_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`domain_router` MODIFY `is_redundant_router` int(1) unsigned NOT NULL COMMENT 'if in redundant router mode';
ALTER TABLE `cloud`.`domain_router` MODIFY `is_priority_bumpup` int(1) unsigned NOT NULL COMMENT 'if the priority has been bumped up';
ALTER TABLE `cloud`.`domain_router` MODIFY `redundant_state` varchar(64) NOT NULL COMMENT 'the state of redundant virtual router';
ALTER TABLE `cloud`.`domain_router` MODIFY `stop_pending` int(1) unsigned NOT NULL COMMENT 'if this router would be stopped after we can connect to it';


ALTER TABLE `cloud`.`service_offering` MODIFY `limit_cpu_use` tinyint(1) unsigned NOT NULL default '0' COMMENT 'Limit the CPU usage to service offering';

ALTER TABLE `cloud`.`vm_instance` MODIFY `limit_cpu_use` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Limit the cpu usage to service offering';

UPDATE `cloud`.`configuration` SET `value`='false' WHERE `name`='agent.lb.enabled';


ALTER TABLE `cloud_usage`.`user_statistics` MODIFY `device_type` varchar(32) NOT NULL;

ALTER TABLE `cloud`.`keystore` ADD seq int;
ALTER TABLE `cloud`.`keystore` MODIFY `cloud`.`keystore`.`key` text;

