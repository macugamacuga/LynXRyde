package com.lynx.ride.lynxryde;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.Permissions;
import java.util.ArrayList;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, PlaceSelectionListener, GoogleMap.OnCameraChangeListener,
        LocationListener,RoutingListener {
    //current location
    Location lastlocation;
    //Destination
    LatLng latLng_Destination;
    private static final int MY_PERMISSION_REQUEST_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private String mPlaceName;
    private LocationRequest locationRequest;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationProviderClient;
//popup
    private PopupWindow popupWindow;
    private LayoutInflater layoutInflater;
    private FrameLayout framelayout;
    TextView textView;
    FloatingActionButton destination ,request_car;
    //routing
    Marker dest_marker,start_marker;

    float distance_A_B;
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.black_color,R.color.primary_dark_material_light,R.color.colorAccent,R.color.primary_dark_material_light};

    private final static int DO_UPDATE_TEXT = 0;
    private final static int DO_THAT = 1;
    private final Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            final int what = msg.what;
            switch(what) {
                case DO_UPDATE_TEXT: doUpdate(); break;
               // case DO_THAT: doThat(); break;
            }
        }
    };

    private void doUpdate() {

        textView.setText(String.valueOf(distance_A_B/1000));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
       destination=findViewById(R.id.fab_button);

        polylines = new ArrayList<>();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button destination=findViewById(R.id.destination);
        destination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                    .build(MapsActivity.this);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException e) {
                    // TODO: Handle the error.
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                }
            }
        });
        request_car=findViewById(R.id.request_button);
        request_car.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupwindow();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.i("", "Place: " + place.getName());
                mPlaceName = place.getName().toString();
                 latLng_Destination = place.getLatLng();

                Log.e(BuildConfig.BUILD_TYPE, "information abt place address = " + latLng_Destination.toString());

                Toast.makeText(this, "Place selection : " + place.getAddress()+latLng_Destination.toString(),
                        Toast.LENGTH_SHORT).show();
               // mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng_Destination, 18));

                //hide center piont
                erasePolyline();
                destination.setVisibility(View.GONE);
                //create route
                getRouteToMArker(latLng_Destination);
                popupwindow();

                //update address on button
                Button destination =findViewById(R.id.destination);
                destination.setText(place.getAddress().toString());


                mMap.setOnCameraChangeListener(this);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i("", status.getStatusMessage());
                Log.e(BuildConfig.BUILD_TYPE, "onError: Status = " + status.toString());

                Toast.makeText(this, "Place selection failed: " + status.getStatusMessage(),
                        Toast.LENGTH_SHORT).show();

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }

        }

    }

    private void getRouteToMArker(LatLng destination) {
        Routing routing = new Routing.Builder()

                .travelMode(AbstractRouting.TravelMode.DRIVING)

                .withListener(this)

                .alternativeRoutes(true)

                .waypoints(new LatLng(lastlocation.getLatitude(),lastlocation.getLongitude()),  destination)
                .key("AIzaSyA55Y0quPaZSwCjN3Lh7pOKFfCuVDte_FI")
                .build();

        routing.execute();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


requestlocationupdate();


    }

    public void requestlocationupdate(){

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            Toast.makeText(MapsActivity.this, "Location approved.",
                    Toast.LENGTH_SHORT).show();
            fusedLocationProviderClient = new FusedLocationProviderClient(this);
            locationRequest = new LocationRequest();
            locationRequest.setInterval(120000);
            locationRequest.setFastestInterval(60000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                     lastlocation = locationResult.getLastLocation();
                    LatLng coordinate = new LatLng(lastlocation.getLatitude(), lastlocation.getLongitude()); //Store these lat lng values somewhere. These should be constant.

                    CameraUpdate location = CameraUpdateFactory.newLatLngZoom(
                            coordinate, 15);
                    mMap.animateCamera(location);
                   // mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 18));

                   String userId=FirebaseAuth.getInstance().getCurrentUser().getUid();
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference  myRef = database.getReference("UserLocation");
                    GeoFire geofire=new GeoFire(myRef);

               geofire.setLocation(userId,new GeoLocation(lastlocation.getLatitude(),lastlocation.getLongitude()));
                  //  myRef.setValue("Hello, World!");

                }
            }, getMainLooper());
        }else

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_REQUEST_FINE_LOCATION);

        return;



    }



    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
       // mMap.addMarker(new MarkerOptions().position(cameraPosition.target).title(mPlaceName));

        mMap.setOnCameraChangeListener(null);
    }

    @Override
    public void onPlaceSelected(Place place) {
        if (mMap == null) {
            return;
        }
       /* mPlaceName = place.getName().toString();

        final LatLng latLng = place.getLatLng();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));

        mMap.setOnCameraChangeListener(this);*/
    }


    public static Double getDistanceBetween(Location latLon1, LatLng latLon2) {
        if (latLon1 == null || latLon2 == null)
            return null;
        float[] result = new float[1];
        Location.distanceBetween(latLon1.getLatitude(), latLon1.getLongitude(),
                latLon2.latitude, latLon2.longitude, result);
        return (double) result[0];
    }

    public void popupwindow(){
        BottomSheetDialog bottomSheetDialog=new BottomSheetDialog(MapsActivity.this);
        View sheet_page=getLayoutInflater().inflate(R.layout.pop_up_window,null);
bottomSheetDialog.setContentView(sheet_page);
        //BottomSheetBehavior bottomSheetBehavior=BottomSheetBehavior.from((View)sheet_page.getParent());
        BottomSheetBehavior bottomSheetBehavior=BottomSheetBehavior.from((View)sheet_page.getParent());
bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
    @Override
    public void onStateChanged(@NonNull View bottomSheet, int newState) {
        switch (newState) {
            case BottomSheetBehavior.STATE_HIDDEN:
request_car.setVisibility(View.VISIBLE);
                break;
            case BottomSheetBehavior.STATE_EXPANDED: {
                textView=bottomSheet.findViewById(R.id.Distance_popUp);
                request_car=findViewById(R.id.request_button);
                request_car.setVisibility(View.GONE);
            }
            break;
        }
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {

    }
});
        bottomSheetDialog.show();
       textView=sheet_page.findViewById(R.id.Distance_popUp);

    }
    @Override
    public void onError(Status status) {
        Log.e(BuildConfig.BUILD_TYPE, "onError: Status = " + status.toString());

        Toast.makeText(this, "Place selection failed: " + status.getStatusMessage(),
                Toast.LENGTH_SHORT).show();
    }




    @Override
    public void onLocationChanged(Location location) {

    }


    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {

            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

        }else {

            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        try {
            Toast.makeText(getApplicationContext(), "Route success ", Toast.LENGTH_SHORT).show();

            zoomtolocation(new LatLng(lastlocation.getLatitude(), lastlocation.getLongitude()), latLng_Destination);

            if (polylines.size() > 0) {

                for (Polyline poly : polylines) {

                    poly.remove();

                }

            }

            polylines = new ArrayList<>();

            //add route(s) to the map.

            //   for (int i = 0; i <route.size(); i++) {


            //In case of more than 5 alternative routes

            int colorIndex = 0 % COLORS.length;


            PolylineOptions polyOptions = new PolylineOptions();

            polyOptions.color(getResources().getColor(COLORS[colorIndex]));

            polyOptions.width(10 + 0 * 3);

            polyOptions.addAll(route.get(0).getPoints());

            Polyline polyline = mMap.addPolyline(polyOptions);

            polylines.add(polyline);


            distance_A_B = route.get(0).getDistanceValue();
            Toast.makeText(getApplicationContext(), "Route " + (0 + 1) + ": distance - " + route.get(0).getDistanceValue() + ": duration - " + route.get(0).getDurationValue(), Toast.LENGTH_SHORT).show();


            // Start marker

            MarkerOptions option1 = new MarkerOptions();

            option1.position(new LatLng(lastlocation.getLatitude(), lastlocation.getLongitude()));

            option1.icon(BitmapDescriptorFactory.fromResource(R.mipmap.pin_larger));

            start_marker= mMap.addMarker(option1);


            // End marker

           MarkerOptions  option2 = new MarkerOptions();

            option2.position(latLng_Destination);

            option2.icon(BitmapDescriptorFactory.fromResource(R.mipmap.iconfinder_pin_large));

             dest_marker=mMap.addMarker(option2);

            myHandler.sendEmptyMessage(DO_UPDATE_TEXT);

        }catch (Exception e){
            Toast.makeText(getApplicationContext(), " route Error: "+e, Toast.LENGTH_LONG).show();

        }
//textView.setText(route.get(1).getDistanceValue());
    }

    private void zoomtolocation(LatLng latLng, LatLng latLng_destination) {


        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(latLng).include(latLng_Destination);

//Animate to the bounds
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(builder.build(), 50);
        mMap.moveCamera(cameraUpdate);

    }

    @Override
    public void onRoutingCancelled() {

        erasePolyline();
    }
    private void erasePolyline(){
        for(Polyline line:polylines){
            line.remove();
        }
      /* start_marker.remove();
        dest_marker.remove();*/
        polylines.clear();
        destination.setVisibility(View.VISIBLE);
    }

}

