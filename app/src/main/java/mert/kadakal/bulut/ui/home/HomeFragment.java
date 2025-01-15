package mert.kadakal.bulut.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mert.kadakal.bulut.R;
import mert.kadakal.bulut.databinding.FragmentHomeBinding;
import mert.kadakal.bulut.databinding.FragmentNotificationsBinding;

public class HomeFragment extends Fragment {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    SharedPreferences sharedPreferences;
    TextView yok;
    ImageView sil;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        sharedPreferences = getContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        ListView listView = rootView.findViewById(R.id.bildirimler_list);
        yok = rootView.findViewById(R.id.bildirim_yok);
        sil = rootView.findViewById(R.id.tüm_bildirimleri_sil);
        sil.setVisibility(View.INVISIBLE);

        if (sharedPreferences.getBoolean("hesap_açık_mı", false)) {

            db.collection("hesaplar")
                    .whereEqualTo("isim", sharedPreferences.getString("hesap_ismi", ""))
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                List<String> bildirimler = (List<String>) document.get("bildirimler");

                                List<String> notificationList = new ArrayList<>();

                                if (bildirimler != null) {
                                    Collections.reverse(bildirimler);
                                    notificationList.addAll(bildirimler);

                                    if (notificationList.size() != 0) {
                                        yok.setVisibility(View.INVISIBLE);
                                        HomeAdapter adapter = new HomeAdapter(requireContext(), notificationList);
                                        listView.setAdapter(adapter);
                                        sil.setVisibility(View.VISIBLE);
                                    }
                                }
                            }
                            yok.setText("Henüz bildirim yok");
                        }
                    });

            sil.setOnClickListener(view -> {
                if (sharedPreferences.getBoolean("hesap_açık_mı", false)) {
                    db.collection("hesaplar")
                            .whereEqualTo("isim", sharedPreferences.getString("hesap_ismi", ""))
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        // Belge kimliğini al
                                        String documentId = document.getId();

                                        // "bildirimler" alanını temizle
                                        db.collection("hesaplar").document(documentId)
                                                .update("bildirimler", new ArrayList<>())
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(getContext(), "Bildirimler temizlendi", Toast.LENGTH_SHORT).show();

                                                    // ListView'i güncelle
                                                    sil.setVisibility(View.INVISIBLE);
                                                    yok.setVisibility(View.VISIBLE);
                                                    yok.setText("Henüz bildirim yok");
                                                    listView.setAdapter(null);
                                                });
                                    }
                                }
                            });
                }
            });


        } else {
            yok.setText("Bildirimler için oturum açın");
        }

        return rootView;
    }

}
