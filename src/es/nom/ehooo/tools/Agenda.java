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


import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ParseException;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.Patterns;

/**
 * @author ehooo
 * @version 1.0.0
 * This class are used to search in Android Agenda.
 */
public class Agenda {
	/**
	 * @param contentResolver
	 * @param number Number to search in the Agenda
	 * @return True if number are inside the Agenda or is a EmergencyNumber
	 */
	public static boolean isContact(final ContentResolver contentResolver, final String number){
		if(getEmailByNumber(contentResolver, number)!=null)
			return true;
		return PhoneNumberUtils.isEmergencyNumber(number);
	}

	/**
	 * @param contentResolver
	 * @param number Number to search in the Agenda
	 * @return Null or String with the Name of the contact with that number phone
	 */
	public static String getContactDisplayNameByNumber(final ContentResolver contentResolver, final String number) {
		final Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
	    String name = null;

	    final Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID, ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

	    try {
	        if (contactLookup != null && contactLookup.getCount() > 0) {
	            contactLookup.moveToNext();
	            name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
	        }
	    } finally {
	        if (contactLookup != null) {
	            contactLookup.close();
	        }
	    }

	    return name;
	}

	/**
	 * @param contentResolver
	 * @param number Number to search in the Agenda
	 * @return Null or String with the Email of the contact with that number phone
	 */
	public static String getEmailByNumber(final ContentResolver contentResolver, final String number) {
	    final Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
	    String email = null;

	    Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID, ContactsContract.PhoneLookup._ID }, null, null, null);

	    try {
	        if (contactLookup != null && contactLookup.getCount() > 0) {
	            contactLookup.moveToNext();
	            final long id = contactLookup.getLong(contactLookup.getColumnIndex(ContactsContract.PhoneLookup._ID));
	            email = "";
	            contactLookup.close();
	            contactLookup = contentResolver.query(	ContactsContract.CommonDataKinds.Email.CONTENT_URI,
	            										new String[] {ContactsContract.CommonDataKinds.Email.DATA}, 
	            										ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
	            										new String[] { String.valueOf(id) },
	            										null);

		        if (contactLookup != null && contactLookup.getCount() > 0) {
		            contactLookup.moveToNext();
		            email = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
		            if(email == null)
		            	email = "";
		        }
	        }
		} finally {
	        if (contactLookup != null) {
	            contactLookup.close();
	        }
	    }

	    return email;
	}

	/**
	 * @param email
	 * @return If the email is valid
	 */
	public static boolean isValidEmail(final String email) {
	    return Patterns.EMAIL_ADDRESS.matcher(email).matches();
	}
	/**
	 * Class for Call functions
	 */
	public static final class Call {

		/**
		 * @param to The phoneNumber to send the SMS
		 * @param message The text to send
		 * @throws IllegalArgumentException If destinationAddress or text are empty 
		 */
		public static void sendSMS(final String to, final String message) throws IllegalArgumentException{
			SmsManager.getDefault().sendTextMessage(to, null, message, null, null);        
	    }

		/**
		 * @param number
		 * @return If number is valid phone number
		 */
		public static final boolean isNumber(final String number){
			try {
				return (Number2Uri(number) != null && PhoneNumberUtils.isGlobalPhoneNumber(number));
			} catch (Exception e) {
				return false;
			}
		}
		/**
		 * @param to The number to send call
		 * @return Uri for call to number 'to'
		 * @throws ParseException if to is not a property number
		 */
		public static final Uri Number2Uri(final String to) throws ParseException{
			final Uri.Builder ub = new Uri.Builder();
			ub.scheme("tel");
			ub.query(to);
			return ub.build();
		}

		/**
		 * @param to The email to send the video call with Gtalk
		 * @return Uri for video gtalk to email 'to'
		 */
		public static final Uri Mail2GtalkVideoUri(final String to){
			final Uri.Builder ub = new Uri.Builder();
			ub.scheme("xmpp");
			ub.authority("gtalk");
			ub.query("call;type=video");
			ub.appendPath(to);
			return ub.build();
		}
		/**
		 * @param to Skype user to Video call
		 * @return Uri for video skype video call to user 'to'
		 */
		public static final Uri SkypeVideoUri(final String to){
			final Uri.Builder ub = new Uri.Builder();
			ub.scheme("skype");
			ub.authority(to);
			ub.query("video");
			return ub.build();
		}
		/**
		 * @param to Skype user to call
		 * @return Uri for video skype call to user 'to'
		 */
		public static final Uri SkypeCallUri(final String to){
			final Uri.Builder ub = new Uri.Builder();
			ub.scheme("skype");
			ub.authority(to);
			ub.query("call");
			return ub.build();
		}

		/**
		 * @param context
		 * @param to Phone number to call
		 * @return If call are running fine
		 * @throws SecurityException If the App have not permission for call
		 * @throws ParseException If Number2Uri throw
		 */
		public static final boolean call(final Context context, final String to) throws SecurityException, ParseException{
			try {
				final Intent call = new Intent(Intent.ACTION_CALL, Number2Uri(to));
				call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(call);
				return true;
			} catch (ActivityNotFoundException e) {
				Log.e(Call.class.getPackage().getName(), "CALL Not supported");
				return false;
			}
		}

		/**
		 * This function use Agenda.getEmailByNumber() for try made vcall with skype or gtalk 
		 * @param context
		 * @param to Phone number to call
		 * @return If vcall are running fine
		 * @throws SecurityException If the App have not permission for vcall
		 */
		public static final boolean vcall(final Context context, String to) throws SecurityException{
			final Intent call = new Intent();
			call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if( !skype_vcall(context, to) ){
				if(isNumber(to)){
					try {
						call.setData(Number2Uri(to));
						call.setAction("com.android.phone.videocall");
						call.putExtra("videocall", true);
						context.startActivity(call);
						return true;
					} catch (ActivityNotFoundException e) {
						Log.e(Call.class.getPackage().getName(), "VCALL (com.android.phone.videocall) Not supported");
						to = Agenda.getEmailByNumber(context.getContentResolver(), to);
					}
				}
				if(isValidEmail(to)){
					try {
						call.setData(Mail2GtalkVideoUri(to));
						call.setAction(Intent.ACTION_SENDTO);
						context.startActivity(call);
						return true;
					} catch (ActivityNotFoundException e) {
						Log.e(Call.class.getPackage().getName(), "VCALL (Gtalk) Not supported");
					}
				}else{
					Log.e(Call.class.getPackage().getName(), "Email, Skype user and/or number not valid for Video Call");
				}
			}
			return false;
		}

		/**
		 * @param context
		 * @param to number or user to send a skype video call
		 * @return If call are running fine
		 * @throws SecurityException
		 */
		public static final boolean skype_vcall(final Context context, final String to) throws SecurityException{
			final Intent call = new Intent();
			call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if(isNumber(to)){
				call.setData(SkypeCallUri(to));
			}else if(!isValidEmail(to)){
				call.setData(SkypeVideoUri(to));
			}

			if( call.getData()!=null ){
				try {
					call.setAction(Intent.ACTION_VIEW);
					call.addCategory(Intent.CATEGORY_DEFAULT);
					call.setType("vnd.android.cursor.item/com.skype.android.videocall.action");
					call.putExtra("video", true);
					context.startActivity(call);
					return true;
				} catch (ActivityNotFoundException e) {
					Log.e(Call.class.getPackage().getName(), "VCALL (Skype) Not supported");
				}
			}
			return false;
		}
		/**
		 * 
		 * @param context
		 * @param to Number or user to send the Skype Video call.
		 * @return If vcall are running fine
		 * @throws SecurityException
		 */
		public static final boolean skype_call(final Context context, final String to) throws SecurityException{
			final Intent call = new Intent();
			call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if(isNumber(to)){
				call.setData(Number2Uri(to));
				/*
				call.setClassName("com.skype.raider", "MainApp");
				/*/
				call.setPackage("com.skype.raider");
				//*/
				call.setAction("android.intent.action.CALL_PRIVILEGED");
			}else if(!isValidEmail(to)){
				call.setData(SkypeCallUri(to));
				call.setAction(Intent.ACTION_VIEW);
				call.setType("vnd.android.cursor.item/com.skype.android.skypecall.action");
			}

			if( call.getData()!=null ){
				try {
					call.addCategory(Intent.CATEGORY_DEFAULT);
					context.startActivity(call);
					return true;
				} catch (ActivityNotFoundException e) {
					Log.e(Call.class.getPackage().getName(), "VCALL (Skype) Not supported");
				}
			}
			return false;
		}

	}
}