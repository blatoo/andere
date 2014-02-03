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

import java.util.concurrent.CyclicBarrier;

/**
 * Thread implementing the parallelized cooc-computation for FDSM
 */
public class AlgoThread extends Thread {
    private Graph g;                    // graph object containing network data
    private CyclicBarrier cybar;        // cyclic barrier used to synchronize with other threads
    private Synchronizer sync;          // synchronizer used to request actor IDs for computation
    private int samples;                // number of samples to take
    private int threadID;               // ID of this thread, used for generating feedback
    private SynchronizedLock synLock;   // Lock used to release main thread after computation is done
    private ProgressUpdater progress;   // class used to output progress reports to UI
    
    /**
     * Default constructor
     * @param graph graph object containing network data
     * @param sync Synchronizer for assigning actor IDs
     * @param cybar CyclicBarrier for multithreading organization
     * @param samples number of samples to take
     * @param synLock Lock to release main thread after computation is done
     */
    public AlgoThread(Graph graph, Synchronizer sync, CyclicBarrier cybar, SynchronizedLock synLock) {
        super();
        g = graph;
        this.cybar = cybar;
        this.sync = sync;
        this.samples = g.samples;
        this.synLock = synLock;
    }
    
    /**
     * Implements parallel computation of FDSM algorithm
     */
    public void run() {
        try {
            threadID = cybar.await();                       // get the ID for this thread
            
            if (threadID == 0) {                            // only do this once (if multiple threads are running)
                progress = new ProgressUpdater(samples);    // create a new progressUpdater to update UI
                progress.initCooc();                        // and tell it that initial cooc is being computed now
            }
            // Compute initial co-occurences
            int a;                                          // actor ID
            while ((a = sync.actorID()) >= 0) {             // for all actors a
                g.projection.doPerActorBeforeSampling(a);   // perform the preSampling action
            }
            
            if (threadID == 0) {                            // only do this once (if multiple threads are running)
                progress.finishCooc();                      // update UI
                progress.initSampling();
            }
            
            // perform random walks and compute coocs
            for (int i=0; i<samples; i++) {
                /* Synchronize with cyclic barrier to ensure all threads are ready 
                 * Once all threads are waiting, the cooc computation will be performed automatically by one thread
                 * before all threads are released. This is done by the Synchronizers run-method. */
                cybar.await();
                int ac1;
                while ((ac1 = sync.actorID()) >= 0) {           // while there are still actors left
                    g.projection.doPerActorDuringSampling(ac1); // perform the inSampling action
                }
                if (threadID == 0) progress.updateSampling(i);  // update UI
            }
            
            if (threadID == 0) sync.disableSwaps();             // turn off cooc computation for the next step as we're done
            cybar.await();                                      // wait for all threads to finish
            
            if (threadID == 0) {
                progress.finishSampling();                      // update UI
                synLock.unlock();                               // then wake the main thread
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
