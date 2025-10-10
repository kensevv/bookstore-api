package com.lvlup.backend.beans

import org.jooq.conf.MappedSchema
import org.jooq.conf.RenderMapping
import org.jooq.conf.Settings
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class JooqConfig {
    @Bean
    fun jooqSettings(@Value("\${spring.datasource.username}") schema: String): Settings = Settings()
        .withRenderMapping(
            RenderMapping()
                .withSchemata(
                    MappedSchema().withInput("BOOKSTORE")
                        .withOutput(schema.uppercase())
                )
        )
}