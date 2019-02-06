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
DROP VIEW IF EXISTS `cloud`.`security_group_view`;
CREATE VIEW `cloud`.`security_group_view` AS
    select
        security_group.id id,
        security_group.name name,
        security_group.description description,
        security_group.uuid uuid,
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
        projects.name project_name,
        security_group_rule.id rule_id,
        security_group_rule.uuid rule_uuid,
        security_group_rule.type rule_type,
        security_group_rule.start_port rule_start_port,
        security_group_rule.end_port rule_end_port,
        security_group_rule.protocol rule_protocol,
        security_group_rule.allowed_network_id rule_allowed_network_id,
        security_group_rule.allowed_ip_cidr rule_allowed_ip_cidr,
        security_group_rule.create_status rule_create_status,
        resource_tags.id tag_id,
        resource_tags.uuid tag_uuid,
        resource_tags.key tag_key,
        resource_tags.value tag_value,
        resource_tags.domain_id tag_domain_id,
        resource_tags.account_id tag_account_id,
        resource_tags.resource_id tag_resource_id,
        resource_tags.resource_uuid tag_resource_uuid,
        resource_tags.resource_type tag_resource_type,
        resource_tags.customer tag_customer,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`security_group`
            left join
        `cloud`.`security_group_rule` ON security_group.id = security_group_rule.security_group_id
            inner join
        `cloud`.`account` ON security_group.account_id = account.id
            inner join
        `cloud`.`domain` ON security_group.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.project_account_id = security_group.account_id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = security_group.id
            and resource_tags.resource_type = 'SecurityGroup'
            left join
        `cloud`.`async_job` ON async_job.instance_id = security_group.id
            and async_job.instance_type = 'SecurityGroup'
            and async_job.job_status = 0;