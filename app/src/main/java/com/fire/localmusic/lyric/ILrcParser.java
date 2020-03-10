package com.fire.localmusic.lyric;

import java.io.BufferedReader;
import java.util.List;

import com.fire.localmusic.lyric.bean.LrcRow;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/28 09:48
 */

public interface ILrcParser {
    void saveLrcRows(List<LrcRow> lrcRows, String key);

    List<LrcRow> getLrcRows(BufferedReader bufferedReader, boolean needCache, String songName, String artistName);
}
