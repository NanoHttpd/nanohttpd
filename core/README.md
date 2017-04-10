## Gradle

The gradle wrapper is distributed with NanoHttpd so developers can work with the project
without needing to install Gradle beforehand. The other benefit is users will automatically
use the version of Gradle the build was designed to work with.

To get started, run `./gradlew wrapper` in the core directory.

Next, you can run `./gradlew build` to run the unit tests and build a NanoHttpd jar.