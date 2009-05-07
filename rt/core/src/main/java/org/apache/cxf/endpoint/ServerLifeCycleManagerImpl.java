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

package org.apache.cxf.endpoint;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.extension.BusExtension;

public class ServerLifeCycleManagerImpl implements ServerLifeCycleManager, BusExtension {
    
    private List<ServerLifeCycleListener> listeners = 
            new CopyOnWriteArrayList<ServerLifeCycleListener>();

    public Class<?> getRegistrationType() {
        return ServerLifeCycleManager.class;
    }

    public synchronized void registerListener(ServerLifeCycleListener listener) {
        listeners.add(listener);
    }

    public void startServer(Server server) {
        for (ServerLifeCycleListener listener : listeners) {
            listener.startServer(server);
        }
    }

    public void stopServer(Server server) {
        for (ServerLifeCycleListener listener : listeners) {
            listener.stopServer(server);
        }
    }

    public synchronized void unRegisterListener(ServerLifeCycleListener listener) {
        listeners.remove(listener);
    }
}
