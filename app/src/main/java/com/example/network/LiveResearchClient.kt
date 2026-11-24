package com.example.network

import com.example.research.LiveResearchModule

/**
 * Delegating client backed by the standalone `:research` Gradle module
 * (Live Web Search, Academic & GitHub Journal Search, and Multi-Source Verification).
 */
class LiveResearchClient : LiveResearchModule()
