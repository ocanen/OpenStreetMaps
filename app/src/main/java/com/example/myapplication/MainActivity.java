package com.example.myapplication;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ZoomButton;

import androidx.core.app.ActivityCompat;


import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.util.shapes.GHPoint;
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

        //String MAP_FILE_RAW = "android.resource://" + getPackageName() + "/res/raw/" + "madrid2.map";
        //String CSV_FILE_RAW = "android.resource://" + getPackageName() + "/" + R.raw.aseos;
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
        //MapDataStore mapDataStore = new MapFile(new File(MAP_FILE_RAW));
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
        //MyMarker marker;
    //lectura de csv
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

                // nextLine[] is an array of values from the line
                //Log.i("MyActivity", tokens[10]);
                //Log.i("MyActivity", tokens[11]);
                if(i!=0){
                    /*
                    if (Double.parseDouble(tokens[10]) == numLong) {
                        longitud_if= Double.parseDouble(tokens[10]);
                        Log.i("MyActivityENTRA!!!!", "ENTRA!!!! "+longitud_if);
                        banderaLong=true;
                    } else if(!banderaLong){
                        //Log.i("MyActivity", "calculo"+Math.abs(Double.parseDouble(tokens[10])-numLong)+"valor:"+numLong+"longitud estatica:"+diferenciaLong+"i: "+i);
                        if(Math.abs(Double.parseDouble(tokens[10])-numLong)<diferenciaLong){
                            cercanoLong=Double.parseDouble(tokens[10]);
                            longitud_const=Double.parseDouble(tokens[10]);
                            latitud_const=Double.parseDouble(tokens[11]);
                            Log.i("MyActivity", "cercanaLong:"+longitud_const);
                            Log.i("MyActivity", "cercanalatitud:"+latitud_const);
                            diferenciaLong = Math.abs(Double.parseDouble(tokens[10])-numLong);
                        }
                        longitud_if=cercanoLong;
                        //Log.i("MyActivity", "longitud:"+cercanoLong);
                    }*/


                    if (Double.parseDouble(tokens[11]) == numLatitud) {
                        longitud_if= Double.parseDouble(tokens[11]);
                        Log.i("MyActivityENTRA!!!!", "ENTRA!!!! "+longitud_if);
                        banderaLong=true;
                    } else if(!banderaLong){
                        //Log.i("MyActivity", "calculo"+Math.abs(Double.parseDouble(tokens[10])-numLong)+"valor:"+numLong+"longitud estatica:"+diferenciaLong+"i: "+i);
                        if(Math.abs(Double.parseDouble(tokens[11])-numLatitud)<diferenciaLatitud){
                            cercanoLatitud=Double.parseDouble(tokens[11]);
                            longitud_const=Double.parseDouble(tokens[10]);
                            latitud_const=Double.parseDouble(tokens[11]);
                            Log.i("MyActivity", "cercanaLong:"+longitud_const);
                            Log.i("MyActivity", "cercanalatitud:"+latitud_const);
                            diferenciaLatitud = Math.abs(Double.parseDouble(tokens[11])-numLatitud);
                        }
                        longitud_if=cercanoLatitud;
                        //Log.i("MyActivity", "longitud:"+cercanoLong);
                    }
                    // int calculateDistanceByHaversineFormula(double lon1, double lat1, double lon2, double lat2)
                   if(i==1)distanciaMinima=calculateDistanceByHaversineFormula(numLong,numLatitud,Double.parseDouble(tokens[10]),Double.parseDouble(tokens[11]));
                    else{
                        if(distanciaMinima>calculateDistanceByHaversineFormula(numLong,numLatitud,Double.parseDouble(tokens[10]),Double.parseDouble(tokens[11]))){
                           distanciaMinima=calculateDistanceByHaversineFormula(numLong,numLatitud,Double.parseDouble(tokens[10]),Double.parseDouble(tokens[11]));
                            Log.i("MyActivity", "mas cercano:"+"latitud: "+ Double.parseDouble(tokens[10]) + "longitud: "+Double.parseDouble(tokens[11]));
                            Log.i("MyActivity", "distancia"+distanciaMinima);
                       }
                   }





                    /*
                    if (Double.parseDouble(tokens[11]) == numLatitud) {
                        latitud= Double.parseDouble(tokens[11]);
                    } else {
                        if(Math.abs(Double.parseDouble(tokens[11])-numLatitud)<diferenciaLatitud){
                            cercanoLatitud=Double.parseDouble(tokens[11]);
                            diferenciaLatitud = Math.abs(Double.parseDouble(tokens[11])-numLatitud);
                        }
                    }
*/
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
/*
        LatLong latLong5 = new LatLong(40.3713163, -3.6183235);
        LatLong latLong6 = new LatLong(latitud, longitud);
        Polyline polyline = new Polyline(createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.BLUE), 8, Style.STROKE), AndroidGraphicFactory.INSTANCE);

        List<LatLong> latLongs = polyline.getLatLongs();

        latLongs.add(latLong5);
        latLongs.add(latLong6);
        Log.i("MyActivity", "No Location Provider Found Check Your Code++++++++++MARCA");
        mapView.getLayerManager().getLayers().add(polyline);
        Log.i("MyActivity", "No Location Provider Found Check Your Code++++++++++MARCA");
        */

        //
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
        /*
        OkHttpClient client = new OkHttpClient();
        Log.i("MyActivityOscar", "ruta");
        Request request = new Request.Builder()
                .url("http://publicobject.com/helloworld.txt")
                .get()
                .build();
        Log.i("MyActivityOscar", "ruta1");
        try{
            Response response = client.newCall(request).execute();
            Log.i("MyActivityOscar", "RUTA");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Log.i("MyActivityOscar", "rutaError");
        }
        */
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

                            //Log.i("MyActivityOscar", "rutaYYYYYXXXXXX"+responseHeaders.name(i) + ": " + responseHeaders.value(i));
                        }
                       //Log.i("MyActivityOscar", "rutaYYYYYXXXXXX2222244"+responseBody.string());
                        String respuesta=responseBody.string();
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode json = objectMapper.readTree(respuesta);
                        JsonNode path = json.get("paths").get(0).get("points").get("coordinates");
                        //JsonNode subpath = path.get("points");
                        //JsonNode subsubpath = subpath.get("coordinates");
                        //System.out.println("car brand = " + json.get("paths"));
                        //System.out.println("car brand = " + subsubpath);
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
                       // Log.i("MyActivityOscar", "rutaYYYYYXXXXXX222225555"+respuesta);
                        //JSONObject json = new JSONObject(respuesta);
                        //JSONArray
                        //JSONObject x = new JSONObject(respuesta);
                        //JSONArray y = x.getJSONArray("hints");
                        /*
                        ObjectMapper objectMapper;
                        Set<String> ignoreSet;

                        ignoreSet = new HashSet<>();
                        ignoreSet.add("calc_points");
                        ignoreSet.add("calcpoints");
                        ignoreSet.add("instructions");
                        ignoreSet.add("elevation");
                        ignoreSet.add("key");
                        ignoreSet.add("optimize");

                        // some parameters are in the request:
                        ignoreSet.add("algorithm");
                        ignoreSet.add("locale");
                        ignoreSet.add("point");
                        ignoreSet.add("vehicle");

                        // some are special and need to be avoided
                        ignoreSet.add("points_encoded");
                        ignoreSet.add("pointsencoded");
                        ignoreSet.add("type");
                        objectMapper = Jackson.newObjectMapper();
                        JsonNode json = objectMapper.reader().readTree(responseBody.byteStream());

                        Log.i("MyActivityOscar", "rutaYYYYYXXXXXX222225555"+json);
                        //JSONArray jArray = json.getJSONArray("hints");
                        Log.i("MyActivityOscar", "rutaYYYYYXXXXXX22222555566");
                        Log.i("MyActivityOscar", "rutaYYYYYXXXXXX222223"+response);

                         */
                    }/*catch (JSONException e) {
                        e.printStackTrace();
                    }*/
                }
            });
            /*
            Log.i("MyActivity", "Pintar.... llega"+longitud_Const+" "+longitudeFinal);
            GHRequest req = new GHRequest(latitud_Const, longitud_Const, latitudeFinal, longitudeFinal)
                    .setWeighting("fastest")
                    .setVehicle("car");

            List<GHPoint> points = req.getPoints();
            GHPoint punto ;
            Log.i("MyActivity", "Pintar.... llega2 "+points.size());
            Log.i("MyActivity", "Pintar.... llega2 "+ points.get(0));
            Log.i("MyActivity", "Pintar.... llega2 "+points.get(1));
            for(int i=0;points.isEmpty() && points.size()>i;){
                punto = points.get(i);
                Polyline polyline = new Polyline(createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.BLUE), 8, Style.STROKE), AndroidGraphicFactory.INSTANCE);

                List<LatLong> latLongs = polyline.getLatLongs();

                latLongs.add(new LatLong(punto.lat, punto.lon));
                punto = points.get(++i);
                latLongs.add(new LatLong(punto.lat, punto.lon));
                Log.i("MyActivity", "Pintar....");
                mapView.getLayerManager().getLayers().add(polyline);

                Log.i("MyActivityOscar", "rutaYpunto");
                Log.i("MyActivityOscar", "rutaYpunto"+points.get(i).getLat());
                i=i+2;
                   }
             */
                /*
                Polyline polyline = new Polyline(createPaint(AndroidGraphicFactory.INSTANCE.createColor(Color.BLUE), 8, Style.STROKE), AndroidGraphicFactory.INSTANCE);

                List<LatLong> latLongs = polyline.getLatLongs();

                latLongs.add(points.get(i).getLat());
                latLongs.add(latLong6);
                Log.i("MyActivity", "No Location Provider Found Check Your Code++++++++++MARCA");
                mapView.getLayerManager().getLayers().add(polyline);
                //marker = new MyMarker(this, new LatLong(Double.parseDouble(tokens[11]), Double.parseDouble(tokens[10])), AndroidGraphicFactory.convertToBitmap(getResources().getDrawable(R.mipmap.pequeno)), 0, 0);

                 */




        }
/*
para obtener los puntos aunque hay que probarlo y añadirle la key

GHRequest req = new GHRequest(32.070113, 34.790266, 32.067103, 34.777861)
   .setWeighting("fastest")
   .setVehicle("car");

GHResponse res = gh.route(req);

if(res.hasErrors()) {
   // handle or throw exceptions res.getErrors()
   return;
}

// get path geometry information (latitude, longitude and optionally elevation)
PointList pl = res.getPoints();
// distance of the full path, in meter
double distance = res.getDistance();
// time of the full path, in milliseconds
long millis = res.getTime();
// get information per turn instruction
InstructionList il = res.getInstructions();



        public static void juan(String... args) throws Exception {
            new com.example.myapplication.hola().run();
        }*/
    }
/*
    public void clickbutton(View v) {
        try {
            // Log.i(getClass().getSimpleName(), "send task - start");
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLISEC);
            HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
            // HttpParams p = new BasicHttpParams();
            // p.setParameter("name", pvo.getName());
            p.setParameter("user", "1"); // Instantiate an HttpClient
            HttpClient httpclient = new DefaultHttpClient(p);
            String url = "http://bhavit.xtreemhost.com/webservice1.php?user=1&format=json";
            HttpPost httppost = new HttpPost(url); // Instantiate a GET HTTP method
             try {
                 Log.i(getClass().getSimpleName(), "send task - start");
                 // List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>( 7);
                 nameValuePairs.add(new BasicNameValuePair("details", "1"));
                 httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                 ResponseHandler<String> responseHandler = new BasicResponseHandler();
                 String responseBody = httpclient.execute(httppost, responseHandler);
                 Parse JSONObject json = new JSONObject(responseBody);
                 JSONArray jArray = json.getJSONArray("posts");
                 ArrayList<HashMap<String, String>> mylist = new ArrayList<HashMap<String, String>>();
                 for (int i = 0; i < jArray.length(); i++) {
                     HashMap<String, String> map = new HashMap<String, String>();
                     JSONObject e = jArray.getJSONObject(i);
                     String s = e.getString("post");
                     JSONObject jObject = new JSONObject(s);
                     map.put("time", jObject.getString("time"));
                     map.put("latitude", jObject.getString("latitude"));
                     map.put("longitude", jObject.getString("longitude"));
                     mylist.add(map); } Toast.makeText(this, responseBody, Toast.LENGTH_LONG).show();
                 **String[] columns = new String[] { "Time", "Latitude", "Longitude" };
                 int[] renderTo = new int[] { R.id.time, R.id.latitude, R.id.longitude };
                 ListAdapter listAdapter = new SimpleAdapter(this, mylist, R.layout.geo_ponts_list, columns, renderTo);
                 setListAdapter(listAdapter);
                 **
             } catch (ClientProtocolException e) { // TODO Auto-generated catch block e.printStackTrace(); } catch (IOException e) { // TODO Auto-generated catch block e.printStackTrace(); } // Log.i(getClass().getSimpleName(), "send task - end"); } catch (Throwable t) { Toast.makeText(this, "Request failed: " + t.toString(), Toast.LENGTH_LONG).show(); } } private void setListAdapter(ListAdapter listAdapter) { // TODO Auto-generated method stub } public class Data { // private List<User> users; public List<details> users; // +getters/setters } static class details { String latitude; String longitude; String time; public String Longitude() { return longitude; } public String Latitude() { return latitude; } public String Time() { return time; } public void setUserName(String value) { longitude = value; } public void setidusers(String value) { latitude = value; }
*/



}