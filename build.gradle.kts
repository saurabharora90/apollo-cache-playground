plugins {
  id("org.jetbrains.kotlin.multiplatform").version("2.0.21")
  id("com.apollographql.apollo").version("4.3.0")
}

kotlin {
  jvm()
  macosArm64()

  compilerOptions {
    freeCompilerArgs.add("-Xdebug")
  }

  sourceSets {
    getByName("commonMain") {
      dependencies {
        implementation("com.apollographql.cache:normalized-cache-sqlite:1.0.0-alpha.5")
        implementation("com.apollographql.apollo:apollo-runtime:4.3.0")
      }
    }
    getByName("commonTest") {
      dependencies {
        implementation("com.apollographql.mockserver:apollo-mockserver:0.1.1")
        implementation("org.jetbrains.kotlin:kotlin-test")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        implementation("app.cash.turbine:turbine:1.2.1")
      }
    }
  }
}

apollo {
  service("service") {
    packageName.set("com.example")
    plugin("com.apollographql.cache:normalized-cache-apollo-compiler-plugin:1.0.0-alpha.3") {
      argument("packageName", packageName.get())
    }
  }
}