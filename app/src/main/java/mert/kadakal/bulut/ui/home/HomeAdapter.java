package mert.kadakal.bulut.ui.home;

import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import mert.kadakal.bulut.R;

public class HomeAdapter extends BaseAdapter {

    private final Context context;
    private final List<String> notifications;
    private ImageView bildirim_gorsel;

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
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.bildirim_item, parent, false);
        }

        TextView title = convertView.findViewById(R.id.item_title);
        TextView tarih = convertView.findViewById(R.id.item_date);
        bildirim_gorsel = convertView.findViewById(R.id.bildirim_görsel);
        title.setText(Html.fromHtml(notifications.get(position).split("<bildirim>")[0]));
        tarih.setText(Html.fromHtml("<br>"+notifications.get(position).split("<tarih>")[1]));

        switch (notifications.get(position).split("<bildirim>")[1].split("<tarih>")[0]) {
            case "çıkış":
                Glide.with(context)
                        .load(R.drawable.logout)
                        .into(bildirim_gorsel);
                break;
            case "pp":
                Glide.with(context)
                        .load(R.drawable.person_png)
                        .into(bildirim_gorsel);
                break;
            case "yeni görsel":
                Glide.with(context)
                        .load(R.drawable.image)
                        .into(bildirim_gorsel);
                break;
            case "yeni yorum":
                Glide.with(context)
                        .load(R.drawable.comment)
                        .into(bildirim_gorsel);
                break;
            case "beğeni":
                Glide.with(context)
                        .load(R.drawable.like)
                        .into(bildirim_gorsel);
                break;
            case "giriş":
                Glide.with(context)
                        .load(R.drawable.login)
                        .into(bildirim_gorsel);
                break;
            case "oluştur":
                Glide.with(context)
                        .load(R.drawable.plus)
                        .into(bildirim_gorsel);
                break;

        }

        return convertView;
    }
}
