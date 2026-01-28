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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.cloudstack.utils.security.ParserUtils;
import org.apache.commons.io.IOUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UnmanageInstanceAnswer;
import com.cloud.agent.api.UnmanageInstanceCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtKvmAgentHook;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  UnmanageInstanceCommand.class)
public final class LibvirtUnmanageInstanceCommandWrapper extends CommandWrapper<UnmanageInstanceCommand, Answer, LibvirtComputingResource> {


    @Override
    public Answer execute(final UnmanageInstanceCommand command, final LibvirtComputingResource libvirtComputingResource) {
        String instanceName = command.getInstanceName();
        VirtualMachineTO vmSpec = command.getVm();
        final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
        logger.debug("Attempting to unmanage KVM instance: {}", instanceName);
        Domain dom = null;
        Connect conn = null;
        String vmFinalSpecification;
        try {
            if (vmSpec == null) {
                conn = libvirtUtilitiesHelper.getConnectionByVmName(instanceName);
                dom = conn.domainLookupByName(instanceName);
                vmFinalSpecification = dom.getXMLDesc(1);
                if (command.isConfigDriveAttached()) {
                    vmFinalSpecification = cleanupConfigDrive(vmFinalSpecification, instanceName);
                }
            } else {
                // define domain using reconstructed vmSpec
                logger.debug("Unmanaging Stopped KVM instance: {}", instanceName);
                LibvirtVMDef vm = libvirtComputingResource.createVMFromSpec(vmSpec);
                libvirtComputingResource.createVbd(conn, vmSpec, instanceName, vm);
                conn = libvirtUtilitiesHelper.getConnectionByType(vm.getHvsType());
                String vmInitialSpecification = vm.toString();
                vmFinalSpecification = performXmlTransformHook(vmInitialSpecification, libvirtComputingResource);
            }
            conn.domainDefineXML(vmFinalSpecification).free();
            logger.debug("Successfully unmanaged KVM instance: {} with domain XML: {}", instanceName, vmFinalSpecification);
            return new UnmanageInstanceAnswer(command, true, "Successfully unmanaged");
        } catch (final LibvirtException e) {
            logger.error("LibvirtException occurred during unmanaging instance: {} ", instanceName, e);
            return new UnmanageInstanceAnswer(command, false, e.getMessage());
        } catch (final IOException
                       | ParserConfigurationException
                       | SAXException
                       | TransformerException
                       | XPathExpressionException
                       | InternalErrorException
                       | URISyntaxException e) {

            logger.error("Failed to unmanage Instance: {}.", instanceName, e);
            return new UnmanageInstanceAnswer(command, false, e.getMessage());
        } finally {
            if (dom != null) {
                try {
                    dom.free();
                } catch (LibvirtException e) {
                    logger.error("Ignore libvirt error on free.", e);
                }
            }
        }
    }

    String cleanupConfigDrive(String domainXML, String instanceName) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException {
        String isoName = "/" + instanceName + ".iso";
        DocumentBuilderFactory docFactory = ParserUtils.getSaferDocumentBuilderFactory();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document document;
        try (InputStream inputStream = IOUtils.toInputStream(domainXML, StandardCharsets.UTF_8)) {
            document = docBuilder.parse(inputStream);
        }
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        // Find all <disk device='cdrom'> elements with source file containing instanceName.iso
        String expression = String.format("//disk[@device='cdrom'][source/@file[contains(., '%s')]]", isoName);
        NodeList cdromDisks = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);

        // If nothing matched, return original XML
        if (cdromDisks == null || cdromDisks.getLength() == 0) {
            logger.debug("No config drive found in domain XML for Instance: {}", instanceName);
            return domainXML;
        }

        // Remove all matched config drive disks
        for (int i = 0; i < cdromDisks.getLength(); i++) {
            Node diskNode = cdromDisks.item(i);
            if (diskNode != null && diskNode.getParentNode() != null) {
                diskNode.getParentNode().removeChild(diskNode);
            }
        }
        logger.debug("Removed {} config drive ISO CD-ROM entries for instance: {}", cdromDisks.getLength(), instanceName);

        TransformerFactory transformerFactory = ParserUtils.getSaferTransformerFactory();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        StringWriter output = new StringWriter();
        StreamResult result = new StreamResult(output);
        transformer.transform(source, result);
        return output.toString();
    }

    private String performXmlTransformHook(String vmInitialSpecification, final LibvirtComputingResource libvirtComputingResource) {
        String vmFinalSpecification;
        try {
            // if transformer fails, everything must go as it's just skipped.
            LibvirtKvmAgentHook t = libvirtComputingResource.getTransformer();
            vmFinalSpecification = (String) t.handle(vmInitialSpecification);
            if (null == vmFinalSpecification) {
                logger.warn("Libvirt XML transformer returned NULL, will use XML specification unchanged.");
                vmFinalSpecification = vmInitialSpecification;
            }
        } catch(Exception e) {
            logger.warn("Exception occurred when handling LibVirt XML transformer hook: {}", e);
            vmFinalSpecification = vmInitialSpecification;
        }
        return vmFinalSpecification;
    }
}
