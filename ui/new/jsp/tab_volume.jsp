<!--
<script type="text/javascript" src="scripts/cloud.core.volume.js"></script>
-->

<%@ page import="java.util.*" %>
<%@ page import="com.cloud.utils.*" %>

<%

    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<!-- volume detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <!--  
    <div class="main_titleicon">
        <img src="images/instancetitle_icons.gif" alt="Instance" /></div>
    -->
    <h1>Volume
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container" style="display:none">
        <p id="after_action_info"></p>
    </div>
    <div class="tabbox" style="margin-top:15px;">
        <div class="content_tabs on">
            <%=t.t("Details")%></div>        
    </div>
    <div class="grid_container">
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
                    <%=t.t("Name")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="name">
                    </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Type")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="type">
                </div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Zone")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="zonename">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Instance.Name")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="vm_name">
                </div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Device.ID")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="device_id">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Size")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="size">
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
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Created")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="created">
                </div>
            </div>
        </div>       
         <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Storage")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="storage">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Account")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="account">
                </div>
            </div>
        </div>           
    </div>
</div>
<!-- volume detail panel (end) -->