// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.hypervisor.ovm3.objects;

import java.util.UUID;

import org.junit.Test;

public class XenTest {
    public XenTest() {
    }
    ConnectionTest con = new ConnectionTest();
    Xen xEn = new Xen(con);
    XmlTestResultTest results = new XmlTestResultTest();
    String DOM0VMNAME = "Domain-0";
    String VMNAME = "i-2-3-VM";
    String VMROOTDISKNAME = "ROOT-3";
    public String getVMNAME() {
        return VMNAME;
    }
    String REPOID = "f12842ebf5ed3fe78da1eb0e17f5ede8";
    public String getRepoId() {
        return REPOID;
    }
    public String getVmId() {
        return VMID;
    }
    String VMNICMAC = "02:00:50:9a:00:01";
    public String getVmNicMac() {
        return VMNICMAC;
    }
    public String getVmNicBridge() {
        return VMNICBR;
    }
    public String getVmNicUuid() {
        return VMNICUUID;
    }
    String VMNICBR = "xenbr0.160";
    String VMNICUUID = "2ad52371-af7d-32d1-ebe1-2b6a811e66c4";
    String VMID = "868a6627-c3b0-3d9b-aea4-f279cbaa253b";
    String VMROOTDISKUUID = "722eb520-dcf5-4113-8f45-22d67c9a2f3c";
    public String getVmRootDiskUuid() {
        return VMROOTDISKUUID;
    }
    public String getVmRootDiskName() {
        return VMROOTDISKNAME;
    }
    String VMROOTDISK = VMROOTDISKUUID + ".raw";
    String VMISO = "xentools.iso";
    String REPOPATH = "/OVS/Repositories";
    String VMROOTDISKPATH = REPOPATH + "/" + REPOID + "/Disks/" + VMROOTDISK;
    String VMISOPATH = REPOPATH + "/" + REPOID + "/ISOs/" + VMISO;
    String MULTIPLEVMSLISTXML = results
            .simpleResponseWrapWrapper(new StringBuilder("<array><data>\n")
                    .append("<value><struct>\n<member>\n<name>on_xend_stop</name>\n<value><string>ignore</string></value>\n</member>\n<member>\n<name>features</name>\n<value><string></string></value>\n</member>\n<member>\n<name>image</name>\n")
                    .append("<value><struct>\n<member>\n<name>tsc_mode</name>\n<value><string>\n</string></value>\n</member>\n<member>\n<name>kernel</name>\n<value><string></string></value>\n</member>\n<member>\n<name>superpages</name>\n")
                    .append("<value><string>\n</string></value>\n</member>\n<member>\n<name>nomigrate</name>\n<value><string>\n</string></value>\n</member>\n<member>\n<name>expose_host_uuid</name>\n<value><string>\n</string></value>\n</member>\n")
                    .append("</struct></value>\n</member>\n<member>\n<name>cpus</name>\n<value><array><data>\n<value><array><data>\n<value><string>\n</string></value>\n<value><string>\n</string></value>\n<value><string>\n</string></value>\n")
                    .append("<value><string>\n</string></value>\n<value><string>\n</string></value>\n<value><string>\n</string></value>\n<value><string>\n</string></value>\n<value><string>\n</string></value>\n<value><string>\n")
                    .append("</string></value>\n<value><string>\n</string></value>\n<value><string>10</string></value>\n<value><string>11</string></value>\n<value><string>12</string></value>\n<value><string>13</string></value>\n")
                    .append("<value><string>14</string></value>\n<value><string>15</string></value>\n<value><string>16</string></value>\n<value><string>17</string></value>\n<value><string>18</string></value>\n")
                    .append("<value><string>19</string></value>\n<value><string>20</string></value>\n<value><string>21</string></value>\n<value><string>22</string></value>\n<value><string>23</string></value>\n")
                    .append("<value><string>24</string></value>\n<value><string>25</string></value>\n<value><string>26</string></value>\n<value><string>27</string></value>\n<value><string>28</string></value>\n")
                    .append("<value><string>29</string></value>\n<value><string>30</string></value>\n<value><string>31</string></value>\n<value><string>32</string></value>\n<value><string>33</string></value>\n")
                    .append("<value><string>34</string></value>\n<value><string>35</string></value>\n<value><string>36</string></value>\n<value><string>37</string></value>\n<value><string>38</string></value>\n")
                    .append("<value><string>39</string></value>\n<value><string>40</string></value>\n<value><string>41</string></value>\n<value><string>42</string></value>\n<value><string>43</string></value>\n")
                    .append("<value><string>44</string></value>\n<value><string>45</string></value>\n<value><string>46</string></value>\n<value><string>47</string></value>\n<value><string>48</string></value>\n")
                    .append("<value><string>49</string></value>\n<value><string>50</string></value>\n<value><string>51</string></value>\n<value><string>52</string></value>\n<value><string>53</string></value>\n")
                    .append("<value><string>54</string></value>\n<value><string>55</string></value>\n<value><string>56</string></value>\n<value><string>57</string></value>\n<value><string>58</string></value>\n")
                    .append("<value><string>59</string></value>\n<value><string>60</string></value>\n<value><string>61</string></value>\n<value><string>62</string></value>\n<value><string>63</string></value>\n")
                    .append("<value><string>64</string></value>\n<value><string>65</string></value>\n<value><string>66</string></value>\n<value><string>67</string></value>\n<value><string>68</string></value>\n")
                    .append("<value><string>69</string></value>\n<value><string>70</string></value>\n<value><string>71</string></value>\n<value><string>72</string></value>\n<value><string>73</string></value>\n")
                    .append("<value><string>74</string></value>\n<value><string>75</string></value>\n<value><string>76</string></value>\n<value><string>77</string></value>\n<value><string>78</string></value>\n")
                    .append("<value><string>79</string></value>\n<value><string>80</string></value>\n<value><string>81</string></value>\n<value><string>82</string></value>\n<value><string>83</string></value>\n")
                    .append("<value><string>84</string></value>\n<value><string>85</string></value>\n<value><string>86</string></value>\n<value><string>87</string></value>\n<value><string>88</string></value>\n")
                    .append("<value><string>89</string></value>\n<value><string>90</string></value>\n<value><string>91</string></value>\n<value><string>92</string></value>\n<value><string>93</string></value>\n")
                    .append("<value><string>94</string></value>\n<value><string>95</string></value>\n<value><string>96</string></value>\n<value><string>97</string></value>\n<value><string>98</string></value>\n")
                    .append("<value><string>99</string></value>\n<value><string>100</string></value>\n<value><string>101</string></value>\n<value><string>102</string></value>\n<value><string>103</string></value>\n")
                    .append("<value><string>104</string></value>\n<value><string>105</string></value>\n<value><string>106</string></value>\n<value><string>107</string></value>\n<value><string>108</string></value>\n")
                    .append("<value><string>109</string></value>\n<value><string>110</string></value>\n<value><string>111</string></value>\n<value><string>112</string></value>\n<value><string>113</string></value>\n")
                    .append("<value><string>114</string></value>\n<value><string>115</string></value>\n<value><string>116</string></value>\n<value><string>117</string></value>\n<value><string>118</string></value>\n")
                    .append("<value><string>119</string></value>\n<value><string>120</string></value>\n<value><string>121</string></value>\n<value><string>122</string></value>\n<value><string>123</string></value>\n")
                    .append("<value><string>124</string></value>\n<value><string>125</string></value>\n<value><string>126</string></value>\n<value><string>127</string></value>\n<value><string>128</string></value>\n")
                    .append("<value><string>129</string></value>\n<value><string>130</string></value>\n<value><string>131</string></value>\n<value><string>132</string></value>\n<value><string>133</string></value>\n")
                    .append("<value><string>134</string></value>\n<value><string>135</string></value>\n<value><string>136</string></value>\n<value><string>137</string></value>\n<value><string>138</string></value>\n")
                    .append("<value><string>139</string></value>\n<value><string>140</string></value>\n<value><string>141</string></value>\n<value><string>142</string></value>\n<value><string>143</string></value>\n")
                    .append("<value><string>144</string></value>\n<value><string>145</string></value>\n<value><string>146</string></value>\n<value><string>147</string></value>\n<value><string>148</string></value>\n")
                    .append("<value><string>149</string></value>\n<value><string>150</string></value>\n<value><string>151</string></value>\n<value><string>152</string></value>\n<value><string>153</string></value>\n")
                    .append("<value><string>154</string></value>\n<value><string>155</string></value>\n<value><string>156</string></value>\n<value><string>157</string></value>\n<value><string>158</string></value>\n")
                    .append("<value><string>159</string></value>\n</data></array></value>\n<value><array><data>\n<value><string>\n</string></value>\n<value><string>\n</string></value>\n<value><string>\n</string></value>\n")
                    .append("<value><string>\n</string></value>\n<value><string>\n</string></value>\n<value><string>\n</string></value>\n<value><string>\n</string></value>\n<value><string>\n</string></value>\n")
                    .append("<value><string>\n</string></value>\n<value><string>\n</string></value>\n<value><string>10</string></value>\n<value><string>11</string></value>\n<value><string>12</string></value>\n")
                    .append("<value><string>13</string></value>\n<value><string>14</string></value>\n<value><string>15</string></value>\n<value><string>16</string></value>\n<value><string>17</string></value>\n")
                    .append("<value><string>18</string></value>\n<value><string>19</string></value>\n<value><string>20</string></value>\n<value><string>21</string></value>\n<value><string>22</string></value>\n")
                    .append("<value><string>23</string></value>\n<value><string>24</string></value>\n<value><string>25</string></value>\n<value><string>26</string></value>\n<value><string>27</string></value>\n")
                    .append("<value><string>28</string></value>\n<value><string>29</string></value>\n<value><string>30</string></value>\n<value><string>31</string></value>\n<value><string>32</string></value>\n")
                    .append("<value><string>33</string></value>\n<value><string>34</string></value>\n<value><string>35</string></value>\n<value><string>36</string></value>\n<value><string>37</string></value>\n")
                    .append("<value><string>38</string></value>\n<value><string>39</string></value>\n<value><string>40</string></value>\n<value><string>41</string></value>\n<value><string>42</string></value>\n")
                    .append("<value><string>43</string></value>\n<value><string>44</string></value>\n<value><string>45</string></value>\n<value><string>46</string></value>\n<value><string>47</string></value>\n")
                    .append("<value><string>48</string></value>\n<value><string>49</string></value>\n<value><string>50</string></value>\n<value><string>51</string></value>\n<value><string>52</string></value>\n")
                    .append("<value><string>53</string></value>\n<value><string>54</string></value>\n<value><string>55</string></value>\n<value><string>56</string></value>\n<value><string>57</string></value>\n")
                    .append("<value><string>58</string></value>\n<value><string>59</string></value>\n<value><string>60</string></value>\n<value><string>61</string></value>\n<value><string>62</string></value>\n")
                    .append("<value><string>63</string></value>\n<value><string>64</string></value>\n<value><string>65</string></value>\n<value><string>66</string></value>\n<value><string>67</string></value>\n")
                    .append("<value><string>68</string></value>\n<value><string>69</string></value>\n<value><string>70</string></value>\n<value><string>71</string></value>\n<value><string>72</string></value>\n")
                    .append("<value><string>73</string></value>\n<value><string>74</string></value>\n<value><string>75</string></value>\n<value><string>76</string></value>\n<value><string>77</string></value>\n")
                    .append("<value><string>78</string></value>\n<value><string>79</string></value>\n<value><string>80</string></value>\n<value><string>81</string></value>\n<value><string>82</string></value>\n")
                    .append("<value><string>83</string></value>\n<value><string>84</string></value>\n<value><string>85</string></value>\n<value><string>86</string></value>\n<value><string>87</string></value>\n")
                    .append("<value><string>88</string></value>\n<value><string>89</string></value>\n<value><string>90</string></value>\n<value><string>91</string></value>\n<value><string>92</string></value>\n")
                    .append("<value><string>93</string></value>\n<value><string>94</string></value>\n<value><string>95</string></value>\n<value><string>96</string></value>\n<value><string>97</string></value>\n")
                    .append("<value><string>98</string></value>\n<value><string>99</string></value>\n<value><string>100</string></value>\n<value><string>101</string></value>\n<value><string>102</string></value>\n")
                    .append("<value><string>103</string></value>\n<value><string>104</string></value>\n<value><string>105</string></value>\n<value><string>106</string></value>\n<value><string>107</string></value>\n")
                    .append("<value><string>108</string></value>\n<value><string>109</string></value>\n<value><string>110</string></value>\n<value><string>111</string></value>\n<value><string>112</string></value>\n")
                    .append("<value><string>113</string></value>\n<value><string>114</string></value>\n<value><string>115</string></value>\n<value><string>116</string></value>\n<value><string>117</string></value>\n")
                    .append("<value><string>118</string></value>\n<value><string>119</string></value>\n<value><string>120</string></value>\n<value><string>121</string></value>\n<value><string>122</string></value>\n")
                    .append("<value><string>123</string></value>\n<value><string>124</string></value>\n<value><string>125</string></value>\n<value><string>126</string></value>\n<value><string>127</string></value>\n")
                    .append("<value><string>128</string></value>\n<value><string>129</string></value>\n<value><string>130</string></value>\n<value><string>131</string></value>\n<value><string>132</string></value>\n")
                    .append("<value><string>133</string></value>\n<value><string>134</string></value>\n<value><string>135</string></value>\n<value><string>136</string></value>\n<value><string>137</string></value>\n")
                    .append("<value><string>138</string></value>\n<value><string>139</string></value>\n<value><string>140</string></value>\n<value><string>141</string></value>\n<value><string>142</string></value>\n")
                    .append("<value><string>143</string></value>\n<value><string>144</string></value>\n<value><string>145</string></value>\n<value><string>146</string></value>\n<value><string>147</string></value>\n")
                    .append("<value><string>148</string></value>\n<value><string>149</string></value>\n<value><string>150</string></value>\n<value><string>151</string></value>\n<value><string>152</string></value>\n")
                    .append("<value><string>153</string></value>\n<value><string>154</string></value>\n<value><string>155</string></value>\n<value><string>156</string></value>\n<value><string>157</string></value>\n")
                    .append("<value><string>158</string></value>\n<value><string>159</string></value>\n</data></array></value>\n</data></array></value>\n</member>\n<member>\n<name>uuid</name>\n<value><string>00000000-0000-0000-0000-000000000000</string></value>\n")
                    .append("</member>\n<member>\n<name>on_reboot</name>\n<value><string>restart</string></value>\n</member>\n<member>\n<name>state</name>\n<value><string>r-----</string></value>\n</member>\n")
                    .append("<member>\n<name>cpu_weight</name>\n<value><string>65535</string></value>\n</member>\n<member>\n<name>online_vcpus</name>\n<value><string>\n</string></value>\n</member>\n")
                    .append("<member>\n<name>memory</name>\n<value><string>672</string></value>\n</member>\n<member>\n<name>cpu_cap</name>\n<value><string>\n</string></value>\n</member>\n<member>\n<name>status</name>\n")
                    .append("<value><string>\n</string></value>\n</member>\n<member>\n<name>pool_name</name>\n<value><string>Pool-0</string></value>\n</member>\n<member>\n<name>on_poweroff</name>\n<value><string>destroy</string></value>\n")
                    .append("</member>\n<member>\n<name>on_xend_start</name>\n<value><string>ignore</string></value>\n</member>\n<member>\n<name>on_crash</name>\n<value><string>restart</string></value>\n</member>\n")
                    .append("<member>\n<name>device</name>\n<value><struct>\n</struct></value>\n</member>\n<member>\n<name>bootloader</name>\n<value><string></string></value>\n</member>\n<member>\n<name>maxmem</name>\n")
                    .append("<value><string>672</string></value>\n</member>\n<member>\n<name>cpu_time</name>\n<value><string>6608.51291287</string></value>\n</member>\n<member>\n<name>shadow_memory</name>\n<value><string>\n")
                    .append("</string></value>\n</member>\n<member>\n<name>name</name>\n<value><string>Domain-0</string></value>\n</member>\n<member>\n<name>builder</name>\n<value><string>linux</string></value>\n</member>\n")
                    .append("<member>\n<name>bootloader_args</name>\n<value><string></string></value>\n</member>\n<member>\n<name>domid</name>\n<value><string>\n</string></value>\n</member>\n<member>\n<name>vcpus</name>\n")
                    .append("<value><string>\n</string></value>\n</member>\n</struct></value>\n<value><struct>\n<member>\n<name>on_xend_stop</name>\n<value><string>ignore</string></value>\n</member>\n<member>\n")
                    .append("<name>console_mfn</name>\n<value><string>873706</string></value>\n</member>\n<member>\n<name>features</name>\n<value><string></string></value>\n</member>\n<member>\n<name>image</name>\n")
                    .append("<value><struct>\n<member>\n<name>tsc_mode</name>\n<value><string>\n</string></value>\n</member>\n<member>\n<name>kernel</name>\n<value><string></string></value>\n</member>\n")
                    .append("<member>\n<name>videoram</name>\n<value><string>\n</string></value>\n</member>\n<member>\n<name>device_model</name>\n<value><string>/usr/lib/xen/bin/qemu-dm</string></value>\n</member>\n")
                    .append("<member>\n<name>notes</name><value><array><data><value><array><data><value><string>HV_START_LOW</string></value><value><string>4118806528</string></value></data></array></value>")
                    .append("<value><array><data><value><string>FEATURES</string></value><value><string>!writable_page_tables|pae_pgdir_above_4gb</string></value></data></array></value><value><array><data>")
                    .append("<value><string>VIRT_BASE</string></value><value><string>3221225472</string></value></data></array></value><value><array><data><value><string>GUEST_VERSION</string></value><value><string>2.6</string></value>")
                    .append("</data></array></value><value><array><data><value><string>PADDR_OFFSET</string></value><value><string></string></value></data></array></value><value><array><data><value><string>GUEST_OS</string></value>")
                    .append("<value><string>linux</string></value></data></array></value><value><array><data><value><string>HYPERCALL_PAGE</string></value><value><string>3238010880</string></value></data></array></value><value><array><data>")
                    .append("<value><string>LOADER</string></value><value><string>generic</string></value></data></array></value><value><array><data><value><string>SUSPEND_CANCEL</string></value><value><string></string></value>")
                    .append("</data></array></value><value><array><data><value><string>PAE_MODE</string></value><value><string>yes</string></value></data></array></value><value><array><data><value><string>ENTRY</string></value>")
                    .append("<value><string>3242303488</string></value></data></array></value><value><array><data><value><string>XEN_VERSION</string></value><value><string>xen-3.0</string></value></data></array></value>")
                    .append("</data></array></value></member><member><name>expose_host_uuid</name><value><string></string></value></member><member><name>pci</name><value><array><data></data></array></value></member>")
                    .append("<member><name>superpages</name>\n<value><string>\n</string></value>\n</member>\n<member>\n<name>nomigrate</name>\n<value><string>\n</string></value>\n</member>\n</struct></value>\n</member>\n")
                    .append("<member>\n<name>cpus</name>\n<value><array><data>\n<value><array><data>\n</data></array></value>\n</data></array></value>\n</member>\n<member>\n<name>store_mfn</name>\n<value><string>873707</string></value>\n")
                    .append("</member>\n<member>\n<name>uuid</name>\n<value><string>").append(VMID).append("</string></value>\n</member>\n<member>\n<name>on_reboot</name>\n<value><string>restart</string></value>\n</member>\n")
                    .append("<member>\n<name>state</name>\n<value><string>-b----</string></value>\n</member>\n<member>\n<name>cpu_weight</name>\n<value><string>27500</string></value>\n</member>\n<member>\n<name>online_vcpus</name>\n")
                    .append("<value><string>\n</string></value>\n</member>\n<member>\n<name>memory</name>\n<value><string>512</string></value>\n</member>\n<member>\n<name>cpu_cap</name>\n<value><string>\n</string></value>\n")
                    .append("</member>\n<member>\n<name>status</name>\n<value><string>\n</string></value>\n</member>\n<member>\n<name>pool_name</name>\n<value><string>Pool-0</string></value>\n</member>\n<member>\n<name>description</name>\n")
                    .append("<value><string></string></value>\n</member>\n<member>\n<name>start_time</name>\n<value><string>1408105444.17</string></value>\n</member>\n<member>\n<name>on_poweroff</name>\n<value><string>destroy</string></value>\n")
                    .append("</member>\n<member>\n<name>on_xend_start</name>\n<value><string>ignore</string></value>\n</member>\n<member>\n<name>on_crash</name>\n<value><string>restart</string></value>\n</member>\n")
                    .append("<member>\n<name>device</name>\n<value><struct>\n<member>\n<name>vif</name>\n<value><array><data>\n<value><struct>\n<member>\n<name>bridge</name>\n<value><string>").append(VMNICBR)
                    .append("</string></value>\n</member>\n<member>\n<name>mac</name>\n<value><string>").append(VMNICMAC).append("</string></value>\n</member>\n<member>\n<name>script</name>\n<value><string>/etc/xen/scripts/vif-bridge</string></value>\n")
                    .append("</member>\n<member>\n<name>uuid</name>\n<value><string>").append(VMNICUUID).append("</string></value>\n</member>\n<member>\n<name>backend</name>\n<value><string>\n</string></value>\n</member>\n")
                    .append("</struct></value>\n</data></array></value>\n</member>\n<member>\n<name>vkbd</name>\n<value><array><data>\n<value><struct>\n<member>\n<name>backend</name>\n<value><string>\n</string></value>\n")
                    .append("</member>\n</struct></value>\n</data></array></value>\n</member>\n<member>\n<name>console</name>\n<value><array><data>\n<value><struct>\n<member>\n<name>protocol</name>\n<value><string>vt100</string></value>\n")
                    .append("</member>\n<member>\n<name>location</name>\n<value><string>\n</string></value>\n</member>\n<member>\n<name>uuid</name>\n<value><string>9beb5016-dde7-8526-491f-e972f262a634</string></value>\n")
                    .append("</member>\n</struct></value>\n</data></array></value>\n</member>\n<member>\n<name>vfb</name>\n<value><array><data>\n<value><struct>\n<member>\n<name>vncunused</name>\n<value><string>\n")
                    .append("</string></value>\n</member>\n<member>\n<name>vnc</name>\n<value><string>\n</string></value>\n</member>\n<member>\n<name>xauthority</name>\n<value><string>/root/.Xauthority</string></value>\n")
                    .append("</member>\n<member>\n<name>vnclisten</name>\n<value><string>0.0.0.0</string></value>\n</member>\n<member>\n<name>vncpasswd</name>\n<value><string>7693f834ca67912e</string></value>\n")
                    .append("</member>\n<member>\n<name>keymap</name>\n<value><string>en-us</string></value>\n</member>\n<member>\n<name>location</name>\n<value><string>0.0.0.0:5900</string></value>\n</member>\n")
                    .append("<member>\n<name>uuid</name>\n<value><string>78edf954-e375-b142-9c76-791ce805b6ef</string></value>\n</member>\n</struct></value>\n</data></array></value>\n</member>\n<member>\n<name>vbd</name>\n")
                    .append("<value><array><data>\n<value><struct>\n<member>\n<name>protocol</name>\n<value><string>x86_32-abi</string></value>\n</member>\n<member>\n<name>uuid</name>\n<value><string>bda35954-2596-025e-168c-b82e1cf92369</string></value>\n")
                    .append("</member>\n<member>\n<name>bootable</name>\n<value><string>\n</string></value>\n</member>\n<member>\n<name>dev</name>\n<value><string>xvda:disk</string></value>\n</member>\n<member>\n<name>uname</name>\n")
                    .append("<value><string>file:/OVS/Repositories/").append(REPOID).append("/VirtualDisks/").append(VMROOTDISK).append("</string></value>\n</member>\n<member>\n<name>mode</name>\n<value><string>\n")
                    .append("</string></value>\n</member>\n<member>\n<name>VDI</name>\n<value><string></string></value>\n</member>\n<member>\n<name>backend</name>\n<value><string>\n</string></value>\n</member>\n</struct></value>\n")
                    .append("</data></array></value>\n</member>\n</struct></value>\n</member>\n<member>\n<name>bootloader</name>\n<value><string>/usr/bin/pygrub</string></value>\n</member>\n<member>\n<name>maxmem</name>\n")
                    .append("<value><string>512</string></value>\n</member>\n<member>\n<name>cpu_time</name>\n<value><string>0.152510481</string></value>\n</member>\n<member>\n<name>shadow_memory</name>\n<value><string>\n")
                    .append("</string></value>\n</member>\n<member>\n<name>name</name>\n<value><string>").append(VMNAME).append("</string></value>\n</member>\n<member>\n<name>builder</name>\n<value><string>linux</string></value>\n")
                    .append("</member>\n<member>\n<name>bootloader_args</name>\n<value><string>-q</string></value>\n</member>\n<member>\n<name>domid</name>\n<value><string>\n</string></value>\n</member>\n<member>\n")
                    .append("<name>vcpus</name>\n<value><string>\n</string></value>\n</member>\n</struct></value>\n</data></array>").toString());

    public String getSingleVmListXML() {
        return SINGLEVMLISTXML;
    }
    String SINGLEVMLISTXML = results
            .simpleResponseWrapWrapper(new StringBuilder("<struct>")
                    .append("<member>")
                    .append("<name>on_xend_stop</name>")
                    .append("<value><string>ignore</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>console_mfn</name>")
                    .append("<value><string>873706</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>features</name>")
                    .append("<value><string></string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>image</name>")
                    .append("<value><struct>")
                    .append("<member>")
                    .append("<name>tsc_mode</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>kernel</name>")
                    .append("<value><string></string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>videoram</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>device_model</name>")
                    .append("<value><string>/usr/lib/xen/bin/qemu-dm</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>notes</name>")
                    .append("<value><array><data>")
                    .append("<value><array><data>")
                    .append("<value><string>HV_START_LOW</string></value>")
                    .append("<value><string>4118806528</string></value>")
                    .append("</data></array></value>")
                    .append("<value><array><data>")
                    .append("<value><string>FEATURES</string></value>")
                    .append("<value><string>!writable_page_tables|pae_pgdir_above_4gb</string></value>")
                    .append("</data></array></value>") .append("<value><array><data>")
                    .append("<value><string>VIRT_BASE</string></value>")
                    .append("<value><string>3221225472</string></value>")
                    .append("</data></array></value>") .append("<value><array><data>")
                    .append("<value><string>GUEST_VERSION</string></value>")
                    .append("<value><string>2.6</string></value>")
                    .append("</data></array></value>") .append("<value><array><data>")
                    .append("<value><string>PADDR_OFFSET</string></value>")
                    .append("<value><string>") .append("</string></value>")
                    .append("</data></array></value>") .append("<value><array><data>")
                    .append("<value><string>GUEST_OS</string></value>")
                    .append("<value><string>linux</string></value>")
                    .append("</data></array></value>") .append("<value><array><data>")
                    .append("<value><string>HYPERCALL_PAGE</string></value>")
                    .append("<value><string>3238010880</string></value>")
                    .append("</data></array></value>") .append("<value><array><data>")
                    .append("<value><string>LOADER</string></value>")
                    .append("<value><string>generic</string></value>")
                    .append("</data></array></value>") .append("<value><array><data>")
                    .append("<value><string>SUSPEND_CANCEL</string></value>")
                    .append("<value><string>") .append("</string></value>")
                    .append("</data></array></value>") .append("<value><array><data>")
                    .append("<value><string>PAE_MODE</string></value>")
                    .append("<value><string>yes</string></value>")
                    .append("</data></array></value>") .append("<value><array><data>")
                    .append("<value><string>ENTRY</string></value>")
                    .append("<value><string>3242303488</string></value>")
                    .append("</data></array></value>") .append("<value><array><data>")
                    .append("<value><string>XEN_VERSION</string></value>")
                    .append("<value><string>xen-3.0</string></value>")
                    .append("</data></array></value>") .append("</data></array></value>")
                    .append("</member>") .append("<member>")
                    .append("<name>expose_host_uuid</name>") .append("<value><string>")
                    .append("</string></value>") .append("</member>") .append("<member>")
                    .append("<name>pci</name>") .append("<value><array><data>")
                    .append("</data></array></value>") .append("</member>") .append("<member>")
                    .append("<name>superpages</name>") .append("<value><string>")
                    .append("</string></value>") .append("</member>") .append("<member>")
                    .append("<name>nomigrate</name>") .append("<value><string>")
                    .append("</string></value>") .append("</member>") .append("</struct></value>")
                    .append("</member>") .append("<member>") .append("<name>cpus</name>")
                    .append("<value><array><data>") .append("<value><array><data>")
                    .append("</data></array></value>") .append("</data></array></value>")
                    .append("</member>") .append("<member>") .append("<name>store_mfn</name>")
                    .append("<value><string>873707</string></value>") .append("</member>")
                    .append("<member>") .append("<name>uuid</name>") .append("<value><string>")
                    .append(VMID)
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>on_reboot</name>")
                    .append("<value><string>restart</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>state</name>")
                    .append("<value><string>-b----</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>cpu_weight</name>")
                    .append("<value><string>27500</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>online_vcpus</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>memory</name>")
                    .append("<value><string>512</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>cpu_cap</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>status</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>pool_name</name>")
                    .append("<value><string>Pool-0</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>description</name>")
                    .append("<value><string></string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>start_time</name>")
                    .append("<value><string>1408105444.17</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>on_poweroff</name>")
                    .append("<value><string>destroy</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>on_xend_start</name>")
                    .append("<value><string>ignore</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>on_crash</name>")
                    .append("<value><string>restart</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>device</name>")
                    .append("<value><struct>")
                    .append("<member>")
                    .append("<name>vif</name>")
                    .append("<value><array><data>")
                    .append("<value><struct>")
                    .append("<member>")
                    .append("<name>bridge</name>")
                    .append("<value><string>").append(VMNICBR).append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>mac</name>")
                    .append("<value><string>").append(VMNICMAC).append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>script</name>")
                    .append("<value><string>/etc/xen/scripts/vif-bridge</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>uuid</name>")
                    .append("<value><string>").append(VMNICUUID).append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>backend</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("</struct></value>")
                    .append("</data></array></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>vkbd</name>")
                    .append("<value><array><data>")
                    .append("<value><struct>")
                    .append("<member>")
                    .append("<name>backend</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("</struct></value>")
                    .append("</data></array></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>console</name>")
                    .append("<value><array><data>")
                    .append("<value><struct>")
                    .append("<member>")
                    .append("<name>protocol</name>")
                    .append("<value><string>vt100</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>location</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>uuid</name>")
                    .append("<value><string>9beb5016-dde7-8526-491f-e972f262a634</string></value>")
                    .append("</member>")
                    .append("</struct></value>")
                    .append("</data></array></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>vfb</name>")
                    .append("<value><array><data>")
                    .append("<value><struct>")
                    .append("<member>")
                    .append("<name>vncunused</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>vnc</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>xauthority</name>")
                    .append("<value><string>/root/.Xauthority</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>vnclisten</name>")
                    .append("<value><string>0.0.0.0</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>vncpasswd</name>")
                    .append("<value><string>7693f834ca67912e</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>keymap</name>")
                    .append("<value><string>en-us</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>location</name>")
                    .append("<value><string>0.0.0.0:5900</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>uuid</name>")
                    .append("<value><string>78edf954-e375-b142-9c76-791ce805b6ef</string></value>")
                    .append("</member>")
                    .append("</struct></value>")
                    .append("</data></array></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>vbd</name>")
                    .append("<value><array><data>")
                    .append("<value><struct>")
                    .append("<member>")
                    .append("<name>protocol</name>")
                    .append("<value><string>x86_32-abi</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>uuid</name>")
                    .append("<value><string>bda35954-2596-025e-168c-b82e1cf92369</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>bootable</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>dev</name>")
                    .append("<value><string>xvda:disk</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>uname</name>")
                    .append("<value><string>file:/OVS/Repositories/")
                    .append(REPOID)
                    .append("/VirtualDisks/")
                    .append(VMROOTDISK)
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>mode</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>VDI</name>")
                    .append("<value><string></string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>backend</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("</struct></value>")
                    .append("</data></array></value>")
                    .append("</member>")
                    .append("</struct></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>bootloader</name>")
                    .append("<value><string>/usr/bin/pygrub</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>maxmem</name>")
                    .append("<value><string>512</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>cpu_time</name>")
                    .append("<value><string>5.627111952</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>shadow_memory</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>name</name>")
                    .append("<value><string>")
                    .append(VMNAME)
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>builder</name>")
                    .append("<value><string>linux</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>bootloader_args</name>")
                    .append("<value><string>-q</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>domid</name>")
                    .append("<value><string>")
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>vcpus</name>")
                    .append("<value><string>")
                    .append("</string></value>") .append("</member>") .append("</struct>").toString());

    public String getSingleVmConfigXML() {
        return this.SINGLEVMCONFIGXML;
    }
    String SINGLEVMCONFIGXML = results
            .simpleResponseWrapWrapper(new StringBuilder("<struct>")
                    .append("<member>")
                    .append("<name>vif</name>")
                    .append("<value><array><data>")
                    .append("<value><string>mac=").append(VMNICMAC).append(",bridge=").append(VMNICBR).append("</string></value>")
                    .append("<value><string>mac=02:00:50:9a:00:02,bridge=xenbr0.240</string></value>")
                    .append("</data></array></value>") .append("</member>") .append("<member>")
                    .append("<name>extra</name>") .append("<value><string></string></value>")
                    .append("</member>") .append("<member>") .append("<name>OVM_simple_name</name>")
                    .append("<value><string>")
                    .append(VMNAME)
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>disk</name>")
                    .append("<value><array><data>")
                    .append("<value><string>file:/OVS/Repositories/")
                    .append(REPOID)
                    .append("/VirtualDisks/")
                    .append(VMROOTDISK)
                    .append(",xvda,w</string></value>")
                    .append("</data></array></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>bootargs</name>")
                    .append("<value><string></string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>uuid</name>")
                    .append("<value><string>")
                    .append(VMID)
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>on_reboot</name>")
                    .append("<value><string>restart</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>cpu_weight</name>")
                    .append("<value><int>27500</int></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>memory</name>")
                    .append("<value><int>512</int></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>cpu_cap</name>")
                    .append("<value><int>0</int></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>maxvcpus</name>")
                    .append("<value><int>1</int></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>OVM_high_availability</name>")
                    .append("<value><boolean>0</boolean></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>OVM_description</name>")
                    .append("<value><string></string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>on_poweroff</name>")
                    .append("<value><string>destroy</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>on_crash</name>")
                    .append("<value><string>restart</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>bootloader</name>")
                    .append("<value><string>/usr/bin/pygrub</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>guest_os_type</name>")
                    .append("<value><string>Other.Linux</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>name</name>")
                    .append("<value><string>")
                    .append(VMNAME)
                    .append("</string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>vfb</name>")
                    .append("<value><array><data>")
                    .append("<value><string>vncunused=1,vncpasswd=7693f834ca67912e,keymap=en-us,type=vnc,vnclisten=0.0.0.0</string></value>")
                    .append("</data></array></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>vcpus</name>")
                    .append("<value><int>1</int></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>OVM_cpu_compat_group</name>")
                    .append("<value><string></string></value>")
                    .append("</member>")
                    .append("<member>")
                    .append("<name>OVM_domain_type</name>")
                    .append("<value><string>xen_pvm</string></value>")
                    .append("</member>")
                    .append("</struct>").toString());

    /* fix */
    @Test
    public void testListVm() throws Ovm3ResourceException {
        con.setResult(this.SINGLEVMLISTXML);
        results.basicBooleanTest(xEn.listVm(REPOID, VMID));
        con.setResult(results.getNil());
        results.basicBooleanTest(xEn.listVm(REPOID, VMID), false);
    }

    @Test
    public void testGetRunningVmConfig() throws Ovm3ResourceException {
        con.setResult(this.MULTIPLEVMSLISTXML);
        Xen.Vm domU = xEn.getRunningVmConfig(VMNAME);

        /* only works from a live configuration */
        results.basicStringTest(domU.getVmRootDiskPoolId(), REPOID);
        results.basicIntTest(domU.getVncPort(), 5900);
        results.basicStringTest(domU.getVncAddress(), "0.0.0.0");
    }

    @Test
    public void testGetVmConfig() throws Ovm3ResourceException {
        con.setResult(this.SINGLEVMCONFIGXML);
        Xen.Vm domU = xEn.getVmConfig(REPOID, VMID);
        /* getVncPort doesn't work with live config due to a bug in the agent */
        // results.basicIntTest(domU.getVncPort(), 5900);
        results.basicStringTest(domU.getVmName(), VMNAME);
        results.basicIntTest(domU.getVifIdByMac(VMNICMAC), 0);
        results.basicIntTest(domU.getVifIdByMac("02:00:50:9a:00:02"), 1);
        results.basicIntTest(domU.getVifIdByMac("02:00:50:9a:00:03"), -1);
        con.setResult(results.getNil());
        xEn.getVmConfig(REPOID, VMID);

        con.setResult(results.getNil());
    }

    @Test
    public void testRebootVM() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(xEn.rebootVm(REPOID, VMID));
        results.basicBooleanTest(xEn.rebootVm(REPOID, VMID, 1));
    }

    @Test
    public void testControlDomain() throws Ovm3ResourceException {
        con.setResult(this.MULTIPLEVMSLISTXML);
        Xen.Vm dom0 = xEn.getRunningVmConfig(DOM0VMNAME);
        results.basicBooleanTest(dom0.isControlDomain(), true);
        Xen.Vm domU = xEn.getRunningVmConfig(VMNAME);
        results.basicBooleanTest(domU.isControlDomain(), false);
        con.setResult(results.getNil());
    }

    @Test
    public void testCreateVm() throws Ovm3ResourceException {
        Xen.Vm domU = xEn.getVmConfig();
        domU.setVmCpus(1);
        domU.setVmMemory(512);
        domU.setVmDomainType("default");
        domU.setVmUuid(UUID.nameUUIDFromBytes(VMNAME.getBytes()).toString());
        domU.setVmName(VMNAME);
        domU.addRootDisk(VMROOTDISK);
        domU.setPrimaryPoolUuid(REPOID);

        domU.addVif(0, VMNICBR, VMNICMAC);
        domU.addVif(0, "xenbr0.240", "02:00:50:9a:00:02");
        domU.removeVif("xenbr0.240", "02:00:50:9a:00:02");
        domU.setVnc("0.0.0.0", "gobbeldygoo");
        con.setResult(results.getNil());
        xEn.createVm(REPOID, VMID);
        xEn.configureVmHa(REPOID, VMID, true);
        xEn.startVm(REPOID, VMID);

        domU.addIso(VMISO);
        domU.addIso(VMISO);
        xEn.configureVm(REPOID, domU.getVmUuid());
        domU.removeDisk(VMISO);
        domU.removeDisk("bogus");
        domU.getVmVifs();
        xEn.configureVm(REPOID, domU.getVmUuid());
        xEn.stopVm(REPOID, VMID, true);

        Xen.Vm domU2 = xEn.getVmConfig();
        domU2.setVmDomainType("hvm");
        domU2.addRootDisk(VMROOTDISK);
        domU2.addDataDisk(VMROOTDISK);
        domU2.getPrimaryPoolUuid();
    }

    @Test
    public void testRemoveMissingVif() throws Ovm3ResourceException {
        Xen.Vm domU = xEn.getVmConfig();
        domU.removeVif("xenbr0.240", "02:00:50:9a:00:02");
    }

    @Test
    public void testVmDomainType() throws Ovm3ResourceException {
        Xen.Vm domU = xEn.getVmConfig();
        domU.getVmDomainType();
    }

    @Test(expected = Ovm3ResourceException.class)
    public void testMissingVncPort() throws Ovm3ResourceException {
        Xen.Vm domU = xEn.getVmConfig();
        domU.getVncPort();
    }

    @Test
    public void testVmCpusExceedsMaxVCPUs() throws Ovm3ResourceException {
        Xen.Vm domU = xEn.getVmConfig();
        domU.setVmMaxCpus(2);
        results.basicIntTest(domU.getVmMaxCpus(), 2);
        domU.setVmCpus(4);
        results.basicIntTest(domU.getVmCpus(), 2);
        domU.setVmMaxCpus(12);
        results.basicIntTest(domU.getVmCpus(), 2);
        domU.setVmCpus(12);
        results.basicIntTest(domU.getVmCpus(), 12);
        domU.setVmMaxCpus(0);
        results.basicIntTest(domU.getVmCpus(), 12);
    }

    @Test
    public void testStopVm() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(xEn.stopVm(REPOID, VMID));
        results.basicBooleanTest(xEn.stopVm(REPOID, VMID, true));
    }

    @Test
    public void testPauseVm() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(xEn.pauseVm(REPOID, VMID));
    }

    public String getMultipleVmsListXML() {
        return MULTIPLEVMSLISTXML;
    }
    public String getVmName() {
        return VMNAME;
    }
}
