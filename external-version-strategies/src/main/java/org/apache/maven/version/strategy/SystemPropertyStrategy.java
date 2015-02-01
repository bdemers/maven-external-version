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
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;

/**
 * Loads version from the System Property 'external.version'.
 *
 * @author <a href="mailto:bdemers@apache.org">Brian Demers</a>
 */
@Component( role = ExternalVersionStrategy.class, hint = "sysprop" )
public class SystemPropertyStrategy
    implements ExternalVersionStrategy
{
    private static final String EXTERNAL_VERSION = "external.version";

    private static final String EXTERNAL_VERSION_QUALIFIER = "external.version-qualifier";

    @Override
    public String getVersion( MavenProject mavenProject )
        throws ExternalVersionException
    {
        String newVersion = System.getProperty( EXTERNAL_VERSION, mavenProject.getVersion() );
        String qualifier = StringUtils.trim( System.getProperty( EXTERNAL_VERSION_QUALIFIER ) );

        if ( StringUtils.isNotBlank( qualifier ) )
        {
            // TODO: this needs to be cleaned up, the calling method will re-add the -SNAPSHOT if needed, but this is dirty
            newVersion = newVersion.replaceFirst( "-SNAPSHOT", "" ) + "-" + qualifier;
        }

        return newVersion;
    }
}
