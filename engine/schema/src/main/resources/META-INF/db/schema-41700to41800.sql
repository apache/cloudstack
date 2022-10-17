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
-- Schema upgrade from 4.17.0.0 to 4.18.0.0
--;

----- PR Quota custom tariffs #5909---
-- Create column 'uuid'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `uuid` varchar(40);

UPDATE  cloud_usage.quota_tariff
SET     uuid = UUID()
WHERE   uuid is null;

ALTER TABLE cloud_usage.quota_tariff
    MODIFY      `uuid` varchar(40) NOT NULL;


-- Create column 'name'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `name` text
    COMMENT     'A name, deﬁned by the user, to the tariff. This column will be used as identiﬁer along the tariff updates.';

UPDATE  cloud_usage.quota_tariff
SET     name = case when effective_on <= now() then usage_name else concat(usage_name, '-', id) end
WHERE   name is null;

ALTER TABLE cloud_usage.quota_tariff
    MODIFY      `name` text NOT NULL;


-- Create column 'description'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `description` text DEFAULT NULL
    COMMENT     'The description of the tariff.';


-- Create column 'activation_rule'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `activation_rule` text DEFAULT NULL
    COMMENT     'JS expression that defines when the tariff should be activated.';


-- Create column 'removed'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `removed` datetime DEFAULT NULL;


-- Create column 'end_date'
ALTER TABLE cloud_usage.quota_tariff
    ADD COLUMN  `end_date` datetime DEFAULT NULL
    COMMENT     'Defines the end date of the tarrif.';


-- Change usage unit to right unit
UPDATE  cloud_usage.quota_tariff
SET     usage_unit = 'Compute*Month'
WHERE   usage_unit = 'Compute-Month';

UPDATE  cloud_usage.quota_tariff
SET     usage_unit = 'IP*Month'
WHERE   usage_unit = 'IP-Month';

UPDATE  cloud_usage.quota_tariff
SET     usage_unit = 'GB*Month'
WHERE   usage_unit = 'GB-Month';

UPDATE  cloud_usage.quota_tariff
SET     usage_unit = 'Policy*Month'
WHERE   usage_unit = 'Policy-Month';

----- PR Quota custom tariffs #5909 -----
