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

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.version.strategy.ExternalVersionException;
import org.apache.maven.version.strategy.ExternalVersionStrategy;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.DefaultExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Maven Extension that will update all the projects in the reactor with an externally managed version.
 * <p/>
 * This extension MUST be configured as a plugin in order to be configured.
 * <p/>
 * <plugin>
 * <groupId>org.apache.maven.plugins</groupId>
 * <artifactId>maven-external-version-plugin</artifactId>
 * <extensions>true</extensions>
 * <configuration>
 * <strategy hint="file">
 * <versionFilePath>VERSION</versionFilePath>
 * </strategy>
 * </configuration>
 * </plugin>
 * <p/>
 * 'strategy' - The configuration for an ExternalVersionStrategy.
 * 'hint' -  A component hint to load the ExternalVersionStrategy.
 *
 * @author <a href="mailto:bdemers@apache.org">Brian Demers</a>
 */
@Component( role = AbstractMavenLifecycleParticipant.class, hint = "external-version" )
public class ExternalVersionExtension
    extends AbstractMavenLifecycleParticipant
{

    @Requirement
    private Logger logger;

    @Requirement
    private PlexusContainer container;

    @Override
    public void afterProjectsRead( MavenSession session )
        throws MavenExecutionException
    {
        logger.info( "About to change project version in reactor." );

        for ( MavenProject mavenProject : session.getAllProjects() )
        {
            // Lookup this plugin's configuration
            Plugin plugin = mavenProject.getPlugin( "org.apache.maven.plugins:maven-external-version-plugin" );

            // now we are going to wedge in the config
            Xpp3Dom configDom = (Xpp3Dom) plugin.getConfiguration();

            ExternalVersionStrategy strategy = getStrategy( configDom, mavenProject.getFile() );

            // grab the old version before changing it
            String oldVersion = mavenProject.getVersion();

            try
            {
                // now use the strategy to figure out the new version
                String newVersion = getNewVersion( strategy, mavenProject );

                // now that we have the new version update the project.
                mavenProject.setVersion( newVersion );
                mavenProject.getArtifact().setVersion( newVersion );

                // TODO: get the unfiltered string, and re-filter it with new version.
                String oldFinalName = mavenProject.getBuild().getFinalName();
                String newFinalName = oldFinalName.replaceFirst( Pattern.quote( oldVersion ), newVersion );
                logger.info( "Updating project.build.finalName: " + newFinalName );
                mavenProject.getBuild().setFinalName( newFinalName );
            }
            catch ( ExternalVersionException e )
            {
                throw new MavenExecutionException( e.getMessage(), e );
            }
        }
    }

    private String getNewVersion( ExternalVersionStrategy strategy, MavenProject mavenProject )
        throws ExternalVersionException
    {

        // snapshot detection against the old version.
        boolean isSnapshot = ArtifactUtils.isSnapshot( mavenProject.getVersion() );

        // lookup the new version
        String newVersion = strategy.getVersion( mavenProject );

        if ( newVersion != null )
        {
            newVersion = newVersion.trim();
        }

        boolean isNewSnapshot = ArtifactUtils.isSnapshot( newVersion );
        // make sure we still have a SNAPSHOT if we had one previously.
        if ( isSnapshot && !isNewSnapshot )
        {
            newVersion = newVersion + "-SNAPSHOT";
        }
        return newVersion;
    }

    private ExternalVersionStrategy getStrategy( Xpp3Dom configDom, File pomFile )
        throws MavenExecutionException
    {
        // get the strategy from the config
        Xpp3Dom strategyNode = configDom.getChild( "strategy" );
        if ( strategyNode == null )
        {
            throw new MavenExecutionException( "Missing configuration, 'strategy' is required. ", pomFile );
        }

        String hint = strategyNode.getAttribute( "hint" );
        if ( hint == null )
        {
            throw new MavenExecutionException( "Missing configuration, '<strategy hint=\"HINT\">' is required. ",
                                               pomFile );
        }

        try
        {
            ExternalVersionStrategy strategy = container.lookup( ExternalVersionStrategy.class, hint );
            logger.info( "component: " + strategy );

            ComponentConfigurator configurator = container.lookup( ComponentConfigurator.class, "basic" );
            configurator.configureComponent( strategy, new XmlPlexusConfiguration( strategyNode ),
                                             new DefaultExpressionEvaluator(), null, null );

            return strategy;

        }
        catch ( ComponentLookupException e )
        {
            throw new MavenExecutionException( e.getMessage(), e );
        }
        catch ( ComponentConfigurationException e )
        {
            throw new MavenExecutionException( e.getMessage(), e );
        }
    }

}
