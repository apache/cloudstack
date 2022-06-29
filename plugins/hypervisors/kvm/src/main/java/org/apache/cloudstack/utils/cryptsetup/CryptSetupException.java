package org.apache.cloudstack.utils.cryptsetup;

public class CryptSetupException extends Exception {
    public CryptSetupException(String message) {
        super(message);
    }

    public CryptSetupException(String message, Exception ex) { super(message, ex); }
}
