<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 	
	'label.action.start.systemvm' : '<fmt:message key="label.action.start.systemvm"/>',
	'label.action.start.systemvm.processing' : '<fmt:message key="label.action.start.systemvm.processing"/>',
	'message.action.start.systemvm' : '<fmt:message key="message.action.start.systemvm"/>',
	'label.action.stop.systemvm' : '<fmt:message key="label.action.stop.systemvm"/>',
	'label.action.stop.systemvm.processing' : '<fmt:message key="label.action.stop.systemvm.processing"/>',
	'message.action.stop.systemvm' : '<fmt:message key="message.action.stop.systemvm"/>',
	'label.action.reboot.systemvm' : '<fmt:message key="label.action.reboot.systemvm"/>',
	'label.action.reboot.systemvm.processing' : '<fmt:message key="label.action.reboot.systemvm.processing"/>',
	'message.action.reboot.systemvm' : '<fmt:message key="message.action.reboot.systemvm"/>',
	'label.action.destroy.systemvm': '<fmt:message key="label.action.destroy.systemvm"/>',
    'label.action.destroy.systemvm.processing': '<fmt:message key="label.action.destroy.systemvm.processing"/>',
    'message.migrate.systemvm.confirm' : '<fmt:message key="message.migrate.systemvm.confirm"/>',
	'label.action.migrate.systemvm': '<fmt:message key="label.action.migrate.systemvm"/>',
    'label.action.migrate.systemvm.processing': '<fmt:message key="label.action.migrate.systemvm.processing"/>',    
    'message.action.destroy.systemvm': '<fmt:message key="message.action.destroy.systemvm"/>',
    'label.full': '<fmt:message key="label.full"/>',
	'label.available': '<fmt:message key="label.available"/>'
};	
</script>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_systemvmicon.gif" /></div>
    <h1>
        <fmt:message key="label.system.vm"/>
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on" id="tab_details">
            <fmt:message key="label.details"/></div>
    </div>
    <!-- Details tab (start)-->
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
	            <div class="grid_header">
	                <div id="grid_header_title" class="grid_header_title">
	                    (title)</div>
	                <div id="action_link" class="grid_actionbox"><p><fmt:message key="label.actions"/></p>
	                    <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
	                        <ul class="actionsdropdown_boxlist" id="action_list">
	                            <li>
	                                <fmt:message key="label.no.actions"/></li>
	                        </ul>
	                    </div>
	                </div>
	                <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
	                    display: none;">
	                    <div class="gridheader_loader" id="icon">
	                    </div>
	                    <p id="description">
	                        <fmt:message key="label.detaching.disk"/> &hellip;</p>
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
	                        <fmt:message key="label.id"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="id">
	                    </div>
	                </div>
	            </div>	            
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.system.vm.type"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="systemvmtype">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.zone"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="zonename">
	                    </div>
	                </div>
	            </div>	           
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.name"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="name">
	                    </div>
	                </div>
	            </div>	            
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.public.ip"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="publicip">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.private.ip"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="privateip">
	                    </div>
	                </div>
	            </div>
				<div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.linklocal.ip"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="linklocalip">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.host"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="hostname">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.gateway"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="gateway">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.created"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="created">
	                    </div>
	                </div>
	            </div>	
	            <div class="grid_rows odd" id="activeviewersessions_container" style="display:none">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.active.sessions"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="activeviewersessions">
	                    </div>
	                </div>
	            </div> 
	        </div>
        </div>  
    </div>
    <!-- Details tab (end)-->
</div>

<!-- Migrate System VM Dialog -->
<div id="dialog_migrate_systemvm" title='<fmt:message key="label.action.migrate.systemvm" />' style="display: none">
    <p> 
		<fmt:message key="message.migrate.systemvm.confirm" />
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <fmt:message key="label.migrate.systemvm.to" />:</label>
                <select class="select" id="migrate_instance_hosts">
                </select>
                <div id="migrate_vm_hosts_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>

<!-- view console template (begin)  -->
<div id="view_console_template" style="display: none">
    <div class="vm_consolebox" id="box0">
    </div>
    <div class="vm_consolebox" id="box1" style="display: none">
    </div>
</div>
<!-- view console template (end)  -->

<div id="hidden_container">
    <!-- advanced search popup (begin) -->
    <div id="advanced_search_popup" class="adv_searchpopup_bg" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>               
                <li>
                    <select class="select" id="adv_search_state">
                        <option value=""><fmt:message key="label.by.state"/></option>
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
                    <select class="select" id="adv_search_zone">
                    </select>
                </li>
                <li id="adv_search_pod_li" style="display: none;">
                    <select class="select" id="adv_search_pod">
                    </select>
                </li>
            </ol>
            </form>
        </div>
    </div>
    <!-- advanced search popup (end) -->
</div>