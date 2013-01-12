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

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;

/**
 * This class are used to en/decrypt a "String" using 'PBEWithMD5AndDES'.<br>
 * By default use 10 iterations with:<br>
 * The password: 'Settings.Secure.ANDROID_ID' (if is not null) or 'Build.ID'<br>
 * The salt: Build.BOOTLOADER + Build.HARDWARE + Build.FINGERPRINT
 * @author ehooo
 * @version 1.0.0
 */
public class Crypto {

	Cipher encript = null;
	Cipher decript = null;

	public Crypto(Context context) {
		//*
		String passPhrase = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		if(passPhrase == null)
			passPhrase = Build.ID;
		final String salt = Build.BOOTLOADER.concat(Build.HARDWARE).concat(Build.FINGERPRINT);
		//*/
		this.regenere(passPhrase, salt, 10);
	}

	/**
	 * Generate the crypto keys.
	 * @param passPhrase
	 * @param salt
	 * @param iteration
	 */
	public void regenere(String passPhrase, String salt, int iteration){
		this.regenere(passPhrase.toCharArray(), salt.getBytes(), iteration);
	}
	/**
	 * Generate the crypto keys.
	 * @param passPhrase
	 * @param salt
	 * @param iteration
	 */
	public void regenere(char[] passPhrase, byte[] salt, int iteration){
        final KeySpec keySpec = new PBEKeySpec(passPhrase, salt, iteration);
		try {
			final SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
			final AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iteration);

	        decript = Cipher.getInstance(key.getAlgorithm());
	        decript.init(Cipher.DECRYPT_MODE, key, paramSpec);
	        encript = Cipher.getInstance(key.getAlgorithm());
	        encript.init(Cipher.ENCRYPT_MODE, key, paramSpec);	        
		} catch (InvalidKeySpecException e) {
		} catch (NoSuchAlgorithmException e) {
		} catch (NoSuchPaddingException e) {
		} catch (InvalidKeyException e) {
		} catch (InvalidAlgorithmParameterException e) {
		}
	}

	/**
	 * decrypt a crypto text.
	 * @param crypto_text
	 * @return String with clean text
	 */
	public String decrypt(String crypto_text) {
		if(crypto_text == "" || decript==null) return "";
		try {
			final byte[] dec = Base64.decode(crypto_text.replace("_", "\n"), Base64.DEFAULT);
			final byte[] utf8 = decript.doFinal(dec);
			return new String(utf8, "UTF8");
		} catch (IllegalBlockSizeException e) {
		} catch (BadPaddingException e) {
		} catch (UnsupportedEncodingException e) {
		}
		return "";
	}
	/**
	 * encrypt a clean text.
	 * @param pain_text
	 * @return Encrypted string (in Base64 changing the \n to _)
	 */
	public String encrypt(String pain_text){
		if( "".equals(pain_text) || encript==null) return "";
		try {
			final byte[] enc = encript.doFinal(pain_text.getBytes("UTF8"));
			return Base64.encodeToString(enc, Base64.DEFAULT).replace("\n", "_");
		} catch (IllegalBlockSizeException e) {
		} catch (BadPaddingException e) {
		} catch (UnsupportedEncodingException e) {
		}
        return "";
	}


}
