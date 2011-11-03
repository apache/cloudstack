<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 	
	'label.action.release.ip' : '<fmt:message key="label.action.release.ip"/>',
	'label.action.release.ip.processing' : '<fmt:message key="label.action.release.ip.processing"/>',
	'message.action.release.ip' : '<fmt:message key="message.action.release.ip"/>',
	'label.action.enable.static.NAT' : '<fmt:message key="label.action.enable.static.NAT"/>',
	'label.action.enable.static.NAT.processing' : '<fmt:message key="label.action.enable.static.NAT.processing"/>',
	'label.action.disable.static.NAT' : '<fmt:message key="label.action.disable.static.NAT"/>',
	'label.action.disable.static.NAT.processing' : '<fmt:message key="label.action.disable.static.NAT.processing"/>',
	'message.action.disable.static.NAT' : '<fmt:message key="message.action.disable.static.NAT"/>'
};	
</script>

<!-- IP Address detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_ipicon.gif" /></div>
    <h1>
        <fmt:message key="label.ip.address"/>
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on" id="tab_details" style="display: none">
            <fmt:message key="label.details"/></div>	
        <div class="content_tabs off" id="tab_firewall" style="display: none">
            <fmt:message key="label.firewall"/></div>	
        <div class="content_tabs off" id="tab_port_range" style="display: none">
            <fmt:message key="label.port.range"/></div>            	
		<div class="content_tabs off" id="tab_port_forwarding" style="display: none">
            <fmt:message key="label.port.forwarding"/></div>
        <div class="content_tabs off" id="tab_load_balancer" style="display: none">
            <fmt:message key="label.load.balancer"/></div>
		<div class="content_tabs off" id="tab_vpn" style="display: none">
            <fmt:message key="label.vpn"/></div>
    </div>  
    
    <!-- Details starts here-->
    <div id="tab_content_details" style="display: none">
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
	            	<div id="grid_header_title" class="grid_header_title" style="font-size:10px"></div>
	                <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
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
	                    <p id="description"></p>
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
	                        <fmt:message key="label.ip"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="ipaddress">
	                    </div>
	                </div>
	            </div>	            
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.state"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="state">
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
	                        <fmt:message key="label.vlan"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="vlanname">
	                    </div>
	                </div>
	            </div>           
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.source.nat"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="source_nat">
	                    </div>
	                </div>
	            </div>	  
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.network.type"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="network_type">
	                    </div>
	                </div>
	            </div>	            
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.network.id"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="networkid">
	                    </div>
	                </div>
	            </div>	            
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.associated.network.id"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="associatednetworkid">
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
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.allocated"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="allocated">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.static.nat"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="static_nat">
	                    </div>
	                </div>
	            </div>	 
	            <div class="grid_rows even" id="vm_of_static_nat_container" style="display:none">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.static.nat.to"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="vm_of_static_nat">
	                    </div>
	                </div>
	            </div>	  
	        </div>
        </div>        
    </div>
    <!-- Details ends here-->
    
    <!-- Firewall start here-->
    <div id="tab_content_firewall" style="display:none">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    <fmt:message key="label.loading"/> &hellip;</p>
            </div>
        </div>        
        <div id="tab_container">
	        <div class="grid_container" id="grid_container">
	            <div class="grid_header">	                
	                <div class="grid_header_cell" style="padding:1px; width: 30%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.cidr.list"/></div>
	                </div>	
	                <div class="grid_header_cell" style="padding:1px; width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.protocol"/></div>
	                </div>	 
	                <div class="grid_header_cell" style="padding:1px; width: 10%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.start.port"/></div>
	                </div>
	                <div class="grid_header_cell" style="padding:1px; width: 10%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.end.port"/></div>
	                </div>	              	                
	                <div class="grid_header_cell" style="padding:1px; width: 10%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="ICMP.type"/></div>
	                </div>
	                 <div class="grid_header_cell" style="padding:1px; width: 10%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="ICMP.code"/></div>
	                </div>
	                <div class="grid_header_cell" style="padding:1px; width: 10%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.actions"/></div>
	                </div>
	            </div>
	            <div class="grid_rows even" id="create_firewall_row">	            
	                <div class="grid_row_cell" style="padding:1px; width: 30%; ">  
	                    <input id="cidr" class="text" type="text" />
	                    <div id="cidr_errormsg" class="errormsg" style="display: none;"></div>
	                </div>	
	                <div class="grid_row_cell" style="padding:1px; width: 15%; ">  
	                   <select class="select" id="protocol" style="width:70%;">	                  
	                   </select>
	                </div>	 	
	                <div class="grid_row_cell" style="padding:1px; width: 10%; ">
	                    <input id="start_port" class="text" style="width: 70%;" type="text" />
	                    <div id="start_port_errormsg" class="errormsg" style="display: none;"></div>
	                </div>
	                <div class="grid_row_cell" style="padding:1px; width: 10%; ">
	                    <input id="end_port" class="text" style="width: 70%;" type="text" />
	                    <div id="end_port_errormsg" class="errormsg" style="display: none;"></div>
	                </div>	                               
	                <div class="grid_row_cell" style="padding:1px; width: 10%; ">
	                    <input id="ICMP_type" class="text" style="width: 70%;" type="text" />
	                    <div id="ICMP_type_errormsg" class="errormsg" style="display: none;"></div>
	                </div>
	                <div class="grid_row_cell" style="padding:1px; width: 10%; ">
	                    <input id="ICMP_code" class="text" style="width: 70%;" type="text" />
	                    <div id="ICMP_code_errormsg" class="errormsg" style="display: none;"></div>
	                </div>
	                <div class="grid_row_cell" style="padding:1px; width: 10%; ">
	                    <div class="row_celltitles">
	                        <a id="add_link" href="#"><fmt:message key="label.add"/></a></div>
	                </div>
	            </div>              
	            <div id="grid_content">
	            </div>            
	        </div>
        </div>      
    </div>
    <!-- Firewall ends here-->
    
    <!-- Port Range start here-->
    <div id="tab_content_port_range" style="display:none">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    <fmt:message key="label.loading"/> &hellip;</p>
            </div>
        </div>        
        <div id="tab_container">
	        <div class="grid_container" id="grid_container">
	            <div class="grid_header">
	                <div class="grid_header_cell" style="width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.start.port"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.end.port"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.protocol"/></div>
	                </div>	                
	                <div class="grid_header_cell" style="width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.state"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.actions"/></div>
	                </div>
	            </div>
	            <div class="grid_rows even" id="create_port_range_row">
	                <div class="grid_row_cell" style="width: 15%; ">
	                    <input id="start_port" class="text" style="width: 70%;" type="text" />
	                    <div id="start_port_errormsg" class="errormsg" style="display: none;"></div>
	                </div>
	                <div class="grid_row_cell" style="width: 15%; ">
	                    <input id="end_port" class="text" style="width: 70%;" type="text" />
	                    <div id="end_port_errormsg" class="errormsg" style="display: none;"></div>
	                </div>
	                <div class="grid_row_cell" style="width: 15%; ">  
	                   <select class="select" id="protocol" style="width:70%;">	                  
	                   </select>
	                </div>	                
	                <div class="grid_row_cell" style="width: 15%; ">   
	                    <div class="row_celltitles" id="state" style="padding:1px;"></div>
	                </div>  
	                <div class="grid_row_cell" style="width: 15%; ">
	                    <div class="row_celltitles">
	                        <a id="add_link" href="#"><fmt:message key="label.add"/></a></div>
	                </div>
	            </div>              
	            <div id="grid_content">
	            </div>            
	        </div>
        </div>      
    </div>
    <!-- Port Range ends here-->
    <!-- Port Forwarding start here-->
    <div id="tab_content_port_forwarding" style="display:none">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    <fmt:message key="label.loading"/> &hellip;</p>
            </div>
        </div>        
        <div id="tab_container">
	        <div class="grid_container" id="grid_container">
	            <div class="grid_header">	                          
	                <div class="grid_header_cell" style="width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.public.port"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.private.port"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 10%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.protocol"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 30%;  border: none;">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.instance"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 10%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.state"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.actions"/></div>
	                </div>
	            </div>
	            <div class="grid_rows even" id="create_port_forwarding_row">	               
	                <div class="grid_row_cell" style="width: 15%; ">
	                    <input id="public_port" class="text" style="width: 30%; " type="text" />
	                    <div id="public_port_errormsg" class="errormsg" style="display: none;"></div>
	                    
	                    <span style="float:left;padding-left:10px"> - </span>
	                    
	                    <input id="public_end_port" class="text" style="width: 30%; " type="text" />
	                    <div id="public_end_port_errormsg" class="errormsg" style="display: none;"></div>	                    
	                </div>
	                <div class="grid_row_cell" style="width: 15%;">
                        <input id="private_port" class="text" style="width: 30%; " type="text" />
	                    <div id="private_port_errormsg" class="errormsg" style="display: none;"></div>
	                    
	                    <span style="float:left;padding-left:10px"> - </span>
	                    
	                    <input id="private_end_port" class="text" style="width: 30%; " type="text" />
	                    <div id="private_end_port_errormsg" class="errormsg" style="display: none;"></div>	 
	                </div>
	                <div class="grid_row_cell" style="width: 10%; ">  
	                   <select class="select" id="protocol" style="width:70%;">	                    
	                   </select>
	                </div>
	                <div class="grid_row_cell" style="width: 30%; ">                   
	                    <select class="select" id="vm">
	                    </select>
	                    <div id="vm_errormsg" class="errormsg" style="display: none;"></div>	                    
	                </div>
	                <div class="grid_row_cell" style="width: 10%; ">   
	                    <div class="row_celltitles" id="state" style="padding:1px;"></div>
	                </div>  
	                <div class="grid_row_cell" style="width: 15%; ">
	                    <div class="row_celltitles">
	                        <a id="add_link" href="#"><fmt:message key="label.add"/></a></div>
	                </div>
	            </div>              
	            <div id="grid_content">
	            </div>            
	        </div>
        </div>      
    </div>
    <!-- Port Forwarding ends here-->
    <!-- Load Balancer start here-->
    <div id="tab_content_load_balancer" style="display:none">
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
	                          
	                <div class="grid_header_cell" style="width: 25%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.name"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.public.port"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.private.port"/></div>
	                </div>
	                <div class="grid_header_cell" style="width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.algorithm"/></div>
	                </div>	                
	                <div class="grid_header_cell" style="width: 10%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.state"/></div>
	                </div>	                
	                <div class="grid_header_cell" style="width: 15%; ">
	                    <div class="grid_header_title" style="font-size:10px">
	                        <fmt:message key="label.actions"/></div>
	                </div>
	            </div>
	            <div class="grid_rows even" id="create_load_balancer_row">	
	                <div class="grid_row_cell" style="width: 25%; ">
	                    <input id="name" class="text" style="width: 70%; " type="text" />
	                    <div id="name_errormsg" class="errormsg" style="display: none;"></div>
	                </div>
	                <div class="grid_row_cell" style="width: 15%;">
	                    <input id="public_port" class="text" style="width: 70%; " type="text" />
	                    <div id="public_port_errormsg" class="errormsg" style="display: none;"></div>
	                </div>
	                <div class="grid_row_cell" style="width: 15%;">
	                    <input id="private_port" class="text" style="width: 70%; " type="text" />
	                    <div id="private_port_errormsg" class="errormsg" style="display: none;"></div>
	                </div>
	                <div class="grid_row_cell" style="width: 15%;">
	                    <select id="algorithm_select" class="select" style="width: 70%;">   
	                        <option value='roundrobin'>roundrobin</option>
			                <option value='leastconn'>leastconn</option>                            
			                <option value='source'>source</option>    	                      
	                    </select>
	                </div>	                
	                <div class="grid_row_cell" style="width: 10%;">	  
                    	<div class="row_celltitles" style="padding:1px;"></div>         
	                </div>	                
	                <div class="grid_row_cell" style="width: 15%; ">
	                    <div class="row_celltitles">
	                        <a id="add_link" href="#"><fmt:message key="label.add"/></a></div>
	                </div>
	            </div>
	            <div id="grid_content">
	            </div>
	        </div>
        </div>      
    </div>
    <!-- Load Balancer ends here-->
	
	<!-- VPN start here-->
	<div id="tab_content_vpn" style="display:none">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    <fmt:message key="label.loading"/> &hellip;</p>
            </div>
        </div>        
        <div id="tab_container" style="display:none">
			<div id="vpn_help" class="info_detailbox defaultbox" style="display:none;"> 
				<p>
					<fmt:message key="message.enabled.vpn"/> : <b><span id="vpn_ip"></span></b><br/>
					<fmt:message key="message.enabled.vpn.ip.sec"/> : <b><span id="vpn_key"></span></b><br/>
				</p>
			</div>
			<div class="grid_container">
	        	<div class="grid_header">
	            	<div id="grid_header_title" class="grid_header_title" style="font-size:10px">VPN Users</div>
	                <div class="grid_actionbox" id="vpn_action_link"><p><fmt:message key="label.actions"/></p>
	                    <div class="grid_actionsdropdown_box" id="vpn_action_menu" style="display: none;">
	                        <ul class="actionsdropdown_boxlist" id="action_list">
	                            <li><fmt:message key="label.no.actions"/></li>
	                        </ul>
	                    </div>
	                </div>
	                <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999; display: none;">
	                    <div class="gridheader_loader" id="icon"></div>
	                    <p id="vpn_enable"><fmt:message key="label.enabling.vpn"/> &hellip;</p>
	                </div>
	            </div>
				<div id="grid_content">
	            </div> 
	        </div>
        </div>    
		<div id="vpn_disabled_msg" class="info_detailbox defaultbox" style="display:none;"> <p><fmt:message key="message.enable.vpn"/></p></div>
    </div>
    <!-- VPN ends here-->
</div>
<!-- IP Address detail panel (end) -->

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="acquire_new_ip_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Acquire New IP" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.acquire.new.ip"/>
            </div>
        </div>
    </div>    
    <div class="actionpanel_button_wrapper" id="add_load_balancer_and_ip_button" style="display:none;">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.load.balancer"/>
            </div>
        </div>
    </div>
</div>
<!--  top buttons (end) -->

<!-- Firewall template (begin) -->
<div class="grid_rows odd" id="firewall_template" style="display: none">    
    <div id="row_container">        
        <div class="grid_row_cell" style="padding:1px; width: 30%; ">
           <div class="row_celltitles" id="cidr"></div>
        </div>   
        <div class="grid_row_cell" style="padding:1px; width: 15%; ">
            <div class="row_celltitles" id="protocol"></div>
        </div>       
        <div class="grid_row_cell" style="padding:1px; width: 10%; ">
            <div class="row_celltitles" id="start_port"></div>
        </div>
        <div class="grid_row_cell" style="padding:1px; width: 10%; ">
            <div class="row_celltitles" id="end_port"></div>
        </div>        
        <div class="grid_row_cell" style="padding:1px; width: 10%; ">
            <div class="row_celltitles" id="ICMP_type"></div>
        </div>
         <div class="grid_row_cell" style="padding:1px; width: 10%; ">
            <div class="row_celltitles" id="ICMP_code"></div>
        </div>
        <div class="grid_row_cell" style="padding:1px; width: 10%; ">
            <div class="row_celltitles">
                <a id="delete_link" href="#" style="float:left;"><fmt:message key="label.delete"/> </a>                            
            </div>
        </div>
        <div class="gridrow_loaderbox" style="display: none;" id="spinning_wheel">
            <div class="gridrow_loader">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/>  &hellip;
            </p>
        </div>
    </div>    
</div>
<!-- Firewall template (end) -->

<!-- Load Balancer Template (begin) -->
<div class="grid_rows odd" id="load_balancer_template" style="display:none">
    <div id="row_container">        
              
        <div class="grid_row_cell" style="width: 25%; ">
            <div class="row_celltitles" id="name"></div>
        </div>
        <div class="grid_row_cell" style="width: 15%; ">
            <div class="row_celltitles" id="public_port"></div>
        </div>
        <div class="grid_row_cell" style="width: 15%; ">
            <div class="row_celltitles" id="private_port"></div>
        </div>
        <div class="grid_row_cell" style="width: 15%; ">
            <div class="row_celltitles" id="algorithm"></div>
        </div>        
        <div class="grid_row_cell" style="width: 10%; ">
            <div class="row_celltitles" id="state" style="padding:1px;"></div>
        </div>  
        <div class="grid_row_cell" style="width: 15%; ">
            <div class="row_celltitles">
                <a id="manage_link" href="#" style="float:left;"><fmt:message key="label.manage"/></a>
                <a id="edit_link" href="#" style="float:left; margin-left:15px;"><fmt:message key="label.edit"/></a>
                <a id="delete_link" href="#" style="float:left; margin-left:15px;" ><fmt:message key="label.delete"/></a>
            </div>
        </div>       
        <div class="gridrow_loaderbox" style="display: none;" id="spinning_wheel">
            <div class="gridrow_loader">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/> &hellip;
            </p>
        </div>
    </div> 
    <div class="grid_rows odd" id="row_container_edit" style="display:none; border-bottom:none;">        
        <div class="grid_row_cell" style="width: 25%; ">
            <div class="row_celltitles" id="cidr" style="padding:1px;"></div>
        </div>       
        <div class="grid_row_cell" style="width: 14%; ">
            <input id="name" class="text" style="width: 70%;" type="text" />
            <div id="name_errormsg" class="errormsg" style="display: none;"></div>
        </div>
        <div class="grid_row_cell" style="width: 12%; ">
            <div class="row_celltitles" id="public_port"></div>
        </div>
        <div class="grid_row_cell" style="width: 12%; ">
            <div class="row_celltitles" id="private_port"></div>
        </div>
        <div class="grid_row_cell" style="width: 15%; ">
            <select id="algorithm_select" class="select" style="width: 70%;">    
                <option value='roundrobin'>roundrobin</option>
                <option value='leastconn'>leastconn</option>                            
                <option value='source'>source</option>            
            </select>
        </div>        
        <div class="grid_row_cell" style="width: 6%; ">
            <div class="row_celltitles" id="state"></div>
        </div>        
        <div class="grid_row_cell" style="width: 17%; ">
            <div class="row_celltitles">
                <a id="save_link" href="#" style="float:left;"><fmt:message key="label.save"/></a>
                <a id="cancel_link" href="#" style="float:left; margin-left:15px; display:inline;"><fmt:message key="label.cancel"/></a>
            </div>
        </div>
        <div class="gridrow_loaderbox" style="display: none;" id="spinning_wheel">
            <div class="gridrow_loader">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/> &hellip;
            </p>
        </div>
    </div>  
    <div class="grid_detailspanel" id="management_area" style="display: none;">
        <div class="grid_details_pointer">
        </div>
        <div class="grid_detailsbox">
            <div class="grid_details_row odd" id="add_vm_to_lb_row">
                <div class="grid_row_cell" style="width: 9%;">
                    <div class="row_celltitles">
                        <img src="images/network_managevmicon.gif" /></div>
                </div>
                <div class="grid_row_cell" style="width: 60%;">
                    <select id="vm_select" class="select" style="width: 90%;">                      
                    </select>
                </div>
                <div class="grid_row_cell" style="width: 30%;">
                    <div class="row_celltitles">
                        <a id="assign_link" href="#"><fmt:message key="label.assign"/></a></div>
                </div>
                <div id="spinning_wheel" class="gridrow_loaderbox" style="display: none;">
                    <div class="gridrow_loader">
                    </div>
                    <p>
                        <fmt:message key="label.assign.to.load.balancer"/> &hellip;</p>
                </div>
            </div>
            <div id="subgrid_content" class="ip_description_managearea">
            </div>
        </div>
    </div>
</div>
<!-- Load Balancer Template (end) -->

<!-- Load Balancer's VM subgrid template (begin) -->
<div id="load_balancer_vm_template" class="grid_details_row odd" style="display:none">
    <div class="grid_row_cell" style="width: 9%;">
        <div class="row_celltitles">
            <img src="images/network_managevmicon.gif" /></div>
    </div>
    <div class="grid_row_cell" style="width: 30%;">
        <div class="row_celltitles" id="vm_name"></div>
    </div>
    <div class="grid_row_cell" style="width: 30%;">
        <div class="row_celltitles" id="vm_private_ip"></div>
    </div>
    <div class="grid_row_cell" style="width: 30%;">
        <div class="row_celltitles">
            <a id="remove_link" href="#">Remove</a></div>
    </div>
    <div id="spinning_wheel" class="gridrow_loaderbox" style="display: none;">
        <div class="gridrow_loader">
        </div>
        <p>
            <fmt:message key="label.remove.from.load.balancer"/> &hellip;</p>
    </div>
</div>
<!-- Load Balancer's VM subgrid template (end) -->

<!-- Port Range template (begin) -->
<div class="grid_rows odd" id="port_range_template" style="display: none">    
    <div id="row_container">
        <div class="grid_row_cell" style="width: 15%; ">
            <div class="row_celltitles" id="start_port"></div>
        </div>
        <div class="grid_row_cell" style="width: 15%; ">
            <div class="row_celltitles" id="end_port"></div>
        </div>
        <div class="grid_row_cell" style="width: 15%; ">
            <div class="row_celltitles" id="protocol"></div>
        </div>       
        <div class="grid_row_cell" style="width: 15%; ">
            <div class="row_celltitles" id="state"></div>
        </div>        
        <div class="grid_row_cell" style="width: 15%; ">
            <div class="row_celltitles">
                <a id="delete_link" href="#" style="float:left;"><fmt:message key="label.delete"/> </a>                            
            </div>
        </div>
        <div class="gridrow_loaderbox" style="display: none;" id="spinning_wheel">
            <div class="gridrow_loader">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/>  &hellip;
            </p>
        </div>
    </div>    
</div>
<!-- Port Range template (end) -->

<!-- Port Forwarding template (begin) -->
<div class="grid_rows odd" id="port_forwarding_template" style="display: none">    
    <div id="row_container">       
        <div class="grid_row_cell" style="width: 15%; ">
            <div class="row_celltitles" id="public_port"></div>
        </div>
        <div class="grid_row_cell" style="width: 15%; ">
            <div class="row_celltitles" id="private_port"></div>
        </div>
        <div class="grid_row_cell" style="width: 10%; ">
            <div class="row_celltitles" id="protocol"></div>
        </div>
        <div class="grid_row_cell" style="width: 30%; ">
            <div class="row_celltitles" id="vm_name"></div>
        </div>   
        <div class="grid_row_cell" style="width: 10%; ">
            <div class="row_celltitles" id="state" style="padding:1px;"></div>
        </div>        
        <div class="grid_row_cell" style="width: 15%; ">
            <div class="row_celltitles">
                <a id="delete_link" href="#" style="float:left;"><fmt:message key="label.delete"/> </a>                                
            </div>
        </div>
        <div class="gridrow_loaderbox" style="display: none;" id="spinning_wheel">
            <div class="gridrow_loader">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/>  &hellip;
            </p>
        </div>
    </div>    
</div>
<!-- Port Forwarding template (end) -->

<!-- VPN Template (begin) -->
<div class="grid_rows odd" id="vpn_template" style="display:none">
	<div class="grid_row_cell" style="width: 20%;">
		<div class="row_celltitles">
			<fmt:message key="label.username"/> :</div>
	</div>
	<div class="grid_row_cell" style="width: 59%;">
		<div class="row_celltitles" id="username"></div>
	</div>
	<div class="grid_row_cell" style="width: 20%;">
		<div class="row_celltitles"><a href="#" id="vpn_delete_user"><fmt:message key="label.delete"/> </a></div>
	</div>
</div>
<!-- VPN Template (end) -->

<!--  dialogs (begin) -->
<div id="dialog_confirmation_remove_vpnuser" title="Confirmation" style="display:none">
    <p>
		<fmt:message key="message.remove.vpn.access"/> : <span id="username"></span>
    </p>
	<!--Loading box-->
	<div id="spinning_wheel" class="ui_dialog_loaderbox" style="display:none;">
		<div class="ui_dialog_loader"></div>
		<p><fmt:message key="label.removing.user"/> ....</p>
	</div>
   
	<!--Confirmation msg box-->
	<!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
	<div id="info_container" class="ui_dialog_messagebox error" style="display:none;">
		<div id="icon" class="ui_dialog_msgicon error"></div>
        <div id="info" class="ui_dialog_messagebox_text error">(info)</div>
	</div>
</div>

<div id="dialog_enable_vpn" title="Enable VPN" style="display:none">
    <p>
		<fmt:message key="message.enable.vpn.access"/> 
    </p>
	<!--Loading box-->
	<div id="spinning_wheel" class="ui_dialog_loaderbox" style="display:none;">
		<div class="ui_dialog_loader"></div>
		<p><fmt:message key="label.enabling.vpn.access"/> ....</p>
	</div>
   
	<!--Confirmation msg box-->
	<!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
	<div id="info_container" class="ui_dialog_messagebox error" style="display:none;">
		<div id="icon" class="ui_dialog_msgicon error"></div>
        <div id="info" class="ui_dialog_messagebox_text error">(info)</div>
	</div>
</div>

<div id="dialog_disable_vpn" title="Disable VPN" style="display:none">
    <p>
		<fmt:message key="message.disable.vpn.access"/> 
    </p>
	<!--Loading box-->
	<div id="spinning_wheel" class="ui_dialog_loaderbox" style="display:none;">
		<div class="ui_dialog_loader"></div>
		<p><fmt:message key="label.disabling.vpn.access"/>....</p>
	</div>
   
	<!--Confirmation msg box-->
	<!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
	<div id="info_container" class="ui_dialog_messagebox error" style="display:none;">
		<div id="icon" class="ui_dialog_msgicon error"></div>
        <div id="info" class="ui_dialog_messagebox_text error">(info)</div>
	</div>
</div>

<div id="dialog_acquire_public_ip" title='<fmt:message key="label.acquire.new.ip"/>' style="display: none">
    <p> 
		<fmt:message key="message.acquire.public.ip"/>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form1">
        <ol>
            <li>
                <label>
                    <fmt:message key="label.zone"/>:</label>
                <select class="select" name="acquire_zone" id="acquire_zone">
                    <option value="default"><fmt:message key="label.waiting"/>....</option>
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>

<div id="dialog_add_load_balancer_and_ip" title='<fmt:message key="label.add.load.balancer"/>' style="display: none">   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form1">
        <ol>
            <li>
                <label>
                    <fmt:message key="label.zone"/>:</label>
                <select class="select" name="acquire_zone" id="acquire_zone">
                    <option value="default"><fmt:message key="label.waiting"/>....</option>
                </select>
            </li>            
            <li>
				<label><fmt:message key="label.name"/>:</label>
				<input class="text" type="text" id="name"/>
				<div id="name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
			</li>
			<li>
				<label><fmt:message key="label.public.port"/>:</label>
				<input class="text" type="text" id="public_port"/>
				<div id="public_port_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
			</li>
			<li>
				<label><fmt:message key="label.private.port"/>:</label>
				<input class="text" type="text" id="private_port"/>
				<div id="private_port_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
			</li>
			<li>
                <label><fmt:message key="label.algorithm"/>:</label>
                <select class="select" id="algorithm_select">
                    <option value='roundrobin'>roundrobin</option>
                    <option value='leastconn'>leastconn</option>                            
                    <option value='source'>source</option>
                </select>
            </li>           
        </ol>
        </form>
    </div>
</div>

<!-- Create User for VPN (begin) -->
<div id="dialog_add_vpnuser" title="Add VPN User" style="display:none">	
	<p> 
		<fmt:message key="message.allow.vpn.access"/>
    </p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form5">
			<ol>			   
				<li>
					<label><fmt:message key="label.username"/>:</label>
					<input class="text" type="text" id="username"/>
					<div id="username_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label><fmt:message key="label.password"/>:</label>
					<input class="text" type="password" id="password"/>
					<div id="password_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>				
			</ol>
		</form>
	</div>
	<!--Loading box-->
	<div id="spinning_wheel" class="ui_dialog_loaderbox" style="display:none;">
		<div class="ui_dialog_loader"></div>
		<p><fmt:message key="label.adding.user"/>....</p>
	</div>
   
	<!--Confirmation msg box-->
	<!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
	<div id="info_container" class="ui_dialog_messagebox error" style="display:none;">
		<div id="icon" class="ui_dialog_msgicon error"></div>
        <div id="info" class="ui_dialog_messagebox_text error">(info)</div>
	</div>
</div>
<!-- Create User for VPN (end) -->

<!-- Enable Static NAT Dialog (begin) -->
<div id="dialog_enable_static_NAT" title='<fmt:message key="label.action.enable.static.NAT"/>' style="display: none">   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form3">
        <ol>
            <li>
                <label for="vm_dropdown">
                    <fmt:message key="label.instance"/>:</label>
                <select class="select" name="vm_dropdown" id="vm_dropdown">
                </select>
                <div id="vm_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Enable Static NAT Dialog (end) -->

<!--  dialogs (end) -->


<div id="hidden_container">
    <!-- advanced search popup (begin) -->
    <div class="adv_searchpopup_bg" id="advanced_search_popup" style="display: none;">
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
    <!-- advanced search popup (end) -->
</div>