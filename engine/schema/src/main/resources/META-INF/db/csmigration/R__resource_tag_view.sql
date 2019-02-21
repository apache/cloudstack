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
DROP VIEW IF EXISTS `cloud`.`resource_tag_view`;
CREATE VIEW `cloud`.`resource_tag_view` AS
    select
        resource_tags.id,
        resource_tags.uuid,
        resource_tags.key,
        resource_tags.value,
        resource_tags.resource_id,
        resource_tags.resource_uuid,
        resource_tags.resource_type,
        resource_tags.customer,
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
    from
        `cloud`.`resource_tags`
            inner join
        `cloud`.`account` ON resource_tags.account_id = account.id
            inner join
        `cloud`.`domain` ON resource_tags.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.project_account_id = resource_tags.account_id;