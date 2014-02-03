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

package tool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class used for handling program settings
 */
public class Settings {
    private static final String iniFileName = "settings.ini";   // name of the file containing program settings
    private static final int default_randomSeed = 0;            // seed used for random number generator (0 takes system time instead)
    private static final int default_samples = 10000;           // number of samples the FDSM algorithm takes
    private static final int default_steps = 0;                 // number of steps in each random walk (0 means m log m will be used)
    private static final int default_threads = 4;               // number of threads
    private static final boolean default_projectionSide = true; // project onto nodes on left or right side of edges in edge list? true for left side
    private static final boolean default_projectionType = true; // projection type (simplex = true, duplex = false)
    private static final boolean default_saveSettings = false;  // save program settings to ini file?
    private static final boolean default_finalize = false;      // finalize computation  by dividing p-value count / samples?
    private static final int default_precision = 8;             // number of decimal places in output numbers
    private static final String default_weightType = "pvalue";  // type of weight to be used for the projection
    
    private String rootPath;            // path of the jar file
    private String inFilePath;          // absolute path to input file
    private String inFileName;          // name of input file
    private int samples;                // number of samples the FDSM algorithm takes
    private int steps;                  // number of steps in each random walk
    private int threads;                // number of threads
    private boolean projectionSide;     // project onto nodes on left or right side of edges in edge list? true for left side
    private int randomSeed;             // seed used for random number generator
    private boolean projectionType;     // type of projection (simplex = true, duplex = false)
    private boolean saveSettings;       // save program settings to ini file?
    private boolean finalize;           // finalize computation  by dividing p-value count / samples?
    private int precision;              // number of decimal places in output numbers
    private String weightType;          // which weight to compute for edges in the resulting projection
    
    private static enum qualifiers { // enum used for switch when reading from ini / parameters
        in,                 // input file
        samples,            // number of samples
        steps,              // number of steps
        threads,            // number of threads
        projection,         // side to project onto
        seed,               // random seed
        type,               // type: simplex or duplex
        savesettings,       // save settings to ini?
        finalize,           // compute final p-values?
        precision,          // number of decimal places in output numbers
        weight;             // which weight is computed for edges
    }
    
    /**
     * Default Constructor.
     * 
     * Will attempt to locate the directory the jar file is started from.
     * Reads program settings from ini file if it is present.
     */
    public Settings() {
        this.reset();           // load default settings for all variables
        this.rootPath = "";
        this.inFileName = null;
        
        try {                   // try to obtain the path of the directory this program was started from
            URI uri = Settings.class.getProtectionDomain().getCodeSource().getLocation().toURI(); // get URI of jar file
            String jarPath = new File(uri).getAbsolutePath();                                     // extract path
            rootPath = jarPath.substring(0,jarPath.lastIndexOf(File.separator)+1);                // and convert to string
        } catch (Exception e) {
            // if the path cannot be obtained the current JVM working directory will be used
            System.out.println("Unable to obtain path of jar file. Using default working directory.");
        }
        
        try {                   // try to load settings from ini-file
            this.loadIniFile();
        } catch (Exception e) {
            // if loading does not work, default settings will be used for all variables
            System.out.println("Unable to load settings from ini file.\n" +
            		           "Using default settings where no parameters are specified.");
        }
    }
    
    /**
     * Reset all program settings to default values
     */
    public void reset() {
        samples = default_samples;
        steps = default_steps;
        threads = default_threads;
        projectionSide = default_projectionSide;
        randomSeed = default_randomSeed;
        projectionType = default_projectionType;
        saveSettings = default_saveSettings;
        finalize = default_finalize;
        precision = default_precision;
        weightType = default_weightType;
    }
    
    /**
     * Print a list of all command line parameters to console
     */
    public void printParameters() {
        System.out.println("The following parameters can be used. The name of an input file is required.");
        System.out.println("Provide parameters in this format: param=value");
        System.out.println("in            name of input file");
        System.out.println("projection    side of graph to project onto [left, right]");
        System.out.println("samples       number of samples the FDSM algorithm takes");
        System.out.println("steps         number of steps per sample (0 = m log m)");
        System.out.println("seed          random seed for FDSM algorithm (0 = system time)");
        System.out.println("threads       number of threads the FDSM algorithm will use");
        System.out.println("type          type of projection [simplex, duplex]");
        System.out.println("savesettings  save current settings to ini file? [true, false]");
        System.out.println("finalize      output final weights? [true, false]");
        System.out.println("precision     precision of numeric outputs (min 1, default 8)");
        System.out.println("weight        compute what weight? [lev, pvalue, PNAS, all]");
        System.out.println();
        System.out.println("Special parameter: merge=directory can be used to merge all non-finalized");
        System.out.println("data files previously created by VertexVortex contained in the specified");
        System.out.println("directory into one finalized file containing similarity measures. The directory");
        System.out.println("must not contain any other files. Additional parameters will be ignored.");
    }
    
    /**
     * Read in a command line argument or ini setting and overwrite the according program setting
     * @param parameter command line parameter string
     * @return true if parameter was set, false if error occurred
     */
    private boolean setParameter(String parameter) {
        try {
            int position = parameter.indexOf("=");                   // find the separator character
            String qualifier = parameter.substring(0, position);     // extract qualifier of the setting to be read
            String value = parameter.substring(position+1);          // extract value of the setting to be read
            switch (qualifiers.valueOf(qualifier)) {                 // find correct place to store the value
                case in:
                    setInFileName(value);
                    break;
                case samples:
                    setSamples(Integer.parseInt(value));
                    break;
                case steps:
                    setSteps (Integer.parseInt(value));
                    break;
                case threads:
                    setThreadCount(Integer.parseInt(value));
                    break;
                case projection:
                    if (value.equals("right")) setProjectionSide(false);
                    else if (value.equals("left")) setProjectionSide(true);
                    else throw new Exception();
                    break;
                case seed:
                    setSeed(Integer.parseInt(value));
                    break;
                case type:
                    if (value.equals("simplex")) setProjectionType(true);
                    else if (value.equals("duplex")) setProjectionType(false);
                    else throw new Exception();
                    break;
                case savesettings:
                    setSaveSettings(Boolean.parseBoolean(value));
                    break;
                case finalize:
                    setFinalize(Boolean.parseBoolean(value));
                    break;
                case weight:
                    setWeightType(value);
                    break;
                case precision:
                    setPrecision(Integer.parseInt(value));
                    break;
                default:
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    
    /**
     * Load settings from ini file
     * 
     * @throws Exception File not found or IO-Exception
     */
    public void loadIniFile() throws Exception {
        File file = new File(rootPath + iniFileName);                           // create new file handle and open ini-file
        BufferedReader buffer = new BufferedReader(new FileReader(file));       // create fileBuffer
        String line;                                                            // temporary variable for reading lines in file
        while ( (line = buffer.readLine()) != null) {           // as long as end of file is not reached, read one line
            if (!setParameter(line)) {                          // try to set parameters
                System.out.println("Invalid parameter in ini-file: " + line + ". Using default");
            }
        }
    }
    
    /**
     * Read settings from command line arguments
     * @param parameters string array containing command line arguments
     */
    public void readParameters(String[] parameters) {
        for (int i=0; i<parameters.length; i++) {                       // for all parameters in program input
            if (!setParameter(parameters[i])) {                         // try to overwrite current setting
                System.out.println("Invalid parameter: " + parameters[i]);
            }
        }
    }
    
    /**
     * Save all currently used settings to ini file. Overwrite previous settings
     * @throws Exception File not found or IO-Exception
     */
    public void saveIniFile() throws Exception {
        File file = new File(rootPath + iniFileName);                           // open ini-file
        BufferedWriter buffer = new BufferedWriter(new FileWriter(file,false)); // create fileBuffer and set to overwrite
        buffer.write("samples="+ getSamples() + "\n");
        buffer.write("steps="+ getSteps() + "\n");
        buffer.write("threads="+ getThreadCount() + "\n");
        buffer.write("projection="+ ((getProjectionSide()) ? "left" : "right") + "\n");
        buffer.write("seed="+ getSeed() + "\n");
        buffer.write("type="+ ((getProjectionType()) ? "simplex" : "duplex") + "\n");
        buffer.write("finalize="+ getFinalize() + "\n");
        buffer.write("precision="+ getPrecision() + "\n");
        buffer.write("weight="+ getWeightType() + "\n");
        buffer.close();
    }
    
    // GET- and SET-Functions
    public int getSeed() { return randomSeed; }
    public void setSeed(int seed) { this.randomSeed = seed; }
    
    public int getSamples() { return samples; }
    public void setSamples(int samples) { this.samples = samples; }
    
    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }
    
    public int getThreadCount() { return threads; }
    public void setThreadCount(int threads) { this.threads = threads; }
    
    public boolean getProjectionSide() { return projectionSide; }
    public void setProjectionSide(boolean projectionSide) { this.projectionSide = projectionSide; }
    
    public boolean getFinalize() { return finalize; }
    public void setFinalize(boolean finalize) { this.finalize = finalize; }
    
    public int getPrecision() { return precision; }
    public void setPrecision(int precision) { this.precision = precision; }
    
    public String getWeightType() { return weightType; }
    public void setWeightType(String weightType) { this.weightType = weightType; }
    
    public boolean getProjectionType() { return projectionType; }
    public void setProjectionType(boolean projectionType) { this.projectionType = projectionType; }
    
    public boolean getSaveSettings() { return saveSettings; }
    public void setSaveSettings(boolean saveSettings) { this.saveSettings = saveSettings; }
    
    public String getInFileName() { return inFileName; }
    public void setInFileName(String inFileName) { this.inFileName = inFileName; }
    
    public String getInFilePath() { return inFilePath; }
    public void setInFilePath(String inFilePath) { this.inFilePath = inFilePath; }
    
    public String getPathToRoot() { return rootPath; }
    
    /**
     * Generate a name used for the output file
     * @return
     */
    public String getOutFilePath() {
        String filename = getInFileName();                              // get name of input file
        int pos = filename.lastIndexOf(".");                            // get index of .
        if (pos > 0) filename = filename.substring(0, pos);             // if . exists, remove extension
        if (filename.length() == 0) filename = "out";                   // if no name is left after this, set default name
        filename += "_samples" + getSamples();                          // then append number of samples
        filename += "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".txt";  // and current date and time           
        return (getPathToRoot() + filename);
    }
    
}
