package com.freshkeeper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProductEntity::class], version = 1)
abstract class FreshKeeperDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
