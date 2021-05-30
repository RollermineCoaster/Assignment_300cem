package com.example.assignment_300cem;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.VoiceInteractor;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private AppBarConfiguration mAppBarConfiguration;
    private Menu menu;
    private Toolbar toolbar;
    private String url = "https://district-buddy.herokuapp.com";
    private OkHttpClient client;
    private NavController navController;
    private SharedPreferences sharedPreferences;
    private Map<Integer, JsonObject> users;
    private Map<Integer, JsonObject> posts;
    private Map<Integer, JsonObject> postsInDistrict;
    private int[] currentPostsIndex;
    private String[] areas;
    private String[] districts;
    private double[][] districts_location;
    private int currentDistrict = 0;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationManager locationManager;
    private LocationCallback locationCallback;
    private Geocoder geocoder;
    private SensorManager sensorManager;
    private Sensor accel;
    private float lastX;
    private float lastY;
    private float lastZ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        geocoder = new Geocoder(this, Locale.getDefault());

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        client = new OkHttpClient().newBuilder().addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)).build();
        users = new HashMap<Integer, JsonObject>();
        posts = new HashMap<Integer, JsonObject>();
        postsInDistrict = new HashMap<Integer, JsonObject>();
        areas = getResources().getStringArray(R.array.areas);
        districts = getResources().getStringArray(R.array.districts);
        districts_location = new double[19][2];

        String d = "";
        try {
            for (int i = 1; i < districts.length; i++) {
                String name = "";
                switch (i) {
                    case 3:
                        name = "Hong Kong, 東區";
                        break;
                    case 4:
                        name = "南區";
                        break;
                    case 8:
                        name = "黃大仙";
                        break;
                    case 14:
                        name = "北區";
                        break;
                    default:
                        name = districts[i];
                }
                List<Address> list = geocoder.getFromLocationName(name, 1);
                if (list.size() > 0) {
                    districts_location[i][0] = list.get(0).getLatitude();
                    districts_location[i][1] = list.get(0).getLongitude();
                } else {
                    Log.d("geocoder", name + " not found");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateData("users");
        updateData("posts");
        updateData("postsInDistrict");

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);

        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        Context context = this;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!sharedPreferences.contains("token")) {
                    Snackbar.make(view, R.string.pls_login, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else if (currentDistrict == 0) {
                    Snackbar.make(view, R.string.pls_select_district, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.new_post);
                    View dialogView = getLayoutInflater().inflate(R.layout.add_post, null);
                    builder.setView(dialogView);
                    builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            RequestBody body = new FormBody.Builder()
                                    .add("token", sharedPreferences.getString("token", ""))
                                    .add("district_id", currentDistrict + "")
                                    .add("content", ((EditText) dialogView.findViewById(R.id.add_post_content)).getText().toString())
                                    .build();
                            Request request = new Request.Builder()
                                    .url(url + "/newpost")
                                    .post(body)
                                    .build();
                            Call call = client.newCall(request);
                            call.enqueue(new Callback() {
                                @Override
                                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                    Log.d("okHttp", e.toString());
                                }

                                @Override
                                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (response.code() == 401) {
                                                Toast.makeText(view.getContext(), R.string.pls_login, Toast.LENGTH_SHORT).show();
                                            } else if (response.code() == 201) {
                                                Toast.makeText(view.getContext(), R.string.post_ok_msg, Toast.LENGTH_SHORT).show();
                                                updateData("posts");
                                                updateData("postsInDistrict");
                                            }
                                        }
                                    });
                                    response.body().close();
                                }
                            });
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                updateNavHeader();
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.mainFragment)
                .setDrawerLayout(drawer)
                .build();

        ListView drawerList = findViewById(R.id.drawer_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, districts);
        drawerList.setAdapter(adapter);
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                currentDistrict = i;
                updateData("users");
                updateData("posts");
                updateData("postsInDistrict");
                drawer.close();
            }
        });

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                if (destination.getId() == R.id.mainFragment) {
                    fab.setVisibility(View.VISIBLE);
                    if (menu != null) {
                        menu.findItem(R.id.action_refresh).setVisible(true);
                        menu.findItem(R.id.action_settings).setVisible(true);
                        updateData("users");
                        updateData("posts");
                        updateData("postsInDistrict");
                    }
                } else {
                    fab.setVisibility(View.GONE);
                    if (menu != null) {
                        menu.findItem(R.id.action_refresh).setVisible(false);
                        menu.findItem(R.id.action_settings).setVisible(false);
                    }
                }
            }
        });
    }

    public double distance(double[] x, double[] y) {
        return Math.sqrt(Math.pow(Math.abs(x[0] - y[0]), 2) + Math.pow(Math.abs(x[1] - y[1]), 2));
    }

    public void updateAddress() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Snackbar.make(findViewById(R.id.nav_host_fragment), R.string.gps_off, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            Snackbar.make(findViewById(R.id.nav_host_fragment), R.string.pls_perm, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            Snackbar.make(findViewById(R.id.nav_host_fragment), R.string.update_loc, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    if (locationResult != null) {
                        try {
                            Location location = locationResult.getLastLocation();
                            double[] loc = {location.getLatitude(), location.getLongitude()};
                            int closestIndex = 1;
                            for (int i = 2; i < districts_location.length; i++) {
                                if (distance(loc, districts_location[i]) < distance(loc, districts_location[closestIndex])){
                                    closestIndex = i;
                                }
                            }
                            currentDistrict = closestIndex;
                            updateData("users");
                            updateData("postsInDistrict");
                            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    fusedLocationProviderClient.removeLocationUpdates(this);
                }
            };
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(1000);
            locationRequest.setFastestInterval(1000);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        } else {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            float deltaX = Math.abs(x - lastX);
            float deltaY = Math.abs(y - lastY);
            float deltaZ = Math.abs(z - lastZ);
            if (deltaX + deltaY + deltaZ > 5) {
                updateAddress();
            }
            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }

    @Override
    protected void onResume() {
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        super.onResume();
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                updateData("users");
                updateData("posts");
                updateData("postsInDistrict");
                return true;
            case R.id.action_settings:
                navController.navigate(R.id.settingsFragment);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void updateData(String type) {
        Map<Integer, JsonObject> data;
        switch (type) {
            case "users":
                data = this.users;
                break;
            case "posts":
                data = this.posts;
                break;
            case "postsInDistrict":
                data = this.postsInDistrict;
                break;
            default:
                data = null;
        }
        if (data != null) {
            data.clear();
            Request request;
            if (type.equals("postsInDistrict")) {
                request = new Request.Builder()
                        .url(url + "/district/" + currentDistrict + "/posts")
                        .build();
            } else {
                request = new Request.Builder()
                        .url(url + "/get/" + type)
                        .build();
            }
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Log.d("okHttp", e.toString());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (response.code() == 200) {
                        try {
                            JsonArray jsonArray = JsonParser.parseString(response.body().string()).getAsJsonArray();
                            for (JsonElement jsonElement : jsonArray) {
                                JsonObject jsonObject = jsonElement.getAsJsonObject();
                                data.put(jsonObject.get("id").getAsInt(), jsonObject);
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updatePostList(currentDistrict);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    response.body().close();
                }
            });
        }
    }

    public void updatePostList(int districtId) {
        try {

            if (districtId == 0) {
                toolbar.setTitle(R.string.all_posts);
                currentPostsIndex = new int[posts.size()];
                List<HashMap<String, String>> posts_list = new ArrayList<HashMap<String, String>>();
                int i = 0;
                for (Map.Entry<Integer, JsonObject> entry : posts.entrySet()) {

                    int postID = entry.getKey();
                    String poster_name = users.get(entry.getValue().get("poster_id").getAsInt()).get("name").getAsString();
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(entry.getValue().get("timestamp").getAsString());
                    String content = entry.getValue().get("content").getAsString();

                    currentPostsIndex[i] = postID;
                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    hashMap.put("title", content);
                    hashMap.put("subTitle", poster_name + "    " + offsetDateTime.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    posts_list.add(hashMap);
                    i++;
                }
                ListAdapter listAdapter = new SimpleAdapter(
                        this,
                        posts_list,
                        android.R.layout.simple_list_item_2,
                        new String[]{"title", "subTitle"},
                        new int[]{android.R.id.text1, android.R.id.text2});
                ListView mainList = findViewById(R.id.main_list);
                mainList.setAdapter(listAdapter);
            } else {
                toolbar.setTitle(districts[districtId] + getResources().getString(R.string.buddy));
                currentPostsIndex = new int[postsInDistrict.size()];
                List<HashMap<String, String>> posts_list = new ArrayList<HashMap<String, String>>();
                int i = 0;
                for (Map.Entry<Integer, JsonObject> entry : postsInDistrict.entrySet()) {

                    int postID = entry.getKey();
                    String poster_name = users.get(entry.getValue().get("poster_id").getAsInt()).get("name").getAsString();
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(entry.getValue().get("timestamp").getAsString());
                    String content = entry.getValue().get("content").getAsString();

                    currentPostsIndex[i] = postID;
                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    hashMap.put("title", content);
                    hashMap.put("subTitle", poster_name + "    " + offsetDateTime.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    posts_list.add(hashMap);
                    i++;
                }
                ListAdapter listAdapter = new SimpleAdapter(
                        this,
                        posts_list,
                        android.R.layout.simple_list_item_2,
                        new String[]{"title", "subTitle"},
                        new int[]{android.R.id.text1, android.R.id.text2});
                ListView mainList = findViewById(R.id.main_list);
                mainList.setAdapter(listAdapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loginBtnOnClick(View view) {
        if (sharedPreferences.contains("token")) {
            //logout
            ((Button) view).setText(R.string.login);
            ((ImageView) findViewById(R.id.user_icon)).setImageResource(R.drawable.noicon);
            ((TextView) findViewById(R.id.username_drawer)).setText(R.string.pls_login);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("token");
            editor.apply();
            Snackbar.make(view.getRootView(), R.string.logout_msg, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            //show login
            navController.navigate(R.id.loginFragment);
            DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
            drawerLayout.closeDrawers();
        }
    }

    public void updateNavHeader() {
        if (sharedPreferences.contains("token")) {
            //login
            ((Button) findViewById(R.id.login_drawer)).setText(R.string.logout);
            ((ImageView) findViewById(R.id.user_icon)).setImageResource(R.drawable.noicon);
            JsonObject user = users.get(sharedPreferences.getInt("id", 0));
            if (user != null) {
                ((TextView) findViewById(R.id.username_drawer)).setText(user.get("name").getAsString());
            }
        } else {
            //not login
            ((Button) findViewById(R.id.login_drawer)).setText(R.string.login);
            ((ImageView) findViewById(R.id.user_icon)).setImageResource(R.drawable.noicon);
            ((TextView) findViewById(R.id.username_drawer)).setText(R.string.pls_login);
        }
    }

    public void switchReg(View view) {
        LinearLayout loginPanel = findViewById(R.id.login_panel);
        LinearLayout regPanel = findViewById(R.id.register_panel);
        TextView msg = findViewById(R.id.login_reg_msg);
        if (loginPanel.getVisibility() == View.VISIBLE) {
            loginPanel.setVisibility(View.GONE);
            regPanel.setVisibility(View.VISIBLE);
            msg.setText(R.string.registered);
            ((Button) view).setText(R.string.login);
        } else {
            loginPanel.setVisibility(View.VISIBLE);
            regPanel.setVisibility(View.GONE);
            msg.setText(R.string.no_account);
            ((Button) view).setText(R.string.register);
        }
    }

    public void login(View view) {
        EditText phone = findViewById(R.id.login_phone);
        EditText pwd = findViewById(R.id.login_pwd);
        phone.clearFocus();
        pwd.clearFocus();
        if (phone.getText().length() == 0 || pwd.getText().length() == 0) {
            Toast.makeText(this, R.string.login_missing_msg, Toast.LENGTH_SHORT).show();
        } else if (phone.getText().length() != 8) {
            Toast.makeText(this, R.string.invalid_phone, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.login_wait_msg, Toast.LENGTH_SHORT).show();
            RequestBody body = new FormBody.Builder()
                    .add("phone", phone.getText().toString())
                    .add("pwd", pwd.getText().toString())
                    .build();
            Request request = new Request.Builder()
                    .url(url + "/login")
                    .post(body)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Log.d("okHttp", e.toString());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.code() == 404) {
                                Toast.makeText(view.getContext(), R.string.login_wrong_msg, Toast.LENGTH_SHORT).show();
                            } else if (response.code() == 200) {
                                Toast.makeText(view.getContext(), R.string.login_ok_msg, Toast.LENGTH_SHORT).show();
                                navController.navigateUp();
                                updateNavHeader();
                            }
                        }
                    });
                    if (response.code() == 200) {
                        try {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            JSONObject jsonObject = new JSONObject(response.body().string());
                            editor.putInt("id", jsonObject.getInt("id"));
                            editor.putString("token", jsonObject.getString("token"));
                            editor.apply();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    response.body().close();
                }
            });
        }
    }

    public void register(View view) {
        EditText username = findViewById(R.id.register_username);
        EditText phone = findViewById(R.id.register_phone);
        EditText pwd1 = findViewById(R.id.register_pwd1);
        EditText pwd2 = findViewById(R.id.register_pwd2);
        username.clearFocus();
        phone.clearFocus();
        pwd1.clearFocus();
        pwd2.clearFocus();
        if (username.getText().length() == 0 || phone.getText().length() == 0 || pwd1.getText().length() == 0 || pwd1.getText().length() == 0) {
            Toast.makeText(this, R.string.register_missing_msg, Toast.LENGTH_SHORT).show();
        } else if (phone.getText().length() != 8) {
            Toast.makeText(this, R.string.invalid_phone, Toast.LENGTH_SHORT).show();
        } else if (!pwd1.getText().toString().equals(pwd2.getText().toString())) {
            Toast.makeText(this, R.string.pwd_confirm_notmatch, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.register_wait_msg, Toast.LENGTH_SHORT).show();
            RequestBody body = new FormBody.Builder()
                    .add("name", username.getText().toString())
                    .add("phone", phone.getText().toString())
                    .add("pwd", pwd1.getText().toString())
                    .build();
            Request request = new Request.Builder()
                    .url(url + "/reg")
                    .post(body)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Log.d("okHttp", e.toString());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.code() == 409) {
                                Toast.makeText(view.getContext(), R.string.register_fail_msg, Toast.LENGTH_SHORT).show();
                            } else if (response.code() == 201) {
                                Toast.makeText(view.getContext(), R.string.register_ok_msg, Toast.LENGTH_SHORT).show();
                                navController.navigateUp();
                            }
                        }
                    });
                    response.body().close();
                }
            });
        }
    }

    public void grantPerm(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            Snackbar.make(view, R.string.granted_perm, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }
}