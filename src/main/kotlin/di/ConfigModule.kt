package com.ua.astrumon.di

import com.ua.astrumon.config.AppConfig
import io.github.cdimascio.dotenv.dotenv
import org.koin.dsl.module

val configModule = module {
    single { AppConfig() }
}
