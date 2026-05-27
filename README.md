# Read Me First

* This project is a Spring Boot microservice that integrates with Keycloak to provide authentication and authorization for Springs of MUD.
* The Springs of MUD game platform is a re-imagining of the Rivers of MUD Multi-User Dungeon.
* This service allows players to authenticate via the PlayerAuthController at this time with just a username and password.
* The README for the springs-of-mud-server repository gives more detail about the goals of this game platform.

# Getting Started

* Today this service requires a Java keystore to encrypt sensitive information such as the Keycloak credentials.
* It is deployed via Jenkins as a Linux systemd service.
* It provides only two endpoints: one for player token provision and one to decode the token.

### Reference Documentation
For further reference, please consider the following sections:

* [Keycloak Documentation](https://www.keycloak.org/documentation)
* [Spring OAuth 2.0](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.4.0-SNAPSHOT/reference/web/servlet.html)

### Future enhancements

* This project will eventually be enhanced to allow authentication via Kerberos.
* While the game is still in development, roles are not completely fleshed out yet, but automation and user roles are planned.
* Automation roles will be primarily for inter-service communication.
  * For example, [springs-of-mud-server](https://github.com/sneibarg/springs-of-mud-server/tree/master) will require a token with an automation role to refresh player accounts and characters.
  * The eventual integration of [som-nlp-service](https://github.com/sneibarg/som-nlp-service) will provide Natural Language Processing capabilities to Non-Player Characters and Agentic Players.
* User roles will be built for player, game master, designer, and administrative activities.
  * Player roles will be required to log into the game server.
  * The game master role will be required to log in as an immortal with classic ROM immortal powers.
  * Designer roles will be created for game design and content creation via [springs-of-mud-designer](https://github.com/sneibarg/springs-of-mud-designer).
  * Administrative roles will be required for taking component-level action such as restarting services.


