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

package org.openbaton.nfvo.core.interfaces;

import java.util.concurrent.ExecutionException;
import org.openbaton.catalogue.nfvo.messages.VnfmOrLogMessage;
import org.openbaton.exceptions.BadFormatException;
import org.openbaton.exceptions.NotFoundException;

/** Created by lto on 17/05/16. */
public interface LogManagement {

  /**
   * Return a VnfmOrLogMessage containing output and error logs related to a host specified by its
   * NSR ID, VNFR name and hostname.
   *
   * @param nsrId NSR ID
   * @param vnfrName VNFR name
   * @param hostname hostname
   * @return log message
   * @throws NotFoundException if not found
   * @throws InterruptedException exception
   * @throws BadFormatException exception
   * @throws ExecutionException exception
   */
  VnfmOrLogMessage getLog(String nsrId, String vnfrName, String hostname)
      throws NotFoundException, InterruptedException, BadFormatException, ExecutionException;
}