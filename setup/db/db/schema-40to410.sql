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
-- Schema upgrade from 4.0.0 to 4.1.0;
--;

CREATE TABLE `cloud`.`s3` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `access_key` varchar(20) NOT NULL COMMENT ' The S3 access key',
  `secret_key` varchar(40) NOT NULL COMMENT ' The S3 secret key',
  `end_point` varchar(1024) COMMENT ' The S3 host',
  `bucket` varchar(63) NOT NULL COMMENT ' The S3 host',
  `https` tinyint unsigned DEFAULT NULL COMMENT ' Flag indicating whether or not to connect over HTTPS',
  `connection_timeout` integer COMMENT ' The amount of time to wait (in milliseconds) when initially establishing a connection before giving up and timing out.',
  `max_error_retry` integer  COMMENT ' The maximum number of retry attempts for failed retryable requests (ex: 5xx error responses from services).',
  `socket_timeout` integer COMMENT ' The amount of time to wait (in milliseconds) for data to be transfered over an established, open connection before the connection times out and is closed.',
  `created` datetime COMMENT 'date the s3 first signed on',
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_s3__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`template_s3_ref` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `s3_id` bigint unsigned NOT NULL COMMENT ' Associated S3 instance id',
  `template_id` bigint unsigned NOT NULL COMMENT ' Associated template id',
  `created` DATETIME NOT NULL COMMENT ' The creation timestamp',
  `size` bigint unsigned COMMENT ' The size of the object',
  `physical_size` bigint unsigned DEFAULT 0 COMMENT ' The physical size of the object',
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_template_s3_ref__template_id` UNIQUE (`template_id`),
  CONSTRAINT `fk_template_s3_ref__s3_id` FOREIGN KEY `fk_template_s3_ref__s3_id` (`s3_id`) REFERENCES `s3` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_template_s3_ref__template_id` FOREIGN KEY `fk_template_s3_ref__template_id` (`template_id`) REFERENCES `vm_template` (`id`),
  INDEX `i_template_s3_ref__swift_id`(`s3_id`),
  INDEX `i_template_s3_ref__template_id`(`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 's3.enable', 'false', 'enable s3');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'router.check.poolsize' , '10', 'Numbers of threads using to check redundant router status.');

ALTER TABLE `cloud`.`snapshots` ADD COLUMN `s3_id` bigint unsigned COMMENT 'S3 to which this snapshot will be stored';

ALTER TABLE `cloud`.`snapshots` ADD CONSTRAINT `fk_snapshots__s3_id` FOREIGN KEY `fk_snapshots__s3_id` (`s3_id`) REFERENCES `s3` (`id`);

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `eip_associate_public_ip` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if public IP is associated with user VM creation by default when EIP service is enabled.' AFTER `elastic_ip_service`;
ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `inline` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is this network offering LB provider is in inline mode';

ALTER TABLE `cloud`.`external_load_balancer_devices` DROP COLUMN `is_inline`;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network','DEFAULT','NetworkManager','network.dhcp.nondefaultnetwork.setgateway.guestos','Windows','The guest OS\'s name start with this fields would result in DHCP server response gateway information even when the network it\'s on is not default network. Names are separated by comma.');

ALTER TABLE `sync_queue` ADD `queue_size` SMALLINT NOT NULL DEFAULT '0' COMMENT 'number of items being processed by the queue';

ALTER TABLE `sync_queue` ADD `queue_size_limit` SMALLINT NOT NULL DEFAULT '1' COMMENT 'max number of items the queue can process concurrently';

ALTER TABLE `sync_queue_item` ADD `queue_proc_time` DATETIME NOT NULL COMMENT 'when processing started for the item' AFTER `queue_proc_number`;

ALTER TABLE `cloud`.`inline_load_balancer_nic_map` DROP FOREIGN KEY fk_inline_load_balancer_nic_map__load_balancer_id;

ALTER TABLE `cloud`.`inline_load_balancer_nic_map` DROP COLUMN load_balancer_id;
