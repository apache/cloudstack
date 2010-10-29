<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>



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
    	<div class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div>
        <div class="grid_container">
        	<div class="grid_header">
            	<div class="grid_header_cell" style="width:35%; border:none;">
            		<div class="grid_header_title">Name</div>
                </div>
                <div class="grid_header_cell" style="width:23%; border:none;">
            		<div class="grid_header_title">Value</div>
                </div>
                <div class="grid_header_cell" style="width:27%; border:none;">
            		<div class="grid_header_title">Description</div>
                </div>                
                <div class="grid_header_cell" style="width:15%; border:none;">
            		<div id="action_link" class="grid_actionbox" id="account_action_link">
                        <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                            <ul class="actionsdropdown_boxlist" id="action_list">
                                <li><%=t.t("no.available.actions")%></li>
                                <li id="edit_button">Edit</a></li>
                            </ul>
                        </div>
                    </div>
                </div>                     
            </div>            
            <div id="grid_content">             	
         	</div>               
        </div>        
        <div class="grid_botactionpanel">
        	<div class="gridbot_buttons" id="save_button" style="display:none;">Save</div>
            <div class="gridbot_buttons" id="cancel_button" style="display:none;">Cancel</div>
        </div>  
    </div>
</div>
<!-- global setting grid template (begin) -->
<div id="globalsetting_template" class="grid_rows even" style="display:none">
    <div class="grid_row_cell" style="width: 35%;">
        <div class="row_celltitles" id="name">
        </div>
    </div>
    <div class="grid_row_cell" style="width: 23%;">
        <div class="row_celltitles" id="value">
        </div>
        <input class="text" id="value_edit" style="width: 200px; display: none;" type="text" />
        <div id="value_edit_errormsg" style="display: none">
        </div>
    </div>
    <div class="grid_row_cell" style="width: 27%;">
        <div class="row_celltitles" id="description">description
        </div>
    </div>
</div>
<!-- global setting grid template (end) -->

<div id="dialog_alert_restart_management_server" title="Alert" style="display:none">
    <p>
        <%=t.t("please.restart.your.management.server.for.your.new.settings.to.take.effect")%>        
    </p>
</div>
