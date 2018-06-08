/*
 * // Licensed to the Apache Software Foundation (ASF) under one
 * // or more contributor license agreements.  See the NOTICE file
 * // distributed with this work for additional information
 * // regarding copyright ownership.  The ASF licenses this file
 * // to you under the Apache License, Version 2.0 (the
 * // "License"); you may not use this file except in compliance
 * // with the License.  You may obtain a copy of the License at
 * //
 * //   http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing,
 * // software distributed under the License is distributed on an
 * // "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * // KIND, either express or implied.  See the License for the
 * // specific language governing permissions and limitations
 * // under the License.
 */

package com.cloud.upgrade.dao;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Upgrade41100to41110 implements DbUpgrade {
    final static Logger LOG = Logger.getLogger(Upgrade41000to41100.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[]{"4.11.0.0", "4.11.1.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.11.1.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41100to41110.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateSystemVmTemplates(conn);
        markUnnecessarySecureConfigsAsUnsecure(conn);
    }

    private void markUnnecessarySecureConfigsAsUnsecure(Connection conn) {
        String[] unsecureItems = new String[] {
                "ldap.basedn",
                "ldap.bind.principal",
                "ldap.email.attribute",
                "ldap.firstname.attribute",
                "ldap.group.object",
                "ldap.group.user.uniquemember",
                "ldap.lastname.attribute",
                "ldap.search.group.principle",
                "ldap.truststore",
                "ldap.user.object",
                "ldap.username.attribute"
        };

        for (String name : unsecureItems) {
            uncrypt(conn, name);
        }
    }

    /**
     * if encrypted, decrypt the ldap hostname and port and then update as they are not encrypted now.
     */
    private void uncrypt(Connection conn, String name)
    {
        String value = null;
        try (
                PreparedStatement prepSelStmt = conn.prepareStatement("SELECT conf.category,conf.value FROM `cloud`.`configuration` conf WHERE conf.name= ?");
        ) {
            prepSelStmt.setString(1,name);
            try (
                    ResultSet resultSet = prepSelStmt.executeQuery();
            ) {
                if (resultSet.next()) {
                    if ("Secure".equals(resultSet.getString(1))) {
                        value = DBEncryptionUtil.decrypt(resultSet.getString(2));
                        try (
                                PreparedStatement prepUpdStmt= conn.prepareStatement("UPDATE `cloud`.`configuration` set category = 'Advanced', value = ? where name is ?" );
                        ) {
                            prepUpdStmt.setString(1, value);
                            prepUpdStmt.setString(2, name);
                            prepUpdStmt.execute();
                        } catch (SQLException e) {
                            if (LOG.isInfoEnabled()) {
                                LOG.info("failed to update configuration item '"+name+"' with value '"+value+"'");
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("");
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("failed to update configuration item '"+name+"' with value '"+value+"'", e);
        }
    }

    @SuppressWarnings("serial")
    private void updateSystemVmTemplates(final Connection conn) {
        LOG.debug("Updating System Vm template IDs");
        final Set<Hypervisor.HypervisorType> hypervisorsListInUse = new HashSet<Hypervisor.HypervisorType>();
        try (PreparedStatement pstmt = conn.prepareStatement("select distinct(hypervisor_type) from `cloud`.`cluster` where removed is null"); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                switch (Hypervisor.HypervisorType.getType(rs.getString(1))) {
                    case XenServer:
                        hypervisorsListInUse.add(Hypervisor.HypervisorType.XenServer);
                        break;
                    case KVM:
                        hypervisorsListInUse.add(Hypervisor.HypervisorType.KVM);
                        break;
                    case VMware:
                        hypervisorsListInUse.add(Hypervisor.HypervisorType.VMware);
                        break;
                    case Hyperv:
                        hypervisorsListInUse.add(Hypervisor.HypervisorType.Hyperv);
                        break;
                    case LXC:
                        hypervisorsListInUse.add(Hypervisor.HypervisorType.LXC);
                        break;
                    case Ovm3:
                        hypervisorsListInUse.add(Hypervisor.HypervisorType.Ovm3);
                        break;
                    default:
                        break;
                }
            }
        } catch (final SQLException e) {
            LOG.error("updateSystemVmTemplates: Exception caught while getting hypervisor types from clusters: " + e.getMessage());
            throw new CloudRuntimeException("updateSystemVmTemplates:Exception while getting hypervisor types from clusters", e);
        }

        final Map<Hypervisor.HypervisorType, String> NewTemplateNameList = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.KVM, "systemvm-kvm-4.11.1");
                put(Hypervisor.HypervisorType.VMware, "systemvm-vmware-4.11.1");
                put(Hypervisor.HypervisorType.XenServer, "systemvm-xenserver-4.11.1");
                put(Hypervisor.HypervisorType.Hyperv, "systemvm-hyperv-4.11.1");
                put(Hypervisor.HypervisorType.LXC, "systemvm-lxc-4.11.1");
                put(Hypervisor.HypervisorType.Ovm3, "systemvm-ovm3-4.11.1");
            }
        };

        final Map<Hypervisor.HypervisorType, String> routerTemplateConfigurationNames = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.KVM, "router.template.kvm");
                put(Hypervisor.HypervisorType.VMware, "router.template.vmware");
                put(Hypervisor.HypervisorType.XenServer, "router.template.xenserver");
                put(Hypervisor.HypervisorType.Hyperv, "router.template.hyperv");
                put(Hypervisor.HypervisorType.LXC, "router.template.lxc");
                put(Hypervisor.HypervisorType.Ovm3, "router.template.ovm3");
            }
        };

        final Map<Hypervisor.HypervisorType, String> newTemplateUrl = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.KVM, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.1-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.VMware, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.1-vmware.ova");
                put(Hypervisor.HypervisorType.XenServer, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.1-xen.vhd.bz2");
                put(Hypervisor.HypervisorType.Hyperv, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.1-hyperv.vhd.zip");
                put(Hypervisor.HypervisorType.LXC, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.1-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.Ovm3, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.1-ovm.raw.bz2");
            }
        };

        final Map<Hypervisor.HypervisorType, String> newTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.KVM, "6019c2ed1a13669dcf334fe380c776b0");
                put(Hypervisor.HypervisorType.XenServer, "f2245e912c856ab610d91f88c362a1f9");
                put(Hypervisor.HypervisorType.VMware, "1dbcd051fcfcd0fd568ff6eb5294988a");
                put(Hypervisor.HypervisorType.Hyperv, "e68ec90f0dc06821d94a2ee0e88fa646");
                put(Hypervisor.HypervisorType.LXC, "6019c2ed1a13669dcf334fe380c776b0");
                put(Hypervisor.HypervisorType.Ovm3, "cd2ac8dcdaf6c05d75e29cb39ee9a10f");
            }
        };

        for (final Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName : NewTemplateNameList.entrySet()) {
            LOG.debug("Updating " + hypervisorAndTemplateName.getKey() + " System Vms");
            try (PreparedStatement pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = ? and removed is null order by id desc limit 1")) {
                // Get 4.11.0 systemvm template id for corresponding hypervisor
                long templateId = -1;
                pstmt.setString(1, hypervisorAndTemplateName.getValue());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        templateId = rs.getLong(1);
                    }
                } catch (final SQLException e) {
                    LOG.error("updateSystemVmTemplates: Exception caught while getting ids of templates: " + e.getMessage());
                    throw new CloudRuntimeException("updateSystemVmTemplates: Exception caught while getting ids of templates", e);
                }

                // change template type to SYSTEM
                if (templateId != -1) {
                    try (PreparedStatement templ_type_pstmt = conn.prepareStatement("update `cloud`.`vm_template` set type='SYSTEM' where id = ?");) {
                        templ_type_pstmt.setLong(1, templateId);
                        templ_type_pstmt.executeUpdate();
                    } catch (final SQLException e) {
                        LOG.error("updateSystemVmTemplates:Exception while updating template with id " + templateId + " to be marked as 'system': " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while updating template with id " + templateId + " to be marked as 'system'", e);
                    }
                    // update template ID of system Vms
                    try (PreparedStatement update_templ_id_pstmt = conn
                            .prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = ?");) {
                        update_templ_id_pstmt.setLong(1, templateId);
                        update_templ_id_pstmt.setString(2, hypervisorAndTemplateName.getKey().toString());
                        update_templ_id_pstmt.executeUpdate();
                    } catch (final Exception e) {
                        LOG.error("updateSystemVmTemplates:Exception while setting template for " + hypervisorAndTemplateName.getKey().toString() + " to " + templateId
                                + ": " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting template for " + hypervisorAndTemplateName.getKey().toString() + " to "
                                + templateId, e);
                    }

                    // Change value of global configuration parameter
                    // router.template.* for the corresponding hypervisor
                    try (PreparedStatement update_pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = ? WHERE name = ?");) {
                        update_pstmt.setString(1, hypervisorAndTemplateName.getValue());
                        update_pstmt.setString(2, routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()));
                        update_pstmt.executeUpdate();
                    } catch (final SQLException e) {
                        LOG.error("updateSystemVmTemplates:Exception while setting " + routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()) + " to "
                                + hypervisorAndTemplateName.getValue() + ": " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting "
                                + routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()) + " to " + hypervisorAndTemplateName.getValue(), e);
                    }

                    // Change value of global configuration parameter
                    // minreq.sysvmtemplate.version for the ACS version
                    try (PreparedStatement update_pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = ? WHERE name = ?");) {
                        update_pstmt.setString(1, "4.11.1");
                        update_pstmt.setString(2, "minreq.sysvmtemplate.version");
                        update_pstmt.executeUpdate();
                    } catch (final SQLException e) {
                        LOG.error("updateSystemVmTemplates:Exception while setting 'minreq.sysvmtemplate.version' to 4.11.1: " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting 'minreq.sysvmtemplate.version' to 4.11.1", e);
                    }
                } else {
                    if (hypervisorsListInUse.contains(hypervisorAndTemplateName.getKey())) {
                        throw new CloudRuntimeException(getUpgradedVersion() + hypervisorAndTemplateName.getKey() + " SystemVm template not found. Cannot upgrade system Vms");
                    } else {
                        LOG.warn(getUpgradedVersion() + hypervisorAndTemplateName.getKey() + " SystemVm template not found. " + hypervisorAndTemplateName.getKey()
                                + " hypervisor is not used, so not failing upgrade");
                        // Update the latest template URLs for corresponding
                        // hypervisor
                        try (PreparedStatement update_templ_url_pstmt = conn
                                .prepareStatement("UPDATE `cloud`.`vm_template` SET url = ? , checksum = ? WHERE hypervisor_type = ? AND type = 'SYSTEM' AND removed is null order by id desc limit 1");) {
                            update_templ_url_pstmt.setString(1, newTemplateUrl.get(hypervisorAndTemplateName.getKey()));
                            update_templ_url_pstmt.setString(2, newTemplateChecksum.get(hypervisorAndTemplateName.getKey()));
                            update_templ_url_pstmt.setString(3, hypervisorAndTemplateName.getKey().toString());
                            update_templ_url_pstmt.executeUpdate();
                        } catch (final SQLException e) {
                            LOG.error("updateSystemVmTemplates:Exception while updating 'url' and 'checksum' for hypervisor type "
                                    + hypervisorAndTemplateName.getKey().toString() + ": " + e.getMessage());
                            throw new CloudRuntimeException("updateSystemVmTemplates:Exception while updating 'url' and 'checksum' for hypervisor type "
                                    + hypervisorAndTemplateName.getKey().toString(), e);
                        }
                    }
                }
            } catch (final SQLException e) {
                LOG.error("updateSystemVmTemplates:Exception while getting ids of templates: " + e.getMessage());
                throw new CloudRuntimeException("updateSystemVmTemplates:Exception while getting ids of templates", e);
            }
        }
        LOG.debug("Updating System Vm Template IDs Complete");
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41100to41110-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }
}
