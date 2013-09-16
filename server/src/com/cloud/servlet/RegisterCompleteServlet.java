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
package com.cloud.servlet;

import java.net.URLEncoder;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.SerialVersionUID;

@Component("registerCompleteServlet")
public class RegisterCompleteServlet extends HttpServlet {
    public static final Logger s_logger = Logger.getLogger(RegisterCompleteServlet.class.getName());

    static final long serialVersionUID = SerialVersionUID.CloudStartupServlet;

    @Inject AccountService _accountSvc;
    @Inject ConfigurationDao _configDao;
    @Inject UserDao _userDao;

    public RegisterCompleteServlet() {
    }
    
    @Override
    public void init(ServletConfig config) throws ServletException {
    	SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());       	
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String registrationToken = req.getParameter("token");
        String expires = req.getParameter("expires");
        int statusCode = HttpServletResponse.SC_OK;
        String responseMessage = null;

        if (registrationToken == null || registrationToken.trim().length() == 0) {
            statusCode = 503;
            responseMessage = "{ \"registration_info\" : { \"errorcode\" : \"503\", \"errortext\" : \"Missing token\" } }";
        } else {
            s_logger.info("Attempting to register user account with token = "+registrationToken);
            User resourceAdminUser = _accountSvc.getActiveUserByRegistrationToken(registrationToken);
            if (resourceAdminUser != null) {
                if(resourceAdminUser.isRegistered()) {
                    statusCode = 503;
                    responseMessage = "{ \"registration_info\" : { \"errorcode\" : \"503\", \"errortext\" : \"Expired token = " + registrationToken + "\" } }";
                } else {
                    if(expires != null && expires.toLowerCase().equals("true")){
                        _accountSvc.markUserRegistered(resourceAdminUser.getId());
                    }

                    Account resourceAdminAccount = _accountSvc.getActiveAccountById(resourceAdminUser.getAccountId());
                    Account rsUserAccount = _accountSvc.getActiveAccountByName(resourceAdminAccount.getAccountName()+"-user", resourceAdminAccount.getDomainId());

                    List<UserVO> users =  _userDao.listByAccount(rsUserAccount.getId());
                    User rsUser = users.get(0);

                    Configuration config = _configDao.findByName("endpointe.url");

                    StringBuffer sb = new StringBuffer();
                    sb.append("{ \"registration_info\" : { \"endpoint_url\" : \""+encodeParam(config.getValue())+"\", ");
                    sb.append("\"domain_id\" : \""+resourceAdminAccount.getDomainId()+"\", ");
                    sb.append("\"admin_account\" : \""+encodeParam(resourceAdminUser.getUsername())+"\", ");
                    sb.append("\"admin_account_api_key\" : \""+resourceAdminUser.getApiKey()+"\", ");
                    sb.append("\"admin_account_secret_key\" : \""+resourceAdminUser.getSecretKey()+"\", ");
                    sb.append("\"user_account\" : \""+encodeParam(rsUser.getUsername())+"\", ");
                    sb.append("\"user_account_api_key\" : \""+rsUser.getApiKey()+"\", ");
                    sb.append("\"user_account_secret_key\" : \""+rsUser.getSecretKey()+"\" ");
                    sb.append("} }");
                    responseMessage = sb.toString();
                }
            } else {
                statusCode = 503;
                responseMessage = "{ \"registration_info\" : { \"errorcode\" : \"503\", \"errortext\" : \"Invalid token = " + registrationToken + "\" } }";
            }
        }

        try {
            resp.setContentType("text/javascript; charset=UTF-8");
            resp.setStatus(statusCode);
            resp.getWriter().print(responseMessage);
        } catch (Exception ex) {
            s_logger.error("unknown exception writing register complete response", ex);
        }
    }

    private String encodeParam(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replaceAll("\\+", "%20");
        } catch (Exception e) {
            s_logger.warn("Unable to encode: " + value);
        }
        return value;
    }
}
