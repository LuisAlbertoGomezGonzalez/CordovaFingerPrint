package com.agomgon.mobile.artifactory.fingerprint;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class FingerprintDialogFragment extends DialogFragment implements FingerprintUiHelper.Callback {

    private static final int RESULT_CODE_PASSWORD = 1;

    public static final String ARG_TITLE_KEY        = "title";
    public static final String ARG_HEADER_KEY       = "messageHeader";
    public static final String ARG_HELP_KEY         = "messageHelp";
    public static final String ARG_CANCEL_KEY       = "messageCancel";
    public static final String ARG_MAX_INTENTOS_KEY = "numIntentos";

    //----- Para guardar el numero de intentos actuales cuando se gira la pantalla -----
    private static final String INTENTOS_ACTUALES_KEY = "numIntentosActuales";

    /**
     * Enumeration to indicate which authentication method the user is trying to authenticate with.
     */
    public enum Stage {
        FINGERPRINT,
        NEW_FINGERPRINT_ENROLLED,
        PASSWORD
    }

    private Button cancelButton;
    private Button usarPassButton;
    private TextView fingerprintDescriptionTextView;

    private Stage mStage = Stage.FINGERPRINT;

    private FingerprintManager.CryptoObject cryptoObject;
    private FingerprintUiHelper fingerprintUiHelper;
    private IFingerPrintPlugin mActivity;

    //----- Controla que no se llame al dialogo de autenticacion al sistema cuando ya se está mostrando el mismo -----
    private boolean isSystemPasswordDialogShowing;

    public FingerprintDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //----- Poner el título por defecto -----
        getDialog().setTitle(getString(R.string.dialog_huella_titulo));
        View view = inflater.inflate(R.layout.fragment_fingerprint_dialog, container, false);

        inicializarElementos(view);

        if(savedInstanceState != null) {
            int numIntentosActuales = savedInstanceState.getInt(INTENTOS_ACTUALES_KEY);

            fingerprintUiHelper.setNumIntentosActuales(numIntentosActuales);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(fingerprintUiHelper != null) {
            savedInstanceState.putInt(INTENTOS_ACTUALES_KEY, fingerprintUiHelper.getNumIntentosActuales());
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    private void inicializarElementos(View view) {
        isSystemPasswordDialogShowing = false;

        FingerprintManager fpManager = (FingerprintManager) getContext().getSystemService(Context.FINGERPRINT_SERVICE);
        FingerprintUiHelper.FingerprintUiHelperBuilder fingerprintUiHelperBuilder =
                new FingerprintUiHelper.FingerprintUiHelperBuilder(fpManager);


        cancelButton   = (Button) view.findViewById(R.id.cancel_button);
        usarPassButton = (Button) view.findViewById(R.id.usar_pass_button);

        setButtonListeners();

        fingerprintDescriptionTextView = (TextView) view.findViewById(R.id.fingerprint_description);

        fingerprintUiHelper = fingerprintUiHelperBuilder.build(
                (ImageView) view.findViewById(R.id.fingerprint_icon),
                (TextView) view.findViewById(R.id.fingerprint_status), this);

        obtenerArguments();

        updateStage();

        // If fingerprint authentication is not available, switch immediately to the backup
        // (password) screen.
        if(!fingerprintUiHelper.isFingerprintAuthAvailable()) {
            pedirPasswordSistema(getString(R.string.password_titulo), getString(R.string.password_description));
        }
    }

    private void setButtonListeners() {
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                mActivity.onCancelled(true);
            }
        });

        usarPassButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mStage == Stage.FINGERPRINT) {
                    pedirPasswordSistema(getString(R.string.password_titulo), getString(R.string.password_description));
                }
            }
        });
    }

    public void obtenerArguments() {
        Bundle bundle = getArguments();

        String title         = bundle.getString(ARG_TITLE_KEY);
        String messageHeader = bundle.getString(ARG_HEADER_KEY);
        String messageHelp   = bundle.getString(ARG_HELP_KEY);
        String messageCancel = bundle.getString(ARG_CANCEL_KEY);

        //----- Devolver -1 por defecto -----
        int numIntentos      = bundle.getInt(   ARG_MAX_INTENTOS_KEY, -1);
        // TODO - Obtener la imagen del icono

        if(title != null && !title.equals("")) {
            getDialog().setTitle(title);
        }

        if(messageHeader != null && !messageHeader.equals("")) {
            fingerprintDescriptionTextView.setText(messageHeader);
        }

        if(messageHelp != null && !messageHelp.equals("")) {
            fingerprintUiHelper.setMessageHelp(messageHelp);
        }

        if(messageCancel != null && !messageCancel.equals("")) {
            cancelButton.setText(messageCancel);
        }

        if(numIntentos > 0) {
            fingerprintUiHelper.setNumIntentosMax(numIntentos);
        }
    }

    /**
     *
     * @param titulo Titulo que se mostrará en la ventana
     * @param mensaje Mensaje
     */
    private void pedirPasswordSistema(String titulo, String mensaje) {
        fingerprintUiHelper.stopListening();

        if(!isSystemPasswordDialogShowing) {
            KeyguardManager keyguardManager = (KeyguardManager) (mActivity.getSystemServiceFingerprint(Context.KEYGUARD_SERVICE));
            Intent passwordIntent = keyguardManager.createConfirmDeviceCredentialIntent(titulo, mensaje);

            startActivityForResult(passwordIntent, RESULT_CODE_PASSWORD);

            isSystemPasswordDialogShowing = true;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_CODE_PASSWORD) {
            isSystemPasswordDialogShowing = false;

            if(resultCode == Activity.RESULT_OK){
                mActivity.onAuthenticate(false);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                mActivity.onCancelled(false);
            }

            dismiss();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (IFingerPrintPlugin) activity;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mStage == Stage.FINGERPRINT) {
            fingerprintUiHelper.startListening(cryptoObject);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        fingerprintUiHelper.stopListening();
    }

    public void setStage(Stage stage) {
        mStage = stage;
    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        this.cryptoObject = cryptoObject;
    }

    private void updateStage() {
        switch(mStage) {
            case NEW_FINGERPRINT_ENROLLED:
                pedirPasswordSistema(getString(R.string.password_titulo), getString(R.string.new_fingerprint_enrolled_description));
                break;

            case PASSWORD:
                pedirPasswordSistema(getString(R.string.password_titulo), getString(R.string.password_description));
                break;
        }
    }

    @Override
    public void onAuthenticated() {
        // Callback from es.renfe.mobile.artifactory.fingerprint.FingerprintUiHelper. Let the activity know that authentication was
        // successful.
        mActivity.onAuthenticate(true);

        dismiss();
    }

    @Override
    public void onError() {
        pedirPasswordSistema(getString(R.string.password_titulo), getString(R.string.password_description));
    }
}