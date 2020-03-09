// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.test;

import com.cloud.host.Status;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDaoImpl;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.dao.DiskOfferingDaoImpl;
import com.cloud.utils.DateUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.net.NfsUtils;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class DatabaseConfig {
    private static final Logger s_logger = Logger.getLogger(DatabaseConfig.class.getName());

    private String _configFileName = null;
    private String _currentObjectName = null;
    private String _currentFieldName = null;
    private Map<String, String> _currentObjectParams = null;

    private static Map<String, String> s_configurationDescriptions = new HashMap<String, String>();
    private static Map<String, String> s_configurationComponents = new HashMap<String, String>();
    private static Map<String, String> s_defaultConfigurationValues = new HashMap<String, String>();

    // Change to HashSet
    private static HashSet<String> objectNames = new HashSet<String>();
    private static HashSet<String> fieldNames = new HashSet<String>();

    // Maintain an IPRangeConfig object to handle IP related logic
    private final IPRangeConfig iprc = ComponentContext.inject(IPRangeConfig.class);

    // Maintain a PodZoneConfig object to handle Pod/Zone related logic
    private final PodZoneConfig pzc = ComponentContext.inject(PodZoneConfig.class);

    // Global variables to store network.throttling.rate and multicast.throttling.rate from the configuration table
    // Will be changed from null to a non-null value if the value existed in the configuration table
    private String _networkThrottlingRate = null;
    private String _multicastThrottlingRate = null;

    static {
        // initialize the objectNames ArrayList
        objectNames.add("zone");
        objectNames.add("physicalNetwork");
        objectNames.add("vlan");
        objectNames.add("pod");
        objectNames.add("cluster");
        objectNames.add("storagePool");
        objectNames.add("secondaryStorage");
        objectNames.add("serviceOffering");
        objectNames.add("diskOffering");
        objectNames.add("user");
        objectNames.add("pricing");
        objectNames.add("configuration");
        objectNames.add("privateIpAddresses");
        objectNames.add("publicIpAddresses");
        objectNames.add("physicalNetworkServiceProvider");
        objectNames.add("virtualRouterProvider");

        // initialize the fieldNames ArrayList
        fieldNames.add("id");
        fieldNames.add("name");
        fieldNames.add("dns1");
        fieldNames.add("dns2");
        fieldNames.add("internalDns1");
        fieldNames.add("internalDns2");
        fieldNames.add("guestNetworkCidr");
        fieldNames.add("gateway");
        fieldNames.add("netmask");
        fieldNames.add("vncConsoleIp");
        fieldNames.add("zoneId");
        fieldNames.add("vlanId");
        fieldNames.add("cpu");
        fieldNames.add("ramSize");
        fieldNames.add("speed");
        fieldNames.add("useLocalStorage");
        fieldNames.add("hypervisorType");
        fieldNames.add("diskSpace");
        fieldNames.add("nwRate");
        fieldNames.add("mcRate");
        fieldNames.add("price");
        fieldNames.add("username");
        fieldNames.add("password");
        fieldNames.add("firstname");
        fieldNames.add("lastname");
        fieldNames.add("email");
        fieldNames.add("priceUnit");
        fieldNames.add("type");
        fieldNames.add("value");
        fieldNames.add("podId");
        fieldNames.add("podName");
        fieldNames.add("ipAddressRange");
        fieldNames.add("vlanType");
        fieldNames.add("vlanName");
        fieldNames.add("cidr");
        fieldNames.add("vnet");
        fieldNames.add("mirrored");
        fieldNames.add("enableHA");
        fieldNames.add("displayText");
        fieldNames.add("domainId");
        fieldNames.add("hostAddress");
        fieldNames.add("hostPath");
        fieldNames.add("guestIpType");
        fieldNames.add("url");
        fieldNames.add("storageType");
        fieldNames.add("category");
        fieldNames.add("tags");
        fieldNames.add("networktype");
        fieldNames.add("clusterId");
        fieldNames.add("physicalNetworkId");
        fieldNames.add("destPhysicalNetworkId");
        fieldNames.add("providerName");
        fieldNames.add("vpn");
        fieldNames.add("dhcp");
        fieldNames.add("dns");
        fieldNames.add("firewall");
        fieldNames.add("sourceNat");
        fieldNames.add("loadBalance");
        fieldNames.add("staticNat");
        fieldNames.add("portForwarding");
        fieldNames.add("userData");
        fieldNames.add("securityGroup");
        fieldNames.add("nspId");

        s_configurationDescriptions.put("host.stats.interval", "the interval in milliseconds when host stats are retrieved from agents");
        s_configurationDescriptions.put("storage.stats.interval", "the interval in milliseconds when storage stats (per host) are retrieved from agents");
        s_configurationDescriptions.put("volume.stats.interval", "the interval in milliseconds when volume stats are retrieved from agents");
        s_configurationDescriptions.put("host", "host address to listen on for agent connection");
        s_configurationDescriptions.put("port", "port to listen on for agent connection");
        s_configurationDescriptions.put("guest.domain.suffix", "domain suffix for users");
        s_configurationDescriptions.put("instance.name", "Name of the deployment instance");
        s_configurationDescriptions.put("storage.overprovisioning.factor", "Storage Allocator overprovisioning factor");
        s_configurationDescriptions.put("retries.per.host", "The number of times each command sent to a host should be retried in case of failure.");
        s_configurationDescriptions.put("integration.api.port", "internal port used by the management server for servicing Integration API requests");
        s_configurationDescriptions.put("usage.stats.job.exec.time",
            "the time at which the usage statistics aggregation job will run as an HH24:MM time, e.g. 00:30 to run at 12:30am");
        s_configurationDescriptions.put("usage.stats.job.aggregation.range",
            "the range of time for aggregating the user statistics specified in minutes (e.g. 1440 for daily, 60 for hourly)");
        s_configurationDescriptions.put("consoleproxy.domP.enable", "Obsolete");
        s_configurationDescriptions.put("consoleproxy.port", "Obsolete");
        s_configurationDescriptions.put("consoleproxy.url.port", "Console proxy port for AJAX viewer");
        s_configurationDescriptions.put("consoleproxy.ram.size", "RAM size (in MB) used to create new console proxy VMs");
        s_configurationDescriptions.put("consoleproxy.cmd.port", "Console proxy command port that is used to communicate with management server");
        s_configurationDescriptions.put("consoleproxy.capacityscan.interval",
            "The time interval(in millisecond) to scan whether or not system needs more console proxy to ensure minimal standby capacity");
        s_configurationDescriptions.put("consoleproxy.capacity.standby",
            "The minimal number of console proxy viewer sessions that system is able to serve immediately(standby capacity)");
        s_configurationDescriptions.put("alert.email.addresses", "comma seperated list of email addresses used for sending alerts");
        s_configurationDescriptions.put("alert.smtp.host", "SMTP hostname used for sending out email alerts");
        s_configurationDescriptions.put("alert.smtp.port", "port the SMTP server is listening on (default is 25)");
        s_configurationDescriptions.put("alert.smtp.useAuth",
            "If true, use SMTP authentication when sending emails.  If false, do not use SMTP authentication when sending emails.");
        s_configurationDescriptions.put("alert.smtp.username", "username for SMTP authentication (applies only if alert.smtp.useAuth is true)");
        s_configurationDescriptions.put("alert.smtp.password", "password for SMTP authentication (applies only if alert.smtp.useAuth is true)");
        s_configurationDescriptions.put("alert.email.sender", "sender of alert email (will be in the From header of the email)");
        s_configurationDescriptions.put("memory.capacity.threshold",
            "percentage (as a value between 0 and 1) of memory utilization above which alerts will be sent about low memory available");
        s_configurationDescriptions.put("cpu.capacity.threshold",
            "percentage (as a value between 0 and 1) of cpu utilization above which alerts will be sent about low cpu available");
        s_configurationDescriptions.put("storage.capacity.threshold",
            "percentage (as a value between 0 and 1) of storage utilization above which alerts will be sent about low storage available");
        s_configurationDescriptions.put("public.ip.capacity.threshold",
            "percentage (as a value between 0 and 1) of public IP address space utilization above which alerts will be sent");
        s_configurationDescriptions.put("private.ip.capacity.threshold",
            "percentage (as a value between 0 and 1) of private IP address space utilization above which alerts will be sent");
        s_configurationDescriptions.put("expunge.interval", "the interval to wait before running the expunge thread");
        s_configurationDescriptions.put("network.throttling.rate", "default data transfer rate in megabits per second allowed per user");
        s_configurationDescriptions.put("multicast.throttling.rate", "default multicast rate in megabits per second allowed");
        s_configurationDescriptions.put("system.vm.use.local.storage", "Indicates whether to use local storage pools or shared storage pools for system VMs.");
        s_configurationDescriptions.put("snapshot.poll.interval", "The time interval in seconds when the management server polls for snapshots to be scheduled.");
        s_configurationDescriptions.put("snapshot.max.hourly", "Maximum hourly snapshots for a volume");
        s_configurationDescriptions.put("snapshot.max.daily", "Maximum daily snapshots for a volume");
        s_configurationDescriptions.put("snapshot.max.weekly", "Maximum weekly snapshots for a volume");
        s_configurationDescriptions.put("snapshot.max.monthly", "Maximum monthly snapshots for a volume");
        s_configurationDescriptions.put("snapshot.delta.max", "max delta snapshots between two full snapshots.");
        s_configurationDescriptions.put("snapshot.recurring.test", "Flag for testing recurring snapshots");
        s_configurationDescriptions.put("snapshot.test.minutes.per.hour", "Set it to a smaller value to take more recurring snapshots");
        s_configurationDescriptions.put("snapshot.test.hours.per.day", "Set it to a smaller value to take more recurring snapshots");
        s_configurationDescriptions.put("snapshot.test.days.per.week", "Set it to a smaller value to take more recurring snapshots");
        s_configurationDescriptions.put("snapshot.test.days.per.month", "Set it to a smaller value to take more recurring snapshots");
        s_configurationDescriptions.put("snapshot.test.weeks.per.month", "Set it to a smaller value to take more recurring snapshots");
        s_configurationDescriptions.put("snapshot.test.months.per.year", "Set it to a smaller value to take more recurring snapshots");
        s_configurationDescriptions.put("hypervisor.type", "The type of hypervisor that this deployment will use.");
        s_configurationDescriptions.put("publish.action.events", "enable or disable to control the publishing of action events on the event bus");
        s_configurationDescriptions.put("publish.alert.events", "enable or disable to control the publishing of alert events on the event bus");
        s_configurationDescriptions.put("publish.resource.state.events", "enable or disable to control the publishing of resource state events on the event bus");
        s_configurationDescriptions.put("publish.usage.events", "enable or disable to control the publishing of usage events on the event bus");
        s_configurationDescriptions.put("publish.async.job.events", "enable or disable to control the publishing of async job events on the event bus");

        s_configurationComponents.put("host.stats.interval", "management-server");
        s_configurationComponents.put("storage.stats.interval", "management-server");
        s_configurationComponents.put("volume.stats.interval", "management-server");
        s_configurationComponents.put("integration.api.port", "management-server");
        s_configurationComponents.put("usage.stats.job.exec.time", "management-server");
        s_configurationComponents.put("usage.stats.job.aggregation.range", "management-server");
        s_configurationComponents.put("consoleproxy.domP.enable", "management-server");
        s_configurationComponents.put("consoleproxy.port", "management-server");
        s_configurationComponents.put("consoleproxy.url.port", "management-server");
        s_configurationComponents.put("alert.email.addresses", "management-server");
        s_configurationComponents.put("alert.smtp.host", "management-server");
        s_configurationComponents.put("alert.smtp.port", "management-server");
        s_configurationComponents.put("alert.smtp.useAuth", "management-server");
        s_configurationComponents.put("alert.smtp.username", "management-server");
        s_configurationComponents.put("alert.smtp.password", "management-server");
        s_configurationComponents.put("alert.email.sender", "management-server");
        s_configurationComponents.put("memory.capacity.threshold", "management-server");
        s_configurationComponents.put("cpu.capacity.threshold", "management-server");
        s_configurationComponents.put("storage.capacity.threshold", "management-server");
        s_configurationComponents.put("public.ip.capacity.threshold", "management-server");
        s_configurationComponents.put("private.ip.capacity.threshold", "management-server");
        s_configurationComponents.put("capacity.check.period", "management-server");
        s_configurationComponents.put("network.throttling.rate", "management-server");
        s_configurationComponents.put("multicast.throttling.rate", "management-server");
        s_configurationComponents.put("event.purge.interval", "management-server");
        s_configurationComponents.put("account.cleanup.interval", "management-server");
        s_configurationComponents.put("expunge.delay", "UserVmManager");
        s_configurationComponents.put("expunge.interval", "UserVmManager");
        s_configurationComponents.put("host", "AgentManager");
        s_configurationComponents.put("port", "AgentManager");
        s_configurationComponents.put("domain", "AgentManager");
        s_configurationComponents.put("instance.name", "AgentManager");
        s_configurationComponents.put("storage.overprovisioning.factor", "StorageAllocator");
        s_configurationComponents.put("retries.per.host", "AgentManager");
        s_configurationComponents.put("start.retry", "AgentManager");
        s_configurationComponents.put("wait", "AgentManager");
        s_configurationComponents.put("ping.timeout", "AgentManager");
        s_configurationComponents.put("ping.interval", "AgentManager");
        s_configurationComponents.put("alert.wait", "AgentManager");
        s_configurationComponents.put("update.wait", "AgentManager");
        s_configurationComponents.put("guest.domain.suffix", "AgentManager");
        s_configurationComponents.put("consoleproxy.ram.size", "AgentManager");
        s_configurationComponents.put("consoleproxy.cmd.port", "AgentManager");
        s_configurationComponents.put("consoleproxy.capacityscan.interval", "AgentManager");
        s_configurationComponents.put("consoleproxy.capacity.standby", "AgentManager");
        s_configurationComponents.put("consoleproxy.session.max", "AgentManager");
        s_configurationComponents.put("consoleproxy.session.timeout", "AgentManager");
        s_configurationComponents.put("expunge.workers", "UserVmManager");
        s_configurationComponents.put("extract.url.cleanup.interval", "management-server");
        s_configurationComponents.put("storage.overwrite.provisioning", "UserVmManager");
        s_configurationComponents.put("init", "none");
        s_configurationComponents.put("system.vm.use.local.storage", "ManagementServer");
        s_configurationComponents.put("snapshot.poll.interval", "SnapshotManager");
        s_configurationComponents.put("snapshot.max.hourly", "SnapshotManager");
        s_configurationComponents.put("snapshot.max.daily", "SnapshotManager");
        s_configurationComponents.put("snapshot.max.weekly", "SnapshotManager");
        s_configurationComponents.put("snapshot.max.monthly", "SnapshotManager");
        s_configurationComponents.put("snapshot.delta.max", "SnapshotManager");
        s_configurationComponents.put("snapshot.recurring.test", "SnapshotManager");
        s_configurationComponents.put("snapshot.test.minutes.per.hour", "SnapshotManager");
        s_configurationComponents.put("snapshot.test.hours.per.day", "SnapshotManager");
        s_configurationComponents.put("snapshot.test.days.per.week", "SnapshotManager");
        s_configurationComponents.put("snapshot.test.days.per.month", "SnapshotManager");
        s_configurationComponents.put("snapshot.test.weeks.per.month", "SnapshotManager");
        s_configurationComponents.put("snapshot.test.months.per.year", "SnapshotManager");
        s_configurationComponents.put("hypervisor.type", "ManagementServer");
        s_configurationComponents.put("publish.action.events", "management-server");
        s_configurationComponents.put("publish.alert.events", "management-server");
        s_configurationComponents.put("publish.resource.state.events", "management-server");
        s_configurationComponents.put("publish.usage.events", "management-server");
        s_configurationComponents.put("publish.async.job.events", "management-server");

        s_defaultConfigurationValues.put("host.stats.interval", "60000");
        s_defaultConfigurationValues.put("storage.stats.interval", "60000");
        s_defaultConfigurationValues.put("volume.stats.interval", "60000");
        s_defaultConfigurationValues.put("port", "8250");
        s_defaultConfigurationValues.put("integration.api.port", "8096");
        s_defaultConfigurationValues.put("usage.stats.job.exec.time", "00:15"); // run at 12:15am
        s_defaultConfigurationValues.put("usage.stats.job.aggregation.range", "1440"); // do a daily aggregation
        s_defaultConfigurationValues.put("storage.overprovisioning.factor", "2");
        s_defaultConfigurationValues.put("retries.per.host", "2");
        s_defaultConfigurationValues.put("ping.timeout", "2.5");
        s_defaultConfigurationValues.put("ping.interval", "60");
        s_defaultConfigurationValues.put("snapshot.poll.interval", "300");
        s_defaultConfigurationValues.put("snapshot.max.hourly", "8");
        s_defaultConfigurationValues.put("snapshot.max.daily", "8");
        s_defaultConfigurationValues.put("snapshot.max.weekly", "8");
        s_defaultConfigurationValues.put("snapshot.max.monthly", "8");
        s_defaultConfigurationValues.put("snapshot.delta.max", "16");
        s_defaultConfigurationValues.put("snapshot.recurring.test", "false");
        s_defaultConfigurationValues.put("snapshot.test.minutes.per.hour", "60");
        s_defaultConfigurationValues.put("snapshot.test.hours.per.day", "24");
        s_defaultConfigurationValues.put("snapshot.test.days.per.week", "7");
        s_defaultConfigurationValues.put("snapshot.test.days.per.month", "30");
        s_defaultConfigurationValues.put("snapshot.test.weeks.per.month", "4");
        s_defaultConfigurationValues.put("snapshot.test.months.per.year", "12");
        s_defaultConfigurationValues.put("alert.wait", "1800");
        s_defaultConfigurationValues.put("update.wait", "600");
        s_defaultConfigurationValues.put("expunge.interval", "86400");
        s_defaultConfigurationValues.put("extract.url.cleanup.interval", "120");
        s_defaultConfigurationValues.put("instance.name", "VM");
        s_defaultConfigurationValues.put("expunge.workers", "1");
        s_defaultConfigurationValues.put("event.purge.interval", "86400");
        s_defaultConfigurationValues.put("account.cleanup.interval", "86400");
        s_defaultConfigurationValues.put("system.vm.use.local.storage", "false");
        s_defaultConfigurationValues.put("init", "false");
        s_defaultConfigurationValues.put("cpu.overprovisioning.factor", "1");
        s_defaultConfigurationValues.put("mem.overprovisioning.factor", "1");
        s_defaultConfigurationValues.put("publish.action.events", "true");
        s_defaultConfigurationValues.put("publish.alert.events", "true");
        s_defaultConfigurationValues.put("publish.resource.state.events", "true");
        s_defaultConfigurationValues.put("publish.usage.events", "true");
        s_defaultConfigurationValues.put("publish.async.job.events", "true");
    }

    protected DatabaseConfig() {
    }

    /**
     * @param args - name of server-setup.xml file which contains the bootstrap data
     */
    public static void main(String[] args) {
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        System.setProperty("javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");

        File file = PropertiesUtil.findConfigFile("log4j-cloud.xml");
        if (file != null) {
            System.out.println("Log4j configuration from : " + file.getAbsolutePath());
            DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
        } else {
            System.out.println("Configure log4j with default properties");
        }

        if (args.length < 1) {
            s_logger.error("error starting database config, missing initial data file");
        } else {
            try {
                DatabaseConfig config = ComponentContext.inject(DatabaseConfig.class);
                config.doVersionCheck();
                config.doConfig();
                System.exit(0);
            } catch (Exception ex) {
                System.out.print("Error Caught");
                ex.printStackTrace();
                s_logger.error("error", ex);
            }
        }
    }

    public DatabaseConfig(String configFileName) {
        _configFileName = configFileName;
    }

    private void doVersionCheck() {
        try {
            String warningMsg = "\nYou are using an outdated format for server-setup.xml. Please switch to the new format.\n";
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbuilder = dbf.newDocumentBuilder();
            File configFile = new File(_configFileName);
            Document d = dbuilder.parse(configFile);
            NodeList nodeList = d.getElementsByTagName("version");

            if (nodeList.getLength() == 0) {
                System.out.println(warningMsg);
                return;
            }

            Node firstNode = nodeList.item(0);
            String version = firstNode.getTextContent();

            if (!version.equals("2.0")) {
                System.out.println(warningMsg);
            }

        } catch (ParserConfigurationException parserException) {
            parserException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (SAXException saxException) {
            saxException.printStackTrace();
        }
    }

    @DB
    protected void doConfig() {
        try {
            final File configFile = new File(_configFileName);

            SAXParserFactory spfactory = SAXParserFactory.newInstance();
            final SAXParser saxParser = spfactory.newSAXParser();
            final DbConfigXMLHandler handler = new DbConfigXMLHandler();
            handler.setParent(this);

            Transaction.execute(new TransactionCallbackWithExceptionNoReturn<Exception>() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) throws Exception {
                    // Save user configured values for all fields
                    saxParser.parse(configFile, handler);

                    // Save default values for configuration fields
                    saveVMTemplate();
                    saveRootDomain();
                    saveDefaultConfiguations();
                }
            });

            // Check pod CIDRs against each other, and against the guest ip network/netmask
            pzc.checkAllPodCidrSubnets();
        } catch (Exception ex) {
            System.out.print("ERROR IS" + ex);
            s_logger.error("error", ex);
        }
    }

    private void setCurrentObjectName(String name) {
        _currentObjectName = name;
    }

    private void saveCurrentObject() {
        if ("zone".equals(_currentObjectName)) {
            saveZone();
        } else if ("physicalNetwork".equals(_currentObjectName)) {
            savePhysicalNetwork();
        } else if ("vlan".equals(_currentObjectName)) {
            saveVlan();
        } else if ("pod".equals(_currentObjectName)) {
            savePod();
        } else if ("serviceOffering".equals(_currentObjectName)) {
            saveServiceOffering();
        } else if ("diskOffering".equals(_currentObjectName)) {
            saveDiskOffering();
        } else if ("user".equals(_currentObjectName)) {
            saveUser();
        } else if ("configuration".equals(_currentObjectName)) {
            saveConfiguration();
        } else if ("storagePool".equals(_currentObjectName)) {
            saveStoragePool();
        } else if ("secondaryStorage".equals(_currentObjectName)) {
            saveSecondaryStorage();
        } else if ("cluster".equals(_currentObjectName)) {
            saveCluster();
        } else if ("physicalNetworkServiceProvider".equals(_currentObjectName)) {
            savePhysicalNetworkServiceProvider();
        } else if ("virtualRouterProvider".equals(_currentObjectName)) {
            saveVirtualRouterProvider();
        }
        _currentObjectParams = null;
    }

    @DB
    public void saveSecondaryStorage() {
        long dataCenterId = Long.parseLong(_currentObjectParams.get("zoneId"));
        String url = _currentObjectParams.get("url");
        String mountPoint;
        try {
            mountPoint = NfsUtils.url2Mount(url);
        } catch (URISyntaxException e1) {
            return;
        }
        String insertSql1 =
            "INSERT INTO `host` (`id`, `name`, `status` , `type` , `private_ip_address`, `private_netmask` ,`private_mac_address` , `storage_ip_address` ,`storage_netmask`, `storage_mac_address`, `data_center_id`, `version`, `dom0_memory`, `last_ping`, `resource`, `guid`, `hypervisor_type`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String insertSqlHostDetails = "INSERT INTO `host_details` (`id`, `host_id`, `name`, `value`) VALUES(?,?,?,?)";
        String insertSql2 = "INSERT INTO `op_host` (`id`, `sequence`) VALUES(?, ?)";
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql1);
            stmt.setLong(1, 0);
            stmt.setString(2, url);
            stmt.setString(3, "UP");
            stmt.setString(4, "SecondaryStorage");
            stmt.setString(5, "192.168.122.1");
            stmt.setString(6, "255.255.255.0");
            stmt.setString(7, "92:ff:f5:ad:23:e1");
            stmt.setString(8, "192.168.122.1");
            stmt.setString(9, "255.255.255.0");
            stmt.setString(10, "92:ff:f5:ad:23:e1");
            stmt.setLong(11, dataCenterId);
            stmt.setString(12, "2.2.4");
            stmt.setLong(13, 0);
            stmt.setLong(14, 1238425896);

            boolean nfs = false;
            if (url.startsWith("nfs") || url.startsWith("cifs")) {
                nfs = true;
            }
            if (nfs) {
                stmt.setString(15, "com.cloud.storage.resource.NfsSecondaryStorageResource");
            } else {
                stmt.setString(15, "com.cloud.storage.secondary.LocalSecondaryStorageResource");
            }
            stmt.setString(16, url);
            stmt.setString(17, "None");
            stmt.executeUpdate();

            stmt = txn.prepareAutoCloseStatement(insertSqlHostDetails);
            stmt.setLong(1, 1);
            stmt.setLong(2, 1);
            stmt.setString(3, "mount.parent");
            if (nfs) {
                stmt.setString(4, "/mnt");
            } else {
                stmt.setString(4, "/");
            }
            stmt.executeUpdate();

            stmt.setLong(1, 2);
            stmt.setLong(2, 1);
            stmt.setString(3, "mount.path");
            if (nfs) {
                stmt.setString(4, mountPoint);
            } else {
                stmt.setString(4, url.replaceFirst("file:/", ""));
            }
            stmt.executeUpdate();

            stmt.setLong(1, 3);
            stmt.setLong(2, 1);
            stmt.setString(3, "orig.url");
            stmt.setString(4, url);
            stmt.executeUpdate();

            stmt = txn.prepareAutoCloseStatement(insertSql2);
            stmt.setLong(1, 1);
            stmt.setLong(2, 1);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            System.out.println("Error creating secondary storage: " + ex.getMessage());
            return;
        }
    }

    @DB
    public void saveCluster() {
        String name = _currentObjectParams.get("name");
        long id = Long.parseLong(_currentObjectParams.get("id"));
        long dataCenterId = Long.parseLong(_currentObjectParams.get("zoneId"));
        long podId = Long.parseLong(_currentObjectParams.get("podId"));
        String hypervisor = _currentObjectParams.get("hypervisorType");
        String insertSql1 =
            "INSERT INTO `cluster` (`id`, `name`, `data_center_id` , `pod_id`, `hypervisor_type` , `cluster_type`, `allocation_state`) VALUES (?,?,?,?,?,?,?)";

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql1);
            stmt.setLong(1, id);
            stmt.setString(2, name);
            stmt.setLong(3, dataCenterId);
            stmt.setLong(4, podId);
            stmt.setString(5, hypervisor);
            stmt.setString(6, "CloudManaged");
            stmt.setString(7, "Enabled");
            stmt.executeUpdate();

        } catch (SQLException ex) {
            System.out.println("Error creating cluster: " + ex.getMessage());
            s_logger.error("error creating cluster", ex);
            return;
        }

    }

    @DB
    public void saveStoragePool() {
        String name = _currentObjectParams.get("name");
        long id = Long.parseLong(_currentObjectParams.get("id"));
        long dataCenterId = Long.parseLong(_currentObjectParams.get("zoneId"));
        long podId = Long.parseLong(_currentObjectParams.get("podId"));
        long clusterId = Long.parseLong(_currentObjectParams.get("clusterId"));
        String hostAddress = _currentObjectParams.get("hostAddress");
        String hostPath = _currentObjectParams.get("hostPath");
        String storageType = _currentObjectParams.get("storageType");
        String uuid = UUID.nameUUIDFromBytes(new String(hostAddress + hostPath).getBytes()).toString();

        String insertSql1 =
            "INSERT INTO `storage_pool` (`id`, `name`, `uuid` , `pool_type` , `port`, `data_center_id` ,`available_bytes` , `capacity_bytes` ,`host_address`, `path`, `created`, `pod_id`,`status` , `cluster_id`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        // String insertSql2 = "INSERT INTO `netfs_storage_pool` VALUES (?,?,?)";

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql1);
            stmt.setLong(1, id);
            stmt.setString(2, name);
            stmt.setString(3, uuid);
            if (storageType == null) {
                stmt.setString(4, "NetworkFileSystem");
            } else {
                stmt.setString(4, storageType);
            }
            stmt.setLong(5, 111);
            stmt.setLong(6, dataCenterId);
            stmt.setLong(7, 0);
            stmt.setLong(8, 0);
            stmt.setString(9, hostAddress);
            stmt.setString(10, hostPath);
            stmt.setDate(11, new Date(DateUtil.currentGMTTime().getTime()));
            stmt.setLong(12, podId);
            stmt.setString(13, Status.Up.toString());
            stmt.setLong(14, clusterId);
            stmt.executeUpdate();

        } catch (SQLException ex) {
            System.out.println("Error creating storage pool: " + ex.getMessage());
            s_logger.error("error creating storage pool ", ex);
            return;
        }

    }

    private void saveZone() {
        long id = Long.parseLong(_currentObjectParams.get("id"));
        String name = _currentObjectParams.get("name");
        //String description = _currentObjectParams.get("description");
        String dns1 = _currentObjectParams.get("dns1");
        String dns2 = _currentObjectParams.get("dns2");
        String internalDns1 = _currentObjectParams.get("internalDns1");
        String internalDns2 = _currentObjectParams.get("internalDns2");
        //String vnetRange = _currentObjectParams.get("vnet");
        String guestNetworkCidr = _currentObjectParams.get("guestNetworkCidr");
        String networkType = _currentObjectParams.get("networktype");

        // Check that all IPs are valid
        String ipError = "Please enter a valid IP address for the field: ";
        if (!IPRangeConfig.validOrBlankIP(dns1)) {
            printError(ipError + "dns1");
        }
        if (!IPRangeConfig.validOrBlankIP(dns2)) {
            printError(ipError + "dns2");
        }
        if (!IPRangeConfig.validOrBlankIP(internalDns1)) {
            printError(ipError + "internalDns1");
        }
        if (!IPRangeConfig.validOrBlankIP(internalDns2)) {
            printError(ipError + "internalDns2");
        }
        if (!IPRangeConfig.validCIDR(guestNetworkCidr)) {
            printError("Please enter a valid value for guestNetworkCidr");
        }

        pzc.saveZone(false, id, name, dns1, dns2, internalDns1, internalDns2, guestNetworkCidr, networkType);
    }

    private void savePhysicalNetwork() {
        long id = Long.parseLong(_currentObjectParams.get("id"));
        String zoneId = _currentObjectParams.get("zoneId");
        String vnetRange = _currentObjectParams.get("vnet");

        int vnetStart = -1;
        int vnetEnd = -1;
        if (vnetRange != null) {
            String[] tokens = vnetRange.split("-");
            vnetStart = Integer.parseInt(tokens[0]);
            vnetEnd = Integer.parseInt(tokens[1]);
        }
        long zoneDbId = Long.parseLong(zoneId);
        pzc.savePhysicalNetwork(false, id, zoneDbId, vnetStart, vnetEnd);

    }

    private void savePhysicalNetworkServiceProvider() {
        long id = Long.parseLong(_currentObjectParams.get("id"));
        long physicalNetworkId = Long.parseLong(_currentObjectParams.get("physicalNetworkId"));
        String providerName = _currentObjectParams.get("providerName");
        long destPhysicalNetworkId = Long.parseLong(_currentObjectParams.get("destPhysicalNetworkId"));
        String uuid = UUID.randomUUID().toString();

        int vpn = Integer.parseInt(_currentObjectParams.get("vpn"));
        int dhcp = Integer.parseInt(_currentObjectParams.get("dhcp"));
        int dns = Integer.parseInt(_currentObjectParams.get("dns"));
        int gateway = Integer.parseInt(_currentObjectParams.get("gateway"));
        int firewall = Integer.parseInt(_currentObjectParams.get("firewall"));
        int sourceNat = Integer.parseInt(_currentObjectParams.get("sourceNat"));
        int lb = Integer.parseInt(_currentObjectParams.get("loadBalance"));
        int staticNat = Integer.parseInt(_currentObjectParams.get("staticNat"));
        int pf = Integer.parseInt(_currentObjectParams.get("portForwarding"));
        int userData = Integer.parseInt(_currentObjectParams.get("userData"));
        int securityGroup = Integer.parseInt(_currentObjectParams.get("securityGroup"));

        String insertSql1 =
            "INSERT INTO `physical_network_service_providers` (`id`, `uuid`, `physical_network_id` , `provider_name`, `state` ,"
                + "`destination_physical_network_id`, `vpn_service_provided`, `dhcp_service_provided`, `dns_service_provided`, `gateway_service_provided`,"
                + "`firewall_service_provided`, `source_nat_service_provided`, `load_balance_service_provided`, `static_nat_service_provided`,"
                + "`port_forwarding_service_provided`, `user_data_service_provided`, `security_group_service_provided`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql1);
            stmt.setLong(1, id);
            stmt.setString(2, uuid);
            stmt.setLong(3, physicalNetworkId);
            stmt.setString(4, providerName);
            stmt.setString(5, "Enabled");
            stmt.setLong(6, destPhysicalNetworkId);
            stmt.setInt(7, vpn);
            stmt.setInt(8, dhcp);
            stmt.setInt(9, dns);
            stmt.setInt(10, gateway);
            stmt.setInt(11, firewall);
            stmt.setInt(12, sourceNat);
            stmt.setInt(13, lb);
            stmt.setInt(14, staticNat);
            stmt.setInt(15, pf);
            stmt.setInt(16, userData);
            stmt.setInt(17, securityGroup);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            System.out.println("Error creating physical network service provider: " + ex.getMessage());
            s_logger.error("error creating physical network service provider", ex);
            return;
        }

    }

    private void saveVirtualRouterProvider() {
        long id = Long.parseLong(_currentObjectParams.get("id"));
        long nspId = Long.parseLong(_currentObjectParams.get("nspId"));
        String uuid = UUID.randomUUID().toString();
        String type = _currentObjectParams.get("type");

        String insertSql1 = "INSERT INTO `virtual_router_providers` (`id`, `nsp_id`, `uuid` , `type` , `enabled`) " + "VALUES (?,?,?,?,?)";

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql1);
            stmt.setLong(1, id);
            stmt.setLong(2, nspId);
            stmt.setString(3, uuid);
            stmt.setString(4, type);
            stmt.setInt(5, 1);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            System.out.println("Error creating virtual router provider: " + ex.getMessage());
            s_logger.error("error creating virtual router provider ", ex);
            return;
        }

    }

    private void saveVlan() {
        String zoneId = _currentObjectParams.get("zoneId");
        String physicalNetworkIdStr = _currentObjectParams.get("physicalNetworkId");
        String vlanId = _currentObjectParams.get("vlanId");
        String gateway = _currentObjectParams.get("gateway");
        String netmask = _currentObjectParams.get("netmask");
        String publicIpRange = _currentObjectParams.get("ipAddressRange");
        String vlanType = _currentObjectParams.get("vlanType");
        String vlanPodName = _currentObjectParams.get("podName");

        String ipError = "Please enter a valid IP address for the field: ";
        if (!IPRangeConfig.validOrBlankIP(gateway)) {
            printError(ipError + "gateway");
        }
        if (!IPRangeConfig.validOrBlankIP(netmask)) {
            printError(ipError + "netmask");
        }

        // Check that the given IP address range was valid
        if (!checkIpAddressRange(publicIpRange)) {
            printError("Please enter a valid public IP range.");
        }

        // Split the IP address range
        String[] ipAddressRangeArray = publicIpRange.split("\\-");
        String startIP = ipAddressRangeArray[0];
        String endIP = null;
        if (ipAddressRangeArray.length > 1) {
            endIP = ipAddressRangeArray[1];
        }

        // If a netmask was provided, check that the startIP, endIP, and gateway all belong to the same subnet
        if (netmask != null && !netmask.equals("")) {
            if (endIP != null) {
                if (!IPRangeConfig.sameSubnet(startIP, endIP, netmask)) {
                    printError("Start and end IPs for the public IP range must be in the same subnet, as per the provided netmask.");
                }
            }

            if (gateway != null && !gateway.equals("")) {
                if (!IPRangeConfig.sameSubnet(startIP, gateway, netmask)) {
                    printError("The start IP for the public IP range must be in the same subnet as the gateway, as per the provided netmask.");
                }
                if (endIP != null) {
                    if (!IPRangeConfig.sameSubnet(endIP, gateway, netmask)) {
                        printError("The end IP for the public IP range must be in the same subnet as the gateway, as per the provided netmask.");
                    }
                }
            }
        }

        long zoneDbId = Long.parseLong(zoneId);
        String zoneName = PodZoneConfig.getZoneName(zoneDbId);

        long physicalNetworkId = Long.parseLong(physicalNetworkIdStr);

        //Set networkId to be 0, the value will be updated after management server starts up
        pzc.modifyVlan(zoneName, true, vlanId, gateway, netmask, vlanPodName, vlanType, publicIpRange, 0, physicalNetworkId);

        long vlanDbId = pzc.getVlanDbId(zoneName, vlanId);
        iprc.saveIPRange("public", -1, zoneDbId, vlanDbId, startIP, endIP, null, physicalNetworkId);

    }

    private void savePod() {
        long id = Long.parseLong(_currentObjectParams.get("id"));
        String name = _currentObjectParams.get("name");
        long dataCenterId = Long.parseLong(_currentObjectParams.get("zoneId"));
        String privateIpRange = _currentObjectParams.get("ipAddressRange");
        String gateway = _currentObjectParams.get("gateway");
        String cidr = _currentObjectParams.get("cidr");
        String zoneName = PodZoneConfig.getZoneName(dataCenterId);
        String startIP = null;
        String endIP = null;
        String vlanRange = _currentObjectParams.get("vnet");

        int vlanStart = -1;
        int vlanEnd = -1;
        if (vlanRange != null) {
            String[] tokens = vlanRange.split("-");
            vlanStart = Integer.parseInt(tokens[0]);
            vlanEnd = Integer.parseInt(tokens[1]);
        }

        // Get the individual cidrAddress and cidrSize values
        String[] cidrPair = cidr.split("\\/");
        String cidrAddress = cidrPair[0];
        String cidrSize = cidrPair[1];
        long cidrSizeNum = Long.parseLong(cidrSize);

        // Check that the gateway is in the same subnet as the CIDR
        if (!IPRangeConfig.sameSubnetCIDR(gateway, cidrAddress, cidrSizeNum)) {
            printError("For pod " + name + " in zone " + zoneName + " , please ensure that your gateway is in the same subnet as the  pod's CIDR address.");
        }

        pzc.savePod(false, id, name, dataCenterId, gateway, cidr, vlanStart, vlanEnd);

        if (privateIpRange != null) {
            // Check that the given IP address range was valid
            if (!checkIpAddressRange(privateIpRange)) {
                printError("Please enter a valid private IP range.");
            }

            String[] ipAddressRangeArray = privateIpRange.split("\\-");
            startIP = ipAddressRangeArray[0];
            endIP = null;
            if (ipAddressRangeArray.length > 1) {
                endIP = ipAddressRangeArray[1];
            }
        }

        // Check that the start IP and end IP match up with the CIDR
        if (!IPRangeConfig.sameSubnetCIDR(startIP, endIP, cidrSizeNum)) {
            printError("For pod " + name + " in zone " + zoneName + ", please ensure that your start IP and end IP are in the same subnet, as per the pod's CIDR size.");
        }

        if (!IPRangeConfig.sameSubnetCIDR(startIP, cidrAddress, cidrSizeNum)) {
            printError("For pod " + name + " in zone " + zoneName + ", please ensure that your start IP is in the same subnet as the pod's CIDR address.");
        }

        if (!IPRangeConfig.sameSubnetCIDR(endIP, cidrAddress, cidrSizeNum)) {
            printError("For pod " + name + " in zone " + zoneName + ", please ensure that your end IP is in the same subnet as the pod's CIDR address.");
        }

        if (privateIpRange != null) {
            // Save the IP address range
            iprc.saveIPRange("private", id, dataCenterId, -1, startIP, endIP, null, -1);
        }

    }

    @DB
    protected void saveServiceOffering() {
        long id = Long.parseLong(_currentObjectParams.get("id"));
        String name = _currentObjectParams.get("name");
        String displayText = _currentObjectParams.get("displayText");
        ProvisioningType provisioningType = ProvisioningType.valueOf(_currentObjectParams.get("provisioningType"));
        int cpu = Integer.parseInt(_currentObjectParams.get("cpu"));
        int ramSize = Integer.parseInt(_currentObjectParams.get("ramSize"));
        int speed = Integer.parseInt(_currentObjectParams.get("speed"));
        String useLocalStorageValue = _currentObjectParams.get("useLocalStorage");

//        int nwRate = Integer.parseInt(_currentObjectParams.get("nwRate"));
//        int mcRate = Integer.parseInt(_currentObjectParams.get("mcRate"));
        boolean ha = Boolean.parseBoolean(_currentObjectParams.get("enableHA"));
        boolean mirroring = Boolean.parseBoolean(_currentObjectParams.get("mirrored"));

        boolean useLocalStorage;
        if (useLocalStorageValue != null) {
            if (Boolean.parseBoolean(useLocalStorageValue)) {
                useLocalStorage = true;
            } else {
                useLocalStorage = false;
            }
        } else {
            useLocalStorage = false;
        }

        ServiceOfferingVO serviceOffering =
            new ServiceOfferingVO(name, cpu, ramSize, speed, null, null, ha, displayText,
                    provisioningType, useLocalStorage, false, null, false, null, false);

        Long bytesReadRate = Long.parseLong(_currentObjectParams.get("bytesReadRate"));
        if ((bytesReadRate != null) && (bytesReadRate > 0))
            serviceOffering.setBytesReadRate(bytesReadRate);
        Long bytesWriteRate = Long.parseLong(_currentObjectParams.get("bytesWriteRate"));
        if ((bytesWriteRate != null) && (bytesWriteRate > 0))
            serviceOffering.setBytesWriteRate(bytesWriteRate);
        Long iopsReadRate = Long.parseLong(_currentObjectParams.get("iopsReadRate"));
        if ((iopsReadRate != null) && (iopsReadRate > 0))
            serviceOffering.setIopsReadRate(iopsReadRate);
        Long iopsWriteRate = Long.parseLong(_currentObjectParams.get("iopsWriteRate"));
        if ((iopsWriteRate != null) && (iopsWriteRate > 0))
            serviceOffering.setIopsWriteRate(iopsWriteRate);

        ServiceOfferingDaoImpl dao = ComponentContext.inject(ServiceOfferingDaoImpl.class);
        try {
            dao.persist(serviceOffering);
        } catch (Exception e) {
            s_logger.error("error creating service offering", e);

        }
        /*
        String insertSql = "INSERT INTO `cloud`.`service_offering` (id, name, cpu, ram_size, speed, nw_rate, mc_rate, created, ha_enabled, mirrored, display_text, guest_ip_type, use_local_storage) " +
                "VALUES (" + id + ",'" + name + "'," + cpu + "," + ramSize + "," + speed + "," + nwRate + "," + mcRate + ",now()," + ha + "," + mirroring + ",'" + displayText + "','" + guestIpType + "','" + useLocalStorage + "')";

        Transaction txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            s_logger.error("error creating service offering", ex);
            return;
        }
         */
    }

    @DB
    protected void saveDiskOffering() {
        long id = Long.parseLong(_currentObjectParams.get("id"));
        String name = _currentObjectParams.get("name");
        String displayText = _currentObjectParams.get("displayText");
        ProvisioningType provisioningType = ProvisioningType.valueOf(_currentObjectParams.get("provisioningtype"));
        long diskSpace = Long.parseLong(_currentObjectParams.get("diskSpace"));
        diskSpace = diskSpace * 1024 * 1024;
//        boolean mirroring = Boolean.parseBoolean(_currentObjectParams.get("mirrored"));
        String tags = _currentObjectParams.get("tags");
        String useLocal = _currentObjectParams.get("useLocal");
        boolean local = false;
        if (useLocal != null) {
            local = Boolean.parseBoolean(useLocal);
        }

        if (tags != null && tags.length() > 0) {
            String[] tokens = tags.split(",");
            StringBuilder newTags = new StringBuilder();
            for (String token : tokens) {
                newTags.append(token.trim()).append(",");
            }
            newTags.delete(newTags.length() - 1, newTags.length());
            tags = newTags.toString();
        }
        DiskOfferingVO diskOffering = new DiskOfferingVO(name, displayText, provisioningType, diskSpace, tags, false, null, null, null);
        diskOffering.setUseLocalStorage(local);

        Long bytesReadRate = Long.parseLong(_currentObjectParams.get("bytesReadRate"));
        if (bytesReadRate != null && (bytesReadRate > 0))
            diskOffering.setBytesReadRate(bytesReadRate);
        Long bytesWriteRate = Long.parseLong(_currentObjectParams.get("bytesWriteRate"));
        if (bytesWriteRate != null && (bytesWriteRate > 0))
            diskOffering.setBytesWriteRate(bytesWriteRate);
        Long iopsReadRate = Long.parseLong(_currentObjectParams.get("iopsReadRate"));
        if (iopsReadRate != null && (iopsReadRate > 0))
            diskOffering.setIopsReadRate(iopsReadRate);
        Long iopsWriteRate = Long.parseLong(_currentObjectParams.get("iopsWriteRate"));
        if (iopsWriteRate != null && (iopsWriteRate > 0))
            diskOffering.setIopsWriteRate(iopsWriteRate);

        DiskOfferingDaoImpl offering = ComponentContext.inject(DiskOfferingDaoImpl.class);
        try {
            offering.persist(diskOffering);
        } catch (Exception e) {
            s_logger.error("error creating disk offering", e);

        }
        /*
        String insertSql = "INSERT INTO `cloud`.`disk_offering` (id, domain_id, name, display_text, disk_size, mirrored, tags) " +
                "VALUES (" + id + "," + domainId + ",'" + name + "','" + displayText + "'," + diskSpace + "," + mirroring + ", ? )";

        Transaction txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            stmt.setString(1, tags);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            s_logger.error("error creating disk offering", ex);
            return;
        }
         */
    }

    @DB
    protected void saveThrottlingRates() {
        boolean saveNetworkThrottlingRate = (_networkThrottlingRate != null);
        boolean saveMulticastThrottlingRate = (_multicastThrottlingRate != null);

        if (!saveNetworkThrottlingRate && !saveMulticastThrottlingRate) {
            return;
        }

        String insertNWRateSql = "UPDATE `cloud`.`service_offering` SET `nw_rate` = ?";
        String insertMCRateSql = "UPDATE `cloud`.`service_offering` SET `mc_rate` = ?";

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt;

            if (saveNetworkThrottlingRate) {
                stmt = txn.prepareAutoCloseStatement(insertNWRateSql);
                stmt.setString(1, _networkThrottlingRate);
                stmt.executeUpdate();
            }

            if (saveMulticastThrottlingRate) {
                stmt = txn.prepareAutoCloseStatement(insertMCRateSql);
                stmt.setString(1, _multicastThrottlingRate);
                stmt.executeUpdate();
            }

        } catch (SQLException ex) {
            s_logger.error("error saving network and multicast throttling rates to all service offerings", ex);
            return;
        }
    }

    // no configurable values for VM Template, hard-code the defaults for now
    private void saveVMTemplate() {
        /*
        long id = 1;
        String uniqueName = "routing";
        String name = "DomR Template";
        int isPublic = 0;
        String path = "template/private/u000000/os/routing";
        String type = "ext3";
        int requiresHvm = 0;
        int bits = 64;
        long createdByUserId = 1;
        int isReady = 1;

        String insertSql = "INSERT INTO `cloud`.`vm_template` (id, unique_name, name, public, path, created, type, hvm, bits, created_by, ready) " +
                "VALUES (" + id + ",'" + uniqueName + "','" + name + "'," + isPublic + ",'" + path + "',now(),'" + type + "'," +
                requiresHvm + "," + bits + "," + createdByUserId + "," + isReady + ")";

        Transaction txn = Transaction.open();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            s_logger.error("error creating vm template: " + ex);
        } finally {
            txn.close();
        }
         */
        /*
        // do it again for console proxy template
        id = 2;
        uniqueName = "consoleproxy";
        name = "Console Proxy Template";
        isPublic = 0;
        path = "template/private/u000000/os/consoleproxy";
        type = "ext3";

        insertSql = "INSERT INTO `cloud`.`vm_template` (id, unique_name, name, public, path, created, type, hvm, bits, created_by, ready) " +
        "VALUES (" + id + ",'" + uniqueName + "','" + name + "'," + isPublic + ",'" + path + "',now(),'" + type + "'," +
        requiresHvm + "," + bits + "," + createdByUserId + "," + isReady + ")";

        Transaction txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            s_logger.error("error creating vm template: " + ex);
        } finally {
            txn.close();
        }
         */
    }

    @DB
    protected void saveUser() {
        // insert system account
        final String insertSystemAccount = "INSERT INTO `cloud`.`account` (id, account_name, type, domain_id) VALUES (1, 'system', '1', '1')";
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSystemAccount);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            s_logger.error("error creating system account", ex);
        }

        // insert system user
        final String insertSystemUser =
            "INSERT INTO `cloud`.`user` (id, username, password, account_id, firstname, lastname, created)"
                + " VALUES (1, 'system', RAND(), 1, 'system', 'cloud', now())";
        txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSystemUser);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            s_logger.error("error creating system user", ex);
        }

        // insert admin user
        long id = Long.parseLong(_currentObjectParams.get("id"));
        String username = _currentObjectParams.get("username");
        String firstname = _currentObjectParams.get("firstname");
        String lastname = _currentObjectParams.get("lastname");
        String password = _currentObjectParams.get("password");
        String email = _currentObjectParams.get("email");

        if (email == null || email.equals("")) {
            printError("An email address for each user is required.");
        }

        String algorithm = "MD5";
        String pwDigest;
        try {
            pwDigest = DigestHelper.getPaddedDigest(algorithm, password);
        } catch (NoSuchAlgorithmException e) {
            s_logger.error("error saving user", e);
            return;
        }

        // create an account for the admin user first
        final String insertAdminAccount = "INSERT INTO `cloud`.`account` (id, account_name, type, domain_id) VALUES (?, ?, '1', '1')";
        txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertAdminAccount);
            stmt.setLong(1, id);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            s_logger.error("error creating account", ex);
        }

        // now insert the user
        final String insertUser =
                "INSERT INTO `cloud`.`user` (id, username, password, account_id, firstname, lastname, email, created) " + "VALUES (?,?,?, 2, ?,?,?,now())";
        txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertUser);
            stmt.setLong(1, id);
            stmt.setString(2, username);
            stmt.setString(3, pwDigest);
            stmt.setString(4, firstname);
            stmt.setString(5, lastname);
            stmt.setString(6, email);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            s_logger.error("error creating user", ex);
        }
    }

    private void saveDefaultConfiguations() {
        for (String name : s_defaultConfigurationValues.keySet()) {
            String value = s_defaultConfigurationValues.get(name);
            saveConfiguration(name, value, null);
        }
    }

    private void saveConfiguration() {
        String name = _currentObjectParams.get("name");
        String value = _currentObjectParams.get("value");
        String category = _currentObjectParams.get("category");
        saveConfiguration(name, value, category);
    }

    @DB
    protected void saveConfiguration(String name, String value, String category) {
        String instance = "DEFAULT";
        String description = s_configurationDescriptions.get(name);
        String component = s_configurationComponents.get(name);
        if (category == null) {
            category = "Advanced";
        }

        String instanceNameError = "Please enter a non-blank value for the field: ";
        if (name.equals("instance.name")) {
            if (value == null || value.isEmpty() || !value.matches("^[A-Za-z0-9]{1,8}$")) {
                printError(instanceNameError + "configuration: instance.name can not be empty and can only contain numbers and alphabets up to 8 characters long");
            }
        }

        if (name.equals("network.throttling.rate")) {
            if (value != null && !value.isEmpty()) {
                _networkThrottlingRate = value;
            }
        }

        if (name.equals("multicast.throttling.rate")) {
            if (value != null && !value.isEmpty()) {
                _multicastThrottlingRate = value;
            }
        }

        String insertSql =
                "INSERT INTO `cloud`.`configuration` (instance, component, name, value, description, category) " +
                "VALUES (?,?,?,?,?,?)";
        String selectSql = "SELECT name FROM cloud.configuration WHERE name = ?";

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
            stmt.setString(1, name);
            ResultSet result = stmt.executeQuery();
            Boolean hasRow = result.next();
            if (!hasRow) {
                stmt = txn.prepareAutoCloseStatement(insertSql);
                stmt.setString(1, instance);
                stmt.setString(2, component);
                stmt.setString(3, name);
                stmt.setString(4, value);
                stmt.setString(5, description);
                stmt.setString(6, category);
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            s_logger.error("error creating configuration", ex);
        }
    }

    private boolean checkIpAddressRange(String ipAddressRange) {
        String[] ipAddressRangeArray = ipAddressRange.split("\\-");
        String startIP = ipAddressRangeArray[0];
        String endIP = null;
        if (ipAddressRangeArray.length > 1) {
            endIP = ipAddressRangeArray[1];
        }

        if (!IPRangeConfig.validIP(startIP)) {
            s_logger.error("The private IP address: " + startIP + " is invalid.");
            return false;
        }

        if (!IPRangeConfig.validOrBlankIP(endIP)) {
            s_logger.error("The private IP address: " + endIP + " is invalid.");
            return false;
        }

        if (!IPRangeConfig.validIPRange(startIP, endIP)) {
            s_logger.error("The  IP range " + startIP + " -> " + endIP + " is invalid.");
            return false;
        }

        return true;
    }

    @DB
    protected void saveRootDomain() {
        String insertSql = "insert into `cloud`.`domain` (id, name, parent, owner, path, level) values (1, 'ROOT', NULL, 2, '/', 0)";
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            s_logger.error("error creating ROOT domain", ex);
        }

        /*
        String updateSql = "update account set domain_id = 1 where id = 2";
        Transaction txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareStatement(updateSql);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            s_logger.error("error updating admin user", ex);
        } finally {
            txn.close();
        }

        updateSql = "update account set domain_id = 1 where id = 1";
        Transaction txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareStatement(updateSql);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            s_logger.error("error updating system user", ex);
        } finally {
            txn.close();
        }
         */
    }

    class DbConfigXMLHandler extends DefaultHandler {
        private DatabaseConfig _parent = null;

        public void setParent(DatabaseConfig parent) {
            _parent = parent;
        }

        @Override
        public void endElement(String s, String s1, String s2) throws SAXException {
            if (DatabaseConfig.objectNames.contains(s2) || "object".equals(s2)) {
                _parent.saveCurrentObject();
            } else if (DatabaseConfig.fieldNames.contains(s2) || "field".equals(s2)) {
                _currentFieldName = null;
            }
        }

        @Override
        public void startElement(String s, String s1, String s2, Attributes attributes) throws SAXException {
            if ("object".equals(s2)) {
                _parent.setCurrentObjectName(convertName(attributes.getValue("name")));
            } else if ("field".equals(s2)) {
                if (_currentObjectParams == null) {
                    _currentObjectParams = new HashMap<String, String>();
                }
                _currentFieldName = convertName(attributes.getValue("name"));
            } else if (DatabaseConfig.objectNames.contains(s2)) {
                _parent.setCurrentObjectName(s2);
            } else if (DatabaseConfig.fieldNames.contains(s2)) {
                if (_currentObjectParams == null) {
                    _currentObjectParams = new HashMap<String, String>();
                }
                _currentFieldName = s2;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if ((_currentObjectParams != null) && (_currentFieldName != null)) {
                String currentFieldVal = new String(ch, start, length);
                _currentObjectParams.put(_currentFieldName, currentFieldVal);
            }
        }

        private String convertName(String name) {
            if (name.contains(".")) {
                String[] nameArray = name.split("\\.");
                for (int i = 1; i < nameArray.length; i++) {
                    String word = nameArray[i];
                    nameArray[i] = word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
                }
                name = "";
                for (int i = 0; i < nameArray.length; i++) {
                    name = name.concat(nameArray[i]);
                }
            }
            return name;
        }
    }

    public static List<String> genReturnList(String success, String message) {
        List<String> returnList = new ArrayList<String>(2);
        returnList.add(0, success);
        returnList.add(1, message);
        return returnList;
    }

    public static void printError(String message) {
        System.out.println(message);
        System.exit(1);
    }

    public static String getDatabaseValueString(String selectSql, String name, String errorMsg) {
        TransactionLegacy txn = TransactionLegacy.open("getDatabaseValueString");
        PreparedStatement stmt = null;

        try {
            stmt = txn.prepareAutoCloseStatement(selectSql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String value = rs.getString(name);
                return value;
            } else {
                return null;
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e.getMessage());
            printError(errorMsg);
        } finally {
            txn.close();
        }
        return null;
    }

    public static long getDatabaseValueLong(String selectSql, String name, String errorMsg) {
        TransactionLegacy txn = TransactionLegacy.open("getDatabaseValueLong");
        PreparedStatement stmt = null;

        try {
            stmt = txn.prepareAutoCloseStatement(selectSql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(name);
            } else {
                return -1;
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e.getMessage());
            printError(errorMsg);
        } finally {
            txn.close();
        }
        return -1;
    }

    public static void saveSQL(String sql, String errorMsg) {
        TransactionLegacy txn = TransactionLegacy.open("saveSQL");
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(sql);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            System.out.println("SQL Exception: " + ex.getMessage());
            printError(errorMsg);
        } finally {
            txn.close();
        }
    }

}
