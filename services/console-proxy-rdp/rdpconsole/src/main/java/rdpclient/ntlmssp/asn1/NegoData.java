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

import common.asn1.SequenceOf;
import common.asn1.Tag;

/**
 * The NegoData structure contains the SPNEGO messages, as specified in
 * [MS-SPNG] section 2.
 *
 * <pre>
 * NegoData ::= SEQUENCE OF SEQUENCE {
 *   negoToken     [0] OCTET STRING
 * }
 * </pre>
 *
 * If we write NegoItem as
 *
 * <pre>
 * NegoItem ::= SEQUENCE {
 *   negoToken     [0] OCTET STRING
 * }
 * </pre>
 *
 * then NegoData can be written as
 *
 * <pre>
 * NegoData ::= SEQUENCE OF NegoItem
 * </pre>
 *
 * <ul>
 * <li>negoToken: One or more SPNEGO tokens, as specified in [MS-SPNG].
 * </ul>
 *
 * @see http://msdn.microsoft.com/en-us/library/cc226781.aspx
 */
public class NegoData extends SequenceOf {

    public NegoData(String name) {
        super(name);
        type = new NegoItem("NegoItem");
    }

    @Override
    public Tag deepCopy(String suffix) {
        return new NegoData(name + suffix).copyFrom(this);
    }

}
