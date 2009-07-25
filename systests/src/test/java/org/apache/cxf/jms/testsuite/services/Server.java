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
package org.apache.cxf.jms.testsuite.services;

import javax.xml.ws.Endpoint;

import org.apache.cxf.jms.testsuite.util.JMSTestUtil;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {

    protected void run() {
        Test0001Impl t0001 = new Test0001Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0001").getAddress().trim(), t0001);
        
        Test0003Impl t0003 = new Test0003Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0003").getAddress().trim(), t0003);
        
        Test0005Impl t0005 = new Test0005Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0005").getAddress().trim(), t0005);
        
        Test0006Impl t0006 = new Test0006Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0006").getAddress().trim(), t0006);
    }

    public static void main(String[] args) {
        try {
            Server s = new Server();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
