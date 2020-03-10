package com.fire.localmusic;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.multidex.MultiDexApplication;

import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.pushtorefresh.storio3.sqlite.StorIOSQLite;
import com.pushtorefresh.storio3.sqlite.impl.DefaultStorIOSQLite;
import com.squareup.leakcanary.LeakCanary;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.update.BmobUpdateAgent;
import com.fire.localmusic.BuildConfig;
import com.fire.localmusic.appshortcuts.DynamicShortcutManager;
import com.fire.localmusic.bean.DbModel;
import com.fire.localmusic.db.DBManager;
import com.fire.localmusic.db.DBOpenHelper;
import com.fire.localmusic.db.DbModelSQLiteTypeMapping;
import com.fire.localmusic.db.TableInfo;
import com.fire.localmusic.misc.cache.DiskCache;
import com.fire.localmusic.theme.ThemeStore;
import com.fire.localmusic.util.ColorUtil;
import com.fire.localmusic.util.CrashHandler;
import com.fire.localmusic.util.ImageUriUtil;
import com.fire.localmusic.util.MediaStoreUtil;
import com.fire.localmusic.util.PermissionUtil;
import com.fire.localmusic.util.PlayListUtil;
import com.fire.localmusic.util.SPUtil;
import com.fire.localmusic.util.Util;

/**
 * Created by Remix on 16-3-16.
 */

public class App extends MultiDexApplication{
    private static Context mContext;
    private static App mApp;

    //是否是googlePlay版本
    public static boolean IS_GP;
    private StorIOSQLite mStorIOSQLite;
    private DBOpenHelper mDbOpenHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mApp = this;
        IS_GP = "google".equalsIgnoreCase(Util.getAppMetaData("UMENG_CHANNEL"));

        initUtil();
        initTheme();

        //友盟
        UMConfigure.init(this,null,null,UMConfigure.DEVICE_TYPE_PHONE,null);
        MobclickAgent.setCatchUncaughtExceptions(true);
        UMConfigure.setLogEnabled(BuildConfig.DEBUG);

        //根据渠道加载其他第三方库
        loadThirdParty();

        //禁止默认的页面统计方式
        MobclickAgent.openActivityDurationTrack(false);
        //异常捕获
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
        //检测内存泄漏
        if(!LeakCanary.isInAnalyzerProcess(this)){
            LeakCanary.install(this);
        }
        //AppShortcut
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            new DynamicShortcutManager(this).setUpShortcut();
        }

        //兼容性
        if(SPUtil.getValue(this,SPUtil.SETTING_KEY.SETTING_NAME,"CategoryRebuild",true)){
            SPUtil.putValue(this,SPUtil.SETTING_KEY.SETTING_NAME,"CategoryRebuild",false);
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY,"");
        }
    }

    public static App getInstance() {
        return mApp;
    }

    private void initUtil() {
        //初始化工具类
        mDbOpenHelper = new DBOpenHelper(this);
        DBManager.initialInstance(mDbOpenHelper);
        initSqlite(mDbOpenHelper);

        PermissionUtil.setContext(this);
        MediaStoreUtil.setContext(this);
        Util.setContext(this);
        ImageUriUtil.setContext(this);
        DiskCache.init(this);
        ColorUtil.setContext(this);
        PlayListUtil.setContext(this);
        final int cacheSize = (int)(Runtime.getRuntime().maxMemory() / 8);
        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setBitmapMemoryCacheParamsSupplier(() -> new MemoryCacheParams(cacheSize, Integer.MAX_VALUE,cacheSize,Integer.MAX_VALUE, 2 * ByteConstants.MB))
                .setBitmapsConfig(Bitmap.Config.RGB_565)
                .setDownsampleEnabled(true)
                .build();
        Fresco.initialize(this,config);
    }
    public StorIOSQLite initSqlite(DBOpenHelper mDbOpenHelper) {
        DefaultStorIOSQLite.CompleteBuilder completeBuilder = DefaultStorIOSQLite.builder()
                .sqliteOpenHelper(mDbOpenHelper)
                .addTypeMapping(DbModel.class, new DbModelSQLiteTypeMapping());
        mStorIOSQLite = TableInfo.buildTypeMapping(completeBuilder);
        return mStorIOSQLite;
    }

    public StorIOSQLite getStorIOSQLite() {
        if (mStorIOSQLite == null) {
            return initSqlite(mDbOpenHelper);
        }
        return mStorIOSQLite;
    }



    /**
     * 初始化主题
     */
    private void initTheme() {
        ThemeStore.THEME_MODE = ThemeStore.loadThemeMode();
        ThemeStore.THEME_COLOR = ThemeStore.loadThemeColor();

        ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColorRes();
        ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColorRes();
    }

    public static Context getContext(){
        return mContext;
    }

    private void loadThirdParty() {
        //bomb
        Bmob.initialize(this, "0c070110fffa9e88a1362643fb9d4d64");
//        BmobUpdateAgent.setUpdateOnlyWifi(false);
//        BmobUpdateAgent.update(this);
    }
}
