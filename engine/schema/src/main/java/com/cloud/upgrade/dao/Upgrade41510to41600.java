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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade41510to41600 implements DbUpgrade {

    final static Logger LOG = Logger.getLogger(Upgrade41510to41600.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.15.1.0", "4.16.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.16.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41510to41600.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateSystemVmTemplates(conn);
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
                put(Hypervisor.HypervisorType.KVM, "systemvm-kvm-4.16.0");
                put(Hypervisor.HypervisorType.VMware, "systemvm-vmware-4.16.0");
                put(Hypervisor.HypervisorType.XenServer, "systemvm-xenserver-4.16.0");
                put(Hypervisor.HypervisorType.Hyperv, "systemvm-hyperv-4.16.0");
                put(Hypervisor.HypervisorType.LXC, "systemvm-lxc-4.16.0");
                put(Hypervisor.HypervisorType.Ovm3, "systemvm-ovm3-4.16.0");
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
                put(Hypervisor.HypervisorType.KVM, "http://sbjenkins-stagingrepo.jenkins.lon/systemvmtemplate/custom/cks-debian/systemvmtemplate-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.VMware, "http://sbjenkins-stagingrepo.jenkins.lon/systemvmtemplate/custom/cks-debian/systemvmtemplate-vmware.ova");
                put(Hypervisor.HypervisorType.XenServer, "http://sbjenkins-stagingrepo.jenkins.lon/systemvmtemplate/custom/cks-debian/systemvmtemplate-xen.vhd.bz2");
                put(Hypervisor.HypervisorType.Hyperv, "http://sbjenkins-stagingrepo.jenkins.lon/systemvmtemplate/custom/cks-debian/systemvmtemplate-hyperv.vhd.zip");
                put(Hypervisor.HypervisorType.LXC, "http://sbjenkins-stagingrepo.jenkins.lon/systemvmtemplate/custom/cks-debian/systemvmtemplate-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.Ovm3, "http://sbjenkins-stagingrepo.jenkins.lon/systemvmtemplate/custom/cks-debian/systemvmtemplate-ovm.raw.bz22");
            }
        };

        final Map<Hypervisor.HypervisorType, String> newTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.KVM, "0493097e888e7bb86c7118d23c5eeb67");
                put(Hypervisor.HypervisorType.XenServer, "b712722757fffcee0cfcdf63abdbbcff");
                put(Hypervisor.HypervisorType.VMware, "3d903f31df5324f21a730cab76706786");
                put(Hypervisor.HypervisorType.Hyperv, "54156ca498d5e96a6eb010c06d10ab55");
                put(Hypervisor.HypervisorType.LXC, "0493097e888e7bb86c7118d23c5eeb67");
                put(Hypervisor.HypervisorType.Ovm3, "e956da950052ad5e62098f3a366a6aa1");
            }
        };

        for (final Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName : NewTemplateNameList.entrySet()) {
            LOG.debug("Updating " + hypervisorAndTemplateName.getKey() + " System Vms");
            try (PreparedStatement pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = ? and removed is null order by id desc limit 1")) {
                // Get systemvm template id for corresponding hypervisor
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
                    if (hypervisorAndTemplateName.getKey() == Hypervisor.HypervisorType.VMware) {
                        updateVMwareSystemvVMTemplateField(conn, hypervisorAndTemplateName.getValue());
                    }
                    // update template ID of system Vms
                    try (PreparedStatement update_templ_id_pstmt = conn
                            .prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = ? and removed is NULL");) {
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
                        update_pstmt.setString(1, "4.16.0");
                        update_pstmt.setString(2, "minreq.sysvmtemplate.version");
                        update_pstmt.executeUpdate();
                    } catch (final SQLException e) {
                        LOG.error("updateSystemVmTemplates:Exception while setting 'minreq.sysvmtemplate.version' to 4.16.0: " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting 'minreq.sysvmtemplate.version' to 4.16.0", e);
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
                        if (hypervisorAndTemplateName.getKey() == Hypervisor.HypervisorType.VMware) {
                            updateVMwareSystemvVMTemplateField(conn, hypervisorAndTemplateName.getValue());
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

    private void updateVMwareSystemvVMTemplateField(final Connection conn, String templateName) {
        try (PreparedStatement update_templ_vmware_pstmt = conn
                .prepareStatement("UPDATE `cloud`.`vm_template` SET deploy_as_is = 1 WHERE name = '"+ templateName +"' AND removed is null order by id desc limit 1");) {
            update_templ_vmware_pstmt.executeUpdate();
        } catch (final SQLException e) {
            LOG.error("updateSystemVmTemplates:Exception while updating 'deploy_as_is' for VMWare hypervisor type : " + e.getMessage());
            throw new CloudRuntimeException("updateSystemVmTemplates:Exception while updating deploy_as_is for VMware hypervisor type ", e);
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41510to41600-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }
}
