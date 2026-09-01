package net.kollnig.missioncontrol;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import eu.faircode.netguard.Rule;

/**
 * Standalone screen listing every app with a dedicated "Block ads for this app"
 * toggle, separate from the per-app details bottom sheet in TrackersListAdapter.
 */
public class ActivityAdBlocker extends AppCompatActivity {
    private AdapterAdBlocker adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_blocker);

        Toolbar toolbar = findViewById(R.id.toolbarAdBlocker);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ad Blocker");
        }

        RecyclerView rv = findViewById(R.id.rvAdBlockerApps);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterAdBlocker(this);
        rv.setAdapter(adapter);

        loadApps();
    }

    private void loadApps() {
        final Context context = getApplicationContext();
        new AsyncTask<Void, Void, List<Rule>>() {
            @Override
            protected List<Rule> doInBackground(Void... voids) {
                return Rule.getRules(false, true, context);
            }

            @Override
            protected void onPostExecute(List<Rule> rules) {
                if (!isFinishing() && !isDestroyed())
                    adapter.set(rules);
            }
        }.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
