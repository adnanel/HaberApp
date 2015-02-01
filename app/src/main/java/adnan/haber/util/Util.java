package adnan.haber.util;

import org.apache.http.HttpConnection;
import org.apache.http.client.HttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Created by Adnan on 26.1.2015..
 */
public class Util {


    public static String makeSHA1Hash(String input)
            throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.reset();
        byte[] buffer = input.getBytes();
        md.update(buffer);
        byte[] digest = md.digest();

        String hexStr = "";
        for (int i = 0; i < digest.length; i++) {
            hexStr +=  Integer.toString( ( digest[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return hexStr;
    }

    public static String encryptPassword(String password)
    {
        String sha1 = "";
        try
        {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(password.getBytes("UTF-8"));
            sha1 = byteToHex(crypt.digest());
        }
        catch(NoSuchAlgorithmException e)
        {
            e.printStackTrace();
            Debug.log(e);
        }
        catch(UnsupportedEncodingException e)
        {
            e.printStackTrace();
            Debug.log(e);
        }
        return sha1;
    }

    private static String byteToHex(final byte[] hash)
    {
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

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
