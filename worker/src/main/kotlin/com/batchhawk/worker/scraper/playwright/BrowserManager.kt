package com.batchhawk.worker.scraper.playwright

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore

@Component
class BrowserManager(private val browser: Browser) {

    private val log = LoggerFactory.getLogger(BrowserManager::class.java)
    private val semaphore = Semaphore(1)
    private var sessionCount = 0

    fun <T> withContext(block: (BrowserContext) -> T): T {
        semaphore.acquire()
        val context = browser.newContext(
            Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setViewportSize(1280, 800)
                .setLocale("en-US")
        )
        log.debug("Browser context acquired (session #{})", sessionCount + 1)
        try {
            return block(context)
        } finally {
            runCatching { context.close() }
                .onFailure { log.warn("Error closing browser context", it) }
            sessionCount++
            log.debug("Browser context released (total sessions: {})", sessionCount)
            semaphore.release()
        }
    }
}
