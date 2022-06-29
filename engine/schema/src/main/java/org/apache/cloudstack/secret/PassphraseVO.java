package org.apache.cloudstack.secret;

import com.cloud.utils.db.Encrypt;
import com.cloud.utils.exception.CloudRuntimeException;

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
    private byte[] passphrase;

    public PassphraseVO() {
        try {
            SecureRandom random = SecureRandom.getInstanceStrong();
            byte[] temporary = new byte[48]; // 48 byte random passphrase buffer
            this.passphrase = new byte[64]; // 48 byte random passphrase as base64 for usability
            random.nextBytes(temporary);
            Base64.getEncoder().encode(temporary, this.passphrase);
            Arrays.fill(temporary, (byte) 0); // clear passphrase from buffer
        } catch (NoSuchAlgorithmException ex ) {
            throw new CloudRuntimeException("Volume encryption requested but system is missing specified algorithm to generate passphrase");
        }
    }

    public PassphraseVO(PassphraseVO existing) {
        this.passphrase = existing.getPassphrase();
    }

    public void clearPassphrase() {
        if (this.passphrase != null) {
            Arrays.fill(this.passphrase, (byte) 0);
        }
    }

    public byte[] getPassphrase() { return this.passphrase; }

    public Long getId() { return this.id; }
}
