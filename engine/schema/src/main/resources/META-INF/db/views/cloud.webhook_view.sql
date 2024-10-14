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

-- VIEW `cloud`.`webhook_view`;

DROP VIEW IF EXISTS `cloud`.`webhook_view`;
CREATE VIEW `cloud`.`webhook_view` AS
    SELECT
        webhook.id,
        webhook.uuid,
        webhook.name,
        webhook.description,
        webhook.state,
        webhook.payload_url,
        webhook.secret_key,
        webhook.ssl_verification,
        webhook.scope,
        webhook.created,
        webhook.removed,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name
    FROM
        `cloud`.`webhook`
            INNER JOIN
        `cloud`.`account` ON webhook.account_id = account.id
            INNER JOIN
        `cloud`.`domain` ON webhook.domain_id = domain.id
            LEFT JOIN
        `cloud`.`projects` ON projects.project_account_id = webhook.account_id;
