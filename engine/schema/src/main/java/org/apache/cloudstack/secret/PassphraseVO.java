/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.secret;

import com.cloud.utils.db.Encrypt;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Entity
@Table(name = "passphrase")
public class PassphraseVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "passphrase")
    @Encrypt
    private String passphrase;

    public PassphraseVO() {
    }

    public PassphraseVO(boolean initialize) {
        if (initialize) {
            try {
                SecureRandom random = SecureRandom.getInstanceStrong();
                byte[] temporary = new byte[48]; // 48 byte random passphrase buffer
                random.nextBytes(temporary);
                this.passphrase = Base64.getEncoder().encodeToString(temporary);
                Arrays.fill(temporary, (byte) 0); // clear passphrase from buffer
            } catch (NoSuchAlgorithmException ex ) {
                throw new CloudRuntimeException("Volume encryption requested but system is missing specified algorithm to generate passphrase");
            }
        }
    }

    public PassphraseVO(PassphraseVO existing) {
        this.passphrase = existing.getPassphraseString();
    }

    public byte[] getPassphrase() {
        if (StringUtils.isBlank(this.passphrase)) {
            return new byte[]{};
        }
        return this.passphrase.getBytes();
    }

    public String getPassphraseString() {
        return this.passphrase;
    }

    public Long getId() { return this.id; }
}
