package mert.kadakal.bulut;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mert.kadakal.bulut.ui.dashboard.DashboardAdapter;
import mert.kadakal.bulut.ui.dashboard.DashboardItem;

public class yorumlar extends AppCompatActivity {
    private List<String> items = new ArrayList<>();
    private YorumlarAdapter adapter;
    private ListView list;
    private TextView yorum_yok;
    private Button yorum_ekle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.yorumlar);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        adapter  = new YorumlarAdapter(this, items, getIntent().getStringExtra("görsel_link"));
        list = findViewById(R.id.list_yorumlar);
        yorum_yok = findViewById(R.id.yorum_yok);
        yorum_ekle = findViewById(R.id.yorum_ekle);
        if (!(sharedPreferences.getBoolean("hesap_açık_mı", false))) {
            yorum_ekle.setVisibility(View.INVISIBLE);
        }

        for (String yorum : getIntent().getStringArrayListExtra("yorumlar_list")) {
            items.add(yorum);
        }

        if (items.size() == 0) {
            list.setVisibility(View.INVISIBLE);
        } else {
            yorum_yok.setVisibility(View.INVISIBLE);
            list.setAdapter(adapter);
        }

        yorum_ekle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editText = new EditText(view.getContext());
                editText.setPadding(30, 150, 30, 25);

                String link = getIntent().getStringExtra("görsel_link");

                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setView(editText)
                        .setPositiveButton("Ekle", (dialog, which) -> {
                            String value = editText.getText().toString();

                            db.collection("görseller")
                                    .whereEqualTo("link", link)
                                    .get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                            DocumentSnapshot document = task.getResult().getDocuments().get(0);

                                            String görsel_sahibi = document.getString("hesap");
                                            String görsel_başlığı = document.getString("başlık");
                                            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR"));
                                            Date specificDate = new Date();  // Örnek tarih, kendi tarihini burada belirleyebilirsin.
                                            String formattedDate = dateFormat.format(specificDate);

                                            db.collection("görseller")
                                                    .document(document.getId())
                                                    .update("yorumlar", FieldValue.arrayUnion(sharedPreferences.getString("hesap_ismi", "") + "<br><br>" + value + "<br><br>" + formattedDate));

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
                                                                            .update("bildirimler", FieldValue.arrayUnion("<b>"+sharedPreferences.getString("hesap_ismi", "") + "</b>, şu gönderine yorum yaptı: <br><br><i>" + görsel_başlığı + "</i><bildirim>yeni yorum<tarih>"+formattedDate));
                                                                }
                                                            }
                                                        }
                                                    });

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
                                                                            .update("bildirimler", FieldValue.arrayUnion("<b>"+görsel_başlığı+"</b> adlı görsele yorum yaptınız:<br><br><i>" + value + "</i><bildirim>yeni yorum<tarih>"+formattedDate));
                                                                }
                                                            }
                                                        }
                                                    });

                                            Toast.makeText(yorumlar.this, "Yorumunuz eklendi", Toast.LENGTH_SHORT).show();
                                            onBackPressed();
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
    }
}
