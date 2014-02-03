package projection.impl;

import java.util.LinkedList;

import projection.abstr.DuplexProjection;

import graph.DuplexGraph;

/**
 * Projection that implements the computation of all possible weights
 * for the edges of the projection. Only use with DUPLEX graphs.
 */
public class AllDuplexProjection extends DuplexProjection {
    /* COMPUTED DATA
     * (this will be generated by the algorithm and does not need to be initialized) */
    public int[][] resultPos;    // structure for storing initial co-occurrences and computed cooc counts
    public long[][] coocPos;
    public int[][] resultNeg;    // coocPos[n] contains all co-occurences for actor n with all actors m that have
    public long[][] coocNeg;
    public int[][] resultMix;    // a higher ID (m > n) and consists of two positive edges. If cooc(n,m)=0 there is no 
    public long[][] coocMix;     // entry. coocNeg contains coocs for two negative edges and coocMix for a negative
                                  // followed by a positive edge.
                                  // Format is a sequence of quintuples: (actorID, initial co-occurrence, cooc-sum, cooc square sum, p-value count)
                                  // actorIDs can be found at positions 5i
                                  // initial co-occurrences can be found at positions 5i +1
                                  // cooc sums are stored at positions 5i +2
                                  // cooc sum squares are stored at positions 5i +3
                                  // p-value counts are stored at positions 5i+4
                                  // like the other two result structures. However, it stores results for edges
                                  // between this actor and others, even if the second actors ID is lower as
                                  // we are looking at directed -+ edges here.
    
    
    public AllDuplexProjection(DuplexGraph graph) {
        super(graph);
        this.weightType="all";
    }
    
    /**
     * Initializes the memory needed to store results of the computation
     */
    public void doOnceBeforeSampling() {
        resultPos = new int[g.actorCount][];    // make sure a list for the initial posCooc exists
        resultNeg = new int[g.actorCount][];    // make sure a list for the initial negCooc exists
        resultMix = new int[g.actorCount][];    // make sure a list for the initial mixedCooc exists
        coocPos = new long[g.actorCount][];
        coocNeg = new long[g.actorCount][];
        coocMix = new long[g.actorCount][];
    }
    
    /**
     * Computes the initial co-occurrences of all all three types for
     * all actors in the original graph
     */
    public void doPerActorBeforeSampling(int a) {
        LinkedList<Integer> tmpCoocs = new LinkedList<Integer>();   // create a list for new ++ coocs as their number is not known
        for (int b=a+1; b<g.actorCount; b++) {                  // for all actors b with ID higher than a
            int cooc = computeCooc(g.adjListPos[a], g.adjListPos[b]);   // compute their co-occurence
            if (cooc > 0) {                                     // if they do co-occur
                tmpCoocs.add(b);                                // add the ID of actor b to the list
                tmpCoocs.add(cooc);                             // and add the co-occurence
            }
        }
        /* create an int array that has 5 entries for each cooc of actor a:
         * position i:   ID of target node
         * position i+1: initial cooc for these two nodes
         * position i+2: counter for observed cooc
         * position i+3: counter for observed cooc squares
         * position i+4: p-value counts */
        coocPos[a] = new long[tmpCoocs.size()];
        resultPos[a] = new int[tmpCoocs.size() + tmpCoocs.size()/2];
        int pos = 0;
        while (!tmpCoocs.isEmpty()) {                   // copy actor IDs and coocs from list to array
            resultPos[a][pos++] = tmpCoocs.remove();
            resultPos[a][pos] = tmpCoocs.remove();
            pos += 2;                                   // leave one space for observed cooc
        }
        
        // Compute initial -- co-occurences
        for (int b=a+1; b<g.actorCount; b++) {                  // for all actors b with ID higher than a
            int cooc = computeCooc(g.adjListNeg[a], g.adjListNeg[b]);   // compute their co-occurence
            if (cooc > 0) {                                     // if they do co-occur
                tmpCoocs.add(b);                                // add the ID of actor b to the list
                tmpCoocs.add(cooc);                             // and add the co-occurence
            }
        }
        /* create an int array that has 5 entries for each cooc of actor a:
         * position i:   ID of target node
         * position i+1: initial cooc for these two nodes
         * position i+2: counter for observed cooc
         * position i+3: counter for observed cooc squares
         * position i+4: p-value counts */
        coocNeg[a] = new long[tmpCoocs.size()];
        resultNeg[a] = new int[tmpCoocs.size() + tmpCoocs.size()/2];
        pos = 0;
        while (!tmpCoocs.isEmpty()) {                   // copy actor IDs and coocs from list to array
            resultNeg[a][pos++] = tmpCoocs.remove();
            resultNeg[a][pos] = tmpCoocs.remove();
            pos += 2;                                   // leave one space for observed cooc
        }

        /* Now compute initial -+ coocs. Here it is important to go through all other actors
         * each time, not just those that have a higher ID since we only look at negative outgoing
         * and positive incoming edges. */
        for (int b=0; b<g.actorCount; b++) {                  // for all other actors b
            if (a != b) {
                int cooc = computeCooc(g.adjListNeg[a], g.adjListPos[b]);   // compute their co-occurence
                if (cooc > 0) {                                     // if they do co-occur
                    tmpCoocs.add(b);                                // add the ID of actor b to the list
                    tmpCoocs.add(cooc);                             // and add the co-occurence
                }
            }
        }
        /* create an int array that has 5 entries for each cooc of actor a:
         * position i:   ID of target node
         * position i+1: initial cooc for these two nodes
         * position i+2: counter for observed cooc
         * position i+3: counter for observed cooc squares
         * position i+4: p-value counts */
        coocMix[a] = new long[tmpCoocs.size()];
        resultMix[a] = new int[tmpCoocs.size() + tmpCoocs.size()/2];
        pos = 0;
        while (!tmpCoocs.isEmpty()) {                   // copy actor IDs and coocs from list to array
            resultMix[a][pos++] = tmpCoocs.remove();
            resultMix[a][pos] = tmpCoocs.remove();
            pos += 2;                                   // leave one space for observed cooc
        }
    }
    
    /**
     * After each new graph is sampled, this computes the co-occurrence for all pairs if nodes with
     * a co-occurrence in the initial graph and adds it to the coocs.
     */
    public void doPerActorDuringSampling(int ac1) {
        int ac2;
        int positionRes = 0;
        int positionCooc = 0;
        while (positionRes < resultPos[ac1].length) {      // go through the lists of initial coocs
            ac2 = (int)resultPos[ac1][positionRes++];      // get IDs of target actors
            // compute cooc for this pair of actors and add it to cooc count
            int cooc = computeCooc(g.adjListPos[ac1], g.adjListPos[ac2]);
            int initCooc = resultPos[ac1][positionRes++];
            if (cooc >= initCooc) resultPos[ac1][positionRes]++;
            positionRes++;
            coocPos[ac1][positionCooc++] += cooc;
            coocPos[ac1][positionCooc++] += cooc*cooc;
        }
        
        // compute cooc counts for -- coocs
        positionRes = 0;
        positionCooc = 0;
        while (positionRes < resultNeg[ac1].length) {      // go through the lists of initial coocs
            ac2 = (int)resultNeg[ac1][positionRes++];      // get IDs of target actors
            // compute cooc for this pair of actors and add it to cooc count
            int cooc = computeCooc(g.adjListNeg[ac1], g.adjListNeg[ac2]);
            long initCooc = resultNeg[ac1][positionRes++];
            if (cooc >= initCooc) resultNeg[ac1][positionRes]++;
            positionRes++;
            coocNeg[ac1][positionCooc++] += cooc;
            coocNeg[ac1][positionCooc++] += cooc*cooc;
        }

        // compute cooc counts for -+ coocs
        positionRes = 0;
        positionCooc = 0;
        while (positionRes < resultMix[ac1].length) {      // go through the lists of initial coocs
            ac2 = (int)resultMix[ac1][positionRes++];      // get IDs of target actors
            // compute cooc for this pair of actors and add it to cooc count
            int cooc = computeCooc(g.adjListNeg[ac1], g.adjListPos[ac2]);
            long initCooc = resultMix[ac1][positionRes++];
            if (cooc >= initCooc) resultMix[ac1][positionRes]++;
            positionRes++;
            coocMix[ac1][positionCooc++] += cooc;
            coocMix[ac1][positionCooc++] += cooc*cooc;
        }
        
    }
    
}
