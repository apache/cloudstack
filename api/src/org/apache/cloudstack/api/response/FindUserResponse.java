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
package org.apache.cloudstack.api.response;

import java.util.Date;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.user.User;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = User.class)
public class FindUserResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the user ID")
    private String id ;

    @SerializedName("username") @Param(description="the user name")
    private String username;

    @SerializedName("password") @Param(description="the password of the user")
    private String password;
    
    @SerializedName("firstname") @Param(description="the user firstname")
    private String firstname;

    @SerializedName("lastname") @Param(description="the user lastname")
    private String lastname;

    @SerializedName("accountId") @Param(description="the account ID of the user")
    private String accountId;
    
    @SerializedName("email") @Param(description="the user email address")
    private String email;

    @SerializedName("state") @Param(description="the user state")
    private String state;

    @SerializedName("apikey") @Param(description="the api key of the user")
    private String apiKey;

    @SerializedName("secretkey") @Param(description="the secret key of the user")
    private String secretKey;
 
    @SerializedName("created") @Param(description="the date and time the user account was created")
    private Date created;

    @SerializedName("timezone") @Param(description="the timezone user was created in")
    private String timezone;
    
    @SerializedName("registrationtoken") @Param(description="the registration token")
    private String registrationToken;
    
    @SerializedName("registered") @Param(description="registration flag")
    boolean registered;
    
    @SerializedName("regionId") @Param(description="source region id of the user")
    private int regionId;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRegistrationToken() {
		return registrationToken;
	}

	public void setRegistrationToken(String registrationToken) {
		this.registrationToken = registrationToken;
	}

	public boolean isRegistered() {
		return registered;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}

	public int getRegionId() {
		return regionId;
	}

	public void setRegionId(int regionId) {
		this.regionId = regionId;
	}
}
