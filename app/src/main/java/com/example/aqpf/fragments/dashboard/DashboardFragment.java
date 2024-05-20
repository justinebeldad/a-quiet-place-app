package com.example.aqpf.fragments.dashboard;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.aqpf.R;
import com.example.aqpf.databinding.FragmentDashboardBinding;
import com.example.aqpf.fragments.AddPointFragment;
import com.example.aqpf.fragments.PopupFragment;
import com.example.aqpf.fragments.SharedViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Map;

public class DashboardFragment extends Fragment {

    private GoogleMap mGoogleMap;
    private SharedViewModel model;
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker currentLocationMarker;
    final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private ActivityResultLauncher<String[]> multiplePermissionActivityResultLauncher;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        new ViewModelProvider(this).get(DashboardViewModel.class);

        com.example.aqpf.databinding.FragmentDashboardBinding binding = FragmentDashboardBinding.inflate(inflater, container, false);
        mapView = binding.mapView;
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    if (currentLocationMarker != null) {
                        currentLocationMarker.remove();
                    }
                    drawMarker(currentLocation);
                }
            }
        };

        try {
            MapsInitializer.initialize(requireActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        multiplePermissionActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
            if (isGranted.containsValue(false)) {
                Log.d(TAG, "At least one of the permissions was not granted, please enable permissions to ensure app functionality");
            } else {
                loadMap();
            }
        });

        if (!hasPermissions()) {
            askPermissions();
        } else {
            loadMap();
        }

        startLocationUpdates();

        return binding.getRoot();

    }
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Check if the app has location permissions
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void refreshMarkers() {
        mGoogleMap.clear();
        loadMarkers();
    }

    private boolean hasPermissions() {
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(requireActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void drawMarker(LatLng location) {
        @SuppressLint("UseCompatLoadingForDrawables") Drawable circleDrawable = getResources().getDrawable(R.drawable.circle_shape);
        BitmapDescriptor markerIcon = getMarkerIconFromDrawable(circleDrawable);

        currentLocationMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(location)
                .title("Current Location")
                .icon(markerIcon)
        );
    }

    private void askPermissions() {
        multiplePermissionActivityResultLauncher.launch(PERMISSIONS);
    }

    private void loadMap() {
        mapView.getMapAsync(googleMap -> {
            mGoogleMap = googleMap;

            if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                multiplePermissionActivityResultLauncher.launch(PERMISSIONS);
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            if (currentLocationMarker == null) {
                                currentLocationMarker = mGoogleMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));
                            } else {
                                currentLocationMarker.setPosition(currentLocation);
                            }
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        }
                    });

            loadMarkers();

            model = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
            model.getSelectedData().observe(getViewLifecycleOwner(), data -> {
                mGoogleMap.clear();
                loadMap();
                loadMarkers();
            });

            googleMap.setOnMarkerClickListener(marker -> {
                if (marker.getTag() != null) {
                    Map<String, Object> locationData = (Map<String, Object>) marker.getTag();
                    PopupFragment dialog = new PopupFragment(locationData);
                    dialog.show(getChildFragmentManager(), "DataDialogFragment");
                }
                return false;
            });

            googleMap.setOnMapClickListener(latLng -> {
                AddPointFragment dialog = new AddPointFragment(latLng, point -> {
                    // Add the marker when a new point is added
                    MarkerOptions options = new MarkerOptions().position(point);
                    mGoogleMap.addMarker(options);
                    refreshMarkers();
                });
                dialog.show(getChildFragmentManager(), "AddPointFragment");
            });
        });
    }

    private void loadMarkers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Points").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Map<String, Object> locationData = document.getData();
                    String documentId = document.getId();
                    locationData.put("documentId", documentId);

                    GeoPoint geoPoint = (GeoPoint) locationData.get("location");
                    assert geoPoint != null;
                    LatLng point = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                    Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(point).title((String) locationData.get("name")));
                    assert marker != null;
                    marker.setTag(locationData);
                }
            } else {
                Log.w(TAG, "Error getting documents.", task.getException());
            }
        });
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }
}