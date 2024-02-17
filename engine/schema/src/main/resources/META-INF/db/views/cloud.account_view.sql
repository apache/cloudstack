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

-- cloud.account_view source


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
    `account`.`network_domain` AS `network_domain`,
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
    (`free_ip_view`
join ((((((((((((((((((((((((((((((`account`
join `domain` on
    ((`account`.`domain_id` = `domain`.`id`)))
left join `data_center` on
    ((`account`.`default_zone_id` = `data_center`.`id`)))
left join `account_netstats_view` on
    ((`account`.`id` = `account_netstats_view`.`account_id`)))
left join `resource_limit` `vmlimit` on
    (((`account`.`id` = `vmlimit`.`account_id`)
        and (`vmlimit`.`type` = 'user_vm'))))
left join `resource_count` `vmcount` on
    (((`account`.`id` = `vmcount`.`account_id`)
        and (`vmcount`.`type` = 'user_vm'))))
left join `account_vmstats_view` `runningvm` on
    (((`account`.`id` = `runningvm`.`account_id`)
        and (`runningvm`.`state` = 'Running'))))
left join `account_vmstats_view` `stoppedvm` on
    (((`account`.`id` = `stoppedvm`.`account_id`)
        and (`stoppedvm`.`state` = 'Stopped'))))
left join `resource_limit` `iplimit` on
    (((`account`.`id` = `iplimit`.`account_id`)
        and (`iplimit`.`type` = 'public_ip'))))
left join `resource_count` `ipcount` on
    (((`account`.`id` = `ipcount`.`account_id`)
        and (`ipcount`.`type` = 'public_ip'))))
left join `resource_limit` `volumelimit` on
    (((`account`.`id` = `volumelimit`.`account_id`)
        and (`volumelimit`.`type` = 'volume'))))
left join `resource_count` `volumecount` on
    (((`account`.`id` = `volumecount`.`account_id`)
        and (`volumecount`.`type` = 'volume'))))
left join `resource_limit` `snapshotlimit` on
    (((`account`.`id` = `snapshotlimit`.`account_id`)
        and (`snapshotlimit`.`type` = 'snapshot'))))
left join `resource_count` `snapshotcount` on
    (((`account`.`id` = `snapshotcount`.`account_id`)
        and (`snapshotcount`.`type` = 'snapshot'))))
left join `resource_limit` `templatelimit` on
    (((`account`.`id` = `templatelimit`.`account_id`)
        and (`templatelimit`.`type` = 'template'))))
left join `resource_count` `templatecount` on
    (((`account`.`id` = `templatecount`.`account_id`)
        and (`templatecount`.`type` = 'template'))))
left join `resource_limit` `vpclimit` on
    (((`account`.`id` = `vpclimit`.`account_id`)
        and (`vpclimit`.`type` = 'vpc'))))
left join `resource_count` `vpccount` on
    (((`account`.`id` = `vpccount`.`account_id`)
        and (`vpccount`.`type` = 'vpc'))))
left join `resource_limit` `projectlimit` on
    (((`account`.`id` = `projectlimit`.`account_id`)
        and (`projectlimit`.`type` = 'project'))))
left join `resource_count` `projectcount` on
    (((`account`.`id` = `projectcount`.`account_id`)
        and (`projectcount`.`type` = 'project'))))
left join `resource_limit` `networklimit` on
    (((`account`.`id` = `networklimit`.`account_id`)
        and (`networklimit`.`type` = 'network'))))
left join `resource_count` `networkcount` on
    (((`account`.`id` = `networkcount`.`account_id`)
        and (`networkcount`.`type` = 'network'))))
left join `resource_limit` `cpulimit` on
    (((`account`.`id` = `cpulimit`.`account_id`)
        and (`cpulimit`.`type` = 'cpu'))))
left join `resource_count` `cpucount` on
    (((`account`.`id` = `cpucount`.`account_id`)
        and (`cpucount`.`type` = 'cpu'))))
left join `resource_limit` `memorylimit` on
    (((`account`.`id` = `memorylimit`.`account_id`)
        and (`memorylimit`.`type` = 'memory'))))
left join `resource_count` `memorycount` on
    (((`account`.`id` = `memorycount`.`account_id`)
        and (`memorycount`.`type` = 'memory'))))
left join `resource_limit` `primary_storage_limit` on
    (((`account`.`id` = `primary_storage_limit`.`account_id`)
        and (`primary_storage_limit`.`type` = 'primary_storage'))))
left join `resource_count` `primary_storage_count` on
    (((`account`.`id` = `primary_storage_count`.`account_id`)
        and (`primary_storage_count`.`type` = 'primary_storage'))))
left join `resource_limit` `secondary_storage_limit` on
    (((`account`.`id` = `secondary_storage_limit`.`account_id`)
        and (`secondary_storage_limit`.`type` = 'secondary_storage'))))
left join `resource_count` `secondary_storage_count` on
    (((`account`.`id` = `secondary_storage_count`.`account_id`)
        and (`secondary_storage_count`.`type` = 'secondary_storage'))))
left join `async_job` on
    (((`async_job`.`instance_id` = `account`.`id`)
        and (`async_job`.`instance_type` = 'Account')
            and (`async_job`.`job_status` = 0)))));
