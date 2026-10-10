**EECS3311 Fall 2026 Course Project Stage 1: Wine\&Dine AI**

**1.1 Project Description**

Wine\&Dine AI: An AI-Powered Toronto Restaurant and Happy-Hour Discovery Agent

***Overview:***  
The proposed project is an AI agent-based software system that helps users discover restaurants and happy-hour deals in Toronto based on their personal preferences, budget, group size, location, and desired time. Instead of requiring users to manually search through restaurant websites, menus, deal listings, and map services, the system allows users to describe what they want using natural language.

For example, a user may enter:

*"Four people, cheap drinks and truffle fries, evening, maximum \$15 per person."*

Rather than immediately interpreting terms such as "cheap" or "evening" using assumptions, the agent identifies missing or ambiguous constraints and asks the user to clarify them. The user may define cheap drinks as drinks costing less than \$10 and evening as 5:00 PM to 7:00 PM. The system then converts the request into structured search constraints and uses available restaurant and location data to identify suitable Toronto restaurants.

The system will retrieve and organize restaurant information, happy-hour information, menu/deal information, operating hours, location information, and other relevant attributes. Candidate restaurants will then be filtered according to required constraints and ranked according to how well they satisfy the user's preferences. The system will explain the reasoning behind its recommendations and display the results through an interactive graphical interface, including a map-based view of Toronto.

The system will also support conversational refinement. For example, after receiving recommendations, a user could say, "Make it closer to Osgoode Station," "Increase the budget per person to \$20," or "Prioritize cocktails over food." The agent will update the existing search constraints and generate a revised set of recommendations rather than requiring the user to start a new search, providing more flexibility.

The application will provide both a graphical user interface (GUI) and a command-line interface (CLI). Both interfaces will access the same underlying application and agent services so that the core functionality is not duplicated between the two interfaces.

***Problem Being Solved:***

Finding a suitable restaurant can become an increasingly time consuming task, because of constraints like distance, group size, budget options for each person, happy hour availability, dietary preferences. It requires users to search through multiple platforms and manually compare information, making the seemingly enjoyable activity a nightmare to plan. 

This proposed system exists to address that problem and solve it by having users express their requirements naturally and by using an AI agent to identify, clarify, and coordinate those requirements before performing the search. The system then combines AI-based language understanding with deterministic filtering and recommendation logic to produce recommendations that can be explained and tested.

***Target Users:***

The primary target users are individuals or groups of people looking for restaurant and happy hour opportunities in Toronto. 

The typical users include but are not limited to:

- University students looking for affordable options.  
- Groups of friends deciding on restaurants together to dine at considering each person’s preferences.  
- Individuals looking for restaurants satisfying their specific cravings/food preferences.  
- Users who want to compare their best options based on time, price, available deals, etc.  
- Users who want to describe their needs in a conversational manner and get a quick resolution to the task instead of manually applying many filters.

The system is particularly useful for group decisions because all varieties of requirements can be considered together when selecting and ranking candidate restaurants.

***What the agent can do:***

The restaurant discovery agent will provide the following major capabilities:

- Interpret natural-language restaurant requests.  
- Identify ambiguous or missing constraints and ask the user for clarification.  
- Convert natural-language requirements into structured search constraints.  
- Retrieve restaurant and location information from external services.  
- Retrieve and organize available happy-hour and deal information.  
- Filter restaurants according to hard constraints such as budget, time, location, and availability.  
- Rank candidate restaurants according to how well they satisfy the user's preferences.  
- Explain why particular restaurants were recommended and identify important trade-offs.  
- Support conversational modification of an existing search.  
- Display recommendations through an interactive GUI and map-based visualization.  
- Track the freshness of retrieved deal information using fields such as the last-updated timestamp.  
- Provide equivalent core search functionality through a CLI.

The agent will distinguish between information that can be handled deterministically by the software and tasks that benefit from AI. For example, once the user has defined "cheap drinks" as drinks costing at most \$10, the Java application can deterministically compare drink prices against that constraint. The AI model is instead responsible for understanding the user's language, identifying ambiguity, interpreting preferences, and helping coordinate multi-step interactions.

***Why is an AI agent appropriate for this problem?:***

A traditional restaurant search application could provide filters such as price, location, cuisine but it would be asking the users to manually select each constraint, understand them and work within those limitations. 

Using an AI agent makes it a more flexible and interactive model because users can express their preferences naturally. An AI agent is appropriate because it can interpret the user's request, identify uncertainty, ask targeted clarification questions, maintain the current search context, and coordinate multiple operations needed to produce a recommendation.

***Planned AI/LLM model(s) to be used:***

The project will integrate an LLM through an application-level LLMClient abstraction. The initial implementation is planned to use an OpenAI GPT-family model with structured output capabilities, although the exact model version may be selected during implementation based on API availability, cost, performance, and other constraints.

***AI model interaction with the rest of the software system:***

The AI model will be integrated into the Java application through an LLMClient and communicate with the system through the agent layer. The overall flow will be:

User → GUI/CLI → Agent Controller → Restaurant Agent → LLMClient → Search Request → Data Services → Recommendation Engine → GUI/CLI

The AI and deterministic Java components will work together, with the AI handling natural-language interaction, such as interpreting and breaking down user requests, while the Java components handle data retrieval, validation, filtering, ranking, and application state. This separation keeps the system reliable, ensuring that the actual decisions, such as comparing prices and determining whether restaurants satisfy constraints, are based on retrieved data and deterministic logic rather than AI-generated information.

**1.2 Feature Specification**

*F01 — Natural-Language Restaurant Search*  
Description — Allows users to describe their restaurant preferences instead of manually sorting through filters.

User Interaction —User accesses the feature by typing out the request in the GUI search bar or CLI.

Input — Natural language text containing information like group size, cuisine, location, time, cost preferences.  
Output — A structured search request or clarification request.

AI Involvement — AI-based/hybrid

Expected Workflow — The agent sends the request to the LLM, interprets the user's requirements, extracts relevant constraints, and creates a structured search request.

Error/Alternative Cases — The request can be too vague or may contain missing information, the LLM asks for the specific information instead of making assumptions.

*F02 — Constraint Clarification*  
Description — Identifies missing or ambiguous information and asks the user to define them before searching.

User Interaction — User responds to the clarification questions displayed in the GUI or CLI.

Input — Missing or vague constraints such as “cheap”, ”afternoon” or unspecific locations.

Output — Results in a clarified and structured set of search constraints.

AI Involvement — AI-based/hybrid

Expected Workflow — Missing information is identified, targeted clarification questions are generated, user’s responses are received and lastly, the search request is updated.

Error/Alternative Cases — If the user refuses to provide the information, the system uses only the constraints that are sufficiently defined and informs the user of any limitations.

*F03 — Happy Hour Discovery*  
Description — Finds Toronto restaurants offering the happy hour deals that match the user’s constraints.

User Interaction — User selects the happy hour search option or submits a request containing happy hour specifics.

Input — Day, time range, location, optional food or drink preferences.

Output — Restaurants with relevant happy hour information.

AI Involvement — Hybrid

Expected Workflow — The system retrieves available restaurant and happy-hour data, checks whether deals overlap with the requested time, and returns matching restaurants.

Error/Alternative Cases — If no matching happy hours are found, the system informs the user and can offer nearby or alternative-time results.

*F04 — Budget Based Filtering*

Description — Finds restaurants and deals according to the user's maximum budget per person.

User Interaction — User provides a maximum budget directly or through a natural language request. 

Input — Maximum budget per person and relevant menu/drink price constraints.

Output — Restaurants that satisfy the user’s budget constraints.

AI Involvement — Deterministic

Expected Workflow — The system compares available food and drink prices against the user's defined budget per person and removes restaurants that exceed the constraint.

Error/Alternative Cases — If prices are unavailable or outdated, the system marks the information as unavailable rather than assuming a price. If the budget is too low, it gets the closest alternative.

*F05 — Preference-Based Restaurant Matching*

Description — Matches restaurants against preferences such as food, drinks, atmosphere, sports bars, live music, or other available venue characteristics.

User Interaction — User specifies their preferences through natural language search/GUI.

Input — Food, drink, venue, atmosphere preferences.

Output — Restaurants ranked/filtered according to preferences.

AI Involvement — Hybrid.

Expected Workflow — AI extracts preferences from the user's request, after which the recommendation engine compares them against restaurant attributes.

Error/Alternative Cases — If an exact preference cannot be matched, the system can provide near matches and explain the difference.

*F06 — Location and Distance Filtering*

Description — Helps users find restaurants based in a specific neighborhood or distance from a location.

User Interaction — The user selects or enters a location through the GUI or specifies one naturally.

Input — Location, landmark, neighbourhood, or maximum distance.

Output — Restaurants satisfying the requirements with map and distance information where available.

AI Involvement — Hybrid.

Expected Workflow — AI interprets natural-language locations when necessary, location services calculate distances and the application filters the results.

Error/Alternative Cases — If a location is not found, the user asks for a more specific location/ alternative location.

*F07 — Personalized Restaurant Recommendations*

Description — Presents matching restaurants according to how well they match the user’s combined set of preferences.

User Interaction — The user receives a list of recommended restaurants according to their preferences after the search proceeds.

Input — Filtered restaurants with the user's preferences and constraints.

Output — Ordered restaurant recommendations with matching information.

AI Involvement — Hybrid.

Expected Workflow — Hard constraints are applied first, followed by deterministic ranking based on factors such as preference matches, budget, distance, and happy-hour compatibility.

Error/Alternative Cases — If more than one restaurant receives a similar score, the system can display them as comparable alternatives.

*F08 — Recommendation Explanation/Summary*

Description — Explains why a restaurant was recommended and potential tradeoffs, identifies important matching information.

User Interaction — The user selects a recommended restaurant or requests an explanation.

Input — Restaurant information, matched constraints, and recommendation results.

Output — A concise explanation of the recommendation.

AI Involvement — AI-based/Hybrid.

Expected Workflow — The deterministic components provide verified restaurant information and matching criteria to the agent, which generates a natural-language explanation.

Error/Alternative Cases — If supporting information is incomplete, the system clearly identifies unavailable information instead of generating unsupported claims.

*F09 — Conversational Search Refinement*

Description — Allows users to modify an existing search without restarting the entire process, thus, more flexibility.

User Interaction —The user sends a follow-up request such as "increase the budget to \$25 per person" or "make it closer to College".

Input — Follow-up natural-language request and existing search context.

Output — Updated recommendations based on the modified constraints.

AI Involvement — AI-based/Hybrid.

Expected Workflow — The agent interprets the change, updates the existing search state, and sends the revised constraints through the recommendation workflow.

Error/Alternative Cases — If the requested modification is vague, the system asks for clarification. If the updated constraints produce no suitable results, the system informs the user and offers alternative options.

*F10 — Interactive Map Visualization*

Description — Displays the recommended restaurants on an interactive Toronto map so users can visually compare their locations. 

User Interaction — The user selects the map view from the recommendation results and can select individual restaurants.

Input — Restaurant locations and recommendation results.

Output — Interactive Toronto map displaying relevant restaurants, markers, and location information.

AI Involvement — Deterministic.

Expected Workflow — The system sends restaurant coordinates to the map component and visually distinguishes recommended and non-recommended results.

Error/Alternative Cases — Restaurants without valid location data are displayed without a map marker or are excluded from the map with an appropriate message.

*F11 — Restaurant and Deal Information Validity*

Description —Shows when restaurant and happy-hour information was last updated so users can assess how current it is.

User Interaction — The user views the last-updated date and freshness indicator when viewing restaurant or deal information.

Input — Restaurant/deal information and its last-updated date.

Output — Last-updated information and freshness indicators.

AI Involvement — Deterministic.

Expected Workflow — The system checks the last-updated date and displays an appropriate freshness indicator alongside the information.

Error/Alternative Cases — If information is considered outdated or its update time is unavailable, the system displays a warning rather than presenting it under false pretences.

*F12 — Search History*

Description —Allows users to view and revisit their previous restaurant searches.

User Interaction — The user selects Search History from the GUI and chooses a previous search.

Input — Previous search requests and their associated constraints or results.

Output — A list of previous searches that can be reviewed or reused.

AI Involvement — Deterministic.

Expected Workflow — The system stores completed searches and allows the user to reopen a previous search and optionally continue refining it.

Error/Alternative Cases — If the information from a previous search is outdated, the system warns the user and allows the search to be refreshed before displaying current results.

**2.1 Class Diagram and Design Patterns**

\-attached as a .uxf file in the repo

***Five meaningful design patterns:***

1. **MVC**  
   What design problem does the pattern address?:  
   The GUI must not contain search logic, and the same logic must serve both the GUI and the CLI. If screens and logic are mixed, every change to one breaks the other, and the CLI would need its own copy of everything.  
     
   Which classes participate in the pattern?  
   Model: SearchConstraints, Restaurant, HappyHourDeal, SearchResult, SearchHistory, AgentResponse.  
   View: MainWindow, SearchPanel, ResultsPanel, CliApp.  
   Controller: AgentController.  
     
   What role does each class play?  
   SearchConstraints (Model): holds the structured requirements and knows which ones are missing.  
   Restaurant (Model): holds venue data such as name, coordinates, cuisine, tags, and opening hours.  
   HappyHourDeal (Model): holds deal times and prices and reports its own freshness.  
   SearchResult (Model): holds one matched restaurant with its deal, matched preferences, warnings, and distance.  
   SearchHistory (Model): holds one saved search.  
   AgentResponse (Model): carries the outcome of an action (clarification question, results, or error) from the controller to the view.  
   MainWindow (View): receives button clicks, forwards each action to AgentController, and hands the returned AgentResponse to the panels.  
   SearchPanel (View): collects the user's text and displays clarification questions.  
   ResultsPanel (View): displays the result list, map, restaurant details, explanation, and notices.  
   CliApp (View): the text-based view; reads typed commands and prints the responses.  
   AgentController (Controller): receives every user action from either view, updates the current constraints and results, and returns an AgentResponse. It never touches GUI widgets.  
     
   Why is this pattern appropriate?  
   GUI and CLI are two views over one controller and one model. Task 1 requires both interfaces to use the same underlying services, and MVC enforces that.  
     
   What would be more difficult if the pattern were not used?  
   Search logic would be mixed into button handlers and would have to be copied into the CLI. Any change to the search workflow would need to be made twice, and the logic could not be unit-tested without opening a window.  
     
2. **Facade**

	What design problem does the pattern address?:  
A search involves the agent, the LLM, the data provider, the filters, and history. The user interfaces should not need to know these classes exist or the order in which to call them. It prevents scalability.

Which classes participate in the pattern?  
Facade: AgentController  
Subsystem: RestaurantAgent, HistoryService(and, hidden behind the agent, RecommendationService, LLMClient, RestaurantDataProvider, and the filters).  
Clients: MainWindow, CliApp

What role does each class play?  
AgentController (Facade): exposes a small set of methods (submitRequest, answerClarification, refineSearch, refreshSearch, getExplanation, getHistory, reopenSearch) and coordinates the subsystem behind them. It also saves each completed search to history.  
RestaurantAgent (Subsystem): performs the AI workflow: extracts constraints, decides whether to ask a question or search, runs the search, and generates explanations.  
HistoryService (Subsystem): saves and loads past searches and checks whether one is outdated.  
RecommendationService, LLMClient, RestaurantDataProvider, filters (Hidden subsystem): used by the agent; the interfaces never see them.  
MainWindow (Client): calls only AgentController.  
CliApp (Client): calls only AgentController.

Why is this pattern appropriate?  
Both interfaces need the same functionality, and the Facade gives them exactly one entry point into the system. 

What would be more difficult if the pattern were not used?  
Each interface would call several subsystem classes in the right order, so both would depend on many internal classes. A change to the workflow (for example, adding a step before searching) would require edits in the GUI and the CLI separately.

3. **Adapter**  
   What design problem does the pattern address?  
   The application should not depend on one LLM vendor's request and response format. Task 1 says the exact model may change during implementation, and Stage 3 tests should not need the network.  
     
   Which classes participate in the pattern?  
   Target: LLMClient.  
   Adapter: OpenAILLMClient.  
   Adaptee: OpenAIAPI.  
   Client: RestaurantAgent.  
     
   What role does each class play?  
   LLMClient (Target): defines the interface the application expects, generateResponse(String prompt), which returns plain text.  
   OpenAILLMClient (Adapter): implements LLMClient; converts the prompt into the vendor's JSON request, sends it, and extracts the reply text.  
   OpenAIAPI (Adaptee): the external vendor API, with its own incompatible request and response format.  
   RestaurantAgent (Client): uses only LLMClient and never refers to OpenAILLMClient or the vendor API directly.  
   Why is this pattern appropriate?  
   It keeps everything vendor-specific in one class. It also allows a fake LLMClient that returns fixed JSON to be used in Stage 3 tests.  
     
   What would be more difficult if the pattern were not used?  
   Vendor-specific code would be inside RestaurantAgent. Changing models or providers, or testing without internet access and an API key, would mean editing the agent itself.  
     
4. **Strategy**  
   What design problem does the pattern address?  
   The filtering rules (budget, time, location, preferences) are separate concerns that should be easy to add, remove, test, or skip without rewriting the recommendation logic.  
     
   Which classes participate in the pattern?  
   Context: RecommendationService.  
   Strategy: FilterStrategy.  
   Concrete Strategies: BudgetFilter, TimeFilter, LocationFilter, PreferenceFilter.  
     
   What role does each class play?  
   RecommendationService (Context): holds the list of strategies, applies them one after another, and skips a rule when relaxing constraints to find near matches.  
   FilterStrategy (Strategy): the common interface, filter(restaurants, constraints), which returns the restaurants that pass one rule.  
   BudgetFilter (Concrete Strategy): removes restaurants whose deal exceeds the per-person budget or drink price; keeps those with an unknown price so a warning can be shown.  
   TimeFilter (Concrete Strategy): keeps restaurants open at the requested time and, for happy-hour searches, whose deal overlaps the requested time window.  
   LocationFilter (Concrete Strategy): keeps restaurants within the maximum distance of the searched location.  
   PreferenceFilter (Concrete Strategy): keeps restaurants matching the food, drink, and venue preferences.  
     
   Why is this pattern appropriate?  
   Each rule is a separate object with the same interface, so each can be unit-tested alone in Stage 3\. Each filter ignores itself when its constraint is not set (for example, no budget given). Here the strategies are applied in sequence as a set of rules, rather than one being chosen

   What would be more difficult if the pattern were not used?  
   All rules would sit in one long method. Adding a rule, or switching one off to produce near matches, would mean editing that method, and the rules could not be tested independently. It prevents scalability.

5. **Factory Method**  
   What design problem does the pattern address?  
   RecommendationService needs a RestaurantDataProvider, but it should not decide which concrete one to build. Today the source is the curated JSON file; later it could be an external API, or a small test dataset in Stage 3\.  
     
   Which classes participate in the pattern?  
   Creator: ProviderFactory.  
   Concrete Creator: CuratedProviderFactory.  
   Product: RestaurantDataProvider.  
   Concrete Product: CuratedTorontoDataProvider.  
   Client: RecommendationService.  
     
   What role does each class play?  
   ProviderFactory (Creator): declares the factory method createProvider(), which returns a RestaurantDataProvider.  
   CuratedProviderFactory (Concrete Creator): implements createProvider() by building a CuratedTorontoDataProvider for a given dataset file.  
   RestaurantDataProvider (Product): the interface for data access: findRestaurants(), getRestaurant(), and resolveLocation().  
   CuratedTorontoDataProvider (Concrete Product): reads the curated JSON file and resolves landmark names to coordinates.  
   RecommendationService (Client): receives a ProviderFactory, calls createProvider(), and uses the returned provider without knowing its concrete class.  
     
   Why is this pattern appropriate?  
   Task 1 describes retrieving data from external services, while the first implementation is a curated dataset. The factory is the single place where that choice is made.  
     
   What would be more difficult if the pattern were not used?  
   new CuratedTorontoDataProvider(...) would be hard-coded(which should always be avoided, leading to wrong results) inside RecommendationService. Switching to an API provider, or to a test dataset, would mean editing the service.   
     
   

**2.2 Use Case Diagrams and Use Case Descriptions**

\-.uxf file attached for the use case diagram in the repo

* ***Use case ID: UC01***  
  Use Case Name: Search Restaurants( natural language)  
    
  Actor(s): User  
  	   LLM Service

     Restaurant Data Source

    
  Goal: Get a list of Toronto restaurants or happy-hour deals that match the described needs.  
    
  Preconditions: Application is running.  
    
  Trigger: The user types a request in the GUI search bar or CLI and submits it (optionally choosing the happy-hour option).  
    
  Main Success Scenario:   
- User submits the request.  
- System asks the LLM to convert it into structured constraints.  
- System checks that required constraints (budget, time, location) are defined.  
- System resolves the location and loads nearby restaurants with their deals.  
- System removes restaurants that fail the budget, time/happy-hour, location, or preference rules.  
- System builds the result list (distance, matched preferences, warnings) and sorts it.  
- System displays the list and the map, and saves the search to history.  
    
  Alternative/Exception Flows:  
- Missing or vague constraints= UC02.  
- LLM unavailable or invalid output \= error message; user can retry or rephrase.  
- Location not found \= system asks for a more specific location.  
- A price is unavailable or outdated \= restaurant is kept with a "price unavailable" warning, never an assumed price.  
- No restaurant passes \=closest alternatives (near matches) with the differences explained, including nearby areas or other times for happy hour.  
- Several restaurants have similar scores \=shown as comparable alternatives.  
    
    
  Postconditions: Results are shown and the search is saved to history.  
    
  Related Feature(s): F01 F03 F04 F05 F06 F07   
    
* ***Use Case ID: UC02***  
  Use Case Name: Clarify Constraints  
    
  Actor(s): User  
  	   LLM Service  
    
  Goal: Replace vague or missing constraints ("cheap", "afternoon", "downtown") with explicit values.  
    
  Preconditions:A search is in progress and some required constraints are missing or vague.  
    
  Trigger: The system displays a clarification question.  
    
  Main Success Scenario:   
- System shows a question for the missing items.  
- User answers (e.g., "under \$10, 5 to 7 PM").  
- System merges the answer into the constraints.  
- Once complete, the search continues (UC01 step 4).  
    
  Alternative/Exception Flows:  
- Answer still unclear \= ask again (at most twice).  
- User skips or refuses \= search runs with only the well-defined constraints and a notice states what was left out.  
    
    
  Postconditions: Constraints are updated. Search is then continued or still waiting for an answer.  
    
  Related Feature(s): F02  
    
* ***Use Case ID: UC03***  
  Use Case Name: Refine Search  
    
  Actor(s): User  
  	   LLM Service  
    
  Goal: Change an existing search without starting over.  
    
  Preconditions: Results are displayed.  
    
  Trigger: The user types a follow-up such as "make it closer to Osgoode" or  "increase the budget per person to \$20".  
    
  Main Success Scenario:   
- LLM extracts only the changed constraints.  
- System merges them into the current constraints.  
- System reruns the search (UC01).  
- Updated results are displayed and saved.


  Alternative/Exception Flows:

- Follow-up too vague= clarification question.  
- No results= system says so and offers alternative options.  
    
  Postconditions: Constraints and results are updated. There is a new entry in history.  
    
  Related Feature(s): F09  
    
* ***Use Case ID: UC04***  
  Use Case Name: Explain Recommendations  
    
  Actor(s): User  
  	   LLM Service  
    
  Goal: Understand why a restaurant was recommended and what the trade-offs are.  
    
  Preconditions: Results are displayed.  
    
  Trigger: The user selects a restaurant and clicks "Why this?" (CLI: /explain n).  
    
  Main Success Scenario:   
- System gathers verified facts (matched preferences, price, distance, deal times, freshness).  
- LLM words an explanation using only those facts.  
- Explanation is shown.

  Alternative/Exception Flows:

- Some facts missing \= shown as "unavailable".  
- LLM fails \= a template explanation built from the same facts is shown.

  Postconditions: None (read-only).


  Related Feature(s): F08


* ***Use Case ID: UC05***  
  Use Case Name: View Map and Restaurant Details  
    
  Actor(s): User  
  	   
  Goal: Compare restaurant locations with better visuals and judge how current the deal information is.  
    
  Preconditions: Results are displayed.  
    
  Trigger: The user opens the map view or selects a restaurant or marker.  
    
  Main Success Scenario:   
- Map shows a marker for each result (near matches in a different colour).  
- User selects a marker or list item.  
- Details show the deal, its last-updated date, and a freshness label (FRESH).  
    
    
  Alternative/Exception Flows:  
- Restaurant has no valid coordinates \= omitted from the map with a message.  
- Freshness is STALE or UNKNOWN \= visible warning instead of presenting the deal as current.  
    
  Postconditions: None (read-only).  
    
  Related Feature(s): F10 F11  
    
* ***Use Case ID: UC06***  
  Use Case Name: Review Search History  
    
  Actor(s): User  
  	   
  Goal: Revisit and reuse earlier searches.  
    
  Preconditions: At least one search has been saved.  
    
  Trigger: The user opens Search History (CLI: /history).  
    
  Main Success Scenario:   
- System lists past searches.  
- User selects one.  
- System restores its constraints and results.  
- User may continue refining (UC03).

  Alternative/Exception Flows:

- The saved search is outdated \= warning plus a Refresh action that reruns the search.  
- History is empty \= informational message.


  Postconditions: Constraints and results are restored.


  Related Feature(s): F12


  

**2.3 Sequence Diagrams**

\-.uxf files are attached to the repo 

**For Reference:**

*SD01: Search with Clarification*  
It shows usage of Use Cases UC01 UC02 and Features F01 F02 F03 F04 F05 F06 F07.

*SD02: Refine Search*  
It shows usage of Use Case UC03 and Feature F09.

*SD03: Map, Details, Freshness, Explanation*  
It shows usage of Use Cases UC04 UC05 and Features F08 F10 F11.

*SD04: Search History and Refresh*  
It shows usage of Use Case UC06 and Feature F12.

**3.0  Feature-to-Design Traceability**

| *Feature* | *Description* | *Type* | *Related Use Case* | *Classes* | *Key Methods* | *Sequence Diagram* | *Design Pattern(s)* |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| F01 | Natural-Language Restaurant Search | AI/Hybrid | UC01 | MainWindow, SearchPanel, CliApp, AgentController, RestaurantAgent, LLMClient, OpenAILLMClient, SearchConstraints, AgentResponse | onSearchClicked(), submitRequest(), processRequest(), generateResponse() | SD01 | MVC, Facade, Adapter |
| F02 | Constraint clarification | AI/Hybrid | UC02 | RestaurantAgent, SearchConstraints, AgentResponse, SearchPanel, CliApp | getMissingFields(), clarifyRequest(), answerClarification(), displayClarification() | SD01 | MVC, Facade |
| F03 | Happy-Hour Discovery | Hybrid | UC01 | RecommendationService, TimeFilter, RestaurantDataProvider, CuratedTorontoDataProvider, Restaurant, HappyHourDeal | findMatches(), filter(), findRestaurants(), isAvailableAt() | SD01 | Strategy,Factory Method |
| F04 | Budget-based filtering | Deterministic | UC01 | RecommendationService, BudgetFilter, HappyHourDeal, SearchResult | filter(), matchesBudget(), findNearMatches() | SD01 | Strategy |
| F05  | Preference-based matching | Hybrid | UC01 | RestaurantAgent, SearchConstraints, PreferenceFilter, SearchResult | processRequest(), filter(), getMatchSummary() | SD01 | Strategy,Adapter |
| F06 | Location and distance filtering | Hybrid  | UC01 | RestaurantDataProvider, CuratedTorontoDataProvider, LocationFilter, SearchConstraints, Restaurant | resolveLocation(), filter(), distanceTo(), hasValidLocation() | SD01 | Strategy,Factory Method |
| F07 | Personalized recommendations | Hybrid | UC01 | RecommendationService, FilterStrategy and its four filters, SearchResult, AgentResponse, ResultsPanel | findMatches(), findNearMatches(), displayResults(), getMatchSummary() | SD01 | Strategy, MVC |
| F08 | Recommendation explanation | AI/Hybrid | UC04 | ResultsPanel, MainWindow, AgentController, RestaurantAgent, LLMClient, SearchResult | onWhyThisClicked(), getExplanation(), generateExplanation(), generateResponse(), showExplanation() | SD03 | MVC, Facade, Adapter |
| F09 | Conversational refinement | AI/Hybrid | UC03 | MainWindow, SearchPanel, AgentController, RestaurantAgent, SearchConstraints, RecommendationService | onRefineClicked(), refineSearch(), update(), findMatches() | SD02 | MVC, Facade |
| F10 | Interactive map | Deterministic | UC05 | ResultsPanel, MainWindow, SearchResult, Restaurant | showMap(), hasValidLocation() | SD03 (also SD01) | MVC |
| F11 | Information validity / freshness | Deterministic  | UC05 | HappyHourDeal, Restaurant, SearchResult, ResultsPanel, CliApp | getFreshness(), showDetails(), displayResponse() | SD03 | MVC |
| F12 | Search History | Deterministic | UC06 | MainWindow, AgentController, HistoryService, SearchHistory | saveSearch(), getHistory(), loadSearch(), reopenSearch(), isOutdated(), refreshSearch() | SD04 | MVC, Facade |

**4.0 Explain How Each Feature Is Realized**

**F01: Natural Language Restaurant Search**   
Related Use Case: UC01 Search Restaurants (Natural Language)  
Related Sequence Diagram: SD01 Search with Clarification

Classes involved:

- MainWindow / CliApp \= collect the user’s typed request and display the response.  
- SearchPanel \= holds the text the user typed.  
- AgentController \= coordinates the request and returns an AgentResponse.  
- RestaurantAgent \= runs the AI workflow: extract constraints, then clarify or search.  
- LLMClient \= communicates with the selected LLM (implemented by OpenAILLMClient).  
- SearchConstraints \= represents the structured requirements extracted from the text.  
- AgentResponse \= carries the outcome (clarification, results, or error) back to the interface.

Important methods:

- MainWindow.onSearchClicked()  
- AgentController.submitRequest()  
- RestaurantAgent.processRequest()  
- LLMClient.generateResponse()  
- SearchConstraints.isComplete()

Execution:  
When the user clicks Search, onSearchClicked() reads the text and sends it to AgentController.submitRequest(). The controller invokes RestaurantAgent.processRequest(), which builds an extraction prompt and passes it to LLMClient.generateResponse(). The reply is converted into a SearchConstraints object. If isComplete() is true, the agent continues with the search (F03–F07); otherwise it starts clarification (F02). If the LLM is unavailable or returns invalid output, the agent returns an AgentResponse of type ERROR and the interface shows a notice.

**F02: Constraint Clarification**  
Related Use Case: UC02 Clarify Constraints  
Related Sequence Diagram: SD01 Search with Clarification

Classes involved:

- SearchConstraints \= identifies which required constraints are still missing.  
- RestaurantAgent \= decides to ask a question instead of searching and merges the answer.   
- LLMClient \= phrases the clarification question and interprets the answer.  
- AgentResponse \= carries the clarification question to the interface.  
- SearchPanel / CliApp \= display the question and collect the answer.  
- AgentController \= passes the answer to the agent.

Important methods:

- SearchConstraints.getMissingFields()  
- RestaurantAgent.clarifyRequest()  
- SearchPanel.displayClarification()  
- AgentController.answerClarification()  
- RestaurantAgent.answerClarification()  
- SearchConstraints.update()

Execution:  
After the first extraction, getMissingFields() (plain Java) lists required items such as budget, time, or location that are missing or vague. RestaurantAgent.clarifyRequest() asks the LLM to phrase a question, and the agent returns it as an AgentResponse of type CLARIFICATION, which SearchPanel.displayClarification() shows. The user’s answer goes to AgentController.answerClarification(), then RestaurantAgent.answerClarification(), which merges it into the constraints with update(). After two rounds, or if the user skips, the search runs with only the defined constraints and a notice lists what was left out.  
**F03: Happy Hour Discovery**  
Related Use Case: UC01 Search Restaurants (Natural Language)  
Related Sequence Diagram: SD01 Search with Clarification

Classes involved:

- RecommendationService \= coordinates loading and filtering of restaurants.  
- RestaurantDataProvider / CuratedTorontoDataProvider \= supply restaurants and their deals from the curated dataset.  
- Restaurant \= represents a venue with its list of deals.  
- HappyHourDeal \= represents a deal and its time window.  
- TimeFilter \= keeps restaurants whose deal overlaps the requested time.

Important methods:

- RecommendationService.findMatches()  
- RestaurantDataProvider.findRestaurants()  
- TimeFilter.filter()  
- HappyHourDeal.isAvailableAt()  
- RecommendationService.findNearMatches()

Execution:  
When happyHourRequired is set in the constraints, RecommendationService.findMatches() loads restaurants through findRestaurants() and passes them to TimeFilter.filter(). The filter calls HappyHourDeal.isAvailableAt() for each deal and keeps only restaurants with a deal that overlaps the requested day and time. If none remain, the service calls findNearMatches() to offer nearby or alternative-time options, and the user is told that no exact match exists.

**F04: Budget Based Filtering**  
Related Use Case: UC01 Search Restaurants (Natural Language)  
Related Sequence Diagram: SD01 Search with Clarification

Classes involved:

- RecommendationService \= applies the budget rule as one of its filters.  
- BudgetFilter \= removes restaurants that exceed the budget.  
- HappyHourDeal \= supplies drink price and average cost per person.  
- SearchConstraints \= supplies the maximum budget and maximum drink price.  
- SearchResult \= stores warnings such as “price unavailable”.

Important methods:

- RecommendationService.findMatches()  
- BudgetFilter.filter()  
- HappyHourDeal.matchesBudget()  
- RecommendationService.findNearMatches()

Execution:  
findMatches() passes the restaurants to BudgetFilter.filter(), which compares each deal against the user’s per-person budget and drink price using matchesBudget(). This is pure Java comparison on loaded data. If a price is missing or outdated, the restaurant is kept and its SearchResult receives a “price unavailable” warning instead of an assumed price. If the budget is too low for any result, findNearMatches() returns the closest options.

**F05: Preference Based Restaurant Matching**  
Related Use Case: UC01 Search Restaurants (Natural Language)  
Related Sequence Diagram: SD01 Search with Clarification

Classes involved:

- RestaurantAgent \= has the LLM extract food, drink, and venue preferences.  
- SearchConstraints \=stores the extracted preferences.  
- PreferenceFilter \= compares preferences with restaurant attributes.  
- Restaurant \= provides cuisine and tags to compare against.  
- SearchResult \= records which preferences matched.

Important methods:

- RestaurantAgent.processRequest()  
- PreferenceFilter.filter()  
- SearchResult.getMatchSummary()

Execution:  
During extraction, processRequest() stores preferences such as “truffle fries” or “sports bar” in SearchConstraints. PreferenceFilter.filter() then compares them with each Restaurant’s cuisine and tags, and the matches are recorded in the SearchResult. The LLM only extracts the preferences; the matching is done in Java. A restaurant that matches only some preferences is shown as a near match, and getMatchSummary() states the difference.

**F06: Location and Distance Filtering**  
Related Use Case: UC01 Search Restaurants (Natural Language)  
Related Sequence Diagram: SD01 Search with Clarification

Classes involved:

- SearchConstraints \= stores the location phrase and the resolved coordinates.  
- RestaurantDataProvider / CuratedTorontoDataProvider \=convert a place name into coordinates.  
- LocationFilter \= keeps restaurants within the maximum distance.  
- Restaurant \= provides its coordinates and computes distance.  
- SearchResult \= stores the distance for display.

Important methods:

- RestaurantDataProvider.resolveLocation()  
- LocationFilter.filter()  
- Restaurant.distanceTo()  
- Restaurant.hasValidLocation()

Execution:  
The LLM extracts the place phrase (for example “Osgoode Station”) into the constraints. RecommendationService calls resolveLocation(), which looks the name up in the dataset’s landmark list and stores the coordinates in SearchConstraints. LocationFilter.filter() then calls Restaurant.distanceTo() for each restaurant and keeps those within the maximum distance. If the location is not found, the agent returns a CLARIFICATION response asking for a more specific location.

**F07: Personalized Restaurant Recommendations**  
Related Use Case: UC01 Search Restaurants (Natural Language)  
Related Sequence Diagram: SD01 Search with Clarification

Classes involved:

- RecommendationService \= applies all filters, builds the results, and sorts them.  
- FilterStrategy and its four filters \= apply the hard constraints.  
- SearchResult \= represents one recommended restaurant with its match information.  
- AgentResponse \= carries the ordered list to the interface.  
- ResultsPanel / CliApp \= display the recommendations.

Important methods:

- RecommendationService.findMatches()  
- FilterStrategy.filter()  
- RecommendationService.findNearMatches()  
- SearchResult.getMatchSummary()  
- ResultsPanel.displayResults()

Execution:  
findMatches() applies the budget, time, location, and preference filters in order, so hard constraints are enforced first. For each remaining restaurant it builds a SearchResult (distance, matched preferences, warnings) and sorts the list by number of matched preferences, then by distance. Restaurants with nearly equal results are shown as comparable alternatives. The list is returned inside an AgentResponse, and ResultsPanel.displayResults() shows it.

**F08: Recommendation Explanation/Summary**  
Related Use Case: UC04 Explain Recommendation  
Related Sequence Diagram: SD03 Map, Details, Freshness, and Explanation

Classes involved:

- MainWindow \= receives the “Why this?” click.  
- AgentController \= finds the selected result and requests the explanation.  
- RestaurantAgent \= builds the explanation from verified facts.  
- SearchResult \= supplies the verified facts about the recommendation.  
- LLMClient \= phrases the facts as a natural-language explanation.  
- ResultsPanel \= displays the explanation.

Important methods:

- MainWindow.onWhyThisClicked()  
- AgentController.getExplanation()  
- RestaurantAgent.generateExplanation()  
- SearchResult.getMatchSummary()  
- LLMClient.generateResponse()  
- ResultsPanel.showExplanation()

Execution:  
When the user clicks “Why this?”, onWhyThisClicked() calls AgentController.getExplanation(), which passes the selected SearchResult to RestaurantAgent.generateExplanation(). The agent obtains the verified facts from getMatchSummary() and sends only those facts to LLMClient.generateResponse(). The returned text is displayed by ResultsPanel.showExplanation(). Missing facts are reported as unavailable, and if the LLM fails, a template explanation is built from the same facts.

**F09: Conversational Search Refinement**  
Related Use Case: UC03 Refine Search  
Related Sequence Diagram: SD02 Refine Search

Classes involved:

- MainWindow / SearchPanel \= collect the follow-up text.  
- AgentController \= holds the current constraints and passes them with the follow-up.  
- RestaurantAgent \= interprets the follow-up as changes to the constraints.  
- LLMClient \= extracts only the changed constraints.  
- SearchConstraints \= merges the changes into the existing constraints.  
- RecommendationService \= reruns the search with the updated constraints.

Important methods:

- MainWindow.onRefineClicked()  
- AgentController.refineSearch()  
- RestaurantAgent.refineSearch()  
- SearchConstraints.update()  
- RecommendationService.findMatches()

Execution:  
When the user types a follow-up such as “make it closer to Osgoode” and clicks Refine, onRefineClicked() calls AgentController.refineSearch(), which passes the stored constraints to RestaurantAgent.refineSearch(). The agent asks the LLM for only the changed constraints and merges them with update(), keeping everything else. It then calls findMatches() and returns the new results. A vague follow-up produces a CLARIFICATION response, and an empty result produces near matches with suggested alternatives.

**F10: Interactive Map Visualization**  
Related Use Case: UC05 View Map and Restaurant Details  
Related Sequence Diagram: SD03 Map, Details, Freshness and Explanation

Classes involved:

- MainWindow \= passes each new result list to the results panel.  
- ResultsPanel \= draws the map and its markers.  
- SearchResult \=provides the restaurant to mark and whether it is a near match.  
- Restaurant \=provides coordinates and validates them.

Important methods:

- MainWindow.handleResponse()  
- ResultsPanel.showMap()  
- Restaurant.hasValidLocation()  
- MainWindow.onResultSelected()

Execution:  
After every result update, MainWindow.handleResponse() calls ResultsPanel.showMap(). For each SearchResult, the panel checks hasValidLocation(), then adds a marker at the stored coordinates (near matches in a different colour). Restaurants without valid coordinates are skipped and a message states how many. When the user selects a marker, onResultSelected() opens the restaurant’s details (F11). No AI is involved.

**F11: Restaurant and Deal Information Validity**  
Related Use Case: UC05 View Map and Restaurant Details  
Related Sequence Diagram: SD03 Map, Details, Freshness and Explanation

Classes involved:

- HappyHourDeal \= stores the last-updated date and computes the freshness label.  
- Restaurant \= stores its own last-updated date.  
- SearchResult \= gives the interface the deal to display.  
- ResultsPanel / CliApp \= show the date, the label, and any warning.  
- AgentController \= returns the selected result.

Important methods:

- HappyHourDeal.getFreshness()  
- AgentController.getResult()  
- ResultsPanel.showDetails()  
- CliApp.displayResponse()

Execution:  
When the user selects a restaurant, MainWindow.onResultSelected() calls AgentController.getResult() and passes the SearchResult to ResultsPanel.showDetails(). The panel calls HappyHourDeal.getFreshness(), which returns FRESH if the deal was updated within 30 days, STALE if older, and UNKNOWN if there is no date. It displays the date and label next to the deal, and shows a visible warning for STALE or UNKNOWN. The CLI shows the same information as text.

**F12: Search History**  
Related Use Case: UC06 Review Search History   
Related Sequence Diagram: SD04 Search History and Refresh

Classes involved:

- MainWindow \= shows the history list and receives the user’s selection.  
- AgentController \= saves each completed search and restores a chosen one.  
- HistoryService \= stores, loads, and age-checks saved searches.  
- SearchHistory \= represents one saved search (query, constraints, results, time).  
- RestaurantAgent \= reruns a stored search for a refresh, without calling the LLM.

Important methods:

- AgentController.saveCurrentSearch()  
- HistoryService.saveSearch()  
- AgentController.getHistory()  
- AgentController.reopenSearch()  
- HistoryService.loadSearch()  
- HistoryService.isOutdated()  
- AgentController.refreshSearch()  
- RestaurantAgent.rerunSearch()

Execution:  
After each completed search, AgentController.saveCurrentSearch() creates a SearchHistory entry and passes it to HistoryService.saveSearch(). When the user opens Search History, getHistory() returns the saved entries. When the user selects one, reopenSearch() calls HistoryService.loadSearch(), restores the stored constraints and results, and checks isOutdated(). If the entry is outdated, a warning is shown with a Refresh option; refreshSearch() then calls RestaurantAgent.rerunSearch() to run the stored constraints again without calling the LLM. The user can also continue refining the reopened search (F09).

