package org.apache.cloudstack.framework.extensions.util;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YamlParser {
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public static ExtensionConfig parseYamlFile(String filePath) {
        ExtensionConfig extensionConfig = null;
        try (InputStream in = Files.newInputStream(Path.of(filePath))) {
            extensionConfig = mapper.readValue(in, ExtensionConfig.class);
        } catch (Exception ex) {
            throw new CloudRuntimeException("Failed to parse YAML", ex);
        }
        return extensionConfig;
    }
}
