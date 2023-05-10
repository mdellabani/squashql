package io.squashql.js;

import io.squashql.jackson.JacksonUtil;
import io.squashql.query.*;
import io.squashql.query.builder.Query;
import io.squashql.query.dto.*;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.squashql.query.Functions.*;

public class TestJavascriptLibrary {

  /**
   * This is test is building the {@link QueryDto} wo. the help of the query builder {@link Query}.
   */
  @Test
  void testReadJsonBuildFromQueryDto() throws IOException {
    var table = new TableDto("myTable");
    var refTable = new TableDto("refTable");
    table.join(refTable, JoinType.INNER, new JoinMappingDto("fromField", "toField"));
    table.join(new TableDto("a"), JoinType.LEFT, new JoinMappingDto("a" + ".a_id", "myTable" + ".id"));

    QueryDto q = new QueryDto()
            .table(table)
            .withColumn("a")
            .withColumn("b");

    var price = new AggregatedMeasure("price.sum", "price", "sum");
    q.withMeasure(price);
    var priceFood = new AggregatedMeasure("alias", "price", "sum", criterion("category", eq("food")));
    q.withMeasure(priceFood);
    var plus = new BinaryOperationMeasure("plusMeasure", BinaryOperator.PLUS, price, priceFood);
    q.withMeasure(plus);
    var expression = new ExpressionMeasure("myExpression", "sum(price*quantity)");
    q.withMeasure(expression);
    q.withMeasure(CountMeasure.INSTANCE);
    q.withMeasure(integer(123));
    q.withMeasure(decimal(1.23));

    q.withMeasure(new ComparisonMeasureReferencePosition("comp bucket",
            ComparisonMethod.ABSOLUTE_DIFFERENCE,
            price,
            Map.of("scenario", "s-1", "group", "g"),
            ColumnSetKey.BUCKET));

    Period.Month month = new Period.Month("mois", "annee");
    q.withMeasure(new ComparisonMeasureReferencePosition("growth",
            ComparisonMethod.DIVIDE,
            price,
            Map.of("Annee", "y-1", "Mois", "m"),
            month));

    q.withMeasure(new ComparisonMeasureReferencePosition("parent",
            ComparisonMethod.DIVIDE,
            price,
            List.of("Mois", "Annee")));

    var queryCondition = or(and(eq("a"), eq("b")), lt(5), like("a%"));
    q.withCondition("f1", queryCondition);
    q.withCondition("f2", gt(659));
    q.withCondition("f3", in(0, 1, 2));
    q.withCondition("f4", isNull());
    q.withCondition("f5", isNotNull());

    q.withHavingCriteria(all(criterion(price, ge(10)), criterion(expression, lt(100))));

    q.orderBy("a", OrderKeywordDto.ASC);
    q.orderBy("b", List.of("1", "l", "p"));

    BucketColumnSetDto columnSet = new BucketColumnSetDto("group", "scenario")
            .withNewBucket("a", List.of("a1", "a2"))
            .withNewBucket("b", List.of("b1", "b2"));
    q.withColumnSet(ColumnSetKey.BUCKET, columnSet);

    QueryDto subQuery = new QueryDto()
            .table(table)
            .withColumn("aa")
            .withMeasure(sum("sum_aa", "f"));
    q.table(subQuery);

    String name = "build-from-querydto.json"; // The content of this file is generated by the js code.
    File file = new File(getClass().getClassLoader().getResource(name).getFile());
    QueryDto qjs = JacksonUtil.deserialize(FileUtils.readFileToString(file, "UTF-8"), QueryDto.class);
    Assertions.assertThat(q.columnSets).isEqualTo(qjs.columnSets);
    Assertions.assertThat(q.columns).isEqualTo(qjs.columns);
    Assertions.assertThat(q.rollupColumns).isEqualTo(qjs.rollupColumns);
    Assertions.assertThat(q.context).isEqualTo(qjs.context);
    Assertions.assertThat(q.orders).isEqualTo(qjs.orders);
    Assertions.assertThat(q.measures).isEqualTo(qjs.measures);
    Assertions.assertThat(q.whereCriteriaDto).isEqualTo(qjs.whereCriteriaDto);
    Assertions.assertThat(q.table).isEqualTo(qjs.table);
    Assertions.assertThat(q.subQuery).isEqualTo(qjs.subQuery);
    Assertions.assertThat(q).isEqualTo(qjs);
  }


  /**
   * This is test is building the {@link QueryDto} <b>with</b> by using the query builder {@link Query}.
   */
  @Test
  void testReadJsonBuildFromQuery() throws IOException {
    var table = new TableDto("myTable");
    var refTable = new TableDto("refTable");
    var cte = new VirtualTableDto("myCte", List.of("id", "min", "max", "other"), List.of(List.of(0, 0, 1, "x"), List.of(1, 2, 3, "y")));

    BucketColumnSetDto bucketColumnSet = new BucketColumnSetDto("group", "scenario")
            .withNewBucket("a", List.of("a1", "a2"))
            .withNewBucket("b", List.of("b1", "b2"));

    Measure measure = sum("sum", "f1");
    Measure measureExpr = new ExpressionMeasure("sum_expr", "sum(f1)");
    QueryDto q = Query.from(table.name)
            .join(refTable.name, JoinType.INNER)
            .on(all(criterion("myTable" + ".id", "refTable" + ".id", ConditionType.EQ),
                    criterion("myTable" + ".a", "refTable" + ".a", ConditionType.EQ)))
            .join(cte, JoinType.INNER)
            .on(all(criterion("myTable.value", "myCte.min", ConditionType.GE), criterion("myTable.value", "myCte.max", ConditionType.LT)))
            .where("f2", gt(659))
            .where("f3", eq(123))
            .select(List.of("a", "b"),
                    List.of(bucketColumnSet),
                    List.of(measure, avg("sum", "f1"), measureExpr))
            .rollup("a", "b")
            .having(all(criterion((BasicMeasure) measure, gt(0)), criterion((BasicMeasure) measureExpr, lt(10))))
            .orderBy("f4", OrderKeywordDto.ASC)
            .limit(10)
            .build();

    String name = "build-from-query.json"; // The content of this file is generated by the js code.
    File file = new File(getClass().getClassLoader().getResource(name).getFile());
    QueryDto qjs = JacksonUtil.deserialize(FileUtils.readFileToString(file, "UTF-8"), QueryDto.class);
    Assertions.assertThat(q.columnSets).isEqualTo(qjs.columnSets);
    Assertions.assertThat(q.columns).isEqualTo(qjs.columns);
    Assertions.assertThat(q.rollupColumns).isEqualTo(qjs.rollupColumns);
    Assertions.assertThat(q.context).isEqualTo(qjs.context);
    Assertions.assertThat(q.orders).isEqualTo(qjs.orders);
    Assertions.assertThat(q.measures).isEqualTo(qjs.measures);
    Assertions.assertThat(q.whereCriteriaDto).isEqualTo(qjs.whereCriteriaDto);
    Assertions.assertThat(q.table).isEqualTo(qjs.table);
    Assertions.assertThat(q.subQuery).isEqualTo(qjs.subQuery);
    Assertions.assertThat(q.limit).isEqualTo(qjs.limit);
    Assertions.assertThat(q.virtualTableDto).isEqualTo(qjs.virtualTableDto);
    Assertions.assertThat(q).isEqualTo(qjs);
  }

  @Test
  void testReadJsonBuildFromQueryMerge() throws IOException {
    var table = new TableDto("myTable");
    QueryDto q1 = Query.from(table.name)
            .select(List.of("a", "b"),
                    List.of(sum("sum", "f1")))
            .build();
    QueryDto q2 = Query.from(table.name)
            .select(List.of("a", "b"),
                    List.of(avg("sum", "f1")))
            .build();

    QueryMergeDto query = new QueryMergeDto(q1, q2, JoinType.LEFT);

    String name = "build-from-query-merge.json"; // The content of this file is generated by the js code.
    File file = new File(getClass().getClassLoader().getResource(name).getFile());
    QueryMergeDto qjs = JacksonUtil.deserialize(FileUtils.readFileToString(file, "UTF-8"), QueryMergeDto.class);
    Assertions.assertThat(qjs).isEqualTo(query);
  }
}
