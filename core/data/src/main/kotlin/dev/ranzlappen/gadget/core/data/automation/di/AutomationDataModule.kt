package dev.ranzlappen.gadget.core.data.automation.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.data.automation.AutomationDatabase
import dev.ranzlappen.gadget.core.data.automation.RoomRuleRepository
import dev.ranzlappen.gadget.core.data.automation.RuleDao
import javax.inject.Singleton

// Repo convention (see CLAUDE.md "Companion-object @Provides" pitfall):
// a top-level `object` module for @Provides + a separate abstract class
// for @Binds — never @Provides on a companion of an abstract @Module.

@Module
@InstallIn(SingletonComponent::class)
object AutomationDataModule {

    @Provides
    @Singleton
    fun provideAutomationDatabase(
        @ApplicationContext context: Context,
    ): AutomationDatabase = Room.databaseBuilder(
        context,
        AutomationDatabase::class.java,
        "automation.db",
    ).build()

    @Provides
    fun provideRuleDao(database: AutomationDatabase): RuleDao = database.ruleDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AutomationDataBindsModule {

    @Binds
    @Singleton
    abstract fun bindRuleRepository(impl: RoomRuleRepository): RuleRepository
}
