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
 * TSCredentials ::= SEQUENCE {
 *   credType      [0] INTEGER,
 *   credentials   [1] OCTET STRING
 * }
 *
 * credType:
 *   1 - credentials contains a TSPasswordCreds structure that defines the user's password credentials.
 *   2 - credentials contains a TSSmartCardCreds structure that defines the user's smart card credentials.
 * </pre>
 */
public class TSCredentials extends Sequence {
    public Asn1Integer credType = new Asn1Integer("credType") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 0;
        }
    };
    public OctetString credentials = new OctetString("credentials") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 1;
        }
    };

    public TSCredentials(String name) {
        super(name);
        tags = new Tag[] {credType, credentials};
    }

    @Override
    public Tag deepCopy(String suffix) {
        return new TSCredentials(name + suffix).copyFrom(this);
    }

}
