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
 * CredSSP/SPNEGO/NTLMSSP implementation.
 *
 * CredSSP ASN.1 definition:
 *
 <pre>
CredSSP DEFINITIONS EXPLICIT TAGS ::=

BEGIN

TSPasswordCreds ::= SEQUENCE {
  domainName    [0] OCTET STRING,
  userName      [1] OCTET STRING,
  password      [2] OCTET STRING
}

TSCspDataDetail ::= SEQUENCE {
  keySpec       [0] INTEGER,
  cardName      [1] OCTET STRING OPTIONAL,
  readerName    [2] OCTET STRING OPTIONAL,
  containerName [3] OCTET STRING OPTIONAL,
  cspName       [4] OCTET STRING OPTIONAL
}

TSSmartCardCreds ::= SEQUENCE {
  pin           [0] OCTET STRING,
  cspData       [1] TSCspDataDetail,
  userHint      [2] OCTET STRING OPTIONAL,
  domainHint    [3] OCTET STRING OPTIONAL
}

TSCredentials ::= SEQUENCE {
  credType      [0] INTEGER,
  credentials   [1] OCTET STRING
}

NegoData ::= SEQUENCE OF SEQUENCE {
  negoToken     [0] OCTET STRING
}

TSRequest ::= SEQUENCE {
  version       [0] INTEGER,
  negoTokens    [1] NegoData OPTIONAL,
  authInfo      [2] OCTET STRING OPTIONAL,
  pubKeyAuth    [3] OCTET STRING OPTIONAL
}

END
</pre>

For packet flow, @see http://msdn.microsoft.com/en-us/library/cc226794.aspx
 */
package rdpclient.ntlmssp;
