package com.fire.localmusic.request.network;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;



public interface ApiService {
    @POST("search/pc")
//    @Headers("Cookie: appver=1.5.0.75771")
    Observable<ResponseBody> getNeteaseSearch(@Query("s") String key, @Query("offset") int offset,
                                              @Query("limit") int limit, @Query("type") int type);
    @GET("song/lyric")
//    @Headers("Cookie: appver=1.5.0.75771")
    Observable<ResponseBody> getNeteaseLyric(@Query("os") String os,@Query("id") int id,@Query("lv") int lv,@Query("kv") int kv,@Query("tv") int tv);

    @GET("search")
    Observable<ResponseBody> getKuGouSearch(@Query("ver") int ver,@Query("man") String man,@Query("client") String client,
                                            @Query("keyword") String keyword,@Query("duration") long duration,@Query("hash") String hash);

    @GET("download")
    Observable<ResponseBody> getKuGouLyric(@Query("ver") int ver,@Query("client") String client,@Query("fmt") String fmt,@Query("charset") String charSet,
                                            @Query("id") int id,@Query("accesskey") String accessKey);
}
