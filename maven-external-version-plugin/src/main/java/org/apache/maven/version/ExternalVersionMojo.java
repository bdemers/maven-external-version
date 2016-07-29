package org.apache.maven.version;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * External Version extension configuration Mojo.  This mojo is ONLY used to configure the extension.
 *
 * @author <a href="mailto:bdemers@apache.org">Brian Demers</a>
 */
@Mojo( name = "external-version" )
public class ExternalVersionMojo
    extends AbstractMojo
{

    @Component
    private MavenProject project;

    @Parameter( property = "strategy", required = true )
    private String strategy;
    
    @Parameter( property = "external-version.deleteTemporaryFile" , defaultValue = "false" )
    private Boolean deleteTemporaryFile;
    
    @Parameter( property = "external-version.generateTemporaryFile" , defaultValue = "false" )
    private Boolean generateTemporaryFile;

    @Parameter( property = "external-version.propertiesToReplace" , defaultValue = "" )
    private String propertiesToReplace;
    
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "This mojo is used to configure an extension, and should NOT be executed directly." );
    }
}
