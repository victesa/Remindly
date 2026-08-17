package com.victorkirui.local.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.victorkirui.local.RemindlyDatabase
import com.victorkirui.local.repository.LocalRepository
import com.victorkirui.local.repository.LocalRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val localModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            RemindlyDatabase::class.java,
            "remindly_db"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        })
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }
    single { get<RemindlyDatabase>().itemDao() }
    single { get<RemindlyDatabase>().pendingSyncDao() }
    single { get<RemindlyDatabase>().reminderDao() }
    single<LocalRepository> { LocalRepositoryImpl(get(), get(), get(), get()) }
}
