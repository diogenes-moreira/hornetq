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

package org.hornetq.core.protocol.core.impl.wireformat;

import org.hornetq.api.core.HornetQBuffer;
import org.hornetq.api.core.SimpleString;
import org.hornetq.core.protocol.core.impl.PacketImpl;

/**
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>

 * @version <tt>$Revision$</tt>
 */
public class CreateQueueMessage extends PacketImpl
{

   private SimpleString address;

   private SimpleString queueName;

   private SimpleString filterString;

   private boolean durable;

   private boolean temporary;

   private boolean requiresResponse;

   public CreateQueueMessage(final SimpleString address,
                             final SimpleString queueName,
                             final SimpleString filterString,
                             final boolean durable,
                             final boolean temporary,
                             final boolean requiresResponse)
   {
      this();

      this.address = address;
      this.queueName = queueName;
      this.filterString = filterString;
      this.durable = durable;
      this.temporary = temporary;
      this.requiresResponse = requiresResponse;
   }

   public CreateQueueMessage()
   {
      super(PacketImpl.CREATE_QUEUE);
   }

   // Public --------------------------------------------------------

   @Override
   public String toString()
   {
      StringBuffer buff = new StringBuffer(getParentString());
      buff.append(", address=" + address);
      buff.append(", queueName=" + queueName);
      buff.append(", filterString=" + filterString);
      buff.append(", durable=" + durable);
      buff.append(", temporary=" + temporary);
      buff.append("]");
      return buff.toString();
   }

   public SimpleString getAddress()
   {
      return address;
   }

   public SimpleString getQueueName()
   {
      return queueName;
   }

   public SimpleString getFilterString()
   {
      return filterString;
   }

   public boolean isDurable()
   {
      return durable;
   }

   public boolean isTemporary()
   {
      return temporary;
   }

   public boolean isRequiresResponse()
   {
      return requiresResponse;
   }

   @Override
   public void encodeRest(final HornetQBuffer buffer)
   {
      buffer.writeSimpleString(address);
      buffer.writeSimpleString(queueName);
      buffer.writeNullableSimpleString(filterString);
      buffer.writeBoolean(durable);
      buffer.writeBoolean(temporary);
      buffer.writeBoolean(requiresResponse);
   }

   @Override
   public void decodeRest(final HornetQBuffer buffer)
   {
      address = buffer.readSimpleString();
      queueName = buffer.readSimpleString();
      filterString = buffer.readNullableSimpleString();
      durable = buffer.readBoolean();
      temporary = buffer.readBoolean();
      requiresResponse = buffer.readBoolean();
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((address == null) ? 0 : address.hashCode());
      result = prime * result + (durable ? 1231 : 1237);
      result = prime * result + ((filterString == null) ? 0 : filterString.hashCode());
      result = prime * result + ((queueName == null) ? 0 : queueName.hashCode());
      result = prime * result + (requiresResponse ? 1231 : 1237);
      result = prime * result + (temporary ? 1231 : 1237);
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (!super.equals(obj))
         return false;
      if (!(obj instanceof CreateQueueMessage))
         return false;
      CreateQueueMessage other = (CreateQueueMessage)obj;
      if (address == null)
      {
         if (other.address != null)
            return false;
      }
      else if (!address.equals(other.address))
         return false;
      if (durable != other.durable)
         return false;
      if (filterString == null)
      {
         if (other.filterString != null)
            return false;
      }
      else if (!filterString.equals(other.filterString))
         return false;
      if (queueName == null)
      {
         if (other.queueName != null)
            return false;
      }
      else if (!queueName.equals(other.queueName))
         return false;
      if (requiresResponse != other.requiresResponse)
         return false;
      if (temporary != other.temporary)
         return false;
      return true;
   }
}
