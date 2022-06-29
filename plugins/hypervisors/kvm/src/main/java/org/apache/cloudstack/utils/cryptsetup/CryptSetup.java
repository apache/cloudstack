package org.apache.cloudstack.utils.cryptsetup;

import com.cloud.utils.script.Script;

import java.io.IOException;

public class CryptSetup {
    protected String _commandPath = "cryptsetup";

    /**
     * LuksType represents the possible types that can be passed to cryptsetup.
     * NOTE: Only "luks1" is currently supported with Libvirt, so while
     * this utility may be capable of creating various types, care should
     * be taken to use types that work for the use case.
     */
    public enum LuksType {
        LUKS("luks1"), LUKS2("luks2"), PLAIN("plain"), TCRYPT("tcrypt"), BITLK("bitlk");

        String luksType;

        LuksType(String type) { this.luksType = type; }

        @Override
        public String toString() {
            return luksType;
        }
    }

    public CryptSetup(final String commandPath) {
        _commandPath = commandPath;
    }

    public CryptSetup() {}

    public void open(byte[] passphrase, LuksType luksType, String diskPath, String diskName) throws CryptSetupException {
        try(KeyFile key = new KeyFile(passphrase)) {
            final Script script = new Script(_commandPath);
            script.add("open");
            script.add("--key-file");
            script.add(key.toString());
            script.add("--allow-discards");
            script.add(diskPath);
            script.add(diskName);

            final String result = script.execute();
            if (result != null) {
                throw new CryptSetupException(result);
            }
        } catch (IOException ex) {
            throw new CryptSetupException(String.format("Failed to open encrypted device at '%s'", diskPath), ex);
        }
    }

    public void close(String diskName) throws CryptSetupException {
        final Script script = new Script(_commandPath);
        script.add("close");
        script.add(diskName);

        final String result = script.execute();
        if (result != null) {
            throw new CryptSetupException(result);
        }
    }

    /**
     * Formats a file using cryptsetup
     * @param passphrase
     * @param luksType
     * @param diskPath
     * @throws CryptSetupException
     */
    public void luksFormat(byte[] passphrase, LuksType luksType, String diskPath) throws CryptSetupException {
        try(KeyFile key = new KeyFile(passphrase)) {
            final Script script = new Script(_commandPath);
            script.add("luksFormat");
            script.add("-q");
            script.add("--force-password");
            script.add("--key-file");
            script.add(key.toString());
            script.add("--type");
            script.add(luksType.toString());
            script.add(diskPath);

            final String result = script.execute();
            if (result != null) {
                throw new CryptSetupException(result);
            }
        } catch (IOException ex) {
            throw new CryptSetupException(String.format("Failed to format encrypted device at '%s'", diskPath), ex);
        }
    }

    public boolean isSupported() {
        final Script script = new Script(_commandPath);
        script.add("--usage");
        final String result = script.execute();
        return result == null;
    }

    public boolean isLuks(String filePath) {
        final Script script = new Script(_commandPath);
        script.add("isLuks");
        script.add(filePath);

        final String result = script.execute();
        return result == null;
    }
}
