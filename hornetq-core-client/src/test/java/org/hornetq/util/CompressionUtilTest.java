/*
 * Copyright 2010 Red Hat, Inc.
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

package org.hornetq.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;

import junit.framework.TestCase;

import org.hornetq.utils.DeflaterReader;
import org.hornetq.utils.InflaterReader;
import org.hornetq.utils.InflaterWriter;

/**
 * A CompressionUtilTest
 *
 * @author <a href="mailto:hgao@redhat.com">Howard Gao</a>
 *
 */
public class CompressionUtilTest extends TestCase
{

   public void testDeflaterReader() throws Exception
   {
      String inputString = "blahblahblah??blahblahblahblahblah??blablahblah??blablahblah??bla";
      byte[] input = inputString.getBytes("UTF-8");

      ByteArrayInputStream inputStream = new ByteArrayInputStream(input);

      AtomicLong counter = new AtomicLong(0);
      DeflaterReader reader = new DeflaterReader(inputStream, counter);

      ArrayList<Integer> zipHolder = new ArrayList<Integer>();
      int b = reader.read();

      while (b != -1)
      {
         zipHolder.add(b);
         b = reader.read();
      }

      assertEquals(input.length, counter.get());

      byte[] allCompressed = new byte[zipHolder.size()];
      for (int i = 0; i < allCompressed.length; i++)
      {
         allCompressed[i] = (byte) zipHolder.get(i).intValue();
      }

      byte[] output = new byte[30];
      Deflater compresser = new Deflater();
      compresser.setInput(input);
      compresser.finish();
      int compressedDataLength = compresser.deflate(output);

      compareByteArray(allCompressed, output, compressedDataLength);
   }

   public void testDeflaterReader2() throws Exception
   {
      String inputString = "blahblahblah??blahblahblahblahblah??blablahblah??blablahblah??bla";
      byte[] input = inputString.getBytes("UTF-8");

      ByteArrayInputStream inputStream = new ByteArrayInputStream(input);
      AtomicLong counter = new AtomicLong(0);

      DeflaterReader reader = new DeflaterReader(inputStream, counter);

      byte[] buffer = new byte[7];
      ArrayList<Integer> zipHolder = new ArrayList<Integer>();

      int n = reader.read(buffer);
      while (n != -1)
      {
         for (int i = 0; i < n; i++)
         {
            zipHolder.add((int)buffer[i]);
         }
         n = reader.read(buffer);
      }

      assertEquals(input.length, counter.get());

      byte[] allCompressed = new byte[zipHolder.size()];
      for (int i = 0; i < allCompressed.length; i++)
      {
         allCompressed[i] = (byte) zipHolder.get(i).intValue();
      }

      byte[] output = new byte[30];
      Deflater compresser = new Deflater();
      compresser.setInput(input);
      compresser.finish();
      int compressedDataLength = compresser.deflate(output);

      compareByteArray(allCompressed, output, compressedDataLength);
   }

   public void testInflaterReader() throws Exception
   {
      String inputString = "blahblahblah??blahblahblahblahblah??blablahblah??blablahblah??bla";
      byte[] input = inputString.getBytes("UTF-8");
      byte[] output = new byte[30];
      Deflater compresser = new Deflater();
      compresser.setInput(input);
      compresser.finish();
      int compressedDataLength = compresser.deflate(output);

      byte[] zipBytes = new byte[compressedDataLength];

      System.arraycopy(output, 0, zipBytes, 0, compressedDataLength);
      ByteArrayInputStream byteInput = new ByteArrayInputStream(zipBytes);

      InflaterReader inflater = new InflaterReader(byteInput);
      ArrayList<Integer> holder = new ArrayList<Integer>();
      int read = inflater.read();

      while (read != -1)
      {
         holder.add(read);
         read = inflater.read();
      }

      byte[] result = new byte[holder.size()];

      for (int i = 0; i < result.length; i++)
      {
         result[i] = holder.get(i).byteValue();
      }

      String txt = new String(result);

      assertEquals(inputString, txt);

   }

   public void testInflaterWriter() throws Exception
   {
      String inputString = "blahblahblah??blahblahblahblahblah??blablahblah??blablahblah??bla";
      byte[] input = inputString.getBytes("UTF-8");
      byte[] output = new byte[30];
      Deflater compresser = new Deflater();
      compresser.setInput(input);
      compresser.finish();
      int compressedDataLength = compresser.deflate(output);

      byte[] zipBytes = new byte[compressedDataLength];

      System.arraycopy(output, 0, zipBytes, 0, compressedDataLength);
      ByteArrayInputStream byteInput = new ByteArrayInputStream(zipBytes);

      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      InflaterWriter writer = new InflaterWriter(byteOutput);

      byte[] zipBuffer = new byte[12];

      int n = byteInput.read(zipBuffer);
      while (n > 0)
      {
         writer.write(zipBuffer, 0, n);
         n = byteInput.read(zipBuffer);
      }

      writer.close();

      byte[] outcome = byteOutput.toByteArray();
      String outStr = new String(outcome);

      assertEquals(inputString, outStr);
   }

   private void compareByteArray(byte[] first, byte[] second, int length)
   {
      for (int i = 0; i < length; i++)
      {
         assertEquals(first[i], second[i]);
      }
   }
}
