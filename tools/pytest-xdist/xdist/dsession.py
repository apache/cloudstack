import difflib

import pytest
import py
from xdist.slavemanage import NodeManager


queue = py.builtin._tryimport('queue', 'Queue')


class EachScheduling:

    def __init__(self, numnodes, log=None):
        self.numnodes = numnodes
        self.node2collection = {}
        self.node2pending = {}
        if log is None:
            self.log = py.log.Producer("eachsched")
        else:
            self.log = log.eachsched
        self.collection_is_completed = False

    def hasnodes(self):
        return bool(self.node2pending)

    def addnode(self, node):
        self.node2collection[node] = None

    def tests_finished(self):
        if not self.collection_is_completed:
            return False
        return True

    def addnode_collection(self, node, collection):
        assert not self.collection_is_completed
        assert self.node2collection[node] is None
        self.node2collection[node] = list(collection)
        self.node2pending[node] = []
        if len(self.node2pending) >= self.numnodes:
            self.collection_is_completed = True

    def remove_item(self, node, item_index, duration=0):
        self.node2pending[node].remove(item_index)

    def remove_node(self, node):
        # KeyError if we didn't get an addnode() yet
        pending = self.node2pending.pop(node)
        if not pending:
            return
        crashitem = self.node2collection[node][pending.pop(0)]
        # XXX do or report something wrt the remaining per-node pending items?
        return crashitem

    def init_distribute(self):
        assert self.collection_is_completed
        for node, pending in self.node2pending.items():
            node.send_runtest_all()
            pending[:] = range(len(self.node2collection[node]))


class LoadScheduling:
    def __init__(self, numnodes, log=None):
        self.numnodes = numnodes
        self.node2pending = {}
        self.node2collection = {}
        self.nodes = []
        self.pending = []
        if log is None:
            self.log = py.log.Producer("loadsched")
        else:
            self.log = log.loadsched
        self.collection_is_completed = False

    def hasnodes(self):
        return bool(self.node2pending)

    def addnode(self, node):
        self.node2pending[node] = []
        self.nodes.append(node)

    def tests_finished(self):
        if not self.collection_is_completed:
            return False
        for pending in self.node2pending.values():
            if len(pending) >= 2:
                return False
        return True

    def addnode_collection(self, node, collection):
        assert not self.collection_is_completed
        assert node in self.node2pending
        self.node2collection[node] = list(collection)
        if len(self.node2collection) >= self.numnodes:
            self.collection_is_completed = True

    def remove_item(self, node, item_index, duration=0):
        self.node2pending[node].remove(item_index)
        self.check_schedule(node, duration=duration)

    def check_schedule(self, node, duration=0):
        if self.pending:
            # how many nodes do we have?
            num_nodes = len(self.node2pending)
            # if our node goes below a heuristic minimum, fill it out to
            # heuristic maximum
            items_per_node_min = max(
                    2, len(self.pending) // num_nodes // 4)
            items_per_node_max = max(
                    2, len(self.pending) // num_nodes // 2)
            node_pending = self.node2pending[node]
            if len(node_pending) < items_per_node_min:
                if duration >= 0.1 and len(node_pending) >= 2:
                    # seems the node is doing long-running tests
                    # and has enough items to continue
                    # so let's rather wait with sending new items
                    return
                num_send = items_per_node_max - len(node_pending)
                self._send_tests(node, num_send)

        self.log("num items waiting for node:", len(self.pending))
        #self.log("node2pending:", self.node2pending)

    def remove_node(self, node):
        self.nodes.remove(node)
        pending = self.node2pending.pop(node)
        if not pending:
            return
        # the node has crashed on the item if there are pending ones
        # and we are told to remove the node
        crashitem = self.collection[pending.pop(0)]

        # put the remaining items back to the general pending list
        self.pending.extend(pending)
        # see if some nodes can pick the remaining tests up already
        for node in self.node2pending:
            self.check_schedule(node)
        return crashitem

    def init_distribute(self):
        assert self.collection_is_completed
        # XXX allow nodes to have different collections
        if not self._check_nodes_have_same_collection():
            self.log('**Different tests collected, aborting run**')
            return

        # all collections are the same, good.
        # we now create an index
        self.collection = list(self.node2collection.values())[0]
        self.pending[:] = range(len(self.collection))
        if not self.collection:
            return

        # how many items per node do we have about?
        items_per_node = len(self.collection) // len(self.node2pending)
        # take a fraction of tests for initial distribution
        node_chunksize = max(items_per_node // 4, 2)
        # and initialize each node with a chunk of tests
        for node in self.nodes:
            self._send_tests(node, node_chunksize)

    #f = open("/tmp/sent", "w")
    def _send_tests(self, node, num):
        tests_per_node = self.pending[:num]
        #print >>self.f, "sent", node, tests_per_node
        if tests_per_node:
            del self.pending[:num]
            self.node2pending[node].extend(tests_per_node)
            node.send_runtest_some(tests_per_node)

    def _check_nodes_have_same_collection(self):
        """
        Return True if all nodes have collected the same items, False otherwise.
        This method also logs the collection differences as they are found.
        """
        node_collection_items = list(self.node2collection.items())
        first_node, col = node_collection_items[0]
        same_collection = True
        for node, collection in node_collection_items[1:]:
            msg = report_collection_diff(
                col,
                collection,
                first_node.gateway.id,
                node.gateway.id,
            )
            if msg:
                self.log(msg)
                same_collection = False

        return same_collection


def report_collection_diff(from_collection, to_collection, from_id, to_id):
    """Report the collected test difference between two nodes.

    :returns: detailed message describing the difference between the given
    collections, or None if they are equal.
    """
    if from_collection == to_collection:
        return None

    diff = difflib.unified_diff(
        from_collection,
        to_collection,
        fromfile=from_id,
        tofile=to_id,
    )
    error_message = py.builtin._totext(
        'Different tests were collected between {from_id} and {to_id}. '
        'The difference is:\n'
        '{diff}'
    ).format(from_id=from_id, to_id=to_id, diff='\n'.join(diff))
    msg = "\n".join([x.rstrip() for x in error_message.split("\n")])
    return msg


class Interrupted(KeyboardInterrupt):
    """ signals an immediate interruption. """

class DSession:
    def __init__(self, config):
        self.config = config
        self.log = py.log.Producer("dsession")
        if not config.option.debug:
            py.log.setconsumer(self.log._keywords, None)
        self.shuttingdown = False
        self.countfailures = 0
        self.maxfail = config.getvalue("maxfail")
        self.queue = queue.Queue()
        self._failed_collection_errors = {}
        try:
            self.terminal = config.pluginmanager.getplugin("terminalreporter")
        except KeyError:
            self.terminal = None
        else:
            self.trdist = TerminalDistReporter(config)
            config.pluginmanager.register(self.trdist, "terminaldistreporter")

    def report_line(self, line):
        if self.terminal and self.config.option.verbose >= 0:
            self.terminal.write_line(line)

    @pytest.mark.trylast
    def pytest_sessionstart(self, session):
        self.nodemanager = NodeManager(self.config)
        self.nodemanager.setup_nodes(putevent=self.queue.put)

    def pytest_sessionfinish(self, session):
        """ teardown any resources after a test run. """
        nm = getattr(self, 'nodemanager', None) # if not fully initialized
        if nm is not None:
            nm.teardown_nodes()

    def pytest_collection(self):
        # prohibit collection of test items in master process
        return True

    def pytest_runtestloop(self):
        numnodes = len(self.nodemanager.specs)
        dist = self.config.getvalue("dist")
        if dist == "load":
            self.sched = LoadScheduling(numnodes, log=self.log)
        elif dist == "each":
            self.sched = EachScheduling(numnodes, log=self.log)
        else:
            assert 0, dist
        self.shouldstop = False
        self.session_finished = False
        while not self.session_finished:
            self.loop_once()
            if self.shouldstop:
                raise Interrupted(str(self.shouldstop))
        return True

    def loop_once(self):
        """ process one callback from one of the slaves. """
        while 1:
            try:
                eventcall = self.queue.get(timeout=2.0)
                break
            except queue.Empty:
                continue
        callname, kwargs = eventcall
        assert callname, kwargs
        method = "slave_" + callname
        call = getattr(self, method)
        self.log("calling method", method, kwargs)
        call(**kwargs)
        if self.sched.tests_finished():
            self.triggershutdown()

    #
    # callbacks for processing events from slaves
    #

    def slave_slaveready(self, node, slaveinfo):
        node.slaveinfo = slaveinfo
        node.slaveinfo['id'] = node.gateway.id
        node.slaveinfo['spec'] = node.gateway.spec
        self.config.hook.pytest_testnodeready(node=node)
        self.sched.addnode(node)
        if self.shuttingdown:
            node.shutdown()

    def slave_slavefinished(self, node):
        self.config.hook.pytest_testnodedown(node=node, error=None)
        if node.slaveoutput['exitstatus'] == 2: # keyboard-interrupt
            self.shouldstop = "%s received keyboard-interrupt" % (node,)
            self.slave_errordown(node, "keyboard-interrupt")
            return
        crashitem = self.sched.remove_node(node)
        assert not crashitem, (crashitem, node)
        if self.shuttingdown and not self.sched.hasnodes():
            self.session_finished = True

    def slave_errordown(self, node, error):
        self.config.hook.pytest_testnodedown(node=node, error=error)
        try:
            crashitem = self.sched.remove_node(node)
        except KeyError:
            pass
        else:
            if crashitem:
                self.handle_crashitem(crashitem, node)
                #self.report_line("item crashed on node: %s" % crashitem)
        if not self.sched.hasnodes():
            self.session_finished = True

    def slave_collectionfinish(self, node, ids):
        self.sched.addnode_collection(node, ids)
        if self.terminal:
            self.trdist.setstatus(node.gateway.spec, "[%d]" %(len(ids)))

        if self.sched.collection_is_completed:
            if self.terminal:
                self.trdist.ensure_show_status()
                self.terminal.write_line("")
                self.terminal.write_line("scheduling tests via %s" %(
                    self.sched.__class__.__name__))

            self.sched.init_distribute()

    def slave_logstart(self, node, nodeid, location):
        self.config.hook.pytest_runtest_logstart(
            nodeid=nodeid, location=location)

    def slave_testreport(self, node, rep):
        if not (rep.passed and rep.when != "call"):
            if rep.when in ("setup", "call"):
                self.sched.remove_item(node, rep.item_index, rep.duration)
        #self.report_line("testreport %s: %s" %(rep.id, rep.status))
        rep.node = node
        self.config.hook.pytest_runtest_logreport(report=rep)
        self._handlefailures(rep)

    def slave_collectreport(self, node, rep):
        if rep.failed:
            self._failed_slave_collectreport(node, rep)

    def _failed_slave_collectreport(self, node, rep):
        # Check we haven't already seen this report (from
        # another slave).
        if rep.longrepr not in self._failed_collection_errors:
            self._failed_collection_errors[rep.longrepr] = True
            self.config.hook.pytest_collectreport(report=rep)
            self._handlefailures(rep)

    def _handlefailures(self, rep):
        if rep.failed:
            self.countfailures += 1
            if self.maxfail and self.countfailures >= self.maxfail:
                self.shouldstop = "stopping after %d failures" % (
                    self.countfailures)

    def triggershutdown(self):
        self.log("triggering shutdown")
        self.shuttingdown = True
        for node in self.sched.node2pending:
            node.shutdown()

    def handle_crashitem(self, nodeid, slave):
        # XXX get more reporting info by recording pytest_runtest_logstart?
        runner = self.config.pluginmanager.getplugin("runner")
        fspath = nodeid.split("::")[0]
        msg = "Slave %r crashed while running %r" %(slave.gateway.id, nodeid)
        rep = runner.TestReport(nodeid, (fspath, None, fspath), (),
            "failed", msg, "???")
        rep.node = slave
        self.config.hook.pytest_runtest_logreport(report=rep)

class TerminalDistReporter:
    def __init__(self, config):
        self.config = config
        self.tr = config.pluginmanager.getplugin("terminalreporter")
        self._status = {}
        self._lastlen = 0

    def write_line(self, msg):
        self.tr.write_line(msg)

    def ensure_show_status(self):
        if not self.tr.hasmarkup:
            self.write_line(self.getstatus())

    def setstatus(self, spec, status, show=True):
        self._status[spec.id] = status
        if show and self.tr.hasmarkup:
            self.rewrite(self.getstatus())

    def getstatus(self):
        parts = ["%s %s" %(spec.id, self._status[spec.id])
                   for spec in self._specs]
        return " / ".join(parts)

    def rewrite(self, line, newline=False):
        pline = line + " " * max(self._lastlen-len(line), 0)
        if newline:
            self._lastlen = 0
            pline += "\n"
        else:
            self._lastlen = len(line)
        self.tr.rewrite(pline, bold=True)

    def pytest_xdist_setupnodes(self, specs):
        self._specs = specs
        for spec in specs:
            self.setstatus(spec, "I", show=False)
        self.setstatus(spec, "I", show=True)
        self.ensure_show_status()

    def pytest_xdist_newgateway(self, gateway):
        if self.config.option.verbose > 0:
            rinfo = gateway._rinfo()
            version = "%s.%s.%s" % rinfo.version_info[:3]
            self.rewrite("[%s] %s Python %s cwd: %s" % (
                gateway.id, rinfo.platform, version, rinfo.cwd),
                newline=True)
        self.setstatus(gateway.spec, "C")

    def pytest_testnodeready(self, node):
        if self.config.option.verbose > 0:
            d = node.slaveinfo
            infoline = "[%s] Python %s" %(
                d['id'],
                d['version'].replace('\n', ' -- '),)
            self.rewrite(infoline, newline=True)
        self.setstatus(node.gateway.spec, "ok")

    def pytest_testnodedown(self, node, error):
        if not error:
            return
        self.write_line("[%s] node down: %s" %(node.gateway.id, error))

    #def pytest_xdist_rsyncstart(self, source, gateways):
    #    targets = ",".join([gw.id for gw in gateways])
    #    msg = "[%s] rsyncing: %s" %(targets, source)
    #    self.write_line(msg)
    #def pytest_xdist_rsyncfinish(self, source, gateways):
    #    targets = ", ".join(["[%s]" % gw.id for gw in gateways])
    #    self.write_line("rsyncfinish: %s -> %s" %(source, targets))

