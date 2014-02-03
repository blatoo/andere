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

import graph.DuplexGraph;
import graph.Graph;
import graph.SimplexGraph;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;


/**
 * Reads network data from file
 * Assumes input contains a bipartite graph 
 */
public class GraphReader {
    
    /**
     * Read a graph from a file containing a global edgelist. The type of graph object
     * created (simplex, duplex, projection left or right) depends on parameters stored
     * in settings.
     * @param file input file handle
     * @param set object containing program settings
     * @return graph containing data from file
     * @throws Exception
     */
    public static Graph readEdgelist(File file, Settings set) throws Exception {
        Graph g;
        if (set.getProjectionType()) {                              // if a graph for a simplex projection is needed
            g = readSimplexEdgeList(file, set.getProjectionSide()); // call that function
        } else {                                                    // otherwise
            g = readDuplexEdgeList(file, set.getProjectionSide());  // call function for reading duplex graphs
        }
        if (set.getSteps() <= 0) {                                  // if the user wants steps in FDSM algorithm to depend on the data set
            g.steps = (int)(g.edgeCount * Math.log(g.edgeCount));   // compute m * log(m) where m is the number of edges
        } else {                                                    // otherwise
            g.steps = set.getSteps();                               // just use the number of steps requested
        }
        if (set.getSeed() == 0) {                                                   // if the user wants the seed to be random
            g.randomSeed = (int)(System.currentTimeMillis() % Integer.MAX_VALUE);   // cast current system time to int
        } else {                                                                    // otherwise
            g.randomSeed = set.getSeed();                                           // use the provided seed
        }
        g.samples = set.getSamples();                               // store number of samples in graph object
        g.threads = set.getThreadCount();                           // and the number of threads
        g.side = (set.getProjectionSide()) ? "left" : "right";      // set string for output to left or right
        g.name = set.getInFileName();                               // and save name of input file for output
        return g;
    }

	/**
	 * Read network data from file for a simplex projection.
	 * 
	 * @param file handle of file containing a bipartite graph as global adjacency list
	 * @param left true: project on nodes on left side of edges. false: project on right side
	 * @return graph object containing all network data
	 */
    private static SimplexGraph readSimplexEdgeList(File file, boolean left) throws Exception {
        int actorSet = (left) ? 0 : 1;                                      // set index of actor set
        int eventSet = (left) ? 1 : 0;                                      // set index of event set
        SimplexGraph g = new SimplexGraph();                                // create new graph object to hold data
        BufferedReader input = new BufferedReader(new FileReader(file));
        String line;
        HashMap<String,Integer> actorMap = new HashMap<String,Integer>();   // create map for actor nodes (set that will be projected onto)
        HashMap<String,Integer> eventMap = new HashMap<String,Integer>();   // create map for event nodes
        int actorCount = 0;                                                 // counter used to assign actor IDs
        int eventCount = 0;                                                 // counter used to assign event IDs
        
        // read the file once to get the set of actor and event nodes
        while ( (line = input.readLine()) != null ) {               // while end of file is not reached
            String[] splitline = line.split(" ");                   // split current line into strings containing node names
            if (!actorMap.containsKey(splitline[actorSet])) {
                actorMap.put(splitline[actorSet], actorCount++);    // put the actor in the map and assign an actorID
            }
            if (!eventMap.containsKey(splitline[eventSet])) {
                eventMap.put(splitline[eventSet], eventCount++);         // put the event in the map and assign an eventID
            }
        }
        
        g.actorCount = actorMap.size();             // store number of actors
        g.eventCount = eventMap.size();             // store number of events
        
        // read the file again and extract edges, put them in a temporary adjacency list
        input = new BufferedReader(new FileReader(file));
        ArrayList<LinkedList<Integer>> tmpAdj = new ArrayList<LinkedList<Integer>>(g.actorCount);   // create temporary adjacency list
        for (int i=0; i<g.actorCount; i++) tmpAdj.add(new LinkedList<Integer>());                   // create new lists for each actor node
        while ( (line = input.readLine()) != null ) {       // while end of file is not reached
            String[] splitline = line.split(" ");           // split line into strings containing actor nodes
            // get IDs of the nodes from maps and store them in the temporary adjacency list
            tmpAdj.get(actorMap.get(splitline[actorSet])).add(eventMap.get(splitline[eventSet]));
        }
        
        // store degrees of all actor nodes and build array based adjacency lists from list based adjacency lists
        g.degrees = new int[g.actorCount];              // allocate memory for degrees
        g.adjList = new int[g.actorCount][];            // allocate memory for adjacency lists
        for (int i=0; i<g.actorCount; i++) {            // for all actors
            g.degrees[i] = tmpAdj.get(i).size();        // store the degree
            g.adjList[i] = new int[g.degrees[i]];       // create an adjacency list of proper size
            LinkedList<Integer> tmp = tmpAdj.get(i);    // get the temporary adjacency list
            for (int j=0; j<g.degrees[i]; j++) {        // and copy everything to the new array based list
                g.adjList[i][j] = tmp.remove();
            }
        }
        
        // store the names of all actors in an array
        g.actorMap = new String[g.actorCount];                  // create string array with one entry per actor node to map them to IDs
        for (Entry<String, Integer> e : actorMap.entrySet()) {  // for all actor nodes
            g.actorMap[e.getValue()] = e.getKey();              // store its name at the position of its ID
        }
        
        // clean up memory
        tmpAdj = null;
        eventMap = null;
        actorMap = null;
        System.gc();
        
        // sort adjacency lists non-decreasingly
        for (int i=0; i<g.actorCount; i++) {
            Arrays.sort(g.adjList[i]);
        }
        
        // compute offset for all actors (= number of edges of all previous nodes)
        g.offset = new int[g.actorCount];
        g.offset[0] = 0;
        for (int i=1; i<g.actorCount; i++) {
            g.offset[i] = g.offset[i-1] + g.degrees[i-1];
        }
        g.edgeCount = g.offset[g.actorCount-1] + g.degrees[g.actorCount-1];
        
        // create a lookup table for random variables, mapping each edge to it's actor node
        g.edgeMap = new int[g.edgeCount];
        int position = 0;
        for (int i=0; i<g.actorCount; i++) {
            for (int j=0; j<g.degrees[i]; j++) {
                g.edgeMap[position++] = i;
            }
        }
        
        return g;
    }
    
    /**
     * Read network data from file for a duplex projection.
     * 
     * @param file handle of file containing a bipartite graph as global adjacency list
     * @param left true: project on nodes on left side of edges. false: project on right side
     * @return graph object containing all network data
     */
    private static DuplexGraph readDuplexEdgeList(File file, boolean left) throws Exception {
        int actorSet = (left) ? 0 : 1;                                      // set index of actor set
        int eventSet = (left) ? 1 : 0;                                      // set index of event set
        DuplexGraph g = new DuplexGraph();                                  // create new graph object to hold data
        BufferedReader input = new BufferedReader(new FileReader(file));
        String line;
        HashMap<String,Integer> actorMap = new HashMap<String,Integer>();   // create map for actor nodes (set that will be projected onto)
        HashMap<String,Integer> eventMap = new HashMap<String,Integer>();   // create map for event nodes
        ArrayList<Integer> positiveDegrees = new ArrayList<Integer>();      // list for storing positive degrees of actor nodes
        ArrayList<Integer> negativeDegrees = new ArrayList<Integer>();      // list for storing negative degrees of actor nodes
        int ACcount = 0;                                                    // counter used to assign IDs to actors
        int EVcount = 0;                                                    // counter used to assign IDs to events
        
        // read the file once to get the set of actor and event nodes
        while ( (line = input.readLine()) != null ) {                   // while end of file is not reached
            String[] splitline = line.split(" ");                       // split current line into strings containing node names
            boolean isNegative = Double.parseDouble(splitline[2]) < 0;  // find out of the weight if this edge is negative
            if (!actorMap.containsKey(splitline[actorSet])) {           // if actor node is not yet mapped
                actorMap.put(splitline[actorSet], ACcount++);           // map it to the next free ID
                if (isNegative) {                                       // if the edge has negative weight
                    negativeDegrees.add(1);                             // add a negative degree of 1 for this node
                    positiveDegrees.add(0);                             // and a positive degree of 0
                } else {                                                // otherwise, if it's positive,
                    negativeDegrees.add(0);                             // add a negative degree of 0
                    positiveDegrees.add(1);                             // and a positive degree of 1
                }
            } else {                                                    // if this actor is known already
                int id = actorMap.get(splitline[actorSet]);             // get its ID 
                if (isNegative) negativeDegrees.set(id, negativeDegrees.get(id)+1); // if edge is negative: increase negative degree of node
                else positiveDegrees.set(id, positiveDegrees.get(id)+1);            // otherwise increase the positive degree
            }
            if (!eventMap.containsKey(splitline[eventSet])) {         // if the event is not yet known
                eventMap.put(splitline[eventSet], EVcount++);         // map it and assign an eventID
            }
        }
        
        g.actorCount = actorMap.size();             // store number of actors
        g.eventCount = eventMap.size();             // store number of events
        
        // store strings containing actors labels in an array sorted by their ID
        g.actorMap = new String[g.actorCount];                  // create string array with one entry per actor node to map them to IDs
        for (Entry<String, Integer> e : actorMap.entrySet()) {  // for all actor nodes
            g.actorMap[e.getValue()] = e.getKey();              // store their label
        }
        
        // convert lists of positive and negative degrees to arrays
        g.degreesPos = new int[g.actorCount];
        ACcount = 0;
        for (Integer deg : positiveDegrees) {
            g.degreesPos[ACcount++] = deg; 
        }
        g.degreesNeg = new int[g.actorCount];
        ACcount = 0;
        for (Integer deg : negativeDegrees) {
            g.degreesNeg[ACcount++] = deg; 
        }
        
        positiveDegrees = null;
        negativeDegrees = null;
        System.gc();
        
        // allocate memory for adjacency lists according to degrees of actors
        g.adjListPos = new int[g.actorCount][];
        for (int i=0; i<g.actorCount; i++) {
            g.adjListPos[i] = new int[g.degreesPos[i]];
        }
        g.adjListNeg = new int[g.actorCount][];
        for (int i=0; i<g.actorCount; i++) {
            g.adjListNeg[i] = new int[g.degreesNeg[i]];
        }
        
        // read the file again and extract edges, put them in the adjacency list
        input = new BufferedReader(new FileReader(file));
        int[] filledPos = new int[g.actorCount];            // temp array to store the position to put the next element for each positive adjList
        int[] filledNeg = new int[g.actorCount];
        while ( (line = input.readLine()) != null ) {                   // while end of file is not reached
            String[] splitline = line.split(" ");                       // split line into strings containing actor nodes
            boolean isNegative = Double.parseDouble(splitline[2]) < 0;  // find out of edge is negative
            if (isNegative) {                                           // and if it is
                /* add this edge to the negative adjacency lists */
                g.adjListNeg[actorMap.get(splitline[actorSet])][filledNeg[actorMap.get(splitline[actorSet])]++] = eventMap.get(splitline[eventSet]);
            } else {
                /* otherwise add this edge to the positive adjacency lists */
                g.adjListPos[actorMap.get(splitline[actorSet])][filledPos[actorMap.get(splitline[actorSet])]++] = eventMap.get(splitline[eventSet]);
            }
        }
        
        // clean up memory
        filledPos = null;
        filledNeg = null;
        eventMap = null;
        actorMap = null;
        System.gc();
        
        // sort all adjacency lists non-decreasingly
        for (int i=0; i<g.actorCount; i++) {
            Arrays.sort(g.adjListPos[i]);
            Arrays.sort(g.adjListNeg[i]);
        }
        
        // compute offset in positive adjacency lists for all actors (= number of edges of all previous nodes)
        g.offsetPos = new int[g.actorCount];
        g.offsetPos[0] = 0;
        for (int i=1; i<g.actorCount; i++) {
            g.offsetPos[i] = g.offsetPos[i-1] + g.degreesPos[i-1];
        }
        g.edgeCountPos = g.offsetPos[g.actorCount-1] + g.degreesPos[g.actorCount-1];
        // and do the same for offset in negative adjacency lists
        g.offsetNeg = new int[g.actorCount];
        g.offsetNeg[0] = 0;
        for (int i=1; i<g.actorCount; i++) {
            g.offsetNeg[i] = g.offsetNeg[i-1] + g.degreesNeg[i-1];
        }
        g.edgeCountNeg = g.offsetNeg[g.actorCount-1] + g.degreesNeg[g.actorCount-1];
        g.edgeCount = g.edgeCountNeg + g.edgeCountPos;
        
        // create a lookup table for random variables, mapping each positive edge to it's actor node
        g.edgeMapPos = new int[g.edgeCountPos];
        int position = 0;
        for (int i=0; i<g.actorCount; i++) {
            for (int j=0; j<g.degreesPos[i]; j++) {
                g.edgeMapPos[position++] = i;
            }
        }
        // and do the same for negative edges
        g.edgeMapNeg = new int[g.edgeCountNeg];
        position = 0;
        for (int i=0; i<g.actorCount; i++) {
            for (int j=0; j<g.degreesNeg[i]; j++) {
                g.edgeMapNeg[position++] = i;
            }
        }
        
        return g;
    }
}
