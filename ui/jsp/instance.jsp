<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 
	'label.action.start.instance': '<fmt:message key="label.action.start.instance"/>',
	'label.action.start.instance.processing': '<fmt:message key="label.action.start.instance.processing"/>',
	'message.action.start.instance': '<fmt:message key="message.action.start.instance"/>',
	'label.action.stop.instance': '<fmt:message key="label.action.stop.instance"/>',
	'label.action.stop.instance.processing': '<fmt:message key="label.action.stop.instance.processing"/>',
	'message.action.stop.instance': '<fmt:message key="message.action.stop.instance"/>',
	'label.action.reboot.instance': '<fmt:message key="label.action.reboot.instance"/>',
	'label.action.reboot.instance.processing': '<fmt:message key="label.action.reboot.instance.processing"/>',
	'message.action.reboot.instance': '<fmt:message key="message.action.reboot.instance"/>',
	'label.action.destroy.instance': '<fmt:message key="label.action.destroy.instance"/>',
	'label.action.destroy.instance.processing': '<fmt:message key="label.action.destroy.instance.processing"/>',
	'message.action.destroy.instance': '<fmt:message key="message.action.destroy.instance"/>',
	'label.action.restore.instance': '<fmt:message key="label.action.restore.instance"/>',
	'label.action.restore.instance.processing': '<fmt:message key="label.action.restore.instance.processing"/>',
	'message.action.restore.instance': '<fmt:message key="message.action.restore.instance"/>',
	'label.action.edit.instance': '<fmt:message key="label.action.edit.instance"/>',
	'label.action.attach.iso': '<fmt:message key="label.action.attach.iso"/>',
	'label.action.attach.iso.processing': '<fmt:message key="label.action.attach.iso.processing"/>',
	'label.action.detach.iso': '<fmt:message key="label.action.detach.iso"/>',
	'label.action.detach.iso.processing': '<fmt:message key="label.action.detach.iso.processing"/>',
	'label.action.reset.password': '<fmt:message key="label.action.reset.password"/>',
	'label.action.reset.password.processing': '<fmt:message key="label.action.reset.password.processing"/>',
	'label.action.change.service': '<fmt:message key="label.action.change.service"/>',
	'label.action.change.service.processing': '<fmt:message key="label.action.change.service.processing"/>',
	'label.action.migrate.instance': '<fmt:message key="label.action.migrate.instance"/>',
	'label.action.migrate.instance.processing': '<fmt:message key="label.action.migrate.instance.processing"/>',
	'label.action.detach.disk': '<fmt:message key="label.action.detach.disk"/>',
	'label.action.detach.disk.processing': '<fmt:message key="label.action.detach.disk.processing"/>',
	'label.action.create.template': '<fmt:message key="label.action.create.template"/>',
	'label.action.create.template.processing': '<fmt:message key="label.action.create.template.processing"/>',
	'label.action.take.snapshot': '<fmt:message key="label.action.take.snapshot"/>',
	'label.action.take.snapshot.processing': '<fmt:message key="label.action.take.snapshot.processing"/>',
	'label.new.password': '<fmt:message key="label.new.password"/>',
	'message.action.instance.reset.password': '<fmt:message key="message.action.instance.reset.password"/>',
	'message.action.take.snapshot': '<fmt:message key="message.action.take.snapshot"/>',
	'label.data.disk.offering': '<fmt:message key="label.data.disk.offering"/>',
	'label.root.disk.offering': '<fmt:message key="label.root.disk.offering"/>',
	'label.full': '<fmt:message key="label.full"/>',
	'label.available': '<fmt:message key="label.available"/>',
	'message.launch.vm.on.private.network': '<fmt:message key="message.launch.vm.on.private.network"/>',
	'message.action.change.service.warning.for.instance': '<fmt:message key="message.action.change.service.warning.for.instance"/>',
	'message.action.reset.password.warning': '<fmt:message key="message.action.reset.password.warning"/>',
	'message.action.reset.password.off': '<fmt:message key="message.action.reset.password.off"/>'
};	
</script>

<!-- VM detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_instanceicons.gif" /></div>
    <h1 id="vm_name">
        <fmt:message key="label.instance"/>
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on" id="tab_details">
            <fmt:message key="label.details" /></div>
		<div class="content_tabs off" id="tab_nic">
            <fmt:message key="label.nics"/></div>
        <div class="content_tabs off" id="tab_securitygroup" style="display:none;">
            <fmt:message key="label.security.group"/></div>
        <div class="content_tabs off" id="tab_volume">
            <fmt:message key="label.volumes"/></div>
        <div class="content_tabs off" id="tab_statistics">
            <fmt:message key="label.statistics"/></div>	       
    </div>
    <!--Details tab (start)-->
    <div  id="tab_content_details">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>  
        <div id="tab_container"> 
	        <div class="grid_container" style="display: block;">            
	            <div class="grid_header">
	            	<div id="title" class="grid_header_title">Title</div>
	                    <div class="grid_actionbox" id="action_link"> <p> <fmt:message key="label.actions"/></p>
	                        <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
	                            <ul class="actionsdropdown_boxlist" id="action_list">
	                                <li><fmt:message key="label.no.actions"/></li>
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
	                <div class="vm_statusbox">
	                    <div id="view_console_container" style="float:left;">  
	                	    <div id="view_console_template" style="display:block">
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
							<!--
	                        <p id="ipAddress">
	                        </p>
							-->
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
	                        <fmt:message key="label.zone"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="zoneName">
	                    </div>
	                </div>
	            </div>        
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.name"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="vmname">
	                    </div>
	                    <input class="text" id="vmname_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="vmname_edit_errormsg" style="display:none"></div>
	                </div>
	            </div>	
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.hypervisor"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="hypervisor">
	                    </div>
	                </div>
	            </div>	
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.template"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="templateName">
	                    </div>
	                </div>
	            </div>	            
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.os.type"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="ostypename">
	                    </div>
	                    <select class="select" id="ostypename_edit" style="width: 202px; display: none;">                      
	                    </select>
	                </div>
	            </div>	            
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.service.offering"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="serviceOfferingName">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.ha.enabled"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="haenable">                   
	                    </div>
	                    <select class="select" id="haenable_edit" style="width: 202px; display: none;">
	                        <option value="false"><fmt:message key="label.no"/></option>
							<option value="true"><fmt:message key="label.yes"/></option>
	                    </select>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.host"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="hostName">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.attached.iso"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="iso">                    
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.group"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="group">
	                    </div>
	                    <input class="text" id="group_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="group_edit_errormsg" style="display:none"></div>
	                </div>
	            </div>		            
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.domain"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="domain">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.account"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="account">
	                    </div>
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
    <!--Details tab (end)-->
	<!--Nic tab (start)-->
    <div style="display: none;" id="tab_content_nic">    
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div> 
        <div id="tab_container">        
        </div>
    </div>
    <!--Nic tab (end)-->
        
    <!--Security Group tab (start)-->
    <div style="display: none;" id="tab_content_securitygroup">    
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>         
		<div id="tab_container">
	        <div class="grid_container">
	            <div class="grid_header">
	                <div class="grid_header_cell" style="width: 5%; ">
	                    <div class="grid_header_title">
	                        <fmt:message key="label.id"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 20%; ">
	                    <div class="grid_header_title">
	                        <fmt:message key="label.name"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 40%; ">
	                    <div class="grid_header_title">
	                        <fmt:message key="label.description"/></div>
	                </div>	                
	                <div class="grid_header_cell" style="width: 30%; ">
	                    <div class="grid_header_title">
	                        <fmt:message key="label.actions"/></div>
	                </div>
	            </div>	            
	            <div id="grid_content">
	            </div>
	        </div>
        </div>     
    </div>
    <!--Security Group tab (end)-->       
    
    <!--Volume tab (start)-->
    <div style="display: none;" id="tab_content_volume">    
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div> 
        <div id="tab_container">        
        </div>
    </div>
    <!--Volume tab (end)-->
    <!--Statistics tab (start)-->
    <div style="display: none;" id="tab_content_statistics">
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container"> 
            <div class="grid_container">
        	    <div class="grid_header">
            	    <div id="grid_header_title" class="grid_header_title"></div>
                </div>
                
                <!--  
                <div class="dbrow odd" id="cpu_barchart">
                    <div class="dbrow_cell" style="width: 40%;">
                        <div class="dbgraph_titlebox">
                            <h2>
                                <fmt:message key="label.cpu"/></h2>
                            <div class="dbgraph_title_usedbox">
                                <p>
                                    Total: <span id="capacityused">
	                                    <span id="cpunumber">M</span> 
	                                    x 
	                                    <span id="cpuspeed">N</span> 
                                    </span>
                                </p>
                            </div>
                        </div>
                    </div>
                    <div class="dbrow_cell" style="width: 43%; border: none;">
                        <div class="db_barbox low" id="bar_chart">
                        </div>
                    </div>
                    <div class="dbrow_cell" style="width: 16%; border: none;">
                        <div class="db_totaltitle" id="percentused">
                        0%
                        </div>
                    </div>
                </div>
                -->
                
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.total.cpu"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles">
                            <span id="cpunumber">M</span> 
	                        x 
	                        <span id="cpuspeed">N</span> 
                        </div>
                    </div>
                </div>
                
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.cpu.utilized"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="percentused">
                        </div>
                    </div>
                </div>
                
                
                
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.network.read"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="networkkbsread">
                        </div>
                    </div>
                </div>
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.network.write"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="networkkbswrite">
                        </div>
                    </div>
                </div>  
            </div>
        </div>   
    </div>
    <!--Statistics tab (end)-->       
    
</div>
<!-- VM detail panel (end) -->

<!-- VM Primary Network Template (begin) -->
<div class="vmpopup_offeringbox" id="wizard_network_direct_template" style="display:none">
	<input type="radio" name="primary_network" class="radio" id="network_direct_checkbox" checked="checked" />
	<label class="label" id="network_direct_name">
	</label>
	<div class="vmpopup_offdescriptionbox">
		<div class="vmpopup_offdescriptionbox_top">
		</div>
		<div class="vmpopup_offdescriptionbox_bot">
			<p id="network_direct_desc">
				<fmt:message key="message.virtual.network.desc"/>
			</p>
		</div>
	</div>
</div>
<!-- VM Network Template (end) -->
<!-- VM Secondary Network Template (begin) -->
<div class="vmpopup_offeringbox" id="wizard_network_direct_secondary_template" style="display:none">
	<input type="checkbox" name="secondary_network" class="radio" id="network_direct_checkbox" />
	<label class="label" id="network_direct_name">
	</label>
	<div class="vmpopup_offdescriptionbox">
		<div class="vmpopup_offdescriptionbox_top">
		</div>
		<div class="vmpopup_offdescriptionbox_bot">
			<p id="network_direct_desc">
				<fmt:message key="message.virtual.network.desc"/>
			</p>
		</div>
	</div>
</div>
<!-- VM Network Template (end) -->
<!-- VM Network Review Template (begin) -->
<div class="vmpopup_reviewbox" id="wizard_network_direct_review_template" style="display:none">
	<div class="vmopopup_reviewtextbox">
		<div class="vmpopup_reviewtick">
		</div>
		<div class="vmopopup_review_label" id="wizard_review_network_label"></div>
		<span id="wizard_review_network_selected"></span>
	</div>
</div>
<!-- VM Network Review Template (end) -->
<!-- VM wizard (begin)-->
<div id="vm_popup" class="vmpopup_container" style="display: none">
    <div id="step1" style="display: block;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step1_bg.png) no-repeat top left">
                <fmt:message key="label.step.1"/></div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                <fmt:message key="label.step.2"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.3"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.4"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.5"/></div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        <fmt:message key="label.step.1.title"/></h2>
                    <p>
						<fmt:message key="message.step.1.desc"/>
                    </p>
                </div>
                <div class="vmpopup_contentpanel">
                    <div class="rev_tempsearchpanel">
                        <label for="wizard_zone">
                            <fmt:message key="label.availability.zone"/>:</label>
                        <select class="select" id="wizard_zone" name="zone">
                        </select>
                        <div class="rev_tempsearchbox">
                            <form method="post" action="#">
                            <ol>
                                <li>
                                    <input id="search_input" class="text" type="text" name="search_input" />
                            </ol>
                            </form>
                            <div id="search_button" class="rev_searchbutton">
                                <fmt:message key="label.search"/></div>
                        </div>
                    </div>
                    <div class="rev_wizformarea">
                        <div class="revwiz_message_container" style="display: none;" id="wiz_message">
                            <div class="revwiz_message_top">
                                <p id="wiz_message_text">
									<fmt:message key="message.step.1.continue"/>
                                </p>
                            </div>
                            <div class="revwiz_message_bottom">
                                <div class="revwizcontinue_button" id="wiz_message_continue">
                                </div>
                            </div>
                        </div>
                        <div class="rev_wizmid_tempbox">
                            <div class="revwiz_loadingbox" id="wiz_template_loading" style="display: none">
                                <div class="loading_gridanimation">
                                </div>
                                <p>
                                    <fmt:message key="label.loading"/>...</p>
                            </div>
                            <div class="rev_wizmid_tempbox_left" id="wiz_template_filter">
                                <div class="rev_wizmid_selectedtempbut" id="wiz_featured">
                                    <fmt:message key="label.menu.featured.templates"/></div>
                                <div class="rev_wizmid_nonselectedtempbut" id="wiz_my">
                                    <fmt:message key="label.menu.my.templates"/></div>
                                <div class="rev_wizmid_nonselectedtempbut" id="wiz_community">
                                    <fmt:message key="label.menu.community.templates"/></div>
                                <div class="rev_wizmid_nonselectedtempbut" id="wiz_blank">
                                    <fmt:message key="label.iso.boot"/></div>
                            </div>
                            <div class="rev_wizmid_tempbox_right">
                                <div class="rev_wiztemplistpanel" id="template_container">  
                                    
                                </div>
                                <div class="rev_wiztemplistactions">
                                    <div class="rev_wiztemplist_actionsbox">
                                        <a href="#" id="prev_page"><fmt:message key="label.prev"/></a> <a href="#" id="next_page"><fmt:message key="label.more.templates"/></a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step" style="display: none;">
                    </div>
                    <div class="vmpop_nextbutton" id="next_step">
                        <fmt:message key="label.go.step.2"/></div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
    <div id="step2" style="display: none;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                <fmt:message key="label.step.1"/></div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                <fmt:message key="label.step.2"/></div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                <fmt:message key="label.step.3"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.4"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.5"/></div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2><fmt:message key="label.step.2.title"/>
                        </h2>
                    <p>
                    </p>
                </div>
                <div class="vmpopup_contentpanel">
                    <h3>
                        <!--Service Offering-->
                    </h3>
                    
                    <div class="revwiz_message_container" style="display: none;" id="wiz_message">
                        <div class="revwiz_message_top">
                            <p id="wiz_message_text">
                                <fmt:message key="message.step.2.continue"/></p>
                        </div>
                        <div class="revwiz_message_bottom">
                            <div class="revwizcontinue_button" id="wiz_message_continue">
                            </div>
                        </div>
                    </div>
                    <form>
                    <div class="vmpopup_offeringpanel" id="service_offering_container">
                    </div>
					</form>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step">
                        <fmt:message key="label.back"/></div>
                    <div class="vmpop_nextbutton" id="next_step">
                        <fmt:message key="label.go.step.3"/></div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
    <div id="step3" style="display: none;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                <fmt:message key="label.step.1"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.2"/></div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                <fmt:message key="label.step.3"/></div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                <fmt:message key="label.step.4"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.5"/></div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
						<fmt:message key="label.step.3.title"/>
                        </h2>
                    <p>
                    </p>
                </div>
                <div class="vmpopup_contentpanel">
                    <h3>
                    </h3>
                    
                    <div class="revwiz_message_container" style="display: none;" id="wiz_message">
                        <div class="revwiz_message_top">
                            <p id="wiz_message_text">
								<fmt:message key="label.step.3.title"/>
                                </p>
                        </div>
                        <div class="revwiz_message_bottom">
                            <div class="revwizcontinue_button" id="wiz_message_continue">
                            </div>
                        </div>
                    </div>
                    <form>
                    <div class="vmpopup_offeringpanel" id="data_disk_offering_container" style="display: none">
                    </div>
					</form>
					<form>
                    <div class="vmpopup_offeringpanel" id="root_disk_offering_container" style="display: none">
                    </div>
					</form>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step">
                        <fmt:message key="label.back"/></div>
                    <div class="vmpop_nextbutton" id="next_step">
                        <fmt:message key="label.go.step.4"/></div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
    <div id="step4" style="display: none;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                <fmt:message key="label.step.1"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.2"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.3"/></div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                <fmt:message key="label.step.4"/></div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                <fmt:message key="label.step.5"/></div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
						<fmt:message key="label.step.4.title"/></h2>
                    <p>
                        <!--  
						<fmt:message key="message.step.4.desc"/>
						-->
                    </p>
                </div>
                <div class="vmpopup_contentpanel">
                    <h3>
                    </h3>
					<div class="revwiz_message_container" style="display: none;" id="wiz_message">
                        <div class="revwiz_message_top">
                            <p id="wiz_message_text">
                                <fmt:message key="message.step.4.continue"/></p>
                        </div>
                        <div class="revwiz_message_bottom">
                            <div class="revwizcontinue_button" id="wiz_message_continue">
                            </div>
                        </div>
                    </div>
                    <div class="vmpopup_offeringpanel" style="position:relative;">
                        <div id="network_container" style="display: none;">
                            <h3><fmt:message key="message.step.4.desc"/></h3>	
	                        <div class="vmpopup_offeringbox" id="network_virtual_container" style="display:none">
	                            <input type="radio" id="network_virtual" name="primary_network" class="radio" checked="checked" />
	                            <label class="label" id="network_virtual_name"><fmt:message key="label.virtual.network"/></label>
	                            <div class="vmpopup_offdescriptionbox">
	                                <div class="vmpopup_offdescriptionbox_top">
	                                </div>
	                                <div class="vmpopup_offdescriptionbox_bot">
	                                    <p id="network_virtual_desc">
											<fmt:message key="message.virtual.network.desc"/>
	                                    </p>
	                                </div>
	                            </div>
	                        </div>
							<div id="network_direct_container"></div>
							<h3 id="secondary_network_title" style="display:none; margin-top:15px;">
							<fmt:message key="label.additional.networks"/>:
							</h3>
							<p id="secondary_network_desc" style="display:none">
								<fmt:message key="message.additional.networks.desc"/>
							</p>
							<div id="network_direct_secondary_container"></div>
						</div>
						<div id="securitygroup_container" style="display:none">		
						    <h3><fmt:message key="label.security.groups"/></h3>		
						    <span id="not_available_message" style="display:none"><fmt:message key="label.no.security.groups"/></span>				    
						    <ol id="security_group_section" style="display:none; float:left; list-style:none">						        
						        <li>
						            <select id="security_group_dropdown" class="multiple" multiple="multiple" size="15">
									</select>
						        </li>
						        <li>		
									<fmt:message key="message.security.group.usage"/>							        
						        </li>
						    </ol>						      
						</div>
						<div id="for_no_network_support" style="display:none">		
						    <span id="not_available_message_1" style="display:none"><fmt:message key="message.no.network.support"/></span>		
						    <span id="not_available_message_2" style="display:none"><fmt:message key="message.no.network.support.configuration.not.true"/></span>				    
						</div>
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step">
                        <fmt:message key="label.back"/></div>
                    <div class="vmpop_nextbutton" id="next_step">
                        <fmt:message key="label.go.step.5"/></div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
    <div id="step5" style="display: none;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                <fmt:message key="label.step.1"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.2"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.3"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.4"/></div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                <fmt:message key="label.step.5"/></div>
            <div class="vmpopup_steps" style="background: url(images/laststep_slectedbg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        <fmt:message key="label.step.5.title"/></h2>
                    <p>
                    </p>
                </div>
                <div class="vmpopup_contentpanel">
                    <h3>
                    </h3>
                    <div class="vmpopup_offeringpanel" style="margin-top: 10px;">
						<div class="vmpopup_reviewbox odd">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
									<fmt:message key="label.name.optional"/>:
                                </div>
                                <input class="text" type="text" id="wizard_vm_name" />
                                <div id="wizard_vm_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                                </div>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox even">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    <fmt:message key="label.group.optional"/>:</div>
                                <input class="text" type="text" id="wizard_vm_group" />
                                <div id="wizard_vm_group_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                                </div>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox odd">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    <fmt:message key="label.zone"/>:</div>
                                <span id="wizard_review_zone"></span>
                            </div>
                        </div>                        
                        <div class="vmpopup_reviewbox even">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    <fmt:message key="label.hypervisor"/>:</div>
                                <span id="wizard_review_hypervisor"></span>
                            </div>
                        </div>                        
                        <div class="vmpopup_reviewbox odd">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    <fmt:message key="label.template"/>:
                                </div>
                                <span id="wizard_review_template"></span>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox even">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    <fmt:message key="label.service.offering"/>:</div>
                                <span id="wizard_review_service_offering"></span>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox odd">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label" id="wizard_review_disk_offering_label">
                                    <fmt:message key="label.disk.offering"/>:
                                </div>
                                <span id="wizard_review_disk_offering"></span>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox even" id="wizard_review_primary_network_container">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    <fmt:message key="label.primary.network"/>:</div>
                                <span id="wizard_review_network"></span>
                            </div>
                        </div>
						<div id="wizard_review_secondary_network_container"></div>
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step">
                        <fmt:message key="label.back"/></div>
                    <div class="vmpop_nextbutton" id="next_step">
                        <fmt:message key="label.submit"/></div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
</div>
<!-- VM wizard (end)-->

<!-- VM Wizard - VM template (begin) -->
<div id="vmtemplate_in_vmwizard" class="rev_wiztemplistbox" style="display:none">
    <div id="icon">
    </div>
    <div class="rev_wiztemp_listtext">
        <span id="name"></span>
    </div>
    <div class="rev_wiztemp_hypervisortext">
            <fmt:message key="label.hypervisor"/>: <strong id="hypervisor_text"></strong>
    </div>
    <div class="rev_wiztemp_ownertext">
		<fmt:message key="label.submitted.by"/></div>
</div>
<!-- VM Wizard - VM template (end) -->

<!-- VM Wizard - ISO template (begin) -->
<div id="vmiso_in_vmwizard" class="rev_wiztemplistbox" style="display:none">
    <div id="icon" class="rev_wiztemo_centosicons">
    </div>
    <div class="rev_wiztemp_listtext">
        <span id="name">Centos</span>
    </div>
    <div class="rev_wiztemp_hypervisortext">
        Hypervisor:
        <select id="hypervisor_select" class="select" style="width: 70px; float: none; height: 15px; font-size: 10px; margin: 0 0 0 5px; display: inline;">
            <!--  
            <option value='XenServer'>XenServer</option>
            <option value='VmWare'>VmWare</option>
            <option value='KVM'>KVM</option>
            -->
        </select>
        <span id="hypervisor_span" style="display:none"></span>
    </div>
    <div class="rev_wiztemp_ownertext">
        <fmt:message key="label.submitted.by"/></div>
</div>
<!-- VM Wizard - ISO template (end) -->

<!-- VM Wizard - Service Offering template (begin) -->
<div id="vm_popup_service_offering_template" style="display: none">
	<div class="vmpopup_offeringbox">
		<input type="radio" name="service_offering_radio" class="radio" checked />
		<label class="label" id="name">
		</label>
		<div class="vmpopup_offdescriptionbox">
			<div class="vmpopup_offdescriptionbox_top">
			</div>
			<div class="vmpopup_offdescriptionbox_bot">
				<p id="description">
				</p>
			</div>
		</div>
	</div>
</div>
<!-- VM Wizard - Service Offering template (end) -->
<!-- VM Wizard - disk Offering template (begin)-->
<div id="vm_popup_disk_offering_template_no" style="display: none">
	<div class="vmpopup_offeringbox">
		<input type="radio" name="data_disk_offering_radio" class="radio" value="no" checked />
		<label class="label">
			<fmt:message key="label.no.thanks"/></label>
	</div>
</div>
<div id="vm_popup_disk_offering_template_custom" style="display: none">
	<div class="vmpopup_offeringbox" >
		<input type="radio" name="data_disk_offering_radio" checked class="radio" value="custom" />
		<label class="label" id="name">
		</label>
		<div class="vmpopup_offdescriptionbox_bot" style="background:none; border:none;">
			<label class="label1" style="margin-left:33px; display:inline;">
				<fmt:message key="label.disk.size"/>:</label>
			<input type="text" id="custom_disk_size" class="text" />
			<span>GB</span>
		   
			<div id="custom_disk_size_errormsg" class="errormsg" style="display: none; margin-left:89px; display:inline;">
			</div>
		 </div>
	</div>
</div>
<div id="vm_popup_disk_offering_template_existing" style="display: none">
	<div class="vmpopup_offeringbox" >
		<input type="radio" name="data_disk_offering_radio" class="radio" checked />
		<label class="label" id="name">
		</label>
		<div class="vmpopup_offdescriptionbox">
			<div class="vmpopup_offdescriptionbox_top">
			</div>
			<div class="vmpopup_offdescriptionbox_bot">
				<p id="description">
				</p>
			</div>
		</div>
	</div>
</div>
<!-- VM Wizard - disk Offering template (end)-->

<!--  nic tab template (begin) -->
<div class="grid_container" id="nic_tab_template" style="display: none">	
    <div class="grid_header">
        <div class="grid_header_title" id="title">
        </div>
		<!--
        <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; border: 1px solid #999; ">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/> &hellip;
            </p>
        </div>    
		-->
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
                <fmt:message key="label.ip"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="ip">
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
                <fmt:message key="label.netmask"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="netmask">
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
</div>
<!--  nic tab template (end) -->


<!-- security group tab template (begin) -->
<div class="grid_rows" id="securitygroup_tab_template" style="display:none"> <!-- add class "odd", "even" in JS file-->
    <div id="row_container">
        <div class="grid_row_cell" style="width: 5%; ">
            <div class="row_celltitles" id="id">
                </div>
        </div>
        <div class="grid_row_cell" style="width: 20%; ">
            <div class="row_celltitles" id="name">
                </div>
        </div>
        <div class="grid_row_cell" style="width: 40%; ">
            <div class="row_celltitles" id="description">
                </div>
        </div>      
        <div class="grid_row_cell" style="width: 30%; ">
            <div class="row_celltitles">                
                <a id="show_ingressrule_link" href="#" style="float:left;"><fmt:message key="label.show.ingress.rule"/></a>
                <a id="hide_ingressrule_link" href="#" style="float:left;display:none" ><fmt:message key="label.hide.ingress.rule"/></a>
            </div>
        </div>  
    </div>     
    <div class="grid_detailspanel" id="management_area" style="display: none;">
        <div class="grid_details_pointer">
        </div>
        <div class="grid_detailsbox">
            <div class="grid_details_row odd" id="add_vm_to_lb_row">
                <div class="grid_header_cell" style="width: 5%; ">
	                <div class="grid_header_title">
                        <fmt:message key="label.id"/>
                    </div>
                </div>
                <div class="grid_header_cell" style="width: 10%; ">
	                <div class="grid_header_title">
                        <fmt:message key="label.protocol"/>
                    </div>
                </div>
                <div class="grid_header_cell" style="width: 40%; ">
	                <div class="grid_header_title">
                        <fmt:message key="label.endpoint.or.operation"/>
                    </div>
                </div>
                <div class="grid_header_cell" style="width: 40%; ">
	                <div class="grid_header_title">
                        <fmt:message key="label.cidr.account"/>
                    </div>
                </div>               
            </div>
            <div id="subgrid_content" class="ip_description_managearea">
            </div>
        </div>
    </div>
</div>
<!-- security group tab template (end) -->

<!-- security group tab - ingress rule - template (begin) -->
<div id="ingressrule_template" class="grid_details_row odd" style="display:none">
   
    <div class="grid_row_cell" style="width: 5%;">
        <div class="row_celltitles" id="id"></div>
    </div>
    <div class="grid_row_cell" style="width: 10%;">
        <div class="row_celltitles" id="protocol"></div>
    </div>
    <div class="grid_row_cell" style="width: 40%;">
        <div class="row_celltitles" id="endpoint"></div>
    </div>
    <div class="grid_row_cell" style="width: 40%;">
        <div class="row_celltitles" id="cidr"></div>
    </div>
</div>
<!-- security group tab - ingress rule - template (end) -->

<!--  volume tab template (begin) -->
<div class="grid_container" id="volume_tab_template" style="display: none">	
    <div class="grid_header">
        <div class="grid_header_title" id="title">
        </div>
        <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; border: 1px solid #999; ">
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
                <fmt:message key="label.size"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="size">
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
</div>
<!--  volume tab template (end) -->

<!-- view console template (begin)  -->
<div id="view_console_template" style="display:none">
    <div class="vm_consolebox" id="box0">
    </div>
    <div class="vm_consolebox" id="box1" style="display: none">
    </div>
</div>
<!-- view console template (end)  -->

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="add_vm_button">
        <div class="actionpanel_button" id="button_content">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.vm.add"/>
            </div>
        </div>
    </div>

    <div class="actionpanel_button_wrapper" id="start_vm_button">
        <div class="actionpanel_button" id="button_content">
            <div class="actionpanel_button_icons">
                <img src="images/startvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.vm.start" />
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="stop_vm_button">
        <div class="actionpanel_button" id="button_content">
            <div class="actionpanel_button_icons">
                <img src="images/stopvm_actionicon.png"/></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.vm.stop" />
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="reboot_vm_button">
        <div class="actionpanel_button" id="button_content">
            <div class="actionpanel_button_icons">
                <img src="images/rebootvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.vm.reboot" />
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="destroy_vm_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/destroyvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.vm.destroy" />
            </div>
        </div>
    </div>
</div>
<!--  top buttons (end) -->

<!--  ***** Dialogs (begin) ***** -->
<!-- Detach ISO Dialog -->
<div id="dialog_detach_iso_from_vm" title="Confirmation" style="display:none">
    <p><fmt:message key="message.detach.iso.confirm" /></p>   
</div>

<!-- Attach ISO Dialog -->
<div id="dialog_attach_iso" title='<fmt:message key="label.action.attach.iso" />' style="display: none">
    <p> 
        <fmt:message key="message.attach.iso.confirm" />     
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <fmt:message key="label.iso" />:</label>
                <select class="select" id="attach_iso_select">
                    <option value="none"><fmt:message key="label.no.isos" /></option>
                </select>
                <div id="attach_iso_select_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
            </li>
        </ol>
        </form>
    </div>
</div>

<!-- Change Service Offering Dialog -->
<div id="dialog_change_service_offering" title='<fmt:message key="label.action.change.service" />' style="display: none">
    <p> 
		<fmt:message key="message.change.offering.confirm" />
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <fmt:message key="label.service.offering" />:</label>
                <select class="select" id="change_service_offerings">
                </select>
                <div id="change_service_offerings_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>

<!-- Migrate VM Dialog -->
<div id="dialog_migrate_instance" title='<fmt:message key="label.action.migrate.instance" />' style="display: none">
    <p> 
		<fmt:message key="message.migrate.instance.confirm" />
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <fmt:message key="label.migrate.instance.to" />:</label>
                <select class="select" id="migrate_instance_hosts">
                </select>
                <div id="migrate_vm_hosts_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>

<!-- Create template from VM dialog (begin) -->
<div id="dialog_create_template_from_vm" title='<fmt:message key="label.action.create.template.from.vm" />' style="display: none">
    <p> 
		<fmt:message key="message.vm.create.template.confirm" />
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <fmt:message key="label.name" />:</label>
                <input class="text" type="text" name="create_template_name" id="create_template_name" />
                <div id="create_template_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.display.text" />:</label>
                <input class="text" type="text" name="create_template_desc" id="create_template_desc" />
                <div id="create_template_desc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="create_template_os_type">
                    <fmt:message key="label.os.type" />:</label>
                <select class="select" name="create_template_os_type" id="create_template_os_type">
                </select>
            </li>
            <li>
                <label for="create_template_public">
                    <fmt:message key="label.public" />:</label>
                <select class="select" name="create_template_public" id="create_template_public">
                    <option value="false"><fmt:message key="label.no" /></option>
                    <option value="true"><fmt:message key="label.yes" /></option>
                </select>
            </li>                 
            <li id="image_directory_container">
                <label>
                    <fmt:message key="image.directory" />:</label>
                <input class="text" type="text" id="image_directory" />
                <div id="image_directory_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>           
        </ol>
        </form>
    </div>
</div>
<!-- Create template from VM dialog (end) -->

<!-- Create template from volume dialog (begin) -->
<div id="dialog_create_template_from_volume" title='<fmt:message key="label.action.create.template.from.volume" />' style="display: none">
    <p> 
		<fmt:message key="message.volume.create.template.confirm" />
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <fmt:message key="label.name" />:</label>
                <input class="text" type="text" name="create_template_name" id="create_template_name" />
                <div id="create_template_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.display.text" />:</label>
                <input class="text" type="text" name="create_template_desc" id="create_template_desc" />
                <div id="create_template_desc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="create_template_os_type">
                    <fmt:message key="label.os.type" />:</label>
                <select class="select" name="create_template_os_type" id="create_template_os_type">
                </select>
            </li>
            <li>
                <label for="create_template_public">
                    <fmt:message key="label.public" />:</label>
                <select class="select" name="create_template_public" id="create_template_public">
                    <option value="false"><fmt:message key="label.no" /></option>
                    <option value="true"><fmt:message key="label.yes" /></option>
                </select>
            </li>
            <li>
                <label>
                    <fmt:message key="label.password.enabled" />:</label>
                <select class="select" name="create_template_password" id="create_template_password">
                    <option value="false"><fmt:message key="label.no" /></option>
                    <option value="true"><fmt:message key="label.yes" /></option>
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Create template from volume dialog (end) -->

<!-- Confirm to stop VM (begin) -->
<div id="dialog_confirmation_stop_vm" title='<fmt:message key="label.confirmation"/>' style="display: none">
 	<p> 
		<fmt:message key="message.action.stop.instance" />
	</p> 		
    <div class="dialog_formcontent" id="force_stop_instance_container" style="display:none">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li style="padding-top:10px">
                <input type="checkbox" class="checkbox" id="force_stop_instance" /> 
                <p style="color:red"><fmt:message key="force.stop" /></p>		
            </li>
            <li>
                <p style="color:red"><fmt:message key="force.stop.instance.warning" /></p>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Confirm to stop VM (end) -->

<!--  ***** Dialogs (end) ***** -->

<div id="hidden_container">
    <!-- advanced search popup (begin) -->
    <div id="advanced_search_popup" class="adv_searchpopup_bg" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>               
                <li>
                    <select class="select" id="adv_search_state">
                        <option value=""><fmt:message key="label.by.state" /></option>
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
                <li id="adv_search_domain_li" style="display: none;">                    
                    <input class="text textwatermark" type="text" id="domain" value='<fmt:message key="label.by.domain" />' />
                    <div id="domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
                    <!--  
                    <select class="select" id="adv_search_domain">
                    </select>
                    -->
                </li>
                <li id="adv_search_account_li" style="display: none;">
                    <input class="text textwatermark" type="text" id="adv_search_account" value='<fmt:message key="label.by.account" />' />
                </li>
            </ol>
            </form>
        </div>
    </div>
    <div id="advanced_search_popup_nostate" class="adv_searchpopup_bg" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>                
                <li>
                    <select class="select" id="adv_search_zone">
                    </select>
                </li>
                <li id="adv_search_domain_li" style="display: none;">
                    <input class="text textwatermark" type="text" id="domain" value='<fmt:message key="label.by.domain" />' />
                    <div id="domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
                    <!--  
                    <select class="select" id="adv_search_domain">
                    </select>
                    -->
                </li>
                <li id="adv_search_account_li" style="display: none;">                    
                    <input class="text textwatermark" type="text" id="adv_search_account" value='<fmt:message key="label.by.account" />' />
                </li>
            </ol>
            </form>
        </div>
    </div>
    <div id="advanced_search_popup_nodomainaccount" class="adv_searchpopup_bg" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>                
                <li>
                    <select class="select" id="adv_search_state">
                        <option value=""><fmt:message key="label.by.state" /></option>
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
            </ol>
            </form>
        </div>
    </div>
    <!-- advanced search popup (end) -->
</div>
