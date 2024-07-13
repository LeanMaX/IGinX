/*
 * IGinX - the polystore system with high performance
 * Copyright (C) Tsinghua University
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cn.edu.tsinghua.iginx.integration.tpch;

import static cn.edu.tsinghua.iginx.integration.controller.Controller.clearAllData;
import static cn.edu.tsinghua.iginx.integration.tpch.TPCHRegressionIT.FieldType.DATE;
import static cn.edu.tsinghua.iginx.integration.tpch.TPCHRegressionIT.FieldType.NUM;
import static cn.edu.tsinghua.iginx.integration.tpch.TPCHRegressionIT.FieldType.STR;
import static org.junit.Assert.fail;

import cn.edu.tsinghua.iginx.exception.SessionException;
import cn.edu.tsinghua.iginx.integration.controller.Controller;
import cn.edu.tsinghua.iginx.integration.tool.ConfLoader;
import cn.edu.tsinghua.iginx.session.Session;
import cn.edu.tsinghua.iginx.session.SessionExecuteSqlResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TPCHRegressionIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(TPCHRegressionIT.class);

  // host info
  protected static String defaultTestHost = "127.0.0.1";
  protected static int defaultTestPort = 6888;
  protected static String defaultTestUser = "root";
  protected static String defaultTestPass = "root";

  // .tbl文件所在目录
  private static final String dataDir =
      System.getProperty("user.dir") + "/../tpc/TPC-H V3.0.1/data";

  // udf文件所在目录
  private static final String udfDir = "src/test/resources/tpch/udf/";

  // 最大重复测试次数
  private static int MAX_REPETITIONS_NUM;

  // 回归阈值
  private static double REGRESSION_THRESHOLD;

  protected static Session session;

  enum FieldType {
    NUM,
    STR,
    DATE
  }

  public TPCHRegressionIT() {
    ConfLoader conf = new ConfLoader(Controller.CONFIG_FILE);
    MAX_REPETITIONS_NUM = conf.getMaxRepetitionsNum();
    REGRESSION_THRESHOLD = conf.getRegressionThreshold();
  }

  @BeforeClass
  public static void setUp() throws SessionException {
    session = new Session(defaultTestHost, defaultTestPort, defaultTestUser, defaultTestPass);
    session.openSession();
  }

  @AfterClass
  public static void tearDown() throws SessionException {
    clearAllData(session);
    session.closeSession();
  }

  @Before
  public void prepare() {
    List<String> tableList =
        Arrays.asList(
            "region", "nation", "supplier", "part", "partsupp", "customer", "orders", "lineitem");

    List<List<String>> fieldsList = new ArrayList<>();
    List<List<FieldType>> typesList = new ArrayList<>();

    // region表
    fieldsList.add(Arrays.asList("r_regionkey", "r_name", "r_comment"));
    typesList.add(Arrays.asList(NUM, STR, STR));

    // nation表
    fieldsList.add(Arrays.asList("n_nationkey", "n_name", "n_regionkey", "n_comment"));
    typesList.add(Arrays.asList(NUM, STR, NUM, STR));

    // supplier表
    fieldsList.add(
        Arrays.asList(
            "s_suppkey",
            "s_name",
            "s_address",
            "s_nationkey",
            "s_phone",
            "s_acctbal",
            "s_comment"));
    typesList.add(Arrays.asList(NUM, STR, STR, NUM, STR, NUM, STR));

    // part表
    fieldsList.add(
        Arrays.asList(
            "p_partkey",
            "p_name",
            "p_mfgr",
            "p_brand",
            "p_type",
            "p_size",
            "p_container",
            "p_retailprice",
            "p_comment"));
    typesList.add(Arrays.asList(NUM, STR, STR, STR, STR, NUM, STR, NUM, STR));

    // partsupp表
    fieldsList.add(
        Arrays.asList("ps_partkey", "ps_suppkey", "ps_availqty", "ps_supplycost", "ps_comment"));
    typesList.add(Arrays.asList(NUM, NUM, NUM, NUM, STR));

    // customer表
    fieldsList.add(
        Arrays.asList(
            "c_custkey",
            "c_name",
            "c_address",
            "c_nationkey",
            "c_phone",
            "c_acctbal",
            "c_mktsegment",
            "c_comment"));
    typesList.add(Arrays.asList(NUM, STR, STR, NUM, STR, NUM, STR, STR));

    // orders表
    fieldsList.add(
        Arrays.asList(
            "o_orderkey",
            "o_custkey",
            "o_orderstatus",
            "o_totalprice",
            "o_orderdate",
            "o_orderpriority",
            "o_clerk",
            "o_shippriority",
            "o_comment"));
    typesList.add(Arrays.asList(NUM, NUM, STR, NUM, DATE, STR, STR, NUM, STR));

    // lineitem表
    fieldsList.add(
        Arrays.asList(
            "l_orderkey",
            "l_partkey",
            "l_suppkey",
            "l_linenumber",
            "l_quantity",
            "l_extendedprice",
            "l_discount",
            "l_tax",
            "l_returnflag",
            "l_linestatus",
            "l_shipdate",
            "l_commitdate",
            "l_receiptdate",
            "l_shipinstruct",
            "l_shipmode",
            "l_comment"));
    typesList.add(
        Arrays.asList(
            NUM, NUM, NUM, NUM, NUM, NUM, NUM, NUM, STR, STR, DATE, DATE, DATE, STR, STR, STR));

    // 插入数据
    for (int i = 0; i < 8; i++) {
      insertTable(tableList.get(i), fieldsList.get(i), typesList.get(i));
    }

    List<List<String>> UDFInfos = new ArrayList<>();
    UDFInfos.add(Arrays.asList("UDTF", "extractYear", "UDFExtractYear", "udtf_extract_year.py"));
    // 注册UDF函数
    for (List<String> UDFInfo : UDFInfos) {
      registerUDF(UDFInfo);
    }
  }

  private void insertTable(String table, List<String> fields, List<FieldType> types) {
    StringBuilder builder = new StringBuilder("INSERT INTO ");
    builder.append(table);
    builder.append("(key, ");
    for (String field : fields) {
      builder.append(field);
      builder.append(", ");
    }
    builder.setLength(builder.length() - 2);
    builder.append(") VALUES ");
    String insertPrefix = builder.toString();

    long count = 0;
    try (BufferedReader br =
        new BufferedReader(new FileReader(String.format("%s/%s.tbl", dataDir, table)))) {
      StringBuilder sb = new StringBuilder(insertPrefix);
      String line;
      while ((line = br.readLine()) != null) {
        String[] items = line.split("\\|");
        sb.append("(");
        sb.append(count); // 插入自增key列
        count++;
        sb.append(", ");
        assert fields.size() == items.length;
        for (int i = 0; i < items.length; i++) {
          switch (types.get(i)) {
            case NUM:
              sb.append(items[i]);
              sb.append(", ");
              break;
            case STR: // 字符串类型在外面需要包一层引号
              sb.append("\"");
              sb.append(items[i]);
              sb.append("\", ");
              break;
            case DATE: // 日期类型需要转为时间戳
              SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
              dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
              long time = dateFormat.parse(items[i]).getTime();
              sb.append(time);
              sb.append(", ");
              break;
            default:
              break;
          }
        }
        sb.setLength(sb.length() - 2);
        sb.append("), ");

        // 每次最多插入10000条数据
        if (count % 10000 == 0) {
          sb.setLength(sb.length() - 2);
          sb.append(";");
          session.executeSql(sb.toString());
          sb = new StringBuilder(insertPrefix);
        }
      }
      // 插入剩余数据
      if (sb.length() != insertPrefix.length()) {
        sb.setLength(sb.length() - 2);
        sb.append(";");
        session.executeSql(sb.toString());
      }
      LOGGER.info("Insert {} records into table [{}].", count, table);
    } catch (IOException | ParseException | SessionException e) {
      LOGGER.error("Insert into table {} fail. Caused by:", table, e);
      fail();
    }
  }

  private void registerUDF(List<String> UDFInfo) {
    String SINGLE_UDF_REGISTER_SQL = "CREATE FUNCTION %s \"%s\" FROM \"%s\" IN \"%s\";";
    File udfFile = new File(udfDir + UDFInfo.get(3));
    String register =
        String.format(
            SINGLE_UDF_REGISTER_SQL,
            UDFInfo.get(0),
            UDFInfo.get(1),
            UDFInfo.get(2),
            udfFile.getAbsolutePath());
    try {
      LOGGER.info("Execute register UDF statement: {}", register);
      session.executeRegisterTask(register, false);
    } catch (SessionException e) {
      LOGGER.error("Statement: \"{}\" execute fail. Caused by:", register, e);
      fail();
    }
  }

  @After
  public void clearData() throws SessionException {
    session.executeSql("CLEAR DATA;");
  }

  @Test
  public void test() {
    try {
      // 获取当前JVM的Runtime实例
      Runtime runtime = Runtime.getRuntime();
      // 执行垃圾回收，尽量释放内存
      runtime.gc();
      // 获取执行语句前的内存使用情况
      long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
      long startTime;
      // 13有问题
      List<Integer> queryIds = Arrays.asList(1, 2, 3, 5, 6, 9, 10, 16, 17, 18, 19, 20);
      for (int queryId : queryIds) {
        // read from sql file
        String sqlString =
            readSqlFileAsString("src/test/resources/tpch/queries/q" + queryId + ".sql");
        // 开始 tpc-h 查询
        System.out.println("start tpc-h query " + queryId);
        startTime = System.currentTimeMillis();
        // 执行查询语句, split by ; 最后一句为执行结果
        SessionExecuteSqlResult result = null;
        String[] sqls = sqlString.split(";");
        for (String sql : sqls) {
          if (sql.trim().isEmpty()) {
            continue;
          }
          sql += ";";
          try {
            result = session.executeSql(sql);
          } catch (SessionException e) {
            LOGGER.error("Statement: \"{}\" execute fail. Caused by:", sql, e);
            fail();
          }
          result.print(false, "");
        }
        // 再次执行垃圾回收
        runtime.gc();
        // 获取执行语句后的内存使用情况
        long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        // 计算内存使用的变化
        long memoryUsed = usedMemoryAfter - usedMemoryBefore;
        // 输出内存使用情况
        System.out.println("Memory used by the statement: " + memoryUsed + " bytes");
        long timeCost = System.currentTimeMillis() - startTime;
        System.out.println("end tpc-h query, time cost: " + timeCost + "ms");

        // 验证
        List<List<Object>> values = result.getValues();
        List<List<String>> answers =
            csvReader("src/test/resources/tpch/sf0.1/q" + queryId + ".csv");
        if (values.size() != answers.size()) {
          System.out.println("values.size() = " + values.size());
          System.out.println("answers.size() = " + answers.size());
          throw new RuntimeException("size not equal");
        }
        for (int i = 0; i < values.size(); i++) {
          for (int j = 0; j < values.get(i).size(); j++) {
            if (result.getPaths().get(j).contains("address")
                || result.getPaths().get(j).contains("comment")
                || result.getPaths().get(j).contains("orderdate")) {
              // TODO change unix time to date
              continue;
            }
            // if only contains number and dot, then parse to double
            if (values.get(i).get(j).toString().matches("-?[0-9]+.*[0-9]*")) {
              double number = Double.parseDouble(values.get(i).get(j).toString());
              double answerNumber = Double.parseDouble(answers.get(i).get(j));
              if (answerNumber - number >= 1e-3 || number - answerNumber >= 1e-3) {
                System.out.println("Number: " + number);
                System.out.println("Answer number: " + answerNumber);
              }
              assert answerNumber - number < 1e-3 && number - answerNumber < 1e-3;
            } else {
              String resultString =
                  new String((byte[]) values.get(i).get(j), StandardCharsets.UTF_8);
              String answerString = answers.get(i).get(j);
              if (!resultString.equals(answerString)) {
                System.out.println("Result string: " + resultString);
                System.out.println("Answer string: " + answerString);
              }
              assert resultString.equals(answerString);
            }
          }
        }
      }

      String fileName = "src/test/resources/tpch/oldTimeCosts.txt";
      if (!Files.exists(Paths.get(fileName))) { // 文件不存在，即此次跑的是主分支代码，需要将查询时间写入文件
        List<Long> timeCosts = new ArrayList<>();
        for (int queryId : queryIds) {
          // 开始 tpc-h 查询
          System.out.printf("start tpc-h query %d in main branch%n", queryId);
          // read from sql file
          String sqlString =
              readSqlFileAsString("src/test/resources/tpch/queries/q" + queryId + ".sql");
          // 执行查询语句, split by ; 最后一句为执行结果
          String[] sqls = sqlString.split(";");

          // 主分支上重复查询两次取平均值
          long sum = 0;
          for (int i = 0; i < 2; i++) {
            long timeCost = executeSQL(sqls[sqls.length - 2] + ";");
            sum += timeCost;
          }
          sum /= 2;
          timeCosts.add(sum);
          System.out.printf("end tpc-h query %d in main branch, time cost: %dms%n", queryId, sum);
        }
        writeToFile(timeCosts, fileName);
      } else { // 文件存在，即此次跑的是新分支代码，需要读取文件进行比较
        List<Long> oldTimeCosts = readFromFile(fileName);
        double multiplyPercentage = 1 + REGRESSION_THRESHOLD;
        for (int i = 0; i < queryIds.size(); i++) {
          double oldTimeCost = (double) oldTimeCosts.get(i);
          double newTimeCost = 0;
          List<Long> newTimeCosts = new ArrayList<>();
          boolean regressionDetected = true;

          while (newTimeCosts.size() < MAX_REPETITIONS_NUM) {
            // 开始 tpc-h 查询
            System.out.printf("start tpc-h query %d in new branch%n", queryIds.get(i));
            // read from sql file
            String sqlString =
                readSqlFileAsString("src/test/resources/tpch/queries/q" + queryIds.get(i) + ".sql");
            // 执行查询语句, split by ; 最后一句为执行结果
            String[] sqls = sqlString.split(";");
            long timeCost = executeSQL(sqls[sqls.length - 2] + ";");
            System.out.printf(
                "end tpc-h query %d in new branch, time cost: %dms%n", queryIds.get(i), timeCost);
            newTimeCosts.add(timeCost);
            newTimeCost = getMedian(newTimeCosts);
            if (oldTimeCost * multiplyPercentage >= newTimeCost) {
              regressionDetected = false;
              break;
            }
          }

          // 重复10次后耗时仍超过阈值
          if (regressionDetected) {
            System.out.printf(
                "performance degradation of query %d exceeds %f%n",
                queryIds.get(i), REGRESSION_THRESHOLD);
            System.out.printf("old timing: %.3fms%n", oldTimeCost);
            System.out.printf("new timing: %.3fms%n", newTimeCost);
            LOGGER.error("TPC-H query {} regression test fail.", queryIds.get(i));
            fail();
          }
        }
      }

    } catch (IOException e) {
      LOGGER.error("Test fail. Caused by:", e);
      fail();
    }
  }

  private static long executeSQL(String sql) {
    long startTime = System.currentTimeMillis();
    try {
      SessionExecuteSqlResult result = session.executeSql(sql);
    } catch (SessionException e) {
      LOGGER.error("Statement: \"{}\" execute fail. Caused by:", sql, e);
      fail();
    }
    return System.currentTimeMillis() - startTime;
  }

  private static double getMedian(List<Long> array) {
    Collections.sort(array);
    int middle = array.size() / 2;
    return array.size() % 2 == 0
        ? (array.get(middle - 1) + array.get(middle)) / 2.0
        : (double) array.get(middle);
  }

  private static void writeToFile(List<Long> timeCosts, String fileName) throws IOException {
    Path filePath = Paths.get(fileName);
    List<String> lines = timeCosts.stream().map(String::valueOf).collect(Collectors.toList());
    Files.write(filePath, lines);
  }

  private static List<Long> readFromFile(String fileName) throws IOException {
    Path filePath = Paths.get(fileName);
    List<String> lines = Files.readAllLines(filePath);
    return lines.stream().map(Long::valueOf).collect(Collectors.toList());
  }

  private static List<List<String>> csvReader(String filePath) {
    List<List<String>> data = new ArrayList<>();
    boolean skipHeader = true;
    try (Scanner scanner = new Scanner(Paths.get(filePath))) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (skipHeader) {
          skipHeader = false;
          continue;
        }
        List<String> row = Arrays.asList(line.split("\\|"));
        data.add(row);
      }
    } catch (IOException e) {
      LOGGER.error("Read file {} fail. Caused by:", filePath, e);
      fail();
    }
    System.out.println(data);
    return data;
  }

  private static String readSqlFileAsString(String filePath) throws IOException {
    StringBuilder contentBuilder = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = br.readLine()) != null) {
        contentBuilder.append(line).append("\n");
      }
    }
    return contentBuilder.toString();
  }
}