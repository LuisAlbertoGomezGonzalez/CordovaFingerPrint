package com.agomgon.mobile.artifactory.fingerprint;

import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
//import android.support.annotation.VisibleForTesting;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * Small helper class to manage text/icon around fingerprint authentication UI.
 */
public class FingerprintUiHelper extends FingerprintManager.AuthenticationCallback {

    static final long ERROR_TIMEOUT_MILLIS = 1600;
    static final long SUCCESS_DELAY_MILLIS = 1300;

    private final FingerprintManager mFingerprintManager;
    private final ImageView mIcon;
    private final TextView mErrorTextView;
    private final Callback mCallback;
    private CancellationSignal mCancellationSignal;

    private String messageHelp;
    private int numMaxIntentos;
    private int numActualIntentos;

    boolean mSelfCancelled;

    /**
     * Builder class for {@link FingerprintUiHelper} in which injected fields from Dagger
     * holds its fields and takes other arguments in the {@link #build} method.
     */
    public static class FingerprintUiHelperBuilder {
        private final FingerprintManager mFingerPrintManager;

        public FingerprintUiHelperBuilder(FingerprintManager fingerprintManager) {
            mFingerPrintManager = fingerprintManager;
        }

        public FingerprintUiHelper build(ImageView icon, TextView errorTextView, Callback callback) {
            return new FingerprintUiHelper(mFingerPrintManager, icon, errorTextView, callback);
        }
    }

    /**
     * Constructor for {@link FingerprintUiHelper}. This method is expected to be called from
     * only the {@link FingerprintUiHelperBuilder} class.
     */
    private FingerprintUiHelper(FingerprintManager fingerprintManager,
                                ImageView icon, TextView errorTextView, Callback callback) {
        mFingerprintManager = fingerprintManager;
        mIcon = icon;
        mErrorTextView = errorTextView;
        mCallback = callback;
        messageHelp = errorTextView.getResources().getString(R.string.fingerprint_hint);

        numMaxIntentos = -1;
        numActualIntentos = 0;
    }

    public boolean isFingerprintAuthAvailable() {
        boolean disponible = mFingerprintManager.isHardwareDetected() && mFingerprintManager.hasEnrolledFingerprints();

        return disponible && (numMaxIntentos==-1 || numActualIntentos < numMaxIntentos);
    }

    public void startListening(FingerprintManager.CryptoObject cryptoObject) {
        if(!isFingerprintAuthAvailable()) {
            return;
        }

        mCancellationSignal = new CancellationSignal();
        mSelfCancelled = false;
        mFingerprintManager.authenticate(cryptoObject, mCancellationSignal, 0 /* flags */, this, null);
        mIcon.setImageResource(R.drawable.ic_fp_40px);
    }

    public void stopListening() {
        if(mCancellationSignal != null) {
            mSelfCancelled = true;
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        if(!mSelfCancelled || (numMaxIntentos!=-1 && numActualIntentos>=numMaxIntentos)) {
            showError(errString);
            mIcon.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCallback.onError();
                }
            }, ERROR_TIMEOUT_MILLIS);
        }
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        showError(helpString);
    }

    @Override
    public void onAuthenticationFailed() {
        showError(mIcon.getResources().getString(R.string.fingerprint_not_recognized));
        numActualIntentos++;

        if(numActualIntentos == numMaxIntentos) {
            stopListening();
        }
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        mErrorTextView.removeCallbacks(mResetErrorTextRunnable);
        mIcon.setImageResource(R.drawable.ic_fingerprint_success);
        mErrorTextView.setTextColor(mErrorTextView.getResources().getColor(R.color.success_color, null));
        mErrorTextView.setText(mErrorTextView.getResources().getString(R.string.fingerprint_success));
        mIcon.postDelayed(new Runnable() {
            @Override
            public void run() {
                mCallback.onAuthenticated();
            }
        }, SUCCESS_DELAY_MILLIS);
    }

    private void showError(CharSequence error) {
        mIcon.setImageResource(R.drawable.ic_fingerprint_error);
        mErrorTextView.setText(error);
        mErrorTextView.setTextColor(mErrorTextView.getResources().getColor(R.color.warning_color, null));
        mErrorTextView.removeCallbacks(mResetErrorTextRunnable);
        mErrorTextView.postDelayed(mResetErrorTextRunnable, ERROR_TIMEOUT_MILLIS);
    }

    public void setMessageHelp(String messageHelp) {
        this.messageHelp = messageHelp;

        mErrorTextView.setText(this.messageHelp);
    }

    public void setNumIntentosMax(int numMaxIntentos) {
        this.numMaxIntentos = numMaxIntentos;
    }

    public void setNumIntentosActuales(int numActualIntentos) {
        this.numActualIntentos = numActualIntentos;
    }

    public int getNumIntentosActuales() {
        return numActualIntentos;
    }

    //@VisibleForTesting
    Runnable mResetErrorTextRunnable = new Runnable() {
        @Override
        public void run() {
            mErrorTextView.setTextColor(mErrorTextView.getResources().getColor(R.color.hint_color, null));

            mErrorTextView.setText(messageHelp);
            //mErrorTextView.setText(mErrorTextView.getResources().getString(R.string.fingerprint_hint));
            mIcon.setImageResource(R.drawable.ic_fp_40px);
        }
    };

    public interface Callback {
        void onAuthenticated();

        void onError();
    }
}