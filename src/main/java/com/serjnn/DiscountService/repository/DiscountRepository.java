package com.serjnn.DiscountService.repository;

import com.serjnn.DiscountService.model.DiscountEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DiscountRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<DiscountEntity> findAll() {
        String sql = "SELECT * FROM discount_entity";
        return jdbcTemplate.query(sql, new DataClassRowMapper<>(DiscountEntity.class));
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

    public void update(DiscountEntity discountEntity) {
        String sql = "UPDATE discount_entity SET discount = ? WHERE product_id = ?";
        jdbcTemplate.update(sql, discountEntity.discount(), discountEntity.productId());
    }
}
