package ncsu.project15.ece484_project15_client;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class GoogleMapsFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener {

    // Maps variables
    private GoogleMap mMap;
    SupportMapFragment mapFragment;
    private LocationRequest mLocationRequest;
    Location mLastLocation;
    Circle mCurrentLocationCircle;
    private FusedLocationProviderClient mFusedLocationClient;
    LocationCallback mLocationCallback;
    CameraPosition position;
    boolean followLocation;
    private GoogleMap.OnInfoWindowClickListener mInfoWindowClickListner;

    // Buttons for location
    View myLocationButton;
    View defaultMyLocationButton;

    // Listener for sending events back to MainActivity
    private OnMapsInteractionListener mMapsListener;



    public static GoogleMapsFragment newInstance() {
        GoogleMapsFragment fragment = new GoogleMapsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the view
        View mView = inflater.inflate(R.layout.fragment_google_maps, null);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        // Get the default location button
        defaultMyLocationButton = ((View) mView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));


        return mView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get the MapFragment and get the callback for the map
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set custom MyLocationBUtton
        myLocationButton = view.findViewById(R.id.myLocationButton);
        myLocationButton.setOnClickListener(this);

        // Location Callback. Set activity for when the location updates
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    if (position != null && followLocation) {
                        Log.i("MapsActivity", "CameraPosition " + position.target);
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                    }
                    mLastLocation = location;
                }
            }
        };
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
        // Get the map once its ready
        mMap = googleMap;
        // disable default location button
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Set the Location request intervals
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); // one second interval
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // Create an OnCameraIdle for when the camera stops moving
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                position = mMap.getCameraPosition();
                if (mLastLocation != null) {
                    //Draw 20 mile radius circle
                    CircleOptions circleOptions = new CircleOptions()
                            .center(position.target)
                            .radius(500); //20 miles
                    if (mCurrentLocationCircle != null) {
                        mCurrentLocationCircle.remove();
                    }
                    mCurrentLocationCircle = mMap.addCircle(circleOptions);
                    followLocation = false;
                } else {
                    followLocation = true;
                }
            }
        });

        // Set up initial Design Day Printer Object
        Printer mDesignDayPrinter = new Printer();
        mDesignDayPrinter.setName("Design Day Printer");
        mDesignDayPrinter.setLocation(new LatLng(35.7829, -78.6851));

        mMap.addMarker(new MarkerOptions()
                .position(mDesignDayPrinter.getLocation())
                .title(mDesignDayPrinter.getName())
                .snippet("Click to Print!"))
                .setTag(mDesignDayPrinter);

        mInfoWindowClickListner = new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Printer printer = (Printer) marker.getTag();
                mMapsListener.onMapsInteraction(printer);
                Toast.makeText(getContext(), "Printing to: " + printer.getName() + "!", Toast.LENGTH_LONG).show();
            }
        };
        mMap.setOnInfoWindowClickListener(mInfoWindowClickListner);

        // Check for location permissions
        checkLocationPermissionMethod();

    }

    public void checkLocationPermissionMethod() {

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission has already been granted
            mFusedLocationClient.getLastLocation().addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
                    }
                }
            });
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            mMap.setMyLocationEnabled(true);
        } else {
            //Request Location Permission
            checkLocationPermission();
        }
    }

    public static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 99;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(getActivity())
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_ACCESS_FINE_LOCATION );
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_ACCESS_FINE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getActivity(), "permission denied", Toast.LENGTH_LONG).show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMapsInteractionListener) {
            mMapsListener = (OnMapsInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMapsInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMapsListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        // Remove Location updates when onPause is called
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Resume Location updates when onResume is called
        if (mMap != null) {
            checkLocationPermission();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            // Set click behavior for location button
            case R.id.myLocationButton: {
                if(mMap != null) {
                    if(defaultMyLocationButton != null) {
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        defaultMyLocationButton.callOnClick();
                        mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    }
                }
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnMapsInteractionListener {
        // TODO: Update argument type and name
        void onMapsInteraction(Printer printer);
    }
}
