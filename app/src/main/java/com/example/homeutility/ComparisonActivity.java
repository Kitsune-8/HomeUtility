package com.example.homeutility;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ComparisonActivity extends AppCompatActivity {

    private EditText etYear;
    private TextView tvYearTitle;
    private Button btnPrevYear, btnNextYear;
    private LinearLayout comparisonContainer;
    private AppDatabase database;
    private UtilityDao utilityDao;
    private String userId;

    private int currentYear = Calendar.getInstance().get(Calendar.YEAR);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comparison);

        // Настраиваем Toolbar с кнопкой "Назад"
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Сравнение");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Получаем текущего пользователя
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = currentUser.getUid();

        // Инициализация базы данных
        try {
            database = AppDatabase.getDatabase(this);
            utilityDao = database.utilityDao();
            Log.d("Comparison", "База данных инициализирована");
        } catch (Exception e) {
            Log.e("Comparison", "Ошибка инициализации базы данных: " + e.getMessage(), e);
            Toast.makeText(this, "Ошибка базы данных", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etYear = findViewById(R.id.et_year);
        tvYearTitle = findViewById(R.id.tv_year_title);
        btnPrevYear = findViewById(R.id.btn_prev_year);
        btnNextYear = findViewById(R.id.btn_next_year);
        comparisonContainer = findViewById(R.id.comparison_container);

        // Устанавливаем текущий год
        updateYearDisplay();

        btnPrevYear.setOnClickListener(v -> {
            currentYear--;
            updateYearDisplay();
            loadComparisonData();
        });

        btnNextYear.setOnClickListener(v -> {
            currentYear++;
            updateYearDisplay();
            loadComparisonData();
        });

        // Обработчик клика на поле года - быстрый выбор года
        etYear.setOnClickListener(v -> showYearPickerDialog());

        // Сначала проверяем все записи
        checkAllRecords();
        loadComparisonData();
    }

    private void checkAllRecords() {
        new Thread(() -> {
            try {
                List<UtilityRecord> allRecords = utilityDao.getAllUserRecords(userId);
                Log.d("Comparison", "=== ПРОВЕРКА ВСЕХ ЗАПИСЕЙ ===");
                Log.d("Comparison", "Всего записей в БД: " + allRecords.size());
                Log.d("Comparison", "User ID: " + userId);

                for (UtilityRecord record : allRecords) {
                    Log.d("Comparison", "Запись: ID=" + record.getId() +
                            ", месяц=" + record.getYearMonth() +
                            ", пользователь=" + record.getUserId() +
                            ", свет=" + record.getElectricity() +
                            ", общая стоимость=" + record.getTotalCost());
                }

                runOnUiThread(() -> {
                    Toast.makeText(ComparisonActivity.this,
                            "Всего записей: " + allRecords.size(),
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e("Comparison", "Ошибка проверки записей: " + e.getMessage(), e);
            }
        }).start();
    }

    private void showYearPickerDialog() {
        // Создаем диалог для выбора года
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_year_picker, null);
        final EditText etDialogYear = dialogView.findViewById(R.id.et_dialog_year);

        // Устанавливаем текущее значение
        etDialogYear.setText(String.valueOf(currentYear));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Выберите год")
                .setView(dialogView)
                .setPositiveButton("Перейти", (d, which) -> {
                    try {
                        int year = Integer.parseInt(etDialogYear.getText().toString());

                        if (year < 2000 || year > 2100) {
                            Toast.makeText(this, "Введите год от 2000 до 2100", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        currentYear = year;
                        updateYearDisplay();
                        loadComparisonData();

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Введите корректный год", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .create();

        dialog.show();
    }

    private void updateYearDisplay() {
        etYear.setText(String.valueOf(currentYear));
        tvYearTitle.setText(currentYear + " год");
    }

    private void loadComparisonData() {
        new Thread(() -> {
            try {
                String yearPattern = currentYear + "-%";
                Log.d("Comparison", "Загружаем данные за год: " + currentYear);
                Log.d("Comparison", "Шаблон поиска: " + yearPattern);

                List<UtilityRecord> records = utilityDao.getRecordsByYearSortedByCost(
                        yearPattern, userId);

                Log.d("Comparison", "Найдено записей: " + (records != null ? records.size() : 0));

                if (records != null) {
                    for (UtilityRecord record : records) {
                        Log.d("Comparison", "Найденная запись: " + record.getYearMonth() +
                                ", стоимость: " + record.getTotalCost());
                    }
                }

                runOnUiThread(() -> {
                    comparisonContainer.removeAllViews();

                    if (records == null || records.isEmpty()) {
                        TextView tvNoData = new TextView(this);
                        tvNoData.setText("Нет данных за " + currentYear + " год\n\nВсего сохранённых месяцев: " +
                                getTotalMonthsCount());
                        tvNoData.setTextSize(18);
                        tvNoData.setTextColor(getResources().getColor(android.R.color.white));
                        tvNoData.setGravity(View.TEXT_ALIGNMENT_CENTER);
                        tvNoData.setPadding(0, 50, 0, 0);
                        comparisonContainer.addView(tvNoData);
                        return;
                    }

                    // Создаём карточки в порядке убывания стоимости
                    for (int i = 0; i < records.size(); i++) {
                        UtilityRecord record = records.get(i);
                        addComparisonCard(record, i + 1);
                    }

                    // Показываем общее количество месяцев
                    TextView tvSummary = new TextView(this);
                    tvSummary.setText("\nВсего месяцев за " + currentYear + " год: " + records.size());
                    tvSummary.setTextSize(16);
                    tvSummary.setTextColor(getResources().getColor(android.R.color.white));
                    tvSummary.setGravity(View.TEXT_ALIGNMENT_CENTER);
                    tvSummary.setPadding(0, 20, 0, 20);
                    comparisonContainer.addView(tvSummary);
                });
            } catch (Exception e) {
                Log.e("Comparison", "Ошибка загрузки данных: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(ComparisonActivity.this,
                            "Ошибка загрузки данных: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private int getTotalMonthsCount() {
        try {
            List<UtilityRecord> allRecords = utilityDao.getAllUserRecords(userId);
            return allRecords != null ? allRecords.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void addComparisonCard(UtilityRecord record, int position) {
        String monthName = getMonthNameFromYearMonth(record.getYearMonth());
        double totalCost = record.getTotalCost();

        // Создаём карточку
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(25, 20, 25, 20);

        // Определяем фон карточки
        if (position <= 3) {
            card.setBackgroundColor(0xB3FF8A24); // Оранжевый для топ-3
            card.setElevation(8f);
        } else {
            card.setBackgroundColor(0x80FFFFFF); // Белый для остальных
            card.setElevation(4f);
        }

        // Скругленные углы
        card.setClipToOutline(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 15);
        card.setLayoutParams(params);

        // Заголовок карточки
        TextView tvHeader = new TextView(this);
        tvHeader.setText(position + " место - " + monthName);
        tvHeader.setTextSize(20);
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHeader.setTextColor(position <= 3 ? 0xFFFFFFFF : 0xFF333333);

        // Общая стоимость
        TextView tvCost = new TextView(this);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("ru", "RU"));
        String costText = "Общая стоимость: " + formatter.format(totalCost);
        tvCost.setText(costText);
        tvCost.setTextSize(18);
        tvCost.setPadding(0, 8, 0, 0);
        tvCost.setTextColor(position <= 3 ? 0xFFFFFFFF : 0xFF333333);

        // Детали по услугам
        TextView tvDetails = new TextView(this);
        String detailsText = String.format(Locale.getDefault(),
                "Свет: %.2f руб.\n" +
                        "Горячая вода: %.2f руб.\n" +
                        "Холодная вода: %.2f руб.\n" +
                        "Газ: %.2f руб.\n" +
                        "Дата: %s",
                record.getElectricityCost(),
                record.getHotWaterCost(),
                record.getColdWaterCost(),
                record.getGasCost(),
                record.getReadingDate() != null ? record.getReadingDate() : "не указана");
        tvDetails.setText(detailsText);
        tvDetails.setTextSize(16);
        tvDetails.setPadding(0, 12, 0, 0);
        tvDetails.setTextColor(position <= 3 ? 0xFFEEEEEE : 0xFF555555);

        // Добавляем элементы в карточку
        card.addView(tvHeader);
        card.addView(tvCost);
        card.addView(tvDetails);

        // Добавляем карточку в контейнер
        comparisonContainer.addView(card);
    }

    private String getMonthNameFromYearMonth(String yearMonth) {
        try {
            String[] parts = yearMonth.split("-");
            if (parts.length >= 2) {
                int month = Integer.parseInt(parts[1]);
                String[] monthNames = {
                        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
                };
                if (month >= 1 && month <= 12) {
                    return monthNames[month - 1] + " " + parts[0];
                }
            }
        } catch (Exception e) {
            Log.e("Comparison", "Ошибка парсинга месяца: " + yearMonth, e);
        }
        return yearMonth;
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
    }
}