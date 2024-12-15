package mert.kadakal.bulut;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class YorumlarAdapter extends BaseAdapter {

    private final Context context;
    private final List<String> items;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    SharedPreferences sharedPreferences;
    private TextView yorum_içerigi;
    private TextView yorum_tarihi;
    private ImageView yorumu_sil;
    private ImageView yorumu_düzenle;
    private String link;

    public YorumlarAdapter(Context context, List<String> items, String link) {
        this.context = context;
        this.items = items;
        this.link = link;
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
            convertView = LayoutInflater.from(context).inflate(R.layout.yorum_item, parent, false);
        }

        sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        String item = items.get(position);

        yorumu_sil = convertView.findViewById(R.id.yorumu_sil);
        yorumu_düzenle = convertView.findViewById(R.id.yorumu_düzenle);
        if (!(sharedPreferences.getBoolean("hesap_açık_mı", false) && sharedPreferences.getString("hesap_ismi", "").equals(item.split("<br>")[0]))) {
            yorumu_sil.setVisibility(View.INVISIBLE);
            yorumu_düzenle.setVisibility(View.INVISIBLE);
        }

        String yorum_içeriği_str = item;
        String yorumcu = item.split("<br><br>")[0];
        String yorum = item.split("<br><br>")[1];
        String tarih = item.split("<br><br>")[2];
        yorum_içerigi = convertView.findViewById(R.id.yorum_içeriği);
        yorum_içerigi.setText(Html.fromHtml("<b>" + yorumcu + "</b><br><br>'" + yorum + "'"));
        yorum_tarihi = convertView.findViewById(R.id.yorum_tarihi);
        yorum_tarihi.setText(Html.fromHtml("<i>"+tarih+"</i>"));

        yorumu_sil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                db.collection("görseller").whereEqualTo("link", link).get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                for (DocumentSnapshot document : task.getResult()) {
                                    List<String> yorumlarList = (List<String>) document.get("yorumlar");

                                    if (yorumlarList != null) {
                                        for (int i = 0; i < yorumlarList.size(); i++) {
                                            if (yorumlarList.get(i).equals(yorum_içeriği_str)) {
                                                String silinen = yorumlarList.get(i);
                                                yorumlarList.remove(i);

                                                db.collection("görseller")
                                                        .document(document.getId())
                                                        .update("yorumlar", yorumlarList);

                                                String görsel_başlığı = document.getString("başlık");
                                                SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR"));
                                                Date specificDate = new Date();  // Örnek tarih, kendi tarihini burada belirleyebilirsin.
                                                String formattedDate = dateFormat.format(specificDate);
                                                db.collection("hesaplar")
                                                        .whereEqualTo("isim", sharedPreferences.getString("hesap_ismi",""))
                                                        .get()
                                                        .addOnCompleteListener(task2 -> {
                                                            if (task2.isSuccessful()) {
                                                                if (!task2.getResult().isEmpty()) {
                                                                    for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                                        // "bildirimler" alanına yeni eleman ekle
                                                                        db.collection("hesaplar")
                                                                                .document(document2.getId())
                                                                                .update("bildirimler", FieldValue.arrayUnion("<b>"+ görsel_başlığı +"</b> adlı görsele yaptığınız yorumu sildiniz:<br><br><i>" + silinen.split("<br><br>")[1] + "</i><bildirim>yeni yorum<tarih>"+formattedDate));
                                                                    }
                                                                }
                                                            }
                                                        });

                                                Toast.makeText(context, "Yorumunuz silindi", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                }
                            }
                        });
            }

        });

        yorumu_düzenle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editText = new EditText(view.getContext());
                editText.setText(yorum);
                editText.setPadding(30, 60, 30, 25);

                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setView(editText)
                        .setPositiveButton("Onayla", (dialog, which) -> {
                            String value = editText.getText().toString();

                            db.collection("görseller").whereEqualTo("link", link).get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                            for (DocumentSnapshot document : task.getResult()) {
                                                List<String> yorumlarList = (List<String>) document.get("yorumlar");

                                                if (yorumlarList != null) {
                                                    for (int i = 0; i < yorumlarList.size(); i++) {
                                                        if (yorumlarList.get(i).equals(yorum_içeriği_str)) {
                                                            String eski = yorumlarList.get(i).split("<br><br>")[1];
                                                            yorumlarList.set(i, sharedPreferences.getString("hesap_ismi", "") + "<br><br>" + value + "<br><br>" + tarih);

                                                            String görsel_başlığı = document.getString("başlık");
                                                            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR"));
                                                            Date specificDate = new Date();
                                                            String formattedDate = dateFormat.format(specificDate);

                                                            db.collection("hesaplar")
                                                                    .whereEqualTo("isim", sharedPreferences.getString("hesap_ismi", ""))
                                                                    .get()
                                                                    .addOnCompleteListener(task2 -> {
                                                                        if (task2.isSuccessful()) {
                                                                            if (!task2.getResult().isEmpty()) {
                                                                                for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                                                    db.collection("hesaplar")
                                                                                            .document(document2.getId())
                                                                                            .update("bildirimler", FieldValue.arrayUnion("<b>" + görsel_başlığı + "</b> adlı görsele yaptığınız yorumu düzenlediniz:<br><br><i>" + eski + "</i><br>↓<br><i>" + value + "</i><bildirim>yeni yorum<tarih>" + formattedDate));
                                                                                }
                                                                            }
                                                                        }
                                                                    });

                                                            db.collection("görseller")
                                                                    .document(document.getId())
                                                                    .update("yorumlar", yorumlarList);

                                                            Toast.makeText(context, "Yorumunuz güncellendi", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    });
                        })
                        .setNegativeButton("İptal", (dialog, which) -> dialog.dismiss());

                AlertDialog dialog = builder.create();
                dialog.show();

                Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

                positiveButton.setTextColor(Color.WHITE);
                negativeButton.setTextColor(Color.WHITE);
            }
        });

        return convertView;
    }
}
