<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<!-- event detail panel (begin) -->
<div class="main_title" id="right_panel_header">
   
    <div class="main_titleicon">
        <img src="images/title_eventsicon.gif"/></div>
   
    <h1> <fmt:message key="label.menu.events"/>
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top:15px;">
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
		                    <fmt:message key="label.initiated.by"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="username">
		                </div>
		            </div>
		        </div>
		        <div class="grid_rows odd">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <fmt:message key="label.owner.account"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="account">
		                    </div>
		            </div>
		        </div>		        
		        <div class="grid_rows even">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <fmt:message key="label.owner.domain"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="domain">
		                    </div>
		            </div>
		        </div>		        
		        <div class="grid_rows odd">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <fmt:message key="label.type"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="type">
		                </div>
		            </div>
		        </div>
		        <div class="grid_rows even">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <fmt:message key="label.level"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="level">
		                </div>
		            </div>
		        </div>
		        <div class="grid_rows odd">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <fmt:message key="label.description"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="description">
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
		                    <fmt:message key="label.date"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="created">
		                </div>
		            </div>
		        </div>       
		    </div>
		</div>    
    </div>     
</div>
<!-- event detail panel (end) -->

<div id="hidden_container">
    <!-- advanced search popup (begin) -->
    <div id="advanced_search_popup" class="adv_searchpopup_bg" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>
                <li>
                    <select class="select" id="adv_search_type">
                        <option value=""><fmt:message key="label.by.type"/></option>
                       
                        <option value="VM.CREATE">VM.CREATE</option>
                        <option value="VM.DESTROY">VM.DESTROY</option>
                        <option value="VM.START">VM.START</option>
                        <option value="VM.STOP">VM.STOP</option>
                        <option value="VM.REBOOT">VM.REBOOT</option>                        
                        <option value="VM.UPDATE">VM.UPDATE</option>
                        <option value="VM.UPGRADE">VM.UPGRADE</option>
                        <option value="VM.RESETPASSWORD">VM.RESETPASSWORD</option>
                        
                        <option value="ROUTER.CREATE">ROUTER.CREATE</option>
                        <option value="ROUTER.DESTROY">ROUTER.DESTROY</option>
                        <option value="ROUTER.START">ROUTER.START</option>
                        <option value="ROUTER.STOP">ROUTER.STOP</option>
                        <option value="ROUTER.REBOOT">ROUTER.REBOOT</option>
                        <option value="ROUTER.HA">ROUTER.HA</option>
                                            
                        <option value="PROXY.CREATE">PROXY.CREATE</option>
                        <option value="PROXY.DESTROY">PROXY.DESTROY</option>
                        <option value="PROXY.START">PROXY.START</option>
                        <option value="PROXY.STOP">PROXY.STOP</option>
                        <option value="PROXY.REBOOT">PROXY.REBOOT</option>
                        <option value="PROXY.HA">PROXY.HA</option> 
                        
                        <option value="VNC.CONNECT">VNC.CONNECT</option>
                        <option value="VNC.DISCONNECT">VNC.DISCONNECT</option>
                        
                        <option value="NET.IPASSIGN">NET.IPASSIGN</option>
                        <option value="NET.IPRELEASE">NET.IPRELEASE</option>
                        <option value="NET.RULEADD">NET.RULEADD</option>
                        <option value="NET.RULEDELETE">NET.RULEDELETE</option>
                        <option value="NET.RULEMODIFY">NET.RULEMODIFY</option>
                        <option value="NETWORK.CREATE">NETWORK.CREATE</option>
                        <option value="NETWORK.DELETE">NETWORK.DELETE</option>
                        
                        <option value="LB.ASSIGN.TO.RULE">LB.ASSIGN.TO.RULE</option>
                        <option value="LB.REMOVE.FROM.RULE">LB.REMOVE.FROM.RULE</option>
                        <option value="LB.CREATE">LB.CREATE</option>
                        <option value="LB.DELETE">LB.DELETE</option>
                        <option value="LB.UPDATE">LB.UPDATE</option>
                                                 
                        <option value="ACCOUNT.CREATE">ACCOUNT.CREATE</option>
                        <option value="ACCOUNT.DELETE">ACCOUNT.DELETE</option>
                        <option value="ACCOUNT.DISABLE">ACCOUNT.DISABLE</option>
                                                
                        <option value="VM.DISABLEHA">VM.DISABLEHA</option>
                        <option value="VM.ENABLEHA">VM.ENABLEHA</option>
                        <option value="VM.UPGRADE">VM.UPGRADE</option>
                        <option value="VM.RESETPASSWORD">VM.RESETPASSWORD</option>                        
                       
                        <option value="USER.LOGIN">USER.LOGIN</option>
                        <option value="USER.LOGOUT">USER.LOGOUT</option>
                        <option value="USER.CREATE">USER.CREATE</option>
                        <option value="USER.DELETE">USER.DELETE</option>
                        <option value="USER.DISABLE">USER.DISABLE</option>
                        <option value="USER.UPDATE">USER.UPDATE</option>
                       
                        <option value="TEMPLATE.CREATE">TEMPLATE.CREATE</option>
                        <option value="TEMPLATE.DELETE">TEMPLATE.DELETE</option>
                        <option value="TEMPLATE.UPDATE">TEMPLATE.UPDATE</option>                        
                        <option value="TEMPLATE.DOWNLOAD.START">TEMPLATE.DOWNLOAD.START</option>
                        <option value="TEMPLATE.DOWNLOAD.SUCCESS">TEMPLATE.DOWNLOAD.SUCCESS</option>
                        <option value="TEMPLATE.DOWNLOAD.FAILED">TEMPLATE.DOWNLOAD.FAILED</option>
                        <option value="TEMPLATE.COPY">TEMPLATE.COPY</option> 
                        <option value="TEMPLATE.EXTRACT">TEMPLATE.EXTRACT</option>
                        <option value="TEMPLATE.UPLOAD">TEMPLATE.UPLOAD</option>
                        <option value="TEMPLATE.CLEANUP">TEMPLATE.CLEANUP</option>
                       
                        <option value="VOLUME.CREATE">VOLUME.CREATE</option>
                        <option value="VOLUME.DELETE">VOLUME.DELETE</option>
                        <option value="VOLUME.ATTACH">VOLUME.ATTACH</option>
                        <option value="VOLUME.DETACH">VOLUME.DETACH</option>
                        <option value="VOLUME.EXTRACT">VOLUME.EXTRACT</option>
                        <option value="VOLUME.UPLOAD">VOLUME.UPLOAD</option>
                       
                        <option value="DOMAIN.CREATE">DOMAIN.CREATE</option>
                        <option value="DOMAIN.DELETE">DOMAIN.DELETE</option>
                        <option value="DOMAIN.UPDATE">DOMAIN.UPDATE</option>
                        
                        <option value="SNAPSHOT.CREATE">SNAPSHOT.CREATE</option>
                        <option value="SNAPSHOT.DELETE">SNAPSHOT.DELETE</option>
                        <option value="SNAPSHOTPOLICY.CREATE">SNAPSHOTPOLICY.CREATE</option>
                        <option value="SNAPSHOTPOLICY.UPDATE">SNAPSHOTPOLICY.UPDATE</option>
                        <option value="SNAPSHOTPOLICY.DELETE">SNAPSHOTPOLICY.DELETE</option>
                        
                        <option value="ISO.CREATE">ISO.CREATE</option>
                        <option value="ISO.DELETE">ISO.DELETE</option>
                        <option value="ISO.COPY">ISO.COPY</option>
                        <option value="ISO.ATTACH">ISO.ATTACH</option>
                        <option value="ISO.DETACH">ISO.DETACH</option>
                        <option value="ISO.EXTRACT">ISO.EXTRACT</option>
                        <option value="ISO.UPLOAD">ISO.UPLOAD</option>
                        
                        <option value="SSVM.CREATE">SSVM.CREATE</option>
                        <option value="SSVM.DESTROY">SSVM.DESTROY</option>
                        <option value="SSVM.START">SSVM.START</option>
                        <option value="SSVM.STOP">SSVM.STOP</option>
                        <option value="SSVM.REBOOT">SSVM.REBOOT</option>
                        <option value="SSVM.HA">SSVM.HA</option>
                                                 
                        <option value="SERVICEOFFERING.CREATE">SERVICEOFFERING.CREATE</option>
                        <option value="SERVICEOFFERING.UPDATE">SERVICEOFFERING.UPDATE</option>
                        <option value="SERVICEOFFERING.DELETE">SERVICEOFFERING.DELETE</option>                                     
                        
                        <option value="DISK.OFFERING.CREATE">DISK.OFFERING.CREATE</option>
                        <option value="DISK.OFFERING.EDIT">DISK.OFFERING.EDIT</option>
                        <option value="DISK.OFFERING.DELETE">DISK.OFFERING.DELETE</option>
                       
                        <option value="NETWORK.OFFERING.CREATE">NETWORK.OFFERING.CREATE</option>
                        <option value="NETWORK.OFFERING.EDIT">NETWORK.OFFERING.EDIT</option>
                        <option value="NETWORK.OFFERING.DELETE">NETWORK.OFFERING.DELETE</option>
                        
                        <option value="POD.CREATE">POD.CREATE</option>
                        <option value="POD.EDIT">POD.EDIT</option>
                        <option value="POD.DELETE">POD.DELETE</option>
                       
                        <option value="ZONE.CREATE">ZONE.CREATE</option>
                        <option value="ZONE.EDIT">ZONE.EDIT</option>
                        <option value="ZONE.DELETE">ZONE.DELETE</option>
                       
                        <option value="VLAN.IP.RANGE.CREATE">VLAN.IP.RANGE.CREATE</option>
                        <option value="VLAN.IP.RANGE.DELETE">VLAN.IP.RANGE.DELETE</option>
                       
                        <option value="CONFIGURATION.VALUE.EDIT">CONFIGURATION.VALUE.EDIT</option>
                       
                        <option value="SG.AUTH.INGRESS">SG.AUTH.INGRESS</option>
                        <option value="SG.REVOKE.INGRESS">SG.REVOKE.INGRESS</option>
                       
                        <option value="HOST.RECONNECT">HOST.RECONNECT</option>
                       
                        <option value="MAINT.CANCEL">MAINT.CANCEL</option>
                        <option value="MAINT.CANCEL.PS">MAINT.CANCEL.PS</option>
                        <option value="MAINT.PREPARE">MAINT.PREPARE</option>
                        <option value="MAINT.PREPARE.PS">MAINT.PREPARE.PS</option>
                       
                        <option value="VPN.REMOTE.ACCESS.CREATE">VPN.REMOTE.ACCESS.CREATE</option>
                        <option value="VPN.REMOTE.ACCESS.DESTROY">VPN.REMOTE.ACCESS.DESTROY</option>
                        <option value="VPN.USER.ADD">VPN.USER.ADD</option>
                        <option value="VPN.USER.REMOVE">VPN.USER.REMOVE</option>
                        
                        <option value="NETWORK.RESTART">NETWORK.RESTART</option>
                        
                        <option value="UPLOAD.CUSTOM.CERTIFICATE">UPLOAD.CUSTOM.CERTIFICATE</option>
                        
                        <option value="STATICNAT.ENABLE">STATICNAT.ENABLE</option>
                        <option value="STATICNAT.DISABLE">STATICNAT.DISABLE</option>
                        
                    </select>
                </li>
                <li>
                    <select class="select" id="adv_search_level">
                        <option value=""><fmt:message key="label.by.level"/></option>
                        <option value="INFO"><fmt:message key="label.info"/></option>
                        <option value="WARN"><fmt:message key="label.warn"/></option>
                        <option value="ERROR"><fmt:message key="label.error"/></option>
                    </select>
                </li>
                <li id="adv_search_domain_li" style="display: none;">
                    <input class="text textwatermark" type="text" id="domain" value='<fmt:message key="label.by.domain" />' />
                    <div id="domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
                    <!--
                    <select class="select" id="adv_search_domain">
                    </select>
                    -->
                </li>
                <li id="adv_search_account_li" style="display: none;">
                    <input class="text textwatermark" type="text" id="adv_search_account" value='<fmt:message key="label.by.account" />' />
                </li>
                <li>
                    <input class="text textwatermark" type="text" id="adv_search_startdate" value='<fmt:message key="label.by.start.date" />' />
                </li>
                <li>
                    <input class="text textwatermark" type="text" id="adv_search_enddate" value='<fmt:message key="label.by.end.date" />' />
                </li>
            </ol>
            </form>
        </div>
    </div>
    <!-- advanced search popup (end) -->
</div>
