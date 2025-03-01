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
package cn.edu.tsinghua.iginx.filesystem.struct.legacy.parquet.db.util;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import java.util.*;

public class AreaSet<K extends Comparable<K>, F> {
  private final Map<F, RangeSet<K>> deletedRanges;

  private final Set<F> deletedColumns;

  private final RangeSet<K> deletedRows;

  public AreaSet() {
    this(new HashMap<>(), new HashSet<>(), TreeRangeSet.create());
  }

  public static <K extends Comparable<K>, F> AreaSet<K, F> create(AreaSet<K, F> areas) {
    Map<F, RangeSet<K>> deletedRanges = new HashMap<>();
    for (Map.Entry<F, RangeSet<K>> entry : areas.deletedRanges.entrySet()) {
      deletedRanges.put(entry.getKey(), TreeRangeSet.create(entry.getValue()));
    }
    return new AreaSet<>(
        deletedRanges, new HashSet<>(areas.deletedColumns), TreeRangeSet.create(areas.deletedRows));
  }

  public static <K extends Comparable<K>, F> AreaSet<K, F> all() {
    return new AreaSet<K, F>(
        new HashMap<>(), new HashSet<>(), TreeRangeSet.create(Collections.singleton(Range.all())));
  }

  private AreaSet(
      Map<F, RangeSet<K>> deletedRanges, Set<F> deletedColumns, RangeSet<K> deletedRows) {
    this.deletedRanges = deletedRanges;
    this.deletedColumns = deletedColumns;
    this.deletedRows = deletedRows;
  }

  public void add(Set<F> fields, RangeSet<K> ranges) {
    RangeSet<K> validRanges = TreeRangeSet.create(ranges);
    validRanges.removeAll(deletedRows);
    if (validRanges.isEmpty()) {
      return;
    }
    Set<F> validFields = new HashSet<>(fields);
    validFields.removeAll(deletedColumns);
    if (validFields.isEmpty()) {
      return;
    }
    for (F field : validFields) {
      deletedRanges
          .computeIfAbsent(Objects.requireNonNull(field), k -> TreeRangeSet.create())
          .addAll(validRanges);
    }
  }

  public void add(RangeSet<K> ranges) {
    if (ranges.isEmpty()) {
      return;
    }
    deletedRows.addAll(ranges);
    deletedRanges.values().forEach(rangeSet -> rangeSet.removeAll(ranges));
    deletedRanges.values().removeIf(RangeSet::isEmpty);
  }

  public void add(Set<F> fields) {
    if (fields.isEmpty()) {
      return;
    }
    deletedColumns.addAll(fields);
    deletedRanges.keySet().removeAll(fields);
  }

  public void addAll(AreaSet<K, F> areas) {
    add(areas.getKeys());
    add(areas.getFields());
    areas.getSegments().forEach((field, rangeSet) -> add(Collections.singleton(field), rangeSet));
  }

  public void clear() {
    deletedRows.clear();
    deletedColumns.clear();
    deletedRanges.clear();
  }

  public Map<F, RangeSet<K>> getSegments() {
    return deletedRanges;
  }

  public Set<F> getFields() {
    return deletedColumns;
  }

  public RangeSet<K> getKeys() {
    return deletedRows;
  }

  public boolean isEmpty() {
    return deletedRanges.isEmpty() && deletedColumns.isEmpty() && deletedRows.isEmpty();
  }

  public boolean isAll() {
    return deletedRows.encloses(Range.all());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AreaSet<?, ?> that = (AreaSet<?, ?>) o;
    return Objects.equals(deletedRanges, that.deletedRanges)
        && Objects.equals(deletedColumns, that.deletedColumns)
        && Objects.equals(deletedRows, that.deletedRows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(deletedRanges, deletedColumns, deletedRows);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", AreaSet.class.getSimpleName() + "[", "]")
        .add("deletedRanges=" + deletedRanges)
        .add("deletedColumns=" + deletedColumns)
        .add("deletedRows=" + deletedRows)
        .toString();
  }
}
