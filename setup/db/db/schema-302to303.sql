# Copyright 2012 Citrix Systems, Inc. Licensed under the
# Apache License, Version 2.0 (the "License"); you may not use this
# file except in compliance with the License.  Citrix Systems, Inc.
# reserves all rights not expressly granted by the License.
# You may obtain a copy of the License at http:#www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 


#Schema upgrade from 3.0.2 to 3.0.3;

DELETE FROM `cloud`.`configuration` WHERE name='consoleproxy.cpu.mhz';
DELETE FROM `cloud`.`configuration` WHERE name='secstorage.vm.cpu.mhz';
DELETE FROM `cloud`.`configuration` WHERE name='consoleproxy.ram.size';
DELETE FROM `cloud`.`configuration` WHERE name='secstorage.vm.ram.size';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'consoleproxy.service.offering', NULL, 'Service offering used by console proxy; if NULL - system offering will be used');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'secstorage.service.offering', NULL, 'Service offering used by secondary storage; if NULL - system offering will be used');

ALTER TABLE `cloud`.`user_vm` ADD COLUMN `update_parameters` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Defines if the parameters need to be set for the vm';
UPDATE `cloud`.`user_vm` SET update_parameters=0 where id>0;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'ha.tag', NULL, 'HA tag defining that the host marked with this tag can be used for HA purposes only');

CREATE TABLE  `cloud`.`volume_host_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `host_id` bigint unsigned NOT NULL,
  `volume_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `job_id` varchar(255),
  `download_pct` int(10) unsigned,
  `size` bigint unsigned,
  `physical_size` bigint unsigned DEFAULT 0,
  `download_state` varchar(255),
  `checksum` varchar(255) COMMENT 'checksum for the data disk',
  `error_str` varchar(255),
  `local_path` varchar(255),
  `install_path` varchar(255),
  `url` varchar(255),
  `format` varchar(32) NOT NULL COMMENT 'format for the volume', 
  `destroyed` tinyint(1) COMMENT 'indicates whether the volume_host entry was destroyed by the user or not',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_volume_host_ref__host_id` FOREIGN KEY `fk_volume_host_ref__host_id` (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE,
  INDEX `i_volume_host_ref__host_id`(`host_id`),
  CONSTRAINT `fk_volume_host_ref__volume_id` FOREIGN KEY `fk_volume_host_ref__volume_id` (`volume_id`) REFERENCES `volumes` (`id`),
  INDEX `i_volume_host_ref__volume_id`(`volume_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`disk_offering` (name, display_text, customized, unique_name, disk_size, system_use) VALUES ( "Custom", "Custom Disk", 1, "Cloud.com-Custom", 0, 1);

