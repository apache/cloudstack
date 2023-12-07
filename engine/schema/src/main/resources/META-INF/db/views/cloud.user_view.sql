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

-- VIEW `cloud`.`user_view`;

DROP VIEW IF EXISTS `cloud`.`user_view`;

CREATE VIEW `cloud`.`user_view` AS
select
    user.id,
    user.uuid,
    user.username,
    user.password,
    user.firstname,
    user.lastname,
    user.email,
    user.state,
    user.api_key,
    user.secret_key,
    user.created,
    user.removed,
    user.timezone,
    user.registration_token,
    user.is_registered,
    user.incorrect_login_attempts,
    user.source,
    user.default,
    account.id account_id,
    account.uuid account_uuid,
    account.account_name account_name,
    account.type account_type,
    account.role_id account_role_id,
    domain.id domain_id,
    domain.uuid domain_uuid,
    domain.name domain_name,
    domain.path domain_path,
    async_job.id job_id,
    async_job.uuid job_uuid,
    async_job.job_status job_status,
    async_job.account_id job_account_id,
    user.is_user_2fa_enabled is_user_2fa_enabled
from
    `cloud`.`user`
        inner join
    `cloud`.`account` ON user.account_id = account.id
        inner join
    `cloud`.`domain` ON account.domain_id = domain.id
        left join
    `cloud`.`async_job` ON async_job.instance_id = user.id
        and async_job.instance_type = 'User'
        and async_job.job_status = 0;
