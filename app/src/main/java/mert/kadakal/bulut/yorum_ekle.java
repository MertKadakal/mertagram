package mert.kadakal.bulut;

import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mert.kadakal.bulut.ui.dashboard.DashboardAdapter;
import mert.kadakal.bulut.ui.dashboard.DashboardItem;

public class yorum_ekle extends AppCompatActivity {
    private EditText yorum;
    private Button ekle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.yorum_ekle);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);

        yorum = findViewById(R.id.yapılan_yorum);
        ekle = findViewById(R.id.ekle);
        ekle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String link = getIntent().getStringExtra("link");

                db.collection("görseller")
                        .whereEqualTo("link", link)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                db.collection("görseller")
                                        .document(document.getId())
                                        .update("yorumlar", FieldValue.arrayUnion(sharedPreferences.getString("hesap_ismi", "") + "<br><br>" + yorum.getText().toString()));

                                String görsel_sahibi = document.getString("hesap");
                                String görsel_başlığı = document.getString("başlık");
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
                                                                .update("bildirimler", FieldValue.arrayUnion("<b>"+sharedPreferences.getString("hesap_ismi", "") + "</b>, şu gönderine yorum yaptı: <i>" + görsel_başlığı + "</i><bildirim>yeni yorum"));
                                                    }
                                                }
                                            }
                                        });


                                onBackPressed();
                            }
                        });
            }
        });
    }
}
