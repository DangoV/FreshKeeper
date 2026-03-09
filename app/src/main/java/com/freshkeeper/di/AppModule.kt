package com.freshkeeper.di

import android.content.Context
import androidx.room.Room
import com.freshkeeper.data.DefaultProductRepository
import com.freshkeeper.data.ProductRepository
import com.freshkeeper.data.local.FreshKeeperDatabase
import com.freshkeeper.data.local.ProductDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FreshKeeperDatabase {
        return Room.databaseBuilder(
            context,
            FreshKeeperDatabase::class.java,
            "fresh_keeper.db",
        ).build()
    }

    @Provides
    fun provideProductDao(db: FreshKeeperDatabase): ProductDao = db.productDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindProductRepository(impl: DefaultProductRepository): ProductRepository
}
