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

-- VIEW `cloud`.`dns_nic_view`;

DROP VIEW IF EXISTS `cloud`.`dns_nic_view`;
CREATE VIEW `cloud`.`dns_nic_view` AS
SELECT
    n.id AS id,
    n.uuid AS uuid,
    n.instance_id AS instance_id,
    n.network_id AS network_id,
    n.ip4_address AS ip4_address,
    n.ip6_address AS ip6_address,
    n.removed AS removed,
    nd.value AS nic_dns_url,
    map.dns_zone_id AS dns_zone_id,
    map.sub_domain AS sub_domain
FROM
    `cloud`.`nics` n
        INNER JOIN
    `cloud`.`dns_zone_network_map` map ON n.network_id = map.network_id
        LEFT JOIN
    `cloud`.`nic_details` nd ON n.id = nd.nic_id AND nd.name = 'nicdnsrecord'
WHERE
    map.removed IS NULL;
