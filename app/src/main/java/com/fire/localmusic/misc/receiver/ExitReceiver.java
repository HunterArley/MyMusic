package com.fire.localmusic.misc.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.umeng.analytics.MobclickAgent;

import com.fire.localmusic.db.DBManager;
import com.fire.localmusic.helper.ShakeDetector;
import com.fire.localmusic.misc.manager.ActivityManager;
import com.fire.localmusic.misc.manager.ServiceManager;

/**
 * Created by taeja on 16-2-16.
 */

/**
 * 接受程序退出的广播
 */
public class ExitReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            //关闭数据库
            DBManager.getInstance().closeDataBase();
            //停止所有service
            ServiceManager.StopAll();
            //关闭通知
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
            //停止摇一摇
            ShakeDetector.getInstance().stopListen();
            MobclickAgent.onKillProcess(context);
            //关闭所有activity
            ActivityManager.FinishAll();
            System.exit(0);
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
