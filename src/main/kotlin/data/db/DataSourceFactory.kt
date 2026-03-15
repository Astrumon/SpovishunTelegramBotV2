package com.ua.astrumon.data.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object DataSourceFactory {
    fun create(
        url: String,
        driver: String,
        username: String = "",
        password: String = "",
        poolSize: Int = 10
    ): HikariDataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = url
        driverClassName = driver
        this.username = username
        this.password = password
        maximumPoolSize = poolSize
        minimumIdle = if (poolSize > 2) 5 else 1
        idleTimeout = 30000
        connectionTimeout = 30000
        maxLifetime = 1800000
    })
}