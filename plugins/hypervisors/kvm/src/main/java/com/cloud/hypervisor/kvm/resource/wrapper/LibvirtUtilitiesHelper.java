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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.VMSnapshotTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.template.Processor;
import com.cloud.storage.template.QCOW2Processor;
import com.cloud.storage.template.TemplateLocation;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;

/**
 * This class is used to wrap the calls to several static methods. By doing so, we make easier to mock this class
 * and the methods wrapped here.
 */
public class LibvirtUtilitiesHelper {
    private static final Logger s_logger = Logger.getLogger(LibvirtUtilitiesHelper.class);

    public static final int TIMEOUT = 10000;

    /**
     * Although the flag '--delete' for command 'virsh blockcommit' already exists in Libvirt 1.2.9, its functionality was only implemented on 6.0.0.
     */
    private static final int LIBVIRT_VERSION_THAT_SUPPORTS_FLAG_DELETE_ON_COMMAND_VIRSH_BLOCKCOMMIT = 6000000;

    public Connect getConnectionByVmName(final String vmName) throws LibvirtException {
        return LibvirtConnection.getConnectionByVmName(vmName);
    }

    public Connect getConnection() throws LibvirtException {
        return LibvirtConnection.getConnection();
    }

    public TemplateLocation buildTemplateLocation(final StorageLayer storage, final String templatePath) {
        final TemplateLocation location = new TemplateLocation(storage, templatePath);
        return location;
    }

    public Processor buildQCOW2Processor(final StorageLayer storage) throws ConfigurationException {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put(StorageLayer.InstanceConfigKey, storage);

        final Processor qcow2Processor = new QCOW2Processor();
        qcow2Processor.configure("QCOW2 Processor", params);

        return qcow2Processor;
    }

    public String generateUUIDName() {
        return UUID.randomUUID().toString();
    }

    public Connect getConnectionByType(final String hvsType) throws LibvirtException {
        return LibvirtConnection.getConnectionByType(hvsType);
    }

    public String retrieveSshKeysPath() {
        return LibvirtComputingResource.SSHKEYSPATH;
    }

    public String retrieveSshPubKeyPath() {
        return LibvirtComputingResource.SSHPUBKEYPATH;
    }

    public String retrieveSshPrvKeyPath() {
        return LibvirtComputingResource.SSHPRVKEYPATH;
    }

    public String retrieveBashScriptPath() {
        return LibvirtComputingResource.BASH_SCRIPT_PATH;
    }

    public Connect retrieveQemuConnection(final String qemuURI) throws LibvirtException {
        return new Connect(qemuURI);
    }

    public Script buildScript(final String scriptPath) {
        final Script script = new Script(scriptPath, TIMEOUT);
        return script;
    }

    public String generateVMSnapshotXML(VMSnapshotTO snapshot, VMSnapshotTO parent, String domainXmlDesc) {
        String parentName = (parent == null)? "": ("  <parent><name>" + parent.getSnapshotName() + "</name></parent>\n");
        String vmSnapshotXML = "<domainsnapshot>\n"
                + "  <name>" + snapshot.getSnapshotName() + "</name>\n"
                + "  <state>running</state>\n"
                + parentName
                + "  <creationTime>" + (int) Math.rint(snapshot.getCreateTime()/1000) + "</creationTime>\n"
                + domainXmlDesc
                + "</domainsnapshot>";
        return vmSnapshotXML;
    }

    /**
     * Validates if the libvirt's version is equal or higher than the parameter.
     * @param conn The libvirt connection to retrieve the version.
     * @param version The version to validate against.
     */
    protected static Pair<String, Boolean> isLibvirtVersionEqualOrHigherThanVersionInParameter(Connect conn, long version) {
        try {
            long currentLibvirtVersion = conn.getLibVirVersion();
            return new Pair<>(String.valueOf(currentLibvirtVersion), currentLibvirtVersion >= version);
        } catch (LibvirtException ex) {
            String exceptionMessage = ex.getMessage();
            s_logger.error(String.format("Unable to validate if the Libvirt's version is equal or higher than [%s] due to [%s]. Returning 'false' as default'.", version,
                    exceptionMessage), ex);
            return new Pair<>(String.format("Unknown due to [%s]", exceptionMessage), false);
        }
    }

    /**
     * Validates if Libvirt supports the flag '--delete' on command 'virsh blockcommit'.
     */
    public static boolean isLibvirtSupportingFlagDeleteOnCommandVirshBlockcommit(Connect conn) {
        Pair<String, Boolean> result = isLibvirtVersionEqualOrHigherThanVersionInParameter(conn, LIBVIRT_VERSION_THAT_SUPPORTS_FLAG_DELETE_ON_COMMAND_VIRSH_BLOCKCOMMIT);
        s_logger.debug(String.format("The current Libvirt's version [%s]%s supports the flag '--delete' on command 'virsh blockcommit'.", result.first(),
                result.second() ? "" : " does not"));
        return result.second();
    }
}
