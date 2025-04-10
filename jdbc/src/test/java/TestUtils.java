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
import cn.edu.tsinghua.iginx.jdbc.IginXResultSet;
import cn.edu.tsinghua.iginx.session.SessionExecuteSqlResult;
import cn.edu.tsinghua.iginx.thrift.DataType;
import cn.edu.tsinghua.iginx.thrift.SqlType;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestUtils {

  public static ResultSet buildMockResultSet() {
    SessionExecuteSqlResult result = new SessionExecuteSqlResult();

    result.setSqlType(SqlType.Query);

    long[] timestamps = new long[] {1L, 2L, 3L, 4L, 5L, 6L, 7L};
    result.setKeys(timestamps);

    List<String> paths =
        new ArrayList<>(
            Arrays.asList(
                "test.result.set.boolean",
                "test.result.set.int",
                "test.result.set.long",
                "test.result.set.float",
                "test.result.set.double",
                "test.result.set.string"));
    result.setPaths(paths);

    List<List<Object>> values = new ArrayList<>();
    Object[][] valueArray = {
      {true, 1, 100000L, 10.1f, 100.5, "one"},
      {null, 2, 200000L, 20.1f, 200.5, "two"},
      {true, null, 300000L, 30.1f, 300.5, "three"},
      {false, 4, null, 40.1f, 400.5, "four"},
      {true, 5, 500000L, null, 500.5, "five"},
      {false, 6, 600000L, 60.1f, null, "six"},
      {true, 7, 700000L, 70.1f, 700.5, null}
    };
    for (Object[] row : valueArray) {
      values.add(new ArrayList<>(Arrays.asList(row)));
    }
    result.setValues(values);

    List<DataType> types =
        new ArrayList<>(
            Arrays.asList(
                DataType.BOOLEAN,
                DataType.INTEGER,
                DataType.LONG,
                DataType.FLOAT,
                DataType.DOUBLE,
                DataType.BINARY));
    result.setDataTypeList(types);

    return new IginXResultSet(null, result);
  }
}
