# exception/

## What lives here

- `BlindPayApiException` — Runtime exception wrapping BlindPay API errors (carries HTTP status + body)
- `GlobalExceptionHandler` — `@RestControllerAdvice` mapping exceptions to HTTP responses

## Patterns used

- `@RestControllerAdvice` for centralized error handling
- `@ExceptionHandler` methods per exception type
- Returns `ApiErrorResponse` DTO for consistent error shape
- `BlindPayApiException` passes through upstream HTTP status code

## Dependencies

- `GlobalExceptionHandler` → `dto/ApiErrorResponse`
- `BlindPayApiException` is thrown by service layer, caught by handler

## Do not

- Throw checked exceptions — project uses runtime exceptions only
- Add exception classes without a corresponding handler method
