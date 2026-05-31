# Code Rules

## Enforcement

| Rule | Tool | What it checks |
|------|------|---------------|
| Controllers must not access Repositories directly | ArchUnit | Package dependency: `..controller..` → `..repository..` |
| Controllers must not be annotated with @Service | ArchUnit | Annotation check on controller classes |
| @Transactional must not be on Controllers or Repositories | ArchUnit | Annotation check on controller/repository classes and methods |
| JPA @Entity must not appear in Controller dependencies | ArchUnit | Dependency check: controllers → @Entity-annotated classes |
| Service classes must implement interfaces | ArchUnit | Interface implementation check in `..service..` package |
| No @Autowired on fields (constructor injection only) | ArchUnit | Field annotation check across all classes |
| Controllers must be annotated @RestController/@Controller | ArchUnit | Annotation presence in `..controller..` package |
| Repositories must extend JpaRepository/CrudRepository | ArchUnit | Type hierarchy check in `..repository..` package |
| Single Responsibility (max 8 project class dependencies) | ArchUnit | Dependency count per class |
| service.impl classes must implement service interface | ArchUnit | Package-to-interface enforcement |
| Max method length 20 lines | Checkstyle | MethodLength check |
| Max file length 200 lines | Checkstyle | FileLength check |
| No wildcard imports | Checkstyle | AvoidStarImport |
| No tab characters | Checkstyle | FileTabCharacter |
| Max line length 120 | Checkstyle | LineLength |
| No empty catch blocks | PMD | EmptyCatchBlock |
| No God classes | PMD | GodClass |
| No dead code (unused fields/methods/variables) | PMD | UnusedPrivateField, UnusedPrivateMethod, UnusedLocalVariable |
| No System.out.println | PMD | SystemPrintln |
| Cyclomatic complexity max 10 per method | PMD | CyclomaticComplexity |
| No duplicate string literals (max 3) | PMD | AvoidDuplicateLiterals |
| Max 15 methods per class | PMD | TooManyMethods |
| Max 10 fields per class | PMD | TooManyFields |
| Correctness bugs | SpotBugs | CORRECTNESS category |
| Bad practices | SpotBugs | BAD_PRACTICE category |
| Performance issues | SpotBugs | PERFORMANCE category |
| Security vulnerabilities | SpotBugs | SECURITY category (via FindSecBugs) |

## Running checks

```bash
mvn verify                           # Run all checks
mvn test                             # Run tests (including ArchUnit)
mvn checkstyle:check                 # Checkstyle only
mvn pmd:check                        # PMD only
mvn spotbugs:check                   # SpotBugs only
```

## Notes

- **ErrorProne**: Removed — incompatible with Lombok on Java 21 (crashes on generated code). SpotBugs covers similar bug categories.
- **SpotBugs exclusions**: COMMAND_INJECTION (curl by design), CRLF_INJECTION_LOGS (internal log params), EI_EXPOSE_REP2 (Lombok constructors in Spring beans).
