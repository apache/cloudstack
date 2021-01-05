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

package org.apache.cloudstack.saml;
/*import com.cloud.utils.db.DB;*/
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class SAMLActiveUser implements HttpSessionBindingListener {
    private String id;
    private SAMLTokenVO token;

    private static final Logger s_logger = Logger.getLogger(SAMLActiveUser.class);
    private SAMLTokenDao samlTokenDao;

    public SAMLActiveUser (SAMLTokenDao samlTokenDao){
        this.samlTokenDao = samlTokenDao;
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event) {
        final HttpSession session = event.getSession();
        if (session != null) {
            id = session.getId();
            token = (SAMLTokenVO)session.getAttribute(SAMLPluginConstants.SAML_TOKEN);
            if (token == null) {
                s_logger.error("token not found in session attributes");
            } else {
                s_logger.debug("Bound listener to active SAML user with token ID of " + token.getId() + " and UUID of " + token.getUuid());
            }
        } else {
            s_logger.error("event.getSession() returned null");
        }
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        if (token != null) {
            s_logger.debug("Removing token with ID " + token.getId());
            samlTokenDao.remove(token.getId());
        }
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(this.getClass().isInstance(obj))) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        final SAMLActiveUser other = (SAMLActiveUser) obj;
        return new EqualsBuilder().append(this.getId(), other.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31)
                .append(this.getClass())
                .append(id)
                .toHashCode();
    }
}
