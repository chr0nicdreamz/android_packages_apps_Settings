/*
 * Copyright (C) 2015 crDroid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.cmremix;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.IActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Point;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.ListPreference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.provider.Settings;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.provider.Settings.SettingNotFoundException;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.util.cm.QSUtils;
import com.android.settings.cmremix.utils.AbstractAsyncSuCMDProcessor;
import com.android.settings.cmremix.utils.CMDProcessor;
import com.android.settings.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "DisplaySettings";

    private static final int DIALOG_DENSITY = 0;
    private static final int DIALOG_DENSITY_WARNING = 1;

    private static final String KEY_LCD_DENSITY = "lcd_density";
    private static final String DISABLE_TORCH_ON_SCREEN_OFF = "disable_torch_on_screen_off";
    private static final String DISABLE_TORCH_ON_SCREEN_OFF_DELAY = "disable_torch_on_screen_off_delay";
    private static final int REQUEST_PICK_BOOT_ANIMATION = 201;
    private static final String PREF_CUSTOM_BOOTANIM = "custom_bootanimation";
    private static final String BOOTANIMATION_SYSTEM_PATH = "/system/media/bootanimation.zip";
    private static final String BACKUP_PATH = new File(Environment
	    .getExternalStorageDirectory(), "/CMRemix").getAbsolutePath();

    private ListPreference mLcdDensityPreference;
    private SwitchPreference mTorchOff;
    private ListPreference mTorchOffDelay;
    private Preference mCustomBootAnimation;
    private ImageView mView;
    private TextView mError;
    private AlertDialog mCustomBootAnimationDialog;
    private AnimationDrawable mAnimationPart1;
    private AnimationDrawable mAnimationPart2;
    private String mErrormsg;
    private String mBootAnimationPath;

    protected Context mContext;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.cmremix_display_settings);
        ContentResolver resolver = getActivity().getContentResolver();
        Activity activity = getActivity();
        PreferenceScreen prefSet = getPreferenceScreen();

	//create the cmRemiX folder if it has not yet been created
	File CMREMIX_FOLDER = new File(BACKUP_PATH);
	if (!CMREMIX_FOLDER.exists()) {
	    CMREMIX_FOLDER.mkdir();
	}

        mContext = getActivity().getApplicationContext();
        int newDensityValue;

        mTorchOff = (SwitchPreference) prefSet.findPreference(DISABLE_TORCH_ON_SCREEN_OFF);
        mTorchOffDelay = (ListPreference) prefSet.findPreference(DISABLE_TORCH_ON_SCREEN_OFF_DELAY);
        int torchOffDelay = Settings.System.getInt(resolver,
                Settings.System.DISABLE_TORCH_ON_SCREEN_OFF_DELAY, 10);
        mTorchOffDelay.setValue(String.valueOf(torchOffDelay));
        mTorchOffDelay.setSummary(mTorchOffDelay.getEntry());
        mTorchOffDelay.setOnPreferenceChangeListener(this);

        if (!QSUtils.deviceSupportsFlashLight(activity)) {
            prefSet.removePreference(mTorchOff);
            prefSet.removePreference(mTorchOffDelay);
        }

        mLcdDensityPreference = (ListPreference) findPreference(KEY_LCD_DENSITY);
        int defaultDensity = DisplayMetrics.DENSITY_DEVICE_DEFAULT;
        String[] densityEntries = new String[9];
        for (int idx = 0; idx < 8; ++idx) {
            int pct = (75 + idx*5);
            densityEntries[idx] = Integer.toString(defaultDensity * pct / 100);
        }
        densityEntries[8] = getString(R.string.custom_density);
        int currentDensity = DisplayMetrics.DENSITY_PREFERRED;
        mLcdDensityPreference.setEntries(densityEntries);
        mLcdDensityPreference.setEntryValues(densityEntries);
        mLcdDensityPreference.setValue(String.valueOf(currentDensity));
        mLcdDensityPreference.setOnPreferenceChangeListener(this);
        updateLcdDensityPreferenceDescription(currentDensity);

        // Custom bootanimation
        mCustomBootAnimation = findPreference(PREF_CUSTOM_BOOTANIM);

        resetBootAnimation();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mCustomBootAnimation) {
            openBootAnimationDialog();
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();
        if (KEY_LCD_DENSITY.equals(key)) {
            String strValue = (String) newValue;
            if (strValue.equals(getResources().getString(R.string.custom_density))) {
                showDialog(DIALOG_DENSITY);
            } else {
                int value = Integer.parseInt((String) newValue);
                writeLcdDensityPreference(value);
                updateLcdDensityPreferenceDescription(value);
            }
        } else if (preference == mTorchOffDelay) {
            int torchOffDelay = Integer.valueOf((String) newValue);
            int index = mTorchOffDelay.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.DISABLE_TORCH_ON_SCREEN_OFF_DELAY, torchOffDelay);
            mTorchOffDelay.setSummary(mTorchOffDelay.getEntries()[index]);
            return true;
        }
        return false;
    }

    /**
     * Resets boot animation path. Essentially clears temporary-set boot animation
     * set by the user from the dialog.
     *
     * @return returns true if a boot animation exists (user or system). false otherwise.
     */
    private boolean resetBootAnimation() {
        boolean bootAnimationExists = false;
        if (new File(BOOTANIMATION_SYSTEM_PATH).exists()) {
            mBootAnimationPath = BOOTANIMATION_SYSTEM_PATH;
            bootAnimationExists = true;
        } else {
            mBootAnimationPath = "";
        }
        return bootAnimationExists;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_BOOT_ANIMATION) {
                if (data == null) {
                    //Nothing returned by user, probably pressed back button in file manager
                    return;
                }
                mBootAnimationPath = data.getData().getPath();
                openBootAnimationDialog();
            }
        }
    }

    private void openBootAnimationDialog() {
        Log.e(TAG, "boot animation path: " + mBootAnimationPath);
        if (mCustomBootAnimationDialog != null) {
            mCustomBootAnimationDialog.cancel();
            mCustomBootAnimationDialog = null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.bootanimation_preview);
        if (!mBootAnimationPath.isEmpty()
                && (!BOOTANIMATION_SYSTEM_PATH.equalsIgnoreCase(mBootAnimationPath))) {
            builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    installBootAnim(dialog, mBootAnimationPath);
                    resetBootAnimation();
                }
            });
        }
        builder.setNeutralButton(R.string.set_custom_bootanimation,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        PackageManager packageManager = getActivity().getPackageManager();
                        Intent test = new Intent(Intent.ACTION_GET_CONTENT);
                        test.setType("file/*");
                        List<ResolveInfo> list = packageManager.queryIntentActivities(test,
                                PackageManager.GET_ACTIVITIES);
                        if (!list.isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                            intent.setType("file/*");
                            startActivityForResult(intent, REQUEST_PICK_BOOT_ANIMATION);
                        } else {
                            //No app installed to handle the intent - file explorer required
                            Toast.makeText(mContext, R.string.install_file_manager_error,
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });
        builder.setNegativeButton(com.android.internal.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        resetBootAnimation();
                        dialog.dismiss();
                    }
                });
        LayoutInflater inflater =
                (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_bootanimation_preview,
                (ViewGroup) getActivity()
                        .findViewById(R.id.bootanimation_layout_root));
        mError = (TextView) layout.findViewById(R.id.textViewError);
        mView = (ImageView) layout.findViewById(R.id.imageViewPreview);
        mView.setVisibility(View.GONE);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mView.setLayoutParams(new LinearLayout.LayoutParams(size.x / 2, size.y / 2));
        mError.setText(R.string.creating_preview);
        builder.setView(layout);
        mCustomBootAnimationDialog = builder.create();
        mCustomBootAnimationDialog.setOwnerActivity(getActivity());
        mCustomBootAnimationDialog.show();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                createPreview(mBootAnimationPath);
            }
        });
        thread.start();
    }

    private void createPreview(String path) {
        File zip = new File(path);
        ZipFile zipfile = null;
        String desc = "";
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            zipfile = new ZipFile(zip);
            ZipEntry ze = zipfile.getEntry("desc.txt");
            inputStream = zipfile.getInputStream(ze);
            inputStreamReader = new InputStreamReader(inputStream);
            StringBuilder sb = new StringBuilder(0);
            bufferedReader = new BufferedReader(inputStreamReader);
            String read = bufferedReader.readLine();
            while (read != null) {
                sb.append(read);
                sb.append('\n');
                read = bufferedReader.readLine();
            }
            desc = sb.toString();
        } catch (Exception handleAllException) {
            mErrormsg = getActivity().getString(R.string.error_reading_zip_file);
            errorHandler.sendEmptyMessage(0);
            return;
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                // we tried
            }
            try {
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                // we tried
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                // moving on...
            }
        }

        String[] info = desc.replace("\\r", "").split("\\n");
        // ignore first two ints height and width
        int delay = Integer.parseInt(info[0].split(" ")[2]);
        String partName1 = info[1].split(" ")[3];
        String partName2;
        try {
            if (info.length > 2) {
                partName2 = info[2].split(" ")[3];
            } else {
                partName2 = "";
            }
        } catch (Exception e) {
            partName2 = "";
        }

        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 4;
        mAnimationPart1 = new AnimationDrawable();
        mAnimationPart2 = new AnimationDrawable();
        try {
            for (Enumeration<? extends ZipEntry> enumeration = zipfile.entries();
                 enumeration.hasMoreElements(); ) {
                ZipEntry entry = enumeration.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String partname = entry.getName().split("/")[0];
                if (partName1.equalsIgnoreCase(partname)) {
                    InputStream partOneInStream = null;
                    try {
                        partOneInStream = zipfile.getInputStream(entry);
                        mAnimationPart1.addFrame(new BitmapDrawable(getResources(),
                                BitmapFactory.decodeStream(partOneInStream,
                                        null, opt)), delay);
                    } finally {
                        if (partOneInStream != null) {
                            partOneInStream.close();
                        }
                    }
                } else if (partName2.equalsIgnoreCase(partname)) {
                    InputStream partTwoInStream = null;
                    try {
                        partTwoInStream = zipfile.getInputStream(entry);
                        mAnimationPart2.addFrame(new BitmapDrawable(getResources(),
                                BitmapFactory.decodeStream(partTwoInStream,
                                        null, opt)), delay);
                    } finally {
                        if (partTwoInStream != null) {
                            partTwoInStream.close();
                        }
                    }
                }
            }
        } catch (IOException e1) {
            mErrormsg = getActivity().getString(R.string.error_creating_preview);
            errorHandler.sendEmptyMessage(0);
            return;
        }

        if (!partName2.isEmpty()) {
            Log.d(TAG, "Multipart Animation");
            mAnimationPart1.setOneShot(false);
            mAnimationPart2.setOneShot(false);
            mAnimationPart1.setOnAnimationFinishedListener(
                    new AnimationDrawable.OnAnimationFinishedListener() {
                        @Override
                        public void onAnimationFinished() {
                            Log.d(TAG, "First part finished");
                            mView.setImageDrawable(mAnimationPart2);
                            mAnimationPart1.stop();
                            mAnimationPart2.start();
                        }
                    });
        } else {
            mAnimationPart1.setOneShot(false);
        }
        finishedHandler.sendEmptyMessage(0);
    }

    private Handler errorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mView.setVisibility(View.GONE);
            mError.setText(mErrormsg);
        }
    };

    private Handler finishedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mView.setImageDrawable(mAnimationPart1);
            mView.setVisibility(View.VISIBLE);
            mError.setVisibility(View.GONE);
            mAnimationPart1.start();
        }
    };

    private void installBootAnim(DialogInterface dialog, String bootAnimationPath) {
        DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
        Date date = new Date();
        String current = (dateFormat.format(date));
        new AbstractAsyncSuCMDProcessor() {
            @Override
            protected void onPostExecute(String result) {
            }
        }.execute("mount -o rw,remount /system",
                "cp -f /system/media/bootanimation.zip " + BACKUP_PATH + "/bootanimation_backup_" + current + ".zip",
                "cp -f " + bootAnimationPath + " /system/media/bootanimation.zip",
                "chmod 644 /system/media/bootanimation.zip",
                "mount -o ro,remount /system");
    }

    private void updateLcdDensityPreferenceDescription(int currentDensity) {
        ListPreference preference = mLcdDensityPreference;
        String summary;
        if (currentDensity < 10 || currentDensity >= 1000) {
            // Unsupported value 
            summary = "";
        }
        else {
            summary = Integer.toString(currentDensity) + " DPI";
        }
        preference.setSummary(summary);
    }

    public void writeLcdDensityPreference(int value) {
        // Set the value clicked on the list
        try {
            SystemProperties.set("persist.sys.lcd_density", Integer.toString(value));
        }
        catch (Exception e) {
            Log.w(TAG, "Unable to save LCD density");
        }
        // Show a dialog before restart
        // and let the user know of it
        showDialogInner(DIALOG_DENSITY_WARNING);

    }

    // Restart the system to apply changes
    static void systemRestart() {
        try {
            final IActivityManager am = ActivityManagerNative.asInterface(ServiceManager.checkService("activity"));
            if (am != null) {
                am.restart();
            }
        }
        catch (RemoteException e) {
            Log.e(TAG, "Failed to restart");
        }
    }

    public Dialog onCreateDialog(int dialogId) {
        LayoutInflater factory = LayoutInflater.from(mContext);

        switch (dialogId) {
            case DIALOG_DENSITY:
                final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
                return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.custom_density_dialog_title)
                .setMessage(getResources().getString(R.string.custom_density_dialog_summary))
                .setView(textEntryView)
                .setPositiveButton(getResources().getString(R.string.set_custom_density_set), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        EditText dpi = (EditText) textEntryView.findViewById(R.id.dpi_edit);
                        Editable text = dpi.getText();
                        Log.i(TAG, text.toString());
                        String editText = dpi.getText().toString();
                        // Set the value of the text box
                        try {
                            SystemProperties.set("persist.sys.lcd_density", editText);
                        }
                        catch (Exception e) {
                            Log.w(TAG, "Unable to save LCD density");
                        }
                        // Show a dialog before restart
                        // and let the user know of it
                        showDialogInner(DIALOG_DENSITY_WARNING);

                    }

                })
                .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                })
                .create();
        }
        return null;
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DIALOG_DENSITY_WARNING:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.attention)
                    .setMessage(R.string.custom_density_warning)
                    .setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // If canceled, set the density value to null avoiding
                            // the storage of the clicked value and forward change
                            // to it in a next restart of the system 
                            try {
                                SystemProperties.set("persist.sys.lcd_density", null);
                            } catch (Exception e) {
                                Log.w(TAG, "Unable to save LCD density");
                            }

                            dialog.cancel();
                        }
                    })
                    .setPositiveButton(R.string.dialog_restart,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // If resatrt is the choosen one do it and apply the value
                            systemRestart();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }
    }
}
