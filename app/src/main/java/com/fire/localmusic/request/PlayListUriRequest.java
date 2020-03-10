package com.fire.localmusic.request;

import android.net.Uri;

import com.facebook.drawee.view.SimpleDraweeView;

import io.reactivex.Observable;
import com.fire.localmusic.bean.netease.NSearchRequest;
import com.fire.localmusic.request.network.RxUtil;
import com.fire.localmusic.util.LogUtil;
import com.fire.localmusic.util.PlayListUtil;

import static com.fire.localmusic.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * Created by Remix on 2017/11/30.
 */

public class PlayListUriRequest extends LibraryUriRequest {
    public PlayListUriRequest(SimpleDraweeView image, NSearchRequest request,RequestConfig config) {
        super(image,request,config);
    }

    @Override
    public void onError(String errMsg) {
        mImage.setImageURI(Uri.EMPTY);
        LogUtil.d("Cover","Err: " + errMsg);
    }

    @Override
    public void load() {
        LogUtil.d("Cover","Request: " + mRequest);
        Observable.concat(
                getCustomThumbObservable(mRequest),
                Observable.fromIterable(PlayListUtil.getMP3ListByIds(PlayListUtil.getIDList(mRequest.getID()),mRequest.getID())).concatMapDelayError(song -> getThumbObservable(getSearchRequestWithAlbumType(song))))
        .firstOrError()
        .toObservable()
        .compose(RxUtil.applyScheduler())
        .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
    }
}
