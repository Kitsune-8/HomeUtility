package com.example.homeutility;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

import java.util.List;

@Dao
public interface UtilityDao {

    // Вставка новой записи
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UtilityRecord record);

    // Обновление существующей записи
    @Update
    void update(UtilityRecord record);

    // Получение записи по конкретному месяцу для пользователя
    @Query("SELECT * FROM utility_records WHERE year_month = :yearMonth AND user_id = :userId LIMIT 1")
    UtilityRecord getRecordByMonth(String yearMonth, String userId);

    // Получение всех записей за конкретный год для пользователя
    @Query("SELECT * FROM utility_records WHERE year_month LIKE :yearPattern AND user_id = :userId")
    List<UtilityRecord> getRecordsByYear(String yearPattern, String userId);

    // Получение записей за год, отсортированных по убыванию общей стоимости
    @Query("SELECT * FROM utility_records WHERE year_month LIKE :yearPattern AND user_id = :userId " +
            "ORDER BY (electricity * electricity_tariff + " +
            "hot_water * hot_water_tariff + " +
            "cold_water * cold_water_tariff + " +
            "gas * gas_tariff) DESC")
    List<UtilityRecord> getRecordsByYearSortedByCost(String yearPattern, String userId);

    // Получение всех записей пользователя для синхронизации
    @Query("SELECT * FROM utility_records WHERE user_id = :userId ORDER BY year_month")
    List<UtilityRecord> getAllRecordsForSync(String userId);

    // Удаление всех записей пользователя
    @Query("DELETE FROM utility_records WHERE user_id = :userId")
    int deleteAllUserRecords(String userId);

    // Проверка существования записи за конкретный месяц
    @Query("SELECT COUNT(*) FROM utility_records WHERE year_month = :yearMonth AND user_id = :userId")
    int countRecordsByMonth(String yearMonth, String userId);

    // Получение последней даты снятия показаний
    @Query("SELECT reading_date FROM utility_records WHERE user_id = :userId ORDER BY year_month DESC LIMIT 1")
    String getLastReadingDate(String userId);

    // Получение всех записей пользователя
    @Query("SELECT * FROM utility_records WHERE user_id = :userId")
    List<UtilityRecord> getAllUserRecords(String userId);
}