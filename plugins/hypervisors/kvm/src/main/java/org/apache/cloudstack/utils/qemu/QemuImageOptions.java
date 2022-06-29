package org.apache.cloudstack.utils.qemu;

import com.google.common.base.Joiner;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class QemuImageOptions {
    private Map<String, String> params = new HashMap<>();
    private static final String FILENAME_PARAM_KEY = "file.filename";
    private static final String LUKS_KEY_SECRET_PARAM_KEY = "key-secret";
    private static final String QCOW2_KEY_SECRET_PARAM_KEY = "encrypt.key-secret";

    public QemuImageOptions(String filePath) {
        params.put(FILENAME_PARAM_KEY, filePath);
    }

    /**
     * Constructor for self-crafting the full map of parameters
     * @param params the map of parameters
     */
    public QemuImageOptions(Map<String, String> params) {
        this.params = params;
    }

    /**
     * Constructor for crafting image options that may contain a secret or format
     * @param format optional format, renders as "driver" option
     * @param filePath required path of image
     * @param secretName optional secret name for image. Secret only applies for QCOW2 or LUKS format
     */
    public QemuImageOptions(QemuImg.PhysicalDiskFormat format, String filePath, String secretName) {
        params.put(FILENAME_PARAM_KEY, filePath);
        if (secretName != null && !secretName.isBlank()) {
            switch (format) {
                case QCOW2:
                    params.put(QCOW2_KEY_SECRET_PARAM_KEY, secretName);
                    break;
                case LUKS:
                    params.put(LUKS_KEY_SECRET_PARAM_KEY, secretName);
                    break;
            }
        }
        if (format != null) {
            params.put("driver", format.toString());
        }
    }

    public void setFormat(QemuImg.PhysicalDiskFormat format) {
        if (format != null) {
            params.put("driver", format.toString());
        }
    }

    /**
     * Converts QemuObject into the command strings required by qemu-img flags
     * @return array of strings representing command flag and value (--object)
     */
    public String[] toCommandFlag() {
        Map<String, String> sorted = new TreeMap<>(params);
        String paramString = Joiner.on(",").withKeyValueSeparator("=").join(sorted);
        return new String[] {"--image-opts", paramString};
    }
}
