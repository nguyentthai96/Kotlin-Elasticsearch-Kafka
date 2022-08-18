package com.search.microservice.configuration

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import co.elastic.clients.elasticsearch.indices.GetIndexRequest
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import reactor.util.Loggers
import java.lang.Exception


@Configuration
class ElasticSearchIndexConfig(private val esClient: ElasticsearchAsyncClient): CommandLineRunner {

    @Value(value = "\${elasticsearch.mappings-path:classpath:mappings.json}")
    val usersMappingsResourceFile: Resource? = null

    @Value(value = "\${elasticsearch.mappings-index-name}")
    val usersIndexName: String? = null

    @Value(value = "\${faker.enable:false}")
    val enable: Boolean = false

    @Value(value = "\${faker.count:555}")
    val count: Int = 555


    override fun run(vararg args: String?): Unit = runBlocking {
        try {
            val mappingBytes = usersMappingsResourceFile?.inputStream?.readAllBytes() ?: byteArrayOf()
            log.info("creating index: $usersIndexName mappings: ${String(mappingBytes)}")

            val exists = esClient.indices().exists(ExistsRequest.Builder().index(usersIndexName).build()).await()
            if (!exists.value()) {
                esClient.indices().create { it.index(usersIndexName).withJson(usersMappingsResourceFile?.inputStream) }.await()
                    .also { log.info("index created: ${it.index()}") }

//                if (enable) generateMockData()
                return@runBlocking
            }

            esClient.indices().get(GetIndexRequest.Builder().index(usersIndexName).build()).await()
                .also { log.info("index already exists: ${it.result()}") }

//            if (enable) generateMockData()
        } catch (ex: Exception) {
            log.error("error while loading mappings file: ${ex.message}", ex)
        }

    }

    companion object {
        private val log = Loggers.getLogger(ElasticSearchIndexConfig::class.java)
    }
}