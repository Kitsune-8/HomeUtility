package com.example.homeutility;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainMenuActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Главное меню");
        }

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String email = currentUser.getEmail();
            String username = email != null ? email.split("@")[0] : "Пользователь";

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Toast.makeText(
                        MainMenuActivity.this,
                        "Добро пожаловать, " + username + "!",
                        Toast.LENGTH_LONG
                ).show();
            }, 500);
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Toast.makeText(
                        MainMenuActivity.this,
                        "Добро пожаловать!",
                        Toast.LENGTH_SHORT
                ).show();
            }, 500);
        }

        Button btnInput = findViewById(R.id.btn_input);
        Button btnComparison = findViewById(R.id.btn_comparison);
        Button btnExit = findViewById(R.id.btn_exit);

        btnInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainMenuActivity.this, UtilityInputActivity.class));
            }
        });

        btnComparison.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainMenuActivity.this, ComparisonActivity.class));
            }
        });

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExitDialog();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_profile) {
            showProfile();
            return true;
        } else if (id == R.id.menu_sync_to_cloud) {
            syncToCloud();
            return true;
        } else if (id == R.id.menu_sync_from_cloud) {
            syncFromCloud();
            return true;
        } else if (id == R.id.menu_clear_local) {
            clearLocalData();
            return true;
        } else if (id == R.id.menu_clear_cloud) {
            clearCloudData();
            return true;
        } else if (id == R.id.menu_change_account) {
            changeAccount();
            return true;
        } else if (id == R.id.menu_logout) {
            showLogoutConfirmation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            String uid = user.getUid();
            new AlertDialog.Builder(this)
                    .setTitle("Профиль")
                    .setMessage("Email: " + (email != null ? email : "не указан") +
                            "\nID: " + (uid != null ? uid.substring(0, Math.min(8, uid.length())) + "..." : "недоступно"))
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncToCloud() {
        new AlertDialog.Builder(this)
                .setTitle("Сохранение в облако")
                .setMessage("Сохранить все локальные данные в облаке?")
                .setPositiveButton("Да", (dialog, which) -> {
                    try {
                        FirebaseSyncManager.getInstance(this).syncToFirebase();
                        Toast.makeText(this, "Начинаю синхронизацию...", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void syncFromCloud() {
        new AlertDialog.Builder(this)
                .setTitle("Загрузка из облака")
                .setMessage("Загрузить данные из облака? Локальные данные будут заменены.")
                .setPositiveButton("Да", (dialog, which) -> {
                    try {
                        FirebaseSyncManager.getInstance(this).syncFromFirebase();
                        Toast.makeText(this, "Начинаю загрузку...", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void clearLocalData() {
        new AlertDialog.Builder(this)
                .setTitle("Очистка локальных данных")
                .setMessage("Удалить все локальные данные? Это действие нельзя отменить.")
                .setPositiveButton("Да", (dialog, which) -> {
                    try {
                        FirebaseSyncManager.getInstance(this).clearLocalData();
                        Toast.makeText(this, "Очищаю локальные данные...", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void clearCloudData() {
        new AlertDialog.Builder(this)
                .setTitle("Очистка облачных данных")
                .setMessage("Удалить все данные в облаке? Это действие нельзя отменить.")
                .setPositiveButton("Да", (dialog, which) -> {
                    try {
                        FirebaseSyncManager.getInstance(this).clearCloudData();
                        Toast.makeText(this, "Удаляю облачные данные...", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void changeAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Сменить аккаунт")
                .setMessage("Выйти из текущего аккаунта?")
                .setPositiveButton("Да", (dialog, which) -> logout())
                .setNegativeButton("Нет", null)
                .show();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Выход из аккаунта")
                .setMessage("Вы действительно хотите выйти?")
                .setPositiveButton("Да", (dialog, which) -> logout())
                .setNegativeButton("Нет", null)
                .show();
    }

    private void logout() {
        try {
            mAuth.signOut();
            Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainMenuActivity.this, MainActivity.class));
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка выхода: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Выход из приложения")
                .setMessage("Закрыть приложение?")
                .setPositiveButton("Да", (dialog, which) -> {
                    try {
                        finish();
                    } catch (Exception e) {
                        // Игнорируем ошибку при завершении
                    }
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Очищаем ресурсы
    }
}