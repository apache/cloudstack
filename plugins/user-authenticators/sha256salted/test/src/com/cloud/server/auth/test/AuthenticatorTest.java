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
