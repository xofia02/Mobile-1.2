package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.myapplication.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    //para usar a localização do dispositivo:
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    //marcador ira mostrar localização atual
    private Marker currentLocationMarker;
    //faz com que o mapa gire de acordo com o giro do dispositivo
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    //
    private float currentHeading = 0f;

    // Callback de localização para atualizar o marcador a cada 5 segundos
    private final LocationCallback locationCallback = new LocationCallback() {

        //atualiza a localização do marcador toda vez que uma nova é recebida
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }

            LatLng newLocation = new LatLng(
                    locationResult.getLastLocation().getLatitude(),
                    locationResult.getLastLocation().getLongitude()
            );

            if (currentLocationMarker != null) {
                currentLocationMarker.setPosition(newLocation);
            } else {
                currentLocationMarker = mMap.addMarker(new MarkerOptions().position(newLocation).title(null));
            }

            float currentZoom = mMap.getCameraPosition().zoom;
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(newLocation)
                    .zoom(currentZoom)  // Usa o nível de zoom ja estabelicido pelo usuario
                    .bearing(currentHeading)  // Define a rotação com base na orientação do dispositivo
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Para obter a localização do dispositivo
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // chama os serviços de sensores do andoroid e retorna a instancia
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        // obtém o SupportMapFragment e é notificado quando o mapa está pronto para uso
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        UiSettings mapUI = mMap.getUiSettings();
        mapUI.setAllGesturesEnabled(true);//permite gestos para controlar visão do mapa
        mapUI.setCompassEnabled(true); //bussola no canto da tela
        mapUI.setZoomControlsEnabled(true); //botão de mais e menos para cotrolar zoom

        // Solicita permissão de localização do usuário
        requestLocationPermission();
    }

    private void requestLocationPermission() {
        // verifica se a permissão de localização já foi concedida
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // pede permissão
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Pprmissão já foi concedida; inicia a atualização de localização
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão foi concedida; inicia a atualização de localização
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Sem permissão de localização", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Inicia as atualizações de localização com intervalo de 5 segundos
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(5000);  // onde é definido que a atualização é cada 5 segundos
            locationRequest.setFastestInterval(2000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    //inicia tarefas usadas quando o sistema está ativo, a atualização da localização e os sensores
    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    //o oposto do onResume, ele para as atividades quando o sistema for ser encerrado
    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        sensorManager.unregisterListener(this);
    }

    //pega os dados do sensor e os converte em graus, assim mudando a rotação do mapa
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);

            // Calcula o heading (em graus) para atualizar o mapa
            currentHeading = (float) Math.toDegrees(orientation[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Não é necessário implementar, mas ele pode ser usado para lidar com mudanças na precisão dos sensores (como lidar com a perda de precisão em sensores magnéticos, por exemplo).
    }
}