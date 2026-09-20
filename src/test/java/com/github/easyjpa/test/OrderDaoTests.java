package com.github.easyjpa.test;

import static com.github.easyjpa.Fields.abs;
import static com.github.easyjpa.Fields.count;
import static com.github.easyjpa.Fields.minus;
import static com.github.easyjpa.Fields.multiply;
import static com.github.easyjpa.Fields.sum;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import com.github.easyjpa.ColumnList;
import com.github.easyjpa.FieldList;
import com.github.easyjpa.Fields;
import com.github.easyjpa.FilterList;
import com.github.easyjpa.JpaQuery;
import com.github.easyjpa.JpaSort;
import com.github.easyjpa.JpaSubQuery;
import com.github.easyjpa.JpaDelete;
import com.github.easyjpa.JpaPageResultSet;
import com.github.easyjpa.JpaUpdate;
import com.github.easyjpa.Property;
import com.github.easyjpa.JpaProviders;
import com.github.easyjpa.Restrictions;
import com.github.easyjpa.Transformers;
import com.github.easyjpa.page.PageRequest;
import com.github.easyjpa.test.dao.OrderDao;
import com.github.easyjpa.test.dao.OrderProductDao;
import com.github.easyjpa.test.dao.ProductDao;
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
import jakarta.persistence.Tuple;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @Description: OrderDaoTests
 * @Author: Fred Feng
 * @Date: 19/03/2025
 * @Version 1.0.0
 */
public class OrderDaoTests extends AbstractDaoTests {

    private static final Logger log = LoggerFactory.getLogger(OrderDaoTests.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserOrderService userOrderService;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderProductDao orderProductDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private ProductDao productDao;

    @BeforeAll
    public void begin() {
        log.info("=========== OrderDaoTests Begin. ===========");
        userService.saveRandomUsers();
        productService.saveRandomProducts();
        userOrderService.makeRandomOrders();
    }

    @Test
    public void test1() {
        final AtomicInteger counter = new AtomicInteger();
        JpaQuery<Order, Tuple> jpaQuery = orderDao.customQuery();
        JpaSubQuery<User, Long> jpaSubQuery = jpaQuery.subQuery(User.class, "u", Long.class)
                .filter(Restrictions.eq(User::getId, Order::getUser)).select(Fields.toLong(1L));
        jpaQuery.filter(Restrictions.exists(jpaSubQuery)).distinct()
                .select(new ColumnList(Order::getUser)).list().forEach(m -> {
                    log.info(m.toString());
                    counter.incrementAndGet();
                });
        assertTrue(counter.get() <= 5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Microwave oven", "Coffee maker"})
    public void test2(String itemName) {
        final AtomicInteger counter = new AtomicInteger();
        JpaQuery<OrderProduct, Tuple> jpaQuery = orderProductDao.customQuery();
        JpaSubQuery<Product, Long> jpaSubQuery = jpaQuery.subQuery(Product.class, "p", Long.class)
                .filter(new FilterList().eq(Product::getId, OrderProduct::getProduct).and()
                        .eq(Product::getName, itemName))
                .select(Product::getId);
        jpaQuery.leftJoin(OrderProduct::getProduct, "p", null).join(Order.class, "o", null)
                .join(User.class, "u", null).filter(Restrictions.exists(jpaSubQuery))
                .select(new ColumnList(OrderProduct::getOrder, OrderProduct::getProduct,
                        OrderProduct::getAmount).addColumns(Product::getName)
                                .addColumns(User::getUsername))
                .setTransformer(Transformers.asMap()).list(10).forEach(m -> {
                    log.info(m.toString());
                    counter.incrementAndGet();
                });
        assertTrue(counter.get() <= 10);
    }

    @Test
    public void test3() {
        final List<Map<String, Object>> dataList = new ArrayList<>();
        orderDao.customPage().join(Order::getUser, "u", null)
                .filter(Restrictions.between(Order::getOrderDate,
                        LocalDate.of(2025, 2, 1).atStartOfDay(),
                        LocalDate.of(2025, 2, 28).atStartOfDay()))
                .groupBy(
                        new FieldList().addFields(Order::getOrderDate).addFields(User::getUsername))
                .having(Restrictions.gt(Fields.avg(Order::getTotalPrice), 20000D))
                .sort(JpaSort.desc(Order::getOrderDate))
                .select(new ColumnList().addColumns(User::getUsername)
                        .addColumns(Order::getOrderDate)
                        .addColumns(Fields.avg(Order::getTotalPrice).as("avgTotalPrice")))
                .setTransformer(Transformers.asCaseInsensitiveMap()).paginate(PageRequest.of(5))
                .forEachPage(eachPage -> {
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
            LocalDateTime ldt = (LocalDateTime) vo.get("orderDate");
            if (ldt.compareTo(LocalDate.of(2025, 2, 1).atStartOfDay()) >= 0
                    && ldt.compareTo(LocalDate.of(2025, 2, 28).atStartOfDay()) <= 0) {
                counter.incrementAndGet();
            }
        });
        assertTrue(counter.get() == dataList.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Petter", "Jack"})
    public void test4(String username) {
        List<User> userList = new ArrayList<>();
        orderDao.customQuery().join(Order::getUser, "u", null)
                .filter(Restrictions.eq(User::getUsername, username))
                .sort(JpaSort.desc(Order::getOrderDate))
                .select(new ColumnList().addFields(Fields.root()).addTableAlias("u")).list()
                .forEach(t -> {
                    Order order = (Order) t.get(0);
                    User user = (User) t.get(1);
                    log.info("Order: " + order + ", User: " + user);
                    userList.add(user);
                });
        AtomicInteger counter = new AtomicInteger();
        userList.forEach(u -> {
            if ("Petter".equals(u.getUsername()) || "Jack".equals(u.getUsername())) {
                counter.incrementAndGet();
            }
        });
        assertTrue(counter.get() == userList.size());
    }

    @Test
    public void test5() {
        List<Map<String, Object>> dataList = new ArrayList<>();
        orderDao.customPage().leftJoin(Order::getOrderProducts, "op", null)
                .leftJoin(OrderProduct::getProduct, "p", null)
                .filter(Restrictions.eq(Product::getLocation, "Australia"))
                .sort(JpaSort.desc(Order::getOrderDate))
                .select(new ColumnList().addFields(Fields.root()).addTableAlias("p"))
                .setTransformer(Transformers.asMap()).paginate(PageRequest.of(10))
                .forEachPage(eachPage -> {
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
            String location = (String) vo.get("location");
            if ("Australia".equals(location)) {
                counter.incrementAndGet();
            }
        });
        assertTrue(counter.get() == dataList.size());
    }

    @Test
    public void test6() {
        assumeTrue(JpaProviders.getProvider().supportsRightJoin(), "The provider renders no right join");
        List<Map<String, Object>> dataList = new ArrayList<>();
        orderDao.customPage().rightJoin(Order::getOrderProducts, "op", null)
                .rightJoin(OrderProduct::getProduct, "p", null)
                .filter(Restrictions.eq(Product::getLocation, "Australia"))
                .sort(JpaSort.desc(Order::getOrderDate), JpaSort.desc(OrderProduct::getAmount))
                .select(new ColumnList()
                        .addColumns(Order::getId, Order::getTotalPrice, Order::getOrderDate)
                        .addColumns(OrderProduct::getAmount)
                        .addColumns(Product::getName, Product::getLocation))
                .setTransformer(Transformers.asMap()).paginate(PageRequest.of(10))
                .forEachPage(eachPage -> {
                    log.info(String.format(
                            "====================== PageNumber/TotalPage: %s/%s  Total Records: %s ======================",
                            eachPage.getPageNumber(), eachPage.getTotalPages(),
                            eachPage.getTotalRecords()));
                    eachPage.getContent().forEach(vo -> {
                        log.info(vo.toString());
                        dataList.add(vo);
                    });
                });
        AtomicInteger counter = new AtomicInteger();
        dataList.forEach(vo -> {
            String location = (String) vo.get("location");
            if ("Australia".equals(location)) {
                counter.incrementAndGet();
            }
        });
        assertTrue(counter.get() == dataList.size());
    }

    @Test
    public void test7() {
        assumeTrue(JpaProviders.getProvider().supportsOrdinalSort(), "The provider sorts by no column position");
        List<UserOrderVo> dataList = new ArrayList<>();
        userDao.customPage().leftJoin(User::getOrders, "o", null)
                .join(Order::getOrderProducts, "op", null).join(OrderProduct::getProduct, "p", null)
                .filter(Restrictions.notNull(Product::getDiscount)
                        .and(Restrictions.in(Product::getId,
                                productDao.query().subQuery(Stock.class, "s", Long.class)
                                        .filter(Restrictions.gt(Stock::getAmount, 10000L))
                                        .select(Stock::getProductId))))
                .groupBy(new FieldList(Product::getName, Product::getLocation, Product::getPrice,
                        Product::getDiscount))
                .sort(JpaSort.desc(4), JpaSort.desc(5))
                .select(new ColumnList().addColumns(Product::getName, Product::getLocation)
                        .addColumns(count(Product::getId).as("productAmount"),
                                count(Order::getId).as("orderAmount"),
                                sum(OrderProduct::getAmount).as("totalAmount"),
                                abs(minus(
                                        multiply(multiply(Product::getPrice, Product::getDiscount),
                                                sum(OrderProduct::getAmount)),
                                        multiply(Product::getPrice, sum(OrderProduct::getAmount))))
                                                .as("savings")))
                .setTransformer(Transformers.asBean(UserOrderVo.class)).paginate(PageRequest.of(10))
                .forEachPage(eachPage -> {
                    log.info(String.format(
                            "====================== PageNumber/TotalPage: %s/%s  Total Records: %s ======================",
                            eachPage.getPageNumber(), eachPage.getTotalPages(),
                            eachPage.getTotalRecords()));
                    eachPage.getContent().forEach(vo -> {
                        log.info(vo.toString());
                        dataList.add(vo);
                    });
                });
        AtomicInteger counter = new AtomicInteger();
        dataList.forEach(vo -> {
            if (vo.getSavings().doubleValue() > 0) {
                counter.incrementAndGet();
            }
        });
        assertTrue(counter.get() == dataList.size());
    }

    @ParameterizedTest
    @CsvSource({"'Flashlight,Iron'"})
    public void test8(String str) {
        String[] itemNames = str.split(",");
        JpaSubQuery<OrderProduct, Order> subQuery =
                orderDao.query().subQuery(OrderProduct.class, "op", Order.class)
                        .join(OrderProduct::getProduct, "p", null)
                        .filter(new FilterList().eq(Product::getId, OrderProduct::getProduct)
                                .in(Product::getName, List.of(itemNames)))
                        .select(OrderProduct::getOrder);
        int rows = orderDao.delete().filter(Restrictions.exists(subQuery)).execute();
        log.info("Affected rows: {}", rows);
        assertTrue(rows >= 0);
    }


    /** The counting of a pagination has to agree with the rows it lists, joins included. */
    @Test
    public void testJoinPaginationRowCount() throws Exception {
        JpaPageResultSet<Tuple> resultSet = orderDao.customPage()
                .leftJoin(Order::getOrderProducts, "op", null)
                .leftJoin(OrderProduct::getProduct, "p", null)
                .filter(Restrictions.notNull(Product::getDiscount))
                .select(new ColumnList().addColumns(Order::getId, Order::getTotalPrice)
                        .addColumns(OrderProduct::getAmount).addColumns(Product::getName));
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
    }

    /** The counting of a grouping pagination counts the groups, having clause included. */
    @Test
    public void testGroupPaginationRowCount() throws Exception {
        JpaPageResultSet<Tuple> resultSet = orderDao.customPage().join(Order::getUser, "u", null)
                .groupBy(new FieldList().addFields(User::getUsername).addFields(Order::getStatus))
                .having(Restrictions.gt(Fields.count(Order::getId), 0L))
                .select(new ColumnList().addColumns(User::getUsername).addColumns(Order::getStatus)
                        .addColumns(Fields.count(Order::getId).as("orderAmount")));
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
        assertTrue(resultSet.rowCount() <= 5 * OrderStatus.values().length);
    }

    /** Join upon an on condition rather than filtering afterwards. */
    @Test
    public void testJoinOnCondition() {
        List<Tuple> dataList = orderDao.customQuery()
                .leftJoin(Order::getOrderProducts, "op", Restrictions.gt("op", "amount", 10))
                .select(new ColumnList().addColumns(Order::getId)
                        .addColumns(OrderProduct::getAmount))
                .list();
        assertEquals(orderDao.count(), dataList.stream().map(t -> t.get(0)).distinct().count());
        dataList.forEach(t -> {
            Integer amount = (Integer) t.get(1);
            assertTrue(amount == null || amount > 10);
        });
    }

    /** The orders of the users who have ever placed more than one order, by a grouping subquery. */
    @Test
    public void testGroupingSubQuery() {
        assumeTrue(JpaProviders.getProvider().supportsSubQueryAsExpression(), "The provider takes no subquery as an expression");
        JpaQuery<Order, Order> query = orderDao.query();
        JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o2", Long.class);
        subQuery.groupBy(new FieldList().addFields(Order::getUser))
                .having(Restrictions.gt(Fields.count(Order::getId), 1L))
                .select(Property.forName(Order::getUser, Long.class));
        List<Order> orders = query
                .filter(Restrictions.in(Property.forName(null, "user", Long.class), subQuery))
                .selectThis().list();
        orders.forEach(order -> assertTrue(
                orderDao.count(Restrictions.eq(Order::getUser, order.getUser().getId())) > 1));
    }

    /** Aggregate the orders by status, which is an enumeration. */
    @Test
    public void testGroupByEnumeration() {
        List<Tuple> dataList = orderDao.customQuery().groupBy(new FieldList(Order::getStatus))
                .sort(JpaSort.asc(Order::getStatus))
                .select(new ColumnList().addColumns(Order::getStatus)
                        .addColumns(Fields.count(Order::getId).as("orderAmount"),
                                Fields.sum(Order::getTotalPrice).as("totalPrice")))
                .list();
        assertTrue(dataList.size() <= OrderStatus.values().length);
        long total = 0L;
        for (Tuple tuple : dataList) {
            OrderStatus status = (OrderStatus) tuple.get(0);
            long orderAmount = ((Number) tuple.get("orderAmount")).longValue();
            assertEquals(orderDao.count(Restrictions.eq(Order::getStatus, status)), orderAmount);
            total += orderAmount;
        }
        assertEquals(orderDao.count(), total);
    }

    /** Sort by the position of a selected column instead of by an attribute. */
    @Test
    public void testSortByColumnPosition() {
        assumeTrue(JpaProviders.getProvider().supportsOrdinalSort(), "The provider sorts by no column position");
        List<Tuple> dataList = orderDao.customQuery().groupBy(new FieldList(Order::getStatus))
                .sort(JpaSort.desc(2))
                .select(new ColumnList().addColumns(Order::getStatus)
                        .addColumns(Fields.count(Order::getId).as("orderAmount")))
                .list();
        long previous = Long.MAX_VALUE;
        for (Tuple tuple : dataList) {
            long orderAmount = ((Number) tuple.get("orderAmount")).longValue();
            assertTrue(orderAmount <= previous);
            previous = orderAmount;
        }
    }

    /** A pagination can be turned into the Page of Spring Data. */
    @Test
    public void testPaginateToPage() throws Exception {
        Page<Tuple> page = orderDao.customPage().sort(JpaSort.desc(Order::getOrderDate))
                .select(new ColumnList(Order::getId, Order::getOrderDate, Order::getTotalPrice))
                .setTransformer(Transformers.noop()).paginate(PageRequest.of(10)).toPage();
        assertEquals(orderDao.count(), page.getTotalElements());
        assertTrue(page.getContent().size() <= 10);
    }

    /** Every order gets shipped once it has been paid. */
    @Test
    public void testUpdateStatus() {
        long paid = orderDao.count(Restrictions.eq(Order::getStatus, OrderStatus.PAID));
        int rows = orderDao.update().set(Order::getStatus, OrderStatus.SHIPPED)
                .filter(Restrictions.eq(Order::getStatus, OrderStatus.PAID)).execute();
        assertEquals(paid, rows);
        assertEquals(0L, orderDao.count(Restrictions.eq(Order::getStatus, OrderStatus.PAID)));
    }

    /** Discount the orders of the given users, which are matched by a subquery. */
    @Test
    public void testUpdateBySubQuery() {
        assumeTrue(JpaProviders.getProvider().supportsSubQueryAsExpression(), "The provider takes no subquery as an expression");
        JpaUpdate<Order> update = orderDao.update();
        JpaSubQuery<User, Long> subQuery = update.subQuery(User.class, Long.class)
                .filter(Restrictions.eq(User::getVip, true)).select(User::getId);
        int rows = update
                .setField(Order::getTotalPrice, Fields.multiplyValue(Order::getTotalPrice, 0.9))
                .filter(Restrictions.in(Property.forName(Order::getUser, Long.class), subQuery))
                .execute();
        assertTrue(rows > 0);
        assertEquals(rows, orderDao.customQuery().join(Order::getUser, "u", null)
                .filter(Restrictions.eq(User::getVip, true))
                .select(new ColumnList(Order::getId)).list().size());
    }

    /** Delete the orders which contain nothing. */
    @Test
    public void testDeleteByNotExistsSubQuery() {
        JpaDelete<Order> delete = orderDao.delete();
        JpaSubQuery<OrderProduct, Long> subQuery = delete.subQuery(OrderProduct.class, Long.class)
                .filter(Restrictions.eq(OrderProduct::getOrder, Order::getId))
                .select(OrderProduct::getId);
        int rows = delete.filter(Restrictions.exists(subQuery).not()).execute();
        assertEquals(0, rows);
    }


    /** A pagination joins by a lambda, by an entity class or by an attribute name. */
    @Test
    public void testJoinOverloads() throws Exception {
        assumeTrue(JpaProviders.getProvider().supportsRightJoin(), "The provider renders no right join");
        long orders = orderDao.count();
        assertEquals(orders, orderDao.customPage().join(Order::getUser, "u", null)
                .select(new ColumnList().addColumns(Order::getId)).rowCount());
        assertEquals(orders, orderDao.customPage().join(User.class, "u", null)
                .select(new ColumnList().addColumns(Order::getId)).rowCount());
        assertEquals(orders, orderDao.customPage().join("user", "u", null)
                .select(new ColumnList().addColumns(Order::getId)).rowCount());
        assertEquals(orders, orderDao.customPage().leftJoin(User.class, "u", null)
                .select(new ColumnList().addColumns(Order::getId)).rowCount());
        assertEquals(orders, orderDao.customPage().leftJoin("user", "u", null)
                .select(new ColumnList().addColumns(Order::getId)).rowCount());
        assertEquals(orders, orderDao.customPage().rightJoin(Order::getUser, "u", null)
                .select(new ColumnList().addColumns(Order::getId)).rowCount());
        assertEquals(orders, orderDao.customPage().rightJoin(User.class, "u", null)
                .select(new ColumnList().addColumns(Order::getId)).rowCount());
        assertEquals(orders, orderDao.customPage().rightJoin("user", "u", null)
                .select(new ColumnList().addColumns(Order::getId)).rowCount());
        assertEquals(orders * 5, orderDao.customPage().crossJoin(User.class, "u")
                .select(new ColumnList().addColumns(Order::getId)).rowCount());
    }

    /** A query joins in the same ways as a pagination does. */
    @Test
    public void testQueryJoinOverloads() {
        assumeTrue(JpaProviders.getProvider().supportsRightJoin(), "The provider renders no right join");
        long orders = orderDao.count();
        assertEquals(orders, orderDao.customQuery().join(User.class, "u", null)
                .select(new ColumnList().addColumns(Order::getId)).list().size());
        assertEquals(orders, orderDao.customQuery().join("user", "u", null)
                .select(new ColumnList().addColumns(Order::getId)).list().size());
        assertEquals(orders, orderDao.customQuery().leftJoin(User.class, "u", null)
                .select(new ColumnList().addColumns(Order::getId)).list().size());
        assertEquals(orders, orderDao.customQuery().leftJoin("user", "u", null)
                .select(new ColumnList().addColumns(Order::getId)).list().size());
        assertEquals(orders, orderDao.customQuery().rightJoin(User.class, "u", null)
                .select(new ColumnList().addColumns(Order::getId)).list().size());
        assertEquals(orders, orderDao.customQuery().rightJoin("user", "u", null)
                .select(new ColumnList().addColumns(Order::getId)).list().size());
        assertEquals(orders, orderDao.customQuery().rightJoin(Order::getUser, "u", null)
                .select(new ColumnList().addColumns(Order::getId)).list().size());
    }

    /** The pagination of a grouping query groups in every way a query does. */
    @Test
    public void testPageGroupByOverloads() throws Exception {
        long statuses = orderDao.customQuery().groupBy(Order::getStatus)
                .select(new ColumnList().addColumns(Fields.count(1).as("total"))).list().size();
        assertEquals(statuses, orderDao.customPage().groupBy("status")
                .select(new ColumnList().addColumns(Fields.count(1).as("total"))).rowCount());
        assertEquals(statuses, orderDao.customPage().groupBy("this", new String[] {"status"})
                .select(new ColumnList().addColumns(Fields.count(1).as("total"))).rowCount());
        assertEquals(statuses, orderDao.customPage().groupBy(Order::getStatus)
                .select(new ColumnList().addColumns(Fields.count(1).as("total"))).rowCount());
        assertEquals(statuses,
                orderDao.customPage().groupBy(Property.forName(Order::getStatus))
                        .select(new ColumnList().addColumns(Fields.count(1).as("total")))
                        .rowCount());
    }

    @Getter
    @Setter
    @ToString
    private static class UserOrderVo {
        private String name;
        private String location;
        private Long productAmount;
        private Long orderAmount;
        private Integer totalAmount;
        private Number savings;

    }

    @AfterAll
    public void end() {
        log.info("=========== OrderDaoTests End. ===========");
    }

}
