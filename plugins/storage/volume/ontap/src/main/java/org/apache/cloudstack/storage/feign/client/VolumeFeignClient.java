package org.apache.cloudstack.storage.feign.client;


import org.apache.cloudstack.storage.feign.FeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


@FeignClient(name = "VolumeClient", url = "https://{clusterIP}/api/storage/volumes", configuration = FeignConfiguration.class)
public interface VolumeFeignClient {

    @DeleteMapping("/storage/volumes/{id}")
    void deleteVolume(@RequestHeader("Authorization") String authHeader,
                      @PathVariable("id") String volumeId);

    @PostMapping("/api/storage/volumes")
    org.apache.cloudstack.storage.feign.model.response.JobResponseDTO createVolumeWithJob(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody org.apache.cloudstack.storage.feign.model.request.VolumeRequestDTO request
    );

    @GetMapping("/api/storage/volumes/{uuid}")
    org.apache.cloudstack.storage.feign.model.response.VolumeDetailsResponseDTO getVolumeDetails(
        @RequestHeader("Authorization") String authHeader,
        @PathVariable("uuid") String uuid
    );

    @PatchMapping("/api/storage/volumes/{uuid}")
    org.apache.cloudstack.storage.feign.model.response.JobResponseDTO updateVolumeRebalancing(
        @RequestHeader("accept") String acceptHeader,
        @PathVariable("uuid") String uuid,
        @RequestBody org.apache.cloudstack.storage.feign.model.request.VolumeRequestDTO request
    );


}
