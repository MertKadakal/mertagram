package mert.kadakal.bulut;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class YorumlarAdapter extends BaseAdapter {

    private final Context context;
    private final List<String> items;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    SharedPreferences sharedPreferences;
    private TextView yorum_içerigi;
    private Button yorumu_sil;
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
        if (!(sharedPreferences.getBoolean("hesap_açık_mı", false) && sharedPreferences.getString("hesap_ismi", "").equals(item.split("<br>")[0]))) {
            yorumu_sil.setVisibility(View.INVISIBLE);
        }

        String yorum_içeriği_str = item;
        yorum_içerigi = convertView.findViewById(R.id.yorum_içeriği);
        yorum_içerigi.setText(Html.fromHtml(item));

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
                                                yorumlarList.remove(i);

                                                db.collection("görseller")
                                                        .document(document.getId())
                                                        .update("yorumlar", yorumlarList);

                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        });
            }

        });

        return convertView;
    }
}
