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

package com.cloud.upgrade.dao;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade421to430 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade421to430.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.2.1", "4.3.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.3.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-421to430.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-421to430.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        encryptLdapConfigParams(conn);
        encryptImageStoreDetails(conn);
        upgradeMemoryOfSsvmOffering(conn);
        updateSystemVmTemplates(conn);
    }

    private void upgradeMemoryOfSsvmOffering(Connection conn) {
        PreparedStatement updatePstmt = null;
        PreparedStatement selectPstmt = null;
        ResultSet selectResultSet = null;
        int newRamSize = 512; //512MB
        long serviceOfferingId = 0;

            /**
             * Pick first row in service_offering table which has system vm type as secondary storage vm. User added offerings would start from 2nd row onwards.
             * We should not update/modify any user-defined offering.
             */

        try {
            selectPstmt = conn.prepareStatement("SELECT id FROM `cloud`.`service_offering` WHERE vm_type='secondarystoragevm'");
            updatePstmt = conn.prepareStatement("UPDATE `cloud`.`service_offering` SET ram_size=? WHERE id=?");
            selectResultSet = selectPstmt.executeQuery();
            if(selectResultSet.next()) {
                serviceOfferingId = selectResultSet.getLong("id");
            }

            updatePstmt.setInt(1, newRamSize);
            updatePstmt.setLong(2, serviceOfferingId);
            updatePstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to upgrade ram_size of service offering for secondary storage vm. ", e);
        } finally {
            try {
                if (selectPstmt != null) {
                    selectPstmt.close();
                }
                if (selectResultSet != null) {
                    selectResultSet.close();
                }
                if (updatePstmt != null) {
                    updatePstmt.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Done upgrading RAM for service offering of Secondary Storage VM to " + newRamSize);
    }

    private void encryptLdapConfigParams(Connection conn) {
        PreparedStatement pstmt = null;

        String[][] ldapParams = { {"ldap.user.object", "inetOrgPerson", "Sets the object type of users within LDAP"},
                {"ldap.username.attribute", "uid", "Sets the username attribute used within LDAP"}, {"ldap.email.attribute", "mail", "Sets the email attribute used within LDAP"},
                {"ldap.firstname.attribute", "givenname", "Sets the firstname attribute used within LDAP"},
                {"ldap.lastname.attribute", "sn", "Sets the lastname attribute used within LDAP"},
                {"ldap.group.object", "groupOfUniqueNames", "Sets the object type of groups within LDAP"},
                {"ldap.group.user.uniquemember", "uniquemember", "Sets the attribute for uniquemembers within a group"}};

        String insertSql = "INSERT INTO `cloud`.`configuration`(category, instance, component, name, value, description) VALUES ('Secure', 'DEFAULT', 'management-server', ?, ?, "
                + "?) ON DUPLICATE KEY UPDATE category='Secure';";

        try {

            for (String[] ldapParam : ldapParams) {
                String name = ldapParam[0];
                String value = ldapParam[1];
                String desc = ldapParam[2];
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt = conn.prepareStatement(insertSql);
                pstmt.setString(1, name);
                pstmt.setBytes(2, encryptedValue.getBytes("UTF-8"));
                pstmt.setString(3, desc);
                pstmt.executeUpdate();
            }

            /**
             * if encrypted, decrypt the ldap hostname and port and then update as they are not encrypted now.
             */
            pstmt = conn.prepareStatement("SELECT conf.value FROM `cloud`.`configuration` conf WHERE conf.name='ldap.hostname'");
            ResultSet resultSet = pstmt.executeQuery();
            String hostname = null;
            String port;
            int portNumber = 0;
            if (resultSet.next()) {
                hostname = DBEncryptionUtil.decrypt(resultSet.getString(1));
            }

            pstmt = conn.prepareStatement("SELECT conf.value FROM `cloud`.`configuration` conf WHERE conf.name='ldap.port'");
            resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                port = DBEncryptionUtil.decrypt(resultSet.getString(1));
                if (StringUtils.isNotBlank(port)) {
                    portNumber = Integer.valueOf(port);
                }
            }

            if (StringUtils.isNotBlank(hostname)) {
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`ldap_configuration`(hostname, port) VALUES(?,?)");
                pstmt.setString(1, hostname);
                if (portNumber != 0) {
                    pstmt.setInt(2, portNumber);
                } else {
                    pstmt.setNull(2, Types.INTEGER);
                }
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert ldap configuration values ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to insert ldap configuration values ", e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Done encrypting ldap Config values");

    }

    private void updateSystemVmTemplates(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        s_logger.debug("Updating System Vm template IDs");
        try{
            //Get all hypervisors in use
            Set<Hypervisor.HypervisorType> hypervisorsListInUse = new HashSet<Hypervisor.HypervisorType>();
            try {
                pstmt = conn.prepareStatement("select distinct(hypervisor_type) from `cloud`.`cluster` where removed is null");
                rs = pstmt.executeQuery();
                while(rs.next()){
                    switch (Hypervisor.HypervisorType.getType(rs.getString(1))) {
                        case XenServer: hypervisorsListInUse.add(Hypervisor.HypervisorType.XenServer);
                            break;
                        case KVM:       hypervisorsListInUse.add(Hypervisor.HypervisorType.KVM);
                            break;
                        case VMware:    hypervisorsListInUse.add(Hypervisor.HypervisorType.VMware);
                            break;
                        case Hyperv:    hypervisorsListInUse.add(Hypervisor.HypervisorType.Hyperv);
                            break;
                        case LXC:       hypervisorsListInUse.add(Hypervisor.HypervisorType.LXC);
                            break;
                    }
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("Error while listing hypervisors in use", e);
            }

            Map<Hypervisor.HypervisorType, String> NewTemplateNameList = new HashMap<Hypervisor.HypervisorType, String>(){
                {   put(Hypervisor.HypervisorType.XenServer, "systemvm-xenserver-4.3");
                    put(Hypervisor.HypervisorType.VMware, "systemvm-vmware-4.3");
                    put(Hypervisor.HypervisorType.KVM, "systemvm-kvm-4.3");
                    put(Hypervisor.HypervisorType.LXC, "systemvm-lxc-4.3");
                    put(Hypervisor.HypervisorType.Hyperv, "systemvm-hyperv-4.3");
                }
            };

            Map<Hypervisor.HypervisorType, String> routerTemplateConfigurationNames = new HashMap<Hypervisor.HypervisorType, String>(){
                {   put(Hypervisor.HypervisorType.XenServer, "router.template.xen");
                    put(Hypervisor.HypervisorType.VMware, "router.template.vmware");
                    put(Hypervisor.HypervisorType.KVM, "router.template.kvm");
                    put(Hypervisor.HypervisorType.LXC, "router.template.lxc");
                    put(Hypervisor.HypervisorType.Hyperv, "router.template.hyperv");
                }
            };

            Map<Hypervisor.HypervisorType, String> newTemplateUrl = new HashMap<Hypervisor.HypervisorType, String>(){
                {   put(Hypervisor.HypervisorType.XenServer, "http://download.cloud.com/templates/4.3/systemvm64template-2014-01-14-master-xen.vhd.bz2");
                    put(Hypervisor.HypervisorType.VMware, "http://download.cloud.com/templates/4.3/systemvm64template-2014-01-14-master-vmware.ova");
                    put(Hypervisor.HypervisorType.KVM, "http://download.cloud.com/templates/4.3/systemvm64template-2014-01-14-master-kvm.qcow2.bz2");
                    put(Hypervisor.HypervisorType.LXC, "http://download.cloud.com/templates/4.3/systemvm64template-2014-01-14-master-kvm.qcow2.bz2");
                    put(Hypervisor.HypervisorType.Hyperv, "http://download.cloud.com/templates/4.3/systemvm64template-2013-12-23-hyperv.vhd.bz2");
                }
            };

            Map<Hypervisor.HypervisorType, String> newTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>(){
                {   put(Hypervisor.HypervisorType.XenServer, "74b92f031cc5c2089ee89efb81344dcf");
                    put(Hypervisor.HypervisorType.VMware, "ef593a061f3b7594ab0bfd9b0ed0a0d4");
                    put(Hypervisor.HypervisorType.KVM, "85a1bed07bf43cbf022451cb2ecae4ff");
                    put(Hypervisor.HypervisorType.LXC, "85a1bed07bf43cbf022451cb2ecae4ff");
                    put(Hypervisor.HypervisorType.Hyperv, "5df45ee6ebe1b703a8805f4e1f4d0818");
                }
            };

            for (Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName : NewTemplateNameList.entrySet()){
                s_logger.debug("Updating " + hypervisorAndTemplateName.getKey() + " System Vms");
                try {
                    //Get 4.3.0 system Vm template Id for corresponding hypervisor
                    pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = ? and removed is null order by id desc limit 1");
                    pstmt.setString(1, hypervisorAndTemplateName.getValue());
                    rs = pstmt.executeQuery();
                    if(rs.next()){
                        long templateId = rs.getLong(1);
                        rs.close();
                        pstmt.close();
//                        // Mark the old system templates as removed
//                        pstmt = conn.prepareStatement("UPDATE `cloud`.`vm_template` SET removed = now() WHERE hypervisor_type = ? AND type = 'SYSTEM' AND removed is null");
//                        pstmt.setString(1, hypervisorAndTemplateName.getKey().toString());
//                        pstmt.executeUpdate();
//                        pstmt.close();
                        // change template type to SYSTEM
                        pstmt = conn.prepareStatement("update `cloud`.`vm_template` set type='SYSTEM' where id = ?");
                        pstmt.setLong(1, templateId);
                        pstmt.executeUpdate();
                        pstmt.close();
                        // update templete ID of system Vms
                        pstmt = conn.prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = ?");
                        pstmt.setLong(1, templateId);
                        pstmt.setString(2, hypervisorAndTemplateName.getKey().toString());
                        pstmt.executeUpdate();
                        pstmt.close();
                        // Change value of global configuration parameter router.template.* for the corresponding hypervisor
                        pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = ? WHERE name = ?");
                        pstmt.setString(1, hypervisorAndTemplateName.getValue());
                        pstmt.setString(2, routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()));
                        pstmt.executeUpdate();
                        pstmt.close();
                    } else {
                        if (hypervisorsListInUse.contains(hypervisorAndTemplateName.getKey())){
                            throw new CloudRuntimeException("4.3.0 " + hypervisorAndTemplateName.getKey() + " SystemVm template not found. Cannot upgrade system Vms");
                        } else {
                            s_logger.warn("4.3.0 " + hypervisorAndTemplateName.getKey() + " SystemVm template not found. " + hypervisorAndTemplateName.getKey() + " hypervisor is not used, so not failing upgrade");
                            // Update the latest template URLs for corresponding hypervisor
                            pstmt = conn.prepareStatement("UPDATE `cloud`.`vm_template` SET url = ? , checksum = ? WHERE hypervisor_type = ? AND type = 'SYSTEM' AND removed is null order by id desc limit 1");
                            pstmt.setString(1, newTemplateUrl.get(hypervisorAndTemplateName.getKey()));
                            pstmt.setString(2, newTemplateChecksum.get(hypervisorAndTemplateName.getKey()));
                            pstmt.setString(3, hypervisorAndTemplateName.getKey().toString());
                            pstmt.executeUpdate();
                            pstmt.close();
                        }
                    }
                } catch (SQLException e) {
                    throw new CloudRuntimeException("Error while updating "+ hypervisorAndTemplateName.getKey() +" systemVm template", e);
                }
            }
            s_logger.debug("Updating System Vm Template IDs Complete");
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private void encryptImageStoreDetails(Connection conn) {
        s_logger.debug("Encrypting image store details");
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("select id, value from `cloud`.`image_store_details` where name = 'key' or name = 'secretkey'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if (value == null) {
                    continue;
                }
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt = conn.prepareStatement("update `cloud`.`image_store_details` set value=? where id=?");
                pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt image_store_details values ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt image_store_details values ", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Done encrypting image_store_details");
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-421to430-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-421to430-cleanup.sql");
        }

        return new File[] {new File(script)};
    }

}
