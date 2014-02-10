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


USE cloudbridge;

-- This file (and cloudbridge_offering_alter.sql) can be applied to an existing cloudbridge 
-- database.   It is used to manage the mappings from the Amazon EC2 offering strings to 
-- cloudstack service offering identifers.
--
SET foreign_key_checks = 0;

DROP TABLE IF EXISTS offering_bundle;

-- AmazonEC2Offering  - string name of an EC2 AMI capability (e.g. "m1.small")
-- CloudStackOffering - string name of the cloud stack service offering identifer (e.g. "1" )
--
CREATE TABLE offering_bundle (
	ID                 INTEGER NOT NULL AUTO_INCREMENT,
	AmazonEC2Offering  VARCHAR(100) NOT NULL,
	CloudStackOffering VARCHAR(20)  NOT NULL,
	PRIMARY KEY(ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET foreign_key_checks = 1;

