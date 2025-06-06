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
package cn.edu.tsinghua.iginx.integration.func.session;

import static cn.edu.tsinghua.iginx.engine.shared.Constants.WINDOW_END_COL;
import static cn.edu.tsinghua.iginx.engine.shared.Constants.WINDOW_START_COL;
import static cn.edu.tsinghua.iginx.integration.controller.Controller.SUPPORT_KEY;
import static cn.edu.tsinghua.iginx.thrift.StorageEngineType.influxdb;
import static cn.edu.tsinghua.iginx.thrift.StorageEngineType.neo4j;
import static org.junit.Assert.*;

import cn.edu.tsinghua.iginx.integration.controller.Controller;
import cn.edu.tsinghua.iginx.integration.tool.ConfLoader;
import cn.edu.tsinghua.iginx.session_v2.*;
import cn.edu.tsinghua.iginx.session_v2.annotations.Field;
import cn.edu.tsinghua.iginx.session_v2.annotations.Measurement;
import cn.edu.tsinghua.iginx.session_v2.domain.ClusterInfo;
import cn.edu.tsinghua.iginx.session_v2.domain.User;
import cn.edu.tsinghua.iginx.session_v2.query.*;
import cn.edu.tsinghua.iginx.session_v2.write.Point;
import cn.edu.tsinghua.iginx.session_v2.write.Record;
import cn.edu.tsinghua.iginx.session_v2.write.Table;
import cn.edu.tsinghua.iginx.thrift.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SessionV2IT {

  private static final long startKey = 0L;
  private static final long endKey = 10000L;
  private static final long SLEEP_TIME = 1000L;
  private static IginXClient iginXClient;
  private static WriteClient writeClient;
  private static AsyncWriteClient asyncWriteClient;
  private static DeleteClient deleteClient;
  private static QueryClient queryClient;
  private static UsersClient usersClient;
  private static ClusterClient clusterClient;

  private static boolean isInfluxdb = false;

  private static boolean isNeo4j = false;

  private static boolean needCompareResult = true;

  @BeforeClass
  public static void setUp() {
    ConfLoader conf = new ConfLoader(Controller.CONFIG_FILE);
    if (StorageEngineType.valueOf(conf.getStorageType(false).toLowerCase()) == influxdb) {
      isInfluxdb = true;
    }
    if (StorageEngineType.valueOf(conf.getStorageType(false).toLowerCase()) == neo4j) {
      isNeo4j = true;
    }
    if (!SUPPORT_KEY.get(conf.getStorageType()) && conf.isScaling()) {
      needCompareResult = false;
    }
    iginXClient = IginXClientFactory.create("127.0.0.1", 6888);

    writeClient = iginXClient.getWriteClient();
    asyncWriteClient = iginXClient.getAsyncWriteClient();
    deleteClient = iginXClient.getDeleteClient();
    queryClient = iginXClient.getQueryClient();
    usersClient = iginXClient.getUserClient();
    clusterClient = iginXClient.getClusterClient();

    //        insertDataByPoints();
    //        asyncInsertDataByPoints();
    //
    //        insertDataByRecords();
    //        asyncInsertDataByRecords();
    //
    insertDataByTable();
    asyncInsertDataByTable();
    insertTagKVDataByTable();
    //
    //        insertDataByMeasurements();
    //        asyncInsertDataByMeasurements();
  }

  @AfterClass
  public static void tearDown() {
    clearDataAndUser();
    iginXClient.close();
  }

  private static List<Point> buildInsertDataPoints() {
    List<Point> points = new ArrayList<>();
    for (long i = startKey; i < endKey; i++) {
      points.add(
          Point.builder()
              .key(i)
              .measurement("test.session.v2.bool")
              .booleanValue(i % 2 == 0)
              .build());
      points.add(
          Point.builder().key(i).measurement("test.session.v2.int").intValue((int) i).build());
      points.add(Point.builder().key(i).measurement("test.session.v2.long").longValue(i).build());
      points.add(
          Point.builder()
              .key(i)
              .measurement("test.session.v2.float")
              .floatValue((float) (i + 0.1))
              .build());
      points.add(
          Point.builder()
              .key(i)
              .measurement("test.session.v2.double")
              .doubleValue(i + 0.2)
              .build());
      if (i % 2 == 0) {
        points.add(
            Point.builder()
                .key(i)
                .measurement("test.session.v2.string")
                .binaryValue(String.valueOf(i).getBytes())
                .build());
      }
    }
    return points;
  }

  private static void insertDataByPoints() {
    List<Point> points = buildInsertDataPoints();
    writeClient.writePoints(points);
  }

  private static void asyncInsertDataByPoints() {
    try {
      List<Point> points = buildInsertDataPoints();
      asyncWriteClient.writePoints(points);
      Thread.sleep(SLEEP_TIME);
    } catch (InterruptedException e) {
      fail(e.getMessage());
    }
  }

  private static List<Record> buildInsertDataRecords() {
    List<Record> records = new ArrayList<>();
    for (long i = startKey; i < endKey; i++) {
      Record.Builder builder =
          Record.builder()
              .measurement("test.session.v2")
              .key(i)
              .addBooleanField("bool", i % 2 == 0)
              .addLongField("long", i)
              .addFloatField("float", (float) (i + 0.1))
              .addDoubleField("double", i + 0.2)
              .addIntField("int", (int) i);
      if (i % 2 == 0) {
        builder.addBinaryField("string", String.valueOf(i).getBytes());
      }
      records.add(builder.build());
    }
    return records;
  }

  private static void insertDataByRecords() {
    List<Record> records = buildInsertDataRecords();
    writeClient.writeRecords(records);
  }

  private static void asyncInsertDataByRecords() {
    try {
      List<Record> records = buildInsertDataRecords();
      asyncWriteClient.writeRecords(records);
      Thread.sleep(SLEEP_TIME);
    } catch (InterruptedException e) {
      fail(e.getMessage());
    }
  }

  private static Table buildInsertDataTable() {
    Table.Builder builder =
        Table.builder()
            .measurement("test.session.v2")
            .addField("bool", DataType.BOOLEAN)
            .addField("int", DataType.INTEGER)
            .addField("long", DataType.LONG)
            .addField("float", DataType.FLOAT)
            .addField("double", DataType.DOUBLE)
            .addField("string", DataType.BINARY);

    for (long i = startKey; i < endKey; i++) {
      if (i % 2 == 0) {
        builder = builder.binaryValue("string", String.valueOf(i).getBytes());
      }
      builder =
          builder
              .key(i)
              .boolValue("bool", i % 2 == 0)
              .intValue("int", (int) i)
              .longValue("long", i)
              .floatValue("float", (float) (i + 0.1))
              .doubleValue("double", i + 0.2)
              .next();
    }

    return builder.build();
  }

  private static void insertDataByTable() {
    Table table = buildInsertDataTable();
    writeClient.writeTable(table);
  }

  private static void asyncInsertDataByTable() {
    try {
      Table table = buildInsertDataTable();
      asyncWriteClient.writeTable(table);
      Thread.sleep(SLEEP_TIME);
    } catch (InterruptedException e) {
      fail(e.getMessage());
    }
  }

  private static Table buildInsertTagKVDataTable() {
    Table.Builder builder =
        Table.builder()
            .measurement("test.session.v3")
            .addField("bool", DataType.BOOLEAN, Collections.singletonMap("k1", "v1"))
            .addField("int", DataType.INTEGER, Collections.singletonMap("k1", "v2"))
            .addField("long", DataType.LONG, Collections.singletonMap("k1", "v3"))
            .addField("float", DataType.FLOAT, Collections.singletonMap("k1", "v4"))
            .addField("double", DataType.DOUBLE, Collections.singletonMap("k1", "v5"))
            .addField("string", DataType.BINARY, Collections.singletonMap("k1", "v6"));

    for (long i = startKey; i < endKey; i++) {
      if (i % 2 == 0) {
        builder = builder.binaryValue("string", String.valueOf(i).getBytes());
      }
      builder =
          builder
              .key(i)
              .boolValue("bool", i % 2 == 0)
              .intValue("int", (int) i)
              .longValue("long", i)
              .floatValue("float", (float) (i + 0.1))
              .doubleValue("double", i + 0.2)
              .next();
    }

    return builder.build();
  }

  private static void insertTagKVDataByTable() {
    Table table = buildInsertTagKVDataTable();
    writeClient.writeTable(table);
  }

  private static List<POJO> buildInsertMeasurements() {
    List<POJO> measurements = new ArrayList<>();
    for (long i = startKey; i < endKey; i++) {
      byte[] binaryValue = null;
      if (i % 2 == 0) {
        binaryValue = String.valueOf(i).getBytes();
      }
      measurements.add(
          new POJO(i, i % 2 == 0, (int) i, i, (float) (i + 0.1), i + 0.2, binaryValue));
    }
    return measurements;
  }

  private static void insertDataByMeasurements() {
    List<POJO> measurements = buildInsertMeasurements();
    writeClient.writeMeasurements(measurements);
  }

  private static void asyncInsertDataByMeasurements() {
    try {
      List<POJO> measurements = buildInsertMeasurements();
      asyncWriteClient.writeMeasurements(measurements);
      Thread.sleep(SLEEP_TIME);
    } catch (InterruptedException e) {
      fail(e.getMessage());
    }
  }

  private static void clearDataAndUser() {
    List<User> users = usersClient.findUsers();
    users.forEach(
        user -> {
          if (!user.getUsername().equals("root")) {
            usersClient.removeUser(user.getUsername());
          }
        });
    // deleteClient.deleteMeasurement("*");
  }

  @Test
  public void testSimpleQuery() {
    IginXTable table =
        queryClient.query(
            SimpleQuery.builder()
                .addMeasurement("test.session.v2.*")
                .startKey(endKey - 1000L)
                .endKey(endKey)
                .build());

    if (!needCompareResult) {
      return;
    }
    assertNotNull(table);
    IginXHeader header = table.getHeader();
    assertTrue(header.hasTimestamp());
    List<IginXColumn> columns = header.getColumns();
    assertEquals(6, columns.size());
    for (IginXColumn column : columns) {
      switch (column.getName()) {
        case "test.session.v2.bool":
          assertEquals(DataType.BOOLEAN, column.getDataType());
          break;
        case "test.session.v2.int":
          if (isInfluxdb || isNeo4j) break;
          assertEquals(DataType.INTEGER, column.getDataType());
          break;
        case "test.session.v2.double":
          assertEquals(DataType.DOUBLE, column.getDataType());
          break;
        case "test.session.v2.float":
          if (isInfluxdb || isNeo4j) break;
          assertEquals(DataType.FLOAT, column.getDataType());
          break;
        case "test.session.v2.long":
          assertEquals(DataType.LONG, column.getDataType());
          break;
        case "test.session.v2.string":
          assertEquals(DataType.BINARY, column.getDataType());
          break;
        default:
          fail();
      }
    }

    List<IginXRecord> records = table.getRecords();
    assertEquals(1000, records.size());
    for (int i = 0; i < records.size(); i++) {
      IginXRecord record = records.get(i);
      long timestamp = endKey - 1000 + i;
      assertEquals(timestamp, record.getKey());
      // 核验 bool 值
      boolean boolValue = (boolean) record.getValue("test.session.v2.bool");
      assertEquals(timestamp % 2 == 0, boolValue);
      // 核验 int 值
      if (isInfluxdb || isNeo4j) {
        long intValue = (long) record.getValue("test.session.v2.int");
        assertEquals((long) timestamp, intValue);
      } else {
        int intValue = (int) record.getValue("test.session.v2.int");
        assertEquals((int) timestamp, intValue);
      }
      // 核验 long 值
      long longValue = (long) record.getValue("test.session.v2.long");
      assertEquals(timestamp, longValue);
      // 核验 float 值
      if (isInfluxdb || isNeo4j) {
        double floatValue = (double) record.getValue("test.session.v2.float");
        assertEquals((double) (timestamp + 0.1), floatValue, 0.05);
      } else {
        float floatValue = (float) record.getValue("test.session.v2.float");
        assertEquals((float) (timestamp + 0.1), floatValue, 0.05);
      }
      // 核验 double 值
      double doubleValue = (double) record.getValue("test.session.v2.double");
      assertEquals(timestamp + 0.2, doubleValue, 0.05);
      // 核验 string 值
      Object object = record.getValue("test.session.v2.string");
      if (timestamp % 2 == 0) {
        byte[] binaryValue = (byte[]) object;
        assertEquals(String.valueOf(timestamp), new String(binaryValue));
      } else {
        assertNull(object);
      }
    }
  }

  @Test
  public void testTagKV() {
    IginXTable table =
        queryClient.query(
            SimpleQuery.builder()
                .addMeasurement("test.session.v3.*")
                .startKey(endKey - 1000L)
                .endKey(endKey)
                .addTagsList(
                    Stream.of(
                            new HashMap<String, List<String>>() {
                              {
                                put("k1", Collections.singletonList("v1"));
                              }
                            },
                            new HashMap<String, List<String>>() {
                              {
                                put("k1", Collections.singletonList("v3"));
                              }
                            },
                            new HashMap<String, List<String>>() {
                              {
                                put("k1", Collections.singletonList("v5"));
                              }
                            })
                        .collect(Collectors.toList()))
                .build());
    if (!needCompareResult) {
      return;
    }

    assertNotNull(table);

    IginXHeader header = table.getHeader();
    assertTrue(header.hasTimestamp());

    List<IginXColumn> columns = header.getColumns();
    assertEquals(3, columns.size());
    for (IginXColumn column : columns) {
      switch (column.getName()) {
        case "test.session.v3.bool{k1=v1}":
          assertEquals(DataType.BOOLEAN, column.getDataType());
          break;
        case "test.session.v3.long{k1=v3}":
          assertEquals(DataType.LONG, column.getDataType());
          break;
        case "test.session.v3.double{k1=v5}":
          assertEquals(DataType.DOUBLE, column.getDataType());
          break;
        default:
          fail();
      }
    }

    List<IginXRecord> records = table.getRecords();
    assertEquals(1000, records.size());
    for (int i = 0; i < records.size(); i++) {
      IginXRecord record = records.get(i);
      long timestamp = endKey - 1000 + i;
      assertEquals(timestamp, record.getKey());
      // 核验 bool 值
      boolean boolValue = (boolean) record.getValue("test.session.v3.bool{k1=v1}");
      assertEquals(timestamp % 2 == 0, boolValue);
      // 核验 long 值
      long longValue = (long) record.getValue("test.session.v3.long{k1=v3}");
      assertEquals(timestamp, longValue);
      // 核验 double 值
      double doubleValue = (double) record.getValue("test.session.v3.double{k1=v5}");
      assertEquals(timestamp + 0.2, doubleValue, 0.05);
    }

    table =
        queryClient.query(
            SimpleQuery.builder()
                .addMeasurement("test.session.v3.*")
                .startKey(endKey - 1000L)
                .endKey(endKey)
                .addTagsList(
                    Stream.of(
                            new HashMap<String, List<String>>() {
                              {
                                put("k1", Collections.singletonList("v2"));
                              }
                            },
                            new HashMap<String, List<String>>() {
                              {
                                put("k1", Collections.singletonList("v4"));
                              }
                            },
                            new HashMap<String, List<String>>() {
                              {
                                put("k1", Collections.singletonList("v6"));
                              }
                            })
                        .collect(Collectors.toList()))
                .build());
    if (!needCompareResult) {
      return;
    }
    assertNotNull(table);

    header = table.getHeader();
    assertTrue(header.hasTimestamp());

    columns = header.getColumns();
    assertEquals(3, columns.size());
    for (IginXColumn column : columns) {
      switch (column.getName()) {
        case "test.session.v3.int{k1=v2}":
          if (isInfluxdb || isNeo4j) break;
          assertEquals(DataType.INTEGER, column.getDataType());
          break;
        case "test.session.v3.float{k1=v4}":
          if (isInfluxdb || isNeo4j) break;
          assertEquals(DataType.FLOAT, column.getDataType());
          break;
        case "test.session.v3.string{k1=v6}":
          assertEquals(DataType.BINARY, column.getDataType());
          break;
        default:
          fail();
      }
    }

    records = table.getRecords();
    assertEquals(1000, records.size());
    for (int i = 0; i < records.size(); i++) {
      IginXRecord record = records.get(i);
      long timestamp = endKey - 1000 + i;
      assertEquals(timestamp, record.getKey());
      // 核验 int 值
      if (isInfluxdb || isNeo4j) {
        long intValue = (long) record.getValue("test.session.v3.int{k1=v2}");
        assertEquals((long) timestamp, intValue);
      } else {
        int intValue = (int) record.getValue("test.session.v3.int{k1=v2}");
        assertEquals((int) timestamp, intValue);
      }
      // 核验 float 值
      if (isInfluxdb || isNeo4j) {
        double floatValue = (double) record.getValue("test.session.v3.float{k1=v4}");
        assertEquals((double) (timestamp + 0.1), floatValue, 0.05);
      } else {
        float floatValue = (float) record.getValue("test.session.v3.float{k1=v4}");
        assertEquals((float) (timestamp + 0.1), floatValue, 0.05);
      }
      // 核验 string 值
      Object object = record.getValue("test.session.v3.string{k1=v6}");
      if (timestamp % 2 == 0) {
        byte[] binaryValue = (byte[]) object;
        assertEquals(String.valueOf(timestamp), new String(binaryValue));
      } else {
        assertNull(object);
      }
    }
  }

  @Test
  public void testAggregateQuery() {
    Query query =
        AggregateQuery.builder()
            .addMeasurements(new HashSet<>(Collections.singletonList("test.session.v2.*")))
            .aggregate(AggregateType.COUNT)
            .startKey(startKey)
            .endKey(endKey)
            .build();
    IginXTable table = queryClient.query(query);
    if (!needCompareResult) {
      return;
    }
    assertNotNull(table);
    IginXHeader header = table.getHeader();
    assertFalse(header.hasTimestamp());
    List<IginXColumn> columns = header.getColumns();
    assertEquals(6, columns.size());
    for (IginXColumn column : columns) {
      switch (column.getName()) {
        case "count(test.session.v2.bool)":
        case "count(test.session.v2.int)":
        case "count(test.session.v2.double)":
        case "count(test.session.v2.float)":
        case "count(test.session.v2.long)":
        case "count(test.session.v2.string)":
          assertEquals(DataType.LONG, column.getDataType());
          break;
        default:
          fail();
      }
    }
    List<IginXRecord> records = table.getRecords();
    assertEquals(1, records.size());

    IginXRecord record = records.get(0);
    for (Map.Entry<String, Object> entry : record.getValues().entrySet()) {
      long value = (long) entry.getValue();
      if (entry.getKey().equals("count(test.session.v2.string)")) {
        assertEquals((endKey - startKey) / 2, value);
      } else {
        assertEquals(endKey - startKey, value);
      }
    }
  }

  @Test
  public void testLastQuery() {
    Query query =
        LastQuery.builder()
            .addMeasurements(
                new HashSet<>(Arrays.asList("test.session.v2.string", "test.session.v2.int")))
            .startKey(startKey)
            .build();

    IginXTable table = queryClient.query(query);
    if (!needCompareResult) {
      return;
    }
    assertNotNull(table);
    IginXHeader header = table.getHeader();
    assertTrue(header.hasTimestamp());

    List<IginXRecord> records = table.getRecords();
    assertEquals(2, records.size());

    for (IginXRecord record : records) {
      String value = new String((byte[]) record.getValue("value"));
      if ((new String((byte[]) record.getValue("path"))).equals("test.session.v2.string")) {
        assertEquals(endKey - 2, record.getKey());
        assertEquals(String.valueOf(endKey - 2), value);
      } else if ((new String((byte[]) record.getValue("path"))).equals("test.session.v2.int")) {
        assertEquals(endKey - 1, record.getKey());
        assertEquals(String.valueOf(endKey - 1), value);
      } else {
        fail();
      }
    }
  }

  @Test
  public void testDownsampleQuery() {
    Query query =
        DownsampleQuery.builder()
            .addMeasurement("test.session.v2.long")
            .addMeasurement("test.session.v2.double")
            .aggregate(AggregateType.SUM)
            .precision((endKey - startKey) / 10)
            .startKey(startKey)
            .endKey(endKey + (endKey - startKey))
            .build();
    executeDownsampleQuery(query);
  }

  @Test
  public void testDownsampleQueryNoInterval() {
    Query query =
        DownsampleQuery.builder()
            .addMeasurement("test.session.v2.long")
            .addMeasurement("test.session.v2.double")
            .aggregate(AggregateType.SUM)
            .precision((endKey - startKey) / 10)
            .build();
    executeDownsampleQuery(query);
  }

  private void executeDownsampleQuery(Query query) {
    IginXTable table = queryClient.query(query);
    if (!needCompareResult) {
      return;
    }
    assertNotNull(table);
    IginXHeader header = table.getHeader();
    assertTrue(header.hasTimestamp());
    List<IginXColumn> columns = header.getColumns();
    assertEquals(4, columns.size());
    for (IginXColumn column : columns) {
      switch (column.getName()) {
        case "sum(test.session.v2.long)":
          assertEquals(DataType.LONG, column.getDataType());
          break;
        case "sum(test.session.v2.double)":
          assertEquals(DataType.DOUBLE, column.getDataType());
          break;
        case WINDOW_START_COL:
        case WINDOW_END_COL:
          break;
        default:
          fail();
      }
    }
    List<IginXRecord> records = table.getRecords();
    assertEquals(10, records.size());
    for (IginXRecord record : records) {
      long timestamp = record.getKey();
      if (timestamp >= endKey) {
        fail();
      } else {
        long nextTimestamps = timestamp + (endKey - startKey) / 10;
        long longSum = (nextTimestamps + timestamp - 1) * (endKey - startKey) / 20;
        double doubleSum = longSum + 0.2 * (endKey - startKey) / 10.0;
        assertEquals(longSum, record.getValue("sum(test.session.v2.long)"));
        assertEquals(doubleSum, (double) record.getValue("sum(test.session.v2.double)"), 0.01);
      }
    }
  }

  @Test
  public void testMeasurementQuery() {
    if (isInfluxdb || isNeo4j) return;
    List<POJO> pojoList =
        queryClient.query(
            SimpleQuery.builder()
                .addMeasurement("test.session.v2.*")
                .startKey(endKey - 1000L)
                .endKey(endKey)
                .build(),
            POJO.class);
    if (!needCompareResult) {
      return;
    }
    assertEquals(1000, pojoList.size());
    for (int i = 0; i < pojoList.size(); i++) {
      POJO pojo = pojoList.get(i);
      long timestamp = endKey - 1000 + i;
      assertEquals(timestamp, pojo.timestamp);
      assertEquals(timestamp % 2 == 0, pojo.boolValue);
      assertEquals((int) timestamp, pojo.intValue);
      assertEquals(timestamp, pojo.longValue);
      assertEquals((float) (timestamp + 0.1), pojo.floatValue, 0.05);
      assertEquals(timestamp + 0.2, pojo.doubleValue, 0.05);
      if (timestamp % 2 == 0) {
        assertEquals(String.valueOf(timestamp), new String(pojo.binaryValue));
      } else {
        assertNull(pojo.binaryValue);
      }
    }
  }

  @Test
  public void testClusterInfo() {
    int expectedReplicaNum = 1;
    assertEquals(expectedReplicaNum, clusterClient.getReplicaNum());

    IginxInfo info = new IginxInfo();
    ClusterInfo actualInfo = clusterClient.getClusterInfo();
    System.out.println();
  }

  @Test
  public void testUserClient() {
    Set<AuthType> fullAuth =
        new HashSet<>(
            Arrays.asList(AuthType.Read, AuthType.Write, AuthType.Admin, AuthType.Cluster));
    Set<AuthType> rdsAuth =
        new HashSet<>(Arrays.asList(AuthType.Read, AuthType.Write, AuthType.Admin));
    Set<AuthType> rdAuth = new HashSet<>(Arrays.asList(AuthType.Read, AuthType.Write));

    List<User> actualUsers = usersClient.findUsers();
    List<User> expectedUsers =
        new ArrayList<>(
            Collections.singletonList(new User("root", "root", UserType.Administrator, fullAuth)));
    assertEqualUsers(expectedUsers, actualUsers);

    // test add user
    User user1 = new User("user1", "user1", UserType.OrdinaryUser, rdAuth);
    User user2 = new User("user2", "user2", UserType.OrdinaryUser, rdsAuth);
    usersClient.addUser(user1);
    usersClient.addUser(user2);

    expectedUsers.add(user1);
    expectedUsers.add(user2);
    actualUsers = usersClient.findUsers();
    assertEqualUsers(expectedUsers, actualUsers);

    // test update user
    expectedUsers.remove(user2);

    user2 = new User("user2", "user2", UserType.OrdinaryUser, fullAuth);
    usersClient.updateUser(user2);

    expectedUsers.add(user2);
    actualUsers = usersClient.findUsers();
    assertEqualUsers(expectedUsers, actualUsers);

    // test remove user
    usersClient.removeUser(user1.getUsername());

    expectedUsers.remove(user1);
    actualUsers = usersClient.findUsers();
    assertEqualUsers(expectedUsers, actualUsers);

    // test find user by name
    User user = usersClient.findUserByName("user2");
    assertEqualUser(user1, user);
  }

  private void assertEqualUsers(List<User> users1, List<User> users2) {
    if (!isEqualUsers(users1, users2)) {
      fail();
    }
  }

  private void assertEqualUser(User user1, User user2) {
    if (isEqualUser(user1, user2)) {
      fail();
    }
  }

  private boolean isEqualUsers(List<User> users1, List<User> users2) {
    if (users1 == null) return users2 == null;
    if (users2 == null) return false;
    if (users1.size() != users2.size()) return false;

    for (User user1 : users1) {
      boolean isMatch = false;
      for (User user2 : users2) {
        if (user1.getUsername().equals(user2.getUsername())) {
          isMatch = true;
          if (!isEqualUser(user1, user2)) {
            return false;
          }
        }
      }
      if (!isMatch) {
        return false;
      }
    }
    return true;
  }

  private boolean isEqualUser(User user1, User user2) {
    if (user1 == null) return user2 == null;
    if (user2 == null) return false;

    if (!user1.getUsername().equals(user2.getUsername())) return false;
    if (!user1.getUserType().equals(user2.getUserType())) return false;

    return user1.getAuths().containsAll(user2.getAuths())
        && user2.getAuths().containsAll(user1.getAuths());
  }

  @Measurement(name = "test.session.v2")
  public static class POJO {

    @Field(timestamp = true)
    long timestamp;

    @Field(name = "bool")
    boolean boolValue;

    @Field(name = "int")
    int intValue;

    @Field(name = "long")
    long longValue;

    @Field(name = "float")
    float floatValue;

    @Field(name = "double")
    double doubleValue;

    @Field(name = "string")
    byte[] binaryValue;

    public POJO() {}

    public POJO(
        long timestamp,
        boolean boolValue,
        int intValue,
        long longValue,
        float floatValue,
        double doubleValue,
        byte[] binaryValue) {
      this.timestamp = timestamp;
      this.boolValue = boolValue;
      this.intValue = intValue;
      this.longValue = longValue;
      this.floatValue = floatValue;
      this.doubleValue = doubleValue;
      this.binaryValue = binaryValue;
    }
  }
}
