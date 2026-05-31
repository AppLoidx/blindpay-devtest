# repository/

## What lives here

- `UserRepository` — JPA repository for `User` entity (extends JpaRepository)

## Patterns used

- Spring Data JPA interface-only repositories
- Must extend `JpaRepository` or `CrudRepository` (ArchUnit enforced)
- No implementation classes needed — Spring generates proxies

## Dependencies

- References `model/User` entity only
- Called by service layer only

## Do not

- Add `@Transactional` — handled at service layer
- Add custom query implementations unless truly needed
