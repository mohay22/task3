package com.example.task3;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {
    private EditText name;
    private Button btnSave, btnAddRecipe;
    private ImageView profileImage;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;

    private static final int IMAGE_PICKER_REQUEST = 1001; // For image selection

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        name = findViewById(R.id.name);
        btnSave = findViewById(R.id.btnSave);
        btnAddRecipe = findViewById(R.id.btnAddRecipe);
        profileImage = findViewById(R.id.profileImage);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recipeList = new ArrayList<>();
        recipeAdapter = new RecipeAdapter(recipeList);
        recyclerView.setAdapter(recipeAdapter);

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            try {
                loadUserProfile(user.getUid());
                loadUserRecipes(user.getUid());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
            }
        }

        profileImage.setOnClickListener(v -> openImagePicker());
// Find the Login button and set click listener
        Button btnLogin = findViewById(R.id.btnLogout);
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnSave.setOnClickListener(v -> {
            try {
                saveUserProfile();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving profile", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddRecipe.setOnClickListener(v -> startActivity(new Intent(UserProfileActivity.this, AddRecipeActivity.class)));
    }

    private void loadUserRecipes(String uid) {
    }

    private void loadUserProfile(String userId) throws Exception {
        DocumentReference docRef = db.collection("users").document(userId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String userName = documentSnapshot.getString("name");
                String profileBase64 = documentSnapshot.getString("profileImage");
                name.setText(userName);
                if (profileBase64 != null) {
                    Bitmap bitmap = convertBase64ToBitmap(profileBase64);
                    if (bitmap != null) {
                        profileImage.setImageBitmap(bitmap);
                    } else {
                        Toast.makeText(UserProfileActivity.this, "Error displaying profile image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }).addOnFailureListener(e -> handleFirestoreError("Error loading user profile", e));
    }

    private void saveUserProfile() throws Exception {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) throw new Exception("User is not authenticated");

        String userId = user.getUid();
        String userName = name.getText().toString().trim();

        if (userName.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] profileImageBase64 = {null};
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        profileImageBase64[0] = documentSnapshot.getString("profileImage");
                    }
                    Uri imageUri = (Uri) profileImage.getTag();
                    if (imageUri != null) {
                        try {
                            profileImageBase64[0] = convertImageToBase64(imageUri);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("name", userName);
                    if (profileImageBase64[0] != null) {
                        userMap.put("profileImage", profileImageBase64[0]);
                    }

                    db.collection("users").document(userId).set(userMap)
                            .addOnSuccessListener(aVoid -> Toast.makeText(UserProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> handleFirestoreError("Failed to update profile", e));
                })
                .addOnFailureListener(e -> handleFirestoreError("Error fetching user data", e));
    }

    private String convertImageToBase64(Uri imageUri) throws Exception {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (FileNotFoundException e) {
            throw new Exception("Image not found: " + e.getMessage());
        }
    }

    private Bitmap convertBase64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                profileImage.setTag(selectedImageUri);
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImageUri));
                    profileImage.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleFirestoreError(String message, Exception e) {
        Log.e("FirestoreError", message, e);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
