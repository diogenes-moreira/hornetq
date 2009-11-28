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

package org.hornetq.core.journal.impl.dataformat;

import org.hornetq.core.buffers.HornetQBuffer;
import org.hornetq.core.journal.EncodingSupport;

/**
 * A InternalEncoder
 *
 * @author <mailto:clebert.suconic@jboss.org">Clebert Suconic</a>
 *
 *
 */
public abstract class JournalInternalRecord implements EncodingSupport
{

   protected int fileID;
   
   public int getFileID()
   {
      return fileID;
   }

   public void setFileID(int fileID)
   {
      this.fileID = fileID;
   }

   public void decode(HornetQBuffer buffer)
   {
   }
   
   public void setNumberOfRecords(int records)
   {
   }
   
   public int getNumberOfRecords()
   {
      return 0;
   }

   public abstract int getEncodeSize();
}
