/*
 * Copyright (C) 2013 Slimroms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cmremix;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.ListPreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.ArrayList;
import java.util.List;

public class LockscreenShortcut extends SettingsPreferenceFragment implements
             Indexable, OnPreferenceChangeListener {

    private static final String PREF_LOCKSCREEN_SHORTCUTS_LAUNCH_TYPE =
            "lockscreen_shortcuts_launch_type";
    private static final String LOCKSCREEN_BOTTOM_SHORTCUTS = "lockscreen_bottom_shortcuts";

    private ListPreference mLockscreenShortcutsLaunchType;
    private SwitchPreference mLockscreenBottomShortcuts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_shortcut_fragment);

        PreferenceScreen prefSet = getPreferenceScreen();

        mLockscreenShortcutsLaunchType = (ListPreference) findPreference(
                PREF_LOCKSCREEN_SHORTCUTS_LAUNCH_TYPE);
        mLockscreenShortcutsLaunchType.setOnPreferenceChangeListener(this);

        // Lockscreen bottom shortcuts
        mLockscreenBottomShortcuts = (SwitchPreference) findPreference(LOCKSCREEN_BOTTOM_SHORTCUTS);
        if (mLockscreenBottomShortcuts != null) {
            boolean lockScreenBottomShortcutsEnabled = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCKSCREEN_BOTTOM_SHORTCUTS, 1) == 1;
            mLockscreenBottomShortcuts.setChecked(lockScreenBottomShortcutsEnabled);
            mLockscreenBottomShortcuts.setSummary(lockScreenBottomShortcutsEnabled
                    ? R.string.lockscreen_bottom_shortcuts_enabled :
                      R.string.lockscreen_bottom_shortcuts_disabled);
            mLockscreenBottomShortcuts.setOnPreferenceChangeListener(this);
        }

        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final ListView list = (ListView) view.findViewById(android.R.id.list);
        // our container already takes care of the padding
        if (list != null) {
            int paddingTop = list.getPaddingTop();
            int paddingBottom = list.getPaddingBottom();
            list.setPadding(0, paddingTop, 0, paddingBottom);
        }
        return view;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLockscreenShortcutsLaunchType) {
            Settings.CMREMIX.putInt(getContentResolver(),
                    Settings.CMREMIX.LOCKSCREEN_SHORTCUTS_LONGPRESS,
                    Integer.valueOf((String) newValue));
        } else if (preference == mLockscreenBottomShortcuts) {
            Settings.Secure.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.Secure.LOCKSCREEN_BOTTOM_SHORTCUTS,
                    (Boolean) newValue ? 1 : 0);
        }
        return true;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                    boolean enabled) {
            ArrayList<SearchIndexableResource> result =
                new ArrayList<SearchIndexableResource>();

            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.lockscreen_shortcut_fragment;
            result.add(sir);

            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            ArrayList<String> result = new ArrayList<String>();
            return result;
        }
    };
}
