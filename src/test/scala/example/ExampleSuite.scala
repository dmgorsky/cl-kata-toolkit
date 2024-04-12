package example

import example.Main.{mainV1, mainV2}

class ExampleSuite extends munit.FunSuite:

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
end ExampleSuite
