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
package cn.edu.tsinghua.iginx.engine.physical.storage.domain;

import cn.edu.tsinghua.iginx.engine.shared.Constants;
import java.util.*;

public class ColumnKey {
  public static final ColumnKey KEY = new ColumnKey(Constants.KEY, Collections.emptyMap());
  private final String path;
  private final SortedMap<String, String> tags;

  public ColumnKey(String path) {
    this(path, Collections.emptyMap());
  }

  public ColumnKey(String path, Map<String, String> tagList) {
    this.path = Objects.requireNonNull(path);
    this.tags = Collections.unmodifiableSortedMap(new TreeMap<>(tagList));
  }

  public String getPath() {
    return path;
  }

  public SortedMap<String, String> getTags() {
    return tags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ColumnKey columnKey = (ColumnKey) o;
    return Objects.equals(path, columnKey.path) && Objects.equals(tags, columnKey.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, tags);
  }

  @Override
  public String toString() {
    return path + tags;
  }
}
