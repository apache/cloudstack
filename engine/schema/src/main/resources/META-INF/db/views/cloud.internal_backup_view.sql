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

-- VIEW `cloud`.`internal_backup_view`;

DROP VIEW IF EXISTS `cloud`.`internal_backup_view`;
CREATE VIEW `cloud`.`internal_backup_view` AS
SELECT  b.id,
        b.uuid,
        b.vm_id,
        b.backed_volumes,
        b.type,
        b.date,
        b.status,
        b.compression_status,
        b.backup_offering_id,
        b.size,
        b.protected_size,
        b.zone_id,
        MAX(CASE WHEN bd.name = 'image_store_id' THEN bd.value END) image_store_id,
        MAX(CASE WHEN bd.name = 'parent_id' THEN bd.value END) parent_id,
        MAX(CASE WHEN bd.name = 'end_of_chain' THEN bd.value END) end_of_chain,
        MAX(CASE WHEN bd.name = 'current' THEN bd.value END) current,
        COALESCE(MAX(CASE WHEN bd.name = 'isolated' THEN bd.value END), 'false') isolated,
        nbpr.volume_id,
        nbsr.path image_store_path
FROM    backups b
LEFT    JOIN backup_details bd ON b.id = bd.backup_id
LEFT    JOIN backup_offering bo ON b.backup_offering_id = bo.id
LEFT    JOIN internal_backup_store_ref nbsr ON b.id = nbsr.backup_id
LEFT    JOIN internal_backup_pool_ref nbpr ON nbpr.volume_id = nbsr.volume_id
WHERE   bo.provider='kboss'
GROUP BY b.id, nbsr.volume_id;
