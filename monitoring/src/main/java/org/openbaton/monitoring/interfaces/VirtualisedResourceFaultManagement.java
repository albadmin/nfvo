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

package org.openbaton.monitoring.interfaces;

import java.util.List;
import org.openbaton.catalogue.mano.common.monitoring.AbstractVirtualizedResourceAlarm;
import org.openbaton.catalogue.mano.common.monitoring.Alarm;
import org.openbaton.catalogue.mano.common.monitoring.AlarmEndpoint;
import org.openbaton.catalogue.mano.common.monitoring.PerceivedSeverity;
import org.openbaton.exceptions.MonitoringException;

/** Created by mob on 15.11.15. */
public interface VirtualisedResourceFaultManagement {
  /*
   * This operation enables the NFVO to subscribeForFault for notifications
   * related to the alarms and their state changes resulting from the
   * virtualised resources faults with the VIM. This also enables the NFVO
   * to specify the scope of the subscription in terms of the specific
   * alarms for the virtualised resources to be reported by the VIM using a filter as the input.
   * */
  String subscribeForFault(AlarmEndpoint filter) throws MonitoringException;

  String unsubscribeForFault(String alarmEndpointId) throws MonitoringException;
  /*
   * This operation distributes notifications to subscribers.
   * It is a one-way operation issued by the VIM that cannot
   * be invoked as an operation by the consumer (NFVO).
   * The following notifications can be published/notified/sent by this operation:
   *   - AlarmNotification  (VirtualizedResourceAlarmNotification)
   *   - AlarmStateChangedNotification  (VirtualizedResourceAlarmStateChangedNotification)
   */
  void notifyFault(AlarmEndpoint endpoint, AbstractVirtualizedResourceAlarm event);
  /*
   * This operation enables the NFVOs to query for active alarms from the VIM.
   */
  List<Alarm> getAlarmList(String vnfId, PerceivedSeverity perceivedSeverity)
      throws MonitoringException;
}
