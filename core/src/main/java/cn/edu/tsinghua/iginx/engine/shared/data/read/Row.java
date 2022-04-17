/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package cn.edu.tsinghua.iginx.engine.shared.data.read;

import cn.edu.tsinghua.iginx.engine.shared.data.Value;
import cn.edu.tsinghua.iginx.thrift.DataType;

import java.util.Arrays;
import java.util.Objects;

public class Row {

    public static final long NON_EXISTED_TIMESTAMP = -1L;

    public static final Row EMPTY_ROW = new Row(Header.EMPTY_HEADER, new Object[0]);

    private final Header header;

    private final long timestamp;

    private final Object[] values;

    public Row(Header header, Object[] values) {
        this(header, NON_EXISTED_TIMESTAMP, values);
    }

    public Row(Header header, long timestamp, Object[] values) {
        this.header = header;
        this.timestamp = timestamp;
        this.values = values;
    }

    public Header getHeader() {
        return header;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Object[] getValues() {
        return values;
    }

    public Object getValue(int i) {
        return values[i];
    }

    public Field getField(int i) {
        return header.getField(i);
    }

    public String getName(int i) {
        return header.getField(i).getName();
    }

    public DataType getType(int i) {
        return header.getField(i).getType();
    }

    public Object getValue(Field field) {
        int index = header.indexOf(field);
        if (index == -1) {
            return null;
        }
        return values[index];
    }

    public Object getValue(String name) {
        int index = header.indexOf(name);
        if (index == -1) {
            return null;
        }
        return values[index];
    }

    public Value getAsValue(String name) {
        int index = header.indexOf(name);
        if (index == -1) {
            return null;
        }
        return new Value(header.getField(index).getType(), values[index]);
    }

    @Override
    public String toString() {
        return "Row{" +
            "timestamp=" + timestamp +
            ", values=" + Arrays.toString(values) +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Row row = (Row) o;
        return timestamp == row.timestamp && Objects.equals(header, row.header) && Arrays.equals(values, row.values);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(header, timestamp);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }
}
