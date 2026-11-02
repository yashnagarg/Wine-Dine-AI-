package com.winedine.model;

//Ranking profile chosen by the LLM from the user's wording ("cheapest first", "closest first").
public enum RankingProfile {
    BALANCED,
    BUDGET_FIRST,
    DISTANCE_FIRST
}
