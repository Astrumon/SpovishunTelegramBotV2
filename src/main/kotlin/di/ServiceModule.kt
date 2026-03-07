package com.ua.astrumon.di

import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.domain.service.GroupService
import com.ua.astrumon.domain.service.MemberService
import org.koin.dsl.module

val serviceModule = module {
    single { MemberService(get()) }
    single { GroupService(get(), get()) }
    single { AutoRegisterService(get()) }
}
