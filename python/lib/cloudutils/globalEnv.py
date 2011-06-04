class globalEnv:
    def __init__(self):
        #Agent/Server/Db
        self.mode = None
        #server mode: normal/mycloud
        self.svrMode = None
        #myCloud/Agent/Console
        self.agentMode = None
        #debug
        self.debug = False
        #management server IP
        self.mgtSvr = "myagent.cloud.com"
        #zone id or zone name
        self.zone = None
        #pod id or pod name
        self.pod = None
        #cluster id or cluster name
        self.cluster = None
        #nics: 0: private nic, 1: guest nic, 2: public nic used by agent
        self.nics = []
        #uuid
        self.uuid = None
        #default private network
        self.privateNet = "cloudbr0"
