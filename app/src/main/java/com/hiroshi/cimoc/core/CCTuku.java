package com.hiroshi.cimoc.core;

import com.hiroshi.cimoc.core.base.Manga;
import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.utils.Decryption;
import com.hiroshi.cimoc.utils.MachiSoup;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import okhttp3.Request;

/**
 * Created by Hiroshi on 2016/7/28.
 */
public class CCTuku extends Manga {

    public CCTuku() {
        super(Kami.SOURCE_CCTUKU, "http://m.tuku.cc");
    }

    @Override
    protected Request buildSearchRequest(String keyword, int page) {
        String url = host + "/comic/search?word=" + keyword + "&page=" + page;
        return new Request.Builder().url(url).build();
    }

    @Override
    protected List<Comic> parseSearch(String html) {
        MachiSoup.Node body = MachiSoup.body(html);
        List<Comic> list = new LinkedList<>();
        for (MachiSoup.Node node : body.list(".main-list > div > div > div")) {
            String cid = node.attr("div:eq(1) > div:eq(0) > a", "href", "/", 2);
            String title = node.text("div:eq(1) > div:eq(0) > a");
            String cover = node.attr("div:eq(0) > a > img", "src");
            String update = node.text("div:eq(1) > div:eq(1) > dl:eq(3) > dd > font");
            String author = node.text("div:eq(1) > div:eq(1) > dl:eq(1) > dd > a");
            list.add(new Comic(source, cid, title, cover, update, author, null));
        }
        return list;
    }

    @Override
    protected Request buildIntoRequest(String cid) {
        String url = host + "/comic/" + cid;
        return new Request.Builder().url(url).build();
    }

    @Override
    protected List<Chapter> parseInto(String html, Comic comic) {
        List<Chapter> list = new LinkedList<>();
        MachiSoup.Node body = MachiSoup.body(html);
        for (MachiSoup.Node node : body.list("ul.list-body > li > a")) {
            String c_title = node.text();
            String c_path = node.attr("href", "/", 3);
            list.add(new Chapter(c_title, c_path));
        }

        String title = body.text("div.title-banner > div.book-title > h1", 0, -2);
        MachiSoup.Node detail = body.select("div.book > div > div:eq(0)");
        String cover = detail.attr("div:eq(0) > a > img", "src");
        String update = detail.text("div:eq(0) > dl:eq(5) > dd > font", 0, 10);
        String author = detail.text("div:eq(0) > dl:eq(1) > dd > a");
        String intro = body.text("div.book-details > p:eq(1)");
        boolean status = "完结".equals(detail.text("div:eq(0) > div"));
        comic.setInfo(title, cover, update, intro, author, status);

        return list;
    }

    @Override
    protected Request buildBrowseRequest(String cid, String path) {
        String url = host + "/comic/" + cid + "/" + path;
        return new Request.Builder().url(url).build();
    }

    @Override
    protected String[] parseBrowse(String html) {
        String packed = MachiSoup.match("eval(.*?)\\n;", html, 1);
        if (packed != null) {
            try {
                String result = Decryption.evalDecrypt(packed);
                String[] array = MachiSoup.match("pic_url='(.*?)';.*?tpf=(\\d+?);.*pages=(\\d+?);.*?pid=(.*?);.*?pic_extname='(.*?)';", result, 1, 2, 3, 4, 5);
                if (array != null) {
                    int tpf = Integer.parseInt(array[1]) + 1;
                    int pages = Integer.parseInt(array[2]);
                    String format = "http://tkpic.um5.cc/" + array[3] + "/" + array[0] + "/%0" + tpf + "d." + array[4];
                    String[] images = new String[pages];
                    for (int i = 0; i != pages; ++i) {
                        images[i] = String.format(Locale.CHINA, format, i + 1);
                    }
                    return images;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
