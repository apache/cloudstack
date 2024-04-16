package org.apache.cloudstack.gui.theme.dao;

import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.gui.themes.GuiThemeVO;

import java.util.List;

public interface GuiThemeDao extends GenericDao<GuiThemeVO, Long> {

    GuiThemeVO findDefaultTheme();

    Pair<List<GuiThemeVO>, Integer> listGuiThemes(Long id, String name, String commonName, String domainId, String accountId, boolean listAll, boolean showRemoved, Boolean showPublic);

    Pair<List<GuiThemeVO>, Integer> listGuiThemesWithNoAuthentication(String commonName);
}
