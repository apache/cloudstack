import py
from xdist.looponfail import RemoteControl
from xdist.looponfail import StatRecorder

class TestStatRecorder:
    def test_filechange(self, tmpdir):
        tmp = tmpdir
        hello = tmp.ensure("hello.py")
        sd = StatRecorder([tmp])
        changed = sd.check()
        assert not changed

        hello.write("world")
        changed = sd.check()
        assert changed

        (hello + "c").write("hello")
        changed = sd.check()
        assert not changed

        p = tmp.ensure("new.py")
        changed = sd.check()
        assert changed

        p.remove()
        changed = sd.check()
        assert changed

        tmp.join("a", "b", "c.py").ensure()
        changed = sd.check()
        assert changed

        tmp.join("a", "c.txt").ensure()
        changed = sd.check()
        assert changed
        changed = sd.check()
        assert not changed

        tmp.join("a").remove()
        changed = sd.check()
        assert changed

    def test_dirchange(self, tmpdir):
        tmp = tmpdir
        tmp.ensure("dir", "hello.py")
        sd = StatRecorder([tmp])
        assert not sd.fil(tmp.join("dir"))

    def test_filechange_deletion_race(self, tmpdir, monkeypatch):
        tmp = tmpdir
        sd = StatRecorder([tmp])
        changed = sd.check()
        assert not changed

        p = tmp.ensure("new.py")
        changed = sd.check()
        assert changed

        p.remove()
        # make check()'s visit() call return our just removed
        # path as if we were in a race condition
        monkeypatch.setattr(tmp, 'visit', lambda *args: [p])

        changed = sd.check()
        assert changed

    def test_pycremoval(self, tmpdir):
        tmp = tmpdir
        hello = tmp.ensure("hello.py")
        sd = StatRecorder([tmp])
        changed = sd.check()
        assert not changed

        pycfile = hello + "c"
        pycfile.ensure()
        hello.write("world")
        changed = sd.check()
        assert changed
        assert not pycfile.check()

    def test_waitonchange(self, tmpdir, monkeypatch):
        tmp = tmpdir
        sd = StatRecorder([tmp])

        l = [True, False]
        monkeypatch.setattr(StatRecorder, 'check', lambda self: l.pop())
        sd.waitonchange(checkinterval=0.2)
        assert not l

class TestRemoteControl:
    def test_nofailures(self, testdir):
        item = testdir.getitem("def test_func(): pass\n")
        control = RemoteControl(item.config)
        control.setup()
        topdir, failures = control.runsession()[:2]
        assert not failures

    def test_failures_somewhere(self, testdir):
        item = testdir.getitem("def test_func():\n assert 0\n")
        control = RemoteControl(item.config)
        control.setup()
        failures = control.runsession()
        assert failures
        control.setup()
        item.fspath.write("def test_func():\n assert 1\n")
        removepyc(item.fspath)
        topdir, failures = control.runsession()[:2]
        assert not failures

    def test_failure_change(self, testdir):
        modcol = testdir.getitem("""
            def test_func():
                assert 0
        """)
        control = RemoteControl(modcol.config)
        control.loop_once()
        assert control.failures
        modcol.fspath.write(py.code.Source("""
            def test_func():
                assert 1
            def test_new():
                assert 0
        """))
        removepyc(modcol.fspath)
        control.loop_once()
        assert not control.failures
        control.loop_once()
        assert control.failures
        assert str(control.failures).find("test_new") != -1

    def test_failure_subdir_no_init(self, testdir):
        modcol = testdir.getitem("""
            def test_func():
                assert 0
        """)
        parent = modcol.fspath.dirpath().dirpath()
        parent.chdir()
        modcol.config.args = [py.path.local(x).relto(parent)
                                for x in modcol.config.args]
        control = RemoteControl(modcol.config)
        control.loop_once()
        assert control.failures
        control.loop_once()
        assert control.failures

class TestLooponFailing:
    def test_looponfail_from_fail_to_ok(self, testdir):
        modcol = testdir.getmodulecol("""
            def test_one():
                x = 0
                assert x == 1
            def test_two():
                assert 1
        """)
        remotecontrol = RemoteControl(modcol.config)
        remotecontrol.loop_once()
        assert len(remotecontrol.failures) == 1

        modcol.fspath.write(py.code.Source("""
            def test_one():
                assert 1
            def test_two():
                assert 1
        """))
        removepyc(modcol.fspath)
        remotecontrol.loop_once()
        assert not remotecontrol.failures

    def test_looponfail_from_one_to_two_tests(self, testdir):
        modcol = testdir.getmodulecol("""
            def test_one():
                assert 0
        """)
        remotecontrol = RemoteControl(modcol.config)
        remotecontrol.loop_once()
        assert len(remotecontrol.failures) == 1
        assert 'test_one' in remotecontrol.failures[0]

        modcol.fspath.write(py.code.Source("""
            def test_one():
                assert 1 # passes now
            def test_two():
                assert 0 # new and fails
        """))
        removepyc(modcol.fspath)
        remotecontrol.loop_once()
        assert len(remotecontrol.failures) == 0
        remotecontrol.loop_once()
        assert len(remotecontrol.failures) == 1
        assert 'test_one' not in remotecontrol.failures[0]
        assert 'test_two' in remotecontrol.failures[0]

    def test_looponfail_removed_test(self, testdir):
        modcol = testdir.getmodulecol("""
            def test_one():
                assert 0
            def test_two():
                assert 0
        """)
        remotecontrol = RemoteControl(modcol.config)
        remotecontrol.loop_once()
        assert len(remotecontrol.failures) == 2

        modcol.fspath.write(py.code.Source("""
            def test_xxx(): # renamed test
                assert 0
            def test_two():
                assert 1 # pass now
        """))
        removepyc(modcol.fspath)
        remotecontrol.loop_once()
        assert len(remotecontrol.failures) == 0

        remotecontrol.loop_once()
        assert len(remotecontrol.failures) == 1

    def test_looponfail_multiple_errors(self, testdir, monkeypatch):
        modcol = testdir.getmodulecol("""
            def test_one():
                assert 0
        """)
        remotecontrol = RemoteControl(modcol.config)
        orig_runsession = remotecontrol.runsession

        def runsession_dups():
            # twisted.trial test cases may report multiple errors.
            failures, reports, collection_failed = orig_runsession()
            print (failures)
            return failures * 2, reports, collection_failed

        monkeypatch.setattr(remotecontrol, 'runsession', runsession_dups)
        remotecontrol.loop_once()
        assert len(remotecontrol.failures) == 1


class TestFunctional:
    def test_fail_to_ok(self, testdir):
        p = testdir.makepyfile("""
            def test_one():
                x = 0
                assert x == 1
        """)
        #p = testdir.mkdir("sub").join(p1.basename)
        #p1.move(p)
        child = testdir.spawn_pytest("-f %s --traceconfig" % p)
        child.expect("def test_one")
        child.expect("x == 1")
        child.expect("1 failed")
        child.expect("### LOOPONFAILING ####")
        child.expect("waiting for changes")
        p.write(py.code.Source("""
            def test_one():
                x = 1
                assert x == 1
        """))
        child.expect(".*1 passed.*")
        child.kill(15)

    def test_xfail_passes(self, testdir):
        p = testdir.makepyfile("""
            import py
            @py.test.mark.xfail
            def test_one():
                pass
        """)
        child = testdir.spawn_pytest("-f %s" % p)
        child.expect("1 xpass")
        child.expect("### LOOPONFAILING ####")
        child.expect("waiting for changes")
        child.kill(15)

def removepyc(path):
    # XXX damn those pyc files
    pyc = path + "c"
    if pyc.check():
        pyc.remove()
    c = path.dirpath("__pycache__")
    if c.check():
        c.remove()

