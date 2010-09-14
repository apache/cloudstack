
<!--
<script type="text/javascript" src="scripts/cloud.core.account.js"></script>
-->

<%@ page import="java.util.*" %>
<%@ page import="com.cloud.utils.*" %>

<%

    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<!-- account detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/accountstitle_icons.gif" alt="Accounts" /></div>
    <h1>Accounts</h1>
</div>

<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container" style="display:none">
        <p id="after_action_info"></p>
    </div>
    <div class="tabbox" style="margin-top:15px;">
        <div class="content_tabs on">
            <%=t.t("Details")%></div>
    </div>
    <div class="grid_actionpanel">
    	<div class="grid_actionbox">
        	<div class="grid_actionsdropdown_box" style="display:none;">
                	<ul class="actionsdropdown_boxlist" id="action_list">
                    	<li> <a href="#"> Delete </a> </li>
                        <li> <a href="#"> Attach Disk </a> </li>
                    </ul>
            </div>
        </div>   
        <div class="grid_editbox"></div>
        <div class="gridheader_loaderbox" style="border:1px solid #999; display:none;">
        	<div class="gridheader_loader"></div>
            <p>Detaching Disk &hellip;</p>
        </div>
        
        <div class="gridheader_message" style="border:1px solid #999; display:block;">
            <p>Disk has been succesfully dettached &hellip;</p>
            <div class="close_button"></div>
        </div>  
        
        <div class="gridheader_message error" style="border:1px solid #999; display:none;">
            <p>Some problem occured while dettaching disk &hellip;</p>
            <div class="close_button"></div>
        </div>  
    </div>
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
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Role")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="role">
                </div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Account")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="account">
                    </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Domain")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="domain">
                </div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("VMs")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="vm_total">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("IPs")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="ip_total">
                </div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Bytes.Received")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="bytes_received">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Bytes.Sent")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="bytes_sent">
                </div>
            </div>
        </div>     
        <div class="grid_rows even">
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
</div>

<!-- account detail panel (end) -->