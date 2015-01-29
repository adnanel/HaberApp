package adnan.haber.util;

import org.apache.http.HttpConnection;
import org.apache.http.client.HttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Adnan on 26.1.2015..
 */
public class Util {

    public static String DownloadString(String _url) {
        try
        {
            URL url = new URL(_url);
            // Read all the text returned by the server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            StringBuilder text = new StringBuilder();
            while ((str = in.readLine()) != null)
            {
                text.append(str + "\n");
            }
            in.close();
            return text.toString();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return "Failed to download!";
    }
}
