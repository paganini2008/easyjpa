package com.github.easyjpa.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
import com.github.easyjpa.JpaPageResultSet;
import com.github.easyjpa.JpaQuery;
import com.github.easyjpa.JpaSort;
import com.github.easyjpa.JpaSubQuery;
import com.github.easyjpa.Property;
import com.github.easyjpa.JpaProviders;
import com.github.easyjpa.Restrictions;
import com.github.easyjpa.Transformers;
import com.github.easyjpa.page.PageRequest;
import com.github.easyjpa.page.PageResponse;
import com.github.easyjpa.page.PageableQuery;
import com.github.easyjpa.test.dao.OrderDao;
import com.github.easyjpa.test.dao.UserDao;
import com.github.easyjpa.test.entity.Order;
import com.github.easyjpa.test.entity.OrderStatus;
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
 * @Description: UserDaoTests
 * @Author: Fred Feng
 * @Date: 22/03/2025
 * @Version 1.0.0
 */
public class UserDaoTests extends AbstractDaoTests {

    private static final Logger log = LoggerFactory.getLogger(UserDaoTests.class);

    /** See UserService#saveRandomUsers, two of which are vip and one has no email. */
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

    @BeforeAll
    public void begin() {
        log.info("=========== UserDaoTests Begin. ===========");
        userService.saveRandomUsers();
        productService.saveRandomProducts();
        userOrderService.makeRandomOrders();
    }

    @Test
    public void testSelectAll() {
        userDao.query().selectThis().list().forEach(u -> {
            log.info(u.toString());
        });
    }

    @Test
    public void testGetUserByUsernameAndPassword() {
        User user = userDao.query().filter(
                new FilterList().eq(User::getUsername, "Jack").eq(User::getPassword, "123456"))
                .selectThis().one();
        log.info("Load user: {}", user);
        assertTrue(user != null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"scott003", "lee004"})
    public void testGetUserByEmail(String email) {
        assumeTrue(JpaProviders.getProvider().supportsPartialEntity(), "The provider builds no partial entity");
        User user = userDao.query().filter(new FilterList().like(User::getEmail, email))
                .select(new ColumnList(User::getUsername, User::getPassword)).first();
        log.info("Load user: {}", user);
        assertTrue(user != null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc009"})
    public void testGetUserNotFoundByEmail(String email) {
        assumeTrue(JpaProviders.getProvider().supportsPartialEntity(), "The provider builds no partial entity");
        User user = userDao.query().filter(new FilterList().like(User::getEmail, email))
                .select(new ColumnList(User::getUsername, User::getPassword, User::getEmail))
                .first();
        log.info("Load user: {}", user);
        assertTrue(user == null);
    }

    @Test
    public void testFilterByBoolean() {
        assertEquals(2L, userDao.count(Restrictions.eq(User::getVip, true)));
        assertEquals(3L, userDao.count(Restrictions.ne(User::getVip, true)));
    }

    /** Terry is the only one who has no email. */
    @Test
    public void testFilterByNullable() {
        assertEquals(1L, userDao.count(Restrictions.isNull(User::getEmail)));
        assertEquals(TOTAL_USERS - 1, userDao.count(Restrictions.notNull(User::getEmail)));
        User user = userDao.query().filter(Restrictions.isNull(User::getEmail)).selectThis().one();
        assertEquals("Terry", user.getUsername());
    }

    @Test
    public void testFilterByInAndNotIn() {
        List<String> usernames = List.of("Jack", "Petter", "Nobody");
        assertEquals(2L, userDao.count(Restrictions.in(User::getUsername, usernames)));
        assertEquals(TOTAL_USERS - 2,
                userDao.count(Restrictions.in(User::getUsername, usernames).not()));
    }

    @Test
    public void testFilterByLikeAndNotLike() {
        assertEquals(TOTAL_USERS - 1, userDao.count(Restrictions.like(User::getEmail, "jpatest")));
        assertEquals(1L, userDao.count(Restrictions.notLike(User::getEmail, "00")
                .or(Restrictions.eq(User::getUsername, "Jack"))));
    }

    /** vip or (username in ('Scott','Lee') and email is not null) */
    @Test
    public void testNestedFilter() {
        List<User> users = userDao.query()
                .filter(Restrictions.eq(User::getVip, true)
                        .or(new FilterList().in(User::getUsername, List.of("Scott", "Lee")).and()
                                .notNull(User::getEmail)))
                .sort(JpaSort.asc(User::getUsername)).selectThis().list();
        assertEquals(4, users.size());
        assertEquals("Jack", users.get(0).getUsername());
    }

    /** The users who have ever ordered, which is an exists subquery. */
    @Test
    public void testExistsSubQuery() {
        JpaQuery<User, User> query = userDao.query();
        JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class)
                .filter(Restrictions.eq(Order::getUser, User::getId)).select(Order::getId);
        List<User> users = query.filter(Restrictions.exists(subQuery)).selectThis().list();
        long expected = orderDao.customQuery().select(new ColumnList(Order::getUser)).list().stream()
                .map(t -> t.get(0)).distinct().count();
        assertEquals(expected, users.size());
    }

    /** The users who have ever placed an order in the given status, which is an in subquery. */
    @ParameterizedTest
    @ValueSource(strings = {"PAID", "CANCELLED"})
    public void testInSubQuery(String status) {
        JpaQuery<User, User> query = userDao.query();
        JpaSubQuery<Order, Long> subQuery = query.subQuery(Order.class, "o", Long.class)
                .filter(Restrictions.eq(Order::getStatus, OrderStatus.valueOf(status)))
                .select(Property.forName("o", "user.id", Long.class));
        List<User> users = query.filter(Restrictions.in(User::getId, subQuery)).selectThis().list();
        users.forEach(user -> assertTrue(orderDao.exists(new FilterList()
                .eq(Order::getUser, user.getId()).and().eq(Order::getStatus,
                        OrderStatus.valueOf(status)))));
        assertTrue(users.size() > 0);
    }

    /** Join the orders and aggregate them by user. */
    @Test
    public void testJoinOrdersGroupByUser() {
        List<UserOrderVo> dataList = userDao.customQuery().leftJoin(User::getOrders, "o", null)
                .groupBy(new FieldList(User::getUsername))
                .sort(JpaSort.asc(User::getUsername))
                .select(new ColumnList().addColumns(User::getUsername)
                        .addColumns(Fields.count(Order::getId).as("orderAmount"),
                                Fields.sum(Order::getTotalPrice).as("totalPrice"),
                                Fields.max(Order::getTotalPrice).as("maxPrice")))
                .setTransformer(Transformers.asBean(UserOrderVo.class)).list();
        assertEquals(TOTAL_USERS, dataList.size());
        dataList.forEach(vo -> {
            Long userId = userDao.query(Long.class)
                    .filter(Restrictions.eq(User::getUsername, vo.getUsername()))
                    .select(new ColumnList(User::getId)).one();
            assertEquals(orderDao.count(Restrictions.eq(Order::getUser, userId)),
                    vo.getOrderAmount().longValue());
        });
    }

    /** The counting of a pagination has to agree with the rows it lists. */
    @Test
    public void testPaginationRowCount() throws Exception {
        JpaPageResultSet<Tuple> resultSet = userDao.customPage()
                .leftJoin(User::getOrders, "o", null)
                .filter(Restrictions.gt(Order::getTotalPrice, BigDecimal.valueOf(1000)))
                .select(new ColumnList().addColumns(User::getUsername)
                        .addColumns(Order::getId, Order::getTotalPrice));
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
    }

    /** The counting of a grouping pagination counts the groups rather than the rows. */
    @Test
    public void testGroupPaginationRowCount() throws Exception {
        JpaPageResultSet<Tuple> resultSet = userDao.customPage()
                .leftJoin(User::getOrders, "o", null).groupBy(new FieldList(User::getUsername))
                .having(Restrictions.gt(Fields.count(Order::getId), 0L))
                .select(new ColumnList().addColumns(User::getUsername)
                        .addColumns(Fields.count(Order::getId).as("orderAmount")));
        assertEquals(resultSet.list().size(), (int) resultSet.rowCount());
        assertTrue(resultSet.rowCount() <= TOTAL_USERS);
    }

    @Test
    public void testPageNavigation() {
        PageableQuery<Map<String, Object>> pageableQuery = userDao.customPage()
                .sort(JpaSort.asc(User::getId))
                .select(new ColumnList(User::getId, User::getUsername, User::getVip))
                .setTransformer(Transformers.asCaseInsensitiveMap());
        PageResponse<Map<String, Object>> pageResponse = pageableQuery.paginate(PageRequest.of(2));
        assertEquals(TOTAL_USERS, pageResponse.getTotalRecords());
        assertEquals(3, pageResponse.getTotalPages());
        assertTrue(pageResponse.isFirstPage());
        assertTrue(pageResponse.hasNextPage());
        assertEquals(2, pageResponse.getContent().size());
        assertEquals("Scott", pageResponse.nextPage().getContent().get(0).get("USERNAME"));
        assertEquals(1, pageResponse.lastPage().getContent().size());
        assertTrue(pageResponse.lastPage().isLastPage());
    }

    @Test
    public void testTransformers() {
        ColumnList columnList = new ColumnList(User::getUsername, User::getEmail);
        List<Map<String, Object>> maps =
                userDao.customQuery().filter(Restrictions.eq(User::getUsername, "Jack"))
                        .select(columnList).setTransformer(Transformers.asMap()).list();
        assertEquals("Jack", maps.get(0).get("username"));

        List<List<Object>> lists =
                userDao.customQuery().filter(Restrictions.eq(User::getUsername, "Jack"))
                        .select(columnList).setTransformer(Transformers.asList()).list();
        assertEquals("Jack", lists.get(0).get(0));

        List<User> users = userDao.query().filter(Restrictions.eq(User::getUsername, "Jack"))
                .selectThis().setTransformer(Transformers.noop()).list();
        assertEquals("Jack", users.get(0).getUsername());
    }

    @Test
    public void testUpdateByFilter() {
        int rows = userDao.update().set(User::getVip, true)
                .filter(Restrictions.eq(User::getVip, false)).execute();
        assertEquals(TOTAL_USERS - 2, rows);
        assertEquals(TOTAL_USERS, userDao.count(Restrictions.eq(User::getVip, true)));
    }

    @Test
    public void testUpdateMultipleAttributes() {
        int rows = userDao.update()
                .set(User::getPassword, "654321", User::getEmail, "nobody@jpatest.com")
                .filter(Restrictions.eq(User::getUsername, "Jack")).execute();
        assertEquals(1, rows);
        User user = userDao.query().filter(Restrictions.eq(User::getUsername, "Jack")).selectThis()
                .one();
        assertEquals("654321", user.getPassword());
        assertEquals("nobody@jpatest.com", user.getEmail());
    }

    @Test
    public void testUpdateThreeAttributes() {
        int rows = userDao.update()
                .set(User::getPassword, "111111", User::getEmail, "jack@jpatest.com",
                        User::getVip, false)
                .filter(Restrictions.eq(User::getUsername, "Jack")).execute();
        assertEquals(1, rows);
        assertEquals(1L, userDao.count(new FilterList().eq(User::getPassword, "111111").and()
                .eq(User::getEmail, "jack@jpatest.com").and().eq(User::getVip, false)));

        int byName = userDao.update().set("password", "222222", "email", "petter@jpatest.com")
                .filter(Restrictions.eq(User::getUsername, "Petter")).execute();
        assertEquals(1, byName);
        int byThree = userDao.update()
                .set("password", "333333", "email", "scott@jpatest.com", "username", "Scott2")
                .filter(Restrictions.eq(User::getUsername, "Scott")).execute();
        assertEquals(1, byThree);
    }

    /** Copy the value of an attribute to another one. */
    @Test
    public void testUpdateSetProperty() {
        int rows = userDao.update().setProperty("email", "username")
                .filter(Restrictions.eq(User::getUsername, "Terry")).execute();
        assertEquals(1, rows);
        assertEquals(1L, userDao.count(new FilterList().eq(User::getUsername, "Terry").and()
                .eq(User::getEmail, "Terry")));
    }

    /** Concat an expression into the attribute. */
    @Test
    public void testUpdateByExpression() {
        int rows = userDao.update()
                .setField(User::getUsername, Fields.concat(Fields.upper(User::getUsername), "!"))
                .filter(Restrictions.eq(User::getUsername, "Lee")).execute();
        assertEquals(1, rows);
        assertTrue(userDao.exists(Restrictions.eq(User::getUsername, "LEE!")));
    }

    @Test
    public void testDeleteUserWithoutOrder() {
        JpaDelete<User> delete = userDao.delete();
        JpaSubQuery<Order, Order> subQuery = delete.subQuery(Order.class)
                .filter(Restrictions.eq(Order::getUser, User::getId));
        int rows = delete.filter(Restrictions.exists(subQuery).not()).execute();
        log.info("Affected rows: {}", rows);
        assertEquals(TOTAL_USERS - userDao.count(), rows);
    }

    @Test
    public void testDeleteByFilter() {
        int rows = userDao.delete().filter(Restrictions.like(User::getUsername, "e")).execute();
        assertEquals(3, rows);
        assertEquals(TOTAL_USERS - 3, userDao.count());
    }

    @Test
    public void testNativeQueryForMap() {
        List<Map<String, Object>> dataList = userDao.queryForMap(
                "select u.username as username, count(o.id) as order_amount"
                        + " from example_user u left join example_order o on o.user_id=u.id"
                        + " group by u.username order by u.username",
                new Object[0]).list();
        assertEquals(TOTAL_USERS, dataList.size());
        assertEquals("Jack", dataList.get(0).get("USERNAME"));
    }

    @AfterAll
    public void end() {
        log.info("=========== UserDaoTests End. ===========");
    }

    @Getter
    @Setter
    @ToString
    public static class UserOrderVo {

        private String username;
        private Long orderAmount;
        private BigDecimal totalPrice;
        private BigDecimal maxPrice;

    }

}
