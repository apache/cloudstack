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
-- Schema upgrade cleanup from 4.9.3.0 to 4.10.0.0;
--;

DELETE FROM `cloud`.`configuration` WHERE name='consoleproxy.loadscan.interval';

DELETE FROM `cloud`.`host_details` where name = 'vmName' and  value in (select name from `cloud`.`vm_instance`  where state = 'Expunging' and hypervisor_type ='BareMetal');
