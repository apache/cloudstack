//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.cloud.agent.api.to.DpdkTO;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.LibvirtException;
import org.libvirt.StorageVol;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.MigrateCommand.MigrateDiskInfo;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.MigrateKVMAsync;
import com.cloud.hypervisor.kvm.resource.VifDriver;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Strings;

@ResourceWrapper(handles =  MigrateCommand.class)
public final class LibvirtMigrateCommandWrapper extends CommandWrapper<MigrateCommand, Answer, LibvirtComputingResource> {

    private static final String GRAPHICS_ELEM_END = "/graphics>";
    private static final String GRAPHICS_ELEM_START = "<graphics";
    private static final String CONTENTS_WILDCARD = "(?s).*";
    private static final Logger s_logger = Logger.getLogger(LibvirtMigrateCommandWrapper.class);

    protected String createMigrationURI(final String destinationIp, final LibvirtComputingResource libvirtComputingResource) {
        if (Strings.isNullOrEmpty(destinationIp)) {
            throw new CloudRuntimeException("Provided libvirt destination ip is invalid");
        }
        return String.format("%s://%s/system", libvirtComputingResource.isHostSecured() ? "qemu+tls" : "qemu+tcp", destinationIp);
    }

    @Override
    public Answer execute(final MigrateCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String vmName = command.getVmName();
        final String destinationUri = createMigrationURI(command.getDestinationIp(), libvirtComputingResource);
        final List<MigrateDiskInfo> migrateDiskInfoList = command.getMigrateDiskInfoList();

        String result = null;

        List<InterfaceDef> ifaces = null;
        List<DiskDef> disks;

        Domain dm = null;
        Connect dconn = null;
        Domain destDomain = null;
        Connect conn = null;
        String xmlDesc = null;
        List<Ternary<String, Boolean, String>> vmsnapshots = null;

        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

            conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            ifaces = libvirtComputingResource.getInterfaces(conn, vmName);
            disks = libvirtComputingResource.getDisks(conn, vmName);
            dm = conn.domainLookupByName(vmName);
            /*
                We replace the private IP address with the address of the destination host.
                This is because the VNC listens on the private IP address of the hypervisor,
                but that address is of course different on the target host.

                MigrateCommand.getDestinationIp() returns the private IP address of the target
                hypervisor. So it's safe to use.

                The Domain.migrate method from libvirt supports passing a different XML
                description for the instance to be used on the target host.

                This is supported by libvirt-java from version 0.50.0

                CVE-2015-3252: Get XML with sensitive information suitable for migration by using
                               VIR_DOMAIN_XML_MIGRATABLE flag (value = 8)
                               https://libvirt.org/html/libvirt-libvirt-domain.html#virDomainXMLFlags

                               Use VIR_DOMAIN_XML_SECURE (value = 1) prior to v1.0.0.
             */
            final int xmlFlag = conn.getLibVirVersion() >= 1000000 ? 8 : 1; // 1000000 equals v1.0.0

            final String target = command.getDestinationIp();
            xmlDesc = dm.getXMLDesc(xmlFlag);
            xmlDesc = replaceIpForVNCInDescFile(xmlDesc, target);

            // delete the metadata of vm snapshots before migration
            vmsnapshots = libvirtComputingResource.cleanVMSnapshotMetadata(dm);

            Map<String, MigrateCommand.MigrateDiskInfo> mapMigrateStorage = command.getMigrateStorage();
            // migrateStorage is declared as final because the replaceStorage method may mutate mapMigrateStorage, but
            // migrateStorage's value should always only be associated with the initial state of mapMigrateStorage.
            final boolean migrateStorage = MapUtils.isNotEmpty(mapMigrateStorage);
            final boolean migrateStorageManaged = command.isMigrateStorageManaged();

            if (migrateStorage) {
                xmlDesc = replaceStorage(xmlDesc, mapMigrateStorage, migrateStorageManaged);
            }

            Map<String, DpdkTO> dpdkPortsMapping = command.getDpdkInterfaceMapping();
            if (MapUtils.isNotEmpty(dpdkPortsMapping)) {
                xmlDesc = replaceDpdkInterfaces(xmlDesc, dpdkPortsMapping);
            }

            dconn = libvirtUtilitiesHelper.retrieveQemuConnection(destinationUri);

            //run migration in thread so we can monitor it
            s_logger.info("Live migration of instance " + vmName + " initiated to destination host: " + dconn.getURI());
            final ExecutorService executor = Executors.newFixedThreadPool(1);
            boolean migrateNonSharedInc = command.isMigrateNonSharedInc() && !migrateStorageManaged;

            final Callable<Domain> worker = new MigrateKVMAsync(libvirtComputingResource, dm, dconn, xmlDesc,
                    migrateStorage, migrateNonSharedInc,
                    command.isAutoConvergence(), vmName, command.getDestinationIp());
            final Future<Domain> migrateThread = executor.submit(worker);
            executor.shutdown();
            long sleeptime = 0;
            while (!executor.isTerminated()) {
                Thread.sleep(100);
                sleeptime += 100;
                if (sleeptime == 1000) { // wait 1s before attempting to set downtime on migration, since I don't know of a VIR_DOMAIN_MIGRATING state
                    final int migrateDowntime = libvirtComputingResource.getMigrateDowntime();
                    if (migrateDowntime > 0 ) {
                        try {
                            final int setDowntime = dm.migrateSetMaxDowntime(migrateDowntime);
                            if (setDowntime == 0 ) {
                                s_logger.debug("Set max downtime for migration of " + vmName + " to " + String.valueOf(migrateDowntime) + "ms");
                            }
                        } catch (final LibvirtException e) {
                            s_logger.debug("Failed to set max downtime for migration, perhaps migration completed? Error: " + e.getMessage());
                        }
                    }
                }
                if (sleeptime % 1000 == 0) {
                    s_logger.info("Waiting for migration of " + vmName + " to complete, waited " + sleeptime + "ms");
                }

                // pause vm if we meet the vm.migrate.pauseafter threshold and not already paused
                final int migratePauseAfter = libvirtComputingResource.getMigratePauseAfter();
                if (migratePauseAfter > 0 && sleeptime > migratePauseAfter) {
                    DomainState state = null;
                    try {
                        state = dm.getInfo().state;
                    } catch (final LibvirtException e) {
                        s_logger.info("Couldn't get VM domain state after " + sleeptime + "ms: " + e.getMessage());
                    }
                    if (state != null && state == DomainState.VIR_DOMAIN_RUNNING) {
                        try {
                            s_logger.info("Pausing VM " + vmName + " due to property vm.migrate.pauseafter setting to " + migratePauseAfter + "ms to complete migration");
                            dm.suspend();
                        } catch (final LibvirtException e) {
                            // pause could be racy if it attempts to pause right when vm is finished, simply warn
                            s_logger.info("Failed to pause vm " + vmName + " : " + e.getMessage());
                        }
                    }
                }
            }
            s_logger.info("Migration thread for " + vmName + " is done");

            destDomain = migrateThread.get(10, TimeUnit.SECONDS);

            if (destDomain != null) {
                deleteOrDisconnectDisksOnSourcePool(libvirtComputingResource, migrateDiskInfoList, disks);
            }

        } catch (final LibvirtException e) {
            s_logger.debug("Can't migrate domain: " + e.getMessage());
            result = e.getMessage();
            if (result.startsWith("unable to connect to server") && result.endsWith("refused")) {
                result = String.format("Migration was refused connection to destination: %s. Please check libvirt configuration compatibility and firewall rules on the source and destination hosts.", destinationUri);
            }
        } catch (final InterruptedException e) {
            s_logger.debug("Interrupted while migrating domain: " + e.getMessage());
            result = e.getMessage();
        } catch (final ExecutionException e) {
            s_logger.debug("Failed to execute while migrating domain: " + e.getMessage());
            result = e.getMessage();
        } catch (final TimeoutException e) {
            s_logger.debug("Timed out while migrating domain: " + e.getMessage());
            result = e.getMessage();
        } catch (final IOException e) {
            s_logger.debug("IOException: " + e.getMessage());
            result = e.getMessage();
        } catch (final ParserConfigurationException e) {
            s_logger.debug("ParserConfigurationException: " + e.getMessage());
            result = e.getMessage();
        } catch (final SAXException e) {
            s_logger.debug("SAXException: " + e.getMessage());
            result = e.getMessage();
        } catch (final TransformerConfigurationException e) {
            s_logger.debug("TransformerConfigurationException: " + e.getMessage());
            result = e.getMessage();
        } catch (final TransformerException e) {
            s_logger.debug("TransformerException: " + e.getMessage());
            result = e.getMessage();
        } finally {
            try {
                if (dm != null && result != null) {
                    // restore vm snapshots in case of failed migration
                    if (vmsnapshots != null) {
                        libvirtComputingResource.restoreVMSnapshotMetadata(dm, vmName, vmsnapshots);
                    }
                }
                if (dm != null) {
                    if (dm.isPersistent() == 1) {
                        dm.undefine();
                    }
                    dm.free();
                }
                if (dconn != null) {
                    dconn.close();
                }
                if (destDomain != null) {
                    destDomain.free();
                }
            } catch (final LibvirtException e) {
                s_logger.trace("Ignoring libvirt error.", e);
            }
        }

        if (result != null) {
        } else {
            libvirtComputingResource.destroyNetworkRulesForVM(conn, vmName);
            for (final InterfaceDef iface : ifaces) {
                // We don't know which "traffic type" is associated with
                // each interface at this point, so inform all vif drivers
                final List<VifDriver> allVifDrivers = libvirtComputingResource.getAllVifDrivers();
                for (final VifDriver vifDriver : allVifDrivers) {
                    vifDriver.unplug(iface);
                }
            }
        }

        return new MigrateAnswer(command, result == null, result, null);
    }

    /**
     * Replace DPDK source path and target before migrations
     */
    protected String replaceDpdkInterfaces(String xmlDesc, Map<String, DpdkTO> dpdkPortsMapping) throws TransformerException, ParserConfigurationException, IOException, SAXException {
        InputStream in = IOUtils.toInputStream(xmlDesc);

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(in);

        // Get the root element
        Node domainNode = doc.getFirstChild();

        NodeList domainChildNodes = domainNode.getChildNodes();

        for (int i = 0; i < domainChildNodes.getLength(); i++) {
            Node domainChildNode = domainChildNodes.item(i);

            if ("devices".equals(domainChildNode.getNodeName())) {
                NodeList devicesChildNodes = domainChildNode.getChildNodes();

                for (int x = 0; x < devicesChildNodes.getLength(); x++) {
                    Node deviceChildNode = devicesChildNodes.item(x);

                    if ("interface".equals(deviceChildNode.getNodeName())) {
                        Node interfaceNode = deviceChildNode;
                        NamedNodeMap attributes = interfaceNode.getAttributes();
                        Node interfaceTypeAttr = attributes.getNamedItem("type");

                        if ("vhostuser".equals(interfaceTypeAttr.getNodeValue())) {
                            NodeList diskChildNodes = interfaceNode.getChildNodes();

                            String mac = null;
                            for (int y = 0; y < diskChildNodes.getLength(); y++) {
                                Node diskChildNode = diskChildNodes.item(y);
                                if (!"mac".equals(diskChildNode.getNodeName())) {
                                    continue;
                                }
                                mac = diskChildNode.getAttributes().getNamedItem("address").getNodeValue();
                            }

                            if (StringUtils.isNotBlank(mac)) {
                                DpdkTO to = dpdkPortsMapping.get(mac);

                                for (int z = 0; z < diskChildNodes.getLength(); z++) {
                                    Node diskChildNode = diskChildNodes.item(z);

                                    if ("target".equals(diskChildNode.getNodeName())) {
                                        Node targetNode = diskChildNode;
                                        Node targetNodeAttr = targetNode.getAttributes().getNamedItem("dev");
                                        targetNodeAttr.setNodeValue(to.getPort());
                                    } else if ("source".equals(diskChildNode.getNodeName())) {
                                        Node sourceNode = diskChildNode;
                                        NamedNodeMap attrs = sourceNode.getAttributes();
                                        Node path = attrs.getNamedItem("path");
                                        path.setNodeValue(to.getPath() + "/" + to.getPort());
                                        Node mode = attrs.getNamedItem("mode");
                                        mode.setNodeValue(to.getMode());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return getXml(doc);
    }

    /**
     * In case of a local file, it deletes the file on the source host/storage pool. Otherwise (for instance iScsi) it disconnects the disk on the source storage pool. </br>
     * This method must be executed after a successful migration to a target storage pool, cleaning up the source storage.
     */
    protected void deleteOrDisconnectDisksOnSourcePool(final LibvirtComputingResource libvirtComputingResource, final List<MigrateDiskInfo> migrateDiskInfoList,
            List<DiskDef> disks) {
        for (DiskDef disk : disks) {
            MigrateDiskInfo migrateDiskInfo = searchDiskDefOnMigrateDiskInfoList(migrateDiskInfoList, disk);
            if (migrateDiskInfo != null && migrateDiskInfo.isSourceDiskOnStorageFileSystem()) {
                deleteLocalVolume(disk.getDiskPath());
            } else {
                libvirtComputingResource.cleanupDisk(disk);
            }
        }
    }

    /**
     * Deletes the local volume from the storage pool.
     */
    protected void deleteLocalVolume(String localPath) {
        try {
            Connect conn = LibvirtConnection.getConnection();
            StorageVol storageVolLookupByPath = conn.storageVolLookupByPath(localPath);
            storageVolLookupByPath.delete(0);
        } catch (LibvirtException e) {
            s_logger.error(String.format("Cannot delete local volume [%s] due to: %s", localPath, e));
        }
    }

    /**
     * Searches for a {@link MigrateDiskInfo} with the path matching the {@link DiskDef} path.
     */
    protected MigrateDiskInfo searchDiskDefOnMigrateDiskInfoList(List<MigrateDiskInfo> migrateDiskInfoList, DiskDef disk) {
        for (MigrateDiskInfo migrateDiskInfo : migrateDiskInfoList) {
            if (StringUtils.contains(disk.getDiskPath(), migrateDiskInfo.getSerialNumber())) {
                return migrateDiskInfo;
            }
        }
        s_logger.debug(String.format("Cannot find Disk [uuid: %s] on the list of disks to be migrated", disk.getDiskPath()));
        return null;
    }

    /**
     * This function assumes an qemu machine description containing a single graphics element like
     *     <graphics type='vnc' port='5900' autoport='yes' listen='10.10.10.1'>
     *       <listen type='address' address='10.10.10.1'/>
     *     </graphics>
     * @param xmlDesc the qemu xml description
     * @param target the ip address to migrate to
     * @return the new xmlDesc
     */
    String replaceIpForVNCInDescFile(String xmlDesc, final String target) {
        final int begin = xmlDesc.indexOf(GRAPHICS_ELEM_START);
        if (begin >= 0) {
            final int end = xmlDesc.lastIndexOf(GRAPHICS_ELEM_END) + GRAPHICS_ELEM_END.length();
            if (end > begin) {
                String graphElem = xmlDesc.substring(begin, end);
                graphElem = graphElem.replaceAll("listen='[a-zA-Z0-9\\.]*'", "listen='" + target + "'");
                graphElem = graphElem.replaceAll("address='[a-zA-Z0-9\\.]*'", "address='" + target + "'");
                xmlDesc = xmlDesc.replaceAll(GRAPHICS_ELEM_START + CONTENTS_WILDCARD + GRAPHICS_ELEM_END, graphElem);
            }
        }
        return xmlDesc;
    }

    /**
     * Pass in a list of the disks to update in the XML (xmlDesc). Each disk passed in needs to have a serial number. If any disk's serial number in the
     * list does not match a disk in the XML, an exception should be thrown.
     * In addition to the serial number, each disk in the list needs the following info:
     * <ul>
     *  <li>The value of the 'type' of the disk (ex. file, block)
     *  <li>The value of the 'type' of the driver of the disk (ex. qcow2, raw)
     *  <li>The source of the disk needs an attribute that is either 'file' or 'dev' as well as its corresponding value.
     * </ul>
     */
    protected String replaceStorage(String xmlDesc, Map<String, MigrateCommand.MigrateDiskInfo> migrateStorage,
                                  boolean migrateStorageManaged)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {
        InputStream in = IOUtils.toInputStream(xmlDesc);

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(in);

        // Get the root element
        Node domainNode = doc.getFirstChild();

        NodeList domainChildNodes = domainNode.getChildNodes();

        for (int i = 0; i < domainChildNodes.getLength(); i++) {
            Node domainChildNode = domainChildNodes.item(i);

            if ("devices".equals(domainChildNode.getNodeName())) {
                NodeList devicesChildNodes = domainChildNode.getChildNodes();

                for (int x = 0; x < devicesChildNodes.getLength(); x++) {
                    Node deviceChildNode = devicesChildNodes.item(x);

                    if ("disk".equals(deviceChildNode.getNodeName())) {
                        Node diskNode = deviceChildNode;

                        String sourceText = getSourceText(diskNode);

                        String path = getPathFromSourceText(migrateStorage.keySet(), sourceText);

                        if (path != null) {
                            MigrateCommand.MigrateDiskInfo migrateDiskInfo = migrateStorage.get(path);

                            NamedNodeMap diskNodeAttributes = diskNode.getAttributes();
                            Node diskNodeAttribute = diskNodeAttributes.getNamedItem("type");

                            diskNodeAttribute.setTextContent(migrateDiskInfo.getDiskType().toString());

                            NodeList diskChildNodes = diskNode.getChildNodes();

                            for (int z = 0; z < diskChildNodes.getLength(); z++) {
                                Node diskChildNode = diskChildNodes.item(z);

                                if (migrateStorageManaged && "driver".equals(diskChildNode.getNodeName())) {
                                    Node driverNode = diskChildNode;

                                    NamedNodeMap driverNodeAttributes = driverNode.getAttributes();
                                    Node driverNodeAttribute = driverNodeAttributes.getNamedItem("type");

                                    driverNodeAttribute.setTextContent(migrateDiskInfo.getDriverType().toString());
                                } else if ("source".equals(diskChildNode.getNodeName())) {
                                    diskNode.removeChild(diskChildNode);

                                    Element newChildSourceNode = doc.createElement("source");

                                    newChildSourceNode.setAttribute(migrateDiskInfo.getSource().toString(), migrateDiskInfo.getSourceText());

                                    diskNode.appendChild(newChildSourceNode);
                                } else if (migrateStorageManaged && "auth".equals(diskChildNode.getNodeName())) {
                                    diskNode.removeChild(diskChildNode);
                                }
                            }
                        }
                    }
                }
            }
        }

        return getXml(doc);
    }

    private String getPathFromSourceText(Set<String> paths, String sourceText) {
        if (paths != null && !StringUtils.isBlank(sourceText)) {
            for (String path : paths) {
                if (sourceText.contains(path)) {
                    return path;
                }
            }
        }

        return null;
    }

    private String getSourceText(Node diskNode) {
        NodeList diskChildNodes = diskNode.getChildNodes();

        for (int i = 0; i < diskChildNodes.getLength(); i++) {
            Node diskChildNode = diskChildNodes.item(i);

            if ("source".equals(diskChildNode.getNodeName())) {
                NamedNodeMap diskNodeAttributes = diskChildNode.getAttributes();

                Node diskNodeAttribute = diskNodeAttributes.getNamedItem("file");

                if (diskNodeAttribute != null) {
                    return diskNodeAttribute.getTextContent();
                }

                diskNodeAttribute = diskNodeAttributes.getNamedItem("dev");

                if (diskNodeAttribute != null) {
                    return diskNodeAttribute.getTextContent();
                }

                diskNodeAttribute = diskNodeAttributes.getNamedItem("protocol");

                if (diskNodeAttribute != null) {
                    String textContent = diskNodeAttribute.getTextContent();

                    if ("rbd".equalsIgnoreCase(textContent)) {
                        diskNodeAttribute = diskNodeAttributes.getNamedItem("name");

                        if (diskNodeAttribute != null) {
                            return diskNodeAttribute.getTextContent();
                        }
                    }
                }
            }
        }

        return null;
    }

    private String getXml(Document doc) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        DOMSource source = new DOMSource(doc);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(byteArrayOutputStream);

        transformer.transform(source, result);

        return byteArrayOutputStream.toString();
    }
}
