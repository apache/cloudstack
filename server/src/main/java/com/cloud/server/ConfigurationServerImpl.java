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
package com.cloud.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigDepotAdmin;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceOwnerType;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.guru.ControlNetworkGuru;
import com.cloud.network.guru.DirectPodBasedNetworkGuru;
import com.cloud.network.guru.PodBasedNetworkGuru;
import com.cloud.network.guru.PublicNetworkGuru;
import com.cloud.network.guru.StorageNetworkGuru;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.test.IPRangeConfig;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;

public class ConfigurationServerImpl extends ManagerBase implements ConfigurationServer {
    public static final Logger s_logger = Logger.getLogger(ConfigurationServerImpl.class);

    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private DataCenterDao _zoneDao;
    @Inject
    private HostPodDao _podDao;
    @Inject
    private DiskOfferingDao _diskOfferingDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    private NetworkOfferingDao _networkOfferingDao;
    @Inject
    private DataCenterDao _dataCenterDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private VlanDao _vlanDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private ResourceCountDao _resourceCountDao;
    @Inject
    private NetworkOfferingServiceMapDao _ntwkOfferingServiceMapDao;
    @Inject
    protected ConfigDepotAdmin _configDepotAdmin;
    @Inject
    protected ConfigDepot _configDepot;
    @Inject
    protected ConfigurationManager _configMgr;
    @Inject
    protected ManagementService _mgrService;


    public ConfigurationServerImpl() {
        setRunLevel(ComponentLifecycle.RUN_LEVEL_FRAMEWORK_BOOTSTRAP);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            persistDefaultValues();
            _configDepotAdmin.populateConfigurations();
        } catch (InternalErrorException e) {
            throw new RuntimeException("Unhandled configuration exception", e);
        }
        return true;
    }

    @Override
    public void persistDefaultValues() throws InternalErrorException {

        // Create system user and admin user
        saveUser();

        // Get init
        String init = _configDao.getValue("init");

        if (init == null || init.equals("false")) {
            s_logger.debug("ConfigurationServer is saving default values to the database.");

            // Save default Configuration Table values
            List<String> categories = Config.getCategories();
            for (String category : categories) {
                // If this is not a premium environment, don't insert premium configuration values
                if (!_configDao.isPremium() && category.equals("Premium")) {
                    continue;
                }

                List<Config> configs = Config.getConfigs(category);
                for (Config c : configs) {
                    String name = c.key();

                    // if the config value already present in the db, don't insert it again
                    if (_configDao.findByName(name) != null) {
                        continue;
                    }

                    String instance = "DEFAULT";
                    String component = c.getComponent();
                    String value = c.getDefaultValue();
                    String description = c.getDescription();
                    ConfigurationVO configVO = new ConfigurationVO(category, instance, component, name, value, description);
                    configVO.setDefaultValue(value);
                    _configDao.persist(configVO);
                }
            }

            _configDao.update(Config.UseSecondaryStorageVm.key(), Config.UseSecondaryStorageVm.getCategory(), "true");
            s_logger.debug("ConfigurationServer made secondary storage vm required.");

            _configDao.update(Config.SecStorageEncryptCopy.key(), Config.SecStorageEncryptCopy.getCategory(), "false");
            s_logger.debug("ConfigurationServer made secondary storage copy encrypt set to false.");

            _configDao.update("secstorage.secure.copy.cert", "realhostip");
            s_logger.debug("ConfigurationServer made secondary storage copy use realhostip.");

            _configDao.update("user.password.encoders.exclude", "MD5,LDAP,PLAINTEXT");
            s_logger.debug("Configuration server excluded insecure encoders");

            _configDao.update("user.authenticators.exclude", "PLAINTEXT");
            s_logger.debug("Configuration server excluded plaintext authenticator");

            // Save default service offerings
            createServiceOffering(User.UID_SYSTEM, "Small Instance", 1, 512, 500, "Small Instance", ProvisioningType.THIN, false, false, null);
            createServiceOffering(User.UID_SYSTEM, "Medium Instance", 1, 1024, 1000, "Medium Instance", ProvisioningType.THIN, false, false, null);
            // Save default disk offerings
            createDefaultDiskOffering("Small", "Small Disk, 5 GB", ProvisioningType.THIN, 5, null, false, false);
            createDefaultDiskOffering("Medium", "Medium Disk, 20 GB", ProvisioningType.THIN, 20, null, false, false);
            createDefaultDiskOffering("Large", "Large Disk, 100 GB", ProvisioningType.THIN, 100, null, false, false);
            createDefaultDiskOffering("Large", "Large Disk, 100 GB", ProvisioningType.THIN, 100, null, false, false);
            createDefaultDiskOffering("Custom", "Custom Disk", ProvisioningType.THIN, 0, null, true, false);

            // Save the mount parent to the configuration table
            String mountParent = getMountParent();
            if (mountParent != null) {
                _configDao.update(Config.MountParent.key(), Config.MountParent.getCategory(), mountParent);
                s_logger.debug("ConfigurationServer saved \"" + mountParent + "\" as mount.parent.");
            } else {
                s_logger.debug("ConfigurationServer could not detect mount.parent.");
            }

            String hostIpAdr = NetUtils.getDefaultHostIp();
            boolean needUpdateHostIp = true;
            if (hostIpAdr != null) {
                Boolean devel = Boolean.valueOf(_configDao.getValue("developer"));
                if (devel) {
                    String value = _configDao.getValue(ApiServiceConfiguration.ManagementServerAddresses.key());
                    if (value != null && !value.equals("localhost")) {
                        needUpdateHostIp = false;
                    }
                }

                if (needUpdateHostIp) {
                    _configDepot.createOrUpdateConfigObject(ApiServiceConfiguration.class.getSimpleName(), ApiServiceConfiguration.ManagementServerAddresses, hostIpAdr);
                    s_logger.debug("ConfigurationServer saved \"" + hostIpAdr + "\" as host.");
                }
            }

            // generate a single sign-on key
            updateSSOKey();

            // Create default network offerings
            createDefaultNetworkOfferings();

            // Create default networks
            createDefaultNetworks();

            // Create userIpAddress ranges

            // Update existing vlans with networkId
            List<VlanVO> vlans = _vlanDao.listAll();
            if (vlans != null && !vlans.isEmpty()) {
                for (final VlanVO vlan : vlans) {
                    if (vlan.getNetworkId().longValue() == 0) {
                        updateVlanWithNetworkId(vlan);
                    }

                    // Create vlan user_ip_address range
                    String ipPange = vlan.getIpRange();
                    String[] range = ipPange.split("-");
                    final String startIp = range[0];
                    final String endIp = range[1];

                    Transaction.execute(new TransactionCallbackNoReturn() {
                        @Override
                        public void doInTransactionWithoutResult(TransactionStatus status) {
                            IPRangeConfig config = new IPRangeConfig();
                            long startIPLong = NetUtils.ip2Long(startIp);
                            long endIPLong = NetUtils.ip2Long(endIp);
                            config.savePublicIPRange(TransactionLegacy.currentTxn(), startIPLong, endIPLong, vlan.getDataCenterId(), vlan.getId(), vlan.getNetworkId(),
                                    vlan.getPhysicalNetworkId(), false);
                        }
                    });

                }
            }
        }
        // Update resource count if needed
        updateResourceCount();

        // store the public and private keys in the database
        updateKeyPairs();

        // generate a PSK to communicate with SSVM
        updateSecondaryStorageVMSharedKey();

        // generate a random password for system vm
        updateSystemvmPassword();

        // generate a random password used to authenticate zone-to-zone copy
        generateSecStorageVmCopyPassword();

        // Update the cloud identifier
        updateCloudIdentifier();

        _configDepotAdmin.populateConfigurations();
        // setup XenServer default PV driver version
        initiateXenServerPVDriverVersion();

        // We should not update seed data UUID column here since this will be invoked in upgrade case as well.
        //updateUuids();
        // Set init to true
        _configDao.update("init", "Hidden", "true");

        // invalidate cache in DAO as we have changed DB status
        _configDao.invalidateCache();
    }

    private void templateDetailsInitIfNotExist(long id, String name, String value) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement stmt = null;
        PreparedStatement stmtInsert = null;
        boolean insert = false;
        try {
            txn.start();
            stmt = txn.prepareAutoCloseStatement("SELECT id FROM vm_template_details WHERE template_id=? and name=?");
            stmt.setLong(1, id);
            stmt.setString(2, name);
            ResultSet rs = stmt.executeQuery();
            if (rs == null || !rs.next()) {
                insert = true;
            }
            stmt.close();

            if (insert) {
                stmtInsert = txn.prepareAutoCloseStatement("INSERT INTO vm_template_details(template_id, name, value) VALUES(?, ?, ?)");
                stmtInsert.setLong(1, id);
                stmtInsert.setString(2, name);
                stmtInsert.setString(3, value);
                if (stmtInsert.executeUpdate() < 1) {
                    throw new CloudRuntimeException("Unable to init template " + id + " datails: " + name);
                }
            }
            txn.commit();
        } catch (Exception e) {
            s_logger.warn("Unable to init template " + id + " datails: " + name, e);
            throw new CloudRuntimeException("Unable to init template " + id + " datails: " + name);
        }
    }

    private void initiateXenServerPVDriverVersion() {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                TransactionLegacy txn = TransactionLegacy.currentTxn();
                String pvdriverversion = Config.XenServerPVdriverVersion.getDefaultValue();
                PreparedStatement pstmt = null;
                ResultSet rs1 = null;
                ResultSet rs2 = null;
                try {
                    String oldValue = _configDao.getValue(Config.XenServerPVdriverVersion.key());
                    if (oldValue == null) {
                        String sql = "select resource from host where hypervisor_type='XenServer' and removed is null and status not in ('Error', 'Removed') group by resource";
                        pstmt = txn.prepareAutoCloseStatement(sql);
                        rs1 = pstmt.executeQuery();
                        while (rs1.next()) {
                            String resouce = rs1.getString(1); //resource column
                            if (resouce == null)
                                continue;
                            if (resouce.equalsIgnoreCase("com.cloud.hypervisor.xenserver.resource.XenServer56Resource")
                                    || resouce.equalsIgnoreCase("com.cloud.hypervisor.xenserver.resource.XenServer56FP1Resource")
                                    || resouce.equalsIgnoreCase("com.cloud.hypervisor.xenserver.resource.XenServer56SP2Resource")
                                    || resouce.equalsIgnoreCase("com.cloud.hypervisor.xenserver.resource.XenServer600Resource")
                                    || resouce.equalsIgnoreCase("com.cloud.hypervisor.xenserver.resource.XenServer602Resource")) {
                                pvdriverversion = "xenserver56";
                                break;
                            }
                        }
                        _configDao.getValueAndInitIfNotExist(Config.XenServerPVdriverVersion.key(), Config.XenServerPVdriverVersion.getCategory(), pvdriverversion,
                                Config.XenServerPVdriverVersion.getDescription());
                        sql = "select id from vm_template where hypervisor_type='XenServer'  and format!='ISO' and removed is null";
                        pstmt = txn.prepareAutoCloseStatement(sql);
                        rs2 = pstmt.executeQuery();
                        List<Long> tmpl_ids = new ArrayList<Long>();
                        while (rs2.next()) {
                            tmpl_ids.add(rs2.getLong(1));
                        }
                        for (Long tmpl_id : tmpl_ids) {
                            templateDetailsInitIfNotExist(tmpl_id, "hypervisortoolsversion", pvdriverversion);
                        }
                    }
                } catch (Exception e) {
                    s_logger.debug("initiateXenServerPVDriverVersion failed due to " + e.toString());
                    // ignore
                }
            }
        });
    }

    private String getMountParent() {
        return getEnvironmentProperty("mount.parent");
    }

    private String getEnvironmentProperty(String name) {
        try {
            final File propsFile = PropertiesUtil.findConfigFile("environment.properties");

            if (propsFile == null) {
                return null;
            } else {
                final Properties props = new Properties();
                try(final FileInputStream finputstream = new FileInputStream(propsFile);) {
                    props.load(finputstream);
                }catch (IOException e) {
                    s_logger.error("getEnvironmentProperty:Exception:" + e.getMessage());
                }
                return props.getProperty("mount.parent");
            }
        } catch (Exception e) {
            return null;
        }
    }

    @DB
    public void saveUser() {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                TransactionLegacy txn = TransactionLegacy.currentTxn();
                // insert system account
                String insertSql = "INSERT INTO `cloud`.`account` (id, uuid, account_name, type, role_id, domain_id, account.default) VALUES (1, UUID(), 'system', '1', '1', '1', 1)";

                try {
                    PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
                    stmt.executeUpdate();
                } catch (SQLException ex) {
                    s_logger.debug("Looks like system account already exists");
                }
                // insert system user
                insertSql = "INSERT INTO `cloud`.`user` (id, uuid, username, password, account_id, firstname, lastname, created, user.default)"
                        + " VALUES (1, UUID(), 'system', RAND(), 1, 'system', 'cloud', now(), 1)";

                try {
                    PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
                    stmt.executeUpdate();
                } catch (SQLException ex) {
                    s_logger.debug("Looks like system user already exists");
                }

                // insert admin user, but leave the account disabled until we set a
                // password with the user authenticator
                long id = 2;
                String username = "admin";
                String firstname = "admin";
                String lastname = "cloud";

                // create an account for the admin user first
                insertSql = "INSERT INTO `cloud`.`account` (id, uuid, account_name, type, role_id, domain_id, account.default) VALUES (" + id + ", UUID(), '" + username
                        + "', '1', '1', '1', 1)";
                try {
                    PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
                    stmt.executeUpdate();
                } catch (SQLException ex) {
                    s_logger.debug("Looks like admin account already exists");
                }

                // now insert the user
                insertSql = "INSERT INTO `cloud`.`user` (id, uuid, username, password, account_id, firstname, lastname, created, state, user.default) " + "VALUES (" + id
                        + ", UUID(), '" + username + "', RAND(), 2, '" + firstname + "','" + lastname + "',now(), 'disabled', 1)";

                try {
                    PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
                    stmt.executeUpdate();
                } catch (SQLException ex) {
                    s_logger.debug("Looks like admin user already exists");
                }

                try {
                    String tableName = "security_group";
                    try {
                        String checkSql = "SELECT * from network_group";
                        PreparedStatement stmt = txn.prepareAutoCloseStatement(checkSql);
                        stmt.executeQuery();
                        tableName = "network_group";
                    } catch (Exception ex) {
                        // Ignore in case of exception, table must not exist
                    }

                    insertSql = "SELECT * FROM " + tableName + " where account_id=2 and name='default'";
                    PreparedStatement stmt = txn.prepareAutoCloseStatement(insertSql);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()) {
                        // save default security group
                        if (tableName.equals("security_group")) {
                            insertSql = "INSERT INTO " + tableName + " (uuid, name, description, account_id, domain_id) "
                                    + "VALUES (UUID(), 'default', 'Default Security Group', 2, 1)";
                        } else {
                            insertSql = "INSERT INTO " + tableName + " (name, description, account_id, domain_id, account_name) "
                                    + "VALUES ('default', 'Default Security Group', 2, 1, 'admin')";
                        }

                        try {
                            stmt = txn.prepareAutoCloseStatement(insertSql);
                            stmt.executeUpdate();
                        } catch (SQLException ex) {
                            s_logger.warn("Failed to create default security group for default admin account due to ", ex);
                        }
                    }
                    rs.close();
                } catch (Exception ex) {
                    s_logger.warn("Failed to create default security group for default admin account due to ", ex);
                }
            }
        });
    }

    protected void updateCloudIdentifier() {
        // Creates and saves a UUID as the cloud identifier
        String currentCloudIdentifier = _configDao.getValue("cloud.identifier");
        if (currentCloudIdentifier == null || currentCloudIdentifier.isEmpty()) {
            String uuid = UUID.randomUUID().toString();
            _configDao.update(Config.CloudIdentifier.key(), Config.CloudIdentifier.getCategory(), uuid);
        }
    }

    @DB
    protected void updateSystemvmPassword() {
        String userid = System.getProperty("user.name");
        if (!userid.startsWith("cloud")) {
            return;
        }

        if (!Boolean.valueOf(_configDao.getValue("system.vm.random.password"))) {
            return;
        }

        String already = _configDao.getValue("system.vm.password");
        if (already == null) {
            TransactionLegacy txn = TransactionLegacy.currentTxn();
            try {
                String rpassword = _mgrService.generateRandomPassword();
                String wSql = "INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) "
                + "VALUES ('Secure','DEFAULT', 'management-server','system.vm.password', ?,'randmon password generated each management server starts for system vm')";
                PreparedStatement stmt = txn.prepareAutoCloseStatement(wSql);
                stmt.setString(1, DBEncryptionUtil.encrypt(rpassword));
                stmt.executeUpdate();
                s_logger.info("Updated systemvm password in database");
            } catch (SQLException e) {
                s_logger.error("Cannot retrieve systemvm password", e);
            }
        }

    }

    @Override
    @DB
    public void updateKeyPairs() {
        // Grab the SSH key pair and insert it into the database, if it is not present

        String username = System.getProperty("user.name");
        Boolean devel = Boolean.valueOf(_configDao.getValue("developer"));
        if (!username.equalsIgnoreCase("cloud") && !devel) {
            s_logger.warn("Systemvm keypairs could not be set. Management server should be run as cloud user, or in development mode.");
            return;
        }
        String already = _configDao.getValue("ssh.privatekey");
        String homeDir = System.getProperty("user.home");
        if (homeDir == null) {
            throw new CloudRuntimeException("Cannot get home directory for account: " + username);
        }

        if (s_logger.isInfoEnabled()) {
            s_logger.info("Processing updateKeyPairs");
        }

        if (homeDir != null && homeDir.startsWith("~")) {
            s_logger.error("No home directory was detected for the user '" + username + "'. Please check the profile of this user.");
            throw new CloudRuntimeException("No home directory was detected for the user '" + username + "'. Please check the profile of this user.");
        }

        // Using non-default file names (id_rsa.cloud and id_rsa.cloud.pub) in developer mode. This is to prevent SSH keys overwritten for user running management server
        File privkeyfile = null;
        File pubkeyfile = null;
        if (devel) {
            privkeyfile = new File(homeDir + "/.ssh/id_rsa.cloud");
            pubkeyfile = new File(homeDir + "/.ssh/id_rsa.cloud.pub");
        } else {
            privkeyfile = new File(homeDir + "/.ssh/id_rsa");
            pubkeyfile = new File(homeDir + "/.ssh/id_rsa.pub");
        }

        if (already == null || already.isEmpty()) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Systemvm keypairs not found in database. Need to store them in the database");
            }
            // FIXME: take a global database lock here for safety.
            boolean onWindows = isOnWindows();
            if(!onWindows) {
                Script.runSimpleBashScript("if [ -f " + privkeyfile + " ]; then rm -f " + privkeyfile + "; fi; ssh-keygen -t rsa -m PEM -N '' -f " + privkeyfile + " -q 2>/dev/null || ssh-keygen -t rsa -N '' -f " + privkeyfile + " -q");
            }

            final String privateKey;
            final String publicKey;
            try {
                privateKey = new String(Files.readAllBytes(privkeyfile.toPath()));
            } catch (IOException e) {
                s_logger.error("Cannot read the private key file", e);
                throw new CloudRuntimeException("Cannot read the private key file");
            }
            try {
                publicKey = new String(Files.readAllBytes(pubkeyfile.toPath()));
            } catch (IOException e) {
                s_logger.error("Cannot read the public key file", e);
                throw new CloudRuntimeException("Cannot read the public key file");
            }

            final String insertSql1 =
                    "INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) " +
                            "VALUES ('Hidden','DEFAULT', 'management-server','ssh.privatekey', '" + DBEncryptionUtil.encrypt(privateKey) +
                            "','Private key for the entire CloudStack')";
            final String insertSql2 =
                    "INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) " +
                            "VALUES ('Hidden','DEFAULT', 'management-server','ssh.publickey', '" + DBEncryptionUtil.encrypt(publicKey) +
                            "','Public key for the entire CloudStack')";

            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {

                    TransactionLegacy txn = TransactionLegacy.currentTxn();
                    try {
                        PreparedStatement stmt1 = txn.prepareAutoCloseStatement(insertSql1);
                        stmt1.executeUpdate();
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Private key inserted into database");
                        }
                    } catch (SQLException ex) {
                        s_logger.error("SQL of the private key failed", ex);
                        throw new CloudRuntimeException("SQL of the private key failed");
                    }

                    try {
                        PreparedStatement stmt2 = txn.prepareAutoCloseStatement(insertSql2);
                        stmt2.executeUpdate();
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Public key inserted into database");
                        }
                    } catch (SQLException ex) {
                        s_logger.error("SQL of the public key failed", ex);
                        throw new CloudRuntimeException("SQL of the public key failed");
                    }
                }
            });

        } else {
            s_logger.info("Keypairs already in database, updating local copy");
            updateKeyPairsOnDisk(homeDir);
        }
        s_logger.info("Going to update systemvm iso with generated keypairs if needed");
        try {
            injectSshKeysIntoSystemVmIsoPatch(pubkeyfile.getAbsolutePath(), privkeyfile.getAbsolutePath());
        } catch (CloudRuntimeException e) {
            if (!devel) {
                throw new CloudRuntimeException(e.getMessage());
            }
        }
    }

    @Override
    public List<ConfigurationVO> getConfigListByScope(String scope, Long resourceId) {

        // Getting the list of parameters defined at the scope
        Set<ConfigKey<?>> configList = _configDepot.getConfigListByScope(scope);
        List<ConfigurationVO> configVOList = new ArrayList<ConfigurationVO>();
        for (ConfigKey<?> param : configList) {
            ConfigurationVO configVo = _configDao.findByName(param.toString());
            configVo.setValue(_configDepot.get(param.toString()).valueIn(resourceId).toString());
            configVOList.add(configVo);
        }
        return configVOList;
    }

    private void writeKeyToDisk(String key, String keyPath) {
        File keyfile = new File(keyPath);
        if (!keyfile.exists()) {
            try {
                keyfile.createNewFile();
            } catch (IOException e) {
                s_logger.warn("Failed to create file: " + e.toString());
                throw new CloudRuntimeException("Failed to update keypairs on disk: cannot create  key file " + keyPath);
            }
        }

        if (keyfile.exists()) {
            try (FileOutputStream kStream = new FileOutputStream(keyfile);){
                if (kStream != null) {
                    kStream.write(key.getBytes());
                }
            } catch (FileNotFoundException e) {
                s_logger.warn("Failed to write  key to " + keyfile.getAbsolutePath(), e);
                throw new CloudRuntimeException("Failed to update keypairs on disk: cannot find  key file " + keyPath);
            } catch (IOException e) {
                s_logger.warn("Failed to write  key to " + keyfile.getAbsolutePath(), e);
                throw new CloudRuntimeException("Failed to update keypairs on disk: cannot write to  key file " + keyPath);
            }
        }

    }

    private void updateKeyPairsOnDisk(String homeDir) {
        File keyDir = new File(homeDir + "/.ssh");
        Boolean devel = Boolean.valueOf(_configDao.getValue("developer"));
        if (!keyDir.isDirectory()) {
            s_logger.warn("Failed to create " + homeDir + "/.ssh for storing the SSH keypars");
            keyDir.mkdirs();
        }
        String pubKey = _configDao.getValue("ssh.publickey");
        String prvKey = _configDao.getValue("ssh.privatekey");

        // Using non-default file names (id_rsa.cloud and id_rsa.cloud.pub) in developer mode. This is to prevent SSH keys overwritten for user running management server
        if (devel) {
            writeKeyToDisk(prvKey, homeDir + "/.ssh/id_rsa.cloud");
            writeKeyToDisk(pubKey, homeDir + "/.ssh/id_rsa.cloud.pub");
        } else {
            writeKeyToDisk(prvKey, homeDir + "/.ssh/id_rsa");
            writeKeyToDisk(pubKey, homeDir + "/.ssh/id_rsa.pub");
        }
    }

    protected void injectSshKeysIntoSystemVmIsoPatch(String publicKeyPath, String privKeyPath) {
        s_logger.info("Trying to inject public and private keys into systemvm iso");
        String injectScript = getInjectScript();
        String scriptPath = Script.findScript("", injectScript);
        String systemVmIsoPath = Script.findScript("", "vms/systemvm.iso");
        if (scriptPath == null) {
            throw new CloudRuntimeException("Unable to find key inject script " + injectScript);
        }
        if (systemVmIsoPath == null) {
            throw new CloudRuntimeException("Unable to find systemvm iso vms/systemvm.iso");
        }
        Script command = null;
        if(isOnWindows()) {
            command = new Script("python", s_logger);
        } else {
            command = new Script("/bin/bash", s_logger);
        }
        if (isOnWindows()) {
            scriptPath = scriptPath.replaceAll("\\\\" ,"/" );
            systemVmIsoPath = systemVmIsoPath.replaceAll("\\\\" ,"/" );
            publicKeyPath = publicKeyPath.replaceAll("\\\\" ,"/" );
            privKeyPath = privKeyPath.replaceAll("\\\\" ,"/" );
        }
        command.add(scriptPath);
        command.add(publicKeyPath);
        command.add(privKeyPath);
        command.add(systemVmIsoPath);

        final String result = command.execute();
        s_logger.info("The script injectkeys.sh was run with result : " + result);
        if (result != null) {
            s_logger.warn("The script injectkeys.sh failed to run successfully : " + result);
            throw new CloudRuntimeException("The script injectkeys.sh failed to run successfully : " + result);
        }
    }

    protected String getInjectScript() {
        String injectScript = null;
        boolean onWindows = isOnWindows();
        if(onWindows) {
            injectScript = "scripts/vm/systemvm/injectkeys.py";
        } else {
            injectScript = "scripts/vm/systemvm/injectkeys.sh";
        }
        return injectScript;
    }

    protected boolean isOnWindows() {
        String os = System.getProperty("os.name", "generic").toLowerCase();
        boolean onWindows = (os != null && os.startsWith("windows"));
        return onWindows;
    }

    @DB
    protected void generateSecStorageVmCopyPassword() {
        String already = _configDao.getValue("secstorage.copy.password");

        if (already == null) {

            s_logger.info("Need to store secondary storage vm copy password in the database");
            String password = PasswordGenerator.generateRandomPassword(12);

            final String insertSql1 =
                    "INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) " +
                            "VALUES ('Hidden','DEFAULT', 'management-server','secstorage.copy.password', '" + DBEncryptionUtil.encrypt(password) +
                            "','Password used to authenticate zone-to-zone template copy requests')";
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {

                    TransactionLegacy txn = TransactionLegacy.currentTxn();
                    try {
                        PreparedStatement stmt1 = txn.prepareAutoCloseStatement(insertSql1);
                        stmt1.executeUpdate();
                        s_logger.debug("secondary storage vm copy password inserted into database");
                    } catch (SQLException ex) {
                        s_logger.warn("Failed to insert secondary storage vm copy password", ex);
                    }
                }
            });
        }
    }

    private void updateSSOKey() {
        try {
            _configDao.update(Config.SSOKey.key(), Config.SSOKey.getCategory(), getPrivateKey());
        } catch (NoSuchAlgorithmException ex) {
            s_logger.error("error generating sso key", ex);
        }
    }

    /**
     * preshared key to be used by management server to communicate with SSVM during volume/template upload
     */
    private void updateSecondaryStorageVMSharedKey() {
        try {
            ConfigurationVO configInDB = _configDao.findByName(Config.SSVMPSK.key());
            if(configInDB == null) {
                ConfigurationVO configVO = new ConfigurationVO(Config.SSVMPSK.getCategory(), "DEFAULT", Config.SSVMPSK.getComponent(), Config.SSVMPSK.key(), getPrivateKey(),
                        Config.SSVMPSK.getDescription());
                s_logger.info("generating a new SSVM PSK. This goes to SSVM on Start");
                _configDao.persist(configVO);
            } else if (StringUtils.isEmpty(configInDB.getValue())) {
                s_logger.info("updating the SSVM PSK with new value. This goes to SSVM on Start");
                _configDao.update(Config.SSVMPSK.key(), Config.SSVMPSK.getCategory(), getPrivateKey());
            }
        } catch (NoSuchAlgorithmException ex) {
            s_logger.error("error generating ssvm psk", ex);
        }
    }

    private String getPrivateKey() throws NoSuchAlgorithmException {
        String encodedKey = null;
        // Algorithm for generating Key is SHA1, should this be configurable?
        KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
        SecretKey key = generator.generateKey();
        encodedKey = Base64.encodeBase64URLSafeString(key.getEncoded());
        return encodedKey;

    }


    @DB
    protected HostPodVO createPod(long userId, String podName, final long zoneId, String gateway, String cidr, final String startIp, String endIp)
            throws InternalErrorException {
        String[] cidrPair = cidr.split("\\/");
        String cidrAddress = cidrPair[0];
        int cidrSize = Integer.parseInt(cidrPair[1]);

        if (startIp != null) {
            if (endIp == null) {
                endIp = NetUtils.getIpRangeEndIpFromCidr(cidrAddress, cidrSize);
            }
        }

        // Create the new pod in the database
        String ipRange;
        if (startIp != null) {
            ipRange = startIp + "-";
            if (endIp != null) {
                ipRange += endIp;
            }
        } else {
            ipRange = "";
        }

        final HostPodVO pod = new HostPodVO(podName, zoneId, gateway, cidrAddress, cidrSize, ipRange);
        try {
            final String endIpFinal = endIp;
            Transaction.execute(new TransactionCallbackWithExceptionNoReturn<InternalErrorException>() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) throws InternalErrorException {
                    if (_podDao.persist(pod) == null) {
                        throw new InternalErrorException("Failed to create new pod. Please contact Cloud Support.");
                    }

                    if (startIp != null) {
                        _zoneDao.addPrivateIpAddress(zoneId, pod.getId(), startIp, endIpFinal, false, null);
                    }

                    String ipNums = _configDao.getValue("linkLocalIp.nums");
                    int nums = Integer.parseInt(ipNums);
                    if (nums > 16 || nums <= 0) {
                        throw new InvalidParameterValueException("The linkLocalIp.nums: " + nums + "is wrong, should be 1~16");
                    }
                    /* local link ip address starts from 169.254.0.2 - 169.254.(nums) */
                    String[] linkLocalIpRanges = NetUtils.getLinkLocalIPRange(_configDao.getValue(Config.ControlCidr.key()));
                    _zoneDao.addLinkLocalIpAddress(zoneId, pod.getId(), linkLocalIpRanges[0], linkLocalIpRanges[1]);
                }
            });
        } catch (Exception e) {
            s_logger.error("Unable to create new pod due to " + e.getMessage(), e);
            throw new InternalErrorException("Failed to create new pod. Please contact Cloud Support.");
        }

        return pod;
    }

    private DiskOfferingVO createDefaultDiskOffering(String name, String description, ProvisioningType provisioningType,
                                                     int numGibibytes, String tags, boolean isCustomized, boolean isSystemUse) {
        long diskSize = numGibibytes;
        diskSize = diskSize * 1024 * 1024 * 1024;
        tags = cleanupTags(tags);

        DiskOfferingVO newDiskOffering = new DiskOfferingVO(name, description, provisioningType, diskSize, tags, isCustomized, null, null, null);
        newDiskOffering.setUniqueName("Cloud.Com-" + name);
        // leaving the above reference to cloud.com in as it is an identifyer and has no real world relevance
        newDiskOffering.setSystemUse(isSystemUse);
        newDiskOffering = _diskOfferingDao.persistDeafultDiskOffering(newDiskOffering);
        return newDiskOffering;
    }

    private ServiceOfferingVO createServiceOffering(long userId, String name, int cpu, int ramSize, int speed, String displayText,
            ProvisioningType provisioningType, boolean localStorageRequired, boolean offerHA, String tags) {
        tags = cleanupTags(tags);
        ServiceOfferingVO offering =
                new ServiceOfferingVO(name, cpu, ramSize, speed, null, null, offerHA, displayText, provisioningType, localStorageRequired, false, tags, false, null, false);
        offering.setUniqueName("Cloud.Com-" + name);
        // leaving the above reference to cloud.com in as it is an identifyer and has no real world relevance
        offering = _serviceOfferingDao.persistSystemServiceOffering(offering);
        return offering;
    }

    private String cleanupTags(String tags) {
        if (tags != null) {
            String[] tokens = tags.split(",");
            StringBuilder t = new StringBuilder();
            for (int i = 0; i < tokens.length; i++) {
                t.append(tokens[i].trim()).append(",");
            }
            t.delete(t.length() - 1, t.length());
            tags = t.toString();
        }

        return tags;
    }

    @DB
    protected void createDefaultNetworkOfferings() {

        NetworkOfferingVO publicNetworkOffering = new NetworkOfferingVO(NetworkOffering.SystemPublicNetwork, TrafficType.Public, true);
        publicNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(publicNetworkOffering);
        NetworkOfferingVO managementNetworkOffering = new NetworkOfferingVO(NetworkOffering.SystemManagementNetwork, TrafficType.Management, false);
        managementNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(managementNetworkOffering);
        NetworkOfferingVO controlNetworkOffering = new NetworkOfferingVO(NetworkOffering.SystemControlNetwork, TrafficType.Control, false);
        controlNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(controlNetworkOffering);
        NetworkOfferingVO storageNetworkOffering = new NetworkOfferingVO(NetworkOffering.SystemStorageNetwork, TrafficType.Storage, true);
        storageNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(storageNetworkOffering);
        NetworkOfferingVO privateGatewayNetworkOffering = new NetworkOfferingVO(NetworkOffering.SystemPrivateGatewayNetworkOffering, GuestType.Isolated);
        privateGatewayNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(privateGatewayNetworkOffering);

        //populate providers
        final Map<Network.Service, Network.Provider> defaultSharedNetworkOfferingProviders = new HashMap<Network.Service, Network.Provider>();
        defaultSharedNetworkOfferingProviders.put(Service.Dhcp, Provider.VirtualRouter);
        defaultSharedNetworkOfferingProviders.put(Service.Dns, Provider.VirtualRouter);
        defaultSharedNetworkOfferingProviders.put(Service.UserData, Provider.VirtualRouter);

        final Map<Network.Service, Network.Provider> defaultIsolatedNetworkOfferingProviders = defaultSharedNetworkOfferingProviders;

        final Map<Network.Service, Network.Provider> defaultSharedSGNetworkOfferingProviders = new HashMap<Network.Service, Network.Provider>();
        defaultSharedSGNetworkOfferingProviders.put(Service.Dhcp, Provider.VirtualRouter);
        defaultSharedSGNetworkOfferingProviders.put(Service.Dns, Provider.VirtualRouter);
        defaultSharedSGNetworkOfferingProviders.put(Service.UserData, Provider.VirtualRouter);
        defaultSharedSGNetworkOfferingProviders.put(Service.SecurityGroup, Provider.SecurityGroupProvider);

        final Map<Network.Service, Network.Provider> defaultIsolatedSourceNatEnabledNetworkOfferingProviders = new HashMap<Network.Service, Network.Provider>();
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Dhcp, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Dns, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.UserData, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Firewall, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Gateway, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Lb, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.SourceNat, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.StaticNat, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.PortForwarding, Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Vpn, Provider.VirtualRouter);

        final Map<Network.Service, Network.Provider> netscalerServiceProviders = new HashMap<Network.Service, Network.Provider>();
        netscalerServiceProviders.put(Service.Dhcp, Provider.VirtualRouter);
        netscalerServiceProviders.put(Service.Dns, Provider.VirtualRouter);
        netscalerServiceProviders.put(Service.UserData, Provider.VirtualRouter);
        netscalerServiceProviders.put(Service.SecurityGroup, Provider.SecurityGroupProvider);
        netscalerServiceProviders.put(Service.StaticNat, Provider.Netscaler);
        netscalerServiceProviders.put(Service.Lb, Provider.Netscaler);

        // The only one diff between 1 and 2 network offerings is that the first one has SG enabled. In Basic zone only
        // first network offering has to be enabled, in Advance zone - the second one
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // Offering #1
                NetworkOfferingVO defaultSharedSGNetworkOffering =
                        new NetworkOfferingVO(NetworkOffering.DefaultSharedNetworkOfferingWithSGService, "Offering for Shared Security group enabled networks",
                                TrafficType.Guest, false, true, null, null, true, Availability.Optional, null, Network.GuestType.Shared, true, true, false, false, false, false);

                defaultSharedSGNetworkOffering.setState(NetworkOffering.State.Enabled);
                defaultSharedSGNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultSharedSGNetworkOffering);

                for (Service service : defaultSharedSGNetworkOfferingProviders.keySet()) {
                    NetworkOfferingServiceMapVO offService =
                            new NetworkOfferingServiceMapVO(defaultSharedSGNetworkOffering.getId(), service, defaultSharedSGNetworkOfferingProviders.get(service));
                    _ntwkOfferingServiceMapDao.persist(offService);
                    s_logger.trace("Added service for the network offering: " + offService);
                }

                // Offering #2
                NetworkOfferingVO defaultSharedNetworkOffering =
                        new NetworkOfferingVO(NetworkOffering.DefaultSharedNetworkOffering, "Offering for Shared networks", TrafficType.Guest, false, true, null, null, true,
                                Availability.Optional, null, Network.GuestType.Shared, true, true, false, false, false, false);

                defaultSharedNetworkOffering.setState(NetworkOffering.State.Enabled);
                defaultSharedNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultSharedNetworkOffering);

                for (Service service : defaultSharedNetworkOfferingProviders.keySet()) {
                    NetworkOfferingServiceMapVO offService =
                            new NetworkOfferingServiceMapVO(defaultSharedNetworkOffering.getId(), service, defaultSharedNetworkOfferingProviders.get(service));
                    _ntwkOfferingServiceMapDao.persist(offService);
                    s_logger.trace("Added service for the network offering: " + offService);
                }

                // Offering #3
                NetworkOfferingVO defaultIsolatedSourceNatEnabledNetworkOffering =
                        new NetworkOfferingVO(NetworkOffering.DefaultIsolatedNetworkOfferingWithSourceNatService,
                                "Offering for Isolated networks with Source Nat service enabled", TrafficType.Guest, false, false, null, null, true, Availability.Required, null,
                                Network.GuestType.Isolated, true, false, false, false, true, false);

                defaultIsolatedSourceNatEnabledNetworkOffering.setState(NetworkOffering.State.Enabled);
                defaultIsolatedSourceNatEnabledNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultIsolatedSourceNatEnabledNetworkOffering);

                for (Service service : defaultIsolatedSourceNatEnabledNetworkOfferingProviders.keySet()) {
                    NetworkOfferingServiceMapVO offService =
                            new NetworkOfferingServiceMapVO(defaultIsolatedSourceNatEnabledNetworkOffering.getId(), service,
                                    defaultIsolatedSourceNatEnabledNetworkOfferingProviders.get(service));
                    _ntwkOfferingServiceMapDao.persist(offService);
                    s_logger.trace("Added service for the network offering: " + offService);
                }

                // Offering #4
                NetworkOfferingVO defaultIsolatedEnabledNetworkOffering =
                        new NetworkOfferingVO(NetworkOffering.DefaultIsolatedNetworkOffering, "Offering for Isolated networks with no Source Nat service", TrafficType.Guest,
                                false, true, null, null, true, Availability.Optional, null, Network.GuestType.Isolated, true, true, false, false, false, false);

                defaultIsolatedEnabledNetworkOffering.setState(NetworkOffering.State.Enabled);
                defaultIsolatedEnabledNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultIsolatedEnabledNetworkOffering);

                for (Service service : defaultIsolatedNetworkOfferingProviders.keySet()) {
                    NetworkOfferingServiceMapVO offService =
                            new NetworkOfferingServiceMapVO(defaultIsolatedEnabledNetworkOffering.getId(), service, defaultIsolatedNetworkOfferingProviders.get(service));
                    _ntwkOfferingServiceMapDao.persist(offService);
                    s_logger.trace("Added service for the network offering: " + offService);
                }

                // Offering #5
                NetworkOfferingVO defaultNetscalerNetworkOffering =
                        new NetworkOfferingVO(NetworkOffering.DefaultSharedEIPandELBNetworkOffering,
                                "Offering for Shared networks with Elastic IP and Elastic LB capabilities", TrafficType.Guest, false, true, null, null, true,
                                Availability.Optional, null, Network.GuestType.Shared, true, false, false, false, true, true, true, false, false, true, true, false, false, false, false, false);

                defaultNetscalerNetworkOffering.setState(NetworkOffering.State.Enabled);
                defaultNetscalerNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultNetscalerNetworkOffering);

                for (Service service : netscalerServiceProviders.keySet()) {
                    NetworkOfferingServiceMapVO offService =
                            new NetworkOfferingServiceMapVO(defaultNetscalerNetworkOffering.getId(), service, netscalerServiceProviders.get(service));
                    _ntwkOfferingServiceMapDao.persist(offService);
                    s_logger.trace("Added service for the network offering: " + offService);
                }

                // Offering #6
                NetworkOfferingVO defaultNetworkOfferingForVpcNetworks =
                        new NetworkOfferingVO(NetworkOffering.DefaultIsolatedNetworkOfferingForVpcNetworks,
                                "Offering for Isolated Vpc networks with Source Nat service enabled", TrafficType.Guest, false, false, null, null, true, Availability.Optional,
                                null, Network.GuestType.Isolated, false, false, false, false, true, true);

                defaultNetworkOfferingForVpcNetworks.setState(NetworkOffering.State.Enabled);
                defaultNetworkOfferingForVpcNetworks = _networkOfferingDao.persistDefaultNetworkOffering(defaultNetworkOfferingForVpcNetworks);

                Map<Network.Service, Network.Provider> defaultVpcNetworkOfferingProviders = new HashMap<Network.Service, Network.Provider>();
                defaultVpcNetworkOfferingProviders.put(Service.Dhcp, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProviders.put(Service.Dns, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProviders.put(Service.UserData, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProviders.put(Service.NetworkACL, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProviders.put(Service.Gateway, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProviders.put(Service.Lb, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProviders.put(Service.SourceNat, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProviders.put(Service.StaticNat, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProviders.put(Service.PortForwarding, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProviders.put(Service.Vpn, Provider.VPCVirtualRouter);

                for (Map.Entry<Service,Provider> entry : defaultVpcNetworkOfferingProviders.entrySet()) {
                     NetworkOfferingServiceMapVO offService =
                            new NetworkOfferingServiceMapVO(defaultNetworkOfferingForVpcNetworks.getId(), entry.getKey(), entry.getValue());
                    _ntwkOfferingServiceMapDao.persist(offService);
                    s_logger.trace("Added service for the network offering: " + offService);
                }

                // Offering #7
                NetworkOfferingVO defaultNetworkOfferingForVpcNetworksNoLB =
                        new NetworkOfferingVO(NetworkOffering.DefaultIsolatedNetworkOfferingForVpcNetworksNoLB,
                                "Offering for Isolated Vpc networks with Source Nat service enabled and LB service Disabled", TrafficType.Guest, false, false, null, null, true,
                                Availability.Optional, null, Network.GuestType.Isolated, false, false, false, false, false, true);

                defaultNetworkOfferingForVpcNetworksNoLB.setState(NetworkOffering.State.Enabled);
                defaultNetworkOfferingForVpcNetworksNoLB = _networkOfferingDao.persistDefaultNetworkOffering(defaultNetworkOfferingForVpcNetworksNoLB);

                Map<Network.Service, Network.Provider> defaultVpcNetworkOfferingProvidersNoLB = new HashMap<Network.Service, Network.Provider>();
                defaultVpcNetworkOfferingProvidersNoLB.put(Service.Dhcp, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProvidersNoLB.put(Service.Dns, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProvidersNoLB.put(Service.UserData, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProvidersNoLB.put(Service.NetworkACL, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProvidersNoLB.put(Service.Gateway, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProvidersNoLB.put(Service.SourceNat, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProvidersNoLB.put(Service.StaticNat, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProvidersNoLB.put(Service.PortForwarding, Provider.VPCVirtualRouter);
                defaultVpcNetworkOfferingProvidersNoLB.put(Service.Vpn, Provider.VPCVirtualRouter);

                for (Map.Entry<Service,Provider> entry : defaultVpcNetworkOfferingProvidersNoLB.entrySet()) {
                    NetworkOfferingServiceMapVO offService =
                            new NetworkOfferingServiceMapVO(defaultNetworkOfferingForVpcNetworksNoLB.getId(), entry.getKey(), entry.getValue());
                    _ntwkOfferingServiceMapDao.persist(offService);
                    s_logger.trace("Added service for the network offering: " + offService);
                }

                //offering #8 - network offering with internal lb service
                NetworkOfferingVO internalLbOff =
                        new NetworkOfferingVO(NetworkOffering.DefaultIsolatedNetworkOfferingForVpcNetworksWithInternalLB,
                                "Offering for Isolated Vpc networks with Internal LB support", TrafficType.Guest, false, false, null, null, true, Availability.Optional, null,
                                Network.GuestType.Isolated, false, false, false, true, false, true);

                internalLbOff.setState(NetworkOffering.State.Enabled);
                internalLbOff = _networkOfferingDao.persistDefaultNetworkOffering(internalLbOff);

                Map<Network.Service, Network.Provider> internalLbOffProviders = new HashMap<Network.Service, Network.Provider>();
                internalLbOffProviders.put(Service.Dhcp, Provider.VPCVirtualRouter);
                internalLbOffProviders.put(Service.Dns, Provider.VPCVirtualRouter);
                internalLbOffProviders.put(Service.UserData, Provider.VPCVirtualRouter);
                internalLbOffProviders.put(Service.NetworkACL, Provider.VPCVirtualRouter);
                internalLbOffProviders.put(Service.Gateway, Provider.VPCVirtualRouter);
                internalLbOffProviders.put(Service.Lb, Provider.InternalLbVm);
                internalLbOffProviders.put(Service.SourceNat, Provider.VPCVirtualRouter);

                for (Service service : internalLbOffProviders.keySet()) {
                    NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO(internalLbOff.getId(), service, internalLbOffProviders.get(service));
                    _ntwkOfferingServiceMapDao.persist(offService);
                    s_logger.trace("Added service for the network offering: " + offService);
                }

                _networkOfferingDao.persistDefaultL2NetworkOfferings();
            }
        });
    }

    private void createDefaultNetworks() {
        List<DataCenterVO> zones = _dataCenterDao.listAll();
        long id = 1;

        HashMap<TrafficType, String> guruNames = new HashMap<TrafficType, String>();
        guruNames.put(TrafficType.Public, PublicNetworkGuru.class.getSimpleName());
        guruNames.put(TrafficType.Management, PodBasedNetworkGuru.class.getSimpleName());
        guruNames.put(TrafficType.Control, ControlNetworkGuru.class.getSimpleName());
        guruNames.put(TrafficType.Storage, StorageNetworkGuru.class.getSimpleName());
        guruNames.put(TrafficType.Guest, DirectPodBasedNetworkGuru.class.getSimpleName());

        for (DataCenterVO zone : zones) {
            long zoneId = zone.getId();
            long accountId = 1L;
            Long domainId = zone.getDomainId();

            if (domainId == null) {
                domainId = 1L;
            }
            // Create default networks - system only
            List<NetworkOfferingVO> ntwkOff = _networkOfferingDao.listSystemNetworkOfferings();

            for (NetworkOfferingVO offering : ntwkOff) {
                if (offering.isSystemOnly()) {
                    long related = id;
                    long networkOfferingId = offering.getId();
                    Mode mode = Mode.Static;
                    String networkDomain = null;

                    BroadcastDomainType broadcastDomainType = null;
                    TrafficType trafficType = offering.getTrafficType();

                    boolean specifyIpRanges = false;

                    if (trafficType == TrafficType.Management) {
                        broadcastDomainType = BroadcastDomainType.Native;
                    } else if (trafficType == TrafficType.Storage) {
                        broadcastDomainType = BroadcastDomainType.Native;
                        specifyIpRanges = true;
                    } else if (trafficType == TrafficType.Control) {
                        broadcastDomainType = BroadcastDomainType.LinkLocal;
                    } else if (offering.getTrafficType() == TrafficType.Public) {
                        if ((zone.getNetworkType() == NetworkType.Advanced && !zone.isSecurityGroupEnabled()) || zone.getNetworkType() == NetworkType.Basic) {
                            specifyIpRanges = true;
                            broadcastDomainType = BroadcastDomainType.Vlan;
                        } else {
                            continue;
                        }
                    }

                    if (broadcastDomainType != null) {
                        NetworkVO network =
                                new NetworkVO(id, trafficType, mode, broadcastDomainType, networkOfferingId, domainId, accountId, related, null, null, networkDomain,
                                        Network.GuestType.Shared, zoneId, null, null, specifyIpRanges, null, offering.isRedundantRouter());
                        network.setGuruName(guruNames.get(network.getTrafficType()));
                        network.setDns1(zone.getDns1());
                        network.setDns2(zone.getDns2());
                        network.setState(State.Implemented);
                        _networkDao.persist(network, false, getServicesAndProvidersForNetwork(networkOfferingId));
                        id++;
                    }
                }
            }
        }
    }

    private void updateVlanWithNetworkId(VlanVO vlan) {
        long zoneId = vlan.getDataCenterId();
        long networkId = 0L;
        DataCenterVO zone = _zoneDao.findById(zoneId);

        if (zone.getNetworkType() == NetworkType.Advanced) {
            networkId = getSystemNetworkIdByZoneAndTrafficType(zoneId, TrafficType.Public);
        } else {
            networkId = getSystemNetworkIdByZoneAndTrafficType(zoneId, TrafficType.Guest);
        }

        vlan.setNetworkId(networkId);
        _vlanDao.update(vlan.getId(), vlan);
    }

    private long getSystemNetworkIdByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        // find system public network offering
        Long networkOfferingId = null;
        List<NetworkOfferingVO> offerings = _networkOfferingDao.listSystemNetworkOfferings();
        for (NetworkOfferingVO offering : offerings) {
            if (offering.getTrafficType() == trafficType) {
                networkOfferingId = offering.getId();
                break;
            }
        }

        if (networkOfferingId == null) {
            throw new InvalidParameterValueException("Unable to find system network offering with traffic type " + trafficType);
        }

        List<NetworkVO> networks = _networkDao.listBy(Account.ACCOUNT_ID_SYSTEM, networkOfferingId, zoneId);
        if (networks == null || networks.isEmpty()) {
            throw new InvalidParameterValueException("Unable to find network with traffic type " + trafficType + " in zone " + zoneId);
        }
        return networks.get(0).getId();
    }

    @DB
    public void updateResourceCount() {
        ResourceType[] resourceTypes = Resource.ResourceType.values();
        List<AccountVO> accounts = _accountDao.listAll();
        List<DomainVO> domains = _domainDao.listAll();
        List<ResourceCountVO> domainResourceCount = _resourceCountDao.listResourceCountByOwnerType(ResourceOwnerType.Domain);
        List<ResourceCountVO> accountResourceCount = _resourceCountDao.listResourceCountByOwnerType(ResourceOwnerType.Account);

        final List<ResourceType> accountSupportedResourceTypes = new ArrayList<ResourceType>();
        final List<ResourceType> domainSupportedResourceTypes = new ArrayList<ResourceType>();

        for (ResourceType resourceType : resourceTypes) {
            if (resourceType.supportsOwner(ResourceOwnerType.Account)) {
                accountSupportedResourceTypes.add(resourceType);
            }
            if (resourceType.supportsOwner(ResourceOwnerType.Domain)) {
                domainSupportedResourceTypes.add(resourceType);
            }
        }

        final int accountExpectedCount = accountSupportedResourceTypes.size();
        final int domainExpectedCount = domainSupportedResourceTypes.size();

        if ((domainResourceCount.size() < domainExpectedCount * domains.size())) {
            s_logger.debug("resource_count table has records missing for some domains...going to insert them");
            for (final DomainVO domain : domains) {
                // Lock domain
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        _domainDao.lockRow(domain.getId(), true);
                        List<ResourceCountVO> domainCounts = _resourceCountDao.listByOwnerId(domain.getId(), ResourceOwnerType.Domain);
                        List<String> domainCountStr = new ArrayList<String>();
                        for (ResourceCountVO domainCount : domainCounts) {
                            domainCountStr.add(domainCount.getType().toString());
                        }

                        if (domainCountStr.size() < domainExpectedCount) {
                            for (ResourceType resourceType : domainSupportedResourceTypes) {
                                if (!domainCountStr.contains(resourceType.toString())) {
                                    ResourceCountVO resourceCountVO = new ResourceCountVO(resourceType, 0, domain.getId(), ResourceOwnerType.Domain);
                                    s_logger.debug("Inserting resource count of type " + resourceType + " for domain id=" + domain.getId());
                                    _resourceCountDao.persist(resourceCountVO);
                                }
                            }
                        }
                    }
                });

            }
        }

        if ((accountResourceCount.size() < accountExpectedCount * accounts.size())) {
            s_logger.debug("resource_count table has records missing for some accounts...going to insert them");
            for (final AccountVO account : accounts) {
                // lock account
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        _accountDao.lockRow(account.getId(), true);
                        List<ResourceCountVO> accountCounts = _resourceCountDao.listByOwnerId(account.getId(), ResourceOwnerType.Account);
                        List<String> accountCountStr = new ArrayList<String>();
                        for (ResourceCountVO accountCount : accountCounts) {
                            accountCountStr.add(accountCount.getType().toString());
                        }

                        if (accountCountStr.size() < accountExpectedCount) {
                            for (ResourceType resourceType : accountSupportedResourceTypes) {
                                if (!accountCountStr.contains(resourceType.toString())) {
                                    ResourceCountVO resourceCountVO = new ResourceCountVO(resourceType, 0, account.getId(), ResourceOwnerType.Account);
                                    s_logger.debug("Inserting resource count of type " + resourceType + " for account id=" + account.getId());
                                    _resourceCountDao.persist(resourceCountVO);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    public Map<String, String> getServicesAndProvidersForNetwork(long networkOfferingId) {
        Map<String, String> svcProviders = new HashMap<String, String>();
        List<NetworkOfferingServiceMapVO> servicesMap = _ntwkOfferingServiceMapDao.listByNetworkOfferingId(networkOfferingId);

        for (NetworkOfferingServiceMapVO serviceMap : servicesMap) {
            if (svcProviders.containsKey(serviceMap.getService())) {
                continue;
            }
            svcProviders.put(serviceMap.getService(), serviceMap.getProvider());
        }

        return svcProviders;
    }

}
