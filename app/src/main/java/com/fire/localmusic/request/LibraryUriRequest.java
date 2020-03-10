package com.fire.localmusic.request;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import com.fire.localmusic.bean.netease.NSearchRequest;
import com.fire.localmusic.request.network.RxUtil;
import com.fire.localmusic.util.LogUtil;

/**
 * Created by Remix on 2017/12/4.
 */

public class LibraryUriRequest extends ImageUriRequest<String> {
    protected SimpleDraweeView mImage;
    NSearchRequest mRequest;
    public LibraryUriRequest(@NonNull SimpleDraweeView image, @NonNull NSearchRequest request, RequestConfig config) {
        super(config);
        mImage = image;
        mRequest = request;
    }

    @Override
    public void onError(String errMsg){
        mImage.setImageURI(Uri.EMPTY);
        LogUtil.d("UriRequest","Error: " + errMsg);
    }

    @Override
    public void onSuccess(String result) {
        ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(result));
        if(mConfig.isResize()){
            imageRequestBuilder.setResizeOptions(ResizeOptions.forDimensions(mConfig.getWidth(),mConfig.getHeight()));
        }
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequestBuilder.build())
                .setOldController(mImage.getController())
                .build();

        mImage.setController(controller);
    }

    @Override
    public void load() {
        getThumbObservable(mRequest)
                .compose(RxUtil.applyScheduler())
                .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
    }
}
