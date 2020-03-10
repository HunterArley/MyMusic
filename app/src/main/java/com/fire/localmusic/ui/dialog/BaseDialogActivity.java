package com.fire.localmusic.ui.dialog;

import android.os.Bundle;
import android.view.View;

import com.fire.localmusic.R;
import com.fire.localmusic.theme.ThemeStore;
import com.fire.localmusic.ui.activity.BaseActivity;

/**
 * Created by Remix on 2016/3/16.
 */


public abstract class BaseDialogActivity extends BaseActivity {
    protected <T extends View> T findView(int id){
        return (T)findViewById(id);
    }

    @Override
    protected void setUpTheme() {
        setTheme(ThemeStore.isDay() ? R.style.Dialog_DayTheme : R.style.Dialog_NightTheme);
    }

    @Override
    protected void setStatusBar() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");
    }

}
