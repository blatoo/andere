/*  Copyright (c) 2012  Andreas Spitz, spitz@stud.uni-heidelberg.de
 *
 *  This file is part of VertexVortex
 *
 *  VertexVortex is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  VertexVortex is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package algo;

/**
 * Class used for updating the UI
 * Built for console width of 60 characters or more
 */
public class ProgressUpdater {
    private String white = "                                                            ";
    private String full =  "====================";  
    private String empty = "--------------------";
    private int percentDone;        // percentage that is done. Range: 0 to 100
    private int samples;            // number of samples the algorithm takes
    private long startTime;         // starting time of the timer
    private int mHour = 3600000;    // hour in milliseconds
    private int mMin = 60000;
    private int mSec = 1000;
    
    /**
     * Default constructor
     * @param samples number of samples the algorithm takes
     */
    public ProgressUpdater(int samples) {
        this.samples = samples;
        percentDone = 0;
    }

    /**
     * Display to UI that initial cooc is being computed
     */
    public void initCooc() {
        startTime = System.currentTimeMillis();
        System.out.print("Computing initial cooc                                      ");
    }
    
    /**
     * Display to UI that initial cooc computation is done
     */
    public void finishCooc() {
        System.out.println("\rInitial cooc complete                                       ");
    }
    
    /**
     * Update UI display to show that sampling is starting
     */
    public void initSampling() {
        String out = "Sampling [" + empty + "] 0% done                     ";
        System.out.print(out);
    }
    
    /**
     * Display percentage of sampling that is complete
     * @param samplesCompleted number of samples completed
     */
    public void updateSampling(int samplesCompleted) {
        int percentage = (100*samplesCompleted)/samples;    // compute the percentage of samples that is done
        if (percentage > percentDone) {                     // if it has increased since last time
            percentDone = percentage;                       // store the new value
            int bars = percentage/5;                        // compute the number of bars in progress bar
            String out = "Sampling [";                      // and build progress bar output string
            out += full.substring(20-bars);
            out += empty.substring(bars);
            out += "] " + percentage + "% done, ETR ";
            int time = (int)(System.currentTimeMillis() - startTime);   // get time elapsed so far
            time = (int)(time*(samples / (samplesCompleted+1.0) - 1.0));// and compute estimated time remaining
            if (time > mHour) out += (time/mHour) + " hours";           // if it's in hours
            else if (time > mMin) out += (time/mMin) + " min";          // if it's in minutes
            else out += (time/mSec) + " sec";                           // if just seconds are left
            if (out.length() < 60) out += white.substring(out.length());// add whitespaces to padd the output to 60 chars width
            System.out.print("\r" + out);
            System.out.flush();
        }
    }
    
    /**
     * Update UI to show that sampling is complete
     */
    public void finishSampling() {
        String out = "Sampling complete. Time required ";
        int time = (int)(System.currentTimeMillis() - startTime);   // get total time elapsed
        if (time >= mHour) {                                        // print hours
            out += (time/mHour) + " hours ";
            time = time%mHour;
        }
        if (time > mMin) {                                          // minutes
            out += (time/mMin) + " min ";
            time = time%mMin;
        }
        out += (time/mSec) + " sec";                                // and seconds
        if (out.length() < 60) out += white.substring(out.length());// pad output to 60 chars width
        System.out.println("\r" + out);
    }
}
