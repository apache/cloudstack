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

<title>CloudStack API Reference</title>
</head>

<body>
<div id="insidetopbg">
<div id="inside_wrapper">
	<div class="uppermenu_panel">
            <div class="uppermenu_box"><!-- #BeginLibraryItem "/libraries/uppermenu.lbi" -->

<div class="uppermenu">
       <a href="libraries/learn_download.html">Downloads</a> | <a href="libraries/company_news.html">News</a> | <a href="#">Contact Us</a> 
</div><!-- #EndLibraryItem --></div>
        </div>
        
        <div id="main_master">
            <div id="inside_header">

                <div class="header_top">
                    <a class="cloud_logo" href="index.html"></a>
                    <div class="mainemenu_panel">
                        
                    </div>
                </div>
                <div class="insideheader_bot">
                	<div class="insideheader_botleft">
                    	<h1></h1>
                    </div>

                    <div class="insideheader_botright">
                    	<div class="insideheader_button">
                        	<a class="insjoincomm_button" href="#"></a>
                            <a class="insdownload_button" href="#"></a>
                        </div>
                        <div class="insheader_buttonshadow"></div>
                    </div>
                </div>
            
            </div>

            <div id="main_content">
             	
                <div class="inside_apileftpanel">
                	<div class="inside_contentpanel" style="width:930px;">
                    	<div class="api_titlebox">
                        	<div class="api_titlebox_left">
				<xsl:for-each select="command/command">
                                <h1><xsl:value-of select="name"/></h1>
                                <span><xsl:value-of select="description"/></span>
				</xsl:for-each>
                            </div>

                            
                            <div class="api_titlebox_right">
                            	<a class="api_backbutton" href="#"></a>
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
					<td style="width:200px;"><strong><xsl:value-of select="name"/></strong></td>
                                    <td style="width:500px;"><xsl:value-of select="description"/></td>
                                    <td style="width:180px;"><xsl:value-of select="required"/></td>

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
					<td style="width:200px;"><strong><xsl:value-of select="name"/></strong></td>
                                    	<td style="width:500px;"><xsl:value-of select="description"/></td>
					</tr>
						<xsl:for-each select="./arguments/arg">
						<tr>
						<td style="width:200px;"><strong><xsl:value-of select="name"/></strong></td>
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
        </div><!-- #BeginLibraryItem "/libraries/footer.lbi" -->
<div id="footer">

        	<div id="footer_mainmaster">
            	<ul class="footer_linksbox">
                	<li><strong> Main </strong></li>
                    <li> <a href="index.html"> Home</a> </li>
                    <li> <a href="learn_whatcloud.html"> Learn</a> </li>

                    <li> <a href="products_cloudplatform.html"> Products</a> </li>
                    <li> <a href="#"> Community</a> </li>
                    <li> <a href="service_overview.html"> Services</a> </li>

                    <li> <a href="Partners_Main.html"> Partners</a> </li>
                    <li> <a href="company_about.html"> Company</a> </li>
                </ul>
                <ul class="footer_linksbox">
                	<li><strong> Sub </strong> </li>

                    <li> <a href="learn_videos.html"> Tour</a> </li>
                    <li> <a href="learn_download.html"> Downloads</a> </li>
                    <li> <a href="learn_FAQ.html"> FAQs</a> </li>

                    <li> <a href="#"> Blog</a> </li>
                    <li> <a href="#"> Contacts</a> </li>
                   
                </ul>
                <ul class="footer_linksbox">
                	<li><strong> Site Info </strong> </li>

                    <li> <a href="#"> Privacy Policy</a> </li>
                    <li> <a href="#"> Term of Use</a> </li>
                    <li> <a href="#"> Contacts</a> </li>

                </ul>
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

