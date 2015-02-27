package adnan.haber.util;

import android.content.Context;
import android.text.Spannable;
import android.text.style.ImageSpan;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adnan.haber.Haber;
import adnan.haber.HaberService;
import adnan.haber.R;
import adnan.haber.types.Rank;

/**
 * Created by Adnan on 24.1.2015..
 */
public class RankIconManager {

    private static final Spannable.Factory spannableFactory = Spannable.Factory.getInstance();

    private static final Map<Pattern, Integer> emoticons = new HashMap<Pattern, Integer>();


    public static Spannable getSpanned(Context context, String username, String altString)  {
        Rank rank = HaberService.GetRankForUser(username);

        String span = "</guest>";
        if ( rank == Rank.Moderator || rank == Rank.Admin ) {
            span = "</moderator>";
        } else if ( rank == Rank.Adnan ) {
            span = "</adnan>";
        } else if ( rank == Rank.User ) {
            span = "</user>";
        } else if ( rank == Rank.Enil ) {
            span = "</enil>";
        } else if ( rank == Rank.Berina ) {
            span = "</berina>";
        } else if ( rank == Rank.Mathilda ) {
            span = "</mathilda>";
        } else if ( rank == Rank.Alma ) {
            span = "</alma>";
        } else if ( rank == Rank.Memi ) {
            span = "</memi>";
        } else if ( rank == Rank.Lamija ) {
            span = "</lamija>";
        } else if ( rank == Rank.Merima ) {
            span = "</merima>";
        } else if ( rank == Rank.Vedran ) {
            span = "</vedran>";
        }

        return _getSpanned(context, span + "  " + altString);
    }

    public static Spannable getSpanned(Context context, String username)  {
        return getSpanned(context, username, Haber.getShortUsername(username));
    }

    static {
        addPattern(emoticons, "</guest>", R.drawable.star_empty);
        addPattern(emoticons, "</user>", R.drawable.star_half);
        addPattern(emoticons, "</moderator>", R.drawable.star_full);
        addPattern(emoticons, "</adnan>", R.drawable.adnan);
        addPattern(emoticons, "</enil>", R.drawable.enil);
        addPattern(emoticons, "</berina>", R.drawable.berina);
        addPattern(emoticons, "</mathilda>", R.drawable.mathilda);
        addPattern(emoticons, "</alma>", R.drawable.alma);
        addPattern(emoticons, "</memi>", R.drawable.memi);
        addPattern(emoticons, "</lamija>", R.drawable.lamija);
        addPattern(emoticons, "</merima>", R.drawable.merima);
        addPattern(emoticons, "</vedran>", R.drawable.tux);
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

    private static Spannable _getSpanned(Context context, CharSequence text) {
        Spannable spannable = spannableFactory.newSpannable(text);
        addSmiles(context, spannable);
        return spannable;
    }

}
