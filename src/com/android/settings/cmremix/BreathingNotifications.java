package com.android.settings.cmremix;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import android.provider.SearchIndexableResource;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.ArrayList;
import java.util.List;

public class BreathingNotifications extends SettingsPreferenceFragment implements Indexable {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.breathing_notifications);
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                    boolean enabled) {
            ArrayList<SearchIndexableResource> result =
                new ArrayList<SearchIndexableResource>();

            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.breathing_notifications;
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
