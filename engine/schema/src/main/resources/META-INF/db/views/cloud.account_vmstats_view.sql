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

-- cloud.account_vmstats_view source


DROP VIEW IF EXISTS `cloud`.`account_vmstats_view`;

CREATE VIEW `cloud`.`account_vmstats_view` AS
select
    `vm_instance`.`account_id` AS `account_id`,
    `vm_instance`.`state` AS `state`,
    count(0) AS `vmcount`
from
    `vm_instance`
where
    ((`vm_instance`.`vm_type` = 'User')
        and (`vm_instance`.`removed` is null))
group by
    `vm_instance`.`account_id`,
    `vm_instance`.`state`;
