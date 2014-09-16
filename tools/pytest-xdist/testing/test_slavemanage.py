import py
import execnet
from xdist.slavemanage import HostRSync, NodeManager

pytest_plugins = "pytester",

def pytest_funcarg__hookrecorder(request):
    _pytest = request.getfuncargvalue('_pytest')
    config = request.getfuncargvalue('config')
    return _pytest.gethookrecorder(config.hook)

def pytest_funcarg__config(request):
    testdir = request.getfuncargvalue("testdir")
    config = testdir.parseconfig()
    return config

def pytest_funcarg__mysetup(request):
    class mysetup:
        def __init__(self, request):
            temp = request.getfuncargvalue("tmpdir")
            self.source = temp.mkdir("source")
            self.dest = temp.mkdir("dest")
            request.getfuncargvalue("_pytest")
    return mysetup(request)

class TestNodeManagerPopen:
    def test_popen_no_default_chdir(self, config):
        gm = NodeManager(config, ["popen"])
        assert gm.specs[0].chdir is None

    def test_default_chdir(self, config):
        l = ["ssh=noco", "socket=xyz"]
        for spec in NodeManager(config, l).specs:
            assert spec.chdir == "pyexecnetcache"
        for spec in NodeManager(config, l, defaultchdir="abc").specs:
            assert spec.chdir == "abc"

    def test_popen_makegateway_events(self, config, hookrecorder, _pytest):
        hm = NodeManager(config, ["popen"] * 2)
        hm.makegateways()
        call = hookrecorder.popcall("pytest_xdist_setupnodes")
        assert len(call.specs) == 2

        call = hookrecorder.popcall("pytest_xdist_newgateway")
        assert call.gateway.spec == execnet.XSpec("popen")
        assert call.gateway.id == "gw0"
        call = hookrecorder.popcall("pytest_xdist_newgateway")
        assert call.gateway.id == "gw1"
        assert len(hm.group) == 2
        hm.teardown_nodes()
        assert not len(hm.group)

    def test_popens_rsync(self, config, mysetup):
        source = mysetup.source
        hm = NodeManager(config, ["popen"] * 2)
        hm.makegateways()
        assert len(hm.group) == 2
        for gw in hm.group:
            class pseudoexec:
                args = []
                def __init__(self, *args):
                    self.args.extend(args)
                def waitclose(self):
                    pass
            gw.remote_exec = pseudoexec
        l = []
        hm.rsync(source, notify=lambda *args: l.append(args))
        assert not l
        hm.teardown_nodes()
        assert not len(hm.group)
        assert "sys.path.insert" in gw.remote_exec.args[0]

    def test_rsync_popen_with_path(self, config, mysetup):
        source, dest = mysetup.source, mysetup.dest
        hm = NodeManager(config, ["popen//chdir=%s" %dest] * 1)
        hm.makegateways()
        source.ensure("dir1", "dir2", "hello")
        l = []
        hm.rsync(source, notify=lambda *args: l.append(args))
        assert len(l) == 1
        assert l[0] == ("rsyncrootready", hm.group['gw0'].spec, source)
        hm.teardown_nodes()
        dest = dest.join(source.basename)
        assert dest.join("dir1").check()
        assert dest.join("dir1", "dir2").check()
        assert dest.join("dir1", "dir2", 'hello').check()

    def test_rsync_same_popen_twice(self, config, mysetup, hookrecorder):
        source, dest = mysetup.source, mysetup.dest
        hm = NodeManager(config, ["popen//chdir=%s" %dest] * 2)
        hm.makegateways()
        source.ensure("dir1", "dir2", "hello")
        hm.rsync(source)
        call = hookrecorder.popcall("pytest_xdist_rsyncstart")
        assert call.source == source
        assert len(call.gateways) == 1
        assert call.gateways[0] in hm.group
        call = hookrecorder.popcall("pytest_xdist_rsyncfinish")

class TestHRSync:
    def pytest_funcarg__mysetup(self, request):
        class mysetup:
            def __init__(self, request):
                tmp = request.getfuncargvalue('tmpdir')
                self.source = tmp.mkdir("source")
                self.dest = tmp.mkdir("dest")
        return mysetup(request)

    def test_hrsync_filter(self, mysetup):
        source, _ = mysetup.source, mysetup.dest  # noqa
        source.ensure("dir", "file.txt")
        source.ensure(".svn", "entries")
        source.ensure(".somedotfile", "moreentries")
        source.ensure("somedir", "editfile~")
        syncer = HostRSync(source, ignores=NodeManager.DEFAULT_IGNORES)
        l = list(source.visit(rec=syncer.filter,
                                   fil=syncer.filter))
        assert len(l) == 3
        basenames = [x.basename for x in l]
        assert 'dir' in basenames
        assert 'file.txt' in basenames
        assert 'somedir' in basenames

    def test_hrsync_one_host(self, mysetup):
        source, dest = mysetup.source, mysetup.dest
        gw = execnet.makegateway("popen//chdir=%s" % dest)
        finished = []
        rsync = HostRSync(source)
        rsync.add_target_host(gw, finished=lambda: finished.append(1))
        source.join("hello.py").write("world")
        rsync.send()
        gw.exit()
        assert dest.join(source.basename, "hello.py").check()
        assert len(finished) == 1


class TestNodeManager:
    @py.test.mark.xfail
    def test_rsync_roots_no_roots(self, testdir, mysetup):
        mysetup.source.ensure("dir1", "file1").write("hello")
        config = testdir.parseconfig(mysetup.source)
        nodemanager = NodeManager(config, ["popen//chdir=%s" % mysetup.dest])
        #assert nodemanager.config.topdir == source == config.topdir
        nodemanager.makegateways()
        nodemanager.rsync_roots()
        p, = nodemanager.gwmanager.multi_exec(
            "import os ; channel.send(os.getcwd())").receive_each()
        p = py.path.local(p)
        py.builtin.print_("remote curdir", p)
        assert p == mysetup.dest.join(config.topdir.basename)
        assert p.join("dir1").check()
        assert p.join("dir1", "file1").check()

    def test_popen_rsync_subdir(self, testdir, mysetup):
        source, dest = mysetup.source, mysetup.dest
        dir1 = mysetup.source.mkdir("dir1")
        dir2 = dir1.mkdir("dir2")
        dir2.ensure("hello")
        for rsyncroot in (dir1, source):
            dest.remove()
            nodemanager = NodeManager(testdir.parseconfig(
                "--tx", "popen//chdir=%s" % dest,
                "--rsyncdir", rsyncroot,
                source,
            ))
            nodemanager.makegateways()
            nodemanager.rsync_roots()
            if rsyncroot == source:
                dest = dest.join("source")
            assert dest.join("dir1").check()
            assert dest.join("dir1", "dir2").check()
            assert dest.join("dir1", "dir2", 'hello').check()
            nodemanager.teardown_nodes()

    def test_init_rsync_roots(self, testdir, mysetup):
        source, dest = mysetup.source, mysetup.dest
        dir2 = source.ensure("dir1", "dir2", dir=1)
        source.ensure("dir1", "somefile", dir=1)
        dir2.ensure("hello")
        source.ensure("bogusdir", "file")
        source.join("tox.ini").write(py.std.textwrap.dedent("""
            [pytest]
            rsyncdirs=dir1/dir2
        """))
        config = testdir.parseconfig(source)
        nodemanager = NodeManager(config, ["popen//chdir=%s" % dest])
        nodemanager.makegateways()
        nodemanager.rsync_roots()
        assert dest.join("dir2").check()
        assert not dest.join("dir1").check()
        assert not dest.join("bogus").check()

    def test_rsyncignore(self, testdir, mysetup):
        source, dest = mysetup.source, mysetup.dest
        dir2 = source.ensure("dir1", "dir2", dir=1)
        source.ensure("dir5", "dir6", "bogus")
        source.ensure("dir5", "file")
        dir2.ensure("hello")
        source.ensure("foo", "bar")
        source.ensure("bar", "foo")
        source.join("tox.ini").write(py.std.textwrap.dedent("""
            [pytest]
            rsyncdirs = dir1 dir5
            rsyncignore = dir1/dir2 dir5/dir6 foo*
        """))
        config = testdir.parseconfig(source)
        config.option.rsyncignore = ['bar']
        nodemanager = NodeManager(config, ["popen//chdir=%s" % dest])
        nodemanager.makegateways()
        nodemanager.rsync_roots()
        assert dest.join("dir1").check()
        assert not dest.join("dir1", "dir2").check()
        assert dest.join("dir5","file").check()
        assert not dest.join("dir6").check()
        assert not dest.join('foo').check()
        assert not dest.join('bar').check()

    def test_optimise_popen(self, testdir, mysetup):
        source = mysetup.source
        specs = ["popen"] * 3
        source.join("conftest.py").write("rsyncdirs = ['a']")
        source.ensure('a', dir=1)
        config = testdir.parseconfig(source)
        nodemanager = NodeManager(config, specs)
        nodemanager.makegateways()
        nodemanager.rsync_roots()
        for gwspec in nodemanager.specs:
            assert gwspec._samefilesystem()
            assert not gwspec.chdir

    def test_ssh_setup_nodes(self, specssh, testdir):
        testdir.makepyfile(__init__="", test_x="""
            def test_one():
                pass
        """)
        reprec = testdir.inline_run("-d", "--rsyncdir=%s" % testdir.tmpdir,
                "--tx", specssh, testdir.tmpdir)
        rep, = reprec.getreports("pytest_runtest_logreport")
        assert rep.passed


