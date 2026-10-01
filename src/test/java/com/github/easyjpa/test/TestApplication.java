package com.github.easyjpa.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 
 * The application the tests are booted with, so that the profiles and the property files of Spring
 * Boot all work as they do in a real one. It sits above the entities and the daos on purpose: they
 * are found from here, which keeps the tests off @EntityScan, a class Spring Boot 4 moved to
 * another package.
 * 
 * @Description: TestApplication
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
@SpringBootApplication
public class TestApplication {

}
