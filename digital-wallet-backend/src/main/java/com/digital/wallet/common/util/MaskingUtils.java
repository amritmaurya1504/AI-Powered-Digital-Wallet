package com.digital.wallet.common.util;

public class MaskingUtils {
    public static String maskPhone(String phone){
        if(phone == null) {
            return "*****";
        }
        return "*****" + phone.substring(phone.length() - 4);
    }
}
