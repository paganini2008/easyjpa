package com.github.easyjpa.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
import com.github.easyjpa.FieldList;
import com.github.easyjpa.Fields;
import com.github.easyjpa.FilterList;
import com.github.easyjpa.JpaDelete;
import com.github.easyjpa.JpaQuery;
import com.github.easyjpa.JpaSort;
import com.github.easyjpa.JpaSubQuery;
import com.github.easyjpa.JpaUpdate;
import com.github.easyjpa.JpaPageResultSet;
import com.github.easyjpa.Property;
import com.github.easyjpa.JpaProviders;
import com.github.easyjpa.Restrictions;
import com.github.easyjpa.Transformers;
import com.github.easyjpa.page.PageRequest;
import com.github.easyjpa.page.PageResponse;
import com.github.easyjpa.page.PageableQuery;
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
 * @Description: StockDaoTests
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class StockDaoTests extends AbstractDaoTests {

    private static final Logger log = LoggerFactory.getLogger(StockDaoTests.class);

    /** Every product gets one stock, see ProductService#saveRandomProducts. */
    private static final long TOTAL_PRODUCTS = 12L;

    private static final long INITIAL_AMOUNT = 99999L;

    @Autowired
    private ProductService productService;

    @Autowired
    private StockDao stockDao;

    @Autowired
    private ProductDao productDao;

    @BeforeAll
    public void begin() {
        log.info("=========== StockDaoTests Begin. ===========");
        productService.saveRandomProducts();
    }

    @Test
    public void testCountAndExists() {
        assertEquals(TOTAL_PRODUCTS, stockDao.count(Restrictions.gt(Stock::getAmount, 0L)));
        assertTrue(stockDao.exists(Restrictions.eq(Stock::getAmount, INITIAL_AMOUNT)));
        assertTrue(!stockDao.exists(Restrictions.lt(Stock::getAmount, 0L)));
    }

    @Test
    public void testAggregations() {
        assertEquals(INITIAL_AMOUNT, stockDao.max("amount", null, Long.class));
        assertEquals(INITIAL_AMOUNT, stockDao.min("amount", null, Long.class));
        assertEquals(INITIAL_AMOUNT * TOTAL_PRODUCTS, stockDao.sum("amount", null, Long.class));
        assertEquals(Double.valueOf(INITIAL_AMOUNT), stockDao.avg("amount", null));
    }

    @Test
    public void testFindAllAndFindOne() {
        List<Stock> stocks = stockDao.findAll(Restrictions.gte(Stock::getAmount, INITIAL_AMOUNT));
        assertEquals(TOTAL_PRODUCTS, stocks.size());
        Long productId = productDao.query(Long.class)
                .filter(Restrictions.eq(Product::getName, "Juicer"))
                .select(new ColumnList(Product::getId)).one();
        assertTrue(stockDao.findOne(Restrictions.eq(Stock::getProductId, productId)).isPresent());
    }

    /**
     * The stocks of the products made in the given location, which is a subquery in the where
     * clause.
     */
    @ParameterizedTest
    @ValueSource(strings = {"Australia", "China", "Thailand"})
    public void testInSubQuery(String location) {
        long expected = productDao.count(Restrictions.eq(Product::getLocation, location));
        JpaQuery<Stock, Stock> query = stockDao.query();
        JpaSubQuery<Product, Long> subQuery = query.subQuery(Product.class, "p", Long.class)
                .filter(Restrictions.eq(Product::getLocation, location)).select(Product::getId);
        List<Stock> stocks =
                query.filter(Restrictions.in(Stock::getProductId, subQuery)).selectThis().list();
        assertEquals(expected, stocks.size());
    }

    /** The products which have never been sold, that is to say, the stock keeps untouched. */
    @Test
    public void testNotExistsSubQuery() {
        JpaQuery<Product, Product> query = productDao.query();
        JpaSubQuery<Stock, Long> subQuery = query.subQuery(Stock.class, "s", Long.class)
                .filter(new FilterList().eq(Stock::getProductId, Product::getId).and()
                        .lt(Stock::getAmount, INITIAL_AMOUNT))
                .select(Stock::getId);
        assertEquals(0, query.filter(Restrictions.exists(subQuery)).selectThis().list().size());
    }

    /** A join between the stock and the product, which are not associated by any attribute. */
    @Test
    public void testCrossJoinProduct() {
        List<StockVo> dataList = stockDao.customQuery().crossJoin(Product.class, "p")
                .filter(new FilterList().eq(Product::getId, Stock::getProductId).and()
                        .gt(Product::getPrice, BigDecimal.valueOf(100)))
                .sort(JpaSort.desc(Product::getPrice))
                .select(new ColumnList().addColumns(Product::getName, Product::getLocation)
                        .addColumns(Stock::getAmount))
                .setTransformer(Transformers.asBean(StockVo.class)).list();
        assertEquals(productDao.count(Restrictions.gt(Product::getPrice, BigDecimal.valueOf(100))),
                dataList.size());
        dataList.forEach(vo -> assertEquals(INITIAL_AMOUNT, vo.getAmount()));
    }

    /** Group the stocks by the location of their products. */
    @Test
    public void testGroupByLocation() {
        assumeTrue(JpaProviders.getProvider().supportsPartialEntity(), "The provider builds no partial entity");
        List<Tuple> dataList = stockDao.customQuery().crossJoin(Product.class, "p")
                .filter(Restrictions.eq(Product::getId, Stock::getProductId))
                .groupBy(new FieldList(Product::getLocation))
                .select(new ColumnList().addColumns(Product::getLocation)
                        .addColumns(Fields.sum(Stock::getAmount).as("totalAmount")))
                .list();
        long locations = productDao.query().distinct().select(new ColumnList(Product::getLocation))
                .list().size();
        assertEquals(locations, dataList.size());
        long total = dataList.stream().mapToLong(t -> ((Number) t.get("totalAmount")).longValue())
                .sum();
        assertEquals(INITIAL_AMOUNT * TOTAL_PRODUCTS, total);
    }

    @Test
    public void testPaginate() throws Exception {
        PageableQuery<Stock> pageableQuery =
                stockDao.page().sort(JpaSort.asc(Stock::getProductId)).selectThis()
                        .setTransformer(Transformers.noop());
        assertEquals(TOTAL_PRODUCTS, pageableQuery.rowCount());

        PageResponse<Stock> pageResponse = pageableQuery.paginate(PageRequest.of(5));
        assertEquals(3, pageResponse.getTotalPages());
        assertTrue(pageResponse.isFirstPage());
        assertEquals(5, pageResponse.getContent().size());
        assertEquals(2, pageResponse.lastPage().getContent().size());
    }

    /** Increase the stock of the given products, which is filtered by a subquery. */
    @ParameterizedTest
    @ValueSource(strings = {"Australia", "New Zealand"})
    public void testUpdateBySubQuery(String location) {
        JpaUpdate<Stock> update = stockDao.update();
        JpaSubQuery<Product, Long> subQuery = update.subQuery(Product.class, Long.class)
                .filter(Restrictions.eq(Product::getLocation, location)).select(Product::getId);
        long expected = productDao.count(Restrictions.eq(Product::getLocation, location));
        int rows = update.setField(Stock::getAmount, Fields.plusValue(Stock::getAmount, 1000L))
                .filter(Restrictions.in(Stock::getProductId, subQuery)).execute();
        assertEquals(expected, rows);
        assertEquals(expected,
                stockDao.count(Restrictions.eq(Stock::getAmount, INITIAL_AMOUNT + 1000L)));
    }

    /** Set an attribute to the value of another attribute. */
    @Test
    public void testUpdateSetProperty() {
        int rows = stockDao.update().setProperty("amount", "productId").execute();
        assertEquals(TOTAL_PRODUCTS, rows);
        stockDao.findAll().forEach(stock -> assertEquals(stock.getProductId(), stock.getAmount()));
    }

    @Test
    public void testDeleteBySubQuery() {
        JpaDelete<Stock> delete = stockDao.delete();
        JpaSubQuery<Product, Long> subQuery = delete.subQuery(Product.class, Long.class)
                .filter(Restrictions.isNull(Product::getDiscount)).select(Product::getId);
        long expected = productDao.count(Restrictions.isNull(Product::getDiscount));
        int rows = delete.filter(Restrictions.in(Stock::getProductId, subQuery)).execute();
        assertEquals(expected, rows);
        assertEquals(TOTAL_PRODUCTS - expected, stockDao.count());
    }

    @Test
    public void testNativeQuery() {
        List<Stock> stocks = stockDao
                .query("select * from example_stock where amount > ?", new Object[] {0L}).list();
        assertEquals(TOTAL_PRODUCTS, stocks.size());

        Number total = stockDao.getSingleResult("select sum(amount) from example_stock", null,
                Number.class);
        assertEquals(INITIAL_AMOUNT * TOTAL_PRODUCTS, total.longValue());
    }

    @Test
    public void testNativeQueryAsBeanAndMap() throws Exception {
        String sql = "select s.amount as amount, p.name as name, p.location as location"
                + " from example_stock s join example_product p on p.id=s.product_id"
                + " where p.location = ?";
        List<StockVo> dataList =
                stockDao.query(sql, new Object[] {"Australia"}, StockVo.class).list();
        assertEquals(3, dataList.size());
        dataList.forEach(vo -> assertEquals("Australia", vo.getLocation()));

        PageableQuery<Map<String, Object>> pageableQuery =
                stockDao.queryForMap(sql, new Object[] {"Australia"});
        assertEquals(3L, pageableQuery.rowCount());
        assertEquals(2, pageableQuery.list(2, 0).size());
    }

    /** Map every row by a customized RowMapper. */
    @Test
    public void testNativeQueryByRowMapper() {
        List<String> names = stockDao
                .query("select p.name as name from example_product p order by p.name",
                        new Object[0], (index, data) -> (String) data.get("name"))
                .list();
        assertEquals(TOTAL_PRODUCTS, names.size());
        assertEquals("Coffee maker", names.get(0));
    }

    @Test
    public void testNativeExecuteAndUpdate() {
        AtomicInteger counter = new AtomicInteger();
        stockDao.execute("select amount from example_stock", new Object[0], query -> {
            query.getResultList().forEach(row -> counter.incrementAndGet());
            return null;
        });
        assertEquals(TOTAL_PRODUCTS, counter.get());

        int rows = stockDao.executeUpdate("update example_stock set amount = amount + ? where amount > ?",
                new Object[] {1L, 0L});
        assertEquals(TOTAL_PRODUCTS, rows);
    }


    /**
     * A where join, that is to say, two tables put side by side and correlated by the where clause,
     * which is how the tables having no association get joined.
     */
    @Test
    public void testWhereJoinSelectBothEntities() {
        // Select both entities, one column per table alias
        List<Tuple> dataList = stockDao.customQuery().crossJoin(Product.class, "p")
                .filter(Restrictions.eq(Stock::getProductId, Product::getId))
                .sort(JpaSort.asc(Product::getName))
                .select(new ColumnList().addTableAlias("this", "p")).list();
        assertEquals(TOTAL_PRODUCTS, dataList.size());
        dataList.forEach(tuple -> {
            Stock stock = (Stock) tuple.get(0);
            Product product = (Product) tuple.get(1);
            assertEquals(product.getId(), stock.getProductId());
        });
    }

    /** A where join transformed into a bean, a map and a list. */
    @Test
    public void testWhereJoinTransformations() {
        List<StockVo> beans = stockDao.customQuery().crossJoin(Product.class, "p")
                .filter(Restrictions.eq(Stock::getProductId, Product::getId))
                .select(new ColumnList().addColumns(Product::getName, Product::getLocation)
                        .addColumns(Stock::getAmount))
                .setTransformer(Transformers.asBean(StockVo.class)).list();
        assertEquals(TOTAL_PRODUCTS, beans.size());

        List<Map<String, Object>> maps = stockDao.customQuery().crossJoin(Product.class, "p")
                .filter(Restrictions.eq(Stock::getProductId, Product::getId))
                .select(new ColumnList().addColumns(Product::getName, Product::getLocation)
                        .addColumns(Stock::getAmount))
                .setTransformer(Transformers.asMap()).list();
        assertEquals(TOTAL_PRODUCTS, maps.size());
        maps.forEach(m -> assertEquals(INITIAL_AMOUNT, m.get("amount")));

        List<List<Object>> lists = stockDao.customQuery().crossJoin(Product.class, "p")
                .filter(Restrictions.eq(Stock::getProductId, Product::getId))
                .select(new ColumnList().addColumns(Product::getName).addColumns(Stock::getAmount))
                .setTransformer(Transformers.asList()).list();
        assertEquals(TOTAL_PRODUCTS, lists.size());
        lists.forEach(l -> assertEquals(2, l.size()));
    }

    /** A where join between three tables. */
    @Test
    public void testWhereJoinThreeTables() throws Exception {
        JpaPageResultSet<Tuple> resultSet = stockDao.customPage().crossJoin(Product.class, "p")
                .crossJoin(Stock.class, "s2")
                .filter(new FilterList()
                        .eq(Property.forName("this", "productId"), Property.forName("p", "id"))
                        .and()
                        .eq(Property.forName("s2", "productId"), Property.forName("p", "id")))
                .select(new ColumnList().addColumns(Product::getName)
                        .addColumns(Property.forName("this", "amount").as("amount"),
                                Property.forName("s2", "amount").as("otherAmount")));
        assertEquals(TOTAL_PRODUCTS, resultSet.rowCount());
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
    }


    /**
     * A statement built but never executed leaves nothing behind, so the next one resolves its
     * lambdas on its own tables.
     */
    @Test
    public void testAliasesNeverLeak() {
        JpaQuery<Product, Product> abandoned = productDao.query();
        abandoned.subQuery(Stock.class, "s", Long.class)
                .filter(Restrictions.gt(Stock::getAmount, 0L)).select(Stock::getId);
        // Stock was aliased s over there, yet it is the root here
        assertEquals(TOTAL_PRODUCTS, stockDao.query().filter(Restrictions.gt(Stock::getAmount, 0L))
                .selectThis().list().size());
    }

    /**
     * An outer query and its subquery may both query the same entity, and a lambda still tells
     * their tables apart, since it is resolved against the statement it belongs to.
     */
    @Test
    public void testOuterAndSubQueryOnSameEntity() {
        assumeTrue(JpaProviders.getProvider().supportsSubQueryAsExpression(), "The provider takes no subquery as an expression");
        JpaQuery<Stock, Stock> query = stockDao.query();
        JpaSubQuery<Stock, Long> maxAmount = query.subQuery(Stock.class, "s2", Long.class)
                .select(Fields.max(Stock::getAmount));
        List<Stock> stocks =
                query.filter(Restrictions.gte(Stock::getAmount, maxAmount)).selectThis().list();
        // Every stock holds the same amount, so all of them reach the maximum
        assertEquals(TOTAL_PRODUCTS, stocks.size());
    }

    /** The same entity joined twice, told apart by the aliases given to the joins. */
    @Test
    public void testSelfJoinTellsTablesApart() {
        List<Tuple> dataList = stockDao.customQuery().crossJoin(Stock.class, "s2")
                .filter(new FilterList()
                        .eq(Property.forName("s2", "productId"), Property.forName(Stock::getProductId))
                        .and().ne(Property.forName("s2", "id"), Property.forName(Stock::getId)))
                .select(new ColumnList().addColumns(Property.forName(Stock::getId).as("id"))
                        .addColumns(Property.forName("s2", "id").as("otherId")))
                .list();
        // A stock never pairs with itself
        assertEquals(0, dataList.size());
    }

    @AfterAll
    public void end() {
        log.info("=========== StockDaoTests End. ===========");
    }

    @Getter
    @Setter
    @ToString
    public static class StockVo {

        private String name;
        private String location;
        private Long amount;

    }

}
