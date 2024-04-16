package org.apache.cloudstack.gui.themes;

import org.apache.cloudstack.api.command.user.gui.themes.CreateGuiThemeCmd;
import org.apache.cloudstack.api.command.user.gui.themes.ListGuiThemesCmd;
import org.apache.cloudstack.api.command.user.gui.themes.RemoveGuiThemeCmd;
import org.apache.cloudstack.api.command.user.gui.themes.UpdateGuiThemeCmd;
import org.apache.cloudstack.api.response.GuiThemeResponse;
import org.apache.cloudstack.api.response.ListResponse;

public interface GuiThemeService {

    ListResponse<GuiThemeResponse> listGuiThemes(ListGuiThemesCmd cmd);

    GuiThemeVO createGuiTheme(CreateGuiThemeCmd cmd);

    GuiThemeVO updateGuiTheme(UpdateGuiThemeCmd cmd);

    void removeGuiTheme(RemoveGuiThemeCmd cmd);
}
