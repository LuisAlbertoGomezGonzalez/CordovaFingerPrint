package com.agomgon.mobile.artifactory.fingerprint;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import android.content.Context;
import android.content.Intent;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


public class FingerPrintPlugin extends CordovaPlugin {
 	//Interfaz de acciones llamadas por el Javascript
	public static final String TAG = "FingerPrintPlugin";
	public static final String ACTION_INSTANCE = "instance";
	public static final String ACTION_WRITEFINGERPRINT = "write";
	public static final String ACTION_AUTHENTICATE = "authenticate";
	public static final String ACTION_AUTHENTICATECUSTOM = "authenticatecustom";
	private static final int MAX_PARAMS = 6;

	//Variables globales internas
	private KeyguardManager keyguardManager;
	private FingerprintManager fingerprintManager;
	private SharedPreferences sharedPreferences;
	private KeyStore keyStore;
	private Cipher cipher;
	private boolean waitFlag = false;
	private int globalresult;
	private boolean isHwSupported = false;

	/**
	* Constructor.
	*/
	public FingerPrintPlugin() {

	}

	/**
	* Sets the context of the Command. This can then be used to do things like
	* get file paths associated with the Activity.
	*
	* @param cordova The context of the main Activity.
	* @param webView The CordovaWebView Cordova is running in.
	*/
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		Log.v(TAG,"FingerPrintPlugin");
		((MainActivity)cordova.getActivity()).setFingerPrintPluginHandler(this);
		try {
			keyguardManager = (KeyguardManager) cordova.getActivity().getSystemService(cordova.getActivity().getApplicationContext().KEYGUARD_SERVICE);
			fingerprintManager = (FingerprintManager) cordova.getActivity().getSystemService(cordova.getActivity().getApplicationContext().FINGERPRINT_SERVICE);
			sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.cordova.getActivity());
			comprobarHardware();
		}catch(Throwable t){
			Log.e("FingerPrintPlugin", t.toString());
		}
	}

	public boolean execute(final String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		boolean result = true;
		final int duration = Toast.LENGTH_SHORT;
		// Shows a toast
		Log.v(TAG,"FingerPrintPlugin"+ action);

		if(ACTION_AUTHENTICATE.equalsIgnoreCase(action)){
			result = authenticateAction();
		}else if(ACTION_AUTHENTICATECUSTOM.equalsIgnoreCase(action)){
			result = authenticateAction(args);
		}else if(ACTION_INSTANCE.equalsIgnoreCase(action)){
			result = instance();
		}else if(ACTION_WRITEFINGERPRINT.equalsIgnoreCase(action)){
			result = writeAction();
		}
		
		if(result)
			callbackContext.success();
		else {
			callbackContext.error(globalresult);
		}

		return result;
	}

	//Métodos del cumplimiento de la interfaz
	private boolean instance(){
		boolean result = true;
		return result;
	}

	private boolean writeAction() {
		boolean result = false;
		try {
			writeFingerprint(null);
			result = true;
		}catch(Exception e){
			result = false;
		}
		return result;
	}

	//==============================================================================================
	//Segundo nivel: toma la acción del javascript y lanza los métodos java nativos necesarios.
	//==============================================================================================
	/**
	 * Comprueba que el dispositivo tenga lector de huellas. Si no lo tiene muestra un dialogo y cierra la app.
	 */
	private void comprobarHardware() {
		if(!fingerprintManager.isHardwareDetected()) { //TODO: documentar el tema de permisos
			isHwSupported = false;
		}
	}

	/**
	 * abre la ventana de opciones para la escritura de la huella digital.
	 * @param view
     */
	public void writeFingerprint(View view) {
		abrirAjustesSeguridad(this.cordova.getActivity().getApplicationContext().getString(R.string.configuracion_seguridad_acceda_huella_digital));
	}

	/**
	 * comprueba la huella digital con el hash previamente almacenado por el writeFingerprint
	 * @return
     */
	private boolean authenticateAction(){
		boolean result=false;

		try {
			waitFlag = true;
			authenticate("","","","",-1);
			lockForResult();
			result = globalresult == 0;
		}catch(Exception e){
			result = false;
		}

		return result;
	}

	private boolean authenticateAction(JSONArray args){
		boolean result = true;

		try{
			if(args != null && MAX_PARAMS == args.length()){
				waitFlag = true;
				authenticate(String.valueOf(args.get(CommomMessages.POS_TITLE)),
						String.valueOf(args.get(CommomMessages.POS_HEADER)),
						String.valueOf(args.get(CommomMessages.POS_HELP)),
						String.valueOf(args.get(CommomMessages.POS_BTNCANCEL)),
						(Integer)(args.get(CommomMessages.POS_RETRY)));
				lockForResult();
				result = globalresult == 0;
			}else{
				result = false;
			}
		}catch(Exception e){
			Log.e(this.getClass().getCanonicalName(), e.toString());
			result = false;
		}

		return result;
	}

	//==============================================================================================
	//Tercer nivel: son los métodos auxiliares que enlazan con las otras clases.
	//==============================================================================================
	/**
	 * Abre la aplicación Ajustes con las opciones de seguridad. Además muestra el mensaje pasado por parámetro
	 * @param mensaje mensaje que se muestra al abrir la aplicación de Ajustes
	 */
	private void abrirAjustesSeguridad(String mensaje) {
		Context context=this.cordova.getActivity().getApplicationContext();
		Intent intent=new Intent(Settings.ACTION_SECURITY_SETTINGS);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	/**
	 * Muestra un dialogo para la autenticación por huella digital especificando los mensajes que aparecerán en el mismo.
	 *
	 * @param title Título del dialogo
	 * @param messageHeader Mensaje para la cabecera
	 * @param messageHelp Mensaje de ayuda
	 * @param messageCancel Texto del botón de cancelar
	 * @param maxIntentos Numero máximo e intentos que se permite hacer por huella digital
	 */
	public void authenticate(String title, String messageHeader, String messageHelp, String messageCancel, int maxIntentos) {
		try {
			int configuracionSeguridad = verificarConfiguracionDispositivo();

			if(configuracionSeguridad == CommomMessages.SEGURIDAD_CONFIGURADA) {
				createKey();

				Bundle bundle = new Bundle();
				bundle.putString(FingerprintDialogFragment.ARG_TITLE_KEY,  title);
				bundle.putString(FingerprintDialogFragment.ARG_HEADER_KEY, messageHeader);
				bundle.putString(FingerprintDialogFragment.ARG_HELP_KEY,   messageHelp);
				bundle.putString(FingerprintDialogFragment.ARG_CANCEL_KEY, messageCancel);
				bundle.putInt(FingerprintDialogFragment.ARG_MAX_INTENTOS_KEY,  maxIntentos);

				FingerprintDialogFragment fragment = new FingerprintDialogFragment();
				fragment.setArguments(bundle);

				//----- No permitir que el dialog se cierre al tocar fuera del mismo -----
				fragment.setCancelable(false);

				if(cipherInit()) {
					FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);
					fragment.setCryptoObject(cryptoObject);

					boolean useFingerprintPreference = sharedPreferences.getBoolean(cordova.getActivity().getApplicationContext().getString(R.string.use_fingerprint_to_authenticate_key), true);
					if(useFingerprintPreference) {
						fragment.setStage(FingerprintDialogFragment.Stage.FINGERPRINT);
					}
					else {
						fragment.setStage(FingerprintDialogFragment.Stage.PASSWORD);
					}

					fragment.show(cordova.getActivity().getFragmentManager(), CommomMessages.DIALOG_FRAGMENT_TAG);
				}
				else {
					fragment.setCryptoObject(new FingerprintManager.CryptoObject(cipher));
					fragment.setStage(FingerprintDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
					fragment.show(this.cordova.getActivity().getFragmentManager(), CommomMessages.DIALOG_FRAGMENT_TAG);
				}
			}
			else {
				devolverResultado(CommomMessages.RETURN_NO_CONTRASTADA);
			}
		}catch(Throwable t) {
			Log.e("authenticate", t.toString());
		}
	}

	/**
	 * Espera síncrona a finalización de la operación.
	 * @throws Exception
     */
	private void lockForResult() throws Exception{
		while(waitFlag){
			Thread.sleep(2500);
		}
	}

	/**
	 * Verifica la configuración de seguridad del dispositivo (Blqueo por PIN o por patrón, y que al menos
	 * haya una huella digital configurada).
	 *
	 * @return
	 * -1 si la seguridad no está configurada,
	 *  0 si la seguridad está configurada,
	 */
	private int verificarConfiguracionDispositivo() {
		int resultadoVerificacion = CommomMessages.SEGURIDAD_NO_CONFIGURADA;

		//----- Compobar que tenga configurada la pantalla de bloqueo -----
		if(keyguardManager.isKeyguardSecure()) {
			// Comprobar que el permiso este activado (No es necesario, el permiso se concede automaticamente)
			if(fingerprintManager.hasEnrolledFingerprints()) { //TODO: documentar el tema de permisos
				resultadoVerificacion = CommomMessages.SEGURIDAD_CONFIGURADA;
			}
			else {
				abrirAjustesSeguridad(cordova.getActivity().getString(R.string.configuracion_seguridad_registre_huella));
			}
		}
		else {
			abrirAjustesSeguridad(cordova.getActivity().getString(R.string.configuracion_seguridad_configure_bloqueo));
		}

		return resultadoVerificacion;
	}

	private void createKey() {
		try {
			keyStore = KeyStore.getInstance("AndroidKeyStore");

			KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

			keyStore.load(null);
			keyGenerator.init(new
					KeyGenParameterSpec.Builder(CommomMessages.KEY_NAME,
					KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
					.setBlockModes(KeyProperties.BLOCK_MODE_CBC)
					.setUserAuthenticationRequired(true)
					.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
					.build());
			keyGenerator.generateKey();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public boolean cipherInit() {
		try {
			cipher = Cipher.getInstance(
					KeyProperties.KEY_ALGORITHM_AES + "/" +
							KeyProperties.BLOCK_MODE_CBC + "/" +
							KeyProperties.ENCRYPTION_PADDING_PKCS7);

			keyStore.load(null);
			SecretKey key = (SecretKey) keyStore.getKey(CommomMessages.KEY_NAME, null);
			cipher.init(Cipher.ENCRYPT_MODE, key);

			return true;
		}
		catch(Exception e) {
			return false;
		}
	}

	/**
	 * Cierra el Activity devolviendo el valor pasado por argumento
	 * @param valor
	 */
	private void devolverResultado(int valor) {
		Intent returnIntent = new Intent();
		returnIntent.putExtra("result", valor);
		cordova.getActivity().setResult(Activity.RESULT_FIRST_USER, returnIntent);

		Log.d("AUTHENTICATE RESULT", "Valor = " + valor);
		globalresult = valor;
		waitFlag = false;
	}

	/**
	 * Cierra el Activity devolviendo el valor RESULT_CANCELED
	 */
	private void devolverCancelado() {
		Intent returnIntent = new Intent();
		cordova.getActivity().setResult(Activity.RESULT_CANCELED, returnIntent);

		Log.d("AUTHENTICATE RESULT", "CANCELADO");
		globalresult = CommomMessages.RETURN_CANCELADO;
		waitFlag = false;
		//finish();
	}

	//==============================================================================================
	// Interfaces de devolución de datos del fragment de validación
	//==============================================================================================
	public void onCancelled(boolean isAuthenticateByFingerprint) {
		String mensaje = "Cancelado desde ";
		if(isAuthenticateByFingerprint) {
			mensaje += "Huella Digital";
		}
		else {
			mensaje += "Contraseña";
		}

		//Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();

		devolverCancelado();
	}

	public void onAuthenticate(boolean isAuthenticateByFingerprint) {
		String mensaje = "Autenticado por ";
		if(isAuthenticateByFingerprint) {
			mensaje += "Huella Digital";
		}
		else {
			mensaje += "Contraseña";
		}

		//Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();

		devolverResultado(CommomMessages.RETURN_AUTENTICADO);
	}
}