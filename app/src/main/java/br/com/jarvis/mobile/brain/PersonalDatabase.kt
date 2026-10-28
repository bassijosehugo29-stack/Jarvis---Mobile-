package br.com.jarvis.mobile.brain

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** No personal text is stored in searchable columns or plaintext FTS indexes. */
@Entity(tableName = "personal_records", primaryKeys = ["kind", "id"])
data class PersonalRecord(val kind: String, val id: String, val updatedAt: Long, val payload: ByteArray)

@Dao
interface PersonalDao {
    @Query("SELECT * FROM personal_records WHERE kind = :kind ORDER BY updatedAt DESC")
    suspend fun records(kind: String): List<PersonalRecord>
    @Query("SELECT COUNT(*) FROM personal_records WHERE kind = :kind")
    suspend fun count(kind: String): Int
    @Query("SELECT * FROM personal_records WHERE kind = :kind")
    fun observe(kind: String): Flow<List<PersonalRecord>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(record: PersonalRecord)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAll(records: List<PersonalRecord>)
    @Query("DELETE FROM personal_records WHERE kind = :kind AND id = :id") suspend fun delete(kind: String, id: String)
    @Query("DELETE FROM personal_records WHERE kind = :kind") suspend fun clearKind(kind: String)
    @Query("DELETE FROM personal_records") suspend fun clear()
}

@Database(entities = [PersonalRecord::class], version = 1, exportSchema = false)
abstract class PersonalDatabase : RoomDatabase() {
    abstract fun records(): PersonalDao
    companion object {
        @Volatile private var instance: PersonalDatabase? = null
        fun get(context: Context): PersonalDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, PersonalDatabase::class.java, "jarvis-personal.db")
                .build().also { instance = it }
        }
    }
}
