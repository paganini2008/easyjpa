package com.github.easyjpa.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.github.easyjpa.AliasMismatchedException;
import com.github.easyjpa.ColumnList;
import com.github.easyjpa.JpaQueryResultSet;
import com.github.easyjpa.Model;
import com.github.easyjpa.PredicateBuilder;
import com.github.easyjpa.Fields;
import com.github.easyjpa.PathMismatchedException;
import com.github.easyjpa.FilterList;
import com.github.easyjpa.JpaQuery;
import com.github.easyjpa.JpaSubQuery;
import com.github.easyjpa.Property;
import com.github.easyjpa.JpaProviders;
import com.github.easyjpa.Restrictions;
import com.github.easyjpa.test.dao.ProductDao;
import com.github.easyjpa.test.dao.StockDao;
import com.github.easyjpa.test.entity.Product;
import com.github.easyjpa.test.entity.Stock;
import com.github.easyjpa.test.service.ProductService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.Tuple;

/**
 *
 * Tests of the filters built by Restrictions and FilterList.
 *
 * @Description: RestrictionsTests
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class RestrictionsTests extends AbstractDaoTests {

    private static final Logger log = LoggerFactory.getLogger(RestrictionsTests.class);

    /** See ProductService#saveRandomProducts, 7 of which are priced 100 or above. */
    private static final long TOTAL_PRODUCTS = 12L;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private StockDao stockDao;

    @BeforeAll
    public void begin() {
        log.info("=========== RestrictionsTests Begin. ===========");
        productService.saveRandomProducts();
    }

    /** An attribute is addressed by a lambda, by its name, or by an alias and its name. */
    @Test
    public void testEqualsByEveryAddressing() {
        assertEquals(1L, productDao.count(Restrictions.eq(Product::getName, "Juicer")));
        assertEquals(1L, productDao.count(Restrictions.eq("name", "Juicer")));
        assertEquals(1L, productDao.count(Restrictions.eq("this", "name", "Juicer")));
        assertEquals(1L,
                productDao.count(Restrictions.eq(Property.forName(Product::getName), "Juicer")));
        // A blank alias means the root entity, just as null does
        assertEquals(1L, productDao.count(Restrictions.eq("", "name", "Juicer")));
        assertEquals(1L, productDao.count(Restrictions.eq(null, "name", "Juicer")));
        assertEquals("this.name", Property.forName("", "name").toString());
    }

    @Test
    public void testNotEquals() {
        assertEquals(TOTAL_PRODUCTS - 1, productDao.count(Restrictions.ne(Product::getName, "Juicer")));
        assertEquals(TOTAL_PRODUCTS - 1, productDao.count(Restrictions.ne("name", "Juicer")));
        assertEquals(TOTAL_PRODUCTS - 1,
                productDao.count(Restrictions.ne("this", "name", "Juicer")));
    }

    /** Compare an attribute against another attribute rather than against a value. */
    @Test
    public void testCompareTwoAttributes() {
        Property<BigDecimal> price = Property.forName(Product::getPrice);
        assertEquals(TOTAL_PRODUCTS, productDao.count(Restrictions.eq(price, price)));
        assertEquals(0L, productDao.count(Restrictions.ne(price, price)));
        assertEquals(0L, productDao.count(Restrictions.gt(price, price)));
        assertEquals(TOTAL_PRODUCTS, productDao.count(Restrictions.gte(price, price)));
        assertEquals(0L, productDao.count(Restrictions.lt(price, price)));
        assertEquals(TOTAL_PRODUCTS, productDao.count(Restrictions.lte(price, price)));
    }

    @Test
    public void testComparison() {
        assertEquals(7L, productDao.count(Restrictions.gte(Product::getPrice, HUNDRED)));
        assertEquals(7L, productDao.count(Restrictions.gt(Product::getPrice, HUNDRED)));
        assertEquals(5L, productDao.count(Restrictions.lt(Product::getPrice, HUNDRED)));
        assertEquals(5L, productDao.count(Restrictions.lte(Product::getPrice, HUNDRED)));
        assertEquals(7L, productDao.count(Restrictions.gte("this", "price", HUNDRED)));
        assertEquals(5L, productDao.count(Restrictions.lt("this", "price", HUNDRED)));
        assertEquals(7L, productDao.count(Restrictions.gt("price", HUNDRED)));
    }

    @Test
    public void testBetween() {
        assertEquals(4L, productDao.count(
                Restrictions.between(Product::getPrice, HUNDRED, BigDecimal.valueOf(150))));
        assertEquals(4L, productDao.count(Restrictions.between("this", "price", HUNDRED,
                BigDecimal.valueOf(150))));
        assertEquals(2L, productDao.count(Restrictions.between(Product::getProduceDate,
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31))));
    }

    @Test
    public void testLike() {
        assertEquals(2L, productDao.count(Restrictions.like(Product::getName, "Electric")));
        assertEquals(2L, productDao.count(Restrictions.like("name", "Electric")));
        assertEquals(2L, productDao.count(Restrictions.like("this", "name", "Electric")));
        assertEquals(TOTAL_PRODUCTS - 2,
                productDao.count(Restrictions.notLike(Product::getName, "Electric")));
        assertEquals(TOTAL_PRODUCTS - 2,
                productDao.count(Restrictions.notLike("this", "name", "Electric")));
        // An escaped pattern matches the underscore literally, which no product name contains
        assertEquals(0L, productDao.count(Restrictions.like(Product::getName, "/_", '/')));
        assertEquals(TOTAL_PRODUCTS,
                productDao.count(Restrictions.notLike(Product::getName, "/_", '/')));
    }

    @Test
    public void testNullable() {
        assertEquals(5L, productDao.count(Restrictions.isNull(Product::getDiscount)));
        assertEquals(5L, productDao.count(Restrictions.isNull("discount")));
        assertEquals(5L, productDao.count(Restrictions.isNull("this", "discount")));
        assertEquals(7L, productDao.count(Restrictions.notNull(Product::getDiscount)));
        assertEquals(7L, productDao.count(Restrictions.notNull("discount")));
        assertEquals(7L, productDao.count(Restrictions.notNull("this", "discount")));
    }

    @Test
    public void testIn() {
        List<String> locations = List.of("China", "Japan", "Nowhere");
        assertEquals(3L, productDao.count(Restrictions.in(Product::getLocation, locations)));
        assertEquals(3L, productDao.count(Restrictions.in("this", "location", locations)));
        assertEquals(3L, productDao
                .count(Restrictions.in(Property.forName(Product::getLocation), locations)));
        assertEquals(TOTAL_PRODUCTS - 3,
                productDao.count(Restrictions.in(Product::getLocation, locations).not()));
    }

    /** in, exists and the comparisons all accept a subquery. */
    @Test
    public void testSubQueryFilters() {
        JpaQuery<Product, Product> query = productDao.query();
        JpaSubQuery<Stock, Long> subQuery = query.subQuery(Stock.class, "s", Long.class)
                .filter(Restrictions.gt(Stock::getAmount, 0L)).select(Stock::getProductId);
        assertEquals(TOTAL_PRODUCTS,
                query.filter(Restrictions.in(Product::getId, subQuery)).selectThis().list().size());

        JpaQuery<Product, Product> another = productDao.query();
        JpaSubQuery<Stock, Long> maxStock = another.subQuery(Stock.class, "s2", Long.class)
                .select(Fields.max(Stock::getAmount));
        assertEquals(0, another.filter(Restrictions.gt(Property.forName(null, "id", Long.class),
                maxStock)).selectThis().list().size());
    }

    /** and, or and not compose the filters. */
    @Test
    public void testLogicalFilters() {
        assertEquals(2L, productDao.count(Restrictions.eq(Product::getLocation, "China")
                .and(Restrictions.lt(Product::getPrice, BigDecimal.valueOf(200)))));
        assertEquals(5L, productDao.count(Restrictions.eq(Product::getLocation, "China")
                .or(Restrictions.eq(Product::getLocation, "Australia"))));
        assertEquals(TOTAL_PRODUCTS - 2,
                productDao.count(Restrictions.eq(Product::getLocation, "China").not()));
        assertEquals(TOTAL_PRODUCTS, productDao.count(Restrictions.is(true)));
        assertEquals(0L, productDao.count(Restrictions.is(false)));
    }

    /** A junction joins the filters by and, whereas a disjunction joins them by or. */
    @Test
    public void testJunctions() {
        assertEquals(1L, productDao.count(Restrictions.juction()
                .and(Restrictions.eq(Product::getLocation, "China"))
                .and(Restrictions.gt(Product::getPrice, HUNDRED))));
        assertEquals(5L, productDao.count(Restrictions.disjuction()
                .or(Restrictions.eq(Product::getLocation, "China"))
                .or(Restrictions.eq(Product::getLocation, "Australia"))));
    }

    /** FilterList builds the same filters fluently. */
    @Test
    public void testFilterList() {
        assertEquals(1L, productDao.count(new FilterList().eq(Product::getLocation, "China").and()
                .gt(Product::getPrice, HUNDRED)));
        assertEquals(5L, productDao.count(new FilterList().eq(Product::getLocation, "China").or()
                .eq(Product::getLocation, "Australia")));
        assertEquals(7L, productDao.count(new FilterList().notNull(Product::getDiscount)));
        assertEquals(5L, productDao.count(new FilterList().isNull(Product::getDiscount)));
        assertEquals(2L, productDao.count(new FilterList().like(Product::getName, "Electric")));
        assertEquals(TOTAL_PRODUCTS - 2,
                productDao.count(new FilterList().notLike(Product::getName, "Electric")));
        assertEquals(3L, productDao.count(new FilterList().in(Product::getLocation,
                List.of("China", "Japan"))));
        assertEquals(4L, productDao.count(new FilterList()
                .between(Product::getPrice, HUNDRED, BigDecimal.valueOf(150))));
        assertEquals(5L, productDao.count(new FilterList().lte(Product::getPrice, HUNDRED)));
        assertEquals(7L, productDao.count(new FilterList().gt(Product::getPrice, HUNDRED)));
        assertEquals(5L, productDao.count(new FilterList().lt(Product::getPrice, HUNDRED)));
        assertEquals(7L, productDao.count(new FilterList().gte(Product::getPrice, HUNDRED)));
        assertEquals(TOTAL_PRODUCTS - 1,
                productDao.count(new FilterList().ne(Product::getName, "Juicer")));
        assertEquals(TOTAL_PRODUCTS - 2,
                productDao.count(new FilterList().eq(Product::getLocation, "China").not()));
    }

    /** Compare the attributes of two entities, which is how a cross join gets correlated. */
    @Test
    public void testCompareAcrossEntities() {
        long rows = stockDao.customQuery().crossJoin(Product.class, "p")
                .filter(new FilterList().eq(Stock::getProductId, Product::getId))
                .select(new com.github.easyjpa.ColumnList(Stock::getId)).list().size();
        assertEquals(TOTAL_PRODUCTS, rows);
        assertTrue(rows > 0);
    }


    /** FilterList addresses an attribute by a name, by an alias and a name, or by a Field. */
    @Test
    public void testFilterListAddressing() {
        Property<BigDecimal> price = Property.forName(Product::getPrice);
        assertEquals(1L, productDao.count(new FilterList().eq("name", "Juicer")));
        assertEquals(1L, productDao.count(new FilterList().eq("this", "name", "Juicer")));
        assertEquals(1L, productDao.count(new FilterList().eq(Property.forName(Product::getName),
                "Juicer")));
        assertEquals(TOTAL_PRODUCTS - 1, productDao.count(new FilterList().ne("name", "Juicer")));
        assertEquals(TOTAL_PRODUCTS - 1,
                productDao.count(new FilterList().ne("this", "name", "Juicer")));
        assertEquals(TOTAL_PRODUCTS - 1, productDao.count(new FilterList()
                .ne(Property.forName(Product::getName), "Juicer")));
        assertEquals(TOTAL_PRODUCTS, productDao.count(new FilterList().eq(price, price)));
        assertEquals(0L, productDao.count(new FilterList().ne(price, price)));

        assertEquals(5L, productDao.count(new FilterList().lt("this", "price", HUNDRED)));
        assertEquals(5L, productDao.count(new FilterList().lt(price, HUNDRED)));
        assertEquals(0L, productDao.count(new FilterList().lt(price, price)));
        assertEquals(5L, productDao.count(new FilterList().lte("this", "price", HUNDRED)));
        assertEquals(5L, productDao.count(new FilterList().lte(price, HUNDRED)));
        assertEquals(TOTAL_PRODUCTS, productDao.count(new FilterList().lte(price, price)));
        assertEquals(7L, productDao.count(new FilterList().gt("this", "price", HUNDRED)));
        assertEquals(7L, productDao.count(new FilterList().gt(price, HUNDRED)));
        assertEquals(0L, productDao.count(new FilterList().gt(price, price)));
        assertEquals(7L, productDao.count(new FilterList().gte("this", "price", HUNDRED)));
        assertEquals(7L, productDao.count(new FilterList().gte(price, HUNDRED)));
        assertEquals(TOTAL_PRODUCTS, productDao.count(new FilterList().gte(price, price)));

        List<String> locations = List.of("China", "Japan");
        assertEquals(3L, productDao.count(new FilterList().in("this", "location", locations)));
        assertEquals(3L, productDao.count(new FilterList()
                .in(Property.forName(Product::getLocation), locations)));
        assertEquals(4L, productDao.count(new FilterList().between("this", "price", HUNDRED,
                BigDecimal.valueOf(150))));
        assertEquals(4L, productDao.count(new FilterList().between(price, HUNDRED,
                BigDecimal.valueOf(150))));
        assertEquals(2L, productDao.count(new FilterList().like("this", "name", "Electric")));
        assertEquals(2L, productDao.count(new FilterList()
                .like(Property.forName(Product::getName), "Electric")));
        assertEquals(TOTAL_PRODUCTS - 2,
                productDao.count(new FilterList().notLike("this", "name", "Electric")));
        assertEquals(TOTAL_PRODUCTS - 2, productDao.count(new FilterList()
                .notLike(Property.forName(Product::getName), "Electric")));
        assertEquals(5L, productDao.count(new FilterList().isNull("discount")));
        assertEquals(5L, productDao.count(new FilterList().isNull("this", "discount")));
        assertEquals(5L, productDao.count(new FilterList()
                .isNull(Property.forName(Product::getDiscount))));
        assertEquals(7L, productDao.count(new FilterList().notNull("discount")));
        assertEquals(7L, productDao.count(new FilterList().notNull("this", "discount")));
        assertEquals(7L, productDao.count(new FilterList()
                .notNull(Property.forName(Product::getDiscount))));
    }

    /** The subquery filters of FilterList, which every comparison accepts. */
    @Test
    public void testFilterListSubQueries() {
        assumeTrue(JpaProviders.getProvider().supportsSubQueryAsExpression(), "The provider takes no subquery as an expression");
        JpaQuery<Product, Product> query = productDao.query();
        JpaSubQuery<Stock, Long> stocked = query.subQuery(Stock.class, "s", Long.class)
                .filter(Restrictions.gt(Stock::getAmount, 0L)).select(Stock::getProductId);
        assertEquals(TOTAL_PRODUCTS, query.filter(new FilterList()
                .in(Property.forName(null, "id", Long.class), stocked)).selectThis().list().size());

        JpaQuery<Product, Product> another = productDao.query();
        JpaSubQuery<Stock, Long> maxAmount = another.subQuery(Stock.class, "s2", Long.class)
                .select(Fields.max(Stock::getAmount));
        assertEquals(TOTAL_PRODUCTS, another.filter(new FilterList()
                .lte(Property.forName(null, "id", Long.class), maxAmount)).selectThis().list()
                .size());
    }

    /** exists takes a subquery which correlates the outer query. */
    @Test
    public void testExistsSubQuery() {
        JpaQuery<Product, Product> query = productDao.query();
        JpaSubQuery<Stock, Long> subQuery = query.subQuery(Stock.class, "s", Long.class)
                .filter(new FilterList().eq(Stock::getProductId, Product::getId).and()
                        .gt(Stock::getAmount, 0L))
                .select(Stock::getId);
        assertEquals(TOTAL_PRODUCTS,
                query.filter(new FilterList().exists(subQuery)).selectThis().list().size());
    }


    /** A customized filter, which is what every Restrictions method is built upon. */
    @Test
    public void testCustomizedFilter() {
        PredicateBuilder<String> startsWith = new PredicateBuilder<String>() {

            @Override
            public Predicate toPredicate(Model<?> model, Expression<String> expression,
                    CriteriaBuilder builder) {
                assertEquals("this", getDefaultAlias());
                return builder.like(expression, "Electric%");
            }
        };
        assertEquals(2L, productDao
                .count(Restrictions.create(Property.forName(Product::getName), startsWith)));
    }

    /** An unknown alias or an unknown attribute is rejected rather than silently ignored. */
    @Test
    public void testUnknownAliasAndAttribute() {
        // Spring Data wraps the exception, so the cause is what tells the reason
        Exception e = assertThrows(Exception.class,
                () -> productDao.count(Restrictions.eq("nowhere", "name", "Juicer")));
        assertTrue(e.getCause() instanceof PathMismatchedException);

        assertThrows(AliasMismatchedException.class, () -> productDao.customQuery()
                .select(new ColumnList().addTableAlias("nowhere")).list());
    }

    /** A grouping subquery selects by an attribute name or by a lambda as well. */
    @Test
    public void testSubQueryGroupBySelect() {
        JpaQuery<Stock, Stock> query = stockDao.query();
        JpaSubQuery<Product, Long> subQuery = query.subQuery(Product.class, "p", Long.class);
        subQuery.groupBy(Property.forName("p", "id")).select("p", "id");
        assertEquals(TOTAL_PRODUCTS,
                query.filter(Restrictions.in(Stock::getProductId, subQuery)).selectThis().list()
                        .size());

        JpaQuery<Stock, Stock> another = stockDao.query();
        JpaSubQuery<Product, Long> byLambda = another.subQuery(Product.class, "p2", Long.class);
        byLambda.groupBy("p2", "id").select(Product::getId);
        assertEquals(TOTAL_PRODUCTS, another
                .filter(Restrictions.in(Stock::getProductId, byLambda)).selectThis().list().size());
    }

    /** A query result set is countable, which counts nothing until it gets paginated. */
    @Test
    public void testQueryResultSetIsCountable() throws Exception {
        JpaQueryResultSet<Tuple> resultSet =
                productDao.customQuery().select(new ColumnList(Product::getId));
        assertEquals(Integer.MAX_VALUE, resultSet.rowCount());
    }


    /** Compare an attribute of one table against an attribute of another table. */
    @Test
    public void testCompareAttributesAcrossAliases() {
        assertEquals(TOTAL_PRODUCTS, stockDao.customQuery().crossJoin(Product.class, "p")
                .filter(Restrictions.eq("this", "productId", "p", "id"))
                .select(new ColumnList(Stock::getId)).list().size());
        assertEquals(TOTAL_PRODUCTS * (TOTAL_PRODUCTS - 1), stockDao.customQuery()
                .crossJoin(Product.class, "p")
                .filter(Restrictions.ne("this", "productId", "p", "id"))
                .select(new ColumnList(Stock::getId)).list().size());
        assertEquals(TOTAL_PRODUCTS, stockDao.customQuery().crossJoin(Product.class, "p")
                .filter(Restrictions.eq(Stock::getProductId, Product::getId))
                .select(new ColumnList(Stock::getId)).list().size());
        assertEquals(TOTAL_PRODUCTS * (TOTAL_PRODUCTS - 1), stockDao.customQuery()
                .crossJoin(Product.class, "p")
                .filter(Restrictions.ne(Stock::getProductId, Product::getId))
                .select(new ColumnList(Stock::getId)).list().size());
    }

    /** Every comparison takes a subquery, addressed by a lambda or by an alias. */
    @Test
    public void testComparisonsAgainstSubQuery() {
        assumeTrue(JpaProviders.getProvider().supportsSubQueryAsExpression(), "The provider takes no subquery as an expression");
        assertEquals(TOTAL_PRODUCTS, countAgainstMaxStock(true));
        assertEquals(0L, countAgainstMaxStock(false));

        JpaQuery<Stock, Stock> query = stockDao.query();
        JpaSubQuery<Stock, Long> maxAmount = query.subQuery(Stock.class, "s2", Long.class)
                .select(Fields.max(Stock::getAmount));
        assertEquals(TOTAL_PRODUCTS,
                query.filter(Restrictions.lte(Property.forName(Stock::getAmount), maxAmount))
                        .selectThis().list()
                        .size());

        JpaQuery<Stock, Stock> byAlias = stockDao.query();
        JpaSubQuery<Stock, Long> minAmount = byAlias.subQuery(Stock.class, "s3", Long.class)
                .select(Fields.min("s3", "amount", Long.class));
        assertEquals(TOTAL_PRODUCTS,
                byAlias.filter(Restrictions.gte("this", "amount", minAmount)).selectThis().list()
                        .size());

        JpaQuery<Stock, Stock> byName = stockDao.query();
        JpaSubQuery<Stock, Long> theMax = byName.subQuery(Stock.class, "s4", Long.class)
                .select(Fields.max("s4", "amount", Long.class));
        assertEquals(0L, byName.filter(Restrictions.lt("this", "amount", theMax)).selectThis()
                .list().size());
    }

    private long countAgainstMaxStock(boolean lessOrEqual) {
        JpaQuery<Stock, Stock> query = stockDao.query();
        JpaSubQuery<Stock, Long> subQuery = query.subQuery(Stock.class, "s1", Long.class)
                .select(Fields.max(Stock::getAmount));
        Property<Long> amount = Property.forName(Stock::getAmount);
        return query.filter(lessOrEqual ? Restrictions.lte(amount, subQuery)
                : Restrictions.gt(amount, subQuery)).selectThis().list().size();
    }

    /** all, any and some turn a subquery into a comparable expression. */
    @Test
    public void testQuantifiedSubQuery() {
        JpaQuery<Stock, Stock> all = stockDao.query();
        JpaSubQuery<Stock, Long> everyAmount = all.subQuery(Stock.class, "s1", Long.class)
                .select(Property.forName("s1", "amount", Long.class));
        assertEquals(TOTAL_PRODUCTS,
                all.filter(Restrictions.gte(Property.forName("this", "amount", Long.class),
                        Fields.all(everyAmount))).selectThis().list().size());

        JpaQuery<Stock, Stock> any = stockDao.query();
        JpaSubQuery<Stock, Long> anyAmount = any.subQuery(Stock.class, "s2", Long.class)
                .select(Property.forName("s2", "amount", Long.class));
        assertEquals(TOTAL_PRODUCTS,
                any.filter(Restrictions.gte(Property.forName("this", "amount", Long.class),
                        Fields.any(anyAmount))).selectThis().list().size());

        JpaQuery<Stock, Stock> some = stockDao.query();
        JpaSubQuery<Stock, Long> someAmount = some.subQuery(Stock.class, "s3", Long.class)
                .select(Property.forName("s3", "amount", Long.class));
        assertEquals(TOTAL_PRODUCTS,
                some.filter(Restrictions.eq(Property.forName("this", "amount", Long.class),
                        Fields.some(someAmount))).selectThis().list().size());
    }

    /** The remaining overloads of the logical operators and the patterns. */
    @Test
    public void testRemainingOverloads() {
        assertEquals(1L, productDao.count(Restrictions.and(
                List.of(Restrictions.eq(Product::getLocation, "China"),
                        Restrictions.gt(Product::getPrice, HUNDRED)))));
        assertEquals(5L, productDao.count(Restrictions.or(
                List.of(Restrictions.eq(Product::getLocation, "China"),
                        Restrictions.eq(Product::getLocation, "Australia")))));
        assertEquals(TOTAL_PRODUCTS - 2, productDao
                .count(Restrictions.not(Fields.eq(Product::getLocation, "China"))));

        assertEquals(2L, productDao.count(Restrictions.like("name", "Electric", '/')));
        assertEquals(2L, productDao.count(Restrictions.like("this", "name", "Electric", '/')));
        assertEquals(2L, productDao
                .count(Restrictions.like(Property.forName(Product::getName), "Electric", '/')));
        assertEquals(TOTAL_PRODUCTS - 2,
                productDao.count(Restrictions.notLike("name", "Electric", '/')));
        assertEquals(TOTAL_PRODUCTS - 2,
                productDao.count(Restrictions.notLike("this", "name", "Electric", '/')));
        assertEquals(TOTAL_PRODUCTS - 2, productDao
                .count(Restrictions.notLike(Property.forName(Product::getName), "Electric", '/')));

        assertEquals(4L, productDao.count(Restrictions.between(
                Property.forName(Product::getPrice), HUNDRED, BigDecimal.valueOf(150))));
        assertEquals(5L, productDao.count(Restrictions.in("location",
                List.of("China", "Australia"))));
        assertEquals(5L, productDao.count(new FilterList().in("location",
                List.of("China", "Australia"))));
    }

    @AfterAll
    public void end() {
        log.info("=========== RestrictionsTests End. ===========");
    }

}
