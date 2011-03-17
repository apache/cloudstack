-- MySQL dump 10.13  Distrib 5.1.48, for redhat-linux-gnu (x86_64)
--
-- Host: localhost    Database: cloud
-- ------------------------------------------------------
-- Server version	5.1.48

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `account`
--

DROP TABLE IF EXISTS `account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `account` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `account_name` varchar(100) DEFAULT NULL COMMENT 'an account name set by the creator of the account, defaults to username for single accounts',
  `type` int(1) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned DEFAULT NULL,
  `state` varchar(10) NOT NULL DEFAULT 'enabled',
  `removed` datetime DEFAULT NULL COMMENT 'date removed',
  `cleanup_needed` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `i_account__domain_id` (`domain_id`),
  KEY `i_account__cleanup_needed` (`cleanup_needed`),
  KEY `i_account__removed` (`removed`),
  KEY `i_account__account_name__domain_id__removed` (`account_name`,`domain_id`,`removed`),
  CONSTRAINT `fk_account__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account`
--

LOCK TABLES `account` WRITE;
/*!40000 ALTER TABLE `account` DISABLE KEYS */;
INSERT INTO `account` VALUES (1,'system',1,1,'enabled',NULL,0),(2,'admin',1,1,'enabled',NULL,0);
/*!40000 ALTER TABLE `account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `account_vlan_map`
--

DROP TABLE IF EXISTS `account_vlan_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `account_vlan_map` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) unsigned DEFAULT NULL COMMENT 'account id. foreign key to account table',
  `vlan_db_id` bigint(20) unsigned NOT NULL COMMENT 'database id of vlan. foreign key to vlan table',
  `domain_id` bigint(20) unsigned DEFAULT NULL COMMENT 'domain id. foreign key to domain table',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `i_account_vlan_map__account_id` (`account_id`),
  KEY `i_account_vlan_map__domain_id` (`domain_id`),
  KEY `i_account_vlan_map__vlan_id` (`vlan_db_id`),
  CONSTRAINT `fk_account_vlan_map__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_account_vlan_map__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_account_vlan_map__vlan_id` FOREIGN KEY (`vlan_db_id`) REFERENCES `vlan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `account_vlan_map`
--

LOCK TABLES `account_vlan_map` WRITE;
/*!40000 ALTER TABLE `account_vlan_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `account_vlan_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alert`
--

DROP TABLE IF EXISTS `alert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `alert` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `type` int(1) unsigned NOT NULL,
  `pod_id` bigint(20) unsigned DEFAULT NULL,
  `data_center_id` bigint(20) unsigned NOT NULL,
  `subject` varchar(999) DEFAULT NULL COMMENT 'according to SMTP spec, max subject length is 1000 including the CRLF character, so allow enough space to fit long pod/zone/host names',
  `sent_count` int(3) unsigned NOT NULL,
  `created` datetime DEFAULT NULL COMMENT 'when this alert type was created',
  `last_sent` datetime DEFAULT NULL COMMENT 'Last time the alert was sent',
  `resolved` datetime DEFAULT NULL COMMENT 'when the alert status was resolved (available memory no longer at critical level, etc.)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alert`
--

LOCK TABLES `alert` WRITE;
/*!40000 ALTER TABLE `alert` DISABLE KEYS */;
INSERT INTO `alert` VALUES (1,13,0,0,'Management server node 127.0.0.1 is up',1,'2011-03-17 00:56:30','2011-03-17 00:56:30',NULL),(2,18,3,3,'Secondary Storage Vm up in zone: Basic, secStorageVm: s-1-TEST, public IP: 172.24.0.120, private IP: 192.168.152.104',1,'2011-03-17 00:58:55','2011-03-17 00:58:55',NULL),(3,9,3,3,'Console proxy up in zone: Basic, proxy: v-2-TEST, public IP: 172.24.0.141, private IP: 192.168.152.106',1,'2011-03-17 00:59:01','2011-03-17 00:59:01',NULL);
/*!40000 ALTER TABLE `alert` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `async_job`
--

DROP TABLE IF EXISTS `async_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `async_job` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `session_key` varchar(64) DEFAULT NULL COMMENT 'all async-job manage to apply session based security enforcement',
  `instance_type` varchar(64) DEFAULT NULL COMMENT 'instance_type and instance_id work together to allow attaching an instance object to a job',
  `instance_id` bigint(20) unsigned DEFAULT NULL,
  `job_cmd` varchar(64) NOT NULL COMMENT 'command name',
  `job_cmd_originator` varchar(64) DEFAULT NULL COMMENT 'command originator',
  `job_cmd_info` text COMMENT 'command parameter info',
  `job_cmd_ver` int(1) DEFAULT NULL COMMENT 'command version',
  `callback_type` int(1) DEFAULT NULL COMMENT 'call back type, 0 : polling, 1 : email',
  `callback_address` varchar(128) DEFAULT NULL COMMENT 'call back address by callback_type',
  `job_status` int(1) DEFAULT NULL COMMENT 'general job execution status',
  `job_process_status` int(1) DEFAULT NULL COMMENT 'job specific process status for asynchronize progress update',
  `job_result_code` int(1) DEFAULT NULL COMMENT 'job result code, specify error code corresponding to result message',
  `job_result` text COMMENT 'job result info',
  `job_init_msid` bigint(20) DEFAULT NULL COMMENT 'the initiating msid',
  `job_complete_msid` bigint(20) DEFAULT NULL COMMENT 'completing msid',
  `created` datetime DEFAULT NULL COMMENT 'date created',
  `last_updated` datetime DEFAULT NULL COMMENT 'date created',
  `last_polled` datetime DEFAULT NULL COMMENT 'date polled',
  `removed` datetime DEFAULT NULL COMMENT 'date removed',
  PRIMARY KEY (`id`),
  KEY `i_async__user_id` (`user_id`),
  KEY `i_async__account_id` (`account_id`),
  KEY `i_async__instance_type_id` (`instance_type`,`instance_id`),
  KEY `i_async__job_cmd` (`job_cmd`),
  KEY `i_async__created` (`created`),
  KEY `i_async__last_updated` (`last_updated`),
  KEY `i_async__last_poll` (`last_polled`),
  KEY `i_async__removed` (`removed`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `async_job`
--

LOCK TABLES `async_job` WRITE;
/*!40000 ALTER TABLE `async_job` DISABLE KEYS */;
INSERT INTO `async_job` VALUES (1,2,2,NULL,NULL,NULL,'DeployVM','virtualmachine','{\"accountId\":2,\"dataCenterId\":3,\"serviceOfferingId\":7,\"templateId\":2,\"password\":\"rF3zcqxda\",\"domainId\":0,\"userId\":2,\"vmId\":0,\"eventId\":24}',0,0,NULL,1,1,0,'com.cloud.async.executor.DeployVMResultObject/{\"id\":3,\"name\":\"i-2-3-TEST\",\"created\":\"Mar 16, 2011 5:59:29 PM\",\"zoneId\":3,\"zoneName\":\"Basic\",\"ipAddress\":\"10.1.1.2\",\"serviceOfferingId\":7,\"haEnabled\":false,\"state\":\"Running\",\"templateId\":2,\"templateName\":\"CentOS 5.3(x86_64) no GUI\",\"templateDisplayText\":\"CentOS 5.3(x86_64) no GUI\",\"passwordEnabled\":false,\"serviceOfferingName\":\"Small Instance, Virtual Networking\",\"cpuNumber\":\"1\",\"cpuSpeed\":\"500\",\"memory\":\"512\",\"displayName\":\"i-2-3-TEST\",\"domainId\":1,\"domain\":\"ROOT\",\"account\":\"admin\",\"hostname\":\"x18\",\"hostid\":2,\"networkGroupList\":\"\"}',120890037145,120890037145,'2011-03-17 00:59:29','2011-03-17 01:01:00','2011-03-17 01:00:50',NULL),(2,2,2,NULL,NULL,NULL,'DeployVM','virtualmachine','{\"accountId\":2,\"dataCenterId\":3,\"serviceOfferingId\":7,\"templateId\":2,\"password\":\"sL3zrzfxf\",\"domainId\":0,\"userId\":2,\"vmId\":0,\"eventId\":28}',0,0,NULL,1,1,0,'com.cloud.async.executor.DeployVMResultObject/{\"id\":5,\"name\":\"i-2-5-TEST\",\"created\":\"Mar 16, 2011 5:59:32 PM\",\"zoneId\":3,\"zoneName\":\"Basic\",\"ipAddress\":\"10.1.1.3\",\"serviceOfferingId\":7,\"haEnabled\":false,\"state\":\"Running\",\"templateId\":2,\"templateName\":\"CentOS 5.3(x86_64) no GUI\",\"templateDisplayText\":\"CentOS 5.3(x86_64) no GUI\",\"passwordEnabled\":false,\"serviceOfferingName\":\"Small Instance, Virtual Networking\",\"cpuNumber\":\"1\",\"cpuSpeed\":\"500\",\"memory\":\"512\",\"displayName\":\"i-2-5-TEST\",\"domainId\":1,\"domain\":\"ROOT\",\"account\":\"admin\",\"hostname\":\"x18\",\"hostid\":2,\"networkGroupList\":\"\"}',120890037145,120890037145,'2011-03-17 00:59:32','2011-03-17 01:01:04','2011-03-17 01:01:03',NULL),(3,2,2,NULL,NULL,NULL,'AssignToLoadBalancer',NULL,'{\"userId\":2,\"domainRouterId\":4,\"loadBalancerId\":1,\"instanceIdList\":[3]}',0,0,NULL,1,0,0,'java.lang.String/\"success\"',120890037145,120890037145,'2011-03-17 01:04:12','2011-03-17 01:04:15',NULL,NULL),(4,2,2,NULL,NULL,NULL,'AssignToLoadBalancer',NULL,'{\"userId\":2,\"domainRouterId\":4,\"loadBalancerId\":2,\"instanceIdList\":[3]}',0,0,NULL,1,0,0,'java.lang.String/\"success\"',120890037145,120890037145,'2011-03-17 01:04:14','2011-03-17 01:04:20','2011-03-17 01:04:19',NULL),(5,2,2,NULL,NULL,NULL,'AssignToLoadBalancer',NULL,'{\"userId\":2,\"domainRouterId\":4,\"loadBalancerId\":1,\"instanceIdList\":[5]}',0,0,NULL,1,0,0,'java.lang.String/\"success\"',120890037145,120890037145,'2011-03-17 01:04:25','2011-03-17 01:04:28',NULL,NULL),(6,2,2,NULL,NULL,NULL,'AssignToLoadBalancer',NULL,'{\"userId\":2,\"domainRouterId\":4,\"loadBalancerId\":2,\"instanceIdList\":[5]}',0,0,NULL,1,0,0,'java.lang.String/\"success\"',120890037145,120890037145,'2011-03-17 01:04:32','2011-03-17 01:04:34',NULL,NULL);
/*!40000 ALTER TABLE `async_job` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cluster`
--

DROP TABLE IF EXISTS `cluster`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cluster` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name for the cluster',
  `pod_id` bigint(20) unsigned NOT NULL COMMENT 'pod id',
  `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'data center id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `i_cluster__pod_id__name` (`pod_id`,`name`),
  KEY `fk_cluster__data_center_id` (`data_center_id`),
  CONSTRAINT `fk_cluster__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center` (`id`),
  CONSTRAINT `fk_cluster__pod_id` FOREIGN KEY (`pod_id`) REFERENCES `host_pod_ref` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cluster`
--

LOCK TABLES `cluster` WRITE;
/*!40000 ALTER TABLE `cluster` DISABLE KEYS */;
INSERT INTO `cluster` VALUES (1,'alena',3,3);
/*!40000 ALTER TABLE `cluster` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `configuration`
--

DROP TABLE IF EXISTS `configuration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `configuration` (
  `category` varchar(255) NOT NULL DEFAULT 'Advanced',
  `instance` varchar(255) NOT NULL,
  `component` varchar(255) NOT NULL DEFAULT 'management-server',
  `name` varchar(255) NOT NULL,
  `value` varchar(4095) DEFAULT NULL,
  `description` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `configuration`
--

LOCK TABLES `configuration` WRITE;
/*!40000 ALTER TABLE `configuration` DISABLE KEYS */;
INSERT INTO `configuration` VALUES ('Advanced','DEFAULT','management-server','account.cleanup.interval','86400','null'),('Advanced','DEFAULT','management-server','alert.email.addresses','some.admin@example.com','comma seperated list of email addresses used for sending alerts'),('Advanced','DEFAULT','management-server','alert.email.sender','some.user@example.com','sender of alert email (will be in the From header of the email)'),('Advanced','DEFAULT','null','alert.smtp.debug','false','null'),('Advanced','DEFAULT','management-server','alert.smtp.host','smtp.host.com','SMTP hostname used for sending out email alerts'),('Advanced','DEFAULT','management-server','alert.smtp.password','password','password for SMTP authentication (applies only if alert.smtp.useAuth is true)'),('Advanced','DEFAULT','management-server','alert.smtp.port','25','port the SMTP server is listening on (default is 25)'),('Advanced','DEFAULT','management-server','alert.smtp.useAuth','false','If true, use SMTP authentication when sending emails.  If false, do not use SMTP authentication when sending emails.'),('Advanced','DEFAULT','management-server','alert.smtp.username','some.user@example.com','username for SMTP authentication (applies only if alert.smtp.useAuth is true)'),('Advanced','DEFAULT','AgentManager','alert.wait','1800','null'),('Advanced','DEFAULT','management-server','allow.public.user.templates','true','If false, users will not be able to create public templates.'),('Advanced','DEFAULT','management-server','capacity.check.period','300000','null'),('Usage','DEFAULT','management-server','capacity.skipcounting.hours','24','The interval in hours since VM has stopped to skip counting its allocated CPU/Memory capacity'),('Advanced','DEFAULT','management-server','check.pod.cidrs','true','If true, different pods must belong to different CIDR subnets.'),('Hidden','DEFAULT','management-server','cloud.identifier','69123cec-6001-463f-ad06-d52401a36ea6','A unique identifier for the cloud.'),('Console Proxy','DEFAULT','AgentManager','consoleproxy.capacity.standby','10','The minimal number of console proxy viewer sessions that system is able to serve immediately(standby capacity)'),('Console Proxy','DEFAULT','AgentManager','consoleproxy.capacityscan.interval','30000','The time interval(in millisecond) to scan whether or not system needs more console proxy to ensure minimal standby capacity'),('Console Proxy','DEFAULT','AgentManager','consoleproxy.cmd.port','8001','Console proxy command port that is used to communicate with management server'),('Console Proxy','DEFAULT','AgentManager','consoleproxy.loadscan.interval','10000','The time interval(in milliseconds) to scan console proxy working-load info'),('Console Proxy','DEFAULT','AgentManager','consoleproxy.ram.size','1024','RAM size (in MB) used to create new console proxy VMs'),('Console Proxy','DEFAULT','AgentManager','consoleproxy.session.max','50','The max number of viewer sessions console proxy is configured to serve for'),('Console Proxy','DEFAULT','AgentManager','consoleproxy.session.timeout','300000','Timeout(in milliseconds) that console proxy tries to maintain a viewer session before it times out the session for no activity'),('Advanced','DEFAULT','management-server','cpu.capacity.threshold','0.85','percentage (as a value between 0 and 1) of cpu utilization above which alerts will be sent about low cpu available'),('Advanced','DEFAULT','management-server','cpu.overprovisioning.factor','1','Used for CPU overprovisioning calculation; available CPU will be (actualCpuCapacity * cpu.overprovisioning.factor)'),('Advanced','DEFAULT','management-server','default.page.size','500','Default page size for API list* commands'),('Advanced','DEFAULT','null','default.zone','ZONE1','null'),('Advanced','DEFAULT','management-server','direct.attach.network.externalIpAllocator.enabled','false','Direct-attach VMs using external DHCP server'),('Advanced','DEFAULT','management-server','direct.attach.network.externalIpAllocator.url',NULL,'Direct-attach VMs using external DHCP server (API url)'),('Advanced','DEFAULT','management-server','direct.attach.network.groups.enabled','false','Ec2-style distributed firewall for direct-attach VMs'),('Advanced','DEFAULT','management-server','direct.attach.untagged.vlan.enabled','false','Indicate whether the system supports direct-attached untagged vlan'),('Advanced','DEFAULT','AgentManager','domain.suffix','qatest-vmops.com','domain suffix for users'),('Premium','DEFAULT','management-server','enable.usage.server','true','Flag for enabling usage'),('Advanced','DEFAULT','management-server','event.purge.delay','0','Events older than specified number days will be purged'),('Advanced','DEFAULT','UserVmManager','expunge.delay','86400','Determines how long to wait before actually expunging destroyed vm. The default value = the default value of expunge.interval'),('Advanced','DEFAULT','UserVmManager','expunge.interval','86400','the interval to wait before running the expunge thread'),('Advanced','DEFAULT','UserVmManager','expunge.workers','1','null'),('Advanced','DEFAULT','null','guest.ip.network','10.1.1.1','null'),('Advanced','DEFAULT','null','guest.netmask','255.255.255.0','null'),('Advanced','DEFAULT','management-server','host','192.168.1.178','The ip address of management server'),('Advanced','DEFAULT','AgentManager','host.retry','2','Number of times to retry hosts for creating a volume'),('Advanced','DEFAULT','management-server','host.stats.interval','60000','the interval in milliseconds when host stats are retrieved from agents'),('Advanced','DEFAULT','ManagementServer','hypervisor.type','xenserver','The type of hypervisor that this deployment will use.'),('Advanced','DEFAULT','none','init','true','null'),('Advanced','DEFAULT','AgentManager','instance.name','TEST','Name of the deployment instance'),('Advanced','DEFAULT','management-server','integration.api.port','8096','internal port used by the management server for servicing Integration API requests'),('Advanced','DEFAULT','HighAvailabilityManager','investigate.retry.interval','60','null'),('Advanced','DEFAULT','management-server','job.expire.minutes','1440','Time (in minutes) for async-jobs to be kept in system'),('Advanced','DEFAULT','management-server','linkLocalIp.nums','10','The number of link local ip that needed by domR(in power of 2)'),('Advanced','DEFAULT','management-server','max.account.public.ips','20','the maximum number of public IPs that can be reserved for an account'),('Advanced','DEFAULT','management-server','max.account.user.vms','20','the maximum number of user VMs that can be deployed for an account'),('Advanced','DEFAULT','management-server','max.template.iso.size','50','The maximum size for a downloaded template or ISO (in GB).'),('Storage','DEFAULT','management-server','max.volume.size.gb','2000','The maximum size for a volume in Gb.'),('Advanced','DEFAULT','management-server','memory.capacity.threshold','0.85','percentage (as a value between 0 and 1) of memory utilization above which alerts will be sent about low memory available'),('Advanced','DEFAULT','HighAvailabilityManager','migrate.retry.interval','120','null'),('Advanced','DEFAULT','management-server','mount.parent','/mnt','The mount point on the Management Server for Secondary Storage.'),('Advanced','DEFAULT','management-server','multicast.throttling.rate','10','default multicast rate in megabits per second allowed'),('Advanced','DEFAULT','management-server','network.throttling.rate','200','default data transfer rate in megabits per second allowed per user'),('Advanced','DEFAULT','ManagementServer','network.type','vlan','The type of network that this deployment will use.'),('Advanced','DEFAULT','AgentManager','ping.interval','60','null'),('Advanced','DEFAULT','AgentManager','ping.timeout','2.5','null'),('Advanced','DEFAULT','AgentManager','port','8250','port to listen on for agent connection'),('Advanced','DEFAULT','management-server','private.ip.capacity.threshold','0.85','percentage (as a value between 0 and 1) of private IP address space utilization above which alerts will be sent'),('Advanced','DEFAULT','management-server','public.ip.capacity.threshold','0.85','percentage (as a value between 0 and 1) of public IP address space utilization above which alerts will be sent'),('Advanced','DEFAULT','HighAvailabilityManager','restart.retry.interval','600','null'),('Advanced','DEFAULT','AgentManager','retries.per.host','2','The number of times each command sent to a host should be retried in case of failure.'),('Advanced','DEFAULT','management-server','router.cleanup.interval','3600','Time in seconds identifies when to stop router when there are no user vms associated with it'),('Advanced','DEFAULT','none','router.ram.size','128','Default RAM for router VM in MB.'),('Advanced','DEFAULT','none','router.stats.interval','300','Interval to report router statistics.'),('Advanced','DEFAULT','none','router.template.id','1','Default ID for template.'),('Advanced','DEFAULT','null','secondary.storage.vm','true','null'),('Advanced','DEFAULT','management-server','secstorage.allowed.internal.sites',NULL,'Comma separated list of cidrs internal to the datacenter that can host template download servers'),('Hidden','DEFAULT','management-server','secstorage.copy.password','mT4hhncdw','Password used to authenticate zone-to-zone template copy requests'),('Advanced','DEFAULT','null','secstorage.encrypt.copy','true','null'),('Advanced','DEFAULT','management-server','secstorage.ssl.cert.domain','realhostip.com','SSL certificate used to encrypt copy traffic between zones'),('Advanced','DEFAULT','AgentManager','secstorage.vm.ram.size',NULL,'RAM size (in MB) used to create new secondary storage vms'),('Hidden','DEFAULT','management-server','security.hash.key',NULL,'for generic key-ed hash'),('Hidden','DEFAULT','management-server','security.singlesignon.key','rZ9-MnSj7U1ejTOjGpgIueJUU5j1_V8X4O5EP82QUqKhKAhtqfTtm0SFkaHpsqkck79CGOPjz-Qw50GLPvxQrg','A Single Sign-On key used for logging into the cloud'),('Advanced','DEFAULT','management-server','security.singlesignon.tolerance.millis','300000','The allowable clock difference in milliseconds between when an SSO login request is made and when it is received.'),('Snapshots','DEFAULT','none','snapshot.max.daily','8','Maximum dalily snapshots for a volume'),('Snapshots','DEFAULT','none','snapshot.max.hourly','8','Maximum hourly snapshots for a volume'),('Snapshots','DEFAULT','none','snapshot.max.monthly','8','Maximum hourly snapshots for a volume'),('Snapshots','DEFAULT','none','snapshot.max.weekly','8','Maximum hourly snapshots for a volume'),('Advanced','DEFAULT','SnapshotManager','snapshot.poll.interval','300','The time interval in seconds when the management server polls for snapshots to be scheduled.'),('Advanced','DEFAULT','SnapshotManager','snapshot.recurring.test','false','Flag for testing recurring snapshots'),('Advanced','DEFAULT','SnapshotManager','snapshot.test.days.per.month','30','Set it to a smaller value to take more recurring snapshots'),('Advanced','DEFAULT','SnapshotManager','snapshot.test.days.per.week','7','Set it to a smaller value to take more recurring snapshots'),('Advanced','DEFAULT','SnapshotManager','snapshot.test.hours.per.day','24','Set it to a smaller value to take more recurring snapshots'),('Advanced','DEFAULT','SnapshotManager','snapshot.test.minutes.per.hour','60','Set it to a smaller value to take more recurring snapshots'),('Advanced','DEFAULT','SnapshotManager','snapshot.test.months.per.year','12','Set it to a smaller value to take more recurring snapshots'),('Advanced','DEFAULT','SnapshotManager','snapshot.test.weeks.per.month','4','Set it to a smaller value to take more recurring snapshots'),('Hidden','DEFAULT','management-server','ssh.privatekey','-----BEGIN RSA PRIVATE KEY-----\nMIIEpQIBAAKCAQEAy0P5ZfxBuyx4ksgmUegqMxzf0EKQQbsV1QsL3xtF0i1g8kRB\nAK+v/k3b23hNGHZ/qDXjGd5nA6UQIU42VM2BJKqlIstasGJ3QWe9AGMqK/WFlFns\nYm3Ufea/jNXXRVxJ80uggWpNhxIerV7g9cxeG18YNVroyN5Od3Jq7x/lpCFhHpon\n4ioWHdlSmRxYeELaQkI/v5rgcq5can8qd4xQsulXw1AfM43YPztBWkK35th4qFwO\nstp9hwPp0GjSC95k4kPP7soR8F4x7/7+MZQ74nojZeA7oEwqv/rpZJnVLhqmitZW\na9Y22Xz1NCl4LEIR0Rv6+LJil7jtZkDiQRIWyQIDAQABAoIBAAL70nNeypA2zOZK\nrdVR9Rye9eIWuAte9akU6m2x9sppBB1KRzH9YjX3apzEHU2Yn7MWZ2j/zL9O8veq\nMD36bkTdhGMIXSJtr5Z0MWAKFJpkljthPokLV60pb/JfSTOpcSRzHb4Ik27CI1Uk\nKWUyRMdeE/rkhLo+GSTDtzIfY72PASyDLQrsOV71hSYtxtuNi6Boug0xdTF5KgN7\nKtE17FouwKj3qH+Sp5EfXkTJ9UA7hVPihaEoZ7qbi5TMkiN8RXY4LIX1JsTtCwED\nA3+vJgkROO2srl8Jfjp5Gs/uesYbD2rVxTi773q/Xy1hZfTBANqW2y2t80po06dO\nz7p4JykCgYEA7B/GOToba+z4y6SaeCc7lj418x8a68ktyzmBj05oNJTCPbX9Cta4\nFf3AX5VhTpCePMk3ZdHdacz/DttQUXb+0JxEYq/gchQqLUcTVZScQ+afowwDmLMP\niAShOzUtyGk+SKPIJa90GBxrM0t/drbRY3RFXeR9EnurkWnrGIX0BEcCgYEA3GAh\nwWQgrqqE9KxpHjuh1ELBqlHcdb6yM1kgHw1EsRzLVbxXesEeXsoDSxuSobOxkbfM\ncIxbC3lH3Ybif4CKK9OqmvtoGbgP2Hh7wXEnaUpJvMkzYHk/fgRxSdnsINZ35z6O\ncQAzOjuDJCuSO25EFCA97U1jW7kG9LLDrsdj5G8CgYEAqHcLEC1a/yd1YqWv5+Xl\n542/JBSfOnawi3wd6cbBJjK0Wx8KyL7lXBtNM2T6pfkcTLYrJiQTWrb9ivYCWf0S\nVHgJeneNJRcp+F/rdXWv/+n9H3Ek579pZ1wWRI3uejxMSbtAw1Jo/ALaoSBOcr6A\nqfp8uz6X1aYShI8TQs3fiWUCgYEAn/Z7qhgXDRIjjkKzzsk0T5HhZ9Mh9qnkShdw\nlHmaGgGvqt0alrUlaWKzf0pVZmC8DOzgTfXjhHGVc7ycHjbclLGBDhxdVQXhIp1a\nH3EWTXwqCAn37Bg2uS9aWGQB3VgwEr+6v1WI2cUnDRM/gzon2oAI036wF304CljU\nZ8K9xKsCgYEApctC7wYxaQxHJJ0RE4wUcIzLN2gQ7iGHqCselMxsSvobMS/HQySC\n6d2hhijRFdkwnxhv7g2yRcqjC9/ktfVzpw7nXJgZfgmTvMmuNIFu3kaMNPFJEhCt\nkps7x4mi0esu0VydSYnnFMDQyli587SyBTumAmmUmh876AZuHEvELwY=\n-----END RSA PRIVATE KEY-----','Private key for the entire CloudStack'),('Hidden','DEFAULT','management-server','ssh.publickey','ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDLQ/ll/EG7LHiSyCZR6CozHN/QQpBBuxXVCwvfG0XSLWDyREEAr6/+TdvbeE0Ydn+oNeMZ3mcDpRAhTjZUzYEkqqUiy1qwYndBZ70AYyor9YWUWexibdR95r+M1ddFXEnzS6CBak2HEh6tXuD1zF4bXxg1WujI3k53cmrvH+WkIWEemifiKhYd2VKZHFh4QtpCQj+/muByrlxqfyp3jFCy6VfDUB8zjdg/O0FaQrfm2HioXA6y2n2HA+nQaNIL3mTiQ8/uyhHwXjHv/v4xlDvieiNl4DugTCq/+ulkmdUuGqaK1lZr1jbZfPU0KXgsQhHRG/r4smKXuO1mQOJBEhbJ alena@alena','Public key for the entire CloudStack'),('Advanced','DEFAULT','AgentManager','start.retry','10','Number of times to retry create and start commands'),('Advanced','DEFAULT','HighAvailabilityManager','stop.retry.interval','600','null'),('Advanced','DEFAULT','null','storage.allocated.capacity.threshold','0.85','null'),('Advanced','DEFAULT','management-server','storage.capacity.threshold','0.85','percentage (as a value between 0 and 1) of storage utilization above which alerts will be sent about low storage available'),('Advanced','DEFAULT','none','storage.cleanup.enabled','true','Enables/disables the storage cleanup thread.'),('Advanced','DEFAULT','none','storage.cleanup.interval','86400','The interval to wait before running the storage cleanup thread.'),('Advanced','DEFAULT','StorageAllocator','storage.overprovisioning.factor','2','Storage Allocator overprovisioning factor'),('Advanced','DEFAULT','management-server','storage.stats.interval','60000','the interval in milliseconds when storage stats (per host) are retrieved from agents'),('Advanced','DEFAULT','null','system.vm.local.storage.required','false','null'),('Advanced','DEFAULT','ManagementServer','system.vm.use.local.storage','false','null'),('Storage','DEFAULT','AgentManager','total.retries','4','The number of times each command sent to a host should be retried in case of failure.'),('Advanced','DEFAULT','AgentManager','update.wait','600','null'),('Advanced','DEFAULT','null','upgrade.url','http://example.com:8080/client/agent/update.zip','null'),('Advanced','DEFAULT','null','usage.aggregation.timezone','GMT','null'),('Premium','DEFAULT','management-server','usage.execution.timezone',NULL,'The timezone to use for usage job execution time'),('Advanced','DEFAULT','management-server','usage.stats.job.aggregation.range','1440','the range of time for aggregating the user statistics specified in minutes (e.g. 1440 for daily, 60 for hourly)'),('Advanced','DEFAULT','management-server','usage.stats.job.exec.time','00:15','the time at which the usage statistics aggregation job will run as an HH24:MM time, e.g. 00:30 to run at 12:30am'),('Advanced','DEFAULT','ManagementServer','use.local.storage','false','Indicates whether to use local storage pools or shared storage pools for system VMs.'),('Advanced','DEFAULT','management-server','vm.allocation.algorithm','random','If \'random\', hosts within a pod will be randomly considered for VM/volume allocation. If \'firstfit\', they will be considered on a first-fit basis.'),('Advanced','DEFAULT','AgentManager','wait','1800','null'),('Advanced','DEFAULT','AgentManager','workers','5','Number of worker threads.'),('Advanced','DEFAULT','management-server','xen.bond.storage.nics',NULL,'Attempt to bond the two networks if found'),('Hidden','DEFAULT','management-server','xen.create.pools.in.pod','false','Should we automatically add XenServers into pools that are inside a Pod'),('Advanced','DEFAULT','management-server','xen.guest.network.device',NULL,'Specify when the guest network does not go over the private network'),('Advanced','DEFAULT','management-server','xen.heartbeat.interval','60','heartbeat to use when implementing XenServer Self Fencing'),('Advanced','DEFAULT','management-server','xen.max.product.version','5.6.0','Maximum XenServer version'),('Advanced','DEFAULT','management-server','xen.max.version','3.4.2','Maximum Xen version'),('Advanced','DEFAULT','management-server','xen.max.xapi.version','1.3','Maximum Xapi Tool Stack version'),('Advanced','DEFAULT','management-server','xen.min.product.version','0.1.1','Minimum XenServer version'),('Advanced','DEFAULT','management-server','xen.min.version','3.3.1','Minimum Xen version'),('Advanced','DEFAULT','management-server','xen.min.xapi.version','1.3','Minimum Xapi Tool Stack version'),('Advanced','DEFAULT','management-server','xen.preallocated.lun.size.range','.05','percentage to add to disk size when allocating'),('Network','DEFAULT','management-server','xen.private.network.device',NULL,'Specify when the private network name is different'),('Network','DEFAULT','management-server','xen.public.network.device',NULL,'[ONLY IF THE PUBLIC NETWORK IS ON A DEDICATED NIC]:The network name label of the physical device dedicated to the public network on a XenServer host'),('Advanced','DEFAULT','management-server','xen.setup.multipath','false','Setup the host to do multipath'),('Network','DEFAULT','management-server','xen.storage.network.device1','cloud-stor1','Specify when there are storage networks'),('Network','DEFAULT','management-server','xen.storage.network.device2','cloud-stor2','Specify when there are storage networks');
/*!40000 ALTER TABLE `configuration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `console_proxy`
--

DROP TABLE IF EXISTS `console_proxy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `console_proxy` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `gateway` varchar(15) DEFAULT NULL COMMENT 'gateway info for this console proxy towards public network interface',
  `dns1` varchar(15) DEFAULT NULL COMMENT 'dns1',
  `dns2` varchar(15) DEFAULT NULL COMMENT 'dns2',
  `domain` varchar(255) DEFAULT NULL COMMENT 'domain',
  `public_mac_address` varchar(17) NOT NULL COMMENT 'mac address of the public facing network card',
  `public_ip_address` varchar(15) DEFAULT NULL COMMENT 'public ip address for the console proxy',
  `public_netmask` varchar(15) DEFAULT NULL COMMENT 'public netmask used for the console proxy',
  `guest_mac_address` varchar(17) NOT NULL COMMENT 'mac address of the guest facing network card',
  `guest_ip_address` varchar(15) DEFAULT NULL COMMENT 'guest ip address for the console proxy',
  `guest_netmask` varchar(15) DEFAULT NULL COMMENT 'guest netmask used for the console proxy',
  `vlan_db_id` bigint(20) unsigned DEFAULT NULL COMMENT 'Foreign key into vlan id table',
  `vlan_id` varchar(255) DEFAULT NULL COMMENT 'optional VLAN ID for console proxy that can be used',
  `ram_size` int(10) unsigned NOT NULL DEFAULT '512' COMMENT 'memory to use in mb',
  `active_session` int(10) NOT NULL DEFAULT '0' COMMENT 'active session number',
  `last_update` datetime DEFAULT NULL COMMENT 'Last session update time',
  `session_details` blob COMMENT 'session detail info',
  PRIMARY KEY (`id`),
  UNIQUE KEY `public_mac_address` (`public_mac_address`),
  UNIQUE KEY `guest_mac_address` (`guest_mac_address`),
  UNIQUE KEY `public_ip_address` (`public_ip_address`),
  UNIQUE KEY `guest_ip_address` (`guest_ip_address`),
  KEY `i_console_proxy__vlan_id` (`vlan_db_id`),
  CONSTRAINT `fk_console_proxy__vlan_id` FOREIGN KEY (`vlan_db_id`) REFERENCES `vlan` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `console_proxy`
--

LOCK TABLES `console_proxy` WRITE;
/*!40000 ALTER TABLE `console_proxy` DISABLE KEYS */;
INSERT INTO `console_proxy` VALUES (2,'172.24.0.1','72.52.126.11',NULL,'foo.com','06:83:c7:31:00:03','172.24.0.141','255.255.0.0','06:03:f6:70:00:04','169.254.2.102','255.255.0.0',1,'24',1024,0,NULL,NULL);
/*!40000 ALTER TABLE `console_proxy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `data_center`
--

DROP TABLE IF EXISTS `data_center`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `data_center` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `dns1` varchar(255) NOT NULL,
  `dns2` varchar(255) DEFAULT NULL,
  `internal_dns1` varchar(255) NOT NULL,
  `internal_dns2` varchar(255) DEFAULT NULL,
  `gateway` varchar(15) DEFAULT NULL,
  `netmask` varchar(15) DEFAULT NULL,
  `vnet` varchar(255) DEFAULT NULL,
  `router_mac_address` varchar(17) NOT NULL DEFAULT '02:00:00:00:00:01' COMMENT 'mac address for the router within the domain',
  `mac_address` bigint(20) unsigned NOT NULL DEFAULT '1' COMMENT 'Next available mac address for the ethernet card interacting with public internet',
  `guest_network_cidr` varchar(15) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `data_center`
--

LOCK TABLES `data_center` WRITE;
/*!40000 ALTER TABLE `data_center` DISABLE KEYS */;
INSERT INTO `data_center` VALUES (3,'Basic',NULL,'72.52.126.11',NULL,'192.168.10.253',NULL,NULL,NULL,'500-600','02:00:00:00:00:01',7,'10.1.1.0/24');
/*!40000 ALTER TABLE `data_center` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `disk_offering`
--

DROP TABLE IF EXISTS `disk_offering`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `disk_offering` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `domain_id` bigint(20) unsigned DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `display_text` varchar(4096) DEFAULT NULL COMMENT 'Description text set by the admin for display purpose only',
  `disk_size` bigint(20) unsigned NOT NULL COMMENT 'disk space in mbs',
  `mirrored` tinyint(1) unsigned NOT NULL DEFAULT '1' COMMENT 'Enable mirroring?',
  `type` varchar(32) DEFAULT NULL COMMENT 'inheritted by who?',
  `tags` varchar(4096) DEFAULT NULL COMMENT 'comma separated tags about the disk_offering',
  `recreatable` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT 'The root disk is always recreatable',
  `use_local_storage` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT 'Indicates whether local storage pools should be used',
  `unique_name` varchar(32) DEFAULT NULL COMMENT 'unique name',
  `removed` datetime DEFAULT NULL COMMENT 'date removed',
  `created` datetime DEFAULT NULL COMMENT 'date the disk offering was created',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_name` (`unique_name`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `disk_offering`
--

LOCK TABLES `disk_offering` WRITE;
/*!40000 ALTER TABLE `disk_offering` DISABLE KEYS */;
INSERT INTO `disk_offering` VALUES (1,NULL,'Small Instance','Small Instance [500MHZ CPU, 512MB MEM, 16GB Disk] - $0.10 per hour',0,0,'Service',NULL,0,0,NULL,NULL,'2011-03-17 00:56:02'),(2,NULL,'Medium Instance','Medium Instance [2GHZ CPU, 2GB MEM, 32GB Disk] - $0.20 per hour',0,0,'Service',NULL,0,0,NULL,NULL,'2011-03-17 00:56:02'),(3,NULL,'Large Instance','Large Instance [2GHZ CPU, 4GB MEM, 64GB Disk] - $0.30 per hour',0,0,'Service',NULL,0,0,NULL,NULL,'2011-03-17 00:56:02'),(4,1,'Small Disk','Small Disk [16GB Disk]',16000,0,'Disk',NULL,0,0,NULL,NULL,'2011-03-17 00:56:02'),(5,1,'Medium Disk','Medium Disk [32GB Disk]',32000,0,'Disk',NULL,0,0,NULL,NULL,'2011-03-17 00:56:02'),(6,1,'Large Disk','Large Disk [64GB Disk]',64000,0,'Disk',NULL,0,0,NULL,NULL,'2011-03-17 00:56:02'),(7,NULL,'Small Instance, Virtual Networking','Small Instance, Virtual Networking, $0.05 per hour',0,0,'Service',NULL,0,0,NULL,NULL,'2011-03-17 00:56:24'),(8,NULL,'Medium Instance, Virtual Networking','Medium Instance, Virtual Networking, $0.10 per hour',0,0,'Service',NULL,0,0,NULL,NULL,'2011-03-17 00:56:24'),(9,NULL,'Small Instance, Direct Networking','Small Instance, Direct Networking, $0.05 per hour',0,0,'Service',NULL,0,0,NULL,NULL,'2011-03-17 00:56:24'),(10,NULL,'Medium Instance, Direct Networking','Medium Instance, Direct Networking, $0.10 per hour',0,0,'Service',NULL,0,0,NULL,NULL,'2011-03-17 00:56:24'),(11,1,'Small','Small Disk, 5 GB',5120,0,'Disk',NULL,0,0,NULL,NULL,'2011-03-17 00:56:24'),(12,1,'Medium','Medium Disk, 20 GB',20480,0,'Disk',NULL,0,0,NULL,NULL,'2011-03-17 00:56:24'),(13,1,'Large','Large Disk, 100 GB',102400,0,'Disk',NULL,0,0,NULL,NULL,'2011-03-17 00:56:24'),(14,NULL,'Fake Offering For DomR',NULL,0,0,'Service',NULL,1,0,'Cloud.Com-SoftwareRouter','2011-03-17 00:56:29','2011-03-17 00:56:29'),(15,NULL,'Fake Offering For Secondary Storage VM',NULL,0,0,'Service',NULL,1,0,'Cloud.com-SecondaryStorage','2011-03-17 00:56:29','2011-03-17 00:56:29'),(16,NULL,'Fake Offering For DomP',NULL,0,0,'Service',NULL,1,0,'Cloud.com-ConsoleProxy','2011-03-17 00:56:29','2011-03-17 00:56:29');
/*!40000 ALTER TABLE `disk_offering` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `disk_template_ref`
--

DROP TABLE IF EXISTS `disk_template_ref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `disk_template_ref` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `description` varchar(255) NOT NULL,
  `host` varchar(255) NOT NULL COMMENT 'host on which the server exists',
  `parent` varchar(255) NOT NULL COMMENT 'parent path',
  `path` varchar(255) NOT NULL,
  `size` int(10) unsigned NOT NULL COMMENT 'size of the disk',
  `type` varchar(255) NOT NULL COMMENT 'file system type',
  `created` datetime NOT NULL COMMENT 'Date created',
  `removed` datetime DEFAULT NULL COMMENT 'Date removed if not null',
  PRIMARY KEY (`id`),
  KEY `i_disk_template_ref__removed` (`removed`),
  KEY `i_disk_template_ref__type__size` (`type`,`size`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `disk_template_ref`
--

LOCK TABLES `disk_template_ref` WRITE;
/*!40000 ALTER TABLE `disk_template_ref` DISABLE KEYS */;
/*!40000 ALTER TABLE `disk_template_ref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `domain`
--

DROP TABLE IF EXISTS `domain`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `domain` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `parent` bigint(20) unsigned DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `owner` bigint(20) unsigned NOT NULL,
  `path` varchar(255) DEFAULT NULL,
  `level` int(10) NOT NULL DEFAULT '0',
  `child_count` int(10) NOT NULL DEFAULT '0',
  `next_child_seq` bigint(20) unsigned NOT NULL DEFAULT '1',
  `removed` datetime DEFAULT NULL COMMENT 'date removed',
  PRIMARY KEY (`id`),
  UNIQUE KEY `parent` (`parent`,`name`,`removed`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `domain`
--

LOCK TABLES `domain` WRITE;
/*!40000 ALTER TABLE `domain` DISABLE KEYS */;
INSERT INTO `domain` VALUES (1,NULL,'ROOT',2,'/',0,0,1,NULL);
/*!40000 ALTER TABLE `domain` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `domain_router`
--

DROP TABLE IF EXISTS `domain_router`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `domain_router` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'Primary Key',
  `gateway` varchar(15) NOT NULL COMMENT 'ip address of the gateway to this domR',
  `ram_size` int(10) unsigned NOT NULL DEFAULT '128' COMMENT 'memory to use in mb',
  `dns1` varchar(15) DEFAULT NULL COMMENT 'dns1',
  `dns2` varchar(15) DEFAULT NULL COMMENT 'dns2',
  `domain` varchar(255) DEFAULT NULL COMMENT 'domain',
  `public_mac_address` varchar(17) DEFAULT NULL COMMENT 'mac address of the public facing network card',
  `public_ip_address` varchar(15) DEFAULT NULL COMMENT 'public ip address used for source net',
  `public_netmask` varchar(15) DEFAULT NULL COMMENT 'netmask used for the domR',
  `guest_mac_address` varchar(17) NOT NULL COMMENT 'mac address of the pod facing network card',
  `guest_dc_mac_address` varchar(17) DEFAULT NULL COMMENT 'mac address of the data center facing network card',
  `guest_netmask` varchar(15) NOT NULL COMMENT 'netmask used for the guest network',
  `guest_ip_address` varchar(15) NOT NULL COMMENT ' ip address in the guest network',
  `vnet` varchar(18) DEFAULT NULL COMMENT 'vnet',
  `dc_vlan` varchar(18) DEFAULT NULL COMMENT 'vnet',
  `vlan_db_id` bigint(20) unsigned DEFAULT NULL COMMENT 'Foreign key into vlan id table',
  `vlan_id` varchar(255) DEFAULT NULL COMMENT 'optional VLAN ID for DomainRouter that can be used in rundomr.sh',
  `account_id` bigint(20) unsigned NOT NULL COMMENT 'account id of owner',
  `domain_id` bigint(20) unsigned NOT NULL,
  `dhcp_ip_address` bigint(20) unsigned NOT NULL DEFAULT '2' COMMENT 'next ip address for dhcp for this domR',
  `role` varchar(64) NOT NULL COMMENT 'type of role played by this router',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `i_domain_router__public_ip_address` (`public_ip_address`),
  KEY `i_domain_router__account_id` (`account_id`),
  KEY `i_domain_router__vlan_id` (`vlan_db_id`),
  CONSTRAINT `fk_domain_router__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_domain_router__id` FOREIGN KEY (`id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_domain_router__public_ip_address` FOREIGN KEY (`public_ip_address`) REFERENCES `user_ip_address` (`public_ip_address`),
  CONSTRAINT `fk_domain_router__vlan_id` FOREIGN KEY (`vlan_db_id`) REFERENCES `vlan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='information about the domR instance';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `domain_router`
--

LOCK TABLES `domain_router` WRITE;
/*!40000 ALTER TABLE `domain_router` DISABLE KEYS */;
INSERT INTO `domain_router` VALUES (4,'172.24.0.1',128,'72.52.126.11',NULL,'v2.myvm.com','06:83:71:58:00:05','172.24.0.140','255.255.0.0','02:00:01:f4:00:01',NULL,'255.255.255.0','10.1.1.1','500',NULL,1,'24',2,1,2,'DHCP_FIREWALL_LB_PASSWD_USERDATA');
/*!40000 ALTER TABLE `domain_router` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `event`
--

DROP TABLE IF EXISTS `event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `event` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `type` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL DEFAULT 'Completed',
  `description` varchar(1024) NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `created` datetime NOT NULL,
  `level` varchar(16) NOT NULL,
  `start_id` bigint(20) unsigned NOT NULL DEFAULT '0',
  `parameters` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `i_event__created` (`created`),
  KEY `i_event__user_id` (`user_id`),
  KEY `i_event__account_id` (`account_id`),
  KEY `i_event__level_id` (`level`),
  KEY `i_event__type_id` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event`
--

LOCK TABLES `event` WRITE;
/*!40000 ALTER TABLE `event` DISABLE KEYS */;
INSERT INTO `event` VALUES (1,'SERVICE.OFFERING.CREATE','Completed','Successfully created new service offering with name: Small Instance, Virtual Networking.',1,1,'2011-03-17 00:56:24','INFO',0,'soId=7\nname=Small Instance, Virtual Networking\nnumCPUs=1\nram=512\ncpuSpeed=500\ndisplayText=Small Instance, Virtual Networking, $0.05 per hour\nguestIPType=Virtualized\nlocalStorageRequired=false\nofferHA=false\nuseVirtualNetwork=true\n'),(2,'SERVICE.OFFERING.CREATE','Completed','Successfully created new service offering with name: Medium Instance, Virtual Networking.',1,1,'2011-03-17 00:56:24','INFO',0,'soId=8\nname=Medium Instance, Virtual Networking\nnumCPUs=1\nram=1024\ncpuSpeed=1000\ndisplayText=Medium Instance, Virtual Networking, $0.10 per hour\nguestIPType=Virtualized\nlocalStorageRequired=false\nofferHA=false\nuseVirtualNetwork=true\n'),(3,'SERVICE.OFFERING.CREATE','Completed','Successfully created new service offering with name: Small Instance, Direct Networking.',1,1,'2011-03-17 00:56:24','INFO',0,'soId=9\nname=Small Instance, Direct Networking\nnumCPUs=1\nram=512\ncpuSpeed=500\ndisplayText=Small Instance, Direct Networking, $0.05 per hour\nguestIPType=DirectSingle\nlocalStorageRequired=false\nofferHA=false\nuseVirtualNetwork=false\n'),(4,'SERVICE.OFFERING.CREATE','Completed','Successfully created new service offering with name: Medium Instance, Direct Networking.',1,1,'2011-03-17 00:56:24','INFO',0,'soId=10\nname=Medium Instance, Direct Networking\nnumCPUs=1\nram=1024\ncpuSpeed=1000\ndisplayText=Medium Instance, Direct Networking, $0.10 per hour\nguestIPType=DirectSingle\nlocalStorageRequired=false\nofferHA=false\nuseVirtualNetwork=false\n'),(5,'CONFIGURATION.VALUE.EDIT','Completed','Successfully edited configuration value.',1,1,'2011-03-17 00:56:24','INFO',0,'name=mount.parent\nvalue=/mnt'),(6,'CONFIGURATION.VALUE.EDIT','Completed','Successfully edited configuration value.',1,1,'2011-03-17 00:56:24','INFO',0,'name=host\nvalue=192.168.1.178'),(7,'ZONE.CREATE','Completed','Successfully created new zone with name: Default.',1,1,'2011-03-17 00:56:24','INFO',0,'dcId=2\ndns1=192.168.10.254\ninternalDns1=192.168.10.254\nvnetRange=1000-2000\nguestCidr=10.1.1.0/24'),(8,'POD.CREATE','Completed','Successfully created new pod with name: Default in zone: Default.',1,1,'2011-03-17 00:56:25','INFO',0,'podId=2\nzoneId=2\ngateway=192.168.1.1\ncidr=192.168.1.1/24\n'),(9,'POD.DELETE','Completed','Successfully deleted pod with name: Default in zone: Default.',1,1,'2011-03-17 00:56:45','INFO',0,'podId=2\ndcId=2'),(10,'ZONE.DELETE','Completed','Successfully deleted zone with name: Default.',1,1,'2011-03-17 00:56:45','INFO',0,'dcId=2'),(11,'POD.DELETE','Completed','Successfully deleted pod with name: POD1 in zone: ZONE1.',1,1,'2011-03-17 00:56:45','INFO',0,'podId=1\ndcId=1'),(12,'ZONE.DELETE','Completed','Successfully deleted zone with name: ZONE1.',1,1,'2011-03-17 00:56:49','INFO',0,'dcId=1'),(13,'ZONE.CREATE','Completed','Successfully created new zone with name: Basic.',1,1,'2011-03-17 00:56:49','INFO',0,'dcId=3\ndns1=72.52.126.11\ninternalDns1=192.168.10.253\nvnetRange=500-600\nguestCidr=10.1.1.0/24'),(14,'POD.CREATE','Completed','Successfully created new pod with name: basic pod in zone: Basic.',1,1,'2011-03-17 00:56:49','INFO',0,'podId=3\nzoneId=3\ngateway=192.168.152.1\ncidr=192.168.152.0/24\nstartIp=192.168.152.100\nendIp=192.168.152.110'),(15,'VLAN.IP.RANGE.CREATE','Completed','Successfully created new VLAN (tag = 24, gateway = 172.24.0.1, netmask = 255.255.0.0, start IP = 172.24.0.111, end IP = 172.24.0.234.',1,1,'2011-03-17 00:56:49','INFO',0,'vlanType=VirtualNetwork\ndcId=3\nvlanId=24\nvlanGateway=172.24.0.1\nvlanNetmask=255.255.0.0\nstartIP=172.24.0.111\nendIP=172.24.0.234\n'),(16,'TEMPLATE.DOWNLOAD.FAILED','Completed','Storage server nfs://192.168.153.15/export/secondary-storage-stress-2.1.x disconnected during download of template CentOS 5.3(x86_64) no GUI',1,1,'2011-03-17 00:56:49','WARN',0,NULL),(17,'TEMPLATE.DOWNLOAD.FAILED','Completed','CentOS 5.3(x86_64) no GUI failed to download to storage server nfs://192.168.153.15/export/secondary-storage-stress-2.1.x',1,1,'2011-03-17 00:56:49','ERROR',0,NULL),(18,'SSVM.CREATE','Completed','New Secondary Storage VM created - s-1-TEST',1,1,'2011-03-17 00:57:29','INFO',0,NULL),(19,'PROXY.CREATE','Completed','New console proxy created - v-2-TEST',1,1,'2011-03-17 00:57:29','INFO',0,NULL),(20,'SSVM.START','Started','Starting secondary storage Vm Id: 1',1,1,'2011-03-17 00:58:24','INFO',0,NULL),(21,'SSVM.START','Completed','Secondary Storage VM started - s-1-TEST',1,1,'2011-03-17 00:58:55','INFO',0,NULL),(22,'PROXY.START','Completed','Console proxy started - v-2-TEST',1,1,'2011-03-17 00:59:01','INFO',0,NULL),(23,'USER.LOGIN','Completed','user has logged in',2,2,'2011-03-17 00:59:21','INFO',0,NULL),(24,'VM.CREATE','Scheduled','Scheduled async job for deploying Vm',2,2,'2011-03-17 00:59:29','INFO',0,NULL),(25,'VM.CREATE','Started','Deploying Vm',2,2,'2011-03-17 00:59:29','INFO',24,NULL),(26,'NET.IPASSIGN','Completed','Acquired a public ip: 172.24.0.140',1,2,'2011-03-17 00:59:29','INFO',0,'address=172.24.0.140\nsourceNat=true\ndcId=3'),(27,'ROUTER.CREATE','Completed','successfully created Domain Router : r-4-TEST with ip : 172.24.0.140',1,2,'2011-03-17 00:59:30','INFO',0,NULL),(28,'VM.CREATE','Scheduled','Scheduled async job for deploying Vm',2,2,'2011-03-17 00:59:32','INFO',0,NULL),(29,'VM.CREATE','Started','Deploying Vm',2,2,'2011-03-17 00:59:32','INFO',28,NULL),(30,'VOLUME.CREATE','Completed','Created volume: i-2-3-TEST-ROOT with size: 8192 MB',2,2,'2011-03-17 01:00:16','INFO',0,'id=4\ndoId=-1\ntId=2\ndcId=3\nsize=8192'),(31,'VM.CREATE','Completed','successfully created VM instance : i-2-3-TEST',2,2,'2011-03-17 01:00:16','INFO',24,'id=3\nvmName=i-2-3-TEST\nsoId=7\ndoId=-1\ntId=2\ndcId=3'),(32,'VM.START','Started','Starting Vm with Id: 3',2,2,'2011-03-17 01:00:16','INFO',0,NULL),(33,'ROUTER.START','Started','Starting Router with Id: 4',1,2,'2011-03-17 01:00:16','INFO',0,NULL),(34,'VOLUME.CREATE','Completed','Created volume: i-2-5-TEST-ROOT with size: 8192 MB',2,2,'2011-03-17 01:00:17','INFO',0,'id=5\ndoId=-1\ntId=2\ndcId=3\nsize=8192'),(35,'VM.CREATE','Completed','successfully created VM instance : i-2-5-TEST',2,2,'2011-03-17 01:00:17','INFO',28,'id=5\nvmName=i-2-5-TEST\nsoId=7\ndoId=-1\ntId=2\ndcId=3'),(36,'VM.START','Started','Starting Vm with Id: 5',2,2,'2011-03-17 01:00:17','INFO',0,NULL),(37,'ROUTER.START','Completed','successfully started Domain Router: r-4-TEST',1,2,'2011-03-17 01:00:53','INFO',0,NULL),(38,'VM.START','Completed','successfully started VM: i-2-3-TEST',2,2,'2011-03-17 01:01:00','INFO',32,'id=3\nvmName=i-2-3-TEST\nsoId=7\ndoId=-1\ntId=2\ndcId=3'),(39,'VM.START','Completed','successfully started VM: i-2-5-TEST',2,2,'2011-03-17 01:01:04','INFO',36,'id=5\nvmName=i-2-5-TEST\nsoId=7\ndoId=-1\ntId=2\ndcId=3'),(40,'NET.RULEADD','Completed','created new ip forwarding rule [172.24.0.140:41]->[10.1.1.2:42] TCP',2,2,'2011-03-17 01:01:29','INFO',0,NULL),(41,'NET.RULEADD','Completed','created new ip forwarding rule [172.24.0.140:51]->[10.1.1.2:52] TCP',2,2,'2011-03-17 01:01:34','INFO',0,NULL),(42,'LB.CREATE','Completed','Successfully created load balancer alenalb on ip address 172.24.0.140[100->101]',2,2,'2011-03-17 01:04:03','INFO',0,'id=1\ndcId=3'),(43,'LB.CREATE','Completed','Successfully created load balancer alenalb1 on ip address 172.24.0.140[200->201]',2,2,'2011-03-17 01:04:10','INFO',0,'id=2\ndcId=3'),(44,'NET.RULEADD','Completed','created new load balancer rule [172.24.0.140:100]->[10.1.1.2:101] TCP',2,2,'2011-03-17 01:04:15','INFO',0,NULL),(45,'NET.RULEADD','Completed','created new load balancer rule [172.24.0.140:200]->[10.1.1.2:201] TCP',2,2,'2011-03-17 01:04:20','INFO',0,NULL),(46,'NET.RULEADD','Completed','created new load balancer rule [172.24.0.140:100]->[10.1.1.3:101] TCP',2,2,'2011-03-17 01:04:28','INFO',0,NULL),(47,'NET.RULEADD','Completed','created new load balancer rule [172.24.0.140:200]->[10.1.1.3:201] TCP',2,2,'2011-03-17 01:04:34','INFO',0,NULL),(48,'NET.RULEADD','Completed','created new ip forwarding rule [172.24.0.140:61]->[10.1.1.3:62] TCP',2,2,'2011-03-17 01:04:45','INFO',0,NULL),(49,'NET.RULEADD','Completed','created new ip forwarding rule [172.24.0.140:71]->[10.1.1.3:72] TCP',2,2,'2011-03-17 01:04:54','INFO',0,NULL);
/*!40000 ALTER TABLE `event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ext_lun_alloc`
--

DROP TABLE IF EXISTS `ext_lun_alloc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ext_lun_alloc` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `size` bigint(20) unsigned NOT NULL COMMENT 'virtual size',
  `portal` varchar(255) NOT NULL COMMENT 'ip or host name to the storage server',
  `target_iqn` varchar(255) NOT NULL COMMENT 'target iqn',
  `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'data center id this belongs to',
  `lun` int(11) NOT NULL COMMENT 'lun',
  `taken` datetime DEFAULT NULL COMMENT 'time occupied',
  `volume_id` bigint(20) unsigned DEFAULT NULL COMMENT 'vm taking this lun',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `i_ext_lun_alloc__target_iqn__lun` (`target_iqn`,`lun`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ext_lun_alloc`
--

LOCK TABLES `ext_lun_alloc` WRITE;
/*!40000 ALTER TABLE `ext_lun_alloc` DISABLE KEYS */;
/*!40000 ALTER TABLE `ext_lun_alloc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ext_lun_details`
--

DROP TABLE IF EXISTS `ext_lun_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ext_lun_details` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `ext_lun_id` bigint(20) unsigned NOT NULL COMMENT 'lun id',
  `tag` varchar(255) DEFAULT NULL COMMENT 'tags associated with this vm',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fk_ext_lun_details__ext_lun_id` (`ext_lun_id`),
  CONSTRAINT `fk_ext_lun_details__ext_lun_id` FOREIGN KEY (`ext_lun_id`) REFERENCES `ext_lun_alloc` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ext_lun_details`
--

LOCK TABLES `ext_lun_details` WRITE;
/*!40000 ALTER TABLE `ext_lun_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `ext_lun_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guest_os`
--

DROP TABLE IF EXISTS `guest_os`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `guest_os` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `category_id` bigint(20) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `display_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_guest_os__category_id` (`category_id`),
  CONSTRAINT `fk_guest_os__category_id` FOREIGN KEY (`category_id`) REFERENCES `guest_os_category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guest_os`
--

LOCK TABLES `guest_os` WRITE;
/*!40000 ALTER TABLE `guest_os` DISABLE KEYS */;
INSERT INTO `guest_os` VALUES (1,1,'CentOS 4.5 (32-bit)','CentOS 4.5 (32-bit)'),(2,1,'CentOS 4.6 (32-bit)','CentOS 4.6 (32-bit)'),(3,1,'CentOS 4.7 (32-bit)','CentOS 4.7 (32-bit)'),(4,1,'CentOS 4.8 (32-bit)','CentOS 4.8 (32-bit)'),(5,1,'CentOS 5.0 (32-bit)','CentOS 5.0 (32-bit)'),(6,1,'CentOS 5.0 (64-bit)','CentOS 5.0 (64-bit)'),(7,1,'CentOS 5.1 (32-bit)','CentOS 5.1 (32-bit)'),(8,1,'CentOS 5.1 (64-bit)','CentOS 5.1 (64-bit)'),(9,1,'CentOS 5.2 (32-bit)','CentOS 5.2 (32-bit)'),(10,1,'CentOS 5.2 (64-bit)','CentOS 5.2 (64-bit)'),(11,1,'CentOS 5.3 (32-bit)','CentOS 5.3 (32-bit)'),(12,1,'CentOS 5.3 (64-bit)','CentOS 5.3 (64-bit)'),(13,1,'CentOS 5.4 (32-bit)','CentOS 5.4 (32-bit)'),(14,1,'CentOS 5.4 (64-bit)','CentOS 5.4 (64-bit)'),(15,2,'Debian Lenny 5.0 (32-bit)','Debian Lenny 5.0 (32-bit)'),(16,3,'Oracle Enterprise Linux 5.0 (32-bit)','Oracle Enterprise Linux 5.0 (32-bit)'),(17,3,'Oracle Enterprise Linux 5.0 (64-bit)','Oracle Enterprise Linux 5.0 (64-bit)'),(18,3,'Oracle Enterprise Linux 5.1 (32-bit)','Oracle Enterprise Linux 5.1 (32-bit)'),(19,3,'Oracle Enterprise Linux 5.1 (64-bit)','Oracle Enterprise Linux 5.1 (64-bit)'),(20,3,'Oracle Enterprise Linux 5.2 (32-bit)','Oracle Enterprise Linux 5.2 (32-bit)'),(21,3,'Oracle Enterprise Linux 5.2 (64-bit)','Oracle Enterprise Linux 5.2 (64-bit)'),(22,3,'Oracle Enterprise Linux 5.3 (32-bit)','Oracle Enterprise Linux 5.3 (32-bit)'),(23,3,'Oracle Enterprise Linux 5.3 (64-bit)','Oracle Enterprise Linux 5.3 (64-bit)'),(24,3,'Oracle Enterprise Linux 5.4 (32-bit)','Oracle Enterprise Linux 5.4 (32-bit)'),(25,3,'Oracle Enterprise Linux 5.4 (64-bit)','Oracle Enterprise Linux 5.4 (64-bit)'),(26,4,'Red Hat Enterprise Linux 4.5 (32-bit)','Red Hat Enterprise Linux 4.5 (32-bit)'),(27,4,'Red Hat Enterprise Linux 4.6 (32-bit)','Red Hat Enterprise Linux 4.6 (32-bit)'),(28,4,'Red Hat Enterprise Linux 4.7 (32-bit)','Red Hat Enterprise Linux 4.7 (32-bit)'),(29,4,'Red Hat Enterprise Linux 4.8 (32-bit)','Red Hat Enterprise Linux 4.8 (32-bit)'),(30,4,'Red Hat Enterprise Linux 5.0 (32-bit)','Red Hat Enterprise Linux 5.0 (32-bit)'),(31,4,'Red Hat Enterprise Linux 5.0 (64-bit)','Red Hat Enterprise Linux 5.0 (64-bit)'),(32,4,'Red Hat Enterprise Linux 5.1 (32-bit)','Red Hat Enterprise Linux 5.1 (32-bit)'),(33,4,'Red Hat Enterprise Linux 5.1 (64-bit)','Red Hat Enterprise Linux 5.1 (64-bit)'),(34,4,'Red Hat Enterprise Linux 5.2 (32-bit)','Red Hat Enterprise Linux 5.2 (32-bit)'),(35,4,'Red Hat Enterprise Linux 5.2 (64-bit)','Red Hat Enterprise Linux 5.2 (64-bit)'),(36,4,'Red Hat Enterprise Linux 5.3 (32-bit)','Red Hat Enterprise Linux 5.3 (32-bit)'),(37,4,'Red Hat Enterprise Linux 5.3 (64-bit)','Red Hat Enterprise Linux 5.3 (64-bit)'),(38,4,'Red Hat Enterprise Linux 5.4 (32-bit)','Red Hat Enterprise Linux 5.4 (32-bit)'),(39,4,'Red Hat Enterprise Linux 5.4 (64-bit)','Red Hat Enterprise Linux 5.4 (64-bit)'),(40,5,'SUSE Linux Enterprise Server 9 SP4 (32-bit)','SUSE Linux Enterprise Server 9 SP4 (32-bit)'),(41,5,'SUSE Linux Enterprise Server 10 SP1 (32-bit)','SUSE Linux Enterprise Server 10 SP1 (32-bit)'),(42,5,'SUSE Linux Enterprise Server 10 SP1 (64-bit)','SUSE Linux Enterprise Server 10 SP1 (64-bit)'),(43,5,'SUSE Linux Enterprise Server 10 SP2 (32-bit)','SUSE Linux Enterprise Server 10 SP2 (32-bit)'),(44,5,'SUSE Linux Enterprise Server 10 SP2 (64-bit)','SUSE Linux Enterprise Server 10 SP2 (64-bit)'),(45,5,'SUSE Linux Enterprise Server 10 SP3 (64-bit)','SUSE Linux Enterprise Server 10 SP3 (64-bit)'),(46,5,'SUSE Linux Enterprise Server 11 (32-bit)','SUSE Linux Enterprise Server 11 (32-bit)'),(47,5,'SUSE Linux Enterprise Server 11 (64-bit)','SUSE Linux Enterprise Server 11 (64-bit)'),(48,6,'Windows 7 (32-bit)','Windows 7 (32-bit)'),(49,6,'Windows 7 (64-bit)','Windows 7 (64-bit)'),(50,6,'Windows Server 2003 (32-bit)','Windows Server 2003 (32-bit)'),(51,6,'Windows Server 2003 (64-bit)','Windows Server 2003 (64-bit)'),(52,6,'Windows Server 2008 (32-bit)','Windows Server 2008 (32-bit)'),(53,6,'Windows Server 2008 (64-bit)','Windows Server 2008 (64-bit)'),(54,6,'Windows Server 2008 R2 (64-bit)','Windows Server 2008 R2 (64-bit)'),(55,6,'Windows 2000 SP4 (32-bit)','Windows 2000 SP4 (32-bit)'),(56,6,'Windows Vista (32-bit)','Windows Vista (32-bit)'),(57,6,'Windows XP SP2 (32-bit)','Windows XP SP2 (32-bit)'),(58,6,'Windows XP SP3 (32-bit)','Windows XP SP3 (32-bit)'),(59,7,'Other install media','Ubuntu'),(60,7,'Other install media','Other');
/*!40000 ALTER TABLE `guest_os` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guest_os_category`
--

DROP TABLE IF EXISTS `guest_os_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `guest_os_category` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guest_os_category`
--

LOCK TABLES `guest_os_category` WRITE;
/*!40000 ALTER TABLE `guest_os_category` DISABLE KEYS */;
INSERT INTO `guest_os_category` VALUES (1,'CentOS'),(2,'Debian'),(3,'Oracle'),(4,'RedHat'),(5,'SUSE'),(6,'Windows'),(7,'Other');
/*!40000 ALTER TABLE `guest_os_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `host`
--

DROP TABLE IF EXISTS `host`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `host` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `status` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `private_ip_address` varchar(15) NOT NULL,
  `private_netmask` varchar(15) DEFAULT NULL,
  `private_mac_address` varchar(17) DEFAULT NULL,
  `storage_ip_address` varchar(15) NOT NULL,
  `storage_netmask` varchar(15) DEFAULT NULL,
  `storage_mac_address` varchar(17) DEFAULT NULL,
  `storage_ip_address_2` varchar(15) DEFAULT NULL,
  `storage_mac_address_2` varchar(17) DEFAULT NULL,
  `storage_netmask_2` varchar(15) DEFAULT NULL,
  `cluster_id` bigint(20) unsigned DEFAULT NULL COMMENT 'foreign key to cluster',
  `public_ip_address` varchar(15) DEFAULT NULL,
  `public_netmask` varchar(15) DEFAULT NULL,
  `public_mac_address` varchar(17) DEFAULT NULL,
  `proxy_port` int(10) unsigned DEFAULT NULL,
  `data_center_id` bigint(20) unsigned NOT NULL,
  `pod_id` bigint(20) unsigned DEFAULT NULL,
  `cpus` int(10) unsigned DEFAULT NULL,
  `speed` int(10) unsigned DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL COMMENT 'iqn for the servers',
  `fs_type` varchar(32) DEFAULT NULL,
  `hypervisor_type` varchar(32) DEFAULT NULL COMMENT 'hypervisor type, can be NONE for storage',
  `ram` bigint(20) unsigned DEFAULT NULL,
  `resource` varchar(255) DEFAULT NULL COMMENT 'If it is a local resource, this is the class name',
  `version` varchar(40) NOT NULL,
  `sequence` bigint(20) unsigned NOT NULL DEFAULT '1',
  `parent` varchar(255) DEFAULT NULL COMMENT 'parent path for the storage server',
  `total_size` bigint(20) unsigned DEFAULT NULL COMMENT 'TotalSize',
  `capabilities` varchar(255) DEFAULT NULL COMMENT 'host capabilities in comma separated list',
  `guid` varchar(255) DEFAULT NULL,
  `available` int(1) unsigned NOT NULL DEFAULT '1' COMMENT 'Is this host ready for more resources?',
  `setup` int(1) unsigned NOT NULL DEFAULT '0' COMMENT 'Is this host already setup?',
  `dom0_memory` bigint(20) unsigned NOT NULL COMMENT 'memory used by dom0 for computing and routing servers',
  `last_ping` int(10) unsigned NOT NULL COMMENT 'time in seconds from the start of machine of the last ping',
  `mgmt_server_id` bigint(20) unsigned DEFAULT NULL COMMENT 'ManagementServer this host is connected to.',
  `disconnected` datetime DEFAULT NULL COMMENT 'Time this was disconnected',
  `created` datetime DEFAULT NULL COMMENT 'date the host first signed on',
  `removed` datetime DEFAULT NULL COMMENT 'date removed if not null',
  PRIMARY KEY (`id`),
  UNIQUE KEY `guid` (`guid`),
  KEY `i_host__removed` (`removed`),
  KEY `i_host__last_ping` (`last_ping`),
  KEY `i_host__status` (`status`),
  KEY `i_host__data_center_id` (`data_center_id`),
  KEY `i_host__pod_id` (`pod_id`),
  KEY `fk_host__cluster_id` (`cluster_id`),
  CONSTRAINT `fk_host__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`),
  CONSTRAINT `fk_host__pod_id` FOREIGN KEY (`pod_id`) REFERENCES `host_pod_ref` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `host`
--

LOCK TABLES `host` WRITE;
/*!40000 ALTER TABLE `host` DISABLE KEYS */;
INSERT INTO `host` VALUES (1,'nfs://192.168.153.15/export/secondary-storage-stress-2.1.x','Up','SecondaryStorage','192.168.153.15','255.255.255.0','06:03:ed:ac:00:01','192.168.152.104','255.255.255.0','06:03:ed:ac:00:01',NULL,NULL,NULL,NULL,'172.24.0.120','255.255.0.0','06:83:ed:ac:00:01',NULL,3,3,NULL,NULL,'nfs://192.168.153.15/export/secondary-storage-stress-2.1.x',NULL,'None',0,NULL,'1.9.1.2011-03-17T00:55:20Z',17,'/mnt/SecStorage/5965348c',3910488817664,NULL,'nfs://192.168.153.15/export/secondary-storage-stress-2.1.x',1,0,0,1269847850,120890037145,'2011-03-17 00:56:49','2011-03-17 00:56:49',NULL),(2,'x18','Up','Routing','192.168.152.18','255.255.255.0','a4:ba:db:11:23:53','192.168.152.18','255.255.255.0','a4:ba:db:11:23:53','192.168.152.18','a4:ba:db:11:23:53','255.255.255.0',1,NULL,NULL,NULL,NULL,3,3,4,2500,'iqn.2005-03.org.open-iscsi:cbd43c6c21f8',NULL,'XenServer',15998459904,'com.cloud.hypervisor.xen.resource.XenServer56Resource','1.9.1.2011-03-17T00:55:20Z',67,NULL,NULL,'xen-3.0-x86_64 , xen-3.0-x86_32p , hvm-3.0-x86_32 , hvm-3.0-x86_32p , hvm-3.0-x86_64','db432159-18ae-4983-b4df-81425ce5e84a',1,1,0,1269847852,120890037145,NULL,'2011-03-17 00:56:56',NULL),(3,'v-2-TEST','Up','ConsoleProxy','192.168.152.106','255.255.255.0','06:03:c7:31:00:03','192.168.152.106','255.255.255.0','06:03:c7:31:00:03',NULL,NULL,NULL,NULL,'172.24.0.141','255.255.0.0','06:83:c7:31:00:03',80,3,3,NULL,NULL,'NoIqn',NULL,NULL,0,NULL,'1.9.1.2011-03-17T00:55:20Z',2,NULL,NULL,NULL,'Proxy.2-ConsoleProxyResource',1,0,0,1269847851,120890037145,NULL,'2011-03-17 00:58:59',NULL);
/*!40000 ALTER TABLE `host` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `host_details`
--

DROP TABLE IF EXISTS `host_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `host_details` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `host_id` bigint(20) unsigned NOT NULL COMMENT 'host id',
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_host_details__host_id` (`host_id`),
  CONSTRAINT `fk_host_details__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `host_details`
--

LOCK TABLES `host_details` WRITE;
/*!40000 ALTER TABLE `host_details` DISABLE KEYS */;
INSERT INTO `host_details` VALUES (1,1,'mount.parent','dummy'),(2,1,'mount.path','dummy'),(3,1,'orig.url','nfs://192.168.153.15/export/secondary-storage-stress-2.1.x'),(4,2,'com.cloud.network.NetworkEnums.RouterPrivateIpStrategy','DcGlobal'),(5,2,'public.network.device','cloud-private'),(6,2,'private.network.device','cloud-private'),(7,2,'Hypervisor.Version','3.4.2'),(8,2,'Host.OS','XenServer'),(9,2,'Host.OS.Kernel.Version','2.6.27.42-0.1.1.xs5.6.0.44.111158xen'),(10,2,'wait','1800'),(11,2,'storage.network.device2','cloud-stor2'),(12,2,'password','password'),(13,2,'storage.network.device1','cloud-stor1'),(14,2,'url','192.168.152.18'),(15,2,'username','root'),(16,2,'pool','e8e08107-da59-e1ff-8881-66057f3a74f9'),(17,2,'guest.network.device','cloud-private'),(18,2,'can_bridge_firewall','false'),(19,2,'Host.OS.Version','5.6.0'),(20,2,'instance.name','TEST');
/*!40000 ALTER TABLE `host_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `host_pod_ref`
--

DROP TABLE IF EXISTS `host_pod_ref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `host_pod_ref` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `data_center_id` bigint(20) unsigned NOT NULL,
  `gateway` varchar(255) NOT NULL COMMENT 'gateway for the pod',
  `cidr_address` varchar(15) NOT NULL COMMENT 'CIDR address for the pod',
  `cidr_size` bigint(20) unsigned NOT NULL COMMENT 'CIDR size for the pod',
  `description` varchar(255) DEFAULT NULL COMMENT 'store private ip range in startIP-endIP format',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `name` (`name`,`data_center_id`),
  KEY `i_host_pod_ref__data_center_id` (`data_center_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `host_pod_ref`
--

LOCK TABLES `host_pod_ref` WRITE;
/*!40000 ALTER TABLE `host_pod_ref` DISABLE KEYS */;
INSERT INTO `host_pod_ref` VALUES (3,'basic pod',3,'192.168.152.1','192.168.152.0',24,'192.168.152.100-192.168.152.110');
/*!40000 ALTER TABLE `host_pod_ref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `host_tags`
--

DROP TABLE IF EXISTS `host_tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `host_tags` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `host_id` bigint(20) unsigned NOT NULL COMMENT 'host id',
  `tag` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_host_tags__host_id` (`host_id`),
  CONSTRAINT `fk_host_tags__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `host_tags`
--

LOCK TABLES `host_tags` WRITE;
/*!40000 ALTER TABLE `host_tags` DISABLE KEYS */;
/*!40000 ALTER TABLE `host_tags` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ip_forwarding`
--

DROP TABLE IF EXISTS `ip_forwarding`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ip_forwarding` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `group_id` bigint(20) unsigned DEFAULT NULL,
  `public_ip_address` varchar(15) NOT NULL,
  `public_port` varchar(10) DEFAULT NULL,
  `private_ip_address` varchar(15) NOT NULL,
  `private_port` varchar(10) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `protocol` varchar(16) NOT NULL DEFAULT 'TCP',
  `forwarding` tinyint(1) NOT NULL DEFAULT '1',
  `algorithm` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `i_ip_forwarding__forwarding` (`forwarding`),
  KEY `i_ip_forwarding__public_ip_address__public_port` (`public_ip_address`,`public_port`),
  KEY `i_ip_forwarding__public_ip_address` (`public_ip_address`),
  CONSTRAINT `fk_ip_forwarding__public_ip_address` FOREIGN KEY (`public_ip_address`) REFERENCES `user_ip_address` (`public_ip_address`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ip_forwarding`
--

LOCK TABLES `ip_forwarding` WRITE;
/*!40000 ALTER TABLE `ip_forwarding` DISABLE KEYS */;
INSERT INTO `ip_forwarding` VALUES (1,NULL,'172.24.0.140','41','10.1.1.2','42',1,'TCP',1,NULL),(2,NULL,'172.24.0.140','51','10.1.1.2','52',1,'TCP',1,NULL),(3,1,'172.24.0.140','100','10.1.1.2','101',1,'TCP',0,'roundrobin'),(4,2,'172.24.0.140','200','10.1.1.2','201',1,'TCP',0,'roundrobin'),(5,1,'172.24.0.140','100','10.1.1.3','101',1,'TCP',0,'roundrobin'),(6,2,'172.24.0.140','200','10.1.1.3','201',1,'TCP',0,'roundrobin'),(7,NULL,'172.24.0.140','61','10.1.1.3','62',1,'TCP',1,NULL),(8,NULL,'172.24.0.140','71','10.1.1.3','72',1,'TCP',1,NULL);
/*!40000 ALTER TABLE `ip_forwarding` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `launch_permission`
--

DROP TABLE IF EXISTS `launch_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `launch_permission` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `template_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `i_launch_permission_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `launch_permission`
--

LOCK TABLES `launch_permission` WRITE;
/*!40000 ALTER TABLE `launch_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `launch_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `load_balancer`
--

DROP TABLE IF EXISTS `load_balancer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `load_balancer` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096) DEFAULT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `ip_address` varchar(15) NOT NULL,
  `public_port` varchar(10) NOT NULL,
  `private_port` varchar(10) NOT NULL,
  `algorithm` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `load_balancer`
--

LOCK TABLES `load_balancer` WRITE;
/*!40000 ALTER TABLE `load_balancer` DISABLE KEYS */;
INSERT INTO `load_balancer` VALUES (1,'alenalb',NULL,2,'172.24.0.140','100','101','roundrobin'),(2,'alenalb1',NULL,2,'172.24.0.140','200','201','roundrobin');
/*!40000 ALTER TABLE `load_balancer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `load_balancer_vm_map`
--

DROP TABLE IF EXISTS `load_balancer_vm_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `load_balancer_vm_map` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `load_balancer_id` bigint(20) unsigned NOT NULL,
  `instance_id` bigint(20) unsigned NOT NULL,
  `pending` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT 'whether the vm is being applied to the load balancer (pending=1) or has already been applied (pending=0)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `load_balancer_vm_map`
--

LOCK TABLES `load_balancer_vm_map` WRITE;
/*!40000 ALTER TABLE `load_balancer_vm_map` DISABLE KEYS */;
INSERT INTO `load_balancer_vm_map` VALUES (1,1,3,0),(2,2,3,0),(3,1,5,0),(4,2,5,0);
/*!40000 ALTER TABLE `load_balancer_vm_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mshost`
--

DROP TABLE IF EXISTS `mshost`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mshost` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `msid` bigint(20) NOT NULL COMMENT 'management server id derived from MAC address',
  `name` varchar(255) DEFAULT NULL,
  `version` varchar(255) DEFAULT NULL,
  `service_ip` varchar(15) NOT NULL,
  `service_port` int(11) NOT NULL,
  `last_update` datetime DEFAULT NULL COMMENT 'Last record update time',
  `removed` datetime DEFAULT NULL COMMENT 'date removed if not null',
  `alert_count` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `msid` (`msid`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mshost`
--

LOCK TABLES `mshost` WRITE;
/*!40000 ALTER TABLE `mshost` DISABLE KEYS */;
INSERT INTO `mshost` VALUES (1,120890037145,'alena','1.9.1.2011-03-17T00:55:20Z','127.0.0.1',9090,'2011-03-17 01:10:51',NULL,0);
/*!40000 ALTER TABLE `mshost` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `netapp_lun`
--

DROP TABLE IF EXISTS `netapp_lun`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `netapp_lun` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `lun_name` varchar(255) NOT NULL COMMENT 'lun name',
  `target_iqn` varchar(255) NOT NULL COMMENT 'target iqn',
  `path` varchar(255) NOT NULL COMMENT 'lun path',
  `size` bigint(20) NOT NULL COMMENT 'lun size',
  `volume_id` bigint(20) unsigned NOT NULL COMMENT 'parent volume id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `i_netapp_lun__volume_id` (`volume_id`),
  KEY `i_netapp_lun__lun_name` (`lun_name`),
  CONSTRAINT `fk_netapp_lun__volume_id` FOREIGN KEY (`volume_id`) REFERENCES `netapp_volume` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `netapp_lun`
--

LOCK TABLES `netapp_lun` WRITE;
/*!40000 ALTER TABLE `netapp_lun` DISABLE KEYS */;
/*!40000 ALTER TABLE `netapp_lun` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `netapp_pool`
--

DROP TABLE IF EXISTS `netapp_pool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `netapp_pool` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name for the pool',
  `algorithm` varchar(255) NOT NULL COMMENT 'algorithm',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `netapp_pool`
--

LOCK TABLES `netapp_pool` WRITE;
/*!40000 ALTER TABLE `netapp_pool` DISABLE KEYS */;
/*!40000 ALTER TABLE `netapp_pool` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `netapp_volume`
--

DROP TABLE IF EXISTS `netapp_volume`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `netapp_volume` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `ip_address` varchar(255) NOT NULL COMMENT 'ip address/fqdn of the volume',
  `pool_id` bigint(20) unsigned NOT NULL COMMENT 'id for the pool',
  `pool_name` varchar(255) NOT NULL COMMENT 'name for the pool',
  `aggregate_name` varchar(255) NOT NULL COMMENT 'name for the aggregate',
  `volume_name` varchar(255) NOT NULL COMMENT 'name for the volume',
  `volume_size` varchar(255) NOT NULL COMMENT 'volume size',
  `snapshot_policy` varchar(255) NOT NULL COMMENT 'snapshot policy',
  `snapshot_reservation` int(11) NOT NULL COMMENT 'snapshot reservation',
  `username` varchar(255) NOT NULL COMMENT 'username',
  `password` varchar(200) DEFAULT NULL COMMENT 'password',
  `round_robin_marker` int(11) DEFAULT NULL COMMENT 'This marks the volume to be picked up for lun creation, RR fashion',
  PRIMARY KEY (`ip_address`,`aggregate_name`,`volume_name`),
  UNIQUE KEY `id` (`id`),
  KEY `i_netapp_volume__pool_id` (`pool_id`),
  CONSTRAINT `fk_netapp_volume__pool_id` FOREIGN KEY (`pool_id`) REFERENCES `netapp_pool` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `netapp_volume`
--

LOCK TABLES `netapp_volume` WRITE;
/*!40000 ALTER TABLE `netapp_volume` DISABLE KEYS */;
/*!40000 ALTER TABLE `netapp_volume` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `network_group`
--

DROP TABLE IF EXISTS `network_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `network_group` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096) DEFAULT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `account_name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_network_group___account_id` (`account_id`),
  KEY `fk_network_group__domain_id` (`domain_id`),
  KEY `i_network_group_name` (`name`),
  CONSTRAINT `fk_network_group__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`),
  CONSTRAINT `fk_network_group___account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `network_group`
--

LOCK TABLES `network_group` WRITE;
/*!40000 ALTER TABLE `network_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `network_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `network_group_vm_map`
--

DROP TABLE IF EXISTS `network_group_vm_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `network_group_vm_map` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `network_group_id` bigint(20) unsigned NOT NULL,
  `instance_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_network_group_vm_map___network_group_id` (`network_group_id`),
  KEY `fk_network_group_vm_map___instance_id` (`instance_id`),
  CONSTRAINT `fk_network_group_vm_map___instance_id` FOREIGN KEY (`instance_id`) REFERENCES `user_vm` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_network_group_vm_map___network_group_id` FOREIGN KEY (`network_group_id`) REFERENCES `network_group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `network_group_vm_map`
--

LOCK TABLES `network_group_vm_map` WRITE;
/*!40000 ALTER TABLE `network_group_vm_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `network_group_vm_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `network_ingress_rule`
--

DROP TABLE IF EXISTS `network_ingress_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `network_ingress_rule` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `network_group_id` bigint(20) unsigned NOT NULL,
  `start_port` varchar(10) DEFAULT NULL,
  `end_port` varchar(10) DEFAULT NULL,
  `protocol` varchar(16) NOT NULL DEFAULT 'TCP',
  `allowed_network_id` bigint(20) unsigned DEFAULT NULL,
  `allowed_network_group` varchar(255) DEFAULT NULL COMMENT 'data duplicated from network_group table to avoid lots of joins when listing rules (the name of the group should be displayed rather than just id)',
  `allowed_net_grp_acct` varchar(100) DEFAULT NULL COMMENT 'data duplicated from network_group table to avoid lots of joins when listing rules (the name of the group owner should be displayed)',
  `allowed_ip_cidr` varchar(44) DEFAULT NULL,
  `create_status` varchar(32) DEFAULT NULL COMMENT 'rule creation status',
  PRIMARY KEY (`id`),
  KEY `i_network_ingress_rule_network_id` (`network_group_id`),
  KEY `i_network_ingress_rule_allowed_network` (`allowed_network_id`),
  CONSTRAINT `fk_network_ingress_rule___allowed_network_id` FOREIGN KEY (`allowed_network_id`) REFERENCES `network_group` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_network_ingress_rule___network_group_id` FOREIGN KEY (`network_group_id`) REFERENCES `network_group` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `network_ingress_rule`
--

LOCK TABLES `network_ingress_rule` WRITE;
/*!40000 ALTER TABLE `network_ingress_rule` DISABLE KEYS */;
/*!40000 ALTER TABLE `network_ingress_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `network_rule_config`
--

DROP TABLE IF EXISTS `network_rule_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `network_rule_config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `security_group_id` bigint(20) unsigned NOT NULL,
  `public_port` varchar(10) DEFAULT NULL,
  `private_port` varchar(10) DEFAULT NULL,
  `protocol` varchar(16) NOT NULL DEFAULT 'TCP',
  `create_status` varchar(32) DEFAULT NULL COMMENT 'rule creation status',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `network_rule_config`
--

LOCK TABLES `network_rule_config` WRITE;
/*!40000 ALTER TABLE `network_rule_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `network_rule_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `op_dc_ip_address_alloc`
--

DROP TABLE IF EXISTS `op_dc_ip_address_alloc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `op_dc_ip_address_alloc` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `ip_address` varchar(15) NOT NULL COMMENT 'ip address',
  `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'data center it belongs to',
  `pod_id` bigint(20) unsigned NOT NULL COMMENT 'pod it belongs to',
  `instance_id` bigint(20) unsigned DEFAULT NULL COMMENT 'instance id',
  `taken` datetime DEFAULT NULL COMMENT 'Date taken',
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_op_dc_ip_address_alloc__ip_address__data_center_id` (`ip_address`,`data_center_id`),
  KEY `i_op_dc_ip_address_alloc__pod_id__data_center_id__taken` (`pod_id`,`data_center_id`,`taken`,`instance_id`),
  KEY `i_op_dc_ip_address_alloc__pod_id` (`pod_id`),
  CONSTRAINT `fk_op_dc_ip_address_alloc__pod_id` FOREIGN KEY (`pod_id`) REFERENCES `host_pod_ref` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=163 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `op_dc_ip_address_alloc`
--

LOCK TABLES `op_dc_ip_address_alloc` WRITE;
/*!40000 ALTER TABLE `op_dc_ip_address_alloc` DISABLE KEYS */;
INSERT INTO `op_dc_ip_address_alloc` VALUES (152,'192.168.152.100',3,3,NULL,NULL),(153,'192.168.152.101',3,3,NULL,NULL),(154,'192.168.152.102',3,3,NULL,NULL),(155,'192.168.152.103',3,3,NULL,NULL),(156,'192.168.152.104',3,3,1,'2011-03-17 00:58:24'),(157,'192.168.152.105',3,3,NULL,NULL),(158,'192.168.152.106',3,3,2,'2011-03-17 00:58:25'),(159,'192.168.152.107',3,3,NULL,NULL),(160,'192.168.152.108',3,3,NULL,NULL),(161,'192.168.152.109',3,3,NULL,NULL),(162,'192.168.152.110',3,3,NULL,NULL);
/*!40000 ALTER TABLE `op_dc_ip_address_alloc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `op_dc_link_local_ip_address_alloc`
--

DROP TABLE IF EXISTS `op_dc_link_local_ip_address_alloc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `op_dc_link_local_ip_address_alloc` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `ip_address` varchar(15) NOT NULL COMMENT 'ip address',
  `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'data center it belongs to',
  `pod_id` bigint(20) unsigned NOT NULL COMMENT 'pod it belongs to',
  `instance_id` bigint(20) unsigned DEFAULT NULL COMMENT 'instance id',
  `taken` datetime DEFAULT NULL COMMENT 'Date taken',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3064 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `op_dc_link_local_ip_address_alloc`
--

LOCK TABLES `op_dc_link_local_ip_address_alloc` WRITE;
/*!40000 ALTER TABLE `op_dc_link_local_ip_address_alloc` DISABLE KEYS */;
INSERT INTO `op_dc_link_local_ip_address_alloc` VALUES (1,'169.254.0.2',1,1,NULL,NULL),(2,'169.254.0.3',1,1,NULL,NULL),(3,'169.254.0.4',1,1,NULL,NULL),(4,'169.254.0.5',1,1,NULL,NULL),(5,'169.254.0.6',1,1,NULL,NULL),(6,'169.254.0.7',1,1,NULL,NULL),(7,'169.254.0.8',1,1,NULL,NULL),(8,'169.254.0.9',1,1,NULL,NULL),(9,'169.254.0.10',1,1,NULL,NULL),(10,'169.254.0.11',1,1,NULL,NULL),(11,'169.254.0.12',1,1,NULL,NULL),(12,'169.254.0.13',1,1,NULL,NULL),(13,'169.254.0.14',1,1,NULL,NULL),(14,'169.254.0.15',1,1,NULL,NULL),(15,'169.254.0.16',1,1,NULL,NULL),(16,'169.254.0.17',1,1,NULL,NULL),(17,'169.254.0.18',1,1,NULL,NULL),(18,'169.254.0.19',1,1,NULL,NULL),(19,'169.254.0.20',1,1,NULL,NULL),(20,'169.254.0.21',1,1,NULL,NULL),(21,'169.254.0.22',1,1,NULL,NULL),(22,'169.254.0.23',1,1,NULL,NULL),(23,'169.254.0.24',1,1,NULL,NULL),(24,'169.254.0.25',1,1,NULL,NULL),(25,'169.254.0.26',1,1,NULL,NULL),(26,'169.254.0.27',1,1,NULL,NULL),(27,'169.254.0.28',1,1,NULL,NULL),(28,'169.254.0.29',1,1,NULL,NULL),(29,'169.254.0.30',1,1,NULL,NULL),(30,'169.254.0.31',1,1,NULL,NULL),(31,'169.254.0.32',1,1,NULL,NULL),(32,'169.254.0.33',1,1,NULL,NULL),(33,'169.254.0.34',1,1,NULL,NULL),(34,'169.254.0.35',1,1,NULL,NULL),(35,'169.254.0.36',1,1,NULL,NULL),(36,'169.254.0.37',1,1,NULL,NULL),(37,'169.254.0.38',1,1,NULL,NULL),(38,'169.254.0.39',1,1,NULL,NULL),(39,'169.254.0.40',1,1,NULL,NULL),(40,'169.254.0.41',1,1,NULL,NULL),(41,'169.254.0.42',1,1,NULL,NULL),(42,'169.254.0.43',1,1,NULL,NULL),(43,'169.254.0.44',1,1,NULL,NULL),(44,'169.254.0.45',1,1,NULL,NULL),(45,'169.254.0.46',1,1,NULL,NULL),(46,'169.254.0.47',1,1,NULL,NULL),(47,'169.254.0.48',1,1,NULL,NULL),(48,'169.254.0.49',1,1,NULL,NULL),(49,'169.254.0.50',1,1,NULL,NULL),(50,'169.254.0.51',1,1,NULL,NULL),(51,'169.254.0.52',1,1,NULL,NULL),(52,'169.254.0.53',1,1,NULL,NULL),(53,'169.254.0.54',1,1,NULL,NULL),(54,'169.254.0.55',1,1,NULL,NULL),(55,'169.254.0.56',1,1,NULL,NULL),(56,'169.254.0.57',1,1,NULL,NULL),(57,'169.254.0.58',1,1,NULL,NULL),(58,'169.254.0.59',1,1,NULL,NULL),(59,'169.254.0.60',1,1,NULL,NULL),(60,'169.254.0.61',1,1,NULL,NULL),(61,'169.254.0.62',1,1,NULL,NULL),(62,'169.254.0.63',1,1,NULL,NULL),(63,'169.254.0.64',1,1,NULL,NULL),(64,'169.254.0.65',1,1,NULL,NULL),(65,'169.254.0.66',1,1,NULL,NULL),(66,'169.254.0.67',1,1,NULL,NULL),(67,'169.254.0.68',1,1,NULL,NULL),(68,'169.254.0.69',1,1,NULL,NULL),(69,'169.254.0.70',1,1,NULL,NULL),(70,'169.254.0.71',1,1,NULL,NULL),(71,'169.254.0.72',1,1,NULL,NULL),(72,'169.254.0.73',1,1,NULL,NULL),(73,'169.254.0.74',1,1,NULL,NULL),(74,'169.254.0.75',1,1,NULL,NULL),(75,'169.254.0.76',1,1,NULL,NULL),(76,'169.254.0.77',1,1,NULL,NULL),(77,'169.254.0.78',1,1,NULL,NULL),(78,'169.254.0.79',1,1,NULL,NULL),(79,'169.254.0.80',1,1,NULL,NULL),(80,'169.254.0.81',1,1,NULL,NULL),(81,'169.254.0.82',1,1,NULL,NULL),(82,'169.254.0.83',1,1,NULL,NULL),(83,'169.254.0.84',1,1,NULL,NULL),(84,'169.254.0.85',1,1,NULL,NULL),(85,'169.254.0.86',1,1,NULL,NULL),(86,'169.254.0.87',1,1,NULL,NULL),(87,'169.254.0.88',1,1,NULL,NULL),(88,'169.254.0.89',1,1,NULL,NULL),(89,'169.254.0.90',1,1,NULL,NULL),(90,'169.254.0.91',1,1,NULL,NULL),(91,'169.254.0.92',1,1,NULL,NULL),(92,'169.254.0.93',1,1,NULL,NULL),(93,'169.254.0.94',1,1,NULL,NULL),(94,'169.254.0.95',1,1,NULL,NULL),(95,'169.254.0.96',1,1,NULL,NULL),(96,'169.254.0.97',1,1,NULL,NULL),(97,'169.254.0.98',1,1,NULL,NULL),(98,'169.254.0.99',1,1,NULL,NULL),(99,'169.254.0.100',1,1,NULL,NULL),(100,'169.254.0.101',1,1,NULL,NULL),(101,'169.254.0.102',1,1,NULL,NULL),(102,'169.254.0.103',1,1,NULL,NULL),(103,'169.254.0.104',1,1,NULL,NULL),(104,'169.254.0.105',1,1,NULL,NULL),(105,'169.254.0.106',1,1,NULL,NULL),(106,'169.254.0.107',1,1,NULL,NULL),(107,'169.254.0.108',1,1,NULL,NULL),(108,'169.254.0.109',1,1,NULL,NULL),(109,'169.254.0.110',1,1,NULL,NULL),(110,'169.254.0.111',1,1,NULL,NULL),(111,'169.254.0.112',1,1,NULL,NULL),(112,'169.254.0.113',1,1,NULL,NULL),(113,'169.254.0.114',1,1,NULL,NULL),(114,'169.254.0.115',1,1,NULL,NULL),(115,'169.254.0.116',1,1,NULL,NULL),(116,'169.254.0.117',1,1,NULL,NULL),(117,'169.254.0.118',1,1,NULL,NULL),(118,'169.254.0.119',1,1,NULL,NULL),(119,'169.254.0.120',1,1,NULL,NULL),(120,'169.254.0.121',1,1,NULL,NULL),(121,'169.254.0.122',1,1,NULL,NULL),(122,'169.254.0.123',1,1,NULL,NULL),(123,'169.254.0.124',1,1,NULL,NULL),(124,'169.254.0.125',1,1,NULL,NULL),(125,'169.254.0.126',1,1,NULL,NULL),(126,'169.254.0.127',1,1,NULL,NULL),(127,'169.254.0.128',1,1,NULL,NULL),(128,'169.254.0.129',1,1,NULL,NULL),(129,'169.254.0.130',1,1,NULL,NULL),(130,'169.254.0.131',1,1,NULL,NULL),(131,'169.254.0.132',1,1,NULL,NULL),(132,'169.254.0.133',1,1,NULL,NULL),(133,'169.254.0.134',1,1,NULL,NULL),(134,'169.254.0.135',1,1,NULL,NULL),(135,'169.254.0.136',1,1,NULL,NULL),(136,'169.254.0.137',1,1,NULL,NULL),(137,'169.254.0.138',1,1,NULL,NULL),(138,'169.254.0.139',1,1,NULL,NULL),(139,'169.254.0.140',1,1,NULL,NULL),(140,'169.254.0.141',1,1,NULL,NULL),(141,'169.254.0.142',1,1,NULL,NULL),(142,'169.254.0.143',1,1,NULL,NULL),(143,'169.254.0.144',1,1,NULL,NULL),(144,'169.254.0.145',1,1,NULL,NULL),(145,'169.254.0.146',1,1,NULL,NULL),(146,'169.254.0.147',1,1,NULL,NULL),(147,'169.254.0.148',1,1,NULL,NULL),(148,'169.254.0.149',1,1,NULL,NULL),(149,'169.254.0.150',1,1,NULL,NULL),(150,'169.254.0.151',1,1,NULL,NULL),(151,'169.254.0.152',1,1,NULL,NULL),(152,'169.254.0.153',1,1,NULL,NULL),(153,'169.254.0.154',1,1,NULL,NULL),(154,'169.254.0.155',1,1,NULL,NULL),(155,'169.254.0.156',1,1,NULL,NULL),(156,'169.254.0.157',1,1,NULL,NULL),(157,'169.254.0.158',1,1,NULL,NULL),(158,'169.254.0.159',1,1,NULL,NULL),(159,'169.254.0.160',1,1,NULL,NULL),(160,'169.254.0.161',1,1,NULL,NULL),(161,'169.254.0.162',1,1,NULL,NULL),(162,'169.254.0.163',1,1,NULL,NULL),(163,'169.254.0.164',1,1,NULL,NULL),(164,'169.254.0.165',1,1,NULL,NULL),(165,'169.254.0.166',1,1,NULL,NULL),(166,'169.254.0.167',1,1,NULL,NULL),(167,'169.254.0.168',1,1,NULL,NULL),(168,'169.254.0.169',1,1,NULL,NULL),(169,'169.254.0.170',1,1,NULL,NULL),(170,'169.254.0.171',1,1,NULL,NULL),(171,'169.254.0.172',1,1,NULL,NULL),(172,'169.254.0.173',1,1,NULL,NULL),(173,'169.254.0.174',1,1,NULL,NULL),(174,'169.254.0.175',1,1,NULL,NULL),(175,'169.254.0.176',1,1,NULL,NULL),(176,'169.254.0.177',1,1,NULL,NULL),(177,'169.254.0.178',1,1,NULL,NULL),(178,'169.254.0.179',1,1,NULL,NULL),(179,'169.254.0.180',1,1,NULL,NULL),(180,'169.254.0.181',1,1,NULL,NULL),(181,'169.254.0.182',1,1,NULL,NULL),(182,'169.254.0.183',1,1,NULL,NULL),(183,'169.254.0.184',1,1,NULL,NULL),(184,'169.254.0.185',1,1,NULL,NULL),(185,'169.254.0.186',1,1,NULL,NULL),(186,'169.254.0.187',1,1,NULL,NULL),(187,'169.254.0.188',1,1,NULL,NULL),(188,'169.254.0.189',1,1,NULL,NULL),(189,'169.254.0.190',1,1,NULL,NULL),(190,'169.254.0.191',1,1,NULL,NULL),(191,'169.254.0.192',1,1,NULL,NULL),(192,'169.254.0.193',1,1,NULL,NULL),(193,'169.254.0.194',1,1,NULL,NULL),(194,'169.254.0.195',1,1,NULL,NULL),(195,'169.254.0.196',1,1,NULL,NULL),(196,'169.254.0.197',1,1,NULL,NULL),(197,'169.254.0.198',1,1,NULL,NULL),(198,'169.254.0.199',1,1,NULL,NULL),(199,'169.254.0.200',1,1,NULL,NULL),(200,'169.254.0.201',1,1,NULL,NULL),(201,'169.254.0.202',1,1,NULL,NULL),(202,'169.254.0.203',1,1,NULL,NULL),(203,'169.254.0.204',1,1,NULL,NULL),(204,'169.254.0.205',1,1,NULL,NULL),(205,'169.254.0.206',1,1,NULL,NULL),(206,'169.254.0.207',1,1,NULL,NULL),(207,'169.254.0.208',1,1,NULL,NULL),(208,'169.254.0.209',1,1,NULL,NULL),(209,'169.254.0.210',1,1,NULL,NULL),(210,'169.254.0.211',1,1,NULL,NULL),(211,'169.254.0.212',1,1,NULL,NULL),(212,'169.254.0.213',1,1,NULL,NULL),(213,'169.254.0.214',1,1,NULL,NULL),(214,'169.254.0.215',1,1,NULL,NULL),(215,'169.254.0.216',1,1,NULL,NULL),(216,'169.254.0.217',1,1,NULL,NULL),(217,'169.254.0.218',1,1,NULL,NULL),(218,'169.254.0.219',1,1,NULL,NULL),(219,'169.254.0.220',1,1,NULL,NULL),(220,'169.254.0.221',1,1,NULL,NULL),(221,'169.254.0.222',1,1,NULL,NULL),(222,'169.254.0.223',1,1,NULL,NULL),(223,'169.254.0.224',1,1,NULL,NULL),(224,'169.254.0.225',1,1,NULL,NULL),(225,'169.254.0.226',1,1,NULL,NULL),(226,'169.254.0.227',1,1,NULL,NULL),(227,'169.254.0.228',1,1,NULL,NULL),(228,'169.254.0.229',1,1,NULL,NULL),(229,'169.254.0.230',1,1,NULL,NULL),(230,'169.254.0.231',1,1,NULL,NULL),(231,'169.254.0.232',1,1,NULL,NULL),(232,'169.254.0.233',1,1,NULL,NULL),(233,'169.254.0.234',1,1,NULL,NULL),(234,'169.254.0.235',1,1,NULL,NULL),(235,'169.254.0.236',1,1,NULL,NULL),(236,'169.254.0.237',1,1,NULL,NULL),(237,'169.254.0.238',1,1,NULL,NULL),(238,'169.254.0.239',1,1,NULL,NULL),(239,'169.254.0.240',1,1,NULL,NULL),(240,'169.254.0.241',1,1,NULL,NULL),(241,'169.254.0.242',1,1,NULL,NULL),(242,'169.254.0.243',1,1,NULL,NULL),(243,'169.254.0.244',1,1,NULL,NULL),(244,'169.254.0.245',1,1,NULL,NULL),(245,'169.254.0.246',1,1,NULL,NULL),(246,'169.254.0.247',1,1,NULL,NULL),(247,'169.254.0.248',1,1,NULL,NULL),(248,'169.254.0.249',1,1,NULL,NULL),(249,'169.254.0.250',1,1,NULL,NULL),(250,'169.254.0.251',1,1,NULL,NULL),(251,'169.254.0.252',1,1,NULL,NULL),(252,'169.254.0.253',1,1,NULL,NULL),(253,'169.254.0.254',1,1,NULL,NULL),(254,'169.254.0.255',1,1,NULL,NULL),(255,'169.254.1.0',1,1,NULL,NULL),(256,'169.254.1.1',1,1,NULL,NULL),(257,'169.254.1.2',1,1,NULL,NULL),(258,'169.254.1.3',1,1,NULL,NULL),(259,'169.254.1.4',1,1,NULL,NULL),(260,'169.254.1.5',1,1,NULL,NULL),(261,'169.254.1.6',1,1,NULL,NULL),(262,'169.254.1.7',1,1,NULL,NULL),(263,'169.254.1.8',1,1,NULL,NULL),(264,'169.254.1.9',1,1,NULL,NULL),(265,'169.254.1.10',1,1,NULL,NULL),(266,'169.254.1.11',1,1,NULL,NULL),(267,'169.254.1.12',1,1,NULL,NULL),(268,'169.254.1.13',1,1,NULL,NULL),(269,'169.254.1.14',1,1,NULL,NULL),(270,'169.254.1.15',1,1,NULL,NULL),(271,'169.254.1.16',1,1,NULL,NULL),(272,'169.254.1.17',1,1,NULL,NULL),(273,'169.254.1.18',1,1,NULL,NULL),(274,'169.254.1.19',1,1,NULL,NULL),(275,'169.254.1.20',1,1,NULL,NULL),(276,'169.254.1.21',1,1,NULL,NULL),(277,'169.254.1.22',1,1,NULL,NULL),(278,'169.254.1.23',1,1,NULL,NULL),(279,'169.254.1.24',1,1,NULL,NULL),(280,'169.254.1.25',1,1,NULL,NULL),(281,'169.254.1.26',1,1,NULL,NULL),(282,'169.254.1.27',1,1,NULL,NULL),(283,'169.254.1.28',1,1,NULL,NULL),(284,'169.254.1.29',1,1,NULL,NULL),(285,'169.254.1.30',1,1,NULL,NULL),(286,'169.254.1.31',1,1,NULL,NULL),(287,'169.254.1.32',1,1,NULL,NULL),(288,'169.254.1.33',1,1,NULL,NULL),(289,'169.254.1.34',1,1,NULL,NULL),(290,'169.254.1.35',1,1,NULL,NULL),(291,'169.254.1.36',1,1,NULL,NULL),(292,'169.254.1.37',1,1,NULL,NULL),(293,'169.254.1.38',1,1,NULL,NULL),(294,'169.254.1.39',1,1,NULL,NULL),(295,'169.254.1.40',1,1,NULL,NULL),(296,'169.254.1.41',1,1,NULL,NULL),(297,'169.254.1.42',1,1,NULL,NULL),(298,'169.254.1.43',1,1,NULL,NULL),(299,'169.254.1.44',1,1,NULL,NULL),(300,'169.254.1.45',1,1,NULL,NULL),(301,'169.254.1.46',1,1,NULL,NULL),(302,'169.254.1.47',1,1,NULL,NULL),(303,'169.254.1.48',1,1,NULL,NULL),(304,'169.254.1.49',1,1,NULL,NULL),(305,'169.254.1.50',1,1,NULL,NULL),(306,'169.254.1.51',1,1,NULL,NULL),(307,'169.254.1.52',1,1,NULL,NULL),(308,'169.254.1.53',1,1,NULL,NULL),(309,'169.254.1.54',1,1,NULL,NULL),(310,'169.254.1.55',1,1,NULL,NULL),(311,'169.254.1.56',1,1,NULL,NULL),(312,'169.254.1.57',1,1,NULL,NULL),(313,'169.254.1.58',1,1,NULL,NULL),(314,'169.254.1.59',1,1,NULL,NULL),(315,'169.254.1.60',1,1,NULL,NULL),(316,'169.254.1.61',1,1,NULL,NULL),(317,'169.254.1.62',1,1,NULL,NULL),(318,'169.254.1.63',1,1,NULL,NULL),(319,'169.254.1.64',1,1,NULL,NULL),(320,'169.254.1.65',1,1,NULL,NULL),(321,'169.254.1.66',1,1,NULL,NULL),(322,'169.254.1.67',1,1,NULL,NULL),(323,'169.254.1.68',1,1,NULL,NULL),(324,'169.254.1.69',1,1,NULL,NULL),(325,'169.254.1.70',1,1,NULL,NULL),(326,'169.254.1.71',1,1,NULL,NULL),(327,'169.254.1.72',1,1,NULL,NULL),(328,'169.254.1.73',1,1,NULL,NULL),(329,'169.254.1.74',1,1,NULL,NULL),(330,'169.254.1.75',1,1,NULL,NULL),(331,'169.254.1.76',1,1,NULL,NULL),(332,'169.254.1.77',1,1,NULL,NULL),(333,'169.254.1.78',1,1,NULL,NULL),(334,'169.254.1.79',1,1,NULL,NULL),(335,'169.254.1.80',1,1,NULL,NULL),(336,'169.254.1.81',1,1,NULL,NULL),(337,'169.254.1.82',1,1,NULL,NULL),(338,'169.254.1.83',1,1,NULL,NULL),(339,'169.254.1.84',1,1,NULL,NULL),(340,'169.254.1.85',1,1,NULL,NULL),(341,'169.254.1.86',1,1,NULL,NULL),(342,'169.254.1.87',1,1,NULL,NULL),(343,'169.254.1.88',1,1,NULL,NULL),(344,'169.254.1.89',1,1,NULL,NULL),(345,'169.254.1.90',1,1,NULL,NULL),(346,'169.254.1.91',1,1,NULL,NULL),(347,'169.254.1.92',1,1,NULL,NULL),(348,'169.254.1.93',1,1,NULL,NULL),(349,'169.254.1.94',1,1,NULL,NULL),(350,'169.254.1.95',1,1,NULL,NULL),(351,'169.254.1.96',1,1,NULL,NULL),(352,'169.254.1.97',1,1,NULL,NULL),(353,'169.254.1.98',1,1,NULL,NULL),(354,'169.254.1.99',1,1,NULL,NULL),(355,'169.254.1.100',1,1,NULL,NULL),(356,'169.254.1.101',1,1,NULL,NULL),(357,'169.254.1.102',1,1,NULL,NULL),(358,'169.254.1.103',1,1,NULL,NULL),(359,'169.254.1.104',1,1,NULL,NULL),(360,'169.254.1.105',1,1,NULL,NULL),(361,'169.254.1.106',1,1,NULL,NULL),(362,'169.254.1.107',1,1,NULL,NULL),(363,'169.254.1.108',1,1,NULL,NULL),(364,'169.254.1.109',1,1,NULL,NULL),(365,'169.254.1.110',1,1,NULL,NULL),(366,'169.254.1.111',1,1,NULL,NULL),(367,'169.254.1.112',1,1,NULL,NULL),(368,'169.254.1.113',1,1,NULL,NULL),(369,'169.254.1.114',1,1,NULL,NULL),(370,'169.254.1.115',1,1,NULL,NULL),(371,'169.254.1.116',1,1,NULL,NULL),(372,'169.254.1.117',1,1,NULL,NULL),(373,'169.254.1.118',1,1,NULL,NULL),(374,'169.254.1.119',1,1,NULL,NULL),(375,'169.254.1.120',1,1,NULL,NULL),(376,'169.254.1.121',1,1,NULL,NULL),(377,'169.254.1.122',1,1,NULL,NULL),(378,'169.254.1.123',1,1,NULL,NULL),(379,'169.254.1.124',1,1,NULL,NULL),(380,'169.254.1.125',1,1,NULL,NULL),(381,'169.254.1.126',1,1,NULL,NULL),(382,'169.254.1.127',1,1,NULL,NULL),(383,'169.254.1.128',1,1,NULL,NULL),(384,'169.254.1.129',1,1,NULL,NULL),(385,'169.254.1.130',1,1,NULL,NULL),(386,'169.254.1.131',1,1,NULL,NULL),(387,'169.254.1.132',1,1,NULL,NULL),(388,'169.254.1.133',1,1,NULL,NULL),(389,'169.254.1.134',1,1,NULL,NULL),(390,'169.254.1.135',1,1,NULL,NULL),(391,'169.254.1.136',1,1,NULL,NULL),(392,'169.254.1.137',1,1,NULL,NULL),(393,'169.254.1.138',1,1,NULL,NULL),(394,'169.254.1.139',1,1,NULL,NULL),(395,'169.254.1.140',1,1,NULL,NULL),(396,'169.254.1.141',1,1,NULL,NULL),(397,'169.254.1.142',1,1,NULL,NULL),(398,'169.254.1.143',1,1,NULL,NULL),(399,'169.254.1.144',1,1,NULL,NULL),(400,'169.254.1.145',1,1,NULL,NULL),(401,'169.254.1.146',1,1,NULL,NULL),(402,'169.254.1.147',1,1,NULL,NULL),(403,'169.254.1.148',1,1,NULL,NULL),(404,'169.254.1.149',1,1,NULL,NULL),(405,'169.254.1.150',1,1,NULL,NULL),(406,'169.254.1.151',1,1,NULL,NULL),(407,'169.254.1.152',1,1,NULL,NULL),(408,'169.254.1.153',1,1,NULL,NULL),(409,'169.254.1.154',1,1,NULL,NULL),(410,'169.254.1.155',1,1,NULL,NULL),(411,'169.254.1.156',1,1,NULL,NULL),(412,'169.254.1.157',1,1,NULL,NULL),(413,'169.254.1.158',1,1,NULL,NULL),(414,'169.254.1.159',1,1,NULL,NULL),(415,'169.254.1.160',1,1,NULL,NULL),(416,'169.254.1.161',1,1,NULL,NULL),(417,'169.254.1.162',1,1,NULL,NULL),(418,'169.254.1.163',1,1,NULL,NULL),(419,'169.254.1.164',1,1,NULL,NULL),(420,'169.254.1.165',1,1,NULL,NULL),(421,'169.254.1.166',1,1,NULL,NULL),(422,'169.254.1.167',1,1,NULL,NULL),(423,'169.254.1.168',1,1,NULL,NULL),(424,'169.254.1.169',1,1,NULL,NULL),(425,'169.254.1.170',1,1,NULL,NULL),(426,'169.254.1.171',1,1,NULL,NULL),(427,'169.254.1.172',1,1,NULL,NULL),(428,'169.254.1.173',1,1,NULL,NULL),(429,'169.254.1.174',1,1,NULL,NULL),(430,'169.254.1.175',1,1,NULL,NULL),(431,'169.254.1.176',1,1,NULL,NULL),(432,'169.254.1.177',1,1,NULL,NULL),(433,'169.254.1.178',1,1,NULL,NULL),(434,'169.254.1.179',1,1,NULL,NULL),(435,'169.254.1.180',1,1,NULL,NULL),(436,'169.254.1.181',1,1,NULL,NULL),(437,'169.254.1.182',1,1,NULL,NULL),(438,'169.254.1.183',1,1,NULL,NULL),(439,'169.254.1.184',1,1,NULL,NULL),(440,'169.254.1.185',1,1,NULL,NULL),(441,'169.254.1.186',1,1,NULL,NULL),(442,'169.254.1.187',1,1,NULL,NULL),(443,'169.254.1.188',1,1,NULL,NULL),(444,'169.254.1.189',1,1,NULL,NULL),(445,'169.254.1.190',1,1,NULL,NULL),(446,'169.254.1.191',1,1,NULL,NULL),(447,'169.254.1.192',1,1,NULL,NULL),(448,'169.254.1.193',1,1,NULL,NULL),(449,'169.254.1.194',1,1,NULL,NULL),(450,'169.254.1.195',1,1,NULL,NULL),(451,'169.254.1.196',1,1,NULL,NULL),(452,'169.254.1.197',1,1,NULL,NULL),(453,'169.254.1.198',1,1,NULL,NULL),(454,'169.254.1.199',1,1,NULL,NULL),(455,'169.254.1.200',1,1,NULL,NULL),(456,'169.254.1.201',1,1,NULL,NULL),(457,'169.254.1.202',1,1,NULL,NULL),(458,'169.254.1.203',1,1,NULL,NULL),(459,'169.254.1.204',1,1,NULL,NULL),(460,'169.254.1.205',1,1,NULL,NULL),(461,'169.254.1.206',1,1,NULL,NULL),(462,'169.254.1.207',1,1,NULL,NULL),(463,'169.254.1.208',1,1,NULL,NULL),(464,'169.254.1.209',1,1,NULL,NULL),(465,'169.254.1.210',1,1,NULL,NULL),(466,'169.254.1.211',1,1,NULL,NULL),(467,'169.254.1.212',1,1,NULL,NULL),(468,'169.254.1.213',1,1,NULL,NULL),(469,'169.254.1.214',1,1,NULL,NULL),(470,'169.254.1.215',1,1,NULL,NULL),(471,'169.254.1.216',1,1,NULL,NULL),(472,'169.254.1.217',1,1,NULL,NULL),(473,'169.254.1.218',1,1,NULL,NULL),(474,'169.254.1.219',1,1,NULL,NULL),(475,'169.254.1.220',1,1,NULL,NULL),(476,'169.254.1.221',1,1,NULL,NULL),(477,'169.254.1.222',1,1,NULL,NULL),(478,'169.254.1.223',1,1,NULL,NULL),(479,'169.254.1.224',1,1,NULL,NULL),(480,'169.254.1.225',1,1,NULL,NULL),(481,'169.254.1.226',1,1,NULL,NULL),(482,'169.254.1.227',1,1,NULL,NULL),(483,'169.254.1.228',1,1,NULL,NULL),(484,'169.254.1.229',1,1,NULL,NULL),(485,'169.254.1.230',1,1,NULL,NULL),(486,'169.254.1.231',1,1,NULL,NULL),(487,'169.254.1.232',1,1,NULL,NULL),(488,'169.254.1.233',1,1,NULL,NULL),(489,'169.254.1.234',1,1,NULL,NULL),(490,'169.254.1.235',1,1,NULL,NULL),(491,'169.254.1.236',1,1,NULL,NULL),(492,'169.254.1.237',1,1,NULL,NULL),(493,'169.254.1.238',1,1,NULL,NULL),(494,'169.254.1.239',1,1,NULL,NULL),(495,'169.254.1.240',1,1,NULL,NULL),(496,'169.254.1.241',1,1,NULL,NULL),(497,'169.254.1.242',1,1,NULL,NULL),(498,'169.254.1.243',1,1,NULL,NULL),(499,'169.254.1.244',1,1,NULL,NULL),(500,'169.254.1.245',1,1,NULL,NULL),(501,'169.254.1.246',1,1,NULL,NULL),(502,'169.254.1.247',1,1,NULL,NULL),(503,'169.254.1.248',1,1,NULL,NULL),(504,'169.254.1.249',1,1,NULL,NULL),(505,'169.254.1.250',1,1,NULL,NULL),(506,'169.254.1.251',1,1,NULL,NULL),(507,'169.254.1.252',1,1,NULL,NULL),(508,'169.254.1.253',1,1,NULL,NULL),(509,'169.254.1.254',1,1,NULL,NULL),(510,'169.254.1.255',1,1,NULL,NULL),(511,'169.254.2.0',1,1,NULL,NULL),(512,'169.254.2.1',1,1,NULL,NULL),(513,'169.254.2.2',1,1,NULL,NULL),(514,'169.254.2.3',1,1,NULL,NULL),(515,'169.254.2.4',1,1,NULL,NULL),(516,'169.254.2.5',1,1,NULL,NULL),(517,'169.254.2.6',1,1,NULL,NULL),(518,'169.254.2.7',1,1,NULL,NULL),(519,'169.254.2.8',1,1,NULL,NULL),(520,'169.254.2.9',1,1,NULL,NULL),(521,'169.254.2.10',1,1,NULL,NULL),(522,'169.254.2.11',1,1,NULL,NULL),(523,'169.254.2.12',1,1,NULL,NULL),(524,'169.254.2.13',1,1,NULL,NULL),(525,'169.254.2.14',1,1,NULL,NULL),(526,'169.254.2.15',1,1,NULL,NULL),(527,'169.254.2.16',1,1,NULL,NULL),(528,'169.254.2.17',1,1,NULL,NULL),(529,'169.254.2.18',1,1,NULL,NULL),(530,'169.254.2.19',1,1,NULL,NULL),(531,'169.254.2.20',1,1,NULL,NULL),(532,'169.254.2.21',1,1,NULL,NULL),(533,'169.254.2.22',1,1,NULL,NULL),(534,'169.254.2.23',1,1,NULL,NULL),(535,'169.254.2.24',1,1,NULL,NULL),(536,'169.254.2.25',1,1,NULL,NULL),(537,'169.254.2.26',1,1,NULL,NULL),(538,'169.254.2.27',1,1,NULL,NULL),(539,'169.254.2.28',1,1,NULL,NULL),(540,'169.254.2.29',1,1,NULL,NULL),(541,'169.254.2.30',1,1,NULL,NULL),(542,'169.254.2.31',1,1,NULL,NULL),(543,'169.254.2.32',1,1,NULL,NULL),(544,'169.254.2.33',1,1,NULL,NULL),(545,'169.254.2.34',1,1,NULL,NULL),(546,'169.254.2.35',1,1,NULL,NULL),(547,'169.254.2.36',1,1,NULL,NULL),(548,'169.254.2.37',1,1,NULL,NULL),(549,'169.254.2.38',1,1,NULL,NULL),(550,'169.254.2.39',1,1,NULL,NULL),(551,'169.254.2.40',1,1,NULL,NULL),(552,'169.254.2.41',1,1,NULL,NULL),(553,'169.254.2.42',1,1,NULL,NULL),(554,'169.254.2.43',1,1,NULL,NULL),(555,'169.254.2.44',1,1,NULL,NULL),(556,'169.254.2.45',1,1,NULL,NULL),(557,'169.254.2.46',1,1,NULL,NULL),(558,'169.254.2.47',1,1,NULL,NULL),(559,'169.254.2.48',1,1,NULL,NULL),(560,'169.254.2.49',1,1,NULL,NULL),(561,'169.254.2.50',1,1,NULL,NULL),(562,'169.254.2.51',1,1,NULL,NULL),(563,'169.254.2.52',1,1,NULL,NULL),(564,'169.254.2.53',1,1,NULL,NULL),(565,'169.254.2.54',1,1,NULL,NULL),(566,'169.254.2.55',1,1,NULL,NULL),(567,'169.254.2.56',1,1,NULL,NULL),(568,'169.254.2.57',1,1,NULL,NULL),(569,'169.254.2.58',1,1,NULL,NULL),(570,'169.254.2.59',1,1,NULL,NULL),(571,'169.254.2.60',1,1,NULL,NULL),(572,'169.254.2.61',1,1,NULL,NULL),(573,'169.254.2.62',1,1,NULL,NULL),(574,'169.254.2.63',1,1,NULL,NULL),(575,'169.254.2.64',1,1,NULL,NULL),(576,'169.254.2.65',1,1,NULL,NULL),(577,'169.254.2.66',1,1,NULL,NULL),(578,'169.254.2.67',1,1,NULL,NULL),(579,'169.254.2.68',1,1,NULL,NULL),(580,'169.254.2.69',1,1,NULL,NULL),(581,'169.254.2.70',1,1,NULL,NULL),(582,'169.254.2.71',1,1,NULL,NULL),(583,'169.254.2.72',1,1,NULL,NULL),(584,'169.254.2.73',1,1,NULL,NULL),(585,'169.254.2.74',1,1,NULL,NULL),(586,'169.254.2.75',1,1,NULL,NULL),(587,'169.254.2.76',1,1,NULL,NULL),(588,'169.254.2.77',1,1,NULL,NULL),(589,'169.254.2.78',1,1,NULL,NULL),(590,'169.254.2.79',1,1,NULL,NULL),(591,'169.254.2.80',1,1,NULL,NULL),(592,'169.254.2.81',1,1,NULL,NULL),(593,'169.254.2.82',1,1,NULL,NULL),(594,'169.254.2.83',1,1,NULL,NULL),(595,'169.254.2.84',1,1,NULL,NULL),(596,'169.254.2.85',1,1,NULL,NULL),(597,'169.254.2.86',1,1,NULL,NULL),(598,'169.254.2.87',1,1,NULL,NULL),(599,'169.254.2.88',1,1,NULL,NULL),(600,'169.254.2.89',1,1,NULL,NULL),(601,'169.254.2.90',1,1,NULL,NULL),(602,'169.254.2.91',1,1,NULL,NULL),(603,'169.254.2.92',1,1,NULL,NULL),(604,'169.254.2.93',1,1,NULL,NULL),(605,'169.254.2.94',1,1,NULL,NULL),(606,'169.254.2.95',1,1,NULL,NULL),(607,'169.254.2.96',1,1,NULL,NULL),(608,'169.254.2.97',1,1,NULL,NULL),(609,'169.254.2.98',1,1,NULL,NULL),(610,'169.254.2.99',1,1,NULL,NULL),(611,'169.254.2.100',1,1,NULL,NULL),(612,'169.254.2.101',1,1,NULL,NULL),(613,'169.254.2.102',1,1,NULL,NULL),(614,'169.254.2.103',1,1,NULL,NULL),(615,'169.254.2.104',1,1,NULL,NULL),(616,'169.254.2.105',1,1,NULL,NULL),(617,'169.254.2.106',1,1,NULL,NULL),(618,'169.254.2.107',1,1,NULL,NULL),(619,'169.254.2.108',1,1,NULL,NULL),(620,'169.254.2.109',1,1,NULL,NULL),(621,'169.254.2.110',1,1,NULL,NULL),(622,'169.254.2.111',1,1,NULL,NULL),(623,'169.254.2.112',1,1,NULL,NULL),(624,'169.254.2.113',1,1,NULL,NULL),(625,'169.254.2.114',1,1,NULL,NULL),(626,'169.254.2.115',1,1,NULL,NULL),(627,'169.254.2.116',1,1,NULL,NULL),(628,'169.254.2.117',1,1,NULL,NULL),(629,'169.254.2.118',1,1,NULL,NULL),(630,'169.254.2.119',1,1,NULL,NULL),(631,'169.254.2.120',1,1,NULL,NULL),(632,'169.254.2.121',1,1,NULL,NULL),(633,'169.254.2.122',1,1,NULL,NULL),(634,'169.254.2.123',1,1,NULL,NULL),(635,'169.254.2.124',1,1,NULL,NULL),(636,'169.254.2.125',1,1,NULL,NULL),(637,'169.254.2.126',1,1,NULL,NULL),(638,'169.254.2.127',1,1,NULL,NULL),(639,'169.254.2.128',1,1,NULL,NULL),(640,'169.254.2.129',1,1,NULL,NULL),(641,'169.254.2.130',1,1,NULL,NULL),(642,'169.254.2.131',1,1,NULL,NULL),(643,'169.254.2.132',1,1,NULL,NULL),(644,'169.254.2.133',1,1,NULL,NULL),(645,'169.254.2.134',1,1,NULL,NULL),(646,'169.254.2.135',1,1,NULL,NULL),(647,'169.254.2.136',1,1,NULL,NULL),(648,'169.254.2.137',1,1,NULL,NULL),(649,'169.254.2.138',1,1,NULL,NULL),(650,'169.254.2.139',1,1,NULL,NULL),(651,'169.254.2.140',1,1,NULL,NULL),(652,'169.254.2.141',1,1,NULL,NULL),(653,'169.254.2.142',1,1,NULL,NULL),(654,'169.254.2.143',1,1,NULL,NULL),(655,'169.254.2.144',1,1,NULL,NULL),(656,'169.254.2.145',1,1,NULL,NULL),(657,'169.254.2.146',1,1,NULL,NULL),(658,'169.254.2.147',1,1,NULL,NULL),(659,'169.254.2.148',1,1,NULL,NULL),(660,'169.254.2.149',1,1,NULL,NULL),(661,'169.254.2.150',1,1,NULL,NULL),(662,'169.254.2.151',1,1,NULL,NULL),(663,'169.254.2.152',1,1,NULL,NULL),(664,'169.254.2.153',1,1,NULL,NULL),(665,'169.254.2.154',1,1,NULL,NULL),(666,'169.254.2.155',1,1,NULL,NULL),(667,'169.254.2.156',1,1,NULL,NULL),(668,'169.254.2.157',1,1,NULL,NULL),(669,'169.254.2.158',1,1,NULL,NULL),(670,'169.254.2.159',1,1,NULL,NULL),(671,'169.254.2.160',1,1,NULL,NULL),(672,'169.254.2.161',1,1,NULL,NULL),(673,'169.254.2.162',1,1,NULL,NULL),(674,'169.254.2.163',1,1,NULL,NULL),(675,'169.254.2.164',1,1,NULL,NULL),(676,'169.254.2.165',1,1,NULL,NULL),(677,'169.254.2.166',1,1,NULL,NULL),(678,'169.254.2.167',1,1,NULL,NULL),(679,'169.254.2.168',1,1,NULL,NULL),(680,'169.254.2.169',1,1,NULL,NULL),(681,'169.254.2.170',1,1,NULL,NULL),(682,'169.254.2.171',1,1,NULL,NULL),(683,'169.254.2.172',1,1,NULL,NULL),(684,'169.254.2.173',1,1,NULL,NULL),(685,'169.254.2.174',1,1,NULL,NULL),(686,'169.254.2.175',1,1,NULL,NULL),(687,'169.254.2.176',1,1,NULL,NULL),(688,'169.254.2.177',1,1,NULL,NULL),(689,'169.254.2.178',1,1,NULL,NULL),(690,'169.254.2.179',1,1,NULL,NULL),(691,'169.254.2.180',1,1,NULL,NULL),(692,'169.254.2.181',1,1,NULL,NULL),(693,'169.254.2.182',1,1,NULL,NULL),(694,'169.254.2.183',1,1,NULL,NULL),(695,'169.254.2.184',1,1,NULL,NULL),(696,'169.254.2.185',1,1,NULL,NULL),(697,'169.254.2.186',1,1,NULL,NULL),(698,'169.254.2.187',1,1,NULL,NULL),(699,'169.254.2.188',1,1,NULL,NULL),(700,'169.254.2.189',1,1,NULL,NULL),(701,'169.254.2.190',1,1,NULL,NULL),(702,'169.254.2.191',1,1,NULL,NULL),(703,'169.254.2.192',1,1,NULL,NULL),(704,'169.254.2.193',1,1,NULL,NULL),(705,'169.254.2.194',1,1,NULL,NULL),(706,'169.254.2.195',1,1,NULL,NULL),(707,'169.254.2.196',1,1,NULL,NULL),(708,'169.254.2.197',1,1,NULL,NULL),(709,'169.254.2.198',1,1,NULL,NULL),(710,'169.254.2.199',1,1,NULL,NULL),(711,'169.254.2.200',1,1,NULL,NULL),(712,'169.254.2.201',1,1,NULL,NULL),(713,'169.254.2.202',1,1,NULL,NULL),(714,'169.254.2.203',1,1,NULL,NULL),(715,'169.254.2.204',1,1,NULL,NULL),(716,'169.254.2.205',1,1,NULL,NULL),(717,'169.254.2.206',1,1,NULL,NULL),(718,'169.254.2.207',1,1,NULL,NULL),(719,'169.254.2.208',1,1,NULL,NULL),(720,'169.254.2.209',1,1,NULL,NULL),(721,'169.254.2.210',1,1,NULL,NULL),(722,'169.254.2.211',1,1,NULL,NULL),(723,'169.254.2.212',1,1,NULL,NULL),(724,'169.254.2.213',1,1,NULL,NULL),(725,'169.254.2.214',1,1,NULL,NULL),(726,'169.254.2.215',1,1,NULL,NULL),(727,'169.254.2.216',1,1,NULL,NULL),(728,'169.254.2.217',1,1,NULL,NULL),(729,'169.254.2.218',1,1,NULL,NULL),(730,'169.254.2.219',1,1,NULL,NULL),(731,'169.254.2.220',1,1,NULL,NULL),(732,'169.254.2.221',1,1,NULL,NULL),(733,'169.254.2.222',1,1,NULL,NULL),(734,'169.254.2.223',1,1,NULL,NULL),(735,'169.254.2.224',1,1,NULL,NULL),(736,'169.254.2.225',1,1,NULL,NULL),(737,'169.254.2.226',1,1,NULL,NULL),(738,'169.254.2.227',1,1,NULL,NULL),(739,'169.254.2.228',1,1,NULL,NULL),(740,'169.254.2.229',1,1,NULL,NULL),(741,'169.254.2.230',1,1,NULL,NULL),(742,'169.254.2.231',1,1,NULL,NULL),(743,'169.254.2.232',1,1,NULL,NULL),(744,'169.254.2.233',1,1,NULL,NULL),(745,'169.254.2.234',1,1,NULL,NULL),(746,'169.254.2.235',1,1,NULL,NULL),(747,'169.254.2.236',1,1,NULL,NULL),(748,'169.254.2.237',1,1,NULL,NULL),(749,'169.254.2.238',1,1,NULL,NULL),(750,'169.254.2.239',1,1,NULL,NULL),(751,'169.254.2.240',1,1,NULL,NULL),(752,'169.254.2.241',1,1,NULL,NULL),(753,'169.254.2.242',1,1,NULL,NULL),(754,'169.254.2.243',1,1,NULL,NULL),(755,'169.254.2.244',1,1,NULL,NULL),(756,'169.254.2.245',1,1,NULL,NULL),(757,'169.254.2.246',1,1,NULL,NULL),(758,'169.254.2.247',1,1,NULL,NULL),(759,'169.254.2.248',1,1,NULL,NULL),(760,'169.254.2.249',1,1,NULL,NULL),(761,'169.254.2.250',1,1,NULL,NULL),(762,'169.254.2.251',1,1,NULL,NULL),(763,'169.254.2.252',1,1,NULL,NULL),(764,'169.254.2.253',1,1,NULL,NULL),(765,'169.254.2.254',1,1,NULL,NULL),(766,'169.254.2.255',1,1,NULL,NULL),(767,'169.254.3.0',1,1,NULL,NULL),(768,'169.254.3.1',1,1,NULL,NULL),(769,'169.254.3.2',1,1,NULL,NULL),(770,'169.254.3.3',1,1,NULL,NULL),(771,'169.254.3.4',1,1,NULL,NULL),(772,'169.254.3.5',1,1,NULL,NULL),(773,'169.254.3.6',1,1,NULL,NULL),(774,'169.254.3.7',1,1,NULL,NULL),(775,'169.254.3.8',1,1,NULL,NULL),(776,'169.254.3.9',1,1,NULL,NULL),(777,'169.254.3.10',1,1,NULL,NULL),(778,'169.254.3.11',1,1,NULL,NULL),(779,'169.254.3.12',1,1,NULL,NULL),(780,'169.254.3.13',1,1,NULL,NULL),(781,'169.254.3.14',1,1,NULL,NULL),(782,'169.254.3.15',1,1,NULL,NULL),(783,'169.254.3.16',1,1,NULL,NULL),(784,'169.254.3.17',1,1,NULL,NULL),(785,'169.254.3.18',1,1,NULL,NULL),(786,'169.254.3.19',1,1,NULL,NULL),(787,'169.254.3.20',1,1,NULL,NULL),(788,'169.254.3.21',1,1,NULL,NULL),(789,'169.254.3.22',1,1,NULL,NULL),(790,'169.254.3.23',1,1,NULL,NULL),(791,'169.254.3.24',1,1,NULL,NULL),(792,'169.254.3.25',1,1,NULL,NULL),(793,'169.254.3.26',1,1,NULL,NULL),(794,'169.254.3.27',1,1,NULL,NULL),(795,'169.254.3.28',1,1,NULL,NULL),(796,'169.254.3.29',1,1,NULL,NULL),(797,'169.254.3.30',1,1,NULL,NULL),(798,'169.254.3.31',1,1,NULL,NULL),(799,'169.254.3.32',1,1,NULL,NULL),(800,'169.254.3.33',1,1,NULL,NULL),(801,'169.254.3.34',1,1,NULL,NULL),(802,'169.254.3.35',1,1,NULL,NULL),(803,'169.254.3.36',1,1,NULL,NULL),(804,'169.254.3.37',1,1,NULL,NULL),(805,'169.254.3.38',1,1,NULL,NULL),(806,'169.254.3.39',1,1,NULL,NULL),(807,'169.254.3.40',1,1,NULL,NULL),(808,'169.254.3.41',1,1,NULL,NULL),(809,'169.254.3.42',1,1,NULL,NULL),(810,'169.254.3.43',1,1,NULL,NULL),(811,'169.254.3.44',1,1,NULL,NULL),(812,'169.254.3.45',1,1,NULL,NULL),(813,'169.254.3.46',1,1,NULL,NULL),(814,'169.254.3.47',1,1,NULL,NULL),(815,'169.254.3.48',1,1,NULL,NULL),(816,'169.254.3.49',1,1,NULL,NULL),(817,'169.254.3.50',1,1,NULL,NULL),(818,'169.254.3.51',1,1,NULL,NULL),(819,'169.254.3.52',1,1,NULL,NULL),(820,'169.254.3.53',1,1,NULL,NULL),(821,'169.254.3.54',1,1,NULL,NULL),(822,'169.254.3.55',1,1,NULL,NULL),(823,'169.254.3.56',1,1,NULL,NULL),(824,'169.254.3.57',1,1,NULL,NULL),(825,'169.254.3.58',1,1,NULL,NULL),(826,'169.254.3.59',1,1,NULL,NULL),(827,'169.254.3.60',1,1,NULL,NULL),(828,'169.254.3.61',1,1,NULL,NULL),(829,'169.254.3.62',1,1,NULL,NULL),(830,'169.254.3.63',1,1,NULL,NULL),(831,'169.254.3.64',1,1,NULL,NULL),(832,'169.254.3.65',1,1,NULL,NULL),(833,'169.254.3.66',1,1,NULL,NULL),(834,'169.254.3.67',1,1,NULL,NULL),(835,'169.254.3.68',1,1,NULL,NULL),(836,'169.254.3.69',1,1,NULL,NULL),(837,'169.254.3.70',1,1,NULL,NULL),(838,'169.254.3.71',1,1,NULL,NULL),(839,'169.254.3.72',1,1,NULL,NULL),(840,'169.254.3.73',1,1,NULL,NULL),(841,'169.254.3.74',1,1,NULL,NULL),(842,'169.254.3.75',1,1,NULL,NULL),(843,'169.254.3.76',1,1,NULL,NULL),(844,'169.254.3.77',1,1,NULL,NULL),(845,'169.254.3.78',1,1,NULL,NULL),(846,'169.254.3.79',1,1,NULL,NULL),(847,'169.254.3.80',1,1,NULL,NULL),(848,'169.254.3.81',1,1,NULL,NULL),(849,'169.254.3.82',1,1,NULL,NULL),(850,'169.254.3.83',1,1,NULL,NULL),(851,'169.254.3.84',1,1,NULL,NULL),(852,'169.254.3.85',1,1,NULL,NULL),(853,'169.254.3.86',1,1,NULL,NULL),(854,'169.254.3.87',1,1,NULL,NULL),(855,'169.254.3.88',1,1,NULL,NULL),(856,'169.254.3.89',1,1,NULL,NULL),(857,'169.254.3.90',1,1,NULL,NULL),(858,'169.254.3.91',1,1,NULL,NULL),(859,'169.254.3.92',1,1,NULL,NULL),(860,'169.254.3.93',1,1,NULL,NULL),(861,'169.254.3.94',1,1,NULL,NULL),(862,'169.254.3.95',1,1,NULL,NULL),(863,'169.254.3.96',1,1,NULL,NULL),(864,'169.254.3.97',1,1,NULL,NULL),(865,'169.254.3.98',1,1,NULL,NULL),(866,'169.254.3.99',1,1,NULL,NULL),(867,'169.254.3.100',1,1,NULL,NULL),(868,'169.254.3.101',1,1,NULL,NULL),(869,'169.254.3.102',1,1,NULL,NULL),(870,'169.254.3.103',1,1,NULL,NULL),(871,'169.254.3.104',1,1,NULL,NULL),(872,'169.254.3.105',1,1,NULL,NULL),(873,'169.254.3.106',1,1,NULL,NULL),(874,'169.254.3.107',1,1,NULL,NULL),(875,'169.254.3.108',1,1,NULL,NULL),(876,'169.254.3.109',1,1,NULL,NULL),(877,'169.254.3.110',1,1,NULL,NULL),(878,'169.254.3.111',1,1,NULL,NULL),(879,'169.254.3.112',1,1,NULL,NULL),(880,'169.254.3.113',1,1,NULL,NULL),(881,'169.254.3.114',1,1,NULL,NULL),(882,'169.254.3.115',1,1,NULL,NULL),(883,'169.254.3.116',1,1,NULL,NULL),(884,'169.254.3.117',1,1,NULL,NULL),(885,'169.254.3.118',1,1,NULL,NULL),(886,'169.254.3.119',1,1,NULL,NULL),(887,'169.254.3.120',1,1,NULL,NULL),(888,'169.254.3.121',1,1,NULL,NULL),(889,'169.254.3.122',1,1,NULL,NULL),(890,'169.254.3.123',1,1,NULL,NULL),(891,'169.254.3.124',1,1,NULL,NULL),(892,'169.254.3.125',1,1,NULL,NULL),(893,'169.254.3.126',1,1,NULL,NULL),(894,'169.254.3.127',1,1,NULL,NULL),(895,'169.254.3.128',1,1,NULL,NULL),(896,'169.254.3.129',1,1,NULL,NULL),(897,'169.254.3.130',1,1,NULL,NULL),(898,'169.254.3.131',1,1,NULL,NULL),(899,'169.254.3.132',1,1,NULL,NULL),(900,'169.254.3.133',1,1,NULL,NULL),(901,'169.254.3.134',1,1,NULL,NULL),(902,'169.254.3.135',1,1,NULL,NULL),(903,'169.254.3.136',1,1,NULL,NULL),(904,'169.254.3.137',1,1,NULL,NULL),(905,'169.254.3.138',1,1,NULL,NULL),(906,'169.254.3.139',1,1,NULL,NULL),(907,'169.254.3.140',1,1,NULL,NULL),(908,'169.254.3.141',1,1,NULL,NULL),(909,'169.254.3.142',1,1,NULL,NULL),(910,'169.254.3.143',1,1,NULL,NULL),(911,'169.254.3.144',1,1,NULL,NULL),(912,'169.254.3.145',1,1,NULL,NULL),(913,'169.254.3.146',1,1,NULL,NULL),(914,'169.254.3.147',1,1,NULL,NULL),(915,'169.254.3.148',1,1,NULL,NULL),(916,'169.254.3.149',1,1,NULL,NULL),(917,'169.254.3.150',1,1,NULL,NULL),(918,'169.254.3.151',1,1,NULL,NULL),(919,'169.254.3.152',1,1,NULL,NULL),(920,'169.254.3.153',1,1,NULL,NULL),(921,'169.254.3.154',1,1,NULL,NULL),(922,'169.254.3.155',1,1,NULL,NULL),(923,'169.254.3.156',1,1,NULL,NULL),(924,'169.254.3.157',1,1,NULL,NULL),(925,'169.254.3.158',1,1,NULL,NULL),(926,'169.254.3.159',1,1,NULL,NULL),(927,'169.254.3.160',1,1,NULL,NULL),(928,'169.254.3.161',1,1,NULL,NULL),(929,'169.254.3.162',1,1,NULL,NULL),(930,'169.254.3.163',1,1,NULL,NULL),(931,'169.254.3.164',1,1,NULL,NULL),(932,'169.254.3.165',1,1,NULL,NULL),(933,'169.254.3.166',1,1,NULL,NULL),(934,'169.254.3.167',1,1,NULL,NULL),(935,'169.254.3.168',1,1,NULL,NULL),(936,'169.254.3.169',1,1,NULL,NULL),(937,'169.254.3.170',1,1,NULL,NULL),(938,'169.254.3.171',1,1,NULL,NULL),(939,'169.254.3.172',1,1,NULL,NULL),(940,'169.254.3.173',1,1,NULL,NULL),(941,'169.254.3.174',1,1,NULL,NULL),(942,'169.254.3.175',1,1,NULL,NULL),(943,'169.254.3.176',1,1,NULL,NULL),(944,'169.254.3.177',1,1,NULL,NULL),(945,'169.254.3.178',1,1,NULL,NULL),(946,'169.254.3.179',1,1,NULL,NULL),(947,'169.254.3.180',1,1,NULL,NULL),(948,'169.254.3.181',1,1,NULL,NULL),(949,'169.254.3.182',1,1,NULL,NULL),(950,'169.254.3.183',1,1,NULL,NULL),(951,'169.254.3.184',1,1,NULL,NULL),(952,'169.254.3.185',1,1,NULL,NULL),(953,'169.254.3.186',1,1,NULL,NULL),(954,'169.254.3.187',1,1,NULL,NULL),(955,'169.254.3.188',1,1,NULL,NULL),(956,'169.254.3.189',1,1,NULL,NULL),(957,'169.254.3.190',1,1,NULL,NULL),(958,'169.254.3.191',1,1,NULL,NULL),(959,'169.254.3.192',1,1,NULL,NULL),(960,'169.254.3.193',1,1,NULL,NULL),(961,'169.254.3.194',1,1,NULL,NULL),(962,'169.254.3.195',1,1,NULL,NULL),(963,'169.254.3.196',1,1,NULL,NULL),(964,'169.254.3.197',1,1,NULL,NULL),(965,'169.254.3.198',1,1,NULL,NULL),(966,'169.254.3.199',1,1,NULL,NULL),(967,'169.254.3.200',1,1,NULL,NULL),(968,'169.254.3.201',1,1,NULL,NULL),(969,'169.254.3.202',1,1,NULL,NULL),(970,'169.254.3.203',1,1,NULL,NULL),(971,'169.254.3.204',1,1,NULL,NULL),(972,'169.254.3.205',1,1,NULL,NULL),(973,'169.254.3.206',1,1,NULL,NULL),(974,'169.254.3.207',1,1,NULL,NULL),(975,'169.254.3.208',1,1,NULL,NULL),(976,'169.254.3.209',1,1,NULL,NULL),(977,'169.254.3.210',1,1,NULL,NULL),(978,'169.254.3.211',1,1,NULL,NULL),(979,'169.254.3.212',1,1,NULL,NULL),(980,'169.254.3.213',1,1,NULL,NULL),(981,'169.254.3.214',1,1,NULL,NULL),(982,'169.254.3.215',1,1,NULL,NULL),(983,'169.254.3.216',1,1,NULL,NULL),(984,'169.254.3.217',1,1,NULL,NULL),(985,'169.254.3.218',1,1,NULL,NULL),(986,'169.254.3.219',1,1,NULL,NULL),(987,'169.254.3.220',1,1,NULL,NULL),(988,'169.254.3.221',1,1,NULL,NULL),(989,'169.254.3.222',1,1,NULL,NULL),(990,'169.254.3.223',1,1,NULL,NULL),(991,'169.254.3.224',1,1,NULL,NULL),(992,'169.254.3.225',1,1,NULL,NULL),(993,'169.254.3.226',1,1,NULL,NULL),(994,'169.254.3.227',1,1,NULL,NULL),(995,'169.254.3.228',1,1,NULL,NULL),(996,'169.254.3.229',1,1,NULL,NULL),(997,'169.254.3.230',1,1,NULL,NULL),(998,'169.254.3.231',1,1,NULL,NULL),(999,'169.254.3.232',1,1,NULL,NULL),(1000,'169.254.3.233',1,1,NULL,NULL),(1001,'169.254.3.234',1,1,NULL,NULL),(1002,'169.254.3.235',1,1,NULL,NULL),(1003,'169.254.3.236',1,1,NULL,NULL),(1004,'169.254.3.237',1,1,NULL,NULL),(1005,'169.254.3.238',1,1,NULL,NULL),(1006,'169.254.3.239',1,1,NULL,NULL),(1007,'169.254.3.240',1,1,NULL,NULL),(1008,'169.254.3.241',1,1,NULL,NULL),(1009,'169.254.3.242',1,1,NULL,NULL),(1010,'169.254.3.243',1,1,NULL,NULL),(1011,'169.254.3.244',1,1,NULL,NULL),(1012,'169.254.3.245',1,1,NULL,NULL),(1013,'169.254.3.246',1,1,NULL,NULL),(1014,'169.254.3.247',1,1,NULL,NULL),(1015,'169.254.3.248',1,1,NULL,NULL),(1016,'169.254.3.249',1,1,NULL,NULL),(1017,'169.254.3.250',1,1,NULL,NULL),(1018,'169.254.3.251',1,1,NULL,NULL),(1019,'169.254.3.252',1,1,NULL,NULL),(1020,'169.254.3.253',1,1,NULL,NULL),(1021,'169.254.3.254',1,1,NULL,NULL),(1022,'169.254.0.2',2,2,NULL,NULL),(1023,'169.254.0.3',2,2,NULL,NULL),(1024,'169.254.0.4',2,2,NULL,NULL),(1025,'169.254.0.5',2,2,NULL,NULL),(1026,'169.254.0.6',2,2,NULL,NULL),(1027,'169.254.0.7',2,2,NULL,NULL),(1028,'169.254.0.8',2,2,NULL,NULL),(1029,'169.254.0.9',2,2,NULL,NULL),(1030,'169.254.0.10',2,2,NULL,NULL),(1031,'169.254.0.11',2,2,NULL,NULL),(1032,'169.254.0.12',2,2,NULL,NULL),(1033,'169.254.0.13',2,2,NULL,NULL),(1034,'169.254.0.14',2,2,NULL,NULL),(1035,'169.254.0.15',2,2,NULL,NULL),(1036,'169.254.0.16',2,2,NULL,NULL),(1037,'169.254.0.17',2,2,NULL,NULL),(1038,'169.254.0.18',2,2,NULL,NULL),(1039,'169.254.0.19',2,2,NULL,NULL),(1040,'169.254.0.20',2,2,NULL,NULL),(1041,'169.254.0.21',2,2,NULL,NULL),(1042,'169.254.0.22',2,2,NULL,NULL),(1043,'169.254.0.23',2,2,NULL,NULL),(1044,'169.254.0.24',2,2,NULL,NULL),(1045,'169.254.0.25',2,2,NULL,NULL),(1046,'169.254.0.26',2,2,NULL,NULL),(1047,'169.254.0.27',2,2,NULL,NULL),(1048,'169.254.0.28',2,2,NULL,NULL),(1049,'169.254.0.29',2,2,NULL,NULL),(1050,'169.254.0.30',2,2,NULL,NULL),(1051,'169.254.0.31',2,2,NULL,NULL),(1052,'169.254.0.32',2,2,NULL,NULL),(1053,'169.254.0.33',2,2,NULL,NULL),(1054,'169.254.0.34',2,2,NULL,NULL),(1055,'169.254.0.35',2,2,NULL,NULL),(1056,'169.254.0.36',2,2,NULL,NULL),(1057,'169.254.0.37',2,2,NULL,NULL),(1058,'169.254.0.38',2,2,NULL,NULL),(1059,'169.254.0.39',2,2,NULL,NULL),(1060,'169.254.0.40',2,2,NULL,NULL),(1061,'169.254.0.41',2,2,NULL,NULL),(1062,'169.254.0.42',2,2,NULL,NULL),(1063,'169.254.0.43',2,2,NULL,NULL),(1064,'169.254.0.44',2,2,NULL,NULL),(1065,'169.254.0.45',2,2,NULL,NULL),(1066,'169.254.0.46',2,2,NULL,NULL),(1067,'169.254.0.47',2,2,NULL,NULL),(1068,'169.254.0.48',2,2,NULL,NULL),(1069,'169.254.0.49',2,2,NULL,NULL),(1070,'169.254.0.50',2,2,NULL,NULL),(1071,'169.254.0.51',2,2,NULL,NULL),(1072,'169.254.0.52',2,2,NULL,NULL),(1073,'169.254.0.53',2,2,NULL,NULL),(1074,'169.254.0.54',2,2,NULL,NULL),(1075,'169.254.0.55',2,2,NULL,NULL),(1076,'169.254.0.56',2,2,NULL,NULL),(1077,'169.254.0.57',2,2,NULL,NULL),(1078,'169.254.0.58',2,2,NULL,NULL),(1079,'169.254.0.59',2,2,NULL,NULL),(1080,'169.254.0.60',2,2,NULL,NULL),(1081,'169.254.0.61',2,2,NULL,NULL),(1082,'169.254.0.62',2,2,NULL,NULL),(1083,'169.254.0.63',2,2,NULL,NULL),(1084,'169.254.0.64',2,2,NULL,NULL),(1085,'169.254.0.65',2,2,NULL,NULL),(1086,'169.254.0.66',2,2,NULL,NULL),(1087,'169.254.0.67',2,2,NULL,NULL),(1088,'169.254.0.68',2,2,NULL,NULL),(1089,'169.254.0.69',2,2,NULL,NULL),(1090,'169.254.0.70',2,2,NULL,NULL),(1091,'169.254.0.71',2,2,NULL,NULL),(1092,'169.254.0.72',2,2,NULL,NULL),(1093,'169.254.0.73',2,2,NULL,NULL),(1094,'169.254.0.74',2,2,NULL,NULL),(1095,'169.254.0.75',2,2,NULL,NULL),(1096,'169.254.0.76',2,2,NULL,NULL),(1097,'169.254.0.77',2,2,NULL,NULL),(1098,'169.254.0.78',2,2,NULL,NULL),(1099,'169.254.0.79',2,2,NULL,NULL),(1100,'169.254.0.80',2,2,NULL,NULL),(1101,'169.254.0.81',2,2,NULL,NULL),(1102,'169.254.0.82',2,2,NULL,NULL),(1103,'169.254.0.83',2,2,NULL,NULL),(1104,'169.254.0.84',2,2,NULL,NULL),(1105,'169.254.0.85',2,2,NULL,NULL),(1106,'169.254.0.86',2,2,NULL,NULL),(1107,'169.254.0.87',2,2,NULL,NULL),(1108,'169.254.0.88',2,2,NULL,NULL),(1109,'169.254.0.89',2,2,NULL,NULL),(1110,'169.254.0.90',2,2,NULL,NULL),(1111,'169.254.0.91',2,2,NULL,NULL),(1112,'169.254.0.92',2,2,NULL,NULL),(1113,'169.254.0.93',2,2,NULL,NULL),(1114,'169.254.0.94',2,2,NULL,NULL),(1115,'169.254.0.95',2,2,NULL,NULL),(1116,'169.254.0.96',2,2,NULL,NULL),(1117,'169.254.0.97',2,2,NULL,NULL),(1118,'169.254.0.98',2,2,NULL,NULL),(1119,'169.254.0.99',2,2,NULL,NULL),(1120,'169.254.0.100',2,2,NULL,NULL),(1121,'169.254.0.101',2,2,NULL,NULL),(1122,'169.254.0.102',2,2,NULL,NULL),(1123,'169.254.0.103',2,2,NULL,NULL),(1124,'169.254.0.104',2,2,NULL,NULL),(1125,'169.254.0.105',2,2,NULL,NULL),(1126,'169.254.0.106',2,2,NULL,NULL),(1127,'169.254.0.107',2,2,NULL,NULL),(1128,'169.254.0.108',2,2,NULL,NULL),(1129,'169.254.0.109',2,2,NULL,NULL),(1130,'169.254.0.110',2,2,NULL,NULL),(1131,'169.254.0.111',2,2,NULL,NULL),(1132,'169.254.0.112',2,2,NULL,NULL),(1133,'169.254.0.113',2,2,NULL,NULL),(1134,'169.254.0.114',2,2,NULL,NULL),(1135,'169.254.0.115',2,2,NULL,NULL),(1136,'169.254.0.116',2,2,NULL,NULL),(1137,'169.254.0.117',2,2,NULL,NULL),(1138,'169.254.0.118',2,2,NULL,NULL),(1139,'169.254.0.119',2,2,NULL,NULL),(1140,'169.254.0.120',2,2,NULL,NULL),(1141,'169.254.0.121',2,2,NULL,NULL),(1142,'169.254.0.122',2,2,NULL,NULL),(1143,'169.254.0.123',2,2,NULL,NULL),(1144,'169.254.0.124',2,2,NULL,NULL),(1145,'169.254.0.125',2,2,NULL,NULL),(1146,'169.254.0.126',2,2,NULL,NULL),(1147,'169.254.0.127',2,2,NULL,NULL),(1148,'169.254.0.128',2,2,NULL,NULL),(1149,'169.254.0.129',2,2,NULL,NULL),(1150,'169.254.0.130',2,2,NULL,NULL),(1151,'169.254.0.131',2,2,NULL,NULL),(1152,'169.254.0.132',2,2,NULL,NULL),(1153,'169.254.0.133',2,2,NULL,NULL),(1154,'169.254.0.134',2,2,NULL,NULL),(1155,'169.254.0.135',2,2,NULL,NULL),(1156,'169.254.0.136',2,2,NULL,NULL),(1157,'169.254.0.137',2,2,NULL,NULL),(1158,'169.254.0.138',2,2,NULL,NULL),(1159,'169.254.0.139',2,2,NULL,NULL),(1160,'169.254.0.140',2,2,NULL,NULL),(1161,'169.254.0.141',2,2,NULL,NULL),(1162,'169.254.0.142',2,2,NULL,NULL),(1163,'169.254.0.143',2,2,NULL,NULL),(1164,'169.254.0.144',2,2,NULL,NULL),(1165,'169.254.0.145',2,2,NULL,NULL),(1166,'169.254.0.146',2,2,NULL,NULL),(1167,'169.254.0.147',2,2,NULL,NULL),(1168,'169.254.0.148',2,2,NULL,NULL),(1169,'169.254.0.149',2,2,NULL,NULL),(1170,'169.254.0.150',2,2,NULL,NULL),(1171,'169.254.0.151',2,2,NULL,NULL),(1172,'169.254.0.152',2,2,NULL,NULL),(1173,'169.254.0.153',2,2,NULL,NULL),(1174,'169.254.0.154',2,2,NULL,NULL),(1175,'169.254.0.155',2,2,NULL,NULL),(1176,'169.254.0.156',2,2,NULL,NULL),(1177,'169.254.0.157',2,2,NULL,NULL),(1178,'169.254.0.158',2,2,NULL,NULL),(1179,'169.254.0.159',2,2,NULL,NULL),(1180,'169.254.0.160',2,2,NULL,NULL),(1181,'169.254.0.161',2,2,NULL,NULL),(1182,'169.254.0.162',2,2,NULL,NULL),(1183,'169.254.0.163',2,2,NULL,NULL),(1184,'169.254.0.164',2,2,NULL,NULL),(1185,'169.254.0.165',2,2,NULL,NULL),(1186,'169.254.0.166',2,2,NULL,NULL),(1187,'169.254.0.167',2,2,NULL,NULL),(1188,'169.254.0.168',2,2,NULL,NULL),(1189,'169.254.0.169',2,2,NULL,NULL),(1190,'169.254.0.170',2,2,NULL,NULL),(1191,'169.254.0.171',2,2,NULL,NULL),(1192,'169.254.0.172',2,2,NULL,NULL),(1193,'169.254.0.173',2,2,NULL,NULL),(1194,'169.254.0.174',2,2,NULL,NULL),(1195,'169.254.0.175',2,2,NULL,NULL),(1196,'169.254.0.176',2,2,NULL,NULL),(1197,'169.254.0.177',2,2,NULL,NULL),(1198,'169.254.0.178',2,2,NULL,NULL),(1199,'169.254.0.179',2,2,NULL,NULL),(1200,'169.254.0.180',2,2,NULL,NULL),(1201,'169.254.0.181',2,2,NULL,NULL),(1202,'169.254.0.182',2,2,NULL,NULL),(1203,'169.254.0.183',2,2,NULL,NULL),(1204,'169.254.0.184',2,2,NULL,NULL),(1205,'169.254.0.185',2,2,NULL,NULL),(1206,'169.254.0.186',2,2,NULL,NULL),(1207,'169.254.0.187',2,2,NULL,NULL),(1208,'169.254.0.188',2,2,NULL,NULL),(1209,'169.254.0.189',2,2,NULL,NULL),(1210,'169.254.0.190',2,2,NULL,NULL),(1211,'169.254.0.191',2,2,NULL,NULL),(1212,'169.254.0.192',2,2,NULL,NULL),(1213,'169.254.0.193',2,2,NULL,NULL),(1214,'169.254.0.194',2,2,NULL,NULL),(1215,'169.254.0.195',2,2,NULL,NULL),(1216,'169.254.0.196',2,2,NULL,NULL),(1217,'169.254.0.197',2,2,NULL,NULL),(1218,'169.254.0.198',2,2,NULL,NULL),(1219,'169.254.0.199',2,2,NULL,NULL),(1220,'169.254.0.200',2,2,NULL,NULL),(1221,'169.254.0.201',2,2,NULL,NULL),(1222,'169.254.0.202',2,2,NULL,NULL),(1223,'169.254.0.203',2,2,NULL,NULL),(1224,'169.254.0.204',2,2,NULL,NULL),(1225,'169.254.0.205',2,2,NULL,NULL),(1226,'169.254.0.206',2,2,NULL,NULL),(1227,'169.254.0.207',2,2,NULL,NULL),(1228,'169.254.0.208',2,2,NULL,NULL),(1229,'169.254.0.209',2,2,NULL,NULL),(1230,'169.254.0.210',2,2,NULL,NULL),(1231,'169.254.0.211',2,2,NULL,NULL),(1232,'169.254.0.212',2,2,NULL,NULL),(1233,'169.254.0.213',2,2,NULL,NULL),(1234,'169.254.0.214',2,2,NULL,NULL),(1235,'169.254.0.215',2,2,NULL,NULL),(1236,'169.254.0.216',2,2,NULL,NULL),(1237,'169.254.0.217',2,2,NULL,NULL),(1238,'169.254.0.218',2,2,NULL,NULL),(1239,'169.254.0.219',2,2,NULL,NULL),(1240,'169.254.0.220',2,2,NULL,NULL),(1241,'169.254.0.221',2,2,NULL,NULL),(1242,'169.254.0.222',2,2,NULL,NULL),(1243,'169.254.0.223',2,2,NULL,NULL),(1244,'169.254.0.224',2,2,NULL,NULL),(1245,'169.254.0.225',2,2,NULL,NULL),(1246,'169.254.0.226',2,2,NULL,NULL),(1247,'169.254.0.227',2,2,NULL,NULL),(1248,'169.254.0.228',2,2,NULL,NULL),(1249,'169.254.0.229',2,2,NULL,NULL),(1250,'169.254.0.230',2,2,NULL,NULL),(1251,'169.254.0.231',2,2,NULL,NULL),(1252,'169.254.0.232',2,2,NULL,NULL),(1253,'169.254.0.233',2,2,NULL,NULL),(1254,'169.254.0.234',2,2,NULL,NULL),(1255,'169.254.0.235',2,2,NULL,NULL),(1256,'169.254.0.236',2,2,NULL,NULL),(1257,'169.254.0.237',2,2,NULL,NULL),(1258,'169.254.0.238',2,2,NULL,NULL),(1259,'169.254.0.239',2,2,NULL,NULL),(1260,'169.254.0.240',2,2,NULL,NULL),(1261,'169.254.0.241',2,2,NULL,NULL),(1262,'169.254.0.242',2,2,NULL,NULL),(1263,'169.254.0.243',2,2,NULL,NULL),(1264,'169.254.0.244',2,2,NULL,NULL),(1265,'169.254.0.245',2,2,NULL,NULL),(1266,'169.254.0.246',2,2,NULL,NULL),(1267,'169.254.0.247',2,2,NULL,NULL),(1268,'169.254.0.248',2,2,NULL,NULL),(1269,'169.254.0.249',2,2,NULL,NULL),(1270,'169.254.0.250',2,2,NULL,NULL),(1271,'169.254.0.251',2,2,NULL,NULL),(1272,'169.254.0.252',2,2,NULL,NULL),(1273,'169.254.0.253',2,2,NULL,NULL),(1274,'169.254.0.254',2,2,NULL,NULL),(1275,'169.254.0.255',2,2,NULL,NULL),(1276,'169.254.1.0',2,2,NULL,NULL),(1277,'169.254.1.1',2,2,NULL,NULL),(1278,'169.254.1.2',2,2,NULL,NULL),(1279,'169.254.1.3',2,2,NULL,NULL),(1280,'169.254.1.4',2,2,NULL,NULL),(1281,'169.254.1.5',2,2,NULL,NULL),(1282,'169.254.1.6',2,2,NULL,NULL),(1283,'169.254.1.7',2,2,NULL,NULL),(1284,'169.254.1.8',2,2,NULL,NULL),(1285,'169.254.1.9',2,2,NULL,NULL),(1286,'169.254.1.10',2,2,NULL,NULL),(1287,'169.254.1.11',2,2,NULL,NULL),(1288,'169.254.1.12',2,2,NULL,NULL),(1289,'169.254.1.13',2,2,NULL,NULL),(1290,'169.254.1.14',2,2,NULL,NULL),(1291,'169.254.1.15',2,2,NULL,NULL),(1292,'169.254.1.16',2,2,NULL,NULL),(1293,'169.254.1.17',2,2,NULL,NULL),(1294,'169.254.1.18',2,2,NULL,NULL),(1295,'169.254.1.19',2,2,NULL,NULL),(1296,'169.254.1.20',2,2,NULL,NULL),(1297,'169.254.1.21',2,2,NULL,NULL),(1298,'169.254.1.22',2,2,NULL,NULL),(1299,'169.254.1.23',2,2,NULL,NULL),(1300,'169.254.1.24',2,2,NULL,NULL),(1301,'169.254.1.25',2,2,NULL,NULL),(1302,'169.254.1.26',2,2,NULL,NULL),(1303,'169.254.1.27',2,2,NULL,NULL),(1304,'169.254.1.28',2,2,NULL,NULL),(1305,'169.254.1.29',2,2,NULL,NULL),(1306,'169.254.1.30',2,2,NULL,NULL),(1307,'169.254.1.31',2,2,NULL,NULL),(1308,'169.254.1.32',2,2,NULL,NULL),(1309,'169.254.1.33',2,2,NULL,NULL),(1310,'169.254.1.34',2,2,NULL,NULL),(1311,'169.254.1.35',2,2,NULL,NULL),(1312,'169.254.1.36',2,2,NULL,NULL),(1313,'169.254.1.37',2,2,NULL,NULL),(1314,'169.254.1.38',2,2,NULL,NULL),(1315,'169.254.1.39',2,2,NULL,NULL),(1316,'169.254.1.40',2,2,NULL,NULL),(1317,'169.254.1.41',2,2,NULL,NULL),(1318,'169.254.1.42',2,2,NULL,NULL),(1319,'169.254.1.43',2,2,NULL,NULL),(1320,'169.254.1.44',2,2,NULL,NULL),(1321,'169.254.1.45',2,2,NULL,NULL),(1322,'169.254.1.46',2,2,NULL,NULL),(1323,'169.254.1.47',2,2,NULL,NULL),(1324,'169.254.1.48',2,2,NULL,NULL),(1325,'169.254.1.49',2,2,NULL,NULL),(1326,'169.254.1.50',2,2,NULL,NULL),(1327,'169.254.1.51',2,2,NULL,NULL),(1328,'169.254.1.52',2,2,NULL,NULL),(1329,'169.254.1.53',2,2,NULL,NULL),(1330,'169.254.1.54',2,2,NULL,NULL),(1331,'169.254.1.55',2,2,NULL,NULL),(1332,'169.254.1.56',2,2,NULL,NULL),(1333,'169.254.1.57',2,2,NULL,NULL),(1334,'169.254.1.58',2,2,NULL,NULL),(1335,'169.254.1.59',2,2,NULL,NULL),(1336,'169.254.1.60',2,2,NULL,NULL),(1337,'169.254.1.61',2,2,NULL,NULL),(1338,'169.254.1.62',2,2,NULL,NULL),(1339,'169.254.1.63',2,2,NULL,NULL),(1340,'169.254.1.64',2,2,NULL,NULL),(1341,'169.254.1.65',2,2,NULL,NULL),(1342,'169.254.1.66',2,2,NULL,NULL),(1343,'169.254.1.67',2,2,NULL,NULL),(1344,'169.254.1.68',2,2,NULL,NULL),(1345,'169.254.1.69',2,2,NULL,NULL),(1346,'169.254.1.70',2,2,NULL,NULL),(1347,'169.254.1.71',2,2,NULL,NULL),(1348,'169.254.1.72',2,2,NULL,NULL),(1349,'169.254.1.73',2,2,NULL,NULL),(1350,'169.254.1.74',2,2,NULL,NULL),(1351,'169.254.1.75',2,2,NULL,NULL),(1352,'169.254.1.76',2,2,NULL,NULL),(1353,'169.254.1.77',2,2,NULL,NULL),(1354,'169.254.1.78',2,2,NULL,NULL),(1355,'169.254.1.79',2,2,NULL,NULL),(1356,'169.254.1.80',2,2,NULL,NULL),(1357,'169.254.1.81',2,2,NULL,NULL),(1358,'169.254.1.82',2,2,NULL,NULL),(1359,'169.254.1.83',2,2,NULL,NULL),(1360,'169.254.1.84',2,2,NULL,NULL),(1361,'169.254.1.85',2,2,NULL,NULL),(1362,'169.254.1.86',2,2,NULL,NULL),(1363,'169.254.1.87',2,2,NULL,NULL),(1364,'169.254.1.88',2,2,NULL,NULL),(1365,'169.254.1.89',2,2,NULL,NULL),(1366,'169.254.1.90',2,2,NULL,NULL),(1367,'169.254.1.91',2,2,NULL,NULL),(1368,'169.254.1.92',2,2,NULL,NULL),(1369,'169.254.1.93',2,2,NULL,NULL),(1370,'169.254.1.94',2,2,NULL,NULL),(1371,'169.254.1.95',2,2,NULL,NULL),(1372,'169.254.1.96',2,2,NULL,NULL),(1373,'169.254.1.97',2,2,NULL,NULL),(1374,'169.254.1.98',2,2,NULL,NULL),(1375,'169.254.1.99',2,2,NULL,NULL),(1376,'169.254.1.100',2,2,NULL,NULL),(1377,'169.254.1.101',2,2,NULL,NULL),(1378,'169.254.1.102',2,2,NULL,NULL),(1379,'169.254.1.103',2,2,NULL,NULL),(1380,'169.254.1.104',2,2,NULL,NULL),(1381,'169.254.1.105',2,2,NULL,NULL),(1382,'169.254.1.106',2,2,NULL,NULL),(1383,'169.254.1.107',2,2,NULL,NULL),(1384,'169.254.1.108',2,2,NULL,NULL),(1385,'169.254.1.109',2,2,NULL,NULL),(1386,'169.254.1.110',2,2,NULL,NULL),(1387,'169.254.1.111',2,2,NULL,NULL),(1388,'169.254.1.112',2,2,NULL,NULL),(1389,'169.254.1.113',2,2,NULL,NULL),(1390,'169.254.1.114',2,2,NULL,NULL),(1391,'169.254.1.115',2,2,NULL,NULL),(1392,'169.254.1.116',2,2,NULL,NULL),(1393,'169.254.1.117',2,2,NULL,NULL),(1394,'169.254.1.118',2,2,NULL,NULL),(1395,'169.254.1.119',2,2,NULL,NULL),(1396,'169.254.1.120',2,2,NULL,NULL),(1397,'169.254.1.121',2,2,NULL,NULL),(1398,'169.254.1.122',2,2,NULL,NULL),(1399,'169.254.1.123',2,2,NULL,NULL),(1400,'169.254.1.124',2,2,NULL,NULL),(1401,'169.254.1.125',2,2,NULL,NULL),(1402,'169.254.1.126',2,2,NULL,NULL),(1403,'169.254.1.127',2,2,NULL,NULL),(1404,'169.254.1.128',2,2,NULL,NULL),(1405,'169.254.1.129',2,2,NULL,NULL),(1406,'169.254.1.130',2,2,NULL,NULL),(1407,'169.254.1.131',2,2,NULL,NULL),(1408,'169.254.1.132',2,2,NULL,NULL),(1409,'169.254.1.133',2,2,NULL,NULL),(1410,'169.254.1.134',2,2,NULL,NULL),(1411,'169.254.1.135',2,2,NULL,NULL),(1412,'169.254.1.136',2,2,NULL,NULL),(1413,'169.254.1.137',2,2,NULL,NULL),(1414,'169.254.1.138',2,2,NULL,NULL),(1415,'169.254.1.139',2,2,NULL,NULL),(1416,'169.254.1.140',2,2,NULL,NULL),(1417,'169.254.1.141',2,2,NULL,NULL),(1418,'169.254.1.142',2,2,NULL,NULL),(1419,'169.254.1.143',2,2,NULL,NULL),(1420,'169.254.1.144',2,2,NULL,NULL),(1421,'169.254.1.145',2,2,NULL,NULL),(1422,'169.254.1.146',2,2,NULL,NULL),(1423,'169.254.1.147',2,2,NULL,NULL),(1424,'169.254.1.148',2,2,NULL,NULL),(1425,'169.254.1.149',2,2,NULL,NULL),(1426,'169.254.1.150',2,2,NULL,NULL),(1427,'169.254.1.151',2,2,NULL,NULL),(1428,'169.254.1.152',2,2,NULL,NULL),(1429,'169.254.1.153',2,2,NULL,NULL),(1430,'169.254.1.154',2,2,NULL,NULL),(1431,'169.254.1.155',2,2,NULL,NULL),(1432,'169.254.1.156',2,2,NULL,NULL),(1433,'169.254.1.157',2,2,NULL,NULL),(1434,'169.254.1.158',2,2,NULL,NULL),(1435,'169.254.1.159',2,2,NULL,NULL),(1436,'169.254.1.160',2,2,NULL,NULL),(1437,'169.254.1.161',2,2,NULL,NULL),(1438,'169.254.1.162',2,2,NULL,NULL),(1439,'169.254.1.163',2,2,NULL,NULL),(1440,'169.254.1.164',2,2,NULL,NULL),(1441,'169.254.1.165',2,2,NULL,NULL),(1442,'169.254.1.166',2,2,NULL,NULL),(1443,'169.254.1.167',2,2,NULL,NULL),(1444,'169.254.1.168',2,2,NULL,NULL),(1445,'169.254.1.169',2,2,NULL,NULL),(1446,'169.254.1.170',2,2,NULL,NULL),(1447,'169.254.1.171',2,2,NULL,NULL),(1448,'169.254.1.172',2,2,NULL,NULL),(1449,'169.254.1.173',2,2,NULL,NULL),(1450,'169.254.1.174',2,2,NULL,NULL),(1451,'169.254.1.175',2,2,NULL,NULL),(1452,'169.254.1.176',2,2,NULL,NULL),(1453,'169.254.1.177',2,2,NULL,NULL),(1454,'169.254.1.178',2,2,NULL,NULL),(1455,'169.254.1.179',2,2,NULL,NULL),(1456,'169.254.1.180',2,2,NULL,NULL),(1457,'169.254.1.181',2,2,NULL,NULL),(1458,'169.254.1.182',2,2,NULL,NULL),(1459,'169.254.1.183',2,2,NULL,NULL),(1460,'169.254.1.184',2,2,NULL,NULL),(1461,'169.254.1.185',2,2,NULL,NULL),(1462,'169.254.1.186',2,2,NULL,NULL),(1463,'169.254.1.187',2,2,NULL,NULL),(1464,'169.254.1.188',2,2,NULL,NULL),(1465,'169.254.1.189',2,2,NULL,NULL),(1466,'169.254.1.190',2,2,NULL,NULL),(1467,'169.254.1.191',2,2,NULL,NULL),(1468,'169.254.1.192',2,2,NULL,NULL),(1469,'169.254.1.193',2,2,NULL,NULL),(1470,'169.254.1.194',2,2,NULL,NULL),(1471,'169.254.1.195',2,2,NULL,NULL),(1472,'169.254.1.196',2,2,NULL,NULL),(1473,'169.254.1.197',2,2,NULL,NULL),(1474,'169.254.1.198',2,2,NULL,NULL),(1475,'169.254.1.199',2,2,NULL,NULL),(1476,'169.254.1.200',2,2,NULL,NULL),(1477,'169.254.1.201',2,2,NULL,NULL),(1478,'169.254.1.202',2,2,NULL,NULL),(1479,'169.254.1.203',2,2,NULL,NULL),(1480,'169.254.1.204',2,2,NULL,NULL),(1481,'169.254.1.205',2,2,NULL,NULL),(1482,'169.254.1.206',2,2,NULL,NULL),(1483,'169.254.1.207',2,2,NULL,NULL),(1484,'169.254.1.208',2,2,NULL,NULL),(1485,'169.254.1.209',2,2,NULL,NULL),(1486,'169.254.1.210',2,2,NULL,NULL),(1487,'169.254.1.211',2,2,NULL,NULL),(1488,'169.254.1.212',2,2,NULL,NULL),(1489,'169.254.1.213',2,2,NULL,NULL),(1490,'169.254.1.214',2,2,NULL,NULL),(1491,'169.254.1.215',2,2,NULL,NULL),(1492,'169.254.1.216',2,2,NULL,NULL),(1493,'169.254.1.217',2,2,NULL,NULL),(1494,'169.254.1.218',2,2,NULL,NULL),(1495,'169.254.1.219',2,2,NULL,NULL),(1496,'169.254.1.220',2,2,NULL,NULL),(1497,'169.254.1.221',2,2,NULL,NULL),(1498,'169.254.1.222',2,2,NULL,NULL),(1499,'169.254.1.223',2,2,NULL,NULL),(1500,'169.254.1.224',2,2,NULL,NULL),(1501,'169.254.1.225',2,2,NULL,NULL),(1502,'169.254.1.226',2,2,NULL,NULL),(1503,'169.254.1.227',2,2,NULL,NULL),(1504,'169.254.1.228',2,2,NULL,NULL),(1505,'169.254.1.229',2,2,NULL,NULL),(1506,'169.254.1.230',2,2,NULL,NULL),(1507,'169.254.1.231',2,2,NULL,NULL),(1508,'169.254.1.232',2,2,NULL,NULL),(1509,'169.254.1.233',2,2,NULL,NULL),(1510,'169.254.1.234',2,2,NULL,NULL),(1511,'169.254.1.235',2,2,NULL,NULL),(1512,'169.254.1.236',2,2,NULL,NULL),(1513,'169.254.1.237',2,2,NULL,NULL),(1514,'169.254.1.238',2,2,NULL,NULL),(1515,'169.254.1.239',2,2,NULL,NULL),(1516,'169.254.1.240',2,2,NULL,NULL),(1517,'169.254.1.241',2,2,NULL,NULL),(1518,'169.254.1.242',2,2,NULL,NULL),(1519,'169.254.1.243',2,2,NULL,NULL),(1520,'169.254.1.244',2,2,NULL,NULL),(1521,'169.254.1.245',2,2,NULL,NULL),(1522,'169.254.1.246',2,2,NULL,NULL),(1523,'169.254.1.247',2,2,NULL,NULL),(1524,'169.254.1.248',2,2,NULL,NULL),(1525,'169.254.1.249',2,2,NULL,NULL),(1526,'169.254.1.250',2,2,NULL,NULL),(1527,'169.254.1.251',2,2,NULL,NULL),(1528,'169.254.1.252',2,2,NULL,NULL),(1529,'169.254.1.253',2,2,NULL,NULL),(1530,'169.254.1.254',2,2,NULL,NULL),(1531,'169.254.1.255',2,2,NULL,NULL),(1532,'169.254.2.0',2,2,NULL,NULL),(1533,'169.254.2.1',2,2,NULL,NULL),(1534,'169.254.2.2',2,2,NULL,NULL),(1535,'169.254.2.3',2,2,NULL,NULL),(1536,'169.254.2.4',2,2,NULL,NULL),(1537,'169.254.2.5',2,2,NULL,NULL),(1538,'169.254.2.6',2,2,NULL,NULL),(1539,'169.254.2.7',2,2,NULL,NULL),(1540,'169.254.2.8',2,2,NULL,NULL),(1541,'169.254.2.9',2,2,NULL,NULL),(1542,'169.254.2.10',2,2,NULL,NULL),(1543,'169.254.2.11',2,2,NULL,NULL),(1544,'169.254.2.12',2,2,NULL,NULL),(1545,'169.254.2.13',2,2,NULL,NULL),(1546,'169.254.2.14',2,2,NULL,NULL),(1547,'169.254.2.15',2,2,NULL,NULL),(1548,'169.254.2.16',2,2,NULL,NULL),(1549,'169.254.2.17',2,2,NULL,NULL),(1550,'169.254.2.18',2,2,NULL,NULL),(1551,'169.254.2.19',2,2,NULL,NULL),(1552,'169.254.2.20',2,2,NULL,NULL),(1553,'169.254.2.21',2,2,NULL,NULL),(1554,'169.254.2.22',2,2,NULL,NULL),(1555,'169.254.2.23',2,2,NULL,NULL),(1556,'169.254.2.24',2,2,NULL,NULL),(1557,'169.254.2.25',2,2,NULL,NULL),(1558,'169.254.2.26',2,2,NULL,NULL),(1559,'169.254.2.27',2,2,NULL,NULL),(1560,'169.254.2.28',2,2,NULL,NULL),(1561,'169.254.2.29',2,2,NULL,NULL),(1562,'169.254.2.30',2,2,NULL,NULL),(1563,'169.254.2.31',2,2,NULL,NULL),(1564,'169.254.2.32',2,2,NULL,NULL),(1565,'169.254.2.33',2,2,NULL,NULL),(1566,'169.254.2.34',2,2,NULL,NULL),(1567,'169.254.2.35',2,2,NULL,NULL),(1568,'169.254.2.36',2,2,NULL,NULL),(1569,'169.254.2.37',2,2,NULL,NULL),(1570,'169.254.2.38',2,2,NULL,NULL),(1571,'169.254.2.39',2,2,NULL,NULL),(1572,'169.254.2.40',2,2,NULL,NULL),(1573,'169.254.2.41',2,2,NULL,NULL),(1574,'169.254.2.42',2,2,NULL,NULL),(1575,'169.254.2.43',2,2,NULL,NULL),(1576,'169.254.2.44',2,2,NULL,NULL),(1577,'169.254.2.45',2,2,NULL,NULL),(1578,'169.254.2.46',2,2,NULL,NULL),(1579,'169.254.2.47',2,2,NULL,NULL),(1580,'169.254.2.48',2,2,NULL,NULL),(1581,'169.254.2.49',2,2,NULL,NULL),(1582,'169.254.2.50',2,2,NULL,NULL),(1583,'169.254.2.51',2,2,NULL,NULL),(1584,'169.254.2.52',2,2,NULL,NULL),(1585,'169.254.2.53',2,2,NULL,NULL),(1586,'169.254.2.54',2,2,NULL,NULL),(1587,'169.254.2.55',2,2,NULL,NULL),(1588,'169.254.2.56',2,2,NULL,NULL),(1589,'169.254.2.57',2,2,NULL,NULL),(1590,'169.254.2.58',2,2,NULL,NULL),(1591,'169.254.2.59',2,2,NULL,NULL),(1592,'169.254.2.60',2,2,NULL,NULL),(1593,'169.254.2.61',2,2,NULL,NULL),(1594,'169.254.2.62',2,2,NULL,NULL),(1595,'169.254.2.63',2,2,NULL,NULL),(1596,'169.254.2.64',2,2,NULL,NULL),(1597,'169.254.2.65',2,2,NULL,NULL),(1598,'169.254.2.66',2,2,NULL,NULL),(1599,'169.254.2.67',2,2,NULL,NULL),(1600,'169.254.2.68',2,2,NULL,NULL),(1601,'169.254.2.69',2,2,NULL,NULL),(1602,'169.254.2.70',2,2,NULL,NULL),(1603,'169.254.2.71',2,2,NULL,NULL),(1604,'169.254.2.72',2,2,NULL,NULL),(1605,'169.254.2.73',2,2,NULL,NULL),(1606,'169.254.2.74',2,2,NULL,NULL),(1607,'169.254.2.75',2,2,NULL,NULL),(1608,'169.254.2.76',2,2,NULL,NULL),(1609,'169.254.2.77',2,2,NULL,NULL),(1610,'169.254.2.78',2,2,NULL,NULL),(1611,'169.254.2.79',2,2,NULL,NULL),(1612,'169.254.2.80',2,2,NULL,NULL),(1613,'169.254.2.81',2,2,NULL,NULL),(1614,'169.254.2.82',2,2,NULL,NULL),(1615,'169.254.2.83',2,2,NULL,NULL),(1616,'169.254.2.84',2,2,NULL,NULL),(1617,'169.254.2.85',2,2,NULL,NULL),(1618,'169.254.2.86',2,2,NULL,NULL),(1619,'169.254.2.87',2,2,NULL,NULL),(1620,'169.254.2.88',2,2,NULL,NULL),(1621,'169.254.2.89',2,2,NULL,NULL),(1622,'169.254.2.90',2,2,NULL,NULL),(1623,'169.254.2.91',2,2,NULL,NULL),(1624,'169.254.2.92',2,2,NULL,NULL),(1625,'169.254.2.93',2,2,NULL,NULL),(1626,'169.254.2.94',2,2,NULL,NULL),(1627,'169.254.2.95',2,2,NULL,NULL),(1628,'169.254.2.96',2,2,NULL,NULL),(1629,'169.254.2.97',2,2,NULL,NULL),(1630,'169.254.2.98',2,2,NULL,NULL),(1631,'169.254.2.99',2,2,NULL,NULL),(1632,'169.254.2.100',2,2,NULL,NULL),(1633,'169.254.2.101',2,2,NULL,NULL),(1634,'169.254.2.102',2,2,NULL,NULL),(1635,'169.254.2.103',2,2,NULL,NULL),(1636,'169.254.2.104',2,2,NULL,NULL),(1637,'169.254.2.105',2,2,NULL,NULL),(1638,'169.254.2.106',2,2,NULL,NULL),(1639,'169.254.2.107',2,2,NULL,NULL),(1640,'169.254.2.108',2,2,NULL,NULL),(1641,'169.254.2.109',2,2,NULL,NULL),(1642,'169.254.2.110',2,2,NULL,NULL),(1643,'169.254.2.111',2,2,NULL,NULL),(1644,'169.254.2.112',2,2,NULL,NULL),(1645,'169.254.2.113',2,2,NULL,NULL),(1646,'169.254.2.114',2,2,NULL,NULL),(1647,'169.254.2.115',2,2,NULL,NULL),(1648,'169.254.2.116',2,2,NULL,NULL),(1649,'169.254.2.117',2,2,NULL,NULL),(1650,'169.254.2.118',2,2,NULL,NULL),(1651,'169.254.2.119',2,2,NULL,NULL),(1652,'169.254.2.120',2,2,NULL,NULL),(1653,'169.254.2.121',2,2,NULL,NULL),(1654,'169.254.2.122',2,2,NULL,NULL),(1655,'169.254.2.123',2,2,NULL,NULL),(1656,'169.254.2.124',2,2,NULL,NULL),(1657,'169.254.2.125',2,2,NULL,NULL),(1658,'169.254.2.126',2,2,NULL,NULL),(1659,'169.254.2.127',2,2,NULL,NULL),(1660,'169.254.2.128',2,2,NULL,NULL),(1661,'169.254.2.129',2,2,NULL,NULL),(1662,'169.254.2.130',2,2,NULL,NULL),(1663,'169.254.2.131',2,2,NULL,NULL),(1664,'169.254.2.132',2,2,NULL,NULL),(1665,'169.254.2.133',2,2,NULL,NULL),(1666,'169.254.2.134',2,2,NULL,NULL),(1667,'169.254.2.135',2,2,NULL,NULL),(1668,'169.254.2.136',2,2,NULL,NULL),(1669,'169.254.2.137',2,2,NULL,NULL),(1670,'169.254.2.138',2,2,NULL,NULL),(1671,'169.254.2.139',2,2,NULL,NULL),(1672,'169.254.2.140',2,2,NULL,NULL),(1673,'169.254.2.141',2,2,NULL,NULL),(1674,'169.254.2.142',2,2,NULL,NULL),(1675,'169.254.2.143',2,2,NULL,NULL),(1676,'169.254.2.144',2,2,NULL,NULL),(1677,'169.254.2.145',2,2,NULL,NULL),(1678,'169.254.2.146',2,2,NULL,NULL),(1679,'169.254.2.147',2,2,NULL,NULL),(1680,'169.254.2.148',2,2,NULL,NULL),(1681,'169.254.2.149',2,2,NULL,NULL),(1682,'169.254.2.150',2,2,NULL,NULL),(1683,'169.254.2.151',2,2,NULL,NULL),(1684,'169.254.2.152',2,2,NULL,NULL),(1685,'169.254.2.153',2,2,NULL,NULL),(1686,'169.254.2.154',2,2,NULL,NULL),(1687,'169.254.2.155',2,2,NULL,NULL),(1688,'169.254.2.156',2,2,NULL,NULL),(1689,'169.254.2.157',2,2,NULL,NULL),(1690,'169.254.2.158',2,2,NULL,NULL),(1691,'169.254.2.159',2,2,NULL,NULL),(1692,'169.254.2.160',2,2,NULL,NULL),(1693,'169.254.2.161',2,2,NULL,NULL),(1694,'169.254.2.162',2,2,NULL,NULL),(1695,'169.254.2.163',2,2,NULL,NULL),(1696,'169.254.2.164',2,2,NULL,NULL),(1697,'169.254.2.165',2,2,NULL,NULL),(1698,'169.254.2.166',2,2,NULL,NULL),(1699,'169.254.2.167',2,2,NULL,NULL),(1700,'169.254.2.168',2,2,NULL,NULL),(1701,'169.254.2.169',2,2,NULL,NULL),(1702,'169.254.2.170',2,2,NULL,NULL),(1703,'169.254.2.171',2,2,NULL,NULL),(1704,'169.254.2.172',2,2,NULL,NULL),(1705,'169.254.2.173',2,2,NULL,NULL),(1706,'169.254.2.174',2,2,NULL,NULL),(1707,'169.254.2.175',2,2,NULL,NULL),(1708,'169.254.2.176',2,2,NULL,NULL),(1709,'169.254.2.177',2,2,NULL,NULL),(1710,'169.254.2.178',2,2,NULL,NULL),(1711,'169.254.2.179',2,2,NULL,NULL),(1712,'169.254.2.180',2,2,NULL,NULL),(1713,'169.254.2.181',2,2,NULL,NULL),(1714,'169.254.2.182',2,2,NULL,NULL),(1715,'169.254.2.183',2,2,NULL,NULL),(1716,'169.254.2.184',2,2,NULL,NULL),(1717,'169.254.2.185',2,2,NULL,NULL),(1718,'169.254.2.186',2,2,NULL,NULL),(1719,'169.254.2.187',2,2,NULL,NULL),(1720,'169.254.2.188',2,2,NULL,NULL),(1721,'169.254.2.189',2,2,NULL,NULL),(1722,'169.254.2.190',2,2,NULL,NULL),(1723,'169.254.2.191',2,2,NULL,NULL),(1724,'169.254.2.192',2,2,NULL,NULL),(1725,'169.254.2.193',2,2,NULL,NULL),(1726,'169.254.2.194',2,2,NULL,NULL),(1727,'169.254.2.195',2,2,NULL,NULL),(1728,'169.254.2.196',2,2,NULL,NULL),(1729,'169.254.2.197',2,2,NULL,NULL),(1730,'169.254.2.198',2,2,NULL,NULL),(1731,'169.254.2.199',2,2,NULL,NULL),(1732,'169.254.2.200',2,2,NULL,NULL),(1733,'169.254.2.201',2,2,NULL,NULL),(1734,'169.254.2.202',2,2,NULL,NULL),(1735,'169.254.2.203',2,2,NULL,NULL),(1736,'169.254.2.204',2,2,NULL,NULL),(1737,'169.254.2.205',2,2,NULL,NULL),(1738,'169.254.2.206',2,2,NULL,NULL),(1739,'169.254.2.207',2,2,NULL,NULL),(1740,'169.254.2.208',2,2,NULL,NULL),(1741,'169.254.2.209',2,2,NULL,NULL),(1742,'169.254.2.210',2,2,NULL,NULL),(1743,'169.254.2.211',2,2,NULL,NULL),(1744,'169.254.2.212',2,2,NULL,NULL),(1745,'169.254.2.213',2,2,NULL,NULL),(1746,'169.254.2.214',2,2,NULL,NULL),(1747,'169.254.2.215',2,2,NULL,NULL),(1748,'169.254.2.216',2,2,NULL,NULL),(1749,'169.254.2.217',2,2,NULL,NULL),(1750,'169.254.2.218',2,2,NULL,NULL),(1751,'169.254.2.219',2,2,NULL,NULL),(1752,'169.254.2.220',2,2,NULL,NULL),(1753,'169.254.2.221',2,2,NULL,NULL),(1754,'169.254.2.222',2,2,NULL,NULL),(1755,'169.254.2.223',2,2,NULL,NULL),(1756,'169.254.2.224',2,2,NULL,NULL),(1757,'169.254.2.225',2,2,NULL,NULL),(1758,'169.254.2.226',2,2,NULL,NULL),(1759,'169.254.2.227',2,2,NULL,NULL),(1760,'169.254.2.228',2,2,NULL,NULL),(1761,'169.254.2.229',2,2,NULL,NULL),(1762,'169.254.2.230',2,2,NULL,NULL),(1763,'169.254.2.231',2,2,NULL,NULL),(1764,'169.254.2.232',2,2,NULL,NULL),(1765,'169.254.2.233',2,2,NULL,NULL),(1766,'169.254.2.234',2,2,NULL,NULL),(1767,'169.254.2.235',2,2,NULL,NULL),(1768,'169.254.2.236',2,2,NULL,NULL),(1769,'169.254.2.237',2,2,NULL,NULL),(1770,'169.254.2.238',2,2,NULL,NULL),(1771,'169.254.2.239',2,2,NULL,NULL),(1772,'169.254.2.240',2,2,NULL,NULL),(1773,'169.254.2.241',2,2,NULL,NULL),(1774,'169.254.2.242',2,2,NULL,NULL),(1775,'169.254.2.243',2,2,NULL,NULL),(1776,'169.254.2.244',2,2,NULL,NULL),(1777,'169.254.2.245',2,2,NULL,NULL),(1778,'169.254.2.246',2,2,NULL,NULL),(1779,'169.254.2.247',2,2,NULL,NULL),(1780,'169.254.2.248',2,2,NULL,NULL),(1781,'169.254.2.249',2,2,NULL,NULL),(1782,'169.254.2.250',2,2,NULL,NULL),(1783,'169.254.2.251',2,2,NULL,NULL),(1784,'169.254.2.252',2,2,NULL,NULL),(1785,'169.254.2.253',2,2,NULL,NULL),(1786,'169.254.2.254',2,2,NULL,NULL),(1787,'169.254.2.255',2,2,NULL,NULL),(1788,'169.254.3.0',2,2,NULL,NULL),(1789,'169.254.3.1',2,2,NULL,NULL),(1790,'169.254.3.2',2,2,NULL,NULL),(1791,'169.254.3.3',2,2,NULL,NULL),(1792,'169.254.3.4',2,2,NULL,NULL),(1793,'169.254.3.5',2,2,NULL,NULL),(1794,'169.254.3.6',2,2,NULL,NULL),(1795,'169.254.3.7',2,2,NULL,NULL),(1796,'169.254.3.8',2,2,NULL,NULL),(1797,'169.254.3.9',2,2,NULL,NULL),(1798,'169.254.3.10',2,2,NULL,NULL),(1799,'169.254.3.11',2,2,NULL,NULL),(1800,'169.254.3.12',2,2,NULL,NULL),(1801,'169.254.3.13',2,2,NULL,NULL),(1802,'169.254.3.14',2,2,NULL,NULL),(1803,'169.254.3.15',2,2,NULL,NULL),(1804,'169.254.3.16',2,2,NULL,NULL),(1805,'169.254.3.17',2,2,NULL,NULL),(1806,'169.254.3.18',2,2,NULL,NULL),(1807,'169.254.3.19',2,2,NULL,NULL),(1808,'169.254.3.20',2,2,NULL,NULL),(1809,'169.254.3.21',2,2,NULL,NULL),(1810,'169.254.3.22',2,2,NULL,NULL),(1811,'169.254.3.23',2,2,NULL,NULL),(1812,'169.254.3.24',2,2,NULL,NULL),(1813,'169.254.3.25',2,2,NULL,NULL),(1814,'169.254.3.26',2,2,NULL,NULL),(1815,'169.254.3.27',2,2,NULL,NULL),(1816,'169.254.3.28',2,2,NULL,NULL),(1817,'169.254.3.29',2,2,NULL,NULL),(1818,'169.254.3.30',2,2,NULL,NULL),(1819,'169.254.3.31',2,2,NULL,NULL),(1820,'169.254.3.32',2,2,NULL,NULL),(1821,'169.254.3.33',2,2,NULL,NULL),(1822,'169.254.3.34',2,2,NULL,NULL),(1823,'169.254.3.35',2,2,NULL,NULL),(1824,'169.254.3.36',2,2,NULL,NULL),(1825,'169.254.3.37',2,2,NULL,NULL),(1826,'169.254.3.38',2,2,NULL,NULL),(1827,'169.254.3.39',2,2,NULL,NULL),(1828,'169.254.3.40',2,2,NULL,NULL),(1829,'169.254.3.41',2,2,NULL,NULL),(1830,'169.254.3.42',2,2,NULL,NULL),(1831,'169.254.3.43',2,2,NULL,NULL),(1832,'169.254.3.44',2,2,NULL,NULL),(1833,'169.254.3.45',2,2,NULL,NULL),(1834,'169.254.3.46',2,2,NULL,NULL),(1835,'169.254.3.47',2,2,NULL,NULL),(1836,'169.254.3.48',2,2,NULL,NULL),(1837,'169.254.3.49',2,2,NULL,NULL),(1838,'169.254.3.50',2,2,NULL,NULL),(1839,'169.254.3.51',2,2,NULL,NULL),(1840,'169.254.3.52',2,2,NULL,NULL),(1841,'169.254.3.53',2,2,NULL,NULL),(1842,'169.254.3.54',2,2,NULL,NULL),(1843,'169.254.3.55',2,2,NULL,NULL),(1844,'169.254.3.56',2,2,NULL,NULL),(1845,'169.254.3.57',2,2,NULL,NULL),(1846,'169.254.3.58',2,2,NULL,NULL),(1847,'169.254.3.59',2,2,NULL,NULL),(1848,'169.254.3.60',2,2,NULL,NULL),(1849,'169.254.3.61',2,2,NULL,NULL),(1850,'169.254.3.62',2,2,NULL,NULL),(1851,'169.254.3.63',2,2,NULL,NULL),(1852,'169.254.3.64',2,2,NULL,NULL),(1853,'169.254.3.65',2,2,NULL,NULL),(1854,'169.254.3.66',2,2,NULL,NULL),(1855,'169.254.3.67',2,2,NULL,NULL),(1856,'169.254.3.68',2,2,NULL,NULL),(1857,'169.254.3.69',2,2,NULL,NULL),(1858,'169.254.3.70',2,2,NULL,NULL),(1859,'169.254.3.71',2,2,NULL,NULL),(1860,'169.254.3.72',2,2,NULL,NULL),(1861,'169.254.3.73',2,2,NULL,NULL),(1862,'169.254.3.74',2,2,NULL,NULL),(1863,'169.254.3.75',2,2,NULL,NULL),(1864,'169.254.3.76',2,2,NULL,NULL),(1865,'169.254.3.77',2,2,NULL,NULL),(1866,'169.254.3.78',2,2,NULL,NULL),(1867,'169.254.3.79',2,2,NULL,NULL),(1868,'169.254.3.80',2,2,NULL,NULL),(1869,'169.254.3.81',2,2,NULL,NULL),(1870,'169.254.3.82',2,2,NULL,NULL),(1871,'169.254.3.83',2,2,NULL,NULL),(1872,'169.254.3.84',2,2,NULL,NULL),(1873,'169.254.3.85',2,2,NULL,NULL),(1874,'169.254.3.86',2,2,NULL,NULL),(1875,'169.254.3.87',2,2,NULL,NULL),(1876,'169.254.3.88',2,2,NULL,NULL),(1877,'169.254.3.89',2,2,NULL,NULL),(1878,'169.254.3.90',2,2,NULL,NULL),(1879,'169.254.3.91',2,2,NULL,NULL),(1880,'169.254.3.92',2,2,NULL,NULL),(1881,'169.254.3.93',2,2,NULL,NULL),(1882,'169.254.3.94',2,2,NULL,NULL),(1883,'169.254.3.95',2,2,NULL,NULL),(1884,'169.254.3.96',2,2,NULL,NULL),(1885,'169.254.3.97',2,2,NULL,NULL),(1886,'169.254.3.98',2,2,NULL,NULL),(1887,'169.254.3.99',2,2,NULL,NULL),(1888,'169.254.3.100',2,2,NULL,NULL),(1889,'169.254.3.101',2,2,NULL,NULL),(1890,'169.254.3.102',2,2,NULL,NULL),(1891,'169.254.3.103',2,2,NULL,NULL),(1892,'169.254.3.104',2,2,NULL,NULL),(1893,'169.254.3.105',2,2,NULL,NULL),(1894,'169.254.3.106',2,2,NULL,NULL),(1895,'169.254.3.107',2,2,NULL,NULL),(1896,'169.254.3.108',2,2,NULL,NULL),(1897,'169.254.3.109',2,2,NULL,NULL),(1898,'169.254.3.110',2,2,NULL,NULL),(1899,'169.254.3.111',2,2,NULL,NULL),(1900,'169.254.3.112',2,2,NULL,NULL),(1901,'169.254.3.113',2,2,NULL,NULL),(1902,'169.254.3.114',2,2,NULL,NULL),(1903,'169.254.3.115',2,2,NULL,NULL),(1904,'169.254.3.116',2,2,NULL,NULL),(1905,'169.254.3.117',2,2,NULL,NULL),(1906,'169.254.3.118',2,2,NULL,NULL),(1907,'169.254.3.119',2,2,NULL,NULL),(1908,'169.254.3.120',2,2,NULL,NULL),(1909,'169.254.3.121',2,2,NULL,NULL),(1910,'169.254.3.122',2,2,NULL,NULL),(1911,'169.254.3.123',2,2,NULL,NULL),(1912,'169.254.3.124',2,2,NULL,NULL),(1913,'169.254.3.125',2,2,NULL,NULL),(1914,'169.254.3.126',2,2,NULL,NULL),(1915,'169.254.3.127',2,2,NULL,NULL),(1916,'169.254.3.128',2,2,NULL,NULL),(1917,'169.254.3.129',2,2,NULL,NULL),(1918,'169.254.3.130',2,2,NULL,NULL),(1919,'169.254.3.131',2,2,NULL,NULL),(1920,'169.254.3.132',2,2,NULL,NULL),(1921,'169.254.3.133',2,2,NULL,NULL),(1922,'169.254.3.134',2,2,NULL,NULL),(1923,'169.254.3.135',2,2,NULL,NULL),(1924,'169.254.3.136',2,2,NULL,NULL),(1925,'169.254.3.137',2,2,NULL,NULL),(1926,'169.254.3.138',2,2,NULL,NULL),(1927,'169.254.3.139',2,2,NULL,NULL),(1928,'169.254.3.140',2,2,NULL,NULL),(1929,'169.254.3.141',2,2,NULL,NULL),(1930,'169.254.3.142',2,2,NULL,NULL),(1931,'169.254.3.143',2,2,NULL,NULL),(1932,'169.254.3.144',2,2,NULL,NULL),(1933,'169.254.3.145',2,2,NULL,NULL),(1934,'169.254.3.146',2,2,NULL,NULL),(1935,'169.254.3.147',2,2,NULL,NULL),(1936,'169.254.3.148',2,2,NULL,NULL),(1937,'169.254.3.149',2,2,NULL,NULL),(1938,'169.254.3.150',2,2,NULL,NULL),(1939,'169.254.3.151',2,2,NULL,NULL),(1940,'169.254.3.152',2,2,NULL,NULL),(1941,'169.254.3.153',2,2,NULL,NULL),(1942,'169.254.3.154',2,2,NULL,NULL),(1943,'169.254.3.155',2,2,NULL,NULL),(1944,'169.254.3.156',2,2,NULL,NULL),(1945,'169.254.3.157',2,2,NULL,NULL),(1946,'169.254.3.158',2,2,NULL,NULL),(1947,'169.254.3.159',2,2,NULL,NULL),(1948,'169.254.3.160',2,2,NULL,NULL),(1949,'169.254.3.161',2,2,NULL,NULL),(1950,'169.254.3.162',2,2,NULL,NULL),(1951,'169.254.3.163',2,2,NULL,NULL),(1952,'169.254.3.164',2,2,NULL,NULL),(1953,'169.254.3.165',2,2,NULL,NULL),(1954,'169.254.3.166',2,2,NULL,NULL),(1955,'169.254.3.167',2,2,NULL,NULL),(1956,'169.254.3.168',2,2,NULL,NULL),(1957,'169.254.3.169',2,2,NULL,NULL),(1958,'169.254.3.170',2,2,NULL,NULL),(1959,'169.254.3.171',2,2,NULL,NULL),(1960,'169.254.3.172',2,2,NULL,NULL),(1961,'169.254.3.173',2,2,NULL,NULL),(1962,'169.254.3.174',2,2,NULL,NULL),(1963,'169.254.3.175',2,2,NULL,NULL),(1964,'169.254.3.176',2,2,NULL,NULL),(1965,'169.254.3.177',2,2,NULL,NULL),(1966,'169.254.3.178',2,2,NULL,NULL),(1967,'169.254.3.179',2,2,NULL,NULL),(1968,'169.254.3.180',2,2,NULL,NULL),(1969,'169.254.3.181',2,2,NULL,NULL),(1970,'169.254.3.182',2,2,NULL,NULL),(1971,'169.254.3.183',2,2,NULL,NULL),(1972,'169.254.3.184',2,2,NULL,NULL),(1973,'169.254.3.185',2,2,NULL,NULL),(1974,'169.254.3.186',2,2,NULL,NULL),(1975,'169.254.3.187',2,2,NULL,NULL),(1976,'169.254.3.188',2,2,NULL,NULL),(1977,'169.254.3.189',2,2,NULL,NULL),(1978,'169.254.3.190',2,2,NULL,NULL),(1979,'169.254.3.191',2,2,NULL,NULL),(1980,'169.254.3.192',2,2,NULL,NULL),(1981,'169.254.3.193',2,2,NULL,NULL),(1982,'169.254.3.194',2,2,NULL,NULL),(1983,'169.254.3.195',2,2,NULL,NULL),(1984,'169.254.3.196',2,2,NULL,NULL),(1985,'169.254.3.197',2,2,NULL,NULL),(1986,'169.254.3.198',2,2,NULL,NULL),(1987,'169.254.3.199',2,2,NULL,NULL),(1988,'169.254.3.200',2,2,NULL,NULL),(1989,'169.254.3.201',2,2,NULL,NULL),(1990,'169.254.3.202',2,2,NULL,NULL),(1991,'169.254.3.203',2,2,NULL,NULL),(1992,'169.254.3.204',2,2,NULL,NULL),(1993,'169.254.3.205',2,2,NULL,NULL),(1994,'169.254.3.206',2,2,NULL,NULL),(1995,'169.254.3.207',2,2,NULL,NULL),(1996,'169.254.3.208',2,2,NULL,NULL),(1997,'169.254.3.209',2,2,NULL,NULL),(1998,'169.254.3.210',2,2,NULL,NULL),(1999,'169.254.3.211',2,2,NULL,NULL),(2000,'169.254.3.212',2,2,NULL,NULL),(2001,'169.254.3.213',2,2,NULL,NULL),(2002,'169.254.3.214',2,2,NULL,NULL),(2003,'169.254.3.215',2,2,NULL,NULL),(2004,'169.254.3.216',2,2,NULL,NULL),(2005,'169.254.3.217',2,2,NULL,NULL),(2006,'169.254.3.218',2,2,NULL,NULL),(2007,'169.254.3.219',2,2,NULL,NULL),(2008,'169.254.3.220',2,2,NULL,NULL),(2009,'169.254.3.221',2,2,NULL,NULL),(2010,'169.254.3.222',2,2,NULL,NULL),(2011,'169.254.3.223',2,2,NULL,NULL),(2012,'169.254.3.224',2,2,NULL,NULL),(2013,'169.254.3.225',2,2,NULL,NULL),(2014,'169.254.3.226',2,2,NULL,NULL),(2015,'169.254.3.227',2,2,NULL,NULL),(2016,'169.254.3.228',2,2,NULL,NULL),(2017,'169.254.3.229',2,2,NULL,NULL),(2018,'169.254.3.230',2,2,NULL,NULL),(2019,'169.254.3.231',2,2,NULL,NULL),(2020,'169.254.3.232',2,2,NULL,NULL),(2021,'169.254.3.233',2,2,NULL,NULL),(2022,'169.254.3.234',2,2,NULL,NULL),(2023,'169.254.3.235',2,2,NULL,NULL),(2024,'169.254.3.236',2,2,NULL,NULL),(2025,'169.254.3.237',2,2,NULL,NULL),(2026,'169.254.3.238',2,2,NULL,NULL),(2027,'169.254.3.239',2,2,NULL,NULL),(2028,'169.254.3.240',2,2,NULL,NULL),(2029,'169.254.3.241',2,2,NULL,NULL),(2030,'169.254.3.242',2,2,NULL,NULL),(2031,'169.254.3.243',2,2,NULL,NULL),(2032,'169.254.3.244',2,2,NULL,NULL),(2033,'169.254.3.245',2,2,NULL,NULL),(2034,'169.254.3.246',2,2,NULL,NULL),(2035,'169.254.3.247',2,2,NULL,NULL),(2036,'169.254.3.248',2,2,NULL,NULL),(2037,'169.254.3.249',2,2,NULL,NULL),(2038,'169.254.3.250',2,2,NULL,NULL),(2039,'169.254.3.251',2,2,NULL,NULL),(2040,'169.254.3.252',2,2,NULL,NULL),(2041,'169.254.3.253',2,2,NULL,NULL),(2042,'169.254.3.254',2,2,NULL,NULL),(2043,'169.254.0.2',3,3,NULL,NULL),(2044,'169.254.0.3',3,3,NULL,NULL),(2045,'169.254.0.4',3,3,NULL,NULL),(2046,'169.254.0.5',3,3,NULL,NULL),(2047,'169.254.0.6',3,3,NULL,NULL),(2048,'169.254.0.7',3,3,NULL,NULL),(2049,'169.254.0.8',3,3,NULL,NULL),(2050,'169.254.0.9',3,3,NULL,NULL),(2051,'169.254.0.10',3,3,NULL,NULL),(2052,'169.254.0.11',3,3,NULL,NULL),(2053,'169.254.0.12',3,3,NULL,NULL),(2054,'169.254.0.13',3,3,NULL,NULL),(2055,'169.254.0.14',3,3,NULL,NULL),(2056,'169.254.0.15',3,3,NULL,NULL),(2057,'169.254.0.16',3,3,NULL,NULL),(2058,'169.254.0.17',3,3,NULL,NULL),(2059,'169.254.0.18',3,3,NULL,NULL),(2060,'169.254.0.19',3,3,NULL,NULL),(2061,'169.254.0.20',3,3,NULL,NULL),(2062,'169.254.0.21',3,3,NULL,NULL),(2063,'169.254.0.22',3,3,NULL,NULL),(2064,'169.254.0.23',3,3,NULL,NULL),(2065,'169.254.0.24',3,3,NULL,NULL),(2066,'169.254.0.25',3,3,NULL,NULL),(2067,'169.254.0.26',3,3,NULL,NULL),(2068,'169.254.0.27',3,3,NULL,NULL),(2069,'169.254.0.28',3,3,NULL,NULL),(2070,'169.254.0.29',3,3,NULL,NULL),(2071,'169.254.0.30',3,3,NULL,NULL),(2072,'169.254.0.31',3,3,NULL,NULL),(2073,'169.254.0.32',3,3,NULL,NULL),(2074,'169.254.0.33',3,3,NULL,NULL),(2075,'169.254.0.34',3,3,NULL,NULL),(2076,'169.254.0.35',3,3,NULL,NULL),(2077,'169.254.0.36',3,3,NULL,NULL),(2078,'169.254.0.37',3,3,NULL,NULL),(2079,'169.254.0.38',3,3,NULL,NULL),(2080,'169.254.0.39',3,3,NULL,NULL),(2081,'169.254.0.40',3,3,NULL,NULL),(2082,'169.254.0.41',3,3,NULL,NULL),(2083,'169.254.0.42',3,3,NULL,NULL),(2084,'169.254.0.43',3,3,NULL,NULL),(2085,'169.254.0.44',3,3,NULL,NULL),(2086,'169.254.0.45',3,3,NULL,NULL),(2087,'169.254.0.46',3,3,NULL,NULL),(2088,'169.254.0.47',3,3,NULL,NULL),(2089,'169.254.0.48',3,3,NULL,NULL),(2090,'169.254.0.49',3,3,NULL,NULL),(2091,'169.254.0.50',3,3,NULL,NULL),(2092,'169.254.0.51',3,3,NULL,NULL),(2093,'169.254.0.52',3,3,NULL,NULL),(2094,'169.254.0.53',3,3,NULL,NULL),(2095,'169.254.0.54',3,3,NULL,NULL),(2096,'169.254.0.55',3,3,NULL,NULL),(2097,'169.254.0.56',3,3,NULL,NULL),(2098,'169.254.0.57',3,3,NULL,NULL),(2099,'169.254.0.58',3,3,NULL,NULL),(2100,'169.254.0.59',3,3,NULL,NULL),(2101,'169.254.0.60',3,3,NULL,NULL),(2102,'169.254.0.61',3,3,NULL,NULL),(2103,'169.254.0.62',3,3,NULL,NULL),(2104,'169.254.0.63',3,3,NULL,NULL),(2105,'169.254.0.64',3,3,NULL,NULL),(2106,'169.254.0.65',3,3,NULL,NULL),(2107,'169.254.0.66',3,3,NULL,NULL),(2108,'169.254.0.67',3,3,NULL,NULL),(2109,'169.254.0.68',3,3,NULL,NULL),(2110,'169.254.0.69',3,3,NULL,NULL),(2111,'169.254.0.70',3,3,NULL,NULL),(2112,'169.254.0.71',3,3,NULL,NULL),(2113,'169.254.0.72',3,3,NULL,NULL),(2114,'169.254.0.73',3,3,NULL,NULL),(2115,'169.254.0.74',3,3,NULL,NULL),(2116,'169.254.0.75',3,3,NULL,NULL),(2117,'169.254.0.76',3,3,NULL,NULL),(2118,'169.254.0.77',3,3,NULL,NULL),(2119,'169.254.0.78',3,3,NULL,NULL),(2120,'169.254.0.79',3,3,NULL,NULL),(2121,'169.254.0.80',3,3,NULL,NULL),(2122,'169.254.0.81',3,3,NULL,NULL),(2123,'169.254.0.82',3,3,NULL,NULL),(2124,'169.254.0.83',3,3,NULL,NULL),(2125,'169.254.0.84',3,3,NULL,NULL),(2126,'169.254.0.85',3,3,NULL,NULL),(2127,'169.254.0.86',3,3,NULL,NULL),(2128,'169.254.0.87',3,3,NULL,NULL),(2129,'169.254.0.88',3,3,NULL,NULL),(2130,'169.254.0.89',3,3,NULL,NULL),(2131,'169.254.0.90',3,3,NULL,NULL),(2132,'169.254.0.91',3,3,NULL,NULL),(2133,'169.254.0.92',3,3,NULL,NULL),(2134,'169.254.0.93',3,3,NULL,NULL),(2135,'169.254.0.94',3,3,NULL,NULL),(2136,'169.254.0.95',3,3,NULL,NULL),(2137,'169.254.0.96',3,3,NULL,NULL),(2138,'169.254.0.97',3,3,NULL,NULL),(2139,'169.254.0.98',3,3,NULL,NULL),(2140,'169.254.0.99',3,3,NULL,NULL),(2141,'169.254.0.100',3,3,NULL,NULL),(2142,'169.254.0.101',3,3,NULL,NULL),(2143,'169.254.0.102',3,3,NULL,NULL),(2144,'169.254.0.103',3,3,NULL,NULL),(2145,'169.254.0.104',3,3,NULL,NULL),(2146,'169.254.0.105',3,3,NULL,NULL),(2147,'169.254.0.106',3,3,NULL,NULL),(2148,'169.254.0.107',3,3,NULL,NULL),(2149,'169.254.0.108',3,3,NULL,NULL),(2150,'169.254.0.109',3,3,NULL,NULL),(2151,'169.254.0.110',3,3,NULL,NULL),(2152,'169.254.0.111',3,3,NULL,NULL),(2153,'169.254.0.112',3,3,NULL,NULL),(2154,'169.254.0.113',3,3,NULL,NULL),(2155,'169.254.0.114',3,3,NULL,NULL),(2156,'169.254.0.115',3,3,NULL,NULL),(2157,'169.254.0.116',3,3,NULL,NULL),(2158,'169.254.0.117',3,3,NULL,NULL),(2159,'169.254.0.118',3,3,NULL,NULL),(2160,'169.254.0.119',3,3,NULL,NULL),(2161,'169.254.0.120',3,3,NULL,NULL),(2162,'169.254.0.121',3,3,NULL,NULL),(2163,'169.254.0.122',3,3,NULL,NULL),(2164,'169.254.0.123',3,3,NULL,NULL),(2165,'169.254.0.124',3,3,NULL,NULL),(2166,'169.254.0.125',3,3,NULL,NULL),(2167,'169.254.0.126',3,3,NULL,NULL),(2168,'169.254.0.127',3,3,NULL,NULL),(2169,'169.254.0.128',3,3,NULL,NULL),(2170,'169.254.0.129',3,3,NULL,NULL),(2171,'169.254.0.130',3,3,NULL,NULL),(2172,'169.254.0.131',3,3,NULL,NULL),(2173,'169.254.0.132',3,3,NULL,NULL),(2174,'169.254.0.133',3,3,NULL,NULL),(2175,'169.254.0.134',3,3,NULL,NULL),(2176,'169.254.0.135',3,3,NULL,NULL),(2177,'169.254.0.136',3,3,NULL,NULL),(2178,'169.254.0.137',3,3,NULL,NULL),(2179,'169.254.0.138',3,3,NULL,NULL),(2180,'169.254.0.139',3,3,NULL,NULL),(2181,'169.254.0.140',3,3,NULL,NULL),(2182,'169.254.0.141',3,3,NULL,NULL),(2183,'169.254.0.142',3,3,NULL,NULL),(2184,'169.254.0.143',3,3,NULL,NULL),(2185,'169.254.0.144',3,3,NULL,NULL),(2186,'169.254.0.145',3,3,NULL,NULL),(2187,'169.254.0.146',3,3,NULL,NULL),(2188,'169.254.0.147',3,3,NULL,NULL),(2189,'169.254.0.148',3,3,NULL,NULL),(2190,'169.254.0.149',3,3,NULL,NULL),(2191,'169.254.0.150',3,3,NULL,NULL),(2192,'169.254.0.151',3,3,NULL,NULL),(2193,'169.254.0.152',3,3,NULL,NULL),(2194,'169.254.0.153',3,3,NULL,NULL),(2195,'169.254.0.154',3,3,NULL,NULL),(2196,'169.254.0.155',3,3,NULL,NULL),(2197,'169.254.0.156',3,3,NULL,NULL),(2198,'169.254.0.157',3,3,NULL,NULL),(2199,'169.254.0.158',3,3,NULL,NULL),(2200,'169.254.0.159',3,3,NULL,NULL),(2201,'169.254.0.160',3,3,NULL,NULL),(2202,'169.254.0.161',3,3,NULL,NULL),(2203,'169.254.0.162',3,3,NULL,NULL),(2204,'169.254.0.163',3,3,NULL,NULL),(2205,'169.254.0.164',3,3,NULL,NULL),(2206,'169.254.0.165',3,3,NULL,NULL),(2207,'169.254.0.166',3,3,NULL,NULL),(2208,'169.254.0.167',3,3,NULL,NULL),(2209,'169.254.0.168',3,3,NULL,NULL),(2210,'169.254.0.169',3,3,NULL,NULL),(2211,'169.254.0.170',3,3,NULL,NULL),(2212,'169.254.0.171',3,3,NULL,NULL),(2213,'169.254.0.172',3,3,NULL,NULL),(2214,'169.254.0.173',3,3,NULL,NULL),(2215,'169.254.0.174',3,3,NULL,NULL),(2216,'169.254.0.175',3,3,NULL,NULL),(2217,'169.254.0.176',3,3,NULL,NULL),(2218,'169.254.0.177',3,3,NULL,NULL),(2219,'169.254.0.178',3,3,NULL,NULL),(2220,'169.254.0.179',3,3,NULL,NULL),(2221,'169.254.0.180',3,3,NULL,NULL),(2222,'169.254.0.181',3,3,NULL,NULL),(2223,'169.254.0.182',3,3,NULL,NULL),(2224,'169.254.0.183',3,3,NULL,NULL),(2225,'169.254.0.184',3,3,NULL,NULL),(2226,'169.254.0.185',3,3,NULL,NULL),(2227,'169.254.0.186',3,3,NULL,NULL),(2228,'169.254.0.187',3,3,NULL,NULL),(2229,'169.254.0.188',3,3,NULL,NULL),(2230,'169.254.0.189',3,3,NULL,NULL),(2231,'169.254.0.190',3,3,NULL,NULL),(2232,'169.254.0.191',3,3,NULL,NULL),(2233,'169.254.0.192',3,3,NULL,NULL),(2234,'169.254.0.193',3,3,NULL,NULL),(2235,'169.254.0.194',3,3,NULL,NULL),(2236,'169.254.0.195',3,3,NULL,NULL),(2237,'169.254.0.196',3,3,NULL,NULL),(2238,'169.254.0.197',3,3,NULL,NULL),(2239,'169.254.0.198',3,3,NULL,NULL),(2240,'169.254.0.199',3,3,NULL,NULL),(2241,'169.254.0.200',3,3,NULL,NULL),(2242,'169.254.0.201',3,3,NULL,NULL),(2243,'169.254.0.202',3,3,NULL,NULL),(2244,'169.254.0.203',3,3,NULL,NULL),(2245,'169.254.0.204',3,3,NULL,NULL),(2246,'169.254.0.205',3,3,NULL,NULL),(2247,'169.254.0.206',3,3,NULL,NULL),(2248,'169.254.0.207',3,3,NULL,NULL),(2249,'169.254.0.208',3,3,NULL,NULL),(2250,'169.254.0.209',3,3,NULL,NULL),(2251,'169.254.0.210',3,3,NULL,NULL),(2252,'169.254.0.211',3,3,NULL,NULL),(2253,'169.254.0.212',3,3,NULL,NULL),(2254,'169.254.0.213',3,3,NULL,NULL),(2255,'169.254.0.214',3,3,NULL,NULL),(2256,'169.254.0.215',3,3,NULL,NULL),(2257,'169.254.0.216',3,3,NULL,NULL),(2258,'169.254.0.217',3,3,NULL,NULL),(2259,'169.254.0.218',3,3,NULL,NULL),(2260,'169.254.0.219',3,3,NULL,NULL),(2261,'169.254.0.220',3,3,NULL,NULL),(2262,'169.254.0.221',3,3,NULL,NULL),(2263,'169.254.0.222',3,3,NULL,NULL),(2264,'169.254.0.223',3,3,NULL,NULL),(2265,'169.254.0.224',3,3,NULL,NULL),(2266,'169.254.0.225',3,3,NULL,NULL),(2267,'169.254.0.226',3,3,NULL,NULL),(2268,'169.254.0.227',3,3,NULL,NULL),(2269,'169.254.0.228',3,3,NULL,NULL),(2270,'169.254.0.229',3,3,NULL,NULL),(2271,'169.254.0.230',3,3,NULL,NULL),(2272,'169.254.0.231',3,3,NULL,NULL),(2273,'169.254.0.232',3,3,NULL,NULL),(2274,'169.254.0.233',3,3,NULL,NULL),(2275,'169.254.0.234',3,3,NULL,NULL),(2276,'169.254.0.235',3,3,NULL,NULL),(2277,'169.254.0.236',3,3,NULL,NULL),(2278,'169.254.0.237',3,3,NULL,NULL),(2279,'169.254.0.238',3,3,NULL,NULL),(2280,'169.254.0.239',3,3,NULL,NULL),(2281,'169.254.0.240',3,3,NULL,NULL),(2282,'169.254.0.241',3,3,NULL,NULL),(2283,'169.254.0.242',3,3,NULL,NULL),(2284,'169.254.0.243',3,3,NULL,NULL),(2285,'169.254.0.244',3,3,NULL,NULL),(2286,'169.254.0.245',3,3,NULL,NULL),(2287,'169.254.0.246',3,3,NULL,NULL),(2288,'169.254.0.247',3,3,NULL,NULL),(2289,'169.254.0.248',3,3,NULL,NULL),(2290,'169.254.0.249',3,3,NULL,NULL),(2291,'169.254.0.250',3,3,NULL,NULL),(2292,'169.254.0.251',3,3,NULL,NULL),(2293,'169.254.0.252',3,3,NULL,NULL),(2294,'169.254.0.253',3,3,NULL,NULL),(2295,'169.254.0.254',3,3,NULL,NULL),(2296,'169.254.0.255',3,3,NULL,NULL),(2297,'169.254.1.0',3,3,NULL,NULL),(2298,'169.254.1.1',3,3,NULL,NULL),(2299,'169.254.1.2',3,3,NULL,NULL),(2300,'169.254.1.3',3,3,NULL,NULL),(2301,'169.254.1.4',3,3,NULL,NULL),(2302,'169.254.1.5',3,3,NULL,NULL),(2303,'169.254.1.6',3,3,NULL,NULL),(2304,'169.254.1.7',3,3,NULL,NULL),(2305,'169.254.1.8',3,3,NULL,NULL),(2306,'169.254.1.9',3,3,NULL,NULL),(2307,'169.254.1.10',3,3,NULL,NULL),(2308,'169.254.1.11',3,3,NULL,NULL),(2309,'169.254.1.12',3,3,NULL,NULL),(2310,'169.254.1.13',3,3,NULL,NULL),(2311,'169.254.1.14',3,3,NULL,NULL),(2312,'169.254.1.15',3,3,NULL,NULL),(2313,'169.254.1.16',3,3,NULL,NULL),(2314,'169.254.1.17',3,3,NULL,NULL),(2315,'169.254.1.18',3,3,NULL,NULL),(2316,'169.254.1.19',3,3,NULL,NULL),(2317,'169.254.1.20',3,3,NULL,NULL),(2318,'169.254.1.21',3,3,NULL,NULL),(2319,'169.254.1.22',3,3,NULL,NULL),(2320,'169.254.1.23',3,3,NULL,NULL),(2321,'169.254.1.24',3,3,NULL,NULL),(2322,'169.254.1.25',3,3,NULL,NULL),(2323,'169.254.1.26',3,3,NULL,NULL),(2324,'169.254.1.27',3,3,NULL,NULL),(2325,'169.254.1.28',3,3,NULL,NULL),(2326,'169.254.1.29',3,3,NULL,NULL),(2327,'169.254.1.30',3,3,NULL,NULL),(2328,'169.254.1.31',3,3,NULL,NULL),(2329,'169.254.1.32',3,3,NULL,NULL),(2330,'169.254.1.33',3,3,NULL,NULL),(2331,'169.254.1.34',3,3,NULL,NULL),(2332,'169.254.1.35',3,3,NULL,NULL),(2333,'169.254.1.36',3,3,NULL,NULL),(2334,'169.254.1.37',3,3,NULL,NULL),(2335,'169.254.1.38',3,3,NULL,NULL),(2336,'169.254.1.39',3,3,NULL,NULL),(2337,'169.254.1.40',3,3,NULL,NULL),(2338,'169.254.1.41',3,3,NULL,NULL),(2339,'169.254.1.42',3,3,NULL,NULL),(2340,'169.254.1.43',3,3,NULL,NULL),(2341,'169.254.1.44',3,3,NULL,NULL),(2342,'169.254.1.45',3,3,NULL,NULL),(2343,'169.254.1.46',3,3,NULL,NULL),(2344,'169.254.1.47',3,3,NULL,NULL),(2345,'169.254.1.48',3,3,NULL,NULL),(2346,'169.254.1.49',3,3,NULL,NULL),(2347,'169.254.1.50',3,3,NULL,NULL),(2348,'169.254.1.51',3,3,NULL,NULL),(2349,'169.254.1.52',3,3,NULL,NULL),(2350,'169.254.1.53',3,3,NULL,NULL),(2351,'169.254.1.54',3,3,NULL,NULL),(2352,'169.254.1.55',3,3,NULL,NULL),(2353,'169.254.1.56',3,3,NULL,NULL),(2354,'169.254.1.57',3,3,NULL,NULL),(2355,'169.254.1.58',3,3,NULL,NULL),(2356,'169.254.1.59',3,3,NULL,NULL),(2357,'169.254.1.60',3,3,NULL,NULL),(2358,'169.254.1.61',3,3,NULL,NULL),(2359,'169.254.1.62',3,3,NULL,NULL),(2360,'169.254.1.63',3,3,NULL,NULL),(2361,'169.254.1.64',3,3,NULL,NULL),(2362,'169.254.1.65',3,3,NULL,NULL),(2363,'169.254.1.66',3,3,NULL,NULL),(2364,'169.254.1.67',3,3,NULL,NULL),(2365,'169.254.1.68',3,3,NULL,NULL),(2366,'169.254.1.69',3,3,NULL,NULL),(2367,'169.254.1.70',3,3,NULL,NULL),(2368,'169.254.1.71',3,3,NULL,NULL),(2369,'169.254.1.72',3,3,NULL,NULL),(2370,'169.254.1.73',3,3,NULL,NULL),(2371,'169.254.1.74',3,3,NULL,NULL),(2372,'169.254.1.75',3,3,NULL,NULL),(2373,'169.254.1.76',3,3,NULL,NULL),(2374,'169.254.1.77',3,3,NULL,NULL),(2375,'169.254.1.78',3,3,NULL,NULL),(2376,'169.254.1.79',3,3,NULL,NULL),(2377,'169.254.1.80',3,3,NULL,NULL),(2378,'169.254.1.81',3,3,NULL,NULL),(2379,'169.254.1.82',3,3,NULL,NULL),(2380,'169.254.1.83',3,3,NULL,NULL),(2381,'169.254.1.84',3,3,NULL,NULL),(2382,'169.254.1.85',3,3,NULL,NULL),(2383,'169.254.1.86',3,3,NULL,NULL),(2384,'169.254.1.87',3,3,NULL,NULL),(2385,'169.254.1.88',3,3,NULL,NULL),(2386,'169.254.1.89',3,3,NULL,NULL),(2387,'169.254.1.90',3,3,NULL,NULL),(2388,'169.254.1.91',3,3,NULL,NULL),(2389,'169.254.1.92',3,3,NULL,NULL),(2390,'169.254.1.93',3,3,NULL,NULL),(2391,'169.254.1.94',3,3,NULL,NULL),(2392,'169.254.1.95',3,3,NULL,NULL),(2393,'169.254.1.96',3,3,NULL,NULL),(2394,'169.254.1.97',3,3,NULL,NULL),(2395,'169.254.1.98',3,3,NULL,NULL),(2396,'169.254.1.99',3,3,NULL,NULL),(2397,'169.254.1.100',3,3,NULL,NULL),(2398,'169.254.1.101',3,3,NULL,NULL),(2399,'169.254.1.102',3,3,NULL,NULL),(2400,'169.254.1.103',3,3,NULL,NULL),(2401,'169.254.1.104',3,3,NULL,NULL),(2402,'169.254.1.105',3,3,NULL,NULL),(2403,'169.254.1.106',3,3,NULL,NULL),(2404,'169.254.1.107',3,3,NULL,NULL),(2405,'169.254.1.108',3,3,NULL,NULL),(2406,'169.254.1.109',3,3,NULL,NULL),(2407,'169.254.1.110',3,3,NULL,NULL),(2408,'169.254.1.111',3,3,NULL,NULL),(2409,'169.254.1.112',3,3,NULL,NULL),(2410,'169.254.1.113',3,3,NULL,NULL),(2411,'169.254.1.114',3,3,NULL,NULL),(2412,'169.254.1.115',3,3,NULL,NULL),(2413,'169.254.1.116',3,3,NULL,NULL),(2414,'169.254.1.117',3,3,NULL,NULL),(2415,'169.254.1.118',3,3,NULL,NULL),(2416,'169.254.1.119',3,3,NULL,NULL),(2417,'169.254.1.120',3,3,NULL,NULL),(2418,'169.254.1.121',3,3,NULL,NULL),(2419,'169.254.1.122',3,3,NULL,NULL),(2420,'169.254.1.123',3,3,NULL,NULL),(2421,'169.254.1.124',3,3,NULL,NULL),(2422,'169.254.1.125',3,3,NULL,NULL),(2423,'169.254.1.126',3,3,NULL,NULL),(2424,'169.254.1.127',3,3,NULL,NULL),(2425,'169.254.1.128',3,3,NULL,NULL),(2426,'169.254.1.129',3,3,NULL,NULL),(2427,'169.254.1.130',3,3,NULL,NULL),(2428,'169.254.1.131',3,3,NULL,NULL),(2429,'169.254.1.132',3,3,NULL,NULL),(2430,'169.254.1.133',3,3,NULL,NULL),(2431,'169.254.1.134',3,3,NULL,NULL),(2432,'169.254.1.135',3,3,NULL,NULL),(2433,'169.254.1.136',3,3,NULL,NULL),(2434,'169.254.1.137',3,3,NULL,NULL),(2435,'169.254.1.138',3,3,NULL,NULL),(2436,'169.254.1.139',3,3,NULL,NULL),(2437,'169.254.1.140',3,3,NULL,NULL),(2438,'169.254.1.141',3,3,NULL,NULL),(2439,'169.254.1.142',3,3,NULL,NULL),(2440,'169.254.1.143',3,3,NULL,NULL),(2441,'169.254.1.144',3,3,NULL,NULL),(2442,'169.254.1.145',3,3,NULL,NULL),(2443,'169.254.1.146',3,3,NULL,NULL),(2444,'169.254.1.147',3,3,NULL,NULL),(2445,'169.254.1.148',3,3,NULL,NULL),(2446,'169.254.1.149',3,3,NULL,NULL),(2447,'169.254.1.150',3,3,NULL,NULL),(2448,'169.254.1.151',3,3,NULL,NULL),(2449,'169.254.1.152',3,3,NULL,NULL),(2450,'169.254.1.153',3,3,NULL,NULL),(2451,'169.254.1.154',3,3,NULL,NULL),(2452,'169.254.1.155',3,3,NULL,NULL),(2453,'169.254.1.156',3,3,NULL,NULL),(2454,'169.254.1.157',3,3,NULL,NULL),(2455,'169.254.1.158',3,3,NULL,NULL),(2456,'169.254.1.159',3,3,NULL,NULL),(2457,'169.254.1.160',3,3,NULL,NULL),(2458,'169.254.1.161',3,3,NULL,NULL),(2459,'169.254.1.162',3,3,NULL,NULL),(2460,'169.254.1.163',3,3,NULL,NULL),(2461,'169.254.1.164',3,3,NULL,NULL),(2462,'169.254.1.165',3,3,NULL,NULL),(2463,'169.254.1.166',3,3,NULL,NULL),(2464,'169.254.1.167',3,3,NULL,NULL),(2465,'169.254.1.168',3,3,NULL,NULL),(2466,'169.254.1.169',3,3,NULL,NULL),(2467,'169.254.1.170',3,3,NULL,NULL),(2468,'169.254.1.171',3,3,NULL,NULL),(2469,'169.254.1.172',3,3,NULL,NULL),(2470,'169.254.1.173',3,3,NULL,NULL),(2471,'169.254.1.174',3,3,NULL,NULL),(2472,'169.254.1.175',3,3,NULL,NULL),(2473,'169.254.1.176',3,3,NULL,NULL),(2474,'169.254.1.177',3,3,NULL,NULL),(2475,'169.254.1.178',3,3,NULL,NULL),(2476,'169.254.1.179',3,3,NULL,NULL),(2477,'169.254.1.180',3,3,NULL,NULL),(2478,'169.254.1.181',3,3,NULL,NULL),(2479,'169.254.1.182',3,3,NULL,NULL),(2480,'169.254.1.183',3,3,NULL,NULL),(2481,'169.254.1.184',3,3,NULL,NULL),(2482,'169.254.1.185',3,3,NULL,NULL),(2483,'169.254.1.186',3,3,NULL,NULL),(2484,'169.254.1.187',3,3,NULL,NULL),(2485,'169.254.1.188',3,3,NULL,NULL),(2486,'169.254.1.189',3,3,NULL,NULL),(2487,'169.254.1.190',3,3,NULL,NULL),(2488,'169.254.1.191',3,3,NULL,NULL),(2489,'169.254.1.192',3,3,NULL,NULL),(2490,'169.254.1.193',3,3,NULL,NULL),(2491,'169.254.1.194',3,3,NULL,NULL),(2492,'169.254.1.195',3,3,NULL,NULL),(2493,'169.254.1.196',3,3,NULL,NULL),(2494,'169.254.1.197',3,3,NULL,NULL),(2495,'169.254.1.198',3,3,NULL,NULL),(2496,'169.254.1.199',3,3,NULL,NULL),(2497,'169.254.1.200',3,3,NULL,NULL),(2498,'169.254.1.201',3,3,NULL,NULL),(2499,'169.254.1.202',3,3,NULL,NULL),(2500,'169.254.1.203',3,3,NULL,NULL),(2501,'169.254.1.204',3,3,NULL,NULL),(2502,'169.254.1.205',3,3,NULL,NULL),(2503,'169.254.1.206',3,3,NULL,NULL),(2504,'169.254.1.207',3,3,NULL,NULL),(2505,'169.254.1.208',3,3,NULL,NULL),(2506,'169.254.1.209',3,3,NULL,NULL),(2507,'169.254.1.210',3,3,NULL,NULL),(2508,'169.254.1.211',3,3,NULL,NULL),(2509,'169.254.1.212',3,3,NULL,NULL),(2510,'169.254.1.213',3,3,NULL,NULL),(2511,'169.254.1.214',3,3,NULL,NULL),(2512,'169.254.1.215',3,3,NULL,NULL),(2513,'169.254.1.216',3,3,NULL,NULL),(2514,'169.254.1.217',3,3,NULL,NULL),(2515,'169.254.1.218',3,3,NULL,NULL),(2516,'169.254.1.219',3,3,NULL,NULL),(2517,'169.254.1.220',3,3,NULL,NULL),(2518,'169.254.1.221',3,3,NULL,NULL),(2519,'169.254.1.222',3,3,NULL,NULL),(2520,'169.254.1.223',3,3,NULL,NULL),(2521,'169.254.1.224',3,3,NULL,NULL),(2522,'169.254.1.225',3,3,NULL,NULL),(2523,'169.254.1.226',3,3,NULL,NULL),(2524,'169.254.1.227',3,3,NULL,NULL),(2525,'169.254.1.228',3,3,NULL,NULL),(2526,'169.254.1.229',3,3,NULL,NULL),(2527,'169.254.1.230',3,3,NULL,NULL),(2528,'169.254.1.231',3,3,NULL,NULL),(2529,'169.254.1.232',3,3,NULL,NULL),(2530,'169.254.1.233',3,3,NULL,NULL),(2531,'169.254.1.234',3,3,NULL,NULL),(2532,'169.254.1.235',3,3,NULL,NULL),(2533,'169.254.1.236',3,3,NULL,NULL),(2534,'169.254.1.237',3,3,NULL,NULL),(2535,'169.254.1.238',3,3,NULL,NULL),(2536,'169.254.1.239',3,3,NULL,NULL),(2537,'169.254.1.240',3,3,NULL,NULL),(2538,'169.254.1.241',3,3,NULL,NULL),(2539,'169.254.1.242',3,3,NULL,NULL),(2540,'169.254.1.243',3,3,NULL,NULL),(2541,'169.254.1.244',3,3,NULL,NULL),(2542,'169.254.1.245',3,3,NULL,NULL),(2543,'169.254.1.246',3,3,NULL,NULL),(2544,'169.254.1.247',3,3,NULL,NULL),(2545,'169.254.1.248',3,3,NULL,NULL),(2546,'169.254.1.249',3,3,NULL,NULL),(2547,'169.254.1.250',3,3,NULL,NULL),(2548,'169.254.1.251',3,3,NULL,NULL),(2549,'169.254.1.252',3,3,NULL,NULL),(2550,'169.254.1.253',3,3,NULL,NULL),(2551,'169.254.1.254',3,3,NULL,NULL),(2552,'169.254.1.255',3,3,NULL,NULL),(2553,'169.254.2.0',3,3,NULL,NULL),(2554,'169.254.2.1',3,3,NULL,NULL),(2555,'169.254.2.2',3,3,NULL,NULL),(2556,'169.254.2.3',3,3,NULL,NULL),(2557,'169.254.2.4',3,3,NULL,NULL),(2558,'169.254.2.5',3,3,NULL,NULL),(2559,'169.254.2.6',3,3,NULL,NULL),(2560,'169.254.2.7',3,3,NULL,NULL),(2561,'169.254.2.8',3,3,NULL,NULL),(2562,'169.254.2.9',3,3,NULL,NULL),(2563,'169.254.2.10',3,3,NULL,NULL),(2564,'169.254.2.11',3,3,NULL,NULL),(2565,'169.254.2.12',3,3,NULL,NULL),(2566,'169.254.2.13',3,3,NULL,NULL),(2567,'169.254.2.14',3,3,NULL,NULL),(2568,'169.254.2.15',3,3,NULL,NULL),(2569,'169.254.2.16',3,3,NULL,NULL),(2570,'169.254.2.17',3,3,NULL,NULL),(2571,'169.254.2.18',3,3,NULL,NULL),(2572,'169.254.2.19',3,3,NULL,NULL),(2573,'169.254.2.20',3,3,NULL,NULL),(2574,'169.254.2.21',3,3,NULL,NULL),(2575,'169.254.2.22',3,3,NULL,NULL),(2576,'169.254.2.23',3,3,NULL,NULL),(2577,'169.254.2.24',3,3,NULL,NULL),(2578,'169.254.2.25',3,3,NULL,NULL),(2579,'169.254.2.26',3,3,NULL,NULL),(2580,'169.254.2.27',3,3,NULL,NULL),(2581,'169.254.2.28',3,3,NULL,NULL),(2582,'169.254.2.29',3,3,NULL,NULL),(2583,'169.254.2.30',3,3,NULL,NULL),(2584,'169.254.2.31',3,3,NULL,NULL),(2585,'169.254.2.32',3,3,NULL,NULL),(2586,'169.254.2.33',3,3,NULL,NULL),(2587,'169.254.2.34',3,3,NULL,NULL),(2588,'169.254.2.35',3,3,NULL,NULL),(2589,'169.254.2.36',3,3,NULL,NULL),(2590,'169.254.2.37',3,3,NULL,NULL),(2591,'169.254.2.38',3,3,NULL,NULL),(2592,'169.254.2.39',3,3,NULL,NULL),(2593,'169.254.2.40',3,3,NULL,NULL),(2594,'169.254.2.41',3,3,NULL,NULL),(2595,'169.254.2.42',3,3,NULL,NULL),(2596,'169.254.2.43',3,3,NULL,NULL),(2597,'169.254.2.44',3,3,NULL,NULL),(2598,'169.254.2.45',3,3,NULL,NULL),(2599,'169.254.2.46',3,3,NULL,NULL),(2600,'169.254.2.47',3,3,NULL,NULL),(2601,'169.254.2.48',3,3,NULL,NULL),(2602,'169.254.2.49',3,3,NULL,NULL),(2603,'169.254.2.50',3,3,NULL,NULL),(2604,'169.254.2.51',3,3,NULL,NULL),(2605,'169.254.2.52',3,3,NULL,NULL),(2606,'169.254.2.53',3,3,NULL,NULL),(2607,'169.254.2.54',3,3,NULL,NULL),(2608,'169.254.2.55',3,3,NULL,NULL),(2609,'169.254.2.56',3,3,NULL,NULL),(2610,'169.254.2.57',3,3,NULL,NULL),(2611,'169.254.2.58',3,3,NULL,NULL),(2612,'169.254.2.59',3,3,NULL,NULL),(2613,'169.254.2.60',3,3,NULL,NULL),(2614,'169.254.2.61',3,3,NULL,NULL),(2615,'169.254.2.62',3,3,NULL,NULL),(2616,'169.254.2.63',3,3,NULL,NULL),(2617,'169.254.2.64',3,3,NULL,NULL),(2618,'169.254.2.65',3,3,NULL,NULL),(2619,'169.254.2.66',3,3,NULL,NULL),(2620,'169.254.2.67',3,3,NULL,NULL),(2621,'169.254.2.68',3,3,NULL,NULL),(2622,'169.254.2.69',3,3,NULL,NULL),(2623,'169.254.2.70',3,3,NULL,NULL),(2624,'169.254.2.71',3,3,NULL,NULL),(2625,'169.254.2.72',3,3,NULL,NULL),(2626,'169.254.2.73',3,3,NULL,NULL),(2627,'169.254.2.74',3,3,NULL,NULL),(2628,'169.254.2.75',3,3,NULL,NULL),(2629,'169.254.2.76',3,3,NULL,NULL),(2630,'169.254.2.77',3,3,NULL,NULL),(2631,'169.254.2.78',3,3,NULL,NULL),(2632,'169.254.2.79',3,3,NULL,NULL),(2633,'169.254.2.80',3,3,NULL,NULL),(2634,'169.254.2.81',3,3,NULL,NULL),(2635,'169.254.2.82',3,3,NULL,NULL),(2636,'169.254.2.83',3,3,NULL,NULL),(2637,'169.254.2.84',3,3,NULL,NULL),(2638,'169.254.2.85',3,3,NULL,NULL),(2639,'169.254.2.86',3,3,NULL,NULL),(2640,'169.254.2.87',3,3,NULL,NULL),(2641,'169.254.2.88',3,3,NULL,NULL),(2642,'169.254.2.89',3,3,NULL,NULL),(2643,'169.254.2.90',3,3,NULL,NULL),(2644,'169.254.2.91',3,3,NULL,NULL),(2645,'169.254.2.92',3,3,NULL,NULL),(2646,'169.254.2.93',3,3,NULL,NULL),(2647,'169.254.2.94',3,3,NULL,NULL),(2648,'169.254.2.95',3,3,NULL,NULL),(2649,'169.254.2.96',3,3,NULL,NULL),(2650,'169.254.2.97',3,3,NULL,NULL),(2651,'169.254.2.98',3,3,NULL,NULL),(2652,'169.254.2.99',3,3,NULL,NULL),(2653,'169.254.2.100',3,3,NULL,NULL),(2654,'169.254.2.101',3,3,NULL,NULL),(2655,'169.254.2.102',3,3,2,'2011-03-17 00:58:25'),(2656,'169.254.2.103',3,3,NULL,NULL),(2657,'169.254.2.104',3,3,NULL,NULL),(2658,'169.254.2.105',3,3,NULL,NULL),(2659,'169.254.2.106',3,3,NULL,NULL),(2660,'169.254.2.107',3,3,NULL,NULL),(2661,'169.254.2.108',3,3,NULL,NULL),(2662,'169.254.2.109',3,3,NULL,NULL),(2663,'169.254.2.110',3,3,NULL,NULL),(2664,'169.254.2.111',3,3,NULL,NULL),(2665,'169.254.2.112',3,3,NULL,NULL),(2666,'169.254.2.113',3,3,NULL,NULL),(2667,'169.254.2.114',3,3,NULL,NULL),(2668,'169.254.2.115',3,3,NULL,NULL),(2669,'169.254.2.116',3,3,NULL,NULL),(2670,'169.254.2.117',3,3,NULL,NULL),(2671,'169.254.2.118',3,3,NULL,NULL),(2672,'169.254.2.119',3,3,NULL,NULL),(2673,'169.254.2.120',3,3,NULL,NULL),(2674,'169.254.2.121',3,3,NULL,NULL),(2675,'169.254.2.122',3,3,NULL,NULL),(2676,'169.254.2.123',3,3,NULL,NULL),(2677,'169.254.2.124',3,3,NULL,NULL),(2678,'169.254.2.125',3,3,NULL,NULL),(2679,'169.254.2.126',3,3,NULL,NULL),(2680,'169.254.2.127',3,3,NULL,NULL),(2681,'169.254.2.128',3,3,NULL,NULL),(2682,'169.254.2.129',3,3,NULL,NULL),(2683,'169.254.2.130',3,3,NULL,NULL),(2684,'169.254.2.131',3,3,NULL,NULL),(2685,'169.254.2.132',3,3,NULL,NULL),(2686,'169.254.2.133',3,3,NULL,NULL),(2687,'169.254.2.134',3,3,NULL,NULL),(2688,'169.254.2.135',3,3,NULL,NULL),(2689,'169.254.2.136',3,3,NULL,NULL),(2690,'169.254.2.137',3,3,NULL,NULL),(2691,'169.254.2.138',3,3,NULL,NULL),(2692,'169.254.2.139',3,3,NULL,NULL),(2693,'169.254.2.140',3,3,NULL,NULL),(2694,'169.254.2.141',3,3,NULL,NULL),(2695,'169.254.2.142',3,3,NULL,NULL),(2696,'169.254.2.143',3,3,NULL,NULL),(2697,'169.254.2.144',3,3,NULL,NULL),(2698,'169.254.2.145',3,3,NULL,NULL),(2699,'169.254.2.146',3,3,NULL,NULL),(2700,'169.254.2.147',3,3,NULL,NULL),(2701,'169.254.2.148',3,3,NULL,NULL),(2702,'169.254.2.149',3,3,NULL,NULL),(2703,'169.254.2.150',3,3,NULL,NULL),(2704,'169.254.2.151',3,3,NULL,NULL),(2705,'169.254.2.152',3,3,NULL,NULL),(2706,'169.254.2.153',3,3,NULL,NULL),(2707,'169.254.2.154',3,3,NULL,NULL),(2708,'169.254.2.155',3,3,NULL,NULL),(2709,'169.254.2.156',3,3,NULL,NULL),(2710,'169.254.2.157',3,3,NULL,NULL),(2711,'169.254.2.158',3,3,NULL,NULL),(2712,'169.254.2.159',3,3,NULL,NULL),(2713,'169.254.2.160',3,3,NULL,NULL),(2714,'169.254.2.161',3,3,NULL,NULL),(2715,'169.254.2.162',3,3,NULL,NULL),(2716,'169.254.2.163',3,3,NULL,NULL),(2717,'169.254.2.164',3,3,NULL,NULL),(2718,'169.254.2.165',3,3,NULL,NULL),(2719,'169.254.2.166',3,3,NULL,NULL),(2720,'169.254.2.167',3,3,NULL,NULL),(2721,'169.254.2.168',3,3,NULL,NULL),(2722,'169.254.2.169',3,3,NULL,NULL),(2723,'169.254.2.170',3,3,NULL,NULL),(2724,'169.254.2.171',3,3,NULL,NULL),(2725,'169.254.2.172',3,3,NULL,NULL),(2726,'169.254.2.173',3,3,NULL,NULL),(2727,'169.254.2.174',3,3,NULL,NULL),(2728,'169.254.2.175',3,3,NULL,NULL),(2729,'169.254.2.176',3,3,NULL,NULL),(2730,'169.254.2.177',3,3,NULL,NULL),(2731,'169.254.2.178',3,3,NULL,NULL),(2732,'169.254.2.179',3,3,NULL,NULL),(2733,'169.254.2.180',3,3,NULL,NULL),(2734,'169.254.2.181',3,3,NULL,NULL),(2735,'169.254.2.182',3,3,NULL,NULL),(2736,'169.254.2.183',3,3,NULL,NULL),(2737,'169.254.2.184',3,3,NULL,NULL),(2738,'169.254.2.185',3,3,NULL,NULL),(2739,'169.254.2.186',3,3,NULL,NULL),(2740,'169.254.2.187',3,3,NULL,NULL),(2741,'169.254.2.188',3,3,NULL,NULL),(2742,'169.254.2.189',3,3,NULL,NULL),(2743,'169.254.2.190',3,3,NULL,NULL),(2744,'169.254.2.191',3,3,NULL,NULL),(2745,'169.254.2.192',3,3,1,'2011-03-17 00:58:24'),(2746,'169.254.2.193',3,3,NULL,NULL),(2747,'169.254.2.194',3,3,NULL,NULL),(2748,'169.254.2.195',3,3,NULL,NULL),(2749,'169.254.2.196',3,3,NULL,NULL),(2750,'169.254.2.197',3,3,NULL,NULL),(2751,'169.254.2.198',3,3,NULL,NULL),(2752,'169.254.2.199',3,3,NULL,NULL),(2753,'169.254.2.200',3,3,NULL,NULL),(2754,'169.254.2.201',3,3,NULL,NULL),(2755,'169.254.2.202',3,3,NULL,NULL),(2756,'169.254.2.203',3,3,NULL,NULL),(2757,'169.254.2.204',3,3,NULL,NULL),(2758,'169.254.2.205',3,3,NULL,NULL),(2759,'169.254.2.206',3,3,NULL,NULL),(2760,'169.254.2.207',3,3,NULL,NULL),(2761,'169.254.2.208',3,3,NULL,NULL),(2762,'169.254.2.209',3,3,NULL,NULL),(2763,'169.254.2.210',3,3,NULL,NULL),(2764,'169.254.2.211',3,3,NULL,NULL),(2765,'169.254.2.212',3,3,NULL,NULL),(2766,'169.254.2.213',3,3,NULL,NULL),(2767,'169.254.2.214',3,3,NULL,NULL),(2768,'169.254.2.215',3,3,NULL,NULL),(2769,'169.254.2.216',3,3,NULL,NULL),(2770,'169.254.2.217',3,3,NULL,NULL),(2771,'169.254.2.218',3,3,NULL,NULL),(2772,'169.254.2.219',3,3,NULL,NULL),(2773,'169.254.2.220',3,3,NULL,NULL),(2774,'169.254.2.221',3,3,NULL,NULL),(2775,'169.254.2.222',3,3,NULL,NULL),(2776,'169.254.2.223',3,3,NULL,NULL),(2777,'169.254.2.224',3,3,NULL,NULL),(2778,'169.254.2.225',3,3,NULL,NULL),(2779,'169.254.2.226',3,3,NULL,NULL),(2780,'169.254.2.227',3,3,NULL,NULL),(2781,'169.254.2.228',3,3,NULL,NULL),(2782,'169.254.2.229',3,3,NULL,NULL),(2783,'169.254.2.230',3,3,NULL,NULL),(2784,'169.254.2.231',3,3,NULL,NULL),(2785,'169.254.2.232',3,3,NULL,NULL),(2786,'169.254.2.233',3,3,NULL,NULL),(2787,'169.254.2.234',3,3,NULL,NULL),(2788,'169.254.2.235',3,3,NULL,NULL),(2789,'169.254.2.236',3,3,NULL,NULL),(2790,'169.254.2.237',3,3,NULL,NULL),(2791,'169.254.2.238',3,3,NULL,NULL),(2792,'169.254.2.239',3,3,NULL,NULL),(2793,'169.254.2.240',3,3,NULL,NULL),(2794,'169.254.2.241',3,3,NULL,NULL),(2795,'169.254.2.242',3,3,NULL,NULL),(2796,'169.254.2.243',3,3,NULL,NULL),(2797,'169.254.2.244',3,3,NULL,NULL),(2798,'169.254.2.245',3,3,NULL,NULL),(2799,'169.254.2.246',3,3,NULL,NULL),(2800,'169.254.2.247',3,3,NULL,NULL),(2801,'169.254.2.248',3,3,NULL,NULL),(2802,'169.254.2.249',3,3,NULL,NULL),(2803,'169.254.2.250',3,3,NULL,NULL),(2804,'169.254.2.251',3,3,NULL,NULL),(2805,'169.254.2.252',3,3,NULL,NULL),(2806,'169.254.2.253',3,3,NULL,NULL),(2807,'169.254.2.254',3,3,NULL,NULL),(2808,'169.254.2.255',3,3,NULL,NULL),(2809,'169.254.3.0',3,3,NULL,NULL),(2810,'169.254.3.1',3,3,NULL,NULL),(2811,'169.254.3.2',3,3,NULL,NULL),(2812,'169.254.3.3',3,3,NULL,NULL),(2813,'169.254.3.4',3,3,NULL,NULL),(2814,'169.254.3.5',3,3,NULL,NULL),(2815,'169.254.3.6',3,3,NULL,NULL),(2816,'169.254.3.7',3,3,NULL,NULL),(2817,'169.254.3.8',3,3,NULL,NULL),(2818,'169.254.3.9',3,3,NULL,NULL),(2819,'169.254.3.10',3,3,NULL,NULL),(2820,'169.254.3.11',3,3,NULL,NULL),(2821,'169.254.3.12',3,3,NULL,NULL),(2822,'169.254.3.13',3,3,NULL,NULL),(2823,'169.254.3.14',3,3,NULL,NULL),(2824,'169.254.3.15',3,3,NULL,NULL),(2825,'169.254.3.16',3,3,NULL,NULL),(2826,'169.254.3.17',3,3,NULL,NULL),(2827,'169.254.3.18',3,3,NULL,NULL),(2828,'169.254.3.19',3,3,NULL,NULL),(2829,'169.254.3.20',3,3,NULL,NULL),(2830,'169.254.3.21',3,3,NULL,NULL),(2831,'169.254.3.22',3,3,NULL,NULL),(2832,'169.254.3.23',3,3,NULL,NULL),(2833,'169.254.3.24',3,3,NULL,NULL),(2834,'169.254.3.25',3,3,NULL,NULL),(2835,'169.254.3.26',3,3,NULL,NULL),(2836,'169.254.3.27',3,3,NULL,NULL),(2837,'169.254.3.28',3,3,NULL,NULL),(2838,'169.254.3.29',3,3,NULL,NULL),(2839,'169.254.3.30',3,3,NULL,NULL),(2840,'169.254.3.31',3,3,NULL,NULL),(2841,'169.254.3.32',3,3,NULL,NULL),(2842,'169.254.3.33',3,3,NULL,NULL),(2843,'169.254.3.34',3,3,NULL,NULL),(2844,'169.254.3.35',3,3,NULL,NULL),(2845,'169.254.3.36',3,3,NULL,NULL),(2846,'169.254.3.37',3,3,NULL,NULL),(2847,'169.254.3.38',3,3,NULL,NULL),(2848,'169.254.3.39',3,3,NULL,NULL),(2849,'169.254.3.40',3,3,NULL,NULL),(2850,'169.254.3.41',3,3,NULL,NULL),(2851,'169.254.3.42',3,3,NULL,NULL),(2852,'169.254.3.43',3,3,NULL,NULL),(2853,'169.254.3.44',3,3,NULL,NULL),(2854,'169.254.3.45',3,3,4,'2011-03-17 01:00:16'),(2855,'169.254.3.46',3,3,NULL,NULL),(2856,'169.254.3.47',3,3,NULL,NULL),(2857,'169.254.3.48',3,3,NULL,NULL),(2858,'169.254.3.49',3,3,NULL,NULL),(2859,'169.254.3.50',3,3,NULL,NULL),(2860,'169.254.3.51',3,3,NULL,NULL),(2861,'169.254.3.52',3,3,NULL,NULL),(2862,'169.254.3.53',3,3,NULL,NULL),(2863,'169.254.3.54',3,3,NULL,NULL),(2864,'169.254.3.55',3,3,NULL,NULL),(2865,'169.254.3.56',3,3,NULL,NULL),(2866,'169.254.3.57',3,3,NULL,NULL),(2867,'169.254.3.58',3,3,NULL,NULL),(2868,'169.254.3.59',3,3,NULL,NULL),(2869,'169.254.3.60',3,3,NULL,NULL),(2870,'169.254.3.61',3,3,NULL,NULL),(2871,'169.254.3.62',3,3,NULL,NULL),(2872,'169.254.3.63',3,3,NULL,NULL),(2873,'169.254.3.64',3,3,NULL,NULL),(2874,'169.254.3.65',3,3,NULL,NULL),(2875,'169.254.3.66',3,3,NULL,NULL),(2876,'169.254.3.67',3,3,NULL,NULL),(2877,'169.254.3.68',3,3,NULL,NULL),(2878,'169.254.3.69',3,3,NULL,NULL),(2879,'169.254.3.70',3,3,NULL,NULL),(2880,'169.254.3.71',3,3,NULL,NULL),(2881,'169.254.3.72',3,3,NULL,NULL),(2882,'169.254.3.73',3,3,NULL,NULL),(2883,'169.254.3.74',3,3,NULL,NULL),(2884,'169.254.3.75',3,3,NULL,NULL),(2885,'169.254.3.76',3,3,NULL,NULL),(2886,'169.254.3.77',3,3,NULL,NULL),(2887,'169.254.3.78',3,3,NULL,NULL),(2888,'169.254.3.79',3,3,NULL,NULL),(2889,'169.254.3.80',3,3,NULL,NULL),(2890,'169.254.3.81',3,3,NULL,NULL),(2891,'169.254.3.82',3,3,NULL,NULL),(2892,'169.254.3.83',3,3,NULL,NULL),(2893,'169.254.3.84',3,3,NULL,NULL),(2894,'169.254.3.85',3,3,NULL,NULL),(2895,'169.254.3.86',3,3,NULL,NULL),(2896,'169.254.3.87',3,3,NULL,NULL),(2897,'169.254.3.88',3,3,NULL,NULL),(2898,'169.254.3.89',3,3,NULL,NULL),(2899,'169.254.3.90',3,3,NULL,NULL),(2900,'169.254.3.91',3,3,NULL,NULL),(2901,'169.254.3.92',3,3,NULL,NULL),(2902,'169.254.3.93',3,3,NULL,NULL),(2903,'169.254.3.94',3,3,NULL,NULL),(2904,'169.254.3.95',3,3,NULL,NULL),(2905,'169.254.3.96',3,3,NULL,NULL),(2906,'169.254.3.97',3,3,NULL,NULL),(2907,'169.254.3.98',3,3,NULL,NULL),(2908,'169.254.3.99',3,3,NULL,NULL),(2909,'169.254.3.100',3,3,NULL,NULL),(2910,'169.254.3.101',3,3,NULL,NULL),(2911,'169.254.3.102',3,3,NULL,NULL),(2912,'169.254.3.103',3,3,NULL,NULL),(2913,'169.254.3.104',3,3,NULL,NULL),(2914,'169.254.3.105',3,3,NULL,NULL),(2915,'169.254.3.106',3,3,NULL,NULL),(2916,'169.254.3.107',3,3,NULL,NULL),(2917,'169.254.3.108',3,3,NULL,NULL),(2918,'169.254.3.109',3,3,NULL,NULL),(2919,'169.254.3.110',3,3,NULL,NULL),(2920,'169.254.3.111',3,3,NULL,NULL),(2921,'169.254.3.112',3,3,NULL,NULL),(2922,'169.254.3.113',3,3,NULL,NULL),(2923,'169.254.3.114',3,3,NULL,NULL),(2924,'169.254.3.115',3,3,NULL,NULL),(2925,'169.254.3.116',3,3,NULL,NULL),(2926,'169.254.3.117',3,3,NULL,NULL),(2927,'169.254.3.118',3,3,NULL,NULL),(2928,'169.254.3.119',3,3,NULL,NULL),(2929,'169.254.3.120',3,3,NULL,NULL),(2930,'169.254.3.121',3,3,NULL,NULL),(2931,'169.254.3.122',3,3,NULL,NULL),(2932,'169.254.3.123',3,3,NULL,NULL),(2933,'169.254.3.124',3,3,NULL,NULL),(2934,'169.254.3.125',3,3,NULL,NULL),(2935,'169.254.3.126',3,3,NULL,NULL),(2936,'169.254.3.127',3,3,NULL,NULL),(2937,'169.254.3.128',3,3,NULL,NULL),(2938,'169.254.3.129',3,3,NULL,NULL),(2939,'169.254.3.130',3,3,NULL,NULL),(2940,'169.254.3.131',3,3,NULL,NULL),(2941,'169.254.3.132',3,3,NULL,NULL),(2942,'169.254.3.133',3,3,NULL,NULL),(2943,'169.254.3.134',3,3,NULL,NULL),(2944,'169.254.3.135',3,3,NULL,NULL),(2945,'169.254.3.136',3,3,NULL,NULL),(2946,'169.254.3.137',3,3,NULL,NULL),(2947,'169.254.3.138',3,3,NULL,NULL),(2948,'169.254.3.139',3,3,NULL,NULL),(2949,'169.254.3.140',3,3,NULL,NULL),(2950,'169.254.3.141',3,3,NULL,NULL),(2951,'169.254.3.142',3,3,NULL,NULL),(2952,'169.254.3.143',3,3,NULL,NULL),(2953,'169.254.3.144',3,3,NULL,NULL),(2954,'169.254.3.145',3,3,NULL,NULL),(2955,'169.254.3.146',3,3,NULL,NULL),(2956,'169.254.3.147',3,3,NULL,NULL),(2957,'169.254.3.148',3,3,NULL,NULL),(2958,'169.254.3.149',3,3,NULL,NULL),(2959,'169.254.3.150',3,3,NULL,NULL),(2960,'169.254.3.151',3,3,NULL,NULL),(2961,'169.254.3.152',3,3,NULL,NULL),(2962,'169.254.3.153',3,3,NULL,NULL),(2963,'169.254.3.154',3,3,NULL,NULL),(2964,'169.254.3.155',3,3,NULL,NULL),(2965,'169.254.3.156',3,3,NULL,NULL),(2966,'169.254.3.157',3,3,NULL,NULL),(2967,'169.254.3.158',3,3,NULL,NULL),(2968,'169.254.3.159',3,3,NULL,NULL),(2969,'169.254.3.160',3,3,NULL,NULL),(2970,'169.254.3.161',3,3,NULL,NULL),(2971,'169.254.3.162',3,3,NULL,NULL),(2972,'169.254.3.163',3,3,NULL,NULL),(2973,'169.254.3.164',3,3,NULL,NULL),(2974,'169.254.3.165',3,3,NULL,NULL),(2975,'169.254.3.166',3,3,NULL,NULL),(2976,'169.254.3.167',3,3,NULL,NULL),(2977,'169.254.3.168',3,3,NULL,NULL),(2978,'169.254.3.169',3,3,NULL,NULL),(2979,'169.254.3.170',3,3,NULL,NULL),(2980,'169.254.3.171',3,3,NULL,NULL),(2981,'169.254.3.172',3,3,NULL,NULL),(2982,'169.254.3.173',3,3,NULL,NULL),(2983,'169.254.3.174',3,3,NULL,NULL),(2984,'169.254.3.175',3,3,NULL,NULL),(2985,'169.254.3.176',3,3,NULL,NULL),(2986,'169.254.3.177',3,3,NULL,NULL),(2987,'169.254.3.178',3,3,NULL,NULL),(2988,'169.254.3.179',3,3,NULL,NULL),(2989,'169.254.3.180',3,3,NULL,NULL),(2990,'169.254.3.181',3,3,NULL,NULL),(2991,'169.254.3.182',3,3,NULL,NULL),(2992,'169.254.3.183',3,3,NULL,NULL),(2993,'169.254.3.184',3,3,NULL,NULL),(2994,'169.254.3.185',3,3,NULL,NULL),(2995,'169.254.3.186',3,3,NULL,NULL),(2996,'169.254.3.187',3,3,NULL,NULL),(2997,'169.254.3.188',3,3,NULL,NULL),(2998,'169.254.3.189',3,3,NULL,NULL),(2999,'169.254.3.190',3,3,NULL,NULL),(3000,'169.254.3.191',3,3,NULL,NULL),(3001,'169.254.3.192',3,3,NULL,NULL),(3002,'169.254.3.193',3,3,NULL,NULL),(3003,'169.254.3.194',3,3,NULL,NULL),(3004,'169.254.3.195',3,3,NULL,NULL),(3005,'169.254.3.196',3,3,NULL,NULL),(3006,'169.254.3.197',3,3,NULL,NULL),(3007,'169.254.3.198',3,3,NULL,NULL),(3008,'169.254.3.199',3,3,NULL,NULL),(3009,'169.254.3.200',3,3,NULL,NULL),(3010,'169.254.3.201',3,3,NULL,NULL),(3011,'169.254.3.202',3,3,NULL,NULL),(3012,'169.254.3.203',3,3,NULL,NULL),(3013,'169.254.3.204',3,3,NULL,NULL),(3014,'169.254.3.205',3,3,NULL,NULL),(3015,'169.254.3.206',3,3,NULL,NULL),(3016,'169.254.3.207',3,3,NULL,NULL),(3017,'169.254.3.208',3,3,NULL,NULL),(3018,'169.254.3.209',3,3,NULL,NULL),(3019,'169.254.3.210',3,3,NULL,NULL),(3020,'169.254.3.211',3,3,NULL,NULL),(3021,'169.254.3.212',3,3,NULL,NULL),(3022,'169.254.3.213',3,3,NULL,NULL),(3023,'169.254.3.214',3,3,NULL,NULL),(3024,'169.254.3.215',3,3,NULL,NULL),(3025,'169.254.3.216',3,3,NULL,NULL),(3026,'169.254.3.217',3,3,NULL,NULL),(3027,'169.254.3.218',3,3,NULL,NULL),(3028,'169.254.3.219',3,3,NULL,NULL),(3029,'169.254.3.220',3,3,NULL,NULL),(3030,'169.254.3.221',3,3,NULL,NULL),(3031,'169.254.3.222',3,3,NULL,NULL),(3032,'169.254.3.223',3,3,NULL,NULL),(3033,'169.254.3.224',3,3,NULL,NULL),(3034,'169.254.3.225',3,3,NULL,NULL),(3035,'169.254.3.226',3,3,NULL,NULL),(3036,'169.254.3.227',3,3,NULL,NULL),(3037,'169.254.3.228',3,3,NULL,NULL),(3038,'169.254.3.229',3,3,NULL,NULL),(3039,'169.254.3.230',3,3,NULL,NULL),(3040,'169.254.3.231',3,3,NULL,NULL),(3041,'169.254.3.232',3,3,NULL,NULL),(3042,'169.254.3.233',3,3,NULL,NULL),(3043,'169.254.3.234',3,3,NULL,NULL),(3044,'169.254.3.235',3,3,NULL,NULL),(3045,'169.254.3.236',3,3,NULL,NULL),(3046,'169.254.3.237',3,3,NULL,NULL),(3047,'169.254.3.238',3,3,NULL,NULL),(3048,'169.254.3.239',3,3,NULL,NULL),(3049,'169.254.3.240',3,3,NULL,NULL),(3050,'169.254.3.241',3,3,NULL,NULL),(3051,'169.254.3.242',3,3,NULL,NULL),(3052,'169.254.3.243',3,3,NULL,NULL),(3053,'169.254.3.244',3,3,NULL,NULL),(3054,'169.254.3.245',3,3,NULL,NULL),(3055,'169.254.3.246',3,3,NULL,NULL),(3056,'169.254.3.247',3,3,NULL,NULL),(3057,'169.254.3.248',3,3,NULL,NULL),(3058,'169.254.3.249',3,3,NULL,NULL),(3059,'169.254.3.250',3,3,NULL,NULL),(3060,'169.254.3.251',3,3,NULL,NULL),(3061,'169.254.3.252',3,3,NULL,NULL),(3062,'169.254.3.253',3,3,NULL,NULL),(3063,'169.254.3.254',3,3,NULL,NULL);
/*!40000 ALTER TABLE `op_dc_link_local_ip_address_alloc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `op_dc_vnet_alloc`
--

DROP TABLE IF EXISTS `op_dc_vnet_alloc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `op_dc_vnet_alloc` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary id',
  `vnet` varchar(18) NOT NULL COMMENT 'vnet',
  `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'data center the vnet belongs to',
  `account_id` bigint(20) unsigned DEFAULT NULL COMMENT 'account the vnet belongs to right now',
  `taken` datetime DEFAULT NULL COMMENT 'Date taken',
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_op_dc_vnet_alloc__vnet__data_center_id` (`vnet`,`data_center_id`),
  UNIQUE KEY `i_op_dc_vnet_alloc__vnet__data_center_id__account_id` (`vnet`,`data_center_id`,`account_id`),
  KEY `i_op_dc_vnet_alloc__dc_taken` (`data_center_id`,`taken`)
) ENGINE=InnoDB AUTO_INCREMENT=65004 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `op_dc_vnet_alloc`
--

LOCK TABLES `op_dc_vnet_alloc` WRITE;
/*!40000 ALTER TABLE `op_dc_vnet_alloc` DISABLE KEYS */;
INSERT INTO `op_dc_vnet_alloc` VALUES (64903,'500',3,2,'2011-03-17 01:00:16'),(64904,'501',3,NULL,NULL),(64905,'502',3,NULL,NULL),(64906,'503',3,NULL,NULL),(64907,'504',3,NULL,NULL),(64908,'505',3,NULL,NULL),(64909,'506',3,NULL,NULL),(64910,'507',3,NULL,NULL),(64911,'508',3,NULL,NULL),(64912,'509',3,NULL,NULL),(64913,'510',3,NULL,NULL),(64914,'511',3,NULL,NULL),(64915,'512',3,NULL,NULL),(64916,'513',3,NULL,NULL),(64917,'514',3,NULL,NULL),(64918,'515',3,NULL,NULL),(64919,'516',3,NULL,NULL),(64920,'517',3,NULL,NULL),(64921,'518',3,NULL,NULL),(64922,'519',3,NULL,NULL),(64923,'520',3,NULL,NULL),(64924,'521',3,NULL,NULL),(64925,'522',3,NULL,NULL),(64926,'523',3,NULL,NULL),(64927,'524',3,NULL,NULL),(64928,'525',3,NULL,NULL),(64929,'526',3,NULL,NULL),(64930,'527',3,NULL,NULL),(64931,'528',3,NULL,NULL),(64932,'529',3,NULL,NULL),(64933,'530',3,NULL,NULL),(64934,'531',3,NULL,NULL),(64935,'532',3,NULL,NULL),(64936,'533',3,NULL,NULL),(64937,'534',3,NULL,NULL),(64938,'535',3,NULL,NULL),(64939,'536',3,NULL,NULL),(64940,'537',3,NULL,NULL),(64941,'538',3,NULL,NULL),(64942,'539',3,NULL,NULL),(64943,'540',3,NULL,NULL),(64944,'541',3,NULL,NULL),(64945,'542',3,NULL,NULL),(64946,'543',3,NULL,NULL),(64947,'544',3,NULL,NULL),(64948,'545',3,NULL,NULL),(64949,'546',3,NULL,NULL),(64950,'547',3,NULL,NULL),(64951,'548',3,NULL,NULL),(64952,'549',3,NULL,NULL),(64953,'550',3,NULL,NULL),(64954,'551',3,NULL,NULL),(64955,'552',3,NULL,NULL),(64956,'553',3,NULL,NULL),(64957,'554',3,NULL,NULL),(64958,'555',3,NULL,NULL),(64959,'556',3,NULL,NULL),(64960,'557',3,NULL,NULL),(64961,'558',3,NULL,NULL),(64962,'559',3,NULL,NULL),(64963,'560',3,NULL,NULL),(64964,'561',3,NULL,NULL),(64965,'562',3,NULL,NULL),(64966,'563',3,NULL,NULL),(64967,'564',3,NULL,NULL),(64968,'565',3,NULL,NULL),(64969,'566',3,NULL,NULL),(64970,'567',3,NULL,NULL),(64971,'568',3,NULL,NULL),(64972,'569',3,NULL,NULL),(64973,'570',3,NULL,NULL),(64974,'571',3,NULL,NULL),(64975,'572',3,NULL,NULL),(64976,'573',3,NULL,NULL),(64977,'574',3,NULL,NULL),(64978,'575',3,NULL,NULL),(64979,'576',3,NULL,NULL),(64980,'577',3,NULL,NULL),(64981,'578',3,NULL,NULL),(64982,'579',3,NULL,NULL),(64983,'580',3,NULL,NULL),(64984,'581',3,NULL,NULL),(64985,'582',3,NULL,NULL),(64986,'583',3,NULL,NULL),(64987,'584',3,NULL,NULL),(64988,'585',3,NULL,NULL),(64989,'586',3,NULL,NULL),(64990,'587',3,NULL,NULL),(64991,'588',3,NULL,NULL),(64992,'589',3,NULL,NULL),(64993,'590',3,NULL,NULL),(64994,'591',3,NULL,NULL),(64995,'592',3,NULL,NULL),(64996,'593',3,NULL,NULL),(64997,'594',3,NULL,NULL),(64998,'595',3,NULL,NULL),(64999,'596',3,NULL,NULL),(65000,'597',3,NULL,NULL),(65001,'598',3,NULL,NULL),(65002,'599',3,NULL,NULL),(65003,'600',3,NULL,NULL);
/*!40000 ALTER TABLE `op_dc_vnet_alloc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `op_ha_work`
--

DROP TABLE IF EXISTS `op_ha_work`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `op_ha_work` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint(20) unsigned NOT NULL COMMENT 'vm instance that needs to be ha.',
  `type` varchar(32) NOT NULL COMMENT 'type of work',
  `vm_type` varchar(32) NOT NULL COMMENT 'VM type',
  `state` varchar(32) NOT NULL COMMENT 'state of the vm instance when this happened.',
  `mgmt_server_id` bigint(20) unsigned DEFAULT NULL COMMENT 'management server that has taken up the work of doing ha',
  `host_id` bigint(20) unsigned DEFAULT NULL COMMENT 'host that the vm is suppose to be on',
  `created` datetime NOT NULL COMMENT 'time the entry was requested',
  `tried` int(10) unsigned DEFAULT NULL COMMENT '# of times tried',
  `taken` datetime DEFAULT NULL COMMENT 'time it was taken by the management server',
  `step` varchar(32) NOT NULL COMMENT 'Step in the work',
  `time_to_try` bigint(20) DEFAULT NULL COMMENT 'time to try do this work',
  `updated` bigint(20) unsigned NOT NULL COMMENT 'time the VM state was updated when it was stored into work queue',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `i_op_ha_work__instance_id` (`instance_id`),
  KEY `i_op_ha_work__host_id` (`host_id`),
  KEY `i_op_ha_work__step` (`step`),
  KEY `i_op_ha_work__type` (`type`),
  KEY `i_op_ha_work__mgmt_server_id` (`mgmt_server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `op_ha_work`
--

LOCK TABLES `op_ha_work` WRITE;
/*!40000 ALTER TABLE `op_ha_work` DISABLE KEYS */;
/*!40000 ALTER TABLE `op_ha_work` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `op_host_capacity`
--

DROP TABLE IF EXISTS `op_host_capacity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `op_host_capacity` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `host_id` bigint(20) unsigned DEFAULT NULL,
  `data_center_id` bigint(20) unsigned NOT NULL,
  `pod_id` bigint(20) unsigned DEFAULT NULL,
  `used_capacity` bigint(20) unsigned NOT NULL,
  `total_capacity` bigint(20) unsigned NOT NULL,
  `capacity_type` int(1) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `i_op_host_capacity__host_type` (`host_id`,`capacity_type`),
  KEY `i_op_host_capacity__pod_id` (`pod_id`),
  KEY `i_op_host_capacity__data_center_id` (`data_center_id`),
  CONSTRAINT `fk_op_host_capacity__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_op_host_capacity__pod_id` FOREIGN KEY (`pod_id`) REFERENCES `host_pod_ref` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `op_host_capacity`
--

LOCK TABLES `op_host_capacity` WRITE;
/*!40000 ALTER TABLE `op_host_capacity` DISABLE KEYS */;
INSERT INTO `op_host_capacity` VALUES (3,200,3,3,579281321984,984519770112,2),(4,200,3,3,27431904256,1969039540224,3),(9,1,3,3,3160065703936,3910488817664,6),(14,2,3,3,2550136832,15998459904,0),(15,2,3,3,1000,10000,1),(16,NULL,3,NULL,3,124,4),(17,NULL,3,3,2,11,5);
/*!40000 ALTER TABLE `op_host_capacity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `op_host_upgrade`
--

DROP TABLE IF EXISTS `op_host_upgrade`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `op_host_upgrade` (
  `host_id` bigint(20) unsigned NOT NULL COMMENT 'host id',
  `version` varchar(20) NOT NULL COMMENT 'version',
  `state` varchar(20) NOT NULL COMMENT 'state',
  PRIMARY KEY (`host_id`),
  UNIQUE KEY `host_id` (`host_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `op_host_upgrade`
--

LOCK TABLES `op_host_upgrade` WRITE;
/*!40000 ALTER TABLE `op_host_upgrade` DISABLE KEYS */;
/*!40000 ALTER TABLE `op_host_upgrade` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `op_lock`
--

DROP TABLE IF EXISTS `op_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `op_lock` (
  `key` varchar(128) NOT NULL COMMENT 'primary key of the table',
  `mac` varchar(17) NOT NULL COMMENT 'mac address of who acquired this lock',
  `ip` varchar(15) NOT NULL COMMENT 'ip address of who acquired this lock',
  `thread` varchar(255) NOT NULL COMMENT 'Thread that acquired this lock',
  `acquired_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Time acquired',
  `waiters` int(11) NOT NULL DEFAULT '0' COMMENT 'How many have waited for this',
  PRIMARY KEY (`key`),
  UNIQUE KEY `key` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `op_lock`
--

LOCK TABLES `op_lock` WRITE;
/*!40000 ALTER TABLE `op_lock` DISABLE KEYS */;
/*!40000 ALTER TABLE `op_lock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `op_nwgrp_work`
--

DROP TABLE IF EXISTS `op_nwgrp_work`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `op_nwgrp_work` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint(20) unsigned NOT NULL COMMENT 'vm instance that needs rules to be synced.',
  `mgmt_server_id` bigint(20) unsigned DEFAULT NULL COMMENT 'management server that has taken up the work of doing rule sync',
  `created` datetime NOT NULL COMMENT 'time the entry was requested',
  `taken` datetime DEFAULT NULL COMMENT 'time it was taken by the management server',
  `step` varchar(32) NOT NULL COMMENT 'Step in the work',
  `seq_no` bigint(20) unsigned DEFAULT NULL COMMENT 'seq number to be sent to agent, uniquely identifies ruleset update',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `i_op_nwgrp_work__instance_id` (`instance_id`),
  KEY `i_op_nwgrp_work__mgmt_server_id` (`mgmt_server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `op_nwgrp_work`
--

LOCK TABLES `op_nwgrp_work` WRITE;
/*!40000 ALTER TABLE `op_nwgrp_work` DISABLE KEYS */;
/*!40000 ALTER TABLE `op_nwgrp_work` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `op_pod_vlan_alloc`
--

DROP TABLE IF EXISTS `op_pod_vlan_alloc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `op_pod_vlan_alloc` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary id',
  `vlan` varchar(18) NOT NULL COMMENT 'vlan id',
  `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'data center the pod belongs to',
  `pod_id` bigint(20) unsigned NOT NULL COMMENT 'pod the vlan belongs to',
  `account_id` bigint(20) unsigned DEFAULT NULL COMMENT 'account the vlan belongs to right now',
  `taken` datetime DEFAULT NULL COMMENT 'Date taken',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `op_pod_vlan_alloc`
--

LOCK TABLES `op_pod_vlan_alloc` WRITE;
/*!40000 ALTER TABLE `op_pod_vlan_alloc` DISABLE KEYS */;
/*!40000 ALTER TABLE `op_pod_vlan_alloc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `op_vm_host`
--

DROP TABLE IF EXISTS `op_vm_host`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `op_vm_host` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'foreign key to host_id',
  `vnc_ports` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT 'vnc ports open on the host',
  `start_at` int(5) unsigned NOT NULL DEFAULT '0' COMMENT 'Start the vnc port look up at this bit',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  CONSTRAINT `fk_op_vm_host__id` FOREIGN KEY (`id`) REFERENCES `host` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `op_vm_host`
--

LOCK TABLES `op_vm_host` WRITE;
/*!40000 ALTER TABLE `op_vm_host` DISABLE KEYS */;
/*!40000 ALTER TABLE `op_vm_host` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `op_vm_ruleset_log`
--

DROP TABLE IF EXISTS `op_vm_ruleset_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `op_vm_ruleset_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint(20) unsigned NOT NULL COMMENT 'vm instance that needs rules to be synced.',
  `created` datetime NOT NULL COMMENT 'time the entry was requested',
  `logsequence` bigint(20) unsigned DEFAULT NULL COMMENT 'seq number to be sent to agent, uniquely identifies ruleset update',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `op_vm_ruleset_log`
--

LOCK TABLES `op_vm_ruleset_log` WRITE;
/*!40000 ALTER TABLE `op_vm_ruleset_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `op_vm_ruleset_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pod_vlan_map`
--

DROP TABLE IF EXISTS `pod_vlan_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pod_vlan_map` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pod_id` bigint(20) unsigned NOT NULL COMMENT 'pod id. foreign key to pod table',
  `vlan_db_id` bigint(20) unsigned NOT NULL COMMENT 'database id of vlan. foreign key to vlan table',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `i_pod_vlan_map__pod_id` (`pod_id`),
  KEY `i_pod_vlan_map__vlan_id` (`vlan_db_id`),
  CONSTRAINT `fk_pod_vlan_map__pod_id` FOREIGN KEY (`pod_id`) REFERENCES `host_pod_ref` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pod_vlan_map__vlan_id` FOREIGN KEY (`vlan_db_id`) REFERENCES `vlan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pod_vlan_map`
--

LOCK TABLES `pod_vlan_map` WRITE;
/*!40000 ALTER TABLE `pod_vlan_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `pod_vlan_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pricing`
--

DROP TABLE IF EXISTS `pricing`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pricing` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `price` float unsigned NOT NULL,
  `price_unit` varchar(45) NOT NULL,
  `type` varchar(255) NOT NULL,
  `type_id` int(10) unsigned DEFAULT NULL,
  `created` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pricing`
--

LOCK TABLES `pricing` WRITE;
/*!40000 ALTER TABLE `pricing` DISABLE KEYS */;
/*!40000 ALTER TABLE `pricing` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resource_count`
--

DROP TABLE IF EXISTS `resource_count`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_count` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `account_id` bigint(20) unsigned NOT NULL,
  `type` varchar(255) DEFAULT NULL,
  `count` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resource_count`
--

LOCK TABLES `resource_count` WRITE;
/*!40000 ALTER TABLE `resource_count` DISABLE KEYS */;
INSERT INTO `resource_count` VALUES (1,2,'public_ip',1),(2,2,'user_vm',2),(3,2,'volume',2);
/*!40000 ALTER TABLE `resource_count` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resource_limit`
--

DROP TABLE IF EXISTS `resource_limit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_limit` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `domain_id` bigint(20) unsigned DEFAULT NULL,
  `account_id` bigint(20) unsigned DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `max` bigint(20) NOT NULL DEFAULT '-1',
  PRIMARY KEY (`id`),
  KEY `i_resource_limit__domain_id` (`domain_id`),
  KEY `i_resource_limit__account_id` (`account_id`),
  CONSTRAINT `fk_resource_limit__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_resource_limit__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resource_limit`
--

LOCK TABLES `resource_limit` WRITE;
/*!40000 ALTER TABLE `resource_limit` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_limit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `secondary_storage_vm`
--

DROP TABLE IF EXISTS `secondary_storage_vm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `secondary_storage_vm` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `gateway` varchar(15) DEFAULT NULL COMMENT 'gateway info for this sec storage vm towards public network interface',
  `dns1` varchar(15) DEFAULT NULL COMMENT 'dns1',
  `dns2` varchar(15) DEFAULT NULL COMMENT 'dns2',
  `domain` varchar(255) DEFAULT NULL COMMENT 'domain',
  `public_mac_address` varchar(17) NOT NULL COMMENT 'mac address of the public facing network card',
  `public_ip_address` varchar(15) DEFAULT NULL COMMENT 'public ip address for the sec storage vm',
  `public_netmask` varchar(15) DEFAULT NULL COMMENT 'public netmask used for the sec storage vm',
  `guest_mac_address` varchar(17) NOT NULL COMMENT 'mac address of the guest facing network card',
  `guest_ip_address` varchar(15) DEFAULT NULL COMMENT 'guest ip address for the console proxy',
  `guest_netmask` varchar(15) DEFAULT NULL COMMENT 'guest netmask used for the console proxy',
  `vlan_db_id` bigint(20) unsigned DEFAULT NULL COMMENT 'Foreign key into vlan id table',
  `vlan_id` varchar(255) DEFAULT NULL COMMENT 'optional VLAN ID for sec storage vm that can be used',
  `ram_size` int(10) unsigned NOT NULL DEFAULT '512' COMMENT 'memory to use in mb',
  `guid` varchar(255) NOT NULL COMMENT 'copied from guid of secondary storage host',
  `nfs_share` varchar(255) NOT NULL COMMENT 'server and path exported by the nfs server ',
  `last_update` datetime DEFAULT NULL COMMENT 'Last session update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `public_mac_address` (`public_mac_address`),
  UNIQUE KEY `guest_mac_address` (`guest_mac_address`),
  UNIQUE KEY `public_ip_address` (`public_ip_address`),
  UNIQUE KEY `guest_ip_address` (`guest_ip_address`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `secondary_storage_vm`
--

LOCK TABLES `secondary_storage_vm` WRITE;
/*!40000 ALTER TABLE `secondary_storage_vm` DISABLE KEYS */;
INSERT INTO `secondary_storage_vm` VALUES (1,'172.24.0.1','192.168.10.253',NULL,'foo.com','06:83:ed:ac:00:01','172.24.0.120','255.255.0.0','06:03:f0:25:00:02','169.254.2.192','255.255.0.0',1,'24',256,'nfs://192.168.153.15/export/secondary-storage-stress-2.1.x','nfs://192.168.153.15/export/secondary-storage-stress-2.1.x',NULL);
/*!40000 ALTER TABLE `secondary_storage_vm` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `security_group`
--

DROP TABLE IF EXISTS `security_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `security_group` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096) DEFAULT NULL,
  `domain_id` bigint(20) unsigned DEFAULT NULL,
  `account_id` bigint(20) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `security_group`
--

LOCK TABLES `security_group` WRITE;
/*!40000 ALTER TABLE `security_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `security_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `security_group_vm_map`
--

DROP TABLE IF EXISTS `security_group_vm_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `security_group_vm_map` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `security_group_id` bigint(20) unsigned NOT NULL,
  `ip_address` varchar(15) NOT NULL,
  `instance_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `security_group_vm_map`
--

LOCK TABLES `security_group_vm_map` WRITE;
/*!40000 ALTER TABLE `security_group_vm_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `security_group_vm_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sequence`
--

DROP TABLE IF EXISTS `sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sequence` (
  `name` varchar(64) NOT NULL COMMENT 'name of the sequence',
  `value` bigint(20) unsigned NOT NULL COMMENT 'sequence value',
  PRIMARY KEY (`name`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sequence`
--

LOCK TABLES `sequence` WRITE;
/*!40000 ALTER TABLE `sequence` DISABLE KEYS */;
INSERT INTO `sequence` VALUES ('private_mac_address_seq',1),('public_mac_address_seq',1),('storage_pool_seq',201),('vm_instance_seq',6),('vm_template_seq',201);
/*!40000 ALTER TABLE `sequence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_offering`
--

DROP TABLE IF EXISTS `service_offering`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `service_offering` (
  `id` bigint(20) unsigned NOT NULL,
  `cpu` int(10) unsigned NOT NULL COMMENT '# of cores',
  `speed` int(10) unsigned NOT NULL COMMENT 'speed per core in mhz',
  `ram_size` bigint(20) unsigned NOT NULL,
  `nw_rate` smallint(5) unsigned DEFAULT '200' COMMENT 'network rate throttle mbits/s',
  `mc_rate` smallint(5) unsigned DEFAULT '10' COMMENT 'mcast rate throttle mbits/s',
  `ha_enabled` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT 'Enable HA',
  `guest_ip_type` varchar(255) NOT NULL DEFAULT 'Virtualized' COMMENT 'Type of guest network -- direct or virtualized',
  `host_tag` varchar(255) DEFAULT NULL COMMENT 'host tag specified by the service_offering',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_service_offering__id` FOREIGN KEY (`id`) REFERENCES `disk_offering` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_offering`
--

LOCK TABLES `service_offering` WRITE;
/*!40000 ALTER TABLE `service_offering` DISABLE KEYS */;
INSERT INTO `service_offering` VALUES (1,1,500,512,200,10,0,'Virtualized',NULL),(2,1,2000,2048,200,10,0,'Virtualized',NULL),(3,2,2000,4096,200,10,0,'Virtualized',NULL),(7,1,500,512,200,10,0,'Virtualized',NULL),(8,1,1000,1024,200,10,0,'Virtualized',NULL),(9,1,500,512,200,10,0,'DirectSingle',NULL),(10,1,1000,1024,200,10,0,'DirectSingle',NULL),(14,1,0,128,0,0,0,'Virtualized',NULL),(15,1,0,256,0,0,0,'Virtualized',NULL),(16,1,0,1024,0,0,0,'Virtualized',NULL);
/*!40000 ALTER TABLE `service_offering` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `snapshot_policy`
--

DROP TABLE IF EXISTS `snapshot_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `snapshot_policy` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volume_id` bigint(20) unsigned NOT NULL,
  `schedule` varchar(100) NOT NULL COMMENT 'schedule time of execution',
  `timezone` varchar(100) NOT NULL COMMENT 'the timezone in which the schedule time is specified',
  `interval` int(4) NOT NULL DEFAULT '4' COMMENT 'backup schedule, e.g. hourly, daily, etc.',
  `max_snaps` int(8) NOT NULL DEFAULT '0' COMMENT 'maximum number of snapshots to maintain',
  `active` tinyint(1) unsigned NOT NULL COMMENT 'Is the policy active',
  PRIMARY KEY (`id`),
  KEY `i_snapshot_policy__volume_id` (`volume_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `snapshot_policy`
--

LOCK TABLES `snapshot_policy` WRITE;
/*!40000 ALTER TABLE `snapshot_policy` DISABLE KEYS */;
INSERT INTO `snapshot_policy` VALUES (1,0,'00','GMT',4,0,1);
/*!40000 ALTER TABLE `snapshot_policy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `snapshot_policy_ref`
--

DROP TABLE IF EXISTS `snapshot_policy_ref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `snapshot_policy_ref` (
  `snap_id` bigint(20) unsigned NOT NULL,
  `volume_id` bigint(20) unsigned NOT NULL,
  `policy_id` bigint(20) unsigned NOT NULL,
  UNIQUE KEY `snap_id` (`snap_id`,`policy_id`),
  KEY `i_snapshot_policy_ref__snap_id` (`snap_id`),
  KEY `i_snapshot_policy_ref__volume_id` (`volume_id`),
  KEY `i_snapshot_policy_ref__policy_id` (`policy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `snapshot_policy_ref`
--

LOCK TABLES `snapshot_policy_ref` WRITE;
/*!40000 ALTER TABLE `snapshot_policy_ref` DISABLE KEYS */;
/*!40000 ALTER TABLE `snapshot_policy_ref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `snapshot_schedule`
--

DROP TABLE IF EXISTS `snapshot_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `snapshot_schedule` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volume_id` bigint(20) unsigned NOT NULL COMMENT 'The volume for which this snapshot is being taken',
  `policy_id` bigint(20) unsigned NOT NULL COMMENT 'One of the policyIds for which this snapshot was taken',
  `scheduled_timestamp` datetime NOT NULL COMMENT 'Time at which the snapshot was scheduled for execution',
  `async_job_id` bigint(20) unsigned DEFAULT NULL COMMENT 'If this schedule is being executed, it is the id of the create aysnc_job. Before that it is null',
  `snapshot_id` bigint(20) unsigned DEFAULT NULL COMMENT 'If this schedule is being executed, then the corresponding snapshot has this id. Before that it is null',
  PRIMARY KEY (`id`),
  UNIQUE KEY `volume_id` (`volume_id`,`policy_id`),
  KEY `i_snapshot_schedule__volume_id` (`volume_id`),
  KEY `i_snapshot_schedule__policy_id` (`policy_id`),
  KEY `i_snapshot_schedule__async_job_id` (`async_job_id`),
  KEY `i_snapshot_schedule__snapshot_id` (`snapshot_id`),
  KEY `i_snapshot_schedule__scheduled_timestamp` (`scheduled_timestamp`),
  CONSTRAINT `fk__snapshot_schedule_async_job_id` FOREIGN KEY (`async_job_id`) REFERENCES `async_job` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk__snapshot_schedule_policy_id` FOREIGN KEY (`policy_id`) REFERENCES `snapshot_policy` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk__snapshot_schedule_snapshot_id` FOREIGN KEY (`snapshot_id`) REFERENCES `snapshots` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk__snapshot_schedule_volume_id` FOREIGN KEY (`volume_id`) REFERENCES `volumes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `snapshot_schedule`
--

LOCK TABLES `snapshot_schedule` WRITE;
/*!40000 ALTER TABLE `snapshot_schedule` DISABLE KEYS */;
/*!40000 ALTER TABLE `snapshot_schedule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `snapshots`
--

DROP TABLE IF EXISTS `snapshots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `snapshots` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `account_id` bigint(20) unsigned NOT NULL COMMENT 'owner.  foreign key to account table',
  `volume_id` bigint(20) unsigned NOT NULL COMMENT 'volume it belongs to. foreign key to volume table',
  `status` varchar(32) DEFAULT NULL COMMENT 'snapshot creation status',
  `path` varchar(255) DEFAULT NULL COMMENT 'Path',
  `name` varchar(255) NOT NULL COMMENT 'snapshot name',
  `snapshot_type` int(4) NOT NULL COMMENT 'type of snapshot, e.g. manual, recurring',
  `type_description` varchar(25) DEFAULT NULL COMMENT 'description of the type of snapshot, e.g. manual, recurring',
  `created` datetime DEFAULT NULL COMMENT 'Date Created',
  `removed` datetime DEFAULT NULL COMMENT 'Date removed.  not null if removed',
  `backup_snap_id` varchar(255) DEFAULT NULL COMMENT 'Back up uuid of the snapshot',
  `prev_snap_id` bigint(20) unsigned DEFAULT NULL COMMENT 'Id of the most recent snapshot',
  PRIMARY KEY (`id`),
  KEY `i_snapshots__account_id` (`account_id`),
  KEY `i_snapshots__volume_id` (`volume_id`),
  KEY `i_snapshots__removed` (`removed`),
  KEY `i_snapshots__name` (`name`),
  KEY `i_snapshots__snapshot_type` (`snapshot_type`),
  KEY `i_snapshots__prev_snap_id` (`prev_snap_id`),
  CONSTRAINT `fk_snapshots__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `snapshots`
--

LOCK TABLES `snapshots` WRITE;
/*!40000 ALTER TABLE `snapshots` DISABLE KEYS */;
/*!40000 ALTER TABLE `snapshots` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stack_maid`
--

DROP TABLE IF EXISTS `stack_maid`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stack_maid` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `msid` bigint(20) unsigned NOT NULL,
  `thread_id` bigint(20) unsigned NOT NULL,
  `seq` int(10) unsigned NOT NULL,
  `cleanup_delegate` varchar(128) DEFAULT NULL,
  `cleanup_context` text,
  `created` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `i_stack_maid_msid_thread_id` (`msid`,`thread_id`),
  KEY `i_stack_maid_seq` (`msid`,`seq`),
  KEY `i_stack_maid_created` (`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stack_maid`
--

LOCK TABLES `stack_maid` WRITE;
/*!40000 ALTER TABLE `stack_maid` DISABLE KEYS */;
/*!40000 ALTER TABLE `stack_maid` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `storage_pool`
--

DROP TABLE IF EXISTS `storage_pool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `storage_pool` (
  `id` bigint(20) unsigned NOT NULL,
  `name` varchar(255) DEFAULT NULL COMMENT 'should be NOT NULL',
  `uuid` varchar(255) DEFAULT NULL,
  `pool_type` varchar(32) NOT NULL,
  `port` int(10) unsigned NOT NULL,
  `data_center_id` bigint(20) unsigned NOT NULL,
  `pod_id` bigint(20) unsigned DEFAULT NULL,
  `cluster_id` bigint(20) unsigned DEFAULT NULL COMMENT 'foreign key to cluster',
  `available_bytes` bigint(20) unsigned DEFAULT NULL,
  `capacity_bytes` bigint(20) unsigned DEFAULT NULL,
  `host_address` varchar(255) NOT NULL COMMENT 'FQDN or IP of storage server',
  `path` varchar(255) NOT NULL COMMENT 'Filesystem path that is shared',
  `created` datetime DEFAULT NULL COMMENT 'date the pool created',
  `removed` datetime DEFAULT NULL COMMENT 'date removed if not null',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  KEY `i_storage_pool__pod_id` (`pod_id`),
  KEY `fk_storage_pool__cluster_id` (`cluster_id`),
  CONSTRAINT `fk_storage_pool__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster` (`id`),
  CONSTRAINT `fk_storage_pool__pod_id` FOREIGN KEY (`pod_id`) REFERENCES `host_pod_ref` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `storage_pool`
--

LOCK TABLES `storage_pool` WRITE;
/*!40000 ALTER TABLE `storage_pool` DISABLE KEYS */;
INSERT INTO `storage_pool` VALUES (200,'primary','1697c352-f325-3d62-acf0-7fa84f7d3b37','NetworkFilesystem',2049,3,3,1,405238448128,984519770112,'192.168.160.20','/export/alena-primary','2011-03-17 00:56:57',NULL,NULL);
/*!40000 ALTER TABLE `storage_pool` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `storage_pool_details`
--

DROP TABLE IF EXISTS `storage_pool_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `storage_pool_details` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `pool_id` bigint(20) unsigned NOT NULL COMMENT 'pool the detail is related to',
  `name` varchar(255) NOT NULL COMMENT 'name of the detail',
  `value` varchar(255) NOT NULL COMMENT 'value of the detail',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fk_storage_pool_details__pool_id` (`pool_id`),
  KEY `i_storage_pool_details__name__value` (`name`,`value`),
  CONSTRAINT `fk_storage_pool_details__pool_id` FOREIGN KEY (`pool_id`) REFERENCES `storage_pool` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `storage_pool_details`
--

LOCK TABLES `storage_pool_details` WRITE;
/*!40000 ALTER TABLE `storage_pool_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `storage_pool_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `storage_pool_host_ref`
--

DROP TABLE IF EXISTS `storage_pool_host_ref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `storage_pool_host_ref` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `host_id` bigint(20) unsigned NOT NULL,
  `pool_id` bigint(20) unsigned NOT NULL,
  `created` datetime NOT NULL,
  `last_updated` datetime DEFAULT NULL,
  `local_path` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `storage_pool_host_ref`
--

LOCK TABLES `storage_pool_host_ref` WRITE;
/*!40000 ALTER TABLE `storage_pool_host_ref` DISABLE KEYS */;
INSERT INTO `storage_pool_host_ref` VALUES (1,2,200,'2011-03-17 00:57:05',NULL,'/mnt//1697c352-f325-3d62-acf0-7fa84f7d3b37');
/*!40000 ALTER TABLE `storage_pool_host_ref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sync_queue`
--

DROP TABLE IF EXISTS `sync_queue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sync_queue` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `sync_objtype` varchar(64) NOT NULL,
  `sync_objid` bigint(20) unsigned NOT NULL,
  `queue_proc_msid` bigint(20) DEFAULT NULL,
  `queue_proc_number` bigint(20) DEFAULT NULL COMMENT 'process number, increase 1 for each iteration',
  `queue_proc_time` datetime DEFAULT NULL COMMENT 'last time to process the queue',
  `created` datetime DEFAULT NULL COMMENT 'date created',
  `last_updated` datetime DEFAULT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_sync_queue__objtype__objid` (`sync_objtype`,`sync_objid`),
  KEY `i_sync_queue__created` (`created`),
  KEY `i_sync_queue__last_updated` (`last_updated`),
  KEY `i_sync_queue__queue_proc_time` (`queue_proc_time`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sync_queue`
--

LOCK TABLES `sync_queue` WRITE;
/*!40000 ALTER TABLE `sync_queue` DISABLE KEYS */;
INSERT INTO `sync_queue` VALUES (1,'Router',4,120890037145,4,NULL,'2011-03-17 01:04:12','2011-03-17 01:04:34');
/*!40000 ALTER TABLE `sync_queue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sync_queue_item`
--

DROP TABLE IF EXISTS `sync_queue_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sync_queue_item` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `queue_id` bigint(20) unsigned NOT NULL,
  `content_type` varchar(64) DEFAULT NULL,
  `content_id` bigint(20) DEFAULT NULL,
  `queue_proc_msid` bigint(20) DEFAULT NULL COMMENT 'owner msid when the queue item is being processed',
  `queue_proc_number` bigint(20) DEFAULT NULL COMMENT 'used to distinguish raw items and items being in process',
  `created` datetime DEFAULT NULL COMMENT 'time created',
  PRIMARY KEY (`id`),
  KEY `i_sync_queue_item__queue_id` (`queue_id`),
  KEY `i_sync_queue_item__created` (`created`),
  CONSTRAINT `fk_sync_queue_item__queue_id` FOREIGN KEY (`queue_id`) REFERENCES `sync_queue` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sync_queue_item`
--

LOCK TABLES `sync_queue_item` WRITE;
/*!40000 ALTER TABLE `sync_queue_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `sync_queue_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `template_host_ref`
--

DROP TABLE IF EXISTS `template_host_ref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `template_host_ref` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `host_id` bigint(20) unsigned NOT NULL,
  `pool_id` bigint(20) unsigned DEFAULT NULL,
  `template_id` bigint(20) unsigned NOT NULL,
  `created` datetime NOT NULL,
  `last_updated` datetime DEFAULT NULL,
  `job_id` varchar(255) DEFAULT NULL,
  `download_pct` int(10) unsigned DEFAULT NULL,
  `size` bigint(20) unsigned DEFAULT NULL,
  `download_state` varchar(255) DEFAULT NULL,
  `error_str` varchar(255) DEFAULT NULL,
  `local_path` varchar(255) DEFAULT NULL,
  `install_path` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `destroyed` tinyint(1) DEFAULT NULL COMMENT 'indicates whether the template_host entry was destroyed by the user or not',
  `is_copy` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'indicates whether this was copied ',
  PRIMARY KEY (`id`),
  KEY `i_template_host_ref__host_id` (`host_id`),
  KEY `i_template_host_ref__template_id` (`template_id`),
  CONSTRAINT `fk_template_host_ref__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_template_host_ref__template_id` FOREIGN KEY (`template_id`) REFERENCES `vm_template` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `template_host_ref`
--

LOCK TABLES `template_host_ref` WRITE;
/*!40000 ALTER TABLE `template_host_ref` DISABLE KEYS */;
INSERT INTO `template_host_ref` VALUES (1,1,NULL,1,'2011-03-17 00:56:49','2011-03-17 00:58:58',NULL,100,2147483648,'DOWNLOADED',NULL,NULL,'template/tmpl/1/1//5e421480-b37d-4044-9e3b-a2e1cb58da8a.vhd','http://download.cloud.com/releases/2.0.0RC5/systemvm.vhd.bz2',0,0),(2,1,NULL,2,'2011-03-17 00:56:49','2011-03-17 00:58:58',NULL,100,8589934592,'DOWNLOADED','',NULL,'template/tmpl/1/2//350e1981-61db-35fe-990d-b2f566a52030.vhd','http://download.cloud.com/templates/builtin/f59f18fb-ae94-4f97-afd2-f84755767aca.vhd.bz2',0,0),(3,2,NULL,200,'2011-03-17 00:56:56','2011-03-17 00:56:56',NULL,100,66023424,'DOWNLOADED',NULL,NULL,'iso/users/2/xs-tools',NULL,0,0);
/*!40000 ALTER TABLE `template_host_ref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `template_spool_ref`
--

DROP TABLE IF EXISTS `template_spool_ref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `template_spool_ref` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pool_id` bigint(20) unsigned NOT NULL,
  `template_id` bigint(20) unsigned NOT NULL,
  `created` datetime NOT NULL,
  `last_updated` datetime DEFAULT NULL,
  `job_id` varchar(255) DEFAULT NULL,
  `download_pct` int(10) unsigned DEFAULT NULL,
  `download_state` varchar(255) DEFAULT NULL,
  `error_str` varchar(255) DEFAULT NULL,
  `local_path` varchar(255) DEFAULT NULL,
  `install_path` varchar(255) DEFAULT NULL,
  `template_size` bigint(20) unsigned NOT NULL COMMENT 'the size of the template on the pool',
  `marked_for_gc` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT 'if true, the garbage collector will evict the template from this pool.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_template_spool_ref__template_id__pool_id` (`template_id`,`pool_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `template_spool_ref`
--

LOCK TABLES `template_spool_ref` WRITE;
/*!40000 ALTER TABLE `template_spool_ref` DISABLE KEYS */;
INSERT INTO `template_spool_ref` VALUES (1,200,1,'2011-03-17 00:57:29',NULL,NULL,0,'DOWNLOADED',NULL,'07b85b67-a300-4374-af94-8e9eb22fe77d','07b85b67-a300-4374-af94-8e9eb22fe77d',2101252608,0),(2,200,2,'2011-03-17 00:59:30',NULL,NULL,0,'DOWNLOADED',NULL,'8e47d22b-8829-4f99-a974-0e2ceebcab8a','8e47d22b-8829-4f99-a974-0e2ceebcab8a',1708331520,0);
/*!40000 ALTER TABLE `template_spool_ref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `template_zone_ref`
--

DROP TABLE IF EXISTS `template_zone_ref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `template_zone_ref` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `zone_id` bigint(20) unsigned NOT NULL,
  `template_id` bigint(20) unsigned NOT NULL,
  `created` datetime NOT NULL,
  `last_updated` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL COMMENT 'date removed if not null',
  PRIMARY KEY (`id`),
  KEY `i_template_zone_ref__zone_id` (`zone_id`),
  KEY `i_template_zone_ref__template_id` (`template_id`),
  CONSTRAINT `fk_template_zone_ref__template_id` FOREIGN KEY (`template_id`) REFERENCES `vm_template` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_template_zone_ref__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `template_zone_ref`
--

LOCK TABLES `template_zone_ref` WRITE;
/*!40000 ALTER TABLE `template_zone_ref` DISABLE KEYS */;
INSERT INTO `template_zone_ref` VALUES (1,3,1,'2011-03-17 00:56:49','2011-03-17 00:56:49',NULL),(2,3,2,'2011-03-17 00:56:49','2011-03-17 00:56:49',NULL);
/*!40000 ALTER TABLE `template_zone_ref` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `firstname` varchar(255) DEFAULT NULL,
  `lastname` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `state` varchar(10) NOT NULL DEFAULT 'enabled',
  `api_key` varchar(255) DEFAULT NULL,
  `secret_key` varchar(255) DEFAULT NULL,
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime DEFAULT NULL COMMENT 'date removed',
  `timezone` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_user__api_key` (`api_key`),
  KEY `i_user__secret_key_removed` (`secret_key`,`removed`),
  KEY `i_user__removed` (`removed`),
  KEY `i_user__account_id` (`account_id`),
  CONSTRAINT `fk_user__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'system','',1,'system','cloud',NULL,'enabled',NULL,NULL,'2011-03-16 17:56:02',NULL,NULL),(2,'admin','5f4dcc3b5aa765d61d8327deb882cf99',2,'Admin','User','admin@mailprovider.com','enabled',NULL,NULL,'2011-03-16 17:56:02',NULL,NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_ip_address`
--

DROP TABLE IF EXISTS `user_ip_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_ip_address` (
  `account_id` bigint(20) unsigned DEFAULT NULL,
  `domain_id` bigint(20) unsigned DEFAULT NULL,
  `public_ip_address` varchar(15) NOT NULL,
  `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'zone that it belongs to',
  `source_nat` int(1) unsigned NOT NULL DEFAULT '0',
  `allocated` datetime DEFAULT NULL COMMENT 'Date this ip was allocated to someone',
  `vlan_db_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`public_ip_address`),
  UNIQUE KEY `public_ip_address` (`public_ip_address`),
  KEY `i_user_ip_address__account_id` (`account_id`),
  KEY `i_user_ip_address__vlan_db_id` (`vlan_db_id`),
  KEY `i_user_ip_address__data_center_id` (`data_center_id`),
  KEY `i_user_ip_address__source_nat` (`source_nat`),
  KEY `i_user_ip_address__allocated` (`allocated`),
  KEY `i_user_ip_address__public_ip_address` (`public_ip_address`),
  CONSTRAINT `fk_user_ip_address__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_user_ip_address__vlan_db_id` FOREIGN KEY (`vlan_db_id`) REFERENCES `vlan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_ip_address`
--

LOCK TABLES `user_ip_address` WRITE;
/*!40000 ALTER TABLE `user_ip_address` DISABLE KEYS */;
INSERT INTO `user_ip_address` VALUES (NULL,NULL,'172.24.0.111',3,0,NULL,1),(NULL,NULL,'172.24.0.112',3,0,NULL,1),(NULL,NULL,'172.24.0.113',3,0,NULL,1),(NULL,NULL,'172.24.0.114',3,0,NULL,1),(NULL,NULL,'172.24.0.115',3,0,NULL,1),(NULL,NULL,'172.24.0.116',3,0,NULL,1),(NULL,NULL,'172.24.0.117',3,0,NULL,1),(NULL,NULL,'172.24.0.118',3,0,NULL,1),(NULL,NULL,'172.24.0.119',3,0,NULL,1),(1,1,'172.24.0.120',3,1,'2011-03-17 00:57:29',1),(NULL,NULL,'172.24.0.121',3,0,NULL,1),(NULL,NULL,'172.24.0.122',3,0,NULL,1),(NULL,NULL,'172.24.0.123',3,0,NULL,1),(NULL,NULL,'172.24.0.124',3,0,NULL,1),(NULL,NULL,'172.24.0.125',3,0,NULL,1),(NULL,NULL,'172.24.0.126',3,0,NULL,1),(NULL,NULL,'172.24.0.127',3,0,NULL,1),(NULL,NULL,'172.24.0.128',3,0,NULL,1),(NULL,NULL,'172.24.0.129',3,0,NULL,1),(NULL,NULL,'172.24.0.130',3,0,NULL,1),(NULL,NULL,'172.24.0.131',3,0,NULL,1),(NULL,NULL,'172.24.0.132',3,0,NULL,1),(NULL,NULL,'172.24.0.133',3,0,NULL,1),(NULL,NULL,'172.24.0.134',3,0,NULL,1),(NULL,NULL,'172.24.0.135',3,0,NULL,1),(NULL,NULL,'172.24.0.136',3,0,NULL,1),(NULL,NULL,'172.24.0.137',3,0,NULL,1),(NULL,NULL,'172.24.0.138',3,0,NULL,1),(NULL,NULL,'172.24.0.139',3,0,NULL,1),(2,1,'172.24.0.140',3,1,'2011-03-17 00:59:29',1),(1,1,'172.24.0.141',3,1,'2011-03-17 00:57:29',1),(NULL,NULL,'172.24.0.142',3,0,NULL,1),(NULL,NULL,'172.24.0.143',3,0,NULL,1),(NULL,NULL,'172.24.0.144',3,0,NULL,1),(NULL,NULL,'172.24.0.145',3,0,NULL,1),(NULL,NULL,'172.24.0.146',3,0,NULL,1),(NULL,NULL,'172.24.0.147',3,0,NULL,1),(NULL,NULL,'172.24.0.148',3,0,NULL,1),(NULL,NULL,'172.24.0.149',3,0,NULL,1),(NULL,NULL,'172.24.0.150',3,0,NULL,1),(NULL,NULL,'172.24.0.151',3,0,NULL,1),(NULL,NULL,'172.24.0.152',3,0,NULL,1),(NULL,NULL,'172.24.0.153',3,0,NULL,1),(NULL,NULL,'172.24.0.154',3,0,NULL,1),(NULL,NULL,'172.24.0.155',3,0,NULL,1),(NULL,NULL,'172.24.0.156',3,0,NULL,1),(NULL,NULL,'172.24.0.157',3,0,NULL,1),(NULL,NULL,'172.24.0.158',3,0,NULL,1),(NULL,NULL,'172.24.0.159',3,0,NULL,1),(NULL,NULL,'172.24.0.160',3,0,NULL,1),(NULL,NULL,'172.24.0.161',3,0,NULL,1),(NULL,NULL,'172.24.0.162',3,0,NULL,1),(NULL,NULL,'172.24.0.163',3,0,NULL,1),(NULL,NULL,'172.24.0.164',3,0,NULL,1),(NULL,NULL,'172.24.0.165',3,0,NULL,1),(NULL,NULL,'172.24.0.166',3,0,NULL,1),(NULL,NULL,'172.24.0.167',3,0,NULL,1),(NULL,NULL,'172.24.0.168',3,0,NULL,1),(NULL,NULL,'172.24.0.169',3,0,NULL,1),(NULL,NULL,'172.24.0.170',3,0,NULL,1),(NULL,NULL,'172.24.0.171',3,0,NULL,1),(NULL,NULL,'172.24.0.172',3,0,NULL,1),(NULL,NULL,'172.24.0.173',3,0,NULL,1),(NULL,NULL,'172.24.0.174',3,0,NULL,1),(NULL,NULL,'172.24.0.175',3,0,NULL,1),(NULL,NULL,'172.24.0.176',3,0,NULL,1),(NULL,NULL,'172.24.0.177',3,0,NULL,1),(NULL,NULL,'172.24.0.178',3,0,NULL,1),(NULL,NULL,'172.24.0.179',3,0,NULL,1),(NULL,NULL,'172.24.0.180',3,0,NULL,1),(NULL,NULL,'172.24.0.181',3,0,NULL,1),(NULL,NULL,'172.24.0.182',3,0,NULL,1),(NULL,NULL,'172.24.0.183',3,0,NULL,1),(NULL,NULL,'172.24.0.184',3,0,NULL,1),(NULL,NULL,'172.24.0.185',3,0,NULL,1),(NULL,NULL,'172.24.0.186',3,0,NULL,1),(NULL,NULL,'172.24.0.187',3,0,NULL,1),(NULL,NULL,'172.24.0.188',3,0,NULL,1),(NULL,NULL,'172.24.0.189',3,0,NULL,1),(NULL,NULL,'172.24.0.190',3,0,NULL,1),(NULL,NULL,'172.24.0.191',3,0,NULL,1),(NULL,NULL,'172.24.0.192',3,0,NULL,1),(NULL,NULL,'172.24.0.193',3,0,NULL,1),(NULL,NULL,'172.24.0.194',3,0,NULL,1),(NULL,NULL,'172.24.0.195',3,0,NULL,1),(NULL,NULL,'172.24.0.196',3,0,NULL,1),(NULL,NULL,'172.24.0.197',3,0,NULL,1),(NULL,NULL,'172.24.0.198',3,0,NULL,1),(NULL,NULL,'172.24.0.199',3,0,NULL,1),(NULL,NULL,'172.24.0.200',3,0,NULL,1),(NULL,NULL,'172.24.0.201',3,0,NULL,1),(NULL,NULL,'172.24.0.202',3,0,NULL,1),(NULL,NULL,'172.24.0.203',3,0,NULL,1),(NULL,NULL,'172.24.0.204',3,0,NULL,1),(NULL,NULL,'172.24.0.205',3,0,NULL,1),(NULL,NULL,'172.24.0.206',3,0,NULL,1),(NULL,NULL,'172.24.0.207',3,0,NULL,1),(NULL,NULL,'172.24.0.208',3,0,NULL,1),(NULL,NULL,'172.24.0.209',3,0,NULL,1),(NULL,NULL,'172.24.0.210',3,0,NULL,1),(NULL,NULL,'172.24.0.211',3,0,NULL,1),(NULL,NULL,'172.24.0.212',3,0,NULL,1),(NULL,NULL,'172.24.0.213',3,0,NULL,1),(NULL,NULL,'172.24.0.214',3,0,NULL,1),(NULL,NULL,'172.24.0.215',3,0,NULL,1),(NULL,NULL,'172.24.0.216',3,0,NULL,1),(NULL,NULL,'172.24.0.217',3,0,NULL,1),(NULL,NULL,'172.24.0.218',3,0,NULL,1),(NULL,NULL,'172.24.0.219',3,0,NULL,1),(NULL,NULL,'172.24.0.220',3,0,NULL,1),(NULL,NULL,'172.24.0.221',3,0,NULL,1),(NULL,NULL,'172.24.0.222',3,0,NULL,1),(NULL,NULL,'172.24.0.223',3,0,NULL,1),(NULL,NULL,'172.24.0.224',3,0,NULL,1),(NULL,NULL,'172.24.0.225',3,0,NULL,1),(NULL,NULL,'172.24.0.226',3,0,NULL,1),(NULL,NULL,'172.24.0.227',3,0,NULL,1),(NULL,NULL,'172.24.0.228',3,0,NULL,1),(NULL,NULL,'172.24.0.229',3,0,NULL,1),(NULL,NULL,'172.24.0.230',3,0,NULL,1),(NULL,NULL,'172.24.0.231',3,0,NULL,1),(NULL,NULL,'172.24.0.232',3,0,NULL,1),(NULL,NULL,'172.24.0.233',3,0,NULL,1),(NULL,NULL,'172.24.0.234',3,0,NULL,1);
/*!40000 ALTER TABLE `user_ip_address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_statistics`
--

DROP TABLE IF EXISTS `user_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_statistics` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `data_center_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `net_bytes_received` bigint(20) unsigned NOT NULL DEFAULT '0',
  `net_bytes_sent` bigint(20) unsigned NOT NULL DEFAULT '0',
  `current_bytes_received` bigint(20) unsigned NOT NULL DEFAULT '0',
  `current_bytes_sent` bigint(20) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `i_user_statistics__account_id` (`account_id`),
  KEY `i_user_statistics__account_id_data_center_id` (`account_id`,`data_center_id`),
  CONSTRAINT `fk_user_statistics__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_statistics`
--

LOCK TABLES `user_statistics` WRITE;
/*!40000 ALTER TABLE `user_statistics` DISABLE KEYS */;
INSERT INTO `user_statistics` VALUES (1,3,2,0,0,0,0);
/*!40000 ALTER TABLE `user_statistics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_vm`
--

DROP TABLE IF EXISTS `user_vm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_vm` (
  `id` bigint(20) unsigned NOT NULL,
  `domain_router_id` bigint(20) unsigned DEFAULT NULL COMMENT 'router id',
  `service_offering_id` bigint(20) unsigned NOT NULL COMMENT 'service offering id',
  `vnet` varchar(18) DEFAULT NULL COMMENT 'vnet',
  `dc_vlan` varchar(18) DEFAULT NULL COMMENT 'zone vlan',
  `account_id` bigint(20) unsigned NOT NULL COMMENT 'user id of owner',
  `domain_id` bigint(20) unsigned NOT NULL,
  `guest_ip_address` varchar(15) DEFAULT NULL COMMENT 'ip address within the guest network',
  `guest_mac_address` varchar(17) DEFAULT NULL COMMENT 'mac address within the guest network',
  `guest_netmask` varchar(15) DEFAULT NULL COMMENT 'netmask within the guest network',
  `external_ip_address` varchar(15) DEFAULT NULL COMMENT 'ip address within the external network',
  `external_mac_address` varchar(17) DEFAULT NULL COMMENT 'mac address within the external network',
  `external_vlan_db_id` bigint(20) unsigned DEFAULT NULL COMMENT 'foreign key into vlan table',
  `user_data` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `i_user_vm__domain_router_id` (`domain_router_id`),
  KEY `i_user_vm__service_offering_id` (`service_offering_id`),
  KEY `i_user_vm__account_id` (`account_id`),
  KEY `i_user_vm__external_ip_address` (`external_ip_address`),
  KEY `i_user_vm__external_vlan_db_id` (`external_vlan_db_id`),
  CONSTRAINT `fk_user_vm__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_user_vm__domain_router_id` FOREIGN KEY (`domain_router_id`) REFERENCES `domain_router` (`id`),
  CONSTRAINT `fk_user_vm__external_ip_address` FOREIGN KEY (`external_ip_address`) REFERENCES `user_ip_address` (`public_ip_address`),
  CONSTRAINT `fk_user_vm__external_vlan_db_id` FOREIGN KEY (`external_vlan_db_id`) REFERENCES `vlan` (`id`),
  CONSTRAINT `fk_user_vm__id` FOREIGN KEY (`id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_vm__service_offering_id` FOREIGN KEY (`service_offering_id`) REFERENCES `service_offering` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_vm`
--

LOCK TABLES `user_vm` WRITE;
/*!40000 ALTER TABLE `user_vm` DISABLE KEYS */;
INSERT INTO `user_vm` VALUES (3,4,7,'500',NULL,2,1,'10.1.1.2','02:03:00:00:01:02','255.255.255.0',NULL,NULL,NULL,NULL),(5,4,7,'500',NULL,2,1,'10.1.1.3','02:03:00:00:01:03','255.255.255.0',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `user_vm` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vlan`
--

DROP TABLE IF EXISTS `vlan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vlan` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vlan_id` varchar(255) DEFAULT NULL,
  `vlan_gateway` varchar(255) DEFAULT NULL,
  `vlan_netmask` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `vlan_type` varchar(255) DEFAULT NULL,
  `data_center_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vlan`
--

LOCK TABLES `vlan` WRITE;
/*!40000 ALTER TABLE `vlan` DISABLE KEYS */;
INSERT INTO `vlan` VALUES (1,'24','172.24.0.1','255.255.0.0','172.24.0.111-172.24.0.234','VirtualNetwork',3);
/*!40000 ALTER TABLE `vlan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vm_disk`
--

DROP TABLE IF EXISTS `vm_disk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vm_disk` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `instance_id` bigint(20) unsigned NOT NULL,
  `disk_offering_id` bigint(20) unsigned NOT NULL,
  `removed` datetime DEFAULT NULL COMMENT 'date removed',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vm_disk`
--

LOCK TABLES `vm_disk` WRITE;
/*!40000 ALTER TABLE `vm_disk` DISABLE KEYS */;
/*!40000 ALTER TABLE `vm_disk` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vm_instance`
--

DROP TABLE IF EXISTS `vm_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vm_instance` (
  `id` bigint(20) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `display_name` varchar(255) DEFAULT NULL,
  `group` varchar(255) DEFAULT NULL,
  `instance_name` varchar(255) NOT NULL COMMENT 'name of the vm instance running on the hosts',
  `state` varchar(32) NOT NULL,
  `vm_template_id` bigint(20) unsigned DEFAULT NULL,
  `iso_id` bigint(20) unsigned DEFAULT NULL,
  `guest_os_id` bigint(20) unsigned NOT NULL,
  `private_mac_address` varchar(17) DEFAULT NULL,
  `private_ip_address` varchar(15) DEFAULT NULL,
  `private_netmask` varchar(15) DEFAULT NULL,
  `pod_id` bigint(20) unsigned DEFAULT NULL,
  `storage_ip` varchar(15) DEFAULT NULL,
  `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'Data Center the instance belongs to',
  `host_id` bigint(20) unsigned DEFAULT NULL,
  `last_host_id` bigint(20) unsigned DEFAULT NULL COMMENT 'tentative host for first run or last host that it has been running on',
  `proxy_id` bigint(20) unsigned DEFAULT NULL COMMENT 'console proxy allocated in previous session',
  `proxy_assign_time` datetime DEFAULT NULL COMMENT 'time when console proxy was assigned',
  `vnc_password` varchar(255) NOT NULL COMMENT 'vnc password',
  `ha_enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Should HA be enabled for this VM',
  `mirrored_vols` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Are the volumes mirrored',
  `update_count` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT 'date state was updated',
  `update_time` datetime DEFAULT NULL COMMENT 'date the destroy was requested',
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime DEFAULT NULL COMMENT 'date removed if not null',
  `type` varchar(32) NOT NULL COMMENT 'type of vm it is',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `i_vm_instance__removed` (`removed`),
  KEY `i_vm_instance__type` (`type`),
  KEY `i_vm_instance__pod_id` (`pod_id`),
  KEY `i_vm_instance__update_time` (`update_time`),
  KEY `i_vm_instance__update_count` (`update_count`),
  KEY `i_vm_instance__state` (`state`),
  KEY `i_vm_instance__data_center_id` (`data_center_id`),
  KEY `i_vm_instance__host_id` (`host_id`),
  KEY `i_vm_instance__last_host_id` (`last_host_id`),
  KEY `i_vm_instance__template_id` (`vm_template_id`),
  CONSTRAINT `fk_vm_instance__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`),
  CONSTRAINT `fk_vm_instance__template_id` FOREIGN KEY (`vm_template_id`) REFERENCES `vm_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vm_instance`
--

LOCK TABLES `vm_instance` WRITE;
/*!40000 ALTER TABLE `vm_instance` DISABLE KEYS */;
INSERT INTO `vm_instance` VALUES (1,'s-1-TEST',NULL,NULL,'s-1-TEST','Running',1,NULL,12,'06:03:ed:ac:00:01','192.168.152.104','255.255.255.0',3,NULL,3,2,2,NULL,NULL,'f7dbf142de7c4b92',1,0,4,'2011-03-17 00:58:55','2011-03-17 00:57:29',NULL,'SecondaryStorageVm'),(2,'v-2-TEST',NULL,NULL,'v-2-TEST','Running',1,NULL,12,'06:03:c7:31:00:03','192.168.152.106','255.255.255.0',3,NULL,3,2,2,NULL,NULL,'729a4471ef328e07',1,0,4,'2011-03-17 00:59:01','2011-03-17 00:57:29',NULL,'ConsoleProxy'),(3,'i-2-3-TEST','i-2-3-TEST',NULL,'i-2-3-TEST-500','Running',2,NULL,12,'02:03:00:00:01:02','10.1.1.2','255.255.255.0',3,NULL,3,2,2,NULL,NULL,'6cdb8c77759dcc2a',0,0,3,'2011-03-17 01:01:00','2011-03-17 00:59:29',NULL,'User'),(4,'r-4-TEST',NULL,NULL,'r-4-TEST-500','Running',1,NULL,12,'06:03:71:58:00:05','169.254.3.45','255.255.0.0',3,NULL,3,2,2,NULL,NULL,'dabe181e7f2a2c0f',1,0,4,'2011-03-17 01:00:53','2011-03-17 00:59:29',NULL,'DomainRouter'),(5,'i-2-5-TEST','i-2-5-TEST',NULL,'i-2-5-TEST-500','Running',2,NULL,12,'02:03:00:00:01:03','10.1.1.3','255.255.255.0',3,NULL,3,2,2,NULL,NULL,'5afc1cd493fee208',0,0,3,'2011-03-17 01:01:04','2011-03-17 00:59:32',NULL,'User');
/*!40000 ALTER TABLE `vm_instance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vm_template`
--

DROP TABLE IF EXISTS `vm_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vm_template` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `unique_name` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `public` int(1) unsigned NOT NULL,
  `featured` int(1) unsigned NOT NULL,
  `type` varchar(32) DEFAULT NULL,
  `hvm` int(1) unsigned NOT NULL COMMENT 'requires HVM',
  `bits` int(6) unsigned NOT NULL COMMENT '32 bit or 64 bit',
  `url` varchar(255) DEFAULT NULL COMMENT 'the url where the template exists externally',
  `format` varchar(32) NOT NULL COMMENT 'format for the template',
  `created` datetime NOT NULL COMMENT 'Date created',
  `removed` datetime DEFAULT NULL COMMENT 'Date removed if not null',
  `account_id` bigint(20) unsigned NOT NULL COMMENT 'id of the account that created this template',
  `checksum` varchar(255) DEFAULT NULL COMMENT 'checksum for the template root disk',
  `display_text` varchar(4096) DEFAULT NULL COMMENT 'Description text set by the admin for display purpose only',
  `enable_password` int(1) unsigned NOT NULL DEFAULT '1' COMMENT 'true if this template supports password reset',
  `guest_os_id` bigint(20) unsigned NOT NULL COMMENT 'the OS of the template',
  `bootable` int(1) unsigned NOT NULL DEFAULT '1' COMMENT 'true if this template represents a bootable ISO',
  `prepopulate` int(1) unsigned NOT NULL DEFAULT '0' COMMENT 'prepopulate this template to primary storage',
  `cross_zones` int(1) unsigned NOT NULL DEFAULT '0' COMMENT 'Make this template available in all zones',
  PRIMARY KEY (`id`),
  KEY `i_vm_template__removed` (`removed`),
  KEY `i_vm_template__public` (`public`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vm_template`
--

LOCK TABLES `vm_template` WRITE;
/*!40000 ALTER TABLE `vm_template` DISABLE KEYS */;
INSERT INTO `vm_template` VALUES (1,'routing','SystemVM Template',0,0,'ext3',0,64,'http://download.cloud.com/releases/2.0.0RC5/systemvm.vhd.bz2','VHD','2011-03-16 17:56:02',NULL,1,'31cd7ce94fe68c973d5dc37c3349d02e','SystemVM Template',0,12,1,0,1),(2,'centos53-x86_64','CentOS 5.3(x86_64) no GUI',1,1,'ext3',0,64,'http://download.cloud.com/templates/builtin/f59f18fb-ae94-4f97-afd2-f84755767aca.vhd.bz2','VHD','2011-03-16 17:56:02',NULL,1,'b63d854a9560c013142567bbae8d98cf','CentOS 5.3(x86_64) no GUI',0,12,1,0,1),(200,'xs-tools.iso','xs-tools.iso',1,1,'cdfs',1,64,'/opt/xensource/packages/iso/xs-tools-5.5.0.iso','ISO','2011-03-17 00:56:56',NULL,1,NULL,'xen-pv-drv-iso',0,1,0,0,0);
/*!40000 ALTER TABLE `vm_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `volumes`
--

DROP TABLE IF EXISTS `volumes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `volumes` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `account_id` bigint(20) unsigned NOT NULL COMMENT 'owner.  foreign key to account table',
  `domain_id` bigint(20) unsigned NOT NULL COMMENT 'the domain that the owner belongs to',
  `pool_id` bigint(20) unsigned DEFAULT NULL COMMENT 'pool it belongs to. foreign key to storage_pool table',
  `instance_id` bigint(20) unsigned DEFAULT NULL COMMENT 'vm instance it belongs to. foreign key to vm_instance table',
  `device_id` bigint(20) unsigned DEFAULT NULL COMMENT 'which device inside vm instance it is ',
  `name` varchar(255) DEFAULT NULL COMMENT 'A user specified name for the volume',
  `size` bigint(20) unsigned NOT NULL COMMENT 'total size',
  `folder` varchar(255) DEFAULT NULL COMMENT 'The folder where the volume is saved',
  `path` varchar(255) DEFAULT NULL COMMENT 'Path',
  `pod_id` bigint(20) unsigned DEFAULT NULL COMMENT 'pod this volume belongs to',
  `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'data center this volume belongs to',
  `iscsi_name` varchar(255) DEFAULT NULL COMMENT 'iscsi target name',
  `host_ip` varchar(15) DEFAULT NULL COMMENT 'host ip address for convenience',
  `volume_type` varchar(64) DEFAULT NULL COMMENT 'root, swap or data',
  `resource_type` varchar(64) DEFAULT NULL COMMENT 'pool-based or host-based',
  `pool_type` varchar(64) DEFAULT NULL COMMENT 'type of the pool',
  `mirror_state` varchar(64) DEFAULT NULL COMMENT 'not_mirrored, active or defunct',
  `mirror_vol` bigint(20) unsigned DEFAULT NULL COMMENT 'the other half of the mirrored set if mirrored',
  `disk_offering_id` bigint(20) unsigned NOT NULL COMMENT 'can be null for system VMs',
  `template_id` bigint(20) unsigned DEFAULT NULL COMMENT 'fk to vm_template.id',
  `first_snapshot_backup_uuid` varchar(255) DEFAULT NULL COMMENT 'The first snapshot that was ever taken for this volume',
  `recreatable` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT 'Is this volume recreatable?',
  `destroyed` tinyint(1) DEFAULT NULL COMMENT 'indicates whether the volume was destroyed by the user or not',
  `created` datetime DEFAULT NULL COMMENT 'Date Created',
  `updated` datetime DEFAULT NULL COMMENT 'Date updated for attach/detach',
  `removed` datetime DEFAULT NULL COMMENT 'Date removed.  not null if removed',
  `status` varchar(32) DEFAULT NULL COMMENT 'Async API volume creation status',
  PRIMARY KEY (`id`),
  KEY `i_volumes__removed` (`removed`),
  KEY `i_volumes__pod_id` (`pod_id`),
  KEY `i_volumes__data_center_id` (`data_center_id`),
  KEY `i_volumes__account_id` (`account_id`),
  KEY `i_volumes__pool_id` (`pool_id`),
  KEY `i_volumes__instance_id` (`instance_id`),
  CONSTRAINT `fk_volumes__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_volumes__instance_id` FOREIGN KEY (`instance_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_volumes__pool_id` FOREIGN KEY (`pool_id`) REFERENCES `storage_pool` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `volumes`
--

LOCK TABLES `volumes` WRITE;
/*!40000 ALTER TABLE `volumes` DISABLE KEYS */;
INSERT INTO `volumes` VALUES (1,1,1,200,1,0,'s-1-TEST-ROOT',2147483648,'/export/alena-primary','802981cb-54fb-4d25-84eb-126ce05439fe',3,3,NULL,NULL,'ROOT','STORAGE_POOL','NetworkFilesystem','NOT_MIRRORED',NULL,15,1,NULL,1,0,'2011-03-17 00:57:29',NULL,NULL,'Created'),(2,1,1,200,2,0,'v-2-TEST-ROOT',2147483648,'/export/alena-primary','2773dc0c-149c-464b-bcb2-97214a8e0711',3,3,NULL,NULL,'ROOT','STORAGE_POOL','NetworkFilesystem','NOT_MIRRORED',NULL,16,1,NULL,1,0,'2011-03-17 00:57:29',NULL,NULL,'Created'),(3,2,1,200,4,0,'r-4-TEST-ROOT',2147483648,'/export/alena-primary','fd9bbd40-d9dc-4c54-801d-6c05a5703155',3,3,NULL,NULL,'ROOT','STORAGE_POOL','NetworkFilesystem','NOT_MIRRORED',NULL,14,1,NULL,1,0,'2011-03-17 00:59:29',NULL,NULL,'Created'),(4,2,1,200,3,0,'i-2-3-TEST-ROOT',8589934592,'/export/alena-primary','022ae62f-716d-40e2-ad79-33a3f0d2d0e8',3,3,NULL,NULL,'ROOT','STORAGE_POOL','NetworkFilesystem','NOT_MIRRORED',NULL,7,2,NULL,0,0,'2011-03-17 00:59:30',NULL,NULL,'Created'),(5,2,1,200,5,0,'i-2-5-TEST-ROOT',8589934592,'/export/alena-primary','efd65a25-1497-4090-a052-aae0c1619d24',3,3,NULL,NULL,'ROOT','STORAGE_POOL','NetworkFilesystem','NOT_MIRRORED',NULL,7,2,NULL,0,0,'2011-03-17 00:59:32',NULL,NULL,'Created');
/*!40000 ALTER TABLE `volumes` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2011-03-16 18:10:52
