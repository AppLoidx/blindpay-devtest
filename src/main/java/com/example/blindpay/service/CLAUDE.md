# service/

## What lives here

- `UserServiceApi` — Interface defining user operations contract
- `UserService` — Implementation: user CRUD, orchestrates payin/payout/transfer
- `BlindPayApi` — Interface defining BlindPay API client contract
- `BlindPayApiService` — Implementation: orchestrates BlindPay API calls
- `CurlHttpClient` — Low-level HTTP via curl/ProcessBuilder (Cloudflare bypass)

## Patterns used

- Interface + implementation pattern (enforced by ArchUnit)
- `@Service` on implementations only
- `@RequiredArgsConstructor` for constructor injection
- `@Slf4j` for logging

## Dependencies

- `UserService` → `UserRepository`, `BlindPayApi`
- `BlindPayApiService` → `CurlHttpClient`, `BlindPayProperties`
- `CurlHttpClient` → `ProcessBuilder` (shells out to curl)

## Do not

- Replace curl with Java HTTP clients — Cloudflare blocks them
- Add controller-layer concerns (request mapping, validation annotations)
