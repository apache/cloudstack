<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_zoneicon.gif" alt="Zone" /></div>
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
        <div class="content_tabs off" id="tab_network">
            <%=t.t("network")%></div>
        <div class="content_tabs off" id="tab_secondarystorage">
            <%=t.t("secondary.storage")%></div>
    </div>
    
    
<!-- Zone wizard (begin)-->
<div class="ui-widget-overlay" style="display:none;"></div>
<div class="zonepopup_container" style="display: none">
    <div id="step1" style="display: block;">
        <div class="zonepopup_container_top">
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step1_bg.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="zonepopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        Step 1: <strong>Select a Network</strong></h2>
                    <p>
                        Please select a template for your new virtual instance. You can also choose to select
                        a blank template from which an ISO image can be installed onto.
                    </p>
                </div>
                <div class="vmpopup_contentpanel">
                    <div class="rev_tempsearchpanel">
                        <label for="wizard_zone">
                            Availability Zone:</label>
                        <select class="select" id="wizard_zone" name="zone">
                        </select>
                        
                                        
                        
                        <div class="rev_tempsearchbox">
                            <form method="post" action="#">
                            <ol>
                                <li>
                                    <input id="search_input" class="text" type="text" name="search_input" />
                            </ol>
                            </form>
                            <div id="search_button" class="rev_searchbutton">
                                Search</div>
                        </div>
                    </div>
                    <div class="rev_wizformarea">
                        <div class="revwiz_message_container" style="display: none;" id="wiz_message">
                            <div class="revwiz_message_top">
                                <p id="wiz_message_text">
                                    Please select a template or ISO to continue</p>
                            </div>
                            <div class="revwiz_message_bottom">
                                <div class="revwizcontinue_button" id="wiz_message_continue">
                                </div>
                            </div>
                        </div>
                        <div class="rev_wizmid_tempbox">
                            <div class="revwiz_loadingbox" id="wiz_template_loading" style="display: none">
                                <div class="loading_gridanimation">
                                </div>
                                <p>
                                    Loading...</p>
                            </div>
                            <div class="rev_wizmid_tempbox_left" id="wiz_template_filter">
                                <div class="rev_wizmid_selectedtempbut" id="wiz_featured">
                                    Featured Template</div>
                                <div class="rev_wizmid_nonselectedtempbut" id="wiz_my">
                                    My Template</div>
                                <div class="rev_wizmid_nonselectedtempbut" id="wiz_community">
                                    Community Template</div>
                                <div class="rev_wizmid_nonselectedtempbut" id="wiz_blank">
                                    Blank Template</div>
                            </div>
                            <div class="rev_wizmid_tempbox_right">
                                <div class="rev_wiztemplistpanel" id="template_container">  
                                    
                                </div>
                                <div class="rev_wiztemplistactions">
                                    <div class="rev_wiztemplist_actionsbox">
                                        <a href="#" id="prev_page">Prev</a> <a href="#" id="next_page">Next</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step" style="display: none;">
                    </div>
                    <div class="vmpop_nextbutton" id="next_step">
                        Go to Step 2</div>
                </div>
            </div>
        </div>
        <div class="zonepopup_container_bot">
        </div>
    </div>
    
    
    
    
</div>
<!-- Zone wizard (end)-->
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
                    <div id="title" class="grid_header_title">
                        Title</div>
                    <div class="grid_actionbox" id="action_link">
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
                <div class="grid_rows even">
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
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("domain")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="domain">
                        </div>
                        <!--  
                        <select class="select" id="domain_edit" style="width: 202px; display: none;">	                       
	                    </select>  
	                    -->
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
    <!-- Network tab (start)-->
    <div style="display: none;" id="tab_content_network">
        <div class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    Loading &hellip;</p>
            </div>
        </div>
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
    <!-- Network tab (end)-->
    <!-- Secondary Storage tab (start)-->
    <div id="tab_content_secondarystorage" style="display: none">
        Secondary Storage
        <div class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    Loading &hellip;</p>
            </div>
        </div>
    </div>
    <!-- Secondary Storage tab (end)-->
</div>

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
                    CIDR:</label>
                <input class="text" type="text" name="add_pod_cidr" id="add_pod_cidr" />
                <div id="add_pod_cidr_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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
                    <option value="false">direct</option>
                    <option value="true">public</option>
                </select>
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
                    <option value="zone-wide">zone-wide</option>
                    <option value="account-specific">account-specific</option>
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
        <div class="grid_actionbox" id="secondarystorage_action_link">
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