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

-- VIEW `cloud`.`data_center_view`;

DROP VIEW IF EXISTS `cloud`.`data_center_view`;

CREATE VIEW `cloud`.`data_center_view` AS
select
    data_center.id,
    data_center.uuid,
    data_center.name,
    data_center.is_security_group_enabled,
    data_center.is_local_storage_enabled,
    data_center.description,
    data_center.dns1,
    data_center.dns2,
    data_center.ip6_dns1,
    data_center.ip6_dns2,
    data_center.internal_dns1,
    data_center.internal_dns2,
    data_center.guest_network_cidr,
    data_center.domain,
    data_center.networktype,
    data_center.allocation_state,
    data_center.zone_token,
    data_center.dhcp_provider,
    data_center.type,
    data_center.removed,
    data_center.sort_key,
    domain.id domain_id,
    domain.uuid domain_uuid,
    domain.name domain_name,
    domain.path domain_path,
    dedicated_resources.affinity_group_id,
    dedicated_resources.account_id,
    affinity_group.uuid affinity_group_uuid
from
    `cloud`.`data_center`
        left join
    `cloud`.`domain` ON data_center.domain_id = domain.id
        left join
    `cloud`.`dedicated_resources` ON data_center.id = dedicated_resources.data_center_id
        left join
    `cloud`.`affinity_group` ON dedicated_resources.affinity_group_id = affinity_group.id;
