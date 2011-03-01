package com.cloud.baremetal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupPxeServerCommand;
import com.cloud.agent.api.baremetal.PrepareLinMinPxeServerAnswer;
import com.cloud.agent.api.baremetal.PrepareLinMinPxeServerCommand;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.cloud.vm.VirtualMachine.State;

public class LinMinPxeServerResource implements ServerResource {
	private static final Logger s_logger = Logger.getLogger(LinMinPxeServerResource.class);
	String _name;
	String _guid;
	String _username;
	String _password;
	String _ip;
	String _zoneId;
	String _podId;
	String _apiUsername;
	String _apiPassword;
	String _apid;
	
	class XmlReturn {
		NodeList nList;
		public XmlReturn(InputSource s, String tagName) {
			try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(s);
				doc.getDocumentElement().normalize();
				nList = doc.getElementsByTagName(tagName);
			} catch (Exception e) {
				s_logger.debug("The XML file:");
				s_logger.debug(s.toString());
				s_logger.debug("Cannot parse XMl file", e);
				nList = null;
			}
		}
		
		public String getValue(String tag) {
			if (nList == null || nList.getLength() == 0) {
				throw new InvalidParameterValueException("invalid XML file");
			}	
			
			Element e = (Element)nList.item(0);
			NodeList nlList= e.getElementsByTagName(tag).item(0).getChildNodes();
		    Node nValue = (Node)nlList.item(0); 
		    return nValue.getNodeValue();
		}
	}
	
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		_name = name;
		_guid = (String)params.get("guid");
		_ip = (String)params.get("ip");
		_username = (String)params.get("username");
		_password = (String)params.get("password");
		_zoneId = (String)params.get("zone");
		_podId = (String)params.get("pod");
		_apiUsername = (String)params.get("apiUsername");
		_apiPassword = (String)params.get("apiPassword");
		_apid = (String)params.get("apid");
		
		if (_guid == null) {
			throw new ConfigurationException("No Guid specified");
		}
		
		if (_zoneId == null) {
			throw new ConfigurationException("No Zone specified");
		}
		
		if (_podId == null) {
			throw new ConfigurationException("No Pod specified");
		}
		
		if (_ip == null) {
			throw new ConfigurationException("No IP specified");
		}
		
		if (_username == null) {
			throw new ConfigurationException("No username specified");
		}
		
		if (_password == null) {
			throw new ConfigurationException("No password specified");
		}
		
		if (_apiUsername == null) {
			throw new ConfigurationException("No API username specified");
		}
		
		if (_apiPassword == null) {
			throw new ConfigurationException("No API password specified");
		}
		
		if (_apid == null) {
			throw new ConfigurationException("No A specified");
		}
		
		com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(_ip, 22);
		
		s_logger.debug(String.format("Trying to connect to LinMin PXE server(IP=%1$s, username=%2$s, password=%3$s", _ip, _username, _password));
		try {
			sshConnection.connect(null, 60000, 60000);
			if (!sshConnection.authenticateWithPassword(_username, _password)) {
				s_logger.debug("SSH Failed to authenticate");
				throw new ConfigurationException(String.format("Cannot connect to LinMin PXE server(IP=%1$s, username=%2$s, password=%3$s", _ip, _username,
						_password));
			}
			
			if (!SSHCmdHelper.sshExecuteCmd(sshConnection, "[ -d '/home/tftpboot' ] && [ -d '/usr/local/linmin' ]")) {
				throw new ConfigurationException("Cannot find LinMin directory /home/tftpboot, /usr/local/linmin on PXE server");
			}
			
			return true;
		} catch (Exception e) {
			throw new ConfigurationException(e.getMessage());
		} finally {
			if (sshConnection != null) {
				sshConnection.close();
			}
		}
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public Type getType() {
		return Type.PxeServer;
	}

	@Override
	public StartupCommand[] initialize() {
		StartupPxeServerCommand cmd = new StartupPxeServerCommand();
		cmd.setName(_name);
		cmd.setDataCenter(_zoneId);
		cmd.setPod(_podId);
		cmd.setPrivateIpAddress(_ip);
		cmd.setStorageIpAddress("");
		cmd.setVersion("");
		cmd.setGuid(_guid);
		return new StartupCommand[]{cmd};
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
		//TODO: check server
		return new PingRoutingCommand(getType(), id, new HashMap<String, State>());
	}
	
	protected ReadyAnswer execute(ReadyCommand cmd) {
		s_logger.debug("LinMin resource " + _name + " is ready");
		return new ReadyAnswer(cmd);
	}
	
	private InputSource httpCall(String urlStr) {
		try {
			s_logger.debug("Execute http call " + urlStr);
			URL url = new URL(urlStr);
			URLConnection conn = url.openConnection();
			conn.setReadTimeout(30000);
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer xmlStuff = new StringBuffer();
			String line;
			while ((line = in.readLine()) != null) {
				xmlStuff.append(line);
			}
			StringReader statsReader = new StringReader(xmlStuff.toString());
	        InputSource statsSource = new InputSource(statsReader);
	        s_logger.debug("Http call retrun:");
	        s_logger.debug(xmlStuff.toString());
	        return statsSource;
		} catch (MalformedURLException e) {
			throw new CloudRuntimeException("URL is malformed " + urlStr, e);
		} catch (IOException e) {
			s_logger.warn("can not do http call", e);
			return null;
		}catch (Exception e) {
			s_logger.warn("Cannot do http call " + urlStr, e);
			throw new CloudRuntimeException(e.getStackTrace().toString());
		}
	}
	
	
	protected PrepareLinMinPxeServerAnswer execute(PrepareLinMinPxeServerCommand cmd) {
		StringBuffer askApid = new StringBuffer();
		
		askApid.append("http://");
		askApid.append(_ip);
		askApid.append("/tftpboot/www/lbmp-API.php?actiontype=provision&apid=");
		askApid.append(_apid);
		askApid.append("&auth_user=");
		askApid.append(_apiUsername);
		askApid.append("&auth_user_pw=");
		askApid.append(_apiPassword);
		askApid.append("&rtn_format=XML&action=authorize");
		InputSource s = httpCall(askApid.toString());
		if (s == null) {
			return new PrepareLinMinPxeServerAnswer(cmd, "Http call failed");
		}
		
		try {
			XmlReturn r = new XmlReturn(s, "LinMinBareMetalAPI");
			String res = r.getValue("actionResultsMsg");
			s_logger.debug(s.toString());
			if (!res.startsWith("Successful")) {
				return new PrepareLinMinPxeServerAnswer(cmd, "Acquire APID failed");
			}
			
			String apid5 = r.getValue("apid");
			if (apid5 == null) {
				return new PrepareLinMinPxeServerAnswer(cmd, "Cannot get 5 minutes APID " + apid5);
			}
			
			StringBuffer addRole = new StringBuffer();
			addRole.append("http://");
			addRole.append(_ip);
			addRole.append("/tftpboot/www/lbmp-API.php?actiontype=provision&user_supplied_id=");
			addRole.append(cmd.getVmName());
			addRole.append("&mac_address=");
			addRole.append(cmd.getMac().replaceAll(":", "%3A"));
			addRole.append("&apid=");
			addRole.append(apid5);
			addRole.append("&control_file_template=");
			addRole.append(cmd.getTemplate().replace(' ', '+'));
			addRole.append("&node_name=");
			addRole.append(cmd.getHostName());
			addRole.append("&node_domain=");
			addRole.append(cmd.getHostName());
			addRole.append("&node_password=password");
			addRole.append("&node_time_zone=Etc%2FGMT-8");
			if (cmd.getIp() != null) {
				addRole.append("&node_ip_address=");
				addRole.append(cmd.getIp());
			}
			if (cmd.getNetMask() != null) {
				addRole.append("&node_subnet_mask=");
				addRole.append(cmd.getNetMask());
			}
			if (cmd.getDns() != null) {
				addRole.append("&node_nameserver=");
				addRole.append(cmd.getDns());
			}
			if (cmd.getGateWay() != null) {
				addRole.append("&node_default_gateway=");
				addRole.append(cmd.getGateWay());
			}
			addRole.append("&enable_provisioning_flag=nextbootonly&rtn_format=XML&action=add");
			
			s = httpCall(addRole.toString());
			if (s == null) {
				return new PrepareLinMinPxeServerAnswer(cmd, "Http call failed");
			}
			r = new XmlReturn(s, "LinMinBareMetalAPI");
			res = r.getValue("actionResultsMsg");
			s_logger.debug(s.toString());
			if (!res.startsWith("Successful")) {
				return new PrepareLinMinPxeServerAnswer(cmd, "Add LinMin role failed");
			}
		} catch (Exception e) {
			s_logger.warn("Cannot parse result from Lin Min server", e);
			return new PrepareLinMinPxeServerAnswer(cmd, e.getMessage());
		}
		
		s_logger.debug("Prepare LinMin PXE server successfully");
		return new PrepareLinMinPxeServerAnswer(cmd);
	}
	
	@Override
	public Answer executeRequest(Command cmd) {
		if (cmd instanceof ReadyCommand) {
			return execute((ReadyCommand) cmd);
		} else if (cmd instanceof PrepareLinMinPxeServerCommand) { 
			return execute((PrepareLinMinPxeServerCommand)cmd);
		} else {
			return Answer.createUnsupportedCommandAnswer(cmd);
		}
	}

	@Override
	public void disconnected() {
		// TODO Auto-generated method stub

	}

	@Override
	public IAgentControl getAgentControl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAgentControl(IAgentControl agentControl) {
		// TODO Auto-generated method stub

	}

}
