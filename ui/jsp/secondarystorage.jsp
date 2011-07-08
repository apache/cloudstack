<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 	
	'label.action.delete.secondary.storage' : '<fmt:message key="label.action.delete.secondary.storage"/>',
	'label.action.delete.secondary.storage.processing' : '<fmt:message key="label.action.delete.secondary.storage.processing"/>',
	'message.action.delete.secondary.storage' : '<fmt:message key="message.action.delete.secondary.storage"/>'
};	
</script>

<div class="main_title" id="right_panel_header">
   
    <div class="main_titleicon">
        <img src="images/title_secondarystorageicon.gif"/></div>
   
    <h1><fmt:message key="label.secondary.storage"/>
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
		        <div class="grid_header">
	                <div id="grid_header_title" class="grid_header_title">
	                    (title)</div>
	                <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
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
	                        <fmt:message key="label.waiting"/> &hellip;</p>
	                </div>
	            </div>
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
		                    <fmt:message key="label.name"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="name">
		                </div>
		            </div>
		        </div>			        
				<div class="grid_rows odd">
			        <div class="grid_row_cell" style="width: 20%;">
			            <div class="row_celltitles">
			                <fmt:message key="label.zone"/>:</div>
			        </div>
			        <div class="grid_row_cell" style="width: 79%;">
			            <div class="row_celltitles" id="zonename">
			            </div>
			        </div>
			    </div>
			    <div class="grid_rows even">
			        <div class="grid_row_cell" style="width: 20%;">
			            <div class="row_celltitles">
			                <fmt:message key="label.type"/>:</div>
			        </div>
			        <div class="grid_row_cell" style="width: 79%;">
			            <div class="row_celltitles" id="type">
			            </div>
			        </div>
			    </div>
			    <div class="grid_rows odd">
			        <div class="grid_row_cell" style="width: 20%;">
			            <div class="row_celltitles">
			                <fmt:message key="label.ip.address"/>:</div>
			        </div>
			        <div class="grid_row_cell" style="width: 79%;">
			            <div class="row_celltitles" id="ipaddress">
			            </div>
			        </div>
			    </div>				   
			    <div class="grid_rows even">
			        <div class="grid_row_cell" style="width: 20%;">
			            <div class="row_celltitles">
			                <fmt:message key="label.last.disconnected"/>:</div>
			        </div>
			        <div class="grid_row_cell" style="width: 79%;">
			            <div class="row_celltitles" id="disconnected">
			            </div>
			        </div>
			    </div>			    
			    <!-- 
			    <div class="grid_rows old">
			        <div class="grid_row_cell" style="width: 20%;">
			            <div class="row_celltitles">
			                <fmt:message key="label.state"/>:</div>
			        </div>
			        <div class="grid_row_cell" style="width: 79%;">
			            <div class="row_celltitles" id="state">
			            </div>
			        </div>
			    </div>
			     -->
		    </div>
		</div>    
    </div>     
</div>

<!--  top buttons (begin) -->
<div id="top_buttons">     
    <div class="actionpanel_button_wrapper" id="add_secondarystorage_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt='<fmt:message key="label.add.secondary.storage"/>' /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.secondary.storage"/>
            </div>
        </div>
    </div>
</div>
<!--  top buttons (end) -->

<!-- Add Secondary Storage Dialog (begin) -->
<div id="dialog_add_secondarystorage" title='<fmt:message key="label.add.secondary.storage"/>' style="display: none">
    <p>
		<fmt:message key="message.add.secondary.storage"/>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form1">
        <ol>
            <li>
                <label>
                    <fmt:message key="label.nfs.server"/>:</label>
                <input class="text" type="text" name="nfs_server" id="nfs_server" />
                <div id="nfs_server_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="path">
                    <fmt:message key="label.path"/>:</label>
                <input class="text" type="text" name="path" id="path" />
                <div id="path_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
    <!--Loading box-->
    <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display: none;">
        <div class="ui_dialog_loader">
        </div>
        <p>
            <fmt:message key="label.adding"/>....</p>
    </div>
    <!--Confirmation msg box-->
    <!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
    <div id="info_container" class="ui_dialog_messagebox error" style="display: none;">
        <div id="icon" class="ui_dialog_msgicon error">
        </div>
        <div id="info" class="ui_dialog_messagebox_text error">
            (info)</div>
    </div>
</div>
<!-- Add Secondary Storage Dialog (end) -->
