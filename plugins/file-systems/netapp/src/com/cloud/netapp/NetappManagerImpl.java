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
package com.cloud.netapp;

import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import netapp.manage.NaAPIFailedException;
import netapp.manage.NaElement;
import netapp.manage.NaException;
import netapp.manage.NaServer;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.api.commands.netapp.AssociateLunCmd;
import com.cloud.api.commands.netapp.CreateLunCmd;
import com.cloud.api.commands.netapp.CreateVolumeOnFilerCmd;
import com.cloud.api.commands.netapp.CreateVolumePoolCmd;
import com.cloud.api.commands.netapp.DeleteVolumePoolCmd;
import com.cloud.api.commands.netapp.DestroyLunCmd;
import com.cloud.api.commands.netapp.DestroyVolumeOnFilerCmd;
import com.cloud.api.commands.netapp.DissociateLunCmd;
import com.cloud.api.commands.netapp.ListLunsCmd;
import com.cloud.api.commands.netapp.ListVolumePoolsCmd;
import com.cloud.api.commands.netapp.ListVolumesOnFilerCmd;
import com.cloud.api.commands.netapp.ModifyVolumePoolCmd;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.netapp.dao.LunDao;
import com.cloud.netapp.dao.PoolDao;
import com.cloud.netapp.dao.VolumeDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value = {NetappManager.class})
public class NetappManagerImpl extends ManagerBase implements NetappManager {
    public enum Algorithm {
        roundrobin, leastfull
    }

    public static final Logger s_logger = Logger.getLogger(NetappManagerImpl.class.getName());
    @Inject
    public VolumeDao _volumeDao;
    @Inject
    public PoolDao _poolDao;
    @Inject
    public LunDao _lunDao;
    private NetappAllocator _netappAllocator = null;

    /**
     * Default constructor
     */
    public NetappManagerImpl() {
    }

    @Override
    public void createPool(String poolName, String algorithm) throws InvalidParameterValueException {
        if (s_logger.isDebugEnabled())
            s_logger.debug("Request --> createPool ");

        PoolVO pool = null;
        validAlgorithm(algorithm);
        try {
            pool = new PoolVO(poolName, algorithm);
            _poolDao.persist(pool);

            if (s_logger.isDebugEnabled())
                s_logger.debug("Response --> createPool:success");

        } catch (CloudRuntimeException cre) {
            pool = _poolDao.findPool(poolName);
            if (pool != null) {
                throw new InvalidParameterValueException("Duplicate Pool Name");
            } else {
                throw cre;
            }
        }
    }

    /**
     * This method validates the algorithm used for allocation of the volume
     * @param algorithm -- algorithm type
     * @throws InvalidParameterValueException
     */
    private void validAlgorithm(String algorithm) throws InvalidParameterValueException {
        //TODO: use enum
        if (!algorithm.equalsIgnoreCase("roundrobin") && !algorithm.equalsIgnoreCase("leastfull")) {
            throw new InvalidParameterValueException("Unknown algorithm " + algorithm);
        }
    }

    /**
     * Utility method to get the netapp server object
     * @param serverIp -- ip address of netapp box
     * @param userName -- username
     * @param password -- password
     * @return
     * @throws UnknownHostException
     */
    private NaServer getServer(String serverIp, String userName, String password) throws UnknownHostException {
        //Initialize connection to server, and
        //request version 1.3 of the API set
        NaServer s = new NaServer(serverIp, 1, 3);
        s.setStyle(NaServer.STYLE_LOGIN_PASSWORD);
        s.setAdminUser(userName, password);

        return s;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateLunCmd.class);
        cmdList.add(ListLunsCmd.class);
        cmdList.add(DissociateLunCmd.class);
        cmdList.add(CreateVolumeOnFilerCmd.class);
        cmdList.add(ModifyVolumePoolCmd.class);
        cmdList.add(ListVolumesOnFilerCmd.class);
        cmdList.add(ListVolumePoolsCmd.class);
        cmdList.add(DestroyLunCmd.class);
        cmdList.add(CreateVolumePoolCmd.class);
        cmdList.add(DeleteVolumePoolCmd.class);
        cmdList.add(AssociateLunCmd.class);
        cmdList.add(DestroyVolumeOnFilerCmd.class);
        return cmdList;
    }

    @Override
    public void modifyPool(String poolName, String algorithm) throws InvalidParameterValueException {
        validAlgorithm(algorithm);
        PoolVO pool = _poolDao.findPool(poolName);

        if (pool == null) {
            throw new InvalidParameterValueException("Cannot find pool " + poolName);
        }

        validAlgorithm(algorithm);

        pool.setAlgorithm(algorithm);
        pool.setName(poolName);

        _poolDao.update(pool.getId(), pool);
    }

    @Override
    public void deletePool(String poolName) throws InvalidParameterValueException, ResourceInUseException {
        if (s_logger.isDebugEnabled())
            s_logger.debug("Request --> deletePool ");

        PoolVO pool = _poolDao.findPool(poolName);
        if (pool == null) {
            throw new InvalidParameterValueException("Cannot find pool " + poolName);
        }
        //check if pool is empty
        int volCount = _volumeDao.listVolumes(poolName).size();

        if (volCount == 0) {
            _poolDao.remove(pool.getId());
            if (s_logger.isDebugEnabled())
                s_logger.debug("Request --> deletePool: Success ");

        } else {
            throw new ResourceInUseException("Cannot delete non-empty pool");
        }
    }

    @Override
    public List<PoolVO> listPools() {
        return _poolDao.listAll();
    }

    /**
     * This method destroys the volume on netapp filer
     * @param ipAddress -- ip address of filer
     * @param aggrName -- name of containing aggregate
     * @param volName -- name of volume to destroy
     * @throws ResourceInUseException
     * @throws NaException
     * @throws NaAPIFailedException
     */
    @Override
    @DB
    public void destroyVolumeOnFiler(String ipAddress, String aggrName, String volName) throws ServerException, InvalidParameterValueException, ResourceInUseException {
        NaElement xi0;
        NaElement xi1;
        NetappVolumeVO volume = null;

        volume = _volumeDao.findVolume(ipAddress, aggrName, volName);

        if (volume == null) {
            s_logger.warn("The volume does not exist in our system");
            throw new InvalidParameterValueException("The given tuple:" + ipAddress + "," + aggrName + "," + volName + " doesn't exist in our system");
        }

        List<LunVO> lunsOnVol = _lunDao.listLunsByVolId(volume.getId());

        if (lunsOnVol != null && lunsOnVol.size() > 0) {
            s_logger.warn("There are luns on the volume");
            throw new ResourceInUseException("There are luns on the volume");
        }

        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        PoolVO pool = _poolDao.findById(volume.getPoolId());
        if (pool == null) {
            throw new InvalidParameterValueException("Failed to find pool for given volume");
            //FIXME: choose a better exception. this is a db integrity exception
        }
        pool = _poolDao.acquireInLockTable(pool.getId());
        if (pool == null) {
            throw new ConcurrentModificationException("Failed to acquire lock on pool " + volume.getPoolId());
        }
        NaServer s = null;
        try {
            s = getServer(volume.getIpAddress(), volume.getUsername(), volume.getPassword());
            //bring the volume down
            xi0 = new NaElement("volume-offline");
            xi0.addNewChild("name", volName);
            s.invokeElem(xi0);

            //now destroy it
            xi1 = new NaElement("volume-destroy");
            xi1.addNewChild("name", volName);
            s.invokeElem(xi1);

            //now delete from our records
            _volumeDao.remove(volume.getId());
            txn.commit();

        } catch (UnknownHostException uhe) {
            s_logger.warn("Unable to delete volume on filer ", uhe);
            throw new ServerException("Unable to delete volume on filer", uhe);
        } catch (NaAPIFailedException naf) {
            s_logger.warn("Unable to delete volume on filer ", naf);
            if (naf.getErrno() == 13040) {
                s_logger.info("Deleting the volume: " + volName);
                _volumeDao.remove(volume.getId());
                txn.commit();
            }

            throw new ServerException("Unable to delete volume on filer", naf);
        } catch (NaException nae) {
            txn.rollback();
            s_logger.warn("Unable to delete volume on filer ", nae);
            throw new ServerException("Unable to delete volume on filer", nae);
        } catch (IOException ioe) {
            txn.rollback();
            s_logger.warn("Unable to delete volume on filer ", ioe);
            throw new ServerException("Unable to delete volume on filer", ioe);
        } finally {
            if (pool != null) {
                _poolDao.releaseFromLockTable(pool.getId());
            }
            if (s != null)
                s.close();
        }

    }

    /**
     * This method creates a volume on netapp filer
     * @param ipAddress -- ip address of the filer
     * @param aggName -- name of aggregate
     * @param poolName -- name of pool
     * @param volName -- name of volume
     * @param volSize -- size of volume to be created
     * @param snapshotPolicy -- associated snapshot policy for volume
     * @param snapshotReservation -- associated reservation for snapshots
     * @param username -- username
     * @param password -- password
     * @throws UnknownHostException
     * @throws InvalidParameterValueException
     */
    @Override
    @DB
    public void createVolumeOnFiler(String ipAddress, String aggName, String poolName, String volName, String volSize, String snapshotPolicy,
        Integer snapshotReservation, String username, String password) throws UnknownHostException, ServerException, InvalidParameterValueException {

        if (s_logger.isDebugEnabled())
            s_logger.debug("Request --> createVolume " + "serverIp:" + ipAddress);

        boolean snapPolicy = false;
        boolean snapshotRes = false;
        boolean volumeCreated = false;

        NaServer s = getServer(ipAddress, username, password);

        NaElement xi = new NaElement("volume-create");
        xi.addNewChild("volume", volName);
        xi.addNewChild("containing-aggr-name", aggName);
        xi.addNewChild("size", volSize);

        NaElement xi1 = new NaElement("snapshot-set-reserve");
        if (snapshotReservation != null) {
            snapshotRes = true;
            xi1.addNewChild("percentage", snapshotReservation.toString());
            xi1.addNewChild("volume", volName);
        }

        NaElement xi2 = new NaElement("snapshot-set-schedule");

        if (snapshotPolicy != null) {
            snapPolicy = true;

            String weeks = null;
            String days = null;
            String hours = null;
            String whichHours = null;
            String minutes = null;
            String whichMinutes = null;

            StringTokenizer s1 = new StringTokenizer(snapshotPolicy, " ");

            //count=4: weeks days hours@csi mins@csi
            //count=3: weeks days hours@csi
            //count=2: weeks days
            //count=1: weeks

            if (s1.hasMoreTokens()) {
                weeks = s1.nextToken();
            }
            if (weeks != null && s1.hasMoreTokens()) {
                days = s1.nextToken();
            }
            if (days != null && s1.hasMoreTokens()) {
                String[] hoursArr = s1.nextToken().split("@");
                hours = hoursArr[0];
                whichHours = hoursArr[1];
            }
            if (hours != null && s1.hasMoreTokens()) {
                String[] minsArr = s1.nextToken().split("@");
                minutes = minsArr[0];
                whichMinutes = minsArr[1];
            }

            if (weeks != null)
                xi2.addNewChild("weeks", weeks);
            if (days != null)
                xi2.addNewChild("days", days);
            if (hours != null)
                xi2.addNewChild("hours", hours);
            if (minutes != null)
                xi2.addNewChild("minutes", minutes);
            xi2.addNewChild("volume", volName);

            if (whichHours != null)
                xi2.addNewChild("which-hours", whichHours);
            if (whichMinutes != null)
                xi2.addNewChild("which-minutes", whichMinutes);
        }
        Long volumeId = null;

        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        NetappVolumeVO volume = null;
        volume = _volumeDao.findVolume(ipAddress, aggName, volName);

        if (volume != null) {
            throw new InvalidParameterValueException("The volume for the given ipAddress/aggregateName/volumeName tuple already exists");
        }
        PoolVO pool = _poolDao.findPool(poolName);
        if (pool == null) {
            throw new InvalidParameterValueException("Cannot find pool " + poolName);
        }
        pool = _poolDao.acquireInLockTable(pool.getId());
        if (pool == null) {
            s_logger.warn("Failed to acquire lock on pool " + poolName);
            throw new ConcurrentModificationException("Failed to acquire lock on pool " + poolName);
        }
        volume = new NetappVolumeVO(ipAddress, aggName, pool.getId(), volName, volSize, "", 0, username, password, 0, pool.getName());
        volume = _volumeDao.persist(volume);

        volumeId = volume.getId();
        try {
            s.invokeElem(xi);
            volumeCreated = true;

            if (snapshotRes) {
                s.invokeElem(xi1);
                volume.setSnapshotReservation(snapshotReservation);
                _volumeDao.update(volumeId, volume);
            }

            if (snapPolicy) {
                s.invokeElem(xi2);
                volume.setSnapshotPolicy(snapshotPolicy);
                _volumeDao.update(volumeId, volume);
            }
            txn.commit();
        } catch (NaException nae) {
            //zapi call failed, log and throw e
            s_logger.warn("Failed to create volume on the netapp filer:", nae);
            txn.rollback();
            if (volumeCreated) {
                try {
                    deleteRogueVolume(volName, s);//deletes created volume on filer
                } catch (NaException e) {
                    s_logger.warn("Failed to cleanup created volume whilst rolling back on the netapp filer:", e);
                    throw new ServerException("Unable to create volume via cloudtools."
                        + "Failed to cleanup created volume on netapp filer whilst rolling back on the cloud db:", e);
                } catch (IOException e) {
                    s_logger.warn("Failed to cleanup created volume whilst rolling back on the netapp filer:", e);
                    throw new ServerException("Unable to create volume via cloudtools."
                        + "Failed to cleanup created volume on netapp filer whilst rolling back on the cloud db:", e);
                }
            }
            throw new ServerException("Unable to create volume", nae);
        } catch (IOException ioe) {
            s_logger.warn("Failed to create volume on the netapp filer:", ioe);
            txn.rollback();
            if (volumeCreated) {
                try {
                    deleteRogueVolume(volName, s);//deletes created volume on filer
                } catch (NaException e) {
                    s_logger.warn("Failed to cleanup created volume whilst rolling back on the netapp filer:", e);
                    throw new ServerException("Unable to create volume via cloudtools."
                        + "Failed to cleanup created volume on netapp filer whilst rolling back on the cloud db:", e);
                } catch (IOException e) {
                    s_logger.warn("Failed to cleanup created volume whilst rolling back on the netapp filer:", e);
                    throw new ServerException("Unable to create volume via cloudtools."
                        + "Failed to cleanup created volume on netapp filer whilst rolling back on the cloud db:", e);
                }
            }
            throw new ServerException("Unable to create volume", ioe);
        } finally {
            if (s != null)
                s.close();
            if (pool != null)
                _poolDao.releaseFromLockTable(pool.getId());

        }
    }

    /**
     * This method is primarily used to cleanup volume created on the netapp filer, when createVol api command fails at snapshot reservation.
     * We roll back the db record, but the record on the netapp box still exists. We clean up that record using this helper method.
     * @param volName
     * @param s -- server reference
     * @throws NaException
     * @throws IOException
     */
    private void deleteRogueVolume(String volName, NaServer s) throws NaException, IOException {
        //bring the volume down
        NaElement xi0 = new NaElement("volume-offline");
        xi0.addNewChild("name", volName);
        s.invokeElem(xi0);

        //now destroy it
        NaElement xi1 = new NaElement("volume-destroy");
        xi1.addNewChild("name", volName);
        s.invokeElem(xi1);
    }

    /**
     * This method lists all the volumes by pool name
     * @param poolName
     * @return -- volumes in that pool
     */
    @Override
    public List<NetappVolumeVO> listVolumesOnFiler(String poolName) {

        List<NetappVolumeVO> vols = _volumeDao.listVolumesAscending(poolName);

        for (NetappVolumeVO vol : vols) {
            try {
                String snapScheduleOnFiler = returnSnapshotSchedule(vol);
                vol.setSnapshotPolicy(snapScheduleOnFiler);

            } catch (ServerException e) {
                s_logger.warn("Error trying to get snapshot schedule for volume" + vol.getVolumeName());
            }
        }
        return vols;
    }

    /**
     * Utility method to return snapshot schedule for a volume
     * @param vol -- volume for the snapshot schedule creation
     * @return -- the snapshot schedule
     * @throws ServerException
     */
    private String returnSnapshotSchedule(NetappVolumeVO vol) throws ServerException {

        NaElement xi = new NaElement("snapshot-get-schedule");
        xi.addNewChild("volume", vol.getVolumeName());
        NaServer s = null;
        try {
            s = getServer(vol.getIpAddress(), vol.getUsername(), vol.getPassword());
            NaElement xo = s.invokeElem(xi);
            String weeks = xo.getChildContent("weeks");
            String days = xo.getChildContent("days");
            String hours = xo.getChildContent("hours");
            String minutes = xo.getChildContent("minutes");
            String whichHours = xo.getChildContent("which-hours");
            String whichMinutes = xo.getChildContent("which-minutes");

            StringBuilder sB = new StringBuilder();
            sB.append(weeks)
                .append(" ")
                .append(days)
                .append(" ")
                .append(hours)
                .append("@")
                .append(whichHours)
                .append(" ")
                .append(minutes)
                .append("@")
                .append(whichMinutes);
            return sB.toString();
        } catch (NaException nae) {
            s_logger.warn("Failed to get volume size ", nae);
            throw new ServerException("Failed to get volume size", nae);
        } catch (IOException ioe) {
            s_logger.warn("Failed to get volume size ", ioe);
            throw new ServerException("Failed to get volume size", ioe);
        } finally {
            if (s != null)
                s.close();
        }
    }

    /**
     * This method returns the ascending order list of volumes based on their ids
     * @param poolName -- name of pool
     * @return -- ascending ordered list of volumes based on ids
     */
    @Override
    public List<NetappVolumeVO> listVolumesAscending(String poolName) {
        return _volumeDao.listVolumesAscending(poolName);
    }

    /**
     * This method returns the available size on the volume in terms of bytes
     * @param volName -- name of volume
     * @param userName -- username
     * @param password -- password
     * @param serverIp -- ip address of filer
     * @throws UnknownHostException
     * @return-- available size on the volume in terms of bytes; return -1 if volume is offline
     * @throws ServerException
     */
    @Override
    public long returnAvailableVolumeSize(String volName, String userName, String password, String serverIp) throws ServerException {
        long availableSize = 0;

        NaElement xi = new NaElement("volume-list-info");
        xi.addNewChild("volume", volName);
        NaServer s = null;
        String volumeState = null;
        try {
            s = getServer(serverIp, userName, password);
            NaElement xo = s.invokeElem(xi);
            List volList = xo.getChildByName("volumes").getChildren();
            Iterator volIter = volList.iterator();
            while (volIter.hasNext()) {
                NaElement volInfo = (NaElement)volIter.next();
                availableSize = volInfo.getChildLongValue("size-available", -1);
                volumeState = volInfo.getChildContent("state");
            }

            if (volumeState != null) {
                return volumeState.equalsIgnoreCase("online") ? availableSize : -1; //return -1 if volume is offline
            } else {
                //catch all
                //volume state unreported
                return -1; // as good as volume offline
            }

        } catch (NaException nae) {
            s_logger.warn("Failed to get volume size ", nae);
            throw new ServerException("Failed to get volume size", nae);
        } catch (IOException ioe) {
            s_logger.warn("Failed to get volume size ", ioe);
            throw new ServerException("Failed to get volume size", ioe);
        } finally {
            if (s != null)
                s.close();
        }
    }

    /**
     * This method creates a lun on the netapp filer
     * @param poolName -- name of the pool
     * @param lunSize -- size of the lun to be created
     * @return -- lun path
     * @throws IOException
     * @throws ResourceAllocationException
     * @throws NaException
     */
    @Override
    @DB
    public String[] createLunOnFiler(String poolName, Long lunSize) throws ServerException, InvalidParameterValueException, ResourceAllocationException {
        String[] result = new String[3];
        StringBuilder lunName = new StringBuilder("lun-");
        LunVO lun = null;
        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        PoolVO pool = _poolDao.findPool(poolName);

        if (pool == null) {
            throw new InvalidParameterValueException("Cannot find pool " + poolName);
        }

        if (lunSize <= 0) {
            throw new InvalidParameterValueException("Please specify a valid lun size in Gb");
        }

        String algorithm = pool.getAlgorithm();
        NetappVolumeVO selectedVol = null;

        //sanity check
        int numVolsInPool = _volumeDao.listVolumes(poolName).size();

        if (numVolsInPool == 0) {
            throw new InvalidParameterValueException("No volumes exist in the given pool");
        }
        pool = _poolDao.acquireInLockTable(pool.getId());
        if (pool == null) {
            s_logger.warn("Failed to acquire lock on the pool " + poolName);
            return result;
        }
        NaServer s = null;

        try {
            if (algorithm == null || algorithm.equals(Algorithm.roundrobin.toString())) {
                selectedVol = _netappAllocator.chooseVolumeFromPool(poolName, lunSize);
            } else if (algorithm.equals(Algorithm.leastfull.toString())) {

                selectedVol = _netappAllocator.chooseLeastFullVolumeFromPool(poolName, lunSize);
            }

            if (selectedVol == null) {
                throw new ServerException("Could not find a suitable volume to create lun on");
            }

            if (s_logger.isDebugEnabled())
                s_logger.debug("Request --> createLun " + "serverIp:" + selectedVol.getIpAddress());

            StringBuilder exportPath = new StringBuilder("/vol/");
            exportPath.append(selectedVol.getVolumeName());
            exportPath.append("/");

            lun = new LunVO(exportPath.toString(), selectedVol.getId(), lunSize, "", "");
            lun = _lunDao.persist(lun);

            //Lun id created: 6 digits right justified eg. 000045
            String lunIdStr = String.valueOf(lun.getId());
            String zeroStr = "000000";
            int length = lunIdStr.length();
            int offset = 6 - length;
            StringBuilder lunIdOnPath = new StringBuilder();
            lunIdOnPath.append(zeroStr.substring(0, offset));
            lunIdOnPath.append(lunIdStr);
            exportPath.append("lun-").append(lunIdOnPath.toString());

            lunName.append(lunIdOnPath.toString());

            //update lun name
            lun.setLunName(lunName.toString());
            _lunDao.update(lun.getId(), lun);

            NaElement xi;
            NaElement xi1;

            long lSizeBytes = 1L * lunSize * 1024 * 1024 * 1024; //This prevents integer overflow
            Long lunSizeBytes = new Long(lSizeBytes);

            s = getServer(selectedVol.getIpAddress(), selectedVol.getUsername(), selectedVol.getPassword());

            //create lun
            xi = new NaElement("lun-create-by-size");
            xi.addNewChild("ostype", "linux");
            xi.addNewChild("path", exportPath.toString());
            xi.addNewChild("size", (lunSizeBytes.toString()));

            s.invokeElem(xi);

            try {
                //now create an igroup
                xi1 = new NaElement("igroup-create");
                xi1.addNewChild("initiator-group-name", lunName.toString());
                xi1.addNewChild("initiator-group-type", "iscsi");
                xi1.addNewChild("os-type", "linux");
                s.invokeElem(xi1);
            } catch (NaAPIFailedException e) {
                if (e.getErrno() == 9004) {
                    //igroup already exists hence no error
                    s_logger.warn("Igroup already exists");
                }
            }

            //get target iqn
            NaElement xi4 = new NaElement("iscsi-node-get-name");
            NaElement xo = s.invokeElem(xi4);
            String iqn = xo.getChildContent("node-name");

            lun.setTargetIqn(iqn);
            _lunDao.update(lun.getId(), lun);

            //create lun mapping
            //now map the lun to the igroup
            NaElement xi3 = new NaElement("lun-map");
            xi3.addNewChild("force", "true");
            xi3.addNewChild("initiator-group", lunName.toString());
            xi3.addNewChild("path", lun.getPath() + lun.getLunName());

            xi3.addNewChild("lun-id", lunIdStr);
            s.invokeElem(xi3);

            txn.commit();
            //set the result
            result[0] = lunName.toString();//lunname
            result[1] = iqn;//iqn
            result[2] = selectedVol.getIpAddress();

            return result;

        } catch (NaAPIFailedException naf) {
            if (naf.getErrno() == 9023) { //lun is already mapped to this group
                result[0] = lunName.toString();//lunname;
                result[1] = lun.getTargetIqn();//iqn
                result[2] = selectedVol.getIpAddress();
                return result;
            }
            if (naf.getErrno() == 9024) { //another lun mapped at this group
                result[0] = lunName.toString();//lunname;
                result[1] = lun.getTargetIqn();//iqn
                result[2] = selectedVol.getIpAddress();
                return result;
            }
        } catch (NaException nae) {
            txn.rollback();
            throw new ServerException("Unable to create LUN", nae);
        } catch (IOException ioe) {
            txn.rollback();
            throw new ServerException("Unable to create LUN", ioe);
        } finally {
            if (pool != null) {
                _poolDao.releaseFromLockTable(pool.getId());
            }
            if (s != null) {
                s.close();
            }
        }

        return result;
    }

    /**
     * This method destroys a lun on the netapp filer
     * @param lunName -- name of the lun to be destroyed
     */
    @Override
    @DB
    public void destroyLunOnFiler(String lunName) throws InvalidParameterValueException, ServerException {

        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();

        LunVO lun = _lunDao.findByName(lunName);

        if (lun == null)
            throw new InvalidParameterValueException("Cannot find lun");

        NetappVolumeVO vol = _volumeDao.acquireInLockTable(lun.getVolumeId());
        if (vol == null) {
            s_logger.warn("Failed to lock volume id= " + lun.getVolumeId());
            return;
        }
        NaServer s = null;
        try {
            s = getServer(vol.getIpAddress(), vol.getUsername(), vol.getPassword());

            if (s_logger.isDebugEnabled())
                s_logger.debug("Request --> destroyLun " + ":serverIp:" + vol.getIpAddress());

            try {
                //Unmap lun
                NaElement xi2 = new NaElement("lun-unmap");
                xi2.addNewChild("initiator-group", lunName);
                xi2.addNewChild("path", lun.getPath() + lun.getLunName());
                s.invokeElem(xi2);
            } catch (NaAPIFailedException naf) {
                if (naf.getErrno() == 9016)
                    s_logger.warn("no map exists excpn 9016 caught in deletelun, continuing with delete");
            }

            //destroy lun
            NaElement xi = new NaElement("lun-destroy");
            xi.addNewChild("force", "true");
            xi.addNewChild("path", lun.getPath() + lun.getLunName());
            s.invokeElem(xi);

            //destroy igroup
            NaElement xi1 = new NaElement("igroup-destroy");
            //xi1.addNewChild("force","true");
            xi1.addNewChild("initiator-group-name", lunName);
            s.invokeElem(xi1);

            _lunDao.remove(lun.getId());
            txn.commit();
        } catch (UnknownHostException uhe) {
            txn.rollback();
            s_logger.warn("Failed to delete lun", uhe);
            throw new ServerException("Failed to delete lun", uhe);
        } catch (IOException ioe) {
            txn.rollback();
            s_logger.warn("Failed to delete lun", ioe);
            throw new ServerException("Failed to delete lun", ioe);
        } catch (NaAPIFailedException naf) {
            if (naf.getErrno() == 9017) {//no such group exists excpn
                s_logger.warn("no such group exists excpn 9017 caught in deletelun, continuing with delete");
                _lunDao.remove(lun.getId());
                txn.commit();
            } else if (naf.getErrno() == 9029) {//LUN maps for this initiator group exist
                s_logger.warn("LUN maps for this initiator group exist errno 9029 caught in deletelun, continuing with delete");
                _lunDao.remove(lun.getId());
                txn.commit();
            } else {
                txn.rollback();
                s_logger.warn("Failed to delete lun", naf);
                throw new ServerException("Failed to delete lun", naf);
            }

        } catch (NaException nae) {
            txn.rollback();
            s_logger.warn("Failed to delete lun", nae);
            throw new ServerException("Failed to delete lun", nae);
        } finally {
            if (vol != null) {
                _volumeDao.releaseFromLockTable(vol.getId());
            }
            if (s != null)
                s.close();
        }

    }

    /**
     * This method lists the luns on the netapp filer
     * @param volId -- id of the containing volume
     * @return -- list of netapp luns
     * @throws NaException
     * @throws IOException
     */
    @Override
    public List<LunVO> listLunsOnFiler(String poolName) {
        if (s_logger.isDebugEnabled())
            s_logger.debug("Request --> listLunsOnFiler ");

        List<LunVO> luns = new ArrayList<LunVO>();

        List<NetappVolumeVO> vols = _volumeDao.listVolumes(poolName);

        for (NetappVolumeVO vol : vols) {
            luns.addAll(_lunDao.listLunsByVolId(vol.getId()));
        }

        if (s_logger.isDebugEnabled())
            s_logger.debug("Response --> listLunsOnFiler:success");

        return luns;
    }

    /**
     * This method disassociates a lun from the igroup on the filer
     * @param iGroup -- igroup name
     * @param lunName -- lun name
     */
    @Override
    public void disassociateLun(String iGroup, String lunName) throws ServerException, InvalidParameterValueException {
        NaElement xi;
        LunVO lun = _lunDao.findByName(lunName);

        if (lun == null)
            throw new InvalidParameterValueException("Cannot find LUN " + lunName);

        NetappVolumeVO vol = _volumeDao.findById(lun.getVolumeId());
        NaServer s = null;
        try {
            s = getServer(vol.getIpAddress(), vol.getUsername(), vol.getPassword());

            if (s_logger.isDebugEnabled())
                s_logger.debug("Request --> disassociateLun " + ":serverIp:" + vol.getIpAddress());

            xi = new NaElement("igroup-remove");
            xi.addNewChild("force", "true");
            xi.addNewChild("initiator", iGroup);
            xi.addNewChild("initiator-group-name", lunName);
            s.invokeElem(xi);

        } catch (UnknownHostException uhe) {
            throw new ServerException("Failed to disassociate lun", uhe);
        } catch (IOException ioe) {
            throw new ServerException("Failed to disassociate lun", ioe);
        } catch (NaException nae) {
            throw new ServerException("Failed to disassociate lun", nae);
        } finally {
            if (s != null)
                s.close();
        }

    }

    /**
     * This method associates a lun to a particular igroup
     * @param iqn
     * @param iGroup
     * @param lunName
     */
    @Override
    public String[] associateLun(String guestIqn, String lunName) throws ServerException, InvalidParameterValueException

    {
        NaElement xi2;

        //get lun id from path
        String[] splitLunName = lunName.split("-");
        String[] returnVal = new String[3];
        if (splitLunName.length != 2)
            throw new InvalidParameterValueException("The lun id is malformed");

        String lunIdStr = splitLunName[1];

        Long lId = new Long(lunIdStr);

        LunVO lun = _lunDao.findById(lId);

        if (lun == null)
            throw new InvalidParameterValueException("Cannot find LUN " + lunName);

        NetappVolumeVO vol = _volumeDao.findById(lun.getVolumeId());

        //assert(vol != null);

        returnVal[0] = lunIdStr;
        returnVal[1] = lun.getTargetIqn();
        returnVal[2] = vol.getIpAddress();

        NaServer s = null;

        try {
            s = getServer(vol.getIpAddress(), vol.getUsername(), vol.getPassword());

            if (s_logger.isDebugEnabled())
                s_logger.debug("Request --> associateLun " + ":serverIp:" + vol.getIpAddress());

            //add iqn to the group
            xi2 = new NaElement("igroup-add");
            xi2.addNewChild("force", "true");
            xi2.addNewChild("initiator", guestIqn);
            xi2.addNewChild("initiator-group-name", lunName);
            s.invokeElem(xi2);

            return returnVal;
        } catch (UnknownHostException uhe) {
            s_logger.warn("Unable to associate LUN ", uhe);
            throw new ServerException("Unable to associate LUN", uhe);
        } catch (NaAPIFailedException naf) {
            if (naf.getErrno() == 9008) { //initiator group already contains node
                return returnVal;
            }
            s_logger.warn("Unable to associate LUN ", naf);
            throw new ServerException("Unable to associate LUN", naf);
        } catch (NaException nae) {
            s_logger.warn("Unable to associate LUN ", nae);
            throw new ServerException("Unable to associate LUN", nae);
        } catch (IOException ioe) {
            s_logger.warn("Unable to associate LUN ", ioe);
            throw new ServerException("Unable to associate LUN", ioe);
        } finally {
            if (s != null)
                s.close();
        }

    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        _netappAllocator = new NetappDefaultAllocatorImpl(this);

        return true;
    }
}
