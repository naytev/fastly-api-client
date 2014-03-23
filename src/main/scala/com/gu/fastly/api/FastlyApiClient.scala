package com.gu.fastly.api

import scala.concurrent.Future
import org.joda.time.DateTime
import dispatch._
import com.ning.http.client.{AsyncHttpClient, Response, AsyncHttpClientConfig}
import com.ning.http.client.providers.netty.{NettyAsyncHttpProvider, NettyConnectionsPool}
import scala.concurrent.ExecutionContext.Implicits.global

// TODO vclUpdate and vclUpload to take an ExecutionContext!

/** An asynchronous Scala client for Fastly's API used to deploy and update configs, de-cache objects and query the stats API, http://docs.fastly.com/api
  *
  * @constructor creates a new FastlyApiClient.
  * @param apiKey apiKey
  * @param serviceId serviceId
  * @param config a custom AsyncHttpClientConfig for configuring the behaviour (timeouts etc) of the Dispatch HTTP library
  */
case class FastlyApiClient(apiKey: String, serviceId: String, config: Option[AsyncHttpClientConfig] = None) {

  private lazy val fastlyApiUrl = "https://api.fastly.com"
  private lazy val commonHeaders = Map("X-Fastly-Key" -> apiKey, "Accept" -> "application/json")

  sealed trait HttpMethod
  object GET extends HttpMethod
  object POST extends HttpMethod
  object PUT extends HttpMethod
  object DELETE extends HttpMethod

  /** Uploads new vcl files for a given version
    *
    * @param version the version number
    * @param vcl a map of fileName -> vclFileAsString
    */
  def vclUpload(version: Int, vcl: Map[String, String]): Future[List[Response]] = {
    val f = vcl.map {
      case (fileName, fileAsString) => {
        val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/vcl"
        HttpExecutor.execute(
          apiUrl,
          POST,
          headers = Map("Content-Type" -> "application/x-www-form-urlencoded"),
          parameters = Map("content" -> fileAsString, "name" -> fileName, "id" -> fileName)
        )
      }
    }.toList
    Future.sequence(f)
  }

  /** Updates existing vcl files for a given version
    *
    * @param version the version number
    * @param vcl a map of fileName -> vclFileAsString
    */
  def vclUpdate(version: Int, vcl: Map[String, String]): Future[List[Response]] = {
    val f = vcl.map {
      case (fileName, fileAsString) => {
        val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/vcl/$fileName"
        HttpExecutor.execute(
          apiUrl,
          PUT,
          headers = Map("Content-Type" -> "application/x-www-form-urlencoded"),
          parameters = Map("content" -> fileAsString, "name" -> fileName)
        )
      }
    }.toList
    Future.sequence(f)
  }

  /** Purges an object from the cache
    *
    * @param url the url of the object to purge
    */
  def purge(url: String): Future[Response] = {
    val urlWithoutPrefix = url.stripPrefix("http://").stripPrefix("https://")
    val apiUrl = s"$fastlyApiUrl/purge/$urlWithoutPrefix"
    HttpExecutor.execute(apiUrl, POST, headers = Map("X-Fastly-Key" -> apiKey))
  }

  /** Creates a new version */
  def versionCreate(): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version"
    HttpExecutor.execute(apiUrl, PUT)
  }

  /** Lists all versions */
  def versionList(): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version"
    HttpExecutor.execute(apiUrl)
  }

  /** Activates a version
    *
    * @param version the version number
    */
  def versionActivate(version: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/activate"
    HttpExecutor.execute(apiUrl, PUT)
  }

  /** Clones a version in its entirety, e.g. VCL files, backends, logging configs, domains
    *
    * @param version the version number
    */
  def versionClone(version: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/clone"
    HttpExecutor.execute(apiUrl, PUT, headers = Map("Content-Type" -> "application/x-www-form-urlencoded"))
  }

  /** Validates a version
    *
    * @param version the version number
    */
  def versionValidate(version: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/validate"
    HttpExecutor.execute(apiUrl)
  }

  /** Sets a particular VCL file as 'main'
    *
    * @param version the version number
    * @param name the name fof the VCL file to set as 'main'
    */
  def vclSetAsMain(version: Int, name: String): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/vcl/$name/main"
    HttpExecutor.execute(apiUrl, PUT)
  }

  /** Lists all VCL files for a version
    *
    * @param version the version number
    */
  def vclList(version: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/vcl"
    HttpExecutor.execute(apiUrl)
  }

  /** Deletes a VCL files for a version
    *
    * @param version the version number
    * @param names the names of the VCL files to delete
    */
  def vclDelete(version: Int, names: Seq[String]): Future[List[Response]] = {

    val f = names.map { name =>
      val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/vcl/$name"
      HttpExecutor.execute(apiUrl, DELETE)
    }.toList

    Future.sequence(f)
  }

  /** Shows the health of the service's backends
    *
    * @param version the version number
    */
  def backendCheckAll(version: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/backend/check_all"
    HttpExecutor.execute(apiUrl)
  }

  /** Creates a backends
    *
    * @param version the version number
    * @param id an identifier for the backend
    * @param address the domain of the backend
    * @param port the port of the backend
    */
  def backendCreate(version: Int, id: String, address: String, port: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/backend"
    val params = Map("ipv4" -> address, "version" -> version.toString, "id" -> id, "port" -> port.toString, "service" -> serviceId)
    HttpExecutor.execute(apiUrl, POST, parameters = params)
  }

  /** Lists all backends
    *
    * @param version the version number
    */
  def backendList(version: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/backend"
    HttpExecutor.execute(apiUrl)
  }

  /** Lists all services */
  def serviceList(): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service"
    HttpExecutor.execute(apiUrl)
  }

  /** Fetches historical stats for each of your services and groups the results by service id, http://docs.fastly.com/api/stats#Ref
    *
    * @param from
    * @param to
    * @param by
    * @param region
    */
  def stats(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats"
    val params = statsParams(from, to, by, region)
    HttpExecutor.execute(apiUrl, parameters = params)
  }

  /** Fetches the specified field from the historical stats API for each of your services and groups the results by service id, http://docs.fastly.com/api/stats#Ref
    *
    * @param from
    * @param to
    * @param by
    * @param region
    * @param field
    */
  def statsWithFieldFilter(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all, field: String): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/field/$field"
    val params = statsParams(from, to, by, region)
    HttpExecutor.execute(apiUrl, parameters = params)
  }

  /** Fetches historical stats aggregated across all of your services, http://docs.fastly.com/api/stats#Ref
   *
   * @param from
   * @param to
   * @param by
   * @param region
   */
  def statsAggregate(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/aggregate"
    val params = statsParams(from, to, by, region)
    HttpExecutor.execute(apiUrl, parameters = params)
  }

  /** Fetches historical stats for a given service, http://docs.fastly.com/api/stats#Ref
   *
   * @param from
   * @param to
   * @param by
   * @param region
   */
  def statsForService(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all, serviceId: String): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/service/$serviceId"
    val params = statsParams(from, to, by, region)
    HttpExecutor.execute(apiUrl, parameters = params)
  }

  /** Fetches the specified field from the historical stats for a given service, http://docs.fastly.com/api/stats#Ref
   *
   * @param from
   * @param to
   * @param by
   * @param region
   * @param serviceId
   * @param field
   */
  def statsForServiceWithFieldFilter(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all, serviceId: String, field: String): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/service/$serviceId/field/$field"
    val params = statsParams(from, to, by, region)
    HttpExecutor.execute(apiUrl, parameters = params)
  }

  /** Returns usage information aggregated across all Fastly services and grouped by region, http://docs.fastly.com/api/stats#Ref
   *
   * @param from
   * @param to
   * @param by
   * @param region
   */
  def statsUsage(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/usage"
    val params = statsParams(from, to, by, region)
    HttpExecutor.execute(apiUrl, parameters = params)
  }

  /** Returns usage information aggregated by service and grouped by service and region, http://docs.fastly.com/api/stats#Ref
   *
   * @param from
   * @param to
   * @param by
   * @param region
   */
  def statsUsageGroupedByService(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/usage_by_service"
    val params = statsParams(from, to, by, region)
    HttpExecutor.execute(apiUrl, parameters = params)
  }

  /** Fetches the list of codes for regions, http://docs.fastly.com/api/stats#Ref */
  def statsRegions(): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/regions"
    HttpExecutor.execute(apiUrl)
  }

  private def statsParams(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all): Map[String, String] = {
    def millis(date: DateTime): String = (date.getMillis / 1000).toString
    Map[String, String]("from" -> millis(from), "to" -> millis(to), "by" -> by.toString, "region" -> region.toString)
  }

  def closeConnectionPool() = HttpExecutor.close()

  private object HttpExecutor {

    private lazy val Http = {
      val conf = config getOrElse new AsyncHttpClientConfig.Builder()
        .setAllowPoolingConnection(true)
        .setMaximumConnectionsTotal(50)
        .setMaxRequestRetry(3)
        .setRequestTimeoutInMs(20000)
        .build()

      val connectionPool = new NettyConnectionsPool(new NettyAsyncHttpProvider(conf))
      val client = new AsyncHttpClient(new AsyncHttpClientConfig.Builder(conf).setConnectionsPool(connectionPool).build)
      dispatch.Http(client)
    }

    def close() = Http.client.close()

    def execute(apiUrl: String,
                method: HttpMethod = GET,
                headers: Map[String, String] = commonHeaders,
                parameters: Map[String, String] = Map()): Future[Response] = {
      val withHeaders = headers.foldLeft(url(apiUrl)) {
        case (url, (k, v)) => url.addHeader(k, v)
      }

      val withParameters = if (method == GET) {
        withHeaders <<? parameters
      } else parameters.foldLeft(withHeaders) {
        case (req, (k, v)) => req.addParameter(k, v)
      }

      val req = method match {
        case GET => withParameters.GET
        case POST => withParameters.POST
        case PUT => withParameters.PUT
        case DELETE => withParameters.DELETE
      }

      headers.get("Host").foreach(req.setVirtualHost)

      Http(req OK as.Response(identity))
    }
  }
}

/** constants for the stats API */
object By extends Enumeration {
  val minute, hour, day = Value
}

/** constants for the stats API */
object Region extends Enumeration {
  val all, usa, europe, ausnz, apac = Value
}
