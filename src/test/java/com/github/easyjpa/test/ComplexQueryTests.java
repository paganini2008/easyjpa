package com.github.easyjpa.test;

import static com.github.easyjpa.Fields.avg;
import static com.github.easyjpa.Fields.count;
import static com.github.easyjpa.Fields.countDistinct;
import static com.github.easyjpa.Fields.max;
import static com.github.easyjpa.Fields.multiply;
import static com.github.easyjpa.Fields.sum;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.github.easyjpa.Column;
import com.github.easyjpa.ColumnList;
import com.github.easyjpa.FieldList;
import com.github.easyjpa.Fields;
import com.github.easyjpa.FilterList;
import com.github.easyjpa.IfExpression;
import com.github.easyjpa.JpaPage;
import com.github.easyjpa.JpaProviders;
import com.github.easyjpa.JpaPageResultSet;
import com.github.easyjpa.JpaQuery;
import com.github.easyjpa.JpaSort;
import com.github.easyjpa.JpaSubQuery;
import com.github.easyjpa.Property;
import com.github.easyjpa.Restrictions;
import com.github.easyjpa.Transformers;
import com.github.easyjpa.page.PageRequest;
import com.github.easyjpa.test.dao.OrderDao;
import com.github.easyjpa.test.dao.OrderProductDao;
import com.github.easyjpa.test.dao.ProductDao;
import com.github.easyjpa.test.dao.StockDao;
import com.github.easyjpa.test.dao.UserDao;
import com.github.easyjpa.test.entity.Order;
import com.github.easyjpa.test.entity.OrderProduct;
import com.github.easyjpa.test.entity.OrderStatus;
import com.github.easyjpa.test.entity.Product;
import com.github.easyjpa.test.entity.Stock;
import com.github.easyjpa.test.entity.User;
import com.github.easyjpa.test.service.ProductService;
import com.github.easyjpa.test.service.UserOrderService;
import com.github.easyjpa.test.service.UserService;
import jakarta.persistence.Persistence;
import jakarta.persistence.Tuple;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * Tests of the complicated statements which a real e-commerce application would run.
 *
 * @Description: ComplexQueryTests
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class ComplexQueryTests extends AbstractDaoTests {

    private static final Logger log = LoggerFactory.getLogger(ComplexQueryTests.class);

    private static final long TOTAL_PRODUCTS = 12L;

    private static final long TOTAL_USERS = 5L;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserOrderService userOrderService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderProductDao orderProductDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private StockDao stockDao;

    @BeforeAll
    public void begin() {
        log.info("=========== ComplexQueryTests Begin. ===========");
        userService.saveRandomUsers();
        productService.saveRandomProducts();
        userOrderService.makeRandomOrders();
    }

    /**
     * The best sellers: how many pieces of every product have been sold, how many orders contain
     * it, and how much it earns, which joins the order products with the products, groups them and
     * keeps only the ones sold more than once.
     */
    @Test
    public void testBestSellers() throws Exception {
        assumeTrue(JpaProviders.getProvider().supportsOrdinalSort(), "The provider sorts by no column position");
        JpaPageResultSet<Tuple> resultSet = orderProductDao.customPage()
                .join(OrderProduct::getProduct, "p", null)
                .groupBy(new FieldList().addFields(Product::getName))
                .having(Restrictions.gt(count(OrderProduct::getId), 1L)).sort(JpaSort.desc(2))
                .select(new ColumnList().addColumns(Product::getName)
                        .addColumns(sum(OrderProduct::getAmount).as("soldAmount"),
                                countDistinct(Property.forName("this", "order.id"))
                                        .as("orderAmount"),
                                sum(multiply(Property.forName(Product::getPrice),
                                        Property.forName(OrderProduct::getAmount)))
                                                .as("turnover")));
        List<SalesVo> dataList =
                resultSet.setTransformer(Transformers.asBean(SalesVo.class)).list();
        assertEquals(dataList.size(), (int) resultSet.rowCount());
        assertTrue(dataList.size() <= TOTAL_PRODUCTS);

        long previous = Long.MAX_VALUE;
        for (SalesVo vo : dataList) {
            log.info(vo.toString());
            assertTrue(vo.getSoldAmount() <= previous);
            previous = vo.getSoldAmount();
            assertTrue(vo.getTurnover().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(vo.getOrderAmount().longValue(), productAmountOf(vo.getName()));
        }
    }

    private long productAmountOf(String productName) {
        Long productId = productDao.query(Long.class)
                .filter(Restrictions.eq(Product::getName, productName))
                .select(new ColumnList(Product::getId)).one();
        return orderProductDao.count(Restrictions.eq(OrderProduct::getProduct, productId));
    }

    /**
     * The profile of every user, which left joins the orders so that the users having ordered
     * nothing are kept as well.
     */
    @Test
    public void testUserProfiles() {
        List<Tuple> dataList = userDao.customQuery().leftJoin(User::getOrders, "o", null)
                .groupBy(new FieldList().addFields(Property.forName("this", "username")))
                .sort(JpaSort.asc("this", "username"))
                .select(new ColumnList().addColumn("this", "username")
                        .addColumns(count(Order::getId).as("orderAmount"),
                                sum(Order::getTotalPrice).as("totalPrice"),
                                avg(Order::getTotalPrice).as("avgPrice"),
                                max(Order::getTotalPrice).as("maxPrice")))
                .list();
        assertEquals(TOTAL_USERS, dataList.size());
        long orders = 0L;
        for (Tuple tuple : dataList) {
            orders += ((Number) tuple.get("orderAmount")).longValue();
        }
        assertEquals(orderDao.count(), orders);
    }

    /**
     * Where the best selling products are made, which joins three tables and aggregates by the
     * location of the products.
     */
    @Test
    public void testSalesByLocation() throws Exception {
        assumeTrue(JpaProviders.getProvider().supportsOrdinalSort(), "The provider sorts by no column position");
        JpaPageResultSet<Tuple> resultSet = orderDao.customPage()
                .join(Order::getOrderProducts, "op", null).join(OrderProduct::getProduct, "p", null)
                .filter(Restrictions.ne("this", "status", OrderStatus.CANCELLED))
                .groupBy(new FieldList().addFields(Product::getLocation))
                .sort(JpaSort.desc(2))
                .select(new ColumnList().addColumns(Product::getLocation)
                        .addColumns(sum(OrderProduct::getAmount).as("soldAmount"),
                                countDistinct(Fields.root()).as("orderAmount")));
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
        assertTrue(resultSet.rowCount() <= 8L);
    }

    /**
     * The repeat customers, that is to say, the users who have ever placed more than one order,
     * which is a grouping subquery in the where clause.
     */
    @Test
    public void testRepeatCustomers() {
        JpaQuery<User, User> query = userDao.query();
        JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class);
        subQuery.groupBy(new FieldList().addFields(Order::getUser))
                .having(Restrictions.gt(count(Order::getId), 1L))
                .select(Property.forName("o", "user.id", Long.class));
        List<User> users =
                query.filter(Restrictions.in(Property.forName(User::getId), subQuery)).selectThis()
                        .list();
        users.forEach(user -> assertTrue(
                orderDao.count(Restrictions.eq(Order::getUser, user.getId())) > 1));
    }

    /** The products which have never been sold, which is a not exists subquery. */
    @Test
    public void testNeverSoldProducts() {
        JpaQuery<Product, Product> query = productDao.query();
        JpaSubQuery<OrderProduct, Long> subQuery =
                query.subQuery(OrderProduct.class, "op", Long.class)
                        .filter(Restrictions.eq(OrderProduct::getProduct, Product::getId))
                        .select(OrderProduct::getId);
        List<Product> products =
                query.filter(Restrictions.exists(subQuery).not()).selectThis().list();
        products.forEach(product -> assertEquals(0L,
                orderProductDao.count(Restrictions.eq(OrderProduct::getProduct, product.getId()))));
    }

    /** The products dearer than the cheapest one, which compares against a scalar subquery. */
    @Test
    public void testProductsDearerThanCheapest() {
        assumeTrue(JpaProviders.getProvider().supportsSubQueryAsExpression(), "The provider takes no subquery as an expression");
        BigDecimal cheapest = productDao.min("price", null, BigDecimal.class);
        JpaQuery<Product, Product> query = productDao.query();
        JpaSubQuery<Product, BigDecimal> subQuery =
                query.subQuery(Product.class, "p2", BigDecimal.class)
                        .select(Fields.min(Product::getPrice));
        List<Product> products = query
                .filter(Restrictions.gt(Property.forName(Product::getPrice), subQuery)).selectThis()
                .list();
        assertEquals(TOTAL_PRODUCTS - 1, products.size());
        products.forEach(product -> assertTrue(product.getPrice().compareTo(cheapest) > 0));
    }

    /**
     * A subquery nested in another subquery: the products stocked more than 1000 which have ever
     * been ordered by a vip.
     */
    @Test
    public void testNestedSubQueries() {
        assumeTrue(JpaProviders.getProvider().supportsSubQueryAsExpression(), "The provider takes no subquery as an expression");
        JpaQuery<Product, Product> query = productDao.query();
        JpaSubQuery<Stock, Long> stocked = query.subQuery(Stock.class, "s", Long.class)
                .filter(Restrictions.gt(Stock::getAmount, 1000L)).select(Stock::getProductId);
        JpaSubQuery<OrderProduct, Long> orderedByVip =
                query.subQuery(OrderProduct.class, "op", Long.class);
        orderedByVip.join(OrderProduct::getOrder, "o", null).join(Order::getUser, "u", null)
                .filter(Restrictions.eq(User::getVip, true));
        orderedByVip.select(Property.forName(OrderProduct::getProduct, Long.class));
        List<Product> products = query.filter(new FilterList()
                .in(Property.forName(null, "id", Long.class), stocked).and()
                .in(Property.forName(null, "id", Long.class), orderedByVip)).selectThis().list();
        products.forEach(product -> log.info(product.toString()));
        assertTrue(products.size() <= TOTAL_PRODUCTS);
    }

    /** Group the products into the price levels, which is a case when expression. */
    @Test
    public void testGroupByPriceLevel() {
        IfExpression<String, String> level = new IfExpression<String, String>(
                Fields.gte(Product::getPrice, BigDecimal.valueOf(200)).as("high").toString())
                        .otherwise("other");
        assertTrue(level != null);

        List<Tuple> dataList = productDao.customQuery()
                .groupBy(new FieldList().addFields(Property.forName(Product::getLocation)))
                .having(Restrictions.gte(count(Product::getId), 2L))
                .select(new ColumnList().addColumns(Product::getLocation)
                        .addColumns(count(Product::getId).as("total"),
                                avg(Product::getPrice).as("avgPrice")))
                .list();
        // Australia(3), China(2) and Thailand(2)
        assertEquals(3, dataList.size());
        dataList.forEach(tuple -> assertTrue(((Number) tuple.get("total")).intValue() >= 2));
    }

    /** Both the stock and the sold amount of every product, which correlates two subqueries. */
    @Test
    public void testStockAgainstSales() {
        assumeTrue(JpaProviders.getProvider().supportsSubQueryAsSelection(), "The provider selects no subquery as a column");
        JpaQuery<Product, Tuple> query = productDao.customQuery();
        JpaSubQuery<Stock, Long> stock = query.subQuery(Stock.class, "s", Long.class)
                .filter(Restrictions.eq(Stock::getProductId, Product::getId))
                .select(Fields.max(Stock::getAmount));
        List<Map<String, Object>> dataList = query.sort(JpaSort.asc(Product::getName))
                .select(new ColumnList().addColumns(Product::getName)
                        .addColumns(Column.forSubQuery(stock, "stockAmount")))
                .setTransformer(Transformers.asCaseInsensitiveMap()).list();
        assertEquals(TOTAL_PRODUCTS, dataList.size());
        dataList.forEach(vo -> {
            log.info(vo.toString());
            assertTrue(((Number) vo.get("stockAmount")).longValue() > 0L);
        });
    }

    /**
     * The order details, which joins four tables and pages the result, so the counting has to
     * agree with the rows.
     */
    @Test
    public void testOrderDetails() throws Exception {
        JpaPageResultSet<Tuple> resultSet = orderProductDao.customPage()
                .join(OrderProduct::getOrder, "o", null).join(Order::getUser, "u", null)
                .join(OrderProduct::getProduct, "p", null)
                .filter(new FilterList().notNull(Product::getDiscount).and()
                        .in(Property.forName("o", "status"),
                                List.of(OrderStatus.PAID, OrderStatus.SHIPPED)))
                .sort(JpaSort.desc(Order::getOrderDate), JpaSort.asc(Product::getName))
                .select(new ColumnList().addColumns(User::getUsername)
                        .addColumns(Order::getId, Order::getOrderDate, Order::getStatus)
                        .addColumns(Product::getName, Product::getPrice)
                        .addColumns(OrderProduct::getAmount)
                        .addColumns(multiply(Property.forName(Product::getPrice),
                                Property.forName(OrderProduct::getAmount)).as("subtotal")));
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());

        resultSet.setTransformer(Transformers.asCaseInsensitiveMap()).paginate(PageRequest.of(10))
                .forEachPage(eachPage -> {
                    eachPage.getContent().forEach(vo -> {
                        log.info(vo.toString());
                        assertTrue(((Number) vo.get("subtotal")).doubleValue() > 0d);
                    });
                });
    }

    /** A self join, which compares the orders of the same user against each other. */
    @Test
    public void testSelfJoin() {
        List<Tuple> dataList = orderDao.customQuery().crossJoin(Order.class, "o2")
                .filter(new FilterList()
                        .eq(Property.forName("o2", "user"), Property.forName(null, "user")).and()
                        .gt(Property.forName("o2", "totalPrice", BigDecimal.class),
                                Property.forName(null, "totalPrice", BigDecimal.class)))
                .select(new ColumnList().addColumns(Property.forName("this", "id").as("orderId"),
                        Property.forName("o2", "id").as("otherOrderId")))
                .list(20, 0);
        dataList.forEach(tuple -> {
            assertTrue(!tuple.get(0).equals(tuple.get(1)));
        });
    }

    /** The sales of every month, which groups by a date function. */
    @Test
    public void testMonthlySales() {
        assumeTrue(JpaProviders.getProvider().supportsPassThroughFunction(), "The provider passes no database function through");
        assumeTrue(JpaProviders.getProvider().supportsDatePart(),
                "The provider renders no date part");
        List<Tuple> dataList = orderDao.customQuery()
                .groupBy(new FieldList().addFields(Fields.month(Order::getOrderDate)))
                .select(new ColumnList()
                        .addColumns(Fields.month(Order::getOrderDate).as("month"))
                        .addColumns(count(Order::getId).as("orderAmount"),
                                sum(Order::getTotalPrice).as("totalPrice")))
                .list();
        long orders = 0L;
        for (Tuple tuple : dataList) {
            log.info("Month: {}, orders: {}", tuple.get("month"), tuple.get("orderAmount"));
            orders += ((Number) tuple.get("orderAmount")).longValue();
        }
        assertEquals(orderDao.count(), orders);
    }

    /** Update the stock according to what has been sold, which sets an attribute by a subquery. */
    @Test
    public void testUpdateStockBySales() {
        long affected = stockDao.update()
                .setField(Stock::getAmount, Fields.minusValue(Stock::getAmount, 1L))
                .filter(Restrictions.gt(Stock::getAmount, 0L)).execute();
        assertEquals(stockDao.count(), affected);
    }

    /** Cancel the orders which contain a product out of stock. */
    @Test
    public void testCancelOrdersOfEmptyStock() {
        long expected = orderDao.count(Restrictions.ne(Order::getStatus, OrderStatus.CANCELLED));
        int rows = orderDao.update().set(Order::getStatus, OrderStatus.CANCELLED)
                .filter(Restrictions.ne(Order::getStatus, OrderStatus.CANCELLED)).execute();
        assertEquals(expected, rows);
        assertEquals(orderDao.count(),
                orderDao.count(Restrictions.eq(Order::getStatus, OrderStatus.CANCELLED)));
    }


    /** A subquery joins, groups, distincts and selects just like an ordinary query does. */
    @Test
    public void testSubQueryVariants() {
        // Distinct the product ids which have ever been ordered
        JpaQuery<Product, Product> query = productDao.query();
        JpaSubQuery<OrderProduct, Long> ordered =
                query.subQuery(OrderProduct.class, "op", Long.class);
        ordered.select("op", "product.id").distinct();
        List<Product> products = query
                .filter(Restrictions.in(Property.forName(null, "id", Long.class), ordered))
                .selectThis().list();
        assertTrue(products.size() <= TOTAL_PRODUCTS);

        // The products ordered by more than one order, which groups inside the subquery
        JpaQuery<Product, Product> another = productDao.query();
        JpaSubQuery<OrderProduct, Long> popular =
                another.subQuery(OrderProduct.class, "op2", Long.class);
        popular.groupBy(new FieldList().addFields(Property.forName("op2", "product")))
                .having(Restrictions.gt(Fields.count("op2", "id"), 1L))
                .select(Property.forName("op2", "product.id", Long.class));
        another.filter(Restrictions.in(Property.forName(null, "id", Long.class), popular))
                .selectThis().list()
                .forEach(product -> assertTrue(orderProductDao
                        .count(Restrictions.eq(OrderProduct::getProduct, product.getId())) > 1));
    }

    /** A subquery may join its own tables, either by an inner join or by an outer one. */
    @Test
    public void testSubQueryJoins() {
        JpaQuery<Product, Product> query = productDao.query();
        JpaSubQuery<OrderProduct, Long> subQuery =
                query.subQuery(OrderProduct.class, "op", Long.class);
        subQuery.leftJoin(OrderProduct::getOrder, "o", null)
                .filter(Restrictions.eq(Order::getStatus, OrderStatus.PAID));
        subQuery.select(Property.forName("op", "product.id", Long.class));
        List<Product> products = query
                .filter(Restrictions.in(Property.forName(null, "id", Long.class), subQuery))
                .selectThis().list();
        assertTrue(products.size() <= TOTAL_PRODUCTS);
    }

    /** An if expression may map a value to another Field rather than to a constant. */
    @Test
    public void testIfExpressionVariants() {
        IfExpression<String, BigDecimal> price =
                new IfExpression<String, BigDecimal>(Property.forName(Product::getLocation))
                        .when("China", Property.forName(Product::getPrice))
                        .otherwise(Fields.toBigDecimal(BigDecimal.ZERO));
        List<Tuple> dataList = productDao.customQuery()
                .select(new ColumnList().addColumns(Product::getLocation)
                        .addColumns(price.as("chinesePrice")))
                .list();
        assertEquals(TOTAL_PRODUCTS, dataList.size());
        dataList.forEach(tuple -> {
            BigDecimal value = (BigDecimal) tuple.get("chinesePrice");
            assertTrue("China".equals(tuple.get(0)) ? value.compareTo(BigDecimal.ZERO) > 0
                    : value.compareTo(BigDecimal.ZERO) == 0);
        });

        IfExpression<String, String> byName = new IfExpression<String, String>("location")
                .when("China", "Asia").otherwise("Other");
        assertEquals(TOTAL_PRODUCTS, productDao.customQuery()
                .select(new ColumnList().addColumns(byName.as("area"))).list().size());
        IfExpression<String, String> byAlias = new IfExpression<String, String>("this", "location")
                .when("China", "Asia").otherwise("Other");
        assertEquals(TOTAL_PRODUCTS, productDao.customQuery()
                .select(new ColumnList().addColumns(byAlias.as("area"))).list().size());
    }

    /** Join by an entity class, which resolves the association itself. */
    @Test
    public void testJoinByEntityClass() {
        List<Tuple> dataList = orderDao.customQuery().join(User.class, "u", null)
                .select(new ColumnList().addColumns(Order::getId).addColumns(User::getUsername))
                .list();
        assertEquals(orderDao.count(), dataList.size());

        List<Tuple> leftJoined = orderDao.customQuery().leftJoin(User.class, "u", null)
                .select(new ColumnList().addColumns(Order::getId).addColumns(User::getUsername))
                .list();
        assertEquals(orderDao.count(), leftJoined.size());
    }


    /**
     * The old fashioned join: two tables put side by side and correlated by the where clause, as in
     * <code>from example_user u, example_order o where u.id = o.user_id</code>.
     */
    @Test
    public void testWhereJoinUsersAndOrders() {
        List<Tuple> dataList = userDao.customQuery().crossJoin(Order.class, "o")
                .filter(Restrictions.eq(Property.forName("this", "id"),
                        Property.forName("o", "user.id")))
                .sort(JpaSort.asc(User::getUsername))
                .select(new ColumnList().addColumns(User::getUsername)
                        .addColumns(Property.forName("o", "id").as("orderId"),
                                Property.forName("o", "totalPrice").as("totalPrice")))
                .list();
        assertEquals(orderDao.count(), dataList.size());
        dataList.forEach(tuple -> assertTrue(((BigDecimal) tuple.get("totalPrice"))
                .compareTo(BigDecimal.ZERO) >= 0));
    }

    /** The same where join, aggregated by user and paginated. */
    @Test
    public void testWhereJoinAggregation() throws Exception {
        JpaPageResultSet<Tuple> resultSet = userDao.customPage().crossJoin(Order.class, "o")
                .filter(new FilterList()
                        .eq(Property.forName("this", "id"), Property.forName("o", "user.id")).and()
                        .ne(Property.forName("o", "status"), OrderStatus.CANCELLED))
                .groupBy(new FieldList().addFields(Property.forName("this", "username")))
                .having(Restrictions.gt(Fields.count("o", "id"), 0L))
                .sort(JpaSort.desc(2))
                .select(new ColumnList().addColumn("this", "username")
                        .addColumns(Fields.count("o", "id").as("orderAmount"),
                                Fields.sum("o", "totalPrice", BigDecimal.class).as("totalPrice")));
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
        assertTrue(resultSet.rowCount() <= TOTAL_USERS);

        long orders = 0L;
        for (Tuple tuple : resultSet.list()) {
            orders += ((Number) tuple.get("orderAmount")).longValue();
        }
        assertEquals(orderDao.count(Restrictions.ne(Order::getStatus, OrderStatus.CANCELLED)),
                orders);
    }

    /** Three tables correlated by the where clause. */
    @Test
    public void testWhereJoinThreeTables() {
        List<Tuple> dataList = orderDao.customQuery().crossJoin(OrderProduct.class, "op")
                .crossJoin(Product.class, "p")
                .filter(new FilterList()
                        .eq(Property.forName("op", "order.id"), Property.forName("this", "id"))
                        .and()
                        .eq(Property.forName("op", "product.id"), Property.forName("p", "id")))
                .select(new ColumnList().addColumn("this", "id")
                        .addColumns(Property.forName("p", "name").as("productName"),
                                Property.forName("op", "amount").as("amount")))
                .list();
        assertEquals(orderProductDao.count(), dataList.size());
        dataList.forEach(tuple -> assertTrue(((Number) tuple.get("amount")).intValue() > 0));
    }

    /** A where join whose correlation is a subquery instead of an attribute. */
    @Test
    public void testWhereJoinAgainstSubQuery() {
        JpaQuery<User, Tuple> query = userDao.customQuery();
        JpaSubQuery<Order, Long> lastOrder = query.subQuery(Order.class, "o2", Long.class)
                .filter(Restrictions.eq(Property.forName("o2", "user.id"),
                        Property.forName("this", "id")))
                .select(Fields.max("o2", "id", Long.class));
        List<Tuple> dataList = query.crossJoin(Order.class, "o")
                .filter(new FilterList()
                        .eq(Property.forName("this", "id"), Property.forName("o", "user.id")).and()
                        .eq(Property.forName("o", "id", Long.class), lastOrder))
                .select(new ColumnList().addColumn("this", "username")
                        .addColumns(Property.forName("o", "id").as("orderId")))
                .list();
        assertTrue(dataList.size() <= TOTAL_USERS);
        dataList.forEach(tuple -> log.info(tuple.toString()));
    }


    /**
     * Two branches growing from the same entity: the order product reaches the user through its
     * order, and reaches the product directly. The lambda tells where every join starts from.
     */
    @Test
    public void testBranchedJoins() throws Exception {
        JpaPageResultSet<Tuple> resultSet = orderProductDao.customPage()
                .join(OrderProduct::getOrder, "o", null).join(Order::getUser, "u", null)
                .join(OrderProduct::getProduct, "p", null)
                .filter(Restrictions.notNull(Product::getDiscount))
                .sort(JpaSort.asc(Product::getName))
                .select(new ColumnList().addColumns(User::getUsername)
                        .addColumns(Order::getId, Order::getStatus)
                        .addColumns(Product::getName, Product::getPrice)
                        .addColumns(OrderProduct::getAmount));
        List<Tuple> dataList = resultSet.list();
        assertEquals(dataList.size(), (int) resultSet.rowCount());
        assertEquals(orderProductDao.customQuery().join(OrderProduct::getProduct, "p", null)
                .filter(Restrictions.notNull(Product::getDiscount))
                .select(new ColumnList(OrderProduct::getId)).list().size(), dataList.size());
        dataList.forEach(tuple -> assertTrue(tuple.get("username") != null));
    }

    /** A branch may be an outer join, which a cross join could never express. */
    @Test
    public void testBranchedLeftJoin() {
        List<Tuple> dataList = orderProductDao.customQuery()
                .join(OrderProduct::getOrder, "o", null).join(Order::getUser, "u", null)
                .leftJoin(OrderProduct::getProduct, "p", null)
                .select(new ColumnList().addColumns(User::getUsername).addColumns(Order::getId)
                        .addColumns(Product::getName))
                .list();
        assertEquals(orderProductDao.count(), dataList.size());
    }

    /** The branch may also be started explicitly by the alias it grows from. */
    @Test
    public void testBranchedJoinByAlias() {
        List<Tuple> dataList = orderProductDao.customQuery()
                .join(OrderProduct::getOrder, "o", null).join(Order::getUser, "u", null)
                .join("this", "product", "p", null)
                .select(new ColumnList().addColumns(User::getUsername)
                        .addColumns(Property.forName("p", "name").as("productName")))
                .list();
        assertEquals(orderProductDao.count(), dataList.size());
        dataList.forEach(tuple -> assertTrue(tuple.get("productName") != null));
    }

    /**
     * Joining by an entity class branches as well: the class is looked up in the entity the last
     * join reached, and then in the ones before it, so Product is found back on the root.
     */
    @Test
    public void testBranchedJoinByEntityClass() {
        List<Tuple> dataList = orderProductDao.customQuery()
                .join(Order.class, "o", null).join(User.class, "u", null)
                .join(Product.class, "p", null)
                .select(new ColumnList().addColumns(User::getUsername)
                        .addColumns(Property.forName("p", "name").as("productName")))
                .list();
        assertEquals(orderProductDao.count(), dataList.size());
        dataList.forEach(t -> assertTrue(t.get("productName") != null));
    }


    /** Fetching a to-one association loads it along with the entities, sparing the extra select. */
    @Test
    public void testFetchToOne() {
        List<Order> orders = orderDao.query().fetch(Order::getUser)
                .filter(Restrictions.ne(Order::getStatus, OrderStatus.CANCELLED)).selectThis()
                .list();
        assertEquals(orderDao.count(Restrictions.ne(Order::getStatus, OrderStatus.CANCELLED)),
                orders.size());
        orders.forEach(order -> {
            assertTrue(isLoaded(order.getUser()));
            assertTrue(order.getUser().getUsername() != null);
        });
    }

    /** Fetching a collection needs an outer join, otherwise the empty ones would be dropped. */
    @Test
    public void testLeftFetchCollection() {
        List<User> users = userDao.query().leftFetch(User::getOrders).distinct().selectThis()
                .list();
        assertEquals(TOTAL_USERS, users.size());
        users.forEach(user -> assertTrue(isLoaded(user.getOrders())));
    }

    /**
     * A fetch may grow on a join, addressed by the alias that join was given, as long as the
     * entity owning it is the one being selected.
     */
    @Test
    public void testFetchUponJoin() {
        List<Order> orders = orderProductDao.query(Order.class)
                .join(OrderProduct::getOrder, "o", null).fetch("o", "user")
                .filter(Restrictions.ne(Order::getStatus, OrderStatus.CANCELLED))
                .selectAlias("o").list();
        assertTrue(orders.size() > 0);
        orders.forEach(order -> assertTrue(isLoaded(order.getUser())));
    }

    /** A pagination fetches on the query it lists, never on the one it counts. */
    @Test
    public void testFetchWithPagination() throws Exception {
        JpaPageResultSet<Order> resultSet = orderDao.page().fetch(Order::getUser)
                .filter(Restrictions.ne(Order::getStatus, OrderStatus.CANCELLED)).selectThis();
        assertEquals(orderDao.count(Restrictions.ne(Order::getStatus, OrderStatus.CANCELLED)),
                resultSet.rowCount());
        List<Order> orders = resultSet.list(5, 0);
        assertTrue(orders.size() <= 5);
        orders.forEach(order -> assertTrue(isLoaded(order.getUser())));
    }


    /**
     * Join an aggregating subquery as a derived table, which is how every product gets its sales
     * without a correlated subquery per row.
     */
    @Test
    public void testJoinDerivedTable() {
        assumeTrue(JpaProviders.getProvider().supportsDerivedTable(),
                "The provider joins no derived table");
        JpaQuery<Product, Tuple> query = productDao.customQuery();
        JpaSubQuery<OrderProduct, Tuple> sales =
                query.subQuery(OrderProduct.class, "op", Tuple.class);
        sales.groupBy(new FieldList().addFields(Property.forName("op", "product.id")))
                .select(new ColumnList()
                        .addColumns(Property.forName("op", "product.id").as("productId"))
                        .addColumns(Fields.sum("op", "amount", Integer.class).as("soldAmount"),
                                Fields.count("op", "id").as("orderAmount")));

        List<Map<String, Object>> dataList = query
                .joinSubQuery(sales, "s", Restrictions.eq(Property.forName("s", "productId"),
                        Property.forName("this", "id")))
                .sort(JpaSort.desc(Property.forName("s", "soldAmount")))
                .select(new ColumnList().addColumns(Product::getName, Product::getPrice)
                        .addColumns(Property.forName("s", "soldAmount").as("soldAmount"),
                                Property.forName("s", "orderAmount").as("orderAmount")))
                .setTransformer(Transformers.asCaseInsensitiveMap()).list();

        assertTrue(dataList.size() > 0);
        long previous = Long.MAX_VALUE;
        for (Map<String, Object> vo : dataList) {
            log.info(vo.toString());
            long sold = ((Number) vo.get("soldAmount")).longValue();
            assertTrue(sold > 0 && sold <= previous);
            previous = sold;
        }
    }

    /** An outer join keeps the products which the derived table knows nothing about. */
    @Test
    public void testLeftJoinDerivedTable() {
        assumeTrue(JpaProviders.getProvider().supportsDerivedTable(),
                "The provider joins no derived table");
        JpaQuery<Product, Tuple> query = productDao.customQuery();
        JpaSubQuery<OrderProduct, Tuple> sales =
                query.subQuery(OrderProduct.class, "op", Tuple.class);
        sales.filter(Restrictions.gt(Property.forName("op", "amount", Integer.class), 15))
                .groupBy(new FieldList().addFields(Property.forName("op", "product.id")))
                .select(new ColumnList()
                        .addColumns(Property.forName("op", "product.id").as("productId"))
                        .addColumns(Fields.sum("op", "amount", Integer.class).as("soldAmount")));

        List<Map<String, Object>> dataList = query
                .leftJoinSubQuery(sales, "s", Restrictions.eq(Property.forName("s", "productId"),
                        Property.forName("this", "id")))
                .sort(JpaSort.asc(Product::getName))
                .select(new ColumnList().addColumns(Product::getName)
                        .addColumns(Property.forName("s", "soldAmount").as("soldAmount")))
                .setTransformer(Transformers.asCaseInsensitiveMap()).list();
        // Every product is kept, whether it sold that much or not
        assertEquals(TOTAL_PRODUCTS, dataList.size());
        assertTrue(dataList.stream().anyMatch(vo -> vo.get("soldAmount") != null));
    }


    /** A pagination joins the derived table into its counting query as well. */
    @Test
    public void testPaginateDerivedTable() throws Exception {
        assumeTrue(JpaProviders.getProvider().supportsDerivedTable(),
                "The provider joins no derived table");
        JpaPage<Product, Tuple> page = productDao.customPage();
        JpaSubQuery<OrderProduct, Tuple> sales =
                page.subQuery(OrderProduct.class, "op", Tuple.class);
        sales.groupBy(new FieldList().addFields(Property.forName("op", "product.id")))
                .having(Restrictions.gt(Fields.sum("op", "amount", Integer.class), 20))
                .select(new ColumnList()
                        .addColumns(Property.forName("op", "product.id").as("productId"))
                        .addColumns(Fields.sum("op", "amount", Integer.class).as("soldAmount")));

        JpaPageResultSet<Tuple> resultSet = page
                .joinSubQuery(sales, "s", Restrictions.eq(Property.forName("s", "productId"),
                        Property.forName("this", "id")))
                .sort(JpaSort.desc(Property.forName("s", "soldAmount")))
                .select(new ColumnList().addColumns(Product::getName)
                        .addColumns(Property.forName("s", "soldAmount").as("soldAmount")));
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
        assertTrue(resultSet.rowCount() <= TOTAL_PRODUCTS);
        resultSet.list().forEach(tuple -> assertTrue(
                ((Number) tuple.get("soldAmount")).intValue() > 20));
    }


    /** A derived table needs no grouping: here it is just the filtered detail rows. */
    @Test
    public void testJoinDerivedTableWithoutGrouping() {
        assumeTrue(JpaProviders.getProvider().supportsDerivedTable(),
                "The provider joins no derived table");
        JpaQuery<Product, Tuple> query = productDao.customQuery();
        JpaSubQuery<OrderProduct, Tuple> details =
                query.subQuery(OrderProduct.class, "op", Tuple.class);
        details.filter(Restrictions.gt(Property.forName("op", "amount", Integer.class), 15))
                .select(new ColumnList()
                        .addColumns(Property.forName("op", "product.id").as("productId"))
                        .addColumns(Property.forName("op", "amount").as("amount")));

        List<Map<String, Object>> dataList = query
                .joinSubQuery(details, "d", Restrictions.eq(Property.forName("d", "productId"),
                        Property.forName("this", "id")))
                .select(new ColumnList().addColumns(Product::getName)
                        .addColumns(Property.forName("d", "amount").as("amount")))
                .setTransformer(Transformers.asCaseInsensitiveMap()).list();
        dataList.forEach(vo -> assertTrue(((Number) vo.get("amount")).intValue() > 15));
    }

    /** The grouping subquery may be handed over directly, without keeping a reference to it. */
    @Test
    public void testJoinGroupedSubQueryDirectly() {
        assumeTrue(JpaProviders.getProvider().supportsDerivedTable(),
                "The provider joins no derived table");
        JpaQuery<Product, Tuple> query = productDao.customQuery();
        List<Map<String, Object>> dataList = query
                .joinSubQuery(
                        query.subQuery(OrderProduct.class, "op", Tuple.class)
                                .groupBy(new FieldList()
                                        .addFields(Property.forName("op", "product.id")))
                                .select(new ColumnList()
                                        .addColumns(Property.forName("op", "product.id")
                                                .as("productId"))
                                        .addColumns(Fields.count("op", "id").as("orderAmount"))),
                        "s",
                        Restrictions.eq(Property.forName("s", "productId"),
                                Property.forName("this", "id")))
                .select(new ColumnList().addColumns(Product::getName)
                        .addColumns(Property.forName("s", "orderAmount").as("orderAmount")))
                .setTransformer(Transformers.asCaseInsensitiveMap()).list();
        assertTrue(dataList.size() > 0);
        dataList.forEach(vo -> assertTrue(((Number) vo.get("orderAmount")).intValue() > 0));
    }

    /** Whether an association has been loaded already, which every provider answers. */
    private boolean isLoaded(Object value) {
        return Persistence.getPersistenceUtil().isLoaded(value);
    }

    @AfterAll
    public void end() {
        log.info("=========== ComplexQueryTests End. ===========");
    }

    @Getter
    @Setter
    @ToString
    public static class SalesVo {

        private String name;
        private Long soldAmount;
        private Long orderAmount;
        private BigDecimal turnover;

    }

}
