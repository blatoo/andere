package projection.impl;

import java.util.LinkedList;

import projection.abstr.SimplexProjection;

import graph.SimplexGraph;

/**
 * Projection that implements the computation of all possible weights
 * for the edges of the projection. Only use with SIMPLEX graphs.
 */
public class AllSimplexProjection extends SimplexProjection {
    /* COMPUTED DATA
     * (this will be generated by the algorithm and does not need to be initialized) */
    public int[][] result;      // structure for storing initial co-occurrences and computed cooc-value counts
    public long[][] coocs;      // cooc[n] contains all co-occurences for actor n with all actors m that have
                                // a higher ID (m > n). If cooc(n,m)=0 there is no entry.
                                // Format is a sequence of quintuple: (actorID, initial co-occurrence, cooc-sum, cooc square sum, p-value count)
                                // actorIDs can be found at positions 5i
                                // initial co-occurrences can be found at positions 5i +1
                                // cooc sums are stored at positions 5i +2
                                // cooc sum squares are stored at positions 5i +3
                                // p-value counts are stored at positions 5i+4
    
    public AllSimplexProjection(SimplexGraph graph) {
        super(graph);
        this.weightType="all";
    }
    
    /**
     * Initializes the memory needed to store results of the computation
     */
    public void doOnceBeforeSampling() {
        result = new int[g.actorCount][];    // make sure a list for the initial cooc exists
        coocs = new long[g.actorCount][];
    }
    
    /**
     * Computes the initial co-occurrences of all actors in the original graph
     */
    public void doPerActorBeforeSampling(int a) {
        LinkedList<Integer> tmpCoocs = new LinkedList<Integer>();   // create a list for new coocs as their number is not known
        for (int b=a+1; b<g.actorCount; b++) {                      // for all actors B with ID higher than a
            int cooc = computeCooc(g.adjList[a], g.adjList[b]);     // compute their co-occurence
            if (cooc > 0) {                                         // if they do co-occur
                tmpCoocs.add(b);                                    // add the ID of actor b to the list
                tmpCoocs.add(cooc);                                 // and add the co-occurence
            }
        }
        /* create an int array that has 5 entries for each cooc of actor a:
         * position i:   ID of target node
         * position i+1: initial cooc for these two nodes
         * position i+2: counter for observed cooc
         * position i+3: counter for observed cooc squares
         * position i+4: p-value counts */
        coocs[a] = new long[tmpCoocs.size()];
        result[a] = new int[tmpCoocs.size() + tmpCoocs.size()/2];
        int pos = 0;
        while (!tmpCoocs.isEmpty()) {                   // copy actor IDs and coocs from list to array
            result[a][pos++] = tmpCoocs.remove();
            result[a][pos] = tmpCoocs.remove();
            pos += 2;                                   // leave three spaces for observed coocm cooc squares and p-value counts
        }
    }
    
    /**
     * After each new graph is sampled, this computes the co-occurrence for all pairs if nodes with
     * a co-occurrence in the initial graph and adds it to the coocs.
     */
    public void doPerActorDuringSampling(int ac1) {
        int ac2;
        int resPos = 0;
        int coocPos = 0;
        while (resPos < result[ac1].length) {      // go through the lists of initial coocs
            ac2 = (int)result[ac1][resPos++];     // get IDs of target actors
            // compute cooc for this pair of actors and add it to cooc count
            int cooc = computeCooc(g.adjList[ac1], g.adjList[ac2]);
            long initCooc = result[ac1][resPos++];
            if (cooc >= initCooc) result[ac1][resPos]++;
            resPos++;
            coocs[ac1][coocPos++] += cooc;
            coocs[ac1][coocPos++] += cooc*cooc;
        }
    }
    
}