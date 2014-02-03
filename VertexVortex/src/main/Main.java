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

package main;

import graph.DuplexGraph;
import graph.Graph;
import graph.SimplexGraph;

import java.io.File;

import projection.impl.AllDuplexProjection;
import projection.impl.AllSimplexProjection;
import projection.impl.LeverageDuplexProjection;
import projection.impl.LeverageSimplexProjection;
import projection.impl.PNASProjection;
import projection.impl.PvalueDuplexProjection;
import projection.impl.PvalueSimplexProjection;

import tool.GraphReader;
import tool.ProjectionWriter;
import tool.Settings;
import algo.Algorithm;

public class Main {
    public static String version = "VertexVortex v1.03 PNAS";

    public static void main(String[] args) {
        
        System.out.println(version + ", Copyright (c) 2013, Andreas Spitz");
        System.out.println("This program comes with ABSOLUTELY NO WARRANTY. It is");
        System.out.println("free software, and you are welcome to redistribute it.");
        System.out.println("For details see enclosed GPL-3 license or visit");
        System.out.println("http://www.gnu.org/licenses/\n");
        
        Settings set = new Settings();          // create settings object to store program settings
        if (args.length==1) {                   // if only one parameter was specified, check if user asked for help
            if (args[0].equals("?") || args[0].equals("h") || args[0].equals("help")) { // if that's the case
                set.printParameters();                                                  // print list of all command line parameters
                System.exit(0);                                                         // and quit
            } else if (args[0].startsWith("merge=")) {      // otherwise if the user selected merge mode
                String dirname = args[0].substring(args[0].indexOf("=")+1);
                File directory;
                if ((directory = new File(set.getPathToRoot() + dirname)).isDirectory());    // if the input directory is given as relative path
                else if ((directory = new File(dirname)).isDirectory());                   // if the input directory is given as absolute path
                else {                                                                        // if input file cannot be found: abort
                    System.out.println("Unable to locate specified directoy. Terminating.");
                    System.exit(0);
                }
                try {
                    ProjectionWriter.mergeFiles(directory);
                } catch (Exception e) {
                    System.out.println("Error occurred while merging files.");
                    System.out.println("You probably had other files in the directory");
                    System.out.println("or the length if all files was not a match.");
                    e.printStackTrace();
                }
                System.exit(0);
            }
        }
        set.readParameters(args);               // overwrite program settings with command line parameters where applicable
        
        if (set.getSaveSettings()) {            // if the user specified that he wants to save the current program settings
            try {
                set.saveIniFile();              // write them to ini file
            } catch (Exception e) {
                System.out.println("Unable to save program settings. Proceeding.");
                e.printStackTrace();
            }
        }
        
        /* check if input file was specified and exists and adjust the stored path accordingly */
        File infile;
        if (set.getInFileName()==null) {                                                        // if the user did not specify an input file: abort
            System.out.println("No input file specified. Terminating.");
            System.exit(0);
        } else if ((infile = new File(set.getPathToRoot() + set.getInFileName())).exists()) {   // if the input file is given as relative path
            set.setInFilePath(infile.getPath());                                                // store as absolute path
            String filename = infile.getName();                                                 // then extract the filename
            set.setInFileName(filename);                                                        // and store it
        } else if ((infile = new File(set.getInFileName())).exists()) {                         // if the input file is given as absolute path
            set.setInFilePath(infile.getPath());                                                // store as absolute path
            String filename = infile.getName();
            set.setInFileName(filename);
        } else {                                                                                // if input file cannot be found: abort
            System.out.println("Unable to locate input file. Terminating.");
            System.exit(0);
        }

        File inputFile = new File(set.getInFilePath());                 // open input file
        Graph g = null;
        try {                                                           // and read graph data
            System.out.println("Reading data from file.");
            g = GraphReader.readEdgelist(inputFile, set);
            if (g.isSimplex) {
                if (set.getWeightType().equals("pvalue")) {
                    g.projection = new PvalueSimplexProjection((SimplexGraph)g);
                } else if (set.getWeightType().equals("lev")) {
                    g.projection = new LeverageSimplexProjection((SimplexGraph)g);
                } else if (set.getWeightType().equals("all")) {
                    g.projection = new AllSimplexProjection((SimplexGraph)g);
                } else if (set.getWeightType().equals("PNAS")){
                    g.projection = new PNASProjection((SimplexGraph)g);
                } else {
                    System.out.println("Unknown weight type " + set.getWeightType() + ", terminating.");
                    System.exit(0);
                }
            } else {
                if (set.getWeightType().equals("pvalue")) {
                    g.projection = new PvalueDuplexProjection((DuplexGraph)g);
                } else if (set.getWeightType().equals("lev")) {
                    g.projection = new LeverageDuplexProjection((DuplexGraph)g);
                } else if (set.getWeightType().equals("all")) {
                    g.projection = new AllDuplexProjection((DuplexGraph)g);
                } else if (set.getWeightType().equals("PNAS")){
                    System.out.println("PNAS weights are currently only supported for simplex projections.");
                    System.exit(0);
                } else {
                    System.out.println("Unknown weight type " + set.getWeightType() + ", terminating.");
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            System.out.println("Error occurred while reading graph.");
            e.printStackTrace();
            System.exit(0);
        }
        
        Algorithm algo;
        try {
            System.out.println("Computing " + ((g.isSimplex) ? "simplex" : "duplex") + " projection.");
            algo = new Algorithm(g);                                    // create a new algorithm of the correct type
            algo.compute();                                             // and have it compute the OMP
        } catch (Exception e) {
            System.out.println("Error occurred while computing projection");
            e.printStackTrace();
            System.exit(0);
        }
            
        try {                                                           // write results to file
            System.out.println("Writing results to file.");
            File outfile = new File(set.getOutFilePath());
            ProjectionWriter.writeResults(outfile, g, set.getFinalize(), version, set.getPrecision());
        } catch (Exception e) {
            System.out.println("Error occurred while writing results to file.");
            e.printStackTrace();
            System.exit(0);
        }
   }

}
