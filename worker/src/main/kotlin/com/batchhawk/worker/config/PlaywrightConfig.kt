package com.batchhawk.worker.config

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PlaywrightConfig {

    private val log = LoggerFactory.getLogger(PlaywrightConfig::class.java)

    private lateinit var playwright: Playwright
    private lateinit var browser: Browser

    @Bean
    fun playwright(): Playwright {
        playwright = Playwright.create()
        return playwright
    }

    @Bean
    fun browser(playwright: Playwright): Browser {
        browser = playwright.chromium().launch(
            BrowserType.LaunchOptions().setHeadless(true)
        )
        log.info("Playwright Chromium browser started")
        return browser
    }

    @PreDestroy
    fun close() {
        runCatching { browser.close() }.onFailure { log.warn("Error closing browser", it) }
        runCatching { playwright.close() }.onFailure { log.warn("Error closing playwright", it) }
        log.info("Playwright browser closed")
    }
}