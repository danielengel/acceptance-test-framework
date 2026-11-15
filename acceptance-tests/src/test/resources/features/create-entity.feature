Feature: Entity creation
  Scenario: Creating an entity stores it, publishes an event, and notifies the third party
    Given the third party accepts notifications for a new entity
    And the event stream is observed before entity creation
    When I create the entity
    Then the response status is 202
    And the response contains the entity ID
    And the entity is stored with status CREATED
    And an event containing the entity is published
    And the third party receives the entity notification
