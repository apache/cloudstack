// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package com.cloud.server.auth;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.cloudstack.api.ApiConstants.LDAPParams;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value={UserAuthenticator.class})
public class LDAPUserAuthenticator extends DefaultUserAuthenticator {
    public static final Logger s_logger = Logger.getLogger(LDAPUserAuthenticator.class);

    @Inject private ConfigurationDao _configDao;
    @Inject private UserAccountDao _userAccountDao;

    @Override
    public boolean authenticate(String username, String password, Long domainId, Map<String, Object[]> requestParameters ) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Retrieving user: " + username);
        }
        UserAccount user = _userAccountDao.getUserAccount(username, domainId);
        if (user == null) {
            s_logger.debug("Unable to find user with " + username + " in domain " + domainId);
            return false;
        }

        String url = _configDao.getValue(LDAPParams.hostname.toString());
        if (url==null){
            s_logger.debug("LDAP authenticator is not configured.");
            return false;
        }
        String port = _configDao.getValue(LDAPParams.port.toString());
        String queryFilter = _configDao.getValue(LDAPParams.queryfilter.toString());
        String searchBase = _configDao.getValue(LDAPParams.searchbase.toString());
        String useSSL = _configDao.getValue(LDAPParams.usessl.toString());
        String bindDN = _configDao.getValue(LDAPParams.dn.toString());
        String bindPasswd = _configDao.getValue(LDAPParams.passwd.toString());
        String trustStore = _configDao.getValue(LDAPParams.truststore.toString());
        String trustStorePassword = _configDao.getValue(LDAPParams.truststorepass.toString());

        try {
            // get all params
            Hashtable<String, String> env = new Hashtable<String, String>(11);
            env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
            String protocol = "ldap://" ;
            if (new Boolean(useSSL)){
                env.put(Context.SECURITY_PROTOCOL, "ssl");
                protocol="ldaps://" ;
                System.setProperty("javax.net.ssl.trustStore", trustStore);
                System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            }
            env.put(Context.PROVIDER_URL, protocol + url  + ":" + port);

            if (bindDN != null && bindPasswd != null){
                env.put(Context.SECURITY_PRINCIPAL, bindDN);
                env.put(Context.SECURITY_CREDENTIALS, bindPasswd);
            }
            else {
                // Use anonymous authentication
                env.put(Context.SECURITY_AUTHENTICATION, "none");
            }
            // Create the initial context
            DirContext ctx = new InitialDirContext(env);
            // use this context to search

            // substitute the queryFilter with this user info
            queryFilter = queryFilter.replaceAll("\\%u", username);
            queryFilter = queryFilter.replaceAll("\\%n", user.getFirstname() + " " + user.getLastname());
            queryFilter = queryFilter.replaceAll("\\%e", user.getEmail());


            SearchControls sc = new SearchControls();
            String[] searchFilter = { "dn" };
            sc.setReturningAttributes(new String[0]); //return no attributes
            sc.setReturningAttributes(searchFilter);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setCountLimit(1);

            // Search for objects with those matching attributes
            NamingEnumeration<SearchResult> answer = ctx.search(searchBase, queryFilter,  sc);
            SearchResult sr = answer.next();
            String cn = sr.getName();
            answer.close();
            ctx.close();

            s_logger.info("DN from LDAP =" + cn);

            // check the password
            env = new Hashtable<String, String>(11);
            env.put(Context.INITIAL_CONTEXT_FACTORY,"com.sun.jndi.ldap.LdapCtxFactory");
            protocol = "ldap://" ;
            if (new Boolean(useSSL)){
                env.put(Context.SECURITY_PROTOCOL, "ssl");
                protocol="ldaps://" ;
            }
            env.put(Context.PROVIDER_URL, protocol + url  + ":" + port);
            env.put(Context.SECURITY_PRINCIPAL, cn + "," + searchBase);
            env.put(Context.SECURITY_CREDENTIALS, password);
            // Create the initial context
            ctx = new InitialDirContext(env);
            ctx.close();

        } catch (NamingException ne) {
            ne.printStackTrace();
            s_logger.warn("Authentication failed due to " + ne.getMessage());
            return false;
        }
        catch (Exception e){
            e.printStackTrace();
            s_logger.warn("Unknown error encountered " + e.getMessage());
            return false;
        }

        // authenticate
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        super.configure(name, params);
        return true;
    }

    @Override
    public String encode(String password) {
        // Password is not used, so set to a random string
        try {
            SecureRandom randomGen = SecureRandom.getInstance("SHA1PRNG");
            byte bytes[] = new byte[20];
            randomGen.nextBytes(bytes);
            return Base64.encode(bytes).toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("Failed to generate random password",e);
        }	
    }
}
