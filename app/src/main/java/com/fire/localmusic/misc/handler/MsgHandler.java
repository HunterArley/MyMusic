package com.fire.localmusic.misc.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.reflect.Method;

import com.fire.localmusic.App;
import com.fire.localmusic.util.ToastUtil;

/**
 * Created by Remix on 2017/11/16.
 */
public class MsgHandler extends Handler {
    private Method mMethod;
    private Object mFrom;

    public MsgHandler(Looper looper, Object from, Class clazz) {
        super(looper);
        mFrom = from;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(OnHandleMessage.class)) {
                mMethod = method;
            }
        }
    }

    public MsgHandler(Object from, Class clazz) {
        this(Looper.getMainLooper(), from, clazz);
    }

    public MsgHandler(Object from){
        this(from,from.getClass());
    }

    @Override
    public void handleMessage(Message msg) {
        if (mMethod == null) {
            return;
        }
        try {
            mMethod.invoke(mFrom, msg);
        } catch (Exception e) {
            ToastUtil.show(App.getContext(),"调用Method失败:" + e.toString());
        }
    }

    public void remove(){
        removeCallbacksAndMessages(null);
    }
}
