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
package cn.edu.tsinghua.iginx.filesystem.struct.legacy.parquet.util;

import java.util.NoSuchElementException;
import org.apache.arrow.util.Preconditions;

public class CloseableHolders {

  public static class NoexceptAutoCloseableHolder<T extends NoexceptAutoCloseable>
      implements NoexceptAutoCloseable {
    private T closeable;

    NoexceptAutoCloseableHolder(T closeable) {
      this.closeable = Preconditions.checkNotNull(closeable);
    }

    public T transfer() {
      if (closeable == null) {
        throw new NoSuchElementException("Already transferred");
      }
      T ret = closeable;
      closeable = null;
      return ret;
    }

    public T peek() {
      return closeable;
    }

    @Override
    public void close() {
      if (closeable != null) {
        closeable.close();
        closeable = null;
      }
    }
  }

  public static <T extends NoexceptAutoCloseable> NoexceptAutoCloseableHolder<T> hold(T closeable) {
    return new NoexceptAutoCloseableHolder<>(closeable);
  }
}
