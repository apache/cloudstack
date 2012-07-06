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
package com.cloud.bridge.util;

import org.apache.log4j.Logger;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;


public class AuthenticationUtils {
    protected final static Logger logger = Logger.getLogger(AuthenticationUtils.class);

	public AuthenticationUtils() {
	}

	/**
	 * The combination of the Issuer and the serial number of a X509 certificate
	 * must be globally unique.  The Issuer can be described by its Distinguished Name (DN).  
	 * The uniqueId is constructed by appending a ", serial=" onto the end of the Issuer's 
	 * DN (thus keeping the DN format).
	 * 
	 * @param cert
	 */
	public static String X509CertUniqueId( Certificate cert ) {
		X509Certificate userCert = (X509Certificate)cert;
        X500Principal issuer = userCert.getIssuerX500Principal();
        BigInteger serialNumber = userCert.getSerialNumber();
        return new String( issuer.toString() + ", serial=" + serialNumber );
	}
}
