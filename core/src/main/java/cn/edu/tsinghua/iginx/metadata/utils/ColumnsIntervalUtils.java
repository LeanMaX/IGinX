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
package cn.edu.tsinghua.iginx.metadata.utils;

import cn.edu.tsinghua.iginx.metadata.entity.ColumnsInterval;

public class ColumnsIntervalUtils {

  public static ColumnsInterval fromString(String str) {
    String[] parts = str.split("-");
    assert parts.length == 2;
    return new ColumnsInterval(
        parts[0].equals("null") ? null : parts[0], parts[1].equals("null") ? null : parts[1]);
  }
}
