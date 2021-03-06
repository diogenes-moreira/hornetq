/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.core.remoting.server;

import java.util.Set;

import org.hornetq.api.core.Interceptor;
import org.hornetq.core.protocol.core.CoreRemotingConnection;
import org.hornetq.core.security.HornetQPrincipal;
import org.hornetq.spi.core.protocol.RemotingConnection;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * @author <a href="mailto:ataylor@redhat.com">Andy Taylor</a>
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 */
public interface RemotingService
{
   /**
    * Remove a connection from the connections held by the remoting service.
    * <strong>This method must be used only from the management API.
    * RemotingConnections are removed from the remoting service when their connectionTTL is hit.</strong>
    * @param remotingConnectionID the ID of the RemotingConnection to removed
    * @return the removed RemotingConnection
    */
   RemotingConnection removeConnection(Object remotingConnectionID);

   Set<RemotingConnection> getConnections();

   /**
    * Adds an interceptor for incoming messages. Invoking this method is equivalent to invoking
    * <code>addIncomingInterceptor(Interceptor)</code>.
    *
    * @param interceptor
    * @deprecated As of HornetQ 2.3.0.Final, replaced by
    * {@link #addIncomingInterceptor(Interceptor)} and
    * {@link #addOutgoingInterceptor(Interceptor)}
    */
   @Deprecated
   void addInterceptor(Interceptor interceptor);

   void addIncomingInterceptor(Interceptor interceptor);

   void addOutgoingInterceptor(Interceptor interceptor);

   /**
    * @param interceptor
    * @deprecated As of HornetQ 2.3.0.Final, replaced by
    * {@link #removeIncomingInterceptor(Interceptor)} and
    * {@link #removeOutgoingInterceptor(Interceptor)}
    */
   @Deprecated
   boolean removeInterceptor(Interceptor interceptor);

   boolean removeIncomingInterceptor(Interceptor interceptor);

   boolean removeOutgoingInterceptor(Interceptor interceptor);

   void stop(boolean criticalError) throws Exception;

   void start() throws Exception;

   boolean isStarted();

   /**
    * allow acceptors to use this as their default security Principal if applicable
    * @param principal
    */
   void allowInvmSecurityOverride(HornetQPrincipal principal);

   /**
    * Freezes and then disconnects all connections except the given one.
    * @param backupTransportConnection
    */
   void freeze(CoreRemotingConnection rc);
}
