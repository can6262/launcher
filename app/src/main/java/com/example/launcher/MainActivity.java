package com.example.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    static class AppItem {
        String label;
        String pkg;
        Drawable icon;
        Intent launch;
    }

    private final List<AppItem> all = new ArrayList<>();
    private final List<AppItem> shown = new ArrayList<>();
    private AppAdapter adapter;
    private EditText search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        search = findViewById(R.id.search);
        GridView grid = findViewById(R.id.grid);
        adapter = new AppAdapter();
        grid.setAdapter(adapter);

        grid.setOnItemClickListener((p, v, pos, id) -> {
            startActivity(shown.get(pos).launch);
        });
        grid.setOnItemLongClickListener((p, v, pos, id) -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + shown.get(pos).pkg));
            startActivity(i);
            return true;
        });

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { filter(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        search.setText("");
    }

    @Override
    public void onBackPressed() {
        // Ana ekranda geri tuşu bir şey yapmaz
    }

    private void loadApps() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN, null);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = pm.queryIntentActivities(main, 0);
            List<AppItem> result = new ArrayList<>();
            for (ResolveInfo ri : infos) {
                AppItem it = new AppItem();
                it.label = ri.loadLabel(pm).toString();
                it.pkg = ri.activityInfo.packageName;
                it.icon = ri.loadIcon(pm);
                it.launch = pm.getLaunchIntentForPackage(it.pkg);
                if (it.launch != null) result.add(it);
            }
            Collections.sort(result, (a, b) ->
                    a.label.toLowerCase(Locale.getDefault())
                            .compareTo(b.label.toLowerCase(Locale.getDefault())));
            runOnUiThread(() -> {
                all.clear();
                all.addAll(result);
                filter(search.getText().toString());
            });
        }).start();
    }

    private void filter(String q) {
        String s = q.toLowerCase(Locale.getDefault());
        shown.clear();
        for (AppItem a : all) {
            if (s.isEmpty() || a.label.toLowerCase(Locale.getDefault()).contains(s)) shown.add(a);
        }
        adapter.notifyDataSetChanged();
    }

    private class AppAdapter extends BaseAdapter {
        public int getCount() { return shown.size(); }
        public Object getItem(int i) { return shown.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(int i, View convert, ViewGroup parent) {
            if (convert == null) {
                convert = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.item_app, parent, false);
            }
            AppItem a = shown.get(i);
            ((ImageView) convert.findViewById(R.id.icon)).setImageDrawable(a.icon);
            ((TextView) convert.findViewById(R.id.label)).setText(a.label);
            return convert;
        }
    }
}
