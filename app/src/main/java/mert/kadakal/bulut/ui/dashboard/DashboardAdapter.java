package mert.kadakal.bulut.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import mert.kadakal.bulut.R;
import mert.kadakal.bulut.yorumlar;

public class DashboardAdapter extends BaseAdapter {

    private final Context context;
    private final List<DashboardItem> items;
    private Button btn_beğen;
    private Button btn_yorumlar;
    private ImageView btn_görseli_kaldır;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    SharedPreferences sharedPreferences;

    public DashboardAdapter(Context context, List<DashboardItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_dashboard, parent, false);
        }
        sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        btn_beğen = convertView.findViewById(R.id.beğen);
        btn_yorumlar = convertView.findViewById(R.id.yorumlar);
        btn_görseli_kaldır = convertView.findViewById(R.id.kaldır);
        ImageView imageView = convertView.findViewById(R.id.image_view);
        TextView textView = convertView.findViewById(R.id.text_view);
        DashboardItem item = items.get(position);

        if (!(sharedPreferences.getBoolean("hesap_açık_mı",false))) {
            btn_beğen.setVisibility(View.INVISIBLE);
            btn_görseli_kaldır.setVisibility(View.INVISIBLE);
        } else {
            if (!(item.getHesap().equals(sharedPreferences.getString("hesap_ismi", "")))) {
                btn_görseli_kaldır.setVisibility(View.INVISIBLE);
            }

            db.collection("görseller")
                    .whereEqualTo("link", item.getLink())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                List<String> arrayList = (List<String>) document.get("beğenenler");
                                btn_beğen.setText("\uD83D\uDC4D");
                                if (arrayList != null && arrayList.contains(sharedPreferences.getString("hesap_ismi", ""))) {
                                    btn_beğen.setText("\uD83D\uDC4E");
                                }
                            }
                        }
                    });
        }

        btn_beğen.setOnClickListener(view ->
                db.collection("görseller")
                .whereEqualTo("link", item.getLink())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            if (btn_beğen.getText().toString().equals("\uD83D\uDC4E")) { //beğeniyi geri çek

                                db.collection("görseller")
                                        .document(document.getId())
                                        .update("beğeni", FieldValue.increment(-1));

                                db.collection("görseller")
                                        .document(document.getId())
                                        .update("beğenenler", FieldValue.arrayRemove(sharedPreferences.getString("hesap_ismi", "")));

                                btn_beğen.setText("\uD83D\uDC4D");
                                textView.setText(Html.fromHtml(String.format("<br><b>Yükleyen:</b> %s<br>%s<br><b>%d</b> beğeni<br>", item.getHesap(), item.getTarih(), item.getBeğeni_sayısı())));

                            } else { //beğen

                                db.collection("görseller")
                                        .document(document.getId())
                                        .update("beğeni", FieldValue.increment(1));

                                db.collection("görseller")
                                        .document(document.getId())
                                        .update("beğenenler", FieldValue.arrayUnion(sharedPreferences.getString("hesap_ismi", "")));

                                db.collection("görseller")
                                        .document(document.getId())
                                        .get()
                                        .addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                DocumentSnapshot document12 = task1.getResult();
                                                if (document12.exists()) {
                                                    // Alanın değerini al
                                                    String görsel_sahibi = document12.getString("hesap");
                                                    String görsel_başlığı = document12.getString("başlık");

                                                    db.collection("hesaplar")
                                                            .whereEqualTo("isim", görsel_sahibi)
                                                            .get()
                                                            .addOnCompleteListener(task2 -> {
                                                                if (task2.isSuccessful()) {
                                                                    if (!task2.getResult().isEmpty()) {
                                                                        for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                                            // "bildirimler" alanına yeni eleman ekle
                                                                            db.collection("hesaplar")
                                                                                    .document(document2.getId())
                                                                                    .update("bildirimler", FieldValue.arrayUnion("<b>"+sharedPreferences.getString("hesap_ismi", "") + "</b>, şu gönderini beğendi: <i>" + görsel_başlığı + "</i><bildirim>beğeni"));
                                                                        }
                                                                    }
                                                                }
                                                            });
                                                }
                                            }
                                        });

                                btn_beğen.setText("\uD83D\uDC4E");
                                textView.setText(Html.fromHtml(String.format("<br><b>Yükleyen:</b> %s<br>%s<br><b>%d</b> beğeni<br>", item.getHesap(), item.getTarih(), item.getBeğeni_sayısı()+1)));
                            }
                        }
                    }
                }));

        btn_yorumlar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                db.collection("görseller")
                        .whereEqualTo("link", item.getLink())
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                DocumentSnapshot document1 = task.getResult().getDocuments().get(0);
                                ArrayList<String> yorumlar_list = (ArrayList<String>) document1.get("yorumlar");

                                Intent intent = new Intent(context, yorumlar.class)
                                        .putStringArrayListExtra("yorumlar_list", yorumlar_list)
                                        .putExtra("görsel_link", item.getLink());
                                context.startActivity(intent);
                            }
                        });


            }
        });

        btn_görseli_kaldır.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                db.collection("görseller").whereEqualTo("link", item.getLink()).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            db.collection("görseller").document(document.getId()).delete();
                            Toast.makeText(context, "Görsel silindi", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        Glide.with(context)
                .load(item.getLink())
                .into(imageView);

        textView.setText(Html.fromHtml(String.format("<br><b>Yükleyen:</b> %s<br>%s<br><b>%d</b> beğeni<br>", item.getHesap(), item.getTarih(), item.getBeğeni_sayısı())));

        return convertView;
    }
}