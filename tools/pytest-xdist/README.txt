xdist: pytest distributed testing plugin
===============================================================

The `pytest-xdist`_ plugin extends py.test with some unique
test execution modes:

* test run parallelization_: if you have multiple CPUs or hosts you can use
  those for a combined test run.  This allows to speed up
  development or to use special resources of `remote machines`_.

* ``--boxed``: (not available on Windows) run each test in a boxed_
  subprocess to survive ``SEGFAULTS`` or otherwise dying processes

* ``--looponfail``: run your tests repeatedly in a subprocess.  After each run
  py.test waits until a file in your project changes and then re-runs
  the previously failing tests.  This is repeated until all tests pass
  after which again a full run is performed.

* `Multi-Platform`_ coverage: you can specify different Python interpreters
  or different platforms and run tests in parallel on all of them.

Before running tests remotely, ``py.test`` efficiently "rsyncs" your
program source code to the remote place.  All test results
are reported back and displayed to your local terminal.
You may specify different Python versions and interpreters.


Installation
-----------------------

Install the plugin with::

    easy_install pytest-xdist

    # or

    pip install pytest-xdist

or use the package in develope/in-place mode with
a checkout of the `pytest-xdist repository`_ ::

    python setup.py develop

Usage examples
---------------------

.. _parallelization:

Speed up test runs by sending tests to multiple CPUs
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

To send tests to multiple CPUs, type::

    py.test -n NUM

Especially for longer running tests or tests requiring
a lot of IO this can lead to considerable speed ups.


Running tests in a Python subprocess
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

To instantiate a python2.5 sub process and send tests to it, you may type::

    py.test -d --tx popen//python=python2.5

This will start a subprocess which is run with the "python2.5"
Python interpreter, found in your system binary lookup path.

If you prefix the --tx option value like this::

    --tx 3*popen//python=python2.5

then three subprocesses would be created and tests
will be load-balanced across these three processes.

.. _boxed:

Running tests in a boxed subprocess
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

If you have tests involving C or C++ libraries you might have to deal
with tests crashing the process.  For this case you may use the boxing
options::

    py.test --boxed

which will run each test in a subprocess and will report if a test
crashed the process.  You can also combine this option with
running multiple processes to speed up the test run and use your CPU cores::

    py.test -n3 --boxed

this would run 3 testing subprocesses in parallel which each
create new boxed subprocesses for each test.


.. _`remote machines`:

Sending tests to remote SSH accounts
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

Suppose you have a package ``mypkg`` which contains some
tests that you can successfully run locally. And you
have a ssh-reachable machine ``myhost``.  Then
you can ad-hoc distribute your tests by typing::

    py.test -d --tx ssh=myhostpopen --rsyncdir mypkg mypkg

This will synchronize your ``mypkg`` package directory
to an remote ssh account and then locally collect tests
and send them to remote places for execution.

You can specify multiple ``--rsyncdir`` directories
to be sent to the remote side.

**NOTE:** For py.test to collect and send tests correctly
you not only need to make sure all code and tests
directories are rsynced, but that any test (sub) directory
also has an ``__init__.py`` file because internally
py.test references tests as a fully qualified python
module path.  **You will otherwise get strange errors**
during setup of the remote side.

You can specify multiple ``--rsyncignore`` glob-patterns
to be ignored when file are sent to the remote side.
There are also internal ignores: .*, *.pyc, *.pyo, *~
Those you cannot override using rsyncignore command-line or
ini-file option(s).


Sending tests to remote Socket Servers
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

Download the single-module `socketserver.py`_ Python program
and run it like this::

    python socketserver.py

It will tell you that it starts listening on the default
port.  You can now on your home machine specify this
new socket host with something like this::

    py.test -d --tx socket=192.168.1.102:8888 --rsyncdir mypkg mypkg


.. _`atonce`:
.. _`Multi-Platform`:


Running tests on many platforms at once
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

The basic command to run tests on multiple platforms is::

    py.test --dist=each --tx=spec1 --tx=spec2

If you specify a windows host, an OSX host and a Linux
environment this command will send each tests to all
platforms - and report back failures from all platforms
at once.   The specifications strings use the `xspec syntax`_.

.. _`xspec syntax`: http://codespeak.net/execnet/trunk/basics.html#xspec

.. _`socketserver.py`: http://bitbucket.org/hpk42/execnet/raw/2af991418160/execnet/script/socketserver.py

.. _`execnet`: http://codespeak.net/execnet

Specifying test exec environments in an ini file
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

pytest (since version 2.0) supports ini-style cofiguration.
You can for example make running with three subprocesses
your default like this::

    [pytest]
    addopts = -n3

You can also add default environments like this::

    [pytest]
    addopts = --tx ssh=myhost//python=python2.5 --tx ssh=myhost//python=python2.6

and then just type::

    py.test --dist=each

to run tests in each of the environments.

Specifying "rsync" dirs in an ini-file
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

In a ``tox.ini`` or ``setup.cfg`` file in your root project directory
you may specify directories to include or to exclude in synchronisation::

    [pytest]
    rsyncdirs = . mypkg helperpkg
    rsyncignore = .hg

These directory specifications are relative to the directory
where the configuration file was found.

.. _`pytest-xdist`: http://pypi.python.org/pypi/pytest-xdist
.. _`pytest-xdist repository`: http://bitbucket.org/hpk42/pytest-xdist
.. _`pytest`: http://pytest.org

Issue and Bug Tracker
------------------------

Please use the pytest issue tracker for bugs in this plugin, see https://bitbucket.org/hpk42/pytest/issues .


