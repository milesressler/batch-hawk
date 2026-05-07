package com.batchhawk.worker.scraper

import com.batchhawk.common.CompleteRunRequest
import com.batchhawk.common.NextJobResponse

interface RoasterScraper {
    fun scrape(job: NextJobResponse): CompleteRunRequest
}