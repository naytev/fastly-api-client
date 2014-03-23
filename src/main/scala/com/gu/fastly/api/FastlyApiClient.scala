package com.gu.fastly.api

import scala.concurrent.Future
import org.joda.time.DateTime
import scala.language.implicitConversions
import dispatch._
import com.ning.http.client.{AsyncHttpClient, Response, AsyncHttpClientConfig}
import com.ning.http.client.providers.netty.{NettyAsyncHttpProvider, NettyConnectionsPool}
import scala.concurrent.ExecutionContext.Implicits.global

// TODO vclDeleteAll
// TODO vclUpdate and vclUpload to return Future[List[Response]] - ExecutionContext!
// TODO Java Docs

// http://docs.fastly.com/api
case class FastlyApiClient(apiKey: String, serviceId: String, config: Option[AsyncHttpClientConfig] = None) {

  private val fastlyApiUrl = "https://api.fastly.com"

  sealed trait HttpMethod
  object GET extends HttpMethod
  object POST extends HttpMethod
  object PUT extends HttpMethod
  object DELETE extends HttpMethod

  def vclUpload(version: Int, vcl: Map[String, String]): Future[List[Response]] = {
    val f = vcl.map {
      case (fileName, fileAsString) => {
        val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/vcl"
        AsyncHttpExecutor.execute(
          apiUrl,
          POST,
          headers = Map("Content-Type" -> "application/x-www-form-urlencoded"),
          parameters = Map("content" -> fileAsString, "name" -> fileName, "id" -> fileName)
        )
      }
    }.toList
    Future.sequence(f)
  }

  def vclUpdate(version: Int, vcl: Map[String, String]): Future[List[Response]] = {
    val f = vcl.map {
      case (fileName, fileAsString) => {
        val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/vcl/$fileName"
        AsyncHttpExecutor.execute(
          apiUrl,
          PUT,
          headers = Map("Content-Type" -> "application/x-www-form-urlencoded"),
          parameters = Map("content" -> fileAsString, "name" -> fileName)
        )
      }
    }.toList
    Future.sequence(f)
  }

  def purge(url: String): Future[Response] = {
    val urlWithoutPrefix = url.stripPrefix("http://").stripPrefix("https://")
    val apiUrl = s"$fastlyApiUrl/purge/$urlWithoutPrefix"
    AsyncHttpExecutor.execute(apiUrl, POST, headers = Map("X-Fastly-Key" -> apiKey))
  }

  def versionCreate(): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version"
    AsyncHttpExecutor.execute(apiUrl, PUT)
  }

  def versionList(): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version"
    AsyncHttpExecutor.execute(apiUrl)
  }

  def versionActivate(version: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/activate"
    AsyncHttpExecutor.execute(apiUrl, PUT)
  }

  def versionClone(version: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/clone"
    AsyncHttpExecutor.execute(apiUrl, PUT, headers = Map("Content-Type" -> "application/x-www-form-urlencoded"))
  }

  def versionValidate(version: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/validate"
    AsyncHttpExecutor.execute(apiUrl)
  }

  def vclSetAsMain(version: Int, name: String): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/vcl/$name/main"
    AsyncHttpExecutor.execute(apiUrl, PUT)
  }

  def vclList(version: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/vcl"
    AsyncHttpExecutor.execute(apiUrl)
  }

  def vclDelete(version: Int, name: String): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/vcl/$name"
    AsyncHttpExecutor.execute(apiUrl, DELETE)
  }

  def backendCheckAll(version: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/backend/check_all"
    AsyncHttpExecutor.execute(apiUrl)
  }

  // TODO what is this ipv4?
  def backendCreate(version: Int, id: String, address: String, port: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/backend"
    val params = Map("ipv4" -> address, "version" -> version.toString, "id" -> id, "port" -> port.toString, "service" -> serviceId)
    AsyncHttpExecutor.execute(apiUrl, POST, parameters = params)
  }

  def backendList(version: Int): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service/$serviceId/version/$version/backend"
    AsyncHttpExecutor.execute(apiUrl)
  }

  def serviceList(): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/service"
    AsyncHttpExecutor.execute(apiUrl)
  }

  def stats(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats"
    val params = statsParams(from, to, by, region)
    AsyncHttpExecutor.execute(apiUrl, parameters = params)
  }

  def statsWithFieldFilter(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all, field: String): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/field/$field"
    val params = statsParams(from, to, by, region)
    AsyncHttpExecutor.execute(apiUrl, parameters = params)
  }

  def statsAggregate(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/aggregate"
    val params = statsParams(from, to, by, region)
    AsyncHttpExecutor.execute(apiUrl, parameters = params)
  }

  def statsForService(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all, serviceId: String): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/service/$serviceId"
    val params = statsParams(from, to, by, region)
    AsyncHttpExecutor.execute(apiUrl, parameters = params)
  }

  def statsForServiceWithFieldFilter(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all, serviceId: String, field: String): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/service/$serviceId/field/$field"
    val params = statsParams(from, to, by, region)
    AsyncHttpExecutor.execute(apiUrl, parameters = params)
  }

  def statsUsage(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/usage"
    val params = statsParams(from, to, by, region)
    AsyncHttpExecutor.execute(apiUrl, parameters = params)
  }

  def statsUsageGroupedByService(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/usage_by_service"
    val params = statsParams(from, to, by, region)
    AsyncHttpExecutor.execute(apiUrl, parameters = params)
  }

  def statsRegions(): Future[Response] = {
    val apiUrl = s"$fastlyApiUrl/stats/regions"
    AsyncHttpExecutor.execute(apiUrl)
  }

  private def statsParams(from: DateTime, to: DateTime, by: By.Value, region: Region.Value = Region.all): Map[String, String] = {
    def millis(date: DateTime): String = (date.getMillis / 1000).toString
    Map[String, String]("from" -> millis(from), "to" -> millis(to), "by" -> by.toString, "region" -> region.toString)
  }

  def closeConnectionPool() = AsyncHttpExecutor.close()

  private object AsyncHttpExecutor {

    private lazy val commonHeaders = Map("X-Fastly-Key" -> apiKey, "Accept" -> "application/json")

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

// constants for the stats API
object By extends Enumeration {
  val minute, hour, day = Value
}

object Region extends Enumeration {
  val all, usa, europe, ausnz, apac = Value
}
