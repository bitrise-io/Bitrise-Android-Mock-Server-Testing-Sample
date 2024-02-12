# Bitrise Android mock server container example

This is a sample Android project that demonstrates the usage of service containers attached to a Bitrise workflow.

## Architecture

The CatFacts app displays random facts about cats. Data is fetched from the awesome [Cat Facts REST API](https://catfact.ninja/) using Retrofit ([jump to code](https://github.com/bitrise-io/Bitrise-Android-Mock-Server-Testing-Sample/blob/9f8b9e0e674724daa11a3ef6336ece8be4b09d16/app/src/main/java/io/bitrise/sample/android/mockserver/data/CatFactService.kt#L5)). The UI is created using Compose ([jump to code](https://github.com/bitrise-io/Bitrise-Android-Mock-Server-Testing-Sample/blob/9f8b9e0e674724daa11a3ef6336ece8be4b09d16/app/src/main/java/io/bitrise/sample/android/mockserver/ui/catfacts/CatFacts.kt#L25)) and a standard Jetpack ViewModel ([jump to code](https://github.com/bitrise-io/Bitrise-Android-Mock-Server-Testing-Sample/blob/9f8b9e0e674724daa11a3ef6336ece8be4b09d16/app/src/main/java/io/bitrise/sample/android/mockserver/ui/catfacts/CatFactsViewModel.kt)). UI state is handled as a Kotlin Flow.

https://github.com/bitrise-io/Bitrise-Android-Mock-Server-Testing-Sample/assets/1694986/66feaeff-d7c3-4bbb-8ec9-df404a5de8f0


## Testing setup

When writing integration or smoke tests, it's often required to swap the production API endpoint with an alternate endpoint or a mocked one.

We are going to demonstrate this by mocking the Cat Facts API to return a robot fact 🤖. After all, testing a feature that displays random information is not easy.

Bitrise service containers help with this: we can spin up an entire API service from a single Docker image that runs on `localhost` next to the tests in CI. This has several benefits:

- Eliminates network latency and flakiness in tests
- Tests become more deterministic as they no longer depend on the API not changing suddenly. The mocked server behavior is defined next to the test code.
- The app can freely make requests to the API without altering production data, requiring special test accounts, etc.

On a high level, the testing workflow looks like this:

- Build and launch a service container that mocks the real API (more on this later)
- Wait for the API to become ready
- Run unit tests, connecting to `localhost` instead of the prod endpoint
- Run instrumented tests, connecting to `localhost` instead of the prod endpoint

This is how the workflow is defined in [bitrise.yml](bitrise.yml):

```yml
tests:
    before_run:
      - _checkout
      - _build-mock-api-image
    services:
      mocked-test-server:
        image: test_server:latest
        ports:
          - 3001:3001
        # Healthcheck makes sure that the service is up and running before the tests start
        options: >-
          --health-cmd "curl http://localhost:3001/health"
          --health-interval 5s
          --health-timeout 5s
          --health-retries 5
    steps:
      - android-unit-test@1:
          inputs:
            - module: app
            - variant: debug
      - avd-manager@1:
          title: Boot emulator
          inputs:
            - api_level: 26
            - tag: default
      - wait-for-android-emulator@1: {}
      - gradle-runner@1:
          title: Run instrumented tests
          inputs:
            - gradle_task: ":app:connectedDebugAndroidTest"
            - gradlew_path: "$BITRISE_SOURCE_DIR/gradlew"

```

### The mock server

This sample uses the popular [Mockoon](https://mockoon.com/) tool for mocking the Cat Facts REST API. The configuration file was generated using its desktop app and it's committed to the repo at [test_server/fixtures/cat_api.json](test_server/fixtures/cat_api.json).

It does the following:

- Mocks the `GET /fact` endpoint by returning a robot fact 🤖 instead of a cat fact 🐈
- Mocks the `GET /health` endpoint by returning `200 OK` (more on this later)

> [!NOTE]
> Service containers with volume mounts are not supported at the moment. We work around this by building a three-line [Dockerfile](test_server/Dockerfile) that copies the mock config into the image:

```
FROM mockoon/cli:latest
COPY fixtures/cat_api.json /data/fixtures/cat_api.json
CMD ["-d", "/data/fixtures/cat_api.json"]
```

Then, the `_build-mock-api-image` workflow builds this Docker image before running the main workflow:

```yml
_build-mock-api-image:
description: |
    Builds a Docker image of the mock API server (preconfigured with the correct routes).
    This is used when launching the service containers of other workflows.
steps:
    - docker-build-push:
        inputs:
        - tags: test_server:latest
        - context: test_server
        - file: test_server/Dockerfile
        - push: "false"
        - extra_options: --platform=linux/amd64
```

### Tests

Because the service container with our mock API is launched before the testing workflow, we can write tests that connect to `localhost` instead of the production endpoint.

This is how an [instrumented test](app/src/androidTest/java/io/bitrise/sample/android/mockserver/CatFactTest.kt) looks like:

```kotlin
class CatFactTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // We recreate the ViewModel for testing, but it connects to a different API endpoint
    private val viewModel = CatViewModel(
        catFactService = Retrofit.Builder()
            // // Since this code is running on the emulator, we use the special address 10.0.2.2 to reach the true localhost
            .baseUrl("http://10.0.2.2:3001")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(CatFactService::class.java)
    )

    @Test
    fun testGetNewCatFact() {
        composeTestRule.setContent {
            CatAppTheme {
                CatFact(vm = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Get a new cat fact").performClick()

        val expectedFact = "In 1997, NASA's Mars Pathfinder mission delivered a robotic rover named Sojourner to the surface of Mars. This marked the first time a mobile robot roamed another planet, paving the way for future exploration rovers like Spirit, Opportunity, Curiosity, and Perseverance."
        composeTestRule.onNodeWithText(expectedFact).assertExists()
    }
}
```

And this is how [a unit test](app/src/test/java/io/bitrise/sample/android/mockserver/CatFactsViewModelTest.kt) would look like, running on JVM:

```kotlin
class CatFactsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockedCatFactService = Retrofit.Builder()
        .baseUrl("http://localhost:3001")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(CatFactService::class.java)

    @Test
    fun `Given default state When a fact is requested Then the fact becomes available`() = runTest {
        val viewModel = CatViewModel(mockedCatFactService)

        viewModel.state.test {
            assertEquals(CatFactState.Initial, awaitItem())

            viewModel.fetchCatFact()

            assertEquals(CatFactState.Loading, awaitItem())

            val expectedFact = "In 1997, NASA's Mars Pathfinder mission delivered a robotic rover named Sojourner to the surface of Mars. This marked the first time a mobile robot roamed another planet, paving the way for future exploration rovers like Spirit, Opportunity, Curiosity, and Perseverance."
            assertEquals(CatFactState.Success(expectedFact), awaitItem())
        }
    }

}
```

## Resources:

- [Using containers in Bitrise workflows](https://devcenter.bitrise.io/en/infrastructure/docker-containers-on-bitrise/using-containers-in-bitrise-workflows.html)
- [Docker Build & Push step](https://github.com/bitrise-steplib/bitrise-step-docker-build-push)
- [Mockoon](https://mockoon.com)
- [Compose UI testing guide](https://developer.android.com/jetpack/compose/testing#sync-auto)
