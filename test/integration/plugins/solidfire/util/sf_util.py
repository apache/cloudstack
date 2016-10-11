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

def check_list(in_list, expected_size_of_list, obj_assert, err_msg):
    obj_assert.assertEqual(
        isinstance(in_list, list),
        True,
        "'in_list' is not a list."
    )

    obj_assert.assertEqual(
        len(in_list),
        expected_size_of_list,
        err_msg
    )

def get_sf_account_id(cs_api, cs_account_id, primary_storage_id, obj_assert, err_msg):
    sf_account_id_request = {'accountid': cs_account_id, 'storageid': primary_storage_id}
    sf_account_id_result = cs_api.getSolidFireAccountId(sf_account_id_request)
    sf_account_id = sf_account_id_result['apisolidfireaccountid']['solidFireAccountId']

    obj_assert.assertEqual(
        isinstance(sf_account_id, int),
        True,
        err_msg
    )

    return sf_account_id

def get_iqn(cs_api, volume, obj_assert):
    # Get volume IQN
    sf_iscsi_name_request = {'volumeid': volume.id}
    sf_iscsi_name_result = cs_api.getVolumeiScsiName(sf_iscsi_name_request)
    sf_iscsi_name = sf_iscsi_name_result['apivolumeiscsiname']['volumeiScsiName']

    check_iscsi_name(sf_iscsi_name, obj_assert)

    return sf_iscsi_name

def check_iscsi_name(sf_iscsi_name, obj_assert):
    obj_assert.assertEqual(
        sf_iscsi_name[0],
        "/",
        "The iSCSI name needs to start with a forward slash."
    )

def set_supports_resign(supports_resign, db_connection):
    _set_supports_resign_for_table(supports_resign, db_connection, "host_details")
    _set_supports_resign_for_table(supports_resign, db_connection, "cluster_details")

def _set_supports_resign_for_table(supports_resign, db_connection, table):
    sql_query = "Update " + str(table) + " Set value = '" + str(supports_resign) + "' Where name = 'supportsResign'"

    # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
    db_connection.execute(sql_query)

def purge_solidfire_volumes(sfe):
    deleted_volumes = sfe.list_deleted_volumes()

    for deleted_volume in deleted_volumes.volumes:
        sfe.purge_deleted_volume(deleted_volume.volume_id)

def get_not_active_sf_volumes(sfe, sf_account_id=None):
    if sf_account_id is not None:
        sf_volumes = sfe.list_volumes_for_account(sf_account_id).volumes

        if sf_volumes is not None and len(sf_volumes) > 0:
            sf_volumes = _get_not_active_sf_volumes_only(sf_volumes)
    else:
        sf_volumes = sfe.list_deleted_volumes().volumes

    return sf_volumes

def _get_not_active_sf_volumes_only(sf_volumes):
    not_active_sf_volumes_only = []

    for sf_volume in sf_volumes:
        if sf_volume.status != "active":
            not_active_sf_volumes_only.append(sf_volume)

    return not_active_sf_volumes_only

def get_active_sf_volumes(sfe, sf_account_id=None):
    if sf_account_id is not None:
        sf_volumes = sfe.list_volumes_for_account(sf_account_id).volumes

        if sf_volumes is not None and len(sf_volumes) > 0:
            sf_volumes = _get_active_sf_volumes_only(sf_volumes)
    else:
        sf_volumes = sfe.list_active_volumes().volumes

    return sf_volumes

def _get_active_sf_volumes_only(sf_volumes):
    active_sf_volumes_only = []

    for sf_volume in sf_volumes:
        if sf_volume.status == "active":
            active_sf_volumes_only.append(sf_volume)

    return active_sf_volumes_only

def check_and_get_sf_volume(sf_volumes, sf_volume_name, obj_assert, should_exist=True):
    sf_volume = None

    for volume in sf_volumes:
        if volume.name == sf_volume_name:
            sf_volume = volume
            break

    if should_exist:
        obj_assert.assertNotEqual(
            sf_volume,
            None,
            "Check if SF volume was created in correct account: " + str(sf_volumes)
        )
    else:
        obj_assert.assertEqual(
            sf_volume,
            None,
            "Check if SF volume was deleted: " + str(sf_volumes)
        )

    return sf_volume

def check_xen_sr(xen_sr_name, xen_session, obj_assert, should_exist=True):
    xen_sr = xen_session.xenapi.SR.get_by_name_label(xen_sr_name)

    if should_exist:
        check_list(xen_sr, 1, obj_assert, "SR " + xen_sr_name + " doesn't exist, but should.")

        sr_shared = xen_session.xenapi.SR.get_shared(xen_sr[0])

        obj_assert.assertEqual(
            sr_shared,
            True,
            "SR " + xen_sr_name + " is not shared, but should be."
        )
    else:
        check_list(xen_sr, 0, obj_assert, "SR " + xen_sr_name + " exists, but shouldn't.")

def check_vag(sf_volume, sf_vag_id, obj_assert):
    obj_assert.assertEqual(
        len(sf_volume.volume_access_groups),
        1,
        "The volume should only be in one VAG."
    )

    obj_assert.assertEqual(
        sf_volume.volume_access_groups[0],
        sf_vag_id,
        "The volume is not in the VAG with the following ID: " + str(sf_vag_id) + "."
    )

def get_vag_id(cs_api, cluster_id, primary_storage_id, obj_assert):
    # Get SF Volume Access Group ID
    sf_vag_id_request = {'clusterid': cluster_id, 'storageid': primary_storage_id}
    sf_vag_id_result = cs_api.getSolidFireVolumeAccessGroupId(sf_vag_id_request)
    sf_vag_id = sf_vag_id_result['apisolidfirevolumeaccessgroupid']['solidFireVolumeAccessGroupId']

    obj_assert.assertEqual(
        isinstance(sf_vag_id, int),
        True,
        "The SolidFire VAG ID should be a non-zero integer."
    )

    return sf_vag_id

def format_iqn(iqn):
    return "/" + iqn + "/0"

def check_size_and_iops(sf_volume, cs_volume, size, obj_assert):
    obj_assert.assertEqual(
        sf_volume.qos.min_iops,
        cs_volume.miniops,
        "Check QoS - Min IOPS: " + str(sf_volume.qos.min_iops)
    )

    obj_assert.assertEqual(
        sf_volume.qos.max_iops,
        cs_volume.maxiops,
        "Check QoS - Max IOPS: " + str(sf_volume.qos.max_iops)
    )

    obj_assert.assertEqual(
        sf_volume.total_size,
        size,
        "Check SolidFire volume size: " + str(sf_volume.total_size)
    )

def get_volume_size_with_hsr(cs_api, cs_volume, obj_assert):
    # Get underlying SF volume size with hypervisor snapshot reserve
    sf_volume_size_request = {'volumeid': cs_volume.id}
    sf_volume_size_result = cs_api.getSolidFireVolumeSize(sf_volume_size_request)
    sf_volume_size = sf_volume_size_result['apisolidfirevolumesize']['solidFireVolumeSize']

    obj_assert.assertEqual(
        isinstance(sf_volume_size, int),
        True,
        "The SolidFire volume size should be a non-zero integer."
    )

    return sf_volume_size
