//package org.apache.cloudstack.storage.ontap.clients;
//
//import org.apache.cloudstack.storage.ontap.models.CreateVolumeRequest;
//import org.apache.cloudstack.storage.ontap.models.CreateVolumeResponse;
//import feign.RequestLine;
//import feign.Body;
//
//public interface OntapFeignClient {
//    @RequestLine("POST /api/storage/volumes")
//    @Body("{body}")
//    CreateVolumeResponse createVolume(CreateVolumeRequest body);
//}
//
//
//
