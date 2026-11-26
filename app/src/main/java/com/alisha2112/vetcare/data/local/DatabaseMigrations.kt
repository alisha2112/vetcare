
package com.alisha2112.vetcare.data.local
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {

        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE observations " +
                        "ADD COLUMN latitude REAL"
            )

            db.execSQL(
                "ALTER TABLE observations " +
                        "ADD COLUMN longitude REAL"
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {

        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE observations " +
                        "ADD COLUMN photo_path TEXT"
            )
        }
    }
}
