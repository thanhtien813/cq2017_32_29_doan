package com.ygaps.travelapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.Intent;


import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;

import android.location.Address;
import android.location.Geocoder;


import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.ygaps.travelapp.ListStopPoint.JSON;
import static com.ygaps.travelapp.ListTourActivity.token;
import static java.util.Locale.getDefault;

public class StopPointMap extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private boolean mLocationPermissionGranted = false;
    public static int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    public static int ID;
    final static String keyAPI = "AIzaSyDA0nzuUp9-hXSMcNliQrzKmlbFudlRQNQ";
    final static String keyAPIHTTP = "AIzaSyC6HrlfZJ18_N9kZBKKnqXZCCfVGqqff74";
    public static final int REQUEST_CODE = 12345;
    LinearLayout btnCreateStopPoint;
    SupportMapFragment mapFragment;
    Geocoder geocoder;
    Dialog dialog;
    Calendar calendar;
    int day, month, year,hour, minute;
    DatePickerDialog datePickerDialog;
    TimePickerDialog timePickerDialog;

    TextView txtArriveTime, txtArriveDate, txtLeaveTime, txtLeaveDate;
    Spinner spnProvince, spnService;
    EditText edtStopPointName, edtAddress,edtMinCost, edtMaxCost;

    ArrayList<StopPoint> stopPointArrayList;
    ArrayList<StopPoint> pendingResult;
    ArrayList<Marker> markerArrayList;

    boolean isDialogShowing = false;



    ArrayList<LatLng> latLngs;
    ImageButton imgbMyLocation;
    ImageButton imgbMenuStopPoint;
    ImageButton imgbAddRecommendedSP;
    Polyline polyline;
    final int REQUEST_CODE_MENU = 1999;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop_point_map);
        setWidget();

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        ID = bundle.getInt("id", -1);
        stopPointArrayList = bundle.getParcelableArrayList("list_stop_point");
        LatLng latLng1 = new LatLng(stopPointArrayList.get(0).Lat, stopPointArrayList.get(0).Long);
        latLngs.add(latLng1);
        LatLng latLng2 = new LatLng(stopPointArrayList.get(1).Lat, stopPointArrayList.get(1).Long);
        latLngs.add(latLng2);
        setEvent();
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        getLocationPermission();

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.get(0), 17));

        Marker ori = mMap.addMarker(new MarkerOptions().position(latLngs.get(0)).title("Origin")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker_icon)));

        Marker des = mMap.addMarker(new MarkerOptions().position(latLngs.get(1)).title("Destination")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker_icon)));
        markerArrayList.add(ori);
        markerArrayList.add(des);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), keyAPI );
        }

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

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
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationSelected, 17));

                }

                @Override
                public void onError(Status status) {
                    Toast.makeText(getApplicationContext(), "ERROR", Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION){
                mLocationPermissionGranted = true;
                try {
                    drawRoute();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED){
                Toast.makeText(getApplicationContext(), "Permission Denied!", Toast.LENGTH_SHORT).show();
                finish();
            }
            mLocationPermissionGranted = false;
        }
    }

    private void setEvent()
    {

        btnCreateStopPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DisplayPopupDialog();
            }
        });

        imgbMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLocationPermission();
                updateLocationUI();
                getDeviceLocation();
            }
        });

        imgbMenuStopPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initTrackNumber();
                Intent intent = new Intent(StopPointMap.this, ListStopPoint.class);
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("list_stop_points", stopPointArrayList);
                intent.putExtras(bundle);
                startActivityForResult(intent, REQUEST_CODE_MENU);
            }
        });

        imgbAddRecommendedSP.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try
                {
                    final OkHttpClient httpClient = new OkHttpClient();

                    final JSONObject jsonObject = new JSONObject();
                    JSONArray coordList = new JSONArray();

                    jsonObject.put("hasOneCoordinate", false);

                    JSONArray coordinateSet = new JSONArray();
                    int size = stopPointArrayList.size();

                    JSONObject begin = new JSONObject();
                    JSONObject end = new JSONObject();
                    begin.put("lat",stopPointArrayList.get(0).Lat);
                    begin.put("long",stopPointArrayList.get(0).Long);

                    end.put("lat",stopPointArrayList.get(size - 1).Lat);
                    end.put("long",stopPointArrayList.get(size - 1).Long);

                    coordinateSet.put(begin);
                    coordinateSet.put(end);

                    JSONObject CoordinateSet = new JSONObject();
                    CoordinateSet.put("coordinateSet",coordinateSet);
                    coordList.put(CoordinateSet);
                    jsonObject.put("coordList",coordList);


                    RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
                    final Request request = new Request.Builder()
                            .url(MainActivity.API_ADDR + "tour/suggested-destination-list")
                            .addHeader("Authorization",token)
                            .post(body)
                            .build();

                    @SuppressLint("StaticFieldLeak") AsyncTask <Void, Void, String> asyncTask = new AsyncTask<Void, Void, String>() {
                        @Override
                        protected String doInBackground(Void... voids) {
                            try {
                                Response response = httpClient.newCall(request).execute();

                                if(!response.isSuccessful())
                                    return null;

                                return response.body().string();

                            } catch (IOException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }

                        @Override
                        protected void onPostExecute(String s) {
                            if (s != null){
                                try {
                                    Intent intent = new Intent(StopPointMap.this, RecommendedStopPointActivity.class);
                                    intent.putExtra("recommendedSP",s);
                                    intent.putExtra("alreadyCheckedSP",pendingResult);
                                    startActivityForResult(intent, REQUEST_CODE);
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };

                    asyncTask.execute();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }



            }
        }));
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
        List <LatLng> latLngList = decodePoly(encodeString);

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
            try {
                drawRoute();
            } catch (JSONException e) {
                e.printStackTrace();
            }
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

    private void setWidget()
    {
        imgbMyLocation = (ImageButton) findViewById(R.id.imgbMyLocation);
        imgbMenuStopPoint = (ImageButton) findViewById(R.id.imgbMenuStopPoint);
        latLngs = new ArrayList<>();
        geocoder = new Geocoder(this, Locale.getDefault());
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        btnCreateStopPoint = (LinearLayout) findViewById(R.id.layoutCreateStopPoint);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        polyline = null;
        stopPointArrayList = new ArrayList<>();

        imgbAddRecommendedSP = (ImageButton) findViewById(R.id.imgbAddRecommendedSP);
        pendingResult = new ArrayList<>();
        markerArrayList = new ArrayList<>();

    }

    int lastIndex = -1;
    public void DisplayPopupDialog()
    {

        isDialogShowing = true;
        dialog = new Dialog(StopPointMap.this);
        dialog.setContentView(R.layout.stoppoint_info_popup);

        spnProvince = (Spinner) dialog.findViewById(R.id.stop_point_province_Category);
        spnService = (Spinner) dialog.findViewById(R.id.stop_point_serviceType_Category);
        ImageButton exitButton = (ImageButton) dialog.findViewById(R.id.stop_point_exit_button);
        Button saveButton =  (Button) dialog.findViewById(R.id.stop_point_OK_button);
        txtArriveDate =(TextView) dialog.findViewById(R.id.stop_point_arrive_date);
        txtArriveTime =(TextView) dialog.findViewById(R.id.stop_point_arrive_time);
        txtLeaveDate =(TextView) dialog.findViewById(R.id.stop_point_leave_date);
        txtLeaveTime =(TextView) dialog.findViewById(R.id.stop_point_leave_time);
        edtAddress =(EditText) dialog.findViewById(R.id.stop_point_address_Content);
        edtMinCost = (EditText)dialog.findViewById(R.id.stop_point_min_cost_Content);
        edtStopPointName = (EditText)dialog.findViewById(R.id.stop_point_name_Content);
        edtMaxCost = (EditText)dialog.findViewById(R.id.stop_point_max_cost_Content);

        String[] arrProvinceName = getResources().getStringArray(R.array.list_province_name);
        String[] arrProvinceId = getResources().getStringArray(R.array.list_province_id);
        String[] arrServiceName = getResources().getStringArray(R.array.list_service);
        ArrayList<String> arrListProvince = new ArrayList<String>(Arrays.asList(arrProvinceName));
        ArrayList<String> arrListService = new ArrayList<String>(Arrays.asList(arrServiceName));

        ArrayAdapter<String> adapter_province = new ArrayAdapter(StopPointMap.this,android.R.layout.simple_spinner_item,arrListProvince);
        adapter_province.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        spnProvince.setAdapter(adapter_province);
        spnProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        ArrayAdapter<String> adapter_service = new ArrayAdapter(StopPointMap.this,android.R.layout.simple_spinner_item,arrListService);
        adapter_service.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        spnService.setAdapter(adapter_service);
        spnService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long timeArrive_ms = 0;
                long timeLeave_ms = 0;
                int provinceId, serviceId;
                String minCost, maxCost;

                provinceId = spnProvince.getSelectedItemPosition() + 1;
                serviceId = spnService.getSelectedItemPosition() + 1;
                String strStopPointName = edtStopPointName.getText().toString();
                String strAddress = edtAddress.getText().toString();
                minCost = edtMinCost.getText().toString();
                maxCost = edtMaxCost.getText().toString();
                String strArriveTime = txtArriveTime.getText().toString();
                String strArriveDate = txtArriveDate.getText().toString();
                String strLeaveTime = txtLeaveTime.getText().toString();
                String strLeaveDate = txtLeaveDate.getText().toString();

                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy, HH:mm");
                formatter.setLenient(false);
                String timeArrived = strArriveDate + ", " + strArriveTime;
                String timeLeave = strLeaveDate + ", " + strLeaveTime;

                try {
                    Date timeArrivedDate = formatter.parse(timeArrived);
                    Date timeLeaveDate = formatter.parse(timeLeave);
                    timeArrive_ms = timeArrivedDate.getTime();
                    timeLeave_ms = timeLeaveDate.getTime();

                } catch (ParseException e) {
                    e.printStackTrace();
                }

                StopPoint sp = new StopPoint();
                sp.id = null;
                sp.arrivalAt = timeArrive_ms;
                sp.leaveAt = timeLeave_ms;
                sp.avatar = null;
                sp.Long = latLngs.get(lastIndex).longitude;
                sp.Lat = latLngs.get(lastIndex).latitude;
                sp.minCost = minCost;
                sp.maxCost = maxCost;
                sp.name = strStopPointName;
                sp.provinceId = provinceId;
                sp.serviceTypeId = serviceId;
                sp.address = strAddress;

                if (stopPointArrayList.size() <= 1)
                    stopPointArrayList.add(sp);
                else
                    stopPointArrayList.add(stopPointArrayList.size() - 1,sp);

                dialog.dismiss();
                isDialogShowing = false;
                try {
                    if (latLngs.size() > 2)
                    {
                        Marker marker;
                        switch (stopPointArrayList.get(lastIndex).serviceTypeId) {
                            case 1:
                                marker = mMap.addMarker(new MarkerOptions().position(latLngs.get(lastIndex)).title("Restaurant")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurant_icon)));
                                break;
                            case 2:
                                marker = mMap.addMarker(new MarkerOptions().position(latLngs.get(lastIndex)).title("Hotel")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.hotel_icon)));
                                break;
                            default:
                                marker = mMap.addMarker(new MarkerOptions().position(latLngs.get(lastIndex)).title("Stop Point")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.stop_point_icon)));
                        }

                        markerArrayList.add(markerArrayList.size() - 1 ,marker);

                        if (polyline != null)
                        {
                            polyline.remove();
                            polyline = null;
                        }
                        drawRoute();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });


        txtArriveDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar = Calendar.getInstance();
                year = calendar.get(Calendar.YEAR);
                month = calendar.get(Calendar.MONTH);
                day = calendar.get(Calendar.DAY_OF_MONTH);
                minute = calendar.get(Calendar.MINUTE);
                hour = calendar.get(Calendar.HOUR);
                datePickerDialog = new DatePickerDialog(StopPointMap.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                        String chosenDay = Integer.toString(dayOfMonth);
                        String chosenMonth = Integer.toString(month + 1);
                        if (dayOfMonth < 10)
                            chosenDay = "0" + chosenDay;
                        if (month + 1 < 10)
                            chosenMonth = "0" + chosenMonth;
                        txtArriveDate.setText(chosenDay + "/" + chosenMonth + "/" + year);
                    }
                }, year, month, day);

                datePickerDialog.show();

            }
        });

        txtLeaveDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar = Calendar.getInstance();
                year = calendar.get(Calendar.YEAR);
                month = calendar.get(Calendar.MONTH);
                day = calendar.get(Calendar.DAY_OF_MONTH);
                minute = calendar.get(Calendar.MINUTE);
                hour = calendar.get(Calendar.HOUR);
                datePickerDialog = new DatePickerDialog(StopPointMap.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                        String chosenDay = Integer.toString(dayOfMonth);
                        String chosenMonth = Integer.toString(month + 1);
                        if (dayOfMonth < 10)
                            chosenDay = "0" + chosenDay;
                        if (month + 1 < 10)
                            chosenMonth = "0" + chosenMonth;

                        txtLeaveDate.setText(chosenDay + "/" + chosenMonth + "/" + year);
                    }
                }, year, month, day);

                datePickerDialog.show();

            }
        });

        txtArriveTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calendar = Calendar.getInstance();
                minute = calendar.get(Calendar.MINUTE);
                hour = calendar.get(Calendar.HOUR_OF_DAY);

                timePickerDialog = new TimePickerDialog(StopPointMap.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        txtArriveTime.setText(String.format("%02d:%02d", i, i1));
                    }
                }, hour, minute, true);

                timePickerDialog.show();
            }
        });

        txtLeaveTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calendar = Calendar.getInstance();
                minute = calendar.get(Calendar.MINUTE);
                hour = calendar.get(Calendar.HOUR_OF_DAY);

                timePickerDialog = new TimePickerDialog(StopPointMap.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        txtLeaveTime.setText(String.format("%02d:%02d", i, i1));
                    }
                }, hour, minute, true);

                timePickerDialog.show();
            }
        });



        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                latLngs.remove(lastIndex);
                dialog.dismiss();
                isDialogShowing = false;
            }
        });

        LatLng middle = mMap.getCameraPosition().target;
        latLngs.add(latLngs.size() - 1, middle);
        lastIndex = latLngs.size() - 2;
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(middle.latitude, middle.longitude, 1);
            edtAddress.setText(addresses.get(0).getAddressLine(0));
        } catch (IOException e) {
            e.printStackTrace();
        }


        dialog.show();
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);

    }




    public int isContainingStopPoint(ArrayList<StopPoint> arr, StopPoint sp, int begin)
    {
        for(int i = begin; i < arr.size(); i++)
        {
            if ((Double.parseDouble(sp.Lat.toString()) == Double.parseDouble(arr.get(i).Lat.toString()))
                    && (Double.parseDouble(sp.Long.toString()) == Double.parseDouble(arr.get(i).Long.toString()))
                    && (arr.get(i).id.equals(sp.id))
                    && (arr.get(i).address.equals(sp.address))
                    && (arr.get(i).name.equals(sp.name))
            )
                return i;
        }
        return -1;
    }

    //Get list stop points without origin and destination

    private void initTrackNumber(){
        for (int i = 0; i < stopPointArrayList.size(); i++){
            stopPointArrayList.get(i).track = i;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_MENU){
            if (resultCode == RESULT_OK){
                Bundle bundle = data.getExtras();
                if (bundle.getBoolean("isRemove", false)){
                    ArrayList <Integer> trackList = bundle.getIntegerArrayList("track_list");
                    int lastRemoveIndex = stopPointArrayList.size();
                    for (int i = 0; i < trackList.size(); i++){
                        int removeIndex;

                        if (trackList.get(i) > lastRemoveIndex){
                            removeIndex = trackList.get(i) - 1;
                        }
                        else{
                            removeIndex = trackList.get(i);
                        }

                        StopPoint sp = stopPointArrayList.get(removeIndex);
                        if (sp.id != null) {
                            int index = isContainingStopPoint(pendingResult, sp, 0);
                            pendingResult.remove(index);
                        }

                        markerArrayList.get(removeIndex).remove();
                        markerArrayList.remove(removeIndex);
                        stopPointArrayList.remove(removeIndex);
                        latLngs.remove(removeIndex);

                    }

                    if (polyline != null){
                        polyline.remove();
                        polyline = null;
                    }
                    try {
                        drawRoute();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                stopPointArrayList = bundle.getParcelableArrayList("list_stop_points");

                updateMarker();
            }
        }
        else
        {
            if ((requestCode == REQUEST_CODE) && (resultCode == RESULT_OK))
            {
                ArrayList<StopPoint> result = new ArrayList<>();
                result = data.getParcelableArrayListExtra("checkedRecommendedSP");

                for(int i = 0; i < result.size(); i++)
                {
                    if (isContainingStopPoint(pendingResult,result.get(i),0) == -1) {
                        stopPointArrayList.add(stopPointArrayList.size() - 1, result.get(i));
                        LatLng latLng = new LatLng(result.get(i).Lat, result.get(i).Long);
                        latLngs.add(latLngs.size() - 1,latLng);
                    }
                }





                for(int i = 0; i < pendingResult.size(); i++)
                {
                    int index = isContainingStopPoint(result, pendingResult.get(i),0);
                    if (index == -1) {
                        stopPointArrayList.remove(pendingResult.get(i));
                        latLngs.remove(index);
                    }
                }


                if (polyline != null)
                {
                    polyline.remove();
                    polyline = null;
                }

                try {
                    drawRoute();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                updateMarker();
                pendingResult = result;

            }
        }
    }

    private void updateMarker(){
        for (int i = 0; i < markerArrayList.size(); i++){
            markerArrayList.get(i).remove();
        }
        markerArrayList.clear();

        Marker ori = mMap.addMarker(new MarkerOptions().position(latLngs.get(0)).title("Origin")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker_icon)));

        Marker des = mMap.addMarker(new MarkerOptions().position(latLngs.get(latLngs.size() - 1)).title("Des")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker_icon)));

        markerArrayList.add(ori);
        markerArrayList.add(des);

        for (int i = 1; i < stopPointArrayList.size() - 1; i++){
            Marker marker;
            switch (stopPointArrayList.get(i).serviceTypeId) {
                case 1:
                    marker = mMap.addMarker(new MarkerOptions().position(latLngs.get(i)).title("Restaurant")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurant_icon)));
                    break;
                case 2:
                    marker = mMap.addMarker(new MarkerOptions().position(latLngs.get(i)).title("Hotel")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.hotel_icon)));
                    break;
                default:
                    marker = mMap.addMarker(new MarkerOptions().position(latLngs.get(i)).title("Stop Point")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.stop_point_icon)));
            }

            markerArrayList.add(markerArrayList.size() - 1 ,marker);
        }
    }


    @Override
    public void onBackPressed() {
        if (isDialogShowing)
        {
            latLngs.remove(lastIndex);
            dialog.dismiss();
            isDialogShowing = false;
        }
        else
        {
            super.onBackPressed();
        }
    }
}
