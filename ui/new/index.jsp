<%@ page import="java.util.Date" %>

<%
long milliseconds = new Date().getTime();
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta http-equiv='cache-control' content='no-cache'>
    <meta http-equiv='expires' content='0'>
    <meta http-equiv='pragma' content='no-cache'>
    <meta name="version" content="1.9.1.2010-08-25T16:16:56Z" />
    <link rel="stylesheet" href="css/main.css" type="text/css" />
    <link rel="stylesheet" href="css/jquery-ui-1.8.2.custom.css" type="text/css" />
    <link rel="stylesheet" href="css/logger.css" type="text/css" />

    <script type="text/javascript" src="scripts/jquery-1.4.2.min.js"></script>

    <script type="text/javascript" src="scripts/jquery-ui-1.8.2.custom.min.js"></script>

    <script type="text/javascript" src="scripts/date.js"></script>

    <script type="text/javascript" src="scripts/jquery.cookies.js"></script>

    <script type="text/javascript" src="scripts/jquery.timers.js"></script>

    <script type="text/javascript" src="scripts/jquery.md5.js"></script>

    <!-- cloud.com scripts -->

    <script type="text/javascript" src="scripts/cloud.logger.js?t=<%=milliseconds%>"></script>

    <script type="text/javascript" src="scripts/cloud.core.js?t=<%=milliseconds%>"></script>

    <script type="text/javascript" src="scripts/cloud.core.init.js?t=<%=milliseconds%>"></script>
    
    <script type="text/javascript" src="scripts/cloud.core.instance.js?t=<%=milliseconds%>"></script>
    
    <script type="text/javascript" src="scripts/cloud.core.event.js?t=<%=milliseconds%>"></script>
    
    <script type="text/javascript" src="scripts/cloud.core.alert.js?t=<%=milliseconds%>"></script>
  
    <script type="text/javascript" src="scripts/cloud.core.account.js?t=<%=milliseconds%>"></script>
    
    <script type="text/javascript" src="scripts/cloud.core.volume.js?t=<%=milliseconds%>"></script>
    
    <script type="text/javascript" src="scripts/cloud.core.snapshot.js?t=<%=milliseconds%>"></script>
  
    <title>Cloud.com CloudStack</title>
</head>
<body>
    <div id="overlay_black" style="display: none">
    </div>
    
    <div id="main">
        <div id="header">
            <div class="header_left">
                <div class="logo">
                </div>
                <div class="mgmtconsole_logo">
                </div>
            </div>
            <div class="header_right">
                <div class="userlinks">
                    <p>
                        Welcome <span>John</span>, <a href="#">Logout</a>
                    </p>
                </div>
            </div>
        </div>
        <div id="main_contentpanel">
            <div class="right_panel">
                <div id="contentwrapper">
                    <!-- Action Panel starts here-->
                    <div class="actionpanel">
                        <div class="searchpanel">
                            <form method="post" action="#">
                            <ol>
                                <li>
                                    <div class="search_textbg">
                                        <input class="text" type="text" name="search_input" />
                                        <div class="search_closebutton" style="display:none;"></div>
                                    </div>
                                </li>
                            </ol>
                            </form>
                            <a href="#">Advanced</a>
                        </div>
                        <div class="actionpanel_button_wrapper" id="action_link" style="position: relative;
                            display: none">
                            <div class="actionpanel_button">
                                <div class="actionpanel_button_icons">
                                    <img src="images/actions_actionicon.png" alt="Add VM" /></div>
                                <div class="actionpanel_button_links">
                                    Actions</div>
                                <div class="action_ddarrow">
                                </div>
                            </div>
                            <div class="actionsdropdown_box" id="action_menu" style="display: none;">
                                <ul class="actionsdropdown_boxlist" id="action_list">
                                </ul>
                            </div>
                        </div>
                        <div class="actionpanel_button_wrapper" id="add_link" style="display: none">
                            <div class="actionpanel_button">
                                <div class="actionpanel_button_icons">
                                    <img src="images/addvm_actionicon.png" alt="Add" /></div>
                                <div class="actionpanel_button_links">
                                    Add</div>
                            </div>
                        </div>
                        
                        <div class="actionpanel_button_wrapper" id="add_link" style="display: block; float:right; background:none; ">
                            <div class="actionpanel_button">
                                <div class="actionpanel_button_icons">
                                    <img src="images/help_actionicon.png" alt="Add" /></div>
                                <div class="actionpanel_button_links">
                                    Help</div>
                            </div>
                        </div>
                        
                    </div>
                    <!-- Action Panel ends here-->
                    <!-- Right Panel starts here-->
                    <div class="main_contentarea" id="right_panel">
                        <!--
						<div class="main_title">
                            <div class="main_titleicon"><img src="images/instancetitle_icons.gif" alt="Instance" /></div>
                             <h1>Name of the Instance Selected.. </h1>
                        </div>
                        <div class="contentbox">
                        	<div class="tabbox">
                            	<div class="content_tabs on">Details</div>
                                <div class="content_tabs off">Volume</div>
                                <div class="content_tabs off">Statistics</div>
                            </div>
                            
                            <div class="grid_container">
                            	<div class="grid_rows odd">
                                	<div class="vm_statusbox">
                                    	<div class="vm_consolebox">
                                        	<div class="vm_liveconsole"></div>
                                        </div>
                                        <div class="vm_status_textbox">
                                        	<div class="vm_status_textline green">Running</div><br />
                                            <p>10.1.1.200</p>
                                        </div>
                                    </div>
                                </div>
                                
                                <div class="grid_rows even">
                                	<div class="grid_row_cell" style="width:20%;">
                                    	<div class="row_celltitles">Zone:</div>
                                    </div>
                                    
                                    <div class="grid_row_cell" style="width:79%;">
                                    	<div class="row_celltitles">JW</div>
                                    </div>
                                </div>
                                
                                <div class="grid_rows odd">
                                	<div class="grid_row_cell" style="width:20%;">
                                    	<div class="row_celltitles">Template:</div>
                                    </div>
                                    
                                    <div class="grid_row_cell" style="width:79%;">
                                    	<div class="row_celltitles">Centos 5.3(x36.4) no GUI</div>
                                    </div>
                                </div>
                                
                                <div class="grid_rows even">
                                	<div class="grid_row_cell" style="width:20%;">
                                    	<div class="row_celltitles">Service:</div>
                                    </div>
                                    
                                    <div class="grid_row_cell" style="width:79%;">
                                    	<div class="row_celltitles">Small Instance</div>
                                    </div>
                                </div>
                                
                                <div class="grid_rows odd">
                                	<div class="grid_row_cell" style="width:20%;">
                                    	<div class="row_celltitles">HA:</div>
                                    </div>
                                    
                                    <div class="grid_row_cell" style="width:79%;">
                                    	<div class="row_celltitles"><div class="tick_icon"></div></div>
                                    </div>
                                </div>
                                
                                 <div class="grid_rows even">
                                	<div class="grid_row_cell" style="width:20%;">
                                    	<div class="row_celltitles">Created:</div>
                                    </div>
                                    
                                    <div class="grid_row_cell" style="width:79%;">
                                    	<div class="row_celltitles">07/20/2010 11:29:04  </div>
                                    </div>
                                </div>
                                
                                <div class="grid_rows odd">
                                	<div class="grid_row_cell" style="width:20%;">
                                    	<div class="row_celltitles">Account:</div>
                                    </div>
                                    
                                    <div class="grid_row_cell" style="width:79%;">
                                    	<div class="row_celltitles">Admin</div>
                                    </div>
                                </div>
                                
                                <div class="grid_rows odd">
                                	<div class="grid_row_cell" style="width:20%;">
                                    	<div class="row_celltitles">Domain:</div>
                                    </div>
                                    
                                    <div class="grid_row_cell" style="width:79%;">
                                    	<div class="row_celltitles">ROOT</div>
                                    </div>
                                </div>
                                
                                 <div class="grid_rows even">
                                	<div class="grid_row_cell" style="width:20%;">
                                    	<div class="row_celltitles">Host:</div>
                                    </div>
                                    
                                    <div class="grid_row_cell" style="width:79%;">
                                    	<div class="row_celltitles">Xenserver-test5 </div>
                                    </div>
                                </div>
                                
                                   <div class="grid_rows odd">
                                	<div class="grid_row_cell" style="width:20%;">
                                    	<div class="row_celltitles">ISO:</div>
                                    </div>
                                    
                                    <div class="grid_row_cell" style="width:79%;">
                                    	<div class="row_celltitles"><div class="cross_icon"></div></div>
                                    </div>
                                </div>
                                
                            </div>
                        </div>
                        -->
                    </div>
                    <div class="midmenu_navigationbox">
                        <div class="midmenu_prevbutton">
                        </div>
                        <div class="midmenu_nextbutton">
                        </div>
                    </div>
                    <!-- Right Panel ends here-->
                </div>
                <!-- Mid Menu starts here-->
                <div class="midmenu_panel">
                    <div class="midmenu_box" id="midmenu_container">
                    	
                        <!--  
                        	<div class="midmenu_list">
                            	<div class="midmenu_content">
                                	<div class="midmenu_icons"><img src="images/status_green.png" alt="Running" /></div>
                                    <div class="midmenu_textbox">
                                    	<p><strong>Instance 1</strong></p>
                                        <p>IP Address: 10.1.1.2</p>
                                    </div>
                                    <div class="midmenu_defaultloader"></div>
                                </div>
                            </div>
                       -->
                       <!--
                            <div class="midmenu_list">
                            	<div class="midmenu_content">
                                	<div class="midmenu_icons"><img src="images/status_red.png" alt="Running" /></div>
                                    <div class="midmenu_textbox">
                                    	<p><strong>Instance 2</strong></p>
                                        <p>IP Address: 10.1.1.2</p>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="midmenu_list">
                            	<div class="midmenu_content selected">
                                	<div class="midmenu_icons"><img src="images/status_gray.png" alt="Running" /></div>
                                    <div class="midmenu_textbox">
                                    	<p><strong>Instance 3</strong></p>
                                        <p>IP Address: 10.1.1.2</p>
                                    </div>
                                    <div class="midmenu_infoicon"></div>
                                </div>
                            </div>
                            -->
                    </div>
                </div>
                <!-- Mid Menu ends here-->
            </div>
        </div>
        <!-- Left Menu starts here-->
        <div class="leftmenu_panel">
            <div class="leftmenu_box" id="accordion_menu" style="display: none">
                <div class="leftmenu_list">
                    <div class="leftmenu_content">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows close" id="arrowIcon">
                            </div>
                            <div class="leftmenu_list_icons">
                                <img src="images/db_leftmenuicon.png" alt="Dashboard" /></div>
                            Dashboard
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content" id="leftmenu_dashboard">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/db_leftmenuicon.png" alt="Dashboard" /></div>
                            Dashboard
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows close" id="arrowIcon">
                            </div>
                            <div class="leftmenu_list_icons">
                                <img src="images/instance_leftmenuicon.png" alt="Instances" /></div>
                            Instances
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div id="leftmenu_instance_group">
                        <div class="leftmenu_content" id="leftmenu_instance_group_header">
                            <div class="leftmenu_secondindent">
                                <div class="leftmenu_arrows close" id="arrow_icon">
                                </div>
                                <div class="leftmenu_list_icons">
                                    <img src="images/instance_leftmenuicon.png" alt="Instances" /></div>
                                Instances
                            </div>
                        </div>
                        <div id="leftmenu_instance_group_container">
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_router">
                        <div class="leftmenu_secondindent">                           
                            <div class="leftmenu_list_icons">
                                <img src="images/routers_leftmenuicon.png" alt="Routers" /></div>
                            Routers
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_system">
                        <div class="leftmenu_secondindent">                           
                            <div class="leftmenu_list_icons">
                                <img src="images/storage_leftmenuicon.png" alt="Storage" /></div>
                            System
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows close" id="arrowIcon">
                            </div>
                            <div class="leftmenu_list_icons">
                                <img src="images/hosts_leftmenuicon.png" alt="Host" /></div>
                            Hosts
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content" id="leftmenu_host">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/hosts_leftmenuicon.png" alt="Host" /></div>
                            Hosts
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows close" id="arrowIcon">
                            </div>
                            <div class="leftmenu_list_icons">
                                <img src="images/storage_leftmenuicon.png" alt="storage" /></div>
                            Storage
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content" id="leftmenu_primary_storage">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/storage_leftmenuicon.png" alt="storage" /></div>
                            Primary Storage
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_secondary_storage">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/storage_leftmenuicon.png" alt="storage" /></div>
                            secondary Storage
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_volume">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/storage_leftmenuicon.png" alt="storage" /></div>
                            Volumes
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_snapshot">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/storage_leftmenuicon.png" alt="storage" /></div>
                            Snapshots
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows close" id="arrowIcon">
                            </div>
                            <div class="leftmenu_list_icons">
                                <img src="images/network_leftmenuicon.png" alt="Network" /></div>
                            Network
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content" id="leftmenu_ip">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/network_leftmenuicon.png" alt="Network" /></div>
                            IP Addresses
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_network_group">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/network_leftmenuicon.png" alt="Network" /></div>
                            Network Groups
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows close" id="arrowIcon">
                            </div>
                            <div class="leftmenu_list_icons">
                                <img src="images/templates_leftmenuicon.png" alt="Templates" /></div>
                            Templates
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content" id="leftmenu_template">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/templates_leftmenuicon.png" alt="Templates" /></div>
                            Template
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_iso">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/templates_leftmenuicon.png" alt="Templates" /></div>
                            ISO
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows close" id="arrowIcon">
                            </div>
                            <div class="leftmenu_list_icons">
                                <img src="images/accounts_leftmenuicon.png" alt="Accounts" /></div>
                            Accounts
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content" id="leftmenu_account">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/accounts_leftmenuicon.png" alt="Accounts" /></div>
                            Accounts
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows close" id="arrowIcon">
                            </div>
                            <div class="leftmenu_list_icons">
                                <img src="images/domain_leftmenuicon.png" alt="Domain" /></div>
                            Domain
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content" id="leftmenu_domain">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/domain_leftmenuicon.png" alt="Domain" /></div>
                            Domain
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows close" id="arrowIcon">
                            </div>
                            <div class="leftmenu_list_icons">
                                <img src="images/events_leftmenuicon.png" alt="Events" /></div>
                            Events
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content" id="leftmenu_event">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/events_leftmenuicon.png" alt="Events" /></div>
                            Events
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_alert">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/events_leftmenuicon.png" alt="Events" /></div>
                            Alerts
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows close" id="arrowIcon">
                            </div>
                            <div class="leftmenu_list_icons">
                                <img src="images/configuration_leftmenuicon.png" alt="Configuration" /></div>
                            Configuration
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content" id="leftmenu_global_setting">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/configuration_leftmenuicon.png" alt="Configuration" /></div>
                            Global Settings
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_zone">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/configuration_leftmenuicon.png" alt="Configuration" /></div>
                            Zones
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_service_offering">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/configuration_leftmenuicon.png" alt="Configuration" /></div>
                            Service Offerings
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_disk_offering">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/configuration_leftmenuicon.png" alt="Configuration" /></div>
                            Disk Offerings
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!-- Left Menu ends here-->
    </div>
    <div id="footer">
        <p>
            Version: <span>2.2</span>
        </p>
        <div class="poweredby_box">
        </div>
    </div>
   
    <!-- Dialogs -->
	<div id="dialog_confirmation" title="Confirmation" style="display:none"></div>
	<div id="dialog_info" title="Info" style="display:none"></div>
	<div id="dialog_alert" title="Alert" style="display:none"></div>
	<div id="dialog_error" title="Error" style="display:none"></div>
	<div id="dialog_session_expired" title="Session Expired" style="display:none">
		<p>Your session has expired.  Please click 'OK' to return to the login screen.</p>
	</div>
	
    <!-- templates starts here-->
    <div class="leftmenu_content" id="leftmenu_instance_group_template" style="display: none">
        <div class="leftmenu_thirdindent">
            <div class="leftmenu_list_icons">
                <img src="images/instance_leftmenuicon.png" alt="Instances" /></div>
            <div id="group_name">
                Group 1</div>
        </div>
    </div>
    <div class="midmenu_list" id="midmenu_item_vm" style="display: none;">
        <div class="midmenu_content" id="content">
            <div class="midmenu_icons" id="status_icon_container">
                <img id="status_icon" src="images/status_gray.png" /></div>
            <div class="midmenu_textbox">
                <p>
                    <strong id="vm_name"></strong>
                </p>
                <p id="ip_address_container">
                    <span id="label">IP Address:</span> <span id="ip_address"></span>
                </p>
            </div>
            <div class="midmenu_inactionloader" id="spinning_wheel" style="display: none;">
            </div>
            <div class="midmenu_infoicon" id="info_icon" style="display: none;">
            </div>
        </div>
    </div>
    
        <div class="midmenu_list" id="midmenu_item" style="display: none;">
        	<div class="midmenu_content" id="content">
                 <div class="midmenu_textbox">
                 	<p>
                    	<strong id="description"></strong>
                    </p>
                 </div>
                 <div class="midmenu_inactionloader" id="spinning_wheel" style="display: none; margin-left:18px; display:none;">
                 </div>
                 <div class="midmenu_infoicon" id="info_icon" style="display: none; margin-left:18px; display:none;">
                 </div>
            </div>
       </div>
    <!-- action list item -->
    <li id="action_list_item"><a id="link" href="#">Stop</a></li>
    <!-- templates ends here-->
</body>
</html>
