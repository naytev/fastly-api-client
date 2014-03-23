Fastly API client
=================

An asynchronous Scala client for [Fastly's API](http://docs.fastly.com/api) used to deploy and update configs, decache objects and query the stats API.

[Released to maven central](http://search.maven.org/#browse|948553587)

[Release notes](https://github.com/guardian/fastly-api-client/releases)

## Installation

### SBT

    libraryDependencies += "com.gu" %% "fastly-api-client" % "0.3.0"

### Maven
   
    <dependency>
        <groupId>com.gu</groupId>
        <artifactId>fastly-api-client_2.10</artifactId>
        <version>0.3.0</version>
    </dependency>


## Configuring the client

This client uses the [Dispatch Core](http://dispatch.databinder.net/Dispatch.html).

Use the defaults,

    val client = FastlyApiClient("my-fastly-api-key", "my-service-id")

Or define your own AsyncHttpClientConfig which will be handed to the underlying Dispatch implementation,

    val client = FastlyApiClient("my-fastly-api-key",
                    "my-service-id",
                    config = Some(asyncHttpClientConfig))


## Asynchronous calls

All methods return a scala.concurrent.Future[Response]

## Examples

### Purging

    client.purge(url)


### Deploying

This is the way Fastly recommend performing releases.

    client.versionList(...) // find the active version
    client.versionClone(...) // clone the active version
    client.vclDelete(...) // delete all the VCL files ready for the new ones
    client.vclUpload(...) // upload you new VCL files
    client.vclSetAsMain(...) // define the main VCL file
    client.versionValidate(...) // validate the cloned version
    client.versionActivate(...) // activate the cloned version


### Datacenter stats

    client.stats(startDatetime, endDatetime, By.minute)
    client.stats(startDatetime, endDatetime, By.hour, region = Region.usa)
    client.stats(startDatetime,
                        endDatetime,
                        By.day,
                        region = Region.all)
