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

-- VIEW `cloud`.`dns_zone_view`;

DROP VIEW IF EXISTS `cloud`.`dns_zone_view`;
CREATE VIEW `cloud`.`dns_zone_view` AS
    SELECT
        zone.id,
        zone.uuid,
        zone.name,
        zone.dns_server_id,
        zone.state,
        zone.description,
        server.uuid dns_server_uuid,
        server.name dns_server_name,
        server_account.account_name dns_server_account_name,
        account.account_name account_name,
        domain.name domain_name,
        domain.uuid domain_uuid,
        domain.path domain_path
    FROM
        `cloud`.`dns_zone` zone
            INNER JOIN
        `cloud`.`dns_server` server ON zone.dns_server_id = server.id
            INNER JOIN
        `cloud`.`account` server_account ON server.account_id = server_account.id
            INNER JOIN
        `cloud`.`account` account ON zone.account_id = account.id
            INNER JOIN
        `cloud`.`domain` domain ON zone.domain_id = domain.id;
