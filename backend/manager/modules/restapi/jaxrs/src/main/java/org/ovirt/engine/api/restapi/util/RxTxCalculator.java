package org.ovirt.engine.api.restapi.util;

public class RxTxCalculator {

    public static Double percent2bytes(Integer speedValueInMegaBits, Double rxTxValueInPrecent) {
        if (speedValueInMegaBits == null || rxTxValueInPrecent == null) {
            return null;
        }
        return megaBitToByte(speedValueInMegaBits * rxTxValueInPrecent / 100);
    }

    private static double megaBitToByte(double megaBit) {
        return megaBit * 125000;
    }

}
