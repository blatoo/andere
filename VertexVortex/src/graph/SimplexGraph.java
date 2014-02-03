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

package graph;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Class used to store all data related to one simplex network
 */
public class SimplexGraph extends Graph {
    /* REQUIRED DATA
     * (this needs to be present before passing the graph to the algorithm) */
    public int[][] adjList;     // adjList[n] is a list of all events connected to actor n
                                // individual lists NEED to be SORTED in ASCENDING order!
    public int[] degrees;       // contains degrees of actors. degree[n] = degree of actor n
    public int[] edgeMap;       // array of length edgeCount. Contains one entry per edge, linking
                                // it to it's actor node. The ordering does not matter.
                                // [0,0,1,1,1,2,3...] if degree[0] = 2, degree[1] = 3, degree[2] = 1, ...
    public int[] offset;        // contains number of edges of all previous actors:
                                // offset[0] = 0, offset[1] = degree[0], offset[2] = degree[0] + degree[1], ...
    
    /**
     * Implements a markov chain to sample a new graph with
     * identical degree sequences.
     */
    public void createNextSample() {
        for (int i=0; i<steps; i++) {               // for the selected number of steps
            int r1 = random.nextInt(edgeCount);     // get two random edges
            int r2 = random.nextInt(edgeCount);
            int ac1 = edgeMap[r1];                  // find the corresponding actor IDs
            int ac2 = edgeMap[r2];
            // swap edges if no multi-edges are created this way
            binarySwap(adjList[ac1], r1-offset[ac1], adjList[ac2], r2-offset[ac2]);
        }
    }
    
    /**
     * Checks for the existence of edges and swaps target nodes if new edges don't exist
     * 
     * Input arrays must be SORTED in ascending order. This order will not be
     * changed by a call to this function.
     * 
     * @param actor1 Array containing all items connected to a user
     * @param index1in1 index of an item in the array given by user1
     * @param actor2 Array containing all items connected to a second user
     * @param index2in2 index of an item in the array given by user2
     */
    private static void binarySwap(int[] actor1, int index1in1, int[] actor2, int index2in2) {
        int item2 = actor2[index2in2];
        int index2in1 = Arrays.binarySearch(actor1, item2);
        if (index2in1 >= 0) return;                         // if new edge already exists: do nothing
        int item1 = actor1[index1in1];
        int index1in2 = Arrays.binarySearch(actor2, item1);
        if (index1in2  >= 0) return;                        // if new edge already exists: do nothing

        /* If we get to this point, both binary searches returned negative
         * index2in1 contains the InsertionPoint for item2 in user1
         * index1in2 contains the InsertionPoint for item1 in user2
         */
        
        index2in1 = -(++index2in1); // convert insertionPoint returned by binSearch to actual index
        index1in2 = -(++index1in2);
        
        // insert item1 in the correct spot in user2  and keep the array sorted
        if (index1in2 <= index2in2) {                   // if insertion point is to the left of the removed item
            for (int i=index2in2; i>index1in2; i--) {   // shuffle elements between both indices to the right
                actor2[i] = actor2[i-1];
            }
        } else {                                        // else if insertion point is to the right of removed item
            index1in2--;                                // adjust insertion point
            for (int i = index2in2; i<index1in2; i++) { // shuffle elements between both indices to the left
                actor2[i] = actor2[i+1];
            }
        }
        actor2[index1in2] = item1;                       // insert new item

        // insert item2 in the correct spot in user1 and keep the array sorted
        if (index2in1 <= index1in1) {
            for (int i=index1in1; i>index2in1; i--) {
                actor1[i] = actor1[i-1];
            }
        } else {
            index2in1--;
            for (int i = index1in1; i<index2in1; i++) {
                actor1[i] = actor1[i+1];
            }
        }
        actor1[index2in1] = item2;
    }
    
    /**
     * Default constructor
     */
    public SimplexGraph() {
        this.isSimplex = true;
    }
    
    /**
     * Constructor that will generate a graph object with all required parameters from
     * an adjacency list. input adjacency list does not need to be sorted.
     * @param adjacencyList adjacency list for the input graph
     */
    public SimplexGraph(int[][] adjacencyList) {
        this.isSimplex = true;
        
        adjList = adjacencyList;
        actorCount = adjList.length;                // get the number of actors
        
        // sort the adjacency lists non-descendingly
        for (int i=0; i<actorCount; i++) {
            Arrays.sort(adjList[i]);
        }
        
        // get and store the degrees of all actors
        degrees = new int[actorCount];
        for (int i=0; i<actorCount; i++) {
            degrees[i] = adjList[i].length;
        }
        
        // compute offset for all actors
        offset = new int[actorCount];
        offset[0] = 0;
        for (int i=1; i<actorCount; i++) {
            offset[i] = offset[i-1] + degrees[i-1];
        }
        
        // compute the number of edges
        edgeCount = offset[actorCount-1] + degrees[actorCount-1];
        
        // generate the edge map, mapping each edge to its actor
        edgeMap = new int[edgeCount];
        int position = 0;
        for (int i=0; i<actorCount; i++) {
            for (int j=0; j<degrees[i]; j++) {
                edgeMap[position++] = i;
            }
        }
        
        // compute number of events
        HashSet<Integer> events = new HashSet<Integer>();
        for (int[] adj : adjList) {
            for (int event : adj) {
                events.add(event);
            }
        }
        eventCount = events.size();

    }
   
}
