<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_zoneicon.gif" /></div>
   <h1>
        Zone
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on" id="tab_details">
            <%=t.t("details")%></div>        
        <div class="content_tabs off" id="tab_secondarystorage">
            <%=t.t("secondary.storage")%></div>   
        <!--    
        <div class="content_tabs off" id="tab_network">
            Network </div>  
        -->         
    </div>
    
    <!-- Details tab (start)-->
    <div id="tab_content_details">
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    Loading &hellip;</p>
            </div>
        </div>
        <div id="tab_container">
            <div class="grid_container">
                <div class="grid_header">
                    <div id="grid_header_title" class="grid_header_title">
                        Title</div>
                    <div class="grid_actionbox" id="action_link"><p>Actions</p>
                        <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                            <ul class="actionsdropdown_boxlist" id="action_list">
                                <li>
                                    <%=t.t("no.available.actions")%></li>
                            </ul>
                        </div>
                    </div>
                    <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
                        display: none;">
                        <div class="gridheader_loader" id="icon">
                        </div>
                        <p id="description">
                            Waiting &hellip;</p>
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
                            <%=t.t("name")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="name">
                        </div>
                        <input class="text" id="name_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="name_edit_errormsg" style="display:none"></div>       
                    </div>
                </div>
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("dns1")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="dns1">
                        </div>
                        <input class="text" id="dns1_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="dns1_edit_errormsg" style="display:none"></div>       
                    </div>
                </div>
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("dns2")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="dns2">
                        </div>
                        <input class="text" id="dns2_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="dns2_edit_errormsg" style="display:none"></div>       
                    </div>
                </div>
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("internaldns1")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="internaldns1">
                        </div>
                        <input class="text" id="internaldns1_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="internaldns1_edit_errormsg" style="display:none"></div>       
                    </div>
                </div>
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("internaldns2")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="internaldns2">
                        </div>
                        <input class="text" id="internaldns2_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="internaldns2_edit_errormsg" style="display:none"></div>       
                    </div>
                </div>                
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("network.type")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="networktype">
                        </div>                             
                    </div>
                </div>                
                <div class="grid_rows even" id="vlan_container">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("vlan")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="vlan">
                        </div>
                        <input class="text" id="startvlan_edit" style="width: 100px; display: none;" type="text" />
	                    <div id="startvlan_edit_errormsg" style="display:none"></div>  	                    
	                    <input class="text" id="endvlan_edit" style="width: 100px; display: none;" type="text" />
	                    <div id="endvlan_edit_errormsg" style="display:none"></div>            
                    </div>
                </div>
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("guestcidraddress")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="guestcidraddress">
                        </div>
                        <input class="text" id="guestcidraddress_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="guestcidraddress_edit_errormsg" style="display:none"></div>       
                    </div>
                </div>
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("domain")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="domain">
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
    <!-- Details tab (end)-->
    
    <!-- Secondary Storage tab (start)-->
    <div id="tab_content_secondarystorage" style="display: none">        
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    Loading &hellip;</p>
            </div>
        </div>
        <div id="tab_container">
        </div>
    </div>
    <!-- Secondary Storage tab (end)-->
    
    <!-- Network tab (start)-->
    <div style="display: none;" id="tab_content_network">
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    Loading &hellip;</p>
            </div>
        </div>
        <div id="tab_container">
	        <div class="text_container">
	            <div class="network_dgbox">
	                <div class="networkdg_zonepanel">
	                    <div class="networkdg_zonebox">
	                        <div class="networkdg_zonecloud" id="zone_cloud">
	                            <p>
	                                <%=t.t("zone")%>
	                                <span id="zone_name"></span>
	                            </p>
	                        </div>
	                        <div class="networkdg_zoneconnect">
	                        </div>
	                    </div>
	                    <div class="networkswitchpanel">
	                        <div class="networkswitch_titlebox">
	                            <p>
	                                <strong>Guest VLAN: <span id="zone_vlan"></span></strong>
	                            </p>
	                        </div>
	                        <div class="networkswitch_top">
	                        </div>
	                        <div class="networkswitch_midpanel" id="vlan_container">
	                        </div>
	                        <div class="networkswitch_end">
	                        </div>
	                    </div>
	                </div>
	            </div>
	        </div>
	    </div>   
    </div>
    <!-- Network tab (end)-->       
</div>

<!--  top buttons (begin) -->
<div id="top_buttons">
	<div class="actionpanel_button_wrapper" id="add_pod_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                Add Pod
            </div>
        </div>
    </div>  
    <div class="actionpanel_button_wrapper" id="add_cluster_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png"/></div>
            <div class="actionpanel_button_links">
                Add Cluster
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_host_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                Add Host
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_primarystorage_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                Add Primary Storage
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_secondarystorage_button"
       >
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                Add Secondary Storage
            </div>
        </div>
    </div>   
    <!--
    <div class="actionpanel_button_wrapper" id="add_vlan_button" style="display: none;">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add VLAN" /></div>
            <div class="actionpanel_button_links">
                Add VLAN
            </div>
        </div>
    </div>
    -->
</div>
<!--  top buttons (end) -->


<!-- ***** dialogs ***** (begin)-->
<!-- Add Pod Dialog (begin) -->
<div id="dialog_add_pod" title="Add Pod" style="display: none">
    <p>
        Add a new pod for zone <b><span id="add_pod_zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="user_name" style="width: 115px;">
                    Name:</label>
                <input class="text" type="text" name="add_pod_name" id="add_pod_name" />
                <div id="add_pod_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_pod_gateway" style="width: 115px;">
                    Gateway:</label>
                <input class="text" type="text" id="add_pod_gateway" />
                <div id="add_pod_gateway_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name" style="width: 115px;">
                    Netmask:</label>
                <input class="text" type="text" name="add_pod_netmask" id="add_pod_netmask" />
                <div id="add_pod_netmask_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name" style="width: 115px;">
                    Private IP Range:</label>
                <input class="text" style="width: 67px" type="text" name="add_pod_startip" id="add_pod_startip" /><span>-</span>
                <input class="text" style="width: 67px" type="text" name="add_pod_endip" id="add_pod_endip" />
                <div id="add_pod_startip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
                <div id="add_pod_endip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>            
            <li id="guestip_container" style="display: none">
                <label style="width: 115px;">
                    Guest IP Range:</label>
                <input class="text" style="width: 92px" type="text" id="startguestip" /><span>-</span>
                <input class="text" style="width: 92px" type="text" id="endguestip" />
                <div id="startguestip_errormsg" class="dialog_formcontent_errormsg" style="display: none;
                    margin-left: 133px;">
                </div>
                <div id="endguestip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="guestnetmask_container" style="display: none">
                <label style="width: 115px;">
                    Guest Netmask:</label>
                <input class="text" type="text" id="guestnetmask" />
                <div id="guestnetmask_errormsg" class="dialog_formcontent_errormsg" style="display: none;
                    margin-left: 0;">
                </div>
            </li>
            <li id="guestgateway_container" style="display: none">
                <label style="width: 115px;">
                    Guest Gateway:</label>
                <input class="text" type="text" id="guestgateway" />
                <div id="guestgateway_errormsg" class="dialog_formcontent_errormsg" style="display: none;
                    margin-left: 0;">
                </div>
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
<!-- Add Pod Dialog (end) -->
<!-- Add Secondary Storage Dialog (begin) -->
<div id="dialog_add_secondarystorage" title="Add Secondary Storage" style="display: none">
    <p>
        Add a new storage for zone <b><span id="zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form1">
        <ol>
            <li>
                <label>
                    NFS Server:</label>
                <input class="text" type="text" name="nfs_server" id="nfs_server" />
                <div id="nfs_server_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="path">
                    Path:</label>
                <input class="text" type="text" name="path" id="path" />
                <div id="path_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
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
<!-- Add Secondary Storage Dialog (end) -->
<!-- Add VLAN IP Range Dialog for zone (begin) -->
<div id="dialog_add_vlan_for_zone" title="Add VLAN IP Range" style="display: none">
    <p>
        Add a new IP range for zone: <b><span id="zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li style="display: none" id="add_publicip_vlan_type_container">
                <label for="add_publicip_vlan_type">
                    Type:</label>
                <select class="select" name="add_publicip_vlan_type" id="add_publicip_vlan_type">
                    <option value="false">Direct</option>
                    <option value="true">Virtual</option>
                </select>
            </li>
			<li style="display: none" id="add_publicip_vlan_network_name_container">
                <label for="user_name">
                    Network Name:</label>
                <input class="text" type="text" name="add_publicip_vlan_network_name" id="add_publicip_vlan_network_name" />
                <div id="add_publicip_vlan_network_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li style="display: none" id="add_publicip_vlan_network_desc_container">
                <label for="user_name">
                    Network Desc:</label>
                <input class="text" type="text" name="add_publicip_vlan_network_desc" id="add_publicip_vlan_network_desc" />
                <div id="add_publicip_vlan_network_desc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_publicip_vlan_container">
                <label for="add_publicip_vlan_tagged">
                    VLAN:</label>
                <select class="select" name="add_publicip_vlan_tagged" id="add_publicip_vlan_tagged">
                </select>
            </li>
            <li style="display: none" id="add_publicip_vlan_vlan_container">
                <label for="user_name">
                    VLAN ID:</label>
                <input class="text" type="text" name="add_publicip_vlan_vlan" id="add_publicip_vlan_vlan" />
                <div id="add_publicip_vlan_vlan_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_publicip_vlan_scope_container">
                <label for="add_publicip_vlan_scope">
                    Scope:</label>
                <select class="select" name="add_publicip_vlan_scope" id="add_publicip_vlan_scope">
                    <!--  
                    <option value="zone-wide">zone-wide</option>
                    <option value="account-specific">account-specific</option>
                    -->
                </select>
            </li>
            <li style="display: none" id="add_publicip_vlan_pod_container">
                <label for="user_name">
                    Pod:</label>
                <select class="select" name="add_publicip_vlan_pod" id="add_publicip_vlan_pod">
                </select>
            </li>
            <li style="display: none" id="add_publicip_vlan_domain_container">
                <label for="user_name">
                    Domain:</label>
                <select class="select" name="add_publicip_vlan_domain" id="add_publicip_vlan_domain">
                </select>
            </li>
            <li style="display: none" id="add_publicip_vlan_account_container">
                <label for="user_name">
                    Account:</label>
                <input class="text" type="text" name="add_publicip_vlan_account" id="add_publicip_vlan_account" />
                <div id="add_publicip_vlan_account_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    Gateway:</label>
                <input class="text" type="text" name="add_publicip_vlan_gateway" id="add_publicip_vlan_gateway" />
                <div id="add_publicip_vlan_gateway_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    Netmask:</label>
                <input class="text" type="text" name="add_publicip_vlan_netmask" id="add_publicip_vlan_netmask" />
                <div id="add_publicip_vlan_netmask_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    IP Range:</label>
                <input class="text" style="width: 67px" type="text" name="add_publicip_vlan_startip"
                    id="add_publicip_vlan_startip" /><span>-</span>
                <input class="text" style="width: 67px" type="text" name="add_publicip_vlan_endip"
                    id="add_publicip_vlan_endip" />
                <div id="add_publicip_vlan_startip_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
                <div id="add_publicip_vlan_endip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
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
<!-- Add VLAN IP Range Dialog for zone (end) -->
<div id="dialog_confirmation_delete_secondarystorage" title="Confirmation" style="display: none">
    <p>
        <%=t.t("please.confirm.you.want.to.delete.the.secondary.storage")%>
    </p>
</div>


<!-- Add Host Dialog in zone page (begin) -->
<div id="dialog_add_host_in_zone_page" title="Add Host" style="display: none">   
    <p>
        Add a host for zone <b><span id="zone_name"></span></b>
        </span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
            	<label for="host_hypervisor">Hypervisor:</label>
                <select class="select" id="host_hypervisor">
                    <option value="XenServer" SELECTED>Xen Server</option>		
                    <option value="KVM">KVM</option>										
                    <option value="VmWare">VMware</option>										
                </select>
            </li>
            <li>
                <label>
                    Pod:</label>
                <select class="select" id="pod_dropdown">
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            <li input_group="general">
                <label for="host_hostname">
                    Host name:</label>
                <input class="text" type="text" name="host_hostname" id="host_hostname" />
                <div id="host_hostname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="general">
                <label for="user_name">
                    User name:</label>
                <input class="text" type="text" name="host_username" id="host_username" />
                <div id="host_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="general">
                <label for="host_password">
                    Password:</label>
                <input class="text" type="password" name="host_password" id="host_password" autocomplete="off" />
                <div id="host_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware">
                <label for="host_vcenter_address">
                    vCenter Address:</label>
                <input class="text" type="text" name="host_vcenter_address" id="host_vcenter_address" />
                <div id="host_vcenter_address_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware">
                <label for="host_vcenter_username">
                    vCenter User:</label>
                <input class="text" type="text" name="host_vcenter_username" id="host_vcenter_username" />
                <div id="host_vcenter_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware">
                <label for="host_vcenter_password">
                    vCenter Password:</label>
                <input class="text" type="password" name="host_vcenter_password" id="host_vcenter_password" autocomplete="off" />
                <div id="host_vcenter_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware">
                <label for="host_vcenter_dc">
                    vCenter Datacenter:</label>
                <input class="text" type="text" name="host_vcenter_dc" id="host_vcenter_dc" />
                <div id="host_vcenter_dc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware">
                <label for="host_vcenter_host">
                    vCenter Host:</label>
                <input class="text" type="text" name="host_vcenter_host" id="host_vcenter_host" />
                <div id="host_vcenter_host_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            
            <li id="cluster_options_container">
                <label>
                    &nbsp;</label><span><u>Cluster Options</u></span> </li>
            <li id="new_cluster_radio_container">
                <label>
                    <input type="radio" name="cluster" value="new_cluster_radio" checked />&nbsp;New
                    cluster:</label>
                <input class="text" type="text" id="new_cluster_name" />
                <div id="new_cluster_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="existing_cluster_radio_container">
                <label>
                    <input type="radio" name="cluster" value="existing_cluster_radio" />&nbsp;Join cluster:</label>
                <select class="select" id="cluster_select">
                </select>
            </li>
            <li id="no_cluster_radio_container">
                <label>
                    <input type="radio" name="cluster" value="no_cluster_radio" />&nbsp;Standalone</label>
                <span style="padding-left: 20px"></span></li>
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
<!-- Add Host Dialog in zone page (end) -->

<!-- Add Hypervisor managed cluster Dialog (begin) -->
<div id="dialog_add_external_cluster_in_zone_page" title="Add Cluster" style="display: none">
    <p>
        Add a hypervisor managed cluster for zone <b><span id="zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
            	<label for="cluster_hypervisor">Hypervisor:</label>
                <select class="select" id="cluster_hypervisor">
                    <option value="XenServer" SELECTED>Xen Server</option>		
                    <option value="KVM">KVM</option>										
                    <option value="VmWare">VMware</option>										
                </select>
            </li>
            <li input_group="vmware">
                <label>
                    Cluster Type:</label>
                <select class="select" id="type_dropdown">
                    <option value="CloudManaged">Cloud.com Managed</option>		
                    <option value="ExternalManaged" SELECTED>vSphere Managed</option>										
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            <li>
                <label>
                    Pod:</label>
                <select class="select" id="pod_dropdown">
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_hostname">
                    vCenter Server:</label>
                <input class="text" type="text" name="cluster_hostname" id="cluster_hostname" />
                <div id="cluster_hostname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_username">
                    vCenter User:</label>
                <input class="text" type="text" name="cluster_username" id="cluster_username" />
                <div id="cluster_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_password">
                    Password:</label>
                <input class="text" type="password" name="cluster_password" id="cluster_password" autocomplete="off" />
                <div id="cluster_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_datacenter">
                    vCenter Datacenter:</label>
                <input class="text" type="text" name="cluster_datacenter" id="cluster_datacenter" />
                <div id="cluster_datacenter_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="cluster_name" id="cluster_name_label">
                    vCenter Cluster:</label>
                <input class="text" type="text" name="cluster_name" id="cluster_name" />
                <div id="cluster_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
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
<!-- Add Hypervisor managed cluster Dialog (end) -->


<!-- Add Primary Storage Dialog  in zone page (begin) -->
<div id="dialog_add_pool_in_zone_page" title="Add Primary Storage" style="display: none"> 
    <p>
        Add a primary storage for zone <b><span id="zone_name"></span></b>
        </span></b>
    </p>   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>            
            <li>
                <label>
                    Pod:</label>
                <select class="select" id="pod_dropdown">
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            <li id="pool_cluster_container">
                <label for="pool_cluster">
                    Cluster:</label>
                <select class="select" id="cluster_select">
                </select>
                <div id="cluster_select_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    Name:</label>
                <input class="text" type="text" name="add_pool_name" id="add_pool_name" />
                <div id="add_pool_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_pool_protocol">
                    Protocol:</label>
                <select class="select" id="add_pool_protocol">
                    <option value="nfs">NFS</option>
                    <option value="iscsi">ISCSI</option>
                    <option value="vmfs">VMFS</option>
                </select>
            </li>
            <li>
                <label for="add_pool_nfs_server">
                    Server:</label>
                <input class="text" type="text" name="add_pool_nfs_server" id="add_pool_nfs_server" />
                <div id="add_pool_nfs_server_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_path_container">
                <label for="add_pool_path">
                    Path:</label>
                <input class="text" type="text" name="add_pool_path" id="add_pool_path" />
                <div id="add_pool_path_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_iqn_container" style="display: none">
                <label for="add_pool_iqn">
                    Target IQN:</label>
                <input class="text" type="text" name="add_pool_iqn" id="add_pool_iqn" />
                <div id="add_pool_iqn_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_lun_container" style="display: none">
                <label for="add_pool_lun">
                    LUN #:</label>
                <input class="text" type="text" name="add_pool_lun" id="add_pool_lun" />
                <div id="add_pool_lun_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_tags_container">
                <label for="add_pool_tags">
                    Tags:</label>
                <input class="text" type="text" id="add_pool_tags" />
                <div id="add_pool_tags_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
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
<!-- Add Primary Storage Dialog  in zone page (end) -->

<!-- ***** dialogs ***** (end)-->
<!-- VLAN Template (begin) -->
<div class="networkswitch_vlanpanel" id="vlan_template" style="display: none;">
    <div class="networkswitch_vlanconnect">
        <div class="networkswitch_closeicon" id="delete_vlan">
        </div>
        <div id="info_icon" class="networkswitch_vlan_infoicon">
        </div>
        <div id="info_dropdown" class="networkswitch_infodropdown" style="display: none;">
            <div class="networkswitch_infodropdown_actionbox">
                <a id="close_link" href="#">Close</a>
            </div>
            <ul class="networkswitch_infodropdown_textbox">
				<li id="network_name_container" style="display: none">
                    <div class="networkswitch_infodropdown_textbox_label">
                        Name:</div>
                    <span id="network_name"></span></li>
				<li id="network_desc_container" style="display: none">
                    <div class="networkswitch_infodropdown_textbox_label">
                        Desc:</div>
                    <span id="network_desc"></span></li>
                <li id="vlan_container">
                    <div class="networkswitch_infodropdown_textbox_label">
                        VLAN ID:</div>
                    <span id="vlan"></span></li>
                <li id="gateway_container">
                    <div class="networkswitch_infodropdown_textbox_label">
                        Gateway:</div>
                    <span id="gateway"></span></li>
                <li id="netmask_container">
                    <div class="networkswitch_infodropdown_textbox_label">
                        Netmask:</div>
                    <span id="netmask"></span></li>
                <li id="iprange_container">
                    <div class="networkswitch_infodropdown_textbox_label">
                        IP Range:</div>
                    <span id="iprange"></span></li>
                <li id="domainid_container" style="display: none">
                    <div class="networkswitch_infodropdown_textbox_label">
                        Domain ID:</div>
                    <span id="domainid"></span></li>
                <li id="domain_container" style="display: none">
                    <div class="networkswitch_infodropdown_textbox_label">
                        Domain:</div>
                    <span id="domain"></span></li>
                <li id="account_container" style="display: none">
                    <div class="networkswitch_infodropdown_textbox_label">
                        Account:</div>
                    <span id="account"></span></li>
                <li id="podname_container" style="display: none">
                    <div class="networkswitch_infodropdown_textbox_label">
                        Pod:</div>
                    <span id="podname"></span></li>
            </ul>
        </div>
        <div class="networkswitch_vlan_detailsbox">
            <div class="networkswitch_vlan_detailsbox_textbox">
                <div class="networkswitch_vlan_detailsbox_textbox_label">
                    VLAN:</div>
                <span id="vlan_id">n</span>
            </div>
            <div class="networkswitch_vlan_detailsbox_textbox">
                <div class="networkswitch_vlan_detailsbox_textbox_label">
                    <%=t.t("ip.address.range")%>:</div>
                <span id="ip_range">n.n.n.n - m.m.m.m</span>
            </div>
        </div>
        <div id="vlan_type_icon" class="networkswitch_typeicon">
        </div>
    </div>
</div>
<!-- VLAN Template (end) -->
<!--  secondary storage tab template (begin) -->
<div class="grid_container" id="secondary_storage_tab_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="title">
        </div>
        <div class="grid_actionbox" id="secondarystorage_action_link"> <p>Actions</p>
            <div class="grid_actionsdropdown_box" id="secondarystorage_action_menu" style="display: none;">
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
    <div class="grid_rows" id="after_action_info_container" style="display: none">
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
                Name:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="name">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("zone")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="zonename">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("type")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="type">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("ip.address")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="ipaddress">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("state")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="state">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("version")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="version">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("last.disconnected")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="disconnected">
            </div>
        </div>
    </div>
</div>
<!--  secondary storage tab template (end) -->