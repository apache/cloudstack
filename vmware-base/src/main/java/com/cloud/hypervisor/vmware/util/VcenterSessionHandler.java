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
package com.cloud.hypervisor.vmware.util;

import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;

import com.cloud.utils.exception.CloudRuntimeException;

public class VcenterSessionHandler implements SOAPHandler<SOAPMessageContext> {
    public static final Logger s_logger = Logger.getLogger(VcenterSessionHandler.class);
    private final String vcSessionCookie;

    public VcenterSessionHandler(String vcSessionCookie) {
        this.vcSessionCookie = vcSessionCookie;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        if (isOutgoingMessage(smc)) {
            try {
                SOAPHeader header = getSOAPHeader(smc);

                SOAPElement vcsessionHeader = header.addChildElement(new javax.xml.namespace.QName("#",
                        "vcSessionCookie"));
                vcsessionHeader.setValue(vcSessionCookie);

            } catch (DOMException e) {
                s_logger.debug(e);
                throw new CloudRuntimeException(e);
            } catch (SOAPException e) {
                s_logger.debug(e);
                throw new CloudRuntimeException(e);
            }
        }
        return true;
    }

    @Override
    public void close(MessageContext arg0) {
    }

    @Override
    public boolean handleFault(SOAPMessageContext arg0) {
        return false;
    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    SOAPHeader getSOAPHeader(SOAPMessageContext smc) throws SOAPException {
        return smc.getMessage().getSOAPPart().getEnvelope().getHeader() == null ? smc
                .getMessage().getSOAPPart().getEnvelope().addHeader()
                : smc.getMessage().getSOAPPart().getEnvelope().getHeader();
    }

    boolean isOutgoingMessage(SOAPMessageContext smc) {
        Boolean outboundProperty = (Boolean)smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        return outboundProperty;
    }

}
