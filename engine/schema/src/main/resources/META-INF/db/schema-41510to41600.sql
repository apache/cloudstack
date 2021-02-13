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
-- Schema upgrade from 4.15.1.0 to 4.16.0.0
--;


--;
-- Stored procedure to do idempotent column add;
-- This is copied from schema-41000to41100.sql
--;
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_ADD_COLUMN`;

CREATE PROCEDURE `cloud`.`IDEMPOTENT_ADD_COLUMN` (
    IN in_table_name VARCHAR(200),
    IN in_column_name VARCHAR(200),
    IN in_column_definition VARCHAR(1000)
)
BEGIN

    DECLARE CONTINUE HANDLER FOR 1060 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name); SET @ddl = CONCAT(@ddl, ' ', 'ADD COLUMN') ; SET @ddl = CONCAT(@ddl, ' ', in_column_name); SET @ddl = CONCAT(@ddl, ' ', in_column_definition); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.account','created', 'datetime DEFAULT NULL COMMENT ''date created'' AFTER `state` ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.domain','created', 'datetime DEFAULT NULL COMMENT ''date created'' AFTER `next_child_seq` ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.account','created', 'datetime DEFAULT NULL COMMENT ''date created'' AFTER `state` ');

DROP VIEW IF EXISTS `cloud`.`account_view`;
CREATE VIEW `cloud`.`account_view` AS
select
    `account`.`id` AS `id`,
    `account`.`uuid` AS `uuid`,
    `account`.`account_name` AS `account_name`,
    `account`.`type` AS `type`,
    `account`.`role_id` AS `role_id`,
    `account`.`state` AS `state`,
    `account`.`created` AS `created`,
    `account`.`removed` AS `removed`,
    `account`.`cleanup_needed` AS `cleanup_needed`,
    `account`.`network_domain` AS `network_domain` ,
    `account`.`default` AS `default`,
    `domain`.`id` AS `domain_id`,
    `domain`.`uuid` AS `domain_uuid`,
    `domain`.`name` AS `domain_name`,
    `domain`.`path` AS `domain_path`,
    `data_center`.`id` AS `data_center_id`,
    `data_center`.`uuid` AS `data_center_uuid`,
    `data_center`.`name` AS `data_center_name`,
    `account_netstats_view`.`bytesReceived` AS `bytesReceived`,
    `account_netstats_view`.`bytesSent` AS `bytesSent`,
    `vmlimit`.`max` AS `vmLimit`,
    `vmcount`.`count` AS `vmTotal`,
    `runningvm`.`vmcount` AS `runningVms`,
    `stoppedvm`.`vmcount` AS `stoppedVms`,
    `iplimit`.`max` AS `ipLimit`,
    `ipcount`.`count` AS `ipTotal`,
    `free_ip_view`.`free_ip` AS `ipFree`,
    `volumelimit`.`max` AS `volumeLimit`,
    `volumecount`.`count` AS `volumeTotal`,
    `snapshotlimit`.`max` AS `snapshotLimit`,
    `snapshotcount`.`count` AS `snapshotTotal`,
    `templatelimit`.`max` AS `templateLimit`,
    `templatecount`.`count` AS `templateTotal`,
    `vpclimit`.`max` AS `vpcLimit`,
    `vpccount`.`count` AS `vpcTotal`,
    `projectlimit`.`max` AS `projectLimit`,
    `projectcount`.`count` AS `projectTotal`,
    `networklimit`.`max` AS `networkLimit`,
    `networkcount`.`count` AS `networkTotal`,
    `cpulimit`.`max` AS `cpuLimit`,
    `cpucount`.`count` AS `cpuTotal`,
    `memorylimit`.`max` AS `memoryLimit`,
    `memorycount`.`count` AS `memoryTotal`,
    `primary_storage_limit`.`max` AS `primaryStorageLimit`,
    `primary_storage_count`.`count` AS `primaryStorageTotal`,
    `secondary_storage_limit`.`max` AS `secondaryStorageLimit`,
    `secondary_storage_count`.`count` AS `secondaryStorageTotal`,
    `async_job`.`id` AS `job_id`,
    `async_job`.`uuid` AS `job_uuid`,
    `async_job`.`job_status` AS `job_status`,
    `async_job`.`account_id` AS `job_account_id`
from
    `cloud`.`free_ip_view`,
    `cloud`.`account`
        inner join
    `cloud`.`domain` ON account.domain_id = domain.id
        left join
    `cloud`.`data_center` ON account.default_zone_id = data_center.id
        left join
    `cloud`.`account_netstats_view` ON account.id = account_netstats_view.account_id
        left join
    `cloud`.`resource_limit` vmlimit ON account.id = vmlimit.account_id
        and vmlimit.type = 'user_vm'
        left join
    `cloud`.`resource_count` vmcount ON account.id = vmcount.account_id
        and vmcount.type = 'user_vm'
        left join
    `cloud`.`account_vmstats_view` runningvm ON account.id = runningvm.account_id
        and runningvm.state = 'Running'
        left join
    `cloud`.`account_vmstats_view` stoppedvm ON account.id = stoppedvm.account_id
        and stoppedvm.state = 'Stopped'
        left join
    `cloud`.`resource_limit` iplimit ON account.id = iplimit.account_id
        and iplimit.type = 'public_ip'
        left join
    `cloud`.`resource_count` ipcount ON account.id = ipcount.account_id
        and ipcount.type = 'public_ip'
        left join
    `cloud`.`resource_limit` volumelimit ON account.id = volumelimit.account_id
        and volumelimit.type = 'volume'
        left join
    `cloud`.`resource_count` volumecount ON account.id = volumecount.account_id
        and volumecount.type = 'volume'
        left join
    `cloud`.`resource_limit` snapshotlimit ON account.id = snapshotlimit.account_id
        and snapshotlimit.type = 'snapshot'
        left join
    `cloud`.`resource_count` snapshotcount ON account.id = snapshotcount.account_id
        and snapshotcount.type = 'snapshot'
        left join
    `cloud`.`resource_limit` templatelimit ON account.id = templatelimit.account_id
        and templatelimit.type = 'template'
        left join
    `cloud`.`resource_count` templatecount ON account.id = templatecount.account_id
        and templatecount.type = 'template'
        left join
    `cloud`.`resource_limit` vpclimit ON account.id = vpclimit.account_id
        and vpclimit.type = 'vpc'
        left join
    `cloud`.`resource_count` vpccount ON account.id = vpccount.account_id
        and vpccount.type = 'vpc'
        left join
    `cloud`.`resource_limit` projectlimit ON account.id = projectlimit.account_id
        and projectlimit.type = 'project'
        left join
    `cloud`.`resource_count` projectcount ON account.id = projectcount.account_id
        and projectcount.type = 'project'
        left join
    `cloud`.`resource_limit` networklimit ON account.id = networklimit.account_id
        and networklimit.type = 'network'
        left join
    `cloud`.`resource_count` networkcount ON account.id = networkcount.account_id
        and networkcount.type = 'network'
        left join
    `cloud`.`resource_limit` cpulimit ON account.id = cpulimit.account_id
        and cpulimit.type = 'cpu'
        left join
    `cloud`.`resource_count` cpucount ON account.id = cpucount.account_id
        and cpucount.type = 'cpu'
        left join
    `cloud`.`resource_limit` memorylimit ON account.id = memorylimit.account_id
        and memorylimit.type = 'memory'
        left join
    `cloud`.`resource_count` memorycount ON account.id = memorycount.account_id
        and memorycount.type = 'memory'
        left join
    `cloud`.`resource_limit` primary_storage_limit ON account.id = primary_storage_limit.account_id
        and primary_storage_limit.type = 'primary_storage'
        left join
    `cloud`.`resource_count` primary_storage_count ON account.id = primary_storage_count.account_id
        and primary_storage_count.type = 'primary_storage'
        left join
    `cloud`.`resource_limit` secondary_storage_limit ON account.id = secondary_storage_limit.account_id
        and secondary_storage_limit.type = 'secondary_storage'
        left join
    `cloud`.`resource_count` secondary_storage_count ON account.id = secondary_storage_count.account_id
        and secondary_storage_count.type = 'secondary_storage'
        left join
    `cloud`.`async_job` ON async_job.instance_id = account.id
        and async_job.instance_type = 'Account'
        and async_job.job_status = 0;


DROP VIEW IF EXISTS `cloud`.`domain_view`;
CREATE VIEW `cloud`.`domain_view` AS
select
    `domain`.`id` AS `id`,
    `domain`.`parent` AS `parent`,
    `domain`.`name` AS `name`,
    `domain`.`uuid` AS `uuid`,
    `domain`.`owner` AS `owner`,
    `domain`.`path` AS `path`,
    `domain`.`level` AS `level`,
    `domain`.`child_count` AS `child_count`,
    `domain`.`next_child_seq` AS `next_child_seq`,
    `domain`.`created` AS `created`,
    `domain`.`removed` AS `removed`,
    `domain`.`state` AS `state`,
    `domain`.`network_domain` AS `network_domain`,
    `domain`.`type` AS `type`,
    `vmlimit`.`max` AS `vmLimit`,
    `vmcount`.`count` AS `vmTotal`,
    `iplimit`.`max` AS `ipLimit`,
    `ipcount`.`count` AS `ipTotal`,
    `volumelimit`.`max` AS `volumeLimit`,
    `volumecount`.`count` AS `volumeTotal`,
    `snapshotlimit`.`max` AS `snapshotLimit`,
    `snapshotcount`.`count` AS `snapshotTotal`,
    `templatelimit`.`max` AS `templateLimit`,
    `templatecount`.`count` AS `templateTotal`,
    `vpclimit`.`max` AS `vpcLimit`,
    `vpccount`.`count` AS `vpcTotal`,
    `projectlimit`.`max` AS `projectLimit`,
    `projectcount`.`count` AS `projectTotal`,
    `networklimit`.`max` AS `networkLimit`,
    `networkcount`.`count` AS `networkTotal`,
    `cpulimit`.`max` AS `cpuLimit`,
    `cpucount`.`count` AS `cpuTotal`,
    `memorylimit`.`max` AS `memoryLimit`,
    `memorycount`.`count` AS `memoryTotal`,
    `primary_storage_limit`.`max` AS `primaryStorageLimit`,
    `primary_storage_count`.`count` AS `primaryStorageTotal`,
    `secondary_storage_limit`.`max` AS `secondaryStorageLimit`,
    `secondary_storage_count`.`count` AS `secondaryStorageTotal`
from
    `cloud`.`domain`
        left join
    `cloud`.`resource_limit` vmlimit ON domain.id = vmlimit.domain_id
        and vmlimit.type = 'user_vm'
        left join
    `cloud`.`resource_count` vmcount ON domain.id = vmcount.domain_id
        and vmcount.type = 'user_vm'
        left join
    `cloud`.`resource_limit` iplimit ON domain.id = iplimit.domain_id
        and iplimit.type = 'public_ip'
        left join
    `cloud`.`resource_count` ipcount ON domain.id = ipcount.domain_id
        and ipcount.type = 'public_ip'
        left join
    `cloud`.`resource_limit` volumelimit ON domain.id = volumelimit.domain_id
        and volumelimit.type = 'volume'
        left join
    `cloud`.`resource_count` volumecount ON domain.id = volumecount.domain_id
        and volumecount.type = 'volume'
        left join
    `cloud`.`resource_limit` snapshotlimit ON domain.id = snapshotlimit.domain_id
        and snapshotlimit.type = 'snapshot'
        left join
    `cloud`.`resource_count` snapshotcount ON domain.id = snapshotcount.domain_id
        and snapshotcount.type = 'snapshot'
        left join
    `cloud`.`resource_limit` templatelimit ON domain.id = templatelimit.domain_id
        and templatelimit.type = 'template'
        left join
    `cloud`.`resource_count` templatecount ON domain.id = templatecount.domain_id
        and templatecount.type = 'template'
        left join
    `cloud`.`resource_limit` vpclimit ON domain.id = vpclimit.domain_id
        and vpclimit.type = 'vpc'
        left join
    `cloud`.`resource_count` vpccount ON domain.id = vpccount.domain_id
        and vpccount.type = 'vpc'
        left join
    `cloud`.`resource_limit` projectlimit ON domain.id = projectlimit.domain_id
        and projectlimit.type = 'project'
        left join
    `cloud`.`resource_count` projectcount ON domain.id = projectcount.domain_id
        and projectcount.type = 'project'
        left join
    `cloud`.`resource_limit` networklimit ON domain.id = networklimit.domain_id
        and networklimit.type = 'network'
        left join
    `cloud`.`resource_count` networkcount ON domain.id = networkcount.domain_id
        and networkcount.type = 'network'
        left join
    `cloud`.`resource_limit` cpulimit ON domain.id = cpulimit.domain_id
        and cpulimit.type = 'cpu'
        left join
    `cloud`.`resource_count` cpucount ON domain.id = cpucount.domain_id
        and cpucount.type = 'cpu'
        left join
    `cloud`.`resource_limit` memorylimit ON domain.id = memorylimit.domain_id
        and memorylimit.type = 'memory'
        left join
    `cloud`.`resource_count` memorycount ON domain.id = memorycount.domain_id
        and memorycount.type = 'memory'
        left join
    `cloud`.`resource_limit` primary_storage_limit ON domain.id = primary_storage_limit.domain_id
        and primary_storage_limit.type = 'primary_storage'
        left join
    `cloud`.`resource_count` primary_storage_count ON domain.id = primary_storage_count.domain_id
        and primary_storage_count.type = 'primary_storage'
        left join
    `cloud`.`resource_limit` secondary_storage_limit ON domain.id = secondary_storage_limit.domain_id
        and secondary_storage_limit.type = 'secondary_storage'
        left join
    `cloud`.`resource_count` secondary_storage_count ON domain.id = secondary_storage_count.domain_id
        and secondary_storage_count.type = 'secondary_storage';
