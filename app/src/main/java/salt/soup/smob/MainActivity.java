package salt.soup.smob;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.View;

import com.chaquo.python.Python;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;

import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private Button buttonObf;
    private Button buttonPur;

    private TextView textView;
    private TextView patchPath;

    private CheckBox checkBox;
    private CheckBox checkBox2;

    private Spinner spinner;

    private String t = "";

    private Python python;
    private PyObject module;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final int STORAGE_PERMISSION_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializePython();
        initializeListeners();
        checkStoragePermission();
    }

    private void initializeViews() {
        buttonObf = findViewById(R.id.button_obf);
        buttonPur = findViewById(R.id.button_pur);

        textView = findViewById(R.id.textView);
        patchPath = findViewById(R.id.patchPath);

        checkBox = findViewById(R.id.checkBox);
        checkBox2 = findViewById(R.id.checkBox2);

        spinner = findViewById(R.id.spinner);

        if (patchPath != null) {
            patchPath.setText("Выберите папку");
            patchPath.setOnClickListener(v -> openFolderPicker());
        }
    }

    private void initializePython() {
        try {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(this));
            }

            python = Python.getInstance();
            module = python.getModule("salt");

        } catch (Exception e) {
            showMessage("Ошибка запуска Python: " + e.getMessage());
        }
    }

    private void initializeListeners() {
        if (buttonObf != null) {
            buttonObf.setOnClickListener(v -> {
                if (checkSelectedFolder()) {
                    startOperation(t, "obfuscate");
                }
            });
        }

        if (buttonPur != null) {
            buttonPur.setOnClickListener(v -> {
                if (checkSelectedFolder()) {
                    startOperation(t, "purge");
                }
            });
        }
    }

    private void openFolderPicker() {
        try {
            DialogProperties properties = new DialogProperties();

            properties.selection_mode = DialogConfigs.SINGLE_MODE;
            properties.selection_type = DialogConfigs.DIR_SELECT;

            File root = new File(DialogConfigs.DEFAULT_DIR);

            properties.root = root;
            properties.offset = root;
            properties.error_dir = root;
            properties.extensions = null;
            properties.show_hidden_files = false;

            FilePickerDialog dialog =
                    new FilePickerDialog(this, properties);

            dialog.setTitle("Выберите папку");
            dialog.setPositiveBtnName("Выбрать");
            dialog.setNegativeBtnName("Отмена");

            dialog.setDialogSelectionListener(files -> {
                if (files == null || files.length == 0) {
                    return;
                }

                File folder = new File(files[0]);

                if (!folder.exists() || !folder.isDirectory()) {
                    showMessage("Неверная папка");
                    return;
                }

                t = folder.getAbsolutePath();

                if (patchPath != null) {
                    patchPath.setText(t);
                }

                updateStatus("Выбрана папка: " + t);
            });

            dialog.show();

        } catch (Exception e) {
            showMessage("Ошибка файлового менеджера: " +
                    e.getMessage());
        }
    }

    private boolean checkSelectedFolder() {
        if (t == null || t.isEmpty()) {
            showMessage("Сначала выберите папку");
            openFolderPicker();
            return false;
        }

        File folder = new File(t);

        if (!folder.exists() || !folder.isDirectory()) {
            showMessage("Папка не существует");
            return false;
        }

        if (!folder.canRead()) {
            showMessage("Нет доступа к папке");
            return false;
        }

        return true;
    }

    private void startOperation(String folderPath, String operation) {
        if (module == null) {
            showMessage("Python-модуль salt не загружен");
            return;
        }

        final int level = getSelectedLevel();
        final boolean recursive =
                checkBox != null && checkBox.isChecked();
        final boolean constructors =
                checkBox2 != null && checkBox2.isChecked();

        setButtonsEnabled(false);
        updateStatus("Выполняется операция...");

        executor.execute(() -> {
            try {
                PyObject result = module.callAttr(
                        "run",
                        folderPath,
                        operation,
                        level,
                        recursive,
                        constructors
                );

                String output = result == null
                        ? "Операция завершена"
                        : result.toString();

                runOnUiThread(() -> {
                    updateStatus(output);
                    setButtonsEnabled(true);
                });

            } catch (Exception e) {
                String error = e.getMessage();

                runOnUiThread(() -> {
                    updateStatus("Ошибка: " + error);
                    setButtonsEnabled(true);
                    showMessage("Ошибка выполнения Python");
                });
            }
        });
    }

    private int getSelectedLevel() {
        try {
            if (spinner == null ||
                    spinner.getSelectedItem() == null) {
                return 1;
            }

            String value = spinner.getSelectedItem().toString();

            return Integer.parseInt(value);

        } catch (Exception e) {
            return 1;
        }
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                showMessage("Разрешите доступ ко всем файлам");

                try {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + getPackageName())
                    );

                    startActivity(intent);

                } catch (Exception e) {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    );

                    startActivity(intent);
                }
            }

        } else if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        STORAGE_PERMISSION_CODE
                );
            }
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        runOnUiThread(() -> {
            if (buttonObf != null) {
                buttonObf.setEnabled(enabled);
            }

            if (buttonPur != null) {
                buttonPur.setEnabled(enabled);
            }
        });
    }

    private void updateStatus(String message) {
        runOnUiThread(() -> {
            if (textView != null) {
                textView.setText(message);
            }
        });
    }

    private void showMessage(String message) {
        Toast.makeText(
                MainActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (executor != null) {
            executor.shutdown();
        }
    }
                }
