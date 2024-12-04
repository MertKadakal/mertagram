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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
                Intent intent = new Intent(yorumlar.this, mert.kadakal.bulut.yorum_ekle.class)
                        .putExtra("link", getIntent().getStringExtra("görsel_link"));
                startActivity(intent);
            }
        });
    }
}
