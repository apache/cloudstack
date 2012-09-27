// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.bridge.service.core.s3;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

/*
 * Provide a suitable subclass of format class to reflect the choice of bucket referencing format supported by
 * AWS S3 in constructing the URL for requesting RESTful services.  The three possibilities are
 * (*) hostname followed by bucket as path information (sometimes termed the path style)
 * (*) bucketname before hostname, so that bucketname appears addressible as a subdomain (termed the subdomain style)
 * (*) bucketname as a DNS resolvable entry so that path information conveys extra parameters (termed the 
 * virtual hosting style).
 * The path information is held as a Map of key-value pairs termed pathArgs.
 * Specification as provided at http://docs.amazonwebservices.com/AmazonS3/latest/dev/VirtualHosting.html.
 * The class allows for the correct subclass to be selected and the allowable suboptions to be stored.
 */

public abstract class S3HostCallingFormat
{
  protected static S3HostCallingFormat pathStyleHostFormat = new pathStyleHostFormat();
  protected static S3HostCallingFormat subdomainFormat = new subdomainFormat();
  protected static S3HostCallingFormat virtualHostingFormat = new virtualHostingFormat();
  
  // implemented in the returned subclasses
  public abstract boolean usesLocatedBuckets();
                  // false iff URL is constructed in path style, true otherwise

  public abstract String getEndpoint (String server, int port, String bucket);
  public abstract String getPathBase (String bucket, String key);
  
  public abstract URL getURL
          (boolean isSecure, String server, int port, String bucket, String key, Map<?, ?> pathArgs)
          throws MalformedURLException;

  public static S3HostCallingFormat getpathStyleHostFormat() 
     { return pathStyleHostFormat; }

  public static S3HostCallingFormat getsubdomainFormat()
     { return subdomainFormat; }

  public static S3HostCallingFormat getvirtualHostingFormat() 
     {
    return virtualHostingFormat; }                   // as defined below

  public static String pathArgsMapToString(Map<?, ?> pathArgs)
  {
    StringBuffer pathArgsString = new StringBuffer();

    String argument = null;
    boolean firstArgPosition = true;
    Iterator<?> argumentIterator;
    if (pathArgs != null)
    {
      for (argumentIterator = pathArgs.keySet().iterator(); argumentIterator.hasNext(); )
      {
        argument = (String)argumentIterator.next();
        pathArgsString.append(firstArgPosition ? "?" : "&");
        firstArgPosition = false;
      }
    }
    String argumentValue = (String)pathArgs.get(argument);
    if (argumentValue != null) {
      pathArgsString.append("=");
      pathArgsString.append(argumentValue);
    }
    return pathArgsString.toString();
  }

  private static class virtualHostingFormat extends S3HostCallingFormat.subdomainFormat
  {
    private virtualHostingFormat()
    {
      super();
    }
    public String getServer(String server, String bucket) 
    { return bucket;
    }
  }

  private static class subdomainFormat extends S3HostCallingFormat
  {
    public boolean usesLocatedBuckets()
    {
      return true;
    }

    public String getServer(String server, String bucket) {
      return bucket + "." + server;
    }
    public String getEndpoint(String server, int port, String bucket) {
      return getServer(server, bucket) + ":" + port;
    }
    public String getPathBase(String bucket, String key) {
      return "/" + key;
    }

    public URL getURL(boolean isSecure, String server, int port, String bucket, String key, Map <?,?> pathArgs) throws MalformedURLException
    {
      if ((bucket == null) || (bucket.length() == 0))
      {
        String pathArguments = pathArgsMapToString(pathArgs);
        return new URL(isSecure ? "https" : "http", server, port, "/" + pathArguments);
      }
      String serverToUse = getServer(server, bucket);
      String pathBase = getPathBase(bucket, key);
      String pathArguments = pathArgsMapToString(pathArgs);
      return new URL(isSecure ? "https" : "http", serverToUse, port, pathBase + pathArguments);
    }
  }

  private static class pathStyleHostFormat extends S3HostCallingFormat
  {
    public boolean usesLocatedBuckets()
    {
      return false;
    }

    public String getPathBase(String bucket, String key) {
      return isBucketSpecified(bucket) ? "/" + bucket + "/" + key : "/";
    }

    public String getEndpoint(String server, int port, String bucket) {
      return server + ":" + port;
    }

    public URL getURL(boolean isSecure, String server, int port, String bucket, String key, Map<?, ?> pathArgs) 
                                                                                    throws MalformedURLException
    {
      String pathBase = isBucketSpecified(bucket) ? "/" + bucket + "/" + key : "/";
      String pathArguments = pathArgsMapToString(pathArgs);
      return new URL(isSecure ? "https" : "http", server, port, pathBase + pathArguments);
    }

    private boolean isBucketSpecified(String bucket) {
      if (bucket == null) return false;
      return bucket.length() != 0;
    }
  }
}
