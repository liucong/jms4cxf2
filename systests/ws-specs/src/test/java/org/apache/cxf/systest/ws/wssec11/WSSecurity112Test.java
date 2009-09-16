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

package org.apache.cxf.systest.ws.wssec11;


import org.apache.cxf.systest.ws.wssec11.server.Server;
import org.apache.cxf.systest.ws.wssec11.server.ServerRestricted;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * This class runs the second half of the tests, as having all in 
 * the one class causes an out of meemory problem in eclipse
 */
public class WSSecurity112Test extends WSSecurity11Common {
    private static boolean unrestrictedPoliciesInstalled;
    
    static {
        unrestrictedPoliciesInstalled = checkUnrestrictedPoliciesInstalled();
    };
      
    @BeforeClass
    public static void startServers() throws Exception {
        if (unrestrictedPoliciesInstalled) {
            assertTrue(
                    "Server failed to launch",
                    // run the server in the same process
                    // set this to false to fork
                    launchServer(Server.class, true)
            );
        } else {
            assertTrue(
                    "Server failed to launch",
                    // run the server in the same process
                    // set this to false to fork
                    launchServer(ServerRestricted.class, true)
            );
        }
    }

    @Test
    public void testClientServer() {
        String[] argv = null;
        if (unrestrictedPoliciesInstalled) {
            argv = new String[] {
                "X",
                "X-NoTimestamp",
                "X-AES128",
                "X-AES192",
                "X-TripleDES",
                "XD",
                "XD-ES",
                "XD-SEES",
            };
        } else {
            argv = new String[] {
                "X",
                "X-NoTimestamp",
                "XD",
                "XD-ES",
                "XD-SEES",
            };
        }
        runClientServer(argv, unrestrictedPoliciesInstalled);

    }
    
 
    
}