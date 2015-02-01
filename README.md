Maven External Version
-----------------------

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


Need to add real example here, when I'm more awake.


TODO:
-----

* Test with multi-module
* Add unit tests (Initial implementation was a bit exploratory hacking, hence why this is temporarily on github)
* Finalize strategy API
* Add APT for doc/site
* find out if anyone else cares about this.

Other Thoughts
---------------

An extension isn't exactly the most flexible way of adding this feature to Maven, but it seems to work (at least with
simple cases)


