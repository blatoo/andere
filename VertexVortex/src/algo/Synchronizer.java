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

import graph.Graph;

/**
 * Synchronizer that serves two purposes:
 * 1) Implements synchronized methods for assigning actor IDs to workthreads for cooc computation
 * 2) Implements runnable interface for performing edge swaps. This can be called from the cyclic barrier  
 */
public class Synchronizer implements Runnable {
    private final Graph g;          // graph object 
    private final int actors;       // number of actor nodes
    private int actorCounter;       // counter for actors
    private boolean ready;          // flag to disable edge swapping. Set to false to disable swapping (for the next step only!)

    /**
     * @param graph graph object containing network data
     * @param seed random seed
     * @param steps number of steps to perform in each random walk
     */
    public Synchronizer (Graph graph) {
        this.g = graph;
        this.actors = g.actorCount;
        this.actorCounter = -1;
        ready = false;
    }
    
    /**
     * Get ID of next actor to compute the algorithm step for. Synchronized for threading.
     * @return ID of next actor
     */
    public synchronized int actorID() {
        if (++actorCounter >= actors) {  // if no ID is left return -1 to signal this
            return -1;
        }
        else return actorCounter;        // otherwise return the next free ID
    }
    
    /**
     * Disable swaps for the next step (next time the cyclic barrier is breached)
     */
    public synchronized void disableSwaps() {
        ready = false;
    }
    
    /**
     * Perform the set number of random walk steps.
     * This method is called every time the cyclic barrier is breached.
     */
    public void run() {
        actorCounter = -1;
        if (ready) {			          // if a random walk should be performed in this step
            g.createNextSample();         // perform random walk to get a new graph
        } else ready = true;              // otherwise enable random walks for the next step
    }
    
}
