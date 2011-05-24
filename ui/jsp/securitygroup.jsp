<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 	
	'label.action.edit.security.group' : '<fmt:message key="label.action.edit.security.group"/>',
	'label.action.delete.security.group' : '<fmt:message key="label.action.delete.security.group"/>',
	'label.action.delete.security.group.processing' : '<fmt:message key="label.action.delete.security.group.processing"/>',
	'message.action.delete.security.group' : '<fmt:message key="message.action.delete.security.group"/>',
	'label.action.delete.ingress.rule' : '<fmt:message key="label.action.delete.ingress.rule"/>',
	'label.action.delete.ingress.rule.processing' : '<fmt:message key="label.action.delete.ingress.rule.processing"/>',
	'message.action.delete.ingress.rule' : '<fmt:message key="message.action.delete.ingress.rule"/>'	
};	
</script>

<div class="main_title" id="right_panel_header">
    
    <div class="main_titleicon">
        <img src="images/title_secgroupicons.gif" /></div>
    
    <h1>
        <fmt:message key="label.security.group"/>
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
        <div class="content_tabs off" id="tab_ingressrule">
            <fmt:message key="label.ingress.rule"/></div>
    </div>
    <!--Details tab (start)-->
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
	            	<div id="grid_header_title" class="grid_header_title">(title)</div>
	                <div id="action_link" class="grid_actionbox"><p><fmt:message key="label.actions"/></p>
	                    <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
	                        <ul class="actionsdropdown_boxlist" id="action_list">
	                        	<li><fmt:message key="label.no.actions"/></li>
	                        </ul>
	                    </div>
	                </div>
	                <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
	                display: none;">
	                    <div class="gridheader_loader" id="Div1">
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
	                        <fmt:message key="label.display.text"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="displaytext">
	                    </div>          
	                </div>
	            </div>	            
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.domain"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="domain">
	                    </div>	                    
	                </div>
	            </div>   	            
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.account"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="account">
	                    </div>          
	                </div>
	            </div>	 
	        </div>        
	        <div class="grid_botactionpanel">
	        	<div class="gridbot_buttons" id="save_button" style="display:none;"><fmt:message key="label.save"/></div>
	            <div class="gridbot_buttons" id="cancel_button" style="display:none;"><fmt:message key="label.cancel"/></div>
	        </div>  
        </div>
    </div>
    <!--Details tab (end)-->
    
    <!--Ingress Rule tab (start)-->
    <div style="display: none;" id="tab_content_ingressrule">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">
        </div>
    </div> 
    <!--Ingress Rule tab (end)-->    
</div>

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="add_securitygroup_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Security Group" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.security.group"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_ingressrule_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Ingress Rule" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.ingress.rule"/>
            </div>
        </div>
    </div>    
</div>
<!--  top buttons (end) -->

<!--  Ingress Rule tab template (begin) -->
<div class="grid_container" id="ingressrule_tab_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/> &hellip;
            </p>
        </div>       
    </div>
    
    <div class="grid_rows" id="after_action_info_container" style="display:none">
        <div class="grid_row_cell" style="width: 90%; border: none;">
            <div class="row_celltitles">
                <strong id="after_action_info">Message will appear here</strong></div>
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
                <fmt:message key="label.protocol"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="protocol">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.endpoint.or.operation"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="endpoint">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.cidr.account"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="cidr">
            </div>
        </div>
    </div>   
</div>
<!--  Ingress Rule tab template (end) -->

<!-- Add Security Group Dialog (begin) -->
<div id="dialog_add_security_group" title='<fmt:message key="label.add.security.group"/>' style="display: none">
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <fmt:message key="label.name"/>:</label>
                <input class="text" type="text" id="name" />
                <div id="name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.description"/>:</label>
                <input class="text" type="text" id="description" />
                <div id="description_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Add Security Group Dialog (end) -->

<!-- Add Ingress Rule Dialog (begin) -->
<div id="dialog_add_ingress_rule" title='<fmt:message key="label.add.ingress.rule"/>' style="display: none">
    <div class="dialog_formcontent">
        <form action="#" method="post">
        <ol>
            <li>
                <label for="protocol">
                    <fmt:message key="label.protocol"/></label>
                <select class="select" id="protocol">
                    <option value="TCP">TCP</option>
                    <option value="UDP">UDP</option>
                    <option value="ICMP">ICMP</option>
                </select>
            </li>
            <li id="start_port_container">
                <label for="start_port">
                    <fmt:message key="label.start.port"/>:</label>
                <input class="text" type="text" id="start_port" />
                <div id="start_port_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="end_port_container">
                <label for="end_port">
                    <fmt:message key="label.end.port"/>:</label>
                <input class="text" type="text" id="end_port" />
                <div id="end_port_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="icmp_type_container">
                <label for="start_port">
                    <fmt:message key="label.type"/>:</label>
                <input class="text" type="text" id="icmp_type" />
                <div id="icmp_type_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="icmp_code_container">
                <label for="end_port">
                    <fmt:message key="label.code"/>:</label>
                <input class="text" type="text" id="icmp_code" />
                <div id="icmp_code_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
            	<input type="radio" class="radio" style="margin:-2px 5px 0 0;" name="ingress_rule_type" value="cidr" checked="checked" />
                <label style="width:80px;">

                    <fmt:message key="label.add.by.cidr"/>:</label>
                <div id="cidr_container" style="float:left; width:170px;">
                </div>
                <a style="margin-left: 110px; display: inline;" id="add_more_cidr" href="#">Add more</a>
            </li>
            <li style="margin-top: 7px;">
            	<input class="radio" style="margin:-2px 5px 0 0;" type="radio" name="ingress_rule_type" value="account_securitygroup">
                <label style="width:80px;">
                    <fmt:message key="label.add.by.group"/>:</label>
                <p style="color: #999;">
                    <fmt:message key="label.account.name"/></p>
                <p style="margin-left: 25px; display: inline; color: #999;">
                    <fmt:message key="label.security.group.name"/></p>
                <div id="account_securitygroup_container" style="float:left; width:200px;">
                </div>
                <a style="margin-left: 110px; display: inline;" id="add_more_account_securitygroup"
                    href="#"><fmt:message key="label.add.more"/></a></li>
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
<!-- Add Ingress Rule Dialog (end) -->

<!-- Add Ingress Rule Dialog - CIDR template (begin) -->
<div id="cidr_template" class="cidr_template" style="display: none">
    <input class="text" type="text" id="cidr" />
    <div id="cidr_errormsg" class="dialog_formcontent_errormsg" style="display: none;
        margin: 0;">
    </div>
</div>
<!-- Add Ingress Rule Dialog - CIDR template (end) -->

<!-- Add Ingress Rule Dialog - Account/Security Group template (begin) -->
<div id="account_securitygroup_template" class="account_securitygroup_template" style="width: 200px;
    height: auto; float: left; display: none">
    <input class="text" style="width: 80px" type="text" id="account" />
    <span>/</span>
    <input class="text" style="width: 80px" type="text" id="securitygroup" />
    <div id="account_securitygroup_template_errormsg" class="dialog_formcontent_errormsg"
        style="display: none; margin: 0;">
    </div>
</div>
<!-- Add Ingress Rule Dialog - Account/Security Group template (end) -->


<div id="hidden_container">
    <!-- advanced search popup (begin) -->
    <div class="adv_searchpopup_bg" id="advanced_search_popup" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>                
                <li id="adv_search_domain_li" style="display: none;">
                    <input class="text textwatermark" type="text" id="domain" value='<fmt:message key="label.by.domain" />' />
                    <div id="domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
                    <!--  
                    <select class="select" id="adv_search_domain">
                    </select>
                    -->
                </li>
            </ol>
            </form>
        </div>
    </div>
    <!-- advanced search popup (end) -->
</div>

