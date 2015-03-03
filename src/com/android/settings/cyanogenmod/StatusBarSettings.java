/*
* Copyright (C) 2014 The CyanogenMod Project
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
package com.android.settings.cyanogenmod;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.SearchIndexableResource;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.Spannable;
import android.widget.EditText;
import android.view.View;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import java.util.Locale;

public class StatusBarSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, Indexable {

    private static final String TAG = "StatusBar";

    private static final String KEY_STATUS_BAR_CLOCK = "clock_style_pref";
    private static final String SMS_BREATH = "sms_breath";
    private static final String MISSED_CALL_BREATH = "missed_call_breath";
    private static final String VOICEMAIL_BREATH = "voicemail_breath";
    private static final String KEY_STATUS_BAR_NETWORK_ARROWS= "status_bar_show_network_activity";
    private static final String KEY_STATUS_BAR_GREETING = "status_bar_greeting";


    private PreferenceScreen mClockStyle;
    private SwitchPreference mSMSBreath;
    private SwitchPreference mMissedCallBreath;
    private SwitchPreference mVoicemailBreath;
    private SwitchPreference mNetworkArrows;
    private SwitchPreference mStatusBarGreeting;

    private String mCustomGreetingText = "";


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.status_bar_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mClockStyle = (PreferenceScreen) prefSet.findPreference(KEY_STATUS_BAR_CLOCK);
        updateClockStyleDescription();

        // Network arrows
        mNetworkArrows = (SwitchPreference) prefSet.findPreference(KEY_STATUS_BAR_NETWORK_ARROWS);
        mNetworkArrows.setChecked(Settings.CMREMIX.getInt(getActivity().getContentResolver(),
            Settings.CMREMIX.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 1) == 1);
        mNetworkArrows.setOnPreferenceChangeListener(this);
        int networkArrows = Settings.CMREMIX.getInt(getContentResolver(),
                Settings.CMREMIX.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 1);
        updateNetworkArrowsSummary(networkArrows);

        // Greeting
        mStatusBarGreeting = (SwitchPreference) findPreference(KEY_STATUS_BAR_GREETING);
        mCustomGreetingText = Settings.CMREMIX.getString(getActivity().getContentResolver(),
                Settings.CMREMIX.STATUS_BAR_GREETING);
        boolean greeting = mCustomGreetingText != null && !TextUtils.isEmpty(mCustomGreetingText);
        mStatusBarGreeting.setChecked(greeting);

        mSMSBreath = (SwitchPreference) findPreference(SMS_BREATH);
        mMissedCallBreath = (SwitchPreference) findPreference(MISSED_CALL_BREATH);
        mVoicemailBreath = (SwitchPreference) findPreference(VOICEMAIL_BREATH);

        Context context = getActivity();
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)) {
            mSMSBreath.setChecked(Settings.CMREMIX.getInt(resolver,
                    Settings.CMREMIX.KEY_SMS_BREATH, 0) == 1);
            mSMSBreath.setOnPreferenceChangeListener(this);

            mMissedCallBreath.setChecked(Settings.CMREMIX.getInt(resolver,
                    Settings.CMREMIX.KEY_MISSED_CALL_BREATH, 0) == 1);
            mMissedCallBreath.setOnPreferenceChangeListener(this);

            mVoicemailBreath.setChecked(Settings.CMREMIX.getInt(resolver,
                    Settings.CMREMIX.KEY_VOICEMAIL_BREATH, 0) == 1);
            mVoicemailBreath.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(mSMSBreath);
            prefSet.removePreference(mMissedCallBreath);
            prefSet.removePreference(mVoicemailBreath);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mSMSBreath) {
            boolean value = (Boolean) objValue;
            Settings.CMREMIX.putInt(resolver,
                    Settings.CMREMIX.KEY_SMS_BREATH, value ? 1 : 0);
        } else if (preference == mNetworkArrows) {
            Settings.CMREMIX.putInt(getContentResolver(),
                    Settings.CMREMIX.STATUS_BAR_SHOW_NETWORK_ACTIVITY,
                    (Boolean) objValue ? 1 : 0);
            int networkArrows = Settings.CMREMIX.getInt(getContentResolver(),
                    Settings.CMREMIX.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 1);
            updateNetworkArrowsSummary(networkArrows);
            return true;
        } else if (preference == mMissedCallBreath) {
            boolean value = (Boolean) objValue;
            Settings.CMREMIX.putInt(resolver,
                    Settings.CMREMIX.KEY_MISSED_CALL_BREATH, value ? 1 : 0);
        } else if (preference == mVoicemailBreath) {
            boolean value = (Boolean) objValue;
            Settings.CMREMIX.putInt(resolver,
                    Settings.CMREMIX.KEY_VOICEMAIL_BREATH, value ? 1 : 0);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
       if (preference == mStatusBarGreeting) {
           boolean enabled = mStatusBarGreeting.isChecked();
           if (enabled) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

                alert.setTitle(R.string.status_bar_greeting_title);
                alert.setMessage(R.string.status_bar_greeting_dialog);

                // Set an EditText view to get user input
                final EditText input = new EditText(getActivity());
                input.setText(mCustomGreetingText != null ? mCustomGreetingText : "Welcome to cmRemiX");
                alert.setView(input);
                alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = ((Spannable) input.getText()).toString();
                        Settings.CMREMIX.putString(getActivity().getContentResolver(),
                                Settings.CMREMIX.STATUS_BAR_GREETING, value);
                        updateCheckState(value);
                    }
                });
                alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
            } else {
                Settings.CMREMIX.putString(getActivity().getContentResolver(),
                        Settings.CMREMIX.STATUS_BAR_GREETING, "");
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateCheckState(String value) {
        if (value == null || TextUtils.isEmpty(value)) mStatusBarGreeting.setChecked(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateClockStyleDescription();
    }

    private void updateClockStyleDescription() {
        if (mClockStyle == null) {
            return;
        }
        if (Settings.System.getInt(getContentResolver(),
               Settings.System.STATUS_BAR_CLOCK, 1) == 1) {
            mClockStyle.setSummary(getString(R.string.enabled));
        } else {
            mClockStyle.setSummary(getString(R.string.disabled));
         }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.status_bar_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
            };

    private void updateNetworkArrowsSummary(int value) {
        String summary = value != 0
                ? getResources().getString(R.string.enabled)
                : getResources().getString(R.string.disabled);
        mNetworkArrows.setSummary(summary);
    }
}
