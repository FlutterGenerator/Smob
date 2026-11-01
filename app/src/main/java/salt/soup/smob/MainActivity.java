package salt.soup.smob;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Импорт FileChooserBuilder добавь через AIDE:
// FileChooserBuilder
import com.maxieds.androidfilepickerlightlibrary.FileChooserBuilder;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.Python;
import com.chaquo.python.PyObject;

public class MainActivity extends Activity {

    public static String t;
    public static String u;

    private CheckBox o, p;
    private Spinner q;

    private SharedPreferences r;

    private Button obfuscateButton;
    private Button purgeButton;

    private TextView resultText;
    private TextView pathText;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final int REQUEST_FOLDER = 501;
    private static final int REQUEST_STORAGE = 502;

    private boolean operationRunning = false;
    private boolean waitingForSettings = false;
    private volatile boolean destroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        obfuscateButton = findViewById(R.id.button_obf);
        purgeButton = findViewById(R.id.button_pur);

        resultText = findViewById(R.id.textView);
        pathText = findViewById(R.id.patchPath);

        o = findViewById(R.id.checkBox);
        p = findViewById(R.id.checkBox2);
        q = findViewById(R.id.spinner);

        r = getSharedPreferences("settings", MODE_PRIVATE);

        if (q != null) {
            q.setSelection(r.getInt("level", 0));
        }

        if (o != null) {
            o.setChecked(r.getBoolean("rec", false));
        }

        if (p != null) {
            p.setChecked(r.getBoolean("con", false));
        }

        obfuscateButton.setOnClickListener(v -> {
            u = "obf";
            openFolderPicker();
        });

        purgeButton.setOnClickListener(v -> {
            u = "pur";
            openFolderPicker();
        });

        preparePython();
    }

    private void preparePython() {
        executor.execute(() -> {
            try {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(this));
                }

                Python.getInstance();

            } catch (Exception e) {
                showMessage("Python error: " + e.getMessage());
            }
        });
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }

        if (Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    private void requestStoragePermission() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                waitingForSettings = true;

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                );

                try {
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    ));
                }

            } else if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        REQUEST_STORAGE
                );
            }

        } catch (Exception e) {
            showMessage("Permission error: " + e.getMessage());
        }
    }

    private void openFolderPicker() {
        if (operationRunning || destroyed) {
            return;
        }

        if (!hasStoragePermission()) {
            requestStoragePermission();
            return;
        }

        try {
            FileChooserBuilder picker =
                    FileChooserBuilder.getDirectoryChooserInstance(this);

            picker.showHidden(false);

            picker.setPickerInitialPath(
                    FileChooserBuilder.BaseFolderPathType.BASE_PATH_DEFAULT
            );

            picker.launchFilePicker();

        } catch (Exception e) {
            showMessage("Picker error: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (waitingForSettings) {
            waitingForSettings = false;

            if (hasStoragePermission()) {
                openFolderPicker();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQUEST_STORAGE) {
            if (hasStoragePermission()) {
                openFolderPicker();
            } else {
                showMessage("Storage permission denied");
            }
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            List<String> paths =
                    FileChooserBuilder.handleActivityResult(
                            this,
                            requestCode,
                            resultCode,
                            data
                    );

            if (paths == null || paths.isEmpty()) {
                return;
            }

            if (requestCode != REQUEST_FOLDER) {
                return;
            }

            String selectedPath = paths.get(0);

            if (selectedPath == null) {
                showMessage("Folder not selected");
                return;
            }

            File folder = new File(selectedPath);

            if (!folder.exists() || !folder.isDirectory()) {
                showMessage("Invalid folder");
                return;
            }

            t = folder.getAbsolutePath();

            if (pathText != null) {
                pathText.setText(t);
            }

            startOperation(t);

        } catch (Exception e) {
            showMessage("Selection error: " + e.getMessage());
        }
    }

    private void startOperation(String folderPath) {
        if (operationRunning) {
            return;
        }

        operationRunning = true;

        obfuscateButton.setEnabled(false);
        purgeButton.setEnabled(false);

        final String operation = u;

        final int level = q != null
                ? q.getSelectedItemPosition()
                : 0;

        final boolean recursive = o != null && o.isChecked();
        final boolean constructors = p != null && p.isChecked();

        r.edit()
                .putInt("level", level)
                .putBoolean("rec", recursive)
                .putBoolean("con", constructors)
                .apply();

        executor.execute(() -> {
            String result;

            try {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(this));
                }

                Python python = Python.getInstance();

                PyObject module = python.getModule("salt");

                PyObject output = module.callAttr(
                        "run",
                        folderPath,
                        operation,
                        level,
                        recursive,
                        constructors
                );

                result = output == null
                        ? "Operation completed"
                        : output.toString();

            } catch (Exception e) {
                result = "Error: " + e.getMessage();
            }

            final String finalResult = result;

            mainHandler.post(() -> {
                if (destroyed) {
                    return;
                }

                if (resultText != null) {
                    resultText.setText(finalResult);
                }

                operationRunning = false;

                obfuscateButton.setEnabled(true);
                purgeButton.setEnabled(true);
            });
        });
    }

    private void showMessage(String message) {
        mainHandler.post(() -> {
            if (!destroyed) {
                Toast.makeText(
                        MainActivity.this,
                        message,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }
                }
