/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.gemstone.gemfire.cache.lucene.internal;

import static org.mockito.Mockito.*;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.PartitionAttributes;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.cache.RegionFactory;
import com.gemstone.gemfire.cache.RegionShortcut;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEventQueueFactory;
import com.gemstone.gemfire.cache.asyncqueue.internal.AsyncEventQueueFactoryImpl;
import com.gemstone.gemfire.internal.cache.PartitionedRegion;
import com.gemstone.gemfire.internal.cache.extension.ExtensionPoint;
import com.gemstone.gemfire.test.fake.Fakes;
import com.gemstone.gemfire.test.junit.categories.UnitTest;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

@Category(UnitTest.class)
public class LuceneIndexForPartitionedRegionTest {

  @Rule
  public ExpectedException expectedExceptions = ExpectedException.none();

  @Test
  public void getIndexNameReturnsCorrectName() {
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = null;
    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    Assert.assertEquals(name, index.getName());
  }

  @Test
  public void getRegionPathReturnsPath() {
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = null;
    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    Assert.assertEquals(regionPath, index.getRegionPath());
  }

  @Test
  public void fileRegionExistsWhenFileRegionExistsShouldReturnTrue() {
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    PartitionedRegion region = mock(PartitionedRegion.class);
    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    String fileRegionName = index.createFileRegionName();
    when(cache.getRegion(fileRegionName)).thenReturn(region);

    Assert.assertTrue(index.fileRegionExists(fileRegionName));
  }

  @Test
  public void fileRegionExistsWhenFileRegionDoesNotExistShouldReturnFalse() {
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    String fileRegionName = index.createFileRegionName();
    when(cache.getRegion(fileRegionName)).thenReturn(null);

    Assert.assertFalse(index.fileRegionExists(fileRegionName));
  }

  @Test
  public void chunkRegionExistsWhenChunkRegionExistsShouldReturnTrue() {
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    PartitionedRegion region = mock(PartitionedRegion.class);
    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    String chunkRegionName = index.createChunkRegionName();
    when(cache.getRegion(chunkRegionName)).thenReturn(region);

    Assert.assertTrue(index.chunkRegionExists(chunkRegionName));
  }

  @Test
  public void chunkRegionExistsWhenChunkRegionDoesNotExistShouldReturnFalse() {
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    String chunkRegionName = index.createChunkRegionName();
    when(cache.getRegion(chunkRegionName)).thenReturn(null);

    Assert.assertFalse(index.chunkRegionExists(chunkRegionName));
  }

  @Test
  public void createChunkRegionWithPartitionShortcutCreatesRegionThroughFactory() {
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    RegionFactory regionFactory = mock(RegionFactory.class);
    PartitionAttributes partitionAttributes = mock(PartitionAttributes.class);
    when(cache.createRegionFactory(RegionShortcut.PARTITION)).thenReturn(regionFactory);
    when(regionFactory.setPartitionAttributes(any())).thenReturn(regionFactory);
    when(partitionAttributes.getTotalNumBuckets()).thenReturn(113);
    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);

    index.createChunkRegion(RegionShortcut.PARTITION, index.createFileRegionName(), partitionAttributes, index.createChunkRegionName());
    verify(regionFactory).create(index.createChunkRegionName());
  }

  @Test
  public void createFileRegionWithPartitionShortcutCreatesRegionThroughFactory() {
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    RegionFactory regionFactory = mock(RegionFactory.class);
    PartitionAttributes partitionAttributes = mock(PartitionAttributes.class);
    when(cache.createRegionFactory(RegionShortcut.PARTITION)).thenReturn(regionFactory);
    when(regionFactory.setPartitionAttributes(any())).thenReturn(regionFactory);
    when(partitionAttributes.getTotalNumBuckets()).thenReturn(113);
    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);

    index.createFileRegion(RegionShortcut.PARTITION, index.createFileRegionName(), partitionAttributes);
    verify(regionFactory).create(index.createFileRegionName());
  }

  @Test
  public void createAEQWithPersistenceCallsCreateOnAEQFactory() {
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    AsyncEventQueueFactoryImpl aeqFactory = mock(AsyncEventQueueFactoryImpl.class);
    when(cache.createAsyncEventQueueFactory()).thenReturn(aeqFactory);

    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    index.createAEQWithPersistence();

    verify(aeqFactory).create(any(), any());
  }

  @Test
  public void createAEQCallsCreateOnAEQFactory() {
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    AsyncEventQueueFactoryImpl aeqFactory = mock(AsyncEventQueueFactoryImpl.class);
    when(cache.createAsyncEventQueueFactory()).thenReturn(aeqFactory);

    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    index.createAEQ();

    verify(aeqFactory).create(any(), any());
  }

  private void initializeScenario(final boolean withPersistence, final String regionPath, final Cache cache) {
    int defaultLocalMemory = 100;
    initializeScenario(withPersistence, regionPath, cache, defaultLocalMemory);
  }

  private void initializeScenario(final boolean withPersistence, final String regionPath, final Cache cache, int localMaxMemory) {
    PartitionedRegion region = mock(PartitionedRegion.class);
    RegionAttributes regionAttributes = mock(RegionAttributes.class);
    PartitionAttributes partitionAttributes = mock(PartitionAttributes.class);
    RegionFactory regionFactory = mock(RegionFactory.class);
    DataPolicy dataPolicy = mock(DataPolicy.class);
    ExtensionPoint extensionPoint = mock(ExtensionPoint.class);
    when(cache.getRegion(regionPath)).thenReturn(region);
    when(cache.createRegionFactory(isA(RegionShortcut.class))).thenReturn(regionFactory);
    when(region.getAttributes()).thenReturn(regionAttributes);
    when(regionAttributes.getPartitionAttributes()).thenReturn(partitionAttributes);
    when(regionAttributes.getDataPolicy()).thenReturn(dataPolicy);
    when(partitionAttributes.getLocalMaxMemory()).thenReturn(localMaxMemory);
    when(partitionAttributes.getTotalNumBuckets()).thenReturn(113);
    when(dataPolicy.withPersistence()).thenReturn(withPersistence);
    when(region.getExtensionPoint()).thenReturn(extensionPoint);
  }

  @Test
  public void initializeWithNoLocalMemoryThrowsException() {
    expectedExceptions.expect(IllegalStateException.class);
    expectedExceptions.expectMessage("The data region to create lucene index should be with storage");
    boolean withPersistence = false;
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    initializeScenario(withPersistence, regionPath, cache, 0);
    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    index.initialize();
  }

  @Test
  public void initializeWithPersistenceShouldCreateAEQWithPersistence() {
    boolean withPersistence = true;
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    initializeScenario(withPersistence, regionPath, cache);

    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    index.setSearchableFields(new String[]{"field"});
    LuceneIndexForPartitionedRegion spy = spy(index);
    doReturn(null).when(spy).createFileRegion(any(), any(), any());
    doReturn(null).when(spy).createChunkRegion(any(), any(), any(), any());
    doReturn(null).when(spy).createAEQWithPersistence();
    spy.initialize();

    verify(spy).createAEQWithPersistence();
  }

  @Test
  public void initializeWithoutPersistenceShouldCreateAEQ() {
    boolean withPersistence = false;
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    initializeScenario(withPersistence, regionPath, cache);

    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    index.setSearchableFields(new String[]{"field"});
    LuceneIndexForPartitionedRegion spy = spy(index);
    doReturn(null).when(spy).createFileRegion(any(), any(), any());
    doReturn(null).when(spy).createChunkRegion(any(), any(), any(), any());
    doReturn(null).when(spy).createAEQ();
    spy.initialize();

    verify(spy).createAEQ();
  }

  @Test
  public void initializeShouldCreatePartitionChunkRegion() {
    boolean withPersistence = false;
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    initializeScenario(withPersistence, regionPath, cache);

    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    index.setSearchableFields(new String[]{"field"});
    LuceneIndexForPartitionedRegion spy = spy(index);
    doReturn(null).when(spy).createFileRegion(any(), any(), any());
    doReturn(null).when(spy).createChunkRegion(any(), any(), any(), any());
    doReturn(null).when(spy).createAEQ();
    spy.initialize();

    verify(spy).createChunkRegion(eq(RegionShortcut.PARTITION), eq(index.createFileRegionName()), any(), eq(index.createChunkRegionName()));
  }

  @Test
  public void initializeShouldCreatePartitionFileRegion() {
    boolean withPersistence = false;
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    initializeScenario(withPersistence, regionPath, cache);

    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    index.setSearchableFields(new String[]{"field"});
    LuceneIndexForPartitionedRegion spy = spy(index);
    doReturn(null).when(spy).createFileRegion(any(), any(), any());
    doReturn(null).when(spy).createChunkRegion(any(), any(), any(), any());
    doReturn(null).when(spy).createAEQ();
    spy.initialize();

    verify(spy).createFileRegion(eq(RegionShortcut.PARTITION), eq(index.createFileRegionName()), any());
  }

  @Test
  public void initializeShouldCreatePartitionPersistentChunkRegion() {
    boolean withPersistence = true;
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    initializeScenario(withPersistence, regionPath, cache);

    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    index.setSearchableFields(new String[]{"field"});
    LuceneIndexForPartitionedRegion spy = spy(index);
    doReturn(null).when(spy).createFileRegion(any(), any(), any());
    doReturn(null).when(spy).createChunkRegion(any(), any(), any(), any());
    doReturn(null).when(spy).createAEQWithPersistence();
    spy.initialize();

    verify(spy).createChunkRegion(eq(RegionShortcut.PARTITION_PERSISTENT), eq(index.createFileRegionName()), any(), eq(index.createChunkRegionName()));
  }

  @Test
  public void initializeShouldCreatePartitionPersistentFileRegion() {
    boolean withPersistence = true;
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    initializeScenario(withPersistence, regionPath, cache);

    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    index.setSearchableFields(new String[]{"field"});
    LuceneIndexForPartitionedRegion spy = spy(index);
    doReturn(null).when(spy).createFileRegion(any(), any(), any());
    doReturn(null).when(spy).createChunkRegion(any(), any(), any(), any());
    doReturn(null).when(spy).createAEQWithPersistence();
    spy.initialize();

    verify(spy).createFileRegion(eq(RegionShortcut.PARTITION_PERSISTENT), eq(index.createFileRegionName()), any());
  }

  @Test
  public void initializeWhenCalledMultipleTimesShouldNotCreateMultipleFileRegions() {
    boolean withPersistence = true;
    String name = "indexName";
    String regionPath = "regionName";
    Cache cache = Fakes.cache();
    initializeScenario(withPersistence, regionPath, cache);

    LuceneIndexForPartitionedRegion index = new LuceneIndexForPartitionedRegion(name, regionPath, cache);
    index.setSearchableFields(new String[]{"field"});
    LuceneIndexForPartitionedRegion spy = spy(index);
    doReturn(null).when(spy).createFileRegion(any(), any(), any());
    doReturn(null).when(spy).createChunkRegion(any(), any(), any(), any());
    doReturn(null).when(spy).createAEQWithPersistence();
    spy.initialize();
    spy.initialize();

    verify(spy).createFileRegion(eq(RegionShortcut.PARTITION_PERSISTENT), eq(index.createFileRegionName()), any());
  }

}
