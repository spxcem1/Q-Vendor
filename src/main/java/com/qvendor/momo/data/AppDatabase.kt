package com.qvendor.momo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.TypeConverter
import androidx.sqlite.db.SupportSQLiteDatabase

class TransactionStatusConverter {
    @TypeConverter
    fun fromStatus(value: TransactionStatus?): String? = value?.name

    @TypeConverter
    fun toStatus(value: String?): TransactionStatus? =
        value?.let { runCatching { TransactionStatus.valueOf(it) }.getOrNull() }
}

@Database(
    entities = [VendorProfile::class, Transaction::class],
    version = 7,
    exportSchema = false
)
@androidx.room.TypeConverters(TransactionStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun profileDao(): VendorProfileDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vendor_profiles ADD COLUMN ussdStepsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vendor_profiles ADD COLUMN actionsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE vendor_profiles SET ispName = 'TELECEL' WHERE ispName = 'VODAFONE'")
                db.execSQL("DELETE FROM vendor_profiles WHERE ispName = 'ZEEPAY'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vendor_profiles ADD COLUMN defaultAmount TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vendor_profiles ADD COLUMN defaultPhone TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vendor_profiles ADD COLUMN defaultReference TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS transactions (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "profileId INTEGER NOT NULL, " +
                            "actionName TEXT NOT NULL, " +
                            "ispName TEXT NOT NULL, " +
                            "phone TEXT NOT NULL, " +
                            "amount REAL NOT NULL, " +
                            "status TEXT NOT NULL, " +
                            "createdAt INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qvendor_db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
