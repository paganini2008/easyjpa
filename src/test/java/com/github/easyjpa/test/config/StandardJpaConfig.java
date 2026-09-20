package com.github.easyjpa.test.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.github.easyjpa.StandardEntityDaoFactoryBean;

/**
 * 
 * The entity manager stays the one Spring Boot autoconfigures, while EasyJPA keeps to the Criteria
 * API alone. Activate it by the profile <code>standard</code>.
 * 
 * @Description: StandardJpaConfig
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
@Profile("standard")
@EntityScan(basePackages = {"com.github.easyjpa.test.entity"})
@EnableJpaRepositories(repositoryFactoryBeanClass = StandardEntityDaoFactoryBean.class,
        basePackages = {"com.github.easyjpa.test.dao"})
@Configuration(proxyBeanMethods = false)
public class StandardJpaConfig {

}
