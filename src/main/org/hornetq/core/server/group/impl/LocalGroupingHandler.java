/*
 * Copyright 2009 Red Hat, Inc.
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.hornetq.core.server.group.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.hornetq.core.client.management.impl.ManagementHelper;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.management.ManagementService;
import org.hornetq.core.management.Notification;
import org.hornetq.core.management.NotificationType;
import org.hornetq.core.persistence.StorageManager;
import org.hornetq.core.postoffice.BindingType;
import org.hornetq.core.server.group.GroupingHandler;
import org.hornetq.utils.SimpleString;
import org.hornetq.utils.TypedProperties;

/**
 * A Local Grouping handler. All the Remote handlers will talk with us
 *
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 */
public class LocalGroupingHandler implements GroupingHandler
{
   private static Logger log = Logger.getLogger(LocalGroupingHandler.class);

   private ConcurrentHashMap<SimpleString, GroupBinding> map = new ConcurrentHashMap<SimpleString, GroupBinding>();

   private ConcurrentHashMap<SimpleString, List<GroupBinding>> groupMap = new ConcurrentHashMap<SimpleString, List<GroupBinding>>();

   private final SimpleString name;

   private final ManagementService managementService;

   private SimpleString address;
   private StorageManager storageManager;
   private int timeout;

   public LocalGroupingHandler(final ManagementService managementService, final SimpleString name, final SimpleString address, StorageManager storageManager, int timeout)
   {
      this.managementService = managementService;
      this.name = name;
      this.address = address;
      this.storageManager = storageManager;
      this.timeout = timeout;
   }

   public SimpleString getName()
   {
      return name;
   }


   public Response propose(Proposal proposal) throws Exception
   {
      log.info("proposing proposal " + proposal);
      if (proposal.getClusterName() == null)
      {
         GroupBinding original = map.get(proposal.getGroupId());
         return original == null ? null : new Response(proposal.getGroupId(), original.getClusterName());
      }
      GroupBinding groupBinding = new GroupBinding(proposal.getGroupId(), proposal.getClusterName());
      if (map.putIfAbsent(groupBinding.getGroupId(), groupBinding) == null)
      {
         groupBinding.setId(storageManager.generateUniqueID());
         List<GroupBinding> newList = new ArrayList<GroupBinding>();
         List<GroupBinding> oldList = groupMap.putIfAbsent(groupBinding.getClusterName(), newList);
         if(oldList != null)
         {
            newList = oldList;
         }
         newList.add(groupBinding);
         storageManager.addGrouping(groupBinding);
         if (storageManager.isReplicated())
         {
            storageManager.waitOnReplication(timeout);
         }
         return new Response(groupBinding.getGroupId(), groupBinding.getClusterName());
      }
      else
      {
         groupBinding = map.get(proposal.getGroupId());
         return new Response(groupBinding.getGroupId(), proposal.getClusterName(), groupBinding.getClusterName());
      }
   }

   public void proposed(Response response) throws Exception
   {
   }

   public void send(Response response, int distance) throws Exception
   {
      TypedProperties props = new TypedProperties();
      props.putStringProperty(ManagementHelper.HDR_PROPOSAL_GROUP_ID, response.getGroupId());
      props.putStringProperty(ManagementHelper.HDR_PROPOSAL_VALUE, response.getClusterName());
      props.putStringProperty(ManagementHelper.HDR_PROPOSAL_ALT_VALUE, response.getAlternativeClusterName());
      props.putIntProperty(ManagementHelper.HDR_BINDING_TYPE, BindingType.LOCAL_QUEUE_INDEX);
      props.putStringProperty(ManagementHelper.HDR_ADDRESS, address);
      props.putIntProperty(ManagementHelper.HDR_DISTANCE, distance);
      Notification notification = new Notification(null, NotificationType.PROPOSAL_RESPONSE, props);
      managementService.sendNotification(notification);
   }

   public Response receive(Proposal proposal, int distance) throws Exception
   {
      log.trace("received proposal " + proposal);
      return propose(proposal);
   }

   public void addGroupBinding(GroupBinding groupBinding)
   {
      map.put(groupBinding.getGroupId(), groupBinding);
      List<GroupBinding> newList = new ArrayList<GroupBinding>();
      List<GroupBinding> oldList = groupMap.putIfAbsent(groupBinding.getClusterName(), newList);
      if(oldList != null)
      {
         newList = oldList;
      }
      newList.add(groupBinding);
   }

   public Response getProposal(SimpleString fullID)
   {
      GroupBinding original = map.get(fullID);
      return original == null ? null : new Response(fullID, original.getClusterName());
   }

   public void onNotification(Notification notification)
   {
      if (notification.getType() == NotificationType.BINDING_REMOVED)
      {
         SimpleString clusterName = (SimpleString) notification.getProperties().getProperty(ManagementHelper.HDR_CLUSTER_NAME);
         List<GroupBinding> list = groupMap.remove(clusterName);
         if (list != null)
         {
            for (GroupBinding val : list)
            {
               if (val != null)
               {
                  map.remove(val.getGroupId());
                  try
                  {
                     storageManager.deleteGrouping(val);
                  }
                  catch (Exception e)
                  {
                     log.warn("Unable to delete group binding info " + val.getGroupId(), e);
                  }
               }
            }
         }
      }
   }
}
