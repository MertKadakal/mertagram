package mert.kadakal.bulut.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import mert.kadakal.bulut.R;

public class DashboardAdapter extends BaseAdapter {

    private final Context context;
    private final List<DashboardItem> items;
    private Button btn_beğen;
    private Button btn_yorumlar;
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
        ImageView imageView = convertView.findViewById(R.id.image_view);
        TextView textView = convertView.findViewById(R.id.text_view);
        DashboardItem item = items.get(position);

        if (!(sharedPreferences.getBoolean("hesap_açık_mı",false))) {
            btn_beğen.setVisibility(View.INVISIBLE);
            btn_yorumlar.setVisibility(View.INVISIBLE);
        } else {
            db.collection("hesaplar")
                    .whereEqualTo("isim", sharedPreferences.getString("hesap_ismi", ""))
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                List<String> begenilenler = (List<String>) document.get("beğenilenler");

                                for (String link : begenilenler) {
                                    if (link.equals(item.getLink())) {
                                        btn_beğen.setText("Beğeniyi geri çek");
                                    }
                                }
                            }
                        }
                    });
            if (!(btn_beğen.getText().toString().equals("Beğeniyi geri çek"))) {
                btn_beğen.setText("Beğen");
            }
        }

        btn_beğen.setOnClickListener(view -> {
            String link = item.getLink();
            String hesapIsmi = sharedPreferences.getString("hesap_ismi", "");

            // Görsel beğeni sayısını artır
            db.collection("görseller")
                    .whereEqualTo("link", link)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                if (btn_beğen.getText().toString().equals("Beğeniyi geri çek")) {
                                    db.collection("hesaplar")
                                            .whereEqualTo("isim", hesapIsmi)
                                            .get()
                                            .addOnCompleteListener(task1 -> {
                                                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                                    for (DocumentSnapshot document1 : task1.getResult()) {
                                                        db.collection("hesaplar")
                                                                .document(document1.getId())
                                                                .update("beğenilenler", FieldValue.arrayRemove(item.getLink()));

                                                        db.collection("görseller")
                                                                .whereEqualTo("link", item.getLink())
                                                                .get()
                                                                .addOnCompleteListener(task2 -> {
                                                                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                                                        for (DocumentSnapshot document2 : task2.getResult()) {
                                                                            db.collection("görseller")
                                                                                    .document(document2.getId())
                                                                                    .update("beğeni", FieldValue.increment(-1));
                                                                        }
                                                                    }
                                                                });

                                                        btn_beğen.setText("Beğen");
                                                    }
                                                }
                                            });
                                } else {
                                    db.collection("görseller")
                                            .document(document.getId())
                                            .update("beğeni", FieldValue.increment(1))
                                            .addOnSuccessListener(aVoid -> {
                                                // Hesabın beğenilen görsellerine ekle
                                                begeniyiHesabaEkle(hesapIsmi, link);
                                                btn_beğen.setText("Beğeniyi geri çek");
                                            });
                                }
                            }
                        }
                    });

        });

        Glide.with(context)
                .load(item.getLink())
                .into(imageView);
        textView.setText(String.format("Yükleyen: %s\n%s\n%d kişi beğendi", item.getHesap(), item.getTarih(), item.getBeğeni_sayısı()));

        return convertView;
    }

    // Beğenilen görseli hesaba ekleyen metod
    private void begeniyiHesabaEkle(String hesapIsmi, String link) {
        db.collection("hesaplar")
                .whereEqualTo("isim", hesapIsmi)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            db.collection("hesaplar")
                                    .document(document.getId())
                                    .update("beğenilenler", FieldValue.arrayUnion(link))
                                    .addOnSuccessListener(aVoid -> {
                                        System.out.println("Beğenilen görsel hesaba eklendi.");
                                    })
                                    .addOnFailureListener(e -> {
                                        System.err.println("Beğenilen görsel hesaba eklenemedi: " + e.getMessage());
                                    });
                        }
                    } else {
                        System.out.println("Hesap bulunamadı veya sorgu hatası.");
                    }
                });
    }
}
