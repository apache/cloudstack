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

-- VIEW `cloud`.`backup_report_view`;

DROP VIEW IF EXISTS `cloud`.`backup_report_view`;
CREATE VIEW `cloud`.`backup_report_view` AS
SELECT dc.id AS zone_id,
       dc.uuid AS zone_uuid,
       dc.name AS zone_name,
       d.id AS domain_id,
       d.uuid AS domain_uuid,
       d.name AS domain_name,
       a.id AS account_id,
       a.uuid AS account_uuid,
       a.account_name,
       p.uuid AS project_uuid,
       p.name as project_name,
       vi.id AS vm_id,
       vi.uuid AS vm_uuid,
       vi.name AS vm_name,
       b.id AS backup_id,
       b.uuid AS backup_uuid,
       b.name AS backup_name,
       bo.name AS offering_name,
       b.size,
       b.status,
       b.failure_reason,
       b.logid,
       b.date,
       b.removed
FROM backups b
         LEFT JOIN vm_instance vi ON b.vm_id = vi.id
         LEFT JOIN account a ON b.account_id = a.id
         LEFT JOIN projects p ON b.account_id = p.project_account_id
         LEFT JOIN domain d ON b.domain_id = d.id
         LEFT JOIN data_center dc ON b.zone_id = dc.id
         LEFT JOIN backup_offering bo ON b.backup_offering_id = bo.id;
