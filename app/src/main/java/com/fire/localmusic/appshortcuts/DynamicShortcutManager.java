package com.fire.localmusic.appshortcuts;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import com.fire.localmusic.appshortcuts.shortcuttype.ContinuePlayShortcutType;
import com.fire.localmusic.appshortcuts.shortcuttype.LastAddedShortcutType;
import com.fire.localmusic.appshortcuts.shortcuttype.MyLoveShortcutType;
import com.fire.localmusic.appshortcuts.shortcuttype.ShuffleShortcutType;

/**
 * Created by Remix on 2017/11/1.
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
public class DynamicShortcutManager extends ContextWrapper {
    private Context mContext;
    private ShortcutManager mShortcutManger;

    public DynamicShortcutManager(Context base) {
        super(base);
        mContext = base;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            mShortcutManger = getSystemService(ShortcutManager.class);
        }
    }

    public void setUpShortcut(){
        if(mShortcutManger.getDynamicShortcuts().size() == 0){
            mShortcutManger.setDynamicShortcuts(getDefaultShortcut());
        }
    }

    @NonNull
    private List<ShortcutInfo> getDefaultShortcut() {
        return Arrays.asList(new ContinuePlayShortcutType(mContext).getShortcutInfo(),
                new LastAddedShortcutType(mContext).getShortcutInfo(),
                new MyLoveShortcutType(mContext).getShortcutInfo(),
                new ShuffleShortcutType(mContext).getShortcutInfo());
    }

    public void updateContinueShortcut(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            mShortcutManger.updateShortcuts(Arrays.asList(new ContinuePlayShortcutType(mContext).getShortcutInfo()));
    }
}
