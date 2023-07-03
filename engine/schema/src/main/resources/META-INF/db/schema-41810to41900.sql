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
-- Schema upgrade from 4.18.1.0 to 4.19.0.0
--;

ALTER TABLE `cloud`.`mshost` MODIFY COLUMN `state` varchar(25);

DROP VIEW IF EXISTS `cloud`.`async_job_view`;
CREATE VIEW `cloud`.`async_job_view` AS
    select
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        user.id user_id,
        user.uuid user_uuid,
        async_job.id,
        async_job.uuid,
        async_job.job_cmd,
        async_job.job_status,
        async_job.job_process_status,
        async_job.job_result_code,
        async_job.job_result,
        async_job.created,
        async_job.removed,
        async_job.instance_type,
        async_job.instance_id,
        async_job.job_executing_msid,
        CASE
            WHEN async_job.instance_type = 'Volume' THEN volumes.uuid
            WHEN
                async_job.instance_type = 'Template'
                    or async_job.instance_type = 'Iso'
            THEN
                vm_template.uuid
            WHEN
                async_job.instance_type = 'VirtualMachine'
                    or async_job.instance_type = 'ConsoleProxy'
                    or async_job.instance_type = 'SystemVm'
                    or async_job.instance_type = 'DomainRouter'
            THEN
                vm_instance.uuid
            WHEN async_job.instance_type = 'Snapshot' THEN snapshots.uuid
            WHEN async_job.instance_type = 'Host' THEN host.uuid
            WHEN async_job.instance_type = 'StoragePool' THEN storage_pool.uuid
            WHEN async_job.instance_type = 'IpAddress' THEN user_ip_address.uuid
            WHEN async_job.instance_type = 'SecurityGroup' THEN security_group.uuid
            WHEN async_job.instance_type = 'PhysicalNetwork' THEN physical_network.uuid
            WHEN async_job.instance_type = 'TrafficType' THEN physical_network_traffic_types.uuid
            WHEN async_job.instance_type = 'PhysicalNetworkServiceProvider' THEN physical_network_service_providers.uuid
            WHEN async_job.instance_type = 'FirewallRule' THEN firewall_rules.uuid
            WHEN async_job.instance_type = 'Account' THEN acct.uuid
            WHEN async_job.instance_type = 'User' THEN us.uuid
            WHEN async_job.instance_type = 'StaticRoute' THEN static_routes.uuid
            WHEN async_job.instance_type = 'PrivateGateway' THEN vpc_gateways.uuid
            WHEN async_job.instance_type = 'Counter' THEN counter.uuid
            WHEN async_job.instance_type = 'Condition' THEN conditions.uuid
            WHEN async_job.instance_type = 'AutoScalePolicy' THEN autoscale_policies.uuid
            WHEN async_job.instance_type = 'AutoScaleVmProfile' THEN autoscale_vmprofiles.uuid
            WHEN async_job.instance_type = 'AutoScaleVmGroup' THEN autoscale_vmgroups.uuid
            ELSE null
        END instance_uuid
    from
        `cloud`.`async_job`
            left join
        `cloud`.`account` ON async_job.account_id = account.id
            left join
        `cloud`.`domain` ON domain.id = account.domain_id
            left join
        `cloud`.`user` ON async_job.user_id = user.id
            left join
        `cloud`.`volumes` ON async_job.instance_id = volumes.id
            left join
        `cloud`.`vm_template` ON async_job.instance_id = vm_template.id
            left join
        `cloud`.`vm_instance` ON async_job.instance_id = vm_instance.id
            left join
        `cloud`.`snapshots` ON async_job.instance_id = snapshots.id
            left join
        `cloud`.`host` ON async_job.instance_id = host.id
            left join
        `cloud`.`storage_pool` ON async_job.instance_id = storage_pool.id
            left join
        `cloud`.`user_ip_address` ON async_job.instance_id = user_ip_address.id
            left join
        `cloud`.`security_group` ON async_job.instance_id = security_group.id
            left join
        `cloud`.`physical_network` ON async_job.instance_id = physical_network.id
            left join
        `cloud`.`physical_network_traffic_types` ON async_job.instance_id = physical_network_traffic_types.id
            left join
        `cloud`.`physical_network_service_providers` ON async_job.instance_id = physical_network_service_providers.id
            left join
        `cloud`.`firewall_rules` ON async_job.instance_id = firewall_rules.id
            left join
        `cloud`.`account` acct ON async_job.instance_id = acct.id
            left join
        `cloud`.`user` us ON async_job.instance_id = us.id
            left join
        `cloud`.`static_routes` ON async_job.instance_id = static_routes.id
            left join
        `cloud`.`vpc_gateways` ON async_job.instance_id = vpc_gateways.id
            left join
        `cloud`.`counter` ON async_job.instance_id = counter.id
            left join
        `cloud`.`conditions` ON async_job.instance_id = conditions.id
            left join
        `cloud`.`autoscale_policies` ON async_job.instance_id = autoscale_policies.id
            left join
        `cloud`.`autoscale_vmprofiles` ON async_job.instance_id = autoscale_vmprofiles.id
            left join
        `cloud`.`autoscale_vmgroups` ON async_job.instance_id = autoscale_vmgroups.id;

DROP TABLE IF EXISTS `cloud`.`object_store`;
CREATE TABLE `cloud`.`object_store` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of object store',
  `object_provider_name` varchar(255) NOT NULL COMMENT 'id of object_store_provider',
  `protocol` varchar(255) NOT NULL COMMENT 'protocol of object store',
  `url` varchar(255) NOT NULL COMMENT 'url of the object store',
  `uuid` varchar(255) COMMENT 'uuid of object store',
  `created` datetime COMMENT 'date the object store first signed on',
  `removed` datetime COMMENT 'date removed if not null',
  `total_size` bigint unsigned COMMENT 'storage total size statistics',
  `used_bytes` bigint unsigned COMMENT 'storage available bytes statistics',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `cloud`.`object_store_details`;
CREATE TABLE `cloud`.`object_store_details` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `store_id` bigint unsigned NOT NULL COMMENT 'store the detail is related to',
  `name` varchar(255) NOT NULL COMMENT 'name of the detail',
  `value` varchar(255) NOT NULL COMMENT 'value of the detail',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_object_store_details__store_id` FOREIGN KEY `fk_object_store__store_id`(`store_id`) REFERENCES `object_store`(`id`) ON DELETE CASCADE,
  INDEX `i_object_store__name__value`(`name`(128), `value`(128))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `cloud`.`bucket`;
CREATE TABLE `cloud`.`bucket` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of bucket',
  `object_store_id` varchar(255) NOT NULL COMMENT 'id of object_store',
  `state` varchar(255) NOT NULL COMMENT 'state of the bucket',
  `uuid` varchar(255) COMMENT 'uuid of bucket',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain the bucket belongs to',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner of this bucket',
  `size` bigint unsigned COMMENT 'total size of bucket objects',
  `created` datetime COMMENT 'date the bucket was created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;