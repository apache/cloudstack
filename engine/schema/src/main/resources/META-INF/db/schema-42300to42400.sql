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
-- Schema upgrade from 4.23.0.0 to 4.24.0.0
--;
-- InstanceBootGroup: ordered boot sequencing for VMs and InstanceGroups
CREATE TABLE IF NOT EXISTS `cloud`.`instance_boot_group` (
    `id`          bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid`        varchar(40)     NOT NULL,
    `name`        varchar(255)    NOT NULL,
    `description` varchar(4096)   DEFAULT NULL,
    `account_id`  bigint unsigned NOT NULL COMMENT 'owner; foreign key to account table',
    `domain_id`   bigint unsigned NOT NULL,
    `created`     datetime        NOT NULL,
    `removed`     datetime        DEFAULT NULL COMMENT 'date the group was soft-deleted',
    PRIMARY KEY (`id`),
    CONSTRAINT `uc_instance_boot_group__uuid`       UNIQUE (`uuid`),
    CONSTRAINT `fk_instance_boot_group__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`),
    CONSTRAINT `fk_instance_boot_group__domain_id`  FOREIGN KEY (`domain_id`)  REFERENCES `domain`  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cloud`.`instance_boot_group_member` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT,
    `uuid`          varchar(40)     NOT NULL,
    `boot_group_id` bigint unsigned NOT NULL,
    `member_type`   varchar(32)     NOT NULL COMMENT 'VirtualMachine or InstanceGroup',
    `member_id`     bigint unsigned NOT NULL,
    `boot_order`    int             NOT NULL DEFAULT 0,
    `created`       datetime        NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uc_instance_boot_group_member__uuid`    UNIQUE (`uuid`),
    CONSTRAINT `uq_instance_boot_group_member__member`  UNIQUE (`member_type`, `member_id`),
    CONSTRAINT `fk_instance_boot_group_member__group_id` FOREIGN KEY (`boot_group_id`) REFERENCES `instance_boot_group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- InstanceBootGroupReadinessRule: a readiness rule always belongs to exactly one boot group and
-- references either a VirtualMachine or InstanceGroup item within it
CREATE TABLE IF NOT EXISTS `cloud`.`instance_boot_group_readiness_rule` (
    `id`            bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid`          varchar(40)     NOT NULL,
    `name`          varchar(255)    NOT NULL,
    `boot_group_id` bigint unsigned NOT NULL,
    `item_type`     varchar(32)     NOT NULL COMMENT 'VirtualMachine or InstanceGroup',
    `item_id`       bigint unsigned NOT NULL,
    `rule_type`     varchar(64)     NOT NULL,
    `enabled`       tinyint(1)      NOT NULL DEFAULT 1,
    `created`       datetime        NOT NULL,
    `removed`       datetime        DEFAULT NULL COMMENT 'date the rule was soft-deleted',
    PRIMARY KEY (`id`),
    CONSTRAINT `uc_instance_boot_group_readiness_rule__uuid`     UNIQUE (`uuid`),
    CONSTRAINT `fk_instance_boot_group_readiness_rule__group_id` FOREIGN KEY (`boot_group_id`) REFERENCES `instance_boot_group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Generic key/value config for a readiness rule (port/protocol, script content, threshold, ...);
CREATE TABLE IF NOT EXISTS `cloud`.`instance_boot_group_readiness_rule_details` (
    `id`      bigint unsigned NOT NULL AUTO_INCREMENT,
    `rule_id` bigint unsigned NOT NULL,
    `name`    varchar(255)    NOT NULL,
    `value`   text            DEFAULT NULL,
    `display` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Whether detail be displayed to the end user',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_instance_boot_group_readiness_rule_details__rule_id` FOREIGN KEY (`rule_id`) REFERENCES `instance_boot_group_readiness_rule` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Last cached evaluation result for a readiness rule, upserted in place (no history, by design).
CREATE TABLE IF NOT EXISTS `cloud`.`instance_boot_group_readiness_check_result` (
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT,
    `rule_id`    bigint unsigned NOT NULL,
    `vm_id`      bigint unsigned NOT NULL DEFAULT 0,
    `status`     varchar(32)     NOT NULL DEFAULT 'Unknown',
    `message`    varchar(4096)   DEFAULT NULL,
    `checked_on` datetime        DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_instance_boot_group_readiness_check_result__rule_vm` (`rule_id`, `vm_id`),
    CONSTRAINT `fk_instance_boot_group_readiness_check_result__rule_id` FOREIGN KEY (`rule_id`) REFERENCES `instance_boot_group_readiness_rule` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cloud`.`instance_boot_group_details` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT,
    `boot_group_id` bigint unsigned NOT NULL,
    `name`          varchar(255)    NOT NULL,
    `value`         varchar(255)    DEFAULT NULL,
    `display` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Whether detail be displayed to the end user',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_instance_boot_group_details__group_id` FOREIGN KEY (`boot_group_id`) REFERENCES `instance_boot_group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
