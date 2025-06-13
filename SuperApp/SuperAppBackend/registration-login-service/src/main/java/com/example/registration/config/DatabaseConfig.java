package com.example.registration.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@Configuration
@EnableScheduling
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Bean
    public void initializeDatabase() {
        try {
            logger.info("Initializing database tables...");
            
            // Create users table
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "email TEXT NOT NULL UNIQUE," +
                    "phone TEXT NOT NULL UNIQUE," +
                    "username TEXT UNIQUE," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "status TEXT DEFAULT 'PENDING'" +
                    ")");
            logger.info("Users table initialized successfully");

            // Create otps table
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS otps (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "email TEXT," +
                    "phone TEXT," +
                    "email_otp TEXT," +
                    "phone_otp TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "expires_at TIMESTAMP," +
                    "verified BOOLEAN DEFAULT 0" +
                    ")");
            logger.info("OTPs table initialized successfully");

            // Create login_sessions table
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS login_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL," +
                    "phone TEXT NOT NULL," +
                    "otp TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "expires_at TIMESTAMP," +
                    "verified BOOLEAN DEFAULT 0" +
                    ")");
            logger.info("Login sessions table initialized successfully");

            // Create user_sessions table for session management
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS user_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL," +
                    "session_id TEXT NOT NULL UNIQUE," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "expires_at TIMESTAMP NOT NULL" +
                    ")");
            logger.info("User sessions table initialized successfully");
            
        } catch (Exception e) {
            logger.error("Error initializing database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database: " + e.getMessage());
        }
    }
}

