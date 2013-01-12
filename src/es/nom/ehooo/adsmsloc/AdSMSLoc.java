package es.nom.ehooo.adsmsloc;

import es.nom.ehooo.tools.Crypto;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AdSMSLoc extends PreferenceActivity {

	public static final int AUTH_OPEN = 0x00;//00
	public static final int AUTH_PASSWORD = 0x01;//01
	public static final int AUTH_CONTACT = 0x02;//10
	public static final int AUTH_PASSWORD_CONTACT = AUTH_PASSWORD | AUTH_CONTACT;//0x03 -> 11

	public static final String DEFAULD_PASSWORD = "";
	public static final String DEFAULD_AUTH = String.valueOf(AUTH_CONTACT);
	public static final boolean DEFAULD_OTHER = false;
	public static final boolean DEFAULD_LOG = true;
	public static final boolean DEFAULD_GEO = true;
	public static final boolean DEFAULD_VCALL = false;
	public static final boolean DEFAULD_CALL = true;

	public static final boolean DEFAULD_GPS = true;
	public static final boolean DEFAULD_NETWORK = true;
	public static final boolean DEFAULD_WIFI = false;
	public static final boolean DEFAULD_PHONE_CELL = false;
	public static final int DEFAULD_TIMEOUT = 60;

	public static final String PREFERENCE_NAME = "AdSMSLoc";

    static final InputFilter no_space_filter = new InputFilter() {
    	public CharSequence filter(CharSequence source, int start, int end,
    			Spanned dest, int dstart, int dend) {
    		 for (int i = start; i < end; i++) {
    			 char c = source.charAt(i);
    			 //Only Allow Base64 charset and '_'
                 if (!(Character.isLetterOrDigit(c) || c=='+' || c=='/' || c=='=' || c=='_')) {
                     return "";
                 } 
    		 } 
    		 return null; 
    	}
    };

	Crypto crypt = null;
    final OnFocusChangeListener clean_password = new OnFocusChangeListener() {
		public void onFocusChange(View v, boolean hasFocus) {
			if(v instanceof EditText && hasFocus){
				EditText et = (EditText) v;
				String dec = crypt.decrypt( et.getText().toString() );
				et.setText( dec );
			}
		}
	};
	final OnPreferenceChangeListener crypt_password = new OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			EditTextPreference pass = (EditTextPreference) preference;
			String enc = crypt.encrypt((String)newValue);
			pass.setText( enc );//Como las password no puede tener \n lo sustituimos por un caracter no Base64 
        	return false;
		}
	};

	private final Activity getSelf(){
		return this;
	}
	final OnDismissListener check_password_ok = new OnDismissListener() {
		public void onDismiss(DialogInterface dialog) {
			if(auth_dialog_break){
				getSelf().finish();
			}
		}
	};

	final OnClickListener check_password = new OnClickListener() {
		public void onClick(View v) {
	    	final SharedPreferences pref = getSharedPreferences(AdSMSLoc.PREFERENCE_NAME, Context.MODE_PRIVATE);
	    	final String pass = crypt.encrypt( ((EditText) auth_dialog.findViewById(R.id.pasword)).getText().toString() );
	    	if(pref.getString("password", DEFAULD_PASSWORD).equals(pass)){
    			auth_dialog_break = false;
    			auth_dialog.dismiss();
    		}else{
    			auth_dialog_break = true;
    			Toast.makeText(getApplicationContext(), R.string.wrong_password, Toast.LENGTH_LONG).show();
    		}
		}
	};

	static Dialog auth_dialog;
	static boolean auth_dialog_break = true;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        crypt = new Crypto(this);

        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
        getPreferenceManager().setSharedPreferencesName(PREFERENCE_NAME);
    	addPreferencesFromResource(R.xml.opciones);

    	final EditTextPreference pass = (EditTextPreference) findPreference("password");
        pass.getEditText().setFilters(new InputFilter[]{no_space_filter});
        pass.setOnPreferenceChangeListener(crypt_password);
		pass.getEditText().setOnFocusChangeListener(clean_password);

    	final SharedPreferences pref = getSharedPreferences(AdSMSLoc.PREFERENCE_NAME, Context.MODE_PRIVATE);
    	if( (AUTH_PASSWORD & Integer.valueOf(pref.getString("auth_mode", DEFAULD_AUTH))) == AUTH_PASSWORD){

    		auth_dialog = new Dialog(this);
    		auth_dialog.setContentView(R.xml.checkpass);
    		auth_dialog.setTitle(R.string.password_insert);
			auth_dialog.setOnDismissListener(check_password_ok);

    		Button ok = (Button) auth_dialog.findViewById(R.id.ok);
    		ok.setOnClickListener(check_password);
    		auth_dialog.show();
    	}//*/
    }

    
}