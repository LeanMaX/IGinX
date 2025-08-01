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
package cn.edu.tsinghua.iginx.engine.logical.utils;

import cn.edu.tsinghua.iginx.metadata.entity.ColumnsInterval;
import cn.edu.tsinghua.iginx.utils.Pair;
import cn.edu.tsinghua.iginx.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PathUtils {

  public static final String STAR = "*";

  public static final Character MIN_CHAR = '!';
  public static final Character MAX_CHAR = '~';

  public static ColumnsInterval trimColumnsInterval(ColumnsInterval columnsInterval) {
    String startColumn = columnsInterval.getStartColumn();
    if (startColumn.contains(STAR)) {
      if (startColumn.startsWith(STAR)) {
        startColumn = null;
      } else {
        startColumn = startColumn.substring(0, startColumn.indexOf(STAR)) + MIN_CHAR;
      }
    }

    String endColumn = columnsInterval.getEndColumn();
    if (endColumn.contains(STAR)) {
      if (endColumn.startsWith(STAR)) {
        endColumn = null;
      } else {
        endColumn = endColumn.substring(0, endColumn.indexOf(STAR)) + MAX_CHAR;
      }
    }

    return new ColumnsInterval(startColumn, endColumn);
  }

  public static ColumnsInterval addSuffix(ColumnsInterval columnsInterval) {
    String startColumn = columnsInterval.getStartColumn();
    String endColumn = columnsInterval.getEndColumn();
    return new ColumnsInterval(startColumn + MIN_CHAR, endColumn + MAX_CHAR);
  }

  /**
   * 反向重命名模式列表中的模式
   *
   * @param aliasList 重命名规则, key为旧模式，value为新模式，在这里我们要将新模式恢复为旧模式
   * @param patterns 要重命名的模式列表
   * @return 重命名后的模式列表
   */
  public static List<String> recoverRenamedPatterns(
      List<Pair<String, String>> aliasList, List<String> patterns) {
    List<String> result = new ArrayList<>();
    patterns.forEach(pattern -> result.addAll(recoverRenamedPattern(aliasList, pattern)));
    return result;
  }

  public static List<String> recoverRenamedPattern(
      List<Pair<String, String>> aliasList, String pattern) {
    List<String> result = new ArrayList<>();
    for (Pair<String, String> pair : aliasList) {
      if (pair.v.equals(pattern)) {
        result.add(pair.k);
        continue;
      }
      String oldPattern = pair.k.replace("*", "$1"); // 通配符转换为正则的捕获组
      String newPattern = pair.v.replace("*", "(.*)"); // 使用反向引用保留原始匹配的部分
      if (pattern.matches(newPattern)) {
        // 如果旧模式中有通配符，但是新模式中没有，我们需要将新模式中的捕获组替换为通配符
        if (oldPattern.contains("$1") && !newPattern.contains("*")) {
          oldPattern = oldPattern.replace("$1", "*");
        }
        result.add(pattern.replaceAll(newPattern, oldPattern));
      } else if (pattern.contains(".*") && newPattern.matches(StringUtils.reformatPath(pattern))) {
        result.add(pair.k);
      }
    }
    if (result.isEmpty()) {
      result.add(pattern);
    }
    return result;
  }

  // 判断是否模式a可以覆盖模式b
  public static boolean covers(String a, String b) {
    // 使用.*作为分隔符分割模式
    String[] partsA = a.split("\\*");
    String[] partsB = b.split("\\*");

    int indexB = 0;
    for (String part : partsA) {
      boolean found = false;
      while (indexB < partsB.length) {
        if (partsB[indexB].contains(part)) {
          found = true;
          indexB++; // 移动到下一个部分
          break;
        }
        indexB++;
      }
      if (!found) {
        return false; // 如果任何部分未找到匹配，则模式a不能覆盖模式b
      }
    }
    return true;
  }

  // 检查第一组模式是否完全包含第二组模式
  public static boolean checkCoverage(Collection<String> groupA, Collection<String> groupB) {
    for (String patternB : groupB) {
      boolean covered = false;
      for (String patternA : groupA) {
        if (covers(patternA, patternB)) {
          covered = true;
          break;
        }
      }
      if (!covered) {
        return false; // 如果找不到覆盖patternB的patternA，则第一组不完全包含第二组
      }
    }
    return true; // 所有的patternB都被至少一个patternA覆盖
  }
}
