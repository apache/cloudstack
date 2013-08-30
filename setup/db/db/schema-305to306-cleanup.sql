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


#Schema cleanup from 3.0.5 to 3.0.6;


DELETE FROM `cloud`.`configuration` where `cloud`.`configuration`.`name`="vm.hostname.flag";
DELETE FROM `cloud`.`storage_pool_host_ref` WHERE `cloud`.`storage_pool_host_ref`.`pool_id` IN (SELECT `cloud`.`storage_pool`.`id` FROM `cloud`.`storage_pool` WHERE `cloud`.`storage_pool`.`removed` IS NOT NULL);

ALTER TABLE `cloud`.`sync_queue` DROP COLUMN queue_proc_msid;
ALTER TABLE `cloud`.`sync_queue` DROP COLUMN queue_proc_time;