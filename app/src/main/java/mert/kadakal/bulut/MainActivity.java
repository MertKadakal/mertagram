package mert.kadakal.bulut;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mert.kadakal.bulut.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 1001;
    private List<String> previousNotifications = new ArrayList<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = binding.navView;

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(navView, navController); // binding kullanılarak doğru yapılandırma

        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        boolean isFirstRun = sharedPreferences.getBoolean("isFirstRun", true);
        if (isFirstRun) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isFirstRun", false);
            editor.putBoolean("hesap_açık_mı", false);
            editor.putString("hesap_ismi", "");
            editor.putString("hesap_şifresi", "");
            editor.apply();  // apply() doğru şekilde çağrılmalı
        }
        checkNotificationPermission();

        HashMap<Integer, String> bildirimler = new HashMap<Integer, String>();
        bildirimler.put(R.drawable.image, "Yeni görsel başarıyla yüklendi");
        bildirimler.put(R.drawable.comment, "Bir gönderine yorum yapıldı");
        bildirimler.put(R.drawable.like, "Bir gönderin beğenildi");

        // Firebase Firestore'dan gerçek zamanlı bildirim kontrolü
        db.collection("hesaplar")
                .whereEqualTo("isim", sharedPreferences.getString("hesap_ismi", ""))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // İlk dokümanı al
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        DocumentReference docRef = document.getReference();

                        // Dinleyiciyi ekle
                        docRef.addSnapshotListener((snapshot, e) -> {
                            Log.d("SnapshotListener", "Dinleyici tetiklendi.");
                            if (e != null) {
                                Log.e("SnapshotListener", "Hata: " + e.getMessage());
                                return;
                            }

                            if (snapshot != null && snapshot.exists()) {
                                // "bildirimler" alanını al
                                List<String> currentNotifications = (List<String>) snapshot.get("bildirimler");

                                if (currentNotifications != null) {
                                    // Yeni bir eleman eklenip eklenmediğini kontrol et
                                    if (!previousNotifications.isEmpty() &&
                                            currentNotifications.size() > previousNotifications.size()) {

                                        int id;
                                        switch (currentNotifications.get(currentNotifications.size()-1).split("<bildirim>")[1].split("<tarih>")[0]) {
                                            case "yeni görsel":
                                                id = R.drawable.image; // "yeni görsel" için id
                                                break;
                                            case "yeni yorum":
                                                id = R.drawable.comment; // "yeni yorum" için id
                                                break;
                                            case "beğeni":
                                                id = R.drawable.like; // "beğeni" için id
                                                break;
                                            default:
                                                id = 0; // Varsayılan id
                                                break;
                                        }
                                        
                                        if (id != 0) {
                                            // Bildirim oluşturma
                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(binding.getRoot().getContext(), "yorumlar_channel")
                                                    .setSmallIcon(id)  // Bildirim ikonu
                                                    .setContentTitle("Yeni Bildirim")
                                                    .setContentText(bildirimler.get(id))
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                            // Bildirim yöneticisi
                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(binding.getRoot().getContext());
                                            notificationManager.notify(1, builder.build());
                                        }
                                    }

                                    // Önceki durumu güncelle
                                    previousNotifications = new ArrayList<>(currentNotifications);
                                }
                            }
                        });
                    }
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = binding.getRoot().getContext().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                if (notificationManager.getNotificationChannel("yorumlar_channel") == null) {
                    NotificationChannel channel = new NotificationChannel("yorumlar_channel", "Yorumlar Kanalı", NotificationManager.IMPORTANCE_DEFAULT);
                    notificationManager.createNotificationChannel(channel);
                }
            }
        }

    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // İzin istemek
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Kullanıcı izni verdi
                // Bildirim izni verildi, işlemlerinizi buraya ekleyin
            } else {
                // Kullanıcı izni reddetti
                // Gerekirse kullanıcıyı bilgilendirin
            }
        }
    }
}
