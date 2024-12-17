package mert.kadakal.bulut.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.checkerframework.checker.i18nformatter.qual.I18nMakeFormat;

import java.util.ArrayList;
import java.util.List;

import mert.kadakal.bulut.R;
import mert.kadakal.bulut.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<DashboardItem> items = new ArrayList<>();
    private DashboardAdapter adapter;
    private TextView post_yok;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        ListView listView = binding.listDashboard;
        adapter = new DashboardAdapter(getContext(), items);

        post_yok = root.findViewById(R.id.post_yok);

        // Firestore'dan verileri çek ve listeye ekle
        loadItemsFromFirestore();
        listView.setAdapter(adapter);

        return root;
    }

    private void loadItemsFromFirestore() {
        db.collection("görseller")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    items.clear();
                    if (queryDocumentSnapshots.size() == 1) post_yok.setVisibility(View.VISIBLE);

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String hesap = document.getString("hesap");
                        String link = document.getString("link");
                        String tarih = document.getString("tarih");
                        long beğeni = document.getLong("beğeni");

                        if (hesap != null && link != null) {
                            items.add(new DashboardItem(link, hesap, tarih, beğeni));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
