/*
 * JBoss, the OpenSource J2EE webOS
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.jms.perf.framework.data;

import org.jboss.logging.Logger;
import org.jboss.jms.perf.framework.protocol.Job;
import org.jboss.jms.perf.framework.Runner;
import org.jboss.jms.perf.framework.configuration.Configuration;
import org.jboss.jms.perf.framework.protocol.Failure;
import org.jboss.jms.perf.framework.protocol.ResetRequest;
import org.jboss.jms.perf.framework.remoting.Coordinator;
import org.jboss.jms.perf.framework.remoting.Result;
import org.jboss.jms.perf.framework.remoting.rmi.RMICoordinator;
import org.jboss.jms.perf.framework.remoting.jbossremoting.JBossRemotingCoordinator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Properties;

/**
 * @author <a href="tim.fox@jboss.com">Tim Fox</a>
 * @author <a href="ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version $Revision$
 *
 * $Id$
 */
public class PerformanceTest implements Serializable, JobList
{
   // Constants -----------------------------------------------------

   private static final long serialVersionUID = 4821514879181362348L;

   private static final Logger log = Logger.getLogger(PerformanceTest.class);

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   private Runner runner;

   protected long id;
   protected String name;
   protected List executions;

   protected String destination;
   protected String connectionFactory;
   protected List jobs;

   // Constructors --------------------------------------------------

   public PerformanceTest(Runner runner, String name)
   {
      this.runner = runner;
      this.name = name;
      id = Long.MIN_VALUE;
      executions = new ArrayList();
      jobs = new ArrayList();
   }

   // JobList implementation ----------------------------------------

   public void addJob(Job job)
   {
      jobs.add(job);
   }

   public int size()
   {
      return jobs.size();
   }

   public Iterator iterator()
   {
      return jobs.iterator();
   }

   // Public --------------------------------------------------------

   public void addParallelJobs(JobList parallelJobs)
   {
      jobs.add(parallelJobs);
   }

   public void setDestination(String destination)
   {
      this.destination = destination;
   }

   public String getDestination()
   {
      return destination;
   }

   public void setConnectionFactory(String cf)
   {
      this.connectionFactory = cf;
   }

   public String getConnectionFactory()
   {
      return connectionFactory;
   }

   public void addExecution(Execution exec)
   {
      executions.add(exec);
   }

   public List getExecutions()
   {
      return executions;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public long getId()
   {
      return id;
   }

   public void run() throws Exception
   {
      log.info("");
      log.info("Performance Test \"" + getName() + "\"");

      int executionCounter = 1;

      for(Iterator ei = executions.iterator(); ei.hasNext(); )
      {
         Execution e = (Execution)ei.next();

         Coordinator coordinator = prepare(e);

         log.info("");
         log.info("Execution " + executionCounter++ + " (provider " + e.getProviderName() + ")");

         for(Iterator i = jobs.iterator(); i.hasNext(); )
         {
            Object o = i.next();

            if (o instanceof Job)
            {
               Result result = run(coordinator, (Job)o);
               log.info(e.size() + ". " + result);
               e.addMeasurement(result);
            }
            else
            {
               log.info(e.size() + ". PARALLEL");

               List results = runParallel(coordinator, (JobList)o);
               for(Iterator ri = results.iterator(); ri.hasNext(); )
               {
                  log.info("    " + ri.next());
               }
               e.addMeasurement(results);
            }
         }
      }
   }

   public String toString()
   {
      StringBuffer sb = new StringBuffer();

      sb.append("PerformanceTest[").append(name).append("](");
      for(Iterator i = executions.iterator(); i.hasNext(); )
      {
         Execution e = (Execution)i.next();
         sb.append(e.getProviderName());
         if (i.hasNext())
         {
            sb.append(", ");
         }
      }
      sb.append(")");
      return sb.toString();
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   /**
    * Prepares the list of jobs to be ran in a specific execution context, and also probe
    * executors.
    */
   private Coordinator prepare(Execution e) throws Exception
   {
      log.info("");
      
      String providerName = e.getProviderName();
      List executorURLs = new ArrayList();

      for(Iterator i = jobs.iterator(); i.hasNext(); )
      {
         Object o = i.next();
         if (o instanceof Job)
         {
            prepare((Job)o, providerName, executorURLs);
         }
         else
         {
            for(Iterator ji = ((JobList)o).iterator(); ji.hasNext(); )
            {
               prepare((Job)ji.next(), providerName, executorURLs);
            }
         }
      }

      Coordinator coordinator = pickCoordinator(executorURLs);
      checkExecutors(coordinator, executorURLs);
      return coordinator;
   }

   private Coordinator pickCoordinator(List executorURLs) throws Exception
   {
      int coordinatorType = -1;

      for(Iterator i = executorURLs.iterator(); i.hasNext(); )
      {
         String executorURL = (String)i.next();
         int type;
         if (JBossRemotingCoordinator.isValidURL(executorURL))
         {
            type = Coordinator.JBOSSREMOTING;
         }
         else if (RMICoordinator.isValidURL(executorURL))
         {
            type = Coordinator.RMI;
         }
         else
         {
            throw new Exception("Unknown URL type: " + executorURL);
         }

         if (coordinatorType != -1 && coordinatorType != type)
         {
            throw new Exception("Mixed URL types (" +
               Configuration.coordinatorTypeToString(coordinatorType) + ", " +
               Configuration.coordinatorTypeToString(type) + "), use a homogeneous configuration");
         }

         coordinatorType = type;
      }

      if (coordinatorType == Coordinator.JBOSSREMOTING)
      {
         return new JBossRemotingCoordinator();
      }
      else if (coordinatorType == Coordinator.RMI)
      {
         return new RMICoordinator();
      }
      else
      {
         throw new Exception("Cannot decide on a coordinator");
      }
   }

   private void checkExecutors(Coordinator coordinator, List executorURLs) throws Exception
   {
      for(Iterator i = executorURLs.iterator(); i.hasNext(); )
      {
         String executorURL = (String)i.next();

         try
         {
            log.debug("resetting " + executorURL);
            coordinator.sendToExecutor(executorURL, new ResetRequest());
            log.info("executor " + executorURL + " on-line");
         }
         catch(Throwable e)
         {
            log.error("executor " + executorURL + " failed", e);
            throw new Exception("executor check failed");
         }
      }
   }

   /**
    * @param executorURLs - list to be updated with current run's executor URLs.
    */
   private void prepare(Job j, String providerName, List executorURLs) throws Exception
   {
      Configuration config = runner.getConfiguration();
      Provider provider = config.getProvider(providerName);
      Properties jndiProperties = provider.getJNDIProperties();

      String executorName = j.getExecutorName();
      String executorURL;

      if (executorName == null)
      {
         // use the default executor
         executorURL = config.getDefaultExecutorURL();
      }
      else
      {
         executorURL = provider.getExecutorURL(executorName);
         if (executorURL == null)
         {
            throw new Exception("Provider " + providerName +
               " doesn't know to map executor " + executorName);
         }
      }

      j.setJNDIProperties(jndiProperties);
      j.setExecutorURL(executorURL);

      // update the executor URL list
      if (!executorURLs.contains(executorURL))
      {
         executorURLs.add(executorURL);
      }
   }

   private Result run(Coordinator coordinator, Job job)
   {
      try
      {
         String executorURL = job.getExecutorURL();

         if (executorURL == null)
         {
            throw new Exception("An executorURL must be configured on this job");
         }

         log.debug("sending job " + job + " to " + coordinator);

         Result result = coordinator.sendToExecutor(executorURL, job);
         result.setRequest(job);
         return result;
      }
      catch(Throwable t)
      {
         log.warn("job " + job + " failed: " + t.getMessage());
         log.debug("job " + job + " failed", t);
         return new Failure(job);
      }
   }

   private List runParallel(Coordinator coordinator, JobList parallelJobs)
   {
      List jobExecutors = new ArrayList();

      for(Iterator i = parallelJobs.iterator(); i.hasNext(); )
      {
         JobExecutor je = new JobExecutor(coordinator, (Job)i.next());
         jobExecutors.add(je);
         Thread t = new Thread(je);
         je.setThread(t);
         t.start();
      }

      log.debug("all " + parallelJobs.size() + " jobs fired");

      List results = new ArrayList();

      for(Iterator i = jobExecutors.iterator(); i.hasNext(); )
      {
         JobExecutor je = (JobExecutor)i.next();
         try
         {
            je.getThread().join();
            log.debug("parallel job finished");
         }
         catch (InterruptedException e)
         {}
         results.add(je.getResult());
      }
      return results;
   }


   // Inner classes -------------------------------------------------

   private class JobExecutor implements Runnable
   {
      private Job job;
      private Coordinator coordinator;
      private Result result;
      private Thread thread;

      JobExecutor(Coordinator coordinator, Job job)
      {
         this.coordinator = coordinator;
         this.job = job;
      }

      public void run()
      {
         log.debug("parallel job fired");
         result = PerformanceTest.this.run(coordinator, job);
      }

      private void setThread(Thread thread)
      {
         this.thread = thread;
      }

      private Thread getThread()
      {
         return thread;
      }

      private Result getResult()
      {
         return result;
      }
   }
}
