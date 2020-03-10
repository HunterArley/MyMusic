package com.fire.localmusic.menu;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.fire.localmusic.R;
import com.fire.localmusic.service.MusicService;
import com.fire.localmusic.util.Constants;

/**
 * Created by Remix on 2015/12/3.
 */

/**
 * 播放控制
 */
public class CtrlButtonListener implements View.OnClickListener {
    private Context context;
    public CtrlButtonListener(Context context) {
        this.context = context;
    }
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(MusicService.ACTION_CMD);
        switch (v.getId()) {
            case R.id.lockscreen_prev:
            case R.id.playbar_prev:
                intent.putExtra("Control", Constants.PREV);
                break;
            case R.id.lockscreen_next:
            case R.id.playbar_next:
                intent.putExtra("Control", Constants.NEXT);
                break;
            case R.id.lockscreen_play:
            case R.id.playbar_play:
                intent.putExtra("Control", Constants.TOGGLE);
                break;
        }
        context.sendBroadcast(intent);
    }
}
