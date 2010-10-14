<%@ page import="java.util.Date" %>
<%
long milliseconds = new Date().getTime();
%>

<script type="text/javascript" src="scripts/cloud.core.instances.js?t=<%=milliseconds%>"></script>

<!-- Content Panel -->
<!-- Submenu -->
<div class="submenu_links" id="submenu_links">
    <div class="submenu_links_on" id="submenu_vms">
        Instances</div>
    <div class="submenu_links_off" id="submenu_routers">
        Routers</div>
    <div class="submenu_links_off" id="submenu_console">
        System</div>
</div>
<!--VM -->
<div class="maincontent" style="display: none;" id="submenu_content_vms">
    <div id="maincontent_title">
        <div class="maintitle_icon">
            <img src="images/instancetitle_icons.gif" title="instance" />
        </div>
        <h1>
            Instances</h1>
        <a class="add_newvmbutton" href="#"></a>
        <div class="search_formarea">
            <form action="#" method="post">
            <ol>
                <li>
                    <input class="text" type="text" name="search_input" id="search_input" /></li>
            </ol>
            </form>
            <a id="search_button" class="search_button" href="#"></a>
            <div id="advanced_search_link" class="advsearch_link">
                Advanced</div>
            <div id="advanced_search" class="adv_searchpopup" style="display: none;">
                <div class="adv_searchformbox">
                    <h3>
                        Advance Search</h3>
                    <a id="advanced_search_close" href="#">Close </a>
                    <form action="#" method="post">
                    <ol>
                        <li>
                            <label for="filter">
                                Name:</label>
                            <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
                        </li>
                        <li>
                            <label for="filter">
                                State:</label>
                            <select class="select" id="adv_search_state">
                                <option value=""></option>
                                <option value="Creating">Creating</option>
                                <option value="Starting">Starting</option>
                                <option value="Running">Running</option>
                                <option value="Stopping">Stopping</option>
                                <option value="Stopped">Stopped</option>
                                <option value="Destroyed">Destroyed</option>
                                <option value="Expunging">Expunging</option>
                                <option value="Migrating">Migrating</option>
                                <option value="Error">Error</option>
                                <option value="Unknown">Unknown</option>
                            </select>
                        </li>
                        <li>
                            <label for="filter">
                                Zone:</label>
                            <select class="select" id="adv_search_zone">
                            </select>
                        </li>
                        <li id="adv_search_pod_li" style="display: none;">
                            <label for="filter">
                                Pod:</label>
                            <select class="select" id="adv_search_pod">
                            </select>
                        </li>
                        <li id="adv_search_domain_li" style="display: none;">
                            <label for="filter">
                                Domain:</label>
                            <select class="select" id="adv_search_domain">
                            </select>
                        </li>
                        <li id="adv_search_account_li" style="display: none;">
                            <label for="filter">
                                Account:</label>
                            <input class="text" type="text" id="adv_search_account" />
                        </li>
                    </ol>
                    </form>
                    <div class="adv_search_actionbox">
                        <div class="adv_searchpopup_button" id="adv_search_button">
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="filter_actionbox">
    </div>
    <div class="grid_container">
        <div id="loading_gridtable" class="loading_gridtable" style="display: none;">
            <div class="loading_gridanimation">
            </div>
            <p>
                Loading...</p>
        </div>
        <div class="grid_header">
            <div class="grid_genheader_cell" style="border: 0;">
                <div class="grid_headertitles">
                    Instances</div>
            </div>
        </div>
        <div id="grid_content">
        </div>
    </div>
    <div id="pagination_panel" class="pagination_panel" style="display: none;">
        <p id="grid_rows_total" />
        <div class="pagination_actionbox">
            <div class="pagination_actions">
                <div class="pagination_actionicon">
                    <img src="images/pagination_refresh.gif" title="refresh" /></div>
                <a id="refresh" href="#">Refresh</a>
            </div>
            <div class="pagination_actions" id="prevPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_previcon.gif" title="prev" /></div>
                <a id="prevPage" href="#">Prev</a>
            </div>
            <div class="pagination_actions" id="nextPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_nexticon.gif" title="next" /></div>
                <a id="nextPage" href="#">Next</a>
            </div>
        </div>
    </div>
</div>
<!-- END Content Panel -->
<!-- VM Instance Template -->
<div id="vm_instance_template" style="width: 100%; height: auto; float: left; padding: 0;
    margin: 0; display: none">
    <div class="vm_rows" id="vm_rows">
        <div class="vm_rows_top">
            <div class="vm_genrows_cell" style="width: 7%;">
                <div class="admin_vmstatus">
                    <div id="vm_state_bar" class="admin_vmgreen_arrow">
                    </div>
                    <div id="vm_state" class="grid_runningtitles" style="margin: 6px 0 0 5px; display: inline;">
                        Running</div>
                </div>
                <div id="vm_action_view_console" class="admin_vmconsole">
                </div>
            </div>
            <div class="vm_genrows_cell" style="width: 1%;">
            </div>
            <div class="vm_genrows_cell" style="width: 29%;">
                <div class="admin_vmcontainer">
                    <div class="admin_vmcontainerleft" style="background: url(images/admin_vmyellow_left.gif) no-repeat top left;">
                    </div>
                    <div class="admin_vmcontainermid" style="background: url(images/admin_vmyellow_mid.gif) repeat-x top left;">
                        <ul class="admin_vmcontainermid_list">
                            <li id="vm_name"></li>
                            <li id="vm_ip_address"></li>
                            <li id="vm_zone"></li>
                        </ul>
                    </div>
                </div>
                <div class="admin_vmcontainerright" style="background: url(images/admin_vmyellow_right.gif) no-repeat  top left;">
                </div>
            </div>
            <div class="vm_genrows_cell" style="width: 1%;">
            </div>
            <div class="vm_genrows_cell" style="width: 30%;">
                <div class="admin_vmcontainer">
                    <div class="admin_vmcontainerleft" style="background: url(images/admin_vmgrey_left.gif) no-repeat top left;">
                    </div>
                    <div class="admin_vmcontainermid" style="background: url(images/admin_vmgrey_mid.gif) repeat-x top left;">
                        <ul class="admin_vmcontainermid_list">
                            <li id="vm_template"></li>
                            <li id="vm_service"></li>
                            <li id="vm_ha"></li>
                        </ul>
                    </div>
                </div>
                <div class="admin_vmcontainerright" style="background: url(images/admin_vmgrey_right.gif) no-repeat  top left;">
                </div>
            </div>
            <div class="vm_genrows_cell" style="width: 1%;">
            </div>
            <div class="vm_genrows_cell" style="width: 29%;">
                <div class="admin_vmcontainer">
                    <div class="admin_vmcontainerleft" style="background: url(images/admin_vmblue_left.gif) no-repeat top left;">
                    </div>
                    <div class="admin_vmcontainermid" style="background: url(images/admin_vmblue_mid.gif) repeat-x top left;">
                        <ul class="admin_vmcontainermid_list">
                            <li style="margin-top: 2px;" id="vm_created"></li>
                            <li style="margin-top: 2px;" id="vm_account"></li>
                            <li style="margin-top: 2px;" id="vm_domain"></li>
                            <li style="margin-top: 2px;" id="vm_host"></li>
                        </ul>
                    </div>
                </div>
                <div class="admin_vmcontainerright" style="background: url(images/admin_vmblue_right.gif) no-repeat  top left;">
                </div>
            </div>
            <div class="vm_genrows_cell" style="width: 1%;">
            </div>
        </div>
        <div class="vm_rows_bot">
            <div id="vm_instance_menu" class="vm_bot_actions" style="display: block;">
                <div id="vm_action_restore" class="vm_botactionslinks" style="display: none">
                    Restore</div>
                <div id="vm_actions" class="vm_botactionslinks">
                    Actions</div>
                <div id="vm_action_volumes" class="vm_botactionslinks_down">
                    Volumes</div>
                <div id="vm_action_statistics" class="vm_botactionslinks_down" style="margin-left:15px; display:inline;">
                    Statistics</div>
            </div>
            <div class="vm_rowbot_loading" style="display: none;" id="vm_loading_container">
                <div class="vm_rowbot_loadinganimation">
                </div>
                <p id="vm_loading_text">
                </p>
            </div>
            <div class="vm_bot_rightinfo">
                <div class="vm_bot_iso">
                    <div id="iso_state" class="vmiso_off">
                    </div>
                    <p>
                        ISO</p>
                </div>
                <div class="vm_bot_groups">
                    <p id="vm_group">
                        No Group</p>
                </div>
            </div>
        </div>
        <div id="instance_loading_overlay" class="vmrow_loading" style="display: none;">
        </div>
        <div class="loadingmessage_container_vm" style="display: none;">
            <div class="loadingmessage_top_vm">
                <p>
                </p>
            </div>
            <div class="loadingmessage_bottom_vm">
                <a id="vm_action_continue" class="continue_button" href="#"></a>
            </div>
        </div>
        <div class="vmactions_dropdownbox" id="vm_actions_container" style="display: none;">
            <div id="vm_actions_close" class="vmactions_dropdownbox_closebutton">
            </div>
            <div class="vmaction_listbox">
                <ul class="vmaction_list">
                    <li>Instance</li>
                    <li>
                        <div id="vm_action_start" class="vmaction_links_on">
                            Start</div>
                    </li>
                    <li>
                        <div id="vm_action_stop" class="vmaction_links_on">
                            Stop</div>
                    </li>
                    <li>
                        <div id="vm_action_reboot" class="vmaction_links_on">
                            Reboot</div>
                    </li>
                    <li>
                        <div id="vm_action_destroy" class="vmaction_links_on">
                            Destroy</div>
                    </li>
                    <li>ISO</li>
                    <li>
                        <div id="vm_action_attach_iso" class="vmaction_links_on">
                            Attach ISO</div>
                    </li>
                    <li>
                        <div id="vm_action_detach_iso" class="vmaction_links_on">
                            Detach ISO</div>
                    </li>
                </ul>
            </div>
            <div class="vmaction_listbox">
                <ul class="vmaction_list" style="border: 0;">
                    <li>Change</li>
                    <li>
                        <div id="vm_action_reset_password" class="vmaction_links_on">
                            Reset Password</div>
                    </li>
                    <li>
                        <div id="vm_action_change_name" class="vmaction_links_on">
                            Change Name</div>
                    </li>
                    <li>
                        <div id="vm_action_change_service" class="vmaction_links_on">
                            Change Service</div>
                    </li>
                    <li>
                        <div id="vm_action_change_group" class="vmaction_links_on">
                            Change Group</div>
                    </li>
                    <li>Others</li>
                    <li id="vm_action_list_network_groups_container">
                        <div id="vm_action_list_network_groups" class="vmaction_links_on">
                            Network Groups</div>
                    </li>
                    <li>
                        <div id="vm_action_ha" class="vmaction_links_on">
                            Enable HA</div>
                    </li>
                </ul>
            </div>
        </div>
    </div>
    <div class="hostadmin_showdetails_panel" id="vm_statistics_panel" style="display:none">
    	<div class="host_statisticspanel">
			<div class="host_statisticslist" id="vm_cpu_stat"><div class="hostcpu_icon"></div><p><strong> CPU Total:</strong>  | <strong>CPU Allocated:</strong>  | <span class="host_statisticspanel_green"> <strong>CPU Used:</strong></span></p></div>
			<div class="host_statisticslist" id="vm_network_stat"><div class="hostnetwork_icon"></div><p><strong> Network read:</strong> | <strong>Network write:</strong></p></div>
        </div>
    </div>    
    <div class="hostadmin_showdetails_panel" id="volume_detail_panel" style="display: none;">
        <div class="hostadmin_showdetails_grid">
            <div class="hostadmin_showdetailsheader">
                <div class="hostadmin_showdetailsheader_cell" style="width: 15%">
                    <div class="grid_headertitles">
                        ID</div>
                </div>
                <div class="hostadmin_showdetailsheader_cell" style="width: 20%">
                    <div class="grid_headertitles">
                        Name</div>
                </div>
                <div class="hostadmin_showdetailsheader_cell" style="width: 15%">
                    <div class="grid_headertitles">
                        Type</div>
                </div>
                <div class="hostadmin_showdetailsheader_cell" style="width: 15%">
                    <div class="grid_headertitles">
                        Size</div>
                </div>
                <div class="hostadmin_showdetailsheader_cell" style="width: 15%">
                    <div class="grid_headertitles">
                        Created</div>
                </div>
                <div class="hostadmin_showdetailsheader_cell" style="width: 19%">
                    <div class="grid_headertitles">
                    </div>
                </div>
            </div>
            <div id="detail_container">
                <div class="hostadmin_showdetails_row_odd">
                    <div class="hostadmin_showdetailsrow_cell" style="width: 100%">
                        <div class="netgrid_celltitles">
                            No Volumes</div>
                    </div>
                </div>
            </div>
        </div>
    </div>       
</div>
<!-- END VM Instance Template -->
<!-- Disk Volume Template -->
<div id="volume_detail_template" style="display: none">
    <div class="adding_loading" style="height: 25px; display: none;">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Detaching &hellip;
        </div>
    </div>
    <div id="volume_body">
        <div class="hostadmin_showdetailsrow_cell" style="width: 15%">
            <div class="netgrid_celltitles" id="detail_id">
            </div>
        </div>
        <div class="hostadmin_showdetailsrow_cell" style="width: 20%">
            <div class="netgrid_celltitles" id="detail_name">
            </div>
        </div>
        <div class="hostadmin_showdetailsrow_cell" style="width: 15%">
            <div class="netgrid_celltitles" id="detail_type">
            </div>
        </div>
        <div class="hostadmin_showdetailsrow_cell" style="width: 15%">
            <div class="netgrid_celltitles" id="detail_size">
            </div>
        </div>
        <div class="hostadmin_showdetailsrow_cell" style="width: 15%">
            <div class="netgrid_celltitles" id="detail_created">
            </div>
        </div>
        <div class="hostadmin_showdetailsrow_cell" style="width: 19%">
            <div class="netgrid_celltitles" id="detail_action">
                <a id="volume_action_detach_disk" href="#">Detach Disk</a><span id="volume_acton_separator"></span>
				<a id="volume_action_create_template" href="#" style="display:none">Create Template</a></div>
        </div>
    </div>
</div>
<!-- Create Template Dialog -->
<div id="dialog_create_template" title="Create Template" style="display:none">
    <p>
        Please specify the following information before creating a template of your disk
        volume: <b><span id="volume_name"></span></b>. Creating a template could take up
        to several hours depending on the size of your disk volume.</p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="user_name">
                    Name:</label>
                <input class="text" type="text" name="create_template_name" id="create_template_name" />
                <div id="create_template_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    Display Text:</label>
                <input class="text" type="text" name="create_template_desc" id="create_template_desc" />
                <div id="create_template_desc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="create_template_os_type">
                    OS Type:</label>
                <select class="select" name="create_template_os_type" id="create_template_os_type">
                </select>
            </li>
            <li>
                <label for="create_template_public">
                    Public:</label>
                <select class="select" name="create_template_public" id="create_template_public">                    
                    <option value="false">No</option>
                    <option value="true">Yes</option>
                </select>
            </li>
            <li>
                <label for="user_name">
                    Password Enabled?:</label>
                <select class="select" name="create_template_password" id="create_template_password">                    
                    <option value="false">No</option>
                    <option value="true">Yes</option>
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Detach Volume Dialog (begin) -->
<div id="dialog_detach_volume" title="Detach Volume" style="display: none">
    <p>
        Please confirm you want to detach the volume</p>
    <div class="dialog_formcontent">
        <div class="selectable_errorbox" style="display: none; width: 250px">
            <p>
                <span style="font-style: bold; color: red"><strong>Error</strong>: </span><span id="apply_error_text">
                    Error text will appear here</span>
            </p>
        </div>
        <div class="selectable_commentbox" style="display: none; width: 250px">
            <div class="selectable_loader">
            </div>
            <p>
                <strong>Please wait...</strong>
            </p>
        </div>
    </div>
</div>
<!-- Console Overlay -->
<div class="overlay_smallpopup" id="spopup">
    <div class="overlay_spopup_midleft">
        <div class="console_box0">
        </div>
        <div class="console_box1" style="display: none">
        </div>
    </div>
</div>
<!--
	<div class="overlay_spopup_midright">
		<span>Additional Details</span><br/>
		<p>Centos 5.3(x86_64)</p><p>Small Instance<br/></p>
		
		<p><strong>Host:</strong> C-1-1-4</p>
		<p><strong>Owner:</strong>sheng@gmail.com</p>
		<p><strong>Domain:</strong>ROOT</p>
		
	</div>
	-->
<!-- END Console Overlay -->
<!-- New VM Wizard (begin) -->
<div class="vmpopup_container" id="vmpopup" style="display: none;">
    <div class="revwiz_closebutton" id="vm_wizard_close">
    </div>
    <div id="step1" style="display: block;">
        <div class="rev_wiztop">
            <div class="rev_wizardbox">
                <div class="rev_wizardsteps">
                    <div class="revwiz_selectednumber">
                        1</div>
                    <h3>
                        Select a Template</h3>
                </div>
                <div class="rev_wizardsteps">
                    <div class="revwiz_nonselectednumber">
                        2</div>
                    <h3>
                        Select a Service</h3>
                </div>
                <div class="rev_wizardsteps">
                    <div class="revwiz_nonselectednumber">
                        3</div>
                    <h3>
                        Optional</h3>
                </div>
                <div class="rev_wizardsteps">
                    <div class="revwiz_nonselectednumber">
                        4</div>
                    <h3>
                        Select Review</h3>
                </div>
            </div>
        </div>
        <div class="rev_wizmid">
            <div class="rev_wizmid_maincontent">
                <div class="rev_wizmid_titlebox">
                    <h2>
                        <strong>Step 1:</strong> Select a Template</h2>
                    <p>
                        Please select a template for your new virtual instance. You can also choose to select
                        a blank template from which an ISO image can be installed onto.
                    </p>
                </div>
                <div class="rev_wizmid_contentbox">
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
                                        <a href="#" id="prevPage">Prev</a>  <a href="#" id="nextPage">Next</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="rev_wizmid_actionbox">
                    <div class="rev_wizmid_actionnext">
                        Go to Step 2</div>
                </div>
            </div>
        </div>
        <div class="rev_wizbot">
        </div>
    </div>
    <div id="step2" style="display: none;">
        <div class="rev_wiztop">
            <div class="rev_wizardbox">
                <div class="rev_wizardsteps">
                    <div class="revwiz_nonselectednumber">
                        1</div>
                    <h3>
                        Select a Template</h3>
                </div>
                <div class="rev_wizardsteps">
                    <div class="revwiz_selectednumber">
                        2</div>
                    <h3>
                        Select a Service</h3>
                </div>
                <div class="rev_wizardsteps">
                    <div class="revwiz_nonselectednumber">
                        3</div>
                    <h3>
                        Optional</h3>
                </div>
                <div class="rev_wizardsteps">
                    <div class="revwiz_nonselectednumber">
                        4</div>
                    <h3>
                        Select Review</h3>
                </div>
            </div>
        </div>
        <div class="rev_wizmid">
            <div class="rev_wizmid_maincontent">
                <div class="rev_wizmid_titlebox">
                    <h2>
                        <strong>Step 2:</strong> Select a Service</h2>
                    <p>
                        Please select the CPU and Memory requirements you need for your new Virtual
                        Instance.</p>
                </div>
                <div class="rev_wizmid_contentbox">
                    <div class="rev_tempsearchpanel">
                    </div>
                    <div class="rev_wizformarea">
                        <div class="revwiz_message_container" style="display: none;" id="wiz_message">
                            <div class="revwiz_message_top">
                                <p id="wiz_message_text">
                                    Please select an offering to continue</p>
                            </div>
                            <div class="revwiz_message_bottom">
                                <div class="revwizcontinue_button" id="wiz_message_continue">
                                </div>
                            </div>
                        </div>
                        <form name="step2" method="post" action="#">
                        <div class="revwiz_formcontent_title">
                            Service Offerings :</div>
                        <ol id="wizard_service_offering">
                        </ol>
                        <ol id="wizard_network_groups_container" style="display:none; margin-top:10px;">
                            <li>
                                <label for="wizard_network_groups">
                                    Network Groups:</label>
                                <select id="wizard_network_groups" class="multiple" multiple="multiple" size="5">
                                </select>
                                <p style="color:#666; float:left; width:auto; margin:15px 0 0 10px; display:inline;">(Use <strong>Ctrl-click</strong> to select all applicable security groups)</p> 
                            </li>
                        </ol>
                        </form>
                    </div>
                </div>
                <div class="rev_wizmid_actionbox">
                    <div class="rev_wizmid_actionnext">
                        Go to Step 3</div>
                    <div class="rev_wizmid_actionback">
                        Back</div>
                </div>
            </div>
        </div>
        <div class="rev_wizbot">
        </div>
    </div>
    <div id="step3" style="display: none;">
        <div class="rev_wiztop">
            <div class="rev_wizardbox">
                <div class="rev_wizardsteps">
                    <div class="revwiz_nonselectednumber">
                        1</div>
                    <h3>
                        Select a Template</h3>
                </div>
                <div class="rev_wizardsteps">
                    <div class="revwiz_nonselectednumber">
                        2</div>
                    <h3>
                        Select a Service</h3>
                </div>
                <div class="rev_wizardsteps">
                    <div class="revwiz_selectednumber">
                        3</div>
                    <h3>
                        Optional</h3>
                </div>
                <div class="rev_wizardsteps">
                    <div class="revwiz_nonselectednumber">
                        4</div>
                    <h3>
                        Select Review</h3>
                </div>
            </div>
        </div>
        <div class="rev_wizmid">
            <div class="rev_wizmid_maincontent">
                <div class="rev_wizmid_titlebox">
                    <h2>
                        <strong>Step 3:</strong> Optional</h2>
                    <p>
                        You can choose to name and group your virtual machine for easy identification. You can also choose additional data storage. (These options can be added at any time.) </p>
                </div>
                <div class="rev_wizmid_contentbox">
                    <div class="rev_tempsearchpanel">
                    </div>
                    <div class="rev_wizformarea">
                        <form name="step1" method="post" action="#">
                        <ol>
                            <li>
                                <div>
                                    <label for="wizard_vm_name">
                                        Name (optional):</label>
                                    <input class="text" type="text" id="wizard_vm_name" />
                                </div>
                                <div id="wizard_vm_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                                </div>
                            </li>
                            <li>
                                <div>
                                    <label for="wizard_vm_group">
                                        Group (optional):</label>
                                    <input class="text" type="text" id="wizard_vm_group" />
                                </div>
                                <div id="wizard_vm_group_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                                </div>
                            </li>
                            <li>
                                <div class="revwiz_formcontent_title" id="wizard_root_disk_offering_title" style="display: none;">
                                    Root Disk Offerings:</div>
                                <ol id="wizard_root_disk_offering">
                                </ol>
                                <div class="revwiz_formcontent_title" id="wizard_data_disk_offering_title" style="display: block; margin-top:10px;">
                                    Data Disk Offerings (optional) :</div>
                                <ol id="wizard_data_disk_offering">
                                </ol>
                            </li>
                        </ol>
                        </form>
                    </div>
                </div>
                <div class="rev_wizmid_actionbox">
                    <div class="rev_wizmid_actionnext">
                        Go to Step 4</div>
                    <div class="rev_wizmid_actionback">
                        Back</div>
                </div>
            </div>
        </div>
        <div class="rev_wizbot">
        </div>
    </div>
    <div id="step4" style="display: none;">
        <div class="rev_wiztop">
            <div class="rev_wizardbox">
                <div class="rev_wizardsteps">
                    <div class="revwiz_nonselectednumber">
                        1</div>
                    <h3>
                        Select a Template</h3>
                </div>
                <div class="rev_wizardsteps">
                    <div class="revwiz_nonselectednumber">
                        2</div>
                    <h3>
                        Select a Service</h3>
                </div>
                <div class="rev_wizardsteps">
                    <div class="revwiz_nonselectednumber">
                        3</div>
                    <h3>
                        Optional</h3>
                </div>
                <div class="rev_wizardsteps">
                    <div class="revwiz_selectednumber">
                        4</div>
                    <h3>
                        Select Review</h3>
                </div>
            </div>
        </div>
        <div class="rev_wizmid">
            <div class="rev_wizmid_maincontent">
                <div class="rev_wizmid_titlebox">
                    <h2>
                        <strong>Step 4:</strong> Review</h2>
                    <p>
                        Please review you selections and hit 'Submit' to create your new instance. Click
                        'Prev' to make any changes to your new VM Instance.</p>
                </div>
                <div class="rev_wizmid_contentbox">
                    <div class="rev_tempsearchpanel">
                    </div>
                    <div class="rev_wiz_reviewbox">
                        <div class="rev_wiz_reviewlist">
                            <div class="rev_wiz_reviewlabel">
                                Zone:</div>
                            <div class="rev_wiz_reviewanswers" id="wizard_review_zone">
                            </div>
                        </div>
                        <div class="rev_wiz_reviewlist">
                            <div class="rev_wiz_reviewlabel">
                                Name:</div>
                            <div class="rev_wiz_reviewanswers" id="wizard_review_name">
                            </div>
                        </div>
                        <div class="rev_wiz_reviewlist">
                            <div class="rev_wiz_reviewlabel">
                                Group:</div>
                            <div class="rev_wiz_reviewanswers" id="wizard_review_group">
                            </div>
                        </div>
                        <div class="rev_wiz_reviewlist">
                            <div class="rev_wiz_reviewlabel">
                                Template:</div>
                            <div class="rev_wiz_reviewanswers" id="wizard_review_template">
                            </div>
                        </div>
                        <div class="rev_wiz_reviewlist" id="wizard_review_iso_p">
                            <div class="rev_wiz_reviewlabel">
                                ISO:</div>
                            <div class="rev_wiz_reviewanswers" id="wizard_review_iso">
                            </div>
                        </div>
                        <div class="rev_wiz_reviewlist">
                            <div class="rev_wiz_reviewlabel">
                                Service Offering:</div>
                            <div class="rev_wiz_reviewanswers" id="wizard_review_service_offering">
                            </div>
                        </div>
                        <div class="rev_wiz_reviewlist" id="wizard_review_network_groups_p" style="display:none">
                            <div class="rev_wiz_reviewlabel">
                                Network Groups:</div>
                            <div class="rev_wiz_reviewanswers" id="wizard_review_network_groups">
                            </div>
                        </div>
                        <div class="rev_wiz_reviewlist" id="wizard_review_data_disk_offering_p">
                            <div class="rev_wiz_reviewlabel">
                                Data Disk Offering:</div>
                            <div class="rev_wiz_reviewanswers" id="wizard_review_data_disk_offering">
                            </div>
                        </div>
                        <div class="rev_wiz_reviewlist" id="wizard_review_root_disk_offering_p">
                            <div class="rev_wiz_reviewlabel">
                                Root Disk Offering:</div>
                            <div class="rev_wiz_reviewanswers" id="wizard_review_root_disk_offering">
                            </div>
                        </div>
                    </div>
                </div>
                <div class="rev_wizmid_actionbox">
                    <div class="rev_wizmid_actionnext">
                        Submit</div>
                    <div class="rev_wizmid_actionback">
                        Back</div>
                </div>
            </div>
        </div>
        <div class="rev_wizbot">
        </div>
    </div>
</div>
<!-- New VM Wizard (end) -->
<!-- Change Service Offering Dialog -->
<div id="dialog_change_service_offering" title="Change Service Offering" style="display:none">
    <p>
        To change your service for <b><span id="change_vm_name"></span></b>, please select
        from the following services and select 'Change'. You must restart your virtual instance
        for the new service offering to take effect.</p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="user_name">
                    Service Offering:</label>
                <select class="select" name="change_service_offerings" id="change_service_offerings">
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- List Network Groups Dialog (begin) -->
<div id="dialog_list_network_groups" title="Network Groups">
    <ul class="network_groups_list_first_level" id="network_groups_list_first_level">         
    </ul>
</div>
<ul class="network_groups_list_second_level" id="network_groups_list_second_level">
</ul>
<!-- List Network Groups Dialog (end) -->
<!-- Change Group Dialog -->
<div id="dialog_change_group" title="Change Group" style="display:none">
    <p>
        Please specify the new group you want to assign to your Virtual Instance: <b><span
            id="vm_name"></span></b>. If no such group exists, a new one will be created
        for you.</p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="change_group_name">
                    Group Name:</label>
                <input class="text" type="text" name="change_group_name" id="change_group_name" />
                <div id="change_group_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Change Name Dialog -->
<div id="dialog_change_name" title="Change Name" style="display:none">
    <p>
        Please specify the new name you want to change for your Virtual Instance: <b><span
            id="vm_name"></span></b>.</p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="user_name">
                    Instance Name:</label>
                <input class="text" type="text" name="change_instance_name" id="change_instance_name" />
                <div id="change_instance_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Attach ISO Dialog -->
<div id="dialog_attach_iso" title="Attach ISO" style="display:none">
    <p>
        Please specify the ISO you wish to attach to your Virtual Instance: <b><span id="vm_name">
        </span></b>.</p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="user_name">
                    Available ISO:</label>
                <select class="select" name="attach_iso_select" id="attach_iso_select">
                    <option value="none">No Available ISO</option>
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>

<!-- Snapshots-->
<div class="maincontent" style="display: none;" id="submenu_content_snapshots">
    <div id="maincontent_title">
        <h1>
            Snapshots</h1>
        <div class="search_formarea">
            <form action="#" method="post">
            <ol>
                <li>
                    <input class="text" type="text" name="search_VM" id="search_VM" /></li>
            </ol>
            </form>
            <a class="search_button" href="#"></a>
        </div>
    </div>
    <div class="filter_actionbox">
        <div class="selection_formarea" style="display: none;">
            <form action="#" method="post">
            <label for="filter">
                Filters:</label>
            <select class="select" id="template_type">
                <option value="true">Public</option>
                <option value="false">Private</option>
            </select>
            </form>
        </div>
    </div>
    <div class="net_gridwrapper">
        <div class="net_gridleft">
            <div class="netgrid_container">
                <div id="loading_gridtable_snapshots" class="loading_gridtable">
                    <div class="loading_gridanimation">
                    </div>
                    <p>
                        Loading...</p>
                </div>
                <div class="grid_header">
                    <div class="snapgridheader_cell1">
                        <div class="grid_headertitles">
                            VM Name</div>
                    </div>
                    <div class="snapgridheader_cell2">
                        <div class="grid_headertitles">
                        </div>
                    </div>
                    <div class="snapgridheader_cell3">
                        <div class="grid_headertitles">
                        </div>
                    </div>
                </div>
                <div id="grid_content">
                </div>
            </div>
        </div>
    </div>
    <div class="net_gridright">
        <div class="net_displaybox">
            <div class="net_displaybox_top">
                <div class="net_displaytitlebox">
                    <h2>
                        Click on a Virtual Instance to see more details
                    </h2>
                </div>
            </div>
            <div class="net_displaybox_mid" style="display: none">
                <div class="display_content">
                    <div class="display_gridbox">
                        <div class="display_gridheader">
                            <div class="display_snapheadercell1">
                                <div class="display_headercell_title">
                                    Type</div>
                            </div>
                            <div class="display_snapheadercell2">
                                <div class="display_headercell_title">
                                    Name</div>
                            </div>
                            <div class="display_snapheadercell2">
                                <div class="display_headercell_title">
                                    Created</div>
                            </div>
                            <div class="display_snapheadercell3">
                            </div>
                        </div>
                        <div id="display_gridcontent">
                        </div>
                    </div>
                </div>
            </div>
            <div class="net_displaybox_bot">
            </div>
        </div>
    </div>
</div>
<!-- Snapshot template -->
<div style="display: none" id="snapshot_template">
    <div class="display_rowloading" style="display: none">
        <div class="display_rowloader_animation">
        </div>
        <p>
            Deleting...</p>
    </div>
    <div id="snap_body">
        <div class="display_snaprowcell1">
            <div class="display_rowcell_title" id="snapshot_interval">
                Hourly</div>
        </div>
        <div class="display_snaprowcell2">
            <div class="display_rowcell_title" id="snapshot_name">
                name</div>
        </div>
        <div class="display_snaprowcell2">
            <div class="display_rowcell_title" id="snapshot_created">
                24</div>
        </div>
        <div class="display_snaprowcell3">
            <a class="display_rollbackbutton" href="#" id="snapshot_action_rollback"></a><a class="display_deletebutton"
                href="#" id="snapshot_action_delete"></a>
        </div>
    </div>
</div>
<!-- VM Snapshot template -->
<div style="display: none" id="vm_snapshot_template">
    <div class="adding_loading" style="height: 25px; display: none;">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Taking Snapshot &hellip;
        </div>
    </div>
    <div id="snap_body">
        <div class="snap_row_cell1">
            <div class="netgrid_celltitles" id="vm_snapshot_name">
            </div>
        </div>
        <div class="snap_row_cell2">
            <div class="netgrid_celltitles">
                <a href="#" id="vm_snapshot_action_take">Take Snapshots </a>
            </div>
        </div>
        <div class="snap_row_cell3">
            <div class="netgrid_celltitles">
                <a href="#" id="vm_snapshot_action_schedule">Schedule Snapshots </a>
                <div id="vm_snapshot_action_schedule_enabled" class="snap_scheduleicon" style="display: none">
                </div>
            </div>
        </div>
    </div>
</div>
<!-- Schedule Snapshots Dialog -->
<div id="dialog_schedule_snapshots" title="Schedule Snapshots" style="display: none">
    <p>
        To schedule snapshots for your <b><span id="snapshot_vm_name"></span></b>, please
        select an interval to take the snapshots and the maximum number of snapshots to
        retain for that interval. If you specify 0 as a value, that interval will be disabled.</p>
    <div class="dialog_snapshotformcontent">
        <form action="#" method="post" id="form_acquire">
        <div class="frequency_left">
            <h2>
                Frequency</h2>
        </div>
        <div class="retention_right">
            <h2>
                Retention</h2>
        </div>
        <ol>
            <div class="frequency_left">
                <li>
                    <input class="checkbox" type="checkbox" name="schedule_snapshot_hourly_enable" id="schedule_snapshot_hourly_enable" />
                    <label for="user_name">
                        Hourly</label>
                </li>
            </div>
            <div class="retention_right">
                <li>
                    <label for="user_name">
                        Retain</label>
                    <input class="text" type="text" name="schedule_snapshot_hourly" id="schedule_snapshot_hourly"
                        value="24" style="width: 30px;" />
                    <span>snapshots</span>
                    <div id="schedule_snapshot_hourly_errormsg" class="dialog_formcontent_errormsg_long"
                        style="display: none;">
                    </div>
                </li>
            </div>
            <div class="frequency_left">
                <li>
                    <input class="checkbox" type="checkbox" name="schedule_snapshot_daily_enable" id="schedule_snapshot_daily_enable" />
                    <label for="user_name">
                        Daily</label>
                </li>
            </div>
            <div class="retention_right">
                <li>
                    <label for="user_name">
                        Retain</label>
                    <input class="text" type="text" name="schedule_snapshot_daily" id="schedule_snapshot_daily"
                        value="7" style="width: 30px;" />
                    <span>snapshots</span>
                    <div id="schedule_snapshot_daily_errormsg" class="dialog_formcontent_errormsg_long"
                        style="display: none;">
                    </div>
                </li>
            </div>
            <div class="frequency_left">
                <li>
                    <input class="checkbox" type="checkbox" name="schedule_snapshot_weekly_enable" id="schedule_snapshot_weekly_enable" />
                    <label for="user_name">
                        Weekly</label></li>
            </div>
            <div class="retention_right">
                <li>
                    <label for="user_name">
                        Retain</label>
                    <input class="text" type="text" name="schedule_snapshot_weekly" id="schedule_snapshot_weekly"
                        value="4" style="width: 30px;" />
                    <span>snapshots </span>
                    <div id="schedule_snapshot_weekly_errormsg" class="dialog_formcontent_errormsg_long"
                        style="display: none;">
                    </div>
                </li>
            </div>
            <div class="frequency_left">
                <li>
                    <input class="checkbox" type="checkbox" name="schedule_snapshot_monthly_enable" id="schedule_snapshot_monthly_enable" />
                    <label for="user_name">
                        Monthly</label>
                </li>
            </div>
            <div class="retention_right">
                <li>
                    <label for="user_name">
                        Retain</label>
                    <input class="text" type="text" name="schedule_snapshot_monthly" id="schedule_snapshot_monthly"
                        value="12" style="width: 30px;" />
                    <span>snapshots </span>
                    <div id="schedule_snapshot_monthly_errormsg" class="dialog_formcontent_errormsg_long"
                        style="display: none;">
                    </div>
                </li>
            </div>
        </ol>
        </form>
    </div>
</div>
<!-- Rollback Snapshot Dialog -->
<div id="dialog_rollback_snapshot" title="Rollback Snapshot" style="display: none">
    <p>
        Please confirm you want to rollback to the following snapshot: <b><span id="snapshot_name"></span>
        </b>for the Virtual Instance: <b><span id="snapshot_vm_name"></span></b>
    </p>
    <div class="selectable_errorbox" style="width: 375px; display: none">
        <p>
            <strong>Alert</strong>: <span id="apply_error_text"></span>
        </p>
    </div>
    <div class="selectable_commentbox" style="width: 375px; display: none">
        <div class="selectable_loader">
        </div>
        <p>
            <strong>Please wait while we rollback to your specified snapshot...</strong></p>
    </div>
</div>
<!-- Routers-->
<div class="maincontent" style="display: none;" id="submenu_content_routers">
    <div id="maincontent_title">
        <div class="maintitle_icon">
            <img src="images/routerstitle_icons.gif" title="routers" />
        </div>
        <h1>
            Routers</h1>
        <div class="search_formarea">
            <form action="#" method="post">
            <ol>
                <li>
                    <input class="text" type="text" name="search_input" id="search_input" /></li>
            </ol>
            </form>
            <a class="search_button" id="search_button" href="#"></a>
            <div id="advanced_search_link" class="advsearch_link">
                Advanced</div>
            <div id="advanced_search" class="adv_searchpopup" style="display: none;">
                <div class="adv_searchformbox">
                    <h3>
                        Advance Search</h3>
                    <a id="advanced_search_close" href="#">Close </a>
                    <form action="#" method="post">
                    <ol>
                        <li>
                            <label for="filter">
                                Name:</label>
                            <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
                        </li>
                        <li>
                            <label for="filter">
                                State:</label>
                            <select class="select" id="adv_search_state">
                                <option value=""></option>
                                <option value="Creating">Creating</option>
                                <option value="Starting">Starting</option>
                                <option value="Running">Running</option>
                                <option value="Stopping">Stopping</option>
                                <option value="Stopped">Stopped</option>
                                <option value="Destroyed">Destroyed</option>
                                <option value="Expunging">Expunging</option>
                                <option value="Migrating">Migrating</option>
                                <option value="Error">Error</option>
                                <option value="Unknown">Unknown</option>
                            </select>
                        </li>
                        <li>
                            <label for="filter">
                                Zone:</label>
                            <select class="select" id="adv_search_zone">
                            </select>
                        </li>
                        <li id="adv_search_pod_li" style="display: none;">
                            <label for="filter">
                                Pod:</label>
                            <select class="select" id="adv_search_pod">
                            </select>
                        </li>
                        <li id="adv_search_domain_li" style="display: none;">
                            <label for="filter">
                                Domain:</label>
                            <select class="select" id="adv_search_domain">
                            </select>
                        </li>
                        <li id="adv_search_account_li" style="display: none;">
                            <label for="filter">
                                Account:</label>
                            <input class="text" type="text" id="adv_search_account" />
                        </li>
                    </ol>
                    </form>
                    <div class="adv_search_actionbox">
                        <div class="adv_searchpopup_button" id="adv_search_button">
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="filter_actionbox">
        <div class="selection_formarea" style="display: none;">
            <form action="#" method="post">
            <label for="filter">
                Filters:</label>
            <select class="select" id="template_type">
                <option value="true">Public</option>
                <option value="false">Private</option>
            </select>
            </form>
        </div>
    </div>
    <div class="grid_container">
        <div id="loading_gridtable" class="loading_gridtable">
            <div class="loading_gridanimation">
            </div>
            <p>
                Loading...</p>
        </div>
        <div class="grid_header">
            <div class="grid_genheader_cell" style="width: 6%;">
                <div class="grid_headertitles">
                </div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Zone</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Name</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Public IP</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Private IP</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Guest IP</div>
            </div>
            <div class="grid_genheader_cell" style="width: 12%;">
                <div class="grid_headertitles">
                    Host</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Network Domain</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Account</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Created</div>
            </div>
        </div>
        <div id="grid_content">
        </div>
    </div>
    <div id="pagination_panel" class="pagination_panel" style="display: none;">
        <p id="grid_rows_total" />
        <div class="pagination_actionbox">
            <div class="pagination_actions">
                <div class="pagination_actionicon">
                    <img src="images/pagination_refresh.gif" title="refresh" /></div>
                <a id="refresh" href="#">Refresh</a>
            </div>
            <div class="pagination_actions" id="prevPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_previcon.gif" title="prev" /></div>
                <a id="prevPage" href="#">Prev</a>
            </div>
            <div class="pagination_actions" id="nextPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_nexticon.gif" title="next" /></div>
                <a id="nextPage" href="#">Next</a>
            </div>
        </div>
    </div>
</div>
<!-- END Content Panel -->
<!-- Router Template -->
<div id="router_template" style="display: none">
    <div class="green_statusbar" id="router_state_bar">
    </div>
    <div class="grid_genrow_cell" style="width: 5%;">
        <div class="grid_runningtitles" id="router_state">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 10%;">
        <div class="grid_celltitles" id="router_zonename">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 10%;">
        <div class="grid_celltitles" id="router_name">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 10%;">
        <div class="grid_celltitles" id="router_public_ip">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 10%;">
        <div class="grid_celltitles" id="router_private_ip">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 10%;">
        <div class="grid_celltitles" id="router_guest_ip">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 12%;">
        <div class="grid_celltitles" id="router_host">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 10%;">
        <div class="grid_celltitles" id="router_domain">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 10%;">
        <div class="grid_celltitles" id="router_owner">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 10%;">
        <div class="grid_celltitles" id="router_created">
        </div>
    </div>
    <div id="grid_links_container" style="display: none;">
        <div class="grid_links">
            <div id="router_action_start_container" style="float: left;">
                <a id="router_action_start" href="#">Start</a> |</div>
            <div id="router_action_stop_container" style="float: left;">
                <a id="router_action_stop" href="#">Stop</a> |</div>
            <div id="router_action_reboot_container" style="float: left;">
                <a id="router_action_reboot" href="#">Reboot</a> |</div>
            <div id="router_action_view_console_container" style="float: left;">
                <a id="router_action_view_console" href="#">View Console</a> |</div>
        </div>
    </div>
    <div class="row_loading" style="display: none; height: 58px;">
    </div>
    <div class="loading_animationcontainer" style="display: none;">
        <div class="loading_animationtext">
        </div>
        <div class="loading_animation">
        </div>
    </div>
    <div class="loadingmessage_container" style="display: none;">
        <div class="loadingmessage_top">
            <p>
            </p>
        </div>
        <div class="loadingmessage_bottom">
            <a class="continue_button" href="#"></a>
        </div>
    </div>
</div>
<!-- END Router Template -->
<!-- System-->
<div class="maincontent" style="display: none;" id="submenu_content_console">
    <div id="maincontent_title">
        <div class="maintitle_icon">
            <img src="images/cproxytitle_icons.gif" title="routers" />
        </div>
        <h1>
            System</h1>
        <div class="search_formarea">
            <form action="#" method="post">
            <ol>
                <li>
                    <input class="text" type="text" name="search_input" id="search_input" /></li>
            </ol>
            </form>
            <a class="search_button" id="search_button" href="#"></a>
            <div id="advanced_search_link" class="advsearch_link">
                Advanced</div>
            <div id="advanced_search" class="adv_searchpopup" style="display: none;">
                <div class="adv_searchformbox">
                    <h3>
                        Advance Search</h3>
                    <a id="advanced_search_close" href="#">Close </a>
                    <form action="#" method="post">
                    <ol>
                        <li>
                            <label for="filter">
                                Name:</label>
                            <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
                        </li>
                        <li>
                            <label for="filter">
                                State:</label>
                            <select class="select" id="adv_search_state">
                                <option value=""></option>
                                <option value="Creating">Creating</option>
                                <option value="Starting">Starting</option>
                                <option value="Running">Running</option>
                                <option value="Stopping">Stopping</option>
                                <option value="Stopped">Stopped</option>
                                <option value="Destroyed">Destroyed</option>
                                <option value="Expunging">Expunging</option>
                                <option value="Migrating">Migrating</option>
                                <option value="Error">Error</option>
                                <option value="Unknown">Unknown</option>
                            </select>
                        </li>
                        <li>
                            <label for="filter">
                                Zone:</label>
                            <select class="select" id="adv_search_zone">
                            </select>
                        </li>
                        <li id="adv_search_pod_li" style="display: none;">
                            <label for="filter">
                                Pod:</label>
                            <select class="select" id="adv_search_pod">
                            </select>
                        </li>
                        <li id="adv_search_domain_li" style="display: none;">
                            <label for="filter">
                                Domain:</label>
                            <select class="select" id="adv_search_domain">
                            </select>
                        </li>
                    </ol>
                    </form>
                    <div class="adv_search_actionbox">
                        <div class="adv_searchpopup_button" id="adv_search_button">
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="filter_actionbox">
        <div class="selection_formarea" style="display: none;">
            <form action="#" method="post">
            <label for="filter">
                Filters:</label>
            <select class="select" id="template_type">
                <option value="true">Public</option>
                <option value="false">Private</option>
            </select>
            </form>
        </div>
    </div>
    <div class="grid_container">
        <div id="loading_gridtable" class="loading_gridtable" style="display: none;">
            <div class="loading_gridanimation">
            </div>
            <p>
                Loading...</p>
        </div>
        <div class="grid_header">
            <div class="grid_genheader_cell" style="width: 7%;">
                <div class="grid_headertitles">
                	Status
                </div>
            </div>
            <div class="grid_genheader_cell" style="width: 11%;">
                <div class="grid_headertitles">
                    Type</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Zone</div>
            </div>
            <div class="grid_genheader_cell" style="width: 9%;">
                <div class="grid_headertitles">
                    Name</div>
            </div>
            <div class="grid_genheader_cell" style="width: 9%;">
                <div class="grid_headertitles">
                    Active Sessions</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Public IP</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Private IP</div>
            </div>
            <div class="grid_genheader_cell" style="width: 12%;">
                <div class="grid_headertitles">
                    Host</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Gateway</div>
            </div>
            <div class="grid_genheader_cell" style="width: 11%; border:0;">
                <div class="grid_headertitles">
                    Created</div>
            </div>
        </div>
        <div id="grid_content">
        </div>
    </div>
    <div id="pagination_panel" class="pagination_panel">
        <p id="grid_rows_total" />
        <div class="pagination_actionbox">
            <div class="pagination_actions">
                <div class="pagination_actionicon">
                    <img src="images/pagination_refresh.gif" title="refresh" /></div>
                <a id="refresh" href="#">Refresh</a>
            </div>
            <div class="pagination_actions" id="prevPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_previcon.gif" title="prev" /></div>
                <a id="prevPage" href="#">Prev</a>
            </div>
            <div class="pagination_actions" id="nextPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_nexticon.gif" title="next" /></div>
                <a id="nextPage" href="#">Next</a>
            </div>
        </div>
    </div>
</div>
<!-- END Content Panel -->
<!-- System VM Template (begin) -->
<div id="console_template" style="display: none">
    <div class="green_statusbar" id="console_state_bar">
    </div>
    <div class="grid_genrow_cell" style="width: 6%;">
        <div class="grid_runningtitles" id="console_state">
            State</div>
    </div>
    <div class="grid_genrow_cell" style="width: 11%;">
        <div class="grid_celltitles" id="console_type">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 10%;">
        <div class="grid_celltitles" id="console_zone">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 9%;">
        <div class="grid_celltitles" id="console_name">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 9%;">
        <div class="grid_celltitles" id="console_active_session">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 10%;">
        <div class="grid_celltitles" id="console_public_ip">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 10%;">
        <div class="grid_celltitles" id="console_private_ip">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 12%;">
        <div class="grid_celltitles" id="console_host">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 10%;">
        <div class="grid_celltitles" id="console_gateway">
        </div>
    </div>
    <div class="grid_genrow_cell" style="width: 11%; border:0;">
        <div class="grid_celltitles" id="console_created">
        </div>
    </div>
    <div id="grid_links_container" style="display: none;">
        <div class="grid_links">
            <div id="console_action_start_container" style="float: left;">
                <a id="console_action_start" href="#">Start</a> |</div>
            <div id="console_action_stop_container" style="float: left;">
                <a id="console_action_stop" href="#">Stop</a> |</div>
            <div id="console_action_reboot_container" style="float: left;">
                <a id="console_action_reboot" href="#">Reboot</a> |</div>
            <div id="console_action_view_console_container" style="float: left;">
                <a id="console_action_view_console" href="#">View Console</a> |</div>
        </div>
    </div>
    <div class="row_loading" style="display: none; height: 58px;">
    </div>
    <div class="loading_animationcontainer" style="display: none;">
        <div class="loading_animationtext">
        </div>
        <div class="loading_animation">
        </div>
    </div>
    <div class="loadingmessage_container" style="display: none;">
        <div class="loadingmessage_top">
            <p>
            </p>
        </div>
        <div class="loadingmessage_bottom">
            <a class="continue_button" href="#"></a>
        </div>
    </div>
</div>
<!-- System VM Template (end) -->
<!--
	Terms and Conditions Dialog 
	****  Please change this accordingly ****
-->
<div id="dialog_t_and_c" title="Terms and Conditions" style="display:none">
    <p>
        <b>Terms and Conditions</b><br />
        </br> Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor
        incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
        exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute
        irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
        pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia
        deserunt mollit anim id est laborum.
    </p>
</div>
