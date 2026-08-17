package com.github.easyjpa.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import com.github.easyjpa.ColumnList;
import com.github.easyjpa.FieldList;
import com.github.easyjpa.Fields;
import com.github.easyjpa.JpaPageResultSet;
import com.github.easyjpa.JpaSort;
import com.github.easyjpa.Restrictions;
import com.github.easyjpa.Transformers;
import com.github.easyjpa.page.EachPage;
import com.github.easyjpa.page.PageRequest;
import com.github.easyjpa.page.PageResponse;
import com.github.easyjpa.page.PageableQuery;
import com.github.easyjpa.test.dao.OrderDao;
import com.github.easyjpa.test.dao.ProductDao;
import com.github.easyjpa.test.entity.Order;
import com.github.easyjpa.test.entity.OrderProduct;
import com.github.easyjpa.test.entity.Product;
import com.github.easyjpa.test.service.ProductService;
import com.github.easyjpa.test.service.UserOrderService;
import com.github.easyjpa.test.service.UserService;
import jakarta.persistence.Tuple;

/**
 *
 * Tests of paginating the orders and the products.
 *
 * @Description: PaginationTests
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class PaginationTests extends AbstractDaoTests {

    private static final Logger log = LoggerFactory.getLogger(PaginationTests.class);

    private static final long TOTAL_PRODUCTS = 12L;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserOrderService userOrderService;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private OrderDao orderDao;

    @BeforeAll
    public void begin() {
        log.info("=========== PaginationTests Begin. ===========");
        userService.saveRandomUsers();
        productService.saveRandomProducts();
        userOrderService.makeRandomOrders();
    }

    /** Walk through the product catalog page by page. */
    @Test
    public void testNavigateProducts() {
        PageResponse<Map<String, Object>> pageResponse = productDao.customPage()
                .sort(JpaSort.asc(Product::getId))
                .select(new ColumnList(Product::getId, Product::getName, Product::getPrice))
                .setTransformer(Transformers.asMap()).paginate(PageRequest.of(5));
        assertEquals(TOTAL_PRODUCTS, pageResponse.getTotalRecords());
        assertEquals(3, pageResponse.getTotalPages());
        assertEquals(5, pageResponse.getPageSize());
        assertEquals(1, pageResponse.getPageNumber());
        assertEquals(0L, pageResponse.getOffset());
        assertTrue(pageResponse.isFirstPage());
        assertTrue(!pageResponse.isLastPage());
        assertTrue(pageResponse.hasNextPage());
        assertTrue(!pageResponse.hasPreviousPage());
        assertTrue(!pageResponse.isEmpty());

        assertEquals(5, pageResponse.nextPage().getContent().size());
        assertEquals(2, pageResponse.setPage(3).getContent().size());
        assertTrue(pageResponse.setPage(3).isLastPage());
        assertEquals(2, pageResponse.lastPage().getContent().size());
        assertEquals(1, pageResponse.firstPage().getPageNumber());
        assertEquals(1, pageResponse.setPage(2).previousPage().getPageNumber());
        assertTrue(pageResponse.setPage(2).hasPreviousPage());
    }

    /** Iterate every page, which is how a large result set gets consumed. */
    @Test
    public void testForEachPage() {
        List<Map<String, Object>> dataList = new ArrayList<>();
        List<Integer> pageNumbers = new ArrayList<>();
        productDao.customPage().sort(JpaSort.asc(Product::getId))
                .select(new ColumnList(Product::getId, Product::getName))
                .setTransformer(Transformers.asMap()).paginate(PageRequest.of(5))
                .forEachPage(eachPage -> {
                    pageNumbers.add(eachPage.getPageNumber());
                    dataList.addAll(eachPage.getContent());
                });
        assertEquals(List.of(1, 2, 3), pageNumbers);
        assertEquals(TOTAL_PRODUCTS, dataList.size());
    }

    /** Every page knows where it is. */
    @Test
    public void testEachPage() {
        List<EachPage<Map<String, Object>>> pages = new ArrayList<>();
        productDao.customPage().sort(JpaSort.asc(Product::getId))
                .select(new ColumnList(Product::getId))
                .setTransformer(Transformers.asMap()).paginate(PageRequest.of(5))
                .forEachPage(eachPage -> pages.add(eachPage));
        EachPage<Map<String, Object>> first = pages.get(0);
        assertTrue(first.isFirstPage());
        assertTrue(!first.isEmpty());
        assertTrue(first.hasNextPage());
        assertTrue(!first.hasPreviousPage());
        assertEquals(0L, first.getOffset());
        assertEquals(5, first.getPageSize());
        assertEquals(3, first.getTotalPages());
        assertEquals(TOTAL_PRODUCTS, first.getTotalRecords());

        EachPage<Map<String, Object>> last = pages.get(pages.size() - 1);
        assertTrue(last.isLastPage());
        assertTrue(last.hasPreviousPage());
        assertEquals(10L, last.getOffset());
        assertEquals(2, last.getContent().size());
    }

    /** A pagination can be adapted to the Page of Spring Data. */
    @Test
    public void testToPage() throws Exception {
        Page<Map<String, Object>> page = productDao.customPage()
                .sort(JpaSort.asc(Product::getId)).select(new ColumnList(Product::getId))
                .setTransformer(Transformers.asMap()).paginate(PageRequest.of(2, 5)).toPage();
        assertEquals(TOTAL_PRODUCTS, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertEquals(5, page.getSize());
        assertEquals(5, page.getContent().size());
        assertEquals(5, page.getNumberOfElements());
        assertTrue(page.hasContent());
        assertTrue(page.hasNext());
        assertTrue(page.hasPrevious());
        assertTrue(!page.isFirst());
        assertTrue(!page.isLast());
        assertEquals(5L, page.stream().count());
    }

    /** An empty result set makes an empty pagination. */
    @Test
    public void testEmptyPagination() throws Exception {
        PageResponse<Tuple> pageResponse = productDao.customPage()
                .filter(Restrictions.gt(Product::getPrice, BigDecimal.valueOf(100000)))
                .select(new ColumnList(Product::getId)).setTransformer(Transformers.noop())
                .paginate(PageRequest.of(10));
        assertEquals(0L, pageResponse.getTotalRecords());
        assertTrue(pageResponse.isEmpty());
        assertTrue(pageResponse.getContent().isEmpty());
    }

    /** Paginate the sales of every product, which is a grouping query upon two joins. */
    @Test
    public void testPaginateSalesPerProduct() throws Exception {
        JpaPageResultSet<Tuple> resultSet = orderDao.customPage()
                .join(Order::getOrderProducts, "op", null).join(OrderProduct::getProduct, "p", null)
                .groupBy(new FieldList().addFields(Product::getName))
                .having(Restrictions.gt(Fields.sum(OrderProduct::getAmount), 0))
                .sort(JpaSort.desc(2))
                .select(new ColumnList().addColumns(Product::getName)
                        .addColumns(Fields.sum(OrderProduct::getAmount).as("soldAmount"),
                                Fields.countDistinct(Fields.root()).as("orderAmount")));
        long groups = resultSet.rowCount();
        assertEquals(resultSet.list().size(), (int) groups);
        assertTrue(groups <= TOTAL_PRODUCTS);

        PageableQuery<Map<String, Object>> pageableQuery =
                resultSet.setTransformer(Transformers.asCaseInsensitiveMap());
        assertEquals(groups, pageableQuery.rowCount());
        pageableQuery.paginate(PageRequest.of(5)).forEachPage(eachPage -> {
            eachPage.getContent().forEach(vo -> log.info(vo.toString()));
        });
    }

    @AfterAll
    public void end() {
        log.info("=========== PaginationTests End. ===========");
    }

}
