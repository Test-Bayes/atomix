/*
 * Copyright 2017-present Open Networking Foundation
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
 * limitations under the License.
 */
package io.atomix.core.transaction;

import io.atomix.primitive.DistributedPrimitiveBuilder;
import io.atomix.primitive.PrimitiveManagementService;
import io.atomix.primitive.PrimitiveType;

/**
 * Transactional set builder.
 */
public abstract class TransactionalSetBuilder<E>
    extends DistributedPrimitiveBuilder<TransactionalSetBuilder<E>, TransactionalSetConfig, TransactionalSet<E>> {
  protected TransactionalSetBuilder(PrimitiveType type, String name, TransactionalSetConfig config, PrimitiveManagementService managementService) {
    super(type, name, config, managementService);
  }
}
