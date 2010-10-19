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
    <link rel="stylesheet" href="css/jquery-ui-1.8.2.custom.css" type="text/css" />
    <link rel="stylesheet" href="css/logger.css" type="text/css" />
    <link rel="stylesheet" href="css/main.css" type="text/css" />

    <script type="text/javascript" src="scripts/jquery-1.4.2.min.js"></script>

    <script type="text/javascript" src="scripts/jquery-ui-1.8.2.custom.min.js"></script>

    <script type="text/javascript" src="scripts/date.js"></script>

    <script type="text/javascript" src="scripts/jquery.cookies.js"></script>

    <script type="text/javascript" src="scripts/jquery.timers.js"></script>

    <script type="text/javascript" src="scripts/jquery.md5.js"></script>

    <!-- cloud.com scripts -->

    <script type="text/javascript" src="scripts/cloud.logger.js?t=<%=milliseconds%>"></script>
	
	<script type="text/javascript" src="scripts/cloud.core2.callbacks.js?t=<%=milliseconds%>"></script>

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

    <script type="text/javascript" src="scripts/cloud.core2.resource.js?t=<%=milliseconds%>"></script>

    <script type="text/javascript" src="scripts/cloud.core2.serviceoffering.js?t=<%=milliseconds%>"></script>

    <script type="text/javascript" src="scripts/cloud.core2.diskoffering.js?t=<%=milliseconds%>"></script>

    <script type="text/javascript" src="scripts/cloud.core2.globalsetting.js?t=<%=milliseconds%>"></script>

    <title>Cloud.com CloudStack</title>
</head>
<body>
	<!-- Main Login Dialog (begin)-->
	<div id="login_wrapper" style="display:none">
    	<div class="login_main">
        	<div class="login_logopanel">
            	<div class="login_logobox"></div>
            </div>
            <div class="main_loginbox">
            	<div class="main_loginbox_top"></div>
                <div class="main_loginbox_mid">
                	<div class="login_contentbox">
                    	<div class="login_contentbox_title">
                        	<h1>Welcome to Management Console &hellip;</h1>
                        </div>
                        
                        <div class="login_formbox">
                        	<form id="loginForm" action="#" method="post" name="loginForm">
                            	<ol>
                                	<li>
                                    	<label for="user_name">Username: </label>
                                        <div class="login_formbox_textbg">
                                        	<input id="account_username" class="text" type="text" name="account_username" AUTOCOMPLETE="off"/>
                                        </div>
                                    </li>
                                    
                                    <li>
                                    	<label for="user_name">Password: </label>
                                        <div class="login_formbox_textbg">
                                        	<input id="account_password" class="text" type="password" name="account_password" AUTOCOMPLETE="off"/>
                                        </div>
                                    </li>
                                    
                                    <li>
                                    	<label for="user_name">Domain: </label>
                                        <div class="login_formbox_textbg">
                                        	<input id="account_domain" class="text" type="text" name="account_domain" />
                                        </div>
                                    </li>
                                </ol>
                                <div class="loginbutton_box">
                                	<div class="login_button" id="loginbutton" >Login</div>
                                </div>
                            </form>
                            
                            <div class="error_box" id="login_error" style="display:none;">
                            	<p>Your username/password does not match our records.</p>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="main_loginbox_bot"></div>
            </div>
        </div>
    </div>
	<!-- Main Login Dialog (end)-->

	<!-- Main Console -->
    <div id="overlay_black" style="display: none">
    </div>
    <div id="main" style="display: none">
        <div id="main_header">
            <div class="header_left">
                <div class="logo">
                </div>
                <div class="mgmtconsole_logo">
                </div>
            </div>
            <div class="header_right">
                <div class="userlinks">
                    <p>
                        Welcome <span id="main_username">Anonymous</span>, <a href="#" id="main_logout">Logout</a>
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
                                        <div class="search_closebutton" style="display: block;">
                                        </div>
                                    </div>
                                </li>
                            </ol>
                            </form>
                            <a href="#">
                                <%=t.t("advanced")%></a>
                            <div class="adv_searchpopup" id="adv_search_dialog" style="display: none;">
                                <div class="adv_searchformbox">
                                    <h3>
                                        Advance Search</h3>
                                    <a id="advanced_search_close" href="#">Close </a>
                                    <form action="#" method="post">
                                    <ol style="margin-top: 8px;">
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
                                    <%=t.t("actions")%></div>
                                <div class="action_ddarrow">
                                </div>
                            </div>
                            <div class="actionsdropdown_box" id="action_menu" style="display: none;">
                                <ul class="actionsdropdown_boxlist" style="width: 97px;" id="action_list">
                                </ul>
                            </div>
                        </div>
                        <div class="actionpanel_button_wrapper" id="midmenu_add_link" style="display: none;">
                            <div class="actionpanel_button">
                                <div class="actionpanel_button_icons">
                                    <img src="images/addvm_actionicon.png" alt="Add" /></div>
                                <div class="actionpanel_button_links" id="label">
                                    <%=t.t("add")%></div>
                            </div>
                        </div>
                        <div class="actionpanel_button_wrapper" id="midmenu_add2_link" style="display: none;">
                            <div class="actionpanel_button">
                                <div class="actionpanel_button_icons">
                                    <img src="images/addvm_actionicon.png" alt="Add" /></div>
                                <div class="actionpanel_button_links" id="label">
                                    <%=t.t("add")%></div>
                            </div>
                        </div>
                        <div class="actionpanel_button_wrapper" id="midmenu_startvm_link" style="display: none;">
                            <div class="actionpanel_button">
                                <div class="actionpanel_button_icons">
                                    <img src="images/startvm_actionicon.png" alt="Start VM" /></div>
                                <div class="actionpanel_button_links">
                                    Start VM</div>
                            </div>
                        </div>
                        <div class="actionpanel_button_wrapper" id="midmenu_stopvm_link" style="display: none;">
                            <div class="actionpanel_button">
                                <div class="actionpanel_button_icons">
                                    <img src="images/stopvm_actionicon.png" alt="Stop VM" /></div>
                                <div class="actionpanel_button_links">
                                    Stop VM</div>
                            </div>
                        </div>
                        <div class="actionpanel_button_wrapper" id="midmenu_rebootvm_link" style="display: none;">
                            <div class="actionpanel_button">
                                <div class="actionpanel_button_icons">
                                    <img src="images/rebootvm_actionicon.png" alt="Reboot VM" /></div>
                                <div class="actionpanel_button_links">
                                    Reboot VM</div>
                            </div>
                        </div>
                        <div class="actionpanel_button_wrapper" id="midmenu_destoryvm_link" style="display: none;">
                            <div class="actionpanel_button">
                                <div class="actionpanel_button_icons">
                                    <img src="images/destroyvm_actionicon.png" alt="Destroy VM" /></div>
                                <div class="actionpanel_button_links">
                                    Destroy VM</div>
                            </div>
                        </div>
                        <div class="actionpanel_button_wrapper" id="help_link" style="display: block; float: right;
                            background: none;">
                            <div class="actionpanel_button">
                                <div class="actionpanel_button_icons">
                                    <img src="images/help_actionicon.png" alt="Help" /></div>
                                <div class="actionpanel_button_links">
                                    <%=t.t("help")%></div>
                            </div>
                        </div>
                    </div>
                    <!-- Action Panel ends here-->
                    <!-- Right Panel starts here-->
                    <div class="main_contentarea" id="right_panel">
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
                    </div>
                </div>
                <!-- Mid Menu ends here-->
            </div>
        </div>
        <!-- Left Menu starts here-->
        <div class="leftmenu_panel">
            <div class="leftmenu_box" id="leftmenu_container" style="display: none">     
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="leftmenu_dashboard">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/leftmenu_dashboardicon.png" alt="Dashboard" /></div>
                            <%=t.t("dashboard")%>
                        </div>
                    </div>
                    
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="expandable_first_level">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/instance_leftmenuicon.png" alt="Instance" /></div>
                            <%=t.t("instance")%>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                        <div id="leftmenu_instance_group">
                            <div class="leftmenu_expandedlist">
                                <div class="leftmenu_content" id="leftmenu_instance_group_header">
                                    <div class="leftmenu_secondindent">
                                        <div class="leftmenu_arrows expanded_close" id="arrow_icon">
                                        </div>
                                        
                                        <%=t.t("instance")%>
                                    </div>
                                </div>
                            </div>
                            <div id="leftmenu_instance_group_container">
                            </div>
                        </div>                        
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="expandable_first_level">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/storage_leftmenuicon.png" alt="Storage" /></div>
                            <%=t.t("storage")%>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_volume">
                                <div class="leftmenu_secondindent">
                                    
                                
                                   	<div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                      
                                    <%=t.t("volume")%>
                                </div>
                            </div>
                        </div>
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_snapshot">
                                <div class="leftmenu_secondindent">
                                	  <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                   
                                    <%=t.t("snapshot")%>
                                </div>
                            </div>
                        </div>
                    </div>
                </div> 
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="expandable_first_level">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/network_leftmenuicon.png" alt="Network" /></div>
                            <%=t.t("Network")%>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_ip">
                                <div class="leftmenu_secondindent">
                                   	<div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <%=t.t("ip.address")%>
                                </div>
                            </div>
                        </div>                        
                    </div>
                </div>  
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="expandable_first_level">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/templates_leftmenuicon.png" alt="Template" /></div>
                            <%=t.t("template")%>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                        <div id="leftmenu_itemplate_filter">
                            <div class="leftmenu_content" id="leftmenu_template_filter_header">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows expanded_open" id="arrowIcon">
                                    </div>
                                    
                                    <%=t.t("template")%>
                                </div>
                            </div>
                            <div id="leftmenu_template_filter_container">
                                <div class="leftmenu_expandedlist">
                                    <div class="leftmenu_content" id="leftmenu_submenu_my_template">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                            
                                            <div>
                                                <%=t.t("my.template")%></div>
                                        </div>
                                    </div>
                                </div>
                                <div class="leftmenu_expandedlist">
                                    <div class="leftmenu_content" id="leftmenu_submenu_featured_template">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                          
                                            <div>
                                                <%=t.t("featured.template")%></div>
                                        </div>
                                    </div>
                                </div>
                                <div class="leftmenu_expandedlist">
                                    <div class="leftmenu_content" id="leftmenu_submenu_community_template">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                            
                                            <div>
                                                <%=t.t("community.template")%></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div id="leftmenu_iso_filter">
                            <div class="leftmenu_content" id="leftmenu_iso_filter_header">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows expanded_open" id="arrowIcon">
                                    </div>
                                    
                                    <%=t.t("iso")%>
                                </div>
                            </div>
                            <div id="leftmenu_iso_filter_container">
                                <div class="leftmenu_expandedlist">
                                    <div class="leftmenu_content" id="leftmenu_submenu_my_iso">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                            
                                            <div>
                                                <%=t.t("my.iso")%></div>
                                        </div>
                                    </div>
                                </div>
                                <div class="leftmenu_expandedlist">
                                    <div class="leftmenu_content" id="leftmenu_submenu_featured_iso">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                            
                                            <div>
                                                <%=t.t("featured.iso")%></div>
                                        </div>
                                    </div>
                                </div>
                                <div class="leftmenu_expandedlist">
                                    <div class="leftmenu_content" id="leftmenu_submenu_community_iso">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                            
                                            <div>
                                                <%=t.t("community.iso")%></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>  
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="leftmenu_account">
                        <div class="leftmenu_firstindent">
                           <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/accounts_leftmenuicon.png" alt="Account" /></div>
                            <%=t.t("account")%>
                        </div>
                    </div>
                    
                </div>                
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="expandable_first_level">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/domain_leftmenuicon.png" alt="Domain" /></div>
                            <%=t.t("domain")%>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content">
                                <div class="leftmenu_domainindent" style="margin-left: 30px;">
                                    <div id="domain_title_container" class="tree_levels">
                                        <div class="leftmenu_arrows expanded_close" id="arrowIcon">
                                        </div>
                                        Domain Name
                                    </div>
                                    <div id="domain_children_container" style="display: none">
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="expandable_first_level">
                        <div class="leftmenu_firstindent">
                             <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/events_leftmenuicon.png" alt="Event" /></div>
                            <%=t.t("event")%>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_event">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <%=t.t("event")%>
                                </div>
                            </div>
                        </div>
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_alert">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <%=t.t("alert")%>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="expandable_first_level">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/configuration_leftmenuicon.png" alt="System" /></div>
                            <%=t.t("system")%>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_resource">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows expanded_close" id="resource_arrow">
                                    </div>
                                   
                                    <%=t.t("resources")%>
                                </div>
                            </div>
                        </div>
                        
                        <div id="leftmenu_zone_tree">
                        	<div class="leftmenu_bigloading" id="loading_container" style="display:none;" ></div>
						    <div id="tree_container"></div>
                        </div>
                                                                    
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" style="border-bottom: 1px dashed b4c8d6;" id="leftmenu_service_offering">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows expanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <%=t.t("service.offerings")%>
                                </div>
                            </div>
                        </div>
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_disk_offering">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows expanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <%=t.t("disk.offerings")%>
                                </div>
                            </div>
                        </div>
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_global_setting">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows expanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <%=t.t("global.settings")%>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <!-- Left Menu ends here-->
    </div>
    <div id="footer">
        <p>
            <%=t.t("version")%>: <span>2.2</span>
        </p>
        <div class="poweredby_box">
        </div>
    </div>
    <!-- Dialogs 1 -->
    <div id="dialog_confirmation" title="Confirmation" style="display: none">
    </div>
    <div id="dialog_info" title="Info" style="display: none">
    </div>
    <div id="dialog_alert" title="Alert" style="display: none">
    </div>
    <div id="dialog_error" title="Error" style="display: none; color: red">
    </div>
    <div id="dialog_session_expired" title="Session Expired" style="display: none">
        <p>
            <%=t.t("your.session.has.expired")%>
        </p>
    </div>
    <div id="dialog_error_internet_not_resolved" title="Error" style="display: none">
        <p style="color: red">
            <%=t.t("internet.name.can.not.be.resolved")%>
        </p>
    </div>
    <div id="dialog_error_management_server_not_accessible" title="Error" style="display: none">
        <p style="color: red">
            <%=t.t("management.server.is.not.accessible")%>
        </p>
    </div>
    <!-- Dialogs 2 -->
    <div id="dialog_info_please_select_one_item_in_middle_menu" title="Alert" style="display: none">
        <p>
            <%=t.t("please.select.at.least.one.item.in.middle.menu")%>
        </p>
    </div>
    <!-- ***** templates (begin) *************************************************************************************************-->
    <div class="leftmenu_content" id="leftmenu_submenu_template" style="display: none">
        <div class="leftmenu_thirdindent">
            <div class="leftmenu_list_icons">
                <img id="icon" style="display: none" /></div>
            <div id="submenu_name">
                (submenu)</div>
        </div>
    </div>
    <div class="midmenu_list" id="midmenu_item" style="display: none;">
        <div class="midmenu_content" id="content">
            <div class="midmenu_icons" id="icon_container" style="display: none">
                <img id="icon" /></div>
            <div class="midmenu_textbox">
                <p style="font-size: 11px;">
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
    <li id="action_list_item_middle_menu" style="display: none; width: 94px;"><a id="link"
        href="#">(middle menu action)</a></li>
    <!-- action list item for details tab, subgrid item-->
    <li id="action_list_item" style="display: none;"><a id="link" href="#">(action)</a></li>
    <li id="no_available_actions" style="display: none">
        <%=t.t("no.available.actions")%></li>
    
    <!-- Zonetree Template (begin) --> 
    <div class="leftmenu_expandedlist" id="leftmenu_zone_node_template" style="display:none">
     	<div class="leftmenu_loadingbox" style="display:none;" id="loading_container">
        	<div class="leftmenu_loader"></div>
            <p> Adding Zone &hellip; </p>
        </div>
        <div id="row_container">
	        <div class="leftmenu_content" id="header">  
	            <div class="leftmenu_thirdindent">
	                <div class="leftmenu_arrows expanded_close" id="zone_arrow">
	                </div>
	                
	                Zone: <strong><span id="zone_name"></span></strong>
	            </div>  
	        </div>			
            <div id="zone_content" style="display: none">
	            <div id="pods_container">
	            </div>
	            <div id="systemvms_container">
	            </div>
	        </div>
		</div>
    </div>
    <!-- Zone Template (end) -->
	<!-- Pod Template (begin) -->    
    <div class="leftmenu_expandedlist" id="leftmenu_pod_node_template" style="display:none">
    	<div class="leftmenu_loadingbox" style="display:none;"  id="loading_container">
        	<div class="leftmenu_loader"></div>
            <p> Adding Pod &hellip; </p>
        </div>
        <div id="row_container">
	        <div class="leftmenu_content" id="header">
	            <div class="leftmenu_fourthindent">
	                <div class="leftmenu_arrows expanded_close" id="pod_arrow">
	                </div>
	               
	                Pod: <strong><span id="pod_name"></span></strong>
	            </div>
	        </div>	
            <div id="pod_content" style="display: none">
	            <div id="clusters_container">
	            </div>
	        </div>
	    </div>
    </div>
    <!-- Pod Template (end) -->
	<!-- Cluster Template (begin) -->    
    <div class="leftmenu_expandedlist" id="leftmenu_cluster_node_template" style="display:none">
    	<div class="leftmenu_loadingbox" style="display:none;" id="loading_container">
        	<div class="leftmenu_loader"></div>
            <p> Adding Cluster &hellip; </p>
        </div>
        <div id="row_container">
	        <div class="leftmenu_content" id="header">
	            <div class="leftmenu_fifthindent">
	                <div class="leftmenu_arrows white_nonexpanded_close" id="cluster_arrow">
	                </div>
	                
	                Cluster: <strong><span id="cluster_name"></span></strong>
	            </div>
	        </div>	
			<div id="cluster_content">
	            <div id="hosts_container">
	            </div>
	            <div id="primarystorages_container">
	            </div>
	        </div>
	    </div>
    </div> 
    <!-- Cluster Template (end) -->
    <!-- SystemVM Template (begin) -->
    <div class="leftmenu_expandedlist" id="leftmenu_systemvm_node_template" style="display:none">
    	<div id="row_container">
	        <div class="leftmenu_content" id="header">
	            <div class="leftmenu_fourthindent">
	                <div class="leftmenu_arrows white_nonexpanded_close" id="systemvm_arrow">
	                </div>
	             
	                System VM: <strong><span id="systemvm_name"></span></strong>
	            </div>
	        </div>
        </div>
    </div> 
    <!-- SystemVM Template (end) -->
    <!-- ***** templates (end) *************************************************************************************************-->
</body>
</html>
