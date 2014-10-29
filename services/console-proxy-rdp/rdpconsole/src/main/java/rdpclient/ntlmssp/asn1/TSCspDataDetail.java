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
package rdpclient.ntlmssp.asn1;

import common.asn1.Asn1Integer;
import common.asn1.OctetString;
import common.asn1.Sequence;
import common.asn1.Tag;

/**
 * <pre>
 * TSCspDataDetail ::= SEQUENCE {
 *   keySpec       [0] INTEGER,
 *   cardName      [1] OCTET STRING OPTIONAL,
 *   readerName    [2] OCTET STRING OPTIONAL,
 *   containerName [3] OCTET STRING OPTIONAL,
 *   cspName       [4] OCTET STRING OPTIONAL
 * }
 * </pre>
 * <ul>
 * <li>keySpec: Defines the specification of the user's smart card.
 *
 * <li>cardName: Specifies the name of the smart card.
 *
 * <li>readerName: Specifies the name of the smart card reader.
 *
 * <li>containerName: Specifies the name of the certificate container.
 *
 * <li>cspName: Specifies the name of the CSP.
 * </ul>
 * @see http://msdn.microsoft.com/en-us/library/cc226785.aspx
 */
public class TSCspDataDetail extends Sequence {
    public Asn1Integer keySpec = new Asn1Integer("keySpec") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 0;
        }
    };
    public OctetString cardName = new OctetString("cardName") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 1;
            optional = true;
        }
    };
    public OctetString readerName = new OctetString("readerName") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 2;
            optional = true;
        }
    };
    public OctetString containerName = new OctetString("containerName") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 3;
            optional = true;
        }
    };
    public OctetString cspName = new OctetString("cspName") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 4;
            optional = true;
        }
    };

    public TSCspDataDetail(String name) {
        super(name);
        tags = new Tag[] {keySpec, cardName, readerName, containerName, cspName};
    }

    @Override
    public Tag deepCopy(String suffix) {
        return new TSCspDataDetail(name + suffix).copyFrom(this);
    }

}
