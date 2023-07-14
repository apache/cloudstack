package org.apache.cloudstack.userdata;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.apache.cloudstack.api.BaseCmd;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserDataManagerImplTest {

    @Spy
    @InjectMocks
    private UserDataManagerImpl userDataManager;

    @Test
    public void testValidateBase64WithoutPadding() {
        // fo should be encoded in base64 either as Zm8 or Zm8=
        String encodedUserdata = "Zm8";
        String encodedUserdataWithPadding = "Zm8=";

        // Verify that we accept both but return the padded version
        assertEquals("validate return the value with padding", encodedUserdataWithPadding, userDataManager.validateUserData(encodedUserdata, BaseCmd.HTTPMethod.GET));
        assertEquals("validate return the value with padding", encodedUserdataWithPadding, userDataManager.validateUserData(encodedUserdataWithPadding, BaseCmd.HTTPMethod.GET));
    }

    @Test
    public void testValidateUrlEncodedBase64() {
        // fo should be encoded in base64 either as Zm8 or Zm8=
        String encodedUserdata = "Zm+8/w8=";
        String urlEncodedUserdata = java.net.URLEncoder.encode(encodedUserdata, StandardCharsets.UTF_8);

        // Verify that we accept both but return the padded version
        assertEquals("validate return the value with padding", encodedUserdata, userDataManager.validateUserData(encodedUserdata, BaseCmd.HTTPMethod.GET));
        assertEquals("validate return the value with padding", encodedUserdata, userDataManager.validateUserData(urlEncodedUserdata, BaseCmd.HTTPMethod.GET));
    }

}