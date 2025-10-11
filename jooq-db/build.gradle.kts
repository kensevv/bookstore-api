import nu.studer.gradle.jooq.JooqGenerate
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Property

plugins {
    java
    kotlin("jvm") version "1.9.20"
    id("nu.studer.jooq") version "8.2.1"
}
group = "com.lvlup"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:23.0.0")
    runtimeOnly("org.postgresql:postgresql:42.7.7")
    jooqGenerator("org.postgresql:postgresql:42.7.7")
}

jooq {
    version.set("3.19.13")

    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(true)
            jooqConfiguration.apply {
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    generate.apply {
                        withRelations(true)
                        withDeprecated(false)
                        withRecords(true)
                        withImmutablePojos(true)
                        withPojosAsKotlinDataClasses(true)
                        withDaos(true)
                    }
                    database.apply {
                        name = "org.jooq.meta.xml.XMLDatabase"
                        properties.add(
                            Property().apply {
                                this.key = "xmlFile"
                                this.value = "database/db-xml/information_schema.xml"
                            })
                        excludes = "databasechangelog|databasechangeloglock" // liquibase tables
                    }
                    target.apply {
                        packageName = "com.lvlup.bookstore.jooq"
                    }
                }
            }
        }
        create("xmlGenerate") {
            generateSchemaSourceOnCompilation.set(false)
            jooqConfiguration.apply {
                generator.apply {
                    name = "org.jooq.codegen.XMLGenerator"
                    onError = org.jooq.meta.jaxb.OnError.LOG
                    generate.apply {
                        withRelations(true)
                        withDeprecated(false)
                        withRecords(true)
                        withImmutablePojos(true)
                        withNullableAnnotation(true)
                        withNonnullAnnotation(true)
                        withNullableAnnotationType("org.jetbrains.annotations.Nullable")
                        withNonnullAnnotationType("org.jetbrains.annotations.NotNull")
                    }
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        withInputSchema("bookstore")
                        includes = ".*"
                        excludes = "databasechangelog|databasechangeloglock"
                        forcedTypes.addAll(
                            listOf(
                                ForcedType().apply {
                                    name = "varchar"
                                    includeTypes = "character varying|varchar|text"
                                },
                                ForcedType().apply {
                                    name = "timestamp"
                                    includeTypes = "timestamp.*"
                                },
                            )
                        )
                    }
                    target.apply {
                        packageName = "db-xml"
                        directory = "database"
                    }
                }
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = System.getenv("DB_BOOKSTORE_URL") ?: "jdbc:postgresql://localhost:5432/bookstore"
                    user = System.getenv("DB_BOOKSTORE_USERNAME") ?: "bookstore"
                    password = System.getenv("DB_BOOKSTORE_PASSWORD") ?: "bookstore123"
                }
            }
        }

    }
}


val generateJooq by tasks.existing(JooqGenerate::class) {
    allInputsDeclared.set(false)
    outputs.cacheIf { false }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
