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
package cn.edu.tsinghua.iginx.mongodb.tools;

import static com.mongodb.client.model.Filters.*;

import cn.edu.tsinghua.iginx.engine.logical.utils.LogicalFilterUtils;
import cn.edu.tsinghua.iginx.engine.shared.KeyRange;
import cn.edu.tsinghua.iginx.engine.shared.data.Value;
import cn.edu.tsinghua.iginx.engine.shared.data.read.Field;
import cn.edu.tsinghua.iginx.engine.shared.operator.filter.*;
import cn.edu.tsinghua.iginx.engine.shared.operator.filter.AndFilter;
import cn.edu.tsinghua.iginx.metadata.entity.KeyInterval;
import cn.edu.tsinghua.iginx.utils.Pair;
import com.mongodb.client.model.Filters;
import java.util.*;
import javax.annotation.Nullable;
import org.bson.BsonInt64;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

public class FilterUtils {

  public static Bson interval(KeyInterval range) {
    Bson left = gte("_id", range.getStartKey());
    Bson right = lt("_id", range.getEndKey());
    return and(left, right);
  }

  public static Bson ranges(List<KeyRange> ranges) {
    List<Bson> rangeFilters = new ArrayList<>();
    for (KeyRange range : ranges) {
      List<Bson> bounds = new ArrayList<>();
      if (range.isIncludeBeginKey()) {
        bounds.add(gte("_id", range.getBeginKey()));
      } else {
        bounds.add(gt("_id", range.getBeginKey()));
      }
      if (range.isIncludeEndKey()) {
        bounds.add(lte("_id", range.getEndKey()));
      } else {
        bounds.add(lt("_id", range.getEndKey()));
      }
      rangeFilters.add(and(bounds));
    }
    return or(rangeFilters);
  }

  public static Bson getPreFilter(Filter filter) {
    List<Bson> rangeFilters = new ArrayList<>();
    for (Pair<Long, Long> range : extractRanges(filter)) {
      if (range.k < range.v) {
        Bson leftBound = Filters.gte("_id", range.k);
        Bson rightBound = Filters.lte("_id", range.v);
        Bson rangeFilter = Filters.and(leftBound, rightBound);
        rangeFilters.add(rangeFilter);
      } else if (range.k.equals(range.v)) {
        Bson rangeFilter = Filters.eq("_id", range.k);
        rangeFilters.add(rangeFilter);
      }
    }
    if (rangeFilters.isEmpty()) {
      return new Document();
    }
    return Filters.or(rangeFilters);
  }

  private static List<Pair<Long, Long>> extractRanges(Filter filter) {
    Filter removeNot = LogicalFilterUtils.removeNot(filter);
    List<KeyRange> ranges = LogicalFilterUtils.getKeyRangesFromFilter(removeNot);
    List<Pair<Long, Long>> result = new ArrayList<>();
    for (KeyRange range : ranges) {
      result.add(new Pair<>(range.getActualBeginKey(), range.getActualEndKey()));
    }
    return result;
  }

  @Nullable
  public static Bson getPostFilter(Filter filter, Map<Field, String> renamedFields) {
    Filter removeNotSupported =
        LogicalFilterUtils.superSetPushDown(
            filter,
            f -> {
              switch (f.getType()) {
                case Value:
                case Path:
                case Bool:
                case And:
                case Or:
                  return false;
                case Not:
                case Key:
                case Expr:
                case In:
                  return true;
                default:
                  throw new IllegalStateException("unexpected filter type: " + f.getType());
              }
            });
    return getPostFilterImpl(removeNotSupported, renamedFields);
  }

  private static Bson getPostFilterImpl(Filter filter, Map<Field, String> renamedFields) {
    switch (filter.getType()) {
      case Value:
        return getFilter((ValueFilter) filter, renamedFields);
      case Path:
        return getFilter((PathFilter) filter, renamedFields);
      case Bool:
        return null;
      case And:
        return getPostFilterImpl((AndFilter) filter, renamedFields);
      case Or:
        return getFilter((OrFilter) filter, renamedFields);
      case Not:
      case Key:
      case Expr:
      case In:
      default:
        throw new IllegalStateException("unexpected filter type: " + filter.getType());
    }
  }

  @Nullable
  public static Bson getFilter(Filter filter, Map<Field, String> renamedFields) {
    switch (filter.getType()) {
      case Key:
        return getFilter((KeyFilter) filter);
      case Value:
        return getFilter((ValueFilter) filter, renamedFields);
      case Path:
        return getFilter((PathFilter) filter, renamedFields);
      case Expr:
      case Not:
      case In:
        return null;
      case Bool:
        return getFilter((BoolFilter) filter);
      case And:
        return getPostFilterImpl((AndFilter) filter, renamedFields);
      case Or:
        return getFilter((OrFilter) filter, renamedFields);
      default:
        throw new IllegalStateException("unexpected filter type: " + filter.getType());
    }
  }

  private static Bson getFilter(KeyFilter filter) {
    return fieldValueOp(filter.getOp(), "_id", new BsonInt64(filter.getValue()));
  }

  @Nullable
  private static Bson getFilter(ValueFilter filter, Map<Field, String> renamedFields) {
    String pattern = filter.getPath();
    Collection<String> names = getMatchNames(pattern, renamedFields);
    List<Bson> subFilters = new ArrayList<>();
    for (String name : names) {
      Value value = filter.getValue();
      BsonValue bsonValue = TypeUtils.toBsonValue(value.getDataType(), value.getValue());
      Bson subFilter = fieldValueOp(filter.getOp(), name, bsonValue);
      subFilters.add(subFilter);
    }
    if (subFilters.size() == 1) {
      return subFilters.get(0);
    } else if (subFilters.isEmpty()) {
      return null;
    } else {
      return unionComparisonFilters(filter.getOp(), subFilters);
    }
  }

  private static List<String> getMatchNames(String pattern, Map<Field, String> renamedFields) {
    List<String> patterns = Collections.singletonList(pattern);
    List<Field> matchedFields = NameUtils.match(renamedFields.keySet(), patterns, null);
    List<String> matchedNames = new ArrayList<>();
    for (Field field : matchedFields) {
      matchedNames.add(renamedFields.get(field));
    }
    return matchedNames;
  }

  public static Bson fieldValueOp(Op op, String fieldName, BsonValue value) {
    if (fieldName.contains("*")) {
      throw new IllegalArgumentException("wildcard is not support");
    }
    switch (op) {
      case GE:
      case GE_AND:
        return gte(fieldName, value);
      case G:
      case G_AND:
        return gt(fieldName, value);
      case LE:
      case LE_AND:
        return lte(fieldName, value);
      case L:
      case L_AND:
        return lt(fieldName, value);
      case E:
      case E_AND:
        return eq(fieldName, value);
      case NE:
      case NE_AND:
        return ne(fieldName, value);
      case LIKE:
      case LIKE_AND:
        // why append a '$' to the pattern
        // for example:
        //   match "sadaa" with /^.*[s|d]/,
        //   mongodb return true, but java return false
        return expr(
            new Document(
                "$regexMatch",
                new Document("input", "$" + fieldName)
                    .append("regex", value.asString().getValue() + "$")));
      case NOT_LIKE:
      case NOT_LIKE_AND:
        return expr(
            new Document(
                "$not",
                new Document(
                    "$regexMatch",
                    new Document("input", "$" + fieldName)
                        .append("regex", value.asString().getValue() + "$"))));
    }
    throw new IllegalStateException("unexpected Filter op: " + op);
  }

  @Nullable
  private static Bson getFilter(PathFilter filter, Map<Field, String> renamedFields) {
    List<String> namesA = getMatchNames(filter.getPathA(), renamedFields);
    List<String> namesB = getMatchNames(filter.getPathB(), renamedFields);

    if (namesA.size() > 1 && namesB.size() > 1) {
      throw new IllegalArgumentException("undefined filter: " + filter);
    }

    List<Bson> subFilters = new ArrayList<>();
    if (namesA.size() == 1) {
      String nameA = namesA.get(0);
      for (String nameB : namesB) {
        subFilters.add(fieldOp(filter.getOp(), nameA, nameB));
      }
    } else if (namesB.size() == 1) {
      String nameB = namesB.get(0);
      for (String nameA : namesA) {
        subFilters.add(fieldOp(filter.getOp(), nameA, nameB));
      }
    }

    if (subFilters.size() == 1) {
      return subFilters.get(0);
    } else if (subFilters.isEmpty()) {
      return null;
    } else {
      return unionComparisonFilters(filter.getOp(), subFilters);
    }
  }

  private static Bson unionComparisonFilters(Op op, List<Bson> subFilters) {
    switch (op) {
      case GE:
      case G:
      case LE:
      case L:
      case E:
      case NE:
      case LIKE:
        return or(subFilters);
      case GE_AND:
      case G_AND:
      case LE_AND:
      case L_AND:
      case E_AND:
      case NE_AND:
      case LIKE_AND:
        return and(subFilters);
      default:
        throw new IllegalArgumentException("unexpected Filter op: " + op);
    }
  }

  public static Bson fieldOp(Op op, String fieldA, String fieldB) {
    List<String> fields = Arrays.asList("$" + fieldA, "$" + fieldB);
    if (fieldA.contains("*") || fieldB.contains("*")) {
      throw new IllegalArgumentException("wildcard is not support");
    }
    switch (op) {
      case GE:
      case GE_AND:
        return expr(new Document("$gte", fields));
      case G:
      case G_AND:
        return expr(new Document("$gt", fields));
      case LE:
      case LE_AND:
        return expr(new Document("$lte", fields));
      case L:
      case L_AND:
        return expr(new Document("$lt", fields));
      case E:
      case E_AND:
        return expr(new Document("$eq", fields));
      case NE:
      case NE_AND:
        return expr(new Document("$ne", fields));
      case LIKE:
      case LIKE_AND:
        Bson pattern = new Document("$concat", Arrays.asList(fields.get(0), "$"));
        return expr(
            new Document(
                "$regexMatch", new Document("input", fields.get(0)).append("regex", pattern)));
    }
    throw new IllegalArgumentException("unexpected Filter op: " + op);
  }

  private static Bson getFilter(BoolFilter filter) {
    if (filter.isTrue()) {
      return new Document();
    } else {
      return nor(new Document());
    }
  }

  @Nullable
  private static Bson getPostFilterImpl(AndFilter filter, Map<Field, String> renamedFields) {
    List<Bson> subFilterList = new ArrayList<>();
    for (Filter child : filter.getChildren()) {
      Bson childFilter = getPostFilterImpl(child, renamedFields);
      if (childFilter != null) {
        subFilterList.add(childFilter);
      }
    }
    if (subFilterList.isEmpty()) {
      return null;
    }
    return and(subFilterList);
  }

  @Nullable
  private static Bson getFilter(OrFilter filter, Map<Field, String> renamedFields) {
    List<Bson> subFilterList = new ArrayList<>();
    for (Filter child : filter.getChildren()) {
      Bson childFilter = getFilter(child, renamedFields);
      if (childFilter != null) {
        subFilterList.add(childFilter);
      }
    }
    if (subFilterList.isEmpty()) {
      return null;
    }
    return or(subFilterList);
  }
}
