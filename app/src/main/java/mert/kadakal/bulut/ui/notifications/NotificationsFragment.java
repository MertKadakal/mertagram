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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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
    private Button btn_hesap_ekle;
    private Button btn_giriş_yap;
    private Button btn_çıkış_yap;
    private Button btn_hesabı_sil;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    SharedPreferences sharedPreferences;

    private static final String CLIENT_ID = "b9aede4074dcb7a"; // Buraya Imgur'dan aldığınız Client-ID'yi koyun
    private static final int PICK_IMAGE_REQUEST = 1; // Dosya seçme için request kodu
    ImageView image;
    Button btn;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);


        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        sharedPreferences = requireActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE);

        btn_hesap_ekle = root.findViewById(R.id.hesap_ekle);
        btn_giriş_yap = root.findViewById(R.id.giriş_yap);
        btn_çıkış_yap = root.findViewById(R.id.çıkış_yap);
        btn_hesabı_sil = root.findViewById(R.id.hesabı_sil);

        if (sharedPreferences.getBoolean("hesap_açık_mı", false)) {
            btn_giriş_yap.setVisibility(View.INVISIBLE);
            btn_hesap_ekle.setVisibility(View.INVISIBLE);
        } else {
            btn_çıkış_yap.setVisibility(View.INVISIBLE);
            btn_hesabı_sil.setVisibility(View.INVISIBLE);
        }

        btn_hesap_ekle.setOnClickListener(view -> startActivity(new Intent(getContext(), hesap_ekleme_ekranı.class).putExtra("giriş/ekle", "Oluştur")));
        btn_giriş_yap.setOnClickListener(view -> startActivity(new Intent(getContext(), hesap_ekleme_ekranı.class).putExtra("giriş/ekle", "Giriş Yap")));
        btn_hesabı_sil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });


        ///////////////////
        image = root.findViewById(R.id.imageButton);
        btn = root.findViewById(R.id.foto_ekle);

        // Görsel yükleme işlemi için kullanıcıdan görsel seçmesini iste
        btn.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });
        //////////////////////////////

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
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Görsel başarıyla yüklendi!", Toast.LENGTH_SHORT).show());
                    // JSON yanıtını işleyebilirsiniz, örneğin:
                    String imageUrl;
                    try {
                        imageUrl = new JSONObject(responseData).getJSONObject("data").getString("link");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    Map<String, Object> imageDb = new HashMap<>();
                    imageDb.put("link", imageUrl);
                    imageDb.put("hesap", sharedPreferences.getString("hesap_ismi", ""));
                    db.collection("görseller").add(imageDb);

                    String finalImageUrl = imageUrl;
                    getActivity().runOnUiThread(() -> {
                        // Glide kullanarak görseli ImageView'a yükle
                        Glide.with(NotificationsFragment.this).load(finalImageUrl).into(image);
                    });
                } else {
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Yükleme başarısız", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}