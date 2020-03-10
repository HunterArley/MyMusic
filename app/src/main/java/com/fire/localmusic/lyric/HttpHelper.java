package com.fire.localmusic.lyric;

import io.reactivex.Observable;
import okhttp3.ResponseBody;



public interface HttpHelper {
    Observable<ResponseBody> getNeteaseSearch(String key,int offset,int limit,int type);

    Observable<ResponseBody> getNeteaseLyric(int id);

    Observable<ResponseBody> getKuGouSearch(String keyword, long duration, String hash);

    Observable<ResponseBody> getKuGouLyric(int id,String accessKey);
}
