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
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Strategy which reads a version string from a 'VERSION' file which  contains a single version string such as '1.2.3'.
 *
 * @author <a href="mailto:bdemers@apache.org">Brian Demers</a>
 */
@Component( role = ExternalVersionStrategy.class, hint = "file" )
public class FileStrategy
    implements ExternalVersionStrategy
{
    @Configuration( "VERSION" )
    private String versionFilePath = "VERSION";

    @Override
    public String getVersion( MavenProject mavenProject )
        throws ExternalVersionException
    {
        String versionString = null;

        File versionFile = new File( mavenProject.getBasedir(), versionFilePath );
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader( new FileReader( versionFile ) );
            // just return the first line of the file, any other format is NOT supported.
            versionString = reader.readLine();
        }
        catch ( IOException e )
        {
            throw new ExternalVersionException( "Failed to read version file: [" + versionFile.getAbsolutePath() + "]",
                                                e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        return versionString;
    }
}
