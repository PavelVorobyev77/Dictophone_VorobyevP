package com.example.dictophone_vorobyevp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AudioRecordDao {
    @Query("SELECT * FROM audioRecords")
    fun getAll():List<AudioRecord>

    @Query("SELECT * FROM audioRecords ORDER BY filename ASC") // Сортировка по алфавиту (по возрастанию)
    fun getAllSortedByAlphabet(): List<AudioRecord>

    @Query("SELECT * FROM audioRecords ORDER BY duration ASC") // Сортировка по продолжительности (по возрастанию)
    fun getAllSortedByDuration(): List<AudioRecord>

    @Query("SELECT * FROM audioRecords WHERE filename LIKE :query")
    fun searchDatabase(query: String):List<AudioRecord>

    @Insert
    fun insert(vararg audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecords: Array<AudioRecord>)

    @Update
    fun update(audioRecord: AudioRecord)
}