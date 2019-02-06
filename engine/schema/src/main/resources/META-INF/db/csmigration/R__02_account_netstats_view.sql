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
DROP VIEW IF EXISTS `cloud`.`account_netstats_view`;
CREATE VIEW `cloud`.`account_netstats_view` AS
    SELECT
        account_id,
        sum(net_bytes_received) + sum(current_bytes_received) as bytesReceived,
        sum(net_bytes_sent) + sum(current_bytes_sent) as bytesSent
    FROM
        `cloud`.`user_statistics`
    group by account_id;