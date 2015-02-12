package adnan.haber.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.Base64;
import android.util.TypedValue;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.Random;

import adnan.haber.Haber;
import adnan.haber.fragments.AdvancedPreferences;
import adnan.haber.packets.PacketTimeStamp;

/**
 * Created by Adnan on 26.1.2015..
 */
public class Util {
    public static String Encode(String string) {
        byte[] bytes = new byte[string.length()];
        int n = 0;
        for ( char c : string.toCharArray() )
            bytes[n++] = (byte)c;
        return Base64.encodeToString(bytes, Base64.DEFAULT).replace("\n", "");
    }

    public static String Decode(String string) {
        byte[] bytes = Base64.decode(string, Base64.DEFAULT);
        StringBuilder builder = new StringBuilder();
        for ( byte b : bytes )
            builder.append((char)b);
        return builder.toString();
    }


    public static String readRawTextFile(Context ctx, int resId)
    {
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            while (( line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }

    public static int DpiToPixel(Context context, int dpi) {
        Resources r = context.getResources();
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpi, r.getDisplayMetrics());
    }

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

    public static String TIME_FORMAT = "HH:mm yyyy-MM-dd";

    public static String GeneratePacketId(Packet packet) {
        String res = _GeneratePacketId(packet);

        Debug.log("dump: " + packet.toXML().toString() + " - " + res);
        return res;
    }

    public static String dateToFormat(Date date) {
        return dateToFormat(TIME_FORMAT, date);
    }

    public static String dateToFormat(String format, Date date) {
        return DateTimeFormat.forPattern(format).print(new DateTime(date.getTime()));
    }

    public static Date getDate(String date) {
        Date res;
        try {
            DateTimeFormatter sdf = DateTimeFormat.forPattern(TIME_FORMAT);

            res = sdf.parseDateTime(date).toDate();
        } catch ( Exception e ) {
            Debug.log(e);
            res = new Date();
        }

        return res;
    }

    public static String _GeneratePacketId(Packet packet) {
        try {
            for (PacketExtension ext : packet.getExtensions() ) {
                if ( ext instanceof PacketTimeStamp ) {
                    PacketTimeStamp stamp = (PacketTimeStamp) ext;
                    Message message = (Message) packet;
                    Date date = getDate(stamp.getTime());

                    String id = (message.getPacketID() == null) ? "" : message.getPacketID();
                    return makeSHA1Hash(date.toString() + message.getBody() + message.getFrom() + id);
                }
            }
            throw new Exception("Couldn't find timestamp!");
        } catch (Exception er) {
            Debug.log("GeneratePacketId - " + er.toString());

            try {
                return makeSHA1Hash(packet.toString());
            } catch (NoSuchAlgorithmException e) {
                return "";
            }
        }
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
                text.append(str).append("\n");
            }
            in.close();
            return text.toString();
        } catch (MalformedURLException e) {
            Debug.log(e);
        } catch (IOException e) {
            Debug.log(e);
        }
        return "Failed to download!";
    }

    public static int getRandomInt(int n) {
        Random rand = new Random();
        return rand.nextInt(n);
    }
}
