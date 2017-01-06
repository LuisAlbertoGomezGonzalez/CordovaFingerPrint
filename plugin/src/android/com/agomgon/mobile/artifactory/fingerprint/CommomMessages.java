package com.agomgon.mobile.artifactory.fingerprint;

/**
 * Mensajes comunes a toda la utilidad
 */
public class CommomMessages {
    public static final String KEY_NAME = "example_key";
    public static final String ID_INTERFACE = "renfefingerprint";

    public static final String DIALOG_FRAGMENT_TAG = "dialogFragment";

    public static final int SEGURIDAD_NO_CONFIGURADA = -1;
    public static final int SEGURIDAD_CONFIGURADA    =  0;

    public static final int RETURN_AUTENTICADO    = 0;
    public static final int RETURN_OKGENERICO     = 0;
    public static final int RETURN_NO_HARDWARE    = 2;
    public static final int RETURN_NO_CONTRASTADA = 4;
    public static final int RETURN_PIN_NO_VALIDO  = 5;
    public static final int RETURN_MAX_INTENTOS   = 6;
    public static final int RETURN_CANCELADO      = 7;

    public static final int OP_WRITE              = 1;
    public static final int OP_AUTH               = 2;
    
    public static final int POS_TITLE = 0;
    public static final int POS_HEADER = 1;
    public static final int POS_HELP = 3;
    public static final int POS_BTNCANCEL = 4;
    public static final int POS_RETRY = 5;
}
