package com.github.easyjpa.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.github.easyjpa.CaseWhenExpression;
import com.github.easyjpa.ColumnList;
import com.github.easyjpa.FieldList;
import com.github.easyjpa.Fields;
import com.github.easyjpa.IfExpression;
import com.github.easyjpa.JpaProviders;
import com.github.easyjpa.JpaSort;
import com.github.easyjpa.Property;
import com.github.easyjpa.Restrictions;
import com.github.easyjpa.test.dao.ProductDao;
import com.github.easyjpa.test.entity.Product;
import com.github.easyjpa.test.service.ProductService;
import jakarta.persistence.Tuple;

/**
 *
 * Tests of the expressions built by Fields, IfExpression and CaseWhenExpression.
 *
 * @Description: FieldsTests
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class FieldsTests extends AbstractDaoTests {

    private static final Logger log = LoggerFactory.getLogger(FieldsTests.class);

    /** See ProductService#saveRandomProducts, the cheapest is 10 and the dearest is 320. */
    private static final long TOTAL_PRODUCTS = 12L;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductDao productDao;

    @BeforeAll
    public void begin() {
        log.info("=========== FieldsTests Begin. ===========");
        productService.saveRandomProducts();
    }

    /** The aggregations of the whole table, which is a query without group by. */
    @Test
    public void testAggregations() {
        Tuple tuple = productDao.customQuery()
                .select(new ColumnList().addColumns(Fields.count(Product::getId).as("total"),
                        Fields.countDistinct(Property.forName(Product::getLocation))
                                .as("totalLocations"),
                        Fields.sum(Product::getPrice).as("totalPrice"),
                        Fields.avg(Product::getPrice).as("avgPrice"),
                        Fields.max(Product::getPrice).as("maxPrice"),
                        Fields.min(Product::getPrice).as("minPrice")))
                .first();
        assertEquals(TOTAL_PRODUCTS, ((Number) tuple.get("total")).longValue());
        assertEquals(8L, ((Number) tuple.get("totalLocations")).longValue());
        assertEquals(0, new BigDecimal("1482").compareTo((BigDecimal) tuple.get("totalPrice")));
        assertEquals(123.5d, ((Number) tuple.get("avgPrice")).doubleValue(), 0.01d);
        assertEquals(0, new BigDecimal("320").compareTo((BigDecimal) tuple.get("maxPrice")));
        assertEquals(0, new BigDecimal("10").compareTo((BigDecimal) tuple.get("minPrice")));
    }

    /** The same aggregations addressed by an alias and an attribute name. */
    @Test
    public void testAggregationsByAttributeName() {
        Tuple tuple = productDao.customQuery()
                .select(new ColumnList().addColumns(Fields.count(1).as("total"),
                        Fields.sum("price", BigDecimal.class).as("totalPrice"),
                        Fields.avg("price", BigDecimal.class).as("avgPrice"),
                        Fields.max("price", BigDecimal.class).as("maxPrice"),
                        Fields.min("price", BigDecimal.class).as("minPrice")))
                .first();
        assertEquals(TOTAL_PRODUCTS, ((Number) tuple.get("total")).longValue());
        assertEquals(0, new BigDecimal("1482").compareTo((BigDecimal) tuple.get("totalPrice")));
        assertEquals(123.5d, ((Number) tuple.get("avgPrice")).doubleValue(), 0.01d);
        assertEquals(0, new BigDecimal("320").compareTo((BigDecimal) tuple.get("maxPrice")));
        assertEquals(0, new BigDecimal("10").compareTo((BigDecimal) tuple.get("minPrice")));
    }

    /** The arithmetic between an attribute and a value. */
    @Test
    public void testArithmeticByValue() {
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addColumns(
                        Fields.plusValue(Product::getPrice, 20).as("plus"),
                        Fields.minusValue(Product::getPrice, 20).as("minus"),
                        Fields.multiplyValue(Product::getPrice, 2).as("multiply"),
                        Fields.divideValue(Product::getPrice, 2).as("divide")))
                .first();
        assertEquals(100d, ((Number) tuple.get("plus")).doubleValue(), 0.01d);
        assertEquals(60d, ((Number) tuple.get("minus")).doubleValue(), 0.01d);
        assertEquals(160d, ((Number) tuple.get("multiply")).doubleValue(), 0.01d);
        assertEquals(40d, ((Number) tuple.get("divide")).doubleValue(), 0.01d);
    }

    /** The arithmetic between two attributes. */
    @Test
    public void testArithmeticByAttribute() {
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addColumns(
                        Fields.plus(Product::getPrice, Product::getPrice).as("plus"),
                        Fields.minus(Product::getPrice, Product::getPrice).as("minus"),
                        Fields.multiply(Product::getPrice, Product::getDiscount).as("multiply"),
                        Fields.divide(Property.forName(Product::getPrice),
                                Property.forName(Product::getPrice)).as("divide")))
                .first();
        assertEquals(0, new BigDecimal("160").compareTo((BigDecimal) tuple.get("plus")));
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) tuple.get("minus")));
        assertEquals(72d, ((Number) tuple.get("multiply")).doubleValue(), 0.01d);
        assertEquals(1d, ((Number) tuple.get("divide")).doubleValue(), 0.01d);
    }

    @Test
    public void testMathFunctions() {
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addColumns(
                        Fields.abs(Fields.neg(Property.forName(Product::getPrice))).as("abs"),
                        Fields.ceil(Product::getDiscount).as("ceil"),
                        Fields.floor(Product::getDiscount).as("floor"),
                        Fields.round(Product::getDiscount, 0).as("round"),
                        Fields.sqrt(Product::getPrice).as("sqrt"),
                        Fields.sign(Product::getPrice).as("sign"),
                        Fields.mod(Fields.toInteger(12), 5).as("mod")))
                .first();
        assertEquals(0, new BigDecimal("80").compareTo((BigDecimal) tuple.get("abs")));
        assertEquals(1, ((Number) tuple.get("ceil")).intValue());
        assertEquals(0, ((Number) tuple.get("floor")).intValue());
        assertEquals(1, ((Number) tuple.get("round")).intValue());
        assertEquals(8.94d, ((Number) tuple.get("sqrt")).doubleValue(), 0.01d);
        assertEquals(1, ((Number) tuple.get("sign")).intValue());
        assertEquals(2, ((Number) tuple.get("mod")).intValue());
        assertEquals(2, ((Number) tuple.get("mod")).intValue());
    }

    @Test
    public void testStringFunctions() {
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addColumns(Fields.upper(Product::getName).as("upper"),
                        Fields.lower(Product::getName).as("lower"),
                        Fields.length(Product::getName).as("length"),
                        Fields.substring(Product::getName, 1, 3).as("substring"),
                        Fields.concat(Product::getName, "!").as("concat"),
                        Fields.concat("[", Property.forName(Product::getName)).as("prefixed")))
                .first();
        assertEquals("JUICER", tuple.get("upper"));
        assertEquals("juicer", tuple.get("lower"));
        assertEquals(6, tuple.get("length"));
        assertEquals("Jui", tuple.get("substring"));
        assertEquals("Juicer!", tuple.get("concat"));
        assertEquals("[Juicer", tuple.get("prefixed"));
    }

    /** coalesce replaces the null values whereas nullif turns a value into null. */
    @Test
    public void testCoalesceAndNullif() {
        Tuple tuple = productDao.customQuery()
                .filter(Restrictions.eq(Product::getName, "Electric fan"))
                .select(new ColumnList()
                        .addColumns(Fields.coalesce(Product::getDiscount, BigDecimal.ONE)
                                .as("discount"))
                        .addColumns(Fields.nullif(Product::getLocation, "Singapore")
                                .as("location")))
                .first();
        assertEquals(0, BigDecimal.ONE.compareTo((BigDecimal) tuple.get("discount")));
        assertEquals(null, tuple.get("location"));
    }

    /** A literal and the current date time of the database. */
    @Test
    public void testLiteralAndCurrentDateTime() {
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList()
                        .addColumns(Fields.toLiteral("easyjpa").as("literal"),
                                Fields.toInteger(1).as("one"))
                        .addColumns(Fields.currentDate().as("currentDate"),
                                Fields.currentTime().as("currentTime"),
                                Fields.currentTimestamp().as("currentTimestamp"),
                                Fields.localDate().as("localDate"),
                                Fields.localTime().as("localTime"),
                                Fields.localDateTime().as("localDateTime")))
                .first();
        assertEquals("easyjpa", tuple.get("literal"));
        assertEquals(1, ((Number) tuple.get("one")).intValue());
        assertNotNull(tuple.get("currentDate"));
        assertNotNull(tuple.get("currentTime"));
        assertNotNull(tuple.get("currentTimestamp"));
        assertEquals(LocalDate.now(), tuple.get("localDate"));
        assertNotNull(tuple.get("localTime"));
        assertNotNull(tuple.get("localDateTime"));
    }

    /** A typed literal makes a constant column. */
    @Test
    public void testTypedLiterals() {
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addColumns(Fields.toString('A').as("asString"),
                        Fields.toInteger(1).as("asInteger"), Fields.toLong(2L).as("asLong"),
                        Fields.toDouble(3d).as("asDouble"), Fields.toFloat(4f).as("asFloat"),
                        Fields.toBigDecimal(BigDecimal.TEN).as("asBigDecimal"),
                        Fields.toBigInteger(BigInteger.TWO).as("asBigInteger")))
                .first();
        assertEquals("A", tuple.get("asString"));
        assertEquals(1, ((Number) tuple.get("asInteger")).intValue());
        assertEquals(2L, ((Number) tuple.get("asLong")).longValue());
        assertEquals(3d, ((Number) tuple.get("asDouble")).doubleValue(), 0.01d);
        assertEquals(4f, ((Number) tuple.get("asFloat")).floatValue(), 0.01f);
        assertEquals(10d, ((Number) tuple.get("asBigDecimal")).doubleValue(), 0.01d);
        assertEquals(2L, ((Number) tuple.get("asBigInteger")).longValue());
    }

    /** The comparisons of Fields make a boolean column instead of a filter. */
    @Test
    public void testComparisonFields() {
        assumeTrue(JpaProviders.getProvider().supportsComparisonAsSelection(),
                "The provider selects no comparison as a column");
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addColumns(
                        Fields.gt(Product::getPrice, BigDecimal.TEN).as("dearer"),
                        Fields.gte(Product::getPrice, BigDecimal.TEN).as("notCheaper"),
                        Fields.lt(Product::getPrice, BigDecimal.TEN).as("cheaper"),
                        Fields.lte(Product::getPrice, BigDecimal.TEN).as("notDearer"),
                        Fields.eq(Product::getName, "Juicer").as("named"),
                        Fields.ne(Product::getName, "Juicer").as("notNamed")))
                .first();
        assertEquals(true, tuple.get("dearer"));
        assertEquals(true, tuple.get("notCheaper"));
        assertEquals(false, tuple.get("cheaper"));
        assertEquals(false, tuple.get("notDearer"));
        assertEquals(true, tuple.get("named"));
        assertEquals(false, tuple.get("notNamed"));
    }

    /** Select the root entity itself as one column, which is what Fields#root does. */
    @Test
    public void testSelectRoot() {
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addFields(Fields.root())).first();
        assertEquals("Juicer", ((Product) tuple.get(0)).getName());
    }

    /** An if expression maps the values of one attribute to the other ones. */
    @Test
    public void testIfExpression() {
        IfExpression<String, String> area = new IfExpression<String, String>(Product::getLocation)
                .when("China", "Asia").when("Japan", "Asia").when("Australia", "Oceania")
                .otherwise("Other");
        long asia = productDao.customQuery().select(new ColumnList().addColumns(area.as("area")))
                .list().stream().filter(t -> "Asia".equals(t.get("area"))).count();
        assertEquals(3L, asia);
    }

    /** A case when expression branches on the conditions rather than on one attribute. */
    @Test
    public void testCaseWhenExpression() {
        CaseWhenExpression<String> level = new CaseWhenExpression<String>()
                .when(Fields.gte(Product::getPrice, BigDecimal.valueOf(200)), "high")
                .when(Fields.gte(Product::getPrice, BigDecimal.valueOf(100)), "middle")
                .otherwise("low");
        long high = productDao.customQuery()
                .select(new ColumnList().addColumns(level.as("level"))).list().stream()
                .filter(t -> "high".equals(t.get("level"))).count();
        assertEquals(3L, high);

        CaseWhenExpression<BigDecimal> discount = new CaseWhenExpression<BigDecimal>()
                .when(Fields.gte(Product::getPrice, BigDecimal.valueOf(200)),
                        Property.forName(Product::getDiscount))
                .otherwise(Fields.toBigDecimal(BigDecimal.ONE));
        assertEquals(TOTAL_PRODUCTS, productDao.customQuery()
                .select(new ColumnList().addColumns(discount.as("discount"))).list().size());
    }

    /** Sort by an attribute, by an expression and by the position of a selected column. */
    @Test
    public void testSorts() {
        assumeTrue(JpaProviders.getProvider().supportsOrdinalSort(), "The provider sorts by no column position");
        assertEquals("Flashlight", productDao.customQuery().sort(JpaSort.asc(Product::getPrice))
                .select(new ColumnList(Product::getName)).first().get(0));
        assertEquals("Microwave oven", productDao.customQuery().sort(JpaSort.desc("price"))
                .select(new ColumnList(Product::getName)).first().get(0));
        assertEquals("Microwave oven", productDao.customQuery().sort(JpaSort.desc("this", "price"))
                .select(new ColumnList(Product::getName)).first().get(0));
        assertEquals("Flashlight", productDao.customQuery().sort(JpaSort.asc("this", "price"))
                .select(new ColumnList(Product::getName)).first().get(0));
        assertEquals("Microwave oven",
                productDao.customQuery().sort(JpaSort.desc(Fields.multiplyValue(Product::getPrice, 2)))
                        .select(new ColumnList(Product::getName)).first().get(0));
        assertEquals("Microwave oven", productDao.customQuery().sort(JpaSort.desc(2))
                .select(new ColumnList(Product::getName, Product::getPrice)).first().get(0));
        assertEquals("Flashlight", productDao.customQuery().sort(JpaSort.asc(2))
                .select(new ColumnList(Product::getName, Product::getPrice)).first().get(0));
    }


    /** The aggregations addressed by an alias, which is what a subquery needs. */
    @Test
    public void testAggregationsByAlias() {
        Tuple tuple = productDao.customQuery()
                .select(new ColumnList().addColumns(Fields.count("this", "id").as("total"),
                        Fields.countDistinct("this", "location").as("totalLocations"),
                        Fields.sum("this", "price", BigDecimal.class).as("totalPrice"),
                        Fields.avg("this", "price", BigDecimal.class).as("avgPrice"),
                        Fields.max("this", "price", BigDecimal.class).as("maxPrice"),
                        Fields.min("this", "price", BigDecimal.class).as("minPrice")))
                .first();
        assertEquals(TOTAL_PRODUCTS, ((Number) tuple.get("total")).longValue());
        assertEquals(8L, ((Number) tuple.get("totalLocations")).longValue());
        assertEquals(0, new BigDecimal("1482").compareTo((BigDecimal) tuple.get("totalPrice")));
        assertEquals(123.5d, ((Number) tuple.get("avgPrice")).doubleValue(), 0.01d);
        assertEquals(0, new BigDecimal("320").compareTo((BigDecimal) tuple.get("maxPrice")));
        assertEquals(0, new BigDecimal("10").compareTo((BigDecimal) tuple.get("minPrice")));
    }

    /** The arithmetic between two Fields, and the division whose dividend is a value. */
    @Test
    public void testArithmeticByFields() {
        Property<BigDecimal> price = Property.forName(Product::getPrice);
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addColumns(Fields.plus(price, price).as("plus"),
                        Fields.minus(price, price).as("minus"),
                        Fields.multiply(price, price).as("multiply"),
                        Fields.divide(BigDecimal.valueOf(160), price).as("divide")))
                .first();
        assertEquals(0, new BigDecimal("160").compareTo((BigDecimal) tuple.get("plus")));
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) tuple.get("minus")));
        assertEquals(0, new BigDecimal("6400").compareTo((BigDecimal) tuple.get("multiply")));
        assertEquals(2d, ((Number) tuple.get("divide")).doubleValue(), 0.01d);
    }

    /** The same functions taking a Field instead of a lambda. */
    @Test
    public void testFunctionsByField() {
        Property<BigDecimal> price = Property.forName(Product::getPrice);
        Property<String> name = Property.forName(Product::getName);
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addColumns(Fields.abs(price).as("abs"),
                        Fields.ceil(price).as("ceil"), Fields.floor(price).as("floor"),
                        Fields.sign(price).as("sign"), Fields.sqrt(price).as("sqrt"),
                        Fields.upper(name).as("upper"), Fields.lower(name).as("lower"),
                        Fields.length(name).as("length"),
                        Fields.substring(name, 1, 3).as("substring"),
                        Fields.coalesce(Property.forName(Product::getDiscount), BigDecimal.ONE)
                                .as("discount")))
                .first();
        assertEquals(0, new BigDecimal("80").compareTo((BigDecimal) tuple.get("abs")));
        assertEquals(80, ((Number) tuple.get("ceil")).intValue());
        assertEquals(80, ((Number) tuple.get("floor")).intValue());
        assertEquals(1, ((Number) tuple.get("sign")).intValue());
        assertEquals(8.94d, ((Number) tuple.get("sqrt")).doubleValue(), 0.01d);
        assertEquals("JUICER", tuple.get("upper"));
        assertEquals("juicer", tuple.get("lower"));
        assertEquals(6, tuple.get("length"));
        assertEquals("Jui", tuple.get("substring"));
        assertEquals(0, new BigDecimal("0.9").compareTo((BigDecimal) tuple.get("discount")));
    }

    /** A FieldList takes the attribute names, the lambdas or the Fields. */
    @Test
    public void testFieldListAddressing() {
        assertEquals(8, groupAmountOf(new FieldList("location")));
        assertEquals(8, groupAmountOf(new FieldList("this", new String[] {"location"})));
        assertEquals(8, groupAmountOf(new FieldList(Product::getLocation)));
        assertEquals(8, groupAmountOf(new FieldList(Property.forName(Product::getLocation))));
        assertEquals(8, groupAmountOf(new FieldList().addField("location")));
        assertEquals(8, groupAmountOf(new FieldList().addField("this", "location")));
        assertEquals(8, groupAmountOf(new FieldList().addField("location", String.class)));
        assertEquals(8, groupAmountOf(new FieldList().addFields(Product::getLocation)));
        assertEquals(8,
                groupAmountOf(new FieldList().addFields(Property.forName(Product::getLocation))));
    }

    private int groupAmountOf(FieldList fieldList) {
        return productDao.customQuery().groupBy(fieldList)
                .select(new ColumnList().addColumns(Fields.count(1).as("total"))).list().size();
    }

    /** A ColumnList takes the attribute names, the lambdas, the Fields or a table alias. */
    @Test
    public void testColumnListAddressing() {
        assertEquals("Juicer", firstValueOf(new ColumnList(new String[] {"name"})));
        assertEquals("Juicer", firstValueOf(new ColumnList("this", new String[] {"name"})));
        assertEquals("Juicer", firstValueOf(new ColumnList(Product::getName)));
        assertEquals("Juicer", firstValueOf(new ColumnList(Property.forName(Product::getName))));
        assertEquals("Juicer", firstValueOf(new ColumnList().addColumn("name")));
        assertEquals("Juicer", firstValueOf(new ColumnList().addColumn("this", "name")));
        assertEquals("Juicer", firstValueOf(new ColumnList().addColumn("name", String.class)));
        assertEquals("Juicer",
                firstValueOf(new ColumnList().addColumn("this", "name", String.class)));
        assertEquals("Juicer", firstValueOf(new ColumnList().addColumns(Product::getName)));
        assertEquals("Juicer", ((Product) productDao.customQuery()
                .filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addTableAlias("this")).first().get(0)).getName());
    }

    private Object firstValueOf(ColumnList columnList) {
        return productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(columnList).first().get(0);
    }

    /** The group by clause takes the attribute names, the lambdas or the Fields as well. */
    @Test
    public void testGroupByOverloads() {
        assertEquals(8, productDao.customQuery().groupBy("location")
                .select(new ColumnList().addColumns(Fields.count(1).as("total"))).list().size());
        assertEquals(8, productDao.customQuery().groupBy("this", new String[] {"location"})
                .select(new ColumnList().addColumns(Fields.count(1).as("total"))).list().size());
        assertEquals(8, productDao.customQuery().groupBy(Product::getLocation)
                .select(new ColumnList().addColumns(Fields.count(1).as("total"))).list().size());
        assertEquals(8, productDao.customQuery()
                .groupBy(Property.forName(Product::getLocation))
                .select(new ColumnList().addColumns(Fields.count(1).as("total"))).list().size());
    }


    /** The remaining overloads of the functions, which take a Field or a value. */
    @Test
    public void testRemainingOverloads() {
        Property<String> name = Property.forName(Product::getName);
        Property<BigDecimal> price = Property.forName(Product::getPrice);
        Property<BigDecimal> discount = Property.forName(Product::getDiscount);
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addColumns(Fields.abs(Product::getPrice).as("abs"),
                        Fields.substring(Product::getName, 2).as("substringByLambda"),
                        Fields.substring(name, 2).as("substringByField"),
                        Fields.substring(name, Fields.toInteger(3)).as("substringByFields"),
                        Fields.concat(Product::getName, "!").as("concatByLambda"),
                        Fields.concat("[", Product::getName).as("concatValueAndLambda"),
                        Fields.concat(name, name).as("concatFields"),
                        Fields.coalesce(discount, Fields.toBigDecimal(BigDecimal.ONE))
                                .as("coalesceFields"),
                        Fields.nullif(name, "Juicer").as("nullifField"),
                        Fields.mod(Fields.toInteger(12), Fields.toInteger(5)).as("modFields"),
                        Fields.mod(12, Fields.toInteger(5)).as("modValueAndField"),
                        Fields.plusValue(price, BigDecimal.TEN).as("plusValue"),
                        Fields.minusValue(price, BigDecimal.TEN).as("minusValue"),
                        Fields.multiplyValue(price, BigDecimal.TEN).as("multiplyValue"),
                        Fields.divideValue(price, BigDecimal.TEN).as("divideValue")))
                .first();
        assertEquals(0, new BigDecimal("80").compareTo((BigDecimal) tuple.get("abs")));
        assertEquals("uicer", tuple.get("substringByLambda"));
        assertEquals("uicer", tuple.get("substringByField"));
        assertEquals("icer", tuple.get("substringByFields"));
        assertEquals("Juicer!", tuple.get("concatByLambda"));
        assertEquals("[Juicer", tuple.get("concatValueAndLambda"));
        assertEquals("JuicerJuicer", tuple.get("concatFields"));
        assertEquals(0, new BigDecimal("0.9").compareTo((BigDecimal) tuple.get("coalesceFields")));
        assertEquals(null, tuple.get("nullifField"));
        assertEquals(2, ((Number) tuple.get("modFields")).intValue());
        assertEquals(2, ((Number) tuple.get("modValueAndField")).intValue());
        assertEquals(0, new BigDecimal("90").compareTo((BigDecimal) tuple.get("plusValue")));
        assertEquals(0, new BigDecimal("70").compareTo((BigDecimal) tuple.get("minusValue")));
        assertEquals(0, new BigDecimal("800").compareTo((BigDecimal) tuple.get("multiplyValue")));
        assertEquals(8d, ((Number) tuple.get("divideValue")).doubleValue(), 0.01d);
    }

    /** The counting overloads, including the ones addressed by an alias. */
    @Test
    public void testCountingOverloads() {
        Tuple tuple = productDao.customQuery()
                .select(new ColumnList().addColumns(
                        Fields.count(Property.forName(Product::getId)).as("byField"),
                        Fields.count("this", "id").as("byAlias"),
                        Fields.countDistinct(Product::getLocation).as("distinctByLambda"),
                        Fields.countDistinct("this", "location").as("distinctByAlias")))
                .first();
        assertEquals(TOTAL_PRODUCTS, ((Number) tuple.get("byField")).longValue());
        assertEquals(TOTAL_PRODUCTS, ((Number) tuple.get("byAlias")).longValue());
        assertEquals(8L, ((Number) tuple.get("distinctByLambda")).longValue());
        assertEquals(8L, ((Number) tuple.get("distinctByAlias")).longValue());
    }

    @AfterAll
    public void end() {
        log.info("=========== FieldsTests End. ===========");
    }

}
