package com.ua.astrumon.di

import com.ua.astrumon.data.memory.repository.GroupMemberRepositoryMockImpl
import com.ua.astrumon.data.memory.repository.GroupRepositoryMockImpl
import com.ua.astrumon.data.memory.repository.MemberRepositoryMockImpl
import com.ua.astrumon.domain.repository.GroupMemberRepository
import com.ua.astrumon.domain.repository.GroupRepository
import com.ua.astrumon.domain.repository.MemberRepository
import org.koin.dsl.module

val devRepositoryModule = module {
    single<MemberRepository> { MemberRepositoryMockImpl() }
    single<GroupRepository> { GroupRepositoryMockImpl() }
    single<GroupMemberRepository> { GroupMemberRepositoryMockImpl() }
}
