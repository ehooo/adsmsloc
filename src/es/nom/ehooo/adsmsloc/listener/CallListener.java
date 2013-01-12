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

import es.nom.ehooo.tools.Agenda;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;

/**
 * Intercept the done calls for turn on the Speaker
 * @author ehooo
 */
public class CallListener extends BroadcastReceiver{

	static Uri last_call = null;
	/**
	 * Send call to 'to'
	 * @param context
	 * @param to Phone number to call
	 */
	public static final boolean call(final Context context, final String to){
		try{
			CallListener.last_call = Agenda.Call.Number2Uri(to);
			if(!Agenda.Call.call(context, to))
				CallListener.last_call = null;
			return true;
		}catch (Exception e) {
			CallListener.last_call = null;
		}
		return false;
	}

	/**
	 * Intercept the call if is the call it throw turn on the Speakers and the Bluetooth
	 */
	@Override
	public void onReceive(final Context context, final Intent intent) {
		//final String newState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
		//if(TelephonyManager.EXTRA_STATE_OFFHOOK.equals(newState)){//Realizando una llamada
		if(CallListener.last_call!=null && intent.hasExtra(Intent.EXTRA_PHONE_NUMBER)){
			final String callingTo = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
			if(!CallListener.last_call.toString().endsWith(callingTo)){
				//CallListener.last_call = null;
				return;
			}
			CallListener.last_call = null;

			final AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
			if( audioManager.isBluetoothA2dpOn() )
				audioManager.setBluetoothScoOn(true);
			if( !audioManager.isSpeakerphoneOn() )
				audioManager.setSpeakerphoneOn(true);

			audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), AudioManager.FLAG_SHOW_UI);
		}
		//}
	}
}