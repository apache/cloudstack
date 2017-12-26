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

ALTER table snapshots add column `data_center_id` bigint unsigned NOT NULL ;
ALTER table snapshots add column `domain_id` bigint unsigned NOT NULL;
ALTER table snapshots add column `disk_offering_id` bigint unsigned NOT NULL;
ALTER table snapshots add column `size` bigint unsigned NOT NULL;
ALTER table snapshots add column `version` varchar(32) DEFAULT '2.1';
ALTER table snapshots add column `hypervisor_type` varchar(32) DEFAULT 'XenServer';

DELETE FROM snapshot_policy where id=1;
UPDATE snapshots s, volumes v SET s.data_center_id=v.data_center_id, s.domain_id=v.domain_id, s.disk_offering_id=v.disk_offering_id, s.size=v.size WHERE s.volume_id = v.id;
UPDATE snapshots s, snapshot_policy sp, snapshot_policy_ref spr SET s.snapshot_type=sp.interval+3 WHERE s.id=spr.snap_id and spr.policy_id=sp.id;

DROP table snapshot_policy_ref;
