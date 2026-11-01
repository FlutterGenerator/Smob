
package salt.soup.smob;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.codekidlabs.storagechooser.StorageChooser;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.PyObject;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener,
        android.widget.CompoundButton.OnCheckedChangeListener {

    public static String t = "";
    public static String u = "";

    public CheckBox o;
    public CheckBox p;
    public Spinner q;
    public SharedPreferences r;
    public View s;

    private Button obfuscateButton;
    private Button purgeButton;

    private TextView resultText;
    private TextView pathText;

    private Handler mainHandler;
    private ExecutorService executor;

    private static final int REQ_STORAGE = 100;

    private boolean waitingForStorageSettings = false;
    private boolean operationRunning = false;
    private volatile boolean activityDestroyed = false;

    // --------------------------------
    // ACTIVITY
    // --------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();

        r = PreferenceManager.getDefaultSharedPreferences(this);

        obfuscateButton = findViewById(R.id.button_obf);
        purgeButton = findViewById(R.id.button_pur);

        resultText = findViewById(R.id.textView);
        pathText = findViewById(R.id.patchPath);

        s = findViewById(R.id.progressBar);

        o = findViewById(R.id.checkBox);
        p = findViewById(R.id.checkBox2);
        q = findViewById(R.id.spinner);

        obfuscateButton.setEnabled(false);
        purgeButton.setEnabled(false);

        setupSpinner();
        setupPreferences();

        o.setOnCheckedChangeListener(this);
        p.setOnCheckedChangeListener(this);

        obfuscateButton.setOnClickListener(v -> {
            if (operationRunning) return;

            u = "obf";
            resetFields();
            openChooser();
        });

        purgeButton.setOnClickListener(v -> {
            if (operationRunning) return;

            u = "pur";
            resetFields();
            openChooser();
        });

        prepareFiles();
    }

    // --------------------------------
    // STORAGE PERMISSIONS
    // --------------------------------

    private boolean hasStorageAccess() {

        // Android 11 and newer: API 30+
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }

        // Android 6 through Android 10: API 23-29
        boolean read = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;

        boolean write = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;

        return read && write;

        // Android 5 and older
    }

    private void openChooser() {

        if (hasStorageAccess()) {
            showStorageChooser();
        } else {
            requestStorageAccess();
        }
    }

    private void requestStorageAccess() {

        // Android 11 and newer
        if (Build.VERSION.SDK_INT >= 30) {

            waitingForStorageSettings = true;

            try {

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                );

                startActivity(intent);

            } catch (Exception e) {

                try {

                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    );

                    startActivity(intent);

                } catch (Exception ex) {

                    waitingForStorageSettings = false;

                    showMessage(
                            "Cannot open storage settings: "
                                    + ex.getMessage()
                    );
                }
            }

            // Android 6 through Android 10
        } else {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQ_STORAGE
            );

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (waitingForStorageSettings) {

            waitingForStorageSettings = false;

            if (hasStorageAccess()) {
                showStorageChooser();
            } else {
                showMessage("Storage permission denied");
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

        if (requestCode == REQ_STORAGE) {

            if (hasStorageAccess()) {
                showStorageChooser();
            } else {
                showMessage("Storage permission denied");
            }
        }
    }

    // --------------------------------
    // STORAGE CHOOSER
    // --------------------------------

    private void showStorageChooser() {

        try {

            StorageChooser chooser = new StorageChooser.Builder()
                    .withActivity(this)
                    .withFragmentManager(getFragmentManager())
                    .withMemoryBar(true)
                    .allowCustomPath(true)
                    .setType(StorageChooser.DIRECTORY_CHOOSER)
                    .build();

            chooser.setOnSelectListener(path -> {

                if (path == null || path.trim().isEmpty()) {
                    showMessage("Folder not selected");
                    return;
                }

                File folder = new File(path);

                if (!folder.exists() || !folder.isDirectory()) {
                    showMessage("Invalid folder");
                    return;
                }

                if (!folder.canRead()) {
                    showMessage("Cannot read selected folder");
                    return;
                }

                pathText.setText(folder.getAbsolutePath());

                executeOperation(folder);
            });

            chooser.show();

        } catch (Exception e) {

            showMessage(
                    "Storage chooser error: " + e.getMessage()
            );
        }
    }

    // --------------------------------
    // PYTHON
    // --------------------------------

    private void initializePython() {

        if (!Python.isStarted()) {
            Python.start(
                    new AndroidPlatform(getApplicationContext())
            );
        }
    }

    private void prepareFiles() {

        s.setVisibility(View.VISIBLE);

        executor.execute(() -> {

            try {

                initializePython();

                mainHandler.post(() -> {

                    if (activityDestroyed) return;

                    s.setVisibility(View.GONE);

                    obfuscateButton.setEnabled(true);
                    purgeButton.setEnabled(true);
                });

            } catch (Exception e) {

                mainHandler.post(() -> {

                    if (activityDestroyed) return;

                    s.setVisibility(View.GONE);

                    showMessage(
                            "Python initialization error: "
                                    + e.getMessage()
                    );
                });
            }
        });
    }

    // --------------------------------
    // SPINNER
    // --------------------------------

    private void setupSpinner() {

        ArrayList<String> levels = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            levels.add("Level: " + i);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                levels
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        q.setAdapter(adapter);
        q.setOnItemSelectedListener(this);
    }

    private void setupPreferences() {

        q.setSelection(getSavedLevel());

        o.setChecked(r.getBoolean("rec", true));
        p.setChecked(r.getBoolean("con", false));
    }

    private int getSavedLevel() {

        try {

            return Math.max(
                    0,
                    Math.min(
                            9,
                            Integer.parseInt(
                                    r.getString("level", "4")
                            )
                    )
            );

        } catch (Exception e) {
            return 4;
        }
    }

    // --------------------------------
    // RUN PYTHON OPERATION
    // --------------------------------

    private void executeOperation(File folder) {

        if (operationRunning) return;

        operationRunning = true;

        obfuscateButton.setEnabled(false);
        purgeButton.setEnabled(false);

        s.setVisibility(View.VISIBLE);

        int level = q.getSelectedItemPosition();

        boolean recursive = o.isChecked();
        boolean constructors = p.isChecked();

        String operation = u;
        String folderPath = folder.getAbsolutePath();

        executor.execute(() -> {

            try {

                initializePython();

                Python py = Python.getInstance();

                PyObject module = py.getModule("salt");

                PyObject result = module.callAttr(
                        "run",
                        folderPath,
                        operation,
                        level,
                        recursive,
                        constructors
                );

                String output = result.toString();

                mainHandler.post(() -> {

                    if (activityDestroyed) return;

                    resultText.setText(output);
                    finishOperation();
                });

            } catch (Exception e) {

                String error =
                        "Python error:\n" + e.getMessage();

                mainHandler.post(() -> {

                    if (activityDestroyed) return;

                    resultText.setText(error);
                    finishOperation();
                });
            }
        });
    }

    private void finishOperation() {

        operationRunning = false;

        s.setVisibility(View.GONE);

        obfuscateButton.setEnabled(true);
        purgeButton.setEnabled(true);
    }

    // --------------------------------
    // PREFERENCES
    // --------------------------------

    private void resetFields() {
        resultText.setText("");
        pathText.setText("");
    }

    private void savePreferences() {

        if (r == null || q == null
                || q.getSelectedItem() == null) {
            return;
        }

        r.edit()
                .putString(
                        "level",
                        String.valueOf(q.getSelectedItemPosition())
                )
                .putBoolean("rec", o.isChecked())
                .putBoolean("con", p.isChecked())
                .apply();
    }

    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onCheckedChanged(
            android.widget.CompoundButton button,
            boolean checked
    ) {
        savePreferences();
    }

    @Override
    public void onItemSelected(
            AdapterView<?> parent,
            View view,
            int position,
            long id
    ) {
        savePreferences();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    // --------------------------------
    // DESTROY
    // --------------------------------

    @Override
    protected void onDestroy() {

        activityDestroyed = true;

        super.onDestroy();

        if (executor != null) {
            executor.shutdown();
        }
    }
}
