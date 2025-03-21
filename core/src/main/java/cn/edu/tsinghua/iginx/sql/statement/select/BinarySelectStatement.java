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
package cn.edu.tsinghua.iginx.sql.statement.select;

import cn.edu.tsinghua.iginx.engine.shared.expr.Expression;
import cn.edu.tsinghua.iginx.engine.shared.operator.type.OperatorType;
import cn.edu.tsinghua.iginx.utils.Pair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BinarySelectStatement extends SelectStatement {

  private final SelectStatement leftQuery;
  private final OperatorType setOperator;
  private final SelectStatement rightQuery;
  private final boolean isDistinct;

  public BinarySelectStatement(
      SelectStatement leftQuery,
      OperatorType setOperator,
      SelectStatement rightQuery,
      boolean isDistinct,
      boolean isSubQuery) {
    super(isSubQuery);
    this.selectStatementType = SelectStatementType.BINARY;
    this.leftQuery = leftQuery;
    this.setOperator = setOperator;
    this.rightQuery = rightQuery;
    this.isDistinct = isDistinct;
  }

  public SelectStatement getLeftQuery() {
    return leftQuery;
  }

  public OperatorType getSetOperator() {
    return setOperator;
  }

  public SelectStatement getRightQuery() {
    return rightQuery;
  }

  public boolean isDistinct() {
    return isDistinct;
  }

  @Override
  public List<Expression> getExpressions() {
    return leftQuery.getExpressions();
  }

  @Override
  public UnarySelectStatement getFirstUnarySelectStatement() {
    return leftQuery.getFirstUnarySelectStatement();
  }

  @Override
  public Set<String> getPathSet() {
    Set<String> pathSet = new HashSet<>(leftQuery.getPathSet());
    pathSet.addAll(rightQuery.getPathSet());
    return pathSet;
  }

  @Override
  public List<String> calculatePrefixSet() {
    return leftQuery.calculatePrefixSet();
  }

  @Override
  public void initFreeVariables() {
    if (freeVariables != null) {
      return;
    }
    leftQuery.initFreeVariables();
    rightQuery.initFreeVariables();
    Set<String> set = new HashSet<>(leftQuery.getFreeVariables());
    set.addAll(rightQuery.getFreeVariables());
    freeVariables = new ArrayList<>(set);
  }

  @Override
  public List<Pair<String, String>> getSubQueryAliasList(String alias) {
    return leftQuery.getSubQueryAliasList(alias);
  }

  @Override
  public boolean isSimpleQuery() {
    return leftQuery.isSimpleQuery() && rightQuery.isSimpleQuery();
  }
}
