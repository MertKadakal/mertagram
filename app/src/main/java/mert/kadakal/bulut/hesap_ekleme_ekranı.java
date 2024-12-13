package mert.kadakal.bulut;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class hesap_ekleme_ekranı extends AppCompatActivity {
    private EditText isim;
    private EditText parola;
    private Button btn_ekle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hesap_ekleme);

        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR"));
        Date specificDate = new Date();  // Örnek tarih, kendi tarihini burada belirleyebilirsin.
        String formattedDate = dateFormat.format(specificDate);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        isim = findViewById(R.id.editTextİsim);
        parola = findViewById(R.id.editTextParola);
        btn_ekle = findViewById(R.id.hesap_ekle_button);
        btn_ekle.setText(getIntent().getStringExtra("giriş/ekle"));

        btn_ekle.setOnClickListener(view -> {
            if (getIntent().getStringExtra("giriş/ekle").equals("Giriş Yap")) {
                db.collection("hesaplar")
                        .get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                boolean isim_kontrol = false;
                                String tem_parola = "";
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    if (document.getString("isim").equals(isim.getText().toString())) {
                                        isim_kontrol = true;
                                        tem_parola = document.getString("parola");
                                    }
                                }
                                if (isim_kontrol) {
                                    if (tem_parola.equals(parola.getText().toString())) {
                                        Toast.makeText(this, "Giriş yapıldı", Toast.LENGTH_SHORT).show();

                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putBoolean("hesap_açık_mı", true);
                                        editor.putString("hesap_ismi", isim.getText().toString());
                                        editor.putString("hesap_şifresi", parola.getText().toString());
                                        editor.apply();

                                        db.collection("hesaplar")
                                                .whereEqualTo("isim", sharedPreferences.getString("hesap_ismi", ""))
                                                .get()
                                                .addOnCompleteListener(task2 -> {
                                                    if (task2.isSuccessful()) {
                                                        if (!task2.getResult().isEmpty()) {
                                                            for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                                // "bildirimler" alanına yeni eleman ekle
                                                                db.collection("hesaplar")
                                                                        .document(document2.getId())
                                                                        .update("bildirimler", FieldValue.arrayUnion("Hesaba giriş yapıldı<bildirim>giriş<tarih>"+formattedDate));
                                                            }
                                                        }
                                                    }
                                                });

                                        onBackPressed();
                                    } else {
                                        Toast.makeText(this, "Parola yanlış girildi", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(this, "Bu isimde bir kullanıcı bulunamadı", Toast.LENGTH_SHORT).show();
                                }
                                isim.getText().clear();
                                parola.getText().clear();
                            }
                        });
            } else {
                db.collection("hesaplar")
                        .get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                boolean isim_kontrol = false;
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    if (document.getString("isim").equals(isim.getText().toString())) {
                                        isim_kontrol = true;
                                    }
                                }
                                if (isim_kontrol) {
                                    Toast.makeText(hesap_ekleme_ekranı.this, "Bu isim kullanılıyor", Toast.LENGTH_SHORT).show();
                                    isim.getText().clear();
                                    parola.getText().clear();
                                } else {
                                    Map<String, Object> eklenen_hesap = new HashMap<>();
                                    eklenen_hesap.put("isim", isim.getText().toString());
                                    eklenen_hesap.put("parola", parola.getText().toString());
                                    eklenen_hesap.put("pp_link", "");
                                    eklenen_hesap.put("bildirimler", new ArrayList<>());
                                    db.collection("hesaplar").add(eklenen_hesap);

                                    Toast.makeText(hesap_ekleme_ekranı.this, "Hesap başarıyla oluşturuldu, oturum açıldı", Toast.LENGTH_SHORT).show();

                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putBoolean("hesap_açık_mı", true);
                                    editor.putString("hesap_ismi", isim.getText().toString());
                                    editor.putString("hesap_şifresi", parola.getText().toString());
                                    editor.apply();

                                    db.collection("hesaplar")
                                            .whereEqualTo("isim", sharedPreferences.getString("hesap_ismi", ""))
                                            .get()
                                            .addOnCompleteListener(task2 -> {
                                                if (task2.isSuccessful()) {
                                                    if (!task2.getResult().isEmpty()) {
                                                        for (QueryDocumentSnapshot document2 : task2.getResult()) {
                                                            // "bildirimler" alanına yeni eleman ekle
                                                            db.collection("hesaplar")
                                                                    .document(document2.getId())
                                                                    .update("bildirimler", FieldValue.arrayUnion("Hesap oluşturuldu<bildirim>oluştur<tarih>"+formattedDate));
                                                        }
                                                    }
                                                }
                                            });

                                    onBackPressed();

                                    isim.getText().clear();
                                    parola.getText().clear();
                                }
                            }
                        });
            }
        });
    }
}
