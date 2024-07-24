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
-- Schema upgrade from 2.2.7 to 2.2.8;
--;

--;
-- Cleanup usage records for bug # 10727;
--;


create table `cloud_usage`.`temp_usage` (  `vol_id` bigint unsigned, `created` DATETIME);

insert into `cloud_usage`.`temp_usage` (vol_id, created) select id, max(created) from `cloud_usage`.`usage_volume` where deleted is null group by id having count(id) > 1;

delete `cloud_usage`.`usage_volume` from `cloud_usage`.`usage_volume` inner join `cloud_usage`.`temp_usage` where `cloud_usage`.`usage_volume`.created = `cloud_usage`.`temp_usage`.created and `cloud_usage`.`usage_volume`.id = `cloud_usage`.`temp_usage`.vol_id and `cloud_usage`.`usage_volume`.deleted is null;

drop table `cloud_usage`.`temp_usage`;

update `cloud_usage`.`cloud_usage` set raw_usage = (raw_usage % 24) where usage_type =6 and raw_usage > 24 and (raw_usage % 24) <> 0;
update `cloud_usage`.`cloud_usage` set raw_usage = 24 where usage_type =6 and raw_usage > 24 and (raw_usage % 24) = 0;
