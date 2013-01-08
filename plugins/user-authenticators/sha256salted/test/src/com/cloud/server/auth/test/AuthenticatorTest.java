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

package src.com.cloud.server.auth.test;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

import javax.naming.ConfigurationException;

import org.bouncycastle.util.encoders.Base64;
import org.junit.Before;
import org.junit.Test;

import com.cloud.server.auth.SHA256SaltedUserAuthenticator;

public class AuthenticatorTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testEncode() throws UnsupportedEncodingException, NoSuchAlgorithmException {
		SHA256SaltedUserAuthenticator authenticator = 
				new SHA256SaltedUserAuthenticator();

		try {
			authenticator.configure("SHA256", Collections.<String,Object>emptyMap());
		} catch (ConfigurationException e) {
			fail(e.toString());
		}
		
		String encodedPassword = authenticator.encode("password");
        
		String storedPassword[] = encodedPassword.split(":");
        assertEquals ("hash must consist of two components", storedPassword.length, 2);

        byte salt[] = Base64.decode(storedPassword[0]);
        String hashedPassword = authenticator.encode("password", salt);
        
        assertEquals("compare hashes", storedPassword[1], hashedPassword);

	}

}
