package com.github.easyjpa.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.TestInstance;
import com.github.easyjpa.test.config.EclipseLinkJpaConfig;
import com.github.easyjpa.test.config.HibernateJpaConfig;
import com.github.easyjpa.test.config.StandardJpaConfig;
import com.github.easyjpa.test.config.TestApplication;
import com.github.easyjpa.test.service.ProductService;
import com.github.easyjpa.test.service.UserOrderService;
import com.github.easyjpa.test.service.UserService;

/**
 * 
 * One and the same set of tests, whichever provider stands behind them: the JPA configuration is
 * picked by the active profile, everything else stays put.
 * 
 * @Description: AbstractDaoTests
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = {TestApplication.class, HibernateJpaConfig.class, StandardJpaConfig.class,
        EclipseLinkJpaConfig.class,
        UserService.class, ProductService.class, UserOrderService.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public abstract class AbstractDaoTests {

}
