<!--
<script type="text/javascript" src="scripts/cloud.core.event.js"></script>
-->

<%@ page import="java.util.*" %>
<%@ page import="com.cloud.utils.*" %>

<%

    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<!-- template detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <!--  
    <div class="main_titleicon">
        <img src="images/instancetitle_icons.gif" alt="Instance" /></div>
    -->
    <h1>Template
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
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Zone")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Name")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Display.Text")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Status")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Password.Enabled")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Public")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Featured")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Cross.Zones")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("OS.Type")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Account")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Created")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Size")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="username">
                </div>
            </div>
        </div>
    </div>
</div>
<!-- template detail panel (end) -->