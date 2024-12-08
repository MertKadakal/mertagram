package mert.kadakal.bulut.ui.notifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import mert.kadakal.bulut.R;
import mert.kadakal.bulut.databinding.FragmentNotificationsBinding;
import mert.kadakal.bulut.hesap_ekleme_ekranı;
import mert.kadakal.bulut.ui.home.HomeFragment;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private ImageView pp;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    SharedPreferences sharedPreferences;
    String[] pp_or_post;

    private static final String CLIENT_ID = "b9aede4074dcb7a"; // Buraya Imgur'dan aldığınız Client-ID'yi koyun
    private static final int PICK_IMAGE_REQUEST = 1; // Dosya seçme için request kodu
    Button btn;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        pp_or_post = new String[]{""};
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        btn = root.findViewById(R.id.foto_ekle);

        sharedPreferences = requireActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);

        Button btn_hesap_ekle = root.findViewById(R.id.hesap_ekle);
        Button btn_giriş_yap = root.findViewById(R.id.giriş_yap);
        Button btn_çıkış_yap = root.findViewById(R.id.çıkış_yap);
        Button btn_hesabı_sil = root.findViewById(R.id.hesabı_sil);
        Button btn_pp_değiştir = root.findViewById(R.id.pp_değiştir);
        Button btn_pp_sil = root.findViewById(R.id.pp_sil);
        TextView isim = root.findViewById(R.id.isim);
        pp = root.findViewById(R.id.profil_resmi);

        if (sharedPreferences.getBoolean("hesap_açık_mı", false)) {
            btn_giriş_yap.setVisibility(View.INVISIBLE);
            btn_hesap_ekle.setVisibility(View.INVISIBLE);
            isim.setText(sharedPreferences.getString("hesap_ismi", ""));
            db.collection("hesaplar")
                    .whereEqualTo("isim", sharedPreferences.getString("hesap_ismi", ""))
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String imageUrl = document.getString("pp_link");
                                if (!Objects.equals(imageUrl, "") && !imageUrl.isEmpty()) {
                                    Glide.with(getContext())
                                            .load(imageUrl)
                                            .placeholder(R.drawable.loading)  // Yüklenirken gösterilecek "loading" görseli
                                            .into(pp);

                                } else {
                                    pp.setImageResource(R.drawable.person_png);
                                }
                            }
                        }
                    });

        } else {
            btn_çıkış_yap.setVisibility(View.INVISIBLE);
            btn.setVisibility(View.INVISIBLE);
            btn_hesabı_sil.setVisibility(View.INVISIBLE);
            btn_pp_değiştir.setVisibility(View.INVISIBLE);
            pp.setVisibility(View.INVISIBLE);
            btn_pp_sil.setVisibility(View.INVISIBLE);
            isim.setVisibility(View.INVISIBLE);
        }

        btn_hesap_ekle.setOnClickListener(view -> startActivity(new Intent(getContext(), hesap_ekleme_ekranı.class).putExtra("giriş/ekle", "Oluştur")));
        btn_giriş_yap.setOnClickListener(view -> startActivity(new Intent(getContext(), hesap_ekleme_ekranı.class).putExtra("giriş/ekle", "Giriş Yap")));
        btn_hesabı_sil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //hesabın görsellerini sil
                db.collection("görseller")
                        .whereEqualTo("hesap", sharedPreferences.getString("hesap_ismi", ""))
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (!task.getResult().isEmpty()) {
                                    for (DocumentSnapshot document : task.getResult()) {
                                        db.collection("görseller").document(document.getId()).delete();
                                    }
                                }
                            }
                        });

                //hesabın beğendiği görsellerin "beğenenler" arraylerinden hesabı sil
                db.collection("görseller").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                db.collection("görseller").document(document.getId())
                                        .update("beğenenler", FieldValue.arrayRemove(sharedPreferences.getString("hesap_ismi", "")));
                                db.collection("görseller").document(document.getId())
                                        .update("beğeni", FieldValue.increment(-1));
                            }
                        }
                    }
                });

                //hesabı sil
                db.collection("hesaplar")
                        .whereEqualTo("isim", sharedPreferences.getString("hesap_ismi", ""))
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                if (!task.getResult().isEmpty()) {
                                    for (DocumentSnapshot document : task.getResult()) {
                                        db.collection("hesaplar").document(document.getId()).delete();

                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putBoolean("hesap_açık_mı", false);
                                        editor.apply();

                                        Toast.makeText(getContext(), "Hesap silindi", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
            }
        });

        btn_çıkış_yap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("hesap_açık_mı", false);
                editor.apply();
                Toast.makeText(getContext(), "Hesaptan çıkış yapıldı", Toast.LENGTH_SHORT).show();
            }
        });

        btn_pp_değiştir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pp_or_post[0] = "pp";
                Toast.makeText(getContext(), "Profil resmi yükleniyor...", Toast.LENGTH_SHORT).show();
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });

        //görsel ekleme
        btn.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pp_or_post[0] = "post";
            Toast.makeText(getContext(), "Yükleniyor...", Toast.LENGTH_SHORT).show();
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        //pp sil
        btn_pp_sil.setOnClickListener(view -> {
            db.collection("hesaplar")
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.getString("isim").equals(sharedPreferences.getString("hesap_ismi", ""))) {
                                    Map<String, Object> updatedField = new HashMap<>();
                                    updatedField.put("pp_link", "");

                                    db.collection("hesaplar").document(document.getId())
                                            .update(updatedField);

                                    Toast.makeText(getContext(), "Profil resmi silindi", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Kullanıcı bir dosya seçtikten sonra bu metod çalışacak
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            File imageFile = new File(getRealPathFromURI(imageUri));
            uploadImageToImgur(imageFile);
        }
    }

    // Uri'yi gerçek dosya yoluna çevirme
    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = getContext().getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(columnIndex);
            }
        }
        return null;
    }

    private void uploadImageToImgur(File file) {
        OkHttpClient client = new OkHttpClient();

        // Görseli yüklemek için Multipart Form-data hazırlıyoruz
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(),
                        RequestBody.create(file, MediaType.parse("image/jpeg")))
                .build();

        // API'ye istek göndermek
        Request request = new Request.Builder()
                .url("https://api.imgur.com/3/image")
                .header("Authorization", "Client-ID " + CLIENT_ID)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Yükleme başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();

                    // JSON yanıtını işleyebilirsiniz, örneğin:
                    String imageUrl;
                    try {
                        imageUrl = new JSONObject(responseData).getJSONObject("data").getString("link");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    //pp değiştir veya post yükle
                    if (pp_or_post[0].equals("post")) {
                        // Belirli bir tarih oluşturma: 2 Aralık 2024, 10:30

                        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR"));
                        Date specificDate = new Date();  // Örnek tarih, kendi tarihini burada belirleyebilirsin.
                        String formattedDate = dateFormat.format(specificDate);

                        Map<String, Object> imageDb = new HashMap<>();
                        imageDb.put("link", imageUrl);
                        imageDb.put("hesap", sharedPreferences.getString("hesap_ismi", ""));
                        imageDb.put("tarih", formattedDate);
                        imageDb.put("beğeni", 0);
                        imageDb.put("yorumlar", new ArrayList<>());

                        db.collection("görseller").add(imageDb);
                        getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Görsel başarıyla yüklendi!", Toast.LENGTH_SHORT).show());
                    } else {
                        db.collection("hesaplar")
                                .get().addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            if (document.getString("isim").equals(sharedPreferences.getString("hesap_ismi", ""))) {
                                                Map<String, Object> updatedField = new HashMap<>();
                                                updatedField.put("pp_link", imageUrl);

                                                db.collection("hesaplar").document(document.getId())
                                                        .update(updatedField);
                                            }
                                        }
                                    }
                                });
                        getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Profil resmi başarıyla yüklendi!", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Yükleme başarısız", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}