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

import common.asn1.Any;
import common.asn1.ObjectID;
import common.asn1.Sequence;
import common.asn1.Tag;

/**
 * X509 SubjectPublicKeyInfo field ASN.1 description.
 */
public class AlgorithmIdentifier extends Sequence {
    public ObjectID algorithm = new ObjectID("algorithm");
    public Any parameters = new Any("parameters") {
        {
            optional = true;
        }
    };

    public AlgorithmIdentifier(String name) {
        super(name);
        tags = new Tag[] {algorithm, parameters};
    }

}
