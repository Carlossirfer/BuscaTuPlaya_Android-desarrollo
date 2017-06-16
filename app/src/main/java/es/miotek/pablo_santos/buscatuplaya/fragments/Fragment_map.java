package es.miotek.pablo_santos.buscatuplaya.fragments;


import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

import es.miotek.pablo_santos.buscatuplaya.R;
import es.miotek.pablo_santos.buscatuplaya.model.MyItem;

/**
 * A simple {@link Fragment} subclass.
 */
public class Fragment_map extends Fragment implements OnMapReadyCallback {

    //CONTROL DE CLUSTER PINTADOS
    private static boolean PINTADOCANARIAS = false;
    private static boolean PINTADONORTE=false;
    private static boolean PINTADOSUR=false;
    private static boolean PINTADOTODO=false;

    private View viewRoot;

    //COMUNIDADES AUTONOMAS SE CAMBIARA MAS ADELANTE
    private String[] comunidadesEste={"Cataluña/Catalunya","Murcia, Region de","Comunitat Valenciana","Andalucía"};
    private String[] comunidadesNorte={"Cantabria","Asturias, Principado de", "Galicia","País Vasco","Navarra"};

    //PARA EL MAPA
    private static GoogleMap mMap;
    private MapView mapView;
    private static Marker marcador;
    public static double LAT = 0.0;
    public static double LNG = 0.0;
    private Boolean isGPSEnabled;
    static LocationManager locationManager;
    static Location location;
    public static float ZOOM=0;
    private static boolean bandera=false;

    //CLUSTERING
    private static es.miotek.pablo_santos.buscatuplaya.clustering.ClusterManager<MyItem> mClusterManager;

    //DATOS DE LAS PLAYAS
    public static ArrayList<MyItem> PLAYAS_NORTE=new ArrayList<>();
    public static ArrayList<MyItem> PLAYAS_SURESTE=new ArrayList<>();
    public static ArrayList<MyItem> PLAYAS_CANARIAS=new ArrayList<>();
    public static ArrayList<MyItem> ALLPLAYAS=new ArrayList<>();

    public Fragment_map() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        if (!GPSEnabled()) activarGPS();


        viewRoot = inflater.inflate(R.layout.fragment_map, container, false);
        MapsInitializer.initialize(this.getActivity());
        mapView = (MapView) viewRoot.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);//when you already implement OnMapReadyCallback in your fragment
        return viewRoot;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getAllPlayas();


    }

    private void filtrarPlayas() {

        for (MyItem item:ALLPLAYAS){

            if (item.getmComunidad().equals("Canarias")) PLAYAS_CANARIAS.add(item);

            for (int i=0;i<comunidadesNorte.length;i++){
                if (item.getmComunidad().equals(comunidadesNorte[i])){
                    PLAYAS_NORTE.add(item);
                    break;
                }
            }
            for (int i=0;i<comunidadesEste.length;i++){
                if (item.getmComunidad().equals(comunidadesEste[i])){
                    PLAYAS_SURESTE .add(item);
                    break;
                }
            }

        }

    }

    private void getAllPlayas() {

        ArrayList<String> campos=new ArrayList<>();
        campos.add("Coordenada_X");
        campos.add("Coordenada_Y");
        campos.add("Comunidad_Autonoma");
        campos.add("Nombre");
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Playas");
        query.selectKeys(campos);
        query.setLimit(3500);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> response, ParseException e) {
                if (e == null) {
                    for (ParseObject playa:response){
                        MyItem item=new MyItem(
                                Double.parseDouble(playa.getString("Coordenada_Y").replace(',','.')),
                                Double.parseDouble(playa.getString("Coordenada_X").replace(',','.')),
                                playa.getString("Comunidad_Autonoma"));

                        ALLPLAYAS.add(item);
                    }
                    filtrarPlayas();
                } else Log.d("PARSE", "Error: " + e.getMessage());
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mClusterManager = new es.miotek.pablo_santos.buscatuplaya.clustering.ClusterManager<MyItem>(getContext(), mMap);
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        if (GPSEnabled())
            miUbicacion(getContext());
        else{
            LatLng coordenadas = new LatLng(40.4893538, -3.6827461);
            CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordenadas, 5);
            mMap.animateCamera(miUbicacion);
        }
    }

    public static void getPlayas(final Context contexto){

        if (PLAYAS_SURESTE.size()==0&&PLAYAS_NORTE.size()==0&&ALLPLAYAS.size()==0)return;
        if (ZOOM<=6.12){
            if(PINTADOTODO)return;
            else{
                //LIMPIAMOS MAPA
                mMap.clear();
                if (location!=null)Fragment_map.agregarMarcador(location);
                mClusterManager.clearItems();
                PINTADOTODO=true;
                PINTADONORTE=false;
                PINTADOSUR=false;
                mClusterManager.addItems(ALLPLAYAS);
//                Toast.makeText(contexto, "TODO EL MAPA: BANDERA SUR"+PINTADOSUR+" BANDERA NORTE: "+PINTADONORTE, Toast.LENGTH_SHORT).show();
            }
        }else if(ZOOM>=5.0){
            PINTADOTODO=false;
            if (LAT>=42.0&&LNG<=-1.0&&!PINTADONORTE){
                //LIMPIAR MAPA
                mMap.clear();
                if (location!=null)Fragment_map.agregarMarcador(location);
                mClusterManager.clearItems();
                PINTADONORTE=true;
                PINTADOSUR=false;
                PINTADOCANARIAS=false;
                mClusterManager.addItems(PLAYAS_NORTE);
//                Toast.makeText(contexto, "NORTE: BANDERA SUR"+PINTADOSUR, Toast.LENGTH_SHORT).show();
            }else if(LAT<42.0&&LAT>35&&!PINTADOSUR){
                //LIMPIAR MAPA
                mMap.clear();
                if (location!=null)Fragment_map.agregarMarcador(location);
                mClusterManager.clearItems();
                PINTADOSUR=true;
                PINTADONORTE=false;
                PINTADOCANARIAS=false;
                mClusterManager.addItems(PLAYAS_SURESTE);
//                Toast.makeText(contexto, "SUR Y ESTE: BANDERA NORTE "+PINTADONORTE, Toast.LENGTH_SHORT).show();
            }else if (LAT<=33&&!PINTADOCANARIAS){
                mMap.clear();
                if (location!=null)Fragment_map.agregarMarcador(location);
                mClusterManager.clearItems();
                PINTADOCANARIAS=true;
                PINTADOSUR=false;
                PINTADONORTE=false;
                mClusterManager.addItems(PLAYAS_CANARIAS);
//                Toast.makeText(contexto, "CANARIAS: BANDERA SUR"+PINTADOSUR+" BANDERA NORTE: "+PINTADONORTE, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean GPSEnabled() {
        return isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static void agregarMarcador(double lat, double lng) {
        LatLng coordenadas = new LatLng(lat, lng);
        CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordenadas, 6);
        if (marcador != null) marcador.remove();
        marcador = mMap.addMarker(new MarkerOptions()
                .position(coordenadas)
                .title("Ubicación Actual"));

       mMap.animateCamera(miUbicacion);
    }
    public static void agregarMarcador(Location loc){
        LatLng coordenadas = new LatLng(loc.getLatitude(), loc.getLongitude());
        CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordenadas, 6);
        if (marcador != null) marcador.remove();
        marcador = mMap.addMarker(new MarkerOptions()
                .position(coordenadas)
                .title("Ubicación Actual"));
    }

    private static void actualizarUbicacion(Location location) {
        if (location != null) {
//            lat = location.getLatitude();
//            lng = location.getLongitude();
            agregarMarcador(location.getLatitude(), location.getLongitude());
        }
    }

    private static void miUbicacion(Context contexto) {
        if (ActivityCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if(location==null)location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        actualizarUbicacion(location);
        if (location==null)locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 0, locListener);
    }

    static LocationListener locListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (!bandera){
                actualizarUbicacion(location);
                bandera=true;
            }

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    /**
     * Function to show settings alert dialog
     */
    public void activarGPS() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this.getContext());

        // Setting Dialog Title
        alertDialog.setTitle("Opciones GPS");

        // Setting Dialog Message
        alertDialog.setMessage("El GPS no está activado. Ir a opciones para activarlo?");

        // Setting Icon to Dialog
        //alertDialog.setIcon(R.drawable.delete);

        // On pressing Settings button
        alertDialog.setPositiveButton("Opciones", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(intent,1);
//                miUbicacion();

            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        isGPSEnabled=GPSEnabled();
        if (isGPSEnabled) miUbicacion(getContext());
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
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mapView != null) {
            try {
                mapView.onDestroy();
            } catch (NullPointerException e) {
                Log.e("TAB", "Error while attempting MapView.onDestroy(), ignoring exception", e);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }
}
