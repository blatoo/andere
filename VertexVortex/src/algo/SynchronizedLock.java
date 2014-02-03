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

/**
 * Synchronized lock that can be used to lock one thread until another releases the lock
 */
public class SynchronizedLock {
    private boolean locked;         // status of the lock. True will stop any thread waiting on this, false will release
    
    /**
     * Create a new lock and initialize it to locked
     */
    public SynchronizedLock() {
        locked = true;
    }
    
    /**
     * Wait on this lock until it is released
     */
    public synchronized void await() {
        while (locked) {
            try {
                this.wait();
            } catch (Exception e) { }
        }
    }
    
    /**
     * Unlock this lock. Causes any thread currently waiting to be released.
     */
    public synchronized void unlock() {
        locked = false;
        this.notifyAll();
    }
}
