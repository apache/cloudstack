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
-- Schema upgrade from 4.19.0.0 to 4.20.0.0
--;

-- Add tag column to tables
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_limit', 'tag', 'varchar(64) DEFAULT NULL COMMENT "tag for the limit" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_count', 'tag', 'varchar(64) DEFAULT NULL COMMENT "tag for the resource count" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_reservation', 'tag', 'varchar(64) DEFAULT NULL COMMENT "tag for the resource reservation" ');
CALL `cloud`.`IDEMPOTENT_DROP_INDEX`('i_resource_count__type_accountId', 'cloud.resource_count');
CALL `cloud`.`IDEMPOTENT_DROP_INDEX`('i_resource_count__type_domaintId', 'cloud.resource_count');

DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_UNIQUE_INDEX`;
CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_UNIQUE_INDEX` (
    IN in_table_name VARCHAR(200),
    IN in_index_name VARCHAR(200),
    IN in_index_definition VARCHAR(1000)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1061 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name, ' ', 'ADD UNIQUE INDEX ', in_index_name, ' ', in_index_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

CALL `cloud`.`IDEMPOTENT_ADD_UNIQUE_INDEX`('cloud.resource_count', 'i_resource_count__type_tag_accountId', '(type, tag, account_id)');
CALL `cloud`.`IDEMPOTENT_ADD_UNIQUE_INDEX`('cloud.resource_count', 'i_resource_count__type_tag_domainId', '(type, tag, domain_id)');


ALTER TABLE `cloud`.`resource_reservation`
    ADD COLUMN `resource_id` bigint unsigned NULL;

ALTER TABLE `cloud`.`resource_reservation`
    MODIFY COLUMN `amount` bigint NOT NULL;


-- Update Default System offering for Router to 512MiB
UPDATE `cloud`.`service_offering` SET ram_size = 512 WHERE unique_name IN ("Cloud.Com-SoftwareRouter", "Cloud.Com-SoftwareRouter-Local",
                                                                           "Cloud.Com-InternalLBVm", "Cloud.Com-InternalLBVm-Local",
                                                                           "Cloud.Com-ElasticLBVm", "Cloud.Com-ElasticLBVm-Local")
                                                       AND system_use = 1 AND ram_size < 512;

-- NSX Plugin --
CREATE TABLE `cloud`.`nsx_providers` (
                                         `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
                                         `uuid` varchar(40),
                                         `zone_id` bigint unsigned NOT NULL COMMENT 'Zone ID',
                                         `host_id` bigint unsigned NOT NULL COMMENT 'Host ID',
                                         `provider_name` varchar(40),
                                         `hostname` varchar(255) NOT NULL,
                                         `port` varchar(255),
                                         `username` varchar(255) NOT NULL,
                                         `password` varchar(255) NOT NULL,
                                         `tier0_gateway` varchar(255),
                                         `edge_cluster` varchar(255),
                                         `transport_zone` varchar(255),
                                         `created` datetime NOT NULL COMMENT 'date created',
                                         `removed` datetime COMMENT 'date removed if not null',
                                         PRIMARY KEY (`id`),
                                         CONSTRAINT `fk_nsx_providers__zone_id` FOREIGN KEY `fk_nsx_providers__zone_id` (`zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE,
                                         INDEX `i_nsx_providers__zone_id`(`zone_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- NSX Plugin --
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','for_nsx', 'int(1) unsigned DEFAULT "0" COMMENT "is nsx enabled for the resource"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings','nsx_mode', 'varchar(32) COMMENT "mode in which the network would route traffic"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc_offerings','for_nsx', 'int(1) unsigned DEFAULT "0" COMMENT "is nsx enabled for the resource"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc_offerings','nsx_mode', 'varchar(32) COMMENT "mode in which the network would route traffic"');


-- Create table to persist quota email template configurations
CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_email_configuration`(
    `account_id` int(11) NOT NULL,
    `email_template_id` bigint(20) NOT NULL,
    `enabled` int(1) UNSIGNED NOT NULL,
    PRIMARY KEY (`account_id`, `email_template_id`),
    CONSTRAINT `FK_quota_email_configuration_account_id` FOREIGN KEY (`account_id`) REFERENCES `cloud_usage`.`quota_account`(`account_id`),
    CONSTRAINT `FK_quota_email_configuration_email_template_id` FOREIGN KEY (`email_template_id`) REFERENCES `cloud_usage`.`quota_email_templates`(`id`));

-- Add `is_implicit` column to `host_tags` table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host_tags', 'is_implicit', 'int(1) UNSIGNED NOT NULL DEFAULT 0 COMMENT "If host tag is implicit or explicit" ');
