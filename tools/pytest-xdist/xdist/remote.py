"""
    This module is executed in remote subprocesses and helps to
    control a remote testing session and relay back information.
    It assumes that 'py' is importable and does not have dependencies
    on the rest of the xdist code.  This means that the xdist-plugin
    needs not to be installed in remote environments.
"""

import sys, os

class SlaveInteractor:
    def __init__(self, config, channel):
        self.config = config
        self.slaveid = config.slaveinput.get('slaveid', "?")
        self.log = py.log.Producer("slave-%s" % self.slaveid)
        if not config.option.debug:
            py.log.setconsumer(self.log._keywords, None)
        self.channel = channel
        config.pluginmanager.register(self)

    def sendevent(self, name, **kwargs):
        self.log("sending", name, kwargs)
        self.channel.send((name, kwargs))

    def pytest_internalerror(self, excrepr):
        for line in str(excrepr).split("\n"):
            self.log("IERROR>", line)

    def pytest_sessionstart(self, session):
        self.session = session
        slaveinfo = getinfodict()
        self.sendevent("slaveready", slaveinfo=slaveinfo)

    def pytest_sessionfinish(self, __multicall__, exitstatus):
        self.config.slaveoutput['exitstatus'] = exitstatus
        res = __multicall__.execute()
        self.sendevent("slavefinished", slaveoutput=self.config.slaveoutput)
        return res

    def pytest_collection(self, session):
        self.sendevent("collectionstart")

    def pytest_runtestloop(self, session):
        self.log("entering main loop")
        torun = []
        while 1:
            name, kwargs = self.channel.receive()
            self.log("received command", name, kwargs)
            if name == "runtests":
                torun.extend(kwargs['indices'])
            elif name == "runtests_all":
                torun.extend(range(len(session.items)))
            self.log("items to run:", torun)
            # only run if we have an item and a next item
            while len(torun) >= 2:
                self.run_tests(torun)
            if name == "shutdown":
                if torun:
                    self.run_tests(torun)
                break
        return True

    def run_tests(self, torun):
        items = self.session.items
        self.item_index = torun.pop(0)
        if torun:
            nextitem = items[torun[0]]
        else:
            nextitem = None
        self.config.hook.pytest_runtest_protocol(
            item=items[self.item_index],
            nextitem=nextitem)

    def pytest_collection_finish(self, session):
        self.sendevent("collectionfinish",
            topdir=str(session.fspath),
            ids=[item.nodeid for item in session.items])

    def pytest_runtest_logstart(self, nodeid, location):
        self.sendevent("logstart", nodeid=nodeid, location=location)

    def pytest_runtest_logreport(self, report):
        data = serialize_report(report)
        data["item_index"] = self.item_index
        assert self.session.items[self.item_index].nodeid == report.nodeid
        self.sendevent("testreport", data=data)

    def pytest_collectreport(self, report):
        data = serialize_report(report)
        self.sendevent("collectreport", data=data)

def serialize_report(rep):
    import py
    d = rep.__dict__.copy()
    if hasattr(rep.longrepr, 'toterminal'):
        d['longrepr'] = str(rep.longrepr)
    else:
        d['longrepr'] = rep.longrepr
    for name in d:
        if isinstance(d[name], py.path.local):
            d[name] = str(d[name])
        elif name == "result":
            d[name] = None # for now
    return d

def getinfodict():
    import platform
    return dict(
        version = sys.version,
        version_info = tuple(sys.version_info),
        sysplatform = sys.platform,
        platform = platform.platform(),
        executable = sys.executable,
        cwd = os.getcwd(),
    )

def remote_initconfig(option_dict, args):
    from _pytest.config import Config
    option_dict['plugins'].append("no:terminal")
    config = Config.fromdictargs(option_dict, args)
    config.option.looponfail = False
    config.option.usepdb = False
    config.option.dist = "no"
    config.option.distload = False
    config.option.numprocesses = None
    config.args = args
    return config


if __name__ == '__channelexec__':
    channel = channel  # noqa
    # python3.2 is not concurrent import safe, so let's play it safe
    # https://bitbucket.org/hpk42/pytest/issue/347/pytest-xdist-and-python-32
    if sys.version_info[:2] == (3,2):
        os.environ["PYTHONDONTWRITEBYTECODE"] = "1"
    slaveinput,args,option_dict = channel.receive()
    importpath = os.getcwd()
    sys.path.insert(0, importpath) # XXX only for remote situations
    os.environ['PYTHONPATH'] = (importpath + os.pathsep +
        os.environ.get('PYTHONPATH', ''))
    #os.environ['PYTHONPATH'] = importpath
    import py
    config = remote_initconfig(option_dict, args)
    config.slaveinput = slaveinput
    config.slaveoutput = {}
    interactor = SlaveInteractor(config, channel)
    config.hook.pytest_cmdline_main(config=config)
