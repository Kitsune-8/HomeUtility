package com.example.homeutility;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseSyncManager {

    private static FirebaseSyncManager instance;
    private FirebaseFirestore firestore;
    private AppDatabase localDatabase;
    private Context context;
    private Handler mainHandler;

    private FirebaseSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.localDatabase = AppDatabase.getDatabase(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized FirebaseSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseSyncManager(context);
        }
        return instance;
    }

    // Синхронизация данных на Firebase
    public void syncToFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToast("Пользователь не авторизован");
            return;
        }

        String userId = currentUser.getUid();

        new Thread(() -> {
            try {
                List<UtilityRecord> records = localDatabase.utilityDao().getAllRecordsForSync(userId);

                if (records.isEmpty()) {
                    showToast("Нет данных для синхронизации");
                    return;
                }

                List<Map<String, Object>> firestoreData = new ArrayList<>();
                for (UtilityRecord record : records) {
                    Map<String, Object> recordMap = new HashMap<>();
                    recordMap.put("userId", record.getUserId());
                    recordMap.put("yearMonth", record.getYearMonth());
                    recordMap.put("readingDate", record.getReadingDate());
                    recordMap.put("electricity", record.getElectricity());
                    recordMap.put("hotWater", record.getHotWater());
                    recordMap.put("coldWater", record.getColdWater());
                    recordMap.put("gas", record.getGas());
                    recordMap.put("electricityTariff", record.getElectricityTariff());
                    recordMap.put("hotWaterTariff", record.getHotWaterTariff());
                    recordMap.put("coldWaterTariff", record.getColdWaterTariff());
                    recordMap.put("gasTariff", record.getGasTariff());
                    recordMap.put("syncTimestamp", System.currentTimeMillis());

                    firestoreData.add(recordMap);
                }

                Map<String, Object> syncData = new HashMap<>();
                syncData.put("records", firestoreData);
                syncData.put("lastSync", System.currentTimeMillis());
                syncData.put("userId", userId);
                syncData.put("recordCount", firestoreData.size());

                firestore.collection("utility_records")
                        .document(userId)
                        .set(syncData)
                        .addOnSuccessListener(aVoid -> showToast("Данные синхронизированы с облаком"))
                        .addOnFailureListener(e -> {
                            Log.e("FirebaseSync", "Ошибка синхронизации: " + e.getMessage(), e);
                            showToast("Ошибка синхронизации: " + e.getMessage());
                        });

                Log.d("FirebaseSync", "Отправлено записей в облако: " + firestoreData.size());

            } catch (Exception e) {
                Log.e("FirebaseSync", "Ошибка подготовки данных: ", e);
                showToast("Ошибка: " + e.getMessage());
            }
        }).start();
    }

    // Загрузка данных из Firebase
    public void syncFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToast("Пользователь не авторизован");
            return;
        }

        String userId = currentUser.getUid();

        showToast("Начинаю загрузку из облака...");

        firestore.collection("utility_records")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("records")) {
                        try {
                            List<Map<String, Object>> records = (List<Map<String, Object>>) documentSnapshot.get("records");

                            new Thread(() -> {
                                try {
                                    // Удаляем старые записи пользователя
                                    int deletedCount = localDatabase.utilityDao().deleteAllUserRecords(userId);
                                    Log.d("FirebaseSync", "Удалено старых записей: " + deletedCount);

                                    int loadedCount = 0;
                                    for (Map<String, Object> recordMap : records) {
                                        try {
                                            UtilityRecord record = new UtilityRecord();
                                            record.setUserId((String) recordMap.get("userId"));
                                            record.setYearMonth((String) recordMap.get("yearMonth"));
                                            record.setReadingDate((String) recordMap.get("readingDate"));

                                            // Безопасное получение числовых значений
                                            record.setElectricity(getDoubleValue(recordMap.get("electricity")));
                                            record.setHotWater(getDoubleValue(recordMap.get("hotWater")));
                                            record.setColdWater(getDoubleValue(recordMap.get("coldWater")));
                                            record.setGas(getDoubleValue(recordMap.get("gas")));

                                            record.setElectricityTariff(getDoubleValue(recordMap.get("electricityTariff")));
                                            record.setHotWaterTariff(getDoubleValue(recordMap.get("hotWaterTariff")));
                                            record.setColdWaterTariff(getDoubleValue(recordMap.get("coldWaterTariff")));
                                            record.setGasTariff(getDoubleValue(recordMap.get("gasTariff")));

                                            localDatabase.utilityDao().insert(record);
                                            loadedCount++;

                                        } catch (Exception e) {
                                            Log.e("FirebaseSync", "Ошибка обработки записи: ", e);
                                        }
                                    }

                                    final int finalLoadedCount = loadedCount;
                                    mainHandler.post(() -> {
                                        showToast("Загружено записей: " + finalLoadedCount);
                                        Log.d("FirebaseSync", "Успешно загружено записей: " + finalLoadedCount);
                                    });

                                } catch (Exception e) {
                                    Log.e("FirebaseSync", "Ошибка в фоновом потоке: ", e);
                                    showToast("Ошибка загрузки данных: " + e.getMessage());
                                }
                            }).start();

                        } catch (Exception e) {
                            Log.e("FirebaseSync", "Ошибка парсинга данных: ", e);
                            showToast("Ошибка формата данных в облаке");
                        }
                    } else {
                        showToast("В облаке нет сохраненных данных");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseSync", "Ошибка загрузки из Firebase: ", e);
                    showToast("Ошибка загрузки: " + e.getMessage());
                });
    }

    // Вспомогательный метод для безопасного получения double
    private double getDoubleValue(Object value) {
        if (value == null) return 0.0;
        try {
            if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof Long) {
                return ((Long) value).doubleValue();
            } else if (value instanceof Integer) {
                return ((Integer) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            } else {
                return 0.0;
            }
        } catch (Exception e) {
            return 0.0;
        }
    }

    // Очистка локальных данных
    public void clearLocalData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToast("Пользователь не авторизован");
            return;
        }

        String userId = currentUser.getUid();

        showToast("Очищаю локальные данные...");

        new Thread(() -> {
            try {
                int deletedCount = localDatabase.utilityDao().deleteAllUserRecords(userId);
                Log.d("FirebaseSync", "Удалено локальных записей: " + deletedCount);
                showToast("Удалено локальных записей: " + deletedCount);
            } catch (Exception e) {
                Log.e("FirebaseSync", "Ошибка очистки локальных данных: ", e);
                showToast("Ошибка очистки: " + e.getMessage());
            }
        }).start();
    }

    // Очистка облачных данных
    public void clearCloudData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToast("Пользователь не авторизован");
            return;
        }

        String userId = currentUser.getUid();

        showToast("Удаляю данные из облака...");

        firestore.collection("utility_records")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    showToast("Данные в облаке удалены");
                    Log.d("FirebaseSync", "Облачные данные удалены для пользователя: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseSync", "Ошибка удаления облачных данных: ", e);
                    showToast("Ошибка удаления: " + e.getMessage());
                });
    }

    // Автоматическая синхронизация
    public void scheduleAutoSync() {
        // Откладываем синхронизацию на 1 секунду, чтобы не блокировать UI
        mainHandler.postDelayed(this::syncToFirebase, 1000);
    }

    // Безопасный показ Toast с проверкой контекста
    private void showToast(String message) {
        if (context != null) {
            mainHandler.post(() -> {
                try {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e("FirebaseSync", "Ошибка показа Toast: ", e);
                }
            });
        }
    }

    // Метод для проверки соединения с Firebase
    public void testConnection() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToast("Нет подключения к Firebase: пользователь не авторизован");
            return;
        }

        showToast("Проверка соединения с Firebase...");

        firestore.collection("test")
                .document("connection")
                .set(new HashMap<String, Object>() {{ put("timestamp", System.currentTimeMillis()); }})
                .addOnSuccessListener(aVoid -> showToast("Соединение с Firebase установлено"))
                .addOnFailureListener(e -> showToast("Ошибка соединения: " + e.getMessage()));
    }
}