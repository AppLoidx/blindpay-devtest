# controller/

## What lives here

- `SetupController` — Endpoints for initial user setup (TOS, receiver creation)
- `TransferController` — Endpoints for crypto transfers between users
- `UserController` — Endpoints for payin, payout, and user info queries

## Patterns used

- `@RestController` + `@RequestMapping` prefix per controller
- `@RequiredArgsConstructor` for constructor injection
- `@Slf4j` for logging
- Delegates all logic to service layer interfaces

## Dependencies

- Calls service interfaces (`UserServiceApi`) only
- Uses DTOs from `dto/` package for request/response bodies
- Never accesses repositories or entities directly

## Do not

- Add `@Transactional` — not appropriate at controller layer
- Import from `repository/` or `model/` packages
- Add business logic — controllers are thin delegation only
