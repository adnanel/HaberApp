package adnan.haber.util;

import android.content.Context;
import android.graphics.Color;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import adnan.haber.R;
import adnan.haber.types.Theme;

/**
 * Created by Adnan on 24.1.2015..
 */
public class JSON {
    public static UpdateInfo getUpdateInfo() {
        try {
            JSONObject object = new JSONObject(downloadJSON("https://drive.google.com/uc?export=download&id=0B310dhQuX3QdLThfYXNUTThWUEk"));

            UpdateInfo info = new UpdateInfo();
            info.changeLog    = object.getString("changeLog");
            info.version      = object.getString("version");
            info.apkUrl       = object.getString("apkURL");
            info.changeLogUrl = object.getString("changeLogUrl");

            return info;
        } catch ( Exception e ) {
            Debug.log(e);
            return null;
        }
    }

    static Theme readThemeJSON(JSONObject object) throws Exception {
        Theme theme = new Theme();
        theme.COLOR_CHAT_ACTIVE_STROKE      = Color.parseColor(object.getString(ThemeManager.COLOR_CHAT_ACTIVE_STROKE));
        theme.COLOR_CHAT_TAB_BACKGROUND     = Color.parseColor(object.getString(ThemeManager.COLOR_CHAT_TAB_BACKGROUND));
        theme.COLOR_CHAT_BACKGROUND         = Color.parseColor(object.getString(ThemeManager.COLOR_CHAT_BACKGROUND));
        theme.COLOR_CHAT_DIVIDER            = Color.parseColor(object.getString(ThemeManager.COLOR_CHAT_DIVIDER));
        theme.COLOR_LEFT_DRAWER_BACKGROUND  = Color.parseColor(object.getString(ThemeManager.COLOR_LEFT_DRAWER_BACKGROUND));
        theme.COLOR_CHAT_USER_BACKGROUND    = Color.parseColor(object.getString(ThemeManager.COLOR_CHAT_USER_BACKGROUND));
        theme.COLOR_CHAT_ITEM_BACKGROUND    = Color.parseColor(object.getString(ThemeManager.COLOR_CHAT_ITEM_BACKGROUND));

        return theme;
    }
    public static Theme GetDefaultTheme(Context context) throws Exception {
        JSONObject object = new JSONObject(Util.readRawTextFile(context, R.raw.default_theme));

        Theme theme = readThemeJSON(object);

        return theme;
    }

    public static Theme GetTheme(Theme theme) throws Exception {
        JSONObject object = new JSONObject(downloadJSON(theme.url));

        return readThemeJSON(object);
    }

    public static ArrayList<Theme> getThemes() throws Exception {
        ArrayList<Theme> themes = new ArrayList<>();

        try {
            JSONObject object = new JSONObject(downloadJSON("https://drive.google.com/uc?export=download&id=0B310dhQuX3QdbmhaejJlZm9yd1k"));

            JSONArray array = object.getJSONArray("themes");
            for ( int i = 0; i < array.length(); i ++ ) {
                Theme current = new Theme();
                current.author = array.getJSONObject(i).getString("author");
                current.name = array.getJSONObject(i).getString("name");
                current.url = array.getJSONObject(i).getString("url");

                themes.add(current);
            }

            return themes;
        } catch ( Exception e ) {
            Debug.log(e);
            return null;
        }
    }

    public static String downloadJSON(String url) throws Exception {
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Authorization", "Client-ID f076662765695cf");
        try {
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } else {
                Debug.log("JSON: Failed to download file");
                if ( statusCode == 401 ) throw new Exception("Not allowed!");

            }
        } catch (ClientProtocolException e) {
            Debug.log("JSON: " + e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Debug.log("JSON: " + e.toString());
            e.printStackTrace();
        }

        return builder.toString();
    }

    public static class UpdateInfo {
        public String changeLog;
        public String version;
        public String apkUrl;
        public String changeLogUrl;
    }
}
