Maven External Version
-----------------------

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


