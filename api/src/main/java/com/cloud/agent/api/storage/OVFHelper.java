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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.exception.InternalErrorException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.cloud.agent.api.to.DatadiskTO;
import com.cloud.utils.exception.CloudRuntimeException;

public class OVFHelper {
    private static final Logger s_logger = Logger.getLogger(OVFHelper.class);

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
     * Get the text value of a node's child with name "childNodeName", null if not present
     * Example:
     * <Node>
     *    <childNodeName>Text value</childNodeName>
     * </Node>
     */
    private String getChildNodeValue(Node node, String childNodeName) {
        if (node != null && node.hasChildNodes()) {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node value = childNodes.item(i);
                if (value != null && value.getNodeName().equals(childNodeName)) {
                    return value.getTextContent();
                }
            }
        }
        return null;
    }

    /**
     * Create OVFProperty class from the parsed node. Note that some fields may not be present.
     * The key attribute is required
     */
    protected OVFPropertyTO createOVFPropertyFromNode(Node node) {
        Element property = (Element) node;
        String key = property.getAttribute("ovf:key");
        if (StringUtils.isBlank(key)) {
            return null;
        }

        String value = property.getAttribute("ovf:value");
        String type = property.getAttribute("ovf:type");
        String qualifiers = property.getAttribute("ovf:qualifiers");
        String userConfigurableStr = property.getAttribute("ovf:userConfigurable");
        boolean userConfigurable = StringUtils.isNotBlank(userConfigurableStr) &&
                userConfigurableStr.equalsIgnoreCase("true");
        String passStr = property.getAttribute("ovf:password");
        boolean password = StringUtils.isNotBlank(passStr) && passStr.equalsIgnoreCase("true");
        String label = getChildNodeValue(node, "Label");
        String description = getChildNodeValue(node, "Description");
        return new OVFPropertyTO(key, type, value, qualifiers, userConfigurable, label, description, password);
    }

    /**
     * Retrieve OVF properties from a parsed OVF file, with attribute 'ovf:userConfigurable' set to true
     */
    private List<OVFPropertyTO> getConfigurableOVFPropertiesFromDocument(Document doc) {
        List<OVFPropertyTO> props = new ArrayList<>();
        NodeList properties = doc.getElementsByTagName("Property");
        if (properties != null) {
            for (int i = 0; i < properties.getLength(); i++) {
                Node node = properties.item(i);
                if (node == null) {
                    continue;
                }
                OVFPropertyTO prop = createOVFPropertyFromNode(node);
                if (prop != null && prop.isUserConfigurable()) {
                    props.add(prop);
                }
            }
        }
        return props;
    }

    /**
     * Get properties from OVF file located on ovfFilePath
     */
    public List<OVFPropertyTO> getOVFPropertiesFromFile(String ovfFilePath) throws ParserConfigurationException, IOException, SAXException {
        if (StringUtils.isBlank(ovfFilePath)) {
            return new ArrayList<>();
        }
        File ovfFile = new File(ovfFilePath);
        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ovfFile);
        return getConfigurableOVFPropertiesFromDocument(doc);
    }

    /**
     * Get properties from OVF XML string
     */
    protected List<OVFPropertyTO> getOVFPropertiesXmlString(final String ovfFilePath) throws ParserConfigurationException, IOException, SAXException {
        InputSource is = new InputSource(new StringReader(ovfFilePath));
        final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        return getConfigurableOVFPropertiesFromDocument(doc);
    }

    public List<DatadiskTO> getOVFVolumeInfo(final String ovfFilePath) {
        if (StringUtils.isBlank(ovfFilePath)) {
            return new ArrayList<DatadiskTO>();
        }
        ArrayList<OVFFile> vf = new ArrayList<OVFFile>();
        ArrayList<OVFDisk> vd = new ArrayList<OVFDisk>();

        File ovfFile = new File(ovfFilePath);
        try {
            final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(ovfFilePath));
            NodeList disks = doc.getElementsByTagName("Disk");
            NodeList files = doc.getElementsByTagName("File");
            NodeList items = doc.getElementsByTagName("Item");
            boolean toggle = true;
            for (int j = 0; j < files.getLength(); j++) {
                Element file = (Element)files.item(j);
                OVFFile of = new OVFFile();
                of._href = file.getAttribute("ovf:href");
                if (of._href.endsWith("vmdk") || of._href.endsWith("iso")) {
                    of._id = file.getAttribute("ovf:id");
                    String size = file.getAttribute("ovf:size");
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
            for (int i = 0; i < disks.getLength(); i++) {
                Element disk = (Element)disks.item(i);
                OVFDisk od = new OVFDisk();
                String virtualSize = disk.getAttribute("ovf:capacity");
                od._capacity = NumberUtils.toLong(virtualSize, 0L);
                String allocationUnits = disk.getAttribute("ovf:capacityAllocationUnits");
                od._diskId = disk.getAttribute("ovf:diskId");
                od._fileRef = disk.getAttribute("ovf:fileRef");
                od._populatedSize = NumberUtils.toLong(disk.getAttribute("ovf:populatedSize"));

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

        } catch (SAXException | IOException | ParserConfigurationException e) {
            s_logger.error("Unexpected exception caught while parsing ovf file:" + ovfFilePath, e);
            throw new CloudRuntimeException(e);
        }

        List<DatadiskTO> disksTO = new ArrayList<DatadiskTO>();
        for (OVFFile of : vf) {
            if (StringUtils.isBlank(of._id)){
                s_logger.error("The ovf file info is incomplete file info");
                throw new CloudRuntimeException("The ovf file info has incomplete file info");
            }
            OVFDisk cdisk = getDisk(of._id, vd);
            if (cdisk == null && !of.isIso){
                s_logger.error("The ovf file info has incomplete disk info");
                throw new CloudRuntimeException("The ovf file info has incomplete disk info");
            }
            Long capacity = cdisk == null ? of._size : cdisk._capacity;
            String controller = "";
            String controllerSubType = "";
            if (cdisk != null) {
                OVFDiskController cDiskController = cdisk._controller;
                controller = cDiskController == null ? "" : cdisk._controller._name;
                controllerSubType = cDiskController == null ? "" : cdisk._controller._subType;
            }

            String dataDiskPath = ovfFile.getParent() + File.separator + of._href;
            File f = new File(dataDiskPath);
            if (!f.exists() || f.isDirectory()) {
                s_logger.error("One of the attached disk or iso does not exists " + dataDiskPath);
                throw new CloudRuntimeException("One of the attached disk or iso as stated on OVF does not exists " + dataDiskPath);
            }
            disksTO.add(new DatadiskTO(dataDiskPath, capacity, of._size, of._id, of.isIso, of._bootable, controller, controllerSubType));
        }
        //check if first disk is an iso move it to the end
        DatadiskTO fd = disksTO.get(0);
        if (fd.isIso()) {
            disksTO.remove(0);
            disksTO.add(fd);
        }
        return disksTO;
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

    public void rewriteOVFFile(final String origOvfFilePath, final String newOvfFilePath, final String diskName) {
        try {
            final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(origOvfFilePath));
            NodeList disks = doc.getElementsByTagName("Disk");
            NodeList files = doc.getElementsByTagName("File");
            NodeList items = doc.getElementsByTagName("Item");
            String keepfile = null;
            List<Element> toremove = new ArrayList<Element>();
            for (int j = 0; j < files.getLength(); j++) {
                Element file = (Element)files.item(j);
                String href = file.getAttribute("ovf:href");
                if (diskName.equals(href)) {
                    keepfile = file.getAttribute("ovf:id");
                } else {
                    toremove.add(file);
                }
            }
            String keepdisk = null;
            for (int i = 0; i < disks.getLength(); i++) {
                Element disk = (Element)disks.item(i);
                String fileRef = disk.getAttribute("ovf:fileRef");
                if (keepfile == null) {
                    s_logger.info("FATAL: OVA format error");
                } else if (keepfile.equals(fileRef)) {
                    keepdisk = disk.getAttribute("ovf:diskId");
                } else {
                    toremove.add(disk);
                }
            }
            for (int k = 0; k < items.getLength(); k++) {
                Element item = (Element)items.item(k);
                NodeList cn = item.getChildNodes();
                for (int l = 0; l < cn.getLength(); l++) {
                    if (cn.item(l) instanceof Element) {
                        Element el = (Element)cn.item(l);
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

            final StringWriter writer = new StringWriter();
            final StreamResult result = new StreamResult(writer);
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();
            final DOMSource domSource = new DOMSource(doc);
            transformer.transform(domSource, result);
            PrintWriter outfile = new PrintWriter(newOvfFilePath);
            outfile.write(writer.toString());
            outfile.close();
        } catch (SAXException | IOException | ParserConfigurationException | TransformerException e) {
            s_logger.info("Unexpected exception caught while removing network elements from OVF:" + e.getMessage(), e);
            throw new CloudRuntimeException(e);
        }
    }

    OVFDisk getDisk(String fileRef, List<OVFDisk> disks) {
        for (OVFDisk disk : disks) {
            if (disk._fileRef.equals(fileRef)) {
                return disk;
            }
        }
        return null;
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
        public String _capacityUnit;
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
