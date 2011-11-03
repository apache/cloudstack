<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 	
	'label.action.edit.global.setting' : '<fmt:message key="label.action.edit.global.setting"/>'
};	
</script>

<div class="main_title" id="right_panel_header">
   
    <div class="main_titleicon">
        <img src="images/title_globalsettingsicon.gif" /></div>
    
    <h1>
        <fmt:message key="label.menu.global.settings"/>
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on">
            <fmt:message key="label.details"/></div>
    </div>
    <div id="tab_content_details">
		<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    <fmt:message key="label.loading"/> &hellip;</p>
            </div>
        </div>    	
		<div id="tab_container">
	        <div class="grid_container">
	        	<div class="grid_header">
	            	<div class="grid_header_cell" style="width:35%; border:none;">
	            		<div class="grid_header_title"><fmt:message key="label.name"/></div>
	                </div>
	                <div class="grid_header_cell" style="width:23%; border:none;">
	            		<div class="grid_header_title"><fmt:message key="label.value"/></div>
	                </div>
	                <div class="grid_header_cell" style="width:27%; border:none;">
	            		<div class="grid_header_title"><fmt:message key="label.description"/></div>
	                </div>                
	                <div class="grid_header_cell" style="width:15%; border:none;">
	            		<div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
	                        <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
	                            <ul class="actionsdropdown_boxlist" id="action_list">
	                                <li><fmt:message key="label.no.actions"/></li>
	                                <li id="edit_button"><fmt:message key="label.edit"/></a></li>
	                            </ul>
	                        </div>
	                    </div>
	                </div>                     
	            </div>            
	            <div id="grid_content" class="fixed_gridcontainer"> 
	                        	
	         	</div>               
	        </div>        
	        <div class="grid_botactionpanel">
	        	<div class="gridbot_buttons" id="save_button" style="display:none;"><fmt:message key="label.save"/></div>
	            <div class="gridbot_buttons" id="cancel_button" style="display:none;"><fmt:message key="label.cancel"/></div>
	        </div>  
        </div>            
    </div>
</div>

<!-- global setting grid template - text type (begin) -->
<div id="globalsetting_template_text" class="grid_rows even" style="display: none; position: none;">
    <div class="grid_row_cell" style="width: 35%; border: none;">
        <div class="row_celltitles" id="name">
        </div>
    </div>
    <div class="grid_row_cell" style="width: 23%; border: none; padding: 1px;">
        <div class="row_celltitles" id="value">
        </div>
        <input class="text" id="value_edit" style="width: 120px; display: none;" type="text" />
        <div id="value_edit_errormsg">
        </div>
    </div>
    <div class="grid_row_cell" style="width: 40%; border: none;">
        <div class="row_celltitles" id="description">
            description
        </div>
    </div>
</div>
<!-- global setting grid template - text type (end) -->

<!-- global setting grid template - password type (begin) -->
<div id="globalsetting_template_password" class="grid_rows even" style="display: none; position: none;">
    <div class="grid_row_cell" style="width: 35%; border: none;">
        <div class="row_celltitles" id="name">
        </div>
    </div>
    <div class="grid_row_cell" style="width: 23%; border: none; padding: 1px;">
        <div class="row_celltitles" id="value" style="display:none">
        </div>        
        <div class="row_celltitles" id="password_mask">
            ********
        </div>        
        <input class="text" id="value_edit" style="width: 120px; display: none;" type="password" />
        <div id="value_edit_errormsg">
        </div>
    </div>
    <div class="grid_row_cell" style="width: 40%; border: none;">
        <div class="row_celltitles" id="description">
            description
        </div>
    </div>
</div>
<!-- global setting grid template - password type (end) -->

<div id="dialog_alert_restart_management_server" title="Alert" style="display:none">
    <p>
		<fmt:message key="message.restart.mgmt.server"/>
    </p>
</div>
