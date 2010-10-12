<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>
<!-- account detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_accountsicon.gif" alt="Accounts" /></div>
    <h1>
        Accounts</h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on">
            <%=t.t("Details")%></div>
    </div> 
    <div id="tab_content_details">
        <div class="grid_actionpanel">
            <div class="grid_actionbox" id="action_link">
                <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                    <ul class="actionsdropdown_boxlist" id="action_list">
                        <li><%=t.t("no.available.actions")%></li>
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
        <div class="grid_container">
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
</div>
<!-- account detail panel (end) -->


<div id="dialog_resource_limits" title="Resource Limits" style="display:none">
	<p>
	    <%=t.t("please.specify.limits.to.the.various.resources.-1.means.the.resource.has.no.limits")%>	    
	</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label><%=t.t("instance.limit")%>:</label>
					<input class="text" type="text" name="limits_vm" id="limits_vm" value="-1" />
					<div id="limits_vm_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label><%=t.t("public.ip.limit")%>:</label>
					<input class="text" type="text" name="limits_ip" id="limits_ip" value="-1" />
					<div id="limits_ip_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label><%=t.t("disk.volume.limit")%>:</label>
					<input class="text" type="text" name="limits_volume" id="limits_volume" value="-1" />
					<div id="limits_volume_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label><%=t.t("snapshot.limit")%>:</label>
					<input class="text" type="text" name="limits_snapshot" id="limits_snapshot" value="-1" />
					<div id="limits_snapshot_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label><%=t.t("template.limit")%>:</label>
					<input class="text" type="text" name="limits_template" id="limits_template" value="-1" />
					<div id="limits_template_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
			</ol>
		</form>
	</div>
</div>

<div id="dialog_disable_account" title="Disable account" style="display:none">
    <p>
        <%=t.t("please.confirm.you.want.to.disable.account.that.will.prevent.account.access.to.the.cloud.and.shut.down.all.existing.virtual.machines")%>        
    </p>
</div>

<div id="dialog_lock_account" title="Lock account" style="display:none">
    <p>
        <%=t.t("please.confirm.you.want.to.lock.account.that.will.prevent.account.access.to.the.cloud")%>        
    </p>
</div>

<div id="dialog_enable_account" title="Enable account" style="display:none">
    <p>
        <%=t.t("please.confirm.you.want.to.enable.account")%>        
    </p>
</div>