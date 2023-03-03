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
