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
import java.io.InputStream;
import java.io.IOException;
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

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo.DomainState;
import org.libvirt.LibvirtException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.DiskDef;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.MigrateKVMAsync;
import com.cloud.hypervisor.kvm.resource.VifDriver;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;

@ResourceWrapper(handles =  MigrateCommand.class)
public final class LibvirtMigrateCommandWrapper extends CommandWrapper<MigrateCommand, Answer, LibvirtComputingResource> {

    private static final String GRAPHICS_ELEM_END = "/graphics>";
    private static final String GRAPHICS_ELEM_START = "<graphics";
    private static final String CONTENTS_WILDCARD = "(?s).*";
    private static final Logger s_logger = Logger.getLogger(LibvirtMigrateCommandWrapper.class);

    @Override
    public Answer execute(final MigrateCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String vmName = command.getVmName();

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

            if (MapUtils.isNotEmpty(mapMigrateStorage)) {
                xmlDesc = replaceStorage(xmlDesc, mapMigrateStorage);
            }

            dconn = libvirtUtilitiesHelper.retrieveQemuConnection("qemu+tcp://" + command.getDestinationIp() + "/system");

            //run migration in thread so we can monitor it
            s_logger.info("Live migration of instance " + vmName + " initiated");
            final ExecutorService executor = Executors.newFixedThreadPool(1);
            final Callable<Domain> worker = new MigrateKVMAsync(libvirtComputingResource, dm, dconn, xmlDesc, MapUtils.isNotEmpty(mapMigrateStorage),
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
                if (migratePauseAfter > 0 && sleeptime > migratePauseAfter && dm.getInfo().state == DomainState.VIR_DOMAIN_RUNNING ) {
                    s_logger.info("Pausing VM " + vmName + " due to property vm.migrate.pauseafter setting to " + migratePauseAfter+ "ms to complete migration");
                    try {
                        dm.suspend();
                    } catch (final LibvirtException e) {
                        // pause could be racy if it attempts to pause right when vm is finished, simply warn
                        s_logger.info("Failed to pause vm " + vmName + " : " + e.getMessage());
                    }
                }
            }
            s_logger.info("Migration thread for " + vmName + " is done");

            destDomain = migrateThread.get(10, TimeUnit.SECONDS);

            if (destDomain != null) {
                for (final DiskDef disk : disks) {
                    libvirtComputingResource.cleanupDisk(disk);
                }
            }

        } catch (final LibvirtException e) {
            s_logger.debug("Can't migrate domain: " + e.getMessage());
            result = e.getMessage();
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

    // Pass in a list of the disks to update in the XML (xmlDesc). Each disk passed in needs to have a serial number. If any disk's serial number in the
    // list does not match a disk in the XML, an exception should be thrown.
    // In addition to the serial number, each disk in the list needs the following info:
    //   * The value of the 'type' of the disk (ex. file, block)
    //   * The value of the 'type' of the driver of the disk (ex. qcow2, raw)
    //   * The source of the disk needs an attribute that is either 'file' or 'dev' as well as its corresponding value.
    private String replaceStorage(String xmlDesc, Map<String, MigrateCommand.MigrateDiskInfo> migrateStorage)
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

                        String sourceFileDevText = getSourceFileDevText(diskNode);

                        String path = getPathFromSourceFileDevText(migrateStorage.keySet(), sourceFileDevText);

                        if (path != null) {
                            MigrateCommand.MigrateDiskInfo migrateDiskInfo = migrateStorage.remove(path);

                            NamedNodeMap diskNodeAttributes = diskNode.getAttributes();
                            Node diskNodeAttribute = diskNodeAttributes.getNamedItem("type");

                            diskNodeAttribute.setTextContent(migrateDiskInfo.getDiskType().toString());

                            NodeList diskChildNodes = diskNode.getChildNodes();

                            for (int z = 0; z < diskChildNodes.getLength(); z++) {
                                Node diskChildNode = diskChildNodes.item(z);

                                if ("driver".equals(diskChildNode.getNodeName())) {
                                    Node driverNode = diskChildNode;

                                    NamedNodeMap driverNodeAttributes = driverNode.getAttributes();
                                    Node driverNodeAttribute = driverNodeAttributes.getNamedItem("type");

                                    driverNodeAttribute.setTextContent(migrateDiskInfo.getDriverType().toString());
                                } else if ("source".equals(diskChildNode.getNodeName())) {
                                    diskNode.removeChild(diskChildNode);

                                    Element newChildSourceNode = doc.createElement("source");

                                    newChildSourceNode.setAttribute(migrateDiskInfo.getSource().toString(), migrateDiskInfo.getSourceText());

                                    diskNode.appendChild(newChildSourceNode);
                                } else if ("auth".equals(diskChildNode.getNodeName())) {
                                    diskNode.removeChild(diskChildNode);
                                } else if ("iotune".equals(diskChildNode.getNodeName())) {
                                    diskNode.removeChild(diskChildNode);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!migrateStorage.isEmpty()) {
            throw new CloudRuntimeException("Disk info was passed into LibvirtMigrateCommandWrapper.replaceStorage that was not used.");
        }

        return getXml(doc);
    }

    private String getPathFromSourceFileDevText(Set<String> paths, String sourceFileDevText) {
        if (paths != null && sourceFileDevText != null) {
            for (String path : paths) {
                if (sourceFileDevText.contains(path)) {
                    return path;
                }
            }
        }

        return null;
    }

    private String getSourceFileDevText(Node diskNode) {
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
