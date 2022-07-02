package org.apache.cloudstack.oauth2;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.extensions.servlet.auth.oauth2.AbstractAuthorizationCodeServlet;
import com.google.api.client.http.GenericUrl;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends AbstractAuthorizationCodeServlet {

    @Override
    protected AuthorizationCodeFlow initializeFlow() throws IOException {
        return OAuth2Utils.newFlow();
    }

    @Override
    protected String getRedirectUri(HttpServletRequest httpServletRequest) {
        GenericUrl url = new GenericUrl(httpServletRequest.getRequestURL().toString());
        url.setRawPath("/login-callback");
        return url.build();
    }

    @Override
    protected String getUserId(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getSession().getId();
    }
}
