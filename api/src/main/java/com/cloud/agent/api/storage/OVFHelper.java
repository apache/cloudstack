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
package com.cloud.agent.api.storage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.cloud.agent.api.to.deployasis.OVFConfigurationTO;
import com.cloud.agent.api.to.deployasis.OVFEulaSectionTO;
import com.cloud.agent.api.to.deployasis.OVFPropertyTO;
import com.cloud.agent.api.to.deployasis.OVFVirtualHardwareItemTO;
import com.cloud.agent.api.to.deployasis.OVFVirtualHardwareSectionTO;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.exception.InternalErrorException;
import com.cloud.utils.Pair;
import com.cloud.utils.compression.CompressionUtil;
import com.cloud.agent.api.to.deployasis.OVFNetworkTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import com.cloud.agent.api.to.DatadiskTO;
import com.cloud.utils.exception.CloudRuntimeException;
import org.xml.sax.SAXException;

public class OVFHelper {
    private static final Logger s_logger = Logger.getLogger(OVFHelper.class);

    private final OVFParser ovfParser;

    public OVFHelper() {
        ovfParser = new OVFParser();
    }

    public OVFParser getOvfParser() {
        return this.ovfParser;
    }

    /**
     * Get disk virtual size given its values on fields: 'ovf:capacity' and 'ovf:capacityAllocationUnits'
     * @param capacity capacity
     * @param allocationUnits capacity allocation units
     * @return disk virtual size
     */
    public static Long getDiskVirtualSize(Long capacity, String allocationUnits, String ovfFilePath) throws InternalErrorException {
        if ((capacity != 0) && (allocationUnits != null)) {
            long units = 1;
            if (allocationUnits.equalsIgnoreCase("KB") || allocationUnits.equalsIgnoreCase("KiloBytes") || allocationUnits.equalsIgnoreCase("byte * 2^10")) {
                units = ResourceType.bytesToKiB;
            } else if (allocationUnits.equalsIgnoreCase("MB") || allocationUnits.equalsIgnoreCase("MegaBytes") || allocationUnits.equalsIgnoreCase("byte * 2^20")) {
                units = ResourceType.bytesToMiB;
            } else if (allocationUnits.equalsIgnoreCase("GB") || allocationUnits.equalsIgnoreCase("GigaBytes") || allocationUnits.equalsIgnoreCase("byte * 2^30")) {
                units = ResourceType.bytesToGiB;
            }
            return capacity * units;
        } else {
            throw new InternalErrorException("Failed to read capacity and capacityAllocationUnits from the OVF file: " + ovfFilePath);
        }
    }

    /**
     * Create OVFProperty class from the parsed node. Note that some fields may not be present.
     * The key attribute is required
     */
    protected OVFPropertyTO createOVFPropertyFromNode(Node node, int index, String category) {
        Element element = (Element) node;
        String key = ovfParser.getNodeAttribute(element, "key");
        if (StringUtils.isBlank(key)) {
            return null;
        }

        String value = ovfParser.getNodeAttribute(element, "value");
        String type = ovfParser.getNodeAttribute(element, "type");
        String qualifiers = ovfParser.getNodeAttribute(element, "qualifiers");
        String userConfigurableStr = ovfParser.getNodeAttribute(element, "userConfigurable");
        boolean userConfigurable = StringUtils.isNotBlank(userConfigurableStr) &&
                userConfigurableStr.equalsIgnoreCase("true");
        String passStr = ovfParser.getNodeAttribute(element, "password");
        boolean password = StringUtils.isNotBlank(passStr) && passStr.equalsIgnoreCase("true");
        String label = ovfParser.getChildNodeValue(node, "Label");
        String description = ovfParser.getChildNodeValue(node, "Description");
        s_logger.debug("Creating OVF property index " + index + (category == null ? "" : " for category " + category)
                + " with key = " + key);
        return new OVFPropertyTO(key, type, value, qualifiers, userConfigurable,
                label, description, password, index, category);
    }

    /**
     * Retrieve OVF properties from a parsed OVF file including its category (if available) and in-order,
     * with attribute 'ovf:userConfigurable' set to true.
     */
    public List<OVFPropertyTO> getConfigurableOVFPropertiesFromDocument(Document doc) {
        List<OVFPropertyTO> props = new ArrayList<>();
        if (doc == null) {
            return props;
        }
        int propertyIndex = 0;
        NodeList productSections = ovfParser.getElementsFromOVFDocument(doc, "ProductSection");
        if (productSections != null) {
            String lastCategoryFound = null;
            for (int i = 0; i < productSections.getLength(); i++) {
                Node node = productSections.item(i);
                if (node == null) {
                    continue;
                }
                NodeList childNodes = node.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node child = childNodes.item(j);
                    if (child == null) {
                        continue;
                    }
                    if (child.getNodeName().equalsIgnoreCase("Category") ||
                            child.getNodeName().endsWith(":Category")) {
                        lastCategoryFound = child.getTextContent();
                        s_logger.info("Category found " + lastCategoryFound);
                    } else if (child.getNodeName().equalsIgnoreCase("Property") ||
                            child.getNodeName().endsWith(":Property")) {
                        OVFPropertyTO prop = createOVFPropertyFromNode(child, propertyIndex, lastCategoryFound);
                        if (prop != null && prop.isUserConfigurable()) {
                            props.add(prop);
                            propertyIndex++;
                        }
                    }
                }
            }
        }
        return props;
    }

    /**
     * Get properties from OVF XML string
     */
    protected List<OVFPropertyTO> getOVFPropertiesFromXmlString(final String ovfString) throws IOException, SAXException {
        final Document doc = ovfParser.parseOVF(ovfString);
        return getConfigurableOVFPropertiesFromDocument(doc);
    }

    protected Pair<String, String> getOperatingSystemInfoFromXmlString(final String ovfString) throws IOException, SAXException {
        final Document doc = ovfParser.parseOVF(ovfString);
        return getOperatingSystemInfoFromDocument(doc);
    }

    protected List<OVFConfigurationTO> getOVFDeploymentOptionsFromXmlString(final String ovfString) throws IOException, SAXException {
        final Document doc = ovfParser.parseOVF(ovfString);
        return getDeploymentOptionsFromDocumentTree(doc);
    }

    protected List<OVFVirtualHardwareItemTO> getOVFVirtualHardwareSectionFromXmlString(final String ovfString) throws IOException, SAXException {
        final Document doc = ovfParser.parseOVF(ovfString);
        return getVirtualHardwareItemsFromDocumentTree(doc);
    }

    protected OVFVirtualHardwareSectionTO getVirtualHardwareSectionFromXmlString(final String ovfString) throws IOException, SAXException {
        final Document doc = ovfParser.parseOVF(ovfString);
        return getVirtualHardwareSectionFromDocument(doc);
    }

    protected List<OVFEulaSectionTO> getOVFEulaSectionFromXmlString(final String ovfString) throws IOException, SAXException {
        final Document doc = ovfParser.parseOVF(ovfString);
        return getEulaSectionsFromDocument(doc);
    }

    public List<DatadiskTO> getOVFVolumeInfoFromFile(final String ovfFilePath, final String configurationId) throws InternalErrorException {
        if (StringUtils.isBlank(ovfFilePath)) {
            return new ArrayList<>();
        }
        Document doc = ovfParser.parseOVFFile(ovfFilePath);

        return getOVFVolumeInfoFromFile(ovfFilePath, doc, configurationId);
    }

    public List<DatadiskTO> getOVFVolumeInfoFromFile(String ovfFilePath, Document doc, String configurationId) throws InternalErrorException {
        if (StringUtils.isBlank(ovfFilePath)) {
            return null;
        }

        File ovfFile = new File(ovfFilePath);
        List<OVFVirtualHardwareItemTO> hardwareItems = getVirtualHardwareItemsFromDocumentTree(doc);
        List<OVFFile> files = extractFilesFromOvfDocumentTree(ovfFile, doc);
        List<OVFDisk> disks = extractDisksFromOvfDocumentTree(doc);

        List<OVFVirtualHardwareItemTO> diskHardwareItems = hardwareItems.stream()
                .filter(x -> x.getResourceType() == OVFVirtualHardwareItemTO.HardwareResourceType.DiskDrive &&
                        hardwareItemContainsConfiguration(x, configurationId))
                .collect(Collectors.toList());
        return matchHardwareItemsToDiskAndFilesInformation(diskHardwareItems, files, disks, ovfFile.getParent());
    }

    private String extractDiskIdFromDiskHostResource(String hostResource) {
        if (hostResource.startsWith("ovf:/disk/")) {
            return hostResource.replace("ovf:/disk/", "");
        }
        String[] resourceParts = hostResource.split("/");
        return resourceParts[resourceParts.length - 1];
    }

    private OVFDisk getDiskDefinitionFromDiskId(String diskId, List<OVFDisk> disks) {
        for (OVFDisk disk : disks) {
            if (disk._diskId.equalsIgnoreCase(diskId)) {
                return disk;
            }
        }
        return null;
    }

    private List<DatadiskTO> matchHardwareItemsToDiskAndFilesInformation(List<OVFVirtualHardwareItemTO> diskHardwareItems,
                                                                         List<OVFFile> files, List<OVFDisk> disks,
                                                                         String ovfParentPath) throws InternalErrorException {
        List<DatadiskTO> diskTOs = new LinkedList<>();
        int diskNumber = 0;
        for (OVFVirtualHardwareItemTO diskItem : diskHardwareItems) {
            if (StringUtils.isBlank(diskItem.getHostResource())) {
                s_logger.error("Missing disk information for hardware item " + diskItem.getElementName() + " " + diskItem.getInstanceId());
                continue;
            }
            String diskId = extractDiskIdFromDiskHostResource(diskItem.getHostResource());
            OVFDisk diskDefinition = getDiskDefinitionFromDiskId(diskId, disks);
            if (diskDefinition == null) {
                s_logger.error("Missing disk definition for disk ID " + diskId);
            }
            OVFFile fileDefinition = getFileDefinitionFromDiskDefinition(diskDefinition._fileRef, files);
            DatadiskTO datadiskTO = generateDiskTO(fileDefinition, diskDefinition, ovfParentPath, diskNumber, diskItem);
            diskTOs.add(datadiskTO);
            diskNumber++;
        }
        List<OVFFile> isoFiles = files.stream().filter(x -> x.isIso).collect(Collectors.toList());
        for (OVFFile isoFile : isoFiles) {
            DatadiskTO isoTO = generateDiskTO(isoFile, null, ovfParentPath, diskNumber, null);
            diskTOs.add(isoTO);
            diskNumber++;
        }
        return diskTOs;
    }

    private DatadiskTO generateDiskTO(OVFFile file, OVFDisk disk, String ovfParentPath, int diskNumber,
                                      OVFVirtualHardwareItemTO diskItem) throws InternalErrorException {
        String path = file != null ? ovfParentPath + File.separator + file._href : null;
        if (StringUtils.isNotBlank(path)) {
            File f = new File(path);
            if (!f.exists() || f.isDirectory()) {
                s_logger.error("One of the attached disk or iso does not exists " + path);
                throw new InternalErrorException("One of the attached disk or iso as stated on OVF does not exists " + path);
            }
        }
        Long capacity = disk != null ? disk._capacity : file._size;
        Long fileSize = file != null ? file._size : 0L;

        String controller = "";
        String controllerSubType = "";
        if (disk != null) {
            OVFDiskController cDiskController = disk._controller;
            controller = cDiskController == null ? "" : disk._controller._name;
            controllerSubType = cDiskController == null ? "" : disk._controller._subType;
        }

        boolean isIso = file != null && file.isIso;
        boolean bootable = file != null && file._bootable;
        String diskId = disk == null ? file._id : disk._diskId;
        String configuration = diskItem != null ? diskItem.getConfigurationIds() : null;
        return new DatadiskTO(path, capacity, fileSize, diskId,
                isIso, bootable, controller, controllerSubType, diskNumber, configuration);
    }

    protected List<OVFDisk> extractDisksFromOvfDocumentTree(Document doc) {
        NodeList disks = ovfParser.getElementsFromOVFDocument(doc, "Disk");
        NodeList items = ovfParser.getElementsFromOVFDocument(doc, "Item");

        ArrayList<OVFDisk> vd = new ArrayList<>();
        for (int i = 0; i < disks.getLength(); i++) {
            Element disk = (Element) disks.item(i);

            if (disk == null) {
                continue;
            }
            OVFDisk od = new OVFDisk();
            String virtualSize = ovfParser.getNodeAttribute(disk, "capacity");
            od._capacity = NumberUtils.toLong(virtualSize, 0L);
            String allocationUnits = ovfParser.getNodeAttribute(disk, "capacityAllocationUnits");
            od._diskId = ovfParser.getNodeAttribute(disk, "diskId");
            od._fileRef = ovfParser.getNodeAttribute(disk, "fileRef");
            od._populatedSize = NumberUtils.toLong(ovfParser.getNodeAttribute(disk, "populatedSize"));

            if ((od._capacity != 0) && (allocationUnits != null)) {
                long units = 1;
                if (allocationUnits.equalsIgnoreCase("KB") || allocationUnits.equalsIgnoreCase("KiloBytes") || allocationUnits.equalsIgnoreCase("byte * 2^10")) {
                    units = ResourceType.bytesToKiB;
                } else if (allocationUnits.equalsIgnoreCase("MB") || allocationUnits.equalsIgnoreCase("MegaBytes") || allocationUnits.equalsIgnoreCase("byte * 2^20")) {
                    units = ResourceType.bytesToMiB;
                } else if (allocationUnits.equalsIgnoreCase("GB") || allocationUnits.equalsIgnoreCase("GigaBytes") || allocationUnits.equalsIgnoreCase("byte * 2^30")) {
                    units = ResourceType.bytesToGiB;
                }
                od._capacity = od._capacity * units;
            }
            od._controller = getControllerType(items, od._diskId);
            vd.add(od);
        }
        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("found %d disk definitions",vd.size()));
        }
        return vd;
    }

    protected List<OVFFile> extractFilesFromOvfDocumentTree(File ovfFile, Document doc) {
        NodeList files = ovfParser.getElementsFromOVFDocument(doc, "File");
        ArrayList<OVFFile> vf = new ArrayList<>();
        boolean toggle = true;
        for (int j = 0; j < files.getLength(); j++) {
            Element file = (Element)files.item(j);
            OVFFile of = new OVFFile();
            of._href = ovfParser.getNodeAttribute(file, "href");
            if (of._href.endsWith("vmdk") || of._href.endsWith("iso")) {
                of._id = ovfParser.getNodeAttribute(file, "id");
                String size = ovfParser.getNodeAttribute(file, "size");
                if (StringUtils.isNotBlank(size)) {
                    of._size = Long.parseLong(size);
                } else {
                    String dataDiskPath = ovfFile.getParent() + File.separator + of._href;
                    File this_file = new File(dataDiskPath);
                    of._size = this_file.length();
                }
                of.isIso = of._href.endsWith("iso");
                if (toggle && !of.isIso) {
                    of._bootable = true;
                    toggle = !toggle;
                }
                vf.add(of);
            }
        }
        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("found %d file definitions in %s",vf.size(), ovfFile.getPath()));
        }
        return vf;
    }

    private OVFDiskController getControllerType(final NodeList itemList, final String diskId) {
        for (int k = 0; k < itemList.getLength(); k++) {
            Element item = (Element)itemList.item(k);
            NodeList cn = item.getChildNodes();
            for (int l = 0; l < cn.getLength(); l++) {
                if (cn.item(l) instanceof Element) {
                    Element el = (Element)cn.item(l);
                    if ("rasd:HostResource".equals(el.getNodeName())
                            && (el.getTextContent().contains("ovf:/file/" + diskId) || el.getTextContent().contains("ovf:/disk/" + diskId))) {
                        Element oe = getParentNode(itemList, item);
                        Element voe = oe;
                        while (oe != null) {
                            voe = oe;
                            oe = getParentNode(itemList, voe);
                        }
                        return getController(voe);
                    }
                }
            }
        }
        return null;
    }

    private Element getParentNode(final NodeList itemList, final Element childItem) {
        NodeList cn = childItem.getChildNodes();
        String parent_id = null;
        for (int l = 0; l < cn.getLength(); l++) {
            if (cn.item(l) instanceof Element) {
                Element el = (Element)cn.item(l);
                if ("rasd:Parent".equals(el.getNodeName())) {
                    parent_id = el.getTextContent();
                }
            }
        }
        if (parent_id != null) {
            for (int k = 0; k < itemList.getLength(); k++) {
                Element item = (Element)itemList.item(k);
                NodeList child = item.getChildNodes();
                for (int l = 0; l < child.getLength(); l++) {
                    if (child.item(l) instanceof Element) {
                        Element el = (Element)child.item(l);
                        if ("rasd:InstanceID".equals(el.getNodeName()) && el.getTextContent().trim().equals(parent_id)) {
                            return item;
                        }
                    }
                }
            }
        }
        return null;
    }

    private OVFDiskController getController(Element controllerItem) {
        OVFDiskController dc = new OVFDiskController();
        NodeList child = controllerItem.getChildNodes();
        for (int l = 0; l < child.getLength(); l++) {
            if (child.item(l) instanceof Element) {
                Element el = (Element)child.item(l);
                if ("rasd:ElementName".equals(el.getNodeName())) {
                    dc._name = el.getTextContent();
                }
                if ("rasd:ResourceSubType".equals(el.getNodeName())) {
                    dc._subType = el.getTextContent();
                }
            }
        }
        return dc;
    }

    public void rewriteOVFFileForSingleDisk(final String origOvfFilePath, final String newOvfFilePath, final String diskName) {
        final Document doc = ovfParser.parseOVFFile(origOvfFilePath);

        NodeList disks = ovfParser.getElementsFromOVFDocument(doc, "Disk");
        NodeList files = ovfParser.getElementsFromOVFDocument(doc, "File");
        NodeList items = ovfParser.getElementsFromOVFDocument(doc, "Item");
        String keepfile = null;
        List<Element> toremove = new ArrayList<>();
        for (int j = 0; j < files.getLength(); j++) {
            Element file = (Element)files.item(j);
            String href = ovfParser.getNodeAttribute(file, "href");
            if (diskName.equals(href)) {
                keepfile = ovfParser.getNodeAttribute(file, "id");
            } else {
                toremove.add(file);
            }
        }
        String keepdisk = null;
        for (int i = 0; i < disks.getLength(); i++) {
            Element disk = (Element)disks.item(i);
            String fileRef = ovfParser.getNodeAttribute(disk, "fileRef");
            if (keepfile == null) {
                s_logger.info("FATAL: OVA format error");
            } else if (keepfile.equals(fileRef)) {
                keepdisk = ovfParser.getNodeAttribute(disk, "diskId");
            } else {
                toremove.add(disk);
            }
        }
        for (int k = 0; k < items.getLength(); k++) {
            Element item = (Element) items.item(k);
            NodeList cn = item.getChildNodes();
            for (int l = 0; l < cn.getLength(); l++) {
                if (cn.item(l) instanceof Element) {
                    Element el = (Element) cn.item(l);
                    if ("rasd:HostResource".equals(el.getNodeName())
                            && !(el.getTextContent().contains("ovf:/file/" + keepdisk) || el.getTextContent().contains("ovf:/disk/" + keepdisk))) {
                        toremove.add(item);
                        break;
                    }
                }
            }
        }

        for (Element rme : toremove) {
            if (rme.getParentNode() != null) {
                rme.getParentNode().removeChild(rme);
            }
        }

        writeDocumentToFile(newOvfFilePath, doc);
    }

    private void writeDocumentToFile(String newOvfFilePath, Document doc) {
        try {

            final StringWriter writer = new StringWriter();
            final StreamResult result = new StreamResult(writer);
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();
            final DOMSource domSource = new DOMSource(doc);
            transformer.transform(domSource, result);
            PrintWriter outfile = new PrintWriter(newOvfFilePath);
            outfile.write(writer.toString());
            outfile.close();
        } catch (IOException | TransformerException e) {
            s_logger.info("Unexpected exception caught while rewriting OVF:" + e.getMessage(), e);
            throw new CloudRuntimeException(e);
        }
    }

    OVFFile getFileDefinitionFromDiskDefinition(String fileRef, List<OVFFile> files) {
        for (OVFFile file : files) {
            if (file._id.equals(fileRef)) {
                return file;
            }
        }
        return null;
    }

    public List<OVFNetworkTO> getNetPrerequisitesFromDocument(Document doc) throws InternalErrorException {
        if (doc == null) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("no document to parse; returning no prerequiste networks");
            }
            return Collections.emptyList();
        }

        Map<String, OVFNetworkTO> nets = getNetworksFromDocumentTree(doc);

        checkForOnlyOneSystemNode(doc);

        matchNicsToNets(nets, doc);

        return new ArrayList<>(nets.values());
    }

    private void matchNicsToNets(Map<String, OVFNetworkTO> nets, Node systemElement) {
        final DocumentTraversal traversal = (DocumentTraversal) systemElement;
        final NodeIterator iterator = traversal.createNodeIterator(systemElement, NodeFilter.SHOW_ELEMENT, null, true);
        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("starting out with %d network-prerequisites, parsing hardware",nets.size()));
        }
        int nicCount = 0;
        for (Node n = iterator.nextNode(); n != null; n = iterator.nextNode()) {
            final Element e = (Element) n;
            if ("rasd:Connection".equals(e.getTagName())) {
                nicCount++;
                String name = e.getTextContent(); // should be in our nets
                if(nets.get(name) == null) {
                    if(s_logger.isInfoEnabled()) {
                        s_logger.info(String.format("found a nic definition without a network definition byname %s, adding it to the list.", name));
                    }
                    nets.put(name, new OVFNetworkTO());
                }
                OVFNetworkTO thisNet = nets.get(name);
                if (e.getParentNode() != null) {
                    fillNicPrerequisites(thisNet,e.getParentNode());
                }
            }
        }
        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("ending up with %d network-prerequisites, parsed %d nics", nets.size(), nicCount));
        }
    }

    /**
     * get all the stuff from parent node
     *
     * @param nic the object to carry through the system
     * @param parentNode the xml container node for nic data
     */
    private void fillNicPrerequisites(OVFNetworkTO nic, Node parentNode) {
        String addressOnParentStr = ovfParser.getChildNodeValue(parentNode, "AddressOnParent");
        String automaticAllocationStr = ovfParser.getChildNodeValue(parentNode, "AutomaticAllocation");
        String description = ovfParser.getChildNodeValue(parentNode, "Description");
        String elementName = ovfParser.getChildNodeValue(parentNode, "ElementName");
        String instanceIdStr = ovfParser.getChildNodeValue(parentNode, "InstanceID");
        String resourceSubType = ovfParser.getChildNodeValue(parentNode, "ResourceSubType");
        String resourceType = ovfParser.getChildNodeValue(parentNode, "ResourceType");

        try {
            int addressOnParent = Integer.parseInt(addressOnParentStr);
            nic.setAddressOnParent(addressOnParent);
        } catch (NumberFormatException e) {
            s_logger.warn("Encountered element of type \"AddressOnParent\", that could not be parse to an integer number: " + addressOnParentStr);
        }

        boolean automaticAllocation = StringUtils.isNotBlank(automaticAllocationStr) && Boolean.parseBoolean(automaticAllocationStr);
        nic.setAutomaticAllocation(automaticAllocation);
        nic.setNicDescription(description);
        nic.setElementName(elementName);

        try {
            int instanceId = Integer.parseInt(instanceIdStr);
            nic.setInstanceID(instanceId);
        } catch (NumberFormatException e) {
            s_logger.warn("Encountered element of type \"InstanceID\", that could not be parse to an integer number: " + instanceIdStr);
        }

        nic.setResourceSubType(resourceSubType);
        nic.setResourceType(resourceType);
    }

    private void checkForOnlyOneSystemNode(Document doc) throws InternalErrorException {
        // get hardware VirtualSystem, for now we support only one of those
        NodeList systemElements = ovfParser.getElementsFromOVFDocument(doc, "VirtualSystem");
        if (systemElements.getLength() != 1) {
            String msg = "found " + systemElements.getLength() + " system definitions in OVA, can only handle exactly one.";
            s_logger.warn(msg);
            throw new InternalErrorException(msg);
        }
    }

    private Map<String, OVFNetworkTO> getNetworksFromDocumentTree(Document doc) {
        NodeList networkElements = ovfParser.getElementsFromOVFDocument(doc,"Network");
        Map<String, OVFNetworkTO> nets = new HashMap<>();
        for (int i = 0; i < networkElements.getLength(); i++) {

            Element networkElement = (Element)networkElements.item(i);
            String networkName = ovfParser.getNodeAttribute(networkElement, "name");

            String description = ovfParser.getChildNodeValue(networkElement, "Description");

            OVFNetworkTO network = new OVFNetworkTO();
            network.setName(networkName);
            network.setNetworkDescription(description);

            nets.put(networkName,network);
        }
        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("found %d networks in template", nets.size()));
        }
        return nets;
    }

    private boolean hardwareItemContainsConfiguration(OVFVirtualHardwareItemTO item, String configurationId) {
        if (StringUtils.isAnyBlank(configurationId, item.getConfigurationIds())) {
            return true;
        }
        String configurationIds = item.getConfigurationIds();
        if (StringUtils.isNotBlank(configurationIds)) {
            String[] configurations = configurationIds.split(" ");
            List<String> confList = Arrays.asList(configurations);
            return confList.contains(configurationId);
        }
        return false;
    }

    /**
     * Retrieve the virtual hardware section and its deployment options as configurations
     */
    public OVFVirtualHardwareSectionTO getVirtualHardwareSectionFromDocument(Document doc) {
        List<OVFConfigurationTO> configurations = getDeploymentOptionsFromDocumentTree(doc);
        List<OVFVirtualHardwareItemTO> items = getVirtualHardwareItemsFromDocumentTree(doc);
        if (CollectionUtils.isNotEmpty(configurations)) {
            for (OVFConfigurationTO configuration : configurations) {
                List<OVFVirtualHardwareItemTO> confItems = items.stream().
                        filter(x -> StringUtils.isNotBlank(x.getConfigurationIds())
                                && hardwareItemContainsConfiguration(x, configuration.getId()))
                        .collect(Collectors.toList());
                configuration.setHardwareItems(confItems);
            }
        }
        List<OVFVirtualHardwareItemTO> commonItems = null;
        if (CollectionUtils.isNotEmpty(items)) {
            commonItems = items.stream().filter(x -> StringUtils.isBlank(x.getConfigurationIds())).collect(Collectors.toList());
        }
        String minimumHardwareVersion = getMinimumHardwareVersionFromDocumentTree(doc);
        return new OVFVirtualHardwareSectionTO(configurations, commonItems, minimumHardwareVersion);
    }

    private String getMinimumHardwareVersionFromDocumentTree(Document doc) {
        String version = null;
        if (doc != null) {
            NodeList systemNodeList = ovfParser.getElementsFromOVFDocument(doc, "System");
            if (systemNodeList.getLength() != 0) {
                Node systemItem = systemNodeList.item(0);
                String hardwareVersions = ovfParser.getChildNodeValue(systemItem, "VirtualSystemType");
                if (StringUtils.isNotBlank(hardwareVersions)) {
                    String[] versions = hardwareVersions.split(",");
                    // Order the hardware versions and retrieve the minimum version
                    List<String> versionsList = Arrays.stream(versions).sorted().collect(Collectors.toList());
                    version = versionsList.get(0);
                }
            }
        }
        return version;
    }

    private List<OVFConfigurationTO> getDeploymentOptionsFromDocumentTree(Document doc) {
        List<OVFConfigurationTO> options = new ArrayList<>();
        if (doc == null) {
            return options;
        }
        NodeList deploymentOptionSection = ovfParser.getElementsFromOVFDocument(doc,"DeploymentOptionSection");
        if (deploymentOptionSection.getLength() == 0) {
            return options;
        }
        Node hardwareSectionNode = deploymentOptionSection.item(0);
        NodeList childNodes = hardwareSectionNode.getChildNodes();
        int index = 0;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node != null && (node.getNodeName().equals("Configuration") || node.getNodeName().equals("ovf:Configuration"))) {
                Element configuration = (Element) node;
                String configurationId = ovfParser.getNodeAttribute(configuration, "id");
                String description = ovfParser.getChildNodeValue(configuration, "Description");
                String label = ovfParser.getChildNodeValue(configuration, "Label");
                OVFConfigurationTO option = new OVFConfigurationTO(configurationId, label, description, index);
                options.add(option);
                index++;
            }
        }
        return options;
    }

    private List<OVFVirtualHardwareItemTO> getVirtualHardwareItemsFromDocumentTree(Document doc) {
        List<OVFVirtualHardwareItemTO> items = new LinkedList<>();
        if (doc == null) {
            return items;
        }
        NodeList hardwareSection = ovfParser.getElementsFromOVFDocument(doc, "VirtualHardwareSection");
        if (hardwareSection.getLength() == 0) {
            return items;
        }
        Node hardwareSectionNode = hardwareSection.item(0);
        NodeList childNodes = hardwareSectionNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node != null && (node.getNodeName().equals("Item") || node.getNodeName().equals("ovf:Item"))) {
                Element configuration = (Element) node;
                String configurationIds = ovfParser.getNodeAttribute(configuration, "configuration");
                String allocationUnits = ovfParser.getChildNodeValue(configuration, "AllocationUnits");
                String description = ovfParser.getChildNodeValue(configuration, "Description");
                String elementName = ovfParser.getChildNodeValue(configuration, "ElementName");
                String instanceID = ovfParser.getChildNodeValue(configuration, "InstanceID");
                String limit = ovfParser.getChildNodeValue(configuration, "Limit");
                String reservation = ovfParser.getChildNodeValue(configuration, "Reservation");
                String resourceType = ovfParser.getChildNodeValue(configuration, "ResourceType");
                String virtualQuantity = ovfParser.getChildNodeValue(configuration, "VirtualQuantity");
                String hostResource = ovfParser.getChildNodeValue(configuration, "HostResource");
                String addressOnParent = ovfParser.getChildNodeValue(configuration, "AddressOnParent");
                String parent = ovfParser.getChildNodeValue(configuration, "Parent");
                OVFVirtualHardwareItemTO item = new OVFVirtualHardwareItemTO();
                item.setConfigurationIds(configurationIds);
                item.setAllocationUnits(allocationUnits);
                item.setDescription(description);
                item.setElementName(elementName);
                item.setInstanceId(instanceID);
                item.setLimit(getLongValueFromString(limit));
                item.setReservation(getLongValueFromString(reservation));
                Integer resType = getIntValueFromString(resourceType);
                if (resType != null) {
                    item.setResourceType(OVFVirtualHardwareItemTO.getResourceTypeFromId(resType));
                }
                item.setVirtualQuantity(getLongValueFromString(virtualQuantity));
                item.setHostResource(hostResource);
                item.setAddressOnParent(addressOnParent);
                item.setParent(parent);
                items.add(item);
            }
        }
        return items;
    }

    private Long getLongValueFromString(String value) {
        if (StringUtils.isNotBlank(value)) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                s_logger.debug("Could not parse the value: " + value + ", ignoring it");
            }
        }
        return null;
    }

    private Integer getIntValueFromString(String value) {
        if (StringUtils.isNotBlank(value)) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                s_logger.debug("Could not parse the value: " + value + ", ignoring it");
            }
        }
        return null;
    }

    protected byte[] compressOVFEula(String license) throws IOException {
        CompressionUtil compressionUtil = new CompressionUtil();
        return compressionUtil.compressString(license);
    }

    public List<OVFEulaSectionTO> getEulaSectionsFromDocument(Document doc) {
        List<OVFEulaSectionTO> eulas = new LinkedList<>();
        if (doc == null) {
            return eulas;
        }
        NodeList eulaSections = ovfParser.getElementsFromOVFDocument(doc, "EulaSection");
        int eulaIndex = 0;
        if (eulaSections.getLength() > 0) {
            for (int index = 0; index < eulaSections.getLength(); index++) {
                Node eulaNode = eulaSections.item(index);
                NodeList eulaChildNodes = eulaNode.getChildNodes();
                String eulaInfo = null;
                String eulaLicense = null;
                for (int i = 0; i < eulaChildNodes.getLength(); i++) {
                    Node eulaItem = eulaChildNodes.item(i);
                    if (eulaItem.getNodeName().equalsIgnoreCase("Info")) {
                        eulaInfo = eulaItem.getTextContent();
                    } else if (eulaItem.getNodeName().equalsIgnoreCase("License")) {
                        eulaLicense = eulaItem.getTextContent();
                    }
                }
                byte[] compressedLicense = new byte[0];
                try {
                    compressedLicense = compressOVFEula(eulaLicense);
                } catch (IOException e) {
                    s_logger.error("Could not compress the license for info " + eulaInfo);
                    continue;
                }
                OVFEulaSectionTO eula = new OVFEulaSectionTO(eulaInfo, compressedLicense, eulaIndex);
                eulas.add(eula);
                eulaIndex++;
            }
        }

        return eulas;
    }

    public Pair<String, String> getOperatingSystemInfoFromDocument(Document doc) {
        if (doc == null) {
            return null;
        }
        NodeList guesOsList = ovfParser.getElementsFromOVFDocument(doc, "OperatingSystemSection");
        if (guesOsList.getLength() == 0) {
            return null;
        }
        Node guestOsNode = guesOsList.item(0);
        Element guestOsElement = (Element) guestOsNode;
        String osType = ovfParser.getNodeAttribute(guestOsElement, "osType");
        String description = ovfParser.getChildNodeValue(guestOsNode, "Description");
        return new Pair<>(osType, description);
    }

    class OVFFile {
        // <File ovf:href="i-2-8-VM-disk2.vmdk" ovf:id="file1" ovf:size="69120" />
        public String _href;
        public String _id;
        public Long _size;
        public boolean _bootable;
        public boolean isIso;
    }

    class OVFDisk {
        //<Disk ovf:capacity="50" ovf:capacityAllocationUnits="byte * 2^20" ovf:diskId="vmdisk2" ovf:fileRef="file2"
        //ovf:format="http://www.vmware.com/interfaces/specifications/vmdk.html#streamOptimized" ovf:populatedSize="43319296" />
        public Long _capacity;
        public String _diskId;
        public String _fileRef;
        public Long _populatedSize;
        public OVFDiskController _controller;
    }

    class OVFDiskController {
        public String _name;
        public String _subType;
    }
}
