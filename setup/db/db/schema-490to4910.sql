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
-- Schema upgrade from 4.9.0 to 4.9.1.0;
--;

-- Fix default user role description
UPDATE `cloud`.`roles` SET `description`='Default user role' WHERE `id`=4 AND `role_type`='User' AND `description`='Default Root Admin role';


ALTER TABLE cloud.load_balancer_cert_map ENGINE=INNODB;
ALTER TABLE cloud.monitoring_services ENGINE=INNODB;
ALTER TABLE cloud.nic_ip_alias ENGINE=INNODB;
ALTER TABLE cloud.sslcerts ENGINE=INNODB;
ALTER TABLE cloud.op_lock ENGINE=INNODB;
ALTER TABLE cloud.op_nwgrp_work ENGINE=INNODB;

ALTER TABLE cloud_usage.quota_account ENGINE=INNODB;
