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
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
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
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
    
    private List<String> artifactsToExclude = new ArrayList<String>();

    @Override
    public void afterProjectsRead( MavenSession session )
        throws MavenExecutionException
    {
        logger.info( "About to change project version in reactor." );

        Map<String, String> gavVersionMap = new HashMap<String, String>();

        for ( MavenProject mavenProject : session.getAllProjects() )
        {
            if ( !artifactsToExclude.contains( mavenProject.getArtifactId() ) )
            {
                // Lookup this plugin's configuration
                Plugin plugin = mavenProject.getPlugin( "org.apache.maven.plugins:maven-external-version-plugin" );

                
                // now we are going to wedge in the config
                Xpp3Dom configDom = (Xpp3Dom) plugin.getConfiguration();
    
                artifactsToExclude = listOfArtifactsToExclude( configDom );

                    
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
                    updateInstallPlugin( mavenProject, oldVersion, newVersion );
                    updateDependencyPlugin( mavenProject, oldVersion, newVersion );
                    updateDeployPlugin( mavenProject, oldVersion, newVersion );
                    // TODO: get the unfiltered string, and re-filter it with new version.
                    String oldFinalName = mavenProject.getBuild().getFinalName();
                    String newFinalName = oldFinalName.replaceFirst( Pattern.quote( oldVersion ), newVersion );
                    logger.info( "Updating project.build.finalName: " + newFinalName );
                    mavenProject.getBuild().setFinalName( newFinalName );
                    gavVersionMap.put( 
                        buildGavKey( mavenProject.getGroupId(), mavenProject.getArtifactId(), oldVersion ),
                                       newVersion );
                   
                    updateDependencyArtifacts( gavVersionMap, mavenProject, oldVersion, newVersion );
                    logger.debug(
                        "new version added to map: " 
                    + buildGavKey( mavenProject.getGroupId(), mavenProject.getArtifactId(),
                                                                    oldVersion ) + ": " + newVersion );
    
                    if ( mavenProject.getParent() != null )
                    {
                        logger.info( "My parent is: " + buildGavKey( mavenProject.getParent() ) );
                    }
    
    
                }
                catch ( ExternalVersionException e )
                {
                    throw new MavenExecutionException( e.getMessage(), e );
                }
            }
        }

        // now we have only updated the versions of the projects, we need to update
        // the references between the updated projects

        for ( MavenProject mavenProject : session.getAllProjects() )
        {
            if ( ! artifactsToExclude.contains( mavenProject.getArtifactId() ) )
            {
                try
                {
    
                    if ( mavenProject.getParent() != null )
                    {
                        logger.info( "looking for parent in map" );
    
                        if ( gavVersionMap.containsKey( buildGavKey( mavenProject.getParent() ) ) )
                        {
                            // we need to update the parent
                            logger.info( "WE NEED TO ACT NOW!" );
                        }
                    }
    
                    // write processed new pom out
                    createNewVersionPom( mavenProject, gavVersionMap );
                }
                catch ( XmlPullParserException e )
                {
                    throw new MavenExecutionException( e.getMessage(), e );
                }
                catch ( IOException e )
                {
                    throw new MavenExecutionException( e.getMessage(), e );
                }
            }
        }

    }

    private void updateDependencyArtifacts( Map<String, String> gavVersionMap, MavenProject mavenProject, 
        String oldVersion, String newVersion ) 
    {
        if ( ! artifactsToExclude.contains(  mavenProject.getArtifactId() ) )
        {
            List<Dependency> dependencies = mavenProject.getDependencies();
            logger.debug( " Before updating the GAV " + mavenProject.getArtifactId() + " : " 
            + mavenProject.getDependencies() );
            for ( Dependency dependency : dependencies ) 
            {
                String buildGavKey = buildGavKey( dependency );
                if ( !gavVersionMap.containsKey( buildGavKey  ) 
                    && ! artifactsToExclude.contains( dependency.getArtifactId() )
                    && dependency.getVersion().equalsIgnoreCase( oldVersion ) )
                {
                    gavVersionMap.put( buildGavKey, newVersion );
                }
            }
        }
    }

    private void updateDependencyPlugin( MavenProject mavenProject, String oldVersion, String newVersion ) 
    {
        Plugin dependencyPlugin = mavenProject.getPlugin( "org.apache.maven.plugins:maven-dependency-plugin" );
        if ( null != dependencyPlugin && !dependencyPlugin.getExecutions().isEmpty() )
        {
            List<PluginExecution> executions = dependencyPlugin.getExecutions();
            for ( PluginExecution pluginExecution : executions ) 
            {
                if ( null != pluginExecution.getConfiguration() 
                    && pluginExecution.getConfiguration() instanceof Xpp3Dom )
                {
                    Xpp3Dom dom = (Xpp3Dom) pluginExecution.getConfiguration();
                    Xpp3Dom artifactItems = dom.getChild( "artifactItems" );
                    if ( artifactItems.getChildCount() > 0 )
                    {
                        if ( artifactItems.getChildCount() > 0 )
                        {
                            for ( int i = 0; i < artifactItems.getChildCount(); i++ ) 
                            {
                                if ( artifactItems.getChild( i ).getChild( "version" ).
                                    getValue().equalsIgnoreCase( oldVersion ) )
                                {
                                    artifactItems.getChild( i ).getChild( "version" ).setValue( newVersion );
                                }
                            }
                        }
                    }
                    
                }
            }
        }
    }
    
    private void updateDeployPlugin( MavenProject mavenProject, String oldVersion, String newVersion ) 
    {
        Plugin deployPlugin = mavenProject.getPlugin( "org.apache.maven.plugins:maven-deploy-plugin" );
        if ( null != deployPlugin )
        {
            List<PluginExecution> executions = deployPlugin.getExecutions();
            for ( PluginExecution pluginExecution : executions ) 
            {
                if ( null != pluginExecution.getConfiguration() 
                    && pluginExecution.getConfiguration() instanceof Xpp3Dom )
                {
                    Xpp3Dom dom = (Xpp3Dom) pluginExecution.getConfiguration();
                    if ( null != dom.getChild( "version" ) )
                    {
                        if ( dom.getChild( "version" ).getValue().equalsIgnoreCase( oldVersion ) )
                        {
                            dom.getChild( "version" ).setValue( newVersion );
                        }
                    }
                }
            }
            if ( null != deployPlugin.getConfiguration() 
                && deployPlugin.getConfiguration() instanceof Xpp3Dom )
            {
                Xpp3Dom dom = (Xpp3Dom) deployPlugin.getConfiguration();
                if ( null != dom.getChild( "version" ) )
                {
                    if ( dom.getChild( "version" ).getValue().equalsIgnoreCase( oldVersion ) )
                    {
                        dom.getChild( "version" ).setValue( newVersion );
                    }
                }
            }
        }
    }
    
    private void updateInstallPlugin( MavenProject mavenProject, String oldVersion, String newVersion ) 
    {
        Plugin installPlugin = mavenProject.getPlugin( "org.apache.maven.plugins:maven-install-plugin" );
        if ( null != installPlugin && !installPlugin.getExecutions().isEmpty() )
        {
            List<PluginExecution> executions = installPlugin.getExecutions();
            for ( PluginExecution pluginExecution : executions ) 
            {
                Object configuration = pluginExecution.getConfiguration();
                if ( configuration instanceof Xpp3Dom )
                {
                    Xpp3Dom dom = (Xpp3Dom) configuration;
                    Xpp3Dom version = dom.getChild( "version" );
                    if ( null != version &&  version.getValue().equalsIgnoreCase( oldVersion ) )
                    {
                        version.setValue( newVersion );
                    }
                    Xpp3Dom file = dom.getChild( "file" );
                    if ( null != version &&  file.getValue().contains(  oldVersion ) )
                    {
                        file.setValue( file.getValue().replaceAll( oldVersion, newVersion ) );
                    }
                }
            }
        }
    }

    private String buildGavKey( MavenProject mavenProject )
    {
        return buildGavKey( mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion() );
    }
    
    private String buildGavKey( Dependency dependency )
    {
        return buildGavKey( dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion() );
    }

    private String buildGavKey( MavenProject mavenProject, String oldVersion )
    {
        return buildGavKey( mavenProject.getGroupId(), mavenProject.getArtifactId(), oldVersion );
    }

    private String buildGavKey( String groupId, String artifactId, String oldVersion )
    {
        return new StringBuilder( groupId ).append( ":" ).append( artifactId ).append( ":" ).append(
            oldVersion ).toString();
    }

    private void createNewVersionPom( MavenProject mavenProject, Map<String, String> gavVersionMap )
        throws IOException, XmlPullParserException
    {
        Reader fileReader = null;
        Writer fileWriter = null;
        try
        {
            fileReader = new FileReader( mavenProject.getFile() );
            Model model = new MavenXpp3Reader().read( fileReader );
            model.setVersion( mavenProject.getVersion() );


            // TODO: this needs to be restructured when other references are updated (dependencies, dep-management, plugins, etc)
            if ( model.getParent() != null && ! artifactsToExclude.contains( model.getParent().getArtifactId() ) )
            {
                 model.getParent().setVersion( mavenProject.getVersion() );
            }
            
            Plugin plugin = mavenProject.getPlugin( "org.apache.maven.plugins:maven-external-version-plugin" );
            // now we are going to wedge in the config
            Xpp3Dom pluginConfiguration = (Xpp3Dom) plugin.getConfiguration();
            List<String> propertiesToUpdate = listOfPropertiesToChange( pluginConfiguration ) ;
            Properties properties =  model.getProperties();
            
            Enumeration<?> e = properties.propertyNames();

            while ( e.hasMoreElements() ) 
            {
              String key = (String) e.nextElement();
              if ( propertiesToUpdate.contains( key ) )
              {
                  properties.put( key, mavenProject.getVersion() );
              }
           }

            File newPom = createFileFromConfiguration( mavenProject, pluginConfiguration ); 
            logger.debug( ExternalVersionExtension.class.getSimpleName() + ": using new pom file => " + newPom );
            fileWriter = new FileWriter( newPom );
            new MavenXpp3Writer().write( fileWriter, model );

            mavenProject.setFile( newPom );
            List<Dependency> dependencies = mavenProject.getDependencies();
            logger.debug( " Before updating the dependency " + mavenProject.getArtifactId() + " : " 
            + mavenProject.getDependencies() );
            for ( Dependency dependency : dependencies ) 
            {
                String buildGavKey = buildGavKey( dependency );
                if ( gavVersionMap.containsKey( buildGavKey  ) )
                {
                    dependency.setVersion( gavVersionMap.get( buildGavKey ) );
                }
            }
            logger.debug( " Updated the dependency " + mavenProject.getArtifactId() + " : " 
            + mavenProject.getDependencies() );
        }
        finally
        {
            IOUtil.close( fileReader );
            IOUtil.close( fileWriter );
        }


    }
    
    private List<String> listOfPropertiesToChange( Xpp3Dom pluginConfiguration )
    {
        List<String> propertyNames = new ArrayList<String>();
        Xpp3Dom values = pluginConfiguration.getChild( "propertiesToReplace" );
        Xpp3Dom property[] = values.getChildren();        
        if ( null != property && property.length > 0 )
        {
            for ( Xpp3Dom xpp3Dom : property ) 
            {
                propertyNames.add( xpp3Dom.getValue() );
            }
        }
        return propertyNames;
    }
    
    private List<String> listOfArtifactsToExclude( Xpp3Dom pluginConfiguration )
    {
        List<String> listOfArtifactsToExclude = new ArrayList<String>();
        Xpp3Dom values = pluginConfiguration.getChild( "artifactIdToExclude" );
        if ( null != values )
        {
            String property[] = values.getValue().split( "," );        
            if ( null != property && property.length > 0 )
            {
                for ( String xpp3Dom : property ) 
                {
                    listOfArtifactsToExclude.add( xpp3Dom );
                }
            }
        }
        return listOfArtifactsToExclude;
    }
    

    private File createFileFromConfiguration( MavenProject mavenProject, Xpp3Dom pluginConfig ) throws IOException
    {
        boolean deleteTemporaryFile = shouldDeleteTemporaryFile( pluginConfig ); 
        boolean generateTemporaryFile = shouldGenerateTemporaryFile( pluginConfig ); 

        // let's keep the default file as a default
        File f = new File( mavenProject.getBasedir(), "pom.xml.new-version" );
        
        if ( generateTemporaryFile ) 
        {
            f = File.createTempFile( "pom", ".maven-external-version" );
        }
        
        if ( deleteTemporaryFile ) 
        {
            f.deleteOnExit();
        }
        return f;
    }

    /*
     * Looks for generateTemporaryFile child configuration node.
     * If not present then no deletion occurs, otherwise return true if value is true, false otherwise
     */
    private boolean shouldGenerateTemporaryFile( Xpp3Dom pluginConfiguration ) 
    {
        return evaluateBooleanNodeInConfiguration( pluginConfiguration, "generateTemporaryFile" );
    }

    /*
     * Looks for deleteTemporaryFile child configuration node.
     * If not present then no deletion occurs, otherwise return true if value is true, false otherwise
     */
    private boolean shouldDeleteTemporaryFile( Xpp3Dom pluginConfiguration ) 
    {
        return evaluateBooleanNodeInConfiguration( pluginConfiguration, "deleteTemporaryFile" );
    }

    private boolean evaluateBooleanNodeInConfiguration( Xpp3Dom pluginConfiguration, String nodeName )
    {
        Xpp3Dom n = pluginConfiguration.getChild( nodeName );
        return n != null && Boolean.parseBoolean( n.getValue() );
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
