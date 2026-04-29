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

package com.cloud.hypervisor.ovm3.objects;

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.NullParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.parser.AtomicParser;
import org.apache.xmlrpc.serializer.NullSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ContentHandler;

public class RpcTypeFactory extends TypeFactoryImpl {

    public RpcTypeFactory(XmlRpcController pController) {
        super(pController);
    }

    @Override
    public TypeParser getParser(XmlRpcStreamConfig pConfig,
            NamespaceContextImpl pContext, String pURI, String pLocalName) {
        if ("".equals(pURI) && NullSerializer.NIL_TAG.equals(pLocalName)) {
            return new NullParser();
        } else if ("i8".equals(pLocalName)) {
            return new LongTypeParser();
        } else {
            return super.getParser(pConfig, pContext, pURI, pLocalName);
        }
    }

    public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig,
            Object pObject) throws SAXException {
        if (pObject instanceof Long) {
            return new LongTypeSerializer();
        } else {
            return super.getSerializer(pConfig, pObject);
        }
    }

    private class LongTypeSerializer extends TypeSerializerImpl {
        /*
         * Tag name of an i8 value.
         */
        public static final String I8_TAG = "i8";
        /*
         * Fully qualified name of an i8 value.
         */
        public static final String EX_I8_TAG = "i8";
        @Override
        public void write(ContentHandler pHandler, Object pObject)
                throws SAXException {
            write(pHandler, I8_TAG, EX_I8_TAG, pObject.toString());
        }
    }

    private class LongTypeParser extends AtomicParser {
        protected void setResult(String pResult) throws SAXException {
            try {
                super.setResult(Long.valueOf(pResult.trim()));
            } catch (NumberFormatException e) {
                throw new SAXParseException("Failed to parse long value: "
                        + pResult, getDocumentLocator());
            }
        }
    }
}
