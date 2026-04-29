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

import common.asn1.OctetString;
import common.asn1.Sequence;
import common.asn1.Tag;

/**
 * <pre>
 * TSSmartCardCreds ::= SEQUENCE {
 *   pin           [0] OCTET STRING,
 *   cspData       [1] TSCspDataDetail,
 *   userHint      [2] OCTET STRING OPTIONAL,
 *   domainHint    [3] OCTET STRING OPTIONAL
 * }
 * </pre>
 *
 * <ul>
 * <li>pin: Contains the user's smart card PIN.
 *
 * <li>cspData: A TSCspDataDetail structure that contains information about the
 * cryptographic service provider (CSP).
 *
 * <li>userHint: Contains the user's account hint.
 *
 * <li>domainHint: Contains the user's domain name to which the user's account
 * belongs. This name could be entered by the user when the user is first
 * prompted for the PIN.
 * </ul>
 *
 * @see http://msdn.microsoft.com/en-us/library/cc226784.aspx
 */
public class TSSmartCardCreds extends Sequence {
    public OctetString pin = new OctetString("pin") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 0;
        }
    };
    public TSCspDataDetail cspData = new TSCspDataDetail("cspData") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 1;
        }
    };
    public OctetString userHint = new OctetString("userHint") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 2;
            optional = true;
        }
    };
    public OctetString domainHint = new OctetString("domainHint") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 3;
            optional = true;
        }
    };

    public TSSmartCardCreds(String name) {
        super(name);
        tags = new Tag[] {pin, cspData, userHint, domainHint};
    }

    @Override
    public Tag deepCopy(String suffix) {
        return new TSSmartCardCreds(name + suffix).copyFrom(this);
    }

}
