package com.batchhawk.worker.scraper.squarespace

import com.batchhawk.common.CompleteRunRequest
import com.batchhawk.common.NextJobResponse
import com.batchhawk.worker.scraper.RoasterScraper
import com.batchhawk.worker.scraper.playwright.PlaywrightAgentScraper
import org.springframework.stereotype.Component

@Component("SQUARESPACE")
class SquarespaceProductScraper(
    private val delegate: PlaywrightAgentScraper,
) : RoasterScraper {
    override fun scrape(job: NextJobResponse): CompleteRunRequest = delegate.scrape(job)
}
