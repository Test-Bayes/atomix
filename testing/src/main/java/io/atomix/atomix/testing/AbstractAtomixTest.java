/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package io.atomix.atomix.testing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import io.atomix.Atomix;
import io.atomix.AtomixClient;
import io.atomix.AtomixReplica;
import io.atomix.AtomixServer;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.LocalServerRegistry;
import io.atomix.catalyst.transport.LocalTransport;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;
import io.atomix.resource.Resource;
import io.atomix.resource.ResourceType;
import net.jodah.concurrentunit.ConcurrentTestCase;

/**
 * Abstract copycat test.
 *
 * @author <a href="http://github.com/kuujo>Jordan Halterman</a>
 */
public abstract class AbstractAtomixTest extends ConcurrentTestCase {
  protected LocalServerRegistry registry;
  protected int port;
  protected List<Address> members;
  protected List<Atomix> clients;
  protected List<Atomix> replicas;
  protected List<AtomixServer> servers;

  @BeforeClass
  protected void beforeClass() {
    init();
  }

  @AfterClass
  protected void afterClass() {
    cleanup();
  }

  protected void init() {
    port = 5000;
    registry = new LocalServerRegistry();
    members = new ArrayList<>();
    clients = new ArrayList<>();
    replicas = new ArrayList<>();
    servers = new ArrayList<>();
  }

  protected void cleanup() {
    clients.stream().forEach(a -> {
      try {
        a.close().join();
      } catch (Exception ignore) {
      }
    });
    replicas.stream().forEach(a -> {
      try {
        a.close().join();
      } catch (Exception ignore) {
      }
    });
    servers.stream().forEach(s -> {
      try {
        s.close().join();
      } catch (Exception ignore) {
      }
    });

    clients.clear();
    replicas.clear();
    servers.clear();
  }

  /**
   * Creates a resource factory for the given type.
   */
  @SuppressWarnings("unchecked")
  protected <T extends Resource<T>> Function<Atomix, T> get(String key, ResourceType<?> type) {
    return a -> {
      try {
        return a.get(key, (ResourceType<T>) type).get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    };
  }

  /**
   * Creates a resource factory for the given type.
   */
  @SuppressWarnings("unchecked")
  protected <T extends Resource<T>> Function<Atomix, T> create(String key, ResourceType<?> type) {
    return a -> {
      try {
        return a.create(key, (ResourceType<T>) type).get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    };
  }

  /**
   * Returns the next server address.
   *
   * @return The next server address.
   */
  protected Address nextAddress() {
    return new Address("localhost", port++);
  }

  /**
   * Creates a client.
   */
  protected Atomix createClient() throws Throwable {
    Atomix client = AtomixClient.builder(members).withTransport(new LocalTransport(registry)).build();
    client.open().thenRun(this::resume);
    clients.add(client);
    await(10000);
    return client;
  }

  /**
   * Creates an Atomix replica.
   */
  protected Atomix createReplica(Address address, List<Address> members) {
    AtomixReplica replica = AtomixReplica.builder(address, members)
        .withTransport(new LocalTransport(registry))
        .withStorage(Storage.builder()
            .withStorageLevel(StorageLevel.MEMORY)
            .withMaxEntriesPerSegment(8)
            .withMinorCompactionInterval(Duration.ofSeconds(5))
            .withMajorCompactionInterval(Duration.ofSeconds(10))
            .build())
        .build();
    replicas.add(replica);
    return replica;
  }

  /**
   * Creates an Atomix server.
   */
  protected AtomixServer createServer(Address address, List<Address> members) {
    AtomixServer server = AtomixServer.builder(address, members)
        .withTransport(new LocalTransport(registry))
        .withStorage(Storage.builder()
            .withStorageLevel(StorageLevel.MEMORY)
            .withMaxSegmentSize(1024 * 1024)
            .withMaxEntriesPerSegment(8)
            .withMinorCompactionInterval(Duration.ofSeconds(3))
            .withMajorCompactionInterval(Duration.ofSeconds(7))
            .build())
        .build();
    servers.add(server);
    return server;
  }

  /**
   * Creates a set of Atomix instances.
   */
  protected List<Atomix> createReplicas(int nodes) throws Throwable {
    List<Address> members = new ArrayList<>();
    for (int i = 0; i < nodes; i++) {
      members.add(nextAddress());
    }
    this.members.addAll(members);

    List<Atomix> replicas = new ArrayList<>();
    for (int i = 0; i < nodes; i++) {
      Atomix atomix = createReplica(members.get(i), members);
      atomix.open().thenRun(this::resume);
      replicas.add(atomix);
    }

    await(0, nodes);
    return replicas;
  }

  /**
   * Creates a set of Raft servers.
   */
  protected List<AtomixServer> createServers(int live, int total) throws Throwable {
    List<Address> members = new ArrayList<>();
    for (int i = 0; i < total; i++) {
      members.add(nextAddress());
    }
    this.members.addAll(members);

    List<AtomixServer> servers = new ArrayList<>();
    for (int i = 0; i < live; i++) {
      AtomixServer server = createServer(members.get(i), members);
      server.open().thenRun(this::resume);
      servers.add(server);
    }

    await(0, live);
    return servers;
  }

  /**
   * Creates a set of Raft servers.
   */
  protected List<AtomixServer> createServers(int nodes) throws Throwable {
    return createServers(nodes, nodes);
  }
}
