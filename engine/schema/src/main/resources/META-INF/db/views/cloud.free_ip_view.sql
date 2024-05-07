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

-- cloud.free_ip_view source


DROP VIEW IF EXISTS `cloud`.`free_ip_view`;

CREATE VIEW `cloud`.`free_ip_view` AS
select
    count(`user_ip_address`.`id`) AS `free_ip`
from
    (`user_ip_address`
join `vlan` on
    (((`vlan`.`id` = `user_ip_address`.`vlan_db_id`)
        and (`vlan`.`vlan_type` = 'VirtualNetwork'))))
where
    (`user_ip_address`.`state` = 'Free');
