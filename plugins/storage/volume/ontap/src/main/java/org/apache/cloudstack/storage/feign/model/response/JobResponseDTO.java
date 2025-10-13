package org.apache.cloudstack.storage.feign.model.response;

public class JobResponseDTO {
    private Job job;
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public static class Job {
        private String uuid;
        private Links links;
        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public Links getLinks() { return links; }
        public void setLinks(Links links) { this.links = links; }
    }

    public static class Links {
        private Self self;
        public Self getSelf() { return self; }
        public void setSelf(Self self) { this.self = self; }
    }

    public static class Self {
        private String href;
        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
    }
}

