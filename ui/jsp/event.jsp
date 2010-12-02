<%@ page import="java.util.*" %>
<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>


<!-- event detail panel (begin) -->
<div class="main_title" id="right_panel_header">
   
    <div class="main_titleicon">
        <img src="images/title_eventsicon.gif" alt="Event" /></div>
   
    <h1>Event
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top:15px;">
        <div class="content_tabs on">
            <%=t.t("Details")%></div>        
    </div>    
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
		        <div class="grid_rows odd">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("id")%>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="id">
		                </div>
		            </div>
		        </div>
		        <div class="grid_rows even">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("Initiated.By")%>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="username">
		                </div>
		            </div>
		        </div>
		        <div class="grid_rows odd">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("Owner.Account")%>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="account">
		                    </div>
		            </div>
		        </div>
		        <div class="grid_rows even">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("Type")%>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="type">
		                </div>
		            </div>
		        </div>
		        <div class="grid_rows odd">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("Level")%>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="level">
		                </div>
		            </div>
		        </div>
		        <div class="grid_rows even">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("Description")%>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="description">
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
		        <div class="grid_rows even">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("Date")%>:</div>
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

<!-- advanced search template (begin) -->
<div id="advanced_search_template" class="adv_searchpopup" style="display: none;">
    <div class="adv_searchformbox">
        <h3>
            Advance Search</h3>
        <a id="advanced_search_close" href="#">Close </a>
        <form action="#" method="post">
        <ol>
            <li>
                <label>
                    Type:</label>
                <select class="select" id="adv_search_type">
                    <option value=""></option>
                    <option value="VM.CREATE">VM.CREATE</option>
                    <option value="VM.DESTROY">VM.DESTROY</option>
                    <option value="VM.START">VM.START</option>
                    <option value="VM.STOP">VM.STOP</option>
                    <option value="VM.REBOOT">VM.REBOOT</option>
                    <option value="VM.DISABLEHA">VM.DISABLEHA</option>
                    <option value="VM.ENABLEHA">VM.ENABLEHA</option>
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
                    <option value="PF.SERVICE.APPLY">PF.SERVICE.APPLY</option>
                    <option value="PF.SERVICE.REMOVE">PF.SERVICE.REMOVE</option>
                    <option value="SECGROUP.APPLY">SECGROUP.APPLY</option>
                    <option value="SECGROUP.REMOVE">SECGROUP.REMOVE</option>
                    <option value="LB.CREATE">LB.CREATE</option>
                    <option value="LB.DELETE">LB.DELETE</option>
                    <option value="USER.LOGIN">USER.LOGIN</option>
                    <option value="USER.LOGOUT">USER.LOGOUT</option>
                    <option value="USER.CREATE">USER.CREATE</option>
                    <option value="USER.DELETE">USER.DELETE</option>
                    <option value="USER.UPDATE">USER.UPDATE</option>
                    <option value="TEMPLATE.CREATE">TEMPLATE.CREATE</option>
                    <option value="TEMPLATE.DELETE">TEMPLATE.DELETE</option>
                    <option value="TEMPLATE.UPDATE">TEMPLATE.UPDATE</option>
                    <option value="TEMPLATE.COPY">TEMPLATE.COPY</option>
                    <option value="TEMPLATE.DOWNLOAD.START">TEMPLATE.DOWNLOAD.START</option>
                    <option value="TEMPLATE.DOWNLOAD.SUCCESS">TEMPLATE.DOWNLOAD.SUCCESS</option>
                    <option value="TEMPLATE.DOWNLOAD.FAILED">TEMPLATE.DOWNLOAD.FAILED</option>
                    <option value="VOLUME.CREATE">VOLUME.CREATE</option>
                    <option value="VOLUME.DELETE">VOLUME.DELETE</option>
                    <option value="VOLUME.ATTACH">VOLUME.ATTACH</option>
                    <option value="VOLUME.DETACH">VOLUME.DETACH</option>
                    <option value="SERVICEOFFERING.CREATE">SERVICEOFFERING.CREATE</option>
                    <option value="SERVICEOFFERING.UPDATE">SERVICEOFFERING.UPDATE</option>
                    <option value="SERVICEOFFERING.DELETE">SERVICEOFFERING.DELETE</option>
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
                    <option value="SSVM.CREATE">SSVM.CREATE</option>
                    <option value="SSVM.DESTROY">SSVM.DESTROY</option>
                    <option value="SSVM.START">SSVM.START</option>
                    <option value="SSVM.STOP">SSVM.STOP</option>
                    <option value="SSVM.REBOOT">SSVM.REBOOT</option>
                    <option value="SSVM.HA">SSVM.HA</option>
                </select>
            </li>
            <li>
                <label>
                    Level:</label>
                <select class="select" id="adv_search_level">
                    <option value=""></option>
                    <option value="INFO">INFO</option>
                    <option value="WARN">WARN</option>
                    <option value="ERROR">ERROR</option>
                </select>
            </li>
            <li id="adv_search_domain_li" style="display: none;">
                <label>
                    Domain:</label>
                <select class="select" id="adv_search_domain">
                </select>
            </li>
            <li id="adv_search_account_li" style="display: none;">
                <label>
                    Account:</label>
                <input class="text" type="text" id="adv_search_account" />
            </li>
            <li>
                <label>
                    Start Date:</label>
                <input class="text" type="text" id="adv_search_startdate" />
            </li>
            <li>
                <label>
                    End Date:</label>
                <input class="text" type="text" id="adv_search_enddate" />
            </li>
        </ol>
        </form>
        <div class="adv_search_actionbox">
            <div class="adv_searchpopup_button" id="adv_search_button">
            </div>
        </div>
    </div>
</div>
<!-- advanced search template (end) -->