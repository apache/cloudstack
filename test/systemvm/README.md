Requirements
============
To run these tests, first get the vagrant setup for the systemvm working,
see ../../tools/vagrant/systemvm.

Then, install dependencies

    pip install nose paramiko python-vagrant envassert cuisine fabric

Running tests
=============
Then run the tests using your favorite python unittest runner

    nosetests-2.7

If you have already started the systemvm with 'vagrant up', that VM will get
used for all the tests.

If you have not started the systemvm yet, it will be started and stopped for
every test case. That's nice for test isolation, but it's very slow, so it is
not recommended.

You can also run these tests out of the box with PyDev or PyCharm or whatever.

Adding tests
============
Simply create new test_xxx.py files with test cases that extend from
SystemVMTestCase.

Use [envassert](https://pypi.python.org/pypi/envassert) checks to define
your test assertions.

Use [cuisine](https://pypi.python.org/pypi/cuisine),
[fab](https://pypi.python.org/pypi/Fabric), or
[paramiko](https://pypi.python.org/pypi/paramiko) to otherwise interact with
the systemvm. When you do, please consider creating your own little wrappers
around fab run. I.e. the pattern is

```
from __future__ import with_statement
from fabric.api import run, hide

def something_to_do(argument):
    with hide("everything"):
        result = run("do something %s" % argument).wrangle()
        return "expected" in result
```

for a new kind of check and then in your test

```
class HelloSystemVMTestCase(SystemVMTestCase):
    @attr(tags=["systemvm"], required_hardware="true")
    def test_something(self):
        assert something_to_do('foo')
```

Edit, test, edit, test
======================
The SystemVM Vagrantfile sets up rsync from systemvm/patches. These rsyncs run
once, when you type 'vagrant up'. To do these rsyncs every time you change a
patch file, run 'vagrant rsync-auto'. With that, your development process can
be,

* once, start up vagrant with 'vagrant up'
* once, start up the rsync watcher with 'vagrant rsync-auto'
* iterate:
  * write a test, save the file
  * run 'nostests' to check that the test fails
  * change a systemvm script to help the test pass, save the file
  * vagrant rsyncs the changed file
  * run 'nosetests' to check that the test now passes

If you use PyDev or PyCharm you can set it up to watch your test files for
changes and auto-run any changed tests.
