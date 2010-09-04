<!--
<script type="text/javascript" src="scripts/cloud.core.alert.js"></script>
-->

<%@ page import="java.util.*" %>
<%@ page import="com.cloud.utils.*" %>

<%

    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<!-- alert detail panel (begin) -->
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
                    <%=t.t("Sent")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="sent">
                </div>
            </div>
        </div>        
    </div>
</div>
<!-- alert detail panel (end) -->