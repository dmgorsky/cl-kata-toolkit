Using "scala/toolkit.local" `sbt` template (Scala Toolkit by Scala Center and VirtusLab), implemented two variants, 

* imperative (mainV1) 
```scala
def mainV1(url: String): Either[String, String] = {
    // consume the following API
    val getUri = Uri.parse(url) match
      case Left(_) => return Left(Errors.parseError)
      case rv@Right(value) => value
    val tryRequest = Try {
      basicRequest.get(getUri)
    } match
      case Failure(exception) => return Left(Errors.networkError)
      case Success(value) => value

    val response = Try {
      tryRequest.send(DefaultSyncBackend())
    } match
      case Failure(exception) => return Left(Errors.networkError)
      case Success(value) => value
    logger.info(s"response from ${getUri.toString}: ${response.show()}")

    // parse the response into a JSON object
    val responseBody = response.body match
      case Left(value) => "---"
      case Right(value) => value

    val result = Try {
      ujson.read(responseBody)("ip").str
    }.fold(_ => Left(Errors.jsonError), successResp => Right(successResp))


    result
  }
```


* and using for-comprehension (mainV2)
```scala
{
    val parsedUri = Uri.parse(url)

    val result = {
      for {
        getUri <- parsedUri.toOption
        response <- Try {
          quickRequest.get(getUri).send()
        }.toOption
        _ = logger.info(s"response from ${getUri.toString}: ${response.show()}")
        responseBody = response.body

        result = Try {
          ujson.read(responseBody)("ip").str
        }.fold(_ => Left(Errors.jsonError), successResp => Right(successResp))

      } yield {
        result
      }
    }.getOrElse(Left(Errors.networkError))

    result
  }

```

Simple munit tests check wrong network address and wrong json output:
```scala
test("invoking an API, parsing the response and returning the IP - imperative V1") {
    assert(mainV1("https://api.ipify.org/?format=json").isRight)
    assert(mainV1("https://api.ipify.org/?format=jfather").isLeft)
    assert(mainV1("https://api.ipify.anarchy/?format=json").isLeft)
  }

  test("invoking an API, parsing the response and returning the IP - for comprehension V2") {
    assert(mainV2("https://api.ipify.org/?format=json").isRight)
    assert(mainV2("https://api.ipify.org/?format=jfather").isLeft)
    assert(mainV2("https://api.ipify.anarchy/?format=json").isLeft)
  }
```

Simple Docker build is setup via `sbt-native-packager` Docker plugin:
```sbt
enablePlugins(
  JavaAppPackaging,
  DockerPlugin
)

compile / mainClass := Some("example.Main")
Docker / packageName := "dmgorsky/command-line-kata"
dockerBaseImage := "adoptopenjdk:11-jre-hotspot"
Docker / containerBuildImage := Some("adoptopenjdk:11")
```

It generates the following multi-stage build:
```dockerfile
FROM adoptopenjdk:11-jre-hotspot as stage0
LABEL snp-multi-stage="intermediate"
LABEL snp-multi-stage-id="b400420c-a816-46da-b41e-dadaefdb7f8c"
WORKDIR /opt/docker
COPY 2/opt /2/opt
COPY 4/opt /4/opt
USER root
RUN ["chmod", "-R", "u=rX,g=rX", "/2/opt/docker"]
RUN ["chmod", "-R", "u=rX,g=rX", "/4/opt/docker"]
RUN ["chmod", "u+x,g+x", "/4/opt/docker/bin/cl-kata-toolkit"]

FROM adoptopenjdk:11-jre-hotspot as mainstage
USER root
RUN id -u demiourgos728 1>/dev/null 2>&1 || (( getent group 0 1>/dev/null 2>&1 || ( type groupadd 1>/dev/null 2>&1 && groupadd -g 0 root || addgroup -g 0 -S root )) && ( type useradd 1>/dev/null 2>&1 && useradd --system --create-home --uid 1001 --gid 0 demiourgos728 || adduser -S -u 1001 -G root demiourgos728 ))
WORKDIR /opt/docker
COPY --from=stage0 --chown=demiourgos728:root /2/opt/docker /opt/docker
COPY --from=stage0 --chown=demiourgos728:root /4/opt/docker /opt/docker
USER 1001:0
ENTRYPOINT ["/opt/docker/bin/cl-kata-toolkit"]
CMD []

```
