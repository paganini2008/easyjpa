package com.github.easyjpa.test.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;
import com.github.easyjpa.eclipselink.EclipseLinkEntityDaoFactoryBean;
import jakarta.persistence.EntityManagerFactory;

/**
 *
 * Spring Boot autoconfigures Hibernate alone, so the entity manager of EclipseLink is put together
 * here.
 *
 * @Description: EclipseLinkJpaConfig
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
@Profile("eclipselink")
@EnableTransactionManagement
@EnableJpaRepositories(repositoryFactoryBeanClass = EclipseLinkEntityDaoFactoryBean.class,
        basePackages = {"com.github.easyjpa.test.dao"})
@Configuration(proxyBeanMethods = false)
public class EclipseLinkJpaConfig {

    /** Spring Boot hands this to the entity manager it makes itself, this one is made here. */
    @Value("${spring.jpa.mapping-resources:}")
    private String mappingResources;

    /** EclipseLink ships no platform for every database, so one can be named per profile. */
    @Value("${eclipselink.target-database:}")
    private String targetDatabase;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factoryBean =
                new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("com.github.easyjpa.test.entity");
        factoryBean.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        if (StringUtils.hasText(mappingResources)) {
            factoryBean.setMappingResources(mappingResources.split(","));
        }

        Properties properties = new Properties();
        properties.setProperty("eclipselink.weaving", "false");
        properties.setProperty("eclipselink.ddl-generation", "drop-and-create-tables");
        properties.setProperty("eclipselink.ddl-generation.output-mode", "database");
        properties.setProperty("eclipselink.logging.level", "FINE");
        if (StringUtils.hasText(targetDatabase)) {
            properties.setProperty("eclipselink.target-database", targetDatabase);
        }
        factoryBean.setJpaProperties(properties);
        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }

    /**
     * EclipseLink drops and creates the tables on startup and leaves them behind on shutdown, so
     * the ones it made are dropped here instead.
     */
    @Bean
    public DisposableBean schemaCleaner(DataSource dataSource) {
        return () -> {
            String[] tables = {"example_order_product", "example_order", "example_stock",
                    "example_product", "example_user", "SEQUENCE"};
            try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()) {
                for (String table : tables) {
                    try {
                        statement.execute("drop table " + table);
                    } catch (SQLException ignored) {
                    }
                }
            } catch (SQLException e) {
                LoggerFactory.getLogger(EclipseLinkJpaConfig.class)
                        .warn("Left the tables behind: {}", e.getMessage());
            }
        };
    }

}
