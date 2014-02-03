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

import java.util.Random;

import projection.abstr.Projection;

/**
 * Abstract class used for storing all data related to one network
 */
public abstract class Graph {
    protected Random random;      // random number generator
    public Projection projection; // implements methods for computation of projection data and stores it
    
    /* REQUIRED DATA
     * (this needs to be present before passing the graph to an algorithm) */
    public int actorCount;      // number of nodes on the side of the bipartite graph that is projected onto
    public int eventCount;      // number of nodes on the other side of the graph
    public int edgeCount;       // number of edges
    
    /* OPTIONAL DATA
     * (not used by algorithm but for writing edgelists to files) */
    public String[] actorMap;               // contains the IDs of actors read from input file. actorMap[n] = ID of actor n
    
    /* SETTINGS
     * (settings used by the FDSM algorithm. required to be set previous to using the algorithm) */
    public int samples;                       // number of samples the FDSM algorithm takes
    public int steps;                         // number of steps in each random walk (sample)
    public int threads;                       // number of threads used for computation
    public int randomSeed;                    // seed for pseudo random number generator
    public String side;                       // side if the original edgelist that contains the actors (left or right)
    public String name;                       // name of the input file
    public boolean isSimplex;                 // true if graph is used for simplex projection, false for duplex
    
    public void initSettings(int samples, int steps, int threads, int seed) {
        this.samples = samples;
        this.steps = steps;
        this.threads = threads;
        this.randomSeed = seed;
    }
    
    /**
     * Computes the co-occurrence of 2 actors by comparing their adjacency lists
     * Input arrays are REQUIRED to be sorted in ascending order
     * 
     * @param adj1 sorted integer array containing all events connected to actor1
     * @param adj2 sorted integer array containing all events connected to actor2
     * @return co-occurrence of actor1 and actor2
     */
    protected static int computeCooc(int[] adj1, int[] adj2) {
        int cooc = 0;
        int pos1 = 0;
        int pos2 = 0;
        while ( (pos1 < adj1.length) && (pos2 < adj2.length) ) {  // while neither list is empty
            if (adj1[pos1] < adj2[pos2]) pos1++;                  // if one list has a smaller element at the first
            else if (adj1[pos1] > adj2[pos2]) pos2++;             // position: advance in that list
            else {              // otherwise, both lists have an identical element
                cooc++;         // so increase cooc
                pos1++;         // and advance in both lists
                pos2++;
            }
        }
        return cooc;
    }
    
    /**
     * Called ONCE before the sampling begins.
     * Used to initialize the random number generator.
     */
    public void initBeforeSampling() {
        random = new Random(randomSeed);
    }
    
    /**
     * This method is called aech time a new sample needs to be generated.
     * It should implement the sampling process for this graph.
     */
    public abstract void createNextSample();
}
