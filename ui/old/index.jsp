<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>
<% long now = System.currentTimeMillis(); %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta http-equiv='cache-control' content='no-cache'>
    <meta http-equiv='expires' content='0'>
    <meta http-equiv='pragma' content='no-cache'>
    <link rel="stylesheet" href="css/jquery-ui.custom.css" type="text/css" />
    <link rel="stylesheet" href="css/logger.css" type="text/css" />
    <link rel="stylesheet" href="css/main.css" type="text/css" />
    <!--<link rel="stylesheet" href="custom/custom1/css/custom1.css" type="text/css" />-->

	<!-- Common libraries -->
    <script type="text/javascript" src="scripts/jquery.min.js"></script>
    <script type="text/javascript" src="scripts/jquery-ui.custom.min.js"></script>
    <script type="text/javascript" src="scripts/date.js"></script>
    <script type="text/javascript" src="scripts/jquery.cookies.js"></script>
    <script type="text/javascript" src="scripts/jquery.timers.js"></script>
    <script type="text/javascript" src="scripts/jquery.md5.js"></script>

    <!-- cloud.com scripts -->
    <script type="text/javascript" src="scripts/cloud.logger.js?t=<%=now%>"></script>
	<script type="text/javascript" src="scripts/cloud.core.callbacks.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.js?t=<%=now%>"></script>
	<script type="text/javascript" src="scripts/cloud.core.init.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.instance.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.event.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.alert.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.account.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.volume.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.snapshot.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.ipaddress.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.securitygroup.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.template.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.iso.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.router.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.dashboard.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.domain.js?t=<%=now%>"></script>    
    <script type="text/javascript" src="scripts/cloud.core.serviceoffering.js?t=<%=now%>"></script>    
    <script type="text/javascript" src="scripts/cloud.core.systemserviceoffering.js?t=<%=now%>"></script>    
    <script type="text/javascript" src="scripts/cloud.core.diskoffering.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.networkoffering.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.globalsetting.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.resource.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.zone.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.secondarystorage.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.network.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.pod.js?t=<%=now%>"></script>  
    <script type="text/javascript" src="scripts/cloud.core.cluster.js?t=<%=now%>"></script> 
    <script type="text/javascript" src="scripts/cloud.core.host.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.primarystorage.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.systemvm.js?t=<%=now%>"></script>
	
	<!-- Favicon -->
	<link rel="shortcut icon" href="favicon.ico" type="image/x-icon" />

    <title><fmt:message key="label.cloud.console"/></title>
    
    <script language="javascript">
	g_dictionary = { 	
		'label.adding.processing': '<fmt:message key="label.adding.processing"/>',
		'label.adding.succeeded': '<fmt:message key="label.adding.succeeded"/>',
		'label.adding.failed': '<fmt:message key="label.adding.failed"/>',
        'label.deleting.processing': '<fmt:message key="label.deleting.processing"/>',
        'label.deleting.failed': '<fmt:message key="label.deleting.failed"/>',
        'label.saving.processing': '<fmt:message key="label.saving.processing"/>',
        'label.succeeded': '<fmt:message key="label.succeeded"/>',
		'label.failed': '<fmt:message key="label.failed"/>',
		'label.error.code': '<fmt:message key="label.error.code"/>',	
		'label.required': '<fmt:message key="label.required"/>',
		'label.invalid.number': '<fmt:message key="label.invalid.number"/>',	
		'label.invalid.integer': '<fmt:message key="label.invalid.integer"/>',	
		'label.minimum': '<fmt:message key="label.minimum"/>',
		'label.maximum': '<fmt:message key="label.maximum"/>',
		'label.character': '<fmt:message key="label.character"/>',
		'label.double.quotes.are.not.allowed': '<fmt:message key="label.double.quotes.are.not.allowed"/>',
		'label.not.found': '<fmt:message key="label.not.found"/>',
		'label.example': '<fmt:message key="label.example"/>',
		'label.by.zone': '<fmt:message key="label.by.zone"/>', 
		'label.by.pod': '<fmt:message key="label.by.pod"/>', 
		'label.by.domain': '<fmt:message key="label.by.domain"/>',
		'label.by.account': '<fmt:message key="label.by.account"/>',
		'label.path': '<fmt:message key="label.path"/>',
		'label.SR.name': '<fmt:message key="label.SR.name"/>',
		'label.nfs': '<fmt:message key="label.nfs"/>',
		'label.ocfs2': '<fmt:message key="label.ocfs2"/>',		
		'label.SharedMountPoint': '<fmt:message key="label.SharedMountPoint"/>',
		'label.PreSetup': '<fmt:message key="label.PreSetup"/>',
		'label.iscsi': '<fmt:message key="label.iscsi"/>',
		'label.clvm': '<fmt:message key="label.clvm"/>',
		'label.VMFS.datastore': '<fmt:message key="label.VMFS.datastore"/>',
		'label.theme.default': '<fmt:message key="label.theme.default"/>',
		'label.none': '<fmt:message key="label.none"/>',
		'label.yes': '<fmt:message key="label.yes"/>',
		'label.no': '<fmt:message key="label.no"/>'		
	};	
	</script>
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
                        	<h1><fmt:message key="label.welcome.cloud.console"/>&hellip;</h1>
                        </div>
                        
                        <div class="login_formbox">
                        	<form id="loginForm" action="#" method="post" name="loginForm">
                            	<ol>
                                	<li>
                                    	<label for="user_name"><fmt:message key="label.username"/>: </label>
                                        <div class="login_formbox_textbg">
                                        	<input id="account_username" class="text" type="text" name="account_username" AUTOCOMPLETE="off"/>
                                        </div>
                                    </li>
                                    
                                    <li>
                                    	<label for="user_name"><fmt:message key="label.password"/>: </label>
                                        <div class="login_formbox_textbg">
                                        	<input id="account_password" class="text" type="password" name="account_password" AUTOCOMPLETE="off"/>
                                        </div>
                                    </li>
                                    
                                    <li>
                                    	<label for="user_name"><fmt:message key="label.domain"/>: </label>
                                        <div class="login_formbox_textbg">
                                        	<input id="account_domain" class="text" type="text" name="account_domain" />
                                        </div>
                                    </li>
                                </ol>
                                <div class="loginbutton_box">                                   
                                	<button type="button" id="loginbutton" class="login_button"><fmt:message key="label.login"/></button>
                                </div>
                            </form>
                            
                            <div class="error_box" id="login_error" style="display:none;">
                            	<p><fmt:message key="error.login"/></p>
                            </div>
                            
                            <div class="loginoptions_panel">
                            	<div class="loginoptions_box">
                                	<div id="lang_button" class="loginoptions_dropdownbutton">
                                    	<p id="lang_name"><fmt:message key="label.lang.english"/></p>
                                        <div class="loginoptions_ddarrow"></div>
         							
										<div id="lang_menu" class="loginoptions_dropdown" style="display:none;">
											<ul>
												<li id="en"><fmt:message key="label.lang.english"/></li>
												<li id="zh_CN"><fmt:message key="label.lang.chinese"/></li>
												<li id="ja"><fmt:message key="label.lang.japanese"/></li>
												<li id="es"><fmt:message key="label.lang.spanish"/></li>
											</ul>
										</div>
                                    </div>
                                    <div id="theme_button" class="loginoptions_dropdownbutton">
                                    	<p id="theme_name"><fmt:message key="label.theme.default"/></p>
                                        <div class="loginoptions_ddarrow"></div>
										<div id="theme_menu" class="loginoptions_dropdown" style="display:none;">
											<ul>
												<li id="theme_default"><fmt:message key="label.theme.default"/></li>
												<li id="custom1"><fmt:message key="label.theme.grey"/></li>
												<li id="custom2"><fmt:message key="label.theme.lightblue"/></li>
											</ul>
										</div>
                                    </div>
                                </div>
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
                        <fmt:message key="label.welcome"/> <span id="main_username"></span>, <a href="#" id="main_logout"><fmt:message key="label.logout"/></a>
                    </p>
                </div>
            </div>
        </div>
        <div id="main_contentpanel">
            <div class="right_panel" id="east_panel">
                <div id="contentwrapper">
                    <!-- Action Panel starts here-->
                    <div class="actionpanel">
                        <div class="searchpanel" id="search_panel">
                            <form method="post" action="#">
                            <ol>
                                <li>
                                    <div id="basic_search" class="search_textbg">
                                        <input class="text" type="text" id="search_input" />
										<div id="refresh_mid" class="search_refreshbutton" style="display: block;"></div>
                                        <div id="clear_search" class="search_closebutton" style="display: block;"></div>
                                    </div>
                                </li>
                            </ol>
                            </form>
                          <div class="searchpanel_filterbutton" id="advanced_search_icon">
                                </div>                           
                        </div>
                                                
                        <div id="top_button_container">		
                           			
						</div>
						
                        <div class="actionpanel_rightbutton_container">
                            <!--<div class="actionpanel_button_wrapper" id="help_link" style="display: block; border:none; float: right; position: relative;">
                                <div class="actionpanel_button" id="help_button">
                                    <div class="actionpanel_button_icons">
                                        <img src="images/help_actionicon.png" alt="Help" /></div>
                                    <div class="actionpanel_button_links">
                                        <fmt:message key="label.help"/>
                                    </div>
                                </div>
                            </div>-->
                            <div class="actionpanel_button_wrapper" id="refresh_link">
                                <div class="actionpanel_button" id="refresh_button">
                                    <div class="actionpanel_button_icons">
                                        <img src="images/refresh_actionicon.png" alt="Refresh" /></div>
                                    <div class="actionpanel_button_links">
                                        <fmt:message key="label.refresh"/></div>
                                </div>
                            </div>
						</div>
							
						<div class="help_dropdown_box" id="help_dropdown_dialog" style="display:none;">
                            	<div class="help_dropdown_box_titlebox">
                                	<h2><fmt:message key="label.help"/></h2>
                                    <a id="help_dropdown_close" href="#"> <fmt:message key="label.close"/></a>
                                </div>
                                
                                <div class="help_dropdown_box_textbox" id="help_dropdown_body">
									<a id="help_top" name="help_top"></a>
                                	<ul>
                                    	<li><a href="#topic1">Topic 1</a></li>
                                        <li><a href="#topic2">Topic 2</a></li>
                                        <li><a href="#topic3">Topic 3</a></li>
                                    </ul>
                                    
                                    
                                    <h3>Topic 1<a id="topic1" name="topic1"></a>
                                	<p>Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. </p>
                                    <p><a style="float:right;" href="#help_top">Top</a></h3></p>
                                    
                                   
                                    <h3>Topic 2 <a id="topic2" name="topic2"></a></h3>
                                	<p>Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. </p><p>It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.</p>	
                                    <p><a style="float:right;" href="#help_top">Top</a></h3></p>
                                    
                                    <h3>Topic 3<a id="topic3" name="topic3"></a></h3>
                                	<p>Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. </p><p>It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.</p>
                                    <p><a style="float:right;" href="#help_top">Top</a></h3></p>
                                </div>
                            </div>
                    </div>
                    <!-- Action Panel ends here-->
                    <!-- Right Panel starts here-->
                    <div class="main_contentarea" id="right_panel">
                    </div>
                    
                    <!-- Right Panel ends here-->
                </div>
                <!-- Mid Menu starts here-->
                <div class="midmenu_panel" id="middle_menu">
                    <div class="midmenu_box" id="midmenu_box">
                    	<div id="advanced_search_container" class="adv_searchpopup"></div>
                        <div id="midmenu_spinning_wheel" class="midmenu_mainloaderbox" style="display: none;">
                            <div class="midmenu_mainloader_animatedicon">
                            </div>
                            <p>
                                <fmt:message key="label.loading"/> &hellip;</p>
                        </div>
                        <div id="midmenu_container">                            
                        </div>
                    </div>
                    <div class="midmenu_navigationbox" id="middle_menu_pagination" style="display:none;">
                        <div id="midmenu_prevbutton" class="midmenu_prevbutton" style="display:none;">
                        </div>
                        <div id="midmenu_nextbutton" class="midmenu_nextbutton" style="display:none;">
                        </div>
                    </div>
                </div>
                <!-- Mid Menu ends here-->
            </div>
        </div>
        <!-- Left Menu starts here-->
        <div class="leftmenu_panel" id="west_panel">
            <div class="leftmenu_box" id="leftmenu_container">     
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="leftmenu_dashboard">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_list_icons">
                                <img src="images/leftmenu_dashboardicon.png" alt="Dashboard" /></div>
                            <fmt:message key="label.menu.dashboard"/>
                        </div>
                    </div>
                    
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="leftmenu_instances">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/instance_leftmenuicon.png" alt="Instances" /></div>
                            <fmt:message key="label.menu.instances"/>
                        </div>
                    </div>
                    
                    <div id="leftmenu_instance_expandedbox" class="leftmenu_expandedbox" style="display: none">
						<div class="leftmenu_expandedlist" id="leftmenu_instances_my_instances_container" style="display:none">
							<div class="leftmenu_content" id="leftmenu_instances_my_instances">
								<div class="leftmenu_secondindent">
									<div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
									</div>
									<span id="label"><fmt:message key="label.menu.my.instances"/></span>
								</div>
							</div>
						</div>
						<div class="leftmenu_expandedlist" id="leftmenu_instances_all_instances_container" style="display:none">
							<div class="leftmenu_content" id="leftmenu_instances_all_instances">
								<div class="leftmenu_secondindent">
									<div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
									</div>
									<span id="label"><fmt:message key="label.menu.all.instances"/></span>
								</div>
							</div>
						</div>
						<div class="leftmenu_expandedlist" id="leftmenu_instances_running_instances_container" style="display:none">
							<div class="leftmenu_content" id="leftmenu_instances_running_instances">
								<div class="leftmenu_secondindent">
									<div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
									</div>
									<span id="label"><fmt:message key="label.menu.running.instances"/></span>
								</div>
							</div>
						</div>
						<div class="leftmenu_expandedlist" id="leftmenu_instances_stopped_instances_container" style="display:none">
							<div class="leftmenu_content" id="leftmenu_instances_stopped_instances">
								<div class="leftmenu_secondindent">
									<div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
									</div>
									<span id="label"><fmt:message key="label.menu.stopped.instances"/></span>
								</div>
							</div>
						</div>
						<div class="leftmenu_expandedlist" id="leftmenu_instances_destroyed_instances_container" style="display:none">
							<div class="leftmenu_content" id="leftmenu_instances_destroyed_instances">
								<div class="leftmenu_secondindent">
									<div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
									</div>
									<span id="label"><fmt:message key="label.menu.destroyed.instances"/></span>
								</div>
							</div>
						</div>		
										
						<div id="leftmenu_instance_group_container">
                    	</div>	
                    						
                    </div>                    
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="leftmenu_storage">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/storage_leftmenuicon.png" alt="Storage" /></div>
                            <fmt:message key="label.menu.storage"/>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_volume">
                                <div class="leftmenu_secondindent">
                                    
                                
                                   	<div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                      
                                    <fmt:message key="label.menu.volumes"/>
                                </div>
                            </div>
                        </div>
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_snapshot">
                                <div class="leftmenu_secondindent">
                                	  <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                   
                                    <fmt:message key="label.menu.snapshots"/>
                                </div>
                            </div>
                        </div>
                    </div>
                </div> 
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="leftmenu_network">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/network_leftmenuicon.png" alt="Network" /></div>
                            <fmt:message key="label.menu.network"/>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_ip">
                                <div class="leftmenu_secondindent">
                                   	<div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>                                    
                                    <fmt:message key="label.menu.ipaddresses"/>
                                </div>
                            </div>
                        </div>  
                        <div class="leftmenu_expandedlist" id="leftmenu_security_group_container" style="display:none;">
                            <div class="leftmenu_content" id="leftmenu_security_group">
                                <div class="leftmenu_secondindent">
                                   	<div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>                                    
                                    <fmt:message key="label.menu.security.groups"/>
                                </div>
                            </div>
                        </div>						
                    </div>                    
                </div>  
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="leftmenu_templates">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/templates_leftmenuicon.png" alt="Templates" /></div>
                            <fmt:message key="label.menu.templates"/>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                        <div id="leftmenu_itemplate_filter">
                            <div class="leftmenu_content" id="leftmenu_template_filter_header">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows expanded_open" id="arrow_icon">
                                    </div>
                                    
                                    <fmt:message key="label.menu.templates"/>
                                </div>
                            </div>
                            <div id="leftmenu_template_filter_container">
                                <div class="leftmenu_expandedlist">
                                    <div class="leftmenu_content" id="leftmenu_submenu_my_template">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                            
                                            <div><fmt:message key="label.menu.my.templates"/></div>
                                        </div>
                                    </div>
                                </div>
                                <div class="leftmenu_expandedlist">
                                    <div class="leftmenu_content" id="leftmenu_submenu_featured_template">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                          
                                            <div><fmt:message key="label.menu.featured.templates"/></div>
                                        </div>
                                    </div>
                                </div>
                                <div class="leftmenu_expandedlist" id="leftmenu_submenu_community_template_container" style="display: none">
                                    <div class="leftmenu_content" id="leftmenu_submenu_community_template">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                            
                                            <div><fmt:message key="label.menu.community.templates"/></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div id="leftmenu_iso_filter">
                            <div class="leftmenu_content" id="leftmenu_iso_filter_header">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows expanded_open" id="arrow_icon">
                                    </div>
                                    
                                    <fmt:message key="label.menu.isos"/>
                                </div>
                            </div>
                            <div id="leftmenu_iso_filter_container">
                                <div class="leftmenu_expandedlist">
                                    <div class="leftmenu_content" id="leftmenu_submenu_my_iso">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                            
                                            <div><fmt:message key="label.menu.my.isos"/></div>
                                        </div>
                                    </div>
                                </div>
                                <div class="leftmenu_expandedlist">
                                    <div class="leftmenu_content" id="leftmenu_submenu_featured_iso">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                            
                                            <div><fmt:message key="label.menu.featured.isos"/></div>
                                        </div>
                                    </div>
                                </div>
                                <div class="leftmenu_expandedlist" id="leftmenu_submenu_community_iso_container" style="display: none">
                                    <div class="leftmenu_content" id="leftmenu_submenu_community_iso">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                            
                                            <div><fmt:message key="label.menu.community.isos"/></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>  
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="leftmenu_account" style="display: none">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/accounts_leftmenuicon.png" alt="Accounts" /></div>
                            <fmt:message key="label.menu.accounts"/>
                        </div>
                    </div>
                                        
                    <div id="leftmenu_account_expandedbox" class="leftmenu_expandedbox" style="display: none">
				        <div class="leftmenu_expandedlist" id="leftmenu_account_my_accounts_container">
				            <div class="leftmenu_content" id="leftmenu_account_my_accounts">
				                <div class="leftmenu_secondindent">
				                    <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
				                    </div>
				                    <span id="label">
				                        <fmt:message key="label.menu.my.accounts"/>
				                    </span>
				                </div>
				            </div>
				        </div>
				        <div class="leftmenu_expandedlist" id="leftmenu_account_all_accounts_container">
				            <div class="leftmenu_content" id="leftmenu_account_all_accounts">
				                <div class="leftmenu_secondindent">
				                    <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
				                    </div>
				                    <span id="label">
				                        <fmt:message key="label.menu.all.accounts"/>
				                    </span>
				                </div>
				            </div>
				        </div>
				    </div>                    
                    
                </div>                
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="leftmenu_domain" style="display: none">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/domain_leftmenuicon.png" alt="Domains" /></div>
                            <fmt:message key="label.menu.domains"/>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                    		<div id="loading_container" class="leftmenu_loadingbox" style="display: none;">
                                <div class="leftmenu_loader">
                                </div>
                                <p>
                                    <fmt:message key="label.loading"/> &hellip;
                                </p>
                        </div>
                        <div id="leftmenu_domain_tree">
                            
                            <div id="tree_container" class="leftmenu_expandedlist">
                            </div>
                        </div>  
                    </div>
                </div>
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="leftmenu_events">
                        <div class="leftmenu_firstindent">
                             <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/events_leftmenuicon.png" alt="Events" /></div>
                            <fmt:message key="label.menu.events"/>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_event">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <fmt:message key="label.menu.events"/>
                                </div>
                            </div>
                        </div>
                        <div class="leftmenu_expandedlist" id="leftmenu_alert_container" style="display: none">
                            <div class="leftmenu_content" id="leftmenu_alert">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <fmt:message key="label.menu.alerts"/>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                             
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="leftmenu_system" style="display: none">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/resource_leftmenuicon.png" alt="System" /></div>
                            <fmt:message key="label.menu.system"/>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_physical_resource">
                                <div class="leftmenu_secondindent">
                                	<div class="leftmenu_arrowloader" id="loading_container" style="display:none;"></div>
                                    <div class="leftmenu_arrows expanded_close" id="physical_resource_arrow">
                                    </div>
                                   
                                    <fmt:message key="label.menu.physical.resources"/>
                                </div>
                            </div>
                        </div>
                        
                        <div id="leftmenu_zone_tree">                            
						    <div id="tree_container"></div>
                        </div>              

						<div>
                            <div class="leftmenu_content">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <fmt:message key="label.menu.virtual.resources"/>
                                </div>
                            </div>
                            <div>
                                <div class="leftmenu_expandedlist">
                                    <div class="leftmenu_content" id="leftmenu_submenu_virtual_router">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                            
                                            <div><fmt:message key="label.menu.virtual.appliances"/></div>
                                        </div>
                                    </div>
                                </div>
                                <div class="leftmenu_expandedlist">
                                    <div class="leftmenu_content" id="leftmenu_submenu_systemvm">
                                        <div class="leftmenu_thirdindent">
                                            <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                            </div>
                                          
                                            <div><fmt:message key="label.menu.system.vms"/></div>
                                        </div>
                                    </div>
                                </div>                                
                            </div>
                        </div>                        
                    </div>
                </div>   
                                
                <div class="leftmenu_list">
                    <div class="leftmenu_content_flevel" id="leftmenu_configuration" style="display: none">
                        <div class="leftmenu_firstindent">
                            <div class="leftmenu_arrows_firstlevel_open" id="expandable_first_level_arrow" style="display:none;"></div>
                            <div class="leftmenu_list_icons">
                                <img src="images/configuration_leftmenuicon.png" alt="Configuration" /></div>
                            <fmt:message key="label.menu.configuration"/>
                        </div>
                    </div>
                    <div class="leftmenu_expandedbox" style="display: none">                                                          
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" style="border-bottom: 1px dashed b4c8d6;" id="leftmenu_service_offering">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <fmt:message key="label.menu.service.offerings"/>
                                </div>
                            </div>
                        </div>
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" style="border-bottom: 1px dashed b4c8d6;" id="leftmenu_system_service_offering">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <fmt:message key="label.menu.system.service.offerings"/>
                                </div>
                            </div>
                        </div>
                        
                        
                        
                        
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_disk_offering">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <fmt:message key="label.menu.disk.offerings"/>
                                </div>
                            </div>
                        </div>
                        
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_network_offering">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                    <fmt:message key="label.menu.network.offerings"/>
                                </div>
                            </div>
                        </div>
                        
                        <div class="leftmenu_expandedlist">
                            <div class="leftmenu_content" id="leftmenu_global_setting">
                                <div class="leftmenu_secondindent">
                                    <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                                    </div>
                                    
                                    <fmt:message key="label.menu.global.settings"/>
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
        <div class="poweredby_box">
        </div>
    </div>
    <!-- Dialogs 1 -->
    <div id="dialog_confirmation" title='<fmt:message key="label.confirmation"/>' style="display: none">
    </div>
    <div id="dialog_info" title='<fmt:message key="label.info"/>' style="display: none">
    </div>
    <div id="dialog_action_complete" title='<fmt:message key="label.info"/>' style="display: none">
    </div>
    <div id="dialog_alert" title='<fmt:message key="label.alert"/>' style="display: none">
    </div>
    <div id="dialog_error" title='<fmt:message key="label.error"/>' style="display: none; color: red">
    </div>
    <div id="dialog_session_expired" title='<fmt:message key="label.session.expired"/>' style="display: none">
        <p>
			<fmt:message key="error.session.expired"/>
        </p>
    </div>
    <div id="dialog_error_internet_not_resolved" title='<fmt:message key="label.error"/>' style="display: none">
        <p style="color: red">
			<fmt:message key="error.unresolved.internet.name"/>
        </p>
    </div>
    <div id="dialog_error_management_server_not_accessible" title='<fmt:message key="label.error"/>' style="display: none">
        <p style="color: red">
			<fmt:message key="error.mgmt.server.inaccessible"/>
        </p>
    </div>
    <!-- Dialogs 2 -->
    <div id="dialog_info_please_select_one_item_in_middle_menu" title='<fmt:message key="label.alert"/>' style="display: none">
        <p>
			<fmt:message key="error.menu.select"/>
        </p>
    </div>
    <!-- ***** templates (begin) *************************************************************************************************-->
    <div id="leftmenu_secondindent_template" class="leftmenu_expandedlist" style="display:none">
        <div class="leftmenu_content">
            <div class="leftmenu_secondindent">
                <div class="leftmenu_arrows white_nonexpanded_close" id="arrowIcon">
                </div>
                <span id="label"></span>
            </div>
        </div>
    </div>
    
    <div id="midmenu_itemheader_without_margin" class="midmenu_itemheader" style="display:none; ">
    	<p id="name"></p>      
    </div>    
    <div id="midmenu_itemheader_with_margin" class="midmenu_itemheader" style="display:none; margin-top:40px;">
    	<p id="name"></p>       
    </div>    
    
    <div class="midmenu_list" id="midmenu_item" style="display: none;">
        <div class="midmenu_content" id="content">
            <div class="midmenu_icons" id="icon_container" style="display: none">
                <img id="icon" /></div>
            <div class="midmenu_textbox">
                <p id="first_row_container">
                    <strong id="first_row">&nbsp;</strong>
                </p>
                <span id="second_row_container">
                    <span id="second_row">&nbsp;</span>
                </span>
            </div>
            <div class="midmenu_inactionloader" id="spinning_wheel" style="display: none;">
            </div>
            <div class="midmenu_infoicon" id="info_icon" style="display: none;">
            </div>
            <div class="midmenu_addingfailed_closeicon" id="close_icon" style="display: none;">
            </div>
        </div>
    </div>
    <!-- action list item for middle menu -->
    <li id="action_list_item_middle_menu" style="display: none; width: 94px;"><a id="link"
        href="#">(middle menu action)</a></li>
    <!-- action list item for details tab, subgrid item-->
    <li id="action_list_item" style="display: none;"><a id="link" href="#">(action)</a></li>
    <li id="no_available_actions" style="display: none">
		<fmt:message key="label.no.actions"/>
    </li>
    
    <!-- middle menu: no items available (begin) --> 
    <div id="midmenu_container_no_items_available" class="midmenu_emptymsgbox" style="display:none">
        <p><fmt:message key="label.no.items"/></p>
    </div>
    <!-- middle menu: no items available (end) --> 
    
    <!-- Zone Template (begin) --> 
    <div class="leftmenu_expandedlist" id="leftmenu_zone_node_template" style="display:none">       
        <div id="zone_header" class="leftmenu_content">  
            <div class="leftmenu_thirdindent">
            	<div class="leftmenu_arrowloader" id="zone_loading_container" style="display:none;"></div>
                <div class="leftmenu_arrows expanded_close" id="zone_arrow">
                </div>	                
                <span id="zone_name_label"><fmt:message key="label.zone"/>: </span>
                <span id="zone_name"></span>
            </div>  
        </div>			
        <div id="zone_content" style="display: none">
            <div id="pods_container">
            </div>	 
                        
	        <div id="secondarystorage_header" class="leftmenu_content">
	            <div class="leftmenu_fourthindent">
	                <div class="leftmenu_arrows white_nonexpanded_close" id="secondarystorage_arrow">
	                </div>	                
	                <span id="secondarystorage_name_label"><fmt:message key="label.secondary.storage"/></span>	 
	            </div>
	        </div>	      
            	            
            <div id="network_header" class="leftmenu_content">
	            <div class="leftmenu_fourthindent">
	                <div class="leftmenu_arrows white_nonexpanded_close" id="network_arrow">
	                </div>	                
	                <span id="network_name_label"><fmt:message key="label.menu.network"/></span>	 
	            </div>
	        </div>		              	                       
        </div>	    
    </div>
    <!-- Zone Template (end) -->
	<!-- Pod Template (begin) -->    
    <div class="leftmenu_expandedlist" id="leftmenu_pod_node_template" style="display:none">              
	    <div id="pod_header" class="leftmenu_content">
            <div class="leftmenu_fourthindent">
            	<div class="leftmenu_arrowloader" id="pod_loading_container" style="display:none;"></div>
                <div class="leftmenu_arrows white_nonexpanded_close" id="pod_arrow">
                </div>	               
                <span id="pod_name_label"><fmt:message key="label.pod"/>: </span>
                <span id="pod_name"></span>
            </div>
        </div>	
        <div id="pod_content" style="display: none">
            <div id="clusters_container">
            </div>
        </div>	    
    </div>
    <!-- Pod Template (end) -->
	
    <!-- cluster Template (begin) --> 
    <div class="leftmenu_expandedlist" id="leftmenu_cluster_node_template" style="display:none">       
        <div id="cluster_header" class="leftmenu_content">  
            <div class="leftmenu_fifthindent">
            	<div class="leftmenu_arrowloader" id="cluster_loading_container" style="display:none;"></div>
                <div class="leftmenu_arrows expanded_close" id="cluster_arrow">
                </div>	                
                <span id="cluster_name_label"><fmt:message key="label.cluster"/>: </span>
                <span id="cluster_name"></span>
            </div>  
        </div>			
        <div id="cluster_content" style="display: none">
             <div class="leftmenu_expandedlist" id="leftmenu_host_node_template">       
                <div id="host_header" class="leftmenu_content">  
                    <div class="leftmenu_sixthindent">
            	        <div class="leftmenu_arrowloader" id="host_loading_container" style="display:none;"></div>
                        <div class="leftmenu_arrows white_nonexpanded_close" id="host_arrow">
                        </div>	                
                        <span><fmt:message key="label.host"/></span>               
                    </div>  
                </div>	           
            </div>           
            <div class="leftmenu_expandedlist" id="leftmenu_primarystorage_node_template">       
                <div id="primarystorage_header" class="leftmenu_content">  
                    <div class="leftmenu_sixthindent">
            	        <div class="leftmenu_arrowloader" id="primarystorage_loading_container" style="display:none;"></div>
                        <div class="leftmenu_arrows white_nonexpanded_close" id="primarystorage_arrow">
                        </div>	                
                        <span><fmt:message key="label.primary.storage"/></span>               
                    </div>  
                </div>	           
            </div>                   	                       
        </div>	    
    </div>
    <!-- cluster Template (end) -->   
    
    <!-- domain tree node template (begin) -->
    <div id="domain_tree_node_template" style="display:none">    	
                
            <div id="domain_title_container" class="leftmenu_content">
	            <div class="leftmenu_domainindent" id="domain_indent">   
	                <div class="leftmenu_arrows expanded_close" id="domain_expand_icon">
	                </div>
	                <span id="domain_name">
	                    <fmt:message key="label.domain.name"/></span>
	          	</div>
            </div>                        
            <div id="domain_children_container">
            </div>
   
    </div>
    <!-- domain tree node template (end) -->    
    <!-- ***** templates (end) *************************************************************************************************-->
</body>
</html>
