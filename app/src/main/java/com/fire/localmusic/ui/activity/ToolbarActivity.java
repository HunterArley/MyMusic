package com.fire.localmusic.ui.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.fire.localmusic.R;
import com.fire.localmusic.theme.Theme;
import com.fire.localmusic.theme.ThemeStore;
import com.fire.localmusic.util.ColorUtil;


/**
 * Created by taeja on 16-3-15.
 */
@SuppressLint("Registered")
public class ToolbarActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setUpToolbar(Toolbar toolbar, String title){
        toolbar.setTitle(title);

        setSupportActionBar(toolbar);
        //主题颜色
        int themeColor = ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.black : R.color.white);
        toolbar.setNavigationIcon(Theme.TintDrawable(R.drawable.common_btn_back,themeColor));
        toolbar.setTitleTextColor(themeColor);

        toolbar.setNavigationOnClickListener(v -> onClickNavigation());
//        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                switch (item.getItemId()) {
//                    case R.id.toolbar_search:
//                        startActivity(new Intent(mContext, SearchActivity.class));
//                        break;
//                    case R.id.toolbar_timer:
//                        startActivity(new Intent(mContext, TimerDialog.class));
//                        break;
//                }
//                return true;
//            }
//        });
    }

    protected void onClickNavigation(){
        finish();
    }

}
