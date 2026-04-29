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

-- SAML keystore for testing, allows testing on ssocirlce and other public IdPs
-- with pre-seeded SP metadata
USE cloud;

-- Enable SAML plugin for developers by default
INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'SAML2-PLUGIN',
            'saml2.enabled', 'true')
            ON DUPLICATE KEY UPDATE value=VALUES(value);

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'SAML2-PLUGIN',
            'saml2.default.idpid', 'https://idp.bhaisaab.org/idp/shibboleth')
            ON DUPLICATE KEY UPDATE value=VALUES(value);

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'SAML2-PLUGIN',
            'saml2.idp.metadata.url', 'http://idp.bhaisaab.org/idp/shibboleth')
            ON DUPLICATE KEY UPDATE value=VALUES(value);

-- Enable LDAP source
INSERT INTO `cloud`.`ldap_configuration` (hostname, port)
            VALUES ('idp.bhaisaab.org', 389);

-- Fix ldap configs
INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'ldap.basedn', 'ou=people,dc=idp,dc=bhaisaab,dc=org')
            ON DUPLICATE KEY UPDATE value=VALUES(value);

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'ldap.bind.principal', 'cn=admin,dc=idp,dc=bhaisaab,dc=org')
            ON DUPLICATE KEY UPDATE value=VALUES(value);

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'ldap.bind.password', 'password')
            ON DUPLICATE KEY UPDATE value=VALUES(value);

-- Add default set of certificates for testing
LOCK TABLES `keystore` WRITE;
/*!40000 ALTER TABLE `keystore` DISABLE KEYS */;
INSERT INTO `keystore` VALUES (1,'SAMLSP_KEYPAIR','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','samlsp-keypair',NULL),(2,'SAMLSP_X509CERT','rO0ABXNyAC1qYXZhLnNlY3VyaXR5LmNlcnQuQ2VydGlmaWNhdGUkQ2VydGlmaWNhdGVSZXCJJ2qdya48DAIAAlsABGRhdGF0AAJbQkwABHR5cGV0ABJMamF2YS9sYW5nL1N0cmluZzt4cHVyAAJbQqzzF/gGCFTgAgAAeHAAAASzMIIErzCCApcCBgFNmkdlAzANBgkqhkiG9w0BAQsFADAbMRkwFwYDVQQDExBBcGFjaGVDbG91ZFN0YWNrMB4XDTE1MDUyNzExMjc1OVoXDTE4MDUyODExMjc1OVowGzEZMBcGA1UEAxMQQXBhY2hlQ2xvdWRTdGFjazCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBANAOKu0BqNOtJxcJSBeuw4GBD77L4hRzLNfBQYRgGUz2QEKZx2Vu3nO5hDyk6+g5DPoIBWpKNg1uduDWmHftl+/+3MZSDZXlykHkF8TK3YpjSYEU1vBrQsqcIBiUGVqubhdkX6hnSZofpx1ugd0eISo1GxzKWddgDbMcpTdigR85H71pyXjPng8DK4+Ov1JZQ9MDJh/fbqLSr6HcnidPoXAEUr7IpILU95Ub++JhDSi1TVRQORDelfzWp8l83/oiHtbi/vcuBSVmxMEzOW7+QnMy4/lZzkiRToy37wQcxl4uc3RkSuQyTQMJATXiEbxbtLz+KdQmnAmWZvkTrfhmry7QjXNVSZKYAaq++ookEaepjlf+PZuwDwHaoWytzFqwiWQppYu9j4C9cPlfQ4fAhnNFu64K2C5LPrvULsLCiTxHc3570BMMeYb2ALXQ0loHp1QED2nZR0DzZNHvfHqcZ7yksmcbJ5wRB/HPX6m09UAQ2PVGXdavC791s939fqKeVim7CWydaLctiuSC5P/1bdna66rk9NVPTjNzJ6/7nSBWcM/NOs0Jl8emYw1AipkYWYUrPib5VRmbNk52DVXHnW3363uIcYeoWTr++eqpaLIS5MQUBoL2SubVU3Ma+ejkvI2bkOT0kYAcV+OA1X76+IZOlI8t4R4yeW8+5AnLk4kzAgMBAAEwDQYJKoZIhvcNAQELBQADggIBAHZWSGpypDmQLQWr2FCVQUnulbPuMMJ0sCH0rNLGLe8qNbZ0YeAuWFsg7+0kVGZ4OuDgioIhD0h3Q3huZtF/WF81eyZqPyVfkXG8egjK58AzMDPHZECeoSVGUCZuq3wjmbnT2sLLDvr8RrzMbbCEvkrYHWivQ18Lbd3eWYYnDbXZRy9GuSWrA9cMqXVYjSTxam9Kel33BIF6CAlMQN5o11oiAv+ciNoxHqGh+8xX3kFKP+x+SRt40NOEs537lEpj/6KdLvd/bP6J4K94jAX3lsdg6zDaBiQWl7P3t50AKtP384Qsb/33uXcbTyw/TkzvPcbmsgTbEUTZIOv44CxMstFrUCyT7ptrzLvDk7Iy2cMgWghULgDvKT3esPE9pleyHG8bkjGt9ypDF/Lmp7j/kILYbF7eq1wIbHOSam4p8WyddVsW4nesu6fqLiCGXum9paChIfvL3To/VHFFKduhJd0Y7LMgWO7pXxWh7XfgRmzQaEN1eJmj5315HEYTS2wXWjptwYDrhiobKuCbpADfOQks8xNKJFLMnXp+IvAqz+ZjkNOz60MLuQ3hvKLTo6nQcTYTfZZxo3Aap30/hA2GtxxSXK/xpBDm58jcVoudgCdxML/OqERBfcADBLvIw5h9+DlXjPUg25IefU0oA336YtnzftJ6cfQfatrc0tBqNEeXdAAFWC41MDk=','','samlsp-x509cert',NULL);
/*!40000 ALTER TABLE `keystore` ENABLE KEYS */;
UNLOCK TABLES;
