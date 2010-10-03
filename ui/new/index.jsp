<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

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

    <script type="text/javascript" src="scripts/cloud.core2.js?t=<%=milliseconds%>"></script>

    <script type="text/javascript" src="scripts/cloud.core2.init.js?t=<%=milliseconds%>"></script>
    
    <script type="text/javascript" src="scripts/cloud.core2.instance.js?t=<%=milliseconds%>"></script>
    
    <script type="text/javascript" src="scripts/cloud.core2.event.js?t=<%=milliseconds%>"></script>
    
    <script type="text/javascript" src="scripts/cloud.core2.alert.js?t=<%=milliseconds%>"></script>
  
    <script type="text/javascript" src="scripts/cloud.core2.account.js?t=<%=milliseconds%>"></script>
    
    <script type="text/javascript" src="scripts/cloud.core2.volume.js?t=<%=milliseconds%>"></script>
    
    <script type="text/javascript" src="scripts/cloud.core2.snapshot.js?t=<%=milliseconds%>"></script>
    
    <script type="text/javascript" src="scripts/cloud.core2.ipaddress.js?t=<%=milliseconds%>"></script>
  
    <script type="text/javascript" src="scripts/cloud.core2.template.js?t=<%=milliseconds%>"></script>
  
    <script type="text/javascript" src="scripts/cloud.core2.iso.js?t=<%=milliseconds%>"></script>
  
    <script type="text/javascript" src="scripts/cloud.core2.router.js?t=<%=milliseconds%>"></script>
    
    <script type="text/javascript" src="scripts/cloud.core2.dashboard.js?t=<%=milliseconds%>"></script>
  
    <script type="text/javascript" src="scripts/cloud.core2.domain.js?t=<%=milliseconds%>"></script>
  
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
                        <div class="searchpanel" id="search_panel">
                            <form method="post" action="#">
                            <ol>
                                <li>
                                    <div class="search_textbg">
                                        <input class="text" type="text" name="search_input" />
                                        <div class="search_closebutton" style="display:block;"></div>
                                    </div>
                                </li>
                            </ol>
                            </form>
                            <a href="#">Advanced</a>
                            <div class="adv_searchpopup" id="adv_search_dialog" style="display: none;">
                                <div class="adv_searchformbox">
                                    <h3>
                                        Advance Search</h3>
                                    <a id="advanced_search_close" href="#">Close </a>
                                    <form action="#"method="post">
                                        <ol style="margin-top:8px;">
                                            <li>
                                                <label for="filter">
                                                    Name:</label>
                                                <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
                                            </li>
                                            <li>
                                                <label for="filter">
                                                    Status:</label>
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
                                           
                                        </ol>
                                    </form>
                                    <div class="adv_search_actionbox">
                                        <div class="adv_searchpopup_button" id="adv_search_button">
                                        </div>
                                    </div>
                                </div>
                        	</div>
                        </div>
                        
                        <div class="actionpanel_button_wrapper" id="midmenu_action_link" style="position: relative;
                            display: none">
                          <div class="actionpanel_button">
                                <div class="actionpanel_button_icons">
                                    <img src="images/actions_actionicon.png" alt="Add" /></div>
                                <div class="actionpanel_button_links">
                                    Actions</div>
                                <div class="action_ddarrow">
                                </div>
                            </div>
                            <div class="actionsdropdown_box" id="action_menu" style="display: none;">
                                <ul class="actionsdropdown_boxlist" style="width:97px;"id="action_list">
                                	
                                </ul>
                            </div>
                        </div>
                        <div class="actionpanel_button_wrapper" id="midmenu_add_link" style="display: none">
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
                    <div class="midmenu_navigationbox" id="middle_menu_pagination">
                        <div class="midmenu_prevbutton">
                        </div>
                        <div class="midmenu_nextbutton">
                        </div>
                    </div>
                    <!-- Right Panel ends here-->
                </div>
                <!-- Mid Menu starts here-->
                <div class="midmenu_panel" id="middle_menu">
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
                                <img src="images/leftmenu_dashboardicon.png" alt="Dashboard" /></div>
                            Dashboard
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content" id="leftmenu_dashboard">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/leftmenu_dashboardicon.png" alt="Dashboard" /></div>
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
                                <img src="images/leftmenu_hosticon.png" alt="Host" /></div>
                            Hosts
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content" id="leftmenu_host">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/leftmenu_hosticon.png" alt="Host" /></div>
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
                                <img src="images/leftmenu_volumeicon.png" alt="storage" /></div>
                            Volumes
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_snapshot">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/leftmenu_snapshotsicon.png" alt="storage" /></div>
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
                                <img src="images/leftmenu_networkgroupicon.png" alt="Network" /></div>
                            Network
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content" id="leftmenu_ip">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/leftmenu_ipaddressicon.png" alt="Network" /></div>
                            IP Addresses
                        </div>
                    </div>
                    <div class="leftmenu_content" id="leftmenu_network_group">
                        <div class="leftmenu_secondindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/leftmenu_networkgroupicon.png" alt="Network" /></div>
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
                    <div id="leftmenu_itemplate_filter">    
                        <div class="leftmenu_content" id="leftmenu_template_filter_header">
                            <div class="leftmenu_secondindent">                                
                                <div class="leftmenu_list_icons">
                                    <img src="images/templates_leftmenuicon.png" alt="Templates" /></div>
                                Template
                            </div>
                        </div>
                        <div id="leftmenu_template_filter_container">                        
                            <div class="leftmenu_content" id="leftmenu_submenu_my_template">
						        <div class="leftmenu_thirdindent">
						            <div class="leftmenu_list_icons">
						                <img src="images/templates_leftmenuicon.png" /></div>
						            <div>
						                My Templates</div>
						        </div>
						    </div>                        
                            <div class="leftmenu_content" id="leftmenu_submenu_featured_template">
						        <div class="leftmenu_thirdindent">
						            <div class="leftmenu_list_icons">
						                <img src="images/templates_leftmenuicon.png" /></div>
						            <div>
						                Featured</div>
						        </div>
						    </div>                        
                            <div class="leftmenu_content" id="leftmenu_submenu_community_template">
						        <div class="leftmenu_thirdindent">
						            <div class="leftmenu_list_icons">
						                <img src="images/templates_leftmenuicon.png" /></div>
						            <div>
						                Community</div>
						        </div>
						    </div>                        
                        </div>
                    </div>
                    <div id="leftmenu_iso_filter">    
                        <div class="leftmenu_content" id="leftmenu_iso_filter_header">
                            <div class="leftmenu_secondindent">                                
                                <div class="leftmenu_list_icons">
                                    <img src="images/templates_leftmenuicon.png" alt="Templates" /></div>
                                ISO
                            </div>
                        </div>
                        <div id="leftmenu_iso_filter_container">
                             <div class="leftmenu_content" id="leftmenu_submenu_my_iso">
						        <div class="leftmenu_thirdindent">
						            <div class="leftmenu_list_icons">
						                <img src="images/templates_leftmenuicon.png" /></div>
						            <div>
						                My ISOs</div>
						        </div>
						    </div>                        
                            <div class="leftmenu_content" id="leftmenu_submenu_featured_iso">
						        <div class="leftmenu_thirdindent">
						            <div class="leftmenu_list_icons">
						                <img src="images/templates_leftmenuicon.png" /></div>
						            <div>
						                Featured</div>
						        </div>
						    </div>                        
                            <div class="leftmenu_content" id="leftmenu_submenu_community_iso">
						        <div class="leftmenu_thirdindent">
						            <div class="leftmenu_list_icons">
						                <img src="images/templates_leftmenuicon.png" /></div>
						            <div>
						                Community</div>
						        </div>
						    </div>                                         
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
	<div id="dialog_error" title="Error" style="display:none;color:red"></div>
	<div id="dialog_session_expired" title="Session Expired" style="display:none">
		<p>
		    <%=t.t("your.session.has.expired")%>		    
		</p>
	</div>
	
	<div id="dialog_error_internet_not_resolved" title="Error" style="display:none">
	    <p style="color:red">
	        <%=t.t("internet.name.can.not.be.resolved")%>	        
	    </p>
	</div>
	
	<div id="dialog_error_management_server_not_accessible" title="Error" style="display:none">
	    <p style="color:red">
	        <%=t.t("management.server.is.not.accessible")%>	        
	     </p>
	</div>
	
	
    <!-- templates starts here-->
    <div class="leftmenu_content" id="leftmenu_submenu_template" style="display: none">
        <div class="leftmenu_thirdindent">
            <div class="leftmenu_list_icons">
                <img id="icon" style="display:none"/></div>
            <div id="submenu_name">
                (submenu)</div>
        </div>
    </div>
    
    <div class="midmenu_list" id="midmenu_item" style="display: none;">
        <div class="midmenu_content" id="content">
            <div class="midmenu_icons" id="icon_container" style="display:none">
                <img id="icon"/></div>
            <div class="midmenu_textbox">
                <p>
                    <strong id="first_row">&nbsp;</strong>
                </p>
                <p id="second_row_container">                    
                    <span id="second_row">&nbsp;</span>
                </p>
            </div>
            <div class="midmenu_inactionloader" id="spinning_wheel" style="display: none;">
            </div>
            <div class="midmenu_infoicon" id="info_icon" style="display: none;">
            </div>
        </div>
    </div>
   
    <!-- action list item for middle menu -->
    <li id="action_list_item_middle_menu" style="display:none; width:94px;"><a id="link" href="#">(middle menu action)</a></li>
    
    <!-- action list item for details tab, subgrid item-->
    <li id="action_list_item" style="display:none;"><a id="link" href="#">(action)</a></li>
    
    <!-- templates ends here-->
</body>
</html>
