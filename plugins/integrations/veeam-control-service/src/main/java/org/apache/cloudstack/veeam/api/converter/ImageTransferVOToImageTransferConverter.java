// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.veeam.api.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cloudstack.backup.ImageTransferVO;
import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.DisksRouteHandler;
import org.apache.cloudstack.veeam.api.HostsRouteHandler;
import org.apache.cloudstack.veeam.api.ImageTransfersRouteHandler;
import org.apache.cloudstack.veeam.api.dto.ImageTransfer;
import org.apache.cloudstack.veeam.api.dto.Link;
import org.apache.cloudstack.veeam.api.dto.NamedList;
import org.apache.cloudstack.veeam.api.dto.Ref;

import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;

public class ImageTransferVOToImageTransferConverter {
    public static ImageTransfer toImageTransfer(ImageTransferVO vo, final Function<Long, HostJoinVO> hostResolver,
            final Function<Long, VolumeJoinVO> volumeResolver) {
        ImageTransfer imageTransfer = new ImageTransfer();
        final String basePath = VeeamControlService.ContextPath.value();
        imageTransfer.setId(vo.getUuid());
        imageTransfer.setHref(basePath + ImageTransfersRouteHandler.BASE_ROUTE + "/" + vo.getUuid());
        imageTransfer.setActive(Boolean.toString(vo.getProgress() != null && vo.getProgress() > 0 && vo.getProgress() < 100));
        imageTransfer.setDirection(vo.getDirection().name());
        imageTransfer.setFormat("cow");
        imageTransfer.setInactivityTimeout(Integer.toString(3600));
        imageTransfer.setPhase(vo.getPhase().name());
        if (org.apache.cloudstack.backup.ImageTransfer.Phase.finished.equals(vo.getPhase())) {
            imageTransfer.setPhase("finished_success");
        } else if (org.apache.cloudstack.backup.ImageTransfer.Phase.failed.equals(vo.getPhase())) {
            imageTransfer.setPhase("finished_failed");
        }
        imageTransfer.setProxyUrl(vo.getTransferUrl());
        imageTransfer.setShallow(Boolean.toString(false));
        imageTransfer.setTimeoutPolicy("legacy");
        imageTransfer.setTransferUrl(vo.getTransferUrl());
        imageTransfer.setTransferred(Long.toString(0));
        if (hostResolver != null) {
            HostJoinVO hostVo = hostResolver.apply(vo.getHostId());
            if (hostVo != null) {
                imageTransfer.setHost(Ref.of(basePath + HostsRouteHandler.BASE_ROUTE + "/" + hostVo.getUuid(), hostVo.getUuid()));
            }
        }
        if (volumeResolver != null) {
            VolumeJoinVO volumeVo = volumeResolver.apply(vo.getVolumeId());
            if (volumeVo != null) {
                imageTransfer.setDisk(Ref.of(basePath + DisksRouteHandler.BASE_ROUTE + "/" + volumeVo.getUuid(), volumeVo.getUuid()));
                imageTransfer.setImage(Ref.of(null, volumeVo.getUuid()));
            }
        }
        final List<Link> links = new ArrayList<>();
        links.add(getLink(imageTransfer, "cancel"));
        links.add(getLink(imageTransfer, "finalize"));
        imageTransfer.setActions(NamedList.of("link", links));
        return imageTransfer;
    }

    public static List<ImageTransfer> toImageTransferList(List<? extends ImageTransferVO> vos,
            final Function<Long, HostJoinVO> hostResolver,
            final Function<Long, VolumeJoinVO> volumeResolver) {
        return vos.stream().map(vo -> toImageTransfer(vo, hostResolver, volumeResolver))
                .collect(Collectors.toList());
    }

    private static Link getLink(ImageTransfer it, String rel) {
        return Link.of(rel, it.getHref() + "/" + rel);
    }
}
