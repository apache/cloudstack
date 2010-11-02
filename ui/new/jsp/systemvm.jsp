<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_systemvmicon.gif" alt="System VM" /></div>
    <h1>
        System VM
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on" id="tab_details">
            <%=t.t("details")%></div>
    </div>
    <!-- Details tab (start)-->
    <div id="tab_content_details">
        <div class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    Loading &hellip;</p>
            </div>
        </div>
        <div class="grid_container">
            <div class="grid_header">
                <div id="grid_header_title" class="grid_header_title">
                    (title)</div>
                <div id="action_link" class="grid_actionbox" id="action_link">
                    <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                        <ul class="actionsdropdown_boxlist" id="action_list">
                            <li>
                                <%=t.t("no.available.actions")%></li>
                        </ul>
                    </div>
                </div>
                <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
                    display: none;">
                    <div class="gridheader_loader" id="icon">
                    </div>
                    <p id="description">
                        Detaching Disk &hellip;</p>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="vm_statusbox">
                    <div id="view_console_container" style="float: left;">
                        <div id="view_console_template" style="display: block">
                            <div class="vm_consolebox" id="box0">
                            </div>
                            <div class="vm_consolebox" id="box1" style="display: none">
                            </div>
                        </div>
                    </div>
                    <div class="vm_status_textbox">
                        <div class="vm_status_textline green" id="state">
                        </div>
                        <br />
                        <p id="ipAddress">
                        </p>
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("system.vm.type")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="systemvmtype">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("zone")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="zonename">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("ID")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="id">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("name")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="name">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("active.sessions")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="activeviewersessions">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("public.ip")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="publicip">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("private.ip")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="privateip">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("host")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="hostname">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("gateway")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="gateway">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("created")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="created">
                    </div>
                </div>
            </div>
        </div>
    </div>
    <!-- Details tab (end)-->
</div>

<!-- view console template (begin)  -->
<div id="view_console_template" style="display: none">
    <div class="vm_consolebox" id="box0">
    </div>
    <div class="vm_consolebox" id="box1" style="display: none">
    </div>
</div>
<!-- view console template (end)  -->
<!-- dialogs -->
<div id="dialog_confirmation_start_systemVM" title="Confirmation" style="display: none">
    <p>
        <%=t.t("please.confirm.you.want.to.start.systemVM")%>
    </p>
</div>
<div id="dialog_confirmation_stop_systemVM" title="Confirmation" style="display: none">
    <p>
        <%=t.t("please.confirm.you.want.to.stop.systemVM")%>
    </p>
</div>
<div id="dialog_confirmation_reboot_systemVM" title="Confirmation" style="display: none">
    <p>
        <%=t.t("please.confirm.you.want.to.reboot.systemVM")%>
    </p>
</div>
