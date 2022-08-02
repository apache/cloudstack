package org.apache.cloudstack.oauth2;

import com.cloud.utils.component.AdapterBase;
import org.apache.log4j.Logger;

import java.util.List;

public class OAuth2AuthManagerImpl extends AdapterBase implements OAuth2AuthManager {
    private static final Logger s_logger = Logger.getLogger(OAuth2AuthManagerImpl.class);

    @Override
    public List<Class<?>> getAuthCommands() {
        return null;
    }

    @Override
    public boolean start() {
        if (isSAMLPluginEnabled()) {
            s_logger.info("OAUTH auth plugin loaded");
            return setup();
        } else {
            s_logger.info("OAUTH auth plugin not enabled so not loading");
            return super.start();
        }
    }

    private boolean setup() {
        return true;
    }

    private boolean isSAMLPluginEnabled() {
        return OAuth2IsPluginEnabled.value();
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public List<Class<?>> getCommands() {
        return null;
    }
}
