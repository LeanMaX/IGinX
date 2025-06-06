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
package cn.edu.tsinghua.iginx.filesystem.struct.legacy.parquet.db.util.iterator;

import cn.edu.tsinghua.iginx.filesystem.struct.legacy.parquet.db.util.AreaSet;
import cn.edu.tsinghua.iginx.filesystem.struct.legacy.parquet.util.exception.StorageException;
import com.google.common.collect.RangeSet;

public class AreaFilterScanner<K extends Comparable<K>, F, V> implements Scanner<K, Scanner<F, V>> {

  private final Scanner<K, Scanner<F, V>> scanner;

  private final AreaSet<K, F> areas;

  public AreaFilterScanner(Scanner<K, Scanner<F, V>> scanner, AreaSet<K, F> exclusive) {
    this.scanner = scanner;
    this.areas = exclusive;
  }

  private K currentKey = null;
  private Scanner<F, V> currentValue = null;

  @Override
  public K key() {
    return currentKey;
  }

  @Override
  public Scanner<F, V> value() {
    return currentValue;
  }

  @Override
  public boolean iterate() throws StorageException {
    while (scanner.iterate()) {
      K key = scanner.key();
      if (!excludeKey(key)) {
        currentKey = key;
        currentValue = new RowFilterScanner(scanner.value(), areas);
        return true;
      }
    }
    currentKey = null;
    currentValue = null;
    return false;
  }

  @Override
  public void close() throws StorageException {
    scanner.close();
  }

  private boolean excludeKey(K key) {
    return areas.getKeys().contains(key);
  }

  private class RowFilterScanner implements Scanner<F, V> {

    private final Scanner<F, V> scanner;

    private final AreaSet<K, F> areas;

    private RowFilterScanner(Scanner<F, V> scanner, AreaSet<K, F> exclusive) {
      this.scanner = scanner;
      this.areas = exclusive;
    }

    private F currentField = null;
    private V currentValue = null;

    @Override
    public F key() {
      return currentField;
    }

    @Override
    public V value() {
      return currentValue;
    }

    @Override
    public boolean iterate() throws StorageException {
      while (scanner.iterate()) {
        F field = scanner.key();
        if (!excludeField(field)) {
          currentField = field;
          currentValue = scanner.value();
          return true;
        }
      }
      currentField = null;
      currentValue = null;
      return false;
    }

    @Override
    public void close() throws StorageException {}

    private boolean excludeField(F field) {
      if (areas.getFields().contains(field)) {
        return true;
      }
      RangeSet<K> rangeSet = areas.getSegments().get(field);
      if (rangeSet == null) {
        return false;
      }
      return rangeSet.contains(currentKey);
    }
  }
}
