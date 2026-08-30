package com.serjnn.discountservice.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public void save(OutboxEvent event) {
        String sql = """
                INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload, status, retry_count, created_at)
                VALUES (?, ?, ?, ?, 'PENDING', 0, NOW())
                """;
        jdbcTemplate.update(sql, event.aggregateType(), event.aggregateId(), event.eventType(), event.payload());
    }

    public void saveBatch(List<OutboxEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO outbox_events (aggregate_type, aggregate_id, event_type, payload, status, retry_count, created_at)
                VALUES (?, ?, ?, ?, 'PENDING', 0, NOW())
                """;
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                OutboxEvent event = events.get(i);
                ps.setString(1, event.aggregateType());
                ps.setString(2, event.aggregateId());
                ps.setString(3, event.eventType());
                ps.setString(4, event.payload());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        });
    }

    public List<OutboxEvent> fetchAndLockPendingEvents(int limit) {
        String sql = """
                SELECT id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, error_message, created_at, processed_at
                FROM outbox_events
                WHERE status = 'PENDING'
                ORDER BY created_at ASC
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """;
        return jdbcTemplate.query(sql, new DataClassRowMapper<>(OutboxEvent.class), limit);
    }

    public void markAsProcessed(Long id) {
        String sql = "UPDATE outbox_events SET status = 'PROCESSED', processed_at = NOW() WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void markBatchAsProcessed(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String sql = "UPDATE outbox_events SET status = 'PROCESSED', processed_at = NOW() WHERE id = ?";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, ids.get(i));
            }

            @Override
            public int getBatchSize() {
                return ids.size();
            }
        });
    }

    public void markAsFailed(Long id, String errorMessage, int maxRetries) {
        String sql = """
                UPDATE outbox_events
                SET retry_count = retry_count + 1,
                    error_message = ?,
                    status = CASE WHEN retry_count + 1 >= ? THEN 'FAILED' ELSE 'PENDING' END
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, errorMessage, maxRetries, id);
    }
}
