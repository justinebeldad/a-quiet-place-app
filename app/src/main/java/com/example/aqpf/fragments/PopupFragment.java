package com.example.aqpf.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.example.aqpf.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;
import java.util.Objects;

public class PopupFragment extends DialogFragment {

    private Map<String, Object> data;
    private SharedViewModel model;
    public PopupFragment(Map<String, Object> data) {
        this.data = data;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_layout, null);
        model = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        ImageView imageView = view.findViewById(R.id.point_image);
        String documentId = (String) data.get("documentId");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch the image_url from Firestore and load the image
        assert documentId != null;
        db.collection("Points").document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String imageUrl = documentSnapshot.getString("image_url");
                    Glide.with(requireContext()).load(imageUrl).into(imageView);
                });

        TextView openingHours = view.findViewById(R.id.opening_hours);
        TextView comfortRating = view.findViewById(R.id.comfort_rating);
        TextView quietnessRating = view.findViewById(R.id.quietness_rating);
        TextView spaciousnessRating = view.findViewById(R.id.spaciousness_rating);
        TextView chargingSpots = view.findViewById(R.id.charging_spots);

        EditText editName = view.findViewById(R.id.edit_name);
        EditText editOpeningHours = view.findViewById(R.id.edit_opening_hours);
        EditText editComfort = view.findViewById(R.id.edit_comfort);
        EditText editQuietness = view.findViewById(R.id.edit_quietness);
        EditText editSpaciousness = view.findViewById(R.id.edit_spaciousness);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch editChargingSpots = view.findViewById(R.id.edit_charging_spots);

        openingHours.setText("Opening Hours: " + data.get("opening_hours"));
        comfortRating.setText("Comfort: " + data.get("comfort_rating"));
        quietnessRating.setText("Quietness: " + data.get("quietness_rating"));
        spaciousnessRating.setText("Spaciousness: " + data.get("spaciousness_rating"));
        chargingSpots.setText("Charging Spots: " + ((boolean) data.get("charging_spots") ? "✔" : "✘"));

        builder.setView(view)
                .setTitle((String) data.get("name"))
                .setPositiveButton("EDIT", null)
                .setNegativeButton("CLOSE", null);

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {

            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            buttonPositive.setOnClickListener(view1 -> {

                editName.setVisibility(View.VISIBLE);
                editOpeningHours.setVisibility(View.VISIBLE);
                editComfort.setVisibility(View.VISIBLE);
                editQuietness.setVisibility(View.VISIBLE);
                editSpaciousness.setVisibility(View.VISIBLE);
                editChargingSpots.setVisibility(View.VISIBLE);

                openingHours.setVisibility(View.GONE);
                comfortRating.setVisibility(View.GONE);
                quietnessRating.setVisibility(View.GONE);
                spaciousnessRating.setVisibility(View.GONE);
                chargingSpots.setVisibility(View.GONE);

                buttonPositive.setText("CONFIRM");
                buttonPositive.setOnClickListener(view2 -> {
                    String newName = editName.getText().toString();
                    String newOpeningHours = editOpeningHours.getText().toString();
                    String newComfort = editComfort.getText().toString();
                    String newQuietness = editQuietness.getText().toString();
                    String newSpaciousness = editSpaciousness.getText().toString();
                    boolean newChargingSpots = editChargingSpots.isChecked();

                    DocumentReference docRef = db.collection("Points").document(documentId);

                    if (!newName.isEmpty()) {
                        docRef.update("name", newName);
                    }
                    if (!newOpeningHours.isEmpty()) {
                        docRef.update("opening_hours", newOpeningHours);
                    }
                    if (!newComfort.isEmpty()) {
                        docRef.update("comfort_rating", Integer.parseInt(newComfort));
                    }
                    if (!newQuietness.isEmpty()) {
                        docRef.update("quietness_rating", Integer.parseInt(newQuietness));
                    }
                    if (!newSpaciousness.isEmpty()) {
                        docRef.update("spaciousness_rating", Integer.parseInt(newSpaciousness));
                    }
                    docRef.update("charging_spots", newChargingSpots);

                    model.selectData(data);
                    dialog.dismiss();
                });

                Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setText("CANCEL");
                buttonNegative.setOnClickListener(view2 -> dialog.dismiss());
            });
        });

        return dialog;
    }
}