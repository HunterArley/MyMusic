package com.fire.localmusic.lyric;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fire.localmusic.lyric.bean.LrcRow;
import com.fire.localmusic.misc.cache.DiskCache;
import com.fire.localmusic.misc.cache.DiskLruCache;
import com.fire.localmusic.util.Util;

/**
 * @ClassName
 * @Description 解析歌词实现类
 * @Author Xiaoborui
 * @Date 2016/10/28 09:50
 */

public class DefaultLrcParser implements ILrcParser {
    @Override
    public void saveLrcRows(List<LrcRow> lrcRows, String key){
        if(lrcRows == null || lrcRows.size() == 0) {
            return;
        }
        DiskLruCache.Editor editor;
        OutputStream lrcCacheStream = null;
        try {
            editor = DiskCache.getLrcDiskCache().edit(key);
            if(editor == null) {
                return;
            }
            lrcCacheStream = editor.newOutputStream(0);
            lrcCacheStream.write(new Gson().toJson(lrcRows,new TypeToken<List<LrcRow>>(){}.getType()).getBytes());
            lrcCacheStream.flush();
            editor.commit();

            DiskCache.getLrcDiskCache().flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(lrcCacheStream != null) {
                    lrcCacheStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public List<LrcRow> getLrcRows(BufferedReader bufferedReader, boolean needCache,String songName,String artistName) {
        if(bufferedReader == null) {
            return null;
        }
        //解析歌词
        List<LrcRow> lrcRows = new ArrayList<>();
        List<String> allLine = new ArrayList<>();
        String line;
        int offset = 0;
        try {
            while ((line = bufferedReader.readLine()) != null) {
               if(!TextUtils.isEmpty(line)){
                   allLine.add(line);
                   //读取offset标签
                   if(line.startsWith("[offset:") && line.endsWith("]")){
                       String offsetInString = line.substring(line.lastIndexOf(":") + 1, line.length() - 1);
                       if(!TextUtils.isEmpty(offsetInString) && TextUtils.isDigitsOnly(offsetInString)){
                           offset = Integer.valueOf(offsetInString);
                       }
                   }
               }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(allLine.size() == 0) {
            return null;
        }

        for(String temp: allLine){
            //解析每一行歌词
            List<LrcRow> rows = LrcRow.createRows(temp,offset);
            if(rows != null && rows.size() > 0) {
                lrcRows.addAll(rows);
            }
        }

        if(lrcRows.size() == 0) {
            return null;
        }
        //为歌词排序
        Collections.sort(lrcRows);

        for (int i = 0; i < lrcRows.size() - 1; i++) {
            lrcRows.get(i).setTotalTime(lrcRows.get(i + 1).getTime() - lrcRows.get(i).getTime());
        }
        lrcRows.get(lrcRows.size() - 1).setTotalTime(5000);

        if (needCache) {
            saveLrcRows(lrcRows,Util.hashKeyForDisk(songName + "/" + artistName));
//            editor = DiskCache.getLrcDiskCache().edit(Util.hashKeyForDisk(songName + "/" + artistName));
//            if(editor != null)
//                lrcCacheStream = editor.newOutputStream(0);
//            if(lrcCacheStream != null){
//                lrcCacheStream.write(new Gson().toJson(lrcRows,new TypeToken<List<LrcRow>>(){}.getType()).getBytes());
//                lrcCacheStream.flush();
//            }
//            if (editor != null) {
//                editor.commit();
//            }
//            DiskCache.getLrcDiskCache().flush();
        }


        return lrcRows;
    }
}
