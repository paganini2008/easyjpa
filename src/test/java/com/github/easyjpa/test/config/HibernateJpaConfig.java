package com.github.easyjpa.test.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.github.easyjpa.hibernate.HibernateEntityDaoFactoryBean;

/**
 * 
 * Hibernate Jpa Configuration Bean
 * 
 * @Author: Fred Feng
 * @Date: 18/03/2025
 * @Version 1.0.0
 */
@Profile("!eclipselink & !standard")
@EntityScan(basePackages = {"com.github.easyjpa.test.entity"})
@EnableJpaRepositories(repositoryFactoryBeanClass = HibernateEntityDaoFactoryBean.class,
        basePackages = {"com.github.easyjpa.test.dao"})
@Configuration(proxyBeanMethods = false)
public class HibernateJpaConfig {

}
