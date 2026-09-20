package com.github.easyjpa.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.easyjpa.ConvertUtils;
import com.github.easyjpa.JpaProvider;
import com.github.easyjpa.JpaProviders;
import com.github.easyjpa.LambdaUtils;
import com.github.easyjpa.LambdaUtils.LambdaInfo;
import com.github.easyjpa.PropertyUtils;
import com.github.easyjpa.TableAlias;
import com.github.easyjpa.page.PageRequest;
import com.github.easyjpa.test.entity.Product;
import com.github.easyjpa.test.entity.User;

/**
 *
 * Unit tests of the utilities, which need no database.
 *
 * @Description: UtilsTests
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class UtilsTests {

    private static final Logger log = LoggerFactory.getLogger(UtilsTests.class);

    @AfterEach
    public void clearTableAlias() {
        TableAlias.clear();
    }

    /** The provider is the one the EntityDaoFactoryBean of the active configuration names. */
    @Test
    public void testProviderLookup() {
        JpaProvider provider = JpaProviders.getProvider();
        assertTrue(List.of("Hibernate", "EclipseLink", "Jakarta Persistence")
                .contains(provider.getName()));
        // Only Hibernate reaches beyond the Criteria API here
        assertEquals("Hibernate".equals(provider.getName()), provider.supportsDerivedTable());
        // The same instance is handed out every time
        assertTrue(provider == JpaProviders.getProvider());
    }

    /** Every capability the provider claims is what the tests skip themselves by. */
    @Test
    public void testProviderCapabilities() {
        JpaProvider provider = JpaProviders.getProvider();
        log.info("Provider: {}, derived table: {}, date part: {}, right join: {},"
                + " subquery as expression: {}, subquery as selection: {}, partial entity: {},"
                + " bean projection: {}, concatenated group key: {}", provider.getName(),
                provider.supportsDerivedTable(), provider.supportsDatePart(),
                provider.supportsRightJoin(), provider.supportsSubQueryAsExpression(),
                provider.supportsSubQueryAsSelection(), provider.supportsPartialEntity(),
                provider.supportsBeanProjection(), provider.supportsConcatenatedGroupKey());
        if ("EclipseLink".equals(provider.getName())) {
            assertTrue(!provider.supportsDerivedTable());
            assertTrue(!provider.supportsRightJoin());
            assertTrue(!provider.supportsSubQueryAsExpression());
            assertTrue(!provider.supportsPartialEntity());
        } else if ("Hibernate".equals(provider.getName())) {
            assertTrue(provider.supportsDerivedTable());
            assertTrue(provider.supportsRightJoin());
            assertTrue(provider.supportsSubQueryAsExpression());
            assertTrue(provider.supportsPartialEntity());
        }
    }

    @Test
    public void testConvertValue() {
        assertEquals(Integer.valueOf(100), ConvertUtils.convertValue("100", Integer.class));
        assertEquals(Long.valueOf(100L), ConvertUtils.convertValue(BigDecimal.TEN.multiply(
                BigDecimal.TEN), Long.class));
        assertEquals("100", ConvertUtils.convertValue(100, String.class));
        assertNull(ConvertUtils.convertValue(null, String.class));
        assertThrows(UnsupportedOperationException.class,
                () -> ConvertUtils.convertValue(new Object(), Product.class));
    }

    @Test
    public void testInspectLambda() {
        LambdaInfo info = LambdaUtils.inspect(Product::getName);
        assertEquals("name", info.getAttributeName());
        assertEquals(String.class, info.getAttributeType());
        assertEquals(Product.class.getName(), info.getClassName());

        // A boolean attribute is read by 'isXxx' rather than by 'getXxx'
        assertEquals("vip", LambdaUtils.inspect(User::getVip).getAttributeName());
    }

    @Test
    public void testTableAlias() {
        TableAlias.put(Product.class, "p");
        assertEquals("p", TableAlias.get(Product.class));
        assertEquals("p", TableAlias.get(Product.class.getName()));
        assertNull(TableAlias.get(User.class));
        assertEquals(1, TableAlias.copy().size());

        TableAlias.clear();
        assertNull(TableAlias.get(Product.class));
    }

    @Test
    public void testBeanProperty() throws Exception {
        Product product = new Product();
        PropertyUtils.setProperty(product, "name", "Juicer");
        PropertyUtils.setProperty(product, "price", BigDecimal.TEN);
        assertEquals("Juicer", PropertyUtils.getProperty(product, "name"));
        assertEquals(BigDecimal.TEN, PropertyUtils.getProperty(product, "price"));
    }

    @Test
    public void testPageRequest() {
        PageRequest pageRequest = PageRequest.of(3, 10);
        assertEquals(3, pageRequest.getPageNumber());
        assertEquals(10, pageRequest.getPageSize());
        assertEquals(20L, pageRequest.getOffset());
        assertTrue(pageRequest.hasPrevious());
        assertEquals(4, pageRequest.next().getPageNumber());
        assertEquals(2, pageRequest.previous().getPageNumber());
        assertEquals(1, pageRequest.first().getPageNumber());
        assertEquals(5, pageRequest.withPage(5).getPageNumber());
        assertEquals(1, PageRequest.of(10).getPageNumber());
        // The first page has no previous one
        assertEquals(1, PageRequest.of(1, 10).previous().getPageNumber());
        assertThrows(UnsupportedOperationException.class, () -> pageRequest.getSort());
    }

}
