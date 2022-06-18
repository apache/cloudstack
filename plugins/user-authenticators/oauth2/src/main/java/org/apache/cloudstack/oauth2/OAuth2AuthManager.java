package org.apache.cloudstack.oauth2;

import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.Collection;

public interface OAuth2AuthManager extends PluggableAPIAuthenticator, PluggableService {
    public static final ConfigKey<Boolean> OAuth2IsPluginEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class, "oauth2.enabled", "false",
            "Indicates whether OAuth SSO plugin is enabled or not", true);
}
