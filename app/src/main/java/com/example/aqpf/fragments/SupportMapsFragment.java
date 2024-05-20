package com.example.aqpf.fragments;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Map;

public class SupportMapsFragment extends Fragment {

    private final OnMapReadyCallback callback = new OnMapReadyCallback() {

        @Override
        public void onMapReady(@NonNull GoogleMap googleMap) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("cities").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Map<String, Object> locationData = document.getData();
                        GeoPoint geoPoint = (GeoPoint) locationData.get("location");
                        assert geoPoint != null;
                        LatLng point = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                        googleMap.addMarker(new MarkerOptions().position(point).title((String) locationData.get("name")));
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            });
        }
    };
}