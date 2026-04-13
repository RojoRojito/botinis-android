package io.botinis.app.domain

import io.botinis.app.data.model.CEFRLevel
import io.botinis.app.data.model.Scenario

object ScenarioCatalog {
    val allScenarios = listOf(
        Scenario(
            id = "coffee_friend",
            name = "Coffee with a Friend",
            description = "Casual chat at a café",
            character = "Alex (your friend)",
            level = CEFRLevel.A2,
            objectives = listOf("Greet and ask how they are", "Order a drink", "Make plans to meet again"),
            targetVocabulary = listOf("grab a coffee", "catch up", "hang out", "what's new", "sounds good", "let's do it"),
            systemPrompt = "You are Alex, a close friend meeting for coffee. Keep language simple (A2 level). Use short sentences. Naturally include target vocabulary. Be warm and casual.",
            voice = "en-US-JennyNeural",
            iconResId = android.R.drawable.ic_menu_gallery
        ),
        Scenario(
            id = "job_interview",
            name = "Job Interview",
            description = "Professional interview scenario",
            character = "Ms. Johnson (HR Manager)",
            level = CEFRLevel.B1,
            objectives = listOf("Introduce yourself professionally", "Describe your experience", "Ask about the role"),
            targetVocabulary = listOf("background", "experience", "strengths", "team player", "challenge", "goal"),
            systemPrompt = "You are Ms. Johnson, an HR manager conducting a job interview. Use B1 level English. Ask professional questions. Give the candidate opportunities to respond.",
            voice = "en-US-GuyNeural",
            iconResId = android.R.drawable.ic_menu_agenda
        ),
        Scenario(
            id = "supermarket",
            name = "At the Supermarket",
            description = "Shopping interaction",
            character = "Cashier",
            level = CEFRLevel.A2,
            objectives = listOf("Ask where an item is", "Ask about price", "Pay and say goodbye"),
            targetVocabulary = listOf("excuse me", "how much", "aisle", "receipt", "cash or card", "have a nice day"),
            systemPrompt = "You are a friendly supermarket cashier. Use simple A2 English. Help the customer find items and process their purchase.",
            voice = "en-US-JennyNeural",
            iconResId = android.R.drawable.ic_menu_search
        ),
        Scenario(
            id = "trip_planning",
            name = "Planning a Trip",
            description = "Travel planning discussion",
            character = "Travel Agent",
            level = CEFRLevel.B1,
            objectives = listOf("Describe your destination", "Ask about accommodation", "Discuss budget and dates"),
            targetVocabulary = listOf("destination", "accommodation", "itinerary", "budget", "book in advance", "peak season"),
            systemPrompt = "You are a helpful travel agent. Use B1 level English. Help plan a trip with practical advice about destinations, hotels, and budgets.",
            voice = "en-US-GuyNeural",
            iconResId = android.R.drawable.ic_menu_compass
        ),
        Scenario(
            id = "doctor_visit",
            name = "At the Doctor",
            description = "Medical consultation",
            character = "Dr. Smith",
            level = CEFRLevel.B1,
            objectives = listOf("Describe your symptoms", "Ask about treatment", "Follow doctor's advice"),
            targetVocabulary = listOf("symptoms", "prescription", "feel dizzy", "take rest", "side effects", "follow up"),
            systemPrompt = "You are Dr. Smith, a general practitioner. Use B1 level English. Ask about symptoms, provide diagnosis, and give advice. Be professional but reassuring.",
            voice = "en-US-GuyNeural",
            iconResId = android.R.drawable.ic_menu_help
        ),
        Scenario(
            id = "movie_discussion",
            name = "Movie Discussion",
            description = "Talking about films",
            character = "Sam (movie buff friend)",
            level = CEFRLevel.B2,
            objectives = listOf("Share your opinion on a movie", "Discuss plot and characters", "Recommend a film"),
            targetVocabulary = listOf("plot twist", "character development", "cinematography", "masterpiece", "overrated", "must-see"),
            systemPrompt = "You are Sam, a friend who loves movies. Use B2 level English with idiomatic expressions. Discuss films with enthusiasm and detail. Debate opinions respectfully.",
            voice = "en-US-JennyNeural",
            iconResId = android.R.drawable.ic_menu_info_details
        ),
        Scenario(
            id = "restaurant",
            name = "At a Restaurant",
            description = "Dining out experience",
            character = "Waiter",
            level = CEFRLevel.A2,
            objectives = listOf("Request a table", "Order food and drinks", "Ask for the bill"),
            targetVocabulary = listOf("table for two", "menu", "I'd like", "recommend", "check please", "delicious"),
            systemPrompt = "You are a polite restaurant waiter. Use A2 level English. Take orders, make recommendations, and ensure the customer has a good experience.",
            voice = "en-US-GuyNeural",
            iconResId = android.R.drawable.ic_menu_day
        ),
        Scenario(
            id = "tech_support",
            name = "Tech Support Call",
            description = "Phone support interaction",
            character = "Tech Support Agent",
            level = CEFRLevel.B2,
            objectives = listOf("Describe the technical issue", "Follow troubleshooting steps", "Confirm resolution"),
            targetVocabulary = listOf("troubleshoot", "restart", "error message", "update", "configuration", "resolve"),
            systemPrompt = "You are a tech support agent helping a customer with a technical issue. Use B2 level English. Guide through troubleshooting steps patiently and professionally.",
            voice = "en-US-JennyNeural",
            iconResId = android.R.drawable.ic_menu_call
        )
    )

    fun getScenarioById(id: String): Scenario? = allScenarios.find { it.id == id }

    fun getScenariosByLevel(level: CEFRLevel): List<Scenario> =
        allScenarios.filter { it.level == level }
}
