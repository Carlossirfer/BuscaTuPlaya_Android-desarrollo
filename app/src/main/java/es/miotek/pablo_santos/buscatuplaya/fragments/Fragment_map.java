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
import android.widget.ImageView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.ui.IconGenerator;
import com.parse.FindCallback;
import com.parse.ParseException;
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

    private int prueba=0;
    //CONTROL DE COMUNIDADES PINTADAS
    private static boolean PINTADOCANARIAS = false;
    private static boolean PINTADONORTE=false;
    private static boolean PINTADOTODO=false;
    private static boolean PINTADONORESTE = false;
    private static boolean PINTADOSURESTE = false;

    //FRAGMENTO
    private View viewRoot;
    private Context contexto;

    //COMUNIDADES AUTONOMAS SE CAMBIARA MAS ADELANTE
    private String[] comunidadesNorEste={"Cataluña/Catalunya","Illes Balears"};
    private String[] comunidadesSurEste={"Murcia, Region de","Comunitat Valenciana","Andalucía"};
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

    //PLAYAS FILTRADAS POR COMUNIDADES
    public static ArrayList<MyItem> PLAYAS_NORTE=new ArrayList<>();
    public static ArrayList<MyItem> PLAYAS_SURESTE=new ArrayList<>();
    public static ArrayList<MyItem> PLAYAS_NORESTE=new ArrayList<>();
    public static ArrayList<MyItem> PLAYAS_CANARIAS=new ArrayList<>();
    public static ArrayList<MyItem> ALLPLAYAS=new ArrayList<>();

    //CLASE QUE NOS PERMITE CUSTOMIZAR LOS ITEMS DE LOS CLUSTER, PARA PONER LAS DIFERENTES BANDERAS DE COLORES
    private class PersonRenderer extends es.miotek.pablo_santos.buscatuplaya.clustering.view.DefaultClusterRenderer<MyItem> {
        private final IconGenerator mIconGenerator = new IconGenerator(contexto);
        private final IconGenerator mClusterIconGenerator = new IconGenerator(contexto);
        private final ImageView mImageView;
        private final ImageView mClusterImageView;
        private final int mDimension;

        public PersonRenderer() {
            super(contexto, mMap, mClusterManager);

            View multiProfile = getLayoutInflater(null).inflate(R.layout.custom_item_cluster, null);
            mClusterIconGenerator.setContentView(multiProfile);
            mClusterImageView = (ImageView) multiProfile.findViewById(R.id.image);

            mImageView = new ImageView(contexto);
            mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
            mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
            int padding = (int) getResources().getDimension(R.dimen.custom_profile_padding);
            mImageView.setPadding(padding, padding, padding, padding);
            mIconGenerator.setContentView(mImageView);
        }

        //PARA CUSTOMIZAR LA IMAGEN DE LA BANDERA DE LA PLAYA Y SUS DATOS A MOSTRAR
        @Override
        protected void onBeforeClusterItemRendered(MyItem person, MarkerOptions markerOptions) {
            // Draw a single person.
            // Set the info window to show their name.
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bandera));
        }

//        @Override
        //ESTE METODO PERMITIRA EN UN FUTURO CAMBIAR LOS CIRCULOS DE COLORES DE LOS CLUSTER CON EL NUMERO DE PLAYAS
//        protected void onBeforeClusterRendered(Cluster<MyItem> cluster, MarkerOptions markerOptions) {
//            // Draw multiple people.
//            // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
//            List<Drawable> profilePhotos = new ArrayList<Drawable>(Math.min(4, cluster.getSize()));
//            int width = mDimension;
//            int height = mDimension;
//
//            for (Person p : cluster.getItems()) {
//                // Draw 4 at most.
//                if (profilePhotos.size() == 4) break;
//                Drawable drawable = getResources().getDrawable(p.profilePhoto);
//                drawable.setBounds(0, 0, width, height);
//                profilePhotos.add(drawable);
//            }
//            MultiDrawable multiDrawable = new MultiDrawable(profilePhotos);
//            multiDrawable.setBounds(0, 0, width, height);
//
//            mClusterImageView.setImageDrawable(multiDrawable);
//            Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
//            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
//        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            // Always render clusters.
            return cluster.getSize() > 1;
        }
    }

    //CONSTRUCTOR
    public Fragment_map() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //PARA EL CONTROL DE LA LOCALIZACION DEL MAPA
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        //ACTIVAMOS EL GPS SI HACE FALTA
        if (!GPSEnabled()) activarGPS();

        contexto=getContext();
        viewRoot = inflater.inflate(R.layout.fragment_map, container, false);
        //PARA INICIALIZAR EL MAPA
        MapsInitializer.initialize(this.getActivity());
        mapView = (MapView) viewRoot.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);//when you already implement OnMapReadyCallback in your fragment
        return viewRoot;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //METODO QUE TRAE TODAS LAS PLAYAS DE LA BASE DE DATOS
        getAllPlayas();
    }

    //METODO PARA FILTRAR LAS PLAYAS POR COMUNIDADES
    private void filtrarPlayas() {

        for (MyItem item:ALLPLAYAS){

            if (item.getmComunidad().equals("Canarias")) PLAYAS_CANARIAS.add(item);

            for (int i=0;i<comunidadesNorte.length;i++){
                if (item.getmComunidad().equals(comunidadesNorte[i])){
                    PLAYAS_NORTE.add(item);
                    break;
                }
            }
            for (int i=0;i<comunidadesNorEste.length;i++){
                if (item.getmComunidad().equals(comunidadesNorEste[i])){
                    PLAYAS_NORESTE .add(item);
                    break;
                }
            }
            for (int i=0;i<comunidadesSurEste.length;i++){
                if (item.getmComunidad().equals(comunidadesSurEste[i])){
                    PLAYAS_SURESTE .add(item);
                    break;
                }
            }



        }

    }

    //METODO PARA TRAER TODAS LAS PLAYAS
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

    //METODO QUE SE EJECUTA AL CARGAR EL MAPA
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //GOOGLE MAP
        mMap = googleMap;

        //PARA CLUSTERING GOOGLE MAPS
        mClusterManager = new es.miotek.pablo_santos.buscatuplaya.clustering.ClusterManager<MyItem>(getContext(), mMap);

        //INDICAMOS EL RENDERER QUE NOS SIRVE PARA CUSTOMINAR LOS CLUSTER
        mClusterManager.setRenderer(new PersonRenderer());

        //EVENTO PARA RECOGER LA POSICION DEL MAPA
        mMap.setOnCameraIdleListener(mClusterManager);

        //EVENTO CLICK EN MARKET. MOSTRARA UNA NUEVA VISTA.
        mMap.setOnMarkerClickListener(mClusterManager);


        if (GPSEnabled())
            //RECOGEMOS NUESTRA UBICACION SI EL GPS ESTA ACTIVADO
            miUbicacion(getContext());
        else{
            //PONEMOS UNA UBICACION DETERMINADA SI GPS ESTA INACTIVO
            LatLng coordenadas = new LatLng(40.4893538, -3.6827461);
            CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordenadas, 5);
            mMap.animateCamera(miUbicacion);
        }


    }

    //METODO PARA EL CONTROL DE LAS PLAYAS PINTADAS Y ACTUALIZAR EL MAPA.
    //UTILIZADO PARA PINTAR POR ZONAS EL MAPA
    public static void pintaPlayas(final Context contexto){

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
                PINTADOSURESTE=false;
                PINTADONORESTE=false;
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
                PINTADOSURESTE=false;
                PINTADONORESTE=false;
                PINTADOCANARIAS=false;
                mClusterManager.addItems(PLAYAS_NORTE);
//                Toast.makeText(contexto, "NORTE: BANDERA SUR"+PINTADOSUR, Toast.LENGTH_SHORT).show();
            }else if(LAT<42.0&&LAT>35&&(!PINTADONORESTE||!PINTADOSURESTE)){
                if (LNG<0&&!PINTADOSURESTE){
                    //LIMPIAR MAPA
                    mMap.clear();
                    if (location!=null)Fragment_map.agregarMarcador(location);
                    mClusterManager.clearItems();
                    PINTADOSURESTE=true;
                    PINTADONORESTE=false;
                    PINTADONORTE=false;
                    PINTADOCANARIAS=false;
                    mClusterManager.addItems(PLAYAS_SURESTE);
                }else if (LNG>0&&!PINTADONORESTE){
                    //LIMPIAR MAPA
                    mMap.clear();
                    if (location!=null)Fragment_map.agregarMarcador(location);
                    mClusterManager.clearItems();
                    PINTADONORESTE=true;
                    PINTADONORTE=false;
                    PINTADOSURESTE=false;
                    PINTADOCANARIAS=false;
                    mClusterManager.addItems(PLAYAS_NORESTE);
                }
            }else if (LAT<=33&&!PINTADOCANARIAS){
                mMap.clear();
                if (location!=null)Fragment_map.agregarMarcador(location);
                mClusterManager.clearItems();
                PINTADOCANARIAS=true;
                PINTADOSURESTE=false;
                PINTADONORESTE=false;
                PINTADONORTE=false;
                mClusterManager.addItems(PLAYAS_CANARIAS);
//                Toast.makeText(contexto, "CANARIAS: BANDERA SUR"+PINTADOSUR+" BANDERA NORTE: "+PINTADONORTE, Toast.LENGTH_SHORT).show();
            }
        }
    }

    //METODO PARA SABER SI EL GPS ESTA ACTIVADO
    private boolean GPSEnabled() {
        return isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    //METODO PARA AGREGAR UN MARCADOR EN EL MAPA
    public static void agregarMarcador(double lat, double lng) {
        LatLng coordenadas = new LatLng(lat, lng);
        CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordenadas, 6);
        if (marcador != null) marcador.remove();
        marcador = mMap.addMarker(new MarkerOptions()
                .position(coordenadas)
                .title("Ubicación Actual"));

       mMap.animateCamera(miUbicacion);
    }

    //METODO PARA AGREGAR UN MARCADOR EN EL MAPA
    public static void agregarMarcador(Location loc){
        LatLng coordenadas = new LatLng(loc.getLatitude(), loc.getLongitude());
        CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordenadas, 6);
        if (marcador != null) marcador.remove();
        marcador = mMap.addMarker(new MarkerOptions()
                .position(coordenadas)
                .title("Ubicación Actual"));
    }

    //METODO QUE NOS COMPRUEBA SI HAY UNA NUEVA UBICACION
    private static void actualizarUbicacion(Location location) {
        if (location != null) {
            agregarMarcador(location.getLatitude(), location.getLongitude());
        }
    }

    //METODO PARA OBTENER NUESTRA UBICACION
    private static void miUbicacion(Context contexto) {
        if (ActivityCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if(location==null)location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        actualizarUbicacion(location);
        if (location==null)locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 0, locListener);
    }

    //LISTENER PARA EL CAMBIO DE UBICACION
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

    //DIALOGO QUE NOS LLEVA A LOS AJUSTER PARA ACTIVAR EL GPS
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
