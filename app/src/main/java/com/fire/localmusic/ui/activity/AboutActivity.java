package com.fire.localmusic.ui.activity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.fire.localmusic.App;
import com.fire.localmusic.R;

/**
 * Created by Remix on 2016/3/26.
 */
public class AboutActivity extends ToolbarActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.about_text)
    TextView mVersion;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ButterKnife.bind(this);
        try {
            PackageManager pm = App.getContext().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(App.getContext().getPackageName(), PackageManager.GET_ACTIVITIES);
            mVersion.setText("v" + pi.versionName);
        }catch (Exception ignored){

        }

        setUpToolbar(mToolBar, getString(R.string.about));
    }

    public void onResume() {
        MobclickAgent.onPageStart(AboutActivity.class.getSimpleName());
        super.onResume();
    }
    public void onPause() {
        MobclickAgent.onPageEnd(AboutActivity.class.getSimpleName());
        super.onPause();
    }

}
