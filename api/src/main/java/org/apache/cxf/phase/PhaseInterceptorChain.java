/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.phase;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.MessageObserver;

/**
 * A PhaseInterceptorChain orders Interceptors according to the phase they
 * participate in and also according to the before & after properties on an
 * Interceptor.
 * <p>
 * A List of phases is supplied to the PhaseInterceptorChain in the constructor.
 * This class is typically instantiated from the PhaseChainCache class in this
 * package. Interceptors that are added to the chain are ordered by phase.
 * Within a phase, interceptors can order themselves. Each PhaseInterceptor 
 * has an ID. PhaseInterceptors can supply a Collection of IDs which they 
 * should run before or after, supplying fine grained ordering.
 * <p>
 *  
 */
public class PhaseInterceptorChain implements InterceptorChain {

    private static final Logger LOG = LogUtils.getL7dLogger(PhaseInterceptorChain.class); 

    private static final ThreadLocal<Message> CURRENT_MESSAGE = new ThreadLocal<Message>();
    
    private final Map<String, Integer> nameMap;
    private final Phase phases[];

    // heads[phase] refers to the first interceptor of the given phase
    private InterceptorHolder heads[];
    // tails[phase] refers to the last interceptor of the given phase
    private InterceptorHolder tails[];
    // hasAfters[phase] indicates that the given phase has already inserted
    // interceptors that may need to be placed after future to-be-inserted
    // interceptors.  This flag is used to activate ordering of interceptors
    // when new ones are added to the list for this phase.
    // Note no hasBefores[] is needed because implementation adds subsequent
    // interceptors to the end of the list by default.
    private boolean hasAfters[];

    
    private State state;
    private Message pausedMessage;
    private MessageObserver faultObserver;
    private PhaseInterceptorIterator iterator;
    
    // currently one chain for one request/response, use below as signal 
    // to avoid duplicate fault processing on nested calling of
    // doIntercept(), which will throw same fault multi-times
    private boolean faultOccurred;
    
    
    
    private PhaseInterceptorChain(PhaseInterceptorChain src) {
        //only used for clone
        state = State.EXECUTING;
        
        //immutable, just repoint
        nameMap = src.nameMap;
        phases = src.phases;
        
        int length = phases.length;
        hasAfters = new boolean[length];
        System.arraycopy(src.hasAfters, 0, hasAfters, 0, length);
        
        heads = new InterceptorHolder[length];
        tails = new InterceptorHolder[length];
        
        InterceptorHolder last = null;
        for (int x = 0; x < length; x++) {
            InterceptorHolder ih = src.heads[x];
            while (ih != null
                && ih.phaseIdx == x) {
                InterceptorHolder ih2 = new InterceptorHolder(ih);
                ih2.prev = last;
                if (last != null) {
                    last.next = ih2;
                }
                if (heads[x] == null) {
                    heads[x] = ih2;
                }
                tails[x] = ih2;
                last = ih2;
                ih = ih.next;
            }
        }
    }
    
    public PhaseInterceptorChain(SortedSet<Phase> ps) {
        state = State.EXECUTING;
        
        int numPhases = ps.size();
        phases = new Phase[numPhases];
        nameMap = new HashMap<String, Integer>();

        heads = new InterceptorHolder[numPhases];
        tails = new InterceptorHolder[numPhases];
        hasAfters = new boolean[numPhases];
        
        int idx = 0;
        for (Phase phase : ps) {
            phases[idx] = phase; 
            nameMap.put(phase.getName(), idx);
            ++idx;
        }
    }
    
    public static Message getCurrentMessage() {
        return CURRENT_MESSAGE.get();
    }
    
    // this method should really be on the InterceptorChain interface
    public State getState() {
        return state;
    }
    
    public PhaseInterceptorChain cloneChain() {
        return new PhaseInterceptorChain(this);
    }
    
    private void updateIterator() {
        if (iterator == null) {
            iterator = new PhaseInterceptorIterator(heads);
            outputChainToLog(false);
            //System.out.println(toString());
        }
    }
    
    public void add(Collection<Interceptor> newhandlers) {
        add(newhandlers, false);
    }

    public void add(Collection<Interceptor> newhandlers, boolean force) {
        if (newhandlers == null) {
            return;
        }

        for (Interceptor handler : newhandlers) {
            add(handler, force);
        }
    }

    public void add(Interceptor i) {
        add(i, false);
    }
    
    public void add(Interceptor i, boolean force) {
        PhaseInterceptor pi = (PhaseInterceptor)i;

        String phaseName = pi.getPhase();        
        Integer phase = nameMap.get(phaseName);
        
        if (phase == null) {
            LOG.fine("Skipping interceptor " + i.getClass().getName() 
                + ((phaseName == null) ? ": Phase declaration is missing." 
                : ": Phase " + phaseName + " specified does not exist."));
        } else {            
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Adding interceptor " + i + " to phase " + phaseName);
            }

            insertInterceptor(phase, pi, force);
        }
    }


    public synchronized void pause() {
        state = State.PAUSED;
    }

    public synchronized void resume() {
        if (state == State.PAUSED) {
            state = State.EXECUTING;
            doIntercept(pausedMessage);
        }
    }

    /**
     * Intercept a message, invoking each phase's handlers in turn.
     * 
     * @param message the message 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public synchronized boolean doIntercept(Message message) {
        updateIterator();
        boolean isFineLogging = LOG.isLoggable(Level.FINE);
        pausedMessage = message;

        Message oldMessage = CURRENT_MESSAGE.get();
        try {
            CURRENT_MESSAGE.set(message);
            while (state == State.EXECUTING && iterator.hasNext()) {
                try {
                    Interceptor currentInterceptor = iterator.next();
                    if (isFineLogging) {
                        LOG.fine("Invoking handleMessage on interceptor " + currentInterceptor);
                    }
                    //System.out.println("-----------" + currentInterceptor);
                    currentInterceptor.handleMessage(message);
                } catch (SuspendedInvocationException ex) {
                    // we need to resume from the same interceptor the exception got originated from
                    if (iterator.hasPrevious()) {
                        iterator.previous();
                    }
                    pause();
                    throw ex;
                } catch (RuntimeException ex) {
                    
                    if (!faultOccurred) {
     
                        faultOccurred = true;
                                            
                        FaultMode mode = message.get(FaultMode.class);
                        if (mode == FaultMode.CHECKED_APPLICATION_FAULT) {
                            if (LOG.isLoggable(Level.FINE)) { 
                                LogUtils.log(LOG, Level.FINE,
                                             "Application has thrown exception, unwinding now", ex);
                            } else if (LOG.isLoggable(Level.INFO)) {
                                Throwable t = ex;
                                if (ex instanceof Fault
                                    && ex.getCause() != null) {
                                    t = ex.getCause();
                                }                            
                                
                                LogUtils.log(LOG, Level.INFO,
                                             "Application has thrown exception, unwinding now: "
                                             + t.getClass().getName() 
                                             + ": " + ex.getMessage());
                            }
                        } else if (LOG.isLoggable(Level.WARNING)) {
                            if (mode == FaultMode.UNCHECKED_APPLICATION_FAULT) {
                                LogUtils.log(LOG, Level.WARNING,
                                             "Application has thrown exception, unwinding now", ex);
                            } else {
                                LogUtils.log(LOG, Level.WARNING,
                                             "Interceptor has thrown exception, unwinding now", ex);
                            }
                        }
    
                        message.setContent(Exception.class, ex);
                        if (message.getExchange() != null) {
                            message.getExchange().put(Exception.class, ex);
                        }                    
                        unwind(message);
                        
                        if (faultObserver != null) {
                            faultObserver.onMessage(message);
                        }
                    }
                    state = State.ABORTED;
                } 
            }
            if (state == State.EXECUTING) {
                state = State.COMPLETE;
                pausedMessage = null;
            }
            return state == State.COMPLETE;
        } finally {
            CURRENT_MESSAGE.set(oldMessage);
        }
    }
    
    /**
     * Intercept a message, invoking each phase's handlers in turn,
     * starting after the specified interceptor.
     * 
     * @param message the message
     * @param startingAfterInterceptorID the id of the interceptor 
     * @throws Exception
     */
    public synchronized boolean doInterceptStartingAfter(Message message,
                                                         String startingAfterInterceptorID) {
        updateIterator();
        while (state == State.EXECUTING && iterator.hasNext()) {
            PhaseInterceptor currentInterceptor = (PhaseInterceptor)iterator.next();
            if (currentInterceptor.getId().equals(startingAfterInterceptorID)) {
                break;
            }
        }
        return doIntercept(message);
    }

    /**
     * Intercept a message, invoking each phase's handlers in turn,
     * starting at the specified interceptor.
     * 
     * @param message the message
     * @param startingAtInterceptorID the id of the interceptor 
     * @throws Exception
     */
    public synchronized boolean doInterceptStartingAt(Message message,
                                                         String startingAtInterceptorID) {
        updateIterator();
        while (state == State.EXECUTING && iterator.hasNext()) {
            PhaseInterceptor currentInterceptor = (PhaseInterceptor)iterator.next();
            if (currentInterceptor.getId().equals(startingAtInterceptorID)) {
                iterator.previous();
                break;
            }
        }
        return doIntercept(message);
    }

    public synchronized void reset() {
        updateIterator();
        if (state == State.COMPLETE) {
            state = State.EXECUTING;
            iterator.reset();
        } else {
            iterator.reset();
        }
    }
    
    @SuppressWarnings("unchecked")
    private void unwind(Message message) {
        boolean isFineLogging = LOG.isLoggable(Level.FINE);
        while (iterator.hasPrevious()) {
            Interceptor currentInterceptor = iterator.previous();
            if (isFineLogging) {
                LOG.fine("Invoking handleFault on interceptor " + currentInterceptor);
            }
            try {
                currentInterceptor.handleFault(message);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Exception in handleFault on interceptor " + currentInterceptor, e);
                throw e;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Exception in handleFault on interceptor " + currentInterceptor, e);
                throw new RuntimeException(e);
            }
        }
    }

    public void remove(Interceptor i) {
        PhaseInterceptorIterator it = new PhaseInterceptorIterator(heads);
        while (it.hasNext()) {
            InterceptorHolder holder = it.nextInterceptorHolder();
            if (holder.interceptor == i) {
                remove(holder);
                return;
            }
        }
    }

    public synchronized void abort() {
        this.state = InterceptorChain.State.ABORTED;
    }

    public Iterator<Interceptor<? extends Message>> iterator() {
        return getIterator();
    }
    public ListIterator<Interceptor<? extends Message>> getIterator() {
        return new PhaseInterceptorIterator(heads);
    }

    private void remove(InterceptorHolder i) {
        if (i.prev != null) {
            i.prev.next = i.next;
        }
        if (i.next != null) {
            i.next.prev = i.prev;
        }
        int ph = i.phaseIdx;
        if (heads[ph] == i) {
            if (i.next != null
                && i.next.phaseIdx == ph) {
                heads[ph] = i.next;
            } else {
                heads[ph] = null;
                tails[ph] = null;
            }
        }
        if (tails[ph] == i) {
            if (i.prev != null
                && i.prev.phaseIdx == ph) {
                tails[ph] = i.prev;
            } else {
                heads[ph] = null;
                tails[ph] = null;
            }
        }
    }
    
    private void insertInterceptor(int phase, PhaseInterceptor interc, boolean force) {
        InterceptorHolder ih = new InterceptorHolder(interc, phase);
        if (heads[phase] == null) {
            // no interceptors yet in this phase
            heads[phase] = ih;
            tails[phase] = ih;
            hasAfters[phase] = !interc.getAfter().isEmpty();
            
            int idx = phase - 1;
            while (idx >= 0) {
                if (tails[idx] != null) {
                    break;
                }
                --idx;
            }
            if (idx >= 0) {
                //found something before us, in an earlier phase
                ih.prev = tails[idx];
                ih.next = tails[idx].next;
                if (ih.next != null) {
                    ih.next.prev = ih;
                }
                tails[idx].next = ih;
            } else {
                //did not find something before us, try after
                idx = phase + 1;
                while (idx < heads.length) {
                    if (heads[idx] != null) {
                        break;
                    }
                    ++idx;
                }
                
                if (idx != heads.length) {
                    //found something after us
                    ih.next = heads[idx];
                    heads[idx].prev = ih;
                }
            }
        } else { // this phase already has interceptors attached
        
            // list of interceptors that the new interceptor should precede
            Set beforeList = interc.getBefore();

            // list of interceptors that the new interceptor should be after
            Set afterList = interc.getAfter();
            
            // firstBefore will hold the first interceptor of a given phase
            // that the interceptor to be added must precede
            InterceptorHolder firstBefore = null;

            // lastAfter will hold the last interceptor of a given phase
            // that the interceptor to be added must come after
            InterceptorHolder lastAfter = null;
            
            String id = interc.getId();
            if (hasAfters[phase] || !beforeList.isEmpty()) {
            
                InterceptorHolder ih2 = heads[phase];
                while (ih2 != tails[phase].next) {
                    PhaseInterceptor cmp = ih2.interceptor;
                    String cmpId = cmp.getId();
                    if (cmpId != null && firstBefore == null
                        && (beforeList.contains(cmpId)
                            || cmp.getAfter().contains(id))) {
                        firstBefore = ih2;
                    } 
                    if (cmpId != null && afterList.contains(cmpId)) {
                        lastAfter = ih2;
                    }
                    if (!force && cmpId.equals(id)) {
                        // interceptor is already in chain
                        return;
                    }
                    ih2 = ih2.next;
                }
                if (lastAfter == null && beforeList.contains("*")) {
                    firstBefore = heads[phase];
                }
                //System.out.print("Didn't skip: " + phase.toString());
                //System.out.println("             " + interc.getId());
            } else if (!force) {
                // skip interceptor if already in chain
                InterceptorHolder ih2 = heads[phase];
                while (ih2 != tails[phase].next) {
                    if (ih2.interceptor.getId().equals(id)) {
                        return;
                    }
                    ih2 = ih2.next;
                }
                
                //System.out.print("Skipped: " + phase.toString());
                //System.out.println("         " + interc.getId());
            }
            hasAfters[phase] |= !afterList.isEmpty();
            
            if (firstBefore == null) {
                //just add new interceptor at the end
                ih.prev = tails[phase];
                ih.next = tails[phase].next;
                tails[phase].next = ih;
                
                if (ih.next != null) {
                    ih.next.prev = ih;
                }
                tails[phase] = ih;
            } else {
                ih.prev = firstBefore.prev;
                if (ih.prev != null) {
                    ih.prev.next = ih;
                }
                ih.next = firstBefore;
                firstBefore.prev = ih;
                
                if (heads[phase] == firstBefore) {
                    heads[phase] = ih;
                }
            }
        }
        if (iterator != null) {
            outputChainToLog(true);
        }
    }

    public String toString() {
        return toString(""); 
    }
    private String toString(String message) {
        StringBuilder chain = new StringBuilder();
        
        chain.append("Chain ")
            .append(super.toString())
            .append(message)
            .append(". Current flow:\n");
        
        for (int x = 0; x < phases.length; x++) {
            if (heads[x] != null) {
                chain.append("  ");
                printPhase(x, chain);
            }            
        }
        return chain.toString();
    }
    private void printPhase(int ph, StringBuilder chain) {
        
        chain.append(phases[ph].getName())
            .append(" [");
        InterceptorHolder i = heads[ph];
        boolean first = true;
        while (i != tails[ph].next) {
            if (first) {
                first = false;
            } else {
                chain.append(", ");
            }
            chain.append(i.interceptor.getClass().getSimpleName());
            i = i.next;
        }
        chain.append("]\n");
    }
    
    private void outputChainToLog(boolean modified) {
        if (LOG.isLoggable(Level.FINE)) {
            if (modified) {
                LOG.fine(toString(" was modified"));
            } else {
                LOG.fine(toString(" was created"));
            }
        }
    }
    
    public MessageObserver getFaultObserver() {
        return faultObserver;
    }
    
    public void setFaultObserver(MessageObserver faultObserver) {
        this.faultObserver = faultObserver;
    }
    
    static final class PhaseInterceptorIterator implements ListIterator<Interceptor<? extends Message>> {
        InterceptorHolder heads[];
        InterceptorHolder prev;
        InterceptorHolder first;
        
        public PhaseInterceptorIterator(InterceptorHolder h[]) {
            heads = h;
            first = findFirst();
        }
        
        public void reset() {
            prev = null;
            first = findFirst();
        }
        
        private InterceptorHolder findFirst() {
            for (int x = 0; x < heads.length; x++) {
                if (heads[x] != null) {
                    return heads[x];
                }
            }
            return null;
        }
        
        
        public boolean hasNext() {
            if (prev == null) {
                return first != null;
            }
            return prev.next != null;
        }

        @SuppressWarnings("unchecked")
        public Interceptor<? extends Message> next() {
            if (prev == null) {
                if (first == null) {
                    throw new NoSuchElementException();
                }
                prev = first;
            } else {
                if (prev.next == null) {
                    throw new NoSuchElementException();
                }
                prev = prev.next;
            }
            return prev.interceptor;
        }
        public InterceptorHolder nextInterceptorHolder() {
            if (prev == null) {
                if (first == null) {
                    throw new NoSuchElementException();
                }
                prev = first;
            } else {
                if (prev.next == null) {
                    throw new NoSuchElementException();
                }
                prev = prev.next;
            }
            return prev;
        }
        
        public boolean hasPrevious() {
            return prev != null;
        }
        @SuppressWarnings("unchecked")
        public Interceptor<? extends Message> previous() {
            if (prev == null) {
                throw new NoSuchElementException();
            }
            InterceptorHolder tmp = prev;
            prev = prev.prev;
            return tmp.interceptor;
        }
        
        public int nextIndex() {
            throw new UnsupportedOperationException();
        }
        public int previousIndex() {
            throw new UnsupportedOperationException();
        }
        public void add(Interceptor o) {
            throw new UnsupportedOperationException();
        }
        public void set(Interceptor o) {
            throw new UnsupportedOperationException();
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    
    static final class InterceptorHolder {
        PhaseInterceptor interceptor;
        InterceptorHolder next;
        InterceptorHolder prev;
        int phaseIdx;
        
        InterceptorHolder(PhaseInterceptor i, int p) {
            interceptor = i;
            phaseIdx = p;
        }
        InterceptorHolder(InterceptorHolder p) {
            interceptor = p.interceptor;
            phaseIdx = p.phaseIdx;
        }
    }

}
