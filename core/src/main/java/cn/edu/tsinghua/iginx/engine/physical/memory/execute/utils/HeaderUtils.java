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
package cn.edu.tsinghua.iginx.engine.physical.memory.execute.utils;

import static cn.edu.tsinghua.iginx.engine.shared.function.system.ArithmeticExpr.ARITHMETIC_EXPR;
import static cn.edu.tsinghua.iginx.engine.shared.function.system.utils.ValueUtils.isNumericType;
import static cn.edu.tsinghua.iginx.sql.SQLConstant.DOT;
import static cn.edu.tsinghua.iginx.thrift.DataType.BOOLEAN;

import cn.edu.tsinghua.iginx.constant.GlobalConstant;
import cn.edu.tsinghua.iginx.engine.physical.exception.InvalidOperatorParameterException;
import cn.edu.tsinghua.iginx.engine.physical.exception.PhysicalException;
import cn.edu.tsinghua.iginx.engine.shared.data.read.Field;
import cn.edu.tsinghua.iginx.engine.shared.data.read.Header;
import cn.edu.tsinghua.iginx.engine.shared.expr.BaseExpression;
import cn.edu.tsinghua.iginx.engine.shared.expr.Expression;
import cn.edu.tsinghua.iginx.engine.shared.expr.KeyExpression;
import cn.edu.tsinghua.iginx.engine.shared.function.Function;
import cn.edu.tsinghua.iginx.engine.shared.function.FunctionCall;
import cn.edu.tsinghua.iginx.engine.shared.function.FunctionParams;
import cn.edu.tsinghua.iginx.engine.shared.function.manager.FunctionManager;
import cn.edu.tsinghua.iginx.engine.shared.operator.GroupBy;
import cn.edu.tsinghua.iginx.engine.shared.operator.RowTransform;
import cn.edu.tsinghua.iginx.engine.shared.operator.Sort;
import cn.edu.tsinghua.iginx.engine.shared.operator.filter.*;
import cn.edu.tsinghua.iginx.engine.shared.source.EmptySource;
import cn.edu.tsinghua.iginx.thrift.DataType;
import cn.edu.tsinghua.iginx.utils.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class HeaderUtils {

  public static Header constructNewHead(Header header, String markColumn) {
    List<Field> fields = new ArrayList<>(header.getFields());
    fields.add(new Field(markColumn, BOOLEAN));
    return header.hasKey() ? new Header(Field.KEY, fields) : new Header(fields);
  }

  public static Header constructNewHead(Header headerA, Header headerB, boolean remainKeyA) {
    return constructNewHead(headerA, headerB, remainKeyA, Collections.emptyList());
  }

  public static Header constructNewHead(
      Header headerA, Header headerB, boolean remainKeyA, List<String> extraJoinPaths) {
    List<Field> fields = new ArrayList<>(headerA.getFields());
    headerB
        .getFields()
        .forEach(
            fieldB -> {
              if (!extraJoinPaths.contains(fieldB.getName())) {
                fields.add(fieldB);
              }
            });
    return remainKeyA && headerA.hasKey() ? new Header(Field.KEY, fields) : new Header(fields);
  }

  public static Header constructNewHead(
      Header headerA, Header headerB, String prefixA, String prefixB) {
    List<Field> fields = new ArrayList<>();
    if (headerA.hasKey() && prefixA != null) {
      fields.add(new Field(prefixA + "." + GlobalConstant.KEY_NAME, DataType.LONG));
    }
    fields.addAll(headerA.getFields());
    if (headerB.hasKey() && prefixB != null) {
      fields.add(new Field(prefixB + "." + GlobalConstant.KEY_NAME, DataType.LONG));
    }
    fields.addAll(headerB.getFields());
    return new Header(fields);
  }

  public static Header constructNewHead(
      Header headerA, Header headerB, String prefixA, String prefixB, List<String> extraJoinPaths) {
    List<Field> fields = new ArrayList<>();
    if (headerA.hasKey()) {
      fields.add(new Field(prefixA + "." + GlobalConstant.KEY_NAME, DataType.LONG));
    }
    fields.addAll(headerA.getFields());
    if (headerB.hasKey()) {
      fields.add(new Field(prefixB + "." + GlobalConstant.KEY_NAME, DataType.LONG));
    }
    headerB
        .getFields()
        .forEach(
            field -> {
              if (!extraJoinPaths.contains(field.getName())) {
                fields.add(field);
              }
            });
    return new Header(fields);
  }

  public static Header constructNewHead(
      Header headerA,
      Header headerB,
      String prefixA,
      String prefixB,
      boolean cutRight,
      List<String> joinColumns,
      List<String> extraJoinPaths) {
    List<Field> fields = new ArrayList<>();
    if (headerA.hasKey() && prefixA != null) {
      fields.add(new Field(prefixA + DOT + GlobalConstant.KEY_NAME, DataType.LONG));
    }

    if (cutRight) {
      fields.addAll(headerA.getFields());
    } else {
      List<String> joinPathA = new ArrayList<>();
      joinColumns.forEach(
          joinColumn -> {
            joinPathA.add(prefixA + DOT + joinColumn);
          });
      headerA
          .getFields()
          .forEach(
              field -> {
                if (extraJoinPaths.contains(field.getName())) {
                  return;
                } else if (joinPathA.contains(field.getName())) {
                  return;
                }
                fields.add(field);
              });
    }

    if (headerB.hasKey() && prefixB != null) {
      fields.add(new Field(prefixB + DOT + GlobalConstant.KEY_NAME, DataType.LONG));
    }

    if (cutRight) {
      List<String> joinPathB = new ArrayList<>();
      joinColumns.forEach(
          joinColumn -> {
            joinPathB.add(prefixB + DOT + joinColumn);
          });
      headerB
          .getFields()
          .forEach(
              field -> {
                if (extraJoinPaths.contains(field.getName())) {
                  return;
                } else if (joinPathB.contains(field.getName())) {
                  return;
                }
                fields.add(field);
              });
    } else {
      fields.addAll(headerB.getFields());
    }

    return new Header(fields);
  }

  public static Pair<int[], Header> constructNewHead(
      Header headerA,
      Header headerB,
      String prefixA,
      String prefixB,
      List<String> joinColumns,
      boolean cutRight) {
    List<Field> fieldsA = headerA.getFields();
    List<Field> fieldsB = headerB.getFields();
    int[] indexOfJoinColumnsInTable = new int[joinColumns.size()];

    List<Field> fields = new ArrayList<>();
    if (headerA.hasKey()) {
      fields.add(new Field(prefixA + "." + GlobalConstant.KEY_NAME, DataType.LONG));
    }
    if (cutRight) {
      fields.addAll(fieldsA);
      if (headerB.hasKey()) {
        fields.add(new Field(prefixB + "." + GlobalConstant.KEY_NAME, DataType.LONG));
      }
      int i = 0;
      flag:
      for (Field fieldB : fieldsB) {
        for (String joinColumn : joinColumns) {
          if (Objects.equals(fieldB.getName(), prefixB + '.' + joinColumn)) {
            indexOfJoinColumnsInTable[i++] = headerB.indexOf(fieldB);
            continue flag;
          }
        }
        fields.add(fieldB);
      }
    } else {
      int i = 0;
      flag:
      for (Field fieldA : fieldsA) {
        for (String joinColumn : joinColumns) {
          if (Objects.equals(fieldA.getName(), prefixA + '.' + joinColumn)) {
            indexOfJoinColumnsInTable[i++] = headerA.indexOf(fieldA);
            continue flag;
          }
        }
        fields.add(fieldA);
      }
      if (headerB.hasKey()) {
        fields.add(new Field(prefixB + "." + GlobalConstant.KEY_NAME, DataType.LONG));
      }
      fields.addAll(fieldsB);
    }
    return new Pair<>(indexOfJoinColumnsInTable, new Header(fields));
  }

  public static Pair<String, String> calculateHashJoinPath(
      Header headerA,
      Header headerB,
      String prefixA,
      String prefixB,
      Filter filter,
      List<String> joinColumns,
      List<String> extraJoinPaths)
      throws InvalidOperatorParameterException {
    String joinPathA = null, joinPathB = null;
    if (!extraJoinPaths.isEmpty()) {
      joinPathA = extraJoinPaths.get(0);
      joinPathB = extraJoinPaths.get(0);
    } else {
      if (!joinColumns.isEmpty()) {
        joinPathA = prefixA + '.' + joinColumns.get(0);
        joinPathB = prefixB + '.' + joinColumns.get(0);
      } else {
        List<Pair<String, String>> pairs = calculateHashJoinPath(filter);
        if (pairs == null) {
          throw new InvalidOperatorParameterException(
              "filter: " + filter + " can't be used in hash join.");
        }
        for (Pair<String, String> pair : pairs) {
          if (headerA.indexOf(pair.k) != -1 && headerB.indexOf(pair.v) != -1) {
            joinPathA = pair.k;
            joinPathB = pair.v;
            break;
          } else if (headerA.indexOf(pair.v) != -1 && headerB.indexOf(pair.k) != -1) {
            joinPathA = pair.v;
            joinPathB = pair.k;
            break;
          }
        }
        if (joinPathA == null || joinPathB == null) {
          throw new InvalidOperatorParameterException(
              String.format(
                  "filter: %s can't be used in hash join, headerA: %s, headerB: %s.",
                  filter, headerA, headerB));
        }
      }
    }
    return new Pair<>(joinPathA, joinPathB);
  }

  private static List<Pair<String, String>> calculateHashJoinPath(Filter filter) {
    if (filter == null) {
      return null;
    }
    List<Pair<String, String>> joinPaths = new ArrayList<>();
    filter.accept(
        new FilterVisitor() {
          @Override
          public void visit(AndFilter filter) {}

          @Override
          public void visit(OrFilter filter) {}

          @Override
          public void visit(NotFilter filter) {}

          @Override
          public void visit(KeyFilter filter) {}

          @Override
          public void visit(ValueFilter filter) {}

          @Override
          public void visit(PathFilter filter) {
            joinPaths.add(new Pair<>(filter.getPathA(), filter.getPathB()));
          }

          @Override
          public void visit(BoolFilter filter) {}

          @Override
          public void visit(ExprFilter filter) {}

          @Override
          public void visit(InFilter filter) {}
        });
    return joinPaths;
  }

  public static void checkHeadersComparable(Header headerA, Header headerB)
      throws PhysicalException {
    // 检查是否同时有或没有key列
    if (headerA.hasKey() ^ headerB.hasKey()) {
      throw new InvalidOperatorParameterException(
          "Row stream to be union, except or intersect must have key or have not key at the same time.");
    }

    // 检查fields数量是否相等
    if (headerA.getFieldSize() != headerB.getFieldSize()) {
      throw new InvalidOperatorParameterException(
          "Row stream to be union, except or intersect must have the same number of fields.");
    }

    // 没有key列时，fields不能为空
    if (!headerA.hasKey() && headerA.getFieldSize() < 1) {
      throw new InvalidOperatorParameterException(
          "Row stream with no key to be union, except or intersect must have more than one field.");
    }

    // 检查对应位置的field是否可比较
    int size = headerA.getFieldSize();
    DataType typeA, typeB;
    for (int index = 0; index < size; index++) {
      typeA = headerA.getField(index).getType();
      typeB = headerB.getField(index).getType();
      boolean comparable = isNumericType(typeA) && isNumericType(typeB) || typeA.equals(typeB);
      if (!comparable) {
        throw new InvalidOperatorParameterException(
            "Field "
                + headerA.getField(index).getName()
                + "("
                + typeA
                + ") and field "
                + headerB.getField(index).getName()
                + "("
                + typeB
                + ") are incomparable.");
      }
    }
  }

  public static RowTransform checkGroupByHeader(Header header, GroupBy groupBy) {
    Set<Expression> appendExpressions = new HashSet<>();
    for (Expression groupByExpr : groupBy.getGroupByExpressions()) {
      String exprName = groupByExpr.getColumnName();
      boolean found =
          header.getFields().stream().anyMatch(field -> field.getName().equals(exprName));
      if (!found) {
        appendExpressions.add(groupByExpr);
      }
    }

    if (appendExpressions.isEmpty()) {
      return null;
    }
    return appendArithExpressions(header, new ArrayList<>(appendExpressions));
  }

  public static RowTransform checkSortHeader(Header header, Sort sort) {
    List<Expression> sortExpressions = new ArrayList<>(sort.getSortByExpressions());
    if (sortExpressions.get(0) instanceof KeyExpression) {
      sortExpressions.remove(0);
    }
    Set<Expression> appendExpressions = new HashSet<>();
    for (Expression sortExpr : sortExpressions) {
      String exprName = sortExpr.getColumnName();
      boolean found =
          header.getFields().stream().anyMatch(field -> field.getName().equals(exprName));
      if (!found) {
        appendExpressions.add(sortExpr);
      }
    }

    if (appendExpressions.isEmpty()) {
      return null;
    }
    return appendArithExpressions(header, new ArrayList<>(appendExpressions));
  }

  private static RowTransform appendArithExpressions(Header header, List<Expression> expressions) {
    List<FunctionCall> functionCallList = new ArrayList<>();
    Function function = FunctionManager.getInstance().getFunction(ARITHMETIC_EXPR);
    for (Field field : header.getFields()) {
      functionCallList.add(
          new FunctionCall(function, new FunctionParams(new BaseExpression(field.getName()))));
    }
    for (Expression expr : expressions) {
      functionCallList.add(new FunctionCall(function, new FunctionParams(expr)));
    }
    return new RowTransform(EmptySource.EMPTY_SOURCE, functionCallList);
  }
}
