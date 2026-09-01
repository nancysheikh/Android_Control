package net.kollnig.missioncontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

import eu.faircode.netguard.GlideApp;
import eu.faircode.netguard.Rule;
import eu.faircode.netguard.ServiceSinkhole;
import net.kollnig.missioncontrol.data.BlockingMode;
import net.kollnig.missioncontrol.data.InternetBlocklist;
import net.kollnig.missioncontrol.details.TrackersListAdapter;

/**
 * Row: app icon + app name + a switch that turns per-app ad blocking on/off.
 * Reuses the same "block_all_ads" SharedPreferences (TrackersListAdapter.PREF_BLOCK_ALL_ADS)
 * that the details-screen bottom sheet already writes to, so both entry points
 * stay in agreement about an app's ad-blocker state.
 */
public class AdapterAdBlocker extends RecyclerView.Adapter<AdapterAdBlocker.ViewHolder> {
    private final Context appContext;
    private final LayoutInflater inflater;
    private final SharedPreferences adBlockPrefs;
    private final SharedPreferences applyPrefs;
    private final SharedPreferences trackerProtectPrefs;
    private final RequestOptions glideOptions;
    private List<Rule> rules = new ArrayList<>();

    public AdapterAdBlocker(Context context) {
        this.appContext = context.getApplicationContext();
        this.inflater = LayoutInflater.from(context);
        this.adBlockPrefs = appContext.getSharedPreferences(
                TrackersListAdapter.PREF_BLOCK_ALL_ADS, Context.MODE_PRIVATE);
        this.applyPrefs = appContext.getSharedPreferences("apply", Context.MODE_PRIVATE);
        this.trackerProtectPrefs = appContext.getSharedPreferences("tracker_protect", Context.MODE_PRIVATE);
        this.glideOptions = new RequestOptions().format(DecodeFormat.PREFER_RGB_565);
    }

    public void set(List<Rule> newRules) {
        this.rules = newRules != null ? newRules : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_ad_blocker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Rule rule = rules.get(position);
        final Context context = holder.itemView.getContext();

        if (rule.icon <= 0) {
            holder.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon);
        } else {
            Uri uri = Uri.parse("android.resource://" + rule.packageName + "/" + rule.icon);
            GlideApp.with(context)
                    .applyDefaultRequestOptions(glideOptions)
                    .load(uri)
                    .into(holder.ivIcon);
        }

        holder.tvName.setText(rule.name);

        // Avoid re-triggering the listener while we set the initial state.
        holder.switchAdBlock.setOnCheckedChangeListener(null);
        holder.switchAdBlock.setChecked(adBlockPrefs.getBoolean(rule.packageName, false));
        holder.switchAdBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed())
                return;
            setAdBlockingForApp(context, rule, isChecked);
        });
    }

    private void setAdBlockingForApp(Context context, Rule rule, boolean block) {
        adBlockPrefs.edit().putBoolean(rule.packageName, block).apply();

        if (block) {
            // Mirrors AppProtectionState.PROTECTED: app stays included, tracker
            // protection on, internet not blocked. Keeps this entry point in
            // agreement with the "Block All Ads" option in the details sheet.
            applyPrefs.edit().putBoolean(rule.packageName, true).apply();
            BlockingMode.clearAutoExcludedApp(context, rule.packageName);
            trackerProtectPrefs.edit().putBoolean(rule.packageName, true).apply();
            InternetBlocklist.getInstance(context).unblock(context, rule.uid);
            Toast.makeText(context, "Ad Blocker activated for " + rule.name, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Ad Blocker turned off for " + rule.name, Toast.LENGTH_SHORT).show();
        }

        AsyncTask.execute(() -> {
            Rule.clearCache(context);
            ServiceSinkhole.reload("ad blocker toggled", context, false);
        });
    }

    @Override
    public int getItemCount() {
        return rules.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvName;
        final MaterialSwitch switchAdBlock;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivAdIcon);
            tvName = itemView.findViewById(R.id.tvAdName);
            switchAdBlock = itemView.findViewById(R.id.switchAdBlock);
        }
    }
}
