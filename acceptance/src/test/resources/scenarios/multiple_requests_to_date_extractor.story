Lifecycle:
Before:
Given MSB configuration with consumer thread pool size 5
And MSB configuration with consumer thread pool queue capacity 20
And MSB configuration with timer thread pool size 10
And start MSB
And microservice DateExtractor
After:
Outcome: ANY
Then shutdown MSB

Scenario: Sending multiple requests to date extractor microservice in parallel

Given 2 requesters send a request to namespace search:parsers:facets:v1 with query 'Holidays in 2015'
Then wait responses in 5000 ms