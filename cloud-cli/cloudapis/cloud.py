'''Implements the Cloud.com API'''


from cloudtool.utils import describe
import urllib
import urllib2
import os
import xml.dom.minidom
import re

class CloudAPI:
    
	@describe("server", "Management Server host name or address")
	@describe("responseformat", "Response format: xml or json")
	@describe("stripxml", "True if xml tags have to be stripped in the output, false otherwise")
	def __init__(self,
			server="127.0.0.1:8096",
			responseformat="xml",
			stripxml="true"
			):
		self.__dict__.update(locals())
        
	def _make_request(self,command,parameters=None):
		'''Command is a string, parameters is a dictionary'''
		if ":" in self.server:
			host,port = self.server.split(":")
			port = int(port)
		else:
			host = self.server
			port = 8096
		
		url = "http://" + self.server + "/?"
		
		if not parameters: parameters = {}
		parameters["command"] = command
		parameters["response"] = self.responseformat
		querystring = urllib.urlencode(parameters)
		url += querystring
		
		f = urllib2.urlopen(url)
		data = f.read()
		if self.stripxml == "true":
			data=re.sub("<\?.*\?>", "\n", data);
			data=re.sub("</[a-z]*>", "\n", data);
			data=data.replace(">", "=");
			data=data.replace("=<", "\n");
			data=data.replace("\n<", "\n");
			data=re.sub("\n.*cloud-stack-version=.*", "", data);
			data=data.replace("\n\n\n", "\n");
                else:
                        data="\n"+data+"\n"
                return data

		return data


def load_dynamic_methods():
	'''creates smart function objects for every method in the commands.xml file'''
	
	def getText(nodelist):
		rc = []
		for node in nodelist:
			if node.nodeType == node.TEXT_NODE: rc.append(node.data)
		return ''.join(rc)
	
	# FIXME figure out installation and packaging
	xmlfile = os.path.join("/etc/cloud/cli/","commands.xml")
	dom = xml.dom.minidom.parse(xmlfile)
	
	for cmd in dom.getElementsByTagName("command"):
		name = getText(cmd.getElementsByTagName('name')[0].childNodes).strip()
		assert name
		
		description = getText(cmd.getElementsByTagName('description')[0].childNodes).strip()
		if description: 
                    description = '"""%s"""' % description
		else: description = ''
		arguments = []
		options = []
		descriptions = []
	
		for param in cmd.getElementsByTagName("request")[0].getElementsByTagName("arg"):
			argname = getText(param.getElementsByTagName('name')[0].childNodes).strip()
			assert argname
			
			required = getText(param.getElementsByTagName('required')[0].childNodes).strip()
			if required == 'true': required = True
			elif required == 'false': required = False
			else: raise AssertionError, "Not reached"
			if required: arguments.append(argname)
			options.append(argname)
			
                        #import ipdb; ipdb.set_trace()
			requestDescription = param.getElementsByTagName('description')
			if requestDescription:			
			    descriptionParam = getText(requestDescription[0].childNodes)
                        else: 
                            descriptionParam = ''
			if descriptionParam: descriptions.append( (argname,descriptionParam) )
		
		funcparams = ["self"] + [ "%s=None"%o for o in options ]
		funcparams = ", ".join(funcparams)
		
		code = """
		def %s(%s):
			%s
			parms = locals()
			del parms["self"]
			for arg in %r:
				if locals()[arg] is None:
					raise TypeError, "%%s is a required option"%%arg 
			for k,v in parms.items():
				if v is None: del parms[k]
			output = self._make_request("%s",parms)
			return output
		"""%(name,funcparams,description,arguments,name)

		namespace = {}
		exec code.strip() in namespace
		
		func = namespace[name]
		for argname,description in descriptions:
			func = describe(argname,description)(func)
		
		yield (name,func)


for name,meth in load_dynamic_methods(): 
    setattr(CloudAPI, name, meth)

implementor = CloudAPI

del name,meth,describe,load_dynamic_methods


