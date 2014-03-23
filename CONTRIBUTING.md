## How to contribute and perform a release

* If the next release does not have a branch, then
    * create a new branch after the release number, e.g. 0.2.2
    * increment the release number in [build.sbt](https://github.com/guardian/fastly-api-client/blob/master/build.sbt)
* Create your own branch based of the next release's branch, e.g. *git checkout 0.2.2; git checkout -b 0.2.2-my-feature*
* Raise a pull request

## Testing

You can test the new release locally with,

```
    sbt clean
    sbt publishLocal
```

## Publishing to maven

Development and testing should be against a snapshot

To publish to [maven](http://search.maven.org/#browse|948553587) with the following (you will need a key and the password),

```
    sbt publishSigned
```

* Log into [sonatype](https://oss.sonatype.org/index.html)
* [Close and then release](https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide#SonatypeOSSMavenRepositoryUsageGuide-8a.ReleaseIt) the new version
* Merge the release branch into master
* Create the next release branch, e.g. 0.2.3
* Create a [release on github](https://github.com/guardian/fastly-api-client/releases)

## Further notes on releasing to maven
https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide

