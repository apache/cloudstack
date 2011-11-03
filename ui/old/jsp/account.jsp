<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 
	'message.disable.account' : '<fmt:message key="message.disable.account"/>',
	'message.lock.account' : '<fmt:message key="message.lock.account"/>',
	'message.enable.account' : '<fmt:message key="message.enable.account"/>',
	'message.delete.account' : '<fmt:message key="message.delete.account"/>',
	'label.action.edit.account' : '<fmt:message key="label.action.edit.account"/>',
	'label.action.update.resource.count' : '<fmt:message key="label.action.update.resource.count"/>',
	'label.action.update.resource.count.processing' : '<fmt:message key="label.action.update.resource.count.processing"/>',		
	'label.action.resource.limits': '<fmt:message key="label.action.resource.limits"/>',
	'label.action.disable.account': '<fmt:message key="label.action.disable.account"/>',
	'label.action.disable.account.processing': '<fmt:message key="label.action.disable.account.processing"/>',
	'label.action.lock.account': '<fmt:message key="label.action.lock.account"/>',
	'label.action.lock.account.processing': '<fmt:message key="label.action.lock.account.processing"/>',
	'label.action.enable.account': '<fmt:message key="label.action.enable.account"/>',
	'label.action.enable.account.processing': '<fmt:message key="label.action.enable.account.processing"/>',
	'label.action.delete.account': '<fmt:message key="label.action.delete.account"/>',
	'label.action.delete.account.processing': '<fmt:message key="label.action.delete.account.processing"/>',
	'label.action.edit.user': '<fmt:message key="label.action.edit.user"/>',
	'label.action.change.password': '<fmt:message key="label.action.change.password"/>',
	'label.action.generate.keys': '<fmt:message key="label.action.generate.keys"/>',
	'label.action.generate.keys.processing': '<fmt:message key="label.action.generate.keys.processing"/>',
	'label.action.disable.user': '<fmt:message key="label.action.disable.user"/>',
	'label.action.disable.user.processing': '<fmt:message key="label.action.disable.user.processing"/>',
	'label.action.enable.user': '<fmt:message key="label.action.enable.user"/>',
	'label.action.enable.user.processing': '<fmt:message key="label.action.enable.user.processing"/>',
	'label.action.delete.user': '<fmt:message key="label.action.delete.user"/>',
	'label.action.delete.user.processing': '<fmt:message key="label.action.delete.user.processing"/>'
};	
</script>

<!-- account detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_accountsicon.gif" /></div>
    <h1><fmt:message key="label.menu.accounts"/></h1>
</div>
<div class="contentbox" id="right_panel_content">	
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div id="tab_details" class="content_tabs on">
            <fmt:message key="label.details"/></div>
        <div id="tab_user" class="content_tabs off">
            <fmt:message key="label.users"/></div>
    </div> 
    <!--Details tab (begin)-->
    <div id="tab_content_details">
        <div class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div class="grid_container">
        	<div class="grid_header">
            	<div id="grid_header_title" class="grid_header_title">(title)</div>
                <div id="action_link" class="grid_actionbox"><p><fmt:message key="label.actions"/></p>
                    <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                        <ul class="actionsdropdown_boxlist" id="action_list">
                        	<li><fmt:message key="label.no.actions"/></li>
                        </ul>
                    </div>
                </div>
                <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
                display: none;">
                    <div class="gridheader_loader" id="icon">
                    </div>
                    <p id="description">
                        <fmt:message key="label.detaching.disk"/> &hellip;</p>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.id"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="id">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.role"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="role">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.account.name"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="name">
                    </div>                    
                    <input class="text" id="name_edit" style="width: 200px; display: none;" type="text" />
	                <div id="name_edit_errormsg" style="display:none"></div>                    
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.domain"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="domain">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.vms"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="vm_total">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.ips"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="ip_total">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.bytes.received"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="bytes_received">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.bytes.sent"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="bytes_sent">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.state"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="state">
                    </div>
                </div>
            </div>
        </div>        
        <div class="grid_botactionpanel">
	        <div class="gridbot_buttons" id="save_button" style="display:none;"><fmt:message key="label.save"/></div>
	        <div class="gridbot_buttons" id="cancel_button" style="display:none;"><fmt:message key="label.cancel"/></div>
	    </div>         
    </div>
    <!--Details tab (end)-->
    <!--user tab (start)-->
    <div style="display: none;" id="tab_content_user">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">
        </div>
    </div> 
    <!--user tab (end)-->    
</div>
<!-- account detail panel (end) -->

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="add_account_button" style="display: none">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.account"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_user_button" style="display: none">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.user"/>
            </div>
        </div>
    </div>
</div>
<!--  top buttons (end) -->

<!-- user tab template (begin) -->
<div class="grid_container" id="user_tab_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/> &hellip;
            </p>
        </div>       
    </div>
    
    <div class="grid_rows" id="after_action_info_container" style="display:none">
        <div class="grid_row_cell" style="width: 90%; border: none;">
            <div class="row_celltitles">
                <strong id="after_action_info"></strong></div>
        </div>
    </div>
        
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.id"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="id">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.username"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="username">
            </div>
        </div>
    </div>   
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.state"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="state">
            </div>
        </div>
    </div>    
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.api.key"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="apikey">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.secret.key"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="secretkey">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.account.name"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="account">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.role"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="role">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.domain"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="domain">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.email"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="email">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.first.name"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="firstname">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.last.name"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="lastname">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.timezone"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="timezone">
            </div>
        </div>
    </div>    
</div>
<!-- user tab template (end) -->

<!-- dialogs (begin) -->
<div id="dialog_resource_limits" title='<fmt:message key="label.action.resource.limits"/>' style="display:none">
	<p>
		<fmt:message key="message.edit.limits"/>
	</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label><fmt:message key="label.instance.limits"/>:</label>
					<input class="text" type="text" name="limits_vm" id="limits_vm" value="-1" />
					<div id="limits_vm_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label><fmt:message key="label.ip.limits"/>:</label>
					<input class="text" type="text" name="limits_ip" id="limits_ip" value="-1" />
					<div id="limits_ip_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label><fmt:message key="label.volume.limits"/>:</label>
					<input class="text" type="text" name="limits_volume" id="limits_volume" value="-1" />
					<div id="limits_volume_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label><fmt:message key="label.snapshot.limits"/>:</label>
					<input class="text" type="text" name="limits_snapshot" id="limits_snapshot" value="-1" />
					<div id="limits_snapshot_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label><fmt:message key="label.template.limits"/>:</label>
					<input class="text" type="text" name="limits_template" id="limits_template" value="-1" />
					<div id="limits_template_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
			</ol>
		</form>
	</div>
</div>

<!-- Add Account Dialog (begin)-->
<div id="dialog_add_account" title='<fmt:message key="label.add.account"/>' style="display: none">   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="add_user_username">
                    <fmt:message key="label.username"/>:</label>
                <input class="text" type="text" id="add_user_username" />
                <div id="add_user_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_password">
                    <fmt:message key="label.password"/>:</label>
                <input class="text" type="password" id="add_user_password"
                    autocomplete="off" />
                <div id="add_user_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_email">
                    <fmt:message key="label.email"/>:</label>
                <input class="text" type="text" id="add_user_email" />
                <div id="add_user_email_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_firstname">
                    <fmt:message key="label.first.name"/>:</label>
                <input class="text" type="text" id="add_user_firstname" />
                <div id="add_user_firstname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_lastname">
                    <fmt:message key="label.last.name"/>:</label>
                <input class="text" type="text" id="add_user_lastname" />
                <div id="add_user_lastname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_account">
                    <fmt:message key="label.account.name"/>:</label>
                <input class="text" type="text" id="add_user_account" />
                <div id="add_user_account_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_account_type">
                    <fmt:message key="label.role"/>:</label>
                <select class="select" id="add_user_account_type">
                    <option value="0"><fmt:message key="label.user"/></option>
                    <option value="1"><fmt:message key="label.admin"/></option>
                </select>
            </li>
            <li>
                <label for="domain">
                    <fmt:message key="label.domain"/>:</label>
                
                <input class="text" type="text" id="domain" />
                <div id="domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
                
                <!--  
                <select class="select" id="domain_dropdown">
                </select>
                -->
                
            </li>
            <li>
                <label for="add_user_timezone">
                    <fmt:message key="label.timezone"/>:</label>
                <select class="select" id="add_user_timezone" style="width: 240px">
                    <option value=""></option>
                    <option value='Etc/GMT+12'>[UTC-12:00] GMT-12:00</option>
                    <option value='Etc/GMT+11'>[UTC-11:00] GMT-11:00</option>
                    <option value='Pacific/Samoa'>[UTC-11:00] Samoa Standard Time</option>
                    <option value='Pacific/Honolulu'>[UTC-10:00] Hawaii Standard Time</option>
                    <option value='US/Alaska'>[UTC-09:00] Alaska Standard Time</option>
                    <option value='America/Los_Angeles'>[UTC-08:00] Pacific Standard Time</option>
                    <option value='Mexico/BajaNorte'>[UTC-08:00] Baja California</option>
                    <option value='US/Arizona'>[UTC-07:00] Arizona</option>
                    <option value='US/Mountain'>[UTC-07:00] Mountain Standard Time</option>
                    <option value='America/Chihuahua'>[UTC-07:00] Chihuahua, La Paz</option>
                    <option value='America/Chicago'>[UTC-06:00] Central Standard Time</option>
                    <option value='America/Costa_Rica'>[UTC-06:00] Central America</option>
                    <option value='America/Mexico_City'>[UTC-06:00] Mexico City, Monterrey</option>
                    <option value='Canada/Saskatchewan'>[UTC-06:00] Saskatchewan</option>
                    <option value='America/Bogota'>[UTC-05:00] Bogota, Lima</option>
                    <option value='America/New_York'>[UTC-05:00] Eastern Standard Time</option>
                    <option value='America/Caracas'>[UTC-04:00] Venezuela Time</option>
                    <option value='America/Asuncion'>[UTC-04:00] Paraguay Time</option>
                    <option value='America/Cuiaba'>[UTC-04:00] Amazon Time</option>
                    <option value='America/Halifax'>[UTC-04:00] Atlantic Standard Time</option>
                    <option value='America/La_Paz'>[UTC-04:00] Bolivia Time</option>
                    <option value='America/Santiago'>[UTC-04:00] Chile Time</option>
                    <option value='America/St_Johns'>[UTC-03:30] Newfoundland Standard Time</option>
                    <option value='America/Araguaina'>[UTC-03:00] Brasilia Time</option>
                    <option value='America/Argentina/Buenos_Aires'>[UTC-03:00] Argentine Time</option>
                    <option value='America/Cayenne'>[UTC-03:00] French Guiana Time</option>
                    <option value='America/Godthab'>[UTC-03:00] Greenland Time</option>
                    <option value='America/Montevideo'>[UTC-03:00] Uruguay Time]</option>
                    <option value='Etc/GMT+2'>[UTC-02:00] GMT-02:00</option>
                    <option value='Atlantic/Azores'>[UTC-01:00] Azores Time</option>
                    <option value='Atlantic/Cape_Verde'>[UTC-01:00] Cape Verde Time</option>
                    <option value='Africa/Casablanca'>[UTC] Casablanca</option>
                    <option value='Etc/UTC'>[UTC] Coordinated Universal Time</option>
                    <option value='Atlantic/Reykjavik'>[UTC] Reykjavik</option>
                    <option value='Europe/London'>[UTC] Western European Time</option>
                    <option value='CET'>[UTC+01:00] Central European Time</option>
                    <option value='Europe/Bucharest'>[UTC+02:00] Eastern European Time</option>
                    <option value='Africa/Johannesburg'>[UTC+02:00] South Africa Standard Time</option>
                    <option value='Asia/Beirut'>[UTC+02:00] Beirut</option>
                    <option value='Africa/Cairo'>[UTC+02:00] Cairo</option>
                    <option value='Asia/Jerusalem'>[UTC+02:00] Israel Standard Time</option>
                    <option value='Europe/Minsk'>[UTC+02:00] Minsk</option>
                    <option value='Europe/Moscow'>[UTC+03:00] Moscow Standard Time</option>
                    <option value='Africa/Nairobi'>[UTC+03:00] Eastern African Time</option>
                    <option value='Asia/Karachi'>[UTC+05:00] Pakistan Time</option>
                    <option value='Asia/Kolkata'>[UTC+05:30] India Standard Time</option>
                    <option value='Asia/Bangkok'>[UTC+05:30] Indochina Time</option>
                    <option value='Asia/Shanghai'>[UTC+08:00] China Standard Time</option>
                    <option value='Asia/Kuala_Lumpur'>[UTC+08:00] Malaysia Time</option>
                    <option value='Australia/Perth'>[UTC+08:00] Western Standard Time (Australia)</option>
                    <option value='Asia/Taipei'>[UTC+08:00] Taiwan</option>
                    <option value='Asia/Tokyo'>[UTC+09:00] Japan Standard Time</option>
                    <option value='Asia/Seoul'>[UTC+09:00] Korea Standard Time</option>
                    <option value='Australia/Adelaide'>[UTC+09:30] Central Standard Time (South Australia)</option>
                    <option value='Australia/Darwin'>[UTC+09:30] Central Standard Time (Northern Territory)</option>
                    <option value='Australia/Brisbane'>[UTC+10:00] Eastern Standard Time (Queensland)</option>
                    <option value='Australia/Canberra'>[UTC+10:00] Eastern Standard Time (New South Wales)</option>
                    <option value='Pacific/Guam'>[UTC+10:00] Chamorro Standard Time</option>
                    <option value='Pacific/Auckland'>[UTC+12:00] New Zealand Standard Time</option>
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Add Account Dialog (end)-->

<!-- Add User Dialog (begin)-->
<div id="dialog_add_user" title='<fmt:message key="label.add.user"/>' style="display: none">  
    <p>
        <fmt:message key="message.new.user"/> :<b><span id="account_name"></span></b>
    </p> 
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="add_user_username">
                    <fmt:message key="label.username"/>:</label>
                <input class="text" type="text" id="add_user_username" />
                <div id="add_user_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_password">
                    <fmt:message key="label.password"/>:</label>
                <input class="text" type="password" id="add_user_password"
                    autocomplete="off" />
                <div id="add_user_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_email">
                    <fmt:message key="label.email"/>:</label>
                <input class="text" type="text" id="add_user_email" />
                <div id="add_user_email_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_firstname">
                    <fmt:message key="label.first.name"/>:</label>
                <input class="text" type="text" id="add_user_firstname" />
                <div id="add_user_firstname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_lastname">
                    <fmt:message key="label.last.name"/>:</label>
                <input class="text" type="text" id="add_user_lastname" />
                <div id="add_user_lastname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>            
            <li>
                <label for="add_user_timezone">
                    <fmt:message key="label.timezone"/>:</label>
                <select class="select" id="add_user_timezone" style="width: 240px">
                    <option value=""></option>
                    <option value='Etc/GMT+12'>[UTC-12:00] GMT-12:00</option>
                    <option value='Etc/GMT+11'>[UTC-11:00] GMT-11:00</option>
                    <option value='Pacific/Samoa'>[UTC-11:00] Samoa Standard Time</option>
                    <option value='Pacific/Honolulu'>[UTC-10:00] Hawaii Standard Time</option>
                    <option value='US/Alaska'>[UTC-09:00] Alaska Standard Time</option>
                    <option value='America/Los_Angeles'>[UTC-08:00] Pacific Standard Time</option>
                    <option value='Mexico/BajaNorte'>[UTC-08:00] Baja California</option>
                    <option value='US/Arizona'>[UTC-07:00] Arizona</option>
                    <option value='US/Mountain'>[UTC-07:00] Mountain Standard Time</option>
                    <option value='America/Chihuahua'>[UTC-07:00] Chihuahua, La Paz</option>
                    <option value='America/Chicago'>[UTC-06:00] Central Standard Time</option>
                    <option value='America/Costa_Rica'>[UTC-06:00] Central America</option>
                    <option value='America/Mexico_City'>[UTC-06:00] Mexico City, Monterrey</option>
                    <option value='Canada/Saskatchewan'>[UTC-06:00] Saskatchewan</option>
                    <option value='America/Bogota'>[UTC-05:00] Bogota, Lima</option>
                    <option value='America/New_York'>[UTC-05:00] Eastern Standard Time</option>
                    <option value='America/Caracas'>[UTC-04:00] Venezuela Time</option>
                    <option value='America/Asuncion'>[UTC-04:00] Paraguay Time</option>
                    <option value='America/Cuiaba'>[UTC-04:00] Amazon Time</option>
                    <option value='America/Halifax'>[UTC-04:00] Atlantic Standard Time</option>
                    <option value='America/La_Paz'>[UTC-04:00] Bolivia Time</option>
                    <option value='America/Santiago'>[UTC-04:00] Chile Time</option>
                    <option value='America/St_Johns'>[UTC-03:30] Newfoundland Standard Time</option>
                    <option value='America/Araguaina'>[UTC-03:00] Brasilia Time</option>
                    <option value='America/Argentina/Buenos_Aires'>[UTC-03:00] Argentine Time</option>
                    <option value='America/Cayenne'>[UTC-03:00] French Guiana Time</option>
                    <option value='America/Godthab'>[UTC-03:00] Greenland Time</option>
                    <option value='America/Montevideo'>[UTC-03:00] Uruguay Time]</option>
                    <option value='Etc/GMT+2'>[UTC-02:00] GMT-02:00</option>
                    <option value='Atlantic/Azores'>[UTC-01:00] Azores Time</option>
                    <option value='Atlantic/Cape_Verde'>[UTC-01:00] Cape Verde Time</option>
                    <option value='Africa/Casablanca'>[UTC] Casablanca</option>
                    <option value='Etc/UTC'>[UTC] Coordinated Universal Time</option>
                    <option value='Atlantic/Reykjavik'>[UTC] Reykjavik</option>
                    <option value='Europe/London'>[UTC] Western European Time</option>
                    <option value='CET'>[UTC+01:00] Central European Time</option>
                    <option value='Europe/Bucharest'>[UTC+02:00] Eastern European Time</option>
                    <option value='Africa/Johannesburg'>[UTC+02:00] South Africa Standard Time</option>
                    <option value='Asia/Beirut'>[UTC+02:00] Beirut</option>
                    <option value='Africa/Cairo'>[UTC+02:00] Cairo</option>
                    <option value='Asia/Jerusalem'>[UTC+02:00] Israel Standard Time</option>
                    <option value='Europe/Minsk'>[UTC+02:00] Minsk</option>
                    <option value='Europe/Moscow'>[UTC+03:00] Moscow Standard Time</option>
                    <option value='Africa/Nairobi'>[UTC+03:00] Eastern African Time</option>
                    <option value='Asia/Karachi'>[UTC+05:00] Pakistan Time</option>
                    <option value='Asia/Kolkata'>[UTC+05:30] India Standard Time</option>
                    <option value='Asia/Bangkok'>[UTC+05:30] Indochina Time</option>
                    <option value='Asia/Shanghai'>[UTC+08:00] China Standard Time</option>
                    <option value='Asia/Kuala_Lumpur'>[UTC+08:00] Malaysia Time</option>
                    <option value='Australia/Perth'>[UTC+08:00] Western Standard Time (Australia)</option>
                    <option value='Asia/Taipei'>[UTC+08:00] Taiwan</option>
                    <option value='Asia/Tokyo'>[UTC+09:00] Japan Standard Time</option>
                    <option value='Asia/Seoul'>[UTC+09:00] Korea Standard Time</option>
                    <option value='Australia/Adelaide'>[UTC+09:30] Central Standard Time (South Australia)</option>
                    <option value='Australia/Darwin'>[UTC+09:30] Central Standard Time (Northern Territory)</option>
                    <option value='Australia/Brisbane'>[UTC+10:00] Eastern Standard Time (Queensland)</option>
                    <option value='Australia/Canberra'>[UTC+10:00] Eastern Standard Time (New South Wales)</option>
                    <option value='Pacific/Guam'>[UTC+10:00] Chamorro Standard Time</option>
                    <option value='Pacific/Auckland'>[UTC+12:00] New Zealand Standard Time</option>
                </select>
            </li>
        </ol>
        </form>
    </div>    
    <!--Loading box-->
    <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display: none;">
        <div class="ui_dialog_loader">
        </div>
        <p>
            <fmt:message key="label.adding"/>....</p>
    </div>
    <!--Confirmation msg box-->
    <!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
    <div id="info_container" class="ui_dialog_messagebox error" style="display: none;">
        <div id="icon" class="ui_dialog_msgicon error">
        </div>
        <div id="info" class="ui_dialog_messagebox_text error">
            (info)</div>
    </div>    
</div>
<!-- Add User Dialog (end)-->

<!-- Edit User Dialog (begin)-->
<div id="dialog_edit_user" title="Edit User" style="display:none">
	<p><fmt:message key="message.edit.confirm"/></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form2">
			<ol>		
				<li>
					<label for="edit_user_username"><fmt:message key="label.username"/>:</label>
					<input class="text" type="text" name="edit_user_username" id="edit_user_username"/>
					<div id="edit_user_username_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="edit_user_email"><fmt:message key="label.email"/>:</label>
					<input class="text" type="text" name="edit_user_email" id="edit_user_email"/>
					<div id="edit_user_email_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="edit_user_firstname"><fmt:message key="label.first.name"/>:</label>
					<input class="text" type="text" name="edit_user_firstname" id="edit_user_firstname"/>
					<div id="edit_user_firstname_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="edit_user_lastname"><fmt:message key="label.last.name"/>:</label>
					<input class="text" type="text" name="edit_user_lastname" id="edit_user_lastname"/>
					<div id="edit_user_lastname_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>	
				<li>
				    <label for="edit_user_timezone"><fmt:message key="label.timezone"/>:</label>
				    <select class="select" id="edit_user_timezone">
                        <option value=""></option>                      
                        <option value='Etc/GMT+11'>[UTC-11:00] GMT-11:00</option>
								<option value='Pacific/Samoa'>[UTC-11:00] Samoa Standard Time</option>
								<option value='Pacific/Honolulu'>[UTC-10:00] Hawaii Standard Time</option>
								<option value='US/Alaska'>[UTC-09:00] Alaska Standard Time</option>
								<option value='America/Los_Angeles'>[UTC-08:00] Pacific Standard Time</option>
								<option value='Mexico/BajaNorte'>[UTC-08:00] Baja California</option>
								<option value='US/Arizona'>[UTC-07:00] Arizona</option>
								<option value='US/Mountain'>[UTC-07:00] Mountain Standard Time</option>
								<option value='America/Chihuahua'>[UTC-07:00] Chihuahua, La Paz</option>
								<option value='America/Chicago'>[UTC-06:00] Central Standard Time</option>
								<option value='America/Costa_Rica'>[UTC-06:00] Central America</option>
								<option value='America/Mexico_City'>[UTC-06:00] Mexico City, Monterrey</option>
								<option value='Canada/Saskatchewan'>[UTC-06:00] Saskatchewan</option>
								<option value='America/Bogota'>[UTC-05:00] Bogota, Lima</option>
								<option value='America/New_York'>[UTC-05:00] Eastern Standard Time</option>
								<option value='America/Caracas'>[UTC-04:00] Venezuela Time</option>
								<option value='America/Asuncion'>[UTC-04:00] Paraguay Time</option>
								<option value='America/Cuiaba'>[UTC-04:00] Amazon Time</option>
								<option value='America/Halifax'>[UTC-04:00] Atlantic Standard Time</option>
								<option value='America/La_Paz'>[UTC-04:00] Bolivia Time</option>
								<option value='America/Santiago'>[UTC-04:00] Chile Time</option>
								<option value='America/St_Johns'>[UTC-03:30] Newfoundland Standard Time</option>
								<option value='America/Araguaina'>[UTC-03:00] Brasilia Time</option>
								<option value='America/Argentina/Buenos_Aires'>[UTC-03:00] Argentine Time</option>
								<option value='America/Cayenne'>[UTC-03:00] French Guiana Time</option>
								<option value='America/Godthab'>[UTC-03:00] Greenland Time</option>
								<option value='America/Montevideo'>[UTC-03:00] Uruguay Time]</option>
								<option value='Etc/GMT+2'>[UTC-02:00] GMT-02:00</option>
								<option value='Atlantic/Azores'>[UTC-01:00] Azores Time</option>
								<option value='Atlantic/Cape_Verde'>[UTC-01:00] Cape Verde Time</option>
								<option value='Africa/Casablanca'>[UTC] Casablanca</option>
								<option value='Etc/UTC'>[UTC] Coordinated Universal Time</option>
								<option value='Atlantic/Reykjavik'>[UTC] Reykjavik</option>
								<option value='Europe/London'>[UTC] Western European Time</option>
								<option value='CET'>[UTC+01:00] Central European Time</option>
								<option value='Europe/Bucharest'>[UTC+02:00] Eastern European Time</option>
								<option value='Africa/Johannesburg'>[UTC+02:00] South Africa Standard Time</option>
								<option value='Asia/Beirut'>[UTC+02:00] Beirut</option>
								<option value='Africa/Cairo'>[UTC+02:00] Cairo</option>
								<option value='Asia/Jerusalem'>[UTC+02:00] Israel Standard Time</option>
								<option value='Europe/Minsk'>[UTC+02:00] Minsk</option>
								<option value='Europe/Moscow'>[UTC+03:00] Moscow Standard Time</option>
								<option value='Africa/Nairobi'>[UTC+03:00] Eastern African Time</option>
								<option value='Asia/Karachi'>[UTC+05:00] Pakistan Time</option>
								<option value='Asia/Kolkata'>[UTC+05:30] India Standard Time</option>
								<option value='Asia/Bangkok'>[UTC+05:30] Indochina Time</option>
								<option value='Asia/Shanghai'>[UTC+08:00] China Standard Time</option>
								<option value='Asia/Kuala_Lumpur'>[UTC+08:00] Malaysia Time</option>
								<option value='Australia/Perth'>[UTC+08:00] Western Standard Time (Australia)</option>
								<option value='Asia/Taipei'>[UTC+08:00] Taiwan</option>
								<option value='Asia/Tokyo'>[UTC+09:00] Japan Standard Time</option>
								<option value='Asia/Seoul'>[UTC+09:00] Korea Standard Time</option>
								<option value='Australia/Adelaide'>[UTC+09:30] Central Standard Time (South Australia)</option>
								<option value='Australia/Darwin'>[UTC+09:30] Central Standard Time (Northern Territory)</option>
								<option value='Australia/Brisbane'>[UTC+10:00] Eastern Standard Time (Queensland)</option>
								<option value='Australia/Canberra'>[UTC+10:00] Eastern Standard Time (New South Wales)</option>
								<option value='Pacific/Guam'>[UTC+10:00] Chamorro Standard Time</option>
								<option value='Pacific/Auckland'>[UTC+12:00] New Zealand Standard Time</option>                                          
         		    </select>
				</li>					
			</ol>
		</form>
	</div>
</div>
<!-- Edit User Dialog (end)-->

<!-- Change Password Dialog (begin) -->
<div id="dialog_change_password" title='<fmt:message key="label.action.change.password"/>' style="display:none">
	<p><fmt:message key="message.edit.confirm"/></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form4">
			<ol>					
				<li>
					<label for="change_password_password1"><fmt:message key="label.password"/>:</label>
					<input class="text" type="password" name="change_password_password1" id="change_password_password1" AUTOCOMPLETE="off"/>
					<div id="change_password_password1_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>							
			</ol>
		</form>
	</div>
</div>
<!-- Change Password Dialog (end) -->

<!-- dialogs (end) -->

<div id="hidden_container">   
    <!-- advanced search popup (begin) -->
    <div class="adv_searchpopup_bg" id="advanced_search_popup" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>               
                <li>
                    <select class="select" id="adv_search_role">
                        <option value=""><fmt:message key="label.by.role"/></option>
                        <option value="0"><fmt:message key="label.user"/></option>
                        <option value="2"><fmt:message key="label.domain.admin"/></option>
                        <option value="1"><fmt:message key="label.admin"/></option>
                    </select>
                </li>
            </ol>
            </form>
        </div>
    </div>    
    <!-- advanced search popup (end) -->
</div>
