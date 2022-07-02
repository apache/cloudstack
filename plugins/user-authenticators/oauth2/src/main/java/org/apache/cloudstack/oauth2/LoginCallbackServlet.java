package org.apache.cloudstack.oauth2;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.servlet.auth.oauth2.AbstractAuthorizationCodeCallbackServlet;
import com.google.api.client.http.GenericUrl;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/login-callback")

public class LoginCallbackServlet extends AbstractAuthorizationCodeCallbackServlet {
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

    @Override
    protected void onSuccess(HttpServletRequest request, HttpServletResponse httpServletResponse, Credential credential)
            throws IOException {
        httpServletResponse.sendRedirect("/client");
    }

    @Override
    protected void onError(HttpServletRequest request, HttpServletResponse httpServletResponse,
                           AuthorizationCodseResponseUrl errorResponse)
            throws IOException {
        httpServletResponse.getWriter().print("Error in authorization");
    }
}
