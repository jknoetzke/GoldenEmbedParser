package org.shampoo.goldenembed.parser;

import java.util.List;

public class Power {
    private double watts;
    private double nm;
    private double rpm;
    private double p;
    private double t;
    private double r;
    private double v; // SRM
    private double totalWattCounter;
    private double totalCadCounter;
    public boolean first12 = false;
    public boolean first0x20 = false;
    private boolean debug = false;
    private boolean megaDebug = false;
    private final GoldenEmbedParserMain parent;
    long lastWattSecs = 0; // To keep track of the last time watts were saved.

    public Power(boolean debug, boolean megaDebug, GoldenEmbedParserMain parent) {
        this.debug = debug;
        this.megaDebug = megaDebug;
        this.parent = parent;
    }

    public boolean isFirst0x20() {
        return first0x20;
    }

    public void setFirst0x20(boolean first0x20) {
        this.first0x20 = first0x20;
    }

    public boolean isFirst12() {
        return first12;
    }

    public void setFirst12(boolean first12) {
        this.first12 = first12;
    }

    public double getTotalCadCounter() {
        return totalCadCounter;
    }

    public void setTotalCadCounter(double totalCadCounter) {
        this.totalCadCounter = totalCadCounter;
    }

    public double getV() {
        return v;
    }

    public double getTotalWattCounter() {
        return totalWattCounter;
    }

    public void setTotalWattCounter(double totalWattCounter) {
        this.totalWattCounter = totalWattCounter;
    }

    public void setV(double v) {
        this.v = v;
    }

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public double getWatts() {
        return watts;
    }

    public void setWatts(double watts) {
        this.watts = watts;
    }

    public double getNm() {
        return nm;
    }

    public void setNm(double nm) {
        this.nm = nm;
    }

    public double getRpm() {
        return rpm;
    }

    public void setRpm(double rpm) {
        this.rpm = rpm;
    }

    public double getP() {
        return p;
    }

    public void setP(double p) {
        this.p = p;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    boolean first0x11 = true; // For 0x11 messages

    public boolean isFirst0x11() {
        return first0x11;
    }

    public void setFirst0x11(boolean first0x11) {
        this.first0x11 = first0x11;
    }

    boolean first0x12 = true; // For 0x12 messages

    public boolean isFirst0x12() {
        return first0x12;
    }

    public void setFirst0x12(boolean first0x12) {
        this.first0x12 = first0x12;
    }

    private double cnt;

    public double getCnt() {
        return cnt;
    }

    public void setCnt(double cnt) {
        this.cnt = cnt;
    }

    public void ANTParsePower(byte[] msgData, int size, GoldenCheetah gc,
            List<GoldenCheetah> gcArray) {
        int i = 4;
        if (megaDebug)
            System.out.println("0x" + UnicodeFormatter.byteToHex(msgData[i]));
        if (msgData[i] == 0x12) // Parse ANT+ 0x12 message (QUARQ)
        {
            ANTParsePower0x12(msgData, size, gc, gcArray);
        } else if (msgData[i] == 0x11) {
            ANTParsePower0x11(msgData, size, gc);
        } else if (msgData[i] == 0x20) {
            ANTParseSRMPower(msgData, size, gc, gcArray);
        }

    }

    private void ANTParseSRMPower(byte[] msgData, int size, GoldenCheetah gc,
            List<GoldenCheetah> gcArray) {
        int t1;
        int p1;
        int r1;
        int v1;
        double period = 0;

        int end = 13;
        double torque = 0;
        double revdiff = 0;

        double nm;
        double rpm;
        double watts;

        Byte aByte;
        int msgN = 0;

        // SS LL BB CH PG RV V1--- TT--- V2--- CK
        // A4 09 4E 01 20 A8 01 A1 67 DA 63 5A 4E 00 05 1B
        // A4 09 4E 01 20 A9 01 A1 6D 79 67 C4 7C 00 05 1C

        // RV: Event Counter (this.R)
        // V1: Slope (this.P) (Slope for GC)
        // TT: Time Stamp (this.T) (Period for GC)
        // V2: Torque
        // CK: Torque Ticks
        /*
         * case ANT_CRANKSRM_POWER: // 0x20 - crank torque (SRM) eventCount =
         * message[5]; slope = message[7] + (message[6]<<8); // yes it is
         * bigendian period = message[9] + (message[8]<<8); // yes it is
         * bigendian torque = message[11] + (message[10]<<8); // yes it is
         * bigendian break;
         */

        /*
         * // SRM - crank torque frequency // case ANT_CRANKSRM_POWER: // 0x20 -
         * crank torque (SRM) { uint16_t period = antMessage.period -
         * lastMessage.period; uint16_t torque = antMessage.torque -
         * lastMessage.torque; float time = (float)period / (float)2000.00;
         * 
         * if (time && antMessage.slope && period) {
         * 
         * nullCount = 0; float torque_freq = torque / time - 420/*srm_offset;
         * float nm_torque = 10.0 * torque_freq / antMessage.slope; float
         * cadence = 2000.0 * 60 * (antMessage.eventCount -
         * lastMessage.eventCount) / period; float power = 3.14159 * nm_torque *
         * cadence / 30;
         */

        for (int i = 5; i < end; i++) {
            if (megaDebug)
                System.out.println("0x"
                        + UnicodeFormatter.byteToHex(msgData[i]));
            if (msgN == 0) { // Event Count
                if (this.first0x20) {
                    // Just store it.
                    aByte = new Byte(msgData[i]);
                    this.setR(aByte.intValue());
                    if (megaDebug)
                        System.out.println("R: " + aByte.intValue());
                } else {
                    // We can calculate and then store
                    aByte = new Byte(msgData[i]);
                    r1 = aByte.intValue();
                    revdiff = r1 - this.getR();
                    if (revdiff > 250)
                        revdiff = this.getR() - (r1 + 255);
                    this.setR(aByte.intValue());
                    if (megaDebug)
                        System.out.println("revdiff is: " + revdiff);
                }
                msgN++;
            } else if (msgN == 1) { // Slope
                byte[] pRdiff = new byte[2];
                pRdiff[0] = msgData[i];
                if (megaDebug)
                    System.out.println("0x"
                            + UnicodeFormatter.byteToHex(msgData[i]) + "\n");
                i++;
                pRdiff[1] = msgData[i];
                if (megaDebug)
                    System.out.println("0x"
                            + UnicodeFormatter.byteToHex(msgData[i]) + "\n");
                p1 = GoldenEmbedParserMain.byteArrayToInt(pRdiff, 0, 2);
                this.setP(p1);

                msgN++;
            } else if (msgN == 2) // Period
            {
                byte[] pRdiff = new byte[2];
                pRdiff[0] = msgData[i++];
                if (megaDebug)
                    System.out
                            .println("0x"
                                    + UnicodeFormatter
                                            .byteToHex(msgData[i - 1]) + "\n");
                pRdiff[1] = msgData[i];
                if (megaDebug)
                    System.out.println("0x"
                            + UnicodeFormatter.byteToHex(msgData[i]) + "\n");

                t1 = GoldenEmbedParserMain.byteArrayToInt(pRdiff, 0, 2);

                if (this.first0x20) {
                    this.setT(t1);
                    if (megaDebug)
                        System.out.println("T: " + t1);
                } else {
                    period = t1 - this.getT();
                    if (Math.abs(period) > 60000)
                        period = this.getT() - (t1 + 65536);
                    this.setT(t1);
                    if (megaDebug)
                        System.out.println("timediff is: " + period);
                }

                msgN++;

            } else if (msgN == 3) // Torque
            {
                byte[] pRdiff = new byte[2];
                pRdiff[0] = msgData[i++];
                if (megaDebug)
                    System.out
                            .println("0x"
                                    + UnicodeFormatter
                                            .byteToHex(msgData[i - 1]) + "\n");
                pRdiff[1] = msgData[i];
                if (megaDebug)
                    System.out.println("0x"
                            + UnicodeFormatter.byteToHex(msgData[i]) + "\n");

                v1 = GoldenEmbedParserMain.byteArrayToInt(pRdiff, 0, 2);

                if (this.first0x20) {
                    this.setV(v1);
                    if (megaDebug)
                        System.out.println("V: " + v1);
                } else {
                    torque = v1 - this.getV();
                    if (Math.abs(torque) > 60000)
                        torque = this.getV() - (v1 + 65536);
                    this.setV(v1);
                    if (megaDebug)
                        System.out.println("vdiff is: " + torque);
                }
                msgN++;
            }
        }

        if (revdiff != 0) {

            /*
             * From Golden Cheetah
             * 
             * eventCount = message[5]; slope = message[7] + (message[6]<<8); //
             * yes it is bigendian period = message[9] + (message[8]<<8); // yes
             * it is bigendian torque = message[11] + (message[10]<<8); // yes
             * it is bigendian float time = (float)period / (float)2000.00;
             * float torque_freq = torque / time - 420/*srm_offset; float
             * nm_torque = 10.0 * torque_freq / antMessage.slope; float cadence
             * = 2000.0 * 60 * (antMessage.eventCount - lastMessage.eventCount)
             * / period; float power = 3.14159 * nm_torque * cadence / 30;
             */

            double time = Math.abs(period) / 2000.00;
            double torqueFreq = Math.abs(torque) / time - 420;
            double nm_torque = 10.0 * torqueFreq / this.getP();

            rpm = 2000.0 * 60 * revdiff / Math.abs(period);
            watts = parent.PI * Math.abs(nm_torque) * rpm / 30;

            if (debug)
                System.out.println("nm: " + nm_torque + " rpm: " + rpm
                        + " watts: " + watts + "\n");

            if (rpm < 300 && rpm > 0 && watts < 5000 && watts > 0) {
                if (gc.newWatts == false) {
                    this.setTotalWattCounter(0);
                    this.setTotalCadCounter(0);
                    this.setRpm(0);
                    this.setWatts(0);
                    gc.newWatts = true;
                }

                this.setRpm(this.getRpm() + Math.abs(rpm));
                this.setWatts(this.getWatts() + Math.abs(watts));
                double wattCounter = this.getTotalWattCounter();
                double cadCounter = this.getTotalCadCounter();
                this.setTotalWattCounter(wattCounter + 1);
                this.setTotalCadCounter(cadCounter + 1);

                flushPowerArray(gc, this, gcArray);
            } else {
                parent.totalSpikes++;
            }
        }

        if (this.first0x20)
            this.first0x20 = false;

    }

    public void ANTParsePower0x12(byte[] msgData, int size, GoldenCheetah gc,
            List<GoldenCheetah> gcArray) {
        int t1;
        int p1;
        int r1;

        int end = 12;
        double rdiff = 0;
        double pdiff = 0;
        double tdiff = 0;
        double nm = 0;
        double rpm = 0;
        double watts = 0;
        Byte aByte;
        int msgN = 0;

        for (int i = 6; i < end; i++) {
            if (megaDebug)
                System.out.println("0x"
                        + UnicodeFormatter.byteToHex(msgData[i]));
            if (msgN == 0) {
                if (this.first0x12) {
                    // Just store it.di
                    aByte = new Byte(msgData[i]);
                    this.setR(aByte.intValue());
                    if (megaDebug)
                        System.out.println("R: " + aByte.intValue());
                } else {
                    // We can calculate and then store
                    aByte = new Byte(msgData[i]);
                    r1 = aByte.intValue();
                    rdiff = (r1 + 256 - this.getR()) % 256;
                    this.setR(r1);
                    if (megaDebug)
                        System.out.println("rdiff is: " + rdiff);
                }
                msgN++;
            } else if (msgN == 1) {
                byte[] pRdiff = new byte[2];
                i++;
                pRdiff[1] = msgData[i];
                if (megaDebug)
                    System.out.println("0x"
                            + UnicodeFormatter.byteToHex(msgData[i]) + "\n");
                i++;
                pRdiff[0] = msgData[i];
                if (megaDebug)
                    System.out.println("0x"
                            + UnicodeFormatter.byteToHex(msgData[i]) + "\n");
                p1 = GoldenEmbedParserMain.byteArrayToInt(pRdiff, 0, 2);

                if (this.first0x12) {
                    this.setP(p1);
                    if (megaDebug)
                        System.out.println("P1: " + p1);
                } else {
                    pdiff = (65536 + p1 - this.getP()) % 65536;
                    this.setP(p1);
                    if (megaDebug)
                        System.out.println("pdiff is: " + pdiff);
                }
                msgN++;
            } else if (msgN == 2) {
                byte[] pRdiff = new byte[2];
                pRdiff[1] = msgData[i];
                if (megaDebug)
                    System.out.println("0x"
                            + UnicodeFormatter.byteToHex(msgData[i]) + "\n");
                i++;
                pRdiff[0] = msgData[i];
                if (megaDebug)
                    System.out.println("0x"
                            + UnicodeFormatter.byteToHex(msgData[i]) + "\n");

                t1 = GoldenEmbedParserMain.byteArrayToInt(pRdiff, 0, 2);

                if (this.first0x12) {
                    this.setT(t1);
                    if (megaDebug)
                        System.out.println("T: " + t1);
                } else {
                    tdiff = (t1 + 65536 - this.getT()) % 65536;
                    this.setT(t1);
                    if (megaDebug)
                        System.out.println("tdiff is: " + tdiff);
                }

                i++;
                msgN++;
            }
        }
        if (tdiff != 0 && rdiff != 0) {
            nm = (float) tdiff / ((float) rdiff * 32.0);
            rpm = rdiff * 122880.0 / pdiff;
            watts = rpm * nm * 2 * parent.PI / 60;

            if (debug)
                System.out.println("ANTParsePower0x12 nm: " + nm + " rpm: "
                        + rpm + " watts: " + watts + "\n");

            if (rpm < 10000 && watts < 10000) {
                if (gc.newWatts == false) {
                    this.setTotalWattCounter(0);
                    this.setTotalCadCounter(0);
                    this.setRpm(0);
                    this.setWatts(0);
                    gc.newWatts = true;
                }

                this.setRpm(this.getRpm() + rpm);
                this.setWatts(this.getWatts() + watts);
                double wattCounter = this.getTotalWattCounter();
                double cadCounter = this.getTotalCadCounter();
                this.setTotalWattCounter(wattCounter + 1);
                this.setTotalCadCounter(cadCounter + 1);

                flushPowerArray(gc, this, gcArray);

            } else {
                if (debug)
                    System.out.println("Spike Found: pdiff: " + pdiff
                            + " rdiff: " + rdiff + " tdiff: " + tdiff + "\n");
                parent.totalSpikes++;
            }
        }
        if (this.first0x12)
            this.first0x12 = false;

    }

    // Powertap support
    public void ANTParsePower0x11(byte[] msgData, int size, GoldenCheetah gc) {
        // For parsing ANT+ 0x11 messages
        // Counter (u8) Unknown (u8) Cadence (u8) Wheel_RPM_Counter (u16)
        // Torque_Counter (u16)
        // Cnt_diff = (Last Counter - Current Counter + 256) mod 256
        // Torque_diff = (Last Torque - Current Torque + 65536) mod 65536
        // NM = Torque_diff / 32 / Cnt_diff
        // Wheel_RPM_diff = (Last Wheel RPM - Current Wheel_RPM + 65536) mod
        // 65536
        // Power = 122880 * Cnt_diff / Wheel_RPM_diff

        int c1, c2, pr1, t1, r1;
        int i = 5;

        double cdiff = 0;
        double tdiff = 0;
        double rdiff = 0;
        double nm = 0;
        double rpm = 0;
        double watts = 0;

        byte aByte;
        byte[] byteArray = new byte[2];

        // 0x11 Message counter
        aByte = msgData[i++];
        c1 = GoldenEmbedParserMain.unsignedByteToInt(aByte);

        // Unknown counter
        aByte = msgData[i++];
        c2 = GoldenEmbedParserMain.unsignedByteToInt(aByte);

        // Pedal RPM (cadence) value
        aByte = msgData[i++];
        pr1 = GoldenEmbedParserMain.unsignedByteToInt(aByte);

        // Wheel RPM counter
        byteArray[1] = msgData[i++];
        byteArray[0] = msgData[i++];
        r1 = GoldenEmbedParserMain.byteArrayToInt(byteArray, 0, 2);

        // Torque counter
        byteArray[1] = msgData[i++];
        byteArray[0] = msgData[i++];
        t1 = GoldenEmbedParserMain.byteArrayToInt(byteArray, 0, 2);

        // System.out.println("c1: " + c1 + " c2: " + c2 + " pr1: " + pr1 +
        // " t1: " + t1 + " r1: " + r1 + "\n");

        if (this.first0x11) {
            this.first0x11 = false;
            this.setR(r1);
            this.setT(t1);
            this.setCnt(c1);
        } else if (c1 != this.getCnt()) {
            cdiff = ((256 + c1 - this.getCnt()) % 256);
            tdiff = (65536 + t1 - this.getT()) % 65536;
            rdiff = (65536 + r1 - this.getR()) % 65536;

            if (tdiff != 0 && rdiff != 0) {
                nm = (float) tdiff / 32 / (float) cdiff;
                rpm = 122880 * (float) cdiff / (float) rdiff;
                watts = rpm * nm * 2 * parent.PI / 60;

                if (debug) {
                    System.out
                            .format("ANTParsePower0x11 cad: %3d  nm: %5.2f  rpm: %5.2f  watts: %6.1f",
                                    pr1, nm, rpm, watts);
                    System.out.println();
                }

                if (rpm < 10000 && watts < 10000) {
                    if (gc.newWatts == false) {
                        this.setTotalWattCounter(0);
                        this.setTotalCadCounter(0);
                        this.setRpm(0);
                        this.setWatts(0);
                        gc.newWatts = true;
                    }
                    gc.setPrevCadSecs(gc.getSecs());
                    this.setRpm(this.getRpm() + pr1);
                    this.setWatts(this.getWatts() + watts);
                    double wattCounter = this.getTotalWattCounter();
                    double cadCounter = this.getTotalCadCounter();
                    this.setTotalWattCounter(wattCounter + 1);
                    this.setTotalCadCounter(cadCounter + 1);
                    gc.setPrevWattsecs(gc.getSecs());
                    gc.setPrevCadSecs(gc.getSecs());
                } else {
                    if (debug)
                        System.out.println("Spike Found: cdiff: " + cdiff
                                + " rdiff: " + rdiff + " tdiff: " + tdiff
                                + "\n");
                    parent.totalSpikes++;
                }
            }
        }

        this.setR(r1);
        this.setT(t1);
        this.setCnt(c1);

        return; // For Loop will advance itself.
    }

    private List<GoldenCheetah> flushPowerArray(GoldenCheetah gc, Power power,
            List<GoldenCheetah> gcArray) {
        GoldenCheetah _gc;
        // Now create GC file records for the missing messages.

        if (gcArray.size() == 0)
            return gcArray;
        long startSecs = lastWattSecs;
        long endSecs = gc.getSecs(); // The next time we are about to save.
        long diffSecs = endSecs - startSecs;
        long watts = 0;
        long cad = 0;

        if (diffSecs != 0) {
            watts = (long) GoldenEmbedParserMain.Round(
                    (this.getWatts() / this.getTotalWattCounter()) / diffSecs,
                    0);
            cad = (long) GoldenEmbedParserMain.Round(
                    (this.getRpm() / this.getTotalCadCounter()) / diffSecs, 0);
        } else {
            watts = (long) GoldenEmbedParserMain.Round(
                    this.getWatts() / this.getTotalWattCounter(), 0);
            cad = (long) GoldenEmbedParserMain.Round(
                    this.getRpm() / this.getTotalCadCounter(), 0);
        }

        for (long x = startSecs; x < endSecs; x++) {
            _gc = parent.findGCByTime(x); // Do we already have a GC record for
                                          // this
            // time ?
            if (_gc != null) {
                GoldenCheetah tmpGC = new GoldenCheetah();
                tmpGC = _gc.clone(_gc);
                tmpGC.setCad(cad);
                tmpGC.setWatts(watts);
                gcArray.set(gcArray.indexOf(_gc), tmpGC);
            }
        }
        lastWattSecs = endSecs + 1;

        return gcArray;

    }

}
