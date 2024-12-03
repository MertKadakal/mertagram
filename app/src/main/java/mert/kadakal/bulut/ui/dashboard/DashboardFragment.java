package mert.kadakal.bulut.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import mert.kadakal.bulut.R;
import mert.kadakal.bulut.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<DashboardItem> items = new ArrayList<>();
    private DashboardAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ListView ayarlama
        ListView listView = binding.listDashboard;
        adapter = new DashboardAdapter(getContext(), items);


        // Firestore'dan verileri çek ve listeye ekle
        loadItemsFromFirestore();
        listView.setAdapter(adapter);

        return root;
    }

    private void loadItemsFromFirestore() {
        db.collection("görseller")  // Koleksiyon adı
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    items.clear();  // Eski verileri temizle
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String hesap = document.getString("hesap");
                        String link = document.getString("link");
                        String tarih = document.getString("tarih");
                        long beğeni = document.getLong("beğeni");

                        if (hesap != null && link != null) {
                            items.add(new DashboardItem(link, hesap, tarih, beğeni));
                        }
                    }
                    adapter.notifyDataSetChanged();  // ListView'i güncelle
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
