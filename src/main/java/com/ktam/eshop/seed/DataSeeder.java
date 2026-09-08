package com.ktam.eshop.seed;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Seeds large volumes of test data via raw JDBC batches (NOT JPA
 * repository.save() in a loop — that would go through Hibernate's persistence
 * context / dirty checking per row and take forever at millions of rows).
 *
 * Disabled by default. Run with:
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--app.seed.enabled=true --app.seed.product-count=10000000"
 *
 * Or set env vars APP_SEED_ENABLED=true / APP_SEED_PRODUCT_COUNT=10000000.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String[] ADJECTIVES = {
            "Sleek", "Rugged", "Compact", "Wireless", "Portable", "Premium",
            "Budget", "Smart", "Classic", "Eco", "Heavy-Duty", "Ultra"
    };
    private static final String[] NOUNS = {
            "Phone", "Laptop", "Headphones", "Camera", "Speaker", "Monitor",
            "Keyboard", "Mouse", "Charger", "Backpack", "Watch", "Tablet"
    };

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.seed.enabled:false}")
    private boolean enabled;

    @Value("${app.seed.category-count:50}")
    private int categoryCount;

    @Value("${app.seed.product-count:1000000}")
    private int productCount;

    @Value("${app.seed.order-count:100000}")
    private int orderCount;

    @Value("${app.seed.batch-size:2000}")
    private int batchSize;

    public DataSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        long start = System.currentTimeMillis();
        System.out.printf("[seed] starting: %d categories, %d products, %d orders (batch=%d)%n",
                categoryCount, productCount, orderCount, batchSize);

        seedCategories();
        seedProducts();
        seedOrders();

        System.out.printf("[seed] done in %.1fs%n", (System.currentTimeMillis() - start) / 1000.0);
    }

    private void seedCategories() {
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Long.class) > 0) {
            System.out.println("[seed] categories already present, skipping");
            return;
        }
        for (int i = 1; i <= categoryCount; i++) {
            jdbcTemplate.update("INSERT INTO categories (name) VALUES (?)", "Category " + i);
        }
        System.out.println("[seed] categories inserted: " + categoryCount);
    }

    private void seedProducts() {
        long existing = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Long.class);
        if (existing >= productCount) {
            System.out.println("[seed] products already present (" + existing + "), skipping");
            return;
        }

        String sql = "INSERT INTO products (name, price, category_id) VALUES (?, ?, ?)";
        long toInsert = productCount - existing;
        int inserted = 0;

        while (inserted < toInsert) {
            int thisBatch = (int) Math.min(batchSize, toInsert - inserted);
            final int batchCount = thisBatch;

            jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    Random r = ThreadLocalRandom.current();
                    String name = ADJECTIVES[r.nextInt(ADJECTIVES.length)] + " "
                            + NOUNS[r.nextInt(NOUNS.length)] + " #" + r.nextInt(1_000_000);
                    double price = 1 + r.nextDouble() * 999; // 1.00 .. 1000.00
                    long categoryId = 1 + r.nextInt(categoryCount);

                    ps.setString(1, name);
                    ps.setDouble(2, Math.round(price * 100.0) / 100.0);
                    ps.setLong(3, categoryId);
                }

                @Override
                public int getBatchSize() {
                    return batchCount;
                }
            });

            inserted += thisBatch;
            if (inserted % 100_000 < batchSize) {
                System.out.println("[seed] products inserted: " + inserted + "/" + toInsert);
            }
        }
    }

    private void seedOrders() {
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class) > 0) {
            System.out.println("[seed] orders already present, skipping");
            return;
        }

        long productTotal = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Long.class);
        if (productTotal == 0) {
            System.out.println("[seed] no products yet, skipping order seeding");
            return;
        }

        String orderSql = "INSERT INTO orders (created_at) VALUES (?)";
        String itemSql = "INSERT INTO order_items (order_id, product_id, quantity) VALUES (?, ?, ?)";

        int inserted = 0;
        while (inserted < orderCount) {
            int thisBatch = Math.min(batchSize, orderCount - inserted);

            // Insert a batch of orders, then their line items, using
            // getGeneratedKeys per row would be slow one-by-one, so instead
            // insert orders individually here (order volume is much smaller
            // than product volume) and batch only the items.
            java.util.List<Object[]> itemRows = new java.util.ArrayList<>();
            for (int i = 0; i < thisBatch; i++) {
                Random r = ThreadLocalRandom.current();
                var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
                jdbcTemplate.update(con -> {
                    var ps = con.prepareStatement(orderSql, java.sql.Statement.RETURN_GENERATED_KEYS);
                    ps.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
                    return ps;
                }, keyHolder);
                long orderId = keyHolder.getKey().longValue();

                int lineItems = 1 + r.nextInt(5);
                for (int j = 0; j < lineItems; j++) {
                    long productId = 1 + r.nextInt((int) Math.min(productTotal, Integer.MAX_VALUE));
                    int quantity = 1 + r.nextInt(5);
                    itemRows.add(new Object[]{orderId, productId, quantity});
                }
            }

            jdbcTemplate.batchUpdate(itemSql, itemRows, itemRows.size(),
                    (ps, row) -> {
                        ps.setLong(1, (Long) row[0]);
                        ps.setLong(2, (Long) row[1]);
                        ps.setInt(3, (Integer) row[2]);
                    });

            inserted += thisBatch;
            if (inserted % 20_000 < batchSize) {
                System.out.println("[seed] orders inserted: " + inserted + "/" + orderCount);
            }
        }
    }
}
