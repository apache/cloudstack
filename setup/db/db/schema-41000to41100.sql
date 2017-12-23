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
-- Schema upgrade from 4.10.0.0 to 4.11.0.0
--;

--Alter view template_view
 
DROP VIEW IF EXISTS `cloud`.`template_view`;
CREATE VIEW `template_view` AS
     SELECT 
         `vm_template`.`id` AS `id`,
         `vm_template`.`uuid` AS `uuid`,
         `vm_template`.`unique_name` AS `unique_name`,
         `vm_template`.`name` AS `name`,
         `vm_template`.`public` AS `public`,
         `vm_template`.`featured` AS `featured`,
         `vm_template`.`type` AS `type`,
         `vm_template`.`hvm` AS `hvm`,
         `vm_template`.`bits` AS `bits`,
         `vm_template`.`url` AS `url`,
         `vm_template`.`format` AS `format`,
         `vm_template`.`created` AS `created`,
         `vm_template`.`checksum` AS `checksum`,
         `vm_template`.`display_text` AS `display_text`,
         `vm_template`.`enable_password` AS `enable_password`,
         `vm_template`.`dynamically_scalable` AS `dynamically_scalable`,
         `vm_template`.`state` AS `template_state`,
         `vm_template`.`guest_os_id` AS `guest_os_id`,
         `guest_os`.`uuid` AS `guest_os_uuid`,
         `guest_os`.`display_name` AS `guest_os_name`,
         `vm_template`.`bootable` AS `bootable`,
         `vm_template`.`prepopulate` AS `prepopulate`,
         `vm_template`.`cross_zones` AS `cross_zones`,
         `vm_template`.`hypervisor_type` AS `hypervisor_type`,
         `vm_template`.`extractable` AS `extractable`,
         `vm_template`.`template_tag` AS `template_tag`,
         `vm_template`.`sort_key` AS `sort_key`,
         `vm_template`.`removed` AS `removed`,
         `vm_template`.`enable_sshkey` AS `enable_sshkey`,
         `source_template`.`id` AS `source_template_id`,
         `source_template`.`uuid` AS `source_template_uuid`,
         `account`.`id` AS `account_id`,
         `account`.`uuid` AS `account_uuid`,
         `account`.`account_name` AS `account_name`,
         `account`.`type` AS `account_type`,
         `domain`.`id` AS `domain_id`,
         `domain`.`uuid` AS `domain_uuid`,
         `domain`.`name` AS `domain_name`,
         `domain`.`path` AS `domain_path`,
         `projects`.`id` AS `project_id`,
         `projects`.`uuid` AS `project_uuid`,
         `projects`.`name` AS `project_name`,
         `data_center`.`id` AS `data_center_id`,
         `data_center`.`uuid` AS `data_center_uuid`,
         `data_center`.`name` AS `data_center_name`,
         `launch_permission`.`account_id` AS `lp_account_id`,
         `template_store_ref`.`store_id` AS `store_id`,
         `image_store`.`scope` AS `store_scope`,
         `template_store_ref`.`state` AS `state`,
         `template_store_ref`.`download_state` AS `download_state`,
         `template_store_ref`.`download_pct` AS `download_pct`,
         `template_store_ref`.`error_str` AS `error_str`,
         `template_store_ref`.`size` AS `size`,
         `template_store_ref`.physical_size AS `physical_size`,
         `template_store_ref`.`destroyed` AS `destroyed`,
         `template_store_ref`.`created` AS `created_on_store`,
         `vm_template_details`.`name` AS `detail_name`,
         `vm_template_details`.`value` AS `detail_value`,
         `resource_tags`.`id` AS `tag_id`,
         `resource_tags`.`uuid` AS `tag_uuid`,
         `resource_tags`.`key` AS `tag_key`,
         `resource_tags`.`value` AS `tag_value`,
         `resource_tags`.`domain_id` AS `tag_domain_id`,
         `domain`.`uuid` AS `tag_domain_uuid`,
         `domain`.`name` AS `tag_domain_name`,
         `resource_tags`.`account_id` AS `tag_account_id`,
         `account`.`account_name` AS `tag_account_name`,
         `resource_tags`.`resource_id` AS `tag_resource_id`,
         `resource_tags`.`resource_uuid` AS `tag_resource_uuid`,
         `resource_tags`.`resource_type` AS `tag_resource_type`,
         `resource_tags`.`customer` AS `tag_customer`,
         CONCAT(`vm_template`.`id`,
                 '_',
                 IFNULL(`data_center`.`id`, 0)) AS `temp_zone_pair`
     FROM
         ((((((((((((`vm_template`
         JOIN `guest_os` ON ((`guest_os`.`id` = `vm_template`.`guest_os_id`)))
         JOIN `account` ON ((`account`.`id` = `vm_template`.`account_id`)))
         JOIN `domain` ON ((`domain`.`id` = `account`.`domain_id`)))
         LEFT JOIN `projects` ON ((`projects`.`project_account_id` = `account`.`id`)))
         LEFT JOIN `vm_template_details` ON ((`vm_template_details`.`template_id` = `vm_template`.`id`)))
         LEFT JOIN `vm_template` `source_template` ON ((`source_template`.`id` = `vm_template`.`source_template_id`)))
         LEFT JOIN `template_store_ref` ON (((`template_store_ref`.`template_id` = `vm_template`.`id`)
             AND (`template_store_ref`.`store_role` = 'Image')
             AND (`template_store_ref`.`destroyed` = 0))))
         LEFT JOIN `image_store` ON ((ISNULL(`image_store`.`removed`)
             AND (`template_store_ref`.`store_id` IS NOT NULL)
             AND (`image_store`.`id` = `template_store_ref`.`store_id`))))
         LEFT JOIN `template_zone_ref` ON (((`template_zone_ref`.`template_id` = `vm_template`.`id`)
             AND ISNULL(`template_store_ref`.`store_id`)
             AND ISNULL(`template_zone_ref`.`removed`))))
         LEFT JOIN `data_center` ON (((`image_store`.`data_center_id` = `data_center`.`id`)
             OR (`template_zone_ref`.`zone_id` = `data_center`.`id`))))
         LEFT JOIN `launch_permission` ON ((`launch_permission`.`template_id` = `vm_template`.`id`)))
         LEFT JOIN `resource_tags` ON (((`resource_tags`.`resource_id` = `vm_template`.`id`)
             AND ((`resource_tags`.`resource_type` = 'Template')
             OR (`resource_tags`.`resource_type` = 'ISO')))));

UPDATE `cloud`.`configuration` SET value = '600', default_value = '600' WHERE category = 'Advanced' AND name = 'router.aggregation.command.each.timeout';

-- CA framework changes
DELETE from `cloud`.`configuration` where name='ssl.keystore';

-- Certificate Revocation List
CREATE TABLE IF NOT EXISTS `cloud`.`crl` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `serial` varchar(255) UNIQUE NOT NULL COMMENT 'certificate\'s serial number as hex string',
  `cn` varchar(255) COMMENT 'certificate\'s common name',
  `revoker_uuid` varchar(40) COMMENT 'revoker user account uuid',
  `revoked` datetime COMMENT 'date of revocation',
  PRIMARY KEY (`id`),
  KEY (`serial`),
  UNIQUE KEY (`serial`, `cn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Host HA feature
CREATE TABLE IF NOT EXISTS `cloud`.`ha_config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resource_id` bigint(20) unsigned DEFAULT NULL COMMENT 'id of the resource',
  `resource_type` varchar(255) NOT NULL COMMENT 'the type of the resource',
  `enabled` int(1) unsigned DEFAULT '0' COMMENT 'is HA enabled for the resource',
  `ha_state` varchar(255) DEFAULT 'Disabled' COMMENT 'HA state',
  `provider` varchar(255) DEFAULT NULL COMMENT 'HA provider',
  `update_count` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT 'state based incr-only counter for atomic ha_state updates',
  `update_time` datetime COMMENT 'last ha_state update datetime',
  `mgmt_server_id` bigint(20) unsigned DEFAULT NULL COMMENT 'management server id that is responsible for the HA for the resource',
  PRIMARY KEY (`id`),
  KEY `i_ha_config__enabled` (`enabled`),
  KEY `i_ha_config__ha_state` (`ha_state`),
  KEY `i_ha_config__mgmt_server_id` (`mgmt_server_id`),
  UNIQUE KEY (`resource_id`, `resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE from `cloud`.`configuration` where name='outofbandmanagement.sync.interval';

-- Annotations specifc changes following
CREATE TABLE IF NOT EXISTS `cloud`.`annotations` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) UNIQUE,
  `annotation` text,
  `entity_uuid` varchar(40),
  `entity_type` varchar(32),
  `user_uuid` varchar(40),
  `created` datetime COMMENT 'date of creation',
  `removed` datetime COMMENT 'date of removal',
  PRIMARY KEY (`id`),
  KEY (`uuid`),
  KEY `i_entity` (`entity_uuid`, `entity_type`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP VIEW IF EXISTS `cloud`.`last_annotation_view`;
CREATE VIEW `last_annotation_view` AS
    SELECT
        `annotations`.`uuid` AS `uuid`,
        `annotations`.`annotation` AS `annotation`,
        `annotations`.`entity_uuid` AS `entity_uuid`,
        `annotations`.`entity_type` AS `entity_type`,
        `annotations`.`user_uuid` AS `user_uuid`,
        `annotations`.`created` AS `created`,
        `annotations`.`removed` AS `removed`
    FROM
        `annotations`
    WHERE
        `annotations`.`created` IN (SELECT
                                        MAX(`annotations`.`created`)
                                    FROM
                                        `annotations`
                                    WHERE
                                        `annotations`.`removed` IS NULL
                                    GROUP BY `annotations`.`entity_uuid`);

-- Host HA changes:
DROP VIEW IF EXISTS `cloud`.`host_view`;
CREATE VIEW `cloud`.`host_view` AS
    SELECT
        host.id,
        host.uuid,
        host.name,
        host.status,
        host.disconnected,
        host.type,
        host.private_ip_address,
        host.version,
        host.hypervisor_type,
        host.hypervisor_version,
        host.capabilities,
        host.last_ping,
        host.created,
        host.removed,
        host.resource_state,
        host.mgmt_server_id,
        host.cpu_sockets,
        host.cpus,
        host.speed,
        host.ram,
        cluster.id cluster_id,
        cluster.uuid cluster_uuid,
        cluster.name cluster_name,
        cluster.cluster_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_type,
        host_pod_ref.id pod_id,
        host_pod_ref.uuid pod_uuid,
        host_pod_ref.name pod_name,
        host_tags.tag,
        guest_os_category.id guest_os_category_id,
        guest_os_category.uuid guest_os_category_uuid,
        guest_os_category.name guest_os_category_name,
        mem_caps.used_capacity memory_used_capacity,
        mem_caps.reserved_capacity memory_reserved_capacity,
        cpu_caps.used_capacity cpu_used_capacity,
        cpu_caps.reserved_capacity cpu_reserved_capacity,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id,
        oobm.enabled AS `oobm_enabled`,
        oobm.power_state AS `oobm_power_state`,
        ha_config.enabled AS `ha_enabled`,
        ha_config.ha_state AS `ha_state`,
        ha_config.provider AS `ha_provider`,
        `last_annotation_view`.`annotation` AS `annotation`,
        `last_annotation_view`.`created` AS `last_annotated`,
        `user`.`username` AS `username`
    FROM
        `cloud`.`host`
            LEFT JOIN
        `cloud`.`cluster` ON host.cluster_id = cluster.id
            LEFT JOIN
        `cloud`.`data_center` ON host.data_center_id = data_center.id
            LEFT JOIN
        `cloud`.`host_pod_ref` ON host.pod_id = host_pod_ref.id
            LEFT JOIN
        `cloud`.`host_details` ON host.id = host_details.host_id
            AND host_details.name = 'guest.os.category.id'
            LEFT JOIN
        `cloud`.`guest_os_category` ON guest_os_category.id = CONVERT ( host_details.value, UNSIGNED )
            LEFT JOIN
        `cloud`.`host_tags` ON host_tags.host_id = host.id
            LEFT JOIN
        `cloud`.`op_host_capacity` mem_caps ON host.id = mem_caps.host_id
            AND mem_caps.capacity_type = 0
            LEFT JOIN
        `cloud`.`op_host_capacity` cpu_caps ON host.id = cpu_caps.host_id
            AND cpu_caps.capacity_type = 1
            LEFT JOIN
        `cloud`.`async_job` ON async_job.instance_id = host.id
            AND async_job.instance_type = 'Host'
            AND async_job.job_status = 0
            LEFT JOIN
        `cloud`.`oobm` ON oobm.host_id = host.id
            left join
        `cloud`.`ha_config` ON ha_config.resource_id=host.id
            and ha_config.resource_type='Host'
            LEFT JOIN
        `cloud`.`last_annotation_view` ON `last_annotation_view`.`entity_uuid` = `host`.`uuid`
            LEFT JOIN
        `cloud`.`user` ON `user`.`uuid` = `last_annotation_view`.`user_uuid`;
-- End Of Annotations specific changes


-- Out-of-band management driver for nested-cloudstack
ALTER TABLE `cloud`.`oobm` MODIFY COLUMN port VARCHAR(255);


-- CLOUDSTACK-9902: Console proxy SSL toggle
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `description`, `default_value`, `is_dynamic`) VALUES ('Console Proxy', 'DEFAULT', 'AgentManager', 'consoleproxy.sslEnabled', 'false', 'Enable SSL for console proxy', 'false', 0);

-- CLOUDSTACK-9859: Retirement of midonet plugin (final removal) 
delete from `cloud`.`configuration` where name in ('midonet.apiserver.address', 'midonet.providerrouter.id');