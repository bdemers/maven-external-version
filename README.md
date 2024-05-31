Maven External Version
-----------------------

> [!NOTE]
> This usecase is now part of Maven 4.  Please try Maven 4 and provide [feedback to the Maven team](https://maven.apache.org/mailing-lists.html)!
>
> [![The Current State of Apache Maven 4 - Karl Heinz Marbaise](https://img.youtube.com/vi/tAGv4QH29QU/0.jpg)](https://www.youtube.com/watch?v=tAGv4QH29QU&t=1112s)

Archived Content
---

Requires Maven 3.2.0 or later.

(github is the temporary home.)

What is this?
--------------

The main use-case of this extension plugin is to allow the POM versions to be updated during a build and NOT require
the pom.xml file to be modified (causing potential merge conflicts down the road, or untracked changes)

When would I use this?
-----------------------

Say, you have github-flow approach to branching, that is you create a lot of small feature branches, they get merged
into 'master' then you release off of master.  Currently if you want to build multiple SNAPSHOT branches in CI and
and deploy them to a Maven repository, you need to update your POMs before you deploy.  This will help you with that.

For example if your `master` is at version `1.1.42-SNAPSHOT`, and for your feature branches you want to add your
branch/feature name to the version, e.g. for branch `everything`, you want to end up with `1.1.42-everything-SNAPSHOT`


Need to add real example here, when I'm more awake. Until then, look at [this](https://github.com/bdemers/maven-external-version/blob/master/maven-external-version-plugin/src/it/simple-module/pom.xml#L54-L68).

Quick and Dirty Example
------------------------


In your pom add:

```xml

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-external-version-plugin</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <extensions>true</extensions>
        <configuration>
            <strategy hint="sysprop"/>
        </configuration>
    </plugin>

```

To replaced the whole version you can run:

```
mvn install -Dexternal.version=1.1.42
```

To add just a qualifier to the version:

```
mvn install -Dexternal.version-qualifier=rc1
# if the original version was 1.1.1 the new version would be 1.1.1-rc1
```

Add a version qualifier to all non-master branches

```
mvn install -Dexternal.version-qualifier=$(git symbolic-ref --short HEAD| sed s_^master\$__)
```

Or how about a short git hash?

```
mvn install -Dexternal.version-qualifier=$(git rev-parse --short HEAD)
```

Configuration & parameters
-----

```xml

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-external-version-plugin</artifactId>
        <version>X.Y.Z</version>
        <extensions>true</extensions>
        <configuration>
            <strategy hint="STRATEGY"/>
            <generateTemporaryFile>true/false</generateTemporaryFile>
            <deleteTemporaryFile>true/false</deleteTemporaryFile>
        </configuration>
    </plugin>

```

- `strategy#hint` key defining which strategy implementation will be used, one of
  - file: read the version from the first line of a given file
  - script: version is given by the first line of the output execution of a given command
  - sysprop: allows to define project version & qualifier from system properties
- `deleteTemporaryFile` will the generated pom files created by this extension will be deleted after execution. Set this parameter to _true_ to activate automatic deletion. Value is optional and defaults to _false_ 
- `generateTemporaryFile` if _true_ generated pom files will be created as temporary files inside the directory pointed by system property `java.io.tmpdir`. If omitted it defaults to  _false_. When false, a file called `pom.xml.new-version` will be generated in the root project directory. 
  
## Strategy : file

This strategy reads the first line of a given file to extract the version to use. 

### Usage

```xml

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-external-version-plugin</artifactId>
        <version>X.Y.Z</version>
        <extensions>true</extensions>
        <configuration>
            <strategy hint="file">
                <versionFilePath>SOME_FILE</versionFilePath>
            </strategy>
        </configuration>
    </plugin>

```

### Parameters

- `versionFilePath`: denotes the file which first line will be read to extract the version from. Can be a fully qualified path or a path relative to the project directory. The parameter is optional, it defaults to `VERSION`, meaning that if not provided, a file called `VERSION` will be read from the project root. 

## Strategy : script

This strategy allows to execute a given command ; the first line of stdout output will be used as version. 

### Usage

```xml

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-external-version-plugin</artifactId>
        <version>X.Y.Z</version>
        <extensions>true</extensions>
        <configuration>
            <strategy hint="script">
                <script>SOME_COMMAND</script>
            </strategy>
        </configuration>
    </plugin>

```

### Parameters

- `script`: a command to execute. The parameter is optional and defaults to `./version.sh`, meaning that if not provided a file called `version.sh` in the project root will be executed. 

## Strategy : sysprop

This strategy uses 2 system properties to define the new project version:

- `external.version`: the main version to use. If omitted, then it defaults to the current `project.version`.
- `external.version-qualifier`: an optional qualifier that will be appended to the given version

### Usage

``` xml

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-external-version-plugin</artifactId>
        <version>X.Y.Z</version>
        <extensions>true</extensions>
        <configuration>
            <strategy hint="sysprop" />
        </configuration>
    </plugin>

```

TODO:
-----

* Add unit tests (Initial implementation was a bit exploratory hacking, hence why this is temporarily on github)
* Finalize strategy API
* Add APT for doc/site
* find out if anyone else cares about this.
* filter new versions into MavenProject and model ( dependency, dependency management, plugin, etc)

Other Thoughts
---------------

An extension isn't exactly the most flexible way of adding this feature to Maven, but it seems to work (at least with
simple cases)


