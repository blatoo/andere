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
 * Class used to store all data related to one duplex network
 */
public class DuplexGraph extends Graph {
    /* REQUIRED DATA
     * (this needs to be present before passing the graph to an algorithm) */
    public int[][] adjListPos;  // adjListPos[n] is a list of all events connected to actor n through a positive edge
                                // individual lists NEED to be SORTED in ASCENDING order!
    public int[][] adjListNeg;
    public int[] degreesPos;    // contains the number of positive edges of actors. degreePos[n] = positive degree of actor n
    public int[] degreesNeg;
    public int[] edgeMapPos;    // array of length edgeCountPos. Contains one entry per edge, linking
                                // it to it's actor node. The ordering does not matter.
                                // [0,0,1,1,1,2,3...] if degreePos[0] = 2, degreePos[1] = 3, degreePos[2] = 1, ...
    public int[] edgeMapNeg;
    public int[] offsetPos;     // contains number of positive edges of all previous actors:
                                // offsetPos[0] = 0, offsetPos[1] = degreePos[0], offsetPos[2] = degreePos[0] + degreePos[1], ...
    public int[] offsetNeg;
    public int edgeCountPos;    // number of positive edges
    public int edgeCountNeg;
    
    /**
     * Implements a markov chain to sample a new graph with
     * identical degree sequences.
     */
    public void createNextSample() {
        for (int i=0; i<steps; i++) {        // for the selected number of steps
            /* Decide which of the two graphs to pick edges from for swapping. Since the number
             * of edges in both graphs does not need to be identical the random value has to be weighted
             * by the number of edges in each adjacency list. */
            int r1 = random.nextInt(edgeCount);           // pick a random edge
            if (r1<edgeCountPos) {                        // if this edge is positive
                int r2 = random.nextInt(edgeCountPos);    // pick another positive edge
                int ac1 = edgeMapPos[r1];                 // find the corresponding actor IDs
                int ac2 = edgeMapPos[r2];
                /* check if the two edges can be swapped. If that is the case, do so */
                binarySwap(adjListPos[ac1], r1-offsetPos[ac1], adjListPos[ac2], r2-offsetPos[ac2],
                                           adjListNeg[ac1], adjListNeg[ac2]);
            } else {                                        // otherwise, the edge is negative
                r1 = r1 - edgeCountPos;                   // so adjust it accordingly
                int r2 = random.nextInt(edgeCountNeg);    // and pick another negative edge
                int ac1 = edgeMapNeg[r1];                 // find the corresponding actor IDs
                int ac2 = edgeMapNeg[r2];
                /* check if the two edges can be swapped. If that is the case, do so */
                binarySwap(adjListNeg[ac1], r1-offsetNeg[ac1], adjListNeg[ac2], r2-offsetNeg[ac2],
                                           adjListPos[ac1], adjListPos[ac2]);
            }
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
    private static void binarySwap(int[] actor1, int index1in1, int[] actor2, int index2in2, int[] oppActor1, int[] oppActor2) {
        int item2 = actor2[index2in2];
        int index2in1 = Arrays.binarySearch(actor1, item2);
        if (index2in1 >= 0) return;                         // if new edge already exists: do nothing
        int item1 = actor1[index1in1];
        int index1in2 = Arrays.binarySearch(actor2, item1);
        if (index1in2  >= 0) return;                        // if new edge already exists: do nothing
        
        // check if the new edge already exists in the other (positive/negative) adjacency list and if so: abort
        if (Arrays.binarySearch(oppActor1, item2) >= 0) return;
        if (Arrays.binarySearch(oppActor2, item1) >= 0) return;

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
    public DuplexGraph() {
        this.isSimplex = false;
    }
    
    /**
     * Constructor that will generate a graph object with all required parameters from
     * an adjacency list. input adjacency lists does not need to be sorted.
     * @param adjacencyList adjacency list for the input graph
     */
    public DuplexGraph(int[][] adjacencyListPositive, int[][] adjacencyListNegative) {
        this.isSimplex = true;
        
        adjListPos = adjacencyListPositive;
        adjListNeg = adjacencyListNegative;
        actorCount = adjListPos.length;                // get the number of actors
        
        // sort the adjacency lists non-descendingly
        for (int i=0; i<actorCount; i++) {
            Arrays.sort(adjListPos[i]);
            Arrays.sort(adjListNeg[i]);
        }
        
        // get and store the degrees of all actors
        degreesPos = new int[actorCount];
        for (int i=0; i<actorCount; i++) {
            degreesPos[i] = adjListPos[i].length;
        }
        degreesNeg = new int[actorCount];
        for (int i=0; i<actorCount; i++) {
            degreesNeg[i] = adjListNeg[i].length;
        }
        
        // compute offset for all actors
        offsetPos = new int[actorCount];
        offsetPos[0] = 0;
        for (int i=1; i<actorCount; i++) {
            offsetPos[i] = offsetPos[i-1] + degreesPos[i-1];
        }
        offsetNeg = new int[actorCount];
        offsetNeg[0] = 0;
        for (int i=1; i<actorCount; i++) {
            offsetNeg[i] = offsetNeg[i-1] + degreesNeg[i-1];
        }
        
        // compute the number of edges
        edgeCountPos = offsetPos[actorCount-1] + degreesPos[actorCount-1];
        edgeCountNeg = offsetNeg[actorCount-1] + degreesNeg[actorCount-1];
        edgeCount = edgeCountPos + edgeCountNeg;
        
        // generate the edge map, mapping each edge to its actor
        edgeMapPos = new int[edgeCount];
        int position = 0;
        for (int i=0; i<actorCount; i++) {
            for (int j=0; j<degreesPos[i]; j++) {
                edgeMapPos[position++] = i;
            }
        }
        edgeMapNeg = new int[edgeCount];
        position = 0;
        for (int i=0; i<actorCount; i++) {
            for (int j=0; j<degreesNeg[i]; j++) {
                edgeMapNeg[position++] = i;
            }
        }
        
        // compute number of events
        HashSet<Integer> events = new HashSet<Integer>();
        for (int[] adj : adjListPos) {
            for (int event : adj) {
                events.add(event);
            }
        }
        for (int[] adj : adjListNeg) {
            for (int event : adj) {
                events.add(event);
            }
        }
        eventCount = events.size();

    }
    
}
