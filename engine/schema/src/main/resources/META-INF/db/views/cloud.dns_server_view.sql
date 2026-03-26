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

-- VIEW `cloud`.`dns_server_view`;

DROP VIEW IF EXISTS `cloud`.`dns_server_view`;
CREATE VIEW `cloud`.`dns_server_view` AS
    SELECT
        dns.id,
        dns.uuid,
        dns.name,
        dns.provider_type,
        dns.url,
        dns.port,
        dns.name_servers,
        dns.is_public,
        dns.public_domain_suffix,
        dns.state,
        dns.created,
        dns.removed,
        account.account_name account_name,
        domain.name domain_name,
        domain.uuid domain_uuid,
        domain.path domain_path
    FROM
        `cloud`.`dns_server` dns
            INNER JOIN
        `cloud`.`account` account ON dns.account_id = account.id
            INNER JOIN
        `cloud`.`domain` domain ON dns.domain_id = domain.id;
