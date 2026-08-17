package com.github.easyjpa.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.github.easyjpa.ColumnList;
import com.github.easyjpa.Fields;
import com.github.easyjpa.FilterList;
import com.github.easyjpa.Function;
import com.github.easyjpa.IfExpression;
import com.github.easyjpa.JpaSort;
import com.github.easyjpa.JpaSubQuery;
import com.github.easyjpa.Column;
import com.github.easyjpa.FieldList;
import com.github.easyjpa.JpaDelete;
import com.github.easyjpa.JpaPageResultSet;
import com.github.easyjpa.JpaQuery;
import com.github.easyjpa.Property;
import com.github.easyjpa.JpaQueryResultSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import com.github.easyjpa.JpaProviders;
import com.github.easyjpa.Restrictions;
import com.github.easyjpa.Transformers;
import com.github.easyjpa.page.PageRequest;
import com.github.easyjpa.test.dao.ProductDao;
import com.github.easyjpa.test.dao.StockDao;
import com.github.easyjpa.test.entity.Product;
import com.github.easyjpa.test.entity.Stock;
import com.github.easyjpa.test.service.ProductService;
import jakarta.persistence.Tuple;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @Description: ProductDaoTests
 * @Author: Fred Feng
 * @Date: 19/03/2025
 * @Version 1.0.0
 */
public class ProductDaoTests extends AbstractDaoTests {

    private static final Logger log = LoggerFactory.getLogger(ProductDaoTests.class);

    /** See ProductService#saveRandomProducts, which makes 12 products of 8 locations. */
    private static final long TOTAL_PRODUCTS = 12L;

    private static final long INITIAL_STOCK = 99999L;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private StockDao stockDao;

    @BeforeAll
    public void begin() {
        log.info("=========== ProductDaoTests Begin. ===========");
        productService.saveRandomProducts();
    }

    @Test
    public void test1() {
        productDao.query().selectThis().list().forEach(p -> {
            log.info(p.toString());
        });
    }

    @Test
    public void test2() {
        assumeTrue(JpaProviders.getProvider().supportsBeanProjection(), "The provider fills a bean through its constructor alone");
        List<ProductVo> dataList = new ArrayList<>();
        productDao.query(ProductVo.class)
                .filter(new FilterList().gte(Product::getPrice, BigDecimal.valueOf(100)).and()
                        .notNull(Product::getDiscount))
                .select(new ColumnList(Product::getName, Product::getPrice).addColumns(
                        Fields.multiply(Product::getPrice, Product::getDiscount).as("actualPrice")))
                .list().forEach(vo -> {
                    log.info(vo.toString());
                    dataList.add(vo);
                });
        AtomicInteger counter = new AtomicInteger();
        dataList.forEach(vo -> {
            if (vo.getPrice().compareTo(BigDecimal.valueOf(100)) >= 0
                    && vo.getActualPrice().compareTo(vo.getPrice()) < 0) {
                counter.incrementAndGet();
            }
        });
        assertTrue(counter.get() == dataList.size());
    }

    @Test
    public void test3() {
        assumeTrue(JpaProviders.getProvider().supportsBeanProjection(), "The provider fills a bean through its constructor alone");
        List<ProductVo> dataList = new ArrayList<>();
        productDao.query(ProductVo.class)
                .filter(new FilterList().gte(Product::getPrice, BigDecimal.valueOf(200))
                        .and(() -> new FilterList().eq(Product::getLocation, "Australia").or()
                                .eq(Product::getLocation, "Thailand")))
                .sort(JpaSort.desc(Fields.toInteger(4)))
                .select(new ColumnList(Product::getName, Product::getLocation, Product::getPrice)
                        .addColumns(Fields.multiply(Product::getPrice, Product::getDiscount)
                                .as("actualPrice")))
                .list().forEach(vo -> {
                    log.info(vo.toString());
                    dataList.add(vo);
                });
        AtomicInteger counter = new AtomicInteger();
        dataList.forEach(vo -> {
            if (vo.getPrice().compareTo(BigDecimal.valueOf(200)) >= 0
                    && ("Australia".equals(vo.getLocation())
                            || "Thailand".equals(vo.getLocation()))) {
                counter.incrementAndGet();
            }
        });
        assertTrue(counter.get() == dataList.size());
    }

    @Test
    public void test4() {
        assumeTrue(JpaProviders.getProvider().supportsPartialEntity(), "The provider builds no partial entity");
        List<ProductAggregationVo> dataList = new ArrayList<>();
        productDao.customQuery().groupBy(Product::getLocation)
                .sort(JpaSort.desc(Fields.avg(Product::getPrice)))
                .select(new ColumnList(Product::getLocation).addColumns(
                        Fields.max(Product::getPrice).as("maxPrice"),
                        Fields.min(Product::getPrice).as("minPrice"),
                        Fields.avg(Product::getPrice).as("avgPrice"), Fields.count(1).as("amount")))
                .setTransformer(Transformers.asBean(ProductAggregationVo.class)).list()
                .forEach(vo -> {
                    log.info(vo.toString());
                    dataList.add(vo);
                });
        int locationSize = productDao.query().distinct()
                .select(new ColumnList(Product::getLocation)).list().size();
        assertTrue(locationSize == dataList.size());
    }

    @Test
    public void test5() {
        List<ProductAggregationVo> dataList = new ArrayList<>();
        productDao.customQuery().groupBy(Product::getLocation)
                .having(Restrictions.gt(Fields.avg(Product::getPrice), 50d)).sort(JpaSort.desc(4))
                .select(new ColumnList(Product::getLocation).addColumns(
                        Fields.max(Product::getPrice).as("maxPrice"),
                        Fields.min(Product::getPrice).as("minPrice"),
                        Fields.avg(Product::getPrice).as("avgPrice"), Fields.count(1).as("amount")))
                .setTransformer(Transformers.asBean(ProductAggregationVo.class)).list()
                .forEach(vo -> {
                    log.info(vo.toString());
                    dataList.add(vo);
                });
        AtomicInteger counter = new AtomicInteger();
        dataList.forEach(vo -> {
            if (vo.getAvgPrice().doubleValue() > 50d) {
                counter.incrementAndGet();
            }
        });
        assertTrue(counter.get() == dataList.size());
    }

    @Test
    public void test6() {
        assumeTrue(JpaProviders.getProvider().supportsPartialEntity(), "The provider builds no partial entity");
        List<ProductAggregationVo> dataList = new ArrayList<>();
        ColumnList columnList = new ColumnList();
        columnList
                .addColumns(Fields.concat(Fields.concat(Fields.max("price", String.class), "/"),
                        Fields.min("price", String.class)).as("repr"))
                .addColumns(Product::getLocation);
        productDao.customQuery().groupBy(Product::getLocation).select(columnList)
                .setTransformer(Transformers.asBean(ProductAggregationVo.class)).list()
                .forEach(vo -> {
                    log.info(vo.toString());
                    dataList.add(vo);
                });
        int locationSize = productDao.query().distinct()
                .select(new ColumnList(Product::getLocation)).list().size();
        assertTrue(locationSize == dataList.size());
    }

    @Test
    public void test7() {
        assumeTrue(JpaProviders.getProvider().supportsPassThroughFunction(), "The provider passes no database function through");
        List<Tuple> dataList = new ArrayList<>();
        ColumnList columnList = new ColumnList().addColumns(
                Function.build("LOWER", String.class, Product::getName).as("name"),
                Function.build("UPPER", String.class, Product::getLocation).as("location"));
        productDao.customQuery().select(columnList).list(10).forEach(t -> {
            log.info(t.toString());
            dataList.add(t);
        });
        assertTrue(dataList.size() <= 10);
    }

    @Test
    public void test8() {
        List<Tuple> dataList = new ArrayList<>();
        IfExpression<String, String> ifExpression =
                new IfExpression<String, String>(Product::getLocation).when("Indonesia", "Asia")
                        .when("Japan", "Asia").when("China", "Asia").when("Singapore", "Asia")
                        .when("Vietnam", "Asia").when("Thailand", "Asia")
                        .when("Australia", "Oceania").when("New Zealand", "Oceania")
                        .otherwise("Other");
        ColumnList columnList = new ColumnList().addColumns(ifExpression.as("area"))
                .addColumns(Product::getLocation);
        productDao.customQuery().select(columnList).list().forEach(t -> {
            log.info(t.toString());
            dataList.add(t);
        });
        assertTrue(dataList.stream().map(t -> t.get("area")).distinct().count() == 2);
    }

    @Test
    public void test9() {
        List<String> locations = List.of("Australia", "New Zealand", "Thailand");
        List<ProductStockVo> dataList = new ArrayList<>();
        productDao.customPage().crossJoin(Stock.class, "a")
                .filter(new FilterList().eq(Stock::getProductId, Product::getId).and()
                        .in(Product::getLocation, locations))
                .select(new ColumnList()
                        .addColumns(Product::getId, Product::getName, Product::getLocation)
                        .addColumns(Stock::getAmount))
                .setTransformer(Transformers.asBean(ProductStockVo.class))
                .paginate(PageRequest.of(10)).forEachPage(eachPage -> {
                    log.info(String.format(
                            "====================== PageNumber/TotalPage: %s/%s  Total Records: %s =====================",
                            eachPage.getPageNumber(), eachPage.getTotalPages(),
                            eachPage.getTotalRecords()));
                    eachPage.getContent().forEach(vo -> {
                        log.info(vo.toString());
                        dataList.add(vo);
                    });
                });
        AtomicInteger counter = new AtomicInteger();
        dataList.forEach(vo -> {
            if (locations.contains(vo.getLocation())) {
                counter.incrementAndGet();
            }
        });
        assertTrue(counter.get() == dataList.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Australia", "New Zealand"})
    public void test10(String location) {
        JpaSubQuery<Product, Long> subQuery = stockDao.update().subQuery(Product.class, Long.class)
                .filter(Restrictions.eq(Product::getLocation, location)).select(Product::getId);
        long rows = stockDao.count(Restrictions.in(Stock::getProductId, subQuery));

        int updatedRows = stockDao.update()
                .setField(Stock::getAmount, Fields.plusValue(Stock::getAmount, 1000))
                .filter(Restrictions.in(Stock::getProductId, subQuery)).execute();
        log.info("Affected rows: {}", updatedRows);
        assertTrue(rows == updatedRows);
    }

    @Test
    public void test11() {
        Long productId = stockDao.query(Long.class).sort(JpaSort.desc(Stock::getAmount))
                .select(new ColumnList(Stock::getProductId)).first();
        int expectedRows = productId != null && productId > 0 ? 1 : 0;
        int affectedRows = productDao.update()
                .set(Product::getPrice, BigDecimal.valueOf(1000), Product::getDiscount,
                        BigDecimal.valueOf(0.8f), Product::getProduceDate, LocalDate.now())
                .filter(Restrictions.eq(Product::getId, productId)).execute();
        log.info("Affected rows: {}", affectedRows);
        assertTrue(affectedRows == expectedRows);
    }


    /** The counting of a grouping pagination counts the groups rather than the rows. */
    @Test
    public void testGroupCountBySingleKey() throws Exception {
        JpaPageResultSet<Tuple> resultSet = productDao.customPage()
                .groupBy(new FieldList(Product::getLocation))
                .select(new ColumnList().addColumns(Product::getLocation)
                        .addColumns(Fields.count(Product::getId).as("total")));
        assertEquals(8L, resultSet.rowCount());
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
    }

    /** The null values of a group-by key make a group of their own. */
    @Test
    public void testGroupCountByNullableKey() throws Exception {
        JpaPageResultSet<Tuple> resultSet = productDao.customPage()
                .groupBy(new FieldList(Product::getDiscount))
                .select(new ColumnList().addColumns(Product::getDiscount)
                        .addColumns(Fields.count(Product::getId).as("total")));
        // 0.9, 0.85, 0.8 and null
        assertEquals(4L, resultSet.rowCount());
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
    }

    @Test
    public void testGroupCountByMultipleKeys() throws Exception {
        JpaPageResultSet<Tuple> resultSet = productDao.customPage()
                .groupBy(new FieldList(Product::getLocation, Product::getDiscount))
                .select(new ColumnList().addColumns(Product::getLocation, Product::getDiscount)
                        .addColumns(Fields.count(Product::getId).as("total")));
        assertEquals(TOTAL_PRODUCTS, resultSet.rowCount());
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
    }

    /** Group by the keys of different types, a date one and a string one. */
    @Test
    public void testGroupCountByMultipleKeyTypes() throws Exception {
        JpaPageResultSet<Tuple> resultSet = productDao.customPage()
                .groupBy(new FieldList(Product::getProduceDate, Product::getLocation))
                .select(new ColumnList().addColumns(Product::getProduceDate, Product::getLocation)
                        .addColumns(Fields.count(Product::getId).as("total")));
        assertEquals(TOTAL_PRODUCTS, resultSet.rowCount());
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
    }

    /** A having clause filters the groups after aggregating. */
    @Test
    public void testGroupCountWithHaving() throws Exception {
        JpaPageResultSet<Tuple> resultSet = productDao.customPage()
                .groupBy(new FieldList(Product::getLocation))
                .having(Restrictions.gt(Fields.count(Product::getId), 1L))
                .select(new ColumnList().addColumns(Product::getLocation)
                        .addColumns(Fields.count(Product::getId).as("total")));
        // Australia(3), China(2) and Thailand(2)
        assertEquals(3L, resultSet.rowCount());
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
    }

    @Test
    public void testGroupCountUponCrossJoin() throws Exception {
        JpaPageResultSet<Tuple> resultSet = productDao.customPage().crossJoin(Stock.class, "s")
                .filter(new FilterList().eq(Stock::getProductId, Product::getId))
                .groupBy(new FieldList(Product::getLocation))
                .select(new ColumnList().addColumns(Product::getLocation)
                        .addColumns(Fields.sum(Stock::getAmount).as("totalAmount")));
        assertEquals(8L, resultSet.rowCount());
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
    }

    /** The counting query keeps reusable, no matter how many times it gets executed. */
    @Test
    public void testRepeatedRowCount() throws Exception {
        JpaPageResultSet<Tuple> resultSet = productDao.customPage()
                .filter(Restrictions.notNull(Product::getLocation))
                .groupBy(new FieldList(Product::getLocation))
                .select(new ColumnList().addColumns(Product::getLocation)
                        .addColumns(Fields.count(Product::getId).as("total")));
        assertEquals(8L, resultSet.rowCount());
        assertEquals(8L, resultSet.rowCount());
        assertEquals(8, resultSet.list().size());
        assertEquals(8L, resultSet.rowCount());
    }

    /** A non grouping pagination counts the rows it lists. */
    @Test
    public void testPaginationRowCount() throws Exception {
        JpaPageResultSet<Tuple> resultSet = productDao.customPage()
                .filter(Restrictions.gte(Product::getPrice, BigDecimal.valueOf(100)))
                .select(new ColumnList(Product::getName, Product::getPrice));
        assertEquals(7L, resultSet.rowCount());
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
    }

    /** Call the sql functions which the Criteria API does not cover. */
    @Test
    public void testCustomFunction() {
        assumeTrue(JpaProviders.getProvider().supportsPassThroughFunction(), "The provider passes no database function through");
        List<Tuple> dataList = productDao.customQuery()
                .filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList()
                        .addColumns(Function.build("LOWER", String.class, "name").as("lowerName"))
                        .addColumns(Function.build("CONCAT", String.class, "name", "location")
                                .as("nameAndLocation"))
                        .addColumns(Function
                                .build("IFNULL", BigDecimal.class, Property.forName(null, "discount"),
                                        Fields.toBigDecimal(BigDecimal.ONE))
                                .as("realDiscount")))
                .list();
        assertEquals(1, dataList.size());
        assertEquals("juicer", dataList.get(0).get("lowerName"));
        assertEquals("JuicerIndonesia", dataList.get(0).get("nameAndLocation"));
    }

    @Test
    public void testStringFunctions() {
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addColumns(Fields.upper(Product::getName).as("upperName"),
                        Fields.lower(Product::getLocation).as("lowerLocation"),
                        Fields.length(Product::getName).as("nameLength"),
                        Fields.substring(Product::getName, 1, 3).as("shortName"),
                        Fields.concat(Fields.concat(Product::getName, "@"),
                                Property.forName(Product::getLocation)).as("fullName")))
                .first();
        assertEquals("JUICER", tuple.get("upperName"));
        assertEquals("indonesia", tuple.get("lowerLocation"));
        assertEquals(6, tuple.get("nameLength"));
        assertEquals("Jui", tuple.get("shortName"));
        assertEquals("Juicer@Indonesia", tuple.get("fullName"));
    }

    @Test
    public void testArithmeticFunctions() {
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addColumns(
                        Fields.plusValue(Product::getPrice, 20).as("plusPrice"),
                        Fields.minusValue(Product::getPrice, 20).as("minusPrice"),
                        Fields.multiplyValue(Product::getPrice, 2).as("doublePrice"),
                        Fields.divideValue(Product::getPrice, 2).as("halfPrice"),
                        Fields.abs(Fields.neg(Product::getPrice)).as("absPrice"),
                        Fields.sqrt(Product::getPrice).as("sqrtPrice")))
                .first();
        assertEquals(100d, ((Number) tuple.get("plusPrice")).doubleValue(), 0.01d);
        assertEquals(60d, ((Number) tuple.get("minusPrice")).doubleValue(), 0.01d);
        assertEquals(160d, ((Number) tuple.get("doublePrice")).doubleValue(), 0.01d);
        assertEquals(40d, ((Number) tuple.get("halfPrice")).doubleValue(), 0.01d);
        assertEquals(80d, ((Number) tuple.get("absPrice")).doubleValue(), 0.01d);
        assertTrue(((Number) tuple.get("sqrtPrice")).doubleValue() > 8.9d);
    }

    /** Replace the null discount with 1, so that every product gets a comparable actual price. */
    @Test
    public void testCoalesce() {
        assumeTrue(JpaProviders.getProvider().supportsBeanProjection(), "The provider fills a bean through its constructor alone");
        List<ProductVo> dataList = productDao.query(ProductVo.class)
                .sort(JpaSort.desc(Product::getPrice))
                .select(new ColumnList(Product::getName, Product::getPrice)
                        .addColumns(Fields
                                .multiply(Property.forName(Product::getPrice),
                                        Fields.coalesce(Product::getDiscount, BigDecimal.ONE))
                                .as("actualPrice")))
                .list();
        assertEquals(TOTAL_PRODUCTS, dataList.size());
        dataList.forEach(vo -> assertTrue(vo.getActualPrice().compareTo(vo.getPrice()) <= 0));
    }

    /** Select a scalar subquery as a column, which is the max stock of all the products. */
    @Test
    public void testScalarSubQueryColumn() {
        assumeTrue(JpaProviders.getProvider().supportsSubQueryAsSelection(), "The provider selects no subquery as a column");
        JpaQuery<Product, Tuple> query = productDao.customQuery();
        JpaSubQuery<Stock, Long> subQuery =
                query.subQuery(Stock.class, "s", Long.class).select(Fields.max(Stock::getAmount));
        List<Tuple> dataList = query.filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList().addColumns(Product::getName)
                        .addColumns(Column.forSubQuery(subQuery, "maxStock")))
                .list();
        assertEquals(1, dataList.size());
        assertEquals(INITIAL_STOCK, ((Number) dataList.get(0).get("maxStock")).longValue());
    }

    /** Count the distinct locations, which is not the same as counting the products. */
    @Test
    public void testCountDistinct() {
        Tuple tuple = productDao.customQuery()
                .select(new ColumnList().addColumns(Fields.count(1).as("total"),
                        Fields.countDistinct(Property.forName(Product::getLocation))
                                .as("totalLocations")))
                .first();
        assertEquals(TOTAL_PRODUCTS, ((Number) tuple.get("total")).longValue());
        assertEquals(8L, ((Number) tuple.get("totalLocations")).longValue());
    }

    @Test
    public void testComparisonFilters() {
        assertEquals(7L, productDao.count(Restrictions.gte(Product::getPrice, BigDecimal.valueOf(100))));
        assertEquals(5L, productDao.count(Restrictions.lt(Product::getPrice, BigDecimal.valueOf(100))));
        assertEquals(2L, productDao.count(Restrictions.between(Product::getProduceDate,
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31))));
        assertEquals(1L, productDao.count(new FilterList().lte(Product::getPrice, BigDecimal.TEN)
                .and().gt(Product::getPrice, BigDecimal.ZERO)));
    }

    /** Compare an attribute against another attribute of the same entity. */
    @Test
    public void testFilterByAnotherField() {
        Property<BigDecimal> price = Property.forName(Product::getPrice);
        assertEquals(TOTAL_PRODUCTS, productDao.count(Restrictions.gte(price, price)));
        assertEquals(0L, productDao.count(Restrictions.lt(price, price)));
    }

    /** Raise the price of the products which are out of the given locations. */
    @Test
    public void testUpdateByExpression() {
        int rows = productDao.update()
                .setField(Product::getPrice, Fields.multiplyValue(Product::getPrice, 2))
                .filter(Restrictions.in(Product::getLocation, List.of("China", "Japan"))).execute();
        assertEquals(3, rows);
        assertEquals(0, new BigDecimal("240").compareTo(productDao.query(BigDecimal.class)
                .filter(Restrictions.eq(Product::getName, "Washing machine"))
                .select(new ColumnList(Product::getPrice)).one()));
    }

    @Test
    public void testDeleteByFilter() {
        int rows = productDao.delete().filter(Restrictions.isNull(Product::getDiscount)).execute();
        assertEquals(5, rows);
        assertEquals(TOTAL_PRODUCTS - 5, productDao.count());
    }

    /** Delete the products which have never been stocked. */
    @Test
    public void testDeleteByNotExistsSubQuery() {
        JpaDelete<Product> delete = productDao.delete();
        JpaSubQuery<Stock, Long> subQuery = delete.subQuery(Stock.class, Long.class)
                .filter(Restrictions.eq(Stock::getProductId, Product::getId)).select(Stock::getId);
        int rows = delete.filter(Restrictions.exists(subQuery).not()).execute();
        assertEquals(0, rows);
        assertEquals(TOTAL_PRODUCTS, productDao.count());
    }


    /** The Spring Data style operations which take a filter. */
    @Test
    public void testEntityDaoOperations() {
        assertEquals(Product.class, productDao.getEntityClass());
        assertTrue(productDao.exists(Restrictions.eq(Product::getLocation, "China")));
        assertEquals(2L, productDao.count(Restrictions.eq(Product::getLocation, "China")));
        assertEquals(2, productDao.findAll(Restrictions.eq(Product::getLocation, "China")).size());
        assertEquals("Iron",
                productDao.findAll(Restrictions.eq(Product::getLocation, "China"),
                        Sort.by(Sort.Direction.ASC, "price")).get(0).getName());
        Page<Product> page = productDao.findAll(Restrictions.notNull(Product::getDiscount),
                org.springframework.data.domain.PageRequest.of(0, 3));
        assertEquals(7L, page.getTotalElements());
        assertEquals(3, page.getContent().size());
        assertTrue(productDao.findOne(Restrictions.eq(Product::getName, "Juicer")).isPresent());

        assertEquals(0, new BigDecimal("1482").compareTo(
                productDao.sum("price", null, BigDecimal.class)));
        assertEquals(123.5d, productDao.avg("price", null), 0.01d);
        assertEquals(0, new BigDecimal("320")
                .compareTo(productDao.max("price", null, BigDecimal.class)));
        assertEquals(0, new BigDecimal("10")
                .compareTo(productDao.min("price", null, BigDecimal.class)));
        assertEquals(0, new BigDecimal("145").compareTo(productDao.sum("price",
                Restrictions.eq(Product::getLocation, "China"), BigDecimal.class)));
    }

    /** Every entry point of a query or a pagination. */
    @Test
    public void testQueryEntryPoints() {
        assertEquals(TOTAL_PRODUCTS, productDao.query().selectThis().list().size());
        assertEquals(TOTAL_PRODUCTS,
                productDao.query(Long.class).select(new ColumnList(Product::getId)).list().size());
        assertEquals(TOTAL_PRODUCTS,
                productDao.customQuery().select(new ColumnList(Product::getId)).list().size());
        assertEquals(TOTAL_PRODUCTS, productDao.page().selectThis().list().size());
        assertEquals(TOTAL_PRODUCTS,
                productDao.page(Long.class).select(new ColumnList(Product::getId)).list().size());
        assertEquals(TOTAL_PRODUCTS,
                productDao.customPage().select(new ColumnList(Product::getId)).list().size());
    }

    /** list, first and one are the three ways of taking the result. */
    @Test
    public void testResultSetOperations() {
        JpaQueryResultSet<Tuple> resultSet = productDao.customQuery()
                .sort(JpaSort.asc(Product::getPrice)).select(new ColumnList(Product::getName));
        assertEquals(TOTAL_PRODUCTS, resultSet.list().size());
        assertEquals(3, resultSet.list(3).size());
        assertEquals(2, resultSet.list(2, 1).size());
        assertEquals("Flashlight", resultSet.first().get(0));
        assertEquals("Flashlight", resultSet.first(true).get(0));
        assertEquals("Juicer", productDao.query(String.class)
                .filter(Restrictions.eq(Product::getId, 1L)).one(Column.forName("name")));
    }


    /**
     * A customized function is a Field like any other one, so it fits a select item, a filter, a
     * sort and a group by clause.
     */
    @Test
    public void testCustomFunctionEverywhere() {
        assumeTrue(JpaProviders.getProvider().supportsPassThroughFunction(), "The provider passes no database function through");
        // As a select item, addressed by an attribute name, by a lambda and by a Field
        Tuple tuple = productDao.customQuery().filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList()
                        .addColumns(Function.build("LEFT", String.class,
                                Property.forName(Product::getName), Fields.toInteger(3))
                                .as("shortName"))
                        .addColumns(Function.build("REPEAT", String.class,
                                Property.forName(null, "name"), Fields.toInteger(2)).as("twice"))
                        .addColumns(Function.build("UPPER", String.class, Product::getLocation)
                                .as("upperLocation")))
                .first();
        assertEquals("Jui", tuple.get("shortName"));
        assertEquals("JuicerJuicer", tuple.get("twice"));
        assertEquals("INDONESIA", tuple.get("upperLocation"));

        // Without an alias the function represents itself, which becomes the column label
        Tuple unaliased = productDao.customQuery()
                .filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList()
                        .addFields(Function.build("UPPER", String.class, "name")))
                .first();
        assertEquals("JUICER", unaliased.get(0));

        // As a filter
        assertEquals(1L, productDao
                .count(Restrictions.eq(Function.build("UPPER", String.class, "name"), "JUICER")));
        // Washing machine, Electric heater and Water dispenser are 15 characters long
        assertEquals(3L, productDao.count(Restrictions.gt(
                Function.build("LENGTH", Integer.class, "name"), 14)));

        // As a sort
        assertEquals(15, ((String) productDao.customQuery()
                .sort(JpaSort.desc(Function.build("LENGTH", Integer.class, "name")))
                .select(new ColumnList(Product::getName)).first().get(0)).length());

        // As a group by key
        List<Tuple> dataList = productDao.customQuery()
                .groupBy(new FieldList().addFields(Function.build("LENGTH", Integer.class, "name")))
                .select(new ColumnList()
                        .addColumns(Function.build("LENGTH", Integer.class, "name")
                                .as("nameLength"))
                        .addColumns(Fields.count(1).as("total")))
                .list();
        long total = dataList.stream().mapToLong(t -> ((Number) t.get("total")).longValue()).sum();
        assertEquals(TOTAL_PRODUCTS, total);
    }

    @AfterAll
    public void end() {
        log.info("=========== ProductDaoTests End. ===========");
        // productDao.deleteAll();
        // stockDao.deleteAll();
    }

    @Getter
    @Setter
    @ToString
    public static class ProductVo {

        private String name;
        private String location;
        private BigDecimal price;
        private BigDecimal actualPrice;

    }

    @Getter
    @Setter
    @ToString
    public static class ProductStockVo {
        private Long id;
        private String name;
        private String location;
        private Long amount;
    }

    @Getter
    @Setter
    @ToString
    public static class ProductAggregationVo {

        private String location;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private BigDecimal avgPrice;
        private Long amount;
        private String repr;
    }

}
