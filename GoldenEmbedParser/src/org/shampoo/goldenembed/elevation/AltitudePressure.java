package org.shampoo.goldenembed.elevation;

public class AltitudePressure {

    private final float altitude;
    private double pressureAtAltitude;
    private float p_fil = 0; // initial pressure

    public AltitudePressure(float altitude) {
        this.altitude = altitude;

        // Actual Pressure at altitude.
        // pressureAtAltitude = 101325 * Math.pow((1 - 2.25577 * Math.pow(10,
        // -5) * altitude), 5.25588);
    }

    public float altiCalc(float _pres) {
        long i = 0, j = 0;
        float alti = 0;

        if (p_fil == 0)
            p_fil = _pres;

        // p_fil = (32 * p_fil + (_pres - p_fil)) / 32;

        float pres = Math.abs((float) (_pres - 1013.25 - p_fil));

        if (pres < 349) {
            i = 210;
            j = 15464;
        } else if (pres < 400.5) {
            i = 186;
            j = 14626;
        } else if (pres < 450) {
            i = 168;
            j = 13905;
        } else if (pres < 499) {
            i = 154;
            j = 13275;
        } else if (pres < 549) {
            i = 142;
            j = 12676;
        } else if (pres < 600) {
            i = 132;
            j = 12127;
        } else if (pres < 650) {
            i = 123;
            j = 11587;
        } else if (pres < 700) {
            i = 116;
            j = 11132;
        } else if (pres < 748) {
            i = 109;
            j = 10642;
        } else if (pres < 800) {
            i = 104;
            j = 10268;
        } else if (pres < 850) {
            i = 98;
            j = 9788;
        } else if (pres < 897.5) {
            i = 94;
            j = 9448;
        } else if (pres < 947.5) {
            i = 90;
            j = 9089;
        } else if (pres < 1006) {
            i = 86;
            j = 8710;
        } else if (pres < 1100) {
            i = 81;
            j = 8207;
        }
        alti = 10 * j - pres * i;

        return alti / 10 + altitude;
    }
}
