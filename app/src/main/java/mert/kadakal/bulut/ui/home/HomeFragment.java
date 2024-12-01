package mert.kadakal.bulut.ui.home;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import mert.kadakal.bulut.R;
import okhttp3.*;

public class HomeFragment extends Fragment {

    private static final String CLIENT_ID = "b9aede4074dcb7a"; // Buraya Imgur'dan aldığınız Client-ID'yi koyun
    private static final int PICK_IMAGE_REQUEST = 1; // Dosya seçme için request kodu
    ImageView image;
    Button btn;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Fragment'in layout dosyasını inflate ediyoruz
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        image = rootView.findViewById(R.id.imageView);
        btn = rootView.findViewById(R.id.button);

        // Görsel yükleme işlemi için kullanıcıdan görsel seçmesini iste
        btn.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        return rootView;
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
                    db.collection("görseller").add(imageDb);

                    String finalImageUrl = imageUrl;
                    getActivity().runOnUiThread(() -> {
                        // Glide kullanarak görseli ImageView'a yükle
                        Glide.with(HomeFragment.this).load(finalImageUrl).into(image);
                    });
                } else {
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Yükleme başarısız", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
