package org.apache.maven.version.strategy;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.project.MavenProject;

/**
 * Simple strategy for resolving a version.
 *
 * @author <a href="mailto:bdemers@apache.org">Brian Demers</a>
 */
public interface ExternalVersionStrategy
{
    /**
     * Returns a new version based on some other source.
     *
     * @param mavenProject project which will be updated.
     * @return a new String version.
     * @throws ExternalVersionException thrown if there is any problems loading the new version.
     */
    String getVersion( MavenProject mavenProject )
        throws ExternalVersionException;
}
