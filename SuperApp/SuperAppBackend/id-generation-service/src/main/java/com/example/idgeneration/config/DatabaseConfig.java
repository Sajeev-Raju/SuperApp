package com.example.idgeneration.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Bean
    public void initializeDatabase() {
        // Create tables if they don't exist
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "email TEXT NOT NULL," +
                "phone TEXT NOT NULL," +
                "username TEXT NOT NULL," +
                "is_fancy BOOLEAN DEFAULT 0," +
                "fancy_type TEXT," +
                "base_price DECIMAL(10,2) NOT NULL," +
                "fancy_price DECIMAL(10,2) DEFAULT 0," +
                "total_price DECIMAL(10,2) NOT NULL," +
                "payment_id TEXT," +
                "payment_status TEXT DEFAULT 'PENDING'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS usernames (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL UNIQUE," +
                "is_assigned BOOLEAN DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
    }
}