package com.example.aqpf;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.aqpf.fragments.SupportMapsFragment;
import com.example.aqpf.activities.Login;
import com.example.aqpf.databinding.ActivityMainBinding;
import com.example.aqpf.fragments.dashboard.DashboardFragment;
import com.example.aqpf.fragments.favourites.FavouritesFragment;
import com.example.aqpf.fragments.home.HomeFragment;
import com.example.aqpf.fragments.profile.ProfileFragment;
import com.example.aqpf.fragments.settings.SettingsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    FirebaseAuth auth;
    Button button;
    TextView textView;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new HomeFragment());

        binding.navView.setOnItemSelectedListener(item -> {

            if (item.getItemId() == R.id.navigation_home){
                replaceFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.navigation_dashboard){
                replaceFragment(new DashboardFragment());
            } else if (item.getItemId() == R.id.navigation_favourites) {
                replaceFragment(new FavouritesFragment());
            } else if (item.getItemId() == R.id.navigation_profile){
                replaceFragment(new ProfileFragment());
            } else if (item.getItemId() == R.id.navigation_settings){
                replaceFragment(new SettingsFragment());
            }
            return true;
        });

        auth = FirebaseAuth.getInstance();
        button = findViewById(R.id.logout_btn);
        textView = findViewById(R.id.user_details);
        user = auth.getCurrentUser();

        if (user == null){
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }
        else {
            textView.setText(user.getEmail());
        }

        button.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });
    }

    private void replaceFragment(Fragment fragment){

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        SupportMapsFragment supportMapsFragment = new SupportMapsFragment();
        fragmentTransaction.replace(R.id.frame_layout,fragment);
        fragmentTransaction.replace(R.id.navigation_dashboard, supportMapsFragment);
        fragmentTransaction.commit();
    }
}