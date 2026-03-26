package com.faarigh.app.di

import com.faarigh.app.module.ModuleRegistry
import com.faarigh.app.module.modules.AppInterceptionModule
import com.faarigh.app.module.modules.DnsFilterModule
import com.faarigh.app.module.modules.NsfwDetectionModule
import com.faarigh.app.module.modules.ShortsBlockerModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ModuleDiModule {

    /**
     * Eagerly registers all wellbeing modules with the central registry.
     * Adding a new module = add its constructor param here + register it.
     */
    @Provides
    @Singleton
    fun providePopulatedRegistry(
        registry: ModuleRegistry,
        appInterception: AppInterceptionModule,
        nsfwDetection: NsfwDetectionModule,
        shortsBlocker: ShortsBlockerModule,
        dnsFilter: DnsFilterModule,
    ): PopulatedModuleRegistry {
        registry.register(appInterception)
        registry.register(nsfwDetection)
        registry.register(shortsBlocker)
        registry.register(dnsFilter)
        return PopulatedModuleRegistry(registry)
    }
}

/**
 * Wrapper that signals the registry has been fully populated.
 * Inject this instead of raw ModuleRegistry when you need all modules available.
 * Scoped as @Singleton via the @Provides method in ModuleDiModule.
 */
class PopulatedModuleRegistry(val registry: ModuleRegistry)
