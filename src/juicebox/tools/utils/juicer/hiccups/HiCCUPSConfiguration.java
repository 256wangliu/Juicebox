/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Broad Institute, Aiden Lab
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package juicebox.tools.utils.juicer.hiccups;

import com.google.common.primitives.Ints;
import juicebox.data.Dataset;
import juicebox.data.HiCFileTools;
import juicebox.tools.clt.CommandLineParserForJuicer;
import juicebox.tools.utils.common.ArrayTools;
import juicebox.windowui.HiCZoom;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by muhammadsaadshamim on 10/8/15.
 */
public class HiCCUPSConfiguration {
    private int windowWidth, peakWidth, clusterRadius;
    private double fdrThreshold;
    private int resolution;

    /*
     * Reasonable Commands
     *
     * fdr = 0.10 for all resolutions
     * peak width = 1 for 25kb, 2 for 10kb, 4 for 5kb
     * window = 3 for 25kb, 5 for 10kb, 7 for 5kb
     *
     * cluster radius is 20kb for 5kb and 10kb res and 50kb for 25kb res
     * fdrsumthreshold is 0.02 for all resolutions
     * oeThreshold1 = 1.5 for all res
     * oeThreshold2 = 1.75 for all res
     * oeThreshold3 = 2 for all res
     *
     * published GM12878 looplist was only generated with 5kb and 10kb resolutions
     * same with published IMR90 looplist
     * published CH12 looplist only generated with 10kb
     */
    public HiCCUPSConfiguration(int resolution, double fdrThreshold, int peakWidth, int windowWidth, int clusterRadius) {
        this.resolution = resolution;
        this.fdrThreshold = fdrThreshold;
        this.windowWidth = windowWidth;
        this.peakWidth = peakWidth;
        this.clusterRadius = clusterRadius;
    }

    public static List<HiCCUPSConfiguration> filterConfigurations(HiCCUPSConfiguration[] configurations, Dataset ds) {

        int[] resolutions = new int[configurations.length];
        for (int i = 0; i < configurations.length; i++) {
            resolutions[i] = configurations[i].resolution;
        }
        List<Integer> filteredResolutions = HiCFileTools.filterResolutions(ds.getBpZooms(), resolutions);

        // using map because duplicate resolutions will be removed while preserving order of respective configurations
        Map<Integer, HiCCUPSConfiguration> configurationMap = new HashMap<Integer, HiCCUPSConfiguration>();
        for (int i = 0; i < configurations.length; i++) {
            configurations[i].resolution = filteredResolutions.get(i);
            configurationMap.put(filteredResolutions.get(i), configurations[i]);
        }

        return new ArrayList<HiCCUPSConfiguration>(configurationMap.values());
    }

    public static List<HiCCUPSConfiguration> extractConfigurationsFromCommandLine(CommandLineParserForJuicer juicerParser,
                                                                                  List<HiCZoom> availableZooms) {
        List<String> resString = juicerParser.getMultipleResolutionOptions();
        if (resString == null) return null;
        int[] resolutions = ArrayTools.extractIntegers(resString);
        int[] filteredResolutions = Ints.toArray(HiCFileTools.filterResolutions(availableZooms, resolutions));

        for (int i = 0; i < resolutions.length; i++) {
            if (resolutions[i] != filteredResolutions[i]) {
                System.err.println("Resolution " + resolutions[i] + " not available. Using " + filteredResolutions[i]);
            }
        }

        Map<Integer, HiCCUPSConfiguration> configurationMap = new HashMap<Integer, HiCCUPSConfiguration>();
        for (int res : filteredResolutions) {
            if (res == 5000) {
                configurationMap.put(res, getDefaultConfigFor5K());
            } else if (res == 10000) {
                configurationMap.put(res, getDefaultConfigFor10K());
            } else if (res == 25000) {
                configurationMap.put(res, getDefaultConfigFor25K());
            } else {
                configurationMap.put(res, getDefaultBlankConfig(res));
            }
        }

        for (int i = 0; i < resolutions.length; i++) {
            if (resolutions[i] != filteredResolutions[i]) {
                System.err.println("Resolution " + resolutions[i] + " not available. Using " + filteredResolutions[i]);
            }
        }

        double[] fdr = HiCCUPSUtils.extractFDRValues(juicerParser.getFDROptions(), resolutions.length, 0.1f); // becomes default 10
        int[] peaks = HiCCUPSUtils.extractIntegerValues(juicerParser.getPeakOptions(), resolutions.length);
        int[] windows = HiCCUPSUtils.extractIntegerValues(juicerParser.getWindowOptions(), resolutions.length);
        int[] radii = HiCCUPSUtils.extractIntegerValues(juicerParser.getClusterRadiusOptions(), resolutions.length);

        for (int i = 0; i < filteredResolutions.length; i++) {
            if (fdr != null) configurationMap.get(filteredResolutions[i]).fdrThreshold = fdr[i];
            if (peaks != null) configurationMap.get(filteredResolutions[i]).peakWidth = peaks[i];
            if (windows != null) configurationMap.get(filteredResolutions[i]).windowWidth = windows[i];
            if (radii != null) configurationMap.get(filteredResolutions[i]).clusterRadius = radii[i];
        }

        List<HiCCUPSConfiguration> configs = new ArrayList<HiCCUPSConfiguration>(configurationMap.values());
        List<HiCCUPSConfiguration> validConfigs = new ArrayList<HiCCUPSConfiguration>();

        for (HiCCUPSConfiguration config : configs) {
            if (config.isValid()) {
                validConfigs.add(config);
            } else {
                System.err.println("Discarding invalid configuration: " + config);
            }
        }

        System.out.println("Using the following configurations for HiCCUPS");
        for (HiCCUPSConfiguration config : validConfigs) {
            System.out.println(config);
        }

        return validConfigs;
    }

    public static HiCCUPSConfiguration getDefaultConfigFor5K() {
        return new HiCCUPSConfiguration(5000, 10, 4, 7, 20000);
    }

    public static HiCCUPSConfiguration getDefaultConfigFor10K() {
        return new HiCCUPSConfiguration(10000, 10, 2, 5, 20000);
    }

    public static HiCCUPSConfiguration getDefaultConfigFor25K() {
        return new HiCCUPSConfiguration(25000, 10, 1, 3, 50000);
    }

    public static HiCCUPSConfiguration getDefaultBlankConfig(int res) {
        return new HiCCUPSConfiguration(res, 10, -1, -1, -1);
    }

    private boolean isValid() {
        if (resolution <= 0)
            return false;
        if (windowWidth <= 0)
            return false;
        if (peakWidth <= 0)
            return false;
        if (clusterRadius <= 0)
            return false;
        return windowWidth > peakWidth;
    }

    public int divisor() {
        return (windowWidth - peakWidth) * (windowWidth + peakWidth);
    }

    public int getResolution() {
        return resolution;
    }

    public int getClusterRadius() {
        return clusterRadius;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getPeakWidth() {
        return peakWidth;
    }

    public double getFDRThreshold() {
        return fdrThreshold;
    }

    public String toString() {
        return "Config res: " + resolution + " peak: " + peakWidth + " window: " + windowWidth +
                " fdr: " + getFDRPercent() + " radius: " + clusterRadius;
    }

    public String getFDRPercent() {
        DecimalFormat format = new DecimalFormat("#.##");
        return format.format(100. * (1. / fdrThreshold));
    }
}
