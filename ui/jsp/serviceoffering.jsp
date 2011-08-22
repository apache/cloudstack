<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 	
	'label.action.edit.service.offering' : '<fmt:message key="label.action.edit.service.offering"/>',
	'label.action.delete.service.offering' : '<fmt:message key="label.action.delete.service.offering"/>',
	'label.action.delete.service.offering.processing' : '<fmt:message key="label.action.delete.service.offering.processing"/>',
	'message.action.delete.service.offering' : '<fmt:message key="message.action.delete.service.offering"/>'
};	
</script>

<div class="main_title" id="right_panel_header">
    
    <div class="main_titleicon">
        <img src="images/title_serviceofferingicon.gif"/></div>
    
    <h1>
        <fmt:message key="label.service.offering"/>
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
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
	                    <input class="text" id="name_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="name_edit_errormsg" style="display:none"></div>        
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
	                    <input class="text" id="displaytext_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="displaytext_edit_errormsg" style="display:none"></div>            
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.storage.type"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="storagetype">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.cpu"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="cpu">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.memory"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="memory">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="network.rate"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="network_rate">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.offer.ha"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="offerha">
	                    </div>	                  
	                </div>
	            </div>
	            
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.CPU.cap"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="limitcpuuse">
	                    </div>	                  
	                </div>
	            </div>
	                       
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.storage.tags"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="tags">
	                    </div>	
	                </div>
	            </div>
	            	            
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.host.tags"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="hosttags">
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
	                    <select class="select" id="domain_edit" style="width: 202px; display: none;">	                       
	                    </select>	
	                </div>
	            </div>
	            
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.created"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="created">
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
</div>

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="add_serviceoffering_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Service Offering" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.service.offering"/>
            </div>
        </div>
    </div>
</div>
<!--  top buttons (end) -->

<!-- Add Service Offering Dialog -->
<div id="dialog_add_service" title='<fmt:message key="label.add.service.offering"/>' style="display:none">
	<p><fmt:message key="message.add.service.offering"/></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label><fmt:message key="label.name"/>:</label>
					<input class="text" type="text" name="add_service_name" id="add_service_name"/>
					<div id="add_service_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label><fmt:message key="label.display.text"/>:</label>
					<input class="text" type="text" name="add_service_display" id="add_service_display"/>
					<div id="add_service_display_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="add_service_storagetype"><fmt:message key="label.storage.type"/>:</label>
					<select class="select" name="add_service_storagetype" id="add_service_storagetype">
					    <option value="shared"><fmt:message key="label.shared"/></option>
						<option value="local"><fmt:message key="label.local"/></option>						
					</select>
				</li>		
				<li>
					<label><fmt:message key="label.num.cpu.cores"/>:</label>
					<input class="text" type="text" name="add_service_cpucore" id="add_service_cpucore"/>
					<div id="add_service_cpucore_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label><fmt:message key="label.cpu.mhz"/>:</label>
					<input class="text" type="text" name="add_service_cpu" id="add_service_cpu"/>
					<div id="add_service_cpu_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label><fmt:message key="label.memory.mb"/>:</label>
					<input class="text" type="text" name="add_service_memory" id="add_service_memory"/>
					<div id="add_service_memory_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>					
				<li>
					<label><fmt:message key="network.rate"/>:</label>
					<input class="text" type="text" id="network_rate"/>
					<div id="network_rate_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>	
				<li id="add_service_offerha_container">
					<label><fmt:message key="label.offer.ha"/>:</label>
					<select class="select" id="add_service_offerha">						
						<option value="false"><fmt:message key="label.no"/></option>
						<option value="true"><fmt:message key="label.yes"/></option>
					</select>
				</li>	
				<li id="add_service_tags_container">
                    <label for="add_service_tags">
                        <fmt:message key="label.storage.tags"/>:</label>
                    <input class="text" type="text" id="add_service_tags" />
                    <div id="add_service_tags_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                    </div>
                </li>	
                <li id="add_service_hosttags_container">
                    <label>
                        <fmt:message key="label.host.tags"/>:</label>
                    <input class="text" type="text" id="add_service_hosttags" />
                    <div id="add_service_hosttags_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                    </div>
                </li>     
 				<li>
				    <label>
				        <fmt:message key="label.CPU.cap"/>:</label>
				    <select class="select" id="cpu_cap_dropdown">				        
				        <option value="false"><fmt:message key="label.no"/></option>
				        <option value="true"><fmt:message key="label.yes"/></option>
				    </select>
				</li>
				<li>
				    <label>
				        <fmt:message key="label.public"/>:</label>
				    <select class="select" id="public_dropdown">
				        <option value="true"><fmt:message key="label.yes"/></option>
				        <option value="false"><fmt:message key="label.no"/></option>
				    </select>
				</li>
				<li id="domain_container" style="display: none">
				    <label>
				        <fmt:message key="label.domain"/>:</label>				        
				    <input class="text" type="text" id="domain" />
                	<div id="domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>  
				    <!--     
				    <select class="select" id="domain_dropdown">
				    </select>
				    --> 
				</li>    			
			</ol>
		</form>
	</div>
</div>


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