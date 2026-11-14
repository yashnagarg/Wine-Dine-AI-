package com.winedine.llm;

import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchResult;

import java.util.List;

/**
 * What the agent needs from a language model (the Target of the Adapter pattern).
 * The LLM only handles language: it never picks restaurants or invents facts.
 */
public interface LLMClient {

    /** Turns a free-text request into constraints. Values the user did not mention stay unset. */
    SearchConstraints extractConstraints(String userText);

    /** Turns a follow-up message (an answer or a change such as "make it $20") into constraint changes. */
    SearchConstraints interpretRefinement(String userText, SearchConstraints current);

    /** Phrases one short question asking for the missing details ("budget", "time", "location"). */
    String askClarification(List<String> missingFields, SearchConstraints known);

    /** Phrases an explanation using only the verified facts in {@link SearchResult#getMatchSummary()}. */
    String explain(SearchResult result, SearchConstraints constraints);

    /** Short name for the CLI/GUI status line, e.g. "OpenAI gpt-4o-mini" or "offline mock". */
    String describe();
}
