package org.shampoo.goldenembed.tools;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.shampoo.goldenembed.parser.GoldenCheetah;

public class GnuPlot {

    private final static String SPACE = " ";

    public GnuPlot() {
    }

    public void writeOutGnuPlot(List<GoldenCheetah> gcArray, String outFile) {
        Iterator<GoldenCheetah> iter = gcArray.iterator();
        GoldenCheetah gc;
        PrintWriter fout = initPlotFile(outFile);

        while (iter.hasNext()) {
            gc = iter.next();

            if (gc.getSecs() != 0) {

                fout.write(convertToTime(gc.getSecs()) + SPACE + gc.getWatts()
                        + SPACE + gc.getHr() + SPACE + gc.getCad() + SPACE
                        + gc.getSpeed() + SPACE + gc.getDistance() + SPACE
                        + gc.getElevation() + "\n");
            }
        }
        fout.close();
    }

    private PrintWriter initPlotFile(String outFile) {
        PrintWriter fout;
        try {
            fout = new PrintWriter(new FileOutputStream(outFile));
            return fout;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private String convertToTime(long secsIn) {
        long hours = secsIn / 3600, remainder = secsIn % 3600, minutes = remainder / 60, seconds = remainder % 60;

        return ((hours < 10 ? "0" : "") + hours + ":"
                + (minutes < 10 ? "0" : "") + minutes + ":"
                + (seconds < 10 ? "0" : "") + seconds);

    }

}
