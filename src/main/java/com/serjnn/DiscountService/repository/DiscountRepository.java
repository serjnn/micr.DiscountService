package com.serjnn.DiscountService.repository;

import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.model.DiscountEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DiscountRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<DiscountEntity> findAll() {
        String sql = "SELECT * FROM discount_entity ORDER BY product_id ASC";
        return jdbcTemplate.query(sql, new DataClassRowMapper<>(DiscountEntity.class));
    }

    public List<DiscountEntity> findAll(int page, int size) {
        int offset = Math.max(0, page) * Math.max(1, size);
        String sql = "SELECT * FROM discount_entity ORDER BY product_id ASC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new DataClassRowMapper<>(DiscountEntity.class), size, offset);
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM discount_entity";
        Long total = jdbcTemplate.queryForObject(sql, Long.class);
        return total != null ? total : 0L;
    }

    public void save(DiscountEntity discountEntity) {
        String sql = "INSERT INTO discount_entity (product_id, discount) VALUES (?, ?)";
        jdbcTemplate.update(sql, discountEntity.productId(), discountEntity.discount());
    }

    public Optional<DiscountEntity> findByProductId(long productId) {
        String sql = "SELECT * FROM discount_entity WHERE product_id = ?";
        return jdbcTemplate.query(sql, new DataClassRowMapper<>(DiscountEntity.class), productId)
                .stream()
                .findFirst();
    }

    public List<DiscountEntity> findByProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT * FROM discount_entity WHERE product_id IN (:productIds)";
        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource("productIds", productIds),
                new DataClassRowMapper<>(DiscountEntity.class)
        );
    }

    public void update(DiscountEntity discountEntity) {
        String sql = "UPDATE discount_entity SET discount = ? WHERE product_id = ?";
        jdbcTemplate.update(sql, discountEntity.discount(), discountEntity.productId());
    }

    public void upsert(long productId, BigDecimal discount) {
        String sql = """
                INSERT INTO discount_entity (product_id, discount)
                VALUES (?, ?)
                ON CONFLICT (product_id) DO UPDATE SET discount = EXCLUDED.discount
                """;
        jdbcTemplate.update(sql, productId, discount);
    }

    public void batchUpsert(List<DiscountRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO discount_entity (product_id, discount)
                VALUES (?, ?)
                ON CONFLICT (product_id) DO UPDATE SET discount = EXCLUDED.discount
                """;
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                DiscountRequest request = requests.get(i);
                ps.setLong(1, request.productId());
                ps.setBigDecimal(2, request.discount());
            }

            @Override
            public int getBatchSize() {
                return requests.size();
            }
        });
    }

    public int deleteByProductId(long productId) {
        String sql = "DELETE FROM discount_entity WHERE product_id = ?";
        return jdbcTemplate.update(sql, productId);
    }
}

