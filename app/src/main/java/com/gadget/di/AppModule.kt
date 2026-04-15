package com.gadget.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WidgetSettingsPrefs

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LinkRulesPrefs

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @WidgetSettingsPrefs
    fun provideWidgetSettingsPrefs(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @LinkRulesPrefs
    fun provideLinkRulesPrefs(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("link_rules", Context.MODE_PRIVATE)
    }
}
