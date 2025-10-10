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
    runtimeOnly("com.oracle.database.jdbc:ojdbc8:21.7.0.0")
    jooqGenerator("com.oracle.database.jdbc:ojdbc8:21.7.0.0")
}

jooq {
    version.set("3.19.12")

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
                        excludes = "DATABASECHANGELOG|DATABASECHANGELOGLOCK" // liquibase tables
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
                        withInputSchema("BOOKSTORE")
                        forcedTypes.apply {
                            add(
                                ForcedType().apply {
                                    this.name = "numeric"
                                    this.sql =
                                        """select col.owner || '.' || col.TABLE_NAME || '.' || col.column_name from all_tab_cols col where col.owner = 'BOOKSTORE' and col.DATA_TYPE = 'NUMBER' """
                                }
                            )
                            add(
                                ForcedType().apply {
                                    this.name = "varchar"
                                    this.sql =
                                        """select col.owner || '.' || col.TABLE_NAME || '.' || col.column_name from all_tab_cols col where col.owner = 'BOOKSTORE' and col.DATA_TYPE = 'VARCHAR2' """
                                }
                            )
                        }
                    }
                    target.apply {
                        packageName = "db-xml"
                        directory = "database"
                    }
                }
                jdbc.apply {
                    driver = "oracle.jdbc.driver.OracleDriver"
                    url = System.getenv("DB_BOOKSTORE_URL") ?: "jdbc:oracle:thin:@localhost:1522/FREEPDB1"
                    user = System.getenv("DB_BOOKSTORE_USERNAME") ?: "BOOKSTORE"
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
