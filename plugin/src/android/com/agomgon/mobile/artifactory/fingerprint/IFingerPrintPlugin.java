package com.agomgon.mobile.artifactory.fingerprint;

public interface IFingerPrintPlugin {
    void onAuthenticate(boolean b);
    void onCancelled(boolean b);
    Object getSystemServiceFingerprint(String service);
}
