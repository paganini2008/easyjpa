
package com.github.easyjpa;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * Look up the JPA provider on the classpath, falling back to whatever the Criteria API alone can
 * do. Every provider specific class is loaded by name, so a provider missing from the classpath
 * never breaks the others.
 *
 * @Description: JpaProviders
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public abstract class JpaProviders {

    private static final String[][] CANDIDATES =
            {{"org.hibernate.query.criteria.JpaCriteriaQuery",
                    "com.github.easyjpa.hibernate.HibernateProvider"},
                    {"org.eclipse.persistence.jpa.JpaEntityManager",
                            "com.github.easyjpa.eclipselink.EclipseLinkProvider"}};

    /**
     * Name the provider to use, either <code>standard</code> or the class name of an
     * implementation. An EntityDaoFactoryBean naming one overrides this, so it only settles the
     * cases where no dao is built at all.
     */
    public static final String PROVIDER_PROPERTY = "easyjpa.provider";

    private static volatile JpaProvider provider = lookup();

    public static JpaProvider getProvider() {
        return provider;
    }

    /**
     * Name the provider yourself, which is what an EntityDaoFactoryBean does. Naming one this way
     * is what settles it, since that is where the choice is written down.
     */
    public static void setProvider(JpaProvider theProvider) {
        if (theProvider != null) {
            provider = theProvider;
        }
    }

    /**
     * Hibernate first, since it is the one Spring Boot brings along, then EclipseLink, and the
     * plain Criteria API where neither is around.
     */
    private static JpaProvider lookup() {
        ClassLoader classLoader = JpaProviders.class.getClassLoader();
        String named = System.getProperty(PROVIDER_PROPERTY);
        if (StringUtils.isNotBlank(named)) {
            if ("standard".equalsIgnoreCase(named)) {
                return new StandardProvider();
            }
            try {
                return (JpaProvider) Class.forName(named, true, classLoader)
                        .getDeclaredConstructor().newInstance();
            } catch (Throwable e) {
                throw new IllegalArgumentException("Unknown jpa provider: " + named, e);
            }
        }
        for (String[] candidate : CANDIDATES) {
            try {
                Class.forName(candidate[0], false, classLoader);
                return (JpaProvider) Class.forName(candidate[1], true, classLoader)
                        .getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
            }
        }
        return new StandardProvider();
    }

}
