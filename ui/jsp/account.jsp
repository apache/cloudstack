<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>
<!-- account detail panel (begin) -->
<!-- Loading -->
    
 
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_accountsicon.gif" /></div>
    <h1>
        Accounts</h1>
</div>
<div class="contentbox" id="right_panel_content">	
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div id="tab_details" class="content_tabs on">
            <%=t.t("Details")%></div>
        <div id="tab_user" class="content_tabs off">
            <%=t.t("Users")%></div>
    </div> 
    <!--Details tab (begin)-->
    <div id="tab_content_details">
        <div class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div>
        <div class="grid_container">
        	<div class="grid_header">
            	<div id="grid_header_title" class="grid_header_title">(title)</div>
                <div id="action_link" class="grid_actionbox">
                    <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                        <ul class="actionsdropdown_boxlist" id="action_list">
                        	<li><%=t.t("no.available.actions")%></li>
                        </ul>
                    </div>
                </div>
                <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
                display: none;">
                    <div class="gridheader_loader" id="icon">
                    </div>
                    <p id="description">
                        Detaching Disk &hellip;</p>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("ID")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="id">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("Role")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="role">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        Account Name:</div>
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
                        <%=t.t("Domain")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="domain">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("VMs")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="vm_total">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("IPs")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="ip_total">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("Bytes.Received")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="bytes_received">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("Bytes.Sent")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="bytes_sent">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("State")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="state">
                    </div>
                </div>
            </div>
        </div>        
        <div class="grid_botactionpanel">
	        <div class="gridbot_buttons" id="save_button" style="display:none;">Save</div>
	        <div class="gridbot_buttons" id="cancel_button" style="display:none;">Cancel</div>
	    </div>         
    </div>
    <!--Details tab (end)-->
    <!--user tab (start)-->
    <div style="display: none;" id="tab_content_user">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
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
    <div class="actionpanel_button_wrapper" id="add_account_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Account" /></div>
            <div class="actionpanel_button_links">
                Add Account
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_user_button" style="display: none">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add User" /></div>
            <div class="actionpanel_button_links">
                Add User
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
        <div class="grid_actionbox" id="user_action_link">
            <div class="grid_actionsdropdown_box" id="user_action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                Waiting &hellip;
            </p>
        </div>       
    </div>
    
    <div class="grid_rows" id="after_action_info_container" style="display:none">
        <div class="grid_row_cell" style="width: 90%; border: none;">
            <div class="row_celltitles">
                <strong id="after_action_info">Message will appear here</strong></div>
        </div>
    </div>
        
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                ID:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="id">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                User Name:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="username">
            </div>
        </div>
    </div>   
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                State:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="state">
            </div>
        </div>
    </div>    
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                API Key:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="apikey">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Secret Key:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="secretkey">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Account Name:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="account">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Role:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="role">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Domain:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="domain">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Email:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="email">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                First Name:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="firstname">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Last Name:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="lastname">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Timezone:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="timezone">
            </div>
        </div>
    </div>    
</div>
<!-- user tab template (end) -->

<!-- dialogs (begin) -->
<div id="dialog_resource_limits" title="Resource Limits" style="display:none">
	<p>
	    <%=t.t("please.specify.limits.to.the.various.resources.-1.means.the.resource.has.no.limits")%>	    
	</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label><%=t.t("instance.limit")%>:</label>
					<input class="text" type="text" name="limits_vm" id="limits_vm" value="-1" />
					<div id="limits_vm_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label><%=t.t("public.ip.limit")%>:</label>
					<input class="text" type="text" name="limits_ip" id="limits_ip" value="-1" />
					<div id="limits_ip_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label><%=t.t("disk.volume.limit")%>:</label>
					<input class="text" type="text" name="limits_volume" id="limits_volume" value="-1" />
					<div id="limits_volume_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label><%=t.t("snapshot.limit")%>:</label>
					<input class="text" type="text" name="limits_snapshot" id="limits_snapshot" value="-1" />
					<div id="limits_snapshot_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label><%=t.t("template.limit")%>:</label>
					<input class="text" type="text" name="limits_template" id="limits_template" value="-1" />
					<div id="limits_template_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
			</ol>
		</form>
	</div>
</div>

<div id="dialog_disable_account" title="Disable account" style="display:none">
    <p>
        <%=t.t("please.confirm.you.want.to.disable.account.that.will.prevent.account.access.to.the.cloud.and.shut.down.all.existing.virtual.machines")%>        
    </p>
</div>

<div id="dialog_lock_account" title="Lock account" style="display:none">
    <p>
        <%=t.t("please.confirm.you.want.to.lock.account.that.will.prevent.account.access.to.the.cloud")%>        
    </p>
</div>

<div id="dialog_enable_account" title="Enable account" style="display:none">
    <p>
        <%=t.t("please.confirm.you.want.to.enable.account")%>        
    </p>
</div>

<!-- Add Account Dialog (begin)-->
<div id="dialog_add_account" title="Add New Account" style="display: none">   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="add_user_username">
                    User name:</label>
                <input class="text" type="text" id="add_user_username" />
                <div id="add_user_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_password">
                    Password:</label>
                <input class="text" type="password" id="add_user_password"
                    autocomplete="off" />
                <div id="add_user_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_email">
                    Email:</label>
                <input class="text" type="text" id="add_user_email" />
                <div id="add_user_email_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_firstname">
                    First name:</label>
                <input class="text" type="text" id="add_user_firstname" />
                <div id="add_user_firstname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_lastname">
                    Last name:</label>
                <input class="text" type="text" id="add_user_lastname" />
                <div id="add_user_lastname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_account">
                    Account:</label>
                <input class="text" type="text" id="add_user_account" />
                <div id="add_user_account_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_account_type">
                    Role:</label>
                <select class="select" id="add_user_account_type">
                    <option value="0">User</option>
                    <option value="1">Admin</option>
                </select>
            </li>
            <li>
                <label for="add_user_domain">
                    Domain:</label>
                <select class="select" id="domain_dropdown">
                </select>
            </li>
            <li>
                <label for="add_user_timezone">
                    Time Zone:</label>
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
<div id="dialog_add_user" title="Add New User" style="display: none">  
    <p>
        Add a new user under account <b><span id="account_name"></span></b>
    </p> 
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="add_user_username">
                    User name:</label>
                <input class="text" type="text" id="add_user_username" />
                <div id="add_user_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_password">
                    Password:</label>
                <input class="text" type="password" id="add_user_password"
                    autocomplete="off" />
                <div id="add_user_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_email">
                    Email:</label>
                <input class="text" type="text" id="add_user_email" />
                <div id="add_user_email_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_firstname">
                    First name:</label>
                <input class="text" type="text" id="add_user_firstname" />
                <div id="add_user_firstname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_user_lastname">
                    Last name:</label>
                <input class="text" type="text" id="add_user_lastname" />
                <div id="add_user_lastname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>            
            <li>
                <label for="add_user_timezone">
                    Time Zone:</label>
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
            Adding....</p>
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
	<p>Please review your changes before clicking 'Save'</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form2">
			<ol>		
				<li>
					<label for="edit_user_username">User name:</label>
					<input class="text" type="text" name="edit_user_username" id="edit_user_username"/>
					<div id="edit_user_username_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="edit_user_email">Email:</label>
					<input class="text" type="text" name="edit_user_email" id="edit_user_email"/>
					<div id="edit_user_email_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="edit_user_firstname">First name:</label>
					<input class="text" type="text" name="edit_user_firstname" id="edit_user_firstname"/>
					<div id="edit_user_firstname_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="edit_user_lastname">Last name:</label>
					<input class="text" type="text" name="edit_user_lastname" id="edit_user_lastname"/>
					<div id="edit_user_lastname_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>	
				<li>
				    <label for="edit_user_timezone">Time Zone:</label>
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
<div id="dialog_change_password" title="Change Password" style="display:none">
	<p>Please review your changes before clicking 'Save'</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form4">
			<ol>					
				<li>
					<label for="change_password_password1">Password:</label>
					<input class="text" type="password" name="change_password_password1" id="change_password_password1" AUTOCOMPLETE="off"/>
					<div id="change_password_password1_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>							
			</ol>
		</form>
	</div>
</div>
<!-- Change Password Dialog (end) -->

<!-- dialogs (end) -->

<!-- advanced search template (begin) -->

<div class="adv_searchpopup_bg" id="advanced_search_template" style="display:none;">
            <div class="adv_searchformbox">
                <form action="#" method="post">
                <ol>
                    <li>
                        <input class="text textwatermark" type="text" id="adv_search_name" value="by Name"/>
                        
                    </li>
                    <li>
                        <select class="select" id="adv_search_role">
                            <option value=""></option>                    
                            <option value="0">User</option>
                            <option value="2">Domain-Admin</option>
                            <option value="1">Admin</option>
                        </select>
                    </li>
                </ol>
                </form>
            </div>
 </div>
<!-- advanced search template (end) -->