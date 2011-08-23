import threading
import cloudstackException
import time
import Queue
import copy
import sys

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
class workThread(threading.Thread):
    def __init__(self, in_queue, out_dict, apiClient, db=None):
        threading.Thread.__init__(self)
        self.inqueue = in_queue
        self.output = out_dict
        self.connection = copy.copy(apiClient.connection)
        self.db = None
        if db is not None:
            self.db = copy.copy(db)
        
    def run(self):
        while self.inqueue.qsize() > 0:
            job = self.inqueue.get()
            cmd = job.cmd
            cmdName = cmd.__class__.__name__
            responseName = cmdName.replace("Cmd", "Response")
            responseInstance = self.connection.getclassFromName(cmd, responseName)
            jobstatus = jobStatus()
            jobId = None
            try:
                if not cmd.isAsync:
                    jobstatus.startTime = time.time()
                    result = self.connection.make_request(cmd, responseInstance)
                    jobstatus.result = result
                    jobstatus.endTime = time.time()
                else:
                    result = self.connection.make_request(cmd, responseInstance, True)
                    jobId = self.connection.getAsyncJobId(responseInstance, result)
                    result = self.connection.pollAsyncJob(cmd, responseInstance, jobId)
                    jobstatus.result = result
                    
                    
                jobstatus.status = True
            except cloudstackException.cloudstackAPIException, e:
                jobstatus.result = str(e)
                jobstatus.status = False
            except:
                jobstatus.status = False
                jobstatus.result = sys.exc_info()
            
            if self.db is not None and jobId is not None:
                result = self.db.execute("select created, last_updated from async_job where id=%s"%jobId)
                if result is not None and len(result) > 0:
                    jobstatus.startTime = result[0][0]
                    jobstatus.endTime = result[0][1]
                    delta = jobstatus.endTime - jobstatus.startTime
                    jobstatus.duration = delta.total_seconds()
            #print job.id
            self.output.lock.acquire()
            self.output.dict[job.id] = jobstatus
            self.output.lock.release()
            self.inqueue.task_done()
            
        '''release the resource'''
        self.connection.close()
        if self.db is not None:
            self.db.close()

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
                '''release the api connection'''
                job.apiClient.connection.close()
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
        self.inqueue = Queue.Queue()
        self.output = outputDict() 
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
    
    def waitForComplete(self):
        self.inqueue.join()
        return self.output.dict
    
    '''put commands into a queue at first, then start workers numbers threads to execute this commands'''
    def submitCmdsAndWait(self, cmds, workers=10):
        self.submitCmds(cmds)
        
        for i in range(workers):
            worker = workThread(self.inqueue, self.output, self.apiClient, self.db)
            worker.start()
        
        return self.waitForComplete()

    '''submit one job and execute the same job ntimes, with nums_threads of threads'''
    def submitJobExecuteNtimes(self, job, ntimes=1, nums_threads=1, interval=1):
        inqueue1 = Queue.Queue()
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
        
    '''submit n jobs, execute them with nums_threads of threads'''
    def submitJobs(self, jobs, nums_threads=1, interval=1):
        inqueue1 = Queue.Queue()
        lock = threading.Condition()
    
        for job in jobs:
            setattr(job, "apiClient", copy.copy(self.apiClient))
            setattr(job, "lock", lock)
            inqueue1.put(job)
        
        for i in range(nums_threads):
            work = jobThread(inqueue1, interval)
            work.start()
        inqueue1.join()