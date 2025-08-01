/*
 * IGinX - the polystore system with high performance
 * Copyright (C) Tsinghua University
 * TSIGinX@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package cn.edu.tsinghua.iginx.metadata.storage;

import cn.edu.tsinghua.iginx.metadata.cache.IMetaCache;
import cn.edu.tsinghua.iginx.metadata.entity.*;
import cn.edu.tsinghua.iginx.metadata.exception.MetaStorageException;
import cn.edu.tsinghua.iginx.metadata.hook.*;
import cn.edu.tsinghua.iginx.metadata.utils.ReshardStatus;
import cn.edu.tsinghua.iginx.transform.pojo.TriggerDescriptor;
import cn.edu.tsinghua.iginx.utils.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IMetaStorage {

  Map<Long, IginxMeta> loadIginx() throws MetaStorageException;

  IginxMeta registerIginx(IginxMeta iginx) throws MetaStorageException;

  void registerIginxChangeHook(IginxChangeHook hook);

  Map<Long, StorageEngineMeta> loadStorageEngine(List<StorageEngineMeta> storageEngines)
      throws MetaStorageException;

  long addStorageEngine(long iginxId, StorageEngineMeta storageEngine) throws MetaStorageException;

  void removeDummyStorageEngine(long iginxId, long storageEngineId, boolean forAllIginx)
      throws MetaStorageException;

  void registerStorageChangeHook(StorageChangeHook hook);

  void refreshAchievableIginx(IginxMeta iginx) throws MetaStorageException;

  Map<Long, Set<Long>> refreshClusterIginxConnectivity() throws MetaStorageException;

  void addStorageConnection(long iginxId, List<StorageEngineMeta> storageEngines)
      throws MetaStorageException;

  Map<Long, Set<Long>> refreshClusterStorageConnections() throws MetaStorageException;

  Map<String, StorageUnitMeta> loadStorageUnit() throws MetaStorageException;

  void lockStorageUnit() throws MetaStorageException;

  String addStorageUnit() throws MetaStorageException;

  void updateStorageUnit(StorageUnitMeta storageUnitMeta) throws MetaStorageException;

  void releaseStorageUnit() throws MetaStorageException;

  void registerStorageUnitChangeHook(StorageUnitChangeHook hook);

  Map<ColumnsInterval, List<FragmentMeta>> loadFragment() throws MetaStorageException;

  void lockFragment() throws MetaStorageException;

  List<FragmentMeta> getFragmentListByColumnNameAndKeyInterval(
      String columnName, KeyInterval keyInterval);

  Map<ColumnsInterval, List<FragmentMeta>> getFragmentMapByColumnsIntervalAndKeyInterval(
      ColumnsInterval columnsInterval, KeyInterval keyInterval);

  void updateFragment(FragmentMeta fragmentMeta) throws MetaStorageException;

  void updateFragmentByColumnsInterval(ColumnsInterval columnsInterval, FragmentMeta fragmentMeta)
      throws MetaStorageException;

  void removeFragment(FragmentMeta fragmentMeta) throws MetaStorageException;

  void addFragment(FragmentMeta fragmentMeta) throws MetaStorageException;

  void releaseFragment() throws MetaStorageException;

  void registerFragmentChangeHook(FragmentChangeHook hook);

  List<UserMeta> loadUser(UserMeta userMeta) throws MetaStorageException;

  void registerUserChangeHook(UserChangeHook hook);

  void addUser(UserMeta userMeta) throws MetaStorageException;

  void updateUser(UserMeta userMeta) throws MetaStorageException;

  void removeUser(String username) throws MetaStorageException;

  void registerTimeseriesChangeHook(TimeSeriesChangeHook hook);

  void registerVersionChangeHook(VersionChangeHook hook);

  boolean election();

  void updateTimeseriesData(Map<String, Double> timeseriesData, long iginxid, long version)
      throws Exception;

  Map<String, Double> getColumnsData();

  void registerPolicy(long iginxId, int num) throws Exception;

  int updateVersion();

  void registerTransformChangeHook(TransformChangeHook hook);

  List<TransformTaskMeta> loadTransformTask() throws MetaStorageException;

  void addTransformTask(TransformTaskMeta transformTask) throws MetaStorageException;

  void updateTransformTask(TransformTaskMeta transformTask) throws MetaStorageException;

  void dropTransformTask(String name) throws MetaStorageException;

  void registerJobTriggerChangeHook(JobTriggerChangeHook hook);

  void storeJobTrigger(TriggerDescriptor descriptor) throws MetaStorageException;

  void dropJobTrigger(String name) throws MetaStorageException;

  void updateJobTrigger(TriggerDescriptor jobTriggerDescriptor) throws MetaStorageException;

  List<TriggerDescriptor> loadJobTrigger() throws MetaStorageException;

  void updateTimeseriesLoad(Map<String, Long> timeseriesLoadMap) throws Exception;

  Map<String, Long> loadTimeseriesHeat() throws MetaStorageException, Exception;

  void removeTimeseriesHeat() throws MetaStorageException;

  void lockTimeseriesHeatCounter() throws MetaStorageException;

  void incrementTimeseriesHeatCounter() throws MetaStorageException;

  void resetTimeseriesHeatCounter() throws MetaStorageException;

  void releaseTimeseriesHeatCounter() throws MetaStorageException;

  int getTimeseriesHeatCounter() throws MetaStorageException;

  void updateFragmentRequests(
      Map<FragmentMeta, Long> writeRequestsMap, Map<FragmentMeta, Long> readRequestsMap)
      throws Exception;

  void removeFragmentRequests() throws MetaStorageException;

  void lockFragmentRequestsCounter() throws MetaStorageException;

  void incrementFragmentRequestsCounter() throws MetaStorageException;

  void resetFragmentRequestsCounter() throws MetaStorageException;

  void releaseFragmentRequestsCounter() throws MetaStorageException;

  int getFragmentRequestsCounter() throws MetaStorageException;

  Map<FragmentMeta, Long> loadFragmentPoints(IMetaCache cache) throws Exception;

  void deleteFragmentPoints(ColumnsInterval columnsInterval, KeyInterval keyInterval)
      throws Exception;

  void updateFragmentPoints(FragmentMeta fragmentMeta, long points) throws Exception;

  void updateFragmentHeat(
      Map<FragmentMeta, Long> writeHotspotMap, Map<FragmentMeta, Long> readHotspotMap)
      throws Exception;

  Pair<Map<FragmentMeta, Long>, Map<FragmentMeta, Long>> loadFragmentHeat(IMetaCache cache)
      throws Exception;

  void removeFragmentHeat() throws MetaStorageException;

  void lockFragmentHeatCounter() throws MetaStorageException;

  void incrementFragmentHeatCounter() throws MetaStorageException;

  void resetFragmentHeatCounter() throws MetaStorageException;

  void releaseFragmentHeatCounter() throws MetaStorageException;

  int getFragmentHeatCounter() throws MetaStorageException;

  boolean proposeToReshard() throws MetaStorageException;

  void lockReshardStatus() throws MetaStorageException;

  void updateReshardStatus(ReshardStatus status) throws MetaStorageException;

  void releaseReshardStatus() throws MetaStorageException;

  void removeReshardStatus() throws MetaStorageException;

  void registerReshardStatusHook(ReshardStatusChangeHook hook);

  void lockReshardCounter() throws MetaStorageException;

  void incrementReshardCounter() throws MetaStorageException;

  void resetReshardCounter() throws MetaStorageException;

  void releaseReshardCounter() throws MetaStorageException;

  void removeReshardCounter() throws MetaStorageException;

  void registerReshardCounterChangeHook(ReshardCounterChangeHook hook);

  void lockMaxActiveEndKeyStatistics() throws MetaStorageException;

  void addOrUpdateMaxActiveEndKeyStatistics(long endKey) throws MetaStorageException;

  long getMaxActiveEndKeyStatistics() throws MetaStorageException;

  void releaseMaxActiveEndKeyStatistics() throws MetaStorageException;

  void registerMaxActiveEndKeyStatisticsChangeHook(MaxActiveEndKeyStatisticsChangeHook hook)
      throws MetaStorageException;
}
