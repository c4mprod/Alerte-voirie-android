package com.fabernovel.alertevoirie.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.telephony.TelephonyManager;

public class Utils {
    public static String getUdid(Context c) {
        TelephonyManager tel = (TelephonyManager) c.getSystemService(c.TELEPHONY_SERVICE);
        return tel.getDeviceId();
    }
    
    public static void fromInputToOutput(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
    }
}
