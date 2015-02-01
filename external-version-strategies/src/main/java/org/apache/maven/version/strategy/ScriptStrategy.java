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
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Executes a script or executable to resolve the version.
 *
 * @author <a href="mailto:bdemers@apache.org">Brian Demers</a>
 */
@Component( role = ExternalVersionStrategy.class, hint = "script" )
public class ScriptStrategy
    implements ExternalVersionStrategy
{

    @Configuration( "./version.sh" )
    private String script;

    @Requirement
    private Logger log;

    @Override
    public String getVersion( MavenProject mavenProject )
        throws ExternalVersionException
    {
        ProcessBuilder ps = new ProcessBuilder( script );
        ps.redirectErrorStream( true );
        BufferedReader reader = null;
        try
        {
            Process pr = ps.start();
            reader = new BufferedReader( new InputStreamReader( pr.getInputStream() ) );

            // we are only interested in the first line
            String versionString = reader.readLine();
            pr.waitFor();

            if ( pr.exitValue() != 0 )
            {
                log.error( "Execution Exit Code: " + pr.exitValue() );
                throw new ExternalVersionException( "The script exit status: " + pr.exitValue() );
            }
            return versionString;
        }
        catch ( IOException e )
        {
            throw new ExternalVersionException( "Failed to execute script: " + e.getMessage(), e );
        }
        catch ( InterruptedException e )
        {
            throw new ExternalVersionException( "Failed to execute script: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }
}
