# HttpClient
Effectful Light-Weight Scala HttpClient 

[![Build Status](https://travis-ci.org/MideO/HttpClient.svg?branch=master)](https://travis-ci.org/MideO/HttpClient)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.mideo/httpclient_2.12/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.mideo%22%20a%3A%22httpclient_2.12%22)



# Usage
Generate a effectful response from a given http request

```scala
# Define your entities

case class Payload(value: String)

case class Response(value: String)

# Define unit/identity function

implicit def unit[T]: T => Future[T] = t => Future(t)

# Create your request

val request = HttpRequest(
        Post,
        "http://localhost:8080/lalal",
        Map("aHeader" -> "value"),
        Payload("abc")
    )

val future: Future[Either[Throwable, HttpResponse[Response]]] = HttpRequestSender.send(request)

# Get you response => Response("response")
val eitherResponse: Either[Throwable, HttpResponse[Response]] = Await.result(future, 5 seconds)
eitherResponse.right.get.Entity


# Create Xml Request

val request = XmlHttpRequest(
        Post,
        "http://localhost:8080/lalal",
        Map("aHeader" -> "value"),
        Payload("abc")
    )

val future: Future[Either[Throwable, HttpResponse[Response]]] = HttpRequestSender.send(request)

# Get you response => Response("response")
val eitherResponse: Either[Throwable, HttpResponse[Response]] = result(future, 5 seconds)
eitherResponse.right.get.Entity


# Create Json Request

val request = JsonHttpRequest(
        Post,
        "http://localhost:8080/lalal",
        Map("aHeader" -> "value"),
        Payload("abc")
     )

# Use a different Monad

implicit def unit[T]: T => Option[T] = t => Some(t)

val option: Future[Either[Throwable, HttpResponse[Response]]] = HttpRequestSender.send(request)

# Get you response => Response("response")

option.get.right.get.Entity
 

```