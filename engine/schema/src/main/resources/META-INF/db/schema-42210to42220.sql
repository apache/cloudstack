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

--;
-- Schema upgrade from 4.22.1.0 to 4.22.2.0
--;

-- Composite index on nics(instance_id, removed) to fix wrong-index pick in user_vm_view.
-- The optimizer picks i_nics__removed over fk_nics__instance_id for the
-- LEFT JOIN nics ON (vm_instance.id = nics.instance_id AND nics.removed IS NULL)
-- because neither single-column index covers both predicates.
CALL `cloud`.`IDEMPOTENT_ADD_INDEX`('i_nics__instance_id_removed', 'cloud.nics', '(instance_id, removed)');
-- account_view performance: covers all 10 resource_limit LEFT JOINs
-- which filter on (account_id = ? AND type = ? AND tag IS NULL)
CALL `cloud`.`IDEMPOTENT_ADD_INDEX`('i_resource_limit__acct_type_tag', 'cloud.resource_limit', '(account_id, type, tag)');
-- account_vmstats_view: enables index-only scan for GROUP BY account_id, state
-- WHERE vm_type = 'User' AND removed IS NULL
CALL `cloud`.`IDEMPOTENT_ADD_INDEX`('i_vm_instance__account_id_state', 'cloud.vm_instance', '(account_id, state)');
-- free_ip_view: covers WHERE state = 'Free' JOIN vlan ON vlan.id = vlan_db_id
CALL `cloud`.`IDEMPOTENT_ADD_INDEX`('i_user_ip_address__state_vlan', 'cloud.user_ip_address', '(state, vlan_db_id)');
-- Prometheus/AlertManager IP count queries: covers WHERE data_center_id = ? AND removed IS NULL
CALL `cloud`.`IDEMPOTENT_ADD_INDEX`('i_user_ip_address__dc_removed_vlan', 'cloud.user_ip_address', '(data_center_id, removed, vlan_db_id)');
-- event table: compound index for frequent Compute API queries
-- (WHERE archived = 0 AND level = ? ORDER BY created)
CALL `cloud`.`IDEMPOTENT_ADD_INDEX`('i_event__archived_level_created', 'cloud.event', '(archived, level, created)');
-- event table: default listEvents path (account + archived + sort)
CALL `cloud`.`IDEMPOTENT_ADD_INDEX`('i_event__account_archived_created', 'cloud.event', '(account_id, archived, created DESC)');
-- event table: domain-scoped ACL queries (completely unindexed)
CALL `cloud`.`IDEMPOTENT_ADD_INDEX`('i_event__domain_id', 'cloud.event', '(domain_id)');
-- event table: resource-based event lookups (added in 4.16.1 with no index)
CALL `cloud`.`IDEMPOTENT_ADD_INDEX`('i_event__resource', 'cloud.event', '(resource_id, resource_type)');
-- event table: start_id for related-event queries and event_view self-join
CALL `cloud`.`IDEMPOTENT_ADD_INDEX`('i_event__start_id', 'cloud.event', '(start_id)');
-- host table: covers findByName lookups (WHERE name = ? AND removed IS NULL)
CALL `cloud`.`IDEMPOTENT_ADD_INDEX`('i_host__name_removed', 'cloud.host', '(name, removed)');
