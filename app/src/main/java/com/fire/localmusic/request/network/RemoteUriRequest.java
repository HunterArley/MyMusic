package com.fire.localmusic.request.network;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.fire.localmusic.bean.netease.NSearchRequest;
import com.fire.localmusic.request.ImageUriRequest;
import com.fire.localmusic.request.RequestConfig;

/**
 * Created by Remix on 2017/12/10.
 */

public abstract class RemoteUriRequest extends ImageUriRequest<Bitmap> {
    private NSearchRequest mRequest;

    public RemoteUriRequest(@NonNull NSearchRequest request,@NonNull RequestConfig config){
        super(config);
        mRequest = request;
    }

    @Override
    public void load() {
        getThumbBitmapObservable(mRequest)
                .compose(RxUtil.applyScheduler())
                .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
    }


}
