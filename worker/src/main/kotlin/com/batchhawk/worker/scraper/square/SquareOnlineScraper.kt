package com.batchhawk.worker.scraper.square

import com.batchhawk.common.CompleteRunRequest
import com.batchhawk.common.NextJobResponse
import com.batchhawk.worker.scraper.RoasterScraper
import com.batchhawk.worker.scraper.playwright.PlaywrightAgentScraper
import org.springframework.stereotype.Component

@Component("SQUARE")
class SquareOnlineScraper(private val playwrightScraper: PlaywrightAgentScraper) : RoasterScraper {
    override fun scrape(job: NextJobResponse): CompleteRunRequest = playwrightScraper.scrape(job)
}
