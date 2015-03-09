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

package com.cloud.utils.cisco.n1kv.vsm;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class VsmResponse {

    // Following error tags, error types and severity have been taken from RFC 4741.
    public enum ErrorTag {
        InUse, // in-use
        InvalidValue, // invalid-value
        TooBig, // too-big
        MissingAttribute, // missing-attribute
        BadAttribute, // bad-attribute
        UnknownAttribute, // unknown-attribute
        MissingElement, // missing-element
        BadElement, // bad-element
        UnknownElement, // unknown-element
        UnknownNamespace, // unknown-namespace
        AccessDenied, // access-denied
        LockDenied, // lock-denied
        ResourceDenied, // resource-denied
        RollbackFailed, // rollback-failed
        DataExists, // data-exists
        DataMissing, // data-missing
        OperationNotSupported, // operation-not-supported
        OperationFailed, // operation-failed
        PartialOperation, // partial-operation
    }

    public enum ErrorType {
        transport, rpc, protocol, application;
    }

    public enum ErrorSeverity {
        error, warning;
    }

    private static final Logger s_logger = Logger.getLogger(VsmResponse.class);

    protected String _xmlResponse;
    protected Document _docResponse;
    protected boolean _responseOk;

    protected ErrorTag _tag;
    protected ErrorType _type;
    protected ErrorSeverity _severity;
    protected String _path;
    protected String _message;
    protected String _info;

    VsmResponse(String response) {
        _xmlResponse = response;
        _responseOk = false;
        _tag = ErrorTag.InUse;
        _type = ErrorType.rpc;
        _severity = ErrorSeverity.error;
        _docResponse = null;
    }

    protected void initialize() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            _docResponse = docBuilder.parse(new InputSource(new StringReader(_xmlResponse)));
            if (_docResponse != null) {
                parse(_docResponse.getDocumentElement());
            }
        } catch (ParserConfigurationException e) {
            s_logger.error("Error parsing the response : " + e.toString());
        } catch (SAXException e) {
            s_logger.error("Error parsing the response : " + e.toString());
        } catch (IOException e) {
            s_logger.error("Error parsing the response : " + e.toString());
        }
    }

    public boolean isResponseOk() {
        return _responseOk;
    }

    @Override
    public String toString() {
        StringBuffer error = new StringBuffer("");

        error.append(" Severity: " + _severity).append(", Error code: " + _tag).append(", Error type: " + _type);

        if (_message != null) {
            error.append(", Error Message: " + _message);
        }

        if (_info != null) {
            error.append(", Error info: " + _info);
        }

        if (_path != null) {
            error.append(", Path: " + _path);
        }

        return error.toString();
    }

    protected abstract void parse(Element root);

    protected void parseError(Node element) {
        Element rpcError = (Element)element;

        try {
            assert (rpcError.getNodeName().equalsIgnoreCase("nf:rpc-error"));
            for (Node node = rpcError.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeName().equalsIgnoreCase("nf:error-type")) {
                    _type = ErrorType.valueOf(node.getTextContent().trim());
                } else if (node.getNodeName().equalsIgnoreCase("nf:error-tag")) {
                    _tag = getErrorTag(node.getTextContent().trim());
                } else if (node.getNodeName().equalsIgnoreCase("nf:error-severity")) {
                    _severity = ErrorSeverity.valueOf(node.getTextContent().trim());
                } else if (node.getNodeName().equalsIgnoreCase("nf:error-path")) {
                    _path = node.getTextContent();
                } else if (node.getNodeName().equalsIgnoreCase("nf:error-message")) {
                    _message = node.getTextContent();
                } else if (node.getNodeName().equalsIgnoreCase("nf:error-info")) {
                    _info = node.getTextContent();
                }
            }
        } catch (DOMException e) {
            s_logger.error("Error parsing the response : " + e.toString());
        }
    }

    protected ErrorTag getErrorTag(String tagText) {
        ErrorTag tag = ErrorTag.InUse;

        if (tagText.equals("in-use")) {
            tag = ErrorTag.InUse;
        } else if (tagText.equals("invalid-value")) {
            tag = ErrorTag.InvalidValue;
        } else if (tagText.equals("too-big")) {
            tag = ErrorTag.TooBig;
        } else if (tagText.equals("missing-attribute")) {
            tag = ErrorTag.MissingAttribute;
        } else if (tagText.equals("bad-attribute")) {
            tag = ErrorTag.BadAttribute;
        } else if (tagText.equals("unknown-attribute")) {
            tag = ErrorTag.UnknownAttribute;
        } else if (tagText.equals("missing-element")) {
            tag = ErrorTag.MissingElement;
        } else if (tagText.equals("bad-element")) {
            tag = ErrorTag.BadElement;
        } else if (tagText.equals("unknown-element")) {
            tag = ErrorTag.UnknownElement;
        } else if (tagText.equals("unknown-namespace")) {
            tag = ErrorTag.UnknownNamespace;
        } else if (tagText.equals("access-denied")) {
            tag = ErrorTag.AccessDenied;
        } else if (tagText.equals("lock-denied")) {
            tag = ErrorTag.LockDenied;
        } else if (tagText.equals("resource-denied")) {
            tag = ErrorTag.ResourceDenied;
        } else if (tagText.equals("rollback-failed")) {
            tag = ErrorTag.RollbackFailed;
        } else if (tagText.equals("data-exists")) {
            tag = ErrorTag.DataExists;
        } else if (tagText.equals("data-missing")) {
            tag = ErrorTag.DataMissing;
        } else if (tagText.equals("operation-not-supported")) {
            tag = ErrorTag.OperationNotSupported;
        } else if (tagText.equals("operation-failed")) {
            tag = ErrorTag.OperationFailed;
        } else if (tagText.equals("partial-operation")) {
            tag = ErrorTag.PartialOperation;
        }

        return tag;
    }

    // Helper routine to check for the response received.
    protected void printResponse() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            DOMImplementationLS ls = (DOMImplementationLS)docBuilder.getDOMImplementation();
            LSSerializer lss = ls.createLSSerializer();
            System.out.println(lss.writeToString(_docResponse));
        } catch (ParserConfigurationException e) {
            s_logger.error("Error parsing the repsonse : " + e.toString());
        }
    }
}
