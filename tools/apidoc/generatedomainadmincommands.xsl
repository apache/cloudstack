<?xml version="1.0"?>
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
version="1.0">
<xsl:output method="html" doctype-public="-//W3C//DTD HTML 1.0 Transitional//EN"/>
<xsl:template match="/">
<html xmlns="http://www.w3.org/1999/xhtml"><head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<link rel= "stylesheet" href="../includes/main.css" type="text/css" />
<link rel="shortcut icon" href="../favicon.ico" type="image/x-icon" />

<title>Apache CloudStack | The Power Behind Your Cloud</title>
</head>

<body>
<div id="insidetopbg">
<div id="inside_wrapper">
	<div class="uppermenu_panel">
            <div class="uppermenu_box">

</div>
        </div>
        
        <div id="main_master">
            <div id="inside_header">

                <div class="header_top">
                    <a class="cloud_logo" href="http://cloudstack.org"></a>
                    <div class="mainemenu_panel">
                        
                    </div>
                </div>

            
            </div>

            <div id="main_content">
             	
                <div class="inside_apileftpanel">
                	<div class="inside_contentpanel" style="width:930px;">
                    	<div class="api_titlebox">
                        	<div class="api_titlebox_left">
				<xsl:for-each select="command/command">
                           		<!-- Modify this line for the release version -->
                           		<span>
									Apache CloudStack v4.6.0 Domain Admin API Reference
								</span>
								<p></p>
                                <h1><xsl:value-of select="name"/></h1>
                                <p><xsl:value-of select="description"/></p>
				</xsl:for-each>
                            </div>

                            
                            <div class="api_titlebox_right">
                            	<a class="api_backbutton" href="../TOC_Domain_Admin.html"></a>
                            </div>
                        </div>
                    	<div class="api_tablepanel">     
                         	<h2>Request parameters</h2>
                        	<table class="apitable">
                            	<tr class="hed">
                                	<td style="width:200px;"><strong>Parameter Name</strong></td>

                                    <td style="width:500px;">Description</td>
                                    <td style="width:180px;">Required</td>
                                </tr>
				<xsl:for-each select="command/command/request/arg">
                                <tr>
				    <xsl:if test="required='true'">
                                    <td style="width:200px;"><strong><xsl:value-of select="name"/></strong></td>
				    <td style="width:500px;"><strong><xsl:value-of select="description"/></strong></td>
                                    <td style="width:180px;"><strong><xsl:value-of select="required"/></strong></td>
				    </xsl:if>
				    <xsl:if test="required='false'">
                			<td style="width:200px;"><i><xsl:value-of select="name"/></i></td>
                                    <td style="width:500px;"><i><xsl:value-of select="description"/></i></td>
                                    <td style="width:180px;"><i><xsl:value-of select="required"/></i></td>
				    </xsl:if>
                                </tr>
				</xsl:for-each>
                            </table>
                        </div>
                         
                         
                         <div class="api_tablepanel">     
                         	<h2>Response Tags</h2>
                        	<table class="apitable">
                            	<tr class="hed">
                                	<td style="width:200px;"><strong>Response Name</strong></td>
                                    <td style="width:500px;">Description</td>
                                </tr>
                                
				<xsl:for-each select="command/command/response/arg">
                                <tr>
					<td style="width:200px;"><strong><xsl:value-of select="name"/></strong></td>
                                    <td style="width:500px;"><xsl:value-of select="description"/></td>
					<xsl:for-each select="./arguments/arg">
					<tr>
					<td style="width:180px; padding-left:25px;"><strong><xsl:value-of select="name"/></strong></td>
                                    	<td style="width:500px;"><xsl:value-of select="description"/></td>
					</tr>
						<xsl:for-each select="./arguments/arg">
						<tr>
						<td style="width:165px; padding-left:40px;"><xsl:value-of select="name"/></td>
		                            	<td style="width:500px;"><xsl:value-of select="description"/></td>
						</tr>
					</xsl:for-each>					
					</xsl:for-each>					
                                </tr>
				</xsl:for-each>
                                
                         
                                
                              
                            
                            </table>

                        </div>
                        
                        
                </div> 
                </div>
                  
     
            </div>
        </div>
<div id="footer">
        	<div id="footer_mainmaster">
                <p>Copyright © 2015 The Apache Software Foundation, Licensed under the
                   <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License, Version 2.0.</a> <br />
                Apache, CloudStack, Apache CloudStack, the Apache CloudStack logo, the CloudMonkey logo  and the Apache feather logo are trademarks of The Apache Software Foundation.</p>
            </div>
        </div>

  </div>
 </div>
</body>
</html>
</xsl:template>
</xsl:stylesheet>

