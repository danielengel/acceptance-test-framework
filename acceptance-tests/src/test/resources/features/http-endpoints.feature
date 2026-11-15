Feature: HTTP endpoints
  Scenario: Service two returns a greeting
    When I request the service two greeting
    Then the response status is 200
    And the greeting is "Hello, world!"

  Scenario: An entity with a blank name is rejected
    When I create an entity with a blank name
    Then the response status is 400
