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

