package mert.kadakal.bulut.ui.home;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import mert.kadakal.bulut.R;

public class HomeAdapter extends BaseAdapter {

    private final Context context;
    private final List<String> notifications;

    public HomeAdapter(Context context, List<String> notifications) {
        this.context = context;
        this.notifications = notifications;
    }

    @Override
    public int getCount() {
        return notifications.size();
    }

    @Override
    public Object getItem(int position) {
        return notifications.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Eğer convertView yoksa, yeni bir View oluştur
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.bildirim_item, parent, false);
        }

        // TextView'i bul ve bildirimi ayarla
        TextView textView = convertView.findViewById(R.id.item_title);
        textView.setText(Html.fromHtml(notifications.get(position)));

        return convertView;
    }
}
