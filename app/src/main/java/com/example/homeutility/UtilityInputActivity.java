package com.example.homeutility;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class UtilityInputActivity extends AppCompatActivity {

    private EditText etMonthYear, etReadingDate;
    private EditText etElectricity, etHotWater, etColdWater, etGas;
    private EditText etElectricityTariff, etHotWaterTariff, etColdWaterTariff, etGasTariff;
    private TextView tvMonthDate, tvElectricityCost, tvHotWaterCost, tvColdWaterCost, tvGasCost, tvTotalCost;
    private Button btnSave, btnPrevMonth, btnNextMonth;

    private Calendar calendar;
    private SimpleDateFormat monthFormat, displayFormat;
    private AppDatabase database;
    private UtilityDao utilityDao;
    private String currentYearMonth;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utility_input);

        // Настраиваем Toolbar с кнопкой "Назад"
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ком.услуги");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Пожалуйста, войдите в систему", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = currentUser.getUid();

        Log.d("UtilityInput", "Пользователь: " + userId);

        try {
            database = AppDatabase.getDatabase(this);
            utilityDao = database.utilityDao();
            Log.d("UtilityInput", "База данных инициализирована успешно");
        } catch (Exception e) {
            Log.e("UtilityInput", "Ошибка инициализации базы данных: " + e.getMessage(), e);
            Toast.makeText(this, "Ошибка базы данных: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        calendar = Calendar.getInstance();
        monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        displayFormat = new SimpleDateFormat("MMMM", new Locale("ru"));
        currentYearMonth = monthFormat.format(calendar.getTime());

        Log.d("UtilityInput", "Текущий месяц: " + currentYearMonth);

        initializeViews();
        setupDateNavigation();
        setupCalculationListeners();

        checkReadingDate();
        loadCurrentMonthData();
    }

    private void initializeViews() {
        etMonthYear = findViewById(R.id.et_month_year);
        etReadingDate = findViewById(R.id.et_reading_date);
        tvMonthDate = findViewById(R.id.tv_month_date);

        etElectricity = findViewById(R.id.et_electricity);
        etHotWater = findViewById(R.id.et_hot_water);
        etColdWater = findViewById(R.id.et_cold_water);
        etGas = findViewById(R.id.et_gas);

        etElectricityTariff = findViewById(R.id.et_electricity_tariff);
        etHotWaterTariff = findViewById(R.id.et_hot_water_tariff);
        etColdWaterTariff = findViewById(R.id.et_cold_water_tariff);
        etGasTariff = findViewById(R.id.et_gas_tariff);

        tvElectricityCost = findViewById(R.id.tv_electricity_cost);
        tvHotWaterCost = findViewById(R.id.tv_hot_water_cost);
        tvColdWaterCost = findViewById(R.id.tv_cold_water_cost);
        tvGasCost = findViewById(R.id.tv_gas_cost);
        tvTotalCost = findViewById(R.id.tv_total_cost);

        btnSave = findViewById(R.id.btn_save);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);

        updateMonthDisplay();

        btnSave.setOnClickListener(v -> saveOrUpdateRecord());
        etReadingDate.setOnClickListener(v -> showDatePicker());

        // Обработчик для выбора произвольной даты - клик на поле месяца/года
        etMonthYear.setOnClickListener(v -> showMonthYearPicker());
    }

    private void setupDateNavigation() {
        btnPrevMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadCurrentMonthData();
        });

        btnNextMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadCurrentMonthData();
        });
    }

    private void showMonthYearPicker() {
        // Диалог для выбора года и месяца
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_month_year_picker, null);
        final EditText etYear = dialogView.findViewById(R.id.et_dialog_year);
        final EditText etMonth = dialogView.findViewById(R.id.et_dialog_month);

        // Устанавливаем текущие значения
        etYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));
        etMonth.setText(String.valueOf(calendar.get(Calendar.MONTH) + 1));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Выберите месяц и год")
                .setView(dialogView)
                .setPositiveButton("Перейти", (d, which) -> {
                    try {
                        int year = Integer.parseInt(etYear.getText().toString());
                        int month = Integer.parseInt(etMonth.getText().toString()) - 1;

                        if (year < 2000 || year > 2100) {
                            Toast.makeText(this, "Введите год от 2000 до 2100", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (month < 0 || month > 11) {
                            Toast.makeText(this, "Введите месяц от 1 до 12", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, 1);

                        updateMonthDisplay();
                        loadCurrentMonthData();

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Введите корректные данные", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .create();

        dialog.show();
    }

    private void updateMonthDisplay() {
        currentYearMonth = monthFormat.format(calendar.getTime());

        String month = String.format("%02d", calendar.get(Calendar.MONTH) + 1);
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        etMonthYear.setText(month + "." + year);

        String monthName = getMonthNameInNominative(calendar.get(Calendar.MONTH));

        // Обновляем дату снятия показаний для текущего месяца
        updateReadingDate();

        // Получаем отформатированную дату для отображения
        String readingDateStr = etReadingDate.getText().toString().trim();
        if (!readingDateStr.isEmpty()) {
            tvMonthDate.setText(monthName + ", (" + readingDateStr + ")");
        } else {
            tvMonthDate.setText(monthName);
        }

        Log.d("UtilityInput", "Отображение месяца: " + monthName + ", формат БД: " + currentYearMonth);
    }

    private String getMonthNameInNominative(int monthIndex) {
        String[] monthNames = {
                "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        };

        if (monthIndex >= 0 && monthIndex < monthNames.length) {
            return monthNames[monthIndex];
        }
        return "";
    }

    private void checkReadingDate() {
        SharedPreferences prefs = getSharedPreferences("utility_app_" + userId, MODE_PRIVATE);
        int defaultDay = prefs.getInt("default_day_of_month", -1);

        if (defaultDay == -1) {
            showFirstTimeDateDialog();
        } else {
            updateReadingDate();
        }
    }

    private void updateReadingDate() {
        SharedPreferences prefs = getSharedPreferences("utility_app_" + userId, MODE_PRIVATE);
        int defaultDay = prefs.getInt("default_day_of_month", -1);

        if (defaultDay != -1) {
            // Создаем копию календаря для расчета даты
            Calendar readingCalendar = (Calendar) calendar.clone();

            // Проверяем, существует ли такой день в текущем месяце
            int maxDay = readingCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            int dayToSet = Math.min(defaultDay, maxDay);

            readingCalendar.set(Calendar.DAY_OF_MONTH, dayToSet);

            // Форматируем дату: день.месяц.год
            String dayStr = String.valueOf(dayToSet);
            String monthStr = String.valueOf(readingCalendar.get(Calendar.MONTH) + 1);
            String yearStr = String.valueOf(readingCalendar.get(Calendar.YEAR));

            String readingDate = dayStr + "." + monthStr + "." + yearStr;
            etReadingDate.setText(readingDate);
        } else {
            etReadingDate.setText("");
        }
    }

    private void showFirstTimeDateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Установите дату снятия показаний")
                .setMessage("Пожалуйста, установите день месяца, к которому вы будете снимать показания со счётчиков (например, 7, 10, 24 и т.д.).")
                .setPositiveButton("Установить", (dialog, which) -> showDatePicker())
                .setCancelable(false)
                .show();
    }

    private void showDatePicker() {
        Calendar datePickerCalendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    // Сохраняем только выбранный день месяца
                    SharedPreferences prefs = getSharedPreferences("utility_app_" + userId, MODE_PRIVATE);
                    prefs.edit().putInt("default_day_of_month", dayOfMonth).apply();

                    // Обновляем дату с учетом текущего месяца и года
                    updateReadingDate();
                    updateMonthDisplay();

                    Toast.makeText(this, "День снятия показаний установлен: " + dayOfMonth + " число", Toast.LENGTH_SHORT).show();
                },
                datePickerCalendar.get(Calendar.YEAR),
                datePickerCalendar.get(Calendar.MONTH),
                datePickerCalendar.get(Calendar.DAY_OF_MONTH));

        datePicker.show();
    }

    private void setupCalculationListeners() {
        setupCostCalculator(etElectricity, etElectricityTariff, tvElectricityCost);
        setupCostCalculator(etHotWater, etHotWaterTariff, tvHotWaterCost);
        setupCostCalculator(etColdWater, etColdWaterTariff, tvColdWaterCost);
        setupCostCalculator(etGas, etGasTariff, tvGasCost);
    }

    private void setupCostCalculator(EditText consumptionEditText, EditText tariffEditText, TextView costTextView) {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateSingleCost(consumptionEditText, tariffEditText, costTextView);
                calculateTotalCost();
            }
        };

        consumptionEditText.addTextChangedListener(watcher);
        tariffEditText.addTextChangedListener(watcher);
    }

    private void calculateSingleCost(EditText consumptionEditText, EditText tariffEditText, TextView costTextView) {
        try {
            double consumption = getDouble(consumptionEditText);
            double tariff = getDouble(tariffEditText);
            double cost = consumption * tariff;
            costTextView.setText(String.format("Сумма: %.2f руб.", cost));
        } catch (NumberFormatException e) {
            costTextView.setText("Сумма: 0.00 руб.");
        }
    }

    private void calculateTotalCost() {
        double total = 0;
        total += getDouble(etElectricity) * getDouble(etElectricityTariff);
        total += getDouble(etHotWater) * getDouble(etHotWaterTariff);
        total += getDouble(etColdWater) * getDouble(etColdWaterTariff);
        total += getDouble(etGas) * getDouble(etGasTariff);

        tvTotalCost.setText(String.format("ИТОГО: %.2f руб.", total));

        calculateSingleCost(etElectricity, etElectricityTariff, tvElectricityCost);
        calculateSingleCost(etHotWater, etHotWaterTariff, tvHotWaterCost);
        calculateSingleCost(etColdWater, etColdWaterTariff, tvColdWaterCost);
        calculateSingleCost(etGas, etGasTariff, tvGasCost);
    }

    private double getDouble(EditText editText) {
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(text.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void loadCurrentMonthData() {
        new Thread(() -> {
            try {
                Log.d("UtilityInput", "Загрузка данных за месяц: " + currentYearMonth + ", пользователь: " + userId);

                UtilityRecord record = utilityDao.getRecordByMonth(currentYearMonth, userId);

                if (record != null) {
                    Log.d("UtilityInput", "Найдена запись ID: " + record.getId() +
                            ", свет: " + record.getElectricity() +
                            ", тариф: " + record.getElectricityTariff());
                } else {
                    Log.d("UtilityInput", "Запись не найдена, будет создана новая");
                }

                runOnUiThread(() -> {
                    if (record != null) {
                        // Загружаем сохраненные данные
                        etElectricity.setText(formatDouble(record.getElectricity()));
                        etHotWater.setText(formatDouble(record.getHotWater()));
                        etColdWater.setText(formatDouble(record.getColdWater()));
                        etGas.setText(formatDouble(record.getGas()));

                        // Тарифы загружаем только если они не пустые
                        if (record.getElectricityTariff() > 0) {
                            etElectricityTariff.setText(formatDouble(record.getElectricityTariff()));
                        }
                        if (record.getHotWaterTariff() > 0) {
                            etHotWaterTariff.setText(formatDouble(record.getHotWaterTariff()));
                        }
                        if (record.getColdWaterTariff() > 0) {
                            etColdWaterTariff.setText(formatDouble(record.getColdWaterTariff()));
                        }
                        if (record.getGasTariff() > 0) {
                            etGasTariff.setText(formatDouble(record.getGasTariff()));
                        }

                        // Загружаем дату снятия показаний
                        if (record.getReadingDate() != null && !record.getReadingDate().isEmpty()) {
                            etReadingDate.setText(record.getReadingDate());
                        }

                        calculateTotalCost();
                        Toast.makeText(UtilityInputActivity.this,
                                "Загружены данные за " + getMonthNameInNominative(calendar.get(Calendar.MONTH)),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Если данных нет, очищаем только показания, тарифы оставляем
                        clearConsumptionFields();
                        Toast.makeText(UtilityInputActivity.this,
                                "Нет данных за " + getMonthNameInNominative(calendar.get(Calendar.MONTH)) + ". Можно ввести новые.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e("UtilityInput", "Ошибка загрузки данных: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(UtilityInputActivity.this,
                            "Ошибка загрузки данных: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String formatDouble(double value) {
        if (value == 0.0) return "";
        if (value == (long) value) {
            return String.format(Locale.getDefault(), "%d", (long) value);
        } else {
            // Оставляем 2 знака после запятой, убираем .00 если целое число
            String formatted = String.format(Locale.getDefault(), "%.2f", value);
            if (formatted.endsWith(".00")) {
                return formatted.substring(0, formatted.length() - 3);
            }
            return formatted;
        }
    }

    private void clearConsumptionFields() {
        // Очищаем только показания счетчиков, тарифы НЕ очищаем
        etElectricity.setText("");
        etHotWater.setText("");
        etColdWater.setText("");
        etGas.setText("");
        calculateTotalCost();
    }

    private void saveOrUpdateRecord() {
        String readingDateStr = etReadingDate.getText().toString().trim();
        if (readingDateStr.isEmpty()) {
            Toast.makeText(this, "Установите дату снятия показаний", Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверяем, что хотя бы одно поле заполнено
        if (etElectricity.getText().toString().trim().isEmpty() &&
                etHotWater.getText().toString().trim().isEmpty() &&
                etColdWater.getText().toString().trim().isEmpty() &&
                etGas.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Введите хотя бы одно показание", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                UtilityRecord record = new UtilityRecord();
                record.setUserId(userId);
                record.setYearMonth(currentYearMonth);
                record.setReadingDate(readingDateStr);

                record.setElectricity(getDouble(etElectricity));
                record.setHotWater(getDouble(etHotWater));
                record.setColdWater(getDouble(etColdWater));
                record.setGas(getDouble(etGas));

                // Если тарифы не заполнены, устанавливаем 0
                record.setElectricityTariff(getDouble(etElectricityTariff));
                record.setHotWaterTariff(getDouble(etHotWaterTariff));
                record.setColdWaterTariff(getDouble(etColdWaterTariff));
                record.setGasTariff(getDouble(etGasTariff));

                Log.d("UtilityInput", "Сохранение записи: месяц=" + currentYearMonth +
                        ", свет=" + record.getElectricity() +
                        ", горячая вода=" + record.getHotWater() +
                        ", холодная вода=" + record.getColdWater() +
                        ", газ=" + record.getGas());

                // Проверяем, существует ли запись
                UtilityRecord existingRecord = utilityDao.getRecordByMonth(currentYearMonth, userId);

                String message;
                if (existingRecord != null) {
                    // Обновляем существующую запись
                    record.setId(existingRecord.getId());
                    utilityDao.update(record);
                    message = "Данные обновлены за " + getMonthNameInNominative(calendar.get(Calendar.MONTH));
                    Log.d("UtilityInput", "Обновлена существующая запись ID: " + existingRecord.getId());
                } else {
                    // Вставляем новую запись
                    utilityDao.insert(record);

                    // Получаем ID новой записи
                    UtilityRecord newRecord = utilityDao.getRecordByMonth(currentYearMonth, userId);
                    message = "Данные сохранены за " + getMonthNameInNominative(calendar.get(Calendar.MONTH));
                    Log.d("UtilityInput", "Создана новая запись" +
                            (newRecord != null ? " ID: " + newRecord.getId() : " (но не найдена после создания)"));
                }

                // Проверяем количество записей
                List<UtilityRecord> allRecords = utilityDao.getAllUserRecords(userId);
                Log.d("UtilityInput", "=== ОТЧЕТ О СОХРАНЕНИИ ===");
                Log.d("UtilityInput", "Всего записей пользователя: " + allRecords.size());
                for (UtilityRecord r : allRecords) {
                    Log.d("UtilityInput", "Запись: ID=" + r.getId() +
                            ", месяц=" + r.getYearMonth() +
                            ", пользователь=" + r.getUserId() +
                            ", свет=" + r.getElectricity() +
                            ", общая стоимость=" + r.getTotalCost());
                }

                runOnUiThread(() -> {
                    Toast.makeText(UtilityInputActivity.this,
                            message + " (всего записей: " + allRecords.size() + ")",
                            Toast.LENGTH_LONG).show();

                    // Автосинхронизация с облаком
                    try {
                        FirebaseSyncManager.getInstance(UtilityInputActivity.this).scheduleAutoSync();
                    } catch (Exception e) {
                        Log.e("UtilityInput", "Ошибка синхронизации: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.e("UtilityInput", "Критическая ошибка сохранения: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(UtilityInputActivity.this,
                            "Ошибка сохранения: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    // Дополнительная информация об ошибке
                    if (e.getMessage() != null && e.getMessage().contains("database")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(UtilityInputActivity.this);
                        builder.setTitle("Ошибка базы данных")
                                .setMessage("Возможно, база данных повреждена. Попробуйте переустановить приложение или очистить данные.")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("UtilityInput", "Activity уничтожена");
    }
}