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
-- Schema upgrade from 4.12.0.0 to 4.13.0.0
--;

-- Move domain_id to disk offering details and drop the domain_id column
INSERT INTO `cloud`.`disk_offering_details` (offering_id, name, value) SELECT id, 'domainid', domain_id FROM `cloud`.`disk_offering` WHERE domain_id IS NOT NULL;
ALTER TABLE `cloud`.`disk_offering` DROP COLUMN `domain_id`;

-- Disk offering with multi-domains and multi-zones
DROP VIEW IF EXISTS `cloud`.`disk_offering_view`;
CREATE VIEW `cloud`.`disk_offering_view` AS
    select
        disk_offering.id,
        disk_offering.uuid,
        disk_offering.name,
        disk_offering.display_text,
        disk_offering.provisioning_type,
        disk_offering.disk_size,
        disk_offering.min_iops,
        disk_offering.max_iops,
        disk_offering.created,
        disk_offering.tags,
        disk_offering.customized,
        disk_offering.customized_iops,
        disk_offering.removed,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        disk_offering.hv_ss_reserve,
        disk_offering.bytes_read_rate,
        disk_offering.bytes_write_rate,
        disk_offering.iops_read_rate,
        disk_offering.iops_write_rate,
        disk_offering.cache_mode,
        disk_offering.sort_key,
        disk_offering.type,
        disk_offering.display_offering,
        disk_offering.state,
        GROUP_CONCAT(domain_details.value) AS domain_id,
        GROUP_CONCAT(domain.uuid) AS domain_uuid,
        GROUP_CONCAT(domain.name) AS domain_name,
        GROUP_CONCAT(domain.path) AS domain_path,
        GROUP_CONCAT(zone_details.value) AS zone_id,
        GROUP_CONCAT(zone.uuid) AS zone_uuid,
        GROUP_CONCAT(zone.name) AS zone_name
    from
        `cloud`.`disk_offering`
            left join
        `cloud`.`disk_offering_details` AS `domain_details` ON `domain_details`.`offering_id` = `disk_offering`.`id` AND `domain_details`.`name`='domainid'
            left join
        `cloud`.`domain` AS `domain` ON `domain`.`id` = `domain_details`.`value`
            left join
        `cloud`.`disk_offering_details` AS `zone_details` ON `zone_details`.`offering_id` = `disk_offering`.`id` AND `zone_details`.`name`='zoneid'
            left join
        `cloud`.`data_center` AS `zone` ON `zone`.`id` = `zone_details`.`value`
    where
        disk_offering.state='ACTIVE' GROUP BY id;

-- Service offering with multi-domains and multi-zones
DROP VIEW IF EXISTS `cloud`.`service_offering_view`;
CREATE VIEW `cloud`.`service_offering_view` AS
    select
        service_offering.id,
        disk_offering.uuid,
        disk_offering.name,
        disk_offering.display_text,
        disk_offering.provisioning_type,
        disk_offering.created,
        disk_offering.tags,
        disk_offering.removed,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        disk_offering.customized_iops,
        disk_offering.min_iops,
        disk_offering.max_iops,
        disk_offering.hv_ss_reserve,
        disk_offering.bytes_read_rate,
        disk_offering.bytes_write_rate,
        disk_offering.iops_read_rate,
        disk_offering.iops_write_rate,
        disk_offering.cache_mode,
        service_offering.cpu,
        service_offering.speed,
        service_offering.ram_size,
        service_offering.nw_rate,
        service_offering.mc_rate,
        service_offering.ha_enabled,
        service_offering.limit_cpu_use,
        service_offering.host_tag,
        service_offering.default_use,
        service_offering.vm_type,
        service_offering.sort_key,
        service_offering.is_volatile,
        service_offering.deployment_planner,
        disk_offering.domain_id,
        disk_offering.domain_uuid,
        disk_offering.domain_name,
        disk_offering.domain_path,
        disk_offering.zone_id,
        disk_offering.zone_uuid,
        disk_offering.zone_name
    from
        `cloud`.`service_offering`
            inner join
        `cloud`.`disk_offering_view` AS `disk_offering` ON service_offering.id = disk_offering.id
    where
        disk_offering.state='Active';
