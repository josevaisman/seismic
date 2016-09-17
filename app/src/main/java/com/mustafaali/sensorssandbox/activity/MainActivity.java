package com.mustafaali.sensorssandbox.activity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.os.AsyncTask;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.androidplot.util.PlotStatistics;
import com.androidplot.xy.*;
import android.graphics.Color;

import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.json.JSONObject;
import org.json.JSONException;
import com.sematext.android.Logsene;

import com.mustafaali.sensorssandbox.R;
import com.mustafaali.sensorssandbox.adapter.SpinnerAdapter;

import java.util.List;
import android.widget.*;
import android.app.*;
import android.graphics.drawable.*;
import android.text.style.*;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import android.location.Location;
import android.view.View.*;
import java.security.*;


public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

	private static int REQUEST_CODE_RECOVER_PLAY_SERVICES = 200;
	
	private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
	private Double latitud = 0.0;
	private Double longitud = 0.0;
	
    private SensorManager mSensorManager;
    private List<Sensor> mSensors;
    private Sensor mSensor;
	private Spinner spinner;
    private TextView registrosTextView;
    private TextView velocidadTextView;
	private TextView ubicacionTextView;
	private TextView lupdateTextView;
	private TextView bandamediaTextView;
	private String locationupdate;
	private Integer numero = 0;
	private Long inicio = System.currentTimeMillis();
	private Long fin = System.currentTimeMillis();
	private float velocidad = 0;
	private int numero_inicio = 0;
	private Double valor_sensor = 9.7;
	private Double valor = 0.0;
	private Double valor_media = null;
	private Double valor_media_inf = null;
	private Double valor_media_sup = null;
	private Double valor_limit_inf = null;
	private Double valor_limit_sup = null;
	private Double delta_limite = 0.0;
	private Double factor_banda = 1.6;
	private Double factor_banda_media = 2.0;
	private Double spread_banda_media = 0.0;
	private Double spread_target = null;
	private Double banda_minima = 0.1;
	private Double valor_banda = null;
	private Double dec_minimo = 0.995;
	private Double dec_media = 0.99;
	private Double dec_env1 = 0.95;
	private Double delta_minimo = 1.0;
	private Boolean showgraph = false;
	private Boolean updateGraph = false;
	private Boolean quieto = false;
	private Boolean quieto_anterior = false;
	private Boolean estable_sup = false;
	private Boolean estable_inf = false;
    private static final int HISTORY_SIZE = 500;          
	
	private NotificationCompat.Builder notificacion;
	
	private XYPlot aprHistoryPlot = null;

	private SimpleXYSeries sensorHistorySeries = null;
	private SimpleXYSeries mediaHistorySeries = null;
	private SimpleXYSeries limiteHistorySeries = null;
	private SimpleXYSeries cotaHistorySeries = null;
	private SimpleXYSeries media_spot_inf_HistorySeries = null;
	private SimpleXYSeries media_spot_sup_HistorySeries = null;
	private SimpleXYSeries media_limit_inf_HistorySeries = null;
	private SimpleXYSeries media_limit_sup_HistorySeries = null;
	
	private JSONObject event = new JSONObject();
	private Boolean logPosition = false;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		
		if (!isTaskRoot()) {
			final Intent intent = getIntent();
			if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(intent.getAction())) {
				Log.w("Actividad", "Main Activity is not the root.  Finishing Main Activity instead of launching.");
				finish();
				return;       
			}
		}
		
        //Toast.makeText(getApplicationContext(), "onCreate", Toast.LENGTH_SHORT).show();
		
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initUi();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        displaySensorsList();
		
		//handler.postDelayed(r, 100);
		notificacion = recordingNotification();
		
		aprHistoryPlot = (XYPlot) findViewById(R.id.aprHistoryPlot);

        sensorHistorySeries = new SimpleXYSeries("Sensor");
        mediaHistorySeries = new SimpleXYSeries("Media");
        media_spot_inf_HistorySeries = new SimpleXYSeries("Media_Spot_Inf");
        media_spot_sup_HistorySeries = new SimpleXYSeries("Media_Spot_Sup");
        media_limit_inf_HistorySeries = new SimpleXYSeries("Media_Limit_Inf");
        media_limit_sup_HistorySeries = new SimpleXYSeries("Media_Limit_Sup");
        limiteHistorySeries = new SimpleXYSeries("Limite");
        cotaHistorySeries = new SimpleXYSeries("Cota");
        sensorHistorySeries.useImplicitXVals();
        mediaHistorySeries.useImplicitXVals();
        media_spot_inf_HistorySeries.useImplicitXVals();
        media_spot_sup_HistorySeries.useImplicitXVals();
        media_limit_inf_HistorySeries.useImplicitXVals();
        media_limit_sup_HistorySeries.useImplicitXVals();
        limiteHistorySeries.useImplicitXVals();
        cotaHistorySeries.useImplicitXVals();
        
        aprHistoryPlot.setDomainBoundaries(0, 500, BoundaryMode.FIXED);
        aprHistoryPlot.addSeries(sensorHistorySeries, new LineAndPointFormatter(Color.YELLOW, null, null, null));
        aprHistoryPlot.addSeries(mediaHistorySeries, new LineAndPointFormatter(Color.rgb(100, 200, 100), null, null, null));
        aprHistoryPlot.addSeries(limiteHistorySeries, new LineAndPointFormatter(Color.rgb(200, 100, 100), null, null, null));
        aprHistoryPlot.addSeries(cotaHistorySeries, new LineAndPointFormatter(Color.GRAY, null, null, null));
        aprHistoryPlot.addSeries(media_spot_inf_HistorySeries, new LineAndPointFormatter(Color.rgb(100, 100, 100), null, null, null));
        aprHistoryPlot.addSeries(media_spot_sup_HistorySeries, new LineAndPointFormatter(Color.rgb(100, 100, 100), null, null, null));
        aprHistoryPlot.addSeries(media_limit_inf_HistorySeries, new LineAndPointFormatter(Color.rgb(200, 100, 100), null, null, null));
        aprHistoryPlot.addSeries(media_limit_sup_HistorySeries, new LineAndPointFormatter(Color.rgb(200, 100, 100), null, null, null));
        aprHistoryPlot.setOnClickListener(clickgrafico);
		aprHistoryPlot.setDomainStepValue(1);
		aprHistoryPlot.setRangeStepValue(5);
		
        aprHistoryPlot.getRangeLabelWidget().pack();
		
		aprHistoryPlot.setBorderStyle(XYPlot.BorderStyle.NONE, null, null);
		aprHistoryPlot.setPlotMargins(0, 0, 0, 0);
		aprHistoryPlot.setPlotPadding(0, 0, 0, 0);
		aprHistoryPlot.setGridPadding(35, 35, 35, 35);
		
		aprHistoryPlot.getBackgroundPaint().setColor(Color.TRANSPARENT);
		aprHistoryPlot.getBorderPaint().setColor(Color.TRANSPARENT);
		
		aprHistoryPlot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
		aprHistoryPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.TRANSPARENT);
		aprHistoryPlot.getGraphWidget().getRangeGridLinePaint().setColor(Color.BLACK);
		aprHistoryPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);
		aprHistoryPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
		aprHistoryPlot.getGraphWidget().getRangeTickLabelPaint().setColor(Color.BLACK);
		aprHistoryPlot.getGraphWidget().getRangeOriginTickLabelPaint().setColor(Color.BLACK);
		aprHistoryPlot.getGraphWidget().getDomainOriginTickLabelPaint().setColor(Color.TRANSPARENT);
		
		aprHistoryPlot.getLayoutManager().remove(aprHistoryPlot.getLegendWidget());
		aprHistoryPlot.getLayoutManager().remove(aprHistoryPlot.getDomainLabelWidget());
		aprHistoryPlot.getLayoutManager().remove(aprHistoryPlot.getRangeLabelWidget());
		aprHistoryPlot.getLayoutManager().remove(aprHistoryPlot.getTitleWidget());
		
		
		final PlotStatistics histStats = new PlotStatistics(1000, false);

        aprHistoryPlot.addListener(histStats);
		if (checkGooglePlayServices()) {
			buildGoogleApiClient();
			
            //prepare connection request
            createLocationRequest();
		}
		
    }

	@Override
	public void onBackPressed()
	{
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		startActivity(intent);
	}
	
	private OnClickListener clickgrafico = new OnClickListener(){

		@Override
		public void onClick(View p1)
		{
			showgraph = !showgraph;
		}

	};
	
	protected void createLocationRequest() {
		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(60000);
		mLocationRequest.setFastestInterval(5000);
		mLocationRequest.setSmallestDisplacement(100);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
	}
	
	protected void stopLocationUpdates() {
		if (mGoogleApiClient != null) {
			LocationServices.FusedLocationApi.removeLocationUpdates(
				mGoogleApiClient, this);
		}
		//Toast.makeText(this, "stopLocationUpdates",Toast.LENGTH_SHORT).show();
		
	}
	
	protected void startLocationUpdates() {
		LocationServices.FusedLocationApi.requestLocationUpdates(
			mGoogleApiClient, mLocationRequest, this);
		//Toast.makeText(this, "startLocationUpdates",Toast.LENGTH_SHORT).show();
		
	}


	public void onLocationChanged(Location location) {
		mLastLocation = location;	
		locationupdate = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());
		latitud = mLastLocation.getLatitude();
		longitud = mLastLocation.getLongitude();
		//Toast.makeText(this, "Latitud:" + latitud+", Longitud:"+longitud,Toast.LENGTH_SHORT).show();
		logPosition = true;

	}
	
	private boolean checkGooglePlayServices() {

		int checkGooglePlayServices = GooglePlayServicesUtil
			.isGooglePlayServicesAvailable(this);
		if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
			/*
			 * google play services is missing or update is required
			 *  return code could be
			 * SUCCESS,
			 * SERVICE_MISSING, SERVICE_VERSION_UPDATE_REQUIRED,
			 * SERVICE_DISABLED, SERVICE_INVALID.
			 */
			GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices,
												  this, REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
			return false;
		}

		return true;

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_RECOVER_PLAY_SERVICES) {

			if (resultCode == RESULT_OK) {
				// Make sure the app is not already connected or attempting to connect
				if (!mGoogleApiClient.isConnecting() &&
					!mGoogleApiClient.isConnected()) {
					mGoogleApiClient.connect();
				}
			} else if (resultCode == RESULT_CANCELED) {
				//Toast.makeText(this, "Google Play Services must be installed.",
				//			   Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	
	@Override
	protected void onStart() {
		super.onStart();
		if (mGoogleApiClient != null) {
			mGoogleApiClient.connect();
		}

	}
	
	@Override
	public void onConnected(Bundle bundle) {

		mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
			mGoogleApiClient);
		if (mLastLocation != null) {
			locationupdate = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());
			latitud = mLastLocation.getLatitude();
			longitud = mLastLocation.getLongitude();
			//Toast.makeText(this, "Latitud:" + latitud+", Longitud:"+longitud,Toast.LENGTH_SHORT).show();
			logPosition = true;
		}
		startLocationUpdates();
	}

	@Override
	public void onConnectionSuspended(int i) {
		latitud = null;
		longitud = null;
		logPosition = false;
		//Toast.makeText(this, "onConnectionSuspended",Toast.LENGTH_SHORT).show();
		
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		latitud = null;
		longitud = null;
		logPosition = false;
		//Toast.makeText(this, "onConnectionFailed",Toast.LENGTH_SHORT).show();
		
	}
	
	
	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
			.addConnectionCallbacks(this)
			.addOnConnectionFailedListener(this)
			.addApi(LocationServices.API)
			.build();
	}
	
    private void initUi() {
        spinner = (Spinner) findViewById(R.id.sensors_spinner);
        spinner.setOnItemSelectedListener(onSpinnerItemSelectedListener);
		registrosTextView = (TextView) findViewById(R.id.registros_tv);
        velocidadTextView = (TextView) findViewById(R.id.velocidad_tv);
		ubicacionTextView = (TextView) findViewById(R.id.ubicacion_tv);
		lupdateTextView = (TextView) findViewById(R.id.lupdate_tv);
		bandamediaTextView = (TextView) findViewById(R.id.banda_media_tv);
    }

    private void displaySensorsList() {
		mSensors = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        SpinnerAdapter adapter = new SpinnerAdapter(this, R.layout.spinner_item, mSensors);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
    }


    @Override
    protected void onPause() {
		//Toast.makeText(getApplicationContext(), "onPause", Toast.LENGTH_SHORT).show();
        super.onPause();
		showgraph = false;
    }

	@Override
	protected void onDestroy()
	{
		if (mGoogleApiClient != null) {
			stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
		
		//Toast.makeText(getApplicationContext(), "onDestroy", Toast.LENGTH_SHORT).show();
		
		mSensorManager.unregisterListener(mSensorEventListener);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(0);
		
		latitud = null;
		longitud = null;
		logPosition = false;
		
		super.onDestroy();
	}
	
    @Override
    protected void onResume() {
		//Toast.makeText(getApplicationContext(), "onResume", Toast.LENGTH_SHORT).show();
		
        super.onResume();
        if (null != mSensor)
            mSensorManager.registerListener(mSensorEventListener, mSensor,
                    SensorManager.SENSOR_DELAY_UI);
					
		showgraph = true;
    }

    private OnItemSelectedListener onSpinnerItemSelectedListener = new OnItemSelectedListener() {
        
		@Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos,
                                   long id) {
            mSensor = mSensorManager.getDefaultSensor(mSensors.get(pos)
                    .getType());

            mSensorManager.unregisterListener(mSensorEventListener);
            
            mSensorManager.registerListener(mSensorEventListener, mSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
			
        }
    };

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
		
        @Override
        public void onSensorChanged(SensorEvent event) {
		
			numero++;
			registrosTextView.setText(numero.toString());
			velocidadTextView.setText(String.valueOf((int) Math.round(velocidad)).concat(" registros/segundo"));
			ubicacionTextView.setText(latitud+", "+longitud);
			lupdateTextView.setText(locationupdate);
			
			if(numero % 50 == 0 || numero < 200){
				fin = System.currentTimeMillis();
				velocidad = (numero-numero_inicio)/((fin - inicio)/((float) 1000));
				numero_inicio = numero;
				inicio = fin;
				notificacion.setContentInfo(numero.toString().concat(" registros - ").concat(String.valueOf((int) Math.round(velocidad))).concat(" registros/segundo") );
				notificacion.setSmallIcon(R.drawable.ic_notify);
				
				NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

				notificationManager.notify(0, notificacion.build());
				updateGraph = true;
			}
			else
			{
				updateGraph = false;
			}
	
			
			// add the latest history sample:
			valor_sensor = Math.sqrt(event.values[0]*event.values[0]+event.values[1]*event.values[1]+event.values[2]*event.values[2]);
			sensorHistorySeries.addLast(null, valor_sensor);
			
			if (valor_media == null) {
				valor_media = valor_sensor;
				valor_media_inf = valor_sensor;
				valor_media_sup = valor_sensor;
				valor_limit_inf = valor_sensor;
				valor_limit_sup = valor_sensor;
				}
			
			valor = valor_media * dec_media + valor_sensor * (1 - dec_media);
			valor_media = valor;
			valor_media_inf = Math.min(valor_media, valor_media_inf) * dec_minimo + valor_media * (1 - dec_minimo);
			valor_media_sup = Math.max(valor_media, valor_media_sup) * dec_minimo + valor_media * (1 - dec_minimo);
			valor_limit_inf = valor_limit_inf * dec_minimo + valor_media_inf * (1 - dec_minimo);
			valor_limit_sup = valor_limit_sup * dec_minimo + valor_media_sup * (1 - dec_minimo);
			
			spread_target = valor_limit_sup - valor_limit_inf;
			spread_banda_media = dec_media * spread_banda_media + (1 - dec_media) * spread_target;
			bandamediaTextView.setText(Math.round(spread_banda_media * 1000000)/1000000D + " -> " + Math.round(spread_target * 1000000)/1000000D);
			
			mediaHistorySeries.addLast(null, valor_media);
			media_spot_inf_HistorySeries.addLast(null, valor_media_inf);
			media_spot_sup_HistorySeries.addLast(null, valor_media_sup);
			media_limit_inf_HistorySeries.addLast(null, valor_limit_inf);
			media_limit_sup_HistorySeries.addLast(null, valor_limit_sup);
			
			valor = Math.sqrt(delta_limite * delta_limite * dec_env1 + (valor_sensor - valor_media)*(valor_sensor - valor_media) * (1 - dec_env1));
			delta_limite = valor;
			limiteHistorySeries.addLast(null, delta_limite + valor_media);

			delta_minimo = Math.min(delta_limite, delta_minimo) * dec_minimo + delta_limite * (1 - dec_minimo);
			valor_banda = Math.max(banda_minima, factor_banda * delta_minimo);
			cotaHistorySeries.addLast(null, valor_banda + valor_media);
			if(!estable_sup) estable_sup = valor_media_sup > valor_limit_sup;
			if(!estable_inf) estable_inf = valor_media_inf < valor_limit_inf;
			
			quieto_anterior = quieto;
			quieto = (delta_limite < valor_banda) && numero > 300 && 
				valor_media < valor_limit_sup * factor_banda_media + valor_limit_inf * (1 - factor_banda_media) && 
				valor_media > valor_limit_inf * factor_banda_media + valor_limit_sup * (1 - factor_banda_media) &&
				valor_media_sup - valor_media_inf < factor_banda_media * spread_banda_media &&
				valor_limit_sup - valor_limit_inf < factor_banda_media * spread_banda_media &&
				estable_sup && estable_inf;
			
			
			if (quieto && !quieto_anterior){
				aprHistoryPlot.removeSeries(sensorHistorySeries);
				aprHistoryPlot.addSeries(sensorHistorySeries, new LineAndPointFormatter(Color.rgb(100, 100, 200), null, null, null));
				
			}
			
			if (!quieto && quieto_anterior){
				aprHistoryPlot.removeSeries(sensorHistorySeries);
				aprHistoryPlot.addSeries(sensorHistorySeries, new LineAndPointFormatter(Color.YELLOW, null, null, null));
				estable_sup = valor_media_sup > valor_limit_sup;
				estable_inf = valor_media_inf < valor_limit_inf;
			}
			
			// get rid the oldest sample in history:
			if (sensorHistorySeries.size() > HISTORY_SIZE) {
				mediaHistorySeries.removeFirst();
				media_spot_inf_HistorySeries.removeFirst();
				media_spot_sup_HistorySeries.removeFirst();
				media_limit_inf_HistorySeries.removeFirst();
				media_limit_sup_HistorySeries.removeFirst();
				limiteHistorySeries.removeFirst();
				sensorHistorySeries.removeFirst();
				cotaHistorySeries.removeFirst();
			}

			LogData(valor_sensor);
			
			if(showgraph && updateGraph){
				// redraw the Plots:
				aprHistoryPlot.redraw();
			}
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
	
	private void LogData(Double val){
		try {
			event = new JSONObject();
			event.put("s", (double) Math.round(val * 100000) / 100000);
			event.put("t", System.currentTimeMillis());
			if (logPosition){
				event.put("lat", latitud);
				event.put("log", longitud);
				event.put("meta", 0);
				logPosition = false;
			}
			Logsene logsene = new Logsene(getApplication());
			logsene.event(event);
		} catch (JSONException e) {
			Log.e("sismic", "Unable to construct json", e);
		}
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

	@Override
	protected void onNewIntent(Intent intent)
	{
		//Toast.makeText(getApplicationContext(), "onNewIntent", Toast.LENGTH_SHORT).show();
		super.onNewIntent(intent);
	}
	
	
	public NotificationCompat.Builder recordingNotification(){
		{
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
			
			Drawable notify_ic = getDrawable(R.drawable.ic_notify);
			notify_ic.setTint(R.color.notify);
			
			NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_notify)
				//.setLargeIcon(((BitmapDrawable) notify_ic).getBitmap())
				.setContentTitle("Sismic")
				.setContentText("Grabando")
				.setAutoCancel(true)
				.setColor(R.color.grabando)
				//.setOnlyAlertOnce(true)
				.setOngoing(false)
				.setContentInfo("0")
				.setColor(Color.parseColor("#32CCF8"))
				//.setUsesChronometer(true)
				//.setShowWhen(true)
				.setContentIntent(pendingIntent);
				
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			
			notificationManager.cancel(0);
			notificationManager.notify(0, notificationBuilder.build());
			return notificationBuilder;
		}
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            showShareDialog();
            return true;
        } else if (item.getItemId() == R.id.action_close) {
			finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

    }

    private void showShareDialog() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));

        startActivity(Intent.createChooser(sendIntent,
                getResources().getText(R.string.send_to)));
    }
	
}
