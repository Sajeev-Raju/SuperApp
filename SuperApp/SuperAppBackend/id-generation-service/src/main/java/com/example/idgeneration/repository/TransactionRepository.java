package com.example.idgeneration.repository;

import com.example.idgeneration.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Transaction> transactionRowMapper = (ResultSet rs, int rowNum) -> {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getLong("id"));
        transaction.setEmail(rs.getString("email"));
        transaction.setPhone(rs.getString("phone"));
        transaction.setUsername(rs.getString("username"));
        transaction.setFancy(rs.getBoolean("is_fancy"));
        transaction.setFancyType(rs.getString("fancy_type"));
        transaction.setBasePrice(rs.getBigDecimal("base_price"));
        transaction.setFancyPrice(rs.getBigDecimal("fancy_price"));
        transaction.setTotalPrice(rs.getBigDecimal("total_price"));
        transaction.setPaymentId(rs.getString("payment_id"));
        transaction.setPaymentStatus(rs.getString("payment_status"));
        transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        transaction.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return transaction;
    };

    public Transaction save(Transaction transaction) {
        if (transaction.getId() == null) {
            // Insert new transaction
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO transactions (email, phone, username, is_fancy, fancy_type, " +
                                "base_price, fancy_price, total_price, payment_id, payment_status) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, transaction.getEmail());
                ps.setString(2, transaction.getPhone());
                ps.setString(3, transaction.getUsername());
                ps.setBoolean(4, transaction.isFancy());
                ps.setString(5, transaction.getFancyType());
                ps.setBigDecimal(6, transaction.getBasePrice());
                ps.setBigDecimal(7, transaction.getFancyPrice());
                ps.setBigDecimal(8, transaction.getTotalPrice());
                ps.setString(9, transaction.getPaymentId());
                ps.setString(10, transaction.getPaymentStatus());
                return ps;
            }, keyHolder);
            
            transaction.setId(keyHolder.getKey().longValue());
            return transaction;
        } else {
            // Update existing transaction
            jdbcTemplate.update(
                    "UPDATE transactions SET payment_id = ?, payment_status = ?, updated_at = ? WHERE id = ?",
                    transaction.getPaymentId(),
                    transaction.getPaymentStatus(),
                    LocalDateTime.now(),
                    transaction.getId()
            );
            return transaction;
        }
    }

    public Optional<Transaction> findById(Long id) {
        try {
            Transaction transaction = jdbcTemplate.queryForObject(
                    "SELECT * FROM transactions WHERE id = ?",
                    transactionRowMapper,
                    id
            );
            return Optional.ofNullable(transaction);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Transaction> findByPaymentId(String paymentId) {
        try {
            Transaction transaction = jdbcTemplate.queryForObject(
                    "SELECT * FROM transactions WHERE payment_id = ?",
                    transactionRowMapper,
                    paymentId
            );
            return Optional.ofNullable(transaction);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void updatePaymentStatus(Long id, String paymentId, String status) {
        jdbcTemplate.update(
                "UPDATE transactions SET payment_id = ?, payment_status = ?, updated_at = ? WHERE id = ?",
                paymentId,
                status,
                LocalDateTime.now(),
                id
        );
    }
}