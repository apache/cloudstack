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

-- cloud_usage.quota_summary_view source

-- Create view for quota summary
DROP VIEW IF EXISTS `cloud_usage`.`quota_summary_view`;
CREATE VIEW `cloud_usage`.`quota_summary_view` AS
SELECT
    cloud_usage.quota_account.account_id AS account_id,
    cloud_usage.quota_account.quota_balance AS quota_balance,
    cloud_usage.quota_account.quota_balance_date AS quota_balance_date,
    cloud_usage.quota_account.quota_enforce AS quota_enforce,
    cloud_usage.quota_account.quota_min_balance AS quota_min_balance,
    cloud_usage.quota_account.quota_alert_date AS quota_alert_date,
    cloud_usage.quota_account.quota_alert_type AS quota_alert_type,
    cloud_usage.quota_account.last_statement_date AS last_statement_date,
    cloud.account.uuid AS account_uuid,
    cloud.account.account_name AS account_name,
    cloud.account.state AS account_state,
    cloud.account.removed AS account_removed,
    cloud.domain.id AS domain_id,
    cloud.domain.uuid AS domain_uuid,
    cloud.domain.name AS domain_name,
    cloud.domain.path AS domain_path,
    cloud.domain.removed AS domain_removed,
    cloud.projects.uuid AS project_uuid,
    cloud.projects.name AS project_name,
    cloud.projects.removed AS project_removed
FROM
    cloud_usage.quota_account
        INNER JOIN cloud.account ON (cloud.account.id = cloud_usage.quota_account.account_id)
        INNER JOIN cloud.domain ON (cloud.domain.id = cloud.account.domain_id)
        LEFT JOIN cloud.projects ON (cloud.account.type = 5 AND cloud.account.id = cloud.projects.project_account_id);
