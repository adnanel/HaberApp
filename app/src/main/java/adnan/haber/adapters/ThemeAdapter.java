package adnan.haber.adapters;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import java.util.List;

import adnan.haber.R;
import adnan.haber.types.Theme;
import adnan.haber.util.ThemeManager;

/**
 * Created by Adnan on 1.2.2015..
 */
public class ThemeAdapter  extends RecyclerView.Adapter<ThemeAdapter.TableViewHolder> {
    private List<Theme> values;
    private OnThemeChangedListener themeListener;

    public ThemeAdapter(List<Theme> contactList, OnThemeChangedListener themeListener) {
        this.values = contactList;
        this.themeListener = themeListener;
    }

    public void addItem(Theme entry) {
        this.values.add(entry);
        this.notifyItemInserted(this.values.size() - 2);
    }

    public void clearItems() {
        this.values.clear();
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    @Override
    public void onBindViewHolder(TableViewHolder contactViewHolder, int i) {
        final Theme ci = values.get(i);

        contactViewHolder.tvTheme.setText(ci.name);
        contactViewHolder.tvAuthor.setText(ci.author);
        contactViewHolder.btApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                themeListener.onApplyTheme(ci);
            }
        });
    }

    @Override
    public TableViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.single_theme, viewGroup, false);

        itemView.startAnimation(AnimationUtils.loadAnimation(viewGroup.getContext(), R.anim.abc_fade_in));
        return new TableViewHolder(itemView);
    }

    public class TableViewHolder extends RecyclerView.ViewHolder  {
        TextView tvAuthor;
        TextView tvTheme;

        View btApply;

        public TableViewHolder(View v) {
            super(v);

            tvAuthor = (TextView)v.findViewById(R.id.tvThemeAuthor);
            btApply = v.findViewById(R.id.btThemeApply);
            tvTheme = (TextView)v.findViewById(R.id.tvThemeName);
        }
    }

    public interface OnThemeChangedListener {
        public abstract void onApplyTheme(Theme theme);
    }
}
