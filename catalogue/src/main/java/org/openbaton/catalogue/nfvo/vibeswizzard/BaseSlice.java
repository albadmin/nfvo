/*
 * Copyright (c) 2015-2018 Open Baton (http://openbaton.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.catalogue.nfvo.vibeswizzard;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.openbaton.catalogue.util.BaseEntity;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name", "projectId"}))
public class BaseSlice extends BaseEntity {

  @NotNull
  @Size(min = 1)
  protected String description;

  @NotNull
  @Size(min = 1)
  protected String subnetwork;

  @NotNull
  @Size(min = 1)
  protected String name;

  public BaseSlice(String name, String description, String subnetwork) {
    this.name = name;
    this.description = description;
    this.subnetwork = subnetwork;
  }

  public String getSubNetwork() {
    return subnetwork;
  }

  public void setSubNetwork(String subnetwork) {
    this.subnetwork = subnetwork;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "=Slice{"
        + "name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + "} "
        + super.toString();
  }
}
