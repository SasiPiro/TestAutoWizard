package com.autowizard;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records every SQL statement Hibernate prepares, for tests that check what a request
 * actually reads. Enable with
 * {@code spring.jpa.properties.hibernate.session_factory.statement_inspector=com.autowizard.SqlCapture}.
 */
public class SqlCapture implements StatementInspector {

    private static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
        STATEMENTS.add(sql);
        return sql;
    }

    public static void clear() {
        STATEMENTS.clear();
    }

    public static List<String> statements() {
        return List.copyOf(STATEMENTS);
    }
}
