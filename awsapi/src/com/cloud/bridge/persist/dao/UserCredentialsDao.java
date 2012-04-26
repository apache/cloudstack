/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.persist.dao;

import org.apache.log4j.Logger;

import com.cloud.bridge.model.UserCredentials;
import com.cloud.bridge.persist.EntityDao;
import com.cloud.bridge.service.exception.NoSuchObjectException;




public class UserCredentialsDao extends EntityDao<UserCredentials>{
	public static final Logger logger = Logger.getLogger(UserCredentialsDao.class);

	public UserCredentialsDao() {
	    super(UserCredentials.class);
	}
	
	public void setUserKeys( String cloudAccessKey, String cloudSecretKey ){
		UserCredentials user = getByAccessKey( cloudAccessKey );
        if ( null == user ) {
          // -> do an insert since the user does not exist yet
          user = new UserCredentials();
          user.setAccessKey(cloudAccessKey);
          user.setSecretKey(cloudSecretKey);
          save(user);
        }
        else {
          // -> do an update since the user exists
          user.setAccessKey(cloudAccessKey);
          user.setSecretKey(cloudSecretKey);
          update(user);
        }
	}

	public void setCertificateId( String cloudAccessKey, String certId ){ 
	    UserCredentials user = getByAccessKey( cloudAccessKey );
	    if (null == user) throw new NoSuchObjectException( "Cloud API Access Key [" + cloudAccessKey + "] is unknown" );
	    user.setCertUniqueId(certId);
	    update(user);      
    }

    public UserCredentials getByAccessKey( String cloudAccessKey ){
        return queryEntity("FROM UserCredentials WHERE AccessKey=?", new Object[] {cloudAccessKey});
    }

    public UserCredentials getByCertUniqueId( String certId ){
        return queryEntity("FROM UserCredentials WHERE CertUniqueId=?", new Object[] {certId});
    }
}
