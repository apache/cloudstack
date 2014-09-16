import pytest
import os

needsfork = pytest.mark.skipif(not hasattr(os, "fork"),
                               reason="os.fork required")

@needsfork
def test_functional_boxed(testdir):
    p1 = testdir.makepyfile("""
        import os
        def test_function():
            os.kill(os.getpid(), 15)
    """)
    result = testdir.runpytest(p1, "--boxed")
    result.stdout.fnmatch_lines([
        "*CRASHED*",
        "*1 failed*"
    ])

@needsfork
@pytest.mark.parametrize("capmode", [
    "no",
    pytest.mark.xfail("sys", reason="capture cleanup needed"),
    pytest.mark.xfail("fd", reason="capture cleanup needed")])
def test_functional_boxed_capturing(testdir, capmode):
    p1 = testdir.makepyfile("""
        import os
        import sys
        def test_function():
            sys.stdout.write("hello\\n")
            sys.stderr.write("world\\n")
            os.kill(os.getpid(), 15)
    """)
    result = testdir.runpytest(p1, "--boxed", "--capture=%s" % capmode)
    result.stdout.fnmatch_lines("""
        *CRASHED*
        *stdout*
        hello
        *stderr*
        world
        *1 failed*
    """)

class TestOptionEffects:
    def test_boxed_option_default(self, testdir):
        tmpdir = testdir.tmpdir.ensure("subdir", dir=1)
        config = testdir.parseconfig()
        assert not config.option.boxed
        pytest.importorskip("execnet")
        config = testdir.parseconfig('-d', tmpdir)
        assert not config.option.boxed

    def test_is_not_boxed_by_default(self, testdir):
        config = testdir.parseconfig(testdir.tmpdir)
        assert not config.option.boxed

