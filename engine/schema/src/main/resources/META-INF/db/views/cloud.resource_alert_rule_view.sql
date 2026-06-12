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

-- VIEW `cloud`.`resource_alert_rule_view`;

DROP VIEW IF EXISTS `cloud`.`resource_alert_rule_view`;
CREATE VIEW `cloud`.`resource_alert_rule_view` AS
    SELECT
        r.id,
        r.uuid,
        r.name,
        r.resource_type,
        r.resource_id,
        r.metric,
        r.condition_operator,
        r.threshold,
        r.severity,
        r.message,
        r.email,
        r.reset_interval,
        r.created,
        r.updated,
        r.removed,
        a.id account_id,
        a.uuid account_uuid,
        a.account_name,
        a.type account_type,
        d.id domain_id,
        d.uuid domain_uuid,
        d.name domain_name,
        d.path domain_path
    FROM `cloud`.`resource_alert_rules` r
    INNER JOIN `cloud`.`account` a ON r.account_id = a.id
    INNER JOIN `cloud`.`domain` d ON r.domain_id = d.id;
