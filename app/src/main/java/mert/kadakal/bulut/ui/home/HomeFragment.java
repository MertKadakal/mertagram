package mert.kadakal.bulut.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        sharedPreferences = getContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        // ListView'i bul
        ListView listView = rootView.findViewById(R.id.bildirimler_list);
        yok = rootView.findViewById(R.id.bildirim_yok);

        List<String> notificationList = new ArrayList<>();

        if (sharedPreferences.getBoolean("hesap_açık_mı", false)) {

            db.collection("hesaplar")
                    .whereEqualTo("isim", sharedPreferences.getString("hesap_ismi", ""))
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // "bildirimler" alanını al
                                List<String> bildirimler = (List<String>) document.get("bildirimler");
                                if (bildirimler != null) {
                                    Collections.reverse(bildirimler);
                                    notificationList.addAll(bildirimler);
                                }
                            }

                            // Adapter'i oluştur ve ListView'e bağla
                            if (notificationList.size() != 0) {
                                yok.setVisibility(View.INVISIBLE);
                                HomeAdapter adapter = new HomeAdapter(requireContext(), notificationList);
                                listView.setAdapter(adapter);
                            }
                            yok.setText("Henüz bildirim yok");
                        }
                    });
        } else {
            yok.setText("Bildirimler için oturum açın");
        }

        return rootView;
    }

}
