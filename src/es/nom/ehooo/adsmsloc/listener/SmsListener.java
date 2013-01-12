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
package es.nom.ehooo.adsmsloc.listener;

import es.nom.ehooo.adsmsloc.AdSMSLoc;
import es.nom.ehooo.adsmsloc.R;
import es.nom.ehooo.tools.Agenda;
import es.nom.ehooo.tools.Crypto;
import es.nom.ehooo.tools.Locator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.SmsMessage;

/**
 * Intercept the SMS and check with the schema, for make the work.
 * Format (GPS|CALL|VCALL) [password] [response_other]
 * Could check if the SMS remiter are in the Agenda.
 * @author ehooo
 */
public class SmsListener extends BroadcastReceiver{

	public static final String ACTION_GPS = "GPS";
	public static final String ACTION_CALL = "CALL";
	public static final String ACTION_VCALL = "VCALL";
	private Context context = null;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		/*
		if (((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getCallState() != TelephonyManager.CALL_STATE_IDLE) {
            return;
	    }//*/

	    final Bundle bundle = intent.getExtras();
	    if (bundle == null)
	    	return;
	    final Object[] pdusObj = (Object[]) bundle.get("pdus");

	    SmsMessage message = null;
	    for (int i = 0; i < pdusObj.length; i++) {
	    	message = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
			parseSMS(context, message.getDisplayMessageBody(), message.getOriginatingAddress());
	    }
	}

	String response_to;
	/**
	 * Verifi the SMS with format and confing
	 * @param msg SMS content
	 * @param orige The phone number of SMS
	 */
	private void parseSMS(final Context context, final String msg, final String orige){
		this.context = context;
		String action = null;
		String password = null;
		String other = null;
		if(orige==null) return;
		response_to = orige;

		final String keys[] = msg.split(" ");
		switch (keys.length) {
			case 3:
				other = keys[2];
			case 2:
				password = keys[1];
			case 1:
				action = keys[0];
				if( ACTION_CALL.equalsIgnoreCase(action) ||
					ACTION_VCALL.equalsIgnoreCase(action) ||
					ACTION_GPS.equalsIgnoreCase(action) )
					break;
			default:
				return;
		}
    	final SharedPreferences pref = context.getSharedPreferences(AdSMSLoc.PREFERENCE_NAME, Context.MODE_PRIVATE);

    	if( (AdSMSLoc.AUTH_CONTACT & Integer.valueOf(pref.getString("auth_mode", AdSMSLoc.DEFAULD_AUTH))) == AdSMSLoc.AUTH_CONTACT){
    		if(!Agenda.isContact(context.getContentResolver(), orige))
    			return;
		}
    	if( (AdSMSLoc.AUTH_PASSWORD & Integer.valueOf(pref.getString("auth_mode", AdSMSLoc.DEFAULD_AUTH))) == AdSMSLoc.AUTH_PASSWORD){
	    	if(password==null || !pref.getString("password", AdSMSLoc.DEFAULD_PASSWORD).equals(new Crypto(context).encrypt(password)))
    			return;
		}else if(password!=null){
			other = password;
		}

    	if(other!=null){
    		if(pref.getBoolean("response_other", AdSMSLoc.DEFAULD_OTHER) && Agenda.Call.isNumber(other))
    			response_to = other;
    		else
    			return;
		}

		if(!pref.getBoolean("log_sms", AdSMSLoc.DEFAULD_LOG))
			this.abortBroadcast();

    	proccessAction(action);
	}
	Locator locator;

	/**
	 * Process the action and send the response 
	 * @param action Action to do values: SmsListener.ACTION_VCALL SmsListener.ACTION_CALL or SmsListener.ACTION_GPS
	 * @param response_to The number to response the action
	 */
	private void proccessAction(final String action){
    	final SharedPreferences pref = context.getSharedPreferences(AdSMSLoc.PREFERENCE_NAME, Context.MODE_PRIVATE);

		if(ACTION_CALL.equalsIgnoreCase(action) && pref.getBoolean("call", AdSMSLoc.DEFAULD_CALL)){
			if(!CallListener.call(context, response_to))
				Agenda.Call.sendSMS( response_to, context.getString(R.string.error_call) );

		}else if(ACTION_VCALL.equalsIgnoreCase(action) && pref.getBoolean("vcall", AdSMSLoc.DEFAULD_VCALL)){
			if(!Agenda.Call.vcall(context, response_to))
				Agenda.Call.sendSMS( response_to, context.getString(R.string.error_vcall) );
			
		}else if(ACTION_GPS.equalsIgnoreCase(action) && pref.getBoolean("geo", AdSMSLoc.DEFAULD_GEO)){
			locator = new Locator(context);

			if( pref.getBoolean("gps", AdSMSLoc.DEFAULD_GPS) ){
				locator.set("gps");
			}if( pref.getBoolean("network", AdSMSLoc.DEFAULD_NETWORK) ){
				locator.set("network");
			}if( pref.getBoolean("phone_cell", AdSMSLoc.DEFAULD_PHONE_CELL) ){
				locator.set("cell");
			}if( pref.getBoolean("wifi", AdSMSLoc.DEFAULD_WIFI) ){
				locator.set("wifi");
			}
			final Thread hilo = new Thread(){
				public void run() {
					if(context != null){
				    	final SharedPreferences pref = context.getSharedPreferences(AdSMSLoc.PREFERENCE_NAME, Context.MODE_PRIVATE);
						try {
							long timeout = pref.getInt("timeout", AdSMSLoc.DEFAULD_TIMEOUT) * 1000;
							if(timeout<=0)
								timeout = AdSMSLoc.DEFAULD_TIMEOUT * 1000;
							Thread.sleep(timeout);
							Looper.prepare();
							locator.cleanListeners();
							Agenda.Call.sendSMS( response_to, locator.toString() );
							Looper.loop();
						} catch (InterruptedException e) {}
					}
				}
			};
			hilo.start();
		}
	}

	

}
