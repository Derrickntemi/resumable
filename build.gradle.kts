plugins {
    id("scala")
    id("application")
    alias(libs.plugins.shadowGradlePlugin)
}

repositories {
    mavenCentral()
}


dependencies {
    implementation(libs.scalaLibrary)

    // pekko http bom
    implementation(platform(libs.pekkoHttpBom))
    // Pekko
    implementation(libs.pekkoActorTyped)
    implementation(libs.pekkoStream)
    implementation(libs.pekkoHttp)
    implementation(libs.sprayJson)

    // MongoDB & Doobie
    implementation(libs.mongoScalaDriver)
    implementation(libs.doobieCore)
    implementation(libs.doobieMongodb)

    // JWT & OAuth
    implementation(libs.javaJwt)

    // Compression
    implementation(libs.commonsCompress)

    // Encryption
    implementation(libs.bouncyCastle)

    // Logging
    implementation(libs.log4jCore)
    implementation(libs.log4jApi)
    implementation(libs.log4jSlf4jBinding)
    implementation(libs.scalaLogging)
}


application {
    mainClass.set("com.fileupload.Main")
}
