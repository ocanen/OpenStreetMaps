package com.example.myapplication;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.input.MapZoomControls;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ZoomButton;
import androidx.core.app.ActivityCompat;
import com.opencsv.CSVReader;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

public class MainActivity extends Activity implements LocationListener {

   private static final String MAP_FILE = "/Download/madrid2.map";
   private static final String CSV_FILE = "/Download/Aseos Publicos Operativos.csv";
    private MapView mapView;
    Polyline gpxObjects;
    MyMarker marker;

    LocationManager locationManager;
    String mprovider;

    Double longitud_if=0.0;
    Double longitud_const=0.0;
    Double latitud_const=0.0;

    Location location ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        mprovider = locationManager.getBestProvider(criteria, false);

        AndroidGraphicFactory.createInstance(this.getApplication());
        this.mapView = new MapView(this);
        setContentView(this.mapView);

        this.mapView.setClickable(true);
        this.mapView.getMapScaleBar().setVisible(true);
        this.mapView.setBuiltInZoomControls(true);
        this.mapView.getMapZoomControls().setZoomLevelMin((byte) 10);
        this.mapView.getMapZoomControls().setZoomLevelMax((byte) 20);
        
        TileCache tileCache = AndroidUtil.createTileCache(this, "mapcache",
                mapView.getModel().displayModel.getTileSize(), 1f,
                this.mapView.getModel().frameBufferModel.getOverdrawFactor());

        MapDataStore mapDataStore = new MapFile(new File(Environment.getExternalStorageDirectory(), MAP_FILE));
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore, this.mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);

        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);
        Log.i("MyActivity", "arranque");
        double latitudeX=0 ;
        double longitudY=0 ;
        if (mprovider != null && !mprovider.equals("")) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            location = locationManager.getLastKnownLocation(mprovider);
            locationManager.requestLocationUpdates(mprovider, 5000, 3, this);

            if (location != null){
                onLocationChanged(location);
                latitudeX= location.getLatitude();
                longitudY = location.getLongitude();
                Log.i("MyActivity", "1define latitud: "+latitudeX+"longitud: "+longitudY);
            }
            else{
                Log.i("MyActivity", "No Location Provider Found Check Your Code++++++++++");
                Log.i("MyActivity", "2define latitud: "+latitudeX+"longitud: "+longitudY);

            }
        }
        else {
            latitudeX= 40.3713163;
            longitudY = -3.6183235;
            this.mapView.setCenter(new LatLong(latitudeX, longitudY));
            Log.i("MyActivity", "3define latitud: "+latitudeX+"longitud: "+longitudY);
        }
        this.mapView.setZoomLevel((byte) 20);
        //cambio de botones zoom
        int bottom = mapView.getMapZoomControls().getBottom();
        MapZoomControls mapZoomControls = mapView.getMapZoomControls();
        ZoomButton buttonZoomIn = (ZoomButton) mapZoomControls.getChildAt(1);
        ZoomButton buttonZoomOut = (ZoomButton) mapZoomControls.getChildAt(0);
        buttonZoomIn.setBackgroundResource(android.R.drawable.btn_plus);
        buttonZoomOut.setBackgroundResource(android.R.drawable.btn_minus);

        try {
            CSVReader reader = new CSVReader(new FileReader(Environment.getExternalStorageDirectory()+CSV_FILE));
            //CSVReader reader = new CSVReader(new FileReader(CSV_FILE_RAW));
            String[] nextLine;
            int i=0;

            Double numLatitud=latitudeX;
            Double numLong=longitudY;
            Log.i("MyActivity", "4define latitud: "+latitudeX+"longitud: "+longitudY);
            Double cercanoLong = 0.0;
            Double diferenciaLong = Double.MAX_VALUE; //inicializado valor máximo de variable de tipo int

            Double cercanoLatitud = 0.0;
            Double diferenciaLatitud = Double.MAX_VALUE; //inicializado valor máximo de variable de tipo int

            boolean banderaLong=false;
            boolean banderaLatitud=false;
            int distanciaMinima=0;

            while ((nextLine = reader.readNext()) != null) {
                String[] tokens = nextLine[0].split(";");
                if(i!=0){
                    if (Double.parseDouble(tokens[11]) == numLatitud) {
                        longitud_if= Double.parseDouble(tokens[11]);
                        Log.i("MyActivityENTRA!!!!", "ENTRA!!!! "+longitud_if);
                        banderaLong=true;
                    } else if(!banderaLong){
                            if(Math.abs(Double.parseDouble(tokens[11])-numLatitud)<diferenciaLatitud){
                            cercanoLatitud=Double.parseDouble(tokens[11]);
                            longitud_const=Double.parseDouble(tokens[10]);
                            latitud_const=Double.parseDouble(tokens[11]);
                            Log.i("MyActivity", "cercanaLong:"+longitud_const);
                            Log.i("MyActivity", "cercanalatitud:"+latitud_const);
                            diferenciaLatitud = Math.abs(Double.parseDouble(tokens[11])-numLatitud);
                        }
                        longitud_if=cercanoLatitud;
                    }
                   if(i==1)distanciaMinima=calculateDistanceByHaversineFormula(numLong,numLatitud,Double.parseDouble(tokens[10]),Double.parseDouble(tokens[11]));
                    else{
                        if(distanciaMinima>calculateDistanceByHaversineFormula(numLong,numLatitud,Double.parseDouble(tokens[10]),Double.parseDouble(tokens[11]))){
                           distanciaMinima=calculateDistanceByHaversineFormula(numLong,numLatitud,Double.parseDouble(tokens[10]),Double.parseDouble(tokens[11]));
                            Log.i("MyActivity", "mas cercano:"+"latitud: "+ Double.parseDouble(tokens[10]) + "longitud: "+Double.parseDouble(tokens[11]));
                            Log.i("MyActivity", "distancia"+distanciaMinima);
                       }
                   }

                   marker = new MyMarker(this, new LatLong(Double.parseDouble(tokens[11]), Double.parseDouble(tokens[10])), AndroidGraphicFactory.convertToBitmap(getResources().getDrawable(R.mipmap.pequeno)), 0, 0);
                    // Log.i("MyActivity", "marcas"+longitud+" "+ latitud);
                    mapView.getLayerManager().getLayers().add(marker);

;
                }
                i++;
            }
            Log.i("MyActivity", "marcas final: "+longitud_const+" "+ latitud_const);
        } catch (IOException e) {

        }

        consultRuta(location.getLatitude(), location.getLongitude(),latitud_const, longitud_const);

        // MyMarker marker = new MyMarker(this, new LatLong(40.37174, -3.6193858), AndroidGraphicFactory.convertToBitmap(getResources().getDrawable(R.mipmap.pequeno)), 0, 0);
        //mapView.getLayerManager().getLayers().add(marker);
    }

    static Paint createPaint(int color, int strokeWidth, Style style) {
        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(style);
        return paint;
    }

    @Override
    protected void onDestroy() {
        this.mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(longitud_const !=0.0){
            this.mapView.setCenter(new LatLong(location.getLatitude(), location.getLongitude()));
            LatLong latLong5 = new LatLong(location.getLatitude(), location.getLongitude());
            Log.i("MyActivity", "No Location Provider Found Check Your Code++++++++++MARCA"+location.getLatitude()+" "+location.getLongitude());
            LatLong latLong6 = new LatLong(latitud_const, longitud_const);
            Log.i("MyActivity", "No Location Provider Found Check Your Code++++++++++MARCA22222"+latitud_const+" "+ longitud_const);
            Polyline polyline = new Polyline(createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.BLUE), 8, Style.STROKE), AndroidGraphicFactory.INSTANCE);

            List<LatLong> latLongs = polyline.getLatLongs();

            latLongs.add(latLong5);
            latLongs.add(latLong6);
            Log.i("MyActivity", "No Location Provider Found Check Your Code++++++++++MARCA");
            //mapView.getLayerManager().getLayers().add(polyline); inicio funcion
            Log.i("MyActivity", "No Location Provider Found Check Your Code++++++++++MARCA");
            Log.i("MyActivity", "Nueva posicion"+location.getLatitude());
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

    Double latitudeFinal=0.1;
    Double longitudeFinal=0.1;
    Double latitud_Const=0.1;
    Double longitud_Const=0.1;

    void consultRuta(Double Latitude, Double Longitude,Double latitud_const, Double longitud_const){
        latitudeFinal=Latitude;
        longitudeFinal=Longitude;
        latitud_Const=latitud_const;
        longitud_Const=longitud_const;

        Log.i("MyActivityOscar", "ruta33333oscar");
        try {
            new hola().run();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("MyActivityOscar", "ruta2");

    }

// calcula distancia entre dos puntos de gps
    private static int calculateDistanceByHaversineFormula(double lon1, double lat1, double lon2, double lat2) {

        double earthRadius = 6371; // km

        lat1 = Math.toRadians(lat1);
        lon1 = Math.toRadians(lon1);
        lat2 = Math.toRadians(lat2);
        lon2 = Math.toRadians(lon2);

        double dlon = (lon2 - lon1);
        double dlat = (lat2 - lat1);

        double sinlat = Math.sin(dlat / 2);
        double sinlon = Math.sin(dlon / 2);

        double a = (sinlat * sinlat) + Math.cos(lat1)*Math.cos(lat2)*(sinlon*sinlon);
        double c = 2 * Math.asin (Math.min(1.0, Math.sqrt(a)));

        double distanceInMeters = earthRadius * c * 1000;

        return (int)distanceInMeters;

    }

    public final class hola {

        private final OkHttpClient client = new OkHttpClient();

        public void run() throws Exception {
            Request request = new Request.Builder()
                    .url("https://graphhopper.com/api/1/route?point="+latitud_Const+"%2C"+longitud_Const+"&point="+latitudeFinal+"%2C"+longitudeFinal+"&vehicle=foot&debug=false&locale=en&points_encoded=false&instructions=true&elevation=false&optimize=false&key=d668dbb5-c46c-4940-978e-cf2a0fc910fc")
                    .build();
            Log.i("Aplicacion","lat_cont "+latitud_Const+"longitud_Const: "+longitud_Const+"latitudeFinal: "+latitudeFinal+"longitudeFinal: "+longitudeFinal);
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.i("MyActivityOscar", "rutaXXXXX1");
                }


                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                public void onResponse(Response response) throws IOException {
                    Log.i("MyActivityOscar", "rutaXXXXX2");
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                        Headers responseHeaders = response.headers();
                        for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                            System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));

                            Log.i("MyActivityOscar", "rutaYYYYYXXXXXX"+responseHeaders.name(i) + ": " + responseHeaders.value(i));
                        }
                        String respuesta=responseBody.string();
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode json = objectMapper.readTree(respuesta);
                        JsonNode path = json.get("paths").get(0).get("points").get("coordinates");
                        Polyline polyline = new Polyline(createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.BLUE), 8, Style.STROKE), AndroidGraphicFactory.INSTANCE);

                        List<LatLong> latLongs = polyline.getLatLongs();
                        for(int puntoPosi=0;path.get(puntoPosi)!=null;puntoPosi++){
                            Log.i("MyActivityOscar", "rutaYYYYYXXXXXX222225555"+path.get(puntoPosi).get(0));
                            Log.i("MyActivityOscar", "rutaYYYYYXXXXXX222225555"+path.get(puntoPosi).get(1));

                            System.out.println("long = " + path.get(puntoPosi).get(0));
                            System.out.println("latitud = " + path.get(puntoPosi).get(1));

                            latLongs.add(new LatLong(Double.parseDouble(path.get(puntoPosi).get(1).toString()), Double.parseDouble(path.get(puntoPosi).get(0).toString())));
                            Log.i("MyActivity", "No Location Provider Found Check Your Code++++++++++MARCA");
                        }
                        mapView.getLayerManager().getLayers().add(polyline);
                    }
                }
            });
         }
    }

}