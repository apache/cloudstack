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

-- Remove leftovers from awsapi
CALL `cloud`.`IDEMPOTENT_DROP_FOREIGN_KEY`('cloud.account','fk_account__default_zone_id');
CALL `cloud`.`IDEMPOTENT_DROP_COLUMN`('cloud.account', 'default_zone_id');
CALL `cloud`.`IDEMPOTENT_DROP_COLUMN`('cloud_usage.account', 'default_zone_id');

DELETE FROM `cloud`.`configuration` WHERE name='enable.ec2.api';
DELETE FROM `cloud`.`configuration` WHERE name='enable.s3.api';
