<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<!-- Loading -->
<div style="display:none;">
    <div class="ui-widget-overlay">
    </div>
    <div class="rightpanel_mainloaderbox" >
       <div class="rightpanel_mainloader_animatedicon"></div>
       <p>Loading &hellip; </p>
    </div>    
</div>

<div class="main_title" id="right_panel_header">
   
    <div class="main_titleicon">
        <img src="images/title_globalsettingsicon.gif" alt=" Global_Settings" /></div>
    
    <h1>
        Global Settings
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on">
            <%=t.t("details")%></div>
    </div>
    <div id="tab_content_details">
        <div class="grid_actionpanel">
            <div class="grid_actionbox" id="action_link">
                <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                    <ul class="actionsdropdown_boxlist" id="action_list">
                        <li><%=t.t("no.available.actions")%></li>
                    </ul>
                </div>
            </div>
            <div class="grid_editbox" id="edit_button">
            </div>
            <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
                display: none;">
                <div class="gridheader_loader" id="Div1">
                </div>
                <p id="description">
                    Waiting &hellip;</p>
            </div>                 
        </div>
        <div class="grid_container">
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("name")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="name">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("value")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="value">
                    </div>                    
                    <input class="text" id="value_edit" style="width: 200px; display: none;" type="text" />
                    <div id="value_edit_errormsg" style="display:none"></div>                    
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("description")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="description">
                    </div>
                </div>
            </div>            
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("category")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="category">
                    </div>
                </div>
            </div>               
        </div>        
        <div class="grid_botactionpanel">
        	<div class="gridbot_buttons" id="save_button" style="display:none;">Save</div>
            <div class="gridbot_buttons" id="cancel_button" style="display:none;">Cancel</div>
        </div>  
    </div>
</div>

<div id="dialog_alert_restart_management_server" title="Alert" style="display:none">
    <p>
        <%=t.t("please.restart.your.management.server.for.your.new.settings.to.take.effect")%>        
    </p>
</div>
