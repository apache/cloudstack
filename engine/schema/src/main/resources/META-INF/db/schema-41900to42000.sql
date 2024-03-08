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
ALTER TABLE `resource_count`
DROP INDEX `i_resource_count__type_accountId`,
DROP INDEX `i_resource_count__type_domaintId`,
ADD UNIQUE INDEX `i_resource_count__type_tag_accountId` (`type`,`tag`,`account_id`),
ADD UNIQUE INDEX `i_resource_count__type_tag_domaintId` (`type`,`tag`,`domain_id`);

-- Update Default System offering for Router to 512MiB
UPDATE `cloud`.`service_offering` SET ram_size = 512 WHERE unique_name IN ("Cloud.Com-SoftwareRouter", "Cloud.Com-SoftwareRouter-Local",
                                                                           "Cloud.Com-InternalLBVm", "Cloud.Com-InternalLBVm-Local",
                                                                           "Cloud.Com-ElasticLBVm", "Cloud.Com-ElasticLBVm-Local")
                                                    AND system_use = 1 AND ram_size < 512;

-- Create command_timeout table and populate it
CREATE TABLE IF NOT EXISTS `cloud`.`command_timeout` (
     id bigint(20) unsigned not null auto_increment primary key,
     command_classpath text unique key,
     timeout int not null,
     created datetime not null,
     updated datetime not null
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `cloud`.`command_timeout` (command_classpath, timeout, created, updated)
VALUES
    ('org.apache.cloudstack.ca.SetupCertificateCommand', 60, now(), now()),
    ('com.cloud.agent.api.CheckS2SVpnConnectionsCommand', 30, now(), now()),
    ('com.cloud.agent.api.CheckOnHostCommand', 20, now(), now()),
    ('com.cloud.agent.api.CheckVirtualMachineCommand', 20, now(), now()),
    ('com.cloud.agent.api.CheckRouterCommand', 30, now(), now()),
    ('com.cloud.agent.api.CheckHealthCommand', 50, now(), now()),
    ('com.cloud.agent.api.routing.GetAutoScaleMetricsCommand', 30, now(), now()),
    ('org.apache.cloudstack.ca.SetupKeyStoreCommand', 30, now(), now()),
    ('org.apache.cloudstack.storage.command.browser.ListDataStoreObjectsCommand', 15, now(), now());
