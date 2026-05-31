# config/

## What lives here

- `BlindPayProperties` — `@ConfigurationProperties` binding for `blindpay.*` keys (apiKey, baseUrl, instanceId)
- `RestClientConfig` — Empty `@Configuration` class (no active beans — intentionally hollow)

## Patterns used

- `@ConfigurationProperties` with prefix binding
- Lombok `@Getter`/`@Setter` on properties classes
- `@Configuration` for Spring bean definitions

## Dependencies

- No dependencies on other project packages
- Consumed by service layer via constructor injection

## Do not

- Add HTTP client beans here — Cloudflare blocks Java HTTP clients
- Remove RestClientConfig — it exists intentionally even though empty
