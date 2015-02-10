package adnan.haber.util;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import adnan.haber.R;

/**
 * Created by Adnan on 24.1.2015..
 */
public class SmileyManager {

    private static final Spannable.Factory spannableFactory = Spannable.Factory.getInstance();

    private static final Map<Pattern, Integer> emoticons = new HashMap<Pattern, Integer>();

    static {
        addPattern(emoticons, "o:)", R.drawable.angel);
        addPattern(emoticons, "O:)", R.drawable.angel);
        addPattern(emoticons, "o:-)", R.drawable.angel);
        addPattern(emoticons, "O:-)", R.drawable.angel);

        addPattern(emoticons, "-.-", R.drawable.ambivalent);

        addPattern(emoticons, ";)", R.drawable.wink);
        addPattern(emoticons, ";-)", R.drawable.wink);

        addPattern(emoticons, ":o", R.drawable.largegasp);
        addPattern(emoticons, ":-o", R.drawable.largegasp);
        addPattern(emoticons, ":-O", R.drawable.largegasp);
        addPattern(emoticons, ":O", R.drawable.largegasp);

        addPattern(emoticons, ">.<", R.drawable.ohnoes);
        addPattern(emoticons, ">_<", R.drawable.ohnoes);


        addPattern(emoticons, "3:)", R.drawable.naughty);
        addPattern(emoticons, "3:-)", R.drawable.naughty);
        addPattern(emoticons, ":666:", R.drawable.naughty);

        addPattern(emoticons, ":)", R.drawable.halo);
        addPattern(emoticons, ":-)", R.drawable.halo);
        addPattern(emoticons, "(:", R.drawable.halo);
        addPattern(emoticons, "(-:", R.drawable.halo);

        addPattern(emoticons, ":D", R.drawable.laugh);
        addPattern(emoticons, ":-D", R.drawable.laugh);

        addPattern(emoticons, ":(", R.drawable.frown);
        addPattern(emoticons, ":-(", R.drawable.frown);
        addPattern(emoticons, "):", R.drawable.frown);
        addPattern(emoticons, ")-:", R.drawable.frown);

        addPattern(emoticons, "^^", R.drawable.grin);
        addPattern(emoticons, "^.^", R.drawable.grin);

        addPattern(emoticons, ":p", R.drawable.stickingouttongue);
        addPattern(emoticons, ":P", R.drawable.stickingouttongue);

        addPattern(emoticons, ":s", R.drawable.confused);
        addPattern(emoticons, ":S", R.drawable.confused);

        addPattern(emoticons, ":/ ", R.drawable.undecided);

        addPattern(emoticons, "8)", R.drawable.hot);
        addPattern(emoticons, "8-)", R.drawable.hot);

        addPattern(emoticons, "$)", R.drawable.moneymouth);
        addPattern(emoticons, "$-)", R.drawable.moneymouth);

        addPattern(emoticons, ":x", R.drawable.lipsaresealed);
        addPattern(emoticons, ":X", R.drawable.lipsaresealed);
        addPattern(emoticons, ":-x", R.drawable.lipsaresealed);
        addPattern(emoticons, ":-X", R.drawable.lipsaresealed);

        addPattern(emoticons, ":$", R.drawable.frown);
        addPattern(emoticons, ":-$", R.drawable.frown);
        addPattern(emoticons, "$:", R.drawable.frown);
        addPattern(emoticons, "$-:", R.drawable.frown);

        addPattern(emoticons, "(y)", R.drawable.thumbsup);
        addPattern(emoticons, ":like:", R.drawable.thumbsup);
        addPattern(emoticons, "(Y)", R.drawable.thumbsup);

        addPattern(emoticons, "(n)", R.drawable.thumbsdown);
        addPattern(emoticons, "(N)", R.drawable.thumbsdown);
        addPattern(emoticons, ":dislike:", R.drawable.thumbsdown);

        addPattern(emoticons, ":hrkljus:", R.drawable.veryangry);

        // oO  :|  :shit:  :cookie:  :crab:

    }

    private static void addPattern(Map<Pattern, Integer> map, String smile,
                                   int resource) {
        map.put(Pattern.compile(Pattern.quote(smile)), resource);
    }

    private static boolean addSmiles(Context context, Spannable spannable) {
        boolean hasChanges = false;
        for (Map.Entry<Pattern, Integer> entry : emoticons.entrySet()) {
            Matcher matcher = entry.getKey().matcher(spannable);
            while (matcher.find()) {
                boolean set = true;
                for (ImageSpan span : spannable.getSpans(matcher.start(),
                        matcher.end(), ImageSpan.class))
                    if (spannable.getSpanStart(span) >= matcher.start()
                            && spannable.getSpanEnd(span) <= matcher.end())
                        spannable.removeSpan(span);
                    else {
                        set = false;
                        break;
                    }
                if (set) {
                    hasChanges = true;
                    spannable.setSpan(new ImageSpan(context, entry.getValue()),
                            matcher.start(), matcher.end(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return hasChanges;
    }

    public static Spannable getSmiledText(Context context, Spannable span ) {
        addSmiles(context, span);
        return span;
    }

    public static Spannable getSmiledText(Context context, CharSequence text) {
        Spannable spannable = spannableFactory.newSpannable(text);
        addSmiles(context, spannable);

        return spannable;
    }

    public static SpannableStringBuilder getSmiledText(Context context, SpannableStringBuilder text) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(text);
        addSmiles(context, spannable);

        return spannable;
    }

}
