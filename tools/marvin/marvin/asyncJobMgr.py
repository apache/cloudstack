# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import threading
from marvin import cloudstackException
import time
import queue
import copy
import sys
from . import jsonHelper
import datetime


class job(object):

    def __init__(self):
        self.id = None
        self.cmd = None


class jobStatus(object):

    def __init__(self):
        self.result = None
        self.status = None
        self.startTime = None
        self.endTime = None
        self.duration = None
        self.jobId = None
        self.responsecls = None

    def __str__(self):
        return '{%s}' % str(', '.join('%s : %s' % (k, repr(v)) for (k, v)
                                      in self.__dict__.items()))


class workThread(threading.Thread):

    def __init__(self, in_queue, outqueue, apiClient, db=None, lock=None):
        threading.Thread.__init__(self)
        self.inqueue = in_queue
        self.output = outqueue
        self.connection = apiClient.connection.__copy__()
        self.db = None
        self.lock = lock

    def queryAsynJob(self, job):
        if job.jobId is None:
            return job

        try:
            self.lock.acquire()
            result = self.connection.poll(job.jobId, job.responsecls).jobresult
        except cloudstackException.CloudstackAPIException as e:
            result = str(e)
        finally:
            self.lock.release()

        job.result = result
        return job

    def executeCmd(self, job):
        cmd = job.cmd

        jobstatus = jobStatus()
        jobId = None
        try:
            self.lock.acquire()

            if cmd.isAsync == "false":
                jobstatus.startTime = datetime.datetime.now()

                result = self.connection.marvin_request(cmd)
                jobstatus.result = result
                jobstatus.endTime = datetime.datetime.now()
                jobstatus.duration =\
                    time.mktime(jobstatus.endTime.timetuple()) - time.mktime(
                        jobstatus.startTime.timetuple())
            else:
                result = self.connection.marvinRequest(cmd)
                if result is None:
                    jobstatus.status = False
                else:
                    jobId = result.jobid
                    jobstatus.jobId = jobId
                    try:
                        responseName =\
                            cmd.__class__.__name__.replace("Cmd", "Response")
                        jobstatus.responsecls =\
                            jsonHelper.getclassFromName(cmd, responseName)
                    except:
                        pass
                    jobstatus.status = True
        except cloudstackException.CloudstackAPIException as e:
            jobstatus.result = str(e)
            jobstatus.status = False
        except:
            jobstatus.status = False
            jobstatus.result = sys.exc_info()
        finally:
            self.lock.release()

        return jobstatus

    def run(self):
        while self.inqueue.qsize() > 0:
            job = self.inqueue.get()
            if isinstance(job, jobStatus):
                jobstatus = self.queryAsynJob(job)
            else:
                jobstatus = self.executeCmd(job)

            self.output.put(jobstatus)
            self.inqueue.task_done()


class jobThread(threading.Thread):

    def __init__(self, inqueue, interval):
        threading.Thread.__init__(self)
        self.inqueue = inqueue
        self.interval = interval

    def run(self):
        while self.inqueue.qsize() > 0:
            job = self.inqueue.get()
            try:
                job.run()
            except:
                pass

            self.inqueue.task_done()
            time.sleep(self.interval)


class outputDict(object):

    def __init__(self):
        self.lock = threading.Condition()
        self.dict = {}


class asyncJobMgr(object):

    def __init__(self, apiClient, db):
        self.inqueue = queue.Queue()
        self.output = outputDict()
        self.outqueue = queue.Queue()
        self.apiClient = apiClient
        self.db = db

    def submitCmds(self, cmds):
        if not self.inqueue.empty():
            return False
        id = 0
        ids = []
        for cmd in cmds:
            asyncjob = job()
            asyncjob.id = id
            asyncjob.cmd = cmd
            self.inqueue.put(asyncjob)
            id += 1
            ids.append(id)
        return ids

    def updateTimeStamp(self, jobstatus):
        jobId = jobstatus.jobId
        if jobId is not None and self.db is not None:
            result = self.db.execute(
                "select job_status, created, last_updated from async_job where\
 id='%s'" % str(jobId))
            if result is not None and len(result) > 0:
                if result[0][0] == 1:
                    jobstatus.status = True
                else:
                    jobstatus.status = False
                    jobstatus.startTime = result[0][1]
                    jobstatus.endTime = result[0][2]
                    delta = jobstatus.endTime - jobstatus.startTime
                    jobstatus.duration = delta.total_seconds()

    def waitForComplete(self, workers=10):
        self.inqueue.join()
        lock = threading.Lock()
        resultQueue = queue.Queue()
        '''intermediate result is stored in self.outqueue'''
        for i in range(workers):
            worker = workThread(self.outqueue, resultQueue, self.apiClient,
                                self.db, lock)
            worker.start()

        self.outqueue.join()

        asyncJobResult = []
        while resultQueue.qsize() > 0:
            jobstatus = resultQueue.get()
            self.updateTimeStamp(jobstatus)
            asyncJobResult.append(jobstatus)

        return asyncJobResult

    def submitCmdsAndWait(self, cmds, workers=10):
        '''
            put commands into a queue at first, then start workers numbers
            threads to execute this commands
        '''
        self.submitCmds(cmds)
        lock = threading.Lock()
        for i in range(workers):
            worker = workThread(self.inqueue, self.outqueue, self.apiClient,
                                self.db, lock)
            worker.start()

        return self.waitForComplete(workers)

    def submitJobExecuteNtimes(self, job, ntimes=1, nums_threads=1,
                               interval=1):
        '''
        submit one job and execute the same job ntimes, with nums_threads
        of threads
        '''
        inqueue1 = queue.Queue()
        lock = threading.Condition()
        for i in range(ntimes):
            newjob = copy.copy(job)
            setattr(newjob, "apiClient", copy.copy(self.apiClient))
            setattr(newjob, "lock", lock)
            inqueue1.put(newjob)

        for i in range(nums_threads):
            work = jobThread(inqueue1, interval)
            work.start()
        inqueue1.join()

    def submitJobs(self, jobs, nums_threads=1, interval=1):
        '''submit n jobs, execute them with nums_threads of threads'''
        inqueue1 = queue.Queue()
        lock = threading.Condition()

        for job in jobs:
            setattr(job, "apiClient", copy.copy(self.apiClient))
            setattr(job, "lock", lock)
            inqueue1.put(job)

        for i in range(nums_threads):
            work = jobThread(inqueue1, interval)
            work.start()
        inqueue1.join()
