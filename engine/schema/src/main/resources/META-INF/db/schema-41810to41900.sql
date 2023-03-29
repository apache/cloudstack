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

--- Create table for Virtual Machine Schedule Entity

CREATE TABLE IF NOT EXISTS `cloud`.`vm_schedule` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) UNIQUE COMMENT 'UUID for the VM Schedule',
  `description` varchar(255) COMMENT 'description of VM Schedule',
  `action` varchar(40) NOT NULL COMMENT 'action Scheduled on VM',
  `schedule_type` varchar(40) NOT NULL COMMENT 'interval type of VM',
  `schedule` varchar(255) NOT NULL COMMENT 'schedule of VM',
  `scheduled_timestamp` DATE  COMMENT 'timestamp of VM Schedule',
  `timezone` varchar(255)  COMMENT 'timezone of VM',
  `state` varchar(40)  COMMENT 'state of VM',
  `tag` varchar(255)  COMMENT 'Tag Value of VM Schedule',
  `async_job_id` bigint(20) unsigned COMMENT 'async job id',
  `vm_id` bigint(20) unsigned NOT NULL COMMENT 'virtual machine id',
  INDEX(`vm_id`),
  PRIMARY KEY (`id`),
  KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
