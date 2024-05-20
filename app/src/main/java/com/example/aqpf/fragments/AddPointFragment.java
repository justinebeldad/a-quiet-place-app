package com.example.aqpf.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.aqpf.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AddPointFragment extends DialogFragment {
    private LatLng point;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private Uri filePath;
    private Button selectImageButton;
    private ImageView imageView;
    private Uri uriImage;

    public interface OnPointAddedListener {
        void onPointAdded(LatLng point);
    }

    private OnPointAddedListener listener;

    public AddPointFragment(LatLng point, OnPointAddedListener listener) {
        this.point = point;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.storageReference = storage.getReference();
    }

    ActivityResultLauncher<Intent> openGallery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        openGallery = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == getActivity().RESULT_OK) {
                    if (result.getData() != null) {
                        uriImage = result.getData().getData();
                        try {
                            assert uriImage != null;
                            InputStream inputStream = requireActivity().getContentResolver().openInputStream(uriImage);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            imageView.setImageBitmap(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.addpoint_layout, null);
        imageView = view.findViewById(R.id.image_upload);
        EditText nameField = view.findViewById(R.id.add_name);
        EditText openingHoursField = view.findViewById(R.id.add_opening_hours);
        EditText comfortField = view.findViewById(R.id.add_comfort);
        EditText quietnessField = view.findViewById(R.id.add_quietness);
        EditText spaciousnessField = view.findViewById(R.id.add_spaciousness);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch chargingSpotsSwitch = view.findViewById(R.id.add_charging_spots);

        selectImageButton = view.findViewById(R.id.uploadButton);

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("AddPointFragment", "Upload button clicked"); // Add this line
                openGallery();
            }
        });

        builder.setView(view)
                .setPositiveButton("Submit", (dialog, id) -> {
                    String name = nameField.getText().toString();
                    String openingHours = openingHoursField.getText().toString();
                    int comfort = comfortField.getText().toString().isEmpty() ? 0 : Integer.parseInt(comfortField.getText().toString());
                    int quietness = quietnessField.getText().toString().isEmpty() ? 0 : Integer.parseInt(quietnessField.getText().toString());
                    int spaciousness = spaciousnessField.getText().toString().isEmpty() ? 0 : Integer.parseInt(spaciousnessField.getText().toString());
                    boolean chargingSpots = chargingSpotsSwitch.isChecked();

                    Map<String, Object> data = new HashMap<>();
                    data.put("name", name);
                    data.put("opening_hours", openingHours);
                    data.put("comfort_rating", comfort);
                    data.put("quietness_rating", quietness);
                    data.put("spaciousness_rating", spaciousness);
                    data.put("charging_spots", chargingSpots);
                    data.put("location", new GeoPoint(point.latitude, point.longitude));

                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        db.collection("Points").add(data).addOnSuccessListener(documentReference -> {
                            String pointId = documentReference.getId();
                            db.collection("Users").document(currentUser.getUid())
                                    .update("Contributions", FieldValue.arrayUnion(pointId));

                            listener.onPointAdded(point);

                            // Upload the image to Firebase Storage
                            if (uriImage != null) {
                                StorageReference ref = storageReference.child("images/" + pointId);
                                ref.putFile(uriImage)
                                        .addOnSuccessListener(taskSnapshot -> {
                                            Log.d("AddPointFragment", "Image uploaded successfully");

                                            ref.getDownloadUrl().addOnSuccessListener(uri -> {
                                                db.collection("Points").document(pointId)
                                                        .update("image_url", uri.toString());
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("AddPointFragment", "Image upload failed", e);
                                        });
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> Objects.requireNonNull(AddPointFragment.this.getDialog()).cancel());


        return builder.create();
    }

    public void openGallery(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        openGallery.launch(intent);
    }

}