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
import com.cloud.utils.script.Script;

public class Upgrade440to441 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade440to441.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.4.0", "4.4.1"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.4.1";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-440to441.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-440to441.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateSystemVmTemplates(conn);
    }

    @SuppressWarnings("serial")
    private void updateSystemVmTemplates(Connection conn) {
        s_logger.debug("Updating System Vm template IDs");
        //Get all hypervisors in use
        Set<Hypervisor.HypervisorType> hypervisorsListInUse = new HashSet<Hypervisor.HypervisorType>();
        try(PreparedStatement  pstmt = conn.prepareStatement("select distinct(hypervisor_type) from `cloud`.`cluster` where removed is null");
            ResultSet rs = pstmt.executeQuery();
           ) {
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
                default: // we don't support system vms on other hypervisors (yet)
                    break;
                }
            }
        } catch (SQLException e) {
            s_logger.error("updateSystemVmTemplates:Exception while getting hypervisor types from clusters: "+e.getMessage());
            throw new CloudRuntimeException("updateSystemVmTemplates:Exception while getting hypervisor types from clusters", e);
        }

        Map<Hypervisor.HypervisorType, String> NewTemplateNameList = new HashMap<Hypervisor.HypervisorType, String>(){
            {
                put(Hypervisor.HypervisorType.XenServer, "systemvm-xenserver-4.4");
                put(Hypervisor.HypervisorType.VMware, "systemvm-vmware-4.4");
                put(Hypervisor.HypervisorType.KVM, "systemvm-kvm-4.4");
                put(Hypervisor.HypervisorType.LXC, "systemvm-lxc-4.4");
                put(Hypervisor.HypervisorType.Hyperv, "systemvm-hyperv-4.4");
            }
        };

        Map<Hypervisor.HypervisorType, String> routerTemplateConfigurationNames = new HashMap<Hypervisor.HypervisorType, String>(){
            {
                put(Hypervisor.HypervisorType.XenServer, "router.template.xen");
                put(Hypervisor.HypervisorType.VMware, "router.template.vmware");
                put(Hypervisor.HypervisorType.KVM, "router.template.kvm");
                put(Hypervisor.HypervisorType.LXC, "router.template.lxc");
                put(Hypervisor.HypervisorType.Hyperv, "router.template.hyperv");
            }
        };

        Map<Hypervisor.HypervisorType, String> newTemplateUrl = new HashMap<Hypervisor.HypervisorType, String>(){
            {
                put(Hypervisor.HypervisorType.XenServer, "http://cloudstack.apt-get.eu/systemvm/4.4/systemvm64template-4.4.0-6-xen.vhd.bz2");
                put(Hypervisor.HypervisorType.VMware, "http://cloudstack.apt-get.eu/systemvm/4.4/systemvm64template-4.4.0-6-vmware.ova");
                put(Hypervisor.HypervisorType.KVM, "http://cloudstack.apt-get.eu/systemvm/4.4/systemvm64template-4.4.0-6-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.LXC, "http://cloudstack.apt-get.eu/systemvm/4.4/systemvm64template-4.4.0-6-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.Hyperv, "http://cloudstack.apt-get.eu/systemvm/4.4/systemvm64template-4.4.0-6-hyperv.vhd.bz2");
            }
        };

        /*
            c230704229bd101eacf83a39b4abb91e *systemvm64template-4.4.0-6-hyperv.vhd
            1c0bdb131e3b7ee753d014961fdd6eb0 *systemvm64template-4.4.0-6-hyperv.vhd.zip
            770a7d3b727ca15e511c33521ef5b8ba *systemvm64template-4.4.0-6-kvm.qcow2.bz2
            1623dea39777319337116439cd2dd379 *systemvm64template-4.4.0-6-vmware.ova
            bd8d1fab55834254f3ee24181430568d *systemvm64template-4.4.0-6-vmware.vmdk.bz2
            7a8a6c4f8478147f1c6aa0d20f5e62b9 *systemvm64template-4.4.0-6-xen.vhd.bz2
         */

        Map<Hypervisor.HypervisorType, String> newTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>(){
            {
                put(Hypervisor.HypervisorType.XenServer, "7a8a6c4f8478147f1c6aa0d20f5e62b9");
                put(Hypervisor.HypervisorType.VMware, "1623dea39777319337116439cd2dd379");
                put(Hypervisor.HypervisorType.KVM, "770a7d3b727ca15e511c33521ef5b8ba");
                put(Hypervisor.HypervisorType.LXC, "770a7d3b727ca15e511c33521ef5b8ba");
                put(Hypervisor.HypervisorType.Hyperv, "1c0bdb131e3b7ee753d014961fdd6eb0");
            }
        };

        for (Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName : NewTemplateNameList.entrySet()){
            s_logger.debug("Updating " + hypervisorAndTemplateName.getKey() + " System Vms");
            try (PreparedStatement pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = ? and removed is null order by id desc limit 1");)
            {
                    //Get 4.4.0 system Vm template Id for corresponding hypervisor
                    long templateId = -1;
                    pstmt.setString(1, hypervisorAndTemplateName.getValue());
                    try(ResultSet rs = pstmt.executeQuery();)
                    {
                        if(rs.next()) {
                            templateId = rs.getLong(1);
                        }
                    }catch (SQLException e)
                    {
                        s_logger.error("updateSystemVmTemplates:Exception while getting ids of templates: "+e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while getting ids of templates", e);
                    }

                    // change template type to SYSTEM
                    if (templateId != -1)
                    {
                        try(PreparedStatement templ_type_pstmt = conn.prepareStatement("update `cloud`.`vm_template` set type='SYSTEM' where id = ?");)
                        {
                            templ_type_pstmt.setLong(1, templateId);
                            templ_type_pstmt.executeUpdate();
                        }
                        catch (SQLException e)
                        {
                            s_logger.error("updateSystemVmTemplates:Exception while updating template with id " + templateId + " to be marked as 'system': "+e.getMessage());
                            throw new CloudRuntimeException("updateSystemVmTemplates:Exception while updating template with id " + templateId + " to be marked as 'system'", e);
                        }
                        // update template ID of system Vms
                        try(PreparedStatement update_templ_id_pstmt = conn.prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = ?");)
                        {
                            update_templ_id_pstmt.setLong(1, templateId);
                            update_templ_id_pstmt.setString(2, hypervisorAndTemplateName.getKey().toString());
                            update_templ_id_pstmt.executeUpdate();
                        }catch (Exception e)
                        {
                            s_logger.error("updateSystemVmTemplates:Exception while setting template for " + hypervisorAndTemplateName.getKey().toString() + " to " + templateId + ": "+e.getMessage());
                            throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting template for " + hypervisorAndTemplateName.getKey().toString() + " to " + templateId, e);
                        }

                        // Change value of global configuration parameter router.template.* for the corresponding hypervisor
                        try(PreparedStatement update_pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = ? WHERE name = ?");) {
                            update_pstmt.setString(1, hypervisorAndTemplateName.getValue());
                            update_pstmt.setString(2, routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()));
                            update_pstmt.executeUpdate();
                        }catch (SQLException e)
                        {
                            s_logger.error("updateSystemVmTemplates:Exception while setting " + routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()) + " to " + hypervisorAndTemplateName.getValue() + ": "+e.getMessage());
                            throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting " + routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()) + " to " + hypervisorAndTemplateName.getValue(), e);
                        }

                    } else {
                        if (hypervisorsListInUse.contains(hypervisorAndTemplateName.getKey())){
                            throw new CloudRuntimeException("4.4.0 " + hypervisorAndTemplateName.getKey() + " SystemVm template not found. Cannot upgrade system Vms");
                        } else {
                            s_logger.warn("4.4.0 " + hypervisorAndTemplateName.getKey() + " SystemVm template not found. " + hypervisorAndTemplateName.getKey() + " hypervisor is not used, so not failing upgrade");
                            // Update the latest template URLs for corresponding hypervisor
                            try(PreparedStatement update_templ_url_pstmt = conn.prepareStatement("UPDATE `cloud`.`vm_template` SET url = ? , checksum = ? WHERE hypervisor_type = ? AND type = 'SYSTEM' AND removed is null order by id desc limit 1");) {
                                update_templ_url_pstmt.setString(1, newTemplateUrl.get(hypervisorAndTemplateName.getKey()));
                                update_templ_url_pstmt.setString(2, newTemplateChecksum.get(hypervisorAndTemplateName.getKey()));
                                update_templ_url_pstmt.setString(3, hypervisorAndTemplateName.getKey().toString());
                                update_templ_url_pstmt.executeUpdate();
                            }catch (SQLException e)
                            {
                                s_logger.error("updateSystemVmTemplates:Exception while updating 'url' and 'checksum' for hypervisor type " + hypervisorAndTemplateName.getKey().toString() + ": "+e.getMessage());
                                throw new CloudRuntimeException("updateSystemVmTemplates:Exception while updating 'url' and 'checksum' for hypervisor type " + hypervisorAndTemplateName.getKey().toString(), e);
                            }
                        }
                    }
            } catch (SQLException e) {
                s_logger.error("updateSystemVmTemplates:Exception while getting ids of templates: "+e.getMessage());
                throw new CloudRuntimeException("updateSystemVmTemplates:Exception while getting ids of templates", e);
            }
        }
        s_logger.debug("Updating System Vm Template IDs Complete");
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-440to441-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-440to441-cleanup.sql");
        }

        return new File[] {new File(script)};
    }
}
