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
package cn.edu.tsinghua.iginx.engine.shared.operator.visitor;

import cn.edu.tsinghua.iginx.engine.shared.operator.BinaryOperator;
import cn.edu.tsinghua.iginx.engine.shared.operator.MultipleOperator;
import cn.edu.tsinghua.iginx.engine.shared.operator.Operator;
import cn.edu.tsinghua.iginx.engine.shared.operator.UnaryOperator;
import cn.edu.tsinghua.iginx.engine.shared.source.OperatorSource;
import cn.edu.tsinghua.iginx.engine.shared.source.Source;
import cn.edu.tsinghua.iginx.engine.shared.source.SourceType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexVisitor implements OperatorVisitor {

  private final Map<Operator, Operator> parentMap = new HashMap<>();

  private final Map<Operator, List<Operator>> childrenMap = new HashMap<>();

  public Map<Operator, Operator> getParentMap() {
    return parentMap;
  }

  public Map<Operator, List<Operator>> getChildrenMap() {
    return childrenMap;
  }

  @Override
  public void visit(UnaryOperator unaryOperator) {
    Source source = unaryOperator.getSource();
    if (source.getType() == SourceType.Operator) {
      Operator child = ((OperatorSource) source).getOperator();
      parentMap.put(child, unaryOperator);
      childrenMap.computeIfAbsent(unaryOperator, k -> new ArrayList<>()).add(child);
    }
  }

  @Override
  public void visit(BinaryOperator binaryOperator) {
    Source sourceA = binaryOperator.getSourceA();
    if (sourceA.getType() == SourceType.Operator) {
      Operator childA = ((OperatorSource) sourceA).getOperator();
      parentMap.put(childA, binaryOperator);
      childrenMap.computeIfAbsent(binaryOperator, k -> new ArrayList<>()).add(childA);
    }
    Source sourceB = binaryOperator.getSourceB();
    if (sourceB.getType() == SourceType.Operator) {
      Operator childB = ((OperatorSource) sourceB).getOperator();
      parentMap.put(childB, binaryOperator);
      childrenMap.computeIfAbsent(binaryOperator, k -> new ArrayList<>()).add(childB);
    }
  }

  @Override
  public void visit(MultipleOperator multipleOperator) {
    for (Source source : multipleOperator.getSources()) {
      if (source.getType() == SourceType.Operator) {
        Operator child = ((OperatorSource) source).getOperator();
        parentMap.put(child, multipleOperator);
        childrenMap.computeIfAbsent(multipleOperator, k -> new ArrayList<>()).add(child);
      }
    }
  }
}
