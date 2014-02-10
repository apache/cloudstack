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
/**
 *
 */
package com.cloud.bridge.io;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.databinding.ADBBean;
import org.apache.axis2.databinding.ADBException;
import org.apache.axis2.databinding.utils.writer.MTOMAwareXMLSerializer;

/**
 * Provide an MTOM aware serializable output stream writer to be consumed by implementors of the
 * com.amazon.s3 Response ADB bean classes.
 * This writer enables participation is StaX based builders and AXIOM om xml stream processing
 * An instance of a MTOMAwareResultStreamWriter is a convenient argument to a com.amazon.s3 Response bean, as generated
 * from the Amazon S3 WSDL using
 * wsdl2java.sh -ss -sd -ssi -g -p com.amazon.s3 -ns2p "http://s3.amazonaws.com/doc/2006-03-01/"=com.amazon.s3 -uri cloud-AmazonS3.wsdl
 * Such a bean implements a serialize method of the form
 *          public void serialize(qualifiedName,omfactory, xmlWriter)
 * where
 * @param qualifiedName is the XML qualified name of the parent
 * @param omfactory is an implementor of the AXIOM object model interface
 * @param xmlWriter is an implementor of XMLStxreamWriter for writing plain XML
 * A convenience constructor of MTOMAwareResultStreamWriter is of the form
 *          MTOMAwareResultStreamWriter(nameOfResult, outputStream)
 * where
 * @param nameOfResult is the name used for the root (parent) tag by the serialization bean
 * @param outputStream is the (servlet) output stream into which the bytes are written
 * Addtionally, as a side effect, ensure that the org.apache.axis2.databinding classes which serialize the
 * output of each fields have been initialized to be aware of any custom classes which override the default
 * output xsd converter methods of Axis2's databinding.  Such a custom class is notified to the ADB framework
 * (via its org.apache.axis2.databinding.utils.ConverterUtil class) by setting a System property,
 * SYSTEM_PROPERTY_ADB_CONVERTERUTIL to name the custom class.
 */
public class MTOMAwareResultStreamWriter {

    // Standard XML prolog to add to the beginning of each XML document.
    public static final String XMLPROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final byte[] XMLPROLOGBYTES = XMLPROLOG.getBytes();

    // The XML namespace used in documents transported to and from the service
    public static final String S3XMLNS = "http://s3.amazonaws.com/doc/2006-03-01/";
    // Prefix to use to represent the default XML Namespace, defined by the Namespaces in XML 3 spec to be ""
    public static final String DEFAULT_NS_PREFIX = XMLConstants.DEFAULT_NS_PREFIX;

    private XMLStreamWriter xmlWriter = null;

    private MTOMAwareXMLSerializer mtomWriter = null;

    // A default instance of AXIOM object model factory suitable for constructing plain XML
    private OMFactory omfactory = OMAbstractFactory.getOMFactory();

    // The qualified name for use in the XML schema as defined by http://www.w3.org/TR/xmlschema-2/#QName
    private QName qualifiedName = null;

    // Usually bound to a servlet output stream
    private OutputStream outputStream = null;

    // Set the system property to notify the ADB framework of its custom class for system-wide side effect
    // at time of initialization of this class (executed once in any JVM running this application)
    static {
        System.setProperty(org.apache.axis2.databinding.utils.ConverterUtil.SYSTEM_PROPERTY_ADB_CONVERTERUTIL, "com.cloud.bridge.util.DatabindingConverterUtil");
    }

    /*
     * @params
     * @param nameOfResult Used as the tag description of the result written out when the requester serializes
     * @param outputStream  The stream capable of sinking bytes written at the time the requester is ready to serialize,
     * assumed to be a ServletOutputStream
     * @param xmlOutputFactory  If passing a non-default factory, used to get an implementor of XmlStreamWriter
     * @throw XMLStreamException
     */
    public MTOMAwareResultStreamWriter(String nameOfResult, OutputStream outputStream, XMLOutputFactory xmlOutputFactory) throws XMLStreamException {
        this.outputStream = outputStream;
        // Create an implementor of xmlWriter for this instance
        xmlWriter = xmlOutputFactory.createXMLStreamWriter(outputStream);
        // Create an MTOM aware XML serializer for this instance
        // An MTOMAwareXMLSerializer wraps a xmlStreamWriter and implements writeDataHandler
        mtomWriter = new MTOMAwareXMLSerializer(xmlWriter);
        // Create a new qualified name passing in namespace URI (default), localpart, prefix (default)
        qualifiedName = new QName(S3XMLNS, nameOfResult, DEFAULT_NS_PREFIX);
    }

    /*
     * @params
     * @param nameOfResult Used as the tag description of the result written out when the requester serializes
     * @param outputStream  The stream capable of sinking bytes written at the time the requester is ready to serialize,
     * assumed to be a ServletOutputStream
     * Uses default implementor of XmlStreamWriter
     * @throw XMLStreamException
     */

    public MTOMAwareResultStreamWriter(String nameOfResult, OutputStream outputStream) throws XMLStreamException {
        this(nameOfResult, outputStream, XMLOutputFactory.newInstance());
    }

    // Housekeeping before consumption in a serialize call
    public void startWrite() throws IOException {
        outputStream.write(XMLPROLOGBYTES);

    }

    public void stopWrite() throws IOException, XMLStreamException {
        xmlWriter.flush();
        xmlWriter.close();
        outputStream.close();
    }

    // Cooperate with an instance of org.apache.axis2.databinding.ADBBean to provide serialization output of XML
    // An org.apache.axis2.databinding.ADBBean implements a serialize method which takes a QName and a XMLStreamWriter
    public void writeout(ADBBean dataBindingBean) throws ADBException, XMLStreamException {

        dataBindingBean.serialize(qualifiedName, omfactory, mtomWriter);
    }

}
