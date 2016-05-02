/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gemstone.gemfire.distributed;

import static com.gemstone.gemfire.distributed.internal.DistributionConfig.*;
import static com.gemstone.gemfire.internal.AvailablePort.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import com.gemstone.gemfire.SystemConnectException;
import com.gemstone.gemfire.cache.client.internal.locator.ClientConnectionRequest;
import com.gemstone.gemfire.cache.client.internal.locator.ClientConnectionResponse;
import com.gemstone.gemfire.cache.client.internal.locator.QueueConnectionRequest;
import com.gemstone.gemfire.cache.client.internal.locator.QueueConnectionResponse;
import com.gemstone.gemfire.distributed.internal.InternalDistributedSystem;
import com.gemstone.gemfire.distributed.internal.InternalLocator;
import com.gemstone.gemfire.distributed.internal.ServerLocation;
import com.gemstone.gemfire.distributed.internal.membership.gms.messenger.JGroupsMessenger;
import com.gemstone.gemfire.distributed.internal.tcpserver.TcpClient;
import com.gemstone.gemfire.internal.OSProcess;
import com.gemstone.gemfire.internal.cache.GemFireCacheImpl;
import com.gemstone.gemfire.internal.cache.tier.sockets.ClientProxyMembershipID;
import com.gemstone.gemfire.management.internal.JmxManagerAdvisor.JmxManagerProfile;
import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

@Category(IntegrationTest.class)
public class LocatorJUnitTest {

  private static final int REQUEST_TIMEOUT = 5 * 1000;

  private Locator locator;
  private int port;
  private File tmpFile;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    tmpFile = File.createTempFile("locator", ".log");
    port = getRandomAvailablePort(SOCKET);
    File locatorFile = new File("locator"+port+".dat");
    if (locatorFile.exists()) {
      locatorFile.delete();
    }
  }

  @After
  public void tearDown() {
    if(locator != null) {
      locator.stop();
    }
   assertEquals(false, Locator.hasLocator());
  }

  /**
   * TRAC #45804: if jmx-manager-start is true in a locator then gfsh connect will fail
   */
  @Test
  public void testGfshConnectShouldSucceedIfJmxManagerStartIsTrueInLocator() throws Exception {
    Properties dsprops = new Properties();
    int jmxPort = getRandomAvailablePort(SOCKET);
    dsprops.setProperty("mcast-port", "0");
    dsprops.setProperty("locators", "localhost[" + port + "]");
    dsprops.setProperty("jmx-manager-port", ""+jmxPort);
    dsprops.setProperty("jmx-manager-start", "true");
    dsprops.setProperty("jmx-manager-http-port", "0");
    dsprops.setProperty(ENABLE_CLUSTER_CONFIGURATION_NAME, "false");
    System.setProperty("gemfire.disableManagement", "false"); // not needed
    try {
      locator = Locator.startLocatorAndDS(port, new File("testJmxManager.log"), dsprops);
      List<JmxManagerProfile> alreadyManaging = GemFireCacheImpl.getInstance().getJmxManagerAdvisor().adviseAlreadyManaging();
      assertEquals(1, alreadyManaging.size());
      assertEquals(GemFireCacheImpl.getInstance().getMyId(), alreadyManaging.get(0).getDistributedMember());
    } finally {
      System.clearProperty("gemfire.enabledManagement");
    }
  }

  @Test
  public void testBasicInfo() throws Exception {
    locator = Locator.startLocator(port, tmpFile);
   assertTrue(locator.isPeerLocator());
   assertFalse(locator.isServerLocator());
    String[] info = InternalLocator.getLocatorInfo(InetAddress.getLocalHost(), port);
   assertNotNull(info);
   assertTrue(info.length > 1);
  }

  @Test
  public void testNoThreadLeftBehind() throws Exception {
    Properties dsprops = new Properties();
    dsprops.setProperty("mcast-port", "0");
    dsprops.setProperty("locators", "localhost[" + port + "]");
    dsprops.setProperty("jmx-manager-start", "false");
    dsprops.setProperty(ENABLE_CLUSTER_CONFIGURATION_NAME, "false");

    JGroupsMessenger.THROW_EXCEPTION_ON_START_HOOK = true;
    int threadCount = Thread.activeCount();
    try {
      locator = Locator.startLocatorAndDS(port, new File(""), dsprops);
      locator.stop();
      fail("expected an exception");
    } catch (SystemConnectException expected) {
      
      for (int i=0; i<10; i++) {
        if (threadCount < Thread.activeCount()) {
          Thread.sleep(1000);
        }
      }
      if (threadCount < Thread.activeCount()) {
          OSProcess.printStacks(0);
          fail("expected " + threadCount + " threads or fewer but found " + Thread.activeCount()
              +".  Check log file for a thread dump.");
        }
    }
  }

  @Test
  public void testServerOnly() throws Exception {
    Properties props = new Properties();
    props.setProperty("mcast-port", "0");
    props.setProperty(ENABLE_CLUSTER_CONFIGURATION_NAME, "false");
    locator = Locator.startLocatorAndDS(port, tmpFile, null, props, false, true, null);
   assertFalse(locator.isPeerLocator());
   assertTrue(locator.isServerLocator());
    Thread.sleep(1000);
    doServerLocation();
  }

  @Test
  public void testBothPeerAndServer() throws Exception {
    Properties props = new Properties();
    props.setProperty("mcast-port", "0");
    props.setProperty(ENABLE_CLUSTER_CONFIGURATION_NAME, "false");

    locator = Locator.startLocatorAndDS(port, tmpFile, null, props);
   assertTrue(locator.isPeerLocator());
   assertTrue(locator.isServerLocator());
    Thread.sleep(1000);
    doServerLocation();
    locator.stop();
  }

  /**
   * Make sure two ServerLocation objects on different hosts but with the same port
   * are not equal
   * <p/>
   * TRAC #42040: LoadBalancing directs all traffic to a single cache server if all servers are started on the same port
   */
  @Test
  public void testServerLocationOnDifferentHostsShouldNotTestEqual() {
    ServerLocation sl1 = new ServerLocation("host1", 777);
    ServerLocation sl2 = new ServerLocation("host2", 777);
    if (sl1.equals(sl2)) {
      fail("ServerLocation instances on different hosts should not test equal");
    }
  }

  private void doServerLocation() throws Exception {
    {
      ClientConnectionRequest request = new ClientConnectionRequest(Collections.EMPTY_SET, "group1");
      ClientConnectionResponse response = (ClientConnectionResponse) TcpClient.requestToServer(InetAddress.getLocalHost(), port, request, REQUEST_TIMEOUT);
     assertEquals(null, response.getServer());
    }

    {
      QueueConnectionRequest request = new QueueConnectionRequest(ClientProxyMembershipID.getNewProxyMembership(InternalDistributedSystem.getAnyInstance()), 3, Collections.EMPTY_SET, "group1",true);
      QueueConnectionResponse response = (QueueConnectionResponse) TcpClient.requestToServer(InetAddress.getLocalHost(), port, request, REQUEST_TIMEOUT);
     assertEquals(new ArrayList(), response.getServers());
     assertFalse(response.isDurableQueueFound());
    }
  }

}
