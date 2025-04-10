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
package cn.edu.tsinghua.iginx.integration.expansion.dameng;

import static cn.edu.tsinghua.iginx.integration.expansion.utils.SQLTestTools.executeShellScript;
import static org.junit.Assert.fail;

import cn.edu.tsinghua.iginx.exception.SessionException;
import cn.edu.tsinghua.iginx.integration.controller.Controller;
import cn.edu.tsinghua.iginx.integration.expansion.BaseCapacityExpansionIT;
import cn.edu.tsinghua.iginx.integration.expansion.constant.Constant;
import cn.edu.tsinghua.iginx.integration.expansion.utils.SQLTestTools;
import cn.edu.tsinghua.iginx.integration.tool.ConfLoader;
import cn.edu.tsinghua.iginx.integration.tool.DBConf;
import cn.edu.tsinghua.iginx.thrift.StorageEngineType;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DamengCapacityExpansionIT extends BaseCapacityExpansionIT {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(
          cn.edu.tsinghua.iginx.integration.expansion.dameng.DamengCapacityExpansionIT.class);

  public DamengCapacityExpansionIT() {
    super(
        StorageEngineType.relational,
        "engine=dameng, username=SYSDBA, password=SYSDBA001",
        new DamengHistoryDataGenerator());
    ConfLoader conf = new ConfLoader(Controller.CONFIG_FILE);
    DBConf dbConf = conf.loadDBConf(conf.getStorageType());
    Constant.oriPort = dbConf.getDBCEPortMap().get(Constant.ORI_PORT_NAME);
    Constant.expPort = dbConf.getDBCEPortMap().get(Constant.EXP_PORT_NAME);
    Constant.readOnlyPort = dbConf.getDBCEPortMap().get(Constant.READ_ONLY_PORT_NAME);
    wrongExtraParams.add("username=wrong, password=SYSDBA001");
    updatedParams.put("password", "newPassword");
  }

  @Override
  protected void updateParams(int port) {
    changeParams(port, "SYSDBA001", "newPassword");
  }

  @Override
  protected void restoreParams(int port) {
    changeParams(port, "newPassword", "SYSDBA001");
  }

  @Override
  protected void shutdownDatabase(int port) {
    shutOrRestart(port, true, "dameng");
  }

  @Override
  protected void startDatabase(int port) {
    shutOrRestart(port, false, "dameng");
  }

  private void changeParams(int port, String oldPw, String newPw) {
    String scriptPath = updateParamsScriptDir + "dameng.sh";
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      scriptPath = updateParamsScriptDir + "dameng_windows.sh";
    }
    // 脚本参数：对应端口，旧密码，新密码
    int res = executeShellScript(scriptPath, String.valueOf(port), oldPw, newPw);
    if (res != 0) {
      fail("Fail to update Dameng database params.");
    }
  }

  @Override
  protected void testQuerySpecialHistoryData() {
    testFloatData();
  }

  /** 测试float类型数据 */
  private void testFloatData() {
    String statement = "select wt02.float from tm.wf05 where wt02.float <= 44.55;";
    List<String> pathList = Constant.READ_ONLY_FLOAT_PATH_LIST;
    List<List<Object>> valuesList = Constant.READ_ONLY_FLOAT_VALUES_LIST;
    SQLTestTools.executeAndCompare(session, statement, pathList, valuesList);
    statement = "select wt02.float from tm.wf05 where wt02.float = 44.55;";
    valuesList = Arrays.asList(Arrays.asList(44.55F));
    SQLTestTools.executeAndCompare(session, statement, pathList, valuesList);
  }

  @Override
  protected void testPathOverlappedDataNotOverlapped() throws SessionException {
    // before
    String statement = "select status from mn.wf01.wt01;";
    String expected =
        "ResultSets:\n"
            + "+-----------------+-------------------+\n"
            + "|              key|mn.wf01.wt01.status|\n"
            + "+-----------------+-------------------+\n"
            + "|32690615153702352|           11111111|\n"
            + "|33357770565002400|           22222222|\n"
            + "+-----------------+-------------------+\n"
            + "Total line number = 2\n";
    SQLTestTools.executeAndCompare(session, statement, expected);

    String insert =
        "insert into mn.wf01.wt01 (key, status) values (10, 33333333), (100, 44444444);";
    session.executeSql(insert);

    // after
    statement = "select status from mn.wf01.wt01;";
    expected =
        "ResultSets:\n"
            + "+-----------------+-------------------+\n"
            + "|              key|mn.wf01.wt01.status|\n"
            + "+-----------------+-------------------+\n"
            + "|               10|           33333333|\n"
            + "|              100|           44444444|\n"
            + "|32690615153702352|           11111111|\n"
            + "|33357770565002400|           22222222|\n"
            + "+-----------------+-------------------+\n"
            + "Total line number = 4\n";
    SQLTestTools.executeAndCompare(session, statement, expected);
  }
}
