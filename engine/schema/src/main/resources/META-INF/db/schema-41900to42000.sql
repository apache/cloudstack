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
-- Schema upgrade from 4.19.0.0 to 4.20.0.0
--;

CREATE TABLE IF NOT EXISTS `cloud`.`command_timeout` (
     id bigint(20) unsigned not null auto_increment primary key,
     command_classpath text unique key,
     timeout int not null,
     created datetime not null,
     updated datetime not null
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `cloud`.`command_timeout` (command_classpath, timeout, created, updated)
VALUES
    ('org.apache.cloudstack.ca.SetupCertificateCommand', 60, now(), now()),
    ('com.cloud.agent.api.CheckS2SVpnConnectionsCommand', 30, now(), now()),
    ('com.cloud.agent.api.CheckOnHostCommand', 20, now(), now());
