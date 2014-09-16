import py
import pytest

def pytest_addoption(parser):
    group = parser.getgroup("xdist", "distributed and subprocess testing")
    group._addoption('-f', '--looponfail',
           action="store_true", dest="looponfail", default=False,
           help="run tests in subprocess, wait for modified files "
                "and re-run failing test set until all pass.")
    group._addoption('-n', dest="numprocesses", metavar="numprocesses",
           action="store", type="int",
           help="shortcut for '--dist=load --tx=NUM*popen'")
    group.addoption('--boxed',
           action="store_true", dest="boxed", default=False,
           help="box each test run in a separate process (unix)")
    group._addoption('--dist', metavar="distmode",
           action="store", choices=['load', 'each', 'no'],
           type="choice", dest="dist", default="no",
           help=("set mode for distributing tests to exec environments.\n\n"
                 "each: send each test to each available environment.\n\n"
                 "load: send each test to available environment.\n\n"
                 "(default) no: run tests inprocess, don't distribute."))
    group._addoption('--tx', dest="tx", action="append", default=[],
           metavar="xspec",
           help=("add a test execution environment. some examples: "
                 "--tx popen//python=python2.5 --tx socket=192.168.1.102:8888 "
                 "--tx ssh=user@codespeak.net//chdir=testcache"))
    group._addoption('-d',
           action="store_true", dest="distload", default=False,
           help="load-balance tests.  shortcut for '--dist=load'")
    group.addoption('--rsyncdir', action="append", default=[], metavar="DIR",
           help="add directory for rsyncing to remote tx nodes.")
    group.addoption('--rsyncignore', action="append", default=[], metavar="GLOB",
           help="add expression for ignores when rsyncing to remote tx nodes.")

    parser.addini('rsyncdirs', 'list of (relative) paths to be rsynced for'
         ' remote distributed testing.', type="pathlist")
    parser.addini('rsyncignore', 'list of (relative) glob-style paths to be ignored '
         'for rsyncing.', type="pathlist")
    parser.addini("looponfailroots", type="pathlist",
        help="directories to check for changes", default=[py.path.local()])

# -------------------------------------------------------------------------
# distributed testing hooks
# -------------------------------------------------------------------------
def pytest_addhooks(pluginmanager):
    from xdist import newhooks
    pluginmanager.addhooks(newhooks)

# -------------------------------------------------------------------------
# distributed testing initialization
# -------------------------------------------------------------------------

def pytest_cmdline_main(config):
    check_options(config)
    if config.getoption("looponfail"):
        from xdist.looponfail import looponfail_main
        looponfail_main(config)
        return 2 # looponfail only can get stop with ctrl-C anyway

def pytest_configure(config, __multicall__):
    __multicall__.execute()
    if config.getoption("dist") != "no":
        from xdist.dsession import DSession
        session = DSession(config)
        config.pluginmanager.register(session, "dsession")
        tr = config.pluginmanager.getplugin("terminalreporter")
        tr.showfspath = False

def check_options(config):
    if config.option.numprocesses:
        config.option.dist = "load"
        config.option.tx = ['popen'] * int(config.option.numprocesses)
    if config.option.distload:
        config.option.dist = "load"
    val = config.getvalue
    if not val("collectonly"):
        usepdb = config.option.usepdb  # a core option
        if val("looponfail"):
            if usepdb:
                raise pytest.UsageError("--pdb incompatible with --looponfail.")
        elif val("dist") != "no":
            if usepdb:
                raise pytest.UsageError("--pdb incompatible with distributing tests.")


def pytest_runtest_protocol(item):
    if item.config.getvalue("boxed"):
        reports = forked_run_report(item)
        for rep in reports:
            item.ihook.pytest_runtest_logreport(report=rep)
        return True

def forked_run_report(item):
    # for now, we run setup/teardown in the subprocess
    # XXX optionally allow sharing of setup/teardown
    from _pytest.runner import runtestprotocol
    EXITSTATUS_TESTEXIT = 4
    import marshal
    from xdist.remote import serialize_report
    from xdist.slavemanage import unserialize_report
    def runforked():
        try:
            reports = runtestprotocol(item, log=False)
        except KeyboardInterrupt:
            py.std.os._exit(EXITSTATUS_TESTEXIT)
        return marshal.dumps([serialize_report(x) for x in reports])

    ff = py.process.ForkedFunc(runforked)
    result = ff.waitfinish()
    if result.retval is not None:
        report_dumps = marshal.loads(result.retval)
        return [unserialize_report("testreport", x) for x in report_dumps]
    else:
        if result.exitstatus == EXITSTATUS_TESTEXIT:
            py.test.exit("forked test item %s raised Exit" %(item,))
        return [report_process_crash(item, result)]

def report_process_crash(item, result):
    path, lineno = item._getfslineno()
    info = ("%s:%s: running the test CRASHED with signal %d" %
            (path, lineno, result.signal))
    from _pytest import runner
    call = runner.CallInfo(lambda: 0/0, "???")
    call.excinfo = info
    rep = runner.pytest_runtest_makereport(item, call)
    if result.out:
        rep.sections.append(("captured stdout", result.out))
    if result.err:
        rep.sections.append(("captured stderr", result.err))
    return rep
