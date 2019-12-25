package com.ygaps.travelapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FollowMap extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private boolean mLocationPermissionGranted = false;
    private ArrayList<MemberLocaltion> memberLocaltions;
    private ArrayList<Marker> memberMarkers;
    private LocationCallback locationCallback;
    public static int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    final static String keyAPI = "AIzaSyDA0nzuUp9-hXSMcNliQrzKmlbFudlRQNQ";
    final static String keyAPIHTTP = "AIzaSyC6HrlfZJ18_N9kZBKKnqXZCCfVGqqff74";

    private BroadcastReceiver dataMessage =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra("isReceived", false))
            {
                String data = intent.getStringExtra("data");
                memberLocaltions = new Gson().fromJson(data,new TypeToken<ArrayList<MemberLocaltion>>(){}.getType());
                if (mMap != null) {
                    try {
                        updateMemberMarkers();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    int tourId, userId;
    ArrayList<StopPoint> stopPointArrayList;
    ArrayList<LatLng> latLngs;
    ImageButton imgbMyLocation;
    Polyline polyline;
    SupportMapFragment mapFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_map);
        setWidget();

        LocalBroadcastManager.getInstance(this).registerReceiver(dataMessage, new IntentFilter("MemberStatus"));

        Bundle bundle = getIntent().getExtras();
        tourId = getIntent().getIntExtra("tourId", -1);
        userId = getIntent().getIntExtra("userId", -1);
        stopPointArrayList = bundle.getParcelableArrayList("list_stop_points");

        Intent intent = new Intent(this, TrackService.class);
        intent.putExtra("tourId", tourId);
        intent.putExtra("userId", userId);
        startService(intent);

        setLatLngList();
        setEvent();
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        getLocationPermission();
        updateLocationUI();
        getDeviceLocationDraw();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), keyAPI );
        }

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment_FollowMap);

        if (autocompleteFragment != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            autocompleteFragment.setCountry("VN");
        }

        if (autocompleteFragment != null) {
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    // TODO: Get info about the selected place.
                    LatLng locationSelected = place.getLatLng();
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationSelected, 15));

                }

                @Override
                public void onError(Status status) {
                    Toast.makeText(getApplicationContext(), "ERROR", Toast.LENGTH_SHORT).show();
                }
            });
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(FollowMap.this, TrackService.class));
    }

    private void setEvent()
    {
        imgbMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLocationPermission();
                updateLocationUI();
                getDeviceLocation();
            }
        });
    }

    private  String getUrlMapApiDirection()
    {
        String origin = "origin=" + latLngs.get(0).latitude + "," + latLngs.get(0).longitude;
        String destination = "destination=" + latLngs.get(latLngs.size() - 1).latitude + "," + latLngs.get(latLngs.size() - 1).longitude;
        String waypoints = "";
        String output = "json";

        for (int i = 1; i < latLngs.size() - 1; i++)
        {
            waypoints = waypoints + latLngs.get(i).latitude + "," + latLngs.get(i).longitude;

            if(i !=  latLngs.size() - 2)
            {
                waypoints = waypoints + "|";
            }
        }

        String params;
        if(waypoints.length() > 0)
        {
            waypoints = "waypoints=" + waypoints;
            params = origin + "&" + destination + "&" + waypoints + "&key=" + keyAPIHTTP;
        }
        else
            params = origin + "&" + destination + "&key=" + keyAPIHTTP;
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + params;

        return url;
    }

    private String getDirectionFromMapApi(String url)
    {
        final OkHttpClient httpClient = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(url)
                .build();

        @SuppressLint("StaticFieldLeak") AsyncTask<Void, Void, String> asyncTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    Response response = httpClient.newCall(request).execute();

                    if(!response.isSuccessful())
                        return null;

                    return response.body().string();
                } catch (IOException e) {
                    return null;
                }
            }

        };

        String res = null;
        try {
            res = asyncTask.execute().get();
        } catch (ExecutionException e) {
            res = null;
        } catch (InterruptedException e) {
            res = null;
        }
        return res;


    }
    private void drawRoute () throws JSONException {
        String url = getUrlMapApiDirection();
        String jsonDirection = getDirectionFromMapApi(url);

        if (jsonDirection == null)
            return;

        JSONObject jsonObject = new JSONObject(jsonDirection);
        JSONArray routeArray  = jsonObject.getJSONArray("routes");
        JSONObject routes = routeArray.getJSONObject(0);
        JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
        String encodeString = overviewPolylines.getString("points");
        List<LatLng> latLngList = decodePoly(encodeString);

        polyline = mMap.addPolyline(new PolylineOptions()
                .addAll(latLngList)
                .width(12)
                .color(Color.parseColor("#FF8922"))
                .geodesic(true)
        );
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void getDeviceLocation() {

        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            Location myLocation = (Location) task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(myLocation.getLatitude(),
                                            myLocation.getLongitude()), 17));
                        } else {

                        }
                    }
                });
            }
        } catch(SecurityException e)  {

        }
    }
    private void getDeviceLocationDraw() {
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            Location myLocation = (Location) task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(myLocation.getLatitude(),
                                            myLocation.getLongitude()), 17));

                            LatLng lng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                            latLngs.add(0, lng);
                            try {
                                if (polyline != null)
                                {
                                    polyline.remove();
                                    polyline = null;
                                }
                                drawRoute();
                                for (int i = 1; i < stopPointArrayList.size() - 1; i++){
                                    StopPoint sp = stopPointArrayList.get(i);
                                    LatLng latLng = new LatLng(sp.Lat, sp.Long);
                                    switch (sp.serviceTypeId){
                                        case 1:
                                            mMap.addMarker(new MarkerOptions().position(latLng).title("Restaurant")
                                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurant_icon)));
                                            break;
                                        case 2:
                                            mMap.addMarker(new MarkerOptions().position(latLng).title("Hotel")
                                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.hotel_icon)));
                                            break;
                                        default:
                                            mMap.addMarker(new MarkerOptions().position(latLng).title("StopPoint")
                                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.stop_point_icon)));

                                    }
                                    StopPoint last = stopPointArrayList.get(stopPointArrayList.size() - 1);
                                    LatLng latLng1 = new LatLng(last.Lat, last.Long);
                                    mMap.addMarker(new MarkerOptions().position(latLng1).title("Destination")
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker_icon)));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {


                        }
                    }
                });
            }
        } catch(SecurityException e)  {

        }
    }
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                getLocationPermission();
            }
        } catch (SecurityException e)  {

        }
    }
    private void setLatLngList(){
        for (int i = 1; i < stopPointArrayList.size(); i++)
        {
            StopPoint sp = stopPointArrayList.get(i);
            LatLng lng = new LatLng(sp.Lat, sp.Long);
            latLngs.add(lng);
        }
    }
    private void updateMemberMarkers() throws JSONException {
        for (int i = 0; i < memberMarkers.size(); i++){
            memberMarkers.get(i).remove();
        }

        memberMarkers.clear();
        LatLng userLatLng = null;

        for (int i = 0; i < memberLocaltions.size(); i++){
            LatLng latLng = new LatLng(memberLocaltions.get(i).Lat, memberLocaltions.get(i).Long);
            if (mMap != null && !(memberLocaltions.get(i).id).equals("" + userId)){
                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(memberLocaltions.get(i).id)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car_icon)));
                memberMarkers.add(marker);
            }
            else{
                if ((memberLocaltions.get(i).id).equals("" + userId))
                {
                    userLatLng = latLng;
                }
            }
        }

        if (!PolyUtil.isLocationOnPath(userLatLng, latLngs, false, 100)){
            latLngs.remove(0);
            latLngs.add(0, userLatLng);
            if (polyline != null){
                polyline.remove();
                polyline = null;
                drawRoute();
            }
        }


    }
    private void setWidget()
    {
        stopPointArrayList = new ArrayList<StopPoint>();
        latLngs = new ArrayList<LatLng>();
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.followMap);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        imgbMyLocation = (ImageButton) findViewById(R.id.imgbMyLocationFollowMap);
        memberMarkers =  new ArrayList<Marker>();
    }
}
